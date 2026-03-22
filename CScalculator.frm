VERSION 5.00
Begin VB.Form CScalculator
  Caption = "Colloidal / Ionic Silver Calculator"
  ScaleMode = 1
  AutoRedraw = False
  FontTransparent = True
  Icon = "CScalculator.frx":0
  LinkTopic = "Form1"
  ClientLeft = 60
  ClientTop = 510
  ClientWidth = 13710
  ClientHeight = 10395
  BeginProperty Font
    Name = "Arial"
    Size = 9.75
    Charset = 0
    Weight = 400
    Underline = 0 'False
    Italic = 0 'False
    Strikethrough = 0 'False
  EndProperty
  StartUpPosition = 2 'CenterScreen
  Begin Frame Frame2
    Caption = "Ionic By Measurement"
    ForeColor = &H0&
    Left = 7080
    Top = 2640
    Width = 3855
    Height = 3735
    TabIndex = 8
    BeginProperty Font
      Name = "Arial"
      Size = 11.25
      Charset = 0
      Weight = 400
      Underline = 0 'False
      Italic = 0 'False
      Strikethrough = 0 'False
    EndProperty
    Begin TextBox Text1
      Index = 6
      ForeColor = &H0&
      Left = 2280
      Top = 1560
      Width = 975
      Height = 435
      Text = "20"
      TabIndex = 17
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
      ToolTipText = "Enter the desired strength of your colloidal silver solution"
    End
    Begin TextBox Text1
      Index = 5
      ForeColor = &H0&
      Left = 2280
      Top = 1080
      Width = 975
      Height = 435
      Text = "0"
      TabIndex = 16
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
      ToolTipText = "CScalculator.frx":FD55
    End
    Begin TextBox Text1
      Index = 4
      ForeColor = &H0&
      Left = 2280
      Top = 600
      Width = 975
      Height = 435
      Text = "0"
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
      ToolTipText = "Enter the starting Total Dissolved Solids (TDS) of your distilled water. You need a special TDS meter to get this value"
    End
    Begin Label Label30
      Caption = "PPM"
      Index = 8
      BackColor = &HFFFFFF&
      ForeColor = &H0&
      Left = 120
      Top = 1680
      Width = 1815
      Height = 375
      TabIndex = 11
      Alignment = 1 'Right Justify
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
      ToolTipText = "This is an estimate of the amount of dissolved solids in your colloidal / ionic solution"
    End
    Begin Label Label30
      Caption = "Current TDS"
      Index = 5
      BackColor = &HFFFFFF&
      ForeColor = &H0&
      Left = 120
      Top = 1200
      Width = 1815
      Height = 375
      TabIndex = 10
      Alignment = 1 'Right Justify
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
      Caption = "Initial TDS"
      Index = 2
      BackColor = &HFFFFFF&
      ForeColor = &H0&
      Left = 120
      Top = 720
      Width = 1815
      Height = 375
      TabIndex = 9
      Alignment = 1 'Right Justify
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
  Begin Frame Frame1
    Caption = "Colloidal By Calculation"
    ForeColor = &H0&
    Left = 2040
    Top = 2640
    Width = 4455
    Height = 3735
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
    Begin TextBox Text1
      Index = 8
      ForeColor = &H0&
      Left = 2880
      Top = 3000
      Width = 975
      Height = 435
      Text = "0"
      TabIndex = 21
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
      ToolTipText = "Enter the number of days the colloidal silver will be generated"
    End
    Begin TextBox Text1
      Index = 7
      ForeColor = &H0&
      Left = 2880
      Top = 2520
      Width = 975
      Height = 435
      Text = "0"
      TabIndex = 20
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
      ToolTipText = "Enter the number of hours the colloidal silver will be generated"
    End
    Begin TextBox Text1
      Index = 3
      ForeColor = &H0&
      Left = 2880
      Top = 2040
      Width = 975
      Height = 435
      Text = "0"
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
      ToolTipText = "Enter the number of minutes the colloidal silver will be generated"
    End
    Begin TextBox Text1
      Index = 2
      ForeColor = &H0&
      Left = 2880
      Top = 1560
      Width = 975
      Height = 435
      Text = ".2"
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
      ToolTipText = "Enter the current that will be passing through the silver electrodes. The default is 0.2 mA."
    End
    Begin TextBox Text1
      Index = 1
      ForeColor = &H0&
      Left = 2880
      Top = 1080
      Width = 975
      Height = 435
      Text = "20"
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
      ToolTipText = "Enter the desired PPM (Parts Per Million) of Silver"
    End
    Begin TextBox Text1
      Index = 0
      ForeColor = &H0&
      Left = 2880
      Top = 600
      Width = 975
      Height = 435
      Text = "500"
      TabIndex = 4
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
      ToolTipText = "Enter the volume of water you are using to make the Colloidal / Ionic Silver"
    End
    Begin Label Label30
      Caption = "Duration (days)"
      Index = 7
      BackColor = &HFFFFFF&
      ForeColor = &H0&
      Left = 120
      Top = 3120
      Width = 2415
      Height = 375
      TabIndex = 19
      Alignment = 1 'Right Justify
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
      Caption = "Duration (hours)"
      Index = 6
      BackColor = &HFFFFFF&
      ForeColor = &H0&
      Left = 120
      Top = 2640
      Width = 2415
      Height = 375
      TabIndex = 18
      Alignment = 1 'Right Justify
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
      Caption = "Duration (mins)"
      Index = 4
      BackColor = &HFFFFFF&
      ForeColor = &H0&
      Left = 120
      Top = 2160
      Width = 2415
      Height = 375
      TabIndex = 7
      Alignment = 1 'Right Justify
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
      Caption = "Current (mA)"
      Index = 9
      BackColor = &HFFFFFF&
      ForeColor = &H0&
      Left = 120
      Top = 1680
      Width = 2415
      Height = 375
      TabIndex = 6
      Alignment = 1 'Right Justify
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
      Caption = "PPM"
      Index = 3
      BackColor = &HFFFFFF&
      ForeColor = &H0&
      Left = 120
      Top = 1200
      Width = 2415
      Height = 375
      TabIndex = 5
      Alignment = 1 'Right Justify
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
      Caption = "Volume (ml)"
      Index = 1
      BackColor = &HFFFFFF&
      ForeColor = &H0&
      Left = 120
      Top = 720
      Width = 2415
      Height = 375
      TabIndex = 3
      Alignment = 1 'Right Justify
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
  Begin ActiveResize ActiveResize1
  End
  Begin Label Label30
    Caption = "CScalculator.frx":FDD9
    Index = 18
    BackColor = &HFFFFFF&
    ForeColor = &H0&
    Left = 2640
    Top = 1440
    Width = 9015
    Height = 1095
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
  Begin Image Image1
    Picture = "CScalculator.frx":FE85
    Left = 0
    Top = 240
    Width = 2175
    Height = 2130
    Stretch = -1  'True
  End
  Begin Label Label1
    Caption = "Colloidal/Ionic Silver Calculator"
    BackColor = &HFFFFFF&
    ForeColor = &H0&
    Left = 2160
    Top = 120
    Width = 7695
    Height = 855
    TabIndex = 0
    BackStyle = 0 'Transparent
    BeginProperty Font
      Name = "Arial"
      Size = 24
      Charset = 0
      Weight = 400
      Underline = 0 'False
      Italic = 0 'False
      Strikethrough = 0 'False
    EndProperty
  End
