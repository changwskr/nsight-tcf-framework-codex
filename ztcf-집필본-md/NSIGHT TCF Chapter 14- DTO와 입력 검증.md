<!-- source: ztcf-집필본/NSIGHT TCF Chapter 14- DTO와 입력 검증.docx -->
<!-- tables: mammoth-html + placeholder table-to-md -->

# 제14장. DTO와 입력 검증

## 이 장을 시작하며

제13장에서는 자연어로 전달된 요구사항을 다음과 같은 개발 단위로 바꾸었다.

요구사항
↓
화면 이벤트
↓
ServiceId
↓
입력·출력
↓
업무 규칙
↓
오류
↓
데이터
↓
운영·테스트 기준

이제 정의된 입력과 출력을 Java 객체로 표현해야 한다.

초보 개발자는 DTO를 다음처럼 생각하기 쉽다.

화면에서 받은 JSON을 담는 클래스

필드를 모아 놓은 Java 객체

Controller와 Service 사이에서 전달하는 데이터

이 설명도 틀리지는 않는다.

그러나 엔터프라이즈 시스템에서 DTO는 단순한 필드 묶음이 아니다.

어떤 데이터를 받을 것인가?

어떤 타입으로 받을 것인가?

어떤 값이 필수인가?

어떤 형식과 범위를 허용할 것인가?

누가 이 객체를 사용할 것인가?

외부에 어떤 데이터를 공개할 것인가?

계약이 변경되면 기존 소비자는 계속 동작하는가?

이 질문에 대한 답을 코드로 표현하는 **데이터 계약**이다.

DTO를 잘못 설계하면 다음과 같은 문제가 발생한다.

화면 Request DTO를 Mapper가 그대로 사용한다.

DB 조회 Row를 화면에 그대로 반환한다.

입력되지 않은 값과 빈 문자열을 구분하지 못한다.

클라이언트가 보낸 userId와 branchId를 신뢰한다.

날짜를 문자열로 받아 잘못된 날짜를 허용한다.

업무 상태 검증을 Annotation 하나로 처리하려 한다.

모든 검증 오류를 시스템 오류로 반환한다.

검증 실패값과 개인정보를 로그에 그대로 남긴다.

NSIGHT TCF의 권장 데이터 흐름은 다음과 같다.

StandardRequest<Map<String, Object>>
↓
요청 Body 변환
↓
Request DTO
↓
형식 Validation
↓
Facade·Service
↓
Query·Command DTO
↓
DAO·Mapper
↓
Result·Row DTO
↓
업무 판단·마스킹
↓
Response DTO
↓
ETF
↓
StandardResponse<Response DTO>

Request·Query·Result·Response DTO는 각각 다른 경계를 표현하며 하나의 DTO를 모든 계층에서 공용으로 사용하지 않는 것이 기준이다.

현재 대표 거래인 SV.Customer.selectSummary도 다음 객체를 분리해 사용한다.

CustomerSummaryRequest
→ CustomerSummaryCriteria
→ CustomerSummaryRow
→ CustomerSummaryResponse

이 구조 자체는 올바른 방향이다. 다만 현재 소스는 Map을 CustomerSummaryRequest.fromMap()으로 변환한 뒤 SvCustomerRule에서 필수값과 길이를 수동 검증한다. 따라서 목표 구조에서는 다음을 보완하는 것이 적절하다.

현재
Map 변환
→ Rule에서 형식 검증
→ 업무 검증

목표
공통 Body 변환
→ Bean Validation
→ Service·Rule 업무 검증

DTO 종류와 사용 경계를 분리하는 구조는 현재 대표 거래 지도에서도 확인된다.

## 핵심 관점

DTO는 데이터를 운반하는 그릇이 아니라
시스템 경계를 통과할 수 있는 데이터의 계약이다.

Validation은 값이 계약에 맞는지 확인하고,
업무 검증은 그 요청을 업무적으로 허용할지 판단한다.

## 학습 목표

이 장을 마치면 다음 내용을 설명하고 구현할 수 있어야 한다.

| 번호 | 학습 목표 |
| --- | --- |
| 1 | DTO가 경계 데이터 계약인 이유를 설명한다. |
| 2 | Request·Response·Query·Command·Result DTO를 구분한다. |
| 3 | DTO와 Entity·Value Object의 차이를 설명한다. |
| 4 | 하나의 DTO를 전체 계층에서 재사용할 때의 문제를 설명한다. |
| 5 | 요청 Body를 타입이 있는 DTO로 안전하게 변환한다. |
| 6 | 필수값·길이·형식·범위 검증을 작성한다. |
| 7 | @Valid와 @Validated의 차이를 설명한다. |
| 8 | 중첩 객체와 목록의 Validation을 구현한다. |
| 9 | 문자열 날짜와 LocalDate 사용 차이를 설명한다. |
| 10 | null·빈 문자열·미전송 값을 구분한다. |
| 11 | 형식 Validation과 업무 검증을 구분한다. |
| 12 | DB 조회가 필요한 검증을 Bean Validation에 넣지 않는다. |
| 13 | 사용자 입력과 인증 Context 값을 구분한다. |
| 14 | Request DTO에서 Query·Command DTO를 생성한다. |
| 15 | DB Result를 Response DTO로 안전하게 변환한다. |
| 16 | 개인정보와 내부 필드를 응답에서 제외한다. |
| 17 | 입력 오류·업무 오류·시스템 오류를 구분한다. |
| 18 | 안전한 검증 오류 메시지를 설계한다. |
| 19 | 검증 실패를 표준 TCF 오류 응답으로 변환한다. |
| 20 | DTO 계약 변경의 하위 호환성을 판단한다. |
| 21 | DTO와 Validation 단위테스트를 작성한다. |
| 22 | DTO·검증 품질 Gate를 CI/CD에 적용한다. |

# 한눈에 보는 DTO와 검증 흐름

┌────────────────────────────────────────────────────────────┐
│ 1. 화면·외부 시스템 │
│ JSON Body 전송 │
└─────────────────────────┬──────────────────────────────────┘
▼
┌────────────────────────────────────────────────────────────┐
│ 2. StandardRequest │
│ Header + Body │
└─────────────────────────┬──────────────────────────────────┘
▼
┌────────────────────────────────────────────────────────────┐
│ 3. Body 변환 │
│ Map·JSON → Request DTO │
│ 타입 변환 오류 확인 │
└─────────────────────────┬──────────────────────────────────┘
▼
┌────────────────────────────────────────────────────────────┐
│ 4. 형식 Validation │
│ 필수·길이·형식·범위·구조 │
└─────────────────────────┬──────────────────────────────────┘
▼
┌────────────────────────────────────────────────────────────┐
│ 5. 업무 검증 │
│ 존재·상태·권한·중복·업무 가능 여부 │
└─────────────────────────┬──────────────────────────────────┘
▼
┌────────────────────────────────────────────────────────────┐
│ 6. Query·Command 변환 │
│ 검증·보정된 내부 처리 객체 │
└─────────────────────────┬──────────────────────────────────┘
▼
┌────────────────────────────────────────────────────────────┐
│ 7. DAO·Mapper │
│ Result·Row DTO 반환 │
└─────────────────────────┬──────────────────────────────────┘
▼
┌────────────────────────────────────────────────────────────┐
│ 8. 업무 결과 조립 │
│ 마스킹·코드명·계산·필드 선택 │
└─────────────────────────┬──────────────────────────────────┘
▼
┌────────────────────────────────────────────────────────────┐
│ 9. Response DTO │
│ 외부 공개 계약 │
└─────────────────────────┬──────────────────────────────────┘
▼
┌────────────────────────────────────────────────────────────┐
│ 10. ETF 표준 응답 │
│ 성공·입력오류·업무오류·시스템오류 │
└────────────────────────────────────────────────────────────┘

# DTO 종류와 책임

| DTO 유형 | 주요 목적 | 생성 위치 | 주요 소비자 |
| --- | --- | --- | --- |
| Request DTO | 화면·외부 입력 계약 | Handler 경계 | Facade·Service |
| Response DTO | 화면·외부 출력 계약 | Service·Assembler | ETF·화면 |
| Query DTO | 조회조건 | Service | DAO·Mapper |
| Command DTO | 등록·변경 명령 | Service·Facade | DAO·Domain |
| Result DTO | 데이터 조회 결과 | Mapper | DAO·Service |
| Row DTO | 물리 SQL 결과 매핑 | Mapper | DAO |
| Contract DTO | 다른 WAR·외부 연계 계약 | Client·Adapter | 연계 시스템 |
| Event DTO | 비동기 이벤트 계약 | Domain·Service | Consumer |
| Domain Object | 업무 상태·불변식 | Domain·Service | 업무 내부 |

DTO 분리 기준은 다음과 같다.

Request DTO
≠ Query DTO
≠ Result DTO
≠ Response DTO

DB 컬럼이 변경되었다고 화면 계약이 함께 변경되어서는 안 되며, 화면 입력 항목이 추가됐다고 Mapper Result에 같은 필드를 강제로 추가해서도 안 된다.

# 현재 구현과 목표 구조

## 현재 대표 거래

StandardRequest<Map<String, Object>>
↓
SvCustomerHandler
↓
SvCustomerFacade
↓ CustomerSummaryRequest.fromMap(body)
CustomerSummaryRequest
↓
SvCustomerRule.buildSummaryCriteria()
├─ 고객번호 필수 검증
├─ 고객번호 길이 검증
└─ CustomerSummaryCriteria 생성
↓
SvCustomerMapper
↓
CustomerSummaryRow
↓
CustomerSummaryResponse.toMap()

## 현재 구현 상태

