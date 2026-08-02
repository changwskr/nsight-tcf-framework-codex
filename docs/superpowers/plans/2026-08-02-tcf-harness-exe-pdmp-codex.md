# tcf-harness-exe-pdmp Codex Parallel Harness Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Preserve every existing `tcf-harness-exe-pdmp` file while adding an independently runnable Codex harness for staged `pdmp-service` development.

**Architecture:** Keep the Claude surface immutable and add a parallel Codex surface that shares only the existing phase JSON/Markdown format. A standalone Python executor invokes `codex exec` against a resolved `pdmp-service` target, while Codex-native instructions and focused PDMP skills govern analysis, approval, implementation, security review, and QA.

**Tech Stack:** Python 3 standard library, pytest, Codex CLI `codex exec`, PowerShell verification, Markdown/JSON contracts, Java 21, Spring Boot 3.5.14, Gradle WAR, MyBatis, H2/Oracle.

## Global Constraints

- Do not modify `tcf-harness-exe-pdmp/CLAUDE.md`, `tcf-harness-exe-pdmp/.claude/**`, `tcf-harness-exe-pdmp/scripts/execute.py`, `tcf-harness-exe-pdmp/scripts/test_execute.py`, or the four existing files under `tcf-harness-exe-pdmp/docs/`.
- Reuse the existing `phases/PHASE_NAME/index.json`, `stepN.md`, and `stepN-output.json` contract without coupling the new executor to `scripts/execute.py`.
- Resolve `../pdmp-service` as the default target and allow `--target TARGET_PATH` to override it.
- Never stage or commit unrelated user changes; automatic commit and push remain disabled unless explicitly requested.
- Preserve UTF-8 and use parameterized subprocess argument arrays, never shell interpolation.
- PDMP code follows `Controller -> Service -> DAO -> MyBatis XML`, with `@TcfTransaction`, JWT boundaries, bound SQL, sanitized logs, H2 verification, and explicit Oracle limitations.
- Each implementation task must use test-first RED/GREEN evidence and commit only its listed paths.

---

## File Map

New files are grouped by one responsibility:

- `tcf-harness-exe-pdmp/preservation-manifest.json`: SHA-256 baseline for every pre-existing file.
- `tcf-harness-exe-pdmp/AGENTS.md`: Codex entry instructions and PDMP scope boundary.
- `tcf-harness-exe-pdmp/README.md`: dual Claude/Codex entry points.
- `tcf-harness-exe-pdmp/docs/CODEX_QUICKSTART.md`: Codex installation, execution, resume, and recovery commands.
- `tcf-harness-exe-pdmp/docs/PDMP_CODEX_WORKFLOW.md`: approval gates, artifacts, and handoffs.
- `tcf-harness-exe-pdmp/skills/pdmp-*/SKILL.md`: orchestration, CRUD, TCF, security, and QA contracts.
- `tcf-harness-exe-pdmp/skills/pdmp-development/references/*.md`: target map and handoff artifact schemas.
- `tcf-harness-exe-pdmp/scripts/execute_codex.py`: Codex-only phase executor.
- `tcf-harness-exe-pdmp/scripts/test_execute_codex.py`: executor unit tests with mocked subprocesses.
- `tcf-harness-exe-pdmp/scripts/verify_codex_harness.ps1`: preservation and static contract verifier.
- `tcf-harness-exe-pdmp/tests/test_codex_harness.ps1`: end-to-end harness contract suite.
- `tcf-harness-exe-pdmp/phases/index.json`: top-level phase catalog.
- `tcf-harness-exe-pdmp/phases/user-information-crud/*`: PDMP example phase and dry-run fixtures.

---

### Task 1: Freeze the Existing Source Contract

**Files:**
- Create: `tcf-harness-exe-pdmp/preservation-manifest.json`
- Create: `tcf-harness-exe-pdmp/scripts/verify_codex_harness.ps1`
- Create: `tcf-harness-exe-pdmp/tests/test_codex_harness.ps1`

**Interfaces:**
- Consumes: the current on-disk contents of `CLAUDE.md`, `.claude/**`, `scripts/execute.py`, `scripts/test_execute.py`, and existing `docs/**`.
- Produces: `Test-Preservation`, a PowerShell verifier entry point accepting `-Root`, and a committed SHA-256 map keyed by forward-slash relative paths.

