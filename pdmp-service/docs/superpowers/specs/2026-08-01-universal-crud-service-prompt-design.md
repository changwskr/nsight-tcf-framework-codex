# Universal CRUD Service Prompt Design

## Goal

Create `docs/범용crudservice프롬프팅.md` as the shortest practical entry point for requesting a complete CRUD implementation in `pdmp-service`.

## Source and Scope

- Condense `docs/CRUD서비스프롬프트가이드.md`; do not duplicate its full explanations.
- Cover `pdmp-service` only and exclude `pdmp-ui`.
- Link to the full guide and `docs/네이밍원칙.md` for detailed rules.

## Document Structure

1. Three-step usage instructions.
2. One minimal filled example containing program ID, package, API path, table, PK, columns, search/sort, and deletion method.
3. One copy-ready universal CRUD prompt with fillable bracket fields.
4. One short result-review checklist.

## Universal Prompt Requirements

- Ask the AI to inspect the current repository and `mpcoa9999` pattern first.
- Restrict changes to `pdmp-service` and preserve user changes.
- Ask rather than guess when schema, business rules, TCF metadata, errors, timeout, security, or concurrency are missing.
- Require list, detail, create, update, and delete.
- Require `StandardRequestDto<T>`, `StandardResponseDto<T>`, and `@TcfTransaction`.
- Require read-only transactions for reads and rollback-capable transactions for writes.
- Require `BizException`, `exceptionCode.yml`, and ETF-compatible failures.
- Require DAO/MyBatis agreement, safe SQL, Oracle/H2 compatibility, tests, and `gradlew.bat test` evidence.

## Verification

- Keep the final document concise, targeting roughly 100 lines.
- Confirm UTF-8, balanced code fences, a minimal example, one universal prompt, and a checklist.
- Confirm all required framework and verification identifiers appear.
- Do not modify Java or runtime files.
