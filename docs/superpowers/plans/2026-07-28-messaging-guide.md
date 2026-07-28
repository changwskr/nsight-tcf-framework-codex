# NSIGHT TCF Messaging Guide Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Create `2026-07-26-06-메시지처리방식.md` as an implementation-grounded guide for the NSIGHT TCF message lifecycle, entry paths, serialization, Relay, security, timeout, service integration, and testing.

**Architecture:** Produce one lifecycle-centered Markdown document beside the existing message-contract and transaction-processing guides. Use `zdocs-1/architecture/04-messaging.md` as the primary narrative reference while verifying every current-state claim against Java, JavaScript, and configuration sources; distinguish `현재 구현`, `개발 표준`, and `개선 권고`.

**Tech Stack:** UTF-8 Markdown, Java 21, Spring Boot 3.3.5, Spring MVC, Jackson, RestClient, NSIGHT TCF STF/TCF/ETF, tcf-ui Relay, tcf-gateway, tcf-eai, PowerShell, `rg`, Git.

## Global Constraints

- Create only `ztcf-다이어리/2026-07-26-아키텍처-이것저것/2026-07-26-06-메시지처리방식.md` as the deliverable.
- Preserve all existing uncommitted user changes and do not modify Java, JavaScript, configuration, samples, help indexes, or existing Markdown files.
- Use `zdocs-1/architecture/04-messaging.md` as the narrative reference but current implementation as the authoritative evidence.
- Label implementation differences as `현재 구현`, `개발 표준`, or `개선 권고`.
- Keep the document UTF-8 and avoid repeating the field dictionary from `2026-07-26-04-전문구성.md` or transaction detail from `2026-07-26-05-트랜잭션처리방식.md`.
- Separate Transport, Serialization, Semantic, and Adaptation responsibilities.
- Classify every entry path as `TCF 전체 적용`, `대상 WAR에서 적용`, or `TCF 미적용`.
- Do not describe Kafka, JMS, or another broker as a currently implemented core capability.
- Do not expose passwords, access/refresh tokens, session IDs, private information, SQL, stack traces, exception classes, or internal paths in examples.
- Do not state that HTTP 200 alone means success; distinguish standard TCF results from JWT Filter, Gateway, Relay, and non-standard REST errors.
- Do not state that Relay may rewrite Header, Body, or Result unless a referenced implementation proves the mutation.
- Stage and commit only the new target Markdown file during implementation.

---

## File Structure

| Path | Responsibility |
| --- | --- |
| `ztcf-다이어리/2026-07-26-아키텍처-이것저것/2026-07-26-06-메시지처리방식.md` | Single messaging guide for layers, lifecycle, entry paths, conversion, errors, security, integration, checklists, tests, and evidence links |

No source, configuration, sample, existing documentation, or index file is modified.

---

### Task 1: Build the messaging layers, lifecycle, and standard entry paths

**Files:**
- Create: `ztcf-다이어리/2026-07-26-아키텍처-이것저것/2026-07-26-06-메시지처리방식.md`
- Reference: `docs/superpowers/specs/2026-07-28-messaging-guide-design.md`
- Reference: `zdocs-1/architecture/04-messaging.md`
- Reference: `tcf-core/src/main/java/com/nh/nsight/tcf/core/support/message/StandardRequest.java`
- Reference: `tcf-core/src/main/java/com/nh/nsight/tcf/core/support/message/StandardResponse.java`
- Reference: `tcf-core/src/main/java/com/nh/nsight/tcf/core/support/processor/STF.java`
- Reference: `tcf-core/src/main/java/com/nh/nsight/tcf/core/support/processor/TCF.java`
- Reference: `tcf-core/src/main/java/com/nh/nsight/tcf/core/support/processor/ETF.java`
- Reference: `tcf-core/src/main/java/com/nh/nsight/tcf/core/support/dispatch/TransactionDispatcher.java`
- Reference: `tcf-web/src/main/java/com/nh/nsight/tcf/web/entry/web/OnlineTransactionController.java`
- Reference: `tcf-web/src/main/java/com/nh/nsight/tcf/web/entry/facade/TcfGateway.java`

**Interfaces:**
- Consumes: Standard request/response models, Controller and Gateway entry behavior, STF/TCF/ETF lifecycle, and serviceId routing.
- Produces: Sections 1–7 and stable definitions for the four layers, lifecycle stages, and entry-path classifications used by Tasks 2–3.

- [ ] **Step 1: Confirm target absence and inspect adjacent documentation**

Run:

```powershell
Test-Path 'ztcf-다이어리\2026-07-26-아키텍처-이것저것\2026-07-26-06-메시지처리방식.md'
Get-Content -Encoding UTF8 'zdocs-1\architecture\04-messaging.md' -TotalCount 160
Get-Content -Encoding UTF8 'ztcf-다이어리\2026-07-26-아키텍처-이것저것\2026-07-26-04-전문구성.md' -TotalCount 60
Get-Content -Encoding UTF8 'ztcf-다이어리\2026-07-26-아키텍처-이것저것\2026-07-26-05-트랜잭션처리방식.md' -TotalCount 60
git status --short
```

