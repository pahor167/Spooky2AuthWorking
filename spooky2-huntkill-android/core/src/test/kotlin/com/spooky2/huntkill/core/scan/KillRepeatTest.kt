package com.spooky2.huntkill.core.scan

import com.spooky2.huntkill.core.model.ScanParameters
import com.spooky2.huntkill.core.model.ScanResult
import com.spooky2.huntkill.core.protocol.GeneratorProtocol
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

/**
 * Verifies the kill-phase "repeat" mode: with [repeatEnabled] returning true the kill
 * loops over the full frequency sequence again at each pass boundary, and stops once the
 * flag flips to false. The default (`repeatEnabled = { false }`) treats the sequence
 * exactly once (back-compat with the single-pass kill).
 */
class KillRepeatTest {

    /** Records every [writeFrequencies] call so the write order/length can be asserted. */
    private class RecordingLink : GeneratorLink {
        val written = ArrayList<Double>()
        var stopCount = 0

        override suspend fun sendCommandWithResponse(command: String): String? = when (command) {
            GeneratorProtocol.READ_ANGLE -> ":r11=52000."
            GeneratorProtocol.READ_CURRENT -> ":r12=6900."
            else -> "ok"
        }

        override suspend fun sendCommandsBatch(commands: List<String>) {}
        override suspend fun writeFrequencies(frequencies: List<Double>) {
            written.addAll(frequencies)
        }
        override suspend fun start() {}
        override suspend fun stop() {
            stopCount++
        }
    }

    private fun hit(freq: Double) = ScanResult(
        frequency = freq,
        reading = 0.0,
        deviation = 1.0,
        hitCount = 1,
        timestamp = Instant.EPOCH,
    )

    @Test
    fun `repeat loops the full sequence until the flag turns off`() = runTest {
        val freqs = listOf(100.0, 200.0, 300.0)
        val link = RecordingLink()
        val engine = ScanEngine(link)

        // Return true for the first two pass-ends, then false: pass 1 and pass 2 both run,
        // pass 3 starts because the flag was true after pass 2, then the flag flips and the
        // loop ends. So the full sequence is written exactly three times.
        var passEnds = 0
        val repeatEnabled = { passEnds++ < 2 }

        engine.killHits(
            hits = freqs.map { hit(it) },
            parameters = ScanParameters(dwellSeconds = 0.0),
            repeatEnabled = repeatEnabled,
        )

        // Looped: the full sequence appears at least twice (here exactly three passes).
        assertEquals(freqs + freqs + freqs, link.written)
        // link.stop() runs exactly once when the loops end.
        assertEquals(1, link.stopCount)
    }

    @Test
    fun `repeat off treats the sequence exactly once`() = runTest {
        val freqs = listOf(100.0, 200.0, 300.0)
        val link = RecordingLink()
        val engine = ScanEngine(link)

        // Default repeatEnabled = { false }: single pass, back-compat with the old kill.
        engine.killHits(
            hits = freqs.map { hit(it) },
            parameters = ScanParameters(dwellSeconds = 0.0),
        )

        assertEquals(freqs, link.written)
        assertEquals(1, link.stopCount)
    }
}
