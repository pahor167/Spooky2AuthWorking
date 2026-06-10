package com.spooky2.huntkill.data

import com.spooky2.huntkill.core.scan.ScanEngine
import com.spooky2.huntkill.transport.GeneratorClient
import com.spooky2.huntkill.transport.SerialTransport

/**
 * One live connection to a generator: the open [SerialTransport], the
 * [GeneratorClient] ([com.spooky2.huntkill.core.scan.GeneratorLink] impl) bridging
 * it onto `core`, and a [ScanEngine] driving that link.
 *
 * Built by [UsbConnectionManager] for the live USB path; closed via [close].
 */
class GeneratorSession(
    val baudRate: Int,
    val generatorType: String,
    val authToken: String?,
    private val transport: SerialTransport,
    val client: GeneratorClient,
    val engine: ScanEngine,
    /**
     * USB endpoint this session was opened on: which serial [portIndex] of how many
     * [portCount]. Used by the in-app generator switcher to re-open a different port of
     * the SAME device without re-enumerating or re-requesting USB permission. Null only
     * for the test replay sessions built off a FakeTransport.
     */
    val usbPort: UsbPortInfo? = null,
    /**
     * True only for the no-hardware test replay sessions (FakeTransport). Replay
     * sessions run with fast scan timing (no settle delay / amplitude ramp) so the
     * recorded dump reproduces; live USB sessions are always `false` and use the
     * original Spooky2 timing. The runtime app never constructs a demo session.
     */
    val isDemo: Boolean = false,
) {
    suspend fun close() {
        client.close()
    }
}

/** Which USB serial port (0-based [index]) of how many [count] a live session uses. */
data class UsbPortInfo(val index: Int, val count: Int)
