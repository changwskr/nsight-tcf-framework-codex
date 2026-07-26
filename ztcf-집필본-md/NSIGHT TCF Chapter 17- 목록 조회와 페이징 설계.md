<!-- source: ztcf-집필본/NSIGHT TCF Chapter 17- 목록 조회와 페이징 설계.docx -->
<!-- tables: mammoth-html + placeholder table-to-md -->

# 제17장. 목록 조회와 페이징

## 이 장을 시작하며

제16장에서는 처음 구현한 단건 조회 거래를 ServiceId에 등록하고, 로컬 환경에서 TCF 전체 흐름을 호출해 응답·로그·SQL·DB 결과를 검증했다.

이제 한 건이 아니라 여러 건을 조회하는 목록 거래를 구현한다.

초보 개발자는 목록 조회를 다음처럼 생각하기 쉽다.

검색조건을 받는다.
→ SELECT SQL을 실행한다.
→ List로 반환한다.

데이터가 적고 사용자가 한 명이라면 이 방식도 동작한다.

그러나 운영 데이터가 수백만 건으로 증가하고 동시에 여러 사용자가 조회하면 단순한 목록 조회가 다음 문제를 일으킬 수 있다.

조건 없는 전체 조회

과도한 pageSize

깊은 Offset 페이지

불안정한 정렬

Count SQL의 장시간 실행

검색조건과 맞지 않는 Index

개인정보 대량 조회

대형 결과의 Heap 적재

DB Connection 장기 점유

동일 조건 반복 조회

엑셀 다운로드를 온라인 조회로 처리

목록 조회는 단순한 List 반환 기능이 아니다.

어떤 조건을 필수로 받을 것인가?

검색기간은 최대 며칠인가?

정렬순서는 항상 같은가?

한 페이지는 최대 몇 건인가?

전체 건수가 반드시 필요한가?

Offset 페이징이 적합한가?

다음 페이지 Cursor 방식이 적합한가?

데이터가 조회 중 변경되면 어떻게 보이는가?

사용자가 조회할 수 있는 데이터 범위는 어디까지인가?

대량 조회는 어떤 별도 경로로 처리할 것인가?

이 질문을 먼저 결정해야 한다.

NSIGHT TCF에서 페이징은 TCF Core가 공통 계산하는 기능이 아니다.

TCF Core
→ 표준 Header
→ 인증·권한
→ 거래통제
→ Timeout
→ 로그·표준 응답

업무 WAR
→ 검색조건 검증
→ 정렬정책
→ pageNo·pageSize 검증
→ Offset·Cursor 계산
→ Count·List SQL
→ 페이징 응답 조립

프로젝트의 기본 목록 조회 패턴은 Count SQL + List SQL을 분리하고, 업무 WAR가 페이지 정보를 계산하는 방식이다.

## 핵심 관점

페이징은 화면에 페이지 번호를 보여 주는 기능이 아니다.

제한된 시간과 자원 안에서
일관되고 재현 가능한 순서로
허용된 데이터만 나누어 조회하는
성능·정합성·보안 통제다.

## 학습 목표

이 장을 마치면 다음 내용을 설명하고 구현할 수 있어야 한다.

| 번호 | 학습 목표 |
| --- | --- |
| 1 | 목록 조회와 단건 조회의 차이를 설명한다. |
| 2 | 검색조건의 필수·선택·기본값을 정의한다. |
| 3 | 검색기간과 최대 조회범위를 제한한다. |
| 4 | 사용자 입력 정렬값을 안전하게 처리한다. |
| 5 | 안정된 정렬키가 필요한 이유를 설명한다. |
| 6 | Offset 페이징의 동작 원리를 설명한다. |
| 7 | Keyset·Cursor 페이징의 동작 원리를 설명한다. |
| 8 | 페이지 번호형과 더 보기형 UI에 맞는 방식을 선택한다. |
| 9 | Count SQL과 List SQL을 분리한다. |
| 10 | 두 SQL의 검색조건 정합성을 유지한다. |
| 11 | pageNo, pageSize, offset을 안전하게 계산한다. |
| 12 | totalCount, totalPages, hasNext를 계산한다. |
| 13 | 목록 0건을 정상 결과로 반환한다. |
| 14 | 깊은 Offset의 성능 문제를 설명한다. |
| 15 | 조건 없는 대량 조회를 차단한다. |
| 16 | 온라인 조회와 대량 다운로드를 분리한다. |
| 17 | MyBatis 동적 SQL을 안전하게 작성한다. |
| 18 | ${}와 #{}의 차이를 설명한다. |
| 19 | 빈 IN 조건과 LIKE 특수문자를 처리한다. |
| 20 | 검색조건과 Index의 관계를 설명한다. |
| 21 | 개인정보 목록 조회의 권한·감사 기준을 적용한다. |
| 22 | 목록 거래의 로그·Metric·Slow SQL을 확인한다. |
| 23 | 경계·대량·동시 변경 테스트를 작성한다. |
| 24 | 페이징 계약 변경의 호환성을 판단한다. |

# 한눈에 보는 목록 조회 흐름

┌────────────────────────────────────────────────────────────┐
│ 1. 화면 요청 │
│ 검색조건 + 정렬 + pageNo/pageSize │
└─────────────────────────┬──────────────────────────────────┘
▼
┌────────────────────────────────────────────────────────────┐
│ 2. Request DTO │
│ 필수값·형식·범위 검증 │
└─────────────────────────┬──────────────────────────────────┘
▼
┌────────────────────────────────────────────────────────────┐
│ 3. Service·Rule │
│ 기본값·최대범위·권한·정렬 허용목록 │
└─────────────────────────┬──────────────────────────────────┘
▼
┌────────────────────────────────────────────────────────────┐
│ 4. Search Query │
│ 검색조건 + 권한범위 + Offset·Cursor │
└─────────────────────────┬──────────────────────────────────┘
▼
┌────────────────────────────────────────────────────────────┐
│ 5. Count Mapper │
│ 전체 건수 │
└─────────────────────────┬──────────────────────────────────┘
▼
┌────────────────────────────────────────────────────────────┐
│ 6. List Mapper │
│ 정렬 + 페이지 범위 │
└─────────────────────────┬──────────────────────────────────┘
▼
┌────────────────────────────────────────────────────────────┐
│ 7. Service │
│ totalPages·hasNext 계산·마스킹 │
└─────────────────────────┬──────────────────────────────────┘
▼
┌────────────────────────────────────────────────────────────┐
│ 8. Page Response │
│ items + page metadata │
└────────────────────────────────────────────────────────────┘

# 현재 구현과 목표 구조

## 현재 소스에서 확인되는 사례

현재 ep-service의 사용자 이벤트 조회는 다음 구조를 사용한다.

UserEventInquiryRequest
↓
EpUserEventRule
├─ 기본 pageNo = 1
├─ 기본 pageSize = 100
├─ 최대 pageSize 제한
└─ offset 계산
↓
UserEventSearchCriteria
↓
EpUserEventDao
├─ searchReceivedEvents()
└─ countReceivedEvents()
↓
EpUserEventMapper.xml
├─ 동적 WHERE
├─ ORDER BY RECEIVED\_AT DESC
└─ OFFSET ... FETCH NEXT ...
↓
UserEventInquiryResponse
├─ list
├─ pageNo
├─ pageSize
├─ totalCount
└─ totalPage

OM 모듈의 여러 Mapper에서도 Oracle 형식의 다음 페이징이 사용된다.

OFFSET #{offset} ROWS
FETCH NEXT #{pageSize} ROWS ONLY

## 현재 구현 판단

