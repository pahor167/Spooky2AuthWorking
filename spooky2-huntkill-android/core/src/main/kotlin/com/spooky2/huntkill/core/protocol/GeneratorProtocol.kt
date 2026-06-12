package com.spooky2.huntkill.core.protocol

import com.spooky2.huntkill.core.model.CommandType
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Represents a parsed response from a Spooky2 generator.
 *
 * Verbatim port of the C# reference `Spooky2.Services.Communication.CommandResponse`.
 *
 * @property isSuccess True if the response indicates success (prefix "ok" or data present).
 * @property rawResponse The raw response string as received.
 * @property value The extracted value after the '=' delimiter, if present.
 */
data class CommandResponse(
    val isSuccess: Boolean,
    val rawResponse: String,
    val value: String,
)

/**
 * Spooky2 generator text-based command protocol.
 *
 * Commands are colon-prefixed strings sent over USB HID, terminated with CRLF.
 * Format: `:Xnn=value,value` where X is a (action), r (read), or w (write).
 * Serial port settings: 57600,n,8,1 (type 1) or 115200,n,8,1 (type 2/3).
 *
 * Verbatim port of the C# reference
 * `Spooky2.Services.Communication.GeneratorProtocol`, itself ported from
 * VB6 Proc_0_270/Proc_0_271/Proc_0_272 in Main.frm.
 */
object GeneratorProtocol {

    /** Command terminator (carriage return + line feed). */
    const val COMMAND_TERMINATOR: String = "\r\n"

    // ────────────────────────────────────────────────────────────────
    // Action commands (:a) - initialization and handshake
    // ────────────────────────────────────────────────────────────────

    /** Initialize/ping generator. Expects "err" response. (Main.frm:27514) */
    const val ACTION_PING: String = ":a00"

    /** Authenticate/handshake with generator. Expects "err" response. (Main.frm:27537) */
    const val ACTION_HANDSHAKE: String = ":a0012345"

    // ────────────────────────────────────────────────────────────────
    // Read commands (:r) - query device state and info
    // ────────────────────────────────────────────────────────────────

    /** Read device info/status. (Main.frm:43818) */
    const val READ_DEVICE_INFO: String = ":r00"

    /** Read device info variant with parameter. (Main.frm:23534) */
    const val READ_DEVICE_INFO_VARIANT: String = ":r00=0,"

    /** Read hardware info. (Main.frm:43833) */
    const val READ_HARDWARE_INFO: String = ":r02=0,"

    /** Read output 1 frequency. (Main.frm:53900) */
    const val READ_OUTPUT1_FREQUENCY: String = ":r10"

    /** Read output 1 amplitude. (Main.frm:53908) */
    const val READ_OUTPUT1_AMPLITUDE: String = ":r11"

    /** Read output 2 amplitude. (Main.frm:53916) */
    const val READ_OUTPUT2_AMPLITUDE: String = ":r12"

    /** Read firmware version. Expects "ok" response. (Main.frm:27577) */
    const val READ_FIRMWARE_VERSION: String = ":r68"

    /** Read hardware type. Expects "ok" response. (Main.frm:27558) */
    const val READ_HARDWARE_TYPE: String = ":r80"

    /** Read hardware capability. Expects "ok" response. (Main.frm:43097) */
    const val READ_HARDWARE_CAPABILITY: String = ":r81"

    /** Read serial number. (Main.frm:27593) */
    const val READ_SERIAL_NUMBER: String = ":r91"

    /** Query firmware name. Used during GeneratorX authentication flow. */
    const val QUERY_FIRMWARE_NAME: String = ":n00=$"

    // ────────────────────────────────────────────────────────────────
    // Write commands (:w) - control outputs and set parameters
    // ────────────────────────────────────────────────────────────────

    // --- Output start/stop (w61/w62) ---

    /** Stop output 1. (Main.frm:37772) */
    const val STOP_OUTPUT1: String = ":w610"

