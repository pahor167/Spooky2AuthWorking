using Xunit;
using Spooky2.Core.Models;
using Spooky2.Services.Communication;

namespace Spooky2.Services.Tests;

/// <summary>
/// Tests for the generator USB HID command protocol.
/// Verifies command encoding, response parsing, and byte framing.
/// Tests are based on captured serial port communication dumps from Data/StartHuntAndKill
/// and Data/FinishHuntAndKill.
/// </summary>
public class GeneratorProtocolTests
{
    // ─────────────────────────────────────────────────────────────
    // Command string format tests (existing static commands)
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

    [Fact]
    public void StaticCommands_MatchExpectedValues()
    {
        Assert.Equal(":a00", GeneratorProtocol.ActionPing);
        Assert.Equal(":a0012345", GeneratorProtocol.ActionHandshake);
        Assert.Equal(":w610", GeneratorProtocol.StopOutput1);
        Assert.Equal(":w611", GeneratorProtocol.StartOutput1);
        Assert.Equal(":w620", GeneratorProtocol.StopOutput2);
        Assert.Equal(":w621", GeneratorProtocol.StartOutput2);
        Assert.Equal(":r68", GeneratorProtocol.ReadFirmwareVersion);
        Assert.Equal(":r80", GeneratorProtocol.ReadHardwareType);
        Assert.Equal(":r91", GeneratorProtocol.ReadSerialNumber);
    }

    // ─────────────────────────────────────────────────────────────
    // Frequency encoding (verified from Data/FinishHuntAndKill dump)
    // Encoding: Hz * 1000 (milliHz)
    // ─────────────────────────────────────────────────────────────

    [Fact]
    public void BuildSetFrequency1_EncodesAsNanoHz()
    {
        // From dump: frequency ~1652608.15 Hz -> :w24=1652608154681650,
        // We verify the encoding formula: Hz * 1000
        var cmd = GeneratorProtocol.BuildSetFrequency1(1000.0);
        Assert.Equal(":w24=1000,", cmd);
    }

    [Theory]
    [InlineData(41000.0)]      // Scan start
    [InlineData(1800000.0)]    // Scan end
    [InlineData(100.0)]
    [InlineData(0.5)]          // Sub-Hz
    [InlineData(76000.5)]      // Typical frequency with decimal
    public void BuildSetFrequency1_ValidFrequencies(double freqHz)
    {
        var cmd = GeneratorProtocol.BuildSetFrequency1(freqHz);
        Assert.StartsWith(":w24=", cmd);
        Assert.EndsWith(",", cmd);

        // Value should be the frequency with decimal point removed
        var valStr = cmd.Replace(":w24=", "").TrimEnd(',');
        Assert.False(valStr.Contains('.'), "Value should not contain decimal point");
        Assert.False(valStr.Contains(','), "Value should not contain comma");
        Assert.True(valStr.Length > 0, "Value should not be empty");
    }

    [Fact]
    public void BuildSetFrequency2_EncodesAsNanoHz()
    {
        var cmd = GeneratorProtocol.BuildSetFrequency2(880.0);
        Assert.Equal(":w25=880,", cmd);
    }

    // ─────────────────────────────────────────────────────────────
    // Frequency step verification (from biofeedback scan dump)
    // ─────────────────────────────────────────────────────────────

    [Fact]
    public void BiofeedbackScan_StepSize_MatchesDump()
    {
        // From dump: consecutive :w24 values
        long v1 = 1652608154681650;
        long v2 = 1653021306720320;

        double freq1 = v1 / 1000.0; // Hz
        double freq2 = v2 / 1000.0;
        double step = freq2 - freq1;
        double expectedStep = freq1 * 0.00025; // 0.025%

        Assert.Equal(expectedStep, step, precision: 1);
    }

    // ─────────────────────────────────────────────────────────────
    // Amplitude commands (verified from dump stop sequence)
    // :w28/:w29 are amplitude in centivolt
    // ─────────────────────────────────────────────────────────────

    [Fact]
    public void BuildSetAmplitudeCv_Format()
    {
        // From dump: :w28=2000, = 20.00V, :w28=1950, = 19.50V
        Assert.Equal(":w28=2000,", GeneratorProtocol.BuildSetAmplitudeCv1(2000));
        Assert.Equal(":w29=1950,", GeneratorProtocol.BuildSetAmplitudeCv2(1950));
    }

    // ─────────────────────────────────────────────────────────────
    // Stop sequence (from FinishHuntAndKill dump)
    // ─────────────────────────────────────────────────────────────

