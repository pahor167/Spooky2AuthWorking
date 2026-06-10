package com.spooky2.huntkill.data

import com.spooky2.huntkill.transport.SerialTransport

/**
 * Produces a fresh [SerialTransport] per connection attempt.
 *
 * The default runtime binding is [DemoTransportFactory] (no hardware — replays the
 * bundled dump). A real USB path can replace this binding later by providing a
 * `UsbTransportFactory` that resolves an attached [com.spooky2.huntkill.transport.usb.UsbCdcSerialTransport]
 * — see `DemoModule` for the Hilt wiring and qualifier.
 */
fun interface TransportFactory {
    /** Create a new, unopened transport. The caller opens/authenticates it. */
    fun create(): SerialTransport
}
