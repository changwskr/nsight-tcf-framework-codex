# 19. Validation 작성 기준

> **NSIGHT TCF 개발 Manual** · 원본: [`znsight-guide-word`](../znsight-guide-word/) · 갱신: 2026-07-05

## 19. Validation 작성 기준

### 19.1 도입 전 안내말

NSIGHT TCF Framework에서 Validation은 단순히 화면 입력값이 비어 있는지 확인하는 기능이 아니다.Validation은 온라인 거래가 업무 로직에 진입하기 전에 표준 전문, Header, 세션, 인증, 권한, 거래통제, 입력값, 중복요청, 업무 규칙, DB 정합성을 단계적으로 검증하는 공통 품질 통제 구조이다.
NSIGHT 전처리·후처리 요구사항에서도 전처리 모듈은 모든 온라인 거래가 동일한 방식으로 수신되고, 검증되고, 추적되고, 처리되고, 응답되도록 보장하는 공통 실행 메커니즘으로 정의되어 있다. 또한 전처리 흐름에는 표준 전문 수신, Header 검증, GUID/Trace 생성, 세션 검증, 인증·권한 확인, 입력값 검증, 중복요청 확인, 거래 시작 로그, PROCESSING 상태 저장이 포함된다.
따라서 NSIGHT 개발자는 Validation을 다음처럼 이해해야 한다.
Validation
= 사용자가 보낸 값만 검증하는 기능이 아니라
= 거래를 실행해도 되는지 판단하는 단계별 방어 구조

### 19.2 Validation 작성 결론

NSIGHT Validation은 다음 기준으로 작성한다.
| 구분 | 설계 기준 |
| --- | --- |
| 검증 구조 | 공통 검증 + 업무 검증 + DB 정합성 검증 분리 |
| 공통 검증 위치 | STF.preProcess() |
| DTO 검증 위치 | Handler 진입 전 또는 Handler 내부 |
| 업무 검증 위치 | Rule 계층 |
| DB 정합성 검증 위치 | Service / DAO 계층 |
| 권한 검증 위치 | STF 또는 Security PreProcessor |
| 검증 실패 처리 | BusinessException 또는 ValidationException 발생 |
| 응답 방식 | 표준 오류코드 + 표준 응답 전문 |
| 사용자 메시지 | 내부 기술정보를 숨기고 입력값 확인 메시지 제공 |
| 운영 로그 | GUID, TraceId, ServiceId, transactionCode 기준 기록 |
| 개발 원칙 | Controller, Handler, DTO에 업무 판단 로직을 넣지 않음 |

핵심 원칙은 다음이다.
형식 검증은 DTO에서 한다.
업무 규칙 검증은 Rule에서 한다.
DB 존재 여부 검증은 Service/DAO에서 한다.
권한과 거래 가능 여부는 STF 전처리에서 한다.

### 19.3 Validation 단계 구조

NSIGHT Validation은 다음 순서로 수행한다.
```text
[요청 수신]
        ↓
```

## 1. 표준 전문 구조 검증

```text
        ↓
```

## 2. Header 필수값 검증

```text
        ↓
```

## 3. GUID / TraceId 검증

```text
        ↓
```

## 4. ServiceId / 거래코드 검증

```text
        ↓
```

## 5. 세션 / 인증 검증

```text
        ↓
```

## 6. 권한 / 데이터권한 검증

```text
        ↓
```

## 7. 거래통제 / 거래 가능 여부 검증

```text
        ↓
```

## 8. DTO 입력값 검증

```text
        ↓
```

## 9. 업무 Rule 검증

```text
        ↓
```

## 10. DB 정합성 검증

```text
        ↓
```

## 11. 중복요청 / 멱등성 검증

```text
        ↓
```

[Handler → Facade → Service → Rule → DAO 실행]

전처리 요구사항에서도 Header 검증, 세션 검증, 인증 검증, 권한 검증, 입력값 검증, 거래 가능 여부 검증, 중복요청 방지, Timeout 정책 설정, 감사 대상 식별, 거래 상태 저장, MDC 설정을 필수 또는 중요 요구사항으로 정의한다.

### 19.4 Validation 유형 분류

| 검증 유형 | 검증 대상 | 검증 위치 |
| --- | --- | --- |
| 예시 | 전문 구조 검증 | header, body 존재 여부 |
| STF | 표준 전문 구조 오류 | Header 검증 |
| serviceId, transactionCode, businessCode | STF | ServiceId 누락 |
| 세션 검증 | 로그인 세션, 사용자, 지점 | STF |
| 세션 만료 | 인증 검증 | SSO, JWT, Token |
| STF / Security | 미인증 사용자 | 권한 검증 |
| 메뉴, 기능, 데이터권한 | STF / Security | 타 지점 고객 조회 |
| DTO 형식 검증 | 필수값, 길이, 형식 | DTO Validator |
| 고객번호 누락 | 업무 규칙 검증 | 업무 상태, 기간, 정책 |
| Rule | 종료일자가 시작일자보다 빠름 | DB 정합성 검증 |
| 존재 여부, 중복 여부 | Service / DAO | 사용자 없음 |
| 코드값 검증 | 공통코드, 상태코드 | Rule / CodeService |
| 미등록 코드 | 범위 검증 | 조회기간, 금액, 건수 |
| Rule | 조회기간 3개월 초과 | 보안 검증 |
| 금지문자, 스크립트, SQL Injection | Common Validator | <script> 입력 |
| 중복 검증 | GUID, RequestId, IdempotencyKey | IdempotencyProcessor |
| 중복 등록 요청 | 파일 검증 | 확장자, 크기, 권한, 건수 |

File Rule
다운로드 건수 초과
입력값 검증 요구사항은 필수값, 타입, 길이, 코드값, 범위, 금지문자, 페이지, 다운로드 검증을 포함한다.

