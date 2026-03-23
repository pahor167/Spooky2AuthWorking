using Avalonia;

namespace Spooky2.App;

public static class Program
{
    [STAThread]
    public static void Main(string[] args)
    {
        // Catch ANY unhandled exception — write to crash.log so the user sees it
        AppDomain.CurrentDomain.UnhandledException += (_, e) =>
        {
            var crashLog = Path.Combine(AppDomain.CurrentDomain.BaseDirectory, "crash.log");
            var msg = $"[{DateTime.Now:yyyy-MM-dd HH:mm:ss}] UNHANDLED: {e.ExceptionObject}\n";
            try { File.AppendAllText(crashLog, msg); } catch { }
        };

        TaskScheduler.UnobservedTaskException += (_, e) =>
        {
            var crashLog = Path.Combine(AppDomain.CurrentDomain.BaseDirectory, "crash.log");
            var msg = $"[{DateTime.Now:yyyy-MM-dd HH:mm:ss}] UNOBSERVED TASK: {e.Exception}\n";
            try { File.AppendAllText(crashLog, msg); } catch { }
            e.SetObserved(); // Don't crash the app
        };

        try
        {
            BuildAvaloniaApp().StartWithClassicDesktopLifetime(args);
        }
        catch (Exception ex)
        {
            var crashLog = Path.Combine(AppDomain.CurrentDomain.BaseDirectory, "crash.log");
            var msg = $"[{DateTime.Now:yyyy-MM-dd HH:mm:ss}] FATAL MAIN: {ex}\n";
            try { File.AppendAllText(crashLog, msg); } catch { }
            throw;
        }
    }

    public static AppBuilder BuildAvaloniaApp()
        => AppBuilder.Configure<App>()
            .UsePlatformDetect()
            .LogToTrace();
}
