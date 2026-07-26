@echo off
setlocal enabledelayedexpansion

rem sv-service Kubernetes apply
rem Usage: k8s-apply.bat
rem   set NAMESPACE=nsight
rem   set WAIT=0   (skip rollout wait)

set "SCRIPT_DIR=%~dp0"
if not defined NAMESPACE set "NAMESPACE=nsight"
if not defined WAIT set "WAIT=1"
set "APP=nsight-sv"

if /i "%~1"=="help" goto :usage
if /i "%~1"=="/?" goto :usage
if /i "%~1"=="-h" goto :usage
if /i "%~1"=="--help" goto :usage

where kubectl >nul 2>&1
if errorlevel 1 (
    echo [sv-k8s-apply] kubectl not found. Install kubectl and configure cluster access.
    exit /b 1
)

echo [sv-k8s-apply] checking cluster connectivity...
kubectl cluster-info >nul 2>&1
if errorlevel 1 (
    echo [sv-k8s-apply] ERROR: cannot reach Kubernetes API ^(usually 127.0.0.1:6443^).
    echo.
    echo   Current context:
    kubectl config current-context 2>nul
    echo.
    echo   Fix for Docker Desktop:
    echo     1^) Open Docker Desktop
    echo     2^) Settings -^> Kubernetes -^> Enable Kubernetes
    echo     3^) Apply ^& Restart, wait until Kubernetes is running
    echo     4^) Verify: kubectl get nodes
    echo     5^) Re-run this script
    echo.
    echo   Or switch to a reachable context:
    echo     kubectl config get-contexts
    echo     kubectl config use-context ^<name^>
    exit /b 1
)

echo [sv-k8s-apply] namespace=!NAMESPACE!
kubectl create namespace "!NAMESPACE!" --dry-run=client -o yaml | kubectl apply -f -
if errorlevel 1 exit /b %errorlevel%

echo [sv-k8s-apply] apply !SCRIPT_DIR!
kubectl apply -f "!SCRIPT_DIR!" -n "!NAMESPACE!"
if errorlevel 1 exit /b %errorlevel%

if not "!WAIT!"=="0" (
    echo [sv-k8s-apply] waiting for deployment/!APP! rollout...
    kubectl rollout status "deployment/!APP!" -n "!NAMESPACE!" --timeout=300s
    if errorlevel 1 exit /b %errorlevel%
)

echo.
kubectl get deploy,svc,pods -n "!NAMESPACE!" -l "app=!APP!"
echo [sv-k8s-apply] done
exit /b 0

:usage
echo Usage: k8s-apply.bat
echo   Creates namespace ^(if needed^) and applies sv-service/scripts/k8s manifests.
echo.
echo Env:
echo   NAMESPACE=nsight
echo   WAIT=1          set WAIT=0 to skip rollout wait
exit /b 0
