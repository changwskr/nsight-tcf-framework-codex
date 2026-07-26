#!/usr/bin/env bash
set -euo pipefail
BASE_URL=${BASE_URL:-https://nh.marketing.com}
fail=0
for ctx in ic pc ms sv pd eb ep ss mg om batch ui jwt; do
  echo "== $ctx =="
  if ! curl -k -fsS "$BASE_URL/$ctx/actuator/health"; then
    echo "health check failed: $ctx"
    fail=1
  fi
  echo
done
exit "$fail"