### 19.5 계층별 Validation 책임

Validation은 한 곳에 몰아서 작성하지 않는다.검증 성격에 따라 계층별 책임을 분리한다.
| 계층 | Validation 책임 | 작성 예시 |
| --- | --- | --- |
| Filter | GUID, TraceId, MDC 초기화 | GUID 형식 검증 |
| Interceptor / STF | Header, 세션, 인증, 권한, 거래통제 | ServiceId 등록 여부 |
| Handler | Request Body DTO 변환, 기본 DTO 검증 호출 | @Valid 적용 |
| Facade | 유스케이스 실행 전 최종 흐름 검증 | 복합 서비스 호출 순서 |
| Service | 업무 처리 흐름상 정합성 검증 | 중복 저장 여부 |
| Rule | 업무 규칙 검증 | 조회기간, 업무 상태, 금액 범위 |
| DAO | DB 조회 결과 존재 여부 보조 | 대상 데이터 미존재 |
| Mapper | 검증 로직 작성 금지 | SQL 실행만 수행 |

전체 아키텍처 기준에서도 TCF는 STF.preProcess()에서 Header, 세션, 인증, 권한, 거래통제, Timeout 정책 등을 처리하고, 업무 계층은 Handler → Facade → Service → Rule → DAO/Mapper 흐름으로 처리된다.

### 19.6 공통 Validation과 업무 Validation 분리

Validation은 크게 두 가지로 나눈다.
공통 Validation
= 모든 거래에 동일하게 적용되는 검증

업무 Validation
= 특정 업무 서비스에서만 필요한 검증

| 구분 | 공통 Validation | 업무 Validation |
| --- | --- | --- |
| 적용 대상 | 모든 온라인 거래 | 특정 ServiceId |
| 위치 | STF, Common Validator | Rule, Service |
| 예시 | Header 필수값, 세션, 권한, 거래통제 | 고객번호 존재 여부, 캠페인 기간 |
| 오류코드 | E-TCF-*, E-COM-* | E-SV-*, E-CM-*, E-OM-* |
| 관리 주체 | 아키텍처팀 / 공통팀 | 업무 개발팀 |
| 변경 영향 | 전체 업무 영향 | 해당 업무 영향 |

예시는 다음과 같다.
검증 항목
공통/업무
| 설명 | serviceId 필수 | 공통 | 모든 거래 공통 |
| --- | --- | --- | --- |
| transactionCode 필수 | 공통 | 모든 거래 공통 | 세션 사용자 일치 |
| 공통 | Header 사용자와 세션 사용자 비교 | 고객번호 필수 | 업무 |
| 고객조회 거래에만 필요 | 조회기간 3개월 이하 | 업무 | 특정 조회 거래 정책 |
| 다운로드 5,000건 이하 | 업무 | 다운로드 거래 정책 | 캠페인 시작일 ≤ 종료일 |

업무
캠페인 업무 규칙

### 19.7 Request DTO Validation 기준

Request DTO에는 형식 검증만 작성한다.
형식 검증은 다음 항목을 의미한다.
| 검증 항목 | 예시 | 필수값 |
| --- | --- | --- |
| 고객번호는 필수 | 길이 | 고객번호는 10자리 이하 |
| 형식 | 날짜는 yyyyMMdd | 숫자 범위 |
| pageNo는 1 이상 | 문자열 패턴 | 코드값은 영문 대문자와 숫자만 허용 |

최대 건수
pageSize는 500 이하
예시는 다음과 같다.
@Getter
@Setter
@NoArgsConstructor
```java
public class SvCustomerSummaryRequest {
    @NotBlank(message = "고객번호는 필수입니다.")
    @Size(max = 20, message = "고객번호는 20자를 초과할 수 없습니다.")
    private String customerNo;
    @Pattern(
        regexp = "^[0-9]{8}$",
        message = "기준일자는 yyyyMMdd 형식이어야 합니다."
    )
    private String baseDate;
}
```

목록 조회 Request DTO 예시는 다음과 같다.
@Getter
@Setter
@NoArgsConstructor
```java
public class SvCustomerSearchRequest {
    @Size(max = 50, message = "고객명은 50자를 초과할 수 없습니다.")
    private String customerName;
    @Pattern(
        regexp = "^[A-Z0-9_]*$",
        message = "고객등급 코드 형식이 올바르지 않습니다."
    )
    private String customerGradeCode;
    @Min(value = 1, message = "pageNo는 1 이상이어야 합니다.")
    private int pageNo = 1;
    @Min(value = 1, message = "pageSize는 1 이상이어야 합니다.")
    @Max(value = 500, message = "pageSize는 500을 초과할 수 없습니다.")
    private int pageSize = 100;
}
```

### 19.8 DTO Validation에서 하지 말아야 할 것

DTO Validation에는 업무 판단 로직을 넣지 않는다.
| 금지 사항 | 이유 |
| --- | --- |
| 권장 위치 | DB 조회 |
| DTO는 데이터 전달 객체 | Service / DAO |
| 권한 확인 | 세션·권한 검증은 공통 보안 책임 |
| STF / Security | 업무 상태 판단 |
| 업무 규칙은 Rule 책임 | Rule |
| 공통코드 DB 조회 | 외부 의존성 발생 |
| Rule / CodeService | 날짜 간 업무 규칙 판단 |
| 복합 검증은 Annotation에 부적합 | Rule |
| 다운로드 가능 여부 판단 | 권한·건수·감사 연계 필요 |
| File Rule | 고객정보 접근권한 판단 |
| 데이터권한 검증 필요 | Security / Rule |

