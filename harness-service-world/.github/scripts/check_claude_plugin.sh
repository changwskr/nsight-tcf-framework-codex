#!/usr/bin/env bash
set -euo pipefail

echo "Checking for .claude-plugin directories under harness-service-world..."

if find harness-service-world -type d -name ".claude-plugin" | grep -q .; then
  echo "Error: .claude-plugin directory found under harness-service-world. Remove before merging." >&2
  exit 1
fi

echo "No .claude-plugin directories found. OK."
