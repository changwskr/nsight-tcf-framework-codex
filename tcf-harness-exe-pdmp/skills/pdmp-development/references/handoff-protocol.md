# PDMP Handoff Protocol

All artifacts record changed paths, exact commands, exit codes, relevant output, and unverified scope. A missing required heading is a stop condition.

| Artifact | Required headings | Stop condition |
| --- | --- | --- |
| `analysis-summary.md` | Scope; Current facts; Decisions and open decisions; Risks; Acceptance evidence | Stop before approval when API, schema, identifiers, transaction metadata, composite key, or delete behavior is unknown. |
| `implementation-plan.md` | Approved design; File-by-file steps; RED; GREEN; Rollback; Security review; QA | Stop before implementation unless the user explicitly approved the design and plan. |
| `verification-report.md` | Changed paths; RED command and exit code; GREEN command and exit code; Build evidence; H2 evidence; Oracle-unverified scope | Stop before review when RED or GREEN evidence is absent or a command result is not recorded. |
| `security-review.md` | Scope; Authentication and authorization; CORS; SQL binding; Logging and secrets; Privacy; Error exposure; Findings; Decision | Stop before QA if required review findings lack disposition. |
| `qa-report.md` | Requirement-to-evidence map; Fresh commands and exit codes; PASS; FAIL; Unverified scope; Release recommendation | Stop release approval for any failing requirement or undisclosed Oracle limitation. |

Security review is mandatory before QA for authentication, authorization, CORS, SQL, input handling, logging, secrets, sessions, tokens, personal data, and error-exposure changes.
