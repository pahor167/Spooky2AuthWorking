package com.spooky2.huntkill.data

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import androidx.core.content.ContextCompat
import com.spooky2.huntkill.core.scan.ScanEngine
import com.spooky2.huntkill.log.LogBus
import com.spooky2.huntkill.log.LoggingSerialTransport
import com.spooky2.huntkill.transport.GeneratorClient
import com.spooky2.huntkill.transport.usb.UsbCdcSerialTransport
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * Real USB bring-up: enumerate the attached generator, request USB permission, open a
 * CDC-ACM [UsbCdcSerialTransport] (wrapped in [LoggingSerialTransport]), run the full
 * [GeneratorClient.connect] probe + auth, and surface a live [GeneratorSession].
 *
 * Every step is logged to [LogBus] (this is the hardware bring-up path), so failures
 * are diagnosable from the in-app Logs screen.
 */
@Singleton
class UsbConnectionManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val usbManager: UsbManager,
    private val log: LogBus,
) {

    /** First attached device matching the GeneratorX vendor id, or null. */
    fun findGenerator(): UsbDevice? {
        val devices = usbManager.deviceList.values
        log.i(TAG, "Enumerating USB devices: ${devices.size} attached")
        for (device in devices) {
            log.i(
                TAG,
                "  device vid=0x%04X pid=0x%04X name=%s".format(
                    device.vendorId,
                    device.productId,
                    device.deviceName,
                ),
            )
        }
        return devices.firstOrNull { it.vendorId == GENERATORX_VENDOR_ID }
            .also {
                if (it == null) log.w(TAG, "No device with vid=0x%04X found".format(GENERATORX_VENDOR_ID))
                else log.i(TAG, "Selected generator: ${it.deviceName}")
            }
    }

    /**
     * Ensure USB permission for [device]. Returns true if already granted or granted
     * by the user; false if denied. Registers a private, non-exported receiver and
     * awaits the system broadcast.
     */
    suspend fun requestPermission(device: UsbDevice): Boolean {
        if (usbManager.hasPermission(device)) {
            log.i(TAG, "USB permission already granted for ${device.deviceName}")
            return true
        }

        log.i(TAG, "Requesting USB permission for ${device.deviceName}")
        return suspendCancellableCoroutine { continuation ->
            val receiver = object : BroadcastReceiver() {
                override fun onReceive(ctx: Context, intent: Intent) {
                    if (intent.action != ACTION_USB_PERMISSION) return
                    runCatching { context.unregisterReceiver(this) }
                    val granted = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)
                    log.i(TAG, if (granted) "USB permission GRANTED" else "USB permission DENIED")
                    if (continuation.isActive) continuation.resume(granted)
                }
            }

            ContextCompat.registerReceiver(
                context,
                receiver,
                IntentFilter(ACTION_USB_PERMISSION),
                ContextCompat.RECEIVER_NOT_EXPORTED,
            )

            continuation.invokeOnCancellation {
                runCatching { context.unregisterReceiver(receiver) }
            }

            val permissionIntent = PendingIntent.getBroadcast(
                context,
                0,
                Intent(ACTION_USB_PERMISSION).setPackage(context.packageName),
                PendingIntent.FLAG_IMMUTABLE,
            )
            usbManager.requestPermission(device, permissionIntent)
        }
    }

    /**
     * Full live connect: find device → request permission → open transport → probe +
     * auth → build the [GeneratorSession]. Throws with a clear message on any failure.
     */
    suspend fun connect(): GeneratorSession {
        val device = findGenerator()
            ?: throw IllegalStateException("No generator found. Attach a Spooky2 generator over USB-OTG.")

        val granted = requestPermission(device)
        if (!granted) {
            throw IllegalStateException("USB permission denied for ${device.deviceName}.")
        }

        log.i(TAG, "Opening transport for ${device.deviceName}")
        val transport = LoggingSerialTransport(
            UsbCdcSerialTransport(usbManager = usbManager, device = device),
            log,
        )
        val client = GeneratorClient(transport = transport)

        log.i(TAG, "Probing baud + authenticating…")
        val connection = client.connect()
        if (connection == null) {
            runCatching { client.close() }
            throw IllegalStateException("No generator answered on any baud rate.")
        }

        log.i(
            TAG,
            "Connected: type=${connection.generatorType} baud=${connection.baudRate}",
        )
        return GeneratorSession(
            baudRate = connection.baudRate,
            generatorType = connection.generatorType,
            authToken = null,
            transport = transport,
            client = client,
            engine = ScanEngine(client),
        )
    }

    companion object {
        private const val TAG = "Usb"
        private const val GENERATORX_VENDOR_ID = UsbCdcSerialTransport.GENERATORX_VENDOR_ID
        private const val ACTION_USB_PERMISSION = "com.spooky2.huntkill.USB_PERMISSION"
    }
}
