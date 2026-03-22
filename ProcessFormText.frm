VERSION 5.00
Begin VB.Form ProcessFormText
  Caption = "Form1"
  ScaleMode = 1
  AutoRedraw = False
  FontTransparent = True
  Icon = "ProcessFormText.frx":0
  LinkTopic = "Form1"
  ClientLeft = 60
  ClientTop = 510
  ClientWidth = 9180
  ClientHeight = 4410
  BeginProperty Font
    Name = "Arial"
    Size = 11.25
    Charset = 0
    Weight = 400
    Underline = 0 'False
    Italic = 0 'False
    Strikethrough = 0 'False
  EndProperty
  StartUpPosition = 3 'Windows Default
  Begin CommandButton Command1
    Caption = "Write"
    Index = 1
    BackColor = &H0&
    Left = 3600
    Top = 1080
    Width = 975
    Height = 495
    TabIndex = 1
  End
  Begin CommandButton Command1
    Caption = "Read"
    Index = 0
    BackColor = &H0&
    Left = 2160
    Top = 1080
    Width = 975
    Height = 495
    TabIndex = 0
  End
End

Attribute VB_Name = "ProcessFormText"


Private Sub Command1_Click() '8C1350
  loc_008C13BB: ReDim Me.SaveProp(ebx To ebx)
  loc_008C13CE: ReDim Me.GetPalette(ebx To ebx)
  loc_008C13D9: If arg_C <> 0 Then GoTo loc_008C18EA
  loc_008C13FC: var_18 = FreeFile(var_30)
  loc_008C140F: Open "ControlText.txt" For Output As #FreeFile(var_30) Len = -1
  loc_008C143F: call __vbaCastObj(var_008D9148, var_004B4954)
  loc_008C1455: var_eax = ProcessFormText.Proc_19_2_8C2570(Me, var_20)
  loc_008C1464: call __vbaCastObj(var_20, var_004A5930, var_20, __vbaCastObj(var_008D9148, var_004B4954))
  loc_008C149C: call __vbaCastObj(var_008D90F8, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A5930, var_20, __vbaCastObj(var_008D9148, var_004B4954)))
  loc_008C14AC: var_eax = ProcessFormText.Proc_19_2_8C2570(Me, var_20)
  loc_008C14BB: call __vbaCastObj(var_20, var_004A2CDC, var_20, __vbaCastObj(var_008D90F8, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A5930, var_20, __vbaCastObj(var_008D9148, var_004B4954))))
  loc_008C14F2: call __vbaCastObj(var_008D9134, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A2CDC, var_20, __vbaCastObj(var_008D90F8, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A5930, var_20, __vbaCastObj(var_008D9148, var_004B4954)))))
  loc_008C1502: var_eax = ProcessFormText.Proc_19_2_8C2570(Me, var_20)
  loc_008C1511: call __vbaCastObj(var_20, var_004A2F9C, var_20, __vbaCastObj(var_008D9134, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A2CDC, var_20, __vbaCastObj(var_008D90F8, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A5930, var_20, __vbaCastObj(var_008D9148, var_004B4954))))))
  loc_008C1549: call __vbaCastObj(var_008D9120, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A2F9C, var_20, __vbaCastObj(var_008D9134, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A2CDC, var_20, __vbaCastObj(var_008D90F8, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A5930, var_20, __vbaCastObj(var_008D9148, var_004B4954)))))))
  loc_008C1559: var_eax = ProcessFormText.Proc_19_2_8C2570(Me, var_20)
  loc_008C1568: call __vbaCastObj(var_20, var_004A2ED8, var_20, __vbaCastObj(var_008D9120, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A2F9C, var_20, __vbaCastObj(var_008D9134, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A2CDC, var_20, __vbaCastObj(var_008D90F8, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A5930, var_20, __vbaCastObj(var_008D9148, var_004B4954))))))))
  loc_008C15A0: call __vbaCastObj(var_008D9094, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A2ED8, var_20, __vbaCastObj(var_008D9120, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A2F9C, var_20, __vbaCastObj(var_008D9134, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A2CDC, var_20, __vbaCastObj(var_008D90F8, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A5930, var_20, __vbaCastObj(var_008D9148, var_004B4954)))))))))
  loc_008C15B0: var_eax = ProcessFormText.Proc_19_2_8C2570(Me, var_20)
  loc_008C15BF: call __vbaCastObj(var_20, var_004A14B8, var_20, __vbaCastObj(var_008D9094, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A2ED8, var_20, __vbaCastObj(var_008D9120, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A2F9C, var_20, __vbaCastObj(var_008D9134, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A2CDC, var_20, __vbaCastObj(var_008D90F8, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A5930, var_20, __vbaCastObj(var_008D9148, var_004B4954))))))))))
  loc_008C15F6: call __vbaCastObj(var_008D90BC, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A14B8, var_20, __vbaCastObj(var_008D9094, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A2ED8, var_20, __vbaCastObj(var_008D9120, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A2F9C, var_20, __vbaCastObj(var_008D9134, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A2CDC, var_20, __vbaCastObj(var_008D90F8, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A5930, var_20, __vbaCastObj(var_008D9148, var_004B4954)))))))))))
  loc_008C1606: var_eax = ProcessFormText.Proc_19_2_8C2570(Me, var_20)
  loc_008C1615: call __vbaCastObj(var_20, var_004A1630, var_20, __vbaCastObj(var_008D90BC, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A14B8, var_20, __vbaCastObj(var_008D9094, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A2ED8, var_20, __vbaCastObj(var_008D9120, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A2F9C, var_20, __vbaCastObj(var_008D9134, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A2CDC, var_20, __vbaCastObj(var_008D90F8, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A5930, var_20, __vbaCastObj(var_008D9148, var_004B4954))))))))))))
  loc_008C164D: call __vbaCastObj(var_008D9170, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A1630, var_20, __vbaCastObj(var_008D90BC, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A14B8, var_20, __vbaCastObj(var_008D9094, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A2ED8, var_20, __vbaCastObj(var_008D9120, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A2F9C, var_20, __vbaCastObj(var_008D9134, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A2CDC, var_20, __vbaCastObj(var_008D90F8, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A5930, var_20, __vbaCastObj(var_008D9148, var_004B4954)))))))))))))
  loc_008C165D: var_eax = ProcessFormText.Proc_19_2_8C2570(Me, var_20)
  loc_008C166C: call __vbaCastObj(var_20, var_004A59D0, var_20, __vbaCastObj(var_008D9170, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A1630, var_20, __vbaCastObj(var_008D90BC, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A14B8, var_20, __vbaCastObj(var_008D9094, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A2ED8, var_20, __vbaCastObj(var_008D9120, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A2F9C, var_20, __vbaCastObj(var_008D9134, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A2CDC, var_20, __vbaCastObj(var_008D90F8, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A5930, var_20, __vbaCastObj(var_008D9148, var_004B4954))))))))))))))
  loc_008C16A4: call __vbaCastObj(var_008D9020, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A59D0, var_20, __vbaCastObj(var_008D9170, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A1630, var_20, __vbaCastObj(var_008D90BC, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A14B8, var_20, __vbaCastObj(var_008D9094, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A2ED8, var_20, __vbaCastObj(var_008D9120, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A2F9C, var_20, __vbaCastObj(var_008D9134, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A2CDC, var_20, __vbaCastObj(var_008D90F8, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A5930, var_20, __vbaCastObj(var_008D9148, var_004B4954)))))))))))))))
  loc_008C16B4: var_eax = ProcessFormText.Proc_19_2_8C2570(Me, var_20)
  loc_008C16C3: call __vbaCastObj(var_20, var_0049C038, var_20, __vbaCastObj(var_008D9020, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A59D0, var_20, __vbaCastObj(var_008D9170, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A1630, var_20, __vbaCastObj(var_008D90BC, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A14B8, var_20, __vbaCastObj(var_008D9094, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A2ED8, var_20, __vbaCastObj(var_008D9120, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A2F9C, var_20, __vbaCastObj(var_008D9134, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A2CDC, var_20, __vbaCastObj(var_008D90F8, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A5930, var_20, __vbaCastObj(var_008D9148, var_004B4954))))))))))))))))
  loc_008C16FA: call __vbaCastObj(var_008D90A8, var_004B4954, vbNullString, __vbaCastObj(var_20, var_0049C038, var_20, __vbaCastObj(var_008D9020, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A59D0, var_20, __vbaCastObj(var_008D9170, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A1630, var_20, __vbaCastObj(var_008D90BC, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A14B8, var_20, __vbaCastObj(var_008D9094, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A2ED8, var_20, __vbaCastObj(var_008D9120, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A2F9C, var_20, __vbaCastObj(var_008D9134, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A2CDC, var_20, __vbaCastObj(var_008D90F8, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A5930, var_20, __vbaCastObj(var_008D9148, var_004B4954)))))))))))))))))
  loc_008C170A: var_eax = ProcessFormText.Proc_19_2_8C2570(Me, var_20)
  loc_008C1719: call __vbaCastObj(var_20, var_004A15D4, var_20, __vbaCastObj(var_008D90A8, var_004B4954, vbNullString, __vbaCastObj(var_20, var_0049C038, var_20, __vbaCastObj(var_008D9020, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A59D0, var_20, __vbaCastObj(var_008D9170, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A1630, var_20, __vbaCastObj(var_008D90BC, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A14B8, var_20, __vbaCastObj(var_008D9094, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A2ED8, var_20, __vbaCastObj(var_008D9120, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A2F9C, var_20, __vbaCastObj(var_008D9134, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A2CDC, var_20, __vbaCastObj(var_008D90F8, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A5930, var_20, __vbaCastObj(var_008D9148, var_004B4954))))))))))))))))))
  loc_008C1751: call __vbaCastObj(var_008D9094, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A15D4, var_20, __vbaCastObj(var_008D90A8, var_004B4954, vbNullString, __vbaCastObj(var_20, var_0049C038, var_20, __vbaCastObj(var_008D9020, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A59D0, var_20, __vbaCastObj(var_008D9170, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A1630, var_20, __vbaCastObj(var_008D90BC, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A14B8, var_20, __vbaCastObj(var_008D9094, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A2ED8, var_20, __vbaCastObj(var_008D9120, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A2F9C, var_20, __vbaCastObj(var_008D9134, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A2CDC, var_20, __vbaCastObj(var_008D90F8, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A5930, var_20, __vbaCastObj(var_008D9148, var_004B4954)))))))))))))))))))
  loc_008C1761: var_eax = ProcessFormText.Proc_19_1_8C1EF0(Me, var_20)
  loc_008C1770: call __vbaCastObj(var_20, var_004A14B8, var_20, __vbaCastObj(var_008D9094, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A15D4, var_20, __vbaCastObj(var_008D90A8, var_004B4954, vbNullString, __vbaCastObj(var_20, var_0049C038, var_20, __vbaCastObj(var_008D9020, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A59D0, var_20, __vbaCastObj(var_008D9170, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A1630, var_20, __vbaCastObj(var_008D90BC, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A14B8, var_20, __vbaCastObj(var_008D9094, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A2ED8, var_20, __vbaCastObj(var_008D9120, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A2F9C, var_20, __vbaCastObj(var_008D9134, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A2CDC, var_20, __vbaCastObj(var_008D90F8, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A5930, var_20, __vbaCastObj(var_008D9148, var_004B4954))))))))))))))))))))
  loc_008C17A8: call __vbaCastObj(var_008D9044, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A14B8, var_20, __vbaCastObj(var_008D9094, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A15D4, var_20, __vbaCastObj(var_008D90A8, var_004B4954, vbNullString, __vbaCastObj(var_20, var_0049C038, var_20, __vbaCastObj(var_008D9020, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A59D0, var_20, __vbaCastObj(var_008D9170, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A1630, var_20, __vbaCastObj(var_008D90BC, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A14B8, var_20, __vbaCastObj(var_008D9094, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A2ED8, var_20, __vbaCastObj(var_008D9120, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A2F9C, var_20, __vbaCastObj(var_008D9134, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A2CDC, var_20, __vbaCastObj(var_008D90F8, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A5930, var_20, __vbaCastObj(var_008D9148, var_004B4954)))))))))))))))))))))
  loc_008C17B8: var_eax = ProcessFormText.Proc_19_2_8C2570(Me, var_20)
  loc_008C17C7: call __vbaCastObj(var_20, var_004A0ED8, var_20, __vbaCastObj(var_008D9044, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A14B8, var_20, __vbaCastObj(var_008D9094, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A15D4, var_20, __vbaCastObj(var_008D90A8, var_004B4954, vbNullString, __vbaCastObj(var_20, var_0049C038, var_20, __vbaCastObj(var_008D9020, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A59D0, var_20, __vbaCastObj(var_008D9170, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A1630, var_20, __vbaCastObj(var_008D90BC, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A14B8, var_20, __vbaCastObj(var_008D9094, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A2ED8, var_20, __vbaCastObj(var_008D9120, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A2F9C, var_20, __vbaCastObj(var_008D9134, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A2CDC, var_20, __vbaCastObj(var_008D90F8, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A5930, var_20, __vbaCastObj(var_008D9148, var_004B4954))))))))))))))))))))))
  loc_008C17FE: call __vbaCastObj(var_008D90E4, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A0ED8, var_20, __vbaCastObj(var_008D9044, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A14B8, var_20, __vbaCastObj(var_008D9094, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A15D4, var_20, __vbaCastObj(var_008D90A8, var_004B4954, vbNullString, __vbaCastObj(var_20, var_0049C038, var_20, __vbaCastObj(var_008D9020, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A59D0, var_20, __vbaCastObj(var_008D9170, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A1630, var_20, __vbaCastObj(var_008D90BC, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A14B8, var_20, __vbaCastObj(var_008D9094, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A2ED8, var_20, __vbaCastObj(var_008D9120, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A2F9C, var_20, __vbaCastObj(var_008D9134, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A2CDC, var_20, __vbaCastObj(var_008D90F8, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A5930, var_20, __vbaCastObj(var_008D9148, var_004B4954)))))))))))))))))))))))
  loc_008C180E: var_eax = ProcessFormText.Proc_19_2_8C2570(Me, var_20)
  loc_008C181D: call __vbaCastObj(var_20, var_004A2A80, var_20, __vbaCastObj(var_008D90E4, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A0ED8, var_20, __vbaCastObj(var_008D9044, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A14B8, var_20, __vbaCastObj(var_008D9094, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A15D4, var_20, __vbaCastObj(var_008D90A8, var_004B4954, vbNullString, __vbaCastObj(var_20, var_0049C038, var_20, __vbaCastObj(var_008D9020, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A59D0, var_20, __vbaCastObj(var_008D9170, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A1630, var_20, __vbaCastObj(var_008D90BC, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A14B8, var_20, __vbaCastObj(var_008D9094, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A2ED8, var_20, __vbaCastObj(var_008D9120, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A2F9C, var_20, __vbaCastObj(var_008D9134, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A2CDC, var_20, __vbaCastObj(var_008D90F8, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A5930, var_20, __vbaCastObj(var_008D9148, var_004B4954))))))))))))))))))))))))
  loc_008C1855: call __vbaCastObj(var_008D90D0, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A2A80, var_20, __vbaCastObj(var_008D90E4, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A0ED8, var_20, __vbaCastObj(var_008D9044, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A14B8, var_20, __vbaCastObj(var_008D9094, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A15D4, var_20, __vbaCastObj(var_008D90A8, var_004B4954, vbNullString, __vbaCastObj(var_20, var_0049C038, var_20, __vbaCastObj(var_008D9020, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A59D0, var_20, __vbaCastObj(var_008D9170, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A1630, var_20, __vbaCastObj(var_008D90BC, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A14B8, var_20, __vbaCastObj(var_008D9094, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A2ED8, var_20, __vbaCastObj(var_008D9120, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A2F9C, var_20, __vbaCastObj(var_008D9134, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A2CDC, var_20, __vbaCastObj(var_008D90F8, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A5930, var_20, __vbaCastObj(var_008D9148, var_004B4954)))))))))))))))))))))))))
  loc_008C1865: var_eax = ProcessFormText.Proc_19_2_8C2570(Me, var_20)
  loc_008C1874: call __vbaCastObj(var_20, var_004A1778, var_20, __vbaCastObj(var_008D90D0, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A2A80, var_20, __vbaCastObj(var_008D90E4, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A0ED8, var_20, __vbaCastObj(var_008D9044, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A14B8, var_20, __vbaCastObj(var_008D9094, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A15D4, var_20, __vbaCastObj(var_008D90A8, var_004B4954, vbNullString, __vbaCastObj(var_20, var_0049C038, var_20, __vbaCastObj(var_008D9020, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A59D0, var_20, __vbaCastObj(var_008D9170, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A1630, var_20, __vbaCastObj(var_008D90BC, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A14B8, var_20, __vbaCastObj(var_008D9094, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A2ED8, var_20, __vbaCastObj(var_008D9120, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A2F9C, var_20, __vbaCastObj(var_008D9134, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A2CDC, var_20, __vbaCastObj(var_008D90F8, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A5930, var_20, __vbaCastObj(var_008D9148, var_004B4954))))))))))))))))))))))))))
  loc_008C18AC: call __vbaCastObj(var_008D9184, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A1778, var_20, __vbaCastObj(var_008D90D0, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A2A80, var_20, __vbaCastObj(var_008D90E4, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A0ED8, var_20, __vbaCastObj(var_008D9044, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A14B8, var_20, __vbaCastObj(var_008D9094, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A15D4, var_20, __vbaCastObj(var_008D90A8, var_004B4954, vbNullString, __vbaCastObj(var_20, var_0049C038, var_20, __vbaCastObj(var_008D9020, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A59D0, var_20, __vbaCastObj(var_008D9170, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A1630, var_20, __vbaCastObj(var_008D90BC, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A14B8, var_20, __vbaCastObj(var_008D9094, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A2ED8, var_20, __vbaCastObj(var_008D9120, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A2F9C, var_20, __vbaCastObj(var_008D9134, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A2CDC, var_20, __vbaCastObj(var_008D90F8, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A5930, var_20, __vbaCastObj(var_008D9148, var_004B4954)))))))))))))))))))))))))))
  loc_008C18BC: var_eax = ProcessFormText.Proc_19_2_8C2570(Me, var_20)
  loc_008C18CB: call __vbaCastObj(var_20, var_004A5A54, var_20, __vbaCastObj(var_008D9184, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A1778, var_20, __vbaCastObj(var_008D90D0, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A2A80, var_20, __vbaCastObj(var_008D90E4, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A0ED8, var_20, __vbaCastObj(var_008D9044, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A14B8, var_20, __vbaCastObj(var_008D9094, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A15D4, var_20, __vbaCastObj(var_008D90A8, var_004B4954, vbNullString, __vbaCastObj(var_20, var_0049C038, var_20, __vbaCastObj(var_008D9020, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A59D0, var_20, __vbaCastObj(var_008D9170, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A1630, var_20, __vbaCastObj(var_008D90BC, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A14B8, var_20, __vbaCastObj(var_008D9094, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A2ED8, var_20, __vbaCastObj(var_008D9120, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A2F9C, var_20, __vbaCastObj(var_008D9134, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A2CDC, var_20, __vbaCastObj(var_008D90F8, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A5930, var_20, __vbaCastObj(var_008D9148, var_004B4954))))))))))))))))))))))))))))
  loc_008C18E2: Close #var_18
  loc_008C18E8: GoTo loc_008C18F6
  loc_008C18EA: 'Referenced from: 008C13D9
  loc_008C18F6: 'Referenced from: 008C18E8
  loc_008C18FD: If arg_C <> 1 Then GoTo loc_008C1E85
  loc_008C192E: var_1C = Dir("Language.txt", 0)
  loc_008C1944: eax = (var_1C = vbNullString) + 1
  loc_008C194A: var_44 = (var_1C = vbNullString) + 1
  loc_008C1961: If var_44 <> 0 Then GoTo loc_008C1EA5
  loc_008C196A: var_eax = ProcessFormText.Proc_19_6_8C31B0(Me, vbNullString)
  loc_008C1978: call UBound(00000002h, var_50, __vbaCastObj(var_20, var_004A5A54, var_20, __vbaCastObj(var_008D9184, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A1778, var_20, __vbaCastObj(var_008D90D0, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A2A80, var_20, __vbaCastObj(var_008D90E4, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A0ED8, var_20, __vbaCastObj(var_008D9044, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A14B8, var_20, __vbaCastObj(var_008D9094, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A15D4, var_20, __vbaCastObj(var_008D90A8, var_004B4954, vbNullString, __vbaCastObj(var_20, var_0049C038, var_20, __vbaCastObj(var_008D9020, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A59D0, var_20, __vbaCastObj(var_008D9170, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A1630, var_20, __vbaCastObj(var_008D90BC, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A14B8, var_20, __vbaCastObj(var_008D9094, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A2ED8, var_20, __vbaCastObj(var_008D9120, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A2F9C, var_20, __vbaCastObj(var_008D9134, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A2CDC, var_20, __vbaCastObj(var_008D90F8, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A5930, var_20, __vbaCastObj(var_008D9148, var_004B4954)))))))))))))))))))))))))))))
  loc_008C1980: If UBound(00000002h, var_50, __vbaCastObj(var_20, var_004A5A54, var_20, __vbaCastObj(var_008D9184, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A1778, var_20, __vbaCastObj(var_008D90D0, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A2A80, var_20, __vbaCastObj(var_008D90E4, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A0ED8, var_20, __vbaCastObj(var_008D9044, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A14B8, var_20, __vbaCastObj(var_008D9094, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A15D4, var_20, __vbaCastObj(var_008D90A8, var_004B4954, vbNullString, __vbaCastObj(var_20, var_0049C038, var_20, __vbaCastObj(var_008D9020, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A59D0, var_20, __vbaCastObj(var_008D9170, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A1630, var_20, __vbaCastObj(var_008D90BC, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A14B8, var_20, __vbaCastObj(var_008D9094, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A2ED8, var_20, __vbaCastObj(var_008D9120, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A2F9C, var_20, __vbaCastObj(var_008D9134, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A2CDC, var_20, __vbaCastObj(var_008D90F8, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A5930, var_20, __vbaCastObj(var_008D9148, var_004B4954))))))))))))))))))))))))))))) = 0 Then GoTo loc_008C1EA5
  loc_008C19A1: var_18 = FreeFile(10)
  loc_008C19B7: Open "Language.txt" For Input As #var_18 Len = -1
  loc_008C19E2: call __vbaCastObj(var_008D9148, var_004B4954)
  loc_008C19F2: var_eax = ProcessFormText.Proc_19_1_8C1EF0(Me, var_20)
  loc_008C1A01: call __vbaCastObj(var_20, var_004A5930, var_20, __vbaCastObj(var_008D9148, var_004B4954))
  loc_008C1A39: call __vbaCastObj(var_008D90F8, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A5930, var_20, __vbaCastObj(var_008D9148, var_004B4954)))
  loc_008C1A49: var_eax = ProcessFormText.Proc_19_1_8C1EF0(Me, var_20)
  loc_008C1A58: call __vbaCastObj(var_20, var_004A2CDC, var_20, __vbaCastObj(var_008D90F8, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A5930, var_20, __vbaCastObj(var_008D9148, var_004B4954))))
  loc_008C1A8F: call __vbaCastObj(var_008D9134, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A2CDC, var_20, __vbaCastObj(var_008D90F8, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A5930, var_20, __vbaCastObj(var_008D9148, var_004B4954)))))
  loc_008C1A9F: var_eax = ProcessFormText.Proc_19_1_8C1EF0(Me, var_20)
  loc_008C1AAE: call __vbaCastObj(var_20, var_004A2F9C, var_20, __vbaCastObj(var_008D9134, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A2CDC, var_20, __vbaCastObj(var_008D90F8, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A5930, var_20, __vbaCastObj(var_008D9148, var_004B4954))))))
  loc_008C1AE6: call __vbaCastObj(var_008D9120, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A2F9C, var_20, __vbaCastObj(var_008D9134, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A2CDC, var_20, __vbaCastObj(var_008D90F8, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A5930, var_20, __vbaCastObj(var_008D9148, var_004B4954)))))))
  loc_008C1AF6: var_eax = ProcessFormText.Proc_19_1_8C1EF0(Me, var_20)
  loc_008C1B05: call __vbaCastObj(var_20, var_004A2ED8, var_20, __vbaCastObj(var_008D9120, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A2F9C, var_20, __vbaCastObj(var_008D9134, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A2CDC, var_20, __vbaCastObj(var_008D90F8, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A5930, var_20, __vbaCastObj(var_008D9148, var_004B4954))))))))
  loc_008C1B3D: call __vbaCastObj(var_008D9094, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A2ED8, var_20, __vbaCastObj(var_008D9120, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A2F9C, var_20, __vbaCastObj(var_008D9134, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A2CDC, var_20, __vbaCastObj(var_008D90F8, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A5930, var_20, __vbaCastObj(var_008D9148, var_004B4954)))))))))
  loc_008C1B4D: var_eax = ProcessFormText.Proc_19_1_8C1EF0(Me, var_20)
  loc_008C1B5C: call __vbaCastObj(var_20, var_004A14B8, var_20, __vbaCastObj(var_008D9094, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A2ED8, var_20, __vbaCastObj(var_008D9120, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A2F9C, var_20, __vbaCastObj(var_008D9134, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A2CDC, var_20, __vbaCastObj(var_008D90F8, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A5930, var_20, __vbaCastObj(var_008D9148, var_004B4954))))))))))
  loc_008C1B93: call __vbaCastObj(var_008D90BC, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A14B8, var_20, __vbaCastObj(var_008D9094, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A2ED8, var_20, __vbaCastObj(var_008D9120, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A2F9C, var_20, __vbaCastObj(var_008D9134, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A2CDC, var_20, __vbaCastObj(var_008D90F8, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A5930, var_20, __vbaCastObj(var_008D9148, var_004B4954)))))))))))
  loc_008C1BA3: var_eax = ProcessFormText.Proc_19_1_8C1EF0(Me, var_20)
  loc_008C1BB2: call __vbaCastObj(var_20, var_004A1630, var_20, __vbaCastObj(var_008D90BC, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A14B8, var_20, __vbaCastObj(var_008D9094, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A2ED8, var_20, __vbaCastObj(var_008D9120, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A2F9C, var_20, __vbaCastObj(var_008D9134, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A2CDC, var_20, __vbaCastObj(var_008D90F8, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A5930, var_20, __vbaCastObj(var_008D9148, var_004B4954))))))))))))
  loc_008C1BEA: call __vbaCastObj(var_008D9170, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A1630, var_20, __vbaCastObj(var_008D90BC, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A14B8, var_20, __vbaCastObj(var_008D9094, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A2ED8, var_20, __vbaCastObj(var_008D9120, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A2F9C, var_20, __vbaCastObj(var_008D9134, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A2CDC, var_20, __vbaCastObj(var_008D90F8, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A5930, var_20, __vbaCastObj(var_008D9148, var_004B4954)))))))))))))
  loc_008C1BFA: var_eax = ProcessFormText.Proc_19_1_8C1EF0(Me, var_20)
  loc_008C1C09: call __vbaCastObj(var_20, var_004A59D0, var_20, __vbaCastObj(var_008D9170, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A1630, var_20, __vbaCastObj(var_008D90BC, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A14B8, var_20, __vbaCastObj(var_008D9094, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A2ED8, var_20, __vbaCastObj(var_008D9120, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A2F9C, var_20, __vbaCastObj(var_008D9134, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A2CDC, var_20, __vbaCastObj(var_008D90F8, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A5930, var_20, __vbaCastObj(var_008D9148, var_004B4954))))))))))))))
  loc_008C1C41: call __vbaCastObj(var_008D9020, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A59D0, var_20, __vbaCastObj(var_008D9170, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A1630, var_20, __vbaCastObj(var_008D90BC, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A14B8, var_20, __vbaCastObj(var_008D9094, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A2ED8, var_20, __vbaCastObj(var_008D9120, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A2F9C, var_20, __vbaCastObj(var_008D9134, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A2CDC, var_20, __vbaCastObj(var_008D90F8, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A5930, var_20, __vbaCastObj(var_008D9148, var_004B4954)))))))))))))))
  loc_008C1C51: var_eax = ProcessFormText.Proc_19_1_8C1EF0(Me, var_20)
  loc_008C1C60: call __vbaCastObj(var_20, var_0049C038, var_20, __vbaCastObj(var_008D9020, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A59D0, var_20, __vbaCastObj(var_008D9170, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A1630, var_20, __vbaCastObj(var_008D90BC, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A14B8, var_20, __vbaCastObj(var_008D9094, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A2ED8, var_20, __vbaCastObj(var_008D9120, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A2F9C, var_20, __vbaCastObj(var_008D9134, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A2CDC, var_20, __vbaCastObj(var_008D90F8, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A5930, var_20, __vbaCastObj(var_008D9148, var_004B4954))))))))))))))))
  loc_008C1C97: call __vbaCastObj(var_008D90A8, var_004B4954, vbNullString, __vbaCastObj(var_20, var_0049C038, var_20, __vbaCastObj(var_008D9020, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A59D0, var_20, __vbaCastObj(var_008D9170, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A1630, var_20, __vbaCastObj(var_008D90BC, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A14B8, var_20, __vbaCastObj(var_008D9094, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A2ED8, var_20, __vbaCastObj(var_008D9120, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A2F9C, var_20, __vbaCastObj(var_008D9134, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A2CDC, var_20, __vbaCastObj(var_008D90F8, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A5930, var_20, __vbaCastObj(var_008D9148, var_004B4954)))))))))))))))))
  loc_008C1CA7: var_eax = ProcessFormText.Proc_19_1_8C1EF0(Me, var_20)
  loc_008C1CB6: call __vbaCastObj(var_20, var_004A15D4, var_20, __vbaCastObj(var_008D90A8, var_004B4954, vbNullString, __vbaCastObj(var_20, var_0049C038, var_20, __vbaCastObj(var_008D9020, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A59D0, var_20, __vbaCastObj(var_008D9170, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A1630, var_20, __vbaCastObj(var_008D90BC, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A14B8, var_20, __vbaCastObj(var_008D9094, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A2ED8, var_20, __vbaCastObj(var_008D9120, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A2F9C, var_20, __vbaCastObj(var_008D9134, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A2CDC, var_20, __vbaCastObj(var_008D90F8, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A5930, var_20, __vbaCastObj(var_008D9148, var_004B4954))))))))))))))))))
  loc_008C1CEE: call __vbaCastObj(var_008D9094, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A15D4, var_20, __vbaCastObj(var_008D90A8, var_004B4954, vbNullString, __vbaCastObj(var_20, var_0049C038, var_20, __vbaCastObj(var_008D9020, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A59D0, var_20, __vbaCastObj(var_008D9170, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A1630, var_20, __vbaCastObj(var_008D90BC, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A14B8, var_20, __vbaCastObj(var_008D9094, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A2ED8, var_20, __vbaCastObj(var_008D9120, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A2F9C, var_20, __vbaCastObj(var_008D9134, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A2CDC, var_20, __vbaCastObj(var_008D90F8, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A5930, var_20, __vbaCastObj(var_008D9148, var_004B4954)))))))))))))))))))
  loc_008C1CFE: var_eax = ProcessFormText.Proc_19_1_8C1EF0(Me, var_20)
  loc_008C1D0D: call __vbaCastObj(var_20, var_004A14B8, var_20, __vbaCastObj(var_008D9094, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A15D4, var_20, __vbaCastObj(var_008D90A8, var_004B4954, vbNullString, __vbaCastObj(var_20, var_0049C038, var_20, __vbaCastObj(var_008D9020, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A59D0, var_20, __vbaCastObj(var_008D9170, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A1630, var_20, __vbaCastObj(var_008D90BC, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A14B8, var_20, __vbaCastObj(var_008D9094, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A2ED8, var_20, __vbaCastObj(var_008D9120, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A2F9C, var_20, __vbaCastObj(var_008D9134, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A2CDC, var_20, __vbaCastObj(var_008D90F8, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A5930, var_20, __vbaCastObj(var_008D9148, var_004B4954))))))))))))))))))))
  loc_008C1D45: call __vbaCastObj(var_008D9044, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A14B8, var_20, __vbaCastObj(var_008D9094, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A15D4, var_20, __vbaCastObj(var_008D90A8, var_004B4954, vbNullString, __vbaCastObj(var_20, var_0049C038, var_20, __vbaCastObj(var_008D9020, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A59D0, var_20, __vbaCastObj(var_008D9170, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A1630, var_20, __vbaCastObj(var_008D90BC, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A14B8, var_20, __vbaCastObj(var_008D9094, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A2ED8, var_20, __vbaCastObj(var_008D9120, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A2F9C, var_20, __vbaCastObj(var_008D9134, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A2CDC, var_20, __vbaCastObj(var_008D90F8, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A5930, var_20, __vbaCastObj(var_008D9148, var_004B4954)))))))))))))))))))))
  loc_008C1D55: var_eax = ProcessFormText.Proc_19_1_8C1EF0(Me, var_20)
  loc_008C1D64: call __vbaCastObj(var_20, var_004A0ED8, var_20, __vbaCastObj(var_008D9044, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A14B8, var_20, __vbaCastObj(var_008D9094, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A15D4, var_20, __vbaCastObj(var_008D90A8, var_004B4954, vbNullString, __vbaCastObj(var_20, var_0049C038, var_20, __vbaCastObj(var_008D9020, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A59D0, var_20, __vbaCastObj(var_008D9170, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A1630, var_20, __vbaCastObj(var_008D90BC, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A14B8, var_20, __vbaCastObj(var_008D9094, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A2ED8, var_20, __vbaCastObj(var_008D9120, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A2F9C, var_20, __vbaCastObj(var_008D9134, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A2CDC, var_20, __vbaCastObj(var_008D90F8, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A5930, var_20, __vbaCastObj(var_008D9148, var_004B4954))))))))))))))))))))))
  loc_008C1D9B: call __vbaCastObj(var_008D90E4, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A0ED8, var_20, __vbaCastObj(var_008D9044, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A14B8, var_20, __vbaCastObj(var_008D9094, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A15D4, var_20, __vbaCastObj(var_008D90A8, var_004B4954, vbNullString, __vbaCastObj(var_20, var_0049C038, var_20, __vbaCastObj(var_008D9020, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A59D0, var_20, __vbaCastObj(var_008D9170, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A1630, var_20, __vbaCastObj(var_008D90BC, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A14B8, var_20, __vbaCastObj(var_008D9094, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A2ED8, var_20, __vbaCastObj(var_008D9120, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A2F9C, var_20, __vbaCastObj(var_008D9134, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A2CDC, var_20, __vbaCastObj(var_008D90F8, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A5930, var_20, __vbaCastObj(var_008D9148, var_004B4954)))))))))))))))))))))))
  loc_008C1DAB: var_eax = ProcessFormText.Proc_19_1_8C1EF0(Me, var_20)
  loc_008C1DBA: call __vbaCastObj(var_20, var_004A2A80, var_20, __vbaCastObj(var_008D90E4, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A0ED8, var_20, __vbaCastObj(var_008D9044, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A14B8, var_20, __vbaCastObj(var_008D9094, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A15D4, var_20, __vbaCastObj(var_008D90A8, var_004B4954, vbNullString, __vbaCastObj(var_20, var_0049C038, var_20, __vbaCastObj(var_008D9020, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A59D0, var_20, __vbaCastObj(var_008D9170, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A1630, var_20, __vbaCastObj(var_008D90BC, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A14B8, var_20, __vbaCastObj(var_008D9094, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A2ED8, var_20, __vbaCastObj(var_008D9120, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A2F9C, var_20, __vbaCastObj(var_008D9134, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A2CDC, var_20, __vbaCastObj(var_008D90F8, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A5930, var_20, __vbaCastObj(var_008D9148, var_004B4954))))))))))))))))))))))))
  loc_008C1DF2: call __vbaCastObj(var_008D90D0, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A2A80, var_20, __vbaCastObj(var_008D90E4, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A0ED8, var_20, __vbaCastObj(var_008D9044, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A14B8, var_20, __vbaCastObj(var_008D9094, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A15D4, var_20, __vbaCastObj(var_008D90A8, var_004B4954, vbNullString, __vbaCastObj(var_20, var_0049C038, var_20, __vbaCastObj(var_008D9020, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A59D0, var_20, __vbaCastObj(var_008D9170, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A1630, var_20, __vbaCastObj(var_008D90BC, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A14B8, var_20, __vbaCastObj(var_008D9094, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A2ED8, var_20, __vbaCastObj(var_008D9120, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A2F9C, var_20, __vbaCastObj(var_008D9134, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A2CDC, var_20, __vbaCastObj(var_008D90F8, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A5930, var_20, __vbaCastObj(var_008D9148, var_004B4954)))))))))))))))))))))))))
  loc_008C1E02: var_eax = ProcessFormText.Proc_19_1_8C1EF0(Me, var_20)
  loc_008C1E11: call __vbaCastObj(var_20, var_004A1778, var_20, __vbaCastObj(var_008D90D0, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A2A80, var_20, __vbaCastObj(var_008D90E4, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A0ED8, var_20, __vbaCastObj(var_008D9044, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A14B8, var_20, __vbaCastObj(var_008D9094, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A15D4, var_20, __vbaCastObj(var_008D90A8, var_004B4954, vbNullString, __vbaCastObj(var_20, var_0049C038, var_20, __vbaCastObj(var_008D9020, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A59D0, var_20, __vbaCastObj(var_008D9170, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A1630, var_20, __vbaCastObj(var_008D90BC, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A14B8, var_20, __vbaCastObj(var_008D9094, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A2ED8, var_20, __vbaCastObj(var_008D9120, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A2F9C, var_20, __vbaCastObj(var_008D9134, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A2CDC, var_20, __vbaCastObj(var_008D90F8, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A5930, var_20, __vbaCastObj(var_008D9148, var_004B4954))))))))))))))))))))))))))
  loc_008C1E49: call __vbaCastObj(var_008D9184, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A1778, var_20, __vbaCastObj(var_008D90D0, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A2A80, var_20, __vbaCastObj(var_008D90E4, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A0ED8, var_20, __vbaCastObj(var_008D9044, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A14B8, var_20, __vbaCastObj(var_008D9094, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A15D4, var_20, __vbaCastObj(var_008D90A8, var_004B4954, vbNullString, __vbaCastObj(var_20, var_0049C038, var_20, __vbaCastObj(var_008D9020, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A59D0, var_20, __vbaCastObj(var_008D9170, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A1630, var_20, __vbaCastObj(var_008D90BC, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A14B8, var_20, __vbaCastObj(var_008D9094, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A2ED8, var_20, __vbaCastObj(var_008D9120, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A2F9C, var_20, __vbaCastObj(var_008D9134, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A2CDC, var_20, __vbaCastObj(var_008D90F8, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A5930, var_20, __vbaCastObj(var_008D9148, var_004B4954)))))))))))))))))))))))))))
  loc_008C1E59: var_eax = ProcessFormText.Proc_19_1_8C1EF0(Me, var_20)
  loc_008C1E68: call __vbaCastObj(var_20, var_004A5A54, var_20, __vbaCastObj(var_008D9184, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A1778, var_20, __vbaCastObj(var_008D90D0, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A2A80, var_20, __vbaCastObj(var_008D90E4, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A0ED8, var_20, __vbaCastObj(var_008D9044, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A14B8, var_20, __vbaCastObj(var_008D9094, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A15D4, var_20, __vbaCastObj(var_008D90A8, var_004B4954, vbNullString, __vbaCastObj(var_20, var_0049C038, var_20, __vbaCastObj(var_008D9020, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A59D0, var_20, __vbaCastObj(var_008D9170, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A1630, var_20, __vbaCastObj(var_008D90BC, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A14B8, var_20, __vbaCastObj(var_008D9094, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A2ED8, var_20, __vbaCastObj(var_008D9120, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A2F9C, var_20, __vbaCastObj(var_008D9134, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A2CDC, var_20, __vbaCastObj(var_008D90F8, var_004B4954, vbNullString, __vbaCastObj(var_20, var_004A5930, var_20, __vbaCastObj(var_008D9148, var_004B4954))))))))))))))))))))))))))))
  loc_008C1E7F: Close #var_18
  loc_008C1E85: 'Referenced from: 008C18FD
  loc_008C1E9C: ReDim var_50(0 To 0)
  loc_008C1EA5: 
  loc_008C1EB1: GoTo loc_008C1ECF
  loc_008C1ECE: Exit Sub
  loc_008C1ECF: 'Referenced from: 008C1EB1
