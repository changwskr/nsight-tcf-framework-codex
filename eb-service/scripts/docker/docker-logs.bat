@echo off
setlocal enabledelayedexpansion

rem eb-service Docker logs
rem Usage: docker-logs.bat [options]
rem   docker-logs.bat
rem   docker-logs.bat --tail 200
rem   docker-logs.bat --since 10m

set "SCRIPT_DIR=%~dp0"
if not defined CONTAINER_NAME set "CONTAINER_NAME=nsight-eb"

if /i "%~1"=="help" goto :usage
if /i "%~1"=="/?" goto :usage
if /i "%~1"=="-h" goto :usage
if /i "%~1"=="--help" goto :usage

where docker >nul 2>&1
if errorlevel 1 (
    echo [eb-docker-logs] docker not found. Install Docker Desktop and retry.
    exit /b 1
)

docker container inspect "!CONTAINER_NAME!" >nul 2>&1
if errorlevel 1 (
    echo [eb-docker-logs] Container not found: !CONTAINER_NAME!
    echo [eb-docker-logs] Start first: docker-compose.bat up -d   or   docker-run.bat -d
    exit /b 1
)

echo [eb-docker-logs] Following logs: !CONTAINER_NAME!
echo [eb-docker-logs] Ctrl+C to stop following.
echo.
if "%~1"=="" (
    docker logs -f --tail 200 "!CONTAINER_NAME!"
) else (
    docker logs -f "!CONTAINER_NAME!" %*
)
exit /b %errorlevel%

:usage
echo Usage: docker-logs.bat [docker-logs-options]
echo   docker-logs.bat
echo   docker-logs.bat --tail 500
echo   docker-logs.bat --since 10m
exit /b 0
