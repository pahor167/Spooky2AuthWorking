package com.spooky2.huntkill.data

import com.spooky2.huntkill.transport.SerialTransport

/**
 * [TransportFactory] that produces a no-hardware [com.spooky2.huntkill.transport.fake.FakeTransport]
 * replaying the bundled demo dump. This is the DEFAULT runtime path: no USB device
 * is needed, so the full Hunt→Kill flow runs in an emulator.
 *
 * Demo data is parsed once (eagerly, by the DI module) and a fresh transport is
 * created per connection so each session replays from the start.
 */
class DemoTransportFactory(
    private val demoData: DemoDumpLoader.DemoData,
) : TransportFactory {
    override fun create(): SerialTransport = DemoDumpLoader.fakeTransportFor(demoData)
}
