package com.spooky2.huntkill.transport.usb

import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbManager
import com.hoho.android.usbserial.driver.CdcAcmSerialDriver
import com.hoho.android.usbserial.driver.Ch34xSerialDriver
import com.hoho.android.usbserial.driver.ProbeTable
import com.hoho.android.usbserial.driver.UsbSerialDriver
import com.hoho.android.usbserial.driver.UsbSerialPort
import com.hoho.android.usbserial.driver.UsbSerialProber
import com.spooky2.huntkill.transport.SerialTransport
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

/**
 * Real CDC-ACM [SerialTransport] over USB-OTG, backed by usb-serial-for-android.
 *
 * This is the single hardware-coupled class. It is written and compiled now and
 * mock-tested; it will be HW-verified in Phase 5. It deliberately does NOT do any
 * permission UI — the app layer must request and be granted USB permission for the
 * [UsbDevice] before constructing this class (mirroring the C# `SerialPortFactory`,
 * which only opens an already-enumerated port).
 *
 * Port settings mirror the C# `SerialPortFactory`:
 *   - 8 data bits, 1 stop bit, no parity, no flow control
 *   - DTR disabled, RTS enabled
 *   - 2000ms read/write timeouts
 *
 * GeneratorX Pro enumerates with USB vendor id 0x04D8 (Microchip). The default
 * usb-serial-for-android prober does not list every Microchip product id, so we
 * build a [ProbeTable] that maps the device's actual product id to the
 * [CdcAcmSerialDriver].
 */
