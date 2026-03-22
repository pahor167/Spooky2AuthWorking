VERSION 5.00
Begin VB.Form CustomCarrierSweep
  Caption = "Create Carrier Sweep"
  ScaleMode = 1
  AutoRedraw = False
  FontTransparent = True
  Icon = "CustomCarrierSweep.frx":0
  LinkTopic = "Form1"
  ClientLeft = 60
  ClientTop = 510
  ClientWidth = 20475
  ClientHeight = 12540
  BeginProperty Font
    Name = "Arial"
    Size = 8.25
    Charset = 0
    Weight = 400
    Underline = 0 'False
    Italic = 0 'False
    Strikethrough = 0 'False
  EndProperty
  Begin CommandButton Command1
    Left = 13200
    Top = 600
    Width = 375
    Height = 375
    Visible = 0   'False
    TabIndex = 32
    ToolTipText = "Hidden control to refresh the graph display"
  End
  Begin CheckBox Check1
    Left = 240
    Top = 7080
    Width = 255
    Height = 255
    TabIndex = 31
    ToolTipText = "CustomCarrierSweep.frx":FD55
  End
  Begin PictureBox Picture1
    BackColor = &HD4D4D4&
    ForeColor = &H0&
    Left = 240
    Top = 7440
    Width = 17775
    Height = 4815
    TabIndex = 29
    ScaleMode = 1
    AutoRedraw = True
    FontTransparent = True
    ToolTipText = "Spectral analysis"
  End
  Begin TextBox Text28
    ForeColor = &H0&
    Left = 240
    Top = 5040
    Width = 7935
    Height = 1815
    TabIndex = 26
    MultiLine = -1  'True
    ScrollBars = 2
    BeginProperty Font
      Name = "Arial"
      Size = 11.25
      Charset = 0
      Weight = 400
      Underline = 0 'False
      Italic = 0 'False
      Strikethrough = 0 'False
    EndProperty
    ToolTipText = "Enter notes for the Program here. This will help in searches later"
  End
  Begin TextBox Text1
    Index = 6
    ForeColor = &H0&
    Left = 3360
    Top = 3240
    Width = 1815
    Height = 345
    Text = "5500"
    TabIndex = 21
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
    ToolTipText = "The Modulation Frequency directly affects the overall Sweep duration since the Carrier must sweep over this range"
  End
  Begin TextBox Text1
    Index = 5
    ForeColor = &H0&
    Left = 240
    Top = 2130
    Width = 14295
    Height = 345
    TabIndex = 19
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
    ToolTipText = "Enter a descriptive Program name"
  End
  Begin TextBox Text1
    Index = 4
    ForeColor = &H0&
    Left = 3360
    Top = 4200
    Width = 1815
    Height = 345
    Text = "300"
    TabIndex = 15
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
    ToolTipText = "Carrier Sweeps gradually increase the Carrier frequency. Each frequency is guaranteed to be hit for this duration during the scan."
  End
  Begin TextBox Text1
    Index = 3
    ForeColor = &H0&
    Left = 3360
    Top = 3720
    Width = 1815
    Height = 345
    Text = ".025"
    TabIndex = 12
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
    ToolTipText = "Frequencies within this tolerance are deemed effective. Small tolerances will increase the duration of Carrier Sweeps"
  End
  Begin TextBox Text1
    Index = 0
    ForeColor = &H0&
    Left = 3360
    Top = 2760
    Width = 1815
    Height = 375
    Text = "200000"
    TabIndex = 5
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
    ToolTipText = "CustomCarrierSweep.frx":FE30
  End
  Begin ActiveResize ActiveResize1
  End
  Begin CommandButton Command5
    Left = 19800
    Top = 120
    Width = 495
    Height = 525
    TabIndex = 3
    BeginProperty Font
      Name = "Arial"
      Size = 11.25
      Charset = 0
      Weight = 400
      Underline = 0 'False
      Italic = 0 'False
      Strikethrough = 0 'False
    EndProperty
    Picture = "CustomCarrierSweep.frx":FEB4
    ToolTipText = "Add the program to the Custom database"
    Style = 1
  End
  Begin Label Label2
    Caption = "Sweep Info"
    Index = 4
    BackColor = &HFFFFFF&
    ForeColor = &H0&
    Left = 10560
    Top = 6960
    Width = 8775
    Height = 375
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
  Begin Label Label1
    Caption = "Ensure ""Out 1 Fixed"" option is not selected in the Spooky2 Output Shadowing pane."
    Index = 2
    BackColor = &HFFFFFF&
    ForeColor = &H0&
    Left = 240
    Top = 840
    Width = 18495
    Height = 375
    TabIndex = 33
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
  Begin Label Label4
    Caption = "Logarithmic (dB)"
    BackColor = &HFFFFFF&
    ForeColor = &H0&
    Left = 600
    Top = 7080
    Width = 4215
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
  Begin Label Label2
    Caption = "Sweep Info"
    Index = 3
    BackColor = &HFFFFFF&
    ForeColor = &H0&
    Left = 10560
    Top = 6600
    Width = 8775
    Height = 375
    TabIndex = 28
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
    Caption = "Notes"
    Index = 0
    BackColor = &HFFFFFF&
    ForeColor = &H0&
    Left = 240
    Top = 4680
    Width = 1095
    Height = 375
    TabIndex = 27
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
  Begin Label Label2
    Caption = "Sweep Info"
    Index = 2
    BackColor = &HFFFFFF&
    ForeColor = &H0&
    Left = 10560
    Top = 6240
    Width = 8775
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
  Begin Label Label2
    Caption = "Sweep Info"
    Index = 1
    BackColor = &HFFFFFF&
    ForeColor = &H0&
    Left = 10560
    Top = 5880
    Width = 8895
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
  Begin Label Label1
    Caption = "The modulation is a fixed frequency. Low frequencies provide a narrow but powerful application of frequencies."
    Index = 6
    BackColor = &HFFFFFF&
    ForeColor = &H0&
    Left = 6120
    Top = 3120
    Width = 14295
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
    Caption = "Hz"
    Index = 9
    BackColor = &HFFFFFF&
    ForeColor = &H0&
    Left = 5400
    Top = 3240
    Width = 615
    Height = 255
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
    Caption = "Modulation Frequency"
    Index = 7
    BackColor = &HFFFFFF&
    ForeColor = &H0&
    Left = 240
    Top = 3240
    Width = 3135
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
  Begin Label Label1
    Caption = "Apply the frequency (within the Frequency Tolerance) for this number of seconds."
    Index = 5
    BackColor = &HFFFFFF&
    ForeColor = &H0&
    Left = 6120
    Top = 3840
    Width = 14295
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
  Begin Label Label1
    Caption = "The frequencies in this range will be applied for the Frequency Application Time."
    Index = 4
    BackColor = &HFFFFFF&
    ForeColor = &H0&
    Left = 6120
    Top = 3480
    Width = 14295
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
    Caption = "Secs"
    Index = 11
    BackColor = &HFFFFFF&
    ForeColor = &H0&
    Left = 5400
    Top = 4200
    Width = 615
    Height = 255
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
    Caption = "Application Time"
    Index = 6
    BackColor = &HFFFFFF&
    ForeColor = &H0&
    Left = 240
    Top = 4200
    Width = 3015
    Height = 375
    TabIndex = 14
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
    Index = 5
    BackColor = &HFFFFFF&
    ForeColor = &H0&
    Left = 5400
    Top = 3720
    Width = 495
    Height = 255
    TabIndex = 13
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
    Top = 3720
    Width = 3135
    Height = 375
    TabIndex = 11
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
  Begin Label Label2
    Caption = "Sweep Info"
    Index = 0
    BackColor = &HFFFFFF&
    ForeColor = &H0&
    Left = 10560
    Top = 5520
    Width = 8775
    Height = 375
    TabIndex = 10
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
    Caption = "Carrier Sweep Info:"
    Index = 24
    BackColor = &HFFFFFF&
    ForeColor = &H0&
    Left = 10560
    Top = 4920
    Width = 2415
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
  Begin Label Label1
    Caption = "CustomCarrierSweep.frx":106C6
    Index = 3
    BackColor = &HFFFFFF&
    ForeColor = &H0&
    Left = 240
    Top = 120
    Width = 18135
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
  Begin Label Label1
    Caption = "Carrier sweeps distribute power over a broad range. This frequency region will receive most of the power."
    Index = 1
    BackColor = &HFFFFFF&
    ForeColor = &H0&
    Left = 6120
    Top = 2760
    Width = 14295
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
  Begin Image Image1
    Picture = "CustomCarrierSweep.frx":10763
    Left = 8280
    Top = 4560
    Width = 2175
    Height = 2130
    Stretch = -1  'True
  End
  Begin Label Label30
    Caption = "Hz"
    Index = 8
    BackColor = &HFFFFFF&
    ForeColor = &H0&
    Left = 5400
    Top = 2760
    Width = 615
    Height = 255
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
    Caption = "Carrier Center Frequency"
    Index = 1
    BackColor = &HFFFFFF&
    ForeColor = &H0&
    Left = 240
    Top = 2760
    Width = 3135
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
    Caption = "Give this program a descriptive name. This will assist you to find it at a later date."
    BackColor = &HFFFFFF&
    ForeColor = &H0&
    Left = 240
    Top = 1440
    Width = 17175
    Height = 375
    TabIndex = 2
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
  Begin Label Label1
    Caption = "Program Name"
    Index = 0
    BackColor = &HFFFFFF&
    ForeColor = &H0&
    Left = 240
    Top = 1800
    Width = 1815
    Height = 255
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
  Begin Label Label1
    Caption = "You may create your own sweep to suit your requirements. Always connect Out 1 to Input, Out 2 to Modulation."
    Index = 18
    BackColor = &HFFFFFF&
    ForeColor = &H0&
    Left = 240
    Top = 480
    Width = 18495
    Height = 375
    TabIndex = 0
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

