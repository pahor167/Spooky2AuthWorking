namespace Spooky2.Core.Models;

public sealed record PresetChain
{
    public required string Name { get; init; }
    public IReadOnlyList<Preset> Presets { get; init; } = [];
    public int RepeatCount { get; init; } = 1;
}
