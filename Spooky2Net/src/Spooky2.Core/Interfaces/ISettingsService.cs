namespace Spooky2.Core.Interfaces;

public interface ISettingsService
{
    Task<Dictionary<string, string>> LoadSettings(string configFile);
    Task SaveSettings(string configFile, Dictionary<string, string> settings);
    Task RestoreDefaults();
    Task RestoreBfbDefaults();
}
