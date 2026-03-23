using System.Collections.Concurrent;
using System.Globalization;
using System.Text;
using Microsoft.Extensions.Logging;
using Microsoft.Extensions.Logging.Abstractions;
using Spooky2.Core.Constants;
using Spooky2.Core.Interfaces;
using Spooky2.Core.Models;
using Spooky2.Services.Encryption;

namespace Spooky2.Services.Database;

/// <summary>
/// Provides access to Spooky2 frequency databases stored as CSV or encrypted S2D files.
/// S2D files are decrypted using the RndCryptLevel2 algorithm with the self-encrypted password.
/// </summary>
public sealed class DatabaseService : IDatabaseService
{
    private const string RawPassword = "2020888376Spooky2 (c) John White. http://www.cancerclinic.co.nz";

    private readonly IFileService _fileService;
    private readonly IEncryptionService _encryptionService;
    private readonly ILogger<DatabaseService> _logger;
    private readonly string _rootPath;
    private readonly ConcurrentDictionary<string, List<DatabaseEntry>> _cache = new(StringComparer.OrdinalIgnoreCase);

    /// <summary>
    /// Pre-computed encrypted password (Base64 of self-encrypted raw password).
    /// VB6 binary computes this once in Form_Load via:
    ///   RndCryptLevel2(rawPassword, rawPassword, 1, 1, 1, 3, True)
    /// and stores the result at form member offset 0x1B4.
    /// </summary>
    private readonly byte[] _encryptedPasswordBytes;

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

    public DatabaseService(IFileService fileService, IEncryptionService encryptionService, ILogger<DatabaseService> logger)
        : this(fileService, encryptionService, Directory.GetCurrentDirectory(), logger)
    {
    }

    public DatabaseService(IFileService fileService, IEncryptionService encryptionService)
        : this(fileService, encryptionService, Directory.GetCurrentDirectory(), null)
    {
    }

    public DatabaseService(IFileService fileService)
        : this(fileService, new EncryptionService(), Directory.GetCurrentDirectory(), null)
    {
    }

    public DatabaseService(IFileService fileService, string rootPath)
        : this(fileService, new EncryptionService(), rootPath, null)
    {
    }

    public DatabaseService(IFileService fileService, IEncryptionService encryptionService, string rootPath)
        : this(fileService, encryptionService, rootPath, null)
    {
    }

    public DatabaseService(IFileService fileService, IEncryptionService encryptionService, string rootPath, ILogger<DatabaseService>? logger)
    {
        _fileService = fileService ?? throw new ArgumentNullException(nameof(fileService));
        _encryptionService = encryptionService ?? throw new ArgumentNullException(nameof(encryptionService));
        _rootPath = rootPath ?? throw new ArgumentNullException(nameof(rootPath));
        _logger = logger ?? NullLogger<DatabaseService>.Instance;
        _encryptedPasswordBytes = EncryptionService.ComputeEncryptedPassword(RawPassword);
        _logger.LogDebug("DatabaseService initialized with root path '{RootPath}'", _rootPath);
    }

    public async Task<List<DatabaseEntry>> LoadDatabase(string databaseName)
    {
        ArgumentException.ThrowIfNullOrWhiteSpace(databaseName);
        _logger.LogDebug("LoadDatabase requested for '{DatabaseName}'", databaseName);

        if (_cache.TryGetValue(databaseName, out var cached))
        {
            _logger.LogDebug("Database '{DatabaseName}' found in cache with {Count} entries", databaseName, cached.Count);
            return cached;
        }

        var filePath = ResolveDatabaseFilePath(databaseName);
        if (filePath is null)
        {
            _logger.LogWarning("Database file not found for '{DatabaseName}'", databaseName);
            return [];
        }

        _logger.LogInformation("Loading database '{DatabaseName}' from '{FilePath}'", databaseName, filePath);

        string content;
        bool isS2d = filePath.EndsWith(".s2d", StringComparison.OrdinalIgnoreCase);

        if (isS2d)
        {
            content = await DecryptS2dFile(filePath);
        }
        else
        {
            content = await _fileService.ReadAllText(filePath);
        }

        var entries = isS2d
            ? ParseS2dContent(content, databaseName)
            : ParseSimpleCsvContent(content, databaseName);
        _cache.TryAdd(databaseName, entries);
        _logger.LogInformation("Database '{DatabaseName}' loaded with {Count} entries (format={Format})", databaseName, entries.Count, isS2d ? "S2D" : "CSV");
        return entries;
    }

