package com.spooky2.huntkill.core.scan

import com.spooky2.huntkill.core.model.ScanParameters
import com.spooky2.huntkill.core.model.ScanResult

/**
 * A contiguous frequency window `[startHz, endHz]` (inclusive) to be re-scanned in
 * a refining generation.
 */
data class FreqWindow(val startHz: Double, val endHz: Double) {
    init {
        require(endHz >= startHz) { "FreqWindow end ($endHz) < start ($startHz)" }
    }

    val widthHz: Double get() = endHz - startHz
}

/**
 * The plan for the next refining generation: the per-hit windows (overlaps merged,
 * clamped to the scan range) plus the **halved** step to sweep them with.
 */
data class RefinedScanPlan(
    val windows: List<FreqWindow>,
    /** Next-generation linear step (Hz). Half the current [ScanParameters.stepSizeHz]. */
    val nextStepSizeHz: Double,
    /** Next-generation percentage step. Half the current [ScanParameters.stepSizePercent]. */
    val nextStepSizePercent: Double,
)

/**
 * Computes the "narrow around each hit" refinement generations of the original
 * Spooky2 Hunt & Kill. See `docs/REFINEMENT.md` for the evidence and its limits.
 *
 * Binary-proven (Ghidra decompile of `Spooky.exe`):
 *  - Step halved each generation: `step = Val(Text19) / 2`  (FUN_0084b3b0, Main.frm
 *    64374/68301 — Text19 is the ScanResults live step field)
 *
 * Guide-derived (Users Guide field "Refine +/-" = preset `BFB_Include_x_Hz_In_Search`),
 * NOT found in the binary: the per-hit re-scan window `[hit-r, hit+r]`. An earlier
 * citation of Main.frm:70211/70215 for this was a MISATTRIBUTION — those lines are
 * the `BFB_RA_Window_1` running-average smoothing window (array indices) inside hit
 * DETECTION, fully recovered and unrelated to the refine grid.
 *
 * The derived default half-width below is therefore calibrated to the original's
 * OBSERVED timing, not decompiled math: a refinement cycle on the original takes
 * ~3 minutes ≈ ~2500 steps at ~70 ms/step ≈ ~250 halved steps per hit (10 hits),
 * i.e. r ≈ 60 coarse steps each side. An explicit `refinePlusMinusHz > 0` is used
 * verbatim.
 */
object RefinementPlanner {

    /**
     * Half-window, in coarse steps each side, used to derive `r` when
     * [ScanParameters.refinePlusMinusHz] is 0. Calibrated to the original's observed
     * ~3-minute refinement cycle (~250 halved steps per hit); the binary does not
     * encode this value anywhere we could recover. Definitive answer needs a serial
     * capture of the original performing a refinement cycle.
     */
    const val REFINE_WINDOW_STEPS: Int = 60

    /**
     * Smallest step (Hz) a refinement sweep may use. Below this, double-precision
     * addition can no longer advance the frequency (`freq + delta == freq`) and the
     * grid loop would spin forever — and sub-µHz steps are physically meaningless
     * anyway. [frequencyStepsFor] stops a window when the step falls under this.
     */
    const val MIN_STEP_HZ: Double = 1e-6

    /** The local coarse step size, in Hz, at [hit] for the given parameters. */
    fun localCoarseStepHz(hit: Double, p: ScanParameters): Double =
        if (p.usePercentageStep) hit * (p.stepSizePercent / 100.0) else p.stepSizeHz

    /**
     * The refine half-width `r` (Hz) for [hit]: the explicit
     * [ScanParameters.refinePlusMinusHz] when > 0 (bit-proven contract), otherwise
     * the derived `REFINE_WINDOW_STEPS × localCoarseStep(hit)` computed from
     * [basis] — the ORIGINAL generation-1 parameters, so the window width stays
     * constant across generations (e.g. ±250 Hz at a 1 MHz hit with the 0.025%
     * step) instead of shrinking with each halving.
     */
    fun resolveHalfWidthHz(hit: Double, basis: ScanParameters): Double =
        if (basis.refinePlusMinusHz > 0.0) {
            basis.refinePlusMinusHz
        } else {
            REFINE_WINDOW_STEPS * localCoarseStepHz(hit, basis)
        }

