# Spooky2 Hunt & Kill — Native Android App (No-Hardware Build Plan)

## Goal

Native Android (Kotlin + Jetpack Compose) app that runs the Spooky2 **Hunt & Kill**
biofeedback protocol against a GeneratorX over USB. This plan covers everything that
can be built and verified **without the physical generator** — the hardware-only
verification (real USB-OTG run) is deferred to a later phase.

## Locked decisions

- **Stack:** Kotlin + Jetpack Compose, native Android.
- **Location:** `spooky2-huntkill-android/` (folder inside this repo).
- **Scope:** Hunt & Kill MVP only. No preset DB, no S2D decryption, no sweeps/calculators.
- **Connection (target):** USB-OTG direct, CDC-ACM (Microchip VID `0x04D8`), via
  `usb-serial-for-android`. Hardware-verified later.
- **Reference spec:** the tested C# port (`Spooky2Net/src/Spooky2.Services`) +
  recorded serial dumps in `Spooky2Net/Data/`. We port *from working code*, not from VB6.

## Reference assets (the "executable spec")

| Asset | Path | Used for |
|---|---|---|
| Auth algorithm | `Spooky2Net/src/Spooky2.Services/Communication/GeneratorAuthentication.cs` | Port echo/token index tables |
| Protocol/commands | `Spooky2Net/src/Spooky2.Services/Communication/GeneratorProtocol.cs` | Port command builders + freq encoding |
| Scan engine | `Spooky2Net/src/Spooky2.Services/Scanner/ScanService.cs` | Port ramp/baseline/sweep/DetectHits + Kill dwell |
| Waveform tables | `Spooky2Net/src/Spooky2.Services/Communication/WaveformTables.cs` | Port DDS tables as data |
| Handshake dumps | `Spooky2Net/Data/Handshake1..10` | Auth golden vectors |
| Full run dump | `Spooky2Net/Data/FullHuntAndKill`, `Start/FinishHuntAndKill` | Scan/Kill replay golden test + FakeTransport script |
| C# replay test | `Spooky2Net/tests/.../HuntAndKillReplayTests.cs`, `PlainTextDumpParser.cs` | Port the dump parser + expected outputs |

## Target module layout

```
spooky2-huntkill-android/
├── settings.gradle.kts            root Gradle (Kotlin DSL)
├── build.gradle.kts               versions, plugins
├── gradle/libs.versions.toml      version catalog
├── core/                          pure-Kotlin (JVM lib, NO android deps) — unit-testable on JVM
│   └── src/main/kotlin/.../core/
│       ├── auth/GeneratorAuthentication.kt
│       ├── protocol/GeneratorProtocol.kt      command builders, FormatFrequency, response parse
│       ├── scan/ScanEngine.kt                 ramp-up, baseline, sweep, DetectHits, Kill dwell
│       ├── scan/SlidingWindow.kt              SMA
│       ├── waveform/WaveformTables.kt
│       └── model/                             ScanParameters, ScanProgress, ScanResult, GeneratorState
│   └── src/test/kotlin/...                    golden tests vs copied dumps
├── transport/                     Android lib — serial abstraction
│   └── SerialTransport.kt (iface) · UsbCdcSerialTransport.kt (real) · FakeTransport.kt (replay)
└── app/                           Android app — Compose UI + ViewModels + DI
    ├── connect/ · hunt/ · kill/ · di/
    └── MainActivity, navigation
```

Rationale: `core` has zero Android deps → full JVM unit testing, no emulator needed.
`transport` isolates the only hardware-coupled code behind one interface, so the app
runs end-to-end on `FakeTransport` until hardware arrives.

---

## Phases (no-hardware)

### Phase 0 — Familiarization + spec doc (in existing repo)
- Add `README.md` (repo root) + `Spooky2Net/README.md`: architecture map, build/run, port status.
- Add focused comments to the 4 reference files (auth, protocol, scan, waveform tables).
- Write `Spooky2Net/docs/HUNT_AND_KILL_SPEC.md` — clean implementation contract:
  auth flow, full command sequence, scan phases, all `ScanParameters` defaults, freq encoding.
- **Exit:** spec doc reviewed; it is the contract Phase 2 builds against.

