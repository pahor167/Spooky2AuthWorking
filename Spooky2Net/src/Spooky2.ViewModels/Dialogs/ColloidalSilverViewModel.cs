using CommunityToolkit.Mvvm.ComponentModel;
using CommunityToolkit.Mvvm.Input;
using Spooky2.Core.Interfaces;
using Spooky2.Core.Models;

namespace Spooky2.ViewModels.Dialogs;

public partial class ColloidalSilverViewModel : ObservableObject
{
    private readonly IColloidalSilverCalculator _calculator;

    public ColloidalSilverViewModel(IColloidalSilverCalculator calculator)
    {
        _calculator = calculator;
    }

    // Inputs
    [ObservableProperty]
    private double _initialTds;

    [ObservableProperty]
    private double _currentTds;

    [ObservableProperty]
    private double _targetPpm = 20;

    [ObservableProperty]
    private double _currentMilliamps;

    [ObservableProperty]
    private int _electrodeGauge;

    [ObservableProperty]
    private double _waterVolumeMl;

    // Outputs
    [ObservableProperty]
    private double _estimatedPpm;

    [ObservableProperty]
    private double _runTimeMinutes;

    [ObservableProperty]
    private string _resultNotes = "";

    [RelayCommand]
    private void CalculateByMeasurement()
    {
        var parameters = new ColloidalSilverParams
        {
            InitialTds = InitialTds,
            CurrentTds = CurrentTds,
            TargetPpm = TargetPpm,
            CurrentMilliamps = CurrentMilliamps,
            ElectrodeGauge = ElectrodeGauge,
            WaterVolumeMl = WaterVolumeMl
        };

        var result = _calculator.CalculateByMeasurement(parameters);
        EstimatedPpm = result.EstimatedPpm;
        RunTimeMinutes = result.RunTimeMinutes;
        ResultNotes = result.Notes;
    }

    [RelayCommand]
    private void CalculateByCalculation()
    {
        var parameters = new ColloidalSilverParams
        {
            InitialTds = InitialTds,
            CurrentTds = CurrentTds,
            TargetPpm = TargetPpm,
            CurrentMilliamps = CurrentMilliamps,
            ElectrodeGauge = ElectrodeGauge,
            WaterVolumeMl = WaterVolumeMl
        };

        var result = _calculator.CalculateByCalculation(parameters);
        EstimatedPpm = result.EstimatedPpm;
        RunTimeMinutes = result.RunTimeMinutes;
        ResultNotes = result.Notes;
    }
}
