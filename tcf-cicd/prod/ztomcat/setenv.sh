#!/bin/sh
# 운영 Tomcat JVM·Spring 샘플 (Git 참조용)
#
# 사용법:
#   1) 운영 서버 Tomcat bin/setenv.sh 에 아래 항목을 반영하거나
#   2) ztomcat 통합 검증이 아닌 운영 배포 시 conf/setenv.sh 대신 이 파일을 복사
#
# dev(통합 검증) 기본값: conf/setenv.sh  (-Dspring.profiles.active=dev)
# prod 활성 시 spring.profiles.group.prod 로 dev yml 도 함께 로드됨 (tcf-web.jar)

if [ -z "${JAVA_HOME:-}" ]; then
  for candidate in \
    "/usr/lib/jvm/java-21-openjdk-amd64" \
    "/usr/lib/jvm/java-21-openjdk" \
    "/usr/lib/jvm/java-21" \
    "/usr/lib/jvm/temurin-21-jdk-amd64" \
    "/usr/lib/jvm/temurin-21-jdk"; do
    if [ -x "${candidate}/bin/java" ]; then
      JAVA_HOME="${candidate}"
      export JAVA_HOME
      break
    fi
  done
fi

JAVA_OPTS="${JAVA_OPTS} -Dfile.encoding=UTF-8 -Dstdout.encoding=UTF-8 -Dstderr.encoding=UTF-8 -Dsun.stdout.encoding=UTF-8 -Dsun.stderr.encoding=UTF-8 -Dsun.jnu.encoding=UTF-8"
export JAVA_OPTS

if [ -z "${JAVA_TOOL_OPTIONS:-}" ]; then
  JAVA_TOOL_OPTIONS="-Dfile.encoding=UTF-8 -Dstdout.encoding=UTF-8 -Dstderr.encoding=UTF-8"
  export JAVA_TOOL_OPTIONS
fi

# === 필수: 공개 API·UI Relay 게이트웨이 (Apache 443 등) ===
export NSIGHT_GATEWAY_BASE_URL="${NSIGHT_GATEWAY_BASE_URL:-https://marketing.example.com}"

# === 선택: OM → 배치 URL (미설정 시 {gateway}/batch) ===
# export NSIGHT_BATCH_SERVICE_URL="${NSIGHT_GATEWAY_BASE_URL}/batch"

# === 선택: 운영 DB (application-prod.yml 의 NSIGHT_* 환경변수) ===
# export NSIGHT_OM_DB_URL=jdbc:oracle:thin:@//dbhost:1521/nsight_om
# export NSIGHT_OM_DB_USER=...
# export NSIGHT_OM_DB_PASSWORD=...
# export NSIGHT_BATCH_DB_URL=jdbc:oracle:thin:@//dbhost:1521/nsight_om
# export NSIGHT_TXLOG_JDBC_URL=jdbc:oracle:thin:@//dbhost:1521/nsight_om
# export NSIGHT_SV_DB_URL=jdbc:oracle:thin:@//dbhost:1521/nsight_sv  # 업무별 NSIGHT_{CODE}_DB_URL

if [ -z "${NSIGHT_TXLOG_PATH:-}" ]; then
  NSIGHT_TXLOG_PATH="/var/nsight/data/nsight-txlog"
fi
export NSIGHT_TXLOG_PATH
CATALINA_OPTS="${CATALINA_OPTS} -Dnsight.txlog.path=${NSIGHT_TXLOG_PATH}"

# runtime mount (tcf-cicd prod/spring -> conf/nsight, apply-tomcat-config.sh)
# CATALINA_OPTS="${CATALINA_OPTS} -Dspring.config.additional-location=file:${CATALINA_BASE}/conf/nsight/"

CATALINA_OPTS="${CATALINA_OPTS} -Xms1024m -Xmx4096m -Duser.timezone=Asia/Seoul -Dspring.profiles.active=prod -Dlogging.charset.console=UTF-8 -Dlogging.charset.file=UTF-8"
export CATALINA_OPTS
