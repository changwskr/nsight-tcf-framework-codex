---
name: pdmp-security
description: Use when reviewing pdmp-service changes that affect JWT authentication, authorization, CORS, MyBatis SQL, logging, secrets, or error exposure.
---

# PDMP Security Review

## Review target

Review the approved analysis, changed files, `SecurityConfig`, JWT filter and authentication entry point, CORS configuration, mapper XML, log configuration, and relevant tests. Produce the `security-review.md` required by the PDMP Security Reviewer contract.

## Required findings

- `/api/mp/co/a/8888/**` remains authenticated through the existing JWT security chain. Do not widen `permitAll` or add an unapproved role bypass.
- CORS rules and authentication-failure behavior are not weakened.
- SQL binding is MyBatis parameter binding for every value; no untrusted value is interpolated into SQL.
- logs do not contain credentials, JWT tokens, sessions, secrets, or personal data.
- Error responses do not expose stack traces, SQL, or internal exception details.

## Decision rule

Return specific affected files, severity, required remediation, residual risk, and inspected evidence. Stop for an explicit product/security decision if authentication scope, role rules, external credentials, or personal-data retention changes.