| 항목 | 상태 | 판단 |
| --- | --- | --- |
| 업무 WAR 자체 페이징 | 구현 확인 | 프로젝트 기준과 일치 |
| 기본 pageNo·pageSize | 구현 확인 | 거래별 기준 통일 필요 |
| 최대 pageSize | 구현 확인 | 거래 유형별 상한 관리 필요 |
| Offset 계산 | 구현 확인 | 정수 Overflow 방어 보완 |
| Count·List SQL 분리 | 구현 확인 | 양호 |
| 공통 WHERE Fragment | 구현 확인 | Count/List 정합에 유리 |
| Oracle Offset/FETCH | 구현 확인 | 일반 화면 조회에 적합 |
| 목록 0건 | 구현 확인 | 정상 빈 목록 기준 |
| 정렬 유일키 | 부분 구현 | 보조 유일키 추가 필요 |
| Keyset 페이징 | 확인되지 않음 | 대용량 거래 권장 확장 |
| 정렬값 허용목록 | 보완 필요 | SQL Injection 방지 |
| LIKE Escape | 보완 필요 | %, \_ 처리 필요 |
| Count 생략형 응답 | 권장 확장 | 더 보기형 UI에 유용 |
| 대량 다운로드 분리 | 설계 기준 | 온라인 List와 분리 필요 |
| 구조화 Paging 로그 | 보완 필요 | Offset·건수·처리시간 기록 |

# 표준 페이징 데이터 구조

## Request

{
"header": {
"businessCode": "SV",
"serviceId": "SV.Customer.selectList",
"transactionCode": "SV-INQ-0002"
},
"body": {
"searchCondition": {
"customerName": "홍",
"gradeCodes": \["VIP", "GOLD"\],
"fromDate": "2026-07-01",
"toDate": "2026-07-18"
},
"sort": {
"sortBy": "customerName",
"sortDirection": "ASC"
},
"page": {
"pageNo": 1,
"pageSize": 100
}
}
}

## Response

{
"header": {
"serviceId": "SV.Customer.selectList",
"guid": "G-20260718-000101"
},
"result": {
"resultStatus": "SUCCESS",
"resultCode": "0000"
},
"body": {
"items": \[
{
"customerNo": "C0\*\*\*\*01",
"customerName": "홍\*동",
"customerGrade": "VIP"
}
\],
"page": {
"pageNo": 1,
"pageSize": 100,
"totalCount": 251,
"totalPages": 3,
"hasPrevious": false,
"hasNext": true
}
}
}

# 17.1 검색 조건과 정렬 기준

## 17.1.1 목록 조회의 검색조건

목록 조회 Request는 일반적으로 세 영역으로 분리한다.

검색조건

정렬조건

페이징조건

예:

public record CustomerListRequest(

@Valid
CustomerSearchCondition searchCondition,

@Valid
SortRequest sort,

@NotNull
@Valid
PageRequest page
) {}

## 17.1.2 검색조건 DTO

public record CustomerSearchCondition(

@Size(max = 50)
String customerName,

@Size(max = 20)
String customerNo,

@Size(max = 10)
List<@NotBlank String> gradeCodes,

@PastOrPresent
LocalDate fromDate,

@PastOrPresent
LocalDate toDate,

CustomerStatus status
) {}

검색조건은 화면 컴포넌트가 아니라 업무 조회조건을 표현한다.

금지:

txtCustomerName

cboGrade

gridPageNo

btnSearchYn

권장:

customerName

gradeCodes

fromDate

toDate

status

## 17.1.3 필수 검색조건

모든 검색조건을 선택값으로 만들면 조건 없는 전체 조회가 가능해질 수 있다.

다음 중 하나를 프로젝트 정책으로 정한다.

### 특정 조건 필수

고객번호

지점

업무일자

상태

조회기간

### 조건 그룹 중 하나 필수

고객번호
또는
고객명 + 생년월일
또는
지점 + 조회기간

### 기본 기간 자동 적용

기간 미입력
→ 최근 7일

기본값을 적용했다면 응답이나 로그에서 실제 적용된 조건을 확인할 수 있어야 한다.

## 17.1.4 조건 없는 전체 조회 차단

Rule 예:

public void validateSearchCondition(
CustomerSearchCondition condition) {

boolean noCondition =
condition == null
|| (
!StringUtils.hasText(condition.customerName())
&& !StringUtils.hasText(condition.customerNo())
&& CollectionUtils.isEmpty(condition.gradeCodes())
&& condition.fromDate() == null
&& condition.toDate() == null
&& condition.status() == null
);

if (noCondition) {
throw new BusinessException(
"E-SV-SRCH-0001",
"하나 이상의 검색조건을 입력해 주세요."
);
}
}

단, 최근 등록순 20건과 같이 조건 없는 조회가 승인된 거래는 별도의 ServiceId와 최대 건수를 사용한다.

## 17.1.5 검색기간 검증

public void validatePeriod(
LocalDate fromDate,
LocalDate toDate,
LocalDate today) {

if (fromDate == null || toDate == null) {
throw new BusinessException(
"E-SV-SRCH-0002",
"조회기간을 입력해 주세요."
);
}

if (fromDate.isAfter(toDate)) {
throw new BusinessException(
"E-SV-SRCH-0003",
"조회 시작일은 종료일보다 늦을 수 없습니다."
);
}

long days =
ChronoUnit.DAYS.between(
fromDate,
toDate
) + 1;

if (days > 31) {
throw new BusinessException(
"E-SV-SRCH-0004",
"조회기간은 최대 31일입니다."
);
}
}

최대 기간은 데이터량·Index·업무 필요·성능시험 결과로 결정한다.

## 17.1.6 개인정보 조회조건

고객명·전화번호·계좌번호 등의 검색은 다음을 검토한다.

검색권한

부분검색 허용 여부

최소 입력길이

원문 로그 금지

반복검색 탐지

결과 마스킹

감사로그

예:

고객명 한 글자 검색
→ 결과가 지나치게 많음

전화번호 뒷자리 두 글자 검색
→ 개인정보 탐색 위험

최소 검색길이와 추가 조건을 요구할 수 있다.

## 17.1.7 검색조건 정규화

| 필드 | 정규화 예 |
| --- | --- |
| 고객번호 | 앞뒤 공백 제거 |
| 코드 | 대문자 변환 가능 |
| 고객명 | 내부 공백 보존 |
| 전화번호 | 승인된 형식으로 정규화 |
| 날짜 | 업무 기준일로 변환 |
| 목록 코드 | 중복 제거 |
| 빈 문자열 | 계약에 따라 null 변환 |

정규화한 값은 Query 객체에 담고 Request 원본은 변경하지 않는 것이 좋다.

## 17.1.8 정렬이 반드시 필요한 이유

관계형 DB는 ORDER BY가 없으면 행의 반환순서를 보장하지 않는다.

금지:

SELECT CUSTOMER\_NO,
CUSTOMER\_NAME
FROM SV\_CUSTOMER
WHERE STATUS\_CODE = 'ACTIVE'
OFFSET #{offset} ROWS
FETCH NEXT #{pageSize} ROWS ONLY

동일 요청이라도 실행계획·통계·병렬처리·데이터 변경에 따라 다른 행이 반환될 수 있다.

## 17.1.9 안정된 정렬키

불안정한 정렬:

ORDER BY CUSTOMER\_NAME

동명이인이 여러 명이면 같은 이름 안의 순서가 정해지지 않는다.

권장:

ORDER BY CUSTOMER\_NAME ASC,
CUSTOMER\_NO ASC

기준:

업무 정렬키
\+ 유일한 보조 정렬키

예:

| 업무 정렬 | 보조 정렬 |
| --- | --- |
| 고객명 | 고객번호 |
| 등록일시 | 등록 Sequence |
| 잔액 | 고객번호 |
| 상태 | 업무 PK |
| 발생시각 | 이벤트 ID |

## 17.1.10 Null 정렬

DB와 정렬 방향에 따라 Null 위치가 달라질 수 있다.

명시:

ORDER BY LAST\_CONTACT\_DATE DESC NULLS LAST,
CUSTOMER\_NO ASC

Null 정렬도 외부 응답 계약의 일부다.

## 17.1.11 사용자 정렬조건

Request:

public record SortRequest(
String sortBy,
SortDirection sortDirection
) {}

사용자가 보낸 sortBy를 SQL에 그대로 사용하면 안 된다.

금지:

ORDER BY ${sortBy} ${sortDirection}

권장:

public enum CustomerSort {
CUSTOMER\_NAME,
CUSTOMER\_GRADE,
REGISTERED\_AT
}

Service가 허용된 DB 정렬 표현으로 변환한다.

## 17.1.12 MyBatis 안전한 정렬

