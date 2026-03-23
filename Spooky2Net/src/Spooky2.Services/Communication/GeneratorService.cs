using Microsoft.Extensions.Logging;
using Spooky2.Core.Interfaces;
using Spooky2.Core.Models;

namespace Spooky2.Services.Communication;

/// <summary>
/// Generator control service. Communicates via serial ports matching the VB6 protocol.
/// Port is kept open per generator to avoid open/close overhead on every command.
/// </summary>
public sealed class GeneratorService : IGeneratorService, IDisposable
{
    private readonly ISerialPortFactory _serialPortFactory;
    private readonly ILogger<GeneratorService> _logger;
    private readonly Dictionary<int, GeneratorState> _generatorStates = new();
    private readonly Dictionary<int, List<double>> _loadedFrequencies = new();
    private readonly Dictionary<int, DateTime> _startTimes = new();
    private readonly Dictionary<int, PortConfig> _portConfigs = new();
    private readonly Dictionary<int, ISerialPortConnection> _openPorts = new();

    private sealed record PortConfig(string PortName, int BaudRate);

    public GeneratorService(ISerialPortFactory serialPortFactory, ILogger<GeneratorService> logger)
    {
        _serialPortFactory = serialPortFactory ?? throw new ArgumentNullException(nameof(serialPortFactory));
        _logger = logger ?? throw new ArgumentNullException(nameof(logger));
    }

