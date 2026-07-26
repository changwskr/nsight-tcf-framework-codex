# 03. 트랜잭션 처리 아키텍처

| 항목 | 내용 |
|------|------|
| 문서 번호 | 03 |
| 제목 | Transaction Processing Architecture |
| 상위 문서 | [architecture.md](architecture.md) |
| 관련 문서 | [02-junmun.md](02-junmun.md), [01-application-layer.md](01-application-layer.md) |
| 구현 모듈 | `tcf-core`, `tcf-web` |
| TCF 엔진 상세 | [33-TCF.md](33-TCF.md) |
| 대상 | 프레임워크·업무 개발자 |

---

## 1. 개요

NSIGHT TCF에서 **트랜잭션(Transaction)** 은 두 가지 의미로 쓰인다. 본 문서는 둘을 구분해 설명한다.

| 용어 | 의미 | 담당 |
|------|------|------|
| **TCF 온라인 거래** | HTTP JSON 전문 1건의 처리 파이프라인 | `TCF.process()` — STF → Dispatcher → ETF |
| **DB 트랜잭션** | 업무 DB 커밋/롤백 단위 | Facade `@Transactional` (Spring) |

```text
┌──────────────────────────────────────────────────────────────┐
│  TCF 온라인 거래 (1 HTTP Request = 1 TCF Transaction)         │
│  STF ──→ Handler ──→ ETF                                     │
│         └─ Facade @Transactional (DB TX, 선택)                 │
└──────────────────────────────────────────────────────────────┘
```

TCF 엔진 컴포넌트 명칭:

| 약어 | 클래스 | 역할 |
|------|--------|------|
| **STF** | `STF` | **S**tandard **T**ransaction **F**ront — 전처리 |
| **TCF** | `TCF` | **T**ransaction **C**ontrol **F**ramework — 오케스트레이션 |
| **ETF** | `ETF` | **E**nd **T**ransaction **F**ramework — 후처리·응답 |

---

## 2. 전체 처리 구조

### 2.1 컴포넌트 맵

```text
HTTP Request
    │
    ▼
GuidMdcCleanupFilter          (요청 종료 시 MDC 정리)
    │
    ▼
OnlineTransactionController   (또는 TcfGateway)
    │  businessCode / clientIp 보정
    ▼
TCF.process()
    │
    ├─ STF.preProcess()
    │     ├─ StandardHeaderValidator
    │     ├─ guid / traceId 부여
    │     ├─ TransactionContext 생성
    │     ├─ SessionValidator
    │     ├─ AuthorizationValidator
    │     ├─ IdempotencyChecker
    │     └─ TransactionLogService.start()
    │
    ├─ TransactionDispatcher.dispatch()
    │     └─ TransactionHandler.doHandle()
    │           └─ Facade → Service → DAO
    │
    └─ ETF (결과에 따라 분기)
          ├─ success()       → S0000
          ├─ businessFail()  → BusinessException
          └─ systemError()   → 기타 Exception
    │
    ▼
StandardResponse JSON
```

### 2.2 시퀀스 (성공 경로)

```text
Client          Controller       TCF          STF         Dispatcher      Handler       ETF
  │                 │             │           │              │             │           │
  │ POST /online    │             │           │              │             │           │
  ├────────────────►│             │           │              │             │           │
  │                 │ process()   │           │              │             │           │
  │                 ├────────────►│           │              │             │           │
  │                 │             │ preProcess│              │             │           │
  │                 │             ├──────────►│              │             │           │
  │                 │             │           │ validate     │             │           │
  │                 │             │           │ context+MDC  │             │           │
  │                 │             │◄──────────┤              │             │           │
  │                 │             │ dispatch  │              │             │           │
  │                 │             ├─────────────────────────►│             │           │
  │                 │             │                          │ doHandle()  │           │
  │                 │             │                          ├────────────►│           │
  │                 │             │                          │◄────────────┤ body    │
  │                 │             │ success()  │              │             │           │
  │                 │             ├──────────────────────────────────────────────────►│
  │                 │             │◄──────────────────────────────────────────────────┤
  │                 │◄────────────┤ StandardResponse         │             │           │
  │◄────────────────┤             │           │              │             │           │
```

