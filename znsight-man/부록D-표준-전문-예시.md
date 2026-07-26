# 부록 D. 표준 전문 예시

> **NSIGHT TCF 개발 Manual** · 원본: [`znsight-guide-word`](../znsight-guide-word/) · 갱신: 2026-07-05

## D. 표준 전문 예시

### D.1 도입 전 안내말

본 부록은 NSIGHT TCF Framework에서 사용하는 표준 요청 전문과 표준 응답 전문의 실제 JSON 예시를 정의한다.
NSIGHT TCF는 REST Resource 방식이 아니라 HTTP/JSON 표준 전문 + ServiceId Dispatcher 방식을 기준으로 한다. URL은 업무 Context를 식별하고, 실제 실행 업무는 JSON Header의 serviceId로 결정한다. 표준 전문 구조는 요청 전문 header + body, 응답 전문 header + result + body를 기본으로 한다.
화면 / WebTopSuite / 외부 API
```text
        ↓
```

POST /{businessCode}/online
```text
        ↓
StandardRequest
```

  - header
  - body
```text
        ↓
TCF.process()
  - STF 전처리
  - TransactionDispatcher
  - Handler
  - ETF 후처리
        ↓
StandardResponse
  - header
  - result
  - body

```

### D.2 표준 전문 기본 구조

#### D.2.1 요청 전문 구조

```json
{
  "header": {
    "systemId": "NSIGHT-MP",
    "businessCode": "SV",
    "serviceId": "SV.Customer.selectSummary",
    "transactionCode": "SV-INQ-0001",
    "processingType": "INQUIRY",
    "serviceName": "고객 요약 조회",
    "guid": "GUID-20260705-000001",
    "traceId": "TRC-20260705-000001",
    "channelId": "WEBTOP",
    "userId": "U123456",
    "branchId": "001234",
    "centerId": "DC1",
    "requestTime": "2026-07-05T10:30:00+09:00",
    "clientIp": "10.10.10.10",
    "idempotencyKey": "SV-INQ-0001-U123456-20260705-000001"
  },
  "body": {
    "customerNo": "CUST0000001"
  }
}
```

#### D.2.2 응답 전문 구조

```json
{
  "header": {
    "systemId": "NSIGHT-MP",
    "businessCode": "SV",
    "serviceId": "SV.Customer.selectSummary",
    "transactionCode": "SV-INQ-0001",
    "processingType": "INQUIRY",
    "serviceName": "고객 요약 조회",
    "guid": "GUID-20260705-000001",
    "traceId": "TRC-20260705-000001",
    "channelId": "WEBTOP",
    "userId": "U123456",
    "branchId": "001234",
    "centerId": "DC1",
    "requestTime": "2026-07-05T10:30:00+09:00",
    "responseTime": "2026-07-05T10:30:00.250+09:00",
    "elapsedTimeMs": 250
  },
  "result": {
    "resultCode": "S0000",
    "resultStatus": "SUCCESS",
    "resultMessage": "정상 처리되었습니다.",
    "errorCode": null,
    "errorMessage": null,
    "errorDetail": null,
    "errorSystemId": null,
    "errorDateTime": null
  },
  "body": {
    "customerNo": "CUST0000001",
    "customerName": "홍길동"
  }
}
```

### D.3 Header 표준 항목

| 항목 | 필수 | 설명 | 예시 |
| --- | --- | --- | --- |
| systemId | N | 요청 시스템 ID | NSIGHT-MP |
| businessCode | Y | 업무코드 | SV |
| serviceId | Y | 실행할 ServiceId | SV.Customer.selectSummary |
| transactionCode | Y | 거래코드 | SV-INQ-0001 |
| processingType | Y | 처리유형 | INQUIRY |
| serviceName | Y | 서비스명 | 고객 요약 조회 |
| guid | N | End-to-End 거래 추적 ID | GUID-20260705-000001 |
| traceId | N | 시스템 내부 Trace ID | TRC-20260705-000001 |
| channelId | Y | 호출 채널 | WEBTOP |
| userId | 조건부 | 사용자 ID | U123456 |
| branchId | 조건부 | 지점 코드 | 001234 |
| centerId | N | 센터 코드 | DC1 |
| requestTime | N | 요청 시각 | 2026-07-05T10:30:00+09:00 |
| clientIp | N | 클라이언트 IP | 10.10.10.10 |
| idempotencyKey | N | 중복요청 방지 키 | SV-INQ-0001-U123456-20260705-000001 |

