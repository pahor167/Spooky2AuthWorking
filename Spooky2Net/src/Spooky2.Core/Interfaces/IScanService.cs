using Spooky2.Core.Models;

namespace Spooky2.Core.Interfaces;

public interface IScanService
{
    Task StartScan(int generatorId, ScanParameters parameters);
    Task StopScan(int generatorId);
    Task<List<ScanResult>> GetScanResults(int generatorId);
    Task ReverseLookup(double frequency, bool harmonics, bool subHarmonics, double tolerance);
}
