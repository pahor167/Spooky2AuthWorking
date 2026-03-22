using CommunityToolkit.Mvvm.ComponentModel;
using CommunityToolkit.Mvvm.Input;

namespace Spooky2.ViewModels.Dialogs;

public partial class IdentifyGeneratorsViewModel : ObservableObject
{
    [ObservableProperty]
    private string _statusText = "Searching for generators...";

    [RelayCommand]
    private void Close()
    {
        // Stub: close the dialog
    }

    [RelayCommand]
    private void Refresh()
    {
        StatusText = "Searching for generators...";
        // Stub: re-scan for generators
    }
}