Attribute VB_Name = "CustomCarrierSweep"


Private Sub Command5_Click() '8BB1B0
  Dim var_B8 As TextBox
  Dim var_B0 As TextBox
  loc_008BB215: On Error Resume Next
  loc_008BB23C: var_B0 = var_28
  loc_008BB25C: var_B4 = var_B0
  loc_008BB2B4: var_24 = Text1.Text
  loc_008BB2BC: var_BC = var_24
  loc_008BB309: eax = Len(var_24) + 1
  loc_008BB30C: var_C0 = Len(var_24) + 1
  loc_008BB338: If var_C0 = 0 Then GoTo loc_008BB522
  loc_008BB35F: var_B0 = var_28
  loc_008BB37F: var_B4 = var_B0
  loc_008BB3BE: var_B8 = var_2C
  loc_008BB3DA: Text1.BackColor = %x1 = Text1.Name
  loc_008BB3DF: var_BC = var_B8
  loc_008BB46A: var_eax = Unknown_VTable_Call[ecx+00000040h]
  loc_008BB46F: var_B4 = Unknown_VTable_Call[ecx+00000040h]
  loc_008BB4C3: var_eax = Text1.SetFocus
  loc_008BB4CB: var_BC = Text1.SetFocus
  loc_008BB51D: GoTo loc_008BCB69
  loc_008BB522: 'Referenced from: 008BB338
  loc_008BB55E: var_eax = Unknown_VTable_Call[ecx+00000040h]
  loc_008BB563: var_B4 = Unknown_VTable_Call[ecx+00000040h]
  loc_008BB5BB: var_24 = Text1.Text
  loc_008BB5C3: var_BC = var_24
  loc_008BB610: eax = Len(var_24) + 1
  loc_008BB613: var_C0 = Len(var_24) + 1
  loc_008BB63F: If var_C0 = 0 Then GoTo loc_008BB829
  loc_008BB681: var_eax = Unknown_VTable_Call[ecx+00000040h]
  loc_008BB686: var_B4 = Unknown_VTable_Call[ecx+00000040h]
  loc_008BB6C5: var_B8 = var_2C
  loc_008BB6E1: Text1.BackColor = %x1 = Text1.Name
  loc_008BB6E6: var_BC = var_B8
  loc_008BB771: var_eax = Unknown_VTable_Call[eax+00000040h]
  loc_008BB776: var_B4 = Unknown_VTable_Call[eax+00000040h]
  loc_008BB7CA: var_eax = Text1.SetFocus
  loc_008BB7D2: var_BC = Text1.SetFocus
  loc_008BB824: GoTo loc_008BCB69
  loc_008BB829: 'Referenced from: 008BB63F
  loc_008BB865: var_eax = Unknown_VTable_Call[eax+00000040h]
  loc_008BB86A: var_B4 = Unknown_VTable_Call[eax+00000040h]
  loc_008BB8C2: var_24 = Text1.Text
  loc_008BB8CA: var_BC = var_24
  loc_008BB917: eax = Len(var_24) + 1
  loc_008BB91A: var_C0 = Len(var_24) + 1
  loc_008BB946: If var_C0 = 0 Then GoTo loc_008BBB30
  loc_008BB988: var_eax = Unknown_VTable_Call[eax+00000040h]
  loc_008BB98D: var_B4 = Unknown_VTable_Call[eax+00000040h]
  loc_008BB9CC: var_B8 = var_2C
