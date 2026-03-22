namespace Spooky2.Core.Interfaces;

public interface IFrequencyEncryptionService
{
    // VB6 original: EncryptFreq
    string EncryptFrequencyLine(string frequencyDataLine);
    // VB6 original: DecryptFreq
    string DecryptFrequencyLine(string frequencyDataLine);
}
