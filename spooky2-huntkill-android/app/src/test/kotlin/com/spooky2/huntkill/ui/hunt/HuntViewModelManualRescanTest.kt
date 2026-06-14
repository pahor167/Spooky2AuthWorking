package com.spooky2.huntkill.ui.hunt

import com.spooky2.huntkill.core.protocol.GeneratorProtocol
import com.spooky2.huntkill.core.scan.ScanEngine
import com.spooky2.huntkill.data.GeneratorSession
import com.spooky2.huntkill.data.SessionHolder
import com.spooky2.huntkill.log.LogBus
import com.spooky2.huntkill.transport.GeneratorClient
import com.spooky2.huntkill.transport.SerialTransport
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
 * VM-level test for [GeneratorRunController.rescanManualRange] (the MANUAL graph-selection
 * re-scan). A completed sweep seeds [lastOutcome] (reached via the real engine on a synthetic
 * transport); the manual re-scan then re-reads the selected sweep-step range, re-detects hits,
 * and must land back in a review phase (HitsReady / HitsReadyWithDropouts) — NEVER Killing/Done.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class HuntViewModelManualRescanTest {

    @Before fun setUp() { Dispatchers.setMain(UnconfinedTestDispatcher()) }
    @After fun tearDown() { Dispatchers.resetMain() }

    private companion object {
        const val SWEEP_START = 1000.0
        const val SWEEP_END = 2000.0
    }

    /**
     * Synthetic transport: angle reads return a steady value, EXCEPT while the last sweep
     * frequency written is in the dropout STEP band, where [readLine] returns null (USB
     * timeout). [recover] flips the band to clean readings for the (manual) re-scan pass.
     * Mirrors HuntViewModelDropoutTest's DropoutTransport.
     */
    private class DropoutTransport(
        sweepFrequencies: List<Double>,
        private val dropStepLo: Int,
        private val dropStepHi: Int,
        private val recover: () -> Boolean,
    ) : SerialTransport {
        private val writeToStep: Map<String, Int> =
            sweepFrequencies.withIndex().associate { (i, f) -> GeneratorProtocol.buildSetFrequency1(f) to i }
        private var opened = false
        private var pending: String? = null
        private var pendingNull = false
        private var currentStep = -1

        override val isOpen: Boolean get() = opened
        override suspend fun open(baudRate: Int) { opened = true }
        override suspend fun close() { opened = false }

        override suspend fun write(bytes: ByteArray) {
            val cmd = String(bytes, Charsets.US_ASCII).trim()
            pendingNull = false
            pending = respond(cmd)
        }

        override suspend fun readLine(timeoutMs: Long): String? {
            if (pendingNull) { pendingNull = false; return null }
            val r = pending
            pending = null
            return r
        }

        private fun respond(cmd: String): String {
            writeToStep[cmd]?.let { currentStep = it; return ":ok" }
            return when (cmd) {
                GeneratorProtocol.READ_ANGLE -> {
                    val inDrop = currentStep in dropStepLo..dropStepHi && !recover()
                    if (inDrop) { pendingNull = true; "" } else ":r11=52000."
                }
                GeneratorProtocol.READ_CURRENT -> ":r12=6900."
                else -> ":ok"
            }
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

    private fun newViewModel(transport: SerialTransport): HuntViewModel {
        val holder = SessionHolder()
        holder.set(buildSession(transport))
        return HuntViewModel(holder, LogBus()).apply {
            updateStartFrequency(SWEEP_START.toString())
            updateEndFrequency(SWEEP_END.toString())
            updateDwellSeconds("0")
        }
    }

    private fun emptyViewModel(): HuntViewModel {
        val holder = SessionHolder()
        // A session so liveSession() resolves; lastOutcome stays null (no sweep run).
        holder.set(buildSession(DropoutTransport(emptyList(), 0, 0, recover = { true })))
        return HuntViewModel(holder, LogBus())
    }

    private fun sweepFrequencies(): List<Double> =
        ScanEngine.calculateFrequencySteps(
            com.spooky2.huntkill.core.model.ScanParameters(
                startFrequency = SWEEP_START,
                endFrequency = SWEEP_END,
            ),
        )

    private fun awaitPhase(viewModel: HuntViewModel, vararg phases: HuntPhase): HuntUiState {
        val deadline = System.currentTimeMillis() + 60_000
        while (System.currentTimeMillis() < deadline) {
            val s = viewModel.state.value
            if (s.phase in phases) return s
            Thread.sleep(10)
        }
        throw AssertionError("Did not reach ${phases.toList()} (was ${viewModel.state.value.phase})")
    }

    @Test
    fun `manual re-scan of a range re-detects and stays in a review phase, never killing`() = runBlocking {
        // Seed lastOutcome via a real sweep with a mid-sweep dropout; the band recovers
        // after the sweep so the manual re-scan reads clean values.
        var sweepDone = false
        val freqs = sweepFrequencies()
        val mid = freqs.size / 2
        val transport = DropoutTransport(freqs, dropStepLo = mid, dropStepHi = mid + 9, recover = { sweepDone })
        val viewModel = newViewModel(transport)

        viewModel.startHunt()
        val review = awaitPhase(viewModel, HuntPhase.HitsReadyWithDropouts, HuntPhase.Error, HuntPhase.Done)
        assertEquals(HuntPhase.HitsReadyWithDropouts, review.phase)
        val sweepSteps = review.fullHistory.size
        assertTrue("seeded sweep history should be non-empty", sweepSteps > 0)

        // Manually re-scan the (now-recovered) dropout band.
        sweepDone = true
        viewModel.rescanManualRange(mid, mid + 9)

        // Must NOT auto-proceed to kill — lands back in a review phase.
        val after = awaitPhase(viewModel, HuntPhase.HitsReady, HuntPhase.HitsReadyWithDropouts, HuntPhase.Killing, HuntPhase.Done, HuntPhase.Error)
        assertTrue(
            "manual re-scan must land in a review phase, not killing (was ${after.phase})",
            after.phase == HuntPhase.HitsReady || after.phase == HuntPhase.HitsReadyWithDropouts,
        )
        assertTrue("re-scan not still in progress", !after.rescanInProgress)
        // History length is preserved (the splice replaces in place, not grows).
        assertEquals(sweepSteps, after.fullHistory.size)
        // The re-scanned band recovered, so the merged sweep is clean -> HitsReady.
        assertEquals(HuntPhase.HitsReady, after.phase)
    }

    @Test
    fun `manual re-scan without a completed sweep is a no-op`() {
        val viewModel = emptyViewModel()
        val before = viewModel.state.value.phase

        viewModel.rescanManualRange(0, 5)

        // No lastOutcome -> guarded no-op, phase unchanged (no transition to Hunting).
        assertEquals(before, viewModel.state.value.phase)
        assertTrue("should not flag a re-scan in progress", !viewModel.state.value.rescanInProgress)
    }
}
