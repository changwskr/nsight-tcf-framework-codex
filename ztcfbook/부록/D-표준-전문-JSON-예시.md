# 부록 D. 표준 전문 JSON 예시

| 항목 | 내용 |
| --- | --- |
| **부록** | D |
| **상태** | 집필 완료 |
| **목차** | [00-목차](../00-목차.md) |

---

## D.1 표준 전문 개요

NSIGHT TCF는 REST Resource URL이 업무 API를 대표하지 않는다. 클라이언트·WebTopSuite·Gateway는 `POST /{businessCode}/online`으로 JSON 표준 전문을 보내고, Header의 `serviceId`로 실행 Handler가 결정된다. 요청은 `header` + `body`, 응답은 `header` + `result` + `body` 구조를 따른다.

TCF 엔진은 STF(전처리) → TransactionDispatcher → Handler → ETF(후처리) 순으로 동작한다. STF는 JSON 파싱, Header 필수값, businessCode·ServiceId·거래코드 정합성, 세션·권한·멱등성을 검증한다. ETF는 `result` 블록 조립, 오류코드 매핑, 거래 종료 로그·감사를 수행한다. 업무 개발자는 Body 스키마와 Handler 로직에 집중하고, 성공·실패 응답 envelope은 프레임워크가 맞춘다.

```text
Client / tcf-ui / 외부 API
        ↓ POST /{businessCode}/online
StandardRequest { header, body }
        ↓ TCF.process()
STF → Dispatcher → Handler → ETF
        ↓
StandardResponse { header, result, body }
```

본 부록은 조회·목록·등록·오류·배치 등 대표 시나리오의 **완전한 JSON 예시**를 제공한다. 복사해 Postman·통합 테스트·화면 연동 명세의 기준으로 사용할 수 있다.

---

## D.2 기본 요청·응답 구조

### 요청 (StandardRequest)

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

### 응답 (StandardResponse)

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

성공 여부는 HTTP 상태코드만으로 판단하지 않는다. `result.resultCode`(`S0000` 등)와 `result.resultStatus`(`SUCCESS` / `FAIL` / `UNKNOWN`)를 함께 본다.

---

## D.3 Header 표준 항목

| 항목 | 필수 | 설명 | 예시 |
| --- | --- | --- | --- |
| systemId | N | 요청 시스템 ID | NSIGHT-MP |
| businessCode | Y | 업무코드 (부록 A) | SV |
| serviceId | Y | 실행 ServiceId (부록 B) | SV.Customer.selectSummary |
| transactionCode | Y | 거래코드 (부록 C) | SV-INQ-0001 |
| processingType | Y | 처리유형 | INQUIRY |
| serviceName | Y | 서비스명(화면·로그용) | 고객 요약 조회 |
| guid | N | E2E 추적 ID (없으면 STF 생성) | GUID-20260705-000001 |
| traceId | N | 내부 Trace (없으면 STF 생성) | TRC-20260705-000001 |
| channelId | Y | 호출 채널 | WEBTOP |
| userId | 조건부 | 사용자 ID (세션 연동) | U123456 |
| branchId | 조건부 | 지점 코드 | 001234 |
| centerId | N | 센터 코드 | DC1 |
| requestTime | N | 요청 시각 (ISO-8601) | 2026-07-05T10:30:00+09:00 |
| responseTime | N | 응답 시각 (응답만) | 2026-07-05T10:30:00.250+09:00 |
| elapsedTimeMs | N | 처리 시간 ms (응답만) | 250 |
| clientIp | N | 클라이언트 IP | 10.10.10.10 |
| idempotencyKey | N | 중복요청 방지 (변경성 권장) | SV-INQ-0001-U123456-... |

Header는 거래통제·권한·Timeout·로그의 단일 출처이다. Body에 `userId`를 넣어 우회하지 않는다. STF는 세션의 사용자·지점과 Header를 재검증한다.

---

