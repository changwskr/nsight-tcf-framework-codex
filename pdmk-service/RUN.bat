@echo off
setlocal
chcp 65001 >nul
set "JAVA_HOME=C:\Users\chang.JWS\.jdks\temurin-21.0.4"
set "JAVA_TOOL_OPTIONS=-Dfile.encoding=UTF-8 -Dsun.jnu.encoding=UTF-8 -Dstdout.encoding=UTF-8 -Dstderr.encoding=UTF-8"
cd /d "%~dp0"
echo Starting pdmk-service on http://localhost:8080 ...
call gradlew.bat --no-daemon bootRun
endlocal
