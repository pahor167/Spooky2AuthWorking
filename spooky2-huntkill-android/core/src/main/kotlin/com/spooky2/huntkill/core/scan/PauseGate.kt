package com.spooky2.huntkill.core.scan

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first

/**
 * Cooperative pause primitive for the [ScanEngine], without busy-waiting.
 *
 * Holds a single paused flag. The engine calls [awaitResumed] at the top of each
 * sweep step and inside the kill-dwell loop: when paused it suspends until
 * [resume] is called (Spooky2 "Hold" semantics — the current frequency stays set
 * and no new commands are sent); when not paused it returns immediately.
 *
 * The default instance is never paused, preserving back-compat for callers and
 * existing tests that don't pass a gate.
 */
class PauseGate {

    private val _isPaused = MutableStateFlow(false)

    /** Observable paused state; the UI can drive a Pause/Resume label off this. */
    val isPaused: StateFlow<Boolean> = _isPaused.asStateFlow()

    fun pause() {
        _isPaused.value = true
    }

    fun resume() {
        _isPaused.value = false
    }

    /** Suspends while paused; returns immediately once resumed (or if never paused). */
    suspend fun awaitResumed() {
        if (!_isPaused.value) return
        _isPaused.first { !it }
    }
}
