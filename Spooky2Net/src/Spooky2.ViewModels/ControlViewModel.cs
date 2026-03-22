using System.Collections.ObjectModel;
using CommunityToolkit.Mvvm.ComponentModel;
using CommunityToolkit.Mvvm.Input;
using Spooky2.Core.Interfaces;
using Spooky2.Core.Models;

namespace Spooky2.ViewModels;

public partial class ControlViewModel : ObservableObject
{
    private readonly IWaveformService _waveformService;

    public ControlViewModel(IWaveformService waveformService)
    {
        _waveformService = waveformService;

        // Initialize frequency list with sample data
        FrequencyItems.Add("898,166.6040000");
        FrequencyItems.Add("124,000.0000000");
        FrequencyItems.Add("20,000.0000000");
        FrequencyItems.Add("2,489.0000000");
        FrequencyItems.Add("2,127.0000000");
        FrequencyItems.Add("2,008.0000000");
        FrequencyItems.Add("1,865.0000000");
        FrequencyItems.Add("880.0000000");
        FrequencyItems.Add("802.0000000");
        FrequencyItems.Add("787.0000000");

        BiofeedbackHits.Add("No hits recorded");
    }

    // ── Generator Control (Left Column) ──

    [ObservableProperty]
    private bool _overwriteGenerator;

    [ObservableProperty]
    private string _generatorSaveName = "";

    [ObservableProperty]
    private int _generatorNumber1 = 3;

    [ObservableProperty]
    private int _generatorPercent1;

    [ObservableProperty]
    private int _generatorNumber2 = 4;

    [ObservableProperty]
    private int _generatorPercent2 = 10;

    // ── Frequency Display & Status (Center) ──

    [ObservableProperty]
    private string _generatorTitle = "Generator 4  GX Hunt and Kill (C) - JW";

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
    private int _dwellValue = 129;

    [ObservableProperty]
    private int _stepValue = 1;

    [ObservableProperty]
    private int _presetValue = 1;

    [ObservableProperty]
    private double _ageFactor = 1.0;

    [ObservableProperty]
    private string _totalStatus = "180/10/1";

    [ObservableProperty]
    private double _frequencyAdjustSlider;

    [ObservableProperty]
    private double _frequencyAdjustHz = 1.0;

    // ── Run Time Display (Right Top) ──

    [ObservableProperty]
    private string _estimatedTotalRunTime = "00:30:00";

    [ObservableProperty]
    private string _currentPresetDuration = "00:02:10";

    [ObservableProperty]
    private string _currentChainDuration = "00:02:10";

    // ── Biofeedback Hits ──

    public ObservableCollection<string> BiofeedbackHits { get; } = new();

    [ObservableProperty]
    private string? _selectedBiofeedbackHit;

    // ── Reverse Lookup ──

    [ObservableProperty]
    private bool _includeHarmonics;

    [ObservableProperty]
    private bool _includeSubHarmonics;

    [ObservableProperty]
    private string _octaveValue = "1";

    [ObservableProperty]
    private double _tolerancePercent = 0.25;

    [ObservableProperty]
    private double _includeHz;

    // ── Generator Output (Right Center) ──

    // Out 1
    [ObservableProperty]
    private double _output1Frequency;

    [ObservableProperty]
    private double _output1Amplitude;

    [ObservableProperty]
    private double _output1Phase;

    [ObservableProperty]
    private WaveformType _output1WaveformType = WaveformType.Sine;

    [ObservableProperty]
    private string _output1WaveformDisplay = "Sine";

    [ObservableProperty]
    private string _output1DutyCycle = "50%";

    [ObservableProperty]
    private string _output1AmplitudeDisplay = "20v";

    [ObservableProperty]
    private string _output1Offset = "0%";

    [ObservableProperty]
    private string _output1PhaseDisplay = "0 Degrees";

    [ObservableProperty]
    private double _output1Angle = 68.65;

    [ObservableProperty]
    private double _output1Current = 523.34;

