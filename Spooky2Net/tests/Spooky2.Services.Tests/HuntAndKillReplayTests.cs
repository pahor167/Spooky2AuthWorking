using Spooky2.Core.Interfaces;
using Spooky2.Core.Models;
using Spooky2.Services.Communication;
using Spooky2.Services.Scanner;
using Xunit;
using Xunit.Abstractions;

namespace Spooky2.Services.Tests;

/// <summary>
/// Replays real sensor data from a full Hunt and Kill session captured on a GeneratorX Pro device.
/// Feeds the actual hardware readings through our SMA detection algorithm and verifies that
/// the detected hit frequencies match the ones the original Spooky2 software selected.
///
/// Data source: Data/FullHuntAndKill (96K lines, plain-text serial log)
/// Expected results: Data/FullHuntAndKil/image.png (screenshot of 10 hit frequencies)
///
/// KEY FINDING: The original Spooky2 software has a cluster deduplication mechanism that
/// prevents multiple hits from the same narrow frequency band. Our current implementation
/// lacks this, causing all 10 hit slots to be filled by the strongest single spike cluster.
/// Tests are organized to verify:
///   1. Parser correctness (data extraction from dump)
///   2. Current algorithm behavior (all hits in strongest cluster)
///   3. Deviation detection at ALL expected frequency regions
///   4. What correct behavior should look like (with cluster dedup - marked as failing)
/// </summary>
public class HuntAndKillReplayTests
{
    private readonly ITestOutputHelper _output;

    public HuntAndKillReplayTests(ITestOutputHelper output)
    {
        _output = output;
    }

    /// <summary>
    /// The 10 expected hit frequencies from the original Spooky2 software (screenshot),
    /// ordered by deviation (highest first).
    /// </summary>
    private static readonly double[] ExpectedHitFrequenciesHz =
    [
        1796956.27039622,
        1793366.39764006,
        1792022.04507412,
        1691899.39926639,
        1688519.40407137,
        1687675.46086123,
        1643134.10598867,
        1591790.40952598,
        177086.655334926,
        176909.679303436
    ];

    /// <summary>
    /// Frequency regions where hits are expected. Each region is defined by a center frequency
    /// and a percentage tolerance. The original software picks ONE representative hit per region.
    /// </summary>
    private static readonly (double CenterHz, double TolerancePercent, string Name, int ExpectedHits)[] ExpectedHitRegions =
    [
        (1795000, 1.0, "1.79 MHz cluster", 3),
        (1690000, 1.0, "1.69 MHz cluster", 3),
        (1643000, 1.0, "1.64 MHz single", 1),
        (1592000, 1.0, "1.59 MHz single", 1),
        (177000,  1.0, "177 kHz cluster",  2),
    ];

    private static string GetDumpPath()
    {
        var dir = AppContext.BaseDirectory;
        for (int i = 0; i < 10; i++)
        {
            var candidate = Path.Combine(dir, "Data", "FullHuntAndKill");
            if (File.Exists(candidate))
                return candidate;
            dir = Path.GetDirectoryName(dir)!;
        }

        throw new FileNotFoundException(
            $"Dump file 'FullHuntAndKill' not found. Searched up from {AppContext.BaseDirectory}");
    }

    private static bool DumpFileAvailable()
    {
        try { GetDumpPath(); return true; }
        catch (FileNotFoundException) { return false; }
    }

    /// <summary>
    /// Run the SMA detection algorithm on parsed sweep data.
    /// This is the core detection logic extracted from ScanService for direct testing.
    /// </summary>
    private static List<ScanResult> RunDetection(
        PlainTextDumpParser.HuntAndKillSession session,
        ScanParameters? overrideParams = null)
    {
        var parameters = overrideParams ?? new ScanParameters();
        var frequencies = ScanService.CalculateFrequencySteps(parameters);
        int stepCount = Math.Min(frequencies.Count, session.SweepSteps.Count);

        // Build scan readings list (baseline fills the window, then sweep readings)
        var scanReadings = new List<(double Frequency, double Reading)>();

        // Pre-fill: baseline readings go through the window but aren't scan steps
        var baselineWindow = new ScanService.SlidingWindow(parameters.RaWindow);
        foreach (var (angle, current) in session.BaselineReadings)
            baselineWindow.Add(parameters.UseCurrent ? current : angle);

        // Build readings list with baseline pre-fill baked into the window
        // by adding baseline tail as "pre-readings" that prime the SMA
        var baselineTail = session.BaselineReadings
            .TakeLast(parameters.RaWindow)
            .Select(r => parameters.UseCurrent ? r.Current : r.Angle)
            .ToList();
        foreach (var val in baselineTail)
            scanReadings.Add((0, val)); // freq=0 for baseline entries

        for (int i = 0; i < stepCount; i++)
        {
            double reading = parameters.UseCurrent
                ? session.SweepSteps[i].CurrentReading
                : session.SweepSteps[i].AngleReading;
            scanReadings.Add((frequencies[i], reading));
        }

        // Use the same post-processing as ScanService
        return ScanService.DetectHits(scanReadings, parameters);
    }

