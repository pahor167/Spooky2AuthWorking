@echo off
echo === Spooky2 S2D Decryptor ===
echo.
echo This tests two modes:
echo   1) Pure C# Math.Pow (64-bit SSE2 double)
echo   2) MSVBVM60.DLL __vbaPowerR8 (x87 FPU, exact VB6 precision)
echo.
echo If mode 2 gives better results, precision IS the issue.
echo If mode 2 gives same results, the algorithm differs from VB6.
echo.

set CSC=C:\Windows\Microsoft.NET\Framework\v4.0.30319\csc.exe
if not exist "%CSC%" (
    for /f "delims=" %%i in ('dir /s /b C:\Windows\Microsoft.NET\Framework\v4*\csc.exe 2^>nul') do set CSC=%%i
)

echo Compiler: %CSC%
"%CSC%" /nologo /unsafe /platform:x86 /out:DecryptS2D.exe Program.cs
if errorlevel 1 (
    echo BUILD FAILED
    pause
    exit /b 1
)
echo Build OK.
echo.

set S2DFILE=%1
if "%S2DFILE%"=="" set S2DFILE=C:\Spooky2\Frequencies.s2d
if not exist "%S2DFILE%" (
    echo File not found: %S2DFILE%
    echo Usage: build_and_run.bat [path_to_s2d_file]
    pause
    exit /b 1
)

DecryptS2D.exe "%S2DFILE%" 10
