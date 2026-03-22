using Spooky2.Core.Interfaces;
using Spooky2.Core.Models;

namespace Spooky2.Services.CarrierSweep;

/// <summary>
/// Generates carrier sweep and spectrum sweep frequency programs.
/// These create frequency programs optimised for specific carrier/modulation combinations.
/// </summary>
public sealed class CarrierSweepService : ICarrierSweepService
{
    private const int MaxFrequencyEntries = 10_000;

    public Task<FrequencyProgram> CreateCarrierSweep(CarrierSweepParams parameters)
    {
        var stepSize = parameters.Tolerance * parameters.ModulationFrequency;

        if (stepSize <= 0)
        {
            stepSize = 1.0;
        }

        var estimatedCount = (long)((parameters.MaxCarrierFrequency - parameters.ModulationFrequency) / stepSize) + 1;

        if (estimatedCount > MaxFrequencyEntries)
        {
            stepSize = (parameters.MaxCarrierFrequency - parameters.ModulationFrequency) / MaxFrequencyEntries;
        }

        var frequencies = new List<double>();
        var current = parameters.ModulationFrequency;

        while (current <= parameters.MaxCarrierFrequency)
        {
            frequencies.Add(current);
            current += stepSize;
        }

        // Ensure the max frequency is included
        if (frequencies.Count > 0 && frequencies[^1] < parameters.MaxCarrierFrequency)
        {
            frequencies.Add(parameters.MaxCarrierFrequency);
        }

        var program = new FrequencyProgram
        {
            Name = string.IsNullOrWhiteSpace(parameters.ProgramName)
                ? "Carrier Sweep"
                : parameters.ProgramName,
            Frequencies = frequencies,
            DwellSeconds = parameters.DwellPerFrequency,
            Notes = string.IsNullOrWhiteSpace(parameters.Notes)
                ? $"Carrier sweep from {parameters.ModulationFrequency:F2} Hz to {parameters.MaxCarrierFrequency:F2} Hz, " +
                  $"step {stepSize:F4} Hz, {frequencies.Count} frequencies"
                : parameters.Notes,
            SourceDatabase = "Generated"
        };

        return Task.FromResult(program);
    }

    public Task<FrequencyProgram> CreateSpectrumSweep(CarrierSweepParams parameters)
    {
        var numberOfBands = parameters.ModulationFrequency > 0
            ? (int)(parameters.MaxCarrierFrequency / parameters.ModulationFrequency)
            : 1;

        if (numberOfBands < 1)
        {
            numberOfBands = 1;
        }

        var bandWidth = parameters.MaxCarrierFrequency / numberOfBands;
        var frequencies = new List<double>(numberOfBands);

        for (var i = 0; i < numberOfBands; i++)
        {
            var bandCenter = (i * bandWidth) + (bandWidth / 2.0);
            frequencies.Add(bandCenter);
        }

        var program = new FrequencyProgram
        {
            Name = string.IsNullOrWhiteSpace(parameters.ProgramName)
                ? "Spectrum Sweep"
                : parameters.ProgramName,
            Frequencies = frequencies,
            DwellSeconds = parameters.DwellPerFrequency,
            Notes = string.IsNullOrWhiteSpace(parameters.Notes)
                ? $"Spectrum sweep across {numberOfBands} bands up to {parameters.MaxCarrierFrequency:F2} Hz, " +
                  $"band width {bandWidth:F2} Hz, {frequencies.Count} frequencies"
                : parameters.Notes,
            SourceDatabase = "Generated"
        };

        return Task.FromResult(program);
    }
}
