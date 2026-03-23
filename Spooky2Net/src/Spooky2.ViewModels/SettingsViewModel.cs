using System.Collections.ObjectModel;
using CommunityToolkit.Mvvm.ComponentModel;
using CommunityToolkit.Mvvm.Input;
using Microsoft.Extensions.Logging;
using Microsoft.Extensions.Logging.Abstractions;

namespace Spooky2.ViewModels;

/// <summary>
/// ViewModel for the Settings tab (waveform/signal configuration).
/// This corresponds to the Spooky2 "Settings" tab which controls signal parameters,
/// gating, wobble, schedule, output control, and waveform setup.
/// </summary>
public partial class SettingsViewModel : ObservableObject
{
    private readonly ILogger<SettingsViewModel> _logger;

    public SettingsViewModel(ILogger<SettingsViewModel>? logger = null)
    {
        _logger = logger ?? NullLogger<SettingsViewModel>.Instance;
        _logger.LogDebug("SettingsViewModel initialized");
        WobbleModes = new ObservableCollection<string> { "Disabled", "Triangle", "Sine", "Sawtooth", "Square" };
        HarmonicWobbleModes = new ObservableCollection<string> { "None", "Octave", "Decade" };
        FreqLimitModes = new ObservableCollection<string> { "Octave", "Decade", "None" };
        WaveformPresets = new ObservableCollection<string> { "Alpha-Stim", "Default", "Custom" };
        WaveformShapes = new ObservableCollection<string> { "Sine", "Square", "Sawtooth", "Triangle", "Inverse Sawtooth", "H-Bomb", "Damped Sinusoid" };
        F1F2Modes = new ObservableCollection<string> { "Add", "Subtract", "Multiply" };
    }

    // ── Signal Settings ──

    [ObservableProperty]
    private double _dutyCycleOut1 = 50;

    [ObservableProperty]
    private double _dutyCycleOut2 = 50;

    [ObservableProperty]
    private double _amplitudeOut1 = 20;

    [ObservableProperty]
    private double _amplitudeOut2 = 20;

    [ObservableProperty]
    private double _offsetOut1;

    [ObservableProperty]
    private double _offsetOut2;

    [ObservableProperty]
    private double _phaseOut1;

    [ObservableProperty]
    private double _phaseOut2;

    [ObservableProperty]
    private bool _modulateOut1;

    // ── Gating ──

    [ObservableProperty]
    private bool _is4HzGate = true;

    [ObservableProperty]
    private bool _isTimedGate;

    [ObservableProperty]
    private bool _gatingOut1Enabled;

    [ObservableProperty]
    private int _gatingOut1MsOn = 125;

    [ObservableProperty]
    private int _gatingOut1MsOff = 125;

    [ObservableProperty]
    private bool _gatingOut2Enabled;

    [ObservableProperty]
    private int _gatingOut2MsOn = 125;

    [ObservableProperty]
    private int _gatingOut2MsOff = 125;

    // ── Wobble ──

    public ObservableCollection<string> WobbleModes { get; }
    public ObservableCollection<string> HarmonicWobbleModes { get; }

    [ObservableProperty]
    private string _amplitudeWobbleMode = "Disabled";

    [ObservableProperty]
    private double _amplitudeWobblePercent;

    [ObservableProperty]
    private int _amplitudeWobbleSteps = 16;

    [ObservableProperty]
    private string _frequencyWobbleMode = "Disabled";

    [ObservableProperty]
    private double _frequencyWobblePercent;

    [ObservableProperty]
    private int _frequencyWobbleSteps = 16;

    [ObservableProperty]
    private string _harmonicWobbleMode = "None";

    // ── Schedule ──

    [ObservableProperty]
    private string _scheduleRunFrom = "08:00 pm";

    [ObservableProperty]
    private string _scheduleRunTo = "08:00 pm";

    [ObservableProperty]
    private int _startInDays;

    [ObservableProperty]
    private int _startInHours;

    [ObservableProperty]
    private int _startInMinutes;

    [ObservableProperty]
    private int _afterHours;

    [ObservableProperty]
    private bool _afterActionStop = true;

    [ObservableProperty]
    private bool _afterActionAdvance;

    // ── Contact Mode Options ──

    [ObservableProperty]
    private bool _rampUpEnabled;

    [ObservableProperty]
    private double _rampUpRate = 4;

    [ObservableProperty]
    private bool _rampDownEnabled;

