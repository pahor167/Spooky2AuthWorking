namespace Spooky2.Core.Models;

public sealed record FrequencyProgram
{
    public required string Name { get; init; }
    public IReadOnlyList<double> Frequencies { get; init; } = [];
    public double DwellSeconds { get; init; } = 180;
    public string Notes { get; init; } = string.Empty;
    public string SourceDatabase { get; init; } = string.Empty;
}
