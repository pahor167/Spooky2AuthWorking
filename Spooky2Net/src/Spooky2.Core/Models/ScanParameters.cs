namespace Spooky2.Core.Models;

public sealed record ScanParameters
{
    public double StartFrequency { get; init; } = 41000;
    public double EndFrequency { get; init; } = 1800000;
    public bool UsePercentageStep { get; init; } = true;
    public double StepSizeHz { get; init; } = 100;
    public double StepSizePercent { get; init; } = 0.025;
    public int MaxHits { get; init; } = 10;
    /// <summary>Primary RA window size (BFB_RA_Window_1).</summary>
    public int RaWindow { get; init; } = 20;
    /// <summary>Secondary RA window size (BFB_RA_Window_2). 0 = same as RaWindow.</summary>
    public int RaWindow2 { get; init; }
    /// <summary>Use the secondary (retentive) RA window for detection.</summary>
    public bool UseRetentiveWindow { get; init; }
    /// <summary>Use peak detection instead of running average.</summary>
    public bool CalculateUsingPeak { get; init; }
    public int SamplesPerStep { get; init; } = 1;
    public int StartDelayMs { get; init; } = 200;
    public double MinReadDelaySeconds { get; init; } = 0.07;
    public bool DetectMax { get; init; } = true;
    public bool DetectMin { get; init; }
    public bool UseCurrent { get; init; } = true;
    public bool UseAngle { get; init; }
    public int Loops { get; init; } = 1;
    public double Threshold { get; init; }
    public bool ContinueRefining { get; init; } = true;
    public bool RunHitsAfterScan { get; init; } = true;
    public int RunOnGeneratorId { get; init; }
    public double DwellSeconds { get; init; } = 180;
    public string LogName { get; init; } = "";

    // Amplitude ramp-up (from preset: Enable_Amplitude_RampUp, Ramp_Amplitude_Up_Rate)
    /// <summary>Enable gradual amplitude ramp from near-zero to target before scanning.</summary>
    public bool EnableAmplitudeRampUp { get; init; } = true;
    /// <summary>Ramp rate: centivolt increment per step (from preset Ramp_Amplitude_Up_Rate * 1.5).</summary>
    public int RampUpRateCv { get; init; } = 6;
    /// <summary>Target amplitude in centivolt (2000 = 20.00V).</summary>
    public int TargetAmplitudeCv { get; init; } = 2000;
    /// <summary>Enable amplitude ramp-down after scan.</summary>
    public bool EnableAmplitudeRampDown { get; init; } = true;
    /// <summary>Number of baseline sensor reads to fill RA buffer before sweeping.</summary>
    public int BaselineReadCount { get; init; } = 42;
}
