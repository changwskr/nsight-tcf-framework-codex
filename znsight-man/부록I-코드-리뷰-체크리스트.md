# 부록 I. 코드 리뷰 체크리스트

> **NSIGHT TCF 개발 Manual** · 원본: [`znsight-guide-word`](../znsight-guide-word/) · 갱신: 2026-07-05

## I. 코드 리뷰 체크리스트

### I.1 도입 전 안내말

본 부록은 NSIGHT TCF Framework 기반 업무 개발 시 Pull Request, Merge Request, 선도개발 검증, 품질 Gate에서 사용할 코드 리뷰 체크리스트를 정의한다.
NSIGHT에서 코드 리뷰는 단순히 “문법이 맞는지”, “코드가 예쁜지”를 보는 활동이 아니다.코드 리뷰는 다음 항목을 확인하는 아키텍처 품질 통제 절차이다.
코드 리뷰
= 표준 구조 준수 확인
+ ServiceId / 거래코드 정합성 확인
+ 표준 전문 / 오류코드 / 로그 확인
+ SQL / 트랜잭션 / Timeout 확인
+ 보안 / 권한 / 마스킹 확인
+ 테스트 / 배포 가능성 확인

NSIGHT Java 코딩 스타일 기준에서도 코딩 표준은 단순 형식이 아니라 업무 책임 경계, 데이터 접근 통제, 오류 추적성, 보안 감사, 운영 가능성을 코드 수준에서 보장하는 표준으로 정의한다. 또한 품질 통제 수단으로 코드 인스펙션, 단위 테스트, SQL 검증, SonarQube, Checkstyle, PMD, JUnit, MyBatis SQL 검증을 사용하도록 정리되어 있다.

### I.2 코드 리뷰 대상

| 리뷰 대상 | 주요 확인 내용 | Java 소스 |
| --- | --- | --- |
| 계층 책임, 명명 규칙, 예외처리, 트랜잭션, 테스트 가능성 | DTO | Request/Response 분리, Validation, 민감정보 포함 여부 |
| Handler | ServiceId 매핑, Body 변환, Facade 호출 | Facade |
| 유스케이스 조립, 트랜잭션 경계, 공통 통제 호출 | Service / Rule | 업무 규칙, Validation, 외부연계, 재처리 판단 |
| DAO / Mapper | DB 접근 책임, Mapper 호출, DB 예외 변환 | Mapper XML |
| SQL ID, Timeout, Paging, Injection 방지 | application.yml | Profile, DB Pool, Timeout, Secret 외부화 |
| 테스트 코드 | 단위/통합/Mapper/오류/권한 테스트 | 배포 설정 |
| Gradle, bootWar, CI/CD, Health Check | 운영 등록 | ServiceId, 거래코드, 오류코드, Timeout, 거래통제 |

### I.3 코드 리뷰 절차

[개발자]
## 1. 개발 완료

## 2. 자체 체크리스트 수행

## 3. 단위 테스트 / Build 수행

```text
        ↓
[Merge Request 생성]
        ↓

```

[동료 리뷰]
## 1. 기능 구현 검토

## 2. 코드 품질 검토

```text
        ↓
```

[아키텍처 리뷰]
## 1. 계층구조 검토

## 2. 명명 / 전문 / 오류 / 로그 검토

```text
        ↓
```

[품질 리뷰]
## 1. 테스트 결과 확인

## 2. 정적분석 결과 확인

## 3. SQL 검증 확인

```text
        ↓
```

[승인 / 보완 / 반려]

### I.4 코드 리뷰 판정 기준

판정
| 의미 | 처리 기준 | 승인 |
| --- | --- | --- |
| 표준 위반 없음, 병합 가능 | Merge 가능 | 조건부 승인 |
| 경미한 보완 필요, 운영 영향 낮음 | 보완 후 Merge | 보완 요청 |
| 표준 위반 또는 테스트 부족 | 수정 후 재리뷰 | 반려 |
| 구조 위반, 보안 위험, 운영 장애 가능 | 재설계 필요 | 보류 |
| 요구사항 또는 설계 기준 불명확 | 기준 확정 후 재리뷰 | 필수 표준 위반 = 보완 요청 또는 반려 |
| 보안/SQL/거래통제 위반 = 원칙적으로 반려 | 테스트 부족 = 보완 요청 | 명명·주석·정렬 수준 = 조건부 승인 가능 |

