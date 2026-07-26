# Header 항목 명명규칙

> **NSIGHT TCF 명명규칙 상세** · 원본: [`znsight-guide-word`](../znsight-guide-word/) · 갱신: 2026-07-05

Header 항목 명명규칙 설계기준

## 1. 도입 전 안내말

Header 항목 명명규칙은 JSON 전문의 `header` 영역에 어떤 필드명을 쓰고, Java·DB·로그·거래통제에서 어떻게 매핑할지를 정의하는 기준이다.

NSIGHT TCF에서 Header는 단순 메타데이터가 아니다. `serviceId`는 Dispatcher 실행 기준, `transactionCode`는 거래로그·감사·거래통제 기준, `businessCode`는 Context·WAR·Package 기준이며, `userId`·`channelId`·`branchId`는 권한·통제·추적 기준으로 사용된다.

원본 `명명규칙 상세 (21).docx`는 본문이 비어 있으므로, 아래 내용은 명명규칙 상세 (1~20).docx 및 관련 Manual 장에서 Header **항목 이름**과 **매핑** 기준을 통합·정리한 것이다.

## 2. Header 항목 명명 설계 결론

| 구분 | 표준 |
| --- | --- |
| --- | --- |
| JSON Header 필드명 | lowerCamelCase |
| Java StandardHeader 필드명 | JSON Header와 동일 |
| --- | --- |
| DTO Field | Header 항목과 동일 용어 사용 (`serviceId`, `transactionCode` 등) |
| DB 컬럼명 | UPPER_SNAKE_CASE (`SERVICE_ID`, `TRANSACTION_CODE` 등) |
| --- | --- |
| MDC Key | lowerCamelCase, Header 필드명과 동일 |
| HTTP Header | JSON Header와 혼용하지 않음 (기술 통제 전용) |
| --- | --- |
| Body Field | Header 항목 중복 금지 |

핵심 원칙은 다음이다.

Header 항목 이름은 **한 번 정하면 JSON · Java · MDC · 거래로그 · 거래통제** 전 구간에서 같은 의미로 사용한다.

## 3. Header 명명 최상위 원칙

| No | 원칙 | 기준 |
| --- | --- | --- |
| --- | --- | --- |
| 1 | lowerCamelCase | `serviceId`, `transactionCode`, `businessCode` |
| 2 | 의미 있는 명사 | `data`, `info`, `temp` 금지 |
| --- | --- | --- |
| 3 | 업무 용어 우선 | `userId`, `branchId`, `channelId` |
| 4 | 약어 지양 | `usr`, `br`, `trx` 금지 |
| --- | --- | --- |
| 5 | Header·Body 분리 | Header 항목을 Body DTO에 중복 포함하지 않음 |
| 6 | HTTP·JSON 구분 | Authorization 등은 HTTP Header, `serviceId` 등은 JSON Header |
| --- | --- | --- |
| 7 | 정합성 연결 | `businessCode` ↔ ServiceId Prefix ↔ 거래코드 Prefix 일치 |
| 8 | 거래통제 7항 | `serviceId`, `transactionCode`, `businessCode`, `serviceName`, `userId`, `channelId`, `branchId` |
| --- | --- | --- |
| 9 | 추적 항목 | `guid`, `traceId`, `transactionId`는 로그·MDC와 동일 명칭 |
| 10 | 일시 접미어 | 일시형은 `At` 또는 `Time` (`requestAt`, `requestTime`) |

## 4. JSON Header / Java Field / DB Column 매핑

| 의미 | JSON Header | Java Field | DB 컬럼 (거래로그·거래통제) | MDC Key |
| --- | --- | --- | --- | --- |
| 서비스 ID | serviceId | serviceId | SERVICE_ID | serviceId |
| 거래코드 | transactionCode | transactionCode | TRANSACTION_CODE | transactionCode |
| 업무코드 | businessCode | businessCode | BUSINESS_CODE | businessCode |
| 서비스명 | serviceName | serviceName | SERVICE_NAME | — |
| 사용자 ID | userId | userId | USER_ID | userId |
| 채널 ID | channelId | channelId | CHANNEL_ID | channelId |
| 지점 코드 | branchId | branchId | BRANCH_ID | branchId |
| GUID | guid | guid | GUID | guid |
| Trace ID | traceId | traceId | TRACE_ID | traceId |
| 거래 ID | transactionId | transactionId | TRANSACTION_ID | transactionId |
| 메뉴 ID | menuId | menuId | MENU_ID | menuId |
| 화면번호 | screenId | screenId | SCREEN_ID | screenId |
| 기능코드 | functionCode | functionCode | FUNCTION_CODE | — |
| 처리유형 | processingType | processingType | PROCESSING_TYPE | — |
| 요청 IP | clientIp | clientIp | CLIENT_IP | clientIp |
| 중복방지 Key | idempotencyKey | idempotencyKey | IDEMPOTENCY_KEY | — |

