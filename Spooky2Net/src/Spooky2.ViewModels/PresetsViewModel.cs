using System.Collections.ObjectModel;
using CommunityToolkit.Mvvm.ComponentModel;
using CommunityToolkit.Mvvm.Input;
using Microsoft.Extensions.Logging;
using Microsoft.Extensions.Logging.Abstractions;
using Spooky2.Core.Interfaces;
using Spooky2.Core.Models;

namespace Spooky2.ViewModels;

public partial class PresetsViewModel : ObservableObject
{
    private readonly IPresetService _presetService;
    private readonly IFileService _fileService;
    private readonly ILogger<PresetsViewModel> _logger;
    private readonly string? _presetsRootPath;
    private Action<Preset>? _onPresetLoaded;
    private Preset? _currentPreset;

    public PresetsViewModel(IPresetService presetService, IFileService fileService, ILogger<PresetsViewModel>? logger = null, string? presetsRootPath = null)
    {
        _presetService = presetService;
        _fileService = fileService;
        _logger = logger ?? NullLogger<PresetsViewModel>.Instance;
        _presetsRootPath = presetsRootPath;
        _logger.LogDebug("PresetsViewModel initialized");
    }

    /// <summary>Sets the callback invoked when a preset is loaded, passing it to the Control tab.</summary>
    public void SetOnPresetLoaded(Action<Preset> callback)
    {
        _onPresetLoaded = callback;
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

    [ObservableProperty]
    private string? _selectedChainItem;

    [ObservableProperty]
    private int _selectedChainIndex = -1;

    [ObservableProperty]
    private string? _selectedProgram;

    [ObservableProperty]
    private string? _selectedLoadedChainItem;

    [ObservableProperty]
    private string _presetCountLabel = "Presets 0";

    [ObservableProperty]
    private string _programCountLabel = "Programs 0";

    [ObservableProperty]
    private string _chainCountLabel = "Presets/Chains 0";

    [ObservableProperty]
    private string _chainLoadedLabel = "Presets/Chains";

    [ObservableProperty]
    private string _estimatedTotalRunTime = "Estimated Total Run Time 00:00:00";

    public ObservableCollection<string> PresetFiles { get; } = new();

    public ObservableCollection<string> ProgramsList { get; } = new();

    public ObservableCollection<string> PresetChainItems { get; } = new();

    public ObservableCollection<string> LoadedChainItems { get; } = new();

    /// <summary>Auto-load preset when selection changes (if LoadProgramsOnSelect is enabled).</summary>
    partial void OnSelectedPresetPathChanged(string value)
    {
        if (LoadProgramsOnSelect && !string.IsNullOrWhiteSpace(value))
        {
            _ = LoadPreset();
        }
    }

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
            PresetCountLabel = $"Presets {PresetFiles.Count}";
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
        if (string.IsNullOrWhiteSpace(CurrentDirectory))
        {
            return;
        }

        // Enforce preset root boundary — don't navigate above the presets base directory
        if (_presetsRootPath is not null)
        {
            var currentFull = Path.GetFullPath(CurrentDirectory);
            var rootFull = Path.GetFullPath(_presetsRootPath);
            if (string.Equals(currentFull, rootFull, StringComparison.OrdinalIgnoreCase))
            {
                _logger.LogDebug("Already at preset root '{Root}', cannot navigate further up", rootFull);
                return;
            }
        }

        var parent = Path.GetDirectoryName(CurrentDirectory);
        CurrentDirectory = parent ?? "";
        _logger.LogDebug("Navigated up to '{Directory}'", CurrentDirectory);
    }

