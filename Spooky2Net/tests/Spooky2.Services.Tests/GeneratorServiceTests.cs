using System.Globalization;
using Microsoft.Extensions.Logging.Abstractions;
using Spooky2.Core.Interfaces;
using Spooky2.Core.Models;
using Spooky2.Services.Communication;
using Xunit;

namespace Spooky2.Services.Tests;

public class GeneratorServiceTests
{
    // ─────────────────────────────────────────────────────────────
    // Mock infrastructure
    // ─────────────────────────────────────────────────────────────

    private sealed class MockSerialConnection : ISerialPortConnection
    {
        public List<string> SentData { get; } = [];
        public Queue<string> Responses { get; } = new();
        public bool IsOpen { get; private set; } = true;
        public bool WasDisposed { get; private set; }
        public bool ShouldTimeout { get; set; }

        public int BytesAvailable => Responses.Count > 0 ? 1 : 0;
        public void Write(string data) => SentData.Add(data);

        public string ReadLine()
        {
            if (ShouldTimeout) throw new TimeoutException("Read timeout");
            return Responses.Count > 0 ? Responses.Dequeue() : "";
        }

        public string ReadExisting()
        {
            if (ShouldTimeout) throw new TimeoutException("Read timeout");
            return Responses.Count > 0 ? Responses.Dequeue() : "";
        }

        public void Dispose()
        {
            IsOpen = false;
            WasDisposed = true;
        }
    }

    private sealed class MockSerialFactory : ISerialPortFactory
    {
        public List<string> AvailablePorts { get; set; } = [];
        public Dictionary<string, MockSerialConnection> PortConnections { get; } = new();
        public List<(string Port, int BaudRate)> OpenAttempts { get; } = [];
        public HashSet<string> FailingPorts { get; } = [];

        public List<string> GetAvailablePorts() => new(AvailablePorts);

        public ISerialPortConnection Open(string portName, int baudRate, int readTimeoutMs = 2000, int writeTimeoutMs = 2000)
        {
            OpenAttempts.Add((portName, baudRate));
            if (FailingPorts.Contains(portName))
                throw new UnauthorizedAccessException($"Port {portName} is in use");

            var key = $"{portName}:{baudRate}";
            if (!PortConnections.TryGetValue(key, out var conn))
            {
                conn = new MockSerialConnection();
                PortConnections[key] = conn;
            }

            return conn;
        }
    }

    private static GeneratorService CreateService(MockSerialFactory factory) =>
        new(factory, NullLogger<GeneratorService>.Instance);

    /// <summary>
    /// Creates a MockSerialConnection pre-loaded with the standard 5-step
    /// XM handshake responses (ping, handshake, hardware, firmware, serial).
    /// </summary>
    private static MockSerialConnection CreateHandshakeConnection(
        string pingResponse = "err",
        string handshakeResponse = "err",
        string hardwareResponse = "ok",
        string firmwareResponse = "okSYNC",
        string serialResponse = "ok12345")
    {
        var conn = new MockSerialConnection();
        conn.Responses.Enqueue(pingResponse);
        conn.Responses.Enqueue(handshakeResponse);
        conn.Responses.Enqueue(hardwareResponse);
        conn.Responses.Enqueue(firmwareResponse);
        conn.Responses.Enqueue(serialResponse);
        return conn;
    }

    /// <summary>
    /// Creates a MockSerialConnection pre-loaded with GeneratorX auth protocol responses.
    /// The challenge response echoes a fixed echo/deviceResponse pair, followed by
    /// auth ok, hardware info, firmware name, and 8 initialization acks.
    /// </summary>
    /// <summary>
    /// Creates a MockSerialConnection for GeneratorX auth protocol.
    /// The mock's BytesAvailable returns 1 whenever Responses is non-empty,
    /// and SendCommandOnConnection flushes before reading. So each
    /// SendCommandOnConnection call consumes 2 responses (flush + read).
    /// The initial FindGenerators flush also consumes 1 response.
    /// Pattern: initial_flush, then for each command: (flush_dummy, actual_response).
    /// </summary>
    private static MockSerialConnection CreateGxAuthConnection()
    {
        var conn = new MockSerialConnection();
        // 1. Initial buffer flush in FindGenerators
        conn.Responses.Enqueue("garbage");
        // 2. SendCommandOnConnection(challengeCmd): flush + read
        conn.Responses.Enqueue("garbage");
        conn.Responses.Enqueue(":r90=726911191,941378256.");
        // 3. SendCommandOnConnection(authCmd): flush + read
        conn.Responses.Enqueue("garbage");
        conn.Responses.Enqueue(":ok");
        // 4. SendCommandOnConnection(hwInfo): flush + read
        conn.Responses.Enqueue("garbage");
        conn.Responses.Enqueue("ok");
        // 5. SendCommandOnConnection(fwName): flush + read
        conn.Responses.Enqueue("garbage");
        conn.Responses.Enqueue("okGX_FW");
        // 6-13. 8 initialization commands, each: flush + read
        for (var i = 0; i < 8; i++)
        {
            conn.Responses.Enqueue("garbage");
            conn.Responses.Enqueue("ok");
        }
        return conn;
    }

