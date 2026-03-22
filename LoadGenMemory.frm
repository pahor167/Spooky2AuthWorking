VERSION 5.00
Begin VB.Form LoadGenMemory
  Caption = "Load Generator"
  ScaleMode = 1
  AutoRedraw = False
  FontTransparent = True
  'Icon = n/a
  LinkTopic = "Form1"
  ClientLeft = 60
  ClientTop = 510
  ClientWidth = 13965
  ClientHeight = 11850
  BeginProperty Font
    Name = "Arial"
    Size = 11.25
    Charset = 0
    Weight = 400
    Underline = 0 'False
    Italic = 0 'False
    Strikethrough = 0 'False
  EndProperty
  StartUpPosition = 2 'CenterScreen
  Begin TextBox Text1
    Left = 120
    Top = 9360
    Width = 13695
    Height = 2175
    Visible = 0   'False
    TabIndex = 24
    MultiLine = -1  'True
    ScrollBars = 2
    ToolTipText = "Commands to send to the main form"
  End
  Begin TextBox NewProgram
    Left = 10440
    Top = 1680
    Width = 2535
    Height = 405
    Visible = 0   'False
    TabIndex = 23
  End
  Begin TextBox NewGate
    Left = 9960
    Top = 2160
    Width = 3735
    Height = 405
    Visible = 0   'False
    TabIndex = 22
  End
  Begin TextBox NewWaveform
    Left = 240
    Top = 6960
    Width = 13455
    Height = 2055
    Visible = 0   'False
    TabIndex = 21
    ScrollBars = 1
    ToolTipText = "Waveform"
  End
  Begin CommandButton Command1
    Left = 9840
    Top = 1680
    Width = 495
    Height = 495
    TabIndex = 16
    Picture = "LoadGenMemory.frx":0
    ToolTipText = "Delete selected program"
    Style = 1
  End
  Begin CommandButton Command2
    Caption = "Save"
    Left = 12720
    Top = 240
    Width = 975
    Height = 495
    TabIndex = 15
    ToolTipText = "Save programs"
  End
  Begin TextBox NewName
    BackColor = &HFFFFFF&
    Left = 1080
    Top = 960
    Width = 8775
    Height = 405
    TabIndex = 4
  End
  Begin ListBox ProgramNames
    BackColor = &H8000000F&
    Left = 1080
    Top = 1680
    Width = 8775
    Height = 9750
    TabIndex = 0
  End
  Begin ActiveResize ActiveResize1
  End
  Begin Image Image1
    Picture = "LoadGenMemory.frx":78F
    Left = 10920
    Top = 1080
    Width = 2175
    Height = 2130
    Stretch = -1  'True
  End
  Begin Label Label1
    Index = 16
    Left = 12240
    Top = 5520
    Width = 1575
    Height = 375
    TabIndex = 20
  End
  Begin Label Label1
    Index = 15
    Left = 12240
    Top = 5160
    Width = 1575
    Height = 375
    TabIndex = 19
  End
  Begin Label Label1
    Caption = "Gate Off (mS)"
    Index = 14
    Left = 9960
    Top = 5520
    Width = 2175
    Height = 375
    TabIndex = 18
    Alignment = 1 'Right Justify
  End
  Begin Label Label1
    Caption = "Gate On (mS)"
    Index = 13
    Left = 9960
    Top = 5160
    Width = 2175
    Height = 375
    TabIndex = 17
    Alignment = 1 'Right Justify
  End
  Begin Label Label1
    Index = 12
    Left = 12240
    Top = 6240
    Width = 1575
    Height = 375
    TabIndex = 14
  End
  Begin Label Label1
    Index = 11
    Left = 12240
    Top = 5880
    Width = 1575
    Height = 375
    TabIndex = 13
  End
  Begin Label Label1
    Index = 10
    Left = 12240
    Top = 4800
    Width = 1575
    Height = 375
    TabIndex = 12
  End
  Begin Label Label1
    Index = 9
    Left = 12240
    Top = 4440
    Width = 1575
    Height = 375
    TabIndex = 11
  End
  Begin Label Label1
    Index = 8
    Left = 12240
    Top = 4080
    Width = 1575
    Height = 375
    TabIndex = 10
  End
  Begin Label Label1
    Caption = "Total Run Time"
    Index = 2
    Left = 9840
    Top = 6240
    Width = 2295
    Height = 375
    TabIndex = 9
    Alignment = 1 'Right Justify
  End
  Begin Label Label1
    Caption = "Frequency Count"
    Index = 7
    Left = 9960
    Top = 5880
    Width = 2175
    Height = 375
    TabIndex = 8
    Alignment = 1 'Right Justify
  End
  Begin Label Label1
    Caption = "Dwell (S)"
    Index = 6
    Left = 9960
    Top = 4800
    Width = 2175
    Height = 375
    TabIndex = 7
    Alignment = 1 'Right Justify
  End
  Begin Label Label1
    Caption = "Offset (%)"
    Index = 5
    Left = 9960
    Top = 4440
    Width = 2175
    Height = 375
    TabIndex = 6
    Alignment = 1 'Right Justify
  End
  Begin Label Label1
    Caption = "Amplitude (V)"
    Index = 4
    Left = 9960
    Top = 4080
    Width = 2175
    Height = 375
    TabIndex = 5
    Alignment = 1 'Right Justify
  End
  Begin Label Label1
    Index = 3
    Left = 12240
    Top = 3360
    Width = 735
    Height = 375
    Visible = 0   'False
    TabIndex = 3
  End
  Begin Label Label1
    Caption = "New Program Name"
    Index = 1
    Left = 3480
    Top = 600
    Width = 3495
    Height = 375
    TabIndex = 2
    Alignment = 2 'Center
  End
  Begin Label Label1
    Caption = "Generator #"
    Index = 0
    Left = 9960
    Top = 3360
    Width = 2175
    Height = 375
    Visible = 0   'False
    TabIndex = 1
    Alignment = 1 'Right Justify
  End
