using Avalonia;
using Avalonia.Controls.ApplicationLifetimes;
using Spooky2.Core.Interfaces;

namespace Spooky2.Views.Services;

public class ClipboardService : IClipboardService
{
    public async Task SetTextAsync(string text)
    {
        var clipboard = GetClipboard();
        if (clipboard != null)
            await clipboard.SetTextAsync(text);
    }

    public async Task<string?> GetTextAsync()
    {
        var clipboard = GetClipboard();
        return clipboard != null ? await clipboard.GetTextAsync() : null;
    }

    private static Avalonia.Input.Platform.IClipboard? GetClipboard()
    {
        if (Application.Current?.ApplicationLifetime is IClassicDesktopStyleApplicationLifetime desktop)
            return desktop.MainWindow?.Clipboard;
        return null;
    }
}
