package com.spooky2.huntkill.core.scan

import com.spooky2.huntkill.core.model.ScanParameters
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for the deterministic [DropoutDetector]: hard-failure passthrough,
 * the deviation heuristic (catches an injected plateau, ignores normal
 * variation), and contiguous-run merging into segments.
 */
class DropoutDetectorTest {

    private val params = ScanParameters(
        dropoutMinRunLength = 3,
        dropoutDeviationFraction = 0.10,
        dropoutMedianWindow = 25,
    )

    /** A steady ~52000 signal with small (<5%) wobble — normal biofeedback. */
    private fun normalSignal(n: Int): FloatArray =
        FloatArray(n) { i -> 52000f + (if (i % 2 == 0) 200f else -200f) }

    @Test
    fun `hard-failed reads are flagged invalid`() {
        val readings = normalSignal(60)
        val hard = BooleanArray(60)
        hard[10] = true
        hard[11] = true
        hard[12] = true

        val valid = DropoutDetector.computeValidity(readings, hard, params)

        assertFalse(valid[10])
        assertFalse(valid[11])
        assertFalse(valid[12])
        assertTrue(valid[9])
        assertTrue(valid[13])
    }

    @Test
    fun `injected plateau run is flagged by heuristic`() {
        val readings = normalSignal(80)
        // Cable half-broken: collapse a stretch to a far-away low plateau (~30000).
        for (i in 30..40) readings[i] = 30000f

        val valid = DropoutDetector.computeValidity(readings, BooleanArray(80), params)

        for (i in 30..40) {
            assertFalse("step $i should be flagged", valid[i])
        }
        assertTrue("step 25 (clean) should be valid", valid[25])
        assertTrue("step 50 (clean) should be valid", valid[50])
    }

    @Test
    fun `normal variation is not flagged`() {
        val readings = normalSignal(200)
        val valid = DropoutDetector.computeValidity(readings, BooleanArray(200), params)
        assertTrue("no step should be flagged on a normal signal", valid.all { it })
    }

    @Test
    fun `short blip below min run length is not flagged`() {
        val readings = normalSignal(60)
        // Two-step spike: below minRunLength=3, must NOT be flagged.
        readings[20] = 30000f
        readings[21] = 30000f

        val valid = DropoutDetector.computeValidity(readings, BooleanArray(60), params)

        assertTrue("2-step blip should stay valid", valid[20] && valid[21])
    }

    @Test
    fun `adjacent flagged runs merge into one segment`() {
        val valid = BooleanArray(20) { true }
        for (i in 5..7) valid[i] = false
        for (i in 8..10) valid[i] = false // contiguous with the previous run

        val freqs = (0 until 20).map { 1000.0 + it * 10 }
        val segments = DropoutDetector.mergeSegments(valid, freqs)

        assertEquals(1, segments.size)
        assertEquals(5, segments[0].startStep)
        assertEquals(10, segments[0].endStep)
        assertEquals(1050.0, segments[0].startFrequency, 0.0)
        assertEquals(1100.0, segments[0].endFrequency, 0.0)
    }

    @Test
    fun `separated flagged runs stay distinct segments`() {
        val valid = BooleanArray(30) { true }
        for (i in 5..7) valid[i] = false
        for (i in 20..22) valid[i] = false

        val freqs = (0 until 30).map { 1000.0 + it }
        val segments = DropoutDetector.mergeSegments(valid, freqs)

        assertEquals(2, segments.size)
        assertEquals(5, segments[0].startStep)
        assertEquals(20, segments[1].startStep)
    }
}