End

Attribute VB_Name = "LoadGenMemory"


Private Sub Command1_Click() '8C3810
  loc_008C3853: var_eax = LoadGenMemory.Proc_21_5_8C4C20(Me, edi)
End Sub

Private Sub Form_QueryUnload(Cancel As Integer, UnloadMode As Integer) '8C50D0
  loc_008C5117: If UnloadMode <> 0 Then GoTo loc_008C5121
  loc_008C5121: 'Referenced from: 008C5117
  loc_008C5143: var_eax = LoadGenMemory.Hide
End Sub

Private Sub Command2_Click() '8C3880
  Dim var_20 As TextBox
  loc_008C38F0: var_1C = Text1.Text
  loc_008C3923: esi = (var_1C = vbNullString) + 1
  loc_008C393B: If (var_1C = vbNullString) + 1 = 0 Then GoTo loc_008C3A01
  loc_008C3949: 
  loc_008C3950: If 00000001h > 30 Then GoTo loc_008C3A01
  loc_008C3970: var_18 = var_18 - 00000001h
  loc_008C3982: var_1C = ProgramNames.List(var_18)
  loc_008C39AF: setle dl
  loc_008C39CC: If edx <> 0 Then GoTo loc_008C39EC
  loc_008C39DC: 00000001h = 00000001h + var_18
  loc_008C39E7: GoTo loc_008C3949
  loc_008C39EC: 'Referenced from: 008C39CC
  loc_008C39F3: var_eax = LoadGenMemory.Proc_21_4_8C3D20(Me, var_18, var_20, 00000001h, Me, var_20)
  loc_008C39FF: GoTo loc_008C3A6B
  loc_008C3A01: 'Referenced from: 008C393B
  loc_008C3A1A: var_1C = Text1.Text
  loc_008C3A4D: esi = (var_1C = vbNullString) + 1
  loc_008C3A65: If (var_1C <> vbNullString) + 1 <> 0 Then GoTo loc_008C3BC4
  loc_008C3A6B: 'Referenced from: 008C39FF
  loc_008C3AA4: var_eax = Unknown_VTable_Call[eax+00000040h]
  loc_008C3ADC: var_1C = Text1.Text
  loc_008C3B01: Text1.Text = var_1C
  loc_008C3B5D: Text1.Text = vbNullString
  loc_008C3BA6: var_eax = LoadGenMemory.Hide
  loc_008C3BC4: 'Referenced from: 008C3A65
  loc_008C3BD0: GoTo loc_008C3BF3
  loc_008C3BF2: Exit Sub
  loc_008C3BF3: 'Referenced from: 008C3BD0
End Sub

