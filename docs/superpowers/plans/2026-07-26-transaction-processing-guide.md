# NSIGHT TCF Transaction Processing Guide Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Create `2026-07-26-05-트랜잭션처리방식.md` as an implementation-grounded guide connecting the TCF online pipeline with Spring database transaction, rollback, timeout, integration, idempotency, and operational rules.

**Architecture:** Produce one flow-centered Markdown document beside the existing layered-architecture and message-contract guides. Use Java sources and current configuration as primary evidence, separate online/DB/query boundaries, and label every difference as `현재 구현`, `개발 표준`, or `개선 권고`.

**Tech Stack:** UTF-8 Markdown, Java 21, Spring Boot 3.3.5, Spring Transaction Management, MyBatis, NSIGHT TCF STF/TCF/ETF pipeline, PowerShell, `rg`, Git.

## Global Constraints

- Create only `ztcf-다이어리/2026-07-26-아키텍처-이것저것/2026-07-26-05-트랜잭션처리방식.md` as the deliverable.
- Preserve all existing uncommitted user changes and do not modify Java, configuration, sample, help-index, or existing Markdown files.
- Use current implementation as primary evidence and label differing rules as `현재 구현`, `개발 표준`, or `개선 권고`.
- Keep the document UTF-8 and avoid duplicating the existing layered-architecture and message-contract guides.
- Separate the online processing boundary, Spring DB transaction boundary, individual query boundary, and external integration boundary.
- Do not claim that an online timeout proves immediate task cancellation or completed DB rollback.
- Do not expose passwords, tokens, session IDs, private data, SQL text, stack traces, exception classes, or internal paths in examples.
- Do not prescribe `@Transactional`, `readOnly`, propagation, retry, or rollback behavior unless supported by current source or explicitly labeled as a development standard.
- Stage and commit only the new target Markdown file for implementation tasks.

---

## File Structure

| Path | Responsibility |
| --- | --- |
| `ztcf-다이어리/2026-07-26-아키텍처-이것저것/2026-07-26-05-트랜잭션처리방식.md` | Single guide for pipeline flow, DB transaction boundaries, errors, three timeout layers, integration, idempotency, checklists, tests, and evidence links |

No production source, configuration, sample, existing documentation, or help index is modified.

---

### Task 1: Document the end-to-end pipeline and transaction boundaries

**Files:**
- Create: `ztcf-다이어리/2026-07-26-아키텍처-이것저것/2026-07-26-05-트랜잭션처리방식.md`
- Reference: `docs/superpowers/specs/2026-07-26-transaction-processing-document-design.md`
- Reference: `tcf-web/src/main/java/com/nh/nsight/tcf/web/entry/web/OnlineTransactionController.java`
- Reference: `tcf-web/src/main/java/com/nh/nsight/tcf/web/entry/facade/TcfGateway.java`
- Reference: `tcf-core/src/main/java/com/nh/nsight/tcf/core/support/processor/TCF.java`
- Reference: `tcf-core/src/main/java/com/nh/nsight/tcf/core/support/processor/STF.java`
- Reference: `tcf-core/src/main/java/com/nh/nsight/tcf/core/support/processor/ETF.java`
- Reference: `tcf-core/src/main/java/com/nh/nsight/tcf/core/support/dispatch/TransactionDispatcher.java`
- Reference: `tcf-core/src/main/java/com/nh/nsight/tcf/core/support/transaction/TransactionHandler.java`
- Reference: `tcf-core/src/main/java/com/nh/nsight/tcf/core/support/timeout/OnlineTransactionTimeoutExecutor.java`

**Interfaces:**
- Consumes: Current controller/gateway entry behavior, TCF orchestration, STF preparation, Dispatcher routing, Handler contract, ETF completion, and online timeout execution.
- Produces: Sections 1–7 and a stable vocabulary for `온라인 처리 경계`, `DB 트랜잭션 경계`, `DB 질의 경계`, and `외부 연동 경계` used by Tasks 2–3.

