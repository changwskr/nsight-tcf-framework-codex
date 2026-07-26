@echo off
setlocal
cd /d "%~dp0.."
gradle :tcf-ui:bootRun