## 5. 표준 Header Field 목록

### 5.1 실행·통제 핵심 항목

| 의미 | 표준 Field | 비고 |
| --- | --- | --- |
| --- | --- | --- |
| 서비스 ID | serviceId | Dispatcher 실행 기준 |
| 거래코드 | transactionCode | 거래로그 / 감사 / 거래통제 기준 |
| --- | --- | --- |
| 업무코드 | businessCode | Context / WAR / Package 기준 |
| 서비스명 | serviceName | 사용자·운영 표시명 |
| --- | --- | --- |
| 처리유형 | processingType | INQUIRY, CREATE, UPDATE 등 |
| 사용자 ID | userId | `user`보다 명확 |
| --- | --- | --- |
| 채널 ID | channelId | WEBTOP, OM-PORTAL, API 등 |
| 지점 코드 | branchId | `branch`보다 명확 (`branchCode`는 Java 내부 DTO에서 병행 가능) |

### 5.2 추적·세션 항목

| 의미 | 표준 Field | 비고 |
| --- | --- | --- |
| --- | --- | --- |
| GUID | guid | End-to-End 추적 |
| Trace ID | traceId | AP 내부·서비스 간 추적 |
| --- | --- | --- |
| 거래 ID | transactionId | 거래로그 단위 ID |
| 세션 ID | sessionId | 세션 추적 (선택) |
| --- | --- | --- |
| 요청 시각 | requestAt / requestTime | 일시형은 `At` 또는 `Time` |
| 응답 시각 | responseAt / responseTime | 응답 Header에 추가 |
| --- | --- | --- |
| 처리 시간(ms) | elapsedTimeMs | 응답 Header (Long) |

### 5.3 화면·메뉴 연계 항목

화면에서 ServiceId를 호출할 때 Header에는 최소 다음 항목이 있어야 한다.

| Header 항목 | 설명 | 예시 |
| --- | --- | --- |
| --- | --- | --- |
| businessCode | 업무코드 | SV |
| menuId | 메뉴 ID | SVMENU0002 |
| --- | --- | --- |
| screenId | 화면번호 | SVLIST0001 |
| functionCode | 화면 기능코드 | SEARCH |
| --- | --- | --- |
| serviceId | 실행 ServiceId | SV.Customer.selectSummary |
| transactionCode | 거래코드 | SV-INQ-0001 |
| --- | --- | --- |
| userId | 사용자 ID | U123456 |
| branchId | 지점코드 | 001234 |
| --- | --- | --- |
| channelId | 채널 ID | WEBTOP |
| guid | 거래 GUID | 자동 생성 또는 전달 |
| --- | --- | --- |
| traceId | Trace ID | 자동 생성 |

### 5.4 시스템·부가 항목

| 의미 | 표준 Field | 비고 |
| --- | --- | --- |
| --- | --- | --- |
| 시스템 ID | systemId | NSIGHT-MP 등 |
| 센터 ID | centerId | DR/센터 식별 |
| --- | --- | --- |
| AP ID | apId | 응답·로그 (MDC) |
| 중복방지 Key | idempotencyKey | 등록·수정·발송 거래 권장 |

## 6. user / branch와 userId / branchId 표준

과거 설계 문서와 외부 전문에서는 `user`, `branch` 표현이 함께 사용되었다. 개발 표준에서는 다음처럼 정리한다.

| 구분 | 표준 |
| --- | --- |
| --- | --- |
| JSON Header 표준 필드명 | userId, branchId |
| 거래통제 논리명 | user, branch |
| --- | --- |
| DB 컬럼명 | USER_ID, BRANCH_ID |
| Java StandardHeader 필드명 | userId, branchId |
| --- | --- |
| Java 내부 DTO (권장) | userId, branchCode |
| MDC Key | userId, branchId |