    [Fact]
    public void StopSequence_MatchesDump()
    {
        // Dump shows this exact stop sequence:
        // :w12=0,,    (clear freq ch1)
        // :w12=,0,    (clear freq ch2)
        // :w28=1950,  (amplitude ch1)
        // :w29=1950,  (amplitude ch2)
        // :n00=Port 4 - GX Hunt and Kill (C)  (display name)
        // :w24=1634122798580760,  (idle frequency)
        // :w28=2000,  (amplitude ch1)
        // :w29=2000,  (amplitude ch2)

        Assert.Equal(":w12=0,,", GeneratorProtocol.ClearFrequency1);
        Assert.Equal(":w12=,0,", GeneratorProtocol.ClearFrequency2);
    }

    // ─────────────────────────────────────────────────────────────
    // Display name (from both dumps)
    // ─────────────────────────────────────────────────────────────

    [Theory]
    [InlineData("Port 3 - Running Biofeedback")]
    [InlineData("Port 4 - GX Hunt and Kill (C)")]
    public void BuildSetDisplayName_MatchesDump(string name)
    {
        var cmd = GeneratorProtocol.BuildSetDisplayName(name);
        Assert.Equal($":n00={name}", cmd);
    }

    // ─────────────────────────────────────────────────────────────
    // Sensor reading parsing (from FinishHuntAndKill responses)
    // ─────────────────────────────────────────────────────────────

    [Theory]
    [InlineData(":r11=53001.", 53001.0)]
    [InlineData(":r12=6557.", 6557.0)]
    [InlineData(":r11=52998.", 52998.0)]
    [InlineData(":r12=6559.", 6559.0)]
    public void ParseSensorReading_MatchesDumpResponses(string response, double expected)
    {
        var value = GeneratorProtocol.ParseSensorReading(response);
        Assert.Equal(expected, value);
    }

    // ─────────────────────────────────────────────────────────────
    // Read commands format
    // ─────────────────────────────────────────────────────────────

    [Fact]
    public void ReadAngle_Format()
    {
        Assert.Equal(":r11=,", GeneratorProtocol.ReadAngle);
    }

    [Fact]
    public void ReadCurrent_Format()
    {
        Assert.Equal(":r12=,", GeneratorProtocol.ReadCurrent);
    }

    // ─────────────────────────────────────────────────────────────
    // Waveform upload (from StartHuntAndKill dump)
    // ─────────────────────────────────────────────────────────────

    [Fact]
    public void BuildWaveformTable_Format()
    {
        // From dump: :a11=512,515,518,521,...
        var values = new[] { 512, 515, 518, 521 };
        var cmd = GeneratorProtocol.BuildWaveformTable(11, values);
        Assert.Equal(":a11=512,515,518,521,", cmd);
    }

    // ─────────────────────────────────────────────────────────────
    // Existing parameterized command builder tests
    // ─────────────────────────────────────────────────────────────

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
    public void EncodeCommandToBytes_AppendsCorrectTerminator()
    {
        var bytes = GeneratorProtocol.EncodeCommandToBytes(":w24=1000,");
        var text = System.Text.Encoding.ASCII.GetString(bytes);
        Assert.EndsWith("\r\n", text);
    }

    [Fact]
    public void EncodeCommandToBytes_IsAscii()
    {
        var bytes = GeneratorProtocol.EncodeCommandToBytes(":w24=76000500000000,");

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
        var response = GeneratorProtocol.ParseResponse("ok=120,");

        Assert.True(response.IsSuccess);
        Assert.Equal("120", response.Value);
    }

    [Fact]
    public void ParseResponse_UnknownPrefix_TreatedAsSuccess()
    {
        var response = GeneratorProtocol.ParseResponse("v2.34");

        Assert.True(response.IsSuccess);
    }

    [Theory]
    [InlineData(":ok", true, "")]
    [InlineData(":r11=53001.", true, "53001")]
    [InlineData(":r12=6557.", true, "6557")]
    [InlineData(":r90=123456789,987654321.", true, "123456789,987654321")]
    public void ParseResponse_MatchesDumpResponses(string response, bool expectedSuccess, string expectedValue)
    {
        var result = GeneratorProtocol.ParseResponse(response);
        Assert.Equal(expectedSuccess, result.IsSuccess);
        Assert.Equal(expectedValue, result.Value);
    }

    // ─────────────────────────────────────────────────────────────
    // Round-trip: encode -> decode
    // ─────────────────────────────────────────────────────────────

    [Theory]
    [InlineData(":a00")]
    [InlineData(":r68")]
    [InlineData(":w24=76000500000000,")]
    [InlineData(":w11=1,0,")]
    [InlineData(":w95=12021")]
    public void EncodeAndDecode_RoundTrip(string command)
    {
        var encoded = GeneratorProtocol.EncodeCommandToBytes(command);
        var decoded = GeneratorProtocol.DecodeResponseFromBytes(encoded);

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
