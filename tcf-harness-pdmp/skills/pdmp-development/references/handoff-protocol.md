# PDMP Handoff Protocol

| Stage | Role contract | Required artifact | May proceed when |
| --- | --- | --- | --- |
| Analyze | `agents/pdmp-analyst.md` | `analysis-summary.md` | Program ID, API, metadata, delete semantics, database facts, and risks are decided or explicitly open. |
| Build | `agents/pdmp-builder.md` | Source/tests and `verification-report.md` | A focused test showed RED, the smallest change showed GREEN, and changed files are listed. |
| Review | `agents/pdmp-security-reviewer.md` | `security-review.md` | JWT/CORS/authentication, SQL binding, logs, secrets, and error exposure are approved or have specific remediation. |
| QA | `agents/pdmp-qa.md` | `qa-report.md` | Requirements map to fresh evidence; H2 and Oracle claims remain distinct. |

## Rules

- The Builder receives the approved analysis and does not invent missing program IDs, transaction metadata, or delete behavior.
- Route a change through security review before QA whenever authentication, authorization, CORS, mapper SQL, input handling, logging, secrets, sessions, tokens, or personal data are affected.
- QA records command, exit code, output summary, defects, and unverified scope. `verification-report.md` is not QA approval.
- Stop for an explicit decision if a request changes an authenticated boundary, schema contract, public API, composite key, or physical-versus-logical delete policy.
