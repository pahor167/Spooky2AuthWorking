VERSION 5.00
Begin VB.Form CreateProgram
  Caption = "Create Program"
  ForeColor = &H0&
  ScaleMode = 1
  AutoRedraw = True
  FontTransparent = True
  FillStyle = 0
  Icon = "CreateProgram.frx":0
  LinkTopic = "Form1"
  ClientLeft = 1035
  ClientTop = 510
  ClientWidth = 20040
  ClientHeight = 11565
  BeginProperty Font
    Name = "Arial"
    Size = 11.25
    Charset = 0
    Weight = 400
    Underline = 0 'False
    Italic = 0 'False
    Strikethrough = 0 'False
  EndProperty
  StartUpPosition = 1 'CenterOwner
  Begin TextBox Text1
    ForeColor = &H0&
    Left = 120
    Top = 1680
    Width = 19815
    Height = 4695
    TabIndex = 11
    BorderStyle = 0 'None
    MultiLine = -1  'True
    ScrollBars = 2
  End
  Begin TextBox EmailText
    Left = 19200
    Top = 6720
    Width = 735
    Height = 360
    Visible = 0   'False
    TabIndex = 9
    BeginProperty Font
      Name = "Arial"
      Size = 9.75
      Charset = 0
      Weight = 400
      Underline = 0 'False
      Italic = 0 'False
      Strikethrough = 0 'False
    EndProperty
  End
  Begin ActiveResize ActiveResize1
  End
  Begin TextBox Text5
    ForeColor = &H0&
    Left = 120
    Top = 10320
    Width = 18375
    Height = 1125
    TabIndex = 4
    MultiLine = -1  'True
    ScrollBars = 2
    Appearance = 0 'Flat
    ToolTipText = "Enter a good description. This will help during searches later"
  End
  Begin TextBox Text9
    ForeColor = &H0&
    Left = 120
    Top = 9000
    Width = 855
    Height = 375
    Text = "180"
    TabIndex = 3
    Alignment = 2 'Center
    Appearance = 0 'Flat
    ToolTipText = "This is the default number of seconds each frequency should run."
  End
  Begin CommandButton Command5
    Left = 19440
    Top = 120
    Width = 495
    Height = 525
    TabIndex = 5
    Picture = "CreateProgram.frx":FD55
    ToolTipText = "Add the program to the Custom database"
    Style = 1
  End
  Begin TextBox Text8
    ForeColor = &H0&
    Left = 120
    Top = 6960
    Width = 18375
    Height = 1035
    TabIndex = 2
    MultiLine = -1  'True
    ScrollBars = 2
    Appearance = 0 'Flat
    ToolTipText = "CreateProgram.frx":10567
  End
  Begin TextBox Text7
    ForeColor = &H0&
    Left = 120
    Top = 960
    Width = 19815
    Height = 345
    TabIndex = 1
    Appearance = 0 'Flat
    ToolTipText = "Give your program set a descriptive name."
  End
  Begin Label Label7
    Caption = "Any notes for this program should be entered here. Spooky2 will include this in searches."
    BackColor = &HFFFFFF&
    ForeColor = &H0&
    Left = 120
    Top = 9600
    Width = 12975
    Height = 375
    TabIndex = 15
    BackStyle = 0 'Transparent
  End
  Begin Label Label6
    Caption = "Enter the default duration (in seconds) you wish to apply each frequency. "
    BackColor = &HFFFFFF&
    ForeColor = &H0&
    Left = 120
    Top = 8280
    Width = 12975
    Height = 375
    TabIndex = 14
    BackStyle = 0 'Transparent
  End
  Begin Label Label5
    Caption = "DBase Name"
    BackColor = &HFFFFFF&
    Left = 10080
    Top = 8880
    Width = 1815
    Height = 375
    Visible = 0   'False
    TabIndex = 13
    BackStyle = 0 'Transparent
    ToolTipText = "Holds the filename of the database to write to"
  End
  Begin Label Label4
    Caption = "Save to ..."
    BackColor = &HFFFFFF&
    ForeColor = &H0&
    Left = 18960
    Top = 10800
    Width = 1935
    Height = 375
    Visible = 0   'False
    TabIndex = 12
    BackStyle = 0 'Transparent
    ToolTipText = "Pathname to save to"
  End
  Begin Label Label3
    Caption = "Give your program a descriptive name. This will assist you when you do a program search."
    BackColor = &HFFFFFF&
    ForeColor = &H0&
    Left = 120
    Top = 240
    Width = 19815
    Height = 375
    TabIndex = 10
    BackStyle = 0 'Transparent
  End
  Begin Label Label1
    Caption = "Frequencies"
    BackColor = &HFFFFFF&
    ForeColor = &H0&
    Left = 120
    Top = 6600
    Width = 12975
    Height = 375
    TabIndex = 8
    BackStyle = 0 'Transparent
  End
  Begin Label Label38
    Caption = "Program Description"
    BackColor = &HFFFFFF&
    ForeColor = &H0&
    Left = 120
    Top = 9960
    Width = 13575
    Height = 375
    TabIndex = 7
    BackStyle = 0 'Transparent
  End
  Begin Label Label32
    Caption = "Dwell"
    BackColor = &HFFFFFF&
    ForeColor = &H0&
    Left = 120
    Top = 8640
    Width = 4095
    Height = 375
    TabIndex = 6
    BackStyle = 0 'Transparent
  End
  Begin Label Label30
    Caption = "Program Name"
    BackColor = &HFFFFFF&
    ForeColor = &H0&
    Left = 120
    Top = 600
    Width = 14535
    Height = 375
    TabIndex = 0
    BackStyle = 0 'Transparent
  End
