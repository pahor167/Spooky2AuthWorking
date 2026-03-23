using Spooky2.Core.Models;
using Spooky2.Services.Communication;
using Spooky2.Services.Scanner;
using Xunit;

namespace Spooky2.Services.Tests;

/// <summary>
/// Integration tests that verify the VirtualGenerator and ScanService behavior
/// against actual serial port dump files captured from a real GeneratorX device.
/// </summary>
public class DumpIntegrationTests
{
    /// <summary>
    /// Resolves the path to a dump file in the Data directory.
    /// The Data directory is at the repository root, outside the Spooky2Net solution folder.
    /// </summary>
    private static string GetDumpPath(string filename)
    {
        // Walk up from the test binary output directory to find the Data folder
        // bin/Debug/net10.0 -> tests/Spooky2.Services.Tests -> tests -> Spooky2Net -> spooky_decompiled_new
        var dir = AppContext.BaseDirectory;
        for (int i = 0; i < 10; i++)
        {
            var candidate = Path.Combine(dir, "Data", filename);
            if (File.Exists(candidate))
                return candidate;
            dir = Path.GetDirectoryName(dir)!;
        }

        // Fallback: try absolute path
        var fallback = Path.GetFullPath(
            Path.Combine(AppContext.BaseDirectory, "..", "..", "..", "..", "..", "Data", filename));
        if (File.Exists(fallback))
            return fallback;

        throw new FileNotFoundException(
            $"Dump file '{filename}' not found. Searched up from {AppContext.BaseDirectory}");
    }

    private static bool DumpFilesAvailable()
    {
        try
        {
            GetDumpPath("StartHuntAndKillLonger");
            return true;
        }
        catch (FileNotFoundException)
        {
            return false;
        }
    }

    // ─────────────────────────────────────────────────────────────
    // Test 1: StartHuntAndKillLonger — Startup Sequence
    // ─────────────────────────────────────────────────────────────

    [Fact]
    public void StartHuntAndKillLonger_StartupSequence_MatchesExpected()
    {
        if (!DumpFilesAvailable()) return;

        var path = GetDumpPath("StartHuntAndKillLonger");
        var txCommands = DumpParser.ExtractTxCommands(path);

        // First command: display name
        Assert.StartsWith(":n00=Port 4 - Running Biofeedback", txCommands[0]);

        // Second command: raw Hz frequency (NOT nanoHz)
        Assert.Equal(":w24=41009,", txCommands[1]);

        // Third and fourth: near-zero amplitude
        Assert.Equal(":w28=6,", txCommands[2]);
        Assert.Equal(":w29=6,", txCommands[3]);

        // Enable outputs
        Assert.Equal(":w11=1,,", txCommands[4]);
        Assert.Equal(":w11=,1,", txCommands[5]);

        // Amplitude ramp starts at index 6 with :w28=12,
        Assert.Equal(":w28=12,", txCommands[6]);
        Assert.Equal(":w29=12,", txCommands[7]);
    }

    [Fact]
    public void StartHuntAndKillLonger_AmplitudeRamp_GoesFrom6To2000()
    {
        if (!DumpFilesAvailable()) return;

        var path = GetDumpPath("StartHuntAndKillLonger");
        var txCommands = DumpParser.ExtractTxCommands(path);

        // Collect all :w28 values before the first :r11
        var w28Values = new List<int>();
        foreach (var cmd in txCommands)
        {
            if (cmd == ":r11=,") break;
            if (cmd.StartsWith(":w28="))
            {
                var valStr = cmd[5..].TrimEnd(',');
                if (int.TryParse(valStr, out var val))
                    w28Values.Add(val);
            }
        }

        Assert.True(w28Values.Count > 100, $"Expected >100 ramp steps, got {w28Values.Count}");
        Assert.Equal(6, w28Values[0]);
        Assert.Equal(2000, w28Values[^1]);

        // Verify steps are approximately 6 cV apart
        for (int i = 1; i < w28Values.Count - 1; i++) // skip last which may be exact target
        {
            var step = w28Values[i] - w28Values[i - 1];
            Assert.InRange(step, 4, 8); // ~6 +/- tolerance
        }
    }