- [ ] **Step 1: Generate and inspect the immutable-file inventory**

Run:

```powershell
Get-ChildItem tcf-harness-exe-pdmp\.claude -File -Recurse
Get-Item tcf-harness-exe-pdmp\CLAUDE.md,tcf-harness-exe-pdmp\scripts\execute.py,tcf-harness-exe-pdmp\scripts\test_execute.py
Get-ChildItem tcf-harness-exe-pdmp\docs -File
```

Expected: exactly the current Claude files, two existing Python files, and `ADR.md`, `ARCHITECTURE.md`, `PRD.md`, `UI_GUIDE.md` are listed.

- [ ] **Step 2: Write the failing preservation test**

Add a `Preservation` mode to `tests/test_codex_harness.ps1` that loads `preservation-manifest.json`, recomputes each file with `Get-FileHash -Algorithm SHA256`, and fails for a missing file, an unknown hash, or a hash mismatch. Include a mutation test that copies the harness to a temp directory, appends one byte to `CLAUDE.md`, and asserts the verifier exits nonzero.

- [ ] **Step 3: Run the test to verify RED**

Run:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\tcf-harness-exe-pdmp\tests\test_codex_harness.ps1 -Mode Preservation
```

Expected: FAIL because `preservation-manifest.json` and `verify_codex_harness.ps1` do not exist.

- [ ] **Step 4: Add the minimal manifest and verifier**

Create a UTF-8 JSON object with this shape, using actual uppercase SHA-256 values from `Get-FileHash`:

```json
{
  "algorithm": "SHA256",
  "files": {
    "CLAUDE.md": "SHA-256 value produced by Get-FileHash",
    ".claude/commands/harness.md": "SHA-256 value produced by Get-FileHash"
  }
}
```

Include every immutable file, not only the two illustrated keys. Implement `verify_codex_harness.ps1` with `param([string]$Root = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path)`, literal paths, JSON parsing, missing-file checks, and exact case-insensitive hash comparison. It must return exit code `0` only when every entry matches.

- [ ] **Step 5: Run the preservation test to verify GREEN**

Run the Step 3 command again.

Expected: `Preservation checks passed.` and exit code `0`; the isolated mutation must be rejected internally.

- [ ] **Step 6: Commit only the preservation contract**

```powershell
git add -- tcf-harness-exe-pdmp/preservation-manifest.json tcf-harness-exe-pdmp/scripts/verify_codex_harness.ps1 tcf-harness-exe-pdmp/tests/test_codex_harness.ps1
git commit -m "test: freeze executable PDMP harness sources"
```

---

### Task 2: Add Codex-Native PDMP Instructions and Skills

**Files:**
- Create: `tcf-harness-exe-pdmp/AGENTS.md`
- Create: `tcf-harness-exe-pdmp/skills/pdmp-development/SKILL.md`
- Create: `tcf-harness-exe-pdmp/skills/pdmp-development/references/pdmp-project-map.md`
- Create: `tcf-harness-exe-pdmp/skills/pdmp-development/references/handoff-protocol.md`
- Create: `tcf-harness-exe-pdmp/skills/pdmp-crud/SKILL.md`
- Create: `tcf-harness-exe-pdmp/skills/pdmp-tcf/SKILL.md`
- Create: `tcf-harness-exe-pdmp/skills/pdmp-security/SKILL.md`
- Create: `tcf-harness-exe-pdmp/skills/pdmp-quality/SKILL.md`
- Modify: `tcf-harness-exe-pdmp/tests/test_codex_harness.ps1`

**Interfaces:**
- Consumes: the approved design and actual `pdmp-service` paths discovered with `rg --files pdmp-service`.
- Produces: stable skill names `pdmp-development`, `pdmp-crud`, `pdmp-tcf`, `pdmp-security`, and `pdmp-quality`; handoff artifacts `analysis-summary.md`, `implementation-plan.md`, `verification-report.md`, `security-review.md`, and `qa-report.md`.

- [ ] **Step 1: Write failing Codex-contract assertions**

Add a `Contracts` mode asserting all listed files exist and contain exact tokens for:

```text
Controller -> Service -> DAO -> MyBatis
@TcfTransaction
MP.{Domain}.{action}
analysis -> user approval -> implementation plan -> implementation -> security review -> QA
../pdmp-service
```

Assert all five `SKILL.md` files have YAML frontmatter with matching `name:` values and that every relative Markdown link resolves.

- [ ] **Step 2: Run contract tests to verify RED**

Run:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\tcf-harness-exe-pdmp\tests\test_codex_harness.ps1 -Mode Contracts
```

