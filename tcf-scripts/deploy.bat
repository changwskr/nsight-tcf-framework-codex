@echo off
setlocal enabledelayedexpansion

set "PROJECT_HOME=%~dp0.."
for %%I in ("!PROJECT_HOME!") do set "PROJECT_HOME=%%~fI"
set "WEBAPPS=!PROJECT_HOME!\ztomcat\apache-tomcat-10.1.34\webapps"
set "BATCH_WARS=!PROJECT_HOME!\ztomcat\wars"
set "BATCH_XML=!PROJECT_HOME!\ztomcat\conf\Catalina\localhost\batch.xml"

if defined TOMCAT_WEBAPPS set "WEBAPPS=!TOMCAT_WEBAPPS!"

call :resolve_gradle
if errorlevel 1 exit /b 1

if /i "%~1"=="help" goto :usage
if /i "%~1"=="/?" goto :usage
if /i "%~1"=="-h" goto :usage

if not exist "!WEBAPPS!" (
  echo [deploy] webapps directory not found: !WEBAPPS!
  exit /b 1
)

set "GRADLE_TASKS="
set "DEPLOY_ENTRIES="
set "BATCH_DEPLOY=0"
set "CLEAN_CTX="
set "DEPLOY_ALL=0"

if "%~1"=="" set "DEPLOY_ALL=1"
if /i "%~1"=="all" set "DEPLOY_ALL=1"

if "!DEPLOY_ALL!"=="1" goto :build_all

:collect_args
if "%~1"=="" goto :args_done
call :resolve_code "%~1"
if errorlevel 1 exit /b 1
shift
goto :collect_args

:args_done
call :prepend_tcf_libs
echo [deploy] Building selected WAR^(s^): !GRADLE_TASKS!
goto :run_build

:build_all
echo [deploy] Building all WAR files (buildBusinessWars) ...
set "GRADLE_TASKS=buildBusinessWars"
set "DEPLOY_ENTRIES=ic-service:ic.war:ic.war:ic pc-service:pc.war:pc.war:pc ms-service:ms.war:ms.war:ms sv-service:sv.war:sv.war:sv pd-service:pd.war:pd.war:pd eb-service:eb.war:eb.war:eb ep-service:ep.war:ep.war:ep ss-service:ss.war:ss.war:ss mg-service:mg.war:mg.war:mg tcf-om:tcf-om.war:om.war:om"
set "CLEAN_CTX=cc ic pc bc ms sv pd cm eb ep bp bd ss cs ct mg om"
goto :run_build

:run_build
cd /d "!PROJECT_HOME!"
call "!GRADLE!" !GRADLE_TASKS!
if errorlevel 1 exit /b 1

echo [deploy] Removing stale exploded directories ...
for %%W in (!CLEAN_CTX!) do call :clean_ctx %%W

if "!BATCH_DEPLOY!"=="1" call :sync_batch_xml

echo [deploy] Copying WAR files ...
for %%M in (!DEPLOY_ENTRIES!) do call :deploy_war %%M
if "!BATCH_DEPLOY!"=="1" call :deploy_batch_war

echo.
echo [deploy] Verifying deployed WAR files ...
set "VERIFY_COUNT=0"
set "VERIFY_FAILED=0"
for %%M in (!DEPLOY_ENTRIES!) do call :verify_war %%M
if "!BATCH_DEPLOY!"=="1" call :verify_batch_war
echo.
if !VERIFY_FAILED! gtr 0 (
  echo [deploy] Verification failed: !VERIFY_FAILED! WAR^(s^) missing
  exit /b 1
)
echo [deploy] All !VERIFY_COUNT! WAR^(s^) verified

if "!DEPLOY_ALL!"=="1" (
  echo [deploy] Done. For batch/ui use: deploy.bat batch ui  or  ztomcat\deploy-wars.bat all
  echo [deploy] Restart Tomcat if it is already running.
) else (
  echo [deploy] Done. Tomcat running: context redeploys automatically ^(~15s^).
)
exit /b 0

:prepend_tcf_libs
if defined GRADLE_TASKS (
  set "GRADLE_TASKS=:tcf-util:build :tcf-core:build :tcf-web:build !GRADLE_TASKS!"
) else (
  set "GRADLE_TASKS=:tcf-util:build :tcf-core:build :tcf-web:build"
)
exit /b 0

:clean_ctx
if exist "!WEBAPPS!\%~1" rmdir /s /q "!WEBAPPS!\%~1" 2>nul
exit /b 0

:deploy_war
for /f "tokens=1,2,3,4 delims=:" %%A in ("%~1") do (
    set "SRC=!PROJECT_HOME!\%%A\build\libs\%%B"
    if exist "!SRC!" (
        copy /Y "!SRC!" "!WEBAPPS!\%%C" >nul
        echo   deployed %%C ^(from %%B^)
    ) else (
        echo   missing %%B in %%A
    )
)
exit /b 0

:deploy_batch_war
set "SRC=!PROJECT_HOME!\tcf-batch\build\libs\tcf-batch.war"
if not exist "!BATCH_WARS!" mkdir "!BATCH_WARS!"
if exist "!SRC!" (
    copy /Y "!SRC!" "!BATCH_WARS!\zz-batch.war" >nul
    echo   deployed wars/zz-batch.war ^(from tcf-batch.war^)
    if exist "!WEBAPPS!\batch" rmdir /s /q "!WEBAPPS!\batch" 2>nul
) else (
    echo   missing tcf-batch.war
)
exit /b 0

