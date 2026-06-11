package com.spooky2.huntkill.ui.hunt

import com.spooky2.huntkill.core.model.ScanParameters
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * VM-level test for the amplitude-ramp / pre-settle LEAD-IN exclusion on the live scan
 * graph. The synthetic transport reproduces the field symptom: the first sweep readings
 * are low and rising (amplitude still settling after the ramp) before the curve flattens.
 *
 * Asserts the lead-in (first [ScanParameters.raWindow] sweep steps) is marked INVALID in
 * the published [HuntUiState.historyValid] so [ScrollableReadingGraph] excludes it from the
 * Y-scale (no leading vertical spike), and that no provisional graph markers ever land in
 * the lead-in region (no false red dots at the far left). Detection is unchanged.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class HuntViewModelLeadInTest {

    @Before fun setUp() { Dispatchers.setMain(UnconfinedTestDispatcher()) }
    @After fun tearDown() { Dispatchers.resetMain() }

    private companion object {
        const val SWEEP_START = 1000.0
        const val SWEEP_END = 2000.0
        // The on-the-wire field shape: low baseline rising smoothly into the settled
        // plateau. Kept gentle so the rising lead-in itself does not trip the dropout
        // deviation heuristic — this test exercises the DISPLAY lead-in, not dropouts.
        const val RAMP_LOW = 45500
        const val SETTLED = 47000
    }

    /**
     * Synthetic device transport reproducing the ramp/settle lead-in: for the first
     * [rampSteps] sweep steps the angle read returns low/rising values; afterwards it
     * returns the settled plateau. To bait a false provisional peak inside the lead-in,
     * one early step reads a local bump. No read failures (no dropouts).
     */
    private class RampLeadInTransport(
        sweepFrequencies: List<Double>,
        private val rampSteps: Int,
    ) : SerialTransport {
        private val writeToStep: Map<String, Int> =
            sweepFrequencies.withIndex().associate { (i, f) -> GeneratorProtocol.buildSetFrequency1(f) to i }
        private var opened = false
        private var pending: String? = null
        private var currentStep = -1

        override val isOpen: Boolean get() = opened
        override suspend fun open(baudRate: Int) { opened = true }
        override suspend fun close() { opened = false }

        override suspend fun write(bytes: ByteArray) {
            pending = respond(String(bytes, Charsets.US_ASCII).trim())
        }

        override suspend fun readLine(timeoutMs: Long): String? {
            val r = pending
            pending = null
            return r
        }

        private fun angleForStep(step: Int): Int {
            if (step < 0) return RAMP_LOW
            if (step >= rampSteps) return SETTLED
            // Smoothly rising ramp from RAMP_LOW to SETTLED across the lead-in. The rising
            // slope is what would otherwise stretch the live Y-scale and bait false
            // provisional "rising" peaks at the far left.
            val frac = step.toDouble() / rampSteps
            return RAMP_LOW + ((SETTLED - RAMP_LOW) * frac).toInt()
        }

        private fun respond(cmd: String): String {
            writeToStep[cmd]?.let { currentStep = it; return ":ok" }
            return when (cmd) {
                GeneratorProtocol.READ_ANGLE -> ":r11=${angleForStep(currentStep)}."
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

    private fun sweepFrequencies(): List<Double> =
        ScanEngine.calculateFrequencySteps(
            ScanParameters(startFrequency = SWEEP_START, endFrequency = SWEEP_END),
        )

    private fun awaitPhase(viewModel: HuntViewModel, vararg phases: HuntPhase): HuntUiState {
        val deadline = System.currentTimeMillis() + 60_000
        while (System.currentTimeMillis() < deadline) {
            val s = viewModel.state.value
            if (s.phase in phases) return s
            Thread.sleep(5)
        }
        throw AssertionError("Did not reach ${phases.toList()} (was ${viewModel.state.value.phase})")
    }

    @Test
    fun `lead-in is excluded from display and no provisional markers land in it`() = runBlocking {
        val leadIn = ScanParameters().raWindow
        val freqs = sweepFrequencies()
        val transport = RampLeadInTransport(freqs, rampSteps = leadIn)

        val holder = SessionHolder()
        holder.set(buildSession(transport))
        val viewModel = HuntViewModel(holder, LogBus()).apply {
            updateStartFrequency(SWEEP_START.toString())
            updateEndFrequency(SWEEP_END.toString())
            updateDwellSeconds("0")
            // Repeat defaults ON (kill loops forever); turn it off so the run reaches Done.
            toggleRepeatKill()
        }

        // Collect every provisional marker step-index seen across the LIVE sweep so we can
        // prove none lands in the lead-in region (polling the StateFlow on a side thread).
        val stop = java.util.concurrent.atomic.AtomicBoolean(false)
        val seenLeadInMarker = java.util.concurrent.atomic.AtomicBoolean(false)
        val collector = Thread {
            while (!stop.get()) {
                val s = viewModel.state.value
                if (s.graphMarkers.any { !it.isFinal && it.stepIndex < leadIn }) {
                    seenLeadInMarker.set(true)
                }
            }
        }.apply { isDaemon = true; start() }

        viewModel.startHunt()
        // The smooth ramp is a DISPLAY lead-in, not a dropout, so the run reaches Done.
        // Accept the dropout-warning phase too only to fail loudly if the heuristic ever
        // misfires on the gentle ramp; the assertions below require Done.
        val done = awaitPhase(viewModel, HuntPhase.Done, HuntPhase.Error, HuntPhase.HitsReadyWithDropouts)
        stop.set(true)
        collector.join(1000)

        assertEquals(HuntPhase.Done, done.phase)

        val valid = done.historyValid
        assertTrue("history should be populated", done.fullHistory.size > leadIn)
        assertEquals("mask aligns to history", done.fullHistory.size, valid.size)

        // The lead-in steps are excluded from display (marked invalid).
        assertTrue(
            "lead-in steps must be flagged invalid for display",
            valid.take(leadIn).all { !it },
        )
        // Past the lead-in the settled sweep is valid (no dropouts injected).
        assertTrue(
            "post lead-in readings must be valid",
            valid.drop(leadIn).all { it },
        )

        // No provisional marker ever surfaced inside the lead-in region.
        assertFalse(
            "no provisional marker may land in the lead-in",
            seenLeadInMarker.get(),
        )
        // And the final published markers (if any) never fall in the lead-in either.
        assertTrue(
            "final markers must not fall in the lead-in",
            done.graphMarkers.none { it.stepIndex < leadIn },
        )
    }
}
