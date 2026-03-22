VERSION 5.00
Begin VB.Form SplashProgress
  ForeColor = &H0&
  ScaleMode = 1
  AutoRedraw = False
  FontTransparent = False
  BorderStyle = 1 'Fixed Single
  Icon = "SplashProgress.frx":0
  LinkTopic = "Form2"
  MaxButton = 0   'False
  MinButton = 0   'False
  ControlBox = 0   'False
  ClientLeft = 8370
  ClientTop = 5385
  ClientWidth = 11745
  ClientHeight = 2805
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
  Begin ProgressBar ProgressBar1
    Left = 1800
    Top = 2400
    Width = 9495
    Height = 255
    Visible = 0   'False
    TabIndex = 2
  End
  Begin ActiveResize ActiveResize1
  End
  Begin Image Image6
    Index = 1
    Picture = "SplashProgress.frx":FD55
    Left = 0
    Top = 360
    Width = 2175
    Height = 2130
    Stretch = -1  'True
  End
  Begin Label Label1
    Caption = "-"
    Index = 1
    ForeColor = &H0&
    Left = 2400
    Top = 1560
    Width = 9135
    Height = 615
    TabIndex = 1
    Alignment = 2 'Center
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
    Caption = "-"
    Index = 0
    ForeColor = &H0&
    Left = 2520
    Top = 600
    Width = 9015
    Height = 615
    TabIndex = 0
    Alignment = 2 'Center
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

Attribute VB_Name = "SplashProgress"


Private Sub Image6_Click() '8AF4B0
  loc_008AF519: var_eax = Unknown_VTable_Call[ecx+00000320h]
  loc_008AF593: GoTo loc_008AF5A9
  loc_008AF5A8: Exit Sub
  loc_008AF5A9: 'Referenced from: 008AF593
End Sub
