# RndCryptLevel2 - Complete Disassembly and Pseudocode

## Function Signature (VB6)

```vb
Function RndCryptLevel2(strValue As String, strPassword As String, _
    bSeedPasses As Byte, bDataPasses As Byte, _
    InputType As Long, OutputType As Long, _
    RemoveInvalidChars As String) As String
```

Called with: `RndCryptLevel2(strValue, password, 1, 1, 3, 1, True)`

## Binary Location

- VA: 0x008B8F70 to 0x008B9C60
- File offset: 0x004B8F70 to 0x004B9C60
- Size: ~3312 bytes

## Stack Frame Layout

| Offset | Type | Name | Description |
|--------|------|------|-------------|
| [ebp+0x08] | Object | Me | Class instance |
| [ebp+0x0C] | BSTR* | strValue | Input string (encrypted data) |
| [ebp+0x10] | BSTR* | strPassword | Password string |
| [ebp+0x14] | Byte* | bSeedPasses | Number of seed passes (=1) |
| [ebp+0x18] | Byte* | bDataPasses | Number of data passes (=1) |
| [ebp+0x1C] | Long* | InputType | 1=StrConv, 2=Hex, 3=Base64 |
| [ebp+0x20] | Long* | OutputType | 1=StrConv(vbUnicode), 2=Hex, 3=Base64 |
| [ebp+0x24] | BSTR* | RemoveInvalidChars | Filter flag |
| [ebp+0x28] | BSTR* | Return value | Output string |
| [ebp-0x18] | BSTR | result | Output string |
| [ebp-0x20] | Long | dataUBound | UBound of data array |
| [ebp-0x24] | Ptr | pwAryData | SafeArrayAccessData result for pw output array |
| [ebp-0x28] | Long | totalCount/temp | totalCount, then power results |
| [ebp-0x2C] | SAFEARRAY* | inputData | Processed input (hex decode path) |
| [ebp-0x30] | Long | passCounter | Outer loop counter (data passes) |
| [ebp-0x38] | SAFEARRAY* | dataArray | Encrypted byte array |
| [ebp-0x3C] | SAFEARRAY* | outArray | Output/working byte array |
| [ebp-0x40] | Byte | keyByte | CByte(acc AND 0xFF) |
| [ebp-0x44] | Long | acc | Accumulator |
| [ebp-0x48] | SAFEARRAY* | pwBytes | Password as byte array |
| [ebp-0x5C] | VARIANT | var1 | Temporary variant (16 bytes) |
| [ebp-0x6C] | VARIANT | var2 | Temporary variant (16 bytes) |
| [ebp-0x90] | VARIANT | var3 | Temporary variant (16 bytes) |
| [ebp-0xA4] | Single | rnd1 | Stored Rnd() result |
| [ebp-0xA8] | Single | rnd2 | Stored Rnd() result |
| [ebp-0xD8] | Long | seedLoopBound | Seed loop upper bound |
| [ebp-0xE8] | Long | passLimit | bDataPasses - 1 |
| [ebp-0x11C] | Long | dataIdx | Array index for reading |
| [ebp-0x120] | Long | outIdx | Array index for writing |

## Floating-Point Constants

| Address | Type | Value | Usage |
|---------|------|-------|-------|
| [0x414FD8] | Double | 2.7526486955 | Exponent multiplier in Power calls |
| [0x4059E8] | Double | 1.0 | Added to exponent |
| [0x406550] | Single | 1000.0 | Seed loop: Rnd() * 1000 |
| [0x414FC8] | Single | 255.0 | Data loop: Rnd() * 255 (Power base) |
| [0x414FD0] | Double | 255.49 | Data loop: Rnd() * 255.49 (XOR byte) |

## IAT Calls Used

