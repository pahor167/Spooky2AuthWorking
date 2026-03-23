using Spooky2.Core.Models;

namespace Spooky2.Core.Interfaces;

public interface IScanService
{
    Task<List<ScanResult>> RunBiofeedbackScan(int generatorId, ScanParameters parameters,
        IProgress<ScanProgress>? progress = null, CancellationToken ct = default);
    Task<List<ScanResult>> RunHuntAndKill(int generatorId, ScanParameters parameters,
        IProgress<ScanProgress>? progress = null, CancellationToken ct = default);
    Task StopScan(int generatorId);
    Task<List<ScanResult>> GetScanResults(int generatorId);
    Task ReverseLookup(double frequency, bool harmonics, bool subHarmonics, double tolerance);
    Task<List<ReverseLookupResult>> ReverseLookup(
        double frequency,
        ReverseLookupParameters parameters,
        IDatabaseService databaseService);
}
