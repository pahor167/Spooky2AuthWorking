# Spooky2 Hunt & Kill — Android

Native Android app (Kotlin + Jetpack Compose) that runs the Spooky2 Hunt & Kill
biofeedback protocol against a GeneratorX Pro over USB-OTG.

Full plan: [PLAN.md](PLAN.md). Implementation spec: [Spooky2Net/docs/HUNT_AND_KILL_SPEC.md](../Spooky2Net/docs/HUNT_AND_KILL_SPEC.md).

---

## Scope

**Hunt & Kill MVP only.** Out of scope for this project:

- Preset database (.s2d files, S2D decryption)
- Frequency calculators, spectrum sweeps
- Remote/WIFI generators
- iOS

**Why Android-only (not iOS):** iOS blocks direct USB-serial access entirely. There
is no public API equivalent to Android's USB Host mode, and the MFi program does not
cover CDC-ACM devices like the GeneratorX. USB-OTG + CDC-ACM via
`usb-serial-for-android` is the simplest path to the physical generator.

---

## Module Layout

```
spooky2-huntkill-android/
├── settings.gradle.kts        root Gradle (Kotlin DSL, 3-module project)
├── build.gradle.kts           plugin versions only
├── gradle/libs.versions.toml  version catalog
│
├── core/                      pure-Kotlin JVM library — NO Android dependencies
│   └── src/main/kotlin/.../core/
│       ├── auth/GeneratorAuthentication.kt    challenge-response handshake
│       ├── protocol/GeneratorProtocol.kt      command builders, FormatFrequency, response parsing
│       ├── scan/ScanEngine.kt                 ramp-up, baseline, sweep, DetectHits, Kill dwell
│       ├── scan/SlidingWindow.kt              SMA window
│       ├── scan/GeneratorLink.kt              interface: the only hardware seam seen by core
│       ├── waveform/WaveformTables.kt         DDS waveform table commands
│       └── model/                             ScanParameters, ScanProgress, ScanResult, GeneratorState
│   └── src/test/kotlin/...                    golden tests vs recorded dumps
│
├── transport/                 Android library — serial transport abstraction
│   └── SerialTransport.kt     interface: open/close/write/readLine
│   (UsbCdcSerialTransport — real USB CDC-ACM impl, Phase 3)
│   (FakeTransport           — dump replay for offline testing, Phase 3)
│
└── app/                       Android application — Compose UI + Hilt DI
    ├── MainActivity.kt        entry point (@AndroidEntryPoint)
    ├── HuntKillApp.kt         @HiltAndroidApp Application class
    └── (screens: connect / hunt / kill — Phase 4)
```

### Rationale for Three-Module Split

`core` has zero Android dependencies. Every algorithm (auth, frequency encoding,
hit detection) runs in a plain JVM test without an emulator or device. The hardware
coupling is isolated to one interface (`SerialTransport`) in `transport`, so the full
Hunt & Kill flow can run end-to-end against `FakeTransport` while hardware is unavailable.

---

## Key Dependencies

| Library | Version | Purpose |
|---------|---------|---------|
| Kotlin | 2.0.21 | Language |
| Kotlin Coroutines | 1.9.0 | Async scan engine |
| Jetpack Compose BOM | 2024.10.01 | UI |
| Hilt | 2.52 | Dependency injection |
| usb-serial-for-android | 3.8.1 | CDC-ACM USB serial (via JitPack) |
| MockK | 1.13.13 | Test doubles |

---

## Building

Prerequisites: Android Studio Hedgehog or later, JDK 17, Android SDK with API 26+.

```sh
# Build everything (compiles core JVM tests + Android modules)
./gradlew build

# Run core unit tests (no emulator needed)
./gradlew :core:test

# Build a debug APK
./gradlew :app:assembleDebug
# Output: app/build/outputs/apk/debug/app-debug.apk

# Build release
./gradlew :app:assembleRelease
```

All commands run from the `spooky2-huntkill-android/` directory.

---

## No-Hardware Testing Strategy

The core algorithms are tested against real GeneratorX Pro serial dumps without any
hardware. The dumps live in `core/src/test/resources/dumps/` (copied from
`Spooky2Net/Data/`):

| Dump | Content | Tests |
|------|---------|-------|
| `Handshake1`..`Handshake10` | 10 real auth exchanges | Auth golden vectors — echo + token must match exactly |
| `FullHuntAndKill` | Complete Hunt & Kill session (~96 K lines) | DetectHits must reproduce the C# golden hits bit-for-bit |

Golden test file: `core/src/test/kotlin/.../scan/HuntAndKillReplayTest.kt`

The test feeds the `FullHuntAndKill` dump through `ScanEngine.detectHits` and asserts
the same 10 hit frequencies and deviations produced by the C# `ScanService.DetectHits`.
The values are identical IEEE-754 results — any algorithm drift fails the test.

```sh
# Run only the replay golden test
./gradlew :core:test --tests "*.HuntAndKillReplayTest"
```

---

## Current Status

| Phase | Description | Status |
|-------|-------------|--------|
| 0 | Spec docs (HUNT_AND_KILL_SPEC.md, READMEs) | Done |
| 1 | Android project skeleton, Gradle, empty Compose app | Done |
| 2 | Port core (auth, protocol, scan) + golden tests | Done — all golden tests pass |
| 3 | Transport layer (SerialTransport interface, FakeTransport, UsbCdcSerialTransport skeleton) | In progress |
| 4 | Hunt & Kill UI + orchestration (emulator, FakeTransport) | In progress |
| 5 | Hardware verification (real USB-OTG + GeneratorX) | Deferred — needs hardware |

Phase 2 exit criterion: all golden tests green = algorithms provably match the working
C# port. Phase 5 is the only remaining unverifiable step — it is isolated to
`UsbCdcSerialTransport`.

---

## Safety Note

This app drives a device that emits electrical signals. The port replicates the
original Spooky2 safety sequences exactly: amplitude ramp-up before scanning,
ramp-down after, and output clear on stop/cancel/error. Do not modify these
sequences. Hardware testing occurs only in Phase 5 with informed oversight.
