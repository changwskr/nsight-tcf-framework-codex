# 18. DTO 작성 기준

> **NSIGHT TCF 개발 Manual** · 원본: [`znsight-guide-word`](../znsight-guide-word/) · 갱신: 2026-07-05

## 18. DTO 작성 기준

### 18.1 도입 전 안내말

NSIGHT TCF Framework에서 DTO(Data Transfer Object)는 화면, TCF Framework, Handler, Facade, Service, Rule, DAO/Mapper 사이에서 데이터를 전달하기 위한 표준 객체이다.
DTO는 단순히 필드를 담는 Java Class가 아니다.NSIGHT에서는 모든 온라인 거래가 HTTP/JSON 표준 전문 + ServiceId Dispatcher 방식으로 처리되므로, DTO는 다음 역할을 가진다.
화면 JSON Body
```text
   ↓
StandardRequest
   ↓
Request DTO
↓
Handler
↓
Facade / Service / Rule
↓
DAO / Mapper Parameter
↓
DB 조회 결과
↓
Response DTO
↓
StandardResponse

```

즉, DTO는 전문 Body와 업무 로직 사이의 계약 객체이다.
기존 TCF 표준 전문 구조에서도 요청 전문은 header + body, 응답 전문은 header + result + body 구조로 정의되어 있고, 실제 실행 대상은 header.serviceId 기준으로 결정된다. 또한 Handler 개발 기준에서도 Handler는 표준 전문을 업무 요청 객체로 변환한 뒤 Facade를 호출하고, 업무 로직이나 응답 전문 조립은 직접 처리하지 않도록 정의되어 있다.

### 18.2 DTO 작성 결론

NSIGHT DTO는 다음 기준으로 작성한다.
| 구분 | 설계 기준 |
| --- | --- |
| DTO 목적 | 계층 간 데이터 전달 |
| DTO 위치 | 업무 WAR 내부 entry.dto, application.dto, persistence.dto 등 |
| 기본 원칙 | 화면 요청 DTO와 DB 조회 DTO를 분리 |
| Request DTO | 화면 또는 API 요청 Body를 표현 |
| Response DTO | 화면 또는 API 응답 Body를 표현 |
| Command DTO | 등록·수정·삭제 명령성 요청 표현 |
| Query DTO | 조회 조건 표현 |
| Result DTO | DB 조회 결과 또는 업무 처리 결과 표현 |
| Entity 사용 여부 | DB Entity를 화면 응답으로 직접 노출 금지 |
| Validation 위치 | Request DTO + Rule/Service 검증 병행 |
| Mapper Parameter | 전용 Criteria DTO 또는 Query DTO 사용 |
| 표준 전문 | StandardRequest.header + body, StandardResponse.header + result + body 유지 |

핵심 원칙은 다음이다.
DTO는 데이터를 담는 그릇이다.
DTO 안에 업무 로직을 넣지 않는다.
화면 DTO와 DB DTO를 섞지 않는다.

### 18.3 DTO 종류

NSIGHT에서는 DTO를 목적에 따라 구분한다.
| DTO 유형 | 용도 | 예시 |
| --- | --- | --- |
| Request DTO | 화면/API 요청 Body | SvCustomerSummaryRequest |
| Response DTO | 화면/API 응답 Body | SvCustomerSummaryResponse |
| Search DTO | 목록 조회 검색조건 | SvCustomerSearchRequest |
| Command DTO | 등록/수정/삭제 명령 | OmUserSaveCommand |
| Result DTO | DB 조회 결과 | SvCustomerSummaryResult |
| Criteria DTO | DAO/Mapper 조회 조건 | SvCustomerSearchCriteria |
| Item DTO | 목록의 단일 행 | SvCustomerProductItem |
| Page DTO | 페이징 응답 정보 | PageResponse |
| Integration DTO | 서비스 간 연동 요청/응답 | SvCustomerSummaryIntegrationRequest |
| File DTO | 파일 업다운로드 메타 | UdFileUploadRequest |

권장 구분은 다음과 같다.
Request DTO
= 외부에서 들어오는 값

Response DTO
= 외부로 나가는 값

Criteria DTO
= DB 조회 조건

Result DTO
= DB 조회 결과

Command DTO
= 저장/수정/삭제 처리 명령