| 항목 | 상태 | 판단 |
| --- | --- | --- |
| Request·Criteria 분리 | 구현 확인 | 올바른 방향 |
| Row·Response 분리 | 구현 확인 | 올바른 방향 |
| Request 불변 필드 | 구현 확인 | 양호 |
| Map 기반 요청 | 구현 확인 | 타입 안전성 보완 필요 |
| 수동 Trim | 구현 확인 | 필드별 정규화 필요 |
| 필수값 검증 | 구현 확인 | Rule에서 수동 처리 |
| 길이 검증 | 구현 확인 | Rule에서 수동 처리 |
| 날짜 형식 검증 | 보완 필요 | 문자열 상태 |
| Bean Validation | 확인되지 않음 | 공통 적용 필요 |
| 공통 변환 오류 처리 | 보완 필요 | 거래별 중복 가능 |
| Response의 Row 직접 포함 | 부분 구현 | 외부·DB 계약 결합 가능 |
| 금액·건수의 Object 타입 | 보완 필요 | 명확한 타입 필요 |
| 표준 Field Error | 보완 필요 | 오류 계약 추가 필요 |

## 목표 구조

StandardRequest<Map<String, Object>>
↓
TransactionBodyConverter
├─ ObjectMapper 변환
├─ 타입 오류 변환
└─ Bean Validator 실행
↓
CustomerSummaryRequest
↓
SvCustomerFacade
↓
SvCustomerService
├─ SvCustomerRule 업무 검증
├─ CustomerSummaryQuery 생성
└─ DAO 호출
↓
CustomerSummaryResult
↓
ResponseAssembler·Masking
↓
CustomerSummaryResponse

# 14.1 요청·응답 DTO 분리

## 14.1.1 DTO란 무엇인가

DTO는 Data Transfer Object의 약자다.

그러나 단순히 데이터를 전달하는 객체라는 설명만으로는 부족하다.

DTO는 특정 경계에서 다음 내용을 약속한다.

| 계약 요소 | 설명 |
| --- | --- |
| 필드 | 전달 가능한 데이터 |
| 타입 | 문자열·숫자·날짜·목록 |
| 필수 여부 | 반드시 입력하거나 반환할 값 |
| 길이 | 최소·최대 크기 |
| 형식 | 코드·날짜·식별자 형식 |
| 범위 | 허용 가능한 숫자·날짜 범위 |
| Null 의미 | 미입력·미존재·적용 안 함 |
| 보안 | 개인정보·마스킹·내부 필드 |
| 버전 | 계약 변경·호환성 |
| 소비자 | 이 계약을 사용하는 화면·시스템 |

## 14.1.2 하나의 DTO를 전체 계층에서 사용하면 안 되는 이유

금지 구조:

CustomerDto
\= 화면 Request
\= Service 입력
\= Mapper Parameter
\= DB Result
\= 화면 Response

문제:

| 문제 | 결과 |
| --- | --- |
| 필드 과다 | 거래에 필요하지 않은 값까지 전달 |
| Null 의미 혼합 | 미입력과 미조회 결과 구분 어려움 |
| 외부·내부 결합 | DB 변경이 화면에 전파 |
| 개인정보 노출 | 내부 조회 필드가 응답에 노출 |
| 검증 혼합 | 입력 형식과 업무 상태가 한 객체에 혼재 |
| 변경 전파 | 단순 화면 변경이 SQL까지 영향 |
| 테스트 복잡 | 경계별 계약을 독립 검증하기 어려움 |
| 권한 위험 | 클라이언트 입력으로 내부 필드 조작 가능 |

## 14.1.3 권장 DTO 변환 흐름

StandardRequest<Map>
↓
CustomerSummaryRequest
↓
CustomerSummaryQuery
↓
CustomerSummaryRow
↓
CustomerSummaryResult
↓
CustomerSummaryResponse
↓
StandardResponse<CustomerSummaryResponse>

객체별 의미:

| 객체 | 설명 |
| --- | --- |
| StandardRequest | TCF 공통 Header·Body |
| CustomerSummaryRequest | 외부에서 받은 조회 입력 |
| CustomerSummaryQuery | 검증·보정된 DB 조회조건 |
| CustomerSummaryRow | MyBatis 물리 조회 결과 |
| CustomerSummaryResult | 업무 내부에서 사용하는 결과 |
| CustomerSummaryResponse | 외부에 공개하는 응답 |

## 14.1.4 Request DTO 설계

권장 예:

public record CustomerSummaryRequest(

@NotBlank(message = "{SV.CUS.customerNo.required}")
@Size(max = 20, message = "{SV.CUS.customerNo.maxLength}")
@Pattern(
regexp = "^\[A-Za-z0-9\]+$",
message = "{SV.CUS.customerNo.format}"
)
String customerNo,

@PastOrPresent(
message = "{SV.CUS.baseDate.pastOrPresent}"
)
LocalDate baseDate,

Boolean includeProducts
) {

public boolean includeProductsOrDefault() {
return Boolean.TRUE.equals(includeProducts);
}
}

요청 DTO에서 확인할 항목:

| 항목 | 기준 |
| --- | --- |
| 클래스명 | {대상}{행위}Request |
| 외부 입력만 포함 | 내부 상태·DB 필드 제외 |
| 타입 | 가능한 구체 타입 사용 |
| Validation | 형식 조건 선언 |
| 불변성 | record 또는 final 필드 우선 |
| 기본값 | 의미가 명확한 경우만 제공 |
| 개인정보 | 최소 입력 원칙 |
| 인증값 | 클라이언트 입력으로 신뢰하지 않음 |

## 14.1.5 인증정보를 Request DTO에 넣을 때의 주의

금지 예:

public record CustomerSummaryRequest(
String customerNo,
String userId,
String branchId,
String roleCode
) {}

클라이언트가 userId, branchId, roleCode를 조작할 수 있다.

권장:

CustomerSummaryRequest
\= 사용자가 입력한 조회조건

AuthenticationContext
\= 검증된 사용자·지점·권한

Service에서는 다음과 같이 사용한다.

CustomerSummaryQuery query =
CustomerSummaryQuery.of(
request.customerNo(),
request.baseDate(),
authenticationContext.getBranchId()
);

클라이언트 Header와 인증 Context 값이 모두 존재하면 STF에서 정합성을 검증하고, 최종 신뢰값은 인증 Context에서 가져온다.

## 14.1.6 Request DTO에 포함하지 않아야 할 값

DB 내부 PK
처리상태
승인자 ID
등록자 ID
최종 변경시각
권한 결과
내부 오류코드
Mapper SQL ID
시스템 계산금액
다른 사용자의 userId
서버에서 생성할 GUID

클라이언트가 보낼 필요가 없는 값은 요청 계약에서 제거한다.

## 14.1.7 Query DTO 설계

Request는 외부 계약이고 Query는 내부 조회조건이다.

public record CustomerSummaryQuery(
String customerNo,
LocalDate baseDate,
String allowedBranchId,
boolean includeProducts
) {

public static CustomerSummaryQuery from(
CustomerSummaryRequest request,
AuthenticationContext authContext,
LocalDate systemDate) {

LocalDate effectiveBaseDate =
request.baseDate() == null
? systemDate
: request.baseDate();

return new CustomerSummaryQuery(
request.customerNo(),
effectiveBaseDate,
authContext.getBranchId(),
request.includeProductsOrDefault()
);
}
}

Query DTO에서 수행할 수 있는 일:

기본값 적용
검증된 인증값 반영
조회범위 확정
정규화된 코드 반영
Mapper에 필요한 조건 구성

Query DTO에 사용자 메시지나 화면 전용 필드를 포함하지 않는다.

## 14.1.8 Command DTO 설계

등록·수정 거래에서는 Query 대신 Command를 사용한다.

public record CustomerMemoCreateCommand(
String customerNo,
String memoText,
String createdBy,
String branchId,
String idempotencyKey
) {}

Command는 다음을 표현한다.

누구의 요청인가?
무엇을 변경하는가?
어떤 권한 범위인가?
중복 요청을 어떻게 식별하는가?

Request와 Command를 분리하면 클라이언트가 보내지 않아야 할 createdBy, branchId를 인증 Context에서 안전하게 채울 수 있다.

## 14.1.9 Row DTO와 Result DTO

현재 소스는 CustomerSummaryRow를 MyBatis Result로 사용한다.

Row DTO는 DB 물리 구조와 가까운 객체다.

public class CustomerSummaryRow {

private String customerNo;
private String customerName;
private String customerGrade;
private String branchCode;
private BigDecimal totalBalance;
private BigDecimal loanBalance;
private Integer productCount;
private LocalDate lastTransactionDate;
}

권장 개선:

Object totalBalance
→ BigDecimal totalBalance

Object productCount
→ Integer productCount

String lastTransactionDate
→ LocalDate lastTransactionDate

Object 타입은 다음 문제를 만든다.

실제 반환 타입 불명확
ClassCastException 가능
JSON 타입 변동
숫자 정밀도 불명확
테스트 어려움

복잡한 업무 조합이 필요한 경우 Row와 Result를 분리한다.

public record CustomerSummaryResult(
String customerNo,
String customerName,
String gradeCode,
String managementBranchId,
BigDecimal totalBalance,
Integer activeProductCount,
LocalDate lastTransactionDate
) {}

## 14.1.10 Response DTO 설계

Response DTO는 외부에 공개하는 최종 업무 계약이다.

