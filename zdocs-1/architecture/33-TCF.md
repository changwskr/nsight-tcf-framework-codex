# 33. TCF 엔진 가이드

| 항목 | 내용 |
|------|------|
| 문서 번호 | 33 |
| 제목 | TCF (Transaction Control Framework) Engine Guide |
| 상위 문서 | [architecture.md](architecture.md) |
| 관련 문서 | [02-junmun.md](02-junmun.md), [03-transaction.md](03-transaction.md), [05-exception.md](05-exception.md), [09-transaction log.md](09-transaction log.md), [14-online-arc.md](14-online-arc.md), [29-facade.md](29-facade.md), [34-STF.md](34-STF.md), [35-BTF.md](35-BTF.md), [36-ETF.md](36-ETF.md), [28-tcf-framework-ref.md](28-tcf-framework-ref.md) |
| 구현 모듈 | `tcf-core` (엔진), `tcf-web` (HTTP 진입·거래로그 SPI 구현) |
| 대상 | 프레임워크·업무 Handler 개발자 |

---

## 1. TCF란?

**TCF(Transaction Control Framework)** 는 NSIGHT 온라인 거래 **1건(HTTP JSON 1회)** 의 처리를 **단일 파이프라인**으로 통제하는 프레임워크 엔진이다.

| 역할 | 설명 |
|------|------|
| **표준 전문** | `StandardRequest` / `StandardResponse` 계약 |
| **전처리 (STF)** | Header 검증, GUID·추적 ID, 세션·권한·멱등성, 거래 시작 로그 |
| **업무 (BTF)** | Handler → Facade → Service → Rule → DAO → Mapper ([35-BTF.md](35-BTF.md)) |
| **라우팅** | `serviceId` → `TransactionHandler` (TCF ↔ BTF 경계) |
| **후처리 (ETF)** | 결과 코드 매핑, 거래 종료 로그, 감사·메트릭 |
| **업무 확장** | Handler SPI — BTF 내부 Facade/Service 구현 |

업무 개발자는 **Controller를 만들지 않고** `TransactionHandler` + `serviceId` 등록으로 거래를 추가한다.

```text
[ tcf-web ]  OnlineTransactionController / TcfGateway
       │  StandardRequest
       ▼
[ tcf-core ]  TCF.process()
       STF → TransactionDispatcher → [ BTF: Handler → Facade → Service → Rule → DAO → Mapper ] → ETF
       ▼
  StandardResponse
```

**DB 트랜잭션(Spring `@Transactional`)** 은 **BTF Facade**에서 동작 — [32-AOP.md](32-AOP.md), [35-BTF.md](35-BTF.md), [03-transaction.md](03-transaction.md) §12.

---

## 2. 모듈·패키지 맵

### 2.1 Gradle

| 모듈 | 산출물 | TCF 관련 |
|------|--------|----------|
| `tcf-util` | JAR | `GuidGenerator`, `DateTimeUtil` (STF·로그) |
| **`tcf-core`** | JAR | **TCF 엔진 전체** (~29 Java 클래스) |
| `tcf-web` | JAR | Controller, `TcfGateway`, `JdbcTransactionLogRepository` |

의존: `tcf-util` ← `tcf-core` ← `tcf-web` ← 업무 WAR

### 2.2 `tcf-core` 패키지 (전 클래스)

```text
com.nh.nsight.tcf.core/
├── config/
│   ├── TcfProperties                    nsight.tcf.* 설정
│   └── NsightTxlogPathEnvironmentPostProcessor   (기동 전, AutoConfig 아님)
├── message/
│   ├── StandardRequest / StandardResponse / StandardHeader / Result
│   └── ProcessingType                     INQUIRY, CREATE, … enum
├── processor/
│   ├── TCF                              오케스트레이션
│   ├── STF                              전처리
│   └── ETF                              후처리
├── dispatch/
│   └── TransactionDispatcher            serviceId → Handler
├── transaction/
│   └── TransactionHandler               업무 SPI
├── context/
│   ├── TransactionContext
│   └── TransactionContextHolder         ThreadLocal
├── validation/
│   └── StandardHeaderValidator
├── security/
│   ├── SessionValidator
│   └── AuthorizationValidator
├── idempotency/
│   ├── IdempotencyChecker               interface
│   └── InMemoryIdempotencyChecker       기본 구현
├── logging/
│   ├── TransactionLogService
│   ├── AuditLogService
│   ├── TransactionLogRepository         SPI (구현: tcf-web)
│   ├── TransactionLogRecord
│   └── TcfTransactionLogConstants
├── metrics/
│   └── TransactionMetricService
├── error/
│   ├── BusinessException / SystemException
│   └── ErrorCode                        E-COM-* 상수
└── support/
    └── TcfConsoleLog                    UTF-8 콘솔 디버그 출력
```

