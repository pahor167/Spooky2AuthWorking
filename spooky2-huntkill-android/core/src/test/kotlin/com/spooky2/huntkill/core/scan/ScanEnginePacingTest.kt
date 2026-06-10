package com.spooky2.huntkill.core.scan

import com.spooky2.huntkill.core.model.ScanParameters
import com.spooky2.huntkill.core.protocol.GeneratorProtocol
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.time.TimeSource

/**
 * Verifies sweep-step pacing: [ScanParameters.minReadDelaySeconds] is the minimum
 * step PERIOD (write-to-write), not an additive sleep.
 *
 * Uses [runBlocking] (real wall-clock) with an instant fake link so the only time
 * cost is the engine's own pacing. With a non-zero period, N steps must take at
 * least N × period. With period 0, the sweep runs unpaced (fast).
 */
class ScanEnginePacingTest {

    /** Fake link that responds instantly; sensor reads return constant values. */
    private class InstantLink : GeneratorLink {
        override suspend fun sendCommandWithResponse(command: String): String? = when (command) {
            GeneratorProtocol.READ_ANGLE -> ":r11=52000."
            GeneratorProtocol.READ_CURRENT -> ":r12=6900."
            else -> "ok"
        }

        override suspend fun sendCommandsBatch(commands: List<String>) {}
        override suspend fun writeFrequencies(frequencies: List<Double>) {}
        override suspend fun start() {}
        override suspend fun stop() {}
    }

    /** A small sweep: enough steps to make the pacing measurable, few enough to stay fast. */
    private fun smallSweepParameters(minReadDelaySeconds: Double) = ScanParameters(
        startFrequency = 1000.0,
        endFrequency = 1010.0,
        usePercentageStep = false,
        stepSizeHz = 1.0, // 11 steps: 1000..1010 inclusive
        startDelayMs = 0,
        enableAmplitudeRampUp = false,
        enableAmplitudeRampDown = false,
        baselineReadCount = 0,
        minReadDelaySeconds = minReadDelaySeconds,
    )

    private fun stepCount(parameters: ScanParameters): Int =
        ScanEngine.calculateFrequencySteps(parameters).size

    @Test
    fun `paced sweep takes at least N times the step period`() = runBlocking {
        val periodSeconds = 0.05
        val parameters = smallSweepParameters(periodSeconds)
        val n = stepCount(parameters)
        val engine = ScanEngine(InstantLink())

        val start = TimeSource.Monotonic.markNow()
        engine.runBiofeedbackScan(parameters)
        val elapsedMs = start.elapsedNow().inWholeMilliseconds

        val minExpectedMs = (n * periodSeconds * 1000).toLong()
        assertTrue(
            "paced sweep of $n steps took ${elapsedMs}ms, expected >= ${minExpectedMs}ms",
            elapsedMs >= minExpectedMs,
        )
    }

    @Test
    fun `unpaced sweep runs fast`() = runBlocking {
        val parameters = smallSweepParameters(0.0)
        val n = stepCount(parameters)
        val engine = ScanEngine(InstantLink())

        val start = TimeSource.Monotonic.markNow()
        engine.runBiofeedbackScan(parameters)
        val elapsedMs = start.elapsedNow().inWholeMilliseconds

        // With no pacing the instant link returns immediately; the whole sweep
        // should complete far faster than even a single 50ms paced step.
        assertTrue(
            "unpaced sweep of $n steps took ${elapsedMs}ms, expected well under ${n * 50}ms",
            elapsedMs < n * 50L,
        )
    }
}
