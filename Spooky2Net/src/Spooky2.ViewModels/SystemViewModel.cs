using System.Collections.ObjectModel;
using CommunityToolkit.Mvvm.ComponentModel;
using CommunityToolkit.Mvvm.Input;
using Microsoft.Extensions.Logging;
using Microsoft.Extensions.Logging.Abstractions;
using Spooky2.Core.Interfaces;
using Spooky2.Core.Models;

namespace Spooky2.ViewModels;

public partial class SystemViewModel : ObservableObject
{
    private readonly ISettingsService _settingsService;
    private readonly ILogger<SystemViewModel> _logger;

    public SystemViewModel(ISettingsService settingsService, ILogger<SystemViewModel>? logger = null)
    {
        _settingsService = settingsService;
        _logger = logger ?? NullLogger<SystemViewModel>.Instance;
        _logger.LogDebug("SystemViewModel initialized");
    }

    // ── Collections for UI bindings ──

    public ObservableCollection<string> Skins { get; } = new(["None", "Dark", "Light"]);

    public ObservableCollection<string> SoundPacks { get; } = new(["Paul", "Jill", "Silent"]);

    public ObservableCollection<string> BlacklistFrequencies { get; } = new();

    public ObservableCollection<string> ConnectedHardware { get; } = new();

    public ObservableCollection<WobbleWaveform> WobbleWaveforms { get; } =
        new(Enum.GetValues<WobbleWaveform>());

    // ── General Settings (Column 1) ──

    [ObservableProperty]
    private bool _displayPortNumber;

    [ObservableProperty]
    private bool _highCpuPriorityEnabled;

    [ObservableProperty]
    private bool _writeLogFile;

    [ObservableProperty]
    private bool _showDecimalTimeInSeconds;

    [ObservableProperty]
    private bool _alwaysOverwriteGenerator;

    [ObservableProperty]
    private bool _autoCloseControlPanel;

    [ObservableProperty]
    private bool _disableTooltips;

    [ObservableProperty]
    private bool _showProgramTotalRunTime = true;

    [ObservableProperty]
    private bool _enableContextualHelp = true;

    [ObservableProperty]
    private bool _preventHibernation = true;

    [ObservableProperty]
    private bool _displayGxCurrentAngle;

    [ObservableProperty]
    private bool _fastBfbModeEnabled;

    // ── General Settings (Column 2) ──

    [ObservableProperty]
    private bool _readSingleBfbVariable;

    [ObservableProperty]
    private bool _saveBfbWorkFiles = true;

    [ObservableProperty]
    private bool _pulseSoundDuringBfb;

    [ObservableProperty]
    private bool _pauseGeneratorsDuringBfb = true;

    [ObservableProperty]
    private bool _saveOnEveryCycle;

    [ObservableProperty]
    private bool _accumulateCycleHits;

    // ── Spooky Pulse Limits ──

    [ObservableProperty]
    private int _minimumBeatsPerMinute = 30;

    [ObservableProperty]
    private int _maximumBeatsPerMinute = 130;

    [ObservableProperty]
    private int _maximumHeartRateVariability = 30;

    // ── Skin / Font / Sounds ──

    [ObservableProperty]
    private string _selectedSkin = "None";

    [ObservableProperty]
    private int _fontSize = 11;

    [ObservableProperty]
    private string _selectedSoundPack = "Paul";

    // ── Frequency Blacklist ──

    [ObservableProperty]
    private string _blacklistFrequencyText = "";

    [ObservableProperty]
    private bool _blacklistOctaveEnabled;

    [ObservableProperty]
    private bool _blacklistDecadeEnabled;

    // ── Global Wobble ──

    [ObservableProperty]
    private WobbleWaveform _amplitudeWobbleWaveform = WobbleWaveform.Triangle;

    [ObservableProperty]
    private double _amplitudeWobblePercentage = 80;

    [ObservableProperty]
    private int _amplitudeWobbleSteps = 16;

    [ObservableProperty]
    private WobbleWaveform _frequencyWobbleWaveform = WobbleWaveform.Triangle;

    [ObservableProperty]
    private double _frequencyWobblePercentage = 0.025;

    [ObservableProperty]
    private int _frequencyWobbleSteps = 16;

    // Aliases so XAML bindings using the short name resolve correctly
    public double AmplitudeWobblePercent
    {
        get => AmplitudeWobblePercentage;
        set => AmplitudeWobblePercentage = value;
    }

    public double FrequencyWobblePercent
    {
        get => FrequencyWobblePercentage;
        set => FrequencyWobblePercentage = value;
    }

    partial void OnAmplitudeWobblePercentageChanged(double value)
        => OnPropertyChanged(nameof(AmplitudeWobblePercent));

    partial void OnFrequencyWobblePercentageChanged(double value)
        => OnPropertyChanged(nameof(FrequencyWobblePercent));

    // ── Frequency Conversions ──

