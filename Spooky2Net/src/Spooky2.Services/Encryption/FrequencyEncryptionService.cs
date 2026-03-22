using System.Text;
using Spooky2.Core.Interfaces;

namespace Spooky2.Services.Encryption;

/// <summary>
/// Implements frequency-line encryption/decryption using 100 digit substitution tables.
/// This is a separate system from the XOR-based EncryptionService.
/// Ported from Main.frm EncryptFreq / DecryptFreq (lines 17935-18097).
/// </summary>
public sealed class FrequencyEncryptionService : IFrequencyEncryptionService
{
    private const string Digits = "0123456789";

    // VB6 original: 100 10-digit permutation strings initialized in Form_Load
    private static readonly string[] DigitSubstitutionTables =
    [
        "5394802167", "8513607942", "7104923586", "6724189035", "0157394286",
        "9167052843", "3874052619", "9026714385", "2589041367", "3608217549",
        "6254078931", "1468297350", "6314098257", "3697801254", "8024796351",
        "2039154678", "1584367209", "6085312479", "1875046932", "9348762150",
        "6957034128", "0685974321", "4952716308", "6032415978", "6925830714",
        "0134897652", "9735468120", "9257681043", "8370265914", "0521836497",
        "7609514823", "2103687495", "9685032417", "1365804279", "9820173465",
        "5312478960", "6745210398", "9012438657", "6752389014", "6123475890",
        "0692873154", "0423795681", "7563419082", "0859621347", "6209187435",
        "3029687415", "9264350871", "6782953041", "5238079164", "7126385049",
        "7653421089", "7854102396", "7081642395", "3087629415", "1083457962",
        "7849105326", "9423857610", "3628145790", "1039247586", "2764195308",
        "5628017349", "1059728643", "0632189475", "6104739825", "2305841967",
        "0643219587", "0469213587", "5083927614", "0245387196", "7516290843",
        "0752419386", "7356190248", "3501674829", "1530468729", "4315268970", // VB6 decompiler artifact: original had "4315299470" (invalid permutation, missing 6 and 8)
        "2319745860", "4826059713", "6035278419", "9761058243", "3958204176",
        "0986745312", "7596108342", "6230954817", "4905812367", "9517284306",
        "1208346597", "8461203579", "1076925348", "3185760942", "5321648970",
        "5308976142", "2734108965", "4879652310", "1836425709", "6901587342",
        "0962374851", "2765183049", "7321489506", "2564817039", "3705896214"
    ];

    // VB6 original: EncryptFreq
    public string EncryptFrequencyLine(string frequencyDataLine)
    {
        if (string.IsNullOrEmpty(frequencyDataLine))
            return string.Empty;

        // Check for marker character (first char check from VB6)
        if (frequencyDataLine[0] == '\0')
            return string.Empty;

        var rng = new Vb6CompatibleRandom();
        rng.Seed(10);

        var sb = new StringBuilder();

        // Generate 4-digit random key prefix
        int runningSum = 0;
        for (int i = 0; i < 4; i++)
        {
            int digit = (int)(rng.NextFloat() * 9);
            sb.Append(digit);
            runningSum += digit;
        }

        // Encrypt each character
        for (int i = 0; i < frequencyDataLine.Length; i++)
        {
            char c = frequencyDataLine[i];
            int digitIndex = Digits.IndexOf(c);

            if (digitIndex < 0)
            {
                // Not a digit - append as-is
                sb.Append(c);
            }
            else
            {
                // Digit found: substitute using the current table
                int tableIndex = runningSum % 100;
                string table = DigitSubstitutionTables[tableIndex];
                // Encrypt: table[digitIndex] gives the substituted character
                sb.Append(table[digitIndex]);
            }

            // VB6 InStr is 1-based position; digitIndex from "0123456789" maps to position = digitIndex + 1
            // But in VB6 code, position comes from InStr(1, "0123456789", char) which returns 1 for "0", etc.
            // We add the 1-based position to the running sum
            if (digitIndex >= 0)
            {
                runningSum += digitIndex + 1;
            }

            if (runningSum >= 100)
            {
                runningSum %= 100;
            }
        }

        return sb.ToString();
    }

    // VB6 original: DecryptFreq
    public string DecryptFrequencyLine(string frequencyDataLine)
    {
        // Minimum length is 5: 4-digit prefix + at least 1 character of data
        if (string.IsNullOrEmpty(frequencyDataLine) || frequencyDataLine.Length < 5)
            return frequencyDataLine ?? string.Empty;

        // Check for marker character
        if (frequencyDataLine[0] == '\0')
            return frequencyDataLine;

        // Extract the 4-digit key prefix from positions 1-4 (0-indexed)
        // In VB6 the first character is at position 1, prefix digits are at 2-5
        // Here the prefix starts right at index 0 (the first 4 chars)
        int runningSum = 0;
        for (int i = 0; i < 4; i++)
        {
            char c = frequencyDataLine[i];
            int digitValue = c - '0';
            if (digitValue < 0 || digitValue > 9)
                return frequencyDataLine; // Not a valid encrypted string
            runningSum += digitValue;
        }

        var sb = new StringBuilder();

        // Process from position 4 onward (after the 4-digit prefix)
        for (int i = 4; i < frequencyDataLine.Length; i++)
        {
            char c = frequencyDataLine[i];
            int digitIndex = Digits.IndexOf(c);

            if (digitIndex < 0)
            {
                // Not a digit - append as-is
                sb.Append(c);
            }
            else
            {
                // Decrypt: find position of the encrypted digit within the substitution table
                int tableIndex = runningSum % 100;
                string table = DigitSubstitutionTables[tableIndex];
                int originalDigit = table.IndexOf(c);
                sb.Append(originalDigit);

                // Add the original digit's 1-based position to the running sum
                runningSum += originalDigit + 1;
            }

            if (digitIndex >= 0 && runningSum >= 100)
            {
                runningSum %= 100;
            }
        }

        return sb.ToString();
    }

    /// <summary>
    /// Faithfully replicates VB6's built-in Randomize(seed) and Rnd() functions.
    /// </summary>
    // VB6 original class: implicitly used by Randomize/Rnd
    private sealed class Vb6CompatibleRandom
    {
        private int _seed;

        // VB6 original: Randomize(n)
        public void Seed(double seed)
        {
            int intSeed = unchecked((int)seed);
            _seed = intSeed ^ 0x4C3B2A1;
        }

        // VB6 original: Rnd()
        public float NextFloat()
        {
            _seed = unchecked((_seed * 0x43FD43FD) + 0xC39EC3);
            int bits = _seed & 0xFFFFFF;
            return bits / (float)0x1000000;
        }
    }
}
