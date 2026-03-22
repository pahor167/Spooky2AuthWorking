using CommunityToolkit.Mvvm.ComponentModel;
using CommunityToolkit.Mvvm.Input;
using Spooky2.Core.Interfaces;
using Spooky2.Core.Models;

namespace Spooky2.ViewModels.Dialogs;

public partial class SpectrumSweepViewModel : ObservableObject
{
    private readonly ICarrierSweepService _carrierSweepService;

    public SpectrumSweepViewModel(ICarrierSweepService carrierSweepService)
    {
        _carrierSweepService = carrierSweepService;
    }

    [ObservableProperty]
    private double _maxCarrierFrequency = 200_000;

    [ObservableProperty]
    private double _modulationFrequency = 5500;

    [ObservableProperty]
    private double _tolerance = 0.025;

    [ObservableProperty]
    private double _dwellPerFrequency = 300;

    [ObservableProperty]
    private string _programName = "";

    [ObservableProperty]
    private string _notes = "";

    [ObservableProperty]
    private string _resultDisplay = "";

    [ObservableProperty]
    private bool _showGraph;

    [ObservableProperty]
    private double[]? _graphFrequencies;

    [ObservableProperty]
    private double[]? _graphAmplitudes;

    [RelayCommand]
    private async Task CreateSweep()
    {
        var parameters = new CarrierSweepParams
        {
            MaxCarrierFrequency = MaxCarrierFrequency,
            ModulationFrequency = ModulationFrequency,
            Tolerance = Tolerance,
            DwellPerFrequency = DwellPerFrequency,
            ProgramName = ProgramName,
            Notes = Notes
        };

        var program = await _carrierSweepService.CreateSpectrumSweep(parameters);
        ResultDisplay = $"Created spectrum sweep '{program.Name}' with {program.Frequencies.Count} frequencies";
    }
}
