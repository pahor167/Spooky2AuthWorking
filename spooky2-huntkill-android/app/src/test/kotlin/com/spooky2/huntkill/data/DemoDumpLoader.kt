package com.spooky2.huntkill.data

import com.spooky2.huntkill.transport.fake.FakeTransport

/**
 * TEST-ONLY replay helper. Builds a no-hardware [FakeTransport] from a parsed dump so
 * the JVM ViewModel tests ([com.spooky2.huntkill.ui.hunt.HuntViewModelTest] and friends)
 * can drive the full Hunt→Kill flow without a device.
 *
 * The runtime app no longer ships a demo path — production connects over USB only — so
 * this and its sibling replay classes ([PlainTextDumpParser], [TransportFactory],
 * [GeneratorSessionFactory]) live in the test source set as the verification backbone.
 * Tests read the dump fixtures from `src/test/resources/dumps/` themselves and construct
 * [DemoData] directly, so this object only exposes the transport builder + data holder.
 */
object DemoDumpLoader {

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

    /** Parsed demo session plus optional recorded handshake fixture. */
    data class DemoData(
        val session: PlainTextDumpParser.HuntAndKillSession,
        val handshake: FakeTransport.HandshakeFixture?,
    )
}