Header는 거래 실행, 거래통제, 권한검증, Timeout 정책, 거래로그, 감사로그의 기준이 된다. 기존 표준 전문 설계에서도 businessCode, serviceId, transactionCode, processingType, channelId를 주요 Header로 두고, guid, traceId, userId, branchId, clientIp, idempotencyKey 등을 포함하도록 정리한다.

### D.4 SV 고객요약조회 요청 예시

| 항목 | 값 |
| --- | --- |
| 업무코드 | SV |
| Endpoint | POST /sv/online |
| ServiceId | SV.Customer.selectSummary |
| 거래코드 | SV-INQ-0001 |
| 처리유형 | INQUIRY |
| 설명 | 고객번호 기준 고객 요약정보 조회 |
| { | "header": { |
| "systemId": "NSIGHT-MP", | "businessCode": "SV", |
| "serviceId": "SV.Customer.selectSummary", | "transactionCode": "SV-INQ-0001", |
| "processingType": "INQUIRY", | "serviceName": "고객 요약 조회", |
| "guid": "", | "traceId": "", |
| "channelId": "WEBTOP", | "userId": "U123456", |
| "branchId": "001234", | "centerId": "DC1", |
| "requestTime": "2026-07-05T10:30:00+09:00", | "clientIp": "10.10.10.10", |
| "idempotencyKey": null | }, |
| "body": { | "customerNo": "CUST0000001", |
| "baseDate": "20260705", | "includeAccount": true, |
| "includeCampaign": true | } |

```java
}
```

### D.5 SV 고객요약조회 성공 응답 예시

```json
{
  "header": {
    "systemId": "NSIGHT-MP",
    "businessCode": "SV",
    "serviceId": "SV.Customer.selectSummary",
    "transactionCode": "SV-INQ-0001",
    "processingType": "INQUIRY",
    "serviceName": "고객 요약 조회",
    "guid": "GUID-20260705-000001",
    "traceId": "TRC-20260705-000001",
    "channelId": "WEBTOP",
    "userId": "U123456",
    "branchId": "001234",
    "centerId": "DC1",
    "requestTime": "2026-07-05T10:30:00+09:00",
    "responseTime": "2026-07-05T10:30:00.250+09:00",
    "elapsedTimeMs": 250
  },
  "result": {
    "resultCode": "S0000",
    "resultStatus": "SUCCESS",
    "resultMessage": "정상 처리되었습니다.",
    "errorCode": null,
    "errorMessage": null,
    "errorDetail": null,
    "errorSystemId": null,
    "errorDateTime": null
  },
  "body": {
    "customerNo": "CUST0000001",
    "customerName": "홍길동",
    "customerGrade": "VIP",
    "branchName": "농협중앙지점",
    "totalBalance": 12500000,
    "lastTransactionDate": "20260704",
    "campaignTargetYn": "Y"
  }
}
```

### D.6 목록조회 요청 예시

목록조회는 body.searchCondition과 body.page를 사용한다.
| 항목 | 값 |
| --- | --- |
| 업무코드 | CM |
| Endpoint | POST /cm/online |
| ServiceId | CM.Campaign.selectList |
| 거래코드 | CM-INQ-0001 |
| 처리유형 | INQUIRY |
| 설명 | 캠페인 목록 조회 |
| { | "header": { |
| "systemId": "NSIGHT-MP", | "businessCode": "CM", |
| "serviceId": "CM.Campaign.selectList", | "transactionCode": "CM-INQ-0001", |
| "processingType": "INQUIRY", | "serviceName": "캠페인 목록 조회", |
| "guid": "", | "traceId": "", |
| "channelId": "WEBTOP", | "userId": "U123456", |
| "branchId": "001234", | "requestTime": "2026-07-05T11:00:00+09:00" |
| }, | "body": { |
| "searchCondition": { | "campaignName": "우대금리", |
| "campaignStatus": "ACTIVE", | "fromDate": "20260701", |
| "toDate": "20260731" | }, |
| "page": { | "pageNo": 1, |
| "pageSize": 100 | } |

```java
}
}
```

### D.7 목록조회 성공 응답 예시

목록조회 응답은 body.list와 body.page를 함께 반환한다.
```json
{
  "header": {
    "systemId": "NSIGHT-MP",
    "businessCode": "CM",
    "serviceId": "CM.Campaign.selectList",
    "transactionCode": "CM-INQ-0001",
    "processingType": "INQUIRY",
    "serviceName": "캠페인 목록 조회",
    "guid": "GUID-20260705-000002",
    "traceId": "TRC-20260705-000002",
    "channelId": "WEBTOP",
    "userId": "U123456",
    "branchId": "001234",
    "requestTime": "2026-07-05T11:00:00+09:00",
    "responseTime": "2026-07-05T11:00:00.430+09:00",
    "elapsedTimeMs": 430
  },
  "result": {
    "resultCode": "S0000",
    "resultStatus": "SUCCESS",
    "resultMessage": "정상 처리되었습니다.",
    "errorCode": null,
    "errorMessage": null
  },
  "body": {
    "list": [
      {
        "campaignId": "CMP2026070001",
        "campaignName": "우대금리 대상 고객 캠페인",
        "campaignStatus": "ACTIVE",
        "startDate": "20260701",
        "endDate": "20260731"
      },
      {
        "campaignId": "CMP2026070002",
        "campaignName": "카드 이용 활성화 캠페인",
        "campaignStatus": "ACTIVE",
        "startDate": "20260705",
        "endDate": "20260731"
      }
    ],
    "page": {
      "pageNo": 1,
      "pageSize": 100,
      "totalCount": 250,
      "totalPage": 3,
      "hasNext": true
    }
  }
}
```

페이징 정보는 표준 전문 안에 포함할 수 있지만, TCF Core가 직접 페이징을 처리하지 않고 업무 WAR 내부의 Rule, Service, DAO, MyBatis에서 처리하는 방식도 가능하다.

### D.8 등록 요청 예시

등록·수정·삭제·실행성 거래는 idempotencyKey를 사용하는 것이 좋다.
| 항목 | 값 |
| --- | --- |
| 업무코드 | CM |
| Endpoint | POST /cm/online |
| ServiceId | CM.Campaign.create |
| 거래코드 | CM-CRT-0001 |
| 처리유형 | CREATE |
| 설명 | 캠페인 신규 등록 |
| { | "header": { |
| "systemId": "NSIGHT-MP", | "businessCode": "CM", |
| "serviceId": "CM.Campaign.create", | "transactionCode": "CM-CRT-0001", |
| "processingType": "CREATE", | "serviceName": "캠페인 등록", |
| "guid": "", | "traceId": "", |
| "channelId": "WEBTOP", | "userId": "U123456", |
| "branchId": "001234", | "requestTime": "2026-07-05T11:30:00+09:00", |
| "idempotencyKey": "CM-CRT-0001-U123456-20260705-000001" | }, |
| "body": { | "campaignName": "신규 고객 우대 캠페인", |
| "campaignType": "CUSTOMER", | "targetSegmentId": "SEG2026070001", |
| "startDate": "20260710", | "endDate": "20260731", |
| "description": "신규 고객 대상 우대 캠페인" | } |

```java
}
```

### D.9 등록 성공 응답 예시

```json
{
  "header": {
    "systemId": "NSIGHT-MP",
    "businessCode": "CM",
    "serviceId": "CM.Campaign.create",
    "transactionCode": "CM-CRT-0001",
    "processingType": "CREATE",
    "serviceName": "캠페인 등록",
    "guid": "GUID-20260705-000003",
    "traceId": "TRC-20260705-000003",
    "channelId": "WEBTOP",
    "userId": "U123456",
    "branchId": "001234",
    "requestTime": "2026-07-05T11:30:00+09:00",
    "responseTime": "2026-07-05T11:30:00.700+09:00",
    "elapsedTimeMs": 700
  },
  "result": {
    "resultCode": "S0000",
    "resultStatus": "SUCCESS",
    "resultMessage": "정상 등록되었습니다.",
    "errorCode": null,
    "errorMessage": null
  },
  "body": {
    "campaignId": "CMP2026070003",
    "campaignStatus": "DRAFT",
    "createdAt": "2026-07-05T11:30:00+09:00"
  }
}
```

### D.10 입력값 오류 응답 예시

필수값 누락, 형식 오류, 길이 오류 등은 VALIDATION_ERROR로 반환한다.
```json
{
  "header": {
    "systemId": "NSIGHT-MP",
    "businessCode": "SV",
    "serviceId": "SV.Customer.selectSummary",
    "transactionCode": "SV-INQ-0001",
    "processingType": "INQUIRY",
    "serviceName": "고객 요약 조회",
    "guid": "GUID-20260705-000004",
    "traceId": "TRC-20260705-000004",
    "channelId": "WEBTOP",
    "userId": "U123456",
    "branchId": "001234",
    "responseTime": "2026-07-05T12:00:00+09:00",
    "elapsedTimeMs": 35
  },
  "result": {
    "resultCode": "E0001",
    "resultStatus": "FAIL",
    "resultMessage": "입력값을 확인해 주십시오.",
    "errorCode": "E-TCF-VAL-0001",
    "errorMessage": "입력값을 확인해 주십시오.",
    "errorDetail": "customerNo는 필수입니다.",
    "errorSystemId": "NSIGHT-MP",
    "errorDateTime": "2026-07-05T12:00:00+09:00"
  },
  "body": null
}
```

### D.11 업무 오류 응답 예시

조회 결과 없음, 업무 조건 불일치 등은 시스템 오류가 아니라 업무 오류로 처리한다.
```json
{
  "header": {
    "systemId": "NSIGHT-MP",
    "businessCode": "SV",
    "serviceId": "SV.Customer.selectSummary",
    "transactionCode": "SV-INQ-0001",
    "processingType": "INQUIRY",
    "serviceName": "고객 요약 조회",
    "guid": "GUID-20260705-000005",
    "traceId": "TRC-20260705-000005",
    "channelId": "WEBTOP",
    "userId": "U123456",
    "branchId": "001234",
    "responseTime": "2026-07-05T12:10:00+09:00",
    "elapsedTimeMs": 120
  },
  "result": {
    "resultCode": "E0001",
    "resultStatus": "FAIL",
    "resultMessage": "조회 결과가 없습니다.",
    "errorCode": "E-SV-BIZ-0001",
    "errorMessage": "조회 결과가 없습니다.",
    "errorDetail": null,
    "errorSystemId": "NSIGHT-MP",
    "errorDateTime": "2026-07-05T12:10:00+09:00"
  },
  "body": null
}
```

모든 예외는 TCF 공통 후처리에서 표준 오류코드로 변환하고 StandardResponse로 조립하며, 거래로그·감사로그·모니터링 지표에 남기는 구조가 기준이다.

### D.12 권한 오류 응답 예시

권한 오류는 사용자에게 상세 내부 권한 구조를 노출하지 않는다.
```json
{
  "header": {
    "systemId": "NSIGHT-MP",
    "businessCode": "SV",
    "serviceId": "SV.Customer.selectDetail",
    "transactionCode": "SV-INQ-0002",
    "processingType": "INQUIRY",
    "serviceName": "고객 상세 조회",
    "guid": "GUID-20260705-000006",
    "traceId": "TRC-20260705-000006",
    "channelId": "WEBTOP",
    "userId": "U123456",
    "branchId": "001234",
    "responseTime": "2026-07-05T12:20:00+09:00",
    "elapsedTimeMs": 80
  },
  "result": {
    "resultCode": "E0003",
    "resultStatus": "FAIL",
    "resultMessage": "접근 권한이 없습니다.",
    "errorCode": "E-TCF-AUTHZ-0001",
    "errorMessage": "접근 권한이 없습니다.",
    "errorDetail": "운영 상세 메시지는 GUID 기준 로그에서 확인하십시오.",
    "errorSystemId": "NSIGHT-MP",
    "errorDateTime": "2026-07-05T12:20:00+09:00"
  },
  "body": null
}
```

### D.13 Timeout 응답 예시

Timeout은 처리 결과가 불명확할 수 있으므로 TIMEOUT 또는 UNKNOWN 상태로 남긴다.
```json
{
  "header": {
    "systemId": "NSIGHT-MP",
    "businessCode": "CM",
    "serviceId": "CM.Campaign.execute",
    "transactionCode": "CM-EXE-0001",
    "processingType": "EXECUTE",
    "serviceName": "캠페인 실행",
    "guid": "GUID-20260705-000007",
    "traceId": "TRC-20260705-000007",
    "channelId": "WEBTOP",
    "userId": "U123456",
    "branchId": "001234",
    "responseTime": "2026-07-05T12:30:05+09:00",
    "elapsedTimeMs": 5000
  },
  "result": {
    "resultCode": "E9001",
    "resultStatus": "UNKNOWN",
    "resultMessage": "처리 상태를 확인해 주십시오.",
    "errorCode": "E-TCF-TIME-0001",
    "errorMessage": "온라인 거래 제한시간을 초과했습니다.",
    "errorDetail": "거래 처리상태는 GUID 기준으로 거래로그에서 확인하십시오.",
    "errorSystemId": "NSIGHT-MP",
    "errorDateTime": "2026-07-05T12:30:05+09:00"
  },
  "body": null
}
```

### D.14 외부 연계 오류 응답 예시

```json
{
  "header": {
    "systemId": "NSIGHT-MP",
    "businessCode": "SV",
    "serviceId": "SV.Customer.selectSummary",
    "transactionCode": "SV-INQ-0001",
    "processingType": "INQUIRY",
    "serviceName": "고객 요약 조회",
    "guid": "GUID-20260705-000008",
    "traceId": "TRC-20260705-000008",
    "channelId": "WEBTOP",
    "userId": "U123456",
    "branchId": "001234",
    "responseTime": "2026-07-05T12:40:00+09:00",
    "elapsedTimeMs": 3000
  },
  "result": {
    "resultCode": "E9101",
    "resultStatus": "FAIL",
    "resultMessage": "연계 시스템 응답이 지연되고 있습니다.",
    "errorCode": "E-TCF-IF-0001",
    "errorMessage": "연계 시스템 응답이 지연되고 있습니다.",
    "errorDetail": "targetSystemId=EXT-CUSTOMER",
    "errorSystemId": "EXT-CUSTOMER",
    "errorDateTime": "2026-07-05T12:40:00+09:00"
  },
  "body": null
}
```

### D.15 다운로드 요청 예시

실제 파일 Binary는 JSON Body에 넣지 않는다.파일 목록 조회, 파일 메타 조회, 다운로드 가능 여부 확인은 표준 전문을 사용하고, 실제 다운로드는 GET /ud/files/{fileId}/download 같은 Stream API를 사용한다.
```json
{
  "header": {
    "systemId": "NSIGHT-MP",
    "businessCode": "OM",
    "serviceId": "OM.File.inquiry",
    "transactionCode": "OM-INQ-0005",
    "processingType": "INQUIRY",
    "serviceName": "파일 메타 조회",
    "guid": "",
    "traceId": "",
    "channelId": "OM-PORTAL",
    "userId": "admin01",
    "branchId": "000000",
    "requestTime": "2026-07-05T13:00:00+09:00"
  },
  "body": {
    "fileId": "FILE-20260705-000001"
  }
}
```

```json
{
  "header": {
    "systemId": "NSIGHT-MP",
    "businessCode": "OM",
    "serviceId": "OM.File.inquiry",
    "transactionCode": "OM-INQ-0005",
    "processingType": "INQUIRY",
    "serviceName": "파일 메타 조회",
    "guid": "GUID-20260705-000009",
    "traceId": "TRC-20260705-000009",
    "channelId": "OM-PORTAL",
    "userId": "admin01",
    "branchId": "000000",
    "responseTime": "2026-07-05T13:00:00.100+09:00",
    "elapsedTimeMs": 100
  },
  "result": {
    "resultCode": "S0000",
    "resultStatus": "SUCCESS",
    "resultMessage": "정상 처리되었습니다.",
    "errorCode": null,
    "errorMessage": null
  },
  "body": {
    "fileId": "FILE-20260705-000001",
    "originalFileName": "고객목록.xlsx",
    "contentType": "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
    "fileSize": 153600,
    "downloadUrl": "/ud/files/FILE-20260705-000001/download",
    "downloadReasonRequired": true
  }
}
```

공통전문조립은 업무 Handler가 반환한 업무 결과 또는 예외 정보를 표준 응답 전문으로 변환하는 후처리 기능이며, Header, Result, Body, Control, Security, 로그 연계 정보를 조립하는 방식으로 확장할 수 있다.

### D.16 Batch 수동실행 요청 예시

```json
{
  "header": {
    "systemId": "NSIGHT-MP",
    "businessCode": "OM",
    "serviceId": "OM.Batch.execute",
    "transactionCode": "OM-BAT-0001",
    "processingType": "EXECUTE",
    "serviceName": "배치 수동 실행",
    "guid": "",
    "traceId": "",
    "channelId": "OM-PORTAL",
    "userId": "admin01",
    "branchId": "000000",
    "requestTime": "2026-07-05T13:30:00+09:00",
    "idempotencyKey": "OM-BAT-0001-admin01-20260705-000001"
  },
  "body": {
    "jobId": "BAT-BATCH-001",
    "businessDate": "20260705",
    "runType": "MANUAL",
    "parameters": {
      "targetGroup": "AP",
      "forceYn": "N"
    }
  }
}
```

### D.17 Batch 수동실행 응답 예시

```json
{
  "header": {
    "systemId": "NSIGHT-MP",
    "businessCode": "OM",
    "serviceId": "OM.Batch.execute",
    "transactionCode": "OM-BAT-0001",
    "processingType": "EXECUTE",
    "serviceName": "배치 수동 실행",
    "guid": "GUID-20260705-000010",
    "traceId": "TRC-20260705-000010",
    "channelId": "OM-PORTAL",
    "userId": "admin01",
    "branchId": "000000",
    "responseTime": "2026-07-05T13:30:00.300+09:00",
    "elapsedTimeMs": 300
  },
  "result": {
    "resultCode": "S0000",
    "resultStatus": "SUCCESS",
    "resultMessage": "배치 실행 요청이 접수되었습니다.",
    "errorCode": null,
    "errorMessage": null
  },
  "body": {
    "executionId": "EXEC-20260705-000001",
    "jobId": "BAT-BATCH-001",
    "jobName": "AP 상태 수집",
    "runType": "MANUAL",
    "executionStatus": "RUNNING",
    "requestedBy": "admin01",
    "requestedAt": "2026-07-05T13:30:00+09:00"
  }
}
```

### D.18 표준 전문 검증 기준

| 검증 항목 | 기준 |
| --- | --- |
| 오류코드 예시 | JSON 형식 |
| JSON 파싱 가능해야 함 | E-TCF-MSG-0001 |
| Header 존재 | header 필수 |
| E-TCF-HDR-0001 | Body 존재 |
| 조회·등록·수정 거래는 업무 기준에 따라 검증 | E-TCF-VAL-0001 |
| 업무코드 | businessCode 필수 |
| E-TCF-HDR-0002 | ServiceId |
| serviceId 필수 | E-TCF-DSP-0001 |
| 거래코드 | transactionCode 필수 |
| E-TCF-TRX-0001 | 처리유형 |
| processingType 필수 | E-TCF-HDR-0003 |
| 채널 | channelId 필수 |
| E-TCF-HDR-0004 | 업무코드 정합성 |
| URL Path와 Header businessCode 일치 | E-TCF-HDR-0005 |
| ServiceId 정합성 | ServiceId Prefix와 businessCode 일치 |
| E-TCF-DSP-0002 | 거래코드 정합성 |
| TransactionCode Prefix와 businessCode 일치 | E-TCF-TRX-0002 |
| 거래통제 | Allow-List 등록 여부 확인 |
| E-TCF-CTL-0001 | 권한 |
| 사용자 권한 확인 | E-TCF-AUTHZ-0001 |

### D.19 표준 전문 작성 원칙

| 구분 | 원칙 | Header |
| --- | --- | --- |
모든 거래 공통 식별자는 Header에 둔다.

| Body | 업무 데이터만 둔다. |
실제 실행 Handler를 식별한다.

| ServiceId | TransactionCode |
거래로그·감사·재처리 기준으로 사용한다.

| GUID | 없으면 STF에서 생성한다. |
| TraceId | 없으면 STF에서 생성한다. | User/Branch |
Header 값을 그대로 신뢰하지 않고 세션 기준으로 재검증한다.

성공·실패 판단은 result.resultCode, result.resultStatus로 한다.

| Result | |
오류 상세는 사용자용과 운영자용을 분리한다.

| Error | File |
Binary 파일은 JSON Body에 넣지 않는다.

목록조회는 body.page 또는 응답 body.page로 관리한다.

| Paging | |
민감정보는 응답 조립 시 마스킹한다.

| Security | |

### D.20 최종 정리

표준 전문 예시의 핵심은 다음이다.
StandardRequest
= header + body

StandardResponse
= header + result + body

Header
= 실행·통제·추적 기준

Body
= 업무별 자유 데이터

Result
= 성공·실패·오류코드·메시지 기준

따라서 NSIGHT TCF Framework의 표준 전문은 다음 한 문장으로 정의할 수 있다.
표준 전문은 모든 온라인 거래를 동일한 구조로 수신하고, 동일한 기준으로 검증·실행·응답·로그·감사하기 위한 NSIGHT TCF의 공통 계약이다.

## 소스·관련 문서

| 참고 |
|------|

> znsight-guide-word: `통합 (75).docx`

| [전문관리.md](../zdoc/전문관리.md) |

## 코드베이스 정정 (develop 기준)

| 항목 | 값 |
|------|-----|
| 업무 WAR | ic, pc, ms, sv, pd, eb, ep, ss, mg + tcf-om |
| ztomcat deploy | `ztomcat/deploy-wars.sh` 13 WAR |
| buildZtomcatWars | 15 WAR |
| bootRun | gateway 8100, uj 8102, jwt 8110, ui 8099 |


---

[← 78. 테스트 코드 샘플](./78-테스트-코드-샘플.md) · [README](./README.md)