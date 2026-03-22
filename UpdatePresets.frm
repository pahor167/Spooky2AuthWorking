VERSION 5.00
Begin VB.Form UpdatePresets
  Caption = "Updating Presets"
  ScaleMode = 1
  AutoRedraw = False
  FontTransparent = True
  'Icon = n/a
  LinkTopic = "Form1"
  ClientLeft = 60
  ClientTop = 510
  ClientWidth = 14175
  ClientHeight = 3600
  StartUpPosition = 2 'CenterScreen
  Begin CommandButton Command2
    Left = 480
    Top = 1440
    Width = 375
    Height = 375
    Visible = 0   'False
    TabIndex = 7
    ToolTipText = "Hidden ctontrol to extract preset frequencies"
  End
  Begin TextBox Text2
    Left = 3600
    Top = 960
    Width = 615
    Height = 285
    Visible = 0   'False
    Text = "Text2"
    TabIndex = 6
  End
  Begin TextBox Text1
    Left = 2760
    Top = 960
    Width = 615
    Height = 285
    Visible = 0   'False
    Text = "Text1"
    TabIndex = 5
    ToolTipText = "Spooky directory"
  End
  Begin CommandButton Command1
    Left = 0
    Top = 1440
    Width = 375
    Height = 375
    Visible = 0   'False
    TabIndex = 3
    ToolTipText = "Hidden control to update presets"
  End
  Begin DirListBox Dir1
    Left = 0
    Top = 600
    Width = 2175
    Height = 765
    Visible = 0   'False
    TabIndex = 1
  End
  Begin FileListBox File1
    Left = 0
    Top = 0
    Width = 2535
    Height = 675
    Visible = 0   'False
    TabIndex = 0
  End
  Begin Label Label2
    Left = 3120
    Top = 0
    Width = 9135
    Height = 375
    Visible = 0   'False
    TabIndex = 4
    ToolTipText = "Pathname of preset to upgrade"
  End
  Begin Label Label1
    ForeColor = &H0&
    Left = 480
    Top = 240
    Width = 13335
    Height = 735
    TabIndex = 2
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
  End
End

Attribute VB_Name = "UpdatePresets"


Private Sub Command2_Click() '8C5810
  loc_008C5875: On Error Resume Next
  loc_008C58B5: var_5C = Text1.Text
  loc_008C58BD: var_24C = var_5C
  loc_008C5905: ecx = var_5C
  loc_008C5957: var_5C = Text2.Text
  loc_008C595F: var_24C = var_5C
  loc_008C59A7: ecx = var_5C
  loc_008C59DD: var_38 = ecx+00000040h & "\Data\PresetPrograms.csv"
  loc_008C5A00: ReDim Me(0 To 0)
  loc_008C5A23: ReDim var_24(0 To 0)
  loc_008C5A4F: var_eax = UpdatePresets.Proc_22_4_8C6D40(Me, "*.txt")
End Sub

Private Sub Command1_Click() '8C5290
  Dim var_B0 As Variant
  loc_008C52F5: On Error Resume Next
  loc_008C5335: var_28 = Text1.Text
  loc_008C533D: var_B4 = var_28
  loc_008C5385: ecx = var_28
  loc_008C53BE: var_B0 = var_2C
  loc_008C53D7: var_28 = Text2.Text
  loc_008C53DF: var_B4 = var_28
  loc_008C5427: ecx = var_28
  loc_008C544F: var_eax = UpdatePresets.Proc_22_7_8C8D70(Me, var_2C, Me, Me, var_2C, edx)
  loc_008C548F: var_eax = Unknown_VTable_Call[ecx+00000050h]
  loc_008C5494: var_B4 = Unknown_VTable_Call[ecx+00000050h]
  loc_008C54E9: var_24 = var_28
  loc_008C5519: var_B0 = var_2C
  loc_008C5538: var_B4 = var_B0
  loc_008C558F: ecx = vbNullString
  loc_008C55AD: If (var_24 <> vbNullString) <> 0 Then GoTo loc_008C55E0
  loc_008C55BF: var_eax = UpdatePresets.Proc_22_8_8D5EF0(Me, var_B0, vbNullString, var_2C, Me, Me, var_B0)
  loc_008C55D5: var_eax = UpdatePresets.Proc_22_2_8C6A10(Me, var_28, var_2C, Me)
  loc_008C55DB: GoTo loc_008C56FA
  loc_008C55E0: 'Referenced from: 008C55AD
  loc_008C55EA: var_74 = var_24
  loc_008C561F: var_B0 = (Dir(var_24, 0) = vbNullString)
  loc_008C5638: If var_B0 = 0 Then GoTo loc_008C56FA
  loc_008C5652: var_eax = UpdatePresets.Proc_22_5_8C7720(Me, var_24, Me)
  loc_008C56D2: var_ret_1 = MsgBox("Preset file updated. Please select again", 0, "Preset Updated", var_5C, 10)
  loc_008C56FA: 'Referenced from: 008C55DB
  loc_008C5703: On Error Resume Next
  loc_008C575C: var_eax = UpdatePresets.Hide
  loc_008C5764: var_B4 = UpdatePresets.Hide
  loc_008C57A3: Exit Sub
  loc_008C57AE: GoTo loc_008C57DE
  loc_008C57DD: Exit Sub
  loc_008C57DE: 'Referenced from: 008C57AE
End Sub

Public Sub Proc_22_2_8C6A10
  Dim var_28 As Me
  loc_008C6A49: On Error Resume Next
  loc_008C6A5D: var_2C = "SystemCFG.txt"
  loc_008C6A89: var_eax = UpdatePresets.Proc_22_3_8C6BD0(Me, Set Me = 00000001h(var_28 = %S_edx_S) & "\Data")
  loc_008C6AAA: var_2C = "SettingsCFG.txt"
  loc_008C6ACD: var_eax = UpdatePresets.Proc_22_3_8C6BD0(Me, Set  = 00000001h() & "\Data")
  loc_008C6AEE: var_2C = "BFBCFG.txt"
  loc_008C6B11: var_eax = UpdatePresets.Proc_22_3_8C6BD0(Me, Set fs:[00000000h] = 00000001h() & "\Data")
  loc_008C6B32: var_2C = "CH*.txt"
  loc_008C6B55: var_eax = UpdatePresets.Proc_22_3_8C6BD0(Me, Set  = 00000001h() & "\Data")
  loc_008C6B76: var_28 = "*.txt"
  loc_008C6B83: var_eax = UpdatePresets.Proc_22_3_8C6BD0(Me)
  loc_008C6B92: var_eax = Close
  loc_008C6B98: Exit Sub
  loc_008C6B9E: Method_8964E44D
  loc_008C6BA3: GoTo loc_008C6BB9
  loc_008C6BB8: Exit Sub
  loc_008C6BB9: 'Referenced from: 008C6BA3
