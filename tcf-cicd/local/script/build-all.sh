#!/usr/bin/env bash
# local — 전체 프로젝트 빌드 (Linux/macOS)
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
CICD_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"
FW_ROOT="$(cd "${CICD_ROOT}/.." && pwd)"
SYNC_PS1="${CICD_ROOT}/scripts/sync-to-framework.ps1"

TARGET="${1:-all}"
SKIP_SYNC=0
NO_GRADLE_STOP=0

usage() {
  cat <<EOF
Usage: build-all.sh [target] [options]

local 프로파일 yml 동기화 후 nsight-tcf-framework 전 모듈을 빌드합니다.

Targets:
  all        (기본) gradle build — compile + test + jar/war (전체 24 모듈)
  wars       gradle buildZtomcatWars — 19 WAR
  business   gradle buildBusinessWars — 17 WAR
  framework  TCF 프레임워크·플랫폼 모듈만 (업무 *-service 제외)
  fast       gradle build -x test

Options:
  --skip-sync       tcf-cicd/local -> framework sync 생략
  --no-gradle-stop  gradle --stop 생략
  -h, --help        도움말

Examples:
  ./build-all.sh
  ./build-all.sh wars
  ./build-all.sh fast --skip-sync

Framework root: ${FW_ROOT}
EOF
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    all|wars|business|framework|fast)
      TARGET="$1"
      shift
      ;;
    --skip-sync)
      SKIP_SYNC=1
      shift
      ;;
    --no-gradle-stop)
      NO_GRADLE_STOP=1
      shift
      ;;
    -h|--help|help)
      usage
      exit 0
      ;;
    *)
      echo "[build-all] unknown argument: $1" >&2
      usage
      exit 1
      ;;
  esac
done

resolve_gradle() {
  if [[ -n "${GRADLE_HOME_OVERRIDE:-}" && -x "${GRADLE_HOME_OVERRIDE}/bin/gradle" ]]; then
    GRADLE="${GRADLE_HOME_OVERRIDE}/bin/gradle"
    return 0
  fi
  if [[ -n "${GRADLE_HOME:-}" && -x "${GRADLE_HOME}/bin/gradle" ]]; then
    GRADLE="${GRADLE_HOME}/bin/gradle"
    return 0
  fi
  if command -v gradle >/dev/null 2>&1; then
    GRADLE="$(command -v gradle)"
    return 0
  fi
  echo "[build-all] gradle not found." >&2
  exit 1
}

if [[ "${SKIP_SYNC}" -eq 0 ]]; then
  if [[ ! -f "${SYNC_PS1}" ]]; then
    echo "[build-all] sync script not found: ${SYNC_PS1}" >&2
    exit 1
  fi
  echo "[build-all] sync local config -> framework"
  pwsh -NoProfile -File "${SYNC_PS1}" -Profile local 2>/dev/null \
    || powershell -NoProfile -ExecutionPolicy Bypass -File "${SYNC_PS1}" -Profile local
fi

resolve_gradle

cd "${FW_ROOT}"

if [[ "${NO_GRADLE_STOP}" -eq 0 ]]; then
  echo "[build-all] gradle --stop"
  "${GRADLE}" --stop >/dev/null 2>&1 || true
fi

case "${TARGET}" in
  all)
    echo "[build-all] gradle build"
    "${GRADLE}" build
    ;;
  wars)
    echo "[build-all] gradle buildZtomcatWars"
    "${GRADLE}" buildZtomcatWars
    ;;
  business)
    echo "[build-all] gradle buildBusinessWars"
    "${GRADLE}" buildBusinessWars
    ;;
  framework)
    echo "[build-all] gradle framework modules"
    "${GRADLE}" \
      :tcf-util:build :tcf-core:build :tcf-cache:build :tcf-web:build \
      :tcf-om:build :tcf-batch:build :tcf-ui:build
    ;;
  fast)
    echo "[build-all] gradle build -x test"
    "${GRADLE}" build -x test
    ;;
esac

echo "[build-all] Done (${TARGET})"
