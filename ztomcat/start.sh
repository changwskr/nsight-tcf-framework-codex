#!/usr/bin/env bash
set -euo pipefail

ZTOMCAT_HOME="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
CATALINA_HOME="${ZTOMCAT_HOME}/apache-tomcat-10.1.34"

# shellcheck disable=SC1091
. "${ZTOMCAT_HOME}/setenv.local.sh"

if [[ ! -f "${CATALINA_HOME}/bin/catalina.sh" ]]; then
  echo "[ztomcat] Tomcat not found. Run install-tomcat.sh first."
  exit 1
fi

export CATALINA_HOME
export CATALINA_BASE="${CATALINA_HOME}"

"${ZTOMCAT_HOME}/apply-config.sh"

if [[ "${ZTOMCAT_SKIP_DEPLOY:-0}" != "1" ]]; then
  echo "[ztomcat] Building and deploying batch.war, ui.war to Tomcat webapps ..."
  "${ZTOMCAT_HOME}/deploy-wars.sh" batch ui
else
  echo "[ztomcat] Skip WAR deploy (ZTOMCAT_SKIP_DEPLOY=1)"
fi

echo "[ztomcat] Starting Tomcat on http://localhost:8080"
echo "[ztomcat]   batch → http://localhost:8080/batch"
echo "[ztomcat]   ui    → http://localhost:8080/ui/om/admin/login.html"
"${CATALINA_HOME}/bin/catalina.sh" start

echo "[ztomcat] Started Tomcat :8080 (tcf-batch /batch, tcf-ui /ui WAR)"