### 18.4 DTO 패키지 기준

업무 WAR 기준 DTO 패키지는 다음처럼 정리한다.
com.nh.nsight.marketing.sv
```text
 ├─ entry
 │   ├─ handler
 │   └─ dto
 │       ├─ request
 │       └─ response
 │
 ├─ application
 │   ├─ facade
 │   ├─ service
 │   ├─ rule
 │   └─ dto
 │       ├─ command
 │       ├─ query
 │       └─ result
 │
 └─ persistence
     ├─ dao
     ├─ mapper
     └─ dto
         ├─ criteria
         └─ result
```

패키지별 책임은 다음과 같다.
| 패키지 | DTO 유형 | 설명 |
| --- | --- | --- |
| entry.dto.request | Request DTO | 화면/API 요청 Body |
| entry.dto.response | Response DTO | 화면/API 응답 Body |
| application.dto.command | Command DTO | 등록·수정·삭제 처리 명령 |
| application.dto.query | Query DTO | 업무 조회 조건 |
| application.dto.result | Result DTO | 업무 처리 결과 |
| persistence.dto.criteria | Criteria DTO | Mapper 조회 조건 |
| persistence.dto.result | DB Result DTO | SQL 조회 결과 |

단순한 업무에서는 entry.dto와 persistence.dto만 두어도 된다.다만 복잡한 업무에서는 화면 DTO, 업무 DTO, DB DTO를 분리하는 것이 유지보수에 유리하다.

### 18.5 DTO 명명 규칙

DTO 이름은 업무코드 + 도메인 + 목적 + 유형으로 작성한다.
{BusinessCode}{Domain}{Purpose}{DtoType}

예시는 다음과 같다.
| 용도 | 명명 예시 | 설명 | 고객 요약 조회 요청 |
| --- | --- | --- | --- |
| SvCustomerSummaryRequest | SV 고객 요약 조회 요청 | 고객 요약 조회 응답 | SvCustomerSummaryResponse |
| SV 고객 요약 조회 응답 | 고객 검색 조건 | SvCustomerSearchRequest | 고객 목록 검색 조건 |
| 고객 상세 응답 | SvCustomerDetailResponse | 고객 상세 응답 | 고객 조회 Criteria |
| SvCustomerSearchCriteria | Mapper 조회 조건 | 고객 DB 조회 결과 | SvCustomerSummaryResult |
| SQL 결과 DTO | 사용자 저장 요청 | OmUserSaveRequest | OM 사용자 저장 요청 |
| 사용자 저장 명령 | OmUserSaveCommand | Service 내부 저장 명령 | 파일 다운로드 요청 |
| UdFileDownloadRequest | 파일 다운로드 요청 | 메시지 발송 요청 | MgMessageSendRequest |

메시지 발송 요청
금지 예시는 다음과 같다.
| 금지 이름 | 문제점 | 권장 이름 |
| --- | --- | --- |
| DataDto | 의미 없음 | SvCustomerSummaryResponse |
| RequestDto | 어떤 요청인지 불명확 | OmUserSaveRequest |
| ResultMapDto | Map 중심 사고 | SvCustomerSummaryResult |
| CustomerVO | DTO/VO/Entity 혼용 | SvCustomerDetailResponse |
| TempDto | 임시 객체가 운영 반영될 위험 | 목적이 드러나는 이름 사용 |

### 18.6 Request DTO 작성 기준

Request DTO는 화면 또는 API에서 들어오는 body 구조를 표현한다.
예시는 다음과 같다.
@Getter
@Setter
@NoArgsConstructor
```java
public class SvCustomerSummaryRequest {
    @NotBlank(message = "고객번호는 필수입니다.")
    private String customerNo;
}
```

Request DTO 작성 기준은 다음과 같다.
| 기준 | 설명 |
| --- | --- |
| 필수값은 Validation Annotation 사용 | @NotBlank, @NotNull, @Size 등 |
| 화면 입력값만 포함 | 서버 계산값은 넣지 않음 |
| Header 항목 중복 금지 | serviceId, userId, branchId는 Header에서 관리 |
| 업무 로직 금지 | DTO 안에서 DB 조회, 계산 로직 수행 금지 |
| 기본값 남용 금지 | 기본값은 Rule 또는 Service에서 명확히 처리 |
| 개인정보 주의 | 주민번호, 계좌번호 등은 마스킹 정책 고려 |
| 날짜 타입 명확화 | 문자열 사용 시 형식 명시, 가능하면 LocalDate 사용 |
| 금액 타입 명확화 | 금액은 BigDecimal 사용 권장 |