---

## 3. TCF.process() — 핵심 오케스트레이션

`com.nh.nsight.tcf.core.support.processor.TCF`가 단일 진입점이다.

```java
public StandardResponse<Object> process(StandardRequest<Map<String, Object>> request) {
    TransactionContext context = null;
    try {
        context = stf.preProcess(request);
        Object body = dispatcher.dispatch(request, context);
        return etf.success(request, body, context);
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

| 단계 | 예외 타입 | ETF 분기 | resultCode |
|------|-----------|----------|------------|
| STF / Dispatcher / Handler | `BusinessException` | `businessFail()` | `E0001` + 업무 errorCode |
| 예기치 않은 오류 | `Exception` | `systemError()` | `E0001` + `E-COM-SYS-0001` |
| 정상 | — | `success()` | `S0000` |

**설계 포인트**

- 예외를 HTTP 500으로 올리지 않고 **표준 응답 전문**으로 변환한다.
- `finally`에서 `TransactionContextHolder`·`MDC`를 반드시 정리한다.
- `BusinessException`은 의도된 업무 오류, 그 외는 시스템 오류로 구분한다.

---

## 4. STF — 전처리 (Standard Transaction Front)

상세: [34-STF.md](34-STF.md)

### 4.1 처리 순서

| 순서 | 처리 | 클래스 | 실패 시 |
|------|------|--------|---------|
| 1 | Header 존재·필수값 검증 | `StandardHeaderValidator` | `E-COM-VALID-0001` |
| 2 | Header 정규화 | `StandardHeader.normalize()` | — |
| 3 | GUID 부여 | `GuidGenerator.newGuid()` | — |
| 4 | TraceId 부여 | `GuidGenerator.newTraceId()` | — |
| 5 | Context 생성·등록 | `TransactionContext`, `TransactionContextHolder` | — |
| 6 | MDC 적재 | `guid`, `traceId`, `serviceId`, `userId`, `branchId` | — |
| 7 | 세션 검증 | `SessionValidator` | `E-COM-AUTH-0001` |
| 8 | 권한 검증 | `AuthorizationValidator` | `E-COM-AUTH-0002` |
| 9 | 멱등성 검사 | `IdempotencyChecker` | `E-COM-IDEMP-0001` |
| 10 | 거래 시작 로그 | `TransactionLogService.start()` | — |

### 4.2 Header 필수 검증

`StandardHeaderValidator`가 검증하는 필수 필드:

- `serviceId`
- `businessCode`
- `transactionCode`
- `processingType`
- `channelId`

상세 필드 정의: [02-junmun.md](02-junmun.md)

### 4.3 세션·권한 검증 (설정 기반)

`nsight.tcf` 프로퍼티로 활성화한다. 기본값은 **비활성**이다.

| 프로퍼티 | 기본값 | 검증 내용 |
|----------|--------|-----------|
| `session-validation-enabled` | `false` | `userId` 필수 |
| `authorization-validation-enabled` | `false` | `branchId` 필수 |

OM Admin 등 로그인 필수 환경에서는 `true`로 설정한다.

---

## 5. TransactionContext — 거래 컨텍스트

요청 1건당 하나의 `TransactionContext`가 STF에서 생성된다.

| 속성 | 설명 |
|------|------|
| `header` | 정규화·보완된 `StandardHeader` (불변 참조) |
| `startTimeMillis` | 거래 시작 시각 (epoch ms) |
| `attributes` | 거래 범위 임의 속성 Map |
| `elapsedMillis()` | 현재까지 소요 시간 |

```java
public class TransactionContext {
    private final StandardHeader header;
    private final long startTimeMillis;
    private final Map<String, Object> attributes;
    // put(key, value), get(key)
}
```

**보관 위치**

| 메커니즘 | 용도 |
|----------|------|
| `TransactionContextHolder` (ThreadLocal) | Handler·Service·DAO 전 구간 접근 |
| 메서드 인자로 전달 | Handler → Facade → Service 명시적 전달 |

TCF `finally`와 `GuidMdcCleanupFilter`가 **이중으로** ThreadLocal·MDC를 정리한다.

---

## 6. TransactionDispatcher — Handler 라우팅

`header.serviceId`를 키로 `TransactionHandler` 빈을 조회한다.

```text
기동 시:  모든 @Component TransactionHandler → Map<serviceId, Handler>
요청 시:  handlerMap.get(serviceId) → handler.handle(request, context)
```

| 상황 | 오류 |
|------|------|
| `serviceId` 없음 | `E-COM-VALID-0001` |
| 미등록 serviceId | `E-COM-DISP-0001` |
| 동일 serviceId 중복 빈 | 기동 시 `IllegalStateException` |

Handler는 `handle()` 기본 메서드에서 로깅 후 `doHandle()`을 호출한다.

---

## 7. Handler 실행 — 어플리케이션 계층 진입

Dispatcher 이후는 [01-application-layer.md](01-application-layer.md)의 계층이 담당한다.

```text
TransactionHandler.doHandle()
  → Facade (@Transactional)
    → Service
      → Rule / DAO
