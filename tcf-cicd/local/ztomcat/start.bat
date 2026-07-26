@echo off
setlocal EnableDelayedExpansion
cd /d "%~dp0"

if /i "%~1"=="-h" goto usage
if /i "%~1"=="--help" goto usage
if /i "%~1"=="help" goto usage

set "ARGS="
:arg_loop
if "%~1"=="" goto run
set "ARGS=!ARGS! %~1"
shift
goto arg_loop

:run
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0start.ps1" %ARGS%
exit /b %errorlevel%

:usage
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0start.ps1" -Help
exit /b 0
