# Spooky2 Authentication Algorithm - Extracted Indices

## Overview

The authentication handshake uses two functions: **Proc_0_353** and **Proc_0_354**.
Both compute 9 output digits using the same formula structure but with different
position index tables and different input strings.

**Key discovery**: There is no fixed "ProtectionVal" position. Instead, each output
digit uses a **dynamic index** determined by the iterator digit value from the
other string. The fild/fadd/fsub operations with constant 2136.0 seen in Proc_0_353
are part of a validation check only, not part of the formula computation.

## Formula

Both functions use the same formula per output digit:

```
output[i] = (S[posB] * S[posC] + S[posA] * S[iter_digit_value]) % 9 + 1
```

Where:
- `S` = the "source" string (challenge for Proc_0_353, device_response for Proc_0_354)
- `posA`, `posB`, `posC` = fixed 1-based position constants (different per digit)
- `iter_digit_value` = integer value of the i-th digit from the "iterator" string
  - For Proc_0_353: iterator string = device_response (arg_10)
  - For Proc_0_354: iterator string = challenge (arg_C)
- `S[iter_digit_value]` = the digit from the source string at position `iter_digit_value`
- All positions are 1-based

The `% 9 + 1` ensures output digits are always in range 1-9.

## Binary Details

- **Modulo divisor**: 9 (confirmed: `mov ecx, 9` = `B9 09 00 00 00` before each `idiv ecx`)
- **+1 offset**: Confirmed: `add edx, 1` = `83 C2 01` after each `idiv ecx`
- **Proc_0_353**: VA 0x898BE0, file offset 0x498BE0
- **Proc_0_354**: VA 0x89A5F0, file offset 0x49A5F0

## Proc_0_353: Echo Computation

**Inputs**: challenge (arg_C), device_response (arg_10)
**Output**: echo string (9 digits, stored in var_1C)

Iterator reads sequentially from **device_response**: `Mid$(arg_10, i+1, 1)` for i=0..8
Formula positions read from **challenge**: `Mid$(arg_C, pos, 1)`
Dynamic index: `challenge[device_response_digit_value - 1]`

```
echo[i] = (C[posB] * C[posC] + C[posA] * C[resp_digit_i]) % 9 + 1
```

Position indices (extracted from binary, 1-based):

| Output | posA | posB | posC | Binary push order |
|--------|------|------|------|-------------------|
| 0      | 8    | 6    | 5    | push 8, push 6, push 5 |
| 1      | 1    | 5    | 7    | push 1, push 5, push 7 |
| 2      | 3    | 4    | 9    | push 3, push 4, push 9 |
| 3      | 8    | 5    | 7    | push 8, push 5, push 7 |
| 4      | 8    | 9    | 6    | push 8, push 9, push 6 |
| 5      | 3    | 1    | 4    | push 3, push 1, push 4 |
| 6      | 3    | 3    | 1    | push 3, push 3, push 1 |
| 7      | 4    | 9    | 3    | push 4, push 9, push 3 |
| 8      | 6    | 7    | 4    | push 6, push 7, push 4 |

## Proc_0_354: Token Computation

**Inputs**: challenge (arg_C), device_response (arg_10)
**Output**: auth token (9 digits, stored in arg_14/var_38)

Iterator reads sequentially from **challenge**: `Mid$(arg_C, i+1, 1)` for i=0..8
Formula positions read from **device_response**: `Mid$(arg_10, pos, 1)`
Dynamic index: `device_response[challenge_digit_value - 1]`

```
token[i] = (R[posB] * R[posC] + R[posA] * R[chal_digit_i]) % 9 + 1
```

Position indices (extracted from binary, 1-based):

| Output | posA | posB | posC | Binary push order |
|--------|------|------|------|-------------------|
| 0      | 4    | 6    | 8    | push 4, push 6, push 8 |
| 1      | 6    | 4    | 1    | push 6, push 4, push 1 |
| 2      | 8    | 6    | 5    | push 8, push 6, push 5 |
| 3      | 3    | 2    | 9    | push 3, push 2, push 9 |
| 4      | 7    | 8    | 4    | push 7, push 8, push 4 |
| 5      | 3    | 1    | 7    | push 3, push 1, push 7 |
| 6      | 9    | 4    | 3    | push 9, push 4, push 3 |
| 7      | 1    | 6    | 2    | push 1, push 6, push 2 |
| 8      | 3    | 2    | 8    | push 3, push 2, push 8 |

