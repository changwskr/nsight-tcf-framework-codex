@echo off

setlocal EnableDelayedExpansion

cd /d "%~dp0.."



if "%~1"=="" goto usage



set "CODE=%~1"

for %%A in ("%CODE%") do set "CODE=%%~A"

set "CODE=%CODE:"=%"

for %%A in ("%CODE%") do set "CODE=%%~A"



set "PORT="

if /i "%CODE%"=="ic" set "PORT=8082"

if /i "%CODE%"=="pc" set "PORT=8083"

if /i "%CODE%"=="ms" set "PORT=8085"

if /i "%CODE%"=="sv" set "PORT=8086"

if /i "%CODE%"=="pd" set "PORT=8087"

if /i "%CODE%"=="eb" set "PORT=8089"

if /i "%CODE%"=="ep" set "PORT=8090"

if /i "%CODE%"=="ss" set "PORT=8093"

if /i "%CODE%"=="mg" set "PORT=8096"

if /i "%CODE%"=="om" set "PORT=8097"



if "%PORT%"=="" (

  echo [curl] Unknown business code: %CODE%

  goto usage

)



set "BODY=@tcf-ui\src\main\resources\sample-requests\%CODE%-sample-inquiry.json"

if not exist "%BODY:~1%" (

  echo [curl] Sample file not found: %BODY%

  exit /b 1

)



echo [curl] POST http://localhost:%PORT%/%CODE%/online

curl.exe -s -X POST "http://localhost:%PORT%/%CODE%/online" ^

  -H "Content-Type: application/json" ^

  --data-binary "%BODY%"

echo.

exit /b 0



:usage

echo.

echo Usage: curl-sample.bat ^<code^>

echo.

echo Codes: ic pc ms sv pd eb ep ss mg om

echo Example: curl-sample.bat sv

echo.

exit /b 1

