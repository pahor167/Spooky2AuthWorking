using Microsoft.Extensions.Logging;
using Spooky2.Core.Interfaces;

namespace Spooky2.Services.Communication;

/// <summary>
/// MicroGen serial port communication service.
/// Implements the same generator protocol as <see cref="GeneratorProtocol"/> but over
/// RS-232 serial instead of USB HID. Ported from VB6 SComm32x.ocx usage.
/// Serial settings: 57600,n,8,1 (low power) or 115200,n,8,1 (high power/zapper/blood purifier).
/// </summary>
public sealed class MicroGenService : IMicroGenService
{
    private const int LowPowerBaudRate = 57600;
    private const int HighPowerBaudRate = 115200;

    /// <summary>Command prefix for zapper mode.</summary>
    private const string ZapperPrefix = "Z";

    /// <summary>Command prefix for blood purifier mode.</summary>
    private const string BloodPurifierPrefix = "B";

    private readonly ISerialPortFactory _portFactory;
    private readonly ILogger<MicroGenService> _logger;

    public MicroGenService(ISerialPortFactory portFactory, ILogger<MicroGenService> logger)
    {
        _portFactory = portFactory;
        _logger = logger;
    }

    public Task<List<string>> FindSerialPorts()
    {
        var ports = _portFactory.GetAvailablePorts();
        _logger.LogInformation("Found {Count} serial port(s): {Ports}", ports.Count, string.Join(", ", ports));
        return Task.FromResult(ports);
    }

    public Task SendToLowPower(string port, List<double> frequencies, int dwellSeconds)
    {
        return SendFrequencies(port, LowPowerBaudRate, frequencies, dwellSeconds, commandPrefix: null);
    }

    public Task SendToHighPower(string port, List<double> frequencies, int dwellSeconds)
    {
        return SendFrequencies(port, HighPowerBaudRate, frequencies, dwellSeconds, commandPrefix: null);
    }

    public Task SendToZapper(string port, List<double> frequencies, int dwellSeconds)
    {
        return SendFrequencies(port, HighPowerBaudRate, frequencies, dwellSeconds, ZapperPrefix);
    }

    public Task SendToBloodPurifier(string port, List<double> frequencies, int dwellSeconds)
    {
        return SendFrequencies(port, HighPowerBaudRate, frequencies, dwellSeconds, BloodPurifierPrefix);
    }

    public Task Stop(string port)
    {
        return Task.Run(() =>
        {
            foreach (var baudRate in new[] { HighPowerBaudRate, LowPowerBaudRate })
            {
                try
                {
                    using var connection = _portFactory.Open(port, baudRate);
                    SendCommand(connection, GeneratorProtocol.StopOutput1);
                    SendCommand(connection, GeneratorProtocol.StopOutput2);
                    _logger.LogInformation("Stop command sent on port {Port} at {BaudRate} baud", port, baudRate);
                    return;
                }
                catch (Exception ex)
                {
                    _logger.LogWarning(ex, "Failed to send stop at {BaudRate} baud on port {Port}", baudRate, port);
                }
            }
            throw new InvalidOperationException($"Failed to send stop command on port {port}");
        });
    }

    private Task SendFrequencies(
        string port,
        int baudRate,
        List<double> frequencies,
        int dwellSeconds,
        string? commandPrefix)
    {
        if (frequencies is null || frequencies.Count == 0)
            throw new ArgumentException("At least one frequency is required.", nameof(frequencies));

        return Task.Run(() =>
        {
            using var connection = _portFactory.Open(port, baudRate);

            _logger.LogInformation(
                "Sending {Count} frequency(ies) to MicroGen on {Port} at {BaudRate} baud, dwell={Dwell}s, prefix={Prefix}",
                frequencies.Count, port, baudRate, dwellSeconds, commandPrefix ?? "(none)");

            SendCommand(connection, MaybePrefix(commandPrefix, GeneratorProtocol.BuildSetDwellTime($"{dwellSeconds},")));

            foreach (var frequency in frequencies)
            {
                SendCommand(connection, MaybePrefix(commandPrefix, GeneratorProtocol.BuildSetFrequency1(frequency)));
                SendCommand(connection, MaybePrefix(commandPrefix, GeneratorProtocol.BuildSetFrequency2(frequency)));
            }

            SendCommand(connection, MaybePrefix(commandPrefix, GeneratorProtocol.StartOutput1));
            SendCommand(connection, MaybePrefix(commandPrefix, GeneratorProtocol.StartOutput2));

            _logger.LogInformation("Frequencies sent successfully on port {Port}", port);
        });
    }

    private static string MaybePrefix(string? prefix, string command)
    {
        return prefix is null ? command : $"{prefix}{command}";
    }

    private void SendCommand(ISerialPortConnection connection, string command)
    {
        var fullCommand = command + GeneratorProtocol.CommandTerminator;
        connection.Write(fullCommand);
        _logger.LogDebug("TX: {Command}", command);
        Thread.Sleep(50);
    }
}
