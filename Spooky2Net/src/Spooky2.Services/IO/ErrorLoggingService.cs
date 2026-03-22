using System.Reflection;
using System.Runtime.InteropServices;
using Spooky2.Core.Interfaces;

namespace Spooky2.Services.IO;

/// <summary>
/// Error logging service ported from VB6 WriteErrorLogFile (Main.frm:18099).
/// Appends timestamped error entries to Data/Error.log with auto-truncation at 10 MB.
/// </summary>
public sealed class ErrorLoggingService : IErrorLoggingService
{
    private const long MaxFileSizeBytes = 10_485_760; // 10 MB
    private const string LogFileName = "Error.log";

    private readonly string _logFilePath;
    private readonly SemaphoreSlim _writeLock = new(1, 1);
    private bool _headerWritten;

    public ErrorLoggingService(string rootPath)
    {
        ArgumentException.ThrowIfNullOrWhiteSpace(rootPath);

        var dataDir = Path.Combine(rootPath, "Data");
        Directory.CreateDirectory(dataDir);
        _logFilePath = Path.Combine(dataDir, LogFileName);
    }

    public async Task WriteError(string procedureName, string errorSource, string errorDescription)
    {
        await _writeLock.WaitAsync().ConfigureAwait(false);
        try
        {
            await TruncateIfNeededInternal().ConfigureAwait(false);

            if (!_headerWritten)
            {
                await WriteHeader().ConfigureAwait(false);
                _headerWritten = true;
            }

            var timestamp = DateTime.Now.ToString("yyyy-MM-dd HH:mm:ss");
            var entry = $"{timestamp}  {procedureName}  {errorSource},  {errorDescription}{Environment.NewLine}";
            await File.AppendAllTextAsync(_logFilePath, entry).ConfigureAwait(false);
        }
        finally
        {
            _writeLock.Release();
        }
    }

    public async Task TruncateIfNeeded()
    {
        await _writeLock.WaitAsync().ConfigureAwait(false);
        try
        {
            await TruncateIfNeededInternal().ConfigureAwait(false);
        }
        finally
        {
            _writeLock.Release();
        }
    }

    private Task TruncateIfNeededInternal()
    {
        if (!File.Exists(_logFilePath))
        {
            return Task.CompletedTask;
        }

        var fileInfo = new FileInfo(_logFilePath);
        if (fileInfo.Length >= MaxFileSizeBytes)
        {
            File.Delete(_logFilePath);
            _headerWritten = false;
        }

        return Task.CompletedTask;
    }

    private async Task WriteHeader()
    {
        var version = Assembly.GetEntryAssembly()?.GetName().Version?.ToString() ?? "unknown";
        var bitness = Environment.Is64BitOperatingSystem ? "64" : "32";

        // Generator count is not known at this level; header uses a placeholder.
        // The VB6 original embedded the live generator count, but the logging service
        // intentionally avoids a dependency on IGeneratorService to keep concerns separated.
        var header =
            $"---------------- Spooky v{version}. Windows {bitness} bit ----------------{Environment.NewLine}";

        await File.AppendAllTextAsync(_logFilePath, header).ConfigureAwait(false);
    }
}