    [Fact]
    public void StartHuntAndKillLonger_BaselineReads_PrecedeScan()
    {
        if (!DumpFilesAvailable()) return;

        var path = GetDumpPath("StartHuntAndKillLonger");
        var txCommands = DumpParser.ExtractTxCommands(path);

        // Find the first :r11 (start of baseline)
        int firstR11 = txCommands.FindIndex(c => c == ":r11=,");
        Assert.True(firstR11 > 0, "Should have :r11 commands");

        // Count r11 reads before the first nanoHz frequency command
        int baselineR11Count = 0;
        for (int i = firstR11; i < txCommands.Count; i++)
        {
            if (txCommands[i].StartsWith(":w24="))
            {
                var valStr = txCommands[i][5..].TrimEnd(',');
                if (valStr.Length >= 10) // nanoHz
                    break;
            }
            if (txCommands[i] == ":r11=,")
                baselineR11Count++;
        }

        // The dump shows baseline reads before scanning starts
        Assert.True(baselineR11Count >= 40,
            $"Expected >=40 baseline r11 reads, got {baselineR11Count}");
    }

    [Fact]
    public void StartHuntAndKillLonger_ScanPattern_IsFreqAngleCurrent()
    {
        if (!DumpFilesAvailable()) return;

        var path = GetDumpPath("StartHuntAndKillLonger");
        var txCommands = DumpParser.ExtractTxCommands(path);

        // Find the first nanoHz frequency command (scan start)
        int scanStart = -1;
        for (int i = 0; i < txCommands.Count; i++)
        {
            if (txCommands[i].StartsWith(":w24="))
            {
                var valStr = txCommands[i][5..].TrimEnd(',');
                if (valStr.Length >= 13) // clearly nanoHz (13+ digits)
                {
                    scanStart = i;
                    break;
                }
            }
        }

        Assert.True(scanStart > 0, "Should find nanoHz scan commands");

        // Verify pattern: :w24=nanoHz, :r11=, :r12=, repeating
        for (int i = scanStart; i + 2 < txCommands.Count && i < scanStart + 30; i += 3)
        {
            Assert.StartsWith(":w24=", txCommands[i]);
            Assert.Equal(":r11=,", txCommands[i + 1]);
            Assert.Equal(":r12=,", txCommands[i + 2]);
        }
    }

    // ─────────────────────────────────────────────────────────────
    // Test 2: FinishHuntAndKill — Stop Sequence
    // ─────────────────────────────────────────────────────────────

    [Fact]
    public void FinishHuntAndKill_StopSequence_MatchesExpected()
    {
        if (!DumpFilesAvailable()) return;

        var path = GetDumpPath("FinishHuntAndKill");
        var txCommands = DumpParser.ExtractTxCommands(path);

        // Last commands should be the stop sequence
        var last = txCommands.TakeLast(8).ToList();

        // Find stop markers
        Assert.Contains(":w12=0,,", txCommands);
        Assert.Contains(":w12=,0,", txCommands);

        // The stop sequence at the end:
        // :w12=0,, :w12=,0, :w28=1950, :w29=1950, :n00=... :w24=... :w28=2000, :w29=2000,
        int clearIdx1 = txCommands.LastIndexOf(":w12=0,,");
        int clearIdx2 = txCommands.LastIndexOf(":w12=,0,");
        Assert.True(clearIdx1 < clearIdx2, "Clear freq ch1 before ch2");

        // After clearing, set amplitude 1950
        Assert.Equal(":w28=1950,", txCommands[clearIdx2 + 1]);
        Assert.Equal(":w29=1950,", txCommands[clearIdx2 + 2]);

        // Display name update
        Assert.StartsWith(":n00=Port 4 - GX Hunt and Kill (C)", txCommands[clearIdx2 + 3]);

        // Idle frequency (nanoHz)
        Assert.StartsWith(":w24=", txCommands[clearIdx2 + 4]);

        // Final amplitude 2000
        Assert.Equal(":w28=2000,", txCommands[clearIdx2 + 5]);
        Assert.Equal(":w29=2000,", txCommands[clearIdx2 + 6]);
    }

    // ─────────────────────────────────────────────────────────────
    // Test 3: Amplitude ramp-up matches dump
    // ─────────────────────────────────────────────────────────────