End Sub

Public Sub Proc_19_1_8C1EF0
  Dim var_B4 As Me
  loc_008C1F3E: On Error Resume Next
  loc_008C1F50: var_B4 = arg_C
  loc_008C1F69: var_3C = var_B4.Controls
  loc_008C1F71: var_B8 = var_3C
  loc_008C1FE5: For Each var_2C In GUID(var_004AC744)
  loc_008C1FEB: var_D8 = For Each var_2C In GUID(var_004AC744)
  loc_008C1FF1: GoTo loc_008C24BA
  loc_008C1FF6: 
  loc_008C2011: var_eax = ProcessFormText.Proc_19_3_8C2BE0(Me, var_2C, var_B0, var_3C, var_BC)
  loc_008C201D: var_30 = var_B0
  loc_008C204B: var_24 = var_2C."Name"
  loc_008C2065: If var_30 < 0 Then GoTo loc_008C212C
  loc_008C2081: var_64 = var_24 & var_004A1040
  loc_008C208E: var_94 = var_30
  loc_008C2104: var_24 = var_24 & var_004A1040 & Trim(Str(var_30)) & &H4A1054
  loc_008C212C: 'Referenced from: 008C2065
  loc_008C213C: call __vbaCheckType(var_2C, var_004A0D48, 0, ebx)
  loc_008C2147: If __vbaCheckType(var_2C, var_004A0D48, 0, ebx) = 0 Then GoTo loc_008C223C
  loc_008C2190: var_38 = var_2C."Text"
  loc_008C2196: var_94 = "Control: "
  loc_008C21AA: var_A4 = ".Text"
  loc_008C2208: var_eax = ProcessFormText.Proc_19_4_8C2CE0(Me, "Control: " & var_2C."Name" & ".Text")
  loc_008C223C: 'Referenced from: 008C2147
  loc_008C224C: call __vbaCheckType(var_2C, var_004A12E4)
  loc_008C2259: esi = __vbaCheckType(var_2C, var_004A12E4) + 1
  loc_008C2263: call __vbaCheckType(var_2C, var_004A0EC4)
  loc_008C2270: ecx = __vbaCheckType(var_2C, var_004A0EC4) + 1
  loc_008C2282: call __vbaCheckType(var_2C, var_004A0FC4)
  loc_008C228F: eax = __vbaCheckType(var_2C, var_004A0FC4) + 1
  loc_008C22A1: call __vbaCheckType(var_2C, var_004A1458)
  loc_008C22AE: edx = __vbaCheckType(var_2C, var_004A1458) + 1
  loc_008C22B3: If __vbaCheckType(var_2C, var_004A1458) + 1 <> 0 Then GoTo loc_008C23A8
  loc_008C22FC: var_38 = var_2C."Caption"
  loc_008C2302: var_94 = "Control: "
  loc_008C2316: var_A4 = ".Caption"
  loc_008C2374: var_eax = ProcessFormText.Proc_19_4_8C2CE0(Me, "Control: " & var_2C."Name" & ".Caption")
  loc_008C23A8: 'Referenced from: 008C22B3
  loc_008C23EB: var_38 = var_2C."ToolTipText"
  loc_008C23F1: var_94 = "Control: "
  loc_008C2405: var_A4 = ".ToolTipText"
  loc_008C2463: var_eax = ProcessFormText.Proc_19_4_8C2CE0(Me, "Control: " & var_2C."Name" & ".ToolTipText")
  loc_008C24AE: Next var_2C
  loc_008C24B4: var_D8 = Next var_2C
  loc_008C24BA: 'Referenced from: 008C1FF1
  loc_008C24C1: If var_D8 <> 0 Then GoTo loc_008C1FF6
  loc_008C24D4: Method_8964E04D
  loc_008C24D9: GoTo loc_008C251A
  loc_008C2519: Exit Sub
  loc_008C251A: 'Referenced from: 008C24D9
