# TCF Harness PDMP Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Transform `tcf-harness-pdmp` into an independent Codex harness dedicated to developing and verifying `pdmp-service`.

**Architecture:** Replace the copied generic harness with PDMP-specific project instructions, four role contracts, one development orchestrator skill, and four focused sub-skills. Keep `pdmp-service` unchanged and validate the harness with deterministic PowerShell/POSIX scripts plus controlled failure scenarios.

**Tech Stack:** Markdown, Codex `AGENTS.md`/`SKILL.md`, PowerShell 5.1+, POSIX shell, Java 21, Gradle Wrapper, Spring Boot, MyBatis.

## Global Constraints

- The only default target is `pdmp-service`.
- Preserve `pdmp-service` Controller → Service → DAO → MyBatis architecture.
- Do not modify `pdmp-service`, `tcf-harness-world`, or unrelated user changes.
- Remove runtime and documentation dependence on `tcf-harness-world` from `tcf-harness-pdmp`.
- Validate `@TcfTransaction`, `MP.{Domain}.{action}`, transactionCode, ProcessingType, JWT/security, personal data, SQL binding, tests, and WAR build guidance.
- Preserve UTF-8 and use explicit file paths and observable completion criteria.

---

## File Structure

- `tcf-harness-pdmp/AGENTS.md`: scoped PDMP development rules.
- `tcf-harness-pdmp/README.md`: independent package entrypoint and commands.
- `tcf-harness-pdmp/docs/{quickstart,pdmp-architecture,workflow}.md`: target architecture and usage.
- `tcf-harness-pdmp/agents/pdmp-*.md`: Analyst, Builder, Security Reviewer, QA contracts.
- `tcf-harness-pdmp/skills/pdmp-*/SKILL.md`: development, CRUD, TCF, security, and quality workflows.
- `tcf-harness-pdmp/skills/pdmp-development/references/*.md`: target map and handoff protocol.
- `tcf-harness-pdmp/skills/pdmp-crud/references/*.md`: CRUD artifact and metadata checklist.
- `tcf-harness-pdmp/samples/pdmp-development/`: deterministic four-role simulation.
- `tcf-harness-pdmp/scripts/verify-pdmp-harness.*`: static package verifiers.
- `tcf-harness-pdmp/tests/test-pdmp-harness.ps1`: acceptance and mutation tests.

### Task 1: Independent PDMP Project Contracts

**Files:**
- Replace: `tcf-harness-pdmp/AGENTS.md`
- Replace: `tcf-harness-pdmp/README.md`
- Replace: `tcf-harness-pdmp/docs/quickstart.md`
- Create: `tcf-harness-pdmp/docs/pdmp-architecture.md`
- Create: `tcf-harness-pdmp/docs/workflow.md`
- Replace directory: `tcf-harness-pdmp/agents/`
- Create: `tcf-harness-pdmp/tests/test-pdmp-harness.ps1`

**Interfaces:**
- Consumes: approved design and current `pdmp-service` structure.
- Produces: four role files with six second-level sections; commands that reference only `tcf-harness-pdmp` and `pdmp-service`.

- [ ] **Step 1: Write failing contract tests**

Create `test-pdmp-harness.ps1 -Mode Contracts`. Require `pdmp-analyst.md`, `pdmp-builder.md`, `pdmp-security-reviewer.md`, `pdmp-qa.md`, three docs, and README tokens `TCF Harness PDMP`, `pdmp-service`, `verify-pdmp-harness.ps1`. Reject any `tcf-harness-world` text outside the test fixture itself.

- [ ] **Step 2: Verify RED**

Run: `powershell -NoProfile -ExecutionPolicy Bypass -File .\tcf-harness-pdmp\tests\test-pdmp-harness.ps1 -Mode Contracts`

Expected: exit 1 naming missing PDMP roles and stale world paths.

- [ ] **Step 3: Implement independent contracts**

