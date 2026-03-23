using System;
using System.IO;
using System.Text;
using Microsoft.VisualBasic;

class Program
{
    // Exact port of VB6 RndCrypt (string version)
    static string RndCrypt(string input, string password)
    {
        if (string.IsNullOrEmpty(input)) return "";

        // Step 1: Randomize(Len(password))
        VBMath.Randomize(password.Length);

        // Step 2: Compute password hash
        int hash = 0;
        for (int i = 0; i < password.Length; i++)
        {
            int r = (int)(VBMath.Rnd() * 256);
            hash += (int)password[i] ^ r;
        }

        // Step 3: Randomize(hash)
        VBMath.Randomize(hash);

        // Step 4: XOR each character
        var sb = new StringBuilder(input.Length);
        for (int i = 0; i < input.Length; i++)
        {
            int r = (int)(VBMath.Rnd() * 256);
            sb.Append((char)(input[i] ^ r));
        }
        return sb.ToString();
    }

    // RndCryptB - byte array version
    static byte[] RndCryptB(byte[] input, string password)
    {
        if (input == null || input.Length == 0) return Array.Empty<byte>();

        // Convert password to bytes (ANSI/Latin-1)
        byte[] pwBytes = Encoding.ASCII.GetBytes(password);

        // Step 1: Randomize(Len(pwBytes))
        VBMath.Randomize(pwBytes.Length);

        // Step 2: Compute password hash from byte array
        int hash = 0;
        for (int i = 0; i < pwBytes.Length; i++)
        {
            int r = (int)(VBMath.Rnd() * 256);
            hash += pwBytes[i] ^ r;
        }

        // Step 3: Randomize(hash)
        VBMath.Randomize(hash);

        // Step 4: XOR each byte
        byte[] result = new byte[input.Length];
        for (int i = 0; i < input.Length; i++)
        {
            int r = (int)(VBMath.Rnd() * 256);
            result[i] = (byte)(input[i] ^ r);
        }
        return result;
    }

    static void Main(string[] args)
    {
        string s2dPath = Path.Combine(
            Path.GetDirectoryName(Path.GetDirectoryName(
                Path.GetDirectoryName(Directory.GetCurrentDirectory())))!,
            "..", "Frequencies.s2d");

        // Try multiple possible paths
        if (!File.Exists(s2dPath))
            s2dPath = "/Users/pavelhornak/repo/2/spooky_decompiled_new/Frequencies.s2d";

        if (!File.Exists(s2dPath))
        {
            Console.WriteLine($"File not found: {s2dPath}");
            return;
        }

        string[] lines = File.ReadLines(s2dPath).Take(10).ToArray();
        Console.WriteLine($"Read {lines.Length} lines from {s2dPath}");

        string[] passwords = {
            "2020888376Spooky2 (c) John White. http://www.cancerclinic.co.nz",
            "202088837620260304",
            "2020888376",
            "20260304",
        };

        foreach (string pw in passwords)
        {
            Console.WriteLine($"\n--- Password: \"{pw.Substring(0, Math.Min(pw.Length, 40))}...\" ---");

            for (int lineIdx = 0; lineIdx < Math.Min(3, lines.Length); lineIdx++)
            {
                string b64 = lines[lineIdx].Trim();
                byte[] decoded = Convert.FromBase64String(b64);

                // Test 1: Simple RndCryptB (base64 decode -> XOR decrypt)
                byte[] result1 = RndCryptB(decoded, pw);
                int printable1 = result1.Count(b => b >= 32 && b <= 126);
                double ratio1 = (double)printable1 / result1.Length;
                string text1 = Encoding.Latin1.GetString(result1);
                Console.WriteLine($"  RndCryptB line {lineIdx}: ratio={ratio1:P0} -> {text1.Substring(0, Math.Min(60, text1.Length))}");

                // Test 2: RndCrypt on the base64 string directly
                string result2 = RndCrypt(b64, pw);
                int printable2 = result2.Count(c => c >= 32 && c <= 126);
                double ratio2 = (double)printable2 / result2.Length;
                Console.WriteLine($"  RndCrypt  line {lineIdx}: ratio={ratio2:P0} -> {result2.Substring(0, Math.Min(60, result2.Length))}");

                // Test 3: RndCrypt on base64 decoded as string (Latin-1)
                string decodedStr = Encoding.Latin1.GetString(decoded);
                string result3 = RndCrypt(decodedStr, pw);
                int printable3 = result3.Count(c => c >= 32 && c <= 126);
                double ratio3 = (double)printable3 / result3.Length;
                Console.WriteLine($"  RndCrypt(decoded) line {lineIdx}: ratio={ratio3:P0} -> {result3.Substring(0, Math.Min(60, result3.Length))}");
            }
        }
    }
}
