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
/// Detection: Simple Moving Average (SMA) with asymptote (local maxima) detection.
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
            // ── Generator init (commands 4-28 from original dump) ──
            // These MUST run before every scan, not just during discovery.
            // They set Low Frequency Mode, sync, waveform types, etc.
            progress?.Report(new ScanProgress { StatusText = "Initializing generator..." });

            await Send(generatorId, $":w14=0,");    // sync off
            await Send(generatorId, $":w17=0,0,");  // waveform inversion off
            await Send(generatorId, $":w24=0,");     // freq 0
            await Send(generatorId, $":w25=0,");     // freq ch2 0
            await Send(generatorId, $":w15=1,1,");   // LOW FREQUENCY MODE — CRITICAL for Hz scale
            await Send(generatorId, $":w24=00,");    // freq 0 raw
            await Send(generatorId, $":w32=120,");   // amplitude ch1
            await Send(generatorId, $":w33=120,");   // amplitude ch2
            await Send(generatorId, GeneratorProtocol.BuildSetDisplayName("Stopped"));
            await Send(generatorId, $":w13=0,");     // modulation off
            await Send(generatorId, $":w28=0,");     // amplitude cv1 = 0
            await Send(generatorId, $":w29=0,");     // amplitude cv2 = 0
            await Send(generatorId, $":w24=00,");    // freq 0
            await Send(generatorId, GeneratorProtocol.ClearFrequency1);  // :w12=0,,
            await Send(generatorId, GeneratorProtocol.ClearFrequency2);  // :w12=,0,
            await Send(generatorId, $":w32=120,");   // amplitude
            await Send(generatorId, $":w40=0,");     // duty cycle
            await Send(generatorId, $":w33=120,");   // amplitude ch2
            await Send(generatorId, $":w40=0,");     // duty cycle
            await Send(generatorId, $":w13=0,");     // modulation off
            await Send(generatorId, $":w20=11,");    // waveform 1 = sine
            await Send(generatorId, $":w14=1,");     // sync ON
            await Send(generatorId, GeneratorProtocol.ClearFrequency1);
            await Send(generatorId, GeneratorProtocol.ClearFrequency2);
            await Send(generatorId, $":w21=25,");    // waveform 2 = inverse

            // ── Display name ──
            var displayName = string.IsNullOrEmpty(parameters.LogName)
                ? "Running Biofeedback"
                : parameters.LogName;
            await Send(generatorId, GeneratorProtocol.BuildSetDisplayName($"Port - {displayName}"));

            // ── Waveform tables ──
            progress?.Report(new ScanProgress { StatusText = "Uploading waveform tables..." });
            _logger.LogInformation("Uploading {Count} waveform tables", WaveformTables.Commands.Length);
            await _generatorService.SendCommandsBatch(generatorId, WaveformTables.Commands);

            // ── Pre-scan sensor reads + frequency set (matches original order) ──
            await Send(generatorId, GeneratorProtocol.ReadAngle);
            await Send(generatorId, GeneratorProtocol.ReadAngle);
            await Send(generatorId, GeneratorProtocol.ReadCurrent);

            // Set start frequency (raw Hz)
            await Send(generatorId, GeneratorProtocol.BuildSetFrequencyRawHz((int)parameters.StartFrequency));
            await Send(generatorId, $":w21=25,");    // waveform 2 = inverse (again, matching original)

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
                await Send(generatorId, $":w20=11,");  // waveform 1 = sine (after output enable, matching original)

                // Baseline sensor reads before ramp (matching original)
                await Send(generatorId, GeneratorProtocol.ReadAngle);
                await Send(generatorId, GeneratorProtocol.ReadCurrent);

                progress?.Report(new ScanProgress
                {
                    StatusText = "Ramping amplitude up...",
                    AmplitudeCv = firstCv,
                    CurrentFrequency = parameters.StartFrequency
                });

                // Build ramp commands in batch for speed
                var rampCmds = new List<string>();
                for (int i = 1; i <= rampDivisor; i++)
                {
                    int cv = Math.Min((int)Math.Round((double)(i + 1) * targetCv / rampDivisor), targetCv);
                    rampCmds.Add(GeneratorProtocol.BuildSetAmplitudeCv1(cv));
                    rampCmds.Add(GeneratorProtocol.BuildSetAmplitudeCv2(cv));
                }

                await _generatorService.SendCommandsBatch(generatorId, rampCmds);

                progress?.Report(new ScanProgress
                {
                    StatusText = "Amplitude ramp complete",
                    AmplitudeCv = targetCv,
                    CurrentFrequency = parameters.StartFrequency
                });

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

            // Collect baseline readings for pre-seeding the SMA window later.
            var baselineReadings = new List<double>();

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

                baselineReadings.Add(parameters.UseCurrent ? current : angle);
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

            // Collect ALL readings during the sweep for post-processing.
            // The VB6 original (Proc_0_331) writes readings to CSV during the scan,
            // then post-processes them in "Detecting Asymptotes" + "Filling GreatestHits".
            var scanReadings = new List<(double Frequency, double Reading)>();

            // Prepend baseline tail (up to RaWindow entries) so the SMA window
            // is pre-seeded when DetectHits processes the first sweep step.
            foreach (var val in baselineReadings.TakeLast(parameters.RaWindow))
                scanReadings.Add((0, val)); // freq=0 marks baseline entries

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

                    double reading = parameters.UseCurrent ? current : angle;

                    // Peak detection mode
                    if (parameters.CalculateUsingPeak && reading > peakReading)
                    {
                        peakReading = reading;
                        peakFrequency = freq;
                    }

                    // Store reading for post-processing
                    scanReadings.Add((freq, reading));

                    // Update all RA windows (for progress reporting)
                    raWindow1.Add(current);
                    raWindow2.Add(current);
                    angleWindow1.Add(angle);
                    angleWindow2.Add(angle);

                    var primaryWindow = parameters.UseCurrent
                        ? (parameters.UseRetentiveWindow ? raWindow2 : raWindow1)
                        : (parameters.UseRetentiveWindow ? angleWindow2 : angleWindow1);

                    progress?.Report(new ScanProgress
                    {
                        CurrentFrequency = freq,
                        PercentComplete = (double)(loop * frequencies.Count + i + 1) /
                                          (parameters.Loops * frequencies.Count) * 100,
                        StepNumber = i + 1,
                        TotalSteps = frequencies.Count,
                        HitsFound = 0,
                        StatusText = $"Scanning {freq:N0} Hz ({i + 1}/{frequencies.Count})",
                        CycleNumber = loop + 1,
                        CurrentReading = reading,
                        CurrentRunningAverage = primaryWindow.IsFull ? primaryWindow.SimpleAverage() : 0
                    });
                }
            }

            // ══════════════════════════════════════════════════════════
            // POST-PROCESSING: Retrospective analysis
            // Decoded from VB6 Proc_0_331_8531A0 using 25+ analysis agents.
            // Phase 1: Compute SMA + deviation for each step
            // Phase 2: "Detecting Asymptotes" — find local maxima of raw signal
            // Phase 3: "Filling GreatestHits" — collect asymptotes with positive deviation
            // Phase 4: Sort by deviation, take top MaxHits
            // ══════════════════════════════════════════════════════════
            var hits = new List<ScanResult>();

            if (parameters.CalculateUsingPeak && peakFrequency > 0)
            {
                double baselineAvg = baselineReadings.Count > 0 ? baselineReadings.Average() : 0;
                hits.Add(new ScanResult
                {
                    Frequency = peakFrequency,
                    Reading = peakReading,
                    Deviation = peakReading - baselineAvg,
                    HitCount = 1,
                    Timestamp = DateTime.UtcNow
                });
            }
            else if (scanReadings.Count > 2)
            {
                hits = DetectHits(scanReadings, parameters);
            }

            _logger.LogInformation("Scan complete: {Count} hits found", hits.Count);
            _scanResults[generatorId] = hits;

            progress?.Report(new ScanProgress
            {
                StatusText = $"Scan complete - {hits.Count} hits found",
                PercentComplete = 100,
                HitsFound = hits.Count
            });

            // Cleanup: clear frequencies
            await Send(generatorId, GeneratorProtocol.ClearFrequency1);
            await Send(generatorId, GeneratorProtocol.ClearFrequency2);

            // Amplitude ramp-down if enabled (batch for speed)
            if (parameters.EnableAmplitudeRampDown)
            {
                int n = parameters.RampSteps;
                int target = parameters.TargetAmplitudeCv;
                var rampDownCmds = new List<string>();
                for (int i = n - 2; i >= 0; i--)
                {
                    int cv = (int)Math.Round((double)(i + 1) * target / n);
                    rampDownCmds.Add(GeneratorProtocol.BuildSetAmplitudeCv1(cv));
                    rampDownCmds.Add(GeneratorProtocol.BuildSetAmplitudeCv2(cv));
                }
                rampDownCmds.Add(GeneratorProtocol.BuildSetAmplitudeCv1(0));
                rampDownCmds.Add(GeneratorProtocol.BuildSetAmplitudeCv2(0));
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
        var lastCycleHits = new List<ScanResult>();
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

            lastCycleHits = hits;

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

            // Dwell at each hit frequency
            int dwellMs = (int)(parameters.DwellSeconds * 1000);
            var killFreqs = hits.Select(h => h.Frequency).ToList();

            // Set first kill frequency BEFORE starting output
            if (killFreqs.Count > 0)
                await _generatorService.WriteFrequencies(targetGen, [killFreqs[0]]);

            await _generatorService.Start(targetGen);

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

                // First frequency already written before Start; subsequent ones set here
                if (i > 0)
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
            cycle, lastCycleHits.Count);
        return lastCycleHits;
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
                && r.Frequency <= frequency + tolerance).ToList();

        _scanResults.AddOrUpdate(0, results, (_, _) => results);
        return Task.CompletedTask;
    }

    public async Task<List<ReverseLookupResult>> ReverseLookup(
        double frequency, ReverseLookupParameters parameters, IDatabaseService databaseService)
    {
        _logger.LogInformation("Reverse lookup for {Freq} Hz with tolerance {Tol}%, harmonics={H}, sub-harmonics={S}",
            frequency, parameters.TolerancePercent, parameters.IncludeHarmonics, parameters.IncludeSubHarmonics);

        var results = new List<ReverseLookupResult>();

        // Build list of frequencies to search for
        var searchFreqs = new List<(double freq, string matchType)>
        {
            (frequency, "Direct")
        };

        if (parameters.IncludeHarmonics)
        {
            for (int m = 2; m <= parameters.MaxHarmonics; m++)
            {
                searchFreqs.Add((frequency * m, $"Harmonic {m}x"));
            }
        }

        if (parameters.IncludeSubHarmonics)
        {
            for (int d = 2; d <= parameters.MaxHarmonics; d++)
            {
                searchFreqs.Add((frequency / d, $"Sub-harmonic 1/{d}"));
            }
        }

        // Search each database
        foreach (var dbName in parameters.Databases)
        {
            List<DatabaseEntry> entries;
            try
            {
                entries = await databaseService.LoadDatabase(dbName);
            }
            catch (Exception ex)
            {
                _logger.LogWarning(ex, "Failed to load database '{Database}', skipping", dbName);
                continue;
            }

            foreach (var entry in entries)
            {
                foreach (var progFreq in entry.Frequencies)
                {
                    foreach (var (searchFreq, matchType) in searchFreqs)
                    {
                        double tolerance = searchFreq * parameters.TolerancePercent / 100.0;
                        if (parameters.IncludeHz > 0)
                        {
                            tolerance = Math.Max(tolerance, parameters.IncludeHz);
                        }

                        if (Math.Abs(progFreq - searchFreq) <= tolerance)
                        {
                            results.Add(new ReverseLookupResult
                            {
                                ProgramName = entry.Name,
                                Category = entry.Category,
                                SourceDatabase = dbName,
                                MatchedFrequency = progFreq,
                                SearchFrequency = searchFreq,
                                MatchType = matchType,
                                ToleranceHz = tolerance
                            });
                        }
                    }
                }
            }
        }

        // Deduplicate by program name, keep best match (Direct > Harmonic > Sub-harmonic)
        results = results
            .GroupBy(r => r.ProgramName)
            .Select(g => g.OrderBy(r => r.MatchType == "Direct" ? 0 : 1).First())
            .OrderBy(r => r.MatchType)
            .ThenBy(r => r.ProgramName)
            .ToList();

        _logger.LogInformation("Reverse lookup found {Count} matching programs", results.Count);
        return results;
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

    /// <summary>
    /// Post-processing hit detection decoded from VB6 Proc_0_331 ("Performing Retrospective Analysis").
    /// The original collects all readings to CSV during the scan, then post-processes:
    ///   1. Compute SMA (Simple Moving Average) for each step
    ///   2. "Detecting Asymptotes" — find local maxima of the raw signal
    ///   3. "Filling GreatestHits" — collect asymptotes where deviation exceeds threshold
    ///   4. Bubble sort by deviation, take top MaxHits
    /// </summary>
    internal static List<ScanResult> DetectHits(
        List<(double Frequency, double Reading)> scanReadings, ScanParameters parameters)
    {
        int windowSize = parameters.RaWindow;
        var window = new SlidingWindow(windowSize);

        // Phase 1: Compute SMA and deviation for each step
        var steps = new List<(double Freq, double Reading, double Deviation, double Ra)>();
        foreach (var (freq, reading) in scanReadings)
        {
            double ra = window.IsFull ? window.SimpleAverage() : 0;
            double deviation = window.IsFull ? reading - ra : 0;
            steps.Add((freq, reading, deviation, ra));
            window.Add(reading);
        }

        // Phase 2: "Detecting Asymptotes" — find local maxima of the raw signal
        // A local max at step i: reading[i] > reading[i-1] AND reading[i] > reading[i+1]
        var greatestHits = new List<ScanResult>();
        for (int i = 1; i < steps.Count - 1; i++)
        {
            var (freq, reading, deviation, ra) = steps[i];
            double prevReading = steps[i - 1].Reading;
            double nextReading = steps[i + 1].Reading;

            bool isLocalMax = reading > prevReading && reading > nextReading;
            bool isLocalMin = reading < prevReading && reading < nextReading;

            // Phase 3: "Filling GreatestHits" — local extrema with deviation exceeding threshold
            bool isHit = (parameters.DetectMax && isLocalMax && deviation > parameters.Threshold)
                      || (parameters.DetectMin && isLocalMin && deviation < -parameters.Threshold);

            if (isHit)
            {
                greatestHits.Add(new ScanResult
                {
                    Frequency = freq,
                    Reading = reading,
                    RunningAverage = ra,
                    Deviation = Math.Abs(deviation),
                    HitCount = 1,
                    Timestamp = DateTime.UtcNow
                });
            }
        }

        // Phase 4: Sort by deviation descending, take top MaxHits
        return greatestHits
            .OrderByDescending(h => h.Deviation)
            .Take(parameters.MaxHits)
            .ToList();
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

        /// <summary>LWMA implementation kept for compatibility.
        /// Production scan detection uses SimpleAverage() as decoded from VB6 Proc_0_331.</summary>
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
