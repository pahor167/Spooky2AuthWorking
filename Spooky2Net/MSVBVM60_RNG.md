# MSVBVM60.DLL RNG Disassembly

## Function Locations

DLL: `Binaries/MSVBVM60.DLL` (image base 0x66000000)

| Ordinal | Name | RVA | File Offset | Description |
|---------|------|-----|-------------|-------------|
| 593 | rtcRandomNext | 0x4871A | 0x4871A | VB6 `Rnd()` function |
| 594 | rtcRandomize | 0x486B1 | 0x486B1 | VB6 `Randomize` statement |

### Helper Functions

| VA | Description |
|----|-------------|
| 0x6601DFF2 | Get per-project RNG state pointer (returns ptr in eax, seed at [eax+4]) |
| 0x66005511 | GetLocalTime -> compute time value for no-arg Randomize |
| 0x660F0060 | Variant to Single conversion |
| 0x660F00AE | Variant to Double conversion |
| 0x660EDF4A | Randomize mixer (with-argument path) |
| 0x66106A18 | __vbaFpI4 (CLng) - ordinal 125 |
| 0x66112045 | __vbaFpUI1 (CByte from FPU) - ordinal 128 |
| 0x6603F7E1 | __vbaPowerR8 (exponentiation) - ordinal 354 |

## rtcRandomNext (Rnd) - Ordinal 593

### Dispatcher (0x6604871A)

```asm
; Entry: [esp+4] = VARIANT* parameter
mov   ecx, [esp+4]       ; ecx = variant ptr
cmp   word ptr [ecx], 0Ah ; VT_ERROR?
jne   has_argument         ; no -> convert argument
cmp   dword ptr [ecx+8], 80020004h ; DISP_E_PARAMNOTFOUND?
jne   has_argument
; Missing argument: use 1.0 as default
or    eax, -1              ; eax = -1 (flag: no argument)
test  ax, ax               ; AX != 0
je    ...                   ; not taken
fld1                       ; ST0 = 1.0
push  ecx
fstp  dword ptr [esp]      ; store as float32
call  inner_rnd            ; call with arg = 1.0f
ret   4

has_argument:
  xor   eax, eax           ; eax = 0
  ; -> falls through to convert variant to Single, then call inner_rnd
```

### Inner Rnd Function (0x6604874F)

```asm
push  ebp
mov   ebp, esp
push  ecx                  ; local var [ebp-4]
push  ecx                  ; local var [ebp-8]
push  esi
call  get_rng_state         ; -> eax = state ptr, [eax+4] = seed

fld   dword ptr [ebp+8]    ; ST0 = float argument
fcomp qword ptr [0x660417F8] ; compare with 0.0
mov   esi, eax              ; esi = state ptr
mov   ecx, [esi+4]          ; ecx = current seed
fnstsw ax
sahf
je    return_current        ; arg == 0: return current seed

; Reload and compare again for negative check
fld   dword ptr [ebp+8]
fcomp qword ptr [0x660417F8]
mov   edx, 0FFFFFFh         ; 24-bit mask
fnstsw ax
sahf
jb    negative_handler      ; arg < 0: reseed

; arg > 0: just advance LCG
lcg_step:
  imul  ecx, ecx, 2BC03h    ; ecx *= 0x2BC03
  mov   eax, 0FFC39EC3h
  sub   eax, ecx             ; eax = 0xFFC39EC3 - ecx
  and   eax, edx             ; eax &= 0xFFFFFF
  mov   ecx, eax

return_current:
  and   dword ptr [ebp-4], 0 ; high dword = 0
  mov   [ebp-8], ecx         ; low dword = seed
  fild  qword ptr [ebp-8]    ; load seed as int64
  mov   [esi+4], ecx         ; store seed back
  pop   esi
  fmul  dword ptr [0x66048890] ; * float32(1/2^24 = 5.96e-8)
  leave
  ret   4

negative_handler:           ; at 0x6608C089
  mov   ecx, [ebp+8]        ; ecx = raw float32 bits of argument
  shr   ecx, 18h            ; ecx = top byte (bits 24-31)
  add   ecx, [ebp+8]        ; ecx += raw float32 bits
  and   ecx, edx            ; ecx &= 0xFFFFFF
  jmp   lcg_step             ; continue to LCG
```