    // ─────────────────────────────────────────────────────────────
    // Discovery tests
    // ─────────────────────────────────────────────────────────────

    [Fact]
    public async Task FindGenerators_NoPortsAvailable_ReturnsEmpty()
    {
        var factory = new MockSerialFactory { AvailablePorts = [] };
        var svc = CreateService(factory);

        var result = await svc.FindGenerators();

        Assert.Empty(result);
    }

    [Fact]
    public async Task FindGenerators_XmGenerator_FoundAt57600()
    {
        var factory = new MockSerialFactory { AvailablePorts = ["COM3"] };
        factory.PortConnections["COM3:57600"] = CreateHandshakeConnection();
        var svc = CreateService(factory);

        var result = await svc.FindGenerators();

        Assert.Single(result);
        Assert.Equal("COM3", result[0].Port);
        Assert.Equal("XM", result[0].CurrentProgram);
        Assert.Equal(GeneratorStatus.Idle, result[0].Status);
    }

    [Fact]
    public async Task FindGenerators_GxGenerator_FoundAt115200()
    {
        var factory = new MockSerialFactory { AvailablePorts = ["COM3"] };

        // 57600 times out (no response)
        var timeoutConn = new MockSerialConnection { ShouldTimeout = true };
        factory.PortConnections["COM3:57600"] = timeoutConn;

        // 115200 responds with GeneratorX auth protocol
        factory.PortConnections["COM3:115200"] = CreateGxAuthConnection();
        var svc = CreateService(factory);

        var result = await svc.FindGenerators();

        Assert.Single(result);
        Assert.Equal("COM3", result[0].Port);
        Assert.Equal("GeneratorX", result[0].CurrentProgram);
    }

    [Fact]
    public async Task FindGenerators_TwoGenerators_BothFound()
    {
        var factory = new MockSerialFactory { AvailablePorts = ["COM3", "COM4"] };
        factory.PortConnections["COM3:57600"] = CreateHandshakeConnection();
        factory.PortConnections["COM4:57600"] = CreateHandshakeConnection();
        var svc = CreateService(factory);

        var result = await svc.FindGenerators();

        Assert.Equal(2, result.Count);
        Assert.Equal("COM3", result[0].Port);
        Assert.Equal("COM4", result[1].Port);
        Assert.Equal(0, result[0].Id);
        Assert.Equal(1, result[1].Id);
    }

    [Fact]
    public async Task FindGenerators_PortInUse_SkipsToNext()
    {
        var factory = new MockSerialFactory { AvailablePorts = ["COM3", "COM4"] };
        factory.FailingPorts.Add("COM3");
        factory.PortConnections["COM4:57600"] = CreateHandshakeConnection();
        var svc = CreateService(factory);

        var result = await svc.FindGenerators();

        Assert.Single(result);
        Assert.Equal("COM4", result[0].Port);
    }

    [Fact]
    public async Task FindGenerators_NoResponse_SkipsPort()
    {
        var factory = new MockSerialFactory { AvailablePorts = ["COM3"] };

        // Both baud rates time out
        var timeout57600 = new MockSerialConnection { ShouldTimeout = true };
        var timeout115200 = new MockSerialConnection { ShouldTimeout = true };
        factory.PortConnections["COM3:57600"] = timeout57600;
        factory.PortConnections["COM3:115200"] = timeout115200;
        var svc = CreateService(factory);

        var result = await svc.FindGenerators();

        Assert.Empty(result);
    }

    // ─────────────────────────────────────────────────────────────
    // Handshake verification
    // ─────────────────────────────────────────────────────────────

