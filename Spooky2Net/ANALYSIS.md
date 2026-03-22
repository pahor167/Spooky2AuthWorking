# Spooky2 Decompiled VB6 Analysis

Analysis of key sections from the decompiled Spooky2 application for C# porting.

---

## 1. API Declarations (Lines 5248-5322)

### Purpose
Win32 P/Invoke declarations for HID USB device communication, process management, and system queries.

### API Categories

**Process Management:**
- `GetCurrentProcessId`, `OpenProcess`, `GetPriorityClass`, `SetPriorityClass` -- Used to adjust process priority at runtime
- `GetCurrentProcess`, `IsWow64Process` -- 64-bit detection

**HID (Human Interface Device) USB:**
- `HidD_GetHidGuid`, `HidD_GetAttributes`, `HidD_GetPreparsedData`, `HidD_FreePreparsedData`
- `HidP_GetCaps`, `HidP_GetValueCaps`
- All from `hid.dll` -- Core USB HID enumeration and communication

**Device Setup:**
- `SetupDiGetClassDevsA`, `SetupDiEnumDeviceInterfaces`, `SetupDiGetDeviceInterfaceDetailA`
- `SetupDiDestroyDeviceInfoList`, `SetupDiCreateDeviceInfoList`
- All from `setupapi.dll` -- Windows device enumeration

**File I/O (Overlapped):**
- `CreateFile`, `ReadFile`, `WriteFile`, `CloseHandle`, `CancelIo`
- `CreateEvent`, `ResetEvent`, `WaitForSingleObject`
- Used for async HID read/write with `OVERLAPPED` structures

**UI/System:**
- `GetCursorPos` (user32) -- Mouse position tracking
- `SendMessage` (user32) -- UI message dispatch
- `ShellExecute` (shell32) -- Launching external processes/URLs
- `GetLocaleInfo` (kernel32) -- Locale detection (Chinese locale check found later)
- `RtlGetVersion` (NTDLL) -- OS version detection
- `GetModuleHandle`, `GetProcAddress` -- Dynamic function loading
- `CopyMemory` (RtlMoveMemory) -- Raw memory operations
- `FormatMessage` -- Win32 error message formatting
- `lstrlen`, `lstrcpy` -- String operations

### C# Porting Notes
- HID communication: Use `HidSharp` or `HidApi.Net` NuGet package instead of raw P/Invoke
- Device enumeration: `SetupApi` calls can be replaced by HidSharp's built-in enumeration
- Overlapped I/O: Replace with async `Stream.ReadAsync`/`WriteAsync`
- Process priority: `System.Diagnostics.Process.GetCurrentProcess().PriorityClass`

### Key Structures Referenced (from context)
- `OVERLAPPED` -- for async file I/O
- `SECURITY_ATTRIBUTES` -- for CreateFile/CreateEvent
- `POINTAPI` -- cursor position

---

## 2. Form_Load -- Initialization Logic (Lines 15351-17470)

### Purpose
Massive initialization routine (~2100 lines) that sets up the entire application on startup.

### Directory Structure Created
The app creates a directory tree under a root path (defaults to `c:\Spooky2` if current path is <= 3 chars):

```
{RootPath}\
  Data\                  -- General data storage
  ScanData\              -- Biofeedback scan results
  Waveforms\             -- Custom waveform files
  Audio\                 -- Audio files
  SystemSounds\          -- System notification sounds
  Preset Collections\    -- Treatment preset files
  Preset Collections\User\          -- User-created presets
  Preset Collections\User\Biofeedback\  -- User BFB presets
  Skins\                 -- UI skin/theme files
  Spooky\Preset Collections\  -- Built-in presets (when VTable_00001B1E flag set)
```

### Recoverable Constants and File Paths

**Error Log:**
- `{DataDir}\Error.log` -- Error log file, auto-deleted when > 10MB (`var_989680` = 10,000,000 bytes)

