@echo off
setlocal
cd /d "%~dp0\.."
call gradlew.bat :tcf-ai-methodology:bootRun
endlocal
