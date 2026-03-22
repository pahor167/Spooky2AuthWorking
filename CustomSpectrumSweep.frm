VERSION 5.00
Begin VB.Form CustomSpectrumSweep
  Caption = "Create Spectrum Sweep"
  ScaleMode = 1
  AutoRedraw = False
  FontTransparent = True
  Icon = "CustomSpectrumSweep.frx":0
  LinkTopic = "Form2"
  ClientLeft = 60
  ClientTop = 510
  ClientWidth = 21165
  ClientHeight = 11355
  BeginProperty Font
    Name = "Arial"
    Size = 9.75
    Charset = 0
    Weight = 400
    Underline = 0 'False
    Italic = 0 'False
    Strikethrough = 0 'False
  EndProperty
  StartUpPosition = 1 'CenterOwner
  Begin OptionButton Option1
    Index = 2
    Left = 240
    Top = 6840
    Width = 255
    Height = 255
    TabIndex = 29
    ToolTipText = "The most thorough sweep. Combines 4 sweeps, each focussing on different quarters of the sweep span"
  End
  Begin OptionButton Option1
    Index = 1
    Left = 240
    Top = 6480
    Width = 255
    Height = 255
    TabIndex = 28
    ToolTipText = "Combines 2 Spectrum sweeps to hit pathogens from both sides"
  End
  Begin OptionButton Option1
    Index = 0
    Left = 240
    Top = 6120
    Width = 255
    Height = 255
    TabIndex = 27
    Value = 255
    ToolTipText = "Simple sweep that targets multiple frequencies simultaneously"
  End
  Begin TextBox Text6
    ForeColor = &H0&
    Left = 3720
    Top = 4920
    Width = 855
    Height = 345
    TabIndex = 15
    Alignment = 2 'Center
    BeginProperty Font
      Name = "Arial"
      Size = 11.25
      Charset = 0
      Weight = 400
      Underline = 0 'False
      Italic = 0 'False
      Strikethrough = 0 'False
    EndProperty
    Appearance = 0 'Flat
    ToolTipText = "During a single sweep each frequency can be targeted multiple times. This is good for hitting very low frequencies"
  End
  Begin TextBox Text5
    ForeColor = &H0&
    Left = 3720
    Top = 4440
    Width = 855
    Height = 405
    TabIndex = 14
    Alignment = 2 'Center
    BeginProperty Font
      Name = "Arial"
      Size = 11.25
      Charset = 0
      Weight = 400
      Underline = 0 'False
      Italic = 0 'False
      Strikethrough = 0 'False
    EndProperty
    Appearance = 0 'Flat
    ToolTipText = "Specify how many frequencies are simultaneously produced either side of the main frequency"
  End
  Begin TextBox Text4
    ForeColor = &H0&
    Left = 3720
    Top = 3960
    Width = 855
    Height = 405
    TabIndex = 13
    Alignment = 2 'Center
    BeginProperty Font
      Name = "Arial"
      Size = 11.25
      Charset = 0
      Weight = 400
      Underline = 0 'False
      Italic = 0 'False
      Strikethrough = 0 'False
    EndProperty
    Appearance = 0 'Flat
    ToolTipText = "Enter the time that each frequency must be applied during this sweep"
  End
  Begin TextBox Text3
    ForeColor = &H0&
    Left = 3720
    Top = 3480
    Width = 855
    Height = 405
    TabIndex = 12
    Alignment = 2 'Center
    BeginProperty Font
      Name = "Arial"
      Size = 11.25
      Charset = 0
      Weight = 400
      Underline = 0 'False
      Italic = 0 'False
      Strikethrough = 0 'False
    EndProperty
    Appearance = 0 'Flat
    ToolTipText = "Enter the acceptable frequency tolerance here"
  End
  Begin TextBox Text2
    ForeColor = &H0&
    Left = 3720
    Top = 3000
    Width = 1695
    Height = 345
    TabIndex = 11
    BeginProperty Font
      Name = "Arial"
      Size = 11.25
      Charset = 0
      Weight = 400
      Underline = 0 'False
      Italic = 0 'False
      Strikethrough = 0 'False
    EndProperty
    Appearance = 0 'Flat
    ToolTipText = "Enter the highest frequency you wish to target with this sweep"
  End
  Begin TextBox Text1
    ForeColor = &H0&
    Left = 3720
    Top = 2520
    Width = 1695
    Height = 345
    TabIndex = 10
    BeginProperty Font
      Name = "Arial"
      Size = 11.25
      Charset = 0
      Weight = 400
      Underline = 0 'False
      Italic = 0 'False
      Strikethrough = 0 'False
    EndProperty
    Appearance = 0 'Flat
    ToolTipText = "Enter the lowest frequency you wish to target with this sweep"
  End
  Begin CommandButton Command5
    Left = 20520
    Top = 120
    Width = 495
    Height = 525
    TabIndex = 2
    BeginProperty Font
      Name = "Arial"
      Size = 11.25
      Charset = 0
      Weight = 400
      Underline = 0 'False
      Italic = 0 'False
      Strikethrough = 0 'False
    EndProperty
    Picture = "CustomSpectrumSweep.frx":FD55
    ToolTipText = "Add the program to the Custom database"
    Style = 1
  End
  Begin TextBox Text7
    BackColor = &HFFFFFF&
    ForeColor = &H0&
    Left = 240
    Top = 1920
    Width = 13215
    Height = 375
    TabIndex = 0
    BeginProperty Font
      Name = "Arial"
      Size = 11.25
      Charset = 0
      Weight = 400
      Underline = 0 'False
      Italic = 0 'False
      Strikethrough = 0 'False
    EndProperty
    Appearance = 0 'Flat
    ToolTipText = "Give your Custom Spectrum Sweep a descriptive name"
  End
  Begin ActiveResize ActiveResize1
  End
  Begin Label Label30
    Caption = "Database Entry View"
    Index = 25
    BackColor = &HFFFFFF&
    ForeColor = &H0&
    Left = 240
    Top = 8040
    Width = 2415
    Height = 375
    TabIndex = 37
    BackStyle = 0 'Transparent
    BeginProperty Font
      Name = "Arial"
      Size = 11.25
      Charset = 0
      Weight = 400
      Underline = 0 'False
      Italic = 0 'False
      Strikethrough = 0 'False
    EndProperty
  End
  Begin Label Label30
    Caption = "Spectrum Sweep Info"
    Index = 24
    BackColor = &HFFFFFF&
    ForeColor = &H0&
    Left = 8880
    Top = 2520
    Width = 4935
    Height = 375
    TabIndex = 36
    BackStyle = 0 'Transparent
    BeginProperty Font
      Name = "Arial"
      Size = 11.25
      Charset = 0
      Weight = 400
      Underline = 0 'False
      Italic = 0 'False
      Strikethrough = 0 'False
    EndProperty
  End
  Begin Label Label30
    Caption = "X"
    Index = 23
    BackColor = &HFFFFFF&
    ForeColor = &H0&
    Left = 4800
    Top = 4560
    Width = 375
    Height = 255
    TabIndex = 35
    BackStyle = 0 'Transparent
    BeginProperty Font
      Name = "Arial"
      Size = 11.25
      Charset = 0
      Weight = 400
      Underline = 0 'False
      Italic = 0 'False
      Strikethrough = 0 'False
    EndProperty
  End
  Begin Label Label30
    Caption = "Secs"
    Index = 11
    BackColor = &HFFFFFF&
    ForeColor = &H0&
    Left = 4800
    Top = 4080
    Width = 615
    Height = 255
    TabIndex = 34
    BackStyle = 0 'Transparent
    BeginProperty Font
      Name = "Arial"
      Size = 11.25
      Charset = 0
      Weight = 400
      Underline = 0 'False
      Italic = 0 'False
      Strikethrough = 0 'False
    EndProperty
  End
  Begin Label Label30
    Index = 22
    BackColor = &HFFFFFF&
    ForeColor = &H0&
    Left = 120
    Top = 8520
    Width = 20895
    Height = 2655
    TabIndex = 33
    BorderStyle = 1 'Fixed Single
    BeginProperty Font
      Name = "Arial"
      Size = 11.25
      Charset = 0
      Weight = 400
      Underline = 0 'False
      Italic = 0 'False
      Strikethrough = 0 'False
    EndProperty
  End
  Begin Label Label30
    Caption = "CustomSpectrumSweep.frx":10567
    Index = 18
    BackColor = &HFFFFFF&
    ForeColor = &H0&
    Left = 240
    Top = 240
    Width = 17055
    Height = 375
    TabIndex = 32
    BackStyle = 0 'Transparent
    BeginProperty Font
      Name = "Arial"
      Size = 11.25
      Charset = 0
      Weight = 400
      Underline = 0 'False
      Italic = 0 'False
      Strikethrough = 0 'False
    EndProperty
  End
  Begin Image Image1
    Picture = "CustomSpectrumSweep.frx":105EE
    Left = 6360
    Top = 2760
    Width = 2175
    Height = 2130
    Stretch = -1  'True
  End
  Begin Label Label30
    Caption = "Single Spectrum Sweep - uses OUT1"
    Index = 17
    BackColor = &HFFFFFF&
    ForeColor = &H0&
    Left = 600
    Top = 6120
    Width = 15255
    Height = 375
    TabIndex = 31
    BackStyle = 0 'Transparent
    BeginProperty Font
      Name = "Arial"
      Size = 11.25
      Charset = 0
      Weight = 400
      Underline = 0 'False
      Italic = 0 'False
      Strikethrough = 0 'False
    EndProperty
  End
  Begin Label Label30
    Caption = "4 x Quarter Weighted Sweeps - uses both OUTs"
    Index = 13
    BackColor = &HFFFFFF&
    ForeColor = &H0&
    Left = 600
    Top = 6840
    Width = 15255
    Height = 375
    TabIndex = 30
    BackStyle = 0 'Transparent
    BeginProperty Font
      Name = "Arial"
      Size = 11.25
      Charset = 0
      Weight = 400
      Underline = 0 'False
      Italic = 0 'False
      Strikethrough = 0 'False
    EndProperty
  End
  Begin Label Label30
    Caption = "Sweep Info"
    Index = 21
    BackColor = &HFFFFFF&
    ForeColor = &H0&
    Left = 8880
    Top = 5160
    Width = 6135
    Height = 375
    TabIndex = 26
    BackStyle = 0 'Transparent
    BeginProperty Font
      Name = "Arial"
      Size = 11.25
      Charset = 0
      Weight = 400
      Underline = 0 'False
      Italic = 0 'False
      Strikethrough = 0 'False
    EndProperty
  End
  Begin Label Label30
    Caption = "Sweep Info"
    Index = 20
    BackColor = &HFFFFFF&
    ForeColor = &H0&
    Left = 8880
    Top = 4800
    Width = 6135
    Height = 375
    TabIndex = 25
    BackStyle = 0 'Transparent
    BeginProperty Font
      Name = "Arial"
      Size = 11.25
      Charset = 0
      Weight = 400
      Underline = 0 'False
      Italic = 0 'False
      Strikethrough = 0 'False
    EndProperty
  End
  Begin Label Label30
    Caption = "Sweep Info"
    Index = 19
    BackColor = &HFFFFFF&
    ForeColor = &H0&
    Left = 8880
    Top = 4440
    Width = 6135
    Height = 375
    TabIndex = 24
    BackStyle = 0 'Transparent
    BeginProperty Font
      Name = "Arial"
      Size = 11.25
      Charset = 0
      Weight = 400
      Underline = 0 'False
      Italic = 0 'False
      Strikethrough = 0 'False
    EndProperty
  End
  Begin Label Label30
    Caption = "Sweep Info"
    Index = 16
    BackColor = &HFFFFFF&
    ForeColor = &H0&
    Left = 8880
    Top = 4080
    Width = 6135
    Height = 375
    TabIndex = 23
    BackStyle = 0 'Transparent
    BeginProperty Font
      Name = "Arial"
      Size = 11.25
      Charset = 0
      Weight = 400
      Underline = 0 'False
      Italic = 0 'False
      Strikethrough = 0 'False
    EndProperty
  End
  Begin Label Label30
    Caption = "Sweep Info"
    Index = 15
    BackColor = &HFFFFFF&
    ForeColor = &H0&
    Left = 8880
    Top = 3720
    Width = 6135
    Height = 375
    TabIndex = 22
    BackStyle = 0 'Transparent
    BeginProperty Font
      Name = "Arial"
      Size = 11.25
      Charset = 0
      Weight = 400
      Underline = 0 'False
      Italic = 0 'False
      Strikethrough = 0 'False
    EndProperty
  End
  Begin Label Label30
    Caption = "Sweep Info"
    Index = 14
    BackColor = &HFFFFFF&
    ForeColor = &H0&
    Left = 8880
    Top = 3360
    Width = 6135
    Height = 375
    TabIndex = 21
    BackStyle = 0 'Transparent
    BeginProperty Font
      Name = "Arial"
      Size = 11.25
      Charset = 0
      Weight = 400
      Underline = 0 'False
      Italic = 0 'False
      Strikethrough = 0 'False
    EndProperty
  End
  Begin Label Label30
    Caption = "Sweep Info"
    Index = 12
    BackColor = &HFFFFFF&
    ForeColor = &H0&
    Left = 8880
    Top = 3000
    Width = 6135
    Height = 375
    TabIndex = 20
    BackStyle = 0 'Transparent
    BeginProperty Font
      Name = "Arial"
      Size = 11.25
      Charset = 0
      Weight = 400
      Underline = 0 'False
      Italic = 0 'False
      Strikethrough = 0 'False
    EndProperty
  End
  Begin Label Label30
    Caption = "%"
    Index = 10
    BackColor = &HFFFFFF&
    ForeColor = &H0&
    Left = 4800
    Top = 3600
    Width = 375
    Height = 255
    TabIndex = 19
    BackStyle = 0 'Transparent
    BeginProperty Font
      Name = "Arial"
      Size = 11.25
      Charset = 0
      Weight = 400
      Underline = 0 'False
      Italic = 0 'False
      Strikethrough = 0 'False
    EndProperty
  End
  Begin Label Label30
    Caption = "Hz"
    Index = 9
    BackColor = &HFFFFFF&
    ForeColor = &H0&
    Left = 5640
    Top = 3000
    Width = 615
    Height = 375
    TabIndex = 18
    BackStyle = 0 'Transparent
    BeginProperty Font
      Name = "Arial"
      Size = 11.25
      Charset = 0
      Weight = 400
      Underline = 0 'False
      Italic = 0 'False
      Strikethrough = 0 'False
    EndProperty
  End
  Begin Label Label30
    Caption = "Hz"
    Index = 8
    BackColor = &HFFFFFF&
    ForeColor = &H0&
    Left = 5640
    Top = 2520
    Width = 615
    Height = 375
    TabIndex = 17
    BackStyle = 0 'Transparent
    BeginProperty Font
      Name = "Arial"
      Size = 11.25
      Charset = 0
      Weight = 400
      Underline = 0 'False
      Italic = 0 'False
      Strikethrough = 0 'False
    EndProperty
  End
  Begin Label Label30
    Caption = "Dual Converge Sweep - uses both OUTs"
    Index = 7
    BackColor = &HFFFFFF&
    ForeColor = &H0&
    Left = 600
    Top = 6480
    Width = 15255
    Height = 375
    TabIndex = 16
    BackStyle = 0 'Transparent
    BeginProperty Font
      Name = "Arial"
      Size = 11.25
      Charset = 0
      Weight = 400
      Underline = 0 'False
      Italic = 0 'False
      Strikethrough = 0 'False
    EndProperty
  End
  Begin Label Label30
    Caption = "Wave Cycle Multiplier"
    Index = 6
    BackColor = &HFFFFFF&
    ForeColor = &H0&
    Left = 240
    Top = 4440
    Width = 3495
    Height = 375
    TabIndex = 9
    BackStyle = 0 'Transparent
    BeginProperty Font
      Name = "Arial"
      Size = 11.25
      Charset = 0
      Weight = 400
      Underline = 0 'False
      Italic = 0 'False
      Strikethrough = 0 'False
    EndProperty
  End
  Begin Label Label30
    Caption = "Frequency Hits Per Sweep"
    Index = 5
    BackColor = &HFFFFFF&
    ForeColor = &H0&
    Left = 240
    Top = 4920
    Width = 3495
    Height = 375
    TabIndex = 8
    BackStyle = 0 'Transparent
    BeginProperty Font
      Name = "Arial"
      Size = 11.25
      Charset = 0
      Weight = 400
      Underline = 0 'False
      Italic = 0 'False
      Strikethrough = 0 'False
    EndProperty
  End
  Begin Label Label30
    Caption = "Frequency Tolerance"
    Index = 4
    BackColor = &HFFFFFF&
    ForeColor = &H0&
    Left = 240
    Top = 3480
    Width = 3495
    Height = 375
    TabIndex = 7
    BackStyle = 0 'Transparent
    BeginProperty Font
      Name = "Arial"
      Size = 11.25
      Charset = 0
      Weight = 400
      Underline = 0 'False
      Italic = 0 'False
      Strikethrough = 0 'False
    EndProperty
  End
  Begin Label Label30
    Caption = "Frequency Application Time"
    Index = 3
    BackColor = &HFFFFFF&
    ForeColor = &H0&
    Left = 240
    Top = 3960
    Width = 3375
    Height = 375
    TabIndex = 6
    BackStyle = 0 'Transparent
    BeginProperty Font
      Name = "Arial"
      Size = 11.25
      Charset = 0
      Weight = 400
      Underline = 0 'False
      Italic = 0 'False
      Strikethrough = 0 'False
    EndProperty
  End
  Begin Label Label30
    Caption = "Spectrum High Frequency"
    Index = 2
    BackColor = &HFFFFFF&
    ForeColor = &H0&
    Left = 240
    Top = 3000
    Width = 3495
    Height = 375
    TabIndex = 5
    BackStyle = 0 'Transparent
    BeginProperty Font
      Name = "Arial"
      Size = 11.25
      Charset = 0
      Weight = 400
      Underline = 0 'False
      Italic = 0 'False
      Strikethrough = 0 'False
    EndProperty
  End
  Begin Label Label30
    Caption = "Spectrum Low Frequency"
    Index = 1
    BackColor = &HFFFFFF&
    ForeColor = &H0&
    Left = 240
    Top = 2520
    Width = 3495
    Height = 375
    TabIndex = 4
    BackStyle = 0 'Transparent
    BeginProperty Font
      Name = "Arial"
      Size = 11.25
      Charset = 0
      Weight = 400
      Underline = 0 'False
      Italic = 0 'False
      Strikethrough = 0 'False
    EndProperty
  End
  Begin Label Label3
    Caption = "CustomSpectrumSweep.frx":20713
    BackColor = &HFFFFFF&
    ForeColor = &H0&
    Left = 240
    Top = 1200
    Width = 18375
    Height = 375
    TabIndex = 3
    BackStyle = 0 'Transparent
    BeginProperty Font
      Name = "Arial"
      Size = 11.25
      Charset = 0
      Weight = 400
      Underline = 0 'False
      Italic = 0 'False
      Strikethrough = 0 'False
    EndProperty
  End
  Begin Label Label30
    Caption = "Program Name"
    Index = 0
    BackColor = &HFFFFFF&
    ForeColor = &H0&
    Left = 240
    Top = 1560
    Width = 2535
    Height = 375
    TabIndex = 1
    BackStyle = 0 'Transparent
    BeginProperty Font
      Name = "Arial"
      Size = 11.25
      Charset = 0
      Weight = 400
      Underline = 0 'False
      Italic = 0 'False
      Strikethrough = 0 'False
    EndProperty
  End
