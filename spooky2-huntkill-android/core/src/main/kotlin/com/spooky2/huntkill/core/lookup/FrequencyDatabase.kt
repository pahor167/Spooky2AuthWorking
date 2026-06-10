package com.spooky2.huntkill.core.lookup

import java.io.Reader

/**
 * One program/condition entry from the bundled Spooky2 frequency database
 * (`Frequencies_decrypted.csv`). Mirrors the C# reference `DatabaseEntry`
 * used by `ScanService.ReverseLookup`: a named program belongs to a [database]
 * code (RIFE, CAFL, ETDF, …) and carries one or more treatment [frequencies].
 */
data class ProgramEntry(
    val name: String,
    val database: String,
    val notes: String,
    val frequencies: DoubleArray,
) {
    // DoubleArray breaks data-class structural equality; provide value semantics so
    // entries compare by content (useful in tests and de-duplication).
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ProgramEntry) return false
        return name == other.name &&
            database == other.database &&
            notes == other.notes &&
            frequencies.contentEquals(other.frequencies)
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + database.hashCode()
        result = 31 * result + notes.hashCode()
        result = 31 * result + frequencies.contentHashCode()
        return result
    }
}

/**
 * Parsed frequency database: the list of usable [entries] plus the count of
 * [skippedLines] that could not be parsed (malformed rows, or rows with no positive
 * frequency). [declaredCount] is the entry count the source file claims in its
 * `#<date>. <n> Entries.` header (0 when absent). Pure data, no Android dependencies.
 *
 * Note: [declaredCount] (11731 for the bundled CSV) counts every source record,
 * including range-only / empty-frequency rows that aren't matchable; [size] counts
 * only the rows that yielded at least one positive frequency, mirroring the line-based
 * parsing of the C# reference `TestDatabaseService`.
 */
data class FrequencyDatabase(
    val entries: List<ProgramEntry>,
    val skippedLines: Int,
    val declaredCount: Int = 0,
) {
    val size: Int get() = entries.size
}

/**
 * Streaming parser for the decrypted Spooky2 CSV. Pure Kotlin: consumes a
 * [Reader] (or a line [Sequence]) and never touches Android APIs, so it runs in
 * plain JVM unit tests against the recorded CSV.
 *
 * CSV shape (see `Frequencies_decrypted.csv`):
 *   line 1  = header comment `#<date>. <n> Entries.`
 *   rows    = `"Name",DBCODE,<int>,"notes","f1,f2,…,","bodypart",?,dwellSeconds`
 *
 * Quoted fields may contain commas; the frequency-list field is itself a
 * comma-separated list with a trailing comma. Lines that don't yield a name +
 * at least one positive frequency are skipped and counted, mirroring the
 * defensive parsing in the C# `TestDatabaseService`.
 */
object FrequencyDatabaseParser {

    private const val NAME_INDEX = 0
    private const val DATABASE_INDEX = 1
    private const val NOTES_INDEX = 3
    private const val FREQUENCIES_INDEX = 4
    private const val MIN_FIELDS = 5

    fun parse(reader: Reader): FrequencyDatabase =
        parse(reader.buffered().lineSequence())

    fun parse(lines: Sequence<String>): FrequencyDatabase {
        val entries = ArrayList<ProgramEntry>()
        var skipped = 0
        var declaredCount = 0

        for (line in lines) {
            // The `#<date>. <n> Entries.` header carries the source's own record count.
            if (line.startsWith("#")) {
                declaredCount = parseDeclaredCount(line) ?: declaredCount
                continue
            }
            // Blank lines are structural, not malformed data rows.
            if (line.isEmpty()) continue

            val entry = parseLine(line)
            if (entry == null) skipped++ else entries.add(entry)
        }

        return FrequencyDatabase(entries, skipped, declaredCount)
    }

    /** Extract `<n>` from a `#<date>. <n> Entries.` header line, or null. */
    private fun parseDeclaredCount(header: String): Int? =
        Regex("(\\d+)\\s+Entries").find(header)?.groupValues?.get(1)?.toIntOrNull()

    private fun parseLine(line: String): ProgramEntry? {
        val fields = splitCsv(line)
        if (fields.size < MIN_FIELDS) return null

        val name = fields[NAME_INDEX].trim().trim('"').trim()
        if (name.isEmpty()) return null

        val database = fields[DATABASE_INDEX].trim().trim('"').trim()
        val notes = fields[NOTES_INDEX].trim().trim('"')

        val frequencies = parseFrequencies(fields[FREQUENCIES_INDEX])
        if (frequencies.isEmpty()) return null

        return ProgramEntry(
            name = name,
            database = database,
            notes = notes,
            frequencies = frequencies.toDoubleArray(),
        )
    }

    /**
     * Parse the quoted frequency-list field: a comma-separated list of doubles with a
     * trailing comma (e.g. `"40,490,450,"`). Non-numeric or non-positive tokens are
     * dropped, mirroring the C# `val > 0` filter.
     */
    private fun parseFrequencies(field: String): List<Double> {
        val inner = field.trim().trim('"')
        if (inner.isEmpty()) return emptyList()

        val result = ArrayList<Double>()
        for (token in inner.split(',')) {
            val trimmed = token.trim()
            if (trimmed.isEmpty()) continue
            val value = trimmed.toDoubleOrNull() ?: continue
            if (value > 0.0) result.add(value)
        }
        return result
    }

    /**
     * Split one CSV line into raw fields, respecting double-quoted regions so commas
     * inside quotes (names, notes, frequency lists) don't break the field. Quotes are
     * left in place here; callers trim them. Mirrors the C# `ParseCsvLine`.
     */
    private fun splitCsv(line: String): List<String> {
        val fields = ArrayList<String>()
        var inQuotes = false
        var start = 0
        for (i in line.indices) {
            val c = line[i]
            if (c == '"') {
                inQuotes = !inQuotes
            } else if (c == ',' && !inQuotes) {
                fields.add(line.substring(start, i))
                start = i + 1
            }
        }
        fields.add(line.substring(start))
        return fields
    }
}