Private Sub ProgramNames_DblClick() '8C3C20
  Dim var_1C As ListBox
  loc_008C3C88: var_20 = ProgramNames.ListIndex
  loc_008C3CB3: var_18 = var_20 + 0001h
  loc_008C3CC5: If var_18 < 1 Then GoTo loc_008C3CD9
  loc_008C3CCA: If var_18 > 30 Then GoTo loc_008C3CD9
  loc_008C3CD3: var_eax = LoadGenMemory.Proc_21_4_8C3D20(Me, var_18, var_1C, Me)
  loc_008C3CD9: 'Referenced from: 008C3CC5
  loc_008C3CE1: GoTo loc_008C3CED
  loc_008C3CEC: Exit Sub
  loc_008C3CED: 'Referenced from: 008C3CE1
  loc_008C3CED: Exit Sub
End Sub

Public Sub Proc_21_4_8C3D20
  Dim var_30 As TextBox
  Dim var_80 As TextBox
  Dim var_78 As Variant
  Dim var_E0 As TextBox
  Dim var_E8 As TextBox
  loc_008C3DE6: var_30 = NewName.Text
  loc_008C3E21: var_34 = NewProgram.Text
  loc_008C3E5C: var_38 = NewGate.Text
  loc_008C3E9A: var_3C = NewWaveform.Text
  loc_008C3ED1: edi = (var_3C = vbNullString) + 1
  loc_008C3EE3: eax = (var_38 = vbNullString) + 1
  loc_008C3EFE: eax = (var_34 = vbNullString) + 1
  loc_008C3F13: eax = (var_30 = vbNullString) + 1
  loc_008C3F51: If (var_30 <> vbNullString) + 1 <> 0 Then GoTo loc_008C4B2E
  loc_008C3F73: var_eax = LoadGenMemory.Proc_21_6_8C5190(Me, arg_C, 2)
  loc_008C3F88: var_1C = var_30
  loc_008C3FAD: 3 = Label1.FormatLength
  loc_008C3FDB: var_eax = Unknown_VTable_Call[ecx+00000050h]
  loc_008C404A: var_eax = LoadGenMemory.Proc_21_6_8C5190(Me, CLng((var_D4 - 1)), 3, var_34, var_7C, var_30)
  loc_008C4059: var_2C = var_34
  loc_008C407E: ecx = ecx + 00000028h
  loc_008C40AA: var_eax = LoadGenMemory.Proc_21_6_8C5190(Me, var_30, 2, var_30)
  loc_008C40B9: var_28 = var_30
  loc_008C40C3: var_24 = "120"
  loc_008C40D1: var_18 = "2000"
  loc_008C40F4: var_D8 = var_78
  loc_008C40FA: 10 = Label1.FormatLength
  loc_008C41A0: var_20 = Trim(Str(Int(Val(var_30))))
  loc_008C41EC: If (var_20 <> var_004A1CA0) <> 0 Then GoTo loc_008C41FC
  loc_008C41FC: 'Referenced from: 008C41EC
  loc_008C421F: 11 = Label1.FormatLength
  loc_008C424D: var_eax = Unknown_VTable_Call[ecx+00000050h]
  loc_008C4274: var_14 = var_30
  loc_008C42AA: var_30 = NewName.Text
  loc_008C42FA: var_8C = var_30
  loc_008C4342: var_34 = CStr(Mid(var_30, 1, 59))
  loc_008C4352: NewName.Text = var_34
  loc_008C43BF: var_E0 = var_94
  loc_008C43DE: var_30 = NewName.Text
  loc_008C442A: var_38 = var_1C & ": " & var_30
  loc_008C4431: ecx = ecx - 00000001h
  loc_008C443B: var_ret_2 = ecx-00000001h
  loc_008C444B: NewName.LinkTopic = var_ret_2
  loc_008C44A9: var_E8 = var_80
  loc_008C44C5: var_30 = Text1.Text
  loc_008C44EA: edx = edx + 00000028h
  loc_008C4516: var_eax = LoadGenMemory.Proc_21_6_8C5190(Me, var_38, 2, var_38, var_78, var_80, Me, var_80)
  loc_008C4539: var_44 = NewWaveform.Text
  loc_008C45DB: NewWaveform.Text = var_30 & var_2C & " :a" & var_38 & var_004A1940 & var_44 & "vbCrLf"
  loc_008C4652: var_E8 = var_78
  loc_008C4671: var_30 = Text1.Text
  loc_008C46AC: var_40 = NewName.Text
  loc_008C4742: var_4C = var_30 & var_2C & " :n" & var_1C & var_004A1940 & var_40 & "vbCrLf"
  loc_008C474E: NewName.Text = var_4C
  loc_008C47C1: var_E8 = var_78
  loc_008C47E0: var_30 = Text1.Text
  loc_008C481B: var_40 = NewGate.Text
  loc_008C48B1: var_4C = var_30 & var_2C & " :g" & var_1C & var_004A1940 & var_40 & "vbCrLf"
  loc_008C48BD: NewGate.Text = var_4C
  loc_008C4930: var_E8 = var_78
  loc_008C494F: var_30 = Text1.Text
  loc_008C498A: var_68 = NewProgram.Text
  loc_008C4A56: var_5C = var_30 & var_2C & " :p" & var_1C & var_004A1940 & var_28 & var_004A187C & var_18 & var_004A187C & var_24 & var_004A187C & var_20
  loc_008C4A9F: var_74 = var_5C & var_004A187C & var_14 & var_004A187C & var_68 & "vbCrLf"
  loc_008C4AA9: NewProgram.Text = var_74
  loc_008C4B2E: 'Referenced from: 008C3F51
  loc_008C4B34: GoTo loc_008C4BC8
  loc_008C4BC7: Exit Sub
  loc_008C4BC8: 'Referenced from: 008C4B34
  loc_008C4BF1: Exit Sub
