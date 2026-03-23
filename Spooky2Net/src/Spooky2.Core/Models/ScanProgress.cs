namespace Spooky2.Core.Models;

public sealed record ScanProgress
{
    public double CurrentFrequency { get; init; }
    public double PercentComplete { get; init; }
    public int StepNumber { get; init; }
    public int TotalSteps { get; init; }
    public int HitsFound { get; init; }
    public string StatusText { get; init; } = "";
    public int CycleNumber { get; init; }
}