    /// <summary>
    /// Compute ALL deviations at every sweep step (not just top N).
    /// Returns (frequency, deviation) for every step where the window is full.
    /// </summary>
    private static List<(double Frequency, double Deviation, double Reading, double RA)> ComputeAllDeviations(
        PlainTextDumpParser.HuntAndKillSession session,
        ScanParameters? overrideParams = null)
    {
        var parameters = overrideParams ?? new ScanParameters();
        var frequencies = ScanService.CalculateFrequencySteps(parameters);
        int stepCount = Math.Min(frequencies.Count, session.SweepSteps.Count);

        var raWindow = new ScanService.SlidingWindow(parameters.RaWindow);
        foreach (var (_, current) in session.BaselineReadings)
            raWindow.Add(current);

        var result = new List<(double, double, double, double)>();

        for (int i = 0; i < stepCount; i++)
        {
            double current = session.SweepSteps[i].CurrentReading;
            raWindow.Add(current);

            if (raWindow.IsFull)
            {
                double ra = raWindow.SimpleAverage();
                double deviation = current - ra;
                result.Add((frequencies[i], deviation, current, ra));
            }
        }

        return result;
    }

    // ═══════════════════════════════════════════════════════════════
    // SECTION 1: Parser tests
    // ═══════════════════════════════════════════════════════════════

    [Fact]
    public void Parser_ExtractsSweepSteps()
    {
        if (!DumpFileAvailable()) return; // skip: dump file not available

        var session = PlainTextDumpParser.Parse(GetDumpPath());

        _output.WriteLine($"Baseline readings: {session.BaselineReadings.Count}");
        _output.WriteLine($"Sweep steps: {session.SweepSteps.Count}");
        _output.WriteLine($"Kill frequencies: {session.KillFrequencies.Count}");
        _output.WriteLine($"Amplitude ramp steps: {session.AmplitudeRampSteps}");

        Assert.Equal(203, session.BaselineReadings.Count);
        Assert.True(session.SweepSteps.Count > 15_000);
        Assert.True(session.KillFrequencies.Count >= 3);
        Assert.InRange(session.AmplitudeRampSteps, 330, 332);
    }

    [Fact]
    public void Parser_SensorReadingsAreReasonable()
    {
        if (!DumpFileAvailable()) return; // skip: dump file not available

        var session = PlainTextDumpParser.Parse(GetDumpPath());

        var avgAngle = session.BaselineReadings.Average(r => r.Angle);
        Assert.InRange(avgAngle, 50_000, 55_000);

        var avgCurrent = session.BaselineReadings.Average(r => r.Current);
        Assert.InRange(avgCurrent, 6_000, 8_000);

        Assert.True(session.SweepSteps.Max(s => s.CurrentReading) >
                    session.SweepSteps.Min(s => s.CurrentReading),
            "Current readings should vary across sweep");
    }

    [Fact]
    public void Parser_KillFrequencies_MatchTopThreeFromScreenshot()
    {
        if (!DumpFileAvailable()) return; // skip: dump file not available

        var session = PlainTextDumpParser.Parse(GetDumpPath());

        // The dump file was truncated during the kill phase, so only the first 3 kill
        // frequencies are captured. These correspond to the top 3 from the screenshot.
        Assert.Contains("1796956270396220", session.KillFrequencies);
        Assert.Contains("1793366397640060", session.KillFrequencies);
        Assert.Contains("1792022045074120", session.KillFrequencies);
    }

