package com.spooky2.huntkill.ui.common

import com.spooky2.huntkill.core.lookup.LookupMatch
import org.junit.Assert.assertEquals
import org.junit.Test

class ConditionTagsTest {

    private fun match(name: String) = LookupMatch(
        programName = name,
        database = "RIFE",
        matchedFrequency = 1000.0,
        searchFrequency = 1000.0,
        matchType = "Direct",
        toleranceHz = 1.0,
    )

    private fun labels(vararg names: String) =
        conditionTagsFor(names.map { match(it) }).map { it.label }

    @Test
    fun `covid program names tag C19`() {
        listOf(
            "Coronavirus", "COVID-19", "Covid", "SARS-CoV-2", "SARS CoV",
            "nCoV", "Corona Virus", "C-19",
        ).forEach { assertEquals("'$it' -> C19", listOf("C19"), labels(it)) }
    }

    @Test
    fun `flu program names tag FLU`() {
        listOf("Influenza", "Influenza A", "Flu", "Swine Flu", "Influenzum")
            .forEach { assertEquals("'$it' -> FLU", listOf("FLU"), labels(it)) }
    }

    @Test
    fun `flu does not false-positive on flu-like words`() {
        listOf("Fluke", "Fluid retention", "Acid reflux", "Fluoride", "Flush")
            .forEach { assertEquals("'$it' -> no tag", emptyList<String>(), labels(it)) }
    }

    @Test
    fun `unrelated programs get no tag`() {
        assertEquals(emptyList<String>(), labels("Catarrh", "Lyme", "Borrelia"))
    }

    @Test
    fun `both tags when matches cover covid and flu`() {
        assertEquals(listOf("C19", "FLU"), labels("Coronavirus", "Influenza A"))
    }

    @Test
    fun `null or empty matches yield no tags`() {
        assertEquals(emptyList<ConditionTag>(), conditionTagsFor(null))
        assertEquals(emptyList<ConditionTag>(), conditionTagsFor(emptyList()))
    }
}
