# 36. ETF (End Transaction Framework) 가이드

| 항목 | 내용 |
|------|------|
| 문서 번호 | 36 |
| 제목 | ETF — End Transaction Framework |
| 상위 문서 | [architecture.md](architecture.md) |
| 관련 문서 | [03-transaction.md](03-transaction.md), [05-exception.md](05-exception.md), [09-transaction log.md](09-transaction log.md), [31-autoconfiguration.md](31-autoconfiguration.md), [33-TCF.md](33-TCF.md), [34-STF.md](34-STF.md), [35-BTF.md](35-BTF.md), [37-transaction-log.md](37-transaction-log.md) |
| 구현 | `tcf-core/.../processor/ETF.java`, `tcf-web/.../logging/JdbcTransactionLogRepository.java` |
| 대상 | 프레임워크·업무·운영 개발자 |

---

## 1. ETF란?

**ETF(End Transaction Framework)** 는 TCF 파이프라인의 **후처리(End)** 단계이다.  
STF·BTF가 끝난 뒤 **표준 응답 조립**, **거래 종료 로그**, **감사·메트릭·멱등성 마킹**을 일괄 수행한다.

| 약어 | 풀네임 | 클래스 |
|------|--------|--------|
| STF | Standard Transaction **Front** | `STF` — 거래 **시작** 로그 |
| BTF | **Business** Transaction Framework | 업무 WAR — `@Transactional` 업무 DB |
| **ETF** | **E**nd Transaction Framework | `ETF` — 거래 **종료** 로그·응답 |

```text
TCF.process()
  ├─ STF.preProcess()              ← TX_START (SLF4J만, DB 없음) — [34-STF.md](34-STF.md)
  ├─ Dispatcher → BTF              ← Facade @Transactional — [35-BTF.md](35-BTF.md)
  └─ ETF.success / businessFail / systemError   ← 본 문서 (후처리)
```

**핵심:** ETF는 **Spring 업무 트랜잭션과 별개**로 동작한다. Facade에서 롤백이 발생해도 ETF는 **실패 거래 로그를 DB에 남긴다**.

---

## 2. TCF에서 ETF 호출 시점

**파일:** `tcf-core/.../processor/TCF.java`

| 경로 | ETF 메서드 | 트리거 |
|------|------------|--------|
| 정상 | `etf.success(request, body, context)` | Dispatcher·Handler·Facade 정상 반환 |
| 업무 오류 | `etf.businessFail(request, e, context)` | `BusinessException` |
| 시스템 오류 | `etf.systemError(request, e, context)` | 그 외 `Exception` |

```27:59:tcf-core/src/main/java/com/nh/nsight/tcf/core/processor/TCF.java
    public StandardResponse<Object> process(StandardRequest<Map<String, Object>> request) {
        TransactionContext context = null;
        try {
            logClientRequest(request);
            context = stf.preProcess(request);
            Object body = dispatcher.dispatch(request, context);
            StandardResponse<Object> response = etf.success(request, body, context);
            logClientResponse(response);
            return response;
        } catch (BusinessException e) {
            StandardResponse<Object> response = etf.businessFail(request, e, context);
            logClientResponse(response);
            return response;
        } catch (Exception e) {
            StandardResponse<Object> response = etf.systemError(request, e, context);
            logClientResponse(response);
            return response;
        } finally {
            TransactionContextHolder.clear();
            MDC.clear();
        }
    }
```

**순서 정리**

1. `logClientRequest` — 입출력 전문 로그 (요청 payload)
2. STF — `TransactionLogService.start()` (TX_START)
3. BTF — 업무 처리 + Spring TX
4. **ETF** — 종료 후처리 + `TransactionLogService.end()` (TX_END + DB)
5. `logClientResponse` — 입출력 전문 로그 (응답 payload)

입출력 전문 로그와 거래 요약 로그는 **목적·저장소가 다르다** — [09-transaction log.md](09-transaction log.md) §1.

---

## 3. ETF 세 가지 분기

**파일:** `tcf-core/.../processor/ETF.java`