### I.5 코드 리뷰 요약 체크리스트

| No | 점검 영역 | 핵심 질문 | 필수 |
| --- | --- | --- | --- |
| 1 | 업무 식별체계 | 업무코드, Context, WAR, Package가 일치하는가? | Y |
| 2 | ServiceId | {업무코드}.{업무대상}.{처리행위} 규칙을 지키는가? | Y |
| 3 | 거래코드 | {업무코드}-{거래유형}-{일련번호} 규칙을 지키는가? | Y |
| 4 | 계층구조 | Handler → Facade → Service → Rule → DAO/Mapper 흐름인가? | Y |
| 5 | 표준 전문 | Request/Response 구조가 표준 전문인가? | Y |
| 6 | DTO | Request, Response, 내부 DTO가 분리되어 있는가? | Y |
| 7 | Validation | 필수값, 형식, 길이, 범위, 코드값 검증이 있는가? | Y |
| 8 | SQL | Mapper XML, SQL ID, Paging, Timeout 기준을 지키는가? | Y |
| 9 | 트랜잭션 | 변경 거래의 트랜잭션 경계가 명확한가? | Y |
| 10 | Timeout | 온라인, DB, 외부연계 Timeout이 반영되어 있는가? | Y |
| 11 | 오류처리 | 표준 예외와 오류코드를 사용하는가? | Y |
| 12 | 로그 | GUID, TraceId, ServiceId, 거래코드가 로그에 남는가? | Y |
| 13 | 보안 | 권한, 마스킹, SQL Injection 방지가 반영되어 있는가? | Y |
| 14 | 테스트 | 단위/통합/Mapper/오류 테스트가 있는가? | Y |
| 15 | 운영 | OM 등록, 거래통제, 오류코드, Timeout 정책이 준비되었는가? | Y |

### I.6 업무 식별체계 리뷰

| 점검 항목 | 확인 기준 | 판정 |
| --- | --- | --- |
| 업무코드 | 표준 업무코드 사용 | □ |
| Context Path | 업무코드 소문자 기준 | □ |
| WAR 파일명 | {업무코드소문자}.war | □ |
| Gradle Module | {업무코드소문자}-service 또는 표준 모듈명 | □ |
| Package Root | 업무코드 또는 업무 도메인 기준 | □ |
| Class Prefix | 업무코드 PascalCase Prefix 사용 | □ |
| Mapper 위치 | resources/mapper/{업무코드소문자} | □ |
| 오류코드 Prefix | 업무코드 또는 TCF, OM, UD, BT 등 표준 Domain 사용 | □ |

잘못된 예시는 다음과 같다.
| 잘못된 예 | 문제 |
| --- | --- |
| 표준 | CustomerService |
| 업무코드 없음 | SvCustomerService |
| /singleview | Context 표준 불일치 |
| /sv | singleview.war |
| WAR 표준 불일치 | sv.war |
| mapper/common/CustomerMapper.xml | 업무 경계 불명확 |
| mapper/sv/SvCustomerMapper.xml | ERROR01 |
| 오류코드 표준 위반 | E-SV-BIZ-0001 |

명명 규칙은 업무코드에서 시작해 Context, WAR, Package, ServiceId, Handler, Mapper, SQL ID, 거래코드, 오류코드까지 연결되어야 한다. NSIGHT 명명 기준도 운영자가 로그만 보고 업무·거래·SQL·오류를 추적할 수 있는 이름을 좋은 이름으로 본다.

### I.7 ServiceId / 거래코드 리뷰

