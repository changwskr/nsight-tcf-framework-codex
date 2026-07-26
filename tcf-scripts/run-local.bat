@echo off
setlocal enabledelayedexpansion
cd /d "%~dp0.."

if "%~1"=="" goto usage
if /i "%~1"=="help" goto usage
if /i "%~1"=="/?" goto usage
if /i "%~1"=="-h" goto usage

call :resolve_gradle
if errorlevel 1 exit /b 1

set "TARGET_COUNT=0"
set "RUN_ALL=0"
set "RUN_FOREGROUND="
set "BACKGROUND_TARGETS="

:arg_loop
if "%~1"=="" goto after_args
call :queue_target "%~1"
shift
goto arg_loop

:after_args
if "%RUN_ALL%"=="1" goto start_all
if %TARGET_COUNT% gtr 1 goto start_background
if defined RUN_FOREGROUND goto start_foreground
goto usage

:start_foreground
echo [run-local] "!GRADLE!" :%RUN_FOREGROUND%:bootRun
"!GRADLE!" :%RUN_FOREGROUND%:bootRun
exit /b %errorlevel%

:start_background
for %%T in (%BACKGROUND_TARGETS%) do (
  echo [run-local] start %%T in new window
  start "%%T" cmd /k "cd /d "%CD%" && "%GRADLE%" :%%T:bootRun"
)
echo [run-local] started %TARGET_COUNT% service(s)
exit /b 0

:start_all
echo [run-local] starting all business services + tcf-om...
for %%C in (ic pc ms sv pd eb ep ss mg) do (
  echo [run-local] start %%C-service
  start "%%C-service" cmd /k "cd /d "%CD%" && "%GRADLE%" :%%C-service:bootRun"
)
echo [run-local] start tcf-om
start "tcf-om" cmd /k "cd /d "%CD%" && "%GRADLE%" :tcf-om:bootRun"
echo [run-local] started 9 service(s) + tcf-om
exit /b 0

:queue_target
set "TARGET=%~1"
set /a TARGET_COUNT+=1

if /i "%TARGET%"=="all" (
  set "RUN_ALL=1"
  goto :eof
)
if /i "%TARGET%"=="ui" set "TARGET=tcf-ui"
if /i "%TARGET%"=="tcf-ui" set "TARGET=tcf-ui"
if /i "%TARGET%"=="uj" set "TARGET=tcf-uj"
if /i "%TARGET%"=="tcf-uj" set "TARGET=tcf-uj"
if /i "%TARGET%"=="tcf-om" set "TARGET=tcf-om"
if /i "%TARGET%"=="batch" set "TARGET=tcf-batch"
if /i "%TARGET%"=="tcf-batch" set "TARGET=tcf-batch"
if /i "%TARGET%"=="jwt" set "TARGET=tcf-jwt"
if /i "%TARGET%"=="tcf-jwt" set "TARGET=tcf-jwt"
if /i "%TARGET%"=="gw" set "TARGET=tcf-gateway"
if /i "%TARGET%"=="gateway" set "TARGET=tcf-gateway"
if /i "%TARGET%"=="tcf-gateway" set "TARGET=tcf-gateway"
if /i "%TARGET%"=="ud" set "TARGET=tcf-om"
if /i "%TARGET%"=="common-updownload" set "TARGET=tcf-om"
if /i "%TARGET%"=="om" set "TARGET=tcf-om"
if /i not "%TARGET:~-8%"=="-service" (
  if /i not "%TARGET%"=="tcf-ui" if /i not "%TARGET%"=="tcf-uj" if /i not "%TARGET%"=="tcf-om" if /i not "%TARGET%"=="tcf-batch" if /i not "%TARGET%"=="tcf-jwt" if /i not "%TARGET%"=="tcf-gateway" set "TARGET=%TARGET%-service"
)

if %TARGET_COUNT%==1 set "RUN_FOREGROUND=%TARGET%"
set "BACKGROUND_TARGETS=!BACKGROUND_TARGETS! %TARGET%"
goto :eof

:resolve_gradle
set "GRADLE=gradle"
if defined GRADLE_HOME_OVERRIDE (
  if exist "!GRADLE_HOME_OVERRIDE!\bin\gradle.bat" (
    set "GRADLE=!GRADLE_HOME_OVERRIDE!\bin\gradle.bat"
    goto :eof
  )
)
if defined GRADLE_HOME (
  if exist "!GRADLE_HOME!\bin\gradle.bat" (
    set "GRADLE=!GRADLE_HOME!\bin\gradle.bat"
    goto :eof
  )
)
for /f "delims=" %%G in ('where gradle.bat 2^>nul') do (
  set "GRADLE=%%G"
  goto :eof
)
where gradle >nul 2>&1
if errorlevel 1 (
  echo [run-local] gradle not found. Set GRADLE_HOME or GRADLE_HOME_OVERRIDE.
  exit /b 1
)
goto :eof

:usage
echo.
echo Usage: run-local.bat ^<target^> [target2 ...]
echo.
echo Targets:
echo   sv ic     service code (ex: sv -^> sv-service bootRun)
echo   ui        tcf-ui bootRun (port 8099)
echo   uj        tcf-uj bootRun (port 8102)
echo   om        tcf-om bootRun (port 8097)
echo   batch     tcf-batch bootRun (port 8098)
echo   ud        tcf-om bootRun (파일 업·다운로드 내장)
echo   all       start 9 *-service + tcf-om in new windows
echo.
echo Gradle: GRADLE_HOME_OVERRIDE ^> GRADLE_HOME ^> PATH
echo.
exit /b 1
