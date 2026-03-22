VERSION 1.0 CLASS
BEGIN
  MultiUse = -1  'True
  Persistable = 0  'NotPersistable
  DataBindingBehavior = 0  'vbNone
  DataSourceBehavior = 0   'vbNone
  MTSTransactionMode = 0   'NotAnMTSObject
END
Attribute VB_Name = "clsRndCrypt"
Attribute VB_GlobalNameSpace = False
Attribute VB_Creatable = True
Attribute VB_PredeclaredId = False
Attribute VB_Exposed = False

Public Function RndCrypt(Str, Password) '8B8830
  loc_008B8894: var_20 = Str
  loc_008B889C: var_18 = Password
  loc_008B88D9: var_38 = Len(var_18)
  loc_008B88E3: Randomize(Len(var_18))
  loc_008B88FA: var_8C = Len(var_18)
  loc_008B890B: If 00000001h > 0 Then GoTo loc_008B89CD
  loc_008B894D: var_2C = Mid$(var_18, 1, 1)
  loc_008B897F: If Sign(-2147483393 - 0) Then GoTo loc_008B8989
  loc_008B8981: ecx = -2147483393 - 1
  loc_008B8988: ecx = -256 + 1
  loc_008B8989: 'Referenced from: 008B897F
  loc_008B898E: Asc(var_2C) xor eax xor ecx = Asc(var_2C) xor eax xor ecx + var_28
  loc_008B8999: var_28 = Asc(var_2C) xor eax xor ecx
  loc_008B89BE: 00000001h = 00000001h + 00000001h
  loc_008B89C8: GoTo loc_008B8905
  loc_008B89CD: 'Referenced from: 008B890B
  loc_008B89F3: var_68 = var_28
  loc_008B89FD: Randomize(var_28)
  loc_008B8A0D: var_94 = Len(var_20)
  loc_008B8A1E: If 00000001h > 0 Then GoTo loc_008B8AF5
  loc_008B8A60: var_2C = Mid$(var_20, 1, 1)
  loc_008B8AB0: call __vbaMidStmtBstr(00000000h, Chr(Asc(var_2C) xor eax), 00000001h, 00000001h, var_20, Asc(var_2C))
  loc_008B8AE6: 00000001h = 00000001h + 00000001h
  loc_008B8AF0: GoTo loc_008B8A18
  loc_008B8AF5: 'Referenced from: 008B8A1E
  loc_008B8AFB: var_24 = var_20
  loc_008B8B07: GoTo loc_008B8B40
  loc_008B8B0D: If var_4 = 0 Then GoTo loc_008B8B18
  loc_008B8B18: 'Referenced from: 008B8B0D
  loc_008B8B3F: Exit Sub
  loc_008B8B40: 'Referenced from: 008B8B07
  loc_008B8B50: Exit Sub
End Function

