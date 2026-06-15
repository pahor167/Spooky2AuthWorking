package com.spooky2.huntkill.core.scan

import com.spooky2.huntkill.core.model.ScanParameters
import com.spooky2.huntkill.core.model.ScanResult
import com.spooky2.huntkill.core.protocol.GeneratorProtocol
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * After a pause the engine silences the output and restores it on resume, so the first
 * reads spike wildly. [ScanEngine] marks the next `raWindow` post-resume sweep steps
 * invalid so that spike can't become a false ~300-deviation candidate. This verifies a
 * spike injected just after a mid-sweep resume produces NO hit, while the SAME spike
 * with no pause does.
 */
class ResumeSettleTest {

    /**
     * Flat baseline angle; a single huge spike on the Nth sweep-step read. Optionally
     * pauses [gate] once [pauseAfterSweepReads] sweep reads have happened, so the resume
     * (driven by the test) lands mid-sweep — the real pause/resume scenario.
     */
    private class SpikeLink(
        private val spikeOnSweepRead: Int,
        private val gate: PauseGate? = null,
        private val pauseAfterSweepReads: Int = -1,
    ) : GeneratorLink {
        private var sweepReads = 0
        private var sweepStarted = false
        override suspend fun sendCommandWithResponse(command: String): String? {
            // The baseline writes raw :w24=<int>,; the sweep writes the encoded posCode
            // form (>=9 chars). Treat the first encoded write as the sweep start.
            if (command.startsWith(":w24=") && command.length >= 12) sweepStarted = true
            return when (command) {
                GeneratorProtocol.READ_ANGLE -> {
                    if (!sweepStarted) return ":r11=52000."
                    sweepReads++
                    if (gate != null && sweepReads == pauseAfterSweepReads) gate.pause()
                    if (sweepReads == spikeOnSweepRead) ":r11=99000." else ":r11=52000."
                }
                GeneratorProtocol.READ_CURRENT -> ":r12=6900."
                else -> "ok"
            }
        }
        override suspend fun sendCommandsBatch(commands: List<String>) {}
        override suspend fun writeFrequencies(frequencies: List<Double>) {}
        override suspend fun start() {}
        override suspend fun stop() {}
    }

    private val params = ScanParameters(
        startFrequency = 90_000.0,
        endFrequency = 110_000.0,
        usePercentageStep = true,
        stepSizePercent = 0.025,
        baselineReadCount = 20,
        startDelayMs = 0,
        minReadDelaySeconds = 0.0,
        dwellSeconds = 0.0,
        enableAmplitudeRampUp = false,
        enableAmplitudeRampDown = false,
    )

    @Test
    fun `spike with no pause IS detected (control)`() = runTest {
        val engine = ScanEngine(SpikeLink(spikeOnSweepRead = 40))
        val hits = engine.runBiofeedbackScan(params)
        assertTrue("a lone spike on a clean baseline should score as a hit", hits.isNotEmpty())
    }

    @Test
    fun `spike right after a mid-sweep resume is suppressed`() = runTest {
        val gate = PauseGate()
        // Pause after sweep read 35; the spike is read 40 — within the post-resume
        // settle window (raWindow = 20) that begins when the engine resumes at read 36.
        val engine = ScanEngine(
            SpikeLink(spikeOnSweepRead = 40, gate = gate, pauseAfterSweepReads = 35),
        )

        var hits: List<ScanResult> = emptyList()
        val job = launch { hits = engine.runBiofeedbackScan(params, pauseGate = gate) }
        // Run until the engine parks at the pause point, then resume and finish.
        testScheduler.advanceUntilIdle()
        gate.resume()
        testScheduler.advanceUntilIdle()
        job.join()

        assertEquals("the post-resume spike must NOT become a candidate", emptyList<ScanResult>(), hits)
    }
}
