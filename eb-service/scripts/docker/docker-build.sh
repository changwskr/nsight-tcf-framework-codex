#!/usr/bin/env bash
# eb-service Docker image build
# Usage:
#   ./docker-build.sh
#   ./docker-build.sh nsight-eb:dev

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_HOME="$(cd "${SCRIPT_DIR}/../../.." && pwd)"
DOCKERFILE="${SCRIPT_DIR}/Dockerfile"
IMAGE_TAG="${1:-nsight-eb:local}"

usage() {
  cat <<'EOF'
Usage: docker-build.sh [image-tag]
  ./docker-build.sh                 Build nsight-eb:local
  ./docker-build.sh nsight-eb:dev   Build with custom tag
EOF
}

if [[ "${1:-}" == "help" || "${1:-}" == "-h" || "${1:-}" == "--help" ]]; then
  usage
  exit 0
fi

if ! command -v docker >/dev/null 2>&1; then
  echo "[eb-docker-build] docker not found. Install Docker and retry." >&2
  exit 1
fi

if [[ ! -f "${DOCKERFILE}" ]]; then
  echo "[eb-docker-build] Dockerfile not found: ${DOCKERFILE}" >&2
  exit 1
fi

echo "[eb-docker-build] PROJECT_HOME=${PROJECT_HOME}"
echo "[eb-docker-build] Dockerfile=${DOCKERFILE}"
echo "[eb-docker-build] Image=${IMAGE_TAG}"
echo "[eb-docker-build] docker build -f eb-service/scripts/docker/Dockerfile -t ${IMAGE_TAG} ."

cd "${PROJECT_HOME}"
docker build -f "eb-service/scripts/docker/Dockerfile" -t "${IMAGE_TAG}" .

echo
echo "[eb-docker-build] Done."
echo "  Image : ${IMAGE_TAG}"
echo "  Run   : eb-service/scripts/docker/docker-run.sh"