    /** Start output 1. (Main.frm:37615) */
    const val START_OUTPUT1: String = ":w611"

    /** Stop output 2. (Main.frm:37794) */
    const val STOP_OUTPUT2: String = ":w620"

    /** Start output 2. (Main.frm:37667) */
    const val START_OUTPUT2: String = ":w621"

    // --- Amplitude on/off (w63/w64) ---

    /** Amplitude off channel 1. (Main.frm:57104) */
    const val AMPLITUDE_OFF_CHANNEL1: String = ":w630"

    /** Amplitude on channel 1. (Main.frm:43203, 57279) */
    const val AMPLITUDE_ON_CHANNEL1: String = ":w631"

    /** Amplitude off channel 2. (Main.frm:57128) */
    const val AMPLITUDE_OFF_CHANNEL2: String = ":w640"

    /** Amplitude on channel 2. (Main.frm:43225, 57303) */
    const val AMPLITUDE_ON_CHANNEL2: String = ":w641"

    // --- Bias/offset (w68) ---

    /** Bias/offset off. (Main.frm:43174, 56949) */
    const val BIAS_OFF: String = ":w680"

    /** Bias/offset on. (Main.frm:56914) */
    const val BIAS_ON: String = ":w681"

    // --- Wobble control (w71/w72) ---

    /** Enable wobble on channel 1. (Main.frm:43305) */
    const val WOBBLE_ON_CHANNEL1: String = ":w711"

    /** Enable wobble on channel 2. (Main.frm:43327) */
    const val WOBBLE_ON_CHANNEL2: String = ":w721"

    // --- Gate off (w43) ---

    /** Gate off. (Main.frm:43349) */
    const val GATE_OFF: String = ":w430"

    // --- Spectrum mode (w42) ---

    /** Disable spectrum mode. (Main.frm:43075) */
    const val SPECTRUM_MODE_OFF: String = ":w420"

    // --- Dwell time (w23) ---

    /** Set dwell time to 0. (Main.frm:43104) */
    const val DWELL_TIME_ZERO: String = ":w230,"

    // --- Special firmware commands ---

    /** Enable firmware diagnostic mode. (Main.frm:53810) */
    const val FIRMWARE_DIAGNOSTIC_ENABLE: String = ":w95=12021"

    /** Enable firmware calibration mode. (Main.frm:53818) */
    const val FIRMWARE_CALIBRATION_ENABLE: String = ":w96=12321"

    // --- Output enable shortcuts (from dump) ---

    /** Enable output 1 only. From dump: `:w11=1,,` */
    const val ENABLE_OUTPUT1: String = ":w11=1,,"

    /** Enable output 2 only. From dump: `:w11=,1,` */
    const val ENABLE_OUTPUT2: String = ":w11=,1,"

    // --- Biofeedback / display reads & clears (from serial dump analysis) ---

    /** Read angle/impedance for biofeedback. Response: `:r11=value.` */
    const val READ_ANGLE: String = ":r11=,"

    /** Read current for biofeedback. Response: `:r12=value.` */
    const val READ_CURRENT: String = ":r12=,"

    /** Clear frequency channel 1. `:w12=0,,` */
    const val CLEAR_FREQUENCY1: String = ":w12=0,,"

    /** Clear frequency channel 2. `:w12=,0,` */
    const val CLEAR_FREQUENCY2: String = ":w12=,0,"

    /**
     * Write frequency ZERO through the frequency register — the original's pause
     * silences the signal with this (`Data/StartPauseAndStop.txt`: `:w24=00,`),
     * NOT with the [CLEAR_FREQUENCY1]/[CLEAR_FREQUENCY2] channel clears. Byte-exact
     * copy of the dump (double zero included).
     */
    const val FREQUENCY1_ZERO: String = ":w24=00,"

    // ────────────────────────────────────────────────────────────────
    // Command builders for parameterized commands
    // ────────────────────────────────────────────────────────────────

