# CRUD Service Prompt Guide Design

## Goal

Create `docs/CRUD서비스프롬프트가이드.md` as a reusable Korean guide for prompting an AI to design, implement, test, and verify CRUD features in `pdmp-service`.

## Audience and Scope

- Audience: Developers and analysts who know the business data but may not know every Spring/MyBatis implementation detail.
- Scope: `pdmp-service` backend only; `pdmp-ui` is excluded.
- The guide must align with `docs/네이밍원칙.md` and the current TCF architecture.

## Document Structure

1. Explain what information must be supplied before requesting CRUD work.
2. Provide a recommended staged prompting workflow from repository inspection through verification.
3. Provide one fill-in-the-blanks master prompt for complete CRUD delivery.
4. Provide smaller prompts for list, detail, create, update, delete, and debugging work.
5. Show weak prompts and improved versions.
6. Provide an AI-output review checklist.
7. Provide a realistic `mpcoa9999`-style example without modifying production code.

## Required Project Conventions

- Packages remain under `nhnis.mp.<business-area>`; common framework code stays under `nhnis.fw`.
- Requests and responses use `StandardRequestDto<T>` and `StandardResponseDto<T>`.
- Controller methods use `@TcfTransaction` with explicit `serviceId`, `transactionCode`, `processingType`, and `serviceName`.
- Controllers adapt HTTP, services own business rules and transactions, DAOs/MyBatis own persistence, and DTOs carry data.
- Read operations use read-only Spring transactions; writes use rollback-capable transactions and state an explicit timeout requirement when needed.
- Business validation uses `BizException`; error messages are defined in `exceptionCode.yml`; response formatting remains centralized in `ETF`.
- MyBatis DAO method, mapper statement ID, namespace, parameter/result types, and SQL aliases must agree.
- Oracle production SQL and H2 local compatibility must be considered explicitly.
- Tests and `gradlew.bat test` verification are part of the prompt, not optional follow-up work.

## Prompt Input Model

The reusable prompt collects:

- Program ID, package, business purpose, API base path
- Table, primary key, columns, Java fields, types, nullability, validation
- List filters, sorting, paging, and maximum page size
- Create/update/delete rules, uniqueness, concurrency, and delete strategy
- TCF transaction metadata and error codes
- Transaction timeout, authorization, audit, and logging requirements
- Required tests and acceptance examples

## Safety and Quality

- The prompt tells the AI to inspect current source before proposing changes.
- It requires an explicit affected-file list and design approval before implementation.
- It prohibits unrelated refactoring, secret exposure, guessing unknown schemas, and changes outside `pdmp-service`.
- It requires preserving existing user changes and verifying results with current commands.

## Verification

- Confirm the final guide is UTF-8 Markdown with balanced code fences.
- Confirm it contains staged prompts, a master prompt, six focused prompt patterns, examples, and a checklist.
- Confirm required identifiers such as `@TcfTransaction`, `StandardRequestDto`, `StandardResponseDto`, `BizException`, `ETF`, `@Transactional`, MyBatis, Oracle, H2, and `gradlew.bat test` appear.
- No Java or runtime files are modified.
