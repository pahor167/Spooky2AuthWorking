using Spooky2.Core.Models;

namespace Spooky2.Core.Interfaces;

public interface IColloidalSilverCalculator
{
    ColloidalSilverResult CalculateByMeasurement(ColloidalSilverParams parameters);
    ColloidalSilverResult CalculateByCalculation(ColloidalSilverParams parameters);
}
