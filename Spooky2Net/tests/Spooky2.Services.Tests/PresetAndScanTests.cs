using Xunit;
using Spooky2.Core.Interfaces;
using Spooky2.Core.Models;
using Spooky2.Services.IO;
using Spooky2.Services.Presets;
using Spooky2.Services.Scanner;
using Spooky2.Services.Communication;
using Microsoft.Extensions.Logging.Abstractions;

namespace Spooky2.Services.Tests;

public class PresetAndScanTests
{
    private readonly string _tempDir = Path.Combine(Path.GetTempPath(), $"spooky2_preset_tests_{Guid.NewGuid():N}");

    public PresetAndScanTests()
    {
        Directory.CreateDirectory(_tempDir);
    }

    // ─────────────────────────────────────────────────────────────
    // Preset I/O
    // ─────────────────────────────────────────────────────────────

    [Fact]
    public async Task Preset_SaveAndLoad_RoundTrip()
    {
        var fileService = new FileService();
        var svc = new PresetService(fileService);

        var original = new Preset
        {
            Name = "Test Preset",
            Settings = new Dictionary<string, string>
            {
                ["DwellMultiplier"] = "2.0",
                ["RepeatSequence"] = "3"
            },
            Programs = [
                new FrequencyProgram
                {
                    Name = "Healing",
                    Frequencies = [100.0, 200.0, 300.0],
                    DwellSeconds = 180
                }
            ]
        };

        var path = Path.Combine(_tempDir, "test_preset.txt");
        await svc.SavePreset(original, path);
        Assert.True(File.Exists(path));

        var loaded = await svc.LoadPreset(path);
        // Name is derived from filename, not stored content
        Assert.Equal("test_preset", loaded.Name);

        File.Delete(path);
    }

    [Fact]
    public async Task Preset_Delete_RemovesFile()
    {
        var fileService = new FileService();
        var svc = new PresetService(fileService);

        var path = Path.Combine(_tempDir, "delete_me.txt");
        await File.WriteAllTextAsync(path, "test");
        Assert.True(File.Exists(path));

        await svc.DeletePreset(path);
        Assert.False(File.Exists(path));
    }

    [Fact]
    public async Task Preset_Search_FindsMatching()
    {
        var fileService = new FileService();
        var svc = new PresetService(fileService);

        await File.WriteAllTextAsync(Path.Combine(_tempDir, "healing_bones.txt"), "test");
        await File.WriteAllTextAsync(Path.Combine(_tempDir, "healing_sleep.txt"), "test");
        await File.WriteAllTextAsync(Path.Combine(_tempDir, "detox_liver.txt"), "test");

        var results = await svc.SearchPresets("healing", _tempDir);
        Assert.Equal(2, results.Count);
        Assert.All(results, r => Assert.Contains("healing", r, StringComparison.OrdinalIgnoreCase));
    }

    [Fact]
    public async Task PresetChain_SaveAndLoad_RoundTrip()
    {
        var fileService = new FileService();
        var svc = new PresetService(fileService);

        var chain = new PresetChain
        {
            Name = "My Chain",
            Presets = [
                new Preset { Name = "Step 1", FilePath = "/presets/step1.txt" },
                new Preset { Name = "Step 2", FilePath = "/presets/step2.txt" }
            ]
        };

        var path = Path.Combine(_tempDir, "chain.txt");
        await svc.SavePresetChain(chain, path);

        var loaded = await svc.LoadPresetChain(path);
        Assert.Equal("My Chain", loaded.Name);

        File.Delete(path);
    }

    // ─────────────────────────────────────────────────────────────
    // Reverse Lookup (harmonics math)
    // ─────────────────────────────────────────────────────────────

    [Fact]
    public async Task ReverseLookup_Harmonics_GeneratesMultiples()
    {
        var genService = new GeneratorService(new MockSerialPortFactory(), NullLogger<GeneratorService>.Instance);
        var svc = new ScanService(genService);

        await svc.ReverseLookup(100.0, harmonics: true, subHarmonics: false, tolerance: 0);

        var results = await svc.GetScanResults(0);
        Assert.Equal(19, results.Count); // 2x through 20x
        Assert.Contains(results, r => Math.Abs(r.Frequency - 200.0) < 0.001);
        Assert.Contains(results, r => Math.Abs(r.Frequency - 500.0) < 0.001);
        Assert.Contains(results, r => Math.Abs(r.Frequency - 2000.0) < 0.001);
    }

