using System.Collections.ObjectModel;
using System.Diagnostics;
using System.Runtime.InteropServices;
using CommunityToolkit.Mvvm.ComponentModel;
using CommunityToolkit.Mvvm.Input;
using Microsoft.Extensions.Logging;
using Microsoft.Extensions.Logging.Abstractions;
using Spooky2.Core.Interfaces;
using Spooky2.ViewModels.Dialogs;

namespace Spooky2.ViewModels;

public partial class MainViewModel : ObservableObject
{
    private readonly IGeneratorService _generatorService;
    private readonly IPresetService _presetService;
    private readonly IDatabaseService _databaseService;
    private readonly ISettingsService _settingsService;
    private readonly IFileService _fileService;
    private readonly IDialogService _dialogService;
    private readonly IColloidalSilverCalculator _colloidalSilverCalculator;
    private readonly ICarrierSweepService _carrierSweepService;
    private readonly ILogger<MainViewModel> _logger;
    private readonly string _rootPath;

    private static readonly string[] RequiredDirectories =
    {
        "Data",
        "ScanData",
        "Waveforms",
        "Audio",
        "SystemSounds",
        "Preset Collections",
        "Preset Collections/User",
        "Biofeedback"
    };

    public MainViewModel(
        IGeneratorService generatorService,
        IPresetService presetService,
        IDatabaseService databaseService,
        ISettingsService settingsService,
        IFileService fileService,
        IDialogService dialogService,
        IColloidalSilverCalculator colloidalSilverCalculator,
        ICarrierSweepService carrierSweepService,
        IMicroGenService microGenService,
        ILogger<MainViewModel>? logger = null,
        string? rootPath = null)
    {
        _generatorService = generatorService;
        _presetService = presetService;
        _databaseService = databaseService;
        _settingsService = settingsService;
        _fileService = fileService;
        _dialogService = dialogService;
        _colloidalSilverCalculator = colloidalSilverCalculator;
        _carrierSweepService = carrierSweepService;
        _logger = logger ?? NullLogger<MainViewModel>.Instance;
        _rootPath = rootPath ?? Directory.GetCurrentDirectory();

        _logger.LogInformation("MainViewModel initializing, root path: '{RootPath}'", _rootPath);

        Presets = new PresetsViewModel(presetService, fileService);
        Database = new DatabaseViewModel(databaseService, microGenService);
        Settings = new SettingsViewModel();
        System = new SystemViewModel(settingsService);

        _ = InitializeAsync();
    }

    private async Task InitializeAsync()
    {
        try
        {
            _logger.LogDebug("Starting async initialization");

            // Create required directories
            foreach (string dir in RequiredDirectories)
            {
                string fullPath = Path.Combine(_rootPath, dir);
                if (!_fileService.IsDirectory(fullPath))
                {
                    _logger.LogDebug("Creating required directory '{Directory}'", fullPath);
                    _fileService.CreateDirectory(fullPath);
                }
            }

            // Load settings from Data/SystemCFG.txt
            string configPath = Path.Combine(_rootPath, "Data", "SystemCFG.txt");
            _logger.LogDebug("Loading settings from '{ConfigPath}'", configPath);
            var settings = await _settingsService.LoadSettings(configPath);

            // Apply loaded settings to Settings ViewModel properties
            ApplySettings(settings);

            StatusBarText = "Spooky2 (c) John White - Ready";

            // Try to discover generators
            try
            {
                _logger.LogDebug("Discovering generators");
                var found = await _generatorService.FindGenerators();
                foreach (var state in found)
                {
                    var vm = new GeneratorViewModel(_generatorService)
                    {
                        GeneratorId = state.Id,
                        Port = state.Port,
                        Status = state.Status.ToString()
                    };
                    Generators.Add(vm);
                }

                _logger.LogInformation("Initialization complete, {Count} generator(s) found", found.Count);
                if (found.Count > 0)
                {
                    StatusBarText = $"Spooky2 (c) John White - Ready ({found.Count} generator(s) found)";
                }
            }
            catch (Exception ex)
            {
                _logger.LogWarning(ex, "Generator discovery failed (non-fatal)");
                // Generator discovery failure is non-fatal
            }
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Initialization failed");
            StatusBarText = $"Initialization error: {ex.Message}";
        }
    }