**Frequency Database Files:**
```
{RootPath}\Frequencies.csv
{RootPath}\DNA_Frequencies.csv
{RootPath}\DNA_Frequencies_NH.csv
{RootPath}\MW_Frequencies.csv
{RootPath}\Frequencies.s2d
{RootPath}\DNA_Frequencies.s2d
{RootPath}\DNA_Frequencies_NH.s2d
{RootPath}\MW_Frequencies.s2d
{RootPath}\Custom.csv
{RootPath}\BFB_Frequencies.csv
```

**Configuration Files:**
```
{DataDir}\SystemFactoryDefault.txt
{DataDir}\ControlFactoryDefault.txt
{DataDir}\BFBFactoryDefault.txt
{DataDir}\SystemCFG.txt
{DataDir}\SettingsStartupDefaultCFG.txt
{DataDir}\BFBCFG.txt
{DataDir}\Skin.txt                    -- Stores selected UI skin
```

**Documentation Files:**
```
{RootPath}\GX_Pro_Firmware_Update.pdf
{RootPath}\Spooky2_Users_Guide_20250124.pdf
{RootPath}\Spooky2_Quick_Start_Guide.pdf
{RootPath}\Spooky2_Changes.pdf
```

**Executables:**
```
ChainEditor.exe
{RootPath}\GX_Pro_Firmware_v7789.exe
```

**USB Debug Logs (when USB debug mode enabled):**
```
{DataDir}\USB_Log_{timestamp}.txt
{DataDir}\USB_FaultLog_{timestamp}.txt
{DataDir}\USB_SentLog_{timestamp}.txt
```

**Sound Files Referenced:**
```
SpookyAlreadyRunning.mp3
WelcomeToSpooky.mp3
InstallDrivers.mp3
Yes.mp3
No.mp3
Cancel.mp3
```

**Other Files:**
```
{DataDir}\Skin.txt          -- 3-line file: line 1-3 unknown, line 4 = skin index
{RootPath}\Skins\Vista.cjstyles  -- Default skin file
New.txt                    -- First-run marker (deleted after first launch)
```

### Encryption Key Table
A large table of 100 10-digit numeric strings is initialized as a lookup/permutation table during Form_Load. These are used by the frequency encryption system. First entries:

```
Index 0:  "5394802167"
Index 1:  "8513607942"
Index 2:  "7104923586"
Index 3:  "6724189035"
Index 4:  "0157394286"
Index 5:  "9167052843"
Index 6:  "3874052619"
Index 7:  "9026714385"
Index 8:  "2589041367"
Index 9:  "3608217549"
Index 10: "6254078931"
Index 11: "1468297350"
... (continues for ~100 entries total)
```

These are digit substitution tables -- each is a permutation of "0123456789". Used by `EncryptFreq`/`DecryptFreq` to obfuscate frequency values in data files.

### Array Dimensions Initialized
Key arrays are pre-allocated at startup:
- Array of 181 elements (0 to 180) -- likely generator/channel state
- Array of 128 elements (0 to 127) -- likely per-channel configuration
- Array of 101 elements (0 to 100) -- likely waveform or preset data
- Array of 1025 elements (0 to 1024) -- likely frequency buffer
- Multiple arrays of 11 elements (0 to 10) -- likely per-generator data

### Startup Sequence
1. Determine application root directory (default `c:\Spooky2`)
2. Create directory structure
3. Check/truncate error log (max 10MB)
4. Initialize arrays and encryption tables
5. Check for previous instance (`App.PrevInstance`) -- show MsgBox "Spooky2 is already running." and exit
6. Set up UI skin system (load Skins directory, read Skin.txt config)
7. Populate combo boxes for sound effects: "Silent", "Beep", "Audrey", "Crystal", "Kate", "Mike", "Paul"
8. Load frequency database files (.csv and .s2d formats)
9. Detect system locale (check for "CHIN" in locale -- Chinese locale handling)
10. Set up USB debug logging if enabled
11. Load factory defaults and saved configuration
12. Check for `New.txt` (first-run experience: play welcome sound, show changelog)
13. Detect generators and prompt for driver installation if none found
14. Initialize various subsystems (Proc_0_160 through Proc_0_167, Proc_0_183, etc.)
15. Set form caption: `"Spooky2 (c) John White {version}"`
16. Version date constant: `"20260304"`

