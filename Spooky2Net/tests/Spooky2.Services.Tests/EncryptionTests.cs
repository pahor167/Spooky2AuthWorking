using Xunit;
using Spooky2.Core.Interfaces;
using Spooky2.Services.Encryption;

namespace Spooky2.Services.Tests;

/// <summary>
/// Tests for VB6-compatible encryption services.
/// These tests verify that:
/// 1. The VB6 PRNG (Randomize/Rnd) produces deterministic, reproducible sequences
/// 2. XOR encryption is symmetric (encrypt then decrypt = original)
/// 3. Frequency substitution encryption round-trips correctly
/// 4. The 100 substitution tables are valid permutations of "0123456789"
/// </summary>
public class EncryptionTests
{
    private readonly IEncryptionService _xorService = new EncryptionService();
    private readonly IFrequencyEncryptionService _freqService = new FrequencyEncryptionService();

    // ─────────────────────────────────────────────────────────────
    // VB6 PRNG determinism tests
    // ─────────────────────────────────────────────────────────────

    [Fact]
    public void Vb6Prng_SameSeed_ProducesSameSequence()
    {
        // The entire encryption system depends on Randomize(n) + Rnd()
        // producing the exact same sequence every time for the same seed.
        // If this fails, nothing else will work.
        var result1 = _xorService.XorEncryptString("test", "password");
        var result2 = _xorService.XorEncryptString("test", "password");

        Assert.Equal(result1, result2);
    }

    [Fact]
    public void Vb6Prng_DifferentSeeds_ProduceDifferentSequences()
    {
        var result1 = _xorService.XorEncryptString("test", "password1");
        var result2 = _xorService.XorEncryptString("test", "password2");

        Assert.NotEqual(result1, result2);
    }

    [Fact]
    public void Vb6Prng_EmptyPassword_ReturnsEmpty()
    {
        var result = _xorService.XorEncryptString("test", "");
        Assert.Empty(result);
    }

    // ─────────────────────────────────────────────────────────────
    // XOR encryption round-trip tests
    // ─────────────────────────────────────────────────────────────

    [Theory]
    [InlineData("Hello World", "secret")]
    [InlineData("12345.678", "key123")]
    [InlineData("a", "k")]
    [InlineData("Special chars: !@#$%^&*()", "p@ss")]
    [InlineData("Unicode: äöü", "password")]
    public void XorEncryptString_RoundTrip_ReturnsOriginal(string plaintext, string password)
    {
        // XOR is symmetric: encrypt(encrypt(x)) = x
        var encrypted = _xorService.XorEncryptString(plaintext, password);
        var decrypted = _xorService.XorEncryptString(encrypted, password);

        Assert.Equal(plaintext, decrypted);
    }

    [Theory]
    [InlineData("test data")]
    [InlineData("frequency,123.456,789.012")]
    public void XorEncryptBytes_RoundTrip_ReturnsOriginal(string plaintext)
    {
        var password = "testkey";
        var input = System.Text.Encoding.UTF8.GetBytes(plaintext);

        var encrypted = _xorService.XorEncryptBytes(input, password);
        var decrypted = _xorService.XorEncryptBytes(encrypted, password);

        Assert.Equal(input, decrypted);
    }

    [Fact]
    public void XorEncryptString_ActuallyChangesData()
    {
        var plaintext = "Hello World";
        var encrypted = _xorService.XorEncryptString(plaintext, "key");

        Assert.NotEqual(plaintext, encrypted);
    }

    [Fact]
    public void XorEncryptString_EmptyInput_ReturnsEmpty()
    {
        Assert.Equal(string.Empty, _xorService.XorEncryptString("", "key"));
        Assert.Equal(string.Empty, _xorService.XorEncryptString(null!, "key"));
    }

    // ─────────────────────────────────────────────────────────────
    // MultiPass XOR encryption tests
    // ─────────────────────────────────────────────────────────────

    [Fact]
    public void MultiPassXorEncrypt_Deterministic()
    {
        var result1 = _xorService.MultiPassXorEncrypt("test", "pass", 2, 3, 0, 0, false);
        var result2 = _xorService.MultiPassXorEncrypt("test", "pass", 2, 3, 0, 0, false);

        Assert.Equal(result1, result2);
    }

    [Fact]
    public void MultiPassXorEncrypt_EmptyInput_ReturnsEmpty()
    {
        var result = _xorService.MultiPassXorEncrypt("", "pass", 1, 1, 0, 0, false);
        Assert.Equal(string.Empty, result);
    }

    // ─────────────────────────────────────────────────────────────
    // Base64 round-trip tests
    // ─────────────────────────────────────────────────────────────

    [Theory]
    [InlineData(new byte[] { 0, 1, 2, 3, 4, 5 })]
    [InlineData(new byte[] { 255, 128, 0, 64, 32 })]
    public void Base64_RoundTrip_ReturnsOriginal(byte[] data)
    {
        var encoded = _xorService.Base64Encode(data);
        var decoded = _xorService.Base64Decode(encoded);

        Assert.Equal(data, decoded);
    }

    // ─────────────────────────────────────────────────────────────
    // Frequency encryption: substitution table validation
    // ─────────────────────────────────────────────────────────────

    [Fact]
    public void SubstitutionTables_AllAreValidPermutations()
    {
        // Each table must be a permutation of "0123456789"
        // If any table is wrong, encryption/decryption will silently corrupt data
        var tables = GetSubstitutionTables();

        Assert.Equal(100, tables.Length);

        for (int i = 0; i < tables.Length; i++)
        {
            Assert.Equal(10, tables[i].Length);

            var sorted = new string(tables[i].OrderBy(c => c).ToArray());
            Assert.Equal("0123456789", sorted);
        }
    }

