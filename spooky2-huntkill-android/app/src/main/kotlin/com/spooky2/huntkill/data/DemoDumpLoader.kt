package com.spooky2.huntkill.data

import android.content.Context
import com.spooky2.huntkill.transport.fake.FakeTransport

/**
 * Builds a no-hardware [FakeTransport] from the bundled demo dump asset so the full
 * Hunt→Kill flow runs in an emulator with zero hardware.
 *
 * Mirrors the wiring the transport end-to-end test performs, but sources the dump
 * from `assets/dumps/FullHuntAndKill` (read at runtime) instead of a test resource
 * file, and supplies a [FakeTransport.HandshakeFixture] parsed from the recorded
 * `assets/dumps/Handshake1` so a real [com.spooky2.huntkill.transport.GeneratorClient]
 * can complete its GeneratorX challenge-response + init against the fake.
 */
object DemoDumpLoader {

    const val DUMP_ASSET_PATH = "dumps/FullHuntAndKill"
    const val HANDSHAKE_ASSET_PATH = "dumps/Handshake1"

    /** Parse the bundled session and the recorded handshake into reusable demo data. */
    fun loadDemoData(context: Context): DemoData {
        val session = PlainTextDumpParser.parseLines(readAssetLines(context, DUMP_ASSET_PATH))
        val handshake = parseHandshake(readAssetLines(context, HANDSHAKE_ASSET_PATH))
        return DemoData(session = session, handshake = handshake)
    }

    /** Build a fresh [FakeTransport] for the given demo data (one per connection). */
    fun fakeTransportFor(data: DemoData): FakeTransport =
        FakeTransport(
            baselineReadings = data.session.baselineReadings.map {
                FakeTransport.SensorPair(it.angle, it.current)
            },
            sweepSteps = data.session.sweepSteps.map {
                FakeTransport.SensorPair(it.angleReading, it.currentReading)
            },
            handshake = data.handshake,
        )

    private fun readAssetLines(context: Context, path: String): List<String> =
        context.assets.open(path).bufferedReader(Charsets.US_ASCII).use { it.readLines() }

    /**
     * Extract the device's echo + response from the recorded handshake dump.
     * The recorded line `:r90=<echo>,<deviceResponse>.` is the device's reply to the
     * client-issued challenge `:r90=<challenge>,`.
     */
    private fun parseHandshake(lines: List<String>): FakeTransport.HandshakeFixture? {
        val reply = lines.firstOrNull {
            it.startsWith(":r90=") && it.contains(',') && it.trimEnd().endsWith(".")
        } ?: return null
        val body = reply.removePrefix(":r90=").trimEnd().trimEnd('.')
        val parts = body.split(',')
        if (parts.size != 2 || parts[0].length != 9 || parts[1].length != 9) return null
        return FakeTransport.HandshakeFixture(echo = parts[0], deviceResponse = parts[1])
    }

    /** Parsed demo session plus optional recorded handshake fixture. */
    data class DemoData(
        val session: PlainTextDumpParser.HuntAndKillSession,
        val handshake: FakeTransport.HandshakeFixture?,
    )
}
