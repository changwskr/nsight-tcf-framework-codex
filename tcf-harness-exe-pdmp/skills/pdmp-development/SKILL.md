---
name: pdmp-development
description: Coordinate an approved PDMP change from analysis through handoffs, security review, and QA.
---

# PDMP Development

Use this skill for the workflow owner of a PDMP change. Read the [project map](references/pdmp-project-map.md) and [handoff protocol](references/handoff-protocol.md) first. The only default target is `../pdmp-service`.

1. Analyze the active program and write `analysis-summary.md`.
2. Present the design and obtain explicit user approval.
3. Write `implementation-plan.md` with file-specific RED, GREEN, rollback, security, and QA steps.
4. Implement only the approved plan and write `verification-report.md`.
5. Obtain `security-review.md` when the protocol requires it.
6. Complete independent QA in `qa-report.md`.

Do not pass a stage while its artifact is incomplete or its stop condition remains open. Use `pdmp-crud`, `pdmp-tcf`, `pdmp-security`, and `pdmp-quality` only for their stated specialist boundaries.