| 점검 항목 | 확인 기준 | 판정 |
| --- | --- | --- |
| ServiceId 형식 | {업무코드}.{업무대상}.{처리행위} | □ |
| ServiceId Prefix | Header businessCode와 일치 | □ |
| Handler 매핑 | ServiceId와 Handler 1:1 매핑 | □ |
| Handler Bean 중복 | 동일 ServiceId 중복 없음 | □ |
| 거래코드 형식 | {업무코드}-{거래유형}-{일련번호} | □ |
| 거래코드 Prefix | 업무코드와 일치 | □ |
| ServiceId-거래코드 관계 | Catalog 기준 매핑 가능 | □ |
| 처리유형 | 거래유형과 processingType 일치 | □ |
| OM 등록 | Service Catalog, 거래 Catalog 등록 대상 | □ |
| 리뷰 질문은 다음처럼 한다. | 이 ServiceId를 보고 어떤 업무의 어떤 Handler가 실행되는지 알 수 있는가? | 이 거래코드를 보고 조회/등록/수정/삭제/실행 거래인지 알 수 있는가? |
| 거래로그에서 ServiceId와 거래코드로 장애 추적이 가능한가? |  |  |

### I.8 계층구조 리뷰

| 계층 | 확인 기준 | 금지 사항 |
| --- | --- | --- |
| 판정 | Controller | 요청 수신, 표준 전문 전달 |
| 업무 판단, SQL 호출 금지 | □ | Handler |
| ServiceId 진입점, Body 변환, Facade 호출 | DAO/Mapper 직접 호출 금지 | □ |
| Facade | 유스케이스 조립, 트랜잭션 경계 | 세부 업무 Rule 직접 구현 금지 |
| □ | Service | 업무 처리 절차, DAO/Adapter 조합 |
| HTTP 응답 직접 생성 금지 | □ | Rule |
| 업무 조건 판단, Validation | DB 직접 접근 금지 | □ |
| DAO | Mapper 호출, DB 예외 변환 | 업무 판단 금지 |
| □ | Mapper | SQL 실행, Result Mapping |
| 사용자 메시지 생성 금지 | □ | Adapter |
| 외부 API, 내부 서비스 연동 캡슐화 | 연계 상세를 Controller에 노출 금지 | □ |

NSIGHT 계층 기준은 Controller, Facade, Service/Rule, DAO/Mapper, Adapter 책임을 분리하고, DAO/Mapper에서 업무 판단이나 사용자 메시지 생성을 금지한다.

### I.9 Handler 리뷰

| 점검 항목 | 확인 기준 | 판정 |
| --- | --- | --- |
| ServiceId 반환 | handler.serviceId()가 표준 ServiceId 반환 | □ |
| Body 변환 | StandardRequest.body를 업무 Request DTO로 변환 | □ |
| Facade 호출 | Handler는 Facade만 호출 | □ |
| 업무 로직 없음 | Handler 내부에 업무 판단 로직 없음 | □ |
| 예외 처리 | 불필요한 try-catch 없이 표준 예외 전파 | □ |
| 테스트 | ServiceId, Body 변환, Facade 호출 테스트 존재 | □ |

```java
@Component
@RequiredArgsConstructor
public class SvCustomerSummaryHandler implements TransactionHandler {
```

    private final SvCustomerFacade svCustomerFacade;

```java
@Override
    public String serviceId() {
        return "SV.Customer.selectSummary";
    }
@Override
    public Object handle(StandardRequest request) {
        SvCustomerSummaryRequest body =
                request.toBody(SvCustomerSummaryRequest.class);
        return svCustomerFacade.selectCustomerSummary(body);
    }
```

금지 예시는 다음이다.
```java
public Object handle(StandardRequest request) {
    // 금지: Handler에서 SQL 직접 호출
    return svCustomerMapper.selectCustomerSummary(...);
}
```

### I.10 Facade / Service / Rule 리뷰

| 영역 | 점검 항목 | 확인 기준 |
| --- | --- | --- |
| 판정 | Facade | 유스케이스 조립 |
| 여러 Service/Adapter 조합 책임만 수행 | □ | Facade |
| 트랜잭션 경계 | 변경 거래는 @Transactional(timeout = ...) 검토 | □ |
| Service | 처리 절차 | 조회, 검증, 조립, 저장 순서 명확 |
| □ | Service | 외부 연계 |
| Adapter를 통해 호출 | □ | Rule |
| 업무 조건 | 조건 판단, Validation, 정책 계산 담당 | □ |
| Rule | DB 접근 | DB 직접 접근 없음 |
| □ | 공통 | 반환 타입 |

Object, Map 남용 금지, 명확한 DTO 사용
□
NSIGHT 코딩 스타일 기준에서도 트랜잭션 경계는 유스케이스 단위에서 명확해야 하고, 타입이 불명확한 API보다 명시적 DTO 사용을 권장한다.