End Sub

Private Sub Command1_Click() '8BB0D0
  loc_008BB113: var_eax = CustomCarrierSweep.Proc_15_7_8BDAE0(Me, edi)
End Sub

Private Sub Check1_Click() '8BB140
  loc_008BB183: var_eax = CustomCarrierSweep.Proc_15_7_8BDAE0(Me, edi)
End Sub

Private Sub Text1_LostFocus() '8BCEA0
  Dim var_28 As TextBox
  Dim var_30 As TextBox
  Dim var_68 As TextBox
  loc_008BCF12: If arg_C <> 0 Then GoTo loc_008BD059
  loc_008BCF65: var_18 = Text1.Text
  loc_008BCFA1: var_eax = Unknown_VTable_Call[ecx+00000040h]
  loc_008BD005: var_eax = CustomCarrierSweep.Proc_15_12_8C0A20(Me, var_18, var_4C, var_54, 8, var_20, var_2C, 00000000h)
  loc_008BD012: Text1.Text = var_20
  loc_008BD057: GoTo loc_008BD05F
  loc_008BD059: 'Referenced from: 008BCF12
  loc_008BD05F: 'Referenced from: 008BD057
  loc_008BD066: If arg_C <> 1 Then GoTo loc_008BD188
  loc_008BD08B: var_eax = Unknown_VTable_Call[ecx+00000040h]
  loc_008BD0AB: var_18 = Text1.Text
  loc_008BD136: var_eax = CustomCarrierSweep.Proc_15_12_8C0A20(Me, var_18, var_4C, var_54, var_44, var_20, Me, 00000001h)
  loc_008BD143: Text1.Text = var_20
  loc_008BD188: 'Referenced from: 008BD066
  loc_008BD18F: If arg_C <> 2 Then GoTo loc_008BD46C
  loc_008BD1D4: var_18 = Text1.Text
  loc_008BD25F: var_eax = CustomCarrierSweep.Proc_15_12_8C0A20(Me, var_18, var_4C, var_54, var_44, var_20, Me, 00000002h)
  loc_008BD26C: Text1.Text = var_20
  loc_008BD2D0: var_eax = Unknown_VTable_Call[ecx+00000040h]
  loc_008BD2F0: var_1C = Text1.Text
  loc_008BD336: var_eax = Unknown_VTable_Call[eax+00000040h]
  loc_008BD356: var_18 = Text1.Text
  loc_008BD393: fcomp real8 ptr [00401EB8h]
  loc_008BD3A5: GoTo loc_008BD3A9
  loc_008BD3A9: 'Referenced from: 008BD3A5
  loc_008BD3DB: If var_2C = 0 Then GoTo loc_008BD46C
  loc_008BD429: var_18 = CStr(30)
  loc_008BD436: Text1.Text = var_18
  loc_008BD46C: 'Referenced from: 008BD18F
  loc_008BD473: If arg_C <> 3 Then GoTo loc_008BD5A1
  loc_008BD498: var_eax = Unknown_VTable_Call[ecx+00000040h]
  loc_008BD4B8: var_18 = Text1.Text
  loc_008BD54F: var_eax = CustomCarrierSweep.Proc_15_12_8C0A20(Me, var_18, var_4C, var_54, 8, var_20, Me, 00000003h)
  loc_008BD55C: Text1.Text = var_20
  loc_008BD5A1: 'Referenced from: 008BD473
  loc_008BD5A8: If arg_C <> 4 Then GoTo loc_008BD6D6
  loc_008BD5ED: var_18 = Text1.Text
  loc_008BD684: var_eax = CustomCarrierSweep.Proc_15_12_8C0A20(Me, var_18, var_4C, var_54, 8, var_20, Me, 00000004h)
  loc_008BD691: Text1.Text = var_20
  loc_008BD6D6: 'Referenced from: 008BD5A8
  loc_008BD6DD: If arg_C <> 5 Then GoTo loc_008BD7C1
  loc_008BD71C: var_68 = var_30
  loc_008BD73B: var_eax = Unknown_VTable_Call[ecx+00000040h]
  loc_008BD75A: var_38 = var_28
  loc_008BD76E: var_eax = CustomCarrierSweep.Proc_15_5_8BCBE0(Me, 9, var_18, var_24, 00000005h, var_28, var_24, var_30)
  loc_008BD77E: Text1.Text = var_18
  loc_008BD7C1: 'Referenced from: 008BD6DD
  loc_008BD7C8: If arg_C <> 6 Then GoTo loc_008BD8F6
  loc_008BD80D: var_18 = Text1.Text
  loc_008BD8A4: var_eax = CustomCarrierSweep.Proc_15_12_8C0A20(Me, var_18, var_4C, var_54, 8, var_20, Me, 00000006h)
  loc_008BD8B1: Text1.Text = var_20
  loc_008BD8F6: 'Referenced from: 008BD7C8
  loc_008BD91C: var_eax = Unknown_VTable_Call[ecx+00000040h]
  loc_008BD93C: var_18 = Text1.Text
  loc_008BD96B: ebx = (var_18 = vbNullString) + 1
  loc_008BD98D: If (var_18 = vbNullString) + 1 = 0 Then GoTo loc_008BD9F3
  loc_008BD9D5: Text1.BackColor = %x1 = Text1.Name
  loc_008BD9F1: GoTo loc_008BDA56
  loc_008BD9F3: 'Referenced from: 008BD98D
  loc_008BDA19: var_eax = Unknown_VTable_Call[ecx+00000040h]
  loc_008BDA3A: Text1.BackColor = var_00FFFFFF
  loc_008BDA56: 'Referenced from: 008BD9F1
  loc_008BDA64: var_eax = CustomCarrierSweep.Proc_15_7_8BDAE0(Me, var_24, var_28, Me, var_24)
  loc_008BDA77: GoTo loc_008BDAB2
  loc_008BDAB1: Exit Sub
  loc_008BDAB2: 'Referenced from: 008BDA77
  loc_008BDAB2: Exit Sub
End Sub

