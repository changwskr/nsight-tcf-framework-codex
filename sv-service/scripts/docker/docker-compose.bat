@echo off
setlocal enabledelayedexpansion

rem sv-service docker compose wrapper
rem Usage: docker-compose.bat [compose-args...]
rem   docker-compose.bat up -d --build
rem   docker-compose.bat down
rem   docker-compose.bat logs -f
rem   docker-compose.bat ps

set "SCRIPT_DIR=%~dp0"
set "COMPOSE_FILE=%SCRIPT_DIR%docker-compose.yml"

if /i "%~1"=="help" goto :usage
if /i "%~1"=="/?" goto :usage
if /i "%~1"=="-h" goto :usage
if /i "%~1"=="--help" goto :usage

where docker >nul 2>&1
if errorlevel 1 (
    echo [sv-docker-compose] docker not found. Install Docker Desktop and retry.
    exit /b 1
)

if not exist "!COMPOSE_FILE!" (
    echo [sv-docker-compose] compose file not found: !COMPOSE_FILE!
    exit /b 1
)

if "%~1"=="" goto :usage

echo [sv-docker-compose] docker compose -f docker-compose.yml %*
pushd "%SCRIPT_DIR%" >nul
docker compose -f "docker-compose.yml" %*
set "RC=!errorlevel!"
popd >nul
exit /b !RC!

:usage
echo Usage: docker-compose.bat [compose-args...]
echo.
echo Common commands:
echo   docker-compose.bat up -d --build   Build image and start detached
echo   docker-compose.bat up -d           Start detached
echo   docker-compose.bat down            Stop and remove containers
echo   docker-compose.bat stop            Stop only
echo   docker-compose.bat start           Start existing
echo   docker-compose.bat ps              Status
echo   docker-compose.bat logs -f         Follow logs
echo   docker-compose.bat build           Build image only
echo.
echo Env overrides ^(optional^):
echo   set HOST_PORT=18086
echo   set IC_BASE_URL=http://host.docker.internal:8082
echo   set IMAGE_TAG=nsight-sv:local
exit /b 0