나쁜 예시는 다음과 같다.
```java
public class SvCustomerSummaryRequest {
    private String customerNo;
    public boolean isValidCustomer() {
        // DTO에서 DB 조회 금지
        return customerRepository.exists(customerNo);
    }
}
```

권장 방식은 다음과 같다.
```java
public class SvCustomerRule {
    public void validateCustomerExists(String customerNo) {
        if (!customerDao.existsCustomer(customerNo)) {
            throw new BusinessException(
                "E-SV-VAL-0001",
                "고객정보가 존재하지 않습니다."
            );
        }
    }
}
```

### 19.9 Handler에서 Validation 호출 기준

Handler는 다음 책임만 가진다.
## 1. StandardRequest.body를 Request DTO로 변환

## 2. DTO 형식 검증 수행

## 3. 검증된 Request DTO를 Facade로 전달

예시는 다음과 같다.
```java
@Component
@RequiredArgsConstructor
public class SvCustomerSummaryHandler implements TransactionHandler {
    private final SvCustomerFacade svCustomerFacade;
    private final Validator validator;
    private final ObjectMapper objectMapper;
    @Override
    public String serviceId() {
        return "SV.Customer.selectSummary";
    }
    @Override
    public Object handle(StandardRequest<?> request, TransactionContext context) {
        SvCustomerSummaryRequest body =
                objectMapper.convertValue(
                        request.getBody(),
                        SvCustomerSummaryRequest.class
                );
        validate(body);
        return svCustomerFacade.selectCustomerSummary(body, context);
    }
    private void validate(Object target) {
        Set<ConstraintViolation<Object>> violations = validator.validate(target);
        if (!violations.isEmpty()) {
            throw new ValidationException("입력값을 확인해 주십시오.", violations);
        }
    }
}
```

다만 공통적으로 처리할 수 있다면 Handler마다 직접 작성하지 않고 RequestValidationProcessor 또는 TcfRequestValidator로 공통화한다.
Handler마다 validator.validate(...) 반복 작성 금지
공통 Validator Utility 또는 STF RequestValidationProcessor 사용 권장

전처리·후처리 프로그램 설계에서도 Validator는 DTO 입력값 검증에 사용하고, HandlerInterceptor는 Header·세션·권한 검증, ControllerAdvice는 공통 예외 변환과 표준 오류 응답 생성에 사용하도록 정리한다.

### 19.10 Rule 계층 Validation 기준

업무 규칙 검증은 Rule 계층에서 수행한다.
Rule Validation 예시는 다음과 같다.
```java
@Component
@RequiredArgsConstructor
public class SvCustomerRule {
    private final SvCustomerDao svCustomerDao;
    public void validateInquiryPeriod(String fromDate, String toDate) {
        if (fromDate == null || toDate == null) {
            return;
        }
        LocalDate from = LocalDate.parse(fromDate, DateTimeFormatter.BASIC_ISO_DATE);
        LocalDate to = LocalDate.parse(toDate, DateTimeFormatter.BASIC_ISO_DATE);
        if (from.isAfter(to)) {
            throw new BusinessException(
                    "E-SV-VAL-0002",
                    "시작일자는 종료일자보다 클 수 없습니다."
            );
        }
        if (ChronoUnit.MONTHS.between(from, to) > 3) {
            throw new BusinessException(
                    "E-SV-VAL-0003",
                    "조회기간은 3개월을 초과할 수 없습니다."
            );
        }
    }
    public void validateCustomerExists(String customerNo) {
        if (!svCustomerDao.existsCustomer(customerNo)) {
            throw new BusinessException(
                    "E-SV-VAL-0004",
                    "고객정보가 존재하지 않습니다."
            );
        }
    }
}
```

Rule Validation 작성 기준은 다음과 같다.
| 기준 | 설명 |
| --- | --- |
| 업무 의미가 있는 검증만 작성 | 기간, 상태, 한도, 가능 여부 |
| DB 조회가 필요한 검증 가능 | 존재 여부, 중복 여부 |
| 여러 필드 간 관계 검증 | 시작일/종료일, 금액 범위 |
| 공통코드 검증 가능 | CodeService 또는 Cache 사용 |
| 권한성 검증은 공통 보안과 중복 주의 | 타 지점 접근권한 등 |
| 오류코드는 업무 도메인 기준 사용 | E-SV-VAL-*, E-CM-VAL-* |

### 19.11 Service 계층 Validation 기준

Service 계층에서는 업무 흐름과 저장·조회 정합성을 검증한다.
예시는 다음과 같다.
```java
@Service
@RequiredArgsConstructor
public class OmUserService {
    private final OmUserDao omUserDao;
    private final OmUserRule omUserRule;
    public OmUserDetailResponse updateUser(
            OmUserUpdateRequest request,
            TransactionContext context
    ) {
        omUserRule.validateUserIdFormat(request.getUserId());
        if (!omUserDao.existsUser(request.getUserId())) {
            throw new BusinessException(
                    "E-OM-VAL-0001",
                    "수정 대상 사용자가 존재하지 않습니다."
            );
        }
        if (omUserDao.isDuplicatedEmployeeNo(request.getEmployeeNo(), request.getUserId())) {
            throw new BusinessException(
                    "E-OM-VAL-0002",
                    "동일한 직원번호가 이미 등록되어 있습니다."
            );
        }
        // update 처리
        omUserDao.updateUser(request, context.getUserId());
        return omUserDao.selectUserDetail(request.getUserId());
    }
}
```

Service Validation 작성 기준은 다음과 같다.
| 검증 항목 | 설명 | 등록 전 중복 여부 |
| --- | --- | --- |
| 동일 사용자, 동일 코드 중복 | 수정 전 존재 여부 | 수정 대상 데이터 존재 |
| 삭제 전 참조 여부 | 하위 데이터 존재 여부 | 상태 전이 가능 여부 |
| 승인 전 → 승인 완료 가능 여부 | 외부 연계 가능 여부 | 연계 대상 상태 확인 |