End

Attribute VB_Name = "CustomSpectrumSweep"


Private Sub Command5_Click() '8AF8A0
  Dim var_B4 As TextBox
  loc_008AF905: On Error Resume Next
  loc_008AF91B: var_eax = CustomSpectrumSweep.Proc_8_17_8B31A0(Me, FFFFFFFFh, edi)
  loc_008AF95B: var_28 = Text1.Text
  loc_008AF963: var_B8 = var_28
  loc_008AF9B0: eax = Len(var_28) + 1
  loc_008AF9B3: var_BC = Len(var_28) + 1
  loc_008AF9D5: If var_BC = 0 Then GoTo loc_008AFAED
  loc_008AF9FC: var_B4 = var_2C
  loc_008AFA18: Text1.BackColor = eax+00000038h
  loc_008AFA1D: var_B8 = var_B4
  loc_008AFA98: var_eax = Text1.SetFocus
  loc_008AFAA0: var_B8 = Text1.SetFocus
  loc_008AFAE8: GoTo loc_008B0ECF
  loc_008AFAED: 'Referenced from: 008AF9D5
  loc_008AFB27: var_28 = Text2.Text
  loc_008AFB2F: var_B8 = var_28
  loc_008AFB7C: eax = Len(var_28) + 1
  loc_008AFB7F: var_BC = Len(var_28) + 1
  loc_008AFBA1: If var_BC = 0 Then GoTo loc_008AFCB9