Header 항목이 외부 전문에서 `user`, `branch`로 들어오더라도 Java 내부 DTO에서는 `userId`, `branchCode`처럼 의미를 명확히 하는 방식을 권장한다. 단, 표준 전문 클래스(`StandardHeader`)가 이미 `userId`, `branchId`를 채택했다면 JSON Header와 동일하게 유지한다.

```json
{
  "header": {
    "serviceId": "SV.Customer.selectSummary",
    "transactionCode": "SV-INQ-0001",
    "businessCode": "SV",
    "serviceName": "고객요약조회",
    "userId": "U123456",
    "channelId": "WEBTOP",
    "branchId": "001234"
  }
}
```

## 7. Header 정합성 명명 기준

Header 항목 이름뿐 아니라 **값 형식**도 다른 명명규칙과 연결된다.

| Header 항목 | 정합성 기준 |
| --- | --- |
| --- | --- |
| businessCode | 거래코드 Prefix와 일치 (`SV` ↔ `SV-INQ-0001`) |
| serviceId | `{businessCode}.{업무대상}.{처리행위}` 형식, Catalog 등록 |
| --- | --- |
| transactionCode | `{업무코드}-{거래유형}-{일련번호}` 형식 |
| serviceName | 거래코드·ServiceId 업무 의미와 일치 |
| --- | --- |
| processingType | 거래코드 유형과 일치 (`INQUIRY` ↔ `INQ`) |
| userId, channelId, branchId | 거래통제 Allow-List 검증 대상 |

```json
{
  "header": {
    "serviceId": "SV.Customer.selectSummary",
    "transactionCode": "SV-INQ-0001",
    "businessCode": "SV",
    "serviceName": "고객요약조회",
    "userId": "U123456",
    "channelId": "WEBTOP",
    "branchId": "001234"
  },
  "body": {
    "customerNo": "CUST0000001"
  }
}
```

Gateway 경유 시 Path 업무코드와 `header.businessCode`도 일치해야 한다.

```text
POST /gw/sv/online
header.businessCode = SV
header.serviceId    = SV.Customer.selectSummary
```

## 8. 거래통제 Header 7항

거래통제는 다음 Header 조합을 기준으로 허용 여부를 판단한다.

| No | Header Field | DB 컬럼 | 예시 |
| --- | --- | --- | --- |
| 1 | serviceId | SERVICE_ID | SV.Customer.selectSummary |
| 2 | transactionCode | TRANSACTION_CODE | SV-INQ-0001 |
| 3 | businessCode | BUSINESS_CODE | SV |
| 4 | serviceName | SERVICE_NAME | 고객요약조회 |
| 5 | userId | USER_ID | U123456 |
| 6 | channelId | CHANNEL_ID | WEBTOP |
| 7 | branchId | BRANCH_ID | 001234 |

## 9. Header ↔ 거래로그 / MDC 매핑

거래로그와 MDC에서는 Header 필드명을 **lowerCamelCase**로 그대로 사용한다.

| Header Field | 거래로그 컬럼 | MDC Key |
| --- | --- | --- |
| --- | --- | --- |
| serviceId | SERVICE_ID | serviceId |
| transactionCode | TRANSACTION_CODE | transactionCode |
| --- | --- | --- |
| businessCode | BUSINESS_CODE | businessCode |
| userId | USER_ID | userId |
| --- | --- | --- |
| channelId | CHANNEL_ID | channelId |
| branchId | BRANCH_ID | branchId |
| --- | --- | --- |
| guid | GUID | guid |
| traceId | TRACE_ID | traceId |
| --- | --- | --- |
| transactionId | TRANSACTION_ID | transactionId |
| menuId | MENU_ID | menuId |
| --- | --- | --- |
| screenId | SCREEN_ID | screenId |
| clientIp | CLIENT_IP | clientIp |

로그 메시지 예시:

```text
[TX-START] transaction started - serviceId=SV.Customer.selectSummary, transactionCode=SV-INQ-0001
```

## 10. Header 관련 오류코드 명명

Header 검증 오류는 `E-TCF-HDR-{NNNN}` 형식을 사용한다.

