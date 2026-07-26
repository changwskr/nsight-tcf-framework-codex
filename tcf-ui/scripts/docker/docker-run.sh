#!/usr/bin/env bash
# tcf-ui Docker container run
# Usage:
#   ./docker-run.sh
#   ./docker-run.sh -d
#   ./docker-run.sh nsight-ui:dev
#   ./docker-run.sh -d nsight-ui:dev

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_HOME="$(cd "${SCRIPT_DIR}/../../.." && pwd)"

IMAGE_TAG="nsight-ui:local"
CONTAINER_NAME="${CONTAINER_NAME:-nsight-ui}"
HOST_PORT="${HOST_PORT:-8099}"
CONTAINER_PORT="${CONTAINER_PORT:-8099}"
PROFILE="${SPRING_PROFILES_ACTIVE:-local}"
BOOTRUN_HOST="${BOOTRUN_HOST:-http://host.docker.internal}"
TOMCAT_GATEWAY_URL="${TOMCAT_GATEWAY_URL:-http://host.docker.internal:8080}"
DETACH=()

usage() {
  cat <<'EOF'
Usage: docker-run.sh [-d|--detach] [image-tag]
  ./docker-run.sh                 Foreground run (nsight-ui:local)
  ./docker-run.sh -d              Detach mode
  ./docker-run.sh nsight-ui:dev   Custom image tag
  ./docker-run.sh -d nsight-ui:dev

Env overrides (optional):
  HOST_PORT=18099
  BOOTRUN_HOST=http://host.docker.internal
  TOMCAT_GATEWAY_URL=http://host.docker.internal:8080
  CONTAINER_NAME=nsight-ui
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
  echo "[ui-docker-run] docker not found. Install Docker and retry." >&2
  exit 1
fi

if ! docker image inspect "${IMAGE_TAG}" >/dev/null 2>&1; then
  echo "[ui-docker-run] Image not found: ${IMAGE_TAG}" >&2
  echo "[ui-docker-run] Build first: tcf-ui/scripts/docker/docker-build.sh" >&2
  exit 1
fi

if docker container inspect "${CONTAINER_NAME}" >/dev/null 2>&1; then
  echo "[ui-docker-run] Removing existing container: ${CONTAINER_NAME}"
  docker rm -f "${CONTAINER_NAME}" >/dev/null 2>&1 || true
fi

echo "[ui-docker-run] Image=${IMAGE_TAG}"
echo "[ui-docker-run] Name=${CONTAINER_NAME}"
echo "[ui-docker-run] Port mapping: 0.0.0.0:${HOST_PORT} -> ${CONTAINER_PORT}"
echo "[ui-docker-run] Profile=${PROFILE}"
echo "[ui-docker-run] BootRun host=${BOOTRUN_HOST}"

docker run --rm "${DETACH[@]}" \
  --name "${CONTAINER_NAME}" \
  -p "0.0.0.0:${HOST_PORT}:${CONTAINER_PORT}" \
  -e "SPRING_PROFILES_ACTIVE=${PROFILE}" \
  -e "SERVER_PORT=${CONTAINER_PORT}" \
  -e "NSIGHT_TCF_UI_BOOTRUN_HOST=${BOOTRUN_HOST}" \
  -e "NSIGHT_TCF_UI_TOMCAT_GATEWAY_URL=${TOMCAT_GATEWAY_URL}" \
  --add-host=host.docker.internal:host-gateway \
  "${IMAGE_TAG}"

if [[ ${#DETACH[@]} -gt 0 ]]; then
  echo
  echo "[ui-docker-run] Started in background."
  echo "[ui-docker-run] Published: $(docker port "${CONTAINER_NAME}" "${CONTAINER_PORT}" 2>/dev/null || echo 'N/A')"
  echo "  Health : http://localhost:${HOST_PORT}/actuator/health"
  echo "  UI     : http://localhost:${HOST_PORT}/"
  echo "  OM     : http://localhost:${HOST_PORT}/om/admin/login.html"
  echo "  Logs   : docker logs -f ${CONTAINER_NAME}"
  echo "  Stop   : docker rm -f ${CONTAINER_NAME}"
fi
