using Spooky2.Core.Interfaces;
using Spooky2.Core.Models;

namespace Spooky2.Services.Calculator;

/// <summary>
/// Colloidal silver PPM calculations.
/// Two methods: measurement-based (TDS meter) and calculation-based (Faraday's law).
/// </summary>
public sealed class ColloidalSilverCalculatorService : IColloidalSilverCalculator
{
    private const double SilverAtomicWeight = 107.87;
    private const double FaradayConstant = 96485.0;
    private const int SilverValence = 1;

    public ColloidalSilverResult CalculateByMeasurement(ColloidalSilverParams parameters)
    {
        var estimatedPpm = parameters.CurrentTds - parameters.InitialTds;

        return new ColloidalSilverResult
        {
            EstimatedPpm = estimatedPpm,
            RunTimeMinutes = 0,
            Notes = "Measured via TDS meter"
        };
    }

    public ColloidalSilverResult CalculateByCalculation(ColloidalSilverParams parameters)
    {
        var currentAmps = parameters.CurrentMilliamps / 1000.0;
        var runTimeSeconds = 0.0;
        var estimatedPpm = 0.0;
        var runTimeMinutes = 0.0;

        if (parameters.WaterVolumeMl > 0 && currentAmps > 0)
        {
            // Calculate run time needed for target PPM
            var targetGrams = parameters.TargetPpm * parameters.WaterVolumeMl / 1_000_000.0;
            var targetCoulombs = targetGrams * FaradayConstant / SilverAtomicWeight;
            runTimeMinutes = targetCoulombs / currentAmps / 60.0;
            runTimeSeconds = runTimeMinutes * 60.0;

            // Calculate estimated PPM based on that run time
            var totalCoulombs = currentAmps * runTimeSeconds;
            var gramsOfSilver = (totalCoulombs * SilverAtomicWeight) / (SilverValence * FaradayConstant);
            estimatedPpm = (gramsOfSilver * 1_000_000.0) / parameters.WaterVolumeMl;
        }

        return new ColloidalSilverResult
        {
            EstimatedPpm = estimatedPpm,
            RunTimeMinutes = runTimeMinutes,
            Notes = $"Calculated via Faraday's law (I={parameters.CurrentMilliamps}mA, V={parameters.WaterVolumeMl}ml)"
        };
    }
}