End Sub

Public Sub Proc_19_2_8C2570
  Dim var_B4 As Me
  loc_008C25BE: On Error Resume Next
  loc_008C25D0: var_B4 = arg_C
  loc_008C25E9: var_3C = var_B4.Controls
  loc_008C25F1: var_B8 = var_3C
  loc_008C2665: For Each var_2C In GUID(var_004AC744)
  loc_008C266B: var_D8 = For Each var_2C In GUID(var_004AC744)
  loc_008C2671: GoTo loc_008C2B2E
  loc_008C2676: 
  loc_008C2691: var_eax = ProcessFormText.Proc_19_3_8C2BE0(Me, var_2C, var_B0, var_3C, var_BC)
  loc_008C269D: var_30 = var_B0
  loc_008C26CB: var_24 = var_2C."Name"
  loc_008C26E5: If var_30 < 0 Then GoTo loc_008C27AC
  loc_008C2701: var_64 = var_24 & var_004A1040
  loc_008C270E: var_94 = var_30
  loc_008C2784: var_24 = var_24 & var_004A1040 & Trim(Str(var_30)) & &H4A1054
  loc_008C27AC: 'Referenced from: 008C26E5
  loc_008C27BC: call __vbaCheckType(var_2C, var_004A0D48, 0, ebx)
  loc_008C27C7: If __vbaCheckType(var_2C, var_004A0D48, 0, ebx) = 0 Then GoTo loc_008C28B8
  loc_008C280F: var_eax = ProcessFormText.Proc_19_5_8C2F90(Me, var_2C."Text")
  loc_008C282E: var_28 = var_38
  loc_008C285E: If (var_28 = vbNullString) = 0 Then GoTo loc_008C28B8
  loc_008C286A: var_94 = var_28
  loc_008C28B2: var_2C."Text" = var_90
  loc_008C28B8: 'Referenced from: 008C27C7
  loc_008C28C8: call __vbaCheckType(var_2C, var_004A12E4, var_90, var_38)
  loc_008C28D5: esi = __vbaCheckType(var_2C, var_004A12E4, var_90, var_38) + 1
  loc_008C28DF: call __vbaCheckType(var_2C, var_004A0EC4)
  loc_008C28EC: ecx = __vbaCheckType(var_2C, var_004A0EC4) + 1
  loc_008C28FE: call __vbaCheckType(var_2C, var_004A0FC4)
  loc_008C290B: eax = __vbaCheckType(var_2C, var_004A0FC4) + 1
  loc_008C291D: call __vbaCheckType(var_2C, var_004A1458)
  loc_008C292A: edx = __vbaCheckType(var_2C, var_004A1458) + 1
  loc_008C292F: If __vbaCheckType(var_2C, var_004A1458) + 1 <> 0 Then GoTo loc_008C2A20
  loc_008C2977: var_eax = ProcessFormText.Proc_19_5_8C2F90(Me, var_2C."Caption")
  loc_008C2996: var_28 = var_38
  loc_008C29C6: If (var_28 = vbNullString) = 0 Then GoTo loc_008C2A20
  loc_008C29D2: var_94 = var_28
  loc_008C2A1A: var_2C."Caption" = var_90
  loc_008C2A20: 'Referenced from: 008C292F
  loc_008C2A62: var_eax = ProcessFormText.Proc_19_5_8C2F90(Me, var_2C."ToolTipText")
  loc_008C2A81: var_28 = var_38
  loc_008C2AB1: If (var_28 = vbNullString) = 0 Then GoTo loc_008C2B0B
  loc_008C2ABD: var_94 = var_28
  loc_008C2B05: var_2C."ToolTipText" = var_24
  loc_008C2B0B: 'Referenced from: 008C2AB1
  loc_008C2B22: Next var_2C
  loc_008C2B28: var_D8 = Next var_2C
  loc_008C2B2E: 'Referenced from: 008C2671
  loc_008C2B35: If var_D8 <> 0 Then GoTo loc_008C2676
  loc_008C2B48: Method_8964E04D
  loc_008C2B4D: GoTo loc_008C2B8E
  loc_008C2B8D: Exit Sub
  loc_008C2B8E: 'Referenced from: 008C2B4D
