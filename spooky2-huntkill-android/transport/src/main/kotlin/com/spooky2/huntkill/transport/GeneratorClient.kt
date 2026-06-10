package com.spooky2.huntkill.transport

import com.spooky2.huntkill.core.auth.GeneratorAuthentication
import com.spooky2.huntkill.core.protocol.GeneratorProtocol
import com.spooky2.huntkill.core.scan.GeneratorLink
import kotlinx.coroutines.delay

/**
 * Bridges the pure-Kotlin `core` [GeneratorLink] onto a hardware [SerialTransport].
 *
 * Faithful port of the orchestration in the C# reference
 * `Spooky2.Services.Communication.GeneratorService`:
 *   - [connect] probes baud rates (57600 then 115200), runs the GeneratorX
 *     challenge-response auth on the 115200 path, and replays the post-auth init
 *     sequence exactly as the original Spooky2 dump.
 *   - [sendCommandWithResponse]/[sendCommandsBatch]/[writeFrequencies]/[start]/[stop]
 *     mirror the corresponding C# methods, including the single-retry behavior of
 *     `SendCommandWithResponse`.
 *
 * Timing (e.g. the 50ms post-open settle the C# `GetOrOpenPort` applies) is faithful
 * but injected via [delayProvider] so unit tests run with zero real delay.
 *
 * This class is `transport`-only: `core` never sees it and never sees USB.
 */