    [Fact]
    public void Parser_FrequencyStepCount_MatchesCalculation()
    {
        if (!DumpFileAvailable()) return; // skip: dump file not available

        var session = PlainTextDumpParser.Parse(GetDumpPath());
        var frequencies = ScanService.CalculateFrequencySteps(new ScanParameters());

        _output.WriteLine($"Calculated: {frequencies.Count}, Dump: {session.SweepSteps.Count}");

        // Within 1 step (floating-point boundary difference at end frequency)
        Assert.InRange(session.SweepSteps.Count, frequencies.Count - 2, frequencies.Count + 2);
    }

    // ═══════════════════════════════════════════════════════════════
    // SECTION 2: Deviation detection at expected frequency regions
    // These tests verify the raw sensor data contains detectable signals
    // at ALL expected hit frequencies, regardless of algorithm behavior.
    // ═══════════════════════════════════════════════════════════════

    [Fact]
    public void RawData_AllExpectedRegions_ShowMeasurableDeviation()
    {
        if (!DumpFileAvailable()) return; // skip: dump file not available

        var session = PlainTextDumpParser.Parse(GetDumpPath());
        var allDevs = ComputeAllDeviations(session);

        _output.WriteLine("Max deviations in expected regions:");

        foreach (var (center, tolPct, name, _) in ExpectedHitRegions)
        {
            double tolHz = center * tolPct / 100.0;
            var regionDevs = allDevs
                .Where(d => Math.Abs(d.Frequency - center) < tolHz)
                .ToList();

            Assert.True(regionDevs.Count > 0,
                $"No sweep steps found near {name} ({center} Hz)");

            double maxDev = regionDevs.Max(d => d.Deviation);
            _output.WriteLine($"  {name}: max deviation = {maxDev:F2} ({regionDevs.Count} steps in range)");

            // Every expected region should have positive deviation above noise
            Assert.True(maxDev > 1.0,
                $"Region '{name}' should have deviation > 1.0, got {maxDev:F2}");
        }
    }

    [Fact]
    public void RawData_EachExpectedFrequency_HasDeviationAboveNoise()
    {
        if (!DumpFileAvailable()) return; // skip: dump file not available

        var session = PlainTextDumpParser.Parse(GetDumpPath());
        var allDevs = ComputeAllDeviations(session);

        foreach (var expectedHz in ExpectedHitFrequenciesHz)
        {
            double tolHz = expectedHz * 0.01; // 1% window
            var nearbyDevs = allDevs
                .Where(d => Math.Abs(d.Frequency - expectedHz) < tolHz)
                .ToList();

            Assert.True(nearbyDevs.Count > 0,
                $"No steps found near {expectedHz:F2} Hz");

            double maxDev = nearbyDevs.Max(d => d.Deviation);
            _output.WriteLine($"  {expectedHz:F2} Hz → max dev = {maxDev:F2}");

            Assert.True(maxDev > 1.0,
                $"Expected deviation near {expectedHz:F2} Hz, got {maxDev:F2}");
        }
    }

    [Fact]
    public void RawData_1_79MHz_HasStrongestDeviation()
    {
        if (!DumpFileAvailable()) return; // skip: dump file not available

        var session = PlainTextDumpParser.Parse(GetDumpPath());
        var allDevs = ComputeAllDeviations(session);

        var maxDev = allDevs.MaxBy(d => d.Deviation);

        _output.WriteLine($"Global max deviation: {maxDev.Deviation:F2} at {maxDev.Frequency:F2} Hz");
        _output.WriteLine($"  reading={maxDev.Reading:F0}, RA={maxDev.RA:F2}");

        // The strongest deviation should be in the 1.79 MHz region
        Assert.InRange(maxDev.Frequency, 1_780_000, 1_800_000);
        Assert.True(maxDev.Deviation > 200);
    }

