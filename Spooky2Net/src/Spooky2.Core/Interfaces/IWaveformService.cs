using Spooky2.Core.Models;

namespace Spooky2.Core.Interfaces;

public interface IWaveformService
{
    Task GenerateWav(WaveformSettings settings, string outputPath);
    Task RefreshWaveforms();
    List<string> GetWaveformTypes();
}
