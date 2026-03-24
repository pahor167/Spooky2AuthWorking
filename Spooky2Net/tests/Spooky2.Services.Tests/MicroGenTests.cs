using Xunit;
using Microsoft.Extensions.Logging.Abstractions;
using Spooky2.Core.Interfaces;
using Spooky2.Services.Communication;

namespace Spooky2.Services.Tests;

/// <summary>
/// Tests for MicroGen serial port communication WITHOUT real hardware.
/// Uses a mock serial port that captures all transmitted commands for verification.
/// </summary>
public class MicroGenTests
{
    // ─────────────────────────────────────────────────────────────
    // Mock serial port infrastructure
    // ─────────────────────────────────────────────────────────────

    private sealed class MockSerialPortConnection : ISerialPortConnection
    {
        public List<string> SentData { get; } = [];
        public bool IsOpen { get; private set; } = true;
        public bool WasDisposed { get; private set; }

        public int BytesAvailable => 1;
        public void Write(string data) => SentData.Add(data);
        public string ReadLine() => "ok";
        public string ReadExisting() => "ok";
        public void Dispose()
        {
            IsOpen = false;
            WasDisposed = true;
        }
    }

    private sealed class MockSerialPortFactory : ISerialPortFactory
    {
        public List<string> AvailablePorts { get; set; } = ["/dev/tty.usbmodem001"];
        public MockSerialPortConnection? LastConnection { get; private set; }
        public List<MockSerialPortConnection> AllConnections { get; } = [];
        public int? LastBaudRate { get; private set; }
        public string? LastPortName { get; private set; }

        public ISerialPortConnection Open(string portName, int baudRate, int readTimeoutMs = 2000, int writeTimeoutMs = 2000)
        {
            LastPortName = portName;
            LastBaudRate = baudRate;
            var connection = new MockSerialPortConnection();
            LastConnection = connection;
            AllConnections.Add(connection);
            return connection;
        }

        public List<string> GetAvailablePorts() => new(AvailablePorts);
    }

    private static (MicroGenService service, MockSerialPortFactory factory) CreateService()
    {
        var factory = new MockSerialPortFactory();
        var service = new MicroGenService(factory, NullLogger<MicroGenService>.Instance);
        return (service, factory);
    }

    // ─────────────────────────────────────────────────────────────
    // Port discovery
    // ─────────────────────────────────────────────────────────────

    [Fact]
    public async Task FindSerialPorts_ReturnsMockedPorts()
    {
        var (service, factory) = CreateService();
        factory.AvailablePorts = ["/dev/tty.usbmodem001", "/dev/tty.usbmodem002"];

        var ports = await service.FindSerialPorts();

        Assert.Equal(2, ports.Count);
        Assert.Contains("/dev/tty.usbmodem001", ports);
    }

    [Fact]
    public async Task FindSerialPorts_NoPorts_ReturnsEmpty()
    {
        var (service, factory) = CreateService();
        factory.AvailablePorts = [];

        var ports = await service.FindSerialPorts();

        Assert.Empty(ports);
    }

    // ─────────────────────────────────────────────────────────────
    // Low power mode (57600 baud)
    // ─────────────────────────────────────────────────────────────

    [Fact]
    public async Task SendToLowPower_UsesBaudRate57600()
    {
        var (service, factory) = CreateService();

        await service.SendToLowPower("/dev/tty.test", [440.0], 180);

        Assert.Equal(57600, factory.LastBaudRate);
    }

    [Fact]
    public async Task SendToLowPower_SendsDwellThenFrequenciesThenStart()
    {
        var (service, factory) = CreateService();

        await service.SendToLowPower("/dev/tty.test", [76000.0], 180);

        var sent = factory.LastConnection!.SentData;

        // Should send: dwell, freq1, freq2, start1, start2
        Assert.Equal(5, sent.Count);
        Assert.Contains(":w23=180,", sent[0]);     // Dwell time
        Assert.Contains(":w24=76000000,", sent[1]);    // Output 1 frequency (milliHz)
        Assert.Contains(":w25=76000000,", sent[2]);    // Output 2 frequency (milliHz)
        Assert.Contains(":w611", sent[3]);           // Start output 1
        Assert.Contains(":w621", sent[4]);           // Start output 2
    }

    [Fact]
    public async Task SendToLowPower_MultipleFrequencies_SendsAll()
    {
        var (service, factory) = CreateService();

        await service.SendToLowPower("/dev/tty.test", [100.0, 200.0, 300.0], 60);

        var sent = factory.LastConnection!.SentData;

        // 1 dwell + 3 frequencies * 2 outputs + 2 start = 9 commands
        Assert.Equal(9, sent.Count);
    }

    // ─────────────────────────────────────────────────────────────
    // High power mode (115200 baud)
    // ─────────────────────────────────────────────────────────────