```

TCF 파이프라인 **안쪽**에서 업무 로직이 실행된다.  
Handler에서 `BusinessException`을 던지면 TCF가 catch하여 `ETF.businessFail()`로 변환한다.

---

## 8. ETF — 후처리 (End Transaction Framework)

### 8.1 success()

| 순서 | 처리 |
|------|------|
| 1 | `IdempotencyChecker.markSuccess()` |
| 2 | `TransactionLogService.end(context, "S0000", null)` |
| 3 | `AuditLogService.audit(context, "S0000")` |
| 4 | `TransactionMetricService.record(context, "S0000")` |
| 5 | `StandardResponse.success(header, body)` |

### 8.2 businessFail()

| 순서 | 처리 |
|------|------|
| 1 | `IdempotencyChecker.markFail()` |
| 2 | `TransactionLogService.end(context, "E0001", errorCode)` |
| 3 | `AuditLogService.audit(context, "E0001")` |
| 4 | `TransactionMetricService.record(context, "E0001")` |
| 5 | `StandardResponse.fail(header, errorCode, message, null)` |

### 8.3 systemError()

`businessFail()`과 동일하나 `errorCode`는 `E-COM-SYS-0001`, SLF4J error 로그를 남긴다.

---

## 9. 멱등성 (Idempotency)

구현: `InMemoryIdempotencyChecker` (프로세스 메모리)

| 설정 | `nsight.tcf.idempotency-enabled` (기본 `true`) |
|------|--------------------------------------------------|

**키 생성 규칙**

```text
idempotencyKey 있음 → idempotencyKey 사용
없음               → guid 사용
```

| 시점 | 동작 |
|------|------|
| STF `checkAndMarkProcessing` | 동일 키가 `PROCESSING`이면 `DUPLICATE_REQUEST` |
| ETF `markSuccess` / `markFail` | 상태를 `SUCCESS` / `FAIL`로 갱신 |

로컬 개발용 인메모리 구현이므로 **다중 인스턴스·재기동 시 공유되지 않는다**. 운영 환경에서는 Redis/DB 기반 구현 교체를 고려한다.

---

## 10. 거래 로그·감사·메트릭

### 10.1 TransactionLogService

| 시점 | 로그 |
|------|------|
| `start()` | `TX_START` — guid, traceId, serviceId, txCode, userId … |
| `end()` | `TX_END` — resultCode, errorCode, elapsedMs |

DB 적재 (`nsight.tcf.transaction-log-enabled=true`, 기본 활성):

- 테이블: **`TCF_TX_LOG`**
- Repository: `JdbcTransactionLogRepository` (`tcf-web`)
- DataSource: `transactionLogDataSource` (업무 DS와 분리 가능)
- 경로: `nsight.txlog.path` → `data/nsight-txlog/nsight_om`

**독립 커밋 설계**

업무 Facade가 `@Transactional`로 롤백되어도 거래 로그는 **별도 auto-commit INSERT**로 남는다.

```text
업무 DB TX (롤백 가능)     ≠     TCF_TX_LOG INSERT (항상 커밋)
```

### 10.2 TCF_TX_LOG 컬럼

| 컬럼 | 설명 |
|------|------|
| `LOG_ID` | 로그 PK |
| `TX_TIME` | 종료 시각 (KST) |
| `BUSINESS_CODE` | 업무 코드 |
| `SERVICE_ID` | serviceId |
| `TRANSACTION_CODE` | transactionCode |
| `GUID` / `TRACE_ID` | 추적 키 |
| `USER_ID` / `BRANCH_ID` | 사용자·지점 |
| `RESULT_STATUS` | `SUCCESS` / `FAIL` |
| `RESULT_CODE` | `S0000` / `E0001` |
| `ERROR_CODE` | 상세 오류 코드 |
| `ELAPSED_TIME_MS` | 소요 시간 |

OM 거래로그 화면(`OM.TransactionLog.inquiry`)이 동일 테이블을 조회한다.

### 10.3 AuditLogService

| 설정 | `nsight.tcf.audit-enabled` (기본 `true`) |
|------|------------------------------------------|
| Logger | `audit.log` |
| 내용 | guid, serviceId, txCode, userId, branchId, resultCode |

### 10.4 TransactionMetricService

현재는 DEBUG 로그로 `serviceId`, `resultCode`, `elapsedMs`를 기록한다.  
Micrometer·Prometheus 연동 확장 지점으로 사용할 수 있다.

---

## 11. HTTP 레이어 보조 처리

### 11.1 OnlineTransactionController

| 보정 | 조건 |
|------|------|
| `businessCode` | URL path `/{code}/online`에서 Header에 없을 때 설정 |
| `clientIp` | Header 비어 있으면 `X-Forwarded-For` / `remoteAddr` |

### 11.2 TcfGateway

`POST /online`이 아닌 REST 진입도 동일 `TCF.process()`를 호출한다.  
Header를 코드에서 조립한 뒤 파이프라인에 태운다.

### 11.3 GuidMdcCleanupFilter

필터 체인 **finally**에서 `MDC.clear()` — 비정상 종료 시에도 로그 오염 방지.

### 11.4 GlobalStandardExceptionHandler

Controller **밖**에서 발생한 예외(validation, wrong HTTP method)도 `StandardResponse.fail()` 형태로 변환한다.  
TCF 파이프라인을 거치지 않은 요청에 대한 안전망이다.

---

## 12. DB 트랜잭션 (Spring) — 업무 계층

TCF 온라인 거래와 **별도**로, 업무 DB의 ACID는 Facade에서 관리한다.

```java
@Service
public class SvSampleFacade {
    @Transactional(readOnly = true, timeout = 5)
    public Map<String, Object> inquiry(Map<String, Object> body, TransactionContext context) {
        return service.inquiry(body, context);
    }
}
```

| 처리 유형 | 권장 어노테이션 |
|-----------|-----------------|
| 조회 | `@Transactional(readOnly = true, timeout = 5)` |
| 등록·수정·삭제 | `@Transactional(timeout = 10)` |

**TCF vs Spring TX 관계**

```text
┌─ TCF 온라인 거래 (항상 1회) ─────────────────────────────┐
│  STF → Handler → Facade [@Transactional] → Service    │
│  ETF (성공/실패 무관하게 거래 로그·감사 기록)              │
└──────────────────────────────────────────────────────────┘
```

- Handler에서 예외 → Spring TX **롤백** + ETF `businessFail()` **동시 발생**
- `TCF_TX_LOG` INSERT는 업무 TX와 **분리**되어 롤백되지 않음

---

## 13. 설정 참조 (`nsight.tcf`)

```yaml
nsight:
  tcf:
    session-validation-enabled: false
    authorization-validation-enabled: false
    idempotency-enabled: true
    audit-enabled: true
    transaction-log-enabled: true
    transaction-log-schema-auto-init: true
    transaction-log-table-name: TCF_TX_LOG
    transaction-log-datasource:
      separate: true
      url: jdbc:h2:file:${nsight.txlog.path}/nsight_om;...
