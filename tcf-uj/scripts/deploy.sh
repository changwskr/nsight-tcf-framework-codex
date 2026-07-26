#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_HOME="$(cd "${SCRIPT_DIR}/../.." && pwd)"
MODULE="tcf-uj"
SRC_JAR="tcf-uj.jar"
DEST_JAR="tcf-uj.jar"

if [[ -n "${TCF_UJ_DEPLOY_DIR:-}" ]]; then
  DEPLOY_DIR="${TCF_UJ_DEPLOY_DIR}"
else
  DEPLOY_DIR="${PROJECT_HOME}/tcf-uj/deploy"
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
  echo "[tcf-uj-deploy] gradle not found." >&2
  exit 1
}

if [[ "${1:-}" == "help" || "${1:-}" == "-h" || "${1:-}" == "--help" ]]; then
  cat <<EOF
Usage: deploy.sh
  Build tcf-uj.jar and copy to deploy directory

Target deploy dir:
  ${DEPLOY_DIR}
  (override: export TCF_UJ_DEPLOY_DIR=...)
EOF
  exit 0
fi

mkdir -p "${DEPLOY_DIR}"
[[ -d "${DEPLOY_DIR}" ]] || { echo "[tcf-uj-deploy] deploy dir not found: ${DEPLOY_DIR}" >&2; exit 1; }

resolve_gradle

echo "[tcf-uj-deploy] Building ${SRC_JAR} ..."
(
  cd "${PROJECT_HOME}"
  "${GRADLE}" ":${MODULE}:bootJar"
)

src="${PROJECT_HOME}/${MODULE}/build/libs/${SRC_JAR}"
[[ -f "${src}" ]] || { echo "[tcf-uj-deploy] JAR not found: ${src}" >&2; exit 1; }

echo "[tcf-uj-deploy] Copying ${DEST_JAR} to ${DEPLOY_DIR} ..."
cp -f "${src}" "${DEPLOY_DIR}/${DEST_JAR}"
echo "  deployed ${DEST_JAR}"

echo
echo "[tcf-uj-deploy] Verifying deployed JAR ..."
if [[ -f "${DEPLOY_DIR}/${DEST_JAR}" ]]; then
  echo "  [OK] ${DEST_JAR}"
  ls -lh "${DEPLOY_DIR}/${DEST_JAR}"
  echo
  echo "[tcf-uj-deploy] Done. Run: java -jar ${DEPLOY_DIR}/${DEST_JAR} (port 8102)"
else
  echo "[tcf-uj-deploy] Verification failed: ${DEST_JAR} not found in ${DEPLOY_DIR}" >&2
  exit 1
fi