<choose>
<when test="sortBy == 'CUSTOMER\_NAME'">
ORDER BY CUSTOMER\_NAME
</when>
<when test="sortBy == 'CUSTOMER\_GRADE'">
ORDER BY CUSTOMER\_GRADE
</when>
<when test="sortBy == 'REGISTERED\_AT'">
ORDER BY REGISTERED\_AT
</when>
<otherwise>
ORDER BY CUSTOMER\_NO
</otherwise>
</choose>

<choose>
<when test="sortDirection == 'DESC'">
DESC
</when>
<otherwise>
ASC
</otherwise>
</choose>

, CUSTOMER\_NO ASC

더 단순하게 정렬 조합 전체를 Enum별 SQL Fragment로 선택할 수도 있다.

## 17.1.13 정렬과 Index

조회 SQL:

WHERE BRANCH\_CODE = #{branchCode}
AND STATUS\_CODE = #{statusCode}
ORDER BY REGISTERED\_AT DESC,
CUSTOMER\_NO DESC

Index 후보:

BRANCH\_CODE
\+ STATUS\_CODE
\+ REGISTERED\_AT
\+ CUSTOMER\_NO

Index는 무조건 SQL 컬럼 순서대로 만드는 것이 아니라 선택도·조회패턴·DML 비용·실행계획을 검토해 DBA와 확정한다.

## 17.1.14 함수 사용과 Index

주의:

WHERE TO\_CHAR(REGISTERED\_AT, 'YYYYMMDD')
BETWEEN #{fromDate} AND #{toDate}

Index 사용이 불리할 수 있다.

권장:

WHERE REGISTERED\_AT >= #{fromDateTime}
AND REGISTERED\_AT < #{toDateTimeExclusive}

컬럼을 변환하기보다 검색값을 컬럼 타입에 맞춘다.

# 17.2 페이징 방식 선택

## 17.2.1 페이징이 필요한 이유

DB 전체 결과
→ 필요한 페이지 범위만 조회

애플리케이션 전체 적재
→ 금지

DB에서 페이지 제한
→ 권장

다음 구조를 사용하지 않는다.

List<Customer> all =
mapper.selectAllCustomers();

int from = (pageNo - 1) \* pageSize;
int to = Math.min(
from + pageSize,
all.size()
);

return all.subList(from, to);

DB·네트워크·Heap 자원을 이미 전체 데이터만큼 사용한 뒤 잘라내는 방식이기 때문이다.

대량 조회 결과를 전체 List로 메모리에 적재하지 않는 것은 프로젝트의 본 개발 품질 기준에도 포함된다.

## 17.2.2 주요 페이징 방식

| 방식 | 특징 | 적합 |
| --- | --- | --- |
| Offset | 페이지 번호·건수 기반 | 일반 업무 화면 |
| Keyset | 마지막 정렬키 이후 조회 | 대량·깊은 페이지 |
| Cursor | 불투명 Token 기반 | 외부 API·무한 스크롤 |
| Top-N | 첫 N건만 반환 | 최근 내역·대시보드 |
| Streaming | 순차 전송 | 파일·대량 처리 |
| Batch Chunk | 일정 단위 반복 처리 | 배치·이관 |

## 17.2.3 프로젝트 기본 방식

일반 화면 조회의 기본은 Offset 페이징이다.

pageNo ≥ 1

1 ≤ pageSize ≤ 최대값

offset = (pageNo - 1) × pageSize

예:

pageNo = 3

pageSize = 100

offset = 200

SQL:

OFFSET 200 ROWS
FETCH NEXT 100 ROWS ONLY

## 17.2.4 Page Request

public record PageRequest(

@NotNull
@Min(1)
Integer pageNo,

@NotNull
@Min(1)
@Max(500)
Integer pageSize
) {

public long offset() {
return Math.multiplyExact(
(long) pageNo - 1L,
(long) pageSize
);
}
}

int 곱셈은 매우 큰 pageNo에서 Overflow가 발생할 수 있으므로 long 계산을 사용한다.

## 17.2.5 기본값 적용

public record PageRequest(
Integer pageNo,
Integer pageSize
) {

public NormalizedPage normalize(
int defaultPageSize,
int maxPageSize) {

int normalizedPageNo =
pageNo == null ? 1 : pageNo;

int normalizedPageSize =
pageSize == null
? defaultPageSize
: pageSize;

if (normalizedPageNo < 1) {
throw PagingException.invalidPageNo();
}

if (normalizedPageSize < 1
|| normalizedPageSize > maxPageSize) {
throw PagingException.invalidPageSize(
maxPageSize
);
}

return NormalizedPage.of(
normalizedPageNo,
normalizedPageSize
);
}
}

잘못된 pageSize를 조용히 최대값으로 보정할지, 오류로 반환할지는 계약으로 정한다.

권장:

미입력
→ 기본값

유효범위 초과
→ 입력 오류

조용한 보정은 호출자의 오류를 숨길 수 있다.

## 17.2.6 Count SQL

<select
id="countCustomerList"
parameterType="CustomerListQuery"
resultType="long">

SELECT COUNT(1)
FROM SV\_CUSTOMER C
<include refid="customerSearchWhere"/>

</select>

건수는 int 범위를 초과할 수 있으므로 범용 표준에서는 long 사용을 검토한다.

## 17.2.7 List SQL

<select
id="selectCustomerList"
parameterType="CustomerListQuery"
resultMap="customerListItemMap"
timeout="2">

SELECT C.CUSTOMER\_NO
, C.CUSTOMER\_NAME
, C.CUSTOMER\_GRADE
, C.REGISTERED\_AT
FROM SV\_CUSTOMER C
<include refid="customerSearchWhere"/>
ORDER BY C.CUSTOMER\_NAME ASC,
C.CUSTOMER\_NO ASC
OFFSET #{offset} ROWS
FETCH NEXT #{pageSize} ROWS ONLY

</select>

## 17.2.8 Count와 List의 WHERE 정합성

금지:

Count SQL
→ 상태조건 없음

List SQL
→ ACTIVE 상태만 조회

결과:

totalCount = 1,000

실제 목록 = 700건 기준

공통 Fragment:

<sql id="customerSearchWhere">
<where>
<if test="branchCode != null">
AND C.BRANCH\_CODE = #{branchCode}
</if>

<if test="statusCode != null">
AND C.STATUS\_CODE = #{statusCode}
</if>

<if test="fromDateTime != null">
AND C.REGISTERED\_AT
<!\[CDATA\[ >= \]\]>
#{fromDateTime}
</if>

<if test="toDateTimeExclusive != null">
AND C.REGISTERED\_AT
<!\[CDATA\[ < \]\]>
#{toDateTimeExclusive}
</if>
</where>
</sql>

Count와 List에서 같은 Fragment를 사용한다.

## 17.2.9 Service 구현

@Service
@RequiredArgsConstructor
public class CustomerListService {

private final CustomerDao customerDao;
private final CustomerSearchRule searchRule;
private final CustomerListAssembler assembler;

public CustomerListResponse selectList(
CustomerListRequest request,
TransactionContext context) {

CustomerListQuery query =
searchRule.toQuery(
request,
context.getAuthenticationContext()
);

long totalCount =
customerDao.countCustomerList(query);

if (totalCount == 0L) {
return CustomerListResponse.empty(
query.page()
);
}

List<CustomerListRow> rows =
customerDao.selectCustomerList(query);

return assembler.toResponse(
rows,
query.page(),
totalCount,
context.getAuthenticationContext()
);
}
}

## 17.2.10 Page Response 계산

public record PageMetadata(
int pageNo,
int pageSize,
long totalCount,
long totalPages,
boolean hasPrevious,
boolean hasNext
) {

public static PageMetadata of(
int pageNo,
int pageSize,
long totalCount) {

long totalPages =
totalCount == 0
? 0
: (totalCount + pageSize - 1L)
/ pageSize;

return new PageMetadata(
pageNo,
pageSize,
totalCount,
totalPages,
pageNo > 1,
pageNo < totalPages
);
}
}

## 17.2.11 요청 페이지가 마지막 페이지보다 큰 경우

예:

totalPages = 3

요청 pageNo = 10

정책 대안:

| 대안 | 결과 |
| --- | --- |
| 빈 목록 | 일반적으로 권장 |
| 마지막 페이지 반환 | 요청 의미 변경 가능 |
| 입력 오류 | 엄격한 화면 계약 |
| 첫 페이지 반환 | 금지 권장 |