세 분기 모두 `context != null`일 때 **동일한 4단계 후처리**를 수행한다.

```text
idempotencyChecker  →  transactionLogService.end  →  auditLogService  →  metricService
```

| 분기 | 멱등성 | `end()` resultCode | errorCode | 응답 |
|------|--------|-------------------|-----------|------|
| `success` | `markSuccess` | `S0000` | `null` | `StandardResponse.success` |
| `businessFail` | `markFail` | `E0001` | `e.getErrorCode()` | `StandardResponse.fail` (업무 메시지) |
| `systemError` | `markFail` | `E0001` | `ErrorCode.SYSTEM_ERROR` | `StandardResponse.fail` (고정 시스템 메시지) |

```36:86:tcf-core/src/main/java/com/nh/nsight/tcf/core/processor/ETF.java
    public StandardResponse<Object> success(...) {
        if (context != null) {
            idempotencyChecker.markSuccess(header);
            transactionLogService.end(context, "S0000", null);
            auditLogService.audit(context, "S0000");
            metricService.record(context, "S0000");
        }
        return StandardResponse.success(header, body);
    }

    public StandardResponse<Object> businessFail(...) {
        if (context != null) {
            idempotencyChecker.markFail(header);
            transactionLogService.end(context, "E0001", e.getErrorCode());
            auditLogService.audit(context, "E0001");
            metricService.record(context, "E0001");
        }
        return StandardResponse.fail(header, e.getErrorCode(), e.getMessage(), null);
    }

    public StandardResponse<Object> systemError(...) {
        log.error("TCF system error. serviceId={}", ...);
        if (context != null) {
            idempotencyChecker.markFail(header);
            transactionLogService.end(context, "E0001", ErrorCode.SYSTEM_ERROR);
            auditLogService.audit(context, "E0001");
            metricService.record(context, "E0001");
        }
        return StandardResponse.fail(header, ErrorCode.SYSTEM_ERROR, "시스템 오류가 발생했습니다.", ...);
    }
```

### 3.1 `context == null`인 경우

STF `preProcess()` **이전** 또는 **실패 직후**에는 `context`가 null일 수 있다.  
이때 ETF는 **로그·멱등성·감사·메트릭을 건너뛰고** Header만으로 `StandardResponse`만 반환한다.

### 3.2 STF 실패 vs BTF 실패

| 실패 위치 | context | ETF 분기 | DB 거래로그 |
|-----------|---------|----------|-------------|
| STF (Header 검증 등) | null | `businessFail` / `systemError` | **없음** (TX_START도 미호출) |
| BTF (Rule, Facade, DAO…) | 있음 | `businessFail` / `systemError` | **INSERT** (FAIL) |

---

## 4. 로그 종류와 ETF 역할

ETF가 담당하는 로그는 **거래 종료·운영 추적** 계열이다.

| 로그 | Logger / 저장 | ETF에서 호출 | Spring `@Transactional` |
|------|---------------|--------------|-------------------------|
| TX_START | `transaction.log` (INFO) | **아니오** — STF `start()` | **무관** (DB INSERT 없음) |
| TX_END | `transaction.log` (INFO) | **예** — `end()` | **무관** (별도 커밋) |
| TCF_TX_LOG | H2/DB INSERT | **예** — `end()` → `persist()` | **분리** (§5) |
| AUDIT | `audit.log` (INFO) | **예** — `audit()` | 무관 |
| TCF_METRIC | DEBUG | **예** — `record()` | 무관 |
| 입출력 전문 | `TcfConsoleLog` | **아니오** — TCF `logClientResponse` | 무관 |

---

## 5. 로그를 위한 트랜잭션 처리 (핵심)

이 절이 **업무 DB 트랜잭션과 거래로그 DB 적재를 어떻게 분리하는지** 설명한다.

### 5.1 두 종류의 “트랜잭션”

NSIGHT TCF에서 “트랜잭션”은 **두 층**으로 나뉜다.