Private Sub Form_Load() '8BAF40
  loc_008BAFA5: On Error Resume Next
  loc_008BAFE9: var_24 = CurDir(vbNullString)
  loc_008BAFF7: ecx = var_24
  loc_008BB02D: var_eax = CustomCarrierSweep.Proc_15_10_8C0430(Me, var_24, FFFFFFFFh)
  loc_008BB03C: ecx = var_24
  loc_008BB06C: var_eax = CustomCarrierSweep.Proc_15_7_8BDAE0(Me, edi)
  loc_008BB08B: GoTo loc_008BB0AA
  loc_008BB0A9: Exit Sub
  loc_008BB0AA: 'Referenced from: 008BB08B
End Sub

Public Sub Proc_15_5_8BCBE0
  loc_008BCC39: ecx = vbNullString
  loc_008BCC57: call __vbaVarVargNofree(__vbaVarVargNofree, arg_C, ebx)
  loc_008BCC64: var_34 = Len(__vbaVarVargNofree(__vbaVarVargNofree, arg_C, ebx))
  loc_008BCC74: If (var_34 = "") = 0 Then GoTo loc_008BCC8E
  loc_008BCC7E: var_20 = vbNullString
  loc_008BCC89: GoTo loc_008BCE52
  loc_008BCC8E: 'Referenced from: 008BCC74
  loc_008BCC93: call __vbaVarVargNofree(var_008BCE5C)
  loc_008BCCA9: var_80 = CLng(Len(__vbaVarVargNofree(var_008BCE5C)))
  loc_008BCCB4: If 00000001h > 0 Then GoTo loc_008BCE0A
  loc_008BCCE7: var_1C = Mid(arg_C, 1, 1)
  loc_008BCD05: var_5C = var_1C
  loc_008BCD28: eax = (var_1C = var_004A5128) + 1
  loc_008BCD32: var_6C = (var_1C = var_004A5128) + 1
  loc_008BCD42: var_ret_2 = (var_1C = Chr(34))
  loc_008BCD51: call Or(var_54, var_74, var_ret_2)
  loc_008BCD72: If CBool(Or(var_54, var_74, var_ret_2)) = 0 Then GoTo loc_008BCD86
  loc_008BCD84: GoTo loc_008BCD8C
  loc_008BCD86: 'Referenced from: 008BCD72
  loc_008BCD8C: 'Referenced from: 008BCD84
  loc_008BCD9D: If (var_1C <> var_004A33E0) <> 0 Then GoTo loc_008BCDA9
  loc_008BCDA7: var_1C = vbNullString
  loc_008BCDA9: 'Referenced from: 008BCD9D
  loc_008BCDBA: If (var_1C <> var_004AE7C8) <> 0 Then GoTo loc_008BCDC6
  loc_008BCDC6: 'Referenced from: 008BCDBA
  loc_008BCDE8: var_8C = var_8C & var_1C
  loc_008BCDFF: 00000001h = 00000001h + 00000001h
  loc_008BCE05: GoTo loc_008BCCB1
  loc_008BCE0A: 'Referenced from: 008BCCB4
  loc_008BCE15: var_20 = eax
  loc_008BCE20: GoTo loc_008BCE52
  loc_008BCE26: If var_4 = 0 Then GoTo loc_008BCE31
  loc_008BCE31: 'Referenced from: 008BCE26
  loc_008BCE51: Exit Sub
  loc_008BCE52: 'Referenced from: 008BCC89
  loc_008BCE5B: Exit Sub
End Sub

Public Sub Proc_15_6_8BCE80
  loc_008BCE87: var_eax = Me.1812
End Sub