Public Function RndCryptB(tmpToEncrypt, tmpPassword) '8B8B90
  loc_008B8BE5: var_2C = tmpPassword
  loc_008B8C23: var_34 = Len(var_2C)
  loc_008B8C2D: Randomize(Len(var_2C))
  loc_008B8C49: var_48 = var_2C
  loc_008B8C80: call UBound(00000001h, StrConv(var_2C, 128, 0), 0, undef 'Ignore this '__vbaFreeVar, ebx)
  loc_008B8C96: If ebx > 0 Then GoTo loc_008B8D44
  loc_008B8CBC: If var_20 = 0 Then GoTo loc_008B8CD8
  loc_008B8CC2: If var_20 <> 1 Then GoTo loc_008B8CD8
  loc_008B8CCE: If ebx < 0 Then GoTo loc_008B8CE0
  loc_008B8CD0: var_eax = Err.Raise
  loc_008B8CD6: GoTo loc_008B8CE0
  loc_008B8CD8: 'Referenced from: 008B8CBC
  loc_008B8CD8: var_eax = Err.Raise
  loc_008B8CE0: 'Referenced from: 008B8CD6
  loc_008B8D13: If Sign(-2147483393 - 0) Then GoTo loc_008B8D1D
  loc_008B8D15: edx = -2147483393 - 1
  loc_008B8D1C: edx = -256 + 1
  loc_008B8D1D: 'Referenced from: 008B8D13
  loc_008B8D1F: CLng() xor ecx xor edx = CLng() xor ecx xor edx + var_28
  loc_008B8D2B: var_28 = CLng() xor ecx xor edx
  loc_008B8D35: 00000001h = 00000001h + var_18
  loc_008B8D3F: GoTo loc_008B8C91
  loc_008B8D44: 'Referenced from: 008B8C96
  loc_008B8D6A: var_48 = var_28
  loc_008B8D74: Randomize(var_28)
  loc_008B8D8C: var_48 = tmpToEncrypt
  loc_008B8DB2: var_24 = StrConv(tmpToEncrypt, 128, 0)
  loc_008B8DC3: call UBound(00000001h, var_24)
  loc_008B8DC9: UBound(00000001h, var_24) = UBound(00000001h, var_24) - 00000001h
  loc_008B8DD2: var_6C = UBound(00000001h, var_24)
  loc_008B8DD9: If edi > 0 Then GoTo loc_008B8EB4
  loc_008B8DFF: If var_24 = 0 Then GoTo loc_008B8E23
  loc_008B8E05: If var_24 <> 1 Then GoTo loc_008B8E23
  loc_008B8E13: If edi < 0 Then GoTo loc_008B8E1E
  loc_008B8E15: var_eax = Err.Raise
  loc_008B8E1E: 'Referenced from: 008B8E13
  loc_008B8E1E: var_78 = edi-Me.%x1 = GetIDsOfNames(%StkVar2) 'Ignore this
  loc_008B8E21: GoTo loc_008B8E2F
  loc_008B8E23: 'Referenced from: 008B8DFF
  loc_008B8E23: var_eax = Err.Raise
  loc_008B8E29: var_78 = Err.Raise
  loc_008B8E2F: 'Referenced from: 008B8E21
  loc_008B8E31: If var_24 = 0 Then GoTo loc_008B8E52
  loc_008B8E37: If var_24 <> 1 Then GoTo loc_008B8E52
  loc_008B8E45: If ebx < 0 Then GoTo loc_008B8E4D
  loc_008B8E47: var_eax = Err.Raise
  loc_008B8E4D: 'Referenced from: 008B8E45
  loc_008B8E4D: var_7C = edi-Me.%x1 = GetIDsOfNames(%StkVar2) 'Ignore this
  loc_008B8E50: GoTo loc_008B8E5B
  loc_008B8E52: 'Referenced from: 008B8E31
  loc_008B8E52: var_eax = Err.Raise
  loc_008B8E58: var_7C = Err.Raise
  loc_008B8E5B: 'Referenced from: 008B8E50
  loc_008B8EA2: 00000001h = 00000001h + edi-Me.%x1 = GetIDsOfNames(%StkVar2) 'Ignore this
  loc_008B8EAF: GoTo loc_008B8DD7
  loc_008B8EB4: 'Referenced from: 008B8DD9
  loc_008B8EC3: var_48 = var_24
  loc_008B8EE2: var_1C = StrConv(var_24, 64, 0)
  loc_008B8EF3: GoTo loc_008B8F1A
  loc_008B8EF9: If var_4 = 0 Then GoTo loc_008B8F04
  loc_008B8F04: 'Referenced from: 008B8EF9
  loc_008B8F19: Exit Sub
  loc_008B8F1A: 'Referenced from: 008B8EF3
  loc_008B8F39: Exit Sub
End Function

