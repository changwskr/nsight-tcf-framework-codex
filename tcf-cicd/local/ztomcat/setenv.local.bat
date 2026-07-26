@echo off
rem ztomcat 로컬 JVM 설정 (Tomcat setenv.bat 에서 call)
rem Spring 프로파일은 conf/setenv.* 가 주입 (dev). 운영은 conf\setenv.prod.* 참고.

if not defined JAVA_HOME (
    if exist "%USERPROFILE%\.jdks\temurin-21.0.4" (
        set "JAVA_HOME=%USERPROFILE%\.jdks\temurin-21.0.4"
    )
)

if not defined JAVA_HOME (
    echo [ztomcat] JAVA_HOME is not set. Install JDK 21 or set JAVA_HOME.
    exit /b 1
)

set "CATALINA_OPTS=-Xms512m -Xmx1536m -Dfile.encoding=UTF-8 -Duser.timezone=Asia/Seoul"
if not defined NSIGHT_TXLOG_PATH (
    for %%I in ("%~dp0..\..\..") do set "NSIGHT_TXLOG_PATH=%%~fI\data\nsight-txlog"
)
if defined NSIGHT_TXLOG_PATH (
    set "CATALINA_OPTS=%CATALINA_OPTS% -Dnsight.txlog.path=%NSIGHT_TXLOG_PATH:\=/%"
)
