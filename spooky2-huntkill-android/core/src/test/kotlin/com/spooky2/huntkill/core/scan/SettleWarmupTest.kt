package com.spooky2.huntkill.core.scan

import com.spooky2.huntkill.core.model.ScanParameters
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

/**
 * Settle-aware detection warm-up tests for [ScanEngine.detectHits].
 *
 * Reproduces the field bug: on a real GeneratorX the first few sweep angle readings
 * sit at the BASELINE level (~40871), then JUMP to the settled level (~52000) once
 * the generator/sensor settles. The SMA window straddling that jump yields a large
 * FALSE deviation at the very start, which used to become the #1 hit and get killed.
 *
 * The settle warm-up suppresses that startup transient: detection scores no step
 * until the SMA window has settled (range ≤ settleToleranceFraction × mean). It is a
 * one-time LEADING gate — genuine peaks later (which widen the window range) are
 * still detected.
 */
class SettleWarmupTest {

    private val rng = java.util.Random(1234)

    /** Build (freq, reading) scan readings from a plain reading list (freq = index). */
    private fun scan(readings: List<Double>): List<Pair<Double, Double>> =
        readings.mapIndexed { i, r -> (1000.0 + i) to r }

    /** Settled noisy level around [center] ± [jitter] (deterministic). */
    private fun settled(center: Double, jitter: Double): Double =
        center + (rng.nextDouble() * 2 - 1) * jitter

    @Test
    fun `startup jump is not selected as a hit while genuine later peaks are`() {
        val params = ScanParameters(raWindow = 20, threshold = 0.0)

        val readings = ArrayList<Double>()
        // 1. SHORT LOW startup plateau at the baseline level (generator not yet
        //    settled). Real data shows ~3 such readings before the jump — crucially
        //    SHORTER than raWindow, so the first full SMA window STRADDLES the jump
        //    (large range → not settled) and warm-up only completes once the window
        //    has fully moved past the transient.
        repeat(3) { readings.add(40000.0) }
        // 2. JUMP to the settled noisy level (~52000), tiny jitter so it settles fast.
        repeat(77) { readings.add(settled(52000.0, 20.0)) }
        // 3. A genuine small peak: a single sample rising clearly above the local mean.
        val genuinePeakIndexA = readings.size
        readings.add(52400.0)
        // back to settled
        repeat(40) { readings.add(settled(52000.0, 20.0)) }
        // 4. A second genuine peak.
        val genuinePeakIndexB = readings.size
        readings.add(52350.0)
        repeat(40) { readings.add(settled(52000.0, 20.0)) }

        val hits = ScanEngine.detectHits(scan(readings), params)

        // The huge startup-jump artifact (deviation ~ 52000-40000 ≈ 12000, or the
        // ~647 SMA-straddle artifact) must NOT appear. No hit may sit in the startup
        // or jump region (indices < a couple of windows past the jump).
        val jumpRegionEnd = 3 + params.raWindow // short low plateau + one window into the jump
        assertTrue(
            "no hit may land in the startup/jump region: ${hits.map { it.frequency to it.deviation }}",
            hits.none { it.frequency < 1000.0 + jumpRegionEnd },
        )
        // No absurdly large deviation from the baseline↔settled jump survives.
        assertTrue(
            "startup-jump deviation artifact must be gone: ${hits.map { it.deviation }}",
            hits.all { it.deviation < 1000.0 },
        )

        // The genuine later peaks ARE selected (their reported frequency is the +1 step).
        val freqA = 1000.0 + genuinePeakIndexA + 1
        val freqB = 1000.0 + genuinePeakIndexB + 1
        assertTrue(
            "genuine peak A near index $genuinePeakIndexA must be detected: " +
                "${hits.map { it.frequency }}",
            hits.any { abs(it.frequency - freqA) < 1.0 },
        )
        assertTrue(
            "genuine peak B near index $genuinePeakIndexB must be detected: " +
                "${hits.map { it.frequency }}",
            hits.any { abs(it.frequency - freqB) < 1.0 },
        )
    }