    [RelayCommand]
    private void NavigateUser()
    {
        var userDir = Path.Combine(CurrentDirectory, "User");
        if (_fileService.IsDirectory(userDir))
        {
            CurrentDirectory = userDir;
            _logger.LogDebug("Navigated to user directory '{Directory}'", CurrentDirectory);
        }
        else
        {
            _logger.LogDebug("User directory not found at '{Directory}'", userDir);
        }
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
            _currentPreset = preset;
            _logger.LogInformation("Loaded preset '{Name}' with {Count} programs", preset.Name, preset.Programs.Count);
            ProgramsDisplay = string.Join(Environment.NewLine, preset.Programs.Select(p => p.Name));
            ProgramsList.Clear();
            foreach (var program in preset.Programs)
            {
                ProgramsList.Add(program.Name);
            }
            ProgramCountLabel = $"Programs {preset.Programs.Count}";
            NotesText = preset.Name;

            // Notify the Control tab to load frequencies
            _onPresetLoaded?.Invoke(preset);
            _logger.LogInformation("Preset '{Name}' sent to Control tab", preset.Name);
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Failed to load preset from '{Path}'", SelectedPresetPath);
        }
    }

    [RelayCommand]
    private async Task SavePreset()
    {
        if (_currentPreset == null)
        {
            _logger.LogWarning("SavePreset: no preset currently loaded");
            return;
        }

        var savePath = string.IsNullOrWhiteSpace(SelectedPresetPath)
            ? Path.Combine(CurrentDirectory, $"{_currentPreset.Name}.txt")
            : SelectedPresetPath;

        try
        {
            _logger.LogInformation("Saving preset '{Name}' to '{Path}'", _currentPreset.Name, savePath);
            await _presetService.SavePreset(_currentPreset, savePath);
            _logger.LogInformation("Preset saved successfully to '{Path}'", savePath);
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Failed to save preset to '{Path}'", savePath);
        }
    }

    [RelayCommand]
    private void SavePresetAs()
    {
        _logger.LogDebug("SavePresetAs: not yet implemented");
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
                PresetFiles.Remove(SelectedPresetPath);
                SelectedPresetPath = "";
                _currentPreset = null;
                ProgramsDisplay = "";
                NotesText = "";
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
        if (_currentPreset == null)
        {
            _logger.LogWarning("EditPreset: no preset currently loaded");
            return;
        }

        // Toggle notes to editable mode so user can update the preset name/notes
        _logger.LogDebug("Edit preset '{Name}' - notes field is now editable", _currentPreset.Name);
        NotesText = _currentPreset.Name;
    }

    [RelayCommand]
    private void AddToChain()
    {
        if (!string.IsNullOrWhiteSpace(SelectedPresetPath))
        {
            PresetChainItems.Add(SelectedPresetPath);
            ChainInfoDisplay = $"{PresetChainItems.Count} presets in chain";
            ChainCountLabel = $"Presets/Chains {PresetChainItems.Count}";
            _logger.LogDebug("Added '{Path}' to chain, total {Count}", SelectedPresetPath, PresetChainItems.Count);
        }
    }

    [RelayCommand]
    private void RemoveFromChain()
    {
        if (SelectedChainItem != null && PresetChainItems.Contains(SelectedChainItem))
        {
            _logger.LogDebug("Removing '{Item}' from chain", SelectedChainItem);
            PresetChainItems.Remove(SelectedChainItem);
            ChainInfoDisplay = PresetChainItems.Count > 0
                ? $"{PresetChainItems.Count} presets in chain"
                : "";
            ChainCountLabel = $"Presets/Chains {PresetChainItems.Count}";
        }
    }

    [RelayCommand]
    private async Task SaveChain()
    {
        if (PresetChainItems.Count == 0)
        {
            _logger.LogWarning("SaveChain: chain is empty");
            return;
        }

        try
        {
            var presets = new List<Preset>();
            foreach (var path in PresetChainItems)
            {
                presets.Add(new Preset
                {
                    Name = Path.GetFileNameWithoutExtension(path),
                    FilePath = path
                });
            }

            var chain = new PresetChain
            {
                Name = "Preset Chain",
                Presets = presets
            };

            var savePath = Path.Combine(CurrentDirectory, "chain.txt");
            _logger.LogInformation("Saving chain with {Count} presets to '{Path}'", presets.Count, savePath);
            await _presetService.SavePresetChain(chain, savePath);
            _logger.LogInformation("Chain saved successfully");
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Failed to save chain");
        }
    }

    [RelayCommand]
    private void CopyChain()
    {
        _logger.LogDebug("CopyChain: not yet implemented");
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
        if (SelectedChainItem == null)
        {
            return;
        }

        var index = PresetChainItems.IndexOf(SelectedChainItem);
        if (index > 0)
        {
            var item = PresetChainItems[index];
            PresetChainItems.RemoveAt(index);
            PresetChainItems.Insert(index - 1, item);
            SelectedChainItem = item;
            _logger.LogDebug("Moved chain item up from {From} to {To}", index, index - 1);
        }
    }

    [RelayCommand]
    private void MoveChainDown()
    {
        if (SelectedChainItem == null)
        {
            return;
        }

        var index = PresetChainItems.IndexOf(SelectedChainItem);
        if (index >= 0 && index < PresetChainItems.Count - 1)
        {
            var item = PresetChainItems[index];
            PresetChainItems.RemoveAt(index);
            PresetChainItems.Insert(index + 1, item);
            SelectedChainItem = item;
            _logger.LogDebug("Moved chain item down from {From} to {To}", index, index + 1);
        }
    }
}
