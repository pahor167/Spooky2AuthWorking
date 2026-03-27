using System.Collections.ObjectModel;
using System.Globalization;
using System.Timers;
using CommunityToolkit.Mvvm.ComponentModel;
using CommunityToolkit.Mvvm.Input;
using Microsoft.Extensions.Logging;
using Microsoft.Extensions.Logging.Abstractions;
using Spooky2.Core.Interfaces;
using Spooky2.Core.Models;

namespace Spooky2.ViewModels;

public partial class ControlViewModel : ObservableObject, IDisposable
{
    private readonly IGeneratorService _generatorService;
    private readonly IWaveformService _waveformService;
    private readonly IScanService _scanService;
    private readonly IDatabaseService? _databaseService;
    private readonly IDialogService? _dialogService;
    private readonly ILogger<ControlViewModel> _logger;
    private readonly System.Timers.Timer _statusTimer;

    private int _currentFrequencyIndex;
    private CancellationTokenSource? _runCts;
    private CancellationTokenSource? _scanCts;

    public ControlViewModel(
        IGeneratorService generatorService,
        IWaveformService waveformService,
        IScanService scanService,
        ILogger<ControlViewModel>? logger = null,
        IDatabaseService? databaseService = null,
        IDialogService? dialogService = null)
    {
        _generatorService = generatorService;
        _waveformService = waveformService;
        _scanService = scanService;
        _databaseService = databaseService;
        _dialogService = dialogService;
        _logger = logger ?? NullLogger<ControlViewModel>.Instance;

        // Timer to poll generator status every 500ms
        _statusTimer = new System.Timers.Timer(500);
        _statusTimer.Elapsed += OnStatusTimerElapsed;

        ReverseLookupResults.CollectionChanged += (_, _) => OnPropertyChanged(nameof(HasReverseLookupResults));

        _logger.LogDebug("ControlViewModel initialized");
    }

    // ── Generator Selection ──

    public ObservableCollection<GeneratorItem> AvailableGenerators { get; } = new();

    [ObservableProperty]
    private GeneratorItem? _selectedGenerator;

    [ObservableProperty]
    private int _selectedGeneratorId = -1;

    [ObservableProperty]
    private string _generatorTitle = "No Generator Selected";

    [ObservableProperty]
    private string _generatorStatusText = "Idle";

    partial void OnSelectedGeneratorChanged(GeneratorItem? value)
    {
        if (value != null)
        {
            SelectedGeneratorId = value.Id;
            GeneratorTitle = value.DisplayName;
            _logger.LogInformation("User selected generator {Id}: {Name}", value.Id, value.DisplayName);
        }
        else
        {
            SelectedGeneratorId = -1;
            GeneratorTitle = "No Generator Selected";
        }
    }

    public sealed record GeneratorItem(int Id, string Port, string Type)
    {
        public string DisplayName => $"Gen {Id} ({Type}) on {Port}";
        public override string ToString() => DisplayName;
    }

    [ObservableProperty]
    private bool _isRunning;

    [ObservableProperty]
    private bool _isPaused;

    [ObservableProperty]
    private bool _isHeld;

    // ── Frequency Display ──

    public ObservableCollection<string> FrequencyItems { get; } = new();

    [ObservableProperty]
    private string? _selectedFrequency;

    [ObservableProperty]
    private int _selectedFrequencyIndex;

    [ObservableProperty]
    private double _progressValue;

    [ObservableProperty]
    private double _progressMaximum = 100;

    [ObservableProperty]
    private int _dwellValue = 180;

    [ObservableProperty]
    private int _stepValue;

    [ObservableProperty]
    private int _presetValue = 1;

    [ObservableProperty]
    private string _totalStatus = "";

    // ── Run Time Display ──

    [ObservableProperty]
    private string _estimatedTotalRunTime = "00:00:00";

    [ObservableProperty]
    private string _currentPresetDuration = "00:00:00";

    [ObservableProperty]
    private string _currentChainDuration = "00:00:00";

    // ── Output Display ──

    [ObservableProperty]
    private double _output1Frequency;

    [ObservableProperty]
    private double _output1Amplitude;

    [ObservableProperty]
    private string _output1WaveformDisplay = "Sine";

    [ObservableProperty]
    private string _output1AmplitudeDisplay = "20v";

    [ObservableProperty]
    private double _output2Frequency;

    [ObservableProperty]
    private double _output2Amplitude;

    [ObservableProperty]
    private string _output2WaveformDisplay = "Inverse";

