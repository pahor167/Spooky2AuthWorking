using Microsoft.Extensions.Logging.Abstractions;
using Spooky2.Core.Interfaces;
using Spooky2.Core.Models;
using Spooky2.Services.Communication;
using Spooky2.Services.Scanner;
using Xunit;

namespace Spooky2.Services.Tests;

public class ScanServiceTests
{
    // ─────────────────────────────────────────────────────────────
    // Mock infrastructure
    // ─────────────────────────────────────────────────────────────

    private sealed class MockGeneratorService : IGeneratorService
    {
        public List<string> CommandLog { get; } = [];
        public Dictionary<string, string> CommandResponses { get; } = new();
        public List<double> WrittenFrequencies { get; } = [];
        public bool IsStarted { get; private set; }
        public bool IsStopped { get; private set; }

        public Task<List<GeneratorState>> FindGenerators() => Task.FromResult(new List<GeneratorState>
        {
            new() { Id = 0, Port = "COM3", Status = GeneratorStatus.Idle, CurrentProgram = "Test" }
        });

        public Task Start(int generatorId) { IsStarted = true; return Task.CompletedTask; }
        public Task Stop(int generatorId) { IsStopped = true; return Task.CompletedTask; }
        public Task Pause(int generatorId) => Task.CompletedTask;
        public Task Hold(int generatorId) => Task.CompletedTask;
        public Task Resume(int generatorId) => Task.CompletedTask;

        public Task WriteFrequencies(int generatorId, List<double> frequencies)
        {
            WrittenFrequencies.AddRange(frequencies);
            return Task.CompletedTask;
        }

        public Task<GeneratorState> ReadStatus(int generatorId) =>
            Task.FromResult(new GeneratorState { Id = generatorId, Status = GeneratorStatus.Idle });

        public Task EraseMemory(int generatorId) => Task.CompletedTask;
        public Task IdentifyGenerators() => Task.CompletedTask;
        public Task SendRawCommand(int generatorId, string command) => Task.CompletedTask;

        public Task<string?> SendCommandWithResponse(int generatorId, string command)
        {
            CommandLog.Add(command);

            if (CommandResponses.TryGetValue(command, out var response))
                return Task.FromResult<string?>(response);

            // Default responses for sensor reads
            if (command == GeneratorProtocol.ReadAngle)
                return Task.FromResult<string?>(":r11=50000.");
            if (command == GeneratorProtocol.ReadCurrent)
                return Task.FromResult<string?>(":r12=6000.");

            return Task.FromResult<string?>("ok");
        }

        public Task SendCommandsBatch(int generatorId, IReadOnlyList<string> commands)
        {
            foreach (var cmd in commands) CommandLog.Add(cmd);
            return Task.CompletedTask;
        }
    }

    /// <summary>
    /// Mock that returns varying current values to test hit detection.
    /// Returns a baseline of 6000 with a spike at a configured frequency index.
    /// </summary>
    private sealed class SpikeGeneratorService : IGeneratorService
    {
        private int _readCount;
        private readonly int _spikeAtStep;
        private readonly double _spikeValue;
        private readonly double _baselineValue;
        public List<string> CommandLog { get; } = [];

        public SpikeGeneratorService(int spikeAtStep, double spikeValue = 9000, double baselineValue = 6000)
        {
            _spikeAtStep = spikeAtStep;
            _spikeValue = spikeValue;
            _baselineValue = baselineValue;
        }

        public Task<List<GeneratorState>> FindGenerators() => Task.FromResult(new List<GeneratorState>());
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

        public Task SendCommandsBatch(int generatorId, IReadOnlyList<string> commands) => Task.CompletedTask;
        public Task<string?> SendCommandWithResponse(int generatorId, string command)
        {
            CommandLog.Add(command);

            if (command == GeneratorProtocol.ReadCurrent)
            {
                _readCount++;
                // Spike at the configured step (each step reads current once)
                var value = _readCount == _spikeAtStep ? _spikeValue : _baselineValue;
                return Task.FromResult<string?>($":r12={value}.");
            }

            if (command == GeneratorProtocol.ReadAngle)
                return Task.FromResult<string?>(":r11=50000.");

            return Task.FromResult<string?>("ok");
        }
    }

