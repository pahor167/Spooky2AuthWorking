VERSION 5.00
Begin VB.Form Graphics
  Caption = "Graphics"
  ScaleMode = 1
  AutoRedraw = False
  FontTransparent = True
  Icon = "Graphics.frx":0
  LinkTopic = "Form1"
  ClientLeft = 60
  ClientTop = 510
  ClientWidth = 15840
  ClientHeight = 10410
  StartUpPosition = 3 'Windows Default
  Begin Image Image1
    Index = 1
    Picture = "Graphics.frx":FD55
    Left = 3600
    Top = 4320
    Width = 1800
    Height = 1410
    Visible = 0   'False
  End
End
Begin Image Image1
  Index = 0
  Picture = "Graphics.frx":10CCD
  Left = 840
  Top = 4320
  Width = 1800
  Height = 1410
  Visible = 0   'False
End
Begin Image Image1
  Index = 18
  Picture = "Graphics.frx":11AAF
  Left = 360
  Top = 240
  Width = 5325
  Height = 4080
  Visible = 0   'False
End
End

Attribute VB_Name = "Graphics"

