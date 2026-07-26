#!/usr/bin/env bash
# tcf-cicd local — Tomcat 중지
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
LOCAL_ZTOMCAT="${SCRIPT_DIR}"
CICD_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"
FW_ROOT="$(cd "${CICD_ROOT}/.." && pwd)"
ZTOMCAT="${FW_ROOT}/ztomcat"
CATALINA_HOME="${ZTOMCAT}/apache-tomcat-10.1.34"

if [[ ! -f "${CATALINA_HOME}/bin/catalina.sh" ]]; then
  echo "[local-ztomcat] Tomcat not found."
  exit 1
fi

if [[ -f "${LOCAL_ZTOMCAT}/setenv.local.sh" ]]; then
  ZTOMCAT_REQUIRE_JAVA=0
  # shellcheck disable=SC1091
  . "${LOCAL_ZTOMCAT}/setenv.local.sh"
fi

export CATALINA_HOME
export CATALINA_BASE="${CATALINA_HOME}"

echo "[local-ztomcat] Stopping Tomcat..."
"${CATALINA_HOME}/bin/catalina.sh" stop

echo "[local-ztomcat] Tomcat stopped."
