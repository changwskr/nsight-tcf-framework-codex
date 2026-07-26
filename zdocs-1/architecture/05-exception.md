# 05. 예외 처리 표준

| 항목 | 내용 |
|------|------|
| 문서 번호 | 05 |
| 제목 | Exception Handling Standard |
| 상위 문서 | [architecture.md](architecture.md) |
| 관련 문서 | [02-junmun.md](02-junmun.md), [03-transaction.md](03-transaction.md), [04-messaging.md](04-messaging.md) |
| 구현 모듈 | `tcf-core`, `tcf-web`, 업무 `*-service`, `tcf-om` |
| 대상 | 프레임워크·업무 개발자 |

---

## 1. 개요

NSIGHT TCF의 예외 처리는 **HTTP 상태 코드가 아닌 표준 응답 전문(`StandardResponse.result`)** 으로 성공·실패를 전달하는 것이 기본 원칙이다.

| 원칙 | 설명 |
|------|------|
| **HTTP 200 우선** | TCF 파이프라인 내 오류도 대부분 HTTP 200 + `resultCode: E0001` |
| **이중 코드** | `resultCode`(S0000/E0001) + `errorCode`(상세) |
| **의도적 vs 비의도적** | `BusinessException` = 업무·검증 오류, 그 외 = 시스템 오류 |
| **스택 미노출** | 클라이언트에는 `errorMessage`·`errorDetail`(요약)만, 전체 스택은 서버 로그 |
| **계층 위임** | Handler는 예외를 catch하지 않고 TCF·ETF에 위임 |

```text
업무 코드 throw BusinessException
        │
        ▼
TCF.process() catch
        │
        ├─ BusinessException  → ETF.businessFail()  → errorCode = 예외 코드
        └─ Exception          → ETF.systemError()   → errorCode = E-COM-SYS-0001
        │
        ▼
StandardResponse JSON (HTTP 200)
```

---

## 2. 예외 유형

### 2.1 BusinessException (표준 업무 예외)

```java
public class BusinessException extends RuntimeException {
    private final String errorCode;
    public BusinessException(String errorCode, String message) { ... }
}
```

| 항목 | 내용 |
|------|------|
| 용도 | 검증 실패, 데이터 없음, 중복, 권한 부족 등 **예상 가능한** 오류 |
| 발생 위치 | Rule, Service, STF, Dispatcher, IdempotencyChecker … |
| 처리 | `TCF` → `ETF.businessFail()` |
| 응답 | `resultCode=E0001`, `errorCode=예외의 errorCode`, `errorMessage=예외 message` |

```java
throw new BusinessException(ErrorCode.BUSINESS_ERROR, "요청 Body가 비어 있습니다.");
throw new BusinessException("E-OM-BIZ-0002", "사용자를 찾을 수 없습니다.");
```

### 2.2 SystemException (정의만 존재)

```java
public class SystemException extends RuntimeException {
    private final String errorCode;
    public SystemException(String errorCode, String message, Throwable cause) { ... }
}
```

`SystemException`은 **cause를 보존하는 래퍼**로 정의되어 있으나, 현재 코드베이스에서는 TCF가 `Exception`을 직접 catch한다.  
신규 코드에서는 **인프라·복구 불가 오류**에 `SystemException`을 쓰거나, unchecked 예외를 그대로 던져 `ETF.systemError()`로 처리하는 패턴을 따른다.

### 2.3 기타 Exception (비의도적 시스템 오류)

| 예시 | 처리 |
|------|------|
| `NullPointerException`, `DataAccessException` | `ETF.systemError()` |
| `errorCode` | `E-COM-SYS-0001` 고정 |
| `errorMessage` | `시스템 오류가 발생했습니다.` |
| `errorDetail` | 예외 클래스 단순명 (`e.getClass().getSimpleName()`) |
| 서버 로그 | SLF4J `error` (serviceId 포함) |

---

