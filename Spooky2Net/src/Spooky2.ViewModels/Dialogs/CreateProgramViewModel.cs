using CommunityToolkit.Mvvm.ComponentModel;
using CommunityToolkit.Mvvm.Input;

namespace Spooky2.ViewModels.Dialogs;

public partial class CreateProgramViewModel : ObservableObject
{
    [ObservableProperty]
    private string _programName = "";

    [ObservableProperty]
    private string _frequencyText = "";

    [ObservableProperty]
    private string _frequencySource = "";

    [ObservableProperty]
    private string _notes = "";

    [ObservableProperty]
    private int _defaultDwell = 180;

    [RelayCommand]
    private void SaveProgram()
    {
        // Stub: save the program with entered frequencies
    }
}
