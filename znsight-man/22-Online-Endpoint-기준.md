# 22. Online Endpoint 기준

> **NSIGHT TCF 개발 Manual** · 원본: [`znsight-guide-word`](../znsight-guide-word/) · 갱신: 2026-07-05

## 22. Online Endpoint 기준

### 22.1 도입 전 안내말

NSIGHT TCF Framework에서 Online Endpoint는 화면, WebTopSuite, API, Gateway가 온라인 거래를 호출하기 위해 사용하는 표준 URL 진입점이다.
NSIGHT는 일반 REST 방식처럼 업무 기능마다 URL을 따로 만들지 않는다.기본 원칙은 다음과 같다.
URL은 업무 Context를 찾는 기준이다.
실제 실행할 프로그램은 Header의 serviceId로 결정한다.

즉, 사용자는 다음처럼 호출한다.
```text
POST /sv/online
POST /om/online
POST /cm/online
```

그리고 실제 실행 대상은 JSON Header의 serviceId로 결정된다.
```json
{
  "header": {
    "businessCode": "SV",
    "serviceId": "SV.Customer.selectSummary",
    "transactionCode": "SV-INQ-0001"
  },
  "body": {
    "customerNo": "CUST00000001"
  }
}
```

기존 TCF Framework 설계 기준에서도 온라인 진입점은 POST /{businessCode}/online, 실행 식별자는 URL이 아니라 header.serviceId, 공통 처리는 STF → TransactionDispatcher → ETF, 업무 처리는 Handler → Facade → Service → Rule → DAO/Mapper 구조로 정의되어 있다.

### 22.2 Online Endpoint 설계 결론

NSIGHT Online Endpoint는 다음 기준으로 설계한다.
| 구분 | 설계 기준 |
| --- | --- |
| 기본 Endpoint | POST /{businessCode}/online |
| 예시 | POST /sv/online, POST /om/online |
| HTTP Method | POST |
| Content-Type | application/json; charset=UTF-8 |
| 요청 구조 | StandardRequest = header + body |
| 응답 구조 | StandardResponse = header + result + body + error |
| 실행 기준 | header.serviceId |
| 업무 기준 | URL Path의 {businessCode} + Header의 businessCode |
| 거래 기준 | header.transactionCode |
| Controller 수 | 업무 WAR당 Online Controller 1개 원칙 |
| 업무 기능 URL | 별도 생성 금지 |
| REST Resource URL | 원칙적으로 사용하지 않음 |
| Gateway 사용 시 | POST /gw/{businessCode}/online 또는 POST /api/tcf/{businessCode}/online |
| tcf-ui Relay 사용 시 | POST /api/relay/{businessCode}/online 가능 |

표준 전문 구조에서도 NSIGHT는 일반 REST Resource 방식이 아니라 HTTP/JSON 표준 전문 + ServiceId Dispatcher 방식을 기준으로 하며, 기본 Endpoint는 /{businessCode}/online, 요청 구조는 header + body, 응답 구조는 header + result + body로 정의되어 있다.

### 22.3 기본 Endpoint 형식

#### 22.3.1 업무 WAR 직접 호출 기준

업무 WAR를 직접 호출하는 경우 기본 형식은 다음과 같다.
POST /{businessCodeLower}/online

예시는 다음과 같다.
| 업무 | 업무코드 | Context Path | Online Endpoint |
| --- | --- | --- | --- |
| Single View | SV | /sv | POST /sv/online |
| Campaign | CM | /cm | POST /cm/online |
| Message | MG | /mg | POST /mg/online |
| Operation Management | OM | /om | POST /om/online |
| Upload / Download | UD | /ud | POST /ud/online |
| Event Batch | EB | /eb | POST /eb/online |
| Event Processing | EP | /ep | POST /ep/online |

#### 22.3.2 Gateway 경유 호출 기준

Gateway를 사용하는 경우 외부 호출 Endpoint는 Gateway 기준으로 단일화할 수 있다.
```text
POST /gw/{businessCode}/online
```

