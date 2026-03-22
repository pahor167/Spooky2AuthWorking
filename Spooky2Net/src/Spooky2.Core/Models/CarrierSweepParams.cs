namespace Spooky2.Core.Models;

public sealed record CarrierSweepParams
{
    public double MaxCarrierFrequency { get; init; } = 200_000;
    public double ModulationFrequency { get; init; } = 5500;
    public double Tolerance { get; init; } = 0.025;
    public double DwellPerFrequency { get; init; } = 300;
    public string ProgramName { get; init; } = string.Empty;
    public string Notes { get; init; } = string.Empty;
}