    [ObservableProperty]
    private string _output2AmplitudeDisplay = "20v";

    // ── Biofeedback ──

    public ObservableCollection<string> BiofeedbackHits { get; } = new();

    [ObservableProperty]
    private bool _isScanning;

    [ObservableProperty]
    private string _scanStatusText = "";

    [ObservableProperty]
    private double _scanProgress;

    // ── Biofeedback Graph ──

    [ObservableProperty]
    private IReadOnlyList<double> _graphReadings = Array.Empty<double>();

    [ObservableProperty]
    private double _graphRunningAverage;

    [ObservableProperty]
    private double _graphMinValue;

    [ObservableProperty]
    private double _graphMaxValue = 100;

    [ObservableProperty]
    private string _graphFrequencyRange = "41000 - 1800000";

    // ── Reverse Lookup ──

    public ObservableCollection<string> ReverseLookupResults { get; } = new();

    public bool HasReverseLookupResults => ReverseLookupResults.Count > 0;

    [ObservableProperty]
    private string? _selectedBiofeedbackHit;

    // ── Program Options ──

    [ObservableProperty]
    private double _dwellMultiplier = 1.0;

    [ObservableProperty]
    private double _frequencyMultiplier = 1.0;

    [ObservableProperty]
    private int _repeatSequence = 1;

    [ObservableProperty]
    private int _repeatProgram = 1;

    [ObservableProperty]
    private int _repeatChain = 1;

    [ObservableProperty]
    private bool _removeDuplicates;

    // ── Loaded Programs ──

    public ObservableCollection<string> LoadedPrograms { get; } = new();

    // ── Collections for UI dropdowns ──

    public ObservableCollection<string> WobbleWaveforms { get; } = ["None", "Sine", "Square", "Sawtooth", "InverseSawtooth", "Triangle"];
    public ObservableCollection<string> HarmonicWobbleTypes { get; } = ["None", "Odd", "Even", "All"];
    public ObservableCollection<string> ModulationTypes { get; } = ["None", "AM", "FM", "PM"];
    public ObservableCollection<string> OctaveValues { get; } = ["1", "2", "3", "4", "5", "6", "7", "8"];

    // ── Methods ──

    /// <summary>Loads frequencies from a database search result or preset into the control.</summary>
    public void LoadFrequencies(IReadOnlyList<double> frequencies, string programName)
    {
        FrequencyItems.Clear();
        foreach (var freq in frequencies)
        {
            FrequencyItems.Add(freq.ToString("N7", CultureInfo.InvariantCulture));
        }
        _currentFrequencyIndex = 0;
        UpdateProgress();
        GeneratorTitle = programName;
        _logger.LogInformation("Loaded {Count} frequencies for '{Program}'", frequencies.Count, programName);
    }

    /// <summary>Adds a discovered generator to the selection list.</summary>
    public void AddGenerator(int generatorId, string port, string type)
    {
        var item = new GeneratorItem(generatorId, port, type);
        AvailableGenerators.Add(item);

        // Auto-select first generator
        if (SelectedGenerator == null)
        {
            SelectedGenerator = item;
        }

        _logger.LogInformation("Added generator {Id} ({Type}) on {Port}", generatorId, type, port);
    }

    /// <summary>Clears the generator list (before rescan).</summary>
    public void ClearGenerators()
    {
        AvailableGenerators.Clear();
        SelectedGenerator = null;
    }

    // ── Commands ──

    [RelayCommand]
    private async Task Start()
    {
        if (SelectedGeneratorId < 0)
        {
            _logger.LogWarning("Start: no generator selected");
            return;
        }

        try
        {
            _logger.LogInformation("Starting generator {Id} with {Count} frequencies, dwell={Dwell}s",
                SelectedGeneratorId, FrequencyItems.Count, DwellValue);

            // Write frequencies to generator
            var frequencies = ParseFrequencies();
            if (frequencies.Count > 0)
            {
                await Task.Run(() => _generatorService.WriteFrequencies(SelectedGeneratorId, frequencies));
            }

            // Start generator
            await Task.Run(() => _generatorService.Start(SelectedGeneratorId));

            IsRunning = true;
            IsPaused = false;
            IsHeld = false;
            GeneratorStatusText = "Running";
            _currentFrequencyIndex = 0;
            _statusTimer.Start();

            // Start frequency cycling
            _runCts?.Cancel();
            _runCts = new CancellationTokenSource();
            _ = RunFrequencyCycleAsync(_runCts.Token);
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Start failed for generator {Id}", SelectedGeneratorId);
        }
    }

