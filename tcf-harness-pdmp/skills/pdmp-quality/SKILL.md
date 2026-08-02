---
name: pdmp-quality
description: Use when preparing QA evidence for an approved pdmp-service change after focused PDMP tests and any required security review.
---

# PDMP Quality

## Evidence order

1. Trace each requirement through Controller, Service, DAO, MyBatis mapper, `@TcfTransaction`, security, and tests.
2. Run the smallest relevant test first and retain the command, exit code, and meaningful output.
3. Run target-wide checks when the environment permits:

```powershell
cd pdmp-service
.\gradlew.bat test
.\gradlew.bat war
```

4. Produce `qa-report.md` with requirement-to-evidence mapping, security review status, defects, and unverified scope.

## Database evidence

H2 verifies local behavior and automated-test paths. Oracle is operational; H2 success does not prove Oracle compatibility. Record unverified Oracle syntax, hints, optimizer behavior, driver differences, collation, and date behavior unless an approved Oracle environment supplied evidence.

## PASS rule

PASS requires fresh focused evidence, required `gradlew.bat test` and `gradlew.bat war` results when runnable, and an approved security review for security-sensitive changes. Report unavailable commands and failed checks as unverified or failed, never as passes.