public record CustomerSummaryResponse(
String customerNo,
String customerName,
String customerGrade,
BigDecimal totalBalance,
Integer activeProductCount,
LocalDate lastTransactionDate,
LocalDateTime dataBaseTime,
boolean masked
) {

public static CustomerSummaryResponse of(
CustomerSummaryResult result,
MaskedCustomer maskedCustomer,
LocalDateTime dataBaseTime) {

return new CustomerSummaryResponse(
maskedCustomer.customerNo(),
maskedCustomer.customerName(),
result.gradeCode(),
result.totalBalance(),
result.activeProductCount(),
result.lastTransactionDate(),
dataBaseTime,
maskedCustomer.masked()
);
}
}

## 14.1.11 Response DTO에 포함하지 않아야 할 값

DB 내부 Sequence
테이블명
SQL ID
내부 상태 플래그
암호화키
JWT
Refresh Token
사용자 비밀번호
권한 전체 목록
내부 예외 메시지
Stack Trace
불필요한 개인정보
DB 연결정보

## 14.1.12 DB Row를 직접 반환하면 안 되는 이유

금지:

public CustomerSummaryRow selectSummary(...) {
return mapper.selectCustomerSummary(query);
}

문제:

DB 컬럼이 외부 계약이 된다.

개인정보가 그대로 노출될 수 있다.

코드값을 업무명으로 변환하기 어렵다.

마스킹 책임이 불분명하다.

여러 DB 결과를 조합할 수 없다.

DB 변경이 모든 소비자에게 전파된다.

권장:

Mapper Row
↓
Service 업무 판단
↓
Masking
↓
Response DTO

## 14.1.13 표준 Header와 업무 Response 분리

현재 CustomerSummaryResponse가 businessCode, serviceId, guid를 직접 포함하면 TCF Header와 업무 Body의 책임이 중복될 수 있다.

권장 구조:

StandardResponse
├─ header
│ ├─ businessCode
│ ├─ serviceId
│ ├─ transactionCode
│ ├─ guid
│ └─ traceId
├─ result
└─ body
└─ CustomerSummaryResponse

업무 Response DTO에는 업무 결과만 포함한다.

TCF 표준 응답은 Header·Result·Body·Error를 분리하여 성공과 오류를 표현하는 구조를 사용한다.

## 14.1.14 중첩 Response DTO

복잡한 응답은 의미 단위로 조합한다.

public record CustomerSummaryResponse(
Customer customer,
Grade grade,
List<Product> products,
LocalDateTime dataBaseTime
) {

public record Customer(
String customerNo,
String customerName,
String statusCode
) {}

public record Grade(
String gradeCode,
String gradeName
) {}

public record Product(
String productCode,
String productName,
String productStatus
) {}
}

장점:

필드 의미 명확
관련 필드 그룹화
계약 문서화 용이
마스킹 범위 명확
부분 변경 영향 축소

## 14.1.15 목록은 불변으로 반환

금지:

this.products = products;

외부에서 목록을 변경할 수 있다.

권장:

this.products =
products == null
? List.of()
: List.copyOf(products);

응답 목록은 null보다 빈 목록을 사용하는 것을 기본으로 하되, null이 업무적으로 다른 의미를 갖는 경우 계약에 명시한다.

## 14.1.16 null, 빈 값, 미전송 구분

다음 값은 서로 다를 수 있다.

{}

{
"customerName": null
}

{
"customerName": ""
}

{
"customerName": " "
}

| 상태 | 일반적인 의미 |
| --- | --- |
| 미전송 | 기존값 유지·기본값 적용 가능 |
| null | 값 제거·미지정 |
| 빈 문자열 | 명시적으로 빈 값 |
| 공백 | 잘못된 입력 또는 의미 있는 문자열 |

특히 수정 거래에서 다음을 구분해야 한다.

필드 미전송
\= 변경하지 않음

필드 null
\= 값을 삭제함

빈 문자열
\= 빈 값으로 변경함

계약에 따라 다르므로 임의로 모두 null로 변환하지 않는다.

## 14.1.17 무조건적인 Trim 금지

현재 fromMap() 방식처럼 모든 문자열을 일괄 trim()하면 고객번호에는 적절할 수 있지만 메모·설명·고정길이 값에는 의미가 달라질 수 있다.

권장:

| 필드 유형 | 정규화 |
| --- | --- |
| 고객번호·코드 | 앞뒤 공백 제거 가능 |
| 검색어 | 업무정책에 따라 공백 정규화 |
| 메모·설명 | 원문 보존 원칙 |
| 비밀번호 | 절대 자동 Trim 금지 |
| 고정길이 전문 | 전문 규격 적용 |
| 이름 | 내부 공백 보존 |

정규화는 공통 변환기에서 무차별 처리하지 않고 필드 계약에 따라 수행한다.

## 14.1.18 DTO에서 Optional 사용

다음과 같은 DTO 필드는 일반적으로 권장하지 않는다.

Optional<String> customerName

이유:

JSON 직렬화 규칙 복잡
Bean Validation 조합 어려움
Jackson 설정 의존
기존 소비자와 호환성 문제

DTO에서는 null과 계약 문서로 선택값을 표현하고, 내부 처리 코드에서 필요하면 Optional로 변환한다.

# 14.2 필수값·형식·범위 검증

## 14.2.1 검증은 가능한 한 경계에서 빠르게 수행한다

잘못된 요청은 DB Connection을 얻거나 트랜잭션을 시작하기 전에 차단해야 한다.

요청 수신
↓
Header 검증
↓
Body 변환
↓
형식 Validation
↓
Facade Transaction
↓
Service·DAO

Body Validation의 최소 완료 시점은 다음과 같다.

Handler 진입 후
Facade 트랜잭션 시작 전

## 14.2.2 주요 Bean Validation Annotation

| Annotation | 목적 | 예 |
| --- | --- | --- |
| @NotNull | 객체가 null이면 안 됨 | 기준일 필수 |
| @NotBlank | 문자열 null·빈 값·공백 금지 | 고객번호 |
| @NotEmpty | 문자열·Collection 빈 값 금지 | 대상 목록 |
| @Size | 문자열·Collection 크기 | 최대 100건 |
| @Pattern | 문자열 형식 | 고객번호·코드 |
| @Min, @Max | 정수 범위 | 페이지 크기 |
| @Positive | 0보다 큰 수 | 금액·건수 |
| @PositiveOrZero | 0 이상 | 잔액 |
| @DecimalMin | 소수 최솟값 | 금액 |
| @Digits | 정수·소수 자리 | 금융금액 |
| @Past | 과거 날짜 | 생년월일 |
| @PastOrPresent | 현재 이하 | 조회 기준일 |
| @Future | 미래 날짜 | 예약일 |
| @Email | 이메일 기본 형식 | 이메일 |
| @Valid | 중첩 DTO 검증 | 주소·상품목록 |

## 14.2.3 고객요약 Request 예

public record CustomerSummaryRequest(

@NotBlank(message = "{SV.CUS.customerNo.required}")
@Size(
min = 1,
max = 20,
message = "{SV.CUS.customerNo.length}"
)
@Pattern(
regexp = "^\[A-Za-z0-9\]+$",
message = "{SV.CUS.customerNo.format}"
)
String customerNo,

@PastOrPresent(
message = "{SV.CUS.baseDate.range}"
)
LocalDate baseDate,

Boolean includeProducts
) {}

## 14.2.4 @NotNull, @NotEmpty, @NotBlank 차이

| 입력 | @NotNull | @NotEmpty | @NotBlank |
| --- | --- | --- | --- |
| null | 실패 | 실패 | 실패 |
| "" | 성공 | 실패 | 실패 |
| " " | 성공 | 성공 | 실패 |
| "ABC" | 성공 | 성공 | 성공 |

고객번호·이름·코드처럼 공백만 있는 값도 허용하지 않으려면 @NotBlank가 적합하다.

## 14.2.5 문자열 날짜보다 날짜 타입을 사용한다

문자열 검증:

@Pattern(regexp = "^\\\\d{8}$")
String baseDate;

이 방식은 다음 값을 형식상 허용할 수 있다.

20260230
20261301
00000000

권장:

@JsonFormat(pattern = "yyyyMMdd")
@PastOrPresent
LocalDate baseDate;

이렇게 하면 JSON 변환 단계에서 실제 달력 날짜인지 검증할 수 있다.

## 14.2.6 날짜 형식 정책

다음 중 하나를 프로젝트 표준으로 확정한다.

| 방식 | 예 | 장점 | 주의 |
| --- | --- | --- | --- |
| ISO | 2026-07-18 | 표준적·가독성 | 기존 전문 호환 |
| 숫자형 문자열 | 20260718 | 기존 금융전문 친화 | 별도 형식 지정 |
| Timestamp | ISO DateTime | 시각 표현 | Timezone 필수 |

날짜 형식을 거래별로 임의 선택하지 않는다.

## 14.2.7 숫자 범위 검증

페이징 요청 예:

public record CustomerListRequest(

@Min(value = 1, message = "{page.min}")
Integer pageNo,

@Min(value = 1, message = "{pageSize.min}")
@Max(value = 100, message = "{pageSize.max}")
Integer pageSize
) {}

Integer를 사용하는 이유:

미전송
→ null

0 전송
→ 범위 오류

원시타입 int를 사용하면 미전송도 0이 되어 두 상황을 구분하기 어렵다.

## 14.2.8 금액 검증

public record TransferRequest(

@NotNull
@DecimalMin(value = "0.01")
@Digits(integer = 15, fraction = 2)
BigDecimal amount
) {}

금액을 double로 표현하지 않는다.

double
→ 이진 부동소수점 오차

BigDecimal
→ 금융 단위 정밀도 제어

## 14.2.9 Enum과 허용값

문자열 코드:

@Pattern(regexp = "^(ACTIVE|INACTIVE|SUSPENDED)$")
String status;

보다 Enum을 사용할 수 있다.

public enum CustomerStatus {
ACTIVE,
INACTIVE,
SUSPENDED
}

