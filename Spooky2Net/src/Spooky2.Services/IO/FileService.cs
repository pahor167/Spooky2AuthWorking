using Spooky2.Core.Interfaces;

namespace Spooky2.Services.IO;

/// <summary>
/// Cross-platform file system operations using System.IO.
/// All paths are normalised to the current OS separator.
/// </summary>
public sealed class FileService : IFileService
{
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
            File.Delete(normalised);
        }
    }

    public void DeleteDirectory(string path, bool recursive = false)
    {
        string normalised = NormalisePath(path);
        if (Directory.Exists(normalised))
        {
            Directory.Delete(normalised, recursive);
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
        return await File.ReadAllTextAsync(NormalisePath(path));
    }

    public async Task WriteAllText(string path, string content)
    {
        string normalised = NormalisePath(path);
        string? dir = Path.GetDirectoryName(normalised);
        if (dir is not null && !Directory.Exists(dir))
        {
            Directory.CreateDirectory(dir);
        }

        await File.WriteAllTextAsync(normalised, content);
    }

    public async Task AppendText(string path, string content)
    {
        string normalised = NormalisePath(path);
        string? dir = Path.GetDirectoryName(normalised);
        if (dir is not null && !Directory.Exists(dir))
        {
            Directory.CreateDirectory(dir);
        }

        await File.AppendAllTextAsync(normalised, content);
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
