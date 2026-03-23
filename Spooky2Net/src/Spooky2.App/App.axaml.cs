using Avalonia;
using Avalonia.Controls.ApplicationLifetimes;
using Avalonia.Markup.Xaml;
using Microsoft.Extensions.DependencyInjection;
using Spooky2.Core.Interfaces;
using Spooky2.Services.Communication;
using Spooky2.Services.Database;
using Spooky2.Services.Encryption;
using Spooky2.Services.IO;
using Spooky2.Services.Presets;
using Spooky2.Services.Settings;
using Spooky2.Services.Waveform;
using Spooky2.Services.Scanner;
using Spooky2.Services.Calculator;
using Spooky2.Services.CarrierSweep;
using Microsoft.Extensions.Logging;
using Spooky2.ViewModels;
using Spooky2.Views;
using Spooky2.Views.Services;
using System.Threading.Tasks;

namespace Spooky2.App;

public class App : Application
{
    public static IServiceProvider? Services { get; private set; }

    public override void Initialize()
    {
        AvaloniaXamlLoader.Load(this);
    }

    public override void OnFrameworkInitializationCompleted()
    {
        if (ApplicationLifetime is IClassicDesktopStyleApplicationLifetime desktop)
        {
            // Show window IMMEDIATELY with loading state — before DI/services
            var window = new MainWindow();
            desktop.MainWindow = window;
            window.Show();

            // Build services and set DataContext in background
            _ = InitializeAsync(window);
        }

        base.OnFrameworkInitializationCompleted();
    }

    private static async Task InitializeAsync(MainWindow window)
    {
        // Small yield to let the window paint first
        await Task.Delay(50);

        var services = new ServiceCollection();

        // Register logging
        var logFilePath = Path.Combine(AppDomain.CurrentDomain.BaseDirectory, "debug.log");
        services.AddLogging(builder =>
        {
            builder.AddDebug();
            builder.AddConsole();
            builder.AddProvider(new FileLoggerProvider(logFilePath));
            builder.SetMinimumLevel(LogLevel.Debug);
        });

        // Register services
        services.AddSingleton<IEncryptionService, EncryptionService>();
        services.AddSingleton<IFrequencyEncryptionService, FrequencyEncryptionService>();
        services.AddSingleton<IFileService, FileService>();
        services.AddSingleton<ISettingsService, SettingsService>();
        services.AddSingleton<ISerialPortFactory, SerialPortFactory>();
        services.AddSingleton<IGeneratorService, GeneratorService>();
        services.AddSingleton<IPresetService, PresetService>();
        services.AddSingleton<IDatabaseService, DatabaseService>();
        services.AddSingleton<IWaveformService, WaveformService>();
        services.AddSingleton<IScanService, ScanService>();
        services.AddSingleton<IColloidalSilverCalculator, ColloidalSilverCalculatorService>();
        services.AddSingleton<ICarrierSweepService, CarrierSweepService>();
        services.AddSingleton<IMicroGenService, MicroGenService>();
        services.AddSingleton<IDialogService, DialogService>();

        // Register ViewModels
        services.AddTransient<MainViewModel>();
        services.AddTransient<PresetsViewModel>();
        services.AddTransient<DatabaseViewModel>();
        services.AddTransient<SettingsViewModel>();
        services.AddTransient<SystemViewModel>();
        services.AddTransient<ControlViewModel>();
        services.AddTransient<GeneratorViewModel>();

        Services = services.BuildServiceProvider();

        // Set DataContext on UI thread — window is already visible
        Avalonia.Threading.Dispatcher.UIThread.Post(() =>
        {
            window.DataContext = Services.GetRequiredService<MainViewModel>();
        });
    }
}
