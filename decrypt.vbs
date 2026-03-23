' Try EVERY possible simple decryption method on line 0

Dim password, inputFile, fso, f, allLines
Dim xml, node, rawData, dataLen, dataBytes(), j
Dim pwLen, pwBytes(), k, output, i, readable, ci, cv

password = "2020888376Spooky2 (c) John White. http://www.cancerclinic.co.nz"

If WScript.Arguments.Count > 0 Then inputFile = WScript.Arguments(0) Else inputFile = "C:\Spooky2\Frequencies.s2d"

Set fso = CreateObject("Scripting.FileSystemObject")
Set f = fso.OpenTextFile(inputFile, 1)
allLines = Split(f.ReadAll, vbCrLf)
f.Close

pwLen = Len(password)
ReDim pwBytes(pwLen - 1)
For k = 0 To pwLen - 1
    pwBytes(k) = Asc(Mid(password, k + 1, 1))
Next

' Decode line 0
Set xml = CreateObject("MSXML2.DOMDocument")
Set node = xml.createElement("b64")
node.DataType = "bin.base64"
node.Text = Trim(allLines(0))
rawData = node.nodeTypedValue
dataLen = LenB(rawData)
ReDim dataBytes(dataLen - 1)
For j = 0 To dataLen - 1
    dataBytes(j) = AscB(MidB(rawData, j + 1, 1))
Next

WScript.Echo "Line 0: " & dataLen & " bytes"
WScript.Echo ""

' === METHOD A: Simple RndCryptB (byte XOR after Base64 decode) ===
' This is what RndCryptLevel2 might do internally for InputType=3
WScript.Echo "=== A: RndCryptB on Base64-decoded bytes ==="

' Password hash using BYTE values (StrConv vbFromUnicode equivalent)
Rnd(-1)
Randomize CLng(pwLen)

Dim pwHash
pwHash = CLng(0)
For k = 0 To pwLen - 1
    Dim rv
    rv = Int(Rnd() * 256)
    If rv < 0 Then rv = rv + 256
    pwHash = (CLng(pwBytes(k)) Xor CLng(rv)) Xor pwHash
Next

Rnd(-1)
Randomize CDbl(pwHash)

output = ""
For i = 0 To dataLen - 1
    Dim xb
    xb = Int(Rnd() * 256)
    If xb < 0 Then xb = xb + 256
    output = output & Chr((dataBytes(i) Xor xb) And 255)
Next

readable = True
For ci = 1 To Len(output)
    cv = Asc(Mid(output, ci, 1))
    If cv < 9 Or (cv > 13 And cv < 32) Or cv > 126 Then readable = False : Exit For
Next
If readable Then
    WScript.Echo "  OK: " & output
Else
    WScript.Echo "  Failed. Bytes: " & Asc(Mid(output,1,1)) & " " & Asc(Mid(output,2,1)) & " " & Asc(Mid(output,3,1))
End If

' === METHOD B: RndCrypt on raw Base64 STRING (before decode) ===
WScript.Echo "=== B: RndCrypt on raw Base64 string ==="
Dim rawLine
rawLine = Trim(allLines(0))

Rnd(-1)
Randomize CLng(Len(password))
pwHash = CLng(0)
For k = 1 To Len(password)
    rv = Int(Rnd() * 256)
    If rv < 0 Then rv = rv + 256
    pwHash = (Asc(Mid(password, k, 1)) Xor CLng(rv)) Xor pwHash
Next

Rnd(-1)
Randomize CDbl(pwHash)

output = ""
For i = 1 To Len(rawLine)
    xb = Int(Rnd() * 256)
    If xb < 0 Then xb = xb + 256
    output = output & Chr((Asc(Mid(rawLine, i, 1)) Xor xb) And 255)
Next

readable = True
For ci = 1 To Len(output)
    cv = Asc(Mid(output, ci, 1))
    If cv < 9 Or (cv > 13 And cv < 32) Or cv > 126 Then readable = False : Exit For
Next
If readable Then
    WScript.Echo "  OK: " & output
Else
    WScript.Echo "  Failed. Bytes: " & Asc(Mid(output,1,1)) & " " & Asc(Mid(output,2,1)) & " " & Asc(Mid(output,3,1))