    // Out 2
    [ObservableProperty]
    private double _output2Frequency;

    [ObservableProperty]
    private double _output2Amplitude;

    [ObservableProperty]
    private double _output2Phase;

    [ObservableProperty]
    private WaveformType _output2WaveformType = WaveformType.Sine;

    [ObservableProperty]
    private string _output2WaveformDisplay = "Inverse";

    [ObservableProperty]
    private string _output2DutyCycle = "50%";

    [ObservableProperty]
    private string _output2AmplitudeDisplay = "20v";

    [ObservableProperty]
    private string _output2Offset = "0%";

    [ObservableProperty]
    private string _output2PhaseDisplay = "0 Degrees";

    [ObservableProperty]
    private double _output2Angle;

    [ObservableProperty]
    private double _output2Current;

    // Sync
    [ObservableProperty]
    private string _syncWaveformDisplay = "Inverse+Sync";

    // ── Biofeedback Scan (Bottom Left) ──

    [ObservableProperty]
    private string _scanLogName = "";

    [ObservableProperty]
    private double _scanStartFrequency = 41000;

    [ObservableProperty]
    private double _scanFinishFrequency = 1800000;

    [ObservableProperty]
    private double _scanInitialStepSize = 100;

    [ObservableProperty]
    private bool _scanStepSizeHz = true;

    [ObservableProperty]
    private bool _scanStepSizePercent;

    [ObservableProperty]
    private double _scanStepSizePercentValue = 0.025;

    [ObservableProperty]
    private int _scanMaxHits = 10;

    [ObservableProperty]
    private int _scanSamplesPerStep = 1;

    [ObservableProperty]
    private int _scanLoops = 1;

    [ObservableProperty]
    private int _scanStartDelay = 200;

    [ObservableProperty]
    private double _scanMinReadDelay = 0.07;

    [ObservableProperty]
    private double _scanThreshold;

    [ObservableProperty]
    private string _scanEstDuration = "00:03:18";

    // ── Detect (Bottom Center) ──

    [ObservableProperty]
    private bool _detectMax = true;

    [ObservableProperty]
    private bool _detectMin;

    [ObservableProperty]
    private bool _detectChange;

    [ObservableProperty]
    private bool _detectAngle = true;

    [ObservableProperty]
    private bool _detectCurrent;

    [ObservableProperty]
    private bool _detectBpm;

    [ObservableProperty]
    private bool _detectHrv;

    [ObservableProperty]
    private bool _detectAnglePlusCurrent;

    // RA Window
    [ObservableProperty]
    private int _raWindowSize = 20;

    [ObservableProperty]
    private int _raWindowN;

    [ObservableProperty]
    private int _raWindowOffset;

    [ObservableProperty]
    private bool _raRetentive;

    // After Scan
    [ObservableProperty]
    private bool _afterScanRunHits;

    [ObservableProperty]
    private bool _afterScanContinueRefining;

    [ObservableProperty]
    private int _afterScanRunOnGen;

    [ObservableProperty]
    private int _afterScanRunCycles;

    [ObservableProperty]
    private int _afterScanRunCycles2 = 1;

    // ── Calculate Using (Bottom Right) ──

    [ObservableProperty]
    private bool _calcRunningAverage = true;

    [ObservableProperty]
    private bool _calcPeak;

    [ObservableProperty]
    private bool _calcSingleScan;

    [ObservableProperty]
    private bool _calcGradeProgram;

    [ObservableProperty]
    private bool _calcPreventDuplicates;

    // BPM/HRV/VI display
    [ObservableProperty]
    private double _displayBpm;

    [ObservableProperty]
    private double _displayHrv;

    [ObservableProperty]
    private double _displayViAngle;

    [ObservableProperty]
    private double _displayCurrent;

    // ── Wobble (from original) ──

    [ObservableProperty]
    private WobbleWaveform _amplitudeWobbleWaveform = WobbleWaveform.None;