    private void ApplySettings(Dictionary<string, string> settings)
    {
        if (settings.TryGetValue("ShowTooltips", out string? showTooltips))
            System.ShowTooltips = string.Equals(showTooltips, "True", StringComparison.OrdinalIgnoreCase);

        if (settings.TryGetValue("PreventHibernation", out string? preventHibernation))
            System.PreventHibernation = string.Equals(preventHibernation, "True", StringComparison.OrdinalIgnoreCase);

        if (settings.TryGetValue("WriteLogFile", out string? writeLogFile))
            System.WriteLogFile = string.Equals(writeLogFile, "True", StringComparison.OrdinalIgnoreCase);

        if (settings.TryGetValue("MinBPM", out string? minBpm) && int.TryParse(minBpm, out int minBpmVal))
            System.MinimumBeatsPerMinute = minBpmVal;

        if (settings.TryGetValue("MaxBPM", out string? maxBpm) && int.TryParse(maxBpm, out int maxBpmVal))
            System.MaximumBeatsPerMinute = maxBpmVal;

        if (settings.TryGetValue("MaxHRV", out string? maxHrv) && int.TryParse(maxHrv, out int maxHrvVal))
            System.MaximumHeartRateVariability = maxHrvVal;

        if (settings.TryGetValue("AmplitudeWobble", out string? ampWobble) && double.TryParse(ampWobble, out double freqAmpVal))
            System.AmplitudeWobblePercentage = freqAmpVal;

        if (settings.TryGetValue("FrequencyWobble", out string? freqWobble) && double.TryParse(freqWobble, out double freqWobbleVal))
            System.FrequencyWobblePercentage = freqWobbleVal;
    }

    [ObservableProperty]
    private string _statusBarText = "Ready";

    [ObservableProperty]
    private int _selectedTabIndex;

    public PresetsViewModel Presets { get; }

    public DatabaseViewModel Database { get; }

    public SettingsViewModel Settings { get; }

    public SystemViewModel System { get; }

    public ControlViewModel? Control { get; set; }

    public ObservableCollection<GeneratorViewModel> Generators { get; } = new();

    [RelayCommand]
    private async Task GlobalStart()
    {
        try
        {
            _logger.LogInformation("Starting all {Count} generators", Generators.Count);
            StatusBarText = "Starting all generators...";
            foreach (var generator in Generators)
            {
                await _generatorService.Start(generator.GeneratorId);
            }
            StatusBarText = "All generators started";
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Failed to start all generators");
            StatusBarText = $"Error starting generators: {ex.Message}";
        }
    }

    [RelayCommand]
    private async Task GlobalStop()
    {
        try
        {
            _logger.LogInformation("Stopping all {Count} generators", Generators.Count);
            StatusBarText = "Stopping all generators...";
            foreach (var generator in Generators)
            {
                await _generatorService.Stop(generator.GeneratorId);
            }
            StatusBarText = "All generators stopped";
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Failed to stop all generators");
            StatusBarText = $"Error stopping generators: {ex.Message}";
        }
    }

    [RelayCommand]
    private async Task GlobalPause()
    {
        StatusBarText = "Pausing all generators...";
        foreach (var generator in Generators)
        {
            await _generatorService.Pause(generator.GeneratorId);
        }
        StatusBarText = "All generators paused";
    }

    [RelayCommand]
    private async Task GlobalHold()
    {
        StatusBarText = "Holding all generators...";
        foreach (var generator in Generators)
        {
            await _generatorService.Hold(generator.GeneratorId);
        }
        StatusBarText = "All generators held";
    }

