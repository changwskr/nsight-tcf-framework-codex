#!/usr/bin/env bash
# tcf-cicd → framework (Linux/macOS)
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
exec pwsh -NoProfile -File "${SCRIPT_DIR}/sync-to-framework.ps1" "$@"
