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
public sealed class ScanService : IScanService, IDisposable
{
    private readonly IGeneratorService _generatorService;
    private readonly ILogger<ScanService> _logger;
    private readonly ConcurrentDictionary<int, CancellationTokenSource> _activeScans = new();
    private readonly ConcurrentDictionary<int, List<ScanResult>> _scanResults = new();

    /// <summary>
    /// Upper bound (in RA windows) for the detection settle warm-up search. If no
    /// settled window is found within WarmupCapWindows * RaWindow steps the warm-up
    /// falls back to RaWindow, so a persistently noisy scan still scores from the first
    /// full window rather than being fully discarded.
    /// </summary>
    private const int WarmupCapWindows = 5;

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

            // Minimum step period (write-to-write), derived from the original dump's
            // 14-15 steps/s ≈ 70 ms/step. This is NOT an additive sleep: the serial
            // round-trips count toward the period and we only sleep the remainder.
            long periodMs = (long)(parameters.MinReadDelaySeconds * 1000);
            // Settle pause between the frequency write and the first read mimics the
            // original's natural ~23 ms bus latency. Skipped entirely when period is 0.
            int settleMs = periodMs > 0 ? (int)Math.Min(25L, periodMs / 3) : 0;

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
                    // Step start (monotonic) right before the :w24 frequency write.
                    var stepStart = System.Diagnostics.Stopwatch.StartNew();
                    double freq = frequencies[i];

                    // Write frequency (nanoHz format for scanning)
                    await Send(generatorId, GeneratorProtocol.BuildSetFrequency1(freq));

                    // Settle pause mimicking the original's natural bus latency.
                    if (settleMs > 0)
                        await Task.Delay(settleMs, cts.Token);

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

                    // Pace to the minimum step period: sleep only the remainder after
                    // the serial I/O already consumed part of it. When period is 0
                    // (test/replay fast path) this is a no-op — behavior is unchanged.
                    if (periodMs > 0)
                    {
                        long remainingMs = periodMs - stepStart.ElapsedMilliseconds;
                        if (remainingMs > 0)
                            await Task.Delay((int)remainingMs, cts.Token);
                    }
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
            if (_activeScans.TryRemove(generatorId, out var removedCts))
                removedCts.Dispose();
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
            cts.Dispose();
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

    // ── IDisposable ──

