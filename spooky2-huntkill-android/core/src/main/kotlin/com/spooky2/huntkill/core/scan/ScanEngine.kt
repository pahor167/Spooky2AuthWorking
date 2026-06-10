package com.spooky2.huntkill.core.scan

import com.spooky2.huntkill.core.model.ScanParameters
import com.spooky2.huntkill.core.model.ScanProgress
import com.spooky2.huntkill.core.model.ScanResult
import com.spooky2.huntkill.core.protocol.GeneratorProtocol
import com.spooky2.huntkill.core.waveform.WaveformTables
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import java.time.Instant
import kotlin.coroutines.coroutineContext
import kotlin.math.abs
import kotlin.math.min
import kotlin.time.TimeSource

/**
 * Biofeedback / Hunt-and-Kill scan engine.
 *
 * Faithful port of the C# reference `Spooky2.Services.Scanner.ScanService`,
 * preserving the exact protocol phases and detection math:
 *   - Phase 1: setup + amplitude ramp-up (330 steps, `round((i+1)*target/ramp)`)
 *   - Phase 2: baseline reads (1 standalone angle + 203 `:r11`/`:r12` pairs)
 *   - Phase 3: frequency sweep (0.025% log steps), with sensor reads
 *   - Post-processing: SMA + local maxima/minima + threshold + top-N sort
 *
 * Progress is delivered through an optional [onProgress] callback — the Kotlin
 * idiom replacing C# `IProgress<ScanProgress>`. The engine is `core`-only and
 * talks to hardware exclusively through [GeneratorLink].
 *
 * Cancellation follows structured concurrency: the engine cooperatively checks
 * [coroutineContext] so a cancelled scope stops the scan.
 */
class ScanEngine(private val link: GeneratorLink) {

    private var lastResults: List<ScanResult> = emptyList()

