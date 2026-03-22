using System.Collections.ObjectModel;
using CommunityToolkit.Mvvm.ComponentModel;
using CommunityToolkit.Mvvm.Input;

namespace Spooky2.ViewModels.Dialogs;

public partial class DebugViewModel : ObservableObject
{
    public ObservableCollection<string> DebugMessages { get; } = new();

    public ObservableCollection<string> ErrorMessages { get; } = new();

    [RelayCommand]
    private void ClearDebug()
    {
        DebugMessages.Clear();
    }

    [RelayCommand]
    private void ClearErrors()
    {
        ErrorMessages.Clear();
    }
}
