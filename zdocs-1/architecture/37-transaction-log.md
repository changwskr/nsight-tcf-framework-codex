# 37. 트랜잭션 로그 처리 가이드

| 항목 | 내용 |
|------|------|
| 문서 번호 | 37 |
| 제목 | Transaction Log — 기록 방법·흐름·설정 |
| 상위 문서 | [architecture.md](architecture.md) |
| 관련 문서 | [03-transaction.md](03-transaction.md), [09-transaction log.md](09-transaction log.md), [31-autoconfiguration.md](31-autoconfiguration.md), [34-STF.md](34-STF.md), [36-ETF.md](36-ETF.md) |
| 구현 | `tcf-core`, `tcf-web`, `tcf-om` |
| 대상 | 프레임워크·업무·운영 개발자 |

---

## 1. 개요 — 무엇을, 어디에 남기는가

NSIGHT TCF는 **한 번의 온라인 거래**에 대해 로그를 **4채널**로 나눠 기록한다.

| # | 로그 | 호출 주체 | Logger / 저장 | DB INSERT |
|---|------|-----------|---------------|-----------|
| 1 | **입출력 전문** | `TCF` | `TcfConsoleLog` (콘솔·앱 로그) | 없음 |
| 2 | **거래 시작** | `STF` | SLF4J `transaction.log` (`TX_START`) | 없음 |
| 3 | **거래 종료 + 요약** | `ETF` | `transaction.log` (`TX_END`) + **`TCF_TX_LOG`** | **ETF에서 1행** |
| 4 | **감사** | `ETF` | SLF4J `audit.log` (`AUDIT`) | 없음 |

```text
                    ┌─────────────────────────────────────────┐
  HTTP JSON 1건     │  TCF.process()                          │
                    │                                         │
  ① 전문(요청)  ◄───┤  logClientRequest()                     │
  ② TX_START    ◄───┤  STF → TransactionLogService.start()    │
                    │  BTF (Facade @Transactional — 업무 DB)   │
  ③ TX_END+DB   ◄───┤  ETF → TransactionLogService.end()       │
  ④ AUDIT       ◄───┤  ETF → AuditLogService.audit()          │
  ① 전문(응답)  ◄───┤  logClientResponse()                     │
                    └─────────────────────────────────────────┘
```

**업무 개발자가 직접 거래로그 INSERT를 하지 않는다.**  
`TransactionLogService` + `JdbcTransactionLogRepository`가 프레임워크에서 자동 처리한다.

입출력 전문·운영 플레이북 등 배경: [09-transaction log.md](09-transaction log.md).  
ETF 후처리·독립 커밋 상세: [36-ETF.md](36-ETF.md) §5.

---

## 2. 한 줄 타임라인 (성공 기준)

```text
시각 ─────────────────────────────────────────────────────────────►

TCF          logClientRequest (header+body 전문)
STF          validate → guid/traceId → MDC → TX_START (SLF4J)
BTF          Handler → Facade @Transactional → Service → DAO
ETF          markSuccess → TX_END → TCF_TX_LOG INSERT → AUDIT → StandardResponse
TCF          logClientResponse (header+result+body 전문)
finally      ContextHolder.clear(), MDC.clear()
```

실패 시에도 **③ ETF `end()` + DB INSERT**는 동일 — resultCode `E0001`, `RESULT_STATUS=FAIL`.

---

## 3. 채널별 기록 방법

### 3.1 입출력 전문 로그 (`TCF`)

**파일:** `tcf-core/.../processor/TCF.java`

| 메서드 | 시점 | 내용 |
|--------|------|------|
| `logClientRequest` | `STF` **이전** | 요청 Header 전 필드 + Body Map |
| `logClientResponse` | `ETF` **이후** | 응답 Header, Result, Body |

- 출력 경로: `TcfConsoleLog` — logback `logback-tcf-console.xml` 포함
- **DB와 무관** — 디버깅·장애 1차 분석용
- Body는 **마스킹 없이** 그대로 출력 → 운영 시 마스킹 정책 필요 ([09-transaction log.md](09-transaction log.md) §10)

### 3.2 TX_START — 거래 시작 (`STF`)

**파일:** `tcf-core/.../processor/STF.java` → `TransactionLogService.start()`

호출 순서 (STF `preProcess` 마지막):

```text
headerValidator → guid/traceId → Context 생성 → MDC → session → auth → idempotency
→ transactionLogService.start(context)
```

**로그 포맷** (logger `transaction.log`, INFO):

```text
TX_START guid=... traceId=... serviceId=... txCode=... userId=... branchId=... channelId=...
```

