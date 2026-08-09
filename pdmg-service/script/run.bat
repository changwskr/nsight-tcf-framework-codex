@echo off
setlocal

rem ============================================================
rem  pdmg-service run script (port 8080)
rem
rem  Usage:
rem    run.bat                                   bootRun
rem    run.bat --args="--spring.profiles.active=dev"
rem
rem  build.gradle disables bootWar because the corporate deployment is a
rem  plain WAR on the WAS, so the artifact is not self-executable. For
rem  local runs we go through bootRun.
rem
rem  Java comes from JAVA_HOME / PATH of the calling shell - set it up
rem  outside this script.
rem
rem  Note: this file stays ASCII only. Korean text saved as UTF-8 is
rem  re-read as CP949 by cmd.exe and breaks the parser, even inside rem.
rem  The project path may contain parentheses, so path variables are
rem  always quoted and never expanded inside a ( ) block.
rem ============================================================

pushd "%~dp0.."
if errorlevel 1 goto :no_project

if not exist "gradlew.bat" goto :no_wrapper

chcp 65001 >nul
set "JAVA_TOOL_OPTIONS=-Dfile.encoding=UTF-8 -Dsun.jnu.encoding=UTF-8 -Dstdout.encoding=UTF-8 -Dstderr.encoding=UTF-8"

echo ------------------------------------------------------------
echo  PROJECT : "%CD%"
echo  URL     : http://localhost:8080
echo ------------------------------------------------------------
echo.

call gradlew.bat --no-daemon bootRun %*
set "RUN_RESULT=%ERRORLEVEL%"

popd
endlocal & exit /b %RUN_RESULT%

:no_wrapper
echo [ERROR] gradlew.bat not found in "%CD%"
popd
endlocal & exit /b 1

:no_project
echo [ERROR] cannot enter project home "%~dp0.."
endlocal & exit /b 1