    [ObservableProperty]
    private double _amplitudeWobblePercent;

    [ObservableProperty]
    private int _amplitudeWobbleSteps;

    [ObservableProperty]
    private WobbleWaveform _frequencyWobbleWaveform = WobbleWaveform.None;

    [ObservableProperty]
    private double _frequencyWobblePercent;

    [ObservableProperty]
    private int _frequencyWobbleSteps;

    [ObservableProperty]
    private bool _frequencyWobbleCycleCount;

    [ObservableProperty]
    private string _selectedHarmonicWobble = "";

    // ── Gating (from original) ──

    [ObservableProperty]
    private bool _gateByFrequency;

    [ObservableProperty]
    private bool _gateByDuration;

    [ObservableProperty]
    private bool _gateEnabled;

    [ObservableProperty]
    private double _gateFrequency;

    [ObservableProperty]
    private double _gateOnMs;

    [ObservableProperty]
    private double _gateOffMs;

    // ── Program Options (from original) ──

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

    // ── Modulation (from original) ──

    [ObservableProperty]
    private string _selectedModulationType = "";

    [ObservableProperty]
    private bool _modulationEnabled;

    public ObservableCollection<string> LoadedPrograms { get; } = new();
    public ObservableCollection<string> WobbleWaveforms { get; } = new() { "None", "Sine", "Square", "Sawtooth", "InverseSawtooth", "Triangle" };
    public ObservableCollection<string> HarmonicWobbleTypes { get; } = new() { "None", "Odd", "Even", "All" };
    public ObservableCollection<string> ModulationTypes { get; } = new() { "None", "AM", "FM", "PM" };
    public ObservableCollection<string> OctaveValues { get; } = new() { "1", "2", "3", "4", "5", "6", "7", "8" };

    // ── Commands ──

    [RelayCommand]
    private async Task Start()
    {
        // Stub: start generator
        await Task.CompletedTask;
    }

    [RelayCommand]
    private async Task Scan()
    {
        // Stub: start scan
        await Task.CompletedTask;
    }

    [RelayCommand]
    private void Pause()
    {
        // Stub: pause generator
    }

    [RelayCommand]
    private void Hold()
    {
        // Stub: hold generator
    }

    [RelayCommand]
    private void AmplitudeWobble()
    {
        // Stub: toggle amplitude wobble
    }

    [RelayCommand]
    private void FrequencyWobble()
    {
        // Stub: toggle frequency wobble
    }

    [RelayCommand]
    private void Stop()
    {
        // Stub: stop generator
    }

    [RelayCommand]
    private void SaveGenerator()
    {
        // Stub: save generator settings
    }

    [RelayCommand]
    private void RefreshFrequency()
    {
        // Stub: refresh frequency adjustment
    }

    [RelayCommand]
    private void ReverseLookupGo()
    {
        // Stub: reverse lookup
    }

    [RelayCommand]
    private void LoadGx()
    {
        // Stub: load GX settings
    }

    [RelayCommand]
    private void PasteFrequencies()
    {
        // Stub: paste frequencies
    }

    [RelayCommand]
    private void CopyFrequencies()
    {
        // Stub: copy frequencies
    }

    [RelayCommand]
    private void EraseFrequencies()
    {
        // Stub: erase frequencies
    }

    [RelayCommand]
    private void ResetGenerator()
    {
        // Stub: reset generator
    }

    [RelayCommand]
    private void Analyze()
    {
        // Stub: analyze biofeedback
    }

    [RelayCommand]
    private void AnalyzePlus()
    {
        // Stub: analyze+ biofeedback
    }

    [RelayCommand]
    private void Baseline()
    {
        // Stub: baseline biofeedback
    }

    [RelayCommand]
    private async Task WriteWaveforms()
    {
        // Stub: write waveform settings to generator
        await Task.CompletedTask;
    }

    [RelayCommand]
    private void RefreshDisplay()
    {
        // Stub: refresh the control display
    }
}
