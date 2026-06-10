package com.spooky2.huntkill.ui.hunt

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spooky2.huntkill.core.model.ScanParameters
import com.spooky2.huntkill.core.model.ScanProgress
import com.spooky2.huntkill.core.model.ScanResult
import com.spooky2.huntkill.data.SessionHolder
import com.spooky2.huntkill.log.LogBus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
    // Demo dwell is short (3s) so the kill countdown is visible but the replayed flow
    // finishes in seconds. Real hardware uses the 180s ScanParameters default.
    val dwellSecondsText: String = "3",
    val targetAmplitudeCvText: String = "2000",
) {
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
    val errorMessage: String? = null,
)

@HiltViewModel
class HuntViewModel @Inject constructor(
    private val sessionHolder: SessionHolder,
    private val log: LogBus,
) : ViewModel() {

    private val _state = MutableStateFlow(HuntUiState())
    val state: StateFlow<HuntUiState> = _state.asStateFlow()

    private var huntJob: Job? = null

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
        _state.update {
            it.copy(
                phase = HuntPhase.Hunting,
                statusText = "Starting hunt...",
                angleHistory = emptyList(),
                hits = emptyList(),
                killIndex = 0,
                killTotal = 0,
                killDwellRemainingSeconds = 0,
                errorMessage = null,
            )
        }

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
                session.engine.runHuntAndKill(parameters) { progress ->
                    onProgress(progress, parameters)
                }
            }.onSuccess { hits ->
                log.i(TAG, "Hunt complete — ${hits.size} hits")
                hits.forEachIndexed { index, hit ->
                    log.i(
                        TAG,
                        "  hit[$index] freq=${"%.2f".format(hit.frequency)} deviation=${hit.deviation}",
                    )
                }
                _state.update {
                    it.copy(
                        phase = HuntPhase.Done,
                        hits = if (it.hits.isNotEmpty()) it.hits else hits,
                        statusText = "Hunt & Kill complete — ${hits.size} hits",
                        killDwellRemainingSeconds = 0,
                    )
                }
            }.onFailure { error ->
                if (error is kotlinx.coroutines.CancellationException) throw error
                log.e(TAG, "Scan failed: ${error.message}")
                safetyStopInternal(session)
                _state.update {
                    it.copy(
                        phase = HuntPhase.Error,
                        errorMessage = error.message ?: "Scan failed",
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
                    parameters.dwellSeconds.toInt()
                } else {
                    current.killDwellRemainingSeconds
                },
            )
        }
    }

    private fun HuntPhase.coerceHunting(): HuntPhase =
        if (this == HuntPhase.Killing || this == HuntPhase.Done) this else HuntPhase.Hunting

    /** Cancel the running scan coroutine and run the safety-stop path. */
    fun cancel() {
        log.w(TAG, "Cancel requested — running safety stop")
        val session = sessionHolder.current()
        huntJob?.cancel()
        huntJob = null
        viewModelScope.launch {
            session?.let { safetyStopInternal(it) }
            _state.update {
                it.copy(
                    phase = HuntPhase.Cancelled,
                    statusText = "Cancelled — output stopped",
                    killDwellRemainingSeconds = 0,
                )
            }
        }
    }

    /** Explicit safety-stop button: stop generator output immediately. */
    fun safetyStop() = cancel()

    private suspend fun safetyStopInternal(session: com.spooky2.huntkill.data.GeneratorSession) {
        runCatching { session.client.stop() }
    }

    override fun onCleared() {
        super.onCleared()
        huntJob?.cancel()
    }

    companion object {
        private const val MAX_HISTORY = 200
        private const val TAG = "Hunt"
    }
}
