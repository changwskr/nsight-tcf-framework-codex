# NSIGHT TCF Standard Message Document Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Create `2026-07-26-04-전문구성.md` as an implementation-grounded standard-message guide covering contracts, runtime flow, inconsistencies, security, checklists, and test scenarios.

**Architecture:** Produce one contract-centered Markdown document beside the existing naming and layered-architecture notes. Treat Java message classes and the runtime catalog/validator as implementation evidence, distinguish documentation and sample drift explicitly, and do not change message code or existing samples.

**Tech Stack:** UTF-8 Markdown, Java 21 source evidence, Spring Boot 3.3.5, NSIGHT TCF `StandardRequest`/`StandardResponse`, PowerShell, `rg`, Git.

## Global Constraints

- Create only `ztcf-다이어리/2026-07-26-아키텍처-이것저것/2026-07-26-04-전문구성.md` for the deliverable.
- Preserve all existing uncommitted user changes and do not edit message classes, validators, catalog entries, or sample JSON.
- Use current implementation as the primary evidence; label differing documentation and samples as `현재 구현`, `문서 설명`, or `개선 권고`.
- Keep the document UTF-8 Markdown and follow the concise explanatory style of the neighboring architecture notes.
- Do not expose passwords, tokens, session IDs, private information, SQL, stack traces, or internal exception details in examples.
- Describe the standard JSON transaction separately from multipart, Relay, Gateway proxy, and other non-standard REST contracts.
- Do not claim a field is required merely because an older document says so; derive request-header required fields from `TcfStandardMessageCatalog.requiredRequestHeaderFieldKeys()` and its `requiredYn` definitions.
- Keep existing ServiceId examples as current-state evidence and explain the approved new CRUD action vocabulary separately.

---

## File Structure

| Path | Responsibility |
| --- | --- |
| `ztcf-다이어리/2026-07-26-아키텍처-이것저것/2026-07-26-04-전문구성.md` | Single standard-message architecture and developer guide: contracts, field dictionaries, flow, errors, security, drift, checklists, tests, and evidence links |

No production source, configuration, sample, or index file is modified by this plan.

---

### Task 1: Build the message contract and field dictionaries

**Files:**
- Create: `ztcf-다이어리/2026-07-26-아키텍처-이것저것/2026-07-26-04-전문구성.md`
- Reference: `docs/superpowers/specs/2026-07-26-standard-message-document-design.md`
- Reference: `tcf-core/src/main/java/com/nh/nsight/tcf/core/support/message/StandardRequest.java`
- Reference: `tcf-core/src/main/java/com/nh/nsight/tcf/core/support/message/StandardHeader.java`
- Reference: `tcf-core/src/main/java/com/nh/nsight/tcf/core/support/message/StandardResponse.java`
- Reference: `tcf-core/src/main/java/com/nh/nsight/tcf/core/support/message/Result.java`
- Reference: `tcf-core/src/main/java/com/nh/nsight/tcf/core/support/message/ProcessingType.java`
- Reference: `tcf-core/src/main/java/com/nh/nsight/tcf/core/support/message/catalog/TcfStandardMessageCatalog.java`

**Interfaces:**
- Consumes: Java fields and factory behavior from `StandardRequest<T>`, `StandardHeader`, `StandardResponse<T>`, and `Result`; request/response field metadata from `TcfStandardMessageCatalog`.
- Produces: Document sections 1–8 containing terminology, transport contract, request/response shapes, complete Header and Result dictionaries, and safe JSON examples used by later tasks.

- [ ] **Step 1: Confirm the target file is absent and neighboring documents remain untouched**

Run:

```powershell
Test-Path 'ztcf-다이어리\2026-07-26-아키텍처-이것저것\2026-07-26-04-전문구성.md'
git status --short
```

Expected: `False` for the target file before creation; existing unrelated changes remain visible and must not be staged.

- [ ] **Step 2: Extract the authoritative field lists**

Run:

```powershell
rg -n 'private String|defineRequestHeaderFields|defineResponseHeaderFields|defineResultFields|requiredYn|field\(' `
  'tcf-core/src/main/java/com/nh/nsight/tcf/core/support/message/StandardHeader.java' `
  'tcf-core/src/main/java/com/nh/nsight/tcf/core/support/message/Result.java' `
  'tcf-core/src/main/java/com/nh/nsight/tcf/core/support/message/catalog/TcfStandardMessageCatalog.java'
```

Expected: all 15 `StandardHeader` fields, all 7 `Result` fields, and request/response catalog definitions are visible. Header fields are `systemId`, `businessCode`, `serviceId`, `serviceName`, `transactionCode`, `processingType`, `guid`, `traceId`, `channelId`, `userId`, `branchId`, `centerId`, `requestTime`, `clientIp`, and `idempotencyKey`.

- [ ] **Step 3: Write the document header, scope, terminology, and transport contract**

Create these exact top-level sections:

```markdown
# NSIGHT TCF 전문 구성