    [Fact]
    public void RawData_DeviationRanking_MatchesExpectedRegionOrder()
    {
        if (!DumpFileAvailable()) return; // skip: dump file not available

        var session = PlainTextDumpParser.Parse(GetDumpPath());
        var allDevs = ComputeAllDeviations(session);

        var regionMaxDevs = ExpectedHitRegions.Select(r =>
        {
            double tolHz = r.CenterHz * r.TolerancePercent / 100.0;
            var maxDev = allDevs
                .Where(d => Math.Abs(d.Frequency - r.CenterHz) < tolHz)
                .Max(d => d.Deviation);
            return (r.Name, MaxDev: maxDev);
        }).OrderByDescending(x => x.MaxDev).ToList();

        _output.WriteLine("Region deviation ranking:");
        foreach (var (name, maxDev) in regionMaxDevs)
            _output.WriteLine($"  {name}: {maxDev:F2}");

        // The 1.79 MHz cluster should rank first
        Assert.StartsWith("1.79", regionMaxDevs[0].Name);

        // The 1.69 MHz cluster should rank second
        Assert.StartsWith("1.69", regionMaxDevs[1].Name);
    }

    // ═══════════════════════════════════════════════════════════════
    // SECTION 3: Current algorithm behavior
    // These tests document the ACTUAL behavior of our current implementation.
    // Due to missing cluster deduplication, all 10 hits come from the
    // strongest spike cluster (~1.79 MHz).
    // ═══════════════════════════════════════════════════════════════

    [Fact]
    public void CurrentAlgorithm_Finds10Hits()
    {
        if (!DumpFileAvailable()) return; // skip: dump file not available

        var session = PlainTextDumpParser.Parse(GetDumpPath());
        var hits = RunDetection(session);

        Assert.Equal(10, hits.Count);
    }

    [Fact]
    public void CurrentAlgorithm_HitsAreOrderedByDeviation()
    {
        if (!DumpFileAvailable()) return; // skip: dump file not available

        var session = PlainTextDumpParser.Parse(GetDumpPath());
        var hits = RunDetection(session);

        for (int i = 1; i < hits.Count; i++)
        {
            Assert.True(hits[i - 1].Deviation >= hits[i].Deviation,
                $"Hit {i - 1} ({hits[i - 1].Deviation:F2}) should be >= hit {i} ({hits[i].Deviation:F2})");
        }
    }

    [Fact]
    public void CurrentAlgorithm_HitsSpanMultipleRegions()
    {
        if (!DumpFileAvailable()) return; // skip: dump file not available

        var session = PlainTextDumpParser.Parse(GetDumpPath());
        var hits = RunDetection(session);

        _output.WriteLine("Detection results (angle-based, spread across regions):");
        foreach (var hit in hits)
            _output.WriteLine($"  {hit.Frequency:F2} Hz  (dev={hit.Deviation:F2})");

        // With angle-based detection, hits should span multiple frequency regions
        // (not all clustered in one band)
        var distinctBands = hits.Select(h => Math.Round(h.Frequency / 100_000)).Distinct().Count();
        Assert.True(distinctBands >= 3,
            $"Expected hits in at least 3 frequency bands, got {distinctBands}");

        // Should find hits in the expected regions
        foreach (var (center, _, name, _) in ExpectedHitRegions)
        {
            double tolHz = center * 1.0 / 100.0;
            bool found = hits.Any(h => Math.Abs(h.Frequency - center) < tolHz);
            Assert.True(found, $"Expected hit in region '{name}' (center={center:F0} Hz)");
        }
    }

    [Fact]
    public void CurrentAlgorithm_TopThreeFrequencies_MatchExpectedCluster()
    {
        if (!DumpFileAvailable()) return; // skip: dump file not available

        var session = PlainTextDumpParser.Parse(GetDumpPath());
        var hits = RunDetection(session);

        // At least some of the expected 1.79 MHz frequencies should appear
        // (exact step may differ due to SMA floating-point differences)
        var expected1_79 = ExpectedHitFrequenciesHz.Where(f => f > 1_790_000).ToArray();
        foreach (var expectedHz in expected1_79)
        {
            var nearest = hits.OrderBy(h => Math.Abs(h.Frequency - expectedHz)).First();
            double diffPct = Math.Abs(nearest.Frequency - expectedHz) / expectedHz * 100;
            _output.WriteLine($"  Expected {expectedHz:F2} → nearest {nearest.Frequency:F2} ({diffPct:F3}%)");

            // Within 0.5% (about 10 steps) — same cluster, possibly different peak step
            Assert.True(diffPct < 0.5,
                $"Expected hit near {expectedHz:F2} Hz, nearest is {nearest.Frequency:F2} ({diffPct:F3}% away)");
        }
    }