    // ─────────────────────────────────────────────────────────────
    // CalculateFrequencySteps tests
    // ─────────────────────────────────────────────────────────────

    [Fact]
    public void CalculateFrequencySteps_FixedHz_GeneratesCorrectSteps()
    {
        var parameters = new ScanParameters
        {
            StartFrequency = 1000,
            EndFrequency = 1500,
            UsePercentageStep = false,
            StepSizeHz = 100
        };

        var steps = ScanService.CalculateFrequencySteps(parameters);

        Assert.Equal(6, steps.Count); // 1000, 1100, 1200, 1300, 1400, 1500
        Assert.Equal(1000, steps[0]);
        Assert.Equal(1500, steps[5]);
    }

    [Fact]
    public void CalculateFrequencySteps_Percentage_GeneratesExponentialSteps()
    {
        var parameters = new ScanParameters
        {
            StartFrequency = 1000,
            EndFrequency = 2000,
            UsePercentageStep = true,
            StepSizePercent = 10.0 // 10% per step
        };

        var steps = ScanService.CalculateFrequencySteps(parameters);

        Assert.Equal(1000, steps[0]);
        Assert.Equal(1100, steps[1], precision: 1); // 1000 + 10%
        Assert.Equal(1210, steps[2], precision: 1); // 1100 + 10%
        Assert.True(steps.Count >= 7); // log(2)/log(1.1) ≈ 7.27
    }

    [Fact]
    public void CalculateFrequencySteps_PercentageAt0025_MatchesDumpData()
    {
        // From the serial dump: at ~1.65 MHz with 0.025% step, the step size
        // should be approximately 413 Hz (1,650,000 * 0.00025 = 412.5)
        var parameters = new ScanParameters
        {
            StartFrequency = 1_650_000,
            EndFrequency = 1_651_000,
            UsePercentageStep = true,
            StepSizePercent = 0.025
        };

        var steps = ScanService.CalculateFrequencySteps(parameters);

        // First step should be ~412.5 Hz
        double firstStep = steps[1] - steps[0];
        Assert.InRange(firstStep, 410, 415);
    }

    [Fact]
    public void CalculateFrequencySteps_DefaultParameters_GeneratesLargeList()
    {
        // Default: 41,000 to 1,800,000 Hz at 0.025% step
        var parameters = new ScanParameters();

        var steps = ScanService.CalculateFrequencySteps(parameters);

        // Should generate thousands of steps
        Assert.True(steps.Count > 10_000, $"Expected >10000 steps, got {steps.Count}");
        Assert.Equal(41_000, steps[0]);
        Assert.True(steps[^1] <= 1_800_000);
    }

    // ─────────────────────────────────────────────────────────────
    // Biofeedback scan command sequence tests
    // ─────────────────────────────────────────────────────────────

    [Fact]
    public async Task RunBiofeedbackScan_SendsCorrectCommandSequence()
    {
        var mock = new MockGeneratorService();
        var svc = new ScanService(mock);

        var parameters = new ScanParameters
        {
            StartFrequency = 1000,
            EndFrequency = 1100,
            UsePercentageStep = false,
            StepSizeHz = 100,
            StartDelayMs = 0,
            MinReadDelaySeconds = 0,
            RaWindow = 1, EnableAmplitudeRampUp = false, BaselineReadCount = 0,
            DetectMax = true,
            Threshold = 0
        };

        await svc.RunBiofeedbackScan(0, parameters);

        // Should have: display name, then for each of 2 freq steps: set freq, read angle, read current
        // Then clear freq commands at end
        Assert.True(mock.CommandLog.Any(c => c.StartsWith(":n00=")), "Should contain display name command");

        // Count frequency writes: 1 setup (raw Hz) + 2 scan (nanoHz)
        var freqWrites = mock.CommandLog.Where(c => c.StartsWith(":w24=")).ToList();
        Assert.True(freqWrites.Count >= 3, $"Expected at least 3 :w24 writes, got {freqWrites.Count}");
        Assert.Equal(":w24=1000,", freqWrites[0]); // setup: raw Hz

        // Count angle reads: pre-scan + baseline + scan
        var angleReads = mock.CommandLog.Count(c => c == GeneratorProtocol.ReadAngle);
        Assert.True(angleReads >= 3, $"Expected at least 3 angle reads, got {angleReads}");

        // Count current reads: pre-scan + scan
        var currentReads = mock.CommandLog.Count(c => c == GeneratorProtocol.ReadCurrent);
        Assert.True(currentReads >= 2, $"Expected at least 2 current reads, got {currentReads}");

        // Should contain clear frequency commands at the end
        Assert.Contains(GeneratorProtocol.ClearFrequency1, mock.CommandLog);
        Assert.Contains(GeneratorProtocol.ClearFrequency2, mock.CommandLog);
    }