---

## 3. 진입점 — `TCF.process()`

모든 온라인 거래는 **`com.nh.nsight.tcf.core.support.processor.TCF`** 한 곳을 통과한다.

```27:64:tcf-core/src/main/java/com/nh/nsight/tcf/core/processor/TCF.java
    public StandardResponse<Object> process(StandardRequest<Map<String, Object>> request) {
        TransactionContext context = null;
        try {
            context = stf.preProcess(request);
            Object body = dispatcher.dispatch(request, context);
            StandardResponse<Object> response = etf.success(request, body, context);
            return response;
        } catch (BusinessException e) {
            return etf.businessFail(request, e, context);
        } catch (Exception e) {
            return etf.systemError(request, e, context);
        } finally {
            TransactionContextHolder.clear();
            MDC.clear();
        }
    }
```

| 단계 | 컴포넌트 | 출력 |
|------|----------|------|
| 1 | `STF.preProcess` | `TransactionContext` |
| 2 | `TransactionDispatcher.dispatch` | Handler body (`Object`, 보통 `Map`) |
| 3 | `ETF.success` / `businessFail` / `systemError` | `StandardResponse` |

**설계 원칙**

- 예외를 HTTP 500으로 던지지 않고 **ETF가 표준 JSON**으로 변환 ([05-exception.md](05-exception.md))
- `finally`에서 `TransactionContextHolder`·`MDC` **필수 정리**
- `BusinessException` = 의도된 업무/검증 오류, 그 외 = 시스템 오류

### 3.1 HTTP에서 TCF 호출

| 진입 | 클래스 | 모듈 |
|------|--------|------|
| 표준 | `OnlineTransactionController` | tcf-web |
| 비표준 REST/파일 | `TcfGateway.invoke()` | tcf-web |
| 브라우저 | `tcf-ui` Relay → 업무 `/online` | (TCF 간접) |

```22:27:tcf-web/src/main/java/com/nh/nsight/tcf/web/gateway/TcfGateway.java
    public StandardResponse<Object> invoke(TcfInvokeRequest invokeRequest) {
        StandardRequest<Map<String, Object>> request = new StandardRequest<>(
                buildHeader(invokeRequest),
                invokeRequest.body() == null ? Map.of() : invokeRequest.body()
        );
        return tcf.process(request);
    }
```

---

## 4. STF — Standard Transaction Front

**파일:** `tcf-core/.../processor/STF.java`  
상세: [34-STF.md](34-STF.md)

```38:62:tcf-core/src/main/java/com/nh/nsight/tcf/core/processor/STF.java
    public TransactionContext preProcess(StandardRequest<Map<String, Object>> request) {
        headerValidator.validate(request);
        StandardHeader header = request.getHeader();
        if (!StringUtils.hasText(header.getGuid())) {
            header.setGuid(GuidGenerator.newGuid());
        }
        if (!StringUtils.hasText(header.getTraceId())) {
            header.setTraceId(GuidGenerator.newTraceId());
        }
        TransactionContext context = new TransactionContext(header);
        TransactionContextHolder.set(context);
        putMdc(header);
        sessionValidator.validate(header);
        authorizationValidator.validate(header);
        idempotencyChecker.checkAndMarkProcessing(header);
        transactionLogService.start(context);
        return context;
    }
```

### 4.1 처리 순서표

| # | 처리 | 클래스 | 실패 errorCode |
|---|------|--------|----------------|
| 1 | Header 필수값 | `StandardHeaderValidator` | `E-COM-VALID-0001` |
| 2 | Header 정규화 | `StandardHeader.normalize()` | — |
| 3 | GUID | `GuidGenerator.newGuid()` | — |
| 4 | TraceId | `GuidGenerator.newTraceId()` | — |
| 5 | Context | `TransactionContext` + `TransactionContextHolder` | — |
| 6 | MDC | guid, traceId, serviceId, userId, branchId | — |
| 7 | 세션 | `SessionValidator` | `E-COM-AUTH-0001` |
| 8 | 권한 | `AuthorizationValidator` | `E-COM-AUTH-0002` |
| 9 | 멱등성 | `InMemoryIdempotencyChecker` | `E-COM-IDEMP-0001` |
| 10 | TX 시작 로그 | `TransactionLogService.start()` | — |

