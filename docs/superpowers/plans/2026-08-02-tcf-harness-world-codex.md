# TCF Harness World Codex Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Convert `harness-service-world` into a working Codex-native harness package under `tcf-harness-world` with role contracts, skills, offline orchestration, and automated verification.

**Architecture:** Keep the harness specification in Markdown and express Codex integration through `AGENTS.md`, focused role contracts, and `SKILL.md` files. Provide deterministic PowerShell and POSIX shell simulations that exchange explicit workspace artifacts, then verify structure, content, and forbidden runtime dependencies with platform-specific scripts.

**Tech Stack:** Markdown, Codex `AGENTS.md`/`SKILL.md`, PowerShell 5.1+, POSIX shell, Git, ripgrep.

## Global Constraints

- Do not modify `harness-service-world`, `harness-service`, Java modules, Gradle modules, or unrelated user changes.
- Do not require Claude CLI, `.claude/`, `CLAUDE.md`, or `.claude-plugin` at runtime.
- Use Codex collaboration concepts: `spawn_agent`, `send_message`, `followup_task`, and `wait_agent`.
- Default to project-local use; document optional reusable skill installation without writing to the user home directory.
- Preserve UTF-8 source and documentation.
- Simulations must be deterministic, bounded, rerunnable, and return nonzero on verification failure.

---

## File Structure

- `tcf-harness-world/AGENTS.md`: scoped Codex operating rules.
- `tcf-harness-world/README.md`: package map, architecture, and commands.
- `tcf-harness-world/docs/*.md`: quickstart and migration boundary.
- `tcf-harness-world/agents/*.md`: Analyst, Builder, and QA contracts.
- `tcf-harness-world/skills/harness/`: meta-skill, references, and sample.
- `tcf-harness-world/scripts/verify-codex-harness.*`: acceptance verifiers.
- `tcf-harness-world/tests/test-verifier.ps1`: contract and failure tests.

### Task 1: Codex Package Contracts and Documentation

**Files:**
- Create: `tcf-harness-world/AGENTS.md`
- Modify: `tcf-harness-world/README.md`
- Modify: `tcf-harness-world/docs/quickstart.md`
- Create: `tcf-harness-world/docs/migration-from-claude.md`
- Create: `tcf-harness-world/agents/analyst.md`
- Create: `tcf-harness-world/agents/builder.md`
- Create: `tcf-harness-world/agents/qa.md`
- Create: `tcf-harness-world/tests/test-verifier.ps1`

**Interfaces:**
- Consumes: `docs/superpowers/specs/2026-08-02-tcf-harness-world-codex-design.md`.
- Produces: role files with sections `핵심 역할`, `입력`, `출력`, `작업 원칙`, `오류 처리`, `협업`.

- [ ] **Step 1: Write the failing documentation contract test**

Create `test-verifier.ps1` with `-Mode Contracts`. Assert that `AGENTS.md`, both docs, and all role files exist; every role has the six required headings; and README contains `Codex`, `AGENTS.md`, `SKILL.md`, and `verify-codex-harness.ps1`.

- [ ] **Step 2: Run the contract test and verify failure**

Run: `powershell -NoProfile -ExecutionPolicy Bypass -File .\tcf-harness-world\tests\test-verifier.ps1 -Mode Contracts`

Expected: nonzero exit naming missing `AGENTS.md` and role contracts.

- [ ] **Step 3: Implement the documentation contracts**

Write project-local usage as the default. State that role files are prompt contracts and actual dispatch uses Codex collaboration tools. Restrict historical product references to the migration document.

- [ ] **Step 4: Run the contract test and verify success**

Run the Step 2 command. Expected: exit 0 with `Contract checks passed.`

- [ ] **Step 5: Commit**

```powershell
git add -- tcf-harness-world/AGENTS.md tcf-harness-world/README.md tcf-harness-world/docs tcf-harness-world/agents tcf-harness-world/tests/test-verifier.ps1
git commit -m "docs: define Codex harness contracts"
```

### Task 2: Codex-Native Harness Skill and References

**Files:**
- Modify: `tcf-harness-world/skills/harness/SKILL.md`
- Modify: `tcf-harness-world/skills/harness/references/agent-design-patterns.md`
- Modify: `tcf-harness-world/skills/harness/references/orchestrator-template.md`
- Create: `tcf-harness-world/skills/harness/references/codex-tool-mapping.md`
- Modify: `tcf-harness-world/skills/harness/orchestrator-sample/SKILL.md`

**Interfaces:**
- Consumes: Task 1 roles and headings.
- Produces: valid `name: harness` frontmatter, `Analyst → Builder → QA`, and all four Codex collaboration operations.

- [ ] **Step 1: Add failing skill assertions**

Add `-Mode Skills`. Assert YAML `name` and `description` in both skills, all four collaboration operations in the main skill, existence of referenced local files, and only `tcf-harness-world` sample paths.

- [ ] **Step 2: Run the skill test and verify failure**

Run: `powershell -NoProfile -ExecutionPolicy Bypass -File .\tcf-harness-world\tests\test-verifier.ps1 -Mode Skills`

Expected: nonzero exit naming the missing mapping and tool contracts.

- [ ] **Step 3: Implement the skill and references**

