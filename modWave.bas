
Public Sub Proc_24_0_8D6930
  loc_008D696E: setz cl
  loc_008D6976: setz dl
  loc_008D697B: If edx <> 0 Then GoTo loc_008D6983
  loc_008D697D: call Stop(edi, esi, ebx)
  loc_008D6983: 'Referenced from: 008D697B
  loc_008D6991: call UBound(00000001h, Me)
  loc_008D6998: UBound(00000001h, Me) = UBound(00000001h, Me) + 00000001h
  loc_008D69A3: var_24 = UBound(00000001h, Me)
  loc_008D69A6: call UBound(00000002h, Me)
  loc_008D69B2: UBound(00000002h, Me) = UBound(00000002h, Me) + 00000001h
  loc_008D69BB: edx = edx * UBound(00000001h, Me)
  loc_008D69C4: edx = edx * ecx
  loc_008D69CD: var_70 = edx*ecx
  loc_008D69D9: var_20 = UBound(00000002h, Me)
  loc_008D69E9: If var_8D9000 <> 0 Then GoTo loc_008D69F3
  loc_008D69F1: GoTo loc_008D6A04
  loc_008D69F3: 'Referenced from: 008D69E9
  loc_008D6A04: 'Referenced from: 008D69F1
  loc_008D6A13: var_3C = CLng((var_78 / 8))
  loc_008D6A18: edx = edx * UBound(00000001h, Me)
  loc_008D6A21: var_7C = edx*UBound(00000001h, Me)
  loc_008D6A3A: If var_8D9000 <> 0 Then GoTo loc_008D6A44
  loc_008D6A42: GoTo loc_008D6A55
  loc_008D6A44: 'Referenced from: 008D6A3A
  loc_008D6A55: 'Referenced from: 008D6A42
  loc_008D6A66: CLng((var_84 / 8)) = CLng((var_84 / 8)) * UBound(00000002h, Me)
  loc_008D6A72: CLng((var_84 / 8)) = CLng((var_84 / 8)) + 0000002Ch
  loc_008D6A7D: CLng((var_84 / 8)) = CLng((var_84 / 8)) - 00000008h
  loc_008D6A86: var_24 = var_24 * var_20
  loc_008D6A90: var_24 = var_24 * edx
  loc_008D6A99: var_88 = var_24
  loc_008D6A9F: var_38 = CLng((var_84 / 8))
  loc_008D6AA8: var_34 = CLng((var_84 / 8))
  loc_008D6ABE: If var_8D9000 <> 0 Then GoTo loc_008D6AC8
  loc_008D6AC6: GoTo loc_008D6AD9
  loc_008D6AC8: 'Referenced from: 008D6ABE
  loc_008D6AD9: 'Referenced from: 008D6AC6
  loc_008D6AE7: CLng((var_84 / 8)) = CLng((var_84 / 8)) - 00000001h
  loc_008D6AF3: var_28 = CLng((var_90 / 8))
  loc_008D6B03: ReDim var_30(0 To CLng((var_84 / 8)))
  loc_008D6B11: If var_30 = 0 Then GoTo loc_008D6B2D
  loc_008D6B17: If var_30 <> 1 Then GoTo loc_008D6B2D
  loc_008D6B23: If esi < 0 Then GoTo loc_008D6B35
  loc_008D6B25: var_eax = Err.Raise
  loc_008D6B2B: GoTo loc_008D6B35
  loc_008D6B2D: 'Referenced from: 008D6B11
  loc_008D6B2D: var_eax = Err.Raise
  loc_008D6B35: 'Referenced from: 008D6B2B
  loc_008D6B58: If var_30 = 0 Then GoTo loc_008D6B79
  loc_008D6B5E: If var_30 <> 1 Then GoTo loc_008D6B79
  loc_008D6B6B: 00000001h = 00000001h - .%x1 = GetIDsOfNames(%StkVar2) 'Ignore this
  loc_008D6B6F: If 00000001h < 0 Then GoTo loc_008D6B81
  loc_008D6B71: var_eax = Err.Raise
  loc_008D6B77: GoTo loc_008D6B81
  loc_008D6B79: 'Referenced from: 008D6B58
  loc_008D6B79: var_eax = Err.Raise
  loc_008D6B81: 'Referenced from: 008D6B77
  loc_008D6B9E: If var_30 = 0 Then GoTo loc_008D6BBF
  loc_008D6BA4: If var_30 <> 1 Then GoTo loc_008D6BBF
  loc_008D6BB1: 00000002h = 00000002h - .%x1 = GetIDsOfNames(%StkVar2) 'Ignore this
  loc_008D6BB5: If 00000002h < 0 Then GoTo loc_008D6BC7
  loc_008D6BB7: var_eax = Err.Raise
  loc_008D6BBD: GoTo loc_008D6BC7
  loc_008D6BBF: 'Referenced from: 008D6B9E
  loc_008D6BBF: var_eax = Err.Raise
  loc_008D6BC7: 'Referenced from: 008D6BBD
  loc_008D6BE4: If var_30 = 0 Then GoTo loc_008D6C05
  loc_008D6BEA: If var_30 <> 1 Then GoTo loc_008D6C05
  loc_008D6BF7: 00000003h = 00000003h - .%x1 = GetIDsOfNames(%StkVar2) 'Ignore this
  loc_008D6BFB: If 00000003h < 0 Then GoTo loc_008D6C0D
  loc_008D6BFD: var_eax = Err.Raise
  loc_008D6C03: GoTo loc_008D6C0D
  loc_008D6C05: 'Referenced from: 008D6BE4
  loc_008D6C05: var_eax = Err.Raise
  loc_008D6C0D: 'Referenced from: 008D6C03
  loc_008D6C2A: If var_30 = 0 Then GoTo loc_008D6C4B
  loc_008D6C30: If var_30 <> 1 Then GoTo loc_008D6C4B
  loc_008D6C3D: 00000004h = 00000004h - .%x1 = GetIDsOfNames(%StkVar2) 'Ignore this
  loc_008D6C41: If 00000004h < 0 Then GoTo loc_008D6C53
  loc_008D6C43: var_eax = Err.Raise
  loc_008D6C49: GoTo loc_008D6C53
  loc_008D6C4B: 'Referenced from: 008D6C2A
  loc_008D6C4B: var_eax = Err.Raise
  loc_008D6C53: 'Referenced from: 008D6C49
  loc_008D6C6F: If var_30 = 0 Then GoTo loc_008D6C90
  loc_008D6C75: If var_30 <> 1 Then GoTo loc_008D6C90
  loc_008D6C82: 00000005h = 00000005h - .%x1 = GetIDsOfNames(%StkVar2) 'Ignore this
  loc_008D6C86: If 00000005h < 0 Then GoTo loc_008D6C98
  loc_008D6C88: var_eax = Err.Raise
  loc_008D6C8E: GoTo loc_008D6C98
  loc_008D6C90: 'Referenced from: 008D6C6F
  loc_008D6C90: var_eax = Err.Raise
  loc_008D6C98: 'Referenced from: 008D6C8E
  loc_008D6CB0: If var_30 = 0 Then GoTo loc_008D6CD1
  loc_008D6CB6: If var_30 <> 1 Then GoTo loc_008D6CD1
  loc_008D6CC3: 00000006h = 00000006h - .%x1 = GetIDsOfNames(%StkVar2) 'Ignore this
  loc_008D6CC7: If 00000006h < 0 Then GoTo loc_008D6CD9
  loc_008D6CC9: var_eax = Err.Raise
  loc_008D6CCF: GoTo loc_008D6CD9
  loc_008D6CD1: 'Referenced from: 008D6CB0
  loc_008D6CD1: var_eax = Err.Raise
  loc_008D6CD9: 'Referenced from: 008D6CCF
  loc_008D6CF2: If var_30 = 0 Then GoTo loc_008D6D13
  loc_008D6CF8: If var_30 <> 1 Then GoTo loc_008D6D13
  loc_008D6D05: 00000007h = 00000007h - .%x1 = GetIDsOfNames(%StkVar2) 'Ignore this
  loc_008D6D09: If 00000007h < 0 Then GoTo loc_008D6D1B
  loc_008D6D0B: var_eax = Err.Raise
  loc_008D6D11: GoTo loc_008D6D1B
  loc_008D6D13: 'Referenced from: 008D6CF2
  loc_008D6D13: var_eax = Err.Raise
  loc_008D6D1B: 'Referenced from: 008D6D11
  loc_008D6D1D: cdq
  loc_008D6D24: CLng((var_84 / 8)) = CLng((var_84 / 8)) + .%x1 = GetIDsOfNames(%StkVar2) 'Ignore this And 16777215
  loc_008D6D28: sar ecx, 18h
  loc_008D6D3F: If var_30 = 0 Then GoTo loc_008D6D60
  loc_008D6D45: If var_30 <> 1 Then GoTo loc_008D6D60
  loc_008D6D52: 00000008h = 00000008h - .%x1 = GetIDsOfNames(%StkVar2) 'Ignore this
  loc_008D6D56: If 00000008h < 0 Then GoTo loc_008D6D68
  loc_008D6D58: var_eax = Err.Raise
  loc_008D6D5E: GoTo loc_008D6D68
  loc_008D6D60: 'Referenced from: 008D6D3F
  loc_008D6D60: var_eax = Err.Raise
  loc_008D6D68: 'Referenced from: 008D6D5E
  loc_008D6D87: If var_30 = 0 Then GoTo loc_008D6DA8
  loc_008D6D8D: If var_30 <> 1 Then GoTo loc_008D6DA8
  loc_008D6D9A: 00000009h = 00000009h - .%x1 = GetIDsOfNames(%StkVar2) 'Ignore this
  loc_008D6D9E: If 00000009h < 0 Then GoTo loc_008D6DB0
  loc_008D6DA0: var_eax = Err.Raise
  loc_008D6DA6: GoTo loc_008D6DB0
  loc_008D6DA8: 'Referenced from: 008D6D87
  loc_008D6DA8: var_eax = Err.Raise
  loc_008D6DB0: 'Referenced from: 008D6DA6
  loc_008D6DC9: If var_30 = 0 Then GoTo loc_008D6DEA
  loc_008D6DCF: If var_30 <> 1 Then GoTo loc_008D6DEA
  loc_008D6DDC: 0000000Ah = 0000000Ah - .%x1 = GetIDsOfNames(%StkVar2) 'Ignore this
  loc_008D6DE0: If 0000000Ah < 0 Then GoTo loc_008D6DF2
  loc_008D6DE2: var_eax = Err.Raise
  loc_008D6DE8: GoTo loc_008D6DF2
  loc_008D6DEA: 'Referenced from: 008D6DC9
  loc_008D6DEA: var_eax = Err.Raise
  loc_008D6DF2: 'Referenced from: 008D6DE8
  loc_008D6E0B: If var_30 = 0 Then GoTo loc_008D6E2C
  loc_008D6E11: If var_30 <> 1 Then GoTo loc_008D6E2C
  loc_008D6E1E: 0000000Bh = 0000000Bh - .%x1 = GetIDsOfNames(%StkVar2) 'Ignore this
  loc_008D6E22: If 0000000Bh < 0 Then GoTo loc_008D6E34
  loc_008D6E24: var_eax = Err.Raise
  loc_008D6E2A: GoTo loc_008D6E34
  loc_008D6E2C: 'Referenced from: 008D6E0B
  loc_008D6E2C: var_eax = Err.Raise
  loc_008D6E34: 'Referenced from: 008D6E2A
  loc_008D6E4D: If var_30 = 0 Then GoTo loc_008D6E6E
  loc_008D6E53: If var_30 <> 1 Then GoTo loc_008D6E6E
  loc_008D6E60: 0000000Ch = 0000000Ch - .%x1 = GetIDsOfNames(%StkVar2) 'Ignore this
  loc_008D6E64: If 0000000Ch < 0 Then GoTo loc_008D6E76
  loc_008D6E66: var_eax = Err.Raise
  loc_008D6E6C: GoTo loc_008D6E76
  loc_008D6E6E: 'Referenced from: 008D6E4D
  loc_008D6E6E: var_eax = Err.Raise
  loc_008D6E76: 'Referenced from: 008D6E6C
  loc_008D6E8F: If var_30 = 0 Then GoTo loc_008D6EB0
  loc_008D6E95: If var_30 <> 1 Then GoTo loc_008D6EB0
  loc_008D6EA2: 0000000Dh = 0000000Dh - .%x1 = GetIDsOfNames(%StkVar2) 'Ignore this
  loc_008D6EA6: If 0000000Dh < 0 Then GoTo loc_008D6EB8
  loc_008D6EA8: var_eax = Err.Raise
  loc_008D6EAE: GoTo loc_008D6EB8
  loc_008D6EB0: 'Referenced from: 008D6E8F
  loc_008D6EB0: var_eax = Err.Raise
  loc_008D6EB8: 'Referenced from: 008D6EAE
  loc_008D6ED1: If var_30 = 0 Then GoTo loc_008D6EF2
  loc_008D6ED7: If var_30 <> 1 Then GoTo loc_008D6EF2
  loc_008D6EE4: 0000000Eh = 0000000Eh - .%x1 = GetIDsOfNames(%StkVar2) 'Ignore this
  loc_008D6EE8: If 0000000Eh < 0 Then GoTo loc_008D6EFA
  loc_008D6EEA: var_eax = Err.Raise
  loc_008D6EF0: GoTo loc_008D6EFA
  loc_008D6EF2: 'Referenced from: 008D6ED1
  loc_008D6EF2: var_eax = Err.Raise
  loc_008D6EFA: 'Referenced from: 008D6EF0
  loc_008D6F13: If var_30 = 0 Then GoTo loc_008D6F34
  loc_008D6F19: If var_30 <> 1 Then GoTo loc_008D6F34
  loc_008D6F26: 0000000Fh = 0000000Fh - .%x1 = GetIDsOfNames(%StkVar2) 'Ignore this
  loc_008D6F2A: If 0000000Fh < 0 Then GoTo loc_008D6F3C
  loc_008D6F2C: var_eax = Err.Raise
  loc_008D6F32: GoTo loc_008D6F3C
  loc_008D6F34: 'Referenced from: 008D6F13
  loc_008D6F34: var_eax = Err.Raise
  loc_008D6F3C: 'Referenced from: 008D6F32
  loc_008D6F55: If var_30 = 0 Then GoTo loc_008D6F78
  loc_008D6F5B: If var_30 <> 1 Then GoTo loc_008D6F78
  loc_008D6F6E: 00000010h = 00000010h - .%x1 = GetIDsOfNames(%StkVar2) 'Ignore this
  loc_008D6F72: If 00000010h < 0 Then GoTo loc_008D6F82
  loc_008D6F74: var_eax = Err.Raise
  loc_008D6F76: GoTo loc_008D6F82
  loc_008D6F78: 'Referenced from: 008D6F55
  loc_008D6F7E: var_eax = Err.Raise
  loc_008D6F82: 'Referenced from: 008D6F76
  loc_008D6F9D: If var_30 = 0 Then GoTo loc_008D6FBA
  loc_008D6FA3: If var_30 <> 1 Then GoTo loc_008D6FBA
  loc_008D6FB0: 00000011h = 00000011h - .%x1 = GetIDsOfNames(%StkVar2) 'Ignore this
  loc_008D6FB4: If 00000011h < 0 Then GoTo loc_008D6FBE
  loc_008D6FB6: var_eax = Err.Raise
  loc_008D6FB8: GoTo loc_008D6FBE
  loc_008D6FBA: 'Referenced from: 008D6F9D
  loc_008D6FBA: var_eax = Err.Raise
  loc_008D6FBE: 'Referenced from: 008D6FB8
  loc_008D6FD0: If var_30 = 0 Then GoTo loc_008D6FED
  loc_008D6FD6: If var_30 <> 1 Then GoTo loc_008D6FED
  loc_008D6FE3: 00000012h = 00000012h - .%x1 = GetIDsOfNames(%StkVar2) 'Ignore this
  loc_008D6FE7: If 00000012h < 0 Then GoTo loc_008D6FF1
  loc_008D6FE9: var_eax = Err.Raise
  loc_008D6FEB: GoTo loc_008D6FF1
  loc_008D6FED: 'Referenced from: 008D6FD0
  loc_008D6FED: var_eax = Err.Raise
  loc_008D6FF1: 'Referenced from: 008D6FEB
  loc_008D7003: If var_30 = 0 Then GoTo loc_008D7020
  loc_008D7009: If var_30 <> 1 Then GoTo loc_008D7020
  loc_008D7016: 00000013h = 00000013h - .%x1 = GetIDsOfNames(%StkVar2) 'Ignore this
  loc_008D701A: If 00000013h < 0 Then GoTo loc_008D7024
  loc_008D701C: var_eax = Err.Raise
  loc_008D701E: GoTo loc_008D7024
  loc_008D7020: 'Referenced from: 008D7003
  loc_008D7020: var_eax = Err.Raise
  loc_008D7024: 'Referenced from: 008D701E
  loc_008D7036: If var_30 = 0 Then GoTo loc_008D7053
  loc_008D703C: If var_30 <> 1 Then GoTo loc_008D7053
  loc_008D7049: 00000014h = 00000014h - .%x1 = GetIDsOfNames(%StkVar2) 'Ignore this
  loc_008D704D: If 00000014h < 0 Then GoTo loc_008D7057
  loc_008D704F: var_eax = Err.Raise
  loc_008D7051: GoTo loc_008D7057
  loc_008D7053: 'Referenced from: 008D7036
  loc_008D7053: var_eax = Err.Raise
  loc_008D7057: 'Referenced from: 008D7051
  loc_008D706C: If var_30 = 0 Then GoTo loc_008D7089
  loc_008D7072: If var_30 <> 1 Then GoTo loc_008D7089
  loc_008D707F: 00000015h = 00000015h - .%x1 = GetIDsOfNames(%StkVar2) 'Ignore this
  loc_008D7083: If 00000015h < 0 Then GoTo loc_008D708D
  loc_008D7085: var_eax = Err.Raise
  loc_008D7087: GoTo loc_008D708D
  loc_008D7089: 'Referenced from: 008D706C
  loc_008D7089: var_eax = Err.Raise
  loc_008D708D: 'Referenced from: 008D7087
  loc_008D709F: If var_30 = 0 Then GoTo loc_008D70BC
  loc_008D70A5: If var_30 <> 1 Then GoTo loc_008D70BC
  loc_008D70B2: 00000016h = 00000016h - .%x1 = GetIDsOfNames(%StkVar2) 'Ignore this
  loc_008D70B6: If 00000016h < 0 Then GoTo loc_008D70C0
  loc_008D70B8: var_eax = Err.Raise
  loc_008D70BA: GoTo loc_008D70C0
  loc_008D70BC: 'Referenced from: 008D709F
  loc_008D70BC: var_eax = Err.Raise
  loc_008D70C0: 'Referenced from: 008D70BA
  loc_008D70E1: If var_30 = 0 Then GoTo loc_008D7102
  loc_008D70E7: If var_30 <> 1 Then GoTo loc_008D7102
  loc_008D70F4: 00000017h = 00000017h - .%x1 = GetIDsOfNames(%StkVar2) 'Ignore this
  loc_008D70F8: If 00000017h < 0 Then GoTo loc_008D710A
  loc_008D70FA: var_eax = Err.Raise
  loc_008D7100: GoTo loc_008D710A
  loc_008D7102: 'Referenced from: 008D70E1
  loc_008D7102: var_eax = Err.Raise
  loc_008D710A: 'Referenced from: 008D7100
  loc_008D711E: If var_30 = 0 Then GoTo loc_008D713F
  loc_008D7124: If var_30 <> 1 Then GoTo loc_008D713F
  loc_008D7131: 00000018h = 00000018h - .%x1 = GetIDsOfNames(%StkVar2) 'Ignore this
  loc_008D7135: If 00000018h < 0 Then GoTo loc_008D7147
  loc_008D7137: var_eax = Err.Raise
  loc_008D713D: GoTo loc_008D7147
  loc_008D713F: 'Referenced from: 008D711E
  loc_008D713F: var_eax = Err.Raise
  loc_008D7147: 'Referenced from: 008D713D
  loc_008D7162: If var_30 = 0 Then GoTo loc_008D7183
  loc_008D7168: If var_30 <> 1 Then GoTo loc_008D7183
  loc_008D7175: 00000019h = 00000019h - .%x1 = GetIDsOfNames(%StkVar2) 'Ignore this
  loc_008D7179: If 00000019h < 0 Then GoTo loc_008D718B
  loc_008D717B: var_eax = Err.Raise
  loc_008D7181: GoTo loc_008D718B
  loc_008D7183: 'Referenced from: 008D7162
  loc_008D7183: var_eax = Err.Raise
  loc_008D718B: 'Referenced from: 008D7181
  loc_008D71A0: If var_30 = 0 Then GoTo loc_008D71C1
  loc_008D71A6: If var_30 <> 1 Then GoTo loc_008D71C1
  loc_008D71B3: 0000001Ah = 0000001Ah - .%x1 = GetIDsOfNames(%StkVar2) 'Ignore this
  loc_008D71B7: If 0000001Ah < 0 Then GoTo loc_008D71C9
  loc_008D71B9: var_eax = Err.Raise
  loc_008D71BF: GoTo loc_008D71C9
  loc_008D71C1: 'Referenced from: 008D71A0
  loc_008D71C1: var_eax = Err.Raise
  loc_008D71C9: 'Referenced from: 008D71BF
  loc_008D71DE: If var_30 = 0 Then GoTo loc_008D71FF
  loc_008D71E4: If var_30 <> 1 Then GoTo loc_008D71FF
  loc_008D71F1: 0000001Bh = 0000001Bh - .%x1 = GetIDsOfNames(%StkVar2) 'Ignore this
  loc_008D71F5: If 0000001Bh < 0 Then GoTo loc_008D7207
  loc_008D71F7: var_eax = Err.Raise
  loc_008D71FD: GoTo loc_008D7207
  loc_008D71FF: 'Referenced from: 008D71DE
  loc_008D71FF: var_eax = Err.Raise
  loc_008D7207: 'Referenced from: 008D71FD
  loc_008D7209: cdq
  loc_008D7210: ebx = ebx + .%x1 = GetIDsOfNames(%StkVar2) 'Ignore this And 16777215
  loc_008D7214: sar ecx, 18h
  loc_008D7227: If var_30 = 0 Then GoTo loc_008D7248
  loc_008D722D: If var_30 <> 1 Then GoTo loc_008D7248
  loc_008D723A: 0000001Ch = 0000001Ch - .%x1 = GetIDsOfNames(%StkVar2) 'Ignore this
  loc_008D723E: If 0000001Ch < 0 Then GoTo loc_008D7250
  loc_008D7240: var_eax = Err.Raise
  loc_008D7246: GoTo loc_008D7250
  loc_008D7248: 'Referenced from: 008D7227
  loc_008D7248: var_eax = Err.Raise
  loc_008D7250: 'Referenced from: 008D7246
  loc_008D726B: If var_30 = 0 Then GoTo loc_008D728C
  loc_008D7271: If var_30 <> 1 Then GoTo loc_008D728C
  loc_008D727E: 0000001Dh = 0000001Dh - .%x1 = GetIDsOfNames(%StkVar2) 'Ignore this
  loc_008D7282: If 0000001Dh < 0 Then GoTo loc_008D7294
  loc_008D7284: var_eax = Err.Raise
  loc_008D728A: GoTo loc_008D7294
  loc_008D728C: 'Referenced from: 008D726B
  loc_008D728C: var_eax = Err.Raise
  loc_008D7294: 'Referenced from: 008D728A
  loc_008D72A8: If var_30 = 0 Then GoTo loc_008D72C9
  loc_008D72AE: If var_30 <> 1 Then GoTo loc_008D72C9
  loc_008D72BB: 0000001Eh = 0000001Eh - .%x1 = GetIDsOfNames(%StkVar2) 'Ignore this
  loc_008D72BF: If 0000001Eh < 0 Then GoTo loc_008D72D1
  loc_008D72C1: var_eax = Err.Raise
  loc_008D72C7: GoTo loc_008D72D1
  loc_008D72C9: 'Referenced from: 008D72A8
  loc_008D72C9: var_eax = Err.Raise
  loc_008D72D1: 'Referenced from: 008D72C7
  loc_008D72E6: If var_30 = 0 Then GoTo loc_008D7307
  loc_008D72EC: If var_30 <> 1 Then GoTo loc_008D7307
  loc_008D72F9: 0000001Fh = 0000001Fh - .%x1 = GetIDsOfNames(%StkVar2) 'Ignore this
  loc_008D72FD: If 0000001Fh < 0 Then GoTo loc_008D730F
  loc_008D72FF: var_eax = Err.Raise
  loc_008D7305: GoTo loc_008D730F
  loc_008D7307: 'Referenced from: 008D72E6
  loc_008D7307: var_eax = Err.Raise
  loc_008D730F: 'Referenced from: 008D7305
  loc_008D7311: cdq
  loc_008D7318: var_3C = var_3C + .%x1 = GetIDsOfNames(%StkVar2) 'Ignore this And 16777215
  loc_008D731C: sar ecx, 18h
  loc_008D732F: If var_30 = 0 Then GoTo loc_008D7350
  loc_008D7335: If var_30 <> 1 Then GoTo loc_008D7350
  loc_008D7342: 00000020h = 00000020h - .%x1 = GetIDsOfNames(%StkVar2) 'Ignore this
  loc_008D7346: If 00000020h < 0 Then GoTo loc_008D7358
  loc_008D7348: var_eax = Err.Raise
  loc_008D734E: GoTo loc_008D7358
  loc_008D7350: 'Referenced from: 008D732F
  loc_008D7350: var_eax = Err.Raise
  loc_008D7358: 'Referenced from: 008D734E
  loc_008D7373: If var_30 = 0 Then GoTo loc_008D7394
  loc_008D7379: If var_30 <> 1 Then GoTo loc_008D7394
  loc_008D7386: 00000021h = 00000021h - .%x1 = GetIDsOfNames(%StkVar2) 'Ignore this
  loc_008D738A: If 00000021h < 0 Then GoTo loc_008D739C
  loc_008D738C: var_eax = Err.Raise
  loc_008D7392: GoTo loc_008D739C
  loc_008D7394: 'Referenced from: 008D7373
  loc_008D7394: var_eax = Err.Raise
  loc_008D739C: 'Referenced from: 008D7392
  loc_008D73B0: If var_30 = 0 Then GoTo loc_008D73D1
  loc_008D73B6: If var_30 <> 1 Then GoTo loc_008D73D1
  loc_008D73C3: 00000022h = 00000022h - .%x1 = GetIDsOfNames(%StkVar2) 'Ignore this
  loc_008D73C7: If 00000022h < 0 Then GoTo loc_008D73D9
  loc_008D73C9: var_eax = Err.Raise
  loc_008D73CF: GoTo loc_008D73D9
  loc_008D73D1: 'Referenced from: 008D73B0
  loc_008D73D1: var_eax = Err.Raise
  loc_008D73D9: 'Referenced from: 008D73CF
  loc_008D73FA: If var_30 = 0 Then GoTo loc_008D741B
  loc_008D7400: If var_30 <> 1 Then GoTo loc_008D741B
  loc_008D740D: 00000023h = 00000023h - .%x1 = GetIDsOfNames(%StkVar2) 'Ignore this
  loc_008D7411: If 00000023h < 0 Then GoTo loc_008D7423
  loc_008D7413: var_eax = Err.Raise
  loc_008D7419: GoTo loc_008D7423
  loc_008D741B: 'Referenced from: 008D73FA
  loc_008D741B: var_eax = Err.Raise
  loc_008D7423: 'Referenced from: 008D7419
  loc_008D7438: If var_30 = 0 Then GoTo loc_008D7459
  loc_008D743E: If var_30 <> 1 Then GoTo loc_008D7459
  loc_008D744B: 00000024h = 00000024h - .%x1 = GetIDsOfNames(%StkVar2) 'Ignore this
  loc_008D744F: If 00000024h < 0 Then GoTo loc_008D7461
  loc_008D7451: var_eax = Err.Raise
  loc_008D7457: GoTo loc_008D7461
  loc_008D7459: 'Referenced from: 008D7438
  loc_008D7459: var_eax = Err.Raise
  loc_008D7461: 'Referenced from: 008D7457
  loc_008D7484: If var_30 = 0 Then GoTo loc_008D74A5
  loc_008D748A: If var_30 <> 1 Then GoTo loc_008D74A5
  loc_008D7497: 00000025h = 00000025h - .%x1 = GetIDsOfNames(%StkVar2) 'Ignore this
  loc_008D749B: If 00000025h < 0 Then GoTo loc_008D74AD
  loc_008D749D: var_eax = Err.Raise
  loc_008D74A3: GoTo loc_008D74AD
  loc_008D74A5: 'Referenced from: 008D7484
  loc_008D74A5: var_eax = Err.Raise
  loc_008D74AD: 'Referenced from: 008D74A3
  loc_008D74CA: If var_30 = 0 Then GoTo loc_008D74EB
  loc_008D74D0: If var_30 <> 1 Then GoTo loc_008D74EB
  loc_008D74DD: 00000026h = 00000026h - .%x1 = GetIDsOfNames(%StkVar2) 'Ignore this
  loc_008D74E1: If 00000026h < 0 Then GoTo loc_008D74F3
  loc_008D74E3: var_eax = Err.Raise
  loc_008D74E9: GoTo loc_008D74F3
  loc_008D74EB: 'Referenced from: 008D74CA
  loc_008D74EB: var_eax = Err.Raise
  loc_008D74F3: 'Referenced from: 008D74E9
  loc_008D7510: If var_30 = 0 Then GoTo loc_008D7531
  loc_008D7516: If var_30 <> 1 Then GoTo loc_008D7531
  loc_008D7523: 00000027h = 00000027h - .%x1 = GetIDsOfNames(%StkVar2) 'Ignore this
  loc_008D7527: If 00000027h < 0 Then GoTo loc_008D7539
  loc_008D7529: var_eax = Err.Raise
  loc_008D752F: GoTo loc_008D7539
  loc_008D7531: 'Referenced from: 008D7510
  loc_008D7531: var_eax = Err.Raise
  loc_008D7539: 'Referenced from: 008D752F
  loc_008D7556: If var_30 = 0 Then GoTo loc_008D7577
  loc_008D755C: If var_30 <> 1 Then GoTo loc_008D7577
  loc_008D7569: 00000028h = 00000028h - .%x1 = GetIDsOfNames(%StkVar2) 'Ignore this
  loc_008D756D: If 00000028h < 0 Then GoTo loc_008D757F
  loc_008D756F: var_eax = Err.Raise
  loc_008D7575: GoTo loc_008D757F
  loc_008D7577: 'Referenced from: 008D7556
  loc_008D7577: var_eax = Err.Raise
  loc_008D757F: 'Referenced from: 008D7575
  loc_008D759E: If var_30 = 0 Then GoTo loc_008D75BF
  loc_008D75A4: If var_30 <> 1 Then GoTo loc_008D75BF
  loc_008D75B1: 00000029h = 00000029h - .%x1 = GetIDsOfNames(%StkVar2) 'Ignore this
  loc_008D75B5: If 00000029h < 0 Then GoTo loc_008D75C7
  loc_008D75B7: var_eax = Err.Raise
  loc_008D75BD: GoTo loc_008D75C7
  loc_008D75BF: 'Referenced from: 008D759E
  loc_008D75BF: var_eax = Err.Raise
  loc_008D75C7: 'Referenced from: 008D75BD
  loc_008D75DF: If var_30 = 0 Then GoTo loc_008D7600
  loc_008D75E5: If var_30 <> 1 Then GoTo loc_008D7600
  loc_008D75F2: 0000002Ah = 0000002Ah - .%x1 = GetIDsOfNames(%StkVar2) 'Ignore this
  loc_008D75F6: If 0000002Ah < 0 Then GoTo loc_008D7608
  loc_008D75F8: var_eax = Err.Raise
  loc_008D75FE: GoTo loc_008D7608
  loc_008D7600: 'Referenced from: 008D75DF
  loc_008D7600: var_eax = Err.Raise
  loc_008D7608: 'Referenced from: 008D75FE
  loc_008D7621: If var_30 = 0 Then GoTo loc_008D7642
  loc_008D7627: If var_30 <> 1 Then GoTo loc_008D7642
  loc_008D7634: 0000002Bh = 0000002Bh - .%x1 = GetIDsOfNames(%StkVar2) 'Ignore this
  loc_008D7638: If 0000002Bh < 0 Then GoTo loc_008D764A
  loc_008D763A: var_eax = Err.Raise
  loc_008D7640: GoTo loc_008D764A
  loc_008D7642: 'Referenced from: 008D7621
  loc_008D7642: var_eax = Err.Raise
  loc_008D764A: 'Referenced from: 008D7640
  loc_008D764C: cdq
  loc_008D7653: var_28 = var_28 + .%x1 = GetIDsOfNames(%StkVar2) 'Ignore this And 16777215
  loc_008D7657: sar ecx, 18h
  loc_008D766E: If edi <> 8 Then GoTo loc_008D7796
  loc_008D7680: var_50 = var_20 - 00000001h
  loc_008D768B: If edi > 0 Then GoTo loc_008D79A9
  loc_008D769D: var_58 = var_24 - 00000001h
  loc_008D76A4: If ebx > 0 Then GoTo loc_008D7782
  loc_008D76B1: If Me = 0 Then GoTo loc_008D76F9
  loc_008D76B7: If esi <> 2 Then GoTo loc_008D76F9
  loc_008D76BE: ebx = ebx - .%x3 = PropBag.ReadProperty(%StkVar1, %StkVar2)
  loc_008D76C3: var_48 = ebx-.%x3 = PropBag.ReadProperty(%StkVar1, %StkVar2)
  loc_008D76C6: If ebx < 0 Then GoTo loc_008D76CE
  loc_008D76C8: var_eax = Err.Raise
  loc_008D76CE: 'Referenced from: 008D76C6
  loc_008D76D8: edi = edi - .%x1 = GetIDsOfNames(%StkVar2) 'Ignore this
  loc_008D76DC: If edi < 0 Then GoTo loc_008D76E4
  loc_008D76DE: var_eax = Err.Raise
  loc_008D76E4: 'Referenced from: 008D76DC
  loc_008D76F7: GoTo loc_008D7701
  loc_008D76F9: 'Referenced from: 008D76B1
  loc_008D76F9: var_eax = Err.Raise
  loc_008D7701: 'Referenced from: 008D76F7
  loc_008D7706: If var_30 = 0 Then GoTo loc_008D7740
  loc_008D770C: If var_30 <> 1 Then GoTo loc_008D7740
  loc_008D7711: var_38 = var_38 * var_18
  loc_008D7721: var_38 = var_38 + 0000002Ch
  loc_008D772A: var_38 = var_38 + ebx-.%x3 = PropBag.ReadProperty(%StkVar1, %StkVar2)
  loc_008D7732: var_38 = var_38 - .%x1 = GetIDsOfNames(%StkVar2) 'Ignore this
  loc_008D7736: If var_38 < 0 Then GoTo loc_008D7748
  loc_008D7738: var_eax = Err.Raise
  loc_008D773E: GoTo loc_008D7748
  loc_008D7740: 'Referenced from: 008D7706
  loc_008D7740: var_eax = Err.Raise
  loc_008D7748: 'Referenced from: 008D773E
