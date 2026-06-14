package com.spooky2.huntkill.ui.hunt

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spooky2.huntkill.core.lookup.LookupMatch
import com.spooky2.huntkill.core.model.DropoutSegment
import com.spooky2.huntkill.core.model.ScanParameters
import com.spooky2.huntkill.core.model.ScanResult
import com.spooky2.huntkill.service.ScanRunNotifier
import com.spooky2.huntkill.data.FrequencyDatabaseSource
import com.spooky2.huntkill.data.RunHistoryRepository
import com.spooky2.huntkill.data.SessionHolder
import com.spooky2.huntkill.data.UsbConnectionManager
import com.spooky2.huntkill.log.LogBus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

/** Phase the Hunt→Kill flow is currently in. */
enum class HuntPhase { Idle, Hunting, HitsReady, HitsReadyWithDropouts, Killing, Done, Cancelled, Error }

/** Default reverse-lookup tolerance, mirroring the original report's .25%. */
const val DEFAULT_LOOKUP_TOLERANCE_PERCENT = 0.25

/** Tolerance presets offered as selectable chips on the Hits screen. */
val LOOKUP_TOLERANCE_OPTIONS = listOf(0.1, 0.25, 0.5, 1.0)

/**
 * Editable scan parameters surfaced to the Hunt config screen. Kept as a flat,
 * UI-friendly view over [ScanParameters] (frequencies, dwell, target amplitude),
 * with sensible Hunt-and-Kill defaults.
 */
data class HuntParamsUi(
    val startFrequencyText: String = "41000",
    val endFrequencyText: String = "1800000",
    // Default is 180s (3 minutes) per frequency — the real Hunt-and-Kill dwell. The JVM
    // demo/pause tests override this with updateDwellSeconds("0") to stay fast.
    val dwellSecondsText: String = "180",
    val targetAmplitudeCvText: String = "2000",
) {
    /** First validation problem with the entered fields, or null when all valid. */
    fun validationError(): String? {
        val start = startFrequencyText.toDoubleOrNull()
        val end = endFrequencyText.toDoubleOrNull()
        val dwell = dwellSecondsText.toDoubleOrNull()
        val amp = targetAmplitudeCvText.toIntOrNull()
        return when {
            start == null || start <= 0 -> "Enter a valid start frequency."
            end == null || end <= 0 -> "Enter a valid end frequency."
            end <= start -> "End frequency must be greater than start."
            dwell == null || dwell < 0 -> "Enter a valid dwell time."
            amp == null || amp <= 0 -> "Enter a valid amplitude."
            else -> null
        }
    }

    /** True when every field parses and the range is sane. */
    fun isValid(): Boolean = validationError() == null

    fun toScanParameters(isDemo: Boolean): ScanParameters {
        val base = ScanParameters(
            startFrequency = startFrequencyText.toDoubleOrNull() ?: 41000.0,
            endFrequency = endFrequencyText.toDoubleOrNull() ?: 1_800_000.0,
            dwellSeconds = dwellSecondsText.toDoubleOrNull() ?: 180.0,
            targetAmplitudeCv = targetAmplitudeCvText.toIntOrNull() ?: 2000,
            // Single Hunt→Kill cycle: the UI flow ends at the Done summary rather
            // than looping scan→kill until no hits remain (original refine loop).
            continueRefining = false,
        )
        // Live hardware keeps the ScanParameters defaults, which mirror the original
        // Spooky2 timing: 0.07s settle per sweep step (~17 min over 15k steps),
        // 200ms start delay, and the 330-step amplitude ramp up/down. Reading the
        // sensor without the settle delay returns values before the response has
        // stabilized, degrading hit quality.
        if (!isDemo) return base
        // Demo replay: the recorded dump has no real latency; run fast and skip the
        // ramp so the replayed session reproduces the golden 10 hits.
        return base.copy(
            startDelayMs = 0,
            minReadDelaySeconds = 0.0,
            enableAmplitudeRampUp = false,
            enableAmplitudeRampDown = false,
        )
    }
}

