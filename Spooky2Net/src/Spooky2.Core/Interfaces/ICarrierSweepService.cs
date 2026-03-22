using Spooky2.Core.Models;

namespace Spooky2.Core.Interfaces;

public interface ICarrierSweepService
{
    Task<FrequencyProgram> CreateCarrierSweep(CarrierSweepParams parameters);
    Task<FrequencyProgram> CreateSpectrumSweep(CarrierSweepParams parameters);
}
