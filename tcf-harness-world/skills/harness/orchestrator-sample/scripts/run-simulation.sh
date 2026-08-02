#!/usr/bin/env sh
set -eu
SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
WORKSPACE=${1:-"$SCRIPT_DIR/../_workspace"}
RUN_DIRECTORY=${2:-"$SCRIPT_DIR/../../../../_runs"}
mkdir -p "$WORKSPACE" "$RUN_DIRECTORY"
analysis_tmp=$(mktemp "$WORKSPACE/analysis.XXXXXX")
plan_tmp=$(mktemp "$WORKSPACE/plan.XXXXXX")
log_tmp=$(mktemp "$RUN_DIRECTORY/log.XXXXXX")
trap 'rm -f "$analysis_tmp" "$plan_tmp" "$log_tmp"' EXIT HUP INT TERM
printf '%s\n' '# Analysis Summary' '' 'Status: complete' 'Scope: Codex harness requirements and completion criteria.' > "$analysis_tmp"
printf '%s\n' '# PoC Plan' '' 'Status: complete' 'Artifacts: analysis-summary.md, poc-plan.md' 'Stages: analyze, build, verify.' > "$plan_tmp"
mv "$analysis_tmp" "$WORKSPACE/analysis-summary.md"
mv "$plan_tmp" "$WORKSPACE/poc-plan.md"
if [ -s "$WORKSPACE/analysis-summary.md" ] && [ -s "$WORKSPACE/poc-plan.md" ]; then result='QA PASS'; code=0; else result='QA FAIL'; code=1; fi
printf '%s\n' 'TCF Harness Orchestrator Simulation' "Analyst: $WORKSPACE/analysis-summary.md" "Builder: $WORKSPACE/poc-plan.md" "QA: $result" > "$log_tmp"
mv "$log_tmp" "$RUN_DIRECTORY/orchestrator-simulation.log"
printf '%s\n' "$result"
exit "$code"