또는
```text
POST /api/tcf/{businessCode}/online
```

예시는 다음과 같다.
외부 호출
Gateway 내부 라우팅
대상 업무 WAR
```text
POST /gw/SV/online
```

businessCode = SV
```text
POST /sv/online
POST /gw/OM/online
```

businessCode = OM
```text
POST /om/online
POST /gw/CM/online
```

businessCode = CM
```text
POST /cm/online
```

Gateway 라우팅 구조에서도 Gateway는 Apache 뒤쪽, 업무 WAR 앞쪽에 위치하여 업무코드 기반 라우팅, 세션 확인, Header 검증, ServiceId 검증, 거래통제 연계, Downstream 업무 WAS Relay를 담당하는 Application Gateway로 정의되어 있다.

### 22.4 REST URL과 Online Endpoint의 차이

NSIGHT는 REST Resource URL을 기본으로 사용하지 않는다.
| 구분 | REST 방식 | NSIGHT Online Endpoint 방식 |
| --- | --- | --- |
| URL 의미 | Resource를 표현 | 업무 Context만 표현 |
| 예시 | POST /api/v1/customers/summary | POST /sv/online |
| 실행 기준 | URL + Method | Header serviceId |
| 거래 식별 | URL 또는 Controller Method | transactionCode |
| 공통 통제 | Controller별 구현 가능성 | TCF 공통 전처리에서 일괄 통제 |
| Handler 선택 | Spring MVC Mapping | TransactionDispatcher |
| 운영관리 | URL별 별도 관리 필요 | ServiceId Catalog 기준 관리 |
| 거래로그 | URL 기준 로그 | GUID + ServiceId + 거래코드 기준 로그 |

비REST HTTP/JSON 가이드에서도 NSIGHT는 REST 방식이 아니라 HTTP + JSON 표준 전문 + 거래코드 기반 처리방식으로 가는 것이 적합하며, URL은 /sv/online, /cm/online, /mg/online처럼 단순화하고 실제 처리 업무는 JSON Body 또는 Header의 serviceId, transactionCode로 구분하는 구조로 정리되어 있다.

### 22.5 Endpoint와 ServiceId 관계

Online Endpoint는 업무 WAR를 찾는 경로이고, ServiceId는 Handler를 찾는 실행 키이다.
```text
POST /sv/online
↓
SV 업무 WAR 진입
↓
header.serviceId = SV.Customer.selectSummary
↓
```

SvCustomerSummaryHandler 실행

| 항목 | 역할 |
| --- | --- |
| 예시 | URL Path |
| 어느 업무 Context로 들어갈지 결정 | /sv/online |
| businessCode | 업무코드 정합성 검증 |
| SV | serviceId |
| 실행 Handler 결정 | SV.Customer.selectSummary |
| transactionCode | 거래로그·감사 기준 |
정합성 기준은 다음과 같다.

| SV-INQ-0001 | |
| URL | Header businessCode |
| Header serviceId | 판단 |
| /sv/online | SV |
| SV.Customer.selectSummary | 정상 |
| /sv/online | CM |
| CM.Campaign.selectList | 오류 |
| /om/online | OM |
| OM.User.selectList | 정상 |
| /om/online | OM |
| SV.Customer.selectSummary | 오류 |

### 22.6 Endpoint Naming 기준

Online Endpoint는 반드시 소문자 Context를 사용한다.
업무코드 대문자: SV
Context 소문자: /sv
Endpoint: /sv/online

| 구분 | 표기 기준 | 예시 |
| --- | --- | --- |
| 업무코드 | 대문자 | SV, OM, CM |
| Context Path | 소문자 | /sv, /om, /cm |
| Endpoint Path | 소문자 | /sv/online |
| Java Package | 소문자 | com.nh.nsight.marketing.sv |
| WAR 파일명 | 소문자 | sv.war |
| ServiceId Prefix | 대문자 업무코드 | SV.Customer.selectSummary |

