package com.spooky2.huntkill.core.scan

import com.spooky2.huntkill.core.model.ScanParameters
import com.spooky2.huntkill.core.model.ScanResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for the Hunt & Kill refinement math decoded from `Spooky.exe`
 * (`Main.frm`) — per-hit window `[f-r, f+r]`, step halving each generation,
 * clamping, overlap merging, replace-not-union. See `docs/REFINEMENT.md`.
 */
class RefinementPlannerTest {

    private fun hit(freq: Double) = ScanResult(frequency = freq, hitCount = 1)

    // ── Explicit refine value r (the bit-proven f-r … f+r contract) ──

    @Test
    fun windowFor_explicitR_isHitPlusMinusR() {
        val p = ScanParameters(refinePlusMinusHz = 500.0)
        val w = RefinementPlanner.windowFor(100_000.0, p)!!
        assertEquals(99_500.0, w.startHz, 0.0)
        assertEquals(100_500.0, w.endHz, 0.0)
    }

    @Test
    fun windowFor_clampsToScanRange() {
        // Hit near the low edge: start clamps to startFrequency (41000).
        val p = ScanParameters(refinePlusMinusHz = 5_000.0)
        val w = RefinementPlanner.windowFor(43_000.0, p)!!
        assertEquals(41_000.0, w.startHz, 0.0) // 43000-5000=38000 -> clamped to 41000
        assertEquals(48_000.0, w.endHz, 0.0)
    }

    @Test
    fun windowFor_degenerateAtEdge_isNull() {
        // r=0 derived window at the exact start with a tiny derived width still > 0,
        // so to force a degenerate window we put the hit AT the end with explicit r
        // landing entirely outside is impossible; instead use a hit at endFrequency
        // with r derived from 0 — the +side clamps to end, the -side stays below, so
        // it is NOT degenerate. Degeneracy only happens if both sides clamp equal:
        val p = ScanParameters(refinePlusMinusHz = 1.0, startFrequency = 50_000.0, endFrequency = 50_000.0)
        assertNull(RefinementPlanner.windowFor(50_000.0, p))
    }

    // ── Derived window when r == 0 (canonical preset value) ──

    @Test
    fun resolveHalfWidth_rZero_derivesFromLocalPercentageStep() {
        // percentage mode 0.025%: localStep @100k = 100000 * 0.00025 = 25 Hz.
        // r = REFINE_WINDOW_STEPS * 25 (binary-proven constant; assert via it).
        val p = ScanParameters(refinePlusMinusHz = 0.0, usePercentageStep = true, stepSizePercent = 0.025)
        val expectedR = RefinementPlanner.REFINE_WINDOW_STEPS * 25.0
        assertEquals(expectedR, RefinementPlanner.resolveHalfWidthHz(100_000.0, p), 1e-9)
        val w = RefinementPlanner.windowFor(100_000.0, p)!!
        assertEquals(100_000.0 - expectedR, w.startHz, 1e-9)
        assertEquals(100_000.0 + expectedR, w.endHz, 1e-9)
    }

    @Test
    fun resolveHalfWidth_rZero_derivesFromLinearStep() {
        // linear mode: localStep = stepSizeHz (100); r = REFINE_WINDOW_STEPS * 100.
        val p = ScanParameters(refinePlusMinusHz = 0.0, usePercentageStep = false, stepSizeHz = 100.0)
        assertEquals(
            RefinementPlanner.REFINE_WINDOW_STEPS * 100.0,
            RefinementPlanner.resolveHalfWidthHz(100_000.0, p),
            0.0,
        )
    }

    // ── Step halving each generation ──

    @Test
    fun planNextGeneration_halvesStep() {
        val p = ScanParameters(stepSizePercent = 0.025, stepSizeHz = 100.0)
        val plan = RefinementPlanner.planNextGeneration(listOf(hit(100_000.0)), p)
        assertEquals(0.0125, plan.nextStepSizePercent, 1e-12)
        assertEquals(50.0, plan.nextStepSizeHz, 0.0)
    }

    // ── Replace + per-hit windows + overlap merge ──

    @Test
    fun planNextGeneration_oneWindowPerHit_sortedByStart() {
        val p = ScanParameters(refinePlusMinusHz = 100.0)
        val plan = RefinementPlanner.planNextGeneration(
            listOf(hit(300_000.0), hit(100_000.0), hit(200_000.0)),
            p,
        )
        assertEquals(3, plan.windows.size)
        assertEquals(listOf(99_900.0, 199_900.0, 299_900.0), plan.windows.map { it.startHz })
    }

    @Test
    fun planNextGeneration_mergesOverlappingWindows() {
        // Two hits 150 Hz apart with r=100 -> windows [99900,100100] and [100050,100250]
        // overlap and merge into [99900,100250].
        val p = ScanParameters(refinePlusMinusHz = 100.0)
        val plan = RefinementPlanner.planNextGeneration(
            listOf(hit(100_000.0), hit(100_150.0)),
            p,
        )
        assertEquals(1, plan.windows.size)
        assertEquals(99_900.0, plan.windows[0].startHz, 0.0)
        assertEquals(100_250.0, plan.windows[0].endHz, 0.0)
    }