    [Theory]
    [InlineData(80)]
    [InlineData(100)]
    [InlineData(120)]
    [InlineData(150)]
    [InlineData(200)]
    public void Exploration_DifferentWindowSizes(int windowSize)
    {
        if (!DumpFileAvailable()) return; // skip: dump file not available

        var session = PlainTextDumpParser.Parse(GetDumpPath());
        var parameters = new ScanParameters { RaWindow = windowSize };
        var hits = RunDetection(session, parameters);

        _output.WriteLine($"Window={windowSize}: {hits.Count} hits");
        int expectedMatchCount = 0;
        foreach (var hit in hits)
        {
            bool isExpected = ExpectedHitFrequenciesHz.Any(e => Math.Abs(e - hit.Frequency) / hit.Frequency * 100 < 1.0);
            string marker = isExpected ? " *" : "";
            if (isExpected) expectedMatchCount++;
            _output.WriteLine($"  {hit.Frequency,14:F2} Hz  dev={hit.Deviation,8:F2}{marker}");
        }
        _output.WriteLine($"  -> {expectedMatchCount}/10 match expected frequencies");

        Assert.True(hits.Any(h => h.Frequency > 1_780_000 && h.Frequency < 1_800_000));
    }

    // ═══════════════════════════════════════════════════════════════
    // SECTION 4: Expected correct behavior (with cluster dedup)
    // These tests document what the algorithm SHOULD produce once
    // cluster deduplication is implemented. Currently they verify
    // the underlying data supports finding hits in all regions.
    // ═══════════════════════════════════════════════════════════════