End Sub

Public Sub Proc_21_5_8C4C20
  Dim var_3C As TextBox
  Dim var_5C As TextBox
  Dim var_38 As TextBox
  loc_008C4C92: var_40 = ProgramNames.ListIndex
  loc_008C4CB7: var_18 = var_40
  loc_008C4CC2: If var_40 < 0 Then GoTo loc_008C505C
  loc_008C4CCB: If var_40 >= 30 Then GoTo loc_008C505C
  loc_008C4CF3: var_eax = LoadGenMemory.Proc_21_6_8C5190(Me, var_40 + 00000001h + 00000001h, 2, var_20, var_38)
  loc_008C4D08: var_14 = var_20
  loc_008C4D2A: 3 = Label1.FormatLength
  loc_008C4D52: var_eax = Unknown_VTable_Call[ecx+00000050h]
  loc_008C4DAC: var_eax = LoadGenMemory.Proc_21_6_8C5190(Me, CLng((var_50 - 1)), 3, var_24, var_3C, var_20, var_3C, var_38)
  loc_008C4DBB: var_1C = var_24
  loc_008C4E0C: var_ret_1 = var_18
  loc_008C4E19: ProgramNames.List(var_ret_1) = var_14 & var_004A2914
  loc_008C4E62: var_5C = var_3C
  loc_008C4E7B: var_20 = Text1.Text
  loc_008C4EE9: var_30 = var_20 & var_1C & " :n" & var_14 & var_004A1940
  loc_008C4F05: Text1.Text = var_30 & "vbCrLf"
  loc_008C4F89: var_20 = Text1.Text
  loc_008C5007: var_34 = var_20 & var_1C & " :n" & var_14 & "=," & "vbCrLf"
  loc_008C500B: Text1.Text = var_34
  loc_008C505C: 'Referenced from: 008C4CC2
  loc_008C5062: GoTo loc_008C5098
  loc_008C5097: Exit Sub
  loc_008C5098: 'Referenced from: 008C5062
  loc_008C50A8: Exit Sub
End Sub

Public Sub Proc_21_6_8C5190
  loc_008C51D4: var_44 = arg_C
  loc_008C5207: var_1C = Trim(Str(arg_C))
  loc_008C5225: 
  loc_008C5231: If Len(var_1C) >= arg_10 Then GoTo loc_008C5247
  loc_008C5243: var_1C = var_004A1CA0 & var_1C
  loc_008C5245: GoTo loc_008C5225
  loc_008C5247: 'Referenced from: 008C5231
  loc_008C524C: GoTo loc_008C5271
  loc_008C5252: If var_4 = 0 Then GoTo loc_008C525D
  loc_008C525D: 'Referenced from: 008C5252
  loc_008C5270: Exit Sub
  loc_008C5271: 'Referenced from: 008C524C
End Sub
