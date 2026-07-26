# 14장. Timeout 관리 구조 — 설명

## 설계서 절 목차

14.1~14.3 개요 · 14.4~14.8 Timeout 유형 · 14.9~14.14 정책·Executor · 14.15~14.20 OM·MyBatis·Hikari · 14.21~14.26 오류·로그·운영

---

## 핵심 결론

| 테이블 | 역할 |
|--------|------|
| TCF_TRANSACTION_CONTROL | **허용** 여부 |
| TCF_SERVICE_TIMEOUT_POLICY | **몇 초** 안에 |

---

## Timeout 목적

- 장애 전파·Pool 고갈 방지  
- SLA (P95 3초)  
- TIMEOUT 로그·재처리 근거  

## Timeout 계층 (14.3)

```
Apache/Gateway     Proxy Timeout
OnlineTransaction  OnlineTransactionTimeoutExecutor
Spring @Transactional  transaction timeout
MyBatis            statement queryTimeout
HikariCP           connectionTimeout, maxLifetime
tcf-eai HTTP       client read timeout
```

## STF (14.x)

```
STF → TCF_SERVICE_TIMEOUT_POLICY (serviceId)
→ TimeoutContext → OnlineTransactionTimeoutExecutor
```

## Timeout 유형 (14.4~14.8)

| 유형 | 설정 위치 |
|------|-----------|
| Online | OM TimeoutPolicy + Executor |
| DB Tx | `@Transactional(timeout=)` Facade |
| Query | MyBatis mapper timeout |
| Connection | HikariCP |
| Session | spring.session (10장) |

## Timeout 발생 (14.x)

- ETF: `E-TCF-TIME-0001`, result FAIL, errorType TIMEOUT  
- TCF_TX_LOG: **TIMEOUT** (등록·연계 → **UNKNOWN** 가능)  
- retryable: true (정책)

## OM (14.15)

- `OmTimeoutPolicyHandler` — CRUD  
- `OM.TimeoutPolicy.inquiry/save/...`

## 코드베이스

- `OnlineTransactionTimeoutExecutor` — tcf-core  
- `JdbcTimeoutPolicyRepository`, `TimeoutPolicySchemaInitializer` — tcf-web  
- `application-tcf.yml`, `application-datasource.yml`

## 운영 체크리스트

- [ ] serviceId별 Online Timeout OM 등록  
- [ ] Query Timeout ≤ Online Timeout  
- [ ] Pool size vs Timeout 튜닝 (DBA)  

## 이전 · 다음

← [13장 거래통제](./13-거래통제.md) · [15장 로그](./15-거래로그-감사로그.md) →