    /**
     * The window `[hit - r, hit + r]` for [hit], with `r` from [basis] (the
     * generation-1 parameters), clamped to `[startFrequency, endFrequency]`.
     * Returns null if the clamped window is degenerate (zero/negative width),
     * e.g. a hit at/over a range edge with r=0.
     */
    fun windowFor(hit: Double, basis: ScanParameters): FreqWindow? {
        val r = resolveHalfWidthHz(hit, basis)
        val start = (hit - r).coerceAtLeast(basis.startFrequency)
        val end = (hit + r).coerceAtMost(basis.endFrequency)
        return if (end > start) FreqWindow(start, end) else null
    }

    /**
     * Build the next refining generation from [hits] found in the current
     * generation under [p]: one window per hit (clamped), overlapping windows
     * merged, plus the halved step. Hits outside the scan range or producing a
     * degenerate window are dropped. Windows are returned sorted by start.
     *
     * [halfWidthBasis] supplies the step the window half-width is derived from —
     * pass the ORIGINAL generation-1 parameters so every generation re-scans the
     * same-width window around its hits; only the sweep step halves. Defaults to
     * [p] for single-step callers/tests.
     */
    fun planNextGeneration(
        hits: List<ScanResult>,
        p: ScanParameters,
        halfWidthBasis: ScanParameters = p,
    ): RefinedScanPlan {
        val raw = hits
            .map { it.frequency }
            .filter { it in p.startFrequency..p.endFrequency }
            .sorted()
            .mapNotNull { windowFor(it, halfWidthBasis) }

        return RefinedScanPlan(
            windows = mergeWindows(raw),
            nextStepSizeHz = p.stepSizeHz / 2.0,
            nextStepSizePercent = p.stepSizePercent / 2.0,
        )
    }

    /** Merge overlapping/touching windows (sorted by start) into disjoint ones. */
    fun mergeWindows(windows: List<FreqWindow>): List<FreqWindow> {
        if (windows.isEmpty()) return emptyList()
        val sorted = windows.sortedBy { it.startHz }
        val merged = ArrayList<FreqWindow>()
        var cur = sorted.first()
        for (w in sorted.drop(1)) {
            cur = if (w.startHz <= cur.endHz) {
                FreqWindow(cur.startHz, maxOf(cur.endHz, w.endHz))
            } else {
                merged.add(cur); w
            }
        }
        merged.add(cur)
        return merged
    }

    /**
     * The concatenated frequency steps for [plan]'s windows, swept at the plan's
     * **halved** step — the same stepping math as the main sweep
     * ([ScanEngine.calculateFrequencySteps]): advance one step BEFORE recording, so
     * the first recorded entry is `windowStart * (1 + step)` (percentage) or
     * `windowStart + stepHz` (linear). Each window is swept independently and the
     * lists concatenated, in window (ascending-frequency) order. The result is the
     * `frequencyOverride` fed to the next [ScanEngine.runBiofeedbackScan].
     */
    fun frequencyStepsFor(plan: RefinedScanPlan, p: ScanParameters): List<Double> {
        val out = ArrayList<Double>()
        for (w in plan.windows) {
            var freq = w.startHz
            while (freq <= w.endHz) {
                // Step magnitude always comes from plan.* (the halved step); [p] only
                // selects the stepping MODE. Guard against a step too small to advance
                // a double — without it `freq += delta` stalls and this loop never ends.
                val delta = if (p.usePercentageStep) {
                    freq * (plan.nextStepSizePercent / 100.0)
                } else {
                    plan.nextStepSizeHz
                }
                if (delta < MIN_STEP_HZ) return emptyList()
                freq += delta
                out.add(freq)
            }
        }
        return out
    }
}
