@echo off
setlocal enabledelayedexpansion

set "PROJECT_HOME=%~dp0..\.."
for %%I in ("!PROJECT_HOME!") do set "PROJECT_HOME=%%~fI"
if exist "!PROJECT_HOME!\scripts\env-jdk21.bat" call "!PROJECT_HOME!\scripts\env-jdk21.bat"
set "GRADLE_HOME=C:\Programming(23-08-15)\gradle-8.10.1"
set "GRADLE=!GRADLE_HOME!\bin\gradle.bat"
set "MODULE=mg-service"

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
    echo [mg-build] gradle not found.
    exit /b 1
)

if /i "%~1"=="help" goto :usage
if /i "%~1"=="/?" goto :usage
if /i "%~1"=="-h" goto :usage

set "TASKS=:tcf-util:build :tcf-core:build :tcf-web:build :!MODULE!:bootWar"
if /i "%~1"=="clean" set "TASKS=clean !TASKS!"
if /i "%~1"=="run" set "TASKS=:!MODULE!:bootRun"

echo [mg-build] Stop Gradle daemons...
call gradle --stop >nul 2>&1

cd /d "!PROJECT_HOME!"
echo [mg-build] !GRADLE! !TASKS!
call "!GRADLE!" !TASKS!
if errorlevel 1 exit /b %errorlevel%

if /i "%~1"=="run" exit /b 0

set "WAR_FILE=!PROJECT_HOME!\!MODULE!\build\libs\mg.war"
echo.
echo [mg-build] Build output:
if exist "!WAR_FILE!" (
    echo   [OK] mg.war
    dir "!WAR_FILE!"
) else (
    echo   [MISSING] mg.war - not found in !MODULE!\build\libs
    exit /b 1
)
exit /b 0

:usage
echo Usage: build.bat [clean^|run]
echo   build.bat        Build tcf-util/core/web + mg.war
echo   build.bat clean  clean + build
echo   build.bat run    bootRun ^(port 8096^)
exit /b 0

