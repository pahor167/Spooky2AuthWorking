package com.spooky2.huntkill.ui.hunt

import com.spooky2.huntkill.core.scan.ScanEngine
import com.spooky2.huntkill.data.GeneratorSession
import com.spooky2.huntkill.data.SessionHolder
import com.spooky2.huntkill.data.UsbConnectionManager
import com.spooky2.huntkill.log.LogBus
import com.spooky2.huntkill.transport.GeneratorClient
import com.spooky2.huntkill.transport.SerialTransport
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Verifies that a USB device-detach event received while a hunt is running:
 *  - cancels the hunt job,
 *  - transitions phase to [HuntPhase.Error],
 *  - sets statusText / errorMessage to a message containing "unplugged",
 *  - clears busyAction and isPaused, and
 *  - resets rescanInProgress to false.
 *
 * Uses a mockk stub for [UsbConnectionManager] so no Android context is needed,
 * exposing a real [MutableSharedFlow] as [UsbConnectionManager.deviceDetached].
 */
@OptIn(ExperimentalCoroutinesApi::class)
class HuntViewModelDetachTest {

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    /** Minimal always-open transport that hangs on readLine so the sweep never finishes. */
    private class HangingTransport : SerialTransport {
        override val isOpen: Boolean = true
        override suspend fun open(baudRate: Int) = Unit
        override suspend fun close() = Unit
        override suspend fun write(bytes: ByteArray) = Unit
        override suspend fun readLine(timeoutMs: Long): String? {
            delay(Long.MAX_VALUE)
            return null
        }
    }

    private fun buildSession(transport: SerialTransport): GeneratorSession {
        val client = GeneratorClient(transport = transport)
        runBlocking { transport.open(GeneratorClient.BAUD_GENERATORX) }
        return GeneratorSession(
            baudRate = GeneratorClient.BAUD_GENERATORX,
            generatorType = GeneratorClient.GENERATOR_TYPE_GENERATORX,
            authToken = null,
            transport = transport,
            client = client,
            engine = ScanEngine(client),
            isDemo = true,
        )
    }

    @Test
    fun `USB detach cancels hunt job and sets Error phase with unplugged message`() = runBlocking {
        // A real MutableSharedFlow lets us inject the detach event from the test.
        val detachFlow = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
        val manager = mockk<UsbConnectionManager>(relaxed = true)
        every { manager.deviceDetached } returns detachFlow

        val transport = HangingTransport()
        val holder = SessionHolder()
        holder.set(buildSession(transport))

        val viewModel = HuntViewModel(
            sessionHolder = holder,
            log = LogBus(),
            usbConnectionManager = manager,
        )
        viewModel.updateDwellSeconds("0")
        viewModel.startHunt()

        // Wait until hunting is genuinely in progress.
        val deadline = System.currentTimeMillis() + 10_000
        while (System.currentTimeMillis() < deadline) {
            if (viewModel.state.value.phase == HuntPhase.Hunting) break
            delay(5)
        }
        assertEquals(HuntPhase.Hunting, viewModel.state.value.phase)

        // Simulate device detach.
        detachFlow.emit(Unit)

        // Wait for the Error transition.
        val errorDeadline = System.currentTimeMillis() + 10_000
        while (System.currentTimeMillis() < errorDeadline) {
            if (viewModel.state.value.phase == HuntPhase.Error) break
            delay(5)
        }

        val state = viewModel.state.value
        assertEquals(HuntPhase.Error, state.phase)
        assertTrue(
            "statusText should mention unplugged, was: ${state.statusText}",
            state.statusText.contains("unplugged", ignoreCase = true),
        )
        assertTrue(
            "errorMessage should mention unplugged, was: ${state.errorMessage}",
            state.errorMessage?.contains("unplugged", ignoreCase = true) == true,
        )
        assertFalse("isPaused should be false after detach", state.isPaused)
        assertFalse("rescanInProgress should be false after detach", state.rescanInProgress)
        assertNull("busyAction should be null after detach", state.busyAction)
    }
}
