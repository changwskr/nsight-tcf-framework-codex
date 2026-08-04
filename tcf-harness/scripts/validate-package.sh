#!/usr/bin/env sh
set -eu
ROOT=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
cd "$ROOT"

required_files='README.md
AGENTS.md
ARCHITECTURE.md
build.gradle
settings.gradle
harness/prompts/MASTER-HARNESS.md
harness/prompts/00-ALL-IN-ONE-HARNESS.md
harness/prompts/01-REQUIREMENT.md
harness/prompts/02-ANALYSIS.md
harness/prompts/03-DESIGN.md
harness/prompts/04-IMPLEMENTATION.md
harness/prompts/05-TEST.md
harness/prompts/06-CLOSE.md
harness/schemas/work-item-state.schema.json
src/main/java/com/nh/nsight/harness/HarnessApplication.java
src/main/java/com/nh/nsight/harness/cli/HarnessCommandRouter.java'

printf '%s\n' "$required_files" | while IFS= read -r file; do
  [ -f "$file" ] || { echo "Missing required file: $file" >&2; exit 1; }
done

for prompt in harness/prompts/*.md; do
  resource="src/main/resources/harness/prompts/$(basename "$prompt")"
  cmp -s "$prompt" "$resource" || { echo "Prompt resource drift: $prompt" >&2; exit 1; }
done

python3 - <<'PY'
import json
import re
from pathlib import Path
root = Path('.')
for path in sorted((root/'harness/schemas').glob('*.json')):
    json.loads(path.read_text(encoding='utf-8'))
for path in [root/'README.md', root/'AGENTS.md', root/'ARCHITECTURE.md']:
    text = path.read_text(encoding='utf-8')
    for target in re.findall(r'\[[^\]]+\]\(([^)]+)\)', text):
        if target.startswith(('http://','https://','#')) or '${' in target:
            continue
        resolved = (path.parent / target).resolve()
        if not resolved.exists():
            raise SystemExit(f'Broken local link: {path} -> {target}')
print('JSON_AND_LINK_VALIDATION_PASS')
PY

if grep -RInE 'TODO|TBD|FIXME|implement later|fill in details' \
  src harness README.md AGENTS.md ARCHITECTURE.md docs/guides docs/references \
  --exclude='*.log'; then
  echo 'Placeholder text found' >&2
  exit 1
fi

sh scripts/offline-smoke-test.sh

git diff --check
printf '%s\n' 'PACKAGE_VALIDATION_PASS'
