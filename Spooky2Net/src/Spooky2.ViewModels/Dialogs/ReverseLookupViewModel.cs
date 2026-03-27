using System.Text;
using CommunityToolkit.Mvvm.ComponentModel;
using CommunityToolkit.Mvvm.Input;
using Spooky2.Core.Interfaces;
using Spooky2.Core.Models;

namespace Spooky2.ViewModels.Dialogs;

public partial class ReverseLookupViewModel : ObservableObject, ICloseable
{
    private readonly IFileService? _fileService;
    private readonly Action? _closeAction;

    public Action? CloseAction { get; set; }

    [ObservableProperty]
    private string _headerText = "";

    [ObservableProperty]
    private string _resultsText = "";

    [ObservableProperty]
    private string _creditsText =
        "Friends of Spooky\r\n" +
        "This program is free software, brought to you by volunteers who believe in making " +
        "Rife encyclopaedia information freely available to all.";

    public ReverseLookupViewModel()
    {
    }

    public ReverseLookupViewModel(
        double frequency,
        ReverseLookupParameters parameters,
        IReadOnlyList<ReverseLookupResult> results,
        IFileService? fileService = null,
        Action? closeAction = null)
    {
        _fileService = fileService;
        _closeAction = closeAction;

        HeaderText = BuildHeader(frequency, parameters, results.Count);
        ResultsText = BuildResults(results);
    }

    [RelayCommand]
    private async Task Save()
    {
        var timestamp = DateTime.Now.ToString("yyyyMMdd_HHmmss");
        var fileName = $"ReverseLookup_{timestamp}.txt";

        // Save to user's Documents folder (cross-platform)
        var documentsPath = Environment.GetFolderPath(Environment.SpecialFolder.MyDocuments);
        var spookyDir = Path.Combine(documentsPath, "Spooky2");
        var filePath = Path.Combine(spookyDir, fileName);

        var content = new StringBuilder();
        content.AppendLine(HeaderText);
        content.AppendLine();
        content.AppendLine("PLEASE NOTE: This is NOT a diagnostic tool. Many health issues share the same frequencies. You do not necessarily have the conditions listed.");
        content.AppendLine();
        content.AppendLine(ResultsText);
        content.AppendLine();
        content.AppendLine(CreditsText);

        if (_fileService is not null)
        {
            _fileService.CreateDirectory(spookyDir);
            await _fileService.WriteAllText(filePath, content.ToString());
        }
        else
        {
            Directory.CreateDirectory(spookyDir);
            await File.WriteAllTextAsync(filePath, content.ToString());
        }

        (_closeAction ?? CloseAction)?.Invoke();
    }

    private static string BuildHeader(
        double frequency,
        ReverseLookupParameters parameters,
        int resultCount)
    {
        var sb = new StringBuilder();
        sb.AppendLine("Spooky2 Reverse Lookup Report");
        sb.AppendLine($"Frequency: {frequency:N2} Hz");
        sb.AppendLine($"Tolerance: {parameters.TolerancePercent}%");
        sb.AppendLine($"Harmonics: {(parameters.IncludeHarmonics ? "Yes" : "No")}");
        sb.AppendLine($"Sub-Harmonics: {(parameters.IncludeSubHarmonics ? "Yes" : "No")}");
        sb.AppendLine($"Max Harmonics: {parameters.MaxHarmonics}");
        sb.AppendLine($"Databases: {string.Join(", ", parameters.Databases)}");
        sb.AppendLine($"Results Found: {resultCount}");
        sb.Append($"Generated: {DateTime.Now:yyyy-MM-dd HH:mm:ss}");
        return sb.ToString();
    }

    private static string BuildResults(IReadOnlyList<ReverseLookupResult> results)
    {
        if (results.Count == 0)
            return "No matching programs found.";

        var sb = new StringBuilder();
        var grouped = results
            .GroupBy(r => r.SourceDatabase)
            .OrderBy(g => g.Key);

        foreach (var group in grouped)
        {
            sb.AppendLine($"=== {group.Key} ===");
            foreach (var result in group.OrderBy(r => r.ProgramName))
            {
                sb.AppendLine(
                    $"  {result.ProgramName} " +
                    $"({result.MatchType}: {result.MatchedFrequency:N2} Hz)");
            }

            sb.AppendLine();
        }

        return sb.ToString().TrimEnd();
    }
}
