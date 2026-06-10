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
 * VM-level test for the [HuntPhase.HitsReadyWithDropouts] branch: a sweep with an
 * injected cable dropout stops before the kill and offers re-scan vs continue.
 * Both choices are exercised against a synthetic transport.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class HuntViewModelDropoutTest {

    @Before fun setUp() { Dispatchers.setMain(UnconfinedTestDispatcher()) }
    @After fun tearDown() { Dispatchers.resetMain() }

    private companion object {
        const val SWEEP_START = 1000.0
        const val SWEEP_END = 2000.0
    }

    /**
     * Synthetic device transport: angle reads return a steady value, EXCEPT while the
     * last sweep frequency written is in the dropout STEP band, where [readLine]
     * returns null to simulate a USB timeout. The frequency-write command for each
     * sweep step is precomputed (so we match by exact bytes, sidestepping the
     * position-coded frequency encoding). [recover] flips the band to clean readings
     * for the re-scan pass.
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
        // Open directly at the GeneratorX baud (post-auth), mirroring the demo factory.
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
        // No reconnector: reuse the single session across the hunt + re-scan.
        return HuntViewModel(holder, LogBus()).apply {
            // Small sweep so the JVM test is fast; dwell 0; the dropout band sits mid-sweep.
            updateStartFrequency(SWEEP_START.toString())
            updateEndFrequency(SWEEP_END.toString())
            updateDwellSeconds("0")
        }
    }

    /** Sweep frequencies for the test params (matches toScanParameters(isDemo=true)). */
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
    fun `sweep with dropout surfaces HitsReadyWithDropouts and continue-anyway kills`() = runBlocking {
        // Drop a 10-step band mid-sweep (well above the min run length of 3).
        val freqs = sweepFrequencies()
        val mid = freqs.size / 2
        val transport = DropoutTransport(freqs, dropStepLo = mid, dropStepHi = mid + 9, recover = { false })
        val viewModel = newViewModel(transport)

        viewModel.startHunt()
        val dropoutState = awaitPhase(viewModel, HuntPhase.HitsReadyWithDropouts, HuntPhase.Error, HuntPhase.Done)

        assertEquals(HuntPhase.HitsReadyWithDropouts, dropoutState.phase)
        assertTrue("expected at least one dropout segment", dropoutState.dropoutSegments.isNotEmpty())
        assertTrue("full history should be populated", dropoutState.fullHistory.isNotEmpty())
        assertTrue("history should have flagged steps", dropoutState.historyValid.any { !it })

        // Continue anyway -> kill -> Done.
        viewModel.continueAnyway()
        val done = awaitPhase(viewModel, HuntPhase.Done, HuntPhase.Error)
        assertEquals(HuntPhase.Done, done.phase)
    }

    @Test
    fun `re-scan recovers the segment and proceeds to kill`() = runBlocking {
        // First pass drops; after the sweep completes, the band recovers so the re-scan
        // reads clean values and the dropout clears.
        var sweepDone = false
        val freqs = sweepFrequencies()
        val mid = freqs.size / 2
        val transport = DropoutTransport(freqs, dropStepLo = mid, dropStepHi = mid + 9, recover = { sweepDone })
        val viewModel = newViewModel(transport)

        viewModel.startHunt()
        val dropoutState = awaitPhase(viewModel, HuntPhase.HitsReadyWithDropouts, HuntPhase.Error, HuntPhase.Done)
        assertEquals(HuntPhase.HitsReadyWithDropouts, dropoutState.phase)

        // Now let the band recover and re-scan; expect the kill to start, then Done.
        sweepDone = true
        viewModel.rescanAffectedSegments()
        val done = awaitPhase(viewModel, HuntPhase.Done, HuntPhase.Error)
        assertEquals(HuntPhase.Done, done.phase)
        assertTrue("re-scanned history should be clean", viewModel.state.value.historyValid.all { it })
    }
}