    public async Task<List<DatabaseEntry>> SearchDatabase(string searchText, IEnumerable<string> databases)
    {
        ArgumentNullException.ThrowIfNull(databases);
        var dbList = databases.ToList();
        _logger.LogDebug("SearchDatabase called for '{SearchText}' across {Count} database(s): {Databases}", searchText, dbList.Count, string.Join(", ", dbList));

        if (string.IsNullOrWhiteSpace(searchText))
            return [];

        var results = new List<DatabaseEntry>();
        foreach (var db in dbList)
        {
            var entries = await LoadDatabase(db);
            results.AddRange(entries.Where(e => e.Name.Contains(searchText, StringComparison.OrdinalIgnoreCase)));
        }
        _logger.LogInformation("Search for '{SearchText}' returned {Count} results", searchText, results.Count);
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
        _logger.LogInformation("Refreshing database cache, clearing {Count} cached database(s)", _cache.Count);
        _cache.Clear();
        return Task.CompletedTask;
    }

    private string? ResolveDatabaseFilePath(string databaseName)
    {
        var baseFileName = DatabaseFileMap.TryGetValue(databaseName, out var mapped) ? mapped : databaseName;

        // Prefer .csv (plaintext), fall back to .s2d (encrypted)
        var csvPath = Path.Combine(_rootPath, $"{baseFileName}.csv");
        if (_fileService.Exists(csvPath))
            return csvPath;

        var s2dPath = Path.Combine(_rootPath, $"{baseFileName}.s2d");
        if (_fileService.Exists(s2dPath))
            return s2dPath;

        return null;
    }

    /// <summary>
    /// Decrypts an .s2d file by decrypting each Base64 line with RndCryptLevel2
    /// using the pre-encrypted password. Returns the combined plaintext content.
    /// </summary>
    private async Task<string> DecryptS2dFile(string filePath)
    {
        _logger.LogDebug("Decrypting S2D file '{FilePath}'", filePath);
        var rawContent = await _fileService.ReadAllText(filePath);
        var lines = rawContent.Split(["\r\n", "\n"], StringSplitOptions.None);
        var encPassword = Encoding.Latin1.GetString(_encryptedPasswordBytes);

        var sb = new StringBuilder();
        int lineCount = 0;
        int successCount = 0;
        int failCount = 0;
        foreach (var line in lines)
        {
            var trimmed = line.Trim();
            if (trimmed.Length == 0)
                continue;

            lineCount++;
            try
            {
                var decrypted = _encryptionService.MultiPassXorEncrypt(
                    trimmed, encPassword,
                    seedPassCount: 1, dataPassCount: 1,
                    inputFormat: 3,  // Base64 input
                    outputFormat: 1, // StrConv(vbUnicode) output
                    sanitizeInput: true);

                sb.AppendLine(decrypted);
                successCount++;
            }
            catch (Exception ex)
            {
                failCount++;
                _logger.LogError(ex, "Failed to decrypt S2D line {LineNumber} in '{FilePath}'", lineCount, filePath);
            }
        }
        _logger.LogInformation("S2D decryption complete for '{FilePath}': {Total} lines, {Success} succeeded, {Failed} failed", filePath, lineCount, successCount, failCount);
        return sb.ToString();
    }

    /// <summary>
    /// Parses decrypted S2D content. Each line is CSV with quoted fields:
    /// "Name",Category,FreqCount,"Description","freq1,freq2,...",,,Dwell
    /// Header lines start with '#'.
    /// </summary>
    private static List<DatabaseEntry> ParseS2dContent(string content, string sourceDatabase)
    {
        if (string.IsNullOrWhiteSpace(content))
            return [];

        var entries = new List<DatabaseEntry>();
        var lines = content.Split(['\r', '\n'], StringSplitOptions.RemoveEmptyEntries);

        foreach (var line in lines)
        {
            var trimmed = line.Trim();
            if (trimmed.Length == 0 || trimmed.StartsWith('#') || trimmed.StartsWith("//"))
                continue;

            var entry = ParseS2dLine(trimmed, sourceDatabase);
            if (entry is not null)
                entries.Add(entry);
        }
        return entries;
    }

