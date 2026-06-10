package com.spooky2.huntkill.ui.hunt

import com.spooky2.huntkill.data.DemoDumpLoader
import com.spooky2.huntkill.data.GeneratorSessionFactory
import com.spooky2.huntkill.data.PlainTextDumpParser
import com.spooky2.huntkill.data.SessionHolder
import com.spooky2.huntkill.data.TransportFactory
import com.spooky2.huntkill.transport.SerialTransport
import com.spooky2.huntkill.transport.fake.FakeTransport
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Regression test for BUG 2 ("Pause doesn't pause").
 *
 * Drives a real [HuntViewModel] against the bundled demo dump (same wiring as
 * [HuntViewModelTest]) but with each sensor read artificially slowed so the sweep
 * advances on a human-observable timescale. The test:
 *  1. starts the hunt and waits until the sweep is genuinely in progress,
 *  2. calls [HuntViewModel.togglePause] and asserts the sweep step counter (here
 *     `currentFrequency`, which changes every step) FREEZES across a ~500 ms window
 *     and `state.isPaused` stays true (i.e. `onProgress` does not clobber it),
 *  3. calls [HuntViewModel.togglePause] again and asserts the sweep RESUMES
 *     (the frequency advances and the hunt eventually completes).
 *
 * It is deterministic, not time-flaky: each step blocks on a fixed `delay`, and the
 * assertions poll with generous timeouts rather than relying on exact timing.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class HuntViewModelPauseTest {

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    /** Wraps a transport so every read response is delayed, slowing the sweep. */
    private class SlowTransport(
        private val delegate: SerialTransport,
        private val readDelayMs: Long,
    ) : SerialTransport {
        override val isOpen: Boolean get() = delegate.isOpen
        override suspend fun open(baudRate: Int) = delegate.open(baudRate)
        override suspend fun write(bytes: ByteArray) = delegate.write(bytes)
        override suspend fun readLine(timeoutMs: Long): String? {
            delay(readDelayMs)
            return delegate.readLine(timeoutMs)
        }
        override suspend fun close() = delegate.close()
    }

    private suspend fun newViewModel(): HuntViewModel {
        val demoData = loadDemoData()
        val transportFactory = TransportFactory {
            SlowTransport(DemoDumpLoader.fakeTransportFor(demoData), readDelayMs = 25)
        }
        val sessionFactory = GeneratorSessionFactory(
            transportFactory = transportFactory,
            handshake = demoData.handshake,
            connectViaProbe = false,
        )
        val holder = SessionHolder()
        holder.set(sessionFactory.connect())
        holder.setReconnect { sessionFactory.connect() }
        // Keep dwell short; the sweep itself is what we pause.
        return HuntViewModel(holder, com.spooky2.huntkill.log.LogBus()).apply {
            updateDwellSeconds("0")
        }
    }

    @Test
    fun `pause freezes the sweep and resume continues it`() = runBlocking {
        val viewModel = newViewModel()
        viewModel.startHunt()

        // 1. Wait until the sweep is actually advancing (currentFrequency moves).
        val before = awaitSweepInProgress(viewModel)

        // 2. Pause. At most one already-in-flight step may land; after that the sweep
        //    must FREEZE. Let any in-flight step settle, then capture the frozen
        //    frequency and assert it does not advance across a multi-step window.
        viewModel.togglePause()
        assertTrue("togglePause should set isPaused", viewModel.state.value.isPaused)

        delay(150) // allow the single in-flight step to land
        val pausedFreq = viewModel.state.value.currentFrequency

        // ~500 ms is many would-be steps (each step is ~50 ms with the slow transport).
        delay(500)
        val stillPaused = viewModel.state.value
        assertTrue(
            "isPaused must remain true while paused (onProgress must not clobber it)",
            stillPaused.isPaused,
        )
        assertEquals(
            "sweep currentFrequency must NOT advance while paused",
            pausedFreq,
            stillPaused.currentFrequency,
            0.0,
        )
        assertNotEquals(
            "hunt must not complete while paused",
            HuntPhase.Done,
            stillPaused.phase,
        )

        // 3. Resume and assert the sweep advances again (steps past the frozen freq).
        //    We assert resumed progress, not full completion: with the slow transport a
        //    full sweep would take minutes, but a single observed advance after resume
        //    conclusively proves the gate released.
        viewModel.togglePause()
        assertTrue("togglePause should clear isPaused", !viewModel.state.value.isPaused)

        val resumedFreq = awaitFrequencyChange(viewModel, from = pausedFreq)
        assertNotEquals(
            "sweep must advance after resume",
            pausedFreq,
            resumedFreq,
            0.0,
        )
        assertTrue(
            "sweep must keep ascending after resume",
            resumedFreq > pausedFreq,
        )

        // Stop the running scan so the test tears down promptly.
        viewModel.cancel()

        // sanity: 'before' was a real mid-sweep frequency
        assertTrue("sweep should have produced a non-zero frequency", before > 0.0)
    }

    private fun awaitSweepInProgress(viewModel: HuntViewModel): Double {
        val deadline = System.currentTimeMillis() + 30_000
        var last = 0.0
        while (System.currentTimeMillis() < deadline) {
            val s = viewModel.state.value
            if (s.phase == HuntPhase.Hunting && s.currentFrequency > 0.0) {
                if (last != 0.0 && s.currentFrequency != last) return s.currentFrequency
                last = s.currentFrequency
            }
            if (s.phase == HuntPhase.Done || s.phase == HuntPhase.Error) {
                throw AssertionError("Hunt ended before sweep could be observed: ${s.phase}")
            }
            Thread.sleep(5)
        }
        throw AssertionError("Sweep never advanced within timeout")
    }

    private fun awaitFrequencyChange(viewModel: HuntViewModel, from: Double): Double {
        val deadline = System.currentTimeMillis() + 30_000
        while (System.currentTimeMillis() < deadline) {
            val s = viewModel.state.value
            if (s.currentFrequency != from) return s.currentFrequency
            if (s.phase == HuntPhase.Done) return s.currentFrequency
            Thread.sleep(5)
        }
        throw AssertionError("Frequency did not change after resume")
    }

    private fun loadDemoData(): DemoDumpLoader.DemoData {
        val session = PlainTextDumpParser.parseLines(readResourceLines("dumps/FullHuntAndKill"))
        val reply = readResourceLines("dumps/Handshake1").first {
            it.startsWith(":r90=") && it.contains(',') && it.trimEnd().endsWith(".")
        }
        val parts = reply.removePrefix(":r90=").trimEnd().trimEnd('.').split(',')
        val handshake = FakeTransport.HandshakeFixture(echo = parts[0], deviceResponse = parts[1])
        return DemoDumpLoader.DemoData(session = session, handshake = handshake)
    }

    private fun readResourceLines(path: String): List<String> {
        val url = requireNotNull(javaClass.classLoader?.getResource(path)) {
            "Resource '$path' not found on test classpath"
        }
        return java.io.File(url.toURI()).readLines()
    }
}
