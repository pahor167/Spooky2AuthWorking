using Spooky2.Core.Interfaces;
using Spooky2.Core.Models;

namespace Spooky2.Services.Presets;

/// <summary>
/// Preset management service.
/// Presets are text files containing key=value settings and frequency data.
/// Preset chains are text files listing preset paths in order.
/// </summary>
public sealed class PresetService : IPresetService
{
    private readonly IFileService _fileService;

    public PresetService(IFileService fileService)
    {
        _fileService = fileService;
    }

    public async Task<Preset> LoadPreset(string path)
    {
        var content = await _fileService.ReadAllText(path);
        var lines = content.Split(['\r', '\n'], StringSplitOptions.RemoveEmptyEntries);

        var settings = new Dictionary<string, string>();
        var frequencies = new List<double>();
        var programName = Path.GetFileNameWithoutExtension(path);

        foreach (var line in lines)
        {
            var trimmed = line.Trim();
            if (string.IsNullOrEmpty(trimmed))
            {
                continue;
            }

            var equalsIndex = trimmed.IndexOf('=');
            if (equalsIndex > 0)
            {
                var key = trimmed[..equalsIndex].Trim();
                var value = trimmed[(equalsIndex + 1)..].Trim();
                settings[key] = value;
            }
            else
            {
                // Try parsing as frequency data (comma-separated or single value per line)
                var parts = trimmed.Split(',', StringSplitOptions.RemoveEmptyEntries | StringSplitOptions.TrimEntries);
                foreach (var part in parts)
                {
                    if (double.TryParse(part, System.Globalization.NumberStyles.Float,
                            System.Globalization.CultureInfo.InvariantCulture, out var freq))
                    {
                        frequencies.Add(freq);
                    }
                }
            }
        }

        var programs = frequencies.Count > 0
            ? new List<FrequencyProgram>
            {
                new()
                {
                    Name = programName,
                    Frequencies = frequencies
                }
            }
            : new List<FrequencyProgram>();

        return new Preset
        {
            Name = programName,
            Programs = programs,
            Settings = settings,
            FilePath = path
        };
    }

    public async Task SavePreset(Preset preset, string path)
    {
        var lines = new List<string>();

        // Write settings as key=value pairs
        foreach (var kvp in preset.Settings)
        {
            lines.Add($"{kvp.Key}={kvp.Value}");
        }

        // Write frequency programs
        foreach (var program in preset.Programs)
        {
            if (program.Frequencies.Count > 0)
            {
                var freqLine = string.Join(",",
                    program.Frequencies.Select(f =>
                        f.ToString(System.Globalization.CultureInfo.InvariantCulture)));
                lines.Add(freqLine);
            }
        }

        var content = string.Join(Environment.NewLine, lines);
        await _fileService.WriteAllText(path, content);
    }

    public Task DeletePreset(string path)
    {
        if (_fileService.IsDirectory(path))
        {
            _fileService.DeleteDirectory(path, recursive: true);
        }
        else if (_fileService.Exists(path))
        {
            _fileService.DeleteFile(path);
        }

        return Task.CompletedTask;
    }

    public Task<List<string>> SearchPresets(string searchText, string directory)
    {
        var results = new List<string>();
        SearchDirectory(directory, searchText, results);
        return Task.FromResult(results);
    }

    private void SearchDirectory(string directory, string searchText, List<string> results)
    {
        var txtFiles = _fileService.GetFiles(directory, "*.txt");
        var s2dFiles = _fileService.GetFiles(directory, "*.s2d");

        var allFiles = txtFiles.Concat(s2dFiles);

        foreach (var file in allFiles)
        {
            var fileName = Path.GetFileName(file);
            if (fileName.Contains(searchText, StringComparison.OrdinalIgnoreCase))
            {
                results.Add(file);
            }
        }

        var subdirectories = _fileService.GetDirectories(directory);
        foreach (var subdir in subdirectories)
        {
            SearchDirectory(subdir, searchText, results);
        }
    }

    public async Task<PresetChain> LoadPresetChain(string path)
    {
        var content = await _fileService.ReadAllText(path);
        var lines = content.Split(['\r', '\n'], StringSplitOptions.RemoveEmptyEntries);

        if (lines.Length == 0)
        {
            return new PresetChain
            {
                Name = Path.GetFileNameWithoutExtension(path)
            };
        }

        var chainName = lines[0].Trim();
        var presets = new List<Preset>();

        for (var i = 1; i < lines.Length; i++)
        {
            var presetPath = lines[i].Trim();
            if (string.IsNullOrEmpty(presetPath))
            {
                continue;
            }

            if (_fileService.Exists(presetPath))
            {
                var preset = await LoadPreset(presetPath);
                presets.Add(preset);
            }
            else
            {
                // Add a placeholder preset for missing files
                presets.Add(new Preset
                {
                    Name = Path.GetFileNameWithoutExtension(presetPath),
                    FilePath = presetPath
                });
            }
        }

        return new PresetChain
        {
            Name = chainName,
            Presets = presets
        };
    }

    public async Task SavePresetChain(PresetChain chain, string path)
    {
        var lines = new List<string> { chain.Name };

        foreach (var preset in chain.Presets)
        {
            lines.Add(preset.FilePath);
        }

        var content = string.Join(Environment.NewLine, lines);
        await _fileService.WriteAllText(path, content);
    }

    public Task UpdatePresets()
    {
        // TODO: Implement preset update from remote server.
        // Original VB6 downloads updated presets from spooky2.com
        // This requires network access and the update server URL.
        return Task.CompletedTask;
    }
}
