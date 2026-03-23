using Microsoft.Extensions.Logging;
using Spooky2.Core.Interfaces;
using Spooky2.Core.Models;

namespace Spooky2.Services.Communication;

/// <summary>
/// Generator control service managing state for discovered Spooky2 generators.
/// Communicates via serial ports (COM ports), matching the VB6 discovery flow
/// (Proc_0_214/Proc_0_213). Generator types:
///   - Type 1 (XM): 57600 baud, 8N1
///   - Type 2 (GeneratorX): 115200 baud, 8N1
///   - Type 3 (GeneratorX Pro): 115200 baud, 8N1
/// </summary>
public sealed class GeneratorService : IGeneratorService
{
    private const int CommandDelayMs = 50;

    private readonly ISerialPortFactory _serialPortFactory;
    private readonly ILogger<GeneratorService> _logger;
    private readonly Dictionary<int, GeneratorState> _generatorStates = new();
    private readonly Dictionary<int, List<double>> _loadedFrequencies = new();
    private readonly Dictionary<int, DateTime> _startTimes = new();
    private readonly Dictionary<int, PortConfig> _portConfigs = new();

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
            // Try XM baud rate first (57600), then GX (115200)
            foreach (var baudRate in new[] { 57600, 115200 })
            {
                try
                {
                    _logger.LogDebug("Probing {Port} at {Baud} baud...", port, baudRate);

                    using var connection = _serialPortFactory.Open(port, baudRate);

                    // Wait for DTR/RTS to settle — CH342 USB-serial needs time
                    Thread.Sleep(200);

                    // Flush any garbage in the buffer
                    try { if (connection.BytesAvailable > 0) connection.ReadExisting(); } catch { }

                    // Determine generator type
                    string generatorType;

                    if (baudRate == 115200)
                    {
                        // GeneratorX authentication protocol
                        // Step 1: Generate challenge (permutation of 1-9)
                        var challenge = GeneratorAuthentication.GenerateChallenge();
                        var challengeCmd = $":r90={challenge},";
                        _logger.LogDebug("  Sending challenge: {Cmd}", challengeCmd);
                        var challengeResponse = SendCommandOnConnection(connection, challengeCmd);

                        if (string.IsNullOrEmpty(challengeResponse))
                        {
                            _logger.LogDebug("  No response on {Port} at {Baud}", port, baudRate);
                            continue;
                        }

                        _logger.LogInformation("  Generator responded on {Port} at {Baud}: {Response}", port, baudRate, challengeResponse);

                        // Step 2: Parse response - format is ":r90=ECHO,DEVICE_RESPONSE."
                        var responseBody = challengeResponse.TrimStart(':');
                        if (!responseBody.StartsWith("r90=")) continue;
                        responseBody = responseBody.Substring(4).TrimEnd('.');

                        var parts = responseBody.Split(',');
                        if (parts.Length != 2 || parts[0].Length != 9 || parts[1].Length != 9) continue;

                        var echo = parts[0];
                        var deviceResp = parts[1];

                        // Verify echo
                        var expectedEcho = GeneratorAuthentication.ComputeEcho(challenge, deviceResp);
                        if (expectedEcho != echo)
                            _logger.LogWarning("  Echo mismatch: expected={Expected} got={Got}", expectedEcho, echo);
                        else
                            _logger.LogDebug("  Echo verified OK");

                        // Step 3: Compute auth token (uses only challenge + deviceResp, not echo)
                        var authToken = GeneratorAuthentication.ComputeAuthToken(challenge, deviceResp);
                        var authCmd = $":w92={authToken}.";
                        _logger.LogDebug("  Sending auth token: {Cmd}", authCmd);
                        var authResponse = SendCommandOnConnection(connection, authCmd);
                        _logger.LogDebug("  Auth response: {Response}", authResponse);

                        if (authResponse == null || !authResponse.Contains("ok"))
                        {
                            _logger.LogWarning("  Auth failed on {Port}", port);
                            continue;
                        }

                        _logger.LogInformation("  Authenticated on {Port}!", port);

                        // Step 4: Read hardware info
                        var hwResponse = SendCommandOnConnection(connection, GeneratorProtocol.ReadHardwareInfo);
                        _logger.LogDebug("  Hardware: {Response}", hwResponse);

                        // Step 5: Read firmware name
                        var fwResponse = SendCommandOnConnection(connection, GeneratorProtocol.QueryFirmwareName);
                        _logger.LogDebug("  Firmware: {Response}", fwResponse);

                        // Step 6: Initialize generator
                        SendCommandOnConnection(connection, GeneratorProtocol.BuildSyncOnOff(false));
                        SendCommandOnConnection(connection, GeneratorProtocol.BuildWaveformInversion(false, false));
                        SendCommandOnConnection(connection, GeneratorProtocol.BuildSetFrequency1(0));
                        SendCommandOnConnection(connection, GeneratorProtocol.BuildSetFrequency2(0));
                        SendCommandOnConnection(connection, GeneratorProtocol.BuildLowFrequencyMode(true, true));
                        SendCommandOnConnection(connection, $":w24=00,");
                        SendCommandOnConnection(connection, GeneratorProtocol.BuildSetAmplitude1(120));
                        SendCommandOnConnection(connection, GeneratorProtocol.BuildSetAmplitude2(120));

                        generatorType = "GeneratorX";
                    }
                    else
                    {
                        // XM legacy handshake protocol
                        // Step 1: Ping with :a00
                        var pingResponse = SendCommandOnConnection(connection, GeneratorProtocol.ActionPing);
                        if (string.IsNullOrEmpty(pingResponse))
                        {
                            _logger.LogDebug("  No response on {Port} at {Baud}", port, baudRate);
                            continue;
                        }
                        _logger.LogInformation("  Generator found on {Port} at {Baud}! Ping response: {Response}", port, baudRate, pingResponse);

                        // Step 2: Handshake with :a0012345
                        var handshakeResponse = SendCommandOnConnection(connection, GeneratorProtocol.ActionHandshake);
                        _logger.LogDebug("  Handshake response: {Response}", handshakeResponse);

                        // Step 3: Read hardware type :r80
                        var hwResponse = SendCommandOnConnection(connection, GeneratorProtocol.ReadHardwareType);
                        _logger.LogDebug("  Hardware type: {Response}", hwResponse);

                        // Step 4: Read firmware :r68
                        var fwResponse = SendCommandOnConnection(connection, GeneratorProtocol.ReadFirmwareVersion);
                        var hasSync = fwResponse?.Contains("SYNC") ?? false;
                        _logger.LogDebug("  Firmware: {Response} (Sync={HasSync})", fwResponse, hasSync);

                        // Step 5: Read serial :r91
                        var serialResponse = SendCommandOnConnection(connection, GeneratorProtocol.ReadSerialNumber);
                        _logger.LogDebug("  Serial: {Response}", serialResponse);

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

                    // Store connection details for later use
                    _portConfigs[genId] = new PortConfig(port, baudRate);

                    _logger.LogInformation("  Registered as Generator {Id} ({Type}) on {Port}", genId, generatorType, port);

                    break; // Found on this port, don't try other baud rates
                }
                catch (Exception ex)
                {
                    _logger.LogDebug("  Failed to probe {Port} at {Baud}: {Error}", port, baudRate, ex.Message);
                }
            }
        }

