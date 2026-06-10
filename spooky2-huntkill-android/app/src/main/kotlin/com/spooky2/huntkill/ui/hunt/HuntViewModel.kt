package com.spooky2.huntkill.ui.hunt

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spooky2.huntkill.core.model.ScanParameters
import com.spooky2.huntkill.core.model.ScanProgress
import com.spooky2.huntkill.core.model.ScanResult
import com.spooky2.huntkill.core.scan.PauseGate
import com.spooky2.huntkill.data.GeneratorSession
import com.spooky2.huntkill.data.SessionHolder
import com.spooky2.huntkill.data.UsbConnectionManager
import com.spooky2.huntkill.log.LogBus
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
enum class HuntPhase { Idle, Hunting, HitsReady, Killing, Done, Cancelled, Error }

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

    fun toScanParameters(): ScanParameters = ScanParameters(
        startFrequency = startFrequencyText.toDoubleOrNull() ?: 41000.0,
        endFrequency = endFrequencyText.toDoubleOrNull() ?: 1_800_000.0,
        dwellSeconds = dwellSecondsText.toDoubleOrNull() ?: 180.0,
        targetAmplitudeCv = targetAmplitudeCvText.toIntOrNull() ?: 2000,
        // Demo dump is post-auth with no real ramp/delay; keep the engine fast and
        // single-cycle so the replayed session reproduces the golden 10 hits.
        startDelayMs = 0,
        minReadDelaySeconds = 0.0,
        enableAmplitudeRampUp = false,
        enableAmplitudeRampDown = false,
        continueRefining = false,
    )
}

/** Connection summary shown as an info chip on the Hunt config screen. */
data class GeneratorInfo(
    val generatorType: String,
    val baudRate: Int,
    /** 0-based port index of the active generator, or null for the demo path. */
    val portIndex: Int?,
    /** Total selectable ports on the device, or null for the demo path. */
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
    val angleHistory: List<Double> = emptyList(),
    val hits: List<ScanResult> = emptyList(),
    val killIndex: Int = 0,
    val killTotal: Int = 0,
    val killDwellRemainingSeconds: Int = 0,
    val isPaused: Boolean = false,
    val elapsedSeconds: Int = 0,
    val errorMessage: String? = null,
    /** Connection summary for the config screen chip; null until connected. */
    val generator: GeneratorInfo? = null,
    /** True while a generator-port switch is in flight (disables the switcher + Start). */
    val isSwitchingGenerator: Boolean = false,
    /** Estimated whole seconds remaining in the current sweep, 0 until measurable. */
    val estimatedRemainingSeconds: Int = 0,
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
) : ViewModel() {

    private val _state = MutableStateFlow(HuntUiState())
    val state: StateFlow<HuntUiState> = _state.asStateFlow()

    /** One-shot UI messages (snackbars), e.g. "Generator zeroed". */
    private val _events = Channel<String>(Channel.BUFFERED)
    val events: Flow<String> = _events.receiveAsFlow()

    private var huntJob: Job? = null

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
     * swaps it into [SessionHolder]. No-op while a hunt is running or for the demo path.
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

        val parameters = _state.value.params.toScanParameters()
        log.i(
            TAG,
            "startHunt: start=${parameters.startFrequency} end=${parameters.endFrequency} " +
                "dwell=${parameters.dwellSeconds}s ampCv=${parameters.targetAmplitudeCv}",
        )
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
            )
        }
        startElapsedTicker()

        huntJob = viewModelScope.launch(Dispatchers.Default) {
            // Demo path rebuilds a fresh single-use replay each run (the FakeTransport
            // read pointer is consumed by a scan); the live USB path reuses the open
            // session. Both go through SessionHolder.acquireForHunt().
            val session = runCatching { sessionHolder.acquireForHunt() }.getOrElse { error ->
                log.e(TAG, "acquireForHunt failed: ${error.message}")
                _state.update {
                    it.copy(
                        phase = HuntPhase.Error,
                        errorMessage = error.message ?: "Reconnect failed",
                    )
                }
                return@launch
            }
            if (session == null) {
                log.e(TAG, "acquireForHunt returned no session")
                _state.update {
                    it.copy(phase = HuntPhase.Error, errorMessage = "Not connected. Connect first.")
                }
                return@launch
            }

            runCatching {
                session.engine.runHuntAndKill(
                    parameters,
                    { progress -> onProgress(progress, parameters) },
                    pauseGate,
                )
            }.onSuccess { hits ->
                log.i(TAG, "Hunt complete — ${hits.size} hits")
                hits.forEachIndexed { index, hit ->
                    log.i(
                        TAG,
                        "  hit[$index] freq=${"%.2f".format(hit.frequency)} deviation=${hit.deviation}",
                    )
                }
                stopElapsedTicker()
                _state.update {
                    it.copy(
                        phase = HuntPhase.Done,
                        hits = if (it.hits.isNotEmpty()) it.hits else hits,
                        statusText = "Hunt & Kill complete — ${hits.size} hits",
                        killDwellRemainingSeconds = 0,
                        isPaused = false,
                    )
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
        viewModelScope.launch {
            session?.let { safetyStopInternal(it) }
            _state.update {
                it.copy(
                    phase = HuntPhase.Cancelled,
                    statusText = "Cancelled — generator zeroed",
                    killDwellRemainingSeconds = 0,
                    isPaused = false,
                )
            }
            _events.trySend("Generator zeroed")
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
     * Close the active session and reset to a disconnected state. Used by the
     * "Disconnect" action on the post-run summary before navigating to Connect.
     */
    fun disconnect() {
        log.i(TAG, "Disconnect requested")
        pauseGate.resume()
        stopElapsedTicker()
        huntJob?.cancel()
        huntJob = null
        viewModelScope.launch {
            runCatching { sessionHolder.clear() }
            _state.update { HuntUiState(params = it.params) }
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopElapsedTicker()
        huntJob?.cancel()
    }

    companion object {
        private const val MAX_HISTORY = 200
        private const val TAG = "Hunt"
    }
}
