#!/usr/bin/env bash
# tcf-cicd — sync + build only
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BUILD_PS1="${SCRIPT_DIR}/cicd-build.ps1"

TARGET="wars"
PROFILE="dev"
DRY_RUN=0
SKIP_SYNC=0
NO_GRADLE_STOP=0
ARTIFACT_DIR=""
CODES=()

usage() {
  cat <<'EOF'
Usage: cicd-build.sh [options] [codes...]

sync + Gradle 빌드 (배포 없음).

Options:
  --profile local|dev|prod   (기본 dev)
  --target wars|business|all|fast
  --dry-run
  --skip-sync
  --no-gradle-stop
  --artifact-dir <path>
  -h, --help

EOF
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --profile)
      PROFILE="${2:-dev}"
      shift 2
      ;;
    --target)
      TARGET="${2:-wars}"
      shift 2
      ;;
    --dry-run) DRY_RUN=1; shift ;;
    --skip-sync) SKIP_SYNC=1; shift ;;
    --no-gradle-stop) NO_GRADLE_STOP=1; shift ;;
    --artifact-dir)
      ARTIFACT_DIR="${2:-}"
      shift 2
      ;;
    -h|--help|help)
      usage
      exit 0
      ;;
    --*)
      echo "[cicd-build] unknown option: $1" >&2
      usage
      exit 1
      ;;
    *)
      CODES+=("$1")
      shift
      ;;
  esac
done

ARGS=(-Profile "${PROFILE}" -Target "${TARGET}")
[[ ${DRY_RUN} -eq 1 ]] && ARGS+=(-DryRun)
[[ ${SKIP_SYNC} -eq 1 ]] && ARGS+=(-SkipSync)
[[ ${NO_GRADLE_STOP} -eq 1 ]] && ARGS+=(-NoGradleStop)
[[ -n "${ARTIFACT_DIR}" ]] && ARGS+=(-ArtifactDir "${ARTIFACT_DIR}")
[[ ${#CODES[@]} -gt 0 ]] && ARGS+=("${CODES[@]}")

if command -v pwsh >/dev/null 2>&1; then
  exec pwsh -NoProfile -File "${BUILD_PS1}" "${ARGS[@]}"
fi
exec powershell -NoProfile -ExecutionPolicy Bypass -File "${BUILD_PS1}" "${ARGS[@]}"
