@echo off
setlocal EnableExtensions DisableDelayedExpansion
for /f %%S in ('powershell.exe -NoLogo -NoProfile -NonInteractive -File "%~dp0..\..\scripts\d22-console.ps1"') do set "wms_console=%%S"
echo %wms_console%
if "%wms_console%"=="CALLER" exit /b 0
exit /b 1
