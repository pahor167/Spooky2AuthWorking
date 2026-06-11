package com.spooky2.huntkill.ui.hunt

import com.spooky2.huntkill.data.DemoDumpLoader
import com.spooky2.huntkill.data.GeneratorSessionFactory
import com.spooky2.huntkill.data.PlainTextDumpParser
import com.spooky2.huntkill.data.SessionHolder
import com.spooky2.huntkill.data.TransportFactory
import com.spooky2.huntkill.log.LogBus
import com.spooky2.huntkill.transport.fake.FakeTransport
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * JVM tests for the History "Re-run" path: [HuntViewModel.startKillFromFrequencies]
 * builds the expected ScanResult list from saved frequencies, drives the kill, and
 * (unlike a fresh hunt) does NOT require a new sweep.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class HuntViewModelReRunTest {

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `startKillFromFrequencies sets hits and enters Killing then completes`() = runBlocking {
        val viewModel = connectedViewModel()
        val freqs = listOf(120000.0, 555000.0, 1000000.0)
        val deviations = listOf(10.0, 20.0, 30.0)

        val started = viewModel.startKillFromFrequencies(
            freqs = freqs,
            dwellSeconds = 0.0, // zero dwell keeps the JVM kill fast
            amplitudeCv = 1500,
            deviations = deviations,
        )

        assertTrue("re-run started on a connected session", started)
        // The synthesized hits carry exactly the saved frequencies + deviations.
        assertEquals(freqs, viewModel.state.value.hits.map { it.frequency })
        assertEquals(deviations, viewModel.state.value.hits.map { it.deviation })
        // Phase entered Killing (or already advanced to Done with zero dwell).
        val phase = viewModel.state.value.phase
        assertTrue("phase is Killing or Done", phase == HuntPhase.Killing || phase == HuntPhase.Done)

        // The kill runs the saved frequencies through the engine and finishes.
        val terminal = awaitDone(viewModel, timeoutMs = 30_000)
        assertEquals(HuntPhase.Done, terminal.phase)
        assertEquals(freqs, terminal.hits.map { it.frequency })
    }

    @Test
    fun `startKillFromFrequencies with no session is a no-op`() {
        // No session set in the holder → not connected.
        val viewModel = HuntViewModel(SessionHolder(), LogBus())
        val started = viewModel.startKillFromFrequencies(
            freqs = listOf(100000.0),
            dwellSeconds = 0.0,
            amplitudeCv = 1500,
        )
        assertFalse("re-run blocked without a session", started)
        assertTrue("no hits set", viewModel.state.value.hits.isEmpty())
        assertEquals(HuntPhase.Idle, viewModel.state.value.phase)
    }

    private fun connectedViewModel(): HuntViewModel {
        val demoData = loadDemoData()
        val transportFactory = TransportFactory { DemoDumpLoader.fakeTransportFor(demoData) }
        val sessionFactory = GeneratorSessionFactory(
            transportFactory = transportFactory,
            handshake = demoData.handshake,
            connectViaProbe = false,
        )
        val holder = SessionHolder()
        holder.set(runBlocking { sessionFactory.connect() })
        return HuntViewModel(holder, LogBus())
    }

    private fun awaitDone(viewModel: HuntViewModel, timeoutMs: Long): HuntUiState {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            val state = viewModel.state.value
            if (state.phase == HuntPhase.Done || state.phase == HuntPhase.Error) return state
            Thread.sleep(10)
        }
        throw AssertionError("Re-run kill did not reach a terminal state within ${timeoutMs}ms")
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
