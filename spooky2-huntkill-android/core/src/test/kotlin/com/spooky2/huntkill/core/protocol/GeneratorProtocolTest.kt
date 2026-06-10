package com.spooky2.huntkill.core.protocol

import com.spooky2.huntkill.core.model.CommandType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Golden tests for [GeneratorProtocol], ported from the C# reference
 * `Spooky2.Services.Tests.GeneratorProtocolTests`.
 *
 * Command strings and encoded frequency values are cross-checked against the
 * recorded serial dumps in `Data/StartHuntAndKill`, `Data/FullHuntAndKill`, and
 * `Data/FinishHuntAndKill`. Assertions are byte-for-byte exact — never weakened.
 */
class GeneratorProtocolTest {

    // ─────────────────────────────────────────────────────────────
    // Static command string format
    // ─────────────────────────────────────────────────────────────

    @Test
    fun staticCommands_matchExpectedValues() {
        assertEquals(":a00", GeneratorProtocol.ACTION_PING)
        assertEquals(":a0012345", GeneratorProtocol.ACTION_HANDSHAKE)
        assertEquals(":w610", GeneratorProtocol.STOP_OUTPUT1)
        assertEquals(":w611", GeneratorProtocol.START_OUTPUT1)
        assertEquals(":w620", GeneratorProtocol.STOP_OUTPUT2)
        assertEquals(":w621", GeneratorProtocol.START_OUTPUT2)
        assertEquals(":r68", GeneratorProtocol.READ_FIRMWARE_VERSION)
        assertEquals(":r80", GeneratorProtocol.READ_HARDWARE_TYPE)
        assertEquals(":r91", GeneratorProtocol.READ_SERIAL_NUMBER)
    }

    @Test
    fun deviceQueryCommands_matchExpectedValues() {
        assertEquals(":r02=0,", GeneratorProtocol.READ_HARDWARE_INFO)
        assertEquals(":r68", GeneratorProtocol.READ_FIRMWARE_VERSION)
        assertEquals(":r80", GeneratorProtocol.READ_HARDWARE_TYPE)
        assertEquals(":r91", GeneratorProtocol.READ_SERIAL_NUMBER)
    }

    @Test
    fun readAngle_format() {
        assertEquals(":r11=,", GeneratorProtocol.READ_ANGLE)
    }

    @Test
    fun readCurrent_format() {
        assertEquals(":r12=,", GeneratorProtocol.READ_CURRENT)
    }

    // ─────────────────────────────────────────────────────────────
    // FormatFrequency — the highest-risk function.
    // Dump-verified golden vectors from the GX Pro F8 decimal-position encoding.
    // ─────────────────────────────────────────────────────────────

    @Test
    fun formatFrequency_41010Hz_matchesFirstSweepStepInDump() {
        // DUMP-DERIVED (Data/FullHuntAndKill, first sweep step = 41000 * 1.00025):
        // 41010.25 Hz → F8 "41010.25000000" → strip → "41010.25" → "4101025" + posCode 6 → "41010256".
        // (The old C#-port expectation "41000000000001" for 41000 Hz was WRONG — it used
        //  posCode = intDigits-4 and a fixed 8-digit fraction, which shifted the value and
        //  drove the generator at ×10/×100. The dump wins.)
        val result = GeneratorProtocol.formatFrequency(41010.25)
        assertEquals("41010256", result)
    }

    @Test
    fun formatFrequency_41000Hz_dumpRule() {
        // DUMP-DERIVED: 41000.0 → F8 "41000.00000000" → strip all fraction → "41000" + posCode 8 → "410008".
        // (C# port discrepancy: previously expected "41000000000001".)
        val result = GeneratorProtocol.formatFrequency(41000.0)
        assertEquals("410008", result)
    }

    @Test
    fun formatFrequency_1796956Hz_matchesLastSweepStepInDump() {
        // DUMP-DERIVED (Data/FullHuntAndKill, last ascending sweep step / first kill freq):
        // 1796956.27039622 → "1796956270396220" (16 chars).
        val result = GeneratorProtocol.formatFrequency(1796956.27039622)
        assertEquals("1796956270396220", result)
        assertEquals(16, result.length)
    }

