VERSION 5.00
Begin VB.Form MsgForm
  ScaleMode = 1
  AutoRedraw = False
  FontTransparent = True
  BorderStyle = 1 'Fixed Single
  Icon = "MsgForm.frx":0
  LinkTopic = "Form1"
  MaxButton = 0   'False
  MinButton = 0   'False
  ControlBox = 0   'False
  ClientLeft = 15
  ClientTop = 15
  ClientWidth = 11835
  ClientHeight = 2925
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
    Caption = "OK"
    Left = 5880
    Top = 2280
    Width = 1215
    Height = 525
    TabIndex = 1
    Picture = "MsgForm.frx":FD55
    ToolTipText = "Close this window"
  End
  Begin ActiveResize ActiveResize1
  End
  Begin Label FunctionNumber
    Caption = "1"
    ForeColor = &H0&
    Left = 11040
    Top = 2400
    Width = 615
    Height = 375
    Visible = 0   'False
    TabIndex = 2
    Alignment = 2 'Center
    ToolTipText = "The function number this form is performing"
  End
  Begin Image Image6
    Index = 0
    Picture = "MsgForm.frx":10533
    Left = 0
    Top = 360
    Width = 2175
    Height = 2130
    Stretch = -1  'True
  End
  Begin Label Label1
    Caption = "-"
    ForeColor = &H0&
    Left = 2280
    Top = 720
    Width = 9255
    Height = 1215
    TabIndex = 0
    Alignment = 2 'Center
  End
End

Attribute VB_Name = "MsgForm"


Private Sub Command1_Click() '8B6D20
  loc_008B6DBA: var_eax = Unknown_VTable_Call[eax+00000050h]
  loc_008B6E2E: var_eax = Unknown_VTable_Call[ecx+00000420h]
  loc_008B6E42: var_eax = Unknown_VTable_Call[eax+000000DCh]
  loc_008B6E8B: var_eax = MsgForm.Hide
  loc_008B6EBB: GoTo loc_008B6EDA
  loc_008B6ED9: Exit Sub
  loc_008B6EDA: 'Referenced from: 008B6EBB
End Sub
