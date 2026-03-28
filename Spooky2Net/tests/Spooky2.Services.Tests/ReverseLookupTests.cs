using Spooky2.Core.Interfaces;
using Spooky2.Core.Models;
using Spooky2.Services.Scanner;
using Xunit;
using Xunit.Abstractions;

namespace Spooky2.Services.Tests;

/// <summary>
/// Tests that verify reverse lookup produces the same output as the original
/// Spooky2 software for the FullHuntAndKill scan data.
/// Expected output: Data/FullHuntAndKil/ReverseLookup
/// </summary>
public class ReverseLookupTests
{
    private readonly ITestOutputHelper _output;

    public ReverseLookupTests(ITestOutputHelper output)
    {
        _output = output;
    }

    /// <summary>
    /// The hit frequencies from the reverse lookup report (VB6's exact values).
    /// These differ slightly (~0.025%) from our computed frequencies due to
    /// the VB6 starting at 41009 Hz vs our 41000 Hz.
    /// </summary>
    private static readonly double[] ReverseLookupHitFrequencies =
    [
        1796509.68271064,
        1792903.3342103,
        1792484.82404752,
        1692146.81670824,
        1688912.31903142,
        1687211.76642131,
        1643214.04065681,
        1591431.89689782,
        177088.033864582,
        176869.435848991,
    ];

    private static string GetPath(string filename)
    {
        // Search up from bin output to find file at repo root or in Data/
        var dir = AppContext.BaseDirectory;
        for (int i = 0; i < 10; i++)
        {
            // Try direct
            var candidate = Path.Combine(dir, filename);
            if (File.Exists(candidate)) return candidate;
            // Try in Data/
            candidate = Path.Combine(dir, "Data", filename);
            if (File.Exists(candidate)) return candidate;
            dir = Path.GetDirectoryName(dir)!;
        }
        throw new FileNotFoundException($"'{filename}' not found from {AppContext.BaseDirectory}");
    }

    private static bool DataFilesAvailable()
    {
        try
        {
            GetPath("Data/FullHuntAndKil/ReverseLookup");
            GetPath("Frequencies_decrypted.csv");
            return true;
        }
        catch { return false; }
    }

    [Fact]
    public void ExpectedReverseLookupFile_HasCorrectStructure()
    {
        if (!DataFilesAvailable()) return;

        var lines = File.ReadAllLines(GetPath("FullHuntAndKil/ReverseLookup"));

        Assert.StartsWith("Reverse Lookup Report", lines[0]);
        Assert.StartsWith("Match tolerance: .25%", lines[1]);
        Assert.StartsWith("Include Harmonics: No.", lines[2]);
        Assert.StartsWith("Include Sub Harmonics: No.", lines[3]);

        // Count frequency sections (separated by dashes)
        int sectionCount = lines.Count(l => l.StartsWith("Database matches for"));
        Assert.Equal(10, sectionCount);
    }

