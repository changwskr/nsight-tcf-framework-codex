@echo off
setlocal
cd /d "%~dp0"
echo [gw-run-dev] profile=dev port=8100 downstream=tomcat ^(http://localhost:8080^)
call build.bat run-dev
exit /b %errorlevel%
