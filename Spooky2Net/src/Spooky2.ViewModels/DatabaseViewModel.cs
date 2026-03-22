using System.Collections.ObjectModel;
using CommunityToolkit.Mvvm.ComponentModel;
using CommunityToolkit.Mvvm.Input;
using Spooky2.Core.Interfaces;

namespace Spooky2.ViewModels;

public partial class DatabaseViewModel : ObservableObject
{
    private readonly IDatabaseService _databaseService;
    private readonly IMicroGenService _microGenService;

    [ObservableProperty]
    private string _selectedMicroGenPort = "";

    public DatabaseViewModel(IDatabaseService databaseService, IMicroGenService microGenService)
    {
        _databaseService = databaseService;
        _microGenService = microGenService;
    }

    [ObservableProperty]
    private string _manualFrequencyText = "";

    [ObservableProperty]
    private int _dwellSeconds = 180;

    // VB6 original: UseRife
    [ObservableProperty]
    private bool _useRifeDatabase = true;

    // CAFL = Consolidated Annotated Frequency List
    [ObservableProperty]
    private bool _useCaflDatabase = true;

    // XTRA = Extra/supplementary frequencies
    [ObservableProperty]
    private bool _useExtraDatabase = true;

    // VB6 original: UseCust
    [ObservableProperty]
    private bool _useCustomDatabase = true;

    // VB6 original: UseDna
    [ObservableProperty]
    private bool _useDnaDatabase = true;

    // HC = Healing Codes database
    [ObservableProperty]
    private bool _useHealingCodesDatabase = true;

    // VB6 original: UseAlt
    [ObservableProperty]
    private bool _useAlternativeDatabase = true;

    // VB6 original: UseBio
    [ObservableProperty]
    private bool _useBiologicalDatabase = true;

    // PROV = Provisional/unverified
    [ObservableProperty]
    private bool _useProvisionalDatabase = true;

    // MW = Molecular Weight
    [ObservableProperty]
    private bool _useMolecularWeightDatabase = true;

    // VB6 original: UseVega
    [ObservableProperty]
    private bool _useVegaDatabase = true;

    // ETDFL = Extended Target Disease Frequency List
    [ObservableProperty]
    private bool _useEtdflDatabase = true;

    // BFB = Biofeedback
    [ObservableProperty]
    private bool _useBiofeedbackDatabase = true;

    // KHZ = Kilohertz range frequencies
    [ObservableProperty]
    private bool _useKilohertzDatabase = true;

    // SD = Spooky Database
    [ObservableProperty]
    private bool _useSdDatabase = true;

    // RUSS = Russian frequency research
    [ObservableProperty]
    private bool _useRussianDatabase = true;

    // RRM = Remote Rife Machine frequencies
    [ObservableProperty]
    private bool _useRrmDatabase = true;

    public ObservableCollection<string> FrequencyList { get; } = new();

    [RelayCommand]
    private async Task LoadFrequencies()
    {
        // Stub: load frequencies from selected databases
        await Task.CompletedTask;
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
        FrequencyList.Clear();
    }

    [RelayCommand]
    private async Task SearchDatabase()
    {
        var enabledDatabases = GetEnabledDatabases();
        var results = await _databaseService.SearchDatabase(ManualFrequencyText, enabledDatabases);
        FrequencyList.Clear();
        foreach (var entry in results)
        {
            FrequencyList.Add($"{entry.Name}: {string.Join(", ", entry.Frequencies)}");
        }
    }

    [RelayCommand]
    private async Task SendToMicroGenLowPower()
    {
        var frequencies = ParseFrequencyList();
        if (frequencies.Count == 0 || string.IsNullOrWhiteSpace(SelectedMicroGenPort)) return;
        await _microGenService.SendToLowPower(SelectedMicroGenPort, frequencies, DwellSeconds);
    }

    [RelayCommand]
    private async Task SendToMicroGenHighPower()
    {
        var frequencies = ParseFrequencyList();
        if (frequencies.Count == 0 || string.IsNullOrWhiteSpace(SelectedMicroGenPort)) return;
        await _microGenService.SendToHighPower(SelectedMicroGenPort, frequencies, DwellSeconds);
    }

    [RelayCommand]
    private async Task SendToMicroGenZapper()
    {
        var frequencies = ParseFrequencyList();
        if (frequencies.Count == 0 || string.IsNullOrWhiteSpace(SelectedMicroGenPort)) return;
        await _microGenService.SendToZapper(SelectedMicroGenPort, frequencies, DwellSeconds);
    }

    [RelayCommand]
    private async Task SendToMicroGenBloodPurifier()
    {
        var frequencies = ParseFrequencyList();
        if (frequencies.Count == 0 || string.IsNullOrWhiteSpace(SelectedMicroGenPort)) return;
        await _microGenService.SendToBloodPurifier(SelectedMicroGenPort, frequencies, DwellSeconds);
    }

    private List<double> ParseFrequencyList()
    {
        var result = new List<double>();
        foreach (var entry in FrequencyList)
        {
            // Entries may be in format "Name: freq1, freq2" or just raw numbers
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
