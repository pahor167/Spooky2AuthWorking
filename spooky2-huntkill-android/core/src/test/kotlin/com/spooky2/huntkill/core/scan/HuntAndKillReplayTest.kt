package com.spooky2.huntkill.core.scan

import com.spooky2.huntkill.core.model.ScanParameters
import com.spooky2.huntkill.core.model.ScanResult
import com.spooky2.huntkill.core.protocol.GeneratorProtocol
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.roundToLong

/**
 * Golden replay test: feeds the recorded GeneratorX Pro Hunt-and-Kill serial
 * dump through the ported [ScanEngine] hit-detection and asserts the SAME hit
 * frequencies / deviations the C# `HuntAndKillReplayTests` produces.
 *
 * Ground truth captured by running the C# `ScanService.DetectHits` over
 * `Data/FullHuntAndKill` (angle-based, default [ScanParameters]):
 * exactly 10 hits, ordered by deviation descending. These values match the
 * C# implementation bit-for-bit (identical IEEE-754 math).
 *
 * The C# test documents a known limitation — no cluster deduplication, so all
 * 10 slots come from the strongest spike clusters. This port replicates that
 * SAME behavior (it does NOT "fix" it).
 */
class HuntAndKillReplayTest {

    // ── Golden values produced by C# ScanService.DetectHits (RunDetection path) ──
    // (frequency, reading, runningAverage, deviation)
    private data class GoldenHit(
        val frequency: Double,
        val reading: Double,
        val runningAverage: Double,
        val deviation: Double,
    )

    private val goldenHits = listOf(
        GoldenHit(1642312.8469206586, 53053.0, 52956.75, 96.25),
        GoldenHit(1791126.369943749, 53072.0, 52989.95, 82.05000000000291),
        GoldenHit(1796058.1290780483, 53108.0, 53030.2, 77.80000000000291),
        GoldenHit(176821.25762329542, 52590.0, 52516.45, 73.55000000000291),
        GoldenHit(1792470.0505853875, 53074.0, 53001.95, 72.05000000000291),
        GoldenHit(176998.14519994234, 52605.0, 52535.65, 69.34999999999854),
        GoldenHit(1686831.9394644983, 52961.0, 52899.65, 61.349999999998545),
        GoldenHit(1691053.7666921832, 52986.0, 52929.5, 56.5),
        GoldenHit(1687675.4608612268, 52958.0, 52902.1, 55.900000000001455),
        GoldenHit(1590994.8126824652, 52965.0, 52909.3, 55.69999999999709),
    )

    private fun dumpPath(): String {
        val url = requireNotNull(javaClass.classLoader.getResource("dumps/FullHuntAndKill")) {
            "Dump resource 'dumps/FullHuntAndKill' not found on test classpath"
        }
        return java.io.File(url.toURI()).absolutePath
    }

    private fun loadSession() = PlainTextDumpParser.parse(dumpPath())

    /**
     * Mirror of the C# `RunDetection` helper: baseline tail primes the SMA
     * window, then aligned sweep readings, fed straight into [ScanEngine.detectHits].
     */
    private fun runDetection(
        session: PlainTextDumpParser.HuntAndKillSession,
        parameters: ScanParameters = ScanParameters(),
    ): List<ScanResult> {
        val frequencies = ScanEngine.calculateFrequencySteps(parameters)
        val stepCount = min(frequencies.size, session.sweepSteps.size)

        val scanReadings = ArrayList<Pair<Double, Double>>()

        val baselineTail = session.baselineReadings
            .takeLast(parameters.raWindow)
            .map { if (parameters.useCurrent) it.current else it.angle }
        for (value in baselineTail) scanReadings.add(0.0 to value)

        for (i in 0 until stepCount) {
            val reading = if (parameters.useCurrent) {
                session.sweepSteps[i].currentReading
            } else {
                session.sweepSteps[i].angleReading
            }
            scanReadings.add(frequencies[i] to reading)
        }

        return ScanEngine.detectHits(scanReadings, parameters)
    }

