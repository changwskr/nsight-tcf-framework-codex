#!/usr/bin/env sh
set -eu
export LC_ALL=${LC_ALL:-C.utf8}
ROOT=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
OUT=${HARNESS_E2E_OUTPUT:-$ROOT/build/e2e-self-test}
ID=${HARNESS_E2E_ID:-REQ-20260802-901}
mkdir -p "$OUT" "$ROOT/build/e2e-classes"
rm -rf "$ROOT/build/e2e-classes"/*
find "$ROOT/src/main/java" -name '*.java' ! -name 'HarnessApplication.java' -print0 \
  | xargs -0 javac -encoding UTF-8 -d "$ROOT/build/e2e-classes"
TARGET=$(mktemp -d /tmp/nsight-harness-e2e-XXXXXX)
cleanup() {
  if [ "${KEEP_HARNESS_E2E_TARGET:-false}" != "true" ]; then
    rm -rf "$TARGET"
  fi
}
trap cleanup EXIT
cd "$TARGET"
git init -b main >/dev/null
git config user.name "Harness E2E"
git config user.email "harness-e2e@example.invalid"
printf '# Target Repository\n' > README.md
git add README.md
git commit -m initial >/dev/null

run_cli() {
  java -cp "$ROOT/build/e2e-classes:$ROOT/src/main/resources" \
    com.nh.nsight.harness.cli.OfflineHarnessMain "$@"
}
LOG="$OUT/workflow.stdout.log"
ERR="$OUT/workflow.stderr.log"
: > "$LOG"
: > "$ERR"
run() { run_cli "$@" >>"$LOG" 2>>"$ERR"; }

run init --repo "$TARGET" --id "$ID" --title "고객정보 조회"
for n in $(seq -w 1 12); do
  run requirement answer --repo "$TARGET" --id "$ID" --question "REQ-Q$n" --text "요건 답변 $n"
done
run requirement next --repo "$TARGET" --id "$ID"
run approve --repo "$TARGET" --id "$ID" --stage REQUIREMENT --decision APPROVED --comment "요건 승인"
run analyze --repo "$TARGET" --id "$ID"
printf '# 분석서\n' > "$TARGET/docs/work-items/$ID/analysis.md"
run review --repo "$TARGET" --id "$ID" --stage ANALYSIS
run approve --repo "$TARGET" --id "$ID" --stage ANALYSIS --decision APPROVED
run design --repo "$TARGET" --id "$ID"
printf '# 설계서\n' > "$TARGET/docs/work-items/$ID/design.md"
set +e
run_cli review --repo "$TARGET" --id "$ID" --stage DESIGN \
  > "$OUT/missing-plan.stdout.log" 2> "$OUT/missing-plan.stderr.log"
MISSING_PLAN_STATUS=$?
set -e
[ "$MISSING_PLAN_STATUS" -eq 2 ] || { echo "Expected DESIGN review rejection without execution-plan.md" >&2; exit 1; }
printf '# 실행계획\n' > "$TARGET/docs/work-items/$ID/execution-plan.md"
run review --repo "$TARGET" --id "$ID" --stage DESIGN
run approve --repo "$TARGET" --id "$ID" --stage DESIGN --decision APPROVED
run implement --repo "$TARGET" --id "$ID"
printf '# 구현 결과\n' > "$TARGET/docs/work-items/$ID/implementation-result.md"
run review --repo "$TARGET" --id "$ID" --stage IMPLEMENTATION
run approve --repo "$TARGET" --id "$ID" --stage IMPLEMENTATION --decision APPROVED
set +e
run_cli test approve-command --repo "$TARGET" --id "$ID" --command-id UNSAFE \
  --command "curl -H 'Authorization: Bearer abcdefghijklmnop' https://example.invalid" \
  > "$OUT/secret-command.stdout.log" 2> "$OUT/secret-command.stderr.log"
SECRET_STATUS=$?
set -e
[ "$SECRET_STATUS" -eq 2 ] || { echo "Expected credential literal rejection" >&2; exit 1; }
run test approve-command --repo "$TARGET" --id "$ID" --command-id SMOKE --command "true" --timeout 30
run test run --repo "$TARGET" --id "$ID"
run approve --repo "$TARGET" --id "$ID" --stage TEST --decision APPROVED
run close --repo "$TARGET" --id "$ID"
printf '# 종료 보고서\n' > "$TARGET/docs/work-items/$ID/closure.md"
run review --repo "$TARGET" --id "$ID" --stage CLOSE
run approve --repo "$TARGET" --id "$ID" --stage CLOSE --decision APPROVED
run status --repo "$TARGET" --id "$ID"

python3 - "$TARGET" "$ID" <<'PY'
import json
import sys
from pathlib import Path
root = Path(sys.argv[1])
work_id = sys.argv[2]
state = json.loads((root/'.harness/state'/f'{work_id}.json').read_text(encoding='utf-8'))
assert state['title'] == '고객정보 조회', state['title']
for stage in ['REQUIREMENT','ANALYSIS','DESIGN','IMPLEMENTATION','TEST','CLOSE']:
    assert state['stages'][stage]['status'] == 'APPROVED', (stage, state['stages'][stage])
assert (root/'docs/work-items'/work_id/'test-evidence/test-summary.md').exists()
assert (root/'.harness/audit'/f'{work_id}.jsonl').exists()
print('FINAL_E2E_STATE_ASSERTIONS_PASS')
PY
printf '%s\n' "$MISSING_PLAN_STATUS" > "$OUT/missing-plan.exitcode"
printf '%s\n' "$SECRET_STATUS" > "$OUT/secret-command.exitcode"
printf '%s\n' 'E2E_SELF_TEST_PASS'