    [ObservableProperty]
    private string _mwToHzFactor = "2.252E+23";

    [ObservableProperty]
    private string _mwInhibitFactor = "1.414";

    [ObservableProperty]
    private string _dnaToHzFactor = "1.555E+17";

    [ObservableProperty]
    private string _rnaToHzFactor = "1.618E+17";

    [ObservableProperty]
    private string _mrnaToHzFactor = "1.506E+17";

    [ObservableProperty]
    private string _tissueFactor = "2.831";

    // ── Active Databases ──

    [ObservableProperty]
    private bool _mainDatabaseEnabled = true;

    [ObservableProperty]
    private string _mainDatabasePath = "";

    [ObservableProperty]
    private bool _dnaDatabaseEnabled = true;

    [ObservableProperty]
    private string _dnaDatabasePath = "";

    [ObservableProperty]
    private bool _nonHumanDnaDatabaseEnabled = true;

    [ObservableProperty]
    private string _nonHumanDnaDatabasePath = "";

    [ObservableProperty]
    private bool _mwDatabaseEnabled = true;

    [ObservableProperty]
    private string _mwDatabasePath = "";

    [ObservableProperty]
    private bool _customDb1Enabled;

    [ObservableProperty]
    private string _customDb1Path = "";

    [ObservableProperty]
    private bool _customDb2Enabled;

    [ObservableProperty]
    private string _customDb2Path = "";

    [ObservableProperty]
    private bool _customDb3Enabled;

    [ObservableProperty]
    private string _customDb3Path = "";

    [ObservableProperty]
    private bool _customDb4Enabled;

    [ObservableProperty]
    private string _customDb4Path = "";

    [ObservableProperty]
    private bool _biofeedbackDatabaseEnabled;

    [ObservableProperty]
    private string _biofeedbackDatabasePath = "";

    [ObservableProperty]
    private bool _encyclopediaEnabled;

    [ObservableProperty]
    private string _encyclopediaPath = "";

    // ── Legacy compatibility properties ──

    // These properties are referenced by MainViewModel.ApplySettings
    [ObservableProperty]
    private bool _showTooltips = true;

    [ObservableProperty]
    private bool _autoLoadLastPreset;

    [ObservableProperty]
    private bool _confirmOnExit = true;

    [ObservableProperty]
    private bool _showFrequenciesInHz = true;

    [ObservableProperty]
    private bool _enableSoundNotifications;

    [ObservableProperty]
    private bool _enableEmailNotifications;

    [ObservableProperty]
    private bool _enableAutoStart;

    [ObservableProperty]
    private bool _enableAutoResume;

    [ObservableProperty]
    private bool _showSplashScreen = true;

    [ObservableProperty]
    private bool _minimizeToTray;

    [ObservableProperty]
    private bool _startMinimized;

    [ObservableProperty]
    private bool _checkForUpdates = true;

    [ObservableProperty]
    private bool _enableRemoteControl;

    [ObservableProperty]
    private bool _enableBfbAutoRun;

    [ObservableProperty]
    private bool _enableGradeScanning;

    [ObservableProperty]
    private bool _enableReversePhase;

    [ObservableProperty]
    private bool _enableDualOutput;

    [ObservableProperty]
    private bool _invertSync;

    [ObservableProperty]
    private bool _advancedFeaturesEnabled;

    // ── WIFI ──

    [ObservableProperty]
    private string _networkName = "";

    [ObservableProperty]
    private string _password = "";

    [ObservableProperty]
    private string _generatorIpPort = "";

    // ── Commands ──

    [RelayCommand]
    private async Task RestoreDefaults()
    {
        try
        {
            _logger.LogInformation("Restoring system defaults");
            await _settingsService.RestoreDefaults();
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Failed to restore system defaults");
        }
    }

    [RelayCommand]
    private async Task RestoreBfbDefaults()
    {
        try
        {
            _logger.LogInformation("Restoring BFB defaults");
            await _settingsService.RestoreBfbDefaults();
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Failed to restore BFB defaults");
        }
    }

    [RelayCommand]
    private async Task SaveSettings()
    {
        try
        {
            _logger.LogInformation("Saving settings");
            // Stub: gather settings into dictionary and save
            await Task.CompletedTask;
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Failed to save settings");
        }
    }

    [RelayCommand]
    private void ResetMwFactors()
    {
        MwToHzFactor = "2.252E+23";
        MwInhibitFactor = "1.414";
        DnaToHzFactor = "1.555E+17";
        RnaToHzFactor = "1.618E+17";
        MrnaToHzFactor = "1.506E+17";
        TissueFactor = "2.831";
    }

    [RelayCommand]
    private void AddBlacklistFrequency()
    {
        // Stub
    }

    [RelayCommand]
    private void ClearBlacklist()
    {
        // Stub
    }

    [RelayCommand]
    private void AddGeneratorIp()
    {
        // Stub
    }
}
