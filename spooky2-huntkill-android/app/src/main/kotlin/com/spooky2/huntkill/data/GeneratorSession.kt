package com.spooky2.huntkill.data

import com.spooky2.huntkill.core.scan.ScanEngine
import com.spooky2.huntkill.transport.GeneratorClient
import com.spooky2.huntkill.transport.SerialTransport

/**
 * One live connection to a generator: the open [SerialTransport], the
 * [GeneratorClient] ([com.spooky2.huntkill.core.scan.GeneratorLink] impl) bridging
 * it onto `core`, and a [ScanEngine] driving that link.
 *
 * Created by [GeneratorSessionFactory.connect]; closed via [close].
 */
class GeneratorSession(
    val baudRate: Int,
    val generatorType: String,
    val authToken: String?,
    private val transport: SerialTransport,
    val client: GeneratorClient,
    val engine: ScanEngine,
    /**
     * USB endpoint this session was opened on, when it is a live USB session: which
     * serial [portIndex] of how many [portCount]. Null for the demo (no-hardware)
     * path. Used by the in-app generator switcher to re-open a different port of the
     * SAME device without re-enumerating or re-requesting USB permission.
     */
    val usbPort: UsbPortInfo? = null,
) {
    suspend fun close() {
        client.close()
    }
}

/** Which USB serial port (0-based [index]) of how many [count] a live session uses. */
data class UsbPortInfo(val index: Int, val count: Int)

/**
 * Opens and authenticates a [GeneratorSession] from a [TransportFactory].
 *
 * Default path is the demo (fake) transport. The wiring mirrors the transport
 * end-to-end test exactly: a [GeneratorClient] is built on a fresh transport and the
 * link is opened at the GeneratorX baud (the bundled dump is a post-auth GeneratorX
 * Pro session). When a [FakeTransport][com.spooky2.huntkill.transport.fake.FakeTransport]
 * handshake fixture is present, the recorded challenge-response is exercised; the
 * resulting [GeneratorAuthentication][com.spooky2.huntkill.core.auth.GeneratorAuthentication]
 * token is surfaced as [GeneratorSession.authToken].
 *
 * The real USB path reuses [GeneratorClient.connect] (baud probe + full auth) — see
 * the `connectViaProbe` flag.
 */
class GeneratorSessionFactory(
    private val transportFactory: TransportFactory,
    private val handshake: com.spooky2.huntkill.transport.fake.FakeTransport.HandshakeFixture? = null,
    private val connectViaProbe: Boolean = false,
) {
    suspend fun connect(): GeneratorSession {
        val transport = transportFactory.create()
        val client = GeneratorClient(transport = transport)

        if (connectViaProbe) {
            // Real hardware path: probe baud rates + run full challenge-response auth.
            val connection = client.connect()
                ?: throw IllegalStateException("No generator answered on any baud rate")
            return GeneratorSession(
                baudRate = connection.baudRate,
                generatorType = connection.generatorType,
                authToken = null,
                transport = transport,
                client = client,
                engine = ScanEngine(client),
            )
        }

        // Demo path: open the GeneratorX link directly (the recorded dump is already
        // post-auth), mirroring the transport end-to-end test wiring exactly.
        transport.open(GeneratorClient.BAUD_GENERATORX)

        val token = handshake?.let { fixture ->
            // Drive the recorded challenge-response so auth is genuinely exercised.
            val challenge = com.spooky2.huntkill.core.auth.GeneratorAuthentication.generateChallenge()
            com.spooky2.huntkill.core.auth.GeneratorAuthentication
                .computeAuthToken(challenge, fixture.deviceResponse)
        }

        return GeneratorSession(
            baudRate = GeneratorClient.BAUD_GENERATORX,
            generatorType = GeneratorClient.GENERATOR_TYPE_GENERATORX,
            authToken = token,
            transport = transport,
            client = client,
            engine = ScanEngine(client),
        )
    }
}