기본 권장:

{
"items": \[\],
"page": {
"pageNo": 10,
"pageSize": 100,
"totalCount": 251,
"totalPages": 3,
"hasNext": false
}
}

요청을 임의로 마지막 페이지로 바꾸지 않는다.

## 17.2.12 Count SQL을 항상 실행해야 하는가

페이지 번호 UI:

1 2 3 4 5
총 2,351건

전체 건수가 필요하다.

더 보기·무한 스크롤 UI:

다음 100건

전체 Count가 필요하지 않을 수 있다.

Count가 매우 비싼 경우 다음 응답을 사용할 수 있다.

{
"items": \[\],
"page": {
"pageSize": 100,
"hasNext": true,
"nextCursor": "..."
}
}

전체 Count가 필요하지 않은 거래에서 습관적으로 COUNT(\*)를 실행하지 않는다.

## 17.2.13 pageSize + 1 조회

hasNext만 필요한 경우:

FETCH NEXT #{fetchSize} ROWS ONLY

fetchSize
\= pageSize + 1

Service:

조회 결과가 101건
→ 100건 반환
→ hasNext = true

Count SQL 없이 다음 페이지 존재 여부를 판단할 수 있다.

## 17.2.14 Offset 페이징의 한계

깊은 페이지:

pageNo = 10,000

pageSize = 100

offset = 999,900

DB는 앞의 많은 행을 찾고 건너뛰어야 할 수 있다.

문제:

깊은 페이지에서 응답시간 증가

CPU·I/O 증가

동시 변경 시 중복·누락

Count SQL 비용

일반 화면이 수백 페이지까지 이동하지 않는다면 Offset이 단순하고 적절하다.

깊은 페이지가 실제 요구라면 Keyset을 검토한다.

## 17.2.15 Keyset 페이징

정렬:

ORDER BY REGISTERED\_AT DESC,
CUSTOMER\_NO DESC

첫 페이지:

SELECT ...
FROM SV\_CUSTOMER
WHERE ...
ORDER BY REGISTERED\_AT DESC,
CUSTOMER\_NO DESC
FETCH FIRST #{pageSize} ROWS ONLY

다음 페이지:

SELECT ...
FROM SV\_CUSTOMER
WHERE ...
AND (
REGISTERED\_AT < #{lastRegisteredAt}
OR (
REGISTERED\_AT = #{lastRegisteredAt}
AND CUSTOMER\_NO < #{lastCustomerNo}
)
)
ORDER BY REGISTERED\_AT DESC,
CUSTOMER\_NO DESC
FETCH FIRST #{pageSize} ROWS ONLY

장점:

깊은 페이지 성능 안정

앞 행 Skip 비용 감소

새 데이터 삽입의 영향 감소

제약:

특정 페이지 번호로 즉시 이동 어려움

정렬키 변경 시 Cursor 변경

복합 정렬조건 처리 필요

Cursor 위·변조 방지 필요

## 17.2.16 Cursor 설계

Cursor 원문:

{
"registeredAt": "2026-07-18T09:00:00+09:00",
"customerNo": "C000100"
}

외부에는 불투명 Token으로 제공할 수 있다.

Base64URL
\+ 서명 또는 MAC
\+ 만료시각
\+ 정렬조건 Hash
\+ 검색조건 Hash

Cursor에 개인정보 원문을 그대로 노출하지 않는다.

## 17.2.17 페이징 중 데이터 변경

Offset 방식에서 첫 페이지 조회 후 신규 행이 앞에 추가되면 두 번째 페이지에서 기존 마지막 행이 다시 나타날 수 있다.

1페이지 조회
→ 신규 데이터 삽입
→ 2페이지 조회
→ 중복·누락 가능

대안:

1.  일정 수준의 중복·누락을 허용한다.
2.  조회 기준시각을 고정한다.
3.  Keyset 페이징을 사용한다.
4.  Snapshot·임시 결과 집합을 사용한다.
5.  대량 보고서는 비동기 파일로 생성한다.

일반 화면 목록과 회계·감사 보고서의 정합성 요구는 다르다.

# 17.3 대량 조회 방지

## 17.3.1 대량 조회는 단순 성능 문제가 아니다

대량 조회는 다음 문제로 확장된다.

DB 부하

Connection Pool 고갈

Tomcat Thread 대기

Heap 증가

네트워크 대역폭 사용

로그 증가

개인정보 대량 노출

다른 업무 거래 지연

따라서 대량 조회 방지는 성능·보안·운영 통제다.

## 17.3.2 다층 제한

화면
→ 최대 조회기간 안내

Request Validation
→ pageSize·기간 범위 검증

Service·Rule
→ 조건 없는 조회 차단

SQL
→ 페이지·상한 적용

TCF
→ 거래 Timeout

Gateway·Apache
→ 요청·응답 크기 제한

OM
→ 거래량·권한·통제

운영
→ Slow SQL·대량조회 탐지

화면 제한만으로 서버를 보호하지 않는다.

## 17.3.3 권장 제한 항목

| 항목 | 예시 기준 |
| --- | --- |
| 기본 pageNo | 1 |
| 기본 pageSize | 100 |
| 최대 pageSize | 500 |
| 일반 조회기간 | 31일 |
| 상세 개인정보 목록 | 100건 |
| 최근 이력 | 1,000건 이내 |
| 온라인 응답 크기 | 거래별 확정 |
| 전체 Timeout | 3초 |
| Query Timeout | 2초 |

수치는 거래별 데이터량과 성능시험을 거쳐 확정한다.

## 17.3.4 Hard Limit

검색조건에 관계없이 온라인 거래가 반환할 수 있는 최대 누적 건수를 설정할 수 있다.

최대 탐색 가능 건수
\= 10,000건

그 이상
→ 조건을 좁히거나 다운로드 거래 사용

예:

if (totalCount > ONLINE\_MAX\_RESULT\_COUNT) {
throw new BusinessException(
"E-SV-SRCH-0010",
"조회 결과가 너무 많습니다. 검색조건을 추가해 주세요."
);
}

단, totalCount 자체가 비싼 경우 별도 상한 탐지 SQL을 사용할 수 있다.

## 17.3.5 Count 상한 탐지

전체 Count 대신 최대값을 초과하는지만 확인할 수 있다.

개념:

SELECT COUNT(1)
FROM (
SELECT 1
FROM SV\_CUSTOMER
WHERE ...
FETCH FIRST 10001 ROWS ONLY
)

결과가 10,001이면 “10,000건 초과”로 판단한다.

정확한 전체 Count가 필요하지 않은 거래에서 유용하다.

## 17.3.6 온라인 조회와 다운로드 분리

금지:

화면 pageSize = 100,000

한 번에 조회

메모리에서 Excel 생성

HTTP 응답으로 즉시 반환

권장:

화면 목록
→ 최대 100·500건

전체 다운로드 요청
→ 권한·감사 확인
→ 비동기 Job 등록
→ Chunk 조회
→ 파일 생성
→ 완료 알림
→ 제한시간 내 다운로드

대량 다운로드는 별도의 ServiceId·거래코드·감사정책을 사용한다.

예:

SV.Customer.selectList
→ 화면 목록

SV.Customer.requestExport
→ 비동기 다운로드 요청

SV.Customer.downloadExport
→ 생성 파일 다운로드

## 17.3.7 목록과 Excel SQL의 관계

다운로드가 화면과 같은 조건을 사용하더라도 실행방식은 다를 수 있다.

검색조건
→ 동일

정렬
→ 동일 또는 명시적 보고서 정렬

페이징
→ 화면 Offset이 아니라 Chunk·Streaming

Timeout
→ 온라인보다 별도 정책

감사
→ 필수

## 17.3.8 Rate Limit과 반복 조회

한 사용자가 첫 페이지를 초당 수십 회 반복 호출하면 개별 요청이 작더라도 DB에 부담이 된다.

관측:

userId

branchId

ServiceId

searchConditionHash

pageNo

호출빈도

필요 시 다음을 적용한다.

UI 중복 클릭 방지

동일 요청 짧은 Cache

Gateway Rate Limit

ServiceId별 TPS 통제

비정상 반복 감사

