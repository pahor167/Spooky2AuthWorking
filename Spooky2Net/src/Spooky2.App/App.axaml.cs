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
        var services = new ServiceCollection();

        // Register logging — console + file output for hardware debugging
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
        services.AddSingleton<IHidDeviceService, HidDeviceService>();
        services.AddSingleton<ISerialPortFactory, SerialPortFactory>();
        services.AddSingleton<IGeneratorService, GeneratorService>();
        services.AddSingleton<IPresetService, PresetService>();
        services.AddSingleton<IDatabaseService, DatabaseService>();
        services.AddSingleton<IWaveformService, WaveformService>();
        services.AddSingleton<IScanService, ScanService>();
        services.AddSingleton<ISettingsService, SettingsService>();
        services.AddSingleton<IColloidalSilverCalculator, ColloidalSilverCalculatorService>();
        services.AddSingleton<ICarrierSweepService, CarrierSweepService>();
        services.AddSingleton<IMicroGenService, MicroGenService>();
        services.AddSingleton<IDialogService, DialogService>();
        services.AddSingleton<IErrorLoggingService>(sp =>
            new ErrorLoggingService(AppDomain.CurrentDomain.BaseDirectory));
        services.AddSingleton<GeneratorPollingService>();

        // Register ViewModels
        services.AddTransient<MainViewModel>();
        services.AddTransient<PresetsViewModel>();
        services.AddTransient<DatabaseViewModel>();
        services.AddTransient<SettingsViewModel>();
        services.AddTransient<SystemViewModel>();
        services.AddTransient<ControlViewModel>();
        services.AddTransient<GeneratorViewModel>();

        Services = services.BuildServiceProvider();

        if (ApplicationLifetime is IClassicDesktopStyleApplicationLifetime desktop)
        {
            desktop.MainWindow = new MainWindow
            {
                DataContext = Services.GetRequiredService<MainViewModel>()
            };
        }

        base.OnFrameworkInitializationCompleted();
    }
}