### Key String Literals
- `"Spooky2 (c) John White. http://www.cancerclinic.co.nz"` -- Copyright
- `"2020888376"` -- Used with EncryptDecrypt function (likely a key/seed)
- `"Spooky2 is already running."` -- Duplicate instance check
- `"No generators were detected. Exit and install drivers?"` -- Driver install prompt
- `"You are currently receiving remote frequency therapy from the revolutionary Spooky2 system..."` -- About text
- `"Join thousands of others..."` with download URL: `http://spooky2.com/downloadsPage/index.html`
- `"For full details, visit https://www.spooky2.com"` -- Website URL
- `"Error in Skin Error"` -- Skin loading error message
- `"Spooky"` -- Application name constant
- `"CHIN"` -- Chinese locale detection string

---

## 3. Timer1_Timer -- Main Polling Loop (Lines 8334-8750)

### Purpose
Main application timer that runs the frequency generation polling loop. Iterates over generator channels and manages the signal output cycle.

### What It Does
1. **Entry guard**: Uses a reentrance flag (`eax+0000055Ch`) to prevent overlapping timer calls
2. **Timeout detection**: Checks elapsed time against a threshold (60,000ms = 60 seconds at line 8372) and calls `Proc_0_225_756950` if exceeded -- likely a watchdog/reconnection routine
3. **Channel iteration**: Loops through an array (up to 128 elements) indexed by `var_28`, processing each active channel
4. **Per-channel processing**:
   - Reads channel type/mode (checks for value `1` and `2`)
   - For type 2 channels: reads Check21 checkbox state, calculates frequency values via `Proc_0_357` and `Proc_0_356`, passes to `Proc_0_223` with parameter `0x27` (39 decimal)
   - Compares current values against expected ranges
   - Calls `Proc_0_228` for waveform generation
   - Manages timing with `Proc_0_227` and `Proc_0_245`
5. **Completion handling**: When all channels processed (`var_28 > max`):
   - Calls `Proc_0_365` with parameter `2`
   - Updates UI labels with `Round(value, 2)` formatted as strings
   - Calls `Proc_0_344` for post-cycle cleanup
6. **Error handling**: Logs errors with format `"Error in Timer1_Timer ErrorCode: {code} Port: {port}"`
7. **Event hold reset**: Detects stale event holds and logs `"Error in Timer1_Timer EventHold manually reset. Expiry time: {seconds} seconds"`

### Recoverable Constants
- Timer watchdog: 60,000 (60 seconds)
- Maximum channel index: 128
- Sub-channel index max: 7
- Rounding precision: 2 decimal places
- Parameter 0x27 (39) passed to Proc_0_223

### Key Error Messages
- `"Error in Timer1_Timer EventHold manually reset. Expiry time: {n} seconds"`
- `"Error in Timer1_Timer ErrorCode: {code} Port: {port}"`

### Architecture Insight
The timer appears to drive a round-robin polling loop across up to 128 generator channels, computing frequency/amplitude values per tick and sending them to hardware. The `Proc_0_357`/`Proc_0_356` functions likely compute instantaneous frequency and amplitude. `Proc_0_223` likely formats and sends the USB HID packet.

---

## 4. Encryption-Related Procedures (Lines 17935-18099)

### EncryptFreq (Line 17935)

**Purpose:** Encrypts frequency values for storage in data files using the digit substitution tables initialized in Form_Load.

**Algorithm (reconstructed):**
1. Seed RNG with `Randomize(10)`
2. If input is empty, return empty
3. Check first character -- reject certain marker characters
4. Generate a 4-digit random key prefix using `CLng()` calls (loop 1 to 4)
5. Accumulate key sum in `esi`
6. For each character of the input:
   - If NOT a digit (not in `"0123456789"`): append as-is (preserves decimal points, signs, etc.)
   - If IS a digit: look up its position in `"0123456789"`, use accumulated sum to index into the substitution table, substitute with the encoded digit
   - Increment accumulated sum by the digit's position
   - Wrap accumulated sum at 100

