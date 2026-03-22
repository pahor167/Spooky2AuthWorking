namespace Spooky2.Core.Models;

public sealed record ScanResult
{
    public double Frequency { get; init; }
    public int HitCount { get; init; }
    public string HarmonicInfo { get; init; } = string.Empty;
    public DateTime Timestamp { get; init; }
}
