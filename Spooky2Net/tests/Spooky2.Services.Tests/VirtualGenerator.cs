using System.Text.RegularExpressions;
using Spooky2.Core.Interfaces;
using Spooky2.Core.Models;

namespace Spooky2.Services.Tests;

/// <summary>
/// Test double that simulates a real GeneratorX device by maintaining internal state,
/// parsing protocol commands, and returning appropriate responses.
/// </summary>
public sealed class VirtualGenerator : IGeneratorService
{
    // ── Internal state ──
    public double CurrentFrequencyHz { get; private set; }
    public long CurrentFrequencyNanoHz { get; private set; }
    public int AmplitudeCv1 { get; private set; }
    public int AmplitudeCv2 { get; private set; }
    public bool Output1Enabled { get; private set; }
    public bool Output2Enabled { get; private set; }
    public string DisplayName { get; private set; } = string.Empty;
    public bool ModulationEnabled { get; private set; }
    public bool FreqChannel1Cleared { get; private set; }
    public bool FreqChannel2Cleared { get; private set; }
    public GeneratorStatus Status { get; private set; } = GeneratorStatus.Idle;

    // ── Command log for assertions ──
    public List<string> CommandLog { get; } = new();

    // ── Configurable sensor responses ──
    public double BaselineAngle { get; set; } = 52207;
    public double BaselineCurrent { get; set; } = 6979;

    private readonly Dictionary<int, double> _angleSpikes = new();
    private readonly Dictionary<int, double> _currentSpikes = new();
    private int _angleReadCount;
    private int _currentReadCount;

    /// <summary>
    /// Inject a spike at a specific read index for angle sensor.
    /// </summary>
    public void InjectAngleSpike(int readIndex, double value)
    {
        _angleSpikes[readIndex] = value;
    }

    /// <summary>
    /// Inject a spike at a specific read index for current sensor.
    /// </summary>
    public void InjectCurrentSpike(int readIndex, double value)
    {
        _currentSpikes[readIndex] = value;
    }

    // ── IGeneratorService implementation ──

    public Task<List<GeneratorState>> FindGenerators() => Task.FromResult(new List<GeneratorState>
    {
        new() { Id = 0, Port = "VIRTUAL", Status = Status, CurrentProgram = DisplayName }
    });

    public Task Start(int generatorId)
    {
        Status = GeneratorStatus.Running;
        return Task.CompletedTask;
    }

    public Task Stop(int generatorId)
    {
        Status = GeneratorStatus.Idle;
        Output1Enabled = false;
        Output2Enabled = false;
        return Task.CompletedTask;
    }

    public Task Pause(int generatorId)
    {
        Status = GeneratorStatus.Paused;
        return Task.CompletedTask;
    }

    public Task Hold(int generatorId)
    {
        Status = GeneratorStatus.Held;
        return Task.CompletedTask;
    }

    public Task Resume(int generatorId)
    {
        Status = GeneratorStatus.Running;
        return Task.CompletedTask;
    }

    public Task WriteFrequencies(int generatorId, List<double> frequencies)
    {
        if (frequencies.Count > 0)
        {
            CurrentFrequencyHz = frequencies[0];
        }
        return Task.CompletedTask;
    }

    public Task<GeneratorState> ReadStatus(int generatorId) =>
        Task.FromResult(new GeneratorState { Id = generatorId, Status = Status });

    public Task EraseMemory(int generatorId) => Task.CompletedTask;
    public Task IdentifyGenerators() => Task.CompletedTask;

    public Task SendRawCommand(int generatorId, string command)
    {
        CommandLog.Add(command);
        return Task.CompletedTask;
    }

    public Task<string?> SendCommandWithResponse(int generatorId, string command)
    {
        CommandLog.Add(command);
        var response = ProcessCommand(command);
        return Task.FromResult<string?>(response);
    }

        public Task SendCommandsBatch(int generatorId, IReadOnlyList<string> commands)
        {
            foreach (var cmd in commands) CommandLog.Add(cmd);
            return Task.CompletedTask;
        }

        public Task WriteWaveformTables(int generatorId)
        {
            CommandLog.Add("[WriteWaveformTables]");
            return Task.CompletedTask;
        }

