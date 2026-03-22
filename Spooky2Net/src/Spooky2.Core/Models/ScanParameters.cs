namespace Spooky2.Core.Models;

public sealed record ScanParameters
{
    public double StartFrequency { get; init; }
    public double EndFrequency { get; init; }
    public double StepSize { get; init; }
    public double DwellMs { get; init; }
}