다만 외부에서 새로운 Enum 값이 추가되면 구버전 소비자가 역직렬화에 실패할 수 있으므로 계약 호환성을 검토한다.

## 14.2.10 목록 검증

public record CustomerBulkRequest(

@NotEmpty(message = "{customerNos.required}")
@Size(max = 100, message = "{customerNos.maxSize}")
List<
@NotBlank(message = "{customerNo.required}")
@Size(max = 20)
String
\> customerNos
) {}

확인:

목록 null
빈 목록
최대 건수
개별 항목 null
개별 항목 형식
중복 항목
전체 Payload 크기

중복 항목 여부는 단순 형식 검증으로 처리할 수도 있지만, 업무적 의미가 있다면 별도 Rule에서 판단한다.

## 14.2.11 중첩 DTO 검증

public record CampaignCreateRequest(

@NotBlank
String campaignName,

@NotNull
@Valid
CampaignPeriod period,

@NotEmpty
List<@Valid CampaignTarget> targets
) {}

@Valid가 없으면 중첩 객체 내부 Annotation이 실행되지 않을 수 있다.

## 14.2.12 교차 필드 검증

예:

시작일은 종료일보다 이전이어야 한다.

고객번호와 고객명 중 하나 이상 필요하다.

종료상태이면 종료일이 필수다.

단일 필드 Annotation만으로 표현하기 어렵다.

방법 1: 클래스 수준 Custom Constraint

@ValidDateRange
public record CampaignPeriodRequest(
LocalDate startDate,
LocalDate endDate
) {}

방법 2: Rule에서 검증

public void validatePeriod(
LocalDate startDate,
LocalDate endDate) {

if (startDate == null ||
endDate == null ||
!startDate.isBefore(endDate)) {

throw new BusinessException(
"E-CM-VAL-0001",
"캠페인 기간이 올바르지 않습니다."
);
}
}

순수한 필드 관계이고 DB가 필요하지 않으면 Custom Validator를 사용할 수 있다. 업무 정책과 상태 의미가 강하면 Rule이 더 적합하다.

## 14.2.13 Custom Validator 금지사항

Custom Bean Validator에서 다음을 수행하지 않는다.

DB 조회
외부 API 호출
Cache 변경
상태 저장
트랜잭션 시작
감사로그 저장

Bean Validator는 가능한 한 입력값만으로 동일한 결과를 내는 순수 검증이어야 한다.

## 14.2.14 @Valid와 @Validated

| 구분 | @Valid | @Validated |
| --- | --- | --- |
| 표준 | Jakarta Validation | Spring |
| 중첩 검증 | 지원 | 지원 가능 |
| Validation Group | 직접 지정 어려움 | 지원 |
| 메서드 Validation | 제한적 | Spring Proxy 기반 |
| 일반 사용 | Request·중첩 DTO | Group·Service Method |

단순 요청 검증은 @Valid를 우선 사용하고, Group이나 메서드 수준 검증이 필요할 때 @Validated를 검토한다.

## 14.2.15 Validation Group

예:

public interface Create {}
public interface Update {}

public record CustomerMemoRequest(

@Null(groups = Create.class)
@NotNull(groups = Update.class)
Long memoId,

@NotBlank(groups = {Create.class, Update.class})
String memoText
) {}

그러나 Group이 많아지면 하나의 DTO가 여러 책임을 가지게 된다.

권장 우선순위:

1순위
CreateRequest·UpdateRequest 분리

2순위
공통 필드 조합

3순위
불가피한 경우 Validation Group

## 14.2.16 기본값 적용 시점

금지:

public record CustomerRequest(
String customerNo,
LocalDate baseDate
) {
public CustomerRequest {
if (baseDate == null) {
baseDate = LocalDate.now();
}
}
}

DTO 생성 시 시스템 시간을 적용하면 테스트와 재현성이 떨어질 수 있다.

권장:

LocalDate effectiveBaseDate =
request.baseDate() == null
? clock.today()
: request.baseDate();

Service 또는 Query 변환 단계에서 주입된 Clock을 사용한다.

## 14.2.17 알려지지 않은 JSON 필드

요청:

{
"customerNo": "C000001",
"adminYn": "Y"
}

adminYn이 DTO에 없을 때 정책을 결정해야 한다.

| 정책 | 장점 | 위험 |
| --- | --- | --- |
| 알 수 없는 필드 거부 | 계약 오류 조기 발견 | 필드 추가 호환성 저하 |
| 알 수 없는 필드 무시 | 확장 호환성 | 오타·공격 입력 은폐 |
| 경고 후 무시 | 운영 확인 가능 | 구현 복잡 |

권장 판단:

등록·변경·보안 민감 거래
→ 알 수 없는 필드 거부 우선

조회·하위 호환 계약
→ 승인된 범위에서 무시 가능

프로젝트 전체 정책을 ObjectMapper 설정과 계약 문서에서 일치시킨다.

## 14.2.18 요청 크기 제한

Validation Annotation만으로 대형 요청을 충분히 방어할 수 없다.

확인:

HTTP 최대 Body 크기
목록 최대 건수
문자열 최대 길이
파일 업로드 제한
JSON 중첩 깊이
압축 요청

대형 Body는 JSON 역직렬화 전에 메모리와 Thread를 점유할 수 있으므로 Apache·Tomcat·Spring 경계에서도 제한한다.

## 14.2.19 공통 Body 변환·검증기

TCF는 ServiceId에 따라 서로 다른 Request DTO를 사용하므로 공통 Controller에서 하나의 구체 DTO 타입을 선언하기 어렵다.

따라서 다음 구조가 적합하다.

@Component
public class TransactionBodyConverter {

private final ObjectMapper objectMapper;
private final Validator validator;

public TransactionBodyConverter(
ObjectMapper objectMapper,
Validator validator) {
this.objectMapper = objectMapper;
this.validator = validator;
}

public <T> T convertAndValidate(
Object body,
Class<T> targetType) {

final T target;

try {
target =
objectMapper.convertValue(body, targetType);
} catch (IllegalArgumentException exception) {
throw RequestValidationException.typeMismatch(
safeRootCause(exception)
);
}

Set<ConstraintViolation<T>> violations =
validator.validate(target);

if (!violations.isEmpty()) {
throw RequestValidationException.of(violations);
}

return target;
}
}

## 14.2.20 Handler 사용 예

@Component
public class SvCustomerHandler
implements TransactionHandler {

private final TransactionBodyConverter bodyConverter;
private final SvCustomerFacade facade;

@Override
public Object doHandle(
StandardRequest<Map<String, Object>> request,
TransactionContext context) {

String serviceId =
context.getHeader().getServiceId();

return switch (serviceId) {
case "SV.Customer.selectSummary" -> {
CustomerSummaryRequest body =
bodyConverter.convertAndValidate(
request.getBody(),
CustomerSummaryRequest.class
);

yield facade.selectCustomerSummary(
body,
context
);
}

default ->
throw new BusinessException(
ErrorCode.SERVICE\_NOT\_FOUND
);
};
}
}

이 구조에서는 Facade가 Map을 받지 않고 타입이 검증된 DTO를 받는다.

## 14.2.21 Facade 목표 인터페이스

현재:

public Map<String, Object> selectCustomerSummary(
Map<String, Object> body,
TransactionContext context)

목표:

@Transactional(readOnly = true, timeout = 3)
public CustomerSummaryResponse selectCustomerSummary(
CustomerSummaryRequest request,
TransactionContext context) {

return customerService.selectCustomerSummary(
request,
context
);
}

장점:

타입 안정성
Validation 완료 보장
Map Key 오타 방지
IDE 자동완성
계약 테스트 용이
Facade 책임 단순화

# 14.3 Validation과 업무 검증의 차이

## 14.3.1 두 검증의 핵심 차이

| 구분 | 형식 Validation | 업무 검증 |
| --- | --- | --- |
| 질문 | 값의 모양이 맞는가 | 업무적으로 허용되는가 |
| 입력 | Request DTO | Request·상태·데이터·권한 |
| 위치 | 거래 경계 | Service·Rule |
| DB 조회 | 원칙적으로 없음 | 필요할 수 있음 |
| 외부 호출 | 금지 | 설계에 따라 가능 |
| 실패 의미 | 입력 계약 위반 | 업무 처리 거절 |
| 오류 유형 | VALIDATION | BUSINESS·AUTHORIZATION |
| 재시도 | 값 수정 후 가능 | 업무 상태에 따라 다름 |
| 테스트 | DTO Validation Test | Service·Rule Test |

원본에서도 필수·길이·형식 검증은 경계에서 수행하고, 중복·상태·권한 같은 업무 규칙은 Service에서 판단하도록 구분한다.

## 14.3.2 예시로 구분하기

### 고객번호가 비어 있음

형식 Validation 오류

DB를 조회할 필요가 없다.

### 고객번호 길이는 맞지만 존재하지 않음

업무 오류

DB 조회 결과가 필요하다.

### 고객은 존재하지만 사용자가 조회할 수 없음

데이터권한 오류

인증 Context와 권한 정책이 필요하다.

### 고객은 존재하지만 정지 상태임

업무 상태 오류

고객 상태와 거래 정책이 필요하다.

## 14.3.3 검증 책임 배치

