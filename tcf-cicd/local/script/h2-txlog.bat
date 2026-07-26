@echo off
setlocal EnableDelayedExpansion
cd /d "%~dp0"

if /i "%~1"=="-h" goto usage
if /i "%~1"=="--help" goto usage
if /i "%~1"=="help" goto usage

set "ACTION="
if /i "%~1"=="start" set "ACTION=start" & shift
if /i "%~1"=="stop" set "ACTION=stop" & shift
if /i "%~1"=="status" set "ACTION=status" & shift
if /i "%~1"=="restart" set "ACTION=restart" & shift

if defined ACTION (
    powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0h2-txlog.ps1" -Action !ACTION!
) else (
    powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0h2-txlog.ps1"
)
exit /b %errorlevel%

:usage
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0h2-txlog.ps1" -Help
exit /b 0