End Sub

Private Sub Text7_LostFocus() '8B1E70
  Dim var_24 As TextBox
  loc_008B1EF2: var_18 = Text7.Text
  loc_008B1F15: var_2C = var_18
  loc_008B1F2F: var_eax = CustomSpectrumSweep.Proc_8_11_8B0F40(Me, 8, var_1C, var_20, var_24, Me)
  loc_008B1F3C: Text7.Text = var_1C
  loc_008B1F8B: GoTo loc_008B1FBA
  loc_008B1FB9: Exit Sub
  loc_008B1FBA: 'Referenced from: 008B1F8B
End Sub

Private Sub Text7_Click() '8B1250
  loc_008B12B2: Text7.BackColor = esi+0000003Ch
  loc_008B12DB: GoTo loc_008B12E7
  loc_008B12E6: Exit Sub
  loc_008B12E7: 'Referenced from: 008B12DB
End Sub

Private Sub Text6_LostFocus() '8B1C70
  Dim var_30 As TextBox
  loc_008B1CF3: var_18 = Text6.Text
  loc_008B1D2E: var_1C = Text5.Text
  loc_008B1D76: var_40 = var_48
  loc_008B1DAE: var_eax = CustomSpectrumSweep.Proc_8_14_8B25D0(Me, var_18, var_3C, var_4C, var_34, var_24, var_30, eax)
  loc_008B1DBB: Text6.Text = var_24
  loc_008B1E07: var_eax = CustomSpectrumSweep.Proc_8_17_8B31A0(Me)
  loc_008B1E16: GoTo loc_008B1E48
  loc_008B1E47: Exit Sub
  loc_008B1E48: 'Referenced from: 008B1E16
