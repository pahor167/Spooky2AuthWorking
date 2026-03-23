using CommunityToolkit.Mvvm.ComponentModel;
using CommunityToolkit.Mvvm.Input;
using Microsoft.Extensions.Logging;
using Microsoft.Extensions.Logging.Abstractions;
using Spooky2.Core.Interfaces;

namespace Spooky2.ViewModels;

public partial class GeneratorViewModel : ObservableObject
{
    private readonly IGeneratorService _generatorService;
    private readonly ILogger<GeneratorViewModel> _logger;

    public GeneratorViewModel(IGeneratorService generatorService, ILogger<GeneratorViewModel>? logger = null)
    {
        _generatorService = generatorService;
        _logger = logger ?? NullLogger<GeneratorViewModel>.Instance;
        _logger.LogDebug("GeneratorViewModel initialized");
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
        try
        {
            _logger.LogInformation("Starting generator {Id}", GeneratorId);
            await _generatorService.Start(GeneratorId);
            Status = "Running";
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Failed to start generator {Id}", GeneratorId);
        }
    }

    [RelayCommand]
    private async Task Stop()
    {
        try
        {
            _logger.LogInformation("Stopping generator {Id}", GeneratorId);
            await _generatorService.Stop(GeneratorId);
            Status = "Idle";
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Failed to stop generator {Id}", GeneratorId);
        }
    }

    [RelayCommand]
    private async Task Pause()
    {
        try
        {
            _logger.LogInformation("Pausing generator {Id}", GeneratorId);
            await _generatorService.Pause(GeneratorId);
            Status = "Paused";
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Failed to pause generator {Id}", GeneratorId);
        }
    }

    [RelayCommand]
    private async Task Hold()
    {
        try
        {
            _logger.LogInformation("Holding generator {Id}", GeneratorId);
            await _generatorService.Hold(GeneratorId);
            Status = "Held";
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Failed to hold generator {Id}", GeneratorId);
        }
    }

    [RelayCommand]
    private async Task Resume()
    {
        try
        {
            _logger.LogInformation("Resuming generator {Id}", GeneratorId);
            await _generatorService.Resume(GeneratorId);
            Status = "Running";
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Failed to resume generator {Id}", GeneratorId);
        }
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
