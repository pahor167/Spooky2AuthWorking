namespace Spooky2.Core.Interfaces;

public interface IEncryptionService
{
    // VB6 original: RndCrypt
    string XorEncryptString(string input, string password);
    // VB6 original: RndCryptB
    byte[] XorEncryptBytes(byte[] input, string password);
    // VB6 original: RndCryptLevel2
    string MultiPassXorEncrypt(
        string value,
        string password,
        int seedPassCount,
        int dataPassCount,
        int inputFormat,
        int outputFormat,
        bool sanitizeInput);
    string Base64Encode(byte[] data);
    byte[] Base64Decode(string data);
}