/**
 * A clickable hit-frequency marker drawn on the scan graph. [stepIndex] is the
 * 0-based sweep-step index, aligned to [HuntUiState.fullHistory]. [isFinal] is
 * false for live provisional candidates (semi-transparent red) and true once
 * detection has completed (solid red).
 */
data class GraphMarker(
    val stepIndex: Int,
    val frequency: Double,
    val deviation: Double,
    val isFinal: Boolean,
)

/** Connection summary shown as an info chip on the Hunt config screen. */
data class GeneratorInfo(
    val generatorType: String,
    val baudRate: Int,
    /** 0-based port index of the active generator, or null for test replay sessions. */
    val portIndex: Int?,
    /** Total selectable ports on the device, or null for test replay sessions. */
    val portCount: Int?,
    /** Device serial of the active generator (`:r91`), or null when not read/timed out. */
    val serialNumber: String? = null,
    /** Firmware version of the active generator (`:r68`), or null when not read. */
    val firmwareVersion: String? = null,
    /** Hardware type of the active generator (`:r80`), or null when not read. */
    val hardwareType: String? = null,
) {
    /** True when the device exposes more than one generator port (switcher shown). */
    val hasMultiplePorts: Boolean get() = (portCount ?: 1) > 1

    /**
     * Label for the ACTIVE generator: its serial when known (so a dual-generator box
     * with distinct serials reads by serial rather than "port 1/2"), else the
     * "Generator N" port fallback, else the generator type for single-port sessions.
     */
    fun activeLabel(): String = when {
        serialNumber != null -> "S/N $serialNumber"
        portIndex != null -> "Generator ${portIndex + 1}"
        else -> generatorType
    }
}

