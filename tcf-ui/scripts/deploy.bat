@echo off
setlocal enabledelayedexpansion

set "PROJECT_HOME=%~dp0..\.."
for %%I in ("!PROJECT_HOME!") do set "PROJECT_HOME=%%~fI"
if exist "!PROJECT_HOME!\scripts\env-jdk21.bat" call "!PROJECT_HOME!\scripts\env-jdk21.bat"
set "DEPLOY_DIR=!PROJECT_HOME!\tcf-ui\deploy"
set "GRADLE_HOME=C:\Programming(23-08-15)\gradle-8.10.1"
set "GRADLE=!GRADLE_HOME!\bin\gradle.bat"
set "MODULE=tcf-ui"
set "SRC_JAR=tcf-ui.jar"
set "DEST_JAR=tcf-ui.jar"

if defined TCF_UI_DEPLOY_DIR set "DEPLOY_DIR=!TCF_UI_DEPLOY_DIR!"

if /i "%~1"=="help" goto :usage
if /i "%~1"=="/?" goto :usage
if /i "%~1"=="-h" goto :usage

if not exist "!DEPLOY_DIR!" mkdir "!DEPLOY_DIR!"
if not exist "!DEPLOY_DIR!" goto :deploy_dir_missing
goto :deploy_dir_ok
:deploy_dir_missing
echo [tcf-ui-deploy] deploy directory not found: !DEPLOY_DIR!
exit /b 1
:deploy_dir_ok

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
echo [tcf-ui-deploy] gradle not found.
exit /b 1
:gradle_found

cd /d "!PROJECT_HOME!"
echo [tcf-ui-deploy] Building !SRC_JAR! ...
call "!GRADLE!" :!MODULE!:bootJar
if errorlevel 1 exit /b 1

set "SRC=!PROJECT_HOME!\!MODULE!\build\libs\!SRC_JAR!"
if not exist "!SRC!" (
    echo [tcf-ui-deploy] JAR not found: !SRC!
    exit /b 1
)

echo [tcf-ui-deploy] Copying !DEST_JAR! to !DEPLOY_DIR! ...
copy /Y "!SRC!" "!DEPLOY_DIR!\!DEST_JAR!" >nul
echo   deployed !DEST_JAR!

echo.
echo [tcf-ui-deploy] Verifying deployed JAR ...
if not exist "!DEPLOY_DIR!\!DEST_JAR!" goto :verify_failed
echo   [OK] !DEST_JAR!
dir "!DEPLOY_DIR!\!DEST_JAR!"
echo.
echo [tcf-ui-deploy] Done. Run: java -jar !DEPLOY_DIR!\!DEST_JAR! ^(port 8099^)
exit /b 0

:verify_failed
echo [tcf-ui-deploy] Verification failed: !DEST_JAR! not found in !DEPLOY_DIR!
exit /b 1

:usage
echo Usage: deploy.bat
echo   Build tcf-ui.jar and copy to deploy directory
echo.
echo Target deploy dir:
echo   !DEPLOY_DIR!
echo   ^(override: set TCF_UI_DEPLOY_DIR=...^)
exit /b 0