```text
┌─ ① TCF 온라인 거래 (논리 1건) ─────────────────────────────────────┐
│  STF → BTF(Handler/Facade/Service/DAO) → ETF                        │
│  “HTTP JSON 1회”의 생명주기 — guid/traceId로 추적                      │
└─────────────────────────────────────────────────────────────────────┘

┌─ ② Spring DB 트랜잭션 (물리 ACID) ──────────────────────────────────┐
│  Facade @Transactional — 업무 테이블 INSERT/UPDATE/SELECT            │
│  auto-commit: false (일반 업무 DS)                                   │
└─────────────────────────────────────────────────────────────────────┘

┌─ ③ 거래로그 DB INSERT (물리, 독립) ─────────────────────────────────┐
│  ETF → TransactionLogService.end → JdbcTransactionLogRepository     │
│  connection.setAutoCommit(true) — 업무 TX와 무관하게 즉시 커밋         │
└─────────────────────────────────────────────────────────────────────┘
```

- **② 업무 TX:** BTF Facade — [32-AOP.md](32-AOP.md), [03-transaction.md](03-transaction.md) §12
- **③ 거래로그 TX:** ETF 종료 시점 — **Spring `@Transactional` 미사용**, JDBC 직접 제어

### 5.2 STF `start()` vs ETF `end()`

**파일:** `tcf-core/.../logging/TransactionLogService.java`

| 메서드 | 호출 | SLF4J | DB INSERT |
|--------|------|-------|-----------|
| `start(context)` | STF | `TX_START guid=...` | **없음** |
| `end(context, resultCode, errorCode)` | ETF | `TX_END ... elapsedMs=...` | **있음** (`persist`) |

```26:58:tcf-core/src/main/java/com/nh/nsight/tcf/core/logging/TransactionLogService.java
    public void start(TransactionContext context) {
        log.info("TX_START guid={} traceId={} serviceId={} ...", ...);
        context.put("txLogStarted", Boolean.TRUE);
    }

    public void end(TransactionContext context, String resultCode, String errorCode) {
        log.info("TX_END guid={} ... resultCode={} errorCode={} elapsedMs={}", ...);
        persist(context, resultCode, errorCode);
    }

    private void persist(...) {
        if (!properties.isTransactionLogEnabled()) return;
        TransactionLogRepository repository = repositoryProvider.getIfAvailable();
        if (repository == null) { log.warn(...); return; }
        try {
            repository.save(toRecord(...));
        } catch (Exception e) {
            log.warn("Failed to persist transaction log...", e);  // 거래 응답은 계속
        }
    }
```

**설계 의도**

1. **시작**은 가볍게 — SLF4J만으로 진입 추적
2. **종료**에만 DB 1행 — 결과·소요시간이 확정된 뒤 적재
3. `persist` 실패는 **WARN** — 업무 응답·롤백 흐름을 **막지 않음**

### 5.3 독립 커밋 (`JdbcTransactionLogRepository`)

**파일:** `tcf-web/.../logging/JdbcTransactionLogRepository.java`

업무 DataSource가 `auto-commit: false`이고 Facade TX가 열려 있어도, 거래로그 INSERT는 **별도 auto-commit**으로 즉시 확정한다.

```32:47:tcf-web/src/main/java/com/nh/nsight/tcf/web/logging/JdbcTransactionLogRepository.java
    public void save(TransactionLogRecord record) {
        jdbcTemplate.execute((java.sql.Connection connection) -> {
            boolean previousAutoCommit = connection.getAutoCommit();
            try {
                connection.setAutoCommit(true);
                try (PreparedStatement ps = connection.prepareStatement(insertSql)) {
                    bind(ps, record);
                    ps.executeUpdate();
                }
            } finally {
                connection.setAutoCommit(previousAutoCommit);
            }
            return null;
        });
    }
```

```text
시나리오: Facade @Transactional에서 BusinessException → 롤백

  업무 테이블 변경     ──► ROLLBACK (취소)
  TCF_TX_LOG INSERT   ──► COMMIT   (ETF에서 이미 확정)

  → “실패한 거래”도 OM·장애 분석에서 조회 가능
```