저장 가능 권한
업무 권한과 데이터권한 확인

### 19.12 공통 Header Validation 기준

Header Validation은 모든 거래에 공통 적용한다.
| Header 항목 | 검증 기준 | 오류 시 처리 |
| --- | --- | --- |
| guid | 없으면 생성, 있으면 형식 검증 | 형식 오류 |
| traceId | 없으면 생성 | 신규 생성 |
| transactionId | 거래 식별 가능 여부 | 필수값 오류 |
| serviceId | 등록된 ServiceId인지 확인 | 미등록 서비스 오류 |
| transactionCode | ServiceId와 매핑 확인 | 거래코드 오류 |
| businessCode | URL 업무코드와 일치 확인 | 업무코드 오류 |
| channelId | 허용 채널 여부 | 허용되지 않은 채널 |
| userId | 세션 사용자와 일치 확인 | 인증 오류 |
| branchId | 사용자 소속 지점과 일치 확인 | 권한 오류 |
| clientIp | 접근 허용 IP 여부 | 접근 차단 |

Header 검증 요구사항에서도 serviceId는 등록된 서비스 ID 여부, channelId는 허용 채널 여부, userId는 세션 사용자와 일치 여부, branchId는 사용자 소속 지점과 일치 여부를 확인해야 한다고 정의한다.
예시는 다음과 같다.
```java
@Component
@RequiredArgsConstructor
public class StandardHeaderValidator {
    private final ServiceCatalogService serviceCatalogService;
    private final ChannelPolicyService channelPolicyService;
    public void validate(StandardHeader header, TransactionContext context) {
        require(header.getServiceId(), "E-TCF-HDR-0001", "ServiceId는 필수입니다.");
        require(header.getTransactionCode(), "E-TCF-HDR-0002", "거래코드는 필수입니다.");
        require(header.getBusinessCode(), "E-TCF-HDR-0003", "업무코드는 필수입니다.");
        if (!header.getServiceId().startsWith(header.getBusinessCode() + ".")) {
            throw new BusinessException(
                    "E-TCF-HDR-0004",
                    "업무코드와 ServiceId가 일치하지 않습니다."
            );
        }
        if (!header.getTransactionCode().startsWith(header.getBusinessCode() + "-")) {
            throw new BusinessException(
                    "E-TCF-HDR-0005",
                    "업무코드와 거래코드가 일치하지 않습니다."
            );
        }
        serviceCatalogService.validateActiveService(
                header.getServiceId(),
                header.getTransactionCode()
        );
        channelPolicyService.validateChannel(header.getChannelId());
    }
    private void require(String value, String errorCode, String message) {
        if (value == null || value.isBlank()) {
            throw new BusinessException(errorCode, message);
        }
    }
}
```

### 19.13 공통코드 Validation 기준

공통코드는 화면, 전문 Header, 입력값 검증, 업무 분기, 권한·감사·로그 분류에 사용되는 기준정보이다. 공통코드 관리 설계에서도 공통코드는 단순 콤보박스 값이 아니라 화면, 전문, 업무, 운영, 감사, 시스템 기준정보로 사용된다고 정의한다.
공통코드 검증 기준은 다음과 같다.
| 기준 | 설명 |
| --- | --- |
| 코드그룹 존재 여부 확인 | CUSTOMER_GRADE, CHANNEL_ID 등 |
| 코드값 사용 여부 확인 | USE_YN = Y |
| 코드 유효기간 확인 | 시작일/종료일 |
| 업무별 허용 코드 확인 | 특정 업무에서만 허용되는 코드 |
| 코드 캐시 사용 | 반복 DB 조회 방지 |
| 미등록 코드 차단 | 기본 차단 |

예시는 다음과 같다.
```java
@Component
@RequiredArgsConstructor
public class CommonCodeValidator {
    private final CommonCodeService commonCodeService;
    public void validateCode(String groupCode, String codeValue) {
        if (codeValue == null || codeValue.isBlank()) {
            return;
        }
        if (!commonCodeService.existsActiveCode(groupCode, codeValue)) {
            throw new BusinessException(
                    "E-COM-VAL-0001",
                    "허용되지 않은 코드값입니다."
            );
        }
    }
}
```

업무 Rule에서 사용하는 예시는 다음과 같다.
```java
public void validateCustomerGrade(String customerGradeCode) {
    commonCodeValidator.validateCode("CUSTOMER_GRADE", customerGradeCode);
}
```

### 19.14 날짜 Validation 기준

날짜 검증은 형식 검증과 업무 기간 검증을 분리한다.
| 검증 항목 | 위치 | 예시 |
| --- | --- | --- |
| 날짜 형식 | DTO | yyyyMMdd |
| 시작일/종료일 관계 | Rule | 시작일 ≤ 종료일 |
| 조회기간 제한 | Rule | 3개월 이하 |
| 미래일자 허용 여부 | Rule | 기준일자는 오늘 이하 |
| 영업일 여부 | Rule / CalendarService | 영업일만 허용 |

DTO 형식 검증 예시는 다음과 같다.
@Pattern(
    regexp = "^[0-9]{8}$",
    message = "조회시작일자는 yyyyMMdd 형식이어야 합니다."
)
private String fromDate;

