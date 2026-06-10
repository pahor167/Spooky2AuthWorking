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
        // A candidate is scored once its NEAREST VALID next neighbor arrives, which
        // is the just-added step — so only fire when that step is itself valid (an
        // invalid step is never a neighbor in detectHits). The candidate is then the
        // nearest valid step before it, and its previous neighbor the nearest valid
        // step before THAT. Mirrors detectHits' local-extremum test exactly.
        val lastIdx = steps.size - 1
        if (lastIdx < 1) return
        if (!steps[lastIdx].valid) return

        val candidatePos = prevValidPos(lastIdx)
        // candidatePos < 1: no candidate, or no room for a previous neighbor (detectHits
        // only scores combined-array indices >= 1).
        if (candidatePos < 1) return

        val prevPos = prevValidPos(candidatePos)
        if (prevPos < 0) return

        val candidate = steps[candidatePos]

        val prevReading = steps[prevPos].reading
        val nextReading = steps[lastIdx].reading

        val isLocalMax = candidate.reading > prevReading && candidate.reading > nextReading
        val isLocalMin = candidate.reading < prevReading && candidate.reading < nextReading

        val isHit =
            (parameters.detectMax && isLocalMax && candidate.deviation > parameters.threshold) ||
                (parameters.detectMin && isLocalMin && candidate.deviation < -parameters.threshold)

        if (isHit) {
            greatestHits.add(
                ProvisionalHit(
                    stepIndex = candidate.index,
                    frequency = candidate.freq,
                    deviation = abs(candidate.deviation),
                ),
            )
        }
    }

    /** Nearest valid step position at or before [pos]-1 (skips flagged dropout steps). */
    private fun prevValidPos(pos: Int): Int {
        var j = pos - 1
        while (j >= 0 && !steps[j].valid) j--
        return j
    }
}
