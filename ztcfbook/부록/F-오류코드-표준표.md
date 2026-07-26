# 부록 F. 오류코드 표준표

| 항목 | 내용 |
| --- | --- |
| **부록** | F |
| **상태** | 집필 완료 |
| **목차** | [00-목차](../00-목차.md) |

---

## F.1 오류코드의 역할

NSIGHT TCF에서 오류코드는 단순한 메시지 번호가 아니다. 표준 응답, 거래로그, 감사로그, 장애 추적, 운영자 조치, 재처리 판단, 모니터링 알림의 **공통 식별자**이다.

```text
Exception 발생
  ↓
TCF / ETF에서 표준 오류코드로 변환
  ↓
OM_ERROR_CODE 조회 (사용자·운영자 메시지, 조치 가이드)
  ↓
StandardResponse 조립
  ↓
거래로그 / 감사로그 / 모니터링 기록
```

모든 예외는 TCF/ETF에서 표준 오류코드로 변환하고, `OM_ERROR_CODE`에서 메시지를 조회한 뒤 표준 응답 전문으로 조립하는 구조를 따른다.

---

## F.2 오류코드 기본 형식

오류코드는 다음 형식을 사용한다.

```text
E-{DOMAIN}-{CATEGORY}-{NNNN}
```

| 구성요소 | 의미 | 표기 |
| --- | --- | --- |
| `E` | Error 고정값 | `E` |
| `DOMAIN` | 오류 발생 영역 | 대문자 (TCF, SV, OM, UD …) |
| `CATEGORY` | 오류 분류 | 대문자 (HDR, VAL, BIZ, DB …) |
| `NNNN` | 일련번호 | 4자리 숫자 (0001~) |

예시: `E-TCF-HDR-0001`, `E-SV-BIZ-0001`, `E-OM-AUTHZ-0001`, `E-UD-FILE-0001`

---

## F.3 DOMAIN 표준표

| DOMAIN | 의미 | 적용 영역 | 예시 |
| --- | --- | --- | --- |
| TCF | Transaction Control Framework | STF, Dispatcher, ETF | `E-TCF-HDR-0001` |
| COM | Common | 공통 유틸, Validator | `E-COM-VAL-0001` |
| IC | Integration Customer | 통합고객 | `E-IC-BIZ-0001` |
| PC | Private Customer | 개인고객 | `E-PC-BIZ-0001` |
| MS | Mini Single View | 미니싱글뷰 | `E-MS-BIZ-0001` |
| SV | Single View | 고객 요약·상세 조회 | `E-SV-BIZ-0001` |
| PD | Product | 상품정보 | `E-PD-BIZ-0001` |
| CM | Campaign | 캠페인 | `E-CM-BIZ-0001` |
| EB | EBM | 이벤트 기반 마케팅 | `E-EB-BIZ-0001` |
| EP | Event Processing | 이벤트 처리 | `E-EP-BIZ-0001` |
| SS | Sales Support | 영업지원 | `E-SS-BIZ-0001` |
| MG | Message | 메시지 발송·관리 | `E-MG-SND-0001` |
| OM | Operation Management | 운영관리 | `E-OM-AUTHZ-0001` |
| UD | Upload / Download | 파일 업·다운로드 | `E-UD-FILE-0001` |
| BT | Batch | 배치 Job, 스케줄 | `E-BT-JOB-0001` |
| GW | Gateway | Gateway 라우팅·인증 | `E-GW-ROUTE-0001` |
| JWT | JWT | 토큰 발급·검증 | `E-JWT-AUTHN-0001` |

---

## F.4 CATEGORY 표준표

