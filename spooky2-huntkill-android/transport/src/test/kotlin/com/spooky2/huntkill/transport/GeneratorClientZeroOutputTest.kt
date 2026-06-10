package com.spooky2.huntkill.transport

import com.spooky2.huntkill.core.protocol.GeneratorProtocol
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Verifies [GeneratorClient.zeroOutput] — the Cancel / safety-stop "make it safe now"
 * path — issues the full zero-out command sequence: clear both frequency channels,
 * set both amplitude CV outputs to 0, then stop outputs.
 */
class GeneratorClientZeroOutputTest {

    @Test
    fun `zeroOutput clears frequencies sets amplitude zero and stops`() = runTest {
        val transport = mockk<SerialTransport>(relaxed = true)
        val writes = mutableListOf<ByteArray>()
        coEvery { transport.write(capture(writes)) } returns Unit
        coEvery { transport.readLine(any()) } returns ":ok"

        val client = GeneratorClient(transport = transport, delayProvider = { })
        client.zeroOutput()

        val sent = writes.map { String(it, Charsets.US_ASCII).trim() }

        // The four explicit zero-out commands from the spec.
        assertTrue("clear freq ch1 (:w12=0,,)", sent.contains(GeneratorProtocol.CLEAR_FREQUENCY1))
        assertTrue("clear freq ch2 (:w12=,0,)", sent.contains(GeneratorProtocol.CLEAR_FREQUENCY2))
        assertTrue("amplitude CV1 -> 0 (:w28=0,)", sent.contains(":w28=0,"))
        assertTrue("amplitude CV2 -> 0 (:w29=0,)", sent.contains(":w29=0,"))

        // Stop outputs ran (output-off commands on the wire).
        assertTrue("stop output 1 (:w610)", sent.contains(GeneratorProtocol.STOP_OUTPUT1))
        assertTrue("stop output 2 (:w620)", sent.contains(GeneratorProtocol.STOP_OUTPUT2))

        // Ordering: amplitude zeroing precedes the stop sequence.
        assertTrue(
            "amplitude zero sent before stop",
            sent.indexOf(":w28=0,") < sent.indexOf(GeneratorProtocol.STOP_OUTPUT1),
        )
    }

    @Test
    fun `zeroOutput never throws when transport write fails`() = runTest {
        val transport = mockk<SerialTransport>(relaxed = true)
        coEvery { transport.write(any()) } throws IllegalStateException("port closed")

        val client = GeneratorClient(transport = transport, delayProvider = { })

        // Must complete without propagating — safe to call mid-scan on a half-open port.
        client.zeroOutput()
    }
}