Rule 업무 검증 예시는 다음과 같다.
```java
public void validateDateRange(String fromDate, String toDate) {
    LocalDate from = LocalDate.parse(fromDate, DateTimeFormatter.BASIC_ISO_DATE);
    LocalDate to = LocalDate.parse(toDate, DateTimeFormatter.BASIC_ISO_DATE);
    if (from.isAfter(to)) {
        throw new BusinessException(
                "E-COM-VAL-0002",
                "조회시작일자는 조회종료일자보다 클 수 없습니다."
        );
    }
    if (ChronoUnit.MONTHS.between(from, to) > 3) {
        throw new BusinessException(
                "E-COM-VAL-0003",
                "조회기간은 3개월을 초과할 수 없습니다."
        );
    }
}
```

### 19.15 페이지 Validation 기준

목록 조회는 반드시 페이지 검증을 수행한다.
| 항목 | 기준 |
| --- | --- |
| pageNo | 1 이상 |
| pageSize | 1 이상 |
| 일반 조회 최대 pageSize | 500 이하 |
| 기본 pageNo | 1 |
| 기본 pageSize | 100 |
| 정렬 컬럼 | Whitelist 방식 |
| 정렬 방향 | ASC, DESC만 허용 |

페이징 처리 설계에서도 STF 또는 업무 처리 흐름에서 pageNo, pageSize, maxPageSize, sort 컬럼 whitelist 검증이 필요하다고 정리되어 있다.
예시는 다음과 같다.
@Getter
@Setter
@NoArgsConstructor
```java
public class PageRequest {
    @Min(value = 1, message = "pageNo는 1 이상이어야 합니다.")
    private int pageNo = 1;
    @Min(value = 1, message = "pageSize는 1 이상이어야 합니다.")
    @Max(value = 500, message = "pageSize는 500을 초과할 수 없습니다.")
    private int pageSize = 100;
    private String sortColumn;
    private String sortDirection;
}
```

Sort 검증 예시는 다음과 같다.
```java
public void validateSortColumn(String sortColumn, Set<String> allowedColumns) {
    if (sortColumn == null || sortColumn.isBlank()) {
        return;
    }
    if (!allowedColumns.contains(sortColumn)) {
        throw new BusinessException(
                "E-COM-VAL-0004",
                "허용되지 않은 정렬 조건입니다."
        );
    }
}
```

### 19.16 금지문자 Validation 기준

입력값에는 SQL Injection, Script 입력, 제어문자 등을 차단하는 검증이 필요하다.
| 검증 항목 | 예시 | Script 입력 |
| --- | --- | --- |
| <script>, javascript: | SQL Injection | ' OR 1=1, --, /* */ |
| 제어문자 | 개행, 탭, NULL 문자 | 경로 조작 |
| ../, ..\\ | 파일명 위험 문자 | /, \, :, *, ?, ", <, >, ` |

예시는 다음과 같다.
```java
@Component
public class DangerousInputValidator {
    private static final Pattern DANGEROUS_PATTERN = Pattern.compile(
            "(?i)(<script|javascript:|\\bor\\b\\s+1\\s*=\\s*1|--|/\\*|\\*/|\\.\\./|\\.\\.\\\\)"
    );
    public void validateSafeText(String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        if (DANGEROUS_PATTERN.matcher(value).find()) {
            throw new BusinessException(
                    "E-COM-VAL-0005",
                    "허용되지 않은 문자가 포함되어 있습니다."
            );
        }
    }
}
```

단, 모든 입력값에 무조건 동일한 정규식을 적용하면 정상 업무 데이터까지 차단할 수 있으므로, 항목 성격별로 허용 문자 기준을 다르게 둔다.

### 19.17 파일 업다운로드 Validation 기준

파일 업다운로드는 일반 입력값보다 강한 검증이 필요하다.
| 검증 항목 | 기준 |
| --- | --- |
| 파일명 | 경로 조작 문자 금지 |
| 확장자 | 허용 확장자만 가능 |
| MIME Type | 확장자와 MIME Type 정합성 확인 |
| 파일 크기 | 업무별 최대 크기 제한 |
| 다운로드 건수 | 대량 다운로드 제한 |
| 다운로드 권한 | 사용자 권한 확인 |
| 다운로드 사유 | 감사 대상이면 필수 |
| 보관기간 | 파일 보관 정책 확인 |
| 바이러스 검사 | 운영 정책에 따라 연계 |

예시는 다음과 같다.
```java
public void validateDownload(
        UdFileDownloadRequest request,
        TransactionContext context
) {
    if (request.getFileId() == null || request.getFileId().isBlank()) {
        throw new BusinessException(
                "E-UD-VAL-0001",
                "파일 ID는 필수입니다."
        );
    }
    if (request.getDownloadReason() == null || request.getDownloadReason().isBlank()) {
        throw new BusinessException(
                "E-UD-VAL-0002",
                "다운로드 사유는 필수입니다."
        );
    }
    if (!fileAuthService.canDownload(context.getUserId(), request.getFileId())) {
        throw new BusinessException(
                "E-UD-AUTHZ-0001",
                "파일 다운로드 권한이 없습니다."
        );
    }
}
```

전처리 요구사항에서도 다운로드 검증은 다운로드 가능 건수와 권한 확인을 포함하며, 고객정보 다운로드는 감사 로그 대상이다.

### 19.18 중복요청 Validation 기준

중복요청 검증은 조회 거래와 변경 거래를 다르게 적용한다.
| 거래 유형 | 중복요청 기준 | 조회 |
| --- | --- | --- |
| 원칙적으로 허용 가능, 성능 보호 필요 시 제한 | 등록 | 중복 차단 필수 |
| 수정 | 중복 차단 권장 | 삭제 |
| 중복 차단 권장 | 이벤트 발행 | 중복 차단 필수 |
| 메시지 발송 | 중복 차단 필수 | 파일 업로드 |
| 중복 차단 권장 | Timeout 후 재요청 | 기존 거래 상태 조회 후 판단 |