    /**
     * Builds a complete command string from its components.
     *
     * @param type Command type prefix (a, r, w).
     * @param register Register number, e.g. "24".
     * @param value Optional value payload.
     * @return The formatted command string without CRLF terminator.
     */
    fun buildCommand(type: CommandType, register: String, value: String? = null): String {
        val prefix = when (type) {
            CommandType.Action -> "a"
            CommandType.Read -> "r"
            CommandType.Write -> "w"
        }

        return if (value.isNullOrEmpty()) {
            ":$prefix$register"
        } else {
            ":$prefix$register=$value"
        }
    }

    /** Build output on/off command. (Main.frm:37886, 37938) */
    fun buildOutputOnOff(output1On: Boolean, output2On: Boolean): String =
        ":w11=${if (output1On) 1 else 0},${if (output2On) 1 else 0},"

    /** Build gating on/off command. (Main.frm:53618) */
    fun buildGatingOnOff(output1On: Boolean, output2On: Boolean): String =
        ":w12=${if (output1On) 1 else 0},${if (output2On) 1 else 0},"

    /** Build output 2 modulation on/off. (Main.frm:32183, 32207) */
    fun buildModulationOnOff(output1On: Boolean, output2On: Boolean): String =
        ":w13=${if (output1On) 1 else 0},${if (output2On) 1 else 0},"

    /** Build output sync on/off. (Main.frm:56920, 56955) */
    fun buildSyncOnOff(output1On: Boolean): String =
        ":w14=${if (output1On) 1 else 0},"

    /** Build low frequency mode. (Main.frm:43562, 57479) */
    fun buildLowFrequencyMode(output1On: Boolean, output2On: Boolean): String =
        ":w15=${if (output1On) 1 else 0},${if (output2On) 1 else 0},"

    /** Build waveform inversion. (Main.frm:43496) */
    fun buildWaveformInversion(output1Invert: Boolean, output2Invert: Boolean): String =
        ":w17=${if (output1Invert) 1 else 0},${if (output2Invert) 1 else 0},"

    /** Build set output 1 waveform command. (Main.frm:53701) */
    fun buildSetWaveform1(waveformNumber: Int): String = ":w20=$waveformNumber"

    /** Build set output 2 waveform command. (Main.frm:53711) */
    fun buildSetWaveform2(waveformNumber: Int): String = ":w21=$waveformNumber"

    /** Build set dwell time command. (Main.frm:57195) */
    fun buildSetDwellTime(value: String): String = ":w23=$value"

    /**
     * Build set output 1 frequency for GeneratorX Pro.
     *
     * See [formatFrequency] for the exact encoding rule.
     * Example: 41010.25 Hz → "41010256" → `:w24=41010256,`
     * (matches the very first sweep step in the original `Data/FullHuntAndKill` dump).
     */
    fun buildSetFrequency1(frequencyHz: Double): String = ":w24=${formatFrequency(frequencyHz)},"

    /** Build set output 2 frequency (same format as output 1). */
    fun buildSetFrequency2(frequencyHz: Double): String = ":w25=${formatFrequency(frequencyHz)},"

