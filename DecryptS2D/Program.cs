using System;
using System.IO;
using System.Runtime.InteropServices;
using System.Text;

/// <summary>
/// Tests whether x87 FPU precision in __vbaPowerR8 affects S2D decryption.
///
/// __vbaPowerR8: standard cdecl, doubles on stack, returns in ST(0) — works via P/Invoke.
/// __vbaFpI4:    reads ST(0) directly (no stack params) — CANNOT call via P/Invoke.
///               We use Math.Round instead (same banker's rounding as frndint).
///
/// BUILD:
///   C:\Windows\Microsoft.NET\Framework\v4.0.30319\csc.exe /unsafe /platform:x86 /out:DecryptS2D.exe Program.cs
/// </summary>
class Program
{
    // x87 FPU power function from MSVBVM60.DLL (ordinal 354)
    // Standard cdecl: two doubles on stack, returns double in ST(0)
    [DllImport("MSVBVM60.DLL", EntryPoint = "#354", CallingConvention = CallingConvention.Cdecl)]
    static extern double __vbaPowerR8(double baseVal, double exponent);

    // ===== VB6 RNG =====

    static uint rngSeed = 0x50000;

    static void VbRndNeg1()
    {
        uint bits = (uint)BitConverter.ToInt32(BitConverter.GetBytes(-1.0f), 0);
        rngSeed = (bits + (bits >> 24)) & 0xFFFFFF;
        rngSeed = (rngSeed * 0x43FD43FD + 0xC39EC3) & 0xFFFFFF;
    }

    static float VbRndNext()
    {
        rngSeed = (rngSeed * 0x43FD43FD + 0xC39EC3) & 0xFFFFFF;
        return rngSeed / 16777216.0f;
    }

    static void VbRandomize(double number)
    {
        byte[] dblBytes = BitConverter.GetBytes(number);
        uint upper = BitConverter.ToUInt32(dblBytes, 4);
        uint lValue = ((upper & 0xFFFF) << 8) ^ ((upper >> 8) & 0xFFFF00);
        rngSeed = (rngSeed & 0xFF) | (lValue & 0xFFFF00);
    }

    // ===== CLng / CByte (banker's rounding, same as x87 frndint) =====

    static int CLng(double val)
    {
        long r = (long)Math.Round(val, MidpointRounding.ToEven);
        if (r > int.MaxValue || r < int.MinValue) return 0;
        return (int)r;
    }

    static byte CByte(double val)
    {
        int r = (int)Math.Round(val, MidpointRounding.ToEven);
        if (r < 0) return 0;
        if (r > 255) return 255;
        return (byte)r;
    }

    // ===== Decrypt one line =====

    static string Decrypt(byte[] data, byte[] pw, bool useDllPow)
    {
        int pwLen = pw.Length;
        int dataLen = data.Length;
        int totalCount = pwLen + dataLen;

        // Seed pass
        VbRndNeg1();
        VbRandomize((double)totalCount);

        int acc = 0;
        for (int i = 1; i <= totalCount; i++)
        {
            float rnd1 = VbRndNext();
            int pwByte = pw[i % pwLen];
            double exponent = (double)rnd1 * 2.7526486955 + 1.0;

            int pr = 0;
            if (pwByte > 0)
            {
                try
                {
                    double powVal = useDllPow
                        ? __vbaPowerR8((double)pwByte, exponent)
                        : Math.Pow((double)pwByte, exponent);
                    pr = CLng(powVal);
                }
                catch { }
            }

            float rnd2 = VbRndNext();
            int r2v = CLng((double)rnd2 * 1000.0);
            acc = r2v | ((acc & 0x3FFFFFFF) + pr);
        }

        // Data pass
        VbRndNeg1();
        VbRandomize((double)acc);

        byte[] output = new byte[dataLen];
        for (int i = 0; i < dataLen; i++)
        {
            byte kb = (byte)(acc & 0xFF);
            float rnd1 = VbRndNext();
            byte rb = CByte((double)rnd1 * 255.49);
            output[i] = (byte)(rb ^ data[i] ^ kb);

            float rnd3 = VbRndNext();
            float rnd4 = VbRndNext();
            double bv = (double)rnd3 * 255.0;
            double ev = (double)rnd4 * 2.7526486955 + 1.0;
            int pv = 0;
            try
            {
                double powVal = useDllPow
                    ? __vbaPowerR8(bv, ev)
                    : Math.Pow(bv, ev);
                pv = CLng(powVal);
            }
            catch { }
            acc = (acc / 2) + pv;
        }

        return Encoding.GetEncoding(28591).GetString(output);
    }

