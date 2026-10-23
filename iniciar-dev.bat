@echo off
setlocal EnableExtensions DisableDelayedExpansion
cd /d "%~dp0"
title WMS Rodogarcia - Desenvolvimento integrado REAL
rem BE02-DEV-FONTE01: prepara/builda fonte atual antes da guarda e do reinicio atestado.
rem O Windows PowerShell deve carregar os modulos nativos, mesmo quando chamado pelo PowerShell 7.
set "PSModulePath="
if not "%~1"=="" (
  echo Uso: iniciar-dev.bat
  echo Reinicia somente este WMS DEV nas portas 25580 e 25581. Preserva outros projetos e producao.
  exit /b 2
)
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0infra\dev\iniciar-dev.ps1"
exit /b %ERRORLEVEL%
