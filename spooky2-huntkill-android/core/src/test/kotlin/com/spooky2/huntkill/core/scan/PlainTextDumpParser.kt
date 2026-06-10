package com.spooky2.huntkill.core.scan

/**
 * Parses plain-text serial dumps (line-by-line command/response format) as
 * captured in the FullHuntAndKill file.
 *
 * Verbatim port of the C# test helper `Spooky2.Services.Tests.PlainTextDumpParser`.
 *
 * Format:
 * ```
 *   :command
 *   :ok   (or :r11=52458. etc.)
 *   :command
 *   :ok
 * ```
 */
object PlainTextDumpParser {

    /** A single frequency step during the sweep phase: frequency string + sensor readings. */
    data class SweepStep(
        /** Raw `:w24=` value string (without the `:w24=` prefix and trailing `,`). */
        val frequencyCommand: String,
        /** `:r11` value. */
        val angleReading: Double,
        /** `:r12` value. */
        val currentReading: Double,
    )

    /** A baseline reading pair. */
    data class BaselineReading(val angle: Double, val current: Double)

    /** Parsed structure of a full Hunt and Kill session. */
    data class HuntAndKillSession(
        val baselineReadings: List<BaselineReading>,
        val sweepSteps: List<SweepStep>,
        /** Frequency strings used in the kill phase. */
        val killFrequencies: List<String>,
        val amplitudeRampSteps: Int,
    )

    private val sensorValuePattern = Regex(""":r1[12]=(\d+)\.""")

    /** Parse a FullHuntAndKill plain-text dump file into structured phases. */
    fun parse(filePath: String): HuntAndKillSession =
        parseLines(java.io.File(filePath).readLines())

    /** Parse already-read lines (for testability without file I/O). */
    fun parseLines(lines: List<String>): HuntAndKillSession {
        // Phase boundaries:
        // 1. Amplitude ramp: :w28/:w29 pairs before first :r11
        // 2. Baseline reads: :r11/:r12 pairs before first long :w24 (milliHz)
        // 3. Sweep: :w24=milliHz -> :r11 -> :r12 repeating
        // 4. Kill phase: after :n00=...Hunt and Kill..., :w24= commands are kill frequencies

        var killPhaseStart = -1
        for (i in lines.indices) {
            if (lines[i].contains("Hunt and Kill")) {
                killPhaseStart = i
                break
            }
        }

        // Count amplitude ramp steps (w28 commands before first r11).
        var rampSteps = 0
        var firstR11Line = -1
        for (i in lines.indices) {
            if (lines[i].startsWith(":r11=") && lines[i] != ":r11=,") {
                firstR11Line = i
                break
            }
            if (lines[i].startsWith(":w28=")) rampSteps++
        }

        // Find first sweep frequency (long :w24 value = milliHz format).
        var firstSweepLine = -1
        for (i in lines.indices) {
            if (i >= (if (killPhaseStart > 0) killPhaseStart else lines.size)) break

            if (lines[i].startsWith(":w24=")) {
                val value = extractW24Value(lines[i])
                if (value.length >= 10) { // milliHz format is 10+ digits
                    firstSweepLine = i
                    break
                }
            }
        }

        // Parse baseline readings (sensor reads between first r11 response and first sweep frequency).
        val baselineReadings = ArrayList<BaselineReading>()
        if (firstR11Line > 0 && firstSweepLine > 0) {
            var pendingAngle: Double? = null
            for (i in firstR11Line until firstSweepLine) {
                val sensorVal = tryParseSensorValue(lines[i]) ?: continue

                if (lines[i].startsWith(":r11=") && lines[i] != ":r11=,") {
                    pendingAngle = sensorVal
                } else if (lines[i].startsWith(":r12=") && lines[i] != ":r12=,") {
                    val angle = pendingAngle
                    if (angle != null) {
                        baselineReadings.add(BaselineReading(angle, sensorVal))
                        pendingAngle = null
                    }
                }
            }
        }

        // Parse sweep steps: :w24=freq -> (next :r11 response) -> (next :r12 response).
        val sweepSteps = ArrayList<SweepStep>()
        val scanEnd = if (killPhaseStart > 0) killPhaseStart else lines.size

        if (firstSweepLine > 0) {
            var currentFreq: String? = null
            var currentAngle: Double? = null

            for (i in firstSweepLine until scanEnd) {
                if (lines[i].startsWith(":w24=")) {
                    currentFreq = extractW24Value(lines[i])
                    currentAngle = null
                } else if (lines[i].startsWith(":r11=") && lines[i] != ":r11=,") {
                    val value = tryParseSensorValue(lines[i])
                    if (value != null) currentAngle = value
                } else if (lines[i].startsWith(":r12=") && lines[i] != ":r12=,") {
                    val value = tryParseSensorValue(lines[i])
                    val freq = currentFreq
                    val angle = currentAngle
                    if (value != null && freq != null && angle != null) {
                        sweepSteps.add(SweepStep(freq, angle, value))
                        currentFreq = null
                        currentAngle = null
                    }
                }
            }
        }

        // Parse kill phase frequencies.
        val killFrequencies = ArrayList<String>()
        if (killPhaseStart > 0) {
            for (i in killPhaseStart until lines.size) {
                if (lines[i].startsWith(":w24=")) {
                    val value = extractW24Value(lines[i])
                    if (value.length >= 10 && !killFrequencies.contains(value)) {
                        killFrequencies.add(value)
                    }
                }
            }
        }

        return HuntAndKillSession(
            baselineReadings = baselineReadings,
            sweepSteps = sweepSteps,
            killFrequencies = killFrequencies,
            amplitudeRampSteps = rampSteps,
        )
    }

    private fun extractW24Value(line: String): String {
        // ":w24=41010256," -> "41010256"
        if (!line.startsWith(":w24=")) return ""
        return line.substring(5).trimEnd(',').trim()
    }

    private fun tryParseSensorValue(line: String): Double? {
        // ":r11=52458." -> 52458 ; ":r12=7106." -> 7106
        val match = sensorValuePattern.find(line) ?: return null
        return match.groupValues[1].toDoubleOrNull()
    }
}
