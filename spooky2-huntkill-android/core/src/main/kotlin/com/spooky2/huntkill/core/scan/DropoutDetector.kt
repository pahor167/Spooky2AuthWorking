package com.spooky2.huntkill.core.scan

import com.spooky2.huntkill.core.model.DropoutSegment
import com.spooky2.huntkill.core.model.ScanParameters
import kotlin.math.abs

/**
 * Deterministic dropout detection for a frequency sweep.
 *
 * Two sources of truth combine into one validity mask:
 *   1. **Hard failures** — steps the sweep loop already flagged invalid because
 *      a sensor read failed (transport timeout/null, unparseable, retry
 *      exhausted). Passed in as [hardInvalid].
 *   2. **Heuristic outliers** — contiguous runs (>= [ScanParameters.dropoutMinRunLength])
 *      where the reading deviates more than [ScanParameters.dropoutDeviationFraction]
 *      from the rolling median of the surrounding VALID window. This catches the
 *      half-broken-cable "collapse to a far-away plateau" garbage that still
 *      parses as a number.
 *
 * The combined mask is then walked to merge overlapping/adjacent flagged steps
 * into [DropoutSegment]s.
 *
 * Pure and side-effect free so it is trivially unit-testable.
 */
object DropoutDetector {

    /**
     * Compute the final per-step validity mask given the raw [readings] and the
     * [hardInvalid] mask from the sweep loop. `true` = valid, `false` = dropout.
     */
    fun computeValidity(
        readings: FloatArray,
        hardInvalid: BooleanArray,
        parameters: ScanParameters,
    ): BooleanArray {
        val n = readings.size
        val valid = BooleanArray(n) { i -> i >= hardInvalid.size || !hardInvalid[i] }
        if (n == 0) return valid

        flagHeuristicOutliers(readings, valid, parameters)
        return valid
    }

    /**
     * Flag heuristic outlier runs in-place on [valid]. A step is an outlier
     * candidate when its reading deviates from the rolling median (over the
     * surrounding valid window, hard failures excluded) by more than the
     * configured fraction. Only contiguous runs of >= minRunLength candidates
     * are committed to [valid]; isolated blips are left valid.
     */
    private fun flagHeuristicOutliers(
        readings: FloatArray,
        valid: BooleanArray,
        parameters: ScanParameters,
    ) {
        val n = readings.size
        val radius = parameters.dropoutMedianWindow.coerceAtLeast(1)
        val fraction = parameters.dropoutDeviationFraction
        val minRun = parameters.dropoutMinRunLength.coerceAtLeast(1)

        // Per-step outlier candidacy (independent of run length).
        val candidate = BooleanArray(n)
        val windowValues = ArrayList<Float>(2 * radius + 1)
        for (i in 0 until n) {
            windowValues.clear()
            val lo = (i - radius).coerceAtLeast(0)
            val hi = (i + radius).coerceAtMost(n - 1)
            for (j in lo..hi) {
                // Exclude the step itself and any already-invalid (hard-failed)
                // neighbor so a cluster of failures doesn't poison its own median.
                if (j == i) continue
                if (j < valid.size && !valid[j]) continue
                windowValues.add(readings[j])
            }
            if (windowValues.size < 2) continue
            val median = medianOf(windowValues)
            // A near-zero median can't anchor a fractional comparison; fall back to
            // an absolute gate relative to the largest seen magnitude so a collapse
            // toward zero is still caught.
            val denom = if (abs(median) > 1e-6) abs(median) else 1.0f
            val deviation = abs(readings[i] - median) / denom
            if (deviation > fraction) candidate[i] = true
        }

        // Commit only runs of >= minRun consecutive candidates.
        var runStart = -1
        for (i in 0..n) {
            val isCandidate = i < n && candidate[i]
            if (isCandidate) {
                if (runStart < 0) runStart = i
            } else if (runStart >= 0) {
                if (i - runStart >= minRun) {
                    for (k in runStart until i) valid[k] = false
                }
                runStart = -1
            }
        }
    }

    /**
     * Merge the invalid steps in [valid] into contiguous [DropoutSegment]s.
     * Adjacent/overlapping invalid runs naturally merge because a run is any
     * maximal stretch of `false` entries. [frequencies] supplies the band edges;
     * indices beyond its bounds clamp to the last available frequency.
     */
    fun mergeSegments(
        valid: BooleanArray,
        frequencies: List<Double>,
    ): List<DropoutSegment> {
        val segments = ArrayList<DropoutSegment>()
        var start = -1
        for (i in valid.indices) {
            if (!valid[i]) {
                if (start < 0) start = i
            } else if (start >= 0) {
                segments.add(segmentOf(start, i - 1, frequencies))
                start = -1
            }
        }
        if (start >= 0) segments.add(segmentOf(start, valid.size - 1, frequencies))
        return segments
    }

    private fun segmentOf(start: Int, end: Int, frequencies: List<Double>): DropoutSegment {
        val startFreq = frequencies.getOrElse(start) { frequencies.lastOrNull() ?: 0.0 }
        val endFreq = frequencies.getOrElse(end) { frequencies.lastOrNull() ?: 0.0 }
        return DropoutSegment(
            startStep = start,
            endStep = end,
            startFrequency = startFreq,
            endFrequency = endFreq,
        )
    }

    /** Median of a (small) float list; copies so the caller's buffer is untouched. */
    private fun medianOf(values: List<Float>): Float {
        val sorted = values.sorted()
        val mid = sorted.size / 2
        return if (sorted.size % 2 == 1) {
            sorted[mid]
        } else {
            (sorted[mid - 1] + sorted[mid]) / 2f
        }
    }
}