    /**
     * Formats a frequency for the GeneratorX Pro `:w24`/`:w25` registers.
     *
     * Encoding rule (DERIVED from the ORIGINAL Spooky2 serial dumps and proven
     * byte-for-byte against 15132/15132 `:w24=` lines in `Data/FullHuntAndKill`
     * — a perfect decode→encode round trip):
     *
     *  1. Round the frequency to **8 decimal places** (banker's rounding,
     *     matching C# `double.ToString("F8")` / `Math.Round`).
     *  2. Remove the decimal point.
     *  3. **Strip the trailing zeros** that came from the fractional part.
     *  4. Append a single **position-code digit** = `8 - (fractional digits kept)`.
     *     The firmware uses this code to re-insert the decimal point: the last
     *     `8 - posCode` digits of the mantissa are the fractional part.
     *
     * Worked examples from the dump:
     *  - 41010.25       → F8 "41010.25000000" → strip → "41010.25"       → mantissa "4101025"      + posCode 6 → "41010256"
     *  - 41020.5025625  → F8 "41020.50256250" → strip → "41020.5025625"  → mantissa "410205025625" + posCode 1 → "4102050256251"
     *  - 41030.75768814 → F8 "41030.75768814" (no trailing zero)         → mantissa "4103075768814"+ posCode 0 → "41030757688140"
     *  - 1796956.27039622 (last sweep step / first kill freq)            →                              "1796956270396220"
     *
     * NOTE — discrepancy with the C# port: the C# reference encoded a fixed
     * 8-fractional-digit field plus `posCode = integerDigits - 4`. That rule is
     * WRONG (it produced e.g. 41000 → "41000000000001" and shifted real sweep
     * values by a digit, making the generator run at ×10/×100). The dump is the
     * ground truth and wins; see `GeneratorProtocolTest` for the corrected
     * golden vectors and `DumpFrequencyEncodingTest` for the full replay.
     */
    fun formatFrequency(frequencyHz: Double): String {
        // 1. Round to 8 decimal places. Use Double.toString so the rounding
        //    operates on the shortest round-trippable decimal (as C# does for
        //    double.ToString("F8")), not on the exact binary expansion.
        val rounded = BigDecimal(frequencyHz.toString())
            .setScale(8, RoundingMode.HALF_EVEN)
            .toPlainString()

        val dotIdx = rounded.indexOf('.')
        val intPart = if (dotIdx >= 0) rounded.substring(0, dotIdx) else rounded
        val fracPart = if (dotIdx >= 0) rounded.substring(dotIdx + 1) else ""

        // 2-3. Drop the dot and strip trailing zeros from the fractional digits.
        //      Leading zeros of the integer part are preserved (they encode
        //      magnitude for sub-kHz frequencies).
        val fracStripped = fracPart.trimEnd('0')

        // 4. Append the position code.
        val posCode = 8 - fracStripped.length
        return intPart + fracStripped + posCode.toString()
    }

    /**
     * Set frequency as raw integer Hz (used during setup/ramp-up, NOT scanning).
     * From dump: `:w24=41009,` before amplitude ramp.
     */
    fun buildSetFrequencyRawHz(frequencyHz: Int): String = ":w24=$frequencyHz,"

    /** Set output 1 amplitude in centivolt. `:w28=2000,` = 20.00V */
    fun buildSetAmplitudeCv1(centivolt: Int): String = ":w28=$centivolt,"

    /** Set output 2 amplitude in centivolt. `:w29=1950,` = 19.50V */
    fun buildSetAmplitudeCv2(centivolt: Int): String = ":w29=$centivolt,"

    /** Build set output 1 amplitude. (Main.frm:43642) */
    fun buildSetAmplitude1(amplitude: Int): String = ":w32=$amplitude,"

    /** Build set output 2 amplitude. (Main.frm:43664) */
    fun buildSetAmplitude2(amplitude: Int): String = ":w33=$amplitude,"

    /** Build set duty cycle command. (Main.frm:53770) */
    fun buildSetDutyCycle(value: String): String = ":w40=$value"

    /** Build set waveform data block. Note: no '=' delimiter. (Main.frm:43132) */
    fun buildSetWaveformData(value: String): String = ":w45$value"

    /** Build set frequency sweep start. (Main.frm:53778) */
    fun buildSetSweepStart(value: String): String = ":w50=$value"

    /** Build set frequency sweep end. (Main.frm:53786) */
    fun buildSetSweepEnd(value: String): String = ":w51=$value"

    /** Set display name. `:n00=text` */
    fun buildSetDisplayName(name: String): String = ":n00=$name"

