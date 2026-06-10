# Phase 5 — Real-Hardware Verification Checklist

Everything in Phases 0–4 runs and is tested with **no hardware** (dump replay via
`FakeTransport`). Phase 5 swaps in the real USB link and verifies against a physical
**Spooky2 GeneratorX** over **USB-OTG**. The only unverified class is
`transport/.../usb/UsbCdcSerialTransport.kt`; `core` is already proven against the
recorded dumps.

## Prerequisites
- Spooky2 **GeneratorX / GeneratorX Pro** (the 115200-baud, challenge-response device).
- Android device with **USB Host (OTG)** support + an OTG adapter/cable.
- Generator externally powered (do **not** assume the phone can bus-power it).
- App installed (`./gradlew :app:installDebug`).

## Step 1 — Confirm USB enumeration (the key unknown)
Plug the generator into the Android device and inspect what it enumerates as:
```
adb shell dumpsys usb            # look for the attached device
adb shell lsusb                  # if available
```
Capture and record:
- [ ] **Vendor ID** — expected `0x04D8` (Microchip). Confirm.
- [ ] **Product ID** of the *serial* (CDC) interface. The `0x0032` constant in the
      codebase is the **HID** interface — the serial PID may differ. Record the real one.
- [ ] **Interface class** — expected CDC-ACM (Communications / 0x02 + Data 0x0A).
      If it is **not** CDC (e.g. FTDI/Prolific/CP210x), select the matching
      `usb-serial-for-android` driver instead of `CdcAcmSerialDriver`.
- [ ] Number of interfaces (some composite devices expose HID + CDC together).

## Step 2 — Wire the real transport into DI
In `app/.../di/`, bind a `@Usb TransportFactory` that builds `UsbCdcSerialTransport`
(with `UsbManager` + the granted `UsbDevice`) and inject `GeneratorSessionFactory` with
that factory + `connectViaProbe = true`. Add the runtime **USB-permission** flow
(`UsbManager.requestPermission` + `BroadcastReceiver`) in the Connect screen.
- [ ] `@Usb` factory bound; default switched (or a Demo/Live toggle added).
- [ ] USB permission requested and granted before `open()`.
- [ ] Update `usb_device_filter.xml` with the confirmed PID once known.
- [ ] Update `CdcAcmSerialTransport` probe table for VID `0x04D8` (+ PID).

## Step 3 — Verify discovery + auth handshake
- [ ] `GeneratorClient.connect()` probes 57600 then 115200 and identifies the GX.
- [ ] Challenge-response succeeds: app sends `:r90=<challenge>,`, device replies
      `:r90=<echo>,<response>.`, app sends `:w92=<token>.`, device replies `:ok`.
- [ ] Echo verification matches (core `GeneratorAuthentication.computeEcho`).
- [ ] Post-auth init sequence accepted (esp. the CRITICAL `:w15=1,1,` low-freq mode).

## Step 4 — Verify the scan against live sensors
- [ ] Amplitude ramp-up (re-enable `enableAmplitudeRampUp`; demo disables it).
- [ ] Baseline reads (`:r11=`/`:r12=`) return plausible values.
- [ ] Sweep steps program frequency (`:w24=`) and read sensors without timeouts.
- [ ] Hit detection finds reaction peaks on a real subject/load.
- [ ] Kill dwell programs each hit frequency for the configured dwell (default 180s).

## Step 5 — Timing & robustness
- [ ] Tune inter-command delays / read timeouts (`GeneratorClient.delayProvider`,
      `SerialTransport` timeouts) — the recorded dump has no real device latency.
- [ ] Handle USB disconnect / permission-revoke mid-scan (surface error, safety-stop).
- [ ] **Foreground service + wake-lock** so a multi-minute scan survives screen-off /
      Doze (currently `ScanForegroundServiceStub`).
- [ ] Verify safety-stop path clears output (`:w12=`, amplitude ramp-down) on
      cancel / error / app-exit.

## Step 6 — Safety sign-off
- [ ] Output sequences match the desktop app exactly (no behavioral changes).
- [ ] Ramp-up/down and stop-clears-output confirmed on a scope or dummy load before
      any contact use.
- [ ] Disclaimers present; only operate with informed consent.

## Known carry-over
- **Cluster deduplication** is not implemented (matches the C# limitation; replicated,
  not fixed). Hits may cluster in the strongest band. Revisit after HW validation.
