namespace Spooky2.Core.Models;

public sealed record DatabaseEntry
{
    public required string Name { get; init; }
    public IReadOnlyList<double> Frequencies { get; init; } = [];
    public string Category { get; init; } = string.Empty;
    public string SourceDatabase { get; init; } = string.Empty;
}
