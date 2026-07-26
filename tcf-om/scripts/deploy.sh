#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_HOME="$(cd "${SCRIPT_DIR}/../.." && pwd)"
MODULE="tcf-om"
SRC_WAR="tcf-om.war"
DEST_WAR="om.war"
CTX="om"

if [[ -n "${TOMCAT_WEBAPPS:-}" ]]; then
  WEBAPPS="${TOMCAT_WEBAPPS}"
elif [[ -d "${PROJECT_HOME}/ztomcat/apache-tomcat-10.1.34/webapps" ]]; then
  WEBAPPS="$(cd "${PROJECT_HOME}/ztomcat/apache-tomcat-10.1.34/webapps" && pwd)"
else
  WEBAPPS="/opt/tomcat/webapps"
fi

resolve_gradle() {
  local home="${GRADLE_HOME_OVERRIDE:-${GRADLE_HOME:-}}"
  if [[ -n "${home}" && -x "${home}/bin/gradle" ]]; then
    GRADLE="${home}/bin/gradle"
    return 0
  fi
  if command -v gradle >/dev/null 2>&1; then
    GRADLE="$(command -v gradle)"
    return 0
  fi
  echo "[tcf-om-deploy] gradle not found." >&2
  exit 1
}

if [[ "${1:-}" == "help" || "${1:-}" == "-h" || "${1:-}" == "--help" ]]; then
  cat <<EOF
Usage: deploy.sh
  Build tcf-om.war and deploy to Tomcat webapps as 00-om.war (/om)

Target webapps:
  ${WEBAPPS}
  (override: export TOMCAT_WEBAPPS=...)
EOF
  exit 0
fi

[[ -d "${WEBAPPS}" ]] || { echo "[tcf-om-deploy] webapps not found: ${WEBAPPS}" >&2; exit 1; }

resolve_gradle

echo "[tcf-om-deploy] Building ${SRC_WAR} ..."
(
  cd "${PROJECT_HOME}"
  "${GRADLE}" :tcf-util:build :tcf-core:build :tcf-web:build ":${MODULE}:bootWar"
)

src="${PROJECT_HOME}/${MODULE}/build/libs/${SRC_WAR}"
[[ -f "${src}" ]] || { echo "[tcf-om-deploy] WAR not found: ${src}" >&2; exit 1; }

rm -rf "${WEBAPPS}/${CTX}"
echo "[tcf-om-deploy] Copying ${DEST_WAR} to ${WEBAPPS} ..."
cp -f "${src}" "${WEBAPPS}/${DEST_WAR}"
echo "  deployed ${DEST_WAR}"

echo
echo "[tcf-om-deploy] Verifying deployed WAR ..."
if [[ -f "${WEBAPPS}/${DEST_WAR}" ]]; then
  echo "  [OK] ${DEST_WAR}"
  ls -lh "${WEBAPPS}/${DEST_WAR}"
  echo
  echo "[tcf-om-deploy] Done. Tomcat running: /om redeploys automatically (~15s)."
else
  echo "[tcf-om-deploy] Verification failed: ${DEST_WAR} not found in ${WEBAPPS}" >&2
  exit 1
fi
