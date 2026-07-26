@echo off
setlocal EnableExtensions
rem List installed software -> CSV (UTF-8)
rem Usage: list-installed-software.bat
rem        list-installed-software.bat -Open

set "SCRIPT_DIR=%~dp0"
powershell -NoProfile -ExecutionPolicy Bypass -File "%SCRIPT_DIR%list-installed-software.ps1" %*
exit /b %ERRORLEVEL%