    @Test
    fun planNextGeneration_dropsHitsOutsideScanRange() {
        val p = ScanParameters(refinePlusMinusHz = 100.0, startFrequency = 41_000.0, endFrequency = 1_800_000.0)
        val plan = RefinementPlanner.planNextGeneration(
            listOf(hit(5_000.0), hit(100_000.0), hit(9_000_000.0)),
            p,
        )
        assertEquals(1, plan.windows.size)
        assertEquals(99_900.0, plan.windows[0].startHz, 0.0)
    }

    @Test
    fun planNextGeneration_noHits_emptyPlan() {
        val plan = RefinementPlanner.planNextGeneration(emptyList(), ScanParameters())
        assertTrue(plan.windows.isEmpty())
    }

    // ── frequencyStepsFor: refined sweep grid ──

    @Test
    fun frequencyStepsFor_staysWithinWindows_andUsesHalvedStep() {
        val p = ScanParameters(refinePlusMinusHz = 500.0, usePercentageStep = true, stepSizePercent = 0.025)
        val plan = RefinementPlanner.planNextGeneration(listOf(hit(100_000.0)), p)
        val freqs = RefinementPlanner.frequencyStepsFor(plan, p)

        assertTrue("expected several refined steps", freqs.size > 10)
        // Ascending, starting inside the window. Like the main sweep, the grid
        // advances one step BEFORE recording, so the last entry overshoots the
        // window end by up to one (halved) step — same "one step past end" behavior
        // as calculateFrequencySteps.
        val w = plan.windows.single()
        val lastSpacing = freqs[freqs.size - 1] - freqs[freqs.size - 2]
        assertTrue(freqs.all { it > w.startHz && it <= w.endHz + lastSpacing + 1e-6 })
        assertTrue(freqs.zipWithNext().all { (a, b) -> b > a })
        // Spacing near the window start ≈ start * halvedPercent/100 (0.0125% of ~100k ≈ 12.5 Hz),
        // i.e. half the coarse 25 Hz spacing.
        val firstSpacing = freqs[1] - freqs[0]
        assertEquals(12.5, firstSpacing, 0.5)
    }

    @Test
    fun frequencyStepsFor_concatenatesMultipleWindows_inAscendingOrder() {
        val p = ScanParameters(refinePlusMinusHz = 200.0, usePercentageStep = false, stepSizeHz = 100.0)
        val plan = RefinementPlanner.planNextGeneration(listOf(hit(100_000.0), hit(500_000.0)), p)
        val freqs = RefinementPlanner.frequencyStepsFor(plan, p)
        // Window A = [99800,100200], window B = [499800,500200], linear step 50.
        // Concatenation is window-ordered, so it is globally ascending here (windows
        // disjoint and sorted). First entry is windowStart+step; last overshoots by
        // up to one step.
        assertTrue(freqs.zipWithNext().all { (a, b) -> b > a })
        assertTrue(freqs.first() in 99_800.0..100_200.0)
        assertTrue(freqs.last() in 499_800.0..500_300.0)
    }

    // ── Window is sized to the halved step and zooms in each generation ──

    @Test
    fun planNextGeneration_windowSizedToHalvedStep_zoomsInEachGeneration() {
        // Binary: window half-width = REFINE_WINDOW_STEPS × (halved step). At a 100 kHz
        // hit, gen-1 step 0.025% halves to 0.0125% -> localStep 12.5 Hz -> r = steps×12.5.
        val gen1 = ScanParameters(refinePlusMinusHz = 0.0, usePercentageStep = true, stepSizePercent = 0.025)
        val planA = RefinementPlanner.planNextGeneration(listOf(hit(100_000.0)), gen1)
        val rA = RefinementPlanner.REFINE_WINDOW_STEPS * 12.5
        assertEquals(2.0 * rA, planA.windows.single().widthHz, 1e-6)
        assertEquals(0.0125, planA.nextStepSizePercent, 1e-12)

        // Next generation (step already 0.0125%) halves to 0.00625% -> the window is
        // HALF as wide (zoom-in), matching the binary's step = V/10 coupling.
        val gen2 = gen1.copy(stepSizePercent = 0.0125)
        val planB = RefinementPlanner.planNextGeneration(listOf(hit(100_000.0)), gen2)
        assertEquals(rA, planB.windows.single().widthHz, 1e-6)
        assertEquals(0.00625, planB.nextStepSizePercent, 1e-12)
    }

    // ── Runaway guard: step too small to advance a double ──

    @Test
    fun frequencyStepsFor_subMicroHzStep_returnsEmptyInsteadOfLooping() {
        val p = ScanParameters(refinePlusMinusHz = 200.0, usePercentageStep = false, stepSizeHz = 100.0)
        val plan = RefinedScanPlan(
            windows = listOf(FreqWindow(99_800.0, 100_200.0)),
            nextStepSizeHz = RefinementPlanner.MIN_STEP_HZ / 2.0, // below the floor
            nextStepSizePercent = 1e-12,
        )
        assertTrue(RefinementPlanner.frequencyStepsFor(plan, p).isEmpty())
    }
}