End Sub

Public Sub Proc_19_3_8C2BE0
  Dim var_28 As Me
  loc_008C2C19: On Error Resume Next
  loc_008C2C4A: var_24 = CLng(arg_C."Index")
  loc_008C2C4D: Exit Sub
  loc_008C2C58: GoTo loc_008C2CBB
  loc_008C2C7A: var_28.PropBag.WriteProperty(0, var_28, Err)
  loc_008C2C9B: Exit Sub
  loc_008C2CA6: GoTo loc_008C2CBB
  loc_008C2CBA: Exit Sub
  loc_008C2CBB: 'Referenced from: 008C2C58
End Sub

Public Sub Proc_19_4_8C2CE0
  loc_008C2D40: var_14 = eax
  loc_008C2D4A: var_20 = ecx
  loc_008C2D58: var_6C = var_20
  loc_008C2D75: var_44 = Mid(var_20, 1, 1)
  loc_008C2DBB: If (var_44 <> &H4A1948) <> 0 Then GoTo loc_008C2F35
  loc_008C2DCE: If Len(var_20) <= 1 Then GoTo loc_008C2F35
  loc_008C2DE8: If InStr(1, var_20, ":\", 0) > 0 Then GoTo loc_008C2F35
  loc_008C2DF2: var_7C = var_20
  loc_008C2E25: var_54 = Trim(Str(Val(var_20)))
  loc_008C2E55: If (var_20 <> var_54) <> 0 Then GoTo loc_008C2F35
  loc_008C2E67: call UBound(00000001h, %ecx = %S_edx_S.GetPalette, @(%StkVar2 = %StkVar1))
  loc_008C2E6E: UBound(00000001h, %ecx = %S_edx_S.GetPalette, @(%StkVar2 = %StkVar1)) = UBound(00000001h, %ecx = %S_edx_S.GetPalette, @(%StkVar2 = %StkVar1)) + 00000001h
  loc_008C2E86: ReDim Preserve %ecx = %S_edx_S.GetPalette 'Ignore this = %S_edx_S.GetPalette(esi To UBound(00000001h, %ecx = %S_edx_S.GetPalette 'Ignore this = %S_edx_S.GetPalette, @(%StkVar2 = %StkVar1)))
  loc_008C2E93: If edi = 0 Then GoTo loc_008C2EB6
  loc_008C2E99: If edi <> 1 Then GoTo loc_008C2EB6
  loc_008C2EA1: UBound(00000001h, %ecx = %S_edx_S.GetPalette, @(%StkVar2 = %StkVar1)) = UBound(00000001h, %ecx = %S_edx_S.GetPalette, @(%StkVar2 = %StkVar1)) - %ecx = %S_edx_S.%x1 = GetIDsOfNames(%StkVar2) 'Ignore this
  loc_008C2EA5: If UBound(00000001h, %ecx = %S_edx_S.GetPalette, @(%StkVar2 = %StkVar1)) < 0 Then GoTo loc_008C2EAD
  loc_008C2EA7: var_eax = Err.Raise
  loc_008C2EAD: 'Referenced from: 008C2EA5
  loc_008C2EB4: GoTo loc_008C2EBC
  loc_008C2EB6: 'Referenced from: 008C2E93
  loc_008C2EB6: var_eax = Err.Raise
  loc_008C2EBC: 'Referenced from: 008C2EB4
  loc_008C2EC6: ecx = var_20
  loc_008C2EE5: Print ecx = %S_edx_S.Reset, var_14
  loc_008C2F11: Print ecx = %S_edx_S.Reset, "English: " & var_20
  loc_008C2F30: Print var_24 = %S_edx_S.Reset, "French : "
  loc_008C2F35: 'Referenced from: 008C2DBB
  loc_008C2F3B: GoTo loc_008C2F62
  loc_008C2F61: Exit Sub
  loc_008C2F62: 'Referenced from: 008C2F3B