좋은 예시는 다음과 같다.
@Getter
@Setter
@NoArgsConstructor
```java
public class CmCampaignCreateRequest {
    @NotBlank(message = "캠페인명은 필수입니다.")
    private String campaignName;
    @NotNull(message = "시작일자는 필수입니다.")
    private LocalDate startDate;
    @NotNull(message = "종료일자는 필수입니다.")
    private LocalDate endDate;
    @Size(max = 500, message = "캠페인 설명은 500자를 초과할 수 없습니다.")
    private String description;
}
```

나쁜 예시는 다음과 같다.
```java
public class RequestDto {
    public String a;
    public String b;
    public String serviceId;
    public String userId;
    public void saveToDatabase() {
        // DTO에서 DB 처리 금지
    }
}
```

### 18.7 Response DTO 작성 기준

Response DTO는 화면 또는 API로 내려줄 body 구조를 표현한다.
예시는 다음과 같다.
@Getter
@Builder
@AllArgsConstructor
```java
public class SvCustomerSummaryResponse {
    private String customerNo;
    private String customerName;
    private String customerGrade;
    private BigDecimal totalBalance;
    private Integer productCount;
}
```

Response DTO 작성 기준은 다음과 같다.
| 기준 | 설명 |
| --- | --- |
| 화면에 필요한 값만 포함 | DB 전체 컬럼 노출 금지 |
| 민감정보 마스킹 | 고객명, 전화번호, 계좌번호 등 마스킹 고려 |
| 내부 코드 직접 노출 주의 | 필요 시 코드명 함께 제공 |
| null 처리 기준 통일 | 빈 목록은 [], 금액은 필요 시 0 |
| 응답 전문 Header 중복 금지 | resultCode, message는 StandardResponse.result에서 관리 |
| Entity 직접 반환 금지 | DB Entity 또는 Mapper Result를 그대로 응답하지 않음 |

권장 예시는 다음과 같다.
@Getter
@Builder
@AllArgsConstructor
```java
public class OmUserDetailResponse {
    private String userId;
    private String userName;
    private String branchId;
    private String branchName;
    private String authGroupId;
    private String authGroupName;
    private String useYn;
}
```

### 18.8 목록 조회 DTO 기준

목록 조회 DTO는 검색조건과 응답 목록을 분리한다.
요청 DTO 예시는 다음과 같다.
@Getter
@Setter
@NoArgsConstructor
```java
public class SvCustomerSearchRequest {
    private String customerName;
    private String customerGrade;
    @Min(value = 1, message = "pageNo는 1 이상이어야 합니다.")
    private int pageNo = 1;
    @Min(value = 1, message = "pageSize는 1 이상이어야 합니다.")
    @Max(value = 500, message = "pageSize는 500을 초과할 수 없습니다.")
    private int pageSize = 100;
}
```

응답 DTO 예시는 다음과 같다.
@Getter
@Builder
@AllArgsConstructor
```java
public class SvCustomerSearchResponse {
    private List<SvCustomerSearchItem> list;
    private PageResponse page;
}
```

목록 Item DTO 예시는 다음과 같다.
@Getter
@Builder
@AllArgsConstructor
```java
public class SvCustomerSearchItem {
    private String customerNo;
    private String customerName;
    private String customerGrade;
    private Integer productCount;
}
```

목록 조회 기준은 다음과 같다.
| 기준 | 설명 |
| --- | --- |
| 목록 응답은 list 사용 | 화면에서 일관되게 처리 |
| 페이징 정보는 page 사용 | pageNo, pageSize, totalCount, totalPage |
| 대량 조회 제한 | pageSize 최대값 제한 |
| 정렬값 검증 | 사용자 입력 정렬 컬럼은 whitelist 검증 |
| Count SQL 분리 | 목록 SQL과 Count SQL 분리 권장 |

페이징은 TCF Core가 직접 관리하지 않고, 업무 WAR 내부의 Rule → Service → DAO → MyBatis 흐름에서 처리하는 방식으로 정리되어 있다.

