package com.spooky2.huntkill.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import com.spooky2.huntkill.MainActivity
import com.spooky2.huntkill.R
import com.spooky2.huntkill.ui.common.asHz
import com.spooky2.huntkill.ui.common.formatElapsed
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.sample
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Foreground service keeping a Hunt & Kill run alive while the app is backgrounded
 * or the screen is off (Phase-5 hardening): an ongoing, silent notification shows
 * live progress — sweep % + current frequency during the hunt, the treated
 * frequency, dwell countdown and elapsed time during the kill.
 *
 * The scan coroutine itself still runs in HuntViewModel's scope; this service's job
 * is to (a) keep the PROCESS alive and exempt from background limits, (b) hold a
 * partial wake lock so the CPU keeps driving the USB generator with the screen off,
 * and (c) render [RunStatusBus.status] into the notification. When the bus clears
 * (run finished/cancelled/errored) the service stops itself and the notification
 * disappears.
 *
 * Known remaining gap (documented, deliberate): swiping the app away from Recents
 * still tears down the task and the ViewModel even with this service running — the
 * engine's cancellation path zeroes the generator on the way down.
 */
@OptIn(FlowPreview::class)
@AndroidEntryPoint
class ScanForegroundService : Service() {

    @Inject lateinit var bus: RunStatusBus

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Must enter the foreground promptly after startForegroundService().
        startForegroundCompat(buildNotification(bus.status.value))

        if (wakeLock == null) {
            val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "huntkill:scan").apply {
                setReferenceCounted(false)
                // Generous ceiling so a leaked lock cannot drain the battery for days;
                // normal runs release it in onDestroy long before this fires.
                acquire(WAKE_LOCK_TIMEOUT_MS)
            }
        }

        // Render bus updates into the notification, at most ~1/s. A null status
        // means the run ended — stop and remove the notification.
        scope.launch {
            bus.status.sample(1_000).collectLatest { status ->
                if (status == null) {
                    stopSelf()
                } else {
                    val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    nm.notify(NOTIFICATION_ID, buildNotification(status))
                }
            }
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        scope.cancel()
        runCatching { wakeLock?.release() }
        wakeLock = null
        super.onDestroy()
    }

    private fun startForegroundCompat(notification: Notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE,
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun buildNotification(status: RunStatus?): Notification {
        val openApp = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val builder = Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_monochrome)
            .setContentIntent(openApp)
            .setOngoing(true)
            .setOnlyAlertOnce(true)

        if (status == null) {
            return builder
                .setContentTitle("Hunt & Kill")
                .setContentText("Starting run…")
                .build()
        }

        val pausedPrefix = if (status.paused) "Paused — " else ""
        val genSuffix = if (status.generation > 1) " · gen ${status.generation}" else ""
        if (status.killing) {
            builder
                .setContentTitle("${pausedPrefix}Killing ${status.killIndex}/${status.killTotal}$genSuffix")
                .setContentText(
                    "${status.currentFrequencyHz.asHz()} · dwell ${status.dwellRemainingSeconds}s · " +
                        "elapsed ${formatElapsed(status.elapsedSeconds)}",
                )
        } else {
            val remaining = if (status.remainingSeconds > 0) {
                " · ~${formatElapsed(status.remainingSeconds)} left"
            } else {
                ""
            }
            builder
                .setContentTitle("${pausedPrefix}Hunting ${status.percentComplete}%$genSuffix")
                .setContentText("${status.currentFrequencyHz.asHz()}$remaining")
                .setProgress(100, status.percentComplete, false)
        }
        return builder.build()
    }

    private fun createChannel() {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                "Active Hunt & Kill run",
                NotificationManager.IMPORTANCE_LOW, // silent, no heads-up
            ).apply {
                description = "Live progress of the running hunt or treatment"
                setShowBadge(false)
            },
        )
    }

    companion object {
        private const val CHANNEL_ID = "scan_run"
        private const val NOTIFICATION_ID = 42
        private const val WAKE_LOCK_TIMEOUT_MS = 12 * 60 * 60 * 1000L // 12 h ceiling

        fun start(context: Context) {
            context.startForegroundService(Intent(context, ScanForegroundService::class.java))
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, ScanForegroundService::class.java))
        }
    }
}