Public Function RndCryptLevel2(strValue, strPassword, bSeedPasses, bDataPasses, InputType, OutputType, RemoveInvalidChars) '8B8F70
  loc_008B8FF5: If Len(strValue) = 0 Then GoTo loc_008B9BBC
  loc_008B9005: If Len(strPassword) <> 0 Then GoTo loc_008B9034
  loc_008B9012: var_5C = Chr(0)
  loc_008B9023: ecx = var_5C
  loc_008B9032: GoTo loc_008B903A
  loc_008B9034: 'Referenced from: 008B9005
  loc_008B903A: 'Referenced from: 008B9032
  loc_008B9040: If bSeedPasses <> 0 Then GoTo loc_008B9052
  loc_008B9052: 'Referenced from: 008B9040
  loc_008B9057: eax = edx - 1
  loc_008B9058: If edx - 1 = 0 Then GoTo loc_008B93B1
  loc_008B905E: eax = edx - 1 - 1
  loc_008B905F: If edx - 1 - 1 = 0 Then GoTo loc_008B90A8
  loc_008B9061: eax = edx - 1 - 1 - 1
  loc_008B9062: If edx - 1 - 1 - 1 <> 0 Then GoTo loc_008B93FD
  loc_008B909D: var_38 = var_100
  loc_008B90A3: GoTo loc_008B93FD
  loc_008B90A8: 'Referenced from: 008B905F
  loc_008B90AE: If RemoveInvalidChars = 0 Then GoTo loc_008B91F5
  loc_008B90B6: 
  loc_008B90BD: If edi > 255 Then GoTo loc_008B91F5
  loc_008B90C8: call Me(var_5C, edi, strValue, RemoveInvalidChars, var_100, 0, strValue, Me)
  loc_008B90D9: var_88 = "[0-9A-F]"
  loc_008B90ED: call __vbaVarLikeVar(var_6C, var_90, var_5C)
  loc_008B90F8: call Not(var_7C, __vbaVarLikeVar(var_6C, var_90, var_5C))
  loc_008B9108: var_AC = CBool(Not(var_7C, __vbaVarLikeVar(var_6C, var_90, var_5C)))
  loc_008B911D: If var_AC = 0 Then GoTo loc_008B91E1
  loc_008B912A: var_88 = strValue
  loc_008B913A: call Me(var_5C, edi)
  loc_008B9163: call InStr(var_6C, 00000000h, var_5C, var_90, 00000001h)
  loc_008B9181: var_AC = (InStr(var_6C, 00000000h, var_5C, var_90, 00000001h) > 0)
  loc_008B9199: If var_AC = 0 Then GoTo loc_008B91E1
  loc_008B91A0: call Me(var_5C, edi)
  loc_008B91C9: ecx = Replace(strValue, CStr(var_5C), vbNullString, 1, -1, 0)
  loc_008B91E1: 'Referenced from: 008B911D
  loc_008B91E6: 00000001h = 00000001h + edi
  loc_008B91F0: GoTo loc_008B90B6
  loc_008B91F5: 'Referenced from: 008B90AE
  loc_008B9207: var_88 = strValue
  loc_008B9225: var_80 = StrConv(strValue, 128, 0)
  loc_008B9233: var_2C = var_80
  loc_008B924D: cdq
  loc_008B924E: Len(strValue) = Len(strValue) - var_80
  loc_008B9250: sar eax, 01h
  loc_008B9252: Len(strValue) = Len(strValue) - 00000001h
  loc_008B926B: ReDim var_38(0 To Len(strValue))
  loc_008B927A: call UBound(00000001h, var_38)
  loc_008B9280: var_D0 = UBound(00000001h, var_38)
  loc_008B9294: If ebx > 0 Then GoTo loc_008B93A3
  loc_008B929F: If var_2C = 0 Then GoTo loc_008B92C2
  loc_008B92A5: If var_2C <> 1 Then GoTo loc_008B92C2
  loc_008B92AF: ebx = ebx * 2
  loc_008B92BC: If ebx < 0 Then GoTo loc_008B92C9
  loc_008B92BE: var_eax = Err.Raise
  loc_008B92C0: GoTo loc_008B92C6
  loc_008B92C2: 'Referenced from: 008B929F
  loc_008B92C2: var_eax = Err.Raise
  loc_008B92C6: 'Referenced from: 008B92C0
  loc_008B92C9: 
End Function

