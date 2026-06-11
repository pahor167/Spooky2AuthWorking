package com.spooky2.huntkill.core.scan

import com.spooky2.huntkill.core.model.ScanParameters
import com.spooky2.huntkill.core.model.ScanResult
import com.spooky2.huntkill.core.protocol.GeneratorProtocol
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

/**
 * Verifies the kill-phase "Treat this now" jump: requesting a jump to an index
 * drives the generator to that hit's frequency next and then continues forward
 * (j, j+1, …) rather than restarting at 0.
 */
class KillControlJumpTest {

    /** Records every [writeFrequencies] call so the write order can be asserted. */
    private class RecordingLink : GeneratorLink {
        val written = ArrayList<Double>()

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
        override suspend fun stop() {}
    }

    private fun hit(freq: Double) = ScanResult(
        frequency = freq,
        reading = 0.0,
        deviation = 1.0,
        hitCount = 1,
        timestamp = Instant.EPOCH,
    )

    @Test
    fun `requestJump drives the kill to that index then continues forward`() = runTest {
        // 5 hits with distinct frequencies so writes are unambiguous.
        val freqs = listOf(100.0, 200.0, 300.0, 400.0, 500.0)
        val hits = freqs.map { hit(it) }

        val link = RecordingLink()
        val engine = ScanEngine(link)
        val killControl = KillControl()

        // A jump requested before the dwell of step 0 completes is taken on the first
        // slice of step 0: the engine writes killFreqs[0] (the start), then jumps to
        // index 2 and continues 2,3,4. Zero dwell keeps each slice a single tick.
        killControl.requestJump(2)

        engine.killHits(
            hits = hits,
            parameters = ScanParameters(dwellSeconds = 0.0),
            killControl = killControl,
        )

        // Start hit 0 (100) is written, then the jump drives the generator straight to
        // killFreqs[2]=300 and continues 400, 500 — it does NOT restart at index 0.
        assertEquals(listOf(100.0, 300.0, 400.0, 500.0), link.written)
    }

    @Test
    fun `no jump request runs every hit in order`() = runTest {
        val freqs = listOf(100.0, 200.0, 300.0)
        val link = RecordingLink()
        val engine = ScanEngine(link)

        engine.killHits(
            hits = freqs.map { hit(it) },
            parameters = ScanParameters(dwellSeconds = 0.0),
            killControl = KillControl(),
        )

        // Back-compat: with no jump the kill writes every hit in order, once each.
        assertEquals(freqs, link.written)
    }

    @Test
    fun `out of range jump target is ignored`() = runTest {
        val freqs = listOf(100.0, 200.0, 300.0)
        val link = RecordingLink()
        val engine = ScanEngine(link)
        val killControl = KillControl()

        // An out-of-range target must be ignored (takeJump returns it but the engine
        // guards against indices outside killFreqs), so the kill proceeds normally.
        killControl.requestJump(99)

        engine.killHits(
            hits = freqs.map { hit(it) },
            parameters = ScanParameters(dwellSeconds = 0.0),
            killControl = killControl,
        )

        assertEquals(freqs, link.written)
    }
}