**`separate: true` (기본, *-service)**  
거래로그 전용 DataSource `transactionLogDataSource` — 업무 DS와 **물리 분리**.

**`separate: false` (tcf-om)**  
Primary `JdbcTemplate` 공유 — **논리 테이블만** `TCF_TX_LOG`. INSERT 시에도 **auto-commit true**로 업무 TX와 분리.

### 5.4 실패 경로에서의 로그 보존

```text
BTF: Facade @Transactional
       │
       ├─ Service/DAO에서 BusinessException
       │     → Spring TX rollback (업무 데이터 미반영)
       │
       └─ 예외 전파 → TCF catch → ETF.businessFail()
             → transactionLogService.end(E0001, 업무errorCode)
             → TCF_TX_LOG INSERT (RESULT_STATUS=FAIL)
```

시스템 오류(`systemError`)도 동일 — `errorCode = E-COM-SYS-0001`, stack trace는 SLF4J ERROR.

### 5.5 `@Transactional`을 ETF에 두지 않는 이유

| ETF에 `@Transactional`을 쓰면 | 현재 설계 |
|------------------------------|-----------|
| 업무 TX와 같은 Connection/TX에 묶일 위험 | JDBC 레벨 독립 커밋 |
| 롤백 시 거래로그까지 함께 롤백 | 실패 이력 **항상** 보존 |
| 로그 persist 실패가 응답 전파 | try/catch WARN, 응답 정상 반환 |

ETF·`TransactionLogService`에는 **`@Transactional` 없음** — [32-AOP.md](32-AOP.md) 범위는 BTF Facade뿐.

---

## 6. TCF_TX_LOG 적재 상세

### 6.1 레코드 필드

**파일:** `TransactionLogRecord` — ETF `end()` 시 `toRecord()`로 생성

| 컬럼 | 소스 |
|------|------|
| `LOG_ID` | 새 GUID |
| `TX_TIME` | `DateTimeUtil.nowKst()` (종료 시각) |
| `BUSINESS_CODE`, `SERVICE_ID`, `TRANSACTION_CODE` | Header |
| `GUID`, `TRACE_ID`, `USER_ID`, `BRANCH_ID` | Header |
| `RESULT_STATUS` | `S0000` → `SUCCESS`, 그 외 `FAIL` |
| `RESULT_CODE` | `S0000` / `E0001` |
| `ERROR_CODE` | 성공 `null`, 실패 업무/시스템 코드 |
| `ELAPSED_TIME_MS` | `context.elapsedMillis()` (STF 시작~ETF 종료) |

### 6.2 스키마·AutoConfiguration

| 구성 | 클래스 | 조건 |
|------|--------|------|
| 거래로그 DS (분리) | `TcfTransactionLogDataSourceConfiguration` | `transaction-log-enabled=true`, `separate=true` |
| Repository 빈 | `TcfTransactionLogConfiguration` | 위 + `JdbcTransactionLogRepository` |
| DDL | `TransactionLogSchemaInitializer` | `transaction-log-schema-auto-init=true` |

기본 H2 경로: `data/nsight-txlog/nsight_om` — OM 조회와 **동일 테이블** 공유.

### 6.3 설정 (`nsight.tcf`)

```yaml
nsight:
  tcf:
    transaction-log-enabled: true          # false면 persist skip
    audit-enabled: true
    transaction-log-schema-auto-init: true
    transaction-log-table-name: TCF_TX_LOG
    transaction-log-datasource:
      separate: true                       # *-service 기본
      url: jdbc:h2:file:${nsight.txlog.path}/nsight_om;...
```

| 모듈 | `transaction-log-enabled` | `separate` |
|------|---------------------------|------------|
| `*-service` | `true` | `true` (별도 DS) |
| `tcf-om` | `true` | `false` (Primary DS, schema.sql) |
| `tcf-batch` | `false` | — |

상세: [31-autoconfiguration.md](31-autoconfiguration.md), [03-transaction.md](03-transaction.md) §13.

---

