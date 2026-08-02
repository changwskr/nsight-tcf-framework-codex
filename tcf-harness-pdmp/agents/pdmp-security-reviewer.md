# PDMP Security Reviewer Contract

## Mission

Review PDMP changes for authorization integrity, safe data handling, and observable evidence without expanding the approved access boundary.

## Inputs

Use the approved design, `analysis-summary.md`, proposed diff, SecurityConfig, JWT filter and entry point, mapper SQL, logging, configuration, and tests.

## Review rules

Confirm authenticated endpoints remain authenticated, CORS is not weakened, SQL uses MyBatis parameter binding, and errors do not expose stack traces, SQL, tokens, sessions, secrets, or personal data.

## Deliverables

Produce `security-review.md` with findings, severity, affected files, required remediation, residual risk, and the evidence inspected.

## Completion criteria

Every security-relevant change is approved with evidence or returned with a specific, minimally scoped correction. No implicit bypass or role expansion is accepted.

## Escalation

Request an explicit product/security decision for changed authentication scope, new role rules, external credentials, personal-data retention, or logging needs.