Public Sub Proc_15_7_8BDAE0
  Dim var_7C As TextBox
  Dim var_78 As Variant
  Dim var_A4 As Label
  Dim var_138 As TextBox
  loc_008BDC08: var_44 = Text1.Text
  loc_008BDC2C: ecx = var_44
  loc_008BDC6D: var_eax = Unknown_VTable_Call[ecx+00000040h]
  loc_008BDC91: var_44 = Text1.Text
  loc_008BDCC0: If var_8D9000 <> 0 Then GoTo loc_008BDCCA
  loc_008BDCC8: GoTo loc_008BDCDB
  loc_008BDCCA: 'Referenced from: 008BDCC0
  loc_008BDCDB: 'Referenced from: 008BDCC8
  loc_008BDD47: var_44 = Text1.Text
  loc_008BDDD0: var_44 = Text1.Text
  loc_008BDE5A: var_44 = Text1.Text
  loc_008BDEA1: fcomp real8 ptr [00401EB8h]
  loc_008BDEB8: fcomp real8 ptr [00401EB8h]
  loc_008BDED2: If var_8D9000 <> 0 Then GoTo loc_008BDED9
  loc_008BDED7: GoTo loc_008BDEE4
  loc_008BDED9: 'Referenced from: 008BDED2
  loc_008BDEE4: 'Referenced from: 008BDED7
  loc_008BDEF4: fcomp real8 ptr [00401EB8h]
  loc_008BDF11: fsubr st0, real8 ptr var_30
  loc_008BDF20: fsubp st1
  loc_008BDF52: faddp st1
  loc_008BDF6D: fcomp real8 ptr [00401EB8h]
  loc_008BDF9A: If var_8D9000 <> 0 Then GoTo loc_008BDFA1
  loc_008BDF9F: GoTo loc_008BDFAC
  loc_008BDFA1: 'Referenced from: 008BDF9A
  loc_008BDFAC: 'Referenced from: 008BDF9F
  loc_008BDFC1: var_FC = esi+00000038h
  loc_008BDFD9: var_A4 = Round(, 8)
  loc_008BDFFD: edi = Label2.FormatLength
  loc_008BE03F: var_eax = CustomCarrierSweep.Proc_15_11_8C0570(Me, var_124, var_44, var_7C, var_78, var_A4, Me, var_28)
  loc_008BE074: var_4C = "Carrier Start Frequency " & var_44 & " Hz."
  loc_008BE0D6: var_FC = %x1 = Label2.TextLayout
  loc_008BE0EE: var_A4 = Round(, 8)
  loc_008BE113: 1 = Label2.FormatLength
  loc_008BE155: var_eax = CustomCarrierSweep.Proc_15_11_8C0570(Me, var_124, var_44, var_7C)
  loc_008BE184: var_4C = "Carrier Finish Frequency " & var_44 & " Hz."
  loc_008BE190: var_eax = Unknown_VTable_Call[ecx+00000054h]
  loc_008BE1EE: var_FC = var_28
  loc_008BE1FE: var_A4 = Round(, 8)
  loc_008BE223: 2 = Label2.FormatLength
  loc_008BE265: var_eax = CustomCarrierSweep.Proc_15_11_8C0570(Me, var_124, var_44, var_7C)
  loc_008BE294: var_4C = "Carrier Sweep Speed " & var_44 & " Hz/Sec."
  loc_008BE2A0: var_eax = Unknown_VTable_Call[ecx+00000054h]
  loc_008BE2F5: If var_8D9000 <> 0 Then GoTo loc_008BE2FF
  loc_008BE2FD: GoTo loc_008BE310
  loc_008BE2FF: 'Referenced from: 008BE2F5
  loc_008BE310: 'Referenced from: 008BE2FD
  loc_008BE32A: If var_8D9000 <> 0 Then GoTo loc_008BE334
  loc_008BE332: GoTo loc_008BE345
  loc_008BE334: 'Referenced from: 008BE32A
  loc_008BE345: 'Referenced from: 008BE332
  loc_008BE34F: var_ret_1 = Int((var_38 / 3600))
  loc_008BE355: fsubr st0, real8 ptr var_194
  loc_008BE385: var_B4 = Round(var_28, 2)
  loc_008BE3AA: 3 = Label2.FormatLength
  loc_008BE3CF: If var_8D9000 <> 0 Then GoTo loc_008BE3D9
  loc_008BE3D7: GoTo loc_008BE3EA
  loc_008BE3D9: 'Referenced from: 008BE3CF
  loc_008BE3EA: 'Referenced from: 008BE3D7
  loc_008BE3FA: var_ret_2 = Int((var_38 / 3600))
  loc_008BE414: var_eax = CustomCarrierSweep.Proc_15_11_8C0570(Me, var_124, var_44, var_406538, var_40653C, var_7C, var_78)
  loc_008BE443: var_ret_3 = var_B4 * 60
  loc_008BE464: var_eax = CustomCarrierSweep.Proc_15_11_8C0570(Me, var_12C, var_4C, Me, Me)
  loc_008BE4B8: var_58 = "Sweep Duration " & var_44 & " Hours " & var_4C & " mins."
  loc_008BE4C4: var_eax = Unknown_VTable_Call[eax+00000054h]
  loc_008BE53B: fcomp real4 ptr [00406EF0h]
  loc_008BE562: fcomp real4 ptr [00406EF0h]
  loc_008BE58A: var_FC = var_20
  loc_008BE5A1: If var_8D9000 <> 0 Then GoTo loc_008BE5A7
  loc_008BE5A3: fdivp st1
  loc_008BE5A5: GoTo loc_008BE5B0
  loc_008BE5A7: 'Referenced from: 008BE5A1
  loc_008BE5AA: call _adj_fdiv_r(Err.Number, 00000008h)
  loc_008BE5B0: 'Referenced from: 008BE5A5
  loc_008BE5CB: var_A4 = Round(, )
  loc_008BE5F0: 4 = Label2.FormatLength
  loc_008BE612: var_138 = var_7C
  loc_008BE632: var_eax = CustomCarrierSweep.Proc_15_11_8C0570(Me, var_124, var_44, var_7C)
  loc_008BE661: var_4C = "Spectrum Percentage +/- " & var_44 & var_004A6A88
  loc_008BE6B8: GoTo loc_008BE729
  loc_008BE6D9: 4 = Label2.FormatLength
  loc_008BE6FE: var_eax = Unknown_VTable_Call[ecx+00000054h]
  loc_008BE729: 'Referenced from: 008BE6B8
  loc_008BE732: var_A4 = Chr(34)
  loc_008BE757: var_eax = Unknown_VTable_Call[ecx+00000040h]
  loc_008BE77B: var_44 = Text1.Text
  loc_008BE79C: var_AC = var_44
  loc_008BE7DE: var_FC = ",CUST,"
  loc_008BE82D: ecx = var_A4 & var_44 & Chr(34) &
  loc_008BE888: If var_8D9000 <> 0 Then GoTo loc_008BE892
  loc_008BE890: GoTo loc_008BE8A3
  loc_008BE892: 'Referenced from: 008BE888
  loc_008BE8A3: 'Referenced from: 008BE890
  loc_008BE8D0: var_B4 = Round((var_38 / 60#), 0)
  loc_008BE8F7: var_eax = CustomCarrierSweep.Proc_15_11_8C0570(Me, var_124, var_44, 00000005h, var_7C)
  loc_008BE90F: var_48 = esi+00000054h & var_44
  loc_008BE928: ecx = var_48 & var_004A187C
  loc_008BE966: var_FC = esi+00000054h
  loc_008BE9A9: ecx =  & Chr(34)
  loc_008BEA0C: var_44 = Text28.Text
  loc_008BEA35: var_9C = var_44
  loc_008BEA58: var_eax = CustomCarrierSweep.Proc_15_5_8BCBE0(Me, 8, var_48, var_78, esi)
  loc_008BEA6B: Text28.Text = var_48
  loc_008BEAD8: var_44 = Text28.Text
  loc_008BEB0A: setg cl
  loc_008BEB30: If var_138 = 0 Then GoTo loc_008BEBCC
  loc_008BEB57: var_44 = Text28.Text
  loc_008BEBA6: ecx = esi+00000054h & var_44 & ". "
  loc_008BEBCC: 'Referenced from: 008BEB30
  loc_008BEBEF: var_eax = Unknown_VTable_Call[ecx+00000040h]
  loc_008BEC1D: var_44 = Text1.Text
  loc_008BEC98: var_50 = Text1.Text
  loc_008BECE5: var_eax = Unknown_VTable_Call[ecx+00000040h]
  loc_008BED16: var_5C = Text1.Text
  loc_008BED94: var_68 = Text1.Text
  loc_008BEE27: var_64 = esi+00000054h & "Carrier Centre Frequency " & var_44 & " Hz. Modulation Frequency " & var_50 & " Hz. Frequency Tolerance " & var_5C
  loc_008BEE65: ecx = var_64 & " Hz. Application Time " & var_68 & " Seconds."
  loc_008BEF04: 0 = Label2.FormatLength
  loc_008BEF7B: ecx = esi+00000054h & var_004A353C & var_44
  loc_008BEFCB: 1 = Label2.FormatLength
  loc_008BF042: ecx = esi+00000054h & var_004A353C & var_44
  loc_008BF092: 2 = Label2.FormatLength
  loc_008BF109: ecx = esi+00000054h & var_004A353C & var_44
  loc_008BF153: var_130 = var_78
  loc_008BF159: 3 = Label2.FormatLength
  loc_008BF1CA: var_4C = esi+00000054h & var_004A353C & var_44
  loc_008BF1D0: ecx = var_4C
  loc_008BF208: var_FC = esi+00000054h
  loc_008BF28D: var_44 =  & Chr(34) & Chr(44) & Chr(34)
  loc_008BF293: ecx = var_44
  loc_008BF2DA: var_FC = Label2.Standing = %x1b
  loc_008BF2FA: var_A4 = Round(, 8)
  loc_008BF30B: var_10C = %x1 = Label2.TextLayout
  loc_008BF323: var_B4 = Round(%x1 = Label2.TextLayout, 8)
  loc_008BF34A: var_eax = CustomCarrierSweep.Proc_15_11_8C0570(Me, var_124)
  loc_008BF371: var_eax = CustomCarrierSweep.Proc_15_11_8C0570(Me, var_12C)
  loc_008BF3C8: var_58 = Text1.Text
  loc_008BF448: var_60 = esi+00000054h & var_44 & var_004A27E4 & var_4C & var_004B48D4 & var_58
  loc_008BF456: var_CC = var_60 & "f0"
  loc_008BF4A2: ecx = var_60 & "f0" & Chr(34)
  loc_008BF51D: var_FC = esi+00000054h
  loc_008BF5A2: var_44 =  & Chr(44) & Chr(44) & Chr(44)
  loc_008BF5A8: ecx = var_44
  loc_008BF5F7: var_FC = var_38
  loc_008BF60F: var_A4 = Round(, 0)
  loc_008BF636: var_eax = CustomCarrierSweep.Proc_15_11_8C0570(Me, var_124)
  loc_008BF654: ecx = esi+00000054h & var_44
  loc_008BF67F: var_11C = esi+00000040h
  loc_008BF685: call __vbaFpCSngR8
  loc_008BF6A2: var_eax = CustomCarrierSweep.Proc_15_8_8BF780(Me, var_118)
  loc_008BF6AE: GoTo loc_008BF75F
  loc_008BF75E: Exit Sub
  loc_008BF75F: 'Referenced from: 008BF6AE
  loc_008BF75F: Exit Sub
End Sub

Public Sub Proc_15_8_8BF780
  Dim var_A4 As PictureBox
  Dim var_24 As Variant
  loc_008BF7F1: var_eax = Picture1.Cls
  loc_008BF83B: var_A4 = var_24
  loc_008BF867: var_E8 = ecx
  loc_008BF86D: fchs
  loc_008BF907: var_eax = Picture1.Scale 4, var_5C
  loc_008BF952: Picture1.ForeColor = var_00FFFFFF
  loc_008BF980: 
  loc_008BF98A: If var_18 > 21 Then GoTo loc_008BFA95
  loc_008BF9C0: If var_8D9000 <> 0 Then GoTo loc_008BF9CA
  loc_008BF9C8: GoTo loc_008BF9DB
  loc_008BF9CA: 'Referenced from: 008BF9C0
  loc_008BF9DB: 'Referenced from: 008BF9C8
  loc_008BF9DD: var_A4 = var_24
  loc_008BFA17: If var_8D9000 <> 0 Then GoTo loc_008BFA21
  loc_008BFA1F: GoTo loc_008BFA32
  loc_008BFA21: 'Referenced from: 008BFA17
  loc_008BFA32: 'Referenced from: 008BFA1F
  loc_008BFA50: var_eax = Picture1.Line (var_405DE0, var_405DE4)-(var_24, 0), var_405DE0
  loc_008BFA85: 00000001h = 00000001h + var_18
  loc_008BFA90: GoTo loc_008BF980
  loc_008BFA95: 'Referenced from: 008BF98A
  loc_008BFA9C: 
  loc_008BFAA9: If var_18 > 100 Then GoTo loc_008BFB54
  loc_008BFB0F: var_eax = Picture1.Line (0, var_10C)-(var_24, var_104), 0
  loc_008BFB44: 0000000Ah = 0000000Ah + var_18
  loc_008BFB4F: GoTo loc_008BFA9C
  loc_008BFB66: var_A4 = var_24
  loc_008BFB6C: Picture1.ForeColor = 0
  loc_008BFBDA: var_eax = Picture1.Line (0, 0)-(Me, 0), 0
  loc_008BFC29: var_A4 = var_24
  loc_008BFC2F: var_eax = Picture1.Line (0, 0)-(0, var_42DC0000), 0
  loc_008BFC63: 
  loc_008BFC6D: If var_18 > 5 Then GoTo loc_008BFD37
  loc_008BFCF2: var_eax = Picture1.Line (Me, var_C0000000)-(Me, var_40000000), 0
  loc_008BFD27: 00000001h = 00000001h + var_18
  loc_008BFD32: GoTo loc_008BFC63
  loc_008BFD37: 'Referenced from: 008BFC6D
  loc_008BFD3E: 
  loc_008BFD48: If var_18 > 5 Then GoTo loc_008BFEDA
  loc_008BFD94: Picture1.CurrentX = var_24
  loc_008BFDDF: Picture1.CurrentY = var_C0400000
  loc_008BFE66: var_70 = " Hz"
  loc_008BFE95: call __vbaPrintObj(var_004A4BF0, var_24, var_24, var_28, Round((?.?<E-313# * var_13C), 0) & " Hz", 0, Me, Me, var_24, Err.Number, Me, var_24, 00000005h, Me, var_24, Me)
  loc_008BFECA: 00000001h = 00000001h + var_18
  loc_008BFED5: GoTo loc_008BFD3E
  loc_008BFEDA: 'Referenced from: 008BFD48
  loc_008BFEF2: 
  loc_008BFEFB: If var_1C > 0 Then GoTo loc_008C017A
  loc_008BFF13: var_eax = CustomCarrierSweep.Proc_15_9_8C01E0(Me, arg_C)
  loc_008BFF20: 
  loc_008BFF2A: If var_18 > 100 Then GoTo loc_008C0074
  loc_008BFF50: var_9C = Check1.Value
  loc_008BFF80: setz dl
  loc_008BFF9C: If var_AC = 0 Then GoTo loc_008BFFA6
  loc_008BFFA4: GoTo loc_008BFFD7
  loc_008BFFA6: 'Referenced from: 008BFF9C
  loc_008BFFB0: If var_8D9000 <> 0 Then GoTo loc_008BFFBA
  loc_008BFFB8: GoTo loc_008BFFC5
  loc_008BFFBA: 'Referenced from: 008BFFB0
  loc_008BFFC0: call _adj_fdiv_m32(var_40834C, var_24, 00000064h, Me, arg_10, var_20)
  loc_008BFFC5: 'Referenced from: 008BFFB8
  loc_008BFFD5: fsubp st1
  loc_008BFFD7: 'Referenced from: 008BFFA4
  loc_008C0019: var_eax = CustomCarrierSweep.Proc_15_9_8C01E0(Me, var_A0)
  loc_008C0037: fsubp st1
  loc_008C0056: var_eax = CustomCarrierSweep.Proc_15_9_8C01E0(Me, var_A0, arg_10)
  loc_008C0064: 00000001h = 00000001h + var_18
  loc_008C006F: GoTo loc_008BFF20
  loc_008C0074: 'Referenced from: 008BFF2A
  loc_008C00B7: var_9C = Check1.Value
  loc_008C00E7: setz dl
  loc_008C0100: If var_AC = 0 Then GoTo loc_008C012D
  loc_008C010C: If var_8D9000 <> 0 Then GoTo loc_008C0116
  loc_008C0114: GoTo loc_008C0121
  loc_008C0116: 'Referenced from: 008C010C
  loc_008C011C: call _adj_fdiv_m32(var_415170, var_24, Err.Number, Me, var_20, arg_10, var_20)
  loc_008C0121: 'Referenced from: 008C0114
  loc_008C012B: GoTo loc_008C0163
  loc_008C012D: 'Referenced from: 008C0100
  loc_008C013A: If var_8D9000 <> 0 Then GoTo loc_008C0144
  loc_008C0142: GoTo loc_008C014F
  loc_008C0144: 'Referenced from: 008C013A
  loc_008C014A: call _adj_fdiv_m32(var_40834C)
  loc_008C014F: 'Referenced from: 008C0142
  loc_008C015B: fsubp st1
  loc_008C0163: 'Referenced from: 008C012B
  loc_008C016E: 00000001h = 00000001h + var_1C
  loc_008C0175: GoTo loc_008BFEF2
  loc_008C0186: GoTo loc_008C01B0
  loc_008C01AF: Exit Sub
  loc_008C01B0: 'Referenced from: 008C0186
  loc_008C01B0: Exit Sub
End Sub

Public Sub Proc_15_9_8C01E0
  Dim var_20 As PictureBox
  loc_008C0216: On Error Resume Next
  loc_008C022C: fsubp st1
  loc_008C023E: fcomp real8 ptr [00401EB8h]
  loc_008C027F: fsubp st1
  loc_008C02A1: fsubp st1
  loc_008C02BA: var_eax = Picture1.Line (Me, 0)-(Me, Err.Number), 0
  loc_008C0348: var_eax = Picture1.Line (var_20, 0)-(var_20, Err.Number), 0
  loc_008C03BD: fsubp st1
  loc_008C03D2: var_eax = Picture1.Line (ecx, ecx)-(arg_14, ecx), 0
  loc_008C03F9: Exit Sub
  loc_008C0400: Method_8964E44D
  loc_008C0405: GoTo loc_008C0411
  loc_008C0410: Exit Sub
  loc_008C0411: 'Referenced from: 008C0405
  loc_008C0411: Exit Sub
End Sub

Public Sub Proc_15_10_8C0430
  loc_008C0479: call __vbaStrR8(var_33333333, var_3FF33333, edi, esi, ebx)
  loc_008C0482: var_24 = __vbaStrR8(var_33333333, var_3FF33333, edi, esi, ebx)
  loc_008C04AE: var_1C = Trim(__vbaStrR8(var_33333333, var_3FF33333, edi, esi, ebx))
  loc_008C04D1: var_44 = var_1C
  loc_008C04FF: var_18 = Mid(var_1C, 2, 1)
  loc_008C0516: GoTo loc_008C053B
  loc_008C051C: If var_4 = 0 Then GoTo loc_008C0527
  loc_008C0527: 'Referenced from: 008C051C
  loc_008C053A: Exit Sub
  loc_008C053B: 'Referenced from: 008C0516
End Sub

Public Sub Proc_15_11_8C0570
  loc_008C05C7: On Error Resume Next
  loc_008C05D9: fcomp real8 ptr [00401EB8h]
  loc_008C05FB: GoTo loc_008C098F
  loc_008C062D: var_84 = arg_C
  loc_008C0699: If (var_004A1C98 = 00000001h.hWnd = var_004A1C98) = 0 Then GoTo loc_008C06FA
  loc_008C06CC: var_eax = CustomCarrierSweep.Proc_15_14_8C11F0(Me, Format(arg_C, "################################################.################################################"), Me, 004A1C98h, var_4C)
  loc_008C06EB: var_24 = var_4C
  loc_008C06FA: 'Referenced from: 008C0699
  loc_008C070B: var_30 = Len(var_24)
  loc_008C0719: If var_30 >= 1 Then GoTo loc_008C0735
  loc_008C0730: GoTo loc_008C098F
  loc_008C0735: 'Referenced from: 008C0719
  loc_008C073F: var_B8 = var_30
  loc_008C0756: GoTo loc_008C076A
  loc_008C0758: 
  loc_008C075B: var_28 = var_28 + 1
  loc_008C0767: var_28 = var_28
  loc_008C076A: 'Referenced from: 008C0756
  loc_008C0773: If var_28 > 0 Then GoTo loc_008C082C
  loc_008C0791: var_84 = var_24
  loc_008C07C9: var_2C = Mid(var_24, var_28, 1)
  loc_008C07FE: If InStr(1, "-01234567890.", var_2C, 0) <= 0 Then GoTo loc_008C0820
  loc_008C081A: var_44 = var_44 & var_2C
  loc_008C0820: 'Referenced from: 008C07FE
  loc_008C0827: GoTo loc_008C0758
  loc_008C082C: 'Referenced from: 008C0773
  loc_008C083D: var_28 = Len(var_44)
  loc_008C0858: var_84 = var_44
  loc_008C08A6: var_B0 = (Mid(var_44, var_28, 1) = &H4A1C98)
  loc_008C08C9: If var_B0 = 0 Then GoTo loc_008C093A
  loc_008C08D5: var_28 = var_28 - 00000001h
  loc_008C08DE: var_54 = var_28
  loc_008C08EB: var_84 = var_44
  loc_008C0921: var_44 = Mid(var_44, 1, var_28)
  loc_008C093A: 'Referenced from: 008C08C9
  loc_008C0952: If (var_44 <> vbNullString) <> 0 Then GoTo loc_008C0969
  loc_008C0969: 'Referenced from: 008C0952
  loc_008C0969: GoTo loc_008C098F
  loc_008C0974: On Error Resume Next
  loc_008C098F: 'Referenced from: 008C05FB
  loc_008C098F: Exit Sub
  loc_008C099B: GoTo loc_008C09DB
  loc_008C09A5: If var_10 And 4 = 0 Then GoTo loc_008C09B0
  loc_008C09B0: 'Referenced from: 008C09A5
  loc_008C09DA: Exit Sub
  loc_008C09DB: 'Referenced from: 008C099B
End Sub

Public Sub Proc_15_12_8C0A20
  loc_008C0A77: On Error Resume Next
  loc_008C0A8C: var_24 = ecx
  loc_008C0A9E: var_40 = arg_10
End Sub

Public Sub Proc_15_13_8C0CB0
  loc_008C0D07: On Error Resume Next
  loc_008C0D1C: var_40 = vbNullString
  loc_008C0D31: var_24 = vbNullString
  loc_008C0D4A: var_34 = Len(arg_C)
  loc_008C0D58: If var_34 <> 0 Then GoTo loc_008C0D5F
  loc_008C0D5A: GoTo loc_008C1178
  loc_008C0D5F: 'Referenced from: 008C0D58
  loc_008C0D91: GoTo loc_008C0DA9
  loc_008C0D93: 
  loc_008C0DA9: 'Referenced from: 008C0D91
  loc_008C0DAC: fcomp real8 ptr var_90
  loc_008C0DD5: var_68 = arg_C
  loc_008C0E0A: var_30 = Mid(arg_C, CLng(var_2C), 1)
  loc_008C0E3B: If (var_30 <> var_004A187C) <> 0 Then GoTo loc_008C0E52
  loc_008C0E52: 'Referenced from: 008C0E3B
  loc_008C0E6A: If (var_30 <> var_004B40A0) <> 0 Then GoTo loc_008C0E81
  loc_008C0E81: 'Referenced from: 008C0E6A
  loc_008C0E99: If (var_24 <> vbNullString) <> 0 Then GoTo loc_008C0EDE
  loc_008C0EB7: If InStr(1, "-.0123456789", var_30, 0) <= 0 Then GoTo loc_008C0ED9
  loc_008C0ED3: var_24 = var_24 & var_30
  loc_008C0ED9: 'Referenced from: 008C0EB7
  loc_008C0ED9: GoTo loc_008C111F
  loc_008C0EDE: 'Referenced from: 008C0E99
  loc_008C0EF6: If (var_30 <> var_004B19FC) <> 0 Then GoTo loc_008C0F75
  loc_008C0F0B: If Len(var_24) <= 0 Then GoTo loc_008C0F70
  loc_008C0F24: fcomp real8 ptr [00401EB8h]
  loc_008C0F4D: If InStr(1, var_24, var_004B19FC, 0) <> 0 Then GoTo loc_008C0F70
  loc_008C0F6A: var_24 = var_24 & var_004B19FC
  loc_008C0F70: 'Referenced from: 008C0F0B
  loc_008C0F70: GoTo loc_008C111F
  loc_008C0F75: 'Referenced from: 008C0EF6
  loc_008C0F8D: If (var_30 <> var_004B40C8) <> 0 Then GoTo loc_008C0FDF
  loc_008C0FB7: If InStr(1, var_24, var_004B19FC, 0) <> 0 Then GoTo loc_008C0FDA
  loc_008C0FD4: var_24 = var_24 & var_004B40C8
  loc_008C0FDA: 'Referenced from: 008C0FB7
  loc_008C0FDA: GoTo loc_008C111F
  loc_008C0FDF: 'Referenced from: 008C0F8D
  loc_008C0FF7: If (var_30 <> var_004A27E4) <> 0 Then GoTo loc_008C1049
  loc_008C1021: If InStr(1, var_24, var_004B19FC, 0) <> 0 Then GoTo loc_008C1044
  loc_008C103E: var_24 = var_24 & var_004A27E4
  loc_008C1044: 'Referenced from: 008C1021
  loc_008C1044: GoTo loc_008C111F
  loc_008C1049: 'Referenced from: 008C0FF7
  loc_008C1080: If (var_30 <> var_004A187C) <> 0 Then GoTo loc_008C10E1
  loc_008C109E: If InStr(1, var_24, var_004A1C98, 0) <> 0 Then GoTo loc_008C10DF
  loc_008C10BC: If InStr(1, var_24, var_004B19FC, 0) <> 0 Then GoTo loc_008C10DF
  loc_008C10D9: var_24 = var_24 & var_004A1C98
  loc_008C10DF: 'Referenced from: 008C109E
  loc_008C10DF: GoTo loc_008C111F
  loc_008C10E1: 'Referenced from: 008C1080
  loc_008C10FD: If InStr(1, "0123456789", var_30, 0) <= 0 Then GoTo loc_008C111F
  loc_008C1119: var_24 = var_24 & var_30
  loc_008C111F: 'Referenced from: 008C0ED9
  loc_008C1126: GoTo loc_008C0D93
  loc_008C114C: var_40 = var_24
  loc_008C1152: GoTo loc_008C1178
  loc_008C115D: On Error Resume Next
  loc_008C1172: var_40 = vbNullString
  loc_008C1178: 'Referenced from: 008C0D5A
  loc_008C1178: Exit Sub
  loc_008C1184: GoTo loc_008C11AD
  loc_008C118E: If var_10 And 4 = 0 Then GoTo loc_008C1199
  loc_008C1199: 'Referenced from: 008C118E
  loc_008C11AC: Exit Sub
  loc_008C11AD: 'Referenced from: 008C1184
End Sub

Public Sub Proc_15_14_8C11F0
  loc_008C1237: var_24 = vbNullString
  loc_008C124F: var_1C = Len(arg_C)
  loc_008C125A: If 00000001h > 0 Then GoTo loc_008C12F0
  loc_008C127B: var_4C = arg_C
  loc_008C129A: var_20 = Mid(arg_C, 1, 1)
  loc_008C12C5: If (var_20 <> arg_10) <> 0 Then GoTo loc_008C12CF
  loc_008C12CD: GoTo loc_008C12D3
  loc_008C12CF: 'Referenced from: 008C12C5
  loc_008C12DE: var_24 = var_20 & var_20
  loc_008C12E5: 00000001h = 00000001h + 00000001h
  loc_008C12EB: GoTo loc_008C1257
  loc_008C12F0: 'Referenced from: 008C125A
  loc_008C12F5: GoTo loc_008C131A
  loc_008C12FB: If var_4 = 0 Then GoTo loc_008C1306
  loc_008C1306: 'Referenced from: 008C12FB
  loc_008C1319: Exit Sub
  loc_008C131A: 'Referenced from: 008C12F5
  loc_008C1323: Exit Sub
End Sub