| 검증 대상 | 권장 위치 |
| --- | --- |
| Header 필수값 | STF |
| ServiceId 형식·등록 | STF·Dispatcher |
| Request 필수값 | Bean Validation |
| 문자열 길이 | Bean Validation |
| 날짜 파싱 | JSON 변환 |
| 숫자 범위 | Bean Validation |
| 필드 간 단순 관계 | Custom Validator·Rule |
| 사용자 인증 | Filter·Gateway |
| 기능권한 | STF |
| 데이터권한 | Service·Rule |
| 데이터 존재 | Service |
| 중복 데이터 | Service·Rule |
| 상태 전이 | Domain·Rule |
| 영향 행 수 | Service |
| 외부 시스템 가능 여부 | Service·Client |
| 거래통제 | STF·OM |
| Timeout | TCF·Facade·Client·DB |

## 14.3.4 고객요약 거래 검증 분리

### Bean Validation

customerNo 필수
customerNo 최대 20자
customerNo 허용 문자
baseDate 실제 날짜
baseDate 현재일 이하

### STF

JWT 유효
userId 존재
branchId 존재
고객조회 기능권한
거래 사용 상태
Timeout 정책

### Service·Rule

고객 존재
조회 가능한 고객 상태
사용자의 데이터권한 범위
고객명 원문권한
상품 조회 가능 여부

### DAO·Mapper

검증된 Query 조건으로 데이터 조회

DAO와 Mapper가 고객 미존재 메시지나 권한 오류를 결정하지 않는다.

## 14.3.5 Rule의 순수성

Rule이 입력값만으로 판단 가능한 경우:

public void validateBaseDate(
LocalDate baseDate,
LocalDate today) {

if (baseDate != null &&
baseDate.isAfter(today)) {

throw new BusinessException(
"E-SV-VAL-0003",
"기준일을 확인해 주세요."
);
}
}

DB 결과를 Service가 조회한 뒤 Rule에 전달하는 방식:

CustomerSummaryResult customer =
customerDao.selectSummary(query)
.orElseThrow(CustomerNotFoundException::new);

customerRule.validateInquiryAllowed(
customer.statusCode(),
authContext
);

Rule이 Mapper를 직접 호출하지 않는다.

## 14.3.6 검증 순서

권장 순서:

1\. JSON 구조·타입
2\. Header 필수값
3\. Request 형식
4\. 인증
5\. 기능권한
6\. 거래통제
7\. 업무 입력 관계
8\. DB 조회
9\. 존재·상태
10\. 데이터권한
11\. 결과 정책

다만 TCF 실제 파이프라인에서는 STF가 Handler보다 먼저 실행되므로 다음과 같이 동작할 수 있다.

JSON 파싱
→ STF Header·인증·통제
→ Handler Body 변환·Validation
→ Facade·Service 업무 검증

중요한 것은 Body 형식 오류가 Facade 트랜잭션과 DAO 호출 이전에 차단되는 것이다.

## 14.3.7 모든 오류를 한 번에 반환할 것인가

### 단순 형식 오류

사용자 편의를 위해 여러 필드 오류를 함께 반환할 수 있다.

{
"errors": \[
{
"field": "customerNo",
"code": "required",
"message": "고객번호를 입력해 주세요."
},
{
"field": "baseDate",
"code": "format",
"message": "기준일 형식을 확인해 주세요."
}
\]
}

### 업무 검증

다음 오류를 모두 동시에 알려 주면 보안정보가 노출될 수 있다.

고객이 존재한다.
사용자에게 권한이 없다.
고객은 휴면상태다.

업무·권한 검증은 정책에 따라 우선순위가 높은 오류 하나만 반환할 수 있다.

## 14.3.8 Fail-Fast와 전체 수집

| 방식 | 장점 | 적합 |
| --- | --- | --- |
| 첫 오류 즉시 종료 | 빠름·단순 | 보안·업무 상태 |
| 모든 형식 오류 수집 | 사용자 수정 편의 | 화면 입력 Validation |
| 오류 상한 수집 | 과도한 응답 방지 | 대형 요청 |

권장:

단순 필드 Validation
→ 최대 N건까지 수집

인증·권한·업무 상태
→ 우선순위 기준 첫 오류

## 14.3.9 중복 검증 방지

다음처럼 같은 조건을 여러 계층에 반복하지 않는다.

UI
Handler
Facade
Service
Mapper SQL

다만 UI 검증과 서버 검증은 목적이 다르다.

UI 검증
\= 사용자 편의

서버 검증
\= 계약과 보안 보장

UI에서 검증했다고 서버 검증을 생략하지 않는다.

## 14.3.10 SQL 조건을 검증 대체수단으로 사용하지 않는다

금지:

SELECT \*
FROM SV\_CUSTOMER
WHERE CUSTOMER\_NO = #{customerNo}
AND BRANCH\_CODE = #{branchId}

결과가 0건인 경우 다음을 구분하기 어렵다.

고객 미존재
지점권한 없음
고객 상태 제한
잘못된 입력

보안상 존재 여부를 숨겨야 하는 경우에는 의도적으로 같은 결과를 반환할 수 있지만, 그 판단은 Service·Rule 정책으로 명시해야 한다.

## 14.3.11 변경 거래 Validation

수정 요청:

public record CustomerMemoUpdateRequest(

@NotNull
Long memoId,

@NotBlank
@Size(max = 1000)
String memoText,

@NotNull
Long versionNo
) {}

업무 검증:

메모 존재
수정 권한
삭제 상태 아님
versionNo 일치
허용 상태

SQL:

UPDATE SV\_CUSTOMER\_MEMO
SET MEMO\_TEXT = #{memoText},
VERSION\_NO = VERSION\_NO + 1
WHERE MEMO\_ID = #{memoId}
AND VERSION\_NO = #{versionNo}

updated = 0을 Validation Annotation으로 처리할 수 없다. Service에서 동시 수정·미존재·권한을 판단해야 한다.

## 14.3.12 검증과 트랜잭션

DB가 필요 없는 형식 Validation은 트랜잭션 전에 수행한다.

DB 상태가 필요한 업무 검증은 Facade 트랜잭션 안에서 조회·변경과 일관된 시점으로 수행할 수 있다.

형식 Validation
→ Transaction 전

상태 조회·중복·Version
→ Transaction 내부

단순 조회 검증을 위해 불필요하게 긴 트랜잭션을 열지 않는다.

# 14.4 안전한 오류 메시지

## 14.4.1 오류 메시지의 두 사용자

오류는 두 종류의 사용자를 가진다.

업무 사용자
→ 다음에 무엇을 해야 하는가?

운영·개발자
→ 어디에서 왜 실패했는가?

두 목적을 한 메시지로 해결하려 하면 민감한 내부정보가 노출된다.

## 14.4.2 사용자 메시지와 운영 로그 분리

### 사용자 응답

고객번호를 입력해 주세요.

기준일을 확인해 주세요.

조회 가능한 고객이 아닙니다.

처리시간을 초과했습니다.

처리 중 오류가 발생했습니다.
GUID: G-20260718-000001

### 운영 로그

serviceId=SV.Customer.selectSummary
guid=G-20260718-000001
errorType=VALIDATION
errorCode=E-SV-VAL-0001
field=customerNo
exception=RequestValidationException
elapsedMs=42

운영 로그에도 고객번호 원문이나 전체 Request Body를 무조건 기록하지 않는다.

## 14.4.3 오류 유형

| 오류 유형 | 의미 | 사용자 행동 | 운영 대응 |
| --- | --- | --- | --- |
| VALIDATION | 입력 계약 위반 | 입력 수정 | 반복 추세 확인 |
| AUTHENTICATION | 인증 실패 | 재로그인 | Token 상태 확인 |
| AUTHORIZATION | 권한 부족 | 권한 요청 | 감사 확인 |
| BUSINESS | 업무 조건 불충족 | 상태·조건 확인 | 업무 정책 확인 |
| CONTROL | 거래 운영 차단 | 안내 | OM 정책 확인 |
| TIMEOUT | 제한시간 초과 | 상태 확인 후 재시도 | 병목 분석 |
| SYSTEM | 시스템 실패 | 나중에 재시도 | 장애 대응 |
| INTEGRATION | 연계 실패 | 정책에 따른 안내 | 대상 시스템 확인 |

## 14.4.4 Validation 오류 응답 예

{
"header": {
"businessCode": "SV",
"serviceId": "SV.Customer.selectSummary",
"transactionCode": "SV-INQ-0001",
"guid": "G-20260718-000001",
"traceId": "T-20260718-000001"
},
"result": {
"resultStatus": "FAIL",
"resultCode": "E-SV-VAL-0001",
"message": "입력값을 확인해 주세요."
},
"body": null,
"error": {
"errorType": "VALIDATION",
"errorCode": "E-SV-VAL-0001",
"fieldErrors": \[
{
"field": "customerNo",
"code": "required",
"message": "고객번호를 입력해 주세요."
}
\]
}
}

## 14.4.5 응답에 포함하지 않아야 할 정보

Java 클래스 전체 경로
Stack Trace
SQL 원문
Table 이름
DB 계정
서버 IP
파일 절대경로
정규식 내부구현
JWT
비밀번호
Private Key
개인정보 원문
내부 시스템 상세구성

## 14.4.6 거절된 값을 반환하지 않는다

금지:

{
"field": "residentNo",
"rejectedValue": "900101-1234567"
}

검증 오류 응답과 로그에 rejectedValue를 기본 포함하지 않는다.

필요한 경우 다음 정도만 남긴다.

valuePresent=true
valueLength=14
valueHash=승인된 경우

## 14.4.7 안전한 필드명

외부 필드명:

customerNo
baseDate
pageSize

내부 Java 경로:

body.customerSearchCondition.customerNo

사용자 응답에는 계약상 필드명을 제공하고 내부 객체 그래프를 노출하지 않는다.

## 14.4.8 메시지 코드와 문구 분리

Annotation에 문구를 직접 작성할 수 있다.

@NotBlank(message = "고객번호는 필수입니다.")