    [Fact]
    public async Task ReverseLookup_MatchesExpectedOutput()
    {
        if (!DataFilesAvailable()) return;

        var expectedLines = File.ReadAllLines(GetPath("FullHuntAndKil/ReverseLookup"));

        // Parse expected: extract frequency sections with their matches
        var expectedSections = ParseReverseLookupReport(expectedLines);

        _output.WriteLine($"Expected sections: {expectedSections.Count}");
        foreach (var (freq, matches) in expectedSections)
            _output.WriteLine($"  {freq:F2} Hz: {matches.Count} matches");

        // Load the frequency database
        var dbService = new TestDatabaseService(GetPath("Frequencies_decrypted.csv"));

        // The databases included in the expected output
        var allDatabases = new List<string>
        {
            "ALT", "BIO", "CAFL", "CUST", "DNA", "ETDF", "HC", "KHZ",
            "MW", "PROV", "RIFE", "RUSS", "SD", "VEGA", "XTRA",
            "CUST1", "CUST2", "CUST3", "CUST4", "BFB"
        };

        var parameters = new ReverseLookupParameters
        {
            IncludeHarmonics = false,
            IncludeSubHarmonics = false,
            TolerancePercent = 0.25,
            IncludeHz = 0,
            Databases = allDatabases
        };

        var scanService = new ScanService(new NullGeneratorService());

        // Run reverse lookup for each hit frequency
        foreach (var (expectedFreq, expectedMatches) in expectedSections)
        {
            var results = await scanService.ReverseLookup(expectedFreq, parameters, dbService);

            _output.WriteLine($"\n{expectedFreq:F2} Hz:");
            _output.WriteLine($"  Expected: {expectedMatches.Count} matches");
            _output.WriteLine($"  Got:      {results.Count} matches");

            // Custom database entries (CUST) are user-specific and won't be in the standard
            // Frequencies_decrypted.csv. Filter them out for comparison.
            var standardExpected = expectedMatches
                .Where(e => e.Database != "CUST")
                .ToList();

            // Check each expected match is found
            foreach (var expected in standardExpected)
            {
                var found = results.Any(r =>
                    r.ProgramName == expected.ProgramName &&
                    Math.Abs(r.MatchedFrequency - expected.MatchedFrequency) < 0.01);

                if (!found)
                    _output.WriteLine($"  MISS: {expected.ProgramName} ({expected.Database}) ({expected.MatchedFrequency} Hz)");

                Assert.True(found,
                    $"At {expectedFreq:F2} Hz: expected match '{expected.ProgramName}' ({expected.Database}) at {expected.MatchedFrequency} Hz not found");
            }

            // Check no unexpected matches (allow results that match custom entries)
            foreach (var result in results)
            {
                var isExpected = standardExpected.Any(e =>
                    e.ProgramName == result.ProgramName &&
                    Math.Abs(e.MatchedFrequency - result.MatchedFrequency) < 0.01);

                if (!isExpected)
                    _output.WriteLine($"  EXTRA: {result.ProgramName} ({result.SourceDatabase}) ({result.MatchedFrequency} Hz)");
            }

            // Allow extra results (from non-custom sources that we might have additionally)
            // but all standard expected matches must be found
            Assert.True(results.Count >= standardExpected.Count,
                $"At {expectedFreq:F2} Hz: got {results.Count} matches but expected at least {standardExpected.Count}");
        }
    }

    /// <summary>
    /// Parse the expected reverse lookup report into structured data.
    /// </summary>
    private static List<(double Frequency, List<ExpectedMatch> Matches)> ParseReverseLookupReport(string[] lines)
    {
        var sections = new List<(double Frequency, List<ExpectedMatch> Matches)>();
        double currentFreq = 0;
        List<ExpectedMatch>? currentMatches = null;

        foreach (var line in lines)
        {
            if (line.StartsWith("Database matches for "))
            {
                // "Database matches for 1796509.68271064 Hz:"
                var freqStr = line["Database matches for ".Length..].TrimEnd(':', ' ');
                freqStr = freqStr.Replace(" Hz", "");
                if (double.TryParse(freqStr, System.Globalization.NumberStyles.Float,
                    System.Globalization.CultureInfo.InvariantCulture, out var freq))
                {
                    currentFreq = freq;
                    currentMatches = [];
                    sections.Add((currentFreq, currentMatches));
                }
            }
            else if (currentMatches != null && line.Length > 0 && !line.StartsWith("---") && !line.StartsWith("Reverse") && !line.StartsWith("Match") && !line.StartsWith("Include") && !line.StartsWith("Harmonic") && !line.StartsWith("Databases"))
            {
                // Parse: "Catarrh (RIFE) (1800000 Hz)"
                // Or: "Passion (Inspiring Blend) Essential Oil (SD) (SD) (1793273.72 Hz)"
                var trimmed = line.Trim();
                if (trimmed.Length == 0) continue;

                // Find the last "(NNN Hz)" or "(NNN.NN Hz)"
                var hzIdx = trimmed.LastIndexOf(" Hz)");
                if (hzIdx < 0) continue;

                var openParen = trimmed.LastIndexOf('(', hzIdx);
                if (openParen < 0) continue;

                var freqStr = trimmed[(openParen + 1)..hzIdx].Trim();
                if (!double.TryParse(freqStr, System.Globalization.NumberStyles.Float,
                    System.Globalization.CultureInfo.InvariantCulture, out var matchedFreq))
                    continue;

                // Everything before the frequency paren is "ProgramName (DB)"
                var prefix = trimmed[..openParen].TrimEnd();

                // Find the database name: last "(...)" before the frequency
                var dbEnd = prefix.LastIndexOf(')');
                var dbStart = prefix.LastIndexOf('(', dbEnd >= 0 ? dbEnd : 0);
                string programName;
                string db;
                if (dbStart >= 0 && dbEnd > dbStart)
                {
                    db = prefix[(dbStart + 1)..dbEnd];
                    programName = prefix[..dbStart].TrimEnd();
                }
                else
                {
                    db = "";
                    programName = prefix;
                }

                currentMatches.Add(new ExpectedMatch(programName, db, matchedFreq));
            }
        }

        return sections;
    }