- [ ] **Step 1: Confirm the target is absent and inspect neighboring document style**

Run:

```powershell
Test-Path 'ztcf-다이어리\2026-07-26-아키텍처-이것저것\2026-07-26-05-트랜잭션처리방식.md'
Get-Content -Encoding UTF8 'ztcf-다이어리\2026-07-26-아키텍처-이것저것\2026-07-26-03-어플리케이션레이어드아키텍처.md' -TotalCount 80
Get-Content -Encoding UTF8 'ztcf-다이어리\2026-07-26-아키텍처-이것저것\2026-07-26-04-전문구성.md' -TotalCount 80
git status --short
```

Expected: target is `False`; neighboring style and unrelated user changes are visible and remain untouched.

- [ ] **Step 2: Extract the authoritative execution order**

Run:

```powershell
rg -n 'process\(|preProcess|execute\(|dispatch\(|success\(|businessFail\(|systemError\(|serviceIds|doHandle' `
  'tcf-web/src/main/java/com/nh/nsight/tcf/web/entry/web/OnlineTransactionController.java' `
  'tcf-web/src/main/java/com/nh/nsight/tcf/web/entry/facade/TcfGateway.java' `
  'tcf-core/src/main/java/com/nh/nsight/tcf/core/support/processor/TCF.java' `
  'tcf-core/src/main/java/com/nh/nsight/tcf/core/support/processor/STF.java' `
  'tcf-core/src/main/java/com/nh/nsight/tcf/core/support/processor/ETF.java' `
  'tcf-core/src/main/java/com/nh/nsight/tcf/core/support/dispatch/TransactionDispatcher.java' `
  'tcf-core/src/main/java/com/nh/nsight/tcf/core/support/transaction/TransactionHandler.java' `
  'tcf-core/src/main/java/com/nh/nsight/tcf/core/support/timeout/OnlineTransactionTimeoutExecutor.java'
```

Expected: evidence for entry enrichment, `TCF.process`, `STF.preProcess`, timeout-wrapped dispatch, Handler invocation, and ETF result paths.

- [ ] **Step 3: Write sections 1–4**

Create these exact headings:

```markdown
# NSIGHT TCF 트랜잭션 처리 방식

## 1. 목적과 범위
## 2. 핵심 용어
## 3. 전체 트랜잭션 처리 구조
## 4. 단계별 실행 흐름
```

Include this flow and explain each transition from source evidence:

```text
Client
→ JWT/Gateway 또는 OnlineTransactionController
→ TCF.process()
→ STF.preProcess()
→ OnlineTransactionTimeoutExecutor
→ TransactionDispatcher
→ Handler → Facade → Service → Rule → DAO/Mapper
→ ETF.success | businessFail | systemError
→ Client
```

Define the four boundaries explicitly and state that they do not share identical start/end times.

- [ ] **Step 4: Write sections 5–7**

Add:

```markdown
## 5. 계층별 책임과 금지사항
## 6. DB 트랜잭션 경계
## 7. 조회·등록·수정·삭제 처리 기준
```

Use a responsibility table for Controller/Gateway, STF, TCF, Timeout Executor, Dispatcher/Handler, Facade, Service, Rule, DAO/Mapper, and ETF. Label `Service 또는 유스케이스 Facade의 명확한 경계`, read-only query policy, and Handler/Controller transaction prohibition as `개발 표준` unless current annotations prove them.

- [ ] **Step 5: Verify Task 1 coverage**

Run:

```powershell
$doc = 'ztcf-다이어리\2026-07-26-아키텍처-이것저것\2026-07-26-05-트랜잭션처리방식.md'
rg -n '^## [1-7]\.' $doc
rg -n '온라인 처리 경계|DB 트랜잭션 경계|DB 질의 경계|외부 연동 경계|STF|TCF|ETF|TransactionDispatcher' $doc
git diff --check -- $doc
```

