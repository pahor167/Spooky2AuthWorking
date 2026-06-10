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
import com.spooky2.huntkill.core.scan.PauseGate
import com.spooky2.huntkill.data.FrequencyDatabaseSource
import com.spooky2.huntkill.data.GeneratorSession
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

/** Connection summary shown as an info chip on the Hunt config screen. */
data class GeneratorInfo(
    val generatorType: String,
    val baudRate: Int,
    /** 0-based port index of the active generator, or null for test replay sessions. */
    val portIndex: Int?,
    /** Total selectable ports on the device, or null for test replay sessions. */
    val portCount: Int?,
) {
    /** True when the device exposes more than one generator port (switcher shown). */
    val hasMultiplePorts: Boolean get() = (portCount ?: 1) > 1
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
    /** True while a segment re-scan is running (drives the warning-card spinner). */
    val rescanInProgress: Boolean = false,
    val hits: List<ScanResult> = emptyList(),
    val killIndex: Int = 0,
    val killTotal: Int = 0,
    val killDwellRemainingSeconds: Int = 0,
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

    /** Last sweep outcome (readings + validity + segments); spliced by a re-scan. */
    private var lastOutcome: ScanOutcome? = null

    init {
        refreshGeneratorInfo()
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
                _events.trySend("Switched to Generator ${portIndex + 1}")
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
        _state.update {
            it.copy(
                phase = HuntPhase.Hunting,
                statusText = "Starting hunt...",
                angleHistory = emptyList(),
                hits = emptyList(),
                killIndex = 0,
                killTotal = 0,
                killDwellRemainingSeconds = 0,
                isPaused = false,
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
            val parameters = _state.value.params.toScanParameters(isDemo = session.isDemo)
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

    /** Store the full reading history + dropout diagnostics into UI state. */
    private fun publishSweepOutcome(outcome: ScanOutcome) {
        _state.update {
            it.copy(
                fullHistory = outcome.sweepReadings,
                historyValid = outcome.sweepValid,
                dropoutSegments = outcome.segments,
                totalSweepSteps = outcome.sweepReadings.size,
            )
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
        if (hits.isNotEmpty()) {
            session.engine.killHits(
                hits,
                parameters,
                { progress -> onProgress(progress, parameters) },
                pauseGate,
                cycle,
            )
        }
        session.engine.finishHuntAndKill(parameters)

        log.i(TAG, "Hunt complete — ${hits.size} hits")
        hits.forEachIndexed { index, hit ->
            log.i(TAG, "  hit[$index] freq=${"%.2f".format(hit.frequency)} deviation=${hit.deviation}")
        }
        stopElapsedTicker()
        val finalHits = _state.value.hits.ifEmpty { hits }
        _state.update {
            it.copy(
                phase = HuntPhase.Done,
                hits = finalHits,
                statusText = "Hunt & Kill complete — ${hits.size} hits",
                killDwellRemainingSeconds = 0,
                isPaused = false,
                busyAction = null,
            )
        }
        // Reverse lookup runs AFTER Done is published, so it never delays the
        // kill flow, zeroing, or navigation. Cancellation-safe and off the UI.
        runReverseLookup(finalHits, _state.value.lookupTolerancePercent)
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
        val session = sessionHolder.current()
        pauseGate.resume()
        stopElapsedTicker()
        huntJob?.cancel()
        huntJob = null
        _state.update { it.copy(busyAction = "Cancelling…") }
        viewModelScope.launch {
            session?.let { safetyStopInternal(it) }
            _state.update {
                it.copy(
                    phase = HuntPhase.Cancelled,
                    statusText = "Cancelled — generator zeroed",
                    killDwellRemainingSeconds = 0,
                    isPaused = false,
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