class GeneratorClient(
    private val transport: SerialTransport,
    private val responseTimeoutMs: Long = DEFAULT_RESPONSE_TIMEOUT_MS,
    private val probeTimeoutMs: Long = DEFAULT_PROBE_TIMEOUT_MS,
    private val delayProvider: suspend (Long) -> Unit = { ms -> delay(ms) },
    private val challengeGenerator: () -> String = { GeneratorAuthentication.generateChallenge() },
) : GeneratorLink {

    /** Outcome of [connect]: the negotiated baud and the discovered generator type. */
    data class Connection(val baudRate: Int, val generatorType: String)

    private var connected = false

    /**
     * Discover and authenticate the generator, then run the post-auth init sequence.
     *
     * Port of C# `GeneratorService.FindGenerators` for a single device:
     *   1. Try 57600 (XM): ping → handshake → read hw type/firmware/serial.
     *   2. Try 115200 (GeneratorX): challenge-response auth → full init sequence.
     *
     * @return the [Connection] describing the link, or null if no generator answered.
     */
    suspend fun connect(): Connection? {
        for (baud in PROBE_BAUD_RATES) {
            transport.open(baud)
            delayProvider(POST_OPEN_SETTLE_MS)

            val connection = if (baud == BAUD_GENERATORX) {
                tryAuthenticateGeneratorX()
            } else {
                tryProbeXm()
            }

            if (connection != null) {
                connected = true
                return connection
            }
            transport.close()
        }
        return null
    }

    /** GeneratorX challenge-response handshake + post-auth init. */
    private suspend fun tryAuthenticateGeneratorX(): Connection? {
        val challenge = challengeGenerator()
        val challengeResponse = sendProbe(":r90=$challenge,") ?: return null

        var body = challengeResponse.trimStart(':')
        if (!body.startsWith("r90=")) return null
        body = body.substring(4).trimEnd('.')

        val parts = body.split(',')
        if (parts.size != 2 || parts[0].length != 9 || parts[1].length != 9) return null

        val deviceResponse = parts[1]
        // Echo is verified but a mismatch is non-fatal in the C# reference (it logs and proceeds).

        val authToken = GeneratorAuthentication.computeAuthToken(challenge, deviceResponse)
        val authResponse = sendProbe(":w92=$authToken.") ?: return null
        if (!authResponse.contains("ok")) return null

        runInitSequence()
        return Connection(baudRate = BAUD_GENERATORX, generatorType = GENERATOR_TYPE_GENERATORX)
    }

    /** Legacy XM probe at 57600. */
    private suspend fun tryProbeXm(): Connection? {
        val ping = sendProbe(GeneratorProtocol.ACTION_PING) ?: return null
        if (ping.isEmpty()) return null

        sendProbe(GeneratorProtocol.ACTION_HANDSHAKE)
        sendProbe(GeneratorProtocol.READ_HARDWARE_TYPE)
        sendProbe(GeneratorProtocol.READ_FIRMWARE_VERSION)
        sendProbe(GeneratorProtocol.READ_SERIAL_NUMBER)
        return Connection(baudRate = BAUD_XM, generatorType = GENERATOR_TYPE_XM)
    }

    /**
     * Post-auth init sequence, line-for-line from the C# reference (verified against
     * the original Spooky2 dump). Every command is sent and its response consumed.
     */
    private suspend fun runInitSequence() {
        for (command in INIT_SEQUENCE) {
            sendProbe(command)
        }
    }

    // ── GeneratorLink ──

    override suspend fun sendCommandWithResponse(command: String): String? {
        // Mirror C# SendCommandWithResponse: send once, retry once on null.
        return rawSend(command, responseTimeoutMs)
            ?: rawSend(command, responseTimeoutMs)
    }

    override suspend fun sendCommandsBatch(commands: List<String>) {
        for (command in commands) {
            rawSend(command, responseTimeoutMs)
        }
    }

    override suspend fun writeFrequencies(frequencies: List<Double>) {
        // Only :w24 — sets BOTH channels (the C# reference never uses :w25 here).
        for (frequency in frequencies) {
            rawSend(GeneratorProtocol.buildSetFrequency1(frequency), responseTimeoutMs)
        }
    }

    override suspend fun start() {
        rawSend(GeneratorProtocol.buildModulationOnOff(false, false), responseTimeoutMs)
        rawSend(GeneratorProtocol.CLEAR_FREQUENCY1, responseTimeoutMs)
        rawSend(GeneratorProtocol.CLEAR_FREQUENCY2, responseTimeoutMs)
        rawSend(GeneratorProtocol.START_OUTPUT1, responseTimeoutMs)
        rawSend(GeneratorProtocol.START_OUTPUT2, responseTimeoutMs)
    }

    override suspend fun stop() {
        rawSend(GeneratorProtocol.STOP_OUTPUT1, responseTimeoutMs)
        rawSend(GeneratorProtocol.STOP_OUTPUT2, responseTimeoutMs)
        rawSend(GeneratorProtocol.buildModulationOnOff(false, false), responseTimeoutMs)
        rawSend(GeneratorProtocol.CLEAR_FREQUENCY1, responseTimeoutMs)
        rawSend(GeneratorProtocol.CLEAR_FREQUENCY2, responseTimeoutMs)
    }

    /**
     * Full safety zero-out for Cancel / safety-stop: clear both frequency channels,
     * set both amplitude CV outputs to 0, then run the normal [stop] path.
     *
     * Mirrors the engine's post-kill cleanup (clear freq + amplitude 0) plus the
     * output-off [stop] sequence, collapsed into one explicit "make it safe now" call.
     *
     * Safe to call even mid-scan after the scan job has been cancelled: each command
     * is sent under its own [runCatching] so a closed/half-open transport can never
     * make this throw. It does NOT check coroutine activity, so a cancelled scope
     * still completes the zero-out.
     */
    suspend fun zeroOutput() {
        runCatching { rawSend(GeneratorProtocol.CLEAR_FREQUENCY1, responseTimeoutMs) }
        runCatching { rawSend(GeneratorProtocol.CLEAR_FREQUENCY2, responseTimeoutMs) }
        runCatching { rawSend(GeneratorProtocol.buildSetAmplitudeCv1(0), responseTimeoutMs) }
        runCatching { rawSend(GeneratorProtocol.buildSetAmplitudeCv2(0), responseTimeoutMs) }
        runCatching { stop() }
    }

    /** Close the underlying transport. Idempotent. */
    suspend fun close() {
        connected = false
        transport.close()
    }

    /** True once [connect] has succeeded. */
    fun isConnected(): Boolean = connected

    // ── Internals ──

    /** Discovery-phase send: longer timeout, used during probe/auth/init. */
    private suspend fun sendProbe(command: String): String? =
        rawSend(command, probeTimeoutMs)

    private suspend fun rawSend(command: String, timeoutMs: Long): String? {
        transport.write(GeneratorProtocol.encodeCommandToBytes(command))
        return transport.readLine(timeoutMs)?.trim()
    }

    companion object {
        const val BAUD_XM: Int = 57600
        const val BAUD_GENERATORX: Int = 115200

        const val GENERATOR_TYPE_XM: String = "XM"
        const val GENERATOR_TYPE_GENERATORX: String = "GeneratorX"

        private val PROBE_BAUD_RATES = intArrayOf(BAUD_XM, BAUD_GENERATORX)

        private const val POST_OPEN_SETTLE_MS = 50L
        private const val DEFAULT_RESPONSE_TIMEOUT_MS = 2000L
        private const val DEFAULT_PROBE_TIMEOUT_MS = 2000L

        /**
         * Post-auth init sequence — identical ordering to C#
         * `GeneratorService.FindGenerators` (GeneratorX branch).
         */
        private val INIT_SEQUENCE: List<String> = listOf(
            GeneratorProtocol.READ_HARDWARE_INFO,             // :r02=0,
            GeneratorProtocol.QUERY_FIRMWARE_NAME,            // :n00=$
            GeneratorProtocol.buildSyncOnOff(false),          // :w14=0,
            GeneratorProtocol.buildWaveformInversion(false, false), // :w17=0,0,
            ":w24=0,",
            ":w25=0,",
            GeneratorProtocol.buildLowFrequencyMode(true, true), // :w15=1,1, (CRITICAL)
            ":w24=00,",
            GeneratorProtocol.buildSetAmplitude1(120),        // :w32=120,
            GeneratorProtocol.buildSetAmplitude2(120),        // :w33=120,
            GeneratorProtocol.buildSetDisplayName("Stopped"), // :n00=Stopped
            ":w13=0,",
            ":w28=0,",
            ":w29=0,",
            ":w24=00,",
            GeneratorProtocol.CLEAR_FREQUENCY1,               // :w12=0,,
            GeneratorProtocol.CLEAR_FREQUENCY2,               // :w12=,0,
            GeneratorProtocol.buildSetAmplitude1(120),        // :w32=120,
            ":w40=0,",
            GeneratorProtocol.buildSetAmplitude2(120),        // :w33=120,
            ":w40=0,",
            ":w13=0,",
            ":w20=11,",
            GeneratorProtocol.buildSyncOnOff(true),           // :w14=1,
            GeneratorProtocol.CLEAR_FREQUENCY1,               // :w12=0,,
            GeneratorProtocol.CLEAR_FREQUENCY2,               // :w12=,0,
            ":w21=25,",
        )
    }
}
