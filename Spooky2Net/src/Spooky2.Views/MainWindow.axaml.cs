using Avalonia.Controls;

namespace Spooky2.Views;

public partial class MainWindow : Window
{
    public MainWindow()
    {
        InitializeComponent();
        DataContextChanged += OnDataContextChanged;
    }

    private void OnDataContextChanged(object? sender, System.EventArgs e)
    {
        if (DataContext != null)
        {
            var splash = this.FindControl<Border>("LoadingSplash");
            if (splash != null)
                splash.IsVisible = false;
        }
    }
}