### 18.9 Command DTO 작성 기준

등록, 수정, 삭제처럼 상태를 변경하는 거래는 Command DTO를 사용한다.
@Getter
@Builder
@AllArgsConstructor
```java
public class OmUserSaveCommand {
    private String userId;
    private String userName;
    private String branchId;
    private String authGroupId;
    private String useYn;
    private String modifiedBy;
}
```

Command DTO 기준은 다음과 같다.
| 기준 | 설명 |
| --- | --- |
| Request DTO를 그대로 DB로 넘기지 않음 | 서버에서 추가한 사용자, 지점, 감사정보 포함 필요 |
| Header 정보 반영 | userId, branchId, channelId 등 Context 정보 포함 |
| 업무 검증 후 생성 | Rule 또는 Service에서 Request → Command 변환 |
| 변경자 정보 포함 | 등록자, 수정자, 요청 채널 등 감사정보 포함 |
| 저장 전용 구조 | 화면 응답 DTO와 분리 |

예시는 다음과 같다.
```java
public OmUserSaveCommand toCommand(
        OmUserSaveRequest request,
        TransactionContext context
) {
    return OmUserSaveCommand.builder()
            .userId(request.getUserId())
            .userName(request.getUserName())
            .branchId(request.getBranchId())
            .authGroupId(request.getAuthGroupId())
            .useYn(request.getUseYn())
            .modifiedBy(context.getUserId())
            .build();
}
```

### 18.10 Criteria DTO 작성 기준

Criteria DTO는 DAO/Mapper에 전달하는 DB 조회 조건이다.
@Getter
@Builder
@AllArgsConstructor
```java
public class SvCustomerSearchCriteria {
    private String customerName;
    private String customerGrade;
    private int offset;
    private int limit;
}
```

Criteria DTO 기준은 다음과 같다.
| 기준 | 설명 |
| --- | --- |
| Mapper 전용 조건 | SQL 실행에 필요한 값만 포함 |
| 화면 값 그대로 전달 금지 | pageNo/pageSize는 offset/limit로 변환 |
| SQL Injection 방지 | 동적 정렬 컬럼은 whitelist 처리 |
| Header 정보 필요 시 명시 | 지점 제한 조회 등은 branchId 포함 |
| Map 사용 지양 | Map<String, Object>보다 타입 있는 DTO 사용 |

DAO/Mapper 계층은 업무 로직을 수행하는 곳이 아니라 SQL 실행 책임만 갖도록 정의되어 있으므로, Mapper Parameter도 명확한 DTO로 전달하는 것이 좋다.

### 18.11 Result DTO 작성 기준

Result DTO는 DB 조회 결과를 담는 객체이다.
@Getter
@Setter
@NoArgsConstructor
```java
public class SvCustomerSummaryResult {
    private String customerNo;
    private String customerName;
    private String customerGradeCode;
    private BigDecimal totalBalance;
    private Integer productCount;
}
```

Result DTO 기준은 다음과 같다.
| 기준 | 설명 |
| --- | --- |
| SQL 결과와 매핑 | MyBatis ResultMap 또는 alias와 일치 |
| DB 컬럼명 그대로 노출 주의 | 응답 DTO와 분리 |
| 코드값 포함 가능 | customerGradeCode 등 DB 기준값 포함 |
| 화면 응답으로 직접 반환 금지 | Response DTO로 변환 후 반환 |
| 조인 결과 표현 가능 | 여러 테이블 조회 결과를 담을 수 있음 |

변환 예시는 다음과 같다.
```java
public SvCustomerSummaryResponse toResponse(SvCustomerSummaryResult result) {
    return SvCustomerSummaryResponse.builder()
            .customerNo(result.getCustomerNo())
            .customerName(maskName(result.getCustomerName()))
            .customerGrade(result.getCustomerGradeCode())
            .totalBalance(result.getTotalBalance())
            .productCount(result.getProductCount())
            .build();
}
```

### 18.12 DTO 변환 기준

