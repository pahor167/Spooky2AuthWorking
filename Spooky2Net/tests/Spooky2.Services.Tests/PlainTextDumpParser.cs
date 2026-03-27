using System.Text.RegularExpressions;

namespace Spooky2.Services.Tests;

/// <summary>
/// Parses plain-text serial dumps (line-by-line command/response format)
/// as captured in the FullHuntAndKill file.
///
/// Format:
///   :command
///   :ok (or :r11=52458. etc.)
///   :command
///   :ok
/// </summary>
public static class PlainTextDumpParser
{
    /// <summary>A single frequency step during the sweep phase: frequency string + sensor readings.</summary>
    public sealed record SweepStep(
        string FrequencyCommand,    // raw :w24= value string (without ":w24=" prefix and trailing ",")
        double AngleReading,        // :r11 value
        double CurrentReading       // :r12 value
    );

    /// <summary>Parsed structure of a full Hunt and Kill session.</summary>
    public sealed record HuntAndKillSession(
        List<(double Angle, double Current)> BaselineReadings,
        List<SweepStep> SweepSteps,
        List<string> KillFrequencies,    // frequency strings used in kill phase
        int AmplitudeRampSteps
    );

    private static readonly Regex SensorValuePattern = new(
        @":r1[12]=(\d+)\.", RegexOptions.Compiled);

    /// <summary>
    /// Parse a FullHuntAndKill plain-text dump file into structured phases.
    /// </summary>
    public static HuntAndKillSession Parse(string filePath)
    {
        var lines = File.ReadAllLines(filePath);
        return ParseLines(lines);
    }

    /// <summary>
    /// Parse lines (for testability without file I/O).
    /// </summary>
    public static HuntAndKillSession ParseLines(string[] lines)
    {
        // Phase boundaries:
        // 1. Amplitude ramp: :w28/:w29 pairs before first :r11
        // 2. Baseline reads: :r11/:r12 pairs before first long :w24 (milliHz)
        // 3. Sweep: :w24=milliHz → :r11 → :r12 repeating
        // 4. Kill phase: after :n00=...Hunt and Kill..., :w24= commands are kill frequencies

        int killPhaseStart = -1;
        for (int i = 0; i < lines.Length; i++)
        {
            if (lines[i].Contains("Hunt and Kill"))
            {
                killPhaseStart = i;
                break;
            }
        }

        // Count amplitude ramp steps (w28 commands before first r11)
        int rampSteps = 0;
        int firstR11Line = -1;
        for (int i = 0; i < lines.Length; i++)
        {
            if (lines[i].StartsWith(":r11=") && lines[i] != ":r11=,")
            {
                firstR11Line = i;
                break;
            }
            if (lines[i].StartsWith(":w28="))
                rampSteps++;
        }

        // Find first sweep frequency (long :w24 value = milliHz format)
        int firstSweepLine = -1;
        for (int i = 0; i < lines.Length; i++)
        {
            if (i >= (killPhaseStart > 0 ? killPhaseStart : lines.Length))
                break;

            if (lines[i].StartsWith(":w24="))
            {
                var val = ExtractW24Value(lines[i]);
                if (val.Length >= 10) // milliHz format is 10+ digits
                {
                    firstSweepLine = i;
                    break;
                }
            }
        }

        // Parse baseline readings (sensor reads between first r11 response and first sweep frequency)
        var baselineReadings = new List<(double Angle, double Current)>();
        if (firstR11Line > 0 && firstSweepLine > 0)
        {
            double? pendingAngle = null;
            for (int i = firstR11Line; i < firstSweepLine; i++)
            {
                var sensorVal = TryParseSensorValue(lines[i]);
                if (sensorVal == null) continue;

                if (lines[i].StartsWith(":r11=") && lines[i] != ":r11=,")
                {
                    pendingAngle = sensorVal.Value;
                }
                else if (lines[i].StartsWith(":r12=") && lines[i] != ":r12=,")
                {
                    if (pendingAngle.HasValue)
                    {
                        baselineReadings.Add((pendingAngle.Value, sensorVal.Value));
                        pendingAngle = null;
                    }
                }
            }
        }

        // Parse sweep steps: :w24=freq → (next :r11 response) → (next :r12 response)
        var sweepSteps = new List<SweepStep>();
        int scanEnd = killPhaseStart > 0 ? killPhaseStart : lines.Length;

        if (firstSweepLine > 0)
        {
            string? currentFreq = null;
            double? currentAngle = null;

            for (int i = firstSweepLine; i < scanEnd; i++)
            {
                if (lines[i].StartsWith(":w24="))
                {
                    // If we had a pending step, this new w24 means we missed readings — skip
                    currentFreq = ExtractW24Value(lines[i]);
                    currentAngle = null;
                }
                else if (lines[i].StartsWith(":r11=") && lines[i] != ":r11=,")
                {
                    var val = TryParseSensorValue(lines[i]);
                    if (val.HasValue)
                        currentAngle = val.Value;
                }
                else if (lines[i].StartsWith(":r12=") && lines[i] != ":r12=,")
                {
                    var val = TryParseSensorValue(lines[i]);
                    if (val.HasValue && currentFreq != null && currentAngle.HasValue)
                    {
                        sweepSteps.Add(new SweepStep(currentFreq, currentAngle.Value, val.Value));
                        currentFreq = null;
                        currentAngle = null;
                    }
                }
            }
        }

        // Parse kill phase frequencies
        var killFrequencies = new List<string>();
        if (killPhaseStart > 0)
        {
            for (int i = killPhaseStart; i < lines.Length; i++)
            {
                if (lines[i].StartsWith(":w24="))
                {
                    var val = ExtractW24Value(lines[i]);
                    if (val.Length >= 10 && !killFrequencies.Contains(val))
                        killFrequencies.Add(val);
                }
            }
        }

        return new HuntAndKillSession(
            baselineReadings,
            sweepSteps,
            killFrequencies,
            rampSteps
        );
    }

    private static string ExtractW24Value(string line)
    {
        // ":w24=41010256," → "41010256"
        if (!line.StartsWith(":w24=")) return "";
        return line[5..].TrimEnd(',').Trim();
    }

    private static double? TryParseSensorValue(string line)
    {
        // ":r11=52458." → 52458
        // ":r12=7106." → 7106
        var match = SensorValuePattern.Match(line);
        if (match.Success && double.TryParse(match.Groups[1].Value,
                System.Globalization.NumberStyles.Float,
                System.Globalization.CultureInfo.InvariantCulture,
                out var val))
        {
            return val;
        }
        return null;
    }
}
