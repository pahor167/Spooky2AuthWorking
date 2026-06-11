package com.spooky2.huntkill.core.scan

import com.spooky2.huntkill.core.model.DropoutSegment
import com.spooky2.huntkill.core.model.ScanOutcome
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
    ): List<ScanResult> = runBiofeedbackScanDetailed(parameters, onProgress, pauseGate).hits

    /**
     * Run a single biofeedback scan and return the full [ScanOutcome] — hits
     * plus the dropout diagnostics (per-step readings, validity mask, merged
     * dropout segments) the UI uses for the re-scan flow and full-history graph.
     *
     * On a clean sweep (no failed reads, no heuristic outliers) the hits are
     * bit-for-bit identical to the pre-dropout behavior and [ScanOutcome.segments]
     * is empty.
     */
    suspend fun runBiofeedbackScanDetailed(
        parameters: ScanParameters,
        onProgress: ((ScanProgress) -> Unit)? = null,
        pauseGate: PauseGate = PauseGate(),
    ): ScanOutcome {
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
        val baselinePreSeed = baselineReadings.takeLast(parameters.raWindow)
        for (value in baselinePreSeed) {
            scanReadings.add(0.0 to value) // freq=0 marks baseline entries
        }

        // Per-sweep-step diagnostics for dropout detection + the full-history graph.
        // sweepReadings/sweepHardInvalid exclude the baseline pre-seed (always valid).
        val sweepReadings = ArrayList<Float>()
        val sweepHardInvalid = ArrayList<Boolean>()
        var consecutiveFailures = 0
        var unstableSurfaced = false

        // Live provisional hit tracker: mirrors detectHits incrementally (one-step
        // lag) to surface red markers DURING the sweep. Display-only — it is never
        // read back into the returned hits, so the golden replay path is unaffected.
        // Seeded with the same baseline tail detectHits gets so it converges.
        val provisionalTracker = ProvisionalHitTracker(parameters)
        provisionalTracker.seedBaseline(baselinePreSeed)
        // Hard-invalid flag aligned to sweep-step index, tracking the in-flight
        // validity (read failures only; the deviation heuristic runs post-sweep).
        var sweepStepIndex = 0

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

                val sensor = readSensorsTracked(parameters.samplesPerStep)
                val angle = sensor.angle
                val current = sensor.current
                val reading = if (parameters.useCurrent) current else angle

                // Live awareness: count consecutive failed reads and surface a
                // status once the threshold is crossed; keep scanning regardless.
                if (sensor.valid) {
                    consecutiveFailures = 0
                    unstableSurfaced = false
                } else {
                    consecutiveFailures++
                }
                sweepReadings.add(reading.toFloat())
                sweepHardInvalid.add(!sensor.valid)

                // Feed the live tracker (sweep-step index == position in sweepReadings).
                provisionalTracker.push(sweepStepIndex, freq, reading, sensor.valid)
                sweepStepIndex++

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

                val unstable = consecutiveFailures >= parameters.dropoutUnstableReadThreshold
                if (unstable) unstableSurfaced = true
                val statusText = if (unstable || (unstableSurfaced && !sensor.valid)) {
                    "Connection unstable — check cable (${i + 1}/${frequencies.size})"
                } else {
                    "Scanning $freq Hz (${i + 1}/${frequencies.size})"
                }

                onProgress?.invoke(
                    ScanProgress(
                        currentFrequency = freq,
                        percentComplete = (loop * frequencies.size + i + 1).toDouble() /
                            (parameters.loops * frequencies.size) * 100,
                        stepNumber = i + 1,
                        totalSteps = frequencies.size,
                        hitsFound = 0,
                        statusText = statusText,
                        cycleNumber = loop + 1,
                        currentReading = reading,
                        currentRunningAverage = if (primaryWindow.isFull) primaryWindow.simpleAverage() else 0.0,
                        provisionalHits = provisionalTracker.topHits(),
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
        // POST-PROCESSING: dropout detection + hit detection
        // ══════════════════════════════════════════════════════════
        // Combine hard read-failures with the deviation heuristic into a final
        // per-sweep-step validity mask, then merge into dropout segments.
        val sweepReadingsArr = sweepReadings.toFloatArray()
        val sweepValid = DropoutDetector.computeValidity(
            sweepReadingsArr,
            sweepHardInvalid.toBooleanArray(),
            parameters,
        )
        val segments = DropoutDetector.mergeSegments(sweepValid, frequencies)

        // Validity mask aligned to scanReadings: the baseline pre-seed is always
        // valid; the sweep tail mirrors sweepValid. Lets detectHits skip dropouts.
        val readingsValid = BooleanArray(scanReadings.size) { true }
        val preSeedCount = baselinePreSeed.size
        for (k in sweepValid.indices) {
            val idx = preSeedCount + k
            if (idx < readingsValid.size) readingsValid[idx] = sweepValid[k]
        }

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
            hits = detectHits(scanReadings, parameters, readingsValid)
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

        return ScanOutcome(
            hits = hits,
            sweepReadings = sweepReadingsArr,
            sweepValid = sweepValid,
            segments = segments,
        )
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

            killHits(hits, parameters, onProgress, pauseGate, cycle)

            if (!parameters.continueRefining) break
        }

        finishHuntAndKill(parameters)
        return lastCycleHits
    }

    /**
     * Run the kill phase for [hits]: dwell on each detected frequency in turn.
     * Extracted from [runHuntAndKill] so the UI flow can run the sweep and the
     * kill as separate steps (it pauses between them for the dropout re-scan
     * decision). Pause + cancellation are honored each ~1s dwell slice.
     */
    suspend fun killHits(
        hits: List<ScanResult>,
        parameters: ScanParameters,
        onProgress: ((ScanProgress) -> Unit)? = null,
        pauseGate: PauseGate = PauseGate(),
        cycle: Int = 1,
        killControl: KillControl = KillControl(),
        // Read at the END of each full frequency pass. When it returns true (and the
        // coroutine is still active) the kill loops back and treats all frequencies
        // again — "repeat" mode. Read live (lambda/StateFlow getter) so a mid-kill UI
        // toggle is honored on the next pass-end. Default false = single pass, so
        // existing kill/golden tests are unchanged (back-compat).
        repeatEnabled: () -> Boolean = { false },
    ) {
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

        link.start()

        // Outer repeat loop: one iteration = one full pass over killFreqs. With repeat
        // OFF (default) this runs exactly once. With repeat ON it re-runs until the flag
        // is toggled off (checked at pass end) or the coroutine is cancelled. passCount
        // counts completed passes; the emitted cycleNumber reflects the current pass so
        // the UI can show "(cycle N)" while looping.
        var passCount = 0
        do {
            val passCycle = cycle + passCount
            var i = 0
            while (i < killFreqs.size && coroutineContext.isActive()) {
                // Always write the current frequency at the top of the inner loop. A jump
                // sets i and breaks the dwell loop to restart here, so writing here covers
                // both the initial step and every jumped-to step uniformly.
                link.writeFrequencies(listOf(killFreqs[i]))

                // Dwell as a loop of ~1s slices so pause + cancellation are checked
                // each second and the countdown stays accurate. While paused the loop
                // holds: the current frequency stays set and no new commands are sent.
                // The do/while shape emits one progress even for a zero dwell so the
                // kill phase is always observable.
                var remainingMs = (dwellSeconds * 1000).toLong()
                var jumped = false
                do {
                    pauseGate.awaitResumed()
                    coroutineContext.ensureActive()

                    // Apply a pending "Treat this now" jump: checked AFTER awaitResumed so
                    // a paused kill applies the jump when resumed (no deadlock). An in-range
                    // target sets i and breaks to restart the inner loop, which writes
                    // killFreqs[j] and dwells fresh; the flow then continues j, j+1, … within
                    // the current pass.
                    val target = killControl.takeJump()
                    if (target != null && target in killFreqs.indices) {
                        i = target
                        jumped = true
                        break
                    }

                    val remainingSeconds = ((remainingMs + 999) / 1000).toInt()
                    val cycleSuffix = if (passCycle > 1) " (cycle $passCycle)" else ""
                    onProgress?.invoke(
                        ScanProgress(
                            currentFrequency = killFreqs[i],
                            statusText = "Killing ${i + 1}/${killFreqs.size}$cycleSuffix: ${killFreqs[i]} Hz",
                            cycleNumber = passCycle,
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
                // On a jump, i was already set to the target; otherwise advance normally.
                if (!jumped) i++
            }
            passCount++
            // Read the repeat flag at the END of each full pass so a mid-kill toggle is
            // honored on the next pass boundary. Cancellation also breaks the loop.
        } while (repeatEnabled() && coroutineContext.isActive())

        link.stop()
    }

    /** Post-run cleanup: clear both frequency channels and restore target amplitude. */
    suspend fun finishHuntAndKill(parameters: ScanParameters) {
        send(GeneratorProtocol.CLEAR_FREQUENCY1)
        send(GeneratorProtocol.CLEAR_FREQUENCY2)
        send(GeneratorProtocol.buildSetAmplitudeCv1(parameters.targetAmplitudeCv))
        send(GeneratorProtocol.buildSetAmplitudeCv2(parameters.targetAmplitudeCv))
    }

    /** Latest hits produced by the most recent scan. */
    fun lastResults(): List<ScanResult> = lastResults

    /**
     * Re-sweep the frequency steps covered by the given dropout [segments]
     * (each padded by [bufferSteps] on both sides, clamped to the sweep bounds)
     * and SPLICE the fresh readings over the old ones in [previous]. The merged
     * readings are re-run through [detectHits] and a new [ScanOutcome] returned.
     *
     * The re-sweep uses the SAME stepping math (`calculateFrequencySteps`) and
     * pacing/settle as the main sweep. Re-scanned steps are marked valid; any
     * step whose re-read fails again stays flagged, so a still-bad cable surfaces
     * as a (smaller) remaining dropout the caller can retry.
     */
    suspend fun rescanSegments(
        parameters: ScanParameters,
        segments: List<DropoutSegment>,
        previous: ScanOutcome,
        bufferSteps: Int = DEFAULT_RESCAN_BUFFER_STEPS,
        onProgress: ((ScanProgress) -> Unit)? = null,
        pauseGate: PauseGate = PauseGate(),
    ): ScanOutcome {
        val frequencies = calculateFrequencySteps(parameters)
        val mergedReadings = previous.sweepReadings.copyOf()
        val mergedValid = previous.sweepValid.copyOf()
        val n = mergedReadings.size
        if (n == 0 || segments.isEmpty()) return previous

        val periodMs = (parameters.minReadDelaySeconds * 1000).toLong()
        val settleMs = if (periodMs > 0) min(25L, periodMs / 3) else 0L

        // Clamp + de-overlap the buffered ranges so a step is never re-swept twice.
        val ranges = segments
            .map { (it.startStep - bufferSteps).coerceAtLeast(0) to (it.endStep + bufferSteps).coerceAtMost(n - 1) }
            .sortedBy { it.first }

        var totalSteps = 0
        for ((lo, hi) in ranges) totalSteps += (hi - lo + 1)
        var done = 0
        var lastEnd = -1

        for ((rawLo, hi) in ranges) {
            val lo = maxOf(rawLo, lastEnd + 1)
            if (lo > hi) continue
            for (i in lo..hi) {
                coroutineContext.ensureActive()
                pauseGate.awaitResumed()
                val stepStart = TimeSource.Monotonic.markNow()
                val freq = frequencies.getOrElse(i) { frequencies.lastOrNull() ?: 0.0 }

                send(GeneratorProtocol.buildSetFrequency1(freq))
                if (settleMs > 0) delay(settleMs)

                val sensor = readSensorsTracked(parameters.samplesPerStep)
                val reading = if (parameters.useCurrent) sensor.current else sensor.angle
                mergedReadings[i] = reading.toFloat()
                mergedValid[i] = sensor.valid

                done++
                onProgress?.invoke(
                    ScanProgress(
                        currentFrequency = freq,
                        percentComplete = if (totalSteps > 0) done.toDouble() / totalSteps * 100 else 100.0,
                        stepNumber = done,
                        totalSteps = totalSteps,
                        statusText = "Re-scanning $freq Hz ($done/$totalSteps)",
                        currentReading = reading,
                    ),
                )

                if (periodMs > 0) {
                    val remainingMs = periodMs - stepStart.elapsedNow().inWholeMilliseconds
                    if (remainingMs > 0) delay(remainingMs)
                }
            }
            lastEnd = hi
        }

        // Re-apply the heuristic over the merged data so a freshly-clean region is
        // un-flagged and any still-bad re-reads remain flagged.
        val hardInvalid = BooleanArray(n) { !mergedValid[it] }
        val finalValid = DropoutDetector.computeValidity(mergedReadings, hardInvalid, parameters)
        val newSegments = DropoutDetector.mergeSegments(finalValid, frequencies)

        // Rebuild scanReadings (baseline pre-seed is unknown post-hoc, but detection
        // only needs the sweep tail; the SMA simply warms up over the first window).
        val scanReadings = ArrayList<Pair<Double, Double>>(n)
        for (i in 0 until n) {
            val freq = frequencies.getOrElse(i) { frequencies.lastOrNull() ?: 0.0 }
            scanReadings.add(freq to mergedReadings[i].toDouble())
        }
        val hits = if (scanReadings.size > 2) {
            detectHits(scanReadings, parameters, finalValid)
        } else {
            emptyList()
        }
        lastResults = hits

        return ScanOutcome(
            hits = hits,
            sweepReadings = mergedReadings,
            sweepValid = finalValid,
            segments = newSegments,
        )
    }

    // ── Helpers ──

    private suspend fun send(command: String): String? =
        link.sendCommandWithResponse(command)

    /** Port of C# `ReadSensors`: averages [samples] angle/current read pairs. */
    suspend fun readSensors(samples: Int): Pair<Double, Double> {
        val r = readSensorsTracked(samples)
        return r.angle to r.current
    }

    /**
     * Averaged sensor read that also reports whether the underlying serial
     * reads succeeded. A step is [SensorRead.valid] = false when ANY angle or
     * current read in the sample returned null (transport timeout, or the
     * [GeneratorLink] retry exhausted) or was unparseable. Invalid reads are
     * NOT coerced to a stale/zero value here — the caller flags the step so
     * dropout detection can exclude it.
     */
    suspend fun readSensorsTracked(samples: Int): SensorRead {
        var angleSum = 0.0
        var currentSum = 0.0
        var validReads = 0
        for (s in 0 until samples) {
            val ar = send(GeneratorProtocol.READ_ANGLE)
            val cr = send(GeneratorProtocol.READ_CURRENT)
            val angleOk = ar != null && GeneratorProtocol.isParseableSensorReading(ar)
            val currentOk = cr != null && GeneratorProtocol.isParseableSensorReading(cr)
            angleSum += GeneratorProtocol.parseSensorReading(ar ?: "")
            currentSum += GeneratorProtocol.parseSensorReading(cr ?: "")
            if (angleOk && currentOk) validReads++
        }
        return SensorRead(
            angle = angleSum / samples,
            current = currentSum / samples,
            valid = validReads == samples,
        )
    }

    /** Result of [readSensorsTracked]: averaged readings plus a validity flag. */
    data class SensorRead(val angle: Double, val current: Double, val valid: Boolean)

    companion object {

        /** Kill-dwell slice length: one second per tick for pause/cancel checks + countdown. */
        private const val KILL_DWELL_SLICE_MS = 1000L

        /** Default buffer (steps) added on both sides of each re-scanned dropout segment. */
        const val DEFAULT_RESCAN_BUFFER_STEPS = 50

        /**
         * Upper bound (in RA windows) for the detection settle warm-up search. If no
         * settled window is found within `WARMUP_CAP_WINDOWS × raWindow` steps the
         * warm-up falls back to `raWindow`, so a persistently noisy scan still scores
         * from the first full window rather than being fully discarded.
         */
        private const val WARMUP_CAP_WINDOWS = 5

        /**
         * Post-processing hit detection. Faithful port of the ORIGINAL Spooky2
         * detection loop decoded from `Spooky.exe` FUN_008531a0 (Ghidra), shared
         * with the C# `ScanService.DetectHits`:
         *   1. Compute SMA + deviation for each step.
         *   2. "Detecting Asymptotes" — plateau-aware slope-change extrema of the
         *      raw signal (a local max is a strict rise into a flat run followed by
         *      a strict fall; the run collapses to its LEFT edge, mirroring the
         *      decompiled `markers[left]=1` after the equal-walk).
         *   3. "Filling GreatestHits" — extrema whose deviation passes the threshold.
         *   4. Sort by deviation descending, take top [ScanParameters.maxHits].
         *
         * Reported frequency: the original associates a peak found at readings
         * index `p` with the frequency of the NEXT sweep step (`scanReadings[p+1]`).
         * This `+1` step pairing is what makes the reported hit frequencies equal
         * the original software's screenshot output exactly (see GROUND_TRUTH.md).
         */
        fun detectHits(
            scanReadings: List<Pair<Double, Double>>,
            parameters: ScanParameters,
        ): List<ScanResult> = detectHits(scanReadings, parameters, valid = null)

        /**
         * Dropout-aware [detectHits]. Steps where `valid[i] == false` are EXCLUDED:
         * they neither fill the SMA window nor are scored as hits, and a flagged
         * step is skipped when picking the previous/next neighbor for the
         * local-extremum test (so the comparison uses the nearest VALID neighbors).
         *
         * When [valid] is null or all-true the result reproduces the original
         * Spooky2 software's hit set (golden replay path, GROUND_TRUTH.md).
         */
        fun detectHits(
            scanReadings: List<Pair<Double, Double>>,
            parameters: ScanParameters,
            valid: BooleanArray?,
        ): List<ScanResult> {
            val windowSize = parameters.raWindow
            val window = SlidingWindow(windowSize)

            fun isValid(i: Int): Boolean = valid == null || i >= valid.size || valid[i]

            // Phase 1: SMA + deviation per step. Invalid steps don't poison the
            // window — they are skipped when filling it (their RA/deviation is left
            // at 0 since they will never be scored as hits anyway).
            data class Step(val freq: Double, val reading: Double, val deviation: Double, val ra: Double)
            val steps = ArrayList<Step>(scanReadings.size)
            // Settle warm-up: the FIRST scanReadings index at which the SMA window is
            // full AND "settled" (range ≤ tolerance × mean). Detection scores no step
            // before this, so a generator/sensor startup transient (early readings at
            // the baseline level that then JUMP to the settled level) is never selected
            // as a hit. One-time leading gate only — a real peak widens the range later
            // and is NOT re-gated. Bounded by a cap so a noisy scan still scores from
            // the first full window. -1 = not yet found.
            var warmupStart = -1
            val warmupCap = WARMUP_CAP_WINDOWS * windowSize
            for (i in scanReadings.indices) {
                val (freq, reading) = scanReadings[i]
                val ra = if (window.isFull) window.simpleAverage() else 0.0
                val deviation = if (window.isFull) reading - ra else 0.0
                // Evaluate settle on the window state BEFORE this reading is added —
                // the same window the deviation above was computed against. The window
                // must be settled AT THIS READING's LEVEL (range small AND the incoming
                // reading consistent with the window mean), so the baseline↔sweep
                // boundary (window full of the baseline level, reading jumped to the
                // settled sweep level) is NOT mistaken for a settled window.
                if (warmupStart < 0 && window.isFull && i <= warmupCap &&
                    window.isSettled(parameters.settleToleranceFraction, reading)
                ) {
                    warmupStart = i
                }
                steps.add(Step(freq, reading, deviation, ra))
                if (isValid(i)) window.add(reading)
            }
            // Fallback: no settled window within the cap → start at the first full
            // window (one raWindow in) so a noisy scan is never fully discarded.
            if (warmupStart < 0) warmupStart = windowSize.coerceAtMost(steps.size)

            // Nearest valid neighbor on each side (skips flagged dropout steps).
            fun prevValid(i: Int): Int {
                var j = i - 1
                while (j >= 0 && !isValid(j)) j--
                return j
            }
            fun nextValid(i: Int): Int {
                var j = i + 1
                while (j < steps.size && !isValid(j)) j++
                return j
            }

            // Phase 2 + 3: plateau-aware slope-change extrema passing the threshold.
            // For each valid candidate, expand the run of equal valid readings around
            // it. A local maximum exists when the nearest valid neighbor BELOW the run
            // and the nearest valid neighbor ABOVE the run are both strictly smaller;
            // the decoded loop collapses the run to its LEFT edge (`markers[left]=1`),
            // so we score the run's left-edge index. Symmetric for minima.
            val greatestHits = ArrayList<ScanResult>()
            var i = 1
            while (i < steps.size - 1) {
                if (!isValid(i)) { i++; continue }
                // Leading settle warm-up: do not score any step before the window has
                // settled (suppresses the startup-transient false peak).
                if (i < warmupStart) { i++; continue }

                // Expand the equal-reading run [left..right] over VALID steps.
                val reading = steps[i].reading
                var left = i
                run {
                    var p = prevValid(left)
                    while (p >= 0 && steps[p].reading == reading) { left = p; p = prevValid(left) }
                }
                var right = i
                run {
                    var n = nextValid(right)
                    while (n < steps.size && steps[n].reading == reading) { right = n; n = nextValid(right) }
                }

                val prevIdx = prevValid(left)
                val nextIdx = nextValid(right)
                if (prevIdx < 0 || nextIdx >= steps.size) { i = right + 1; continue }

                val prevReading = steps[prevIdx].reading
                val nextReading = steps[nextIdx].reading

                val isLocalMax = prevReading < reading && nextReading < reading
                val isLocalMin = prevReading > reading && nextReading > reading

                // Plateau representative = LEFT edge (matches decoded markers[left]).
                val peak = steps[left]
                val isHit =
                    (parameters.detectMax && isLocalMax && peak.deviation > parameters.threshold) ||
                        (parameters.detectMin && isLocalMin && peak.deviation < -parameters.threshold)

                if (isHit) {
                    // Report the NEXT sweep step's frequency (`+1` step pairing) so the
                    // reported hit frequency matches the original software exactly.
                    val reportFreq = if (left + 1 < steps.size) steps[left + 1].freq else peak.freq
                    greatestHits.add(
                        ScanResult(
                            frequency = reportFreq,
                            reading = peak.reading,
                            runningAverage = peak.ra,
                            deviation = abs(peak.deviation),
                            hitCount = 1,
                            timestamp = Instant.now(),
                        ),
                    )
                }

                // Advance past this run so a plateau is registered once.
                i = right + 1
            }

            // Phase 4: stable sort by deviation descending, take top maxHits.
            // C# OrderByDescending is a stable sort; sortedByDescending in Kotlin
            // is likewise stable, preserving original order on ties.
            return greatestHits
                .sortedByDescending { it.deviation }
                .take(parameters.maxHits)
        }

        /**
         * Port of C# `CalculateFrequencySteps`: 0.025% log (or linear Hz) steps.
         *
         * Alignment with the ORIGINAL Spooky2 (proven against Data/FullHuntAndKill):
         * sweep step `i` TRANSMITS `startFrequency * (1 + step)^(i+1)`, i.e. the
         * first recorded sweep frequency is `startFrequency * (1 + step)`
         * (41010.25 Hz for the defaults, NOT 41000.00), and the grid runs one step
         * PAST `endFrequency` (last ≈ 1800103.30 Hz). We therefore ADVANCE one step
         * BEFORE recording each entry. Readings pair 1:1 with this corrected grid,
         * so a reported/killed hit frequency matches what the original transmitted.
         *
         * Verified: 15130 entries for the default params; first = 41010.25,
         * last ≈ 1800103.2959…; `calculateFrequencySteps(default)[i]` equals the
         * dump's decoded `:w24` sweep frequency at index `i` within 1e-6 relative
         * for all 15130 steps. The continuation condition still tests the
         * pre-advance value so the entry count matches the dump exactly.
         *
         * Linear (Hz-step) mode applies the same one-step-in shift for consistency
         * (the first recorded entry is `startFrequency + stepSizeHz`).
         */
        fun calculateFrequencySteps(parameters: ScanParameters): List<Double> {
            val frequencies = ArrayList<Double>()
            var freq = parameters.startFrequency
            while (freq <= parameters.endFrequency) {
                // Advance one step BEFORE recording so the first transmitted/recorded
                // frequency is startFrequency*(1+step), matching the original dump.
                freq += if (parameters.usePercentageStep) {
                    freq * (parameters.stepSizePercent / 100.0)
                } else {
                    parameters.stepSizeHz
                }
                frequencies.add(freq)
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
