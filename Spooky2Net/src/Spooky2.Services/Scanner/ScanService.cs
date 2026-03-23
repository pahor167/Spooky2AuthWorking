using System.Collections.Concurrent;
using Microsoft.Extensions.Logging;
using Microsoft.Extensions.Logging.Abstractions;
using Spooky2.Core.Interfaces;
using Spooky2.Core.Models;
using Spooky2.Services.Communication;

namespace Spooky2.Services.Scanner;

/// <summary>
/// Full biofeedback scan engine implementing the Spooky2 scan protocol.
/// Protocol per frequency step (from serial dump analysis):
///   1. Write frequency: :w24=freq_nanoHz,
///   2. Read angle: :r11=, → response :r11=value.
///   3. Read current: :r12=, → response :r12=value.
///
/// Detection uses a Linearly Weighted Moving Average (LWMA) matching VB6:
///   LWMA = Σ(reading[i] * weight[i]) / Σ(weight[i])
///   where weight[i] = i+1 for position i (newest sample gets highest weight).
/// This matches the decompiled VB6 at VA 0x854F00 which multiplies each
/// reading by its window position before accumulating.
///
/// Supports dual RA windows (BFB_RA_Window_1, BFB_RA_Window_2) and
/// both Peak and Running Average detection modes.
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
        _logger.LogInformation("Starting biofeedback scan on generator {Id}: {Start}-{End} Hz, mode={Mode}",
            generatorId, parameters.StartFrequency, parameters.EndFrequency,
            parameters.CalculateUsingPeak ? "Peak" : "RA");

        var cts = CancellationTokenSource.CreateLinkedTokenSource(ct);
        _activeScans[generatorId] = cts;

        try
        {
            var frequencies = CalculateFrequencySteps(parameters);
            _logger.LogInformation("Scan has {Count} frequency steps", frequencies.Count);

            if (parameters.StartDelayMs > 0)
                await Task.Delay(parameters.StartDelayMs, cts.Token);

            await _generatorService.SendCommandWithResponse(generatorId,
                GeneratorProtocol.BuildSetDisplayName($"Scanning {parameters.LogName}..."));

            // 4 parallel accumulators (matching VB6's 4-channel tracking):
            //   0: current (primary), 1: angle, 2: current window 2, 3: angle window 2
            var raWindow1 = new SlidingWindow(parameters.RaWindow);
            var raWindow2 = new SlidingWindow(parameters.RaWindow2 > 0 ? parameters.RaWindow2 : parameters.RaWindow);
            var angleWindow1 = new SlidingWindow(parameters.RaWindow);
            var angleWindow2 = new SlidingWindow(parameters.RaWindow2 > 0 ? parameters.RaWindow2 : parameters.RaWindow);

            double peakReading = double.MinValue;
            double peakFrequency = 0;
            var hits = new List<ScanResult>();

            for (int loop = 0; loop < parameters.Loops; loop++)
            {
                for (int i = 0; i < frequencies.Count; i++)
                {
                    cts.Token.ThrowIfCancellationRequested();
                    double freq = frequencies[i];

                    // Write frequency
                    await _generatorService.SendCommandWithResponse(generatorId,
                        GeneratorProtocol.BuildSetFrequency1(freq));

                    // Read delay
                    if (parameters.MinReadDelaySeconds > 0)
                        await Task.Delay((int)(parameters.MinReadDelaySeconds * 1000), cts.Token);

                    // Read sensor values (average over SamplesPerStep)
                    double angleSum = 0, currentSum = 0;
                    for (int s = 0; s < parameters.SamplesPerStep; s++)
                    {
                        var angleResp = await _generatorService.SendCommandWithResponse(generatorId,
                            GeneratorProtocol.ReadAngle);
                        var currentResp = await _generatorService.SendCommandWithResponse(generatorId,
                            GeneratorProtocol.ReadCurrent);

                        angleSum += GeneratorProtocol.ParseSensorReading(angleResp ?? "");
                        currentSum += GeneratorProtocol.ParseSensorReading(currentResp ?? "");
                    }
                    double angle = angleSum / parameters.SamplesPerStep;
                    double current = currentSum / parameters.SamplesPerStep;

                    // Update all 4 accumulators
                    raWindow1.Add(current);
                    raWindow2.Add(current);
                    angleWindow1.Add(angle);
                    angleWindow2.Add(angle);

                    // Select primary detection value and RA
                    double reading = parameters.UseCurrent ? current : angle;
                    var primaryWindow = parameters.UseRetentiveWindow ? raWindow2 : raWindow1;
                    if (!parameters.UseCurrent)
                        primaryWindow = parameters.UseRetentiveWindow ? angleWindow2 : angleWindow1;

                    // Detection
                    if (parameters.CalculateUsingPeak)
                    {
                        // Peak mode: track absolute maximum
                        if (reading > peakReading)
                        {
                            peakReading = reading;
                            peakFrequency = freq;
                        }
                    }

                    // Running average detection (always computed, used unless peak-only)
                    if (primaryWindow.IsFull)
                    {
                        double ra = primaryWindow.WeightedAverage();
                        double deviation = reading - ra;

                        bool isHit = false;
                        if (parameters.DetectMax && deviation > parameters.Threshold)
                            isHit = true;
                        else if (parameters.DetectMin && deviation < -parameters.Threshold)
                            isHit = true;

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
                        StatusText = $"Scanning {freq:N0} Hz ({i + 1}/{frequencies.Count}) Loop {loop + 1}/{parameters.Loops}",
                        CycleNumber = loop + 1
                    });
                }
            }

            // In peak mode, the single peak IS the hit
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

            await _generatorService.SendCommandWithResponse(generatorId, GeneratorProtocol.ClearFrequency1);
            await _generatorService.SendCommandWithResponse(generatorId, GeneratorProtocol.ClearFrequency2);

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
                _logger.LogInformation("No hits found in cycle {Cycle}, stopping", cycle);
                break;
            }

            allHits = hits;

            // KILL phase: run hit frequencies with refinement
            progress?.Report(new ScanProgress
            {
                StatusText = $"Hunt and Kill - Cycle {cycle} - Running {hits.Count} hits...",
                CycleNumber = cycle,
                HitsFound = hits.Count
            });

            int targetGen = parameters.RunOnGeneratorId > 0 ? parameters.RunOnGeneratorId : generatorId;
            var killFrequencies = hits.Select(h => h.Frequency).ToList();

            // Set amplitude for kill phase
            await _generatorService.SendCommandWithResponse(targetGen,
                GeneratorProtocol.BuildSetAmplitudeCv1(2000));
            await _generatorService.SendCommandWithResponse(targetGen,
                GeneratorProtocol.BuildSetAmplitudeCv2(2000));

            await _generatorService.Start(targetGen);

            // Dwell at each hit frequency
            int dwellMs = (int)(parameters.DwellSeconds * 1000);
            for (int i = 0; i < killFrequencies.Count && !ct.IsCancellationRequested; i++)
            {
                progress?.Report(new ScanProgress
                {
                    CurrentFrequency = killFrequencies[i],
                    StatusText = $"Killing freq {i + 1}/{killFrequencies.Count}: {killFrequencies[i]:N0} Hz",
                    CycleNumber = cycle,
                    HitsFound = hits.Count,
                    StepNumber = i + 1,
                    TotalSteps = killFrequencies.Count,
                    PercentComplete = (double)(i + 1) / killFrequencies.Count * 100
                });

                await _generatorService.WriteFrequencies(targetGen, [killFrequencies[i]]);
                try { await Task.Delay(dwellMs, ct); }
                catch (OperationCanceledException) { break; }
            }

            await _generatorService.Stop(targetGen);

            if (!parameters.ContinueRefining)
                break;
        }

        // Final cleanup (matches stop sequence from serial dump)
        await _generatorService.SendCommandWithResponse(generatorId, GeneratorProtocol.ClearFrequency1);
        await _generatorService.SendCommandWithResponse(generatorId, GeneratorProtocol.ClearFrequency2);
        await _generatorService.SendCommandWithResponse(generatorId,
            GeneratorProtocol.BuildSetAmplitudeCv1(2000));
        await _generatorService.SendCommandWithResponse(generatorId,
            GeneratorProtocol.BuildSetAmplitudeCv2(2000));

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
        {
            for (var m = 2; m <= 20; m++)
                results.Add(new ScanResult
                {
                    Frequency = frequency * m, HitCount = 1,
                    HarmonicInfo = $"Harmonic {m}x of {frequency:F4} Hz", Timestamp = now
                });
        }

        if (subHarmonics)
        {
            for (var d = 2; d <= 20; d++)
                results.Add(new ScanResult
                {
                    Frequency = frequency / d, HitCount = 1,
                    HarmonicInfo = $"Sub-harmonic 1/{d} of {frequency:F4} Hz", Timestamp = now
                });
        }

        if (tolerance > 0)
        {
            results = results
                .Where(r => r.Frequency >= frequency - tolerance && r.Frequency <= frequency + tolerance
                            || r.Frequency > tolerance)
                .ToList();
        }

        _scanResults.AddOrUpdate(0, results, (_, _) => results);
        return Task.CompletedTask;
    }

    /// <summary>
    /// Calculates frequency steps for a scan sweep.
    /// Supports fixed Hz and percentage stepping (verified: 0.025% matches serial dump).
    /// </summary>
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
    /// Linearly Weighted Moving Average (LWMA) sliding window.
    /// Matches the VB6 implementation at VA 0x854F00 which weights each reading
    /// by its position: newest sample gets weight N, oldest gets weight 1.
    ///
    /// LWMA = (N*x_N + (N-1)*x_(N-1) + ... + 1*x_1) / (N*(N+1)/2)
    ///
    /// This makes the average track recent values more closely than a simple mean,
    /// so sudden spikes stand out while the baseline remains stable.
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
            if (_buffer.Count > _size)
                _buffer.Dequeue();
        }

        public bool IsFull => _buffer.Count >= _size;
        public int Count => _buffer.Count;

        /// <summary>Simple arithmetic mean (equal weights).</summary>
        public double SimpleAverage() => _buffer.Count > 0 ? _buffer.Average() : 0;

        /// <summary>
        /// Linearly Weighted Moving Average.
        /// Newest sample gets weight = N, oldest gets weight = 1.
        /// </summary>
        public double WeightedAverage()
        {
            if (_buffer.Count == 0) return 0;

            int n = _buffer.Count;
            double weightedSum = 0;
            int weight = 1;

            foreach (var value in _buffer)
            {
                weightedSum += value * weight;
                weight++;
            }

            double totalWeight = n * (n + 1) / 2.0;
            return weightedSum / totalWeight;
        }

        /// <summary>Peak value in the current window.</summary>
        public double Peak() => _buffer.Count > 0 ? _buffer.Max() : 0;
    }
}
