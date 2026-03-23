using System.Collections.Concurrent;
using Microsoft.Extensions.Logging;
using Microsoft.Extensions.Logging.Abstractions;
using Spooky2.Core.Interfaces;
using Spooky2.Core.Models;
using Spooky2.Services.Communication;

namespace Spooky2.Services.Scanner;

/// <summary>
/// Biofeedback scan engine matching the exact Spooky2 serial protocol.
/// Verified from Data/StartHuntAndKillLonger serial dump (2704 TX commands).
///
/// Protocol phases:
///   Phase 1 - Setup + Amplitude Ramp-Up:
///     :n00=Port N - Running Biofeedback
///     :w24=41009,           (start freq in raw Hz)
///     :w28=6, :w29=6,       (amplitude near zero)
///     :w11=1,, :w11=,1,     (enable both outputs)
///     :w28=12,:w29=12, ... :w28=2000,:w29=2000,  (ramp 0.06V→20V in 330 steps)
///
///   Phase 2 - Baseline reads (fill RA buffer):
///     42× {:r11=, :r12=,}   (84 sensor reads at start frequency)
///
///   Phase 3 - Frequency sweep:
///     :w24=nanoHz, :r11=, :r12=,  (per step, 0.025% increments)
///
/// Detection: Linearly Weighted Moving Average (LWMA) with dual windows.
/// </summary>
public sealed class ScanService : IScanService
{
    private readonly IGeneratorService _generatorService;
    private readonly ILogger<ScanService> _logger;
    private readonly ConcurrentDictionary<int, CancellationTokenSource> _activeScans = new();
    private readonly ConcurrentDictionary<int, List<ScanResult>> _scanResults = new();

    public ScanService(IGeneratorService generatorService, ILogger<ScanService>? logger = null)
    {
        _generatorService = generatorService;
        _logger = logger ?? NullLogger<ScanService>.Instance;
    }

