#!/usr/bin/env bash
# tcf-cicd prod Spring 설정을 Tomcat runtime mount 경로에 배치
# WAR 재빌드 없이 application-prod.yml 갱신
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
CICD_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
PROFILE="${1:-prod}"

if [[ "${PROFILE}" != "prod" ]]; then
  echo "Usage: apply-tomcat-config.sh [prod]" >&2
  echo "  prod Spring yml -> \$CATALINA_BASE/conf/nsight/" >&2
  exit 1
fi

CATALINA_BASE="${CATALINA_BASE:-${CATALINA_HOME:-}}"
if [[ -z "${CATALINA_BASE}" ]]; then
  echo "[apply-tomcat-config] CATALINA_BASE or CATALINA_HOME required" >&2
  exit 1
fi

MOUNT_ROOT="${CATALINA_BASE}/conf/nsight"
SPRING_SRC="${CICD_ROOT}/prod/spring"

if [[ ! -d "${SPRING_SRC}" ]]; then
  echo "[apply-tomcat-config] not found: ${SPRING_SRC}" >&2
  exit 1
fi

echo "[apply-tomcat-config] ${SPRING_SRC} -> ${MOUNT_ROOT}"
mkdir -p "${MOUNT_ROOT}"

count=0
while IFS= read -r -d '' yml; do
  rel="${yml#${SPRING_SRC}/}"
  mod="$(dirname "${rel}")"
  base="$(basename "${yml}")"
  dest_dir="${MOUNT_ROOT}/${mod}"
  mkdir -p "${dest_dir}"
  cp -f "${yml}" "${dest_dir}/${base}"
  echo "  ${mod}/${base}"
  count=$((count + 1))
done < <(find "${SPRING_SRC}" -name 'application-prod.yml' -print0)

echo "[apply-tomcat-config] copied ${count} file(s)"
echo "[apply-tomcat-config] setenv에 추가 권장:"
echo "  CATALINA_OPTS=\"\${CATALINA_OPTS} -Dspring.config.additional-location=file:\${CATALINA_BASE}/conf/nsight/\""
