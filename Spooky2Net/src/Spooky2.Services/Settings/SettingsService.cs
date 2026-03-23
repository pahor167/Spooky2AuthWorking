using Microsoft.Extensions.Logging;
using Microsoft.Extensions.Logging.Abstractions;
using Spooky2.Core.Interfaces;

namespace Spooky2.Services.Settings;

/// <summary>
/// Settings service that handles key=value text config files.
/// Supports loading, saving, and restoring defaults for system and BFB settings.
/// </summary>
public sealed class SettingsService : ISettingsService
{
    private readonly string _rootPath;
    private readonly ILogger<SettingsService> _logger;

    public SettingsService(ILogger<SettingsService> logger)
        : this(".", logger)
    {
    }

    public SettingsService(string rootPath = ".", ILogger<SettingsService>? logger = null)
    {
        _rootPath = rootPath;
        _logger = logger ?? NullLogger<SettingsService>.Instance;
        _logger.LogDebug("SettingsService initialized with root path '{RootPath}'", _rootPath);
    }

    public async Task<Dictionary<string, string>> LoadSettings(string configFile)
    {
        _logger.LogDebug("Loading settings from '{ConfigFile}'", configFile);
        var settings = new Dictionary<string, string>(StringComparer.OrdinalIgnoreCase);

        if (!File.Exists(configFile))
        {
            _logger.LogWarning("Settings file not found: '{ConfigFile}'", configFile);
            return settings;
        }

        var lines = await File.ReadAllLinesAsync(configFile);

        foreach (string line in lines)
        {
            string trimmed = line.Trim();

            // Skip empty lines and comments
            if (string.IsNullOrEmpty(trimmed) || trimmed.StartsWith('#') || trimmed.StartsWith(';'))
                continue;

            int separatorIndex = trimmed.IndexOf('=');
            if (separatorIndex <= 0)
                continue;

            string key = trimmed[..separatorIndex].Trim();
            string value = trimmed[(separatorIndex + 1)..].Trim();

            settings[key] = value;
        }

        _logger.LogInformation("Loaded {Count} settings from '{ConfigFile}'", settings.Count, configFile);
        return settings;
    }

    public async Task SaveSettings(string configFile, Dictionary<string, string> settings)
    {
        _logger.LogDebug("Saving {Count} settings to '{ConfigFile}'", settings.Count, configFile);
        string? directory = Path.GetDirectoryName(configFile);
        if (directory is not null && !Directory.Exists(directory))
        {
            Directory.CreateDirectory(directory);
        }

        var lines = new List<string>(settings.Count);
        foreach (var kvp in settings.OrderBy(kvp => kvp.Key, StringComparer.OrdinalIgnoreCase))
        {
            lines.Add($"{kvp.Key}={kvp.Value}");
        }

        await File.WriteAllLinesAsync(configFile, lines);
        _logger.LogInformation("Settings saved to '{ConfigFile}'", configFile);
    }

    public async Task RestoreDefaults()
    {
        _logger.LogInformation("Restoring default system settings");
        var defaults = new Dictionary<string, string>
        {
            ["ShowTooltips"] = "True",
            ["PreventHibernation"] = "True",
            ["WriteLogFile"] = "False",
            ["CPUPriority"] = "Normal",
            ["DefaultDwell"] = "180",
            ["FrequencyMultiplier"] = "1",
            ["DwellMultiplier"] = "1",
            ["RepeatSequence"] = "1",
            ["RepeatProgram"] = "1",
            ["RepeatChain"] = "1",
            ["RemoveDuplicates"] = "False",
            ["AmplitudeWobble"] = "0",
            ["FrequencyWobble"] = "0",
            ["AmplitudeWobbleWaveform"] = "Sine",
            ["FrequencyWobbleWaveform"] = "Sine",
            ["GateType"] = "Timed",
            ["GateOnMs"] = "0",
            ["GateOffMs"] = "0",
            ["MinBPM"] = "30",
            ["MaxBPM"] = "130",
            ["MaxHRV"] = "30",
            ["OutputMode"] = "MN",
            ["SkinIndex"] = "0",
            ["SoundScheme"] = "Silent",
            ["WifiEnabled"] = "False",
            ["USBDebug"] = "False",
            ["AutoStartGenerators"] = "False",
            ["AdvancedFeatures"] = "False"
        };

        string configPath = Path.Combine(_rootPath, "Data", "SystemCFG.txt");
        await SaveSettings(configPath, defaults);
    }

    public async Task RestoreBfbDefaults()
    {
        _logger.LogInformation("Restoring default BFB settings");
        var defaults = new Dictionary<string, string>
        {
            ["BFB_ScanType"] = "Range",
            ["BFB_StartFreq"] = "76000",
            ["BFB_EndFreq"] = "152000",
            ["BFB_StepSize"] = "0.025",
            ["BFB_DwellMs"] = "300",
            ["BFB_Sensitivity"] = "5",
            ["BFB_MaxHits"] = "20",
            ["BFB_RepeatCount"] = "1",
            ["BFB_UseHarmonics"] = "True",
            ["BFB_UseSubharmonics"] = "False"
        };

        string configPath = Path.Combine(_rootPath, "Data", "BFBCFG.txt");
        await SaveSettings(configPath, defaults);
    }
}
