package com.spooky2.huntkill.data

import com.spooky2.huntkill.core.scan.ScanEngine
import com.spooky2.huntkill.transport.GeneratorClient

/**
 * TEST-ONLY factory that opens and authenticates a [GeneratorSession] from a
 * [TransportFactory].
 *
 * Used by the JVM ViewModel replay tests to build a no-hardware session over a
 * [FakeTransport][com.spooky2.huntkill.transport.fake.FakeTransport]: a
 * [GeneratorClient] is built on a fresh transport and the link is opened at the
 * GeneratorX baud (the bundled dump is a post-auth GeneratorX Pro session). When a
 * handshake fixture is present, the recorded challenge-response is exercised and the
 * resulting token is surfaced as [GeneratorSession.authToken].
 *
 * The runtime app authenticates over real USB via
 * [com.spooky2.huntkill.data.UsbConnectionManager] and does not use this factory.
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
            isDemo = true,
        )
    }
}
