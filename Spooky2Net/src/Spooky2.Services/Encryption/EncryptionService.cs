using System.Runtime.InteropServices;
using System.Text;
using Microsoft.Extensions.Logging;
using Microsoft.Extensions.Logging.Abstractions;
using Spooky2.Core.Interfaces;

namespace Spooky2.Services.Encryption;

/// <summary>
/// Implements VB6-compatible encryption (clsRndCrypt) using the exact MSVBVM60.DLL
/// RNG algorithm (24-bit LCG with Rnd/Randomize).
///
/// Verified against the Spooky.exe binary disassembly (VA 0x8B8F70) and
/// confirmed with x32dbg on the running process.
/// </summary>
public sealed class EncryptionService : IEncryptionService
{
    private readonly ILogger<EncryptionService> _logger;

    public EncryptionService(ILogger<EncryptionService>? logger = null)
    {
        _logger = logger ?? NullLogger<EncryptionService>.Instance;
        _logger.LogDebug("EncryptionService initialized");
    }
    /// <summary>VB6 original: RndCrypt (simple string XOR cipher).</summary>
    public string XorEncryptString(string input, string password)
    {
        _logger.LogDebug("XorEncryptString called with input length {InputLength}, password length {PasswordLength}", input?.Length ?? 0, password?.Length ?? 0);
        if (string.IsNullOrEmpty(input) || string.IsNullOrEmpty(password))
            return string.Empty;

        var rng = new Vb6Rng();
        var pwBytes = Encoding.Latin1.GetBytes(password);

        // Phase 1: Hash password
        rng.Randomize(pwBytes.Length);
        int hash = 0;
        foreach (byte b in pwBytes)
        {
            int rv = (int)(rng.RndNext() * 256);
            hash = (b ^ rv) ^ hash;
        }

        // Phase 2: XOR data with hash-seeded RNG
        rng.RndNeg1();
        rng.Randomize(hash);

        var sb = new StringBuilder(input.Length);
        foreach (char c in input)
        {
            int rv = (int)(rng.RndNext() * 256);
            sb.Append((char)(c ^ rv));
        }
        return sb.ToString();
    }

    /// <summary>VB6 original: RndCryptB (byte array XOR cipher).</summary>
    public byte[] XorEncryptBytes(byte[] input, string password)
    {
        _logger.LogDebug("XorEncryptBytes called with input length {InputLength}, password length {PasswordLength}", input?.Length ?? 0, password?.Length ?? 0);
        if (input is null || input.Length == 0 || string.IsNullOrEmpty(password))
            return [];

        var rng = new Vb6Rng();
        var pwBytes = Encoding.Latin1.GetBytes(password);

        // Phase 1: Hash password
        rng.Randomize(pwBytes.Length);
        int hash = 0;
        foreach (byte b in pwBytes)
        {
            int rv = (int)(rng.RndNext() * 256);
            hash = (b ^ rv) ^ hash;
        }

        // Phase 2: XOR data
        rng.RndNeg1();
        rng.Randomize(hash);

        var result = new byte[input.Length];
        for (int i = 0; i < input.Length; i++)
        {
            int rv = (int)(rng.RndNext() * 256);
            result[i] = (byte)(input[i] ^ rv);
        }
        return result;
    }

    /// <summary>
    /// VB6 original: RndCryptLevel2. Multi-pass XOR cipher with power-function
    /// accumulator, verified from Spooky.exe binary disassembly at VA 0x8B8F70.
    ///
    /// InputFormat: 1=StrConv(vbFromUnicode), 2=Hex, 3=Base64
    /// OutputFormat: 1=StrConv(vbUnicode), 2=Hex, 3=Base64
    /// </summary>
    public string MultiPassXorEncrypt(
        string value,
        string password,
        int seedPassCount,
        int dataPassCount,
        int inputFormat,
        int outputFormat,
        bool sanitizeInput)
    {
        _logger.LogDebug("MultiPassXorEncrypt called: valueLen={ValueLength}, seedPasses={SeedPasses}, dataPasses={DataPasses}, inFmt={InputFormat}, outFmt={OutputFormat}",
            value?.Length ?? 0, seedPassCount, dataPassCount, inputFormat, outputFormat);
        if (string.IsNullOrEmpty(value) || string.IsNullOrEmpty(password))
            return string.Empty;

        // Decode input
        byte[] data = inputFormat switch
        {
            1 => Encoding.Latin1.GetBytes(value),
            3 => Convert.FromBase64String(value),
            _ => Encoding.Latin1.GetBytes(value),
        };

        byte[] pwBytes = Encoding.Latin1.GetBytes(password);
        int pwLen = pwBytes.Length;
        int dataLen = data.Length;
        int totalCount = seedPassCount * pwLen + dataLen;

        // === SEED PASS ===
        var rng = new Vb6Rng();
        rng.RndNeg1();
        rng.Randomize(totalCount);

        int acc = 0;
        for (int i = 1; i <= totalCount; i++)
        {
            float rnd1 = rng.RndNext();
            int pwByte = pwBytes[i % pwLen];
            double exponent = (double)rnd1 * 2.7526486955 + 1.0;

            int pr = 0;
            if (pwByte > 0)
            {
                double powVal = Math.Pow(pwByte, exponent);
                pr = BankersRoundToInt(powVal);
            }

            float rnd2 = rng.RndNext();
            int r2v = BankersRoundToInt((double)rnd2 * 1000.0);
            acc = r2v | ((acc & 0x3FFFFFFF) + pr);
        }

        // === DATA PASS (first pass) ===
        rng.RndNeg1();
        rng.Randomize(acc);

        byte[] output = new byte[dataLen];
        for (int i = 0; i < dataLen; i++)
        {
            byte keyByte = (byte)(acc & 0xFF);
            float rnd1 = rng.RndNext();
            byte rndByte = BankersRoundToByte((double)rnd1 * 255.49);
            output[i] = (byte)(rndByte ^ data[i] ^ keyByte);

            float rnd3 = rng.RndNext();
            float rnd4 = rng.RndNext();
            double bv = (double)rnd3 * 255.0;
            double ev = (double)rnd4 * 2.7526486955 + 1.0;
            int pv = 0;
            try
            {
                pv = BankersRoundToInt(Math.Pow(bv, ev));
            }
            catch (Exception ex)
            {
                _logger.LogWarning(ex, "Pow overflow in MultiPassXorEncrypt data pass at index {Index}: base={Base}, exponent={Exponent}", i, bv, ev);
            }
            acc = (acc / 2) + pv;
        }

        // Additional data passes (only if dataPassCount > 1)
        for (int pass = 1; pass < dataPassCount; pass++)
        {
            for (int i = 0; i < dataLen; i++)
            {
                byte keyByte = (byte)(acc & 0xFF);
                byte rndByte = BankersRoundToByte((double)rng.RndNext() * 255.49);
                output[i] = (byte)(rndByte ^ output[i] ^ keyByte);

                float rnd3 = rng.RndNext();
                float rnd4 = rng.RndNext();
                int pv = 0;
                try
                {
                    pv = BankersRoundToInt(Math.Pow((double)rnd3 * 255.0, (double)rnd4 * 2.7526486955 + 1.0));
                }
                catch (Exception ex)
                {
                    _logger.LogWarning(ex, "Pow overflow in MultiPassXorEncrypt additional pass {Pass} at index {Index}", pass, i);
                }
                acc = (acc / 2) + pv;
            }
        }

        // Encode output
        return outputFormat switch
        {
            1 => Encoding.Latin1.GetString(output),
            3 => Convert.ToBase64String(output),
            _ => Encoding.Latin1.GetString(output),
        };
    }

