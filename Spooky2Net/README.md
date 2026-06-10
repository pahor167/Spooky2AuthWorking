# Spooky2Net

.NET 10 / Avalonia port of the Spooky2 biofeedback application. This is the
**reference implementation** for the Android port (`spooky2-huntkill-android/`).
The C# services are the executable spec: the Android Kotlin code is a direct port
of the algorithms here, and the golden tests in both projects use the same serial
dump files.

---

## Project Structure

Five production projects + one test project, all under `src/` and `tests/`:

```
Spooky2Net/
├── src/
│   ├── Spooky2.Core/          Domain models, interfaces, enums — no framework deps
│   ├── Spooky2.Services/      Business logic: communication, scan engine, encryption
│   ├── Spooky2.ViewModels/    MVVM ViewModels (CommunityToolkit.Mvvm)
│   ├── Spooky2.Views/         Avalonia XAML views and controls
│   └── Spooky2.App/           Entry point, DI setup, app host
└── tests/
    └── Spooky2.Services.Tests/  Unit + integration tests (xUnit)
```

### Key Services in `Spooky2.Services`

| Service | File | Purpose |
|---------|------|---------|
| `GeneratorService` | `Communication/GeneratorService.cs` | Serial port discovery, command send/receive, port lifecycle |
| `ScanService` | `Scanner/ScanService.cs` | Hunt & Kill scan engine: ramp-up, baseline, sweep, hit detection |
| `GeneratorAuthentication` | `Communication/GeneratorAuthentication.cs` | Challenge-response auth (GeneratorX handshake) |
| `GeneratorProtocol` | `Communication/GeneratorProtocol.cs` | Command builders, frequency encoding, response parsing |
| `EncryptionService` | (not used for Hunt & Kill) | S2D file decryption — see DECRYPTION_STATUS.md |
| `DatabaseService` | (not used for Hunt & Kill) | Frequency database loading from .s2d files |
| `PresetService` | (not used for Hunt & Kill) | Preset loading and management |

---

## Building and Running

Prerequisites: .NET 10 SDK, Avalonia templates (optional for IDE).

```sh
# Build all projects
dotnet build Spooky2.sln

# Run the application
dotnet run --project src/Spooky2.App

# Run all tests
dotnet test tests/Spooky2.Services.Tests

# Run a specific test class
dotnet test --filter "ClassName=HuntAndKillReplayTests"
```

All commands run from the `Spooky2Net/` directory.

---

## Test Coverage

The test project covers:

| Test file | What it tests |
|-----------|---------------|
| `AuthenticationTests.cs` | Echo + token computation for all 10 handshake dumps |
| `HuntAndKillReplayTests.cs` | Full DetectHits replay against `Data/FullHuntAndKill` dump |
| `GeneratorProtocolTests.cs` | Command builders, FormatFrequency edge cases |
| `ScanServiceTests.cs` | Phase logic, frequency step calculation |
| `DumpIntegrationTests.cs` | Serial dump parse correctness |

Golden vectors for auth are the 10 real handshake captures in `../Data/Handshake1`..
`Handshake10`. The replay test expects exactly 10 hit frequencies matching a screenshot
of the original Spooky2 software output.

---

## S2D Decryption Status

The `EncryptionService` / `DatabaseService` path for loading `.s2d` frequency databases
is **currently blocked**. The seed-pass accumulator diverges from the expected value
despite all constants and algorithm structure being verified from binary disassembly.

This does **not affect Hunt & Kill**. The scan engine, auth, and protocol services
work independently of the database. See `DECRYPTION_STATUS.md` for full details.

---

## Role as Reference Spec

The Android port (`spooky2-huntkill-android/`) is a direct port of:

- `GeneratorAuthentication.cs` → `core/auth/GeneratorAuthentication.kt`
- `GeneratorProtocol.cs` → `core/protocol/GeneratorProtocol.kt`
- `ScanService.cs` → `core/scan/ScanEngine.kt`

The shared dump files (`../Data/`) are used as test resources in both projects.
The Android golden tests assert bit-identical IEEE-754 results to the C# tests.
A full implementation contract is in `docs/HUNT_AND_KILL_SPEC.md`.