End Sub

Private Sub Text4_LostFocus() '8B17F0
  loc_008B186D: var_18 = Text4.Text
  loc_008B18C9: var_1C = var_18
  loc_008B18E6: var_eax = CustomSpectrumSweep.Proc_8_14_8B25D0(Me, var_1C, var_34, var_3C, var_2C, var_20, var_28, esi)
  loc_008B18F3: Text4.Text = var_20
  loc_008B1937: var_eax = CustomSpectrumSweep.Proc_8_17_8B31A0(Me)
  loc_008B1946: GoTo loc_008B1970
  loc_008B196F: Exit Sub
  loc_008B1970: 'Referenced from: 008B1946
End Sub

Private Sub Text5_LostFocus() '8B1990
  Dim var_2C As TextBox
  loc_008B1A13: var_18 = Text5.Text
  loc_008B1A6B: var_1C = var_18
  loc_008B1A88: var_eax = CustomSpectrumSweep.Proc_8_14_8B25D0(Me, var_1C, var_3C, var_44, var_34, var_20, var_2C, esi)
  loc_008B1A95: Text5.Text = var_20
  loc_008B1AF3: var_18 = Text6.Text
  loc_008B1B2E: var_1C = Text5.Text
  loc_008B1B78: var_40 = var_48
  loc_008B1B91: var_20 = var_18
  loc_008B1BAE: var_eax = CustomSpectrumSweep.Proc_8_14_8B25D0(Me, var_20, var_3C, var_44, var_34, var_24, var_30, eax)
  loc_008B1BBB: Text6.Text = var_24
  loc_008B1C07: var_eax = CustomSpectrumSweep.Proc_8_17_8B31A0(Me)
  loc_008B1C16: GoTo loc_008B1C48
  loc_008B1C47: Exit Sub
  loc_008B1C48: 'Referenced from: 008B1C16
End Sub

Private Sub Text3_LostFocus() '8B1650
  loc_008B16CD: var_18 = Text3.Text
  loc_008B1729: var_1C = var_18
  loc_008B1746: var_eax = CustomSpectrumSweep.Proc_8_14_8B25D0(Me, var_1C, var_34, var_3C, var_2C, var_20, var_28, esi)
  loc_008B1753: Text3.Text = var_20
  loc_008B1797: var_eax = CustomSpectrumSweep.Proc_8_17_8B31A0(Me)
  loc_008B17A6: GoTo loc_008B17D0
  loc_008B17CF: Exit Sub
  loc_008B17D0: 'Referenced from: 008B17A6
End Sub