class UsbCdcSerialTransport(
    private val usbManager: UsbManager,
    private val device: UsbDevice,
    private val readTimeoutMs: Int = DEFAULT_TIMEOUT_MS,
    private val writeTimeoutMs: Int = DEFAULT_TIMEOUT_MS,
) : SerialTransport {

    private var port: UsbSerialPort? = null
    private var connection: UsbDeviceConnection? = null

    /** Accumulates bytes across [read] calls until a CRLF (or '.') terminator is seen. */
    private val lineBuffer = StringBuilder()

    override val isOpen: Boolean
        get() = port?.isOpen == true

    override suspend fun open(baudRate: Int): Unit = withContext(Dispatchers.IO) {
        if (isOpen) return@withContext

        val driver = resolveDriver(device)
            ?: throw IOException("No CDC-ACM driver for device vid=${device.vendorId} pid=${device.productId}")

        val serialPort = driver.ports.firstOrNull()
            ?: throw IOException("Driver exposed no serial ports")

        val deviceConnection = usbManager.openDevice(device)
            ?: throw IOException("Permission denied or device unavailable: ${device.deviceName}")

        try {
            serialPort.open(deviceConnection)
            // 8N1, no flow control — identical to the C# SerialPortFactory.
            serialPort.setParameters(
                baudRate,
                UsbSerialPort.DATABITS_8,
                UsbSerialPort.STOPBITS_1,
                UsbSerialPort.PARITY_NONE,
            )
            serialPort.setDTR(false)
            serialPort.setRTS(true)
        } catch (e: IOException) {
            runCatching { serialPort.close() }
            runCatching { deviceConnection.close() }
            throw e
        }

        lineBuffer.setLength(0)
        connection = deviceConnection
        port = serialPort
    }

    override suspend fun write(bytes: ByteArray): Unit = withContext(Dispatchers.IO) {
        val activePort = port ?: throw IOException("Port not open")
        activePort.write(bytes, writeTimeoutMs)
    }

    override suspend fun readLine(timeoutMs: Long): String? = withContext(Dispatchers.IO) {
        val activePort = port ?: throw IOException("Port not open")

        // Drain any complete line already buffered from a previous read.
        extractLine()?.let { return@withContext it }

        val deadline = System.currentTimeMillis() + timeoutMs
        val chunk = ByteArray(READ_CHUNK_SIZE)

        while (System.currentTimeMillis() < deadline) {
            val remaining = (deadline - System.currentTimeMillis()).coerceAtLeast(1).toInt()
            val read = activePort.read(chunk, minOf(remaining, readTimeoutMs))
            if (read > 0) {
                lineBuffer.append(String(chunk, 0, read, Charsets.US_ASCII))
                extractLine()?.let { return@withContext it }
            }
        }
        // Timeout: return whatever partial content accumulated (or null if none),
        // mirroring the C# BlockingRead which returns null on TimeoutException.
        null
    }

    override suspend fun close(): Unit = withContext(Dispatchers.IO) {
        runCatching { port?.close() }
        runCatching { connection?.close() }
        port = null
        connection = null
        lineBuffer.setLength(0)
    }

    /**
     * Assemble a single response line from [lineBuffer] when a terminator is present.
     * Generators terminate responses with CRLF; some reads (`:r02=200.`) also carry a
     * trailing '.' before the CRLF, so CRLF is the authoritative delimiter.
     */
    private fun extractLine(): String? {
        val newlineIdx = lineBuffer.indexOf("\n")
        if (newlineIdx < 0) return null

        val line = lineBuffer.substring(0, newlineIdx).trim()
        lineBuffer.delete(0, newlineIdx + 1)
        // Skip blank lines (a lone CR/LF) and keep scanning.
        return line.ifEmpty { extractLine() }
    }

    private fun resolveDriver(device: UsbDevice): UsbSerialDriver? {
        // The default prober already recognizes the common USB-serial bridges seen on
        // Spooky2 generators: CDC-ACM, FTDI, CP210x, and WCH CH34x/CH9102 (incl.
        // 1A86:55D2). Prefer it so the correct driver is selected per chip.
        UsbSerialProber.getDefaultProber().probeDevice(device)?.let { return it }

        // Fallback for a bridge the default table doesn't list: force a driver by
        // vendor — WCH -> CH34x, everything else -> CDC-ACM (e.g. Microchip 0x04D8).
        val driverClass = if (device.vendorId == WCH_VENDOR_ID) {
            Ch34xSerialDriver::class.java
        } else {
            CdcAcmSerialDriver::class.java
        }
        val table = ProbeTable().apply {
            addProduct(device.vendorId, device.productId, driverClass)
        }
        return UsbSerialProber(table).probeDevice(device)
    }

    companion object {
        /** Microchip vendor id (some GeneratorX Pro units present a CDC-ACM bridge). */
        const val GENERATORX_VENDOR_ID: Int = 0x04D8

        /** WCH/QinHeng vendor id — CH340/CH9102 bridges (e.g. 1A86:55D2). */
        const val WCH_VENDOR_ID: Int = 0x1A86

        /** USB-serial bridge vendor ids seen on Spooky2 generators (Microchip, WCH, FTDI, Silabs). */
        val GENERATOR_VENDOR_IDS: Set<Int> = setOf(0x04D8, 0x1A86, 0x0403, 0x10C4)

        /**
         * Pick the first attached device the USB-serial stack can drive: one the
         * default prober recognizes, else one with a known bridge vendor id.
         */
        fun findSupportedDevice(usbManager: UsbManager): UsbDevice? {
            val devices = usbManager.deviceList.values
            return devices.firstOrNull { UsbSerialProber.getDefaultProber().probeDevice(it) != null }
                ?: devices.firstOrNull { it.vendorId in GENERATOR_VENDOR_IDS }
        }

        /**
         * All attached devices the USB-serial stack can drive: ones the default prober
         * recognizes, plus any with a known bridge vendor id. Lets the user pick which
         * generator to run on when more than one is attached.
         */
        fun listSupportedDevices(usbManager: UsbManager): List<UsbDevice> {
            val prober = UsbSerialProber.getDefaultProber()
            return usbManager.deviceList.values.filter {
                prober.probeDevice(it) != null || it.vendorId in GENERATOR_VENDOR_IDS
            }
        }

        private const val DEFAULT_TIMEOUT_MS = 2000
        private const val READ_CHUNK_SIZE = 256
    }
}
