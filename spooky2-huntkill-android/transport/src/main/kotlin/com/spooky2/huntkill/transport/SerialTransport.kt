package com.spooky2.huntkill.transport

/**
 * The single hardware-coupled seam in the app.
 *
 * `core` (auth/protocol/scan) talks only to this interface, never to USB directly.
 * Implementations:
 *  - [com.spooky2.huntkill.transport.usb.UsbCdcSerialTransport] — real CDC-ACM over USB-OTG (Phase 3, HW-verified Phase 5)
 *  - FakeTransport — replays a recorded generator dump so the full flow runs with no hardware (Phase 3)
 *
 * Commands are colon-prefixed ASCII terminated with CRLF (see GeneratorProtocol).
 */
interface SerialTransport {
    /** True once a port is open and ready for read/write. */
    val isOpen: Boolean

    /** Open the link at the given baud (Spooky2 uses 57600 or 115200, 8N1). */
    suspend fun open(baudRate: Int)

    /** Write raw bytes (already CRLF-terminated by the protocol layer). */
    suspend fun write(bytes: ByteArray)

    /**
     * Read one response line (up to the CRLF / '.' terminator), or null on timeout.
     * Generators reply in <1 ms at 115200 baud.
     */
    suspend fun readLine(timeoutMs: Long): String?

    /** Close the link and release the device. Idempotent. */
    suspend fun close()
}
