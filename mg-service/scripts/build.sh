#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_HOME="$(cd "${SCRIPT_DIR}/../.." && pwd)"
MODULE="mg-service"

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
  echo "[mg-build] gradle not found." >&2
  exit 1
}

resolve_gradle

tasks=":tcf-util:build :tcf-core:build :tcf-web:build :${MODULE}:bootWar"
case "${1:-}" in
  help|-h|--help)
    cat <<EOF
Usage: build.sh [clean|run]
  build.sh        Build tcf-util/core/web + mg.war
  build.sh clean  clean + build
  build.sh run    bootRun (port 8096)
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

war_file="${PROJECT_HOME}/${MODULE}/build/libs/mg.war"
echo
echo "[mg-build] Build output:"
if [[ -f "${war_file}" ]]; then
  echo "  [OK] mg.war"
  ls -lh "${war_file}"
else
  echo "  [MISSING] mg.war - not found in ${MODULE}/build/libs" >&2
  exit 1
fi

