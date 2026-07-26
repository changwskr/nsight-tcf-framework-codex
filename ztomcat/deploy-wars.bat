@echo off
setlocal EnableDelayedExpansion
cd /d "%~dp0"

set "DEPLOY_PS1=%~dp0..\tcf-cicd\local\script\deploy-wars.ps1"
if not exist "!DEPLOY_PS1!" (
  echo [ztomcat] deploy script not found: !DEPLOY_PS1!
  exit /b 1
)

powershell -NoProfile -ExecutionPolicy Bypass -File "!DEPLOY_PS1!" -SkipSync -SyncProfile dev -SkipBatchCollect %*
exit /b %errorlevel%
