using Avalonia.Controls;
using Avalonia.Input;
using Spooky2.ViewModels;

namespace Spooky2.Views.Tabs;

public partial class DatabaseTab : UserControl
{
    public DatabaseTab()
    {
        InitializeComponent();
    }

    private void SearchBox_KeyDown(object? sender, KeyEventArgs e)
    {
        if (e.Key == Key.Enter && DataContext is DatabaseViewModel vm && vm.SearchDatabaseCommand.CanExecute(null))
        {
            vm.SearchDatabaseCommand.Execute(null);
        }
    }
}
