package com.spooky2.huntkill.core.model

import java.time.Instant

/**
 * A single detected hit (or recorded sample) from a Hunt & Kill scan.
 *
 * Verbatim port of the C# reference `Spooky2.Core.Models.ScanResult`.
 * The C# `DateTime Timestamp` (default `DateTime.MinValue`) maps to
 * [java.time.Instant], defaulting to [Instant.EPOCH].
 */
data class ScanResult(
    val frequency: Double = 0.0,
    val reading: Double = 0.0,
    val runningAverage: Double = 0.0,
    val deviation: Double = 0.0,
    val hitCount: Int = 0,
    val harmonicInfo: String = "",
    val timestamp: Instant = Instant.EPOCH,
)
