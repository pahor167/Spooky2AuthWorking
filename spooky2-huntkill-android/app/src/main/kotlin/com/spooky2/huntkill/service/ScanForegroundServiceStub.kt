package com.spooky2.huntkill.service

/**
 * PHASE-5 HARDENING STUB — foreground service + wake lock for long scans.
 *
 * Hunt→Kill scans can run for many minutes (kill dwell defaults to 180s per
 * frequency). On real hardware the scan must survive the screen turning off and the
 * app being backgrounded, which on Android requires a started foreground service
 * (type `connectedDevice`) holding a partial `WakeLock` for the duration of the scan.
 *
 * This is intentionally NOT wired yet. Today the scan already survives recomposition
 * and navigation because it runs in [HuntViewModel][com.spooky2.huntkill.ui.hunt.HuntViewModel]'s
 * `viewModelScope`, which is scoped to the navigation-graph back stack entry and
 * outlives individual screens. That is sufficient for the in-app, foreground demo.
 *
 * Phase 5 work items:
 *   1. Add a `ScanForegroundService` (FGS type `connectedDevice`) started when a hunt
 *      begins and stopped on completion/cancel/error.
 *   2. Acquire a `PowerManager.PARTIAL_WAKE_LOCK` while output is active; release it in
 *      the same finally path that runs the generator safety-stop.
 *   3. Move the scan coroutine ownership into the service (or bind the ViewModel to it)
 *      so a backgrounded scan keeps the generator and wake lock alive.
 *   4. Add `FOREGROUND_SERVICE` + `FOREGROUND_SERVICE_CONNECTED_DEVICE` +
 *      `WAKE_LOCK` permissions and the `<service>` entry to the manifest.
 */
internal object ScanForegroundServiceStub