    [Fact]
    public void SubstitutionTables_AllAreUnique()
    {
        var tables = GetSubstitutionTables();
        var distinct = tables.Distinct().Count();

        // Most tables should be unique (allows a few duplicates in case
        // the VB6 source had any, but there shouldn't be many)
        Assert.True(distinct >= 95, $"Only {distinct}/100 tables are unique — possible data corruption");
    }

    // ─────────────────────────────────────────────────────────────
    // Frequency encryption: round-trip tests
    // ─────────────────────────────────────────────────────────────

    [Theory]
    [InlineData("123.456")]
    [InlineData("76000")]
    [InlineData("152000.025")]
    [InlineData("0.001")]
    [InlineData("999999.999")]
    public void FrequencyEncryption_RoundTrip_ReturnsOriginal(string frequency)
    {
        var encrypted = _freqService.EncryptFrequencyLine(frequency);
        var decrypted = _freqService.DecryptFrequencyLine(encrypted);

        Assert.Equal(frequency, decrypted);
    }

    [Theory]
    [InlineData("Healing,123.456,789.012,456.789")]
    [InlineData("RIFE_Program,100,200,300,400,500")]
    [InlineData("Test-Entry,1.5,2.5,3.5")]
    public void FrequencyEncryption_RoundTrip_CsvLine(string csvLine)
    {
        var encrypted = _freqService.EncryptFrequencyLine(csvLine);
        var decrypted = _freqService.DecryptFrequencyLine(encrypted);

        Assert.Equal(csvLine, decrypted);
    }

    [Fact]
    public void FrequencyEncryption_ActuallyChangesDigits()
    {
        var original = "123456";
        var encrypted = _freqService.EncryptFrequencyLine(original);

        // Encrypted should have a 4-digit prefix + substituted digits
        Assert.True(encrypted.Length > original.Length,
            "Encrypted should be longer than original (4-digit prefix added)");

        // The prefix is 4 digits, so encrypted[4..] should differ from original
        var encryptedBody = encrypted[4..];
        Assert.NotEqual(original, encryptedBody);
    }

    [Fact]
    public void FrequencyEncryption_PreservesNonDigits()
    {
        var original = "abc.def,ghi";
        var encrypted = _freqService.EncryptFrequencyLine(original);

        // Non-digit characters should pass through unchanged
        // The prefix is 4 random digits, so encrypted = "NNNN" + "abc.def,ghi"
        var body = encrypted[4..];
        Assert.Equal(original, body);
    }

    [Fact]
    public void FrequencyEncryption_EmptyInput_ReturnsEmpty()
    {
        Assert.Equal(string.Empty, _freqService.EncryptFrequencyLine(""));
        Assert.Equal(string.Empty, _freqService.EncryptFrequencyLine(null!));
    }

    [Fact]
    public void FrequencyDecryption_ShortInput_ReturnsUnchanged()
    {
        // Lines < 5 chars should pass through (not enough for 4-digit prefix + data)
        Assert.Equal("1234", _freqService.DecryptFrequencyLine("1234"));
        Assert.Equal("abc", _freqService.DecryptFrequencyLine("abc"));
    }

    [Fact]
    public void FrequencyEncryption_Deterministic()
    {
        // Same input must produce same output every time
        // (VB6 seeds with Randomize(10) which is constant)
        var result1 = _freqService.EncryptFrequencyLine("123.456");
        var result2 = _freqService.EncryptFrequencyLine("123.456");

        Assert.Equal(result1, result2);
    }

    [Fact]
    public void FrequencyEncryption_PrefixIsFourDigits()
    {
        var encrypted = _freqService.EncryptFrequencyLine("100");

        Assert.True(encrypted.Length >= 4);
        Assert.True(char.IsDigit(encrypted[0]));
        Assert.True(char.IsDigit(encrypted[1]));
        Assert.True(char.IsDigit(encrypted[2]));
        Assert.True(char.IsDigit(encrypted[3]));
    }

    [Fact]
    public void FrequencyEncryption_RoundTrip_ManyFrequencies()
    {
        // Test a large batch to catch edge cases with table wrapping
        var frequencies = new[]
        {
            "1", "10", "100", "1000", "10000", "100000", "1000000",
            "0.1", "0.01", "0.001",
            "76000", "152000", "200000",
            "999999999",
            "3.1415926535",
            "0",
        };

        foreach (var freq in frequencies)
        {
            var encrypted = _freqService.EncryptFrequencyLine(freq);
            var decrypted = _freqService.DecryptFrequencyLine(encrypted);

            Assert.Equal(freq, decrypted);
        }
    }

    [Fact]
    public void FrequencyEncryption_RoundTrip_AllSingleDigits()
    {
        for (int d = 0; d <= 9; d++)
        {
            var original = d.ToString();
            var encrypted = _freqService.EncryptFrequencyLine(original);
            var decrypted = _freqService.DecryptFrequencyLine(encrypted);

            Assert.Equal(original, decrypted);
        }
    }

    // ─────────────────────────────────────────────────────────────
    // Helper: extract substitution tables via reflection
    // ─────────────────────────────────────────────────────────────

    private static string[] GetSubstitutionTables()
    {
        var field = typeof(FrequencyEncryptionService)
            .GetField("DigitSubstitutionTables",
                System.Reflection.BindingFlags.NonPublic | System.Reflection.BindingFlags.Static);

        Assert.NotNull(field);
        var tables = field!.GetValue(null) as string[];
        Assert.NotNull(tables);

        return tables!;
    }
}
