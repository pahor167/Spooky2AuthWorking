using System.Collections.ObjectModel;
using CommunityToolkit.Mvvm.ComponentModel;
using CommunityToolkit.Mvvm.Input;
using Microsoft.Extensions.Logging;
using Microsoft.Extensions.Logging.Abstractions;
using Spooky2.Core.Interfaces;
using Spooky2.Core.Models;

namespace Spooky2.ViewModels;

public partial class DatabaseViewModel : ObservableObject
{
    private readonly IDatabaseService _databaseService;
    private readonly IMicroGenService _microGenService;
    private readonly ILogger<DatabaseViewModel> _logger;

    /// <summary>All entries loaded from all enabled databases (decrypted once, kept in memory).</summary>
    private List<DatabaseEntry> _allEntries = [];

    [ObservableProperty]
    private string _selectedMicroGenPort = "";

    [ObservableProperty]
    private string _manualFrequencyText = "";

    [ObservableProperty]
    private int _dwellSeconds = 180;

    [ObservableProperty]
    private bool _isLoading;

    [ObservableProperty]
    private string _statusText = "";

    // Database selection flags (all on by default)
    [ObservableProperty] private bool _useRifeDatabase = true;
    [ObservableProperty] private bool _useCaflDatabase = true;
    [ObservableProperty] private bool _useExtraDatabase = true;
    [ObservableProperty] private bool _useCustomDatabase = true;
    [ObservableProperty] private bool _useDnaDatabase = true;
    [ObservableProperty] private bool _useHealingCodesDatabase = true;
    [ObservableProperty] private bool _useAlternativeDatabase = true;
    [ObservableProperty] private bool _useBiologicalDatabase = true;
    [ObservableProperty] private bool _useProvisionalDatabase = true;
    [ObservableProperty] private bool _useMolecularWeightDatabase = true;
    [ObservableProperty] private bool _useVegaDatabase = true;
    [ObservableProperty] private bool _useEtdflDatabase = true;
    [ObservableProperty] private bool _useBiofeedbackDatabase = true;
    [ObservableProperty] private bool _useKilohertzDatabase = true;
    [ObservableProperty] private bool _useSdDatabase = true;
    [ObservableProperty] private bool _useRussianDatabase = true;
    [ObservableProperty] private bool _useRrmDatabase = true;

    // Loaded Programs panel
    public ObservableCollection<string> LoadedPrograms { get; } = new();

    [ObservableProperty]
    private string? _selectedLoadedProgram;

    [ObservableProperty]
    private string? _selectedFrequencyItem;

    // Options panel
    [ObservableProperty] private int _repeatEachFrequency = 1;
    [ObservableProperty] private int _repeatEachProgram = 1;
    [ObservableProperty] private int _repeatSequence = 1;
    [ObservableProperty] private int _repeatChain = 1;
    [ObservableProperty] private double _dwellMultiplier = 1;
    [ObservableProperty] private double _frequencyMultiplier = 1;

    // Options panel: run/frequency options
    [ObservableProperty]
    private bool _runEnabled;

    [ObservableProperty]
    private double _out1Hz;

    [ObservableProperty]
    private double _out2Hz;

    [ObservableProperty]
    private bool _removeDuplicateFrequencies;

    [ObservableProperty]
    private double _duplicateTolerancePercent;

    [ObservableProperty]
    private bool _applyInhibitFactor;

    [ObservableProperty]
    private bool _applyTissueFactor;

    [ObservableProperty]
    private double _addHz;

    [ObservableProperty]
    private string _estimatedRunTime = "Estimated Total Run Time 00:00:00";

    public ObservableCollection<string> FrequencyList { get; } = new();

    public DatabaseViewModel(IDatabaseService databaseService, IMicroGenService microGenService, ILogger<DatabaseViewModel>? logger = null)
    {
        _databaseService = databaseService;
        _microGenService = microGenService;
        _logger = logger ?? NullLogger<DatabaseViewModel>.Instance;

        // Load all databases at startup (fire-and-forget, shows loading indicator)
        _ = LoadAllDatabasesAsync();
    }

