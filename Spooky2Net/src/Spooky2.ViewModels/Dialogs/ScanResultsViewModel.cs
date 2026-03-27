using System.Collections.ObjectModel;
using System.Globalization;
using CommunityToolkit.Mvvm.ComponentModel;
using CommunityToolkit.Mvvm.Input;
using Microsoft.Extensions.Logging;
using Microsoft.Extensions.Logging.Abstractions;
using Spooky2.Core.Interfaces;
using Spooky2.Core.Models;

namespace Spooky2.ViewModels.Dialogs;

public partial class ScanResultsViewModel : ObservableObject
{
    private readonly IScanService _scanService;
    private readonly IDatabaseService _databaseService;
    private readonly ILogger<ScanResultsViewModel> _logger;

    public ScanResultsViewModel(
        IScanService scanService,
        IDatabaseService databaseService,
        ILogger<ScanResultsViewModel>? logger = null)
    {
        _scanService = scanService;
        _databaseService = databaseService;
        _logger = logger ?? NullLogger<ScanResultsViewModel>.Instance;

        DatabaseOptions = new ObservableCollection<string>(
            ["All Databases", "Rife", "CAFL", "XTRA", "BIO", "RUSS"]);
        SelectedDatabase = "All Databases";
    }

    public ObservableCollection<ScanResult> ScanResults { get; } = new();

    public ObservableCollection<ScanResult> SelectedResults { get; } = new();

    public ObservableCollection<string> ReverseLookupResults { get; } = new();

    public ObservableCollection<string> DatabaseOptions { get; }

    [ObservableProperty]
    private string _selectedDatabase = "All Databases";

    [ObservableProperty]
    private double[]? _graphFrequencies;

    [ObservableProperty]
    private double[]? _graphAmplitudes;

    [ObservableProperty]
    private double _graphMaxFrequency = 200_000;

    [ObservableProperty]
    private bool _includeHarmonics = true;

    [ObservableProperty]
    private bool _includeSubHarmonics = true;

    [ObservableProperty]
    private string _reverseLookupTolerance = "0.25";

    [ObservableProperty]
    private string _harmonicFactor = "2";

    [RelayCommand]
    private void SelectAll()
    {
        SelectedResults.Clear();
        foreach (var result in ScanResults)
        {
            SelectedResults.Add(result);
        }
    }

    [RelayCommand]
    private void ClearSelection()
    {
        SelectedResults.Clear();
    }

    [RelayCommand]
    private void ClearHits()
    {
        ScanResults.Clear();
        SelectedResults.Clear();
        ReverseLookupResults.Clear();
    }

    [RelayCommand]
    private void Copy()
    {
        // Stub: copy results to clipboard
    }

    [RelayCommand]
    private void CreateProgram()
    {
        // Stub: create frequency program from selected results
    }

    [RelayCommand]
    private async Task ReverseLookup()
    {
        var selected = SelectedResults.ToList();
        if (selected.Count == 0)
        {
            _logger.LogWarning("ReverseLookup: no scan results selected");
            return;
        }

        if (!double.TryParse(ReverseLookupTolerance, NumberStyles.Float,
                CultureInfo.InvariantCulture, out var tolerance))
        {
            tolerance = 0.25;
        }

        if (!int.TryParse(HarmonicFactor, NumberStyles.Integer,
                CultureInfo.InvariantCulture, out var factor))
        {
            factor = 2;
        }

        var databases = SelectedDatabase == "All Databases"
            ? new List<string> { "Rife", "CAFL", "XTRA", "BIO", "RUSS" }
            : new List<string> { SelectedDatabase };

        var parameters = new ReverseLookupParameters
        {
            IncludeHarmonics = IncludeHarmonics,
            IncludeSubHarmonics = IncludeSubHarmonics,
            TolerancePercent = tolerance,
            MaxHarmonics = factor,
            Databases = databases
        };

        ReverseLookupResults.Clear();

        try
        {
            foreach (var scanResult in selected)
            {
                var frequency = scanResult.Frequency;
                _logger.LogInformation("Starting reverse lookup for {Freq} Hz", frequency);

                var results = await Task.Run(() =>
                    _scanService.ReverseLookup(frequency, parameters, _databaseService));

                foreach (var result in results)
                {
                    ReverseLookupResults.Add(
                        $"{result.ProgramName} [{result.SourceDatabase}] " +
                        $"({result.MatchType}: {result.MatchedFrequency:N2} Hz)");
                }

                _logger.LogInformation(
                    "Reverse lookup complete: {Count} results for {Freq} Hz",
                    results.Count, frequency);
            }

            _logger.LogInformation(
                "Reverse lookup finished: {Total} total results for {Count} frequencies",
                ReverseLookupResults.Count, selected.Count);
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Reverse lookup failed");
        }
    }
}
