@echo off
setlocal enabledelayedexpansion

rem sv-service Kubernetes delete
rem Usage: k8s-delete.bat
rem   k8s-delete.bat --keep-ns

set "SCRIPT_DIR=%~dp0"
if not defined NAMESPACE set "NAMESPACE=nsight"
set "APP=nsight-sv"
set "KEEP_NS=0"

:parse_args
if "%~1"=="" goto :args_done
if /i "%~1"=="help" goto :usage
if /i "%~1"=="/?" goto :usage
if /i "%~1"=="-h" goto :usage
if /i "%~1"=="--help" goto :usage
if /i "%~1"=="--keep-ns" (
    set "KEEP_NS=1"
    shift
    goto :parse_args
)
echo [sv-k8s-delete] unknown arg: %~1
exit /b 1

:args_done

where kubectl >nul 2>&1
if errorlevel 1 (
    echo [sv-k8s-delete] kubectl not found.
    exit /b 1
)

echo [sv-k8s-delete] delete resources in namespace=!NAMESPACE!
kubectl delete -f "!SCRIPT_DIR!" -n "!NAMESPACE!" --ignore-not-found=true
if errorlevel 1 exit /b %errorlevel%

if "!KEEP_NS!"=="0" (
    echo [sv-k8s-delete] namespace !NAMESPACE! is kept ^(shared^). Use kubectl delete ns !NAMESPACE! manually if needed.
)

kubectl get deploy,svc,pods -n "!NAMESPACE!" -l "app=!APP!" 2>nul
echo [sv-k8s-delete] done
exit /b 0

:usage
echo Usage: k8s-delete.bat [--keep-ns]
echo   Deletes sv-service k8s resources from namespace.
echo.
echo   --keep-ns   reserved ^(namespace is always kept; shared with other services^)
echo.
echo Env:
echo   NAMESPACE=nsight
exit /b 0
