# 정상·오류·Timeout·장애 흐름

근거: A07 · A12 · `[실제 소스 확인]`

## 정상

```text
POST /online | /{businessCode}/online
  → OnlineTransactionController
  → TCF → STF
  → OnlineTransactionTimeoutExecutor
  → TransactionDispatcher
  → TransactionHandler → Facade → Service → Rule/Dao → Mapper
  → ETF
```

## 오류·Timeout

| 유형 | 처리 |
| --- | --- |
| 업무 오류 | ETF businessFail |
| 시스템 오류 | ETF systemError |
| Timeout | OnlineTransactionTimeoutExecutor |
| 멱등/중복 | 프레임 설정 · 세부 `[확인 필요]` |
| 부분장애 | WAR/DB 격리 · Runbook |

## 장애 Runbook (최소)

증상 → 확인(guid/traceId/serviceId) → 조치 → escalation  
OM Catalog/Timeout 미등록 = Gap Explicit