### Phase 1 — Android project skeleton
- Gradle (Kotlin DSL) + version catalog. `core` (JVM lib), `transport` (android-lib), `app`.
- min SDK 26, target latest. `<uses-feature android:name="android.hardware.usb.host">`.
- Compose, coroutines, DI (Hilt or manual), empty screens + nav graph.
- CI: `./gradlew build test` (GitHub Actions). 
- **Exit:** `./gradlew build` green; empty app launches in emulator.

### Phase 2 — Port `core` (pure Kotlin) + golden tests   ← biggest verifiable chunk
- Port `model/`, `auth/`, `protocol/` (incl. `FormatFrequency` verbatim), `scan/`, `waveform/`.
- Port `PlainTextDumpParser` → Kotlin; copy `Handshake1..10` + `FullHuntAndKill` dumps into
  `core/src/test/resources/`.
- Golden tests (gate):
  - Auth: reproduce echo + token for all handshake dumps.
  - `FormatFrequency`: match dump-encoded frequency strings.
  - `DetectHits`: match C# replay-test hit output on `FullHuntAndKill`.
- **Exit:** all golden tests green = algorithms provably match the working C# port.

### Phase 3 — Transport layer (no hardware)
- `SerialTransport` interface: `open/close`, `write(bytes)`, `readLine(timeout)`, port enumerate, USB-permission hook.
- `FakeTransport`: replays a recorded dump — feeds device responses (`:r90=...`, `:r11=`, `:ok`)
  in order so the full Hunt→Kill flow runs offline.
- `UsbCdcSerialTransport`: real impl on `usb-serial-for-android` `CdcAcmSerialDriver`,
  custom probe for VID `0x04D8`, 57600→115200, USB-permission `BroadcastReceiver`.
  **Written + compiles + unit-tested with mocks, but NOT hardware-verified this round.**
- **Exit:** `core` driven by `FakeTransport` completes a scripted Hunt→Kill in a JVM/instrumented test.

### Phase 4 — Hunt & Kill UI + orchestration (emulator)
- ViewModels (StateFlow) + Compose screens:
  - **Connect/Auth:** device list, request USB permission, run handshake, show result.
  - **Hunt config:** start/end freq, dwell, target amplitude (defaults from `ScanParameters`).
  - **Live scan:** progress (current freq, amplitude CV, angle reading), angle graph, Cancel.
  - **Hits:** detected frequencies + deviation.
  - **Kill:** dwell countdown per frequency; safety stop.
- All wired through DI to **`FakeTransport`** → entire flow demoable in emulator, no generator.
- Long-run plumbing: foreground service + wake-lock scaffolding (scans run minutes).
- **Exit:** full Hunt→Kill walkthrough in emulator against replayed dump; cancel works;
  safety stop (clear freq + ramp-down) fires on exit/error.

### Phase 5 — Hardware verification *(DEFERRED — needs GeneratorX + OTG device)*
- Swap DI to `UsbCdcSerialTransport`; confirm CDC enumeration / exact serial PID; real handshake;
  full Hunt→Kill on hardware; tune timing/delays; USB disconnect handling.

---

## Deliverable at end of no-HW work

App installs on an Android emulator and runs a **complete Hunt → Kill flow end-to-end**
against `FakeTransport` replaying a real generator dump, with **all core algorithms
green against the same golden vectors the C# port is tested on**. Only the physical
USB link remains unverified — isolated to one class (`UsbCdcSerialTransport`).

## Risks (no-HW portion)

| Risk | Sev | Mitigation |
|---|---|---|
| Ported algo drift from C# | MED | Golden tests vs shared dumps gate Phase 2 |
| Freq encoding edge cases (sub-Hz, leading zeros) | MED | Port `FormatFrequency` verbatim + unit tests on dump values |
| Dump format quirks when porting parser | LOW | Port `PlainTextDumpParser` 1:1; diff against C# parser output |
| `usb-serial-for-android` API assumptions wrong | MED (defers to Ph5) | Keep real transport thin; mock-test now, verify on HW later |

## Safety note

App drives a device delivering electrical signals to a person. Port the desktop safety
sequences **exactly** (amplitude ramp-up/down, stop-clears-output). No behavior changes vs
original. Add explicit disclaimers in UI. Hardware-output testing happens only in Phase 5
with informed control.

## Open items to confirm before coding

1. DI: **Hilt** (recommended) or manual?
2. Package name (e.g. `com.spooky2.huntkill`)?
3. Do Phase 0 doc/comments now in this repo, or fold into the Android project only?
