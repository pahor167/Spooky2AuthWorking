package com.spooky2.huntkill.transport

import com.spooky2.huntkill.core.protocol.GeneratorProtocol
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Verifies that [GeneratorClient.connect] reads the generator device-info queries once
 * at connect (after `:w92` auth, before the init sequence) and captures the parsed
 * values into [GeneratorClient.Connection] — and that a TIMEOUT (null read) on any one
 * query leaves that field null WITHOUT failing the connect.
 */
class GeneratorClientDeviceInfoTest {

    private val challenge = "271543986"
    private val deviceEcho = "726911191"
    private val deviceResponse = "941378256"
    private val expectedToken = "883542462"

    /**
     * Build a transport whose readLine answers the device-info reads from [responses]
     * (keyed by command), the auth handshake from the recorded vector, and everything
     * else with ":ok". Returns null (timeout) for any command mapped to null.
     */
    private fun deviceInfoTransport(
        writes: MutableList<ByteArray>,
        responses: Map<String, String?>,
    ): SerialTransport {
        val transport = mockk<SerialTransport>(relaxed = true)
        coEvery { transport.write(capture(writes)) } returns Unit
        coEvery { transport.readLine(any()) } answers {
            val lastCmd = writes.lastOrNull()?.let { String(it, Charsets.US_ASCII).trim() }
            when {
                lastCmd == null -> null
                lastCmd == GeneratorProtocol.ACTION_PING -> null // XM probe: no answer
                lastCmd.startsWith(":r90=") -> ":r90=$deviceEcho,$deviceResponse."
                lastCmd.startsWith(":w92=") -> ":ok"
                responses.containsKey(lastCmd) -> responses[lastCmd]
                else -> ":ok"
            }
        }
        return transport
    }

    @Test
    fun `connect captures serial firmware hardwareType and hardwareInfo`() = runTest {
        val writes = mutableListOf<ByteArray>()
        val transport = deviceInfoTransport(
            writes,
            mapOf(
                GeneratorProtocol.READ_SERIAL_NUMBER to ":r91=SN123.",
                GeneratorProtocol.READ_FIRMWARE_VERSION to ":r68=201.",
                GeneratorProtocol.READ_HARDWARE_TYPE to ":r80=2.",
                GeneratorProtocol.READ_HARDWARE_INFO to ":r02=200.",
            ),
        )

        val client = GeneratorClient(
            transport = transport,
            delayProvider = { },
            challengeGenerator = { challenge },
        )

        val connection = client.connect()

        assertNotNull("connect should authenticate", connection)
        assertEquals("SN123", connection!!.serialNumber)
        assertEquals("201", connection.firmwareVersion)
        assertEquals("2", connection.hardwareType)
        assertEquals("200", connection.hardwareInfo)
    }

    @Test
    fun `a timed out device-info query leaves its field null but connect still succeeds`() = runTest {
        val writes = mutableListOf<ByteArray>()
        // Serial number TIMES OUT (null), the rest answer normally.
        val transport = deviceInfoTransport(
            writes,
            mapOf(
                GeneratorProtocol.READ_SERIAL_NUMBER to null,
                GeneratorProtocol.READ_FIRMWARE_VERSION to ":r68=201.",
                GeneratorProtocol.READ_HARDWARE_TYPE to ":r80=2.",
                GeneratorProtocol.READ_HARDWARE_INFO to ":r02=200.",
            ),
        )

        val client = GeneratorClient(
            transport = transport,
            delayProvider = { },
            challengeGenerator = { challenge },
        )

        val connection = client.connect()

        // Auth still returns a valid Connection despite the timed-out serial query.
        assertNotNull("connect must still succeed when a device-info query times out", connection)
        assertEquals(GeneratorClient.BAUD_GENERATORX, connection!!.baudRate)
        assertNull("timed-out serial leaves field null", connection.serialNumber)
        assertEquals("201", connection.firmwareVersion)
        assertEquals("2", connection.hardwareType)
        assertEquals("200", connection.hardwareInfo)
    }
}
