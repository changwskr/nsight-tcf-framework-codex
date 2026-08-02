# PDMP Harness Quickstart

## 1. Establish the target

Work only on `pdmp-service` unless the user explicitly changes the scope. Inspect the approved design, current controller, service, DAO, MyBatis mapper, security configuration, tests, and the local H2 configuration before deciding what changes are required.

## 2. Use the contract flow

For a PDMP program, preserve `Controller -> Service -> DAO -> MyBatis`. Controllers convert standard HTTP requests and declare `@TcfTransaction`. Services own validation, business errors, and transaction boundaries. DAOs and MyBatis own only persistence contracts and parameter-bound SQL.

## 3. Run the required handoffs

Start with `agents/pdmp-analyst.md`, then implement through `agents/pdmp-builder.md`. Send authentication, authorization, SQL binding, logging, or personal-data changes through `agents/pdmp-security-reviewer.md`. Finish with `agents/pdmp-qa.md` and retain command output as evidence.

## 4. Verify locally

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\tcf-harness-pdmp\tests\test-pdmp-harness.ps1 -Mode All
.\tcf-harness-pdmp\scripts\verify-pdmp-harness.ps1
.\pdmp-service\gradlew.bat test
.\pdmp-service\gradlew.bat war
```

H2 validates local behavior. Oracle is operational-only unless an approved Oracle environment is available; report that distinction rather than treating an H2 result as Oracle evidence.