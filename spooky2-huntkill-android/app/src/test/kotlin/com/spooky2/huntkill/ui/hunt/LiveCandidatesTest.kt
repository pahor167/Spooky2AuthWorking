package com.spooky2.huntkill.ui.hunt

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LiveCandidatesTest {

    private fun marker(step: Int, freq: Double, dev: Double, isFinal: Boolean = false) =
        GraphMarker(stepIndex = step, frequency = freq, deviation = dev, isFinal = isFinal)

    @Test
    fun `empty markers yields empty rows`() {
        assertEquals(emptyList<CandidateRow>(), candidateRows(emptyList()))
    }

    @Test
    fun `rows sorted by deviation descending`() {
        val rows = candidateRows(
            listOf(
                marker(step = 1, freq = 1000.0, dev = 12.5),
                marker(step = 2, freq = 2000.0, dev = 96.2),
                marker(step = 3, freq = 3000.0, dev = 40.0),
            ),
        )
        assertEquals(listOf(96.2, 40.0, 12.5), rows.map { it.marker.deviation })
    }

    @Test
    fun `ties broken by frequency ascending for stable order`() {
        val rows = candidateRows(
            listOf(
                marker(step = 1, freq = 3000.0, dev = 50.0),
                marker(step = 2, freq = 1000.0, dev = 50.0),
                marker(step = 3, freq = 2000.0, dev = 50.0),
            ),
        )
        assertEquals(listOf(1000.0, 2000.0, 3000.0), rows.map { it.marker.frequency })
    }

    @Test
    fun `labels formatted with asHz and one-decimal deviation`() {
        val rows = candidateRows(listOf(marker(step = 1, freq = 1795160.43, dev = 96.24)))
        val row = rows.single()
        assertEquals("1,795,160.43 Hz", row.frequencyLabel)
        assertEquals("dev 96.2", row.deviationLabel)
    }

    @Test
    fun `row retains its source marker for tap wiring`() {
        val m = marker(step = 7, freq = 5000.0, dev = 80.0, isFinal = true)
        val row = candidateRows(listOf(m)).single()
        assertEquals(m, row.marker)
        assertTrue(row.marker.isFinal)
    }
}
