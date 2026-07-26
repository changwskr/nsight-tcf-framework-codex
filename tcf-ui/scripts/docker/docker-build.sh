#!/usr/bin/env bash
# tcf-ui Docker image build
# Usage:
#   ./docker-build.sh
#   ./docker-build.sh nsight-ui:dev

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_HOME="$(cd "${SCRIPT_DIR}/../../.." && pwd)"
DOCKERFILE="${SCRIPT_DIR}/Dockerfile"
IMAGE_TAG="${1:-nsight-ui:local}"

usage() {
  cat <<'EOF'
Usage: docker-build.sh [image-tag]
  ./docker-build.sh                 Build nsight-ui:local
  ./docker-build.sh nsight-ui:dev   Build with custom tag
EOF
}

if [[ "${1:-}" == "help" || "${1:-}" == "-h" || "${1:-}" == "--help" ]]; then
  usage
  exit 0
fi

if ! command -v docker >/dev/null 2>&1; then
  echo "[ui-docker-build] docker not found. Install Docker and retry." >&2
  exit 1
fi

if [[ ! -f "${DOCKERFILE}" ]]; then
  echo "[ui-docker-build] Dockerfile not found: ${DOCKERFILE}" >&2
  exit 1
fi

echo "[ui-docker-build] PROJECT_HOME=${PROJECT_HOME}"
echo "[ui-docker-build] Dockerfile=${DOCKERFILE}"
echo "[ui-docker-build] Image=${IMAGE_TAG}"
echo "[ui-docker-build] docker build -f tcf-ui/scripts/docker/Dockerfile -t ${IMAGE_TAG} ."

cd "${PROJECT_HOME}"
docker build -f "tcf-ui/scripts/docker/Dockerfile" -t "${IMAGE_TAG}" .

echo
echo "[ui-docker-build] Done."
echo "  Image : ${IMAGE_TAG}"
echo "  Run   : tcf-ui/scripts/docker/docker-run.sh"
