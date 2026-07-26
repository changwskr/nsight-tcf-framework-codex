@echo off
setlocal enabledelayedexpansion

set "PROJECT_HOME=%~dp0..\.."
for %%I in ("!PROJECT_HOME!") do set "PROJECT_HOME=%%~fI"
if exist "!PROJECT_HOME!\scripts\env-jdk21.bat" call "!PROJECT_HOME!\scripts\env-jdk21.bat"
set "GRADLE_HOME=C:\Programming(23-08-15)\gradle-8.10.1"
set "GRADLE=!GRADLE_HOME!\bin\gradle.bat"
set "MODULE=tcf-web"

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
    echo [tcf-web-build] gradle not found.
    exit /b 1
)

if /i "%~1"=="help" goto :usage
if /i "%~1"=="/?" goto :usage
if /i "%~1"=="-h" goto :usage

set "TASKS=:tcf-util:build :tcf-core:build :!MODULE!:build"
if /i "%~1"=="clean" set "TASKS=clean !TASKS!"
if /i "%~1"=="test" set "TASKS=:tcf-util:build :tcf-core:build :!MODULE!:test"

echo [tcf-web-build] Stop Gradle daemons...
call gradle --stop >nul 2>&1

cd /d "!PROJECT_HOME!"
echo [tcf-web-build] !GRADLE! !TASKS!
call "!GRADLE!" !TASKS!
if errorlevel 1 exit /b %errorlevel%

if /i "%~1"=="test" exit /b 0

set "JAR_DIR=!PROJECT_HOME!\!MODULE!\build\libs"
set "JAR_FOUND="
echo.
echo [tcf-web-build] Build output:
for %%F in ("!JAR_DIR!\tcf-web-*.jar") do (
    if exist "%%F" (
        set "JAR_FOUND=1"
        echo   [OK] %%~nxF
        dir "%%F"
    )
)
if not defined JAR_FOUND (
    echo   [MISSING] tcf-web-*.jar - not found in !MODULE!\build\libs
    exit /b 1
)
exit /b 0

:usage
echo Usage: build.bat [clean^|test]
echo   build.bat        Build tcf-util/core + tcf-web JAR
echo   build.bat clean  clean + build
echo   build.bat test   Run tcf-web unit tests
exit /b 0