Expected: target is `False`; the source guide, neighboring style, and unrelated user changes are visible.

- [ ] **Step 2: Extract authoritative lifecycle evidence**

Run:

```powershell
rg -n '@RequestBody|process\(|preProcess|dispatch\(|success\(|businessFail\(|systemError\(|buildHeader|invoke\(' `
  'tcf-web/src/main/java/com/nh/nsight/tcf/web/entry/web/OnlineTransactionController.java' `
  'tcf-web/src/main/java/com/nh/nsight/tcf/web/entry/facade/TcfGateway.java' `
  'tcf-core/src/main/java/com/nh/nsight/tcf/core/support/processor/STF.java' `
  'tcf-core/src/main/java/com/nh/nsight/tcf/core/support/processor/TCF.java' `
  'tcf-core/src/main/java/com/nh/nsight/tcf/core/support/processor/ETF.java' `
  'tcf-core/src/main/java/com/nh/nsight/tcf/core/support/dispatch/TransactionDispatcher.java'
```

Expected: evidence for MVC deserialization, Controller enrichment, TCF orchestration, STF processing, serviceId routing, ETF assembly, and Gateway-created requests.

- [ ] **Step 3: Write sections 1–4**

Create these exact headings:

```markdown
# NSIGHT TCF 메시지 처리 방식

## 1. 목적과 범위
## 2. 핵심 용어
## 3. 메시지 처리 4계층
## 4. 전체 메시지 생명주기
```

Define Transport, Serialization, Semantic, and Adaptation. Include the exact lifecycle from channel composition through client result judgment and state that the current core uses synchronous HTTP/JSON.

- [ ] **Step 4: Write entry-path sections 5–7**

Add:

```markdown
## 5. 진입점 유형 비교
## 6. 표준 JSON 메시지 처리
## 7. TcfGateway 프로그램 위임
```

The comparison table must use:

```text
유형 | 진입 컴포넌트 | 입력 | 메시지 변환 | TCF 적용 위치 | 오류 계약 | 사용 사례
```

Classify standard JSON as `TCF 전체 적용` and Gateway invocation as a Java adaptation that creates `StandardRequest` and then uses the full pipeline. Verify every Gateway default before documenting it.

- [ ] **Step 5: Verify Task 1 structure and scope**

Run:

```powershell
$doc = 'ztcf-다이어리\2026-07-26-아키텍처-이것저것\2026-07-26-06-메시지처리방식.md'
rg -n '^## [1-7]\.' $doc
rg -n 'Transport|Serialization|Semantic|Adaptation|TCF 전체 적용|StandardRequest|StandardResponse|TcfGateway' $doc
git diff --check -- $doc
```

Expected: sections 1–7 and all layer/lifecycle terms are present with no whitespace errors.

- [ ] **Step 6: Commit Task 1**

```powershell
git add -- 'ztcf-다이어리/2026-07-26-아키텍처-이것저것/2026-07-26-06-메시지처리방식.md'
git diff --cached --check
git diff --cached --name-only
git commit -m "docs: define messaging lifecycle and entries"
```

Expected: only the new messaging guide is committed.

---

### Task 2: Document Relay, conversion, errors, security, and service integration

**Files:**
- Modify: `ztcf-다이어리/2026-07-26-아키텍처-이것저것/2026-07-26-06-메시지처리방식.md`
- Reference: `tcf-ui/src/main/java/com/nh/nsight/tcf/ui/application/service/TransactionRelayService.java`
- Reference: `tcf-ui/src/main/java/com/nh/nsight/tcf/ui/entry/web/TcfApiController.java`
- Reference: `tcf-ui/src/main/resources/static/js/online-multi.js`
- Reference: `tcf-ui/src/main/resources/static/om/js/om-admin.js`
- Reference: `tcf-gateway/src/main/java/com/nh/nsight/gateway/entry/facade/GatewayRouteDispatcher.java`
- Reference: `tcf-web/src/main/java/com/nh/nsight/tcf/web/entry/web/TcfJwtAuthenticationFilter.java`
- Reference: `tcf-om/src/main/java/com/nh/nsight/om/entry/web/OmUpdownloadFileController.java`
- Reference: `tcf-eai/src/main/java/com/nh/nsight/tcf/eai/support/StandardRequestBuilder.java`
- Reference: `zdocs-1/architecture/46-service-integration-contract.md`
- Reference: `zdocs-1/architecture/51-api-gateway.md`

