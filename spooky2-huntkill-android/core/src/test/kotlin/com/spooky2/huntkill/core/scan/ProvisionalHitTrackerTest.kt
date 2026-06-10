package com.spooky2.huntkill.core.scan

import com.spooky2.huntkill.core.model.ProvisionalHit
import com.spooky2.huntkill.core.model.ScanParameters
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.min

/**
 * Convergence test for the live [ProvisionalHitTracker]: on clean data the
 * tracker's FINAL top-N state must equal the retrospective [ScanEngine.detectHits]
 * result — same sweep-step indices, same |deviation| values, same ordering.
 *
 * Also verifies invalid sweep steps never appear as provisional hits.
 */
class ProvisionalHitTrackerTest {

    private fun dumpPath(): String {
        val url = requireNotNull(javaClass.classLoader.getResource("dumps/FullHuntAndKill")) {
            "Dump resource 'dumps/FullHuntAndKill' not found on test classpath"
        }
        return java.io.File(url.toURI()).absolutePath
    }

    /** Build the same `scanReadings` detectHits consumes (baseline pre-seed + sweep). */
    private fun buildScanReadings(
        session: PlainTextDumpParser.HuntAndKillSession,
        parameters: ScanParameters,
        frequencies: List<Double>,
        stepCount: Int,
    ): Pair<List<Double>, List<Pair<Double, Double>>> {
        val baselineTail = session.baselineReadings
            .takeLast(parameters.raWindow)
            .map { if (parameters.useCurrent) it.current else it.angle }

        val scanReadings = ArrayList<Pair<Double, Double>>()
        for (value in baselineTail) scanReadings.add(0.0 to value)
        for (i in 0 until stepCount) {
            val reading = if (parameters.useCurrent) {
                session.sweepSteps[i].currentReading
            } else {
                session.sweepSteps[i].angleReading
            }
            scanReadings.add(frequencies[i] to reading)
        }
        return baselineTail to scanReadings
    }

    /** Map detectHits results to sweep-step indices by frequency (exact match on the step freq). */
    private fun detectHitsAsProvisional(
        scanReadings: List<Pair<Double, Double>>,
        parameters: ScanParameters,
        preSeedCount: Int,
    ): List<ProvisionalHit> {
        val hits = ScanEngine.detectHits(scanReadings, parameters)
        // freq -> sweep-step index (scanReadings index minus the baseline pre-seed).
        val freqToStep = HashMap<Double, Int>()
        for (i in scanReadings.indices) {
            val sweepIdx = i - preSeedCount
            if (sweepIdx >= 0) freqToStep.putIfAbsent(scanReadings[i].first, sweepIdx)
        }
        // detectHits reports a peak at readings index p at the frequency of step p+1
        // (the +1 step pairing), so the PEAK sweep-step index is the freq's step - 1.
        return hits.map {
            val freqStep = requireNotNull(freqToStep[it.frequency])
            ProvisionalHit(
                stepIndex = freqStep - 1,
                frequency = it.frequency,
                deviation = it.deviation,
            )
        }
    }

    @Test
    fun `tracker final state converges to detectHits on clean golden data`() {
        val session = PlainTextDumpParser.parse(dumpPath())
        val parameters = ScanParameters()
        val frequencies = ScanEngine.calculateFrequencySteps(parameters)
        val stepCount = min(frequencies.size, session.sweepSteps.size)

        val (baselineTail, scanReadings) = buildScanReadings(session, parameters, frequencies, stepCount)
        val preSeedCount = baselineTail.size

        // Live tracker fed the SAME data, step by step.
        val tracker = ProvisionalHitTracker(parameters)
        tracker.seedBaseline(baselineTail)
        for (i in 0 until stepCount) {
            tracker.push(i, frequencies[i], scanReadings[preSeedCount + i].second, valid = true)
        }

        val expected = detectHitsAsProvisional(scanReadings, parameters, preSeedCount)
        val actual = tracker.topHits()

        assertEquals("hit count", expected.size, actual.size)
        assertEquals("expected 10 golden hits", 10, actual.size)
        for (i in expected.indices) {
            assertEquals("hit[$i].stepIndex", expected[i].stepIndex, actual[i].stepIndex)
            assertEquals("hit[$i].frequency", expected[i].frequency, actual[i].frequency, 0.0)
            assertEquals("hit[$i].deviation", expected[i].deviation, actual[i].deviation, 0.0)
        }
    }

    @Test
    fun `invalid sweep steps never appear as provisional hits`() {
        // Synthetic sweep: a flat baseline then a single sharp spike. Marking the
        // spike step invalid must exclude it (and it must NOT be used as a neighbor).
        val parameters = ScanParameters(raWindow = 4, threshold = 0.0)
        val tracker = ProvisionalHitTracker(parameters)
        // Seed a full, flat window so deviations are well-defined from the first step.
        tracker.seedBaseline(List(parameters.raWindow) { 100.0 })

        // Steps: flat 100s with one spike at index 3, flagged INVALID.
        val readings = listOf(100.0, 100.0, 100.0, 9999.0, 100.0, 100.0, 100.0)
        val invalidStep = 3
        for (i in readings.indices) {
            tracker.push(i, 1000.0 + i, readings[i], valid = i != invalidStep)
        }

        val hits = tracker.topHits()
        assertTrue(
            "invalid spike step $invalidStep must not be a provisional hit: $hits",
            hits.none { it.stepIndex == invalidStep },
        )
    }

    @Test
    fun `tracker honors maxHits cap`() {
        val parameters = ScanParameters(raWindow = 4, threshold = 0.0, maxHits = 2)
        val tracker = ProvisionalHitTracker(parameters)
        tracker.seedBaseline(List(parameters.raWindow) { 100.0 })

        // Three increasingly tall spikes separated by flat valleys -> 3 local maxima.
        val readings = listOf(
            100.0, 200.0, 100.0, // spike +100 at idx 1
            100.0, 400.0, 100.0, // spike +~300 at idx 4
            100.0, 300.0, 100.0, // spike +~200 at idx 7
        )
        for (i in readings.indices) tracker.push(i, 1000.0 + i, readings[i], valid = true)

        val hits = tracker.topHits()
        assertEquals("capped to maxHits", 2, hits.size)
        // Top two by |deviation| are the +300 (idx 4) and +200 (idx 7) spikes.
        assertEquals(setOf(4, 7), hits.map { it.stepIndex }.toSet())
    }
}
