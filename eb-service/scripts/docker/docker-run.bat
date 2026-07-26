@echo off
setlocal enabledelayedexpansion

rem eb-service Docker container run
rem Usage: docker-run.bat [options]
rem   docker-run.bat
rem   docker-run.bat -d
rem   docker-run.bat nsight-eb:dev
rem   docker-run.bat -d nsight-eb:dev

set "SCRIPT_DIR=%~dp0"
for %%I in ("%SCRIPT_DIR%..\..\..") do set "PROJECT_HOME=%%~fI"

set "IMAGE_TAG=nsight-eb:local"
if not defined CONTAINER_NAME set "CONTAINER_NAME=nsight-eb"
if not defined HOST_PORT set "HOST_PORT=8089"
if not defined CONTAINER_PORT set "CONTAINER_PORT=8089"
if not defined PROFILE set "PROFILE=local"
if defined SPRING_PROFILES_ACTIVE set "PROFILE=%SPRING_PROFILES_ACTIVE%"
if not defined VOLUME_NAME set "VOLUME_NAME=nsight-eb-data"
if not defined EP_ONLINE_URL set "EP_ONLINE_URL=http://host.docker.internal:8090/ep/online"
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
    echo [eb-docker-run] docker not found. Install Docker Desktop and retry.
    exit /b 1
)

docker image inspect "!IMAGE_TAG!" >nul 2>&1
if errorlevel 1 (
    echo [eb-docker-run] Image not found: !IMAGE_TAG!
    echo [eb-docker-run] Build first: eb-service\scripts\docker\docker-build.bat
    exit /b 1
)

docker container inspect "!CONTAINER_NAME!" >nul 2>&1
if not errorlevel 1 (
    echo [eb-docker-run] Removing existing container: !CONTAINER_NAME!
    docker rm -f "!CONTAINER_NAME!" >nul 2>&1
)

echo [eb-docker-run] Image=!IMAGE_TAG!
echo [eb-docker-run] Name=!CONTAINER_NAME!
echo [eb-docker-run] Port=!HOST_PORT!^>!CONTAINER_PORT!
echo [eb-docker-run] Profile=!PROFILE!
echo [eb-docker-run] EP URL=!EP_ONLINE_URL!

docker run --rm !DETACH! ^
  --name "!CONTAINER_NAME!" ^
  -p !HOST_PORT!:!CONTAINER_PORT! ^
  -e SPRING_PROFILES_ACTIVE=!PROFILE! ^
  -e SERVER_PORT=!CONTAINER_PORT! ^
  -e NSIGHT_EB_EVENT_PUBLISH_EP_ONLINE_URL=!EP_ONLINE_URL! ^
  -e NSIGHT_TXLOG_PATH=/app/data/nsight-txlog ^
  -v !VOLUME_NAME!:/app/data ^
  "!IMAGE_TAG!"
if errorlevel 1 exit /b %errorlevel%

if defined DETACH (
    echo.
    echo [eb-docker-run] Started in background.
    echo   Health : http://localhost:!HOST_PORT!/actuator/health
    echo   Online : http://localhost:!HOST_PORT!/eb/online
    echo   Logs   : docker logs -f !CONTAINER_NAME!
    echo   Stop   : docker rm -f !CONTAINER_NAME!
)
exit /b 0

:usage
echo Usage: docker-run.bat [-d^|--detach] [image-tag]
echo   docker-run.bat                 Foreground run ^(nsight-eb:local^)
echo   docker-run.bat -d              Detach mode
echo   docker-run.bat nsight-eb:dev   Custom image tag
echo   docker-run.bat -d nsight-eb:dev
echo.
echo Env overrides ^(optional^):
echo   set HOST_PORT=18089
echo   set EP_ONLINE_URL=http://host.docker.internal:8090/ep/online
exit /b 0
