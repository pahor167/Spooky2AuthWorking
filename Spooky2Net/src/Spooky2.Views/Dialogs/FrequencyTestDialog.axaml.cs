using Avalonia.Controls;
using Avalonia.Interactivity;
using Spooky2.ViewModels.Dialogs;

namespace Spooky2.Views.Dialogs;

public partial class FrequencyTestDialog : Window
{
    public FrequencyTestDialog()
    {
        InitializeComponent();
    }

    protected override void OnLoaded(RoutedEventArgs e)
    {
        base.OnLoaded(e);
        var listBox = this.FindControl<ListBox>("ScanFreqList");
        if (listBox != null)
        {
            listBox.SelectionChanged += (s, args) =>
            {
                if (listBox.SelectedItem is string freqText && DataContext is FrequencyTestViewModel vm)
                {
                    vm.SelectFrequencyCommand.Execute(freqText);
                }
            };
        }
    }
}
