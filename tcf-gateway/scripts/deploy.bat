@echo off
setlocal enabledelayedexpansion

set "PROJECT_HOME=%~dp0..\.."
for %%I in ("!PROJECT_HOME!") do set "PROJECT_HOME=%%~fI"
if exist "!PROJECT_HOME!\scripts\env-jdk21.bat" call "!PROJECT_HOME!\scripts\env-jdk21.bat"
set "WEBAPPS=!PROJECT_HOME!\ztomcat\apache-tomcat-10.1.34\webapps"
set "GRADLE_HOME=C:\Programming(23-08-15)\gradle-8.10.1"
set "GRADLE=!GRADLE_HOME!\bin\gradle.bat"
set "MODULE=tcf-gateway"
set "SRC_WAR=gw.war"
set "DEST_WAR=gw.war"
set "CTX=gw"

if defined TOMCAT_WEBAPPS set "WEBAPPS=!TOMCAT_WEBAPPS!"

if not exist "!WEBAPPS!" (
    echo [gw-deploy] webapps directory not found: !WEBAPPS!
    exit /b 1
)

if defined GRADLE_HOME_OVERRIDE set "GRADLE_HOME=!GRADLE_HOME_OVERRIDE!"
if defined GRADLE_HOME set "GRADLE=!GRADLE_HOME!\bin\gradle.bat"
if not exist "!GRADLE!" (
    echo [gw-deploy] gradle not found.
    exit /b 1
)

cd /d "!PROJECT_HOME!"
echo [gw-deploy] Building !SRC_WAR! ...
call "!GRADLE!" :!MODULE!:bootWar
if errorlevel 1 exit /b 1

set "SRC=!PROJECT_HOME!\!MODULE!\build\libs\!SRC_WAR!"
if not exist "!SRC!" (
    echo [gw-deploy] WAR not found: !SRC!
    exit /b 1
)

if exist "!WEBAPPS!\!CTX!" rmdir /s /q "!WEBAPPS!\!CTX!" 2>nul

echo [gw-deploy] Copying !DEST_WAR! to !WEBAPPS! ...
copy /Y "!SRC!" "!WEBAPPS!\!DEST_WAR!" >nul
echo   deployed !DEST_WAR!
exit /b 0