    [ObservableProperty]
    private double _rampDownRate = 4;

    [ObservableProperty]
    private bool _reduceAmplitudeEnabled;

    [ObservableProperty]
    private double _reduceAmplitudeFrequency = 10;

    // ── Frequency Limits ──

    public ObservableCollection<string> FreqLimitModes { get; }

    [ObservableProperty]
    private double _freqLimitGreaterOut1;

    [ObservableProperty]
    private double _freqLimitGreaterOut2;

    [ObservableProperty]
    private string _freqLimitGreaterMode = "Octave";

    [ObservableProperty]
    private double _freqLimitLessOut1;

    [ObservableProperty]
    private double _freqLimitLessOut2;

    [ObservableProperty]
    private string _freqLimitLessMode = "Octave";

    // ── Output Control ──

    [ObservableProperty]
    private double _out1Multiplier;

    [ObservableProperty]
    private double _out1Factor;

    [ObservableProperty]
    private double _out1Offset;

    [ObservableProperty]
    private int _swapWaveformSeconds;

    [ObservableProperty]
    private bool _out2FollowsOut1 = true;

    [ObservableProperty]
    private bool _out1Fixed;

    [ObservableProperty]
    private bool _out2FollowsProgram;

    [ObservableProperty]
    private bool _out2AbsDiff;

    [ObservableProperty]
    private bool _out2EverySecond;

    [ObservableProperty]
    private bool _out1AllFreqs;

    [ObservableProperty]
    private bool _out2AbsOut1F1;

    [ObservableProperty]
    private bool _out2AbsF1F2;

    [ObservableProperty]
    private bool _swapOut1Out2;

    // ── Waveform Setup ──

    public ObservableCollection<string> WaveformPresets { get; }
    public ObservableCollection<string> WaveformShapes { get; }
    public ObservableCollection<string> F1F2Modes { get; }

    [ObservableProperty]
    private string _selectedWaveformPreset = "Alpha-Stim";

    [ObservableProperty]
    private string _selectedWaveformShape = "Sine";

    // Wave row 1
    [ObservableProperty] private bool _wave1Out1Selected = true;
    [ObservableProperty] private bool _wave1Out2Selected;
    [ObservableProperty] private bool _wave1Enabled = true;
    [ObservableProperty] private string _wave1Wcm = "0";
    [ObservableProperty] private string _wave1Spike = "0";
    [ObservableProperty] private string _wave1LengthRatio = "1";
    [ObservableProperty] private string _wave1Percent = "100";

    // Wave row 2
    [ObservableProperty] private bool _wave2Out1Selected;
    [ObservableProperty] private bool _wave2Out2Selected;
    [ObservableProperty] private bool _wave2Enabled;
    [ObservableProperty] private string _wave2Wcm = "0";
    [ObservableProperty] private string _wave2Spike = "0";
    [ObservableProperty] private string _wave2LengthRatio = "1";
    [ObservableProperty] private string _wave2Percent = "100";

    // Wave row 3
    [ObservableProperty] private bool _wave3Out1Selected;
    [ObservableProperty] private bool _wave3Out2Selected;
    [ObservableProperty] private bool _wave3Enabled;
    [ObservableProperty] private string _wave3Wcm = "0";
    [ObservableProperty] private string _wave3Spike = "0";
    [ObservableProperty] private string _wave3LengthRatio = "1";
    [ObservableProperty] private string _wave3Percent = "100";

    // Wave row 4
    [ObservableProperty] private bool _wave4Out1Selected;
    [ObservableProperty] private bool _wave4Out2Selected;
    [ObservableProperty] private bool _wave4Enabled;
    [ObservableProperty] private string _wave4Wcm = "0";
    [ObservableProperty] private string _wave4Spike = "0";
    [ObservableProperty] private string _wave4LengthRatio = "1";
    [ObservableProperty] private string _wave4Percent = "100";

    // Wave row 5
    [ObservableProperty] private bool _wave5Out1Selected;
    [ObservableProperty] private bool _wave5Out2Selected;
    [ObservableProperty] private bool _wave5Enabled;
    [ObservableProperty] private string _wave5Wcm = "0";
    [ObservableProperty] private string _wave5Spike = "0";
    [ObservableProperty] private string _wave5LengthRatio = "1";
    [ObservableProperty] private string _wave5Percent = "100";

