@echo off
setlocal
chcp 65001 >nul
cd /d "%~dp0\..\.."
echo [import] scanning pdmg-* into test-data/ontology/inventory-pdmg.yml ...
call gradlew.bat --no-daemon bootRun --args="--nhnis.ontology.job=import"
endlocal