**Interfaces:**
- Consumes: Layer, lifecycle, and entry-path definitions from Task 1.
- Produces: Sections 8–20 covering Relay, Gateway, hybrid REST, conversion, routing, response/error contracts, security, timeout/idempotency, tcf-eai, asynchronous-extension boundaries, and a drift matrix.

- [ ] **Step 1: Locate actual Relay, Gateway, hybrid REST, and EAI files**

Run:

```powershell
rg --files tcf-ui tcf-gateway tcf-om tcf-eai | rg 'TransactionRelayService|TcfApiController|online-multi\.js|om-admin\.js|GatewayRouteDispatcher|TcfJwtAuthenticationFilter|Updownload|StandardRequestBuilder|Integration.*Client'
```

Expected: exact current paths are listed. If a planned reference moved, use the discovered path and record the path difference in the implementation report.

- [ ] **Step 2: Extract transformation and forwarding evidence**

Run:

```powershell
rg -n 'RestClient|body\(|header\(|Cookie|Authorization|Set-Cookie|responseBody|readTimeout|connectTimeout|StandardRequest|StandardResponse|resultCode' `
  'tcf-ui/src/main/java' 'tcf-ui/src/main/resources/static' `
  'tcf-gateway/src/main/java' 'tcf-om/src/main/java' 'tcf-eai/src/main/java'
```

Expected: evidence for transparent forwarding, allowed header/cookie propagation, target response wrapping, hybrid response construction, and EAI request/response handling.

- [ ] **Step 3: Write Relay and adaptation sections**

Add:

```markdown
## 8. tcf-ui Relay
## 9. API Gateway Relay
## 10. 하이브리드 REST·multipart
```

Classify UI and Gateway Relay as `대상 WAR에서 적용`; classify direct hybrid REST as `TCF 미적용` unless the actual controller calls `TcfGateway`. State exactly which request/response fields each Relay preserves, wraps, or forwards.

- [ ] **Step 4: Write serialization, enrichment, routing, and response sections**

Add:

```markdown
## 11. 직렬화·역직렬화
## 12. Header 보완·정규화·검증
## 13. Handler 라우팅과 Body 전달
## 14. 응답 메시지 조립
```

Document camelCase, `header + body`, `header + result + body`, Controller/STF mutation ownership, serviceId-only Dispatcher routing, and ETF response assembly. Link to the field dictionary rather than duplicating all Header and Result fields.

- [ ] **Step 5: Write error, security, timeout, integration, and extension sections**

Add:

```markdown
## 15. 오류·HTTP 상태 처리
## 16. Cookie·Authorization·보안
## 17. Timeout·재시도·멱등성
## 18. 서비스 간 tcf-eai 메시징
## 19. 비동기 메시징 확장 고려사항
```

Separate standard HTTP 200/resultCode behavior from MVC, JWT Filter, Gateway, Relay, and hybrid errors. Record current Cookie/Authorization forwarding and Set-Cookie reverse propagation only where source evidence exists. Describe Kafka/JMS only as a future extension contract.

- [ ] **Step 6: Write the implementation-alignment matrix**

Add:

```markdown
## 20. 구현 정합성 및 개선 권고
```

Use columns:

```text
항목 | 현재 구현 | 문서 또는 기대 | 영향 | 개선 권고
```

Include at least: `X-Forwarded-For` trust, `errorDetail` exposure, standard-vs-Gateway HTTP status, UI Relay wrapping, hybrid REST TCF bypass, duplicated client message composers, Relay timeout/retry, and absence of a core Kafka/JMS broker.

- [ ] **Step 7: Verify claims and security wording**

Run:

```powershell
$doc = 'ztcf-다이어리\2026-07-26-아키텍처-이것저것\2026-07-26-06-메시지처리방식.md'
rg -n '^## (8|9|10|11|12|13|14|15|16|17|18|19|20)\.' $doc
rg -n '현재 구현|개발 표준|개선 권고|대상 WAR에서 적용|TCF 미적용|HTTP 200|401|Cookie|Authorization|멱등|tcf-eai' $doc
rg -n -i 'password|access.?token|refresh.?token|session.?id|private.?key|stack trace|select \* from|jdbc:' $doc
git diff --check -- $doc
```

Expected: sections 8–20 exist; sensitive scan finds only prohibition or field-name discussion, never real secrets, SQL, or connection strings.

- [ ] **Step 8: Run focused module tests and commit**

Run:

```powershell
.\gradlew.bat :tcf-core:test :tcf-web:test :tcf-ui:test :tcf-eai:test --no-daemon
git add -- 'ztcf-다이어리/2026-07-26-아키텍처-이것저것/2026-07-26-06-메시지처리방식.md'
git diff --cached --check
git diff --cached --name-only
git commit -m "docs: explain relay and messaging contracts"
```

Expected: `BUILD SUCCESSFUL`; only the target Markdown is committed.

---