### I.11 DTO / Validation 리뷰

| 점검 항목 | 확인 기준 | 판정 |
| --- | --- | --- |
| Request DTO | 화면/전문 입력 구조와 매핑 | □ |
| Response DTO | 화면 반환 데이터만 포함 | □ |
| 내부 DTO | DB DTO, 외부연계 DTO와 분리 | □ |
| Entity 노출 | Entity 또는 DB Row를 화면에 직접 반환하지 않음 | □ |
| Bean Validation | @NotBlank, @Size, @Pattern 등 적용 | □ |
| 업무 Validation | Rule 또는 Validator에서 추가 검증 | □ |
| 날짜 | String 사용 시 형식 검증, 내부는 LocalDate 권장 | □ |
| 금액 | BigDecimal 사용 | □ |
| 민감정보 | 응답 DTO에 불필요한 민감정보 없음 | □ |

```java
@Getter
@Setter
public class SvCustomerSummaryRequest {
```

    @NotBlank(message = "고객번호는 필수입니다.")
    @Size(max = 20)
    private String customerNo;

    @Pattern(regexp = "\\d{8}", message = "기준일자는 yyyyMMdd 형식이어야 합니다.")
```java
private String baseDate;
}
```

입력값 검증은 필수값, 타입, 길이, 코드값, 범위, 금지문자, 페이지, 다운로드 가능 건수를 포함해야 한다.

### I.12 Mapper / SQL 리뷰

| 점검 항목 | 확인 기준 | 판정 |
| --- | --- | --- |
| Mapper XML 위치 | resources/mapper/{업무코드} 하위 | □ |
| Namespace | Java Mapper Interface FQCN과 일치 | □ |
| SQL ID | Mapper Method명과 일치 | □ |
| SELECT 컬럼 | SELECT * 금지 | □ |
| 목록조회 | Paging 적용 | □ |
| Count SQL | 목록조회 Count SQL 분리 | □ |
| ORDER BY | 고정 정렬 기준 존재 | □ |
| Timeout | RDW 3초, ADW 5초 등 기준 적용 | □ |
| Parameter Binding | #{} 사용 | □ |
| ${} 사용 | 사용자 입력 직접 삽입 금지 | □ |
| 대량조회 | 일반 온라인 거래와 분리 검토 | □ |
| RDW/ADW | Mapper 또는 DataSource 분리 | □ |

금지 예시는 다음이다.
```xml
<select id="selectList" resultType="map">
    SELECT *
    FROM RDW_CUSTOMER
    WHERE CUSTOMER_NAME LIKE '%${customerName}%'
</select>
```

표준 예시는 다음이다.
```xml
<select id="selectCustomerList"
        parameterType="com.nh.nsight.marketing.sv.dto.request.SvCustomerListRequest"
        resultMap="SvCustomerResultMap"
        timeout="3">
    SELECT
          C.CUSTOMER_NO
        , C.CUSTOMER_NAME
        , C.CUSTOMER_GRADE
        , C.BRANCH_CODE
    FROM RDW_CUSTOMER C
    WHERE 1 = 1
    <if test="customerName != null and customerName != ''">
        AND C.CUSTOMER_NAME LIKE '%' || #{customerName} || '%'
    </if>
    ORDER BY C.CUSTOMER_NO
    OFFSET #{offset} ROWS FETCH NEXT #{pageSize} ROWS ONLY
</select>
```

### I.13 트랜잭션 / Timeout 리뷰

| 점검 항목 | 확인 기준 | 판정 |
| --- | --- | --- |
| 조회 거래 | readOnly=true 검토 | □ |
| 변경 거래 | 트랜잭션 경계 명확 | □ |
| DB Timeout | MyBatis timeout 적용 | □ |
| 외부 연계 | Connect/Read Timeout 적용 | □ |
| 장시간 거래 | 온라인 거래에서 분리 검토 | □ |
| Timeout 상태 | TIMEOUT, UNKNOWN 처리 기준 있음 | □ |
| 재처리 | 변경성 거래 재처리 기준 있음 | □ |
| Idempotency | 등록/수정/삭제/실행/발송 거래에 적용 검토 | □ |

