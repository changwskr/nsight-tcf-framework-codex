@echo off
setlocal

powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0check-h2.ps1" %*
set EXITCODE=%ERRORLEVEL%

endlocal & exit /b %EXITCODE%
