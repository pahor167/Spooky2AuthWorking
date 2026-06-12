package com.spooky2.huntkill.service

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Snapshot of a running Hunt & Kill for the foreground-service notification.
 * Pure data — formatting lives in [ScanForegroundService].
 */
data class RunStatus(
    /** True while the kill phase is treating; false during hunt/refinement sweeps. */
    val killing: Boolean,
    val paused: Boolean,
    /** Sweep completion 0–100 (hunt) — ignored during kill. */
    val percentComplete: Int,
    val currentFrequencyHz: Double,
    /** Kill progress: current index (1-based) / total. 0/0 outside the kill. */
    val killIndex: Int,
    val killTotal: Int,
    /** Seconds remaining in the current kill dwell. */
    val dwellRemainingSeconds: Int,
    /** Total run time, seconds. */
    val elapsedSeconds: Int,
    /** Estimated sweep time remaining, seconds (hunt only; 0 = unknown). */
    val remainingSeconds: Int,
    /** Hunt & Kill generation (1 = initial sweep, 2+ = refinement cycles). */
    val generation: Int,
)

/**
 * Singleton bridge between [HuntViewModel][com.spooky2.huntkill.ui.hunt.HuntViewModel]
 * (producer) and [ScanForegroundService] (consumer). A non-null [status] means a run
 * is active; null means no run (the service stops itself on it).
 */
@Singleton
class RunStatusBus @Inject constructor() {
    private val _status = MutableStateFlow<RunStatus?>(null)
    val status: StateFlow<RunStatus?> = _status.asStateFlow()

    fun publish(status: RunStatus) {
        _status.value = status
    }

    /** Mark the run finished; the service observes this and stops itself. */
    fun clear() {
        _status.value = null
    }
}