    // ═══════════════════════════════════════════════════════════════
    // Parser correctness
    // ═══════════════════════════════════════════════════════════════

    @Test
    fun `parser extracts expected phase structure`() {
        val session = loadSession()

        assertEquals(203, session.baselineReadings.size)
        assertTrue(
            "sweep steps should be > 15000, got ${session.sweepSteps.size}",
            session.sweepSteps.size > 15_000,
        )
        assertTrue(session.killFrequencies.size >= 3)
        assertTrue(
            "ramp steps in [330,332], got ${session.amplitudeRampSteps}",
            session.amplitudeRampSteps in 330..332,
        )
    }

    @Test
    fun `parser kill frequencies match top three from screenshot`() {
        val session = loadSession()
        assertTrue(session.killFrequencies.contains("1796956270396220"))
        assertTrue(session.killFrequencies.contains("1793366397640060"))
        assertTrue(session.killFrequencies.contains("1792022045074120"))
    }

    @Test
    fun `frequency step count matches sweep length within tolerance`() {
        val session = loadSession()
        val frequencies = ScanEngine.calculateFrequencySteps(ScanParameters())
        assertTrue(
            "sweep=${session.sweepSteps.size} calc=${frequencies.size}",
            session.sweepSteps.size in (frequencies.size - 2)..(frequencies.size + 2),
        )
    }

    @Test
    fun `frequency steps reproduce recorded boundaries`() {
        val frequencies = ScanEngine.calculateFrequencySteps(ScanParameters())
        assertEquals(15130, frequencies.size)
        assertEquals(41000.0, frequencies[0], 0.0)
        assertEquals(41010.25, frequencies[1], 0.0)
        assertEquals(1799653.390009972, frequencies.last(), 0.0)
    }

    // ═══════════════════════════════════════════════════════════════
    // GOLDEN: detection matches C# exactly
    // ═══════════════════════════════════════════════════════════════

    @Test
    fun `detectHits reproduces C# golden hits exactly`() {
        val session = loadSession()
        val hits = runDetection(session)

        assertEquals("hit count", goldenHits.size, hits.size)
        for (i in goldenHits.indices) {
            val expected = goldenHits[i]
            val actual = hits[i]
            assertEquals("hit[$i].frequency", expected.frequency, actual.frequency, 0.0)
            assertEquals("hit[$i].reading", expected.reading, actual.reading, 0.0)
            assertEquals("hit[$i].runningAverage", expected.runningAverage, actual.runningAverage, 0.0)
            assertEquals("hit[$i].deviation", expected.deviation, actual.deviation, 0.0)
        }
    }

    @Test
    fun `detectHits returns exactly ten hits`() {
        val session = loadSession()
        assertEquals(10, runDetection(session).size)
    }

    @Test
    fun `hits are ordered by deviation descending`() {
        val session = loadSession()
        val hits = runDetection(session)
        for (i in 1 until hits.size) {
            assertTrue(
                "hit ${i - 1} (${hits[i - 1].deviation}) >= hit $i (${hits[i].deviation})",
                hits[i - 1].deviation >= hits[i].deviation,
            )
        }
    }