    @Test
    fun `baseline preseed jump is not selected as a hit while genuine later peaks are`() {
        // This reproduces the EXACT engine path: the SMA window is pre-seeded with a
        // raWindow-long BASELINE plateau at ~40000 (freq=0, as ScanEngine prepends
        // baselineReadings.takeLast(raWindow)), then the sweep readings JUMP to a
        // settled noisy ~52000. The baseline-filled window is internally homogeneous
        // (tiny range) so the OLD range-only settle returned true immediately and the
        // baseline↔sweep boundary artifact (deviation ~ 52000-40000 ≈ 11000) was scored
        // as the #1 hit. The strengthened settle (reading-vs-mean clause) must reject it.
        val params = ScanParameters(raWindow = 20, threshold = 0.0)

        val readings = ArrayList<Double>()
        // 1. raWindow-long BASELINE plateau at ~40000 — exactly fills the SMA window.
        repeat(params.raWindow) { readings.add(40000.0) }
        // 2. JUMP to the settled noisy sweep level (~52000).
        repeat(80) { readings.add(settled(52000.0, 20.0)) }
        // 3. A genuine peak well past the jump.
        val genuinePeakIndexA = readings.size
        readings.add(52400.0)
        repeat(40) { readings.add(settled(52000.0, 20.0)) }
        // 4. A second genuine peak.
        val genuinePeakIndexB = readings.size
        readings.add(52350.0)
        repeat(40) { readings.add(settled(52000.0, 20.0)) }

        val hits = ScanEngine.detectHits(scan(readings), params)

        // (a) NO hit in the baseline/jump/boundary region. No ~40k/41k-level frequency
        //     and no deviation-in-the-thousands artifact survives.
        val jumpRegionEnd = params.raWindow + params.raWindow // baseline window + one more
        assertTrue(
            "no hit may land in the baseline/jump region: ${hits.map { it.frequency to it.deviation }}",
            hits.none { it.frequency < 1000.0 + jumpRegionEnd },
        )
        assertTrue(
            "baseline↔sweep deviation artifact (~11000) must be gone: ${hits.map { it.deviation }}",
            hits.all { it.deviation < 1000.0 },
        )

        // (b) The genuine later peaks ARE selected.
        val freqA = 1000.0 + genuinePeakIndexA + 1
        val freqB = 1000.0 + genuinePeakIndexB + 1
        assertTrue(
            "genuine peak A near index $genuinePeakIndexA must be detected: ${hits.map { it.frequency }}",
            hits.any { abs(it.frequency - freqA) < 1.0 },
        )
        assertTrue(
            "genuine peak B near index $genuinePeakIndexB must be detected: ${hits.map { it.frequency }}",
            hits.any { abs(it.frequency - freqB) < 1.0 },
        )
    }

    @Test
    fun `fully settled series warms up at raWindow (behavior unchanged)`() {
        // No startup transient: a flat settled series from step 0. The warm-up start
        // must equal raWindow (the first full window), so detection is unchanged vs the
        // pre-warm-up behavior on already-settled data.
        val params = ScanParameters(raWindow = 20, threshold = 0.0)
        val readings = ArrayList<Double>()
        repeat(120) { readings.add(settled(52000.0, 15.0)) }
        // A single genuine peak well past the first window.
        val peakIndex = 60
        readings[peakIndex] = 52500.0

        val hits = ScanEngine.detectHits(scan(readings), params)
        assertEquals(WarmupProbe.warmupStartFor(readings, params), params.raWindow)

        // The genuine peak is detected and there is no spurious leading artifact.
        val peakFreq = 1000.0 + peakIndex + 1
        assertTrue(
            "genuine peak must be detected: ${hits.map { it.frequency }}",
            hits.any { abs(it.frequency - peakFreq) < 1.0 },
        )
    }
}

/**
 * Test-only re-derivation of the warm-up start, mirroring [ScanEngine.detectHits]'s
 * settle rule, so a test can assert the computed `warmupStart` directly without
 * exposing internal engine state.
 */
private object WarmupProbe {
    fun warmupStartFor(readings: List<Double>, params: ScanParameters): Int {
        val w = ArrayDeque<Double>()
        val size = params.raWindow
        val cap = 5 * size
        for (i in readings.indices) {
            val full = w.size >= size
            if (full && i <= cap) {
                val min = w.min()
                val max = w.max()
                val mean = w.sum() / w.size
                val tol = params.settleToleranceFraction * mean
                // Strengthened settle: range small AND the incoming reading consistent
                // with the window mean (no level discontinuity) — mirrors detectHits.
                if (mean > 0.0 && (max - min) <= tol &&
                    kotlin.math.abs(readings[i] - mean) <= tol
                ) {
                    return i
                }
            }
            w.addLast(readings[i])
            if (w.size > size) w.removeFirst()
        }
        return size.coerceAtMost(readings.size)
    }
}
