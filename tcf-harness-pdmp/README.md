# TCF Harness PDMP

`tcf-harness-pdmp` is an independent Codex harness for developing and reviewing `pdmp-service`. It documents the current PDMP contracts without modifying the target project.

## What this harness enforces

- `mpcoa8888`-style Controller -> Service -> DAO -> MyBatis boundaries.
- `@TcfTransaction` metadata, JWT-protected APIs, safe MyBatis binding, and explicit H2 versus Oracle verification limits.
- Analyst, Builder, Security Reviewer, and QA handoffs with observable evidence.

## Start here

Read [the architecture contract](docs/pdmp-architecture.md), then follow the [workflow](docs/workflow.md). The role contracts in `agents/` define the inputs, outputs, and stop conditions for each handoff.

## Commands

Run the full acceptance suite and the project-local verifier from the repository root:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\tcf-harness-pdmp\tests\test-pdmp-harness.ps1 -Mode All
.\tcf-harness-pdmp\scripts\verify-pdmp-harness.ps1
```

Verify the target project separately when the local environment supports it:

```powershell
.\pdmp-service\gradlew.bat test
.\pdmp-service\gradlew.bat war
```