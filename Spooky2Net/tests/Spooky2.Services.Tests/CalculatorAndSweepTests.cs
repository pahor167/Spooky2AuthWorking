using Xunit;
using Spooky2.Core.Models;
using Spooky2.Services.Calculator;
using Spooky2.Services.CarrierSweep;

namespace Spooky2.Services.Tests;

public class CalculatorAndSweepTests
{
    // ─────────────────────────────────────────────────────────────
    // Colloidal Silver Calculator
    // ─────────────────────────────────────────────────────────────

    [Fact]
    public void ByMeasurement_PpmIsDelta()
    {
        var svc = new ColloidalSilverCalculatorService();
        var result = svc.CalculateByMeasurement(new ColloidalSilverParams
        {
            InitialTds = 5, CurrentTds = 25, TargetPpm = 20
        });
        Assert.Equal(20, result.EstimatedPpm);
        Assert.Equal(0, result.RunTimeMinutes);
    }

    [Fact]
    public void ByMeasurement_NegativeDelta()
    {
        var svc = new ColloidalSilverCalculatorService();
        var result = svc.CalculateByMeasurement(new ColloidalSilverParams
        {
            InitialTds = 30, CurrentTds = 10
        });
        Assert.Equal(-20, result.EstimatedPpm);
    }

    [Fact]
    public void ByCalculation_ProducesPositiveRunTime()
    {
        var svc = new ColloidalSilverCalculatorService();
        var result = svc.CalculateByCalculation(new ColloidalSilverParams
        {
            CurrentMilliamps = 1, WaterVolumeMl = 250, TargetPpm = 20
        });
        Assert.True(result.RunTimeMinutes > 0);
        Assert.True(result.EstimatedPpm > 0);
    }

    [Fact]
    public void ByCalculation_TargetPpmMatchesEstimate()
    {
        var svc = new ColloidalSilverCalculatorService();
        var result = svc.CalculateByCalculation(new ColloidalSilverParams
        {
            CurrentMilliamps = 1, WaterVolumeMl = 250, TargetPpm = 20
        });
        // Estimated PPM should match target since we calculated run time FOR target
        Assert.Equal(20, result.EstimatedPpm, 0.01);
    }

    [Fact]
    public void ByCalculation_ZeroCurrent_ReturnsZero()
    {
        var svc = new ColloidalSilverCalculatorService();
        var result = svc.CalculateByCalculation(new ColloidalSilverParams
        {
            CurrentMilliamps = 0, WaterVolumeMl = 250, TargetPpm = 20
        });
        Assert.Equal(0, result.EstimatedPpm);
        Assert.Equal(0, result.RunTimeMinutes);
    }

    [Fact]
    public void ByCalculation_HigherCurrent_ShorterTime()
    {
        var svc = new ColloidalSilverCalculatorService();
        var low = svc.CalculateByCalculation(new ColloidalSilverParams
        {
            CurrentMilliamps = 1, WaterVolumeMl = 250, TargetPpm = 20
        });
        var high = svc.CalculateByCalculation(new ColloidalSilverParams
        {
            CurrentMilliamps = 10, WaterVolumeMl = 250, TargetPpm = 20
        });
        Assert.True(high.RunTimeMinutes < low.RunTimeMinutes);
    }

    // ─────────────────────────────────────────────────────────────
    // Carrier Sweep
    // ─────────────────────────────────────────────────────────────

    [Fact]
    public async Task CarrierSweep_GeneratesFrequencies()
    {
        var svc = new CarrierSweepService();
        var result = await svc.CreateCarrierSweep(new CarrierSweepParams
        {
            MaxCarrierFrequency = 200_000,
            ModulationFrequency = 5500,
            Tolerance = 0.025,
            DwellPerFrequency = 300,
            ProgramName = "Test Sweep"
        });

        Assert.NotEmpty(result.Frequencies);
        Assert.Equal("Test Sweep", result.Name);
        Assert.Equal(300, result.DwellSeconds);
        Assert.True(result.Frequencies[0] >= 5500);
        Assert.True(result.Frequencies[^1] <= 200_000);
    }

    [Fact]
    public async Task CarrierSweep_CapsAt10000Entries()
    {
        var svc = new CarrierSweepService();
        var result = await svc.CreateCarrierSweep(new CarrierSweepParams
        {
            MaxCarrierFrequency = 10_000_000,
            ModulationFrequency = 1,
            Tolerance = 0.0001,
            DwellPerFrequency = 1
        });
        Assert.True(result.Frequencies.Count <= 10_001);
    }

    [Fact]
    public async Task CarrierSweep_FrequenciesAreAscending()
    {
        var svc = new CarrierSweepService();
        var result = await svc.CreateCarrierSweep(new CarrierSweepParams
        {
            MaxCarrierFrequency = 100_000,
            ModulationFrequency = 1000,
            Tolerance = 0.1
        });

        for (int i = 1; i < result.Frequencies.Count; i++)
        {
            Assert.True(result.Frequencies[i] > result.Frequencies[i - 1]);
        }
    }

    // ─────────────────────────────────────────────────────────────
    // Spectrum Sweep
    // ─────────────────────────────────────────────────────────────

    [Fact]
    public async Task SpectrumSweep_GeneratesBandCenters()
    {
        var svc = new CarrierSweepService();
        var result = await svc.CreateSpectrumSweep(new CarrierSweepParams
        {
            MaxCarrierFrequency = 200_000,
            ModulationFrequency = 5000,
            ProgramName = "Spectrum Test"
        });

        // 200000 / 5000 = 40 bands
        Assert.Equal(40, result.Frequencies.Count);
        Assert.Equal("Spectrum Test", result.Name);
        // First band center = 5000/2 = 2500
        Assert.Equal(2500, result.Frequencies[0]);
    }

    [Fact]
    public async Task SpectrumSweep_ZeroModulation_SingleBand()
    {
        var svc = new CarrierSweepService();
        var result = await svc.CreateSpectrumSweep(new CarrierSweepParams
        {
            MaxCarrierFrequency = 200_000,
            ModulationFrequency = 0
        });
        Assert.Single(result.Frequencies);
    }
}