Write the exact Controller → Service → DAO → MyBatis target, program ID and transaction metadata rules, H2/Oracle distinction, security constraints, and project-local commands. Remove copied migration documentation and generic role files.

- [ ] **Step 4: Verify GREEN**

Run the Step 2 command. Expected: exit 0 with `Contracts checks passed.`

- [ ] **Step 5: Commit**

```powershell
git add -- tcf-harness-pdmp/AGENTS.md tcf-harness-pdmp/README.md tcf-harness-pdmp/docs tcf-harness-pdmp/agents tcf-harness-pdmp/tests/test-pdmp-harness.ps1
git commit -m "docs: define independent PDMP harness"
```

### Task 2: PDMP Development and Specialist Skills

**Files:**
- Replace directory: `tcf-harness-pdmp/skills/harness/`
- Create: `tcf-harness-pdmp/skills/pdmp-development/SKILL.md`
- Create: `tcf-harness-pdmp/skills/pdmp-development/references/pdmp-project-map.md`
- Create: `tcf-harness-pdmp/skills/pdmp-development/references/handoff-protocol.md`
- Create: `tcf-harness-pdmp/skills/pdmp-crud/SKILL.md`
- Create: `tcf-harness-pdmp/skills/pdmp-crud/references/crud-checklist.md`
- Create: `tcf-harness-pdmp/skills/pdmp-tcf/SKILL.md`
- Create: `tcf-harness-pdmp/skills/pdmp-security/SKILL.md`
- Create: `tcf-harness-pdmp/skills/pdmp-quality/SKILL.md`

**Interfaces:**
- Consumes: Task 1 role paths and PDMP architecture.
- Produces: `pdmp-development` orchestration and four independently triggerable specialist skills.

- [ ] **Step 1: Add failing skill tests**

Add `-Mode Skills`. Require YAML `name` and `description`, local references, all four role paths in `pdmp-development`, CRUD actions `list/detail/create/update/delete`, TCF metadata tokens, security review tokens, and `gradlew.bat test` plus `gradlew.bat war` in quality guidance. Reject `skills/harness` and `orchestrator-sample`.

- [ ] **Step 2: Verify RED**

Run: `powershell -NoProfile -ExecutionPolicy Bypass -File .\tcf-harness-pdmp\tests\test-pdmp-harness.ps1 -Mode Skills`

Expected: exit 1 naming missing specialist skills.

- [ ] **Step 3: Implement focused skills**

Make `pdmp-development` select only the required sub-skills. Document exact source/resource/test locations, `@TcfTransaction` fields, safe delete decision, SQL binding, JWT/CORS/log review, H2 verification, and Oracle limitations.

- [ ] **Step 4: Verify GREEN**

Run the Step 2 command. Expected: exit 0 with `Skills checks passed.`

- [ ] **Step 5: Commit**

```powershell
git add -- tcf-harness-pdmp/skills tcf-harness-pdmp/tests/test-pdmp-harness.ps1
git commit -m "feat: add PDMP development skills"
```

### Task 3: Four-Role Offline Simulation

**Files:**
- Replace directory: `tcf-harness-pdmp/skills/harness/orchestrator-sample/`
- Create: `tcf-harness-pdmp/samples/pdmp-development/scripts/run-simulation.ps1`
- Create: `tcf-harness-pdmp/samples/pdmp-development/scripts/run-simulation.bat`
- Create: `tcf-harness-pdmp/samples/pdmp-development/scripts/run-simulation.sh`
- Create: sample workspace artifacts for analysis, implementation verification, security review, and QA.

**Interfaces:**
- Consumes: Task 1 roles and Task 2 handoff protocol.
- Produces: four nonempty artifacts and `_runs/pdmp-development-simulation.log` containing `QA PASS`.

- [ ] **Step 1: Add failing simulation tests**

Add `-Mode Simulation`. Run the PowerShell simulator twice in a unique temp directory; require exactly one title in each artifact, `QA PASS`, and exit 0. Run a failure switch that omits `security-review.md` and require exit 1 with `QA FAIL`.

