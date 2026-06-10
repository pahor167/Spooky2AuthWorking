package com.spooky2.huntkill.core.model

/**
 * Full result of a biofeedback sweep: the detected [hits] plus the diagnostic
 * data the UI needs for the dropout / re-scan flow and the full-history graph.
 *
 * [sweepReadings] is the per-step detection reading (angle or current per
 * [ScanParameters.useCurrent]) for every sweep step in order — NOT including
 * the baseline pre-seed. [sweepValid] is the parallel validity mask: `false`
 * marks a step whose read failed or was flagged as a dropout, so the UI can
 * tint those regions and detection can skip them.
 *
 * [segments] is the merged list of flagged dropout runs (empty on a clean
 * sweep), aligned to the same sweep-step indices.
 */
data class ScanOutcome(
    val hits: List<ScanResult>,
    val sweepReadings: FloatArray,
    val sweepValid: BooleanArray,
    val segments: List<DropoutSegment>,
) {
    /** Fraction (0..1) of sweep steps that were flagged invalid. */
    val invalidFraction: Double
        get() = if (sweepValid.isEmpty()) 0.0 else sweepValid.count { !it }.toDouble() / sweepValid.size

    // FloatArray/BooleanArray break the data-class default equals/hashCode (identity
    // compare); provide structural ones so VM-state copies compare by content.
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ScanOutcome) return false
        return hits == other.hits &&
            sweepReadings.contentEquals(other.sweepReadings) &&
            sweepValid.contentEquals(other.sweepValid) &&
            segments == other.segments
    }

    override fun hashCode(): Int {
        var result = hits.hashCode()
        result = 31 * result + sweepReadings.contentHashCode()
        result = 31 * result + sweepValid.contentHashCode()
        result = 31 * result + segments.hashCode()
        return result
    }
}
