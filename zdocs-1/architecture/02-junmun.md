# 02. 표준 전문(준문) 구조

| 항목 | 내용 |
|------|------|
| 문서 번호 | 02 |
| 제목 | Standard Message (Junmun) Specification |
| 상위 문서 | [architecture.md](architecture.md) |
| 구현 모듈 | `tcf-core` → `com.nh.nsight.tcf.core.support.message` |
| 대상 | 채널·업무·OM 개발자, 연동 테스트 담당자 |

---

## 1. 개요

NSIGHT TCF Framework에서 **전문(준문)** 이란 온라인 거래를 주고받는 **표준 JSON 메시지**를 말한다.

| 구분 | 클래스 | JSON 루트 |
|------|--------|-----------|
| 요청 전문 | `StandardRequest<T>` | `header` + `body` |
| 응답 전문 | `StandardResponse<T>` | `header` + `result` + `body` |

```text
요청 (Client → Server)                응답 (Server → Client)
┌─────────────────────┐              ┌─────────────────────┐
│ header  (공통 헤더)  │              │ header  (공통 헤더)  │
│ body    (업무 데이터)│              │ result  (처리 결과)  │
└─────────────────────┘              │ body    (업무 데이터)│
                                       └─────────────────────┘
```

전송 규약:

| 항목 | 값 |
|------|-----|
| Protocol | HTTP/1.1 |
| Method | **POST** (`GET /online` 불가) |
| Content-Type | `application/json; charset=UTF-8` |
| Endpoint | `/online` 또는 `/{businessCode}/online` |
| HTTP Status | 성공·실패 모두 주로 **200 OK** (결과는 `result.resultCode`로 판별) |

---

## 2. 전문 처리 흐름

```text
1. Client — StandardRequest JSON 작성·전송
2. OnlineTransactionController — businessCode·clientIp 보정
3. STF — Header 검증·정규화, guid/traceId 부여, 세션·권한·멱등성 ([34-STF.md](34-STF.md))
4. Dispatcher — header.serviceId → Handler
5. Handler/Service — body 처리, 응답 body Map 조립
6. ETF — StandardResponse 생성 (result + body)
7. Client — result.resultCode == "S0000" 이면 성공
```

요청 Header는 STF 단계에서 **정규화·보완**되고, 응답 Header는 동일 거래의 guid/traceId를 유지한다.

---

## 3. 요청 전문 (StandardRequest)

### 3.1 JSON 구조

```json
{
  "header": { ... },
  "body": { ... }
}
```

| 필드 | 타입 | 설명 |
|------|------|------|
| `header` | Object | `StandardHeader` — 거래 공통 메타 |
| `body` | Object | 업무별 가변 필드 (`Map<String, Object>`) |

Java 타입: `StandardRequest<Map<String, Object>>`

### 3.2 요청 Header 필드 (`StandardHeader`)

| 필드 | 필수 | 설명 | 비고 |
|------|:----:|------|------|
| `systemId` | △ | 연계 시스템 ID | 미입력 시 `NSIGHT-MP` (normalize) |
| `businessCode` | ● | 업무 코드 | CC, SV, OM … 대문자 정규화 |
| `serviceId` | ● | 거래 식별자 | Dispatcher 라우팅 키 |
| `transactionCode` | ● | 화면·거래 코드 | 예: `SV-INQ-0001` |
| `processingType` | ● | 처리 유형 | `INQUIRY`, `CREATE` … 대문자 정규화 |
| `channelId` | ● | 채널 ID | 예: `WEBTOP`, `OM-PORTAL` |
| `guid` | △ | 거래 GUID | 빈 값이면 STF가 자동 생성 |
| `traceId` | △ | 추적 ID | 빈 값이면 STF가 자동 생성 |
| `userId` | △ | 사용자 ID | 세션·권한·감사에 사용 |
| `branchId` | △ | 부점 코드 | |
| `centerId` | △ | 센터 코드 | |
| `requestTime` | △ | 요청 시각 (ISO-8601) | 미입력 시 서버 현재 시각 |
| `clientIp` | △ | 클라이언트 IP | 미입력 시 Controller가 HttpServletRequest에서 추출 |
| `idempotencyKey` | △ | 멱등 키 | 중복 요청 방지 (설정 시) |

