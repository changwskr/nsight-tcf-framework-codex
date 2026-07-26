#!/usr/bin/env bash
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
export SPRING_PROFILES_ACTIVE=local
echo "[gw-run-local] profile=local port=8100 downstream=bootrun (업무별 포트)"
exec "${SCRIPT_DIR}/build.sh" run-local
