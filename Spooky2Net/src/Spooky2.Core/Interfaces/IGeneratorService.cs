using Spooky2.Core.Models;

namespace Spooky2.Core.Interfaces;

public interface IGeneratorService
{
    Task<List<GeneratorState>> FindGenerators();
    Task Start(int generatorId);
    Task Stop(int generatorId);
    Task Pause(int generatorId);
    Task Hold(int generatorId);
    Task Resume(int generatorId);
    Task WriteFrequencies(int generatorId, List<double> frequencies);
    Task<GeneratorState> ReadStatus(int generatorId);
    Task EraseMemory(int generatorId);
    Task IdentifyGenerators();
    Task SendRawCommand(int generatorId, string command);
    Task<string?> SendCommandWithResponse(int generatorId, string command);
    /// <summary>Send multiple commands rapidly without waiting for individual responses.
    /// Used for amplitude ramp-up (330 steps in ~2s, matching original Spooky2).</summary>
    Task SendCommandsBatch(int generatorId, IReadOnlyList<string> commands);
}