## D.4 조회 거래: SV 고객 요약

| 항목 | 값 |
| --- | --- |
| Endpoint | POST /sv/online |
| businessCode | SV |
| serviceId | SV.Customer.selectSummary |
| transactionCode | SV-INQ-0001 |
| processingType | INQUIRY |

**요청**

```json
{
  "header": {
    "systemId": "NSIGHT-MP",
    "businessCode": "SV",
    "serviceId": "SV.Customer.selectSummary",
    "transactionCode": "SV-INQ-0001",
    "processingType": "INQUIRY",
    "serviceName": "고객 요약 조회",
    "guid": "",
    "traceId": "",
    "channelId": "WEBTOP",
    "userId": "U123456",
    "branchId": "001234",
    "centerId": "DC1",
    "requestTime": "2026-07-05T10:30:00+09:00",
    "clientIp": "10.10.10.10",
    "idempotencyKey": null
  },
  "body": {
    "customerNo": "CUST0000001",
    "baseDate": "20260705",
    "includeAccount": true,
    "includeCampaign": true
  }
}
```

**성공 응답**

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

---

## D.5 목록 조회·페이징: CM 캠페인

목록 조회는 `body.searchCondition`과 `body.page`를 사용한다. 응답은 `body.list`와 `body.page`를 함께 반환한다. 페이징 계산은 업무 WAR의 Rule·Service·DAO에서 수행하며, TCF Core가 자동 페이징하지 않을 수 있다.

| 항목 | 값 |
| --- | --- |
| Endpoint | POST /cm/online |
| serviceId | CM.Campaign.selectList |
| transactionCode | CM-INQ-0001 |

**요청**

```json
{
  "header": {
    "systemId": "NSIGHT-MP",
    "businessCode": "CM",
    "serviceId": "CM.Campaign.selectList",
    "transactionCode": "CM-INQ-0001",
    "processingType": "INQUIRY",
    "serviceName": "캠페인 목록 조회",
    "guid": "",
    "traceId": "",
    "channelId": "WEBTOP",
    "userId": "U123456",
    "branchId": "001234",
    "requestTime": "2026-07-05T11:00:00+09:00"
  },
  "body": {
    "searchCondition": {
      "campaignName": "우대금리",
      "campaignStatus": "ACTIVE",
      "fromDate": "20260701",
      "toDate": "20260731"
    },
    "page": {
      "pageNo": 1,
      "pageSize": 100
    }
  }
}
```

**성공 응답**

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

---

## D.6 등록 거래: CM 캠페인 생성

등록·수정·삭제·실행 거래는 `idempotencyKey` 사용을 권장한다.

| 항목 | 값 |
| --- | --- |
| Endpoint | POST /cm/online |
| serviceId | CM.Campaign.create |
| transactionCode | CM-CRT-0001 |
| processingType | CREATE |

**요청**

```json
{
  "header": {
    "systemId": "NSIGHT-MP",
    "businessCode": "CM",
    "serviceId": "CM.Campaign.create",
    "transactionCode": "CM-CRT-0001",
    "processingType": "CREATE",
    "serviceName": "캠페인 등록",
    "guid": "",
    "traceId": "",
    "channelId": "WEBTOP",
    "userId": "U123456",
    "branchId": "001234",
    "requestTime": "2026-07-05T11:30:00+09:00",
    "idempotencyKey": "CM-CRT-0001-U123456-20260705-000001"
  },
  "body": {
    "campaignName": "신규 고객 우대 캠페인",
    "campaignType": "CUSTOMER",
    "targetSegmentId": "SEG2026070001",
    "startDate": "20260710",
    "endDate": "20260731",
    "description": "신규 고객 대상 우대 캠페인"
  }
}
```

**성공 응답**

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

---

## D.7 입력값 오류 (VALIDATION)

필수값 누락·형식·길이 오류는 업무 예외가 아니라 검증 오류로 `resultStatus: FAIL`을 반환한다.

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

