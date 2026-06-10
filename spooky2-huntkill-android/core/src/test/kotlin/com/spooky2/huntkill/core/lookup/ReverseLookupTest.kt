package com.spooky2.huntkill.core.lookup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.InputStreamReader

/**
 * Ports the relevant cases from the C# `ReverseLookupTests`: runs reverse lookup over
 * the real bundled database for the golden FullHuntAndKill hit frequencies and asserts
 * that the matches expected by the original Spooky2 report are produced.
 */
class ReverseLookupTest {

    private val database: List<ProgramEntry> by lazy {
        val stream = requireNotNull(
            javaClass.classLoader?.getResourceAsStream("frequencies.csv"),
        ) { "frequencies.csv test resource missing" }
        InputStreamReader(stream, Charsets.UTF_8).use { FrequencyDatabaseParser.parse(it).entries }
    }

    // The default Hunt & Kill params: 0.25% tolerance, no harmonics — same as the
    // report header "Match tolerance: .25% / Include Harmonics: No".
    private val params = ReverseLookupParameters(
        tolerancePercent = 0.25,
        includeHarmonics = false,
        includeSubHarmonics = false,
    )

    @Test
    fun `1800000 RIFE Catarrh matches at 1796509 hit`() {
        val results = ReverseLookup.lookup(1796509.68271064, database, params)
        assertContains(results, "Catarrh", "RIFE", 1_800_000.0)
    }

    @Test
    fun `essential oil matches at 1692146 hit`() {
        val results = ReverseLookup.lookup(1692146.81670824, database, params)
        // From the golden report's "Database matches for 1692146.81670824 Hz:" section.
        assertContains(results, "Helichrysum (Helichrysum italicum) Essential Oil (SD)", "SD", 1691463.62)
        assertContains(results, "Lavender Essential Oil (SD)", "SD", 1692986.78)
    }

    @Test
    fun `ETDF and HC matches at 177088 hit`() {
        val results = ReverseLookup.lookup(177088.033864582, database, params)
        assertContains(results, "Aflatoxin 1", "HC", 177000.0)
        assertContains(results, "Hydatid Cyst", "ETDF", 177250.0)
        assertContains(results, "Factor X Deficiency", "KHZ", 177200.0)
    }

    @Test
    fun `results are de-duplicated by program name and sorted by name`() {
        val results = ReverseLookup.lookup(177088.033864582, database, params)

        // No duplicate program names (C# GroupBy by ProgramName).
        val names = results.map { it.programName }
        assertEquals(names.size, names.toSet().size)

        // All Direct → ordering is purely alphabetical by program name.
        val sorted = names.sorted()
        assertEquals(sorted, names)
    }

    @Test
    fun `tolerance below the gap drops near-but-outside matches`() {
        // Lavender at 1692986.78 is ~840 Hz above the hit; at 0.25% (~4232 Hz) it
        // matches, at a far tighter tolerance it must not.
        val tight = ReverseLookup.lookup(
            1692146.81670824,
            database,
            params.copy(tolerancePercent = 0.001),
        )
        assertTrue(
            "tight tolerance should not include far Lavender match",
            tight.none { it.programName.startsWith("Lavender") && it.matchedFrequency == 1692986.78 },
        )
    }

    @Test
    fun `octave harmonics widen the search when enabled`() {
        // A 528 Hz program (7 Chakras, XTRA) should surface when hunting 264 Hz with
        // sub-harmonics OFF but harmonics ON: 264 * 2 = 528 (octave harmonic).
        val withHarmonics = ReverseLookup.lookup(
            264.0,
            database,
            ReverseLookupParameters(includeHarmonics = true, includeSubHarmonics = false),
        )
        val withoutHarmonics = ReverseLookup.lookup(
            264.0,
            database,
            ReverseLookupParameters(includeHarmonics = false, includeSubHarmonics = false),
        )
        val harmonicMatch = withHarmonics.any { it.matchType == "Harmonic 2x" }
        assertTrue("expected an octave harmonic match at 264 Hz", harmonicMatch)
        assertTrue(
            "harmonic-on search should find at least as many matches",
            withHarmonics.size >= withoutHarmonics.size,
        )
    }

    private fun assertContains(
        results: List<LookupMatch>,
        programName: String,
        database: String,
        matchedFrequency: Double,
    ) {
        val found = results.any {
            it.programName == programName &&
                it.database == database &&
                kotlin.math.abs(it.matchedFrequency - matchedFrequency) < 0.01
        }
        assertTrue(
            "expected match '$programName ($database) ($matchedFrequency Hz)' in:\n" +
                results.joinToString("\n") { "  ${it.toReportLine()}" },
            found,
        )
    }
}
