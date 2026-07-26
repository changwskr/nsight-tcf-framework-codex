@echo off
setlocal enabledelayedexpansion

rem eb-service Docker start
rem Usage: docker-start.bat

set "SCRIPT_DIR=%~dp0"
if not defined CONTAINER_NAME set "CONTAINER_NAME=nsight-eb"
if not defined HOST_PORT set "HOST_PORT=8089"

if /i "%~1"=="help" goto :usage
if /i "%~1"=="/?" goto :usage
if /i "%~1"=="-h" goto :usage
if /i "%~1"=="--help" goto :usage

where docker >nul 2>&1
if errorlevel 1 (
    echo [eb-docker-start] docker not found. Install Docker Desktop and retry.
    exit /b 1
)

docker container inspect "!CONTAINER_NAME!" >nul 2>&1
if not errorlevel 1 (
    for /f "tokens=*" %%S in ('docker inspect -f "{{.State.Status}}" "!CONTAINER_NAME!" 2^>nul') do set "STATUS=%%S"
    if /i "!STATUS!"=="running" (
        echo [eb-docker-start] Already running: !CONTAINER_NAME!
        goto :done
    )
    echo [eb-docker-start] Starting existing container: !CONTAINER_NAME!
    docker start "!CONTAINER_NAME!"
    if errorlevel 1 exit /b %errorlevel%
    goto :done
)

echo [eb-docker-start] Container not found. Trying compose up -d...
pushd "%SCRIPT_DIR%" >nul
docker compose -f "docker-compose.yml" up -d
set "RC=!errorlevel!"
popd >nul
if not "!RC!"=="0" (
    echo [eb-docker-start] Compose start failed.
    echo [eb-docker-start] Build/run first:
    echo   docker-build.bat
    echo   docker-compose.bat up -d
    echo   or docker-run.bat -d
    exit /b !RC!
)

:done
echo.
echo [eb-docker-start] Done.
echo   Health : http://localhost:!HOST_PORT!/actuator/health
echo   Online : http://localhost:!HOST_PORT!/eb/online
echo   Logs   : docker-logs.bat
echo   Stop   : docker-stop.bat
exit /b 0

:usage
echo Usage: docker-start.bat
echo   Start existing nsight-eb container, or compose up -d if missing.
exit /b 0