    /// <summary>
    /// Loads and decrypts all enabled databases once at startup.
    /// Populates the full list so the user sees all entries immediately.
    /// </summary>
    private async Task LoadAllDatabasesAsync()
    {
        try
        {
            IsLoading = true;
            StatusText = "Decrypting frequency database...";
            _logger.LogInformation("Loading all databases at startup");

            var enabledDatabases = GetEnabledDatabases();
            var allEntries = new List<DatabaseEntry>();

            foreach (var db in enabledDatabases)
            {
                try
                {
                    var entries = await Task.Run(() => _databaseService.LoadDatabase(db));
                    allEntries.AddRange(entries);
                    StatusText = $"Loading... {allEntries.Count:N0} programs";
                }
                catch (Exception ex)
                {
                    _logger.LogError(ex, "Failed to load database '{Database}'", db);
                }
            }

            _allEntries = allEntries;
            _logger.LogInformation("All databases loaded: {Count} total entries", _allEntries.Count);

            // Show all entries
            ApplyFilter("");
            StatusText = $"{_allEntries.Count:N0} programs loaded";
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Failed to load databases at startup");
            StatusText = "Failed to load databases";
        }
        finally
        {
            IsLoading = false;
        }
    }

    /// <summary>Filters the in-memory list instantly. Empty query = show all.</summary>
    private void ApplyFilter(string query)
    {
        FrequencyList.Clear();

        IEnumerable<DatabaseEntry> filtered = _allEntries;
        if (!string.IsNullOrWhiteSpace(query))
        {
            filtered = _allEntries.Where(e => e.Name.Contains(query, StringComparison.OrdinalIgnoreCase));
        }

        foreach (var entry in filtered)
        {
            var freqs = string.Join(", ", entry.Frequencies);
            FrequencyList.Add($"{entry.Name} [{entry.Category}]: {freqs}");
        }

        StatusText = string.IsNullOrWhiteSpace(query)
            ? $"{FrequencyList.Count:N0} programs"
            : $"{FrequencyList.Count:N0} / {_allEntries.Count:N0} programs matching \"{query}\"";
    }

    [RelayCommand]
    private void AddManualFrequency()
    {
        if (!string.IsNullOrWhiteSpace(ManualFrequencyText))
        {
            FrequencyList.Add(ManualFrequencyText);
            ManualFrequencyText = "";
        }
    }

    [RelayCommand]
    private void ClearFrequencies()
    {
        ManualFrequencyText = "";
        ApplyFilter("");
    }

    [RelayCommand]
    private Task SearchDatabase()
    {
        _logger.LogDebug("Search: '{Query}'", ManualFrequencyText);
        ApplyFilter(ManualFrequencyText);
        return Task.CompletedTask;
    }

    [RelayCommand]
    private async Task SendToMicroGenLowPower()
    {
        try
        {
            var frequencies = ParseFrequencyList();
            if (frequencies.Count == 0 || string.IsNullOrWhiteSpace(SelectedMicroGenPort)) return;
            _logger.LogInformation("Sending {Count} frequencies to MicroGen LowPower on port {Port}", frequencies.Count, SelectedMicroGenPort);
            await _microGenService.SendToLowPower(SelectedMicroGenPort, frequencies, DwellSeconds);
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Failed to send to MicroGen LowPower");
        }
    }

    [RelayCommand]
    private async Task SendToMicroGenHighPower()
    {
        try
        {
            var frequencies = ParseFrequencyList();
            if (frequencies.Count == 0 || string.IsNullOrWhiteSpace(SelectedMicroGenPort)) return;
            await _microGenService.SendToHighPower(SelectedMicroGenPort, frequencies, DwellSeconds);
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Failed to send to MicroGen HighPower");
        }
    }

    [RelayCommand]
    private async Task SendToMicroGenZapper()
    {
        try
        {
            var frequencies = ParseFrequencyList();
            if (frequencies.Count == 0 || string.IsNullOrWhiteSpace(SelectedMicroGenPort)) return;
            await _microGenService.SendToZapper(SelectedMicroGenPort, frequencies, DwellSeconds);
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Failed to send to MicroGen Zapper");
        }
    }

