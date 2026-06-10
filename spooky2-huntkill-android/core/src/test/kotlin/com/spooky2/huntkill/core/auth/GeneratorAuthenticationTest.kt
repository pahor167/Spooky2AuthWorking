package com.spooky2.huntkill.core.auth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Golden tests for [GeneratorAuthentication] driven by recorded hardware handshake
 * dumps in `core/src/test/resources/dumps/Handshake1..10`.
 *
 * Mirrors `Spooky2.Services.Tests.AuthenticationTests`: each dump yields a
 * (challenge, deviceResponse, expectedEcho, expectedToken) vector, and the ported
 * Kotlin must reproduce the device's ECHO and the app's AUTH_TOKEN bit-for-bit.
 */
class GeneratorAuthenticationTest {

    /** One parsed handshake: the four 9-digit fields extracted from a dump. */
    private data class HandshakeVector(
        val name: String,
        val challenge: String,
        val deviceResponse: String,
        val expectedEcho: String,
        val expectedToken: String,
    )

    @Test
    fun computeEcho_matchesAllRecordedHandshakes() {
        val vectors = loadAllHandshakes()
        assertTrue("expected at least one handshake dump", vectors.isNotEmpty())
        for (v in vectors) {
            assertEquals(
                "echo mismatch for ${v.name}",
                v.expectedEcho,
                GeneratorAuthentication.computeEcho(v.challenge, v.deviceResponse),
            )
        }
    }

    @Test
    fun computeAuthToken_matchesAllRecordedHandshakes() {
        val vectors = loadAllHandshakes()
        assertTrue("expected at least one handshake dump", vectors.isNotEmpty())
        for (v in vectors) {
            assertEquals(
                "token mismatch for ${v.name}",
                v.expectedToken,
                GeneratorAuthentication.computeAuthToken(v.challenge, v.deviceResponse),
            )
        }
    }

    @Test
    fun generateChallenge_isPermutationOf1To9() {
        repeat(50) {
            val challenge = GeneratorAuthentication.generateChallenge()
            assertEquals(9, challenge.length)
            assertEquals("123456789", challenge.toCharArray().sorted().joinToString(""))
        }
    }

    @Test
    fun generateChallenge_producesDifferentValues() {
        val challenges = (0 until 20)
            .map { GeneratorAuthentication.generateChallenge() }
            .toSet()
        assertTrue(challenges.size >= 2)
    }

    // --- Dump parsing -------------------------------------------------------

    private fun loadAllHandshakes(): List<HandshakeVector> =
        (1..10).map { i -> parseDump("Handshake$i") }

    /**
     * Parses one serial-monitor dump. The dump interleaves "Written data" (TX) and
     * "Read data" (RX) blocks; each block lists raw bytes as hex pairs (the ASCII
     * column on the right is decorative and may split a payload across two lines, so
     * we decode the hex instead).
     *
     * Frames of interest (one challenge-response handshake per dump):
     *   TX `:r90=CHALLENGE,`             -> challenge
     *   RX `:r90=ECHO,DEVICE_RESPONSE.`  -> expectedEcho, deviceResponse
     *   TX `:w92=AUTH_TOKEN.`            -> expectedToken
     */
    private fun parseDump(name: String): HandshakeVector {
        val text = readResource("dumps/$name")
        val frames = decodeFrames(text)

        val r90Tx = frames.first { it.startsWith(":r90=") && it.endsWith(",") }
        val r90Rx = frames.first { it.startsWith(":r90=") && it.endsWith(".") && it.contains(",") }
        val w92Tx = frames.first { it.startsWith(":w92=") && it.endsWith(".") }

        // ":r90=271543986," -> "271543986"
        val challenge = r90Tx.removePrefix(":r90=").removeSuffix(",")
        // ":r90=726911191,941378256." -> echo, deviceResponse
        val rxBody = r90Rx.removePrefix(":r90=").removeSuffix(".")
        val (expectedEcho, deviceResponse) = rxBody.split(",", limit = 2)
        // ":w92=883542462." -> "883542462"
        val expectedToken = w92Tx.removePrefix(":w92=").removeSuffix(".")

        require(challenge.length == 9) { "$name: bad challenge '$challenge'" }
        require(deviceResponse.length == 9) { "$name: bad deviceResponse '$deviceResponse'" }
        require(expectedEcho.length == 9) { "$name: bad echo '$expectedEcho'" }
        require(expectedToken.length == 9) { "$name: bad token '$expectedToken'" }

        return HandshakeVector(name, challenge, deviceResponse, expectedEcho, expectedToken)
    }

    /**
     * Walks the dump line by line. Header lines ("... Written/Read data ...") start a
     * block; subsequent indented hex lines contribute bytes. Each completed block is
     * decoded to a string and split on the protocol terminator `\r\n` into frames.
     */
    private fun decodeFrames(text: String): List<String> {
        val frames = mutableListOf<String>()
        val current = StringBuilder()

        fun flush() {
            if (current.isEmpty()) return
            // Frames are CRLF-terminated; a block may contain multiple frames.
            current.toString()
                .split("\r\n")
                .map { it.trim('\r', '\n') }
                .filter { it.isNotEmpty() }
                .forEach { frames.add(it) }
            current.clear()
        }

        for (rawLine in text.lines()) {
            if (rawLine.isHeaderLine()) {
                flush()
                continue
            }
            val bytes = hexBytesOf(rawLine)
            if (bytes.isNotEmpty()) {
                current.append(String(bytes, Charsets.US_ASCII))
            }
        }
        flush()
        return frames
    }

    /** A block header looks like `[22/03/2026 15:33:17] Written data (COM3)`. */
    private fun String.isHeaderLine(): Boolean = trimStart().startsWith("[")

    /**
     * Extracts the leading run of 2-char hex byte tokens from a hex-dump line,
     * stopping at the ASCII rendering column (anything that is not a valid hex pair).
     */
    private fun hexBytesOf(line: String): ByteArray {
        val tokens = line.trim().split(Regex("\\s+"))
        val bytes = mutableListOf<Byte>()
        for (token in tokens) {
            if (token.length == 2 && token.all { it.isHexDigit() }) {
                bytes.add(token.toInt(16).toByte())
            } else {
                break // reached the ASCII column
            }
        }
        return bytes.toByteArray()
    }

    private fun Char.isHexDigit(): Boolean =
        this in '0'..'9' || this in 'a'..'f' || this in 'A'..'F'

    private fun readResource(path: String): String {
        val stream = javaClass.classLoader?.getResourceAsStream(path)
            ?: error("missing test resource: $path")
        return stream.bufferedReader(Charsets.US_ASCII).use { it.readText() }
    }
}
