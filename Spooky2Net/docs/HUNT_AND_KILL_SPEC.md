# Hunt & Kill Implementation Spec

This document is the implementation contract for the Spooky2 Hunt & Kill biofeedback
protocol, distilled from the verified C# port in `Spooky2Net/src/Spooky2.Services/`.
A developer on any platform should be able to re-implement the full flow from this
document alone. All claims are cross-referenced to the source files.

---

## Table of Contents

1. [Overview](#overview)
2. [Auth: Challenge-Response Handshake](#auth-challenge-response-handshake)
3. [Protocol: Command Grammar and Framing](#protocol-command-grammar-and-framing)
4. [Discovery and Initialization](#discovery-and-initialization)
5. [Scan Engine: Hunt Phase](#scan-engine-hunt-phase)
6. [Kill Phase](#kill-phase)
7. [ScanParameters Reference](#scanparameters-reference)

---

## Overview

Hunt & Kill is a two-phase biofeedback scan:

- **Hunt** — sweep a frequency range while reading sensor data from the generator,
  detect frequencies where the signal deviates significantly from the running average
  (local maxima/minima), and return the top-N "hit" frequencies.
- **Kill** — dwell at each hit frequency for a configurable time, outputting at full
  amplitude.

The generator is a GeneratorX Pro connected via USB-serial (CDC-ACM). Before any
commands can be sent the app must complete a proprietary challenge-response handshake.

---

## Auth: Challenge-Response Handshake

**Source:** `Spooky2Net/src/Spooky2.Services/Communication/GeneratorAuthentication.cs`

### Flow

```
App → Device:    :r90=CHALLENGE,\r\n
Device → App:    :r90=ECHO,DEVICE_RESPONSE.\r\n
App → Device:    :w92=AUTH_TOKEN.\r\n
Device → App:    :ok\r\n
```

Steps:

1. App generates a 9-digit `CHALLENGE` — a random permutation of digits 1–9.
2. App sends `:r90=CHALLENGE,` terminated with CRLF.
3. Device responds with `:r90=ECHO,DEVICE_RESPONSE.` where both fields are 9-digit
   strings. Strip the leading `:`, strip the trailing `.`, split on `,`.
4. App verifies `ECHO == ComputeEcho(challenge, device_response)`. A mismatch is
   logged as a warning but does not abort the flow (source line 83).
5. App computes `AUTH_TOKEN = ComputeAuthToken(challenge, device_response)`.
6. App sends `:w92=AUTH_TOKEN.` (note: terminated with `.` not `,`).
7. Device responds `:ok`. Any response not containing "ok" means auth failed.

### Challenge Generation

Fisher-Yates shuffle of `[1,2,3,4,5,6,7,8,9]`. Result is a 9-digit string, all
digits in 1–9 with no repeats. (`GeneratorAuthentication.cs:41`)

### Echo Formula (`ComputeEcho`)

Implements `Proc_0_353` extracted from the VB6 binary at VA `0x898BE0`.
(`GeneratorAuthentication.cs:56`)

```
Input:  challenge[1..9]  (1-based; digit values not indices)
        device_response[1..9]
Output: echo[9]

For i in 0..8:
    (posA, posB, posC) = EchoIndices[i]
    respDigit = int(device_response[i])          // value of i-th response digit
    d = (challenge[posB] * challenge[posC]
         + challenge[posA] * challenge[respDigit]) % 9 + 1
    echo[i] = char('0' + d)
```

**Critical:** modulo is **9**, not 10. The `+1` ensures output is always in 1–9.

`EchoIndices` table (0-based index → `(posA, posB, posC)`, 1-based positions):

| i | posA | posB | posC |
|---|------|------|------|
| 0 |  8   |  6   |  5   |
| 1 |  1   |  5   |  7   |
| 2 |  3   |  4   |  9   |
| 3 |  8   |  5   |  7   |
| 4 |  8   |  9   |  6   |
| 5 |  3   |  1   |  4   |
| 6 |  3   |  3   |  1   |
| 7 |  4   |  9   |  3   |
| 8 |  6   |  7   |  4   |

### Token Formula (`ComputeAuthToken`)

Implements `Proc_0_354` extracted from the VB6 binary at VA `0x89A5F0`.
(`GeneratorAuthentication.cs:77`)

```
Input:  challenge[1..9]
        device_response[1..9]  (1-based; digit values not indices)
Output: token[9]

For i in 0..8:
    (posA, posB, posC) = TokenIndices[i]
    chalDigit = int(challenge[i])               // value of i-th challenge digit
    d = (device_response[posB] * device_response[posC]
         + device_response[posA] * device_response[chalDigit]) % 9 + 1
    token[i] = char('0' + d)
```

`TokenIndices` table:

| i | posA | posB | posC |
|---|------|------|------|
| 0 |  4   |  6   |  8   |
| 1 |  6   |  4   |  1   |
| 2 |  8   |  6   |  5   |
| 3 |  3   |  2   |  9   |
| 4 |  7   |  8   |  4   |
| 5 |  3   |  1   |  7   |
| 6 |  9   |  4   |  3   |
| 7 |  1   |  6   |  2   |
| 8 |  3   |  2   |  8   |

### Key Differences Between Echo and Token

| | Echo (`ComputeEcho`) | Token (`ComputeAuthToken`) |
|---|---|---|
| Iterator digit source | `device_response[i]` | `challenge[i]` |
| Array indexed | `challenge[]` | `device_response[]` |
| Used for | Verification (step 4) | Authentication (step 6) |

---

## Protocol: Command Grammar and Framing

**Source:** `Spooky2Net/src/Spooky2.Services/Communication/GeneratorProtocol.cs`

### Command Format

```
:<type><register>[=<value>]\r\n
```

- `:` — mandatory leading colon
- `<type>` — one character: `a` (action), `r` (read), `w` (write), `n` (name/display)
- `<register>` — decimal register number, no leading zeros except as required
- `=<value>` — optional payload; many write commands use a trailing `,`
- `\r\n` — CRLF terminator (ASCII 0x0D 0x0A); mandatory on every command

### Response Format

Responses are ASCII text lines terminated with CRLF.

| Prefix | Meaning | Example |
|--------|---------|---------|
| `:ok`  | Success | `:ok\r\n` |
| `:err` | Error (or expected response for ping/handshake commands) | `:err\r\n` |
| `:<cmd>=<value>.` | Read response with data | `:r11=53001.\r\n` |

Parsing rules (`GeneratorProtocol.cs:312`):
1. Strip leading `:` if present.
2. If starts with `ok` → success; extract value after `=` if present.
3. If starts with `err` → failure; extract value after `=` if present.
4. Otherwise: if a value exists after `=` treat as success, else failure.
5. Strip trailing `,` and `.` from extracted values.

### Sensor Read Response Parsing

`:r11=53001.` → parse as double `53001.0`. (`GeneratorProtocol.cs:377`)
Strip leading `:`, find `=`, take substring after it, strip trailing `.`.

### Frequency Encoding (`FormatFrequency`)

**Source:** `GeneratorProtocol.cs:223`

Used for all frequency writes during the scan sweep (`:w24=`, `:w25=`).

> **CORRECTED 2026-06-10.** An earlier revision of this spec (and the C# port)
> documented `posCode = integer_digit_count - 4` over a fixed 8-fractional-digit
> field. That rule is **WRONG**: on real GeneratorX Pro hardware the device ran
> at ×10/×100 the intended frequency (e.g. app 105,235.87 Hz → device
> 10,523,587.32 Hz). The rule below was derived from the original Spooky2
> software's own serial dump and reproduces **every** `:w24=` sweep/kill line in
> `Data/FullHuntAndKill` byte-for-byte (15132/15132), and is hardware-verified.

Encoding:

1. Round the frequency to **8 decimal places** (`F8`, half-even).
2. Remove the decimal point.
3. **Strip the trailing zeros** that came from the fractional part
   (leading zeros of the integer part are preserved — they encode the
   magnitude for sub-Hz frequencies).
4. Append one position-code digit:

```
position_code = 8 - fractional_digits_kept
```

The firmware re-inserts the decimal point: the last `8 - posCode` mantissa
digits are the fractional part.

#### Worked Examples (from `Data/FullHuntAndKill`)

| Frequency (Hz) | F8 string | frac kept | posCode | Full payload | Command |
|---|---|---|---|---|---|
| 41010.25 | `41010.25000000` | `25` (2) | 6 | `41010256` | `:w24=41010256,` |
| 41020.5025625 | `41020.50256250` | `5025625` (7) | 1 | `4102050256251` | `:w24=4102050256251,` |
| 41030.75768814 | `41030.75768814` | `75768814` (8) | 0 | `41030757688140` | `:w24=41030757688140,` |
| 41000 | `41000.00000000` | (0) | 8 | `410008` | `:w24=410008,` |
| 1796956.27039622 | `1796956.27039622` | `27039622` (8) | 0 | `1796956270396220` | `:w24=1796956270396220,` |
| 0.5 | `0.50000000` | `5` (1) | 7 | `057` | `:w24=057,` |

#### Raw Hz Format (Pre-Scan Only)

During setup and the amplitude ramp, the start frequency is sent as a plain integer
with a trailing comma: `:w24=41000,`. This is `BuildSetFrequencyRawHz`.
(`GeneratorProtocol.cs:244`). Do NOT use this format during the sweep.

### Amplitude Commands

Amplitude is expressed in **centivolts** (hundredths of a volt):

```
:w28=2000,    →  20.00 V on output 1
:w29=2000,    →  20.00 V on output 2
```

`BuildSetAmplitudeCv1/2` (`GeneratorProtocol.cs:249,253`).

### Key Command Reference

| Command | Register | Example | Purpose |
|---------|----------|---------|---------|
| `:r11=,` | r11 | `:r11=,` | Read angle/phase (biofeedback sensor) |
| `:r12=,` | r12 | `:r12=,` | Read current mA (biofeedback sensor) |
| `:w24=X,` | w24 | `:w24=410000000000001,` | Set output 1 frequency (both channels) |
| `:w28=X,` | w28 | `:w28=2000,` | Set output 1 amplitude (centivolt) |
| `:w29=X,` | w29 | `:w29=2000,` | Set output 2 amplitude (centivolt) |
| `:w11=1,,` | w11 | `:w11=1,,` | Enable output 1 |
| `:w11=,1,` | w11 | `:w11=,1,` | Enable output 2 |
| `:w12=0,,` | w12 | `:w12=0,,` | Clear frequency channel 1 |
| `:w12=,0,` | w12 | `:w12=,0,` | Clear frequency channel 2 |
| `:w61=1` | w61 | `:w61=1` | Start output 1 |
| `:w62=1` | w62 | `:w62=1` | Start output 2 |
| `:w61=0` | w61 | `:w61=0` | Stop output 1 |
| `:w62=0` | w62 | `:w62=0` | Stop output 2 |
| `:w14=X,` | w14 | `:w14=1,` | Sync on (1) / off (0) |
| `:w15=X,Y,` | w15 | `:w15=1,1,` | Low frequency mode (CRITICAL — must be set before scanning) |
| `:w20=X,` | w20 | `:w20=11,` | Set waveform ch1 (11 = sine) |
| `:w21=X,` | w21 | `:w21=25,` | Set waveform ch2 (25 = inverse) |
| `:n00=X` | n00 | `:n00=Stopped` | Set display name |

---

## Discovery and Initialization

**Source:** `Spooky2Net/src/Spooky2.Services/Communication/GeneratorService.cs`

### Port Settings

- 8N1 (8 data bits, no parity, 1 stop bit)
- DTR = false, RTS = true (inferred from VB6 original behavior)
- Try 115200 baud first (GeneratorX), then 57600 (XM generators)

### Discovery Probe Sequence

For each available serial port, try baud rates `[57600, 115200]` in that order
(`GeneratorService.cs:45`):

**At 115200 (GeneratorX):**

1. Open port, wait 200 ms, flush any buffered bytes.
2. Generate challenge and send `:r90=CHALLENGE,`.
3. Parse response: expect `:r90=ECHO,DEVICE_RESPONSE.`
4. Verify echo (log warning on mismatch, continue).
5. Compute and send auth token `:w92=AUTH_TOKEN.`
6. Confirm `:ok` response. If not ok, skip this port.
7. Run the post-auth init sequence (see below).
8. Record port as GeneratorX.

**At 57600 (XM):**

1. Open port, send `:a00` (ping).
2. If no response, skip.
3. Send `:a0012345` (handshake), `:r80`, `:r68`, `:r91`.
4. Record port as XM.

Discovery timeout per command: 2000 ms (`GeneratorService.cs:484`).

### Post-Auth Init Sequence (GeneratorX)

Sent immediately after successful authentication. Verified from the
`Data/LatestComparison/OldSpooky` serial dump (`GeneratorService.cs:99`):

```
:r02=0,        read hardware info
:n00=$         query firmware name
:w14=0,        sync off
:w17=0,0,      waveform inversion off
:w24=0,        frequency 0
:w25=0,        frequency ch2 0
:w15=1,1,      LOW FREQUENCY MODE — CRITICAL
:w24=00,       frequency 0 (raw)
:w32=120,      amplitude ch1 = 120
:w33=120,      amplitude ch2 = 120
:n00=...Stopped
:w13=0,        modulation off
:w28=0,        amplitude cv1 = 0
:w29=0,        amplitude cv2 = 0
:w24=00,       frequency 0
:w12=0,,       clear frequency ch1
:w12=,0,       clear frequency ch2
:w32=120,      amplitude ch1 = 120
:w40=0,        duty cycle 0
:w33=120,      amplitude ch2 = 120
:w40=0,        duty cycle 0
:w13=0,        modulation off
:w20=11,       waveform 1 = sine
:w14=1,        sync ON
:w12=0,,       clear frequency ch1
:w12=,0,       clear frequency ch2
:w21=25,       waveform 2 = inverse
```

`:w15=1,1,` (low frequency mode on both channels) is CRITICAL. Without it the
firmware interprets frequency values on the wrong scale.

### Port Management

The port is kept **persistently open** per generator after discovery (not opened/closed
per command). Commands write CRLF-terminated ASCII and block-read until a newline
arrives or the timeout expires. No explicit flush between commands — ReadLine() handles
synchronization via CRLF terminators (`GeneratorService.cs:440`).

---

## Scan Engine: Hunt Phase

**Source:** `Spooky2Net/src/Spooky2.Services/Scanner/ScanService.cs`

The scan runs in three sequential phases.

### Phase 1: Generator Setup + Amplitude Ramp-Up

Sent at the start of every scan (not just during discovery). Matches the original
Spooky2 serial dump commands 4–28 (`ScanService.cs:65`):

```
:w14=0,        sync off
:w17=0,0,      waveform inversion off
:w24=0,        freq 0
:w25=0,        freq ch2 0
:w15=1,1,      LOW FREQUENCY MODE
:w24=00,       freq 0 raw
:w32=120,      amplitude ch1
:w33=120,      amplitude ch2
:n00=Stopped   display name
:w13=0,        modulation off
:w28=0,        amplitude cv1 = 0
:w29=0,        amplitude cv2 = 0
:w24=00,       freq 0
:w12=0,,       clear ch1
:w12=,0,       clear ch2
:w32=120,      amplitude ch1
:w40=0,        duty cycle
:w33=120,      amplitude ch2
:w40=0,        duty cycle
:w13=0,        modulation off
:w20=11,       waveform 1 = sine
:w14=1,        sync ON
:w12=0,,       clear ch1
:w12=,0,       clear ch2
:w21=25,       waveform 2 = inverse
[waveform table upload — batch]
:r11=,         pre-scan sensor read
:r11=,         pre-scan sensor read
:r12=,         pre-scan sensor read
:w24=<startHz>, set start frequency (raw Hz integer)
:w21=25,       waveform 2 = inverse (repeated)
```

#### Amplitude Ramp-Up

When `enableAmplitudeRampUp = true` (`ScanService.cs:111`):

1. Compute `firstCv = round(targetAmplitudeCv / rampSteps)`.
2. Send `:w28=firstCv,` and `:w29=firstCv,`.
3. Enable outputs: `:w11=1,,` then `:w11=,1,`.
4. Send `:w20=11,` (waveform 1 = sine, after output enable).
5. Take two baseline sensor reads (`:r11=,`, `:r12=,`).
6. Batch-send `rampSteps` pairs:

```
for i in 1..rampSteps:
    cv = min(round((i+1) * targetAmplitudeCv / rampSteps), targetAmplitudeCv)
    send :w28=cv,
    send :w29=cv,
```

Default `rampSteps = 330`, `targetAmplitudeCv = 2000` (20 V).
Formula verified from serial dump: 330 steps, `round((i+1)*target/330)`.
(`ScanParameters.cs:49`)

After the ramp, wait `startDelayMs` (default 200 ms) before baseline reads.

### Phase 2: Baseline Reads

Fill the SMA window with stable readings at the start frequency before any frequency
changes. This prevents the first sweep steps from producing false hits.
(`ScanService.cs:175`)

Pattern (verified from serial dump: 204 `:r11=,` reads + 203 `:r12=,` reads):

1. One standalone `:r11=,` read (initial angle).
2. `baselineReadCount` (default 203) paired reads: `:r11=,` then `:r12=,` each pair.

Both the SMA window and a `baselineReadings` list are filled. The last `raWindow`
entries of the baseline list are prepended to `scanReadings` as seed entries with
`frequency = 0` so the SMA is pre-warmed for the first sweep step. (`ScanService.cs:232`)

### Phase 3: Frequency Sweep

`ScanService.cs:218`. For each frequency step:

1. Send `:w24=<encoded_freq>,` using `FormatFrequency`.
2. Wait `minReadDelaySeconds` (default 0.07 s).
3. Read sensors: `samplesPerStep` (default 1) pairs of `:r11=,` / `:r12=,`.
   Average across samples if `samplesPerStep > 1`.
4. Append `(frequency, reading)` to `scanReadings`.

#### Frequency Step Calculation

`ScanService.cs:667`:

```
freq = startFrequency
while freq <= endFrequency:
    emit freq
    if usePercentageStep:
        freq += freq * (stepSizePercent / 100.0)
    else:
        freq += stepSizeHz
```

Default parameters produce **15,130 steps** from 41,000 Hz to ~1,799,653 Hz at
0.025% increments. Verified by the golden test in the Android port.

#### Sensor Selection

Despite the preset flag `BFB_Detect_mA=True`, empirical testing against real dump
data shows the original Spooky2 uses **angle** (`:r11`) for hit detection, not current
(`:r12`). Using current produces spurious hits at 980 kHz / 1150 kHz. Default
`useCurrent = false`, `useAngle = true`. (`ScanParameters.cs:30`)

### Post-Processing: Hit Detection

`ScanService.DetectHits` / `ScanService.cs:614`. Decoded from VB6 `Proc_0_331`.

#### Phase 1: SMA + Deviation

Sliding window of size `raWindow` (default 20). For each step in `scanReadings`:

```
ra = window.SimpleAverage()   // 0 if not yet full
deviation = reading - ra      // 0 if window not yet full
steps.append(freq, reading, deviation, ra)
window.add(reading)
```

`SimpleAverage()` = arithmetic mean of buffered values (not weighted).
(`ScanService.cs:706`)

#### Phase 2: Local Extrema Detection ("Detecting Asymptotes")

For each step `i` in `[1, len-2]`:

```
isLocalMax = reading[i] > reading[i-1] AND reading[i] > reading[i+1]
isLocalMin = reading[i] < reading[i-1] AND reading[i] < reading[i+1]
```

(`ScanService.cs:633`)

#### Phase 3: Threshold Filter ("Filling GreatestHits")

A step is a hit if:

```
(detectMax AND isLocalMax AND deviation > threshold)
OR
(detectMin AND isLocalMin AND deviation < -threshold)
```

Default `detectMax = true`, `detectMin = false`, `threshold = 0`.
(`ScanParameters.cs:22–33`)

#### Phase 4: Sort and Truncate

Sort hits by `deviation` descending, take top `maxHits` (default 10).
(`ScanService.cs:661`)

#### Known Limitation

No cluster deduplication. With `threshold = 0`, all 10 hit slots can be filled
by frequencies from the same narrow spike cluster. The original VB6 has a
deduplication mechanism that the C# port does not yet implement. This is a known
gap documented in `HuntAndKillReplayTests.cs`.

---

## Kill Phase

`ScanService.RunHuntAndKill` / `ScanService.cs:362`.

1. Run `RunBiofeedbackScan` → get hits list.
2. If no hits, stop.
3. Set full amplitude: `:w28=targetAmplitudeCv,` and `:w29=targetAmplitudeCv,`.
4. Write first kill frequency to the generator, then call `Start` (enables outputs).
5. For each hit frequency:
   - Send `:w24=<encoded_freq>,` (via `WriteFrequencies`).
   - Dwell `dwellSeconds` (default 180 s = 3 min).
6. Stop outputs.
7. If `continueRefining = true`, repeat from step 1 (cycles until no hits or cancelled).
8. Final cleanup: clear both frequency channels, restore amplitude.

### Start / Stop Sequences

**Start** (`GeneratorService.cs:184`):
```
:w13=0,0,    modulation off
:w12=0,,     clear ch1
:w12=,0,     clear ch2
:w61=1       start output 1
:w62=1       start output 2
```

**Stop** (`GeneratorService.cs:204`):
```
:w61=0       stop output 1
:w62=0       stop output 2
:w13=0,0,    modulation off
:w12=0,,     clear ch1
:w12=,0,     clear ch2
```

---

## ScanParameters Reference

**Source:** `Spooky2Net/src/Spooky2.Core/Models/ScanParameters.cs`

All fields are immutable (C# `init`-only record properties).

| Field | Default | Unit | Description |
|-------|---------|------|-------------|
| `startFrequency` | `41000` | Hz | Sweep start |
| `endFrequency` | `1800000` | Hz | Sweep end |
| `usePercentageStep` | `true` | — | Use percentage-based step size |
| `stepSizePercent` | `0.025` | % | Step increment when `usePercentageStep` is true |
| `stepSizeHz` | `100` | Hz | Step increment when `usePercentageStep` is false |
| `maxHits` | `10` | count | Maximum hits to return |
| `raWindow` | `20` | samples | Primary SMA window size |
| `raWindow2` | `0` | samples | Secondary SMA window; 0 = same as `raWindow` |
| `useRetentiveWindow` | `false` | — | Use secondary window for detection |
| `calculateUsingPeak` | `false` | — | Peak detection mode instead of SMA |
| `samplesPerStep` | `1` | count | Sensor reads per frequency step (averaged) |
| `startDelayMs` | `200` | ms | Delay after ramp-up before baseline reads |
| `minReadDelaySeconds` | `0.07` | s | Delay between frequency write and sensor read |
| `detectMax` | `true` | — | Detect local maxima |
| `detectMin` | `false` | — | Detect local minima |
| `useCurrent` | `false` | — | Use `:r12` (current) for detection |
| `useAngle` | `true` | — | Use `:r11` (angle/phase) for detection |
| `loops` | `1` | count | Number of sweep passes |
| `threshold` | `0` | raw units | Minimum deviation to qualify as a hit |
| `continueRefining` | `true` | — | Repeat Hunt+Kill until no hits |
| `runOnGeneratorId` | `0` | id | Generator for Kill phase; 0 = same as Hunt |
| `dwellSeconds` | `180` | s | Kill dwell time per frequency (3 min) |
| `logName` | `""` | — | Display name shown on generator screen |
| `enableAmplitudeRampUp` | `true` | — | Gradual ramp from near-zero to target |
| `rampSteps` | `330` | steps | Number of amplitude ramp steps |
| `targetAmplitudeCv` | `2000` | cV | Target amplitude (2000 cV = 20 V) |
| `enableAmplitudeRampDown` | `true` | — | Ramp amplitude down after scan |
| `baselineReadCount` | `203` | pairs | Baseline `:r11`/`:r12` read pairs before sweep |

### Notes

- `dwellSeconds = 180` is derived from observed behavior in the serial dump; there
  is no explicit preset field for it. (`ScanParameters.cs:40`)
- `rampSteps = 330` is verified from the dump; the VB6 preset field
  `Ramp_Amplitude_Up_Rate=4` mapping formula is unknown. (`ScanParameters.cs:49`)
- `threshold = 0` means every local extremum with any positive deviation qualifies.
  With default parameters this produces dense clusters; the original VB6 applies
  cluster deduplication not yet ported.
- `baselineReadCount = 203` reflects the dump pattern: 1 initial standalone read +
  203 pairs = 407 total sensor reads before the sweep. (`ScanParameters.cs:57`)
