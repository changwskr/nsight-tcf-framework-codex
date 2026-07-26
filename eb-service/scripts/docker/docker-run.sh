#!/usr/bin/env bash
# eb-service Docker container run
# Usage:
#   ./docker-run.sh
#   ./docker-run.sh -d
#   ./docker-run.sh nsight-eb:dev
#   ./docker-run.sh -d nsight-eb:dev

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_HOME="$(cd "${SCRIPT_DIR}/../../.." && pwd)"

IMAGE_TAG="nsight-eb:local"
CONTAINER_NAME="${CONTAINER_NAME:-nsight-eb}"
HOST_PORT="${HOST_PORT:-8089}"
CONTAINER_PORT="${CONTAINER_PORT:-8089}"
PROFILE="${SPRING_PROFILES_ACTIVE:-local}"
VOLUME_NAME="${VOLUME_NAME:-nsight-eb-data}"
EP_ONLINE_URL="${EP_ONLINE_URL:-http://host.docker.internal:8090/ep/online}"
DETACH=()

usage() {
  cat <<'EOF'
Usage: docker-run.sh [-d|--detach] [image-tag]
  ./docker-run.sh                 Foreground run (nsight-eb:local)
  ./docker-run.sh -d              Detach mode
  ./docker-run.sh nsight-eb:dev   Custom image tag
  ./docker-run.sh -d nsight-eb:dev

Env overrides (optional):
  HOST_PORT=18089
  EP_ONLINE_URL=http://host.docker.internal:8090/ep/online
  CONTAINER_NAME=nsight-eb
  VOLUME_NAME=nsight-eb-data
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
  echo "[eb-docker-run] docker not found. Install Docker and retry." >&2
  exit 1
fi

if ! docker image inspect "${IMAGE_TAG}" >/dev/null 2>&1; then
  echo "[eb-docker-run] Image not found: ${IMAGE_TAG}" >&2
  echo "[eb-docker-run] Build first: eb-service/scripts/docker/docker-build.sh" >&2
  exit 1
fi

if docker container inspect "${CONTAINER_NAME}" >/dev/null 2>&1; then
  echo "[eb-docker-run] Removing existing container: ${CONTAINER_NAME}"
  docker rm -f "${CONTAINER_NAME}" >/dev/null 2>&1 || true
fi

echo "[eb-docker-run] Image=${IMAGE_TAG}"
echo "[eb-docker-run] Name=${CONTAINER_NAME}"
echo "[eb-docker-run] Port=${HOST_PORT}->${CONTAINER_PORT}"
echo "[eb-docker-run] Profile=${PROFILE}"
echo "[eb-docker-run] EP URL=${EP_ONLINE_URL}"

docker run --rm "${DETACH[@]}" \
  --name "${CONTAINER_NAME}" \
  -p "${HOST_PORT}:${CONTAINER_PORT}" \
  -e "SPRING_PROFILES_ACTIVE=${PROFILE}" \
  -e "SERVER_PORT=${CONTAINER_PORT}" \
  -e "NSIGHT_EB_EVENT_PUBLISH_EP_ONLINE_URL=${EP_ONLINE_URL}" \
  -e "NSIGHT_TXLOG_PATH=/app/data/nsight-txlog" \
  -v "${VOLUME_NAME}:/app/data" \
  "${IMAGE_TAG}"

if [[ ${#DETACH[@]} -gt 0 ]]; then
  echo
  echo "[eb-docker-run] Started in background."
  echo "  Health : http://localhost:${HOST_PORT}/actuator/health"
  echo "  Online : http://localhost:${HOST_PORT}/eb/online"
  echo "  Logs   : docker logs -f ${CONTAINER_NAME}"
  echo "  Stop   : docker rm -f ${CONTAINER_NAME}"
fi
