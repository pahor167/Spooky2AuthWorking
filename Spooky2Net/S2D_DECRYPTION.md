# S2D File Decryption Analysis

## File Format

- `Frequencies.s2d` contains 11,732 lines
- Each line is a Base64-encoded encrypted string
- When decrypted, each line should produce CSV: `ProgramName,freq1,freq2,...`

## Encryption Password

**Confirmed from binary disassembly** (at VA 0x6E3B5D-0x6E3B88):

```
password = "2020888376" & "Spooky2 (c) John White. http://www.cancerclinic.co.nz"
```

Full password: `"2020888376Spooky2 (c) John White. http://www.cancerclinic.co.nz"` (63 chars)

This is assembled in `Form_Load` at VA 0x6E3B5D:
1. Push string `"2020888376"` (at 0x4A0DCC)
2. Load `VTable_00001BC4` (copyright string at 0x4A0D5C)
3. Call `__vbaStrCat` to concatenate
4. Store at form member offset `0x1B4` via `__vbaStrCopy`

The copyright string stored at 0x4A0D5C is:
`"Spooky2 (c) John White. http://www.cancerclinic.co.nz"` (53 chars, confirmed from binary BSTR)

## EncryptDecrypt Function (VA 0x89D7D0)

`EncryptDecrypt(Text, PW, EncryptStrn)` creates a `clsRndCrypt` COM object and calls `RndCryptLevel2`.

### Parameters for DECRYPT (EncryptStrn=0)

Traced from binary at VA 0x89D89C-0x89D8F8:

```
clsRndCrypt.RndCryptLevel2(
    strValue        = Text,           // the Base64 line from .s2d file
    strPassword     = PW,             // password from [self + 0x1B4]
    bSeedPasses     = 1,              // byte
    bDataPasses     = 1,              // byte
    InputType       = 3,              // Base64 input -> internal Base64DecodingB
    OutputType      = 1,              // StrConv(result, vbUnicode) -> string output
    RemoveInvalidChars = True          // -1 / True
)
```

### Parameters for ENCRYPT (EncryptStrn=FFFFFFh/-1)

```
RndCryptLevel2(Text, PW, 1, 1, 1, 3, True)
    InputType  = 1   // hex string input
    OutputType = 3   // Base64EncodingB output
```

## VB6 PRNG Implementation

From .NET runtime source (`Microsoft.VisualBasic.VBMath`):

### Rnd(Number)

```python
# LCG: seed = (seed * 0x43FD43FD + 0xC39EC3) & 0xFFFFFF
# Returns: seed / 16777216.0
# Default initial seed: 327680 (0x50000)

def rnd(seed, number=None):
    if number is not None and number < 0:
        f32_bytes = struct.pack('<f', float(number))
        raw = struct.unpack('<I', f32_bytes)[0]
        i64 = raw & 0xFFFFFFFF
        seed = (i64 + (i64 >> 24)) & 0xFFFFFF
    if number is None or number != 0:
        seed = ((seed * 0x43FD43FD) + 0xC39EC3) & 0xFFFFFF
    return seed, seed / 16777216.0
```

### Randomize(Number)

```python
def randomize(seed, number):
    dbl_bytes = struct.pack('<d', float(number))
    lValue = struct.unpack('<i', dbl_bytes[4:8])[0]  # UPPER 4 bytes of Double!
    lValue = (((lValue & 0xFFFF) ^ (lValue >> 16)) << 8) & 0xFFFFFFFF
    seed = (seed & 0xFF0000FF) | (lValue & 0x00FFFF00)
    return seed & 0xFFFFFF
```

### IAT Mapping (from PE import table)

```
[0x4010B8] = ordinal 593 = rtcRandomNext (Rnd)
[0x4010C4] = ordinal 594 = rtcRandomize (Randomize)
```

Confirmed by code patterns:
- `call [0x4010B8]` is always followed by `fstp` (saves float return value) = **Rnd**
- `call [0x4010C4]` is never followed by `fstp` = **Randomize**

## RndCryptLevel2 Algorithm (from binary at VA 0x8B8F70)

The algorithm is significantly more complex than simple XOR encryption.

### Phase 1: Input Processing (0x8B93FD)

- Password converted to byte array: `StrConv(password, vbFromUnicode)`
- For InputType=3: `Base64DecodingB(strValue)` -> byte array via vtable[0x2C]

### Phase 2: Initialize (0x8B94A6-0x8B94F8)

```
Rnd(-1)                          // Reset PRNG to deterministic state
total_count = pw_len + data_len  // Sum of password and data byte counts
Randomize(total_count)           // Seed PRNG
```

### Phase 3: Seed Derivation Pass (0x8B9507-0x8B9637)

For `i = 1` to `total_count`:
1. `rnd1 = Rnd()` - random float
2. Password byte: `pw_bytes[i mod pw_len]`
3. Data byte: `data_bytes[i mod data_len]` (with SAFEARRAY bounds check)
4. Exponent: `rnd1 * 255.49 + 1.0` (constant at 0x414FD8=255.49, 0x4059E8=1.0)
5. `power_result = Int(pw_byte ^ exponent)` via `__vbaPowerR8` + `__vbaFpI4`
6. `rnd2 = Rnd()` - second random float
7. `rnd2_scaled = Int(rnd2 * 255.0)` (constant at 0x414FC8=255.0)
8. XOR into result array: `result[j] = CByte(Rnd*255.49) XOR data[j] XOR pw_key`
9. Accumulator update: `acc = rnd2_scaled | ((acc & 0x3FFFFFFF) + power_result)`

