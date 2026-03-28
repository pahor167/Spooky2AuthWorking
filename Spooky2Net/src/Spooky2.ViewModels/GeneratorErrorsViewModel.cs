using System.Collections.ObjectModel;
using CommunityToolkit.Mvvm.ComponentModel;
using CommunityToolkit.Mvvm.Input;
using Spooky2.Core.Interfaces;

namespace Spooky2.ViewModels;

public partial class GeneratorErrorsViewModel : ObservableObject
{
    private readonly IFileService _fileService;
    private readonly string _rootPath;

    public GeneratorErrorsViewModel(IFileService fileService, string rootPath)
    {
        _fileService = fileService;
        _rootPath = rootPath;
    }

    public ObservableCollection<string> ErrorMessages { get; } = new();

    public void AddError(string message)
    {
        var timestamped = $"[{DateTime.Now:yyyy-MM-dd HH:mm:ss}] {message}";
        ErrorMessages.Add(timestamped);
    }

    [RelayCommand]
    private void ClearErrors()
    {
        ErrorMessages.Clear();
    }

    [RelayCommand]
    private async Task WriteErrorsToFile()
    {
        if (ErrorMessages.Count == 0)
            return;

        var filePath = Path.Combine(_rootPath, "Data", "GeneratorErrors.txt");
        var content = string.Join(Environment.NewLine, ErrorMessages);
        await _fileService.WriteAllText(filePath, content);
    }
}