DTO 변환은 명확한 위치에서 수행한다.
| 변환 | 권장 위치 |
| --- | --- |
| 설명 | StandardRequest.body → Request DTO |
| Handler | 전문 Body를 업무 요청으로 변환 |
| Request DTO → Command DTO | Service 또는 Rule |
| 저장/수정용 내부 명령 생성 | Request DTO → Criteria DTO |
| Rule 또는 Service | 조회 조건 생성 |
| Result DTO → Response DTO | Service 또는 Rule |
| 화면 응답 구조 생성 | Response DTO → StandardResponse.body |
| ETF | 표준 응답 전문 조립 |

금지 기준은 다음과 같다.
Controller에서 업무 DTO를 직접 DB DTO로 변환하지 않는다.
Handler에서 SQL 조회 결과를 응답 DTO로 직접 만들지 않는다.
DAO에서 화면 Response DTO를 만들지 않는다.
Mapper XML에서 화면 응답 구조를 의식하지 않는다.

### 18.13 DTO Validation 기준

DTO 검증은 1차 형식 검증과 2차 업무 검증으로 나눈다.
| 검증 단계 | 위치 |
| --- | --- |
| 예시 | 1차 형식 검증 |
| Request DTO Annotation | 필수값, 길이, 숫자 범위 |
| 2차 업무 검증 | Rule / Service |
| 시작일 ≤ 종료일, 권한, 상태값 | 3차 DB 정합성 검증 |
| Service / DAO | 존재 여부, 중복 여부 |
| 4차 거래통제 검증 | STF |
Request DTO Annotation 예시는 다음과 같다.

| ServiceId, 거래코드, 사용자, 채널, 지점 | |

```java
@Getter
@Setter
@NoArgsConstructor
public class CmCampaignSearchRequest {
```

    @Size(max = 100, message = "캠페인명은 100자를 초과할 수 없습니다.")
    private String campaignName;

    @Pattern(regexp = "^[A-Z0-9_-]*$", message = "상태코드 형식이 올바르지 않습니다.")
    private String campaignStatus;

    @Min(value = 1, message = "pageNo는 1 이상이어야 합니다.")
    private int pageNo = 1;

    @Max(value = 500, message = "pageSize는 500을 초과할 수 없습니다.")
```java
private int pageSize = 100;
}
```

업무 Rule 검증 예시는 다음과 같다.
```java
public void validateSearchPeriod(LocalDate startDate, LocalDate endDate) {
    if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
        throw new BusinessException(
            "E-CM-VAL-0001",
            "시작일자는 종료일자보다 클 수 없습니다."
        );
    }
}
```

### 18.14 DTO 필드 타입 기준

DTO 필드 타입은 다음 기준을 사용한다.
데이터 유형
권장 타입
| 설명 | 문자열 | String |
| --- | --- | --- |
| 코드, 명칭, ID | 정수 | Integer, Long |
| 건수, 일련번호 | 금액 | BigDecimal |
| 금액, 잔액, 비율 계산 | 날짜 | LocalDate |
| 일자 | 일시 | LocalDateTime |
| 처리일시 | 여부 | String 또는 Boolean |
| DB 기준 Y/N이면 String 가능 | 코드 | String |
| 공통코드 기준 | 목록 | List<T> |
| 목록 응답 | Map | 제한 사용 |

동적 구조가 필요한 경우만 사용
금액에 double, float 사용은 금지한다.
// 금지
private double amount;

// 권장
private BigDecimal amount;

### 18.15 DTO와 Header 정보 분리 기준

표준 전문 Header에 있는 값은 업무 Request DTO에 중복해서 넣지 않는다.
| Header 항목 | DTO 포함 여부 | 기준 |
| --- | --- | --- |
| serviceId | X | Header 기준 |
| transactionCode | X | Header 기준 |
| businessCode | X | Header 기준 |
| userId | X | TransactionContext 기준 |
| channelId | X | TransactionContext 기준 |
| branchId | X | TransactionContext 기준 |
| guid | X | 공통 추적 기준 |
| traceId | X | 공통 추적 기준 |

업무 DTO에는 업무 Body 값만 둔다.
좋은 예시는 다음과 같다.
```java
public class SvCustomerSummaryRequest {
    private String customerNo;
}
```

나쁜 예시는 다음과 같다.
```java
public class SvCustomerSummaryRequest {
    private String serviceId;
    private String transactionCode;
    private String businessCode;
    private String userId;
    private String branchId;
    private String customerNo;
}
```