    /** Build waveform table upload. `:a11=val1,val2,...` through `:a24=...` */
    fun buildWaveformTable(tableIndex: Int, values: Iterable<Int>): String =
        ":a$tableIndex=${values.joinToString(",")},"

    // ────────────────────────────────────────────────────────────────
    // Response parsing
    // ────────────────────────────────────────────────────────────────

    /**
     * Parses a response string from the generator.
     * Responses are text strings; read responses return values after "=" delimiter.
     * Known prefixes: "ok" (success), "err" (error/expected for some commands).
     */
    fun parseResponse(response: String?): CommandResponse {
        if (response.isNullOrBlank()) {
            return CommandResponse(isSuccess = false, rawResponse = response ?: "", value = "")
        }

        var trimmed = response.trim()

        // Strip leading colon if present — GeneratorX responses use ":ok" prefix.
        if (trimmed.startsWith(':')) {
            trimmed = trimmed.substring(1)
        }

        if (trimmed.startsWith("ok", ignoreCase = true)) {
            val value = extractResponseValue(trimmed)
            return CommandResponse(isSuccess = true, rawResponse = trimmed, value = value)
        }

        if (trimmed.startsWith("err", ignoreCase = true)) {
            val value = extractResponseValue(trimmed)
            return CommandResponse(isSuccess = false, rawResponse = trimmed, value = value)
        }

        // Responses with extractable data (e.g. read responses "r11=53001.") are treated as success;
        // truly unknown responses with no data are treated as failure.
        val fallbackValue = extractResponseValue(trimmed)
        val hasData = fallbackValue.isNotEmpty()
        return CommandResponse(isSuccess = hasData, rawResponse = trimmed, value = fallbackValue)
    }

    /**
     * Encodes a command string into bytes ready for USB HID transmission.
     * Appends CRLF terminator and converts to ASCII bytes.
     */
    fun encodeCommandToBytes(command: String): ByteArray =
        (command + COMMAND_TERMINATOR).toByteArray(Charsets.US_ASCII)

    /**
     * Decodes a response from raw USB HID bytes into a string.
     * Strips null bytes and trims whitespace.
     */
    fun decodeResponseFromBytes(data: ByteArray?): String {
        if (data == null || data.isEmpty()) {
            return ""
        }

        // HID reports may contain trailing null bytes; strip them.
        var length = data.indexOf(0.toByte())
        if (length < 0) length = data.size

        return String(data, 0, length, Charsets.US_ASCII).trim()
    }

    /**
     * Parses a sensor reading response from the generator.
     * Response format: `:r11=53001.` or `:r12=6557.` where the trailing period is a terminator.
     */
    fun parseSensorReading(response: String): Double {
        val trimmed = response.trim().trimStart(':')
        val eqIdx = trimmed.indexOf('=')
        if (eqIdx < 0) return 0.0
        val valStr = trimmed.substring(eqIdx + 1).trimEnd('.')
        return valStr.toDoubleOrNull() ?: 0.0
    }

    /**
     * True when [response] is a well-formed sensor reading that parses to a
     * number. Used by the scan engine to distinguish a real reading from a
     * timeout/garbage response (which [parseSensorReading] coerces to 0.0) so
     * dropout detection can flag the failed step instead of trusting the zero.
     */
    fun isParseableSensorReading(response: String): Boolean {
        val trimmed = response.trim().trimStart(':')
        val eqIdx = trimmed.indexOf('=')
        if (eqIdx < 0) return false
        val valStr = trimmed.substring(eqIdx + 1).trimEnd('.')
        return valStr.toDoubleOrNull() != null
    }

    private fun extractResponseValue(response: String): String {
        val equalsIndex = response.indexOf('=')
        if (equalsIndex >= 0 && equalsIndex < response.length - 1) {
            return response.substring(equalsIndex + 1).trimEnd(',').trimEnd('.').trim()
        }

        return ""
    }
}
