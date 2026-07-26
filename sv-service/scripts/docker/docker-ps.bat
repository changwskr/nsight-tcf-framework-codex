@echo off
setlocal enabledelayedexpansion

rem sv-service Docker container status
rem Usage: docker-ps.bat

set "SCRIPT_DIR=%~dp0"
if not defined CONTAINER_NAME set "CONTAINER_NAME=nsight-sv"

if /i "%~1"=="help" goto :usage
if /i "%~1"=="/?" goto :usage
if /i "%~1"=="-h" goto :usage
if /i "%~1"=="--help" goto :usage

where docker >nul 2>&1
if errorlevel 1 (
    echo [sv-docker-ps] docker not found. Install Docker Desktop and retry.
    exit /b 1
)

echo [sv-docker-ps] Container filter: !CONTAINER_NAME!
echo.
docker ps -a --filter "name=^/!CONTAINER_NAME!$" --format "table {{.ID}}\t{{.Names}}\t{{.Image}}\t{{.Status}}\t{{.Ports}}"
if errorlevel 1 exit /b %errorlevel%

echo.
echo [sv-docker-ps] Compose services:
pushd "%SCRIPT_DIR%" >nul
docker compose -f "docker-compose.yml" ps
popd >nul
exit /b 0

:usage
echo Usage: docker-ps.bat
echo   Show nsight-sv container and compose service status.
exit /b 0
