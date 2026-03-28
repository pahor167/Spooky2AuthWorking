using System.Globalization;
using CommunityToolkit.Mvvm.ComponentModel;
using CommunityToolkit.Mvvm.Input;
using Spooky2.Core.Interfaces;

namespace Spooky2.ViewModels.Dialogs;

public partial class CreateProgramViewModel : ObservableObject, ICloseable
{
    private readonly IFileService? _fileService;
    private readonly string _rootPath;

    public Action? CloseAction { get; set; }

    [ObservableProperty]
    private string _programName = "";

    [ObservableProperty]
    private string _frequencyText = "";

    [ObservableProperty]
    private string _frequencySource = "";

    [ObservableProperty]
    private string _notes = "";

    [ObservableProperty]
    private int _defaultDwell = 180;

    public CreateProgramViewModel()
        : this(null, Directory.GetCurrentDirectory())
    {
    }

    public CreateProgramViewModel(IFileService? fileService, string rootPath)
    {
        _fileService = fileService;
        _rootPath = rootPath;
    }

    [RelayCommand]
    private async Task SaveProgram()
    {
        if (string.IsNullOrWhiteSpace(ProgramName) || string.IsNullOrWhiteSpace(FrequencyText))
            return;

        var frequencies = ParseFrequencies(FrequencyText);
        if (frequencies.Count == 0)
            return;

        var freqCsv = string.Join(",", frequencies.Select(f => f.ToString("G", CultureInfo.InvariantCulture)));
        var csvLine = $"{ProgramName},{freqCsv}{Environment.NewLine}";

        var filePath = Path.Combine(_rootPath, "Custom.csv");

        if (_fileService is not null)
        {
            await _fileService.AppendText(filePath, csvLine);
        }
        else
        {
            await File.AppendAllTextAsync(filePath, csvLine);
        }

        CloseAction?.Invoke();
    }

    private static List<double> ParseFrequencies(string text)
    {
        var frequencies = new List<double>();
        var parts = text.Split([',', '\n', '\r'], StringSplitOptions.RemoveEmptyEntries);
        foreach (var part in parts)
        {
            if (double.TryParse(part.Trim(), NumberStyles.Float, CultureInfo.InvariantCulture, out var freq) && freq > 0)
                frequencies.Add(freq);
        }
        return frequencies;
    }
}
