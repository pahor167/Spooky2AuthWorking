# S2D Decryption Status

## What Works
- Generator authentication: SOLVED (10/10 handshakes verified)
- Serial communication: WORKING (both COM3/COM4 authenticated)
- RNG (Rnd/Randomize): VERIFIED against VBScript and MSVBVM60.DLL disassembly
- Password: CONFIRMED from binary ("2020888376Spooky2 (c) John White. http://www.cancerclinic.co.nz")
- All constants: VERIFIED from binary (2.7526486955, 255.49, 1000.0f, 255.0f, 1.0)
- Seed pass algorithm structure: VERIFIED from binary disassembly
- Data pass XOR formula: VERIFIED (`rndByte XOR data[i] XOR keyByte`)

## What's Blocked: S2D File Decryption

### The Problem
Our seed pass produces `acc = 183289807` but the correct value is `acc = 24735680`.

The correct acc was found by brute-forcing all 65536×256 possible seed+keyByte combinations
and finding the one that decrypts the first byte of line 0 to `#` (header marker).

### What's Been Ruled Out
1. **FPU precision**: Tested with mpmath at 64-bit and 128-bit mantissa — zero differences in integer-rounded power results across all 88 iterations
2. **Rounding mode**: Tested round/floor/ceil/trunc for both pow and r2v — no match
3. **Constants**: All 6 constants verified from binary bytes
4. **Overflow**: No integer overflow in any of 88 iterations
5. **Password**: Confirmed from binary analysis
6. **totalCount**: Confirmed as `pwLen + dataLen = 63 + 25 = 88`
7. **InputType=3**: Confirmed to call Base64DecodingB first, then StrConv(vbFromUnicode)

### What's Still Unknown
The seed pass algorithm produces the wrong accumulator value. Since:
- All constants are correct
- Precision is not the issue
- The algorithm structure matches the binary disassembly

There must be a subtle error in how we're interpreting the binary code that causes the
accumulator to diverge. Possibilities:
- The `Or` operation might use different operand sizes (Word vs Long)
- The `And 0x3FFFFFFF` mask might not apply where we think
- The loop counter might be 0-based instead of 1-based
- The password byte index `i Mod pwLen` might use a different modulus

### Brute-Force Approach
Since there are only 65536 possible seeds (Randomize only affects 16 bits of the 24-bit seed),
we CAN decrypt any single line by trying all seeds. But the data pass accumulator uses pow()
which makes subsequent bytes wrong unless we also brute-force each byte's accumulator state.

A full brute-force of all 65536 seeds for each of 11732 lines would be feasible (~770M iterations)
but each line needs the FULL data pass with correct accumulator tracking.

## Files
- `MSVBVM60_RNG.md` — MSVBVM60.DLL Rnd/Randomize disassembly
- `AUTH_INDICES.md` — Authentication algorithm extracted from binary
- `LEVEL2_DISASM.md` — RndCryptLevel2 disassembly notes
- `S2D_DECRYPTION.md` — Earlier decryption analysis
- `CONSTANTS_VERIFY.md` — All constants verified from binary
