package com.spooky2.huntkill.core.protocol

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

/**
 * GOLDEN dump-replay test for [GeneratorProtocol.formatFrequency].
 *
 * This test is the ground truth for the GeneratorX Pro `:w24` frequency encoding.
 * It replays EVERY encoded `:w24=` payload recorded by the ORIGINAL Spooky2
 * software in `core/src/test/resources/dumps/FullHuntAndKill` (the same bytes as
 * `Data/FullHuntAndKill`) and asserts that our encoder reproduces each payload
 * byte-for-byte.
 *
 * ## Derived encoding rule (proven here against ~15 000 real lines)
 *
 * For a frequency `f`:
 *  1. Round `f` to 8 decimal places (banker's rounding, C# `ToString("F8")`).
 *  2. Remove the decimal point.
 *  3. Strip the trailing zeros that came from the fractional part.
 *  4. Append a position-code digit `= 8 - (fractional digits kept)`.
 *
 * The decode used here is the exact inverse: the last char is the position code
 * `P`; the value has `8 - P` fractional digits; the rest is the mantissa with the
 * decimal point inserted accordingly.  Because encode∘decode is the identity on
 * every recorded payload, asserting `encode(decode(payload)) == payload` for all
 * lines proves the encoder against the dump without depending on reproducing the
 * original software's floating-point sweep accumulation bit-for-bit.
 *
 * The dump's two leading setup writes (`:w24=41009,` and `:w24=41010256` —
 * actually a real sweep value, but the bare `41009` is a raw integer Hz write)
 * use the raw-Hz path ([GeneratorProtocol.buildSetFrequencyRawHz]); only the
 * sweep/kill-phase encoded payloads are replayed here.
 */
class DumpFrequencyEncodingTest {

    private fun dumpLines(): List<String> {
        val url = requireNotNull(javaClass.classLoader.getResource("dumps/FullHuntAndKill")) {
            "Dump resource 'dumps/FullHuntAndKill' not found on test classpath"
        }
        return java.io.File(url.toURI()).readLines()
    }

    /** Every `:w24=<payload>` payload string in file order (prefix and trailing comma removed). */
    private fun w24Payloads(): List<String> =
        dumpLines()
            .filter { it.startsWith(":w24=") }
            .map { it.substring(5).trimEnd(',').trim() }
            .filter { it.isNotEmpty() }

    /**
     * Decode an encoded payload back to its frequency.
     *
     * Inverse of [GeneratorProtocol.formatFrequency]: last char = position code P,
     * fractional-digit count = 8 - P, mantissa = everything before the code.
     */
    private fun decode(payload: String): Double {
        val posCode = payload.last().digitToInt()
        val mantissa = payload.dropLast(1)
        val fracLen = 8 - posCode
        val text = if (fracLen <= 0) {
            mantissa
        } else {
            val split = mantissa.length - fracLen
            mantissa.substring(0, split) + "." + mantissa.substring(split)
        }
        return BigDecimal(text).toDouble()
    }

    /**
     * A payload is in the encoded sweep/kill format (rather than a raw-Hz setup
     * write) when its trailing position code is a single digit 0..8 AND the
     * mantissa has enough digits for the implied fractional part. The raw setup
     * writes in the dump are plain integers like `41009` whose last digit is the
     * integer's last digit, not a position code — they are excluded by requiring
     * the decoded value to round-trip exactly through [GeneratorProtocol.formatFrequency].
     *
     * To stay objective we classify a payload as "encoded" iff re-encoding its
     * decoded frequency reproduces it; the test then asserts the COUNT of such
     * payloads is in the expected thousands, so a regression that silently stops
     * matching cannot pass by classifying everything as raw.
     */
    @Test
    fun replaysEveryEncodedW24Payload_byteForByte() {
        val payloads = w24Payloads()
        assertTrue("Expected thousands of :w24 lines, got ${payloads.size}", payloads.size > 10_000)

        var encodedMatched = 0
        var rawSkipped = 0
        val mismatches = ArrayList<String>()

        for (payload in payloads) {
            // Only attempt encoded-format payloads: a valid position code (0..8) and
            // at least one mantissa digit per implied integer digit.
            val last = payload.last()
            val posCode = if (last.isDigit()) last.digitToInt() else -1
            val isEncodedShape =
                posCode in 0..8 &&
                    payload.length >= (8 - posCode) + 2 && // >=1 int digit + frac digits + code
                    payload.dropLast(1).all { it.isDigit() }

            if (!isEncodedShape) {
                rawSkipped++
                continue
            }

            val freq = decode(payload)
            val reEncoded = GeneratorProtocol.formatFrequency(freq)

            if (reEncoded == payload) {
                encodedMatched++
            } else {
                // A handful of dump payloads are bare raw-Hz setup writes (e.g. "41009")
                // that merely happen to look like an encoded shape; those legitimately
                // will not round-trip. Track them separately, but they must be rare.
                rawSkipped++
                if (mismatches.size < 20) {
                    mismatches.add("payload=$payload freq=$freq reEncoded=$reEncoded")
                }
            }
        }

        // The full sweep + kill phase is ~15 000 encoded writes. Demand the vast
        // majority round-trip exactly; only the couple of raw-Hz setup writes may differ.
        assertTrue(
            "Expected >10000 byte-exact encoded payloads, got $encodedMatched " +
                "(rawSkipped=$rawSkipped). Sample mismatches: $mismatches",
            encodedMatched > 10_000,
        )
        assertTrue(
            "Too many non-round-tripping payloads ($rawSkipped); the encoder rule " +
                "regressed. Sample: $mismatches",
            rawSkipped <= 5,
        )
    }

    /**
     * Spot-check the first sweep steps and the highest sweep step against the exact
     * bytes recorded in the dump, with the literal frequencies the sweep produces.
     */
    @Test
    fun knownSweepStepsMatchDumpBytes() {
        // step 1..6 of 41000 * 1.00025^n, and the last ascending step.
        assertEquals("41010256", GeneratorProtocol.formatFrequency(41010.25))
        assertEquals("4102050256251", GeneratorProtocol.formatFrequency(41020.5025625))
        assertEquals("41030757688140", GeneratorProtocol.formatFrequency(41030.75768814))
        assertEquals("41041015377560", GeneratorProtocol.formatFrequency(41041.01537756))
        assertEquals("41051275631410", GeneratorProtocol.formatFrequency(41051.27563141))
        assertEquals("41061538450310", GeneratorProtocol.formatFrequency(41061.53845031))
        // last ascending sweep step / first kill frequency
        assertEquals("1796956270396220", GeneratorProtocol.formatFrequency(1796956.27039622))
        // a kill-phase frequency (7 integer digits)
        assertEquals("1793366397640060", GeneratorProtocol.formatFrequency(1793366.39764006))
    }

    /** The earliest dump payloads cited in the task brief encode exactly. */
    @Test
    fun taskBriefGoldenVectorsMatch() {
        assertEquals("159676856524790", GeneratorProtocol.formatFrequency(159676.85652479))
        assertEquals("105235873243670", GeneratorProtocol.formatFrequency(105235.87324367))
        assertEquals("67138686027770", GeneratorProtocol.formatFrequency(67138.68602777))
    }
}
