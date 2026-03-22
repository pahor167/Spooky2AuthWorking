using Xunit;
using Spooky2.Core.Models;
using Spooky2.Services.Communication;

namespace Spooky2.Services.Tests;

/// <summary>
/// Tests for the generator USB HID command protocol.
/// Verifies command encoding, response parsing, and byte framing.
/// Concern: HID report sizing — commands must be properly encoded/decoded.
/// </summary>
public class GeneratorProtocolTests
{
    // ─────────────────────────────────────────────────────────────
    // Command string format tests
    // ─────────────────────────────────────────────────────────────

    [Fact]
    public void ActionPing_HasCorrectFormat()
    {
        Assert.Equal(":a00", GeneratorProtocol.ActionPing);
    }

    [Fact]
    public void ActionHandshake_HasCorrectFormat()
    {
        Assert.Equal(":a0012345", GeneratorProtocol.ActionHandshake);
    }

    [Fact]
    public void StartOutput1_HasCorrectFormat()
    {
        // Register 61, value 1
        Assert.Equal(":w611", GeneratorProtocol.StartOutput1);
    }

    [Fact]
    public void StopOutput1_HasCorrectFormat()
    {
        Assert.Equal(":w610", GeneratorProtocol.StopOutput1);
    }

    [Fact]
    public void StartOutput2_HasCorrectFormat()
    {
        Assert.Equal(":w621", GeneratorProtocol.StartOutput2);
    }

    [Fact]
    public void StopOutput2_HasCorrectFormat()
    {
        Assert.Equal(":w620", GeneratorProtocol.StopOutput2);
    }

    // ─────────────────────────────────────────────────────────────
    // Parameterized command builder tests
    // ─────────────────────────────────────────────────────────────

    [Fact]
    public void BuildSetFrequency1_FormatsCorrectly()
    {
        var cmd = GeneratorProtocol.BuildSetFrequency1(76000.5);
        Assert.Equal(":w24=76000.5,", cmd);
    }

    [Fact]
    public void BuildSetFrequency2_FormatsCorrectly()
    {
        var cmd = GeneratorProtocol.BuildSetFrequency2(152000);
        Assert.Equal(":w25=152000,", cmd);
    }

    [Fact]
    public void BuildSetAmplitude1_FormatsCorrectly()
    {
        var cmd = GeneratorProtocol.BuildSetAmplitude1(120);
        Assert.Equal(":w32=120,", cmd);
    }

    [Fact]
    public void BuildSetAmplitude2_FormatsCorrectly()
    {
        var cmd = GeneratorProtocol.BuildSetAmplitude2(120);
        Assert.Equal(":w33=120,", cmd);
    }

    [Fact]
    public void BuildOutputOnOff_BothOn()
    {
        var cmd = GeneratorProtocol.BuildOutputOnOff(true, true);
        Assert.Equal(":w11=1,1,", cmd);
    }

    [Fact]
    public void BuildOutputOnOff_BothOff()
    {
        var cmd = GeneratorProtocol.BuildOutputOnOff(false, false);
        Assert.Equal(":w11=0,0,", cmd);
    }

    [Fact]
    public void BuildOutputOnOff_Mixed()
    {
        var cmd = GeneratorProtocol.BuildOutputOnOff(true, false);
        Assert.Equal(":w11=1,0,", cmd);
    }

    [Fact]
    public void BuildGatingOnOff_FormatsCorrectly()
    {
        Assert.Equal(":w12=1,0,", GeneratorProtocol.BuildGatingOnOff(true, false));
    }

    [Fact]
    public void BuildModulationOnOff_FormatsCorrectly()
    {
        Assert.Equal(":w13=0,1,", GeneratorProtocol.BuildModulationOnOff(false, true));
    }

    [Fact]
    public void BuildSyncOnOff_FormatsCorrectly()
    {
        Assert.Equal(":w14=1,", GeneratorProtocol.BuildSyncOnOff(true));
        Assert.Equal(":w14=0,", GeneratorProtocol.BuildSyncOnOff(false));
    }

    [Fact]
    public void BuildLowFrequencyMode_FormatsCorrectly()
    {
        Assert.Equal(":w15=1,1,", GeneratorProtocol.BuildLowFrequencyMode(true, true));
    }

    [Fact]
    public void BuildWaveformInversion_FormatsCorrectly()
    {
        Assert.Equal(":w17=1,0,", GeneratorProtocol.BuildWaveformInversion(true, false));
    }

    [Fact]
    public void BuildSetWaveform1_FormatsCorrectly()
    {
        Assert.Equal(":w20=3", GeneratorProtocol.BuildSetWaveform1(3));
    }

    [Fact]
    public void BuildSetWaveform2_FormatsCorrectly()
    {
        Assert.Equal(":w21=5", GeneratorProtocol.BuildSetWaveform2(5));
    }

    [Fact]
    public void BuildCommand_Action()
    {
        var cmd = GeneratorProtocol.BuildCommand(CommandType.Action, "00");
        Assert.Equal(":a00", cmd);
    }

    [Fact]
    public void BuildCommand_ReadWithoutValue()
    {
        var cmd = GeneratorProtocol.BuildCommand(CommandType.Read, "68");
        Assert.Equal(":r68", cmd);
    }

    [Fact]
    public void BuildCommand_WriteWithValue()
    {
        var cmd = GeneratorProtocol.BuildCommand(CommandType.Write, "24", "76000,");
        Assert.Equal(":w24=76000,", cmd);
    }

    // ─────────────────────────────────────────────────────────────
    // Byte encoding tests (HID report framing concern)
    // ─────────────────────────────────────────────────────────────