Expected: sections 1–7 and all four boundaries are present; no whitespace errors.

- [ ] **Step 6: Commit Task 1**

```powershell
git add -- 'ztcf-다이어리/2026-07-26-아키텍처-이것저것/2026-07-26-05-트랜잭션처리방식.md'
git diff --cached --check
git diff --cached --name-only
git commit -m "docs: define transaction processing flow"
```

Expected: only the new transaction-processing Markdown file is committed.

---

### Task 2: Document rollback, timeout, integration, and operational behavior

**Files:**
- Modify: `ztcf-다이어리/2026-07-26-아키텍처-이것저것/2026-07-26-05-트랜잭션처리방식.md`
- Reference: `tcf-web/src/main/java/com/nh/nsight/tcf/web/application/rule/PolicyDrivenTransactionExecutor.java`
- Reference: `tcf-web/src/main/java/com/nh/nsight/tcf/web/application/rule/PolicyDrivenTransactionAttributeSource.java`
- Reference: `tcf-web/src/main/java/com/nh/nsight/tcf/web/support/PolicyDrivenQueryTimeoutInterceptor.java`
- Reference: `tcf-core/src/main/java/com/nh/nsight/tcf/core/support/timeout/TimeoutPolicyService.java`
- Reference: `tcf-core/src/main/java/com/nh/nsight/tcf/core/support/timeout/TimeoutExceptionResolver.java`
- Reference: `tcf-core/src/main/java/com/nh/nsight/tcf/core/support/idempotency/IdempotencyChecker.java`
- Reference: `tcf-core/src/main/java/com/nh/nsight/tcf/core/support/control/TransactionControlService.java`
- Reference: `tcf-web/src/main/java/com/nh/nsight/tcf/web/entry/web/GuidMdcCleanupFilter.java`
- Reference: `zdocs-1/architecture/46-service-integration-contract.md`

**Interfaces:**
- Consumes: The four boundary definitions and layer responsibilities produced by Task 1.
- Produces: Sections 8–16 with exact current rollback and timeout behavior, external integration rules, idempotency, security/control, observability, cleanup, and a current-state drift matrix.

- [ ] **Step 1: Extract transaction and timeout implementation evidence**

Run:

```powershell
rg -n '@Transactional|TransactionAttribute|rollback|readOnly|propagation|timeout|setQueryTimeout|TimeoutContext|idempot|transactionControl|clear\(|remove\(' `
  'tcf-web/src/main/java/com/nh/nsight/tcf/web/application/rule/PolicyDrivenTransactionExecutor.java' `
  'tcf-web/src/main/java/com/nh/nsight/tcf/web/application/rule/PolicyDrivenTransactionAttributeSource.java' `
  'tcf-web/src/main/java/com/nh/nsight/tcf/web/support/PolicyDrivenQueryTimeoutInterceptor.java' `
  'tcf-core/src/main/java/com/nh/nsight/tcf/core/support/timeout' `
  'tcf-core/src/main/java/com/nh/nsight/tcf/core/support/idempotency' `
  'tcf-core/src/main/java/com/nh/nsight/tcf/core/support/control' `
  'tcf-web/src/main/java/com/nh/nsight/tcf/web/entry/web/GuidMdcCleanupFilter.java'
