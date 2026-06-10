package com.spooky2.huntkill.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.Instant

/**
 * Locks the ported Hunt & Kill model defaults to the exact values from the C#
 * reference records (`Spooky2.Core.Models.*`). Any drift breaks these tests.
 */
class ScanParametersDefaultsTest {

    @Test
    fun scanParameters_defaultsMatchReference() {
        val p = ScanParameters()
        assertEquals(41000.0, p.startFrequency, 0.0)
        assertEquals(1800000.0, p.endFrequency, 0.0)
        assertTrue(p.usePercentageStep)
        assertEquals(100.0, p.stepSizeHz, 0.0)
        assertEquals(0.025, p.stepSizePercent, 0.0)
        assertEquals(10, p.maxHits)
        assertEquals(20, p.raWindow)
        assertEquals(0, p.raWindow2)
        assertFalse(p.useRetentiveWindow)
        assertFalse(p.calculateUsingPeak)
        assertEquals(1, p.samplesPerStep)
        assertEquals(200, p.startDelayMs)
        assertEquals(0.07, p.minReadDelaySeconds, 0.0)
        assertTrue(p.detectMax)
        assertFalse(p.detectMin)
        assertFalse(p.useCurrent)
        assertTrue(p.useAngle)
        assertEquals(1, p.loops)
        assertEquals(0.0, p.threshold, 0.0)
        assertTrue(p.continueRefining)
        assertEquals(0, p.runOnGeneratorId)
        assertEquals(180.0, p.dwellSeconds, 0.0)
        assertEquals("", p.logName)
        assertTrue(p.enableAmplitudeRampUp)
        assertEquals(330, p.rampSteps)
        assertEquals(2000, p.targetAmplitudeCv)
        assertTrue(p.enableAmplitudeRampDown)
        assertEquals(203, p.baselineReadCount)
    }

    @Test
    fun scanProgress_defaultsMatchReference() {
        val p = ScanProgress()
        assertEquals(0.0, p.currentFrequency, 0.0)
        assertEquals(0.0, p.percentComplete, 0.0)
        assertEquals(0, p.stepNumber)
        assertEquals(0, p.totalSteps)
        assertEquals(0, p.hitsFound)
        assertEquals("", p.statusText)
        assertEquals(0, p.cycleNumber)
        assertEquals(0, p.amplitudeCv)
        assertEquals(0.0, p.currentReading, 0.0)
        assertEquals(0.0, p.currentRunningAverage, 0.0)
    }

    @Test
    fun scanResult_defaultsMatchReference() {
        val r = ScanResult()
        assertEquals(0.0, r.frequency, 0.0)
        assertEquals(0.0, r.reading, 0.0)
        assertEquals(0.0, r.runningAverage, 0.0)
        assertEquals(0.0, r.deviation, 0.0)
        assertEquals(0, r.hitCount)
        assertEquals("", r.harmonicInfo)
        assertEquals(Instant.EPOCH, r.timestamp)
    }

    @Test
    fun generatorState_defaultsMatchReference() {
        val s = GeneratorState(id = 1)
        assertEquals(1, s.id)
        assertEquals("", s.port)
        assertEquals(GeneratorStatus.Idle, s.status)
        assertEquals(0.0, s.currentFrequency, 0.0)
        assertEquals("", s.currentProgram)
        assertEquals(Duration.ZERO, s.elapsedTime)
    }

    @Test
    fun generatorStatus_hasExpectedValues() {
        assertEquals(
            listOf("Idle", "Running", "Paused", "Held"),
            GeneratorStatus.entries.map { it.name },
        )
    }
}