    public async Task<List<ScanResult>> RunBiofeedbackScan(
        int generatorId, ScanParameters parameters,
        IProgress<ScanProgress>? progress = null, CancellationToken ct = default)
    {
        _logger.LogInformation("Starting biofeedback scan on generator {Id}: {Start}-{End} Hz",
            generatorId, parameters.StartFrequency, parameters.EndFrequency);

        var cts = CancellationTokenSource.CreateLinkedTokenSource(ct);
        _activeScans[generatorId] = cts;

        try
        {
            // ══════════════════════════════════════════════════════════
            // PHASE 1: Setup + Amplitude Ramp-Up
            // Matches serial dump: :n00 → :w24=Hz → :w28/:w29=6 →
            //   :w11=1,, → :w11=,1, → ramp 6→2000 in ~330 steps
            // ══════════════════════════════════════════════════════════
            progress?.Report(new ScanProgress { StatusText = "Setting up generator..." });

            // Display name
            var displayName = string.IsNullOrEmpty(parameters.LogName)
                ? "Running Biofeedback"
                : parameters.LogName;
            await Send(generatorId, GeneratorProtocol.BuildSetDisplayName(displayName));

            // Set start frequency (raw Hz, NOT nanoHz — matches dump)
            await Send(generatorId, GeneratorProtocol.BuildSetFrequencyRawHz((int)parameters.StartFrequency));

            if (parameters.EnableAmplitudeRampUp)
            {
                int targetCv = parameters.TargetAmplitudeCv;
                int rampDivisor = parameters.RampSteps;

                // First step + output enable (these need responses)
                int firstCv = (int)Math.Round((double)targetCv / rampDivisor);
                await Send(generatorId, GeneratorProtocol.BuildSetAmplitudeCv1(firstCv));
                await Send(generatorId, GeneratorProtocol.BuildSetAmplitudeCv2(firstCv));
                await Send(generatorId, GeneratorProtocol.EnableOutput1);
                await Send(generatorId, GeneratorProtocol.EnableOutput2);

                progress?.Report(new ScanProgress { StatusText = "Ramping amplitude up..." });

                // Build ramp commands in batch — fire-and-forget for speed
                // Original Spooky2 does 660 commands in ~2 seconds (no response wait)
                var rampCmds = new List<string>();
                for (int i = 1; i <= rampDivisor; i++)
                {
                    int cv = Math.Min((int)Math.Round((double)(i + 1) * targetCv / rampDivisor), targetCv);
                    rampCmds.Add(GeneratorProtocol.BuildSetAmplitudeCv1(cv));
                    rampCmds.Add(GeneratorProtocol.BuildSetAmplitudeCv2(cv));
                }

                await _generatorService.SendCommandsBatch(generatorId, rampCmds);

                _logger.LogInformation("Amplitude ramp-up: {Steps} steps → {Target} cV ({TargetV}V)",
                    rampDivisor, targetCv, targetCv / 100.0);
            }
            else
            {
                // No ramp — set full amplitude and enable outputs
                await Send(generatorId, GeneratorProtocol.BuildSetAmplitudeCv1(parameters.TargetAmplitudeCv));
                await Send(generatorId, GeneratorProtocol.BuildSetAmplitudeCv2(parameters.TargetAmplitudeCv));
                await Send(generatorId, GeneratorProtocol.EnableOutput1);
                await Send(generatorId, GeneratorProtocol.EnableOutput2);
            }

            // Initial delay after ramp
            if (parameters.StartDelayMs > 0)
                await Task.Delay(parameters.StartDelayMs, cts.Token);

            // ══════════════════════════════════════════════════════════
            // PHASE 2: Baseline sensor reads (fill RA buffer)
            // Matches dump: 84 reads (42 pairs) at start frequency
            // before any frequency changes. This gives the RA a stable
            // baseline so the first scan steps don't produce false hits.
            // ══════════════════════════════════════════════════════════
            var raWindow1 = new SlidingWindow(parameters.RaWindow);
            var raWindow2 = new SlidingWindow(parameters.RaWindow2 > 0 ? parameters.RaWindow2 : parameters.RaWindow);
            var angleWindow1 = new SlidingWindow(parameters.RaWindow);
            var angleWindow2 = new SlidingWindow(parameters.RaWindow2 > 0 ? parameters.RaWindow2 : parameters.RaWindow);

            progress?.Report(new ScanProgress { StatusText = "Reading baseline..." });

            // Baseline pattern from dump: 1 initial :r11 (standalone), then N × (:r11, :r12) pairs
            // Dump shows 204 :r11 + 203 :r12 = 1 + 203 pairs
            _logger.LogDebug("Taking baseline reads at {Freq} Hz: 1 initial + {Count} pairs",
                parameters.StartFrequency, parameters.BaselineReadCount);

            // Initial standalone angle read
            {
                var initAngle = await Send(generatorId, GeneratorProtocol.ReadAngle);
                double a = GeneratorProtocol.ParseSensorReading(initAngle ?? "");
                angleWindow1.Add(a);
                angleWindow2.Add(a);
            }

            // Then paired :r11, :r12 reads
            for (int b = 0; b < parameters.BaselineReadCount; b++)
            {
                cts.Token.ThrowIfCancellationRequested();
                var (angle, current) = await ReadSensors(generatorId, parameters.SamplesPerStep);

                raWindow1.Add(current);
                raWindow2.Add(current);
                angleWindow1.Add(angle);
                angleWindow2.Add(angle);
            }

            _logger.LogInformation("Baseline complete: RA buffer filled with {Count} samples", parameters.BaselineReadCount);

            // ══════════════════════════════════════════════════════════
            // PHASE 3: Frequency sweep
            // Matches dump: :w24=nanoHz → :r11 → :r12, 0.025% steps
            // ══════════════════════════════════════════════════════════
            var frequencies = CalculateFrequencySteps(parameters);
            _logger.LogInformation("Sweep: {Count} frequency steps from {Start} to {End} Hz",
                frequencies.Count, parameters.StartFrequency, parameters.EndFrequency);

            double peakReading = double.MinValue;
            double peakFrequency = 0;
            var hits = new List<ScanResult>();

            for (int loop = 0; loop < parameters.Loops; loop++)
            {
                for (int i = 0; i < frequencies.Count; i++)
                {
                    cts.Token.ThrowIfCancellationRequested();
                    double freq = frequencies[i];

                    // Write frequency (nanoHz format for scanning)
                    await Send(generatorId, GeneratorProtocol.BuildSetFrequency1(freq));

                    // Read delay
                    if (parameters.MinReadDelaySeconds > 0)
                        await Task.Delay((int)(parameters.MinReadDelaySeconds * 1000), cts.Token);

                    // Read sensors
                    var (angle, current) = await ReadSensors(generatorId, parameters.SamplesPerStep);

                    // Update all 4 accumulators
                    raWindow1.Add(current);
                    raWindow2.Add(current);
                    angleWindow1.Add(angle);
                    angleWindow2.Add(angle);

                    // Select primary detection value and RA window
                    double reading = parameters.UseCurrent ? current : angle;
                    var primaryWindow = parameters.UseCurrent
                        ? (parameters.UseRetentiveWindow ? raWindow2 : raWindow1)
                        : (parameters.UseRetentiveWindow ? angleWindow2 : angleWindow1);

                    // Peak detection
                    if (parameters.CalculateUsingPeak && reading > peakReading)
                    {
                        peakReading = reading;
                        peakFrequency = freq;
                    }

                    // Running average detection
                    if (primaryWindow.IsFull)
                    {
                        double ra = primaryWindow.WeightedAverage();
                        double deviation = reading - ra;

                        bool isHit = (parameters.DetectMax && deviation > parameters.Threshold)
                                  || (parameters.DetectMin && deviation < -parameters.Threshold);

                        if (isHit && !parameters.CalculateUsingPeak)
                        {
                            hits.Add(new ScanResult
                            {
                                Frequency = freq,
                                Reading = reading,
                                RunningAverage = ra,
                                Deviation = Math.Abs(deviation),
                                HitCount = 1,
                                Timestamp = DateTime.UtcNow
                            });
                            hits = hits.OrderByDescending(h => h.Deviation)
                                       .Take(parameters.MaxHits).ToList();
                        }
                    }

                    progress?.Report(new ScanProgress
                    {
                        CurrentFrequency = freq,
                        PercentComplete = (double)(loop * frequencies.Count + i + 1) /
                                          (parameters.Loops * frequencies.Count) * 100,
                        StepNumber = i + 1,
                        TotalSteps = frequencies.Count,
                        HitsFound = hits.Count,
                        StatusText = $"Scanning {freq:N0} Hz ({i + 1}/{frequencies.Count})",
                        CycleNumber = loop + 1
                    });
                }
            }

            // Peak mode: the single peak is the hit
            if (parameters.CalculateUsingPeak && peakFrequency > 0)
            {
                hits.Add(new ScanResult
                {
                    Frequency = peakFrequency,
                    Reading = peakReading,
                    Deviation = peakReading,
                    HitCount = 1,
                    Timestamp = DateTime.UtcNow
                });
            }

            _logger.LogInformation("Scan complete: {Count} hits found", hits.Count);
            _scanResults[generatorId] = hits;

            // Cleanup: clear frequencies
            await Send(generatorId, GeneratorProtocol.ClearFrequency1);
            await Send(generatorId, GeneratorProtocol.ClearFrequency2);

            // Amplitude ramp-down if enabled (batch for speed)
            if (parameters.EnableAmplitudeRampDown)
            {
                int n = parameters.RampSteps;
                int target = parameters.TargetAmplitudeCv;
                var rampDownCmds = new List<string>();
                for (int i = n - 1; i >= 1; i--)
                {
                    int cv = (int)Math.Round((double)(i + 1) * target / n);
                    rampDownCmds.Add(GeneratorProtocol.BuildSetAmplitudeCv1(cv));
                    rampDownCmds.Add(GeneratorProtocol.BuildSetAmplitudeCv2(cv));
                }
                await _generatorService.SendCommandsBatch(generatorId, rampDownCmds);
            }

            return hits;
        }
        catch (OperationCanceledException)
        {
            _logger.LogInformation("Scan cancelled on generator {Id}", generatorId);
            return _scanResults.TryGetValue(generatorId, out var partial) ? partial : [];
        }
        finally
        {
            _activeScans.TryRemove(generatorId, out _);
        }
    }