End

Attribute VB_Name = "CScalculator"


Private Sub Text1_Change() '8B70E0
  Dim var_20 As TextBox
  loc_008B713B: If %x1 = Me.Caption = 0 Then GoTo loc_008B8340
  loc_008B7148: 
  loc_008B7152: If Me.Caption = %StkVar1 > 0 Then GoTo loc_008B7221
  loc_008B7177: var_ret_1 = esi+00000054h
  loc_008B717F: var_eax = Unknown_VTable_Call[ebx+00000040h]
  loc_008B71A5: var_18 = Text1.Text
  loc_008B7211: esi+00000054h = esi+00000054h + 00000001h
  loc_008B721C: GoTo loc_008B7148
  loc_008B7221: 'Referenced from: 008B7152
  loc_008B7230: If var_8D9000 <> 0 Then GoTo loc_008B723A
  loc_008B7238: GoTo loc_008B7245
  loc_008B723A: 'Referenced from: 008B7230
  loc_008B7240: call _adj_fdiv_m32(var_406550, var_1C, 00000008h, Me, ebx)
  loc_008B7245: 'Referenced from: 008B7238
  loc_008B725A: setnz cl
  loc_008B7262: setnz dl
  loc_008B7273: setnz dl
  loc_008B7278: If edx <> 0 Then GoTo loc_008B764A
  loc_008B7283: fcomp real4 ptr [00406EF0h]
  loc_008B7295: GoTo loc_008B7299
  loc_008B7299: 'Referenced from: 008B7295
  loc_008B729C: fcomp real4 ptr [00406EF0h]
  loc_008B72AE: GoTo loc_008B72B2
  loc_008B72B2: 'Referenced from: 008B72AE
  loc_008B72B5: fcomp real4 ptr [00406EF0h]
  loc_008B72C7: GoTo loc_008B72CB
  loc_008B72CB: 'Referenced from: 008B72C7
  loc_008B72D5: If eax <> 0 Then GoTo loc_008B72DC
  loc_008B72DA: GoTo loc_008B730A
  loc_008B72DC: 'Referenced from: 008B72D5
  loc_008B72EE: If var_8D9000 <> 0 Then GoTo loc_008B72F5
  loc_008B72F3: GoTo loc_008B72FD
  loc_008B72F5: 'Referenced from: 008B72EE
  loc_008B72F8: call _adj_fdiv_m32(edi+00000008h)
  loc_008B72FD: 'Referenced from: 008B72F3
  loc_008B730A: 'Referenced from: 008B72DA
  loc_008B735A: var_ret_2 = Int((?.?<E-313 * 10))
  loc_008B7367: If var_8D9000 <> 0 Then GoTo loc_008B7371
  loc_008B736F: GoTo loc_008B737C
  loc_008B7371: 'Referenced from: 008B7367
  loc_008B7377: call _adj_fdiv_m32(var_403DD4, Me, 00000003h, var_20, var_1C, Me, Me)
  loc_008B737C: 'Referenced from: 008B736F
  loc_008B7398: var_40 = Str((&H0000000300905A4D&H / &H41200000&H))
  loc_008B73B6: var_18 = CStr(Trim(var_40))
  loc_008B73BE: Text1.Text = var_18
  loc_008B742B: var_eax = Unknown_VTable_Call[eax+00000040h]
  loc_008B7453: If var_8D9000 <> 0 Then GoTo loc_008B745D
  loc_008B745B: GoTo loc_008B7468
  loc_008B745D: 'Referenced from: 008B7453
  loc_008B7463: call _adj_fdiv_m32(var_406EEC, var_30, 00000007h, var_20, var_1C, var_30, Me)
  loc_008B7468: 'Referenced from: 008B745B
  loc_008B7478: var_ret_3 = Int(((?.?<E-313 / 60) * 10))
  loc_008B7485: If var_8D9000 <> 0 Then GoTo loc_008B748F
  loc_008B748D: GoTo loc_008B749A
  loc_008B748F: 'Referenced from: 008B7485
  loc_008B7495: call _adj_fdiv_m32(var_403DD4)
  loc_008B749A: 'Referenced from: 008B748D
  loc_008B74B6: var_40 = Str((&H0000000300905A4D&H / &H41200000&H))
  loc_008B74D4: var_18 = CStr(Trim(var_40))
  loc_008B74DC: Text1.Text = var_18
  loc_008B7549: var_eax = Unknown_VTable_Call[ecx+00000040h]
  loc_008B7571: If var_8D9000 <> 0 Then GoTo loc_008B757B
  loc_008B7579: GoTo loc_008B7586
  loc_008B757B: 'Referenced from: 008B7571
  loc_008B7581: call _adj_fdiv_m32(var_414F74, var_1C, 00000008h, var_20, var_1C, var_40, Me)
  loc_008B7586: 'Referenced from: 008B7579
  loc_008B7596: var_ret_4 = Int(((?.?<E-313 / 1440) * 10))
  loc_008B75A3: If var_8D9000 <> 0 Then GoTo loc_008B75AD
  loc_008B75AB: GoTo loc_008B75B8
  loc_008B75AD: 'Referenced from: 008B75A3
  loc_008B75B3: call _adj_fdiv_m32(var_403DD4)
  loc_008B75B8: 'Referenced from: 008B75AB
  loc_008B75D4: var_40 = Str((&H0000000300905A4D&H / &H41200000&H))
  loc_008B75F2: var_18 = CStr(Trim(var_40))
  loc_008B75FA: Text1.Text = var_18
  loc_008B764A: 'Referenced from: 008B7278
  loc_008B7651: If arg_C <> 3 Then GoTo loc_008B79CB
  loc_008B7670: If var_8D9000 <> 0 Then GoTo loc_008B7676
  loc_008B7672: fdivp st1
  loc_008B7674: GoTo loc_008B767F
  loc_008B7676: 'Referenced from: 008B7670
  loc_008B7679: call _adj_fdiv_r(arg_C, Me)
  loc_008B767F: 'Referenced from: 008B7674
  loc_008B76AA: var_eax = Unknown_VTable_Call[eax+00000040h]
  loc_008B76DB: var_ret_5 = Int((?.?<E-313 * 10))
  loc_008B76E8: If var_8D9000 <> 0 Then GoTo loc_008B76F2
  loc_008B76F0: GoTo loc_008B76FD
  loc_008B76F2: 'Referenced from: 008B76E8
  loc_008B76F8: call _adj_fdiv_m32(var_403DD4, Err.Number, 00000001h, var_20, var_1C, Err.Number)
  loc_008B76FD: 'Referenced from: 008B76F0
  loc_008B7719: var_40 = Str(((&H0000000300905A4D&H * &H00905A4D&H) / &H41200000&H))
  loc_008B7737: var_18 = CStr(Trim(var_40))
  loc_008B773F: Text1.Text = var_18
  loc_008B77AC: var_eax = Unknown_VTable_Call[ecx+00000040h]
  loc_008B77D4: If var_8D9000 <> 0 Then GoTo loc_008B77DE
  loc_008B77DC: GoTo loc_008B77E9
  loc_008B77DE: 'Referenced from: 008B77D4
  loc_008B77E4: call _adj_fdiv_m32(var_406EEC, var_1C, 00000007h, var_20, var_1C, var_40, Me)
  loc_008B77E9: 'Referenced from: 008B77DC
  loc_008B77F9: var_ret_6 = Int(((?.?<E-313 / 60) * 10))
  loc_008B7806: If var_8D9000 <> 0 Then GoTo loc_008B7810
  loc_008B780E: GoTo loc_008B781B
  loc_008B7810: 'Referenced from: 008B7806
  loc_008B7816: call _adj_fdiv_m32(var_403DD4)
  loc_008B781B: 'Referenced from: 008B780E
  loc_008B7837: var_40 = Str(_adj_fdiv_m32(var_403DD4))
  loc_008B7855: var_18 = CStr(Trim(var_40))
  loc_008B785D: Text1.Text = var_18
  loc_008B78F2: If var_8D9000 <> 0 Then GoTo loc_008B78FC
  loc_008B78FA: GoTo loc_008B7907
  loc_008B78FC: 'Referenced from: 008B78F2
  loc_008B7902: call _adj_fdiv_m32(var_414F74, Me, 00000008h, var_20, var_1C, Me, Me)
  loc_008B7907: 'Referenced from: 008B78FA
  loc_008B7917: var_ret_7 = Int(((?.?<E-313 / 1440) * 10))
  loc_008B7924: If var_8D9000 <> 0 Then GoTo loc_008B792E
  loc_008B792C: GoTo loc_008B7939
  loc_008B792E: 'Referenced from: 008B7924
  loc_008B7934: call _adj_fdiv_m32(var_403DD4)
  loc_008B7939: 'Referenced from: 008B792C
  loc_008B7955: var_40 = Str(_adj_fdiv_m32(var_403DD4))
  loc_008B7973: var_18 = CStr(Trim(var_40))
  loc_008B797B: Text1.Text = var_18
  loc_008B79CB: 'Referenced from: 008B7651
  loc_008B79D7: setnz dl
  loc_008B79E0: setnz cl
  loc_008B79E5: If ecx <> 0 Then GoTo loc_008B7B21
  loc_008B79FC: If var_8D9000 <> 0 Then GoTo loc_008B7A06
  loc_008B7A04: GoTo loc_008B7A11
  loc_008B7A06: 'Referenced from: 008B79FC
  loc_008B7A0C: call _adj_fdiv_m32(var_403BF8, Me)
  loc_008B7A11: 'Referenced from: 008B7A04
  loc_008B7A3C: var_eax = Unknown_VTable_Call[ecx+00000040h]
  loc_008B7A6D: var_ret_8 = Int((?.?<E-313 * 10))
  loc_008B7A7A: If var_8D9000 <> 0 Then GoTo loc_008B7A84
  loc_008B7A82: GoTo loc_008B7A8F
  loc_008B7A84: 'Referenced from: 008B7A7A
  loc_008B7A8A: call _adj_fdiv_m32(var_403DD4, var_1C, 00000006h, var_20, var_1C, Err.Number)
  loc_008B7A8F: 'Referenced from: 008B7A82
  loc_008B7AAB: var_40 = Str(_adj_fdiv_m32(var_403DD4, var_1C, 00000006h, var_20, var_1C, Err.Number))
  loc_008B7AC9: var_18 = CStr(Trim(var_40))
  loc_008B7AD1: Text1.Text = var_18
  loc_008B7B21: 'Referenced from: 008B79E5
  loc_008B7B28: If arg_C <> 6 Then GoTo loc_008B7C64
  loc_008B7B3F: If var_8D9000 <> 0 Then GoTo loc_008B7B49
  loc_008B7B47: GoTo loc_008B7B54
  loc_008B7B49: 'Referenced from: 008B7B3F
  loc_008B7B4F: call _adj_fdiv_m32(var_403BF8, Me)
  loc_008B7B54: 'Referenced from: 008B7B47
  loc_008B7B7F: var_eax = Unknown_VTable_Call[eax+00000040h]
  loc_008B7BB0: var_ret_9 = Int((?.?<E-313 * 10))
  loc_008B7BBD: If var_8D9000 <> 0 Then GoTo loc_008B7BC7
  loc_008B7BC5: GoTo loc_008B7BD2
  loc_008B7BC7: 'Referenced from: 008B7BBD
  loc_008B7BCD: call _adj_fdiv_m32(var_403DD4, Err.Number, 00000005h, var_20, var_1C, Err.Number)
  loc_008B7BD2: 'Referenced from: 008B7BC5
  loc_008B7BEE: var_40 = Str(_adj_fdiv_m32(var_403DD4, Err.Number, 00000005h, var_20, var_1C, Err.Number))
  loc_008B7C0C: var_18 = CStr(Trim(var_40))
  loc_008B7C14: Text1.Text = var_18
  loc_008B7C64: 'Referenced from: 008B7B28
  loc_008B7C6B: If arg_C <> 7 Then GoTo loc_008B7FD5
  loc_008B7C90: If var_8D9000 <> 0 Then GoTo loc_008B7C96
  loc_008B7C92: fdivp st1
  loc_008B7C94: GoTo loc_008B7C9F
  loc_008B7C96: 'Referenced from: 008B7C90
  loc_008B7C99: call _adj_fdiv_r(var_40, Me)
  loc_008B7C9F: 'Referenced from: 008B7C94
  loc_008B7CFB: var_ret_A = Int((?.?<E-313 * 10))
  loc_008B7D08: If var_8D9000 <> 0 Then GoTo loc_008B7D12
  loc_008B7D10: GoTo loc_008B7D1D
  loc_008B7D12: 'Referenced from: 008B7D08
  loc_008B7D18: call _adj_fdiv_m32(var_403DD4, Me, 00000001h, var_20, var_1C, Me)
  loc_008B7D1D: 'Referenced from: 008B7D10
  loc_008B7D39: var_40 = Str((((&H0000000300905A4D&H * &H42700000&H) * &H00905A4D&H) / &H41200000&H))
  loc_008B7D57: var_18 = CStr(Trim(var_40))
  loc_008B7D5F: Text1.Text = var_18
  loc_008B7DCC: var_eax = Unknown_VTable_Call[eax+00000040h]
  loc_008B7E03: var_ret_B = Int(((?.?<E-313 * 60) * 10))
  loc_008B7E10: If var_8D9000 <> 0 Then GoTo loc_008B7E1A
  loc_008B7E18: GoTo loc_008B7E25
  loc_008B7E1A: 'Referenced from: 008B7E10
  loc_008B7E20: call _adj_fdiv_m32(var_403DD4, var_30, 00000003h, var_20, var_1C, var_30, Me)
  loc_008B7E25: 'Referenced from: 008B7E18
  loc_008B7E41: var_40 = Str(_adj_fdiv_m32(var_403DD4, 4, 00000003h, var_20, var_1C, 4, Me))
  loc_008B7E5F: var_18 = CStr(Trim(var_40))
  loc_008B7E67: Text1.Text = var_18
  loc_008B7ED4: var_eax = Unknown_VTable_Call[ecx+00000040h]
  loc_008B7EFC: If var_8D9000 <> 0 Then GoTo loc_008B7F06
  loc_008B7F04: GoTo loc_008B7F11
  loc_008B7F06: 'Referenced from: 008B7EFC
  loc_008B7F0C: call _adj_fdiv_m32(var_414F70, var_1C, 00000008h, var_20, var_1C, var_40, Me)
  loc_008B7F11: 'Referenced from: 008B7F04
  loc_008B7F21: var_ret_C = Int(((?.?<E-313 / 24) * 10))
  loc_008B7F2E: If var_8D9000 <> 0 Then GoTo loc_008B7F38
  loc_008B7F36: GoTo loc_008B7F43
  loc_008B7F38: 'Referenced from: 008B7F2E
  loc_008B7F3E: call _adj_fdiv_m32(var_403DD4)
  loc_008B7F43: 'Referenced from: 008B7F36
  loc_008B7F5F: var_40 = Str(_adj_fdiv_m32(var_403DD4))
  loc_008B7F7D: var_18 = CStr(Trim(var_40))
  loc_008B7F85: Text1.Text = var_18
  loc_008B7FD5: 'Referenced from: 008B7C6B
  loc_008B7FDC: If arg_C <> 8 Then GoTo loc_008B833C
  loc_008B8007: If var_8D9000 <> 0 Then GoTo loc_008B800D
  loc_008B8009: fdivp st1
  loc_008B800B: GoTo loc_008B8016
  loc_008B800D: 'Referenced from: 008B8007
  loc_008B8010: call _adj_fdiv_r(arg_C, Me)
  loc_008B8016: 'Referenced from: 008B800B
  loc_008B8041: var_eax = Unknown_VTable_Call[eax+00000040h]
  loc_008B8072: var_ret_D = Int((?.?<E-313 * 10))
  loc_008B807F: If var_8D9000 <> 0 Then GoTo loc_008B8089
  loc_008B8087: GoTo loc_008B8094
  loc_008B8089: 'Referenced from: 008B807F
  loc_008B808F: call _adj_fdiv_m32(var_403DD4, Err.Number, 00000001h, var_20, var_1C, Err.Number)
  loc_008B8094: 'Referenced from: 008B8087
  loc_008B80B0: var_40 = Str(((((&H0000000300905A4D&H * &H42700000&H) * &H41C00000&H) * &H00905A4D&H) / &H41200000&H))
  loc_008B80CE: var_18 = CStr(Trim(var_40))
  loc_008B80D6: Text1.Text = var_18
  loc_008B8143: var_eax = Unknown_VTable_Call[ecx+00000040h]
  loc_008B8180: var_ret_E = Int((((?.?<E-313 * 60) * 24) * 10))
  loc_008B818D: If var_8D9000 <> 0 Then GoTo loc_008B8197
  loc_008B8195: GoTo loc_008B81A2
  loc_008B8197: 'Referenced from: 008B818D
  loc_008B819D: call _adj_fdiv_m32(var_403DD4, var_1C, 00000003h, var_20, var_1C, var_40, Me)
  loc_008B81A2: 'Referenced from: 008B8195
  loc_008B81BE: var_40 = Str(_adj_fdiv_m32(var_403DD4, var_1C, 00000003h, var_20, var_1C, var_40, Me))
  loc_008B81DC: var_18 = CStr(Trim(var_40))
  loc_008B81E4: Text1.Text = var_18
  loc_008B8288: var_ret_F = Int(((?.?<E-313 * 24) * 10))
  loc_008B8295: If var_8D9000 <> 0 Then GoTo loc_008B829F
  loc_008B829D: GoTo loc_008B82AA
  loc_008B829F: 'Referenced from: 008B8295
  loc_008B82A5: call _adj_fdiv_m32(var_403DD4, Me, 00000007h, var_20, var_1C, Me, Me)
  loc_008B82AA: 'Referenced from: 008B829D
  loc_008B82E4: var_18 = CStr(Trim(Str(_adj_fdiv_m32(var_403DD4, Me, 00000007h, var_20, var_1C, Me, Me))))
  loc_008B82EC: Text1.Text = var_18
  loc_008B833C: 'Referenced from: 008B7FDC
  loc_008B8340: 'Referenced from: 008B713B
  loc_008B8349: GoTo loc_008B837C
  loc_008B837B: Exit Sub
  loc_008B837C: 'Referenced from: 008B8349
  loc_008B837C: Exit Sub
End Sub

Private Sub Form_Load() '8B7020
  loc_008B7073: var_eax = Call CScalculator.Text1_Change
  loc_008B709D: var_eax = Call CScalculator.Text1_Change
End Sub