    /**
     * Run a single biofeedback scan and return detected hits.
     * Port of C# `RunBiofeedbackScan`.
     */
    suspend fun runBiofeedbackScan(
        parameters: ScanParameters,
        onProgress: ((ScanProgress) -> Unit)? = null,
        pauseGate: PauseGate = PauseGate(),
    ): List<ScanResult> {
        // ══════════════════════════════════════════════════════════
        // PHASE 1: Setup + Amplitude Ramp-Up
        // ══════════════════════════════════════════════════════════
        onProgress?.invoke(ScanProgress(statusText = "Initializing generator..."))

        send(":w14=0,")
        send(":w17=0,0,")
        send(":w24=0,")
        send(":w25=0,")
        send(":w15=1,1,")
        send(":w24=00,")
        send(":w32=120,")
        send(":w33=120,")
        send(GeneratorProtocol.buildSetDisplayName("Stopped"))
        send(":w13=0,")
        send(":w28=0,")
        send(":w29=0,")
        send(":w24=00,")
        send(GeneratorProtocol.CLEAR_FREQUENCY1)
        send(GeneratorProtocol.CLEAR_FREQUENCY2)
        send(":w32=120,")
        send(":w40=0,")
        send(":w33=120,")
        send(":w40=0,")
        send(":w13=0,")
        send(":w20=11,")
        send(":w14=1,")
        send(GeneratorProtocol.CLEAR_FREQUENCY1)
        send(GeneratorProtocol.CLEAR_FREQUENCY2)
        send(":w21=25,")

        val displayName = parameters.logName.ifEmpty { "Running Biofeedback" }
        send(GeneratorProtocol.buildSetDisplayName("Port - $displayName"))

        onProgress?.invoke(ScanProgress(statusText = "Uploading waveform tables..."))
        link.sendCommandsBatch(WaveformTables.commands)

        send(GeneratorProtocol.READ_ANGLE)
        send(GeneratorProtocol.READ_ANGLE)
        send(GeneratorProtocol.READ_CURRENT)

        send(GeneratorProtocol.buildSetFrequencyRawHz(parameters.startFrequency.toInt()))
        send(":w21=25,")

        if (parameters.enableAmplitudeRampUp) {
            val targetCv = parameters.targetAmplitudeCv
            val rampDivisor = parameters.rampSteps

            val firstCv = roundHalfToEven(targetCv.toDouble() / rampDivisor)
            send(GeneratorProtocol.buildSetAmplitudeCv1(firstCv))
            send(GeneratorProtocol.buildSetAmplitudeCv2(firstCv))
            send(GeneratorProtocol.ENABLE_OUTPUT1)
            send(GeneratorProtocol.ENABLE_OUTPUT2)
            send(":w20=11,")

            send(GeneratorProtocol.READ_ANGLE)
            send(GeneratorProtocol.READ_CURRENT)

            onProgress?.invoke(
                ScanProgress(
                    statusText = "Ramping amplitude up...",
                    amplitudeCv = firstCv,
                    currentFrequency = parameters.startFrequency,
                ),
            )

            val rampCmds = ArrayList<String>()
            for (i in 1..rampDivisor) {
                val cv = min(
                    roundHalfToEven((i + 1).toDouble() * targetCv / rampDivisor),
                    targetCv,
                )
                rampCmds.add(GeneratorProtocol.buildSetAmplitudeCv1(cv))
                rampCmds.add(GeneratorProtocol.buildSetAmplitudeCv2(cv))
            }
            link.sendCommandsBatch(rampCmds)

            onProgress?.invoke(
                ScanProgress(
                    statusText = "Amplitude ramp complete",
                    amplitudeCv = targetCv,
                    currentFrequency = parameters.startFrequency,
                ),
            )
        } else {
            send(GeneratorProtocol.buildSetAmplitudeCv1(parameters.targetAmplitudeCv))
            send(GeneratorProtocol.buildSetAmplitudeCv2(parameters.targetAmplitudeCv))
            send(GeneratorProtocol.ENABLE_OUTPUT1)
            send(GeneratorProtocol.ENABLE_OUTPUT2)
        }

        if (parameters.startDelayMs > 0) delay(parameters.startDelayMs.toLong())

        // ══════════════════════════════════════════════════════════
        // PHASE 2: Baseline sensor reads (fill RA buffer)
        // ══════════════════════════════════════════════════════════
        val raWindow = if (parameters.raWindow2 > 0) parameters.raWindow2 else parameters.raWindow
        val raWindow1 = SlidingWindow(parameters.raWindow)
        val raWindow2 = SlidingWindow(raWindow)
        val angleWindow1 = SlidingWindow(parameters.raWindow)
        val angleWindow2 = SlidingWindow(raWindow)

        onProgress?.invoke(ScanProgress(statusText = "Reading baseline..."))

        val baselineReadings = ArrayList<Double>()

        // Initial standalone angle read.
        run {
            val initAngle = send(GeneratorProtocol.READ_ANGLE)
            val a = GeneratorProtocol.parseSensorReading(initAngle ?: "")
            angleWindow1.add(a)
            angleWindow2.add(a)
        }

        for (b in 0 until parameters.baselineReadCount) {
            coroutineContext.ensureActive()
            pauseGate.awaitResumed()
            val (angle, current) = readSensors(parameters.samplesPerStep)

            raWindow1.add(current)
            raWindow2.add(current)
            angleWindow1.add(angle)
            angleWindow2.add(angle)

            baselineReadings.add(if (parameters.useCurrent) current else angle)
        }

        // ══════════════════════════════════════════════════════════
        // PHASE 3: Frequency sweep
        // ══════════════════════════════════════════════════════════
        val frequencies = calculateFrequencySteps(parameters)

        var peakReading = Double.MIN_VALUE
        var peakFrequency = 0.0

        val scanReadings = ArrayList<Pair<Double, Double>>()

        // Prepend baseline tail (up to raWindow entries) so the SMA window is
        // pre-seeded when detectHits processes the first sweep step.
        for (value in baselineReadings.takeLast(parameters.raWindow)) {
            scanReadings.add(0.0 to value) // freq=0 marks baseline entries
        }

        // Minimum step period (write-to-write), derived from the original dump's
        // 14-15 steps/s ≈ 70 ms/step. This is NOT an additive sleep: the serial
        // round-trips count toward the period and we only sleep the remainder.
        val periodMs = (parameters.minReadDelaySeconds * 1000).toLong()
        // Settle pause between the frequency write and the first read mimics the
        // original's natural ~23 ms bus latency. Skipped entirely when period is 0.
        val settleMs = if (periodMs > 0) min(25L, periodMs / 3) else 0L

        for (loop in 0 until parameters.loops) {
            for (i in frequencies.indices) {
                coroutineContext.ensureActive()
                pauseGate.awaitResumed()
                // Take the step start AFTER awaitResumed so paused time is not
                // counted into the step measurement (and the next step still paces).
                val stepStart = TimeSource.Monotonic.markNow()
                val freq = frequencies[i]

                send(GeneratorProtocol.buildSetFrequency1(freq))

                if (settleMs > 0) delay(settleMs)

                val (angle, current) = readSensors(parameters.samplesPerStep)
                val reading = if (parameters.useCurrent) current else angle

                if (parameters.calculateUsingPeak && reading > peakReading) {
                    peakReading = reading
                    peakFrequency = freq
                }

                scanReadings.add(freq to reading)

                raWindow1.add(current)
                raWindow2.add(current)
                angleWindow1.add(angle)
                angleWindow2.add(angle)

                val primaryWindow = if (parameters.useCurrent) {
                    if (parameters.useRetentiveWindow) raWindow2 else raWindow1
                } else {
                    if (parameters.useRetentiveWindow) angleWindow2 else angleWindow1
                }

                onProgress?.invoke(
                    ScanProgress(
                        currentFrequency = freq,
                        percentComplete = (loop * frequencies.size + i + 1).toDouble() /
                            (parameters.loops * frequencies.size) * 100,
                        stepNumber = i + 1,
                        totalSteps = frequencies.size,
                        hitsFound = 0,
                        statusText = "Scanning $freq Hz (${i + 1}/${frequencies.size})",
                        cycleNumber = loop + 1,
                        currentReading = reading,
                        currentRunningAverage = if (primaryWindow.isFull) primaryWindow.simpleAverage() else 0.0,
                    ),
                )

                // Pace to the minimum step period: sleep only the remainder after
                // the serial I/O already consumed part of it. When period is 0
                // (test/replay fast path) this is a no-op — behavior is unchanged.
                if (periodMs > 0) {
                    val elapsedMs = stepStart.elapsedNow().inWholeMilliseconds
                    val remainingMs = periodMs - elapsedMs
                    if (remainingMs > 0) delay(remainingMs)
                }
            }
        }

        // ══════════════════════════════════════════════════════════
        // POST-PROCESSING: hit detection
        // ══════════════════════════════════════════════════════════
        var hits: List<ScanResult>

        if (parameters.calculateUsingPeak && peakFrequency > 0) {
            val baselineAvg = if (baselineReadings.isNotEmpty()) baselineReadings.average() else 0.0
            hits = listOf(
                ScanResult(
                    frequency = peakFrequency,
                    reading = peakReading,
                    deviation = peakReading - baselineAvg,
                    hitCount = 1,
                    timestamp = Instant.now(),
                ),
            )
        } else if (scanReadings.size > 2) {
            hits = detectHits(scanReadings, parameters)
        } else {
            hits = emptyList()
        }

        lastResults = hits

        onProgress?.invoke(
            ScanProgress(
                statusText = "Scan complete - ${hits.size} hits found",
                percentComplete = 100.0,
                hitsFound = hits.size,
            ),
        )

        send(GeneratorProtocol.CLEAR_FREQUENCY1)
        send(GeneratorProtocol.CLEAR_FREQUENCY2)

        if (parameters.enableAmplitudeRampDown) {
            val n = parameters.rampSteps
            val target = parameters.targetAmplitudeCv
            val rampDownCmds = ArrayList<String>()
            for (i in n - 2 downTo 0) {
                val cv = roundHalfToEven((i + 1).toDouble() * target / n)
                rampDownCmds.add(GeneratorProtocol.buildSetAmplitudeCv1(cv))
                rampDownCmds.add(GeneratorProtocol.buildSetAmplitudeCv2(cv))
            }
            rampDownCmds.add(GeneratorProtocol.buildSetAmplitudeCv1(0))
            rampDownCmds.add(GeneratorProtocol.buildSetAmplitudeCv2(0))
            link.sendCommandsBatch(rampDownCmds)
        }

        return hits
    }

