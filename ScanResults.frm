VERSION 5.00
Begin VB.Form ScanResults
  Caption = "Scan Results"
  ScaleMode = 1
  AutoRedraw = False
  FontTransparent = True
  Icon = "ScanResults.frx":0
  LinkTopic = "Form1"
  ClientLeft = 3990
  ClientTop = 2175
  ClientWidth = 10920
  ClientHeight = 11505
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
  Begin CommandButton Command6
    Left = 5640
    Top = 2760
    Width = 495
    Height = 525
    TabIndex = 16
    Picture = "ScanResults.frx":FD55
    ToolTipText = "Clear the hits"
    Style = 1
  End
  Begin CommandButton Command5
    Left = 5640
    Top = 3480
    Width = 495
    Height = 525
    TabIndex = 13
    Picture = "ScanResults.frx":104E4
    ToolTipText = "Copy the frequencies to the clipboard"
    Style = 1
  End
  Begin Frame Frame1
    Caption = "Reverse Lookup"
    Index = 36
    ForeColor = &H0&
    Left = 6600
    Top = 1440
    Width = 4215
    Height = 2295
    TabIndex = 6
    Begin TextBox Text19
      Index = 2
      ForeColor = &H0&
      Left = 120
      Top = 1680
      Width = 1455
      Height = 330
      Text = "2"
      TabIndex = 18
      BorderStyle = 0 'None
      Alignment = 2 'Center
      Appearance = 0 'Flat
      ToolTipText = "The factor or divisor to determine harmonic or subharmonic values"
    End
    Begin ComboBox Combo2
      Left = 120
      Top = 1200
      Width = 1695
      Height = 375
      Text = "Combo2"
      TabIndex = 17
    End
    Begin CheckBox Check1
      Index = 1
      Left = 120
      Top = 840
      Width = 255
      Height = 285
      TabIndex = 15
      ToolTipText = "Include sub harmonic matches of lower database frequencies in the reverse lookup analysis"
    End
    Begin CheckBox Check1
      Index = 0
      Left = 120
      Top = 480
      Width = 255
      Height = 285
      TabIndex = 10
      ToolTipText = "Include harmonic matches of higher database frequencies in the reverse lookup analysis"
    End
    Begin CommandButton Command4
      Caption = "Go"
      Index = 0
      Left = 3240
      Top = 240
      Width = 855
      Height = 495
      TabIndex = 9
      ToolTipText = "Find frequency matches from the selected Spooky2 databases"
    End
    Begin TextBox Text19
      Index = 0
      ForeColor = &H0&
      Left = 1920
      Top = 1200
      Width = 735
      Height = 330
      Text = "0.025"
      TabIndex = 7
      BorderStyle = 0 'None
      Alignment = 2 'Center
      Appearance = 0 'Flat
      ToolTipText = "Apply this tolerance when finding frequency matches"
    End
    Begin Label Label2
      Caption = "Include Sub-Harmonics"
      Index = 1
      ForeColor = &H0&
      Left = 480
      Top = 840
      Width = 2655
      Height = 375
      TabIndex = 14
    End
    Begin Label Label2
      Caption = "Include Harmonics"
      Index = 0
      ForeColor = &H0&
      Left = 480
      Top = 480
      Width = 2175
      Height = 375
      TabIndex = 11
    End
    Begin Label Label8
      Caption = "% Tolerance"
      Index = 60
      BackColor = &HFFFFC0&
      ForeColor = &H0&
      Left = 2760
      Top = 1200
      Width = 1455
      Height = 375
      TabIndex = 8
      BackStyle = 0 'Transparent
    End
  End
  Begin CommandButton Command3
    Left = 5640
    Top = 1560
    Width = 495
    Height = 525
    TabIndex = 5
    Picture = "ScanResults.frx":10C3E
    ToolTipText = "Select all frequencies"
    Style = 1
  End
  Begin CommandButton Command1
    Left = 5640
    Top = 2160
    Width = 495
    Height = 525
    TabIndex = 4
    Picture = "ScanResults.frx":113AC
    ToolTipText = "Clear the frequency selection"
    Style = 1
  End
  Begin ListBox List3
    ForeColor = &H0&
    Left = 9240
    Top = 10200
    Width = 1455
    Height = 1080
    Visible = 0   'False
    TabIndex = 3
  End
  Begin ActiveResize ActiveResize1
  End
  Begin CommandButton Command2
    Left = 10320
    Top = 120
    Width = 465
    Height = 495
    TabIndex = 2
    Picture = "ScanResults.frx":11A7C
    ToolTipText = "Opens the ""Create Program"" window to save the frequencies found"
    Style = 1
  End
  Begin ListBox List1
    Index = 0
    BackColor = &H8000000F&
    ForeColor = &H0&
    Left = 240
    Top = 1560
    Width = 5415
    Height = 9480
    TabIndex = 0
    Appearance = 0 'Flat
    IntegralHeight = 0   'False
    Style = 1
  End
  Begin Label Label1
    Caption = "Value is"
    Index = 0
    BackColor = &HFFFFC0&
    ForeColor = &H0&
    Left = 240
    Top = 960
    Width = 10215
    Height = 375
    TabIndex = 12
    BackStyle = 0 'Transparent
  End
  Begin Image Image1
    Picture = "ScanResults.frx":1228E
    Left = 7440
    Top = 4440
    Width = 2175
    Height = 2130
    Stretch = -1  'True
  End
  Begin Label Label1
    Caption = "Hit frequencies (Value)"
    Index = 1
    BackColor = &HFFFFC0&
    ForeColor = &H0&
    Left = 240
    Top = 600
    Width = 10215
    Height = 375
    TabIndex = 1
    BackStyle = 0 'Transparent
  End
