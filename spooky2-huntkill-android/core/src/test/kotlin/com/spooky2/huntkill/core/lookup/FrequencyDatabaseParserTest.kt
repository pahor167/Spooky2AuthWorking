package com.spooky2.huntkill.core.lookup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.InputStreamReader

class FrequencyDatabaseParserTest {

    @Test
    fun `parses a small embedded sample with quoted commas and trailing comma`() {
        val sample = """
            #20260305. 2 Entries.
            "'Erasing' negative thoughts",RUSS,369,,"40,490,450,",,,180
            "40KHz General",XTRA,15,"Apply directly, helps lesions.","40000,",,,900
        """.trimIndent()

        val db = FrequencyDatabaseParser.parse(sample.lineSequence())

        assertEquals(2, db.size)
        assertEquals(0, db.skippedLines)

        val first = db.entries[0]
        assertEquals("'Erasing' negative thoughts", first.name)
        assertEquals("RUSS", first.database)
        assertTrue(first.frequencies.contentEquals(doubleArrayOf(40.0, 490.0, 450.0)))

        val second = db.entries[1]
        assertEquals("40KHz General", second.name)
        assertEquals("XTRA", second.database)
        // Note field contains a comma inside quotes — must survive CSV splitting.
        assertEquals("Apply directly, helps lesions.", second.notes)
        assertTrue(second.frequencies.contentEquals(doubleArrayOf(40000.0)))
    }

    @Test
    fun `malformed lines are skipped and counted`() {
        val sample = """
            #header
            "Good",BIO,1,,"100,200,",,,180
            this,is,not,enough
            "NoFreqs",CAFL,2,,"",,,180
            "ZeroOnly",HC,3,,"0,",,,180
        """.trimIndent()

        val db = FrequencyDatabaseParser.parse(sample.lineSequence())

        assertEquals(1, db.size)
        assertEquals("Good", db.entries[0].name)
        // 3 malformed: too-few-fields, empty freq list, zero-only freq list.
        assertEquals(3, db.skippedLines)
    }

    @Test
    fun `parses the bundled real CSV - declared 11731 entries and a known entry`() {
        val stream = requireNotNull(
            javaClass.classLoader?.getResourceAsStream("frequencies.csv"),
        ) { "frequencies.csv test resource missing" }

        val db = InputStreamReader(stream, Charsets.UTF_8).use { FrequencyDatabaseParser.parse(it) }

        // The CSV header `#20260305. 11731 Entries.` claims 11731 source records.
        assertEquals(11731, db.declaredCount)

        // Most rows yield a matchable entry; a minority (range-only/empty frequency
        // lists, embedded-newline notes) are skipped — same line-based behaviour as the
        // C# reference. Guard the usable count stays in the expected band.
        assertTrue("expected most rows to parse, got ${db.size}", db.size in 11400..11731)
        assertEquals(11731, db.size + db.skippedLines)

        val catarrh = db.entries.firstOrNull { it.name == "Catarrh" && it.database == "RIFE" }
        assertNotNull("expected a RIFE 'Catarrh' entry", catarrh)
        assertTrue(
            "expected Catarrh to contain 1800000 Hz",
            catarrh!!.frequencies.any { it == 1_800_000.0 },
        )
    }
}