| 특징 | 설명 |
|------|------|
| DB | **INSERT 없음** — SLF4J만 |
| Context | `context.put("txLogStarted", true)` |
| MDC | `guid`, `traceId`, `serviceId`, `userId`, `branchId` — logback 패턴에 `%X{guid}` 등 반영 |

**STF 실패 시:** `start()` 미호출 → `TX_START`·`TCF_TX_LOG` 모두 없을 수 있음.

### 3.3 TX_END + DB — 거래 종료 (`ETF`)

**파일:** `tcf-core/.../processor/ETF.java` → `TransactionLogService.end()`

세 ETF 분기 모두 `transactionLogService.end(context, resultCode, errorCode)` 호출:

| ETF 분기 | resultCode | errorCode | RESULT_STATUS |
|----------|------------|-----------|---------------|
| `success` | `S0000` | `null` | `SUCCESS` |
| `businessFail` | `E0001` | 업무 코드 | `FAIL` |
| `systemError` | `E0001` | `E-COM-SYS-0001` | `FAIL` |

**SLF4J** (`TX_END`):

```text
TX_END guid=... traceId=... serviceId=... resultCode=... errorCode=... elapsedMs=...
```

`elapsedMs` = `TransactionContext.elapsedMillis()` — STF에서 Context 생성 시각 ~ ETF 종료.

**DB persist** (`TransactionLogService.persist()`):

1. `nsight.tcf.transaction-log-enabled == true` (기본 true)
2. `TransactionLogRepository` 빈 존재
3. `repository.save(toRecord(...))` — 실패 시 WARN, **응답은 유지**

### 3.4 감사 로그 (`ETF`)

**파일:** `tcf-core/.../logging/AuditLogService.java`

- Logger: `audit.log`
- 설정: `nsight.tcf.audit-enabled` (기본 true)
- 포맷: `AUDIT guid=... serviceId=... txCode=... userId=... branchId=... resultCode=...`
- DB 없음 — 규정 준수·사용자 행위 추적용 SLF4J 전용

---

## 4. `TCF_TX_LOG` 테이블 적재

### 4.1 레코드 생성

**파일:** `TransactionLogService.toRecord()` → `TransactionLogRecord`

| 컬럼 | 값 |
|------|-----|
| `LOG_ID` | 새 GUID (`GuidGenerator.newGuid()`) |
| `TX_TIME` | `DateTimeUtil.nowKst()` — **종료 시각** |
| `BUSINESS_CODE`, `SERVICE_ID`, `TRANSACTION_CODE` | Header |
| `GUID`, `TRACE_ID`, `USER_ID`, `BRANCH_ID` | Header |
| `RESULT_STATUS` | `S0000` → `SUCCESS`, 그 외 `FAIL` |
| `RESULT_CODE` | `S0000` / `E0001` |
| `ERROR_CODE` | 성공 null, 실패 시 업무/시스템 코드 |
| `ELAPSED_TIME_MS` | Context 경과 ms |

### 4.2 INSERT 구현

**SPI:** `tcf-core/.../logging/TransactionLogRepository.java`

```java
void save(TransactionLogRecord record);
```

**구현:** `tcf-web/.../logging/JdbcTransactionLogRepository.java`

- PreparedStatement INSERT
- **`connection.setAutoCommit(true)`** — 업무 `@Transactional`과 **분리 커밋**
- Facade 롤백 후에도 FAIL 행은 **남음**

### 4.3 DDL·인덱스

**파일:** `tcf-web/.../logging/TransactionLogSchemaInitializer.java`

- `@PostConstruct` — `CREATE TABLE IF NOT EXISTS TCF_TX_LOG`
- 인덱스: `(GUID, TX_TIME DESC)`, `(SERVICE_ID, TX_TIME DESC)`
- 조건: `transaction-log-schema-auto-init=true` (로컬/dev 기본 true, prod often false)

**tcf-om:** `schema.sql` + `separate: false` — Primary DS, auto-init false.

### 4.4 물리 DB 위치

| 상수 | 값 |
|------|-----|
| 테이블 | `TCF_TX_LOG` (`TcfTransactionLogConstants.TABLE_NAME`) |
| 기본 H2 URL | `jdbc:h2:file:${nsight.txlog.path}/nsight_om;...` |
| 경로 자동 설정 | `NsightTxlogPathEnvironmentPostProcessor` → 프로젝트 `data/nsight-txlog` |

업무 WAR(`*-service`)와 OM이 **동일 H2 파일**을 공유 — 업무에서 적재, OM에서 조회.

---

## 5. Spring 업무 TX와의 관계

거래로그는 **Facade `@Transactional` 바깥**에서 기록된다.

