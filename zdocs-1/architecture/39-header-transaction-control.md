# 39. 전문 Header 기반 거래통제 설계안

| 항목 | 내용 |
|------|------|
| 문서 번호 | 39 |
| 제목 | Header 기반 거래통제 (Transaction Control) |
| 상위 문서 | [architecture.md](architecture.md) |
| 관련 문서 | [02-junmun.md](02-junmun.md), [03-transaction.md](03-transaction.md), [34-STF.md](34-STF.md), [36-ETF.md](36-ETF.md), [37-transaction-log.md](37-transaction-log.md), [19-tcf-table.md](19-tcf-table.md) |
| 구현 현황 | [§17 현재 코드베이스 대응](#17-현재-코드베이스-대응-2026-06) |
| 대상 | 프레임워크·업무·OM·아키텍트 |

---

## 1. 결론

NSIGHT에서 거래를 통제하려면 **Body가 아니라 Header를 기준으로 거래의 성격, 권한, 중복, Timeout, 감사, 재처리 여부를 결정**해야 합니다.

즉, Body는 업무 데이터이고, Header는 거래를 통제하는 기준입니다.

```text
전문 Header = 거래통제 기준
전문 Body   = 업무 처리 데이터
```

기존 NSIGHT 전처리·후처리 기준에서도 모든 온라인 거래는 표준 전문으로 수신되고, Header 검증, GUID/Trace 생성, 세션·인증·권한 검증, 중복요청 확인, 거래 시작 로그, PROCESSING 상태 저장 후 업무 처리로 들어가도록 정의되어 있습니다. HTTP/JSON 표준에서도 JSON Header는 `serviceId`, `transactionId`, `userId`, `branchId` 같은 업무 전문용 정보이고, Body는 고객번호·조회조건 같은 업무 데이터로 분리합니다.

---

## 2. 전체 구조

```text
[WebTopSuite / UI / API]
        │
        │ POST /{업무코드}/online
        │ JSON Header + Body
        ▼
[Apache / Tomcat]
        ▼
[OnlineTransactionController]
        ▼
[HeaderTransactionControlFilter]   ← 목표: STF.preProcess()와 동일 책임
        │
        ├─ 1. 표준 Header 추출
        ├─ 2. GUID / TraceId 생성·검증
        ├─ 3. serviceId / transactionCode 검증
        ├─ 4. channelId / businessCode 검증
        ├─ 5. 세션 사용자와 Header 사용자 일치 검증
        ├─ 6. 권한 / 데이터권한 / 마스킹등급 결정
        ├─ 7. 거래 가능시간 / 점검 / 차단 여부 확인
        ├─ 8. IdempotencyKey / GUID 기준 중복요청 확인
        ├─ 9. Timeout 정책 결정
        ├─ 10. 거래상태 PROCESSING 저장
        ▼
[Transaction Dispatcher]
        │ serviceId 기준 Handler 선택
        ▼
[Business Handler]
        ▼
[Facade → Service → Rule → DAO/MyBatis]
        ▼
[HeaderTransactionPostProcessor]   ← 목표: ETF.success/fail
        │
        ├─ SUCCESS / FAIL / TIMEOUT / UNKNOWN 상태 갱신
        ├─ 표준 응답 Header 생성
        ├─ 오류코드 매핑
        ├─ 데이터 마스킹
        ├─ 거래 종료 로그
        ├─ 감사 로그
        └─ 성능 메트릭 기록
```

**현재 구현:** `HeaderTransactionControlFilter` 명칭 대신 `TCF` → `STF` → `Dispatcher` → `ETF` 파이프라인으로 동일 역할을 분담합니다. ([34-STF.md](34-STF.md), [36-ETF.md](36-ETF.md))

---

## 3. 표준 전문 구조

### 3.1 요청 전문

```json
{
  "header": {
    "systemId": "NSIGHT-MP",
    "businessCode": "SV",
    "serviceId": "SV.Customer.selectSummary",
    "transactionCode": "SV-INQ-0001",
    "processingType": "INQUIRY",
    "guid": "7f9c2e9a-5e12-4e44-9c5a-123456789abc",
    "traceId": "trc-20260627-0001",
    "idempotencyKey": "SV-INQ-0001-U123456-CUST0001-20260627",
    "channelId": "WEBTOP",
    "userId": "U123456",
    "branchId": "001234",
    "roleId": "BRANCH_USER",
    "centerId": "DC1",
    "clientIp": "10.10.10.10",
    "requestTime": "2026-06-27T10:30:00+09:00",
    "screenId": "SVSCR001",
    "menuId": "SVMENU001",
    "languageCode": "ko_KR"
  },
  "body": {
    "customerId": "CUST0000001",
    "baseDate": "20260627",
    "includeAccount": true,
    "includeCampaign": true
  }
}
```

### 3.2 응답 전문

```json
{
  "header": {
    "systemId": "NSIGHT-MP",
    "businessCode": "SV",
    "serviceId": "SV.Customer.selectSummary",
    "transactionCode": "SV-INQ-0001",
    "guid": "7f9c2e9a-5e12-4e44-9c5a-123456789abc",
    "traceId": "trc-20260627-0001",
    "channelId": "WEBTOP",
    "userId": "U123456",
    "branchId": "001234",
    "centerId": "DC1",
    "apId": "sv-ap01",
    "responseTime": "2026-06-27T10:30:01+09:00",
    "elapsedTimeMs": 312
  },
  "result": {
    "status": "SUCCESS",
    "resultCode": "S0000",
    "resultMessage": "정상 처리되었습니다.",
    "errorCode": null
  },
  "security": {
    "maskingLevel": "LEVEL_2",
    "auditRequiredYn": "Y"
  },
  "body": {
    "customerId": "CUST0000001",
    "customerName": "홍*동",
    "grade": "A"
  }
}
```

---

## 4. Header 항목별 거래통제 기준

| Header 항목 | 필수 | 거래통제 용도 | 검증 기준 | 오류 시 처리 |
|-------------|:----:|---------------|-----------|-------------|
| `systemId` | Y | 시스템 식별 | `NSIGHT-MP` 허용 | 시스템 오류 |
| `businessCode` | Y | 업무 WAR/업무영역 식별 | IC, PC, SV, OM 등 허용 | 업무코드 오류 |
| `serviceId` | Y | Handler 선택, 권한, 로그 기준 | 서비스 마스터 등록 여부 | 미등록 서비스 오류 |
| `transactionCode` | Y | 거래 식별, 재처리, 감사 | serviceId와 매핑 일치 | 거래코드 오류 |
| `processingType` | Y | 조회/등록/수정/삭제 구분 | INQUIRY, CREATE, UPDATE, DELETE, DOWNLOAD | 처리구분 오류 |
| `guid` | 조건부 | End-to-End 추적 | 없으면 생성, 있으면 형식/중복 검증 | 중복 또는 형식 오류 |
| `traceId` | 조건부 | AP 내부 추적 | 없으면 생성 | 신규 생성 |
| `idempotencyKey` | 변경거래 필수 | 중복요청 방지 | 등록/수정/삭제/발송 거래는 필수 | 중복요청 오류 |
| `channelId` | Y | 채널별 권한/Timeout/감사 | WEBTOP, ADMIN, API, BATCH 허용 | 채널 오류 |
| `userId` | Y | 사용자 식별 | **세션 사용자와 일치** | 인증 오류 |
| `branchId` | Y | 지점 권한 | 세션 지점 또는 허용 지점 | 권한 오류 |
| `roleId` | Y | 기능권한/마스킹 | 권한 DB 기준 재조회 | 권한 오류 |
| `screenId` | 화면거래 필수 | 화면-거래 매핑 | 메뉴권한과 일치 | 메뉴권한 오류 |
| `menuId` | 화면거래 필수 | 메뉴권한 | Role에 메뉴권한 존재 | 메뉴권한 오류 |
| `clientIp` | Y | 접근통제/감사 | 허용 IP 또는 Proxy IP | 접근 차단 |
| `requestTime` | Y | Replay 방지 | 서버시간 대비 허용오차 | 요청시간 오류 |
| `centerId` | Y | 센터/DR 추적 | DC1, DC2 등 허용 | 센터 식별 오류 |

> `userId`, `branchId`, `roleId`는 클라이언트 값을 그대로 신뢰하지 않고 **서버 세션·권한 DB**로 재검증해야 합니다.

---

## 5. 거래통제 판단 매트릭스

### 5.1 processingType 기준 통제

| processingType | 예시 | 중복통제 | 권한통제 | 감사로그 | 재처리 | Timeout |
|----------------|------|:--------:|:--------:|:--------:|:------:|:-------:|
| INQUIRY | 고객조회 | 선택 | 메뉴/데이터권한 | 고객정보 조회 시 필수 | 보통 불필요 | 3초 |
| CREATE | 캠페인 등록 | 필수 | 기능권한 | 필수 | 조건부 | 5초 |
| UPDATE | 고객 메모 변경 | 필수 | 기능/데이터권한 | 필수 | 조건부 | 5초 |
| DELETE | 대상자 삭제 | 필수 | 기능권한 | 필수 | 신중 | 5초 |
| DOWNLOAD | 엑셀 다운로드 | 필수 | 다운로드권한 | 필수 | 승인 | 별도 |
| SEND | 메시지 발송 | 필수 | 발송권한 | 필수 | 멱등 필수 | 별도 |
| BATCH | 배치 실행 | 필수 | 운영권한 | 필수 | 가능 | 별도 |

### 5.2 channelId 기준 통제

| channelId | 사용자 | 허용 거래 | 보안 수준 | 감사 기준 |
|-----------|--------|-----------|-----------|-----------|
| WEBTOP | 영업점 사용자 | 조회, 상담 | 세션 필수 | 고객조회 감사 |
| ADMIN | 운영자 | 기준정보, 로그 | 관리자 권한 | 관리자 행위 감사 |
| API | 내부/외부 연계 | 등록, 조회 | Token/mTLS | 연계 감사 |
| BATCH | 스케줄러 | 집계, 재처리 | 시스템 계정 | 배치 실행 로그 |
| EAI | 연계 시스템 | 이벤트 | 연계 인증 | 전문 로그 |

---

## 6. 거래 상태 설계

```text
RECEIVED
   ↓
PROCESSING
   ├─ SUCCESS
   ├─ FAIL
   ├─ TIMEOUT
   └─ UNKNOWN
          ↓
       RETRYING
          ↓
       SUCCESS / FAIL
```

| 상태 | 의미 | 처리 기준 |
|------|------|-----------|
| RECEIVED | 요청 수신 | Header만 파싱 |
| PROCESSING | 업무 처리 중 | 업무 진입 전 저장 |
| SUCCESS | 정상 처리 | 중복 요청 시 기존 결과 반환 가능 |
| FAIL | 업무 실패 | 재처리 가능 여부 판단 |
| TIMEOUT | 제한시간 초과 | 상태조회 후 재처리 |
| UNKNOWN | 결과 불명확 | 운영자/보정 배치 |
| RETRYING | 재처리 중 | 중복 재처리 방지 |

---

## 7. DB 테이블 설계

### 7.1 `TCF_TRANSACTION_CONTROL`

거래통제의 중심 테이블.

| 컬럼 | 타입 예시 | 설명 |
|------|-----------|------|
| `TX_ID` | VARCHAR(40) | 내부 거래 ID |
| `GUID` | VARCHAR(64) | End-to-End 추적 |
| `TRACE_ID` | VARCHAR(64) | AP 내부 추적 |
| `IDEMPOTENCY_KEY` | VARCHAR(200) | 중복요청 Key |
| `SYSTEM_ID` | VARCHAR(20) | 시스템 ID |
| `BUSINESS_CODE` | VARCHAR(10) | 업무코드 |
| `SERVICE_ID` | VARCHAR(100) | 서비스 ID |
| `TRANSACTION_CODE` | VARCHAR(50) | 거래코드 |
| `PROCESSING_TYPE` | VARCHAR(20) | 처리구분 |
| `CHANNEL_ID` | VARCHAR(20) | 채널 |
| `USER_ID` | VARCHAR(50) | 사용자 |
| `BRANCH_ID` | VARCHAR(20) | 지점 |
| `ROLE_ID` | VARCHAR(50) | 권한 |
| `SCREEN_ID` | VARCHAR(50) | 화면 ID |
| `MENU_ID` | VARCHAR(50) | 메뉴 ID |
| `CLIENT_IP` | VARCHAR(50) | 클라이언트 IP |
| `CENTER_ID` | VARCHAR(20) | 센터 ID |
| `AP_ID` | VARCHAR(50) | 처리 AP |
| `REQUEST_HASH` | VARCHAR(128) | Body Hash |
| `STATUS` | VARCHAR(20) | PROCESSING/SUCCESS/FAIL/TIMEOUT/UNKNOWN |
| `RESULT_CODE` | VARCHAR(20) | 결과코드 |
| `ERROR_CODE` | VARCHAR(50) | 오류코드 |
| `ERROR_MESSAGE` | VARCHAR(500) | 오류 메시지 |
| `RETRY_ALLOWED_YN` | CHAR(1) | 재처리 가능 |
| `RETRY_COUNT` | NUMBER | 재처리 횟수 |
| `START_TIME` | TIMESTAMP | 시작시각 |
| `END_TIME` | TIMESTAMP | 종료시각 |
| `ELAPSED_TIME_MS` | NUMBER | 처리시간 |
| `TIMEOUT_AT` | TIMESTAMP | Timeout 기준 |
| `CREATED_AT` | TIMESTAMP | 생성 |
| `UPDATED_AT` | TIMESTAMP | 수정 |

**핵심 인덱스:** `UK_TCF_TX_GUID`, `UK_TCF_TX_IDEMPOTENCY`, `IX_TCF_TX_SERVICE_TIME`, `IX_TCF_TX_USER_TIME`, `IX_TCF_TX_STATUS_TIME`

### 7.2 `TCF_SERVICE_MASTER`

Header `serviceId` 검증 기준. **현재 OM:** `OM_SERVICE_CATALOG` + `TransactionDispatcher` Handler Map.

### 7.3 `TCF_AUTH_MAPPING`

serviceId ↔ menuId ↔ roleId ↔ functionCode ↔ dataScope ↔ maskingLevel. **현재 OM:** `OM_FUNCTION_AUTH`, `OM_DATA_AUTH`.

### 7.4 `TCF_TIMEOUT_POLICY`

거래별 Timeout. **현재:** `OM_SERVICE_CATALOG.TIMEOUT_SEC`, Facade `@Timeout` (부분).

### 7.5 `TCF_AUDIT_LOG`

감사 대상 거래. **현재 OM:** `OM_AUDIT_LOG` + ETF `auditLogService`.

---

## 8. 처리 로직 상세

### 8.1 전처리 (STF)

1. JSON 수신 → header 존재 확인  
2. Header 필수값 검증  
3. GUID/TraceId 생성  
4. serviceId 등록·transactionCode 매핑 확인  
5. channelId 허용 확인  
6. 세션 조회 → Header userId/branchId 일치  
7. 메뉴/기능/데이터 권한 확인  
8. maskingLevel·auditRequiredYn 결정  
9. idempotencyKey/GUID 중복 확인  
10. Timeout 정책 결정  
11. PROCESSING 저장  
12. MDC 적재 → Handler 호출  

### 8.2 후처리 (ETF)

1. 업무 결과 수신  
2. resultCode·errorCode 매핑  
3. 응답 Header 생성  
4. Body 마스킹  
5. SUCCESS/FAIL 갱신  
6. 감사·메트릭  
7. MDC 정리  

---

## 9. 중복요청 처리 기준

| 상황 | 판단 | 응답 |
|------|------|------|
| 신규 | GUID/IdempotencyKey 없음 | PROCESSING 후 처리 |
| 처리 중 | PROCESSING | "처리 중" |
| 성공 | SUCCESS | 기존 결과 반환 |
| 실패 | FAIL | 재처리 판단 |
| Timeout 후 | TIMEOUT/UNKNOWN | 상태조회 후 재처리 |
| 변경거래 중복 | 동일 idempotencyKey | 재처리 금지 |
| 조회 중복 | 동일 GUID | 허용(과다 요청 제한) |

---

## 10. 오류코드 체계

| 영역 | 코드 예시 | 조건 |
|------|-----------|------|
| Header | `E-HDR-0001` | Header 없음 |
| Header 필수 | `E-HDR-0002` | serviceId 누락 |
| 미등록 서비스 | `E-SVC-0001` | serviceId 미등록 |
| 거래코드 | `E-TX-0001` | serviceId·transactionCode 불일치 |
| 인증 | `E-AUTH-0001` | 세션 없음 |
| 권한 | `E-AUTHZ-0001` | 메뉴권한 없음 |
| 데이터권한 | `E-AUTHZ-0002` | 타 지점 조회 |
| 중복 | `E-DUP-0001` | idempotencyKey 처리 중 |
| Timeout | `E-TIME-0001` | 온라인 Timeout |

**현재 구현:** `ErrorCode` (`E-COM-*`) — [05-exception.md](05-exception.md)

---

## 11. Java 패키지 설계 (목표)

```text
com.nh.nsight.tcf
 ├─ header          StandardHeader, StandardHeaderValidator, HeaderContext
 ├─ transaction     TransactionControlService, TransactionStatus
 ├─ service         ServiceRegistry, ServiceMaster
 ├─ security        SessionValidator, AuthorizationChecker, DataAuthorityChecker
 ├─ idempotency     IdempotencyProcessor
 ├─ timeout         TimeoutPolicyResolver
 ├─ audit           AuditLogService
 ├─ response        StandardResponse, ResponseBuilder
 └─ error           ErrorCode, BusinessException
```

**현재:** 대부분 `tcf-core`에 분산 (`processor/STF`, `processor/ETF`, `validation/`, `security/`, `idempotency/`, `logging/`).

---

## 12. 핵심 클래스 책임

| 클래스 | 책임 |
|--------|------|
| `StandardHeader` | Header 데이터 구조 |
| `StandardHeaderValidator` | 필수값·형식 검증 |
| `STF` / `TransactionControlFilter` | 전처리 오케스트레이션 |
| `TransactionDispatcher` | serviceId → Handler |
| `IdempotencyChecker` | GUID/IdempotencyKey 중복 |
| `SessionValidator` | 세션·사용자 일치 |
| `AuthorizationValidator` | 권한 (확장 필요) |
| `TransactionLogService` | 거래 시작/종료 로그 |
| `ETF` | 후처리·감사·메트릭 |

---

## 13. Header 기반 거래통제 의사코드

```java
public StandardResponse<?> control(StandardRequest<?> request) {
    StandardHeader header = request.getHeader();
    headerValidator.validateRequired(header);
    traceManager.prepare(header);
    ServiceMaster service = serviceRegistry.getService(header.getServiceId());
    serviceValidator.validateMapping(header, service);
    SessionUser sessionUser = sessionValidator.validate();
    authenticationValidator.validate(header, sessionUser);
    AuthorizationResult auth = authorizationChecker.check(header, sessionUser, service);
    idempotencyProcessor.check(header, request);
    TimeoutPolicy timeoutPolicy = timeoutPolicyResolver.resolve(service, header);
    TransactionControl tx = transactionStatusService.start(header, request, service, timeoutPolicy);
    try {
        Object result = dispatcher.dispatch(request);
        transactionStatusService.success(tx, result);
        auditLogService.writeIfRequired(header, auth, result);
        return responseBuilder.success(header, result, auth.getMaskingLevel());
    } catch (BusinessException e) {
        transactionStatusService.fail(tx, e);
        return responseBuilder.businessFail(header, e);
    } finally {
        mdcManager.clear();
    }
}
```

---

## 14. OM 운영관리 화면

| 화면 | 기능 | 현재 OM |
|------|------|---------|
| ServiceId 관리 | serviceId, Handler, 사용 | ✅ `service-catalog.html` |
| 거래코드 관리 | transactionCode, Timeout | △ 카탈로그 일부 |
| 권한 매핑 | serviceId ↔ menu ↔ role | ✅ `user-auth.html` (기능·데이터권한) |
| 거래로그 | GUID, serviceId, 오류 | ✅ `transaction-log.html` |
| 거래상태 | PROCESSING/SUCCESS/… | △ 로그 기반, 전용 상태 화면 없음 |
| 재처리 관리 | UNKNOWN/TIMEOUT | ❌ 미구현 |
| 감사로그 | 고객조회, 다운로드 | ✅ `audit-log.html` |
| 오류코드 | Header/Auth/System | ✅ `error-code.html` |
| Timeout 정책 | 거래별 Timeout | ❌ 전용 화면 없음 |

---

## 15. 구현 우선순위

| 순서 | 항목 | 이유 |
|:--:|------|------|
| 1 | StandardHeader / Request / Response | 기본 구조 |
| 2 | StandardHeaderValidator | 거래통제 시작 |
| 3 | ServiceRegistry / SERVICE_MASTER | serviceId 검증 |
| 4 | TransactionControl / TRANSACTION_CONTROL | 거래상태 |
| 5 | GUID / Trace / MDC | 추적 |
| 6 | Session / Auth / Authorization | 보안 |
| 7 | IdempotencyProcessor | 중복 방지 |
| 8 | TimeoutPolicyResolver | 장애 확산 방지 |
| 9 | AuditLogService | 감사 |
| 10 | OM 거래통제 화면 | 운영 |

---

## 16. 최종 설계 판단

| 설계 원칙 | 적용 기준 |
|-----------|-----------|
| 거래 식별 | `guid + serviceId + transactionCode` |
| 업무 분기 | `businessCode + serviceId` |
| 권한 통제 | `serviceId + menuId + roleId + dataScope` |
| 중복 방지 | `idempotencyKey`, 없으면 `guid` |
| 감사 | `processingType + serviceId + customerDataYn` |
| Timeout | serviceId별 정책 |
| 재처리 | `transactionStatus + retryPolicy` |
| 로그 추적 | `guid + traceId + serviceId + userId` |

### 통제 원칙 (운영 규칙)

```text
Header 검증 없이 업무 진입 금지
serviceId 등록 없이 업무 진입 금지
권한 검증 없이 업무 진입 금지
거래상태 PROCESSING 저장 없이 업무 진입 금지
변경거래는 idempotencyKey 없이 업무 진입 금지
Timeout/UNKNOWN 거래는 상태조회 없이 재처리 금지
```

---

## 17. 현재 코드베이스 대응 (2026-06)

**적용 범위 (2026-06-25 확정):** Header 7개 필드 허용 목록(whitelist) 방식. [40-header-7-transaction-control.md](40-header-7-transaction-control.md) 참고.

| 설계 항목 | 현재 구현 | 갭 |
|-----------|-----------|-----|
| Header 7필드 거래통제 | `TransactionControlService.check()` | STF에서 필수값 검증 + `TCF_TRANSACTION_CONTROL` COUNT 조회 |
| 거래통제 테이블 | H2 `TCF_TRANSACTION_CONTROL` | 7컬럼 복합 PK (허용 목록). STATUS/GUID 등 상태 컬럼 없음 |
| Header JSON | `StandardHeader` | `user`/`branch` JsonAlias → `userId`/`branchId` |
| STF 전처리 | `STF.preProcess()` | 권한·Timeout·카탈로그 매핑은 미적용 |
| 멱등성 | `InMemoryIdempotencyChecker` | 거래통제와 별개 |
| 거래 로그 | `TransactionLogService` → `TCF_TX_LOG` | 사후 로그 (통제와 분리) |
| OM 시드 | `TransactionControlSeedData` | 샘플·주요 거래 허용 Row MERGE |
| OM 화면 | — | 거래통제 CRUD 화면 없음 |

---

## 18. 관련 소스

| 영역 | 경로 |
|------|------|
| STF | `tcf-core/.../processor/STF.java` |
| Header | `tcf-core/.../message/StandardHeader.java` |
| 거래통제 | `tcf-core/.../control/TransactionControlService.java` |
| 거래통제 JDBC | `tcf-web/.../control/JdbcTransactionControlRepository.java` |
| OM 시드 | `tcf-om/.../support/TransactionControlSeedData.java` |