        // Update internal state
        _generatorStates.Clear();
        _loadedFrequencies.Clear();
        _startTimes.Clear();
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

        _startTimes[generatorId] = DateTime.UtcNow;
        _generatorStates[generatorId] = _generatorStates[generatorId] with
        {
            Status = GeneratorStatus.Running,
            ElapsedTime = TimeSpan.Zero
        };

        // Start output 1 (:w611) and output 2 (:w621) - Main.frm:37615, 37667
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

        // Stop output 1 (:w610) and output 2 (:w620) - Main.frm:37772, 37794
        SendCommand(generatorId, GeneratorProtocol.StopOutput1);
        SendCommand(generatorId, GeneratorProtocol.StopOutput2);
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

        // Pause is implemented as stopping both outputs
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

        // Hold keeps outputs running but freezes frequency progression;
        // no stop command is sent - the frequency timer is paused in state only.
        // Outputs remain active so the current frequency continues to be emitted.
        await Task.CompletedTask;
    }

    public async Task Resume(int generatorId)
    {
        ValidateGeneratorExists(generatorId);

        _startTimes[generatorId] = DateTime.UtcNow;
        _generatorStates[generatorId] = _generatorStates[generatorId] with
        {
            Status = GeneratorStatus.Running
        };

        // Resume by restarting both outputs
        SendCommand(generatorId, GeneratorProtocol.StartOutput1);
        SendCommand(generatorId, GeneratorProtocol.StartOutput2);
        await Task.CompletedTask;
    }

    public async Task WriteFrequencies(int generatorId, List<double> frequencies)
    {
        ValidateGeneratorExists(generatorId);
        ArgumentNullException.ThrowIfNull(frequencies);

        _loadedFrequencies[generatorId] = new List<double>(frequencies);

        // Write each frequency pair to both outputs using :w24 (out1) and :w25 (out2)
        // Main.frm:43518, 43540, 57198, 57357, 57360
        foreach (var frequency in frequencies)
        {
            var cmd1 = GeneratorProtocol.BuildSetFrequency1(frequency);
            var cmd2 = GeneratorProtocol.BuildSetFrequency2(frequency);
            SendCommand(generatorId, cmd1);
            SendCommand(generatorId, cmd2);
        }

        if (frequencies.Count > 0)
        {
            _generatorStates[generatorId] = _generatorStates[generatorId] with
            {
                CurrentFrequency = frequencies[0]
            };
        }

        await Task.CompletedTask;
    }

    public Task<GeneratorState> ReadStatus(int generatorId)
    {
        if (!_generatorStates.TryGetValue(generatorId, out var state))
        {
            state = new GeneratorState
            {
                Id = generatorId,
                Status = GeneratorStatus.Idle
            };
        }

        // Update elapsed time if currently running
        if (state.Status == GeneratorStatus.Running && _startTimes.TryGetValue(generatorId, out var startTime))
        {
            state = state with
            {
                ElapsedTime = DateTime.UtcNow - startTime
            };
        }

        return Task.FromResult(state);
    }

    public async Task EraseMemory(int generatorId)
    {
        ValidateGeneratorExists(generatorId);

        _generatorStates[generatorId] = _generatorStates[generatorId] with
        {
            Status = GeneratorStatus.Idle,
            CurrentFrequency = 0,
            CurrentProgram = string.Empty,
            ElapsedTime = TimeSpan.Zero
        };

        _loadedFrequencies[generatorId] = new List<double>();
        _startTimes.Remove(generatorId);

        // Stop both outputs first
        SendCommand(generatorId, GeneratorProtocol.StopOutput1);
        SendCommand(generatorId, GeneratorProtocol.StopOutput2);

        // Turn off amplitudes on both channels
        SendCommand(generatorId, GeneratorProtocol.AmplitudeOffChannel1);
        SendCommand(generatorId, GeneratorProtocol.AmplitudeOffChannel2);

        // Turn off bias/offset
        SendCommand(generatorId, GeneratorProtocol.BiasOff);

        // Turn off gating
        SendCommand(generatorId, GeneratorProtocol.GateOff);

        // Reset spectrum mode (:w420)
        SendCommand(generatorId, GeneratorProtocol.SpectrumModeOff);

        // Reset dwell time
        SendCommand(generatorId, GeneratorProtocol.DwellTimeZero);

        // Set default amplitude (120) on both channels
        SendCommand(generatorId, GeneratorProtocol.BuildSetAmplitude1(120));
        SendCommand(generatorId, GeneratorProtocol.BuildSetAmplitude2(120));

        // Reset frequencies to 0
        SendCommand(generatorId, GeneratorProtocol.BuildSetFrequency1(0));
        SendCommand(generatorId, GeneratorProtocol.BuildSetFrequency2(0));

        await Task.CompletedTask;
    }

    public async Task IdentifyGenerators()
    {
        await FindGenerators();

        // Send identification queries to each discovered generator
        foreach (var generatorId in _generatorStates.Keys.ToList())
        {
            // Read device info (:r00) - Main.frm:43818
            SendCommand(generatorId, GeneratorProtocol.ReadDeviceInfo);

            // Read hardware info (:r02=0,) - Main.frm:43833
            SendCommand(generatorId, GeneratorProtocol.ReadHardwareInfo);

            // Read hardware capability (:r81) - Main.frm:43097
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

    /// <summary>
    /// Sends a text-based command to a generator over serial port and reads the response.
    /// Commands are ASCII-encoded with CRLF terminator, matching the VB6
    /// Proc_0_272 implementation.
    /// </summary>
    /// <param name="generatorId">The generator device index.</param>
    /// <param name="command">The command string (e.g. ":w611", ":r68").</param>
    /// <returns>The response string, or null if communication failed.</returns>
    private string? SendCommand(int generatorId, string command)
    {
        if (!_portConfigs.TryGetValue(generatorId, out var config))
        {
            _logger.LogWarning("[GEN {Id}] No port config found", generatorId);
            return null;
        }

        try
        {
            _logger.LogDebug("[GEN {Id}] TX: {Command}", generatorId, command);

            using var connection = _serialPortFactory.Open(config.PortName, config.BaudRate);
            var response = SendCommandOnConnection(connection, command);

            _logger.LogDebug("[GEN {Id}] RX: {Response}", generatorId, response);
            return response;
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "[GEN {Id}] Error sending: {Command}", generatorId, command);
            return null;
        }
    }

    private string? SendCommandOnConnection(ISerialPortConnection connection, string command)
    {
        // Flush any stale data in the buffer before sending
        try { if (connection.BytesAvailable > 0) connection.ReadExisting(); } catch { }

        var fullCommand = command + GeneratorProtocol.CommandTerminator;
        var txBytes = System.Text.Encoding.ASCII.GetBytes(fullCommand);
        _logger.LogDebug("    TX bytes [{Len}]: {Hex}",
            txBytes.Length, BitConverter.ToString(txBytes));

        connection.Write(fullCommand);

        // Wait for response — VB6 SComm32x reads from input buffer after a delay.
        // Poll the buffer for up to ReadTimeoutMs, checking every PollIntervalMs.
        const int readTimeoutMs = 2000;
        const int pollIntervalMs = 100;
        var elapsed = 0;

        while (elapsed < readTimeoutMs)
        {
            Thread.Sleep(pollIntervalMs);
            elapsed += pollIntervalMs;

            try
            {
                var avail = connection.BytesAvailable;
                if (avail > 0)
                {
                    _logger.LogDebug("    {Avail} byte(s) available after {Ms}ms", avail, elapsed);
                    // Small extra delay to let the full response arrive
                    Thread.Sleep(100);
                    var response = connection.ReadExisting();
                    var rxBytes = System.Text.Encoding.ASCII.GetBytes(response);
                    _logger.LogDebug("    RX bytes [{Len}]: {Hex}",
                        rxBytes.Length, BitConverter.ToString(rxBytes));
                    _logger.LogDebug("    RX text: '{Response}'", response.Trim());
                    var trimmed = response.Trim();
                    if (!string.IsNullOrEmpty(trimmed))
                        return trimmed;
                }
            }
            catch (Exception ex)
            {
                _logger.LogDebug("    Read error: {Error}", ex.Message);
                return null;
            }
        }

        _logger.LogDebug("    Timeout after {Ms}ms, no data received", readTimeoutMs);
        return null;
    }

    private void ValidateGeneratorExists(int generatorId)
    {
        if (!_generatorStates.ContainsKey(generatorId))
        {
            throw new InvalidOperationException(
                $"Generator {generatorId} not found. Call FindGenerators() first to discover available generators.");
        }
    }

    private TimeSpan CalculateElapsed(int generatorId)
    {
        if (_startTimes.TryGetValue(generatorId, out var startTime))
        {
            return DateTime.UtcNow - startTime;
        }

        return _generatorStates[generatorId].ElapsedTime;
    }
}
