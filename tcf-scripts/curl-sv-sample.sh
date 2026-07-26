#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
exec ./curl-sample.sh sv
