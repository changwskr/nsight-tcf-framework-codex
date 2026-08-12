@echo off
setlocal
chcp 65001 >nul
set "JAVA_HOME=C:\Users\chang.JWS\.jdks\temurin-21.0.4"
set "JAVA_TOOL_OPTIONS=-Dfile.encoding=UTF-8 -Dsun.jnu.encoding=UTF-8 -Dstdout.encoding=UTF-8 -Dstderr.encoding=UTF-8"

rem scripts\RUN.bat → module root (where gradlew.bat lives)
pushd "%~dp0.."
if errorlevel 1 (
  echo [ERROR] cannot enter project home "%~dp0.."
  endlocal & exit /b 1
)
if not exist "gradlew.bat" (
  echo [ERROR] gradlew.bat not found in "%CD%"
  popd
  endlocal & exit /b 1
)

echo Starting tcf-ontology-service on http://localhost:8098 (profile=local) ...
echo PROJECT : "%CD%"
call gradlew.bat --no-daemon bootRun --args="--spring.profiles.active=local"
set "RC=%ERRORLEVEL%"
popd
endlocal & exit /b %RC%
