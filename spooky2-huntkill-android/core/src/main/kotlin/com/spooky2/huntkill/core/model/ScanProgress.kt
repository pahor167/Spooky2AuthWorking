package com.spooky2.huntkill.core.model

/**
 * Live progress snapshot emitted during a Hunt & Kill scan.
 *
 * Verbatim port of the C# reference `Spooky2.Core.Models.ScanProgress`.
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
)