    public async Task<List<ScanResult>> RunHuntAndKill(
        int generatorId, ScanParameters parameters,
        IProgress<ScanProgress>? progress = null, CancellationToken ct = default)
    {
        _logger.LogInformation("Starting Hunt and Kill on generator {Id}", generatorId);
        var allHits = new List<ScanResult>();
        int cycle = 0;

        while (!ct.IsCancellationRequested)
        {
            cycle++;
            _logger.LogInformation("Hunt and Kill cycle {Cycle}", cycle);

            progress?.Report(new ScanProgress
            {
                StatusText = $"Hunt and Kill - Cycle {cycle} - Scanning...",
                CycleNumber = cycle
            });

            var hits = await RunBiofeedbackScan(generatorId, parameters, progress, ct);

            if (hits.Count == 0)
            {
                _logger.LogInformation("No hits in cycle {Cycle}, stopping", cycle);
                break;
            }

            allHits = hits;

            // KILL phase
            progress?.Report(new ScanProgress
            {
                StatusText = $"Hunt and Kill - Cycle {cycle} - Running {hits.Count} hits...",
                CycleNumber = cycle, HitsFound = hits.Count
            });

            int targetGen = parameters.RunOnGeneratorId > 0 ? parameters.RunOnGeneratorId : generatorId;

            // Set amplitude for kill
            await Send(targetGen, GeneratorProtocol.BuildSetAmplitudeCv1(parameters.TargetAmplitudeCv));
            await Send(targetGen, GeneratorProtocol.BuildSetAmplitudeCv2(parameters.TargetAmplitudeCv));
            await _generatorService.Start(targetGen);

            // Dwell at each hit frequency
            int dwellMs = (int)(parameters.DwellSeconds * 1000);
            var killFreqs = hits.Select(h => h.Frequency).ToList();

            for (int i = 0; i < killFreqs.Count && !ct.IsCancellationRequested; i++)
            {
                progress?.Report(new ScanProgress
                {
                    CurrentFrequency = killFreqs[i],
                    StatusText = $"Killing {i + 1}/{killFreqs.Count}: {killFreqs[i]:N0} Hz",
                    CycleNumber = cycle, HitsFound = hits.Count,
                    StepNumber = i + 1, TotalSteps = killFreqs.Count,
                    PercentComplete = (double)(i + 1) / killFreqs.Count * 100
                });

                await _generatorService.WriteFrequencies(targetGen, [killFreqs[i]]);
                try { await Task.Delay(dwellMs, ct); }
                catch (OperationCanceledException) { break; }
            }

            await _generatorService.Stop(targetGen);

            if (!parameters.ContinueRefining) break;
        }

        // Final cleanup (matches stop sequence from serial dump)
        await Send(generatorId, GeneratorProtocol.ClearFrequency1);
        await Send(generatorId, GeneratorProtocol.ClearFrequency2);
        await Send(generatorId, GeneratorProtocol.BuildSetAmplitudeCv1(parameters.TargetAmplitudeCv));
        await Send(generatorId, GeneratorProtocol.BuildSetAmplitudeCv2(parameters.TargetAmplitudeCv));

        _logger.LogInformation("Hunt and Kill finished after {Cycles} cycles, {Hits} final hits",
            cycle, allHits.Count);
        return allHits;
    }

