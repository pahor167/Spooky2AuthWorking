# Spooky2 — Hunt & Kill

This repository contains three related codebases at different stages of
implementation.

---

## What Is Spooky2 / Hunt & Kill?

Spooky2 is a PC application that drives a frequency generator (the GeneratorX Pro,
connected via USB-serial) to output configurable waveforms at configurable
frequencies. It is used in biofeedback and experimental frequency therapy research.

**Hunt & Kill** is a two-phase biofeedback scan protocol: the generator outputs a
swept sine wave while simultaneously sampling two impedance/current sensors on the
subject. A running-average algorithm detects frequencies where the sensor signal
deviates significantly from baseline (the "Hunt" phase). The detected frequencies
are then applied at full amplitude for a configurable dwell time (the "Kill" phase).
The cycle repeats until no new hits are detected or the user cancels.

The device communicates over USB-serial using a proprietary ASCII command protocol
with a challenge-response authentication handshake before any commands are accepted.

---

## Three Codebases

### 1. VB6 Original (repo root)

The original Spooky2 application, written in VB6. Source is `.frm` and `.bas` files
at the repo root (`Main.frm`, `ApiDeclarations.bas`, etc.). This is the ground-truth
behavior reference; the protocol and algorithms were reverse-engineered from the
compiled binary and verified against real hardware captures.

Not runnable without a legacy VB6 environment. Read-only reference.

### 2. .NET 10 / Avalonia Port (`Spooky2Net/`)

A cross-platform .NET 10 + Avalonia desktop port. All core logic (auth, protocol,
scan engine) has been verified against real GeneratorX hardware and serial dumps.
This is the **executable spec** for the Android port.

See [`Spooky2Net/README.md`](Spooky2Net/README.md) for architecture, build
instructions, and test coverage.

Implementation spec: [`Spooky2Net/docs/HUNT_AND_KILL_SPEC.md`](Spooky2Net/docs/HUNT_AND_KILL_SPEC.md)

### 3. Android Port (`spooky2-huntkill-android/`)

A native Android (Kotlin + Jetpack Compose) app porting the Hunt & Kill MVP. Targets
USB-OTG direct connection to the GeneratorX via CDC-ACM. The core algorithms are a
direct port from the C# services and share the same golden test vectors.

See [`spooky2-huntkill-android/README.md`](spooky2-huntkill-android/README.md) for
module layout, build instructions, and current phase status.

---

## Serial Dump Data

`Data/` at the repo root contains real GeneratorX Pro serial captures:

- `Handshake1`..`Handshake10` — 10 auth exchanges (golden vectors for auth tests)
- `FullHuntAndKill` — complete Hunt & Kill session (~96 K lines, used for replay tests)

These files are shared between the .NET and Android test suites.

---

## Disclaimer

This software interfaces with a device that delivers electrical signals. It is
provided for educational and research purposes only. Use only with appropriate
hardware, following all relevant safety guidelines. The authors make no claims
regarding medical efficacy or safety for any particular use. Do not use on humans
without full understanding of the device's operating parameters and applicable
regulations in your jurisdiction.