Private Sub Text2_LostFocus() '8B14B0
  loc_008B152D: var_18 = Text2.Text
  loc_008B1585: var_1C = var_18
  loc_008B15A2: var_eax = CustomSpectrumSweep.Proc_8_14_8B25D0(Me, var_1C, var_34, var_3C, var_2C, var_20, var_28, esi)
  loc_008B15AF: Text2.Text = var_20
  loc_008B15F3: var_eax = CustomSpectrumSweep.Proc_8_17_8B31A0(Me)
  loc_008B1602: GoTo loc_008B162C
  loc_008B162B: Exit Sub
  loc_008B162C: 'Referenced from: 008B1602
End Sub

Private Sub Text1_LostFocus() '8B1310
  loc_008B138D: var_18 = Text1.Text
  loc_008B13E1: var_1C = var_18
  loc_008B13FE: var_eax = CustomSpectrumSweep.Proc_8_14_8B25D0(Me, var_1C, var_34, var_3C, var_2C, var_20, var_28, esi)
  loc_008B140B: Text1.Text = var_20
  loc_008B144F: var_eax = CustomSpectrumSweep.Proc_8_17_8B31A0(Me)
  loc_008B145E: GoTo loc_008B1488
  loc_008B1487: Exit Sub
  loc_008B1488: 'Referenced from: 008B145E
End Sub

Private Sub Option1_Click() '8B11E0
  loc_008B1223: var_eax = CustomSpectrumSweep.Proc_8_17_8B31A0(Me, edi)
End Sub

Private Sub Form_Load() '8AF5D0
  loc_008AF658: var_18 = CurDir(vbNullString)
  loc_008AF669: ecx = var_18
  loc_008AF69E: var_eax = CustomSpectrumSweep.Proc_8_12_8B1FE0(Me, var_18)
  loc_008AF6AA: ecx = var_18
  loc_008AF6D1: Text1.Text = var_004A1CA0
  loc_008AF712: Text2.Text = "3000000"
  loc_008AF753: Text3.Text = ".025"
  loc_008AF794: Text4.Text = "180"
  loc_008AF7D5: Text5.Text = "32"
  loc_008AF816: Text6.Text = var_004A2CA8
  loc_008AF840: var_eax = CustomSpectrumSweep.Proc_8_17_8B31A0(Me, var_1C, esi, Me, var_1C, esi, Me)
  loc_008AF852: GoTo loc_008AF87A
  loc_008AF879: Exit Sub
  loc_008AF87A: 'Referenced from: 008AF852
End Sub

Public Sub Proc_8_11_8B0F40
  loc_008B0F99: ecx = vbNullString
  loc_008B0FB7: call __vbaVarVargNofree(__vbaVarVargNofree, arg_C, ebx)
  loc_008B0FC4: var_34 = Len(__vbaVarVargNofree(__vbaVarVargNofree, arg_C, ebx))
  loc_008B0FD4: If (var_34 = "") = 0 Then GoTo loc_008B0FEE
  loc_008B0FDE: var_20 = vbNullString
  loc_008B0FE9: GoTo loc_008B11B2
  loc_008B0FEE: 'Referenced from: 008B0FD4
  loc_008B0FF3: call __vbaVarVargNofree(var_008B11BC)
  loc_008B1009: var_80 = CLng(Len(__vbaVarVargNofree(var_008B11BC)))
  loc_008B1014: If 00000001h > 0 Then GoTo loc_008B116A
  loc_008B1047: var_1C = Mid(arg_C, 1, 1)
  loc_008B1065: var_5C = var_1C
  loc_008B1088: eax = (var_1C = var_004A5128) + 1
  loc_008B1092: var_6C = (var_1C = var_004A5128) + 1
  loc_008B10A2: var_ret_2 = (var_1C = Chr(34))
  loc_008B10B1: call Or(var_54, var_74, var_ret_2)
  loc_008B10D2: If CBool(Or(var_54, var_74, var_ret_2)) = 0 Then GoTo loc_008B10E6
  loc_008B10E4: GoTo loc_008B10EC
  loc_008B10E6: 'Referenced from: 008B10D2
  loc_008B10EC: 'Referenced from: 008B10E4
  loc_008B10FD: If (var_1C <> var_004A33E0) <> 0 Then GoTo loc_008B1109
  loc_008B1107: var_1C = vbNullString
  loc_008B1109: 'Referenced from: 008B10FD
  loc_008B111A: If (var_1C <> var_004AE7C8) <> 0 Then GoTo loc_008B1126
  loc_008B1126: 'Referenced from: 008B111A
  loc_008B1148: var_8C = var_8C & var_1C
  loc_008B115F: 00000001h = 00000001h + 00000001h
  loc_008B1165: GoTo loc_008B1011
  loc_008B116A: 'Referenced from: 008B1014
  loc_008B1175: var_20 = eax
  loc_008B1180: GoTo loc_008B11B2
  loc_008B1186: If var_4 = 0 Then GoTo loc_008B1191
  loc_008B1191: 'Referenced from: 008B1186
  loc_008B11B1: Exit Sub
  loc_008B11B2: 'Referenced from: 008B0FE9
  loc_008B11BB: Exit Sub
End Sub

Public Sub Proc_8_12_8B1FE0
  loc_008B2029: call __vbaStrR8(var_33333333, var_3FF33333, edi, esi, ebx)
  loc_008B2032: var_24 = __vbaStrR8(var_33333333, var_3FF33333, edi, esi, ebx)
  loc_008B205E: var_1C = Trim(__vbaStrR8(var_33333333, var_3FF33333, edi, esi, ebx))
  loc_008B2081: var_44 = var_1C
  loc_008B20AF: var_18 = Mid(var_1C, 2, 1)
  loc_008B20C6: GoTo loc_008B20EB
  loc_008B20CC: If var_4 = 0 Then GoTo loc_008B20D7
  loc_008B20D7: 'Referenced from: 008B20CC
  loc_008B20EA: Exit Sub
  loc_008B20EB: 'Referenced from: 008B20C6
End Sub

