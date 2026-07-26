@echo off
setlocal enabledelayedexpansion

set "PROJECT_HOME=%~dp0..\.."
for %%I in ("!PROJECT_HOME!") do set "PROJECT_HOME=%%~fI"
set "WEBAPPS=!PROJECT_HOME!\ztomcat\apache-tomcat-10.1.34\webapps"
set "GRADLE_HOME=C:\Programming(23-08-15)\gradle-8.10.1"
set "GRADLE=!GRADLE_HOME!\bin\gradle.bat"
set "MODULE=tcf-uj"
set "SRC_WAR=tcf-uj.war"
set "DEST_WAR=uj.war"
set "CTX=uj"

if defined TOMCAT_WEBAPPS set "WEBAPPS=!TOMCAT_WEBAPPS!"

if not exist "!WEBAPPS!" (
    echo [tcf-uj-deploy-war] webapps directory not found: !WEBAPPS!
    exit /b 1
)

if defined GRADLE_HOME_OVERRIDE set "GRADLE_HOME=!GRADLE_HOME_OVERRIDE!"
if defined GRADLE_HOME set "GRADLE=!GRADLE_HOME!\bin\gradle.bat"
if not exist "!GRADLE!" (
    echo [tcf-uj-deploy-war] gradle not found.
    exit /b 1
)

cd /d "!PROJECT_HOME!"
echo [tcf-uj-deploy-war] Building !SRC_WAR! ...
call "!GRADLE!" :!MODULE!:bootWar
if errorlevel 1 exit /b 1

set "SRC=!PROJECT_HOME!\!MODULE!\build\libs\!SRC_WAR!"
if not exist "!SRC!" (
    echo [tcf-uj-deploy-war] WAR not found: !SRC!
    exit /b 1
)

if exist "!WEBAPPS!\!CTX!" rmdir /s /q "!WEBAPPS!\!CTX!" 2>nul

echo [tcf-uj-deploy-war] Copying !DEST_WAR! to !WEBAPPS! ...
copy /Y "!SRC!" "!WEBAPPS!\!DEST_WAR!" >nul
echo   deployed !DEST_WAR! ^(/uj^)
exit /b 0