● STF `StandardHeaderValidator` 필수 검증  
△ 선택 — 프레임워크 또는 Controller가 보완

**serviceId 명명 규칙**

```text
{BusinessCode}.{Domain}.{action}

예) SV.Sample.inquiry
    OM.User.save
    OM.Dashboard.inquiry
```

### 3.3 processingType (처리 유형)

프레임워크 enum (`ProcessingType`)과 JSON 문자열 값:

| 값 | 의미 | 사용 예 |
|----|------|---------|
| `INQUIRY` | 조회 | 목록·단건 조회 |
| `CREATE` | 등록 | 신규 저장 |
| `UPDATE` | 수정 | 기존 데이터 변경 |
| `DELETE` | 삭제 | 논리·물리 삭제 |
| `EXECUTE` | 실행 | 배치·승인·처리 실행 |
| `DOWNLOAD` | 다운로드 | 파일·리포트 |
| `UPLOAD` | 업로드 | 파일 전송 |

JSON에는 enum **이름 문자열**을 사용한다 (`"INQUIRY"`).

### 3.4 요청 Body

업무·화면마다 자유롭게 정의한다. 공통 스키마는 없으며, Rule/Service에서 검증한다.

**샘플 (SV 조회)**

```json
{
  "header": {
    "systemId": "NSIGHT-MP",
    "businessCode": "SV",
    "serviceId": "SV.Sample.inquiry",
    "transactionCode": "SV-INQ-0001",
    "processingType": "INQUIRY",
    "guid": "",
    "traceId": "",
    "channelId": "WEBTOP",
    "userId": "U123456",
    "branchId": "001234",
    "centerId": "DC1",
    "requestTime": "2026-06-14T10:30:00+09:00",
    "clientIp": "10.10.10.10"
  },
  "body": {
    "sampleKey": "SV-SAMPLE",
    "baseDate": "20260614"
  }
}
```

샘플 파일 위치: `tcf-ui/src/main/resources/sample-requests/{code}-sample-inquiry.json`

### 3.5 샘플 JSON의 확장 Header 필드

일부 샘플 JSON에는 아래 필드가 포함되어 있으나, **`StandardHeader` 클래스에는 정의되어 있지 않다**.

| 샘플 전용 필드 | 설명 |
|----------------|------|
| `transactionIntime` | 채널 측 요청 시각 (레거시·UI 표시용) |
| `transactionOuttime` | 채널 측 응답 시각 |
| `systemDate` | 시스템 일자 (yyyyMMdd) |
| `bizDate` | 영업 일자 (yyyyMMdd) |

Jackson 역직렬화 시 **미매핑 필드는 무시**되며, 서버 TCF 파이프라인에는 영향을 주지 않는다.  
서버가 인식·로깅하는 Header는 `StandardHeader` 필드만 해당한다.

---

## 4. 응답 전문 (StandardResponse)

### 4.1 JSON 구조

```json
{
  "header": { ... },
  "result": { ... },
  "body": { ... }
}
```

| 필드 | 타입 | 설명 |
|------|------|------|
| `header` | Object | 요청 Header 기반 (guid/traceId 유지) |
| `result` | Object | `Result` — 처리 결과 코드·메시지 |
| `body` | Object | 업무 응답 데이터 (실패 시 null 가능) |

Java 타입: `StandardResponse<Object>`

### 4.2 Result 필드

| 필드 | 성공 시 | 실패 시 | 설명 |
|------|---------|---------|------|
| `resultCode` | `S0000` | `E0001` | 처리 결과 코드 |
| `resultMessage` | `정상 처리되었습니다.` | `처리 중 오류가 발생했습니다.` | 요약 메시지 |
| `errorCode` | — | `E-COM-...` | 상세 오류 코드 |
| `errorMessage` | — | 오류 설명 | 사용자·화면용 메시지 |
| `errorDetail` | — | 기술 상세 | 예외 클래스명 등 |
| `errorSystemId` | — | `NSIGHT-MP` | 오류 발생 시스템 |
| `errorDateTime` | — | ISO-8601 | 오류 발생 시각 |

**성공 판별**

```text
response.result.resultCode == "S0000"
```

HTTP 4xx/5xx가 아닌 **200 OK + resultCode** 조합으로 성공·실패를 구분하는 것이 NSIGHT TCF의 기본 계약이다.