```java
Timeout
@Transactional(timeout=...) 또는 정책 적용
□
@Transactional(timeout = 5)
public CmCampaignCreateResponse createCampaign(CmCampaignCreateRequest request) {
cmCampaignRule.validateCreate(request);
String campaignId = cmCampaignDao.insertCampaign(request);
return new CmCampaignCreateResponse(campaignId);
}
```

### I.14 오류처리 리뷰

| 점검 항목 | 확인 기준 | 판정 |
| --- | --- | --- |
| 업무 오류 | BusinessException 사용 | □ |
| 입력 오류 | ValidationException 사용 | □ |
| 권한 오류 | AuthorizationException 사용 | □ |
| DB 오류 | DB 예외를 표준 예외로 변환 | □ |
| 외부연계 오류 | IntegrationException 사용 | □ |
| Timeout 오류 | TcfTimeoutException 사용 | □ |
| 미처리 오류 | SystemException 또는 Global Handler에서 변환 | □ |
| 오류코드 | E-{DOMAIN}-{CATEGORY}-{NNNN} 사용 | □ |
| 사용자 메시지 | 기술정보 제거 | □ |
| 운영자 메시지 | GUID, ServiceId, SQL ID 등 추적 가능 | □ |

금지 예시는 다음이다.
catch (Exception e) {
    return "오류";
}

표준 예시는 다음이다.
if (customer == null) {
    throw new BusinessException(
            "E-SV-BIZ-0001",
            "조회 결과가 없습니다.",
            "SV 고객요약조회 결과 없음. customerNo=" + customerNo
    );
}

예외는 숨기지 않고 표준 방식으로 전환해야 하며, 업무 예외와 시스템 예외, 사용자 메시지와 운영 메시지를 분리해야 한다.

### I.15 로그 / 감사 리뷰

| 점검 항목 | 확인 기준 | 판정 |
| --- | --- | --- |
| GUID | 모든 거래 로그에 포함 | □ |
| TraceId | 내부 처리 로그에 포함 | □ |
| ServiceId | 거래로그에 포함 | □ |
| TransactionCode | 거래로그에 포함 | □ |
| MDC | 요청 시작 시 설정, 종료 시 제거 | □ |
| 거래 시작 로그 | PROCESSING 상태 기록 | □ |
| 거래 종료 로그 | SUCCESS, FAIL, TIMEOUT, UNKNOWN 갱신 | □ |
| 감사로그 | 고객정보, 다운로드, 권한위반, 관리자 기능 기록 | □ |
| 성능로그 | AP, DB, 외부연계 처리시간 기록 | □ |
| 마스킹 | 고객번호, 계좌번호, Token, 비밀번호 마스킹 | □ |

전처리·후처리 기준에서도 모든 온라인 거래는 GUID, TraceId 기반 추적, 인증·권한·마스킹, 거래로그, 성능로그, 감사로그, 모니터링 지표를 제공해야 한다.

### I.16 보안 코드 리뷰

| 점검 항목 | 확인 기준 | 판정 |
| --- | --- | --- |
| 인증 | 세션 또는 JWT 검증 대상 확인 | □ |
| 권한 | 메뉴권한, 기능권한, 데이터권한 검증 | □ |
| SQL Injection | ${} 사용자 입력 직접 사용 금지 | □ |
| XSS | 화면 출력 대상 Escape 또는 정제 | □ |
| 민감정보 | 로그/응답/예외 메시지에 평문 노출 금지 | □ |
| Token | Access/Refresh Token 로그 노출 금지 | □ |
| 파일 업로드 | 확장자, 크기, MIME 검증 | □ |
| 파일 다운로드 | 권한, 사유, 감사로그 확인 | □ |
| Secret | yml, Git, 소스에 비밀번호 저장 금지 | □ |
| 관리자 기능 | 변경 전/후 값과 사유 감사로그 기록 | □ |
= 사용자 입력은 신뢰하지 않는다.

= Header 사용자정보는 세션 기준으로 재검증한다.

| 보안 리뷰의 기본 원칙 | |
= 민감정보는 응답과 로그에서 마스킹한다.

= 권한 실패는 감사로그 대상이다.

### I.17 외부 연계 / 내부 서비스 호출 리뷰

