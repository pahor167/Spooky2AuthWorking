namespace Spooky2.Core.Models;

public sealed record ReverseLookupResult
{
    public required string ProgramName { get; init; }
    public string Category { get; init; } = "";
    public string SourceDatabase { get; init; } = "";
    public double MatchedFrequency { get; init; }
    public double SearchFrequency { get; init; }
    public string MatchType { get; init; } = "Direct"; // "Direct", "Harmonic 2x", "Sub-harmonic 1/3", etc.
    public double ToleranceHz { get; init; }
}
