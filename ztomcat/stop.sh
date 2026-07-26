#!/usr/bin/env bash
set -euo pipefail

ZTOMCAT_HOME="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
CATALINA_HOME="${ZTOMCAT_HOME}/apache-tomcat-10.1.34"

if [[ ! -f "${CATALINA_HOME}/bin/catalina.sh" ]]; then
  echo "[ztomcat] Tomcat not found."
  exit 1
fi

if [[ -f "${ZTOMCAT_HOME}/setenv.local.sh" ]]; then
  ZTOMCAT_REQUIRE_JAVA=0
  # shellcheck disable=SC1091
  . "${ZTOMCAT_HOME}/setenv.local.sh"
fi

export CATALINA_HOME
export CATALINA_BASE="${CATALINA_HOME}"

echo "[ztomcat] Stopping Tomcat..."
"${CATALINA_HOME}/bin/catalina.sh" stop