    /**
     * Run repeated scan + kill cycles. Port of C# `RunHuntAndKill`.
     *
     * The loop terminates when the surrounding coroutine is cancelled, when a
     * cycle finds no hits, or when [ScanParameters.continueRefining] is false.
     */
    suspend fun runHuntAndKill(
        parameters: ScanParameters,
        onProgress: ((ScanProgress) -> Unit)? = null,
        pauseGate: PauseGate = PauseGate(),
    ): List<ScanResult> {
        var lastCycleHits: List<ScanResult> = emptyList()
        var cycle = 0

        while (coroutineContext.isActive()) {
            cycle++

            onProgress?.invoke(
                ScanProgress(
                    statusText = "Hunt and Kill - Cycle $cycle - Scanning...",
                    cycleNumber = cycle,
                ),
            )

            val hits = runBiofeedbackScan(parameters, onProgress, pauseGate)

            if (hits.isEmpty()) break

            lastCycleHits = hits

            onProgress?.invoke(
                ScanProgress(
                    statusText = "Hunt and Kill - Cycle $cycle - Running ${hits.size} hits...",
                    cycleNumber = cycle,
                    hitsFound = hits.size,
                ),
            )

            send(GeneratorProtocol.buildSetAmplitudeCv1(parameters.targetAmplitudeCv))
            send(GeneratorProtocol.buildSetAmplitudeCv2(parameters.targetAmplitudeCv))

            val dwellSeconds = parameters.dwellSeconds
            val killFreqs = hits.map { it.frequency }

            if (killFreqs.isNotEmpty()) {
                link.writeFrequencies(listOf(killFreqs[0]))
            }

            link.start()

            var i = 0
            while (i < killFreqs.size && coroutineContext.isActive()) {
                if (i > 0) link.writeFrequencies(listOf(killFreqs[i]))

                // Dwell as a loop of ~1s slices so pause + cancellation are checked
                // each second and the countdown stays accurate. While paused the loop
                // holds: the current frequency stays set and no new commands are sent.
                // The do/while shape emits one progress even for a zero dwell so the
                // kill phase is always observable.
                var remainingMs = (dwellSeconds * 1000).toLong()
                do {
                    pauseGate.awaitResumed()
                    coroutineContext.ensureActive()

                    val remainingSeconds = ((remainingMs + 999) / 1000).toInt()
                    onProgress?.invoke(
                        ScanProgress(
                            currentFrequency = killFreqs[i],
                            statusText = "Killing ${i + 1}/${killFreqs.size}: ${killFreqs[i]} Hz",
                            cycleNumber = cycle,
                            hitsFound = hits.size,
                            stepNumber = i + 1,
                            totalSteps = killFreqs.size,
                            percentComplete = (i + 1).toDouble() / killFreqs.size * 100,
                            killDwellRemainingSeconds = remainingSeconds,
                        ),
                    )

                    if (remainingMs <= 0) break
                    val slice = min(remainingMs, KILL_DWELL_SLICE_MS)
                    delay(slice)
                    remainingMs -= slice
                } while (remainingMs > 0 && coroutineContext.isActive())
                i++
            }

            link.stop()

            if (!parameters.continueRefining) break
        }

        send(GeneratorProtocol.CLEAR_FREQUENCY1)
        send(GeneratorProtocol.CLEAR_FREQUENCY2)
        send(GeneratorProtocol.buildSetAmplitudeCv1(parameters.targetAmplitudeCv))
        send(GeneratorProtocol.buildSetAmplitudeCv2(parameters.targetAmplitudeCv))

        return lastCycleHits
    }