Public Function Base64EncodingB(StrIn) '8B9C70
  loc_008B9CC6: call UBound(00000001h, StrIn, edi, UBound, StrIn)
  loc_008B9CCB: If UBound(00000001h, StrIn, edi, UBound, StrIn) = 0 Then GoTo loc_008BA442
  loc_008B9CD6: call UBound(00000001h, StrIn)
  loc_008B9CDA: UBound(00000001h, StrIn) = UBound(00000001h, StrIn) + 00000001h
  loc_008B9CE5: cdq
  loc_008B9CEB: idiv ecx
  loc_008B9CED: var_1C = UBound(00000001h, StrIn)
  loc_008B9CF7: If arg_10 <> 0 Then GoTo loc_008B9D0A
  loc_008B9CF9: UBound(00000001h, StrIn) = UBound(00000001h, StrIn) * 
  loc_008B9D00: arg_10 = arg_10 + arg_10
  loc_008B9D08: GoTo loc_008B9D22
  loc_008B9D0A: 'Referenced from: 008B9CF7
  loc_008B9D0A: UBound(00000001h, StrIn) = UBound(00000001h, StrIn) * 
  loc_008B9D11: arg_10 = arg_10 + arg_10
  loc_008B9D17: arg_10 = arg_10 + 00000001h
  loc_008B9D22: 'Referenced from: 008B9D08
  loc_008B9D22: arg_10 = arg_10 * 4
  loc_008B9D2B: arg_10 = arg_10 - 00000001h
  loc_008B9D44: ReDim var_24(0 To arg_10)
  loc_008B9D4D: arg_10 = arg_10 - 00000001h
  loc_008B9D56: var_5C = arg_10
  loc_008B9D66: If var_20 > 0 Then GoTo loc_008B9FFA
  loc_008B9D70: If StrIn = 0 Then GoTo loc_008B9DA1
  loc_008B9D76: If ebx <> 1 Then GoTo loc_008B9DA1
  loc_008B9D7B: var_20 = var_20 * 3
  loc_008B9D87: var_20 = var_20 + 00000002h
  loc_008B9D94: If var_20 < 0 Then GoTo loc_008B9D9C
  loc_008B9D96: var_eax = Err.Raise
  loc_008B9D9C: 'Referenced from: 008B9D94
  loc_008B9D9C: var_68 = var_20
  loc_008B9D9F: GoTo loc_008B9DAA
  loc_008B9DA1: 'Referenced from: 008B9D70
  loc_008B9DA1: var_eax = Err.Raise
  loc_008B9DA7: var_68 = Err.Raise
  loc_008B9DAA: 'Referenced from: 008B9D9F
  loc_008B9DAE: If StrIn = 0 Then GoTo loc_008B9DE1
  loc_008B9DB4: If ebx <> 1 Then GoTo loc_008B9DE1
  loc_008B9DBC: var_20 = var_20 * 3
  loc_008B9DC8: var_20 = var_20 + 00000001h
  loc_008B9DD5: If var_20 < 0 Then GoTo loc_008B9DDD
  loc_008B9DD7: var_eax = Err.Raise
  loc_008B9DDD: 'Referenced from: 008B9DD5
  loc_008B9DDF: GoTo loc_008B9DE9
  loc_008B9DE1: 'Referenced from: 008B9DAE
  loc_008B9DE1: var_eax = Err.Raise
  loc_008B9DE9: 'Referenced from: 008B9DDF
  loc_008B9DED: If StrIn = 0 Then GoTo loc_008B9E17
  loc_008B9DF3: If ebx <> 1 Then GoTo loc_008B9E17
  loc_008B9DFB: var_20 = var_20 * 3
  loc_008B9E0B: If var_20 < 0 Then GoTo loc_008B9E13
  loc_008B9E0D: var_eax = Err.Raise
  loc_008B9E13: 'Referenced from: 008B9E0B
  loc_008B9E15: GoTo loc_008B9E1D
  loc_008B9E17: 'Referenced from: 008B9DED
  loc_008B9E17: var_eax = Err.Raise
  loc_008B9E1D: 'Referenced from: 008B9E15
  loc_008B9E29: ecx+eax = ecx+eax * 65536
  loc_008B9E3A: ecx+edi = ecx+edi * 256
  loc_008B9E54: sar edi, 12h
  loc_008B9E5D: If ecx+eax And 63 < 0 Then GoTo loc_008B9E65
  loc_008B9E5F: var_eax = Err.Raise
  loc_008B9E65: 'Referenced from: 008B9E5D
  loc_008B9E6A: If var_24 = 0 Then GoTo loc_008B9E97
  loc_008B9E70: If var_24 <> 1 Then GoTo loc_008B9E97
  loc_008B9E78: var_20 = var_20 * 4
  loc_008B9E88: If var_20 < 0 Then GoTo loc_008B9E93
  loc_008B9E8A: var_eax = Err.Raise
  loc_008B9E93: 'Referenced from: 008B9E88
  loc_008B9E95: GoTo loc_008B9EA0
  loc_008B9E97: 'Referenced from: 008B9E6A
  loc_008B9E97: var_eax = Err.Raise
  loc_008B9EA0: 'Referenced from: 008B9E95
End Function

