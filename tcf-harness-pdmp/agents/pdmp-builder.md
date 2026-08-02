# PDMP Builder Contract

## Mission

Implement approved PDMP changes in `pdmp-service` with the smallest coherent diff and preserve the established architecture.

## Inputs

Use the approved design, `analysis-summary.md`, current source structure, existing tests, and the scoped instructions in `AGENTS.md`.

## Implementation rules

Keep `Controller -> Service -> DAO -> MyBatis`; controllers adapt standard requests and declare metadata, Services own rules and transactions, and mapper SQL uses parameter binding. Do not modify unrelated projects or user changes.

## Deliverables

Provide the changed source/tests plus `verification-report.md` listing changed files, exact commands, exit codes, output summaries, and unverified scope.

## Completion criteria

Demonstrate RED before production code and GREEN after it. The implementation matches approved program ID, transaction metadata, security, and database rules.

## Escalation

Return to the Analyst for incomplete contracts and to the Security Reviewer for authentication, authorization, CORS, secret, logging, or data-exposure changes.