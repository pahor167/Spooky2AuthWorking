using System.Buffers.Binary;
using Spooky2.Core.Interfaces;
using Spooky2.Core.Models;

namespace Spooky2.Services.Waveform;

/// <summary>
/// Waveform generation service.
/// Generates standard 16-bit PCM WAV files with 44-byte RIFF headers.
/// </summary>
public sealed class WaveformService : IWaveformService
{
    private const int SampleRate = 48000;
    private const short BitsPerSample = 16;
    private const short Channels = 1;
    private const int DurationSamples = SampleRate; // 1 second
    private const short AudioFormat = 1; // PCM

    private static readonly List<string> WaveformTypes =
    [
        "Sine",
        "Square",
        "Sawtooth",
        "Inverse Sawtooth",
        "Triangle",
        "Damped",
        "Damped Square",
        "H-Bomb"
    ];

    public async Task GenerateWav(WaveformSettings settings, string outputPath)
    {
        ArgumentNullException.ThrowIfNull(settings);
        ArgumentException.ThrowIfNullOrWhiteSpace(outputPath);

        var samples = GenerateSamples(settings);
        var wavBytes = BuildWavFile(samples);

        var directory = Path.GetDirectoryName(outputPath);
        if (!string.IsNullOrEmpty(directory))
        {
            Directory.CreateDirectory(directory);
        }

        await File.WriteAllBytesAsync(outputPath, wavBytes);
    }

    public Task RefreshWaveforms()
    {
        // No-op until waveforms directory is configured
        return Task.CompletedTask;
    }

    public List<string> GetWaveformTypes()
    {
        // Return a new list each call to preserve immutability of the internal list
        return new List<string>(WaveformTypes);
    }

    private static short[] GenerateSamples(WaveformSettings settings)
    {
        var samples = new short[DurationSamples];
        var phaseRadians = settings.Phase * Math.PI / 180.0;
        var amplitude = Math.Clamp(settings.Amplitude, 0.0, 1.0);

        for (var i = 0; i < DurationSamples; i++)
        {
            var t = (double)i / DurationSamples; // normalized position [0, 1)
            var value = ComputeWaveformValue(settings.WaveformType, t, phaseRadians);
            var scaled = value * amplitude * short.MaxValue;
            samples[i] = (short)Math.Clamp(scaled, short.MinValue, short.MaxValue);
        }

        return samples;
    }

    private static double ComputeWaveformValue(WaveformType waveformType, double t, double phaseRadians)
    {
        return waveformType switch
        {
            WaveformType.Sine => Math.Sin(2.0 * Math.PI * t + phaseRadians),
            WaveformType.Square => t < 0.5 ? 1.0 : -1.0,
            WaveformType.Sawtooth => 2.0 * t - 1.0,
            WaveformType.InverseSawtooth => 1.0 - 2.0 * t,
            WaveformType.Triangle => t < 0.5 ? 4.0 * t - 1.0 : 3.0 - 4.0 * t,
            WaveformType.Damped => Math.Sin(2.0 * Math.PI * t) * Math.Exp(-3.0 * t),
            WaveformType.DampedSquare => (t < 0.5 ? 1.0 : -1.0) * Math.Exp(-3.0 * t),
            WaveformType.HBomb => Math.Sin(2.0 * Math.PI * t) * Math.Sin(Math.PI * t),
            _ => throw new ArgumentOutOfRangeException(nameof(waveformType), waveformType, "Unknown waveform type")
        };
    }

    private static byte[] BuildWavFile(short[] samples)
    {
        const short blockAlign = Channels * BitsPerSample / 8;
        const int byteRate = SampleRate * blockAlign;
        var dataSize = samples.Length * blockAlign;
        var fileSize = 44 + dataSize; // 44-byte header + data

        var buffer = new byte[fileSize];
        var span = buffer.AsSpan();

        // RIFF header
        "RIFF"u8.CopyTo(span);
        BinaryPrimitives.WriteInt32LittleEndian(span[4..], fileSize - 8);
        "WAVE"u8.CopyTo(span[8..]);

        // fmt chunk
        "fmt "u8.CopyTo(span[12..]);
        BinaryPrimitives.WriteInt32LittleEndian(span[16..], 16); // chunk size
        BinaryPrimitives.WriteInt16LittleEndian(span[20..], AudioFormat);
        BinaryPrimitives.WriteInt16LittleEndian(span[22..], Channels);
        BinaryPrimitives.WriteInt32LittleEndian(span[24..], SampleRate);
        BinaryPrimitives.WriteInt32LittleEndian(span[28..], byteRate);
        BinaryPrimitives.WriteInt16LittleEndian(span[32..], blockAlign);
        BinaryPrimitives.WriteInt16LittleEndian(span[34..], BitsPerSample);

        // data chunk
        "data"u8.CopyTo(span[36..]);
        BinaryPrimitives.WriteInt32LittleEndian(span[40..], dataSize);

        // Write sample data
        var dataSpan = span[44..];
        for (var i = 0; i < samples.Length; i++)
        {
            BinaryPrimitives.WriteInt16LittleEndian(dataSpan[(i * 2)..], samples[i]);
        }

        return buffer;
    }
}
