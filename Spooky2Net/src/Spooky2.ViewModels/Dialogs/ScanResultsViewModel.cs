using System.Collections.ObjectModel;
using CommunityToolkit.Mvvm.ComponentModel;
using CommunityToolkit.Mvvm.Input;
using Spooky2.Core.Models;

namespace Spooky2.ViewModels.Dialogs;

public partial class ScanResultsViewModel : ObservableObject
{
    public ObservableCollection<ScanResult> Results { get; } = new();

    [ObservableProperty]
    private double[]? _graphFrequencies;

    [ObservableProperty]
    private double[]? _graphAmplitudes;

    [ObservableProperty]
    private double _graphMaxFrequency = 200_000;

    [RelayCommand]
    private void SelectAll()
    {
        // Stub: select all results
    }

    [RelayCommand]
    private void ClearSelection()
    {
        // Stub: clear selection
    }

    [RelayCommand]
    private void ClearHits()
    {
        Results.Clear();
    }

    [RelayCommand]
    private void CopyResults()
    {
        // Stub: copy results to clipboard
    }

    [RelayCommand]
    private void CreateProgram()
    {
        // Stub: create frequency program from selected results
    }

    [RelayCommand]
    private void ReverseLookup()
    {
        // Stub: reverse lookup selected frequencies
    }
}