### 4.2 Header 검증·정규화

**필수 필드:** `serviceId`, `businessCode`, `transactionCode`, `processingType`, `channelId`

```24:36:tcf-core/src/main/java/com/nh/nsight/tcf/core/message/StandardHeader.java
    public void normalize() {
        if (systemId == null || systemId.isBlank()) {
            systemId = "NSIGHT-MP";
        }
        if (requestTime == null || requestTime.isBlank()) {
            requestTime = OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        }
        if (businessCode != null) {
            businessCode = businessCode.trim().toUpperCase();
        }
        if (processingType != null) {
            processingType = processingType.trim().toUpperCase();
        }
    }
```

전문 필드 상세: [02-junmun.md](02-junmun.md)

### 4.3 세션·권한 (설정 기반)

| `TcfProperties` | 기본 | 활성 시 검증 |
|-----------------|------|--------------|
| `session-validation-enabled` | `false` | `userId` 필수 |
| `authorization-validation-enabled` | `false` | `branchId` 필수 |

OM Admin 등 로그인 환경에서 `true`로 전환.

### 4.4 멱등성

**구현:** `InMemoryIdempotencyChecker` — JVM 메모리 `ConcurrentHashMap`

| 키 | `idempotencyKey` Header, 없으면 `guid` |
|----|----------------------------------------|
| 중복 처리 중 | `E-COM-IDEMP-0001` |
| 설정 off | `idempotency-enabled=false` → 스킵 |

운영 다중 AP에서는 **Redis 등 외부 구현체**로 `IdempotencyChecker` 교체 가능(SPI).

---

## 5. TransactionDispatcher — Handler 라우팅

**파일:** `tcf-core/.../dispatch/TransactionDispatcher.java`

### 5.1 기동 시 등록

Spring `@Component`인 모든 `TransactionHandler`를 주입받아 **`serviceId` → Handler** Map 구성.
핸들러는 도메인당 1개이므로, 각 핸들러의 `serviceIds()` 목록을 순회하며 등록한다.

```22:39:tcf-core/src/main/java/com/nh/nsight/tcf/core/dispatch/TransactionDispatcher.java
    public TransactionDispatcher(List<TransactionHandler> handlers) {
        for (TransactionHandler handler : handlers) {
            Collection<String> serviceIds = handler.serviceIds();
            if (serviceIds == null || serviceIds.isEmpty()) {
                log.warn("Handler declares no serviceId, skipped: {}", handler.getClass().getName());
                continue;
            }
            for (String serviceId : serviceIds) {
                TransactionHandler previous = handlerMap.put(serviceId, handler);
                if (previous != null) {
                    throw new IllegalStateException("Duplicate serviceId detected: " + serviceId);
                }
                log.info("Registered NSIGHT handler. serviceId={} handler={}",
                        serviceId, handler.getClass().getSimpleName());
            }
        }
    }
```

- 한 핸들러가 `serviceIds()`로 **여러 serviceId**를 담당 (도메인당 1개 핸들러)
- **중복 serviceId** → 기동 실패 (`IllegalStateException`)
- 로그 `Registered NSIGHT handler` → 등록 성공 확인

### 5.2 요청 시 dispatch

```34:50:tcf-core/src/main/java/com/nh/nsight/tcf/core/dispatch/TransactionDispatcher.java
    public Object dispatch(StandardRequest<Map<String, Object>> request, TransactionContext context) {
        String serviceId = request.getHeader() == null ? null : request.getHeader().getServiceId();
        if (!StringUtils.hasText(serviceId)) {
            throw new BusinessException(ErrorCode.INVALID_HEADER, "serviceId가 없습니다.");
        }
        TransactionHandler handler = handlerMap.get(serviceId);
        if (handler == null) {
            throw new BusinessException(ErrorCode.SERVICE_NOT_FOUND, "등록되지 않은 serviceId입니다: " + serviceId);
        }
        return handler.handle(request, context);
    }
```

| 상황 | errorCode |
|------|-----------|
| serviceId 없음 | `E-COM-VALID-0001` |
| 미등록 serviceId | `E-COM-DISP-0001` |

---

## 6. TransactionHandler — 업무 SPI

**인터페이스:** `tcf-core/.../transaction/TransactionHandler.java`