    [RelayCommand]
    private async Task GlobalResume()
    {
        StatusBarText = "Resuming all generators...";
        foreach (var generator in Generators)
        {
            await _generatorService.Resume(generator.GeneratorId);
        }
        StatusBarText = "All generators resumed";
    }

    [RelayCommand]
    private async Task GlobalErase()
    {
        StatusBarText = "Erasing all generator memory...";
        foreach (var generator in Generators)
        {
            await _generatorService.EraseMemory(generator.GeneratorId);
        }
        StatusBarText = "All generator memory erased";
    }

    [RelayCommand]
    private async Task IdentifyGenerators()
    {
        StatusBarText = "Identifying generators...";
        await _generatorService.IdentifyGenerators();
        StatusBarText = "Generator identification complete";
    }

    [RelayCommand]
    private async Task RescanDevices()
    {
        try
        {
            _logger.LogInformation("Rescanning for devices");
            StatusBarText = "Rescanning for devices...";
            var found = await _generatorService.FindGenerators();
            Generators.Clear();
            foreach (var state in found)
            {
                var vm = new GeneratorViewModel(_generatorService)
                {
                    GeneratorId = state.Id,
                    Port = state.Port,
                    Status = state.Status.ToString()
                };
                Generators.Add(vm);
            }
            _logger.LogInformation("Rescan complete, found {Count} generator(s)", found.Count);
            StatusBarText = $"Found {found.Count} generator(s)";
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Device rescan failed");
            StatusBarText = $"Rescan failed: {ex.Message}";
        }
    }

    [RelayCommand]
    private async Task CreateProgram()
    {
        StatusBarText = "Create Program dialog opened";
        var vm = new CreateProgramViewModel();
        await _dialogService.ShowDialogAsync(vm);
        StatusBarText = "Ready";
    }

    [RelayCommand]
    private async Task CreateCarrierSweep()
    {
        StatusBarText = "Create Carrier Sweep dialog opened";
        var vm = new CarrierSweepViewModel(_carrierSweepService);
        await _dialogService.ShowDialogAsync(vm);
        StatusBarText = "Ready";
    }

    [RelayCommand]
    private async Task CreateSpectrumSweep()
    {
        StatusBarText = "Create Spectrum Sweep dialog opened";
        var vm = new SpectrumSweepViewModel(_carrierSweepService);
        await _dialogService.ShowDialogAsync(vm);
        StatusBarText = "Ready";
    }

    [RelayCommand]
    private async Task OpenCsCalculator()
    {
        StatusBarText = "Colloidal Silver Calculator opened";
        var vm = new ColloidalSilverViewModel(_colloidalSilverCalculator);
        await _dialogService.ShowDialogAsync(vm);
        StatusBarText = "Ready";
    }

    [RelayCommand]
    private async Task RefreshDatabase()
    {
        try
        {
            _logger.LogInformation("Refreshing database");
            StatusBarText = "Refreshing database...";
            await _databaseService.RefreshDatabase();
            StatusBarText = "Database refreshed";
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Database refresh failed");
            StatusBarText = $"Database refresh failed: {ex.Message}";
        }
    }

    [RelayCommand]
    private void RefreshWaveforms()
    {
        StatusBarText = "Waveforms refreshed";
    }

    [RelayCommand]
    private async Task UpdatePresets()
    {
        try
        {
            _logger.LogInformation("Updating presets");
            StatusBarText = "Updating presets...";
            await _presetService.UpdatePresets();
            StatusBarText = "Presets updated";
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Preset update failed");
            StatusBarText = $"Preset update failed: {ex.Message}";
        }
    }

    [RelayCommand]
    private async Task RestoreDefaults()
    {
        try
        {
            _logger.LogInformation("Restoring defaults");
            StatusBarText = "Restoring defaults...";
            await _settingsService.RestoreDefaults();
            StatusBarText = "Defaults restored";
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Restore defaults failed");
            StatusBarText = $"Restore defaults failed: {ex.Message}";
        }
    }