Public Function Base64DecodingB(StrToDecode, RemoveInvalidChars) '8BA4A0
  loc_008BA504: var_24 = StrToDecode
  loc_008BA51B: If (var_24 = vbNullString) = 0 Then GoTo loc_008BAEA5
  loc_008BA52C: var_74 = var_24
  loc_008BA560: If RemoveInvalidChars = 0 Then GoTo loc_008BA68E
  loc_008BA56E: 
  loc_008BA575: If esi > 255 Then GoTo loc_008BA68E
  loc_008BA580: var_48 = Chr(0)
  loc_008BA58E: var_74 = "[A-Za-z0-9+/=]"
  loc_008BA59C: call __vbaVarLikeVar(var_58, var_7C, var_48, 0, esi, ebx)
  loc_008BA5A7: call Not(var_68, __vbaVarLikeVar(var_58, var_7C, var_48, 0, esi, ebx))
  loc_008BA5C3: If CBool(Not(var_68, __vbaVarLikeVar(var_58, var_7C, var_48, 0, esi, ebx))) = 0 Then GoTo loc_008BA678
  loc_008BA5D1: var_74 = Trim(var_24)
  loc_008BA5DB: var_48 = Chr(0)
  loc_008BA601: call InStr(var_58, 00000000h, var_48, var_7C, 00000001h)
  loc_008BA62E: If (InStr(var_58, 00000000h, var_48, var_7C, 00000001h) > 0) = 0 Then GoTo loc_008BA678
  loc_008BA660: var_24 = Replace(var_24, CStr(Chr(0)), vbNullString, 1, -1, 0)
  loc_008BA678: 'Referenced from: 008BA5C3
  loc_008BA67D: 00000001h = 00000001h + esi
  loc_008BA689: GoTo loc_008BA56E
  loc_008BA68E: 'Referenced from: 008BA560
  loc_008BA69F: var_74 = var_24
  loc_008BA6C5: var_20 = StrConv(var_24, 128, 0)
  loc_008BA6D9: If var_20 = 0 Then GoTo loc_008BA713
  loc_008BA6DF: If var_20 <> 1 Then GoTo loc_008BA713
  loc_008BA6E4: call UBound(00000001h, var_20)
  loc_008BA6FB: UBound(00000001h, var_20) = UBound(00000001h, var_20) - 00000001h
  loc_008BA708: If UBound(00000001h, var_20) < 0 Then GoTo loc_008BA70F
  loc_008BA70A: var_eax = Err.Raise
  loc_008BA70F: 'Referenced from: 008BA708
  loc_008BA711: GoTo loc_008BA71E
  loc_008BA713: 'Referenced from: 008BA6D9
  loc_008BA719: var_eax = Err.Raise
  loc_008BA71E: 'Referenced from: 008BA711
  loc_008BA725: If var_278 <> 61 Then GoTo loc_008BA75E
  loc_008BA735: cdq
  loc_008BA73B: sar eax, 02h
  loc_008BA73E: Len(var_24) = Len(var_24) * 3
  loc_008BA747: Len(var_24) = Len(var_24) - 00000003h
  loc_008BA759: GoTo loc_008BA7FA
  loc_008BA75E: 'Referenced from: 008BA725
  loc_008BA760: If var_2C = 0 Then GoTo loc_008BA788
  loc_008BA766: If var_2C <> 1 Then GoTo loc_008BA788
  loc_008BA76B: call UBound(00000001h, var_2C, var_2C, 00000011h, 00000001h, Len(var_24), 00000000h)
  loc_008BA780: If UBound(00000001h, var_2C, var_2C, 00000011h, 00000001h, Len(var_24), 00000000h) < 0 Then GoTo loc_008BA784
  loc_008BA782: var_eax = Err.Raise
  loc_008BA784: 'Referenced from: 008BA780
  loc_008BA786: GoTo loc_008BA78A
  loc_008BA788: 'Referenced from: 008BA760
  loc_008BA788: var_eax = Err.Raise
  loc_008BA78A: 'Referenced from: 008BA786
End Function

