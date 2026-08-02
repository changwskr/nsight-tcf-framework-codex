#!/usr/bin/env sh
set -eu
ROOT=${1:-"$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)"}; failed=0
for path in AGENTS.md README.md docs/quickstart.md docs/migration-from-claude.md agents/analyst.md agents/builder.md agents/qa.md skills/harness/SKILL.md skills/harness/orchestrator-sample/SKILL.md skills/harness/references/codex-tool-mapping.md; do if [ ! -f "$ROOT/$path" ]; then printf 'Missing file: %s\n' "$path" >&2; failed=1; fi; done
if [ -f "$ROOT/.github/scripts/check_claude_plugin.sh" ]; then printf '%s\n' 'Legacy plugin check still exists' >&2; failed=1; fi
if grep -RniE '(\.claude/|(^|[^[:alnum:]_-])CLAUDE\.md|\.claude-plugin|Claude CLI|Anthropic API)' "$ROOT" --include='*.md' --include='*.sh' --include='*.ps1' --include='*.bat' --exclude='migration-from-claude.md' --exclude='test-verifier.ps1' --exclude='verify-codex-harness.ps1' --exclude='verify-codex-harness.sh'; then printf '%s\n' 'Forbidden runtime reference found' >&2; failed=1; fi
if [ "$failed" -ne 0 ]; then exit 1; fi; printf '%s\n' 'Codex harness verification passed.'
