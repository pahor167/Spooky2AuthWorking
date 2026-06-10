package com.spooky2.huntkill.log

import com.spooky2.huntkill.transport.SerialTransport

/**
 * A [SerialTransport] decorator that records every operation onto a [LogBus] while
 * passing all byte traffic through to [delegate] unchanged. Byte semantics are
 * identical to the wrapped transport; this only observes.
 *
 * Frames are colon-prefixed ASCII terminated with CRLF; in the log the CRLF is shown
 * as ⏎ so the wire content stays on a single readable line.
 */
class LoggingSerialTransport(
    private val delegate: SerialTransport,
    private val log: LogBus,
) : SerialTransport {

    override val isOpen: Boolean
        get() = delegate.isOpen

    override suspend fun open(baudRate: Int) {
        log.i(TAG, "open @$baudRate")
        runLogging("open") { delegate.open(baudRate) }
    }

    override suspend fun write(bytes: ByteArray) {
        log.d(TAG, "TX: ${render(bytes)}")
        runLogging("write") { delegate.write(bytes) }
    }

    override suspend fun readLine(timeoutMs: Long): String? {
        val line = runLogging("readLine") { delegate.readLine(timeoutMs) }
        if (line == null) {
            log.d(TAG, "RX: <timeout>")
        } else {
            log.d(TAG, "RX: $line")
        }
        return line
    }

    override suspend fun close() {
        log.i(TAG, "close")
        runLogging("close") { delegate.close() }
    }

    /** Run a delegate call, logging (and rethrowing) any thrown exception at level 'e'. */
    private suspend fun <T> runLogging(op: String, block: suspend () -> T): T =
        try {
            block()
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Throwable) {
            log.e(TAG, "$op failed: ${e.message ?: e::class.java.simpleName}")
            throw e
        }

    /** ASCII-render the frame with CRLF shown as ⏎ and other control chars stripped. */
    private fun render(bytes: ByteArray): String {
        val text = String(bytes, Charsets.US_ASCII)
        return buildString {
            for (ch in text) {
                when (ch) {
                    '\r', '\n' -> append('⏎')
                    else -> if (ch.code in 0x20..0x7E) append(ch)
                }
            }
        }
    }

    companion object {
        private const val TAG = "Serial"
    }
}
