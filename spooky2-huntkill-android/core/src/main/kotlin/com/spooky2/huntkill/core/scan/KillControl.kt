package com.spooky2.huntkill.core.scan

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.atomic.AtomicInteger

/**
 * Cooperative "jump" primitive for the kill phase of the [ScanEngine], analogous
 * to [PauseGate].
 *
 * The UI can ask the running kill to immediately jump to a specific hit index and
 * continue treating from there onward. [requestJump] stores a pending target index;
 * the kill loop calls [takeJump] once per dwell slice and, when a target is pending,
 * consumes it (returning and clearing it) so it is applied exactly once.
 *
 * Thread-safe: the pending target is held in an [AtomicInteger] so a UI-thread
 * [requestJump] and an engine-coroutine [takeJump] never race. A mirrored
 * [MutableStateFlow] exposes the latest requested target for any observer (display).
 *
 * The default instance has no pending jump, preserving back-compat for callers and
 * existing tests that don't pass a control.
 */
class KillControl {

    /** [NO_TARGET] = no pending jump; any other value is a pending 0-based hit index. */
    private val pending = AtomicInteger(NO_TARGET)

    private val _requestedTarget = MutableStateFlow<Int?>(null)

    /** Observable last-requested jump target (null once consumed or never requested). */
    val requestedTarget: StateFlow<Int?> = _requestedTarget.asStateFlow()

    /** Request the kill to jump to [index] (0-based) on the next dwell slice. */
    fun requestJump(index: Int) {
        pending.set(index)
        _requestedTarget.value = index
    }

    /**
     * Return and CLEAR any pending jump target. Returns null when no jump is
     * pending. Atomic: a concurrent [requestJump] is either fully observed here or
     * remains pending for the next call — never lost or double-applied.
     */
    fun takeJump(): Int? {
        val target = pending.getAndSet(NO_TARGET)
        if (target == NO_TARGET) return null
        _requestedTarget.value = null
        return target
    }

    companion object {
        private const val NO_TARGET = Int.MIN_VALUE
    }
}