:sync_batch_xml
if not exist "!PROJECT_HOME!\ztomcat\conf\Catalina\localhost" mkdir "!PROJECT_HOME!\ztomcat\conf\Catalina\localhost"
set "WAR_PATH=!BATCH_WARS:\=/!"
set "WAR_PATH=!WAR_PATH!/zz-batch.war"
(
  echo ^<?xml version="1.0" encoding="UTF-8"?^>
  echo ^<Context docBase="!WAR_PATH!" /^>
) > "!BATCH_XML!"
exit /b 0

:verify_war
for /f "tokens=1,2,3,4 delims=:" %%A in ("%~1") do (
    if exist "!WEBAPPS!\%%C" (
        set /a VERIFY_COUNT+=1
        echo   [OK] %%C
    ) else (
        set /a VERIFY_FAILED+=1
        echo   [MISSING] %%C
    )
)
exit /b 0

:verify_batch_war
if exist "!BATCH_WARS!\zz-batch.war" (
    set /a VERIFY_COUNT+=1
    echo   [OK] wars/zz-batch.war
) else (
    set /a VERIFY_FAILED+=1
    echo   [MISSING] wars/zz-batch.war
)
exit /b 0

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
  echo [deploy] gradle not found. Set GRADLE_HOME or GRADLE_HOME_OVERRIDE.
  exit /b 1
)
goto :eof

:usage
echo Usage:
echo   deploy.bat              Build and deploy business WARs + om (17)
echo   deploy.bat all          Same as above
echo   deploy.bat sv           Build and deploy one code
echo   deploy.bat sv cc om     Build and deploy multiple codes
echo   deploy.bat batch ui     tcf-batch ^(wars/zz-batch.war^) + tcf-ui
echo.
echo Target webapps: !WEBAPPS!
echo   ^(override: set TOMCAT_WEBAPPS=...^)
echo.
echo Codes: ic pc ms sv pd eb ep ss mg om ui uj jwt gw batch
echo Full 19 WAR: ztomcat\deploy-wars.bat all
exit /b 0

:resolve_code
set "CODE=%~1"
if /i "!CODE!"=="ic" call :add_entry ic-service ic.war ic.war ic & exit /b 0
if /i "!CODE!"=="pc" call :add_entry pc-service pc.war pc.war pc & exit /b 0
if /i "!CODE!"=="ms" call :add_entry ms-service ms.war ms.war ms & exit /b 0
if /i "!CODE!"=="sv" call :add_entry sv-service sv.war sv.war sv & exit /b 0
if /i "!CODE!"=="pd" call :add_entry pd-service pd.war pd.war pd & exit /b 0
if /i "!CODE!"=="eb" call :add_entry eb-service eb.war eb.war eb & exit /b 0
if /i "!CODE!"=="ep" call :add_entry ep-service ep.war ep.war ep & exit /b 0
if /i "!CODE!"=="ss" call :add_entry ss-service ss.war ss.war ss & exit /b 0
if /i "!CODE!"=="mg" call :add_entry mg-service mg.war mg.war mg & exit /b 0
if /i "!CODE!"=="om" call :add_entry tcf-om tcf-om.war om.war om & exit /b 0
if /i "!CODE!"=="tcf-om" call :add_entry tcf-om tcf-om.war om.war om & exit /b 0
if /i "!CODE!"=="ud" call :add_entry tcf-om tcf-om.war om.war om & exit /b 0
if /i "!CODE!"=="ui" call :add_entry tcf-ui tcf-ui.war ui.war ui & exit /b 0
if /i "!CODE!"=="tcf-ui" call :add_entry tcf-ui tcf-ui.war ui.war ui & exit /b 0
if /i "!CODE!"=="uj" call :add_entry tcf-uj tcf-uj.war uj.war uj & exit /b 0
if /i "!CODE!"=="tcf-uj" call :add_entry tcf-uj tcf-uj.war uj.war uj & exit /b 0
if /i "!CODE!"=="jwt" call :add_entry tcf-jwt jwt.war jwt.war jwt & exit /b 0
if /i "!CODE!"=="tcf-jwt" call :add_entry tcf-jwt jwt.war jwt.war jwt & exit /b 0
if /i "!CODE!"=="gw" call :add_entry tcf-gateway gw.war gw.war gw & exit /b 0
if /i "!CODE!"=="gateway" call :add_entry tcf-gateway gw.war gw.war gw & exit /b 0
if /i "!CODE!"=="tcf-gateway" call :add_entry tcf-gateway gw.war gw.war gw & exit /b 0
if /i "!CODE!"=="batch" call :add_batch & exit /b 0
if /i "!CODE!"=="tcf-batch" call :add_batch & exit /b 0
echo [deploy] Unknown code: !CODE!
echo [deploy] Codes: ic pc ms sv pd eb ep ss mg om ui jwt batch
exit /b 1

:add_entry
if defined GRADLE_TASKS (
  set "GRADLE_TASKS=!GRADLE_TASKS! :%~1:bootWar"
) else (
  set "GRADLE_TASKS=:%~1:bootWar"
)
set "DEPLOY_ENTRIES=!DEPLOY_ENTRIES! %~1:%~2:%~3:%~4"
set "CLEAN_CTX=!CLEAN_CTX! %~4"
exit /b 0

:add_batch
set "BATCH_DEPLOY=1"
if defined GRADLE_TASKS (
  set "GRADLE_TASKS=!GRADLE_TASKS! :tcf-batch:bootWar"
) else (
  set "GRADLE_TASKS=:tcf-batch:bootWar"
)
set "CLEAN_CTX=!CLEAN_CTX! batch"
exit /b 0