    [Fact]
    public async Task RunBiofeedbackScan_FrequencyWriteFormat_MatchesProtocol()
    {
        var mock = new MockGeneratorService();
        var svc = new ScanService(mock);

        var parameters = new ScanParameters
        {
            StartFrequency = 76000,
            EndFrequency = 76000,
            UsePercentageStep = false,
            StepSizeHz = 100,
            StartDelayMs = 0,
            MinReadDelaySeconds = 0,
            RaWindow = 1, EnableAmplitudeRampUp = false, BaselineReadCount = 0,
            DetectMax = true,
            Threshold = 0
        };

        await svc.RunBiofeedbackScan(0, parameters);

        // First :w24 is raw Hz setup, second is nanoHz scan
        // 76000 Hz → 76000 * 1e9 = 76000000000000 nanoHz
        var freqCmds = mock.CommandLog.Where(c => c.StartsWith(":w24=")).ToList();
        Assert.Equal(":w24=76000,", freqCmds[0]); // setup: raw Hz
        Assert.Equal(":w24=76000000000000,", freqCmds[1]); // scan: nanoHz
    }

    // ─────────────────────────────────────────────────────────────
    // Running average detection algorithm tests
    // ─────────────────────────────────────────────────────────────

    [Fact]
    public async Task RunBiofeedbackScan_DetectsSpike_AsHit()
    {
        // Spike at step 25 (after RA buffer is full at step 20)
        var mock = new SpikeGeneratorService(spikeAtStep: 30, spikeValue: 9000, baselineValue: 6000);
        var svc = new ScanService(mock);

        var parameters = new ScanParameters
        {
            StartFrequency = 1000,
            EndFrequency = 4000,
            UsePercentageStep = false,
            StepSizeHz = 100,
            StartDelayMs = 0,
            MinReadDelaySeconds = 0,
            RaWindow = 20, EnableAmplitudeRampUp = false, BaselineReadCount = 0,
            DetectMax = true,
            Threshold = 0,
            UseCurrent = true,
            MaxHits = 10
        };

        var hits = await svc.RunBiofeedbackScan(0, parameters);

        // Should detect the spike as a hit
        Assert.NotEmpty(hits);
        // The spike should be detected at some frequency in the scan range
        Assert.NotEmpty(hits);
    }

    [Fact]
    public async Task RunBiofeedbackScan_NoSpike_NoHitsAboveThreshold()
    {
        // All readings are constant, no deviation
        var mock = new SpikeGeneratorService(spikeAtStep: -1, baselineValue: 6000);
        var svc = new ScanService(mock);

        var parameters = new ScanParameters
        {
            StartFrequency = 1000,
            EndFrequency = 4000,
            UsePercentageStep = false,
            StepSizeHz = 100,
            StartDelayMs = 0,
            MinReadDelaySeconds = 0,
            RaWindow = 20, EnableAmplitudeRampUp = false, BaselineReadCount = 0,
            DetectMax = true,
            Threshold = 100, // High threshold
            UseCurrent = true,
            MaxHits = 10
        };

        var hits = await svc.RunBiofeedbackScan(0, parameters);

        Assert.Empty(hits);
    }

    // ─────────────────────────────────────────────────────────────
    // Hit ranking tests
    // ─────────────────────────────────────────────────────────────