**Key Constants:**
- Randomize seed: `10`
- Digit lookup string: `"0123456789"`
- Substitution table size: 100 entries (matching the 100 10-digit strings from Form_Load)
- Sum wrap threshold: 100

### DecryptFreq (Line 18051)

**Purpose:** Reverse of EncryptFreq -- decrypts stored frequency values.

**Algorithm (reconstructed):**
1. Minimum input length: 6 characters (first 5 are the key prefix)
2. Read first character as marker check
3. Extract 4 pairs of digits (positions 2-5) to reconstruct the original random key
4. For each remaining character:
   - If NOT a digit: append as-is
   - If IS a digit: reverse the substitution lookup, subtract accumulated sum
5. Decrement/adjust accumulated sum accordingly

### GetArrayLength (Line 17984)
Utility to get `UBound` of an array with error handling.

### OpenFile (Line 17998)
Common dialog wrapper for Open/Save file dialogs. Parameters: `InitDir`, `Filter`, `Filename`, `DefaultExt`, `DialogTitle`, `OpenMode` (1=Open, 2=Save). Checks file existence and whether file is already open.

### WriteErrorLogFile (Line 18099)
Appends errors to the log file with procedure name, error source, and description.

### C# Porting Notes
The encrypt/decrypt system is a polyalphabetic substitution cipher with:
- A fixed seed (10) for the VB6 `Rnd()` function
- 100 substitution alphabets (the 10-digit permutation strings)
- A running sum counter that selects which alphabet to use
- Non-digit characters passed through unmodified

This must be reimplemented exactly if reading existing encrypted `.s2d` files is required. The VB6 `Randomize`/`Rnd` functions have specific behavior that must be matched precisely.

---

## 5. FindTheHid -- HID Device Enumeration (Lines 18177-18330)

### Purpose
Enumerates USB HID devices to find Spooky2 generator hardware by VendorID and ProductID.

### Algorithm
1. Call `HidD_GetHidGuid` to get the HID device class GUID
2. Build a device path string from GUID components using `Hex$()` separated by hyphens
3. Call `SetupDiGetClassDevsA` with the HID GUID to get device info set
4. Call `Proc_0_361` with parameter `0x20` (32) -- likely sets buffer size
5. Loop with `SetupDiEnumDeviceInterfaces` incrementing device index (`var_40`)
6. For each device:
   - Get required buffer size via `SetupDiGetDeviceInterfaceDetailA` (first call with null buffer)
   - Allocate buffer with `ReDim`
   - Copy memory with `CopyMemory` (4 bytes for cbSize)
   - Get actual device path via second `SetupDiGetDeviceInterfaceDetailA` call
   - Convert path string with `StrConv(path, 64, 0)` -- vbUnicode conversion
   - Strip first 4 characters, take `Right$` of remaining
   - Open device with `CreateFile` (access=`0xC0000000` GENERIC_READ|GENERIC_WRITE, shareMode=3, disposition=3 OPEN_EXISTING)
   - Call `HidD_GetAttributes` to read VendorID and ProductID
   - Compare against target VendorID and ProductID
   - If match: close handle, continue; if both VendorID and ProductID match (`VTable_00001C7C` and `VTable_00001C7E`), break loop
7. After match found:
   - Call `SetupDiDestroyDeviceInfoList`
   - Call `Proc_0_363` -- likely prepares device for communication
   - Reopen with `CreateFile` using flags `0x40000000` (`FILE_FLAG_OVERLAPPED`) for async I/O
   - Call `Proc_0_364` -- likely initializes read/write overlapped structures