| CATEGORY | 의미 | 발생 위치 | 예시 |
| --- | --- | --- | --- |
| HDR | Header 오류 | STF, Gateway | `E-TCF-HDR-0001` |
| MSG | 전문 구조 오류 | TCF 전문 Parser | `E-TCF-MSG-0001` |
| VAL | 입력값 검증 오류 | STF, Handler, Rule | `E-TCF-VAL-0001` |
| SVC | ServiceId 오류 | Dispatcher | `E-TCF-SVC-0001` |
| TRX | 거래코드 오류 | STF, 거래 Catalog | `E-TCF-TRX-0001` |
| CTL | 거래통제 오류 | Transaction Control | `E-TCF-CTL-0001` |
| SES | 세션 오류 | Session 관리 | `E-TCF-SES-0001` |
| AUTHN | 인증 오류 | Login, SSO, JWT | `E-TCF-AUTHN-0001` |
| AUTHZ | 권한 오류 | 메뉴·기능·데이터 권한 | `E-TCF-AUTHZ-0001` |
| BIZ | 업무 오류 | Service, Rule | `E-SV-BIZ-0001` |
| DB | DB 오류 | DAO, Mapper, HikariCP | `E-TCF-DB-0001` |
| IF | 연계 오류 | 외부 API, EAI | `E-TCF-IF-0001` |
| TIME | Timeout 오류 | TCF, DB, 외부연계 | `E-TCF-TIME-0001` |
| FILE | 파일 오류 | 업·다운로드, Storage | `E-UD-FILE-0001` |
| CACHE | 캐시 오류 | Cache 조회·갱신 | `E-TCF-CACHE-0001` |
| JOB | Batch Job 오류 | Batch Handler | `E-BT-JOB-0001` |
| SCH | Scheduler 오류 | Scheduler | `E-BT-SCH-0001` |
| ROUTE | 라우팅 오류 | Gateway | `E-GW-ROUTE-0001` |
| SYS | 시스템 오류 | 미처리 예외 | `E-TCF-SYS-9999` |
| SEC | 보안 오류 | 위변조, 차단 IP | `E-TCF-SEC-0001` |

---

## F.5 ResultCode와 ErrorCode 구분

| 구분 | 역할 | 예시 |
| --- | --- | --- |
| resultStatus | 처리 상태 | SUCCESS, FAIL, TIMEOUT, UNKNOWN |
| resultCode | 화면·연계 응답용 결과 코드 | S0000, E0001, T0001 |
| errorCode | 운영·로그·조치 기준 상세 식별자 | E-TCF-HDR-0001 |
| userMessage | 사용자 안내 메시지 | 입력값을 확인해 주십시오. |
| operatorMessage | 운영자 상세 메시지 | header.serviceId is blank |
| actionGuide | 조치 가이드 | 요청 Header를 확인하십시오. |

권장 resultCode:

| resultStatus | resultCode | 의미 |
| --- | --- | --- |
| SUCCESS | S0000 | 정상 |
| FAIL | E0001 | 일반 실패 |
| VALIDATION_FAIL | E0002 | 입력값 오류 |
| AUTH_FAIL | E0003 | 인증·권한 오류 |
| TIMEOUT | T0001 | Timeout |
| UNKNOWN | U0001 | 처리상태 불명확 |
| SYSTEM_ERROR | E9999 | 시스템 오류 |

---

## F.6 TCF 공통 오류코드 (대표)

### Header · 전문 · ServiceId

| 오류코드 | 오류명 | 발생 조건 | 사용자 메시지 |
| --- | --- | --- | --- |
| E-TCF-HDR-0001 | ServiceId 누락 | header.serviceId 없음 | 요청 정보가 올바르지 않습니다. |
| E-TCF-HDR-0002 | TransactionCode 누락 | header.transactionCode 없음 | 요청 정보가 올바르지 않습니다. |
| E-TCF-HDR-0003 | BusinessCode 누락 | header.businessCode 없음 | 요청 정보가 올바르지 않습니다. |
| E-TCF-MSG-0001 | JSON 파싱 오류 | JSON 형식 오류 | 요청 전문 형식이 올바르지 않습니다. |
| E-TCF-SVC-0001 | ServiceId 미등록 | Catalog에 없음 | 요청한 서비스를 찾을 수 없습니다. |
| E-TCF-SVC-0002 | Handler 없음 | Registry에 Handler 없음 | 요청한 서비스를 처리할 수 없습니다. |
| E-TCF-TRX-0001 | 거래코드 형식 오류 | `{업무}-{유형}-{번호}` 아님 | 거래 정보가 올바르지 않습니다. |
| E-TCF-CTL-0001 | 미등록 거래 | Allow-List 없음 | 해당 거래를 수행할 수 없습니다. |
| E-TCF-CTL-0004 | 차단 거래 | 차단 상태 | 현재 차단된 거래입니다. |