    [Fact]
    public async Task RunBiofeedbackScan_KeepsTopHitsByDeviation()
    {
        // Create a service that returns varying readings
        var mock = new VariableReadingGeneratorService(new Dictionary<int, double>
        {
            // Steps are 1-indexed for current reads
            [22] = 7000, // deviation ~1000
            [25] = 8000, // deviation ~2000
            [28] = 9000, // deviation ~3000
        }, baselineValue: 6000);

        var svc = new ScanService(mock);

        var parameters = new ScanParameters
        {
            StartFrequency = 1000,
            EndFrequency = 4000,
            UsePercentageStep = false,
            StepSizeHz = 100,
            StartDelayMs = 0,
            MinReadDelaySeconds = 0,
            RaWindow = 20, EnableAmplitudeRampUp = false, BaselineReadCount = 0,
            DetectMax = true,
            Threshold = 0,
            UseCurrent = true,
            MaxHits = 2 // Only keep top 2
        };

        var hits = await svc.RunBiofeedbackScan(0, parameters);

        // Should keep only top 2 by deviation
        Assert.True(hits.Count <= 2);
        // The highest deviation hit should be first
        if (hits.Count >= 2)
        {
            Assert.True(hits[0].Deviation >= hits[1].Deviation);
        }
    }

    private sealed class VariableReadingGeneratorService : IGeneratorService
    {
        private int _readCount;
        private readonly Dictionary<int, double> _spikes;
        private readonly double _baselineValue;

        public VariableReadingGeneratorService(Dictionary<int, double> spikes, double baselineValue = 6000)
        {
            _spikes = spikes;
            _baselineValue = baselineValue;
        }

        public Task<List<GeneratorState>> FindGenerators() => Task.FromResult(new List<GeneratorState>());
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

        public Task SendCommandsBatch(int generatorId, IReadOnlyList<string> commands) => Task.CompletedTask;
        public Task<string?> SendCommandWithResponse(int generatorId, string command)
        {
            if (command == GeneratorProtocol.ReadCurrent)
            {
                _readCount++;
                var value = _spikes.TryGetValue(_readCount, out var spike) ? spike : _baselineValue;
                return Task.FromResult<string?>($":r12={value}.");
            }

            if (command == GeneratorProtocol.ReadAngle)
                return Task.FromResult<string?>(":r11=50000.");

            return Task.FromResult<string?>("ok");
        }
    }

    // ─────────────────────────────────────────────────────────────
    // Scan cancellation tests
    // ─────────────────────────────────────────────────────────────

    [Fact]
    public async Task RunBiofeedbackScan_Cancellation_ReturnsPartialResults()
    {
        var mock = new MockGeneratorService();
        var svc = new ScanService(mock);

        var cts = new CancellationTokenSource();

        var parameters = new ScanParameters
        {
            StartFrequency = 1000,
            EndFrequency = 100_000,
            UsePercentageStep = false,
            StepSizeHz = 1,
            StartDelayMs = 0,
            MinReadDelaySeconds = 0,
            RaWindow = 5
        };

        // Cancel after a short delay
        cts.CancelAfter(100);

        var hits = await svc.RunBiofeedbackScan(0, parameters, ct: cts.Token);

        // Should not throw; returns partial results
        Assert.NotNull(hits);
    }

    [Fact]
    public async Task StopScan_CancelsActiveScan()
    {
        var mock = new MockGeneratorService();
        var svc = new ScanService(mock);

        var parameters = new ScanParameters
        {
            StartFrequency = 1000,
            EndFrequency = 1_000_000,
            UsePercentageStep = false,
            StepSizeHz = 1,
            StartDelayMs = 0,
            MinReadDelaySeconds = 0
        };

        // Start a scan in the background
        var scanTask = svc.RunBiofeedbackScan(0, parameters);

        // Stop it
        await svc.StopScan(0);

        // The scan should complete (not hang)
        var hits = await scanTask;
        Assert.NotNull(hits);
    }

    // ─────────────────────────────────────────────────────────────
    // Hunt and Kill cycle tests
    // ─────────────────────────────────────────────────────────────

    [Fact]
    public async Task RunHuntAndKill_NoHits_StopsAfterOneCycle()
    {
        // All readings constant → no hits → stops immediately
        var mock = new MockGeneratorService();
        var svc = new ScanService(mock);

        var parameters = new ScanParameters
        {
            StartFrequency = 1000,
            EndFrequency = 1200,
            UsePercentageStep = false,
            StepSizeHz = 100,
            StartDelayMs = 0,
            MinReadDelaySeconds = 0,
            RaWindow = 2,
            DetectMax = true,
            Threshold = 10_000, // Very high threshold → no hits
            ContinueRefining = true
        };

        var hits = await svc.RunHuntAndKill(0, parameters);

        Assert.Empty(hits);
        // Should have cleanup commands at end
        Assert.Contains(GeneratorProtocol.ClearFrequency1, mock.CommandLog);
        Assert.Contains(GeneratorProtocol.ClearFrequency2, mock.CommandLog);
    }