End If

' === METHOD C: RndCryptB with Rnd*255 instead of Rnd*256 ===
WScript.Echo "=== C: RndCryptB with Rnd*255 ==="
Rnd(-1)
Randomize CLng(pwLen)
pwHash = CLng(0)
For k = 0 To pwLen - 1
    rv = Int(Rnd() * 255)
    pwHash = (CLng(pwBytes(k)) Xor CLng(rv)) Xor pwHash
Next
Rnd(-1)
Randomize CDbl(pwHash)
output = ""
For i = 0 To dataLen - 1
    xb = Int(Rnd() * 255)
    output = output & Chr((dataBytes(i) Xor xb) And 255)
Next
readable = True
For ci = 1 To Len(output)
    cv = Asc(Mid(output, ci, 1))
    If cv < 9 Or (cv > 13 And cv < 32) Or cv > 126 Then readable = False : Exit For
Next
If readable Then
    WScript.Echo "  OK: " & output
Else
    WScript.Echo "  Failed. Bytes: " & Asc(Mid(output,1,1)) & " " & Asc(Mid(output,2,1)) & " " & Asc(Mid(output,3,1))
End If

' === METHOD D: RndCryptB with NO Rnd(-1) reset before hash ===
WScript.Echo "=== D: No Rnd(-1) before hash ==="
Randomize CLng(pwLen)
pwHash = CLng(0)
For k = 0 To pwLen - 1
    rv = Int(Rnd() * 256)
    If rv < 0 Then rv = rv + 256
    pwHash = (CLng(pwBytes(k)) Xor CLng(rv)) Xor pwHash
Next
Randomize CDbl(pwHash)
output = ""
For i = 0 To dataLen - 1
    xb = Int(Rnd() * 256)
    If xb < 0 Then xb = xb + 256
    output = output & Chr((dataBytes(i) Xor xb) And 255)
Next
readable = True
For ci = 1 To Len(output)
    cv = Asc(Mid(output, ci, 1))
    If cv < 9 Or (cv > 13 And cv < 32) Or cv > 126 Then readable = False : Exit For
Next
If readable Then
    WScript.Echo "  OK: " & output
Else
    WScript.Echo "  Failed. Bytes: " & Asc(Mid(output,1,1)) & " " & Asc(Mid(output,2,1)) & " " & Asc(Mid(output,3,1))
End If

' === METHOD E: Try on the RAW line (not base64 decoded) with RndCryptB approach ===
' Maybe the .s2d file is NOT base64 encoded at all?
WScript.Echo "=== E: Treat file as raw binary, not Base64 ==="
Dim rawBytes()
ReDim rawBytes(Len(rawLine) - 1)
For j = 0 To Len(rawLine) - 1
    rawBytes(j) = Asc(Mid(rawLine, j + 1, 1))
Next

Rnd(-1)
Randomize CLng(pwLen)
pwHash = CLng(0)
For k = 0 To pwLen - 1
    rv = Int(Rnd() * 256)
    If rv < 0 Then rv = rv + 256
    pwHash = (CLng(pwBytes(k)) Xor CLng(rv)) Xor pwHash
Next
Rnd(-1)
Randomize CDbl(pwHash)
output = ""
For i = 0 To Len(rawLine) - 1
    xb = Int(Rnd() * 256)
    If xb < 0 Then xb = xb + 256
    output = output & Chr((rawBytes(i) Xor xb) And 255)
Next
readable = True
For ci = 1 To Len(output)
    cv = Asc(Mid(output, ci, 1))
    If cv < 9 Or (cv > 13 And cv < 32) Or cv > 126 Then readable = False : Exit For
Next
If readable Then
    WScript.Echo "  OK: " & output
Else
    WScript.Echo "  Failed. Bytes: " & Asc(Mid(output,1,1)) & " " & Asc(Mid(output,2,1)) & " " & Asc(Mid(output,3,1))
End If

' === METHOD F: EncryptFreq (digit substitution) on raw line ===
WScript.Echo "=== F: Maybe it's EncryptFreq, not RndCrypt? ==="
WScript.Echo "  First 30 chars of raw line: " & Left(rawLine, 30)
WScript.Echo "  Contains only base64 chars: yes (it IS base64)"