End Sub

Public Sub Proc_24_1_8D7A00
  Dim var_48 As Me
  loc_008D7A4E: On Error Resume Next
  loc_008D7A67: Open Me For Input As #1 Len = -1
  loc_008D7A76: Close #1
  loc_008D7A87: If var_24 <> 0 Then GoTo loc_008D7AA7
  loc_008D7AA1: var_eax = Kill &H4008
  loc_008D7AA7: 'Referenced from: 008D7A87
  loc_008D7ABA: Open Me For Binary As #1 Len = -1
  loc_008D7AD2: Put #1, arg_C
  loc_008D7AE7: call UBound(00000001h, arg_C, FFFFFFFFh, edi, esi, ebx)
  loc_008D7AED: var_2C = UBound(00000001h, arg_C, FFFFFFFFh, edi, esi, ebx)
  loc_008D7AFA: var_54 = var_2C
  loc_008D7B0B: GoTo loc_008D7B1C
  loc_008D7B0D: 
  loc_008D7B10: var_28 = var_28 + 1
  loc_008D7B19: var_28 = var_28
  loc_008D7B1C: 'Referenced from: 008D7B0B
  loc_008D7B22: If var_28 > 0 Then GoTo loc_008D7C02
  loc_008D7B35: If arg_C = 0 Then GoTo loc_008D7B77
  loc_008D7B40: If ecx <> 1 Then GoTo loc_008D7B77
  loc_008D7B4A: var_28 = var_28 - ecx+00000014h
  loc_008D7B4D: var_4C = var_28
  loc_008D7B5B: If var_4C >= 0 Then GoTo loc_008D7B66
  loc_008D7B64: GoTo loc_008D7B6F
  loc_008D7B66: 'Referenced from: 008D7B5B
  loc_008D7B66: var_eax = Err.Raise
  loc_008D7B6C: var_74 = Err.Raise
  loc_008D7B6F: 'Referenced from: 008D7B64
  loc_008D7B72: var_78 = var_4C
  loc_008D7B75: GoTo loc_008D7B80
  loc_008D7B77: 'Referenced from: 008D7B35
  loc_008D7B77: var_eax = Err.Raise
  loc_008D7B7D: var_78 = Err.Raise
  loc_008D7B80: 'Referenced from: 008D7B75
  loc_008D7B86: If arg_C = 0 Then GoTo loc_008D7BD1
  loc_008D7B91: If edx <> 1 Then GoTo loc_008D7BD1
  loc_008D7B96: var_28 = var_28 - 0000002Ch
  loc_008D7BA7: var_48 = var_28 - eax+00000014h
  loc_008D7BB5: If var_48 >= edx+00000010h Then GoTo loc_008D7BC0
  loc_008D7BBE: GoTo loc_008D7BC9
  loc_008D7BC0: 'Referenced from: 008D7BB5
  loc_008D7BC0: var_eax = Err.Raise
  loc_008D7BC6: var_7C = Err.Raise
  loc_008D7BC9: 'Referenced from: 008D7BBE
  loc_008D7BCC: var_80 = var_48
  loc_008D7BCF: GoTo loc_008D7BDA
  loc_008D7BD1: 'Referenced from: 008D7B86
  loc_008D7BD1: var_eax = Err.Raise
  loc_008D7BD7: var_80 = Err.Raise
  loc_008D7BDA: 'Referenced from: 008D7BCF
  loc_008D7BFD: GoTo loc_008D7B0D
  loc_008D7C02: 'Referenced from: 008D7B22
  loc_008D7C27: ReDim Preserve arg_C(0 To var_2C - 0000002Bh)
  loc_008D7C4C: GoTo loc_008D7C5D
  loc_008D7C4E: 
  loc_008D7C51: var_28 = var_28 + 1
  loc_008D7C5A: var_28 = var_28
  loc_008D7C5D: 'Referenced from: 008D7C4C
  loc_008D7C63: If var_28 > 0 Then GoTo loc_008D7C86
  loc_008D7C77: Put #1, arg_C
  loc_008D7C84: GoTo loc_008D7C4E
  loc_008D7C86: 'Referenced from: 008D7C63
  loc_008D7C8F: Close #1
  loc_008D7CAD: var_48 = Err
  loc_008D7CC2: var_4C = PropBag.ReadProperty(var_44, var_30)
  loc_008D7CF5: var_24 = var_44
  loc_008D7D0A: On Error Resume Next
  loc_008D7D15: GoTo loc_008D7D21
  loc_008D7D20: Exit Sub
  loc_008D7D21: 'Referenced from: 008D7D15
  loc_008D7D21: Exit Sub
End Sub
