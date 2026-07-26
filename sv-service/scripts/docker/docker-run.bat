@echo off
setlocal enabledelayedexpansion

rem sv-service Docker container run
rem Usage: docker-run.bat [options]
rem   docker-run.bat
rem   docker-run.bat -d
rem   docker-run.bat nsight-sv:dev
rem   docker-run.bat -d nsight-sv:dev

set "SCRIPT_DIR=%~dp0"
for %%I in ("%SCRIPT_DIR%..\..\..") do set "PROJECT_HOME=%%~fI"

set "IMAGE_TAG=nsight-sv:local"
if not defined CONTAINER_NAME set "CONTAINER_NAME=nsight-sv"
if not defined HOST_PORT set "HOST_PORT=8086"
if not defined CONTAINER_PORT set "CONTAINER_PORT=8086"
if not defined PROFILE set "PROFILE=local"
if defined SPRING_PROFILES_ACTIVE set "PROFILE=%SPRING_PROFILES_ACTIVE%"
if not defined VOLUME_NAME set "VOLUME_NAME=nsight-sv-data"
if not defined IC_BASE_URL set "IC_BASE_URL=http://host.docker.internal:8082"
set "DETACH="

:parse_args
if "%~1"=="" goto :args_done
if /i "%~1"=="help" goto :usage
if /i "%~1"=="/?" goto :usage
if /i "%~1"=="-h" goto :usage
if /i "%~1"=="--help" goto :usage
if /i "%~1"=="-d" (
    set "DETACH=-d"
    shift
    goto :parse_args
)
if /i "%~1"=="--detach" (
    set "DETACH=-d"
    shift
    goto :parse_args
)
set "IMAGE_TAG=%~1"
shift
goto :parse_args
:args_done

where docker >nul 2>&1
if errorlevel 1 (
    echo [sv-docker-run] docker not found. Install Docker Desktop and retry.
    exit /b 1
)

docker image inspect "!IMAGE_TAG!" >nul 2>&1
if errorlevel 1 (
    echo [sv-docker-run] Image not found: !IMAGE_TAG!
    echo [sv-docker-run] Build first: sv-service\scripts\docker\docker-build.bat
    exit /b 1
)

docker container inspect "!CONTAINER_NAME!" >nul 2>&1
if not errorlevel 1 (
    echo [sv-docker-run] Removing existing container: !CONTAINER_NAME!
    docker rm -f "!CONTAINER_NAME!" >nul 2>&1
)

echo [sv-docker-run] Image=!IMAGE_TAG!
echo [sv-docker-run] Name=!CONTAINER_NAME!
echo [sv-docker-run] Port mapping: 0.0.0.0:!HOST_PORT! -^> !CONTAINER_PORT!
echo [sv-docker-run] Profile=!PROFILE!
echo [sv-docker-run] IC URL=!IC_BASE_URL!

docker run --rm !DETACH! ^
  --name "!CONTAINER_NAME!" ^
  -p 0.0.0.0:!HOST_PORT!:!CONTAINER_PORT! ^
  -e SPRING_PROFILES_ACTIVE=!PROFILE! ^
  -e SERVER_PORT=!CONTAINER_PORT! ^
  -e NSIGHT_INTEGRATION_SERVICES_IC_BASE_URL=!IC_BASE_URL! ^
  -e NSIGHT_TXLOG_PATH=/app/data/nsight-txlog ^
  -v !VOLUME_NAME!:/app/data ^
  --add-host=host.docker.internal:host-gateway ^
  "!IMAGE_TAG!"
if errorlevel 1 exit /b %errorlevel%

if defined DETACH (
    echo.
    echo [sv-docker-run] Started in background.
    for /f "tokens=*" %%P in ('docker port "!CONTAINER_NAME!" !CONTAINER_PORT! 2^>nul') do echo [sv-docker-run] Published: %%P
    echo   Health : http://localhost:!HOST_PORT!/actuator/health
    echo   Online : http://localhost:!HOST_PORT!/sv/online
    echo   Logs   : docker logs -f !CONTAINER_NAME!
    echo   Stop   : docker rm -f !CONTAINER_NAME!
)
exit /b 0

:usage
echo Usage: docker-run.bat [-d^|--detach] [image-tag]
echo   docker-run.bat                 Foreground run ^(nsight-sv:local^)
echo   docker-run.bat -d              Detach mode
echo   docker-run.bat nsight-sv:dev   Custom image tag
echo   docker-run.bat -d nsight-sv:dev
echo.
echo Env overrides ^(optional^):
echo   set HOST_PORT=18086
echo   set IC_BASE_URL=http://host.docker.internal:8082
exit /b 0