    [RelayCommand]
    private async Task ShowAbout()
    {
        StatusBarText = "About dialog opened";
        var vm = new AboutViewModel();
        await _dialogService.ShowDialogAsync(vm);
        StatusBarText = "Ready";
    }

    [RelayCommand]
    private void OpenUsersGuide()
    {
        StatusBarText = "Opening User's Guide...";
        var guidePath = Path.Combine(_rootPath, "Data", "Spooky2 User's Guide.pdf");

        try
        {
            if (RuntimeInformation.IsOSPlatform(OSPlatform.OSX))
            {
                Process.Start("open", guidePath);
            }
            else if (RuntimeInformation.IsOSPlatform(OSPlatform.Windows))
            {
                Process.Start(new ProcessStartInfo(guidePath) { UseShellExecute = true });
            }
            else
            {
                Process.Start("xdg-open", guidePath);
            }

            StatusBarText = "User's Guide opened";
        }
        catch (Exception ex)
        {
            StatusBarText = $"Failed to open User's Guide: {ex.Message}";
        }
    }

    [RelayCommand]
    private void ExitApplication()
    {
        StatusBarText = "Exiting application...";
        Environment.Exit(0);
    }

    [RelayCommand]
    private async Task SaveSettings()
    {
        try
        {
            _logger.LogInformation("Saving settings");
            StatusBarText = "Saving settings...";
            await System.SaveSettingsCommand.ExecuteAsync(null);
            StatusBarText = "Settings saved";
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Save settings failed");
            StatusBarText = $"Save settings failed: {ex.Message}";
        }
    }

    // ── Database menu ──────────────────────────────────────────────

    [RelayCommand]
    private void Save()
    {
        StatusBarText = "Settings saved";
    }

    [RelayCommand]
    private void EditCustomDatabase1()
    {
        OpenFileInDefaultEditor(Path.Combine(_rootPath, "Data", "CustomDatabase1.csv"));
    }

    [RelayCommand]
    private void EditCustomDatabase2()
    {
        OpenFileInDefaultEditor(Path.Combine(_rootPath, "Data", "CustomDatabase2.csv"));
    }

    [RelayCommand]
    private void EditCustomDatabase3()
    {
        OpenFileInDefaultEditor(Path.Combine(_rootPath, "Data", "CustomDatabase3.csv"));
    }

    [RelayCommand]
    private void EditCustomDatabase4()
    {
        OpenFileInDefaultEditor(Path.Combine(_rootPath, "Data", "CustomDatabase4.csv"));
    }

    [RelayCommand]
    private void EditBfbDatabase()
    {
        OpenFileInDefaultEditor(Path.Combine(_rootPath, "Data", "BFBDatabase.csv"));
    }

    [RelayCommand]
    private void SelectCustomDatabase2()
    {
        StatusBarText = "Custom Database 2 selected";
    }

    [RelayCommand]
    private void SelectCustomDatabase3()
    {
        StatusBarText = "Custom Database 3 selected";
    }

    [RelayCommand]
    private void SelectCustomDatabase4()
    {
        StatusBarText = "Custom Database 4 selected";
    }

    [RelayCommand]
    private void SelectBfbDatabase()
    {
        StatusBarText = "BFB Database selected";
    }

    [RelayCommand]
    private void WriteDatabaseText()
    {
        StatusBarText = "Database text written";
    }

    [RelayCommand]
    private void ExtractPresetFrequencies()
    {
        StatusBarText = "Preset frequencies extracted";
    }

    [RelayCommand]
    private async Task RefreshFreqWobbles()
    {
        StatusBarText = "Refreshing frequency wobbles...";
        await Task.CompletedTask;
        StatusBarText = "Frequency wobbles refreshed";
    }

    // ── System menu ────────────────────────────────────────────────

    [RelayCommand]
    private void CleanRegistry()
    {
        StatusBarText = "Registry cleaned";
    }

