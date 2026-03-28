namespace Spooky2.Core.Models;

public sealed record ReverseLookupParameters
{
    public bool IncludeHarmonics { get; init; } = true;
    public bool IncludeSubHarmonics { get; init; } = true;
    public double TolerancePercent { get; init; } = 0.25;
    public double IncludeHz { get; init; }
    public int MaxHarmonics { get; init; } = 20;
    public IReadOnlyList<string> Databases { get; init; } = ["Rife", "CAFL", "XTRA", "BIO", "RUSS"];
}
