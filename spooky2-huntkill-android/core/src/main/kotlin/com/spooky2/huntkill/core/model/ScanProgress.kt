package com.spooky2.huntkill.core.model

/**
 * A provisional ("live") hit candidate surfaced DURING the sweep, before
 * post-processing runs. Carries the sweep-step index (0-based into the sweep
 * readings / full-history graph), the frequency at that step, and the absolute
 * deviation used to rank candidates. Display-only: provisional hits never feed
 * back into the engine's final results.
 */
data class ProvisionalHit(
    val stepIndex: Int,
    val frequency: Double,
    val deviation: Double,
)

/**
 * Live progress snapshot emitted during a Hunt & Kill scan.
 *
 * Verbatim port of the C# reference `Spooky2.Core.Models.ScanProgress`,
 * extended with [provisionalHits] for the live hit-frequency markers.
 */
data class ScanProgress(
    val currentFrequency: Double = 0.0,
    val percentComplete: Double = 0.0,
    val stepNumber: Int = 0,
    val totalSteps: Int = 0,
    val hitsFound: Int = 0,
    val statusText: String = "",
    val cycleNumber: Int = 0,
    val amplitudeCv: Int = 0,
    val currentReading: Double = 0.0,
    val currentRunningAverage: Double = 0.0,
    /** Remaining dwell seconds for the current kill step; drives the countdown. */
    val killDwellRemainingSeconds: Int = 0,
    /**
     * Current top-N provisional hit candidates by |deviation|, evaluated live with
     * the same local-max/min + threshold logic as `detectHits` (one-step lag).
     * Empty outside the sweep phase. Display-only — does not affect final results.
     */
    val provisionalHits: List<ProvisionalHit> = emptyList(),
    /**
     * Whether THIS step's reading is valid for display + detection. False for a read
     * failure or a post-resume settle step — the live graph marks it invalid so the
     * pause/resume spike is excluded from the trace and Y-scale, not just from hits.
     */
    val currentStepValid: Boolean = true,
)
