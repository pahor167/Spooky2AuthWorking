VERSION 5.00
Begin VB.Form Splash
  ScaleMode = 1
  AutoRedraw = False
  FontTransparent = True
  BorderStyle = 1 'Fixed Single
  'Icon = n/a
  LinkTopic = "Form1"
  MaxButton = 0   'False
  MinButton = 0   'False
  ControlBox = 0   'False
  ClientLeft = 10890
  ClientTop = 15
  ClientWidth = 11745
  ClientHeight = 2805
  BeginProperty Font
    Name = "Arial"
    Size = 8.25
    Charset = 0
    Weight = 400
    Underline = 0 'False
    Italic = 0 'False
    Strikethrough = 0 'False
  EndProperty
  StartUpPosition = 1 'CenterOwner
  Begin ActiveResize ActiveResize1
  End
  Begin Label label1
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
  Begin Label label1
    Caption = "-"
    Index = 0
    ForeColor = &H0&
    Left = 2400
    Top = 600
    Width = 9135
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
  Begin Image Image6
    Index = 1
    Picture = "Splash.frx":0
    Left = 0
    Top = 360
    Width = 2175
    Height = 2130
    Stretch = -1  'True
  End
End

Attribute VB_Name = "Splash"


Private Sub Image6_Click() '8B6F00
  loc_008B6F69: var_eax = Unknown_VTable_Call[ecx+00000320h]
  loc_008B6FE3: GoTo loc_008B6FF9
  loc_008B6FF8: Exit Sub
  loc_008B6FF9: 'Referenced from: 008B6FE3
End Sub
