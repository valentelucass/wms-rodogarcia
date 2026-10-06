@echo off
setlocal EnableExtensions DisableDelayedExpansion
title WMS - Bancos locais
echo WMS - entrega manual
echo Alvo fixo: TCP127.0.0.1:1433 - SQL auth sa - master
echo Bancos: WMS_DEV e WMS_PROD. DEV primeiro. Existentes preservados.
echo Sem migrations/cargas. TLS com validacao obrigatoria. Sem alteracoes globais.
if not "%~2"=="" goto argumento
if /I "%~1"=="--offline" goto offline
if not "%~1"=="" goto argumento
powershell.exe -NoLogo -NoProfile -File "%~dp0scripts\iniciar-bancos.ps1"
set "wms_exit=%errorlevel%"
echo Processo encerrado. Consulte o resultado acima e a evidencia local sanitizada.
pause
exit /b %wms_exit%
:offline
powershell.exe -NoLogo -NoProfile -NonInteractive -File "%~dp0scripts\iniciar-bancos.ps1" -Offline
exit /b %errorlevel%
:argumento
echo Argumento invalido. Use sem argumentos ou --offline.
pause
exit /b 2