### Validation · DB · Timeout · System

| 오류코드 | 오류명 | 발생 조건 | 사용자 메시지 |
| --- | --- | --- | --- |
| E-TCF-VAL-0001 | 필수값 누락 | 필수 항목 없음 | 입력값을 확인해 주십시오. |
| E-TCF-VAL-0002 | 형식 오류 | 날짜·숫자·코드 형식 오류 | 입력 형식이 올바르지 않습니다. |
| E-TCF-DB-0001 | DB 처리 오류 | 일반 DB 오류 | 일시적으로 처리할 수 없습니다. |
| E-TCF-DB-0002 | Query Timeout | SQL 제한시간 초과 | 조회가 지연되고 있습니다. |
| E-TCF-IF-0001 | 연계 Timeout | 외부 응답 지연 | 연계 시스템 응답이 지연되고 있습니다. |
| E-TCF-TIME-0001 | 온라인 전체 Timeout | 거래 제한시간 초과 | 처리 상태를 확인해 주십시오. |
| E-TCF-SES-0001 | 세션 만료 | Session 없음/만료 | 다시 로그인해 주십시오. |
| E-TCF-AUTHZ-0001 | 메뉴권한 없음 | 메뉴 접근 불가 | 접근 권한이 없습니다. |
| E-TCF-SYS-9999 | 미처리 시스템 오류 | 예상 못한 RuntimeException | 시스템 처리 중 오류가 발생했습니다. |

---

## F.7 업무별 오류코드 예시

### SV (Single View)

| 오류코드 | 오류명 | 발생 조건 | 사용자 메시지 |
| --- | --- | --- | --- |
| E-SV-VAL-0001 | 고객번호 누락 | customerNo 없음 | 고객번호를 입력해 주십시오. |
| E-SV-BIZ-0001 | 고객정보 없음 | 조회 결과 없음 | 조회 결과가 없습니다. |
| E-SV-AUTHZ-0001 | 타 지점 조회 제한 | 데이터권한 없음 | 조회 권한이 없습니다. |
| E-SV-DB-0001 | 고객조회 DB 오류 | RDW 조회 오류 | 일시적으로 조회할 수 없습니다. |

### CM (Campaign)

| 오류코드 | 오류명 | 발생 조건 | 사용자 메시지 |
| --- | --- | --- | --- |
| E-CM-VAL-0001 | 캠페인명 누락 | 캠페인명 없음 | 캠페인명을 입력해 주십시오. |
| E-CM-BIZ-0001 | 캠페인 없음 | 조회 결과 없음 | 캠페인을 찾을 수 없습니다. |
| E-CM-BIZ-0002 | 실행 불가 상태 | 상태가 DRAFT 아님 | 현재 상태에서는 실행할 수 없습니다. |

### OM (Operation Management)

| 오류코드 | 오류명 | 발생 조건 | 사용자 메시지 |
| --- | --- | --- | --- |
| E-OM-AUTHZ-0001 | 관리자 권한 없음 | OM 기능권한 없음 | 운영관리 권한이 없습니다. |
| E-OM-SVC-0001 | ServiceId 중복 | 동일 ServiceId 등록 | 이미 등록된 ServiceId입니다. |
| E-OM-ERR-0001 | 오류코드 중복 | 동일 오류코드 존재 | 이미 등록된 오류코드입니다. |

### UD (파일) · Gateway · JWT

| 오류코드 | 오류명 | 사용자 메시지 |
| --- | --- | --- |
| E-UD-FILE-0001 | 파일 없음 | 파일을 찾을 수 없습니다. |
| E-UD-AUTHZ-0001 | 다운로드 권한 없음 | 파일 다운로드 권한이 없습니다. |
| E-GW-ROUTE-0001 | Path 업무코드 오류 | 요청 경로가 올바르지 않습니다. |
| E-GW-TIME-0001 | Downstream Timeout | 처리 상태를 확인해 주십시오. |
| E-JWT-AUTHN-0003 | 토큰 만료 | 다시 로그인해 주십시오. |