## 1. 목적과 범위
## 2. 핵심 용어
## 3. 전문 전체 구조
## 4. 전송 계약
```

Include:

```text
StandardRequest<T> = header + body
StandardResponse<T> = header + result + body
POST /online or POST /{businessCode}/online
Content-Type: application/json; charset=UTF-8
Standard transaction success/failure: HTTP 200 plus result.resultCode
```

Explicitly state that non-standard multipart, Relay, and administrative REST endpoints can use different HTTP-status behavior.

- [ ] **Step 4: Write the request contract and complete Header dictionary**

Add:

```markdown
## 5. 요청 전문
### 5.1 요청 JSON
### 5.2 Header 필드 사전
### 5.3 processingType
### 5.4 Body 계약
```

The Header table must use these columns:

```text
필드 | Java 타입 | 요청 필수 | 생성·보완 주체 | 검증·정규화 | 주요 사용처 | 예시 | 보안 주의
```

Record request-required catalog fields as `businessCode`, `serviceId`, `transactionCode`, `processingType`, and `channelId`. Record normalization: default `systemId=NSIGHT-MP`, default ISO-8601 `requestTime`, uppercase trimmed `businessCode`, and uppercase trimmed `processingType`. Mention `user` → `userId` and `branch` → `branchId` JSON aliases.

- [ ] **Step 5: Write the response and Result contracts**

Add:

```markdown
## 6. 응답 전문
### 6.1 응답 JSON
### 6.2 응답 Header
### 6.3 Result 필드 사전
### 6.4 성공 판정
```

Document `S0000` as success and current generic failure `E0001`. State that `StandardResponse.fail(...)` does not set a body. Include all Result fields and classify `errorDetail` as an internal diagnostic value that must not expose SQL, stack traces, paths, or exception classes to external clients.

- [ ] **Step 6: Add five safe JSON examples**

Add:

```markdown
## 7. JSON 예시
### 7.1 정상 요청
### 7.2 정상 응답
### 7.3 Header 검증 오류
### 7.4 업무 오류
### 7.5 시스템 오류
```

Use fictional identifiers such as `U-DEMO-001`, `127.0.0.1`, and generated-looking GUIDs. Do not include tokens, session IDs, passwords, real IPs, SQL, or stack traces. Use current-state ServiceId `SV.Sample.inquiry` only when labeled as an existing example; use `SV.Customer.selectList` when illustrating the approved new CRUD naming standard.

- [ ] **Step 7: Verify contract completeness**

Run:

```powershell
$doc = 'ztcf-다이어리\2026-07-26-아키텍처-이것저것\2026-07-26-04-전문구성.md'
rg -n 'systemId|businessCode|serviceId|serviceName|transactionCode|processingType|guid|traceId|channelId|userId|branchId|centerId|requestTime|clientIp|idempotencyKey' $doc
rg -n 'resultCode|resultMessage|errorCode|errorMessage|errorDetail|errorSystemId|errorDateTime' $doc
```

Expected: every Header and Result field appears in its dictionary and relevant example or explanation.

- [ ] **Step 8: Commit the contract section**

```powershell
git add -- 'ztcf-다이어리/2026-07-26-아키텍처-이것저것/2026-07-26-04-전문구성.md'
git diff --cached --check
git commit -m "docs: define standard message contracts"
```

Expected: only the new professional-message Markdown file is committed.

---

### Task 2: Add runtime flow, validation, errors, security, and drift analysis

**Files:**
- Modify: `ztcf-다이어리/2026-07-26-아키텍처-이것저것/2026-07-26-04-전문구성.md`
- Reference: `tcf-web/src/main/java/com/nh/nsight/tcf/web/entry/web/OnlineTransactionController.java`
- Reference: `tcf-core/src/main/java/com/nh/nsight/tcf/core/support/processor/STF.java`
- Reference: `tcf-core/src/main/java/com/nh/nsight/tcf/core/support/processor/ETF.java`
- Reference: `tcf-core/src/main/java/com/nh/nsight/tcf/core/support/validation/StandardHeaderValidator.java`
- Reference: `tcf-core/src/main/java/com/nh/nsight/tcf/core/support/message/catalog/TcfStandardMessageCatalog.java`
- Reference: `tcf-ui/src/main/resources/sample-requests/sv-sample-inquiry.json`
- Reference: `zdocs-1/architecture/02-junmun.md`
- Reference: `zdocs-1/architecture/04-messaging.md`

**Interfaces:**
- Consumes: Contract and field names produced by Task 1; runtime behavior from Controller, STF, ETF, Validator, and catalog.
- Produces: Document sections explaining field lifecycle, validation ownership, error paths, security constraints, and an evidence-based inconsistency matrix.

- [ ] **Step 1: Extract runtime mutation and validation evidence**

Run:

```powershell
rg -n 'businessCode|clientIp|normalize|validate|guid|traceId|success|businessFail|systemError|StandardResponse|resultCode' `
  'tcf-web/src/main/java/com/nh/nsight/tcf/web/entry/web/OnlineTransactionController.java' `
  'tcf-core/src/main/java/com/nh/nsight/tcf/core/support/processor/STF.java' `
  'tcf-core/src/main/java/com/nh/nsight/tcf/core/support/processor/ETF.java' `
  'tcf-core/src/main/java/com/nh/nsight/tcf/core/support/validation/StandardHeaderValidator.java'