End

Attribute VB_Name = "CreateProgram"


Private Sub Text8_KeyPress(KeyAscii As Integer) '8AA060
  loc_008AA0AC: If KeyAscii <> 1 Then GoTo loc_008AA0D3
  loc_008AA0C4: var_eax = CreateProgram.Proc_1_12_8AA890(Me, var_18, var_18, Me)
  loc_008AA0D3: 'Referenced from: 008AA0AC
  loc_008AA0DB: GoTo loc_008AA0E7
  loc_008AA0E6: Exit Sub
  loc_008AA0E7: 'Referenced from: 008AA0DB
End Sub

Private Sub Text8_LostFocus() '8AA1C0

End Sub

Private Sub Text9_KeyPress(KeyAscii As Integer) '8AA110
  loc_008AA15C: If KeyAscii <> 1 Then GoTo loc_008AA183
  loc_008AA174: var_eax = CreateProgram.Proc_1_12_8AA890(Me, var_18, var_18, Me)
  loc_008AA183: 'Referenced from: 008AA15C
  loc_008AA18B: GoTo loc_008AA197
  loc_008AA196: Exit Sub
  loc_008AA197: 'Referenced from: 008AA18B
End Sub

Private Sub Text9_LostFocus() '8AA230
  Dim var_1C As TextBox
  loc_008AA295: var_18 = Text9.Text
  loc_008AA2C3: fcomp real8 ptr [00401EB8h]
  loc_008AA2D5: GoTo loc_008AA2D9
  loc_008AA2D9: 'Referenced from: 008AA2D5
  loc_008AA2F4: If eax = 0 Then GoTo loc_008AA322
  loc_008AA317: var_eax = CreateProgram.Proc_1_11_8AA6B0(Me, esi+00000038h & "\CreateProgram.txt", var_1C, Me)
  loc_008AA322: 'Referenced from: 008AA2F4
  loc_008AA32F: GoTo loc_008AA344
  loc_008AA343: Exit Sub
  loc_008AA344: 'Referenced from: 008AA32F
End Sub