Define discovery, duplicate detection, role design, dispatch eligibility, artifact handoff, QA, and evolution phases. Replace placeholder links to source directories with complete local guidance.

- [ ] **Step 4: Run the skill test and verify success**

Run the Step 2 command. Expected: exit 0 with `Skill checks passed.`

- [ ] **Step 5: Commit**

```powershell
git add -- tcf-harness-world/skills/harness
git commit -m "feat: add Codex-native harness skill"
```

### Task 3: Deterministic Offline Orchestrator Simulation

**Files:**
- Create: `tcf-harness-world/skills/harness/orchestrator-sample/scripts/run-simulation.ps1`
- Modify: `tcf-harness-world/skills/harness/orchestrator-sample/scripts/run-simulation.bat`
- Modify: `tcf-harness-world/skills/harness/orchestrator-sample/scripts/run-simulation.sh`
- Modify: `tcf-harness-world/skills/harness/orchestrator-sample/_workspace/analysis-summary.md`
- Modify: `tcf-harness-world/skills/harness/orchestrator-sample/_workspace/poc-plan.md`

**Interfaces:**
- Consumes: Task 2 role sequence.
- Produces: nonempty `analysis-summary.md`, `poc-plan.md`, and `_runs/orchestrator-simulation.log`; returns 0 only on QA PASS.

- [ ] **Step 1: Add failing simulation assertions**

Add `-Mode Simulation`. Copy the sample to a unique temp directory, run `run-simulation.ps1`, assert both artifacts and `QA PASS`, rerun, and assert each output has exactly one title.

- [ ] **Step 2: Run the simulation test and verify failure**

Run: `powershell -NoProfile -ExecutionPolicy Bypass -File .\tcf-harness-world\tests\test-verifier.ps1 -Mode Simulation`

Expected: nonzero exit because the PowerShell entry point is absent.

- [ ] **Step 3: Implement atomic bounded simulations**

PowerShell accepts optional `-Workspace` and `-RunDirectory`, writes UTF-8 temporary files, moves them into place, validates nonempty outputs, writes a fresh log, and exits 1 on QA failure. Batch delegates to PowerShell. Shell uses destination-local `mktemp`, `mv`, and `trap` cleanup with the same contract.

- [ ] **Step 4: Run simulation entry points**

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\tcf-harness-world\tests\test-verifier.ps1 -Mode Simulation
cmd /c tcf-harness-world\skills\harness\orchestrator-sample\scripts\run-simulation.bat
```

Expected: both exit 0; artifacts are nonempty; log contains `QA PASS`.

- [ ] **Step 5: Commit**

```powershell
git add -- tcf-harness-world/skills/harness/orchestrator-sample tcf-harness-world/tests/test-verifier.ps1
git commit -m "feat: add deterministic harness simulation"
```

### Task 4: Package Verifiers and Acceptance Tests

**Files:**
- Create: `tcf-harness-world/scripts/verify-codex-harness.ps1`
- Create: `tcf-harness-world/scripts/verify-codex-harness.sh`
- Modify: `tcf-harness-world/tests/test-verifier.ps1`
- Delete: `tcf-harness-world/.github/scripts/check_claude_plugin.sh` if present
- Modify: `tcf-harness-world/.github/workflows/ci.yml` if present

**Interfaces:**
- Consumes: Tasks 1–3 package contracts.
- Produces: stable verifier commands returning 0 for a valid tree and 1 with file-specific diagnostics for an invalid tree.

- [ ] **Step 1: Write failing verifier integration tests**

Add `-Mode Verifier`. Run the future verifier on the real tree, copy the tree to temp, inject a forbidden runtime path in README and expect failure, then remove `agents/qa.md` and expect missing-file failure.

- [ ] **Step 2: Run the verifier test and verify failure**

Run: `powershell -NoProfile -ExecutionPolicy Bypass -File .\tcf-harness-world\tests\test-verifier.ps1 -Mode Verifier`

Expected: nonzero exit because verifier scripts are absent.

- [ ] **Step 3: Implement verifiers and CI command**

Check required files, role headings, skill frontmatter, relative Markdown links, script presence, and forbidden runtime tokens outside the migration document and explicit test fixtures. Print all failures and exit 1 if any exist. Replace any legacy plugin check in CI with the package verifier.

- [ ] **Step 4: Run full acceptance verification**

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\tcf-harness-world\tests\test-verifier.ps1 -Mode All
powershell -NoProfile -ExecutionPolicy Bypass -File .\tcf-harness-world\scripts\verify-codex-harness.ps1
git diff --check -- tcf-harness-world docs/superpowers/plans/2026-08-02-tcf-harness-world-codex.md
rg -n -i "claude|anthropic|\.claude|CLAUDE\.md|\.claude-plugin" tcf-harness-world
```

Expected: tests and verifier exit 0; diff check is silent; search results are limited to the migration document and explicit verifier fixtures or allowlist logic.

- [ ] **Step 5: Inspect scope and commit**

```powershell
git status --short
git diff -- tcf-harness-world
git add -- tcf-harness-world docs/superpowers/plans/2026-08-02-tcf-harness-world-codex.md
git commit -m "test: verify Codex harness package"
```

Do not stage unrelated existing modifications or untracked directories.
