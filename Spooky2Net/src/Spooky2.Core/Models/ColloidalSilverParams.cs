namespace Spooky2.Core.Models;

public sealed record ColloidalSilverParams
{
    public double InitialTds { get; init; }
    public double CurrentTds { get; init; }
    public double TargetPpm { get; init; } = 20;
    public double CurrentMilliamps { get; init; }
    public int ElectrodeGauge { get; init; }
    public double WaterVolumeMl { get; init; }
}
