@echo off
setlocal
chcp 65001 >nul
cd /d "%~dp0\..\.."
set OVERWRITE=%1
if /I "%OVERWRITE%"=="overwrite" (
  echo [seed] generate drafts + overwrite curated mappings
  call gradlew.bat --no-daemon bootRun --args="--nhnis.ontology.job=seed --nhnis.ontology.seed.overwrite=true"
) else (
  echo [seed] generate drafts; create curated only if missing
  call gradlew.bat --no-daemon bootRun --args="--nhnis.ontology.job=seed --nhnis.ontology.seed.overwrite=false"
)
echo drafts: ontology\mappings\_generated\
echo report: test-data\queries\last-seed-report.json
endlocal
