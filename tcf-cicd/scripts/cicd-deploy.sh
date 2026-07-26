#!/usr/bin/env bash
# tcf-cicd — CI/CD 배포 파이프라인 (Linux/macOS / Git Bash)
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
DEPLOY_PS1="${SCRIPT_DIR}/cicd-deploy.ps1"

usage() {
  cat <<'EOF'
Usage: cicd-deploy.sh [options] [codes...]

CI/CD 파이프라인 wrapper (PowerShell cicd-deploy.ps1 호출).

Options:
  --action full|sync|build|deploy|config   (기본 full)
  --profile local|dev|prod                 (기본 dev)
  --dry-run
  --skip-sync
  --skip-build
  --skip-deploy
  --no-gradle-stop
  --restart
  --apply-config
  --health-check
  --artifact-dir <path>
  --gateway-base <url>
  -h, --help

Codes: ic pc ms sv pd eb ep ss mg om ui batch (생략 = all)

Examples:
  ./cicd-deploy.sh
  ./cicd-deploy.sh --profile dev sv om --restart
  ./cicd-deploy.sh --action build --profile prod --artifact-dir ./artifacts

EOF
}

ACTION="full"
PROFILE="dev"
DRY_RUN=0
SKIP_SYNC=0
SKIP_BUILD=0
SKIP_DEPLOY=0
NO_GRADLE_STOP=0
RESTART=0
APPLY_CONFIG=0
HEALTH_CHECK=0
ARTIFACT_DIR=""
GATEWAY_BASE=""
CODES=()

while [[ $# -gt 0 ]]; do
  case "$1" in
    --action)
      ACTION="${2:-full}"
      shift 2
      ;;
    --profile)
      PROFILE="${2:-dev}"
      shift 2
      ;;
    --dry-run) DRY_RUN=1; shift ;;
    --skip-sync) SKIP_SYNC=1; shift ;;
    --skip-build) SKIP_BUILD=1; shift ;;
    --skip-deploy) SKIP_DEPLOY=1; shift ;;
    --no-gradle-stop) NO_GRADLE_STOP=1; shift ;;
    --restart) RESTART=1; shift ;;
    --apply-config) APPLY_CONFIG=1; shift ;;
    --health-check) HEALTH_CHECK=1; shift ;;
    --artifact-dir)
      ARTIFACT_DIR="${2:-}"
      shift 2
      ;;
    --gateway-base)
      GATEWAY_BASE="${2:-}"
      shift 2
      ;;
    -h|--help|help)
      usage
      exit 0
      ;;
    --*)
      echo "[cicd-deploy] unknown option: $1" >&2
      usage
      exit 1
      ;;
    *)
      CODES+=("$1")
      shift
      ;;
  esac
done

ARGS=(-Action "${ACTION}" -Profile "${PROFILE}")
[[ ${DRY_RUN} -eq 1 ]] && ARGS+=(-DryRun)
[[ ${SKIP_SYNC} -eq 1 ]] && ARGS+=(-SkipSync)
[[ ${SKIP_BUILD} -eq 1 ]] && ARGS+=(-SkipBuild)
[[ ${SKIP_DEPLOY} -eq 1 ]] && ARGS+=(-SkipDeploy)
[[ ${NO_GRADLE_STOP} -eq 1 ]] && ARGS+=(-NoGradleStop)
[[ ${RESTART} -eq 1 ]] && ARGS+=(-Restart)
[[ ${APPLY_CONFIG} -eq 1 ]] && ARGS+=(-ApplyConfig)
[[ ${HEALTH_CHECK} -eq 1 ]] && ARGS+=(-HealthCheck)
[[ -n "${ARTIFACT_DIR}" ]] && ARGS+=(-ArtifactDir "${ARTIFACT_DIR}")
[[ -n "${GATEWAY_BASE}" ]] && ARGS+=(-GatewayBase "${GATEWAY_BASE}")
[[ ${#CODES[@]} -gt 0 ]] && ARGS+=("${CODES[@]}")

if command -v pwsh >/dev/null 2>&1; then
  exec pwsh -NoProfile -File "${DEPLOY_PS1}" "${ARGS[@]}"
fi
exec powershell -NoProfile -ExecutionPolicy Bypass -File "${DEPLOY_PS1}" "${ARGS[@]}"
