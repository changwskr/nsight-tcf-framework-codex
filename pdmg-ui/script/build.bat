@echo off
setlocal

rem ============================================================
rem  pdmg-ui JAR build script
rem
rem  Usage:
rem    build.bat                                 clean build (default)
rem    build.bat bootJar                         run the given Gradle tasks
rem    build.bat clean build --refresh-dependencies
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

set "GRADLE_TASKS=clean build"
if not "%~1"=="" set "GRADLE_TASKS=%*"

echo ------------------------------------------------------------
echo  PROJECT : "%CD%"
echo  TASKS   : %GRADLE_TASKS%
echo ------------------------------------------------------------
echo.

call gradlew.bat --no-daemon %GRADLE_TASKS%
set "BUILD_RESULT=%ERRORLEVEL%"

echo.
if not "%BUILD_RESULT%"=="0" goto :failed

echo [OK] build succeeded
if not exist "build\libs\*.jar" goto :done
echo.
echo  Artifacts:
for %%F in ("build\libs\*.jar") do echo   - "%%~fF"  %%~zF bytes

:done
popd
endlocal & exit /b 0

:failed
echo [FAIL] build failed - exit=%BUILD_RESULT%
popd
endlocal & exit /b 1

:no_wrapper
echo [ERROR] gradlew.bat not found in "%CD%"
popd
endlocal & exit /b 1

:no_project
echo [ERROR] cannot enter project home "%~dp0.."
endlocal & exit /b 1
