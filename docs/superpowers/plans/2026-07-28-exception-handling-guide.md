# NSIGHT TCF Exception Handling Guide Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Create an implementation-grounded Korean exception-handling guide at `ztcf-다이어리/2026-07-26-아키텍처-이것저것/2026-07-26-07-예외처리방식.md`.

**Architecture:** Organize the guide around the exception lifecycle from throw site through TCF/ETF or MVC advice to response, logs, and rollback evidence. Distinguish current implementation, development standard, and improvement recommendations; link every implementation claim to repository sources and finish with actionable checklists and `EXC` test scenarios.

**Tech Stack:** Markdown, Java 21, Spring Boot 3.3.5, Gradle, Spring MVC, NSIGHT TCF core/web/EAI modules

## Global Constraints

- Use `zdocs-1/architecture/05-exception.md` as the baseline, but treat actual Java code and tests as the final source of truth.
- Create only `ztcf-다이어리/2026-07-26-아키텍처-이것저것/2026-07-26-07-예외처리방식.md`; do not modify framework code.
- Preserve UTF-8 and all existing uncommitted user changes.
- Mark policy statements as **현재 구현**, **개발 표준**, or **개선 권고** where the distinction matters.
- Do not expose passwords, tokens, private keys, session IDs, personal data, SQL, stack traces, internal paths, or internal URLs in public-response examples.
- Use repository-relative Markdown links and verify every local link.
- Reuse the transaction and messaging guides through links instead of duplicating their detailed contracts.

## File Structure

- Create: `ztcf-다이어리/2026-07-26-아키텍처-이것저것/2026-07-26-07-예외처리방식.md`
  - Single authoritative guide containing the approved 21 top-level sections, implementation evidence, checklists, test matrix, and source links.
- Read only: `zdocs-1/architecture/05-exception.md`
  - Baseline exception policy and source inventory.
- Read only: `ztcf-다이어리/2026-07-26-아키텍처-이것저것/2026-07-26-05-트랜잭션처리방식.md`
  - Rollback and transaction-boundary cross-reference.
- Read only: `ztcf-다이어리/2026-07-26-아키텍처-이것저것/2026-07-26-06-messaging.md`
  - Message, HTTP-status, Relay, hybrid REST, and EAI cross-reference.

---

### Task 1: Establish the Exception Evidence Matrix

**Files:**
- Create: `ztcf-다이어리/2026-07-26-아키텍처-이것저것/2026-07-26-07-예외처리방식.md`
- Read: `zdocs-1/architecture/05-exception.md`
- Read: `tcf-core/src/main/java/com/nh/nsight/tcf/core/support/error/BusinessException.java`
- Read: `tcf-core/src/main/java/com/nh/nsight/tcf/core/support/error/SystemException.java`
- Read: `tcf-core/src/main/java/com/nh/nsight/tcf/core/support/error/ErrorCode.java`
- Read: `tcf-core/src/main/java/com/nh/nsight/tcf/core/support/message/Result.java`
- Read: `tcf-core/src/main/java/com/nh/nsight/tcf/core/support/processor/TCF.java`
- Read: `tcf-core/src/main/java/com/nh/nsight/tcf/core/support/processor/ETF.java`
- Read: `tcf-web/src/main/java/com/nh/nsight/tcf/web/entry/web/GlobalStandardExceptionHandler.java`
- Read: `tcf-eai/src/main/java/com/nh/nsight/tcf/eai/client/DefaultTcfServiceClient.java`

**Interfaces:**
- Consumes: approved design `docs/superpowers/specs/2026-07-28-exception-handling-guide-design.md` and repository implementation.
- Produces: sections 1-5 and a verified fact inventory used by all later sections.

- [ ] **Step 1: Confirm the working tree and target are safe**

Run:

```powershell
git status --short
Test-Path -LiteralPath 'ztcf-다이어리/2026-07-26-아키텍처-이것저것/2026-07-26-07-예외처리방식.md'
```

Expected: existing user changes are recorded; target returns `False` or is identified before any edit.

- [ ] **Step 2: Extract exact exception and response behavior**

Run:

```powershell
rg -n 'class |catch \(|throw new|businessFail|systemError|Result\.fail|@ExceptionHandler|resultCode|errorCode' tcf-core/src/main/java/com/nh/nsight/tcf/core/support/error tcf-core/src/main/java/com/nh/nsight/tcf/core/support/processor tcf-core/src/main/java/com/nh/nsight/tcf/core/support/message tcf-web/src/main/java/com/nh/nsight/tcf/web/entry/web tcf-eai/src/main/java/com/nh/nsight/tcf/eai
```

Expected: evidence for business/system/MVC/timeout/integration classification and response conversion is visible.

- [ ] **Step 3: Write sections 1-5**

Create the target with these exact top-level headings:

```markdown
# NSIGHT TCF 예외 처리 방식

## 1. 목적과 적용 범위
## 2. 핵심 원칙과 용어
## 3. 예외 처리 전체 생명주기
## 4. 예외 분류 체계
## 5. 핵심 예외 클래스의 역할
```

Include a lifecycle diagram, the five-category classification table, and explicit current behavior for `BusinessException`, `SystemException`, general `Exception`, MVC errors, timeout errors, and integration errors.

- [ ] **Step 4: Verify the first deliverable**

Run:

```powershell
rg -n '^#|^## [1-5]\.' -- 'ztcf-다이어리/2026-07-26-아키텍처-이것저것/2026-07-26-07-예외처리방식.md'
rg -n 'BusinessException|SystemException|GlobalStandardExceptionHandler|IntegrationException|현재 구현|개발 표준' -- 'ztcf-다이어리/2026-07-26-아키텍처-이것저것/2026-07-26-07-예외처리방식.md'
git diff --check -- 'ztcf-다이어리/2026-07-26-아키텍처-이것저것/2026-07-26-07-예외처리방식.md'
```

Expected: headings 1-5 exist; all key classifications are present; no whitespace errors.

### Task 2: Document Layer Responsibilities and Conversion Paths

**Files:**
- Modify: `ztcf-다이어리/2026-07-26-아키텍처-이것저것/2026-07-26-07-예외처리방식.md`
- Read: `tcf-core/src/main/java/com/nh/nsight/tcf/core/support/validation/StandardHeaderValidator.java`
- Read: `tcf-core/src/main/java/com/nh/nsight/tcf/core/support/dispatch/TransactionDispatcher.java`
- Read: `tcf-core/src/main/java/com/nh/nsight/tcf/core/support/processor/STF.java`
- Read: `tcf-core/src/main/java/com/nh/nsight/tcf/core/support/processor/TCF.java`
- Read: `tcf-core/src/main/java/com/nh/nsight/tcf/core/support/processor/ETF.java`
- Read: `tcf-web/src/main/java/com/nh/nsight/tcf/web/entry/web/OnlineTransactionController.java`
- Read: `tcf-web/src/main/java/com/nh/nsight/tcf/web/entry/web/GlobalStandardExceptionHandler.java`
- Read: `tcf-core/src/main/java/com/nh/nsight/tcf/core/support/timeout/OnlineTransactionTimeoutExecutor.java`
- Read: `tcf-core/src/main/java/com/nh/nsight/tcf/core/support/timeout/TimeoutExceptionResolver.java`
- Read: `tcf-eai/src/main/java/com/nh/nsight/tcf/eai/support/ResponseResultValidator.java`

**Interfaces:**
- Consumes: classification and terminology from Task 1.
- Produces: sections 6-13 defining throw ownership, catch boundaries, conversions, response contract, and HTTP policy.

- [ ] **Step 1: Verify layer and conversion evidence**

Run:

```powershell
rg -n 'throw new BusinessException|catch \(|businessFail|systemError|toBusinessException|@ExceptionHandler|ResponseEntity|Result\.fail' tcf-core/src/main/java tcf-web/src/main/java tcf-eai/src/main/java
```

Expected: exact throw, catch, timeout resolution, ETF, MVC advice, and EAI conversion sites are listed.

- [ ] **Step 2: Write sections 6-9**

Append these exact headings:

```markdown
## 6. 계층별 예외 발생·처리 책임
## 7. STF·Dispatcher·Handler 예외 흐름
## 8. TCF Catch 및 ETF 응답 조립
## 9. Web MVC·Global Exception Handler
```

Include Handler→Facade→Service→Rule→DAO/Mapper responsibilities, the TCF catch ordering, ETF side effects, and the differences between ETF and MVC advice paths.