End

Attribute VB_Name = "ScanResults"


Private Sub Command6_Click() '8AD2F0
  loc_008AD356: var_eax = Unknown_VTable_Call[ecx+00000040h]
  loc_008AD376: var_eax = List1.Clear
  loc_008AD3AF: GoTo loc_008AD3C5
  loc_008AD3C4: Exit Sub
  loc_008AD3C5: 'Referenced from: 008AD3AF
End Sub

Private Sub Command1_Click() '8ABF00
  loc_008ABF43: var_eax = ScanResults.Proc_6_10_8AD610(Me, edi)
End Sub

Private Sub Combo2_Click() '8AF2D0
  loc_008AF33A: var_18 = Combo2.Text
  loc_008AF36D: edi = (var_18 = "Custom") + 1
  loc_008AF385: If (var_18 = "Custom") + 1 = 0 Then GoTo loc_008AF3EC
  loc_008AF3A2: var_eax = Unknown_VTable_Call[eax+00000040h]
  loc_008AF3C4: Text19.Visible = True
  loc_008AF3EA: GoTo loc_008AF44F
  loc_008AF3EC: 'Referenced from: 008AF385
  loc_008AF407: var_eax = Unknown_VTable_Call[ecx+00000040h]
  loc_008AF429: Text19.Visible = False
  loc_008AF44F: 'Referenced from: 008AF3EA
  loc_008AF466: GoTo loc_008AF485
  loc_008AF484: Exit Sub
  loc_008AF485: 'Referenced from: 008AF466
End Sub