그러나 다국어·일관성·문구 변경을 위해 메시지 코드를 권장한다.

@NotBlank(
message = "{SV.CUS.customerNo.required}"
)

메시지 파일:

SV.CUS.customerNo.required=고객번호를 입력해 주세요.
SV.CUS.customerNo.length=고객번호 길이를 확인해 주세요.
SV.CUS.baseDate.range=기준일을 확인해 주세요.

오류코드와 메시지를 동일한 것으로 취급하지 않는다.

오류코드
\= 시스템 식별자

메시지
\= 사용자 표현

## 14.4.9 JSON 변환 오류

요청:

{
"baseDate": "2026-99-99"
}

이 오류는 Bean Validation 이전 JSON 변환 단계에서 발생할 수 있다.

표준 변환:

HttpMessageNotReadableException
또는 ObjectMapper 변환 오류
↓
RequestValidationException
↓
E-TCF-VAL-TYPE
↓
“입력값의 형식을 확인해 주세요.”

내부 Jackson 오류 문구를 사용자에게 그대로 반환하지 않는다.

## 14.4.10 Body 누락

{
"header": {
"serviceId": "SV.Customer.selectSummary"
},
"body": null
}

정책:

Body가 필수인 거래
→ Validation 오류

Body가 없어도 되는 거래
→ 빈 Request DTO 생성 또는 별도 거래 계약

거래별로 임의 처리하지 않고 Handler 등록정보나 Request 계약에서 정의한다.

## 14.4.11 HTTP 상태와 TCF 결과

프로젝트에서 다음 정책 중 하나를 일관되게 적용해야 한다.

| 상황 | HTTP | TCF Result |
| --- | --- | --- |
| JSON 자체가 파싱 불가 | 400 | 표준 오류 가능 |
| 인증 실패 | 401 | 인증 오류 |
| 권한 실패 | 403 | 권한 오류 |
| 업무 Validation | 200 또는 400 | FAIL/VALIDATION |
| 업무 오류 | 200 | FAIL/BUSINESS |
| 시스템 오류 | 500 또는 표준 200 | FAIL/SYSTEM |

NSIGHT의 표준 전문이 HTTP 200 안에서 업무 성공·실패를 구분한다면 UI는 HTTP 성공만 보고 업무 성공으로 판단하면 안 된다.

HTTP 200
≠ 업무 성공

resultStatus, resultCode를 확인해야 한다.

## 14.4.12 오류 우선순위

예를 들어 요청에 다음 문제가 동시에 있을 수 있다.

Token 만료
고객번호 누락
ServiceId 거래 중지

권장 우선순위 예:

전송·파싱 오류
→ 인증
→ Header
→ 기능권한
→ 거래통제
→ Body Validation
→ 업무 규칙
→ DB·연계

실제 TCF 파이프라인 순서와 일치하도록 정의한다.

## 14.4.13 검증 실패 로그

권장:

event=REQUEST\_VALIDATION\_FAILED
businessCode=SV
serviceId=SV.Customer.selectSummary
guid=G-...
errorCode=E-SV-VAL-0001
field=customerNo
constraint=NotBlank
elapsedMs=35

금지:

body={전체 요청 원문}
token={JWT}
customerNo={고객번호 원문}
residentNo={주민번호}

## 14.4.14 동일 오류 로그 폭증 방지

잘못된 UI 배포로 같은 Validation 오류가 대량 발생할 수 있다.

확인:

오류코드별 발생건수
채널별 발생건수
화면 ID
App Version
첫 발생시각
최근 발생시각

개별 오류 로그와 별도로 Metric을 수집해 대량 발생을 탐지한다.

# DTO 변환과 검증 전체 설계

OnlineTransactionController
↓
TCF.process()
↓
STF
├─ Header
├─ 인증
├─ 권한
├─ 거래통제
└─ Timeout
↓
TransactionDispatcher
↓
SvCustomerHandler
├─ ServiceId 분기
├─ Body → Request DTO
└─ Bean Validation
↓
SvCustomerFacade
├─ Typed Request
└─ readOnly Transaction
↓
SvCustomerService
├─ AuthenticationContext 반영
├─ Query 생성
├─ 업무 Rule
├─ DAO 호출
├─ 결과 없음 판단
├─ 마스킹
└─ Response 조립
↓
ETF
├─ success
├─ businessFail
└─ systemError

# 정상 처리 흐름

1\. 화면이 고객번호와 기준일을 전송한다.
2\. JSON이 StandardRequest로 파싱된다.
3\. STF가 Header·인증·권한·통제를 검증한다.
4\. Dispatcher가 SvCustomerHandler를 선택한다.
5\. Handler가 Body를 CustomerSummaryRequest로 변환한다.
6\. Bean Validation이 필수·길이·날짜를 검증한다.
7\. Facade가 읽기전용 트랜잭션을 시작한다.
8\. Service가 인증 Context를 반영해 Query를 만든다.
9\. Rule이 데이터권한과 업무조건을 검증한다.
10\. DAO·Mapper가 고객을 조회한다.
11\. Service가 결과를 마스킹한다.
12\. CustomerSummaryResponse를 조립한다.
13\. ETF가 StandardResponse를 반환한다.
14\. 거래로그와 감사로그를 종료한다.

# 입력 형식 오류 흐름

Body 변환
↓
고객번호 누락
↓
Bean Validation 실패
↓
Facade·DAO 미실행
↓
RequestValidationException
↓
ETF Validation 오류
↓
안전한 Field Error 반환

# 타입 변환 오류 흐름

baseDate = "2026-99-99"
↓
ObjectMapper 변환 실패
↓
타입 오류 표준화
↓
E-TCF-VAL-TYPE
↓
“입력값의 형식을 확인해 주세요.”

# 업무 검증 오류 흐름

형식 Validation 통과
↓
고객 조회
↓
고객 미존재
↓
Service가 업무 오류 판단
↓
CustomerNotFoundException
↓
ETF.businessFail()

# 권한 오류 흐름

형식 Validation 통과
↓
기능권한 또는 데이터권한 부족
↓
고객 존재·상태 상세 미노출
↓
권한 오류
↓
감사로그

# 시스템 오류 흐름

형식·업무 검증 통과
↓
Mapper 실행
↓
DB Connection 오류
↓
Persistence Exception
↓
Transaction Rollback
↓
ETF.systemError()
↓
안전한 메시지와 GUID

# 정상 예시

## Request DTO

public record CustomerSummaryRequest(

@NotBlank
@Size(max = 20)
@Pattern(regexp = "^\[A-Za-z0-9\]+$")
String customerNo,

@PastOrPresent
LocalDate baseDate,

Boolean includeProducts
) {}

## Handler

CustomerSummaryRequest body =
bodyConverter.convertAndValidate(
request.getBody(),
CustomerSummaryRequest.class
);

return facade.selectCustomerSummary(
body,
context
);

## Service

CustomerSummaryQuery query =
CustomerSummaryQuery.from(
request,
authContext,
clock.today()
);

CustomerSummaryResult customer =
customerDao.selectSummary(query)
.orElseThrow(
CustomerNotFoundException::new
);

customerRule.validateViewable(
customer,
authContext
);

return responseAssembler.toResponse(
customer,
authContext
);

# 금지 예시

Map을 Service와 Mapper까지 전달한다.

하나의 CustomerDto를 Request·Result·Response로 사용한다.

Entity나 MyBatis Row를 화면에 직접 반환한다.

Request DTO에 DB 내부 상태와 감사 필드를 넣는다.

클라이언트가 보낸 userId·branchId를 신뢰한다.

날짜를 문자열로만 받아 실제 날짜 유효성을 확인하지 않는다.

모든 문자열을 무조건 trim한다.

선택 입력에 primitive 타입을 사용해 미전송과 0을 구분하지 못한다.

금액을 double로 표현한다.

Bean Validator 안에서 DB를 조회한다.

Rule에서 Mapper를 직접 호출한다.

Validation 실패 후에도 Facade Transaction을 시작한다.

모든 검증 오류를 시스템 오류로 반환한다.

오류 응답에 rejectedValue를 포함한다.

오류 메시지에 SQL·Table·클래스 경로를 노출한다.

Request Body 전체를 로그에 남긴다.

Validation Annotation 문구를 거래마다 다르게 작성한다.

필수 필드를 추가하면서 기존 소비자 호환성을 검토하지 않는다.

Response DTO에 Standard Header 값을 중복 포함한다.

알 수 없는 JSON 필드 정책을 거래마다 다르게 적용한다.

# 책임 경계와 RACI

| 활동 | UI | 업무개발 | FW | 업무분석 | 보안 | DBA | QA | AA |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 입력 필드 정의 | R | C | I | A | C | I | C | C |
| Request DTO | C | R | C | C | C | I | C | A |
| Response DTO | C | R | C | A | C | I | C | C |
| Bean Validation | I | R | C | C | C | I | C | A |
| 공통 Body 변환 | I | C | R/A | I | C | I | C | C |
| 업무 검증 | I | R | I | A | C | C | C | C |
| 데이터권한 | I | R | C | C | A | I | C | C |
| Query·Command | I | R | I | C | I | C | C | A |
| Result·Row | I | R | I | I | C | A/C | C | C |
| 오류 응답 표준 | C | C | R | C | C | I | C | A |
| 오류 메시지 | C | R | C | A | C | I | C | C |
| 개인정보 마스킹 | C | R | C | C | A | I | C | C |
| 계약테스트 | R | R | C | C | C | I | A | C |
| 호환성 승인 | C | C | C | R | C | I | C | A |

# 데이터 및 상태관리

DTO 자체는 영속 상태를 소유하지 않는다.

DTO
\= 특정 경계의 데이터 Snapshot

