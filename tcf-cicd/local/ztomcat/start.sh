#!/usr/bin/env bash
# tcf-cicd local — Tomcat 기동
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
START_PS1="${SCRIPT_DIR}/start.ps1"

SKIP_SYNC=0
SKIP_DEPLOY=0
DEPLOY_ALL=0

usage() {
  cat <<EOF
Usage: start.sh [options]

Options:
  --skip-sync     dev config sync 생략
  --skip-deploy   WAR 배포 생략
  --deploy-all    19 WAR 전체 배포 (기본: batch + ui)
  -h, --help

EOF
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --skip-sync) SKIP_SYNC=1; shift ;;
    --skip-deploy) SKIP_DEPLOY=1; shift ;;
    --deploy-all) DEPLOY_ALL=1; shift ;;
    -h|--help|help) usage; exit 0 ;;
    *) echo "[local-ztomcat] unknown: $1" >&2; usage; exit 1 ;;
  esac
done

ARGS=()
[[ "${SKIP_SYNC}" -eq 1 ]] && ARGS+=('-SkipSync')
[[ "${SKIP_DEPLOY}" -eq 1 ]] && ARGS+=('-SkipDeploy')
[[ "${DEPLOY_ALL}" -eq 1 ]] && ARGS+=('-DeployAll')

if command -v pwsh >/dev/null 2>&1; then
  exec pwsh -NoProfile -File "${START_PS1}" "${ARGS[@]}"
fi
exec powershell -NoProfile -ExecutionPolicy Bypass -File "${START_PS1}" "${ARGS[@]}"
