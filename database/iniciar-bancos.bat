@echo off
setlocal EnableExtensions DisableDelayedExpansion
set "PSModulePath=%SystemRoot%\System32\WindowsPowerShell\v1.0\Modules;%ProgramFiles%\WindowsPowerShell\Modules;%USERPROFILE%\Documents\WindowsPowerShell\Modules"
title WMS - Bancos locais
echo WMS - D22 bootstrap/upgrade automatico
echo Alvo fixo: TCP127.0.0.1:1433 - SQL auth sa - master
echo Bancos: WMS_DEV e WMS_PROD. DEV primeiro. Existentes preservados.
echo Flyway migrations DEV e PROD. Qualquer falha DEV bloqueia PROD.
echo TLS com validacao obrigatoria. Sem clean/repair/baseline ou alteracoes globais.
if not "%~2"=="" goto argumento
if /I "%~1"=="--offline" goto offline
if /I "%~1"=="--fixture" goto fixture
if not "%~1"=="" goto argumento
for /f %%S in ('powershell.exe -NoLogo -NoProfile -NonInteractive -File "%~dp0scripts\d22-console.ps1"') do set "wms_console=%%S"
if "%wms_console%"=="EXPLORER" (
  cmd.exe /d /k call "%~f0"
  exit /b %errorlevel%
)
powershell.exe -NoLogo -NoProfile -NonInteractive -File "%~dp0scripts\iniciar-bancos.ps1"
set "wms_exit=%errorlevel%"
echo Processo encerrado. Consulte o resultado acima e a evidencia local sanitizada.
exit /b %wms_exit%
:offline
powershell.exe -NoLogo -NoProfile -NonInteractive -File "%~dp0scripts\iniciar-bancos.ps1" -Offline
exit /b %errorlevel%
:fixture
echo FIXTURE ficticia explicita. Nenhum segredo real nem SQL.
powershell.exe -NoLogo -NoProfile -NonInteractive -File "%~dp0scripts\iniciar-bancos.ps1" -Fixture
exit /b %errorlevel%
:argumento
echo Argumento invalido. Use sem argumentos, --offline ou --fixture.
exit /b 2