```text
┌─ BTF Facade @Transactional ─────────────┐
│  업무 테이블 변경                        │
│  BusinessException → ROLLBACK           │
└─────────────────────────────────────────┘
                    │
                    ▼ (예외 전파)
┌─ ETF TransactionLogService.end ─────────┐
│  JDBC auto-commit INSERT → TCF_TX_LOG   │  ← 롤백과 무관
└─────────────────────────────────────────┘
```

| 구분 | Spring 업무 TX | 거래로그 INSERT |
|------|----------------|-----------------|
| 시작 | Facade 메서드 진입 | ETF `end()` |
| 종료 | commit / rollback | 항상 auto-commit |
| DataSource | `spring.datasource` | `transactionLogDataSource` (separate=true) |
| `@Transactional` | Facade | **없음** |

상세: [03-transaction.md](03-transaction.md) §12, [36-ETF.md](36-ETF.md) §5.

---

## 6. AutoConfiguration·빈 등록

```text
transaction-log-enabled=true (기본)
    │
    ├─ TcfTransactionLogDataSourceConfiguration
    │     separate=true (기본) → transactionLogDataSource + transactionLogJdbcTemplate
    │
    └─ TcfTransactionLogConfiguration
          → JdbcTransactionLogRepository (TransactionLogRepository)
          → TransactionLogSchemaInitializer (schema-auto-init=true)
```

| 빈 | 모듈 | 역할 |
|----|------|------|
| `TransactionLogService` | tcf-core | start/end, persist 오케스트레이션 |
| `JdbcTransactionLogRepository` | tcf-web | INSERT |
| `TransactionLogSchemaInitializer` | tcf-web | DDL |
| `transactionLogDataSource` | tcf-web | 별도 H2 (separate=true) |

`transaction-log-enabled=false` → Repository·DS 빈 **미등록** → `persist()` skip.

상세: [31-autoconfiguration.md](31-autoconfiguration.md).

---

## 7. 설정 (`nsight.tcf`)

```yaml
nsight:
  txlog.path: ./data/nsight-txlog    # 선택 — 미설정 시 PostProcessor 자동
  tcf:
    transaction-log-enabled: true
    audit-enabled: true
    transaction-log-schema-auto-init: true
    transaction-log-table-name: TCF_TX_LOG
    transaction-log-datasource:
      separate: true                 # *-service 기본
      url: jdbc:h2:file:${nsight.txlog.path}/nsight_om;MODE=Oracle;AUTO_SERVER=TRUE;DATABASE_TO_UPPER=false
      username: sa
      password:
      driver-class-name: org.h2.Driver
```

| 모듈 | transaction-log-enabled | separate | schema-auto-init |
|------|-------------------------|----------|------------------|
| `*-service` | true | true (별도 DS) | true (local/dev) |
| `tcf-om` | true | **false** (Primary DS) | false (`schema.sql`) |
| `tcf-batch` | **false** | — | — |

**로컬 예** (`sv-service/application-local.yml`): 업무는 H2 in-memory, 거래로그만 file H2.

---

## 8. Logback 출력

**파일:** `tcf-web/src/main/resources/logback-nsight-base.xml`

```xml
<logger name="transaction.log" level="INFO" additivity="false">
    <appender-ref ref="CONSOLE"/>
    <appender-ref ref="NSIGHT_FILE"/>
</logger>
<logger name="audit.log" level="INFO" additivity="false">
    ...
</logger>
```

공통 패턴에 MDC 포함:

```text
%d ... guid=%X{guid} traceId=%X{traceId} serviceId=%X{serviceId} ...
```

→ `TX_START`/`TX_END`/`AUDIT`와 root 로그를 **guid/traceId**로 상관 추적.

---

## 9. 경로별 로그 시퀀스

### 9.1 성공

```text
logClientRequest
→ STF: MDC + TX_START
→ BTF: Facade commit
→ ETF: markSuccess → TX_END → INSERT(SUCCESS,S0000) → AUDIT
→ logClientResponse
```

### 9.2 업무 실패 (롤백 + 로그 보존)

```text
logClientRequest → TX_START
→ BTF: BusinessException → Facade ROLLBACK
→ ETF.businessFail: markFail → TX_END(E0001,업무코드) → INSERT(FAIL)
→ logClientResponse (fail JSON)
```

### 9.3 시스템 오류

```text
→ Exception → ETF.systemError
→ log.error(stack) → TX_END(E0001,E-COM-SYS-0001) → INSERT(FAIL)
```

### 9.4 STF 이전/직후 실패

```text
→ context == null
→ ETF: end/audit/metric skip
→ logClientResponse만 (가능하면)
→ TCF_TX_LOG 없음
```

---

