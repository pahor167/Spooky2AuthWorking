package com.spooky2.huntkill.data

import com.spooky2.huntkill.transport.SerialTransport

/**
 * TEST-ONLY factory producing a fresh [SerialTransport] per connection attempt.
 *
 * Used by the JVM ViewModel replay tests to build a no-hardware
 * [FakeTransport][com.spooky2.huntkill.transport.fake.FakeTransport] per hunt. The
 * runtime app connects over USB directly (see
 * [com.spooky2.huntkill.data.UsbConnectionManager]) and does not use this seam.
 */
fun interface TransportFactory {
    /** Create a new, unopened transport. The caller opens/authenticates it. */
    fun create(): SerialTransport
}
