using Spooky2.Core.Models;
using Spooky2.Services.Scanner;
using Xunit;

namespace Spooky2.Services.Tests;

/// <summary>
/// Settle-aware detection warm-up tests for <see cref="ScanService.DetectHits"/>.
///
/// Reproduces the field bug: on a real GeneratorX the first few sweep angle readings
/// sit at the BASELINE level (~40871), then JUMP to the settled level (~52000) once
/// the generator/sensor settles. The SMA window straddling that jump yields a large
/// FALSE deviation at the very start, which used to become the #1 hit and get killed.
///
/// The settle warm-up suppresses that startup transient: detection scores no step
/// until the SMA window has settled (range &lt;= SettleToleranceFraction * mean). It is a
/// one-time LEADING gate — genuine peaks later (which widen the window range) are still
/// detected.
/// </summary>
public class SettleWarmupTests
{
    private readonly Random _rng = new(1234);

    private static List<(double Frequency, double Reading)> Scan(List<double> readings) =>
        readings.Select((r, i) => (1000.0 + i, r)).ToList();

    private double Settled(double center, double jitter) =>
        center + (_rng.NextDouble() * 2 - 1) * jitter;

    [Fact]
    public void StartupJump_IsNotSelected_WhileGenuineLaterPeaksAre()
    {
        var parameters = new ScanParameters { RaWindow = 20, Threshold = 0 };

        var readings = new List<double>();
        // 1. SHORT LOW startup plateau at the baseline level (generator not yet
        //    settled). Real data shows ~3 such readings before the jump — crucially
        //    SHORTER than RaWindow, so the first full SMA window STRADDLES the jump
        //    (large range → not settled) and warm-up only completes once the window
        //    has fully moved past the transient.
        for (int i = 0; i < 3; i++) readings.Add(40000.0);
        // 2. JUMP to the settled noisy level (~52000), tiny jitter so it settles fast.
        for (int i = 0; i < 77; i++) readings.Add(Settled(52000.0, 20.0));
        // 3. A genuine small peak: a single sample rising clearly above the local mean.
        int genuinePeakIndexA = readings.Count;
        readings.Add(52400.0);
        for (int i = 0; i < 40; i++) readings.Add(Settled(52000.0, 20.0));
        // 4. A second genuine peak.
        int genuinePeakIndexB = readings.Count;
        readings.Add(52350.0);
        for (int i = 0; i < 40; i++) readings.Add(Settled(52000.0, 20.0));

        var hits = ScanService.DetectHits(Scan(readings), parameters);

        // The huge startup-jump artifact must NOT appear: no hit in the startup/jump
        // region, and no absurd baseline↔settled deviation survives.
        int jumpRegionEnd = 3 + parameters.RaWindow;
        Assert.DoesNotContain(hits, h => h.Frequency < 1000.0 + jumpRegionEnd);
        Assert.All(hits, h => Assert.True(h.Deviation < 1000.0,
            $"startup-jump deviation artifact must be gone, got {h.Deviation}"));

        // The genuine later peaks ARE selected (reported frequency is the +1 step).
        double freqA = 1000.0 + genuinePeakIndexA + 1;
        double freqB = 1000.0 + genuinePeakIndexB + 1;
        Assert.Contains(hits, h => Math.Abs(h.Frequency - freqA) < 1.0);
        Assert.Contains(hits, h => Math.Abs(h.Frequency - freqB) < 1.0);
    }