| 점검 항목 | 확인 기준 | 판정 |
| --- | --- | --- |
| 호출 위치 | Service 또는 Integration Adapter에서 호출 | □ |
| Controller 직접 호출 | 금지 | □ |
| Timeout | Connect/Read Timeout 적용 | □ |
| Retry | 멱등성 있는 조회성 호출에만 제한 적용 | □ |
| Circuit Breaker | 필요 시 장애 전파 방지 적용 | □ |
| GUID 전달 | 원거래 GUID 유지 | □ |
| TraceId 전달 | 호출 구간 TraceId 또는 SpanId 유지 | □ |
| 오류 변환 | 외부 오류를 표준 IntegrationException으로 변환 | □ |
| 로그 | 대상시스템, URI, elapsedTimeMs, 결과 기록 | □ |
| 민감정보 | 요청/응답 로그 마스킹 | □ |

### I.18 Cache 리뷰

| 점검 항목 | 확인 기준 | 판정 |
| --- | --- | --- |
| Cache 대상 | 공통코드, ServiceId, 거래통제, 오류코드 등 기준정보 | □ |
| TTL | 업무 영향도에 맞게 설정 | □ |
| 무효화 | OM Cache Refresh 또는 TTL 만료 기준 존재 | □ |
| 변경성 데이터 | 고객잔액, 거래결과 등 실시간 데이터 Cache 금지 | □ |
| 장애 시 | Cache Miss 시 DB 조회 가능 여부 | □ |
| 감사 | 운영자 Cache Refresh 이력 기록 | □ |

### I.19 Batch 리뷰

| 점검 항목 | 확인 기준 | 판정 |
| --- | --- | --- |
| Job ID | 표준 Job ID 사용 | □ |
| Handler | BatchJobHandler 구현 | □ |
| Execution | 실행 시작/종료 이력 기록 | □ |
| Lock | 중복 실행 방지 | □ |
| Timeout | Job별 Timeout 설정 | □ |
| 부분 실패 | read/write/error count 기록 | □ |
| 재처리 | 실패 Job 재처리 기준 정의 | □ |
| 수동 실행 | OM 권한자만 실행 가능 | □ |
| 감사로그 | 수동 실행, 중지, 재처리 기록 | □ |

### I.20 파일 업로드/다운로드 리뷰

| 점검 항목 | 확인 기준 | 판정 |
| --- | --- | --- |
| 파일 원본 | DB/Session 저장 금지 | □ |
| 파일 메타 | 파일 메타 테이블 저장 | □ |
| 다운로드 | Streaming 방식 사용 | □ |
| 권한 | 다운로드 직전 서버에서 권한 확인 | □ |
| 사유 | 개인정보·대량파일 다운로드 사유 필수 | □ |
| 감사로그 | 사용자, 파일명, 사유, 시간 기록 | □ |
| 경로 조작 | ../ 등 Path Traversal 차단 | □ |
| 확장자 | 허용 확장자 검증 | □ |
| 크기 | 최대 파일 크기 제한 | □ |

### I.21 테스트 코드 리뷰

| 테스트 구분 | 확인 기준 | 판정 |
| --- | --- | --- |
| Rule 테스트 | 업무 조건, 경계값 검증 | □ |

Service 테스트
DAO/Adapter 호출 흐름 검증
□
Handler 테스트
ServiceId, Body 변환 검증
□
TCF 통합 테스트
/업무코드/online End-to-End 검증
□
Mapper 테스트
SQL ID, 파라미터, 결과 매핑 검증
□
오류 테스트
표준 오류코드, 사용자 메시지 검증
□
Timeout 테스트
Timeout 오류와 거래상태 검증
□
권한 테스트
권한 없음, 타 지점 접근 제한 검증
□
Batch 테스트
성공, 실패, 중복 실행 검증
□
파일 테스트
다운로드 Header, Stream, 감사로그 검증
□
테스트 클래스는 대상 클래스명 뒤에 Test를 붙이고, 테스트 메서드는 의미가 드러나도록 작성한다. NSIGHT 명명 기준에서도 SvCustomerRuleTest, SvCustomerServiceTest, SvCustomerMapperTest, SvCustomerSummaryHandlerTest와 같은 패턴을 제시한다.

