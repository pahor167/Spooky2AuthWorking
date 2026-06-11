package com.spooky2.huntkill.core.scan

import com.spooky2.huntkill.core.model.ProvisionalHit
import com.spooky2.huntkill.core.model.ScanParameters
import kotlin.math.abs

/**
 * Live, incremental version of [ScanEngine.detectHits].
 *
 * During the sweep this maintains the same SMA window, deviation, and local
 * maximum/minimum + threshold tests as the retrospective `detectHits`, so the
 * tracker's final state CONVERGES to the same hit step-indices/deviations on
 * clean data (no dropouts). It is DISPLAY-ONLY: the engine's returned results
 * still come from `detectHits` — the golden tests prove the live tracker never
 * changes them.
 *
 * One-step lag: a step's local-max test needs its NEXT neighbor, so step `i-1`
 * is only scored once step `i`'s reading has arrived. The tracker mirrors that
 * retrospective shape exactly.
 *
 * Indexing: [push] is called once per SWEEP step (NOT the baseline pre-seed),
 * with the sweep-step index `i` matching the position in the engine's
 * `sweepReadings` / the UI full-history graph. The baseline tail that primes
 * the SMA window is supplied once up front via [seedBaseline], mirroring how
 * `detectHits` is fed the pre-seed entries before the sweep.
 *
 * Skipping invalid steps follows `detectHits`: an invalid step neither fills
 * the window, is scored as a hit, nor is used as a neighbor.
 */
class ProvisionalHitTracker(private val parameters: ScanParameters) {

    private val window = SlidingWindow(parameters.raWindow)

    /** Sentinel [Step.index] for baseline pre-seed entries (neighbors only, never hits). */
    private companion object {
        const val BASELINE_INDEX = -1

        /** Mirror of [ScanEngine] `WARMUP_CAP_WINDOWS` — settle-search cap in RA windows. */
        const val WARMUP_CAP_WINDOWS = 5
    }

    /**
     * Record the settle warm-up for the COMBINED-steps [position] about to be added,
     * evaluated against the window state BEFORE this reading is added (the same window
     * the deviation is computed against), exactly as `ScanEngine.detectHits` does.
     */
    private fun noteSettle(position: Int, reading: Double) {
        if (warmupStart < 0 && position <= warmupCap &&
            window.isFull && window.isSettled(parameters.settleToleranceFraction, reading)
        ) {
            warmupStart = position
        }
    }

    /** Per-step record kept for the lagged local-extremum test. */
    private data class Step(
        /** Sweep-step index, or [BASELINE_INDEX] for a baseline pre-seed entry. */
        val index: Int,
        val freq: Double,
        val reading: Double,
        val deviation: Double,
        val valid: Boolean,
    )

    // detectHits walks indices 1..size-2 over the COMBINED [pre-seed + sweep]
    // array, so a sweep step near the start can use a baseline entry as its
    // previous neighbor. We keep the baseline pre-seed entries here too (as
    // neighbor-only records) so the boundary scoring matches detectHits exactly.
    // Keeping the full list is cheap (one tiny record per step).
    private val steps = ArrayList<Step>()

    // Settle warm-up, mirroring ScanEngine.detectHits: the first COMBINED-steps
    // position (pre-seed + sweep) at which the SMA window was full AND settled
    // (range ≤ settleToleranceFraction × mean). No candidate is scored before this,
    // so a startup transient never surfaces as a provisional hit. -1 = not yet found.
    // The cap mirrors detectHits' WARMUP_CAP_WINDOWS × raWindow fallback.
    private var warmupStart = -1
    private val warmupCap = WARMUP_CAP_WINDOWS * parameters.raWindow

    /** Accumulated hits in detection order, mirroring detectHits' greatestHits. */
    private val greatestHits = ArrayList<ProvisionalHit>()

    /**
     * Prime the SMA window with the baseline tail (the last [ScanParameters.raWindow]
     * baseline readings), exactly as the engine pre-seeds `scanReadings` before the
     * sweep. Baseline entries fill the window AND are retained as neighbor-only
     * records (never scored as hits — their deviation stays 0 like detectHits).
     */
    fun seedBaseline(baselineTail: List<Double>) {
        for (value in baselineTail) {
            val ra = if (window.isFull) window.simpleAverage() else 0.0
            val deviation = if (window.isFull) value - ra else 0.0
            noteSettle(steps.size, value)
            steps.add(Step(BASELINE_INDEX, 0.0, value, deviation, valid = true))
            window.add(value)
        }
    }