금지 예시는 다음과 같다.
| 금지 Endpoint | 문제점 | 권장 Endpoint |
| --- | --- | --- |
| /SV/online | Context 대소문자 혼선 | /sv/online |
| /sv/customer/selectSummary | REST식 기능 URL 증가 | /sv/online |
| /sv/api/online | 불필요한 중간 경로 | /sv/online |
| /online/sv | 업무 Context 기준 불명확 | /sv/online |
| /common/execute | 업무 경계 불명확 | /{businessCode}/online |

### 22.7 HTTP Method 기준

Online Endpoint는 기본적으로 POST만 사용한다.
| Method | 사용 여부 | 기준 |
| --- | --- | --- |
| POST | Y | 모든 온라인 거래 기본 Method |
| GET | N | 업무 거래 호출에 사용하지 않음 |
| PUT | N | REST Resource 방식이 아니므로 사용하지 않음 |
| PATCH | N | 사용하지 않음 |
| DELETE | N | 사용하지 않음 |

이유는 다음과 같다.
| 이유 | 설명 |
| --- | --- |
| 표준 전문 Body 필요 | header + body JSON 구조를 전달해야 함 |
| Header 통제 필요 | ServiceId, 거래코드, 사용자, 지점, 채널 정보 필요 |
| 조회도 통제 대상 | 조회 거래도 권한, 거래통제, 감사 대상일 수 있음 |
| 대량 조건 전달 | 조회 조건이 URL QueryString에 노출되지 않도록 함 |
| 감사/보안 | 고객번호, 검색조건 등을 URL에 남기지 않음 |

HTTP/JSON 통신 표준에서도 기본 Method는 POST, 데이터 형식은 JSON, JSON 위치는 HTTP Body, Content-Type은 application/json; charset=UTF-8로 정의되어 있다.

### 22.8 Content-Type 기준

Online Endpoint는 JSON 요청만 허용한다.
Content-Type: application/json; charset=UTF-8
Accept: application/json

| 항목 | 기준 |
| --- | --- |
| 요청 Content-Type | application/json; charset=UTF-8 |
| 응답 Content-Type | application/json; charset=UTF-8 |
| 문자셋 | UTF-8 |
| 요청 Body | StandardRequest |
| 응답 Body | StandardResponse |
| 파일 다운로드 | Stream 응답 가능, 단 요청과 감사는 표준 전문 기준 |
| 잘못된 요청은 차단한다. | 잘못된 Content-Type |
| 처리 기준 | text/plain |
| 차단 | application/x-www-form-urlencoded |
| 차단 | multipart/form-data |
| 파일 업로드 전용 Endpoint에서만 제한 허용 | Content-Type 없음 |
| 차단 |  |

### 22.9 Controller 작성 기준

업무 WAR에는 Online Controller를 1개만 둔다.
업무 WAR
```text
 └─ OnlineTransactionController
      └─ POST /{businessCode}/online
           ↓
```

         TCF.process()

Controller는 업무 로직을 처리하지 않는다.Controller는 요청을 받아 TCF.process()로 넘기는 역할만 수행한다.
@RestController
```java
@RequiredArgsConstructor
@RequestMapping("/sv")
public class SvOnlineTransactionController {
    private final TcfProcessor tcfProcessor;
    @PostMapping("/online")
    public ResponseEntity<StandardResponse<Object>> online(
            @RequestBody StandardRequest<Object> request,
            HttpServletRequest httpRequest
    ) {
        StandardResponse<Object> response =
                tcfProcessor.process("SV", request, httpRequest);
        return ResponseEntity.ok(response);
    }
}
```

공통화가 가능한 경우 업무별 Controller를 최소화하고 tcf-web 공통 Controller를 사용한다.
@RestController
```java
@RequiredArgsConstructor
public class OnlineTransactionController {
    private final TcfProcessor tcfProcessor;
    @PostMapping("/{businessCode}/online")
    public ResponseEntity<StandardResponse<Object>> online(
            @PathVariable String businessCode,
            @RequestBody StandardRequest<Object> request,
            HttpServletRequest httpRequest
    ) {
        StandardResponse<Object> response =
                tcfProcessor.process(businessCode.toUpperCase(), request, httpRequest);
        return ResponseEntity.ok(response);
    }
}
```

