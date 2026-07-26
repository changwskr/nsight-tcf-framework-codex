#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_HOME="$(cd "${SCRIPT_DIR}/../.." && pwd)"
MODULE="tcf-ui"
SRC_JAR="tcf-ui.jar"
DEST_JAR="tcf-ui.jar"

if [[ -n "${TCF_UI_DEPLOY_DIR:-}" ]]; then
  DEPLOY_DIR="${TCF_UI_DEPLOY_DIR}"
else
  DEPLOY_DIR="${PROJECT_HOME}/tcf-ui/deploy"
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
  echo "[tcf-ui-deploy] gradle not found." >&2
  exit 1
}

if [[ "${1:-}" == "help" || "${1:-}" == "-h" || "${1:-}" == "--help" ]]; then
  cat <<EOF
Usage: deploy.sh
  Build tcf-ui.jar and copy to deploy directory

Target deploy dir:
  ${DEPLOY_DIR}
  (override: export TCF_UI_DEPLOY_DIR=...)
EOF
  exit 0
fi

mkdir -p "${DEPLOY_DIR}"
[[ -d "${DEPLOY_DIR}" ]] || { echo "[tcf-ui-deploy] deploy dir not found: ${DEPLOY_DIR}" >&2; exit 1; }

resolve_gradle

echo "[tcf-ui-deploy] Building ${SRC_JAR} ..."
(
  cd "${PROJECT_HOME}"
  "${GRADLE}" ":${MODULE}:bootJar"
)

src="${PROJECT_HOME}/${MODULE}/build/libs/${SRC_JAR}"
[[ -f "${src}" ]] || { echo "[tcf-ui-deploy] JAR not found: ${src}" >&2; exit 1; }

echo "[tcf-ui-deploy] Copying ${DEST_JAR} to ${DEPLOY_DIR} ..."
cp -f "${src}" "${DEPLOY_DIR}/${DEST_JAR}"
echo "  deployed ${DEST_JAR}"

echo
echo "[tcf-ui-deploy] Verifying deployed JAR ..."
if [[ -f "${DEPLOY_DIR}/${DEST_JAR}" ]]; then
  echo "  [OK] ${DEST_JAR}"
  ls -lh "${DEPLOY_DIR}/${DEST_JAR}"
  echo
  echo "[tcf-ui-deploy] Done. Run: java -jar ${DEPLOY_DIR}/${DEST_JAR} (port 8099)"
else
  echo "[tcf-ui-deploy] Verification failed: ${DEST_JAR} not found in ${DEPLOY_DIR}" >&2
  exit 1
fi