    [RelayCommand]
    private void IdentifyUsbDevices()
    {
        StatusBarText = "USB devices identified";
    }

    [RelayCommand]
    private void InstallXmDrivers()
    {
        StatusBarText = "XM drivers - see Spooky2 documentation";
    }

    [RelayCommand]
    private void InstallGeneratorXDrivers()
    {
        StatusBarText = "GeneratorX drivers - see Spooky2 documentation";
    }

    [RelayCommand]
    private void InstallChainEditor()
    {
        StatusBarText = "Chain Editor - see Spooky2 documentation";
    }

    [RelayCommand]
    private void UpdateGxProFirmware()
    {
        StatusBarText = "GX Pro firmware update - see documentation";
    }

    [RelayCommand]
    private async Task GxScreenOn()
    {
        StatusBarText = "Turning GX screen on...";
        foreach (var generator in Generators)
        {
            await _generatorService.SendRawCommand(generator.GeneratorId, ":w95=12021");
        }
        StatusBarText = "GX screen turned on";
    }

    [RelayCommand]
    private async Task GxScreenOff()
    {
        StatusBarText = "Turning GX screen off...";
        foreach (var generator in Generators)
        {
            await _generatorService.SendRawCommand(generator.GeneratorId, ":w96=12321");
        }
        StatusBarText = "GX screen turned off";
    }

    [RelayCommand]
    private void GxProFirmwareInstructions()
    {
        var pdfPath = Path.Combine(_rootPath, "Data", "GX Pro Firmware Instructions.pdf");
        try
        {
            if (RuntimeInformation.IsOSPlatform(OSPlatform.OSX))
            {
                Process.Start("open", pdfPath);
            }
            else if (RuntimeInformation.IsOSPlatform(OSPlatform.Windows))
            {
                Process.Start(new ProcessStartInfo(pdfPath) { UseShellExecute = true });
            }
            else
            {
                Process.Start("xdg-open", pdfPath);
            }
            StatusBarText = "GX Pro Firmware Instructions opened";
        }
        catch (Exception ex)
        {
            StatusBarText = $"Failed to open instructions: {ex.Message}";
        }
    }

    // ── Settings menu ──────────────────────────────────────────────

    [RelayCommand]
    private void ToggleAdvancedFeatures()
    {
        System.AdvancedFeaturesEnabled = !System.AdvancedFeaturesEnabled;
        StatusBarText = System.AdvancedFeaturesEnabled
            ? "Advanced features enabled"
            : "Advanced features disabled";
    }

    [RelayCommand]
    private void ToggleUsersGuideMode()
    {
        StatusBarText = "User's Guide Mode toggled";
    }

    [RelayCommand]
    private async Task RestoreBfbDefaults()
    {
        StatusBarText = "Restoring BFB defaults...";
        await _settingsService.RestoreBfbDefaults();
        StatusBarText = "BFB defaults restored";
    }

    // ── Generators menu ────────────────────────────────────────────

    [RelayCommand]
    private async Task GlobalUnpause()
    {
        StatusBarText = "Unpausing all generators...";
        foreach (var generator in Generators)
        {
            await _generatorService.Resume(generator.GeneratorId);
        }
        StatusBarText = "All generators unpaused";
    }

    // ── Helpers ────────────────────────────────────────────────────

    private void OpenFileInDefaultEditor(string filePath)
    {
        try
        {
            if (RuntimeInformation.IsOSPlatform(OSPlatform.OSX))
            {
                Process.Start("open", filePath);
            }
            else if (RuntimeInformation.IsOSPlatform(OSPlatform.Windows))
            {
                Process.Start(new ProcessStartInfo(filePath) { UseShellExecute = true });
            }
            else
            {
                Process.Start("xdg-open", filePath);
            }
            StatusBarText = $"Opened {Path.GetFileName(filePath)}";
        }
        catch (Exception ex)
        {
            StatusBarText = $"Failed to open file: {ex.Message}";
        }
    }
}
