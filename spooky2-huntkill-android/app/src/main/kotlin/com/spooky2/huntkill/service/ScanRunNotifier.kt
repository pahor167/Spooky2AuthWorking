package com.spooky2.huntkill.service

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The HuntViewModel-facing facade for the run notification: starts the foreground
 * service when a run begins, feeds it live [RunStatus] snapshots, and clears the bus
 * when the run ends (on which the service stops itself and removes the notification).
 * Injected as nullable into the ViewModel so plain-JVM tests skip it.
 */
@Singleton
class ScanRunNotifier @Inject constructor(
    @ApplicationContext private val context: Context,
    private val bus: RunStatusBus,
) {

    fun runStarted() {
        runCatching { ScanForegroundService.start(context) }
    }

    fun publish(status: RunStatus) {
        bus.publish(status)
    }

    fun runEnded() {
        bus.clear()
        // Belt and braces: the service also stops itself on the cleared bus.
        runCatching { ScanForegroundService.stop(context) }
    }
}
