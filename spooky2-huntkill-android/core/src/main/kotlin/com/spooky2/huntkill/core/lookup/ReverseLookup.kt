package com.spooky2.huntkill.core.lookup

import kotlin.math.abs

/**
 * Parameters for a reverse lookup. Verbatim port of the C# reference
 * `Spooky2.Core.Models.ReverseLookupParameters`.
 *
 * Hunt & Kill defaults differ from the C# record defaults: the FullHuntAndKill
 * report (`Data/FullHuntAndKil/ReverseLookup`) was produced with harmonics OFF,
 * sub-harmonics OFF, and a 0.25% tolerance, so those are the defaults here.
 *
 * @property tolerancePercent matched-frequency tolerance as a percentage of the
 *   search frequency (0.25 → 0.25%).
 * @property includeHarmonics when true, also search octave multiples (2×…[maxHarmonics]×).
 * @property includeSubHarmonics when true, also search octave divisions (1/2…1/[maxHarmonics]).
 * @property includeHz absolute tolerance floor in Hz; the effective tolerance is the
 *   larger of the percentage and this value (C# `Math.Max`). 0 disables the floor.
 * @property maxHarmonics highest harmonic order searched (C# default 20).
 */
data class ReverseLookupParameters(
    val tolerancePercent: Double = 0.25,
    val includeHarmonics: Boolean = false,
    val includeSubHarmonics: Boolean = false,
    val includeHz: Double = 0.0,
    val maxHarmonics: Int = 20,
)

/**
 * One reverse-lookup match. Mirrors the C# `ReverseLookupResult`.
 *
 * @property matchType "Direct", "Harmonic 2x", "Sub-harmonic 1/3", …
 * @property searchFrequency the (possibly harmonic-shifted) frequency that matched.
 */
data class LookupMatch(
    val programName: String,
    val database: String,
    val matchedFrequency: Double,
    val searchFrequency: Double,
    val matchType: String,
    val toleranceHz: Double,
) {
    /** Report line, e.g. `Catarrh (RIFE) (1800000 Hz)`. */
    fun toReportLine(): String =
        "$programName ($database) (${formatFrequency(matchedFrequency)} Hz)"
}

/**
 * Reverse frequency lookup: given a detected hit [frequency], find which database
 * programs/conditions contain a matching frequency.
 *
 * Direct port of `ScanService.ReverseLookup(double, ReverseLookupParameters,
 * IDatabaseService)` from the C# reference. The only structural difference is that the
 * C# version loaded one database at a time via `IDatabaseService`; here every entry is
 * already in memory, so we iterate the full [entries] list once. Matching, tolerance,
 * harmonic loop bounds, de-duplication and ordering are identical.
 */
object ReverseLookup {

    private const val DIRECT = "Direct"

    fun lookup(
        frequency: Double,
        entries: List<ProgramEntry>,
        parameters: ReverseLookupParameters = ReverseLookupParameters(),
    ): List<LookupMatch> {
        // Build the list of frequencies to search for: direct plus optional octave
        // harmonics / sub-harmonics (C# loops m=2..MaxHarmonics, d=2..MaxHarmonics).
        val searchFreqs = ArrayList<Pair<Double, String>>()
        searchFreqs.add(frequency to DIRECT)

        if (parameters.includeHarmonics) {
            for (m in 2..parameters.maxHarmonics) {
                searchFreqs.add((frequency * m) to "Harmonic ${m}x")
            }
        }
        if (parameters.includeSubHarmonics) {
            for (d in 2..parameters.maxHarmonics) {
                searchFreqs.add((frequency / d) to "Sub-harmonic 1/$d")
            }
        }

        val matches = ArrayList<LookupMatch>()
        for (entry in entries) {
            for (progFreq in entry.frequencies) {
                for ((searchFreq, matchType) in searchFreqs) {
                    var tolerance = searchFreq * parameters.tolerancePercent / 100.0
                    if (parameters.includeHz > 0.0) {
                        tolerance = maxOf(tolerance, parameters.includeHz)
                    }
                    if (abs(progFreq - searchFreq) <= tolerance) {
                        matches.add(
                            LookupMatch(
                                programName = entry.name,
                                database = entry.database,
                                matchedFrequency = progFreq,
                                searchFrequency = searchFreq,
                                matchType = matchType,
                                toleranceHz = tolerance,
                            ),
                        )
                    }
                }
            }
        }

        // De-duplicate by program name, keeping the best match (Direct over harmonic),
        // then order by match type then program name — mirrors the C# GroupBy/OrderBy.
        return matches
            .groupBy { it.programName }
            .map { (_, group) -> group.minByOrNull { if (it.matchType == DIRECT) 0 else 1 }!! }
            .sortedWith(compareBy({ it.matchType }, { it.programName }))
    }
}

/** Format a frequency the way the original report does: drop a trailing `.0`. */
internal fun formatFrequency(value: Double): String {
    val rounded = value.toLong()
    return if (value == rounded.toDouble()) rounded.toString() else value.toString()
}
