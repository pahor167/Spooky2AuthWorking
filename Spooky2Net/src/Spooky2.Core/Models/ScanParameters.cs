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
    /// <summary>Use current (mA) sensor for hit detection.
    /// Despite the GX Hunt and Kill preset saying BFB_Detect_mA=True,
    /// empirical testing against real scan data proves the original uses
    /// angle/phase for detection — angle finds all 10 expected hits across
    /// all parameter combinations, while current produces spurious hits at
    /// 980K/1150K Hz that mask weaker real hits.</summary>
    public bool UseCurrent { get; init; }
    public bool UseAngle { get; init; } = true;
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
    /// <summary>Number of ramp steps (verified from dump: 330 steps, formula round((i+1)*target/330)).</summary>
    public int RampSteps { get; init; } = 330;
    /// <summary>Target amplitude in centivolt (2000 = 20.00V).</summary>
    public int TargetAmplitudeCv { get; init; } = 2000;
    /// <summary>Enable amplitude ramp-down after scan.</summary>
    public bool EnableAmplitudeRampDown { get; init; } = true;
    /// <summary>Number of baseline :r11/:r12 read pairs before sweeping.
    /// Dump shows 203 pairs (+ 1 initial standalone :r11 = 407 total reads).</summary>
    public int BaselineReadCount { get; init; } = 203;
    /// <summary>Minimum frequency separation between hits as a percentage (0.025 = one step).
    /// When a new hit is within this distance of an existing hit, the one with higher deviation
    /// is kept and the other is discarded. Prevents cluster monopolization of hit slots.
    /// Verified from original VB6 output: nearest distinct hits are ~0.05% apart.</summary>
    public double MinHitSeparationPercent { get; init; } = 0.05;
}