DTO에 다음 상태를 저장하지 않는다.

전역 사용자 상태
Session 상태
Cache 상태
거래 진행상태
Spring Bean
DB Connection
ThreadLocal

요청 종료 후 DTO를 Static Collection에 저장하거나 재사용하지 않는다.

# 성능·용량·확장성

## 입력 크기

| 항목 | 권장 관리 |
| --- | --- |
| 문자열 | 필드별 최대 길이 |
| 목록 | 최대 항목 수 |
| 중첩 객체 | 최대 깊이 |
| 요청 Body | Apache·Tomcat 제한 |
| 응답 Body | 최대 크기·페이징 |
| 파일 | 별도 업로드 거래 |

## Validation 성능

Bean Validation은 DB 조회보다 비용이 작지만 대량 목록에서 항목별 복잡한 Custom Validator를 수행하면 CPU 부하가 발생할 수 있다.

대형 목록
× 다수 Annotation
× 복잡한 정규식
\= CPU 사용 증가

정규식은 ReDoS 위험이 없는 단순 패턴을 사용하고 최대 입력 길이를 먼저 제한한다.

# 보안·개인정보·감사

| 영역 | 기준 |
| --- | --- |
| 입력 최소화 | 업무에 필요한 값만 수신 |
| 인증정보 | Token Context에서 확보 |
| Mass Assignment | DTO에 변경 허용 필드만 선언 |
| 민감값 | 로그·오류 응답 원문 금지 |
| 마스킹 | Response 조립 전 적용 |
| 알 수 없는 필드 | 민감 거래는 거부 우선 |
| 요청 크기 | 경계에서 제한 |
| 정규식 | 과도한 연산 방지 |
| 감사 | 검증 실패·권한 실패 추적 |
| 오류 | 내부 구조 미노출 |

Mass Assignment 위험 예:

{
"customerNo": "C000001",
"status": "APPROVED",
"adminYn": "Y"
}

Request DTO에 허용 필드만 선언하고 Entity에 직접 바인딩하지 않는다.

# 운영·모니터링·장애 대응

## 권장 Metric

validation.failure.count
validation.failure.byServiceId
validation.failure.byField
json.typeMismatch.count
unknownField.count
payloadTooLarge.count

## 운영 확인 질문

어느 ServiceId에서 검증 오류가 증가했는가?

어느 화면·App Version에서 시작됐는가?

특정 필드 오류가 집중되는가?

UI 배포 이후 오류가 증가했는가?

외부 소비자의 계약이 변경됐는가?

공격성 대량 요청인가?

Validation 오류가 급증하면 업무 서버를 재기동하기보다 UI·외부 계약·배포 버전을 먼저 확인한다.

# 자동검증 및 품질 Gate

## 1\. DTO 경계 Gate

금지:

Request DTO를 Mapper Parameter로 직접 사용

Row DTO를 Response로 직접 반환

Persistence 패키지가 Response DTO 참조

Response DTO가 Entity 참조

업무 WAR 간 DTO 직접 Import

## 2\. 타입 Gate

검출 대상:

금액의 double·float
날짜의 무분별한 String
Response의 Object
Optional DTO 필드
Raw List·Map

## 3\. Validation Gate

Request DTO마다 다음 중 하나가 있어야 한다.

Bean Validation Annotation
또는
승인된 명시적 Validator

필수값·길이·형식이 요구사항과 일치하는지 계약테스트로 확인한다.

## 4\. 보안 Gate

password
token
privateKey
residentNo
accountNo

등 민감 필드가 Response DTO·로그 객체에 포함되는지 정적 분석한다.

## 5\. 계층 Gate

ArchUnit 예:

noClasses()
.that()
.resideInAPackage("..persistence..")
.should()
.dependOnClassesThat()
.resideInAPackage("..dto.response..");

noClasses()
.that()
.haveSimpleNameEndingWith("Response")
.should()
.dependOnClassesThat()
.haveSimpleNameEndingWith("Row");

## 6\. 계약 호환성 Gate

검출:

필수 필드 추가
필드 삭제
타입 변경
필드명 변경
Enum 값 삭제
Validation 범위 축소·강화
Null 의미 변경

Validation을 강화하는 것도 계약 변경이다.

기존 maxLength=100
→ 신규 maxLength=20

기존 요청이 실패할 수 있으므로 영향분석이 필요하다.

## 7\. 오류 응답 Gate

GUID 존재
표준 오류코드 존재
안전한 메시지
Stack Trace 없음
SQL 없음
Rejected Value 없음
개인정보 없음

# 테스트 시나리오

| ID | 시나리오 | 기대 결과 |
| --- | --- | --- |
| DTO-001 | 정상 Request 변환 | DTO 생성 |
| DTO-002 | Body null | Validation 오류 |
| DTO-003 | 고객번호 null | 필수 오류 |
| DTO-004 | 고객번호 빈 문자열 | 필수 오류 |
| DTO-005 | 고객번호 공백 | 필수 오류 |
| DTO-006 | 고객번호 최대길이 | 정상 |
| DTO-007 | 고객번호 길이 초과 | 길이 오류 |
| DTO-008 | 고객번호 특수문자 | 형식 오류 |
| DTO-009 | 기준일 정상 | LocalDate 변환 |
| DTO-010 | 존재하지 않는 날짜 | 타입 오류 |
| DTO-011 | 미래 기준일 | 범위 오류 |
| DTO-012 | 기준일 미전송 | 기본일 적용 |
| DTO-013 | includeProducts 미전송 | 기본값 적용 |
| DTO-014 | 알 수 없는 필드 | 정책에 따른 거부·무시 |
| DTO-015 | 잘못된 숫자 타입 | 타입 오류 |
| DTO-016 | 목록 null | 필수 정책 확인 |
| DTO-017 | 빈 목록 | 빈 목록 정책 확인 |
| DTO-018 | 목록 최대건수 | 정상 |
| DTO-019 | 목록 건수 초과 | 크기 오류 |
| DTO-020 | 목록 내부 null | 항목 오류 |
| DTO-021 | 중첩 DTO 오류 | @Valid 동작 |
| DTO-022 | 교차 필드 오류 | Custom Validator·Rule |
| DTO-023 | 고객 미존재 | 업무 오류 |
| DTO-024 | 조회권한 없음 | 권한 오류 |
| DTO-025 | 타 지점 고객 | 데이터권한 오류 |
| DTO-026 | DB 장애 | 시스템 오류 |
| DTO-027 | Row→Response 변환 | 공개 필드만 포함 |
| DTO-028 | 개인정보 마스킹 | 원문 미노출 |
| DTO-029 | 빈 상품 목록 | \[\] 반환 |
| DTO-030 | 금액 타입 | BigDecimal 유지 |
| DTO-031 | 검증 오류 응답 | Field Error 표준 |
| DTO-032 | 검증 오류 로그 | 원문 미기록 |
| DTO-033 | GUID 제공 | 운영 추적 가능 |
| DTO-034 | Stack Trace | 응답 미포함 |
| DTO-035 | 기존 Request | 변경 후 호환 |
| DTO-036 | 신규 선택 필드 | 구 소비자 정상 |
| DTO-037 | 신규 필수 필드 | 비호환 Gate |
| DTO-038 | Validation 강화 | 영향분석 요구 |
| DTO-039 | 대형 Payload | 경계에서 차단 |
| DTO-040 | 반복 오류 | Metric 증가 |
| DTO-041 | 다른 개발자 테스트 | 같은 결과 재현 |
| DTO-042 | Request→Query 변환 | 인증값 안전 반영 |
| DTO-043 | Client userId 위조 | 인증 Context 우선 |
| DTO-044 | Response에 Row 포함 | 구조 Gate 실패 |
| DTO-045 | Map Service 전달 | 구조 Gate 실패 |

# 따라 하는 실무 절차

## 1단계. 요구사항의 입력·출력 표를 확인한다

완료 증적:

필드명
타입
필수
길이
형식
Null 의미
개인정보 여부

## 2단계. DTO 종류를 분리한다

Request
Query·Command
Result·Row
Response

완료 증적:

DTO 관계도

## 3단계. 타입을 확정한다

날짜 → LocalDate·LocalDateTime
금액 → BigDecimal
건수 → Integer·Long
상태 → Enum·Code
목록 → Generic List

## 4단계. Request Validation을 작성한다

필수
길이
형식
범위
목록크기
중첩구조

## 5단계. 공통 Body 변환기를 적용한다

완료 증적:

정상 변환 테스트
타입 오류 테스트
Validation 오류 테스트

## 6단계. 업무 Rule을 분리한다

존재
상태
중복
권한
데이터 범위

Bean Validation과 중복 작성하지 않는다.

## 7단계. Query·Command를 생성한다

검증된 인증 Context와 기본값을 반영한다.

## 8단계. Result를 Response로 변환한다

필드 최소화
코드명 변환
마스킹
불변 목록
기준시각

## 9단계. 오류를 표준화한다

Validation
Business
Authorization
System
Timeout

## 10단계. 정상·경계·실패 테스트를 실행한다

완료 증적:

테스트 결과
표준 응답
GUID 로그

## 11단계. 계약 호환성을 검토한다

기존 UI
다른 WAR Client
외부 시스템
Batch

## 12단계. CI/CD Gate를 통과한다

DTO 의존성
민감정보
Validation
계약 Diff
테스트

# 완료 체크리스트

## DTO 분리

| 확인 항목 | 완료 |
| --- | --- |
| Request DTO가 정의됐다. | □ |
| Response DTO가 정의됐다. | □ |
| Query·Command DTO를 구분했다. | □ |
| Result·Row DTO를 구분했다. | □ |
| DTO와 Domain Object를 구분했다. | □ |
| 하나의 DTO를 전체 계층에서 재사용하지 않는다. | □ |
| Response가 Row·Entity를 직접 포함하지 않는다. | □ |
| Standard Header와 업무 Body가 분리됐다. | □ |