## Calling Convention

From the calling code (Main.frm lines 81156-81161):
1. Software sends challenge to device: `:r90=<challenge>`
2. Device responds with echo + separator + device_response
3. `Proc_0_353(Me, challenge, device_response)` computes echo for validation
4. Software validates echo matches
5. `Proc_0_354(Me, challenge, device_response, token_output)` computes auth token
6. Software sends token: `:w92=<token>`

Proc_0_353 does NOT modify the device_response (no ByRef assignment found in binary).
The echo is computed for verification purposes only. The same unmodified device_response
is passed to Proc_0_354.

## VB6 Runtime Functions Used

| IAT Address | Function | Purpose |
|-------------|----------|---------|
| [00401130] | rtcMidCharBstr (Ord#632) | Mid$() single character extraction |
| [00401224] | __vbaStrVarVal | Extract string from variant (CStr) |
| [00401364] | Ordinal#581 (rtcVal) | Val() - string to Double |
| [004012EC] | __vbaFpI4 | FP stack to Int32 (CLng from float) |
| [004012DC] | __vbaStrR8 (Ord#613) | Str$() - Double to string |
| [004010F8] | rtcTrimBstr (Ord#520) | Trim$() |
| [00401304] | __vbaStrMove | String concatenation (&) |
| [0040104C] | __vbaFreeVarList | Cleanup variant list |

## Verification Code

```python
def compute_echo(challenge, device_response):
    """Proc_0_353: compute echo from challenge and device_response"""
    INDICES_353 = [
        (8, 6, 5), (1, 5, 7), (3, 4, 9), (8, 5, 7), (8, 9, 6),
        (3, 1, 4), (3, 3, 1), (4, 9, 3), (6, 7, 4),
    ]
    C = [0] + [int(c) for c in challenge]   # 1-based indexing
    echo = ""
    for i in range(9):
        posA, posB, posC = INDICES_353[i]
        resp_digit = int(device_response[i])  # value of i-th response digit
        d = (C[posB] * C[posC] + C[posA] * C[resp_digit]) % 9 + 1
        echo += str(d)
    return echo


def compute_token(challenge, device_response):
    """Proc_0_354: compute auth token from challenge and device_response"""
    INDICES_354 = [
        (4, 6, 8), (6, 4, 1), (8, 6, 5), (3, 2, 9), (7, 8, 4),
        (3, 1, 7), (9, 4, 3), (1, 6, 2), (3, 2, 8),
    ]
    R = [0] + [int(c) for c in device_response]  # 1-based indexing
    token = ""
    for i in range(9):
        posA, posB, posC = INDICES_354[i]
        chal_digit = int(challenge[i])  # value of i-th challenge digit
        d = (R[posB] * R[posC] + R[posA] * R[chal_digit]) % 9 + 1
        token += str(d)
    return token


# Test vectors
test_vectors = [
    ("271543986", "726911191", "941378256", "883542462"),
    ("128743956", "622425247", "518926473", "778914182"),
    ("953471268", "569931448", "516273489", "757466413"),
    ("568732914", "648244366", "325891674", "885362331"),
    ("132795684", "371718423", "479235681", "499857653"),
    ("532697841", "225113832", "637518249", "263339911"),
    ("431697852", "273821994", "947832165", "567898698"),
    ("571892436", "752745965", "642195837", "721863165"),
]

all_pass = True
for challenge, expected_echo, dev_resp, expected_token in test_vectors:
    echo = compute_echo(challenge, dev_resp)
    token = compute_token(challenge, dev_resp)

    echo_ok = echo == expected_echo
    token_ok = token == expected_token

    status = "PASS" if (echo_ok and token_ok) else "FAIL"
    print(f"[{status}] C={challenge} R={dev_resp}")
    print(f"         Echo:  {echo} {'OK' if echo_ok else 'FAIL (expected ' + expected_echo + ')'}")
    print(f"         Token: {token} {'OK' if token_ok else 'FAIL (expected ' + expected_token + ')'}")

    if not (echo_ok and token_ok):
        all_pass = False

print(f"\n{'ALL TESTS PASSED' if all_pass else 'SOME TESTS FAILED'}")
```

## Running the Verification

```bash
python3 -c "
$(cat <<'PYEOF'
# Paste the verification code above
PYEOF
)"
```
