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
 * Verifies the GeneratorX connect reads [hardwareInfo] (`:r02`) and captures it into
 * [GeneratorClient.Connection]. The serial/firmware/hw-type reads are intentionally NOT
 * sent on the GeneratorX path (they time out on real units and only slow the connect),
 * so those fields are null. Connect must still succeed when `:r02` times out too.
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
    fun `GeneratorX connect captures hardwareInfo only`() = runTest {
        val writes = mutableListOf<ByteArray>()
        val transport = deviceInfoTransport(
            writes,
            mapOf(GeneratorProtocol.READ_HARDWARE_INFO to ":r02=200."),
        )

        val client = GeneratorClient(
            transport = transport,
            delayProvider = { },
            challengeGenerator = { challenge },
        )

        val connection = client.connect()

        assertNotNull("connect should authenticate", connection)
        assertEquals("200", connection!!.hardwareInfo)
        // serial/firmware/hw-type are NOT queried on the GeneratorX path.
        assertNull(connection.serialNumber)
        assertNull(connection.firmwareVersion)
        assertNull(connection.hardwareType)
        // And those dead reads were never put on the wire.
        val sent = writes.map { String(it, Charsets.US_ASCII).trim() }
        assertEquals(false, sent.any { it == GeneratorProtocol.READ_SERIAL_NUMBER })
        assertEquals(false, sent.any { it == GeneratorProtocol.READ_FIRMWARE_VERSION })
        assertEquals(false, sent.any { it == GeneratorProtocol.READ_HARDWARE_TYPE })
    }

    @Test
    fun `connect still succeeds when hardwareInfo query times out`() = runTest {
        val writes = mutableListOf<ByteArray>()
        val transport = deviceInfoTransport(
            writes,
            mapOf(GeneratorProtocol.READ_HARDWARE_INFO to null),
        )

        val client = GeneratorClient(
            transport = transport,
            delayProvider = { },
            challengeGenerator = { challenge },
        )

        val connection = client.connect()

        assertNotNull("connect must still succeed when :r02 times out", connection)
        assertEquals(GeneratorClient.BAUD_GENERATORX, connection!!.baudRate)
        assertNull(connection.hardwareInfo)
    }
}
