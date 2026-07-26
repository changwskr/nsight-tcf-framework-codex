# 5장. TCF Framework 처리 구조 — 설명

## 설계서 절 목차

5.1~5.3 개요·흐름 · 5.4 단계별 · 5.5 STF · 5.6 Dispatcher · 5.7 Handler · 5.8 ETF · 5.9~5.10 정상/오류 · 5.11 Timeout · 5.12 거래상태 · 5.13~5.15 컴포넌트·원칙

---

## 핵심 결론

> 모든 온라인 거래 = **TCF.process()** 단일 진입  
> **STF → [TimeoutExecutor] → Dispatcher → Handler → ETF**

---

## 전체 흐름 (5.3)

```
Client → POST /{businessCode}/online
→ tcf-web (OnlineTransactionController, GlobalStandardExceptionHandler)
→ TCF.process()
   1. STF.preProcess()
   2. OnlineTransactionTimeoutExecutor → TransactionDispatcher
   3. Business Handler → Facade → Service → Rule → DAO
   4. ETF.postProcess()
→ StandardResponse JSON
```

## STF 전처리 (5.5)

| # | 항목 |
|---|------|
| 1~3 | StandardRequest, Header 필수, GUID/TraceId |
| 4 | MDC (로그 correlation) |
| 5~7 | 세션, 인증, 권한 |
| 8 | 거래통제 (13장) |
| 9 | Idempotency |
| 10 | Timeout 정책 조회 |
| 11~13 | 감사 대상, **TCF_TX_LOG PROCESSING**, 상태 저장 |

구현: `tcf-core` STF, `StandardHeaderValidator`

## Dispatcher (5.6)

- `header.serviceId` → Handler Registry
- 원칙: URL 라우팅 ❌, serviceId만 ✅
- `TransactionDispatcher` — `serviceIds()` 등록, 중복 기동 실패

## Business Handler (5.7)

```
Handler → Facade(@Transactional) → Service → Rule → DAO/Mapper
```

Handler는 **Adapter** — 검증·응답 조립 없음.

## ETF 후처리 (5.8)

- Result, errorCode, message (OM_ERROR_CODE)
- header echo (responseTime, elapsedTimeMs)
- 마스킹, TCF_TX_LOG UPDATE, OM_AUDIT_LOG, Metrics

## 오류 처리 (5.10)

| 유형 | 처리 |
|------|------|
| Header | STF → E-TCF-HDR-* |
| BusinessException | ETF businessFail |
| System | ETF systemError |
| Timeout | TIMEOUT + E-TCF-TIME-* |

## 거래상태 (5.12)

PROCESSING → SUCCESS | FAIL | TIMEOUT | **UNKNOWN**

## TCF 컴포넌트 책임 (5.13)

| 컴포넌트 | 책임 |
|----------|------|
| tcf-web | HTTP 수신·예외 |
| STF | 검증·통제·로그 시작 |
| TimeoutExecutor | 거래 전체 시간 |
| Dispatcher | Handler 선택 |
| Handler~DAO | 업무 |
| ETF | 응답·로그 종료 |

## 핵심 원칙 (5.14)

1. 업무 개발자는 Controller/STF/ETF **미구현**  
2. serviceId = 실행 Key  
3. 오류·응답 **표준화** (ETF)  

## 코드베이스

```
tcf-core/.../TCF.java
tcf-core/.../STF.java, ETF.java
tcf-core/.../TransactionDispatcher.java
tcf-core/.../OnlineTransactionTimeoutExecutor.java
tcf-web/.../OnlineTransactionController.java
```

## 관련 문서

- [06-표준전문](./06-표준전문구조.md) · [07-Dispatcher](./07-ServiceIdDispatcher.md) · [08-Handler](./08-업무Handler개발.md)

## 이전 · 다음

← [4장](./04-모듈구성.md) · [6장](./06-표준전문구조.md) →
