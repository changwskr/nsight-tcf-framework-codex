# CRUD Service Prompt Guide Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Create a reusable Korean Markdown guide that helps users prompt an AI to implement production-quality CRUD features in `pdmk-service`.

**Architecture:** Organize the guide around a requirements intake form, a staged workflow, and copy-ready prompts. Encode current Pdmk TCF, Spring transaction, MyBatis, error, testing, and verification conventions directly in the prompts so important requirements are not left to AI inference.

**Tech Stack:** Markdown, Java 21, Spring Boot 3.5, Spring MVC, Spring Security, Spring AOP, Spring Transactions, MyBatis, Oracle, H2, Gradle

## Global Constraints

- Work only inside `pdmk-service/docs`.
- Create `docs/CRUD서비스프롬프트가이드.md` in UTF-8 Korean Markdown.
- Cover `pdmk-service` backend only; do not include `pdmk-ui` implementation prompts.
- Align all names with `docs/네이밍원칙.md` and current TCF architecture.
- Prompts must require repository inspection, user-change preservation, tests, and evidence-based verification.
- Do not include real passwords, JWT secrets, private addresses, or production personal data.

---

### Task 1: Author the CRUD service prompting guide

**Files:**
- Create: `docs/CRUD서비스프롬프트가이드.md`

**Interfaces:**
- Consumes: `docs/네이밍원칙.md`, current `mpcoa9999` Controller/Service/DAO/DTO/MyBatis structure, TCF framework components, transaction and error conventions.
- Produces: Copy-ready prompts for complete CRUD and focused CRUD tasks, plus a review checklist.

- [ ] **Step 1: Create the guide structure**

Use these top-level sections:

```text
1. 이 가이드의 사용법
2. 프롬프트 전에 준비할 정보
3. 권장 작업 순서
4. 단계별 프롬프트
5. 전체 CRUD 완성형 프롬프트
6. 작업별 짧은 프롬프트
7. mpcoa9999 형식 작성 예시
8. 나쁜 프롬프트와 개선 예시
9. AI 결과 검수 체크리스트
10. 최종 요청 예시
```

- [ ] **Step 2: Write the requirements intake template**

Include fill-in fields for program ID, package, purpose, API path, table, PK, columns, Java fields/types, nullability, list filters, sort, paging, uniqueness, create/update/delete rules, logical versus physical deletion, TCF metadata, error codes, timeout, permissions, audit fields, Oracle/H2 differences, and acceptance examples. Mark unknown information as a question for the AI to ask rather than guess.

- [ ] **Step 3: Write the staged workflow prompts**

Provide separate copy-ready prompts for:

```text
Repository and convention inspection
Requirements-gap questions
Design and affected-file proposal
Test-first implementation
DTO and validation
DAO and MyBatis XML
Service transactions and business errors
Controller and TCF metadata
Verification and handoff
```

Each prompt must restrict changes to `pdmk-service`, preserve user changes, and stop for approval when business information is missing.

- [ ] **Step 4: Write the complete CRUD master prompt**

The prompt must contain fillable bracket fields and require:

```text
StandardRequestDto<T> / StandardResponseDto<T>
@TcfTransaction metadata for list, detail, create, update, delete
@Transactional(readOnly = true) for reads
rollback-capable @Transactional for writes
explicit timeout input instead of invented values
BizException and exceptionCode.yml
ETF-compatible response handling
DAO method and MyBatis statement ID agreement
Oracle SQL and H2 local compatibility
pagination and maximum page size
not-found, duplicate, required-field, and concurrent-update behavior
unit/integration tests and gradlew.bat test
final affected-file list and verification evidence
```

- [ ] **Step 5: Write six focused prompt patterns**

Provide prompts for list, detail, create, update, delete, and existing-CRUD debugging. Each must state its narrower acceptance criteria and transaction behavior.

- [ ] **Step 6: Add realistic examples and anti-patterns**

Use an `mpcoa9999`-style example with fictional or existing sample identifiers only. Compare weak prompts such as “CRUD 만들어줘” with improved prompts that specify schema, behavior, errors, transaction rules, tests, and scope. Do not request source changes as part of the example.

- [ ] **Step 7: Add the result-review checklist**

Cover architecture boundaries, naming, DTO/JSON mapping, TCF annotations, transaction read/write rules, rollback, timeout, MyBatis binding, SQL safety, pagination, validation, errors, security, logs, Oracle/H2 compatibility, tests, secrets, unrelated changes, and final evidence.

- [ ] **Step 8: Verify the guide**

Check that the file is UTF-8, the 10 top-level sections exist in order, code fences are balanced, no replacement or draft markers occur, and all required identifiers from the design appear. Confirm at least one master prompt and six focused prompt headings exist.

- [ ] **Step 9: Commit only the guide and implementation plan**

```powershell
git add -- 'docs/CRUD서비스프롬프트가이드.md' 'docs/superpowers/plans/2026-08-01-crud-service-prompt-guide.md'
git commit -m "docs: add CRUD service prompt guide"
```
