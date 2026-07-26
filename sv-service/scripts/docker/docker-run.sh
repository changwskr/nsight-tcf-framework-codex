#!/usr/bin/env bash
# sv-service Docker container run
# Usage:
#   ./docker-run.sh
#   ./docker-run.sh -d
#   ./docker-run.sh nsight-sv:dev
#   ./docker-run.sh -d nsight-sv:dev

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_HOME="$(cd "${SCRIPT_DIR}/../../.." && pwd)"

IMAGE_TAG="nsight-sv:local"
CONTAINER_NAME="${CONTAINER_NAME:-nsight-sv}"
HOST_PORT="${HOST_PORT:-8086}"
CONTAINER_PORT="${CONTAINER_PORT:-8086}"
PROFILE="${SPRING_PROFILES_ACTIVE:-local}"
VOLUME_NAME="${VOLUME_NAME:-nsight-sv-data}"
IC_BASE_URL="${IC_BASE_URL:-http://host.docker.internal:8082}"
DETACH=()

usage() {
  cat <<'EOF'
Usage: docker-run.sh [-d|--detach] [image-tag]
  ./docker-run.sh                 Foreground run (nsight-sv:local)
  ./docker-run.sh -d              Detach mode
  ./docker-run.sh nsight-sv:dev   Custom image tag
  ./docker-run.sh -d nsight-sv:dev

Env overrides (optional):
  HOST_PORT=18086
  IC_BASE_URL=http://host.docker.internal:8082
  CONTAINER_NAME=nsight-sv
  VOLUME_NAME=nsight-sv-data
EOF
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    help|-h|--help)
      usage
      exit 0
      ;;
    -d|--detach)
      DETACH=(-d)
      shift
      ;;
    *)
      IMAGE_TAG="$1"
      shift
      ;;
  esac
done

if ! command -v docker >/dev/null 2>&1; then
  echo "[sv-docker-run] docker not found. Install Docker and retry." >&2
  exit 1
fi

if ! docker image inspect "${IMAGE_TAG}" >/dev/null 2>&1; then
  echo "[sv-docker-run] Image not found: ${IMAGE_TAG}" >&2
  echo "[sv-docker-run] Build first: sv-service/scripts/docker/docker-build.sh" >&2
  exit 1
fi

if docker container inspect "${CONTAINER_NAME}" >/dev/null 2>&1; then
  echo "[sv-docker-run] Removing existing container: ${CONTAINER_NAME}"
  docker rm -f "${CONTAINER_NAME}" >/dev/null 2>&1 || true
fi

echo "[sv-docker-run] Image=${IMAGE_TAG}"
echo "[sv-docker-run] Name=${CONTAINER_NAME}"
echo "[sv-docker-run] Port mapping: 0.0.0.0:${HOST_PORT} -> ${CONTAINER_PORT}"
echo "[sv-docker-run] Profile=${PROFILE}"
echo "[sv-docker-run] IC URL=${IC_BASE_URL}"

docker run --rm "${DETACH[@]}" \
  --name "${CONTAINER_NAME}" \
  -p "0.0.0.0:${HOST_PORT}:${CONTAINER_PORT}" \
  -e "SPRING_PROFILES_ACTIVE=${PROFILE}" \
  -e "SERVER_PORT=${CONTAINER_PORT}" \
  -e "NSIGHT_INTEGRATION_SERVICES_IC_BASE_URL=${IC_BASE_URL}" \
  -e "NSIGHT_TXLOG_PATH=/app/data/nsight-txlog" \
  -v "${VOLUME_NAME}:/app/data" \
  --add-host=host.docker.internal:host-gateway \
  "${IMAGE_TAG}"

if [[ ${#DETACH[@]} -gt 0 ]]; then
  echo
  echo "[sv-docker-run] Started in background."
  echo "[sv-docker-run] Published: $(docker port "${CONTAINER_NAME}" "${CONTAINER_PORT}" 2>/dev/null || echo 'N/A')"
  echo "  Health : http://localhost:${HOST_PORT}/actuator/health"
  echo "  Online : http://localhost:${HOST_PORT}/sv/online"
  echo "  Logs   : docker logs -f ${CONTAINER_NAME}"
  echo "  Stop   : docker rm -f ${CONTAINER_NAME}"
fi
