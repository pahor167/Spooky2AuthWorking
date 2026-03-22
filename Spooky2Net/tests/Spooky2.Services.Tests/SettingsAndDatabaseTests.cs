using Xunit;
using Spooky2.Services.Settings;
using Spooky2.Services.Database;
using Spooky2.Services.IO;

namespace Spooky2.Services.Tests;

/// <summary>
/// Tests for settings persistence and database loading.
/// Concern: config file format must match what the VB6 app writes/reads.
/// </summary>
public class SettingsAndDatabaseTests
{
    private readonly string _tempDir = Path.Combine(Path.GetTempPath(), "spooky2_settings_tests");

    public SettingsAndDatabaseTests()
    {
        Directory.CreateDirectory(_tempDir);
    }

    // ─────────────────────────────────────────────────────────────
    // Settings: key=value file format
    // ─────────────────────────────────────────────────────────────

    [Fact]
    public async Task Settings_RoundTrip()
    {
        var service = new SettingsService(_tempDir);
        var path = Path.Combine(_tempDir, "test_settings.txt");

        var original = new Dictionary<string, string>
        {
            ["DefaultDwell"] = "180",
            ["ShowTooltips"] = "True",
            ["FrequencyMultiplier"] = "1.5",
            ["SkinIndex"] = "0"
        };

        await service.SaveSettings(path, original);
        var loaded = await service.LoadSettings(path);

        Assert.Equal(original.Count, loaded.Count);
        foreach (var kvp in original)
        {
            Assert.True(loaded.ContainsKey(kvp.Key), $"Missing key: {kvp.Key}");
            Assert.Equal(kvp.Value, loaded[kvp.Key]);
        }

        File.Delete(path);
    }

    [Fact]
    public async Task Settings_SkipsCommentsAndBlanks()
    {
        var path = Path.Combine(_tempDir, "comments_test.txt");
        await File.WriteAllTextAsync(path, """
            # This is a comment
            ; This is also a comment

            Key1=Value1
            Key2=Value2
            """);

        var service = new SettingsService(_tempDir);
        var settings = await service.LoadSettings(path);

        Assert.Equal(2, settings.Count);
        Assert.Equal("Value1", settings["Key1"]);
        Assert.Equal("Value2", settings["Key2"]);

        File.Delete(path);
    }

    [Fact]
    public async Task Settings_CaseInsensitiveKeys()
    {
        var path = Path.Combine(_tempDir, "case_test.txt");
        await File.WriteAllTextAsync(path, "DefaultDwell=180\n");

        var service = new SettingsService(_tempDir);
        var settings = await service.LoadSettings(path);

        Assert.True(settings.ContainsKey("defaultdwell"));
        Assert.True(settings.ContainsKey("DEFAULTDWELL"));
        Assert.Equal("180", settings["DefaultDwell"]);

        File.Delete(path);
    }

    [Fact]
    public async Task Settings_MissingFile_ReturnsEmpty()
    {
        var service = new SettingsService(_tempDir);
        var settings = await service.LoadSettings("/nonexistent/path/file.txt");

        Assert.Empty(settings);
    }

    [Fact]
    public async Task Settings_ValueWithEquals_PreservesFullValue()
    {
        var path = Path.Combine(_tempDir, "equals_test.txt");
        await File.WriteAllTextAsync(path, "ConnectionString=server=localhost;port=5432\n");

        var service = new SettingsService(_tempDir);
        var settings = await service.LoadSettings(path);

        Assert.Equal("server=localhost;port=5432", settings["ConnectionString"]);

        File.Delete(path);
    }

    [Fact]
    public async Task RestoreDefaults_CreatesConfigFile()
    {
        var service = new SettingsService(_tempDir);
        await service.RestoreDefaults();

        var path = Path.Combine(_tempDir, "Data", "SystemCFG.txt");
        Assert.True(File.Exists(path));

        var settings = await service.LoadSettings(path);
        Assert.True(settings.Count > 10, "Defaults should have many settings");
        Assert.Equal("180", settings["DefaultDwell"]);
        Assert.Equal("True", settings["ShowTooltips"]);

        // Cleanup
        Directory.Delete(Path.Combine(_tempDir, "Data"), true);
    }

    [Fact]
    public async Task RestoreBfbDefaults_CreatesConfigFile()
    {
        var service = new SettingsService(_tempDir);
        await service.RestoreBfbDefaults();

        var path = Path.Combine(_tempDir, "Data", "BFBCFG.txt");
        Assert.True(File.Exists(path));

        var settings = await service.LoadSettings(path);
        Assert.True(settings.ContainsKey("BFB_StartFreq"));
        Assert.Equal("76000", settings["BFB_StartFreq"]);

        // Cleanup
        Directory.Delete(Path.Combine(_tempDir, "Data"), true);
    }