```

Expected: evidence for URL/client-IP enrichment, catalog-driven required validation, normalization, correlation IDs, and response assembly.

- [ ] **Step 2: Write the runtime sequence and field lifecycles**

Add:

```markdown
## 8. 전문 처리 흐름
## 9. 주요 필드 생명주기
```

Use this precise shape:

```text
Client
→ OnlineTransactionController
→ TCF.process
→ STF.preProcess
→ TransactionDispatcher
→ Handler → Facade → Service
→ ETF.success / businessFail / systemError
→ Client
```

Describe `businessCode`, `serviceId`, `guid`, `traceId`, `clientIp`, and `body` from producer through final consumer. Do not claim a mutation unless it is supported by the referenced source.

- [ ] **Step 3: Write validation ownership and processingType rules**

Add:

```markdown
## 10. 검증과 정규화
```

Separate responsibilities:

```text
Controller: HTTP/JSON and URL enrichment
STF/StandardHeaderValidator: common Header, normalization, session/auth/idempotency/control
Handler/DTO: body shape
Rule/Service: business rules and state
```

List `INQUIRY`, `CREATE`, `UPDATE`, `DELETE`, `EXECUTE`, `DOWNLOAD`, and `UPLOAD` exactly as defined by `ProcessingType`.

- [ ] **Step 4: Write error paths and security rules**

Add:

```markdown
## 11. 성공과 오류 응답
## 12. 보안·개인정보·로그
```

Cover common validation, business, and system errors separately. Explain that ETF creates the standardized response and performs final logging/metrics behavior. Include explicit prohibitions on secrets, session IDs, PII, full body logging, SQL, stack traces, and internal paths.

- [ ] **Step 5: Build the implementation/document/sample inconsistency matrix**

Add:

```markdown
## 13. 구현·문서·샘플 정합성 이슈
```

Use columns:

```text
항목 | 현재 구현 | 문서·샘플 | 영향 | 개선 권고
```

Include at least:

1. `transactionIntime`, `transactionOuttime`, `systemDate`, and `bizDate` exist in `sv-sample-inquiry.json` but not `StandardHeader`.
2. Required fields are catalog-driven through `requiredRequestHeaderFieldKeys()`.
3. Existing examples use `inquiry`/`save`, while the approved new CRUD vocabulary is `selectList`/`selectDetail`/`create`/`update`/`delete`.
4. Some response bodies duplicate `businessCode`, `serviceId`, or `guid` already present in Header.
5. Standard JSON transactions use HTTP 200 plus `resultCode`, while non-standard REST can use HTTP status differently.

- [ ] **Step 6: Verify claims against source and scan for sensitive example data**

Run:

```powershell
$doc = 'ztcf-다이어리\2026-07-26-아키텍처-이것저것\2026-07-26-04-전문구성.md'
rg -n '현재 구현|문서·샘플|개선 권고|transactionIntime|transactionOuttime|systemDate|bizDate|requiredRequestHeaderFieldKeys' $doc
rg -n -i 'password|access.?token|refresh.?token|session.?id|private.?key|stack trace|select \* from|jdbc:' $doc
```

Expected: the inconsistency matrix contains all five required issues; the sensitive-data scan finds only prohibition text, never actual credentials or SQL examples.

- [ ] **Step 7: Commit runtime and drift sections**

```powershell
git add -- 'ztcf-다이어리/2026-07-26-아키텍처-이것저것/2026-07-26-04-전문구성.md'
git diff --cached --check
git commit -m "docs: explain message runtime and drift"
```

Expected: only the standard-message Markdown file is committed.

---

### Task 3: Add operational checklists, test scenarios, links, and final verification

**Files:**
- Modify: `ztcf-다이어리/2026-07-26-아키텍처-이것저것/2026-07-26-04-전문구성.md`
- Reference: `ztcf-다이어리/2026-07-26-아키텍처-이것저것/2026-07-26-02-명명규칙.md`
- Reference: `ztcf-다이어리/2026-07-26-아키텍처-이것저것/2026-07-26-03-어플리케이션레이어드아키텍처.md`
- Reference: `zdocs-1/architecture/02-junmun.md`
- Reference: `zdocs-1/architecture/04-messaging.md`
- Reference: `zdocs-1/architecture/34-STF.md`
- Reference: `zdocs-1/architecture/36-ETF.md`
- Reference: `zdocs-1/architecture/39-header-transaction-control.md`
- Reference: `zdocs-1/architecture/41-service-timeout-policy.md`

**Interfaces:**
- Consumes: Contract, runtime, security, and inconsistency sections from Tasks 1–2.
- Produces: Complete guide with reusable creation/change checklists, 16 explicit test scenarios, evidence links, and a verified final document.

- [ ] **Step 1: Add the new-message checklist**

Add:

```markdown
## 14. 신규 전문 체크리스트
```

Include checkboxes for business code/ServiceId, processing type/transaction code, required Header values, request/response bodies, DTO mapping, Header/body duplication, validation ownership, error codes, PII/logging, Service Catalog, transaction control, timeout policy, sample JSON, and help indexing.

- [ ] **Step 2: Add the change checklist**

Add:

```markdown
## 15. 전문 변경 체크리스트
```

Include checkboxes for caller compatibility, field add/delete/type changes, required-flag changes, ServiceId/transactionCode consumers, Validator/Catalog consistency, Handler/DTO/UI/sample/test coordination, rollback, and coexistence of old/new contracts.

- [ ] **Step 3: Add the 16 scenario test matrix**

Add:

```markdown
## 16. 테스트 시나리오
```

Use columns:

```text
ID | 분류 | 입력·조건 | 기대 처리 | 기대 응답·증적
```

Define exactly these cases: normal success, missing required Header, businessCode/ServiceId mismatch, unknown ServiceId, invalid processingType, GUID/TraceId generation, session failure, authorization failure, idempotency duplicate, transaction-control block, online timeout, transaction timeout, DB query timeout, body validation failure, business error, and system error/internal-detail non-exposure. Note the unregistered sample Header fields as an additional compatibility observation below the matrix.

- [ ] **Step 4: Add evidence links and final summary**

Add:

```markdown
## 17. 근거 소스와 관련 문서
## 18. 핵심 원칙 요약
```

Use repository-relative Markdown links to every source listed in Tasks 1–3. End with a concise statement that Header carries common routing/control/trace context, Body carries service-specific data, Result carries outcome, and TCF owns common validation and response assembly.

- [ ] **Step 5: Verify section coverage and relative-link targets**

Run:

```powershell
$doc = 'ztcf-다이어리\2026-07-26-아키텍처-이것저것\2026-07-26-04-전문구성.md'
rg -n '^## ' $doc
rg -n '^- \[ \]' $doc
rg -n '^\| T(0[1-9]|1[0-6]) ' $doc
git diff --check -- $doc
```

Expected: sections 1–18 are present, both checklists contain unchecked reusable items, all T01–T16 scenarios exist, and `git diff --check` produces no errors.

- [ ] **Step 6: Verify Markdown links resolve**

Run the following read-only PowerShell check from the repository root:

```powershell
$doc = Resolve-Path 'ztcf-다이어리\2026-07-26-아키텍처-이것저것\2026-07-26-04-전문구성.md'
$base = Split-Path $doc
$text = Get-Content -Raw -Encoding UTF8 $doc
$links = [regex]::Matches($text, '\]\((?!https?://|#)([^)>]+)(?:#[^)]+)?\)')
$missing = foreach ($link in $links) {
    $raw = [uri]::UnescapeDataString($link.Groups[1].Value.Trim('<','>'))
    $path = Join-Path $base $raw
    if (-not (Test-Path $path)) { $raw }
}
$missing
```

Expected: no output.

- [ ] **Step 7: Confirm only the intended file is staged and commit**

```powershell
git add -- 'ztcf-다이어리/2026-07-26-아키텍처-이것저것/2026-07-26-04-전문구성.md'
git diff --cached --check
git diff --cached --name-only
git commit -m "docs: complete standard message guide"
```

Expected: staged name output contains only `2026-07-26-04-전문구성.md`; commit succeeds.

- [ ] **Step 8: Final evidence check**

```powershell
git show --stat --oneline --summary HEAD
git status --short -- 'ztcf-다이어리/2026-07-26-아키텍처-이것저것/2026-07-26-04-전문구성.md'
```

Expected: the final commit contains only the target guide change and the target path is clean. Report unrelated pre-existing worktree changes separately.