    // ── Command processing ──

    private string ProcessCommand(string command)
    {
        // Display name: :n00=text
        if (command.StartsWith(":n00="))
        {
            DisplayName = command[5..];
            return ":ok";
        }

        // Set frequency: :w24=value,
        if (command.StartsWith(":w24="))
        {
            var valStr = command[5..].TrimEnd(',');
            if (long.TryParse(valStr, out var val))
            {
                // Detect nanoHz vs raw Hz by digit count
                // nanoHz values are typically > 10 digits (e.g., 41020502562510)
                // Raw Hz values are typically < 8 digits (e.g., 41009)
                if (valStr.Length >= 10)
                {
                    CurrentFrequencyNanoHz = val;
                    CurrentFrequencyHz = val / 1e9;
                }
                else
                {
                    CurrentFrequencyHz = val;
                    CurrentFrequencyNanoHz = val * 1_000_000_000L;
                }
            }
            return ":ok";
        }

        // Set amplitude ch1: :w28=value,
        if (command.StartsWith(":w28="))
        {
            var valStr = command[5..].TrimEnd(',');
            if (int.TryParse(valStr, out var val))
                AmplitudeCv1 = val;
            return ":ok";
        }

        // Set amplitude ch2: :w29=value,
        if (command.StartsWith(":w29="))
        {
            var valStr = command[5..].TrimEnd(',');
            if (int.TryParse(valStr, out var val))
                AmplitudeCv2 = val;
            return ":ok";
        }

        // Enable output: :w11=1,, or :w11=,1,
        if (command.StartsWith(":w11="))
        {
            var valPart = command[5..];
            if (valPart.StartsWith("1,,"))
                Output1Enabled = true;
            else if (valPart.StartsWith(",1,"))
                Output2Enabled = true;
            return ":ok";
        }

        // Read angle: :r11=,
        if (command == ":r11=,")
        {
            _angleReadCount++;
            var value = _angleSpikes.TryGetValue(_angleReadCount, out var spike)
                ? spike
                : BaselineAngle;
            return $":r11={value}.";
        }

        // Read current: :r12=,
        if (command == ":r12=,")
        {
            _currentReadCount++;
            var value = _currentSpikes.TryGetValue(_currentReadCount, out var spike)
                ? spike
                : BaselineCurrent;
            return $":r12={value}.";
        }

        // Clear freq ch1: :w12=0,,
        if (command == ":w12=0,,")
        {
            FreqChannel1Cleared = true;
            return ":ok";
        }

        // Clear freq ch2: :w12=,0,
        if (command == ":w12=,0,")
        {
            FreqChannel2Cleared = true;
            return ":ok";
        }

        // Disable modulation: :w13=0,
        if (command.StartsWith(":w13="))
        {
            var valStr = command[5..].TrimEnd(',');
            ModulationEnabled = valStr != "0";
            return ":ok";
        }

        // Start/stop outputs via w61/w62
        if (command == ":w611") { Output1Enabled = true; return ":ok"; }
        if (command == ":w610") { Output1Enabled = false; return ":ok"; }
        if (command == ":w621") { Output2Enabled = true; return ":ok"; }
        if (command == ":w620") { Output2Enabled = false; return ":ok"; }

        // Default: return :ok for any write command
        if (command.StartsWith(":w") || command.StartsWith(":n") || command.StartsWith(":a"))
            return ":ok";

        // Read commands return empty by default
        if (command.StartsWith(":r"))
            return ":ok";

        return ":ok";
    }

    /// <summary>
    /// Reset all state and command log.
    /// </summary>
    public void Reset()
    {
        CurrentFrequencyHz = 0;
        CurrentFrequencyNanoHz = 0;
        AmplitudeCv1 = 0;
        AmplitudeCv2 = 0;
        Output1Enabled = false;
        Output2Enabled = false;
        DisplayName = string.Empty;
        ModulationEnabled = false;
        FreqChannel1Cleared = false;
        FreqChannel2Cleared = false;
        Status = GeneratorStatus.Idle;
        CommandLog.Clear();
        _angleSpikes.Clear();
        _currentSpikes.Clear();
        _angleReadCount = 0;
        _currentReadCount = 0;
    }
}