- [ ] **Step 3: Write sections 10-13**

Append these exact headings:

```markdown
## 10. 검증·Timeout·트랜잭션 예외
## 11. tcf-eai 연동 예외
## 12. 오류 코드와 표준 응답 계약
## 13. HTTP 상태 정책
```

Document timeout conversion, rollback cross-reference, EAI business/system/message/timeout distinctions, `S0000` versus `E0001`, error-code roles, and path-specific HTTP status behavior.

- [ ] **Step 4: Verify section coverage and response safety**

Run:

```powershell
rg -n '^## ([6-9]|1[0-3])\.' -- 'ztcf-다이어리/2026-07-26-아키텍처-이것저것/2026-07-26-07-예외처리방식.md'
rg -n 'Handler|Facade|Service|Rule|DAO|ETF\.businessFail|ETF\.systemError|E0001|S0000|HTTP|Rollback' -- 'ztcf-다이어리/2026-07-26-아키텍처-이것저것/2026-07-26-07-예외처리방식.md'
git diff --check -- 'ztcf-다이어리/2026-07-26-아키텍처-이것저것/2026-07-26-07-예외처리방식.md'
```

Expected: headings 6-13 and all named responsibilities/contracts are present; no whitespace errors.

### Task 3: Add Operations, Security, Drift, and Developer Checklists

**Files:**
- Modify: `ztcf-다이어리/2026-07-26-아키텍처-이것저것/2026-07-26-07-예외처리방식.md`
- Read: `tcf-core/src/main/java/com/nh/nsight/tcf/core/support/logging/AuditLogService.java`
- Read: `tcf-web/src/main/java/com/nh/nsight/tcf/web/entry/web/GuidMdcCleanupFilter.java`
- Read: `tcf-core/src/main/java/com/nh/nsight/tcf/core/support/processor/ETF.java`
- Read: `ztcf-다이어리/2026-07-26-아키텍처-이것저것/2026-07-26-05-트랜잭션처리방식.md`
- Read: `ztcf-다이어리/2026-07-26-아키텍처-이것저것/2026-07-26-06-messaging.md`

**Interfaces:**
- Consumes: conversion paths and contracts from Tasks 1-2.
- Produces: sections 14-18 and an eight-row implementation-alignment matrix.

- [ ] **Step 1: Inspect logging, MDC, transaction, and non-standard path evidence**

Run:

```powershell
rg -n 'guid|traceId|serviceId|errorCode|audit|MDC|finally|remove\(|clear\(|multipart|Relay|Timeout' tcf-core/src/main/java tcf-web/src/main/java tcf-ui/src/main/java ztcf-다이어리/2026-07-26-아키텍처-이것저것/2026-07-26-05-트랜잭션처리방식.md ztcf-다이어리/2026-07-26-아키텍처-이것저것/2026-07-26-06-messaging.md
```

Expected: logging correlation, cleanup, rollback, Relay, hybrid REST, and timeout evidence is visible.

- [ ] **Step 2: Write sections 14-16**

Append:

```markdown
## 14. 로깅·감사·추적성
## 15. 보안과 내부 정보 비노출
## 16. 현재 구현 정합성 및 개선 권고
```

The section 16 matrix must cover exactly these topics: `SystemException` usage, ETF versus MVC advice, system-message leakage, pre-TCF correlation IDs, timeout-to-business conversion, hybrid REST drift, EAI code preservation, and HTTP/resultCode differences.

- [ ] **Step 3: Write sections 17-18**

Append:

```markdown
## 17. 신규 예외 체크리스트
## 18. 예외 변경 체크리스트
```

Use unchecked Markdown checklist items. Require ownership, code naming, public message, rollback, logging, sensitive-data, client contract, compatibility, deployment order, and rollback-plan checks.

- [ ] **Step 4: Scan for unsafe examples and incomplete guidance**

Run:

```powershell
rg -n -i 'password|passwd|secret|private[ _-]?key|access[ _-]?token|refresh[ _-]?token|session[ _-]?id|stack trace|select .* from|jdbc:' -- 'ztcf-다이어리/2026-07-26-아키텍처-이것저것/2026-07-26-07-예외처리방식.md'
rg -n '^- \[ \]' -- 'ztcf-다이어리/2026-07-26-아키텍처-이것저것/2026-07-26-07-예외처리방식.md'
```

