package com.spooky2.huntkill.ui.hunt

import com.spooky2.huntkill.data.DemoDumpLoader
import com.spooky2.huntkill.data.GeneratorSessionFactory
import com.spooky2.huntkill.data.PlainTextDumpParser
import com.spooky2.huntkill.data.SessionHolder
import com.spooky2.huntkill.data.TransportFactory
import com.spooky2.huntkill.log.LogBus
import com.spooky2.huntkill.transport.SerialTransport
import com.spooky2.huntkill.transport.fake.FakeTransport
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.Collections

/**
 * Verifies the Cancel / safety-stop path:
 *  1. issues the zero-out command sequence — `:w12=0,,`, `:w12=,0,`, `:w28=0,`,
 *     `:w29=0,` plus the stop outputs — on the wire, and
 *  2. transitions the ViewModel to [HuntPhase.Cancelled] so the UI can navigate back.
 *
 * Drives a real [HuntViewModel] against the bundled demo dump (same wiring as
 * [HuntViewModelTest]) through a [RecordingTransport] that captures every command.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class HuntViewModelCancelTest {

    @Before
    fun setUp() {
        // Plain Unconfined (real wall-clock delays, not the virtual-time test
        // scheduler) so the cancel/zero-out path — which runs on viewModelScope (Main)
        // and awaits real transport delays — actually completes under runBlocking.
        Dispatchers.setMain(Dispatchers.Unconfined)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    /** Captures each written command (thread-safe) for post-cancel assertions. */
    private class RecordingTransport(
        private val delegate: SerialTransport,
        val commands: MutableList<String> = Collections.synchronizedList(mutableListOf()),
    ) : SerialTransport {
        override val isOpen: Boolean get() = delegate.isOpen
        override suspend fun open(baudRate: Int) = delegate.open(baudRate)
        override suspend fun write(bytes: ByteArray) {
            commands.add(String(bytes, Charsets.US_ASCII).trim())
            delegate.write(bytes)
        }
        override suspend fun readLine(timeoutMs: Long): String? {
            // Slow the sweep so the test can cancel while it is genuinely running.
            delay(20)
            return delegate.readLine(timeoutMs)
        }
        override suspend fun close() = delegate.close()
    }

    @Test
    fun `cancel issues zero-out sequence and transitions to Cancelled`() = runBlocking {
        val demoData = loadDemoData()
        val recorded = Collections.synchronizedList(mutableListOf<String>())
        val transportFactory = TransportFactory {
            RecordingTransport(DemoDumpLoader.fakeTransportFor(demoData), recorded)
        }
        val sessionFactory = GeneratorSessionFactory(
            transportFactory = transportFactory,
            handshake = demoData.handshake,
            connectViaProbe = false,
        )
        val holder = SessionHolder()
        holder.set(sessionFactory.connect())
        holder.setReconnect { sessionFactory.connect() }

        val viewModel = HuntViewModel(holder, LogBus())
        viewModel.updateDwellSeconds("0")
        viewModel.startHunt()

        awaitSweepInProgress(viewModel)

        // Clear the recording so we only assert on commands sent BY the cancel path.
        recorded.clear()
        viewModel.cancel()

        // Wait for the Cancelled transition (cancel zeroes the output asynchronously).
        awaitPhase(viewModel, HuntPhase.Cancelled)
        assertEquals(HuntPhase.Cancelled, viewModel.state.value.phase)

        val sent = synchronized(recorded) { recorded.toList() }
        assertTrue("clear freq ch1 (:w12=0,,) sent", sent.contains(":w12=0,,"))
        assertTrue("clear freq ch2 (:w12=,0,) sent", sent.contains(":w12=,0,"))
        assertTrue("amplitude CV1 -> 0 (:w28=0,) sent", sent.contains(":w28=0,"))
        assertTrue("amplitude CV2 -> 0 (:w29=0,) sent", sent.contains(":w29=0,"))
        assertTrue("stop output 1 (:w610) sent", sent.contains(":w610"))
        assertTrue("stop output 2 (:w620) sent", sent.contains(":w620"))
    }

    // Suspending polls (delay, not Thread.sleep) so the runBlocking event loop keeps
    // dispatching the ViewModel's coroutines (the cancel/zero-out path runs on Main).
    private suspend fun awaitSweepInProgress(viewModel: HuntViewModel) {
        val deadline = System.currentTimeMillis() + 30_000
        while (System.currentTimeMillis() < deadline) {
            val s = viewModel.state.value
            if (s.phase == HuntPhase.Hunting && s.currentFrequency > 0.0) return
            if (s.phase == HuntPhase.Done || s.phase == HuntPhase.Error) {
                throw AssertionError("Hunt ended before it could be cancelled: ${s.phase}")
            }
            delay(5)
        }
        throw AssertionError("Sweep never started within timeout")
    }

    private suspend fun awaitPhase(viewModel: HuntViewModel, phase: HuntPhase) {
        val deadline = System.currentTimeMillis() + 30_000
        while (System.currentTimeMillis() < deadline) {
            if (viewModel.state.value.phase == phase) return
            delay(5)
        }
        throw AssertionError("ViewModel did not reach $phase within timeout")
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