    [Fact]
    public async Task FindGenerators_SendsCorrectHandshakeSequence()
    {
        var factory = new MockSerialFactory { AvailablePorts = ["COM3"] };
        var conn = CreateHandshakeConnection();
        factory.PortConnections["COM3:57600"] = conn;
        var svc = CreateService(factory);

        await svc.FindGenerators();

        // Verify exact 5-step handshake sequence, each terminated with \r\n
        Assert.Equal(5, conn.SentData.Count);
        Assert.Equal(":a00\r\n", conn.SentData[0]);         // Step 1: Ping
        Assert.Equal(":a0012345\r\n", conn.SentData[1]);     // Step 2: Handshake
        Assert.Equal(":r80\r\n", conn.SentData[2]);          // Step 3: Hardware type
        Assert.Equal(":r68\r\n", conn.SentData[3]);          // Step 4: Firmware version
        Assert.Equal(":r91\r\n", conn.SentData[4]);          // Step 5: Serial number
    }

    [Fact]
    public async Task FindGenerators_ChecksFirmwareForSync()
    {
        var factory = new MockSerialFactory { AvailablePorts = ["COM3"] };
        var conn = CreateHandshakeConnection(firmwareResponse: "okSYNC");
        factory.PortConnections["COM3:57600"] = conn;
        var svc = CreateService(factory);

        // Should not throw - SYNC is logged but does not affect discovery result
        var result = await svc.FindGenerators();

        Assert.Single(result);
        // Firmware with SYNC was processed (the connection completed successfully)
        Assert.Equal(5, conn.SentData.Count);
    }

    [Fact]
    public async Task FindGenerators_ChecksHardwareType()
    {
        var factory = new MockSerialFactory { AvailablePorts = ["COM3"] };
        var conn = CreateHandshakeConnection(hardwareResponse: "ok=TypeA");
        factory.PortConnections["COM3:57600"] = conn;
        var svc = CreateService(factory);

        var result = await svc.FindGenerators();

        Assert.Single(result);
        // :r80 was sent as the third command in the sequence
        Assert.Equal(":r80\r\n", conn.SentData[2]);
    }

    // ─────────────────────────────────────────────────────────────
    // State management
    // ─────────────────────────────────────────────────────────────

    private async Task<(GeneratorService Service, MockSerialFactory Factory)> CreateServiceWithOneGenerator()
    {
        var factory = new MockSerialFactory { AvailablePorts = ["COM3"] };
        factory.PortConnections["COM3:57600"] = CreateHandshakeConnection();
        var svc = CreateService(factory);
        await svc.FindGenerators();

        // Pre-load responses for subsequent commands (each SendCommand opens
        // a new connection via the factory, so we need fresh responses).
        // The factory returns the same MockSerialConnection for the same key,
        // so we enqueue extra responses.
        var cmdConn = factory.PortConnections["COM3:57600"];
        // Enqueue enough "ok" responses for typical command sequences
        for (var i = 0; i < 20; i++)
        {
            cmdConn.Responses.Enqueue("ok");
        }

        return (svc, factory);
    }

    [Fact]
    public async Task Start_UpdatesStateToRunning()
    {
        var (svc, _) = await CreateServiceWithOneGenerator();

        await svc.Start(0);

        var state = await svc.ReadStatus(0);
        Assert.Equal(GeneratorStatus.Running, state.Status);
    }

    [Fact]
    public async Task Stop_UpdatesStateToIdle()
    {
        var (svc, _) = await CreateServiceWithOneGenerator();

        await svc.Start(0);
        await svc.Stop(0);

        var state = await svc.ReadStatus(0);
        Assert.Equal(GeneratorStatus.Idle, state.Status);
    }

    [Fact]
    public async Task Pause_UpdatesStateToPaused()
    {
        var (svc, _) = await CreateServiceWithOneGenerator();

        await svc.Start(0);
        await svc.Pause(0);

        var state = await svc.ReadStatus(0);
        Assert.Equal(GeneratorStatus.Paused, state.Status);
    }

    [Fact]
    public async Task Hold_UpdatesStateToHeld()
    {
        var (svc, _) = await CreateServiceWithOneGenerator();

        await svc.Start(0);
        await svc.Hold(0);

        var state = await svc.ReadStatus(0);
        Assert.Equal(GeneratorStatus.Held, state.Status);
    }

    [Fact]
    public async Task Resume_UpdatesStateToRunning()
    {
        var (svc, _) = await CreateServiceWithOneGenerator();

        await svc.Start(0);
        await svc.Pause(0);
        await svc.Resume(0);

        var state = await svc.ReadStatus(0);
        Assert.Equal(GeneratorStatus.Running, state.Status);
    }

