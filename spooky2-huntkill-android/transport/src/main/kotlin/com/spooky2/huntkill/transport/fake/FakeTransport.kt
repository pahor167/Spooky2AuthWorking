package com.spooky2.huntkill.transport.fake

import com.spooky2.huntkill.core.protocol.GeneratorProtocol
import com.spooky2.huntkill.transport.SerialTransport
import kotlin.math.min
import kotlin.math.roundToLong

/**
 * A no-hardware [SerialTransport] that replays a recorded generator session so the
 * full Hunt→Kill flow runs offline.
 *
 * Unlike a blind tape that returns recorded lines positionally, this emulates the
 * *device*: it inspects each written command and answers like a GeneratorX would.
 *   - Standalone angle reads (`:r11=,`) and current reads (`:r12=,`) return the next
 *     recorded baseline reading, then the next recorded sweep reading, in order —
 *     the same counting logic as the core `ReplayGeneratorLink` golden test, lifted
 *     to the transport (line) level.
 *   - The GeneratorX auth handshake is answered from [handshake] when provided, so a
 *     [com.spooky2.huntkill.transport.GeneratorClient] can complete its full
 *     challenge-response + init sequence against the fake.
 *   - Every other command returns `":ok"`.
 *
 * It is parser-agnostic: callers extract [baselineReadings]/[sweepSteps] from a dump
 * (see the test's `PlainTextDumpParser`) and hand them in, keeping this main-source
 * class free of any test-only dump format.
 */
class FakeTransport(
    private val baselineReadings: List<SensorPair>,
    private val sweepSteps: List<SensorPair>,
    private val handshake: HandshakeFixture? = null,
) : SerialTransport {

    /** A paired angle/current sensor reading recorded from the device. */
    data class SensorPair(val angle: Double, val current: Double)

    /**
     * Recorded GeneratorX auth exchange. When set, the fake answers the challenge
     * (`:r90=<challenge>,`) with `:r90=<echo>,<deviceResponse>.` and the auth write
     * (`:w92=...`) with `:ok`, letting the client run its real handshake.
     */
    data class HandshakeFixture(
        val echo: String,
        val deviceResponse: String,
    )

    private var open = false
    private var totalAngleReads = 0
    private var totalCurrentReads = 0

    /** The single response queued by the most recent [write], drained by [readLine]. */
    private var pendingResponse: String? = null

    override val isOpen: Boolean
        get() = open

    override suspend fun open(baudRate: Int) {
        open = true
        totalAngleReads = 0
        totalCurrentReads = 0
        pendingResponse = null
    }

    override suspend fun write(bytes: ByteArray) {
        check(open) { "FakeTransport.write before open()" }
        val command = String(bytes, Charsets.US_ASCII).trim()
        pendingResponse = respondTo(command)
    }

    override suspend fun readLine(timeoutMs: Long): String? {
        val response = pendingResponse
        pendingResponse = null
        return response
    }

    override suspend fun close() {
        open = false
        pendingResponse = null
    }

    private fun respondTo(command: String): String {
        when (command) {
            GeneratorProtocol.READ_ANGLE -> { // ":r11=,"
                totalAngleReads++
                return ":r11=${format(nextAngle())}."
            }
            GeneratorProtocol.READ_CURRENT -> { // ":r12=,"
                totalCurrentReads++
                return ":r12=${format(nextCurrent())}."
            }
        }

        // GeneratorX challenge: ":r90=<challenge>,"
        if (command.startsWith(":r90=") && handshake != null && !command.startsWith(":r90=,")) {
            val challenge = command.removePrefix(":r90=").trimEnd(',')
            if (challenge.length == 9) {
                return ":r90=${handshake.echo},${handshake.deviceResponse}."
            }
        }

        return ":ok"
    }

    /**
     * Baseline angle reads come first (the engine does 1 standalone angle read plus
     * one per baseline sample), then sweep-step angle reads, matching the core
     * golden `ReplayGeneratorLink`.
     */
    private fun nextAngle(): Double {
        val baselineTotal = 1 + baselineReadings.size
        return if (totalAngleReads <= baselineTotal) {
            val idx = min(totalAngleReads - 1, baselineReadings.size - 1)
            if (idx >= 0) baselineReadings[idx].angle else FALLBACK_ANGLE
        } else {
            val sweepIdx = totalAngleReads - baselineTotal - 1
            sweepSteps.getOrNull(sweepIdx)?.angle ?: FALLBACK_ANGLE
        }
    }

    private fun nextCurrent(): Double {
        return if (totalCurrentReads <= baselineReadings.size) {
            baselineReadings[totalCurrentReads - 1].current
        } else {
            val sweepIdx = totalCurrentReads - baselineReadings.size - 1
            sweepSteps.getOrNull(sweepIdx)?.current ?: FALLBACK_CURRENT
        }
    }

    /** Render a whole-number reading without a trailing ".0" (matches recorded wire format). */
    private fun format(value: Double): String {
        val rounded = value.roundToLong()
        return if (rounded.toDouble() == value) rounded.toString() else value.toString()
    }

    companion object {
        private const val FALLBACK_ANGLE = 52000.0
        private const val FALLBACK_CURRENT = 6900.0
    }
}