Private Sub Command2_Click() '8ABFE0
  Dim var_44 As Variant
  Dim var_40 As Variant
  Dim var_008D90F8 As Variant
  Dim var_48 As TextBox
  Dim var_008D9020 As Label
  loc_008AC08E: var_eax = Unknown_VTable_Call[ecx+00000040h]
  loc_008AC0B5: var_C0 = List1.ListCount
  loc_008AC0EF: If var_C0 = 0 Then GoTo loc_008AD247
  loc_008AC10E: var_eax = List3.Clear
  loc_008AC13E: var_EC = var_C0 - 00000001h
  loc_008AC152: If edi > 0 Then GoTo loc_008AC43F
  loc_008AC174: var_eax = Unknown_VTable_Call[ecx+00000040h]
  loc_008AC19A: var_104 = var_44
  loc_008AC1A0: var_ret_1 = edi
  loc_008AC1AE: var_ret_1 = List1.Selected
  loc_008AC1D9: setz dl
  loc_008AC1F4: If edx = 0 Then GoTo loc_008AC428
  loc_008AC219: var_eax = Unknown_VTable_Call[ecx+00000040h]
  loc_008AC245: var_30 = List1.List(var_18)
  loc_008AC297: If InStr(1, var_30, var_004A353C, 0) <= 0 Then GoTo loc_008AC428
  loc_008AC2BC: var_eax = Unknown_VTable_Call[eax+00000040h]
  loc_008AC2E8: var_30 = List1.List(var_18)
  loc_008AC309: var_54 = var_30
  loc_008AC319: InStr(1, var_30, var_004A353C, 0) = InStr(1, var_30, var_004A353C, 0) - 00000001h
  loc_008AC323: var_64 = InStr(1, var_30, var_004A353C, 0)
  loc_008AC376: var_eax = ScanResults.Proc_6_13_8AEBA0(Me, Mid(0, 1, InStr(1, 0, var_004A353C, 0)), 004A187Ch, var_3C, var_40, 00000000h, var_44, var_40)
  loc_008AC3CB: var_eax = List3.AddItem var_3C, var_B0
  loc_008AC428: 'Referenced from: 008AC1F4
  loc_008AC430: 00000001h = 00000001h + var_18
  loc_008AC43A: GoTo loc_008AC146
  loc_008AC45C: var_C0 = List3.ListCount
  loc_008AC486: setz al
  loc_008AC497: If eax <> 0 Then GoTo loc_008AD247
  loc_008AC4B1: var_2C = ebx+00000038h & "\ScanTemp.txt"
  loc_008AC4F5: CreateProgram.Text8.Text = vbNullString
  loc_008AC538: var_C0 = List3.ListCount
  loc_008AC571: var_F4 = var_C0 - 00000001h
  loc_008AC580: If eax > 0 Then GoTo loc_008AC8EA
  loc_008AC588: If eax <= 0 Then GoTo loc_008AC67A
  loc_008AC5FD: var_30 = CreateProgram.Text8.Text
  loc_008AC631: var_34 = var_30 & var_004A187C
  loc_008AC639: CreateProgram.Text8.Text = var_34
  loc_008AC67A: 'Referenced from: 008AC588
  loc_008AC6E9: var_30 = CreateProgram.Text8.Text
  loc_008AC72E: var_34 = List3.List(var_18)
  loc_008AC767: var_38 = var_30 & var_34
  loc_008AC76B: List3.FontName = var_38
  loc_008AC7B9: If var_18 <> 0 Then GoTo loc_008AC8C9
  loc_008AC7CB: var_A4 = var_2C
  loc_008AC7E6: var_30 = Dir(var_2C, 0)
  loc_008AC80A: If (var_30 = vbNullString) = 0 Then GoTo loc_008AC8C9
  loc_008AC847: Open var_2C For Input As #FreeFile(var_5C) Len = -1
  loc_008AC856: Line Input #FreeFile(var_5C), var_28
  loc_008AC897: CreateProgram.Text5.Text = var_28
  loc_008AC8C3: Close #FreeFile(var_5C)
  loc_008AC8C9: 'Referenced from: 008AC7B9
  loc_008AC8DA: 00000001h = 00000001h + var_18
  loc_008AC8E5: GoTo loc_008AC57A
  loc_008AC8EA: 'Referenced from: 008AC580
  loc_008AC922: CreateProgram.Text7.Text = "BFB "
  loc_008AC982: var_eax = Unknown_VTable_Call[ecx+00000040h]
  loc_008AC9A6: var_30 = CreateProgram.Text7.Text
  loc_008AC9FC: If (var_30 = vbNullString) = 0 Then GoTo loc_008ACB93
  loc_008ACA71: var_30 = CreateProgram.Text7.Text
  loc_008ACACC: var_eax = Unknown_VTable_Call[ecx+00000040h]
  loc_008ACAF0: var_34 = CreateProgram.Text7.Text
  loc_008ACB3A: var_3C = var_30 & var_34 & var_004A353C
  loc_008ACB42: CreateProgram.Text7.Text = var_3C
  loc_008ACB93: 'Referenced from: 008AC9FC
  loc_008ACC02: var_30 = CreateProgram.Text7.Text
  loc_008ACC44: var_eax = ScanResults.Proc_6_14_8AED20(Me, True, True, var_34, var_40, var_008D90F8)
  loc_008ACC5F: var_38 = var_30 & var_34
  loc_008ACC67: CreateProgram.Text7.Text = var_38
  loc_008ACCE9: var_A4 = "Program Created "
  loc_008ACCF9: var_5C = Date
  loc_008ACD59: var_30 = CStr("Program Created " & var_5C & &H4A353C & Time)
  loc_008ACD61: CreateProgram.Text5.Text = var_30
  loc_008ACDE2: var_eax = Unknown_VTable_Call[eax+00000040h]
  loc_008ACDFE: var_eax = List1.Clear
  loc_008ACE44: var_eax = List3.Clear
  loc_008ACE93: var_eax = ScanResults.Hide
  loc_008ACEE6: var_eax = Unknown_VTable_Call[eax+00000040h]
  loc_008ACF06: var_30 = ScanResults.Command2.Caption
  loc_008ACF57: If (var_30 = vbNullString) = 0 Then GoTo loc_008AD012
  loc_008ACFC5: 54 = CreateProgram.Label1.FormatLength
  loc_008AD007: If var_30 >= 0 Then GoTo loc_008AD0C9
  loc_008AD00D: GoTo loc_008AD0BE
  loc_008AD012: 'Referenced from: 008ACF57
  loc_008AD014: If var_30 <> 0 Then GoTo loc_008AD027
  loc_008AD020: call var_48(var_00492138, vbNullString, var_48, var_30, var_44, var_30, var_44, var_40, var_008D9020, var_008D9020, var_48, var_008D90F8, var_008D90F8, var_40, var_008D9020, var_008D9020)
  loc_008AD027: 'Referenced from: 008AD014
  loc_008AD07A: 35 = CreateProgram.Label1.FormatLength
  loc_008AD0BC: If var_30 >= 0 Then GoTo loc_008AD0C9
  loc_008AD0BE: 'Referenced from: 008AD00D
  loc_008AD0C7: var_30 = CheckObj(var_008D90F8, var_004A12E4, 84)
  loc_008AD0C9: 'Referenced from: 008AD007
  loc_008AD127: var_eax = Unknown_VTable_Call[eax+00000054h]
  loc_008AD17C: CreateProgram.Command5.ToolTipText = "Add the program to the Biofeedback database"
  loc_008AD223: var_eax = CreateProgram.Show var_A0
  loc_008AD247: 'Referenced from: 008AC0EF
  loc_008AD253: GoTo loc_008AD2AB
  loc_008AD2AA: Exit Sub
  loc_008AD2AB: 'Referenced from: 008AD253
  loc_008AD2BB: Exit Sub
