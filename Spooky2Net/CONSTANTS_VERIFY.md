# RndCryptLevel2 Seed Pass — Binary Constant Verification

Binary: `Binaries/Spooky.exe`
Function: `RndCryptLevel2` at VA `0x8B8F70` (file offset `0x4B8F70`)

## 1. Verified Constants (ALL CORRECT)

| # | File Offset | Type | Claimed Value | Actual Bytes | Actual Value | Status |
|---|-------------|------|---------------|--------------|--------------|--------|
| 1 | `0x14FD8` | double | 2.7526486955 | `8165e4ad6c050640` | 2.7526486955 | CORRECT |
| 2 | `0x59E8` | double | 1.0 | `000000000000f03f` | 1.0 | CORRECT |
| 3 | `0x6550` | float32 | 1000.0 | `00007a44` | 1000.0 | CORRECT |
| 4 | `0x14FC8` | float32 | 255.0 | `00007f43` | 255.0 | CORRECT |
| 5 | `0x14FD0` | double | 255.49 | `48e17a14aeef6f40` | 255.49 | CORRECT |
| 6 | `0x4B9610` | instruction | `and ecx, 0x3FFFFFFF` | `81e1ffffff3f` | `and ecx, 0x3FFFFFFF` | CORRECT |

**All 6 constants match exactly.** The constants are NOT the source of the bug.

## 2. CRITICAL FINDINGS — Structural Issues

### 2a. totalCount Formula

```
esi = UBound(pw_byte_array) + 1 = pwLen  (NOT pwLen+1)
edi = UBound(data_array) + 1 = dataLen

totalCount = bSeedPasses * pwLen + dataLen
```

The `add esi, 1` at `0x8B9497` adds 1 to `UBound()` (which returns `length-1` for a 0-based array), making `esi = pwLen` (the actual array length), NOT `pwLen + 1`.

With `bSeedPasses=1`, `pwLen=63`, `dataLen=25`: `totalCount = 1*63 + 25 = 88` — **matches the expected 88 iterations**.

### 2b. Password Byte Index (Divisor)

```asm
8B C7          mov eax, edi       ; eax = loop counter (1-based)
99             cdq
F7 FE          idiv esi           ; edx = eax mod esi (esi = pwLen)
2B 51 14       sub edx, [ecx+14h] ; edx -= lLbound (0 for 0-based array)
```

The divisor is `esi = pwLen`. So: `pwIndex = (loop_counter) mod pwLen`.

Loop counter starts at 1, so indices cycle: `1, 2, ..., pwLen-1, 0, 1, 2, ...`

All indices are valid for a 0-based array of length `pwLen`.

### 2c. Or Operation Order — CONFIRMED

```asm
0x8B960A: mov ecx, [ebp-44h]          ; ecx = acc
0x8B960D: mov edx, [ebp-28h]          ; edx = pr (from CLng(pow(...)))
0x8B9610: and ecx, 0x3FFFFFFF         ; mask acc
0x8B9616: add ecx, edx                ; ecx = masked_acc + pr
0x8B961E: or eax, ecx                 ; eax = r2v Or (masked_acc + pr)
0x8B9623: mov [ebp-44h], eax          ; acc = result
```

Order is: `acc = r2v Or ((acc And 0x3FFFFFFF) + pr)` — NOT `(r2v Or masked) + pr`.

### 2d. Rnd() * 1000.0 — Uses float32

```asm
0x8B95F4: D8 0D 50654000    fmul dword ptr [0x00406550]  ; float32 multiply
```

The `D8` opcode prefix means float32 (not `DC` for float64). The 1000.0 constant at `0x406550` is a `float32`. The Rnd() result is also stored/reloaded as `float32` before this multiply.

### 2e. 255.0 (float32) Is NOT Used in the Seed Pass

The constant at `0x414FC8` (255.0 as float32) is only referenced at `0x8B97BB`, which is in the **data pass** (second loop), NOT the seed pass.

In the seed pass, the exponent is:
```
exponent = float32(Rnd()) * 2.7526486955 + 1.0
```

There is NO `/ 255.0` or `* 255.0` in the seed pass exponent calculation.

## 3. MOST CRITICAL FINDINGS — Possible Bugs

