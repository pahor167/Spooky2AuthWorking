VERSION 5.00
Begin VB.Form AboutSpooky
  Caption = "About Spooky"
  ScaleMode = 1
  AutoRedraw = False
  FontTransparent = True
  Icon = "AboutSpooky.frx":0
  LinkTopic = "Form2"
  ClientLeft = 60
  ClientTop = 510
  ClientWidth = 17760
  ClientHeight = 11970
  BeginProperty Font
    Name = "Arial"
    Size = 8.25
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
    Left = 2640
    Top = 360
    Width = 14895
    Height = 10815
    Text = "AboutSpooky.frx":FD55
    TabIndex = 0
    BorderStyle = 0 'None
    MultiLine = -1  'True
    ScrollBars = 2
    Locked = -1  'True
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
  Begin Image Image3
    Picture = "AboutSpooky.frx":10233
    Left = 240
    Top = 3600
    Width = 1815
    Height = 2775
    Stretch = -1  'True
    ToolTipText = "New Zealand. Unpolluted paradise"
  End
  Begin Image Image1
    Picture = "AboutSpooky.frx":12E8F
    Left = 240
    Top = 720
    Width = 2055
    Height = 2130
    Stretch = -1  'True
  End
End

Attribute VB_Name = "AboutSpooky"

