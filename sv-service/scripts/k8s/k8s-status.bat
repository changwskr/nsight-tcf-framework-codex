@echo off
setlocal enabledelayedexpansion

rem sv-service Kubernetes status
rem Usage: k8s-status.bat

if not defined NAMESPACE set "NAMESPACE=nsight"
set "APP=nsight-sv"

if /i "%~1"=="help" goto :usage
if /i "%~1"=="/?" goto :usage
if /i "%~1"=="-h" goto :usage
if /i "%~1"=="--help" goto :usage

where kubectl >nul 2>&1
if errorlevel 1 (
    echo [sv-k8s-status] kubectl not found.
    exit /b 1
)

echo [sv-k8s-status] namespace=!NAMESPACE! app=!APP!
echo.
echo === Deploy / Service / Pod ===
kubectl get deploy,svc,pods -n "!NAMESPACE!" -l "app=!APP!" -o wide
echo.
echo === ConfigMap / Secret ===
kubectl get configmap,secret -n "!NAMESPACE!" -l "app=!APP!"
echo.
echo === Recent events ===
kubectl get events -n "!NAMESPACE!" --field-selector "involvedObject.name=!APP!" --sort-by=.lastTimestamp 2>nul
echo.
kubectl get deployment "!APP!" -n "!NAMESPACE!" >nul 2>&1
if not errorlevel 1 (
    echo === Rollout ===
    kubectl rollout status "deployment/!APP!" -n "!NAMESPACE!" --timeout=5s 2>nul
    echo.
    echo === Describe deployment ===
    kubectl describe deployment "!APP!" -n "!NAMESPACE!"
)
exit /b 0

:usage
echo Usage: k8s-status.bat
echo.
echo Env:
echo   NAMESPACE=nsight
exit /b 0
