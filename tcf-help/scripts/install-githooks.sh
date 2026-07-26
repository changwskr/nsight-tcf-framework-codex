#!/usr/bin/env bash
# Git hooks 설치: core.hooksPath = .githooks
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
cd "$ROOT"
git config core.hooksPath .githooks
chmod +x .githooks/pre-commit 2>/dev/null || true
echo "Installed git hooks → .githooks (core.hooksPath)"
echo "Pre-commit runs: gradle :tcf-help:verifyHelp when HELP-related files change"