    [Fact]
    public void StartHuntAndKillLonger_AmplitudeRampCount_MatchesDump()
    {
        if (!DumpFilesAvailable()) return;

        var path = GetDumpPath("StartHuntAndKillLonger");
        var txCommands = DumpParser.ExtractTxCommands(path);

        int w28Count = 0;
        var w28Values = new List<int>();

        foreach (var cmd in txCommands)
        {
            if (cmd == ":r11=,") break;
            if (cmd.StartsWith(":w28="))
            {
                w28Count++;
                var valStr = cmd[5..].TrimEnd(',');
                if (int.TryParse(valStr, out var val))
                    w28Values.Add(val);
            }
        }

        // From dump analysis: 331 :w28 commands before first :r11
        Assert.Equal(331, w28Count);

        // Ramp from 6 to 2000 in steps of ~6
        Assert.Equal(6, w28Values.First());
        Assert.Equal(2000, w28Values.Last());
    }

    // ─────────────────────────────────────────────────────────────
    // Test 4: Frequency step matches 0.025%
    // ─────────────────────────────────────────────────────────────

    [Fact]
    public void StartHuntAndKillLonger_FrequencySteps_Match0025Percent()
    {
        if (!DumpFilesAvailable()) return;

        var path = GetDumpPath("StartHuntAndKillLonger");
        var txCommands = DumpParser.ExtractTxCommands(path);

        // Extract nanoHz scan frequencies (long values from :w24=)
        var scanFreqsNanoHz = new List<long>();
        foreach (var cmd in txCommands)
        {
            if (cmd.StartsWith(":w24="))
            {
                var valStr = cmd[5..].TrimEnd(',');
                if (valStr.Length >= 13 && long.TryParse(valStr, out var val))
                    scanFreqsNanoHz.Add(val);
            }
        }

        Assert.True(scanFreqsNanoHz.Count > 10,
            $"Expected >10 scan frequencies, got {scanFreqsNanoHz.Count}");

        // Verify each step is 0.025% of the previous frequency
        // Skip the first frequency (might have different origin)
        int matchCount = 0;
        int totalChecked = 0;

        for (int i = 2; i < Math.Min(100, scanFreqsNanoHz.Count); i++)
        {
            double stepPercent = (double)(scanFreqsNanoHz[i] - scanFreqsNanoHz[i - 1])
                / scanFreqsNanoHz[i - 1] * 100.0;
            totalChecked++;

            // Allow small floating-point tolerance
            if (Math.Abs(stepPercent - 0.025) < 0.001)
                matchCount++;
        }

        double matchRate = (double)matchCount / totalChecked;
        Assert.True(matchRate > 0.80,
            $"Expected >80% of steps to be 0.025%, got {matchRate * 100:F1}% ({matchCount}/{totalChecked})");
    }

    // ─────────────────────────────────────────────────────────────
    // Test 5: Run ScanService with VirtualGenerator
    // ─────────────────────────────────────────────────────────────

    [Fact]
    public async Task ScanService_WithVirtualGenerator_ProducesExpectedCommands()
    {
        var vgen = new VirtualGenerator();
        var svc = new ScanService(vgen);

        var parameters = new ScanParameters
        {
            StartFrequency = 41000,
            EndFrequency = 42000,
            UsePercentageStep = true,
            StepSizePercent = 0.025,
            StartDelayMs = 0,
            MinReadDelaySeconds = 0,
            RaWindow = 20,
            EnableAmplitudeRampUp = true,
            RampUpRateCv = 6,
            TargetAmplitudeCv = 2000,
            BaselineReadCount = 42,
            DetectMax = true,
            Threshold = 0
        };

        await svc.RunBiofeedbackScan(0, parameters);

        var log = vgen.CommandLog;

        // Should start with display name
        Assert.StartsWith(":n00=", log[0]);

        // Should set raw Hz frequency
        Assert.Equal(":w24=41000,", log[1]);

        // Should have amplitude ramp-up
        Assert.Equal(":w28=6,", log[2]);
        Assert.Equal(":w29=6,", log[3]);

        // Should enable outputs
        Assert.Contains(":w11=1,,", log);
        Assert.Contains(":w11=,1,", log);

        // After ramp and scan, amplitude may have been ramped down again
        // Check that the ramp-up commands were sent by verifying the log
        var w28At2000 = log.Where(c => c == ":w28=2000,").ToList();
        Assert.True(w28At2000.Count >= 1, "Should have sent :w28=2000, during ramp-up");

        // Should have baseline reads (42 pairs of r11/r12)
        var r11Count = log.Count(c => c == ":r11=,");
        var r12Count = log.Count(c => c == ":r12=,");
        Assert.True(r11Count >= 42, $"Expected >=42 r11 reads, got {r11Count}");
        Assert.True(r12Count >= 42, $"Expected >=42 r12 reads, got {r12Count}");

        // Should have nanoHz frequency writes during scan
        var nanoHzWrites = log.Where(c =>
            c.StartsWith(":w24=") && c[5..].TrimEnd(',').Length >= 10).ToList();
        Assert.True(nanoHzWrites.Count > 0, "Should have nanoHz frequency writes during scan");

        // Should have cleanup commands
        Assert.Contains(":w12=0,,", log);
        Assert.Contains(":w12=,0,", log);

        // Outputs should have been enabled
        Assert.True(vgen.Output1Enabled || log.Contains(":w11=1,,"));
        Assert.True(vgen.Output2Enabled || log.Contains(":w11=,1,"));
    }