- [ ] **Step 2: Verify RED**

Run: `powershell -NoProfile -ExecutionPolicy Bypass -File .\tcf-harness-pdmp\tests\test-pdmp-harness.ps1 -Mode Simulation`

Expected: exit 1 because the PDMP simulator is absent.

- [ ] **Step 3: Implement atomic deterministic simulation**

PowerShell accepts `-Workspace`, `-RunDirectory`, and `-OmitSecurityReview`; writes UTF-8 temp files then moves them; batch delegates to PowerShell; shell uses destination-local `mktemp`, `trap`, and equivalent positional arguments.

- [ ] **Step 4: Verify GREEN**

Run the Step 2 command and `cmd /c tcf-harness-pdmp\samples\pdmp-development\scripts\run-simulation.bat`. Expected: both success paths exit 0 and the mutation path is asserted as exit 1.

- [ ] **Step 5: Commit**

```powershell
git add -- tcf-harness-pdmp/samples tcf-harness-pdmp/tests/test-pdmp-harness.ps1 tcf-harness-pdmp/.gitignore
git commit -m "feat: add PDMP workflow simulation"
```

### Task 4: Independent Verifiers and Target Checks

**Files:**
- Replace: `tcf-harness-pdmp/scripts/verify-codex-harness.ps1`
- Replace: `tcf-harness-pdmp/scripts/verify-codex-harness.sh`
- Create: `tcf-harness-pdmp/scripts/verify-pdmp-harness.ps1`
- Create: `tcf-harness-pdmp/scripts/verify-pdmp-harness.sh`
- Modify: `tcf-harness-pdmp/tests/test-pdmp-harness.ps1`
- Remove copied obsolete files after their PDMP replacements exist.

**Interfaces:**
- Consumes: Tasks 1–3 contracts.
- Produces: verifier exit 0 for the real harness and exit 1 for stale world path, missing role, missing skill, or missing `pdmp-service` target.

- [ ] **Step 1: Add failing verifier mutation tests**

Add `-Mode Verifier`. Verify the real tree, then copy it to temp and independently inject `tcf-harness-world`, delete `agents/pdmp-qa.md`, delete `skills/pdmp-security/SKILL.md`, and point the verifier at a missing target. Each mutation must return nonzero.

- [ ] **Step 2: Verify RED**

Run: `powershell -NoProfile -ExecutionPolicy Bypass -File .\tcf-harness-pdmp\tests\test-pdmp-harness.ps1 -Mode Verifier`

Expected: exit 1 because `verify-pdmp-harness.ps1` is absent.

- [ ] **Step 3: Implement verifiers and remove copied surface**

Validate required files, frontmatter, six role sections, local Markdown links, forbidden copied names, target directory and representative `pdmp-service` files. Keep project source validation read-only. Remove `docs/migration-from-claude.md`, generic agents, `skills/harness`, old verifier names, and copied sample after replacements pass.

- [ ] **Step 4: Run full acceptance**

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\tcf-harness-pdmp\tests\test-pdmp-harness.ps1 -Mode All
powershell -NoProfile -ExecutionPolicy Bypass -File .\tcf-harness-pdmp\scripts\verify-pdmp-harness.ps1
git diff --check -- tcf-harness-pdmp
rg -n "tcf-harness-world|skills/harness|orchestrator-sample" tcf-harness-pdmp
```

Expected: tests and verifier exit 0; diff check and search are silent except explicit mutation strings inside tests/verifier logic.

- [ ] **Step 5: Run target project verification**

```powershell
.\pdmp-service\gradlew.bat test
.\pdmp-service\gradlew.bat war
```

Expected: both exit 0. If dependency or closed-network constraints prevent execution, record the exact command, failure, and unverified scope without changing `pdmp-service`.

- [ ] **Step 6: Commit**

```powershell
git add -- tcf-harness-pdmp
git commit -m "test: verify independent PDMP harness"
```

Do not stage unrelated existing changes.