    [RelayCommand]
    private async Task Stop()
    {
        if (SelectedGeneratorId < 0) return;

        try
        {
            _logger.LogInformation("Stopping generator {Id}", SelectedGeneratorId);
            _runCts?.Cancel();
            _scanCts?.Cancel();
            _statusTimer.Stop();

            await Task.Run(() => _scanService.StopScan(SelectedGeneratorId));
            await Task.Run(() => _generatorService.Stop(SelectedGeneratorId));

            IsRunning = false;
            IsPaused = false;
            IsHeld = false;
            GeneratorStatusText = "Idle";
            Output1Frequency = 0;
            Output2Frequency = 0;
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Stop failed for generator {Id}", SelectedGeneratorId);
        }
    }

    [RelayCommand]
    private async Task Pause()
    {
        if (SelectedGeneratorId < 0 || !IsRunning) return;

        try
        {
            if (IsPaused)
            {
                _logger.LogInformation("Resuming generator {Id}", SelectedGeneratorId);
                await Task.Run(() => _generatorService.Resume(SelectedGeneratorId));
                IsPaused = false;
                GeneratorStatusText = "Running";
                _runCts = new CancellationTokenSource();
                _ = RunFrequencyCycleAsync(_runCts.Token);
            }
            else
            {
                _logger.LogInformation("Pausing generator {Id}", SelectedGeneratorId);
                _runCts?.Cancel();
                await Task.Run(() => _generatorService.Pause(SelectedGeneratorId));
                IsPaused = true;
                GeneratorStatusText = "Paused";
            }
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Pause/Resume failed for generator {Id}", SelectedGeneratorId);
        }
    }

    [RelayCommand]
    private async Task Hold()
    {
        if (SelectedGeneratorId < 0 || !IsRunning) return;

        try
        {
            if (IsHeld)
            {
                _logger.LogInformation("Releasing hold on generator {Id}", SelectedGeneratorId);
                await Task.Run(() => _generatorService.Resume(SelectedGeneratorId));
                IsHeld = false;
                GeneratorStatusText = "Running";
                _runCts = new CancellationTokenSource();
                _ = RunFrequencyCycleAsync(_runCts.Token);
            }
            else
            {
                _logger.LogInformation("Holding generator {Id} at {Freq} Hz",
                    SelectedGeneratorId, Output1Frequency);
                _runCts?.Cancel();
                await Task.Run(() => _generatorService.Hold(SelectedGeneratorId));
                IsHeld = true;
                GeneratorStatusText = $"Held at {Output1Frequency:N2} Hz";
            }
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Hold failed for generator {Id}", SelectedGeneratorId);
        }
    }

    [RelayCommand]
    private async Task EraseFrequencies()
    {
        if (SelectedGeneratorId < 0) return;

        try
        {
            _logger.LogInformation("Erasing generator {Id} memory", SelectedGeneratorId);
            _runCts?.Cancel();
            _statusTimer.Stop();

            await Task.Run(() => _generatorService.EraseMemory(SelectedGeneratorId));

            FrequencyItems.Clear();
            IsRunning = false;
            IsPaused = false;
            IsHeld = false;
            GeneratorStatusText = "Idle";
            Output1Frequency = 0;
            Output2Frequency = 0;
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Erase failed for generator {Id}", SelectedGeneratorId);
        }
    }

    [RelayCommand]
    private async Task ResetGenerator()
    {
        if (SelectedGeneratorId < 0) return;

        try
        {
            _logger.LogInformation("Resetting generator {Id}", SelectedGeneratorId);
            await Task.Run(() => _generatorService.EraseMemory(SelectedGeneratorId));
            GeneratorStatusText = "Reset";
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Reset failed for generator {Id}", SelectedGeneratorId);
        }
    }

