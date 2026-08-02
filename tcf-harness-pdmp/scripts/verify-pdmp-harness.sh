#!/usr/bin/env sh
set -eu

ROOT=''
TARGET_ROOT=''

while [ "$#" -gt 0 ]; do
    case "$1" in
        --root) ROOT=$2; shift 2 ;;
        --target-root) TARGET_ROOT=$2; shift 2 ;;
        *) if [ -z "$ROOT" ]; then ROOT=$1; shift; else printf 'Unknown argument: %s\n' "$1" >&2; exit 1; fi ;;
    esac
done

if [ -z "$ROOT" ]; then
    ROOT=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
elif ! ROOT=$(CDPATH= cd -- "$ROOT" && pwd); then
    printf 'Invalid harness root: %s\n' "$ROOT" >&2
    exit 1
fi

if [ -z "$TARGET_ROOT" ]; then TARGET_ROOT=$(dirname -- "$ROOT")/pdmp-service; fi
if ! TARGET_ROOT=$(CDPATH= cd -- "$TARGET_ROOT" && pwd); then
    printf 'Invalid pdmp-service target: %s\n' "$TARGET_ROOT" >&2
    exit 1
fi

failed=0
failure_marker=$(mktemp "${TMPDIR:-/tmp}/pdmp-harness-verify.XXXXXX")
trap 'rm -f "$failure_marker"' EXIT HUP INT TERM
fail() { printf '%s\n' "$1" >&2; printf 'failed\n' > "$failure_marker"; failed=1; }
require_file() { [ -f "$ROOT/$1" ] || fail "Missing file: $1"; }

for path in AGENTS.md README.md docs/quickstart.md docs/pdmp-architecture.md docs/workflow.md agents/pdmp-analyst.md agents/pdmp-builder.md agents/pdmp-security-reviewer.md agents/pdmp-qa.md skills/pdmp-development/SKILL.md skills/pdmp-development/references/pdmp-project-map.md skills/pdmp-development/references/handoff-protocol.md skills/pdmp-crud/SKILL.md skills/pdmp-crud/references/crud-checklist.md skills/pdmp-tcf/SKILL.md skills/pdmp-security/SKILL.md skills/pdmp-quality/SKILL.md samples/pdmp-development/scripts/run-simulation.ps1 samples/pdmp-development/scripts/run-simulation.bat samples/pdmp-development/scripts/run-simulation.sh samples/pdmp-development/workspace/analysis-summary.md samples/pdmp-development/workspace/verification-report.md samples/pdmp-development/workspace/security-review.md samples/pdmp-development/workspace/qa-report.md; do
    require_file "$path"
done

for role in agents/pdmp-analyst.md agents/pdmp-builder.md agents/pdmp-security-reviewer.md agents/pdmp-qa.md; do
    if [ -f "$ROOT/$role" ]; then
        count=$(grep -Ec '^## [^#[:cntrl:]]+$' "$ROOT/$role" || true)
        [ "$count" -eq 6 ] || fail "Expected 6 second-level sections in $role, found $count"
    fi
done

for skill in skills/pdmp-development/SKILL.md skills/pdmp-crud/SKILL.md skills/pdmp-tcf/SKILL.md skills/pdmp-security/SKILL.md skills/pdmp-quality/SKILL.md; do
    if [ -f "$ROOT/$skill" ] && ! awk '
        NR == 1 { valid = ($0 == "---"); next }
        valid && ! closed && $0 ~ /^name:[[:space:]]*[^[:space:]]/ { name = 1 }
        valid && ! closed && $0 ~ /^description:[[:space:]]*[^[:space:]]/ { description = 1 }
        valid && NR > 1 && $0 == "---" { closed = 1; exit }
        END { exit !(valid && closed && name && description) }
    ' "$ROOT/$skill"; then
        fail "Invalid skill frontmatter: $skill"
    fi
done

for path in docs/migration-from-claude.md agents/analyst.md agents/builder.md agents/qa.md skills/harness samples/orchestrator-sample scripts/verify-codex-harness.ps1 scripts/verify-codex-harness.sh tests/test-verifier.ps1; do
    [ ! -e "$ROOT/$path" ] || fail "Copied legacy path remains: $path"
done

find "$ROOT" -type f \( -name '*.md' -o -name '*.ps1' -o -name '*.bat' -o -name '*.sh' \) -print |
while IFS= read -r file; do
    case "$file" in
        "$ROOT"/tests/*|"$ROOT"/scripts/verify-pdmp-harness.ps1|"$ROOT"/scripts/verify-pdmp-harness.sh) continue ;;
    esac
    if grep -qE 'tcf-harness-world|skills/harness|orchestrator-sample|verify-codex-harness|migration-from-claude|agents/(analyst|builder|qa)\.md' "$file"; then
        fail "Forbidden copied reference: ${file#"$ROOT"/}"
    fi
done

find "$ROOT" -type f -name '*.md' -print |
while IFS= read -r file; do
    awk '{
        rest = $0
        while (match(rest, /\[[^]]+\]\([^)]+\)/)) {
            link = substr(rest, RSTART, RLENGTH)
            sub(/^[^(]*\(/, "", link)
            sub(/\)$/, "", link)
            print link
            rest = substr(rest, RSTART + RLENGTH)
        }
    }' "$file" |
    while IFS= read -r link; do
        case "$link" in http://*|https://*|mailto:*|\#*) continue ;; esac
        link=${link%%#*}
        [ -z "$link" ] && continue
        [ -e "$(dirname -- "$file")/$link" ] || fail "Broken Markdown link: ${file#"$ROOT"/} -> $link"
    done
done

for path in gradlew.bat build.gradle settings.gradle src/main/java/nhnis/mp/PdmpApplication.java src/main/java/nhnis/mp/config/SecurityConfig.java src/main/java/nhnis/mp/co/a/controller/mpcoa8888Controller.java src/main/java/nhnis/mp/co/a/service/mpcoa8888Service.java src/main/java/nhnis/mp/co/a/dao/mpcoa8888Dao.java src/main/resources/rdw.mp.co.a/mpcoa8888-ORA.xml src/test/java/nhnis/mp/co/a/controller/mpcoa8888ControllerTest.java src/test/java/nhnis/mp/co/a/service/mpcoa8888ServiceTest.java src/test/java/nhnis/mp/co/a/dao/mpcoa8888DaoIntegrationTest.java; do
    [ -f "$TARGET_ROOT/$path" ] || fail "Missing pdmp-service target file: $path"
done

[ "$failed" -eq 0 ] && [ ! -s "$failure_marker" ] || exit 1
printf '%s\n' 'PDMP harness verification passed.'
