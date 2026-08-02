# PDMP Delivery Workflow

## 1. Analyze the approved design

The analyst reads the approved design and current `pdmp-service` structure, including the closest program, DTOs, Controller, Service, DAO, mapper, tests, security rules, H2 resources, and Oracle limitations. The analysis names the program ID, API contract, database object, transaction metadata, risks, and acceptance evidence before implementation begins.

## 2. Confirm a safe change boundary

Do not modify `pdmp-service` until requirements that affect public API, schema, security, program ID, or `@TcfTransaction` metadata have an approved decision. Keep unrelated user changes intact. If required source information is missing, report the missing decision rather than guessing.

## 3. Build with RED then GREEN

The builder writes the smallest relevant test first and runs it to prove RED. Implement the minimal production change, then rerun the same test for GREEN. Add only tests that exercise the observable PDMP boundary; preserve evidence of the command, exit code, and relevant output.

## 4. Review security before sign-off

Use `pdmp-security-reviewer.md` for JWT, Spring Security, CORS, SQL binding, secrets, token/session data, personal data, logs, and error-response exposure. The reviewer confirms that the existing authenticated boundary is not widened and returns specific findings or an evidence-backed approval.

## 5. Verify the target project

The QA role follows `pdmp-qa.md` to trace requirements through Controller, Service, DAO, mapper, metadata, configuration, and tests. Run focused tests first, then use `pdmp-service\gradlew.bat test` and `pdmp-service\gradlew.bat war` when available. Treat local H2 success and Oracle verification as separate evidence.

## 6. Report completion honestly

Report changed files, command outputs, unexecuted checks, compatibility risks, and rollback considerations. A failed or unavailable command is not a pass; state its cause and the unverified scope.