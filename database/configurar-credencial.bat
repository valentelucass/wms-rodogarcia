@echo off
setlocal EnableExtensions DisableDelayedExpansion
title WMS - Configurar credencial local
if not "%~2"=="" goto argumento
if /I "%~1"=="--offline" goto offline
if not "%~1"=="" goto argumento
for /f %%S in ('powershell.exe -NoLogo -NoProfile -NonInteractive -File "%~dp0scripts\d22-console.ps1"') do set "wms_console=%%S"
if "%wms_console%"=="EXPLORER" (
  cmd.exe /d /k call "%~f0"
  exit /b %errorlevel%
)
powershell.exe -NoLogo -NoProfile -File "%~dp0scripts\configurar-credencial.ps1"
exit /b %errorlevel%
:offline
powershell.exe -NoLogo -NoProfile -NonInteractive -File "%~dp0scripts\configurar-credencial.ps1" -Offline
exit /b %errorlevel%
:argumento
echo Argumento invalido. Use sem argumentos ou --offline.
exit /b 2