### 3a. Randomize Seed = totalCount (NOT password-derived!)

```asm
0x8B94CE: mov [ebp-28h], eax          ; store totalCount
0x8B94D1: call [0x4010B8]             ; Rnd() — DISCARDED (advances state)
0x8B94D7: fstp st(0)                  ; discard Rnd result
... variant setup ...
0x8B94F8: call [0x4010C4]             ; Randomize(totalCount)
```

The `Randomize` call uses `totalCount` as its seed, NOT a password-derived value. Import table confirms:
- `[0x4010B8]` = ordinal 593 = `rtcRandomNext` (Rnd)
- `[0x4010C4]` = ordinal 594 = `rtcRandomize` (Randomize)

This means for the seed pass, the Rnd() sequence is deterministic based on `totalCount`, not on the password.

### 3b. Discarded Rnd() Call Before Randomize

At `0x8B94D1`, `Rnd()` is called and the result is discarded (`fstp st(0)`). This advances the PRNG state by one step, but since `Randomize(totalCount)` follows immediately and reseeds, this discarded call has no effect on the seed pass.

### 3c. TWO Rnd() Calls Per Iteration

Each loop iteration makes TWO `Rnd()` calls:

1. **Rnd() at `0x8B952A`** — stored as float32, used for exponent:
   `exponent = float32(Rnd()) * 2.7526486955 + 1.0`

2. **Rnd() at `0x8B95E2`** — stored as float32, used for r2v:
   `r2v = CLng(float32(Rnd()) * float32(1000.0))`

Both Rnd() values undergo `fstp dword / fld dword` (store then reload as float32), meaning they are truncated to float32 (Single) precision before use.

### 3d. pr Overwrites totalCount Storage

`[ebp-28h]` initially holds `totalCount`, but it gets overwritten each iteration:

```asm
0x8B95CB: mov [ebp-28h], eax   ; pr = CLng(pow(pwByte, exponent))
```

The `totalCount` is separately saved to `[ebp-0D8h]` for the loop bound check. The value read at `0x8B960D` (`mov edx, [ebp-28h]`) is `pr`, not `totalCount`.

## 4. Reconstructed Seed Pass Algorithm

```
Input: password (string), data (byte array), bSeedPasses (byte)
Output: acc (Long/int32)

pwBytes = StrConv(password, vbFromUnicode)  ' ASCII byte array, 0-based
pwLen = UBound(pwBytes) + 1                 ' = Len(password)
dataLen = UBound(data) + 1                  ' = number of data bytes

totalCount = bSeedPasses * pwLen + dataLen

Rnd()                          ' advance state, discard result
Randomize(totalCount)          ' seed the RNG with totalCount

acc = 0

For i = 1 To totalCount
    rndForExponent = float32(Rnd())        ' truncated to Single precision
    exponent = CDbl(rndForExponent) * 2.7526486955# + 1.0#

    pwIndex = i Mod pwLen                  ' 0-based index into pwBytes
    pwByte = pwBytes(pwIndex)              ' byte value 0-255

    pr = CLng(pwByte ^ exponent)           ' pow(pwByte, exponent), then CLng

    rndForR2v = float32(Rnd())             ' truncated to Single precision
    r2v = CLng(CDbl(rndForR2v) * CDbl(float32(1000.0)))  ' CLng of float product

    acc = r2v Or ((acc And &H3FFFFFFF) + pr)
Next i
```

## 5. Summary of Discrepancies vs Implementation

1. **Constants**: All 6 verified constants are CORRECT.
2. **totalCount**: `bSeedPasses * pwLen + dataLen` — verify implementation uses this exact formula.
3. **Randomize seed**: Uses `totalCount`, not a password hash. The current `EncryptionService.cs` implementation is completely wrong — it uses `ComputePasswordHash(password)` and does simple XOR passes instead of the actual pow/Or accumulator algorithm.
4. **Loop counter**: 1-based (`For i = 1 To totalCount`), password index = `i Mod pwLen`.
5. **Two Rnd() calls per iteration**: First for exponent, second for r2v.
6. **Float32 precision**: Both Rnd() values are truncated to float32 via store/reload cycle.
7. **No 255.0 in seed pass**: The 255.0 constant is only used in the data pass, not the seed pass.