    // Wave row 6
    [ObservableProperty] private bool _wave6Out1Selected;
    [ObservableProperty] private bool _wave6Out2Selected;
    [ObservableProperty] private bool _wave6Enabled;
    [ObservableProperty] private string _wave6Wcm = "0";
    [ObservableProperty] private string _wave6Spike = "0";
    [ObservableProperty] private string _wave6LengthRatio = "1";
    [ObservableProperty] private string _wave6Percent = "100";

    // Wave row 7
    [ObservableProperty] private bool _wave7Out1Selected;
    [ObservableProperty] private bool _wave7Out2Selected;
    [ObservableProperty] private bool _wave7Enabled;
    [ObservableProperty] private string _wave7Wcm = "0";
    [ObservableProperty] private string _wave7Spike = "0";
    [ObservableProperty] private string _wave7LengthRatio = "1";
    [ObservableProperty] private string _wave7Percent = "100";

    // Wave row 8
    [ObservableProperty] private bool _wave8Out1Selected;
    [ObservableProperty] private bool _wave8Out2Selected;
    [ObservableProperty] private bool _wave8Enabled;
    [ObservableProperty] private string _wave8Wcm = "0";
    [ObservableProperty] private string _wave8Spike = "0";
    [ObservableProperty] private string _wave8LengthRatio = "1";
    [ObservableProperty] private string _wave8Percent = "100";

    // Wave row 9
    [ObservableProperty] private bool _wave9Out1Selected;
    [ObservableProperty] private bool _wave9Out2Selected;
    [ObservableProperty] private bool _wave9Enabled;
    [ObservableProperty] private string _wave9Wcm = "0";
    [ObservableProperty] private string _wave9Spike = "0";
    [ObservableProperty] private string _wave9LengthRatio = "1";
    [ObservableProperty] private string _wave9Percent = "100";

    // Wave row 10
    [ObservableProperty] private bool _wave10Out1Selected;
    [ObservableProperty] private bool _wave10Out2Selected;
    [ObservableProperty] private bool _wave10Enabled;
    [ObservableProperty] private string _wave10Wcm = "0";
    [ObservableProperty] private string _wave10Spike = "0";
    [ObservableProperty] private string _wave10LengthRatio = "1";
    [ObservableProperty] private string _wave10Percent = "100";

    // Follow/Spike/Inverse
    [ObservableProperty]
    private bool _followOut1 = true;

    [ObservableProperty]
    private bool _spikePlus;

    [ObservableProperty]
    private bool _inverseMinus;

    [ObservableProperty]
    private bool _plusSpikeEnabled;

    [ObservableProperty]
    private bool _minusSpikeEnabled;

    [ObservableProperty]
    private bool _minusEdgeEnabled;

    [ObservableProperty]
    private int _spikeCount;

    // F2 formula
    [ObservableProperty]
    private double _f2Multiplier = 1;

    [ObservableProperty]
    private double _f2Offset;

    [ObservableProperty]
    private bool _addF1ToF2;

    [ObservableProperty]
    private string _selectedF1F2Mode = "Add";

    // Graph display
    [ObservableProperty]
    private bool _graphDisplayMn = true;

    [ObservableProperty]
    private bool _graphDisplayBn;

    // ── Startup Options ──

    [ObservableProperty]
    private bool _manualStart = true;

    [ObservableProperty]
    private bool _autoStart;

    [ObservableProperty]
    private bool _autoResume;

    // ── Audio Options ──

    [ObservableProperty]
    private bool _notifyWhenZeroHz = true;

    [ObservableProperty]
    private bool _notifyWhenProgramAdvances;

    // ── Commands ──

    [RelayCommand]
    private void WriteWaveforms()
    {
        // Stub: write waveform data to generator
    }

    [RelayCommand]
    private void RestoreDefaults()
    {
        _logger.LogInformation("Restoring settings defaults");
        DutyCycleOut1 = 50;
        DutyCycleOut2 = 50;
        AmplitudeOut1 = 20;
        AmplitudeOut2 = 20;
        OffsetOut1 = 0;
        OffsetOut2 = 0;
        PhaseOut1 = 0;
        PhaseOut2 = 0;
        ModulateOut1 = false;
        Is4HzGate = true;
        IsTimedGate = false;
        GatingOut1MsOn = 125;
        GatingOut1MsOff = 125;
        GatingOut2MsOn = 125;
        GatingOut2MsOff = 125;
        ManualStart = true;
        AutoStart = false;
        AutoResume = false;
    }
}