    public async Task<List<GeneratorState>> FindGenerators()
    {
        var ports = _serialPortFactory.GetAvailablePorts();
        _logger.LogInformation("Scanning {Count} serial port(s): {Ports}", ports.Count, string.Join(", ", ports));

        var foundGenerators = new List<GeneratorState>();

        foreach (var port in ports)
        {
            foreach (var baudRate in new[] { 57600, 115200 })
            {
                try
                {
                    _logger.LogDebug("Probing {Port} at {Baud} baud...", port, baudRate);

                    using var connection = _serialPortFactory.Open(port, baudRate);
                    Thread.Sleep(200);
                    try { if (connection.BytesAvailable > 0) connection.ReadExisting(); } catch { }

                    string generatorType;

                    if (baudRate == 115200)
                    {
                        var challenge = GeneratorAuthentication.GenerateChallenge();
                        var challengeCmd = $":r90={challenge},";
                        _logger.LogDebug("  Sending challenge: {Cmd}", challengeCmd);
                        var challengeResponse = SendOnConnection(connection, challengeCmd);

                        if (string.IsNullOrEmpty(challengeResponse))
                        {
                            _logger.LogDebug("  No response on {Port} at {Baud}", port, baudRate);
                            continue;
                        }

                        _logger.LogInformation("  Generator responded on {Port} at {Baud}: {Response}", port, baudRate, challengeResponse);

                        var responseBody = challengeResponse.TrimStart(':');
                        if (!responseBody.StartsWith("r90=")) continue;
                        responseBody = responseBody.Substring(4).TrimEnd('.');

                        var parts = responseBody.Split(',');
                        if (parts.Length != 2 || parts[0].Length != 9 || parts[1].Length != 9) continue;

                        var echo = parts[0];
                        var deviceResp = parts[1];

                        var expectedEcho = GeneratorAuthentication.ComputeEcho(challenge, deviceResp);
                        if (expectedEcho != echo)
                            _logger.LogWarning("  Echo mismatch: expected={Expected} got={Got}", expectedEcho, echo);

                        var authToken = GeneratorAuthentication.ComputeAuthToken(challenge, deviceResp);
                        var authCmd = $":w92={authToken}.";
                        var authResponse = SendOnConnection(connection, authCmd);

                        if (authResponse == null || !authResponse.Contains("ok"))
                        {
                            _logger.LogWarning("  Auth failed on {Port}", port);
                            continue;
                        }

                        _logger.LogInformation("  Authenticated on {Port}!", port);

                        // Full init sequence matching original Spooky2 line-by-line
                        // (verified from Data/LatestComparison/OldSpooky dump)
                        SendOnConnection(connection, GeneratorProtocol.ReadHardwareInfo);       // :r02=0,
                        SendOnConnection(connection, GeneratorProtocol.QueryFirmwareName);       // :n00=$
                        SendOnConnection(connection, GeneratorProtocol.BuildSyncOnOff(false));   // :w14=0,
                        SendOnConnection(connection, GeneratorProtocol.BuildWaveformInversion(false, false)); // :w17=0,0,
                        SendOnConnection(connection, $":w24=0,");                               // :w24=0,
                        SendOnConnection(connection, $":w25=0,");                               // :w25=0,
                        SendOnConnection(connection, GeneratorProtocol.BuildLowFrequencyMode(true, true)); // :w15=1,1, CRITICAL!
                        SendOnConnection(connection, $":w24=00,");                              // :w24=00,
                        SendOnConnection(connection, GeneratorProtocol.BuildSetAmplitude1(120)); // :w32=120,
                        SendOnConnection(connection, GeneratorProtocol.BuildSetAmplitude2(120)); // :w33=120,
                        SendOnConnection(connection, GeneratorProtocol.BuildSetDisplayName("Stopped")); // :n00=...Stopped
                        SendOnConnection(connection, $":w13=0,");                               // modulation off
                        SendOnConnection(connection, $":w28=0,");                               // amplitude cv1 = 0
                        SendOnConnection(connection, $":w29=0,");                               // amplitude cv2 = 0
                        SendOnConnection(connection, $":w24=00,");                              // freq 0
                        SendOnConnection(connection, GeneratorProtocol.ClearFrequency1);        // :w12=0,,
                        SendOnConnection(connection, GeneratorProtocol.ClearFrequency2);        // :w12=,0,
                        SendOnConnection(connection, GeneratorProtocol.BuildSetAmplitude1(120)); // :w32=120,
                        SendOnConnection(connection, $":w40=0,");                               // duty cycle 0
                        SendOnConnection(connection, GeneratorProtocol.BuildSetAmplitude2(120)); // :w33=120,
                        SendOnConnection(connection, $":w40=0,");                               // duty cycle 0
                        SendOnConnection(connection, $":w13=0,");                               // modulation off
                        SendOnConnection(connection, $":w20=11,");                              // waveform 1 = sine
                        SendOnConnection(connection, GeneratorProtocol.BuildSyncOnOff(true));    // :w14=1, sync ON
                        SendOnConnection(connection, GeneratorProtocol.ClearFrequency1);        // :w12=0,,
                        SendOnConnection(connection, GeneratorProtocol.ClearFrequency2);        // :w12=,0,
                        SendOnConnection(connection, $":w21=25,");                              // waveform 2 = inverse

                        generatorType = "GeneratorX";
                    }
                    else
                    {
                        var pingResponse = SendOnConnection(connection, GeneratorProtocol.ActionPing);
                        if (string.IsNullOrEmpty(pingResponse))
                        {
                            _logger.LogDebug("  No response on {Port} at {Baud}", port, baudRate);
                            continue;
                        }

                        SendOnConnection(connection, GeneratorProtocol.ActionHandshake);
                        SendOnConnection(connection, GeneratorProtocol.ReadHardwareType);
                        SendOnConnection(connection, GeneratorProtocol.ReadFirmwareVersion);
                        SendOnConnection(connection, GeneratorProtocol.ReadSerialNumber);

                        generatorType = "XM";
                    }

                    var genId = foundGenerators.Count;
                    var state = new GeneratorState
                    {
                        Id = genId,
                        Port = port,
                        Status = GeneratorStatus.Idle,
                        CurrentFrequency = 0,
                        CurrentProgram = generatorType,
                        ElapsedTime = TimeSpan.Zero
                    };

                    foundGenerators.Add(state);
                    _portConfigs[genId] = new PortConfig(port, baudRate);
                    _logger.LogInformation("  Registered as Generator {Id} ({Type}) on {Port}", genId, generatorType, port);
                    break;
                }
                catch (Exception ex)
                {
                    _logger.LogDebug("  Failed to probe {Port} at {Baud}: {Error}", port, baudRate, ex.Message);
                }
            }
        }

        _generatorStates.Clear();
        _loadedFrequencies.Clear();
        _startTimes.Clear();
        CloseAllPorts();
        foreach (var state in foundGenerators)
        {
            _generatorStates[state.Id] = state;
            _loadedFrequencies[state.Id] = new List<double>();
        }

        _logger.LogInformation("Discovery complete: {Count} generator(s) found", foundGenerators.Count);
        return foundGenerators;
    }