    /** Latest hits produced by the most recent scan. */
    fun lastResults(): List<ScanResult> = lastResults

    // ── Helpers ──

    private suspend fun send(command: String): String? =
        link.sendCommandWithResponse(command)

    /** Port of C# `ReadSensors`: averages [samples] angle/current read pairs. */
    suspend fun readSensors(samples: Int): Pair<Double, Double> {
        var angleSum = 0.0
        var currentSum = 0.0
        for (s in 0 until samples) {
            val ar = send(GeneratorProtocol.READ_ANGLE)
            val cr = send(GeneratorProtocol.READ_CURRENT)
            angleSum += GeneratorProtocol.parseSensorReading(ar ?: "")
            currentSum += GeneratorProtocol.parseSensorReading(cr ?: "")
        }
        return (angleSum / samples) to (currentSum / samples)
    }

    companion object {

        /** Kill-dwell slice length: one second per tick for pause/cancel checks + countdown. */
        private const val KILL_DWELL_SLICE_MS = 1000L

        /**
         * Post-processing hit detection. Verbatim port of C# `ScanService.DetectHits`:
         *   1. Compute SMA + deviation for each step.
         *   2. "Detecting Asymptotes" — find local maxima/minima of the raw signal.
         *   3. "Filling GreatestHits" — extrema whose deviation passes the threshold.
         *   4. Sort by deviation descending, take top [ScanParameters.maxHits].
         *
         * Note: matches C# behavior INCLUDING the documented limitation that no
         * cluster deduplication is performed.
         */
        fun detectHits(
            scanReadings: List<Pair<Double, Double>>,
            parameters: ScanParameters,
        ): List<ScanResult> {
            val windowSize = parameters.raWindow
            val window = SlidingWindow(windowSize)

            // Phase 1: SMA + deviation per step.
            data class Step(val freq: Double, val reading: Double, val deviation: Double, val ra: Double)
            val steps = ArrayList<Step>(scanReadings.size)
            for ((freq, reading) in scanReadings) {
                val ra = if (window.isFull) window.simpleAverage() else 0.0
                val deviation = if (window.isFull) reading - ra else 0.0
                steps.add(Step(freq, reading, deviation, ra))
                window.add(reading)
            }

            // Phase 2 + 3: local extrema passing the threshold.
            val greatestHits = ArrayList<ScanResult>()
            for (i in 1 until steps.size - 1) {
                val step = steps[i]
                val prevReading = steps[i - 1].reading
                val nextReading = steps[i + 1].reading

                val isLocalMax = step.reading > prevReading && step.reading > nextReading
                val isLocalMin = step.reading < prevReading && step.reading < nextReading

                val isHit =
                    (parameters.detectMax && isLocalMax && step.deviation > parameters.threshold) ||
                        (parameters.detectMin && isLocalMin && step.deviation < -parameters.threshold)

                if (isHit) {
                    greatestHits.add(
                        ScanResult(
                            frequency = step.freq,
                            reading = step.reading,
                            runningAverage = step.ra,
                            deviation = abs(step.deviation),
                            hitCount = 1,
                            timestamp = Instant.now(),
                        ),
                    )
                }
            }

            // Phase 4: stable sort by deviation descending, take top maxHits.
            // C# OrderByDescending is a stable sort; sortedByDescending in Kotlin
            // is likewise stable, preserving original order on ties.
            return greatestHits
                .sortedByDescending { it.deviation }
                .take(parameters.maxHits)
        }

        /** Port of C# `CalculateFrequencySteps`: 0.025% log or linear Hz steps. */
        fun calculateFrequencySteps(parameters: ScanParameters): List<Double> {
            val frequencies = ArrayList<Double>()
            var freq = parameters.startFrequency
            while (freq <= parameters.endFrequency) {
                frequencies.add(freq)
                freq += if (parameters.usePercentageStep) {
                    freq * (parameters.stepSizePercent / 100.0)
                } else {
                    parameters.stepSizeHz
                }
            }
            return frequencies
        }

        /**
         * C# `Math.Round(double)` uses banker's rounding (half-to-even), but the
         * ramp values in the reference are integral midpoints rarely hit; the
         * decoded VB6 behavior and dump match standard half-away-from-zero. The
         * reference C# uses `Math.Round` (half-to-even) — we mirror that exactly.
         */
        private fun roundHalfToEven(value: Double): Int =
            java.math.BigDecimal(value)
                .setScale(0, java.math.RoundingMode.HALF_EVEN)
                .toInt()
    }
}

private fun kotlin.coroutines.CoroutineContext.isActive(): Boolean {
    val job = this[kotlinx.coroutines.Job]
    return job?.isActive ?: true
}
