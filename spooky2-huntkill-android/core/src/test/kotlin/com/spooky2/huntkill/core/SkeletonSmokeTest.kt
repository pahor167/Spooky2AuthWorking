package com.spooky2.huntkill.core

import com.spooky2.huntkill.core.model.GeneratorType
import org.junit.Assert.assertEquals
import org.junit.Test

/** Placeholder to prove the JVM test pipeline runs without an emulator (Phase 1). */
class SkeletonSmokeTest {
    @Test
    fun generatorTypes_exist() {
        assertEquals(3, GeneratorType.entries.size)
    }
}