| 오류코드 | 오류명 | 관련 Header Field |
| --- | --- | --- |
| --- | --- | --- |
| E-TCF-HDR-0001 | ServiceId 누락 | serviceId |
| E-TCF-HDR-0002 | TransactionCode 누락 | transactionCode |
| --- | --- | --- |
| E-TCF-HDR-0003 | BusinessCode 누락 | businessCode |
| E-TCF-HDR-0004 | ChannelId 누락 | channelId |
| --- | --- | --- |
| E-TCF-HDR-0005 | UserId 누락 | userId |
| E-TCF-HDR-0006 | BranchId 누락 | branchId |
| --- | --- | --- |
| E-TCF-HDR-0011 | URL 업무코드 불일치 | businessCode |
| E-TCF-HDR-0012 | ServiceId Prefix 불일치 | serviceId, businessCode |
| --- | --- | --- |
| E-TCF-HDR-0013 | 거래코드 Prefix 불일치 | transactionCode, businessCode |

연결 메시지코드: `MSG-TCF-HDR-{NNNN}` ↔ `E-TCF-HDR-{NNNN}`

## 11. Header 항목 금지 사례

| 잘못된 예 | 문제 | 표준 |
| --- | --- | --- |
| --- | --- | --- |
| Body에 serviceId 작성 | Dispatcher 기준 훼손 | header.serviceId |
| Request DTO에 userId 중복 | Header·Body 혼선 | Header만 사용 |
| --- | --- | --- |
| user, branch (신규 JSON) | 필드 의미 불명확 | userId, branchId |
| usrId, brId | 약어·비표준 | userId, branchId |
| --- | --- | --- |
| ServiceID, Transaction_Code | 표기법 불일치 | serviceId, transactionCode |
| Header에 customerNo | Header 오염·로그 노출 | body.customerNo |
| --- | --- | --- |
| HTTP Header에 serviceId | 영역 혼용 | JSON header.serviceId |

## 12. 개발자 체크리스트

| 점검 항목 | 확인 |
| --- | --- |
| --- | --- |
| JSON Header 필드명이 lowerCamelCase인가? | □ |
| serviceId, transactionCode, businessCode가 표준 형식인가? | □ |
| --- | --- |
| userId, branchId, channelId 표준명을 사용하는가? | □ |
| Request DTO에 Header 항목이 중복 포함되지 않았는가? | □ |
| --- | --- |
| Java StandardHeader 필드명이 JSON과 일치하는가? | □ |
| MDC Key가 Header 필드명과 동일한가? | □ |
| --- | --- |
| 거래로그 컬럼 매핑이 UPPER_SNAKE_CASE로 정의되었는가? | □ |
| businessCode ↔ serviceId ↔ transactionCode Prefix가 일치하는가? | □ |
| --- | --- |
| 화면 호출 시 menuId, screenId, functionCode를 포함하는가? | □ |
| guid, traceId가 로그·MDC에 기록되는가? | □ |

## 13. 마무리말

Header 항목 명명은 NSIGHT TCF의 **실행·통제·추적** 계약이다. 필드명 하나가 ServiceId, 거래코드, 거래통제, 거래로그, MDC, 오류코드까지 연결되므로, JSON · Java · DB · 로그 전 구간에서 **같은 이름·같은 의미**를 유지해야 한다.

```text
header.serviceId
   ↓
StandardHeader.serviceId
   ↓
TransactionContext.serviceId
   ↓
MDC.serviceId / TCF_TRANSACTION_LOG.SERVICE_ID
   ↓
거래통제 TCF_TRANSACTION_CONTROL.SERVICE_ID
```

---

> **출처 통합**: [명명 - 21. Header 항목.docx](../znsight-guide-word/NSIGHT%20TCF%20개발%20매뉴얼%20-%20명명%20-%2021.%20Header%20항목.docx)는 본 md 기준으로 생성됨.  
> `명명규칙 상세 (8, 10, 11, 14, 16, 17, 18).docx` 및 Manual [21장 Header 작성 기준](./21-Header-작성-기준.md)에서 Header **항목 이름·매핑** 기준을 추출·통합함.

---

## 관련 Manual 장

- [21장](./21-Header-작성-기준.md)

## 원본

- [`znsight-guide-word`](../znsight-guide-word/) — [명명 - 21. Header 항목.docx](../znsight-guide-word/NSIGHT%20TCF%20개발%20매뉴얼%20-%20명명%20-%2021.%20Header%20항목.docx)

> znsight-man 통합본(`명명규칙-21-Header-항목.md`)은 명명규칙 8·10·11·14·16·17·18 및 Manual 21장 기준으로 정리함. docx 재생성: `node _gen-naming21-docx.cjs`