Private Sub Form_Load() '8A7170
  Dim var_2C As Me
  loc_008A71DB: Dim var_2C(40) As String
  loc_008A71EF: var_20 = var_20 + 00000004h
  loc_008A71F2: var_20 = "Frequencies (and optional advanced commands) are entered using a comma to separate each program step."
  loc_008A71FF: ecx = vbNullString
  loc_008A7209: var_20 = var_20 + 0000000Ch
  loc_008A720C: var_20 = "xxx-yyy Sweeps the frequency from xxx Hz to yyy Hz."
  loc_008A7219: ecx = "=xxx    x is the dwell (seconds) this frequency is to be applied. Example: 2127=180,2128=240,2127-2128=600"
  loc_008A7223: var_20 = var_20 + 00000014h
  loc_008A7226: var_20 = "Wx      x is the waveform for this frequency. W sets Out 1 and w sets Out 2. The values of x for when specifying the waveforms are:"
  loc_008A7233: ecx = "1 for Sine."
  loc_008A723D: var_20 = var_20 + 0000001Ch
  loc_008A7240: var_20 = "2 for Square."
  loc_008A724D: ecx = "3 for Sawtooth."
  loc_008A7257: var_20 = var_20 + 00000024h
  loc_008A725A: var_20 = "4 for Inverted Sawtooth."
  loc_008A7267: ecx = "5 for Triangle."
  loc_008A7271: var_20 = var_20 + 0000002Ch
  loc_008A7274: var_20 = "6 for Damped Sinusoidal."
  loc_008A7281: ecx = "7 for Damped Square."
  loc_008A728B: var_20 = var_20 + 00000034h
  loc_008A728E: var_20 = "8 for H-Bomb Sinusoidal."
  loc_008A729B: ecx = "9 for H-Bomb Square"
  loc_008A72A5: var_20 = var_20 + 0000003Ch
  loc_008A72A8: var_20 = "10 for User Defined #1"
  loc_008A72B5: ecx = "11 for User Defined #2"
  loc_008A72BF: var_20 = var_20 + 00000044h
  loc_008A72C2: var_20 = "Gx  x is 1 to turn gating on. 0 for no gating."
  loc_008A72CF: ecx = "Ax  x is the amplitude (voltage peak to peak) of Out 1."
  loc_008A72D9: var_20 = var_20 + 0000004Ch
  loc_008A72DC: var_20 = "ax  x is the amplitude (voltage peak to peak) of Out 2."
  loc_008A72E9: ecx = "Lx  x is the light wavelength in nanometers (nm) which Spooky2 will convert to a frequency. CLx can be used as a wavelength Constant for Out 2."
  loc_008A72F3: var_20 = var_20 + 00000054h
  loc_008A72F6: var_20 = "Mx  x is the Monoisotopic Molecular Weight (g/mol) which Spooky2 will convert to a frequency. Cx can be used as a Multiplier and/or Constant for Out 2."
  loc_008A7303: ecx = "Bx  x is the number of Base Pairs in a genome. Spooky will convert this to a frequency if a Factor is entered. Bx can be used as a Multiplier and/or Constant for Out 2."
  loc_008A730D: var_20 = var_20 + 0000005Ch
  loc_008A7310: var_20 = "BCx x is the number of Base Pairs in a Circular genome. BCx can be used as a Multiplier and/or Constant for Out 2."
  loc_008A731D: ecx = "BLx x is the number of Base Pairs in a Linear genome. BLx can be used as a Multiplier and/or Constant for Out 2."
  loc_008A7327: var_20 = var_20 + 00000064h
  loc_008A732A: var_20 = "Ox  x is the offset (%) of the output. Use lower case 'o' for negative offset, upper case 'O' for positive."
  loc_008A7337: ecx = "R defines an RNA entry. Example: BLR29900, BCR200"
  loc_008A7341: var_20 = var_20 + 0000006Ch
  loc_008A7344: var_20 = "m defines an mRNA entry. Examples: BLm29900, BCm3560"
  loc_008A7351: ecx = "Px  x is the phase angle of the output."
  loc_008A735B: var_20 = var_20 + 00000074h
  loc_008A735E: var_20 = "Fx  x is the Factor (multiplier) to be applied to OUT1 frequency to determine OUT2 frequency. OUT2 = OUT1 x (Factor) + (Constant). This overrides OUT2 Sync settings."
  loc_008A736B: ecx = "Cx  x is the Constant in the above equation."
  loc_008A7375: var_20 = var_20 + 0000007Ch
  loc_008A7378: var_20 = "T+ forces Tissue Factor on"
  loc_008A7388: ecx = "T- forces Tissue Factor off"
  loc_008A7392: var_20 = var_20 + 00000084h
  loc_008A7398: var_20 = "[...] A chemical formula inside square brackets converts the formula to a monoisotopic mass frequency. For example: [C860H1353N227O255S9] is a valid entry."
  loc_008A73A8: ecx = "(...) Letters inside round brackets convert amino acid (protein) letters to their fundamental frequencies. Prefixing with '-' means inhibit function. Example: (mkalivlg),(-mgasvi)"
  loc_008A73B2: var_20 = var_20 + 0000008Ch
  loc_008A73B8: var_20 = "{...} Letters inside curly brackets convert nucleotide letters to their fundamental frequencies. Prefixing with '-' means inhibit function. Example: {ctaggaat},{-tagc}"
  loc_008A73C8: ecx = "Example: 1604000=180 F1 C14,100-200=120,(GASP)=.5,BCT-29980=180,MT+654.032"
  loc_008A73D2: var_20 = var_20 + 00000094h
  loc_008A73D8: var_20 = "This will produce frequency of 1604000 Hz at 5 volts with a 100% volt offset (suitable for Spooky Central) with OUT2 set to run 14Hz faster than OUT1 (OUT2 = OUT1 x 1 + 14), then a sweep from 100 Hz to 200 Hz over 120 seconds (OUT2 will Exit Subain its relation to OUT1 until instructed otherwise). The frequencies for the protein 'GASP' will then run, followed by a DNA frequency, and finally a molecular weight frequency."