    public async Task Start(int generatorId)
    {
        ValidateGeneratorExists(generatorId);

        SendCommand(generatorId, GeneratorProtocol.BuildModulationOnOff(false, false));
        SendCommand(generatorId, GeneratorProtocol.ClearFrequency1);
        SendCommand(generatorId, GeneratorProtocol.ClearFrequency2);

        _startTimes[generatorId] = DateTime.UtcNow;
        _generatorStates[generatorId] = _generatorStates[generatorId] with
        {
            Status = GeneratorStatus.Running,
            ElapsedTime = TimeSpan.Zero
        };

        SendCommand(generatorId, GeneratorProtocol.StartOutput1);
        SendCommand(generatorId, GeneratorProtocol.StartOutput2);
        await Task.CompletedTask;
    }

    public async Task Stop(int generatorId)
    {
        ValidateGeneratorExists(generatorId);

        _startTimes.Remove(generatorId);
        _generatorStates[generatorId] = _generatorStates[generatorId] with
        {
            Status = GeneratorStatus.Idle,
            ElapsedTime = TimeSpan.Zero,
            CurrentFrequency = 0
        };

        SendCommand(generatorId, GeneratorProtocol.StopOutput1);
        SendCommand(generatorId, GeneratorProtocol.StopOutput2);
        SendCommand(generatorId, GeneratorProtocol.BuildModulationOnOff(false, false));
        SendCommand(generatorId, GeneratorProtocol.ClearFrequency1);
        SendCommand(generatorId, GeneratorProtocol.ClearFrequency2);
        await Task.CompletedTask;
    }

    public async Task Pause(int generatorId)
    {
        ValidateGeneratorExists(generatorId);
        var elapsed = CalculateElapsed(generatorId);
        _generatorStates[generatorId] = _generatorStates[generatorId] with
        {
            Status = GeneratorStatus.Paused,
            ElapsedTime = elapsed
        };
        SendCommand(generatorId, GeneratorProtocol.StopOutput1);
        SendCommand(generatorId, GeneratorProtocol.StopOutput2);
        await Task.CompletedTask;
    }

    public async Task Hold(int generatorId)
    {
        ValidateGeneratorExists(generatorId);
        var elapsed = CalculateElapsed(generatorId);
        _generatorStates[generatorId] = _generatorStates[generatorId] with
        {
            Status = GeneratorStatus.Held,
            ElapsedTime = elapsed
        };
        await Task.CompletedTask;
    }

    public async Task Resume(int generatorId)
    {
        ValidateGeneratorExists(generatorId);
        _startTimes[generatorId] = DateTime.UtcNow;
        _generatorStates[generatorId] = _generatorStates[generatorId] with { Status = GeneratorStatus.Running };
        SendCommand(generatorId, GeneratorProtocol.StartOutput1);
        SendCommand(generatorId, GeneratorProtocol.StartOutput2);
        await Task.CompletedTask;
    }