---

## F.8 OM_ERROR_CODE 등록

오류코드는 `OM_ERROR_CODE` 테이블에서 중앙 관리한다.

| 컬럼 | 설명 | 예시 |
| --- | --- | --- |
| ERROR_CODE | 표준 오류코드 | E-TCF-HDR-0001 |
| ERROR_NAME | 오류명 | ServiceId 누락 |
| ERROR_CATEGORY | 오류 분류 | HDR |
| ERROR_LEVEL | 심각도 | WARN, ERROR, CRITICAL |
| RESULT_CODE | 응답 결과코드 | E0002 |
| HTTP_STATUS | HTTP 상태코드 | 200 |
| USER_MESSAGE | 사용자 메시지 | 입력값을 확인해 주십시오. |
| OPERATOR_MESSAGE | 운영자 메시지 | header.serviceId is blank |
| ACTION_GUIDE | 조치 가이드 | Header의 serviceId를 확인하십시오. |
| NOTIFY_TARGET | 알림 대상 | 개발팀, 운영팀 |
| RETRY_YN | 재시도 가능 여부 | Y, N |
| AUDIT_YN | 감사로그 대상 | Y, N |
| MASK_DETAIL_YN | 상세 오류 마스킹 | Y, N |
| USE_YN | 사용 여부 | Y, N |

등록 예시:

```sql
INSERT INTO OM_ERROR_CODE (
      ERROR_CODE, ERROR_NAME, ERROR_CATEGORY, ERROR_LEVEL
    , RESULT_CODE, HTTP_STATUS
    , USER_MESSAGE, OPERATOR_MESSAGE, ACTION_GUIDE
    , NOTIFY_TARGET, RETRY_YN, AUDIT_YN, MASK_DETAIL_YN, USE_YN
    , CREATED_BY, CREATED_AT
) VALUES (
      'E-TCF-HDR-0001', 'ServiceId 누락', 'HDR', 'WARN'
    , 'E0002', 200
    , '입력값을 확인해 주십시오.'
    , 'header.serviceId is blank'
    , '요청 Header의 serviceId 값을 확인하십시오.'
    , '개발팀', 'N', 'N', 'Y', 'Y'
    , 'admin', SYSDATE
);
```

신규 오류코드는 **OM에 먼저 등록**한 뒤 코드에서 사용한다. 폐기 시 삭제하지 않고 `USE_YN = N`으로 관리하며, 번호 재사용은 금지한다.

---

## F.9 응답 매핑 · Java 사용 예시

### Java 상수

```java
public final class TcfErrorCodes {
    private TcfErrorCodes() {}
    public static final String HEADER_SERVICE_ID_REQUIRED = "E-TCF-HDR-0001";
    public static final String SERVICE_ID_NOT_REGISTERED = "E-TCF-SVC-0001";
    public static final String TRANSACTION_CONTROL_DENIED = "E-TCF-CTL-0001";
    public static final String VALIDATION_REQUIRED_FIELD = "E-TCF-VAL-0001";
    public static final String QUERY_TIMEOUT = "E-TCF-DB-0002";
    public static final String SYSTEM_ERROR = "E-TCF-SYS-9999";
}
```

### 업무 예외 발생

```java
// Validation
if (request.getCustomerNo() == null || request.getCustomerNo().isBlank()) {
    throw new ValidationException(
        "E-SV-VAL-0001",
        "고객번호를 입력해 주십시오.",
        List.of(new FieldErrorDetail("customerNo", "고객번호는 필수입니다."))
    );
}

// Business
if (customer == null) {
    throw new BusinessException(
        "E-SV-BIZ-0001",
        "조회 결과가 없습니다.",
        "SV 고객요약조회 결과 없음. customerNo=" + customerNo
    );
}

// DB Timeout
try {
    return mapper.selectCustomerSummary(criteria);
} catch (QueryTimeoutException e) {
    throw new DatabaseException(
        "E-TCF-DB-0002",
        "조회가 지연되고 있습니다.",
        "SQL Timeout. sqlId=SvCustomerMapper.selectCustomerSummary",
        e
    );
}
```

