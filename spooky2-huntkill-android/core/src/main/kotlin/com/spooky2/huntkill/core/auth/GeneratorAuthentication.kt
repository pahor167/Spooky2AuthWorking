package com.spooky2.huntkill.core.auth

/**
 * Implements the GeneratorX challenge-response authentication protocol.
 *
 * Verbatim port of the C# reference
 * `Spooky2.Services.Communication.GeneratorAuthentication`, whose algorithm was
 * extracted from Spooky.exe binary analysis of Proc_0_353 / Proc_0_354.
 *
 * Protocol flow:
 *   1. App sends:       `:r90=CHALLENGE,\r\n`     (challenge = permutation of 1-9)
 *   2. Device responds: `:r90=ECHO,DEVICE_RESPONSE.\r\n`
 *   3. App verifies ECHO matches [computeEcho] (challenge, device_response)
 *   4. App computes AUTH_TOKEN = [computeAuthToken] (challenge, device_response)
 *   5. App sends:       `:w92=AUTH_TOKEN.\r\n`
 *   6. Device responds: `:ok\r\n`
 *
 * Formula per digit: `(S[posB] * S[posC] + S[posA] * S[iterDigit]) % 9 + 1`.
 * Modulo is 9 (not 10!), confirmed from binary: `mov ecx, 9` before `idiv ecx`.
 * The `+ 1` ensures output digits are always 1-9.
 *
 * All index tables use 1-based indexing exactly as the C# source.
 */
object GeneratorAuthentication {

    private data class PosTriple(val posA: Int, val posB: Int, val posC: Int)

    // Proc_0_353 position indices (1-based), extracted from binary at VA 0x898BE0.
    // Formula: echo[i] = (C[posB]*C[posC] + C[posA]*C[resp_digit_value_at_i]) % 9 + 1
    private val echoIndices: List<PosTriple> = listOf(
        PosTriple(8, 6, 5), PosTriple(1, 5, 7), PosTriple(3, 4, 9),
        PosTriple(8, 5, 7), PosTriple(8, 9, 6), PosTriple(3, 1, 4),
        PosTriple(3, 3, 1), PosTriple(4, 9, 3), PosTriple(6, 7, 4),
    )

    // Proc_0_354 position indices (1-based), extracted from binary at VA 0x89A5F0.
    // Formula: token[i] = (R[posB]*R[posC] + R[posA]*R[chal_digit_value_at_i]) % 9 + 1
    private val tokenIndices: List<PosTriple> = listOf(
        PosTriple(4, 6, 8), PosTriple(6, 4, 1), PosTriple(8, 6, 5),
        PosTriple(3, 2, 9), PosTriple(7, 8, 4), PosTriple(3, 1, 7),
        PosTriple(9, 4, 3), PosTriple(1, 6, 2), PosTriple(3, 2, 8),
    )

    /**
     * Generates a random challenge string: a permutation of digits 1-9
     * (Fisher-Yates shuffle, mirroring the C# `GenerateChallenge`).
     */
    fun generateChallenge(): String {
        val digits = intArrayOf(1, 2, 3, 4, 5, 6, 7, 8, 9)
        for (i in digits.size - 1 downTo 1) {
            val j = (0..i).random()
            val tmp = digits[i]
            digits[i] = digits[j]
            digits[j] = tmp
        }
        return digits.joinToString(separator = "")
    }

    /**
     * Computes the 9-digit echo for verification (Proc_0_353).
     * Iterator reads from [deviceResponse]; the formula indexes into [challenge].
     */
    fun computeEcho(challenge: String, deviceResponse: String): String {
        // 1-based indexing: index 0 is an unused dummy element.
        val c = IntArray(10)
        for (i in 0 until 9) c[i + 1] = challenge[i] - '0'

        val result = CharArray(9)
        for (i in 0 until 9) {
            val (posA, posB, posC) = echoIndices[i]
            val respDigit = deviceResponse[i] - '0' // value of i-th response digit
            val d = (c[posB] * c[posC] + c[posA] * c[respDigit]) % 9 + 1
            result[i] = ('0' + d)
        }
        return String(result)
    }

    /**
     * Computes the 9-digit auth token (Proc_0_354).
     * Iterator reads from [challenge]; the formula indexes into [deviceResponse].
     */
    fun computeAuthToken(challenge: String, deviceResponse: String): String {
        // 1-based indexing: index 0 is an unused dummy element.
        val r = IntArray(10)
        for (i in 0 until 9) r[i + 1] = deviceResponse[i] - '0'

        val result = CharArray(9)
        for (i in 0 until 9) {
            val (posA, posB, posC) = tokenIndices[i]
            val chalDigit = challenge[i] - '0' // value of i-th challenge digit
            val d = (r[posB] * r[posC] + r[posA] * r[chalDigit]) % 9 + 1
            result[i] = ('0' + d)
        }
        return String(result)
    }
}
