rem 운영 Tomcat JVM·Spring 샘플 (Git 참조용)
rem
rem 사용법:
rem   1) 운영 서버 Tomcat bin\setenv.bat 에 아래 항목을 반영하거나
rem   2) ztomcat 통합 검증이 아닌 운영 배포 시 conf\setenv.bat 대신 이 파일을 복사
rem
rem dev(통합 검증) 기본값: conf\setenv.bat  (-Dspring.profiles.active=dev)
rem prod 활성 시 spring.profiles.group.prod 로 dev yml 도 함께 로드됨 (tcf-web.jar)

if not defined JAVA_HOME (
    if exist "C:\Program Files\Eclipse Adoptium\jdk-21*" (
        for /d %%J in ("C:\Program Files\Eclipse Adoptium\jdk-21*") do set "JAVA_HOME=%%J"
    )
)

set "JAVA_OPTS=%JAVA_OPTS% -Dfile.encoding=UTF-8 -Dstdout.encoding=UTF-8 -Dstderr.encoding=UTF-8 -Dsun.stdout.encoding=UTF-8 -Dsun.stderr.encoding=UTF-8 -Dsun.jnu.encoding=UTF-8"
if not defined JAVA_TOOL_OPTIONS (
    set "JAVA_TOOL_OPTIONS=-Dfile.encoding=UTF-8 -Dstdout.encoding=UTF-8 -Dstderr.encoding=UTF-8"
)

rem === 필수: 공개 API·UI Relay 게이트웨이 (Apache 443 등) ===
if not defined NSIGHT_GATEWAY_BASE_URL set "NSIGHT_GATEWAY_BASE_URL=https://marketing.example.com"

rem === 선택: OM → 배치 URL (미설정 시 {gateway}/batch) ===
rem set "NSIGHT_BATCH_SERVICE_URL=%NSIGHT_GATEWAY_BASE_URL%/batch"

rem === 선택: 운영 DB (application-prod.yml 의 NSIGHT_* 환경변수) ===
rem set "NSIGHT_OM_DB_URL=jdbc:oracle:thin:@//dbhost:1521/nsight_om"
rem set "NSIGHT_OM_DB_USER=..."
rem set "NSIGHT_OM_DB_PASSWORD=..."
rem set "NSIGHT_BATCH_DB_URL=jdbc:oracle:thin:@//dbhost:1521/nsight_om"
rem set "NSIGHT_TXLOG_JDBC_URL=jdbc:oracle:thin:@//dbhost:1521/nsight_om"
rem set "NSIGHT_SV_DB_URL=jdbc:oracle:thin:@//dbhost:1521/nsight_sv"

if not defined NSIGHT_TXLOG_PATH set "NSIGHT_TXLOG_PATH=C:\nsight\data\nsight-txlog"
if defined NSIGHT_TXLOG_PATH (
    set "CATALINA_OPTS=%CATALINA_OPTS% -Dnsight.txlog.path=%NSIGHT_TXLOG_PATH:\=/%"
)

set "CATALINA_OPTS=%CATALINA_OPTS% -Xms1024m -Xmx4096m -Duser.timezone=Asia/Seoul -Dspring.profiles.active=prod -Dlogging.charset.console=UTF-8 -Dlogging.charset.file=UTF-8"
