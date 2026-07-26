#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_HOME="$(cd "${SCRIPT_DIR}/../.." && pwd)"
MODULE="tcf-ui"

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
  echo "[tcf-ui-build] gradle not found." >&2
  exit 1
}

resolve_gradle

tasks=":${MODULE}:bootJar"
case "${1:-}" in
  help|-h|--help)
    cat <<EOF
Usage: build.sh [clean|run]
  build.sh        Build tcf-ui.jar
  build.sh clean  clean + build
  build.sh run    bootRun (port 8099)
EOF
    exit 0
    ;;
  clean) tasks="clean ${tasks}" ;;
  run) tasks=":${MODULE}:bootRun" ;;
esac

gradle --stop >/dev/null 2>&1 || true
(
  cd "${PROJECT_HOME}"
  "${GRADLE}" ${tasks}
)

if [[ "${1:-}" == "run" ]]; then
  exit 0
fi

jar_file="${PROJECT_HOME}/${MODULE}/build/libs/tcf-ui.jar"
echo
echo "[tcf-ui-build] Build output:"
if [[ -f "${jar_file}" ]]; then
  echo "  [OK] tcf-ui.jar"
  ls -lh "${jar_file}"
else
  echo "  [MISSING] tcf-ui.jar - not found in ${MODULE}/build/libs" >&2
  exit 1
fi