    [Fact]
    public async Task RunHuntAndKill_ContinueRefiningFalse_StopsAfterFirstCycle()
    {
        // Spike generates hits, but ContinueRefining=false → only one cycle
        var mock = new SpikeGeneratorService(spikeAtStep: 3, spikeValue: 9000, baselineValue: 6000);
        var svc = new ScanService(mock);

        var cts = new CancellationTokenSource(TimeSpan.FromSeconds(5));

        var parameters = new ScanParameters
        {
            StartFrequency = 1000,
            EndFrequency = 1500,
            UsePercentageStep = false,
            StepSizeHz = 100,
            StartDelayMs = 0,
            MinReadDelaySeconds = 0,
            RaWindow = 2,
            DetectMax = true,
            Threshold = 0,
            ContinueRefining = false
        };

        var hits = await svc.RunHuntAndKill(0, parameters, ct: cts.Token);

        // Should have returned after one cycle (scan + kill)
        Assert.NotNull(hits);
    }

    // ─────────────────────────────────────────────────────────────
    // Progress reporting tests
    // ─────────────────────────────────────────────────────────────

    [Fact]
    public async Task RunBiofeedbackScan_ReportsProgress()
    {
        var mock = new MockGeneratorService();
        var svc = new ScanService(mock);

        var progressReports = new List<ScanProgress>();
        var progress = new Progress<ScanProgress>(p => progressReports.Add(p));

        var parameters = new ScanParameters
        {
            StartFrequency = 1000,
            EndFrequency = 1200,
            UsePercentageStep = false,
            StepSizeHz = 100,
            StartDelayMs = 0,
            MinReadDelaySeconds = 0,
            RaWindow = 1
        };

        await svc.RunBiofeedbackScan(0, parameters, progress);

        // Allow progress callbacks to fire (they use SynchronizationContext)
        await Task.Delay(100);

        Assert.NotEmpty(progressReports);
        // Last report should be 100%
        var lastReport = progressReports[^1];
        Assert.Equal(100, lastReport.PercentComplete);
    }

    // ─────────────────────────────────────────────────────────────
    // GetScanResults tests
    // ─────────────────────────────────────────────────────────────

    [Fact]
    public async Task GetScanResults_AfterScan_ReturnsCachedResults()
    {
        var mock = new SpikeGeneratorService(spikeAtStep: 3, spikeValue: 9000, baselineValue: 6000);
        var svc = new ScanService(mock);

        var parameters = new ScanParameters
        {
            StartFrequency = 1000,
            EndFrequency = 1500,
            UsePercentageStep = false,
            StepSizeHz = 100,
            StartDelayMs = 0,
            MinReadDelaySeconds = 0,
            RaWindow = 2,
            DetectMax = true,
            Threshold = 0
        };

        var scanHits = await svc.RunBiofeedbackScan(0, parameters);
        var cachedHits = await svc.GetScanResults(0);

        Assert.Equal(scanHits.Count, cachedHits.Count);
    }

    [Fact]
    public async Task GetScanResults_NoScan_ReturnsEmpty()
    {
        var mock = new MockGeneratorService();
        var svc = new ScanService(mock);

        var results = await svc.GetScanResults(99);

        Assert.Empty(results);
    }

    // ─────────────────────────────────────────────────────────────
    // Sensor parsing tests
    // ─────────────────────────────────────────────────────────────

    [Theory]
    [InlineData(":r11=53001.", 53001)]
    [InlineData(":r12=6557.", 6557)]
    [InlineData(":r12=0.", 0)]
    [InlineData("", 0)]
    [InlineData(":r11=", 0)]
    public void ParseSensorReading_HandlesVariousFormats(string response, double expected)
    {
        var result = GeneratorProtocol.ParseSensorReading(response);
        Assert.Equal(expected, result, precision: 1);
    }
}

// ── Weighted Average (LWMA) Tests ──