    @Test
    fun formatFrequency_subKHz_preservesLeadingZeros() {
        // DUMP-DERIVED rule: 0.5 Hz → F8 "0.50000000" → strip → "0.5" → "05" + posCode 7 → "057".
        // (C# port discrepancy: previously expected fixed-width "0500000000".)
        val result = GeneratorProtocol.formatFrequency(0.5)
        assertEquals("057", result)
        assertTrue(result.startsWith("0"))
    }

    @Test
    fun formatFrequency_100Hz_dumpRule() {
        // DUMP-DERIVED rule: 100 Hz → F8 "100.00000000" → strip all fraction → "100" + posCode 8 → "1008".
        // (C# port discrepancy: previously expected fixed-width "100000000000".)
        val result = GeneratorProtocol.formatFrequency(100.0)
        assertEquals("1008", result)
    }

    @Test
    fun formatFrequency_validFrequencies_haveNoSeparators() {
        val freqs = listOf(41000.0, 1800000.0, 100.0, 0.5, 76000.5)
        for (freq in freqs) {
            val v = GeneratorProtocol.formatFrequency(freq)
            assertFalse("Value should not contain decimal point", v.contains('.'))
            assertFalse("Value should not contain comma", v.contains(','))
            assertTrue("Value should not be empty", v.isNotEmpty())
        }
    }

    // ─────────────────────────────────────────────────────────────
    // Frequency command builders (:w24 / :w25)
    // ─────────────────────────────────────────────────────────────

    @Test
    fun buildSetFrequency1_encodesExact() {
        // DUMP-DERIVED: 1000.0 → "1000" + posCode 8 → "10008".
        assertEquals(":w24=10008,", GeneratorProtocol.buildSetFrequency1(1000.0))
    }

    @Test
    fun buildSetFrequency2_encodesExact() {
        // DUMP-DERIVED: 880.0 → "880" + posCode 8 → "8808".
        assertEquals(":w25=8808,", GeneratorProtocol.buildSetFrequency2(880.0))
    }

    @Test
    fun buildSetFrequency1_41010_matchesEncodedCommand() {
        // DUMP-DERIVED (first sweep step in Data/FullHuntAndKill).
        // (C# port discrepancy: previously expected ":w24=41000000000001,".)
        assertEquals(":w24=41010256,", GeneratorProtocol.buildSetFrequency1(41010.25))
    }

    @Test
    fun buildSetFrequencyRawHz_format() {
        assertEquals(":w24=41009,", GeneratorProtocol.buildSetFrequencyRawHz(41009))
    }

    // ─────────────────────────────────────────────────────────────
    // Amplitude commands (centivolt)
    // ─────────────────────────────────────────────────────────────

    @Test
    fun buildSetAmplitudeCv_format() {
        // From dump: :w28=2000, = 20.00V, :w29=1950, = 19.50V.
        assertEquals(":w28=2000,", GeneratorProtocol.buildSetAmplitudeCv1(2000))
        assertEquals(":w29=1950,", GeneratorProtocol.buildSetAmplitudeCv2(1950))
    }

    @Test
    fun buildSetAmplitude1_formatsCorrectly() {
        assertEquals(":w32=120,", GeneratorProtocol.buildSetAmplitude1(120))
    }

    @Test
    fun buildSetAmplitude2_formatsCorrectly() {
        assertEquals(":w33=120,", GeneratorProtocol.buildSetAmplitude2(120))
    }

    // ─────────────────────────────────────────────────────────────
    // Output / gating / modulation / sync / low-freq / inversion / clear
    // ─────────────────────────────────────────────────────────────

    @Test
    fun buildOutputOnOff_variants() {
        assertEquals(":w11=1,1,", GeneratorProtocol.buildOutputOnOff(true, true))
        assertEquals(":w11=0,0,", GeneratorProtocol.buildOutputOnOff(false, false))
        assertEquals(":w11=1,0,", GeneratorProtocol.buildOutputOnOff(true, false))
    }

    @Test
    fun buildGatingOnOff_formatsCorrectly() {
        assertEquals(":w12=1,0,", GeneratorProtocol.buildGatingOnOff(true, false))
    }

    @Test
    fun clearFrequency_matchesDump() {
        assertEquals(":w12=0,,", GeneratorProtocol.CLEAR_FREQUENCY1)
        assertEquals(":w12=,0,", GeneratorProtocol.CLEAR_FREQUENCY2)
    }

