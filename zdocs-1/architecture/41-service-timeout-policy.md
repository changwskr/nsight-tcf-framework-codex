# 41. 서비스별 Transaction Timeout 정책

| 항목 | 내용 |
|------|------|
| 문서 번호 | 41 |
| 제목 | Service Timeout Policy |
| 상위 문서 | [architecture.md](architecture.md), [08-timeout.md](08-timeout.md), [40-header-7-transaction-control.md](40-header-7-transaction-control.md) |
| 구현 모듈 | `tcf-core`, `tcf-web`, `tcf-om`, `tcf-ui` |

---

## 1. 결론

서비스별 Transaction Timeout은 **거래통제 테이블과 분리**한다.

```text
TCF_TRANSACTION_CONTROL     = 이 거래를 허용할 것인가?
TCF_SERVICE_TIMEOUT_POLICY  = 이 거래를 몇 초 안에 끝내야 하는가?
```

거래통제 7필드(`serviceId`, `transactionCode`, `businessCode`, `serviceName`, `user`, `channelId`, `branch`)는 허용/차단만 담당한다. Timeout은 `serviceId + transactionCode + businessCode` PK 정책 테이블에서 관리한다.

---

## 2. Timeout 종류

| 구분 | 의미 | 기본값(예) |
|------|------|-----------|
| Online Transaction Timeout | 서비스 전체 처리 | 5초 |
| Spring Transaction Timeout | DB 트랜잭션 유지 | 5초 |
| MyBatis Query Timeout | SQL 1건 실행 | 3초 |
| Hikari Connection Timeout | Connection 획득 대기 | 3초 |
| External API Timeout | 외부 연계 connect/read | 3초 / 5초 |

Hikari `connectionTimeout`은 SQL 실행 시간이 아니라 Pool에서 Connection을 빌리기 위해 기다리는 시간이다.

---

## 3. 적용 흐름

```text
Header 7필드
  → TCF_TRANSACTION_CONTROL (허용/차단)
  → TCF_SERVICE_TIMEOUT_POLICY (정책 조회)
  → STF: TimeoutContextHolder + TransactionContext 바인딩
  → Facade: PolicyDrivenTransactionAttributeSource (TX timeout AOP)
  → Online: OnlineTransactionTimeoutExecutor (onlineTimeoutSec)
  → MyBatis: PolicyDrivenQueryTimeoutInterceptor (dbQueryTimeoutSec)
  → ETF: Timeout 표준 오류 (E-TCF-TIME-001~006)
```

---

## 4. 테이블 `TCF_SERVICE_TIMEOUT_POLICY`

| 컬럼 | 설명 |
|------|------|
| SERVICE_ID, TRANSACTION_CODE, BUSINESS_CODE | PK (SERVICE_NAME은 PK 제외) |
| ONLINE_TIMEOUT_SEC | 온라인 전체 Timeout |
| TX_TIMEOUT_SEC | Spring Transaction Timeout |
| DB_QUERY_TIMEOUT_SEC | MyBatis Query Timeout |
| EXTERNAL_CONNECT_TIMEOUT_MS | 외부 연계 connect |
| EXTERNAL_READ_TIMEOUT_MS | 외부 연계 read |
| TIMEOUT_ACTION | FAIL / UNKNOWN / STATUS_CHECK |
| USE_YN, DESCRIPTION | 사용·설명 |

---

## 5. 처리 기준 요약

| 거래 유형 | Online/TX | DB Query | Timeout 처리 |
|-----------|-----------|----------|----------------|
| 조회 | 3~5초 | 3초 | FAIL |
| 등록·변경 | 5초 | 3초 | UNKNOWN 또는 STATUS_CHECK |
| 발송·외부연계 | 10초 / TX 5초 | 3초 | STATUS_CHECK |

---

## 6. 오류코드

| 코드 | 의미 |
|------|------|
| E-TCF-TIME-001 | 온라인 거래 Timeout |
| E-TCF-TIME-002 | Transaction Timeout |
| E-TCF-TIME-003 | DB Query Timeout |
| E-TCF-TIME-004 | DB Connection 획득 Timeout |
| E-TCF-TIME-005 | 외부 Connect Timeout |
| E-TCF-TIME-006 | 외부 Read Timeout |

---

## 7. 구현 (2026-06)

| 영역 | 경로 |
|------|------|
| Resolver/Service | `tcf-core/.../timeout/TimeoutPolicyResolver.java`, `TimeoutPolicyService.java` |
| STF 연동 | `STF.preProcess()` — 거래통제 다음 단계 |
| JDBC | `tcf-web/.../timeout/JdbcTimeoutPolicyRepository.java` |
| Schema | `TimeoutPolicySchemaInitializer` |
| 동적 TX | `PolicyDrivenTransactionAttributeSource` + `TcfTimeoutTransactionManagementConfiguration` |
| 온라인 timeout | `OnlineTransactionTimeoutExecutor` (`TCF` dispatch 구간) |
| MyBatis timeout | `PolicyDrivenQueryTimeoutInterceptor` + `ConfigurationCustomizer` |
| 프로그램 TX | `PolicyDrivenTransactionExecutor` (비-Facade·수동 호출용) |
| OM CRUD | `OM.TimeoutPolicy.inquiry/save/update/delete` |
| UI | `/ui/om/admin/timeout-policy.html` |
| 시드 | `TimeoutPolicySeedData` — `OM_SERVICE_CATALOG` 기준 MERGE |

---

## 8. 운영 원칙

1. 기본 온라인 Timeout 5초, RDW Query 3초.
2. 등록·변경 Timeout 시 FAIL 단독 처리 지양 — UNKNOWN/STATUS_CHECK 검토.
3. 정책 변경은 OM 화면 + 권한이력(`TIMEOUT_POLICY`) 기록.
4. 10초 초과 온라인 Timeout은 예외 승인 대상.

---

← [40-header-7-transaction-control.md](40-header-7-transaction-control.md) · [42-jwt.md](42-jwt.md) →
