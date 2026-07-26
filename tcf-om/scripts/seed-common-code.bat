@echo off
setlocal enabledelayedexpansion

set "PROJECT_HOME=%~dp0..\.."
for %%I in ("!PROJECT_HOME!") do set "PROJECT_HOME=%%~fI"
set "DB_PATH=!PROJECT_HOME!\data\nsight-txlog\nsight_om"
set "SQL_FILE=%~dp0seed-common-code.sql"

for /f "delims=" %%J in ('dir /s /b "%USERPROFILE%\.gradle\caches\modules-2\files-2.1\com.h2database\h2\h2-*.jar" 2^>nul ^| sort /r') do (
    set "H2_JAR=%%J"
    goto :h2_ok
)
echo [seed-common-code] h2 jar not found.
exit /b 1

:h2_ok
echo [seed-common-code] DB=!DB_PATH!
java -cp "!H2_JAR!" org.h2.tools.RunScript -url "jdbc:h2:file:!DB_PATH!;MODE=Oracle;AUTO_SERVER=TRUE" -user sa -script "!SQL_FILE!"
exit /b %errorlevel%
