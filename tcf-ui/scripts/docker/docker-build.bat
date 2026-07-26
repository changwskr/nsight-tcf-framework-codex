@echo off
setlocal enabledelayedexpansion

rem tcf-ui Docker image build
rem Usage: docker-build.bat [image-tag]
rem   docker-build.bat
rem   docker-build.bat nsight-ui:dev

set "SCRIPT_DIR=%~dp0"
for %%I in ("%SCRIPT_DIR%..\..\..") do set "PROJECT_HOME=%%~fI"
set "DOCKERFILE=%SCRIPT_DIR%Dockerfile"
set "IMAGE_TAG=%~1"
if "%IMAGE_TAG%"=="" set "IMAGE_TAG=nsight-ui:local"

if /i "%~1"=="help" goto :usage
if /i "%~1"=="/?" goto :usage
if /i "%~1"=="-h" goto :usage
if /i "%~1"=="--help" goto :usage

where docker >nul 2>&1
if errorlevel 1 (
    echo [ui-docker-build] docker not found. Install Docker Desktop and retry.
    exit /b 1
)

if not exist "!DOCKERFILE!" (
    echo [ui-docker-build] Dockerfile not found: !DOCKERFILE!
    exit /b 1
)

echo [ui-docker-build] PROJECT_HOME=!PROJECT_HOME!
echo [ui-docker-build] Dockerfile=!DOCKERFILE!
echo [ui-docker-build] Image=!IMAGE_TAG!
echo [ui-docker-build] docker build -f tcf-ui/scripts/docker/Dockerfile -t !IMAGE_TAG! .

cd /d "!PROJECT_HOME!"
docker build -f "tcf-ui/scripts/docker/Dockerfile" -t "!IMAGE_TAG!" .
if errorlevel 1 exit /b %errorlevel%

echo.
echo [ui-docker-build] Done.
echo   Image : !IMAGE_TAG!
echo   Run   : tcf-ui\scripts\docker\docker-run.bat
exit /b 0

:usage
echo Usage: docker-build.bat [image-tag]
echo   docker-build.bat                 Build nsight-ui:local
echo   docker-build.bat nsight-ui:dev   Build with custom tag
exit /b 0