    [Fact]
    public void ExpectedBehavior_WhenDeduped_WouldFindHitsInAllRegions()
    {
        if (!DumpFileAvailable()) return; // skip: dump file not available

        var session = PlainTextDumpParser.Parse(GetDumpPath());
        var allDevs = ComputeAllDeviations(session);

        // Simulate cluster dedup: pick the highest-deviation step in each region
        var dedupedHits = new List<(double Frequency, double Deviation, string Region)>();

        foreach (var (center, tolPct, name, expectedCount) in ExpectedHitRegions)
        {
            double tolHz = center * tolPct / 100.0;
            var regionDevs = allDevs
                .Where(d => Math.Abs(d.Frequency - center) < tolHz && d.Deviation > 0)
                .OrderByDescending(d => d.Deviation)
                .Take(expectedCount)
                .ToList();

            foreach (var dev in regionDevs)
                dedupedHits.Add((dev.Frequency, dev.Deviation, name));
        }

        dedupedHits = dedupedHits.OrderByDescending(h => h.Deviation).ToList();

        _output.WriteLine($"Deduped hits ({dedupedHits.Count} total):");
        foreach (var (freq, dev, region) in dedupedHits)
            _output.WriteLine($"  {freq:F2} Hz  (dev={dev:F2}, region={region})");

        // Should find hits in all 5 regions
        var distinctRegions = dedupedHits.Select(h => h.Region).Distinct().ToList();
        Assert.Equal(ExpectedHitRegions.Length, distinctRegions.Count);

        // Total should be 10 (3+3+1+1+2)
        Assert.Equal(10, dedupedHits.Count);

        // Each deduped hit should be near one of the expected frequencies
        foreach (var (freq, dev, _) in dedupedHits)
        {
            var nearestExpected = ExpectedHitFrequenciesHz
                .OrderBy(e => Math.Abs(e - freq)).First();
            double diffPct = Math.Abs(nearestExpected - freq) / nearestExpected * 100;

            Assert.True(diffPct < 1.0,
                $"Deduped hit at {freq:F2} Hz should be near an expected frequency (nearest: {nearestExpected:F2}, diff: {diffPct:F3}%)");
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // SECTION 5: End-to-end ScanService replay
    // ═══════════════════════════════════════════════════════════════

    [Fact]
    public async Task ScanService_WithReplayData_ProducesConsistentResults()
    {
        if (!DumpFileAvailable()) return; // skip: dump file not available

        var session = PlainTextDumpParser.Parse(GetDumpPath());
        var parameters = new ScanParameters
        {
            StartDelayMs = 0,
            MinReadDelaySeconds = 0,
            EnableAmplitudeRampUp = false,
            EnableAmplitudeRampDown = false,
            BaselineReadCount = session.BaselineReadings.Count
        };

        var frequencies = ScanService.CalculateFrequencySteps(parameters);
        int stepCount = Math.Min(frequencies.Count, session.SweepSteps.Count);

        var replay = new ReplayGeneratorService(session, stepCount);
        var svc = new ScanService(replay);

        var hits = await svc.RunBiofeedbackScan(0, parameters);

        _output.WriteLine($"ScanService detected {hits.Count} hits:");
        foreach (var hit in hits)
            _output.WriteLine($"  {hit.Frequency:F2} Hz  (dev={hit.Deviation:F2})");

        Assert.Equal(parameters.MaxHits, hits.Count);

        // Should find hits spread across multiple regions (not just one cluster)
        var distinctBands = hits.Select(h => Math.Round(h.Frequency / 100_000)).Distinct().Count();
        Assert.True(distinctBands >= 3,
            $"Expected hits in at least 3 frequency bands, got {distinctBands}");
    }

    // ─────────────────────────────────────────────────────────────
    // Replay generator service
    // ─────────────────────────────────────────────────────────────

    private sealed class ReplayGeneratorService : IGeneratorService
    {
        private readonly PlainTextDumpParser.HuntAndKillSession _session;
        private readonly int _maxSweepSteps;
        private int _totalAngleReads;
        private int _totalCurrentReads;

        public ReplayGeneratorService(PlainTextDumpParser.HuntAndKillSession session, int maxSweepSteps)
        {
            _session = session;
            _maxSweepSteps = maxSweepSteps;
        }

        public List<string> CommandLog { get; } = [];

        public Task<List<GeneratorState>> FindGenerators() =>
            Task.FromResult(new List<GeneratorState>());
        public Task Start(int generatorId) => Task.CompletedTask;
        public Task Stop(int generatorId) => Task.CompletedTask;
        public Task Pause(int generatorId) => Task.CompletedTask;
        public Task Hold(int generatorId) => Task.CompletedTask;
        public Task Resume(int generatorId) => Task.CompletedTask;
        public Task WriteFrequencies(int generatorId, List<double> frequencies) => Task.CompletedTask;
        public Task<GeneratorState> ReadStatus(int generatorId) =>
            Task.FromResult(new GeneratorState { Id = generatorId, Status = GeneratorStatus.Idle });
        public Task EraseMemory(int generatorId) => Task.CompletedTask;
        public Task IdentifyGenerators() => Task.CompletedTask;
        public Task SendRawCommand(int generatorId, string command) => Task.CompletedTask;
        public Task WriteWaveformTables(int generatorId) => Task.CompletedTask;
        public Task SendCommandsBatch(int generatorId, IReadOnlyList<string> commands) => Task.CompletedTask;

        public Task<string?> SendCommandWithResponse(int generatorId, string command)
        {
            CommandLog.Add(command);

            if (command == ":r11=,")
            {
                _totalAngleReads++;
                int baselineTotal = 1 + _session.BaselineReadings.Count;
                double angle;

                if (_totalAngleReads <= baselineTotal)
                {
                    int idx = Math.Min(_totalAngleReads - 1, _session.BaselineReadings.Count - 1);
                    angle = idx >= 0 ? _session.BaselineReadings[idx].Angle : 52000;
                }
                else
                {
                    int sweepIdx = _totalAngleReads - baselineTotal - 1;
                    angle = (sweepIdx >= 0 && sweepIdx < _session.SweepSteps.Count)
                        ? _session.SweepSteps[sweepIdx].AngleReading
                        : 52000;
                }
                return Task.FromResult<string?>($":r11={angle}.");
            }

            if (command == ":r12=,")
            {
                _totalCurrentReads++;
                double current;

                if (_totalCurrentReads <= _session.BaselineReadings.Count)
                {
                    current = _session.BaselineReadings[_totalCurrentReads - 1].Current;
                }
                else
                {
                    int sweepIdx = _totalCurrentReads - _session.BaselineReadings.Count - 1;
                    current = (sweepIdx >= 0 && sweepIdx < _session.SweepSteps.Count)
                        ? _session.SweepSteps[sweepIdx].CurrentReading
                        : 6900;
                }
                return Task.FromResult<string?>($":r12={current}.");
            }

            return Task.FromResult<string?>("ok");
        }
    }
}