    [RelayCommand]
    private async Task SendToMicroGenBloodPurifier()
    {
        try
        {
            var frequencies = ParseFrequencyList();
            if (frequencies.Count == 0 || string.IsNullOrWhiteSpace(SelectedMicroGenPort)) return;
            await _microGenService.SendToBloodPurifier(SelectedMicroGenPort, frequencies, DwellSeconds);
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Failed to send to MicroGen BloodPurifier");
        }
    }

    [RelayCommand]
    private void RemoveLoadedProgram()
    {
        if (SelectedLoadedProgram is not null && LoadedPrograms.Remove(SelectedLoadedProgram))
        {
            SelectedLoadedProgram = null;
        }
    }

    [RelayCommand]
    private void SaveLoadedPrograms()
    {
        _logger.LogInformation("SaveLoadedPrograms: {Count} programs (stub)", LoadedPrograms.Count);
    }

    [RelayCommand]
    private void SaveLoadedProgramsAs()
    {
        _logger.LogInformation("SaveLoadedProgramsAs: {Count} programs (stub)", LoadedPrograms.Count);
    }

    [RelayCommand]
    private void MoveLoadedProgramUp()
    {
        if (SelectedLoadedProgram is null) return;
        var index = LoadedPrograms.IndexOf(SelectedLoadedProgram);
        if (index > 0)
        {
            LoadedPrograms.Move(index, index - 1);
        }
    }

    [RelayCommand]
    private void MoveLoadedProgramTop()
    {
        if (SelectedLoadedProgram is null) return;
        var index = LoadedPrograms.IndexOf(SelectedLoadedProgram);
        if (index > 0)
        {
            LoadedPrograms.Move(index, 0);
        }
    }

    [RelayCommand]
    private void MoveLoadedProgramDown()
    {
        if (SelectedLoadedProgram is null) return;
        var index = LoadedPrograms.IndexOf(SelectedLoadedProgram);
        if (index >= 0 && index < LoadedPrograms.Count - 1)
        {
            LoadedPrograms.Move(index, index + 1);
        }
    }

    [RelayCommand]
    private void SearchInResults()
    {
        if (string.IsNullOrWhiteSpace(ManualFrequencyText)) return;
        var query = ManualFrequencyText;
        var filtered = FrequencyList
            .Where(item => item.Contains(query, StringComparison.OrdinalIgnoreCase))
            .ToList();
        FrequencyList.Clear();
        foreach (var item in filtered)
        {
            FrequencyList.Add(item);
        }
        StatusText = $"{FrequencyList.Count:N0} results matching \"{query}\" in current list";
    }

    private List<double> ParseFrequencyList()
    {
        var result = new List<double>();
        foreach (var entry in FrequencyList)
        {
            var colonIndex = entry.IndexOf(':');
            var freqPart = colonIndex >= 0 ? entry[(colonIndex + 1)..] : entry;
            foreach (var token in freqPart.Split(',', StringSplitOptions.RemoveEmptyEntries | StringSplitOptions.TrimEntries))
            {
                if (double.TryParse(token, System.Globalization.NumberStyles.Float,
                    System.Globalization.CultureInfo.InvariantCulture, out var freq))
                {
                    result.Add(freq);
                }
            }
        }
        return result;
    }

    private List<string> GetEnabledDatabases()
    {
        var databases = new List<string>();
        if (UseRifeDatabase) databases.Add("Rife");
        if (UseCaflDatabase) databases.Add("CAFL");
        if (UseExtraDatabase) databases.Add("XTRA");
        if (UseCustomDatabase) databases.Add("CUST");
        if (UseDnaDatabase) databases.Add("DNA");
        if (UseHealingCodesDatabase) databases.Add("HC");
        if (UseAlternativeDatabase) databases.Add("ALT");
        if (UseBiologicalDatabase) databases.Add("BIO");
        if (UseProvisionalDatabase) databases.Add("PROV");
        if (UseMolecularWeightDatabase) databases.Add("MW");
        if (UseVegaDatabase) databases.Add("VEGA");
        if (UseEtdflDatabase) databases.Add("ETDFL");
        if (UseBiofeedbackDatabase) databases.Add("BFB");
        if (UseKilohertzDatabase) databases.Add("KHZ");
        if (UseSdDatabase) databases.Add("SD");
        if (UseRussianDatabase) databases.Add("RUSS");
        if (UseRrmDatabase) databases.Add("RRM");
        return databases;
    }
}