    public void Dispose()
    {
        foreach (var kvp in _activeScans)
        {
            if (_activeScans.TryRemove(kvp.Key, out var cts))
            {
                cts.Cancel();
                cts.Dispose();
            }
        }
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
    /// Post-processing hit detection. Faithful port of the ORIGINAL Spooky2
    /// detection loop decoded from Spooky.exe FUN_008531a0 (Ghidra). The original
    /// collects all readings during the scan, then post-processes:
    ///   1. Compute SMA (Simple Moving Average) + deviation for each step.
    ///   2. "Detecting Asymptotes" — plateau-aware slope-change extrema of the raw
    ///      signal. A local max is a strict rise into a flat run followed by a
    ///      strict fall; the run collapses to its LEFT edge, mirroring the decoded
    ///      <c>markers[left]=1</c> placed after walking back over the equal run.
    ///   3. "Filling GreatestHits" — collect extrema whose deviation exceeds threshold.
    ///   4. Sort by deviation descending, take top MaxHits.
    ///
    /// Reported frequency: the original associates a peak found at readings index
    /// p with the frequency of the NEXT sweep step (scanReadings[p+1]). This +1
    /// step pairing makes the reported hit frequencies equal the original
    /// software's screenshot output exactly (see GROUND_TRUTH.md).
    /// </summary>
    internal static List<ScanResult> DetectHits(
        List<(double Frequency, double Reading)> scanReadings, ScanParameters parameters)
    {
        int windowSize = parameters.RaWindow;
        var window = new SlidingWindow(windowSize);

        // Phase 1: Compute SMA and deviation for each step.
        // Settle warm-up: the FIRST scanReadings index at which the SMA window is full
        // AND "settled" (range <= tolerance * mean). Detection scores no step before
        // this, so a generator/sensor startup transient (early readings at the baseline
        // level that then JUMP to the settled level) is never selected as a hit. This
        // is a one-time LEADING gate — a real peak widens the range later and is NOT
        // re-gated. Bounded by a cap so a noisy scan still scores from the first full
        // window. -1 = not yet found.
        int warmupStart = -1;
        int warmupCap = WarmupCapWindows * windowSize;
        var steps = new List<(double Freq, double Reading, double Deviation, double Ra)>();
        for (int idx = 0; idx < scanReadings.Count; idx++)
        {
            var (freq, reading) = scanReadings[idx];
            double ra = window.IsFull ? window.SimpleAverage() : 0;
            double deviation = window.IsFull ? reading - ra : 0;
            // Evaluate settle on the window state BEFORE this reading is added — the
            // same window the deviation above was computed against.
            if (warmupStart < 0 && window.IsFull && idx <= warmupCap && window.IsSettled(parameters.SettleToleranceFraction))
                warmupStart = idx;
            steps.Add((freq, reading, deviation, ra));
            window.Add(reading);
        }
        // Fallback: no settled window within the cap → start at the first full window
        // (one RaWindow in) so a noisy scan is never fully discarded.
        if (warmupStart < 0)
            warmupStart = Math.Min(windowSize, steps.Count);

        // Phase 2 + 3: plateau-aware slope-change extrema passing the threshold.
        // For each candidate, expand the run of equal readings around it. A local
        // maximum exists when the neighbor BELOW the run and the neighbor ABOVE the
        // run are both strictly smaller; the decoded loop collapses the run to its
        // LEFT edge (markers[left]=1), so we score the run's left-edge index.
        var greatestHits = new List<ScanResult>();
        int i = 1;
        while (i < steps.Count - 1)
        {
            // Leading settle warm-up: do not score any step before the window has
            // settled (suppresses the startup-transient false peak).
            if (i < warmupStart) { i++; continue; }

            double reading = steps[i].Reading;

            // Expand the equal-reading run [left..right].
            int left = i;
            while (left - 1 >= 0 && steps[left - 1].Reading == reading) left--;
            int right = i;
            while (right + 1 < steps.Count && steps[right + 1].Reading == reading) right++;

            int prevIdx = left - 1;
            int nextIdx = right + 1;
            if (prevIdx < 0 || nextIdx >= steps.Count) { i = right + 1; continue; }

            double prevReading = steps[prevIdx].Reading;
            double nextReading = steps[nextIdx].Reading;

            bool isLocalMax = prevReading < reading && nextReading < reading;
            bool isLocalMin = prevReading > reading && nextReading > reading;

            // Plateau representative = LEFT edge (matches decoded markers[left]).
            var peak = steps[left];
            bool isHit = (parameters.DetectMax && isLocalMax && peak.Deviation > parameters.Threshold)
                      || (parameters.DetectMin && isLocalMin && peak.Deviation < -parameters.Threshold);

            if (isHit)
            {
                // Report the NEXT sweep step's frequency (+1 step pairing) so the
                // reported hit frequency matches the original software exactly.
                double reportFreq = left + 1 < steps.Count ? steps[left + 1].Freq : peak.Freq;
                greatestHits.Add(new ScanResult
                {
                    Frequency = reportFreq,
                    Reading = peak.Reading,
                    RunningAverage = peak.Ra,
                    Deviation = Math.Abs(peak.Deviation),
                    HitCount = 1,
                    Timestamp = DateTime.UtcNow
                });
            }

            // Advance past this run so a plateau is registered once.
            i = right + 1;
        }

        // Phase 4: Sort by deviation descending, take top MaxHits
        return greatestHits
            .OrderByDescending(h => h.Deviation)
            .Take(parameters.MaxHits)
            .ToList();
    }

    /// <summary>
    /// 0.025% log (or linear Hz) sweep grid.
    ///
    /// Alignment with the ORIGINAL Spooky2 (proven against Data/FullHuntAndKill):
    /// sweep step i TRANSMITS StartFrequency * (1 + step)^(i+1), i.e. the first
    /// recorded sweep frequency is StartFrequency * (1 + step) (41010.25 Hz for the
    /// defaults, NOT 41000.00), and the grid runs one step PAST EndFrequency
    /// (last ≈ 1800103.30 Hz). We therefore ADVANCE one step BEFORE recording each
    /// entry. Readings pair 1:1 with this corrected grid, so a reported/killed hit
    /// frequency matches what the original transmitted.
    ///
    /// Verified: 15130 entries for the default params; first = 41010.25,
    /// last ≈ 1800103.2959…; CalculateFrequencySteps(default)[i] equals the dump's
    /// decoded :w24 sweep frequency at index i within 1e-6 relative for all 15130
    /// steps. The continuation condition still tests the pre-advance value so the
    /// entry count matches the dump exactly. Linear (Hz-step) mode applies the same
    /// one-step-in shift (first recorded entry = StartFrequency + StepSizeHz).
    /// </summary>
    internal static List<double> CalculateFrequencySteps(ScanParameters parameters)
    {
        var frequencies = new List<double>();
        double freq = parameters.StartFrequency;
        while (freq <= parameters.EndFrequency)
        {
            // Advance one step BEFORE recording so the first transmitted/recorded
            // frequency is StartFrequency*(1+step), matching the original dump.
            freq += parameters.UsePercentageStep
                ? freq * (parameters.StepSizePercent / 100.0)
                : parameters.StepSizeHz;
            frequencies.Add(freq);
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

        /// <summary>
        /// True when the window's spread is within <paramref name="toleranceFraction"/>
        /// of its mean, i.e. (max - min) &lt;= toleranceFraction * mean. Used by the
        /// detection settle warm-up to detect that the signal has stopped jumping
        /// (the generator/sensor has settled). An empty or non-positive-mean window is
        /// treated as not settled.
        /// </summary>
        public bool IsSettled(double toleranceFraction)
        {
            if (_buffer.Count == 0) return false;
            double min = double.MaxValue, max = double.MinValue, sum = 0;
            foreach (var value in _buffer)
            {
                if (value < min) min = value;
                if (value > max) max = value;
                sum += value;
            }
            double mean = sum / _buffer.Count;
            if (mean <= 0) return false;
            return (max - min) <= toleranceFraction * mean;
        }
    }
}