End Sub

Public Sub Proc_22_3_8C6BD0
  Dim var_1C As Me
  loc_008C6C0A: var_28 = arg_C
  loc_008C6C1F: var_20 = Dir(arg_C, 16)
  loc_008C6C37: esi = (var_20 = vbNullString) + 1
  loc_008C6C46: If (var_20 <> vbNullString) + 1 <> 0 Then GoTo loc_008C6D05
  loc_008C6C62: ReDim 00000008h.Reset(arg_C To 0)
  loc_008C6C78: var_eax = UpdatePresets.Proc_22_4_8C6D40(Me, eax)
  loc_008C6C83: call UBound(00000001h, 00000008h.Reset, arg_C, 00000008h.Reset)
  loc_008C6C8D: var_18 = UBound(00000001h, 00000008h.Reset, arg_C, 00000008h.Reset)
  loc_008C6C90: If UBound(00000001h, 00000008h.Reset, arg_C, 00000008h.Reset) = 0 Then GoTo loc_008C6D05
  loc_008C6C96: If edi > 0 Then GoTo loc_008C6D05
  loc_008C6CAB: If var_1C = 0 Then GoTo loc_008C6CD6
  loc_008C6CB1: If var_1C <> 1 Then GoTo loc_008C6CD6
  loc_008C6CBB: edi = edi - 00000008h.%x1 = GetIDsOfNames(%StkVar2) 'Ignore this
  loc_008C6CBF: If edi < 0 Then GoTo loc_008C6CCA
  loc_008C6CC1: var_eax = Err.Raise
  loc_008C6CCA: 'Referenced from: 008C6CBF
  loc_008C6CD4: GoTo loc_008C6CDF
  loc_008C6CD6: 'Referenced from: 008C6CAB
  loc_008C6CD6: var_eax = Err.Raise
  loc_008C6CDF: 'Referenced from: 008C6CD4
  loc_008C6CE8: var_eax = UpdatePresets.Proc_22_5_8C7720(Me)
  loc_008C6CFD: 00000001h = 00000001h + edi-00000008h.%x1 = GetIDsOfNames(%StkVar2) 'Ignore this
  loc_008C6D03: GoTo loc_008C6C94
  loc_008C6D05: 'Referenced from: 008C6C46
  loc_008C6D0A: GoTo loc_008C6D20
  loc_008C6D1F: Exit Sub
  loc_008C6D20: 'Referenced from: 008C6D0A
  loc_008C6D20: Exit Sub
End Sub

Public Sub Proc_22_4_8C6D40
  Dim var_AC As DirListBox
  Dim var_40 As Variant
  Dim var_34 As DirListBox
  Dim var_30 As FileListBox
  loc_008C6DB9: var_20 = arg_C
  loc_008C6DC1: var_1C = arg_10
  loc_008C6DFF: If Me = 0 Then GoTo loc_008C6E77
  loc_008C6E19: var_AC = (Right$(var_1C, 1) = var_004A1644)
  loc_008C6E33: var_34 = var_1C & var_004A1644
  loc_008C6E43: Dir1.Path = var_34
  loc_008C6E75: GoTo loc_008C6EC1
  loc_008C6E77: 'Referenced from: 008C6DFF
  loc_008C6E94: Dir1.Path = var_1C
  loc_008C6EC1: 'Referenced from: 008C6E75
  loc_008C6EE1: var_A8 = Dir1.ListCount
  loc_008C6F0E: var_C0 = var_A8
  loc_008C6F1D: 
  loc_008C6F28: If var_18 > 0 Then GoTo loc_008C7695
  loc_008C6F4B: var_34 = Dir1.List(var_18)
  loc_008C6F97: If (var_34 = vbNullString) = 0 Then GoTo loc_008C7006
  loc_008C6FBC: var_34 = Dir1.List(var_18)
  loc_008C6FE9: var_eax = UpdatePresets.Proc_22_4_8C6D40(Me, var_20, var_34, arg_14, var_40, DoEvents, Me, var_40)
  loc_008C7001: var_eax = Unknown_8280(var_A8, Me, var_40, esi)
  loc_008C7006: 'Referenced from: 008C6F97
  loc_008C701F: var_34 = Dir1.Path
  loc_008C704E: var_38 = Right$(var_34, 1)
  loc_008C706E: edi = (var_38 = var_004A1644) + 1
  loc_008C7088: If (var_38 = var_004A1644) + 1 = 0 Then GoTo loc_008C7175
  loc_008C70A7: var_34 = Dir1.Path
  loc_008C70DE: var_38 = Dir1.Path
  loc_008C70FF: var_4C = var_34
  loc_008C711A: Len(var_38) = Len(var_38) - 00000001h
  loc_008C7141: var_30 = Left(0, Len(var_38))
  loc_008C7173: GoTo loc_008C71C8
  loc_008C7175: 'Referenced from: 008C7088
  loc_008C718E: var_34 = Dir1.Path
  loc_008C71B9: var_30 = var_34
  loc_008C71C8: 'Referenced from: 008C7173
  loc_008C71E1: File1.Path = var_30
  loc_008C7221: File1.Pattern = var_20
  loc_008C7264: var_A8 = File1.ListCount
  loc_008C728E: setg al
  loc_008C729F: If eax = 0 Then GoTo loc_008C7458
  loc_008C72C1: var_A8 = File1.ListCount
  loc_008C72E6: var_A8 = var_A8 - 0001h
  loc_008C72F5: var_C8 = var_A8
  loc_008C7304: If ebx > var_A8 Then GoTo loc_008C7452
  loc_008C7314: call UBound(00000001h, arg_14, 00000000h, var_40, var_A8, Me, var_40, var_30, Me, var_40, var_30, Me, var_40, Me, Me)
  loc_008C731A: UBound(00000001h, arg_14, 00000000h, var_40, var_A8, Me, var_40, var_30, Me, var_40, var_30, Me, var_40, Me, Me) = UBound(00000001h, arg_14, 00000000h, var_40, var_A8, Me, var_40, var_30, Me, var_40, var_30, Me, var_40, Me, Me) + 00000001h
  loc_008C7330: ReDim Preserve arg_14( To UBound(00000001h, arg_14, 00000000h, var_40, var_A8, Me, var_40, var_30, Me, var_40, var_30, Me, var_40, Me, Me))
  loc_008C7357: var_34 = File1.List(0)
  loc_008C737C: If arg_14 = 0 Then GoTo loc_008C73C5
  loc_008C7382: If ecx <> 1 Then GoTo loc_008C73C5
  loc_008C7387: var_E4 = arg_14
  loc_008C738D: call UBound(00000001h, arg_14, var_40, Me, Me)
  loc_008C73A1: UBound(00000001h, arg_14, var_40, Me, Me) = UBound(00000001h, arg_14, var_40, Me, Me) - 00000001h
  loc_008C73AA: UBound(00000001h, arg_14, var_40, Me, Me) = UBound(00000001h, arg_14, var_40, Me, Me) - eax+00000014h
  loc_008C73AE: If UBound(00000001h, arg_14, var_40, Me, Me) < 0 Then GoTo loc_008C73B6
  loc_008C73B0: var_eax = Err.Raise
  loc_008C73B6: 'Referenced from: 008C73AE
  loc_008C73BD: var_E8 = edi*0/
  loc_008C73C3: GoTo loc_008C73D1
  loc_008C73C5: 'Referenced from: 008C737C
  loc_008C73C5: var_eax = Err.Raise
  loc_008C73CB: var_E8 = Err.Raise
  loc_008C73D1: 'Referenced from: 008C73C3
  loc_008C740F: eax+0000000Ch = eax+0000000Ch + var_E8
  loc_008C7411: ecx = var_30 & var_004A1644 & var_34
  loc_008C7442: 00000001h = 00000001h + ebx
  loc_008C744D: GoTo loc_008C7301
  loc_008C7452: 'Referenced from: 008C7304
  loc_008C7458: 'Referenced from: 008C729F
  loc_008C7471: var_34 = Dir1.Path
  loc_008C74A4: var_14 = Len(var_34)
  loc_008C74B6: 
  loc_008C74CF: var_34 = Dir1.Path
  loc_008C74FE: var_4C = var_34
  loc_008C7526: var_74 = Mid(0, Len(0), 1)
  loc_008C7573: If (var_74 = &H4A1644) = 0 Then GoTo loc_008C7591
  loc_008C757F: var_14 = var_14 - 0001h
  loc_008C7589: var_14 = var_14
  loc_008C758C: GoTo loc_008C74B6
  loc_008C7591: 'Referenced from: 008C7573
  loc_008C75AE: var_34 = Dir1.Path
  loc_008C75E5: var_4C = var_34
  loc_008C75F7: var_8C = var_14
  loc_008C7626: var_38 = CStr(1)
  loc_008C762E: Dir1.Path = var_38
  loc_008C7683: 00000001h = 00000001h + var_18
  loc_008C7690: GoTo loc_008C6F1D
  loc_008C7695: 'Referenced from: 008C6F28
  loc_008C769A: GoTo loc_008C76DF
  loc_008C76DE: Exit Sub
  loc_008C76DF: 'Referenced from: 008C769A
  loc_008C76F9: Exit Sub