ETF는 `errorCode`를 기준으로 `OM_ERROR_CODE`를 조회하고, `StandardResponse`의 `error` 영역에 `resultCode`, `errorCode`, `userMessage`를 조립한다. 사용자 응답에는 SQL, StackTrace, 서버 IP, 개인정보를 **노출하지 않는다**.

---

## F.10 오류 심각도 · Retry · 감사 기준

| ERROR_LEVEL | 의미 | 알림 |
| --- | --- | --- |
| INFO | 정상적 업무 실패 (조회 결과 없음) | 없음 |
| WARN | Validation, 권한, 거래 차단 | 필요 시 집계 |
| ERROR | DB·연계·파일 오류 | 운영팀 알림 |
| CRITICAL | 전체 Timeout, Gateway 장애 | 즉시 알림 |
| FATAL | 기동 실패, 설정 불능 | 즉시 장애 전파 |

| 오류 유형 | Retry | Audit | Mask |
| --- | --- | --- | --- |
| Header / Validation | N | N | Y |
| 인증 / 권한 | N | Y | Y |
| 거래통제 | N | Y | Y |
| DB Connection | Y | Y | Y |
| Query Timeout | Y | Y | Y |
| 시스템 / 보안 | N | Y | Y |

Timeout은 FAIL로만 처리하지 않고, 처리 결과가 불명확하면 `TIMEOUT` 또는 `UNKNOWN` 상태로 거래로그에 남긴다.

---

## F.11 작성 금지 패턴 · 체크리스트

### 금지 패턴

| 금지 패턴 | 문제 |
| --- | --- |
| `ERR001`, `SV-ERROR-1` | 영역·분류·형식 불일치 |
| `E-sv-biz-0001` | 소문자 사용 |
| `E_SV_BIZ_0001` | 구분자 위반 |
| `RuntimeException("오류")` | 운영 추적 불가 |
| SQL 오류를 화면에 표시 | 보안 위험 |

### 체크리스트

| 점검 항목 | 확인 기준 |
| --- | --- |
| 형식 | `E-{DOMAIN}-{CATEGORY}-{NNNN}` 형식인가? |
| DOMAIN | 업무코드 또는 공통 도메인과 일치하는가? |
| CATEGORY | 표준 분류값을 사용하는가? |
| 메시지 분리 | 사용자·운영자 메시지가 분리되어 있는가? |
| OM 등록 | `OM_ERROR_CODE`에 등록되어 있는가? |
| 로그 연계 | GUID, TraceId, ServiceId와 함께 기록되는가? |
| 마스킹 | 상세 오류가 사용자에게 노출되지 않는가? |

---

## 요약

오류코드 `E-{DOMAIN}-{CATEGORY}-{NNNN}`은 예외를 화면에 보여주기 위한 값이 아니라, **장애를 표준 응답·로그·감사·운영 조치로 연결하는 운영 식별자**이다. TCF 공통 오류는 TCF·GW·JWT DOMAIN에서, 업무 오류는 SV·CM·OM 등 각 DOMAIN에서 채번한다. `OM_ERROR_CODE`에 사용자·운영자 메시지와 조치 가이드를 등록하고, ETF가 `StandardResponse`로 조립한다. 사용자 메시지는 짧고 명확하게, 운영자 메시지는 GUID·ServiceId·SQL ID 기준으로 추적 가능하게 작성한다.

---

## 이전 · 다음

| | |
| --- | --- |
| ← 이전 | [부록 E](./E-Mapper-XML-템플릿.md) |
| → 다음 | [부록 G](./G-application-yml-템플릿.md) |

---

## 출처 색인

- [znsight-man/부록F-오류코드-표준표.md](../../znsight-man/부록F-오류코드-표준표.md)
- [znsight-man/21-Header-작성-기준.md](../../znsight-man/21-Header-작성-기준.md)
- [zdoc/예외처리.md](../../zdoc/예외처리.md)
- [znsight-man/35-예외처리-기준.md](../../znsight-man/35-예외처리-기준.md)
- [znsight-man/36-ETF-오류응답-기준.md](../../znsight-man/36-ETF-오류응답-기준.md)