    /// <summary>
    /// Parses a single decrypted S2D line.
    /// Format: "Name",Category,FreqCount,"Description","freq1,freq2,...",field5,field6,field7,Dwell
    /// </summary>
    private static DatabaseEntry? ParseS2dLine(string line, string sourceDatabase)
    {
        var fields = ParseCsvFields(line);
        if (fields.Count < 2)
            return null;

        var name = Unquote(fields[0]);
        if (string.IsNullOrEmpty(name))
            return null;

        var category = fields.Count > 1 ? Unquote(fields[1]) : string.Empty;

        // Frequencies are in field index 4 (comma-separated within quotes)
        var frequencies = new List<double>();
        if (fields.Count > 4)
        {
            var freqStr = Unquote(fields[4]);
            if (!string.IsNullOrEmpty(freqStr))
            {
                foreach (var part in freqStr.Split(',', StringSplitOptions.RemoveEmptyEntries))
                {
                    if (double.TryParse(part.Trim(), NumberStyles.Float, CultureInfo.InvariantCulture, out var freq))
                        frequencies.Add(freq);
                }
            }
        }

        // Filter by requested category if sourceDatabase is specific
        if (!string.IsNullOrEmpty(sourceDatabase)
            && !string.Equals(category, sourceDatabase, StringComparison.OrdinalIgnoreCase)
            && DatabaseFileMap.ContainsKey(sourceDatabase)
            && string.Equals(DatabaseFileMap[sourceDatabase], "Frequencies", StringComparison.OrdinalIgnoreCase))
        {
            return null;
        }

        return new DatabaseEntry
        {
            Name = name,
            Frequencies = frequencies,
            Category = category,
            SourceDatabase = sourceDatabase,
        };
    }

    /// <summary>Parses CSV with quoted field support (handles commas inside quotes).</summary>
    private static List<string> ParseCsvFields(string line)
    {
        var fields = new List<string>();
        bool inQuotes = false;
        int start = 0;

        for (int i = 0; i < line.Length; i++)
        {
            if (line[i] == '"')
            {
                inQuotes = !inQuotes;
            }
            else if (line[i] == ',' && !inQuotes)
            {
                fields.Add(line[start..i]);
                start = i + 1;
            }
        }
        fields.Add(line[start..]);
        return fields;
    }

    /// <summary>Parses simple CSV: Name,freq1,freq2,... (used for .csv files).</summary>
    private static List<DatabaseEntry> ParseSimpleCsvContent(string content, string sourceDatabase)
    {
        if (string.IsNullOrWhiteSpace(content))
            return [];

        var entries = new List<DatabaseEntry>();
        foreach (var rawLine in content.Split(['\r', '\n'], StringSplitOptions.RemoveEmptyEntries))
        {
            var line = rawLine.Trim();
            if (line.Length == 0 || line.StartsWith("//"))
                continue;

            var fields = line.Split(',');
            if (fields.Length < 1)
                continue;

            var name = fields[0].Trim();
            if (string.IsNullOrEmpty(name))
                continue;

            var freqs = new List<double>();
            for (int i = 1; i < fields.Length; i++)
            {
                if (double.TryParse(fields[i].Trim(), NumberStyles.Float, CultureInfo.InvariantCulture, out var f))
                    freqs.Add(f);
            }

            entries.Add(new DatabaseEntry
            {
                Name = name,
                Frequencies = freqs,
                SourceDatabase = sourceDatabase,
            });
        }
        return entries;
    }

    private static string Unquote(string s)
    {
        s = s.Trim();
        if (s.Length >= 2 && s[0] == '"' && s[^1] == '"')
            return s[1..^1];
        return s;
    }
}
