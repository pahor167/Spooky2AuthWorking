namespace Spooky2.Core.Interfaces;

public interface IClipboardService
{
    Task SetTextAsync(string text);
    Task<string?> GetTextAsync();
}
