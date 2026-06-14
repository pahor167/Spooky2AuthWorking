package com.spooky2.huntkill.ui.hunt

import androidx.annotation.VisibleForTesting
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.max

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
 * Lightweight, observe-only summary of ONE generator's run, used to render the generator
 * tab strip without subscribing each tab to a full [HuntUiState]. Built by combining every
 * controller's state; the active tab's full state still drives the screens.
 */
data class GeneratorTabInfo(
    /** Position of this controller in the coordinator's controller list (= tab index). */
    val index: Int,
    /** Display label: the generator's serial/port label, else "Generator N". */
    val label: String,
    val phase: HuntPhase,
    val isPaused: Boolean,
    /** Seconds remaining: kill dwell while Killing, else the sweep estimate. */
    val timeLeftSeconds: Int,
)

/**
 * Multi-generator coordinator over N [GeneratorRunController]s — one per connected
 * generator. Each controller owns its OWN run state; the coordinator exposes the ACTIVE
 * controller's [state]/[events] (flipped instantly by [setActiveGenerator], no I/O) and an
 * aggregate [tabs] strip. It rebuilds the controller list as [SessionHolder.sessions]
 * changes (reusing running controllers), routes every public action to the active
 * controller, and enforces hunt mutual-exclusion (starting/resuming a hunt pauses any other
 * running hunt). Single-generator behaviour is preserved exactly: there is always at least
 * one controller, so routed calls and [state] behave as the old single-controller holder did.
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

    /**
     * One [GeneratorRunController] per connected generator, keyed by ascending registry
     * (port) order. There is ALWAYS at least one controller: when no session is connected
     * a single default controller at index 0 stands in so single-generator behaviour (and
     * the empty-holder JVM tests) work exactly as the old single-controller holder did.
     */
    private val _controllers = MutableStateFlow<List<GeneratorRunController>>(emptyList())

    /** Index of the controller whose state/events the UI currently observes (selected tab). */
    private val _activeIndex = MutableStateFlow(0)
    val activeIndex: StateFlow<Int> = _activeIndex.asStateFlow()

    /**
     * The active controller's run snapshot, re-pointed instantly on a tab switch. Eagerly
     * shared so `state.value` is always current for tests that read it synchronously right
     * after construction or a routed call.
     */
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val state: StateFlow<HuntUiState> =
        combine(_controllers, _activeIndex) { list, i -> list.getOrNull(i) }
            .flatMapLatest { it?.state ?: flowOf(HuntUiState()) }
            .stateIn(viewModelScope, SharingStarted.Eagerly, HuntUiState())

    /** One-shot UI messages (snackbars) from the ACTIVE controller, e.g. "Generator zeroed". */
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val events: Flow<String> =
        combine(_controllers, _activeIndex) { list, i -> list.getOrNull(i) }
            .flatMapLatest { it?.events ?: flowOf() }

    /**
     * Aggregate tab strip: one [GeneratorTabInfo] per controller, recomputed whenever any
     * controller's state changes. Eagerly shared with an empty initial value.
     */
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val tabs: StateFlow<List<GeneratorTabInfo>> =
        _controllers
            .flatMapLatest { list ->
                if (list.isEmpty()) {
                    flowOf(emptyList())
                } else {
                    combine(list.map { it.state }) { states ->
                        states.mapIndexed { index, s ->
                            GeneratorTabInfo(
                                index = index,
                                label = s.generator?.activeLabel() ?: "Generator ${index + 1}",
                                phase = s.phase,
                                isPaused = s.isPaused,
                                timeLeftSeconds = if (s.phase == HuntPhase.Killing) {
                                    s.killDwellRemainingSeconds
                                } else {
                                    s.estimatedRemainingSeconds
                                },
                            )
                        }
                    }
                }
            }
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    init {
        // Build the initial controller list SYNCHRONOUSLY from the registry so a session set
        // before construction yields a controller (and a current `state.value`) immediately —
        // the JVM tests construct the VM then call routed methods on the same dispatch tick.
        rebuildControllers()
        // Reactively rebuild as sessions are connected/removed; reuses running controllers.
        observeSessions()
        observeUsbDetach()
    }

    /** The controller the UI is currently routing actions to (selected tab), or null. */
    private fun activeController(): GeneratorRunController? =
        _controllers.value.getOrNull(_activeIndex.value)

    /**
     * Rebuild [_controllers] to hold one controller per registry session, keyed by ascending
     * port order. Existing controllers for ports that still exist are REUSED (never recreated
     * — a running controller must keep its job/state); controllers whose port disappeared are
     * disposed. When no session is connected, a single default controller stands in at index 0
     * so single-generator routing/state behaves exactly as before.
     */
    private fun rebuildControllers() {
        val keys = sessionHolder.sessions.value.keys.sorted()
        val previous = _controllers.value
        val rebuilt: List<GeneratorRunController>

        if (keys.isEmpty()) {
            // No connected session. PRESERVE existing controllers as-is: an empty registry
            // is normally the result of a detach/disconnect, and the controller that just
            // transitioned to Error/Idle must keep showing that terminal state (the old
            // single-controller holder survived sessionHolder.clear() the same way). Only
            // synthesize a fresh default placeholder when there is no controller at all
            // (first construction with nothing connected).
            rebuilt = previous.ifEmpty { listOf(createController(index = 0, portKey = null)) }
        } else {
            val byKey = previous.associateBy { it.portKey }
            rebuilt = keys.mapIndexed { index, key ->
                // Reuse the live controller for this port; otherwise make a fresh one.
                byKey[key]?.also { it.rebindIndex(index) }
                    ?: createController(index = index, portKey = key)
            }
            // Dispose controllers whose port vanished (including any default placeholder).
            val keptKeys = keys.toSet()
            previous.filter { it.portKey == null || it.portKey !in keptKeys }
                .forEach { it.dispose() }
        }

        _controllers.value = rebuilt
        // Keep the active index in range after the list shrinks.
        _activeIndex.value = _activeIndex.value.coerceIn(0, max(0, rebuilt.size - 1))
    }

    /**
     * Construct a controller for [portKey] (null = the single-session/default placeholder
     * resolving via SessionHolder.current()). Each controller gets a SupervisorJob child
     * scope, this generator's session getters, the shared notifier, and the mutual-exclusion
     * hook that pauses every OTHER running hunt before this one enters its sweep.
     */
    private fun createController(index: Int, portKey: Int?): GeneratorRunController {
        val childScope = CoroutineScope(viewModelScope.coroutineContext + SupervisorJob())
        return GeneratorRunController(
            index = index,
            scope = childScope,
            log = log,
            // Default placeholder resolves like the monolith (lowest registered key); a
            // keyed controller resolves exactly its own port.
            liveSession = { if (portKey == null) sessionHolder.current() else sessionHolder.get(portKey) },
            acquireSession = { sessionHolder.acquireForHunt() },
            frequencyDatabase = frequencyDatabase,
            runHistory = runHistory,
            runNotifier = runNotifier,
            // Hunt mutual-exclusion: pause every OTHER running hunt before this one sweeps.
            onAcquireHuntSlot = { pauseOtherHunts(except = index) },
        ).apply { portKey?.let { bindPortKey(it) } }
    }

    /** Collect the registry and rebuild controllers on every change (reuses running ones). */
    private fun observeSessions() {
        viewModelScope.launch {
            sessionHolder.sessions.collect { rebuildControllers() }
        }
    }

    /**
     * Switch the observed generator to tab [index]. Pure index flip — the active controller's
     * already-running state becomes visible instantly with no session I/O. Coerced into range.
     */
    fun setActiveGenerator(index: Int) {
        _activeIndex.value = index.coerceIn(0, max(0, _controllers.value.size - 1))
    }

    /**
     * Hunt mutual-exclusion: pause every controller other than [except] that is actively
     * Hunting (not Killing, not already paused). Invoked from the acquiring controller's
     * onAcquireHuntSlot hook, which suspends until this returns — guaranteeing the other
     * hunts are paused before the acquiring sweep enters live treatment.
     */
    private suspend fun pauseOtherHunts(except: Int) {
        _controllers.value.forEachIndexed { i, controller ->
            if (i == except) return@forEachIndexed
            val s = controller.state.value
            if (s.phase == HuntPhase.Hunting && !s.isPaused) {
                controller.pauseForExclusion()
            }
        }
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
                    // One physical device backs all sessions, so a detach kills them all:
                    // error every controller, then clear the registry once.
                    _controllers.value.forEach { c ->
                        c.onDeviceDetached(onSessionLost = { sessionHolder.clear() })
                    }
                } catch (e: kotlinx.coroutines.CancellationException) {
                    throw e
                } catch (e: Exception) {
                    log.e(TAG, "Detach handling failed: ${e.message}")
                }
            }
        }
    }

    /** Pull connection info from the active session into [HuntUiState.generator]. */
    fun refreshGeneratorInfo() {
        activeController()?.refreshGeneratorInfo()
    }

    fun toggleRepeatKill() {
        activeController()?.toggleRepeatKill()
    }

    fun setHitsCompactView(compact: Boolean) {
        activeController()?.setHitsCompactView(compact)
    }

    fun toggleRefineHits() {
        activeController()?.toggleRefineHits()
    }

    fun updateStartFrequency(v: String) { activeController()?.updateStartFrequency(v) }
    fun updateEndFrequency(v: String) { activeController()?.updateEndFrequency(v) }
    fun updateDwellSeconds(v: String) { activeController()?.updateDwellSeconds(v) }
    fun updateTargetAmplitude(v: String) { activeController()?.updateTargetAmplitude(v) }

    /** Launch the full Hunt→Kill flow off the main thread; collect progress into state. */
    fun startHunt() { activeController()?.startHunt() }

    /**
     * "Continue anyway": skip the re-scan and kill using the hits computed with
     * the invalid steps already excluded. No-op outside the dropout-warning state.
     */
    fun continueAnyway() { activeController()?.continueAnyway() }

    /**
     * Re-run a treatment from a saved run's frequencies (History → "Re-run treatment").
     * See [GeneratorRunController.startKillFromFrequencies].
     */
    fun startKillFromFrequencies(
        freqs: List<Double>,
        dwellSeconds: Double,
        amplitudeCv: Int,
        deviations: List<Double> = emptyList(),
    ): Boolean = activeController()?.startKillFromFrequencies(freqs, dwellSeconds, amplitudeCv, deviations) ?: false

    /**
     * "Re-scan affected segments": re-sweep the flagged segments, splice the fresh
     * readings over the old, recompute hits, then proceed to kill with the merged hits.
     */
    fun rescanAffectedSegments() { activeController()?.rescanAffectedSegments() }

    /**
     * Toggle pause/resume on the running hunt. While paused the engine holds at the
     * current frequency (no new commands), the sweep progress and kill countdown
     * freeze, and the elapsed clock stops advancing.
     */
    fun togglePause() { activeController()?.togglePause() }

    /**
     * "Treat this now": ask the running kill to immediately jump to hit [index]
     * (0-based) and continue treating from there onward. No-op outside the kill phase.
     */
    fun jumpToHit(index: Int) { activeController()?.jumpToHit(index) }

    /** Cancel the running scan coroutine and run the safety-stop path. */
    fun cancel() { activeController()?.cancel() }

    /** Explicit safety-stop button: zero the generator output immediately. */
    fun safetyStop() { activeController()?.safetyStop() }

    /**
     * Reset a terminal phase (Done/Cancelled/Error) back to Idle when the user returns
     * to the Hunt config screen, so the config UI isn't stuck showing a finished run.
     */
    fun prepareForConfig() { activeController()?.prepareForConfig() }

    /** Re-run reverse lookup at a different tolerance from the Hits screen. */
    fun setLookupTolerance(tolerancePercent: Double) { activeController()?.setLookupTolerance(tolerancePercent) }

    /**
     * On-demand reverse lookup for a SINGLE frequency — used by the graph marker popup.
     * See [GeneratorRunController.lookupForFrequency].
     */
    suspend fun lookupForFrequency(
        frequency: Double,
        tolerancePercent: Double = state.value.lookupTolerancePercent,
    ): List<LookupMatch>? = activeController()?.lookupForFrequency(frequency, tolerancePercent)

    /**
     * Close the active session and reset to a disconnected state. Used by the
     * "Disconnect" action on the post-run summary before navigating to Connect.
     */
    fun disconnect() {
        activeController()?.disconnect(onDisconnect = { sessionHolder.clear() })
    }

    /** Test-only accessor for the active controller (used to reflect on its `_state`). */
    @VisibleForTesting
    internal fun activeControllerForTest(): GeneratorRunController? = activeController()

    override fun onCleared() {
        super.onCleared()
        _controllers.value.forEach { it.dispose() }
    }

    companion object {
        private const val TAG = "Hunt"
    }
}