End Sub

Private Sub Form_Load() '8ABDD0
  Dim var_38 As Me
  loc_008ABE40: var_38 = CurDir(vbNullString)
  loc_008ABE62: ecx = 0
  loc_008ABEA2: ecx = Me.SaveProp
  loc_008ABEB9: GoTo loc_008ABED8
  loc_008ABED7: Exit Sub
  loc_008ABED8: 'Referenced from: 008ABEB9
End Sub

Private Sub Command3_Click() '8ABF70
  loc_008ABFB3: var_eax = ScanResults.Proc_6_9_8AD470(Me, edi)
End Sub

Private Sub Command5_Click() '8AE480
  Dim var_40 As ListBox
  loc_008AE513: var_eax = Unknown_VTable_Call[ecx+00000040h]
  loc_008AE53A: var_94 = List1.ListCount
  loc_008AE569: var_20 = var_94
  loc_008AE577: If var_94 = 0 Then GoTo loc_008AE9B8
  loc_008AE587: If 00000001h > 0 Then GoTo loc_008AE876
  loc_008AE5AC: var_eax = Unknown_VTable_Call[ecx+00000040h]
  loc_008AE5D1: 00000001h = 00000001h - 00000001h
  loc_008AE5DB: var_C0 = var_40
  loc_008AE5EF: var_ret_1 = List1.Selected
  loc_008AE61A: setz dl
  loc_008AE634: If edx = 0 Then GoTo loc_008AE859
  loc_008AE659: var_eax = Unknown_VTable_Call[ecx+00000040h]
  loc_008AE67C: var_1C = var_1C - 00000001h
  loc_008AE68E: var_2C = List1.List(var_1C)
  loc_008AE6E0: If InStr(1, var_2C, var_004A353C, 0) <= 0 Then GoTo loc_008AE859
  loc_008AE6E9: var_28 = var_28 + 00000001h
  loc_008AE6F5: var_28 = var_28
  loc_008AE6F8: If var_28 <= 1 Then GoTo loc_008AE714
  loc_008AE70E: var_18 = var_18 & var_004A187C
  loc_008AE714: 'Referenced from: 008AE6F8
  loc_008AE733: var_eax = Unknown_VTable_Call[ecx+00000040h]
  loc_008AE75C: var_1C = var_1C - 00000001h
  loc_008AE776: var_2C = List1.List(var_1C)
  loc_008AE79B: var_48 = var_2C
  loc_008AE7A7: InStr(1, var_2C, var_004A353C, 0) = InStr(1, var_2C, var_004A353C, 0) - 00000001h
  loc_008AE7B1: var_58 = InStr(1, var_2C, var_004A353C, 0)
  loc_008AE803: var_eax = ScanResults.Proc_6_13_8AEBA0(Me, Mid(0, 1, InStr(1, 0, var_004A353C, 0)), 004A187Ch, var_38, var_3C, 00000000h, var_40, var_3C)
  loc_008AE81C: var_18 = var_18 & var_38
  loc_008AE859: 'Referenced from: 008AE634
  loc_008AE864: 00000001h = 00000001h + var_1C
  loc_008AE871: GoTo loc_008AE585
  loc_008AE876: 'Referenced from: 008AE587
  loc_008AE87B: If var_28 <= 0 Then GoTo loc_008AE9B6
  loc_008AE888: var_eax = ScanResults.Proc_6_12_8AEA40(Me, var_18)
  loc_008AE8BF: MsgForm.Caption = "Frequencies copied to clipboard"
  loc_008AE90F: var_eax = Unknown_VTable_Call[eax+00000054h]
  loc_008AE992: var_eax = MsgForm.Show var_74
  loc_008AE9B6: 'Referenced from: 008AE87B
  loc_008AE9B8: 'Referenced from: 008AE577
  loc_008AE9C0: GoTo loc_008AEA02
  loc_008AEA01: Exit Sub
  loc_008AEA02: 'Referenced from: 008AE9C0
  loc_008AEA0B: Exit Sub