Expected: FAIL listing the missing Codex instruction and skill files.

- [ ] **Step 3: Implement the minimal instruction surface**

Write `AGENTS.md` with these mandatory rules: target only the resolved PDMP root, preserve user changes, require explicit approval before implementation, enforce TDD, require security review for authentication/authorization/SQL/logging/personal-data changes, and record exact commands plus exit codes.

Write focused skills with these boundaries:

```text
pdmp-development: owns sequence and handoffs
pdmp-crud: owns list/detail/create/update/delete patterns
pdmp-tcf: owns serviceId/transactionCode/processingType/header/MDC checks
pdmp-security: owns JWT/CORS/SQL binding/secrets/privacy/error exposure
pdmp-quality: owns fresh test, WAR, evidence, and Oracle-unverified reporting
```

The project map must reference the real `mpcoa8888` Controller, Service, DAO, mapper, tests, `SecurityConfig`, `JwtAuthenticationFilter`, and TCF framework classes. The handoff protocol must define required headings and stop conditions for all five artifacts.

- [ ] **Step 4: Run contract and preservation tests to verify GREEN**

Run:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\tcf-harness-exe-pdmp\tests\test_codex_harness.ps1 -Mode Contracts
powershell -NoProfile -ExecutionPolicy Bypass -File .\tcf-harness-exe-pdmp\tests\test_codex_harness.ps1 -Mode Preservation
```

Expected: both commands exit `0` and preservation hashes remain unchanged.

- [ ] **Step 5: Commit the Codex contracts**

```powershell
git add -- tcf-harness-exe-pdmp/AGENTS.md tcf-harness-exe-pdmp/skills tcf-harness-exe-pdmp/tests/test_codex_harness.ps1
git commit -m "feat: add Codex PDMP workflow contracts"
```

---

### Task 3: Build the Independent Codex Phase Executor

**Files:**
- Create: `tcf-harness-exe-pdmp/scripts/execute_codex.py`
- Create: `tcf-harness-exe-pdmp/scripts/test_execute_codex.py`
- Modify: `tcf-harness-exe-pdmp/tests/test_codex_harness.ps1`

**Interfaces:**
- Consumes: `StepExecutor(phase_dir_name: str, target: Optional[Path] = None, timeout_seconds: int = 1800, max_retries: int = 3)` and existing phase JSON.
- Produces: `resolve_target(root: Path, supplied: Optional[str]) -> Path`, `build_codex_command(target: Path) -> list[str]`, `invoke_codex(step: dict, preamble: str) -> dict`, and CLI `python scripts/execute_codex.py PHASE [--target PATH] [--dry-run]`.

- [ ] **Step 1: Write failing target and CLI tests**

In `test_execute_codex.py`, create temporary harness/target/phase fixtures and assert:

```python
assert resolve_target(harness, None) == harness.parent / "pdmp-service"
assert resolve_target(harness, "custom").is_absolute()
assert build_codex_command(target)[:2] == ["codex", "exec"]
assert "--dangerously-bypass-approvals-and-sandbox" not in build_codex_command(target)
```

Also assert missing targets and targets without `build.gradle` raise a typed `HarnessError`, and `--help` documents `--target` and `--dry-run`.

- [ ] **Step 2: Run focused tests to verify RED**

Run:

```powershell
python -m pytest .\tcf-harness-exe-pdmp\scripts\test_execute_codex.py -q
```

Expected: FAIL because `execute_codex` cannot be imported.

- [ ] **Step 3: Implement path resolution and command construction**

Implement the functions with `Path.resolve()`, `subprocess.run(command, cwd=target, input=prompt, text=True, capture_output=True, timeout=self.timeout_seconds)`, and this safe command contract:

```python
["codex", "exec", "-", "--cd", str(target), "--sandbox", "workspace-write", "--ask-for-approval", "never", "--color", "never"]
```

Do not use `shell=True`, implicit push, or the dangerous bypass flag.

- [ ] **Step 4: Write failing state-transition tests**

Cover `pending -> completed`, three retries ending in `error`, immediate `blocked`, timeout output, accumulated completed-step summaries, missing step Markdown, malformed JSON, and dry-run command display without subprocess invocation. Mock `subprocess.run`; never call a real Codex session from unit tests.

- [ ] **Step 5: Run state tests to verify RED**

Run the Step 2 command.

Expected: new state tests FAIL at the first unimplemented transition.

- [ ] **Step 6: Implement the minimal executor loop**

Read/write JSON explicitly as UTF-8, record KST timestamps, write `stepN-output.json` with `step`, `name`, `exitCode`, `stdout`, `stderr`, and `command`, and require the agent-produced index status to drive transition handling. A user-approval step that returns `blocked` must stop with process exit code `2`; terminal errors exit `1`; full completion exits `0`.

- [ ] **Step 7: Run executor tests and static harness checks**

Run:

```powershell
python -m pytest .\tcf-harness-exe-pdmp\scripts\test_execute_codex.py -q
python .\tcf-harness-exe-pdmp\scripts\execute_codex.py --help
powershell -NoProfile -ExecutionPolicy Bypass -File .\tcf-harness-exe-pdmp\tests\test_codex_harness.ps1 -Mode Preservation
```

Expected: pytest PASS, help exits `0`, and preservation exits `0`.

- [ ] **Step 8: Commit the independent executor**

```powershell
git add -- tcf-harness-exe-pdmp/scripts/execute_codex.py tcf-harness-exe-pdmp/scripts/test_execute_codex.py tcf-harness-exe-pdmp/tests/test_codex_harness.ps1
git commit -m "feat: add independent Codex phase executor"
```

---

### Task 4: Add the User-Information CRUD Example and Documentation

**Files:**
- Create: `tcf-harness-exe-pdmp/phases/index.json`
- Create: `tcf-harness-exe-pdmp/phases/user-information-crud/index.json`
- Create: `tcf-harness-exe-pdmp/phases/user-information-crud/step0.md`
- Create: `tcf-harness-exe-pdmp/phases/user-information-crud/step1.md`
- Create: `tcf-harness-exe-pdmp/phases/user-information-crud/step2.md`
- Create: `tcf-harness-exe-pdmp/phases/user-information-crud/step3.md`
- Create: `tcf-harness-exe-pdmp/phases/user-information-crud/step4.md`
- Create: `tcf-harness-exe-pdmp/phases/user-information-crud/step5.md`
- Create: `tcf-harness-exe-pdmp/README.md`
- Create: `tcf-harness-exe-pdmp/docs/CODEX_QUICKSTART.md`
- Create: `tcf-harness-exe-pdmp/docs/PDMP_CODEX_WORKFLOW.md`
- Modify: `tcf-harness-exe-pdmp/tests/test_codex_harness.ps1`
- Modify: `tcf-harness-exe-pdmp/scripts/verify_codex_harness.ps1`

**Interfaces:**
- Consumes: executor phase contract and the five Codex skills.
- Produces: runnable phase name `user-information-crud`, six ordered steps, `Examples`/`All` test modes, and exact user commands.

- [ ] **Step 1: Write failing phase and documentation tests**

Assert the phase index has six unique integer steps with `pending` status, every `stepN.md` exists, and the ordered step names are `analysis`, `approval`, `plan`, `implementation`, `security-review`, `qa`. Assert the approval step explicitly instructs Codex to stop without approval. Assert README and quickstart contain:

```powershell
python .\scripts\execute_codex.py user-information-crud --dry-run
python .\scripts\execute_codex.py user-information-crud
python .\scripts\execute_codex.py user-information-crud --target ..\pdmp-service
```

- [ ] **Step 2: Run example tests to verify RED**

Run:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\tcf-harness-exe-pdmp\tests\test_codex_harness.ps1 -Mode Examples
```

