package com.spooky2.huntkill.core.model

import java.time.Duration

/**
 * Runtime status of a Spooky2 generator.
 *
 * Verbatim port of the C# reference `Spooky2.Core.Models.GeneratorStatus`.
 */
enum class GeneratorStatus {
    Idle,
    Running,
    Paused,
    Held,
}

/**
 * Snapshot of a generator's current state.
 *
 * Verbatim port of the C# reference `Spooky2.Core.Models.GeneratorState`.
 * The C# `required int Id` maps to a non-defaulted [id] parameter, and
 * `TimeSpan ElapsedTime` (default `TimeSpan.Zero`) maps to [java.time.Duration].
 */
data class GeneratorState(
    val id: Int,
    val port: String = "",
    val status: GeneratorStatus = GeneratorStatus.Idle,
    val currentFrequency: Double = 0.0,
    val currentProgram: String = "",
    val elapsedTime: Duration = Duration.ZERO,
)