---

## D.8 업무 오류 (BUSINESS)

조회 결과 없음·업무 조건 불일치는 시스템 장애가 아닌 업무 오류이다. `errorCode`에 업무코드가 포함된다 (`E-SV-BIZ-0001`).

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

---

## D.9 권한 오류 (AUTHORIZATION)

권한 오류 응답에는 내부 권한 매트릭스를 노출하지 않는다. 상세는 GUID 기준 서버 로그에서 확인한다.

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

---

## D.10 Timeout (UNKNOWN)

Timeout은 처리 결과가 불명확할 수 있어 `resultStatus: UNKNOWN`으로 반환한다. 재처리 전 거래로그·원장·외부 연계 상태를 확인한다.

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

---

## D.11 배치 수동 실행 (OM)

| 항목 | 값 |
| --- | --- |
| Endpoint | POST /om/online |
| serviceId | OM.Batch.execute |
| transactionCode | OM-BAT-0001 |
| processingType | EXECUTE |

**요청**

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

**성공 응답**

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

배치 본 처리는 비동기일 수 있다. 응답 `executionStatus: RUNNING`은 요청 접수를 의미하며, 최종 결과는 별도 조회 ServiceId로 확인한다.

---

## D.12 표준 전문 검증·작성 원칙

### STF 검증 항목

| 검증 항목 | 기준 | 오류 예시 |
| --- | --- | --- |
| JSON 형식 | 파싱 가능 | E-TCF-MSG-0001 |
| header | 필수 존재 | E-TCF-HDR-0001 |
| businessCode | 필수, Path 일치 | E-TCF-HDR-0005 |
| serviceId | 필수, Prefix 일치 | E-TCF-DSP-0001 |
| transactionCode | 필수, 형식·Prefix | E-TCF-TRX-0001 |
| processingType | 필수, Catalog 일치 | E-TCF-TRX-0004 |
| channelId | 필수 | E-TCF-HDR-0004 |
| 거래통제 | Allow-List | E-TCF-CTL-0001 |
| 권한 | 사용자 권한 | E-TCF-AUTHZ-0001 |

### 작성 원칙

| 구분 | 원칙 |
| --- | --- |
| Header | 식별·통제·추적 정보만 |
| Body | 업무 데이터만 |
| ServiceId | 실행 Handler 식별 |
| TransactionCode | 로그·감사·재처리 |
| GUID / TraceId | 없으면 STF 생성 |
| Result | 성공·실패 판단 기준 |
| Error | 사용자 메시지와 운영 상세 분리 |
| File | Binary는 JSON Body에 넣지 않음 |
| Paging | body.page / body.list 표준 |
| Security | 민감정보 마스킹 |

파일 다운로드는 JSON으로 바이너리를 실어 보내지 않는다. `OM.File.inquiry`로 메타·`downloadUrl`을 받고, Stream API(`GET /ud/files/{fileId}/download`)로 전송한다.

---

## 요약

표준 전문은 `StandardRequest(header+body)`와 `StandardResponse(header+result+body)`로 모든 온라인 거래의 공통 계약을 정의한다. 본 부록 예시는 조회·페이징·등록·검증/업무/권한 오류·Timeout·배치를 포괄하며, Header 필수항목과 부록 A~C 식별자 정합성을 전제로 한다.

---

## 이전 · 다음

| | |
| --- | --- |
| ← 이전 | [부록 C](./C-거래코드-명명규칙.md) |
| → 다음 | [부록 E](./E-Mapper-XML-템플릿.md) |

---

## 출처 색인

| 참고 | 경로 |
| --- | --- |
| NSIGHT TCF 개발 매뉴얼 (원본) | `znsight-guide-word/통합 (75).docx` |
| znsight-man 부록 D | `znsight-man/부록D-표준-전문-예시.md` |
| 전문 관리 설계 | `zdoc/전문관리.md` |
