VERSION 5.00
Begin VB.Form DebugForm
  Caption = "Debug Form"
  ScaleMode = 1
  AutoRedraw = False
  FontTransparent = True
  Icon = "DebugForm.frx":0
  LinkTopic = "Form2"
  ClientLeft = 60
  ClientTop = 510
  ClientWidth = 21120
  ClientHeight = 10365
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
  Begin TextBox Text1
    Left = 240
    Top = 5160
    Width = 20415
    Height = 4935
    Text = "Text1"
    TabIndex = 4
    MultiLine = -1  'True
  End
  Begin CommandButton Command1
    Caption = "-"
    Left = 20640
    Top = 120
    Width = 495
    Height = 465
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
    ToolTipText = "Clear all the loaded frequencies"
  End
  Begin CommandButton Command6
    Caption = "-"
    Left = 15000
    Top = 120
    Width = 495
    Height = 465
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
    ToolTipText = "Clear all the loaded frequencies"
  End
  Begin ListBox List2
    Left = 15600
    Top = 120
    Width = 5055
    Height = 4860
    TabIndex = 1
  End
  Begin ActiveResize ActiveResize1
  End
  Begin ListBox List1
    Left = 240
    Top = 120
    Width = 14775
    Height = 4860
    TabIndex = 0
  End
End

Attribute VB_Name = "DebugForm"


Private Sub Command6_Click() '8B6BA0
  loc_008B6BFE: var_eax = List1.Clear
  loc_008B6C2D: GoTo loc_008B6C39
  loc_008B6C38: Exit Sub
  loc_008B6C39: 'Referenced from: 008B6C2D
End Sub

Private Sub Command1_Click() '8B6C60
  loc_008B6CBE: var_eax = List2.Clear
  loc_008B6CED: GoTo loc_008B6CF9
  loc_008B6CF8: Exit Sub
  loc_008B6CF9: 'Referenced from: 008B6CED
End Sub