    @Test
    fun buildModulationOnOff_formatsCorrectly() {
        assertEquals(":w13=0,1,", GeneratorProtocol.buildModulationOnOff(false, true))
    }

    @Test
    fun buildSyncOnOff_formatsCorrectly() {
        assertEquals(":w14=1,", GeneratorProtocol.buildSyncOnOff(true))
        assertEquals(":w14=0,", GeneratorProtocol.buildSyncOnOff(false))
    }

    @Test
    fun buildLowFrequencyMode_formatsCorrectly() {
        assertEquals(":w15=1,1,", GeneratorProtocol.buildLowFrequencyMode(true, true))
    }

    @Test
    fun buildWaveformInversion_formatsCorrectly() {
        assertEquals(":w17=1,0,", GeneratorProtocol.buildWaveformInversion(true, false))
    }

    // ─────────────────────────────────────────────────────────────
    // Waveform / duty / display name / waveform table
    // ─────────────────────────────────────────────────────────────

    @Test
    fun buildSetWaveform1_formatsCorrectly() {
        assertEquals(":w20=3", GeneratorProtocol.buildSetWaveform1(3))
    }

    @Test
    fun buildSetWaveform2_formatsCorrectly() {
        assertEquals(":w21=5", GeneratorProtocol.buildSetWaveform2(5))
    }

    @Test
    fun buildSetDutyCycle_formatsCorrectly() {
        assertEquals(":w40=50", GeneratorProtocol.buildSetDutyCycle("50"))
    }

    @Test
    fun buildSetDisplayName_matchesDump() {
        assertEquals(
            ":n00=Port 3 - Running Biofeedback",
            GeneratorProtocol.buildSetDisplayName("Port 3 - Running Biofeedback"),
        )
        assertEquals(
            ":n00=Port 4 - GX Hunt and Kill (C)",
            GeneratorProtocol.buildSetDisplayName("Port 4 - GX Hunt and Kill (C)"),
        )
    }

    @Test
    fun buildWaveformTable_format() {
        // From dump: :a11=512,515,518,521,
        val cmd = GeneratorProtocol.buildWaveformTable(11, listOf(512, 515, 518, 521))
        assertEquals(":a11=512,515,518,521,", cmd)
    }

    // ─────────────────────────────────────────────────────────────
    // BuildCommand generic builder
    // ─────────────────────────────────────────────────────────────

    @Test
    fun buildCommand_action() {
        assertEquals(":a00", GeneratorProtocol.buildCommand(CommandType.Action, "00"))
    }

    @Test
    fun buildCommand_readWithoutValue() {
        assertEquals(":r68", GeneratorProtocol.buildCommand(CommandType.Read, "68"))
    }

    @Test
    fun buildCommand_writeWithValue() {
        assertEquals(":w24=76000,", GeneratorProtocol.buildCommand(CommandType.Write, "24", "76000,"))
    }

    // ─────────────────────────────────────────────────────────────
    // Byte encoding / decoding (HID framing) incl. CRLF
    // ─────────────────────────────────────────────────────────────

    @Test
    fun encodeCommandToBytes_appendsCrlf() {
        val bytes = GeneratorProtocol.encodeCommandToBytes(":a00")
        val str = String(bytes, Charsets.US_ASCII)
        assertTrue(str.endsWith("\r\n"))
    }

    @Test
    fun encodeCommandToBytes_isAscii() {
        val bytes = GeneratorProtocol.encodeCommandToBytes(":w24=76000500000000,")
        for (b in bytes) {
            assertTrue("Byte $b is not valid ASCII", b.toInt() and 0xFF < 128)
        }
    }

    @Test
    fun encodeCommandToBytes_correctLength() {
        val cmd = ":a00"
        val bytes = GeneratorProtocol.encodeCommandToBytes(cmd)
        // cmd length + 2 bytes for \r\n
        assertEquals(cmd.length + 2, bytes.size)
    }

    @Test
    fun encodeCommandToBytes_exactCrlfTerminated() {
        val bytes = GeneratorProtocol.encodeCommandToBytes(":w24=1000000000000,")
        assertEquals(":w24=1000000000000,\r\n", String(bytes, Charsets.US_ASCII))
    }