```

Expected: evidence for transaction attributes, timeout propagation, query timeout, idempotency/control calls, and cleanup paths.

- [ ] **Step 2: Write Commit, Rollback, and exception sections**

Add:

```markdown
## 8. Commit과 Rollback 규칙
## 9. 예외별 처리 방식
```

Separate normal success, `BusinessException`, unexpected system exception, checked exception, swallowed exception, self-invocation, private method, and asynchronous execution. Do not state rollback behavior for `BusinessException` until its inheritance and transaction proxy configuration are verified.

- [ ] **Step 3: Write three-level Timeout behavior**

Add:

```markdown
## 10. 3단계 Timeout
```

Use columns:

```text
구분 | 정책 값 | 적용 지점 | 제한 범위 | Timeout 후 상태 | 운영 확인
```

Cover Online, Transaction, and DB Query timeout. Explicitly state that Online Timeout response, worker interruption, transaction completion, and database rollback are distinct observations.

- [ ] **Step 4: Write integration, idempotency, control, and observability sections**

Add:

```markdown
## 11. 외부 서비스 연동과 트랜잭션
## 12. 멱등성·재시도·중복 방지
## 13. 거래통제·권한·세션
## 14. 로그·감사·메트릭
## 15. ThreadLocal·MDC 정리
```

Explain local transaction limits, call ordering, compensation, state inquiry, idempotency keys, safe retry conditions, STF control order, and cleanup on every termination path. Reference `tcf-eai` and the integration contract without inventing distributed transaction support.

- [ ] **Step 5: Write the implementation alignment matrix**

Add:

```markdown
## 16. 구현 정합성 및 개선 권고
```

Use columns:

```text
항목 | 현재 구현 | 개발 표준 | 위험 | 개선 권고
```

Include at least: transaction annotation location, checked-exception rollback, swallowed exceptions, self-invocation, Online Timeout cancellation ambiguity, external-call partial success, retry eligibility, and ThreadLocal/MDC cleanup.

- [ ] **Step 6: Verify claims and sensitive-data safety**

Run:

```powershell
$doc = 'ztcf-다이어리\2026-07-26-아키텍처-이것저것\2026-07-26-05-트랜잭션처리방식.md'
rg -n '^## (8|9|10|11|12|13|14|15|16)\.' $doc
rg -n '현재 구현|개발 표준|개선 권고|Online Timeout|Transaction Timeout|DB Query Timeout|Rollback|멱등' $doc
rg -n -i 'password|access.?token|refresh.?token|session.?id|private.?key|stack trace|select \* from|jdbc:' $doc
git diff --check -- $doc
```

Expected: sections 8–16 exist; sensitive scan hits only prohibition or conceptual text, never actual credentials, SQL, or connection strings.

- [ ] **Step 7: Run focused framework tests**

Run:

```powershell
.\gradlew.bat :tcf-core:test :tcf-web:test --no-daemon
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 8: Commit Task 2**

```powershell
git add -- 'ztcf-다이어리/2026-07-26-아키텍처-이것저것/2026-07-26-05-트랜잭션처리방식.md'
git diff --cached --check
git diff --cached --name-only
git commit -m "docs: explain transaction rollback and timeout"
```

Expected: only the target Markdown file is committed.

---

### Task 3: Add checklists, test scenarios, evidence links, and final verification

**Files:**
- Modify: `ztcf-다이어리/2026-07-26-아키텍처-이것저것/2026-07-26-05-트랜잭션처리방식.md`
- Reference: `ztcf-다이어리/2026-07-26-아키텍처-이것저것/2026-07-26-03-어플리케이션레이어드아키텍처.md`
- Reference: `ztcf-다이어리/2026-07-26-아키텍처-이것저것/2026-07-26-04-전문구성.md`
- Reference: `zdocs-1/architecture/03-transaction.md`
- Reference: `zdocs-1/architecture/05-exception.md`
- Reference: `zdocs-1/architecture/08-timeout.md`
- Reference: `zdocs-1/architecture/39-header-transaction-control.md`
- Reference: `zdocs-1/architecture/41-service-timeout-policy.md`
- Reference: `zdocs-1/architecture/50-test-architecture.md`

**Interfaces:**
- Consumes: The pipeline, boundaries, error/timeout rules, and alignment matrix from Tasks 1–2.
- Produces: Sections 17–21 with reusable new/change checklists, exactly 16 transaction scenarios, valid evidence links, a concise summary, and final verification evidence.

- [ ] **Step 1: Add the new-transaction checklist**