## 7. 경로별 전체 시퀀스

### 7.1 성공

```text
logClientRequest
→ STF: TX_START (SLF4J)
→ BTF: Facade @Transactional commit
→ ETF.success
     markSuccess
     TX_END + TCF_TX_LOG (SUCCESS, S0000)
     AUDIT + METRIC
→ logClientResponse
→ finally: Context/MDC clear
```

### 7.2 업무 실패 (롤백 + 로그 보존)

```text
logClientRequest
→ STF: TX_START
→ BTF: BusinessException → Facade TX rollback
→ ETF.businessFail
     markFail
     TX_END + TCF_TX_LOG (FAIL, E0001, 업무errorCode)
→ logClientResponse (fail payload)
```

### 7.3 시스템 오류

```text
→ BTF: NPE/SQL 등 → Facade TX rollback (있었다면)
→ ETF.systemError
     log.error(stack)
     TX_END + TCF_TX_LOG (FAIL, E0001, E-COM-SYS-0001)
→ logClientResponse
```

---

## 8. STF · BTF · ETF 대칭

```text
         STF                    BTF                      ETF
         ───                    ───                      ───
역할     전처리(Front)          업무(Business)           후처리(End)
구현     tcf-core/STF.java      업무 WAR                 tcf-core/ETF.java
Context  생성·Holder set        read/write               read (종료 기록)
로그     TX_START (SLF4J)       (업무 로그는 각 Service)  TX_END + DB + AUDIT
TX       없음                   @Transactional (Facade)  JDBC 독립 커밋
실패     context null 가능      Exception → TCF catch    success/fail/error 분기
```

---

## 9. 운영·OM 연계

`tcf-om`은 `TCF_TX_LOG`를 조회해 거래 이력·통계·장애 분석에 사용한다.

- 성공/실패 건수, 평균 `ELAPSED_TIME_MS`, `serviceId`/`errorCode` Top N
- ETF가 **실패 시에도 INSERT**하므로 “롤백된 거래”도 OM에서 추적 가능

입출력 전문은 DB가 아닌 **애플리케이션 로그** — [09-transaction log.md](09-transaction log.md).

---

## 10. 개발 시 체크리스트

1. **업무 롤백 ≠ 거래로그 없음** — 실패해도 `TCF_TX_LOG`에 FAIL 행이 있어야 정상.
2. **Facade에 로그 INSERT 넣지 않기** — 거래 요약은 ETF·`TransactionLogService` 전담.
3. **`transaction-log-enabled: false`** — SLF4J TX_END만, DB skip (batch 등).
4. **STF 전 실패** — context null → DB 거래로그 없음 (Header 검증 실패 등).
5. **persist 예외** — WARN만; 클라이언트 응답은 ETF가 이미 만든 `StandardResponse` 유지.

---

## 11. 관련 소스 인덱스

| 클래스 | 모듈 | 역할 |
|--------|------|------|
| `ETF` | tcf-core | 후처리 오케스트레이션 |
| `TransactionLogService` | tcf-core | start/end, persist |
| `TransactionLogRepository` | tcf-core (SPI) | save 인터페이스 |
| `JdbcTransactionLogRepository` | tcf-web | INSERT + auto-commit |
| `TcfTransactionLogConfiguration` | tcf-web | Repository 빈 |
| `TcfTransactionLogDataSourceConfiguration` | tcf-web | 별도 DS |
| `TransactionLogSchemaInitializer` | tcf-web | DDL |
| `AuditLogService` | tcf-core | audit.log |
| `TransactionMetricService` | tcf-core | DEBUG 메트릭 |
| `InMemoryIdempotencyChecker` | tcf-core | markSuccess/markFail |

---

## 12. 참고 문서

- 거래로그 아키텍처 전체: [09-transaction log.md](09-transaction log.md)
- Spring 업무 TX: [03-transaction.md](03-transaction.md) §12
- AutoConfiguration: [31-autoconfiguration.md](31-autoconfiguration.md)
- TCF 엔진 흐름: [33-TCF.md](33-TCF.md)