```

| 모듈 | transaction-log-enabled | 비고 |
|------|-------------------------|------|
| `*-service`, `tcf-om` | `true` (기본) | `TCF_TX_LOG` 적재 |
| `tcf-batch` | `false` | 수집 전용, 로그 DS 미사용 |

---

## 14. 오류 처리 요약

```text
                    ┌─────────────────┐
                    │  STF.preProcess │
                    └────────┬────────┘
                             │
              ┌──────────────┼──────────────┐
              ▼              ▼              ▼
        BusinessException  (정상)      (미래 확장)
              │              │
              ▼              ▼
     ┌────────────────┐  ┌──────────────────┐
     │ Dispatcher     │  │ Handler 실행      │
     └────────┬───────┘  └────────┬─────────┘
              │                   │
              ▼                   ▼
     BusinessException    BusinessException / Exception
              │                   │
              └─────────┬─────────┘
                        ▼
              ┌─────────────────────┐
              │ ETF                 │
              │ businessFail /      │
              │ systemError /       │
              │ success             │
              └─────────────────────┘
```

| ErrorCode | 상수 | 발생 위치 예 |
|-----------|------|--------------|
| `E-COM-VALID-0001` | `INVALID_HEADER` | STF Header 검증 |
| `E-COM-DISP-0001` | `SERVICE_NOT_FOUND` | Dispatcher |
| `E-COM-AUTH-0001` | `SESSION_INVALID` | SessionValidator |
| `E-COM-AUTH-0002` | `AUTHORIZATION_DENIED` | AuthorizationValidator |
| `E-COM-IDEMP-0001` | `DUPLICATE_REQUEST` | IdempotencyChecker |
| `E-COM-BIZ-0001` | `BUSINESS_ERROR` | Rule, Service |
| `E-COM-SYS-0001` | `SYSTEM_ERROR` | ETF systemError |

---

## 15. 확장·교체 지점

| 인터페이스 | 기본 구현 | 확장 방향 |
|------------|-----------|-----------|
| `TransactionHandler` | 업무별 `@Component` | serviceId 추가로 거래 확장 |
| `IdempotencyChecker` | `InMemoryIdempotencyChecker` | Redis / DB |
| `TransactionLogRepository` | `JdbcTransactionLogRepository` | Kafka, Elastic |
| `SessionValidator` | userId 검사 | Spring Session 연동 강화 |
| `AuthorizationValidator` | branchId 검사 | OM 권한 테이블 연동 |

---

## 16. 참고 소스 (읽는 순서)

| 순서 | 파일 |
|------|------|
| 1 | `tcf-core/.../processor/TCF.java` |
| 2 | `tcf-core/.../processor/STF.java` |
| 3 | `tcf-core/.../processor/ETF.java` |
| 4 | `tcf-core/.../dispatch/TransactionDispatcher.java` |
| 5 | `tcf-core/.../context/TransactionContext.java` |
| 6 | `tcf-core/.../logging/TransactionLogService.java` |
| 7 | `tcf-web/.../controller/OnlineTransactionController.java` |
| 8 | `tcf-web/.../logging/JdbcTransactionLogRepository.java` |
| 9 | `sv-service/.../entry/handler/SvSampleHandler.java` |

---

## 17. 관련 문서

| 문서 | 설명 |
|------|------|
| [architecture.md](architecture.md) | 전체 아키텍처 |
| [02-junmun.md](02-junmun.md) | 요청·응답 전문 |
| [04-messaging.md](04-messaging.md) | 메시지 처리·릴레이 |
| [05-exception.md](05-exception.md) | 예외 처리 표준 |
| [01-application-layer.md](01-application-layer.md) | Handler 이후 계층 |
| [TCF_FRAMEWORK_GUIDE.md](../TCF_FRAMEWORK_GUIDE.md) | 개발 가이드 |

---

## 18. 변경 이력

| 일자 | 변경 내용 |
|------|-----------|
| 2026-06 | 최초 작성 — STF/TCF/ETF·Context·로그·멱등성 |
