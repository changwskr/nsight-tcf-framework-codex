@echo off
setlocal enabledelayedexpansion

set "PROJECT_HOME=%~dp0..\.."
for %%I in ("!PROJECT_HOME!") do set "PROJECT_HOME=%%~fI"
if exist "!PROJECT_HOME!\scripts\env-jdk21.bat" call "!PROJECT_HOME!\scripts\env-jdk21.bat"
set "WEBAPPS=!PROJECT_HOME!\ztomcat\apache-tomcat-10.1.34\webapps"
set "GRADLE_HOME=C:\Programming(23-08-15)\gradle-8.10.1"
set "GRADLE=!GRADLE_HOME!\bin\gradle.bat"
set "MODULE=ms-service"
set "SRC_WAR=ms.war"
set "DEST_WAR=ms.war"
set "CTX=ms"

if defined TOMCAT_WEBAPPS set "WEBAPPS=!TOMCAT_WEBAPPS!"

if /i "%~1"=="help" goto :usage
if /i "%~1"=="/?" goto :usage
if /i "%~1"=="-h" goto :usage

if not exist "!WEBAPPS!" goto :webapps_missing
goto :webapps_ok
:webapps_missing
echo [ms-deploy] webapps directory not found: !WEBAPPS!
exit /b 1
:webapps_ok

if defined GRADLE_HOME_OVERRIDE set "GRADLE_HOME=!GRADLE_HOME_OVERRIDE!"
if defined GRADLE_HOME set "GRADLE=!GRADLE_HOME!\bin\gradle.bat"
if not exist "!GRADLE!" for /f "delims=" %%G in ('where gradle.bat 2^>nul') do (
    set "GRADLE=%%G"
    goto :gradle_ok
)
:gradle_ok
if not exist "!GRADLE!" goto :gradle_missing
goto :gradle_found
:gradle_missing
echo [ms-deploy] gradle not found.
exit /b 1
:gradle_found

cd /d "!PROJECT_HOME!"
echo [ms-deploy] Building !SRC_WAR! ...
call "!GRADLE!" :tcf-util:build :tcf-core:build :tcf-web:build :!MODULE!:bootWar
if errorlevel 1 exit /b 1

set "SRC=!PROJECT_HOME!\!MODULE!\build\libs\!SRC_WAR!"
if not exist "!SRC!" (
    echo [ms-deploy] WAR not found: !SRC!
    exit /b 1
)

if exist "!WEBAPPS!\!CTX!" rmdir /s /q "!WEBAPPS!\!CTX!" 2>nul

echo [ms-deploy] Copying !DEST_WAR! to !WEBAPPS! ...
copy /Y "!SRC!" "!WEBAPPS!\!DEST_WAR!" >nul
echo   deployed !DEST_WAR!

echo.
echo [ms-deploy] Verifying deployed WAR ...
if not exist "!WEBAPPS!\!DEST_WAR!" goto :verify_failed
echo   [OK] !DEST_WAR!
dir "!WEBAPPS!\!DEST_WAR!"
echo.
echo [ms-deploy] Done. Tomcat running: /ms redeploys automatically ^(~15s^).
exit /b 0

:verify_failed
echo [ms-deploy] Verification failed: !DEST_WAR! not found in !WEBAPPS!
exit /b 1

:usage
echo Usage: deploy.bat
echo   Build ms.war and deploy to Tomcat webapps ^(/ms^)
echo.
echo Target webapps:
echo   !WEBAPPS!
echo   ^(override: set TOMCAT_WEBAPPS=...^)
exit /b 0