Public Sub Proc_8_13_8B2120
  loc_008B2177: On Error Resume Next
  loc_008B2189: fcomp real8 ptr [00401EB8h]
  loc_008B21AB: GoTo loc_008B253F
  loc_008B21DD: var_84 = arg_C
  loc_008B2249: If (var_004A1C98 = 00000001h.Name = var_004A1C98) = 0 Then GoTo loc_008B22AA
  loc_008B227C: var_eax = CustomSpectrumSweep.Proc_8_18_8B6A40(Me, Format(arg_C, "################################################.################################################"), Me, 004A1C98h, var_4C)
  loc_008B229B: var_24 = var_4C
  loc_008B22AA: 'Referenced from: 008B2249
  loc_008B22BB: var_30 = Len(var_24)
  loc_008B22C9: If var_30 >= 1 Then GoTo loc_008B22E5
  loc_008B22E0: GoTo loc_008B253F
  loc_008B22E5: 'Referenced from: 008B22C9
  loc_008B22EF: var_B8 = var_30
  loc_008B2306: GoTo loc_008B231A
  loc_008B2308: 
  loc_008B230B: var_28 = var_28 + 1
  loc_008B2317: var_28 = var_28
  loc_008B231A: 'Referenced from: 008B2306
  loc_008B2323: If var_28 > 0 Then GoTo loc_008B23DC
  loc_008B2341: var_84 = var_24
  loc_008B2379: var_2C = Mid(var_24, var_28, 1)
  loc_008B23AE: If InStr(1, "-01234567890.", var_2C, 0) <= 0 Then GoTo loc_008B23D0
  loc_008B23CA: var_44 = var_44 & var_2C
  loc_008B23D0: 'Referenced from: 008B23AE
  loc_008B23D7: GoTo loc_008B2308
  loc_008B23DC: 'Referenced from: 008B2323
  loc_008B23ED: var_28 = Len(var_44)
  loc_008B2408: var_84 = var_44
  loc_008B2456: var_B0 = (Mid(var_44, var_28, 1) = &H4A1C98)
  loc_008B2479: If var_B0 = 0 Then GoTo loc_008B24EA
  loc_008B2485: var_28 = var_28 - 00000001h
  loc_008B248E: var_54 = var_28
  loc_008B249B: var_84 = var_44
  loc_008B24D1: var_44 = Mid(var_44, 1, var_28)
  loc_008B24EA: 'Referenced from: 008B2479
  loc_008B2502: If (var_44 <> vbNullString) <> 0 Then GoTo loc_008B2519
  loc_008B2519: 'Referenced from: 008B2502
  loc_008B2519: GoTo loc_008B253F
  loc_008B2524: On Error Resume Next
  loc_008B253F: 'Referenced from: 008B21AB
  loc_008B253F: Exit Sub
  loc_008B254B: GoTo loc_008B258B
  loc_008B2555: If var_10 And 4 = 0 Then GoTo loc_008B2560
  loc_008B2560: 'Referenced from: 008B2555
  loc_008B258A: Exit Sub
  loc_008B258B: 'Referenced from: 008B254B
End Sub

Public Sub Proc_8_14_8B25D0
  loc_008B2627: On Error Resume Next
  loc_008B263C: var_24 = ecx
  loc_008B264E: var_40 = arg_10
End Sub

Public Sub Proc_8_15_8B2860
  loc_008B28B7: On Error Resume Next
  loc_008B28CC: var_40 = vbNullString
  loc_008B28E1: var_24 = vbNullString
  loc_008B28FA: var_34 = Len(arg_C)
  loc_008B2908: If var_34 <> 0 Then GoTo loc_008B290F
  loc_008B290A: GoTo loc_008B2D28
  loc_008B290F: 'Referenced from: 008B2908
  loc_008B2941: GoTo loc_008B2959
  loc_008B2943: 
  loc_008B2959: 'Referenced from: 008B2941
  loc_008B295C: fcomp real8 ptr var_90
  loc_008B2985: var_68 = arg_C
  loc_008B29BA: var_30 = Mid(arg_C, CLng(var_2C), 1)
  loc_008B29EB: If (var_30 <> var_004A187C) <> 0 Then GoTo loc_008B2A02
  loc_008B2A02: 'Referenced from: 008B29EB
  loc_008B2A1A: If (var_30 <> var_004B40A0) <> 0 Then GoTo loc_008B2A31
  loc_008B2A31: 'Referenced from: 008B2A1A
  loc_008B2A49: If (var_24 <> vbNullString) <> 0 Then GoTo loc_008B2A8E
  loc_008B2A67: If InStr(1, "-.0123456789", var_30, 0) <= 0 Then GoTo loc_008B2A89
  loc_008B2A83: var_24 = var_24 & var_30
  loc_008B2A89: 'Referenced from: 008B2A67
  loc_008B2A89: GoTo loc_008B2CCF
  loc_008B2A8E: 'Referenced from: 008B2A49
  loc_008B2AA6: If (var_30 <> var_004B19FC) <> 0 Then GoTo loc_008B2B25
  loc_008B2ABB: If Len(var_24) <= 0 Then GoTo loc_008B2B20
  loc_008B2AD4: fcomp real8 ptr [00401EB8h]
  loc_008B2AFD: If InStr(1, var_24, var_004B19FC, 0) <> 0 Then GoTo loc_008B2B20
  loc_008B2B1A: var_24 = var_24 & var_004B19FC
  loc_008B2B20: 'Referenced from: 008B2ABB
  loc_008B2B20: GoTo loc_008B2CCF
  loc_008B2B25: 'Referenced from: 008B2AA6
  loc_008B2B3D: If (var_30 <> var_004B40C8) <> 0 Then GoTo loc_008B2B8F
  loc_008B2B67: If InStr(1, var_24, var_004B19FC, 0) <> 0 Then GoTo loc_008B2B8A
  loc_008B2B84: var_24 = var_24 & var_004B40C8
  loc_008B2B8A: 'Referenced from: 008B2B67
  loc_008B2B8A: GoTo loc_008B2CCF
  loc_008B2B8F: 'Referenced from: 008B2B3D
  loc_008B2BA7: If (var_30 <> var_004A27E4) <> 0 Then GoTo loc_008B2BF9
  loc_008B2BD1: If InStr(1, var_24, var_004B19FC, 0) <> 0 Then GoTo loc_008B2BF4
  loc_008B2BEE: var_24 = var_24 & var_004A27E4
  loc_008B2BF4: 'Referenced from: 008B2BD1
  loc_008B2BF4: GoTo loc_008B2CCF
  loc_008B2BF9: 'Referenced from: 008B2BA7
  loc_008B2C30: If (var_30 <> var_004A187C) <> 0 Then GoTo loc_008B2C91
  loc_008B2C4E: If InStr(1, var_24, var_004A1C98, 0) <> 0 Then GoTo loc_008B2C8F
  loc_008B2C6C: If InStr(1, var_24, var_004B19FC, 0) <> 0 Then GoTo loc_008B2C8F
  loc_008B2C89: var_24 = var_24 & var_004A1C98
  loc_008B2C8F: 'Referenced from: 008B2C4E
  loc_008B2C8F: GoTo loc_008B2CCF
  loc_008B2C91: 'Referenced from: 008B2C30
  loc_008B2CAD: If InStr(1, "0123456789", var_30, 0) <= 0 Then GoTo loc_008B2CCF
  loc_008B2CC9: var_24 = var_24 & var_30
  loc_008B2CCF: 'Referenced from: 008B2A89
  loc_008B2CD6: GoTo loc_008B2943
  loc_008B2CFC: var_40 = var_24
  loc_008B2D02: GoTo loc_008B2D28
  loc_008B2D0D: On Error Resume Next
  loc_008B2D22: var_40 = vbNullString
  loc_008B2D28: 'Referenced from: 008B290A
  loc_008B2D28: Exit Sub
  loc_008B2D34: GoTo loc_008B2D5D
  loc_008B2D3E: If var_10 And 4 = 0 Then GoTo loc_008B2D49
  loc_008B2D49: 'Referenced from: 008B2D3E
  loc_008B2D5C: Exit Sub
  loc_008B2D5D: 'Referenced from: 008B2D34