    public async Task WriteFrequencies(int generatorId, List<double> frequencies)
    {
        ValidateGeneratorExists(generatorId);
        ArgumentNullException.ThrowIfNull(frequencies);
        _loadedFrequencies[generatorId] = new List<double>(frequencies);

        // Only :w24 — sets BOTH channels. :w25 is NEVER used (verified from dump).
        foreach (var frequency in frequencies)
        {
            SendCommand(generatorId, GeneratorProtocol.BuildSetFrequency1(frequency));
        }

        if (frequencies.Count > 0)
        {
            _generatorStates[generatorId] = _generatorStates[generatorId] with { CurrentFrequency = frequencies[0] };
        }
        await Task.CompletedTask;
    }

    public Task<GeneratorState> ReadStatus(int generatorId)
    {
        if (!_generatorStates.TryGetValue(generatorId, out var state))
            state = new GeneratorState { Id = generatorId, Status = GeneratorStatus.Idle };

        if (state.Status == GeneratorStatus.Running && _startTimes.TryGetValue(generatorId, out var startTime))
            state = state with { ElapsedTime = DateTime.UtcNow - startTime };

        return Task.FromResult(state);
    }

    public async Task EraseMemory(int generatorId)
    {
        ValidateGeneratorExists(generatorId);
        _generatorStates[generatorId] = _generatorStates[generatorId] with
        {
            Status = GeneratorStatus.Idle, CurrentFrequency = 0,
            CurrentProgram = string.Empty, ElapsedTime = TimeSpan.Zero
        };
        _loadedFrequencies[generatorId] = new List<double>();
        _startTimes.Remove(generatorId);

        SendCommand(generatorId, GeneratorProtocol.StopOutput1);
        SendCommand(generatorId, GeneratorProtocol.StopOutput2);
        SendCommand(generatorId, GeneratorProtocol.AmplitudeOffChannel1);
        SendCommand(generatorId, GeneratorProtocol.AmplitudeOffChannel2);
        SendCommand(generatorId, GeneratorProtocol.BiasOff);
        SendCommand(generatorId, GeneratorProtocol.GateOff);
        SendCommand(generatorId, GeneratorProtocol.SpectrumModeOff);
        SendCommand(generatorId, GeneratorProtocol.DwellTimeZero);
        SendCommand(generatorId, GeneratorProtocol.BuildSetAmplitude1(120));
        SendCommand(generatorId, GeneratorProtocol.BuildSetAmplitude2(120));
        SendCommand(generatorId, GeneratorProtocol.ClearFrequency1);
        SendCommand(generatorId, GeneratorProtocol.ClearFrequency2);
        await Task.CompletedTask;
    }

    public async Task IdentifyGenerators()
    {
        await FindGenerators();
        foreach (var generatorId in _generatorStates.Keys.ToList())
        {
            SendCommand(generatorId, GeneratorProtocol.ReadDeviceInfo);
            SendCommand(generatorId, GeneratorProtocol.ReadHardwareInfo);
            SendCommand(generatorId, GeneratorProtocol.ReadHardwareCapability);
        }
    }

    public async Task SendRawCommand(int generatorId, string command)
    {
        ValidateGeneratorExists(generatorId);
        SendCommand(generatorId, command);
        await Task.CompletedTask;
    }

    public Task<string?> SendCommandWithResponse(int generatorId, string command)
    {
        ValidateGeneratorExists(generatorId);
        var response = SendCommand(generatorId, command);
        return Task.FromResult(response);
    }

    public Task SendCommandsBatch(int generatorId, IReadOnlyList<string> commands)
    {
        ValidateGeneratorExists(generatorId);
        var conn = GetOrOpenPort(generatorId);
        if (conn == null) return Task.CompletedTask;

        _logger.LogDebug("[GEN {Id}] Batch: {Count} commands", generatorId, commands.Count);
        foreach (var command in commands)
        {
            conn.Write(command + GeneratorProtocol.CommandTerminator);
            BlockingRead(conn);
        }
        // Flush any leftover responses so the next SendCommand starts clean
        Thread.Sleep(10);
        try { if (conn.BytesAvailable > 0) conn.ReadExisting(); } catch { }
        return Task.CompletedTask;
    }

