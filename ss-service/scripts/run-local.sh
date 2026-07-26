#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_HOME="$(cd "${SCRIPT_DIR}/../.." && pwd)"
MODULE="ss-service"

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
  echo "[ss-run] gradle not found." >&2
  exit 1
}

resolve_gradle

echo "[ss-run] gradle :${MODULE}:bootRun (port 8093)"
(
  cd "${PROJECT_HOME}"
  "${GRADLE}" ":${MODULE}:bootRun"
)