    public Task StopScan(int generatorId)
    {
        if (_activeScans.TryRemove(generatorId, out var cts))
        {
            _logger.LogInformation("Stopping scan on generator {Id}", generatorId);
            cts.Cancel();
        }
        return Task.CompletedTask;
    }

    public Task<List<ScanResult>> GetScanResults(int generatorId)
    {
        var results = _scanResults.TryGetValue(generatorId, out var stored) ? stored.ToList() : [];
        return Task.FromResult(results);
    }

    public Task ReverseLookup(double frequency, bool harmonics, bool subHarmonics, double tolerance)
    {
        var results = new List<ScanResult>();
        var now = DateTime.UtcNow;

        if (harmonics)
            for (var m = 2; m <= 20; m++)
                results.Add(new ScanResult { Frequency = frequency * m, HitCount = 1,
                    HarmonicInfo = $"Harmonic {m}x of {frequency:F4} Hz", Timestamp = now });

        if (subHarmonics)
            for (var d = 2; d <= 20; d++)
                results.Add(new ScanResult { Frequency = frequency / d, HitCount = 1,
                    HarmonicInfo = $"Sub-harmonic 1/{d} of {frequency:F4} Hz", Timestamp = now });

        if (tolerance > 0)
            results = results.Where(r => r.Frequency >= frequency - tolerance
                && r.Frequency <= frequency + tolerance || r.Frequency > tolerance).ToList();

        _scanResults.AddOrUpdate(0, results, (_, _) => results);
        return Task.CompletedTask;
    }