## 3. 오류 코드 체계

### 3.1 명명 규칙

```text
E-{영역}-{분류}-{일련번호}

영역  : COM(공통), OM, SV, UD, MG …
분류  : VALID, BIZ, AUTH, DB, SYS, VAL, TMO, IDEMP …
```

| 예 | 의미 |
|----|------|
| `E-COM-VALID-0001` | 프레임워크 Header 검증 실패 |
| `E-COM-DISP-0001` | 미등록 serviceId |
| `E-OM-BIZ-0002` | OM 업무 — 대상 데이터 없음 |
| `E-OM-VAL-0001` | OM 입력값 검증 실패 |
| `E-SV-BIZ-0001` | SV 업무 — 조회 0건 |
| `E-UD-VAL-0001` | UD 파일 API 검증 실패 |

### 3.2 프레임워크 공통 코드 (`ErrorCode`)

`com.nh.nsight.tcf.core.support.error.ErrorCode`:

| 상수 | 코드 | 발생 위치 |
|------|------|-----------|
| `INVALID_HEADER` | `E-COM-VALID-0001` | `StandardHeaderValidator`, Dispatcher(serviceId 누락) |
| `SERVICE_NOT_FOUND` | `E-COM-DISP-0001` | `TransactionDispatcher` |
| `SESSION_INVALID` | `E-COM-AUTH-0001` | `SessionValidator` |
| `AUTHORIZATION_DENIED` | `E-COM-AUTH-0002` | `AuthorizationValidator` |
| `DUPLICATE_REQUEST` | `E-COM-IDEMP-0001` | `InMemoryIdempotencyChecker` |
| `BUSINESS_ERROR` | `E-COM-BIZ-0001` | 업무 Rule 샘플·범용 업무 오류 |
| `SYSTEM_ERROR` | `E-COM-SYS-0001` | `ETF.systemError()`, `GlobalStandardExceptionHandler` |

### 3.3 업무·OM 상세 코드

- **OM**: `E-OM-*` — Service·Rule에서 직접 사용, `OM_ERROR_CODE` 테이블에 마스터 등록
- **샘플 업무**(SV, CC …): 샘플 Rule은 `ErrorCode.BUSINESS_ERROR` 사용, 실제 업무는 `E-{BC}-BIZ-0001` 등 **업무별 코드** 권장
- **UD REST**: `E-UD-VAL-0001`, `E-UD-BIZ-0001`, `E-UD-SYS-0001` (TCF 미경유)

### 3.4 OM_ERROR_CODE 마스터

`tcf-om` DB 테이블로 운영·화면 메시지를 관리한다.

| 컬럼 | 용도 |
|------|------|
| `ERROR_CODE` | PK — `E-OM-BIZ-0002` 등 |
| `ERROR_CATEGORY` | BIZ, AUTH, VALIDATION, DB, SYS … |
| `USER_MESSAGE` | 최종 사용자 안내 (향후 i18n·화면 매핑) |
| `OPERATOR_MESSAGE` | 운영자·OM Admin 설명 |
| `ACTION_GUIDE` | 조치 가이드 |
| `NOTIFY_TARGET` | 알림 대상 팀 |

OM Admin **오류코드 관리** 화면(`OM.ErrorCode.*`)으로 CRUD한다.  
런타임 `BusinessException`의 `errorMessage`는 **개발 시점 메시지**이며, 장기적으로 마스터 `USER_MESSAGE`와 연동할 수 있다.

---

## 4. 응답 전문 매핑

### 4.1 Result 구조

실패 시 `Result.fail(errorCode, message, detail)`:

| 필드 | 값 |
|------|-----|
| `resultCode` | `E0001` (고정) |
| `resultMessage` | `처리 중 오류가 발생했습니다.` (고정) |
| `errorCode` | `E-COM-...` 또는 `E-OM-...` |
| `errorMessage` | `BusinessException.getMessage()` |
| `errorDetail` | 선택 — 기술 상세 (systemError 시 클래스명) |
| `errorSystemId` | `NSIGHT-MP` |
| `errorDateTime` | KST ISO-8601 |