### 22.10 Controller 금지 사항

Controller는 다음을 수행하면 안 된다.
| 금지 사항 | 사유 |
| --- | --- |
| Controller Method를 ServiceId별로 생성 | ServiceId Dispatcher 구조 훼손 |
| Controller에서 업무 Service 직접 호출 | TCF 전처리·후처리 우회 |
| Controller에서 DB 조회 | 계층 책임 위반 |
| Controller에서 거래통제 직접 구현 | 공통 STF 책임 |
| Controller에서 StandardResponse 직접 조립 | ETF 책임 |
| Controller에서 오류코드 직접 매핑 | 공통 예외 처리 책임 |
| Controller에서 세션·권한을 부분 구현 | 공통 검증 누락 위험 |
| /sv/customer/summary 같은 업무 URL 생성 | Endpoint 표준 위반 |

나쁜 예시는 다음과 같다.
@RestController
@RequestMapping("/sv/customer")
```java
public class BadCustomerController {
    @PostMapping("/summary")
    public SvCustomerSummaryResponse summary(@RequestBody SvCustomerSummaryRequest request) {
        // TCF 우회
        return customerService.selectSummary(request);
    }
}
```

권장 구조는 다음과 같다.
```text
POST /sv/online
   ↓
TCF.process()
   ↓
```

serviceId = SV.Customer.selectSummary
```text
   ↓
SvCustomerSummaryHandler
```

### 22.11 Gateway Endpoint 기준

Gateway를 두는 경우 Endpoint는 다음처럼 구분한다.
| 구분 | Endpoint | 설명 |
| --- | --- | --- |
| 외부 Gateway 진입 | /gw/{businessCode}/online | 외부 화면 또는 API가 호출 |
| 내부 업무 WAR 진입 | /{businessCodeLower}/online | Gateway가 업무 WAR로 Relay |
| UI Relay 진입 | /api/relay/{businessCode}/online | tcf-ui에서 Gateway 또는 업무 WAR로 Relay |
| OM 진입 | /om/online | OM 운영관리 업무 호출 |

Gateway는 다음을 수행한다.
## 1. URL Path businessCode 추출

## 2. JSON Header businessCode와 비교

## 3. 세션/JWT 1차 확인

## 4. ServiceId 형식 확인

## 5. Gateway Route 테이블 조회

## 6. 대상 업무 WAR URL 결정

## 7. Header와 Cookie 전달

## 8. Downstream /{businessCode}/online 호출

Gateway 라우팅 설계 기준에서도 businessCode로 /sv/online, /om/online 등 대상 업무 WAR를 결정하고, Path 업무코드와 JSON Header businessCode가 다르면 차단해야 한다고 정리되어 있다.

### 22.12 Apache Routing 기준

Apache는 외부 URL을 업무 Context로 전달한다.
ProxyPass        /sv/  http://nsight-ap01:8080/sv/
ProxyPassReverse /sv/  http://nsight-ap01:8080/sv/

ProxyPass        /om/  http://nsight-ap01:8080/om/
ProxyPassReverse /om/  http://nsight-ap01:8080/om/

ProxyPass        /cm/  http://nsight-ap01:8080/cm/
ProxyPassReverse /cm/  http://nsight-ap01:8080/cm/

Apache 기준은 다음과 같다.
| 항목 | 기준 |
| --- | --- |
| URL Prefix | 업무 Context와 일치 |
| Reverse Proxy | Tomcat 업무 Context로 전달 |
| Sticky Session | 필요 시 JSESSIONID 기준 |
| Timeout | 온라인 거래 기준 5~10초 수준 |
| Logging | Access Log에 URI, 응답시간, 상태코드 기록 |
| 보안 | SSL 종료, Header 보정, 내부 URL 은닉 |