Expected: FAIL listing missing phase and documentation files.

- [ ] **Step 3: Implement the six-step example**

Each step must be self-contained and name exact input/output artifacts. Step 0 inspects existing user code and writes `analysis-summary.md`; Step 1 presents API/data/TCF/security/test design and blocks for approval; Step 2 writes `implementation-plan.md`; Step 3 uses TDD and writes `verification-report.md`; Step 4 always reviews personal-data, JWT, SQL binding, logging, and error exposure into `security-review.md`; Step 5 independently checks evidence and writes `qa-report.md` with PASS/FAIL/unverified sections.

- [ ] **Step 4: Add dual-runtime documentation**

README must state that the original Claude files remain available and unchanged, then route Codex users to `CODEX_QUICKSTART.md`. The quickstart must document prerequisites, default target resolution, dry-run, execution, resetting an `error`/`blocked` step to `pending`, and the fact that no commit/push occurs automatically. The workflow document must show artifact handoffs and the approval stop.

- [ ] **Step 5: Extend the verifier and run all harness tests**

The verifier must run preservation, required-file, skill-frontmatter, link, phase-index, safe-command-token, and documentation-command checks. Run:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\tcf-harness-exe-pdmp\tests\test_codex_harness.ps1 -Mode All
powershell -NoProfile -ExecutionPolicy Bypass -File .\tcf-harness-exe-pdmp\scripts\verify_codex_harness.ps1
python -m pytest .\tcf-harness-exe-pdmp\scripts\test_execute_codex.py -q
```

Expected: all three commands exit `0`.

- [ ] **Step 6: Commit the example and docs**

```powershell
git add -- tcf-harness-exe-pdmp/phases tcf-harness-exe-pdmp/README.md tcf-harness-exe-pdmp/docs/CODEX_QUICKSTART.md tcf-harness-exe-pdmp/docs/PDMP_CODEX_WORKFLOW.md tcf-harness-exe-pdmp/tests/test_codex_harness.ps1 tcf-harness-exe-pdmp/scripts/verify_codex_harness.ps1
git commit -m "docs: add executable PDMP Codex workflow"
```

---

### Task 5: Run Final Preservation and PDMP Baseline Verification

**Files:**
- Modify only if a test exposes a defect: new Codex files listed in Tasks 1-4
- Do not modify: any immutable file in `preservation-manifest.json`

**Interfaces:**
- Consumes: the completed Codex harness and sibling `pdmp-service`.
- Produces: fresh verification evidence for handoff; no generated evidence is committed unless explicitly required by a test fixture.

- [ ] **Step 1: Run the complete harness suite from the repository root**

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\tcf-harness-exe-pdmp\tests\test_codex_harness.ps1 -Mode All
powershell -NoProfile -ExecutionPolicy Bypass -File .\tcf-harness-exe-pdmp\scripts\verify_codex_harness.ps1
python -m pytest .\tcf-harness-exe-pdmp\scripts\test_execute_codex.py -q
python .\tcf-harness-exe-pdmp\scripts\execute_codex.py user-information-crud --dry-run
```

