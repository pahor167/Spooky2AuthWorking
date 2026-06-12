package com.spooky2.huntkill.ui.hunt

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spooky2.huntkill.core.lookup.LookupMatch
import com.spooky2.huntkill.core.lookup.ReverseLookup
import com.spooky2.huntkill.core.lookup.ReverseLookupParameters
import com.spooky2.huntkill.core.model.DropoutSegment
import com.spooky2.huntkill.core.model.ScanOutcome
import com.spooky2.huntkill.core.model.ScanParameters
import com.spooky2.huntkill.core.model.ScanProgress
import com.spooky2.huntkill.core.model.ScanResult
import com.spooky2.huntkill.core.scan.KillControl
import com.spooky2.huntkill.core.scan.PauseGate
import com.spooky2.huntkill.core.scan.RefinementPlanner
import com.spooky2.huntkill.data.FrequencyDatabaseSource
import com.spooky2.huntkill.data.GeneratorSession
import com.spooky2.huntkill.data.RunHistoryRepository
import com.spooky2.huntkill.data.RunHit
import com.spooky2.huntkill.data.RunRecord
import com.spooky2.huntkill.data.SessionHolder
import com.spooky2.huntkill.data.UsbConnectionManager
import com.spooky2.huntkill.log.LogBus
import kotlinx.coroutines.ensureActive
import kotlin.coroutines.coroutineContext
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
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
     * control. Mirrors [repeatKillFlag], which the engine reads live each pass-end.
     */
    val repeatKill: Boolean = true,
    /**
     * Refinement mode (original Spooky2 "Continue Refining Hits"). When true, each
     * kill pass runs ONCE (superseding [repeatKill]) and is followed by a refinement
     * generation: re-scan a narrow window around each hit at a halved step, kill the
     * refined hits, repeat — until no hits remain, the user toggles it off, or stops.
     * Mirrors [refineFlag], which the run loop reads live at each pass boundary.
     */
    val refineHits: Boolean = false,
    /** Current Hunt & Kill generation (1 = initial full sweep; 2+ = refinements). */
    val refineGeneration: Int = 1,
    /**
     * Hit-list view mode shared by the Hits and Kill screens. Compact shows only the
     * frequency per row; details adds deviation + reverse-lookup matches. Toggling it
     * also standardizes per-row "show all" expansion (screens reset their local maps).
     */
    val hitsCompactView: Boolean = false,
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
) : ViewModel() {

    private val _state = MutableStateFlow(HuntUiState())
    val state: StateFlow<HuntUiState> = _state.asStateFlow()

    /** One-shot UI messages (snackbars), e.g. "Generator zeroed". */
    private val _events = Channel<String>(Channel.BUFFERED)
    val events: Flow<String> = _events.receiveAsFlow()

    private var huntJob: Job? = null

    /** Reverse-lookup coroutine; cancelled/replaced when the tolerance is re-selected. */
    private var lookupJob: Job? = null

    /** Parameters of the in-flight/just-finished hunt; reused by the re-scan flow. */
    private var activeParameters: ScanParameters? = null

    /**
     * True while the current Kill is treating frequencies loaded from a saved run
     * (Re-run from History) rather than a fresh hunt. Re-runs are NOT persisted again —
     * they treat already-saved frequencies, so saving would just duplicate the record.
     */
    private var fromReRun: Boolean = false

    /** Guards against double-persisting the same hunt (publishSweepOutcome can re-run). */
    private var savedThisRun: Boolean = false

    /** Last sweep outcome (readings + validity + segments); spliced by a re-scan. */
    private var lastOutcome: ScanOutcome? = null

    /**
     * Incrementally-grown per-step reading history for the LIVE scrollable graph.
     * Indexed by sweep-step (0-based), so provisional marker step indices align to
     * it directly. Published as [HuntUiState.fullHistory] each step; replaced by the
     * authoritative [ScanOutcome.sweepReadings] once the sweep finishes.
     */
    private val liveHistory = ArrayList<Float>()

    init {
        refreshGeneratorInfo()
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
                    pauseGate.resume()
                    stopElapsedTicker()
                    huntJob?.cancel()
                    huntJob = null
                    // Mark the session dead so subsequent startHunt blocks on "not connected".
                    sessionHolder.clear()
                    _state.update {
                        it.copy(
                            phase = HuntPhase.Error,
                            statusText = "Generator unplugged",
                            errorMessage = "Generator unplugged",
                            isPaused = false,
                            rescanInProgress = false,
                            busyAction = null,
                        )
                    }
                } catch (e: kotlinx.coroutines.CancellationException) {
                    throw e
                } catch (e: Exception) {
                    log.e(TAG, "Detach handling failed: ${e.message}")
                }
            }
        }
    }

    /** Pull connection info from the current session into [HuntUiState.generator]. */
    fun refreshGeneratorInfo() {
        val session = sessionHolder.current()
        _state.update {
            it.copy(
                generator = session?.let { s ->
                    GeneratorInfo(
                        generatorType = s.generatorType,
                        baudRate = s.baudRate,
                        portIndex = s.usbPort?.index,
                        portCount = s.usbPort?.count,
                        serialNumber = s.serialNumber,
                        firmwareVersion = s.firmwareVersion,
                        hardwareType = s.hardwareType,
                    )
                },
            )
        }
    }

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
                refreshGeneratorInfo()
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

    /** Drives a 1s tick so [HuntUiState.elapsedSeconds] advances while running, not paused. */
    private var elapsedTicker: Job? = null

    /** Cooperative pause for the running scan; shared between this VM and the engine. */
    private val pauseGate = PauseGate()

    /** Cooperative jump control for the kill phase; shared between this VM and the engine. */
    private val killControl = KillControl()

    /**
     * Live repeat flag the engine's `killHits` reads at each frequency-pass boundary.
     * Held separately from [HuntUiState.repeatKill] (which mirrors it for the UI) so a
     * mid-kill toggle is honored on the next pass without rebuilding the kill call. Default
     * true = repeat ON. Kept in sync by [toggleRepeatKill].
     */
    private val repeatKillFlag = MutableStateFlow(true)

    /**
     * Flip kill-phase repeat on/off. Updates both the UI state and the live
     * [repeatKillFlag] the running engine reads, so a mid-kill toggle takes effect at the
     * next pass-end: turning repeat OFF lets the current pass finish and the run complete;
     * turning it ON keeps looping.
     */
    fun toggleRepeatKill() {
        val next = !repeatKillFlag.value
        repeatKillFlag.value = next
        _state.update { it.copy(repeatKill = next) }
        log.i(TAG, "Repeat kill ${if (next) "enabled" else "disabled"}")
    }

    /**
     * Switch the hit-list view between compact (frequency only) and details (deviation +
     * matches). Shared across the Hits and Kill screens; screens reset their per-row
     * "show all" expansion when this changes so every frequency renders uniformly.
     */
    fun setHitsCompactView(compact: Boolean) {
        _state.update { it.copy(hitsCompactView = compact) }
    }

    /**
     * Live refinement flag the run loop reads at each kill-pass boundary, mirroring
     * [HuntUiState.refineHits] — same live-toggle pattern as [repeatKillFlag]. While
     * true the kill does NOT repeat (one pass per generation); after each pass a
     * refinement generation re-scans around the hits at a halved step.
     */
    private val refineFlag = MutableStateFlow(false)

    /**
     * True while a refinement generation's sweep is running. Gates [onProgress] so the
     * narrow-window readings do NOT append to the gen-1 live graph (different grid).
     */
    @Volatile
    private var refinementSweepActive = false

    /** Flip refinement mode. Honored live at the next kill-pass boundary. */
    fun toggleRefineHits() {
        val next = !refineFlag.value
        refineFlag.value = next
        _state.update { it.copy(refineHits = next) }
        log.i(TAG, "Refine hits ${if (next) "enabled" else "disabled"}")
    }

    fun updateStartFrequency(v: String) = updateParams { it.copy(startFrequencyText = v) }
    fun updateEndFrequency(v: String) = updateParams { it.copy(endFrequencyText = v) }
    fun updateDwellSeconds(v: String) = updateParams { it.copy(dwellSecondsText = v) }
    fun updateTargetAmplitude(v: String) = updateParams { it.copy(targetAmplitudeCvText = v) }

    private fun updateParams(transform: (HuntParamsUi) -> HuntParamsUi) {
        _state.update { it.copy(params = transform(it.params)) }
    }

    /** Launch the full Hunt→Kill flow off the main thread; collect progress into state. */
    fun startHunt() {
        if (huntJob?.isActive == true) return
        if (sessionHolder.current() == null) {
            log.e(TAG, "startHunt blocked: not connected")
            _state.update {
                it.copy(phase = HuntPhase.Error, errorMessage = "Not connected. Connect first.")
            }
            return
        }

        pauseGate.resume()
        liveHistory.clear()
        // A fresh hunt: its hits ARE persisted (not a History re-run), once.
        fromReRun = false
        savedThisRun = false
        _state.update {
            it.copy(
                phase = HuntPhase.Hunting,
                statusText = "Starting hunt...",
                angleHistory = emptyList(),
                fullHistory = FloatArray(0),
                historyValid = BooleanArray(0),
                graphMarkers = emptyList(),
                hits = emptyList(),
                killIndex = 0,
                killTotal = 0,
                killDwellRemainingSeconds = 0,
                isPaused = false,
                rescanInProgress = false,
                elapsedSeconds = 0,
                estimatedRemainingSeconds = 0,
                errorMessage = null,
                busyAction = "Starting…",
            )
        }
        startElapsedTicker()

        huntJob = viewModelScope.launch(Dispatchers.Default) {
            // The live USB path reuses the open session across hunts (no reconnector).
            // Test replay sessions set a reconnector so a fresh single-use FakeTransport
            // is rebuilt each run. Both go through SessionHolder.acquireForHunt().
            val session = runCatching { sessionHolder.acquireForHunt() }.getOrElse { error ->
                log.e(TAG, "acquireForHunt failed: ${error.message}")
                _state.update {
                    it.copy(
                        phase = HuntPhase.Error,
                        errorMessage = error.message ?: "Reconnect failed",
                        busyAction = null,
                    )
                }
                return@launch
            }
            if (session == null) {
                log.e(TAG, "acquireForHunt returned no session")
                _state.update {
                    it.copy(
                        phase = HuntPhase.Error,
                        errorMessage = "Not connected. Connect first.",
                        busyAction = null,
                    )
                }
                return@launch
            }

            // Timing depends on the session kind: live hardware uses the original
            // Spooky2 settle delay + amplitude ramp; the demo replay runs fast.
            // continueRefining mirrors the UI switch (the run loop reads refineFlag live;
            // the param keeps the persisted/logged parameters honest).
            val parameters = _state.value.params
                .toScanParameters(isDemo = session.isDemo)
                .copy(continueRefining = refineFlag.value)
            _state.update { it.copy(refineGeneration = 1) }
            log.i(
                TAG,
                "startHunt(${if (session.isDemo) "demo" else "live"}): " +
                    "start=${parameters.startFrequency} end=${parameters.endFrequency} " +
                    "dwell=${parameters.dwellSeconds}s ampCv=${parameters.targetAmplitudeCv} " +
                    "readDelay=${parameters.minReadDelaySeconds}s ramp=${parameters.enableAmplitudeRampUp}",
            )

            activeParameters = parameters

            runCatching {
                // Run the SWEEP only. The kill decision is made afterwards so a
                // dropout can be surfaced before any frequencies are treated.
                val outcome = session.engine.runBiofeedbackScanDetailed(
                    parameters,
                    { progress -> onProgress(progress, parameters) },
                    pauseGate,
                )
                lastOutcome = outcome
                publishSweepOutcome(outcome)

                if (outcome.segments.isEmpty()) {
                    // Clean sweep: unchanged auto-kill flow.
                    proceedToKill(session, parameters, outcome.hits, cycle = 1)
                } else {
                    // Dropouts detected: STOP before the kill, surface the warning.
                    surfaceDropouts(outcome, parameters)
                }
            }.onFailure { error ->
                if (error is kotlinx.coroutines.CancellationException) throw error
                log.e(TAG, "Scan failed: ${error.message}")
                stopElapsedTicker()
                safetyStopInternal(session)
                _state.update {
                    it.copy(
                        phase = HuntPhase.Error,
                        errorMessage = error.message ?: "Scan failed",
                        isPaused = false,
                        busyAction = null,
                    )
                }
            }
        }
    }

    /** Store the full reading history + dropout diagnostics into UI state, and kick off
     *  reverse lookup immediately so matches are available as soon as hits are known. */
    private fun publishSweepOutcome(outcome: ScanOutcome) {
        val markers = finalMarkers(outcome.hits)
        // Overlay the display lead-in onto the validity mask for the FINAL graph too, so
        // the unsettled lead-in (which would re-introduce the leading vertical spike) stays
        // excluded after the sweep completes — matching the live view. This is a DISPLAY
        // copy; detection ran on outcome.sweepValid untouched, and lastOutcome (used by the
        // re-scan splice) still holds the original engine mask. Real dropouts are preserved.
        val displayValid = displayValidWithLeadIn(outcome.sweepValid)
        _state.update {
            it.copy(
                fullHistory = outcome.sweepReadings,
                historyValid = displayValid,
                dropoutSegments = outcome.segments,
                totalSweepSteps = outcome.sweepReadings.size,
                graphMarkers = markers,
                hits = outcome.hits,
            )
        }
        // Start reverse lookup as soon as hits are known (sweep complete), so the Kill
        // screen and dropout-warning screen can already show matches during treatment.
        if (outcome.hits.isNotEmpty()) {
            runReverseLookup(outcome.hits, _state.value.lookupTolerancePercent)
        }
        // Persist the run the MOMENT final hits are known — before the kill starts — so a
        // later cancel never loses the found frequencies. Re-runs and empty-hit sweeps are
        // not saved; the savedThisRun guard makes this idempotent across re-scan merges.
        persistRunIfNeeded(outcome.hits)
    }

    /**
     * Save the completed hunt's final frequencies as a [RunRecord]. No-op when there are
     * no hits, when the frequencies came from a History re-run, when no repository is
     * wired (JVM tests), or when this run was already saved.
     */
    private fun persistRunIfNeeded(hits: List<ScanResult>) {
        if (hits.isEmpty() || fromReRun || savedThisRun) return
        val repository = runHistory ?: return
        savedThisRun = true
        val params = activeParameters
        val generatorLabel = _state.value.generator?.activeLabel()
        val record = RunRecord(
            id = "",
            timestampMs = System.currentTimeMillis(),
            generatorLabel = generatorLabel,
            startFrequency = params?.startFrequency ?: 0.0,
            endFrequency = params?.endFrequency ?: 0.0,
            dwellSeconds = params?.dwellSeconds ?: 0.0,
            targetAmplitudeCv = params?.targetAmplitudeCv ?: 0,
            hits = hits.map { RunHit(frequency = it.frequency, deviation = it.deviation) },
        )
        viewModelScope.launch {
            runCatching { repository.save(record) }
                .onSuccess { saved -> log.i(TAG, "Saved run ${saved.id}: ${saved.hits.size} frequencies") }
                .onFailure { error -> log.e(TAG, "Failed to save run: ${error.message}") }
        }
    }

    /**
     * Map each detected hit to its sweep-step index so the graph can draw a FINAL
     * (solid red) marker aligned to [HuntUiState.fullHistory]. Uses the same step
     * frequencies the sweep produced; ties resolve to the first matching step.
     */
    private fun finalMarkers(hits: List<ScanResult>): List<GraphMarker> {
        if (hits.isEmpty()) return emptyList()
        val parameters = activeParameters ?: return emptyList()
        val frequencies = com.spooky2.huntkill.core.scan.ScanEngine.calculateFrequencySteps(parameters)
        // Build a freq -> first step-index map once (O(N)); hits is tiny.
        val freqToStep = HashMap<Double, Int>(frequencies.size)
        for (i in frequencies.indices) freqToStep.putIfAbsent(frequencies[i], i)
        return hits.mapNotNull { hit ->
            val step = freqToStep[hit.frequency] ?: return@mapNotNull null
            GraphMarker(step, hit.frequency, hit.deviation, isFinal = true)
        }
    }

    /**
     * Surface the [HuntPhase.HitsReadyWithDropouts] warning state and log the
     * detected segments with their frequency ranges. No kill happens until the
     * user chooses "Re-scan affected segments" or "Continue anyway".
     */
    private fun surfaceDropouts(outcome: ScanOutcome, parameters: ScanParameters) {
        stopElapsedTicker()
        val pct = (outcome.invalidFraction * 100)
        log.w(
            TAG,
            "Dropout detected: ${outcome.segments.size} segment(s), " +
                "~${"%.1f".format(pct)}% of sweep flagged",
        )
        outcome.segments.forEachIndexed { i, seg ->
            log.w(
                TAG,
                "  dropout[$i] steps ${seg.startStep}..${seg.endStep} " +
                    "(${"%.2f".format(seg.startFrequency)}–${"%.2f".format(seg.endFrequency)} Hz)",
            )
        }
        _state.update {
            it.copy(
                phase = HuntPhase.HitsReadyWithDropouts,
                hits = outcome.hits,
                statusText = "Connection dropped during ${outcome.segments.size} segment(s)",
                isPaused = false,
                busyAction = null,
            )
        }
    }

    /** Run the kill phase for [hits], then publish Done + reverse lookup. */
    private suspend fun proceedToKill(
        session: GeneratorSession,
        parameters: ScanParameters,
        hits: List<ScanResult>,
        cycle: Int,
    ) {
        var currentHits = hits
        var generation = cycle
        // Params of the generation that found [currentHits]; refinement halves its step.
        var huntParams = parameters

        while (currentHits.isNotEmpty()) {
            session.engine.killHits(
                currentHits,
                huntParams,
                { progress -> onProgress(progress, huntParams) },
                pauseGate,
                generation,
                killControl,
                // Read live so a mid-kill repeat toggle is honored at the next pass-end.
                // With repeat ON this call does not return until the user turns repeat off
                // (current pass finishes) or cancels — so HuntPhase.Done is reached then.
                // Refinement supersedes repeat: one pass per generation, then refine.
                repeatEnabled = { repeatKillFlag.value && !refineFlag.value },
            )

            // ── Refinement generations (original "Continue Refining Hits") ──
            // Checked at each pass boundary so a mid-kill toggle is honored live.
            if (!refineFlag.value) break
            kotlin.coroutines.coroutineContext.ensureActive()

            generation++
            // Window half-width basis = the ORIGINAL gen-1 parameters (constant width
            // across generations); sweep step = previous generation's step halved.
            val plan = RefinementPlanner.planNextGeneration(currentHits, huntParams, parameters)
            val freqs = RefinementPlanner.frequencyStepsFor(plan, huntParams)
            if (freqs.isEmpty()) {
                log.i(TAG, "Refinement gen $generation: no scannable window — stopping")
                break
            }
            val scanParams = huntParams.copy(
                stepSizeHz = plan.nextStepSizeHz,
                stepSizePercent = plan.nextStepSizePercent,
            )
            log.i(
                TAG,
                "Refinement gen $generation: ${plan.windows.size} window(s), " +
                    "${freqs.size} steps at ${"%.5f".format(plan.nextStepSizePercent)}%",
            )
            _state.update {
                it.copy(
                    refineGeneration = generation,
                    statusText = "Refining (cycle $generation) — scanning…",
                )
            }

            // Narrow-window sweep. The UI stays on the Kill screen (phase unchanged);
            // [refinementSweepActive] keeps these readings off the gen-1 live graph.
            // Dropout segments are not surfaced mid-refinement (the gen-1 dropout flow
            // already gated the run); a dropped refinement read just weakens that hit.
            refinementSweepActive = true
            val outcome = try {
                session.engine.runBiofeedbackScanDetailed(
                    scanParams,
                    { progress -> onProgress(progress, scanParams) },
                    pauseGate,
                    freqs,
                )
            } finally {
                refinementSweepActive = false
            }

            currentHits = outcome.hits
            huntParams = scanParams
            if (currentHits.isEmpty()) {
                log.i(TAG, "Refinement gen $generation: no hits — refinement complete")
                break
            }
            log.i(TAG, "Refinement gen $generation: ${currentHits.size} refined hits")
            // Show the refined hits on the Kill screen (gen-1 graph/markers are kept —
            // refined frequencies don't map onto the full-range step grid).
            _state.update { it.copy(hits = currentHits) }
            runReverseLookup(currentHits, _state.value.lookupTolerancePercent)
        }
        session.engine.finishHuntAndKill(parameters)

        log.i(TAG, "Hunt complete — ${currentHits.size} hits (generation $generation)")
        currentHits.forEachIndexed { index, hit ->
            log.i(TAG, "  hit[$index] freq=${"%.2f".format(hit.frequency)} deviation=${hit.deviation}")
        }
        stopElapsedTicker()
        val finalHits = currentHits.ifEmpty { _state.value.hits.ifEmpty { hits } }
        _state.update {
            it.copy(
                phase = HuntPhase.Done,
                hits = finalHits,
                statusText = "Hunt & Kill complete — ${finalHits.size} hits",
                killDwellRemainingSeconds = 0,
                isPaused = false,
                busyAction = null,
            )
        }
        // Lookup was already started in publishSweepOutcome when hits were first found.
        // Only re-run here if the hit set changed (e.g. after a re-scan merge changed hits)
        // or if results are still absent for some reason (e.g. DB load failed earlier).
        val currentState = _state.value
        val hitsChanged = currentState.lookupResults.keys != finalHits.map { it.frequency }.toSet()
        if (currentState.lookupResults.isEmpty() || hitsChanged) {
            runReverseLookup(finalHits, currentState.lookupTolerancePercent)
        }
    }

    /**
     * "Continue anyway": skip the re-scan and kill using the hits computed with
     * the invalid steps already excluded. No-op outside the dropout-warning state.
     */
    fun continueAnyway() {
        if (_state.value.phase != HuntPhase.HitsReadyWithDropouts) return
        val session = sessionHolder.current() ?: return
        val parameters = activeParameters ?: return
        val hits = _state.value.hits

        log.i(TAG, "User chose Continue anyway — killing ${hits.size} hits (dropouts ignored)")
        pauseGate.resume()
        _state.update { it.copy(phase = HuntPhase.Killing, busyAction = null, isPaused = false) }
        startElapsedTicker()
        huntJob = viewModelScope.launch(Dispatchers.Default) {
            runCatching { proceedToKill(session, parameters, hits, cycle = 1) }
                .onFailure { error ->
                    if (error is kotlinx.coroutines.CancellationException) throw error
                    log.e(TAG, "Kill failed: ${error.message}")
                    stopElapsedTicker()
                    safetyStopInternal(session)
                    _state.update {
                        it.copy(phase = HuntPhase.Error, errorMessage = error.message ?: "Kill failed", busyAction = null)
                    }
                }
        }
    }

    /**
     * Re-run a treatment from a saved run's frequencies (History → "Re-run treatment").
     * Treats [freqs] again via the KILL phase only — no new hunt — on the currently
     * connected generator, driving the same Kill screen (progress, jump, reverse lookup,
     * zero-on-cancel) as a fresh hunt. The synthesized hits carry the saved [deviations]
     * (aligned by index, 0.0 when absent) and a zero reading; only their frequency is
     * treated. Requires a connected session — returns false (no state change) otherwise.
     *
     * Re-runs are intentionally NOT persisted again (they treat already-saved
     * frequencies), so [fromReRun] is set to suppress the save trigger.
     */
    fun startKillFromFrequencies(
        freqs: List<Double>,
        dwellSeconds: Double,
        amplitudeCv: Int,
        deviations: List<Double> = emptyList(),
    ): Boolean {
        if (huntJob?.isActive == true) return false
        if (freqs.isEmpty()) return false
        val session = sessionHolder.current() ?: run {
            log.e(TAG, "Re-run blocked: not connected")
            return false
        }

        val parameters = ScanParameters(
            startFrequency = freqs.min(),
            endFrequency = freqs.max(),
            dwellSeconds = dwellSeconds,
            targetAmplitudeCv = amplitudeCv,
            continueRefining = false,
        )
        val hits = freqs.mapIndexed { index, freq ->
            ScanResult(
                frequency = freq,
                reading = 0.0,
                runningAverage = 0.0,
                deviation = deviations.getOrElse(index) { 0.0 },
                hitCount = 1,
            )
        }

        log.i(TAG, "Re-run: treating ${hits.size} saved frequencies (dwell=${dwellSeconds}s, ampCv=$amplitudeCv)")
        activeParameters = parameters
        lastOutcome = null
        // Re-run: do NOT persist these frequencies again.
        fromReRun = true
        savedThisRun = true
        pauseGate.resume()
        _state.update {
            it.copy(
                phase = HuntPhase.Killing,
                statusText = "Re-running ${hits.size} frequencies…",
                hits = hits,
                graphMarkers = emptyList(),
                fullHistory = FloatArray(0),
                historyValid = BooleanArray(0),
                dropoutSegments = emptyList(),
                killIndex = 0,
                killTotal = hits.size,
                killDwellRemainingSeconds = 0,
                isPaused = false,
                elapsedSeconds = 0,
                estimatedRemainingSeconds = 0,
                errorMessage = null,
                lookupResults = emptyMap(),
                busyAction = null,
            )
        }
        // Surface reverse-lookup matches for the re-run frequencies too.
        runReverseLookup(hits, _state.value.lookupTolerancePercent)
        startElapsedTicker()
        huntJob = viewModelScope.launch(Dispatchers.Default) {
            runCatching { proceedToKill(session, parameters, hits, cycle = 1) }
                .onFailure { error ->
                    if (error is kotlinx.coroutines.CancellationException) throw error
                    log.e(TAG, "Re-run kill failed: ${error.message}")
                    stopElapsedTicker()
                    safetyStopInternal(session)
                    _state.update {
                        it.copy(phase = HuntPhase.Error, errorMessage = error.message ?: "Re-run failed", busyAction = null)
                    }
                }
        }
        return true
    }

    /**
     * "Re-scan affected segments": re-sweep the flagged segments (buffered), splice
     * the fresh readings over the old, recompute hits, then proceed to kill with the
     * merged hits. If the re-scan itself fails (cable still bad), surface the error
     * and re-offer the dropout choice so the user can retry.
     */
    fun rescanAffectedSegments() {
        if (_state.value.phase != HuntPhase.HitsReadyWithDropouts) return
        val session = sessionHolder.current() ?: return
        val parameters = activeParameters ?: return
        val outcome = lastOutcome ?: return

        log.i(TAG, "User chose Re-scan — re-sweeping ${outcome.segments.size} segment(s)")
        pauseGate.resume()
        _state.update { it.copy(phase = HuntPhase.Hunting, rescanInProgress = true, isPaused = false, errorMessage = null) }
        startElapsedTicker()
        huntJob = viewModelScope.launch(Dispatchers.Default) {
            runCatching {
                val merged = session.engine.rescanSegments(
                    parameters,
                    outcome.segments,
                    outcome,
                    onProgress = { progress -> onProgress(progress, parameters) },
                    pauseGate = pauseGate,
                )
                lastOutcome = merged
                log.i(
                    TAG,
                    "Re-scan merged: ${merged.segments.size} dropout(s) remain, ${merged.hits.size} hits",
                )
                publishSweepOutcome(merged)
                _state.update { it.copy(rescanInProgress = false, hits = merged.hits) }
                proceedToKill(session, parameters, merged.hits, cycle = 1)
            }.onFailure { error ->
                if (error is kotlinx.coroutines.CancellationException) throw error
                log.e(TAG, "Re-scan failed: ${error.message}")
                stopElapsedTicker()
                // Re-offer the dropout choice so the user can retry the re-scan.
                _state.update {
                    it.copy(
                        phase = HuntPhase.HitsReadyWithDropouts,
                        rescanInProgress = false,
                        isPaused = false,
                        errorMessage = error.message ?: "Re-scan failed — cable may still be unstable",
                        busyAction = null,
                    )
                }
            }
        }
    }

    private fun onProgress(progress: ScanProgress, parameters: ScanParameters) {
        val isKill = progress.statusText.startsWith("Killing")
        if (isKill && _state.value.phase != HuntPhase.Killing) {
            val count = sessionHolder.current()?.engine?.lastResults()?.size ?: 0
            log.i(TAG, "Entering kill phase — $count hits to treat")
        }
        if (isKill) {
            log.d(
                TAG,
                "Kill ${progress.stepNumber}/${progress.totalSteps} " +
                    "freq=${"%.2f".format(progress.currentFrequency)}",
            )
        }
        // Grow the live per-step history during the MAIN sweep only (not kill, not the
        // segment re-scan, which publishes its own merged outcome). The main sweep is
        // the only path that produces provisional hits, so gate on that.
        // Refinement-generation sweeps run on a narrow per-hit grid that does not align
        // with the gen-1 full-range graph — keep their readings off the live history.
        val isMainSweep = !isKill && !progress.statusText.startsWith("Re-scanning") &&
            !refinementSweepActive
        // Lead-in length: the first `raWindow` sweep steps are the unsettled lead-in
        // (the amplitude is still physically settling from the ramp and the detection
        // SMA window has not warmed up). Their low/rising readings are real on the wire
        // but would stretch the live graph's Y-scale (a leading vertical spike at index 0)
        // and trigger false "rising" provisional peaks at the far left. We mark them
        // INVALID for DISPLAY only — detection is unchanged (the engine's ScanOutcome
        // drives the final published history/markers). The mask keeps indices aligned so
        // later markers still map to the correct sweep-step X.
        val leadIn = leadInSteps(parameters)
        val liveSnapshot: FloatArray? = if (isMainSweep && progress.currentReading != 0.0) {
            // stepNumber is 1-based; append in order. Defensive against a missed step.
            val idx = (progress.stepNumber - 1).coerceAtLeast(liveHistory.size)
            while (liveHistory.size <= idx) liveHistory.add(progress.currentReading.toFloat())
            liveHistory[idx] = progress.currentReading.toFloat()
            liveHistory.toFloatArray()
        } else {
            null
        }
        val provisionalMarkers: List<GraphMarker>? = if (isMainSweep) {
            // Drop provisional hits that land in the lead-in region so there are no
            // false red dots bunched at the far left while the signal is still settling.
            progress.provisionalHits
                .filter { it.stepIndex >= leadIn }
                .map { GraphMarker(it.stepIndex, it.frequency, it.deviation, isFinal = false) }
        } else {
            null
        }

        _state.update { current ->
            val newHistory = if (progress.currentReading != 0.0 && !isKill) {
                (current.angleHistory + progress.currentReading).takeLast(MAX_HISTORY)
            } else {
                current.angleHistory
            }
            current.copy(
                phase = if (isKill) HuntPhase.Killing else current.phase.coerceHunting(),
                busyAction = null,
                statusText = progress.statusText,
                currentFrequency = progress.currentFrequency,
                amplitudeCv = if (progress.amplitudeCv != 0) progress.amplitudeCv else current.amplitudeCv,
                currentReading = if (progress.currentReading != 0.0) progress.currentReading else current.currentReading,
                runningAverage = if (progress.currentRunningAverage != 0.0) {
                    progress.currentRunningAverage
                } else {
                    current.runningAverage
                },
                percentComplete = progress.percentComplete,
                angleHistory = newHistory,
                fullHistory = liveSnapshot ?: current.fullHistory,
                historyValid = if (liveSnapshot != null) {
                    // Lead-in steps are marked invalid so ScanGraph excludes them from
                    // the Y-scale and draws a gap (instead of a leading vertical spike).
                    // Real read-failure dropouts during the live sweep are surfaced after
                    // post-processing via publishSweepOutcome; the live mask only encodes
                    // the display lead-in.
                    BooleanArray(liveSnapshot.size) { it >= leadIn }
                } else {
                    current.historyValid
                },
                totalSweepSteps = if (isMainSweep && progress.totalSteps > 0) {
                    progress.totalSteps
                } else {
                    current.totalSweepSteps
                },
                graphMarkers = provisionalMarkers ?: current.graphMarkers,
                hits = if (progress.hitsFound > 0 && current.hits.isEmpty()) {
                    // Hits become known once a kill cycle starts; pull them from the engine.
                    sessionHolder.current()?.engine?.lastResults() ?: current.hits
                } else {
                    current.hits
                },
                killIndex = if (isKill) progress.stepNumber else current.killIndex,
                killTotal = if (isKill) progress.totalSteps else current.killTotal,
                killDwellRemainingSeconds = if (isKill) {
                    progress.killDwellRemainingSeconds
                } else {
                    current.killDwellRemainingSeconds
                },
                estimatedRemainingSeconds = if (!isKill) {
                    estimateRemaining(current.elapsedSeconds, progress.percentComplete)
                } else {
                    current.estimatedRemainingSeconds
                },
            )
        }
    }

    /**
     * Estimate sweep seconds remaining from elapsed time and percent complete, assuming
     * a roughly constant step rate. Returns 0 until there is enough progress to be
     * meaningful (avoids a wild estimate in the first instants).
     */
    private fun estimateRemaining(elapsedSeconds: Int, percentComplete: Double): Int {
        if (elapsedSeconds <= 0 || percentComplete < 1.0 || percentComplete >= 100.0) return 0
        val total = elapsedSeconds / (percentComplete / 100.0)
        return (total - elapsedSeconds).coerceAtLeast(0.0).toInt()
    }

    private fun HuntPhase.coerceHunting(): HuntPhase =
        if (this == HuntPhase.Killing || this == HuntPhase.Done) this else HuntPhase.Hunting

    /**
     * Number of leading SWEEP steps excluded from the LIVE graph display. These are
     * the unsettled lead-in readings: the amplitude is still physically settling after
     * the ramp and the detection SMA window has not warmed up, so they read low/rising
     * before the curve flattens. Equal to one RA window ([ScanParameters.raWindow]),
     * clamped to >= 0. Display-only — does not affect detection or the final published
     * [ScanOutcome] history/markers.
     */
    private fun leadInSteps(parameters: ScanParameters): Int =
        parameters.raWindow.coerceAtLeast(0)

    /**
     * Return a DISPLAY copy of [sweepValid] with the leading lead-in steps additionally
     * marked invalid, so the final graph excludes the unsettled lead-in just like the live
     * view. The input array is never mutated (the engine [ScanOutcome] keeps the true
     * detection mask). Falls through unchanged when no active parameters are known.
     */
    private fun displayValidWithLeadIn(sweepValid: BooleanArray): BooleanArray {
        val leadIn = activeParameters?.let { leadInSteps(it) } ?: 0
        if (leadIn <= 0) return sweepValid
        val copy = sweepValid.copyOf()
        val end = leadIn.coerceAtMost(copy.size)
        for (i in 0 until end) copy[i] = false
        return copy
    }

    /**
     * Toggle pause/resume on the running hunt. While paused the engine holds at the
     * current frequency (no new commands), the sweep progress and kill countdown
     * freeze, and the elapsed clock stops advancing.
     */
    fun togglePause() {
        val phase = _state.value.phase
        if (phase != HuntPhase.Hunting && phase != HuntPhase.Killing) return

        val nowPaused = !_state.value.isPaused
        if (nowPaused) pauseGate.pause() else pauseGate.resume()
        log.i(TAG, if (nowPaused) "Hunt paused (hold)" else "Hunt resumed")
        _state.update { it.copy(isPaused = nowPaused) }
    }

    /**
     * "Treat this now": ask the running kill to immediately jump to hit [index]
     * (0-based) and continue treating from there onward. No-op outside the kill
     * phase. The engine applies the jump on its next dwell slice (after resuming
     * if paused); the kill index/countdown then update via engine progress.
     */
    fun jumpToHit(index: Int) {
        if (_state.value.phase != HuntPhase.Killing) return
        if (index !in _state.value.hits.indices) return
        log.i(TAG, "Jump to hit $index requested (treat now)")
        killControl.requestJump(index)
    }

    /** 1s ticker: advances elapsedSeconds only while running and not paused. */
    private fun startElapsedTicker() {
        elapsedTicker?.cancel()
        elapsedTicker = viewModelScope.launch {
            while (true) {
                delay(1000)
                val s = _state.value
                val running = s.phase == HuntPhase.Hunting || s.phase == HuntPhase.Killing
                if (running && !s.isPaused) {
                    _state.update { it.copy(elapsedSeconds = it.elapsedSeconds + 1) }
                }
            }
        }
    }

    private fun stopElapsedTicker() {
        elapsedTicker?.cancel()
        elapsedTicker = null
    }

    /** Cancel the running scan coroutine and run the safety-stop path. */
    fun cancel() {
        log.w(TAG, "Cancel requested — running safety stop")
        pauseGate.resume()
        stopElapsedTicker()
        huntJob?.cancel()
        huntJob = null
        _state.update { it.copy(busyAction = "Cancelling…") }
        viewModelScope.launch {
            // Capture the session INSIDE the coroutine so a concurrent generator switch
            // cannot race us: we zero the session that is current at cancellation time,
            // not the one that was current when cancel() was first called.
            val session = sessionHolder.current()
            session?.let { safetyStopInternal(it) }
            _state.update {
                it.copy(
                    phase = HuntPhase.Cancelled,
                    statusText = "Cancelled — generator zeroed",
                    killDwellRemainingSeconds = 0,
                    isPaused = false,
                    rescanInProgress = false,
                    busyAction = null,
                )
            }
            _events.trySend("Generator zeroed")
            // If the cancel happened after hits were already detected, still surface
            // their reverse-lookup matches (results not yet computed for this run).
            val hits = _state.value.hits
            if (hits.isNotEmpty() && _state.value.lookupResults.isEmpty()) {
                runReverseLookup(hits, _state.value.lookupTolerancePercent)
            }
        }
    }

    /** Explicit safety-stop button: zero the generator output immediately. */
    fun safetyStop() = cancel()

    /**
     * Reset a terminal phase (Done/Cancelled/Error) back to Idle when the user returns
     * to the Hunt config screen, so the config UI isn't stuck showing a finished run.
     * Keeps params + generator info. No-op while a run is active.
     */
    fun prepareForConfig() {
        refreshGeneratorInfo()
        if (_state.value.isRunning) return
        _state.update {
            it.copy(
                phase = HuntPhase.Idle,
                statusText = "",
                errorMessage = null,
                killDwellRemainingSeconds = 0,
            )
        }
    }

    /**
     * Zero the generator: clear both frequency channels, set amplitude CV to 0 on both
     * outputs, then stop outputs. Delegates to [GeneratorSession.client] zeroOutput,
     * which is itself fully guarded so it never throws even on a half-open transport.
     */
    private suspend fun safetyStopInternal(session: GeneratorSession) {
        runCatching { session.client.zeroOutput() }
    }

    /**
     * Re-run reverse lookup at a different [tolerancePercent] from the Hits screen.
     * Re-uses the already-loaded database, so this is fast; cancels any in-flight lookup.
     * No-op when there are no hits.
     */
    fun setLookupTolerance(tolerancePercent: Double) {
        if (_state.value.lookupTolerancePercent == tolerancePercent && _state.value.lookupResults.isNotEmpty()) {
            return
        }
        val hits = _state.value.hits
        _state.update { it.copy(lookupTolerancePercent = tolerancePercent) }
        if (hits.isEmpty()) return
        runReverseLookup(hits, tolerancePercent)
    }

    /**
     * Match each [hits] frequency against the bundled frequency database and store the
     * results in [HuntUiState.lookupResults]. Runs on [Dispatchers.Default] off the UI
     * thread; the database load itself happens on IO inside the repository. Cancellation
     * of [lookupJob] (re-run with a new tolerance, or VM cleared) is honoured between hits.
     */
    private fun runReverseLookup(hits: List<ScanResult>, tolerancePercent: Double) {
        val repository = frequencyDatabase ?: return
        if (hits.isEmpty()) return

        lookupJob?.cancel()
        _state.update { it.copy(lookupBusy = true) }
        lookupJob = viewModelScope.launch(Dispatchers.Default) {
            val database = runCatching { repository.database() }.getOrElse { error ->
                if (error is kotlinx.coroutines.CancellationException) throw error
                log.e(TAG, "Reverse lookup DB load failed: ${error.message}")
                _state.update { it.copy(lookupBusy = false) }
                return@launch
            }

            val params = ReverseLookupParameters(tolerancePercent = tolerancePercent)
            val results = LinkedHashMap<Double, List<LookupMatch>>(hits.size)
            for (hit in hits) {
                coroutineContext.ensureActive()
                results[hit.frequency] = ReverseLookup.lookup(hit.frequency, database.entries, params)
            }

            logLookupSummary(tolerancePercent, hits, results)
            _state.update { it.copy(lookupResults = results, lookupBusy = false) }
        }
    }

    /**
     * On-demand reverse lookup for a SINGLE frequency — used by the graph marker
     * popup, where a provisional (live) hit may not yet be in [HuntUiState.lookupResults].
     * Returns the matches (empty list = no matches) or null when the database source is
     * unavailable (JVM tests) or the load failed. Runs off the UI thread; cancellation-safe.
     */
    suspend fun lookupForFrequency(
        frequency: Double,
        tolerancePercent: Double = _state.value.lookupTolerancePercent,
    ): List<LookupMatch>? {
        _state.value.lookupResults[frequency]?.let { return it }
        val repository = frequencyDatabase ?: return null
        return withContext(Dispatchers.Default) {
            val database = runCatching { repository.database() }.getOrElse { error ->
                if (error is kotlinx.coroutines.CancellationException) throw error
                log.e(TAG, "Marker lookup DB load failed: ${error.message}")
                return@withContext null
            }
            val params = ReverseLookupParameters(tolerancePercent = tolerancePercent)
            ReverseLookup.lookup(frequency, database.entries, params)
        }
    }

    /** Emit a per-hit summary so the matches land in the daily log files / ZIP export. */
    private fun logLookupSummary(
        tolerancePercent: Double,
        hits: List<ScanResult>,
        results: Map<Double, List<LookupMatch>>,
    ) {
        log.i(TAG, "Reverse lookup @ ${tolerancePercent}% tolerance — ${hits.size} hits:")
        hits.forEach { hit ->
            val matches = results[hit.frequency].orEmpty()
            val top = matches.take(LOOKUP_SUMMARY_TOP)
                .joinToString("; ") { it.toReportLine() }
                .ifEmpty { "No matches" }
            val more = if (matches.size > LOOKUP_SUMMARY_TOP) " (+${matches.size - LOOKUP_SUMMARY_TOP} more)" else ""
            log.i(TAG, "  ${"%.2f".format(hit.frequency)} Hz → $top$more")
        }
    }

    /**
     * Close the active session and reset to a disconnected state. Used by the
     * "Disconnect" action on the post-run summary before navigating to Connect.
     */
    fun disconnect() {
        log.i(TAG, "Disconnect requested")
        pauseGate.resume()
        stopElapsedTicker()
        huntJob?.cancel()
        huntJob = null
        _state.update { it.copy(busyAction = "Disconnecting…") }
        viewModelScope.launch {
            runCatching { sessionHolder.clear() }
            _state.update { HuntUiState(params = it.params) }
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopElapsedTicker()
        huntJob?.cancel()
        lookupJob?.cancel()
    }

    companion object {
        private const val MAX_HISTORY = 200
        private const val TAG = "Hunt"

        /** How many matches per hit are written to the log summary. */
        private const val LOOKUP_SUMMARY_TOP = 5
    }
}
