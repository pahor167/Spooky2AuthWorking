using Spooky2.Core.Models;

namespace Spooky2.Services.Communication;

/// <summary>
/// Spooky2 generator text-based command protocol.
/// Commands are colon-prefixed strings sent over USB HID, terminated with CRLF.
/// Format: :Xnn=value,value where X is a (action), r (read), or w (write).
/// Serial port settings: 57600,n,8,1 (type 1) or 115200,n,8,1 (type 2/3).
/// Ported from VB6 Proc_0_270/Proc_0_271/Proc_0_272 in Main.frm.
/// </summary>
public static class GeneratorProtocol
{
    /// <summary>Command terminator (carriage return + line feed).</summary>
    public const string CommandTerminator = "\r\n";

    // ────────────────────────────────────────────────────────────────
    // Action commands (:a) - initialization and handshake
    // ────────────────────────────────────────────────────────────────

    /// <summary>Initialize/ping generator. Expects "err" response. (Main.frm:27514)</summary>
    public static readonly string ActionPing = ":a00"; // Raw command: ":a00"

    /// <summary>Authenticate/handshake with generator. Expects "err" response. (Main.frm:27537)</summary>
    public static readonly string ActionHandshake = ":a0012345"; // Raw command: ":a0012345"

    // ────────────────────────────────────────────────────────────────
    // Read commands (:r) - query device state and info
    // ────────────────────────────────────────────────────────────────

    /// <summary>Read device info/status. (Main.frm:43818)</summary>
    public static readonly string ReadDeviceInfo = ":r00"; // Raw command: ":r00"

    /// <summary>Read device info variant with parameter. (Main.frm:23534)</summary>
    public static readonly string ReadDeviceInfoVariant = ":r00=0,"; // Raw command: ":r00=0,"

    /// <summary>Read hardware info. (Main.frm:43833)</summary>
    public static readonly string ReadHardwareInfo = ":r02=0,"; // Raw command: ":r02=0,"

    /// <summary>Read output 1 frequency. (Main.frm:53900)</summary>
    public static readonly string ReadOutput1Frequency = ":r10"; // Raw command: ":r10"

    /// <summary>Read output 1 amplitude. (Main.frm:53908)</summary>
    public static readonly string ReadOutput1Amplitude = ":r11"; // Raw command: ":r11"

    /// <summary>Read output 2 amplitude. (Main.frm:53916)</summary>
    public static readonly string ReadOutput2Amplitude = ":r12"; // Raw command: ":r12"

    /// <summary>Read firmware version. Expects "ok" response. (Main.frm:27577)</summary>
    public static readonly string ReadFirmwareVersion = ":r68"; // Raw command: ":r68"

    /// <summary>Read hardware type. Expects "ok" response. (Main.frm:27558)</summary>
    public static readonly string ReadHardwareType = ":r80"; // Raw command: ":r80"

    /// <summary>Read hardware capability. Expects "ok" response. (Main.frm:43097)</summary>
    public static readonly string ReadHardwareCapability = ":r81"; // Raw command: ":r81"

    /// <summary>Read serial number. (Main.frm:27593)</summary>
    public static readonly string ReadSerialNumber = ":r91"; // Raw command: ":r91"

    /// <summary>Query firmware name. Used during GeneratorX authentication flow.</summary>
    public static readonly string QueryFirmwareName = ":n00=$"; // Raw command: ":n00=$"

    // ────────────────────────────────────────────────────────────────
    // Write commands (:w) - control outputs and set parameters
    // ────────────────────────────────────────────────────────────────

    // --- Output start/stop (w61/w62) ---

    /// <summary>Stop output 1. (Main.frm:37772)</summary>
    public static readonly string StopOutput1 = ":w610"; // Raw command: ":w610" (write register 61, value 0)

    /// <summary>Start output 1. (Main.frm:37615)</summary>
    public static readonly string StartOutput1 = ":w611"; // Raw command: ":w611" (write register 61, value 1)

    /// <summary>Stop output 2. (Main.frm:37794)</summary>
    public static readonly string StopOutput2 = ":w620"; // Raw command: ":w620" (write register 62, value 0)

    /// <summary>Start output 2. (Main.frm:37667)</summary>
    public static readonly string StartOutput2 = ":w621"; // Raw command: ":w621" (write register 62, value 1)

    // --- Amplitude on/off (w63/w64) ---

    /// <summary>Amplitude off channel 1. (Main.frm:57104)</summary>
    public static readonly string AmplitudeOffChannel1 = ":w630"; // Raw command: ":w630" (write register 63, value 0)

    /// <summary>Amplitude on channel 1. (Main.frm:43203, 57279)</summary>
    public static readonly string AmplitudeOnChannel1 = ":w631"; // Raw command: ":w631" (write register 63, value 1)

