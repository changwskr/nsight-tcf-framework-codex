# Universal CRUD Service Prompt Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Create a concise, copy-ready universal CRUD prompt for `pdmk-service` with one minimal filled example.

**Architecture:** Reduce the comprehensive CRUD guide to four sections: three-step usage, a minimal example, one fill-in prompt, and a short review checklist. Link to the full guide and naming principles instead of repeating detailed explanations.

**Tech Stack:** Markdown, Java 21, Spring Boot 3.5, Spring Transactions, MyBatis, Oracle, H2, Gradle

## Global Constraints

- Work only inside `pdmk-service/docs`.
- Create the exact filename `docs/범용crudservice프롬프팅.md` in UTF-8.
- Keep the document near 100 lines and optimized for copying.
- Cover `pdmk-service` only; exclude `pdmk-ui`.
- Do not modify Java or runtime files.

---

### Task 1: Create the universal CRUD prompt

**Files:**
- Create: `docs/범용crudservice프롬프팅.md`

**Interfaces:**
- Consumes: `docs/CRUD서비스프롬프트가이드.md` and `docs/네이밍원칙.md`.
- Produces: One minimal-input prompt that can start a full CRUD design and implementation workflow.

- [ ] **Step 1: Write three-step usage instructions**

Explain: copy the input block, replace brackets, paste the completed input and universal prompt to the AI. Link to the full guide for advanced options.

- [ ] **Step 2: Add one minimal filled example**

Use an `mpcoa9999`-style example containing program ID, package, API, table, PK, column definitions, search/sort, paging, deletion method, and explicitly marked unknown write metadata. Do not invent create/update/delete TCF codes or error codes.

- [ ] **Step 3: Add one universal prompt**

Use fillable fields for:

```text
program ID, business name, package, API path
table, PK, columns
search, sort, paging, deletion method
TCF metadata, errors, timeout, permissions
```

Require repository inspection; missing-information questions; design approval; test-first implementation; Controller/Service/DAO/DTO/MyBatis boundaries; standard request/response; `@TcfTransaction`; read-only and write transactions; rollback; `BizException`; `exceptionCode.yml`; ETF; safe Oracle/H2 SQL; and `gradlew.bat test` evidence.

- [ ] **Step 4: Add a short review checklist**

Include scope, naming, TCF, transactions, MyBatis binding, safe update/delete, errors, tests, secrets, and verification evidence in no more than 12 checklist items.

- [ ] **Step 5: Verify the document**

Confirm UTF-8, four top-level sections, balanced code fences, one minimal example, one universal prompt, required identifiers, no draft markers, and concise length between 80 and 140 lines.

- [ ] **Step 6: Commit only the document and plan**

```powershell
git add -- 'docs/범용crudservice프롬프팅.md' 'docs/superpowers/plans/2026-08-01-universal-crud-service-prompt.md'
git commit -m "docs: add universal CRUD service prompt"
```