End Sub

Public Sub Proc_22_5_8C7720
  loc_008C77D8: If (arg_C = vbNullString) = 0 Then GoTo loc_008C8A14
  loc_008C77E6: var_F8 = arg_C
  loc_008C7801: var_5C = Dir(, 0)
  loc_008C7815: esi = (var_5C = vbNullString) + 1
  loc_008C7824: If (var_5C <> vbNullString) + 1 <> 0 Then GoTo loc_008C8A14
  loc_008C785C: var_5C = "Updating " & arg_C
  loc_008C7864: var_eax = Unknown_VTable_Call[ebx+00000054h]
  loc_008C78CA: Open arg_C For Input As #FreeFile(var_70) Len = -1
  loc_008C78DA: 
  loc_008C78DC: var_ret_2 = FreeFile(var_70)
  loc_008C78E8: If EOF(var_ret_2) <> 0 Then GoTo loc_008C7966
  loc_008C78EC: var_30 = var_30 + 00000001h
  loc_008C7905: var_30 = var_30
  loc_008C7908: ReDim Preserve var_24(0 To var_30)
  loc_008C7916: If var_24 = 0 Then GoTo loc_008C793F
  loc_008C791C: If var_24 <> 1 Then GoTo loc_008C793F
  loc_008C7924: var_30 = var_30 - ecx+00000014h
  loc_008C7928: If var_30 < 0 Then GoTo loc_008C7933
  loc_008C792A: var_eax = Err.Raise
  loc_008C7933: 'Referenced from: 008C7928
  loc_008C793D: GoTo loc_008C7948
  loc_008C793F: 'Referenced from: 008C7916
  loc_008C793F: var_eax = Err.Raise
  loc_008C7948: 'Referenced from: 008C793D
  loc_008C794B: ecx+0000000Ch = ecx+0000000Ch + Err.Raise
  loc_008C7958: Input FreeFile(var_70), ecx+0000000Ch
  loc_008C7961: GoTo loc_008C78DA
  loc_008C7966: 'Referenced from: 008C78E8
  loc_008C796B: Close #FreeFile(var_70)
  loc_008C797A: call UBound(00000002h, %x1 = Label1.Font)
  loc_008C7990: var_3C = UBound(00000002h, %x1 = Label1.Font)
  loc_008C7995: If var_20 > 0 Then GoTo loc_008C88FB
  loc_008C79A0: If var_24 = 0 Then GoTo loc_008C79C8
  loc_008C79A6: If var_24 <> 1 Then GoTo loc_008C79C8
  loc_008C79B4: var_20 = var_20 - ecx+00000014h
  loc_008C79B8: If var_20 < 0 Then GoTo loc_008C79BF
  loc_008C79BA: var_eax = Err.Raise
  loc_008C79BF: 'Referenced from: 008C79B8
  loc_008C79C6: GoTo loc_008C79D7
  loc_008C79C8: 'Referenced from: 008C79A0
  loc_008C79C8: var_eax = Err.Raise
  loc_008C79D7: 'Referenced from: 008C79C6
  loc_008C79E7: If Len(edx+eax) <> 3 Then GoTo loc_008C7B9F
  loc_008C7A0E: If var_50 = 0 Then GoTo loc_008C7A33
  loc_008C7A14: If var_50 <> 1 Then GoTo loc_008C7A33
  loc_008C7A1F: var_20 = var_20 - ecx+00000014h
  loc_008C7A23: If var_20 < 0 Then GoTo loc_008C7A2A
  loc_008C7A25: var_eax = Err.Raise
  loc_008C7A2A: 'Referenced from: 008C7A23
  loc_008C7A31: GoTo loc_008C7A38
  loc_008C7A33: 'Referenced from: 008C7A0E
  loc_008C7A33: var_eax = Err.Raise
  loc_008C7A38: 'Referenced from: 008C7A31
  loc_008C7A3F: ecx+0000000Ch = ecx+0000000Ch + Err.Raise
  loc_008C7A49: var_F8 = ecx+0000000Ch
  loc_008C7A5E: var_80 = Mid(, 1, 1)
  loc_008C7A75: var_90 = Chr(34)
  loc_008C7AA2: If var_54 = 0 Then GoTo loc_008C7ACB
  loc_008C7AA8: If var_54 <> 1 Then GoTo loc_008C7ACB
  loc_008C7AB3: var_20 = var_20 - ecx+00000014h
  loc_008C7AB7: If var_20 < 0 Then GoTo loc_008C7AC2
  loc_008C7AB9: var_eax = Err.Raise
  loc_008C7AC2: 'Referenced from: 008C7AB7
  loc_008C7AC9: GoTo loc_008C7AD4
  loc_008C7ACB: 'Referenced from: 008C7AA2
  loc_008C7ACB: var_eax = Err.Raise
  loc_008C7AD4: 'Referenced from: 008C7AC9
  loc_008C7ADE: ecx+0000000Ch = ecx+0000000Ch + Err.Raise
  loc_008C7AE8: var_118 = ecx+0000000Ch
  loc_008C7B51: var_ret_7 = (var_80 = var_90) And (Mid(ecx+0000000Ch, 3, 1) = Chr(34))
  loc_008C7B93: If CBool(var_ret_7) <> 0 Then GoTo loc_008C888A
  loc_008C7B9F: 'Referenced from: 008C79E7
  loc_008C7BA4: If var_24 = 0 Then GoTo loc_008C7BC9
  loc_008C7BAA: If var_24 <> 1 Then GoTo loc_008C7BC9
  loc_008C7BB5: var_20 = var_20 - ecx+00000014h
  loc_008C7BB9: If var_20 < 0 Then GoTo loc_008C7BC0
  loc_008C7BBB: var_eax = Err.Raise
  loc_008C7BC0: 'Referenced from: 008C7BB9
  loc_008C7BC7: GoTo loc_008C7BCE
  loc_008C7BC9: 'Referenced from: 008C7BA4
  loc_008C7BC9: var_eax = Err.Raise
  loc_008C7BCE: 'Referenced from: 008C7BC7
  loc_008C7BEB: If InStr(1, edx+eax, "Text22(2)<>" > 0 Then GoTo loc_008C7DD6
  loc_008C7BF3: If var_24 = 0 Then GoTo loc_008C7C18
  loc_008C7BF9: If var_24 <> 1 Then GoTo loc_008C7C18
  loc_008C7C04: var_20 = var_20 - ecx+00000014h
  loc_008C7C08: If var_20 < 0 Then GoTo loc_008C7C0F
  loc_008C7C0A: var_eax = Err.Raise
  loc_008C7C0F: 'Referenced from: 008C7C08
  loc_008C7C16: GoTo loc_008C7C1D
  loc_008C7C18: 'Referenced from: 008C7BF3
  loc_008C7C18: var_eax = Err.Raise
  loc_008C7C1D: 'Referenced from: 008C7C16
  loc_008C7C52: If var_50 = 0 Then GoTo loc_008C7C7B
  loc_008C7C58: If var_50 <> 1 Then GoTo loc_008C7C7B
  loc_008C7C63: var_20 = var_20 - ecx+00000014h
  loc_008C7C67: If var_20 < 0 Then GoTo loc_008C7C72
  loc_008C7C69: var_eax = Err.Raise
  loc_008C7C72: 'Referenced from: 008C7C67
  loc_008C7C79: GoTo loc_008C7C80
  loc_008C7C7B: 'Referenced from: 008C7C52
  loc_008C7C7B: call var_20(var_50, var_24, var_54, var_54, var_24, var_50, var_50, var_24)
  loc_008C7C80: 'Referenced from: 008C7C79
  loc_008C7C83: ecx+0000000Ch = ecx+0000000Ch + var_20(var_50, var_24, var_54, var_54, var_24, var_50, var_50, var_24)
  loc_008C7C89: InStr(1, ecx+eax, var_004A1940, 0) = InStr(1, ecx+eax, var_004A1940, 0) + 00000001h
  loc_008C7C99: var_F8 = ecx+0000000Ch
  loc_008C7CC6: var_5C = CStr(Mid(, InStr(1, ecx+eax, var_004A1940, 0), var_70))
  loc_008C7D11: var_108 = "Out2_Amplitude="
  loc_008C7D3D: var_90 = Trim(Str((var_2C * var_18)))
  loc_008C7D48: If var_24 = 0 Then GoTo loc_008C7D6A
  loc_008C7D4E: If var_24 <> 1 Then GoTo loc_008C7D6A
  loc_008C7D59: var_20 = var_20 - eax+00000014h
  loc_008C7D5D: If var_20 < 0 Then GoTo loc_008C7D65
  loc_008C7D5F: var_eax = Err.Raise
  loc_008C7D65: 'Referenced from: 008C7D5D
  loc_008C7D68: GoTo loc_008C7D72
  loc_008C7D6A: 'Referenced from: 008C7D48
  loc_008C7D6A: var_eax = Err.Raise
  loc_008C7D72: 'Referenced from: 008C7D68
  loc_008C7DA7: eax+0000000Ch = eax+0000000Ch + Err.Raise
  loc_008C7DA9: ecx = "Out2_Amplitude=" & var_90
  loc_008C7DD1: GoTo loc_008C88D1
  loc_008C7DD6: 'Referenced from: 008C7BEB
  loc_008C7DD8: If var_70 = 0 Then GoTo loc_008C7DFD
  loc_008C7DDE: If var_70 <> 1 Then GoTo loc_008C7DFD
  loc_008C7DE9: var_20 = var_20 - ecx+00000014h
  loc_008C7DED: If var_20 < 0 Then GoTo loc_008C7DF4
  loc_008C7DF4: 'Referenced from: 008C7DED
  loc_008C7DFB: GoTo loc_008C7E02
  loc_008C7E02: 'Referenced from: 008C7DFB
  loc_008C7E16: If var_278 <= 0 Then GoTo loc_008C7FCC
  loc_008C7E2F: If var_24 = 0 Then GoTo loc_008C7E54
  loc_008C7E35: If var_24 <> 1 Then GoTo loc_008C7E54
  loc_008C7E40: var_20 = var_20 - eax+00000014h
  loc_008C7E44: If var_20 < 0 Then GoTo loc_008C7E4B
  loc_008C7E4B: 'Referenced from: 008C7E44
  loc_008C7E52: GoTo loc_008C7E5F
  loc_008C7E54: 'Referenced from: 008C7E2F
  loc_008C7E54: var_eax = Err.Raise
  loc_008C7E5F: 'Referenced from: 008C7E52
  loc_008C7E6F: If var_50 = 0 Then GoTo loc_008C7E98
  loc_008C7E75: If var_50 <> 1 Then GoTo loc_008C7E98
  loc_008C7E80: var_20 = var_20 - ecx+00000014h
  loc_008C7E84: If var_20 < 0 Then GoTo loc_008C7E8F
  loc_008C7E86: var_eax = Err.Raise
  loc_008C7E8F: 'Referenced from: 008C7E84
  loc_008C7E96: GoTo loc_008C7EA1
  loc_008C7E98: 'Referenced from: 008C7E6F
  loc_008C7E98: var_eax = Err.Raise
  loc_008C7EA1: 'Referenced from: 008C7E96
  loc_008C7EA7: ecx+0000000Ch = ecx+0000000Ch + Err.Raise
  loc_008C7EA9: var_F8 = ecx+0000000Ch
  loc_008C7ED6: var_80 = Mid(, Len(edx+edi), 1)
  loc_008C7F24: If (var_80 = &H4A187C) = 0 Then GoTo loc_008C88D8
  loc_008C7F2F: If var_24 = 0 Then GoTo loc_008C7F5A
  loc_008C7F35: If var_24 <> 1 Then GoTo loc_008C7F5A
  loc_008C7F46: var_20 = var_20 - eax+00000014h
  loc_008C7F4A: If var_20 < 0 Then GoTo loc_008C7F51
  loc_008C7F4C: var_eax = Err.Raise
  loc_008C7F51: 'Referenced from: 008C7F4A
  loc_008C7F58: GoTo loc_008C7F67
  loc_008C7F5A: 'Referenced from: 008C7F2F
  loc_008C7F60: var_eax = Err.Raise
  loc_008C7F67: 'Referenced from: 008C7F58
  loc_008C7F69: If var_24 = 0 Then GoTo loc_008C7F8A
  loc_008C7F6F: If var_24 <> 1 Then GoTo loc_008C7F8A
  loc_008C7F7A: var_20 = var_20 - eax+00000014h
  loc_008C7F7E: If var_20 < 0 Then GoTo loc_008C7F85
  loc_008C7F80: var_eax = Err.Raise
  loc_008C7F85: 'Referenced from: 008C7F7E
  loc_008C7F88: GoTo loc_008C7F91
  loc_008C7F8A: 'Referenced from: 008C7F69
  loc_008C7F8A: var_eax = Err.Raise
  loc_008C7F91: 'Referenced from: 008C7F88
  loc_008C7FB6: ecx+0000000Ch = ecx+0000000Ch + Err.Raise
  loc_008C7FB8: ecx = edx+edi & var_004A187C
  loc_008C7FC7: GoTo loc_008C88D1
  loc_008C7FCC: 'Referenced from: 008C7E16
  loc_008C7FD1: If var_24 = 0 Then GoTo loc_008C7FF6
  loc_008C7FD7: If var_24 <> 1 Then GoTo loc_008C7FF6
  loc_008C7FE2: var_20 = var_20 - ecx+00000014h
  loc_008C7FE6: If var_20 < 0 Then GoTo loc_008C7FED
  loc_008C7FE8: var_eax = Err.Raise
  loc_008C7FED: 'Referenced from: 008C7FE6
  loc_008C7FF4: GoTo loc_008C7FFB
  loc_008C7FF6: 'Referenced from: 008C7FD1
  loc_008C7FF6: var_eax = Err.Raise
  loc_008C7FFB: 'Referenced from: 008C7FF4
  loc_008C800B: var_eax = Err.Raise
  loc_008C800F: If Err.Raise <= 0 Then GoTo loc_008C82A9
  loc_008C801A: If var_24 = 0 Then GoTo loc_008C803F
  loc_008C8020: If var_24 <> 1 Then GoTo loc_008C803F
  loc_008C802B: var_20 = var_20 - ecx+00000014h
  loc_008C802F: If var_20 < 0 Then GoTo loc_008C8036
  loc_008C8031: var_eax = Err.Raise
  loc_008C8036: 'Referenced from: 008C802F
  loc_008C803D: GoTo loc_008C8044
  loc_008C803F: 'Referenced from: 008C801A
  loc_008C803F: var_eax = Err.Raise
  loc_008C8044: 'Referenced from: 008C803D
  loc_008C8054: var_eax = Err.Raise
  loc_008C8079: If var_50 = 0 Then GoTo loc_008C80A2
  loc_008C807F: If var_50 <> 1 Then GoTo loc_008C80A2
  loc_008C808A: var_20 = var_20 - ecx+00000014h
  loc_008C808E: If var_20 < 0 Then GoTo loc_008C8099
  loc_008C8090: var_eax = Err.Raise
  loc_008C8099: 'Referenced from: 008C808E
  loc_008C80A0: GoTo loc_008C80A7
  loc_008C80A2: 'Referenced from: 008C8079
  loc_008C80A2: call var_20(var_50, var_24, 00000000h, var_004A1940, ecx+eax, 00000001h, 00000000h, "Out1_Out1_Amplitude=", var_278, 00000001h, var_50, var_50, var_24, 00000000h, "Loaded_Frequencies=", var_278)
  loc_008C80A7: 'Referenced from: 008C80A0
  loc_008C80AA: ecx+0000000Ch = ecx+0000000Ch + var_20(var_50, var_24, 00000000h, var_004A1940, ecx+eax, 00000001h, 00000000h, "Out1_Out1_Amplitude=", var_278, 00000001h, var_50, var_50, var_24, 00000000h, "Loaded_Frequencies=", var_278)
  loc_008C80B0: Err.Raise = Err.Raise + 00000001h
  loc_008C80C0: var_F8 = ecx+0000000Ch
  loc_008C80ED: var_5C = CStr(Mid(, Err.Raise, var_70))
  loc_008C812E: var_108 = "Out1_Amplitude="
  loc_008C813E: var_F8 = var_18
  loc_008C8162: var_80 = Trim(Str())
  loc_008C8175: var_118 = "vbCrLf"
  loc_008C8185: var_128 = "Out2_Amplitude="
  loc_008C8195: var_138 = var_18
  loc_008C81B9: var_D0 = Trim(Str(var_18))
  loc_008C81C0: If var_24 = 0 Then GoTo loc_008C81E2
  loc_008C81C6: If var_24 <> 1 Then GoTo loc_008C81E2
  loc_008C81D1: var_20 = var_20 - eax+00000014h
  loc_008C81D5: If var_20 < 0 Then GoTo loc_008C81DD
  loc_008C81D7: var_eax = Err.Raise
  loc_008C81DD: 'Referenced from: 008C81D5
  loc_008C81E0: GoTo loc_008C81EA
  loc_008C81E2: 'Referenced from: 008C81C0
  loc_008C81E2: var_eax = Err.Raise
  loc_008C81EA: 'Referenced from: 008C81E0
  loc_008C824D: var_5C = "Out1_Amplitude=" & var_80 & "vbCrLf" & "Out2_Amplitude=" & var_D0
  loc_008C825B: eax+0000000Ch = eax+0000000Ch + Err.Raise
  loc_008C825D: ecx = var_5C
  loc_008C82A4: GoTo loc_008C88D1
  loc_008C82A9: 'Referenced from: 008C800F
  loc_008C82B7: If 00000001h > 0 Then GoTo loc_008C873B
  loc_008C82BF: If var_24 = 0 Then GoTo loc_008C82EA
  loc_008C82C5: If var_24 <> 1 Then GoTo loc_008C82EA
  loc_008C82D6: var_20 = var_20 - ecx+00000014h
  loc_008C82DA: If var_20 < 0 Then GoTo loc_008C82E1
  loc_008C82DC: var_eax = Err.Raise
  loc_008C82E1: 'Referenced from: 008C82DA
  loc_008C82E8: GoTo loc_008C82F5
  loc_008C82EA: 'Referenced from: 008C82BF
  loc_008C82EA: call var_20(var_50)
  loc_008C82F5: 'Referenced from: 008C82E8
  loc_008C830F: If InStr(1, ecx+eax, "Text20=", 0) > 0 Then GoTo loc_008C84F6
  loc_008C8335: If var_58 = 0 Then GoTo loc_008C837F
  loc_008C833B: If var_58 <> 2 Then GoTo loc_008C837F
  loc_008C8348: 00000002h = 00000002h - eax+0000001Ch
  loc_008C834C: If 00000002h < 0 Then GoTo loc_008C8357
  loc_008C834E: var_eax = Err.Raise
  loc_008C8357: 'Referenced from: 008C834C
  loc_008C835D: 00000001h = 00000001h - eax+00000014h
  loc_008C8361: If 00000001h < 0 Then GoTo loc_008C836C
  loc_008C8363: var_eax = Err.Raise
  loc_008C836C: 'Referenced from: 008C8361
  loc_008C836F: eax+00000018h = eax+00000018h * 1
  loc_008C8372: eax+00000018h = eax+00000018h + 00000002h
  loc_008C837D: GoTo loc_008C8383
  loc_008C837F: 'Referenced from: 008C8335
  loc_008C837F: var_eax = Err.Raise
  loc_008C8383: 'Referenced from: 008C837D
  loc_008C8399: If var_54 = 0 Then GoTo loc_008C83EE
  loc_008C839F: If var_54 <> 2 Then GoTo loc_008C83EE
  loc_008C83AC: 00000001h = 00000001h - eax+0000001Ch
  loc_008C83B6: If 00000001h < 0 Then GoTo loc_008C83BD
  loc_008C83B8: var_eax = Err.Raise
  loc_008C83BD: 'Referenced from: 008C83B6
  loc_008C83C6: var_34 = var_34 - eax+00000014h
  loc_008C83CA: If var_34 < 0 Then GoTo loc_008C83D5
  loc_008C83CC: var_eax = Err.Raise
  loc_008C83D5: 'Referenced from: 008C83CA
  loc_008C83DE: eax+00000018h = eax+00000018h * var_34
  loc_008C83E7: eax+00000018h = eax+00000018h + var_154
  loc_008C83EC: GoTo loc_008C83F2
  loc_008C83EE: 'Referenced from: 008C8399
  loc_008C83EE: var_eax = Err.Raise
  loc_008C83F2: 'Referenced from: 008C83EC
  loc_008C8405: If var_50 = 0 Then GoTo loc_008C842E
  loc_008C840B: If var_50 <> 1 Then GoTo loc_008C842E
  loc_008C8416: var_20 = var_20 - ecx+00000014h
  loc_008C841A: If var_20 < 0 Then GoTo loc_008C8425
  loc_008C841C: var_eax = Err.Raise
  loc_008C8425: 'Referenced from: 008C841A
  loc_008C842C: GoTo loc_008C8433
  loc_008C842E: 'Referenced from: 008C8405
  loc_008C842E: call var_20(var_50, var_24, var_54, eax+00000048h, var_58, eax+00000048h)
  loc_008C8433: 'Referenced from: 008C842C
  loc_008C844C: ebx+0000000Ch = ebx+0000000Ch + Err.Raise
  loc_008C8458: edi+0000000Ch = edi+0000000Ch + Err.Raise
  loc_008C845B: ecx+0000000Ch = ecx+0000000Ch + var_20(var_50, var_24, var_54, eax+00000048h, var_58, eax+00000048h)
  loc_008C845F: var_eax = Font.1808
  loc_008C8482: If var_24 = 0 Then GoTo loc_008C84AB
  loc_008C8488: If var_24 <> 1 Then GoTo loc_008C84AB
  loc_008C8493: var_20 = var_20 - ecx+00000014h
  loc_008C8497: If var_20 < 0 Then GoTo loc_008C84A2
  loc_008C8499: var_eax = Err.Raise
  loc_008C84A2: 'Referenced from: 008C8497
  loc_008C84A9: GoTo loc_008C84B4
  loc_008C84AB: 'Referenced from: 008C8482
  loc_008C84AB: var_eax = Err.Raise
  loc_008C84B4: 'Referenced from: 008C84A9
  loc_008C84BA: ecx+0000000Ch = ecx+0000000Ch + Err.Raise
  loc_008C84BC: ecx = var_5C
  loc_008C84D0: If ebx+0000004Ch = var_FFFFFF Then GoTo loc_008C86C9
  loc_008C84E4: 00000001h = 00000001h + var_34
  loc_008C84F1: GoTo loc_008C82B1
  loc_008C84F6: 'Referenced from: 008C830F
  loc_008C84FB: If var_24 = 0 Then GoTo loc_008C8520
  loc_008C8501: If var_24 <> 1 Then GoTo loc_008C8520
  loc_008C850C: var_20 = var_20 - ecx+00000014h
  loc_008C8510: If var_20 < 0 Then GoTo loc_008C8517
  loc_008C8512: call Me(var_58, var_54, var_50, Me, ecx+0000000Ch, edi+0000000Ch, ebx+0000000Ch, var_144, var_5C)
  loc_008C8517: 'Referenced from: 008C8510
  loc_008C851E: GoTo loc_008C8525
  loc_008C8520: 'Referenced from: 008C84FB
  loc_008C8520: call Me
  loc_008C8525: 'Referenced from: 008C851E
  loc_008C8535: var_eax = Err.Raise
  loc_008C855A: If var_50 = 0 Then GoTo loc_008C857F
  loc_008C8560: If var_50 <> 1 Then GoTo loc_008C857F
  loc_008C856B: var_20 = var_20 - ecx+00000014h
  loc_008C856F: If var_20 < 0 Then GoTo loc_008C8576
  loc_008C8571: call Me(var_50, var_24, 00000000h, var_004A1940, var_278, 00000001h)
  loc_008C8576: 'Referenced from: 008C856F
  loc_008C857D: GoTo loc_008C8584
  loc_008C857F: 'Referenced from: 008C855A
  loc_008C857F: call Me
  loc_008C8584: 'Referenced from: 008C857D
  loc_008C858D: Err.Raise = Err.Raise + 00000001h
  loc_008C859D: var_F8 = ecx+0000000Ch
  loc_008C85CA: var_5C = CStr(Mid(, Err.Raise, var_70))
  loc_008C8608: var_108 = "Out1_Amplitude="
  loc_008C861C: var_F8 = var_18
  loc_008C863A: var_80 = Trim(Str())
  loc_008C8645: If var_24 = 0 Then GoTo loc_008C8667
  loc_008C864B: If var_24 <> 1 Then GoTo loc_008C8667
  loc_008C8656: var_20 = var_20 - eax+00000014h
  loc_008C865A: If var_20 < 0 Then GoTo loc_008C8662
  loc_008C865C: var_eax = Err.Raise
  loc_008C8662: 'Referenced from: 008C865A
  loc_008C8665: GoTo loc_008C866F
  loc_008C8667: 'Referenced from: 008C8645
  loc_008C8667: var_eax = Err.Raise
  loc_008C866F: 'Referenced from: 008C8665
  loc_008C86A1: eax+0000000Ch = eax+0000000Ch + Err.Raise
  loc_008C86A3: ecx = "Out1_Amplitude=" & var_80
  loc_008C86C4: GoTo loc_008C88D1
  loc_008C86C9: 'Referenced from: 008C84D0
  loc_008C86CE: If ebx+00000048h = 0 Then GoTo loc_008C8713
  loc_008C86D4: If ebx+00000048h <> 2 Then GoTo loc_008C8713
  loc_008C86E1: 00000002h = 00000002h - eax+0000001Ch
  loc_008C86E5: If 00000002h < 0 Then GoTo loc_008C86ED
  loc_008C86E7: var_eax = Err.Raise
  loc_008C86ED: 'Referenced from: 008C86E5
  loc_008C86F3: var_34 = var_34 - eax+00000014h
  loc_008C86FB: If var_34 < 0 Then GoTo loc_008C8703
  loc_008C86FD: var_eax = Err.Raise
  loc_008C8703: 'Referenced from: 008C86FB
  loc_008C8709: ecx+00000018h = ecx+00000018h * var_34
  loc_008C870C: ecx+00000018h = ecx+00000018h + 00000002h
  loc_008C8711: GoTo loc_008C8719
  loc_008C8713: 'Referenced from: 008C86CE
  loc_008C8713: var_eax = Err.Raise
  loc_008C8719: 'Referenced from: 008C8711
End Sub

Public Sub Proc_22_6_8C8AD0
  loc_008C8B3F: var_20 = edi
  loc_008C8B58: var_DC = Len(arg_C)
  loc_008C8B68: If var_DC < 0 Then GoTo loc_008C8D0E
  loc_008C8B75: If Len(arg_10) = 0 Then GoTo loc_008C8D0E
  loc_008C8B82: If arg_18 <> var_FFFFFF Then GoTo loc_008C8BFC
  loc_008C8B95: var_88 = arg_C
  loc_008C8BA5: var_30 = LCase(arg_C)
  loc_008C8BB2: var_98 = arg_10
  loc_008C8BC2: var_40 = LCase(arg_10)
  loc_008C8BD4: call InStr(var_50, 00000000h, var_40, var_30, 00000001h, arg_C, arg_10, %x1 = LCase(%StkVar2))
  loc_008C8BDB: var_ret_1 = CLng(InStr(var_50, 00000000h, var_40, var_30, 00000001h, arg_C, arg_10, %x1 = LCase(%StkVar2)))
  loc_008C8BFA: GoTo loc_008C8C0E
  loc_008C8BFC: 'Referenced from: 008C8B82
  loc_008C8C0E: 'Referenced from: 008C8BFA
  loc_008C8C10: If InStr(1, arg_C, arg_10, 0) = 0 Then GoTo loc_008C8D0E
  loc_008C8C27: InStr(1, arg_C, arg_10, 0) = InStr(1, arg_C, arg_10, 0) - 00000001h
  loc_008C8C38: var_28 = InStr(1, arg_C, arg_10, 0)
  loc_008C8C47: var_88 = arg_C
  loc_008C8C66: Len(arg_10) = Len(arg_10) + InStr(1, arg_C, arg_10, 0)
  loc_008C8C75: var_A8 = var_C0
  loc_008C8C98: var_B8 = arg_C
  loc_008C8CDC: var_20 = Mid(arg_C, 1, InStr(1, arg_C, arg_10, 0)) & &H4008 & Mid(arg_C, Len(arg_10), 10)
  loc_008C8D0E: 'Referenced from: 008C8B68
  loc_008C8D13: GoTo loc_008C8D48
  loc_008C8D19: If var_4 = 0 Then GoTo loc_008C8D24
  loc_008C8D24: 'Referenced from: 008C8D19
  loc_008C8D47: Exit Sub
  loc_008C8D48: 'Referenced from: 008C8D13
End Sub

Public Sub Proc_22_7_8C8D70
  loc_008C8D92: ReDim %x1 = 00000008h.Name(2 To 0)
  loc_008C8D9F: If %x1 = 00000008h.Name = 0 Then GoTo loc_008C8DE4
  loc_008C8DA5: If esi <> 2 Then GoTo loc_008C8DE4
  loc_008C8DB8: 00000001h = 00000001h - 00000008h.%x3 = PropBag.ReadProperty(%StkVar1, %StkVar2)
  loc_008C8DBC: If 00000001h < 0 Then GoTo loc_008C8DC0
  loc_008C8DBE: var_eax = Err.Raise
  loc_008C8DC0: 'Referenced from: 008C8DBC
  loc_008C8DCD: 00000001h = 00000001h - 00000008h.%x1 = GetIDsOfNames(%StkVar2) 'Ignore this
  loc_008C8DD1: If 00000001h < 0 Then GoTo loc_008C8DD5
  loc_008C8DD3: var_eax = Err.Raise
  loc_008C8DD5: 'Referenced from: 008C8DD1
  loc_008C8DE2: GoTo loc_008C8DEA
  loc_008C8DE4: 'Referenced from: 008C8D9F
  loc_008C8DE4: var_eax = Err.Raise
  loc_008C8DEA: 'Referenced from: 008C8DE2
  loc_008C8DFC: ecx = "Spooky Coil (XM Direct) - JW"
  loc_008C8E02: If %x1 = 00000008h.Name = 0 Then GoTo loc_008C8E4F
  loc_008C8E08: If esi <> 2 Then GoTo loc_008C8E4F
  loc_008C8E15: 00000002h = 00000002h - 00000008h.%x3 = PropBag.ReadProperty(%StkVar1, %StkVar2)
  loc_008C8E19: If 00000002h < 0 Then GoTo loc_008C8E21
  loc_008C8E1B: var_eax = Err.Raise
  loc_008C8E21: 'Referenced from: 008C8E19
  loc_008C8E2E: 00000001h = 00000001h - 00000008h.%x1 = GetIDsOfNames(%StkVar2) 'Ignore this
  loc_008C8E32: If 00000001h < 0 Then GoTo loc_008C8E3A
  loc_008C8E34: var_eax = Err.Raise
  loc_008C8E3A: 'Referenced from: 008C8E32
End Sub

Public Sub Proc_22_8_8D5EF0
  loc_008D5F44: ecx = "Padlock=PresetName=Base_Preset=Loaded_Programs=Loaded_Frequencies=Loaded_Spooky_Program="
  loc_008D5F68: var_18 = FreeFile(var_30)
  loc_008D5F99: Open Set  = edi(FreeFile(var_30)) & "\Data\ControlFactoryDefault.txt" For Input As #FreeFile(var_30) Len = -1
  loc_008D5FB4: 
  loc_008D5FB6: var_ret_2 = FreeFile(var_30)
  loc_008D5FC8: If EOF(var_ret_2) <> 0 Then GoTo loc_008D6104
  loc_008D5FD9: Line Input #FreeFile(var_30), var_1C
  loc_008D5FEB: var_68 = var_1C
  loc_008D602C: var_94 = (Mid(var_1C, 1, 1) = Chr(34))
  loc_008D6045: If var_94 = 0 Then GoTo loc_008D5FB4
  loc_008D6060: If InStr(1, var_1C, var_004A1940, 0) <= 0 Then GoTo loc_008D5FB4
End Sub

Public Sub Proc_22_9_8D6300
  loc_008D6386: var_30 = " *?/><^&'@{}[],$=!-#()%.+~_abcdefghijlklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789" & var_004B7294 & var_004A33E0
  loc_008D6396: var_48 = var_30 & var_004AE7C8
  loc_008D63FD: If Len(arg_C) = 0 Then GoTo loc_008D652B
  loc_008D640F: var_9C = Len(arg_C)
  loc_008D6423: If 00000001h > 0 Then GoTo loc_008D651F
  loc_008D6431: var_68 = arg_C
  loc_008D6451: var_50 = Mid(arg_C, 1, 1)
  loc_008D6466: var_24 = var_50
  loc_008D647D: var_78 = var_30 & var_004AE7C8 & Chr(34)
  loc_008D648F: var_68 = var_24
  loc_008D6499: var_40 = Ucase(var_24)
  loc_008D64BE: call InStr(var_50, ebx, var_40, var_80, 00000001h)
  loc_008D64E6: If (InStr(var_50, ebx, var_40, var_80, 00000001h) > "") = 0 Then GoTo loc_008D6501
  loc_008D64FB: var_18 = var_18 & var_24
  loc_008D6501: 'Referenced from: 008D64E6
  loc_008D650F: 00000001h = 00000001h + var_1C
  loc_008D651A: GoTo loc_008D641D
  loc_008D651F: 'Referenced from: 008D6423
  loc_008D6525: var_28 = var_18
  loc_008D652B: 'Referenced from: 008D63FD
  loc_008D6530: GoTo loc_008D6569
  loc_008D6536: If var_4 = 0 Then GoTo loc_008D6541
  loc_008D6541: 'Referenced from: 008D6536
  loc_008D6568: Exit Sub
  loc_008D6569: 'Referenced from: 008D6530
  loc_008D657E: Exit Sub
End Sub

Public Sub Proc_22_10_8D65B0
  loc_008D6615: If (arg_C = vbNullString) = 0 Then GoTo loc_008D673C
  loc_008D662F: If 00000001h > 0 Then GoTo loc_008D673C
  loc_008D6640: var_5C = arg_C
  loc_008D6672: var_20 = Mid(arg_C, 1, 1)
  loc_008D6698: If (var_20 <> var_004A1940) <> 0 Then GoTo loc_008D66A1
  loc_008D66A1: 'Referenced from: 008D6698
  loc_008D66B2: If (var_20 = var_004A187C) = 0 Then GoTo loc_008D66B9
  loc_008D66B7: If 00000001h <> 0 Then GoTo loc_008D6714
  loc_008D66B9: 'Referenced from: 008D66B2
  loc_008D66D0: var_14 = var_14 + 00000001h
  loc_008D66DB: var_14 = var_14
  loc_008D66F4: var_28 = var_28 & var_30 & var_004A187C
  loc_008D6707: var_30 = vbNullString
  loc_008D6714: 
  loc_008D6719: If var_24 <> 0 Then GoTo loc_008D672C
  loc_008D672A: var_30 = var_30 & vbNullString
  loc_008D672C: 'Referenced from: 008D6719
  loc_008D6731: 00000001h = 00000001h + 00000001h
  loc_008D6737: GoTo loc_008D662C
  loc_008D673C: 'Referenced from: 008D6615
  loc_008D6741: GoTo loc_008D6760
  loc_008D675F: Exit Sub
  loc_008D6760: 'Referenced from: 008D6741
  loc_008D6775: Exit Sub
End Sub

Public Sub Proc_22_11_8D67A0
  loc_008D67E7: var_18 = vbNullString
  loc_008D67FB: var_24 = Len(arg_C)
  loc_008D67FE: If Len(arg_C) = 0 Then GoTo loc_008D68CD
  loc_008D681E: If 00000001h > 0 Then GoTo loc_008D68CD
  loc_008D682F: var_50 = arg_C
  loc_008D6861: var_20 = Mid(arg_C, 1, 1)
  loc_008D6883: If (var_20 <> var_004A1940) <> 0 Then GoTo loc_008D6888
  loc_008D6885: var_28 = (var_20 = var_004A1940)
  loc_008D6888: 'Referenced from: 008D6883
  loc_008D6895: If (var_20 <> var_004A187C) <> 0 Then GoTo loc_008D689E
  loc_008D689E: 'Referenced from: 008D6895
  loc_008D68A3: If var_28 <> var_FFFFFF Then GoTo loc_008D68BA
  loc_008D68B8: var_18 = var_18 & var_20
  loc_008D68BA: 'Referenced from: 008D68A3
  loc_008D68BF: 00000001h = 00000001h + 00000001h
  loc_008D68C8: GoTo loc_008D681C
  loc_008D68CD: 'Referenced from: 008D67FE
  loc_008D68D2: GoTo loc_008D68F7
  loc_008D68D8: If var_4 = 0 Then GoTo loc_008D68E3
  loc_008D68E3: 'Referenced from: 008D68D8
  loc_008D68F6: Exit Sub
  loc_008D68F7: 'Referenced from: 008D68D2
  loc_008D6900: Exit Sub
End Sub
