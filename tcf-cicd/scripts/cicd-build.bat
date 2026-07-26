@echo off
setlocal
cd /d "%~dp0"

where pwsh >nul 2>&1
if not errorlevel 1 goto run_pwsh
goto run_powershell

:run_pwsh
pwsh -NoProfile -ExecutionPolicy Bypass -File "%~dp0cicd-build.ps1" %*
exit /b %errorlevel%

:run_powershell
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0cicd-build.ps1" %*
exit /b %errorlevel%