```20:57:tcf-core/src/main/java/com/nh/nsight/tcf/core/transaction/TransactionHandler.java
public interface TransactionHandler {

    // 단일 거래 매핑용 (하위호환). 도메인 묶음 핸들러는 재정의 불필요.
    default String serviceId() {
        return null;
    }

    // 이 핸들러가 담당하는 serviceId 목록. 기본은 serviceId() 단일값을 감싼다.
    default Collection<String> serviceIds() {
        String id = serviceId();
        return id == null ? List.of() : List.of(id);
    }

    default Object handle(StandardRequest<Map<String, Object>> request, TransactionContext context) {
        try {
            return doHandle(request, context);
        } catch (RuntimeException e) {
            throw e;
        }
    }

    Object doHandle(StandardRequest<Map<String, Object>> request, TransactionContext context);
}
```

| 규칙 | 내용 |
|------|------|
| 등록 | `@Component` + `scanBasePackages = "com.nh.nsight"` |
| serviceId | `{BusinessCode}.{Domain}.{action}` — 예: `SV.Sample.inquiry` |
| 구현 | `doHandle` → Facade 위임, **예외 catch 금지** |
| 반환 | `Map<String, Object>` 등 → ETF `body` |

Handler 작성·Facade 연결: [29-facade.md](29-facade.md)

---

## 7. TransactionContext

**파일:** `tcf-core/.../context/TransactionContext.java`

| 멤버 | 용도 |
|------|------|
| `header` | STF 이후 확정된 `StandardHeader` |
| `startTimeMillis` | 거래 시작 시각 |
| `attributes` | 거래 범위 임의 Map (`put`/`get`) |
| `elapsedMillis()` | ETF·로그·메트릭용 소요 시간 |

**접근 방법**

1. Handler → Facade → Service **메서드 인자**로 전달 (권장)
2. `TransactionContextHolder.get()` — ThreadLocal (동일 스레드)

```3:10:tcf-core/src/main/java/com/nh/nsight/tcf/core/context/TransactionContextHolder.java
public final class TransactionContextHolder {
    private static final ThreadLocal<TransactionContext> HOLDER = new ThreadLocal<>();
    public static void set(TransactionContext context) { HOLDER.set(context); }
    public static TransactionContext get() { return HOLDER.get(); }
    public static void clear() { HOLDER.remove(); }
}
```

TCF `finally` + `GuidMdcCleanupFilter`가 **이중 clear** — ThreadLocal 누수 방지.

---

## 8. ETF — End Transaction Framework

**파일:** `tcf-core/.../processor/ETF.java`

### 8.1 success()

| 순서 | 처리 |
|------|------|
| 1 | `IdempotencyChecker.markSuccess()` |
| 2 | `TransactionLogService.end(context, "S0000", null)` |
| 3 | `AuditLogService.audit(context, "S0000")` |
| 4 | `TransactionMetricService.record(context, "S0000")` |
| 5 | `StandardResponse.success(header, body)` |

### 8.2 businessFail() / systemError()

| 분기 | trigger | resultCode | errorCode |
|------|---------|------------|-----------|
| `businessFail` | `BusinessException` | `E0001` | 예외의 `errorCode` |
| `systemError` | 기타 `Exception` | `E0001` | `E-COM-SYS-0001` |

공통: idempotency fail, TX_END 로그, audit, metric.

### 8.3 Result 코드

```16:32:tcf-core/src/main/java/com/nh/nsight/tcf/core/message/Result.java
    public static Result success() {
        result.resultCode = "S0000";
        result.resultMessage = "정상 처리되었습니다.";
    }

    public static Result fail(String errorCode, String message, String detail) {
        result.resultCode = "E0001";
        result.resultMessage = "처리 중 오류가 발생했습니다.";
        result.errorCode = errorCode;
        // ...
    }
```

클라이언트 판별: **`result.resultCode == "S0000"`** → 성공 (HTTP는 주로 200).

---

## 9. 프레임워크 공통 errorCode

**파일:** `tcf-core/.../error/ErrorCode.java`

| 상수 | 코드 | 발생 위치 |
|------|------|-----------|
| `INVALID_HEADER` | `E-COM-VALID-0001` | STF Header 검증 |
| `SERVICE_NOT_FOUND` | `E-COM-DISP-0001` | Dispatcher |
| `SESSION_INVALID` | `E-COM-AUTH-0001` | SessionValidator |
| `AUTHORIZATION_DENIED` | `E-COM-AUTH-0002` | AuthorizationValidator |
| `DUPLICATE_REQUEST` | `E-COM-IDEMP-0001` | IdempotencyChecker |
| `BUSINESS_ERROR` | `E-COM-BIZ-0001` | 업무 Rule (일반) |
| `SYSTEM_ERROR` | `E-COM-SYS-0001` | ETF systemError |