Header 정보가 필요하면 TransactionContext에서 읽는다.
```java
public SvCustomerSummaryResponse selectCustomerSummary(
        SvCustomerSummaryRequest request,
        TransactionContext context
) {
    String userId = context.getUserId();
    String branchId = context.getBranchId();
    // 업무 처리
}
```

### 18.16 DTO와 Entity 분리 기준

NSIGHT에서는 Entity 또는 DB Result를 화면 응답으로 직접 반환하지 않는다.
```text
DB Entity / Result DTO
↓ 변환
Response DTO
↓
StandardResponse.body
```

분리해야 하는 이유는 다음과 같다.
| 이유 | 설명 |
| --- | --- |
| 보안 | DB 내부 컬럼이 외부로 노출될 수 있음 |
| 변경 영향 최소화 | DB 컬럼 변경이 화면 API 변경으로 이어지지 않게 함 |
| 마스킹 | 응답 전 개인정보 마스킹 가능 |
| 코드명 변환 | 코드값을 화면용 명칭으로 변환 가능 |
| 응답 표준화 | 화면별 필요한 구조로 조립 가능 |

### 18.17 DTO와 MyBatis Mapper 기준

MyBatis Mapper에는 Map보다 명확한 DTO를 전달한다.
권장 예시는 다음과 같다.
```java
@Mapper
public interface SvCustomerMapper {
    SvCustomerSummaryResult selectCustomerSummary(
            SvCustomerSearchCriteria criteria
    );
}
```

Mapper XML 예시는 다음과 같다.
<select id="selectCustomerSummary"
        parameterType="com.nh.nsight.marketing.sv.persistence.dto.criteria.SvCustomerSearchCriteria"
        resultType="com.nh.nsight.marketing.sv.persistence.dto.result.SvCustomerSummaryResult">
    SELECT
        CUSTOMER_NO        AS customerNo,
        CUSTOMER_NAME      AS customerName,
        CUSTOMER_GRADE_CD  AS customerGradeCode,
        TOTAL_BALANCE      AS totalBalance,
        PRODUCT_COUNT      AS productCount
    FROM SV_CUSTOMER_SUMMARY
    WHERE CUSTOMER_NO = #{customerNo}
</select>

MyBatis는 DAO/Mapper 계층에 위치하고, SQL 위치와 작성 방식, DB 접근 경계, Query Timeout, Mapper 표준, SQL ID와 GUID/거래로그 연계를 통제하는 표준 메커니즘으로 정의되어 있다.

### 18.18 DTO 예시: SV 고객 요약 조회

#### 18.18.1 Request DTO

@Getter
@Setter
@NoArgsConstructor
```java
public class SvCustomerSummaryRequest {
    @NotBlank(message = "고객번호는 필수입니다.")
    private String customerNo;
}
```

#### 18.18.2 Criteria DTO

@Getter
@Builder
@AllArgsConstructor
```java
public class SvCustomerSummaryCriteria {
    private String customerNo;
    private String branchId;
}
```

#### 18.18.3 Result DTO

@Getter
@Setter
@NoArgsConstructor
```java
public class SvCustomerSummaryResult {
    private String customerNo;
    private String customerName;
    private String customerGradeCode;
    private BigDecimal totalBalance;
    private Integer productCount;
}
```

#### 18.18.4 Response DTO

@Getter
@Builder
@AllArgsConstructor
```java
public class SvCustomerSummaryResponse {
    private String customerNo;
    private String customerName;
    private String customerGrade;
    private BigDecimal totalBalance;
    private Integer productCount;
}
```

#### 18.18.5 변환 예시

```java
public SvCustomerSummaryCriteria toCriteria(
        SvCustomerSummaryRequest request,
        TransactionContext context
) {
    return SvCustomerSummaryCriteria.builder()
            .customerNo(request.getCustomerNo())
            .branchId(context.getBranchId())
            .build();
}
public SvCustomerSummaryResponse toResponse(
        SvCustomerSummaryResult result
) {
    return SvCustomerSummaryResponse.builder()
            .customerNo(result.getCustomerNo())
            .customerName(maskName(result.getCustomerName()))
            .customerGrade(result.getCustomerGradeCode())
            .totalBalance(result.getTotalBalance())
            .productCount(result.getProductCount())
            .build();
}
```

### 18.19 DTO 작성 시 금지 사항

