package com.spooky2.huntkill.core.model

/**
 * A contiguous run of sweep steps whose sensor readings were flagged as
 * unreliable (cable dropout / half-broken connection garbage).
 *
 * Step indices are 0-based into the sweep (excluding baseline pre-seed), and
 * the range is inclusive on both ends. The frequencies are the sweep
 * frequencies at [startStep] and [endStep] so the UI/log can describe the
 * affected band, and [rescanSegments] knows what to re-sweep.
 */
data class DropoutSegment(
    val startStep: Int,
    val endStep: Int,
    val startFrequency: Double,
    val endFrequency: Double,
) {
    /** Number of sweep steps covered by this segment (inclusive). */
    val stepCount: Int get() = endStep - startStep + 1
}
