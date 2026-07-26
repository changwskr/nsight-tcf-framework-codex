#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_HOME="$(cd "${SCRIPT_DIR}/../.." && pwd)"
MODULE="tcf-gateway"
GRADLE="${GRADLE:-gradle}"

TASK=":${MODULE}:bootWar"
if [[ "${1:-}" == "clean" ]]; then
  TASK="clean ${TASK}"
elif [[ "${1:-}" == "run" || "${1:-}" == "run-local" || "${1:-}" == "run-dev" ]]; then
  TASK=":${MODULE}:bootRun"
  if [[ "${1:-}" == "run-local" ]]; then
    export SPRING_PROFILES_ACTIVE=local
  elif [[ "${1:-}" == "run-dev" ]]; then
    export SPRING_PROFILES_ACTIVE=dev
  fi
fi

cd "${PROJECT_HOME}"
echo "[gw-build] ${GRADLE} ${TASK} (SPRING_PROFILES_ACTIVE=${SPRING_PROFILES_ACTIVE:-default})"
"${GRADLE}" ${TASK}

if [[ "${1:-}" == "run" || "${1:-}" == "run-local" || "${1:-}" == "run-dev" ]]; then
  exit 0
fi

WAR_FILE="${PROJECT_HOME}/${MODULE}/build/libs/gw.war"
if [[ -f "${WAR_FILE}" ]]; then
  echo "[gw-build] OK gw.war"
  ls -la "${WAR_FILE}"
else
  echo "[gw-build] MISSING gw.war" >&2
  exit 1
fi
