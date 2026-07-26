@echo off
setlocal enabledelayedexpansion

rem eb-service Docker image build
rem Usage: docker-build.bat [image-tag]
rem   docker-build.bat
rem   docker-build.bat nsight-eb:dev

set "SCRIPT_DIR=%~dp0"
for %%I in ("%SCRIPT_DIR%..\..\..") do set "PROJECT_HOME=%%~fI"
set "DOCKERFILE=%SCRIPT_DIR%Dockerfile"
set "IMAGE_TAG=%~1"
if "%IMAGE_TAG%"=="" set "IMAGE_TAG=nsight-eb:local"

if /i "%~1"=="help" goto :usage
if /i "%~1"=="/?" goto :usage
if /i "%~1"=="-h" goto :usage
if /i "%~1"=="--help" goto :usage

where docker >nul 2>&1
if errorlevel 1 (
    echo [eb-docker-build] docker not found. Install Docker Desktop and retry.
    exit /b 1
)

if not exist "!DOCKERFILE!" (
    echo [eb-docker-build] Dockerfile not found: !DOCKERFILE!
    exit /b 1
)

echo [eb-docker-build] PROJECT_HOME=!PROJECT_HOME!
echo [eb-docker-build] Dockerfile=!DOCKERFILE!
echo [eb-docker-build] Image=!IMAGE_TAG!
echo [eb-docker-build] docker build -f eb-service/scripts/docker/Dockerfile -t !IMAGE_TAG! .

cd /d "!PROJECT_HOME!"
docker build -f "eb-service/scripts/docker/Dockerfile" -t "!IMAGE_TAG!" .
if errorlevel 1 exit /b %errorlevel%

echo.
echo [eb-docker-build] Done.
echo   Image : !IMAGE_TAG!
echo   Run   : eb-service\scripts\docker\docker-run.bat
exit /b 0

:usage
echo Usage: docker-build.bat [image-tag]
echo   docker-build.bat                 Build nsight-eb:local
echo   docker-build.bat nsight-eb:dev   Build with custom tag
exit /b 0
