namespace Spooky2.Core.Models;

public enum WobbleWaveform
{
    None,
    Sine,
    Square,
    Sawtooth,
    InverseSawtooth,
    Triangle
}

public sealed record WobbleSettings
{
    public WobbleWaveform WaveformType { get; init; } = WobbleWaveform.None;
    public double Percentage { get; init; }
    public int Steps { get; init; }
}
