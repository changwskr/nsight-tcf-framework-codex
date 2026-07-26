@echo off
setlocal enabledelayedexpansion

rem tcf-ui Docker container run
rem Usage: docker-run.bat [options]
rem   docker-run.bat
rem   docker-run.bat -d
rem   docker-run.bat nsight-ui:dev
rem   docker-run.bat -d nsight-ui:dev

set "SCRIPT_DIR=%~dp0"
for %%I in ("%SCRIPT_DIR%..\..\..") do set "PROJECT_HOME=%%~fI"

set "IMAGE_TAG=nsight-ui:local"
if not defined CONTAINER_NAME set "CONTAINER_NAME=nsight-ui"
if not defined HOST_PORT set "HOST_PORT=8099"
if not defined CONTAINER_PORT set "CONTAINER_PORT=8099"
if not defined PROFILE set "PROFILE=local"
if defined SPRING_PROFILES_ACTIVE set "PROFILE=%SPRING_PROFILES_ACTIVE%"
if not defined BOOTRUN_HOST set "BOOTRUN_HOST=http://host.docker.internal"
if not defined TOMCAT_GATEWAY_URL set "TOMCAT_GATEWAY_URL=http://host.docker.internal:8080"
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
    echo [ui-docker-run] docker not found. Install Docker Desktop and retry.
    exit /b 1
)

docker image inspect "!IMAGE_TAG!" >nul 2>&1
if errorlevel 1 (
    echo [ui-docker-run] Image not found: !IMAGE_TAG!
    echo [ui-docker-run] Build first: tcf-ui\scripts\docker\docker-build.bat
    exit /b 1
)

docker container inspect "!CONTAINER_NAME!" >nul 2>&1
if not errorlevel 1 (
    echo [ui-docker-run] Removing existing container: !CONTAINER_NAME!
    docker rm -f "!CONTAINER_NAME!" >nul 2>&1
)

echo [ui-docker-run] Image=!IMAGE_TAG!
echo [ui-docker-run] Name=!CONTAINER_NAME!
echo [ui-docker-run] Port mapping: 0.0.0.0:!HOST_PORT! -^> !CONTAINER_PORT!
echo [ui-docker-run] Profile=!PROFILE!
echo [ui-docker-run] BootRun host=!BOOTRUN_HOST!

docker run --rm !DETACH! ^
  --name "!CONTAINER_NAME!" ^
  -p 0.0.0.0:!HOST_PORT!:!CONTAINER_PORT! ^
  -e SPRING_PROFILES_ACTIVE=!PROFILE! ^
  -e SERVER_PORT=!CONTAINER_PORT! ^
  -e NSIGHT_TCF_UI_BOOTRUN_HOST=!BOOTRUN_HOST! ^
  -e NSIGHT_TCF_UI_TOMCAT_GATEWAY_URL=!TOMCAT_GATEWAY_URL! ^
  --add-host=host.docker.internal:host-gateway ^
  "!IMAGE_TAG!"
if errorlevel 1 exit /b %errorlevel%

if defined DETACH (
    echo.
    echo [ui-docker-run] Started in background.
    for /f "tokens=*" %%P in ('docker port "!CONTAINER_NAME!" !CONTAINER_PORT! 2^>nul') do echo [ui-docker-run] Published: %%P
    echo   Health : http://localhost:!HOST_PORT!/actuator/health
    echo   UI     : http://localhost:!HOST_PORT!/
    echo   OM     : http://localhost:!HOST_PORT!/om/admin/login.html
    echo   Logs   : docker logs -f !CONTAINER_NAME!
    echo   Stop   : docker rm -f !CONTAINER_NAME!
)
exit /b 0

:usage
echo Usage: docker-run.bat [-d^|--detach] [image-tag]
echo   docker-run.bat                 Foreground run ^(nsight-ui:local^)
echo   docker-run.bat -d              Detach mode
echo   docker-run.bat nsight-ui:dev   Custom image tag
echo   docker-run.bat -d nsight-ui:dev
echo.
echo Env overrides ^(optional^):
echo   set HOST_PORT=18099
echo   set BOOTRUN_HOST=http://host.docker.internal
echo   set TOMCAT_GATEWAY_URL=http://host.docker.internal:8080
exit /b 0
