@echo off
setlocal

set ACTION=%1
if "%ACTION%"=="" set ACTION=start

powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0run-h2.ps1" %ACTION%
set EXITCODE=%ERRORLEVEL%

endlocal & exit /b %EXITCODE%