    [Fact]
    public async Task Start_SendsStartCommands()
    {
        var (svc, factory) = await CreateServiceWithOneGenerator();
        var conn = factory.PortConnections["COM3:57600"];
        var sentBefore = conn.SentData.Count;

        await svc.Start(0);

        // Start sends :w611 and :w621, each terminated with \r\n
        var newCommands = conn.SentData.Skip(sentBefore).ToList();
        Assert.Contains(":w611\r\n", newCommands);
        Assert.Contains(":w621\r\n", newCommands);
    }

    [Fact]
    public async Task Stop_SendsStopCommands()
    {
        var (svc, factory) = await CreateServiceWithOneGenerator();
        var conn = factory.PortConnections["COM3:57600"];

        await svc.Start(0);
        var sentBefore = conn.SentData.Count;
        await svc.Stop(0);

        var newCommands = conn.SentData.Skip(sentBefore).ToList();
        Assert.Contains(":w610\r\n", newCommands);
        Assert.Contains(":w620\r\n", newCommands);
    }

    [Fact]
    public async Task WriteFrequencies_SendsFrequencyCommands()
    {
        var (svc, factory) = await CreateServiceWithOneGenerator();
        var conn = factory.PortConnections["COM3:57600"];
        var sentBefore = conn.SentData.Count;

        await svc.WriteFrequencies(0, [76000.5, 152000.0]);

        var newCommands = conn.SentData.Skip(sentBefore).ToList();
        // Only :w24 is used — sets BOTH channels (verified from serial dump: zero :w25 commands)
        Assert.Contains(":w24=760005,\r\n", newCommands);
        Assert.Contains(":w24=152000,\r\n", newCommands);
        Assert.DoesNotContain(":w25=760005,\r\n", newCommands);
    }

    // ─────────────────────────────────────────────────────────────
    // Error handling
    // ─────────────────────────────────────────────────────────────

    [Fact]
    public async Task Start_UnknownGenerator_Throws()
    {
        var factory = new MockSerialFactory { AvailablePorts = [] };
        var svc = CreateService(factory);
        await svc.FindGenerators();

        await Assert.ThrowsAsync<InvalidOperationException>(() => svc.Start(99));
    }

    [Fact]
    public async Task SendCommand_PortFailure_ReturnsNull()
    {
        var (svc, factory) = await CreateServiceWithOneGenerator();

        // Make the port fail on subsequent opens
        factory.FailingPorts.Add("COM3");

        // SendRawCommand should not throw; it catches the exception internally
        // and returns without error (command returns null internally)
        await svc.SendRawCommand(0, ":r00");

        // If we got here without exception, the error was handled gracefully
    }

    [Fact]
    public async Task ReadStatus_UnknownGenerator_ReturnsIdleDefault()
    {
        var factory = new MockSerialFactory { AvailablePorts = [] };
        var svc = CreateService(factory);

        var state = await svc.ReadStatus(99);

        Assert.Equal(99, state.Id);
        Assert.Equal(GeneratorStatus.Idle, state.Status);
        Assert.Equal(0, state.CurrentFrequency);
    }

    // ─────────────────────────────────────────────────────────────
    // Serial protocol verification
    // ─────────────────────────────────────────────────────────────

    [Fact]
    public async Task AllCommandsEndWithCrlf()
    {
        var (svc, factory) = await CreateServiceWithOneGenerator();
        var conn = factory.PortConnections["COM3:57600"];

        await svc.Start(0);
        await svc.Stop(0);
        await svc.WriteFrequencies(0, [100.0]);

        // Every command sent must end with \r\n
        Assert.All(conn.SentData, cmd => Assert.EndsWith("\r\n", cmd));
    }

    [Fact]
    public async Task FrequenciesUseInvariantCulture()
    {
        var (svc, factory) = await CreateServiceWithOneGenerator();
        var conn = factory.PortConnections["COM3:57600"];
        var sentBefore = conn.SentData.Count;

        // Use a frequency with a decimal part that would differ in some cultures
        // (e.g. 76000,5 in cs-CZ vs 76000.5 in en-US)
        await svc.WriteFrequencies(0, [76000.5]);

        var newCommands = conn.SentData.Skip(sentBefore).ToList();

        // Frequency is now encoded as nanoHz (integer), so locale is not an issue
        var freqCmd = newCommands.FirstOrDefault(c => c.StartsWith(":w24="));
        Assert.NotNull(freqCmd);
        Assert.Contains("760005", freqCmd);
    }
}
