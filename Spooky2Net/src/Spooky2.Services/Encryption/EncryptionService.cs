using System.Text;
using System.Text.RegularExpressions;
using Spooky2.Core.Interfaces;

namespace Spooky2.Services.Encryption;

/// <summary>
/// Implements VB6-compatible XOR encryption (clsRndCrypt.cls) using a faithful
/// port of the VB6 Linear Congruential Generator (Randomize / Rnd).
/// </summary>
public sealed class EncryptionService : IEncryptionService
{
    // VB6 original: RndCrypt
    public string XorEncryptString(string input, string password)
    {
        if (string.IsNullOrEmpty(input))
            return string.Empty;

        var rng = new Vb6CompatibleRandom();
        rng.Seed(ComputePasswordHash(password));

        var sb = new StringBuilder(input.Length);
        for (int i = 0; i < input.Length; i++)
        {
            int rndValue = (int)(rng.NextFloat() * 256);
            sb.Append((char)(input[i] ^ rndValue));
        }
        return sb.ToString();
    }

    // VB6 original: RndCryptB
    public byte[] XorEncryptBytes(byte[] input, string password)
    {
        if (input is null || input.Length == 0)
            return [];

        var rng = new Vb6CompatibleRandom();
        rng.Seed(ComputePasswordHash(password));

        var result = new byte[input.Length];
        for (int i = 0; i < input.Length; i++)
        {
            int rndValue = (int)(rng.NextFloat() * 256);
            result[i] = (byte)(input[i] ^ rndValue);
        }
        return result;
    }

    // VB6 original: RndCryptLevel2
    public string MultiPassXorEncrypt(
        string value,
        string password,
        int seedPassCount,
        int dataPassCount,
        int inputFormat,
        int outputFormat,
        bool sanitizeInput)
    {
        if (string.IsNullOrEmpty(value))
            return string.Empty;

        // Decode input based on inputFormat (0 = plain text, 1 = hex)
        string workingValue = inputFormat == 1 ? HexToString(value) : value;

        // Multi-pass seed derivation: hash the password through seedPassCount iterations
        string derivedPassword = password;
        for (int i = 0; i < seedPassCount; i++)
        {
            derivedPassword = XorEncryptString(derivedPassword, password);
        }

        // Multi-pass data encryption
        string result = workingValue;
        for (int i = 0; i < dataPassCount; i++)
        {
            result = XorEncryptString(result, derivedPassword);
        }

        // Encode output based on outputFormat (0 = plain text, 1 = hex)
        if (outputFormat == 1)
        {
            result = StringToHex(result);
        }

        // Optionally strip characters that are not valid hex digits
        if (sanitizeInput)
        {
            result = Regex.Replace(result, "[^0-9A-Fa-f]", string.Empty);
        }

        return result;
    }

    public string Base64Encode(byte[] data)
    {
        return Convert.ToBase64String(data);
    }

    public byte[] Base64Decode(string data)
    {
        return Convert.FromBase64String(data);
    }

    /// <summary>
    /// Derives a numeric seed from the password by summing character values.
    /// Mirrors the VB6 approach of creating a deterministic seed from a string.
    /// </summary>
    // VB6 original: GetPasswordSeed
    private static double ComputePasswordHash(string password)
    {
        double seed = 0;
        for (int i = 0; i < password.Length; i++)
        {
            seed += password[i] * (i + 1);
        }
        return seed;
    }

    private static string StringToHex(string input)
    {
        var sb = new StringBuilder(input.Length * 2);
        foreach (char c in input)
        {
            sb.Append(((int)c).ToString("X2"));
        }
        return sb.ToString();
    }

    private static string HexToString(string hex)
    {
        var sb = new StringBuilder(hex.Length / 2);
        for (int i = 0; i < hex.Length - 1; i += 2)
        {
            sb.Append((char)Convert.ToByte(hex.Substring(i, 2), 16));
        }
        return sb.ToString();
    }

    /// <summary>
    /// Faithfully replicates VB6's built-in Randomize(seed) and Rnd() functions.
    /// VB6 uses a 24-bit Linear Congruential Generator:
    ///   seed = seed XOR &amp;H4C3B2A1 (during Randomize)
    ///   Rnd  = ((seed * &amp;H43FD43FD) + &amp;HC39EC3) AND &amp;HFFFFFF  /  &amp;H1000000
    /// The seed is stored as a 32-bit integer but only 24 bits participate in Rnd output.
    /// </summary>
    // VB6 original class: implicitly used by Randomize/Rnd
    private sealed class Vb6CompatibleRandom
    {
        private int _seed;

        /// <summary>
        /// VB6 Randomize statement. Mixes the supplied numeric seed into the
        /// internal state using XOR with the constant 0x4C3B2A1.
        /// </summary>
        // VB6 original: Randomize(n)
        public void Seed(double seed)
        {
            // VB6 truncates the seed to a 32-bit integer before XOR
            int intSeed = unchecked((int)seed);
            _seed = intSeed ^ 0x4C3B2A1;
        }

        /// <summary>
        /// VB6 Rnd function. Returns a Single in [0, 1) using the 24-bit LCG.
        /// </summary>
        // VB6 original: Rnd()
        public float NextFloat()
        {
            // Advance the LCG state (all arithmetic in unchecked 32-bit)
            _seed = unchecked((_seed * 0x43FD43FD) + 0xC39EC3);

            // Extract lower 24 bits and normalise to [0, 1)
            int bits = _seed & 0xFFFFFF;
            return bits / (float)0x1000000;
        }
    }
}
