using Microsoft.Extensions.Logging;

namespace Spooky2.App;

/// <summary>
/// Simple file logger that appends all log messages to a text file.
/// Used for debugging when running as a GUI exe without a console window.
/// </summary>
public sealed class FileLoggerProvider : ILoggerProvider
{
    private readonly string _filePath;
    private readonly object _lock = new();

    public FileLoggerProvider(string filePath)
    {
        _filePath = filePath;
        // Clear previous log on startup
        try { File.WriteAllText(_filePath, $"=== Spooky2 Debug Log - {DateTime.Now:yyyy-MM-dd HH:mm:ss} ==={Environment.NewLine}"); }
        catch { /* ignore if can't write */ }
    }

    public ILogger CreateLogger(string categoryName) => new FileLogger(_filePath, categoryName, _lock);
    public void Dispose() { }
}

public sealed class FileLogger : ILogger
{
    private readonly string _filePath;
    private readonly string _category;
    private readonly object _lock;

    public FileLogger(string filePath, string category, object lockObj)
    {
        _filePath = filePath;
        _category = category;
        _lock = lockObj;
    }

    public IDisposable? BeginScope<TState>(TState state) where TState : notnull => null;
    public bool IsEnabled(LogLevel logLevel) => logLevel >= LogLevel.Debug;

    public void Log<TState>(LogLevel logLevel, EventId eventId, TState state, Exception? exception, Func<TState, Exception?, string> formatter)
    {
        if (!IsEnabled(logLevel)) return;

        var level = logLevel switch
        {
            LogLevel.Debug => "DBG",
            LogLevel.Information => "INF",
            LogLevel.Warning => "WRN",
            LogLevel.Error => "ERR",
            LogLevel.Critical => "CRT",
            _ => "???"
        };

        var message = $"[{DateTime.Now:HH:mm:ss.fff}] [{level}] {_category}: {formatter(state, exception)}";
        if (exception != null)
            message += $"{Environment.NewLine}  Exception: {exception}";

        lock (_lock)
        {
            try { File.AppendAllText(_filePath, message + Environment.NewLine); }
            catch { /* ignore write failures */ }
        }
    }
}
