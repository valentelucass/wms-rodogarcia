@echo off
setlocal EnableExtensions DisableDelayedExpansion
cd /d "%~dp0"
title WMS Rodogarcia - Producao local
if "%~1"=="" goto iniciar
if /i "%~1"=="--preparar" if "%~2"=="" goto preparar
echo Uso: iniciar-prod.bat [--preparar]
echo Producao usa somente WMS_PROD e identidade propria. Nao encerra processos existentes.
exit /b 2
:preparar
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0infra\prod\iniciar-prod.ps1" -PrepareOnly
exit /b %ERRORLEVEL%
:iniciar
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0infra\prod\iniciar-prod.ps1"
exit /b %ERRORLEVEL%
