@echo off
setlocal EnableExtensions DisableDelayedExpansion
cd /d "%~dp0..\.."
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0configurar-login.ps1"
exit /b %ERRORLEVEL%