    // ─────────────────────────────────────────────────────────────
    // Test 6: Verify scan produces same command sequence as dump
    // ─────────────────────────────────────────────────────────────

    [Fact]
    public async Task ScanService_CommandSequence_MatchesDumpPattern()
    {
        if (!DumpFilesAvailable()) return;

        var dumpPath = GetDumpPath("StartHuntAndKillLonger");
        var dumpTx = DumpParser.ExtractTxCommands(dumpPath);

        var vgen = new VirtualGenerator();
        var svc = new ScanService(vgen);

        var parameters = new ScanParameters
        {
            StartFrequency = 41009, // Match the dump's start frequency
            EndFrequency = 1_800_000,
            UsePercentageStep = true,
            StepSizePercent = 0.025,
            StartDelayMs = 0,
            MinReadDelaySeconds = 0,
            RaWindow = 20,
            EnableAmplitudeRampUp = true,
            RampUpRateCv = 6,
            TargetAmplitudeCv = 2000,
            BaselineReadCount = 42,
            DetectMax = true,
            Threshold = 0,
            LogName = "Port 4 - Running Biofeedback"
        };

        // Use a cancellation token to stop after enough commands
        var cts = new CancellationTokenSource();
        // Cancel after we have enough commands to compare
        _ = Task.Run(async () =>
        {
            while (vgen.CommandLog.Count < 100)
                await Task.Delay(1);
            cts.Cancel();
        });

        try
        {
            await svc.RunBiofeedbackScan(0, parameters, ct: cts.Token);
        }
        catch (OperationCanceledException)
        {
            // Expected
        }

        var ourCmds = vgen.CommandLog;

        // Compare first commands: display name
        Assert.StartsWith(":n00=Port 4 - Running Biofeedback", dumpTx[0]);
        Assert.StartsWith(":n00=Port 4 - Running Biofeedback", ourCmds[0]);

        // Compare: raw Hz frequency
        Assert.Equal(dumpTx[1], ourCmds[1]); // :w24=41009,

        // Compare: initial amplitude
        Assert.Equal(dumpTx[2], ourCmds[2]); // :w28=6,
        Assert.Equal(dumpTx[3], ourCmds[3]); // :w29=6,

        // Compare: output enables
        Assert.Equal(dumpTx[4], ourCmds[4]); // :w11=1,,
        Assert.Equal(dumpTx[5], ourCmds[5]); // :w11=,1,

        // Compare: first ramp step
        Assert.Equal(dumpTx[6], ourCmds[6]); // :w28=12,
        Assert.Equal(dumpTx[7], ourCmds[7]); // :w29=12,
    }

    // ─────────────────────────────────────────────────────────────
    // VirtualGenerator state tracking tests
    // ─────────────────────────────────────────────────────────────

    [Fact]
    public async Task VirtualGenerator_FeedDumpCommands_TracksState()
    {
        if (!DumpFilesAvailable()) return;

        var path = GetDumpPath("StartHuntAndKillLonger");
        var txCommands = DumpParser.ExtractTxCommands(path);

        var vgen = new VirtualGenerator();

        // Feed first 670 commands (up through ramp + some baseline)
        int firstR11Idx = txCommands.FindIndex(c => c == ":r11=,");
        for (int i = 0; i < firstR11Idx; i++)
        {
            await vgen.SendCommandWithResponse(0, txCommands[i]);
        }

        // After ramp: amplitude should be 2000
        Assert.Equal(2000, vgen.AmplitudeCv1);
        Assert.Equal(2000, vgen.AmplitudeCv2);

        // Outputs should be enabled
        Assert.True(vgen.Output1Enabled);
        Assert.True(vgen.Output2Enabled);

        // Display name should be set
        Assert.Contains("Running Biofeedback", vgen.DisplayName);

        // Frequency should be 41009 (raw Hz from setup)
        Assert.Equal(41009, vgen.CurrentFrequencyHz);
    }

