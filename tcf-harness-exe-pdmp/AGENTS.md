# PDMP Codex Harness

## Scope and preservation

Resolve the sole default target as `../pdmp-service` from this harness. Work only in that resolved PDMP root unless the user explicitly expands scope. Inspect `git status --short` first and preserve every user change; do not reset, overwrite, or include unrelated files in a commit.

## Architecture and TCF contract

Keep the program flow `Controller -> Service -> DAO -> MyBatis`. Controllers adapt HTTP and declare `@TcfTransaction`; Services own business orchestration and transaction boundaries; DAOs declare persistence operations; MyBatis owns SQL with bound parameters.

Before changing a transaction, verify its `serviceId`, `transactionCode`, `processingType`, request header, and MDC/trace behavior. Use the approved `MP.{Domain}.{action}` naming pattern and do not infer identifiers, public API, schema, or delete semantics.

## Required workflow

Follow this order exactly: `analysis -> user approval -> implementation plan -> implementation -> security review -> QA`. Obtain explicit user approval before implementation, including before a public API, schema, security-boundary, transaction-metadata, or delete-policy change.

Use TDD: write the narrowest test, run it and retain the RED result, implement the minimum approved change, then run the same test for GREEN. Record every executed command, its exit code, and the relevant output in the handoff artifact.

## Security and evidence

Require a security review before QA for changes involving authentication, authorization, CORS, SQL, logging, secrets, tokens, sessions, personal data, or error exposure. Keep JWT-protected paths protected, bind all SQL values through MyBatis, and do not log credentials, tokens, or personal data.

Run focused tests first; then run `gradlew.bat test` and `gradlew.bat war` when scope and environment permit. Record H2 evidence separately from Oracle behavior, which is unverified without an approved Oracle environment.
