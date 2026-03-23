using System.Collections.ObjectModel;
using CommunityToolkit.Mvvm.ComponentModel;
using CommunityToolkit.Mvvm.Input;
using Microsoft.Extensions.Logging;
using Microsoft.Extensions.Logging.Abstractions;
using Spooky2.Core.Interfaces;

namespace Spooky2.ViewModels;

public partial class PresetsViewModel : ObservableObject
{
    private readonly IPresetService _presetService;
    private readonly IFileService _fileService;
    private readonly ILogger<PresetsViewModel> _logger;

    public PresetsViewModel(IPresetService presetService, IFileService fileService, ILogger<PresetsViewModel>? logger = null)
    {
        _presetService = presetService;
        _fileService = fileService;
        _logger = logger ?? NullLogger<PresetsViewModel>.Instance;
        _logger.LogDebug("PresetsViewModel initialized");
    }

    [ObservableProperty]
    private string _searchText = "";

    [ObservableProperty]
    private string _selectedPresetPath = "";

    [ObservableProperty]
    private string _programsDisplay = "";

    [ObservableProperty]
    private string _notesText = "";

    [ObservableProperty]
    private string _chainInfoDisplay = "";

    [ObservableProperty]
    private int _selectedPresetIndex = -1;

    [ObservableProperty]
    private bool _loadProgramsOnSelect = true;

    [ObservableProperty]
    private string _currentDirectory = "";

    public ObservableCollection<string> PresetFiles { get; } = new();

    public ObservableCollection<string> PresetChainItems { get; } = new();

    [RelayCommand]
    private async Task Search()
    {
        if (string.IsNullOrWhiteSpace(SearchText))
        {
            return;
        }

        try
        {
            _logger.LogDebug("Searching presets for '{SearchText}' in '{Directory}'", SearchText, CurrentDirectory);
            var results = await _presetService.SearchPresets(SearchText, CurrentDirectory);
            _logger.LogInformation("Preset search returned {Count} results for '{SearchText}'", results.Count, SearchText);
            PresetFiles.Clear();
            foreach (var file in results)
            {
                PresetFiles.Add(file);
            }
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Preset search failed for '{SearchText}'", SearchText);
        }
    }

    [RelayCommand]
    private void ClearSearch()
    {
        SearchText = "";
    }

    [RelayCommand]
    private void NavigateHome()
    {
        CurrentDirectory = "";
    }

    [RelayCommand]
    private void NavigateUp()
    {
        // Stub: navigate to parent directory
    }

    [RelayCommand]
    private void NavigateUser()
    {
        // Stub: navigate to user presets directory
    }

    [RelayCommand]
    private async Task LoadPreset()
    {
        if (string.IsNullOrWhiteSpace(SelectedPresetPath))
        {
            return;
        }

        try
        {
            _logger.LogDebug("Loading preset from '{Path}'", SelectedPresetPath);
            var preset = await _presetService.LoadPreset(SelectedPresetPath);
            _logger.LogInformation("Loaded preset '{Name}' with {Count} programs", preset.Name, preset.Programs.Count);
            ProgramsDisplay = string.Join(Environment.NewLine, preset.Programs.Select(p => p.Name));
            NotesText = preset.Name;
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Failed to load preset from '{Path}'", SelectedPresetPath);
        }
    }

    [RelayCommand]
    private async Task SavePreset()
    {
        // Stub: save current preset
        await Task.CompletedTask;
    }

    [RelayCommand]
    private async Task DeletePreset()
    {
        if (!string.IsNullOrWhiteSpace(SelectedPresetPath))
        {
            try
            {
                _logger.LogInformation("Deleting preset at '{Path}'", SelectedPresetPath);
                await _presetService.DeletePreset(SelectedPresetPath);
            }
            catch (Exception ex)
            {
                _logger.LogError(ex, "Failed to delete preset at '{Path}'", SelectedPresetPath);
            }
        }
    }

    [RelayCommand]
    private void EditPreset()
    {
        // Stub: open preset editor
    }

    [RelayCommand]
    private void AddToChain()
    {
        if (!string.IsNullOrWhiteSpace(SelectedPresetPath))
        {
            PresetChainItems.Add(SelectedPresetPath);
        }
    }

    [RelayCommand]
    private void RemoveFromChain()
    {
        // Stub: remove selected item from chain
    }

    [RelayCommand]
    private async Task SaveChain()
    {
        // Stub: save current chain
        await Task.CompletedTask;
    }

    [RelayCommand]
    private void ClearChain()
    {
        PresetChainItems.Clear();
        ChainInfoDisplay = "";
    }

    [RelayCommand]
    private void MoveChainUp()
    {
        // Stub: move selected chain item up
    }

    [RelayCommand]
    private void MoveChainDown()
    {
        // Stub: move selected chain item down
    }
}