중복요청 방지 요구사항도 GUID, RequestId, IdempotencyKey 기준으로 중복 여부를 판단하고, 등록·수정·삭제·이벤트 발행 거래는 반드시 중복 처리를 차단해야 한다고 정의한다.
예시는 다음과 같다.
```java
public void validateDuplicateRequest(StandardHeader header) {
    String idempotencyKey = header.getIdempotencyKey();
    if (idempotencyKey == null || idempotencyKey.isBlank()) {
        return;
    }
    TransactionStatus status =
            transactionStatusService.findByIdempotencyKey(idempotencyKey);
    if (status == TransactionStatus.PROCESSING) {
        throw new BusinessException(
                "E-TCF-IDEMP-0001",
                "동일한 요청이 처리 중입니다."
        );
    }
    if (status == TransactionStatus.SUCCESS) {
        throw new BusinessException(
                "E-TCF-IDEMP-0002",
                "이미 처리된 요청입니다."
        );
    }
}
```

### 19.19 Validation 오류코드 기준

Validation 오류코드는 공통 오류와 업무 오류를 분리한다.
오류 구분
| 오류코드 형식 | 예시 |
| --- | --- |
| Header 검증 오류 | E-TCF-HDR-{NNNN} |
| E-TCF-HDR-0001 | 공통 입력값 오류 |
| E-COM-VAL-{NNNN} | E-COM-VAL-0001 |
| 업무 입력값 오류 | E-{업무코드}-VAL-{NNNN} |
| E-SV-VAL-0001 | 권한 오류 |
| E-TCF-AUTHZ-{NNNN} | E-TCF-AUTHZ-0001 |
| 거래통제 오류 | E-TCF-CTL-{NNNN} |
| E-TCF-CTL-0001 | 중복요청 오류 |
| E-TCF-IDEMP-{NNNN} | E-TCF-IDEMP-0001 |
| 파일 검증 오류 | E-UD-VAL-{NNNN} |

E-UD-VAL-0001
오류 유형별 사용자 메시지는 내부 기술정보를 노출하지 않아야 한다. 전처리·후처리 요구사항의 오류처리 기준에서도 입력 오류는 “입력값을 확인해 주십시오.”로 사용자에게 안내하고, 시스템 오류나 DB 오류는 내부 상세정보를 사용자에게 노출하지 않는 구조로 정리되어 있다.

### 19.20 Validation 예외 처리 기준

Validation 실패 시에는 표준 예외로 변환한다.
Validation 실패
```text
   ↓
BusinessException 또는 ValidationException 발생
   ↓
GlobalExceptionHandler
   ↓

```

표준 오류코드 매핑
```text
   ↓
StandardResponse.error 조립
   ↓
거래로그 FAIL 저장
   ↓

```

감사 대상이면 감사로그 기록

예시는 다음과 같다.
@Getter
```java
public class ValidationException extends BusinessException {
    private final List<FieldErrorDetail> fieldErrors;
    public ValidationException(
            String errorCode,
            String message,
            List<FieldErrorDetail> fieldErrors
    ) {
        super(errorCode, message);
        this.fieldErrors = fieldErrors;
    }
}
```

필드 오류 DTO 예시는 다음과 같다.
@Getter
@Builder
@AllArgsConstructor
```java
public class FieldErrorDetail {
    private String field;
    private String reason;
    private String rejectedValue;
}
```

응답 전문 예시는 다음과 같다.
```json
{
  "header": {
    "guid": "G202607040001",
    "traceId": "T202607040001",
    "serviceId": "SV.Customer.selectSummary",
    "transactionCode": "SV-INQ-0001",
    "responseDateTime": "2026-07-04T18:30:00"
  },
  "result": {
    "resultCode": "FAIL",
    "message": "입력값을 확인해 주십시오."
  },
  "error": {
    "errorCode": "E-SV-VAL-0001",
    "errorMessage": "고객번호는 필수입니다."
  },
  "body": null
}
```

### 19.21 Bean Validation Annotation 사용 기준

Spring Boot 3 기준으로 jakarta.validation을 사용한다.
| Annotation | 용도 |
| --- | --- |
| 예시 | @NotNull |
| Null 금지 | 숫자, 날짜, 객체 |
| @NotBlank | 빈 문자열 금지 |
| 고객번호, 사용자ID | @Size |
| 문자열/목록 길이 | 이름, 설명 |
| @Min | 최소값 |
| pageNo | @Max |
| 최대값 | pageSize |
| @Pattern | 정규식 |
| 날짜, 코드 | @Email |
| 이메일 형식 | 이메일 |
| @Positive | 양수 |
| 금액, 건수 | @Valid |
| 중첩 객체 검증 | 목록 Item |

예시는 다음과 같다.
@Getter
@Setter
@NoArgsConstructor
```java
public class OmUserSaveRequest {
    @NotBlank(message = "사용자ID는 필수입니다.")
    @Size(max = 30, message = "사용자ID는 30자를 초과할 수 없습니다.")
    private String userId;
    @NotBlank(message = "사용자명은 필수입니다.")
    @Size(max = 50, message = "사용자명은 50자를 초과할 수 없습니다.")
    private String userName;
    @NotBlank(message = "권한그룹ID는 필수입니다.")
    private String authGroupId;
    @Pattern(regexp = "^[YN]$", message = "사용여부는 Y 또는 N만 가능합니다.")
    private String useYn;
}
```

### 19.22 커스텀 Annotation 작성 기준

반복되는 검증은 커스텀 Annotation으로 작성할 수 있다.
예를 들어 yyyyMMdd 형식 검증은 다음처럼 공통화한다.
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = YyyyMmDdValidator.class)
```java
public @interface YyyyMmDd {
    String message() default "날짜는 yyyyMMdd 형식이어야 합니다.";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
```