### Algorithm Summary

```
Rnd(number):
  if number < 0:
    raw = float32_bits(number)
    seed = (raw + (raw >> 24)) & 0xFFFFFF
  if number == 0:
    return seed / 16777216.0  (no state change)
  # LCG step (for number > 0, number < 0, or no argument):
  seed = (seed * 0x43FD43FD + 0xC39EC3) & 0xFFFFFF
  return seed / 16777216.0
```

**LCG equivalence:** The DLL computes `(0xFFC39EC3 - seed * 0x2BC03) & 0xFFFFFF`, which is mathematically equivalent to `(seed * 0x43FD43FD + 0xC39EC3) & 0xFFFFFF` modulo 2^24, because `0xFD43FD = 0x1000000 - 0x2BC03`.

**Default seed:** 0x50000 (327680)

**Verification:** `Rnd()` with default seed produces 0.7055475 (seed becomes 0xB49EC3).

## rtcRandomize (Randomize) - Ordinal 594

### Dispatcher (0x660486B1)

```asm
; Entry: [esp+4] = VARIANT* parameter
mov   ecx, [esp+4]
cmp   word ptr [ecx], 0Ah    ; VT_ERROR?
jne   has_argument
cmp   dword ptr [ecx+8], 80020004h
jne   has_argument
; No argument: use system time
or    eax, -1
test  ax, ax                  ; AX != 0
je    ...                     ; not taken
call  internal_randomize_noarg ; uses GetLocalTime
ret   4

has_argument:
  xor   eax, eax              ; eax = 0
  jmp   test_ax
test_ax:
  test  ax, ax                ; AX == 0
  je    convert_and_mix        ; -> convert variant to double, apply mixer
```

### No-Argument Path (0x660486E0)

```asm
push  ebp
mov   ebp, esp
push  ecx
call  0x66005511              ; GetLocalTime -> time value on FPU
fstp  dword ptr [ebp-4]       ; store as FLOAT32 (4 bytes!)
call  get_rng_state           ; eax = state ptr
; Mixer using raw float32 bytes:
mov   ecx, [ebp-4]            ; ecx = raw float32 bits
mov   edx, ecx
and   ecx, 0FFFFh             ; lower 16 bits
shr   edx, 8                  ; >> 8
and   edx, 0FFFF00h           ; bits 8-23
shl   ecx, 8                  ; << 8
xor   edx, ecx               ; mix
mov   ecx, [eax+4]            ; current seed
and   ecx, 0FF0000FFh         ; keep bits 0-7 (and 24-31, but they're 0)
or    edx, ecx                ; merge
mov   [eax+4], edx            ; store new seed
leave
ret
```

### With-Argument Path (0x660EDF4A)

```asm
; Called with double on stack: [esp+4]=low, [esp+8]=high
call  get_rng_state           ; eax = state ptr
mov   ecx, [esp+8]            ; ecx = UPPER 4 bytes of double
mov   edx, ecx
and   edx, 0FFFFh             ; lower 16 bits of upper dword
shr   ecx, 8
shl   edx, 8
and   ecx, 0FFFF00h
xor   edx, ecx
mov   ecx, [eax+4]            ; current seed
and   ecx, 0FF0000FFh         ; keep bits 0-7
or    edx, ecx
mov   [eax+4], edx            ; store new seed
ret   8
```

### Algorithm Summary

```
Randomize(number):
  upper = upper_4_bytes_of_double(number)
  lValue = ((upper & 0xFFFF) << 8) XOR ((upper >> 8) & 0xFFFF00)
  seed = (seed & 0xFF) | lValue
```

**Key insight:** Only bits 8-23 of the seed are modified. Bits 0-7 are preserved from the old seed.

**Equivalence with .NET:** The .NET formula `((x & 0xFFFF) ^ (x >> 16)) << 8` produces the same result for the bits that matter (8-23), since:
- `((x & 0xFFFF) << 8) XOR ((x >> 8) & 0xFFFF00)` (DLL)
- `(((x & 0xFFFF) XOR (x >> 16)) << 8)` (.NET)
These are mathematically equivalent for the output bits 8-23.

## __vbaFpI4 (CLng) - Ordinal 125