End Sub

Public Sub Proc_19_5_8C2F90
  loc_008C2FD7: var_18 = vbNullString
  loc_008C2FF0: If (arg_C = vbNullString) = 0 Then GoTo loc_008C314B
  loc_008C2FFF: call UBound(00000002h, 0.SaveProp, esi, ebx)
  loc_008C3005: var_20 = UBound(00000002h, 0.SaveProp, esi, ebx)
  loc_008C3010: If 00000001h > 0 Then GoTo loc_008C314B
End Sub

Public Sub Proc_19_6_8C31B0
  loc_008C3211: var_20 = Dir("Language.txt", 0)
  loc_008C3229: edi = (var_20 = vbNullString) + 1
  loc_008C3243: If (var_20 <> vbNullString) + 1 <> 0 Then GoTo loc_008C34B0
  loc_008C3260: ReDim Me(esi To esi)
  loc_008C3286: var_18 = FreeFile(var_30)
  loc_008C3295: Open "Language.txt" For Input As #FreeFile(var_30) Len = -1
  loc_008C329D: 
  loc_008C32A8: If EOF(FreeFile(var_30)) <> 0 Then GoTo loc_008C34AA
  loc_008C32B2: Line Input #FreeFile(var_30), var_1C
  loc_008C32CC: If InStr(1, var_1C, "[EOF]", 0) = 0 Then GoTo loc_008C329D
  loc_008C32D7: ebx = ebx + 00000001h