    /**
     * Feed one sweep step. Computes its SMA/deviation against the window state
     * BEFORE the reading is added (matching detectHits), then — with one-step
     * lag — scores the PREVIOUS valid candidate now that its next neighbor (this
     * step) is known.
     */
    fun push(stepIndex: Int, frequency: Double, reading: Double, valid: Boolean) {
        val ra = if (window.isFull) window.simpleAverage() else 0.0
        val deviation = if (window.isFull) reading - ra else 0.0
        noteSettle(steps.size, reading)
        steps.add(Step(stepIndex, frequency, reading, deviation, valid))
        if (valid) window.add(reading)

        // Lagged scoring: the just-added step is the "next" neighbor for the step
        // we now evaluate. detectHits scores indices 1..size-2, so the candidate
        // must have both a previous and a next valid neighbor — which we now have.
        scoreLaggedCandidate()
    }

    /** Current top-N provisional hits by |deviation| (stable on ties, matching detectHits). */
    fun topHits(): List<ProvisionalHit> =
        greatestHits
            .sortedByDescending { it.deviation }
            .take(parameters.maxHits)

    private fun scoreLaggedCandidate() {
        // Plateau-aware mirror of detectHits. A candidate run is scored once its
        // NEAREST VALID next neighbor arrives — the just-added step — so only fire
        // when that step is itself valid (an invalid step is never a neighbor). The
        // candidate is the run of EQUAL valid readings immediately before it; that
        // run collapses to its LEFT edge (matching the decoded markers[left]=1).
        val lastIdx = steps.size - 1
        if (lastIdx < 1) return
        if (!steps[lastIdx].valid) return

        // Right edge of the candidate run = nearest valid step before the new one.
        val right = prevValidPos(lastIdx)
        if (right < 1) return

        // Expand the run of equal valid readings backward to its left edge.
        val runReading = steps[right].reading
        var left = right
        run {
            var p = prevValidPos(left)
            while (p >= 0 && steps[p].reading == runReading) {
                left = p
                p = prevValidPos(left)
            }
        }

        // Previous neighbor of the whole run, and the next neighbor (the new step).
        val prevPos = prevValidPos(left)
        if (prevPos < 0) return

        // Settle warm-up gate: do not score a candidate whose combined-steps position
        // is before the warm-up start (mirrors detectHits' `i < warmupStart` skip),
        // suppressing the startup-transient false candidate. Once the cap is passed
        // with no settled window found, fall back to raWindow like detectHits.
        if (left < effectiveWarmupStart()) return

        val candidate = steps[left]
        val prevReading = steps[prevPos].reading
        val nextReading = steps[lastIdx].reading

        val isLocalMax = prevReading < runReading && nextReading < runReading
        val isLocalMin = prevReading > runReading && nextReading > runReading

        val isHit =
            (parameters.detectMax && isLocalMax && candidate.deviation > parameters.threshold) ||
                (parameters.detectMin && isLocalMin && candidate.deviation < -parameters.threshold)

        if (isHit) {
            // Report the NEXT step's frequency (+1 step pairing), matching detectHits.
            val nextStep = steps.getOrNull(left + 1)
            val reportFreq = nextStep?.freq ?: candidate.freq
            greatestHits.add(
                ProvisionalHit(
                    stepIndex = candidate.index,
                    frequency = reportFreq,
                    deviation = abs(candidate.deviation),
                ),
            )
        }
    }

    /**
     * Resolved settle warm-up start for the gate. Returns the found settled position,
     * or — once enough steps have passed the cap with no settled window — the same
     * `raWindow` fallback `detectHits` uses. While still searching (within the cap and
     * not yet settled) it returns a large sentinel so no candidate is scored prematurely.
     */
    private fun effectiveWarmupStart(): Int {
        if (warmupStart >= 0) return warmupStart
        // Mirror detectHits: if the search window (cap) has fully elapsed without a
        // settled window, fall back to raWindow. steps.size-1 is the latest position.
        return if (steps.size - 1 > warmupCap) parameters.raWindow else Int.MAX_VALUE
    }

    /** Nearest valid step position at or before [pos]-1 (skips flagged dropout steps). */
    private fun prevValidPos(pos: Int): Int {
        var j = pos - 1
        while (j >= 0 && !steps[j].valid) j--
        return j
    }
}
