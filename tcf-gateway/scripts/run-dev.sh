#!/usr/bin/env bash
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
export SPRING_PROFILES_ACTIVE=dev
echo "[gw-run-dev] profile=dev port=8100 downstream=tomcat (http://localhost:8080)"
exec "${SCRIPT_DIR}/build.sh" run-dev