성공 시 `resultCode = S0000`. 상세: [02-junmun.md §4](02-junmun.md).

### 4.2 ETF 분기

| ETF 메서드 | 트리거 | errorCode |
|------------|--------|-----------|
| `success()` | 정상 | — |
| `businessFail()` | `BusinessException` | `e.getErrorCode()` |
| `systemError()` | `Exception` | `E-COM-SYS-0001` |

ETF는 실패 시에도 **멱등성 종료·거래로그·감사·메트릭**을 기록한다 (`resultCode=E0001`).

### 4.3 실패 응답 JSON 예

```json
{
  "header": {
    "serviceId": "SV.Sample.inquiry",
    "guid": "a1b2c3d4-e5f6-...."
  },
  "result": {
    "resultCode": "E0001",
    "resultMessage": "처리 중 오류가 발생했습니다.",
    "errorCode": "E-COM-BIZ-0001",
    "errorMessage": "요청 Body가 비어 있습니다.",
    "errorDetail": null,
    "errorSystemId": "NSIGHT-MP",
    "errorDateTime": "2026-06-20T14:30:00+09:00"
  },
  "body": null
}
```

---

## 5. 처리 경로 (어디서 잡히는가)

```text
                    ┌─────────────────────────────────────┐
                    │     TCF 파이프라인 (주 경로)          │
                    │  STF → Dispatcher → Handler → ETF   │
                    │  BusinessException → businessFail     │
                    │  Exception         → systemError      │
                    └─────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────┐
│  파이프라인 밖 (보조 경로)                                      │
│  GlobalStandardExceptionHandler (@RestControllerAdvice)       │
│    - JSON 파싱 전·Controller 진입 전 예외                       │
│    - MethodArgumentNotValidException, GET /online 등            │
│    - StandardResponse.fail(null, ErrorCode, ...)              │
└──────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────┐
│  TCF 미경유 (예외 — UD REST 등)                                 │
│  Controller try/catch → Map 응답 또는 OmUpdownloadResponseSupport │
└──────────────────────────────────────────────────────────────┘
```

### 5.1 GlobalStandardExceptionHandler

`TCF.process()` **이전**에 터지는 예외용 안전망.

| `@ExceptionHandler` | errorCode | 비고 |
|---------------------|-----------|------|
| `BusinessException` | 예외 코드 | TCF 밖에서 던진 경우 |
| `MethodArgumentNotValidException` | `E-COM-VALID-0001` | Bean Validation |
| `HttpRequestMethodNotSupportedException` | `E-COM-VALID-0001` | GET 접근 차단 안내 |
| `Exception` | `E-COM-SYS-0001` | 기타 |

`header`가 없을 수 있어 `StandardResponse.fail(null, ...)` 형태.

### 5.2 릴레이·연결 오류

`tcf-ui` `TransactionRelayService` 연결 실패는 **TCF 예외 체계 밖**:

```json
{"error":"...","targetUrl":"...","hint":"대상 WAS가 기동 중인지 확인하세요."}
```

클라이언트는 `RelayResult`와 `result.resultCode`를 구분해 처리한다.  
상세: [04-messaging.md §7](04-messaging.md).

---

## 6. 계층별 throw 가이드

### 6.1 어디서 던지는가

| 계층 | 예외 | errorCode 예 |
|------|------|--------------|
| **STF / Validator** | `BusinessException` | `E-COM-VALID-0001`, `E-COM-AUTH-*`, `E-COM-IDEMP-0001` |
| **Dispatcher** | `BusinessException` | `E-COM-DISP-0001` |
| **Rule** | `BusinessException` | `E-COM-BIZ-0001` 또는 `E-{BC}-VAL-*` |
| **Service** | `BusinessException` | `E-{BC}-BIZ-*` (없음, 중복, 상태 오류) |
| **DAO** | (권장) 예외 전파 | Spring `DataAccessException` → systemError |
| **Handler** | **던지지 않음** | Facade/Service 예외를 TCF에 위임 |
| **Facade** | (권장) Service 위임 | `@Transactional` 롤백 + 예외 전파 |