### 4.3 응답 Body 예시 (SV 조회 성공)

```json
{
  "header": {
    "systemId": "NSIGHT-MP",
    "businessCode": "SV",
    "serviceId": "SV.Sample.inquiry",
    "transactionCode": "SV-INQ-0001",
    "processingType": "INQUIRY",
    "guid": "a1b2c3d4-....",
    "traceId": "TRC-....",
    "channelId": "WEBTOP",
    "userId": "U123456"
  },
  "result": {
    "resultCode": "S0000",
    "resultMessage": "정상 처리되었습니다."
  },
  "body": {
    "businessCode": "SV",
    "serviceId": "SV.Sample.inquiry",
    "guid": "a1b2c3d4-....",
    "data": {
      "sampleKey": "SV-SAMPLE",
      "sampleName": "SingleView sample response"
    }
  }
}
```

응답 Body 구조는 **Service가 조립**한다. 공통 관례로 `businessCode`, `serviceId`, `guid`, `data`를 포함하는 패턴이 샘플에 사용된다.

### 4.4 오류 응답 예시

```json
{
  "header": {
    "serviceId": "SV.Sample.inquiry",
    "guid": "a1b2c3d4-...."
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

## 5. 공통 오류 코드 (ErrorCode)

프레임워크 공통 오류는 `com.nh.nsight.tcf.core.support.error.ErrorCode`에 정의된다.

| 상수 | 코드 | 발생 시점 |
|------|------|-----------|
| `INVALID_HEADER` | `E-COM-VALID-0001` | Header 누락·형식 오류 |
| `SERVICE_NOT_FOUND` | `E-COM-DISP-0001` | 미등록 serviceId |
| `SESSION_INVALID` | `E-COM-AUTH-0001` | 세션 검증 실패 |
| `AUTHORIZATION_DENIED` | `E-COM-AUTH-0002` | 권한 없음 |
| `DUPLICATE_REQUEST` | `E-COM-IDEMP-0001` | 멱등 중복 요청 |
| `BUSINESS_ERROR` | `E-COM-BIZ-0001` | 업무 Rule·Service 예외 |
| `SYSTEM_ERROR` | `E-COM-SYS-0001` | 예기치 않은 시스템 오류 |

업무별·OM 상세 오류코드는 `OM_ERROR_CODE` 마스터 및 `tcf-om` Handler에서 별도 관리한다.

---

## 6. 식별자·추적

### 6.1 GUID / TraceId

| 필드 | 생성 | 용도 |
|------|------|------|
| `guid` | STF — `GuidGenerator.newGuid()` | 거래 단위 고유 ID, 거래로그·MDC |
| `traceId` | STF — `GuidGenerator.newTraceId()` | 분산 추적·로그 상관 |

클라이언트가 빈 문자열(`""`)로내면 서버가 채운다.  
동일 guid로 재요청 시 `idempotencyKey`와 함께 멱등 처리된다.

### 6.2 MDC 로깅 키

STF가 다음 값을 SLF4J MDC에 적재한다.

```text
guid, traceId, serviceId, userId, branchId
```

---

## 7. HTTP 엔드포인트와 Header 보정

### 7.1 URL 패턴

| 패턴 | 예 | 설명 |
|------|-----|------|
| `POST /online` | bootRun `http://localhost:8086/online` | `header.businessCode` 필수 |
| `POST /{code}/online` | `http://localhost:8080/sv/online` | path의 `{code}` → `businessCode` 자동 설정 |

`OnlineTransactionController` 동작:

1. `header`가 null이면 빈 `StandardHeader` 생성
2. path `businessCode`가 있고 Header에 없으면 **자동 설정**
3. `clientIp` 없으면 `X-Forwarded-For` 또는 `remoteAddr` 설정
4. `TCF.process(request)` 호출

### 7.2 호출 예 (curl)

```bash
curl -X POST http://localhost:8080/sv/online \
  -H "Content-Type: application/json" \
  -d @tcf-ui/src/main/resources/sample-requests/sv-sample-inquiry.json
```

---

## 8. 비표준 진입 — TcfGateway

`POST /online` JSON이 아닌 REST·multipart 호출도 TCF 전문으로 변환할 수 있다.