### Recoverable Constants
- Device enumeration limit: 8 devices per bus (array bound check `>= 8`)
- Bus byte threshold: 16 (split between high/low bytes at line 18202)
- CreateFile access: `0xC0000000` (GENERIC_READ | GENERIC_WRITE)
- CreateFile share mode: 3 (FILE_SHARE_READ | FILE_SHARE_WRITE)
- CreateFile flags for async: `0x40000000` (FILE_FLAG_OVERLAPPED)
- Device path padding string: `var_004A1CA0` (likely "0" for hex padding)
- Device path separator: `var_004A27E4` (likely "-")
- Device path suffix: `var_004A353C` (purpose unclear)

### Error Messages
- `"Error in FindTheHid"` -- General error handler

### C# Porting Notes
Replace entire function with HidSharp library:
```
var devices = DeviceList.Local.GetHidDevices(vendorId, productId);
```
This eliminates ~150 lines of SetupApi/HID P/Invoke code.

---

## 6. modWave.bas -- Waveform Module (556 lines)

### Purpose
WAV file generation for audio output. Constructs WAV file binary data in memory.

### Proc_24_0 (Line 2) -- WAV Header Construction

**What it does:**
Builds a WAV file header and data section from a 2D array of audio samples.

**WAV Format Constants (reconstructed from byte offsets):**
- Header offset `0x2C` (44 bytes) -- standard WAV header size
- Byte packing with `And 16777215` (0xFFFFFF) and `sar 18h` (shift right 24) -- splitting 32-bit values into bytes
- Sample data stored in byte array `var_30`
- Header fields written at byte positions 0x00-0x2B (offsets 0-43)
- Multiple channels supported (UBound dimension 1 = channels, dimension 2 = samples)

**Byte Layout (from assembly analysis):**
The code writes bytes 0 through 43 of the WAV header, with sample data following at offset 44. The formula `var_84 / 8` suggests bits-per-sample to bytes-per-sample conversion (e.g., 8-bit or 16-bit audio). The array stores:
- Dimension 1: samples per channel
- Dimension 2: channels (stereo or multi-channel)

Data size calculation: `samples * channels * bytesPerSample`
File size: data size + 44 (header) - 8

**Bit depth handling:** Code at line 450 checks `If edi <> 8` suggesting 8-bit vs 16-bit sample paths. For 8-bit samples, a simpler byte copy is used; for 16-bit, each sample is split into two bytes using shift operations.

### Proc_24_1 (Line 484) -- WAV File Save

**What it does:**
Saves a byte array as a WAV file to disk.

**Algorithm:**
1. Attempt to open file for Input to check existence
2. If file exists and has content, kill (delete) it
3. Open for Binary write (`Open For Binary As #1`)
4. Write initial header/data via `Put #1`
5. Loop through array, writing additional data blocks
6. Resize array (ReDim Preserve) to trim trailing data
7. Write remaining blocks
8. Close file
9. Error handling with `On Error Resume Next`

### C# Porting Notes
Replace with standard WAV writing using `System.IO.BinaryWriter`:
```csharp
// Write RIFF header, fmt chunk, data chunk
// Use NAudio library for more complex waveform generation
```

---

## 7. clsRndCrypt.cls -- Encryption Class (397 lines)

### Purpose
Multi-level XOR-based encryption/decryption class used for securing configuration data and preset files.

### RndCrypt (Line 15) -- Basic String XOR Encryption

**Algorithm:**
1. Seed RNG: `Randomize(Len(password))`
2. Compute password hash: For each character in password, XOR its ASCII value with running accumulator `var_28`, using `Rnd()` as additional entropy
3. Re-seed RNG: `Randomize(var_28)` (password-derived seed)
4. For each character in input string: XOR with next `Rnd()` value, replace in-place using `Mid$` statement

**Key detail:** The XOR uses `Asc(char) xor eax xor ecx` where `eax` comes from `Rnd()` and there is a mask operation involving `-2147483393` and `-256 + 1` to constrain the XOR key to byte range.

### RndCryptB (Line 50) -- Binary/Byte Array XOR Encryption

Same algorithm as RndCrypt but operates on byte arrays (via `StrConv(string, 128)` = vbFromUnicode). Converts password and input to byte arrays before XOR processing. Returns result via `StrConv(result, 64)` = vbUnicode.

