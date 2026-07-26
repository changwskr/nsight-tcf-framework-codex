@echo off
setlocal
set "ZTOMCAT_HOME=%~dp0"
set "TOMCAT_VERSION=10.1.34"
set "TOMCAT_DIR=apache-tomcat-%TOMCAT_VERSION%"
set "ZIP_FILE=%ZTOMCAT_HOME%%TOMCAT_DIR%-windows-x64.zip"
set "DOWNLOAD_URL=https://archive.apache.org/dist/tomcat/tomcat-10/v%TOMCAT_VERSION%/bin/%TOMCAT_DIR%-windows-x64.zip"

if exist "%ZTOMCAT_HOME%%TOMCAT_DIR%\bin\catalina.bat" (
    echo [ztomcat] Already installed: %TOMCAT_DIR%
    exit /b 0
)

echo [ztomcat] Downloading %TOMCAT_DIR% ...
powershell -NoProfile -Command ^
    "$ProgressPreference='SilentlyContinue';" ^
    "Invoke-WebRequest -Uri '%DOWNLOAD_URL%' -OutFile '%ZIP_FILE%'"

if errorlevel 1 (
    echo [ztomcat] Download failed.
    exit /b 1
)

echo [ztomcat] Extracting ...
powershell -NoProfile -Command ^
    "Expand-Archive -Path '%ZIP_FILE%' -DestinationPath '%ZTOMCAT_HOME%' -Force"

del /Q "%ZIP_FILE%" >nul 2>&1
echo [ztomcat] Installed to %ZTOMCAT_HOME%%TOMCAT_DIR%