    static bool IsReadable(string text)
    {
        foreach (char c in text)
            if (c < 9 || (c > 13 && c < 32) || c > 126) return false;
        return true;
    }

    static string Sanitize(string text, int maxLen)
    {
        if (text.Length > maxLen) text = text.Substring(0, maxLen);
        char[] arr = text.ToCharArray();
        for (int i = 0; i < arr.Length; i++)
            if (arr[i] < 32 || arr[i] > 126) arr[i] = '.';
        return new string(arr);
    }

    static void Main(string[] args)
    {
        Console.OutputEncoding = Encoding.GetEncoding(28591);

        string inputFile = args.Length > 0 ? args[0] : @"C:\Spooky2\Frequencies.s2d";
        int maxLines = args.Length > 1 ? int.Parse(args[1]) : 10;

        if (!File.Exists(inputFile))
        {
            Console.Error.WriteLine("File not found: " + inputFile);
            return;
        }

        string password = "2020888376Spooky2 (c) John White. http://www.cancerclinic.co.nz";
        byte[] pw = Encoding.Default.GetBytes(password);

        string[] allLines = File.ReadAllLines(inputFile);
        int n = maxLines > 0 ? Math.Min(maxLines, allLines.Length) : allLines.Length;

        // Test __vbaPowerR8
        bool dllOk = false;
        try
        {
            double r = __vbaPowerR8(2.0, 10.0);
            Console.Error.WriteLine("__vbaPowerR8(2,10) = " + r + " (expect 1024)");
            dllOk = (r == 1024.0);
        }
        catch (Exception ex)
        {
            Console.Error.WriteLine("DLL error: " + ex.Message);
            Console.Error.WriteLine("Make sure to compile with /platform:x86");
        }

        if (!dllOk)
        {
            Console.Error.WriteLine("MSVBVM60.DLL not available. Running C# Math.Pow only.");
            Console.Error.WriteLine();
        }

        // Compare seed pass pow results for first line
        if (dllOk)
        {
            string line0 = allLines[0].Trim();
            byte[] data0 = Convert.FromBase64String(line0);
            int totalCount = pw.Length + data0.Length;

            Console.Error.WriteLine();
            Console.Error.WriteLine("=== Comparing pow() for line 0 seed pass (totalCount=" + totalCount + ") ===");

            VbRndNeg1();
            VbRandomize((double)totalCount);
            uint savedSeed = rngSeed;

            int diffs = 0;
            for (int i = 1; i <= totalCount; i++)
            {
                float rnd1 = VbRndNext();
                int pwByte = pw[i % pw.Length];
                double exponent = (double)rnd1 * 2.7526486955 + 1.0;

                double csPow = Math.Pow((double)pwByte, exponent);
                double dllPow = __vbaPowerR8((double)pwByte, exponent);
                int csClng = CLng(csPow);
                int dllClng = CLng(dllPow);

                if (csClng != dllClng)
                {
                    diffs++;
                    Console.Error.WriteLine("  i=" + i + " base=" + pwByte + " exp=" + exponent.ToString("F6")
                        + " CS=" + csPow.ToString("F4") + "->" + csClng
                        + " DLL=" + dllPow.ToString("F4") + "->" + dllClng);
                }

                VbRndNext(); // consume rnd2
            }

            Console.Error.WriteLine("  Pow differences: " + diffs + " / " + totalCount);
            Console.Error.WriteLine();
        }

        // Decrypt lines
        Console.Error.WriteLine("=== MODE 1: C# Math.Pow ===");
        for (int li = 0; li < n; li++)
        {
            string line = allLines[li].Trim();
            if (line.Length == 0) continue;
            byte[] data;
            try { data = Convert.FromBase64String(line); } catch { continue; }
            string text = Decrypt(data, pw, false);
            Console.Error.WriteLine("  Line " + li + " " + (IsReadable(text) ? "OK" : "FAIL") + ": " + Sanitize(text, 80));
        }

        if (dllOk)
        {
            Console.Error.WriteLine();
            Console.Error.WriteLine("=== MODE 2: MSVBVM60.DLL __vbaPowerR8 ===");
            for (int li = 0; li < n; li++)
            {
                string line = allLines[li].Trim();
                if (line.Length == 0) continue;
                byte[] data;
                try { data = Convert.FromBase64String(line); } catch { continue; }
                string text = Decrypt(data, pw, true);
                Console.Error.WriteLine("  Line " + li + " " + (IsReadable(text) ? "OK" : "FAIL") + ": " + Sanitize(text, 80));
            }
        }

        Console.Error.WriteLine();
        Console.Error.WriteLine("Press Enter to exit...");
        Console.In.ReadLine();
    }
}
