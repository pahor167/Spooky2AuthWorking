VERSION 5.00
Begin VB.Form DatabaseHelp
  Caption = "Database Help"
  ScaleMode = 1
  AutoRedraw = False
  FontTransparent = True
  'Icon = n/a
  LinkTopic = "Form1"
  ClientLeft = 60
  ClientTop = 510
  ClientWidth = 15315
  ClientHeight = 7500
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
  Begin ActiveResize ActiveResize1
  End
  Begin TextBox Text1
    BackColor = &H8000000F&
    ForeColor = &H0&
    Left = 240
    Top = 360
    Width = 15015
    Height = 6975
    Text = "DatabaseHelp.frx":0
    TabIndex = 0
    BorderStyle = 0 'None
    MultiLine = -1  'True
    Locked = -1  'True
  End
End

Attribute VB_Name = "DatabaseHelp"