    [Fact]
    public void EncodeCommandToBytes_AppendsClrf()
    {
        var bytes = GeneratorProtocol.EncodeCommandToBytes(":a00");
        var str = System.Text.Encoding.ASCII.GetString(bytes);

        Assert.EndsWith("\r\n", str);
    }

    [Fact]
    public void EncodeCommandToBytes_IsAscii()
    {
        var bytes = GeneratorProtocol.EncodeCommandToBytes(":w24=76000.5,");

        // All bytes should be valid ASCII (< 128)
        Assert.All(bytes, b => Assert.True(b < 128, $"Byte {b} is not valid ASCII"));
    }

    [Fact]
    public void EncodeCommandToBytes_CorrectLength()
    {
        var cmd = ":a00";
        var bytes = GeneratorProtocol.EncodeCommandToBytes(cmd);

        // cmd length + 2 bytes for \r\n
        Assert.Equal(cmd.Length + 2, bytes.Length);
    }

    [Fact]
    public void DecodeResponseFromBytes_StripsNullBytes()
    {
        // HID reports are often padded with null bytes
        var data = new byte[] { (byte)'o', (byte)'k', 0, 0, 0, 0, 0 };
        var result = GeneratorProtocol.DecodeResponseFromBytes(data);

        Assert.Equal("ok", result);
    }

    [Fact]
    public void DecodeResponseFromBytes_EmptyArray_ReturnsEmpty()
    {
        Assert.Equal(string.Empty, GeneratorProtocol.DecodeResponseFromBytes([]));
        Assert.Equal(string.Empty, GeneratorProtocol.DecodeResponseFromBytes(null!));
    }

    [Fact]
    public void DecodeResponseFromBytes_TrimsWhitespace()
    {
        var data = System.Text.Encoding.ASCII.GetBytes("  ok=value  ");
        var result = GeneratorProtocol.DecodeResponseFromBytes(data);

        Assert.Equal("ok=value", result);
    }

    // ─────────────────────────────────────────────────────────────
    // Response parsing tests
    // ─────────────────────────────────────────────────────────────

    [Fact]
    public void ParseResponse_OkWithoutValue()
    {
        var response = GeneratorProtocol.ParseResponse("ok");

        Assert.True(response.IsSuccess);
        Assert.Equal("ok", response.RawResponse);
        Assert.Equal(string.Empty, response.Value);
    }

    [Fact]
    public void ParseResponse_OkWithValue()
    {
        var response = GeneratorProtocol.ParseResponse("ok=76000.5");

        Assert.True(response.IsSuccess);
        Assert.Equal("76000.5", response.Value);
    }

    [Fact]
    public void ParseResponse_ErrorResponse()
    {
        var response = GeneratorProtocol.ParseResponse("err");

        Assert.False(response.IsSuccess);
    }

    [Fact]
    public void ParseResponse_EmptyResponse()
    {
        var response = GeneratorProtocol.ParseResponse("");

        Assert.False(response.IsSuccess);
    }

    [Fact]
    public void ParseResponse_NullResponse()
    {
        var response = GeneratorProtocol.ParseResponse(null!);

        Assert.False(response.IsSuccess);
    }

    [Fact]
    public void ParseResponse_ValueWithTrailingComma()
    {
        // Generator responses often have trailing commas
        var response = GeneratorProtocol.ParseResponse("ok=120,");

        Assert.True(response.IsSuccess);
        Assert.Equal("120", response.Value);
    }

    [Fact]
    public void ParseResponse_UnknownPrefix_TreatedAsSuccess()
    {
        // Some responses may not start with ok/err
        var response = GeneratorProtocol.ParseResponse("v2.34");

        Assert.True(response.IsSuccess);
    }

    // ─────────────────────────────────────────────────────────────
    // Round-trip: encode -> decode
    // ─────────────────────────────────────────────────────────────

    [Theory]
    [InlineData(":a00")]
    [InlineData(":r68")]
    [InlineData(":w24=76000.5,")]
    [InlineData(":w11=1,0,")]
    [InlineData(":w95=12021")]
    public void EncodeAndDecode_RoundTrip(string command)
    {
        var encoded = GeneratorProtocol.EncodeCommandToBytes(command);
        var decoded = GeneratorProtocol.DecodeResponseFromBytes(encoded);

        // After round-trip, we get the command back (minus CRLF which gets trimmed)
        Assert.Equal(command, decoded);
    }

    // ─────────────────────────────────────────────────────────────
    // Command constants completeness
    // ─────────────────────────────────────────────────────────────

    [Fact]
    public void AllReadCommands_StartWithColonR()
    {
        var readFields = typeof(GeneratorProtocol)
            .GetFields(System.Reflection.BindingFlags.Public | System.Reflection.BindingFlags.Static)
            .Where(f => f.Name.StartsWith("Read"))
            .Select(f => (Name: f.Name, Value: (string)f.GetValue(null)!))
            .ToList();

        Assert.NotEmpty(readFields);
        // ReadFirmwareName uses :n prefix (named register), not :r
        Assert.All(readFields, f =>
            Assert.True(f.Value.StartsWith(":r") || f.Value.StartsWith(":n"),
                $"{f.Name} = '{f.Value}' should start with :r or :n"));
    }

    [Fact]
    public void AllActionCommands_StartWithColonA()
    {
        var actionFields = typeof(GeneratorProtocol)
            .GetFields(System.Reflection.BindingFlags.Public | System.Reflection.BindingFlags.Static)
            .Where(f => f.Name.StartsWith("Action"))
            .Select(f => (string)f.GetValue(null)!)
            .ToList();

        Assert.NotEmpty(actionFields);
        Assert.All(actionFields, cmd => Assert.StartsWith(":a", cmd));
    }
}