/** Live scan + kill state observed by the Live, Hits, and Kill screens. */
data class HuntUiState(
    val phase: HuntPhase = HuntPhase.Idle,
    val params: HuntParamsUi = HuntParamsUi(),
    val statusText: String = "",
    val currentFrequency: Double = 0.0,
    val amplitudeCv: Int = 0,
    val currentReading: Double = 0.0,
    val runningAverage: Double = 0.0,
    val percentComplete: Double = 0.0,
    /** Live-tail window of the most recent readings (default graph view). */
    val angleHistory: List<Double> = emptyList(),
    /**
     * FULL per-step reading history for the current hunt (up to ~15k points),
     * backing the horizontally scrollable graph. Stored as a [FloatArray] so 15k
     * points stay cheap. Grows during the sweep; replaced by the merged outcome
     * after a re-scan.
     */
    val fullHistory: FloatArray = FloatArray(0),
    /**
     * Per-step validity mask aligned to [fullHistory]: `false` marks a flagged
     * dropout step so the graph can tint those regions. Same length as
     * [fullHistory] once the sweep completes.
     */
    val historyValid: BooleanArray = BooleanArray(0),
    /** Merged dropout segments from the completed sweep; empty on a clean sweep. */
    val dropoutSegments: List<DropoutSegment> = emptyList(),
    /** Total sweep steps, used to map [fullHistory] indices onto the X axis. */
    val totalSweepSteps: Int = 0,
    /**
     * Hit-frequency markers drawn on the scan graph. During Hunting these are the
     * live provisional candidates (isFinal=false); after detection completes they
     * are the real hits mapped to sweep-step indices (isFinal=true). Aligned to
     * [fullHistory] / [angleHistory] indices.
     */
    val graphMarkers: List<GraphMarker> = emptyList(),
    /** True while a segment re-scan is running (drives the warning-card spinner). */
    val rescanInProgress: Boolean = false,
    val hits: List<ScanResult> = emptyList(),
    val killIndex: Int = 0,
    val killTotal: Int = 0,
    val killDwellRemainingSeconds: Int = 0,
    /**
     * Repeat mode for the kill phase. When true (the DEFAULT) the kill loops over all
     * detected frequencies continuously until the user toggles it off (the current pass
     * then finishes and the run completes) or stops. Drives the Kill screen's repeat
     * control. Mirrors [GeneratorRunController.repeatKillFlag], which the engine reads live each pass-end.
     */
    val repeatKill: Boolean = true,
    /**
     * Refinement mode (original Spooky2 "Continue Refining Hits"). When true (the
     * DEFAULT — matches the canonical GX preset's BFB_Continue_Refining_Hits=1), each
     * kill pass runs ONCE (superseding [repeatKill]) and is followed by a refinement
     * generation: re-scan a narrow window around each hit at a halved step, kill the
     * refined hits, repeat — until no hits remain, the user toggles it off, or stops.
     * Mirrors [GeneratorRunController.refineFlag], which the run loop reads live at each pass boundary.
     */
    val refineHits: Boolean = true,
    /** Current Hunt & Kill generation (1 = initial full sweep; 2+ = refinements). */
    val refineGeneration: Int = 1,
    /**
     * Hit-list view mode shared by the Hits and Kill screens. Compact (the DEFAULT)
     * shows only the frequency per row; details adds deviation + reverse-lookup
     * matches. Toggling it also standardizes per-row "show all" expansion (screens
     * reset their local maps).
     */
    val hitsCompactView: Boolean = true,
    val isPaused: Boolean = false,
    val elapsedSeconds: Int = 0,
    val errorMessage: String? = null,
    /**
     * Reverse-lookup matches per hit frequency, keyed by [ScanResult.frequency]. Populated
     * after a hunt completes (phase Done/Cancelled) by matching each hit against the
     * bundled frequency database. Empty until the lookup runs; an empty list value means
     * "no matches" for that hit.
     */
    val lookupResults: Map<Double, List<LookupMatch>> = emptyMap(),
    /** True while reverse lookup is computing (database loading or matching in flight). */
    val lookupBusy: Boolean = false,
    /** Tolerance (percent) the current [lookupResults] were computed at. */
    val lookupTolerancePercent: Double = com.spooky2.huntkill.ui.hunt.DEFAULT_LOOKUP_TOLERANCE_PERCENT,
    /** Connection summary for the config screen chip; null until connected. */
    val generator: GeneratorInfo? = null,
    /** True while a generator-port switch is in flight (disables the switcher + Start). */
    val isSwitchingGenerator: Boolean = false,
    /** Estimated whole seconds remaining in the current sweep, 0 until measurable. */
    val estimatedRemainingSeconds: Int = 0,
    /**
     * Short label describing the action currently in flight (e.g. "Starting…",
     * "Cancelling…", "Disconnecting…"). Null when no slow operation is running.
     * Drives busy indicators and button-disabled state in the UI.
     */
    val busyAction: String? = null,
) {
    /** True while a hunt/kill is actively running (used to lock the switcher). */
    val isRunning: Boolean get() = phase == HuntPhase.Hunting || phase == HuntPhase.Killing
}

/**
 * Thin holder over a single [GeneratorRunController]. Owns the shared [_state]/[_events]
 * (the single source of truth the UI observes) and the [SessionHolder], and forwards every
 * public method to the controller. The run logic lives entirely in the controller; this
 * keystone keeps behaviour identical while making the run lifecycle reusable per-generator
 * for the upcoming multi-generator refactor.
 */
