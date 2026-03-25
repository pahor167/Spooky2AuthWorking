using System.Collections.ObjectModel;
using System.Globalization;
using CommunityToolkit.Mvvm.ComponentModel;
using CommunityToolkit.Mvvm.Input;
using Microsoft.Extensions.Logging;
using Microsoft.Extensions.Logging.Abstractions;
using Spooky2.Core.Interfaces;

namespace Spooky2.ViewModels.Dialogs;

public partial class FrequencyTestViewModel : ObservableObject
{
    private readonly IGeneratorService _generatorService;
    private readonly int _generatorId;
    private readonly ILogger<FrequencyTestViewModel> _logger;

    [ObservableProperty] private string _frequencyInput = "41000";
    [ObservableProperty] private string _w24Preview = "";
    [ObservableProperty] private string _statusText = "Ready";
    [ObservableProperty] private string _lastSentCommand = "";

    public ObservableCollection<string> ScanFrequencies { get; } = new();
    public ObservableCollection<string> CommandLog { get; } = new();

    public FrequencyTestViewModel(IGeneratorService generatorService, int generatorId)
    {
        _generatorService = generatorService;
        _generatorId = generatorId;
        _logger = NullLogger<FrequencyTestViewModel>.Instance;

        // Pre-calculate first 20 biofeedback frequencies (41kHz, 0.025% step)
        var freq = 41000.0;
        for (var i = 0; i < 20; i++)
        {
            var f8 = freq.ToString("F8", CultureInfo.InvariantCulture);
            var w24 = f8.Replace(".", "").TrimStart('0');
            ScanFrequencies.Add($"{freq:F3} Hz → :w24={w24}");
            freq += freq * 0.00025;
        }

        UpdatePreview();
    }

    partial void OnFrequencyInputChanged(string value) => UpdatePreview();

    private void UpdatePreview()
    {
        if (double.TryParse(FrequencyInput, NumberStyles.Float, CultureInfo.InvariantCulture, out var freq))
        {
            var s = freq.ToString("F8", CultureInfo.InvariantCulture);
            var noDot = s.Replace(".", "").TrimStart('0');
            if (noDot.Length == 0) noDot = "0";
            W24Preview = $":w24={noDot},";
        }
        else
        {
            W24Preview = "(invalid frequency)";
        }
    }

    [RelayCommand]
    private async Task SendInit()
    {
        StatusText = "Sending init sequence...";
        CommandLog.Clear();

        var cmds = new[]
        {
            ":w14=0,", ":w17=0,0,", ":w24=0,", ":w25=0,",
            ":w15=1,1,",  // LOW FREQUENCY MODE
            ":w24=00,", ":w32=120,", ":w33=120,",
            ":n00=Freq Test",
            ":w13=0,", ":w28=0,", ":w29=0,", ":w24=00,",
            ":w12=0,,", ":w12=,0,",
            ":w32=120,", ":w40=0,", ":w33=120,", ":w40=0,",
            ":w13=0,", ":w20=11,", ":w14=1,",
            ":w12=0,,", ":w12=,0,", ":w21=25,",
            ":w11=1,,", ":w11=,1,",  // enable outputs
            ":w28=2000,", ":w29=2000,",  // full amplitude
        };

        foreach (var cmd in cmds)
        {
            var resp = await Task.Run(() => _generatorService.SendCommandWithResponse(_generatorId, cmd));
            CommandLog.Add($"TX: {cmd}  ->  RX: {resp}");
        }

        StatusText = $"Init complete ({cmds.Length} commands sent)";
    }

    [RelayCommand]
    private async Task SendFrequency()
    {
        if (!double.TryParse(FrequencyInput, NumberStyles.Float, CultureInfo.InvariantCulture, out var freq))
        {
            StatusText = "Invalid frequency";
            return;
        }

        var s = freq.ToString("F8", CultureInfo.InvariantCulture);
        var noDot = s.Replace(".", "").TrimStart('0');
        if (noDot.Length == 0) noDot = "0";
        var cmd = $":w24={noDot},";

        LastSentCommand = cmd;
        StatusText = $"Sending {cmd}...";

        var resp = await Task.Run(() => _generatorService.SendCommandWithResponse(_generatorId, cmd));
        CommandLog.Add($"TX: {cmd}  ->  RX: {resp}");
        StatusText = $"Sent: {cmd} -> {resp}";
    }

    [RelayCommand]
    private async Task SendRawW24()
    {
        var cmd = $":w24={FrequencyInput},";
        LastSentCommand = cmd;
        StatusText = $"Sending RAW {cmd}...";

        var resp = await Task.Run(() => _generatorService.SendCommandWithResponse(_generatorId, cmd));
        CommandLog.Add($"TX: {cmd}  ->  RX: {resp}");
        StatusText = $"Sent RAW: {cmd} -> {resp}";
    }

    [RelayCommand]
    private void SelectFrequency(string freqText)
    {
        var hz = freqText.Replace(" Hz", "").Trim();
        FrequencyInput = hz;
    }
}
