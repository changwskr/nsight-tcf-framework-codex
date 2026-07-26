@echo off
setlocal enabledelayedexpansion

rem NSIGHT TCF Framework Kubernetes deploy (Windows helper)
rem Prefer Git Bash / WSL for the real script.
rem
rem Usage:
rem   deploy.bat development abc1234
rem   deploy.bat production abc1234 eb,sv,ui

set "SCRIPT_DIR=%~dp0"

if /i "%~1"=="help" goto :usage
if /i "%~1"=="/?" goto :usage
if /i "%~1"=="-h" goto :usage
if /i "%~1"=="--help" goto :usage

if "%~1"=="" goto :usage
if "%~2"=="" goto :usage

where bash >nul 2>&1
if errorlevel 1 (
    echo [tcf-k8s-deploy] bash not found.
    echo   Run from Git Bash / WSL:
    echo     ./deploy.sh %*
    exit /b 1
)

bash "%SCRIPT_DIR%deploy.sh" %*
exit /b %errorlevel%

:usage
echo Usage: deploy.bat ^<environment^> ^<commit-sha^> [module,...]
echo   environment : development ^| staging ^| production
echo   commit-sha  : image tag ^(git short SHA^)
echo.
echo Examples:
echo   deploy.bat development abc1234
echo   deploy.bat production  abc1234 eb,sv,ui
exit /b 0