    [Fact]
    public async Task ReverseLookup_SubHarmonics_GeneratesDivisors()
    {
        var genService = new GeneratorService(new MockSerialPortFactory(), NullLogger<GeneratorService>.Instance);
        var svc = new ScanService(genService);

        await svc.ReverseLookup(1000.0, harmonics: false, subHarmonics: true, tolerance: 0);

        var results = await svc.GetScanResults(0);
        Assert.Equal(19, results.Count); // /2 through /20
        Assert.Contains(results, r => Math.Abs(r.Frequency - 500.0) < 0.001);
        Assert.Contains(results, r => Math.Abs(r.Frequency - 100.0) < 0.001);
        Assert.Contains(results, r => Math.Abs(r.Frequency - 50.0) < 0.001);
    }

    [Fact]
    public async Task ReverseLookup_Both_Generates38Results()
    {
        var genService = new GeneratorService(new MockSerialPortFactory(), NullLogger<GeneratorService>.Instance);
        var svc = new ScanService(genService);

        await svc.ReverseLookup(1000.0, harmonics: true, subHarmonics: true, tolerance: 0);

        var results = await svc.GetScanResults(0);
        Assert.Equal(38, results.Count); // 19 harmonics + 19 sub-harmonics
    }

    [Fact]
    public async Task ReverseLookup_Neither_ReturnsEmpty()
    {
        var genService = new GeneratorService(new MockSerialPortFactory(), NullLogger<GeneratorService>.Instance);
        var svc = new ScanService(genService);

        await svc.ReverseLookup(1000.0, harmonics: false, subHarmonics: false, tolerance: 0);

        var results = await svc.GetScanResults(0);
        Assert.Empty(results);
    }

    // ─────────────────────────────────────────────────────────────
    // Scan state management
    // ─────────────────────────────────────────────────────────────

    [Fact]
    public async Task Scan_StopScan_DoesNotThrow()
    {
        var genService = new GeneratorService(new MockSerialPortFactory(), NullLogger<GeneratorService>.Instance);
        var svc = new ScanService(genService);

        // StopScan on a generator that isn't scanning should not throw
        await svc.StopScan(1);

        var results = await svc.GetScanResults(1);
        Assert.Empty(results);
    }

    // ─────────────────────────────────────────────────────────────
    // Error Logging
    // ─────────────────────────────────────────────────────────────

    [Fact]
    public async Task ErrorLog_WritesFile()
    {
        var logDir = Path.Combine(_tempDir, "log_test");
        Directory.CreateDirectory(logDir);

        var svc = new Spooky2.Services.IO.ErrorLoggingService(logDir);
        await svc.WriteError("TestProc", "TestSource", "TestDescription");

        var logPath = Path.Combine(logDir, "Data", "Error.log");
        Assert.True(File.Exists(logPath));

        var content = await File.ReadAllTextAsync(logPath);
        Assert.Contains("TestProc", content);
        Assert.Contains("TestSource", content);
        Assert.Contains("TestDescription", content);

        Directory.Delete(logDir, true);
    }

    [Fact]
    public async Task ErrorLog_TruncatesLargeFile()
    {
        var logDir = Path.Combine(_tempDir, "truncate_test");
        var dataDir = Path.Combine(logDir, "Data");
        Directory.CreateDirectory(dataDir);

        var logPath = Path.Combine(dataDir, "Error.log");
        // Create a file just over 10MB
        await File.WriteAllBytesAsync(logPath, new byte[10_485_761]);
        Assert.True(new FileInfo(logPath).Length > 10_485_760);

        var svc = new Spooky2.Services.IO.ErrorLoggingService(logDir);
        await svc.TruncateIfNeeded();

        // File should be deleted or recreated smaller
        if (File.Exists(logPath))
        {
            Assert.True(new FileInfo(logPath).Length < 10_485_760);
        }

        Directory.Delete(logDir, true);
    }

    // ─────────────────────────────────────────────────────────────
    // Mock HID service for generator-dependent tests
    // ─────────────────────────────────────────────────────────────

    private sealed class MockSerialPortConnection : ISerialPortConnection
    {
        public bool IsOpen { get; private set; } = true;
        public int BytesAvailable => 0;
        public void Write(string data) { }
        public string ReadLine() => "ok";
        public string ReadExisting() => "ok";
        public void Dispose() => IsOpen = false;
    }

    private sealed class MockSerialPortFactory : ISerialPortFactory
    {
        public ISerialPortConnection Open(string portName, int baudRate, int readTimeoutMs = 2000, int writeTimeoutMs = 2000)
            => new MockSerialPortConnection();
        public List<string> GetAvailablePorts() => new();
    }
}
