using System.Collections.Concurrent;
using Spooky2.Core.Interfaces;
using Spooky2.Core.Models;

namespace Spooky2.Services.Scanner;

/// <summary>
/// In-memory implementation of frequency scanning.
/// Actual biofeedback scanning requires USB communication with the generator hardware.
/// ReverseLookup is fully implemented as pure math (harmonics / sub-harmonics).
/// </summary>
public sealed class ScanService : IScanService
{
    private readonly IGeneratorService _generatorService;
    private readonly ConcurrentDictionary<int, ScanParameters> _activeScans = new();
    private readonly ConcurrentDictionary<int, List<ScanResult>> _scanResults = new();

    public ScanService(IGeneratorService generatorService)
    {
        _generatorService = generatorService;
    }

    public Task StartScan(int generatorId, ScanParameters parameters)
    {
        _activeScans[generatorId] = parameters;
        _scanResults.TryAdd(generatorId, []);

        // Actual scanning requires USB communication with the generator hardware.
        // This stores the parameters and marks the generator as scanning.

        return Task.CompletedTask;
    }

    public Task StopScan(int generatorId)
    {
        _activeScans.TryRemove(generatorId, out _);
        return Task.CompletedTask;
    }

    public Task<List<ScanResult>> GetScanResults(int generatorId)
    {
        var results = _scanResults.TryGetValue(generatorId, out var stored)
            ? stored.ToList()
            : [];

        return Task.FromResult(results);
    }

    public Task ReverseLookup(double frequency, bool harmonics, bool subHarmonics, double tolerance)
    {
        var results = new List<ScanResult>();
        var now = DateTime.UtcNow;

        if (harmonics)
        {
            for (var multiplier = 2; multiplier <= 20; multiplier++)
            {
                var harmonicFreq = frequency * multiplier;
                results.Add(new ScanResult
                {
                    Frequency = harmonicFreq,
                    HitCount = 1,
                    HarmonicInfo = $"Harmonic {multiplier}x of {frequency:F4} Hz",
                    Timestamp = now
                });
            }
        }

        if (subHarmonics)
        {
            for (var divisor = 2; divisor <= 20; divisor++)
            {
                var subHarmonicFreq = frequency / divisor;
                results.Add(new ScanResult
                {
                    Frequency = subHarmonicFreq,
                    HitCount = 1,
                    HarmonicInfo = $"Sub-harmonic 1/{divisor} of {frequency:F4} Hz",
                    Timestamp = now
                });
            }
        }

        if (tolerance > 0)
        {
            results = results
                .Where(r => r.Frequency >= frequency - tolerance && r.Frequency <= frequency + tolerance
                            || r.Frequency > tolerance)
                .ToList();
        }

        // Store reverse lookup results under generator ID 0 (non-generator-specific)
        _scanResults.AddOrUpdate(0, results, (_, _) => results);

        return Task.CompletedTask;
    }
}
