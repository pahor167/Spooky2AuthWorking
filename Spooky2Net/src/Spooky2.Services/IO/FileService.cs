using Microsoft.Extensions.Logging;
using Microsoft.Extensions.Logging.Abstractions;
using Spooky2.Core.Interfaces;

namespace Spooky2.Services.IO;

/// <summary>
/// Cross-platform file system operations using System.IO.
/// All paths are normalised to the current OS separator.
/// </summary>
public sealed class FileService : IFileService
{
    private readonly ILogger<FileService> _logger;

    public FileService(ILogger<FileService>? logger = null)
    {
        _logger = logger ?? NullLogger<FileService>.Instance;
        _logger.LogDebug("FileService initialized");
    }
    public bool Exists(string path)
    {
        return File.Exists(NormalisePath(path));
    }

    public bool IsDirectory(string path)
    {
        return Directory.Exists(NormalisePath(path));
    }

    public void DeleteFile(string path)
    {
        string normalised = NormalisePath(path);
        if (File.Exists(normalised))
        {
            _logger.LogDebug("Deleting file '{Path}'", normalised);
            File.Delete(normalised);
        }
        else
        {
            _logger.LogWarning("Attempted to delete non-existent file '{Path}'", normalised);
        }
    }

    public void DeleteDirectory(string path, bool recursive = false)
    {
        string normalised = NormalisePath(path);
        if (Directory.Exists(normalised))
        {
            _logger.LogDebug("Deleting directory '{Path}' (recursive={Recursive})", normalised, recursive);
            Directory.Delete(normalised, recursive);
        }
        else
        {
            _logger.LogWarning("Attempted to delete non-existent directory '{Path}'", normalised);
        }
    }

    public string[] GetFiles(string directory, string pattern = "*")
    {
        string normalised = NormalisePath(directory);
        if (!Directory.Exists(normalised))
            return [];

        return Directory.GetFiles(normalised, pattern);
    }

    public string[] GetDirectories(string directory)
    {
        string normalised = NormalisePath(directory);
        if (!Directory.Exists(normalised))
            return [];

        return Directory.GetDirectories(normalised);
    }

    public void CreateDirectory(string path)
    {
        Directory.CreateDirectory(NormalisePath(path));
    }

    public async Task<string> ReadAllText(string path)
    {
        string normalised = NormalisePath(path);
        _logger.LogDebug("Reading file '{Path}'", normalised);
        try
        {
            return await File.ReadAllTextAsync(normalised);
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Failed to read file '{Path}'", normalised);
            throw;
        }
    }

    public async Task WriteAllText(string path, string content)
    {
        string normalised = NormalisePath(path);
        _logger.LogDebug("Writing file '{Path}' ({Length} chars)", normalised, content?.Length ?? 0);
        string? dir = Path.GetDirectoryName(normalised);
        if (dir is not null && !Directory.Exists(dir))
        {
            Directory.CreateDirectory(dir);
        }

        try
        {
            await File.WriteAllTextAsync(normalised, content);
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Failed to write file '{Path}'", normalised);
            throw;
        }
    }

    public async Task AppendText(string path, string content)
    {
        string normalised = NormalisePath(path);
        _logger.LogDebug("Appending to file '{Path}' ({Length} chars)", normalised, content?.Length ?? 0);
        string? dir = Path.GetDirectoryName(normalised);
        if (dir is not null && !Directory.Exists(dir))
        {
            Directory.CreateDirectory(dir);
        }

        try
        {
            await File.AppendAllTextAsync(normalised, content);
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Failed to append to file '{Path}'", normalised);
            throw;
        }
    }

    /// <summary>
    /// Normalise path separators for the current OS so that paths created on
    /// Windows (backslash) work on macOS/Linux (forward slash) and vice versa.
    /// </summary>
    private static string NormalisePath(string path)
    {
        if (string.IsNullOrWhiteSpace(path))
            return path;

        // Replace both separators with the OS-native one
        return path
            .Replace('\\', Path.DirectorySeparatorChar)
            .Replace('/', Path.DirectorySeparatorChar);
    }
}