End Sub

Private Sub Command4_Click() '8AD8C0
  Dim var_38 As Variant
  Dim var_40 As Variant
  Dim var_3C As ComboBox
  loc_008AD950: var_eax = Unknown_VTable_Call[ecx+00000040h]
  loc_008AD981: var_94 = List1.ListCount
  loc_008AD9AC: var_1C = var_94
  loc_008AD9BA: If var_94 <= 0 Then GoTo loc_008AE406
  loc_008AD9FD: var_eax = Unknown_VTable_Call[ecx+00000040h]
  loc_008ADA19: var_eax = List1.Clear
  loc_008ADA51: If 00000001h > 0 Then GoTo loc_008ADD97
  loc_008ADA79: var_eax = Unknown_VTable_Call[eax+00000040h]
  loc_008ADA9A: 00000001h = 00000001h - 00000001h
  loc_008ADAA4: var_CC = var_38
  loc_008ADAB8: var_ret_1 = List1.Selected
  loc_008ADADF: setz dl
  loc_008ADAFA: If edx = 0 Then GoTo loc_008ADD80
  loc_008ADB22: var_eax = Unknown_VTable_Call[ecx+00000040h]
  loc_008ADB41: var_18 = var_18 - 00000001h
  loc_008ADB53: var_24 = List1.List(var_18)
  loc_008ADBA1: If InStr(1, var_24, var_004A353C, 0) <= 0 Then GoTo loc_008ADD80
  loc_008ADBC9: var_eax = Unknown_VTable_Call[eax+00000040h]
  loc_008ADBE8: var_18 = var_18 - 00000001h
  loc_008ADBFA: var_24 = List1.List(var_18)
  loc_008ADC1B: var_48 = var_24
  loc_008ADC2B: InStr(1, var_24, var_004A353C, 0) = InStr(1, var_24, var_004A353C, 0) - 00000001h
  loc_008ADC35: var_58 = InStr(1, var_24, var_004A353C, 0)
  loc_008ADC88: var_eax = ScanResults.Proc_6_13_8AEBA0(Me, Mid(0, 1, InStr(1, 0, var_004A353C, 0)), 004A187Ch, var_30, Me, 00000000h, var_38, var_34)
  loc_008ADCAF: var_eax = Unknown_VTable_Call[ecx+00000350h]
  loc_008ADCCB: var_eax = Unknown_VTable_Call[eax+00000040h]
  loc_008ADD15: var_eax = List1.AddItem var_30, var_84
  loc_008ADD80: 'Referenced from: 008ADAFA
  loc_008ADD88: 00000001h = 00000001h + var_18
  loc_008ADD92: GoTo loc_008ADA4B
  loc_008ADD97: 'Referenced from: 008ADA51
  loc_008ADDB8: var_eax = Unknown_VTable_Call[ecx+0000038Ch]
  loc_008ADDD4: var_eax = Unknown_VTable_Call[eax+00000040h]
  loc_008ADE0D: var_eax = Unknown_VTable_Call[ecx+00000040h]
  loc_008ADE30: var_94 = Check1.Value
  loc_008ADE54: Check1.Value = var_94
  loc_008ADEAA: var_eax = Unknown_VTable_Call[ecx+0000038Ch]
  loc_008ADEC6: var_eax = Unknown_VTable_Call[eax+00000040h]
  loc_008ADEFF: var_eax = Unknown_VTable_Call[ecx+00000040h]
  loc_008ADF22: var_94 = Check1.Value
  loc_008ADF46: Check1.Value = var_94
  loc_008ADF9C: var_eax = Unknown_VTable_Call[ecx+000003CCh]
  loc_008ADFBA: var_eax = Unknown_VTable_Call[eax+00000040h]
  loc_008ADFDB: Check1.MousePointer = var_004A1CA0
  loc_008AE041: var_eax = Unknown_VTable_Call[ecx+00000040h]
  loc_008AE07B: var_94 = Combo2.ListIndex
  loc_008AE09F: Combo2.ListIndex = var_94
  loc_008AE0F1: var_eax = Unknown_VTable_Call[ecx+000003C4h]
  loc_008AE10F: var_eax = Unknown_VTable_Call[eax+00000040h]
  loc_008AE12F: var_24 = Combo2.Text
  loc_008AE15E: esi = (var_24 = "Custom") + 1
  loc_008AE185: If (var_24 = "Custom") + 1 = 0 Then GoTo loc_008AE2FD
  loc_008AE1BF: var_eax = Unknown_VTable_Call[ecx+00000040h]
  loc_008AE1DD: Combo2.Enabled = True
  loc_008AE22B: var_eax = Unknown_VTable_Call[ecx+000003CCh]
  loc_008AE243: var_eax = Unknown_VTable_Call[eax+00000040h]
  loc_008AE27C: var_eax = Unknown_VTable_Call[ecx+00000040h]
  loc_008AE29C: var_24 = Text19.Text
  loc_008AE2BD: Text19.Text = var_24
  loc_008AE2FB: GoTo loc_008AE37C
  loc_008AE2FD: 'Referenced from: 008AE185
  loc_008AE319: var_eax = Unknown_VTable_Call[ecx+000003CCh]
  loc_008AE324: call var_40(var_34, Unknown_VTable_Call[ecx+000003CCh], var_008D9020, Me, Unknown_VTable_Call[ecx+000003CCh], 00000002h, var_40, var_3C, Unknown_VTable_Call[ecx+000003CCh])
  loc_008AE331: var_eax = Unknown_VTable_Call[eax+00000040h]
  loc_008AE34F: Text19.Visible = False
  loc_008AE37C: 'Referenced from: 008AE2FB
  loc_008AE3B9: var_eax = Unknown_VTable_Call[ecx+00000040h]
  loc_008AE3D7: Text19.TabIndex = CInt(-1)
  loc_008AE406: 'Referenced from: 008AD9BA
  loc_008AE40E: GoTo loc_008AE458
  loc_008AE457: Exit Sub
  loc_008AE458: 'Referenced from: 008AE40E
  loc_008AE458: Exit Sub