### RndCryptLevel2 (Line 117) -- Enhanced Multi-Pass Encryption

**Parameters:**
- `strValue` -- Data to encrypt
- `strPassword` -- Encryption key
- `bSeedPasses` -- Number of seed generation passes
- `bDataPasses` -- Number of data encryption passes
- `InputType` -- Input format (0=raw, 1=hex, 2=byte array)
- `OutputType` -- Output format
- `RemoveInvalidChars` -- Whether to sanitize input

**Algorithm:**
1. Handle empty input/password (use `Chr(0)` as default password)
2. Based on `InputType`:
   - Type 0: Process raw string
   - Type 1: Clean input of non-hex characters matching `[0-9A-F]` pattern, convert from hex
   - Type 2: Convert from byte array
3. Multi-pass encryption with configurable seed and data passes
4. Result stored in byte array

### Base64EncodingB (Line 179) -- Base64 Encoding

Standard Base64 encoding operating on byte arrays. Uses standard 6-bit grouping with bit manipulation:
- `* 65536`, `* 256` for byte packing
- `sar 12h` (shift right 18) for 6-bit extraction
- `And 63` for 6-bit masking
- Output array sized as `ceil(input_length / 3) * 4`

### Base64DecodingB (Line 254) -- Base64 Decoding

Standard Base64 decoding. Validates characters against `[A-Za-z0-9+/=]` pattern. Handles padding (`=` character, ASCII 61). Optional `RemoveInvalidChars` parameter.

### Proc_14_5 (Line 307) -- Base64 Lookup Table Initialization

Builds encode and decode lookup tables:
- Positions 0-25: 'A'-'Z' (0x41-0x5A)
- Positions 26-51: 'a'-'z' (0x61-0x7A)
- Positions 52-61: '0'-'9' (0x30-0x39)
- Position 62: '+' (0x2B implied)
- Position 63: '/' (0x2F implied)

Decode table maps ASCII values back to 6-bit values using:
- '0'-'9': value - 0x30
- 'A'-'F': value - 0x37

Also builds a hex decode table for the Level2 encryption's hex input mode.

### C# Porting Notes

**Critical:** The VB6 `Rnd()` function uses a specific linear congruential generator. To decrypt existing files, the C# implementation MUST replicate VB6's exact PRNG:
```
seed = (seed * 0x43FD43FD + 0xC39EC3) And 0xFFFFFF
result = seed / 16777216.0  (0x1000000)
```

The `Randomize(n)` function in VB6 combines the parameter with the current seed in a specific way that must also be replicated exactly.

For new data, use `System.Security.Cryptography.Aes` or similar modern encryption.

---

## Summary of Key Findings for C# Port

### Critical Path Components
1. **HID USB Communication** -- Replace with HidSharp library
2. **Frequency Encryption/Decryption** -- Must replicate VB6 `Rnd()` PRNG exactly for backward compatibility with `.s2d` files
3. **Timer Polling Loop** -- Convert to async Task-based pattern or `System.Timers.Timer`
4. **WAV Generation** -- Use NAudio or manual BinaryWriter
5. **Configuration File I/O** -- Plain text files with key=value or line-based format

### File Format Summary
| Extension | Format | Encryption |
|-----------|--------|------------|
| `.csv` | Comma-separated frequency data | None |
| `.s2d` | Spooky2 data file | EncryptFreq/DecryptFreq |
| `.txt` (config) | Line-based key-value | RndCrypt/RndCryptLevel2 |
| `.cjstyles` | UI skin file (third-party) | None |
| `.mp3` | Audio notifications | None |

### Hardware Constants
- USB HID communication via overlapped I/O
- Up to 128 channels per generator
- VendorID/ProductID matching for device discovery
- 60-second watchdog timer for USB connection health

### Application Identity
- Name: Spooky2
- Author: John White
- Copyright URL: http://www.cancerclinic.co.nz
- Product URL: https://www.spooky2.com
- Download URL: http://spooky2.com/downloadsPage/index.html
- Default install path: `c:\Spooky2`
- Version date in code: `20260304`
