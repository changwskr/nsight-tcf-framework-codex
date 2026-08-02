@echo off
setlocal
set "SCRIPT_DIR=%~dp0"
set "SIMULATOR=%SCRIPT_DIR%run-simulation.ps1"
set "WORKSPACE=%SCRIPT_DIR%..\workspace"
set "RUN_DIRECTORY=%SCRIPT_DIR%..\_runs"
set "OMIT_SECURITY="

if not "%~1"=="" set "WORKSPACE=%~1"
if not "%~2"=="" set "RUN_DIRECTORY=%~2"
if /I "%~3"=="--omit-security-review" set "OMIT_SECURITY=-OmitSecurityReview"

powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%SIMULATOR%" -Workspace "%WORKSPACE%" -RunDirectory "%RUN_DIRECTORY%" %OMIT_SECURITY%
exit /b %ERRORLEVEL%
