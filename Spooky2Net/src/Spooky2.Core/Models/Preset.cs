namespace Spooky2.Core.Models;

public sealed record Preset
{
    public required string Name { get; init; }
    public IReadOnlyList<FrequencyProgram> Programs { get; init; } = [];
    public IReadOnlyDictionary<string, string> Settings { get; init; } = new Dictionary<string, string>();
    public string FilePath { get; init; } = string.Empty;
}