Validator 예시는 다음과 같다.
```java
public class YyyyMmDdValidator implements ConstraintValidator<YyyyMmDd, String> {
    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) {
            return true;
        }
        try {
            LocalDate.parse(value, DateTimeFormatter.BASIC_ISO_DATE);
            return true;
        } catch (DateTimeParseException e) {
            return false;
        }
    }
}
```

사용 예시는 다음과 같다.
@YyyyMmDd
private String baseDate;

커스텀 Annotation은 단일 필드 형식 검증에만 사용하는 것이 좋다.여러 필드 간 관계나 DB 조회가 필요한 검증은 Rule에서 처리한다.

### 19.23 Validation 로그 기준

Validation 실패도 거래로그와 오류로그에 남아야 한다.
| 로그 항목 | 설명 |
| --- | --- |
| GUID | 거래 추적 ID |
| TraceId | 내부 처리 추적 ID |
| ServiceId | 실행 요청 서비스 |
| transactionCode | 거래코드 |
| userId | 사용자 |
| branchId | 지점 |
| errorCode | Validation 오류코드 |
| fieldName | 오류 필드 |
| rejectReason | 검증 실패 사유 |
| txStatus | FAIL |
| processingTime | 처리시간 |
사용자 입력값 전체를 로그에 남기지 않는다.

| 주의할 점은 다음이다. | |
개인정보, 계좌번호, 고객번호는 마스킹한다.

SQL, 서버 IP, StackTrace는 사용자 응답에 노출하지 않는다.

전처리·후처리 비기능 요구사항에서도 로그와 응답에 개인정보, SQL, 서버 IP, Stack Trace를 노출하지 않아야 하며, 모든 로그는 GUID, TraceId, ServiceId 기준으로 검색 가능해야 한다고 정의한다.

### 19.24 Validation 테스트 기준

Validation은 반드시 단위 테스트와 통합 테스트를 작성한다.
| 테스트 구분 | 테스트 내용 |
| --- | --- |
| DTO 단위 테스트 | 필수값, 길이, 형식 오류 |
| Rule 단위 테스트 | 업무 기간, 상태, 한도 검증 |
| Service 테스트 | 중복, 존재 여부, 상태 전이 |
| Header 테스트 | serviceId, transactionCode 누락 |
| 권한 테스트 | 타 지점 고객 조회 차단 |
| 거래통제 테스트 | 미등록 거래 차단 |
| 중복요청 테스트 | 동일 IdempotencyKey 중복 요청 차단 |
| 오류응답 테스트 | 표준 오류코드와 메시지 반환 |
| 로그 테스트 | 거래로그에 Validation 오류 기록 |

테스트 예시는 다음과 같다.
```java
@Test
void 고객번호가_없으면_검증오류가_발생한다() {
    SvCustomerSummaryRequest request = new SvCustomerSummaryRequest();
    request.setCustomerNo("");
    Set<ConstraintViolation<SvCustomerSummaryRequest>> violations =
            validator.validate(request);
    assertThat(violations).isNotEmpty();
}
```

Rule 테스트 예시는 다음과 같다.
```java
@Test
void 조회기간이_3개월을_초과하면_업무오류가_발생한다() {
    assertThatThrownBy(() ->
            svCustomerRule.validateInquiryPeriod("20260101", "20260701")
    ).isInstanceOf(BusinessException.class);
}
```

### 19.25 Validation 작성 예시: SV 고객 요약 조회

#### 19.25.1 Request DTO

@Getter
@Setter
@NoArgsConstructor
```java
public class SvCustomerSummaryRequest {
    @NotBlank(message = "고객번호는 필수입니다.")
    @Size(max = 20, message = "고객번호는 20자를 초과할 수 없습니다.")
    private String customerNo;
    @YyyyMmDd
    private String baseDate;
}
```

#### 19.25.2 Handler

```java
@Component
@RequiredArgsConstructor
public class SvCustomerSummaryHandler implements TransactionHandler {
    private final SvCustomerFacade svCustomerFacade;
    private final TcfRequestValidator tcfRequestValidator;
    private final ObjectMapper objectMapper;
    @Override
    public String serviceId() {
        return "SV.Customer.selectSummary";
    }
    @Override
    public Object handle(StandardRequest<?> request, TransactionContext context) {
        SvCustomerSummaryRequest body =
                objectMapper.convertValue(
                        request.getBody(),
                        SvCustomerSummaryRequest.class
                );
        tcfRequestValidator.validate(body);
        return svCustomerFacade.selectCustomerSummary(body, context);
    }
}
```

#### 19.25.3 Rule

```java
@Component
@RequiredArgsConstructor
public class SvCustomerRule {
    private final SvCustomerDao svCustomerDao;
    public void validateCustomerInquiry(
            SvCustomerSummaryRequest request,
            TransactionContext context
    ) {
        validateCustomerExists(request.getCustomerNo());
        validateBranchAccess(request.getCustomerNo(), context.getBranchId());
    }
    private void validateCustomerExists(String customerNo) {
        if (!svCustomerDao.existsCustomer(customerNo)) {
            throw new BusinessException(
                    "E-SV-VAL-0001",
                    "고객정보가 존재하지 않습니다."
            );
        }
    }
    private void validateBranchAccess(String customerNo, String branchId) {
        if (!svCustomerDao.canAccessCustomer(customerNo, branchId)) {
            throw new BusinessException(
                    "E-SV-AUTHZ-0001",
                    "고객정보 접근 권한이 없습니다."
            );
        }
    }
}
```

#### 19.25.4 Service

