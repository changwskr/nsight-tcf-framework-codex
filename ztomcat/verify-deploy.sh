#!/usr/bin/env bash
set -uo pipefail

contexts=(ic pc ms sv pd eb ep ss mg om ui jwt batch)
base="http://localhost:8080"

echo "[ztomcat] Health check (GET /{context}/actuator/health)"
ok=0
fail=0

for ctx in "${contexts[@]}"; do
  url="${base}/${ctx}/actuator/health"
  code="$(curl -s -o /dev/null -w "%{http_code}" --max-time 30 "${url}" 2>/dev/null || echo "000")"
  if [[ "${code}" == "200" ]]; then
    echo "  OK   ${ctx} -> ${code}"
    ok=$((ok + 1))
  elif [[ "${code}" == "000" ]]; then
    echo "  FAIL ${ctx} -> connection failed"
    fail=$((fail + 1))
  else
    echo "  FAIL ${ctx} -> HTTP ${code}"
    fail=$((fail + 1))
  fi
done

echo "[ztomcat] Result: ${ok} OK, ${fail} FAIL (total ${#contexts[@]})"
[[ "${fail}" -eq 0 ]]
