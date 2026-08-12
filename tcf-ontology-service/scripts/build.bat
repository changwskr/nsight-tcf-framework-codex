@echo off
setlocal

rem ============================================================
rem  tcf-ontology-service build / seed helper
rem
rem  Usage:
rem    scripts\build.bat                         clean test war (default)
rem    scripts\build.bat seedPdmg
rem    scripts\build.bat validatePdmg
rem    scripts\build.bat seedPdmg validatePdmg
rem
rem  Always runs from tcf-ontology-service (this module's gradlew),
rem  NOT from the monorepo root.
rem ============================================================

pushd "%~dp0.."
if errorlevel 1 goto :no_project

if not exist "gradlew.bat" goto :no_wrapper

set "GRADLE_TASKS=clean test war"
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
if not exist "build\libs\*.war" goto :done
echo.
echo  Artifacts:
for %%F in ("build\libs\*.war") do echo   - "%%~fF"  %%~zF bytes

:done
popd
endlocal & exit /b 0

:failed
echo [FAIL] build failed - exit=%BUILD_RESULT%
popd
endlocal & exit /b 1

:no_wrapper
echo [ERROR] gradlew.bat not found in "%CD%"
echo         Run this script from tcf-ontology-service\scripts\
popd
endlocal & exit /b 1

:no_project
echo [ERROR] cannot enter project home "%~dp0.."
endlocal & exit /b 1
