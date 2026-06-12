package com.spooky2.huntkill.core.scan

import com.spooky2.huntkill.core.model.ScanParameters
import com.spooky2.huntkill.core.model.ScanResult
import com.spooky2.huntkill.core.protocol.GeneratorProtocol
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

/**
 * Verifies the zeroed-pause semantics in the kill dwell: pausing mid-dwell clears
 * both frequency channels and drops the amplitude to 0 (the generator goes silent),
 * and resuming restores the amplitude and rewrites the current hit frequency before
 * the dwell continues. The never-paused path sends none of those commands.
 */
class KillPauseZeroTest {

    /** Records every command and [writeFrequencies] call, in order. */
    private class RecordingLink : GeneratorLink {
        val commands = ArrayList<String>()
        val written = ArrayList<Double>()

        override suspend fun sendCommandWithResponse(command: String): String? {
            commands.add(command)
            return "ok"
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
    fun `pause mid-dwell zeroes the generator and resume restores it`() = runTest {
        val link = RecordingLink()
        val engine = ScanEngine(link)
        val gate = PauseGate()
        val params = ScanParameters(dwellSeconds = 10.0, targetAmplitudeCv = 2000)

        val job = launch {
            engine.killHits(hits = listOf(hit(100.0)), parameters = params, pauseGate = gate)
        }

        // Let the kill start: frequency written, first dwell slices running.
        testScheduler.advanceTimeBy(1_500)
        testScheduler.runCurrent()
        assertEquals(listOf(100.0), link.written)

        // Pause; the next ~1s slice boundary hits the pause point and silences output.
        gate.pause()
        testScheduler.advanceTimeBy(1_100)
        testScheduler.runCurrent()
        assertTrue("freq ch1 cleared on pause", link.commands.contains(GeneratorProtocol.CLEAR_FREQUENCY1))
        assertTrue("freq ch2 cleared on pause", link.commands.contains(GeneratorProtocol.CLEAR_FREQUENCY2))
        assertTrue("amplitude 1 zeroed on pause", link.commands.contains(":w28=0,"))
        assertTrue("amplitude 2 zeroed on pause", link.commands.contains(":w29=0,"))
        assertTrue("output 1 stopped on pause", link.commands.contains(GeneratorProtocol.STOP_OUTPUT1))
        assertTrue("output 2 stopped on pause", link.commands.contains(GeneratorProtocol.STOP_OUTPUT2))
        // Still paused: the hit frequency has NOT been rewritten yet.
        assertEquals(listOf(100.0), link.written)

        // Resume: outputs re-started, amplitude restored, hit frequency rewritten.
        gate.resume()
        testScheduler.advanceUntilIdle()
        assertTrue("output 1 restarted on resume", link.commands.contains(GeneratorProtocol.START_OUTPUT1))
        assertTrue("output 2 restarted on resume", link.commands.contains(GeneratorProtocol.START_OUTPUT2))
        assertTrue("amplitude 1 restored on resume", link.commands.contains(":w28=2000,"))
        assertTrue("amplitude 2 restored on resume", link.commands.contains(":w29=2000,"))
        assertEquals("hit frequency rewritten on resume", listOf(100.0, 100.0), link.written)
        assertTrue(job.isCompleted)
    }

    @Test
    fun `never-paused kill sends no zero or restore commands`() = runTest {
        val link = RecordingLink()
        val engine = ScanEngine(link)

        engine.killHits(
            hits = listOf(hit(100.0), hit(200.0)),
            parameters = ScanParameters(dwellSeconds = 0.0),
        )

        assertEquals(listOf(100.0, 200.0), link.written)
        assertTrue(link.commands.none { it == GeneratorProtocol.CLEAR_FREQUENCY1 })
        assertTrue(link.commands.none { it == ":w28=0," })
    }
}