    /// <summary>
    /// Decrypts an .s2d encrypted line using the pre-encrypted password.
    /// Equivalent to VB6: RndCryptLevel2(line, encryptedPassword, 1, 1, 3, 1, True)
    /// </summary>
    public string DecryptS2dLine(string base64Line, byte[] encryptedPasswordBytes)
    {
        _logger.LogDebug("DecryptS2dLine called with line length {LineLength}", base64Line?.Length ?? 0);
        return MultiPassXorEncrypt(base64Line, Encoding.Latin1.GetString(encryptedPasswordBytes), 1, 1, 3, 1, true);
    }

    /// <summary>
    /// Computes the encrypted password used for .s2d file decryption.
    /// The VB6 binary self-encrypts the raw password during Form_Load:
    ///   RndCryptLevel2(rawPassword, rawPassword, 1, 1, 1, 3, True)
    /// and stores the Base64 result for subsequent use.
    /// </summary>
    public static byte[] ComputeEncryptedPassword(string rawPassword)
    {
        var svc = new EncryptionService();
        string encryptedBase64 = svc.MultiPassXorEncrypt(rawPassword, rawPassword, 1, 1, 1, 3, true);
        return Encoding.Latin1.GetBytes(encryptedBase64);
    }

    public string Base64Encode(byte[] data) => Convert.ToBase64String(data);
    public byte[] Base64Decode(string data) => Convert.FromBase64String(data);

    private static int BankersRoundToInt(double val)
    {
        long r = (long)Math.Round(val, MidpointRounding.ToEven);
        if (r > int.MaxValue || r < int.MinValue) return 0;
        return (int)r;
    }

    private static byte BankersRoundToByte(double val)
    {
        int r = (int)Math.Round(val, MidpointRounding.ToEven);
        return (byte)Math.Clamp(r, 0, 255);
    }

    /// <summary>
    /// Exact replica of MSVBVM60.DLL's Rnd/Randomize functions.
    /// Verified against DLL disassembly (ordinals #593/#594) and x32dbg.
    ///
    /// LCG: seed = (seed * 0x43FD43FD + 0xC39EC3) &amp; 0xFFFFFF
    /// Rnd(-1): reseed from float32 bits, then advance LCG
    /// Randomize(n): mix upper bytes of double(n) into seed bits 8-23
    /// </summary>
    internal sealed class Vb6Rng
    {
        private uint _seed = 0x50000; // VB6 default

        /// <summary>Rnd(-1): reset to deterministic state from float32(-1.0) bits.</summary>
        public void RndNeg1()
        {
            uint bits = (uint)BitConverter.ToInt32(BitConverter.GetBytes(-1.0f), 0);
            _seed = (bits + (bits >> 24)) & 0xFFFFFF;
            _seed = AdvanceLcg(_seed);
        }

        /// <summary>Rnd() with no argument: advance LCG and return [0, 1).</summary>
        public float RndNext()
        {
            _seed = AdvanceLcg(_seed);
            return _seed / 16777216.0f;
        }

        /// <summary>
        /// Randomize(number): mix double representation into seed bits 8-23.
        /// Bits 0-7 are preserved from the current seed.
        /// </summary>
        public void Randomize(double number)
        {
            Span<byte> dblBytes = stackalloc byte[8];
            BitConverter.TryWriteBytes(dblBytes, number);
            uint upper = MemoryMarshal.Read<uint>(dblBytes[4..]);
            uint lValue = ((upper & 0xFFFF) << 8) ^ ((upper >> 8) & 0xFFFF00);
            _seed = (_seed & 0xFF) | (lValue & 0xFFFF00);
        }

        private static uint AdvanceLcg(uint seed)
        {
            return (seed * 0x43FD43FD + 0xC39EC3) & 0xFFFFFF;
        }
    }
}
