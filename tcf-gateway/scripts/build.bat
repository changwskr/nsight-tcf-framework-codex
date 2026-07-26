@echo off
setlocal enabledelayedexpansion

set "PROJECT_HOME=%~dp0..\.."
for %%I in ("!PROJECT_HOME!") do set "PROJECT_HOME=%%~fI"
if exist "!PROJECT_HOME!\scripts\env-jdk21.bat" call "!PROJECT_HOME!\scripts\env-jdk21.bat"
set "GRADLE_HOME=C:\Programming(23-08-15)\gradle-8.10.1"
set "GRADLE=!GRADLE_HOME!\bin\gradle.bat"
set "MODULE=tcf-gateway"

if defined GRADLE_HOME_OVERRIDE set "GRADLE_HOME=!GRADLE_HOME_OVERRIDE!"
if defined GRADLE_HOME set "GRADLE=!GRADLE_HOME!\bin\gradle.bat"
if not exist "!GRADLE!" for /f "delims=" %%G in ('where gradle.bat 2^>nul') do (
    set "GRADLE=%%G"
    goto :gradle_ok
)
if not exist "!GRADLE!" (
    where gradle >nul 2>&1
    if not errorlevel 1 set "GRADLE=gradle"
)
:gradle_ok
if not exist "!GRADLE!" (
    echo [gw-build] gradle not found.
    exit /b 1
)

if /i "%~1"=="help" goto :usage
if /i "%~1"=="/?" goto :usage
if /i "%~1"=="-h" goto :usage

set "TASKS=:!MODULE!:bootWar"
if /i "%~1"=="clean" set "TASKS=clean !TASKS!"
if /i "%~1"=="run" set "TASKS=:!MODULE!:bootRun"
if /i "%~1"=="run-local" (
    set "TASKS=:!MODULE!:bootRun"
    set "SPRING_PROFILES_ACTIVE=local"
)
if /i "%~1"=="run-dev" (
    set "TASKS=:!MODULE!:bootRun"
    set "SPRING_PROFILES_ACTIVE=dev"
)

echo [gw-build] Stop Gradle daemons...
call gradle --stop >nul 2>&1

cd /d "!PROJECT_HOME!"
echo [gw-build] !GRADLE! !TASKS!
call "!GRADLE!" !TASKS!
if errorlevel 1 exit /b %errorlevel%

if /i "%~1"=="run" exit /b 0
if /i "%~1"=="run-local" exit /b 0
if /i "%~1"=="run-dev" exit /b 0

set "WAR_FILE=!PROJECT_HOME!\!MODULE!\build\libs\gw.war"
echo.
echo [gw-build] Build output:
if exist "!WAR_FILE!" (
    echo   [OK] gw.war
    dir "!WAR_FILE!"
) else (
    echo   [MISSING] gw.war
    exit /b 1
)
exit /b 0

:usage
echo Usage: build.bat [clean^|run^|run-local^|run-dev]
echo   build.bat            Build gw.war
echo   build.bat clean      clean + build
echo   build.bat run        bootRun ^(SPRING_PROFILES_ACTIVE or default local^)
echo   build.bat run-local  bootRun profile=local ^(8100 -^> per-module bootRun ports^)
echo   build.bat run-dev    bootRun profile=dev ^(8100 -^> Tomcat 8080^)
exit /b 0
