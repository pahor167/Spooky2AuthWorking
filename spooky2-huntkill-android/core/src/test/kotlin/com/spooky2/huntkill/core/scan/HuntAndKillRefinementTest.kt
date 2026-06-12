package com.spooky2.huntkill.core.scan

import com.spooky2.huntkill.core.model.ScanParameters
import com.spooky2.huntkill.core.protocol.GeneratorProtocol
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * End-to-end test of the refinement generations in [ScanEngine.runHuntAndKill]:
 * after the first full-range scan finds a hit, the next generation must re-scan
 * only a NARROW window around that hit, at a HALVED step. Decoded from
 * `Spooky.exe` (`Main.frm`); see `docs/REFINEMENT.md`.
 */
private fun decodeSweepW24(command: String): Double? {
    val m = Regex(""":w24=(\d+),""").find(command) ?: return null
    val s = m.groupValues[1]
    if (s.length < 2) return null
    val p = s.last() - '0'
    if (p !in 0..8) return null
    val digits = s.dropLast(1)
    val fracLen = 8 - p
    if (fracLen > digits.length) return null
    val cut = digits.length - fracLen
    val withDot = digits.substring(0, cut) + "." + digits.substring(cut)
    return withDot.toDoubleOrNull()
}

class HuntAndKillRefinementTest {

    /**
     * Fake generator that emits a single angle peak at [peakHz] (± [peakBandHz]).
     * Records the decoded sweep frequency of every `:w24` sweep write so the test
     * can separate generations (each new sweep restarts at a lower frequency) and
     * assert the refined generation's window + step.
     */
    private class PeakLink(
        val peakHz: Double,
        val peakBandHz: Double,
        val baselineAngle: String = ":r11=52000.",
        val peakAngle: String = ":r11=53000.",
    ) : GeneratorLink {
        val sweptFreqs = ArrayList<Double>()
        private var currentFreq = 0.0

        override suspend fun sendCommandWithResponse(command: String): String? {
            decodeSweepW24(command)?.let {
                currentFreq = it
                sweptFreqs.add(it)
            }
            return when (command) {
                GeneratorProtocol.READ_ANGLE ->
                    if (kotlin.math.abs(currentFreq - peakHz) <= peakBandHz) peakAngle else baselineAngle
                GeneratorProtocol.READ_CURRENT -> ":r12=6900."
                else -> "ok"
            }
        }

        override suspend fun sendCommandsBatch(commands: List<String>) {}
        override suspend fun writeFrequencies(frequencies: List<Double>) {}
        override suspend fun start() {}
        override suspend fun stop() {}
    }

    /** Split a flat list of swept frequencies into ascending runs (one per sweep). */
    private fun ascendingRuns(freqs: List<Double>): List<List<Double>> {
        val runs = ArrayList<MutableList<Double>>()
        var cur = mutableListOf<Double>()
        for (f in freqs) {
            if (cur.isNotEmpty() && f < cur.last()) {
                runs.add(cur); cur = mutableListOf()
            }
            cur.add(f)
        }
        if (cur.isNotEmpty()) runs.add(cur)
        return runs
    }

    private val fastParams = ScanParameters(
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
        continueRefining = true,
        repeatBfbCycles = 2, // exactly 2 generations, then stop (also tests the cap)
        refinePlusMinusHz = 0.0, // derived window (10 × local step)
    )

    @Test
    fun secondGeneration_narrowsAroundHit_atHalvedStep() = runTest {
        val link = PeakLink(peakHz = 100_000.0, peakBandHz = 30.0)
        val engine = ScanEngine(link)

        val finalHits = engine.runHuntAndKill(fastParams)

        val runs = ascendingRuns(link.sweptFreqs)
        assertEquals("expected exactly 2 sweep generations", 2, runs.size)

        val gen1 = runs[0]
        val gen2 = runs[1]

        // Gen 1 sweeps the full configured range.
        assertTrue("gen1 should start near 90k", gen1.first() < 90_100.0)
        assertTrue("gen1 should reach ~110k", gen1.last() > 109_000.0)

        // A hit was found near 100k.
        assertTrue("expected a hit near 100k", finalHits.any { kotlin.math.abs(it.frequency - 100_000.0) < 200.0 })
        val hit = finalHits.minByOrNull { kotlin.math.abs(it.frequency - 100_000.0) }!!.frequency

        // Gen 2 is a strict, narrow refinement: far fewer steps than gen 1, tightly
        // clustered around the hit. With the derived window r ≈ 10 × local 0.025% step
        // ≈ 250 Hz, a single hit spans ≈ 500 Hz; gen-1 plateau hits whose windows merge
        // can widen it modestly. Assert it is both narrow and centered on the hit.
        assertTrue("gen2 must be much smaller than gen1", gen2.size < gen1.size / 5)
        val span = gen2.last() - gen2.first()
        assertTrue("gen2 span ($span Hz) should be a narrow window, not a full sweep", span < 800.0)
        assertTrue("gen2 must be centered near the hit", gen2.all { kotlin.math.abs(it - hit) < 600.0 })

        // Gen 2 step is half of gen 1's: percentage stepping means spacing scales with
        // frequency, so compare the spacing of BOTH sweeps in the same band around the
        // hit (gen1 ≈ 100k*0.00025 ≈ 25 Hz, gen2 ≈ 12.5 Hz).
        fun spacingNearHit(run: List<Double>): Double =
            run.zipWithNext { a, b -> a to (b - a) }
                .filter { (a, d) -> d > 0 && kotlin.math.abs(a - hit) < 1_000.0 }
                .map { it.second }
                .let { if (it.isEmpty()) Double.NaN else it.average() }
        val gen1Spacing = spacingNearHit(gen1)
        val gen2Spacing = spacingNearHit(gen2)
        assertEquals("gen2 step ≈ half of gen1 step", gen1Spacing / 2.0, gen2Spacing, gen1Spacing * 0.2)
    }

    @Test
    fun singleGeneration_whenContinueRefiningFalse() = runTest {
        val link = PeakLink(peakHz = 100_000.0, peakBandHz = 30.0)
        val engine = ScanEngine(link)

        engine.runHuntAndKill(fastParams.copy(continueRefining = false))

        val runs = ascendingRuns(link.sweptFreqs)
        assertEquals("continueRefining=false → exactly one sweep", 1, runs.size)
    }
}