End Sub

Private Sub Command5_Click() '8A7890
  Dim var_258 As TextBox
  loc_008A78F5: On Error Resume Next
  loc_008A7935: var_38 = Text7.Text
  loc_008A793D: var_25C = var_38
  loc_008A798A: eax = Len(var_38) + 1
  loc_008A798D: var_260 = Len(var_38) + 1
  loc_008A79AF: If var_260 = 0 Then GoTo loc_008A7A41
  loc_008A79D6: var_258 = var_48
  loc_008A79F2: Text7.BackColor = eax+0000003Ch
  loc_008A79F7: var_25C = var_258
  loc_008A7A3C: GoTo loc_008A990A
  loc_008A7A41: 'Referenced from: 008A79AF
  loc_008A7A7B: var_38 = Text8.Text
  loc_008A7A83: var_25C = var_38
  loc_008A7AD0: eax = Len(var_38) + 1
  loc_008A7AD3: var_260 = Len(var_38) + 1
  loc_008A7AF5: If var_260 = 0 Then GoTo loc_008A7B87
End Sub

Private Sub Text7_KeyPress(KeyAscii As Integer) '8A9FB0
  loc_008A9FFC: If KeyAscii <> 1 Then GoTo loc_008AA023
  loc_008AA014: var_eax = CreateProgram.Proc_1_12_8AA890(Me, var_18, var_18, Me)
  loc_008AA023: 'Referenced from: 008A9FFC
  loc_008AA02B: GoTo loc_008AA037
  loc_008AA036: Exit Sub
  loc_008AA037: 'Referenced from: 008AA02B
End Sub

Private Sub Text5_KeyPress(KeyAscii As Integer) '8A9F00
  loc_008A9F4C: If KeyAscii <> 1 Then GoTo loc_008A9F73
  loc_008A9F64: var_eax = CreateProgram.Proc_1_12_8AA890(Me, var_18, var_18, Me)
  loc_008A9F73: 'Referenced from: 008A9F4C
  loc_008A9F7B: GoTo loc_008A9F87
  loc_008A9F86: Exit Sub
  loc_008A9F87: 'Referenced from: 008A9F7B
End Sub

