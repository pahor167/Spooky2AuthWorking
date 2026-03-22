VERSION 5.00
Begin VB.Form ReverseLookup
  Caption = "Reverse Lookup Results"
  ScaleMode = 1
  AutoRedraw = False
  FontTransparent = True
  Icon = "ReverseLookup.frx":0
  LinkTopic = "Form1"
  ClientLeft = 270
  ClientTop = 1875
  ClientWidth = 19185
  ClientHeight = 12195
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
  Begin TextBox Text2
    Left = 1080
    Top = 120
    Width = 615
    Height = 375
    Visible = 0   'False
    TabIndex = 4
    ToolTipText = "Spooky directory"
  End
  Begin RichTextBox RichTextBox2
    Left = 2640
    Top = 1080
    Width = 16335
    Height = 8055
    TabIndex = 3
  End
  Begin CommonDialog CommonDialog1
  End
  Begin TextBox Text1
    Index = 0
    BackColor = &H8000000F&
    ForeColor = &H0&
    Left = 120
    Top = 9360
    Width = 18975
    Height = 2655
    Enabled = 0   'False
    Text = "ReverseLookup.frx":FD55
    TabIndex = 1
    BorderStyle = 0 'None
    MultiLine = -1  'True
    Locked = -1  'True
    ToolTipText = "Friends of Spooky."
  End
  Begin CommandButton Command5
    Left = 18480
    Top = 120
    Width = 495
    Height = 525
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
    Picture = "ReverseLookup.frx":104FB
    ToolTipText = "Save and exit"
    Style = 1
  End
  Begin ActiveResize ActiveResize1
  End
  Begin Label Label1
    Caption = "Header"
    ForeColor = &H0&
    Left = 2640
    Top = 240
    Width = 10335
    Height = 975
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
  End
  Begin Image Image1
    Picture = "ReverseLookup.frx":10D0D
    Left = 240
    Top = 2160
    Width = 2175
    Height = 2130
    Stretch = -1  'True
  End
End

Attribute VB_Name = "ReverseLookup"


Private Sub Text2_KeyPress(KeyAscii As Integer) '8AB7E0
  loc_008AB82F: If KeyAscii <> 1 Then GoTo loc_008AB877
  loc_008AB84D: call __vbaCastObj(var_18, var_18, var_18, Me, var_004A0D48, esi, Me, Set %StkVar1 = %StkVar2 'Ignore this)
  loc_008AB85C: ReverseLookup.Proc_2_3_8AB8C0(Me, __vbaCastObj(var_18, var_18, var_18, Me, var_004A0D48, esi, Me, Set var_1C = __vbaCastObj(var_18, var_18, var_18, Me, var_004A0D48, esi, Me, Set %StkVar1 = %StkVar2)
  loc_008AB877: 'Referenced from: 008AB82F
  loc_008AB87F: GoTo loc_008AB895
  loc_008AB894: Exit Sub
  loc_008AB895: 'Referenced from: 008AB87F
End Sub

Private Sub Command5_Click() '8AACD0
  Dim edx As Variant
  Dim var_80 As Variant
  Dim var_34 As Variant
  Dim Me As CommonDialog
  loc_008AAD6E: var_2C = edx.defTextRTF
  loc_008AAD7F: eax = Len(var_2C) + 1
  loc_008AAD82: var_80 = Len(var_2C) + 1
  loc_008AADA7: If var_80 = 0 Then GoTo loc_008AADAE
  loc_008AADA9: GoTo loc_008AB769
  loc_008AADAE: 'Referenced from: 008AADA7
  loc_008AADB7: On Error Resume Next
  loc_008AADEE: var_2C = Text2.Text
  loc_008AADF6: var_84 = var_2C
  loc_008AAE3B: ecx = var_2C
  loc_008AAED0: var_40 = "\Data" = var_34._Action.Filter & "\Data"
  loc_008AAF1A: var_34 = var_34._Action
  loc_008AAF39: var_60 = "Text (*.txt)|*.txt"
  loc_008AAF87: Me = CommonDialog._Action
  loc_008AAF9D: var_60 = "Reverse_Lookup.txt"
  loc_008AAFEB: edx = CommonDialog._Action
  loc_008AB001: var_60 = "*.txt"
  loc_008AB065: var_60 = "Select Destination"
  loc_008AB0B3: Me = CommonDialog._Action
  loc_008AB0E8: edx = CommonDialog._Action
  loc_008AB127: var_84 = edx._Action
  loc_008AB169: setz cl
  loc_008AB187: If var_88 = 0 Then GoTo loc_008AB1ED
  loc_008AB190: var_60 = vbNullString
  loc_008AB1DE: var_34 = var_34._Action
  loc_008AB1ED: 'Referenced from: 008AB187
  loc_008AB235: ChDrive CStr(Mid(Me, 1, 1))
  loc_008AB265: ChDir %x1 = var_34.Filter
  loc_008AB2AA: var_2C = edx._Action
  loc_008AB2C0: eax = (var_2C = vbNullString) + 1
  loc_008AB2C3: var_80 = (var_2C = vbNullString) + 1
  loc_008AB2E8: If var_80 = 0 Then GoTo loc_008AB2EF
  loc_008AB2EA: GoTo loc_008AB769
  loc_008AB2EF: 'Referenced from: 008AB2E8
  loc_008AB311: var_28 = FreeFile(10)
  loc_008AB347: var_48 = Me._Action
  loc_008AB371: Open Me For Output As #var_28 Len = -1
  loc_008AB3ED: Print var_28, var_34.defTextRTF
  loc_008AB422: Close #var_28
  loc_008AB466: var_80 = var_A8
  loc_008AB477: MsgForm.Caption = "Report created"
  loc_008AB47C: var_84 = var_80
  loc_008AB535: var_48 = edx._Action
  loc_008AB561: var_30 = var_48 & " written"
  loc_008AB576: var_84 = var_48.Flags
  loc_008AB68A: var_eax = MsgForm.Show var_5C
  loc_008AB692: var_84 = MsgForm.Show var_5C
  loc_008AB725: var_eax = ReverseLookup.Hide
  loc_008AB72D: var_84 = ReverseLookup.Hide
  loc_008AB775: GoTo loc_008AB7B1
  loc_008AB7B0: Exit Sub
  loc_008AB7B1: 'Referenced from: 008AB775
End Sub

Private Sub Form_Load() '8AABB0
  loc_008AAC3D: var_1C = "PLEASE NOTE:  This is NOT a diagnostic tool. Many health issues share the same frequencies." & "vbCrLf" & "You do not necessarily have the conditions listed."
  loc_008AAC45: var_eax = Unknown_VTable_Call[ebx+00000054h]
  loc_008AAC85: GoTo loc_008AACA4
  loc_008AACA3: Exit Sub
  loc_008AACA4: 'Referenced from: 008AAC85
End Sub

Public Sub Proc_2_3_8AB8C0
  Dim var_14 As Me
  loc_008AB8F9: Set var_14 = arg_C
  loc_008AB93B: var_18 = var_14.MousePointer
  loc_008AB96C: var_14.FontTransparent = Len(var_18)
  loc_008AB997: GoTo loc_008AB9A3
  loc_008AB9A2: Exit Sub
  loc_008AB9A3: 'Referenced from: 008AB997
End Sub