End Sub

Private Sub List1_KeyPress(KeyAscii As Integer) '8AD3F0
  loc_008AD437: If arg_10 <> 1 Then GoTo loc_008AD442
  loc_008AD43C: var_eax = ScanResults.Proc_6_9_8AD470(Me, edi)
  loc_008AD442: 'Referenced from: 008AD437
End Sub

Public Sub Proc_6_9_8AD470
  loc_008AD4E9: var_24 = List1.ListCount
  loc_008AD522: var_18 = var_24 - 0001h
  loc_008AD530: If var_24 = 0 Then GoTo loc_008AD5D2
  loc_008AD53F: If edi > 0 Then GoTo loc_008AD5D2
  loc_008AD585: var_ret_1 = edi
  loc_008AD58D: List1.Selected = var_ret_1
  loc_008AD5C7: var_38 = var_38 + edi
  loc_008AD5CD: GoTo loc_008AD53D
  loc_008AD5D2: 'Referenced from: 008AD530
  loc_008AD5D7: GoTo loc_008AD5ED
  loc_008AD5EC: Exit Sub
  loc_008AD5ED: 'Referenced from: 008AD5D7
  loc_008AD5ED: Exit Sub
End Sub

Public Sub Proc_6_10_8AD610
  loc_008AD689: var_24 = List1.ListCount
  loc_008AD6C2: var_18 = var_24 - 0001h
  loc_008AD6D0: If var_24 = 0 Then GoTo loc_008AD772
  loc_008AD6DF: If edi > 0 Then GoTo loc_008AD772
  loc_008AD725: var_ret_1 = edi
  loc_008AD72D: List1.Selected = var_ret_1
  loc_008AD767: var_38 = var_38 + edi
  loc_008AD76D: GoTo loc_008AD6DD
  loc_008AD772: 'Referenced from: 008AD6D0
  loc_008AD777: GoTo loc_008AD78D
  loc_008AD78C: Exit Sub
  loc_008AD78D: 'Referenced from: 008AD777
  loc_008AD78D: Exit Sub