NSIGHT Apache 운영 기준에서도 Apache는 SSL Termination, Reverse Proxy, URL Routing, Load Balancing, Sticky Session, Logging, 장애 격리 역할을 수행한다고 정의되어 있다.

### 22.13 Local / Dev / Prod Endpoint 기준

환경별 Endpoint는 도메인과 Host만 다르고 Path 구조는 동일해야 한다.
| 환경 | 호출 예시 | 기준 | local |
| --- | --- | --- | --- |
| http://localhost:8086/sv/online | 개발자 로컬 | dev | https://dev-nh-marketing.com/sv/online |
| 개발계 | stg | https://stg-nh-marketing.com/sv/online | 검증계 |

prod
https://nh.marketing.com/sv/online
운영계
Gateway를 사용하는 경우는 다음과 같다.
| 환경 | Gateway 호출 예시 | local |
| --- | --- | --- |
| http://localhost:8080/gw/SV/online | dev | https://dev-nh-marketing.com/gw/SV/online |
| stg | https://stg-nh-marketing.com/gw/SV/online | prod |

https://nh.marketing.com/gw/SV/online
환경별 설정은 local / dev / stg / prd Profile로 분리하고, Endpoint Path 자체는 환경별로 바꾸지 않는다.

### 22.14 업무별 Endpoint 표준표

| 업무코드 | 업무명 | Context Path |
| --- | --- | --- |
| Online Endpoint | WAR | CC |
| Common | /cc | POST /cc/online |
| cc.war | IC | Integration Customer |
| /ic | POST /ic/online | ic.war |
| PC | Private Customer | /pc |

```text
POST /pc/online
pc.war
BC
```

Business Customer
/bc
```text
POST /bc/online
bc.war
MS
```

Marketing Support
/ms
```text
POST /ms/online
ms.war
SV
```

Single View
/sv
```text
POST /sv/online
sv.war
PD
Product
```

/pd
```text
POST /pd/online
pd.war
CM
Campaign
```

/cm
```text
POST /cm/online
cm.war
EB
```

Event Batch
/eb
```text
POST /eb/online
eb.war
EP
```

Event Processing
/ep
```text
POST /ep/online
ep.war
BP
```

Business Process
/bp
```text
POST /bp/online
bp.war
BD
```

Big Data
/bd
```text
POST /bd/online
bd.war
SS
```

Statistics Service
/ss
```text
POST /ss/online
ss.war
CS
```

Customer Service
/cs
```text
POST /cs/online
cs.war
CT
Contact
```

/ct
```text
POST /ct/online
ct.war
MG
Message
```

/mg
```text
POST /mg/online
mg.war
OM
```

Operation Management
/om
```text
POST /om/online
om.war
```

### 22.15 요청 전문 예시

SV 고객 요약 조회 호출 예시는 다음과 같다.
```text
POST /sv/online HTTP/1.1
```

Host: nh.marketing.com
Content-Type: application/json; charset=UTF-8
Authorization: Bearer {accessToken}
Cookie: JSESSIONID=...

```json
{
  "header": {
    "systemId": "NSIGHT-MP",
    "businessCode": "SV",
    "serviceId": "SV.Customer.selectSummary",
    "transactionCode": "SV-INQ-0001",
    "processingType": "INQUIRY",
    "serviceName": "고객요약조회",
    "guid": "G202607040001",
    "traceId": "T202607040001",
    "transactionId": "TX202607040001",
    "channelId": "WEBTOP",
    "userId": "U123456",
    "branchId": "001234",
    "requestTime": "2026-07-04T18:30:00+09:00"
  },
  "body": {
    "customerNo": "CUST00000001"
  }
}
```

### 22.16 응답 전문 예시

