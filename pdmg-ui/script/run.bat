@echo off
setlocal

rem ============================================================
rem  pdmg-ui run script (port 8090)
rem
rem  Usage:
rem    run.bat                                   gradle bootRun
rem    run.bat jar                               run build\libs\pdmg-ui.jar
rem    run.bat jar --pdmg.ui.target-base-url=http://localhost:8081
rem
rem  Build first with script\build.bat when using jar mode.
rem  Prefer RUN.bat / bootRun for local development.
rem
rem  Note: this file stays ASCII only. Korean text saved as UTF-8 is
rem  re-read as CP949 by cmd.exe and breaks the parser, even inside rem.
rem  The project path may contain parentheses, so path variables are
rem  always quoted and never expanded inside a ( ) block.
rem ============================================================

pushd "%~dp0.."
if errorlevel 1 goto :no_project

chcp 65001 >nul
if not defined JAVA_HOME set "JAVA_HOME=C:\Users\chang.JWS\.jdks\temurin-21.0.4"
set "JAVA_TOOL_OPTIONS=-Dfile.encoding=UTF-8 -Dsun.jnu.encoding=UTF-8 -Dstdout.encoding=UTF-8 -Dstderr.encoding=UTF-8"

if /i "%~1"=="jar" goto :run_jar

echo ------------------------------------------------------------
echo  PROJECT : "%CD%"
echo  URL     : http://localhost:8090
echo  TARGET  : pdmg-service http://localhost:8080
echo ------------------------------------------------------------
echo.

call gradlew.bat --no-daemon bootRun
set "RUN_RESULT=%ERRORLEVEL%"
popd
endlocal & exit /b %RUN_RESULT%

:run_jar
shift
if not exist "build\libs\pdmg-ui.jar" goto :no_jar

echo ------------------------------------------------------------
echo  PROJECT : "%CD%"
echo  URL     : http://localhost:8090
echo ------------------------------------------------------------
echo.

java -Dfile.encoding=UTF-8 -jar "build\libs\pdmg-ui.jar" %*
set "RUN_RESULT=%ERRORLEVEL%"

popd
endlocal & exit /b %RUN_RESULT%

:no_jar
echo [ERROR] build\libs\pdmg-ui.jar not found - run script\build.bat first
popd
endlocal & exit /b 1

:no_project
echo [ERROR] cannot enter project home "%~dp0.."
endlocal & exit /b 1
