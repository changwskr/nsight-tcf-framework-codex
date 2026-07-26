@echo off
setlocal
cd /d "%~dp0"
echo [gw-run-local] profile=local port=8100 downstream=bootrun ^(per-module ports^)
call build.bat run-local
exit /b %errorlevel%
