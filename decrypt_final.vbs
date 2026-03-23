' Test: does encrypting the password with itself produce the correct key?

' ALL variable declarations at top (VBScript requirement)
Dim rawPw, pwLen, rawPwBytes(), encPwBytes(), k, pwHash, rv
Dim inputFile, fso, f2, allLines, xml, nd, rawData, dataLen, dataBytes(), j
Dim encHex, pIdx, curPwBytes(), totalCount, acc, i
Dim rnd1, pb, ex, pr, rnd2, r2v, kb, rnd1b, rb, db, decByte
Dim r3, r4, pv, output, readable, ci, cv

rawPw = "2020888376Spooky2 (c) John White. http://www.cancerclinic.co.nz"
pwLen = Len(rawPw)

ReDim rawPwBytes(pwLen - 1)
ReDim encPwBytes(pwLen - 1)
ReDim curPwBytes(pwLen - 1)

For k = 0 To pwLen - 1
    rawPwBytes(k) = Asc(Mid(rawPw, k + 1, 1))
Next

' RndCrypt: encrypt rawPw bytes using rawPw as key
Rnd(-1)
Randomize CLng(pwLen)
pwHash = CLng(0)
For k = 0 To pwLen - 1
    rv = Int(Rnd() * 256)
    If rv < 0 Then rv = rv + 256
    pwHash = (CLng(rawPwBytes(k)) Xor CLng(rv)) Xor pwHash
Next
Rnd(-1)
Randomize CDbl(pwHash)
For k = 0 To pwLen - 1
    rv = Int(Rnd() * 256)
    If rv < 0 Then rv = rv + 256
    encPwBytes(k) = (rawPwBytes(k) Xor rv) And 255
Next

encHex = ""
For k = 0 To 19
    encHex = encHex & Right("0" & Hex(encPwBytes(k)), 2) & " "
Next
WScript.Echo "Encrypted pw: " & encHex

' Load file
If WScript.Arguments.Count > 0 Then inputFile = WScript.Arguments(0) Else inputFile = "C:\Spooky2\Frequencies.s2d"
Set fso = CreateObject("Scripting.FileSystemObject")
Set f2 = fso.OpenTextFile(inputFile, 1)
allLines = Split(f2.ReadAll, vbCrLf)
f2.Close

Set xml = CreateObject("MSXML2.DOMDocument")
Set nd = xml.createElement("b64")
nd.DataType = "bin.base64"
nd.Text = Trim(allLines(0))
rawData = nd.nodeTypedValue
dataLen = LenB(rawData)
ReDim dataBytes(dataLen - 1)
For j = 0 To dataLen - 1
    dataBytes(j) = AscB(MidB(rawData, j + 1, 1))
Next

WScript.Echo "Line 0: " & dataLen & " bytes"
WScript.Echo ""

For pIdx = 0 To 1
    For k = 0 To pwLen - 1
        If pIdx = 0 Then curPwBytes(k) = rawPwBytes(k) Else curPwBytes(k) = encPwBytes(k)
    Next

    If pIdx = 0 Then WScript.Echo "=== RAW password ===" Else WScript.Echo "=== ENCRYPTED password ==="
    totalCount = pwLen + dataLen

    Rnd(-1)
    Randomize CLng(totalCount)
    acc = CLng(0)

    For i = 1 To totalCount
        rnd1 = Rnd()
        pb = CLng(curPwBytes(i Mod pwLen))
        ex = CDbl(rnd1) * 2.7526486955 + 1.0
        If pb <= 0 Then
            pr = 0
        Else
            On Error Resume Next
            pr = CLng(CDbl(pb) ^ ex)
            If Err.Number <> 0 Then pr = 0 : Err.Clear
            On Error GoTo 0
        End If
        rnd2 = Rnd()
        On Error Resume Next
        r2v = CLng(CDbl(rnd2) * 1000.0)
        If Err.Number <> 0 Then r2v = 0 : Err.Clear
        On Error GoTo 0
        On Error Resume Next
        acc = r2v Or ((acc And 1073741823) + pr)
        If Err.Number <> 0 Then acc = r2v : Err.Clear
        On Error GoTo 0
    Next

    WScript.Echo "  acc = " & acc

    Rnd(-1)
    Randomize acc
    output = ""

    For i = 0 To dataLen - 1
        kb = CInt(acc And 255)
        rnd1b = Rnd()
        On Error Resume Next
        rb = CInt(Int(CDbl(rnd1b) * 255.49)) And 255
        If Err.Number <> 0 Then rb = 0 : Err.Clear
        On Error GoTo 0
        db = dataBytes(i)
        decByte = (rb Xor db Xor kb) And 255
        output = output & Chr(decByte)
        r3 = Rnd()
        r4 = Rnd()
        On Error Resume Next
        pv = CLng((CDbl(r3) * 255.0) ^ (CDbl(r4) * 2.7526486955 + 1.0))
        If Err.Number <> 0 Then pv = 0 : Err.Clear
        On Error GoTo 0
        acc = (acc \ 2) + pv
    Next

    readable = True
    For ci = 1 To Len(output)
        cv = Asc(Mid(output, ci, 1))
        If cv < 9 Or (cv > 13 And cv < 32) Or cv > 126 Then readable = False : Exit For
    Next
    If readable Then
        WScript.Echo "  *** DECRYPTED: " & output & " ***"
    Else
        WScript.Echo "  Failed. Bytes: " & Asc(Mid(output,1,1)) & " " & Asc(Mid(output,2,1)) & " " & Asc(Mid(output,3,1)) & " " & Asc(Mid(output,4,1)) & " " & Asc(Mid(output,5,1))
    End If
    WScript.Echo ""
Next