End Sub

Public Sub Proc_6_11_8AD7B0
  Dim var_14 As Me
  loc_008AD7E9: Set var_14 = arg_C
  loc_008AD82B: var_18 = var_14.MousePointer
  loc_008AD85C: var_14.FontTransparent = Len(var_18)
  loc_008AD887: GoTo loc_008AD893
  loc_008AD892: Exit Sub
  loc_008AD893: 'Referenced from: 008AD887
End Sub

Public Sub Proc_6_12_8AEA40
  Dim var_18 As Clipboard
  loc_008AEA77: var_14 = arg_C
  loc_008AEAA2: var_18 = Global.Clipboard
  loc_008AEACC: var_eax = Global.Clear
  loc_008AEB0E: var_18 = Global.Clipboard
  loc_008AEB4E: var_eax = Global.SetText var_14, var_1C
  loc_008AEB74: GoTo loc_008AEB80
  loc_008AEB7F: Exit Sub
  loc_008AEB80: 'Referenced from: 008AEB74
End Sub

Public Sub Proc_6_13_8AEBA0
  loc_008AEBF0: var_24 = Len(arg_C)
  loc_008AEBF3: If Len(arg_C) <> 0 Then GoTo loc_008AEC0D
  loc_008AEBFD: var_18 = vbNullString
  loc_008AEC08: GoTo loc_008AECE4
  loc_008AEC0D: 'Referenced from: 008AEBF3
  loc_008AEC1D: If 00000001h > 0 Then GoTo loc_008AECAE
  loc_008AEC2E: var_50 = arg_C
  loc_008AEC60: var_20 = Mid(arg_C, 1, 1)
  loc_008AEC84: If (var_20 = arg_10) = 0 Then GoTo loc_008AEC9B
  loc_008AEC99: var_28 = var_28 & var_20
  loc_008AEC9B: 'Referenced from: 008AEC84
  loc_008AECA0: 00000001h = 00000001h + 00000001h
  loc_008AECA9: GoTo loc_008AEC1B
  loc_008AECAE: 'Referenced from: 008AEC1D
  loc_008AECB4: var_18 = var_28
  loc_008AECBF: GoTo loc_008AECE4
  loc_008AECC5: If var_4 = 0 Then GoTo loc_008AECD0
  loc_008AECD0: 'Referenced from: 008AECC5
  loc_008AECE3: Exit Sub
  loc_008AECE4: 'Referenced from: 008AEC08
  loc_008AECF4: Exit Sub