| 금지 사항 | 사유 |
| --- | --- |
| DTO에 업무 로직 작성 | 책임 분리 위반 |
| DTO에서 DB 접근 | 계층구조 위반 |
| DTO에 ServiceId/Header 값 중복 | 전문 Header와 중복 |
| Entity를 Response로 직접 반환 | 보안·변경 영향 문제 |
| Map<String, Object> 남용 | 타입 안정성 저하 |
| Object 타입 남용 | 계약 불명확 |
| double로 금액 표현 | 정밀도 오류 |
| Request DTO를 Mapper에 직접 전달 | 화면 구조와 SQL 구조 결합 |
| DB Result DTO를 화면에 직접 반환 | 내부 구조 노출 |
| DTO 이름을 DataDto, TempDto로 작성 | 의미 불명확 |
| 하나의 DTO를 요청/응답/DB에 모두 사용 | 변경 영향 확대 |

```java
public field 사용
캡슐화 위반
```

### 18.20 개발자 체크리스트

| 점검 항목 | 확인 |
| --- | --- |
| Request DTO와 Response DTO를 분리했는가? | □ |
| Header 항목을 DTO에 중복해서 넣지 않았는가? | □ |
| DTO 이름이 업무코드 + 도메인 + 목적 + 유형으로 작성되었는가? | □ |
| Request DTO에 필수값 검증이 있는가? | □ |
| 금액은 BigDecimal을 사용하는가? | □ |
| 날짜/일시는 LocalDate, LocalDateTime을 사용하는가? | □ |
| 목록 조회는 list + page 구조인가? | □ |
| Mapper Parameter는 Criteria DTO를 사용하는가? | □ |
| DB Result DTO와 Response DTO를 분리했는가? | □ |
| Entity를 화면에 직접 반환하지 않는가? | □ |
| 개인정보 마스킹 기준을 반영했는가? | □ |
| DTO에 업무 로직이나 DB 접근 코드가 없는가? | □ |
| Map<String, Object>를 불필요하게 사용하지 않았는가? | □ |
| 오류 발생 시 표준 오류코드와 연결되는가? | □ |
| 거래로그에서 ServiceId, 거래코드, DTO 입력값 추적이 가능한가? | □ |

### 18.21 마무리말

DTO는 NSIGHT 업무 개발의 가장 기본이 되는 계약 객체이다.DTO가 명확하면 Handler, Facade, Service, Rule, DAO, Mapper의 책임도 명확해진다.
반대로 DTO를 대충 만들면 화면 구조, 업무 로직, SQL 구조가 하나로 엉키고, 나중에 작은 컬럼 하나를 바꾸더라도 화면·서비스·SQL 전체를 수정해야 하는 문제가 발생한다.

### 18.22 시사점

NSIGHT TCF 개발에서 DTO 작성의 핵심은 다음이다.
외부 요청 DTO, 내부 업무 DTO, DB DTO, 외부 응답 DTO를 구분한다.DTO는 데이터를 전달할 뿐, 업무 판단은 Rule과 Service에서 수행한다.DTO 표준화는 개발 편의가 아니라 운영 안정성과 장애 추적성을 위한 기준이다.

---

## 관련 명명규칙 상세

세부 명명규칙은 [`명명규칙-00-목차.md`](./명명규칙-00-목차.md)의 분리본을 참조한다.

- [Java DTO 명명규칙](./명명규칙-11-Java-DTO.md)
- [Gateway 라우팅 명명규칙](./명명규칙-18-Gateway-라우팅.md)

## 소스·관련 문서

| 참고 |
|------|

> znsight-guide-word: `통합 (23).docx` + 명명규칙 상세 11,18

| [applicationNaming.md](../zdoc/applicationNaming.md) |

## 코드베이스 정정 (develop 기준)

| 항목 | 값 |
|------|-----|
| 업무 WAR | ic, pc, ms, sv, pd, eb, ep, ss, mg + tcf-om |
| ztomcat deploy | `ztomcat/deploy-wars.sh` 13 WAR |
| buildZtomcatWars | 15 WAR |
| bootRun | gateway 8100, uj 8102, jwt 8110, ui 8099 |


---

← [17. 거래코드 설계 기준](./17-거래코드-설계.md) · [19. Validation 작성 기준](./19-Validation-작성-기준.md) →