Add:

```markdown
## 17. 신규 거래 체크리스트
```

Include unchecked items for ServiceId/processing type, read vs write classification, transaction owner, rollback exceptions, propagation, three timeout policies, external calls, compensation, idempotency, retry, transaction control, permission/session, logging/PII, MDC/ThreadLocal cleanup, and contract tests.

- [ ] **Step 2: Add the transaction-change checklist**

Add:

```markdown
## 18. 변경 거래 체크리스트
```

Include unchecked items for transaction-boundary movement, added DB writes, exception conversion, timeout change, external side effects, old/new coexistence, rollback and compensation, Service Catalog/control/timeout consumers, deployment rollback, and post-deployment evidence.

- [ ] **Step 3: Add exactly 16 test scenarios**

Add:

```markdown
## 19. 테스트 시나리오
```

Use columns:

```text
ID | 분류 | 입력·조건 | 기대 트랜잭션 상태 | 기대 응답·증적
```

Define `TX01` through `TX16` exactly as: query success, write commit, business-error rollback, system-error rollback, checked exception, swallowed exception, self-invocation, Online Timeout, Transaction Timeout, DB Query Timeout, external integration timeout, duplicate idempotency key, transaction-control block, authentication/authorization failure, logging/MDC cleanup, and partial success/compensation.

- [ ] **Step 4: Add source links and principles summary**

Add:

```markdown
## 20. 근거 소스와 관련 문서
## 21. 핵심 원칙 요약
```

Use target-file-relative Markdown links for every referenced Java and Markdown source. End with concise principles: TCF owns the online pipeline, Service/Facade owns the DB boundary, DAO/Mapper owns access only, timeout layers are distinct, and external side effects require idempotency and compensation.

- [ ] **Step 5: Verify structural coverage**

Run:

```powershell
$doc = 'ztcf-다이어리\2026-07-26-아키텍처-이것저것\2026-07-26-05-트랜잭션처리방식.md'
rg -n '^## ' $doc
rg -n '^- \[ \]' $doc
rg -n '^\| TX(0[1-9]|1[0-6]) ' $doc
git diff --check -- $doc
```

Expected: top-level sections 1–21, both reusable checklists, exactly TX01–TX16, and no whitespace errors.

- [ ] **Step 6: Verify every relative link resolves**

Run:

```powershell
$doc = Resolve-Path 'ztcf-다이어리\2026-07-26-아키텍처-이것저것\2026-07-26-05-트랜잭션처리방식.md'
$base = Split-Path $doc
$text = Get-Content -Raw -Encoding UTF8 $doc
$links = [regex]::Matches($text, '\]\((?!https?://|#)([^)>]+)(?:#[^)]+)?\)')
$missing = foreach ($link in $links) {
    $raw = [uri]::UnescapeDataString($link.Groups[1].Value.Trim('<','>'))
    if (-not (Test-Path (Join-Path $base $raw))) { $raw }
}
$missing
```

Expected: no output.

- [ ] **Step 7: Run final focused tests and verify the exact staged scope**

Run:

```powershell
.\gradlew.bat :tcf-core:test :tcf-web:test --no-daemon
git add -- 'ztcf-다이어리/2026-07-26-아키텍처-이것저것/2026-07-26-05-트랜잭션처리방식.md'
git diff --cached --check
git diff --cached --name-only
```

Expected: Gradle reports `BUILD SUCCESSFUL`; the staged file list contains only `2026-07-26-05-트랜잭션처리방식.md`.

- [ ] **Step 8: Commit and capture final evidence**

```powershell
git commit -m "docs: complete transaction processing guide"
git show --stat --oneline --summary HEAD
git status --short -- 'ztcf-다이어리/2026-07-26-아키텍처-이것저것/2026-07-26-05-트랜잭션처리방식.md'
```

Expected: the commit contains only the target guide and the target path is clean. Report unrelated pre-existing worktree changes separately.
