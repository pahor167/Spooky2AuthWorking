package com.spooky2.huntkill.core.scan

import com.spooky2.huntkill.core.model.ScanOutcome
import com.spooky2.huntkill.core.model.ScanParameters
import com.spooky2.huntkill.core.protocol.GeneratorProtocol
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Exercises the dropout-aware engine paths end-to-end over a programmable fake
 * link: invalid-step exclusion in detectHits, the sweep flagging failed reads,
 * and [ScanEngine.rescanSegments] splicing a re-read over a poisoned segment so
 * a spike hidden inside the dropout is recovered.
 */
class DropoutRescanTest {

    /**
     * Fake link that maps each angle read to a sweep step by the LAST frequency
     * written (robust against the setup/baseline reads that also call READ_ANGLE).
     * [sweepAngle] returns the angle for a given sweep index, or null to simulate
     * a transport timeout. Before any sweep frequency is written the read returns a
     * steady baseline value so the SMA warms up cleanly. Single-retry on null is
     * mirrored: a null step stays null across both attempts.
     */
    private class ProgrammableLink(
        private val frequencies: List<Double>,
        private val baselineValue: Double,
        private val sweepAngle: (sweepIndex: Int) -> Double?,
    ) : GeneratorLink {
        // Map each step's frequency-write command back to its sweep index.
        private val writeToIndex: Map<String, Int> =
            frequencies.withIndex().associate { (i, f) -> GeneratorProtocol.buildSetFrequency1(f) to i }
        private var currentSweepIndex: Int = -1

        override suspend fun sendCommandWithResponse(command: String): String? {
            // Detect a sweep frequency write to advance the active step.
            writeToIndex[command]?.let {
                currentSweepIndex = it
                return "ok"
            }
            return when (command) {
                GeneratorProtocol.READ_ANGLE -> {
                    val idx = currentSweepIndex
                    if (idx < 0) {
                        ":r11=${fmt(baselineValue)}."
                    } else {
                        val v = sweepAngle(idx) ?: return null
                        ":r11=${fmt(v)}."
                    }
                }
                GeneratorProtocol.READ_CURRENT -> ":r12=6900."
                else -> "ok"
            }
        }

        override suspend fun sendCommandsBatch(commands: List<String>) {}
        override suspend fun writeFrequencies(frequencies: List<Double>) {}
        override suspend fun start() {}
        override suspend fun stop() {}

        private fun fmt(v: Double): String {
            val r = Math.round(v)
            return if (r.toDouble() == v) r.toString() else v.toString()
        }
    }

    private fun smallParams() = ScanParameters(
        startFrequency = 1000.0,
        endFrequency = 2000.0,
        usePercentageStep = false,
        stepSizeHz = 1.0, // 1001 steps
        startDelayMs = 0,
        minReadDelaySeconds = 0.0,
        enableAmplitudeRampUp = false,
        enableAmplitudeRampDown = false,
        baselineReadCount = 30,
        raWindow = 20,
        dropoutMinRunLength = 3,
        dropoutDeviationFraction = 0.10,
        dropoutMedianWindow = 25,
    )

    @Test
    fun `detectHits excludes invalid steps from window and scoring`() {
        val params = ScanParameters(raWindow = 5, threshold = 0.0)
        // Steady 100, then a poisoned plateau at step 8..12 (value 10), then steady.
        val readings = ArrayList<Pair<Double, Double>>()
        for (i in 0 until 25) {
            val v = if (i in 8..12) 10.0 else 100.0
            readings.add((1000.0 + i) to v)
        }
        val valid = BooleanArray(25) { it !in 8..12 }

        val hits = ScanEngine.detectHits(readings, params, valid)

        // The poisoned dip must NOT produce a (min) hit, and must not poison the SMA
        // so the steady 100 region stays at deviation ~0 (no spurious max hits).
        assertTrue("poisoned plateau must not generate hits", hits.none { it.frequency in 1008.0..1012.0 })
    }

    @Test
    fun `failed reads are flagged and a hidden spike is recovered after rescan`() = runTest {
        val params = smallParams()
        val freqs = ScanEngine.calculateFrequencySteps(params)

        // Sweep: steady 52000 everywhere EXCEPT a dropout at sweep steps 400..410 where
        // the cable times out (null). A real spike to 60000 is HIDDEN at step 405 — but
        // during the dropout the read fails so it is never seen on the first pass.
        val firstPass: (Int) -> Double? = { idx ->
            when {
                idx in 400..410 -> null // timeout during dropout
                else -> 52000.0
            }
        }
        val link = ProgrammableLink(frequencies = freqs, baselineValue = 52000.0, sweepAngle = firstPass)
        val engine = ScanEngine(link)

        val outcome: ScanOutcome = engine.runBiofeedbackScanDetailed(params)

        // The failed reads must be flagged invalid and merged into a segment.
        for (i in 400..410) assertFalse("step $i should be invalid", outcome.sweepValid[i])
        assertTrue("expected a dropout segment", outcome.segments.isNotEmpty())
        val seg = outcome.segments.first { it.startStep <= 405 && it.endStep >= 405 }
        assertTrue(seg.startStep <= 400 && seg.endStep >= 410)

        // No spike was detectable on the first pass.
        val spikeFreqExpected = freqs[405]
        assertTrue(
            "spike must be hidden initially",
            outcome.hits.none { kotlin.math.abs(it.frequency - spikeFreqExpected) < 0.5 },
        )

        // Re-scan the segment; this pass the cable is fine and reveals the spike at 405.
        val secondPass: (Int) -> Double? = { idx ->
            when (idx) {
                405 -> 60000.0 // the previously-hidden spike
                else -> 52000.0
            }
        }
        val rescanLink = ProgrammableLink(frequencies = freqs, baselineValue = 52000.0, sweepAngle = secondPass)
        val rescanEngine = ScanEngine(rescanLink)

        val merged = rescanEngine.rescanSegments(params, outcome.segments, outcome, bufferSteps = 50)

        // The spliced region is now valid and the spike at step 405 is found.
        assertTrue("rescanned region should be valid", (400..410).all { merged.sweepValid[it] })
        // detectHits reports a peak found at sweep step p at the NEXT step's frequency
        // (the +1 step pairing, matching the original software), so the spike planted
        // at step 405 is reported at freqs[406].
        val spikeFreq = freqs[406]
        assertTrue(
            "spike at $spikeFreq Hz should be recovered, hits=${merged.hits.map { it.frequency }}",
            merged.hits.any { kotlin.math.abs(it.frequency - spikeFreq) < 0.5 },
        )
    }

    @Test
    fun `clean sweep yields no segments`() = runTest {
        val params = smallParams()
        val freqs = ScanEngine.calculateFrequencySteps(params)
        val link = ProgrammableLink(freqs, 52000.0) { 52000.0 }
        val engine = ScanEngine(link)

        val outcome = engine.runBiofeedbackScanDetailed(params)

        assertTrue("clean sweep must have no dropouts", outcome.segments.isEmpty())
        assertTrue("all steps valid", outcome.sweepValid.all { it })
    }
}
