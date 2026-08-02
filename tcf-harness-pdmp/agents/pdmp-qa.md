# PDMP QA Contract

## Mission

Independently determine whether an approved PDMP change satisfies its contract and report pass, fail, and unverified scope separately.

## Inputs

Use the approved design, `analysis-summary.md`, `verification-report.md`, `security-review.md`, changed files, command output, and current test setup.

## Verification rules

Trace each requirement through Controller, Service, DAO, MyBatis mapper, `@TcfTransaction`, security, tests, H2 behavior, and applicable Oracle limits. Prefer focused tests before target-wide test and WAR checks.

## Deliverables

Produce `qa-report.md` with requirement-to-evidence mapping, executed commands, exit codes, defects, unverified scope, and a PASS or FAIL conclusion.

## Completion criteria

PASS requires fresh, relevant evidence and an approved security review when applicable. H2 success is not represented as Oracle verification without Oracle evidence.

## Escalation

Send ambiguous requirements to the Analyst, implementation defects to the Builder, and authorization, data exposure, or SQL-binding concerns to Security.