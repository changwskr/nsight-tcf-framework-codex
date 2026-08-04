@echo off
setlocal
if not "%GRADLE_HOME%"=="" if exist "%GRADLE_HOME%\bin\gradle.bat" (
  call "%GRADLE_HOME%\bin\gradle.bat" %*
  exit /b %ERRORLEVEL%
)
where gradle >nul 2>nul
if %ERRORLEVEL%==0 (
  call gradle %*
  exit /b %ERRORLEVEL%
)
set LOCAL_GRADLE=%~dp0.gradle-bootstrap\gradle-8.10.2\bin\gradle.bat
if exist "%LOCAL_GRADLE%" (
  call "%LOCAL_GRADLE%" %*
  exit /b %ERRORLEVEL%
)
echo Gradle 8.10.2 is not installed. 1>&2
echo Run scripts\install-gradle.ps1 or set GRADLE_HOME, then retry. 1>&2
exit /b 127