    /// <summary>Amplitude off channel 2. (Main.frm:57128)</summary>
    public static readonly string AmplitudeOffChannel2 = ":w640"; // Raw command: ":w640" (write register 64, value 0)

    /// <summary>Amplitude on channel 2. (Main.frm:43225, 57303)</summary>
    public static readonly string AmplitudeOnChannel2 = ":w641"; // Raw command: ":w641" (write register 64, value 1)

    // --- Bias/offset (w68) ---

    /// <summary>Bias/offset off. (Main.frm:43174, 56949)</summary>
    public static readonly string BiasOff = ":w680"; // Raw command: ":w680" (write register 68, value 0)

    /// <summary>Bias/offset on. (Main.frm:56914)</summary>
    public static readonly string BiasOn = ":w681"; // Raw command: ":w681" (write register 68, value 1)

    // --- Wobble control (w71/w72) ---

    /// <summary>Enable wobble on channel 1. (Main.frm:43305)</summary>
    public static readonly string WobbleOnChannel1 = ":w711"; // Raw command: ":w711" (write register 71, value 1) — originally Param71On

    /// <summary>Enable wobble on channel 2. (Main.frm:43327)</summary>
    public static readonly string WobbleOnChannel2 = ":w721"; // Raw command: ":w721" (write register 72, value 1) — originally Param72On

    // --- Gate off (w43) ---

    /// <summary>Gate off. (Main.frm:43349)</summary>
    public static readonly string GateOff = ":w430"; // Raw command: ":w430" (write register 43, value 0)

    // --- Spectrum mode (w42) ---

    /// <summary>Disable spectrum mode. (Main.frm:43075)</summary>
    public static readonly string SpectrumModeOff = ":w420"; // Raw command: ":w420" (write register 42, value 0) — originally Param42Off

    // --- Dwell time (w23) ---

    /// <summary>Set dwell time to 0. (Main.frm:43104)</summary>
    public static readonly string DwellTimeZero = ":w230,"; // Raw command: ":w230,"

    // --- Special firmware commands ---

    /// <summary>Enable firmware diagnostic mode. (Main.frm:53810)</summary>
    public static readonly string FirmwareDiagnosticEnable = ":w95=12021"; // Raw command: ":w95=12021" (write register 95) — originally FirmwareSpecial95

    /// <summary>Enable firmware calibration mode. (Main.frm:53818)</summary>
    public static readonly string FirmwareCalibrationEnable = ":w96=12321"; // Raw command: ":w96=12321" (write register 96) — originally FirmwareSpecial96

    // ────────────────────────────────────────────────────────────────
    // Command builders for parameterized commands
    // ────────────────────────────────────────────────────────────────

    /// <summary>
    /// Builds a complete command string from its components.
    /// </summary>
    /// <param name="type">Command type prefix (a, r, w).</param>
    /// <param name="register">Register number, e.g. "24".</param>
    /// <param name="value">Optional value payload.</param>
    /// <returns>The formatted command string without CRLF terminator.</returns>
    public static string BuildCommand(CommandType type, string register, string? value = null)
    {
        var prefix = type switch
        {
            CommandType.Action => "a",
            CommandType.Read => "r",
            CommandType.Write => "w",
            _ => throw new ArgumentOutOfRangeException(nameof(type))
        };

        return string.IsNullOrEmpty(value)
            ? $":{prefix}{register}"
            : $":{prefix}{register}={value}";
    }

    /// <summary>Build output on/off command. (Main.frm:37886, 37938)</summary>
    public static string BuildOutputOnOff(bool output1On, bool output2On) =>
        $":w11={(output1On ? 1 : 0)},{(output2On ? 1 : 0)},";

    /// <summary>Enable output 1 only. From dump: :w11=1,,</summary>
    public static readonly string EnableOutput1 = ":w11=1,,";

    /// <summary>Enable output 2 only. From dump: :w11=,1,</summary>
    public static readonly string EnableOutput2 = ":w11=,1,";

    /// <summary>Build gating on/off command. (Main.frm:53618)</summary>
    public static string BuildGatingOnOff(bool output1On, bool output2On) =>
        $":w12={(output1On ? 1 : 0)},{(output2On ? 1 : 0)},"; // Sends ":w12=<out1>,<out2>,"

    /// <summary>Build output 2 modulation on/off. (Main.frm:32183, 32207)</summary>
    public static string BuildModulationOnOff(bool output1On, bool output2On) =>
        $":w13={(output1On ? 1 : 0)},{(output2On ? 1 : 0)},"; // Sends ":w13=<out1>,<out2>,"