업무 전용 코드: `E-OM-BIZ-*`, `E-SV-*` 등 — `BusinessException`에 임의 문자열 지정.

---

## 10. 로깅·감사·메트릭

### 10.1 TransactionLogService

| 시점 | 메서드 | 출력 |
|------|--------|------|
| STF | `start()` | SLF4J `transaction.log` — TX_START |
| ETF | `end()` | TX_END + **DB persist** |

DB 적재는 **`TransactionLogRepository` SPI** — `tcf-web`의 `JdbcTransactionLogRepository`가 `TCF_TX_LOG` INSERT.

```40:57:tcf-core/src/main/java/com/nh/nsight/tcf/core/logging/TransactionLogService.java
    private void persist(TransactionContext context, String resultCode, String errorCode) {
        if (!properties.isTransactionLogEnabled()) {
            return;
        }
        TransactionLogRepository repository = repositoryProvider.getIfAvailable();
        if (repository == null) {
            log.warn("TransactionLogRepository not available; skip DB persist.");
            return;
        }
        try {
            repository.save(toRecord(context, resultCode, errorCode));
        } catch (Exception e) {
            log.warn("Failed to persist transaction log.", e);
        }
    }
```

- 업무 DB TX **롤백**과 **독립** — 별도 DataSource·실패 시 warn만 ([09-transaction log.md](09-transaction log.md))
- `tcf-batch`: `transaction-log-enabled=false` → Repository 없음

### 10.2 AuditLogService

Logger `audit.log` — `nsight.tcf.audit-enabled=true`(기본)일 때 ETF마다 1줄.

### 10.3 TransactionMetricService

현재 **debug 로그** 수준 — `serviceId`, `resultCode`, `elapsedMs`.  
Micrometer/Prometheus 연동 확장 지점.

---

## 11. 설정 — `TcfProperties`

**prefix:** `nsight.tcf` (`tcf-core/.../config/TcfProperties.java`)

| 프로퍼티 | 기본값 | STF/ETF 영향 |
|----------|--------|--------------|
| `session-validation-enabled` | `false` | SessionValidator |
| `authorization-validation-enabled` | `false` | AuthorizationValidator |
| `idempotency-enabled` | `true` | IdempotencyChecker |
| `audit-enabled` | `true` | AuditLogService |
| `transaction-log-enabled` | `true` | TransactionLogService DB persist |
| `transaction-log-schema-auto-init` | `true` | tcf-web DDL |
| `transaction-log-table-name` | `TCF_TX_LOG` | INSERT 대상 |
| `transaction-log-datasource.*` | separate H2 | tcf-web AutoConfig |

바인딩: `TcfAutoConfiguration` ([31-autoconfiguration.md](31-autoconfiguration.md))

**기동 전 경로:** `NsightTxlogPathEnvironmentPostProcessor` → `nsight.txlog.path`

---

## 12. ProcessingType

**enum:** `tcf-core/.../message/ProcessingType.java`

```text
INQUIRY, CREATE, UPDATE, DELETE, EXECUTE, DOWNLOAD, UPLOAD
```

Header `processingType` 문자열과 매칭 — STF에서 **대문자 정규화**.  
OM ServiceId 카탈로그·Facade `@Transactional(readOnly)` 선택 참고용.

---

## 13. End-to-End Walkthrough

**요청:** `POST /sv/online`, `serviceId = SV.Sample.inquiry`

```text
1. OnlineTransactionController
     · businessCode ← path "sv"
     · clientIp ← X-Forwarded-For
     · tcf.process(request)

2. STF
     · validate header (SV, SV.Sample.inquiry, …)
     · guid/traceId 생성
     · Context + MDC
     · idempotency check
     · TX_START log

3. TransactionDispatcher
     · handlerMap.get("SV.Sample.inquiry")
     · SvSampleHandler.doHandle()

4. Handler → Facade.inquiry() [@Transactional AOP]
     · Service → Rule → DAO

5. ETF.success
     · markSuccess idempotency
     · TX_END + TCF_TX_LOG INSERT
     · audit + metric
     · StandardResponse { result: S0000, body: {...} }

6. TCF finally
     · ContextHolder.clear(), MDC.clear()
```

