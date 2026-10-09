@echo off
setlocal EnableExtensions DisableDelayedExpansion
cd /d "%~dp0"
title WMS Rodogarcia - Desenvolvimento integrado REAL
if not "%~1"=="" (
  echo Uso: iniciar-dev.bat
  echo Inicia somente WMS_DEV/WMSDEV. Nao usa mock nem encerra processos existentes.
  exit /b 2
)
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0infra\dev\iniciar-dev.ps1"
exit /b %ERRORLEVEL%