## 17.3.9 Timeout 후 작업 종료

TCF가 Timeout 응답을 반환했는데 SQL이 계속 실행되면 자원이 남는다.

확인:

Query Timeout

Statement Cancel

Transaction 종료

Connection 반환

Executor Task 취소

Thread Context 제거

Timeout은 사용자 응답만 빨리 보내는 기능이 아니다.

하위 작업이 실제로 정리돼야 한다.

## 17.3.10 Count SQL의 부하

Count SQL은 목록 Row를 반환하지 않지만 대규모 Join·동적 조건에서 List보다 느릴 수 있다.

확인:

불필요한 Join 제거 가능 여부

COUNT DISTINCT 필요 여부

권한조건 비용

통계정보

Index

Partition Pruning

정확한 Count 필요 여부

Count와 List를 단순히 같은 SELECT에서 컬럼만 COUNT(\*)로 바꾸면 최적이지 않을 수 있다.

검색조건 의미는 같게 유지하되 Count에 필요 없는 조인은 제거할 수 있다.

## 17.3.11 대량 결과의 메모리 처리

금지:

List<CustomerRow> rows =
mapper.selectAll(query);

byte\[\] excel =
excelGenerator.create(rows);

권장:

Cursor·Chunk Reader

일정 건수 단위 처리

Streaming Writer

진행상태 저장

실패 재시작

입력·출력 건수 대사

# 17.4 동적 SQL의 주의점

## 17.4.1 동적 SQL이 필요한 이유

검색조건이 선택값이면 SQL의 WHERE가 달라진다.

고객명만 입력

지점만 입력

기간만 입력

상태 + 기간 입력

고객번호 + 상태 입력

MyBatis는 <if>, <where>, <choose>, <foreach>로 동적 SQL을 구성할 수 있다.

동적 SQL은 편리하지만 실행 가능한 SQL 조합이 급격히 증가한다.

## 17.4.2 <where> 사용

금지:

WHERE 1 = 1
<if test="customerName != null">
AND CUSTOMER\_NAME = #{customerName}
</if>

WHERE 1=1 자체가 반드시 잘못은 아니지만, 공통 Fragment와 조합 과정에서 조건 누락을 숨길 수 있다.

권장:

<where>
<if test="customerName != null and customerName != ''">
AND CUSTOMER\_NAME = #{customerName}
</if>

<if test="statusCode != null">
AND STATUS\_CODE = #{statusCode}
</if>
</where>

## 17.4.3 ${}와 #{}

#{customerNo}

Prepared Statement Parameter Binding을 사용한다.

${customerNo}

SQL 문자열에 직접 삽입한다.

사용자 입력에 ${}를 사용하면 SQL Injection 위험이 있다.

금지:

AND CUSTOMER\_NAME LIKE '%${customerName}%'

권장:

AND CUSTOMER\_NAME LIKE
'%' || #{customerName} || '%'

## 17.4.4 LIKE 특수문자

사용자가 % 또는 \_를 입력하면 Wildcard로 처리될 수 있다.

%
→ 임의 길이 문자열

\_
→ 임의 한 글자

Literal 검색이 필요하면 Escape한다.

WHERE CUSTOMER\_NAME LIKE #{escapedName}
ESCAPE '\\'

Service:

String escapeLike(String value) {
return value
.replace("\\\\", "\\\\\\\\")
.replace("%", "\\\\%")
.replace("\_", "\\\\\_");
}

검색정책에 따라 부분검색 Wildcard를 의도적으로 허용할 수도 있지만 명시해야 한다.

## 17.4.5 IN 조건

<if test="gradeCodes != null and !gradeCodes.isEmpty()">
AND CUSTOMER\_GRADE IN
<foreach
collection="gradeCodes"
item="gradeCode"
open="("
separator=","
close=")">
#{gradeCode}
</foreach>
</if>

빈 목록 정책:

조건 생략
또는
결과 0건

두 의미는 다르다.

gradeCodes 미전송
→ 전체 등급

gradeCodes = \[\]
→ 전체 등급인가?
→ 결과 없음인가?
→ 입력 오류인가?

Request 계약에서 확정한다.

## 17.4.6 동적 정렬

금지:

ORDER BY ${sortBy} ${sortDirection}

사용자 입력을 SQL 문법으로 직접 넣지 않는다.

안전한 방법:

외부 sortBy
→ Enum 검증
→ 서버 내부 SortSpec
→ 승인된 SQL Fragment

## 17.4.7 Count와 List Fragment 공유

권장:

<sql id="customerSearchFrom">
FROM SV\_CUSTOMER C
</sql>

<sql id="customerSearchWhere">
<where>
...
</where>
</sql>

Count:

SELECT COUNT(1)
<include refid="customerSearchFrom"/>
<include refid="customerSearchWhere"/>

List:

SELECT ...
<include refid="customerSearchFrom"/>
<include refid="customerSearchWhere"/>
ORDER BY ...
OFFSET ...

공통 Fragment에 ORDER BY와 페이징을 넣지 않는다.

## 17.4.8 공통 Fragment의 과도한 추상화

하나의 거대한 Fragment를 모든 목록 SQL이 공유하면 다음 문제가 발생한다.

사용하지 않는 Join 포함

Count SQL 성능 저하

변경 영향 확대

조건 조합 이해 어려움

실행계획 불안정

업무 의미가 같은 검색조건 범위에서만 공유한다.

## 17.4.9 조건에 따른 Join

<if test="gradeName != null">
JOIN SV\_GRADE G
ON G.GRADE\_CODE = C.GRADE\_CODE
</if>

동적 Join은 Count와 List의 정합성과 실행계획을 복잡하게 만든다.

가능하면 다음을 검토한다.

항상 필요한 Join인가?

EXISTS로 대체 가능한가?

별도 조회 거래가 적절한가?

검색전용 View가 필요한가?

## 17.4.10 권한조건 누락 방지

동적 SQL에서 가장 위험한 누락은 데이터권한 조건이다.

<if test="allowedBranchIds != null">
AND BRANCH\_CODE IN (...)
</if>

allowedBranchIds가 null이면 전체 지점이 조회될 수 있다.

권장:

권한범위 null
→ 전체 허용이 아님
→ 권한 오류

Service가 유효한 권한 Query를 생성한 뒤 Mapper를 호출한다.

Mapper에서도 Empty·Null 권한조건을 안전하게 차단할 수 있다.

## 17.4.11 날짜 경계

다음 조건은 종료일 당일의 시간 데이터를 누락할 수 있다.

REGISTERED\_AT <= #{toDate}

toDate가 2026-07-18 00:00:00이면 그날 이후 시간이 제외된다.

권장:

REGISTERED\_AT >= #{fromDateTime}
AND REGISTERED\_AT < #{toDateExclusive}

## 17.4.12 Optional 조건과 실행계획

선택조건이 많으면 하나의 SQL이 수십·수백 가지 형태로 실행된다.

조건 A

조건 B

조건 C

A+B

A+C

B+C

A+B+C

모든 조합이 같은 Index로 빠르지 않을 수 있다.

빈도가 높은 검색패턴은 별도 SQL로 분리할 수 있다.

고객번호 정확조회

고객명·지점 검색

기간·상태 검색

하나의 거대한 동적 SQL이 항상 최선은 아니다.

## 17.4.13 조건 문자열 변환

주의:

WHERE UPPER(CUSTOMER\_NAME)
LIKE UPPER(#{customerName})

컬럼 함수가 Index 사용을 방해할 수 있다.

대안:

정규화 컬럼

Function-Based Index

검색 전용 컬럼

전문검색 엔진

DBA와 실행계획을 확인한다.

## 17.4.14 동적 SQL 테스트 조합

모든 가능한 조합을 전부 테스트하기 어렵다면 위험 기반으로 선정한다.

조건 없음

조건 하나씩

가장 흔한 조합

가장 선택도가 낮은 조합

최대 목록조건

권한조건만 존재

빈 IN 목록

LIKE 특수문자

최대 기간

잘못된 정렬값

# 목표 아키텍처

StandardRequest
↓
CustomerListRequest
├─ SearchCondition
├─ SortRequest
└─ PageRequest
↓
Bean Validation
↓
CustomerSearchRule
├─ 필수조건
├─ 기간 상한
├─ pageSize 상한
├─ 정렬 허용목록
└─ 데이터권한
↓
CustomerListQuery
├─ 정규화 검색조건
├─ 권한범위
├─ SortSpec
└─ NormalizedPage
↓
CustomerDao
├─ countCustomerList()
└─ selectCustomerList()
↓
CustomerMapper
├─ 공통 WHERE
├─ 안정된 ORDER BY
└─ OFFSET/FETCH 또는 Keyset
↓
CustomerListResponse
├─ items
└─ page metadata

# 정상 처리 흐름

1\. 화면이 검색조건·정렬·페이지 정보를 전송한다.
2\. STF가 인증·권한·거래통제·Timeout을 확인한다.
3\. Handler가 Body를 CustomerListRequest로 변환한다.
4\. Bean Validation이 필드 형식과 범위를 검증한다.
5\. Service·Rule이 검색기간과 pageSize 상한을 검증한다.
6\. 인증 Context에서 데이터권한 범위를 가져온다.
7\. 정렬값을 승인된 SortSpec으로 변환한다.
8\. offset 또는 Cursor를 계산한다.
9\. Count SQL을 실행한다.
10\. 0건이면 빈 목록 응답을 만든다.
11\. List SQL을 안정된 정렬과 페이징으로 실행한다.
12\. 개인정보를 권한에 따라 마스킹한다.
13\. Page Metadata를 계산한다.
14\. ETF가 표준 성공 응답을 생성한다.
15\. GUID·ServiceId·Count/List Statement ID를 기록한다.

# 목록 0건 흐름

검색조건 정상
↓
Count SQL = 0
↓
List SQL 생략 가능
↓
items = \[\]
↓
totalCount = 0
↓
totalPages = 0
↓
SUCCESS

목록 0건은 일반적으로 업무 오류가 아니다.

# 입력 오류 흐름

pageSize = 10,000
↓
Validation·Rule
↓
최대값 초과
↓
DAO·Mapper 미실행
↓
VALIDATION 오류

# 대량 조회 차단 흐름

조건 없는 조회
↓
Search Rule
↓
전체 조회 위험 판단
↓
E-SV-SRCH-0001
↓
“검색조건을 추가해 주세요.”

# Timeout 흐름

Count SQL 장기 실행
↓
Query Timeout
↓
Statement Cancel
↓
Facade Transaction 종료
↓
Connection 반환
↓
TCF Timeout 응답
↓
Slow SQL·실행계획·DB Pool 확인

# 정상 예시

## Request DTO

public record CustomerListRequest(
@Valid CustomerSearchCondition searchCondition,
@Valid SortRequest sort,
@NotNull @Valid PageRequest page
) {}

## Query

public record CustomerListQuery(
String customerName,
Set<String> gradeCodes,
LocalDateTime fromDateTime,
LocalDateTime toDateTimeExclusive,
Set<String> allowedBranchIds,
CustomerSort sort,
SortDirection direction,
long offset,
int pageSize
) {}

## Service

public CustomerListResponse selectList(
CustomerListRequest request,
TransactionContext context) {

CustomerListQuery query =
rule.buildQuery(
request,
context.getAuthenticationContext()
);

long totalCount =
dao.countCustomerList(query);

if (totalCount == 0L) {
return CustomerListResponse.empty(
query
);
}

List<CustomerListRow> rows =
dao.selectCustomerList(query);

return assembler.toResponse(
rows,
query,
totalCount,
context.getAuthenticationContext()
);
}

# 금지 예시

조건 없는 전체 조회를 허용한다.

화면 pageSize 값을 그대로 SQL에 사용한다.

전체 데이터를 List로 조회한 뒤 Java에서 잘라낸다.

ORDER BY 없이 Offset 페이징한다.

고객명만 정렬하고 유일 보조키를 두지 않는다.

요청 sortBy를 ${}로 SQL에 삽입한다.

Count SQL과 List SQL의 WHERE가 다르다.

빈 IN 목록의 의미를 정의하지 않는다.

LIKE 검색에서 %, \_를 무조건 허용한다.

권한조건이 null이면 전체 데이터를 조회한다.

종료일 00시를 <= 조건으로 사용한다.

깊은 Offset 성능을 확인하지 않는다.

전체 Count가 필요하지 않은데 항상 Count SQL을 실행한다.

온라인 목록 거래로 Excel 전체 데이터를 생성한다.

페이지 요청 오류를 조용히 임의 값으로 바꾼다.

페이지 번호가 범위를 넘으면 첫 페이지를 반환한다.

동적 SQL 한 개로 모든 검색패턴을 억지로 처리한다.

Count SQL에 불필요한 대형 Join을 모두 포함한다.

개인정보 조회조건과 결과를 로그에 원문으로 남긴다.

Timeout 응답 후 SQL이 계속 실행되게 둔다.

# 책임 경계와 RACI

| 활동 | UI | 업무분석 | 업무개발 | DA·DBA | FW | 보안 | QA | 운영 | AA |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 검색조건 정의 | R | A | C | C | I | C | C | I | C |
| 기본·최대값 | C | R | R | C | C | C | C | C | A |
| 페이징 방식 | C | C | R | C | C | I | C | C | A |
| 정렬정책 | C | R | R | C | I | I | C | I | A |
| SQL·Index | I | C | R | A/R | I | C | C | C | C |
| 데이터권한 | I | C | C | I | C | A/R | C | C | C |
| 개인정보 마스킹 | C | C | R | I | C | A | C | C | C |
| 대량조회 통제 | C | C | R | C | C | C | C | R | A |
| Timeout | I | C | R | C | C | I | C | R | A |
| 성능시험 | I | C | C | C | C | I | A/R | R | C |
| 운영 Metric | I | I | C | I | R | C | C | A/R | C |

# 데이터 및 상태관리

목록 Response는 특정 시점의 조회 Snapshot이다.

페이지 1 조회시점
≠ 페이지 2 조회시점

일반 화면에서는 일정 수준의 변동을 허용할 수 있다.

다음 업무는 더 강한 일관성이 필요할 수 있다.

회계 보고

감사 보고

대량 다운로드

승인 대상 목록

정산 목록

이 경우 다음을 검토한다.

조회 기준시각

Snapshot ID

Cursor

임시 결과 Table

Batch 추출

파일 결과 고정

# 성능·용량·확장성

## 비용 구성

전체 거래시간
\=
STF
\+ Count SQL
\+ List SQL
\+ Mapping
\+ 마스킹
\+ JSON 직렬화
\+ 네트워크

## 관측 항목

| 항목 | 설명 |
| --- | --- |
| Count 시간 | 전체 건수 SQL |
| List 시간 | 페이지 목록 SQL |
| Row Count | 실제 반환 건수 |
| pageSize | 요청 크기 |
| pageNo·Offset | 깊은 페이지 여부 |
| Result Bytes | 응답 크기 |
| DB Pool Wait | Connection 대기 |
| Serialization | JSON 변환시간 |
| Total Time | 전체 TCF 처리시간 |

## 확장 전략

일반 페이지
→ Offset

깊은 페이지
→ Keyset

대형 다운로드
→ Batch·Streaming

반복 동일 조회
→ 짧은 Cache 검토

복잡한 검색
→ 검색전용 모델·Index·Engine 검토

# 보안·개인정보·감사

| 영역 | 기준 |
| --- | --- |
| 검색권한 | ServiceId 기능권한 |
| 데이터권한 | 지점·조직 범위 |
| 검색 최소길이 | 개인정보 탐색 방지 |
| 결과 상한 | 대량 노출 방지 |
| 마스킹 | 항목별 원문권한 |
| 정렬값 | 허용목록 |
| SQL Parameter | #{} 사용 |
| 다운로드 | 별도 권한·감사 |
| 로그 | 조건·결과 원문 제한 |
| 반복조회 | 사용자별 이상징후 탐지 |

# 운영·모니터링·장애 대응

권장 로그:

event=LIST\_QUERY\_COMPLETED
guid=G-...
serviceId=SV.Customer.selectList
pageNo=1
pageSize=100
offset=0
totalCount=251
returnedCount=100
countElapsedMs=34
listElapsedMs=86
elapsedMs=151
sort=CUSTOMER\_NAME\_ASC
result=SUCCESS

민감 검색조건은 Hash 또는 마스킹된 형태로 기록한다.

운영 질문:

어떤 ServiceId에서 pageSize가 큰가?

어떤 거래에서 Offset이 깊어지는가?

Count와 List 중 어느 SQL이 느린가?

조건 없는 조회가 발생했는가?

특정 사용자·지점이 반복 조회하는가?

응답 크기가 급증했는가?

Timeout 후 Connection이 반환됐는가?

# 자동검증 및 품질 Gate

## 1\. Request Gate

pageNo 최소값

pageSize 최소·최대값

최대 검색기간

필수 검색조건

정렬 허용목록

## 2\. SQL Gate

검출:

ORDER BY 없는 페이징

SELECT \*

${사용자입력}

무제한 목록조회

조건 없는 개인정보 조회

정렬 없는 FETCH FIRST

Query Timeout 누락

## 3\. Count/List 정합 Gate

검색조건 Fragment

권한조건

상태조건

논리삭제조건

기준일조건

두 SQL이 동일한 업무 집합을 조회하는지 검사한다.

## 4\. 정렬 Gate

업무 정렬키
\+ 유일 보조키

유일 보조키가 없으면 리뷰 결함으로 등록한다.

## 5\. 계층 Gate

TCF Core에서 업무 pageSize 결정 금지

Handler에서 SQL Offset 계산 금지

Mapper에서 사용자 권한 생성 금지

DAO에서 업무 상한 결정 금지

## 6\. 대량조회 Gate

최대 pageSize

최대 조회기간

온라인 최대 결과

다운로드 분리

응답 크기

## 7\. 성능 Gate

Count 실행계획

List 실행계획

첫 페이지 p95

깊은 페이지 p95

최대 조건 p95

동시 사용자 TPS

DB Pool 사용률

# 테스트 시나리오

| ID | 시나리오 | 기대 결과 |
| --- | --- | --- |
| PAG-001 | 정상 첫 페이지 | 목록·메타 정상 |
| PAG-002 | pageNo 미입력 | 기본 1 |
| PAG-003 | pageSize 미입력 | 기본값 |
| PAG-004 | pageNo 0 | 입력 오류 |
| PAG-005 | pageNo 음수 | 입력 오류 |
| PAG-006 | pageSize 0 | 입력 오류 |
| PAG-007 | pageSize 최대값 | 정상 |
| PAG-008 | pageSize 최대 초과 | 입력 오류 |
| PAG-009 | 결과 0건 | 빈 목록·SUCCESS |
| PAG-010 | 마지막 페이지 | 잔여 건수 반환 |
| PAG-011 | 마지막 이후 페이지 | 빈 목록 |
| PAG-012 | totalCount 정확성 | DB와 일치 |
| PAG-013 | totalPages 계산 | 올림값 |
| PAG-014 | hasNext | 페이지에 맞게 계산 |
| PAG-015 | 조건 없음 | 업무 오류 |
| PAG-016 | 기간 시작일 누락 | 정책에 따른 오류·기본값 |
| PAG-017 | 시작일 > 종료일 | 입력 오류 |
| PAG-018 | 최대기간 | 정상 |
| PAG-019 | 최대기간 초과 | 입력 오류 |
| PAG-020 | 고객명 최소길이 미만 | 검색 오류 |
| PAG-021 | 안정된 정렬 | 반복조회 순서 동일 |
| PAG-022 | 동명이인 정렬 | 고객번호로 순서 고정 |
| PAG-023 | Null 정렬 | 정책 일치 |
| PAG-024 | 승인된 정렬값 | 정상 |
| PAG-025 | 미승인 정렬값 | 입력 오류 |
| PAG-026 | SQL Injection 정렬 | 차단 |
| PAG-027 | LIKE % | Escape 정책 |
| PAG-028 | LIKE \_ | Escape 정책 |
| PAG-029 | IN 목록 정상 | 조건 적용 |
| PAG-030 | IN 빈 목록 | 계약대로 처리 |
| PAG-031 | Count/List 조건 | 같은 대상 집합 |
| PAG-032 | 논리삭제 조건 | 양 SQL 일치 |
| PAG-033 | 데이터권한 | 허용 지점만 반환 |
| PAG-034 | 권한범위 null | 전체조회 차단 |
| PAG-035 | 일반 사용자 | 개인정보 마스킹 |
| PAG-036 | 원문 권한 | 승인 필드만 원문 |
| PAG-037 | 깊은 Offset | 성능 기준 확인 |
| PAG-038 | Keyset 다음 페이지 | 중복·누락 확인 |
| PAG-039 | 데이터 삽입 중 Offset | 허용정책 확인 |
| PAG-040 | Count SQL Timeout | Timeout 오류 |
| PAG-041 | List SQL Timeout | Timeout 오류 |
| PAG-042 | Timeout 후 Pool | Connection 반환 |
| PAG-043 | 최대 동시조회 | p95·Pool 기준 |
| PAG-044 | 온라인 대량요청 | 차단 |
| PAG-045 | 다운로드 요청 | 별도 거래 전환 |
| PAG-046 | Count 생략형 | hasNext 정상 |
| PAG-047 | 응답 크기 | 최대 기준 이내 |
| PAG-048 | GUID 추적 | Count·List 연결 |
| PAG-049 | 민감 로그 | 원문 없음 |
| PAG-050 | Oracle 실행 | 문법·계획 정상 |

# 따라 하는 실무 절차

## 1단계. 목록 거래를 정의한다

ServiceId

화면 이벤트

검색 대상

데이터 소유권

권한

결과 항목

## 2단계. 검색조건을 분류한다

필수조건

선택조건

기본값

최대 범위

개인정보

## 3단계. 정렬정책을 정의한다

기본 정렬

허용 정렬

방향

Null 위치

유일 보조키

## 4단계. 페이징 방식을 선택한다

페이지 번호 필요
→ Offset

더 보기·깊은 페이지
→ Keyset·Cursor

대량 파일
→ Batch·Streaming

## 5단계. Request·Query DTO를 작성한다

완료 증적:

Validation

기본값

Offset·Cursor

권한범위

SortSpec

## 6단계. Count와 List Mapper를 작성한다

같은 WHERE

안정된 ORDER BY

페이지 제한

Query Timeout

## 7단계. 실행계획을 확인한다

Index

예상 Row

실제 Row

Sort 비용

Offset 비용

Count 비용

## 8단계. Service 응답을 조립한다

items

pageNo

pageSize

totalCount

totalPages

hasNext

## 9단계. 정상·경계·실패 테스트를 수행한다

0건

첫 페이지

마지막 페이지

최대 pageSize

조건 없음

권한 없음

Timeout

## 10단계. 운영 로그를 확인한다

GUID

Count SQL

List SQL

Offset

Row Count

처리시간

# 완료 체크리스트

## 검색조건

| 확인 항목 | 완료 |
| --- | --- |
| 검색조건의 필수·선택 여부를 정의했다. | □ |
| 조건 없는 조회 정책이 있다. | □ |
| 기본 조회기간을 정의했다. | □ |
| 최대 조회기간을 정의했다. | □ |
| 개인정보 검색 최소길이를 정의했다. | □ |
| 정규화 정책을 정의했다. | □ |
| 데이터권한 조건을 포함했다. | □ |

## 정렬

| 확인 항목 | 완료 |
| --- | --- |
| 기본 정렬이 있다. | □ |
| 정렬 허용목록이 있다. | □ |
| 정렬 방향을 검증한다. | □ |
| 유일 보조키가 있다. | □ |
| Null 정렬정책이 있다. | □ |
| 사용자 정렬값을 ${}로 사용하지 않는다. | □ |
| 정렬과 Index를 검토했다. | □ |

## 페이징

| 확인 항목 | 완료 |
| --- | --- |
| 기본 pageNo가 있다. | □ |
| 기본 pageSize가 있다. | □ |
| 최대 pageSize가 있다. | □ |
| Offset을 long으로 안전하게 계산한다. | □ |
| Count와 List SQL이 분리됐다. | □ |
| 두 SQL의 검색조건이 일치한다. | □ |
| 목록 0건은 빈 목록이다. | □ |
| totalPages 계산을 검증했다. | □ |
| 깊은 페이지 성능을 검증했다. | □ |
| Keyset 필요 여부를 판단했다. | □ |

## 대량조회

| 확인 항목 | 완료 |
| --- | --- |
| 온라인 최대 결과를 정의했다. | □ |
| 응답 최대 크기를 정의했다. | □ |
| 대량 다운로드를 별도 거래로 분리했다. | □ |
| Count SQL 비용을 확인했다. | □ |
| Query Timeout이 있다. | □ |
| 반복 조회 탐지 기준이 있다. | □ |
| Timeout 후 Connection 반환을 검증했다. | □ |

## 동적 SQL

| 확인 항목 | 완료 |
| --- | --- |
| #{} Parameter Binding을 사용한다. | □ |
| LIKE Escape 정책이 있다. | □ |
| 빈 IN 목록 정책이 있다. | □ |
| 날짜 종료경계를 정확히 처리한다. | □ |
| 공통 WHERE가 Count·List에 적용된다. | □ |
| 권한조건 누락을 차단한다. | □ |
| 주요 조건조합을 테스트했다. | □ |
| 실행계획을 검증했다. | □ |

## 운영·보안

| 확인 항목 | 완료 |
| --- | --- |
| GUID로 Count·List SQL을 추적한다. | □ |
| pageNo·pageSize를 Metric으로 확인한다. | □ |
| Count·List 시간을 분리 측정한다. | □ |
| 개인정보 조건·결과를 로그에 남기지 않는다. | □ |
| 대량조회는 감사 대상이다. | □ |
| Slow SQL 경보가 있다. | □ |
| 성능시험을 통과했다. | □ |

# 변경·호환성·폐기 관리

## 기본 pageSize 변경

기존 100
→ 신규 50

호환성 영향:

화면 페이지 수 증가

호출 횟수 증가

응답 크기 감소

DB 거래량 증가 가능

성능과 UI 영향을 함께 검토한다.

## 최대 pageSize 축소

500
→ 100

기존 소비자의 요청이 실패할 수 있으므로 계약 변경이다.

## 정렬 기준 변경

등록일시 DESC
→ 고객명 ASC

페이지 결과와 사용자 경험이 달라진다.

Cursor 방식에서는 기존 Cursor가 무효가 될 수 있다.

## 검색조건 의미 변경

toDate 포함
→ toDate 미포함

같은 요청이 다른 결과를 만들기 때문에 비호환 변경으로 취급한다.

## Offset에서 Cursor로 전환

한 번에 기존 계약을 제거하지 않는다.

기존 ServiceId
SV.Customer.selectList

신규 ServiceId
SV.Customer.selectListByCursor

또는 API Version을 분리한다.

소비자 전환 후 기존 호출량이 0인지 확인하고 폐기한다.

## totalCount 제거

페이지 번호 UI가 totalCount를 사용한다면 비호환이다.

Count 생략형은 별도의 응답 계약으로 제공한다.

## 목록 거래 폐기

신규 호출 중지
↓
UI·Client·Batch 소비자 조사
↓
대체 ServiceId 제공
↓
호출량 확인
↓
OM 비활성
↓
Mapper·SQL 제거
↓
Index·View 폐기 검토

# 시사점

## 핵심 아키텍처 판단

첫째, 페이징은 TCF Core의 공통 업무 기능이 아니라 각 업무 WAR가 데이터 특성에 맞게 처리해야 하는 업무 데이터 접근 책임이다.

둘째, 일반적인 업무 화면은 Offset 페이징으로 충분하지만, 깊은 페이지와 대용량 연속 조회는 Keyset·Cursor가 더 적합할 수 있다.

셋째, 목록 조회의 정합성은 안정된 정렬에서 시작한다.

업무 정렬키
\+ 유일 보조키

가 없으면 페이지 중복과 누락을 제어하기 어렵다.

넷째, Count SQL + List SQL은 같은 검색조건과 권한범위를 가져야 한다.

다섯째, 대량 다운로드는 pageSize를 크게 만드는 방식으로 해결하지 않는다.

온라인 목록
≠ 대량 파일 추출

여섯째, 동적 SQL의 가장 큰 위험은 문법 오류보다 권한조건 누락과 사용자 입력의 SQL 직접 삽입이다.

## 주요 위험

| 위험 | 결과 |
| --- | --- |
| 조건 없는 조회 | DB 전체 Scan |
| pageSize 무제한 | Heap·Network 부하 |
| ORDER BY 없음 | 중복·누락 |
| 비유일 정렬 | 페이지 순서 변동 |
| 깊은 Offset | 응답시간 증가 |
| Count/List 조건 차이 | 잘못된 페이지 정보 |
| ${} 정렬 | SQL Injection |
| 빈 권한조건 | 전체 데이터 노출 |
| LIKE 특수문자 | 예상 밖 대량 결과 |
| 종료일 처리 오류 | 당일 데이터 누락 |
| 전체 List 적재 | 메모리 고갈 |
| Count 상시 실행 | 불필요한 DB 부하 |
| 온라인 Excel 생성 | Thread·Connection 장기 점유 |
| Timeout 후 SQL 잔존 | Pool 고갈 |
| 개인정보 로그 | 정보유출 |
| 페이징 변경 무관리 | 기존 UI 장애 |

## 우선 보완 과제

1.  업무 공통 PageRequest, PageMetadata 표준을 정의한다.
2.  pageNo·pageSize 기본값과 최대값을 거래 유형별로 기준화한다.
3.  Offset 계산을 long으로 안전하게 처리한다.
4.  Count·List 공통 WHERE Fragment 사용을 표준화한다.
5.  모든 페이징 SQL에 안정된 유일 정렬키를 강제한다.
6.  정렬값을 Enum 허용목록으로 제한한다.
7.  ${} 사용자 입력 사용을 정적검사로 차단한다.
8.  조건 없는 목록 조회를 품질 Gate에서 탐지한다.
9.  온라인 최대 결과와 최대 조회기간을 정의한다.
10.  대량 다운로드를 별도 비동기 거래로 분리한다.
11.  Count 생략형·Keyset 표준을 추가한다.
12.  LIKE Escape와 빈 IN 정책을 공통화한다.
13.  데이터권한 Query가 null일 때 전체조회되지 않도록 차단한다.
14.  Count·List 실행시간을 별도 Metric으로 수집한다.
15.  깊은 페이지 성능시험을 CI·성능 Gate에 포함한다.

## 중장기 발전 방향

업무별 수동 Offset 페이징
↓
공통 Page 계약 표준화
↓
Count·List SQL 정합성 자동검사
↓
정렬 허용목록·권한조건 자동검증
↓
Keyset·Cursor 공통 컴포넌트
↓
대량 다운로드 Batch·Streaming 표준
↓
실행계획·성능 회귀 자동검사
↓
운영 데이터 기반 페이징 방식 최적화

# 마무리말

목록 조회와 페이징을 설계하는 과정은 다음 질문에 답하는 일이다.

어떤 검색조건이 반드시 필요한가?

조건이 없으면 무엇을 조회하는가?

최대 조회기간은 얼마인가?

한 페이지의 최대 건수는 얼마인가?

정렬순서는 항상 재현 가능한가?

동일한 정렬값 안의 순서는 무엇으로 결정하는가?

전체 건수가 정말 필요한가?

Offset이 적합한가, Cursor가 적합한가?

Count와 List는 같은 데이터를 대상으로 하는가?

권한조건이 모든 SQL에 적용되는가?

대량 결과는 온라인에서 처리해도 되는가?

Timeout 후 SQL과 Connection이 정리되는가?

GUID로 Count와 List SQL을 모두 추적할 수 있는가?

제17장의 핵심 흐름은 다음과 같다.

검색조건
↓
정렬정책
↓
페이징 방식 선택
↓
권한·상한 검증
↓
Count SQL
↓
List SQL
↓
빈 목록·페이지 메타
↓
성능·로그·감사 검증

가장 중요한 원칙은 다음과 같다.

목록을 나누어 반환한다고
안전한 페이징이 되는 것은 아니다.

제한된 검색범위,
안정된 정렬,
일치하는 Count와 List,
허용된 데이터권한이 함께 있어야
운영 가능한 목록 거래가 된다.
