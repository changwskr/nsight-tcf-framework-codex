# TCF Harness PDMP Instructions

## Scope

This directory is an independent Codex harness for `pdmp-service`. Its only default development target is `pdmp-service`; do not change another project unless the user explicitly expands the scope. Preserve existing user changes.

## Architecture contract

PDMP business code follows `Controller -> Service -> DAO -> MyBatis`. Controllers adapt HTTP and declare `@TcfTransaction`; Services own business rules and Spring transaction boundaries; DAOs declare persistence operations; and MyBatis XML owns parameter-bound SQL. Do not move business decisions into a Controller or DAO.

Use a program identifier such as `mpcoa8888`, the package family `nhnis.mp.co.a`, and `MP.{Domain}.{action}` service IDs. Each controller method must declare `serviceId`, `transactionCode`, `processingType`, and `serviceName` in `@TcfTransaction`.

## Data and security constraints

Use MyBatis parameter binding for every SQL value. H2 is the local verification database; Oracle is the operational database. A successful H2 run does not prove Oracle compatibility, so record unverified Oracle behavior explicitly.

Keep `/api/mp/co/a/8888/**` authenticated through the existing JWT security chain. Do not widen `permitAll`, add a role bypass, log credentials/tokens or personal data, expose stack traces, or interpolate untrusted input into SQL.

## Required workflow

Read the approved design and current `pdmp-service` implementation first. Add the smallest relevant test, run it to demonstrate RED, implement the minimal change, and rerun it for GREEN. Route a security-sensitive change through the PDMP Security Reviewer before QA sign-off.

## Evidence and reporting

Record exact commands, exit codes, and their relevant output. Distinguish executed checks from unverified work. Run `pdmp-service\gradlew.bat test` and `pdmp-service\gradlew.bat war` when the change scope and environment permit; otherwise report why they could not run.