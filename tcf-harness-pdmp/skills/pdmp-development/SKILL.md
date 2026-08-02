---
name: pdmp-development
description: Use when starting an initial PDMP development request for pdmp-service or coordinating it from exploration and design through planning, implementation, security review, and QA.
---

# PDMP Development

## Purpose

Use this skill as the entry point for an initial PDMP development request. The required sequence is exploration and analysis, user design approval, an implementation plan, Builder implementation, Security Reviewer, and QA. Coordinate a bounded `pdmp-service` change without changing the Controller -> Service -> DAO -> MyBatis contract, and do not use this harness to edit another project.

## Read first

- [PDMP project map](references/pdmp-project-map.md) for source, resource, test, security, and H2 locations.
- [Handoff protocol](references/handoff-protocol.md) for deliverables and stop conditions.
- [PDMP architecture](../../docs/pdmp-architecture.md) and [PDMP workflow](../../docs/workflow.md) before proposing a design.
- The applicable role contracts: [PDMP Analyst](../../agents/pdmp-analyst.md), [PDMP Builder](../../agents/pdmp-builder.md), [PDMP Security Reviewer](../../agents/pdmp-security-reviewer.md), and [PDMP QA](../../agents/pdmp-qa.md).

## Required workflow

1. **Exploration and analysis:** follow the [PDMP Analyst](../../agents/pdmp-analyst.md) contract, inspect the current target and closest program, and record facts, open decisions, risks, and acceptance evidence in `analysis-summary.md`.
2. **User design approval:** present the proposed API, data, transaction metadata, security boundary, and test design. Obtain explicit user approval before changing a public API, schema, security rule, program ID, or transaction metadata.
3. **Implementation plan:** turn the approved design into a file-specific, test-first plan with RED, minimal GREEN, security review, QA, and rollback steps.
4. **Builder implementation:** follow the [PDMP Builder](../../agents/pdmp-builder.md) contract, make the smallest approved change, and preserve exact RED and GREEN commands, exit codes, and output in `verification-report.md`.
5. **Security Reviewer:** apply [the handoff protocol security-review rule](references/handoff-protocol.md#rules) and use the [PDMP Security Reviewer](../../agents/pdmp-security-reviewer.md) before QA whenever authentication, authorization, CORS, mapper SQL, input handling, logging, secrets, sessions, tokens, or personal data are affected.
6. **QA:** follow the [PDMP QA](../../agents/pdmp-qa.md) contract only after Builder evidence and any required `security-review.md` are complete. QA independently maps every requirement to fresh evidence and records PASS, FAIL, and unverified scope separately.

Select only the specialist skills the approved plan needs:

- `pdmp-crud` for list, detail, create, update, or delete behavior.
- `pdmp-tcf` for `@TcfTransaction`, standard request/response, or transaction-boundary work.
- `pdmp-security` for JWT, CORS, authorization, SQL binding, logging, or error-exposure review.
- `pdmp-quality` for final evidence and QA.

Do not invoke unrelated copied harness skills or broaden the target scope. A missing decision is a stop condition, not a reason to infer a public contract.

## Completion record

Return changed paths, RED/GREEN command output and exit codes, security-review status, H2 evidence, and Oracle limitations. State unverified items separately from passes.
