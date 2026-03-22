using CommunityToolkit.Mvvm.ComponentModel;
using CommunityToolkit.Mvvm.Input;
using Spooky2.Core.Interfaces;

namespace Spooky2.ViewModels;

public partial class GeneratorViewModel : ObservableObject
{
    private readonly IGeneratorService _generatorService;

    public GeneratorViewModel(IGeneratorService generatorService)
    {
        _generatorService = generatorService;
    }

    [ObservableProperty]
    private int _generatorId;

    [ObservableProperty]
    private string _port = "";

    [ObservableProperty]
    private string _status = "Idle";

    [ObservableProperty]
    private double _currentFrequency;

    [ObservableProperty]
    private string _currentProgram = "";

    [ObservableProperty]
    private string _elapsedTime = "00:00:00";

    [RelayCommand]
    private async Task Start()
    {
        await _generatorService.Start(GeneratorId);
        Status = "Running";
    }

    [RelayCommand]
    private async Task Stop()
    {
        await _generatorService.Stop(GeneratorId);
        Status = "Idle";
    }

    [RelayCommand]
    private async Task Pause()
    {
        await _generatorService.Pause(GeneratorId);
        Status = "Paused";
    }

    [RelayCommand]
    private async Task Hold()
    {
        await _generatorService.Hold(GeneratorId);
        Status = "Held";
    }

    [RelayCommand]
    private async Task Resume()
    {
        await _generatorService.Resume(GeneratorId);
        Status = "Running";
    }

    [RelayCommand]
    private void AmpWobble()
    {
        // Stub: amplitude wobble toggle
    }

    [RelayCommand]
    private void FreqWobble()
    {
        // Stub: frequency wobble toggle
    }

    [RelayCommand]
    private void Scan()
    {
        // Stub: initiate scan on this generator
    }
}
