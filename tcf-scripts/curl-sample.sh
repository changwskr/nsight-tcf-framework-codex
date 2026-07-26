#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."

usage() {
  cat <<'EOF'

Usage: ./tcf-scripts/curl-sample.sh <code>

Codes: ic pc ms sv pd eb ep ss mg om
Example: ./tcf-scripts/curl-sample.sh sv

EOF
  exit 1
}

[[ $# -eq 1 ]] || usage
code="$(echo "$1" | tr '[:upper:]' '[:lower:]')"

case "$code" in
  ic) port=8082 ;;
  pc) port=8083 ;;
  ms) port=8085 ;;
  sv) port=8086 ;;
  pd) port=8087 ;;
  eb) port=8089 ;;
  ep) port=8090 ;;
  ss) port=8093 ;;
  mg) port=8096 ;;
  om) port=8097 ;;
  *) echo "[curl] Unknown business code: $code"; usage ;;
esac

body="tcf-ui/src/main/resources/sample-requests/${code}-sample-inquiry.json"
[[ -f "$body" ]] || { echo "[curl] Sample file not found: $body"; exit 1; }

echo "[curl] POST http://localhost:${port}/${code}/online"
curl -s -X POST "http://localhost:${port}/${code}/online" \
  -H "Content-Type: application/json" \
  --data-binary "@${body}"
echo
