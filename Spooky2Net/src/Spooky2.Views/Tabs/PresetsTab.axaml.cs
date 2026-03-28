using Avalonia.Controls;
using Avalonia.Input;
using Avalonia.Interactivity;
using Spooky2.ViewModels;

namespace Spooky2.Views.Tabs;

public partial class PresetsTab : UserControl
{
    public PresetsTab()
    {
        InitializeComponent();
    }

    private void PresetListBox_DoubleTapped(object? sender, TappedEventArgs e)
    {
        if (DataContext is PresetsViewModel vm && vm.LoadPresetCommand.CanExecute(null))
        {
            vm.LoadPresetCommand.Execute(null);
        }
    }
}
