#!/bin/sh
# Applied by ztomcat/start.sh on each start (WAR modules require JDK 21)
# dev(통합 검증) 기본값. 운영 샘플: conf/setenv.prod.sh

if [ -z "${JAVA_HOME:-}" ]; then
  for candidate in \
    "${HOME}/.jdks/temurin-21.0.4" \
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

if [ -z "${NSIGHT_TXLOG_PATH:-}" ] && [ -n "${CATALINA_HOME:-}" ]; then
  NSIGHT_TXLOG_PATH="$(cd "${CATALINA_HOME}/../../data/nsight-txlog" && pwd)"
fi
if [ -n "${NSIGHT_TXLOG_PATH:-}" ]; then
  CATALINA_OPTS="${CATALINA_OPTS} -Dnsight.txlog.path=${NSIGHT_TXLOG_PATH}"
fi

CATALINA_OPTS="${CATALINA_OPTS} -Xms512m -Xmx1536m -Duser.timezone=Asia/Seoul -Dspring.profiles.active=dev -Dlogging.charset.console=UTF-8 -Dlogging.charset.file=UTF-8"
export CATALINA_OPTS