| IAT Address | Function | VB6 Equivalent |
|-------------|----------|----------------|
| [0x4010B8] | rtcRandomNext | Rnd() |
| [0x4010C4] | rtcRandomize | Randomize() |
| [0x40129C] | __vbaPowerR8 | base ^ exponent |
| [0x4012EC] | __vbaFpI4 | CLng() (FPU to Int32, banker's rounding) |
| [0x401180] | __vbaFpUI1 | CByte() from FPU (reads ST0, returns byte in AL) |
| [0x4011D0] | __vbaUI1I4 | CByte() from Int32 (ecx -> al) |
| [0x4011A4] | __vbaRedimPreserve | ReDim Preserve |
| [0x40122C] | __vbaAryLock | SafeArrayAccessData |
| [0x401144] | __vbaAryBoundsCheck | Bounds check |
| [0x401220] | __vbaVarStrConv | StrConv (variant) |
| [0x40102C] | __vbaFreeVar | Free variant |
| [0x40104C] | __vbaFreeVarList | Free multiple variants |
| [0x401304] | __vbaStrMove | String move |
| [0x401038] | __vbaStrCat | String concatenation |

## Detailed Disassembly Trace

### Phase 1: Input Processing (0x8B9052 - 0x8B93FD)

```
switch (InputType):
  case 1: StrConv(strValue, vbFromUnicode) -> dataArray     ; VA 0x8B93B1
  case 2: Hex decode with RemoveInvalidChars processing      ; VA 0x8B90A8
  case 3: Me.DecodeBase64(strValue, RemoveInvalidChars) -> dataArray  ; VA 0x8B9068
```

For InputType=3 (Base64), the decoded bytes go into `[ebp-0x38]` (dataArray).

### Phase 2: Password Conversion (0x8B93FD - 0x8B944D)

```
At 0x8B93FD:
  pwBytes = StrConv(strPassword, vbFromUnicode)    ; 0x80 = 128 = vbFromUnicode
  ; Converts Unicode string to ANSI byte array
  ; Stored in [ebp-0x48]
```

### Phase 3: Array Setup (0x8B944F - 0x8B94A0)

```asm
; Get data array info
0x8B944F: mov edx, [ebp-0x38]     ; edx = dataArray
0x8B9458: push edx
0x8B9459: push 1
0x8B945B: call [0x40122C]         ; __vbaAryLock -> eax = UBound/info
0x8B945F: mov edi, eax            ; edi = data array bound value

; ReDim output array to match
0x8B9461-0x8B9474: __vbaRedimPreserve(0x80, 1, &outArray, VT_UI1, 1, edi, 0)
0x8B9471: mov [ebp-0x20], edi     ; dataUBound saved

; Lock output array
0x8B947A-0x8B9483: __vbaAryLock(outArray, 1) -> [ebp-0x24]

; Lock password bytes
0x8B9485-0x8B948E: __vbaAryLock(pwBytes, 1) -> eax
0x8B9493: mov esi, eax
0x8B9495: add esi, 1              ; esi = pwLen (UBound + 1)

; Get bSeedPasses
0x8B9490: mov ecx, [ebp+0x14]
0x8B949E: xor eax, eax
0x8B94A0: mov al, [ecx]           ; al = bSeedPasses (=1)
```

### Phase 4: Compute totalCount (0x8B94A2 - 0x8B94CE)

```asm
0x8B94B4: imul eax, esi           ; eax = bSeedPasses * pwLen
0x8B94BD: add edi, 1              ; edi = dataUBound + 1 = dataLen
0x8B94C6: add eax, edi            ; eax = bSeedPasses * pwLen + dataLen
0x8B94CE: mov [ebp-0x28], eax     ; totalCount = bSeedPasses * pwLen + dataLen
```

For our test case: totalCount = 1 * 63 + 38 = 101

### Phase 5: Initialize RNG (0x8B94D1 - 0x8B94F8)

```asm
; Rnd(-1) - reset to reproducible sequence
0x8B94A6: mov [ebp-0x54], -1            ; Variant value = -1
0x8B94AD: mov [ebp-0x5C], 2             ; VT_I2
0x8B94D1: call [0x4010B8]               ; Rnd(-1)
0x8B94D7: fstp st(0)                    ; discard result

; Randomize(totalCount)
0x8B94E4: lea eax, [ebp-0x28]           ; &totalCount
0x8B94EE: mov [ebp-0x90], 0x4003        ; VT_BYREF | VT_I4
0x8B94F8: call [0x4010C4]               ; Randomize(totalCount)

; Save totalCount as seed loop bound
0x8B94FE: mov edx, [ebp-0x28]
0x8B9501: mov [ebp-0xD8], edx           ; seedLoopBound = totalCount
```

### Phase 6: Seed Derivation Loop (0x8B9507 - 0x8B9637)

Loop: `For edi = 1 To totalCount`

```asm
; --- Rnd() call ---
0x8B951C: mov [ebp-0x54], 0x80020004    ; VT_ERROR, DISP_E_PARAMNOTFOUND
0x8B9523: mov [ebp-0x5C], 0x0A          ; vbError (=missing optional param)
0x8B952A: call [0x4010B8]               ; Rnd() with no argument -> next random
0x8B9530: fstp [ebp-0xA4]              ; rnd1 stored as Single

; --- Password byte access ---
; index = edi Mod esi  (i Mod pwLen)
0x8B9543: mov eax, edi                  ; eax = i
0x8B9545: cdq
0x8B9546: idiv esi                      ; edx = i Mod pwLen
0x8B9548: sub edx, [ecx+0x14]          ; edx -= lBound (=0)
; Read byte from password array:
0x8B9572: fld [ebp-0xA4]               ; ST0 = rnd1
0x8B9578: mov ecx, [ecx+0x0C]          ; ecx = pvData
0x8B957B: fmul qword ptr [0x414FD8]    ; ST0 = rnd1 * 2.7526486955
0x8B9584: fadd qword ptr [0x4059E8]    ; ST0 += 1.0 (exponent ready)
0x8B9594: fstp qword ptr [esp]         ; store exponent to stack

; Load password byte value
0x8B9597: xor eax, eax
0x8B9599: mov al, [ecx+edx]            ; al = pwByte = pwBytes(i Mod pwLen)
0x8B959C: mov [ebp-0x110], eax         ; store as Int32
0x8B95A2: fild [ebp-0x110]             ; ST0 = CDbl(pwByte)
0x8B95A8: fstp qword ptr [ebp-0x118]   ; store as Double

; Push base (pwByte as Double) for Power call
0x8B95AE: mov ecx, [ebp-0x114]         ; high dword
0x8B95B4: mov edx, [ebp-0x118]         ; low dword
0x8B95BA: push ecx                      ; push high(base)
0x8B95BB: push edx                      ; push low(base)

; __vbaPowerR8(base=pwByte, exponent=rnd1*2.7526486955+1.0)
0x8B95BC: call [0x40129C]              ; Power(pwByte, rnd1*2.7526+1)
; Result in ST0

; Convert to Long
0x8B95C2: call [0x4012EC]              ; __vbaFpI4 -> eax = CLng(result)
0x8B95CB: mov [ebp-0x28], eax          ; powerResult saved

; --- Second Rnd() call ---
0x8B95D4-0x8B95E2: setup missing param variant
0x8B95E2: call [0x4010B8]              ; rnd2 = Rnd()
0x8B95E8: fstp [ebp-0xA4]             ; rnd2 stored

; Scale rnd2
0x8B95EE: fld [ebp-0xA4]              ; ST0 = rnd2
0x8B95F4: fmul dword ptr [0x406550]    ; ST0 = rnd2 * 1000.0
0x8B9604: call [0x4012EC]              ; eax = CLng(rnd2 * 1000)

; --- Accumulator update ---
0x8B960A: mov ecx, [ebp-0x44]          ; ecx = acc
0x8B960D: mov edx, [ebp-0x28]          ; edx = powerResult
0x8B9610: and ecx, 0x3FFFFFFF          ; ecx = acc AND &H3FFFFFFF
0x8B9616: add ecx, edx                 ; ecx = (acc AND &H3FFFFFFF) + powerResult
0x8B961E: or eax, ecx                  ; eax = CLng(rnd2*1000) OR above
0x8B9623: mov [ebp-0x44], eax          ; acc = result

; --- Loop increment ---
0x8B9628: mov eax, 1
0x8B962D: add eax, edi                 ; eax = i + 1
0x8B9635: mov edi, eax                 ; i = i + 1
0x8B9637: jmp 0x8B950C                 ; back to loop test
```

**Seed Loop Pseudocode:**
```
acc = 0
For i = 1 To totalCount
    rnd1 = Rnd()
    pwByte = pwBytes(i Mod pwLen)
    exponent = CDbl(rnd1) * 2.7526486955 + 1.0
    powerResult = CLng(CDbl(pwByte) ^ exponent)
    rnd2 = Rnd()
    rnd2val = CLng(CDbl(rnd2) * 1000.0)
    acc = rnd2val Or ((acc And &H3FFFFFFF) + powerResult)
Next
```

### Phase 7: Re-initialize RNG (0x8B963C - 0x8B9675)

```asm
; Rnd(-1) - reset
0x8B9640: mov [ebp-0x54], -1
0x8B9647: mov [ebp-0x5C], 2              ; VT_I2, value = -1
0x8B964E: call [0x4010B8]                ; Rnd(-1)
0x8B9654: fstp st(0)                     ; discard

; Randomize(acc)
0x8B9661: lea eax, [ebp-0x44]            ; &acc
0x8B966B: mov [ebp-0x90], 0x4003         ; VT_BYREF | VT_I4
0x8B9675: call [0x4010C4]                ; Randomize(acc)
```

Note: acc is passed directly -- no masking. The VB6 Randomize function receives the full Long value.

### Phase 8: Data Decryption Loop (0x8B967B - 0x8B9815)

Loop: `For esi = 0 To dataUBound`

```asm
; --- keyByte from accumulator ---
0x8B9689: mov ecx, eax                   ; ecx = acc
0x8B968B: and ecx, 0xFF                  ; ecx = acc AND &HFF
0x8B9691: call [0x4011D0]                ; al = CByte(ecx)
0x8B969B: mov [ebp-0x40], al             ; keyByte = CByte(acc AND &HFF)

; --- Rnd() ---
0x8B96AC: call [0x4010B8]                ; rnd1 = Rnd()
0x8B96B2: fstp [ebp-0xA4]               ; store rnd1

; --- Data array access: dataArray(i) ---
; Bounds check on [ebp-0x38] (input data)
; edi = esi - lBound -> [ebp-0x11C] = dataIdx

; --- Output array access: outArray(i) ---
; Bounds check on [ebp-0x3C] (output)
; edi = esi - lBound -> edi = outIdx

; --- XOR operation ---
0x8B9718: fld [ebp-0xA4]                 ; ST0 = rnd1
0x8B971E: fmul qword ptr [0x414FD0]     ; ST0 = rnd1 * 255.49
0x8B972E: call [0x401180]                ; al = CByte(rnd1 * 255.49)
                                          ; (reads ST0, rounds, returns byte)

0x8B9734: mov ecx, [ebp-0x38]            ; ecx = dataArray
0x8B9737: mov edx, [ecx+0x0C]           ; edx = pvData
0x8B973A: mov ecx, [ebp-0x11C]          ; ecx = dataIdx
0x8B9740: xor al, [edx+ecx]             ; al ^= dataArray(dataIdx)
0x8B9743: mov cl, [ebp-0x40]            ; cl = keyByte
0x8B9749: xor al, cl                     ; al ^= keyByte
0x8B974B: mov ecx, [edx+0x0C]           ; ecx = output pvData (via [ebp-0x3C])
0x8B974E: mov [ecx+edi], al             ; outArray(outIdx) = result

; --- Power for accumulator update ---
; rnd3 = Rnd()
0x8B9769: call [0x4010B8]                ; rnd3
0x8B976F: fstp [ebp-0xA4]

; rnd4 = Rnd()
0x8B9783: call [0x4010B8]                ; rnd4
0x8B9789: fstp [ebp-0xA8]

; Compute exponent = rnd4 * 2.7526486955 + 1.0
0x8B978F: fld [ebp-0xA8]                ; ST0 = rnd4
0x8B9798: fmul qword ptr [0x414FD8]     ; * 2.7526486955
0x8B97A1: fadd qword ptr [0x4059E8]     ; + 1.0
0x8B97B1: fstp qword ptr [esp+8]        ; store exponent

; Compute base = rnd3 * 255.0
0x8B97B5: fld [ebp-0xA4]                ; ST0 = rnd3
0x8B97BB: fmul dword ptr [0x414FC8]     ; * 255.0
0x8B97CB: fstp qword ptr [esp]          ; store base

; Power(base, exponent)
0x8B97CE: call [0x40129C]               ; __vbaPowerR8
0x8B97D4: call [0x4012EC]               ; __vbaFpI4 -> eax = CLng(result)
0x8B97E4: mov [ebp-0x28], eax           ; powerResult

; --- Accumulator update ---
0x8B97ED: mov eax, [ebp-0x44]           ; eax = acc
0x8B97F0: mov edi, [ebp-0x28]           ; edi = powerResult
0x8B97F3: cdq                            ; sign extend
0x8B97F4: sub eax, edx                  ; handle negative (acc + signbit)
0x8B97F6: sar eax, 1                    ; arithmetic right shift = acc \ 2
0x8B97FB: add eax, edi                  ; eax = (acc \ 2) + powerResult
0x8B9810: mov [ebp-0x44], eax           ; acc = result

; --- Loop increment ---
0x8B9803: mov ecx, 1
0x8B9808: add ecx, esi                  ; ecx = i + 1
0x8B9813: mov esi, ecx                  ; i++
0x8B9815: jmp 0x8B9680                  ; back to loop
```

**Data Loop Pseudocode:**
```
For i = 0 To dataUBound
    keyByte = CByte(acc And &HFF)
    rnd1 = Rnd()
    rndByte = CByte(CSng(rnd1) * 255.49)   ' reads FPU, rounds to byte
    outArray(i) = rndByte Xor dataArray(i) Xor keyByte

    rnd3 = Rnd()
    rnd4 = Rnd()
    base = CDbl(rnd3) * 255.0
    exponent = CDbl(rnd4) * 2.7526486955 + 1.0
    powerResult = CLng(base ^ exponent)
    acc = (acc \ 2) + powerResult
Next
```

### Phase 9: Additional Data Passes (0x8B981A - 0x8B99FF)

Only runs when bDataPasses > 1 (not the case for .s2d files where bDataPasses=1).

```
For pass = 1 To bDataPasses - 1
    For i = 0 To dataUBound
        ' Same XOR operation but reads/writes outArray only (no dataArray)
        keyByte = CByte(acc And &HFF)
        rnd1 = Rnd()
        rndByte = CByte(CSng(rnd1) * 255.49)
        outArray(i) = rndByte Xor outArray(i) Xor keyByte

        ' Same accumulator update
        rnd3 = Rnd()
        rnd4 = Rnd()
        powerResult = CLng((CDbl(rnd3)*255.0) ^ (CDbl(rnd4)*2.7526486955+1.0))
        acc = (acc \ 2) + powerResult
    Next i
Next pass
```

### Phase 10: Output Processing (0x8B9A04+)

```
switch (OutputType):
  case 1: result = StrConv(outArray, vbUnicode)     ; 0x40 = 64   -> VA 0x8B9B7A
  case 2: Hex encode with lookup table               -> VA 0x8B9A56
  case 3: Me.EncodeBase64(outArray) -> result         -> VA 0x8B9A1A
```

For OutputType=1: `StrConv(outArray, vbUnicode)` converts ANSI bytes to Unicode string.

## Complete Algorithm Summary

```
Function RndCryptLevel2(data, password, seedPasses=1, dataPasses=1):
    pwBytes = StrConv(password, vbFromUnicode)  ' ANSI byte array
    pwLen = UBound(pwBytes) + 1                  ' = Len(password)
    dataLen = UBound(data) + 1
    totalCount = seedPasses * pwLen + dataLen

    ' Phase 1: Seed derivation
    Rnd(-1)
    Randomize(totalCount)

    acc = 0
    For i = 1 To totalCount
        rnd1 = Rnd()
        pwByte = pwBytes(i Mod pwLen)
        exponent = CDbl(rnd1) * 2.7526486955 + 1.0
        powerResult = CLng(CDbl(pwByte) ^ exponent)

        rnd2 = Rnd()
        rnd2val = CLng(CDbl(rnd2) * 1000.0)

        acc = rnd2val Or ((acc And &H3FFFFFFF) + powerResult)
    Next

    ' Phase 2: Data decryption (first pass)
    Rnd(-1)
    Randomize(acc)

    ReDim output(0 To UBound(data))
    For i = 0 To UBound(data)
        keyByte = CByte(acc And &HFF)
        rnd1 = Rnd()
        rndByte = CByte(CSng(rnd1) * 255.49)
        output(i) = rndByte Xor data(i) Xor keyByte

        rnd3 = Rnd()
        rnd4 = Rnd()
        powerResult = CLng((CDbl(rnd3)*255.0) ^ (CDbl(rnd4)*2.7526486955+1.0))
        acc = (acc \ 2) + powerResult
    Next

    ' Phase 3: Additional data passes (only if dataPasses > 1)
    For pass = 1 To dataPasses - 1
        For i = 0 To UBound(output)
            keyByte = CByte(acc And &HFF)
            rndByte = CByte(CSng(Rnd()) * 255.49)
            output(i) = rndByte Xor output(i) Xor keyByte

            powerResult = CLng((CDbl(Rnd())*255) ^ (CDbl(Rnd())*2.7526486955+1))
            acc = (acc \ 2) + powerResult
        Next
    Next

    Return StrConv(output, vbUnicode)
End Function
```

## Key Constants Verified from Binary

- `2.7526486955` at VA 0x414FD8 (hex: `8165e4ad6c050640`) - used as exponent multiplier in ALL Power calls
- `1.0` at VA 0x4059E8 - added to exponent
- `1000.0` at VA 0x406550 (hex: `00007a44` as float) - seed loop Rnd scaling
- `255.0` at VA 0x414FC8 (hex: `00007f43` as float) - data loop Power base scaling
- `255.49` at VA 0x414FD0 (hex: `48e17a14aeef6f40`) - data loop XOR byte scaling

## Critical Differences from Initial Reverse Engineering Attempt

1. **Exponent multiplier**: 2.7526486955, NOT 255.49. The 255.49 is only used for the XOR byte.
2. **Seed loop scaling**: `CLng(Rnd() * 1000)`, NOT `CInt(Int(Rnd() * 255))`.
3. **Randomize(acc)**: Full acc value, no masking to 24 bits.
4. **CByte from FPU**: `[0x401180]` reads ST(0) directly (float-to-byte), not Int32-to-byte.
5. **Accumulator integer division**: `acc \ 2` uses `cdq; sub eax,edx; sar eax,1` (VB6 signed integer division rounding toward zero).

## Brute-Force Verification Attempt

A complete brute-force search was performed over all 2^24 possible RNG states (the VB6 Rnd() state space). For each state, the initial `acc` value was solved for bit-by-bit using the keyByte chain constraint. No solution was found that produces the expected plaintext output.

This indicates that one of the following remains unresolved:

1. **x87 FPU 80-bit precision**: VB6 performs all floating-point operations in x87 80-bit extended precision, while Python uses 64-bit double. For `Math.Pow(base, exponent)` where the result can be millions, the rounding difference between 80-bit and 64-bit can change the integer result by 1 or more. This propagates through the accumulator chain and corrupts all subsequent bytes.

2. **Exact Randomize() byte rearrangement**: The MSVBVM60.DLL `rtcRandomize` function's exact byte manipulation of the seed could not be verified without access to the DLL binary or a Windows test environment.

3. **Rnd() float output method**: It is uncertain whether VB6 converts the 24-bit state to float via division by 2^24, or via the IEEE 754 mantissa OR trick (which gives different values for states with bit 23 set).

To fully resolve this, one of these approaches is needed:
- Run the binary under Wine or on Windows to capture the exact Rnd() output sequence
- Disassemble the MSVBVM60.DLL `rtcRandomize` and `rtcRandomNext` functions
- Use a Windows VB6 runtime to generate reference Rnd()/Randomize() sequences for known seeds