```json
{
  "header": {
    "systemId": "NSIGHT-MP",
    "businessCode": "SV",
    "serviceId": "SV.Customer.selectSummary",
    "transactionCode": "SV-INQ-0001",
    "processingType": "INQUIRY",
    "serviceName": "고객요약조회",
    "guid": "G202607040001",
    "traceId": "T202607040001",
    "transactionId": "TX202607040001",
    "channelId": "WEBTOP",
    "userId": "U123456",
    "branchId": "001234",
    "responseTime": "2026-07-04T18:30:01+09:00",
    "elapsedTimeMs": 120
  },
  "result": {
    "resultCode": "SUCCESS",
    "message": "정상 처리되었습니다."
  },
  "body": {
    "customerNo": "CUST00000001",
    "customerName": "홍길동",
    "customerGrade": "VIP",
    "productCount": 5
  },
  "error": null
}
```

### 22.17 Endpoint Validation 기준

Online Endpoint 진입 시 다음 검증을 수행한다.
| 검증 항목 | 기준 |
| --- | --- |
| 오류코드 예시 | HTTP Method |
| POST만 허용 | E-TCF-HTTP-0001 |
| Content-Type | application/json만 허용 |
| E-TCF-HTTP-0002 | Path businessCode |
| 지원 업무코드인지 확인 | E-TCF-ROUTE-0001 |
| Header businessCode | Path와 일치 |
| E-TCF-HDR-0011 | ServiceId Prefix |
| businessCode와 일치 | E-TCF-HDR-0012 |
| TransactionCode Prefix | businessCode와 일치 |
| E-TCF-HDR-0013 | ServiceId 등록 여부 |
| Service Catalog 확인 | E-TCF-SVC-0001 |
| 거래통제 | Allow-List 확인 |
| E-TCF-CTL-0001 | 세션/JWT |
| 로그인 상태 확인 | E-TCF-AUTH-0001 |
| 권한 | 사용자 권한 확인 |
| E-TCF-AUTHZ-0001 |  |

### 22.18 Health Check Endpoint와 구분

Online Endpoint와 Health Check Endpoint는 분리한다.
| 구분 | Endpoint | 설명 |
| --- | --- | --- |
| Liveness | /actuator/health/liveness | 프로세스 생존 여부 |
| Readiness | /actuator/health/readiness | 업무 요청 수신 가능 여부 |
| Deep Check | /actuator/health 또는 별도 /om/health/deep | DB, SessionDB, Cache 상태 |
| Smoke Check | POST /{businessCode}/online | 실제 ServiceId 거래 확인 |
| Online 거래 | POST /{businessCode}/online | 실제 업무 거래 |

Health Check 설계에서도 Liveness, Readiness, Deep Check, Smoke Check를 분리하고, Smoke Check는 실제 serviceId 기반 거래가 정상 수행되는지 확인하는 구조가 적합하다고 정의되어 있다.

### 22.19 파일 업다운로드 Endpoint 기준

파일 업다운로드는 두 가지 방식 중 하나로 정리한다.
| 방식 | 기준 | 사용 예 | 표준 전문 방식 |
| --- | --- | --- | --- |
| POST /ud/online | 파일 메타 조회, 다운로드 요청, 권한 검증 | 전용 Stream 방식 | /ud/files/{fileId} |

실제 파일 Stream 다운로드
권장 구조는 다음이다.
## 1. POST /ud/online

   serviceId = UD.File.prepareDownload
   → 권한 검증, 감사 로그 시작, 다운로드 토큰 발급

## 2. GET /ud/files/{downloadToken}

   → 실제 파일 Stream 응답

단, 실제 파일 원본을 JSON Body에 Base64로 넣는 방식은 금지한다.

### 22.20 Timeout 기준

Online Endpoint는 장시간 요청을 허용하지 않는다.
| Timeout 구분 | 기준 |
| --- | --- |
| Gateway Read Timeout | 5~10초 |
| Apache Proxy Timeout | 5~10초 |
| Online Transaction Timeout | 기본 5초 |
| DB Query Timeout | 기본 3초 |
| 외부 연계 Timeout | 서비스별 정책 |
| 파일 다운로드 | 별도 정책 |

