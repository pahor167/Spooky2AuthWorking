package com.spooky2.huntkill.transport

import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbManager
import com.hoho.android.usbserial.driver.UsbSerialDriver
import com.hoho.android.usbserial.driver.UsbSerialPort
import com.spooky2.huntkill.transport.usb.UsbCdcSerialTransport
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Verifies dual-port handling: [UsbCdcSerialTransport.countPorts] reports the driver's
 * port count, and constructing the transport with `portIndex = 1` opens the SECOND
 * serial port (the second generator inside one GeneratorX USB device).
 *
 * The static driver resolution is stubbed via [mockkObject] so the test runs on the JVM
 * without a real usb-serial driver/probe.
 */
class UsbCdcSerialTransportPortTest {

    @After
    fun tearDown() {
        unmockkObject(UsbCdcSerialTransport)
    }

    private fun twoPortDriver(): Pair<UsbSerialDriver, List<UsbSerialPort>> {
        val port0 = mockk<UsbSerialPort>(relaxed = true)
        val port1 = mockk<UsbSerialPort>(relaxed = true)
        val driver = mockk<UsbSerialDriver>()
        every { driver.ports } returns listOf(port0, port1)
        return driver to listOf(port0, port1)
    }

    @Test
    fun `countPorts returns the driver port count`() {
        val (driver, _) = twoPortDriver()
        val usbManager = mockk<UsbManager>()
        val device = mockk<UsbDevice>(relaxed = true)

        mockkObject(UsbCdcSerialTransport)
        every { UsbCdcSerialTransport.resolveDriver(usbManager, device) } returns driver

        assertEquals(2, UsbCdcSerialTransport.countPorts(usbManager, device))
    }

    @Test
    fun `countPorts returns zero when no driver`() {
        val usbManager = mockk<UsbManager>()
        val device = mockk<UsbDevice>(relaxed = true)

        mockkObject(UsbCdcSerialTransport)
        every { UsbCdcSerialTransport.resolveDriver(usbManager, device) } returns null

        assertEquals(0, UsbCdcSerialTransport.countPorts(usbManager, device))
    }

    @Test
    fun `open with portIndex 1 opens the second serial port`() = runTest {
        val (driver, ports) = twoPortDriver()
        val usbManager = mockk<UsbManager>()
        val device = mockk<UsbDevice>(relaxed = true)
        val connection = mockk<UsbDeviceConnection>(relaxed = true)
        every { usbManager.openDevice(device) } returns connection

        mockkObject(UsbCdcSerialTransport)
        every { UsbCdcSerialTransport.resolveDriver(usbManager, device) } returns driver

        val transport = UsbCdcSerialTransport(usbManager = usbManager, device = device, portIndex = 1)
        transport.open(baudRate = 115200)

        // The SECOND port (index 1) is opened, not the first.
        coVerify(exactly = 1) { ports[1].open(connection) }
        coVerify(exactly = 0) { ports[0].open(any()) }
    }
}