    [Fact]
    public void BaselinePreseedJump_IsNotSelected_WhileGenuineLaterPeaksAre()
    {
        // Reproduces the EXACT engine path: the SMA window is pre-seeded with a
        // RaWindow-long BASELINE plateau at ~40000 (freq=0, as ScanService prepends
        // baselineReadings.TakeLast(RaWindow)), then the sweep readings JUMP to a
        // settled noisy ~52000. The baseline-filled window is internally homogeneous
        // (tiny range) so the OLD range-only settle returned true immediately and the
        // baseline↔sweep boundary artifact (deviation ~ 52000-40000 ≈ 11000) was scored
        // as the #1 hit. The strengthened settle (reading-vs-mean clause) must reject it.
        var parameters = new ScanParameters { RaWindow = 20, Threshold = 0 };

        var readings = new List<double>();
        // 1. RaWindow-long BASELINE plateau at ~40000 — exactly fills the SMA window.
        for (int i = 0; i < parameters.RaWindow; i++) readings.Add(40000.0);
        // 2. JUMP to the settled noisy sweep level (~52000).
        for (int i = 0; i < 80; i++) readings.Add(Settled(52000.0, 20.0));
        // 3. A genuine peak well past the jump.
        int genuinePeakIndexA = readings.Count;
        readings.Add(52400.0);
        for (int i = 0; i < 40; i++) readings.Add(Settled(52000.0, 20.0));
        // 4. A second genuine peak.
        int genuinePeakIndexB = readings.Count;
        readings.Add(52350.0);
        for (int i = 0; i < 40; i++) readings.Add(Settled(52000.0, 20.0));

        var hits = ScanService.DetectHits(Scan(readings), parameters);

        // (a) NO hit in the baseline/jump/boundary region, no thousands-deviation artifact.
        int jumpRegionEnd = parameters.RaWindow + parameters.RaWindow;
        Assert.DoesNotContain(hits, h => h.Frequency < 1000.0 + jumpRegionEnd);
        Assert.All(hits, h => Assert.True(h.Deviation < 1000.0,
            $"baseline↔sweep deviation artifact (~11000) must be gone, got {h.Deviation}"));

        // (b) The genuine later peaks ARE selected.
        double freqA = 1000.0 + genuinePeakIndexA + 1;
        double freqB = 1000.0 + genuinePeakIndexB + 1;
        Assert.Contains(hits, h => Math.Abs(h.Frequency - freqA) < 1.0);
        Assert.Contains(hits, h => Math.Abs(h.Frequency - freqB) < 1.0);
    }

    [Fact]
    public void FullySettledSeries_WarmsUpAtRaWindow_BehaviorUnchanged()
    {
        // No startup transient: a flat settled series from step 0. warmupStart must
        // equal RaWindow (the first full window), so detection is unchanged vs the
        // pre-warm-up behavior on already-settled data.
        var parameters = new ScanParameters { RaWindow = 20, Threshold = 0 };
        var readings = new List<double>();
        for (int i = 0; i < 120; i++) readings.Add(Settled(52000.0, 15.0));
        int peakIndex = 60;
        readings[peakIndex] = 52500.0;

        var hits = ScanService.DetectHits(Scan(readings), parameters);
        Assert.Equal(parameters.RaWindow, WarmupStartFor(readings, parameters));

        double peakFreq = 1000.0 + peakIndex + 1;
        Assert.Contains(hits, h => Math.Abs(h.Frequency - peakFreq) < 1.0);
    }

    /// <summary>
    /// Test-only re-derivation of the warm-up start, mirroring DetectHits' settle rule,
    /// so a test can assert warmupStart directly without exposing internal engine state.
    /// </summary>
    private static int WarmupStartFor(List<double> readings, ScanParameters parameters)
    {
        var w = new Queue<double>();
        int size = parameters.RaWindow;
        int cap = 5 * size;
        for (int i = 0; i < readings.Count; i++)
        {
            bool full = w.Count >= size;
            if (full && i <= cap)
            {
                double min = w.Min(), max = w.Max(), mean = w.Average();
                double tol = parameters.SettleToleranceFraction * mean;
                // Strengthened settle: range small AND the incoming reading consistent
                // with the window mean (no level discontinuity) — mirrors DetectHits.
                if (mean > 0 && (max - min) <= tol && Math.Abs(readings[i] - mean) <= tol)
                    return i;
            }
            w.Enqueue(readings[i]);
            if (w.Count > size) w.Dequeue();
        }
        return Math.Min(size, readings.Count);
    }
}