### I.22 application.yml 리뷰

| 점검 항목 | 확인 기준 | 판정 |
| --- | --- | --- |
| Profile | local/dev/stg/prod 분리 | □ |
| Context Path | 업무코드와 일치 | □ |
| DB URL | 환경변수 또는 Secret 외부화 | □ |
| 비밀번호 | Git 저장 금지 | □ |
| HikariCP | Pool Size, Timeout, Lifetime 설정 | □ |
| MyBatis | Mapper 위치, Query Timeout 설정 | □ |
| Session | Spring Session JDBC, Timeout 설정 | □ |
| TCF | 거래통제, 권한, Timeout, 로그 설정 | □ |
| Actuator | Health/Metrics 제한 노출 | □ |
| Logging | GUID/MDC/마스킹 설정 | □ |

### I.23 정적분석 / 품질 Gate 리뷰

| 품질 항목 | 확인 기준 | 판정 |
| --- | --- | --- |
| Compile | 오류 없음 | □ |

Unit Test
실패 0건
□

| Integration Test | 핵심 시나리오 성공 |
| --- | --- |
| □ | Checkstyle |

중대 위반 없음
□

| PMD | 주요 결함 없음 |
| --- | --- |
| □ | SonarQube |
| Blocker/Critical 0건 | □ |
| Duplication | 중복 코드 기준 이내 |

□

| Coverage | 프로젝트 기준 충족 |
| --- | --- |
| □ | SQL 검증 |

주요 SQL 검증 완료
□
보안 점검
중대 취약점 없음
□

### I.24 코드 리뷰 금지 패턴

| 금지 패턴 | 문제 |
| --- | --- |
| Controller에서 Mapper 직접 호출 | 계층 책임 위반 |
| Handler에서 업무 로직 처리 | ServiceId 진입점 책임 초과 |
| Service에서 HTTP 응답 생성 | 기술 계층 의존성 증가 |
| Rule에서 DB 직접 조회 | 업무 판단과 데이터 접근 혼합 |
| Mapper에서 사용자 메시지 생성 | 오류처리 표준 위반 |
| Java 코드에 SQL 문자열 작성 | SQL 추적 불가 |
| SELECT * 사용 | 컬럼 변경 장애, 불필요 조회 |
| ${} 사용자 입력 사용 | SQL Injection 위험 |
| catch(Exception e) {} | 장애 은폐 |
| RuntimeException("오류") | 오류코드 추적 불가 |
| StackTrace 화면 반환 | 보안 취약 |
| Token, 비밀번호 로그 출력 | 보안 사고 위험 |
| 운영 yml에 Secret 저장 | Secret 유출 위험 |
| 테스트 없는 변경 | 회귀 장애 위험 |

### I.25 리뷰 코멘트 작성 기준

코드 리뷰 코멘트는 감정적 표현이 아니라 기준, 사유, 수정 방향을 함께 적는다.

| 나쁜 리뷰 코멘트 | 좋은 리뷰 코멘트 |
| --- | --- |
| “이건 별로입니다.” | “Handler에서 Mapper를 직접 호출하고 있어 계층 책임 기준에 맞지 않습니다. Facade 또는 Service를 통해 호출하도록 수정해 주세요.” |
| “이름이 이상합니다.” | “ServiceId가 selectCustomer로 되어 있어 업무코드와 업무대상이 식별되지 않습니다. SV.Customer.selectSummary 형식으로 변경해 주세요.” |
| “SQL 다시 짜세요.” | “목록조회 SQL에 Paging이 없어 대량조회 위험이 있습니다. OFFSET/FETCH와 pageSize 최대값 제한을 추가해 주세요.” |
| “예외처리 틀렸습니다.” | “RuntimeException 대신 표준 오류코드가 포함된 BusinessException 또는 SystemException으로 변환해 주세요.” |
| “로그가 부족합니다.” | “장애 추적을 위해 GUID, TraceId, ServiceId, TransactionCode가 MDC와 거래로그에 포함되어야 합니다.” |

### I.26 코드 리뷰 승인 기준

