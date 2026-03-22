using Spooky2.Core.Models;

namespace Spooky2.Core.Interfaces;

public interface IPresetService
{
    Task<Preset> LoadPreset(string path);
    Task SavePreset(Preset preset, string path);
    Task DeletePreset(string path);
    Task<List<string>> SearchPresets(string searchText, string directory);
    Task<PresetChain> LoadPresetChain(string path);
    Task SavePresetChain(PresetChain chain, string path);
    Task UpdatePresets();
}