**BusinessException** (Rule에서 Body 비어 있음):

```text
Service throw BusinessException
  → TCF catch → ETF.businessFail
  → result E0001, Facade @Transactional rollback
  → TCF_TX_LOG에는 FAIL 기록 (별도 DS)
```

---

## 14. TCF vs Spring DB TX vs 거래로그

```text
┌── 1 HTTP Request ──────────────────────────────────────────┐
│ TCF 온라인 거래 (tcf-core)                                  │
│   STF ── Handler ── ETF                                      │
│         └─ Facade @Transactional ← Spring AOP (업무 DB)     │
└────────────────────────────────────────────────────────────┘

┌── 별도 Connection ─────────────────────────────────────────┐
│ TCF_TX_LOG INSERT (tcf-web JdbcTransactionLogRepository)   │
│   · transaction-log-datasource (often separate from dataSource)│
│   · Facade rollback과 무관                                   │
└────────────────────────────────────────────────────────────┘
```

| 개념 | 범위 | 롤백 |
|------|------|------|
| TCF 온라인 거래 | HTTP 1건 전체 | 없음 — ETF가 결과 전문 생성 |
| Spring `@Transactional` | Facade 메서드 1회 | BusinessException 시 rollback |
| 거래로그 DB | STF~ETF 이력 | auto-commit, 업무 TX와 분리 |

---

## 15. 확장·교체 포인트 (SPI)

| SPI / Bean | 기본 구현 | 교체 방법 |
|------------|-----------|-----------|
| `TransactionHandler` | 업무 `@Component` | 도메인당 1 Handler, `serviceIds()`에 거래 추가 |
| `TransactionLogRepository` | `JdbcTransactionLogRepository` | `@Bean` 오버라이드 |
| `IdempotencyChecker` | `InMemoryIdempotencyChecker` | `@Primary` Bean |
| `SessionValidator` / `AuthorizationValidator` | tcf-core 기본 | `@Component` 교체 또는 설정 off |

**하지 않는 것**

- STF/ETF/TCF 클래스를 업무 WAR에서 상속·오버라이드
- Handler에서 try-catch로 `BusinessException` 삼키기
- TCF 파이프라인 우회 Controller 직접 작성 (표준 온라인)

---

## 16. 빈 등록·기동

TCF 관련 `@Component`는 **`com.nh.nsight` ComponentScan** 으로 등록 ([30-springboot.md](30-springboot.md)).

| Bean | 스테레오타입 |
|------|-------------|
| `TCF`, `STF`, `ETF` | `@Component` |
| `TransactionDispatcher` | `@Component` |
| `StandardHeaderValidator`, Validators | `@Component` |
| `TransactionLogService`, `AuditLogService` | `@Service` |
| `TransactionHandler` 구현체 | `@Component` (업무 WAR) |

`tcf-ui`·`tcf-batch`(기본 scan)는 **TCF 빈 없음** — 온라인 `/online` 미제공.

---

## 17. 디버깅

| 확인 | 방법 |
|------|------|
| Handler 등록 | 기동 로그 `Registered NSIGHT handler. serviceId=` |
| STF~ETF 흐름 | `TcfConsoleLog` / System.out (개발용) |
| guid 추적 | MDC `guid`, `traceId` — logback pattern |
| 미등록 serviceId | 응답 `E-COM-DISP-0001` |
| 거래로그 미적재 | `TransactionLogRepository not available` warn |
| OM 조회 | `OM.TransactionLog.inquiry` — `TCF_TX_LOG` |

---

## 18. 관련 문서

| 주제 | 문서 |
|------|------|
| 표준 전문 JSON | [02-junmun.md](02-junmun.md) |
| 트랜잭션·STF/ETF 요약 | [03-transaction.md](03-transaction.md) |
| 예외·resultCode | [05-exception.md](05-exception.md) |
| 거래로그 테이블 | [09-transaction log.md](09-transaction log.md) |
| 온라인 아키텍처 | [14-online-arc.md](14-online-arc.md) |
| Handler 이후 계층 | [29-facade.md](29-facade.md) |
| tcf-core README | [tcf-core/README.md](../../tcf-core/README.md) |

---

## 19. 변경 이력

| 일자 | 변경 내용 |
|------|-----------|
| 2026-06 | 최초 작성 — tcf-core TCF 엔진 소스 레벨 가이드 |