### Task 3: Add operational checklists, tests, links, and final verification

**Files:**
- Modify: `ztcf-다이어리/2026-07-26-아키텍처-이것저것/2026-07-26-06-메시지처리방식.md`
- Reference: `ztcf-다이어리/2026-07-26-아키텍처-이것저것/2026-07-26-04-전문구성.md`
- Reference: `ztcf-다이어리/2026-07-26-아키텍처-이것저것/2026-07-26-05-트랜잭션처리방식.md`
- Reference: `zdocs-1/architecture/04-messaging.md`
- Reference: `zdocs-1/architecture/43-security-operations.md`
- Reference: `zdocs-1/architecture/44-observability.md`
- Reference: `zdocs-1/architecture/46-service-integration-contract.md`
- Reference: `zdocs-1/architecture/50-test-architecture.md`
- Reference: `zdocs-1/architecture/51-api-gateway.md`

**Interfaces:**
- Consumes: Complete lifecycle, entry-path, conversion, error, security, timeout, and integration descriptions from Tasks 1–2.
- Produces: Sections 21–25 with reusable checklists, exactly MSG01–MSG22 scenarios, valid evidence links, concise principles, and final verification evidence.

- [ ] **Step 1: Add the new-message checklist**

Add:

```markdown
## 21. 신규 메시지 체크리스트
```

Include unchecked items for entry type and TCF classification, endpoint/Content-Type, ServiceId and transaction code, Header/body ownership, JSON compatibility, Relay mutation, authentication/cookies, errors/HTTP status, timeout, idempotency, logging/PII, sample request, service catalog/control, and contract tests.

- [ ] **Step 2: Add the message-change checklist**

Add:

```markdown
## 22. 메시지 변경 체크리스트
```

Include unchecked items for backward compatibility, field add/delete/type changes, requiredness, caller/Relay/Gateway/EAI consumers, old/new coexistence, response/error-code changes, timeout/retry, security propagation, rollback plan, and deployment order.

- [ ] **Step 3: Add exactly 22 test scenarios**

Add:

```markdown
## 23. 테스트 시나리오
```

Use columns:

```text
ID | 경로 | 입력·조건 | 기대 처리 | 기대 응답·증적
```

Define `MSG01` through `MSG22` exactly as: standard success, invalid JSON, unsupported Content-Type, required Header missing, GUID/TraceId generation, unknown ServiceId, business error, system error, JWT/Gateway 401, UI Relay success, Relay connection failure, Relay timeout, Cookie forwarding, Authorization forwarding, TcfGateway defaults, multipart/hybrid path, response serialization, idempotency duplicate, change timeout/retry prevention, tcf-eai success, tcf-eai business error, and tcf-eai timeout.

- [ ] **Step 4: Add evidence links and final principles**

Add:

```markdown
## 24. 근거 소스와 관련 문서
## 25. 핵심 원칙 요약
```

Use target-file-relative Markdown links to every source actually used. End with: keep transport/serialization/semantic/adaptation distinct; standard paths use resultCode; Relay is transparent; hybrid REST must declare missing TCF controls; service integration reuses the standard contract; asynchronous brokers are not current core features.

- [ ] **Step 5: Verify structure and scenario count**

Run:

```powershell
$doc = 'ztcf-다이어리\2026-07-26-아키텍처-이것저것\2026-07-26-06-메시지처리방식.md'
rg -n '^## ' $doc
rg -n '^- \[ \]' $doc
rg -n '^\| MSG(0[1-9]|1[0-9]|2[0-2]) ' $doc
git diff --check -- $doc
```

Expected: sections 1–25, both reusable checklists, exactly MSG01–MSG22, and no whitespace errors.

- [ ] **Step 6: Verify relative links**

Run:

```powershell
$doc = Resolve-Path 'ztcf-다이어리\2026-07-26-아키텍처-이것저것\2026-07-26-06-메시지처리방식.md'
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

- [ ] **Step 7: Run final focused tests and stage exact scope**

Run:

```powershell
.\gradlew.bat :tcf-core:test :tcf-web:test :tcf-ui:test :tcf-eai:test --no-daemon
git add -- 'ztcf-다이어리/2026-07-26-아키텍처-이것저것/2026-07-26-06-메시지처리방식.md'
git diff --cached --check
git diff --cached --name-only
```

Expected: Gradle reports `BUILD SUCCESSFUL`; staged output contains only `2026-07-26-06-메시지처리방식.md`.

- [ ] **Step 8: Commit and capture final evidence**

```powershell
git commit -m "docs: complete messaging processing guide"
git show --stat --oneline --summary HEAD
git status --short -- 'ztcf-다이어리/2026-07-26-아키텍처-이것저것/2026-07-26-06-메시지처리방식.md'
```

Expected: only the target guide is committed and its path is clean. Report unrelated pre-existing changes separately.