## 10. OM에서의 조회 (적재의 반대편)

**파일:** `tcf-om/.../mapper/om/OmOperationMapper.xml`

| SQL ID | 용도 |
|--------|------|
| `searchTransactionLogs` | guid/traceId/serviceId 등 조건 페이징 조회 |
| `summarizeTransactionLogs` | success/error/timeout/avgElapsed 집계 |
| `selectTxSummary` | 대시보드 일별 요약 |
| `selectErrorTop` | serviceId+errorCode Top N |
| `selectSlowTransactionsTop` | ELAPSED_TIME_MS 상위 |

OM Facade: `OmTransactionLogFacade` → `OmTransactionLogService`.

**전제:** 업무 WAR가 `separate=true`로 **같은 H2 파일**에 INSERT해야 OM 화면에 보임.

---

## 11. 검증 (통합 테스트)

**파일:** `sv-service/.../SvTransactionLogIntegrationTest.java`

| 테스트 | 검증 |
|--------|------|
| `transactionLogRepositoryIsRegistered` | `TransactionLogRepository` 빈 존재 |
| `transactionLogTableExistsAfterStartup` | `TCF_TX_LOG` 테이블 생성 |
| `onlineCallPersistsTransactionLog` | POST `/online` 후 row count 증가 |

로컬: `./gradlew :sv-service:test --tests SvTransactionLogIntegrationTest`

---

## 12. 개발·운영 체크리스트

### 12.1 프레임워크 (자동 — 확인만)

- [ ] `transaction-log-enabled=true` (*-service, tcf-om)
- [ ] `TransactionLogRepository` 빈 주입 (WARN "not available" 없음)
- [ ] `data/nsight-txlog/nsight_om` H2 파일 생성·쓰기 가능
- [ ] `TX_START` / `TX_END`가 `transaction.log`에 쌍으로 출력

### 12.2 업무 개발자 (하지 말 것 / 할 것)

| 하지 말 것 | 할 것 |
|------------|-------|
| Facade/Service에서 `TCF_TX_LOG` 직접 INSERT | `BusinessException`으로 실패 전달 — ETF가 FAIL 로그 |
| Handler에서 예외 catch 후 swallow | 예외를 TCF까지 전파 |
| 거래로그 DS를 업무 DS와 혼동 | 업무 TX는 `spring.datasource`만 사용 |

### 12.3 장애 추적 순서

1. 응답 JSON의 `guid` / `traceId` 확보
2. `transaction.log`에서 `TX_START` → `TX_END` 확인 (resultCode, elapsedMs)
3. 앱 로그에서 `logClientRequest` / `logClientResponse` 전문 비교
4. H2/DB `TCF_TX_LOG` WHERE `GUID = ?`
5. OM 거래로그 화면 또는 `OmOperationMapper.searchTransactionLogs`

---

## 13. 클래스·파일 인덱스

| 클래스 | 모듈 | 역할 |
|--------|------|------|
| `TCF` | tcf-core | 입출력 전문 로그 |
| `STF` | tcf-core | TX_START, MDC |
| `ETF` | tcf-core | TX_END 트리거, audit |
| `TransactionLogService` | tcf-core | start / end / persist |
| `TransactionLogRecord` | tcf-core | DB row DTO |
| `TransactionLogRepository` | tcf-core | SPI |
| `JdbcTransactionLogRepository` | tcf-web | INSERT + auto-commit |
| `TransactionLogSchemaInitializer` | tcf-web | DDL |
| `TcfTransactionLogConfiguration` | tcf-web | 빈 등록 |
| `TcfTransactionLogDataSourceConfiguration` | tcf-web | 별도 DS |
| `NsightTxlogPathEnvironmentPostProcessor` | tcf-core | `nsight.txlog.path` |
| `TcfTransactionLogConstants` | tcf-core | 테이블명·기본 URL |
| `AuditLogService` | tcf-core | audit.log |
| `TransactionContext` | tcf-core | elapsedMillis |
| `OmOperationMapper.xml` | tcf-om | 조회 SQL |

---

## 14. 문서 관계

| 문서 | 초점 |
|------|------|
| **본 문서 (37)** | **어떻게** 남기는지 — 호출 순서·설정·코드·체크리스트 |
| [09-transaction log.md](09-transaction log.md) | 입출력 vs 거래요약 이중 구조·운영 원칙·플레이북 |
| [36-ETF.md](36-ETF.md) | ETF 후처리·독립 커밋·STF/BTF 대칭 |
| [34-STF.md](34-STF.md) | STF 전처리·TX_START 위치 |
| [03-transaction.md](03-transaction.md) §10·§12 | TCF 로그 vs Spring TX |
