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
}
