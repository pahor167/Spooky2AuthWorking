package com.spooky2.huntkill.data

import android.content.Context
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import com.spooky2.huntkill.log.LogBus
import com.spooky2.huntkill.transport.usb.UsbCdcSerialTransport
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

/**
 * Verifies the dual-generator enumeration: a single GeneratorX USB device that exposes
 * two serial ports yields TWO selectable [UsbGeneratorPort] entries (port index 0 and
 * 1, both pointing at the same physical device), while a single-port device yields one.
 *
 * The static driver/port resolution on [UsbCdcSerialTransport] is stubbed via mockk so
 * the test runs on the JVM with no real USB stack.
 */
class UsbConnectionManagerTest {

    @After
    fun tearDown() {
        unmockkObject(UsbCdcSerialTransport)
    }

    private fun manager(): UsbConnectionManager {
        val context = mockk<Context>(relaxed = true)
        val usbManager = mockk<UsbManager>(relaxed = true)
        return UsbConnectionManager(context, usbManager, LogBus())
    }

    private fun device(name: String): UsbDevice = mockk<UsbDevice>(relaxed = true).also {
        every { it.deviceName } returns name
        every { it.vendorId } returns 0x1A86
        every { it.productId } returns 0x55D2
    }

    @Test
    fun `dual-port device yields one entry per port`() {
        val gx = device("/dev/bus/usb/001/002")
        mockkObject(UsbCdcSerialTransport)
        every { UsbCdcSerialTransport.listSupportedDevices(any()) } returns listOf(gx)
        every { UsbCdcSerialTransport.countPorts(any(), gx) } returns 2

        val ports = manager().listGenerators()

        assertEquals("two generators on one device -> two entries", 2, ports.size)
        assertEquals(0, ports[0].portIndex)
        assertEquals(1, ports[1].portIndex)
        // Both entries point at the SAME physical USB device (one permission grant).
        assertSame(gx, ports[0].device)
        assertSame(gx, ports[1].device)
        assertEquals(2, ports[0].portCount)
    }

    @Test
    fun `single-port device yields one entry`() {
        val single = device("/dev/bus/usb/001/003")
        mockkObject(UsbCdcSerialTransport)
        every { UsbCdcSerialTransport.listSupportedDevices(any()) } returns listOf(single)
        every { UsbCdcSerialTransport.countPorts(any(), single) } returns 1

        val ports = manager().listGenerators()

        assertEquals(1, ports.size)
        assertEquals(0, ports[0].portIndex)
    }
}