    [Fact]
    public async Task SendToHighPower_UsesBaudRate115200()
    {
        var (service, factory) = CreateService();

        await service.SendToHighPower("/dev/tty.test", [440.0], 180);

        Assert.Equal(115200, factory.LastBaudRate);
    }

    [Fact]
    public async Task SendToHighPower_NoCommandPrefix()
    {
        var (service, factory) = CreateService();

        await service.SendToHighPower("/dev/tty.test", [440.0], 180);

        var sent = factory.LastConnection!.SentData;
        // No prefix — commands start with ":"
        Assert.All(sent, cmd => Assert.StartsWith(":", cmd));
    }

    // ─────────────────────────────────────────────────────────────
    // Zapper mode (prefix "Z")
    // ─────────────────────────────────────────────────────────────

    [Fact]
    public async Task SendToZapper_UsesBaudRate115200()
    {
        var (service, factory) = CreateService();

        await service.SendToZapper("/dev/tty.test", [440.0], 180);

        Assert.Equal(115200, factory.LastBaudRate);
    }

    [Fact]
    public async Task SendToZapper_PrefixesCommandsWithZ()
    {
        var (service, factory) = CreateService();

        await service.SendToZapper("/dev/tty.test", [440.0], 180);

        var sent = factory.LastConnection!.SentData;
        // All commands should start with "Z:"
        Assert.All(sent, cmd => Assert.StartsWith("Z:", cmd));
    }

    // ─────────────────────────────────────────────────────────────
    // Blood purifier mode (prefix "B")
    // ─────────────────────────────────────────────────────────────

    [Fact]
    public async Task SendToBloodPurifier_PrefixesCommandsWithB()
    {
        var (service, factory) = CreateService();

        await service.SendToBloodPurifier("/dev/tty.test", [440.0], 180);

        var sent = factory.LastConnection!.SentData;
        Assert.All(sent, cmd => Assert.StartsWith("B:", cmd));
    }

    // ─────────────────────────────────────────────────────────────
    // Stop command
    // ─────────────────────────────────────────────────────────────

    [Fact]
    public async Task Stop_SendsStopCommands()
    {
        var (service, factory) = CreateService();

        await service.Stop("/dev/tty.test");

        var sent = factory.LastConnection!.SentData;
        Assert.Equal(2, sent.Count);
        Assert.Contains(":w610", sent[0]); // Stop output 1
        Assert.Contains(":w620", sent[1]); // Stop output 2
    }

    // ─────────────────────────────────────────────────────────────
    // Command format verification
    // ─────────────────────────────────────────────────────────────

    [Fact]
    public async Task AllCommandsEndWithCrlf()
    {
        var (service, factory) = CreateService();

        await service.SendToLowPower("/dev/tty.test", [440.0], 180);

        var sent = factory.LastConnection!.SentData;
        Assert.All(sent, cmd => Assert.EndsWith("\r\n", cmd));
    }

    [Fact]
    public async Task FrequenciesUseInvariantCulture()
    {
        var (service, factory) = CreateService();

        // 76000.5 Hz should encode as 76000500 milliHz (integer, no locale issues)
        await service.SendToLowPower("/dev/tty.test", [76000.5], 180);

        var sent = factory.LastConnection!.SentData;
        var freqCmd = sent.First(s => s.Contains(":w24="));
        Assert.Contains("76000500", freqCmd);
    }

    // ─────────────────────────────────────────────────────────────
    // Connection lifecycle
    // ─────────────────────────────────────────────────────────────

    [Fact]
    public async Task Connection_IsDisposedAfterUse()
    {
        var (service, factory) = CreateService();

        await service.SendToLowPower("/dev/tty.test", [440.0], 180);

        Assert.True(factory.LastConnection!.WasDisposed);
    }

    [Fact]
    public async Task Connection_OpensCorrectPort()
    {
        var (service, factory) = CreateService();

        await service.SendToHighPower("/dev/tty.usbmodem42", [440.0], 180);

        Assert.Equal("/dev/tty.usbmodem42", factory.LastPortName);
    }

    // ─────────────────────────────────────────────────────────────
    // Validation
    // ─────────────────────────────────────────────────────────────

    [Fact]
    public async Task SendToLowPower_EmptyFrequencies_Throws()
    {
        var (service, _) = CreateService();

        await Assert.ThrowsAsync<ArgumentException>(
            () => service.SendToLowPower("/dev/tty.test", [], 180));
    }

    [Fact]
    public async Task SendToLowPower_NullFrequencies_Throws()
    {
        var (service, _) = CreateService();

        await Assert.ThrowsAsync<ArgumentException>(
            () => service.SendToLowPower("/dev/tty.test", null!, 180));
    }
}
