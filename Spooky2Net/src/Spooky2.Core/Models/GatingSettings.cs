namespace Spooky2.Core.Models;

public enum GateType
{
    Timed,
    Hz
}

public sealed record GatingSettings
{
    public GateType GateType { get; init; } = GateType.Timed;
    public double OnDurationMs { get; init; }
    public double OffDurationMs { get; init; }
    public bool Output1Enabled { get; init; }
    public bool Output2Enabled { get; init; }
}