End Sub

Public Sub Proc_19_7_8C3500
  loc_008C3568: var_20 = ebx
  loc_008C3581: var_DC = Len(arg_C)
  loc_008C3595: setge dl
  loc_008C35AF: If Len(arg_10) = 0 Then GoTo loc_008C3764
  loc_008C35BC: If Len(arg_10) = 0 Then GoTo loc_008C3764
  loc_008C35C2: 
  loc_008C35C9: If arg_18 <> var_FFFFFF Then GoTo loc_008C3643
  loc_008C35DC: var_88 = arg_C
  loc_008C35EC: var_30 = LCase(arg_C)
  loc_008C35F9: var_98 = arg_10
  loc_008C3609: var_40 = LCase(arg_10)
  loc_008C361B: call InStr(var_50, 00000000h, var_40, var_30, 00000001h, @Len(%StkVar1), arg_10, arg_C)
  loc_008C3622: var_ret_1 = CLng(InStr(var_50, 00000000h, var_40, var_30, 00000001h, @Len(%StkVar1), arg_10, arg_C))
  loc_008C3641: GoTo loc_008C3655
  loc_008C3643: 'Referenced from: 008C35C9
  loc_008C3655: 'Referenced from: 008C3641
  loc_008C3657: If InStr(1, arg_C, arg_10, 0) = 0 Then GoTo loc_008C3759
  loc_008C366E: InStr(1, arg_C, arg_10, 0) = InStr(1, arg_C, arg_10, 0) - 00000001h
  loc_008C367F: var_28 = InStr(1, arg_C, arg_10, 0)
  loc_008C368E: var_88 = arg_C
  loc_008C36AD: Len(arg_10) = Len(arg_10) + InStr(1, arg_C, arg_10, 0)
  loc_008C36BC: var_A8 = var_C0
  loc_008C36DF: var_B8 = arg_C
  loc_008C3722: ecx = Mid(arg_C, 1, InStr(1, arg_C, arg_10, 0)) & &H4008 & Mid(arg_C, Len(arg_10), 10)
  loc_008C3754: GoTo loc_008C35C2
  loc_008C3759: 'Referenced from: 008C3657
  loc_008C375E: var_20 = ebx
  loc_008C3764: 'Referenced from: 008C35AF
  loc_008C3769: GoTo loc_008C379E
  loc_008C376F: If var_4 = 0 Then GoTo loc_008C377A
  loc_008C377A: 'Referenced from: 008C376F
  loc_008C379D: Exit Sub
  loc_008C379E: 'Referenced from: 008C3769
End Sub