Public Sub Proc_14_5_8B83B0
  loc_008B8411: 
  loc_008B8418: If ecx > 25 Then GoTo loc_008B8455
  loc_008B841F: If ecx < 64 Then GoTo loc_008B842A
  loc_008B8421: var_eax = Err.Raise
  loc_008B842A: 'Referenced from: 008B841F
  loc_008B8444: 00000001h = 00000001h + var_18 + 00000041h
  loc_008B8453: GoTo loc_008B8411
  loc_008B8455: 'Referenced from: 008B8418
  loc_008B845F: 
  loc_008B8466: If ecx > 25 Then GoTo loc_008B84AC
  loc_008B846A: ecx = ecx + 0000001Ah
  loc_008B8476: If ecx < 64 Then GoTo loc_008B8481
  loc_008B8478: var_eax = Err.Raise
  loc_008B8481: 'Referenced from: 008B8476
  loc_008B849B: 00000001h = 00000001h + var_18 + 00000061h
  loc_008B84AA: GoTo loc_008B845F
  loc_008B84AC: 'Referenced from: 008B8466
  loc_008B84B3: 
  loc_008B84BD: If ecx > 9 Then GoTo loc_008B84FE
  loc_008B84C1: ecx = ecx + 00000034h
  loc_008B84CD: If ecx < 64 Then GoTo loc_008B84D8
  loc_008B84CF: var_eax = Err.Raise
  loc_008B84D8: 'Referenced from: 008B84CD
  loc_008B84F2: 00000001h = 00000001h + var_18 + 00000030h
  loc_008B84FC: GoTo loc_008B84B3
  loc_008B84FE: 'Referenced from: 008B84BD
  loc_008B8525: 
  loc_008B852F: If eax > 255 Then GoTo loc_008B8560
  loc_008B8538: If eax < 256 Then GoTo loc_008B8540
  loc_008B853A: var_eax = Err.Raise
  loc_008B8540: 'Referenced from: 008B8538
  loc_008B8556: 00000001h = 00000001h + var_18
  loc_008B855E: GoTo loc_008B8525
  loc_008B8560: 'Referenced from: 008B852F
  loc_008B8567: 
  loc_008B8571: If ecx > 25 Then GoTo loc_008B85AC
  loc_008B8575: ecx = ecx + 00000041h
  loc_008B8584: If ecx < 256 Then GoTo loc_008B858F
  loc_008B8586: var_eax = Err.Raise
  loc_008B85A0: 00000001h = 00000001h + var_18
  loc_008B85AA: GoTo loc_008B8567
  loc_008B85AC: 'Referenced from: 008B8571
  loc_008B85B6: 
  loc_008B85C0: If 0000001Ah > 51 Then GoTo loc_008B85FB
  loc_008B85C4: 0000001Ah = 0000001Ah + 00000047h
  loc_008B85D3: If 0000001Ah < 256 Then GoTo loc_008B85DE
  loc_008B85D5: var_eax = Err.Raise
  loc_008B85EF: 00000001h = 00000001h + &H1A
  loc_008B85F9: GoTo loc_008B85B6
  loc_008B85FB: 'Referenced from: 008B85C0
  loc_008B8605: 
  loc_008B860F: If 00000034h > 61 Then GoTo loc_008B864A
  loc_008B8613: 00000034h = 00000034h - 00000004h
  loc_008B8622: If 00000034h < 256 Then GoTo loc_008B862D
  loc_008B8624: var_eax = Err.Raise
  loc_008B863E: 00000001h = 00000001h + &H34
  loc_008B8648: GoTo loc_008B8605
  loc_008B864A: 'Referenced from: 008B860F
  loc_008B8674: 
  loc_008B867E: If 00000030h > 57 Then GoTo loc_008B86B9
  loc_008B8688: If 00000030h < 256 Then GoTo loc_008B8693
  loc_008B868A: var_eax = Err.Raise
  loc_008B8693: 'Referenced from: 008B8688
  loc_008B86AD: 00000001h = 00000001h + var_18 - 00000030h
  loc_008B86B7: GoTo loc_008B8674
  loc_008B86B9: 'Referenced from: 008B867E
  loc_008B86C3: 
  loc_008B86CD: If 00000041h > 70 Then GoTo loc_008B8708
  loc_008B86D7: If 00000041h < 256 Then GoTo loc_008B86E2
  loc_008B86D9: var_eax = Err.Raise
  loc_008B86E2: 'Referenced from: 008B86D7
  loc_008B86FC: 00000001h = 00000001h + var_18 - 00000037h
  loc_008B8706: GoTo loc_008B86C3
  loc_008B8708: 'Referenced from: 008B86CD
  loc_008B872D: If eax > 0 Then GoTo loc_008B87DE
  loc_008B873A: var_30 = var_18
  loc_008B874F: var_28 = Hex$(var_18)
  loc_008B8763: var_3C = var_18
  loc_008B8766: If var_18 < 256 Then GoTo loc_008B876E
  loc_008B8768: var_eax = Err.Raise
  loc_008B876E: 'Referenced from: 008B8766
  loc_008B87A8: call __vbaLsetFixstr(00000002h, var_278, Right$(var_004A1CA0 & var_28, 2))
  loc_008B87D6: var_18 = var_88 + var_18
  loc_008B87D9: GoTo loc_008B8727
  loc_008B87DE: 'Referenced from: 008B872D
  loc_008B87EA: GoTo loc_008B8808
  loc_008B8807: Exit Sub
  loc_008B8808: 'Referenced from: 008B87EA
  loc_008B8808: Exit Sub
End Sub
