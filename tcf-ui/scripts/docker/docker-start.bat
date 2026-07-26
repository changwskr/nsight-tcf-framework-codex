@echo off
setlocal enabledelayedexpansion

rem tcf-ui Docker start
rem Usage: docker-start.bat

set "SCRIPT_DIR=%~dp0"
if not defined CONTAINER_NAME set "CONTAINER_NAME=nsight-ui"
if not defined HOST_PORT set "HOST_PORT=8099"

if /i "%~1"=="help" goto :usage
if /i "%~1"=="/?" goto :usage
if /i "%~1"=="-h" goto :usage
if /i "%~1"=="--help" goto :usage

where docker >nul 2>&1
if errorlevel 1 (
    echo [ui-docker-start] docker not found. Install Docker Desktop and retry.
    exit /b 1
)

docker container inspect "!CONTAINER_NAME!" >nul 2>&1
if not errorlevel 1 (
    for /f "tokens=*" %%S in ('docker inspect -f "{{.State.Status}}" "!CONTAINER_NAME!" 2^>nul') do set "STATUS=%%S"
    for /f "tokens=*" %%P in ('docker port "!CONTAINER_NAME!" 8099 2^>nul') do set "PUBLISHED=%%P"

    if /i "!STATUS!"=="running" if defined PUBLISHED (
        echo [ui-docker-start] Already running with port mapping: !PUBLISHED!
        goto :done
    )

    if /i "!STATUS!"=="running" if not defined PUBLISHED (
        echo [ui-docker-start] Running without host port mapping. Recreating with -p 0.0.0.0:!HOST_PORT!:8099 ...
        docker rm -f "!CONTAINER_NAME!" >nul 2>&1
        call "%SCRIPT_DIR%docker-run.bat" -d
        exit /b %errorlevel%
    )

    echo [ui-docker-start] Starting existing container: !CONTAINER_NAME!
    docker start "!CONTAINER_NAME!"
    if errorlevel 1 exit /b %errorlevel%

    for /f "tokens=*" %%P in ('docker port "!CONTAINER_NAME!" 8099 2^>nul') do set "PUBLISHED=%%P"
    if not defined PUBLISHED (
        echo [ui-docker-start] No host port mapping on existing container. Recreating...
        docker rm -f "!CONTAINER_NAME!" >nul 2>&1
        call "%SCRIPT_DIR%docker-run.bat" -d
        exit /b %errorlevel%
    )
    goto :done
)

echo [ui-docker-start] Container not found. Trying compose up -d...
pushd "%SCRIPT_DIR%" >nul
docker compose -f "docker-compose.yml" up -d
set "RC=!errorlevel!"
popd >nul
if not "!RC!"=="0" (
    echo [ui-docker-start] Compose start failed.
    echo [ui-docker-start] Build/run first:
    echo   docker-build.bat
    echo   docker-compose.bat up -d
    echo   or docker-run.bat -d
    exit /b !RC!
)

:done
echo.
echo [ui-docker-start] Done.
for /f "tokens=*" %%P in ('docker port "!CONTAINER_NAME!" 8099 2^>nul') do echo [ui-docker-start] Published: %%P
echo   Health : http://localhost:!HOST_PORT!/actuator/health
echo   UI     : http://localhost:!HOST_PORT!/
echo   OM     : http://localhost:!HOST_PORT!/om/admin/login.html
echo   Logs   : docker-logs.bat
echo   Stop   : docker-stop.bat
exit /b 0

:usage
echo Usage: docker-start.bat
echo   Start existing nsight-ui container, or compose up -d if missing.
exit /b 0
