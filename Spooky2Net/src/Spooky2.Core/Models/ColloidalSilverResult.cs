namespace Spooky2.Core.Models;

public sealed record ColloidalSilverResult
{
    public double EstimatedPpm { get; init; }
    public double RunTimeMinutes { get; init; }
    public string Notes { get; init; } = string.Empty;
}
