package com.spooky2.huntkill.transport

import com.spooky2.huntkill.core.auth.GeneratorAuthentication
import com.spooky2.huntkill.core.protocol.GeneratorProtocol
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Verifies that [GeneratorClient] performs the GeneratorX challenge-response
 * authentication and the post-auth init sequence exactly like the C# reference,
 * driving a mocked [SerialTransport].
 *
 * The challenge/response/token vector is the recorded `Handshake1` exchange:
 *   challenge       = 271543986
 *   device echo     = 726911191
 *   device response = 941378256
 *   auth token      = 883542462
 */
class GeneratorClientAuthTest {

    private val challenge = "271543986"
    private val deviceEcho = "726911191"
    private val deviceResponse = "941378256"
    private val expectedToken = "883542462"

    @Test
    fun `auth math reproduces recorded Handshake1 token`() {
        // Sanity: the recorded dump is a valid golden vector for core's auth.
        assertEquals(deviceEcho, GeneratorAuthentication.computeEcho(challenge, deviceResponse))
        assertEquals(expectedToken, GeneratorAuthentication.computeAuthToken(challenge, deviceResponse))
    }

    @Test
    fun `connect sends correct w92 token derived from device challenge response`() = runTest {
        val transport = mockk<SerialTransport>(relaxed = true)

        // 57600 probe path: ping returns nothing → fall through to 115200.
        // 115200 path: r90 challenge → recorded echo,response; w92 → ok; init → ok.
        val writes = mutableListOf<ByteArray>()
        coEvery { transport.write(capture(writes)) } returns Unit
        coEvery { transport.readLine(any()) } answers {
            val lastCmd = writes.lastOrNull()?.let { String(it, Charsets.US_ASCII).trim() }
            when {
                lastCmd == null -> null
                lastCmd == GeneratorProtocol.ACTION_PING -> null // XM probe: no answer
                lastCmd.startsWith(":r90=") ->
                    ":r90=$deviceEcho,$deviceResponse."
                lastCmd.startsWith(":w92=") -> ":ok"
                else -> ":ok"
            }
        }

        val client = GeneratorClient(
            transport = transport,
            delayProvider = { /* no real delay in tests */ },
            challengeGenerator = { challenge },
        )

        val connection = client.connect()

        assertNotNull("connect should authenticate", connection)
        assertEquals(GeneratorClient.BAUD_GENERATORX, connection!!.baudRate)
        assertEquals(GeneratorClient.GENERATOR_TYPE_GENERATORX, connection.generatorType)

        // The exact challenge and the derived token must be on the wire.
        val sent = writes.map { String(it, Charsets.US_ASCII).trim() }
        assertTrue("challenge sent", sent.contains(":r90=$challenge,"))
        assertTrue("derived token sent", sent.contains(":w92=$expectedToken."))
    }

    @Test
    fun `connect opens 57600 first then 115200`() = runTest {
        val transport = mockk<SerialTransport>(relaxed = true)
        val writes = mutableListOf<ByteArray>()
        coEvery { transport.write(capture(writes)) } returns Unit
        coEvery { transport.readLine(any()) } answers {
            val lastCmd = writes.lastOrNull()?.let { String(it, Charsets.US_ASCII).trim() }
            when {
                lastCmd == GeneratorProtocol.ACTION_PING -> null
                lastCmd?.startsWith(":r90=") == true -> ":r90=$deviceEcho,$deviceResponse."
                else -> ":ok"
            }
        }

        val client = GeneratorClient(
            transport = transport,
            delayProvider = { },
            challengeGenerator = { challenge },
        )
        client.connect()

        coVerifyOrder {
            transport.open(GeneratorClient.BAUD_XM)
            transport.open(GeneratorClient.BAUD_GENERATORX)
        }
    }

    @Test
    fun `init sequence is sent in the exact C-sharp order after auth`() = runTest {
        val transport = mockk<SerialTransport>(relaxed = true)
        val writes = mutableListOf<ByteArray>()
        coEvery { transport.write(capture(writes)) } returns Unit
        coEvery { transport.readLine(any()) } answers {
            val lastCmd = writes.lastOrNull()?.let { String(it, Charsets.US_ASCII).trim() }
            when {
                lastCmd == GeneratorProtocol.ACTION_PING -> null
                lastCmd?.startsWith(":r90=") == true -> ":r90=$deviceEcho,$deviceResponse."
                else -> ":ok"
            }
        }

        val client = GeneratorClient(
            transport = transport,
            delayProvider = { },
            challengeGenerator = { challenge },
        )
        client.connect()

        val sent = writes.map { String(it, Charsets.US_ASCII).trim() }

        // Everything after :w92 is the init sequence; assert the documented opening of it.
        val authIdx = sent.indexOf(":w92=$expectedToken.")
        assertTrue("auth token present", authIdx >= 0)
        val init = sent.subList(authIdx + 1, sent.size)

        val expectedHead = listOf(
            ":r02=0,",        // READ_HARDWARE_INFO
            ":n00=\$",        // QUERY_FIRMWARE_NAME
            ":w14=0,",        // sync off
            ":w17=0,0,",      // waveform inversion off
            ":w24=0,",
            ":w25=0,",
            ":w15=1,1,",      // low-frequency mode (CRITICAL)
        )
        assertEquals(expectedHead, init.take(expectedHead.size))

        // The CRITICAL low-frequency-mode and final waveform-2 commands must both appear.
        assertTrue(":w15=1,1, present", init.contains(":w15=1,1,"))
        assertEquals("last init command", ":w21=25,", init.last())
    }

    @Test
    fun `sendCommandWithResponse retries once on null`() = runTest {
        val transport = mockk<SerialTransport>(relaxed = true)
        var calls = 0
        coEvery { transport.readLine(any()) } answers {
            calls++
            if (calls == 1) null else ":r11=52458."
        }

        val client = GeneratorClient(transport = transport, delayProvider = { })
        val result = client.sendCommandWithResponse(GeneratorProtocol.READ_ANGLE)

        assertEquals(":r11=52458.", result)
        coVerify(exactly = 2) { transport.write(any()) }
    }
}