### 6.2 Rule 예시 (SV 샘플)

```java
@Component
public class SvSampleRule {
    public void validateInquiry(Map<String, Object> body) {
        if (body == null || body.isEmpty()) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "요청 Body가 비어 있습니다.");
        }
    }
}
```

### 6.3 Service 예시 (OM)

```java
Map<String, Object> row = dao.selectErrorCodeByCode(errorCode);
if (row == null) {
    throw new BusinessException("E-OM-BIZ-0002", "오류코드를 찾을 수 없습니다.");
}
```

### 6.4 OM Rule — 검증 전용

```java
public void requireField(Map<String, Object> body, String fieldName) {
    if (OmBodySupport.stringValue(body, fieldName) == null) {
        throw new BusinessException("E-OM-VAL-0002", fieldName + "은(는) 필수입니다.");
    }
}
```

---

## 7. 금지·권장 사항

### 7.1 권장

| 항목 | 내용 |
|------|------|
| 업무 오류 | `BusinessException(errorCode, userMessage)` |
| errorCode | 업무 도메인·OM 마스터와 일치하는 `E-{BC}-*` |
| 메시지 | 사용자·운영자가 이해할 수 있는 한글 문장 |
| 검증 | Rule 계층에서 선행, Service는 비즈니스 판단 |
| 로깅 | 시스템 오류만 `log.error` + stack trace |

### 7.2 금지

| 금지 | 이유 |
|------|------|
| Handler에서 `try/catch` 후 `StandardResponse` 직접 생성 | ETF·거래로그·멱등성 우회 |
| HTTP 4xx/5xx로 업무 실패 반환 (`/online`) | 채널 계약 위반 |
| `errorCode` 없이 `RuntimeException`만 던지기 | `E-COM-SYS-0001`로 뭉개짐 |
| 스택 트레이스를 `errorMessage`에 포함 | 보안·UX 저하 |
| Rule에서 DAO 호출 | 계층 혼재 ([01-application-layer.md](01-application-layer.md)) |

### 7.3 Handler·Facade와 트랜잭션

```text
Service throw BusinessException
  → Facade @Transactional 롤백
  → Handler 예외 전파
  → TCF catch → ETF.businessFail()
  → TCF_TX_LOG 에 FAIL 기록 (별도 커밋)
```

업무 DB는 롤백되어도 **거래 로그는 FAIL로 남는다**.

---

## 8. 비표준 경로 — UD REST

`OmUpdownloadFileController`는 TCF를 거치지 않고 Controller `try/catch`로 처리한다.

```java
try {
    return OmUpdownloadResponseSupport.success(service.upload(...));
} catch (IllegalArgumentException e) {
    return OmUpdownloadResponseSupport.fail("E-UD-VAL-0001", e.getMessage());
} catch (Exception e) {
    return OmUpdownloadResponseSupport.fail("E-UD-SYS-0001", "업로드 처리 중 오류가 발생했습니다.");
}
```

`OmUpdownloadResponseSupport.fail()`은 `result.resultCode`에 **errorCode 문자열을 직접** 넣는 등 TCF `Result.fail()`과 다르다.  
파일 API 신규 개발 시 가능하면 `TcfGateway` + Handler 통합을 검토한다.

---

## 9. 클라이언트 오류 처리

### 9.1 표준 판별 순서

```javascript
// 1. 릴레이 사용 시
if (relay.httpStatus >= 400) { /* 연결·게이트웨이 오류 */ }

// 2. TCF 응답
const result = payload.result;
if (result.resultCode !== 'S0000') {
  const msg = result.errorMessage || result.resultMessage;
  const code = result.errorCode;
  // 화면 표시·로깅
}
```