    @Test
    fun decodeResponseFromBytes_stripsNullBytes() {
        val data = byteArrayOf('o'.code.toByte(), 'k'.code.toByte(), 0, 0, 0, 0, 0)
        assertEquals("ok", GeneratorProtocol.decodeResponseFromBytes(data))
    }

    @Test
    fun decodeResponseFromBytes_emptyArray_returnsEmpty() {
        assertEquals("", GeneratorProtocol.decodeResponseFromBytes(ByteArray(0)))
        assertEquals("", GeneratorProtocol.decodeResponseFromBytes(null))
    }

    @Test
    fun decodeResponseFromBytes_trimsWhitespace() {
        val data = "  ok=value  ".toByteArray(Charsets.US_ASCII)
        assertEquals("ok=value", GeneratorProtocol.decodeResponseFromBytes(data))
    }

    @Test
    fun encodeAndDecode_roundTrip() {
        val commands = listOf(
            ":a00",
            ":r68",
            ":w24=76000500000000,",
            ":w11=1,0,",
            ":w95=12021",
        )
        for (command in commands) {
            val encoded = GeneratorProtocol.encodeCommandToBytes(command)
            assertEquals(command, GeneratorProtocol.decodeResponseFromBytes(encoded))
        }
    }

    // ─────────────────────────────────────────────────────────────
    // Response parsing
    // ─────────────────────────────────────────────────────────────

    @Test
    fun parseResponse_okWithoutValue() {
        val r = GeneratorProtocol.parseResponse("ok")
        assertTrue(r.isSuccess)
        assertEquals("ok", r.rawResponse)
        assertEquals("", r.value)
    }

    @Test
    fun parseResponse_okWithValue() {
        val r = GeneratorProtocol.parseResponse("ok=76000.5")
        assertTrue(r.isSuccess)
        assertEquals("76000.5", r.value)
    }

    @Test
    fun parseResponse_errorResponse() {
        assertFalse(GeneratorProtocol.parseResponse("err").isSuccess)
    }

    @Test
    fun parseResponse_emptyResponse() {
        assertFalse(GeneratorProtocol.parseResponse("").isSuccess)
    }

    @Test
    fun parseResponse_nullResponse() {
        assertFalse(GeneratorProtocol.parseResponse(null).isSuccess)
    }

    @Test
    fun parseResponse_valueWithTrailingComma() {
        val r = GeneratorProtocol.parseResponse("ok=120,")
        assertTrue(r.isSuccess)
        assertEquals("120", r.value)
    }

    @Test
    fun parseResponse_unknownPrefixWithoutData_treatedAsFailure() {
        assertFalse(GeneratorProtocol.parseResponse("v2.34").isSuccess)
    }

    @Test
    fun parseResponse_matchesDumpResponses() {
        // :ok → success, no value (auth :w92 success).
        GeneratorProtocol.parseResponse(":ok").let {
            assertTrue(it.isSuccess)
            assertEquals("", it.value)
        }
        // Sensor read responses with trailing period terminator.
        GeneratorProtocol.parseResponse(":r11=53001.").let {
            assertTrue(it.isSuccess)
            assertEquals("53001", it.value)
        }
        GeneratorProtocol.parseResponse(":r12=6557.").let {
            assertTrue(it.isSuccess)
            assertEquals("6557", it.value)
        }
        // Auth challenge response :r90=ECHO,RESPONSE.
        GeneratorProtocol.parseResponse(":r90=123456789,987654321.").let {
            assertTrue(it.isSuccess)
            assertEquals("123456789,987654321", it.value)
        }
    }

    // ─────────────────────────────────────────────────────────────
    // Sensor reading parse (:r11 / :r12)
    // ─────────────────────────────────────────────────────────────

    @Test
    fun parseSensorReading_matchesDumpResponses() {
        assertEquals(53001.0, GeneratorProtocol.parseSensorReading(":r11=53001."), 0.0)
        assertEquals(6557.0, GeneratorProtocol.parseSensorReading(":r12=6557."), 0.0)
        assertEquals(52998.0, GeneratorProtocol.parseSensorReading(":r11=52998."), 0.0)
        assertEquals(6559.0, GeneratorProtocol.parseSensorReading(":r12=6559."), 0.0)
    }
}