End Sub

Public Sub Proc_8_16_8B2DA0
  loc_008B2DF9: ecx = vbNullString
  loc_008B2E07: If var_8D9000 <> 0 Then GoTo loc_008B2E11
  loc_008B2E0F: GoTo loc_008B2E22
  loc_008B2E11: 'Referenced from: 008B2E07
  loc_008B2E22: 'Referenced from: 008B2E0F
  loc_008B2E2C: var_ret_1 = Int((?.?<E-313 / 3600))
  loc_008B2E41: If var_8D9000 <> 0 Then GoTo loc_008B2E4B
  loc_008B2E49: GoTo loc_008B2E5C
  loc_008B2E4B: 'Referenced from: 008B2E41
  loc_008B2E5C: 'Referenced from: 008B2E49
  loc_008B2E6F: fsubp st1
  loc_008B2E7B: var_ret_2 = Int((var_18 * 60))
  loc_008B2EA2: fsubr st0, real8 ptr [eax]
  loc_008B2EBF: fsubp st1
  loc_008B2ECE: var_40 = Round((var_14 * 1099511899904#), 0)
  loc_008B2ED8: var_40 = CSng(var_401CD0)
  loc_008B2EF4: fcomp real4 ptr [00403DD4h]
  loc_008B2F23: ecx = var_004A1CA0 = edi.Caption & var_004A1CA0
  loc_008B2F2E: GoTo loc_008B2F36
  loc_008B2F36: 'Referenced from: 008B2F2E
  loc_008B2F3B: var_68 = %x1 = edi.Caption
  loc_008B2F4D: var_58 = var_18
  loc_008B2F8F: ecx = %x1 = edi.Caption & Trim(Str(var_18))
  loc_008B2FCA: ecx = var_004A2914 = edi.Caption & var_004A2914
  loc_008B2FD8: fcomp real4 ptr [00403DD4h]
  loc_008B2FFE: ecx = var_004A1CA0 = edi.Caption & var_004A1CA0
  loc_008B300E: var_68 = %x1 = edi.Caption
  loc_008B3020: var_58 = var_14
  loc_008B3062: ecx = %x1 = edi.Caption & Trim(Str(var_14))
  loc_008B309D: ecx = var_004A2914 = edi.Caption & var_004A2914
  loc_008B30AB: fcomp real4 ptr [00403DD4h]
  loc_008B30D1: ecx = var_004A1CA0 = edi.Caption & var_004A1CA0
  loc_008B30E1: var_68 = %x1 = edi.Caption
  loc_008B30F3: var_58 = var_1C
  loc_008B3135: ecx = %x1 = edi.Caption & Trim(Str(var_1C))
  loc_008B315D: GoTo loc_008B3180
  loc_008B317F: Exit Sub
  loc_008B3180: 'Referenced from: 008B315D
  loc_008B3180: Exit Sub
End Sub

Public Sub Proc_8_17_8B31A0
  Dim var_A8 As Variant
  Dim var_BC As Label
  loc_008B3302: var_98 = Text1.Text
  loc_008B336B: var_98 = Text2.Text
  loc_008B33D4: var_98 = Text3.Text
  loc_008B343D: var_98 = Text4.Text
  loc_008B34A6: var_98 = Text5.Text
  loc_008B350F: var_98 = Text6.Text
  loc_008B3562: If var_8D9000 <> 0 Then GoTo loc_008B356C
  loc_008B356A: GoTo loc_008B357D
  loc_008B356C: 'Referenced from: 008B3562
  loc_008B357D: 'Referenced from: 008B356A
  loc_008B358D: fcomp real8 ptr [00401EB8h]
  loc_008B35A0: fabs
  loc_008B35AF: If var_8D9000 <> 0 Then GoTo loc_008B35B6
  loc_008B35B4: GoTo loc_008B35C1
  loc_008B35B6: 'Referenced from: 008B35AF
  loc_008B35C1: 'Referenced from: 008B35B4
  loc_008B35D1: fcomp real8 ptr [00401EB8h]
  loc_008B35E4: fabs
  loc_008B35F2: If var_8D9000 <> 0 Then GoTo loc_008B35F8
  loc_008B35F4: fdivp st1
  loc_008B35F6: GoTo loc_008B3601
  loc_008B35F8: 'Referenced from: 008B35F2
  loc_008B35FB: call _adj_fdiv_r(Err.Number, var_18, var_14, var_405B50, var_405B54, var_A8, 0Dh, Me, var_A8, 0Dh, Me, var_A8, Me, Me, var_A8, 0Dh)
  loc_008B3601: 'Referenced from: 008B35F6
  loc_008B3620: fsubr st0, real8 ptr var_18
  loc_008B3630: If var_8D9000 <> 0 Then GoTo loc_008B363A
  loc_008B3638: GoTo loc_008B364B
  loc_008B363A: 'Referenced from: 008B3630
  loc_008B364B: 'Referenced from: 008B3638
  loc_008B364B: fsubp st1
  loc_008B3679: If var_8D9000 <> 0 Then GoTo loc_008B3683
  loc_008B3681: GoTo loc_008B3694
  loc_008B3683: 'Referenced from: 008B3679
  loc_008B3694: 'Referenced from: 008B3681
  loc_008B3694: faddp st1
  loc_008B36A9: fcomp real8 ptr [00401EB8h]
  loc_008B36C2: fcomp real8 ptr [00401EB8h]
  loc_008B36DE: fcomp real8 ptr [00401EB8h]
  loc_008B36ED: var_48 = var_6C
  loc_008B36F3: var_4C = var_80
  loc_008B36F8: var_44 = var_68
  loc_008B36FE: var_50 = var_84
  loc_008B370F: If var_8D9000 <> 0 Then GoTo loc_008B3719
  loc_008B3717: GoTo loc_008B372A
  loc_008B3719: 'Referenced from: 008B370F
  loc_008B372A: 'Referenced from: 008B3717
  loc_008B3734: If var_8D9000 <> 0 Then GoTo loc_008B373B
  loc_008B3739: GoTo loc_008B3746
  loc_008B373B: 'Referenced from: 008B3734
  loc_008B3746: 'Referenced from: 008B3739
  loc_008B3756: fcomp real8 ptr [00401EB8h]
  loc_008B376C: fabs
  loc_008B3775: If var_8D9000 <> 0 Then GoTo loc_008B377C
  loc_008B377A: GoTo loc_008B3787
  loc_008B377C: 'Referenced from: 008B3775
  loc_008B3787: 'Referenced from: 008B377A
  loc_008B379A: fcomp real8 ptr [00401EB8h]
  loc_008B37B4: If var_8D9000 <> 0 Then GoTo loc_008B37BB
  loc_008B37B9: GoTo loc_008B37C6
  loc_008B37BB: 'Referenced from: 008B37B4
  loc_008B37C6: 'Referenced from: 008B37B9
  loc_008B37F8: 12 = Label30.FormatLength
  loc_008B3833: var_224 = "Spectrum = "
  loc_008B3843: var_214 = var_74
  loc_008B388E: var_234 = " %"
  loc_008B38C9: var_98 = CStr("Spectrum = " & Trim(Str(Round(var_74, 8))) & " %")
  loc_008B38D9: var_eax = Unknown_VTable_Call[eax+00000054h]
  loc_008B3966: 14 = Label30.FormatLength
  loc_008B399D: var_224 = "Sweep Start Frequency = "
  loc_008B39B1: var_214 = var_6C
  loc_008B3A03: var_234 = " Hz"
  loc_008B3A35: var_98 = CStr("Sweep Start Frequency = " & Trim(Str(Round(var_6C, 8))) & " Hz")
  loc_008B3A45: var_eax = Unknown_VTable_Call[ecx+00000054h]
  loc_008B3AD2: 15 = Label30.FormatLength
  loc_008B3B0C: var_224 = "Sweep Stop Frequency = "
  loc_008B3B20: var_214 = var_84
  loc_008B3B30: var_BC = Round(var_84, 8)
  loc_008B3B72: var_234 = " Hz"
  loc_008B3BA4: var_98 = CStr("Sweep Stop Frequency = " & Trim(Str(var_BC)) & " Hz")
  loc_008B3C41: 16 = Label30.FormatLength
  loc_008B3C7B: var_224 = "Frequency Spacing = "
  loc_008B3C8F: var_214 = var_94
  loc_008B3C9F: var_BC = Round(var_94, 8)
  loc_008B3CE1: var_234 = " Hz"
  loc_008B3D13: var_98 = CStr("Frequency Spacing = " & Trim(Str(var_BC)) & " Hz")
  loc_008B3D23: var_eax = Unknown_VTable_Call[eax+00000054h]
  loc_008B3D95: var_eax = CustomSpectrumSweep.Proc_8_16_8B2DA0(Me, var_8C, var_A8, var_BC, Me)
  loc_008B3DC0: 19 = Label30.FormatLength
  loc_008B3E03: var_98 = "Total Sweep Duration = " & edi+00000050h
  loc_008B3E0F: var_eax = Unknown_VTable_Call[eax+00000054h]
  loc_008B3E74: 20 = Label30.FormatLength
  loc_008B3EAB: var_224 = "Sweep Speed = "
  loc_008B3EBF: var_214 = var_7C
  loc_008B3F11: var_234 = " Hz/Second"
  loc_008B3F43: var_98 = CStr("Sweep Speed = " & Trim(Str(Round(var_7C, 8))) & " Hz/Second")
  loc_008B3F53: var_eax = Unknown_VTable_Call[eax+00000054h]
  loc_008B3FE3: 21 = Label30.FormatLength
  loc_008B401A: var_224 = "Spectrum Amplitude = "
  loc_008B402E: var_214 = var_38
  loc_008B4080: var_234 = " Volts"
  loc_008B40B2: var_98 = CStr("Spectrum Amplitude = " & Trim(Str(Round(var_38, 3))) & " Volts")
  loc_008B40C2: var_eax = Unknown_VTable_Call[ecx+00000054h]
  loc_008B418A: var_280 = Option1.Value
  loc_008B41BE: setz dl
  loc_008B41E4: If var_294 = 0 Then GoTo loc_008B4AEA
  loc_008B41F3: var_BC = Chr(34)
  loc_008B421C: var_98 = Text7.Text
  loc_008B4264: var_214 = " (Single Spectrum Sweep)"
  loc_008B4274: var_FC = Chr(34)
End Sub

Public Sub Proc_8_18_8B6A40
  loc_008B6A87: var_24 = vbNullString
  loc_008B6A9F: var_1C = Len(arg_C)
  loc_008B6AAA: If 00000001h > 0 Then GoTo loc_008B6B40
  loc_008B6ACB: var_4C = arg_C
  loc_008B6AEA: var_20 = Mid(arg_C, 1, 1)
  loc_008B6B15: If (var_20 <> arg_10) <> 0 Then GoTo loc_008B6B1F
  loc_008B6B1D: GoTo loc_008B6B23
  loc_008B6B1F: 'Referenced from: 008B6B15
  loc_008B6B2E: var_24 = var_20 & var_20
  loc_008B6B35: 00000001h = 00000001h + 00000001h
  loc_008B6B3B: GoTo loc_008B6AA7
  loc_008B6B40: 'Referenced from: 008B6AAA
  loc_008B6B45: GoTo loc_008B6B6A
  loc_008B6B4B: If var_4 = 0 Then GoTo loc_008B6B56
  loc_008B6B56: 'Referenced from: 008B6B4B
  loc_008B6B69: Exit Sub
  loc_008B6B6A: 'Referenced from: 008B6B45
  loc_008B6B73: Exit Sub
End Sub