    @Test
    fun `hits span multiple frequency regions`() {
        val session = loadSession()
        val hits = runDetection(session)

        val distinctBands = hits.map { Math.round(it.frequency / 100_000.0) }.distinct().size
        assertTrue("expected >= 3 distinct bands, got $distinctBands", distinctBands >= 3)

        // Should find a hit in each documented expected region.
        val regions = listOf(1795000.0, 1690000.0, 1643000.0, 1592000.0, 177000.0)
        for (center in regions) {
            val tolHz = center * 1.0 / 100.0
            assertTrue(
                "expected a hit near $center Hz",
                hits.any { abs(it.frequency - center) < tolHz },
            )
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // End-to-end: ScanEngine.runBiofeedbackScan over the replay link
    // ═══════════════════════════════════════════════════════════════

    @Test
    fun `runBiofeedbackScan over replay link produces consistent results`() = runTest {
        val session = loadSession()
        val parameters = ScanParameters(
            startDelayMs = 0,
            minReadDelaySeconds = 0.0,
            enableAmplitudeRampUp = false,
            enableAmplitudeRampDown = false,
            baselineReadCount = session.baselineReadings.size,
        )

        val frequencies = ScanEngine.calculateFrequencySteps(parameters)
        val stepCount = min(frequencies.size, session.sweepSteps.size)

        val link = ReplayGeneratorLink(session, stepCount)
        val engine = ScanEngine(link)

        val hits = engine.runBiofeedbackScan(parameters)

        assertEquals(parameters.maxHits, hits.size)

        // Deviations are identical to the direct-detection golden path; only the
        // frequency alignment shifts by one step because the replay link maps
        // sweep readings by read-count rather than by sweep index (matching C#).
        val expectedDeviations = goldenHits.map { it.deviation }
        assertEquals(expectedDeviations, hits.map { it.deviation })

        val distinctBands = hits.map { Math.round(it.frequency / 100_000.0) }.distinct().size
        assertTrue("expected >= 3 distinct bands, got $distinctBands", distinctBands >= 3)
    }

    /**
     * Fake [GeneratorLink] that replays the recorded dump's device responses.
     *
     * Faithful port of the C# `ReplayGeneratorService`: it counts standalone
     * angle (`:r11=,`) and current (`:r12=,`) reads and returns the recorded
     * baseline then sweep readings in order. All other commands return `"ok"`.
     */
    private class ReplayGeneratorLink(
        private val session: PlainTextDumpParser.HuntAndKillSession,
        @Suppress("unused") private val maxSweepSteps: Int,
    ) : GeneratorLink {

        private var totalAngleReads = 0
        private var totalCurrentReads = 0

        override suspend fun sendCommandWithResponse(command: String): String? {
            if (command == GeneratorProtocol.READ_ANGLE) { // ":r11=,"
                totalAngleReads++
                val baselineTotal = 1 + session.baselineReadings.size
                val angle: Double = if (totalAngleReads <= baselineTotal) {
                    val idx = min(totalAngleReads - 1, session.baselineReadings.size - 1)
                    if (idx >= 0) session.baselineReadings[idx].angle else 52000.0
                } else {
                    val sweepIdx = totalAngleReads - baselineTotal - 1
                    if (sweepIdx in session.sweepSteps.indices) {
                        session.sweepSteps[sweepIdx].angleReading
                    } else {
                        52000.0
                    }
                }
                return ":r11=${formatDeviceDouble(angle)}."
            }

            if (command == GeneratorProtocol.READ_CURRENT) { // ":r12=,"
                totalCurrentReads++
                val current: Double = if (totalCurrentReads <= session.baselineReadings.size) {
                    session.baselineReadings[totalCurrentReads - 1].current
                } else {
                    val sweepIdx = totalCurrentReads - session.baselineReadings.size - 1
                    if (sweepIdx in session.sweepSteps.indices) {
                        session.sweepSteps[sweepIdx].currentReading
                    } else {
                        6900.0
                    }
                }
                return ":r12=${formatDeviceDouble(current)}."
            }

            return "ok"
        }

        override suspend fun sendCommandsBatch(commands: List<String>) { /* no-op replay */ }
        override suspend fun writeFrequencies(frequencies: List<Double>) { /* no-op replay */ }
        override suspend fun start() { /* no-op replay */ }
        override suspend fun stop() { /* no-op replay */ }

        /**
         * Mirror C# `$"{value}"` interpolation for a double sensor reading: the
         * recorded values are whole numbers, so render without a trailing ".0".
         */
        private fun formatDeviceDouble(value: Double): String {
            val rounded = value.roundToLong()
            return if (rounded.toDouble() == value) rounded.toString() else value.toString()
        }
    }
}