    // ── Port management ──

    private ISerialPortConnection? GetOrOpenPort(int generatorId)
    {
        if (_openPorts.TryGetValue(generatorId, out var existing))
            return existing;

        if (!_portConfigs.TryGetValue(generatorId, out var config))
        {
            _logger.LogWarning("[GEN {Id}] No port config found", generatorId);
            return null;
        }

        try
        {
            var conn = _serialPortFactory.Open(config.PortName, config.BaudRate);
            Thread.Sleep(50);
            _openPorts[generatorId] = conn;
            return conn;
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "[GEN {Id}] Failed to open port", generatorId);
            return null;
        }
    }

    private void CloseAllPorts()
    {
        foreach (var conn in _openPorts.Values)
        {
            try { conn.Dispose(); } catch { }
        }
        _openPorts.Clear();
    }

    // ── Command sending ──

    private string? SendCommand(int generatorId, string command)
    {
        var conn = GetOrOpenPort(generatorId);
        if (conn == null) return null;

        try
        {
            _logger.LogDebug("[GEN {Id}] TX: {Command}", generatorId, command);

            // NO flush — ReadLine() handles synchronization via CRLF terminators.
            // Flushing here would discard valid responses during fast scan loops
            // and cause response desynchronization.

            conn.Write(command + GeneratorProtocol.CommandTerminator);

            var response = BlockingRead(conn);

            _logger.LogDebug("[GEN {Id}] RX: {Response}", generatorId, response);
            return response;
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "[GEN {Id}] Error sending: {Command}", generatorId, command);
            // Port might be dead, close it so next call reopens
            _openPorts.Remove(generatorId);
            try { conn.Dispose(); } catch { }
            return null;
        }
    }

    /// <summary>
    /// Blocking read — waits for CRLF-terminated response from generator.
    /// No polling needed. ReadLine blocks until \n arrives or timeout expires.
    /// Generator responds in &lt;1ms at 115200 baud, so this returns almost instantly.
    /// </summary>
    private static string? BlockingRead(ISerialPortConnection conn)
    {
        try
        {
            var line = conn.ReadLine(); // blocks until \n or timeout
            return line?.Trim();
        }
        catch { return null; }
    }

    /// <summary>For discovery probing — uses the old slow method with proper timeouts.</summary>
    private string? SendOnConnection(ISerialPortConnection connection, string command)
    {
        try { if (connection.BytesAvailable > 0) connection.ReadExisting(); } catch { }

        connection.Write(command + GeneratorProtocol.CommandTerminator);

        const int timeoutMs = 2000;
        var sw = System.Diagnostics.Stopwatch.StartNew();

        while (sw.ElapsedMilliseconds < timeoutMs)
        {
            Thread.Sleep(10);
            try
            {
                if (connection.BytesAvailable > 0)
                {
                    Thread.Sleep(20); // let full response arrive for multi-byte responses
                    var response = connection.ReadExisting()?.Trim();
                    if (!string.IsNullOrEmpty(response))
                        return response;
                }
            }
            catch { return null; }
        }

        _logger.LogDebug("    Timeout after {Ms}ms, no data received", timeoutMs);
        return null;
    }

    private void ValidateGeneratorExists(int generatorId)
    {
        if (!_generatorStates.ContainsKey(generatorId))
            throw new InvalidOperationException($"Generator {generatorId} not found. Call FindGenerators() first.");
    }

    private TimeSpan CalculateElapsed(int generatorId) =>
        _startTimes.TryGetValue(generatorId, out var startTime)
            ? DateTime.UtcNow - startTime
            : _generatorStates[generatorId].ElapsedTime;

    public void Dispose() => CloseAllPorts();
}
