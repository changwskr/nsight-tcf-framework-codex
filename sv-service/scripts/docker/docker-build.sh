#!/usr/bin/env bash
# sv-service Docker image build
# Usage:
#   ./docker-build.sh
#   ./docker-build.sh nsight-sv:dev

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_HOME="$(cd "${SCRIPT_DIR}/../../.." && pwd)"
DOCKERFILE="${SCRIPT_DIR}/Dockerfile"
IMAGE_TAG="${1:-nsight-sv:local}"

usage() {
  cat <<'EOF'
Usage: docker-build.sh [image-tag]
  ./docker-build.sh                 Build nsight-sv:local
  ./docker-build.sh nsight-sv:dev   Build with custom tag
EOF
}

if [[ "${1:-}" == "help" || "${1:-}" == "-h" || "${1:-}" == "--help" ]]; then
  usage
  exit 0
fi

if ! command -v docker >/dev/null 2>&1; then
  echo "[sv-docker-build] docker not found. Install Docker and retry." >&2
  exit 1
fi

if [[ ! -f "${DOCKERFILE}" ]]; then
  echo "[sv-docker-build] Dockerfile not found: ${DOCKERFILE}" >&2
  exit 1
fi

echo "[sv-docker-build] PROJECT_HOME=${PROJECT_HOME}"
echo "[sv-docker-build] Dockerfile=${DOCKERFILE}"
echo "[sv-docker-build] Image=${IMAGE_TAG}"
echo "[sv-docker-build] docker build -f sv-service/scripts/docker/Dockerfile -t ${IMAGE_TAG} ."

cd "${PROJECT_HOME}"
docker build -f "sv-service/scripts/docker/Dockerfile" -t "${IMAGE_TAG}" .

echo
echo "[sv-docker-build] Done."
echo "  Image : ${IMAGE_TAG}"
echo "  Run   : sv-service/scripts/docker/docker-run.sh"