### 9.2 OM Admin (`om-admin.js`)

```javascript
if (payload.result?.resultCode !== 'S0000') {
  throw new Error(payload.result.errorMessage || payload.result.resultMessage);
}
```

로그인·세션 API는 `errorCode`별 분기보다 **메시지 표시** 위주.

---

## 10. 오류와 로그·감사

| 이벤트 | 기록 |
|--------|------|
| `BusinessException` | ETF `transactionLog.end(E0001, errorCode)`, `audit.log` |
| `systemError` | SLF4J error + 동일 종료 로그 |
| MDC | `guid`, `serviceId` — 로그 상관 |

`errorDetail`은 클라이언트용 요약이며, 상세 진단은 **서버 application 로그**와 `TCF_TX_LOG.ERROR_CODE`를 본다.

---

## 11. 신규 오류코드 등록 절차

1. **코드 할당** — `E-{BC}-{분류}-{번호}` 규칙 준수
2. **OM_ERROR_CODE 등록** — OM Admin 또는 `data.sql` (개발)
3. **코드 throw** — Rule/Service에서 `BusinessException` 사용
4. **Service Catalog** — 필요 시 transactionCode·Handler와 함께 문서화
5. **샘플·테스트** — `tcf-ui/sample-requests` 또는 거래 테스트 UI로 실패 케이스 확인

---

## 12. 전체 흐름도

```text
[Client]
   │ POST StandardRequest
   ▼
[OnlineTransactionController] ──(예외)──► GlobalStandardExceptionHandler
   │
   ▼
[TCF.process]
   │
   ├─ STF ──► BusinessException? ──► ETF.businessFail ──┐
   │                                                    │
   ├─ Dispatcher ──► BusinessException? ────────────────┤
   │                                                    │
   ├─ Handler → Facade → Service → Rule                 │
   │              │         │                          │
   │              │         └── BusinessException? ─────┤
   │              └── (rollback)                         │
   │                                                    │
   └─ Exception (NPE, SQL…) ──► ETF.systemError ───────┤
                                                        ▼
                                            StandardResponse (HTTP 200)
                                                        │
[Client] ◄──────────────────────────────────────────────┘
   result.resultCode / errorCode / errorMessage
```

---

## 13. 참고 소스 (읽는 순서)

| 순서 | 파일 |
|------|------|
| 1 | `tcf-core/.../error/BusinessException.java` |
| 2 | `tcf-core/.../error/ErrorCode.java` |
| 3 | `tcf-core/.../message/Result.java` |
| 4 | `tcf-core/.../processor/TCF.java` |
| 5 | `tcf-core/.../processor/ETF.java` |
| 6 | `tcf-web/.../exception/GlobalStandardExceptionHandler.java` |
| 7 | `sv-service/.../rule/SvSampleRule.java` |
| 8 | `tcf-om/.../rule/OmOperationRule.java` |
| 9 | `tcf-om/.../service/OmErrorCodeService.java` |

---

## 14. 관련 문서

| 문서 | 설명 |
|------|------|
| [02-junmun.md](02-junmun.md) | Result 필드·오류 응답 JSON |
| [03-transaction.md](03-transaction.md) | ETF·거래로그·멱등성 |
| [04-messaging.md](04-messaging.md) | 릴레이·비표준 오류 |
| [01-application-layer.md](01-application-layer.md) | Rule·Service 예외 위치 |
| [TCF_FRAMEWORK_GUIDE.md](../TCF_FRAMEWORK_GUIDE.md) | Handler 개발 |

---

## 15. 변경 이력

| 일자 | 변경 내용 |
|------|-----------|
| 2026-06 | 최초 작성 — BusinessException·ErrorCode·ETF·계층 가이드 |
