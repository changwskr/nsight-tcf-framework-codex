@echo off
setlocal

rem ============================================================
rem  pdmp-ui run script (port 8090)
rem
rem  Usage:
rem    run.bat                                   run build\libs\pdmp-ui.jar
rem    run.bat --pdmp.ui.target-base-url=http://localhost:8081
rem
rem  Build first with script\build.bat. Java comes from JAVA_HOME / PATH
rem  of the calling shell - set it up outside this script.
rem
rem  Note: this file stays ASCII only. Korean text saved as UTF-8 is
rem  re-read as CP949 by cmd.exe and breaks the parser, even inside rem.
rem  The project path may contain parentheses, so path variables are
rem  always quoted and never expanded inside a ( ) block.
rem ============================================================

pushd "%~dp0.."
if errorlevel 1 goto :no_project

if not exist "build\libs\pdmp-ui.jar" goto :no_jar

echo ------------------------------------------------------------
echo  PROJECT : "%CD%"
echo  URL     : http://localhost:8090
echo ------------------------------------------------------------
echo.

java -Dfile.encoding=UTF-8 -jar "build\libs\pdmp-ui.jar" %*
set "RUN_RESULT=%ERRORLEVEL%"

popd
endlocal & exit /b %RUN_RESULT%

:no_jar
echo [ERROR] build\libs\pdmp-ui.jar not found - run script\build.bat first
popd
endlocal & exit /b 1

:no_project
echo [ERROR] cannot enter project home "%~dp0.."
endlocal & exit /b 1