```java
@Service
@RequiredArgsConstructor
public class SvCustomerService {
    private final SvCustomerRule svCustomerRule;
    private final SvCustomerDao svCustomerDao;
    public SvCustomerSummaryResponse selectCustomerSummary(
            SvCustomerSummaryRequest request,
            TransactionContext context
    ) {
        svCustomerRule.validateCustomerInquiry(request, context);
        SvCustomerSummaryResult result =
                svCustomerDao.selectCustomerSummary(
                        SvCustomerSummaryCriteria.builder()
                                .customerNo(request.getCustomerNo())
                                .branchId(context.getBranchId())
                                .build()
                );
        return SvCustomerSummaryResponse.from(result);
    }
}
```

### 19.26 Validation 금지 사항

| 금지 사항 | 사유 |
| --- | --- |
| Controller에서 모든 검증을 처리 | TCF 구조와 계층 책임 위반 |
| Handler에 업무 검증을 많이 작성 | Handler가 비대해짐 |
| DTO에서 DB 조회 | DTO 책임 위반 |
| Mapper XML에서 업무 검증 | SQL과 업무 규칙 결합 |
| Validation 실패 시 RuntimeException 직접 발생 | 표준 오류 응답 불가 |
| 오류 메시지에 SQL, StackTrace 노출 | 보안 위반 |
| 사용자 입력값 전체를 로그에 저장 | 개인정보 노출 위험 |
| Map<String, Object>로 검증 회피 | 타입 안정성 저하 |
| 조회기간, pageSize 제한 없음 | 대량조회 위험 |
| 파일 다운로드 사유 검증 누락 | 감사 대응 불가 |
| 변경 거래 중복요청 미검증 | 중복 등록·중복 발송 위험 |
| 공통코드 값을 하드코딩 | 운영 기준정보와 불일치 |

### 19.27 개발자 체크리스트

| 점검 항목 | 확인 |
| --- | --- |
| Header 검증은 STF 공통 영역에서 수행되는가? | □ |
| Request DTO에 필수값, 길이, 형식 검증이 정의되어 있는가? | □ |
| 업무 규칙 검증은 Rule 계층에 작성했는가? | □ |
| DB 존재 여부와 중복 여부는 Service/DAO 계층에서 확인하는가? | □ |
| 거래코드와 ServiceId 정합성 검증이 되는가? | □ |
| 세션 사용자와 Header 사용자가 일치하는지 확인하는가? | □ |
| 타 지점 고객 조회 등 데이터권한 검증이 있는가? | □ |
| 조회기간과 pageSize 제한이 있는가? | □ |
| 공통코드 값은 코드 마스터 또는 캐시 기준으로 검증하는가? | □ |
| 금지문자와 Script 입력 차단 기준이 있는가? | □ |
| 파일 다운로드는 건수, 권한, 사유를 검증하는가? | □ |
| 등록·수정·삭제 거래는 중복요청을 차단하는가? | □ |
| Validation 실패 시 표준 오류코드로 응답하는가? | □ |
| 거래로그에 Validation 실패가 기록되는가? | □ |
| 사용자 메시지와 운영 상세 메시지를 분리했는가? | □ |
| 개인정보와 내부 기술정보가 응답에 노출되지 않는가? | □ |

### 19.28 마무리말

Validation은 NSIGHT TCF Framework에서 업무 품질을 지키는 첫 번째 방어선이다.입력값 검증이 약하면 잘못된 데이터가 업무 로직, DB, 외부 연계까지 흘러가고, 장애 발생 시 원인을 추적하기 어려워진다.
따라서 Validation은 다음 기준으로 작성해야 한다.
공통 검증은 STF에서 처리한다.
형식 검증은 DTO에서 처리한다.
업무 검증은 Rule에서 처리한다.
DB 정합성 검증은 Service/DAO에서 처리한다.
검증 실패는 표준 오류코드와 거래로그로 남긴다.

### 19.29 시사점

NSIGHT 개발에서 Validation을 제대로 분리하면 다음 효과가 있다.
| 효과 | 설명 |
| --- | --- |
| 품질 향상 | 잘못된 요청이 업무 로직에 진입하기 전에 차단 |
| 보안 강화 | 권한 없는 조회, 금지문자, Script 입력 차단 |
| 운영 안정성 | 대량조회, 중복요청, 장시간 거래 사전 차단 |
| 장애 추적성 | GUID, ServiceId, 거래코드 기준 오류 추적 |
| 감사 대응 | 고객정보 조회, 다운로드, 권한 위반 기록 |
| 유지보수성 | 검증 위치가 명확해져 수정 영향 최소화 |

최종 원칙은 다음이다.
Validation은 개발 편의 기능이 아니라 운영 안정성과 보안성을 보장하는 아키텍처 기준이다.업무 개발자는 검증을 생략하지 말고, 검증 위치를 정확히 나누어 작성해야 한다.

---

## 관련 명명규칙 상세

세부 명명규칙은 [`명명규칙-00-목차.md`](./명명규칙-00-목차.md)의 분리본을 참조한다.

- [Batch / Scheduler 명명규칙](./명명규칙-19-Batch-Scheduler.md)

## 소스·관련 문서

| 참고 |
|------|

> znsight-guide-word: `통합 (24).docx` + 명명규칙 상세 19

| [어플리케이션계층.md](../zdoc/어플리케이션계층.md) |

## 코드베이스 정정 (develop 기준)

| 항목 | 값 |
|------|-----|
| 업무 WAR | ic, pc, ms, sv, pd, eb, ep, ss, mg + tcf-om |
| ztomcat deploy | `ztomcat/deploy-wars.sh` 13 WAR |
| buildZtomcatWars | 15 WAR |
| bootRun | gateway 8100, uj 8102, jwt 8110, ui 8099 |


---

← [18. DTO 작성 기준](./18-DTO-작성-기준.md) · [20. 표준 전문 구조](./20-표준-전문-구조.md) →