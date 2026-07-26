#!/usr/bin/env bash
# ztomcat — WAR 배포 + (필요 시) Tomcat 재기동 + health verify
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
DEPLOY_PS1="${SCRIPT_DIR}/deploy-restart.ps1"

SKIP_VERIFY=0
FORCE_RESTART=0
CODES=()

usage() {
  cat <<EOF
Usage: deploy-restart.sh [codes...] [options]

전체(19 WAR): stop -> clean -> deploy -> start -> health 확인
일부 WAR:     Tomcat 유지 -> deploy -> autoDeploy (~15s) -> health 확인

Codes (생략 또는 all = 전체):
  ic pc ms sv pd eb ep ss mg oc om ui jwt batch
  (별칭: tcf-oc → oc, tcf-jwt → jwt)

Options:
  --skip-verify   health 검증 생략
  --restart       일부 WAR만 배포해도 Tomcat 전체 재기동
  -h, --help

Examples:
  ./deploy-restart.sh
  ./deploy-restart.sh om ui
  ./deploy-restart.sh batch
  ./deploy-restart.sh sv om --restart

EOF
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --skip-verify) SKIP_VERIFY=1; shift ;;
    --restart) FORCE_RESTART=1; shift ;;
    -h|--help|help) usage; exit 0 ;;
    --*)
      echo "[ztomcat] unknown option: $1" >&2
      usage
      exit 1
      ;;
    *)
      CODES+=("$1")
      shift
      ;;
  esac
done

ARGS=()
[[ ${#CODES[@]} -gt 0 ]] && ARGS+=("${CODES[@]}")
[[ "${SKIP_VERIFY}" -eq 1 ]] && ARGS+=('-SkipVerify')
[[ "${FORCE_RESTART}" -eq 1 ]] && ARGS+=('-Restart')

if command -v pwsh >/dev/null 2>&1; then
  exec pwsh -NoProfile -File "${DEPLOY_PS1}" "${ARGS[@]}"
fi
exec powershell -NoProfile -ExecutionPolicy Bypass -File "${DEPLOY_PS1}" "${ARGS[@]}"
