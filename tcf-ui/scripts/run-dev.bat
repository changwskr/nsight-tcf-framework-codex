@echo off
setlocal enabledelayedexpansion

set "PROJECT_HOME=%~dp0..\.."
for %%I in ("!PROJECT_HOME!") do set "PROJECT_HOME=%%~fI"
if exist "!PROJECT_HOME!\scripts\env-jdk21.bat" call "!PROJECT_HOME!\scripts\env-jdk21.bat"
set "GRADLE_HOME=C:\Programming(23-08-15)\gradle-8.10.1"
set "GRADLE=!GRADLE_HOME!\bin\gradle.bat"
set "MODULE=tcf-ui"

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
    echo [tcf-ui-run-dev] gradle not found.
    exit /b 1
)

cd /d "!PROJECT_HOME!"
chcp 65001 >nul
set "JAVA_TOOL_OPTIONS=-Dfile.encoding=UTF-8 -Dstdout.encoding=UTF-8 -Dstderr.encoding=UTF-8"

REM UI는 8099 bootRun, 업무 API는 ztomcat 8080으로 relay (profile=dev, deployment-mode=tomcat)
set "SPRING_PROFILES_ACTIVE=dev"

echo [tcf-ui-run-dev] profile=dev port=8099 deployment-mode=tomcat
echo [tcf-ui-run-dev] UI http://localhost:8099/om/admin/login.html
echo [tcf-ui-run-dev] relay target http://localhost:8080/{code}/online ^(ztomcat WAR^)
echo [tcf-ui-run-dev] OM gateway http://localhost:8080/gw/om/online ^(JWT 시^)

echo [tcf-ui-run-dev] sync static resources...
call "!GRADLE!" :!MODULE!:classes -q
if errorlevel 1 exit /b %errorlevel%

echo [tcf-ui-run-dev] gradle :!MODULE!:bootRun ^(port 8099^)
call "!GRADLE!" :!MODULE!:bootRun -PspringProfilesActive=dev
exit /b %errorlevel%