Expected: every command exits `0`; dry-run prints six safe `codex exec` commands and changes no phase status.

- [ ] **Step 2: Run the PDMP target baseline**

```powershell
Set-Location .\pdmp-service
.\gradlew.bat test
.\gradlew.bat war
```

Expected: both Gradle commands report `BUILD SUCCESSFUL`. If dependency access or Oracle-only behavior blocks a check, record the exact command, failure, and unverified scope rather than weakening the test.

- [ ] **Step 3: Audit scope and immutable hashes**

```powershell
Set-Location ..
git status --short
git diff --check a27931d..HEAD -- tcf-harness-exe-pdmp
powershell -NoProfile -ExecutionPolicy Bypass -File .\tcf-harness-exe-pdmp\tests\test_codex_harness.ps1 -Mode Preservation
```

Expected: no whitespace errors, preservation PASS, no `pdmp-service` source changes, and no unrelated user files staged.

- [ ] **Step 4: Commit only a necessary final correction**

If and only if the final suite required a correction to a new Codex file:

```powershell
git add -- tcf-harness-exe-pdmp/AGENTS.md tcf-harness-exe-pdmp/README.md tcf-harness-exe-pdmp/preservation-manifest.json tcf-harness-exe-pdmp/docs/CODEX_QUICKSTART.md tcf-harness-exe-pdmp/docs/PDMP_CODEX_WORKFLOW.md tcf-harness-exe-pdmp/phases tcf-harness-exe-pdmp/skills tcf-harness-exe-pdmp/scripts/execute_codex.py tcf-harness-exe-pdmp/scripts/test_execute_codex.py tcf-harness-exe-pdmp/scripts/verify_codex_harness.ps1 tcf-harness-exe-pdmp/tests/test_codex_harness.ps1
git commit -m "fix: complete executable PDMP Codex harness"
```

Otherwise create no empty commit.
