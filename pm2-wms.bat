@echo off
setlocal EnableExtensions DisableDelayedExpansion
cd /d "%~dp0"
node "%~dp0infra\prod\pm2-control.cjs" %*
exit /b %ERRORLEVEL%
