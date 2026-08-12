@echo off
setlocal
chcp 65001 >nul
cd /d "%~dp0\..\.."
echo [validate] scan + shapes/mapping validation ...
call gradlew.bat --no-daemon bootRun --args="--nhnis.ontology.job=validate"
set ERR=%ERRORLEVEL%
echo report: test-data\queries\last-validation-report.json
exit /b %ERR%