End Sub

Public Sub Proc_6_14_8AED20
  loc_008AED84: var_3C = Date
  loc_008AED96: var_24 = Time
  loc_008AEDA0: var_28 = vbNullString
  loc_008AEDAD: If arg_C <> var_FFFFFF Then GoTo loc_008AEF97
  loc_008AEDF4: var_28 = Trim(Str(Year(var_3C)))
  loc_008AEE45: var_90 = (Month(var_3C) < 10)
  loc_008AEE59: If var_90 = 0 Then GoTo loc_008AEE71
  loc_008AEE6F: var_28 = var_28 & var_004A1CA0
  loc_008AEE71: 'Referenced from: 008AEE59
  loc_008AEE7C: var_84 = var_28
  loc_008AEECB: var_28 = var_28 & Trim(Str(Month(var_3C)))
  loc_008AEF1A: var_90 = (Day(var_3C) < 10)
  loc_008AEF2E: If var_90 = 0 Then GoTo loc_008AEF46
  loc_008AEF44: var_28 = var_28 & var_004A1CA0
  loc_008AEF46: 'Referenced from: 008AEF2E
  loc_008AEF51: var_84 = var_28
  loc_008AEF84: var_28 = var_28 & Day(var_3C)
  loc_008AEF95: GoTo loc_008AEFA9
  loc_008AEF97: 'Referenced from: 008AEDAD
  loc_008AEFA9: 'Referenced from: 008AEF95
  loc_008AEFB0: If arg_10 <> var_FFFFFF Then GoTo loc_008AF26A
  loc_008AEFBD: If arg_C <> var_FFFFFF Then GoTo loc_008AEFD5
  loc_008AEFD3: var_28 = var_28 & var_004A1E3C
  loc_008AEFD5: 'Referenced from: 008AEFBD
  loc_008AF00B: var_90 = (Hour(var_24) < 10)
  loc_008AF01F: If var_90 = 0 Then GoTo loc_008AF037
  loc_008AF035: var_28 = var_28 & var_004A1CA0
  loc_008AF037: 'Referenced from: 008AF01F
  loc_008AF042: var_84 = var_28
  loc_008AF091: var_28 = var_28 & Trim(Str(Hour(var_24)))
  loc_008AF0E0: var_90 = (Minute(var_24) < 10)
  loc_008AF0F4: If var_90 = 0 Then GoTo loc_008AF10C
  loc_008AF10A: var_28 = var_28 & var_004A1CA0
  loc_008AF10C: 'Referenced from: 008AF0F4
  loc_008AF117: var_84 = var_28
  loc_008AF166: var_28 = var_28 & Trim(Str(Minute(var_24)))
  loc_008AF193: var_28 = var_28 & var_004A1E3C
  loc_008AF1CB: var_90 = (Second(var_24) < 10)
  loc_008AF1DF: If var_90 = 0 Then GoTo loc_008AF1F7
  loc_008AF1F5: var_28 = var_28 & var_004A1CA0
  loc_008AF1F7: 'Referenced from: 008AF1DF
  loc_008AF202: var_84 = var_28
  loc_008AF251: var_28 = var_28 & Trim(Str(Second(var_24)))
  loc_008AF26A: 'Referenced from: 008AEFB0
  loc_008AF26F: GoTo loc_008AF29C
  loc_008AF275: If var_4 = 0 Then GoTo loc_008AF280
  loc_008AF280: 'Referenced from: 008AF275
  loc_008AF29B: Exit Sub
  loc_008AF29C: 'Referenced from: 008AF26F
End Sub