    [Fact]
    public async Task VirtualGenerator_SensorResponses_AreCorrect()
    {
        var vgen = new VirtualGenerator
        {
            BaselineAngle = 53000,
            BaselineCurrent = 6500
        };

        var angleResponse = await vgen.SendCommandWithResponse(0, ":r11=,");
        var currentResponse = await vgen.SendCommandWithResponse(0, ":r12=,");

        Assert.Equal(":r11=53000.", angleResponse);
        Assert.Equal(":r12=6500.", currentResponse);
    }

    [Fact]
    public async Task VirtualGenerator_SpikeInjection_Works()
    {
        var vgen = new VirtualGenerator
        {
            BaselineCurrent = 6000
        };

        vgen.InjectCurrentSpike(3, 9000);

        // Reads 1 and 2: baseline
        var r1 = await vgen.SendCommandWithResponse(0, ":r12=,");
        var r2 = await vgen.SendCommandWithResponse(0, ":r12=,");
        Assert.Equal(":r12=6000.", r1);
        Assert.Equal(":r12=6000.", r2);

        // Read 3: spike
        var r3 = await vgen.SendCommandWithResponse(0, ":r12=,");
        Assert.Equal(":r12=9000.", r3);

        // Read 4: back to baseline
        var r4 = await vgen.SendCommandWithResponse(0, ":r12=,");
        Assert.Equal(":r12=6000.", r4);
    }

    // ─────────────────────────────────────────────────────────────
    // DumpParser unit tests
    // ─────────────────────────────────────────────────────────────

    [Fact]
    public void DumpParser_ExtractTxCommands_ReturnsCorrectCount()
    {
        if (!DumpFilesAvailable()) return;

        var path = GetDumpPath("StartHuntAndKillLonger");
        var txCommands = DumpParser.ExtractTxCommands(path);

        // From analysis: 2704 TX commands
        Assert.Equal(2704, txCommands.Count);
    }

    [Fact]
    public void DumpParser_ExtractPairs_MatchesTxRx()
    {
        if (!DumpFilesAvailable()) return;

        var path = GetDumpPath("StartHuntAndKillLonger");
        var pairs = DumpParser.ExtractPairs(path);

        Assert.True(pairs.Count > 0);

        // First pair: display name -> :ok
        Assert.StartsWith(":n00=Port 4 - Running Biofeedback", pairs[0].Tx);
        Assert.Equal(":ok", pairs[0].Rx);

        // Second pair: frequency -> :ok
        Assert.Equal(":w24=41009,", pairs[1].Tx);
        Assert.Equal(":ok", pairs[1].Rx);
    }

    [Fact]
    public void DumpParser_FinishHuntAndKill_ParsesCorrectly()
    {
        if (!DumpFilesAvailable()) return;

        var path = GetDumpPath("FinishHuntAndKill");
        var txCommands = DumpParser.ExtractTxCommands(path);

        Assert.Equal(1039, txCommands.Count);

        // Last command should be :w29=2000,
        Assert.Equal(":w29=2000,", txCommands[^1]);
    }

    // ─────────────────────────────────────────────────────────────
    // Frequency nanoHz detection test
    // ─────────────────────────────────────────────────────────────

    [Theory]
    [InlineData(":w24=41009,", 41009, false)]  // Raw Hz
    [InlineData(":w24=41020502562510,", 41020.50256251, true)]  // nanoHz
    [InlineData(":w24=1652608154681650,", 1652608.15468165, true)]  // nanoHz
    public async Task VirtualGenerator_FrequencyDetection_RawVsNanoHz(string command, double expectedHz, bool isNanoHz)
    {
        var vgen = new VirtualGenerator();
        await vgen.SendCommandWithResponse(0, command);

        Assert.Equal(expectedHz, vgen.CurrentFrequencyHz, precision: 2);

        if (isNanoHz)
        {
            var valStr = command[5..].TrimEnd(',');
            Assert.Equal(long.Parse(valStr), vgen.CurrentFrequencyNanoHz);
        }
    }
}