    /// <summary>Build output sync on/off. (Main.frm:56920, 56955)</summary>
    public static string BuildSyncOnOff(bool output1On) =>
        $":w14={(output1On ? 1 : 0)},"; // Sends ":w14=<out1>,"

    /// <summary>Build low frequency mode. (Main.frm:43562, 57479)</summary>
    public static string BuildLowFrequencyMode(bool output1On, bool output2On) =>
        $":w15={(output1On ? 1 : 0)},{(output2On ? 1 : 0)},"; // Sends ":w15=<out1>,<out2>,"

    /// <summary>Build waveform inversion. (Main.frm:43496)</summary>
    public static string BuildWaveformInversion(bool output1Invert, bool output2Invert) =>
        $":w17={(output1Invert ? 1 : 0)},{(output2Invert ? 1 : 0)},"; // Sends ":w17=<inv1>,<inv2>,"

    /// <summary>Build set output 1 waveform command. (Main.frm:53701)</summary>
    public static string BuildSetWaveform1(int waveformNumber) =>
        $":w20={waveformNumber}"; // Sends ":w20=<waveform>"

    /// <summary>Build set output 2 waveform command. (Main.frm:53711)</summary>
    public static string BuildSetWaveform2(int waveformNumber) =>
        $":w21={waveformNumber}"; // Sends ":w21=<waveform>"

    /// <summary>Build set dwell time command. (Main.frm:57195)</summary>
    public static string BuildSetDwellTime(string value) =>
        $":w23={value}"; // Sends ":w23=<dwell>" — originally BuildSetParam23

    /// <summary>Build set output 1 frequency. VB6 formats the frequency as a string,
    /// strips trailing zeros, then removes the decimal point.
    /// Example: 41020.50256251 → "41020.50256251" → "4102050256251"
    /// Verified from Data/LatestComparison/OldOldSpooky dump.</summary>
    public static string BuildSetFrequency1(double frequencyHz)
    {
        var s = frequencyHz.ToString("G15", System.Globalization.CultureInfo.InvariantCulture);
        s = s.Replace(".", "");
        return $":w24={s},";
    }

    /// <summary>Build set output 2 frequency (same format as output 1).</summary>
    public static string BuildSetFrequency2(double frequencyHz)
    {
        var s = frequencyHz.ToString("G15", System.Globalization.CultureInfo.InvariantCulture);
        s = s.Replace(".", "");
        return $":w25={s},";
    }

    /// <summary>Set frequency as raw integer Hz (used during setup/ramp-up, NOT scanning).
    /// From dump: :w24=41009, before amplitude ramp.</summary>
    public static string BuildSetFrequencyRawHz(int frequencyHz) =>
        $":w24={frequencyHz},";


    /// <summary>Set output 1 amplitude in centivolt. :w28=2000, = 20.00V</summary>
    public static string BuildSetAmplitudeCv1(int centivolt) =>
        $":w28={centivolt},"; // Sends ":w28=<centivolt>,"

    /// <summary>Set output 2 amplitude in centivolt. :w29=1950, = 19.50V</summary>
    public static string BuildSetAmplitudeCv2(int centivolt) =>
        $":w29={centivolt},"; // Sends ":w29=<centivolt>,"

    /// <summary>Build set output 1 amplitude. (Main.frm:43642)</summary>
    public static string BuildSetAmplitude1(int amplitude) =>
        $":w32={amplitude},"; // Sends ":w32=<amp>,"

    /// <summary>Build set output 2 amplitude. (Main.frm:43664)</summary>
    public static string BuildSetAmplitude2(int amplitude) =>
        $":w33={amplitude},"; // Sends ":w33=<amp>,"

    /// <summary>Build set duty cycle command. (Main.frm:53770)</summary>
    public static string BuildSetDutyCycle(string value) =>
        $":w40={value}"; // Sends ":w40=<duty>" — originally BuildSetParam40

    /// <summary>Build set waveform data block. Note: no '=' delimiter. (Main.frm:43132)</summary>
    public static string BuildSetWaveformData(string value) =>
        $":w45{value}"; // Sends ":w45<data>" — originally BuildSetParam45

    /// <summary>Build set frequency sweep start. (Main.frm:53778)</summary>
    public static string BuildSetSweepStart(string value) =>
        $":w50={value}"; // Sends ":w50=<freq>" — originally BuildSetParam50

    /// <summary>Build set frequency sweep end. (Main.frm:53786)</summary>
    public static string BuildSetSweepEnd(string value) =>
        $":w51={value}"; // Sends ":w51=<freq>" — originally BuildSetParam51

    // ────────────────────────────────────────────────────────────────
    // Biofeedback and display commands (from serial dump analysis)
    // ────────────────────────────────────────────────────────────────

