using Avalonia;
using Avalonia.Controls;
using Avalonia.Controls.ApplicationLifetimes;
using Spooky2.Core.Interfaces;
using Spooky2.ViewModels.Dialogs;
using Spooky2.Views.Dialogs;

namespace Spooky2.Views.Services;

public class DialogService : IDialogService
{
    private static readonly Dictionary<Type, Func<Window>> ViewModelToWindowMap = new()
    {
        { typeof(ColloidalSilverViewModel), () => new ColloidalSilverCalculatorView() },
        { typeof(CarrierSweepViewModel), () => new CustomCarrierSweepView() },
        { typeof(SpectrumSweepViewModel), () => new CustomSpectrumSweepView() },
        { typeof(CreateProgramViewModel), () => new CreateProgramView() },
        { typeof(ScanResultsViewModel), () => new ScanResultsView() },
        { typeof(IdentifyGeneratorsViewModel), () => new IdentifyGeneratorsView() },
        { typeof(DebugViewModel), () => new DebugView() },
        { typeof(AboutViewModel), () => new AboutView() },
    };

    public async Task ShowDialogAsync<TViewModel>(TViewModel viewModel) where TViewModel : class
    {
        if (!ViewModelToWindowMap.TryGetValue(typeof(TViewModel), out var windowFactory))
        {
            throw new InvalidOperationException(
                $"No dialog window registered for view model type '{typeof(TViewModel).Name}'.");
        }

        var window = windowFactory();
        window.DataContext = viewModel;

        var parentWindow = GetParentWindow();
        if (parentWindow is not null)
        {
            await window.ShowDialog(parentWindow);
        }
        else
        {
            window.Show();
        }
    }

    public async Task ShowDialogAsync(string dialogName)
    {
        var window = dialogName switch
        {
            "ColloidalSilverCalculator" => (Window)new ColloidalSilverCalculatorView(),
            "CarrierSweep" => new CustomCarrierSweepView(),
            "SpectrumSweep" => new CustomSpectrumSweepView(),
            "CreateProgram" => new CreateProgramView(),
            "ScanResults" => new ScanResultsView(),
            "IdentifyGenerators" => new IdentifyGeneratorsView(),
            "Debug" => new DebugView(),
            "About" => new AboutView(),
            _ => throw new InvalidOperationException($"Unknown dialog name '{dialogName}'.")
        };

        var parentWindow = GetParentWindow();
        if (parentWindow is not null)
        {
            await window.ShowDialog(parentWindow);
        }
        else
        {
            window.Show();
        }
    }

    private static Window? GetParentWindow()
    {
        if (Application.Current?.ApplicationLifetime is IClassicDesktopStyleApplicationLifetime desktop)
        {
            return desktop.MainWindow;
        }

        return null;
    }
}
