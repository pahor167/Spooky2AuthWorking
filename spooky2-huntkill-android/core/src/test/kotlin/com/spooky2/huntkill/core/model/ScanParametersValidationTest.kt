package com.spooky2.huntkill.core.model

import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.test.assertFailsWith

/**
 * Verifies the [ScanParameters.init] contract: [ScanParameters.samplesPerStep] must be > 0.
 * A zero value would cause a divide-by-zero in [ScanEngine.readSensorsTracked].
 */
class ScanParametersValidationTest {

    @Test
    fun `samplesPerStep zero throws IllegalArgumentException`() {
        assertFailsWith<IllegalArgumentException> {
            ScanParameters(samplesPerStep = 0)
        }
    }

    @Test
    fun `samplesPerStep negative throws IllegalArgumentException`() {
        assertFailsWith<IllegalArgumentException> {
            ScanParameters(samplesPerStep = -1)
        }
    }

    @Test
    fun `samplesPerStep one is valid`() {
        val p = ScanParameters(samplesPerStep = 1)
        assertEquals(1, p.samplesPerStep)
    }
}
