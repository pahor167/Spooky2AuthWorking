using System.Collections.Concurrent;
using System.Globalization;
using Spooky2.Core.Constants;
using Spooky2.Core.Interfaces;
using Spooky2.Core.Models;

namespace Spooky2.Services.Database;

/// <summary>
/// Provides access to Spooky2 frequency databases stored as CSV/S2D files.
/// Databases contain frequency program entries parsed from comma-separated text files
/// in the Data directory.
/// </summary>
public sealed class DatabaseService : IDatabaseService
{
    private readonly IFileService _fileService;
    private readonly string _rootPath;
    private readonly ConcurrentDictionary<string, List<DatabaseEntry>> _cache = new(StringComparer.OrdinalIgnoreCase);

    private static readonly IReadOnlyDictionary<string, string> DatabaseFileMap = new Dictionary<string, string>(StringComparer.OrdinalIgnoreCase)
    {
        [DatabaseNames.RIFE] = "Frequencies",
        [DatabaseNames.CAFL] = "Frequencies",
        [DatabaseNames.XTRA] = "Frequencies",
        [DatabaseNames.HC] = "Frequencies",
        [DatabaseNames.ALT] = "Frequencies",
        [DatabaseNames.BIO] = "Frequencies",
        [DatabaseNames.PROV] = "Frequencies",
        [DatabaseNames.VEGA] = "Frequencies",
        [DatabaseNames.ETDFL] = "Frequencies",
        [DatabaseNames.KHZ] = "Frequencies",
        [DatabaseNames.SD] = "Frequencies",
        [DatabaseNames.RUSS] = "Frequencies",
        [DatabaseNames.RRM] = "Frequencies",
        [DatabaseNames.DNA] = "DNA_Frequencies",
        [DatabaseNames.MW] = "MW_Frequencies",
        [DatabaseNames.CUST] = "Custom",
        [DatabaseNames.BFB] = "BFB_Frequencies",
        [DatabaseNames.CUST1] = "Custom",
        [DatabaseNames.CUST2] = "Custom",
        [DatabaseNames.CUST3] = "Custom",
        [DatabaseNames.CUST4] = "Custom",
    };

    public DatabaseService(IFileService fileService)
        : this(fileService, Directory.GetCurrentDirectory())
    {
    }

    public DatabaseService(IFileService fileService, string rootPath)
    {
        _fileService = fileService ?? throw new ArgumentNullException(nameof(fileService));
        _rootPath = rootPath ?? throw new ArgumentNullException(nameof(rootPath));
    }

    public async Task<List<DatabaseEntry>> LoadDatabase(string databaseName)
    {
        ArgumentException.ThrowIfNullOrWhiteSpace(databaseName);

        if (_cache.TryGetValue(databaseName, out var cached))
        {
            return cached;
        }

        var filePath = ResolveDatabaseFilePath(databaseName);
        if (filePath is null)
        {
            return [];
        }

        var content = await _fileService.ReadAllText(filePath);
        var entries = ParseCsvContent(content, databaseName);

        _cache.TryAdd(databaseName, entries);
        return entries;
    }

    public async Task<List<DatabaseEntry>> SearchDatabase(string searchText, IEnumerable<string> databases)
    {
        ArgumentNullException.ThrowIfNull(databases);

        if (string.IsNullOrWhiteSpace(searchText))
        {
            return [];
        }

        var results = new List<DatabaseEntry>();

        foreach (var db in databases)
        {
            var entries = await LoadDatabase(db);
            var matched = entries
                .Where(e => e.Name.Contains(searchText, StringComparison.OrdinalIgnoreCase))
                .ToList();
            results.AddRange(matched);
        }

        return results;
    }

    public async Task<DatabaseEntry?> GetEntry(string name, string database)
    {
        ArgumentException.ThrowIfNullOrWhiteSpace(name);
        ArgumentException.ThrowIfNullOrWhiteSpace(database);

        var entries = await LoadDatabase(database);
        return entries.FirstOrDefault(e => string.Equals(e.Name, name, StringComparison.OrdinalIgnoreCase));
    }

    public Task RefreshDatabase()
    {
        _cache.Clear();
        return Task.CompletedTask;
    }

    private string? ResolveDatabaseFilePath(string databaseName)
    {
        var baseFileName = DatabaseFileMap.TryGetValue(databaseName, out var mapped)
            ? mapped
            : databaseName;

        var csvPath = Path.Combine(_rootPath, $"{baseFileName}.csv");
        if (_fileService.Exists(csvPath))
        {
            return csvPath;
        }

        var s2dPath = Path.Combine(_rootPath, $"{baseFileName}.s2d");
        if (_fileService.Exists(s2dPath))
        {
            return s2dPath;
        }

        return null;
    }

    private static List<DatabaseEntry> ParseCsvContent(string content, string sourceDatabase)
    {
        if (string.IsNullOrWhiteSpace(content))
        {
            return [];
        }

        var entries = new List<DatabaseEntry>();
        var lines = content.Split(['\r', '\n'], StringSplitOptions.RemoveEmptyEntries);

        foreach (var line in lines)
        {
            var trimmed = line.Trim();

            if (trimmed.Length == 0 || trimmed.StartsWith("//"))
            {
                continue;
            }

            var entry = ParseCsvLine(trimmed, sourceDatabase);
            if (entry is not null)
            {
                entries.Add(entry);
            }
        }

        return entries;
    }

    private static DatabaseEntry? ParseCsvLine(string line, string sourceDatabase)
    {
        var fields = line.Split(',');
        if (fields.Length < 1)
        {
            return null;
        }

        var name = fields[0].Trim();
        if (string.IsNullOrEmpty(name))
        {
            return null;
        }

        var frequencies = new List<double>();
        for (var i = 1; i < fields.Length; i++)
        {
            var field = fields[i].Trim();
            if (string.IsNullOrEmpty(field))
            {
                continue;
            }

            if (double.TryParse(field, NumberStyles.Float, CultureInfo.InvariantCulture, out var freq))
            {
                frequencies.Add(freq);
            }
        }

        return new DatabaseEntry
        {
            Name = name,
            Frequencies = frequencies,
            SourceDatabase = sourceDatabase,
        };
    }
}
