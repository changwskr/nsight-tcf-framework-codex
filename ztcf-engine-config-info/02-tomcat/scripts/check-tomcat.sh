#!/usr/bin/env bash
set -euo pipefail
curl -fsS http://127.0.0.1:${HTTP_PORT:-8080}/sv/actuator/health || true
curl -fsS http://127.0.0.1:${HTTP_PORT:-8080}/ui/actuator/health || true
grep -E "jvmRoute|DeltaManager|SimpleTcpCluster" $CATALINA_BASE/logs/catalina.out || true