    [RelayCommand]
    private async Task WriteWaveforms()
    {
        if (SelectedGeneratorId < 0) return;

        try
        {
            _logger.LogInformation("Writing waveforms to generator {Id}", SelectedGeneratorId);
            // Send waveform type commands to generator
            // TODO: map UI waveform selections to generator protocol commands
            await Task.CompletedTask;
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "WriteWaveforms failed for generator {Id}", SelectedGeneratorId);
        }
    }

    [RelayCommand]
    private async Task Scan()
    {
        if (SelectedGeneratorId < 0)
        {
            _logger.LogWarning("Scan: no generator selected");
            return;
        }

        try
        {
            IsScanning = true;
            ScanStatusText = "Starting scan...";
            _scanCts?.Cancel();
            _scanCts = new CancellationTokenSource();

            var parameters = new ScanParameters();
            var graphData = new List<(DateTime time, double value)>();
            var scanProgress = new Progress<ScanProgress>(p =>
            {
                ScanProgress = p.PercentComplete;
                ScanStatusText = p.StatusText;
                Output1Frequency = p.CurrentFrequency;
                Output2Frequency = p.CurrentFrequency;
                if (p.AmplitudeCv > 0)
                    Output1AmplitudeDisplay = $"{p.AmplitudeCv / 100.0:F1}v";

                if (p.CurrentReading > 0)
                {
                    graphData.Add((DateTime.UtcNow, p.CurrentReading));
                    // Sliding 10-second window
                    var cutoff = DateTime.UtcNow.AddSeconds(-10);
                    graphData.RemoveAll(x => x.time < cutoff);

                    var vals = graphData.Select(x => x.value).ToList();
                    GraphReadings = vals;
                    GraphRunningAverage = p.CurrentRunningAverage;
                    GraphMinValue = vals.Min() - 10;
                    GraphMaxValue = vals.Max() + 10;
                    GraphFrequencyRange = $"{p.CurrentFrequency:N0} Hz";
                }
            });

            _logger.LogInformation("Starting biofeedback scan on generator {Id}", SelectedGeneratorId);
            var hits = await Task.Run(() => _scanService.RunBiofeedbackScan(
                SelectedGeneratorId, parameters, scanProgress, _scanCts.Token));

            BiofeedbackHits.Clear();
            foreach (var hit in hits)
            {
                BiofeedbackHits.Add($"{hit.Frequency:N2} Hz (dev: {hit.Deviation:N2})");
            }

            ScanStatusText = $"Scan complete: {hits.Count} hits";
            _logger.LogInformation("Scan complete: {Count} hits", hits.Count);
        }
        catch (OperationCanceledException)
        {
            ScanStatusText = "Scan cancelled";
            _logger.LogInformation("Scan cancelled");
        }
        catch (Exception ex)
        {
            ScanStatusText = $"Scan error: {ex.Message}";
            _logger.LogError(ex, "Scan failed for generator {Id}", SelectedGeneratorId);
        }
        finally
        {
            IsScanning = false;
        }
    }

    [RelayCommand]
    private async Task HuntAndKill()
    {
        if (SelectedGeneratorId < 0)
        {
            _logger.LogWarning("HuntAndKill: no generator selected");
            return;
        }

        try
        {
            IsScanning = true;
            ScanStatusText = "Starting Hunt and Kill...";
            _scanCts?.Cancel();
            _scanCts = new CancellationTokenSource();

            var parameters = new ScanParameters();
            var hkGraphData = new List<(DateTime time, double value)>();
            var scanProgress = new Progress<ScanProgress>(p =>
            {
                ScanProgress = p.PercentComplete;
                ScanStatusText = p.StatusText;
                Output1Frequency = p.CurrentFrequency;
                Output2Frequency = p.CurrentFrequency;
                if (p.AmplitudeCv > 0)
                    Output1AmplitudeDisplay = $"{p.AmplitudeCv / 100.0:F1}v";

                if (p.CurrentReading > 0)
                {
                    hkGraphData.Add((DateTime.UtcNow, p.CurrentReading));
                    var cutoff = DateTime.UtcNow.AddSeconds(-10);
                    hkGraphData.RemoveAll(x => x.time < cutoff);

                    var vals = hkGraphData.Select(x => x.value).ToList();
                    GraphReadings = vals;
                    GraphRunningAverage = p.CurrentRunningAverage;
                    GraphMinValue = vals.Min() - 10;
                    GraphMaxValue = vals.Max() + 10;
                    GraphFrequencyRange = $"{p.CurrentFrequency:N0} Hz";
                }
            });

            _logger.LogInformation("Starting Hunt and Kill on generator {Id}", SelectedGeneratorId);
            var hits = await Task.Run(() => _scanService.RunHuntAndKill(
                SelectedGeneratorId, parameters, scanProgress, _scanCts.Token));

            BiofeedbackHits.Clear();
            foreach (var hit in hits)
            {
                BiofeedbackHits.Add($"{hit.Frequency:N2} Hz (dev: {hit.Deviation:N2})");
            }

            ScanStatusText = $"Hunt and Kill complete: {hits.Count} final hits";
            _logger.LogInformation("Hunt and Kill complete: {Count} hits", hits.Count);
        }
        catch (OperationCanceledException)
        {
            ScanStatusText = "Hunt and Kill cancelled";
        }
        catch (Exception ex)
        {
            ScanStatusText = $"Hunt and Kill error: {ex.Message}";
            _logger.LogError(ex, "Hunt and Kill failed for generator {Id}", SelectedGeneratorId);
        }
        finally
        {
            IsScanning = false;
        }
    }

    [RelayCommand]
    private void RefreshDisplay()
    {
        UpdateProgress();
    }

    [RelayCommand]
    private void CopyFrequencies()
    {
        // TODO: copy to clipboard
        _logger.LogDebug("CopyFrequencies stub");
    }

    [RelayCommand]
    private void PasteFrequencies()
    {
        // TODO: paste from clipboard
        _logger.LogDebug("PasteFrequencies stub");
    }

    [RelayCommand]
    private async Task ReverseLookup()
    {
        if (_databaseService == null)
        {
            _logger.LogWarning("ReverseLookup: no database service available");
            return;
        }

        // Parse the selected biofeedback hit frequency
        double frequency = 0;
        var hitText = SelectedBiofeedbackHit;
        if (string.IsNullOrWhiteSpace(hitText))
        {
            _logger.LogWarning("ReverseLookup: no biofeedback hit selected");
            return;
        }

        // Parse frequency from hit display format: "1234.56 Hz (dev: 78.90)"
        var hzIndex = hitText.IndexOf(" Hz", StringComparison.OrdinalIgnoreCase);
        if (hzIndex > 0)
        {
            var freqStr = hitText[..hzIndex].Replace(",", "").Trim();
            double.TryParse(freqStr, System.Globalization.NumberStyles.Float,
                System.Globalization.CultureInfo.InvariantCulture, out frequency);
        }

        if (frequency <= 0)
        {
            _logger.LogWarning("ReverseLookup: could not parse frequency from '{Hit}'", hitText);
            return;
        }

        try
        {
            _logger.LogInformation("Starting reverse lookup for {Freq} Hz", frequency);
            ScanStatusText = $"Reverse lookup for {frequency:N2} Hz...";

            var parameters = new ReverseLookupParameters();
            var results = await Task.Run(() =>
                _scanService.ReverseLookup(frequency, parameters, _databaseService));

            ReverseLookupResults.Clear();
            foreach (var result in results)
            {
                ReverseLookupResults.Add(
                    $"{result.ProgramName} [{result.SourceDatabase}] " +
                    $"({result.MatchType}: {result.MatchedFrequency:N2} Hz)");
            }

            ScanStatusText = $"Reverse lookup: {results.Count} programs found for {frequency:N2} Hz";
            _logger.LogInformation("Reverse lookup complete: {Count} results for {Freq} Hz",
                results.Count, frequency);
        }
        catch (Exception ex)
        {
            ScanStatusText = $"Reverse lookup error: {ex.Message}";
            _logger.LogError(ex, "Reverse lookup failed for {Freq} Hz", frequency);
        }
    }

    /// <summary>Loads a preset's frequencies and settings into the control.</summary>
    public void LoadPreset(Preset preset)
    {
        _logger.LogInformation("Loading preset '{Name}' with {Count} programs",
            preset.Name, preset.Programs.Count);

        FrequencyItems.Clear();
        LoadedPrograms.Clear();

        var allFrequencies = new List<double>();
        foreach (var program in preset.Programs)
        {
            LoadedPrograms.Add(program.Name);
            allFrequencies.AddRange(program.Frequencies);
        }

        foreach (var freq in allFrequencies)
        {
            FrequencyItems.Add(freq.ToString("N7", CultureInfo.InvariantCulture));
        }

        _currentFrequencyIndex = 0;
        UpdateProgress();

        // Apply preset settings
        if (preset.Settings.TryGetValue("DwellMultiplier", out var dwellMulStr) &&
            double.TryParse(dwellMulStr, CultureInfo.InvariantCulture, out var dwellMul))
        {
            DwellMultiplier = dwellMul;
        }

        if (preset.Settings.TryGetValue("RepeatSequence", out var repSeqStr) &&
            int.TryParse(repSeqStr, out var repSeq))
        {
            RepeatSequence = repSeq;
        }

        if (preset.Settings.TryGetValue("RepeatProgram", out var repProgStr) &&
            int.TryParse(repProgStr, out var repProg))
        {
            RepeatProgram = repProg;
        }

        if (preset.Settings.TryGetValue("Amplitude", out var ampStr) &&
            double.TryParse(ampStr, CultureInfo.InvariantCulture, out var amp))
        {
            Output1Amplitude = amp;
            Output2Amplitude = amp;
            Output1AmplitudeDisplay = $"{amp}v";
            Output2AmplitudeDisplay = $"{amp}v";
        }

        if (preset.Settings.TryGetValue("DwellSeconds", out var dwellStr) &&
            int.TryParse(dwellStr, out var dwell))
        {
            DwellValue = dwell;
        }
        else if (preset.Programs.Count > 0 && preset.Programs[0].DwellSeconds > 0)
        {
            DwellValue = (int)preset.Programs[0].DwellSeconds;
        }

        GeneratorTitle = preset.Name;
        _logger.LogInformation("Preset '{Name}' loaded: {FreqCount} frequencies, dwell={Dwell}s",
            preset.Name, allFrequencies.Count, DwellValue);
    }

    // ── Private Logic ──

    /// <summary>Cycles through loaded frequencies at the dwell rate.</summary>
    private async Task RunFrequencyCycleAsync(CancellationToken ct)
    {
        var frequencies = ParseFrequencies();
        if (frequencies.Count == 0) return;

        try
        {
            for (int rep = 0; rep < RepeatProgram && !ct.IsCancellationRequested; rep++)
            {
                for (int i = 0; i < frequencies.Count && !ct.IsCancellationRequested; i++)
                {
                    _currentFrequencyIndex = i;
                    var freq = frequencies[i] * FrequencyMultiplier;

                    // Write frequency to generator
                    await Task.Run(() => _generatorService.WriteFrequencies(SelectedGeneratorId, [freq]));

                    Output1Frequency = freq;
                    Output2Frequency = freq;
                    StepValue = i + 1;
                    UpdateProgress();

                    _logger.LogDebug("Frequency step {Step}/{Total}: {Freq} Hz",
                        i + 1, frequencies.Count, freq);

                    // Dwell
                    var dwellMs = (int)(DwellValue * DwellMultiplier * 1000);
                    await Task.Delay(dwellMs, ct);
                }
            }

            // Finished all frequencies
            _logger.LogInformation("Frequency program completed for generator {Id}", SelectedGeneratorId);
            await Stop();
        }
        catch (OperationCanceledException)
        {
            _logger.LogDebug("Frequency cycle cancelled");
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Error in frequency cycle");
        }
    }

    private async void OnStatusTimerElapsed(object? sender, ElapsedEventArgs e)
    {
        if (SelectedGeneratorId < 0) return;

        try
        {
            var state = await Task.Run(() => _generatorService.ReadStatus(SelectedGeneratorId));
            CurrentPresetDuration = state.ElapsedTime.ToString(@"hh\:mm\:ss");

            var totalSeconds = FrequencyItems.Count * DwellValue * DwellMultiplier * RepeatProgram;
            EstimatedTotalRunTime = TimeSpan.FromSeconds(totalSeconds).ToString(@"hh\:mm\:ss");
        }
        catch { /* timer callback, don't throw */ }
    }

    private void UpdateProgress()
    {
        var total = FrequencyItems.Count;
        ProgressMaximum = total > 0 ? total : 1;
        ProgressValue = _currentFrequencyIndex;
        TotalStatus = $"{DwellValue}/{total}/{RepeatProgram}";
    }

    private List<double> ParseFrequencies()
    {
        var result = new List<double>();
        foreach (var item in FrequencyItems)
        {
            var cleaned = item.Replace(",", "").Trim();
            if (double.TryParse(cleaned, NumberStyles.Float, CultureInfo.InvariantCulture, out var freq))
            {
                result.Add(freq);
            }
        }
        return result;
    }

    [RelayCommand]
    private async Task OpenFrequencyTest()
    {
        if (SelectedGeneratorId < 0)
        {
            _logger.LogWarning("FreqTest: no generator selected");
            return;
        }

        if (_dialogService == null)
        {
            _logger.LogWarning("FreqTest: no dialog service available");
            return;
        }

        var vm = new Dialogs.FrequencyTestViewModel(_generatorService, SelectedGeneratorId);
        await _dialogService.ShowDialogAsync(vm);
    }

    public void Dispose()
    {
        _runCts?.Cancel();
        _runCts?.Dispose();
        _scanCts?.Cancel();
        _scanCts?.Dispose();
        _statusTimer.Stop();
        _statusTimer.Dispose();
    }
}
