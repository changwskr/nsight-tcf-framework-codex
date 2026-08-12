@echo off
setlocal
chcp 65001 >nul
cd /d "%~dp0\..\.."
set TARGET=%1
if "%TARGET%"=="" set TARGET=mgcoa9001
echo [prompt] export ontology context for %TARGET% ...
call gradlew.bat --no-daemon bootRun --args="--nhnis.ontology.job=prompt --nhnis.ontology.target=%TARGET%"
echo output: test-data\queries\prompt-context-%TARGET%.md
endlocal