@HiltViewModel
class HuntViewModel @Inject constructor(
    private val sessionHolder: SessionHolder,
    private val log: LogBus,
    // Optional so JVM ViewModel tests can construct with just (holder, log); only the
    // generator switcher needs it and that path is USB-only.
    private val usbConnectionManager: UsbConnectionManager? = null,
    // Optional for the same reason: reverse lookup loads a bundled Android asset, so JVM
    // tests pass null (or an in-memory fake) and the post-hunt lookup adapts accordingly.
    private val frequencyDatabase: FrequencyDatabaseSource? = null,
    // Optional so JVM ViewModel tests can construct with just (holder, log); when present
    // every completed hunt's final frequencies are persisted as a RunRecord.
    private val runHistory: RunHistoryRepository? = null,
    // Optional for the same reason: drives the foreground-service run notification
    // (live hunt/kill progress, keep-alive + wake lock while backgrounded).
    private val runNotifier: ScanRunNotifier? = null,
) : ViewModel() {

    // Owned here (not in the controller) so the UI observes one source of truth and the
    // existing cancel test's reflection on `_state` continues to address the live flow.
    private val _state = MutableStateFlow(HuntUiState())
    val state: StateFlow<HuntUiState> = _state.asStateFlow()

    /** One-shot UI messages (snackbars), e.g. "Generator zeroed". */
    private val _events = Channel<String>(Channel.BUFFERED)
    val events: Flow<String> = _events.receiveAsFlow()

    /** The single run controller this holder wraps. */
    private val controller = GeneratorRunController(
        index = 0,
        scope = viewModelScope,
        log = log,
        _state = _state,
        _events = _events,
        // Live session getter: each entry method resolves the currently-connected session
        // exactly as the monolith did (sessionHolder.current()), preserving null/error guards.
        liveSession = { sessionHolder.current() },
        // Per-run reconnect seam: startHunt rebuilds a fresh replay session (tests) or reuses
        // the open live session — the SessionHolder.acquireForHunt() contract, unchanged.
        acquireSession = { sessionHolder.acquireForHunt() },
        frequencyDatabase = frequencyDatabase,
        runHistory = runHistory,
        runNotifier = runNotifier,
    )

    init {
        controller.refreshGeneratorInfo()
        observeUsbDetach()
    }

    /**
     * Collect USB device-detach events from [UsbConnectionManager]. On detach of the
     * active device: cancel the running hunt job, mark the session dead, and transition
     * to Error. We do NOT attempt zeroOutput over the dead transport — the USB link is
     * gone so it would time out. The safety-stop path is kept intact for live cancels
     * where the transport is still open.
     */
    private fun observeUsbDetach() {
        val manager = usbConnectionManager ?: return
        viewModelScope.launch {
            manager.deviceDetached.collect {
                // Guard the handler so one failure can't kill this collector — it must
                // keep observing detach events for the whole ViewModel lifetime.
                try {
                    log.w(TAG, "USB device detached — aborting hunt")
                    controller.onDeviceDetached(onSessionLost = { sessionHolder.clear() })
                } catch (e: kotlinx.coroutines.CancellationException) {
                    throw e
                } catch (e: Exception) {
                    log.e(TAG, "Detach handling failed: ${e.message}")
                }
            }
        }
    }

    /** Pull connection info from the current session into [HuntUiState.generator]. */
    fun refreshGeneratorInfo() = controller.refreshGeneratorInfo()

    /**
     * Switch the active generator to a different USB port of the SAME device. Closes
     * the current session, opens the chosen port (permission already granted), and
     * swaps it into [SessionHolder]. No-op while a hunt is running or with no USB manager.
     */
    fun switchGenerator(portIndex: Int) {
        if (_state.value.isRunning || _state.value.isSwitchingGenerator) return
        val manager = usbConnectionManager ?: return
        val current = _state.value.generator
        if (current?.portIndex == portIndex) return

        log.i(TAG, "Switching generator to port $portIndex")
        _state.update { it.copy(isSwitchingGenerator = true) }
        viewModelScope.launch {
            runCatching {
                val session = manager.switchPort(portIndex)
                sessionHolder.replace(session)
            }.onSuccess {
                controller.refreshGeneratorInfo()
                _state.update { it.copy(isSwitchingGenerator = false) }
                // Prefer the new port's serial in the toast when it was read.
                val label = _state.value.generator?.activeLabel() ?: "Generator ${portIndex + 1}"
                _events.trySend("Switched to $label")
            }.onFailure { error ->
                log.e(TAG, "Generator switch failed: ${error.message}")
                _state.update {
                    it.copy(
                        isSwitchingGenerator = false,
                        errorMessage = error.message ?: "Generator switch failed",
                    )
                }
            }
        }
    }

    fun toggleRepeatKill() = controller.toggleRepeatKill()

    fun setHitsCompactView(compact: Boolean) = controller.setHitsCompactView(compact)

    fun toggleRefineHits() = controller.toggleRefineHits()

    fun updateStartFrequency(v: String) = controller.updateStartFrequency(v)
    fun updateEndFrequency(v: String) = controller.updateEndFrequency(v)
    fun updateDwellSeconds(v: String) = controller.updateDwellSeconds(v)
    fun updateTargetAmplitude(v: String) = controller.updateTargetAmplitude(v)

    /** Launch the full Hunt→Kill flow off the main thread; collect progress into state. */
    fun startHunt() = controller.startHunt()

    /**
     * "Continue anyway": skip the re-scan and kill using the hits computed with
     * the invalid steps already excluded. No-op outside the dropout-warning state.
     */
    fun continueAnyway() = controller.continueAnyway()

    /**
     * Re-run a treatment from a saved run's frequencies (History → "Re-run treatment").
     * See [GeneratorRunController.startKillFromFrequencies].
     */
    fun startKillFromFrequencies(
        freqs: List<Double>,
        dwellSeconds: Double,
        amplitudeCv: Int,
        deviations: List<Double> = emptyList(),
    ): Boolean = controller.startKillFromFrequencies(freqs, dwellSeconds, amplitudeCv, deviations)

    /**
     * "Re-scan affected segments": re-sweep the flagged segments, splice the fresh
     * readings over the old, recompute hits, then proceed to kill with the merged hits.
     */
    fun rescanAffectedSegments() = controller.rescanAffectedSegments()

    /**
     * Toggle pause/resume on the running hunt. While paused the engine holds at the
     * current frequency (no new commands), the sweep progress and kill countdown
     * freeze, and the elapsed clock stops advancing.
     */
    fun togglePause() = controller.togglePause()

    /**
     * "Treat this now": ask the running kill to immediately jump to hit [index]
     * (0-based) and continue treating from there onward. No-op outside the kill phase.
     */
    fun jumpToHit(index: Int) = controller.jumpToHit(index)

    /** Cancel the running scan coroutine and run the safety-stop path. */
    fun cancel() = controller.cancel()

    /** Explicit safety-stop button: zero the generator output immediately. */
    fun safetyStop() = controller.safetyStop()

    /**
     * Reset a terminal phase (Done/Cancelled/Error) back to Idle when the user returns
     * to the Hunt config screen, so the config UI isn't stuck showing a finished run.
     */
    fun prepareForConfig() = controller.prepareForConfig()

    /** Re-run reverse lookup at a different tolerance from the Hits screen. */
    fun setLookupTolerance(tolerancePercent: Double) = controller.setLookupTolerance(tolerancePercent)

    /**
     * On-demand reverse lookup for a SINGLE frequency — used by the graph marker popup.
     * See [GeneratorRunController.lookupForFrequency].
     */
    suspend fun lookupForFrequency(
        frequency: Double,
        tolerancePercent: Double = _state.value.lookupTolerancePercent,
    ): List<LookupMatch>? = controller.lookupForFrequency(frequency, tolerancePercent)

    /**
     * Close the active session and reset to a disconnected state. Used by the
     * "Disconnect" action on the post-run summary before navigating to Connect.
     */
    fun disconnect() = controller.disconnect(onDisconnect = { sessionHolder.clear() })

    override fun onCleared() {
        super.onCleared()
        controller.dispose()
    }

    companion object {
        private const val TAG = "Hunt"
    }
}