Expected: sensitive terms appear only in prohibition/masking guidance and no sample contains a real value; both checklist sections contain actionable items.

### Task 4: Complete Test Matrix, Evidence Links, and Final Verification

**Files:**
- Modify: `ztcf-다이어리/2026-07-26-아키텍처-이것저것/2026-07-26-07-예외처리방식.md`
- Test: `tcf-core/src/test/java/com/nh/nsight/tcf/core/support/`
- Test: `tcf-web/src/test/java/`
- Test: `tcf-eai/src/test/java/`

**Interfaces:**
- Consumes: all sections and evidence from Tasks 1-3.
- Produces: sections 19-21, a link-clean and format-clean final document, test evidence, and one isolated documentation commit.

- [ ] **Step 1: Write sections 19-21**

Append:

```markdown
## 19. 테스트 시나리오
## 20. 근거 소스와 관련 문서
## 21. 핵심 원칙 요약
```

Create a table with consecutive IDs beginning at `EXC01`. Include success, business failure, system failure, Header validation, missing service, MVC validation, malformed JSON, unsupported method, transaction timeout, EAI business/message/connect/read failures, rollback, sensitive-data non-disclosure, and correlation evidence.

- [ ] **Step 2: Verify document structure and scenario continuity**

Run:

```powershell
rg -n '^## ' -- 'ztcf-다이어리/2026-07-26-아키텍처-이것저것/2026-07-26-07-예외처리방식.md'
$p='ztcf-다이어리/2026-07-26-아키텍처-이것저것/2026-07-26-07-예외처리방식.md'; $text=Get-Content -LiteralPath $p -Raw -Encoding utf8; [regex]::Matches($text,'EXC\d{2}') | ForEach-Object Value | Sort-Object -Unique
```

Expected: exactly 21 top-level numbered sections; `EXC` identifiers are consecutive without gaps.

- [ ] **Step 3: Validate every local Markdown link**

Run:

```powershell
$p=Resolve-Path -LiteralPath 'ztcf-다이어리/2026-07-26-아키텍처-이것저것/2026-07-26-07-예외처리방식.md'; $dir=Split-Path -Parent $p; $text=Get-Content -LiteralPath $p -Raw -Encoding utf8; $links=[regex]::Matches($text,'\[[^\]]+\]\(([^)#]+)(?:#[^)]+)?\)') | ForEach-Object {$_.Groups[1].Value} | Where-Object {$_ -notmatch '^(https?|mailto):'} | Sort-Object -Unique; $missing=@(); foreach($link in $links){$target=Join-Path $dir ([uri]::UnescapeDataString($link)); if(-not(Test-Path -LiteralPath $target)){$missing+=$link}}; Write-Output ('LOCAL_LINK_COUNT='+$links.Count); Write-Output ('MISSING_LINK_COUNT='+$missing.Count); $missing; if($missing.Count -gt 0){exit 1}
```

Expected: `MISSING_LINK_COUNT=0`.

- [ ] **Step 4: Run focused framework tests**

Run:

```powershell
.\gradlew.bat :tcf-core:test :tcf-web:test :tcf-eai:test --no-daemon
```

Expected: `BUILD SUCCESSFUL`. Record `NO-SOURCE` tasks accurately; do not claim test cases where no test source exists.

- [ ] **Step 5: Run final formatting and scope checks**

Run:

```powershell
git diff --check -- 'ztcf-다이어리/2026-07-26-아키텍처-이것저것/2026-07-26-07-예외처리방식.md'
git diff --stat -- 'ztcf-다이어리/2026-07-26-아키텍처-이것저것/2026-07-26-07-예외처리방식.md'
git status --short
```

Expected: no formatting error; only the target guide belongs to this implementation; pre-existing user changes remain untouched.

- [ ] **Step 6: Commit only the completed guide**

Run:

```powershell
git add -- 'ztcf-다이어리/2026-07-26-아키텍처-이것저것/2026-07-26-07-예외처리방식.md'
git diff --cached --check
git diff --cached --name-only
git commit -m "docs: add exception handling guide"
```

Expected: staged scope contains exactly the target guide and the commit succeeds without including user-owned changes.