| 구분 | 승인 가능 | 승인 불가 |
| --- | --- | --- |
| 구조 | 계층 책임 준수 | Controller/Handler에서 DB 직접 접근 |
| 명명 | 업무코드, ServiceId, 거래코드 표준 준수 | 임의 명명, Prefix 불일치 |
| 전문 | 표준 요청/응답 구조 준수 | 업무별 임의 응답 구조 |
| SQL | Mapper XML, Paging, Timeout 적용 | Java SQL, SELECT *, ${} 사용 |
| 오류 | 표준 예외·오류코드 사용 | RuntimeException, 문자열 오류 |
| 로그 | GUID 기준 추적 가능 | 거래로그 누락 |
| 보안 | 권한·마스킹·Secret 기준 준수 | 민감정보 노출 |
| 테스트 | 핵심 테스트 존재 | 테스트 없음 |
| 운영 | OM 등록 대상 정리 | ServiceId/거래코드/오류코드 미정의 |

### I.27 코드 리뷰 결과 기록 양식

| 항목 | 내용 |
| --- | --- |
| 리뷰 ID | CR-YYYYMMDD-0001 |
| 대상 Branch | feature/sv-customer-summary |
| 대상 ServiceId | SV.Customer.selectSummary |
| 거래코드 | SV-INQ-0001 |
| 리뷰 유형 | 동료 / 아키텍처 / 품질 / 보안 / 운영 |
| 리뷰 결과 | 승인 / 조건부 승인 / 보완 요청 / 반려 |
| 주요 지적사항 | 계층구조, SQL, 오류코드, 테스트 등 |
| 보완 담당자 | 개발자명 |
| 보완 완료일 | YYYY-MM-DD |
| 재리뷰 여부 | Y / N |
| 최종 승인자 | 리뷰어명 |

### I.28 최종 체크리스트

| 영역 | 최종 질문 | 결과 |
| --- | --- | --- |
| 구조 | 이 코드는 NSIGHT 표준 계층구조를 따르는가? | □ |
| 식별 | ServiceId, 거래코드, 오류코드가 표준인가? | □ |
| 전문 | 표준 전문으로 요청·응답하는가? | □ |
| SQL | Mapper XML, SQL ID, Timeout, Paging이 적절한가? | □ |
| 오류 | 예외가 표준 오류코드로 변환되는가? | □ |
| 로그 | GUID 기준으로 장애 추적이 가능한가? | □ |
| 보안 | 권한, 마스킹, 입력검증이 되어 있는가? | □ |
| 테스트 | 변경 내용이 테스트로 검증되는가? | □ |
| 운영 | 운영자가 OM, 로그, 모니터링으로 확인 가능한가? | □ |

### I.29 최종 정리

코드 리뷰 체크리스트의 핵심은 다음이다.
코드 리뷰
= 개발자 취향 검토가 아니라
= NSIGHT 운영 표준 준수 여부 검토

운영 관점에서는 다음 기준이 가장 중요하다.
로그만 보고
어느 업무인지 알 수 있는가?

ServiceId만 보고
어느 Handler가 실행되는지 알 수 있는가?

오류코드만 보고
무엇을 조치해야 하는지 알 수 있는가?

SQL ID만 보고
어느 Mapper와 거래에서 실행됐는지 알 수 있는가?

따라서 NSIGHT TCF Framework에서 코드 리뷰는 다음 한 문장으로 정의할 수 있다.
코드 리뷰는 기능 동작 여부를 확인하는 절차가 아니라, 개발 결과가 표준 구조·보안·로그·SQL·테스트·운영 기준을 만족하는지 검증하는 아키텍처 품질 Gate이다.

## 소스·관련 문서

| 참고 |
|------|

> znsight-guide-word: `통합 (79).docx`

| [61-코드-리뷰-기준.md](../znsight-man/61-코드-리뷰-기준.md) |

## 코드베이스 정정 (develop 기준)

| 항목 | 값 |
|------|-----|
| 업무 WAR | ic, pc, ms, sv, pd, eb, ep, ss, mg + tcf-om |
| ztomcat deploy | `ztomcat/deploy-wars.sh` 13 WAR |
| buildZtomcatWars | 15 WAR |
| bootRun | gateway 8100, uj 8102, jwt 8110, ui 8099 |


---

[← 78. 테스트 코드 샘플](./78-테스트-코드-샘플.md) · [README](./README.md)