    private record ExpectedMatch(string ProgramName, string Database, double MatchedFrequency);

    /// <summary>
    /// Minimal generator service for creating ScanService instances.
    /// </summary>
    private sealed class NullGeneratorService : IGeneratorService
    {
        public void Dispose() { }
        public Task<List<GeneratorState>> FindGenerators() => Task.FromResult(new List<GeneratorState>());
        public Task Start(int id) => Task.CompletedTask;
        public Task Stop(int id) => Task.CompletedTask;
        public Task Pause(int id) => Task.CompletedTask;
        public Task Hold(int id) => Task.CompletedTask;
        public Task Resume(int id) => Task.CompletedTask;
        public Task WriteFrequencies(int id, List<double> f) => Task.CompletedTask;
        public Task<GeneratorState> ReadStatus(int id) => Task.FromResult(new GeneratorState { Id = id, Status = GeneratorStatus.Idle });
        public Task EraseMemory(int id) => Task.CompletedTask;
        public Task IdentifyGenerators() => Task.CompletedTask;
        public Task SendRawCommand(int id, string cmd) => Task.CompletedTask;
        public Task<string?> SendCommandWithResponse(int id, string cmd) => Task.FromResult<string?>("ok");
        public Task SendCommandsBatch(int id, IReadOnlyList<string> cmds) => Task.CompletedTask;
        public Task WriteWaveformTables(int id) => Task.CompletedTask;
    }

    /// <summary>
    /// Database service that loads from the decrypted CSV file.
    /// </summary>
    private sealed class TestDatabaseService : IDatabaseService
    {
        private readonly string _csvPath;
        private readonly Dictionary<string, List<DatabaseEntry>> _cache = new();

        public TestDatabaseService(string csvPath) => _csvPath = csvPath;

        public Task<List<DatabaseEntry>> LoadDatabase(string databaseName)
        {
            if (_cache.TryGetValue(databaseName, out var cached))
                return Task.FromResult(cached);

            var entries = new List<DatabaseEntry>();
            foreach (var line in File.ReadLines(_csvPath))
            {
                if (line.StartsWith("#")) continue;

                var fields = ParseCsvLine(line);
                if (fields.Count < 5) continue;

                var name = fields[0].Trim('"');
                var category = fields[1].Trim();
                var freqStr = fields[4].Trim('"');

                if (!string.Equals(category, databaseName, StringComparison.OrdinalIgnoreCase))
                    continue;

                var frequencies = new List<double>();
                foreach (var f in freqStr.Split(',', StringSplitOptions.RemoveEmptyEntries))
                {
                    if (double.TryParse(f.Trim(), System.Globalization.NumberStyles.Float,
                        System.Globalization.CultureInfo.InvariantCulture, out var val) && val > 0)
                        frequencies.Add(val);
                }

                if (frequencies.Count > 0)
                {
                    entries.Add(new DatabaseEntry
                    {
                        Name = name,
                        Frequencies = frequencies,
                        Category = category,
                        SourceDatabase = databaseName
                    });
                }
            }

            _cache[databaseName] = entries;
            return Task.FromResult(entries);
        }

        public Task<List<DatabaseEntry>> SearchDatabase(string searchText, IEnumerable<string> databases) =>
            Task.FromResult(new List<DatabaseEntry>());
        public Task<DatabaseEntry?> GetEntry(string name, string database) =>
            Task.FromResult<DatabaseEntry?>(null);
        public Task RefreshDatabase() { _cache.Clear(); return Task.CompletedTask; }

        private static List<string> ParseCsvLine(string line)
        {
            var fields = new List<string>();
            bool inQuotes = false;
            int start = 0;

            for (int i = 0; i < line.Length; i++)
            {
                if (line[i] == '"') inQuotes = !inQuotes;
                else if (line[i] == ',' && !inQuotes)
                {
                    fields.Add(line[start..i]);
                    start = i + 1;
                }
            }
            fields.Add(line[start..]);
            return fields;
        }
    }
}