    // ─────────────────────────────────────────────────────────────
    // Database: CSV loading
    // ─────────────────────────────────────────────────────────────

    [Fact]
    public async Task Database_LoadsCsvFile()
    {
        var dbDir = Path.Combine(_tempDir, "db_test");
        Directory.CreateDirectory(dbDir);
        var csvPath = Path.Combine(dbDir, "Frequencies.csv");

        await File.WriteAllTextAsync(csvPath, """
            Healing Program,100,200,300,400,500
            Pain Relief,76000,152000
            Sleep Aid,3.5,7.83,14.1
            """);

        var fileService = new FileService();
        var dbService = new DatabaseService(fileService, dbDir);
        var entries = await dbService.LoadDatabase("RIFE");

        Assert.Equal(3, entries.Count);
        Assert.Equal("Healing Program", entries[0].Name);
        Assert.Equal(5, entries[0].Frequencies.Count);
        Assert.Equal(100.0, entries[0].Frequencies[0]);
        Assert.Equal(500.0, entries[0].Frequencies[4]);
        Assert.Equal("Pain Relief", entries[1].Name);
        Assert.Equal(2, entries[1].Frequencies.Count);

        Directory.Delete(dbDir, true);
    }

    [Fact]
    public async Task Database_SkipsEmptyLinesAndComments()
    {
        var dbDir = Path.Combine(_tempDir, "db_comments_test");
        Directory.CreateDirectory(dbDir);
        var csvPath = Path.Combine(dbDir, "Frequencies.csv");

        await File.WriteAllTextAsync(csvPath, """
            // This is a comment

            Program1,100,200

            // Another comment
            Program2,300,400
            """);

        var fileService = new FileService();
        var dbService = new DatabaseService(fileService, dbDir);
        var entries = await dbService.LoadDatabase("RIFE");

        Assert.Equal(2, entries.Count);

        Directory.Delete(dbDir, true);
    }

    [Fact]
    public async Task Database_SearchFiltersCorrectly()
    {
        var dbDir = Path.Combine(_tempDir, "db_search_test");
        Directory.CreateDirectory(dbDir);
        var csvPath = Path.Combine(dbDir, "Frequencies.csv");

        await File.WriteAllTextAsync(csvPath, """
            Healing Cancer,100,200
            Pain Relief,300,400
            Healing Sleep,500,600
            Detox Program,700,800
            """);

        var fileService = new FileService();
        var dbService = new DatabaseService(fileService, dbDir);
        var results = await dbService.SearchDatabase("Healing", new[] { "RIFE" });

        Assert.Equal(2, results.Count);
        Assert.All(results, r => Assert.Contains("Healing", r.Name));

        Directory.Delete(dbDir, true);
    }

    [Fact]
    public async Task Database_SearchIsCaseInsensitive()
    {
        var dbDir = Path.Combine(_tempDir, "db_case_test");
        Directory.CreateDirectory(dbDir);
        var csvPath = Path.Combine(dbDir, "Frequencies.csv");

        await File.WriteAllTextAsync(csvPath, "healing program,100\nHEALING PROGRAM2,200\n");

        var fileService = new FileService();
        var dbService = new DatabaseService(fileService, dbDir);
        var results = await dbService.SearchDatabase("healing", new[] { "RIFE" });

        Assert.Equal(2, results.Count);

        Directory.Delete(dbDir, true);
    }

    [Fact]
    public async Task Database_CachesResults()
    {
        var dbDir = Path.Combine(_tempDir, "db_cache_test");
        Directory.CreateDirectory(dbDir);
        var csvPath = Path.Combine(dbDir, "Frequencies.csv");

        await File.WriteAllTextAsync(csvPath, "Program1,100\n");

        var fileService = new FileService();
        var dbService = new DatabaseService(fileService, dbDir);

        var result1 = await dbService.LoadDatabase("RIFE");
        Assert.Single(result1);

        // Modify file - should still get cached result
        await File.WriteAllTextAsync(csvPath, "Program1,100\nProgram2,200\n");
        var result2 = await dbService.LoadDatabase("RIFE");
        Assert.Single(result2); // Still cached

        // Refresh clears cache
        await dbService.RefreshDatabase();
        var result3 = await dbService.LoadDatabase("RIFE");
        Assert.Equal(2, result3.Count); // Fresh read

        Directory.Delete(dbDir, true);
    }

    [Fact]
    public async Task Database_MissingFile_ReturnsEmpty()
    {
        var dbDir = Path.Combine(_tempDir, "db_missing_test");
        Directory.CreateDirectory(dbDir);

        var fileService = new FileService();
        var dbService = new DatabaseService(fileService, dbDir);
        var entries = await dbService.LoadDatabase("RIFE");

        Assert.Empty(entries);

        Directory.Delete(dbDir, true);
    }
}
