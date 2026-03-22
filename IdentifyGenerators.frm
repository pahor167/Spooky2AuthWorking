VERSION 5.00
Begin VB.Form IdentifyGenerators
  Caption = "Loaded Programs"
  ScaleMode = 1
  AutoRedraw = False
  FontTransparent = True
  BorderStyle = 1 'Fixed Single
  Icon = "IdentifyGenerators.frx":0
  LinkTopic = "Form1"
  MaxButton = 0   'False
  MinButton = 0   'False
  ControlBox = 0   'False
  ClientLeft = 6585
  ClientTop = 1650
  ClientWidth = 16590
  ClientHeight = 10785
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
  Begin CommandButton Command1
    Caption = "X"
    BackColor = &HC000&
    Left = 15960
    Top = 120
    Width = 495
    Height = 525
    TabIndex = 2
    ToolTipText = "Close this window"
  End
  Begin TextBox Text1
    ForeColor = &H0&
    Left = 120
    Top = 3000
    Width = 16335
    Height = 7575
    Text = "Text1"
    TabIndex = 1
    MultiLine = -1  'True
    ScrollBars = 3
  End
  Begin ActiveResize ActiveResize1
  End
  Begin Label FunctionNumber
    Caption = "1"
    Left = 2280
    Top = 240
    Width = 615
    Height = 375
    Visible = 0   'False
    TabIndex = 3
  End
  Begin Image Image1
    Picture = "IdentifyGenerators.frx":FD55
    Left = 120
    Top = 120
    Width = 2175
    Height = 2130
    Stretch = -1  'True
  End
  Begin Label Label1
    Caption = "Generator Status"
    BackColor = &HFFFFC0&
    ForeColor = &H0&
    Left = 120
    Top = 2640
    Width = 16335
    Height = 375
    TabIndex = 0
    Alignment = 2 'Center
    BackStyle = 0 'Transparent
  End
End

Attribute VB_Name = "IdentifyGenerators"


Private Sub Text1_KeyPress(KeyAscii As Integer) '8ABC10
  loc_008ABC5C: If KeyAscii <> 1 Then GoTo loc_008ABC83
  loc_008ABC74: var_eax = IdentifyGenerators.Proc_3_2_8ABCC0(Me, var_18, var_18, Me)
  loc_008ABC83: 'Referenced from: 008ABC5C
  loc_008ABC8B: GoTo loc_008ABC97
  loc_008ABC96: Exit Sub
  loc_008ABC97: 'Referenced from: 008ABC8B
End Sub

Private Sub Command1_Click() '8AB9D0
  loc_008ABABC: var_eax = Unknown_VTable_Call[ecx+00000054h]
  loc_008ABB13: var_eax = Unknown_VTable_Call[ecx+00000424h]
  loc_008ABB2F: 1 = FunctionNumber.FormatLength
  loc_008ABB98: var_eax = IdentifyGenerators.Hide
  loc_008ABBC4: GoTo loc_008ABBE7
  loc_008ABBE6: Exit Sub
  loc_008ABBE7: 'Referenced from: 008ABBC4
End Sub

Public Sub Proc_3_2_8ABCC0
  Dim var_14 As Me
  loc_008ABCF9: Set var_14 = arg_C
  loc_008ABD3B: var_18 = var_14.MousePointer
  loc_008ABD6C: var_14.FontTransparent = Len(var_18)
  loc_008ABD97: GoTo loc_008ABDA3
  loc_008ABDA2: Exit Sub
  loc_008ABDA3: 'Referenced from: 008ABD97
End Sub
