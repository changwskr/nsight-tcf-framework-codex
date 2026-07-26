#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_HOME="$(cd "${SCRIPT_DIR}/../.." && pwd)"
MODULE="sv-service"
SRC_WAR="sv.war"
DEST_WAR="sv.war"
CTX="sv"

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
  echo "[sv-deploy] gradle not found." >&2
  exit 1
}

if [[ "${1:-}" == "help" || "${1:-}" == "-h" || "${1:-}" == "--help" ]]; then
  cat <<EOF
Usage: deploy.sh
  Build sv.war and deploy to Tomcat webapps (/sv)

Target webapps:
  ${WEBAPPS}
  (override: export TOMCAT_WEBAPPS=...)
EOF
  exit 0
fi

[[ -d "${WEBAPPS}" ]] || { echo "[sv-deploy] webapps not found: ${WEBAPPS}" >&2; exit 1; }

resolve_gradle

echo "[sv-deploy] Building ${SRC_WAR} ..."
(
  cd "${PROJECT_HOME}"
  "${GRADLE}" :tcf-util:build :tcf-core:build :tcf-web:build ":${MODULE}:bootWar"
)

src="${PROJECT_HOME}/${MODULE}/build/libs/${SRC_WAR}"
[[ -f "${src}" ]] || { echo "[sv-deploy] WAR not found: ${src}" >&2; exit 1; }

rm -rf "${WEBAPPS}/${CTX}"
echo "[sv-deploy] Copying ${DEST_WAR} to ${WEBAPPS} ..."
cp -f "${src}" "${WEBAPPS}/${DEST_WAR}"
echo "  deployed ${DEST_WAR}"

echo
echo "[sv-deploy] Verifying deployed WAR ..."
if [[ -f "${WEBAPPS}/${DEST_WAR}" ]]; then
  echo "  [OK] ${DEST_WAR}"
  ls -lh "${WEBAPPS}/${DEST_WAR}"
  echo
  echo "[sv-deploy] Done. Tomcat running: /sv redeploys automatically (~15s)."
else
  echo "[sv-deploy] Verification failed: ${DEST_WAR} not found in ${WEBAPPS}" >&2
  exit 1
fi

