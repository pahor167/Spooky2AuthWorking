namespace Spooky2.Core.Models;

public sealed record PulseLimits
{
    // VB6 original: MinBPM
    public int MinimumBeatsPerMinute { get; init; } = 30;

    // VB6 original: MaxBPM
    public int MaximumBeatsPerMinute { get; init; } = 130;

    // HRV = Heart Rate Variability
    // VB6 original: MaxHRV
    public int MaximumHeartRateVariability { get; init; } = 30;
}
