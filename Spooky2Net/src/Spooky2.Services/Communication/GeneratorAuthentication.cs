namespace Spooky2.Services.Communication;

/// <summary>
/// Implements the GeneratorX challenge-response authentication protocol.
/// Algorithm extracted from Spooky.exe binary analysis of Proc_0_353/Proc_0_354.
///
/// Protocol flow:
///   1. App sends:      :r90=CHALLENGE,\r\n    (challenge = permutation of 1-9)
///   2. Device responds: :r90=ECHO,DEVICE_RESPONSE.\r\n
///   3. App verifies ECHO matches ComputeEcho(challenge, device_response)
///   4. App computes AUTH_TOKEN = ComputeAuthToken(challenge, device_response)
///   5. App sends:      :w92=AUTH_TOKEN.\r\n
///   6. Device responds: :ok\r\n
///
/// Formula per digit: (S[posB] * S[posC] + S[posA] * S[iterDigit]) % 9 + 1
/// Modulo is 9 (not 10!), confirmed from binary: mov ecx, 9 before idiv ecx.
/// The +1 ensures output digits are always 1-9.
/// </summary>
public static class GeneratorAuthentication
{
    // Proc_0_353 position indices (1-based), extracted from binary at VA 0x898BE0
    // Formula: echo[i] = (C[posB]*C[posC] + C[posA]*C[resp_digit_value_at_i]) % 9 + 1
    private static readonly (int PosA, int PosB, int PosC)[] EchoIndices =
    [
        (8, 6, 5), (1, 5, 7), (3, 4, 9), (8, 5, 7), (8, 9, 6),
        (3, 1, 4), (3, 3, 1), (4, 9, 3), (6, 7, 4),
    ];

    // Proc_0_354 position indices (1-based), extracted from binary at VA 0x89A5F0
    // Formula: token[i] = (R[posB]*R[posC] + R[posA]*R[chal_digit_value_at_i]) % 9 + 1
    private static readonly (int PosA, int PosB, int PosC)[] TokenIndices =
    [
        (4, 6, 8), (6, 4, 1), (8, 6, 5), (3, 2, 9), (7, 8, 4),
        (3, 1, 7), (9, 4, 3), (1, 6, 2), (3, 2, 8),
    ];

    /// <summary>
    /// Generates a random challenge string: a permutation of digits 1-9.
    /// </summary>
    public static string GenerateChallenge()
    {
        var digits = new[] { 1, 2, 3, 4, 5, 6, 7, 8, 9 };
        var rng = new Random();
        for (var i = digits.Length - 1; i > 0; i--)
        {
            var j = rng.Next(i + 1);
            (digits[i], digits[j]) = (digits[j], digits[i]);
        }
        return string.Concat(digits);
    }

    /// <summary>
    /// Computes the 9-digit echo for verification (Proc_0_353).
    /// Iterator reads from device_response, formula indexes into challenge.
    /// </summary>
    public static string ComputeEcho(string challenge, string deviceResponse)
    {
        // 1-based indexing: prepend a dummy 0 element
        var c = new int[10];
        for (var i = 0; i < 9; i++) c[i + 1] = challenge[i] - '0';

        var result = new char[9];
        for (var i = 0; i < 9; i++)
        {
            var (posA, posB, posC) = EchoIndices[i];
            var respDigit = deviceResponse[i] - '0'; // value of i-th response digit
            var d = (c[posB] * c[posC] + c[posA] * c[respDigit]) % 9 + 1;
            result[i] = (char)('0' + d);
        }
        return new string(result);
    }

    /// <summary>
    /// Computes the 9-digit auth token (Proc_0_354).
    /// Iterator reads from challenge, formula indexes into device_response.
    /// </summary>
    public static string ComputeAuthToken(string challenge, string deviceResponse)
    {
        // 1-based indexing: prepend a dummy 0 element
        var r = new int[10];
        for (var i = 0; i < 9; i++) r[i + 1] = deviceResponse[i] - '0';

        var result = new char[9];
        for (var i = 0; i < 9; i++)
        {
            var (posA, posB, posC) = TokenIndices[i];
            var chalDigit = challenge[i] - '0'; // value of i-th challenge digit
            var d = (r[posB] * r[posC] + r[posA] * r[chalDigit]) % 9 + 1;
            result[i] = (char)('0' + d);
        }
        return new string(result);
    }
}
