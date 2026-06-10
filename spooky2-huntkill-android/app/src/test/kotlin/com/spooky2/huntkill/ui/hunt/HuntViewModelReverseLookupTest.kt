package com.spooky2.huntkill.ui.hunt

import com.spooky2.huntkill.core.lookup.FrequencyDatabase
import com.spooky2.huntkill.core.lookup.ProgramEntry
import com.spooky2.huntkill.data.DemoDumpLoader
import com.spooky2.huntkill.data.FrequencyDatabaseSource
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
import java.util.concurrent.CountDownLatch

/**
 * Verifies the reverse-lookup wiring on [HuntViewModel]: after a demo hunt reaches Done,
 * each hit's matches are computed from the injected [FrequencyDatabaseSource] and stored
 * in [HuntUiState.lookupResults], and re-selecting the tolerance re-runs the lookup.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class HuntViewModelReverseLookupTest {

    /** In-memory database: one entry whose frequency sits inside the first golden hit. */
    private class FakeDatabaseSource(entries: List<ProgramEntry>) : FrequencyDatabaseSource {
        private val db = FrequencyDatabase(entries, skippedLines = 0, declaredCount = entries.size)
        var calls = 0
            private set

        override suspend fun database(): FrequencyDatabase {
            calls++
            return db
        }
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `done hunt populates per-hit lookup results and logs a summary`() = runBlocking {
        // Two probe entries: one whose frequency sits dead-centre on a real hit (must
        // match at any tolerance) and one far from every hit (must never match). Using a
        // hit value directly keeps the test robust against small replay-alignment shifts.
        val viewModel = buildViewModel(probeSource())

        viewModel.startHunt()
        val terminal = awaitDone(viewModel)
        assertEquals(HuntPhase.Done, terminal.phase)
        assertEquals(10, terminal.hits.size)

        val state = awaitLookupComplete(viewModel)
        // Seed the matchable entry's frequency to an actual hit so the match is exact.
        val onHit = state.hits.first().frequency

        // Re-run with an entry pinned to a real hit frequency; first pass used a stand-in.
        val pinned = buildViewModel(
            FakeDatabaseSource(
                listOf(
                    ProgramEntry("Catarrh", "RIFE", "", doubleArrayOf(onHit)),
                    ProgramEntry("Nowhere", "XTRA", "", doubleArrayOf(12.0)),
                ),
            ),
        )
        pinned.startHunt()
        awaitDone(pinned)
        val pinnedState = awaitLookupComplete(pinned)

        // Every hit has an entry in the result map (empty list = no matches).
        assertEquals(
            pinnedState.hits.map { it.frequency }.toSet(),
            pinnedState.lookupResults.keys,
        )

        val allMatches = pinnedState.lookupResults.values.flatten()
        assertTrue(
            "expected the RIFE Catarrh match on the pinned hit frequency",
            allMatches.any { it.programName == "Catarrh" && it.database == "RIFE" },
        )
        assertTrue("far entry must not match", allMatches.none { it.programName == "Nowhere" })
        assertEquals(false, pinnedState.lookupBusy)
    }

    /**
     * Lookup must be started as soon as the sweep yields hits — i.e. during or before
     * [HuntPhase.Killing], not only after [HuntPhase.Done]. This uses a blocking DB source
     * that holds until the kill phase is confirmed, guaranteeing [lookupBusy] is true while
     * the kill runs if (and only if) the lookup was triggered at sweep end.
     */
    @Test
    fun `lookup is triggered at sweep end before kill completes`() = runBlocking {
        // A latch that keeps the DB load blocked until we release it. This lets us
        // observe lookupBusy == true during the Killing phase with certainty.
        val releaseDb = CountDownLatch(1)
        val blockingSource = object : FrequencyDatabaseSource {
            val db = FrequencyDatabase(
                listOf(ProgramEntry("EarlyMatch", "RIFE", "", doubleArrayOf(0.0))),
                skippedLines = 0,
                declaredCount = 1,
            )
            override suspend fun database(): FrequencyDatabase {
                releaseDb.await() // block until test releases
                return db
            }
        }
        val viewModel = buildViewModel(blockingSource)

        viewModel.startHunt()

        // Wait for Killing phase. At this point the lookup job is blocked on the DB latch,
        // so lookupBusy must be true — confirming the lookup was triggered at sweep end.
        val killingState = awaitPhase(viewModel, HuntPhase.Killing)
        val lookupBusyDuringKill = killingState.lookupBusy

        // Release the DB so the hunt can finish.
        releaseDb.countDown()
        awaitDone(viewModel)
        val finalState = awaitLookupComplete(viewModel)

        assertTrue(
            "lookupBusy should be true during Killing (lookup triggered at sweep end, not only at Done)",
            lookupBusyDuringKill,
        )
        assertTrue("lookupResults should be non-empty after Done", finalState.lookupResults.isNotEmpty())
    }

    @Test
    fun `changing tolerance re-runs the lookup`() = runBlocking {
        val source = FakeDatabaseSource(
            listOf(ProgramEntry("Catarrh", "RIFE", "", doubleArrayOf(1_800_000.0))),
        )
        val viewModel = buildViewModel(source)

        viewModel.startHunt()
        awaitDone(viewModel)
        awaitLookupComplete(viewModel)
        val firstCalls = source.calls

        viewModel.setLookupTolerance(1.0)
        val state = awaitLookupComplete(viewModel)
        assertEquals(1.0, state.lookupTolerancePercent, 0.0)
        assertTrue("tolerance change should re-run lookup", source.calls > firstCalls)
    }

    /** Source for the first pass: only a far entry, used just to discover a hit freq. */
    private fun probeSource() =
        FakeDatabaseSource(listOf(ProgramEntry("Nowhere", "XTRA", "", doubleArrayOf(12.0))))

    private suspend fun buildViewModel(source: FrequencyDatabaseSource): HuntViewModel {
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

        return HuntViewModel(holder, LogBus(), usbConnectionManager = null, frequencyDatabase = source)
            .apply { updateDwellSeconds("0") }
    }

    /**
     * Blocks until [phase] or a terminal phase (Done/Error) is first observed, returning
     * the state snapshot at that moment.
     */
    private fun awaitPhase(viewModel: HuntViewModel, phase: HuntPhase): HuntUiState {
        val deadline = System.currentTimeMillis() + 60_000
        while (System.currentTimeMillis() < deadline) {
            val s = viewModel.state.value
            if (s.phase == phase || s.phase == HuntPhase.Done || s.phase == HuntPhase.Error) return s
            Thread.sleep(2)
        }
        throw AssertionError("Hunt did not reach $phase within 60s")
    }

    private fun awaitDone(viewModel: HuntViewModel): HuntUiState {
        val deadline = System.currentTimeMillis() + 60_000
        while (System.currentTimeMillis() < deadline) {
            val s = viewModel.state.value
            if (s.phase == HuntPhase.Done || s.phase == HuntPhase.Error) return s
            Thread.sleep(10)
        }
        throw AssertionError("Hunt did not reach Done within 60s")
    }

    private fun awaitLookupComplete(viewModel: HuntViewModel): HuntUiState {
        val deadline = System.currentTimeMillis() + 30_000
        while (System.currentTimeMillis() < deadline) {
            val s = viewModel.state.value
            if (!s.lookupBusy && s.lookupResults.isNotEmpty()) return s
            Thread.sleep(10)
        }
        throw AssertionError("Reverse lookup did not complete within 30s")
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
