package com.spooky2.huntkill.service

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The HuntViewModel-facing facade for the run notification: starts the foreground
 * service when a run begins, feeds it live [RunStatus] snapshots, and clears the bus
 * when the run ends (on which the service stops itself and removes the notification).
 * Injected as nullable into the ViewModel so plain-JVM tests skip it.
 *
 * Multi-generator: several controllers can run in parallel and each calls
 * [runStarted]/[runEnded] independently. A ref-count keeps exactly ONE foreground
 * notification alive across all concurrent runs: the service starts on the first
 * [runStarted] (0 → 1) and stops only when the last run ends ([runEnded] back to 0).
 * Without this, a second run's end would stop the service while another is still running.
 */
@Singleton
class ScanRunNotifier @Inject constructor(
    @ApplicationContext private val context: Context,
    private val bus: RunStatusBus,
) {

    /** Number of in-flight runs across all generators; gates service start/stop. */
    private val activeRuns = AtomicInteger(0)

    fun runStarted() {
        // Start the service only on the 0 → 1 transition; later concurrent runs just
        // increment the count and reuse the single notification.
        if (activeRuns.getAndIncrement() == 0) {
            runCatching { ScanForegroundService.start(context) }
        }
    }

    fun publish(status: RunStatus) {
        bus.publish(status)
    }

    fun runEnded() {
        // Stop the service only when the LAST run ends (count returns to 0). decrementAndGet
        // is clamped at 0 so an extra runEnded (idempotent terminal paths) can't go negative.
        val remaining = activeRuns.updateAndGet { current -> (current - 1).coerceAtLeast(0) }
        if (remaining == 0) {
            bus.clear()
            // Belt and braces: the service also stops itself on the cleared bus.
            runCatching { ScanForegroundService.stop(context) }
        }
    }
}
