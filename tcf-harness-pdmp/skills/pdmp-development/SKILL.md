---
name: pdmp-development
description: Use when coordinating an approved pdmp-service change that needs PDMP analysis, implementation, security review, and QA handoffs.
---

# PDMP Development

## Purpose

Coordinate a bounded `pdmp-service` change without changing the Controller -> Service -> DAO -> MyBatis contract. Start with the approved design and the current target sources; do not use this harness to edit another project.

## Read first

- [PDMP project map](references/pdmp-project-map.md) for source, resource, test, security, and H2 locations.
- [Handoff protocol](references/handoff-protocol.md) for deliverables and stop conditions.
- `docs/pdmp-architecture.md`, `docs/workflow.md`, and the applicable role contract before delegating or implementing work.

## Required handoffs

1. Read `agents/pdmp-analyst.md`; require an approved `analysis-summary.md` before public API, schema, security, program-ID, or metadata changes.
2. Read `agents/pdmp-builder.md`; keep the smallest approved implementation and preserve RED then GREEN evidence.
3. Select only the specialist skills the change needs:
   - Use `agents/pdmp-security-reviewer.md` whenever the security-sensitive change criteria below apply.
   - `pdmp-crud` for list, detail, create, update, or delete behavior.
   - `pdmp-tcf` for `@TcfTransaction`, standard request/response, or transaction-boundary work.
   - `pdmp-security` for JWT, CORS, authorization, SQL binding, logging, or error-exposure review.
   - `pdmp-quality` for final evidence and QA.
4. When a security-sensitive surface changed, complete `pdmp-security` before reading `agents/pdmp-qa.md`. Then hand the evidence to QA.

Do not invoke unrelated copied harness skills or broaden the target scope. A missing decision is a stop condition, not a reason to infer a public contract.

## Completion record

Return changed paths, RED/GREEN command output and exit codes, security-review status, H2 evidence, and Oracle limitations. State unverified items separately from passes.
