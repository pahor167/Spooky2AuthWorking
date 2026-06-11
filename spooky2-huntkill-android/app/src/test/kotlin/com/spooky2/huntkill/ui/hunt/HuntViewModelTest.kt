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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * JVM ViewModel test: a full demo Hunt (driven by FakeTransport replaying the bundled
 * `FullHuntAndKill` dump, through GeneratorClient + ScanEngine) produces the golden 10
 * hits. Mirrors the transport end-to-end wiring but exercises [HuntViewModel] and its
 * StateFlow.
 *
 * Main is set to an [UnconfinedTestDispatcher] so `viewModelScope` dispatches eagerly;
 * the scan itself runs on `Dispatchers.Default` with zero-delay demo params, so the
 * test simply polls the StateFlow until the terminal state (real wall-clock, fast).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class HuntViewModelTest {

    private val goldenDeviations = listOf(
        96.25,
        82.05000000000291,
        77.80000000000291,
        73.55000000000291,
        72.05000000000291,
        69.34999999999854,
        61.349999999998545,
        56.5,
        55.900000000001455,
        55.69999999999709,
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `full demo hunt produces the golden ten hits`() = runBlocking {
        val demoData = loadDemoData()
        val transportFactory = TransportFactory { DemoDumpLoader.fakeTransportFor(demoData) }
        val sessionFactory = GeneratorSessionFactory(
            transportFactory = transportFactory,
            handshake = demoData.handshake,
            connectViaProbe = false,
        )
        val holder = SessionHolder()
        holder.set(sessionFactory.connect())
        holder.setReconnect { sessionFactory.connect() }

        val viewModel = HuntViewModel(holder, LogBus())
        viewModel.updateDwellSeconds("0") // zero-dwell keeps the JVM test fast
        // Repeat defaults ON (kill loops forever); turn it off so the single-pass kill
        // completes and the run reaches Done for this end-to-end assertion.
        viewModel.toggleRepeatKill()
        viewModel.startHunt()

        val terminal = awaitTerminalState(viewModel, timeoutMs = 60_000)

        assertEquals(HuntPhase.Done, terminal.phase)
        assertEquals(10, terminal.hits.size)
        assertEquals(goldenDeviations, terminal.hits.map { it.deviation })

        val distinctBands = terminal.hits.map { Math.round(it.frequency / 100_000.0) }.distinct().size
        assertTrue("expected >= 3 distinct bands, got $distinctBands", distinctBands >= 3)
    }

    @Test
    fun `re-running hunt produces ten hits each time`() = runBlocking {
        val demoData = loadDemoData()
        val transportFactory = TransportFactory { DemoDumpLoader.fakeTransportFor(demoData) }
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
        // Repeat defaults ON; disable so each single-pass kill completes (reaches Done).
        viewModel.toggleRepeatKill()

        // First run.
        viewModel.startHunt()
        val first = awaitTerminalState(viewModel, timeoutMs = 60_000)
        assertEquals(HuntPhase.Done, first.phase)
        assertEquals(10, first.hits.size)

        // Second run on the SAME ViewModel: startHunt rebuilds a fresh session, so the
        // single-use FakeTransport replay is reset and the golden 10 hits reappear
        // (the regression this fixes produced 0 hits on the stale session).
        viewModel.startHunt()
        val second = awaitTerminalState(viewModel, timeoutMs = 60_000)
        assertEquals(HuntPhase.Done, second.phase)
        assertEquals(goldenDeviations, second.hits.map { it.deviation })
    }

    private fun awaitTerminalState(viewModel: HuntViewModel, timeoutMs: Long): HuntUiState {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            val state = viewModel.state.value
            if (state.phase == HuntPhase.Done || state.phase == HuntPhase.Error) return state
            Thread.sleep(10)
        }
        throw AssertionError("Hunt did not reach a terminal state within ${timeoutMs}ms")
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
