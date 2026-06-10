package com.spooky2.huntkill.data

/**
 * TEST-ONLY parser for the plain-text serial dumps (line-by-line command/response)
 * used to drive a [com.spooky2.huntkill.transport.fake.FakeTransport] from a recorded
 * `dumps/FullHuntAndKill` fixture.
 *
 * Parses an in-memory line list (read from a `src/test/resources` dump), keeping it
 * free of `java.io.File`. The runtime app has no demo replay path, so this lives in
 * the test source set as part of the no-hardware verification backbone.
 */
object PlainTextDumpParser {

    data class SweepStep(
        val frequencyCommand: String,
        val angleReading: Double,
        val currentReading: Double,
    )

    data class BaselineReading(val angle: Double, val current: Double)

    data class HuntAndKillSession(
        val baselineReadings: List<BaselineReading>,
        val sweepSteps: List<SweepStep>,
        val killFrequencies: List<String>,
        val amplitudeRampSteps: Int,
    )

    private val sensorValuePattern = Regex(""":r1[12]=(\d+)\.""")

    fun parseLines(lines: List<String>): HuntAndKillSession {
        var killPhaseStart = -1
        for (i in lines.indices) {
            if (lines[i].contains("Hunt and Kill")) {
                killPhaseStart = i
                break
            }
        }

        var rampSteps = 0
        var firstR11Line = -1
        for (i in lines.indices) {
            if (lines[i].startsWith(":r11=") && lines[i] != ":r11=,") {
                firstR11Line = i
                break
            }
            if (lines[i].startsWith(":w28=")) rampSteps++
        }

        var firstSweepLine = -1
        for (i in lines.indices) {
            if (i >= (if (killPhaseStart > 0) killPhaseStart else lines.size)) break
            if (lines[i].startsWith(":w24=")) {
                val value = extractW24Value(lines[i])
                if (value.length >= 10) {
                    firstSweepLine = i
                    break
                }
            }
        }

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
        if (!line.startsWith(":w24=")) return ""
        return line.substring(5).trimEnd(',').trim()
    }

    private fun tryParseSensorValue(line: String): Double? {
        val match = sensorValuePattern.find(line) ?: return null
        return match.groupValues[1].toDoubleOrNull()
    }
}
