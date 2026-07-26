rem Applied by ztomcat/start.ps1 on each start (WAR modules require JDK 21)
rem dev(통합 검증) 기본값. 운영 샘플: conf\setenv.prod.bat
if exist "%USERPROFILE%\.jdks\temurin-21.0.4" set "JAVA_HOME=%USERPROFILE%\.jdks\temurin-21.0.4"
set "JAVA_OPTS=%JAVA_OPTS% -Dfile.encoding=UTF-8 -Dstdout.encoding=UTF-8 -Dstderr.encoding=UTF-8 -Dsun.stdout.encoding=UTF-8 -Dsun.stderr.encoding=UTF-8 -Dsun.jnu.encoding=UTF-8"
if not defined JAVA_TOOL_OPTIONS set "JAVA_TOOL_OPTIONS=-Dfile.encoding=UTF-8 -Dstdout.encoding=UTF-8 -Dstderr.encoding=UTF-8"
if defined NSIGHT_TXLOG_PATH goto :apply_txlog_opts
if not defined CATALINA_HOME goto :skip_txlog_opts
for %%I in ("%CATALINA_HOME%\..\..\data\nsight-txlog") do set "NSIGHT_TXLOG_PATH=%%~fI"
:apply_txlog_opts
if not defined NSIGHT_TXLOG_PATH goto :skip_txlog_opts
set "CATALINA_OPTS=%CATALINA_OPTS% -Dnsight.txlog.path=%NSIGHT_TXLOG_PATH:\=/%"
:skip_txlog_opts
set "CATALINA_OPTS=%CATALINA_OPTS% -Xms512m -Xmx1536m -Duser.timezone=Asia/Seoul -Dspring.profiles.active=dev -Dlogging.charset.console=MS949 -Dlogging.charset.file=UTF-8"
