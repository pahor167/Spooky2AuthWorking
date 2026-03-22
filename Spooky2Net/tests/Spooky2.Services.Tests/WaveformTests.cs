using Xunit;
using Spooky2.Core.Models;
using Spooky2.Services.Waveform;

namespace Spooky2.Services.Tests;

/// <summary>
/// Tests for WAV file generation.
/// Verifies that generated WAV files have correct headers and valid sample data.
/// </summary>
public class WaveformTests
{
    private readonly WaveformService _service = new();
    private readonly string _tempDir = Path.Combine(Path.GetTempPath(), "spooky2_wav_tests");

    public WaveformTests()
    {
        Directory.CreateDirectory(_tempDir);
    }

    [Theory]
    [InlineData(WaveformType.Sine)]
    [InlineData(WaveformType.Square)]
    [InlineData(WaveformType.Sawtooth)]
    [InlineData(WaveformType.InverseSawtooth)]
    [InlineData(WaveformType.Triangle)]
    [InlineData(WaveformType.Damped)]
    [InlineData(WaveformType.DampedSquare)]
    [InlineData(WaveformType.HBomb)]
    public async Task GenerateWav_AllWaveformTypes_CreateValidFile(WaveformType waveformType)
    {
        var settings = new WaveformSettings
        {
            WaveformType = waveformType,
            Amplitude = 0.8,
            Phase = 0,
            Frequency = 440
        };

        var path = Path.Combine(_tempDir, $"{waveformType}.wav");
        await _service.GenerateWav(settings, path);

        Assert.True(File.Exists(path));

        var bytes = await File.ReadAllBytesAsync(path);

        // Minimum WAV file size: 44 byte header + at least some samples
        Assert.True(bytes.Length > 44, $"WAV file too small: {bytes.Length} bytes");

        // RIFF header check
        Assert.Equal((byte)'R', bytes[0]);
        Assert.Equal((byte)'I', bytes[1]);
        Assert.Equal((byte)'F', bytes[2]);
        Assert.Equal((byte)'F', bytes[3]);

        // WAVE format
        Assert.Equal((byte)'W', bytes[8]);
        Assert.Equal((byte)'A', bytes[9]);
        Assert.Equal((byte)'V', bytes[10]);
        Assert.Equal((byte)'E', bytes[11]);

        // fmt chunk
        Assert.Equal((byte)'f', bytes[12]);
        Assert.Equal((byte)'m', bytes[13]);
        Assert.Equal((byte)'t', bytes[14]);
        Assert.Equal((byte)' ', bytes[15]);

        // Audio format = 1 (PCM)
        Assert.Equal(1, BitConverter.ToInt16(bytes, 20));

        // Channels = 1 (mono)
        Assert.Equal(1, BitConverter.ToInt16(bytes, 22));

        // data chunk
        Assert.Equal((byte)'d', bytes[36]);
        Assert.Equal((byte)'a', bytes[37]);
        Assert.Equal((byte)'t', bytes[38]);
        Assert.Equal((byte)'a', bytes[39]);

        // Clean up
        File.Delete(path);
    }

    [Fact]
    public async Task GenerateWav_ZeroAmplitude_ProducesSilence()
    {
        var settings = new WaveformSettings
        {
            WaveformType = WaveformType.Sine,
            Amplitude = 0,
            Phase = 0,
            Frequency = 440
        };

        var path = Path.Combine(_tempDir, "silence.wav");
        await _service.GenerateWav(settings, path);

        var bytes = await File.ReadAllBytesAsync(path);

        // Sample data starts at offset 44, each sample is 2 bytes (16-bit)
        // All samples should be 0 (silence)
        for (int i = 44; i < bytes.Length - 1; i += 2)
        {
            var sample = BitConverter.ToInt16(bytes, i);
            Assert.Equal(0, sample);
        }

        File.Delete(path);
    }

    [Fact]
    public async Task GenerateWav_PhaseOffset_ChangesSamples()
    {
        var settings0 = new WaveformSettings
        {
            WaveformType = WaveformType.Sine,
            Amplitude = 1.0,
            Phase = 0,
            Frequency = 440
        };
        var settings90 = new WaveformSettings
        {
            WaveformType = WaveformType.Sine,
            Amplitude = 1.0,
            Phase = 90,
            Frequency = 440
        };

        var path0 = Path.Combine(_tempDir, "phase0.wav");
        var path90 = Path.Combine(_tempDir, "phase90.wav");
        await _service.GenerateWav(settings0, path0);
        await _service.GenerateWav(settings90, path90);

        var bytes0 = await File.ReadAllBytesAsync(path0);
        var bytes90 = await File.ReadAllBytesAsync(path90);

        // Same length
        Assert.Equal(bytes0.Length, bytes90.Length);

        // But different sample data (phase shifted)
        bool anyDifferent = false;
        for (int i = 44; i < bytes0.Length; i++)
        {
            if (bytes0[i] != bytes90[i]) { anyDifferent = true; break; }
        }
        Assert.True(anyDifferent, "Phase offset should produce different samples");

        File.Delete(path0);
        File.Delete(path90);
    }

    [Fact]
    public void GetWaveformTypes_Returns8Types()
    {
        var types = _service.GetWaveformTypes();

        Assert.Equal(8, types.Count);
        Assert.Contains("Sine", types);
        Assert.Contains("Square", types);
        Assert.Contains("Sawtooth", types);
        Assert.Contains("Inverse Sawtooth", types);
        Assert.Contains("Triangle", types);
        Assert.Contains("Damped", types);
        Assert.Contains("Damped Square", types);
        Assert.Contains("H-Bomb", types);
    }

    [Fact]
    public void GetWaveformTypes_ReturnsDefensiveCopy()
    {
        var types1 = _service.GetWaveformTypes();
        var types2 = _service.GetWaveformTypes();

        Assert.NotSame(types1, types2);
    }
}
