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
 * One selectable generator endpoint: a single serial [portIndex] on a physical
 * [device]. The GeneratorX box enumerates as ONE USB device exposing TWO ports, so a
 * single device yields two [UsbGeneratorPort] entries (one per generator).
 */
data class UsbGeneratorPort(
    val device: UsbDevice,
    val portIndex: Int,
    val portCount: Int,
    val label: String,
)

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

    /**
     * First attached device the USB-serial stack can drive (any supported bridge —
     * CDC-ACM, FTDI, CP210x, WCH CH34x/CH9102 …), not just one fixed vendor id.
     */
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
        return UsbCdcSerialTransport.findSupportedDevice(usbManager)
            .also {
                if (it == null) {
                    log.w(TAG, "No USB-serial device the driver stack recognizes was found")
                } else {
                    log.i(
                        TAG,
                        "Selected device: vid=0x%04X pid=0x%04X name=%s".format(
                            it.vendorId,
                            it.productId,
                            it.deviceName,
                        ),
                    )
                }
            }
    }

    /**
     * Every selectable generator endpoint: ONE entry per serial port per supported
     * device. The GeneratorX box is a single USB device exposing two ports (two
     * generators), so it yields two entries. Logs the discovered port count per device.
     */
    fun listGenerators(): List<UsbGeneratorPort> {
        val devices = UsbCdcSerialTransport.listSupportedDevices(usbManager)
        log.i(TAG, "Supported USB devices: ${devices.size}")

        val ports = ArrayList<UsbGeneratorPort>()
        for (device in devices) {
            val count = UsbCdcSerialTransport.countPorts(usbManager, device).coerceAtLeast(1)
            log.i(
                TAG,
                "  device vid=0x%04X pid=0x%04X name=%s ports=%d".format(
                    device.vendorId,
                    device.productId,
                    device.deviceName,
                    count,
                ),
            )
            for (index in 0 until count) {
                val label = if (count > 1) {
                    "0x%04X:0x%04X port %d/%d".format(
                        device.vendorId,
                        device.productId,
                        index + 1,
                        count,
                    )
                } else {
                    "0x%04X:0x%04X %s".format(device.vendorId, device.productId, device.deviceName)
                }
                ports.add(UsbGeneratorPort(device, index, count, label))
            }
        }
        log.i(TAG, "Selectable generator ports: ${ports.size}")
        return ports
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

    /** The device the most recent [connect] opened, kept so the in-app generator
     *  switcher can re-open a different port of the SAME device without re-enumerating
     *  or re-requesting USB permission. */
    private var lastConnectedDevice: UsbDevice? = null

    /**
     * Convenience: connect to the first auto-detected generator on port 0. The Connect
     * screen uses this — it no longer asks the user to pick a port up front; the
     * generator is chosen/switched later on the Hunt config screen.
     */
    suspend fun connect(): GeneratorSession {
        val device = findGenerator()
            ?: throw IllegalStateException("No generator found. Attach a Spooky2 generator over USB-OTG.")
        val count = UsbCdcSerialTransport.countPorts(usbManager, device).coerceAtLeast(1)
        return connect(device, portIndex = 0, portCount = count)
    }

    /**
     * Re-open the SAME device on a different [portIndex]. Used by the generator
     * switcher: permission is already granted, so this skips enumeration/permission and
     * just opens the chosen port. Throws if no device has been connected yet.
     */
    suspend fun switchPort(portIndex: Int): GeneratorSession {
        val device = lastConnectedDevice
            ?: throw IllegalStateException("No connected device to switch ports on.")
        val count = UsbCdcSerialTransport.countPorts(usbManager, device).coerceAtLeast(1)
        return connect(device, portIndex = portIndex, portCount = count)
    }

    /**
     * Full live connect on a chosen [device] / [portIndex]: request permission → open
     * transport on that port → probe + auth → build the [GeneratorSession]. Both ports
     * of a dual-generator device share one USB permission grant. Throws with a clear
     * message on any failure.
     */
    suspend fun connect(
        device: UsbDevice,
        portIndex: Int = 0,
        portCount: Int = UsbCdcSerialTransport.countPorts(usbManager, device).coerceAtLeast(1),
    ): GeneratorSession {
        log.i(
            TAG,
            "Connecting to selected generator vid=0x%04X pid=0x%04X name=%s port=%d/%d".format(
                device.vendorId,
                device.productId,
                device.deviceName,
                portIndex,
                portCount,
            ),
        )

        val granted = requestPermission(device)
        if (!granted) {
            throw IllegalStateException("USB permission denied for ${device.deviceName}.")
        }

        log.i(TAG, "Opening transport for ${device.deviceName} port $portIndex")
        val transport = LoggingSerialTransport(
            UsbCdcSerialTransport(usbManager = usbManager, device = device, portIndex = portIndex),
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

        // Safety: a prior session (or one killed by an app force-stop) can leave the
        // generator emitting at its last frequency. Zero it on every fresh connect —
        // clear both frequency channels, set amplitude CV to 0, outputs off.
        log.i(TAG, "Zeroing generator output on connect")
        client.zeroOutput()

        lastConnectedDevice = device
        return GeneratorSession(
            baudRate = connection.baudRate,
            generatorType = connection.generatorType,
            authToken = null,
            transport = transport,
            client = client,
            engine = ScanEngine(client),
            usbPort = UsbPortInfo(index = portIndex, count = portCount),
        )
    }

    companion object {
        private const val TAG = "Usb"
        private const val ACTION_USB_PERMISSION = "com.spooky2.huntkill.USB_PERMISSION"
    }
}
