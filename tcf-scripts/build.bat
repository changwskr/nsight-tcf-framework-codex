@echo off
setlocal EnableDelayedExpansion
cd /d "%~dp0.."

call :resolve_gradle
if errorlevel 1 exit /b 1

if "%~1"=="" goto usage
if /i "%~1"=="help" goto usage
if /i "%~1"=="/?" goto usage
if /i "%~1"=="-h" goto usage

echo [build] Stop Gradle daemons...
call "!GRADLE!" --stop >nul 2>&1

set "GRADLE_TASKS="

:arg_loop
if "%~1"=="" goto run_build
call :resolve_target "%~1"
shift
goto arg_loop

:run_build
if not defined GRADLE_TASKS goto usage
echo [build] "!GRADLE!" %GRADLE_TASKS%
"!GRADLE!" %GRADLE_TASKS%
exit /b %errorlevel%

:resolve_target
set "TARGET=%~1"

if /i "%TARGET%"=="all" (
  set "GRADLE_TASKS=clean buildBusinessWars"
  goto :eof
)
if /i "%TARGET%"=="wars" (call :append_task buildBusinessWars & goto :eof)
if /i "%TARGET%"=="ztomcat" (call :append_task buildZtomcatWars & goto :eof)
if /i "%TARGET%"=="tcf" (
  call :append_task :tcf-util:build
  call :append_task :tcf-core:build
  call :append_task :tcf-web:build
  goto :eof
)
if /i "%TARGET%"=="common" (
  echo [build] common-etc module was removed. Use tcf-om for shared features.
  exit /b 1
)
if /i "%TARGET%"=="ui" (call :append_task :tcf-ui:bootJar & goto :eof)
if /i "%TARGET%"=="tcf-ui" (call :append_task :tcf-ui:bootJar & goto :eof)
if /i "%TARGET%"=="uj" (call :append_task :tcf-uj:bootJar & goto :eof)
if /i "%TARGET%"=="tcf-uj" (call :append_task :tcf-uj:bootJar & goto :eof)
if /i "%TARGET%"=="batch" (call :append_task :tcf-batch:bootWar & goto :eof)
if /i "%TARGET%"=="tcf-batch" (call :append_task :tcf-batch:bootWar & goto :eof)
if /i "%TARGET%"=="services" (
  call :append_task :ic-service:build
  call :append_task :pc-service:build
  call :append_task :ms-service:build
  call :append_task :sv-service:build
  call :append_task :pd-service:build
  call :append_task :eb-service:build
  call :append_task :ep-service:build
  call :append_task :ss-service:build
  call :append_task :mg-service:build
  call :append_task :tcf-om:bootWar
  goto :eof
)
if /i "%TARGET%"=="tcf-om" (call :append_task :tcf-om:bootWar & goto :eof)
if /i "%TARGET%"=="om" (call :append_task :tcf-om:bootWar & goto :eof)
if /i "%TARGET%"=="gw" (call :append_task :tcf-gateway:bootWar & goto :eof)
if /i "%TARGET%"=="gateway" (call :append_task :tcf-gateway:bootWar & goto :eof)
if /i "%TARGET%"=="tcf-gateway" (call :append_task :tcf-gateway:bootWar & goto :eof)
if /i "%TARGET%"=="tcf-util" (call :append_task :tcf-util:build & goto :eof)
if /i "%TARGET%"=="tcf-core" (call :append_task :tcf-core:build & goto :eof)
if /i "%TARGET%"=="tcf-web" (call :append_task :tcf-web:build & goto :eof)
if /i "%TARGET%"=="tcf-cache" (call :append_task :tcf-cache:build & goto :eof)

set "SERVICE=%TARGET%"
if /i not "%TARGET:~-8%"=="-service" set "SERVICE=%TARGET%-service"
call :append_task :!SERVICE!:build
goto :eof

:append_task
set "GRADLE_TASKS=%GRADLE_TASKS% %~1"
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
  echo [build] gradle not found. Set GRADLE_HOME or GRADLE_HOME_OVERRIDE.
  exit /b 1
)
goto :eof

:usage
echo.
echo Usage: build.bat ^<target^> [target2 ...]
echo.
echo Targets:
echo   all      clean + buildBusinessWars (17 WAR)
echo   wars     buildBusinessWars only
echo   ztomcat  buildZtomcatWars (19 WAR: + batch + ui)
echo   tcf      tcf-util, tcf-core, tcf-web
echo   tcf-util tcf-core tcf-web tcf-cache  (library modules)
echo   ui       tcf-ui bootJar
echo   batch    tcf-batch bootWar
echo   services all *-service modules + tcf-om bootWar
echo   sv ic    service code (ex: sv -^> sv-service)
echo   tcf-om   tcf-om bootWar
echo.
echo Gradle: GRADLE_HOME_OVERRIDE ^> GRADLE_HOME ^> PATH
echo.
exit /b 1