```asm
; At 0x66106A18
frndint                   ; round ST(0) to integer using FPU rounding mode
sub   esp, 0Ch
fist  dword ptr [esp]     ; store as int32
fistp qword ptr [esp+4]   ; store as int64, pop
fnstsw ax
test  al, 0Dh             ; check for exceptions
jne   error
pop   eax                 ; return int32 in eax
add   esp, 8
ret
```

Uses FRNDINT which respects the FPU control word rounding mode. VB6 default is **round-to-nearest-even** (banker's rounding, RC=00).

## __vbaFpUI1 (CByte) - Ordinal 128

```asm
; At 0x66112045
frndint                   ; same rounding
sub   esp, 0Ch
fist  dword ptr [esp]
fistp qword ptr [esp+4]
fnstsw ax
test  al, 0Dh
jne   error
pop   eax
add   esp, 8
test  eax, 0FFFFFF00h     ; check byte range
jne   overflow_error
ret
```

Same as CLng but validates the result fits in a byte (0-255).

## __vbaPowerR8 - Ordinal 354

Core computation at 0x6603F87C:

```asm
; Ensure FPU is in 80-bit extended precision mode (FPCW = 0x27F)
fnstcw [esp]
cmp   word ptr [esp], 027Fh
je    skip_set
call  set_cw_to_27F

; Compute base^exponent using x87:
fyl2x                     ; ST(0) = exponent * log2(base)
call  exp2_function        ; ST(0) = 2^(result)

; exp2 implementation:
;   fld   st(0)            ; duplicate
;   frndint                ; integer part
;   fsubr st(1), st(0)     ; fractional = x - int(x)
;   fxch  st(1)
;   fchs                   ; negate fractional
;   f2xm1                  ; 2^frac - 1
;   fld1
;   faddp                  ; 2^frac
;   fscale                 ; * 2^int_part
;   fstp  st(1)            ; clean up

; Restore original FPU control word
```

The power function temporarily sets the FPU to 80-bit extended precision (FPCW=0x27F), computes the result, then restores the original control word.

## Python Implementation

See `/tmp/vb6_rng.py` for a complete Python implementation with:
- `VB6Rng` class (Rnd and Randomize)
- `rndcrypt_level2_decrypt()` function
- Test suite verifying Rnd() = 0.7055475

## Verification Status

| Test | Status |
|------|--------|
| Rnd() default seed = 0.7055475 | PASS |
| Rnd(-1) determinism | PASS |
| Rnd(0) returns current | PASS |
| Randomize formula matches DLL | PASS |
| LCG formula matches DLL | PASS |
| .s2d line 26 decryption | FAIL - see notes |

### Decryption Issue

The RNG implementation (Rnd/Randomize) is verified correct against the DLL disassembly. However, the full RndCryptLevel2 decryption of .s2d files does not produce the expected plaintext. The most likely cause is a subtle difference in the `pow()` function between the x87 FPU (80-bit extended precision with FYL2X+F2XM1 instructions) and Python's `math.pow()` (which uses the platform C library, typically with 64-bit SSE2 on modern systems).

The seed derivation loop runs 101 iterations, each calling `pow(base, exponent)` and rounding to the nearest integer. Even a single-bit difference in any intermediate `pow()` result that crosses an integer boundary would cascade through all subsequent iterations via the accumulator, producing a completely different RNG state for the data decryption phase.

**To resolve:** The decryption needs to run on a system with an x87 FPU (native Windows x86) or use an x87-compatible power function implementation. Alternatively, running the .NET `Microsoft.VisualBasic.VBMath` on Windows with the DecryptTest program should produce correct results, as the .NET runtime matches the native VB6 behavior.

### Constants from Spooky.exe

| Address | Type | Value | Hex | Usage |
|---------|------|-------|-----|-------|
| 0x414FD8 | double | 2.7526486955 | 8165e4ad6c050640 | Exponent multiplier in Power calls |
| 0x4059E8 | double | 1.0 | 000000000000f03f | Added to exponent |
| 0x406550 | float32 | 1000.0 | 00007a44 | Seed loop: Rnd() * 1000 |
| 0x414FC8 | float32 | 255.0 | 00007f43 | Data loop: Power base scaling |
| 0x414FD0 | double | 255.49 | 48e17a14aeef6f40 | Data loop: XOR byte scaling |