```java
StandardResponse<Object> response = tcfGateway.invoke(
    TcfInvokeRequest.builder("UD.File.list", "UD-LST-0001", "INQUIRY")
        .body(bodyMap)
        .userId(userId)
        .clientIp(clientIp)
        .build()
);
```

`TcfGateway.buildHeader()`가 `StandardHeader`를 조립한 뒤 동일한 `TCF.process()` 파이프라인을 탄다.

| 자동 설정 | 값 |
|-----------|-----|
| `systemId` | `NSIGHT-MP` |
| `businessCode` | serviceId 첫 토큰 (예: `UD.File.list` → `UD`) |
| `channelId` | 기본 `WEBTOP` |

---

## 9. Java 모델 참조

### 9.1 클래스 다이어그램 (논리)

```text
StandardRequest<T>
  ├─ StandardHeader header
  └─ T body

StandardResponse<T>
  ├─ StandardHeader header
  ├─ Result result
  └─ T body

StandardHeader
  ├─ systemId, businessCode, serviceId, transactionCode
  ├─ processingType, guid, traceId, channelId
  ├─ userId, branchId, centerId
  ├─ requestTime, clientIp, idempotencyKey
  └─ normalize()  // 기본값·대문자 정규화

Result
  ├─ resultCode, resultMessage
  └─ errorCode, errorMessage, errorDetail, errorSystemId, errorDateTime
```

### 9.2 팩토리 메서드

```java
// 성공 응답
StandardResponse.success(header, body);

// 실패 응답
StandardResponse.fail(header, errorCode, message, detail);

// Result 단독
Result.success();   // S0000
Result.fail(errorCode, message, detail);  // E0001
```

---

## 10. 채널별 전문 작성 가이드

### 10.1 tcf-ui 거래 테스트

1. `/{code}/index.html` 또는 `message-composer.html`에서 Header·Body 편집
2. `POST /api/relay/{code}/online` → tcf-ui가 업무 WAR로 Relay
3. 응답 JSON의 `result.resultCode` 확인

### 10.2 OM Admin (tcf-ui → tcf-om)

| 항목 | 값 예 |
|------|-------|
| URL | `/om/online` (Relay 경유) |
| serviceId | `OM.Dashboard.inquiry`, `OM.User.save` … |
| channelId | `OM-PORTAL` |
| userId | 로그인 세션 사용자 |

### 10.3 신규 거래 등록 시 전문 체크리스트

- [ ] `serviceId` — Handler와 일치
- [ ] `businessCode` — URL path·업무 코드 일치
- [ ] `transactionCode` — `OM_SERVICE_CATALOG` 또는 화면 정의와 일치
- [ ] `processingType` — enum 값 중 하나
- [ ] `channelId` — 채널 식별
- [ ] Body — Rule 검증 조건 충족
- [ ] 샘플 JSON — `sample-requests/` 추가

---

## 11. 전문과 로그·DB

| 구분 | 저장·기록 |
|------|-----------|
| 거래 시작/종료 | `TransactionLogService` → H2 거래로그 (`nsight.txlog.path`) |
| 감사 | `AuditLogService` |
| 메트릭 | `TransactionMetricService` |
| OM 조회 | `OM.TransactionLog.inquiry` — 동일 로그 DB 조회 |

Header의 `guid`, `serviceId`, `userId`가 로그·감사 레코드의 핵심 키이다.

---

## 12. 관련 문서

| 문서 | 설명 |
|------|------|
| [architecture.md](architecture.md) | 전체 아키텍처 |
| [01-application-layer.md](01-application-layer.md) | Handler·Service 계층 |
| [03-transaction.md](03-transaction.md) | 트랜잭션 처리 아키텍처 |
| [04-messaging.md](04-messaging.md) | 메시지 처리·릴레이 |
| [05-exception.md](05-exception.md) | 예외 처리 표준 |
| [TCF_FRAMEWORK_GUIDE.md](../TCF_FRAMEWORK_GUIDE.md) | Handler 개발 가이드 |
| [tcf-ui/sample-requests](../../tcf-ui/src/main/resources/sample-requests/) | 업무별 샘플 전문 |

---

## 13. 변경 이력

| 일자 | 변경 내용 |
|------|-----------|
| 2026-06 | 최초 작성 — `tcf-core.message` 기준 |
