#!/usr/bin/env bash
# local — Tomcat deploy-wars (19 WAR 빌드 + webapps 배포)
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
CICD_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"
FW_ROOT="$(cd "${CICD_ROOT}/.." && pwd)"
ZTOMCAT="${FW_ROOT}/ztomcat"
DEPLOY_PS1="${SCRIPT_DIR}/deploy-wars.ps1"

SYNC_PROFILE="dev"
SKIP_SYNC=0
SKIP_BUILD=0
NO_GRADLE_STOP=0
RESTART=0
CODES=()

usage() {
  cat <<EOF
Usage: deploy-wars.sh [codes...] [options]

tcf-cicd 설정 sync -> WAR 빌드 -> ztomcat webapps 배포 (19 context).

Codes (생략 또는 all = 전체):
  cc ic pc bc ms sv pd cm eb ep bp bd ss cs ct mg om batch ui

Options:
  --sync-profile dev|local   framework sync (기본 dev)
  --skip-sync                sync 생략
  --skip-build               기존 WAR만 복사
  --no-gradle-stop
  --restart                  배포 후 ztomcat/start.sh
  -h, --help

Examples:
  ./deploy-wars.sh
  ./deploy-wars.sh sv om batch
  ./deploy-wars.sh --skip-sync --skip-build

EOF
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --sync-profile)
      SYNC_PROFILE="${2:-dev}"
      shift 2
      ;;
    --skip-sync) SKIP_SYNC=1; shift ;;
    --skip-build) SKIP_BUILD=1; shift ;;
    --no-gradle-stop) NO_GRADLE_STOP=1; shift ;;
    --restart) RESTART=1; shift ;;
    -h|--help|help)
      usage
      exit 0
      ;;
    --*)
      echo "[deploy-wars] unknown option: $1" >&2
      usage
      exit 1
      ;;
    *)
      CODES+=("$1")
      shift
      ;;
  esac
done

PS_ARGS=()
[[ ${#CODES[@]} -gt 0 ]] && PS_ARGS+=("${CODES[@]}")
[[ "${SKIP_SYNC}" -eq 1 ]] && PS_ARGS+=('-SkipSync')
[[ "${SKIP_BUILD}" -eq 1 ]] && PS_ARGS+=('-SkipBuild')
[[ "${NO_GRADLE_STOP}" -eq 1 ]] && PS_ARGS+=('-NoGradleStop')
[[ "${RESTART}" -eq 1 ]] && PS_ARGS+=('-Restart')
PS_ARGS+=('-SyncProfile' "${SYNC_PROFILE}")

if command -v pwsh >/dev/null 2>&1; then
  exec pwsh -NoProfile -File "${DEPLOY_PS1}" "${PS_ARGS[@]}"
fi
exec powershell -NoProfile -ExecutionPolicy Bypass -File "${DEPLOY_PS1}" "${PS_ARGS[@]}"
