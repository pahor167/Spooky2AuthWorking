namespace Spooky2.Core.Models;

public enum WaveformType
{
    Sine,
    Square,
    Sawtooth,
    InverseSawtooth,
    Triangle,
    Damped,
    DampedSquare,
    HBomb
}

public sealed record WaveformSettings
{
    public WaveformType WaveformType { get; init; } = WaveformType.Sine;
    public double Amplitude { get; init; }
    public double Phase { get; init; }
    public double Frequency { get; init; }
}