Timeout 관리 구조에서도 거래통제는 실행 허용 여부를 판단하고, Timeout 정책은 실행 제한시간을 판단하므로 TCF_TRANSACTION_CONTROL과 TCF_SERVICE_TIMEOUT_POLICY를 분리하는 것이 기준이다.

### 22.21 Endpoint 오류 응답 기준

Endpoint 오류도 표준 응답 전문으로 반환한다.
```json
{
  "header": {
    "businessCode": "SV",
    "serviceId": "SV.Customer.selectSummary",
    "transactionCode": "SV-INQ-0001",
    "guid": "G202607040001",
    "traceId": "T202607040001",
    "responseTime": "2026-07-04T18:30:01+09:00",
    "elapsedTimeMs": 15
  },
  "result": {
    "resultCode": "FAIL",
    "message": "요청 정보를 확인해 주십시오."
  },
  "body": null,
  "error": {
    "errorCode": "E-TCF-HDR-0012",
    "errorMessage": "업무코드와 ServiceId가 일치하지 않습니다.",
    "errorType": "VALIDATION",
    "fieldErrors": []
  }
}
```

오류 응답에는 StackTrace, 서버 IP, SQL, DB 오류 상세, 내부 클래스명을 노출하지 않는다.

### 22.22 Endpoint와 거래로그 연계

Online Endpoint 호출 시 거래로그에는 다음 항목을 남긴다.
| 항목 | 설명 |
| --- | --- |
| REQUEST_URI | /sv/online |
| HTTP_METHOD | POST |
| BUSINESS_CODE | SV |
| SERVICE_ID | SV.Customer.selectSummary |
| TRANSACTION_CODE | SV-INQ-0001 |
| GUID | 거래 추적 ID |
| TRACE_ID | 내부 추적 ID |
| USER_ID | 사용자 |
| BRANCH_ID | 지점 |
| CHANNEL_ID | 채널 |
| CLIENT_IP | 요청 IP |
| TX_STATUS | PROCESSING, SUCCESS, FAIL, TIMEOUT |
| ELAPSED_TIME_MS | 처리시간 |
| ERROR_CODE | 오류코드 |

거래로그 구조에서도 거래 시작 시 PROCESSING 로그를 생성하고 종료 시 SUCCESS / FAIL / TIMEOUT / UNKNOWN 상태로 갱신하며, GUID, TraceId, ServiceId, 거래코드 기준으로 운영 추적하도록 정의되어 있다.

### 22.23 Endpoint 보안 기준

| 보안 항목 | 기준 |
| --- | --- |
| HTTPS | 외부 구간 HTTPS 적용 |
| Authorization | JWT 또는 세션 기반 인증 |
| Cookie | Secure, HttpOnly, SameSite 적용 |
| CSRF | 화면 구조에 따라 적용 검토 |
| Content-Type | JSON만 허용 |
| Method 제한 | POST만 허용 |
| CORS | 허용 Origin 제한 |
| Client IP | 서버 기준 보정 |
| Header 위변조 | 세션/JWT 기준 재검증 |
| 거래통제 | 미등록 거래 기본 차단 |
| 오류응답 | 내부 정보 노출 금지 |

Spring 환경설정 기준에서도 보안 설정은 SSO, JWT, CSRF, Cookie, 마스킹 등을 포함하고, 로그 설정은 GUID, TraceId, MDC, 감사로그와 연결되도록 정리되어 있다.

### 22.24 Endpoint 작성 시 금지 사항

| 금지 사항 | 사유 |
| --- | --- |
| ServiceId별 URL 생성 | Dispatcher 구조 훼손 |
| Controller에서 업무별 Method 다수 생성 | TCF 공통 처리 우회 |
| GET QueryString으로 고객번호 전달 | URL 로그에 개인정보 노출 위험 |
| Header 없이 Body만으로 업무 실행 | 거래통제 불가 |
| businessCode와 URL Path 불일치 허용 | 라우팅 오류 |
| Gateway에서 Header 검증 없이 Relay | 잘못된 업무 호출 가능 |
| 파일 원본을 JSON Body에 Base64 포함 | Heap 부담과 성능 저하 |
| Endpoint별 임의 응답 구조 사용 | 표준 응답 훼손 |
| 오류를 HTTP 500 HTML로 반환 | 화면·운영 추적 어려움 |
| 운영에서 Endpoint Path 임의 변경 | Apache, Gateway, UI 영향 |