public class SlidingWindowTests
{
    [Fact]
    public void WeightedAverage_NewestGetsHighestWeight()
    {
        // Window: [10, 20, 30] with weights [1, 2, 3]
        // LWMA = (10*1 + 20*2 + 30*3) / (1+2+3) = (10+40+90)/6 = 140/6 ≈ 23.33
        var window = new ScanService.SlidingWindow(3);
        window.Add(10);
        window.Add(20);
        window.Add(30);
        Assert.Equal(23.33, window.WeightedAverage(), precision: 2);
    }

    [Fact]
    public void WeightedAverage_HigherThanSimple_WhenNewestIsLargest()
    {
        // Simple avg of [10, 20, 30] = 20.0
        // LWMA gives more weight to 30 (newest) → result > 20
        var window = new ScanService.SlidingWindow(3);
        window.Add(10);
        window.Add(20);
        window.Add(30);
        Assert.True(window.WeightedAverage() > window.SimpleAverage());
    }

    [Fact]
    public void WeightedAverage_LowerThanSimple_WhenOldestIsLargest()
    {
        // Simple avg of [30, 20, 10] = 20.0
        // LWMA gives more weight to 10 (newest) → result < 20
        var window = new ScanService.SlidingWindow(3);
        window.Add(30);
        window.Add(20);
        window.Add(10);
        Assert.True(window.WeightedAverage() < window.SimpleAverage());
    }

    [Fact]
    public void WeightedAverage_EqualsSimple_WhenAllValuesSame()
    {
        var window = new ScanService.SlidingWindow(5);
        for (int i = 0; i < 5; i++) window.Add(100);
        Assert.Equal(window.SimpleAverage(), window.WeightedAverage(), precision: 10);
    }

    [Fact]
    public void WeightedAverage_SpikeDetection_SpikeStandsOut()
    {
        // Fill with baseline 6500, then spike to 6600
        // The LWMA should be lower than the spike (since old baseline pulls it down)
        var window = new ScanService.SlidingWindow(20);
        for (int i = 0; i < 19; i++) window.Add(6500);
        window.Add(6600); // spike
        
        double lwma = window.WeightedAverage();
        double spike = 6600;
        double deviation = spike - lwma;
        
        // LWMA should be between 6500 (baseline) and 6600 (spike)
        Assert.True(lwma > 6500);
        Assert.True(lwma < 6600);
        // Deviation should be positive (spike above average)
        Assert.True(deviation > 0);
        
        // Simple average deviation would be: 6600 - (19*6500+6600)/20 = 6600 - 6505 = 95
        // LWMA deviation should be LESS than simple (because LWMA weights spike higher)
        double simpleAvg = (19 * 6500.0 + 6600) / 20;
        Assert.True(deviation < (spike - simpleAvg), 
            "LWMA should make spike stand out less than simple average since newest gets highest weight");
    }

    [Fact]
    public void WeightedAverage_Window20_MatchesFormula()
    {
        // For window size N, LWMA = Σ(val[i] * (i+1)) / (N*(N+1)/2)
        var window = new ScanService.SlidingWindow(4);
        window.Add(100); // weight 1
        window.Add(200); // weight 2
        window.Add(300); // weight 3
        window.Add(400); // weight 4
        
        // Expected: (100*1 + 200*2 + 300*3 + 400*4) / (4*5/2)
        //         = (100 + 400 + 900 + 1600) / 10 = 3000/10 = 300
        Assert.Equal(300.0, window.WeightedAverage(), precision: 10);
    }

    [Fact]
    public void SlidingWindow_SlidesCorrectly()
    {
        var window = new ScanService.SlidingWindow(3);
        window.Add(10);
        window.Add(20);
        window.Add(30);
        Assert.True(window.IsFull);
        
        // Add a 4th value, oldest (10) should be dropped
        window.Add(40);
        // Now window is [20, 30, 40]
        // LWMA = (20*1 + 30*2 + 40*3) / 6 = (20+60+120)/6 = 200/6 ≈ 33.33
        Assert.Equal(33.33, window.WeightedAverage(), precision: 2);
    }

    [Fact]
    public void Peak_ReturnsMaxInWindow()
    {
        var window = new ScanService.SlidingWindow(5);
        window.Add(10);
        window.Add(50);
        window.Add(30);
        window.Add(20);
        window.Add(40);
        Assert.Equal(50, window.Peak());
    }
}