    /// <summary>Set display name. :n00=text</summary>
    public static string BuildSetDisplayName(string name) => $":n00={name}";

    /// <summary>Read angle/impedance for biofeedback. Response: :r11=value.</summary>
    public static readonly string ReadAngle = ":r11=,";

    /// <summary>Read current for biofeedback. Response: :r12=value.</summary>
    public static readonly string ReadCurrent = ":r12=,";

    /// <summary>Clear frequency channel 1. :w12=0,,</summary>
    public static readonly string ClearFrequency1 = ":w12=0,,";

    /// <summary>Clear frequency channel 2. :w12=,0,</summary>
    public static readonly string ClearFrequency2 = ":w12=,0,";

    /// <summary>Build waveform table upload. :a11=val1,val2,... through :a24=...</summary>
    public static string BuildWaveformTable(int tableIndex, IEnumerable<int> values) =>
        $":a{tableIndex}={string.Join(",", values)},";

    // ────────────────────────────────────────────────────────────────
    // Response parsing
    // ────────────────────────────────────────────────────────────────

    /// <summary>
    /// Parses a response string from the generator.
    /// Responses are text strings; read responses return values after "=" delimiter.
    /// Known prefixes: "ok" (success), "err" (error/expected for some commands).
    /// </summary>
    public static CommandResponse ParseResponse(string response)
    {
        if (string.IsNullOrWhiteSpace(response))
        {
            return new CommandResponse(IsSuccess: false, RawResponse: response ?? string.Empty, Value: string.Empty);
        }

        var trimmed = response.Trim();

        // Strip leading colon if present — GeneratorX responses use ":ok" prefix
        if (trimmed.StartsWith(':'))
        {
            trimmed = trimmed[1..];
        }

        if (trimmed.StartsWith("ok", StringComparison.OrdinalIgnoreCase))
        {
            var value = ExtractResponseValue(trimmed);
            return new CommandResponse(IsSuccess: true, RawResponse: trimmed, Value: value);
        }

        if (trimmed.StartsWith("err", StringComparison.OrdinalIgnoreCase))
        {
            var value = ExtractResponseValue(trimmed);
            return new CommandResponse(IsSuccess: false, RawResponse: trimmed, Value: value);
        }

        // Some responses may not have a known prefix but still contain data
        var fallbackValue = ExtractResponseValue(trimmed);
        return new CommandResponse(IsSuccess: true, RawResponse: trimmed, Value: fallbackValue);
    }

    /// <summary>
    /// Encodes a command string into bytes ready for USB HID transmission.
    /// Appends CRLF terminator and converts to ASCII bytes.
    /// </summary>
    public static byte[] EncodeCommandToBytes(string command) // Originally EncodeCommand
    {
        return System.Text.Encoding.ASCII.GetBytes(command + CommandTerminator);
    }

    /// <summary>
    /// Decodes a response from raw USB HID bytes into a string.
    /// Strips null bytes and trims whitespace.
    /// </summary>
    public static string DecodeResponseFromBytes(byte[] data) // Originally DecodeResponse
    {
        if (data is null || data.Length == 0)
        {
            return string.Empty;
        }

        // HID reports may contain trailing null bytes; strip them
        int length = Array.IndexOf(data, (byte)0);
        if (length < 0) length = data.Length;

        return System.Text.Encoding.ASCII.GetString(data, 0, length).Trim();
    }

    /// <summary>
    /// Parses a sensor reading response from the generator.
    /// Response format: ":r11=53001." or ":r12=6557." where the trailing period is a terminator.
    /// </summary>
    public static double ParseSensorReading(string response)
    {
        var trimmed = response.Trim().TrimStart(':');
        var eqIdx = trimmed.IndexOf('=');
        if (eqIdx < 0) return 0;
        var valStr = trimmed[(eqIdx + 1)..].TrimEnd('.');
        return double.TryParse(valStr, System.Globalization.NumberStyles.Float,
            System.Globalization.CultureInfo.InvariantCulture, out var val) ? val : 0;
    }

    private static string ExtractResponseValue(string response)
    {
        var equalsIndex = response.IndexOf('=');
        if (equalsIndex >= 0 && equalsIndex < response.Length - 1)
        {
            return response[(equalsIndex + 1)..].TrimEnd(',').TrimEnd('.').Trim();
        }

        return string.Empty;
    }
}

/// <summary>
/// Represents a parsed response from a Spooky2 generator.
/// </summary>
/// <param name="IsSuccess">True if the response indicates success (prefix "ok" or data present).</param>
/// <param name="RawResponse">The raw response string as received.</param>
/// <param name="Value">The extracted value after the '=' delimiter, if present.</param>
public sealed record CommandResponse(bool IsSuccess, string RawResponse, string Value);