### Phase 4: Data Pass Setup (0x8B963C-0x8B967B)

```
Rnd(-1)          // Reset PRNG
Randomize(acc)   // Seed with accumulated value from seed pass
```

### Phase 5: Data Decryption Pass (0x8B967E-0x8B9815)

For `i = 0` to `data_len`:
1. `key_byte = acc & 0xFF` via `__vbaUI1I4`
2. `rnd1 = Rnd()` - random float
3. `rnd_byte = CByte(rnd1 * 255.49)` via `__vbaFpUI1`
4. **XOR**: `result[i] = rnd_byte XOR result[i] XOR key_byte`
5. More `Rnd()` calls for accumulator update with `__vbaPowerR8`
6. Accumulator: `acc = (acc >> 1) + Int(pow(rnd3*255.0, rnd4*255.49+1.0))`

### Phase 6: Output (0x8B9A04)

For OutputType=1: `StrConv(result_bytes, vbUnicode)` -> converts decrypted bytes to Unicode string.

## Key Constants (from binary data section)

| Address  | Value    | Type    | Used For |
|----------|----------|---------|----------|
| 0x414FD8 | 255.49   | double  | XOR key scaling and exponent calculation |
| 0x4059E8 | 1.0      | double  | Exponent offset |
| 0x414FD0 | 255.49   | double  | Data XOR scaling |
| 0x414FC8 | 255.0    | float32 | Accumulator scaling |
| 0x406550 | 1000.0   | float32 | Additional scaling |

## Current Status

The exact byte-level implementation of the seed pass accumulator and the data pass
accumulator update involves `__vbaPowerR8` (exponentiation) which makes the algorithm
overflow-sensitive and precision-dependent. The VB6 runtime's exact handling of
overflow in these power operations (which can produce astronomically large numbers
from `pow(byte_value, float_up_to_256)`) is critical to get right.

### What Works

- Password extraction: confirmed
- VB6 PRNG (Rnd/Randomize): verified against .NET runtime source
- EncryptDecrypt parameter mapping: confirmed from disassembly
- InputType/OutputType semantics: confirmed

### What Remains

- Exact overflow handling in the `__vbaPowerR8` accumulator computation
- Precise interaction between the seed pass XOR and the data pass XOR
- The exact array indexing pattern (1-based vs 0-based boundary in the seed pass)

## Simple Decryption Test Code (Python)

```python
import struct
import base64

class Vb6Rng:
    """Exact VB6 PRNG from .NET runtime source."""

    def __init__(self):
        self._seed = 327680  # 0x50000

    def rnd(self, number=None):
        if number is not None and number != 0:
            if number < 0:
                f32_bytes = struct.pack('<f', float(number))
                raw = struct.unpack('<I', f32_bytes)[0]
                i64 = raw & 0xFFFFFFFF
                self._seed = (i64 + (i64 >> 24)) & 0xFFFFFF
            self._seed = ((self._seed * 0x43FD43FD) + 0xC39EC3) & 0xFFFFFF
        return self._seed / 16777216.0

    def randomize(self, number):
        dbl_bytes = struct.pack('<d', float(number))
        lValue = struct.unpack('<i', dbl_bytes[4:8])[0]
        lValue = (((lValue & 0xFFFF) ^ (lValue >> 16)) << 8) & 0xFFFFFFFF
        self._seed = (self._seed & 0xFF) | (lValue & 0xFFFF00)
        self._seed &= 0xFFFFFF


PASSWORD = "2020888376Spooky2 (c) John White. http://www.cancerclinic.co.nz"


def rndcrypt_b_decrypt(data_bytes: bytes, password: str) -> bytes:
    """Simple RndCryptB decryption (NOT the full Level2 algorithm)."""
    pw = password.encode('ascii')
    rng = Vb6Rng()
    rng.randomize(len(pw))
    h = 0
    for b in pw:
        rv = int(rng.rnd() * 256)
        h += b ^ rv
    rng2 = Vb6Rng()
    rng2.randomize(h)
    result = bytearray(len(data_bytes))
    for i in range(len(data_bytes)):
        rv = int(rng2.rnd() * 256)
        result[i] = data_bytes[i] ^ rv
    return bytes(result)


# Read and attempt decryption
with open("Frequencies.s2d", "r") as f:
    for i, line in enumerate(f):
        if i >= 10:
            break
        b64 = line.strip()
        raw = base64.b64decode(b64)
        decrypted = rndcrypt_b_decrypt(raw, PASSWORD)
        text = decrypted.decode('latin-1', errors='replace')
        print(f"Line {i}: {text[:80]}")
```

Note: This uses simple RndCryptB which is NOT the algorithm used for .s2d files.
The actual algorithm is RndCryptLevel2 with power operations as described above.

## Related Files

- Binary: `/Users/pavelhornak/repo/2/spooky_decompiled_new/Binaries/Spooky.exe`
- Decompiled VB6: `/Users/pavelhornak/repo/2/spooky_decompiled_new/Main.frm`
- Decompiled class: `/Users/pavelhornak/repo/2/spooky_decompiled_new/clsRndCrypt.cls`
- C# encryption service: `/Users/pavelhornak/repo/2/spooky_decompiled_new/Spooky2Net/src/Spooky2.Services/Encryption/EncryptionService.cs`
- .NET test program: `/Users/pavelhornak/repo/2/spooky_decompiled_new/Spooky2Net/DecryptTest/Program.cs`