### 22.25 개발자 체크리스트

| 점검 항목 | 확인 |
| --- | --- |
| Online Endpoint가 POST /{businessCode}/online 형식인가? | □ |
| Context Path가 업무코드 소문자와 일치하는가? | □ |
| HTTP Method가 POST인가? | □ |
| Content-Type이 application/json; charset=UTF-8인가? | □ |
| 요청 전문이 header + body 구조인가? | □ |
| 응답 전문이 header + result + body + error 구조인가? | □ |
| URL Path의 업무코드와 Header businessCode가 일치하는가? | □ |
| serviceId Prefix가 businessCode와 일치하는가? | □ |
| transactionCode Prefix가 businessCode와 일치하는가? | □ |
| Controller가 TCF.process()만 호출하는가? | □ |
| Controller에서 업무 Service를 직접 호출하지 않는가? | □ |
| ServiceId별 URL을 만들지 않았는가? | □ |
| Gateway 사용 시 Route 테이블과 Endpoint가 일치하는가? | □ |
| Apache ProxyPass 경로와 Context Path가 일치하는가? | □ |
| 거래로그에 Request URI, ServiceId, 거래코드가 기록되는가? | □ |
| 오류 응답이 StandardResponse 구조인가? | □ |
| Health Check Endpoint와 Online Endpoint가 분리되어 있는가? | □ |

### 22.26 마무리말

Online Endpoint는 단순 URL이 아니다.NSIGHT TCF Framework에서 Online Endpoint는 모든 온라인 거래가 TCF 공통 파이프라인으로 들어오도록 강제하는 표준 진입점이다.
Endpoint가 표준화되면 다음이 가능해진다.
| 효과 | 설명 |
| --- | --- |
| 개발 단순화 | 업무별 Controller URL 증가 방지 |
| 통제 일원화 | 모든 거래가 STF 전처리 통과 |
| 운영 추적 | URI, ServiceId, 거래코드 기준 로그 분석 |
| 보안 강화 | 세션, 권한, 거래통제 공통 적용 |
| Gateway 라우팅 | businessCode 기준 대상 업무 WAR 결정 |
| 배포 안정성 | Context, WAR, Apache Routing 기준 통일 |

### 22.27 시사점

NSIGHT 개발에서 Endpoint를 REST식으로 계속 늘리면, 시간이 지날수록 Controller가 비대해지고, 거래통제·권한·Timeout·로그 기준이 흩어진다.
따라서 NSIGHT TCF 개발자는 다음 원칙을 지켜야 한다.
업무별 Endpoint는 하나로 단순화한다.실행할 업무는 URL이 아니라 Header의 ServiceId로 결정한다.모든 온라인 거래는 POST /{businessCode}/online을 통해 TCF 공통 파이프라인을 통과해야 한다.

## 소스·관련 문서

| 참고 |
|------|

> znsight-guide-word: `통합 (27).docx`

| [온라인처리.md](../zdoc/온라인처리.md) |

## 코드베이스 정정 (develop 기준)

| 항목 | 값 |
|------|-----|
| 업무 WAR | ic, pc, ms, sv, pd, eb, ep, ss, mg + tcf-om |
| ztomcat deploy | `ztomcat/deploy-wars.sh` 13 WAR |
| buildZtomcatWars | 15 WAR |
| bootRun | gateway 8100, uj 8102, jwt 8110, ui 8099 |


---

← [21. Header 작성 기준](./21-Header-작성-기준.md) · [23. TransactionHandler 개발](./23-TransactionHandler-개발.md) →