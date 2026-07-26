@echo off
setlocal enabledelayedexpansion

rem tcf-ui Docker stop
rem Usage: docker-stop.bat

set "SCRIPT_DIR=%~dp0"
if not defined CONTAINER_NAME set "CONTAINER_NAME=nsight-ui"

if /i "%~1"=="help" goto :usage
if /i "%~1"=="/?" goto :usage
if /i "%~1"=="-h" goto :usage
if /i "%~1"=="--help" goto :usage

where docker >nul 2>&1
if errorlevel 1 (
    echo [ui-docker-stop] docker not found. Install Docker Desktop and retry.
    exit /b 1
)

set "STOPPED=0"

docker container inspect "!CONTAINER_NAME!" >nul 2>&1
if not errorlevel 1 (
    echo [ui-docker-stop] Stopping container: !CONTAINER_NAME!
    docker stop "!CONTAINER_NAME!"
    if errorlevel 1 exit /b %errorlevel%
    set "STOPPED=1"
)

pushd "%SCRIPT_DIR%" >nul
docker compose -f "docker-compose.yml" ps -q >nul 2>&1
if not errorlevel 1 (
    for /f "delims=" %%C in ('docker compose -f "docker-compose.yml" ps -q 2^>nul') do (
        echo [ui-docker-stop] Stopping compose services...
        docker compose -f "docker-compose.yml" stop
        set "STOPPED=1"
        goto :compose_done
    )
)
:compose_done
popd >nul

if "!STOPPED!"=="0" (
    echo [ui-docker-stop] No running nsight-ui container/compose service found.
    exit /b 0
)

echo [ui-docker-stop] Done. Start again with docker-start.bat
exit /b 0

:usage
echo Usage: docker-stop.bat
echo   Stop nsight-ui container / compose service ^(keeps container for restart^).
exit /b 0