Public Sub Proc_1_8_8A9A40
  loc_008A9A9B: If Len(arg_C) = 0 Then GoTo loc_008A9C48
  loc_008A9AB2: ecx = vbNullString
  loc_008A9AC3: var_84 = Len(arg_C)
  loc_008A9AD4: If 00000001h > 0 Then GoTo loc_008A9C37
  loc_008A9AF5: var_60 = arg_C
  loc_008A9B14: var_20 = Mid(arg_C, 1, 1)
  loc_008A9B32: var_60 = var_20
  loc_008A9B55: eax = (var_20 = var_004A5128) + 1
  loc_008A9B5B: var_70 = (var_20 = var_004A5128) + 1
  loc_008A9B6F: var_ret_1 = (var_20 = Chr(34))
  loc_008A9B7E: call Or(var_58, var_78, var_ret_1, @Len(%StkVar1), arg_C, undef 'Ignore this '__vbaFreeVarList)
  loc_008A9B9F: If CBool(Or(var_58, var_78, var_ret_1, @Len(%StkVar1) = 0 Then GoTo loc_008A9BB3
  loc_008A9BB1: GoTo loc_008A9BB9
  loc_008A9BB3: 'Referenced from: 008A9B9F
  loc_008A9BB9: 'Referenced from: 008A9BB1
  loc_008A9BCA: If (var_20 <> var_004A33E0) <> 0 Then GoTo loc_008A9BD6
  loc_008A9BD4: var_20 = vbNullString
  loc_008A9BD6: 'Referenced from: 008A9BCA
  loc_008A9BE7: If (var_20 <> var_004AE7C8) <> 0 Then GoTo loc_008A9BF3
  loc_008A9BF3: 'Referenced from: 008A9BE7
  loc_008A9C15: var_90 = var_90 & var_20
  loc_008A9C2C: 00000001h = 00000001h + 00000001h
  loc_008A9C32: GoTo loc_008A9ACE
  loc_008A9C37: 'Referenced from: 008A9AD4
  loc_008A9C42: var_18 = ecx
  loc_008A9C48: 'Referenced from: 008A9A9B
  loc_008A9C4D: GoTo loc_008A9C7F
  loc_008A9C53: If var_4 = 0 Then GoTo loc_008A9C5E
  loc_008A9C5E: 'Referenced from: 008A9C53
  loc_008A9C7E: Exit Sub
  loc_008A9C7F: 'Referenced from: 008A9C4D
  loc_008A9C8F: Exit Sub
End Sub

Public Sub Proc_1_9_8A9CC0
  loc_008A9D24: If Len(arg_C) = 0 Then GoTo loc_008A9E89
  loc_008A9D3B: var_94 = Len(arg_C)
  loc_008A9D4F: If 00000001h > 0 Then GoTo loc_008A9E7A
  loc_008A9D5D: var_60 = arg_C
  loc_008A9D7B: var_48 = Mid(arg_C, 1, 1)
  loc_008A9D90: var_1C = var_48
  loc_008A9DAC: var_70 = "~-=WMBT+[]GALOPFC0123456789,."
  loc_008A9DBA: var_60 = var_1C
  loc_008A9DC0: var_38 = Ucase(var_1C)
  loc_008A9DE7: call InStr(var_48, 00000000h, var_38, var_78, 00000001h, @Len(%StkVar1), 00004008h, undef 'Ignore this '__vbaFreeVarList)
  loc_008A9E0F: If (InStr(var_48, 00000000h, var_38, var_78, 00000001h, @Len(0) = 0 Then GoTo loc_008A9E21
  loc_008A9E1F: GoTo loc_008A9E43
  loc_008A9E21: 'Referenced from: 008A9E0F
  loc_008A9E32: If (var_1C <> var_004A52C0) <> 0 Then GoTo loc_008A9E63
End Sub

Public Sub Proc_1_10_8AA370
  Dim var_7C As TextBox
  loc_008AA3BE: On Error Resume Next
  loc_008AA3CE: var_60 = arg_C
  loc_008AA401: esi = (Dir(arg_C, 0) = vbNullString) + 1
  loc_008AA419: eax = (arg_C = vbNullString) + 1
  loc_008AA41F: var_7C = (arg_C = vbNullString) + 1
  loc_008AA432: If var_7C = 0 Then GoTo loc_008AA439
  loc_008AA434: GoTo loc_008AA64F
  loc_008AA439: 'Referenced from: 008AA432
  loc_008AA45B: var_2C = FreeFile(10)
  loc_008AA482: Open ecx For Input As #var_2C Len = -1
  loc_008AA488: 
  loc_008AA492: var_ret_2 = var_2C
  loc_008AA4A4: If EOF(var_ret_2) <> 0 Then GoTo loc_008AA638
  loc_008AA4C4: Input var_2C, var_30
  loc_008AA4E7: var_28 = InStr(1, var_30, var_004A1940, 0)
  loc_008AA4F5: If var_28 <> 0 Then GoTo loc_008AA4FC
  loc_008AA4F7: GoTo loc_008AA633
  loc_008AA4FC: 'Referenced from: 008AA4F5
  loc_008AA510: If var_28 <> 0 Then GoTo loc_008AA529
  loc_008AA521: var_24 = vbNullString
  loc_008AA527: GoTo loc_008AA59F
  loc_008AA529: 'Referenced from: 008AA510
  loc_008AA533: var_28 = var_28 + 00000001h
  loc_008AA557: var_60 = var_30
  loc_008AA586: var_24 = Mid(var_30, var_28, 10)
  loc_008AA59F: 'Referenced from: 008AA527
  loc_008AA5BC: If InStr(1, var_30, "Text9<>", 0) <> 0 Then GoTo loc_008AA633
  loc_008AA5DF: var_7C = var_38
  loc_008AA5EF: Text9.Text = var_24
  loc_008AA5F7: var_80 = var_7C
  loc_008AA633: GoTo loc_008AA488
  loc_008AA638: 'Referenced from: 008AA4A4
  loc_008AA649: Close #var_2C
  loc_008AA64F: 'Referenced from: 008AA434
  loc_008AA64F: Method_8964E04D
  loc_008AA654: GoTo loc_008AA67C
  loc_008AA67B: Exit Sub
  loc_008AA67C: 'Referenced from: 008AA654
End Sub

Public Sub Proc_1_11_8AA6B0
  loc_008AA6FE: On Error Resume Next
  loc_008AA726: var_28 = FreeFile(10)
  loc_008AA74D: Open arg_C For Output As #var_28 Len = -1
  loc_008AA784: var_2C = Text9.Text
  loc_008AA78C: var_5C = var_2C
  loc_008AA7E3: var_eax = CancelIo(var_004A04AC, var_28, "Text9=" & var_2C, var_34, Me)
  loc_008AA819: Close #var_28
  loc_008AA81F: GoTo loc_008AA83D
  loc_008AA82A: On Error Resume Next
  loc_008AA837: var_eax = Close
  loc_008AA83D: 'Referenced from: 008AA81F
  loc_008AA83D: Exit Sub
  loc_008AA843: Method_8964E04D
  loc_008AA848: GoTo loc_008AA870
  loc_008AA86F: Exit Sub
  loc_008AA870: 'Referenced from: 008AA848
End Sub

Public Sub Proc_1_12_8AA890
  Dim var_14 As Me
  loc_008AA8C9: Set var_14 = arg_C
  loc_008AA90B: var_18 = var_14.MousePointer
  loc_008AA93C: var_14.FontTransparent = Len(var_18)
  loc_008AA967: GoTo loc_008AA973
  loc_008AA972: Exit Sub
  loc_008AA973: 'Referenced from: 008AA967
End Sub

Public Sub Proc_1_13_8AA9A0
  loc_008AA9E9: var_4C = arg_C
  loc_008AAA07: ecx = Trim(arg_C)
  loc_008AAA21: var_20 = Len(arg_C)
  loc_008AAA24: If Len(arg_C) = 0 Then GoTo loc_008AAB50
  loc_008AAA31: If 00000001h > 0 Then GoTo loc_008AAB50
  loc_008AAA58: var_4C = arg_C
  loc_008AAA73: var_1C = Mid(arg_C, 1, 1)
  loc_008AAA9D: If (var_1C <> var_004A187C) <> 0 Then GoTo loc_008AAB3D
  loc_008AAAA6: If 00000001h >= 0 Then GoTo loc_008AAB3D
  loc_008AAAB2: 00000001h = 00000001h + 00000001h
  loc_008AAAD2: var_4C = arg_C
  loc_008AAAE6: var_24 = CStr(Mid(arg_C, 1, 10))
  loc_008AAAF9: fcomp real8 ptr [00401EB8h]
  loc_008AAB0B: GoTo loc_008AAB0F
  loc_008AAB0F: 'Referenced from: 008AAB0B
  loc_008AAB30: If ebx = 0 Then GoTo loc_008AAB3D
  loc_008AAB35: var_18 = var_18 + 00000001h
  loc_008AAB3A: var_18 = var_18
  loc_008AAB3D: 'Referenced from: 008AAA9D
  loc_008AAB42: 00000001h = 00000001h + 00000001h
  loc_008AAB4B: GoTo loc_008AAA2F
  loc_008AAB50: 'Referenced from: 008AAA24
  loc_008AAB56: GoTo loc_008AAB75
  loc_008AAB74: Exit Sub
  loc_008AAB75: 'Referenced from: 008AAB56
  loc_008AAB7E: Exit Sub
End Sub
