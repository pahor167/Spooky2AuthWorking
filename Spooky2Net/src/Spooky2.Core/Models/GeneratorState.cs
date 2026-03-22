namespace Spooky2.Core.Models;

public enum GeneratorStatus
{
    Idle,
    Running,
    Paused,
    Held
}

public sealed record GeneratorState
{
    public required int Id { get; init; }
    public string Port { get; init; } = string.Empty;
    public GeneratorStatus Status { get; init; } = GeneratorStatus.Idle;
    public double CurrentFrequency { get; init; }
    public string CurrentProgram { get; init; } = string.Empty;
    public TimeSpan ElapsedTime { get; init; }
}
