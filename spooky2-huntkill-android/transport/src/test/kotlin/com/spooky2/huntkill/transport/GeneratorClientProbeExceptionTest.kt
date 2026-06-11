package com.spooky2.huntkill.transport

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.IOException

/**
 * Verifies that an [IOException] thrown by the transport during probe (write or readLine on
 * a dead port) does NOT bypass [SerialTransport.close]:
 *   - The leaked-port bug: if close() is skipped the USB port stays open and subsequent
 *     connect() calls return immediately (isOpen guard), leaving the engine in stale state.
 *   - After the exception the connect() returns null — no generator found.
 *
 * The mock throws on every write/readLine so neither baud rate succeeds; close() must be
 * called once per failed baud.
 */
class GeneratorClientProbeExceptionTest {

    @Test
    fun `IOException during probe closes transport and returns null`() = runTest {
        val transport = mockk<SerialTransport>(relaxed = true)

        // Every write throws — simulates a dead USB port dropping the connection.
        coEvery { transport.write(any()) } throws IOException("USB write failed")

        val client = GeneratorClient(
            transport = transport,
            delayProvider = { },
            challengeGenerator = { "271543986" },
        )

        val result = client.connect()

        // No generator answered — connect must return null.
        assertNull("connect must return null when transport throws on every baud", result)

        // close() must have been called for each probed baud so the port is not leaked.
        coVerify(exactly = GeneratorClient.PROBE_BAUD_COUNT) { transport.close() }
    }

    @Test
    fun `IOException on readLine during probe closes transport and tries next baud`() = runTest {
        val transport = mockk<SerialTransport>(relaxed = true)

        // write succeeds but readLine throws — the response path on a half-open port.
        coEvery { transport.readLine(any()) } throws IOException("USB read failed")

        val client = GeneratorClient(
            transport = transport,
            delayProvider = { },
            challengeGenerator = { "271543986" },
        )

        val result = client.connect()

        assertNull("connect must return null when readLine throws on every baud", result)
        coVerify(exactly = GeneratorClient.PROBE_BAUD_COUNT) { transport.close() }
    }
}