## 타입과 계약

| 확인 항목 | 완료 |
| --- | --- |
| 날짜가 명확한 타입으로 정의됐다. | □ |
| 금액이 BigDecimal로 정의됐다. | □ |
| 선택값의 null 의미가 정의됐다. | □ |
| 목록의 null·빈 목록 의미가 정의됐다. | □ |
| 최대 길이와 건수가 정의됐다. | □ |
| 알 수 없는 필드 정책이 정의됐다. | □ |
| 개인정보 필드가 식별됐다. | □ |
| DTO가 가능한 한 불변이다. | □ |

## Validation

| 확인 항목 | 완료 |
| --- | --- |
| 필수값 검증이 있다. | □ |
| 길이 검증이 있다. | □ |
| 형식 검증이 있다. | □ |
| 범위 검증이 있다. | □ |
| 중첩 DTO에 @Valid가 있다. | □ |
| 목록 항목 검증이 있다. | □ |
| Custom Validator가 DB를 호출하지 않는다. | □ |
| Body Validation이 Transaction 전에 수행된다. | □ |
| UI 검증과 별도로 서버 검증이 있다. | □ |

## 업무 검증

| 확인 항목 | 완료 |
| --- | --- |
| 존재 여부 검증 위치가 정의됐다. | □ |
| 상태 검증 위치가 정의됐다. | □ |
| 중복 검증 위치가 정의됐다. | □ |
| 기능권한과 데이터권한을 구분했다. | □ |
| 인증 Context를 신뢰값으로 사용한다. | □ |
| DAO·Mapper가 업무 메시지를 결정하지 않는다. | □ |
| 영향 행 수를 Service에서 검증한다. | □ |

## 오류·운영

| 확인 항목 | 완료 |
| --- | --- |
| Validation 오류코드가 정의됐다. | □ |
| Field Error 형식이 정의됐다. | □ |
| 사용자·운영 메시지를 분리했다. | □ |
| 응답에 Stack Trace가 없다. | □ |
| 오류에 개인정보 원문이 없다. | □ |
| 로그에 GUID·ServiceId가 있다. | □ |
| Validation Metric을 수집한다. | □ |
| 오류 급증 시 운영 확인절차가 있다. | □ |

## 호환성·품질

| 확인 항목 | 완료 |
| --- | --- |
| 신규 필드의 선택·필수 여부를 검토했다. | □ |
| 필드 삭제·타입 변경 영향이 분석됐다. | □ |
| Validation 강화 영향이 분석됐다. | □ |
| 계약테스트가 있다. | □ |
| DTO 구조 Gate가 있다. | □ |
| 민감정보 정적검사가 있다. | □ |
| 다른 소비자 회귀테스트가 있다. | □ |

# 변경·호환성·폐기 관리

## 선택 필드 추가

기존:

{
"customerNo": "C000001"
}

변경:

{
"customerNo": "C000001",
"baseDate": "2026-07-18"
}

baseDate가 선택 필드이고 기존 기본 동작이 유지되면 일반적으로 하위 호환 가능성이 높다.

## 필수 필드 추가

기존 소비자
→ 신규 필드 미전송
→ Validation 실패

비호환 변경이다.

권장:

선택 필드로 우선 추가
→ 사용량·소비자 전환
→ 신규 Version에서 필수화

## 필드명 변경

customerGrade
→ customerLevel

권장 전환:

신규 필드 추가
→ 기존 필드 병행
→ 소비자 전환
→ 기존 호출량 확인
→ 기존 필드 Deprecated
→ 제거

## 타입 변경

productCount
String → Integer

JSON 표현과 소비자 코드가 달라질 수 있으므로 비호환으로 판단한다.

## Validation 강화

maxLength 100
→ maxLength 20

코드 내부 변경처럼 보여도 기존 요청을 거절할 수 있으므로 계약 변경이다.

## Enum 변경

기존 Enum 값을 삭제하거나 이름을 변경하면 구버전 데이터와 소비자가 실패할 수 있다.

신규 값 추가
→ Unknown 처리정책 확인

기존 값 삭제
→ 대체값·이행 필요

## Response 필드 삭제

필드 삭제 전에 다음을 확인한다.

UI 사용 여부
다른 WAR Client
외부 소비자
Batch·파일
운영 보고서

## DTO 폐기

신규 사용 중지
↓
소비자 조사
↓
대체 DTO·Version 제공
↓
Deprecated
↓
사용량 0 확인
↓
코드·문서·테스트 제거

# 시사점

## 핵심 아키텍처 판단

첫째, DTO는 계층마다 필요한 데이터를 분리하는 계약이다.

외부 입력
≠ DB 조회조건
≠ DB 결과
≠ 외부 응답

둘째, 형식 Validation과 업무 검증은 실패 의미와 책임이 다르다.

값의 모양
→ Bean Validation

업무 허용 여부
→ Service·Rule

셋째, 입력 검증은 보안 통제다.

DTO에 허용된 필드만 선언하고 서버가 인증정보와 내부 상태를 채워야 Mass Assignment와 권한 위조를 방지할 수 있다.

넷째, 오류 메시지는 사용자 행동과 운영 진단을 동시에 고려하되 내부 구조와 개인정보를 노출해서는 안 된다.

다섯째, Validation 조건 변경도 외부 계약 변경이다.

## 주요 위험

| 위험 | 결과 |
| --- | --- |
| DTO 전 계층 공용 | 화면·DB 강결합 |
| Map 중심 처리 | 타입 오류·Key 오타 |
| Row 직접 응답 | 내부정보·개인정보 노출 |
| 문자열 날짜 | 잘못된 날짜 허용 |
| Object 타입 | 런타임 타입 오류 |
| 사용자 인증값 신뢰 | 권한 위·변조 |
| Bean Validator의 DB 조회 | 성능·트랜잭션 문제 |
| 업무 검증 위치 혼재 | 오류 의미 불일치 |
| Validation 후 DB 실행 | 불필요한 자원 사용 |
| 오류 원문 반환 | 내부정보 노출 |
| Rejected Value 기록 | 개인정보 유출 |
| Validation 강화 무관리 | 기존 소비자 장애 |
| 목록 제한 없음 | 메모리·DB 부하 |
| 공통 오류계약 부재 | UI별 예외처리 증가 |
| UI 검증만 의존 | 우회 요청 허용 |

## 우선 보완 과제

1.  spring-boot-starter-validation 적용 여부를 기준화한다.
2.  TransactionBodyConverter를 공통 모듈에 구현한다.
3.  현재 Map → fromMap() 수동 변환을 타입 변환기로 통합한다.
4.  Request DTO에 Bean Validation을 적용한다.
5.  SvCustomerRule의 필수·길이 검증을 경계 Validation으로 이동한다.
6.  Rule에는 존재·상태·권한 등 업무 검증만 유지한다.
7.  Facade가 Map 대신 Typed Request를 받도록 변경한다.
8.  CustomerSummaryRow의 Object 필드를 구체 타입으로 변경한다.
9.  Response DTO에서 Row 직접 참조를 제거한다.
10.  Standard Header와 업무 Response의 중복 필드를 제거한다.
11.  공통 Validation 오류 응답 모델을 정의한다.
12.  검증 오류의 개인정보 로그 방지 정책을 적용한다.
13.  DTO 계약 Diff 검사를 CI/CD에 추가한다.
14.  Validation 조건 변경을 영향도 분석 대상으로 관리한다.
15.  Request·Query·Result·Response 의존성 ArchUnit 검사를 추가한다.

## 중장기 발전 방향

수동 Map 변환
↓
공통 Typed Body 변환
↓
Bean Validation 표준화
↓
Request·Query·Result·Response 계약 분리
↓
Schema·DTO 자동 정합성 검사
↓
계약 Diff 기반 호환성 Gate
↓
테스트 케이스 자동 생성
↓
운영 오류 통계 기반 UX 개선

# 마무리말

DTO와 입력 검증을 설계하는 과정은 다음 질문에 답하는 일이다.

외부에서 무엇을 받을 것인가?

어떤 값만 허용할 것인가?

어떤 타입과 형식을 사용할 것인가?

클라이언트가 보내면 안 되는 값은 무엇인가?

어떤 값은 인증 Context에서 가져올 것인가?

DB 조회조건은 어떻게 분리할 것인가?

DB 결과 중 무엇을 외부에 공개할 것인가?

개인정보는 어디에서 마스킹할 것인가?

형식 오류와 업무 오류를 어떻게 구분할 것인가?

사용자에게 어떤 메시지를 보여 줄 것인가?

운영자는 어떤 GUID와 오류코드로 추적할 것인가?

계약이 바뀌어도 기존 소비자가 계속 동작하는가?

제14장의 핵심 흐름은 다음과 같다.

외부 JSON
↓
Request DTO
↓
형식 Validation
↓
Query·Command
↓
업무 Rule
↓
Result DTO
↓
Response DTO
↓
안전한 표준 응답

가장 중요한 원칙은 다음과 같다.

DTO에 값을 담았다고
입력이 검증된 것은 아니다.

Annotation을 통과했다고
업무적으로 허용된 것도 아니다.

형식 계약과 업무 규칙을
각자의 책임 위치에서 검증해야 한다.

다음 장에서는 검증된 Request DTO를 Service와 Repository·DAO·Mapper에 연결하고, 조회 결과 없음·데이터 소유권·트랜잭션 경계를 고려하여 실제 데이터 처리 프로그램을 구현한다.