    // ── Helpers ──

    private async Task<string?> Send(int generatorId, string command) =>
        await _generatorService.SendCommandWithResponse(generatorId, command);

    private async Task<(double angle, double current)> ReadSensors(int generatorId, int samples)
    {
        double angleSum = 0, currentSum = 0;
        for (int s = 0; s < samples; s++)
        {
            var ar = await Send(generatorId, GeneratorProtocol.ReadAngle);
            var cr = await Send(generatorId, GeneratorProtocol.ReadCurrent);
            angleSum += GeneratorProtocol.ParseSensorReading(ar ?? "");
            currentSum += GeneratorProtocol.ParseSensorReading(cr ?? "");
        }
        return (angleSum / samples, currentSum / samples);
    }

    internal static List<double> CalculateFrequencySteps(ScanParameters parameters)
    {
        var frequencies = new List<double>();
        double freq = parameters.StartFrequency;
        while (freq <= parameters.EndFrequency)
        {
            frequencies.Add(freq);
            freq += parameters.UsePercentageStep
                ? freq * (parameters.StepSizePercent / 100.0)
                : parameters.StepSizeHz;
        }
        return frequencies;
    }

    /// <summary>
    /// Linearly Weighted Moving Average sliding window.
    /// LWMA = (N×newest + (N-1)×next + ... + 1×oldest) / (N×(N+1)/2)
    /// Matches VB6 implementation at VA 0x854F00.
    /// </summary>
    internal sealed class SlidingWindow
    {
        private readonly int _size;
        private readonly Queue<double> _buffer;

        public SlidingWindow(int size)
        {
            _size = Math.Max(1, size);
            _buffer = new Queue<double>(_size);
        }

        public void Add(double value)
        {
            _buffer.Enqueue(value);
            if (_buffer.Count > _size) _buffer.Dequeue();
        }

        public bool IsFull => _buffer.Count >= _size;
        public int Count => _buffer.Count;

        public double SimpleAverage() => _buffer.Count > 0 ? _buffer.Average() : 0;

        public double WeightedAverage()
        {
            if (_buffer.Count == 0) return 0;
            int n = _buffer.Count;
            double weightedSum = 0;
            int weight = 1;
            foreach (var value in _buffer) { weightedSum += value * weight; weight++; }
            return weightedSum / (n * (n + 1) / 2.0);
        }

        public double Peak() => _buffer.Count > 0 ? _buffer.Max() : 0;
    }
}
