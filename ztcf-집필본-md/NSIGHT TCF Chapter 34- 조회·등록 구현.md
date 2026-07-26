<!-- source: ztcf-집필본/NSIGHT TCF Chapter 34- 조회·등록 구현.docx -->
<!-- tables: mammoth-html + placeholder table-to-md -->

# 제34장. 조회·등록 구현

## 도입 전 안내말

제33장에서는 상담예약 CRUD를 구현하기 전에 다음 설계를 확정했다.

\`\`\`text id=“impl34001” 업무코드 CT

화면 ID CT-RSV-0001

Aggregate 상담예약 Master + 변경이력

상태 READY · COMPLETED · CANCELED

조회 ServiceId CT.Reservation.selectList CT.Reservation.selectDetail

등록 ServiceId CT.Reservation.create

수정 ServiceId CT.Reservation.update

취소 ServiceId CT.Reservation.cancel



제34장에서는 이 중 다음 세 거래를 실제 코드 구조로 구체화한다.

\`\`\`text id="impl34002"
CT.Reservation.selectList

CT.Reservation.selectDetail

CT.Reservation.create

수정·취소·Version 동시성 제어는 제35장에서 이어서 구현한다.

상담예약 업무를 구현한다고 해서 새로운 업무 Controller를 만드는 것은 아니다.

NSIGHT TCF에서는 공통 온라인 진입점을 사용한다.

\`\`\`text id=“impl34003” 화면

→ 공통 OnlineTransactionController

→ TCF.process()

→ STF

→ TransactionDispatcher

→ CtReservationHandler

→ CtReservationFacade

→ CtReservationService

→ Rule·DAO·Mapper

→ DB

→ ETF

→ StandardResponse



따라서 이번 장에서 새로 만드는 주요 구성요소는 다음과 같다.

\`\`\`text id="impl34004"
CtReservationHandler

CtReservationFacade

CtReservationService

CtReservationRule

CtReservationDao

CtReservationMapper

Request·Response·Query·Command·Data DTO

Mapper XML

Table DDL

오류코드

테스트

구현에서 가장 주의해야 할 점은 코드를 작성하는 순서다.

초보 개발자는 Mapper SQL부터 만들기 쉽다.

\`\`\`text id=“impl34005” Table 확인

→ Mapper XML 작성

→ DAO 작성

→ Service 작성

→ 화면에 맞춰 수정



이 순서로 진행하면 외부 요청 DTO와 DB 구조가 직접 결합되고, 업무규칙이 SQL과 Service 곳곳에 흩어질 가능성이 높다.

권장 구현 순서는 다음과 같다.

\`\`\`text id="impl34006"
ServiceId와 입출력 계약

→ 업무규칙

→ Transaction 경계

→ 내부 Query·Command

→ DAO·Mapper 계약

→ SQL

→ 응답 변환

→ 테스트

→ OM·로그·Metric

이번 장의 핵심은 단순히 조회 SQL과 INSERT SQL을 작성하는 것이 아니다.

\`\`\`text id=“impl34007” 목록의 결과가 안정적으로 Paging되는가?

상세 결과 없음이 명확히 표현되는가?

조회 권한이 SQL 범위에 반영되는가?

동일 예약이 동시에 두 번 등록되지 않는가?

등록 재요청이 중복 Row를 만들지 않는가?

Master와 History가 함께 Commit되는가?

INSERT 결과가 실제 1건인지 확인하는가?

운영자가 GUID로 전체 처리를 추적할 수 있는가?



를 코드와 테스트로 증명하는 것이 목표다.

\---

\# 문서 개요

\## 목적

본 장의 목적은 제33장에서 설계한 상담예약의 목록조회·상세조회·등록 거래를 NSIGHT TCF 표준 계층에 맞게 구현하는 것이다.

세부 목적은 다음과 같다.

\`\`\`text id="impl34008"
공통 Controller와 업무 Handler 경계 유지

ServiceId별 명시적 Handler 분기

목록·상세·등록 DTO 분리

화면 Request와 DB Data DTO 분리

인증문맥 기반 데이터권한 적용

조회조건 기본값·최대범위 검증

안정적인 정렬과 Paging 구현

목록 결과 없음과 상세 결과 없음 구분

등록 업무규칙과 중복방지 구현

사전 중복조회와 DB Unique Constraint 병행

Idempotency Key 기반 재요청 통제

Master·History Transaction 원자성 확보

INSERT 영향 행 수 검증

표준 오류코드 변환

거래로그·감사로그·Metric 연결

단위·Mapper·통합·동시성 테스트 구현

## 적용범위

| 구분 | 적용 대상 |
| --- | --- |
| 화면 | CT-RSV-0001 |
| 목록조회 | CT.Reservation.selectList |
| 상세조회 | CT.Reservation.selectDetail |
| 등록 | CT.Reservation.create |
| 업무 WAR | ct-service 목표 구조 |
| DB | 상담예약 Master·History |
| 고객 확인 | IC 고객 계약 |
| 권한 | 조회·등록 기능권한과 지점 데이터권한 |
| 운영 | Service Catalog·Timeout·로그·Metric |
| 테스트 | Rule·Mapper·Service·통합·E2E |

## 대상 독자

\`\`\`text id=“impl34009” 신규 CRUD를 처음 구현하는 개발자

Handler·Facade·Service 역할이 혼란스러운 개발자

MyBatis 목록·Paging을 구현해야 하는 개발자

등록 중복과 네트워크 재요청을 구분해야 하는 개발자

Transaction과 History 저장을 구현해야 하는 개발자

AI 코딩도구가 생성한 CRUD를 검토해야 하는 개발자

프로그램 설계서와 소스를 함께 관리해야 하는 개발자


\## 선행조건

\`\`\`text id="impl34010"
제33장 상담예약 설계

TCF 공통 온라인 처리 흐름

ServiceId 표준

AuthenticationContext

Facade Transaction

MyBatis Mapper

Oracle SQL

오류코드 표준

거래로그·감사로그

Idempotency 기본 개념

# 핵심 관점

\`\`\`text id=“impl34011” 조회·등록 구현은 SQL을 실행하는 코드를 만드는 일이 아니다.

입력계약을 내부 업무명령으로 변환하고, 권한과 업무규칙을 검증한 뒤,

정해진 트랜잭션 안에서 정확히 필요한 데이터만 읽고 변경하며,

그 결과를 표준응답과 운영 증적으로 남기는 일이다.


\---

\# 학습 목표

이 장을 마치면 다음 내용을 설명하고 구현할 수 있어야 한다.

| 번호 | 학습 목표 |
|---:|---|
| 1 | 업무별 Controller를 추가하지 않는 이유를 설명한다. |
| 2 | Handler에서 ServiceId를 명시적으로 분기한다. |
| 3 | 목록·상세·등록 Request DTO를 분리한다. |
| 4 | Request DTO를 Mapper에 직접 전달하지 않는다. |
| 5 | 화면 사용자정보와 인증 사용자정보를 구분한다. |
| 6 | 목록 기본값과 최대 Page Size를 검증한다. |
| 7 | 조회기간을 반개방구간으로 변환한다. |
| 8 | Paging 정렬에 유일한 Tie-breaker를 적용한다. |
| 9 | 목록 결과 없음과 상세 결과 없음을 구분한다. |
| 10 | 목록과 Count SQL의 조건을 동일하게 유지한다. |
| 11 | 목록 N+1 호출을 방지한다. |
| 12 | 등록 Command에 서버 결정값을 주입한다. |
| 13 | 사전 중복조회와 DB Unique Constraint를 병행한다. |
| 14 | Idempotency Key와 업무 중복키를 구분한다. |
| 15 | Master와 History를 같은 Transaction으로 저장한다. |
| 16 | INSERT 영향 행 수가 1건인지 검증한다. |
| 17 | DB 제약 위반을 표준 업무 오류로 변환한다. |
| 18 | 외부 고객확인과 DB Transaction의 위치를 판단한다. |
| 19 | 등록 Timeout 후 결과확인 방식을 설명한다. |
| 20 | Rule·Mapper·통합·동시성 테스트를 작성한다. |
| 21 | 한 거래를 GUID·ServiceId·SQL ID로 추적한다. |
| 22 | 현재 샘플 소스에서 재사용할 패턴과 보완할 패턴을 구분한다. |

\---

\# 핵심 용어

| 용어 | 의미 |
|---|---|
| List Query | 여러 건을 조건과 Paging으로 조회하는 거래 |
| Detail Query | 식별자로 정확히 한 건을 조회하는 거래 |
| Stable Sort | 같은 요청에서 순서가 뒤바뀌지 않는 정렬 |
| Tie-breaker | 정렬값이 같을 때 순서를 확정하는 고유값 |
| Offset Paging | Page 번호와 Offset으로 조회하는 방식 |
| Keyset Paging | 마지막 정렬키 이후 데이터를 조회하는 방식 |
| Search Criteria | 검증·보정된 내부 조회조건 |
| Command | 서버가 완성한 변경 명령 |
| Row DTO | Mapper 입출력용 DB 데이터객체 |
| Affected Rows | DML 실행으로 실제 변경된 행 수 |
| Business Duplicate | 업무상 같은 예약이 이미 존재하는 상태 |
| Request Duplicate | 동일 네트워크 요청이 다시 전달된 상태 |
| Idempotency Key | 동일 요청 재처리를 식별하는 키 |
| Unique Constraint | DB가 중복 저장을 최종 차단하는 제약 |
| Read-only Transaction | 데이터 변경을 의도하지 않는 조회 트랜잭션 |
| Atomicity | 여러 변경이 전부 성공하거나 전부 실패하는 성질 |
| History | Aggregate 변경 사실을 보관하는 이력 |
| Data Scope | 사용자가 조회할 수 있는 데이터 범위 |
| Anti-Corruption Layer | 다른 도메인의 계약을 내부 모델로 변환하는 경계 |

\---

\# 전체 구현 흐름

\## 목록조회

\`\`\`text id="impl34012"
StandardRequest

→ CtReservationHandler

→ ReservationListRequest

→ CtReservationFacade.selectList

→ CtReservationRule.buildListQuery

→ 인증 사용자 지점범위 결합

→ CtReservationDao.selectList

→ CtReservationMapper.selectReservationList

→ CT\_CONTACT\_RESERVATION

→ ReservationListResponse

→ ETF

## 상세조회

\`\`\`text id=“impl34013” StandardRequest

→ CtReservationHandler

→ ReservationDetailRequest

→ CtReservationFacade.selectDetail

→ 데이터권한 Scope 생성

→ DAO.selectDetail

→ 결과 없음 검증

→ 상세 Response 변환

→ 개인정보 조회감사

→ ETF


\## 등록

\`\`\`text id="impl34014"
StandardRequest

→ CtReservationHandler

→ ReservationCreateRequest

→ CtReservationFacade.create

→ 입력 Rule

→ 고객 계약 확인

→ 상담목적 코드 확인

→ Idempotency 확인

→ 업무 중복 확인

→ 서버 Command 생성

→ Master INSERT

→ History INSERT

→ Commit

→ ReservationCreateResponse

→ ETF

# 현재 기준 소스에서 참고할 구현 패턴

현재 기준 소스에는 완성된 상담예약 기능은 없지만 조회·등록에 참고할 수 있는 패턴이 존재한다.

## 조회 참고 패턴

SV 고객조회 샘플은 다음 구조를 사용한다.

\`\`\`text id=“impl34015” SvCustomerHandler

→ SvCustomerFacade

→ SvCustomerService

→ SvCustomerRule

→ SvCustomerDao

→ SvCustomerMapper



좋은 점:

\`\`\`text id="impl34016"
ServiceId를 Handler가 명시적으로 등록한다.

Facade가 readOnly Transaction을 선언한다.

Rule이 Request를 조회조건으로 변환한다.

Service가 DAO 결과를 Response로 변환한다.

Mapper SQL에 명확한 SQL ID가 있다.

## 목록·등록 참고 패턴

EB 사용자 샘플은 다음 기능을 제공한다.

\`\`\`text id=“impl34017” EB.User.inquiry

EB.User.create



조회에서는 다음 패턴을 확인할 수 있다.

\`\`\`text id="impl34018"
Page 기본값

최대 Page Size 100

Search Criteria 분리

목록 SQL

Count SQL

Offset Paging

등록에서는 다음 패턴을 확인할 수 있다.

\`\`\`text id=“impl34019” 사전 중복조회

Master INSERT

같은 업무 Transaction의 Event INSERT

등록 Response 반환


\## 상담예약 구현에서 보완할 항목

현재 샘플을 그대로 복사해서는 안 된다.

| 샘플 패턴 | 상담예약 구현 기준 |
|---|---|
| \`Map<String,Object>\` 입력 | Handler 경계에서 Typed DTO 변환 |
| Request의 사용자·지점 값 사용 | AuthenticationContext에서 취득 |
| \`LIKE '%값%'\` | 검색요건·Index 기준으로 제한 |
| \`ORDER BY CREATED\_AT DESC\` | 고유 Tie-breaker 추가 |
| 선조회 후 INSERT | Unique Constraint까지 적용 |
| INSERT 결과 미검사 | 영향 행 수 1건 검증 |
| Service에도 \`@Transactional\` | Transaction Owner를 Facade로 명확화 |
| \`System.out\` 진단 | 구조화 Logger로 교체 |
| 공통 BUSINESS\_ERROR | 상담예약 전용 오류코드 |
| 테스트 미확인 | Rule·Mapper·Transaction·동시성 Test 필수 |

현재 샘플은 개발 구조를 이해하기 위한 참고자료이며 상담예약 운영 품질을 자동 보장하는 완성 Template은 아니다.

\---

\# 목표 패키지 구조

\`\`\`text id="impl34020"
ct-service
└─ src/main/java
└─ com.nh.nsight.marketing.ct.reservation
├─ entry
│ ├─ handler
│ │ └─ CtReservationHandler.java
│ └─ facade
│ └─ CtReservationFacade.java
├─ application
│ ├─ service
│ │ └─ CtReservationService.java
│ ├─ rule
│ │ └─ CtReservationRule.java
│ └─ dto
│ ├─ request
│ ├─ response
│ ├─ query
│ └─ command
├─ persistence
│ ├─ dao
│ │ └─ CtReservationDao.java
│ ├─ mapper
│ │ └─ CtReservationMapper.java
│ └─ dto
│ ├─ ReservationListRow.java
│ ├─ ReservationDetailRow.java
│ ├─ ReservationInsertRow.java
│ └─ ReservationHistoryInsertRow.java
├─ client
│ └─ CtCustomerClient.java
├─ model
│ └─ ReservationStatus.java
└─ support
├─ CtReservationErrorCode.java
└─ ReservationKeys.java

Mapper XML:

text id="impl34021" ct-service └─ src/main/resources └─ mapper └─ ct └─ CtReservationMapper.xml

# 공통 Handler 구현

\`\`\`java id=“impl34022” @Component public class CtReservationHandler implements TransactionHandler {

public static final String SELECT\_LIST =
"CT.Reservation.selectList";

public static final String SELECT\_DETAIL =
"CT.Reservation.selectDetail";

public static final String CREATE =
"CT.Reservation.create";

private final CtReservationFacade facade;

public CtReservationHandler(
CtReservationFacade facade) {
this.facade = facade;
}

@Override
public Collection<String> serviceIds() {
return List.of(
SELECT\_LIST,
SELECT\_DETAIL,
CREATE
);
}

@Override
public Object doHandle(
StandardRequest<Map<String, Object>> request,
TransactionContext context) {

String serviceId =
context.getHeader().getServiceId();

Map<String, Object> body =
request.getBody() != null
? request.getBody()
: Map.of();

return switch (serviceId) {
case SELECT\_LIST ->
facade.selectList(body, context);

case SELECT\_DETAIL ->
facade.selectDetail(body, context);

case CREATE ->
facade.create(body, context);

default ->
throw new BusinessException(
ErrorCode.SERVICE\_NOT\_FOUND,
"CtReservationHandler가 지원하지 않는 "
\+ "serviceId입니다: "
\+ serviceId
);
};
}

}



Handler의 책임:

\`\`\`text id="impl34023"
지원 ServiceId 선언

요청 Body 안전 처리

ServiceId별 Facade 호출

미지원 ServiceId 거절

Handler에서 하지 않을 일:

\`\`\`text id=“impl34024” SQL 호출

업무 중복 판정

Transaction 관리

고객 연계

권한 데이터 조회

응답 Header 직접 생성


\---

\# Facade 구현

\`\`\`java id="impl34025"
@Service
public class CtReservationFacade {

private final CtReservationService service;

public CtReservationFacade(
CtReservationService service) {
this.service = service;
}

@Transactional(
readOnly = true,
timeout = 3
)
public Map<String, Object> selectList(
Map<String, Object> body,
TransactionContext context) {

ReservationListRequest request =
ReservationListRequest.from(body);

return service
.selectList(request, context)
.toMap();
}

@Transactional(
readOnly = true,
timeout = 2
)
public Map<String, Object> selectDetail(
Map<String, Object> body,
TransactionContext context) {

ReservationDetailRequest request =
ReservationDetailRequest.from(body);

return service
.selectDetail(request, context)
.toMap();
}

@Transactional(timeout = 5)
public Map<String, Object> create(
Map<String, Object> body,
TransactionContext context) {

ReservationCreateRequest request =
ReservationCreateRequest.from(body);

return service
.create(request, context)
.toMap();
}
}

## Transaction Owner

본 장에서는 Facade를 Transaction Owner로 정한다.

\`\`\`text id=“impl34026” Handler → Transaction 없음

Facade → Transaction 경계

Service → 업무 처리

DAO·Mapper → Transaction 참여



따라서 Service의 \`create()\`에 다시 동일한 \`@Transactional\`을 중복 선언하지 않는다.

예외:

\`\`\`text id="impl34027"
Service가 여러 Facade와 비동기 Listener에서
독립적으로 호출돼야 하는 구조

에서는 별도 Transaction 정책을 설계할 수 있다.

기본 CRUD에서는 Facade와 Service 중 한 계층을 Transaction Owner로 명확히 정한다.

# 인증문맥 취득

화면 Body에서 다음 값을 받지 않는다.

\`\`\`text id=“impl34028” userId

branchId

roles

createdBy



검증된 TransactionContext에서 가져온다.

예:

\`\`\`java id="impl34029"
public record ActorContext(
String userId,
String branchId,
Set<String> roles
) {
public static ActorContext from(
TransactionContext context) {

String userId =
context.getHeader().getUserId();

String branchId =
context.getHeader().getBranchId();

Set<String> roles =
context.getAuthentication() != null
? Set.copyOf(
context.getAuthentication()
.getRoles()
)
: Set.of();

if (userId == null || userId.isBlank()) {
throw new BusinessException(
ErrorCode.AUTHENTICATION\_REQUIRED,
"인증 사용자정보가 없습니다."
);
}

return new ActorContext(
userId,
branchId,
roles
);
}
}

실제 TransactionContext의 인증정보 접근 API는 현재 TCF 버전에 맞춰 조정한다.

핵심은 다음이다.

\`\`\`text id=“impl34030” Request Body의 userId ≠ 인증 사용자

Header의 사용자정보 → JWT·SSO 검증 이후 값만 사용


\---

\# 34.1 목록·상세 조회

\## 34.1.1 목록조회 계약

\### 요청 예

\`\`\`json id="impl34031"
{
"customerNo": "C000012345",
"fromDate": "2026-07-01",
"toDate": "2026-07-31",
"statusCode": "READY",
"pageSize": 20,
"lastReservationDtm": null,
"lastReservationId": null
}

### 응답 예

json id="impl34032" { "items": \[ { "reservationId": "RSV-20260718-000001", "customerNo": "C0000\*\*\*\*\*", "reservationDtm": "2026-07-20T10:30:00", "purposeCode": "PRODUCT", "purposeName": "상품상담", "statusCode": "READY", "statusName": "예약", "ownerDisplayName": "홍\*동", "versionNo": 1, "editable": true, "cancellable": true } \], "pageSize": 20, "hasNext": false, "nextCursor": null }

## 34.1.2 목록 Request DTO

\`\`\`java id=“impl34033” public record ReservationListRequest( String customerNo, LocalDate fromDate, LocalDate toDate, String statusCode, Integer pageSize, LocalDateTime lastReservationDtm, String lastReservationId ) { public static ReservationListRequest from( Map<String, Object> body) {

Map<String, Object> safe =
body != null ? body : Map.of();

return new ReservationListRequest(
TextValues.trimToNull(
safe.get("customerNo")
),
DateValues.toLocalDate(
safe.get("fromDate")
),
DateValues.toLocalDate(
safe.get("toDate")
),
TextValues.trimToNull(
safe.get("statusCode")
),
NumberValues.toInteger(
safe.get("pageSize")
),
DateValues.toLocalDateTime(
safe.get("lastReservationDtm")
),
TextValues.trimToNull(
safe.get("lastReservationId")
)
);
}

}



변환 실패를 Null로 조용히 바꾸면 안 된다.

\`\`\`text id="impl34034"
pageSize = "ABC"

→ null

→ 기본값 20

으로 처리하면 잘못된 입력이 정상요청처럼 실행된다.

날짜·숫자 형식이 잘못됐으면 Validation 오류를 반환해야 한다.

## 34.1.3 목록 Rule

\`\`\`java id=“impl34035” @Component public class CtReservationRule {

private static final int DEFAULT\_PAGE\_SIZE = 20;
private static final int MAX\_PAGE\_SIZE = 100;
private static final int MAX\_SEARCH\_MONTHS = 3;

public ReservationListQuery buildListQuery(
ReservationListRequest request,
ActorContext actor) {

if (request == null) {
throw error(
"E-CT-RSV-0000",
"요청 Body가 없습니다."
);
}

LocalDate today =
LocalDate.now(ZoneId.of("Asia/Seoul"));

LocalDate fromDate =
request.fromDate() != null
? request.fromDate()
: today.minusMonths(1);

LocalDate toDate =
request.toDate() != null
? request.toDate()
: today;

if (fromDate.isAfter(toDate)) {
throw error(
"E-CT-RSV-0002",
"조회 시작일은 종료일보다 "
\+ "클 수 없습니다."
);
}

if (fromDate.plusMonths(
MAX\_SEARCH\_MONTHS
).isBefore(toDate)) {
throw error(
"E-CT-RSV-0002",
"조회기간은 최대 "
\+ MAX\_SEARCH\_MONTHS
\+ "개월입니다."
);
}

int pageSize =
request.pageSize() != null
? request.pageSize()
: DEFAULT\_PAGE\_SIZE;

if (pageSize < 1
|| pageSize > MAX\_PAGE\_SIZE) {
throw error(
"E-CT-RSV-0009",
"pageSize는 1 이상 "
\+ MAX\_PAGE\_SIZE
\+ " 이하여야 합니다."
);
}

validateStatusForSearch(
request.statusCode()
);

LocalDateTime fromDtm =
fromDate.atStartOfDay();

LocalDateTime toExclusiveDtm =
toDate.plusDays(1).atStartOfDay();

return new ReservationListQuery(
normalizeCustomerNo(
request.customerNo()
),
fromDtm,
toExclusiveDtm,
request.statusCode(),
actor.branchId(),
pageSize + 1,
request.lastReservationDtm(),
request.lastReservationId()
);
}

}


\---

\## 34.1.4 날짜조건

다음 SQL을 피한다.

\`\`\`sql id="impl34036"
WHERE TO\_CHAR(RESERVATION\_DTM, 'YYYYMMDD')
BETWEEN :fromDate
AND :toDate

이 방식은 Column에 함수를 적용해 일반 Index 사용을 어렵게 만들 수 있다.

권장:

sql id="impl34037" WHERE RESERVATION\_DTM >= :fromDtm AND RESERVATION\_DTM < :toExclusiveDtm

\`\`\`text id=“impl34038” 조회 종료일 2026-07-31

toExclusiveDtm 2026-08-01 00:00:00



반개방구간을 사용하면 시·분·초·밀리초 경계를 안전하게 처리할 수 있다.

\---

\## 34.1.5 Keyset Paging

상담예약 목록은 예약일시 내림차순으로 조회한다고 가정한다.

정렬값이 같은 예약이 여러 건 있을 수 있으므로 예약 ID를 Tie-breaker로 사용한다.

\`\`\`text id="impl34039"
ORDER BY RESERVATION\_DTM DESC,
RESERVATION\_ID DESC

다음 페이지 조건:

sql id="impl34040" AND ( r.RESERVATION\_DTM < #{lastReservationDtm} OR ( r.RESERVATION\_DTM = #{lastReservationDtm} AND r.RESERVATION\_ID < #{lastReservationId} ) )

정렬과 Cursor 조건의 방향이 일치해야 한다.

## 34.1.6 목록 Query DTO

java id="impl34041" public record ReservationListQuery( String customerNo, LocalDateTime fromDtm, LocalDateTime toExclusiveDtm, String statusCode, String permittedBranchId, int fetchSize, LocalDateTime lastReservationDtm, String lastReservationId ) { public boolean hasCursor() { return lastReservationDtm != null && lastReservationId != null; } }

fetchSize는 화면 Page Size보다 1 크게 설정한다.

\`\`\`text id=“impl34042” 화면 Page Size 20

DB 조회 21건

21건 조회됨 → hasNext=true → 응답은 앞의 20건



전체 Count SQL 없이 다음 페이지 존재 여부를 판단할 수 있다.

\---

\## 34.1.7 목록 Mapper Interface

\`\`\`java id="impl34043"
@Mapper
public interface CtReservationMapper {

List<ReservationListRow>
selectReservationList(
ReservationListQuery query
);

ReservationDetailRow
selectReservationDetail(
ReservationDetailQuery query
);

int countDuplicateReservation(
ReservationDuplicateQuery query
);

int insertReservation(
ReservationInsertRow row
);

int insertReservationHistory(
ReservationHistoryInsertRow row
);
}

Mapper Interface Method와 XML SQL ID는 정확히 같아야 한다.

## 34.1.8 목록 Mapper XML

\`\`\`xml id=“impl34044”

/\* SQL\_ID: CT-RSV-SEL-001 \*/

SELECT
r.RESERVATION\_ID
AS reservationId
, r.CUSTOMER\_NO
AS customerNo
, r.RESERVATION\_DTM
AS reservationDtm
, r.PURPOSE\_CD
AS purposeCode
, r.STATUS\_CD
AS statusCode
, r.OWNER\_USER\_ID
AS ownerUserId
, r.BRANCH\_ID
AS branchId
, r.VERSION\_NO
AS versionNo
FROM CT\_CONTACT\_RESERVATION r
WHERE r.RESERVATION\_DTM
\>= #{fromDtm}
AND r.RESERVATION\_DTM
< #{toExclusiveDtm}

<if test=
"customerNo != null
and customerNo != ''">
AND r.CUSTOMER\_NO
\= #{customerNo}
</if>

<if test=
"statusCode != null
and statusCode != ''">
AND r.STATUS\_CD
\= #{statusCode}
</if>

AND r.BRANCH\_ID
\= #{permittedBranchId}

<if test=
"lastReservationDtm != null
and lastReservationId != null">
AND (
r.RESERVATION\_DTM
< #{lastReservationDtm}
OR (
r.RESERVATION\_DTM
\= #{lastReservationDtm}
AND r.RESERVATION\_ID
< #{lastReservationId}
)
)
</if>

ORDER BY
r.RESERVATION\_DTM DESC
, r.RESERVATION\_ID DESC

FETCH FIRST #{fetchSize} ROWS ONLY


\---

\## 34.1.9 데이터권한을 SQL에 적용하는 이유

다음 방식은 권장하지 않는다.

\`\`\`text id="impl34045"
전체 지점 예약 조회

→ Java에서 다른 지점 Row 제거

문제:

\`\`\`text id=“impl34046” 불필요한 개인정보 DB 조회

Network·Heap 사용 증가

실수로 필터 누락 가능

Count와 목록 불일치

감사범위 확대



권한 Scope는 가능한 한 SQL 조건에 포함한다.

\`\`\`sql id="impl34047"
AND r.BRANCH\_ID = #{permittedBranchId}

관리자처럼 여러 지점을 조회할 수 있다면 허용 지점목록을 무제한 IN으로 전달하지 않는다.

대안:

\`\`\`text id=“impl34048” 권한 Scope Table Join

조직계층 Query

승인된 임시 권한 Table

별도 관리자 조회 ServiceId


\---

\## 34.1.10 목록 Row DTO

\`\`\`java id="impl34049"
public record ReservationListRow(
String reservationId,
String customerNo,
LocalDateTime reservationDtm,
String purposeCode,
String statusCode,
String ownerUserId,
String branchId,
long versionNo
) {}

DB Column명과 Java 필드명을 명시적으로 Mapping한다.

## 34.1.11 목록 Service

\`\`\`java id=“impl34050” @Service public class CtReservationService {

private final CtReservationRule rule;
private final CtReservationDao dao;
private final CtCustomerClient customerClient;
private final CtReferenceCodeReader codeReader;
private final ReservationIdGenerator idGenerator;

public ReservationListResponse selectList(
ReservationListRequest request,
TransactionContext context) {

ActorContext actor =
ActorContext.from(context);

ReservationListQuery query =
rule.buildListQuery(
request,
actor
);

List<ReservationListRow> rows =
dao.selectList(query);

int requestedPageSize =
query.fetchSize() - 1;

boolean hasNext =
rows.size() > requestedPageSize;

List<ReservationListRow> pageRows =
hasNext
? rows.subList(
0,
requestedPageSize
)
: rows;

List<ReservationListItem> items =
pageRows.stream()
.map(row ->
toListItem(
row,
actor
)
)
.toList();

ReservationCursor nextCursor =
hasNext && !pageRows.isEmpty()
? ReservationCursor.from(
pageRows.get(
pageRows.size() - 1
)
)
: null;

return ReservationListResponse.of(
context,
items,
requestedPageSize,
hasNext,
nextCursor
);
}

}


\---

\## 34.1.12 목록 N+1 방지

다음 구현을 피한다.

\`\`\`java id="impl34051"
for (ReservationListRow row : rows) {
String customerName =
customerClient.selectCustomerName(
row.customerNo()
);
}

목록 100건이면 고객 Service가 100번 호출된다.

대안:

\`\`\`text id=“impl34052” 목록에는 고객번호 마스킹만 표시

또는

고객 Read Model을 Batch·Event로 동기화

또는

고객번호 묶음조회 계약

또는

별도 화면 단계에서 선택 Row만 상세조회



도메인 소유권을 지키면서 N+1도 방지해야 한다.

\---

\## 34.1.13 코드명 조회

상담목적명과 상태명은 기준정보 Cache를 사용할 수 있다.

\`\`\`java id="impl34053"
private ReservationListItem toListItem(
ReservationListRow row,
ActorContext actor) {

String purposeName =
codeReader.findName(
"CT\_PURPOSE",
row.purposeCode()
);

String statusName =
codeReader.findName(
"CT\_RESERVATION\_STATUS",
row.statusCode()
);

boolean editable =
rule.canUpdate(row, actor);

boolean cancellable =
rule.canCancel(row, actor);

return new ReservationListItem(
row.reservationId(),
MaskingValues.customerNo(
row.customerNo()
),
row.reservationDtm(),
row.purposeCode(),
purposeName,
row.statusCode(),
statusName,
MaskingValues.userId(
row.ownerUserId()
),
row.versionNo(),
editable,
cancellable
);
}

Cache 장애 시 기준코드 원본조회 또는 안전한 코드값 표시정책을 적용한다.

## 34.1.14 목록 결과 없음

목록 결과가 0건인 것은 일반적으로 정상이다.

json id="impl34054" { "items": \[\], "pageSize": 20, "hasNext": false, "nextCursor": null }

다음처럼 업무 오류로 처리하지 않는다.

text id="impl34055" E-CT-RSV-0001 예약을 찾을 수 없습니다.

목록에서 0건은 정상 결과이고, 상세조회에서 0건은 NOT\_FOUND다.

## 34.1.15 상세조회 Request

\`\`\`java id=“impl34056” public record ReservationDetailRequest( String reservationId ) { public static ReservationDetailRequest from( Map<String, Object> body) {

if (body == null) {
return new ReservationDetailRequest(
null
);
}

return new ReservationDetailRequest(
TextValues.trimToNull(
body.get("reservationId")
)
);
}

}


\---

\## 34.1.16 상세 Query

\`\`\`java id="impl34057"
public record ReservationDetailQuery(
String reservationId,
String permittedBranchId
) {}

\`\`\`java id=“impl34058” public ReservationDetailQuery buildDetailQuery( ReservationDetailRequest request, ActorContext actor) {

if (request == null
|| request.reservationId() == null) {
throw validation(
"예약 ID는 필수입니다."
);
}

return new ReservationDetailQuery(
request.reservationId(),
actor.branchId()
);

}


\---

\## 34.1.17 상세 SQL

\`\`\`xml id="impl34059"
<select id="selectReservationDetail"
parameterType=
"com.nh.nsight.marketing.ct.reservation.application.dto.query.ReservationDetailQuery"
resultType=
"com.nh.nsight.marketing.ct.reservation.persistence.dto.ReservationDetailRow"
timeout="2">

/\* SQL\_ID: CT-RSV-SEL-002 \*/

SELECT
r.RESERVATION\_ID
AS reservationId
, r.CUSTOMER\_NO
AS customerNo
, r.RESERVATION\_DTM
AS reservationDtm
, r.PURPOSE\_CD
AS purposeCode
, r.MEMO\_CONTENT
AS memo
, r.STATUS\_CD
AS statusCode
, r.BRANCH\_ID
AS branchId
, r.OWNER\_USER\_ID
AS ownerUserId
, r.VERSION\_NO
AS versionNo
, r.CREATED\_BY
AS createdBy
, r.CREATED\_DTM
AS createdDtm
, r.UPDATED\_BY
AS updatedBy
, r.UPDATED\_DTM
AS updatedDtm
FROM CT\_CONTACT\_RESERVATION r
WHERE r.RESERVATION\_ID
\= #{reservationId}
AND r.BRANCH\_ID
\= #{permittedBranchId}
</select>

## 34.1.18 상세 결과 없음과 권한

권한조건을 SQL에 포함하면 조회 결과 0건은 다음 두 경우일 수 있다.

\`\`\`text id=“impl34060” 예약이 존재하지 않음

예약은 존재하지만 조회권한 없음



보안상 외부 사용자에게 두 경우를 동일하게 표시할 수 있다.

\`\`\`text id="impl34061"
E-CT-RSV-0001
예약을 찾을 수 없습니다.

이 방식은 다른 지점의 예약 존재 여부를 노출하지 않는다.

운영 감사로그에서는 내부적으로 다음을 구분할 수 있다.

\`\`\`text id=“impl34062” NOT\_FOUND

ACCESS\_SCOPE\_DENIED



단, 권한 확인을 위해 무권한 데이터의 개인정보를 불필요하게 조회하지 않는다.

\---

\## 34.1.19 상세 Service

\`\`\`java id="impl34063"
public ReservationDetailResponse selectDetail(
ReservationDetailRequest request,
TransactionContext context) {

ActorContext actor =
ActorContext.from(context);

ReservationDetailQuery query =
rule.buildDetailQuery(
request,
actor
);

ReservationDetailRow row =
dao.selectDetail(query);

if (row == null) {
throw new BusinessException(
CtReservationErrorCode.NOT\_FOUND,
"상담예약을 찾을 수 없습니다."
);
}

ReservationDetailResponse response =
toDetailResponse(
row,
actor
);

auditReservationInquiry(
row.reservationId(),
row.customerNo(),
context
);

return response;
}

## 34.1.20 개인정보 조회감사

상세조회에는 상담메모와 고객번호가 포함된다.

감사기록:

\`\`\`text id=“impl34064” eventType CT\_RESERVATION\_DETAIL\_VIEW

userId

branchId

reservationId

customerNoHash

serviceId

guid

traceId

result



상담메모 원문은 감사로그에 기록하지 않는다.

\---

\## 34.1.21 DAO 구현

\`\`\`java id="impl34065"
@Repository
public class CtReservationDao {

private final CtReservationMapper mapper;

public CtReservationDao(
CtReservationMapper mapper) {
this.mapper = mapper;
}

public List<ReservationListRow> selectList(
ReservationListQuery query) {

return mapper.selectReservationList(
query
);
}

public ReservationDetailRow selectDetail(
ReservationDetailQuery query) {

return mapper.selectReservationDetail(
query
);
}

public boolean existsDuplicate(
ReservationDuplicateQuery query) {

return mapper
.countDuplicateReservation(query)
\> 0;
}

public int insertReservation(
ReservationInsertRow row) {

return mapper.insertReservation(row);
}

public int insertHistory(
ReservationHistoryInsertRow row) {

return mapper
.insertReservationHistory(row);
}
}

DAO는 Mapper 영향 행 수를 그대로 Service에 전달한다.

DAO가 다음과 같이 실패를 숨기면 안 된다.

java id="impl34066" public void insertReservation( ReservationInsertRow row) { mapper.insertReservation(row); }

Service가 실제 1건 저장됐는지 검증할 수 없기 때문이다.

# 34.2 등록과 중복 방지

## 34.2.1 등록 요청 계약

json id="impl34067" { "customerNo": "C000012345", "reservationDtm": "2026-07-20T10:30:00", "purposeCode": "PRODUCT", "memo": "예금상품 상담 희망" }

화면이 전송하지 않는 값:

\`\`\`text id=“impl34068” reservationId

statusCode

branchId

ownerUserId

versionNo

createdBy

createdDtm



서버가 결정한다.

\---

\## 34.2.2 등록 Request DTO

\`\`\`java id="impl34069"
public record ReservationCreateRequest(
String customerNo,
LocalDateTime reservationDtm,
String purposeCode,
String memo
) {
public static ReservationCreateRequest from(
Map<String, Object> body) {

if (body == null) {
return null;
}

return new ReservationCreateRequest(
TextValues.trimToNull(
body.get("customerNo")
),
DateValues.toLocalDateTimeRequired(
body.get("reservationDtm"),
"reservationDtm"
),
TextValues.trimToNull(
body.get("purposeCode")
),
TextValues.trimToNull(
body.get("memo")
)
);
}
}

## 34.2.3 등록 Validation

\`\`\`java id=“impl34070” public void validateCreate( ReservationCreateRequest request) {

if (request == null) {
throw validation(
"요청 Body가 없습니다."
);
}

if (!StringUtils.hasText(
request.customerNo())) {
throw validation(
"고객번호는 필수입니다."
);
}

if (request.reservationDtm() == null) {
throw validation(
"예약일시는 필수입니다."
);
}

if (!StringUtils.hasText(
request.purposeCode())) {
throw validation(
"상담목적은 필수입니다."
);
}

if (request.memo() != null
&& request.memo().length() > 1000) {
throw validation(
"상담메모는 최대 1,000자입니다."
);
}

validateReservationDtm(
request.reservationDtm()
);

}



예약일시 정책 예:

\`\`\`text id="impl34071"
현재시각보다 과거
→ 등록 금지

최대 예약 가능일
→ 3개월 이후 금지

업무시간 외
→ 정책에 따라 금지

휴일
→ 영업일 Calendar 확인

## 34.2.4 상담목적 코드 검증

\`\`\`java id=“impl34072” if (!codeReader.exists( “CT\_PURPOSE”, request.purposeCode())) {

throw new BusinessException(
CtReservationErrorCode
.INVALID\_PURPOSE\_CODE,
"유효하지 않은 상담목적입니다."
);

}



화면 Select Box에 존재했다는 이유만으로 서버 검증을 생략하지 않는다.

\---

\## 34.2.5 고객 확인

\`\`\`java id="impl34073"
CustomerVerificationResult customer =
customerClient.verifyCustomer(
new CustomerVerificationRequest(
request.customerNo()
),
context
);

if (!customer.exists()) {
throw new BusinessException(
CtReservationErrorCode
.CUSTOMER\_NOT\_FOUND,
"고객을 찾을 수 없습니다."
);
}

if (!customer.available()) {
throw new BusinessException(
CtReservationErrorCode
.CUSTOMER\_NOT\_AVAILABLE,
"상담예약을 등록할 수 없는 "
\+ "고객상태입니다."
);
}

고객 연계 오류와 고객 없음은 구분한다.

\`\`\`text id=“impl34074” IC 응답 고객 없음 → 업무 오류

IC Timeout → 연계 시스템 오류

IC HTTP 500 → 연계 시스템 오류


\---

\## 34.2.6 외부 확인과 Transaction

권장 기본흐름:

\`\`\`text id="impl34075"
입력 Rule

→ 고객 존재 확인

→ 기준코드 확인

→ DB 변경 Transaction

외부 고객 확인을 DB Transaction 밖에서 수행하면 DB Connection 장기점유를 줄일 수 있다.

그러나 고객 상태가 확인 직후 바뀔 가능성은 존재한다.

따라서 다음을 판단해야 한다.

\`\`\`text id=“impl34076” 상담예약 등록에 고객상태의 강한 순간 일관성이 필요한가?

고객 도메인이 Version 또는 검증 Token을 제공할 수 있는가?

일정 시간 내 확인값을 허용할 수 있는가?



상담예약은 일반적으로 금융 원장 변경보다 위험도가 낮으므로 짧은 시간의 검증결과를 사용하는 구조가 가능하지만 업무 승인이 필요하다.

\---

\## 34.2.7 업무 중복과 요청 중복

두 중복은 다르다.

\### 업무 중복

\`\`\`text id="impl34077"
동일 고객

동일 예약일시

READY 상태 예약이 이미 존재

### 요청 중복

\`\`\`text id=“impl34078” 같은 등록 버튼 요청이

Network Retry로 두 번 도착



업무 중복은 Business Key와 DB Constraint로 통제한다.

요청 중복은 Idempotency Key로 통제한다.

\---

\## 34.2.8 업무 중복 Query

\`\`\`java id="impl34079"
public record ReservationDuplicateQuery(
String customerNo,
LocalDateTime reservationDtm
) {}

\`\`\`xml id=“impl34080”

/\* SQL\_ID: CT-RSV-SEL-003 \*/

SELECT COUNT(1)
FROM CT\_CONTACT\_RESERVATION r
WHERE r.CUSTOMER\_NO
\= #{customerNo}
AND r.RESERVATION\_DTM
\= #{reservationDtm}
AND r.STATUS\_CD
\= 'READY'


\---

\## 34.2.9 사전 중복조회만으로 부족한 이유

\`\`\`text id="impl34081"
요청 A
중복조회 0건

요청 B
중복조회 0건

요청 A
INSERT 성공

요청 B
INSERT 성공

두 요청이 동시에 실행되면 사전조회만으로 중복을 막을 수 없다.

DB가 최종 방어를 해야 한다.

## 34.2.10 Unique Constraint 대안

Oracle에서는 상태가 READY인 Row에만 단순 조건부 Unique Constraint를 직접 적용하기 어렵다.

대안 1: 활성 중복키 Column

\`\`\`text id=“impl34082” ACTIVE\_RESERVATION\_KEY

READY → CUSTOMER\_NO + 예약일시

COMPLETED·CANCELED → NULL



Oracle Unique Index는 여러 Null을 허용한다.

예:

\`\`\`sql id="impl34083"
ALTER TABLE CT\_CONTACT\_RESERVATION
ADD ACTIVE\_RESERVATION\_KEY
VARCHAR2(200);

CREATE UNIQUE INDEX
UX\_CT\_RSV\_ACTIVE
ON CT\_CONTACT\_RESERVATION (
ACTIVE\_RESERVATION\_KEY
);

등록 시:

text id="impl34084" ACTIVE\_RESERVATION\_KEY = CUSTOMER\_NO + '|' + YYYYMMDDHH24MISS

상태가 종료되면 NULL로 변경한다.

대안 2: Function-Based Unique Index

sql id="impl34085" CREATE UNIQUE INDEX UX\_CT\_RSV\_ACTIVE ON CT\_CONTACT\_RESERVATION ( CASE WHEN STATUS\_CD = 'READY' THEN CUSTOMER\_NO END, CASE WHEN STATUS\_CD = 'READY' THEN RESERVATION\_DTM END );

프로젝트 Oracle 표준과 DBA 검토를 거쳐 선택한다.

## 34.2.11 Idempotency Key

표준 Header 예:

json id="impl34086" { "serviceId": "CT.Reservation.create", "idempotencyKey": "CT-RSV-20260718-000001" }

Idempotency Key의 범위:

\`\`\`text id=“impl34087” 사용자

-   ServiceId
-   Idempotency Key



동일 Key를 다른 사용자가 재사용하지 못하게 한다.

\---

\## 34.2.12 Idempotency 처리상태

\`\`\`text id="impl34088"
RECEIVED

PROCESSING

SUCCESS

FAIL

UNKNOWN

동일 요청 재수신:

| 기존 상태 | 응답 |
| --- | --- |
| SUCCESS | 기존 성공결과 반환 |
| PROCESSING | 처리 중 응답 |
| FAIL | 오류정책에 따라 기존 오류 또는 재실행 |
| UNKNOWN | 결과조회 필요 |
| 없음 | 신규 실행 |

## 34.2.13 등록 Command

java id="impl34089" public record ReservationCreateCommand( String reservationId, String customerNo, LocalDateTime reservationDtm, String purposeCode, String memo, String statusCode, String branchId, String ownerUserId, long versionNo, String actorUserId, String traceId, String idempotencyKey, String activeReservationKey ) {}

Command는 Request에 서버 결정정보를 결합한 내부 계약이다.

## 34.2.14 Command 생성

\`\`\`java id=“impl34090” public ReservationCreateCommand buildCreateCommand( ReservationCreateRequest request, ActorContext actor, TransactionContext context, String idempotencyKey) {

return new ReservationCreateCommand(
idGenerator.newReservationId(),
normalizeCustomerNo(
request.customerNo()
),
request.reservationDtm(),
request.purposeCode(),
normalizeMemo(
request.memo()
),
ReservationStatus.READY.name(),
actor.branchId(),
actor.userId(),
1L,
actor.userId(),
context.getTraceId(),
idempotencyKey,
ReservationKeys.activeKey(
request.customerNo(),
request.reservationDtm()
)
);

}


\---

\## 34.2.15 Insert Row DTO

\`\`\`java id="impl34091"
public record ReservationInsertRow(
String reservationId,
String customerNo,
LocalDateTime reservationDtm,
String purposeCode,
String memo,
String statusCode,
String branchId,
String ownerUserId,
long versionNo,
String activeReservationKey,
String createdBy
) {
public static ReservationInsertRow from(
ReservationCreateCommand command) {

return new ReservationInsertRow(
command.reservationId(),
command.customerNo(),
command.reservationDtm(),
command.purposeCode(),
command.memo(),
command.statusCode(),
command.branchId(),
command.ownerUserId(),
command.versionNo(),
command.activeReservationKey(),
command.actorUserId()
);
}
}

## 34.2.16 Master INSERT SQL

\`\`\`xml id=“impl34092”

/\* SQL\_ID: CT-RSV-INS-001 \*/

INSERT INTO CT\_CONTACT\_RESERVATION (
RESERVATION\_ID
, CUSTOMER\_NO
, RESERVATION\_DTM
, PURPOSE\_CD
, MEMO\_CONTENT
, STATUS\_CD
, BRANCH\_ID
, OWNER\_USER\_ID
, VERSION\_NO
, ACTIVE\_RESERVATION\_KEY
, CREATED\_BY
, CREATED\_DTM
, UPDATED\_BY
, UPDATED\_DTM
)
VALUES (
#{reservationId}
, #{customerNo}
, #{reservationDtm}
, #{purposeCode}
, #{memo}
, #{statusCode}
, #{branchId}
, #{ownerUserId}
, #{versionNo}
, #{activeReservationKey}
, #{createdBy}
, SYSTIMESTAMP
, #{createdBy}
, SYSTIMESTAMP
)


\---

\## 34.2.17 History Row DTO

\`\`\`java id="impl34093"
public record ReservationHistoryInsertRow(
String historyId,
String reservationId,
String changeTypeCode,
String beforeStatusCode,
String afterStatusCode,
String changeSummary,
String changedBy,
String traceId
) {
public static ReservationHistoryInsertRow
forCreate(
ReservationCreateCommand command) {

return new ReservationHistoryInsertRow(
GuidGenerator.newGuid(),
command.reservationId(),
"CREATE",
null,
command.statusCode(),
"상담예약 신규 등록",
command.actorUserId(),
command.traceId()
);
}
}

## 34.2.18 History INSERT SQL

\`\`\`xml id=“impl34094”

/\* SQL\_ID: CT-RSV-INS-002 \*/

INSERT INTO
CT\_CONTACT\_RESERVATION\_HISTORY (
HISTORY\_ID
, RESERVATION\_ID
, CHANGE\_TYPE\_CD
, BEFORE\_STATUS\_CD
, AFTER\_STATUS\_CD
, CHANGE\_CONTENT
, CHANGED\_BY
, CHANGED\_DTM
, TRACE\_ID
)
VALUES (
#{historyId}
, #{reservationId}
, #{changeTypeCode}
, #{beforeStatusCode}
, #{afterStatusCode}
, #{changeSummary}
, #{changedBy}
, SYSTIMESTAMP
, #{traceId}
)


\---

\## 34.2.19 등록 Service

\`\`\`java id="impl34095"
public ReservationCreateResponse create(
ReservationCreateRequest request,
TransactionContext context) {

ActorContext actor =
ActorContext.from(context);

rule.validateCreate(request);

customerClient.verifyAvailableCustomer(
request.customerNo(),
context
);

rule.validatePurposeCode(
request.purposeCode()
);

String idempotencyKey =
context.getHeader()
.getIdempotencyKey();

IdempotencyDecision decision =
idempotencyService.begin(
context.getHeader().getServiceId(),
actor.userId(),
idempotencyKey
);

if (decision.isCompleted()) {
return ReservationCreateResponse
.fromSavedResult(
decision.savedResult()
);
}

ReservationDuplicateQuery duplicateQuery =
new ReservationDuplicateQuery(
request.customerNo(),
request.reservationDtm()
);

if (dao.existsDuplicate(
duplicateQuery)) {

throw new BusinessException(
CtReservationErrorCode.DUPLICATE,
"동일한 상담예약이 이미 존재합니다."
);
}

ReservationCreateCommand command =
rule.buildCreateCommand(
request,
actor,
context,
idempotencyKey
);

try {
int masterAffected =
dao.insertReservation(
ReservationInsertRow.from(
command
)
);

requireOneRow(
masterAffected,
"상담예약 Master 등록"
);

int historyAffected =
dao.insertHistory(
ReservationHistoryInsertRow
.forCreate(command)
);

requireOneRow(
historyAffected,
"상담예약 등록이력 저장"
);

ReservationCreateResponse response =
ReservationCreateResponse.of(
context,
command.reservationId(),
command.statusCode(),
command.versionNo()
);

idempotencyService.completeSuccess(
decision.executionId(),
response
);

return response;

} catch (DuplicateKeyException ex) {

throw new BusinessException(
CtReservationErrorCode.DUPLICATE,
"동일한 상담예약이 이미 존재합니다.",
ex
);
}
}

## 34.2.20 영향 행 수 검증

\`\`\`java id=“impl34096” private void requireOneRow( int affectedRows, String operation) {

if (affectedRows != 1) {
throw new SystemException(
CtReservationErrorCode
.PERSISTENCE\_ERROR,
operation
\+ " 결과가 1건이 아닙니다. "
\+ "affectedRows="
\+ affectedRows
);
}

}



판단:

\`\`\`text id="impl34097"
INSERT 1건
→ 정상

INSERT 0건
→ SQL·Trigger·설정 결함 가능

INSERT 2건 이상
→ 구조적 결함

MyBatis 단일 INSERT가 일반적으로 1을 반환하더라도 반드시 검사한다.

## 34.2.21 Master와 History Rollback

Facade의 @Transactional 범위에서 다음이 수행된다.

\`\`\`text id=“impl34098” Master INSERT 성공

History INSERT 실패

→ Runtime Exception

→ Transaction Rollback

→ Master도 취소



History 저장 예외를 다음처럼 숨기면 안 된다.

\`\`\`java id="impl34099"
try {
dao.insertHistory(history);
} catch (Exception ex) {
log.warn("이력 저장 실패", ex);
}

결과:

\`\`\`text id=“impl34100” Master 존재

History 없음



상담예약 Aggregate의 감사·변경이력 요구를 위반한다.

\---

\## 34.2.22 Idempotency 저장의 Transaction 위치

Idempotency 상태를 업무 DB Transaction과 어떻게 연결할지 결정해야 한다.

대안:

\### 같은 DB Transaction

\`\`\`text id="impl34101"
업무 Master

History

Idempotency SUCCESS

→ 같은 Commit

장점:

text id="impl34102" 업무 성공과 성공결과 상태 일치

### 별도 Transaction

\`\`\`text id=“impl34103” PROCESSING

→ 업무 Transaction

→ 별도 SUCCESS 기록



장점:

\`\`\`text id="impl34104"
업무 Rollback 후에도 요청상태 보존

위험:

\`\`\`text id=“impl34105” 업무 Commit 성공

SUCCESS 기록 실패

→ UNKNOWN



등록 거래에는 결과조회와 UNKNOWN 대사가 필요하다.

프로젝트의 공통 Idempotency Framework가 있다면 그 정책을 따른다.

\---

\## 34.2.23 등록 Response

\`\`\`java id="impl34106"
public record ReservationCreateResponse(
String reservationId,
String statusCode,
long versionNo,
LocalDateTime createdDtm,
String guid,
String traceId
) {
public static ReservationCreateResponse of(
TransactionContext context,
String reservationId,
String statusCode,
long versionNo) {

return new ReservationCreateResponse(
reservationId,
statusCode,
versionNo,
LocalDateTime.now(
ZoneId.of("Asia/Seoul")
),
context.getHeader().getGuid(),
context.getTraceId()
);
}

public Map<String, Object> toMap() {
return Map.of(
"reservationId", reservationId,
"statusCode", statusCode,
"versionNo", versionNo,
"createdDtm", createdDtm,
"guid", guid,
"traceId", traceId
);
}
}

정확한 생성시각이 중요하면 애플리케이션 현재시각보다 DB에서 저장된 CREATED\_DTM을 반환하는 구조를 검토한다.

## 34.2.24 등록 Timeout

다음 상황을 고려한다.

\`\`\`text id=“impl34107” DB Commit 완료

→ 응답 전송 중 Timeout

→ 화면은 실패로 인식

→ 사용자가 다시 등록



대응:

\`\`\`text id="impl34108"
동일 Idempotency Key 재사용

또는

예약 ID·GUID로 결과조회

또는

업무 중복키로 기존 예약 반환

사용자에게 단순히 다음 메시지만 보여서는 안 된다.

text id="impl34109" 등록에 실패했습니다. 다시 시도하세요.

권장:

\`\`\`text id=“impl34110” 처리결과를 확인하고 있습니다.

GUID G-20260718-000001

동일 요청으로 다시 시도할 경우 중복 등록되지 않습니다.


\---

\## 34.2.25 등록 감사

등록 감사 항목:

\`\`\`text id="impl34111"
reservationId

customerNoHash

reservationDtm

purposeCode

branchId

ownerUserId

actorUserId

serviceId

guid

traceId

result

상담메모 원문은 감사로그에 기록하지 않는다.

# 34.3 SQL과 트랜잭션

## 34.3.1 SQL 추적성

| 거래 | Mapper SQL ID | 관리 SQL ID | Table |
| --- | --- | --- | --- |
| 목록 | selectReservationList | CT-RSV-SEL-001 | Master |
| 상세 | selectReservationDetail | CT-RSV-SEL-002 | Master |
| 중복 | countDuplicateReservation | CT-RSV-SEL-003 | Master |
| 등록 | insertReservation | CT-RSV-INS-001 | Master |
| 이력 | insertReservationHistory | CT-RSV-INS-002 | History |

정방향 추적:

\`\`\`text id=“impl34112” CT-RSV-0001-E04

→ CT.Reservation.create

→ CtReservationHandler

→ CtReservationFacade.create

→ CtReservationService.create

→ insertReservation

→ CT-RSV-INS-001

→ CT\_CONTACT\_RESERVATION


\---

\## 34.3.2 Master DDL 예

\`\`\`sql id="impl34113"
CREATE TABLE CT\_CONTACT\_RESERVATION (
RESERVATION\_ID
VARCHAR2(40) NOT NULL,

CUSTOMER\_NO
VARCHAR2(30) NOT NULL,

RESERVATION\_DTM
TIMESTAMP NOT NULL,

PURPOSE\_CD
VARCHAR2(30) NOT NULL,

MEMO\_CONTENT
VARCHAR2(1000),

STATUS\_CD
VARCHAR2(20) NOT NULL,

BRANCH\_ID
VARCHAR2(20) NOT NULL,

OWNER\_USER\_ID
VARCHAR2(100) NOT NULL,

VERSION\_NO
NUMBER(18) NOT NULL,

ACTIVE\_RESERVATION\_KEY
VARCHAR2(200),

CREATED\_BY
VARCHAR2(100) NOT NULL,

CREATED\_DTM
TIMESTAMP NOT NULL,

UPDATED\_BY
VARCHAR2(100) NOT NULL,

UPDATED\_DTM
TIMESTAMP NOT NULL,

CONSTRAINT PK\_CT\_CONTACT\_RESERVATION
PRIMARY KEY (RESERVATION\_ID),

CONSTRAINT CK\_CT\_RSV\_STATUS
CHECK (
STATUS\_CD IN (
'READY',
'COMPLETED',
'CANCELED'
)
),

CONSTRAINT CK\_CT\_RSV\_VERSION
CHECK (VERSION\_NO >= 1)
);

## 34.3.3 History DDL 예

\`\`\`sql id=“impl34114” CREATE TABLE CT\_CONTACT\_RESERVATION\_HISTORY (

HISTORY\_ID
VARCHAR2(40) NOT NULL,

RESERVATION\_ID
VARCHAR2(40) NOT NULL,

CHANGE\_TYPE\_CD
VARCHAR2(20) NOT NULL,

BEFORE\_STATUS\_CD
VARCHAR2(20),

AFTER\_STATUS\_CD
VARCHAR2(20) NOT NULL,

CHANGE\_CONTENT
VARCHAR2(1000),

CHANGED\_BY
VARCHAR2(100) NOT NULL,

CHANGED\_DTM
TIMESTAMP NOT NULL,

TRACE\_ID
VARCHAR2(100),

CONSTRAINT PK\_CT\_RSV\_HISTORY
PRIMARY KEY (HISTORY\_ID)

);



History Table에 외래키를 적용할지 여부는 대량보관·Archive·삭제정책을 고려해 DBA와 결정한다.

\---

\## 34.3.4 Index 예

\`\`\`sql id="impl34115"
CREATE INDEX IX\_CT\_RSV\_CUST\_DTM
ON CT\_CONTACT\_RESERVATION (
CUSTOMER\_NO,
RESERVATION\_DTM DESC,
RESERVATION\_ID DESC
);

sql id="impl34116" CREATE INDEX IX\_CT\_RSV\_BRANCH\_DTM ON CT\_CONTACT\_RESERVATION ( BRANCH\_ID, RESERVATION\_DTM DESC, RESERVATION\_ID DESC );

sql id="impl34117" CREATE INDEX IX\_CT\_RSV\_HISTORY ON CT\_CONTACT\_RESERVATION\_HISTORY ( RESERVATION\_ID, CHANGED\_DTM DESC );

Index는 실제 주요 조회조건과 데이터분포를 이용한 실행계획으로 확정한다.

## 34.3.5 목록 SQL 주의사항

### SELECT \* 금지

필요한 Column만 조회한다.

### 앞부분 Wildcard 주의

sql id="impl34118" CUSTOMER\_NO LIKE '%123%'

고객번호 검색은 정확 일치 또는 승인된 Prefix 검색을 권장한다.

### 동적 정렬 문자열 치환 금지

xml id="impl34119" ORDER BY ${sortColumn}

SQL Injection 위험이 있다.

정렬이 필요하면 Allow List를 Java Enum 또는 MyBatis <choose>로 통제한다.

## 34.3.6 Count SQL 여부

목록 화면에서 전체 건수가 꼭 필요하지 않다면 Keyset Paging과 hasNext를 사용한다.

전체 Count가 필요한 경우 목록과 Count 조건을 하나의 공통 <sql> Fragment로 유지한다.

xml id="impl34120" <sql id="reservationSearchCondition"> ... </sql>

목록과 Count 조건이 다르면 다음 문제가 발생한다.

\`\`\`text id=“impl34121” 목록 18건

화면 Total Count 21건


\---

\## 34.3.7 조회 Transaction

\`\`\`java id="impl34122"
@Transactional(
readOnly = true,
timeout = 3
)

readOnly=true의 의미:

\`\`\`text id=“impl34123” 조회 목적을 선언한다.

일부 Framework·DB 최적화에 도움을 줄 수 있다.

실수로 변경 SQL을 실행하면 검토 대상이 된다.



\`readOnly=true\`만으로 DB가 모든 변경을 물리적으로 차단한다고 가정하지 않는다.

\---

\## 34.3.8 등록 Transaction

\`\`\`text id="impl34124"
Customer 연계
DB Transaction 밖

DB 중복 최종확인

Master INSERT

History INSERT

Idempotency 결과

Commit

Transaction 범위 안에서 외부 HTTP 호출·파일 처리·장시간 계산을 수행하지 않는다.

## 34.3.9 Transaction 전파

기본:

\`\`\`text id=“impl34125” Facade REQUIRED

Service Annotation 없음

DAO Annotation 없음



History 저장을 별도 \`REQUIRES\_NEW\`로 실행하지 않는다.

\`\`\`text id="impl34126"
Master Rollback

History Commit

처럼 실제 상태와 이력이 어긋날 수 있다.

일반 거래 History는 업무 Transaction에 참여한다.

감사로그는 별도 정책을 가질 수 있으나 업무 History와 구분한다.

## 34.3.10 오류 변환

DB Unique 위반:

\`\`\`text id=“impl34127” DuplicateKeyException

→ E-CT-RSV-0008

→ “동일한 상담예약이 이미 존재합니다.”



DB 연결 오류:

\`\`\`text id="impl34128"
DataAccessResourceFailureException

→ E-CT-RSV-9002

→ 시스템 오류·운영 Alert

Query Timeout:

\`\`\`text id=“impl34129” QueryTimeoutException

→ E-CT-RSV-9003

→ Timeout Metric



SQL·Table명·Connection 문자열을 사용자 메시지로 반환하지 않는다.

\---

\## 34.3.11 Transaction Rollback 규칙

기본:

\`\`\`text id="impl34130"
BusinessException
→ 프로젝트 정책에 따라
Rollback 여부 명시

SystemException
→ Rollback

Runtime DataAccessException
→ Rollback

등록 중 중복이 Master INSERT 전에 발견되면 변경이 없다.

Unique Constraint가 INSERT에서 발생하면 전체 Transaction이 Rollback돼야 한다.

프로젝트의 BusinessException Rollback 정책을 반드시 확인한다.

## 34.3.12 등록 완료조건

다음이 모두 충족돼야 등록 성공이다.

\`\`\`text id=“impl34131” Master 영향 행 1

History 영향 행 1

Transaction Commit 성공

Idempotency 결과 SUCCESS 또는 결과조회 가능

표준 응답 reservationId 포함

거래로그 SUCCESS

감사로그 등록 기록


\---

\# 34.4 단위·통합 테스트

\## 34.4.1 테스트 구조

\`\`\`text id="impl34132"
Rule Unit Test

DTO Conversion Test

Service Unit Test

Mapper Integration Test

Transaction Test

Idempotency·Concurrency Test

TCF Integration Test

Security Test

Performance Test

E2E Test

## 34.4.2 Rule Unit Test

\`\`\`java id=“impl34133” class CtReservationRuleTest {

private final CtReservationRule rule =
new CtReservationRule(
fixedClock,
codeReader,
idGenerator
);

@Test
void 목록조회\_기본값을\_적용한다() {
ReservationListRequest request =
new ReservationListRequest(
null,
null,
null,
null,
null,
null,
null
);

ReservationListQuery query =
rule.buildListQuery(
request,
actor()
);

assertThat(query.fetchSize())
.isEqualTo(21);
}

@Test
void 조회기간이\_3개월을\_초과하면\_오류다() {
ReservationListRequest request =
new ReservationListRequest(
null,
LocalDate.of(2026, 1, 1),
LocalDate.of(2026, 5, 1),
null,
20,
null,
null
);

assertThatThrownBy(() ->
rule.buildListQuery(
request,
actor()
)
).isInstanceOf(
BusinessException.class
);
}

@Test
void 메모가\_1000자를\_초과하면\_오류다() {
ReservationCreateRequest request =
new ReservationCreateRequest(
"C001",
futureReservationDtm(),
"PRODUCT",
"A".repeat(1001)
);

assertThatThrownBy(() ->
rule.validateCreate(request)
).isInstanceOf(
BusinessException.class
);
}

}


\---

\## 34.4.3 날짜 테스트

시스템 현재시각을 코드에서 직접 호출하면 테스트가 불안정해진다.

권장:

\`\`\`java id="impl34134"
@Configuration
public class TimeConfiguration {

@Bean
Clock businessClock() {
return Clock.system(
ZoneId.of("Asia/Seoul")
);
}
}

Rule은 Clock을 주입받는다.

\`\`\`text id=“impl34135” LocalDateTime.now() 직접 호출

→ 테스트 시점에 따라 결과 변경

Clock 주입 → 고정된 시각으로 테스트


\---

\## 34.4.4 Mapper 목록 테스트

검증:

\`\`\`text id="impl34136"
기간 시작 경계 포함

기간 종료 다음날 제외

상태조건

지점권한

고객번호

예약일시 동일 Row의 정렬

Cursor 다음 페이지

Page 중복 0

Page 누락 0

## 34.4.5 안정적 정렬 테스트

데이터:

\`\`\`text id=“impl34137” 예약 A 2026-07-20 10:00 ID 003

예약 B 2026-07-20 10:00 ID 002

예약 C 2026-07-20 10:00 ID 001



기대 순서:

\`\`\`text id="impl34138"
003

002

001

첫 페이지가 003, 002라면 다음 Cursor는 002이고 두 번째 페이지는 001부터 시작해야 한다.

## 34.4.6 상세조회 테스트

| 시나리오 | 기대 |
| --- | --- |
| 같은 지점 예약 | 상세 성공 |
| 존재하지 않는 ID | E-CT-RSV-0001 |
| 다른 지점 예약 | 외부에는 Not Found |
| 취소 예약 | 상세 정책에 따라 조회 |
| 메모 Null | 정상 |
| 고객번호 | 마스킹 응답 |
| 상세 성공 | 감사로그 생성 |

## 34.4.7 등록 Service Unit Test

\`\`\`java id=“impl34139” @Test void 등록시\_Master와\_History를\_저장한다() {

when(customerClient.verifyAvailableCustomer(
anyString(),
any()
)).thenReturn(
CustomerVerificationResult.available()
);

when(dao.existsDuplicate(any()))
.thenReturn(false);

when(dao.insertReservation(any()))
.thenReturn(1);

when(dao.insertHistory(any()))
.thenReturn(1);

ReservationCreateResponse response =
service.create(
validCreateRequest(),
transactionContext()
);

assertThat(response.statusCode())
.isEqualTo("READY");

verify(dao).insertReservation(any());
verify(dao).insertHistory(any());

}



내부 호출순서만 검증하지 않고 반환계약과 저장결과를 함께 검증한다.

\---

\## 34.4.8 중복 등록 테스트

\### 사전 중복

\`\`\`text id="impl34140"
DAO 중복조회
1건

기대
E-CT-RSV-0008

Master INSERT
0회

History INSERT
0회

### 동시 중복

\`\`\`text id=“impl34141” Thread A 중복조회 0

Thread B 중복조회 0

동시 INSERT

기대 한 건만 성공

다른 한 건 DB Unique 오류 → E-CT-RSV-0008



동시성 테스트는 실제 DB Unique Index를 사용해야 한다.

Mock만으로는 DB Race Condition을 검증할 수 없다.

\---

\## 34.4.9 Idempotency 테스트

| 시나리오 | 기대 |
|---|---|
| 신규 Key | 신규 등록 |
| 동일 Key 재요청 | 기존 결과 반환 |
| 동일 Key 동시 요청 | Master 1건 |
| Key 없이 등록 | 정책상 거절 또는 제한 허용 |
| 다른 사용자 같은 Key | 별도 범위 또는 거절 |
| PROCESSING 재요청 | 처리 중 응답 |
| UNKNOWN 재요청 | 결과조회 안내 |

\---

\## 34.4.10 Transaction Rollback 테스트

\`\`\`java id="impl34142"
@SpringBootTest
@Transactional
class CtReservationTransactionTest {

@Test
void History저장실패시\_Master도\_Rollback된다() {

historyFailureFixture
.failNextInsert();

assertThatThrownBy(() ->
facade.create(
validBody(),
context()
)
).isInstanceOf(
RuntimeException.class
);

assertThat(
reservationMapper
.countByBusinessKey(
customerNo,
reservationDtm
)
).isZero();

assertThat(
historyMapper
.countByReservationId(
generatedReservationId
)
).isZero();
}
}

테스트 자체에 @Transactional을 적용하면 테스트 종료 시 자동 Rollback되므로 실제 Facade Commit 확인이 가려질 수 있다.

Commit 검증이 필요하면 별도 Transaction 또는 테스트 DB 초기화 방식을 사용한다.

## 34.4.11 영향 행 수 테스트

| Mapper 결과 | 기대 |
| --- | --- |
| 1 | 성공 |
| 0 | 시스템 오류·Rollback |
| 2 | 시스템 오류·Rollback |

History INSERT 결과도 동일하게 검증한다.

## 34.4.12 고객 연계 테스트

\`\`\`text id=“impl34143” 고객 정상 → 등록 진행

고객 없음 → 업무 오류

고객 사용중지 → 업무 오류

IC Timeout → Master 0건

IC 시스템 오류 → Master 0건

잘못된 계약 Version → 연계 오류


\---

\## 34.4.13 TCF 통합 테스트

요청:

\`\`\`json id="impl34144"
{
"header": {
"businessCode": "CT",
"serviceId":
"CT.Reservation.create",
"transactionCode":
"CT-REG-0101",
"guid":
"G-TEST-CT-0001",
"traceId":
"T-TEST-CT-0001",
"idempotencyKey":
"I-TEST-CT-0001"
},
"body": {
"customerNo": "C000001",
"reservationDtm":
"2026-07-20T10:30:00",
"purposeCode": "PRODUCT",
"memo": "상품상담"
}
}

검증:

\`\`\`text id=“impl34145” STF 인증·권한 통과

Handler 선택

Facade Transaction

Master 1건

History 1건

표준 결과코드

GUID 유지

거래로그 SUCCESS


\---

\## 34.4.14 권한 테스트

| 시나리오 | 기대 |
|---|---|
| 조회권한 없음 | 403 |
| 등록권한 없음 | 403 |
| 같은 지점 목록 | 조회 |
| 다른 지점 목록 | 미노출 |
| Request branchId 위조 | 무시 |
| Request userId 위조 | 무시 |
| 인증정보 없음 | 401 |

\---

\## 34.4.15 성능 테스트

\### 목록

\`\`\`text id="impl34146"
운영 유사 데이터

최대 조회기간 3개월

Page Size 100

같은 예약일시 다수

지점별 데이터 편향

측정:

\`\`\`text id=“impl34147” p95

SQL p95

읽은 Row

실행계획

DB Pool Pending

응답 크기


\### 등록

\`\`\`text id="impl34148"
정상 등록 TPS

동일 키 중복

다른 키 동시 등록

고객 연계 지연

Unique Index 경합

History INSERT

## 34.4.16 로그 추적 테스트

한 등록 거래에 다음 값이 연결돼야 한다.

\`\`\`text id=“impl34149” 화면 이벤트 CT-RSV-0001-E04

ServiceId CT.Reservation.create

GUID G-TEST-CT-0001

Handler CtReservationHandler

SQL CT-RSV-INS-001 CT-RSV-INS-002

Table Master History

결과 SUCCESS



로그에 기록하지 않을 값:

\`\`\`text id="impl34150"
상담메모

고객번호 원문

JWT

Request Body 전체

# 표준 오류 처리

| 상황 | 오류코드 | 처리 |
| --- | --- | --- |
| 상세 없음 | E-CT-RSV-0001 | 업무 오류 |
| 기간 초과 | E-CT-RSV-0002 | Validation |
| 코드 오류 | E-CT-RSV-0003 | 업무 오류 |
| 고객 없음 | E-CT-RSV-0007 | 업무 오류 |
| 업무 중복 | E-CT-RSV-0008 | 업무 오류 |
| Page 초과 | E-CT-RSV-0009 | Validation |
| 고객 연계 | E-CT-RSV-9001 | 시스템·연계 |
| DB 오류 | E-CT-RSV-9002 | 시스템 |
| Timeout | E-CT-RSV-9003 | Timeout |
| 결과 불명 | E-CT-RSV-0012 | UNKNOWN |

# 정상 처리 흐름

## 목록조회

text id="impl34151" 1. 화면이 조회조건을 전송한다. 2. STF가 인증·조회권한을 검증한다. 3. Handler가 selectList를 선택한다. 4. Facade가 readOnly Transaction을 시작한다. 5. Rule이 기간·Page·상태를 검증한다. 6. 인증 지점으로 데이터 Scope를 생성한다. 7. Mapper가 Page Size+1건을 조회한다. 8. Service가 hasNext와 Cursor를 계산한다. 9. 고객번호와 사용자정보를 마스킹한다. 10. ETF가 표준응답을 반환한다.

## 상세조회

text id="impl34152" 1. 화면이 예약 ID를 전송한다. 2. STF가 상세조회권한을 확인한다. 3. Mapper가 예약 ID와 지점범위로 조회한다. 4. 결과가 없으면 Not Found를 반환한다. 5. 상세 Response를 생성한다. 6. 개인정보 조회감사를 저장한다. 7. 표준응답을 반환한다.

## 등록

text id="impl34153" 1. 화면이 등록정보와 Idempotency Key를 전송한다. 2. STF가 인증·등록권한을 검증한다. 3. Rule이 필수값·예약일시·메모를 검증한다. 4. 고객 도메인에서 고객을 확인한다. 5. 상담목적 코드를 확인한다. 6. Idempotency 상태를 확인한다. 7. 업무 중복을 사전 확인한다. 8. 서버가 예약 ID·상태·지점·사용자를 결정한다. 9. Master를 INSERT한다. 10. History를 INSERT한다. 11. 영향 행 수를 각각 확인한다. 12. Transaction을 Commit한다. 13. Idempotency 성공결과를 저장한다. 14. 등록 결과와 GUID를 반환한다.

# 오류·Timeout·장애 흐름

## 목록 SQL Timeout

\`\`\`text id=“impl34154” 목록 SQL 지연

→ Query Timeout

→ Transaction Rollback·종료

→ E-CT-RSV-9003

→ Slow SQL·Timeout Metric

→ DB Connection 반환 확인


\## 고객 연계 실패

\`\`\`text id="impl34155"
IC 고객 확인 실패

→ DB Transaction 시작 전 중단

→ Master 0건

→ History 0건

→ E-CT-RSV-9001

## DB Unique 충돌

\`\`\`text id=“impl34156” 동시 등록

→ 한 거래 INSERT 성공

→ 다른 거래 Unique 충돌

→ 전체 Rollback

→ E-CT-RSV-0008


\## History 실패

\`\`\`text id="impl34157"
Master INSERT

→ History INSERT 실패

→ 전체 Rollback

→ Master 0건

→ E-CT-RSV-9002

## 응답 전송 Timeout

\`\`\`text id=“impl34158” DB Commit 성공

→ 응답 전달 실패

→ Idempotency Key로 결과조회

→ 중복 등록 차단


\---

\# 정상 예시

\`\`\`text id="impl34159"
화면
CT-RSV-0001

이벤트
E04 신규 저장

ServiceId
CT.Reservation.create

입력
고객번호
예약일시
상담목적
메모

인증 사용자
U00001

인증 지점
B001

서버 생성
RSV-20260718-000001

상태
READY

Version
1

DB
Master 1건
History 1건

응답
SUCCESS
예약 ID·상태·Version·GUID

운영
거래로그·감사로그·Metric 정상

# 금지 예시

\`\`\`text id=“impl34160” 업무별 신규 Controller를 만든다.

Handler에서 Mapper를 직접 호출한다.

Handler가 Transaction을 시작한다.

Request Map을 그대로 Mapper에 전달한다.

잘못된 날짜형식을 Null로 바꾸고 기본값을 사용한다.

목록 0건을 오류로 반환한다.

상세 0건을 빈 객체로 반환한다.

목록 정렬에 유일한 Tie-breaker가 없다.

Offset 깊은 페이지를 무제한 허용한다.

목록에서 고객 Service를 행마다 호출한다.

다른 지점 데이터를 전체 조회 후 Java에서 제거한다.

화면의 branchId와 userId를 신뢰한다.

등록 ID와 상태를 화면이 결정한다.

중복을 화면 버튼 비활성화로만 막는다.

중복 사전조회만 하고 DB Unique를 적용하지 않는다.

Unique 위반을 시스템 메시지 그대로 반환한다.

INSERT 영향 행 수를 확인하지 않는다.

Master 저장 후 History 오류를 무시한다.

History를 REQUIRES\_NEW로 별도 Commit한다.

Facade와 Service에 이유 없이 동일 Transaction을 중복 선언한다.

Transaction 안에서 외부 고객 호출을 장시간 대기한다.

등록 Timeout 후 새로운 요청번호로 다시 저장한다.

상담메모와 고객번호 원문을 로그에 출력한다.

System.out으로 등록 흐름을 기록한다.

Rule·Mapper·Rollback·동시성 테스트를 생략한다.

ServiceId를 OM Catalog에 등록하지 않는다.


\---

\# 연계 규칙

\## 화면 연계

\`\`\`text id="impl34161"
화면 ID

→ 이벤트 ID

→ ServiceId

→ 거래코드

## 고객 도메인 연계

\`\`\`text id=“impl34162” CT.Reservation.create

→ CtCustomerClient

→ IC 고객 확인 계약

→ GUID·TraceId 유지

→ Parent ServiceId 기록



CT 업무가 IC Mapper나 Java Service Bean을 직접 참조하지 않는다.

\## 기준정보 연계

\`\`\`text id="impl34163"
상담목적 코드

예약상태 코드

→ 승인된 공통코드 Reader

## 운영 연계

\`\`\`text id=“impl34164” ServiceId

→ OM Catalog

→ 권한

→ Timeout

→ 거래통제

→ 로그·Metric


\---

\# 책임 경계와 RACI

| 활동 | 업무 | UI | 개발 | AA | DBA | 보안 | QA | 운영 |
|---|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|
| 입출력 계약 | A/C | R | R | C | I | C | C | I |
| 조회규칙 | A/R | C | R | C | C | C | C | I |
| 등록규칙 | A/R | C | R | C | C | C | C | I |
| Handler·Facade | I | I | A/R | C | I | I | C | I |
| DTO 구조 | C | C | A/R | C | I | C | C | I |
| SQL·Index | C | I | R | C | A/R | C | C | C |
| 중복·멱등성 | A/C | C | R | A | C | C | R/C | C |
| Transaction | C | I | R | A | C | I | R/C | I |
| 개인정보 | C | C | R | C | I | A/R | C | C |
| 테스트 | C | C | R | C | C | C | A/R | I |
| OM 등록 | I | I | R/C | C | I | C | C | A/R |
| 운영 Smoke | I | I | C | C | C | C | R/C | A/R |

\---

\# 성능·용량·확장성

\## 목록

\`\`\`text id="impl34165"
최대 조회기간
3개월

Page Size
최대 100

정렬
예약일시 + 예약 ID

목록 응답
상세 메모 제외

Count
필요성 검토

SQL Timeout
3초 이내

## 등록

\`\`\`text id=“impl34166” 고객 연계 Timeout

중복조회

Unique Index

Master INSERT

History INSERT

Idempotency



각 단계의 시간을 거래 Trace에서 분리할 수 있어야 한다.

\## Index

주요 조회축:

\`\`\`text id="impl34167"
지점 + 예약일시

고객 + 예약일시

예약 ID

활성 중복키

## History 증가

\`\`\`text id=“impl34168” 등록 1건당 History 최소 1건

수정·취소가 추가되면 Master보다 History가 빠르게 증가



보존·Partition·Archive는 전체 예상량으로 설계한다.

\---

\# 보안·개인정보·감사

\`\`\`text id="impl34169"
인증 사용자와 지점정보만 사용한다.

다른 지점 데이터는 SQL 단계에서 제한한다.

고객번호는 응답 목적에 맞게 마스킹한다.

상담메모는 목록과 로그에 포함하지 않는다.

상세조회는 개인정보 조회감사를 남긴다.

등록은 변경감사를 남긴다.

Idempotency 저장소에 Request Body 전체를 저장하지 않는다.

SQL Parameter 원문을 운영로그에 기록하지 않는다.

OM에서 상담예약 상세를 조회할 때도 권한과 감사를 적용한다.

# 운영·모니터링·장애 대응

## Metric

\`\`\`text id=“impl34170” ct.reservation.list.count

ct.reservation.list.duration

ct.reservation.detail.count

ct.reservation.detail.duration

ct.reservation.create.count

ct.reservation.create.duration

ct.reservation.duplicate.count

ct.reservation.idempotency.hit.count

ct.reservation.customer.error.count

ct.reservation.persistence.error.count

ct.reservation.timeout.count



Metric Label:

\`\`\`text id="impl34171"
serviceId

resultType

instanceId

version

금지 Label:

\`\`\`text id=“impl34172” reservationId

customerNo

userId

guid


\## 장애 점검순서

\`\`\`text id="impl34173"
1\. 화면 이벤트와 ServiceId 확인
2\. GUID·TraceId 확인
3\. 고객 연계 구간 확인
4\. Idempotency 상태 확인
5\. DB Pool Pending 확인
6\. SQL ID 처리시간 확인
7\. Master·History 건수 확인
8\. Unique 충돌 여부 확인
9\. 배포·설정 Version 확인
10\. 결과 UNKNOWN 대사

# 자동검증 및 품질 Gate

## Handler Gate

\`\`\`text id=“impl34174” ServiceId 상수

serviceIds 등록

Switch 분기

미지원 오류

DAO·Mapper 직접 호출 금지


\## DTO Gate

\`\`\`text id="impl34175"
Request·Query·Command·Row·Response 분리

userId·branchId Request 금지

날짜·숫자 변환 오류 명시

개인정보 최소화

## 조회 Gate

\`\`\`text id=“impl34176” 최대 조회기간

Page Size 상한

안정적 정렬

SQL 권한조건

SELECT \* 금지

전체 Count 필요성

목록 N+1 금지


\## 등록 Gate

\`\`\`text id="impl34177"
업무 중복키

DB Unique

Idempotency

서버 ID 생성

영향 행 1건

Master·History Transaction

Rollback Test

## SQL Gate

\`\`\`text id=“impl34178” Mapper Method와 XML ID 일치

Parameter Type 일치

Result Type 일치

SQL ID 주석

Timeout

Index 실행계획

문자열 치환 금지


\## 운영 Gate

\`\`\`text id="impl34179"
OM Catalog

권한

Timeout

거래통제

오류코드

감사

Metric

Runbook

# 테스트 시나리오

| ID | 시나리오 | 기대 결과 |
| --- | --- | --- |
| CT34-001 | 목록 기본조건 | 최근 1개월·20건 |
| CT34-002 | 목록 결과 없음 | 빈 목록 |
| CT34-003 | 조회기간 초과 | 업무 오류 |
| CT34-004 | 시작일 > 종료일 | Validation 오류 |
| CT34-005 | Page Size 100 | 성공 |
| CT34-006 | Page Size 101 | 오류 |
| CT34-007 | 상태코드 오류 | 오류 |
| CT34-008 | 같은 예약일시 다수 | 안정적 정렬 |
| CT34-009 | Cursor 다음 Page | 중복·누락 없음 |
| CT34-010 | 다른 지점 목록 | 미노출 |
| CT34-011 | 상세 정상 | 상세·감사 |
| CT34-012 | 상세 없음 | Not Found |
| CT34-013 | 다른 지점 상세 | Not Found |
| CT34-014 | 메모 Null | 정상 |
| CT34-015 | 등록 정상 | Master·History 1건 |
| CT34-016 | 고객번호 누락 | Validation |
| CT34-017 | 예약일시 누락 | Validation |
| CT34-018 | 목적코드 누락 | Validation |
| CT34-019 | 메모 1,001자 | 오류 |
| CT34-020 | 과거 예약일시 | 정책 오류 |
| CT34-021 | 고객 없음 | 업무 오류 |
| CT34-022 | 고객 사용불가 | 업무 오류 |
| CT34-023 | 고객 연계 Timeout | DB 변경 없음 |
| CT34-024 | 목적코드 오류 | 업무 오류 |
| CT34-025 | 사전 중복 발견 | INSERT 없음 |
| CT34-026 | 동시 중복 등록 | 1건 성공 |
| CT34-027 | 동일 Idempotency 재요청 | 기존 결과 |
| CT34-028 | 동일 Key 동시 요청 | 1건 저장 |
| CT34-029 | Master 영향 행 0 | Rollback |
| CT34-030 | History 영향 행 0 | 전체 Rollback |
| CT34-031 | History DB 오류 | Master 0건 |
| CT34-032 | Unique 위반 | 중복 오류 |
| CT34-033 | DB Connection 오류 | 시스템 오류 |
| CT34-034 | Query Timeout | Timeout 오류 |
| CT34-035 | 응답 전송 Timeout | 결과조회 가능 |
| CT34-036 | Request userId 위조 | 무시 |
| CT34-037 | Request branchId 위조 | 무시 |
| CT34-038 | 조회권한 없음 | 403 |
| CT34-039 | 등록권한 없음 | 403 |
| CT34-040 | JWT 없음 | 401 |
| CT34-041 | 상담메모 로그검색 | 원문 없음 |
| CT34-042 | 고객번호 로그검색 | 마스킹 |
| CT34-043 | 등록 GUID 검색 | 전체 경로 |
| CT34-044 | Mapper Namespace 오류 | 기동 실패 |
| CT34-045 | Mapper SQL ID 누락 | Gate 실패 |
| CT34-046 | 목록 실행계획 | Index 검증 |
| CT34-047 | 목록 최대조건 성능 | p95 충족 |
| CT34-048 | 등록 동시부하 | 중복·오류 정상 |
| CT34-049 | 배포 후 Smoke | 3개 거래 성공 |
| CT34-050 | OM Catalog 누락 | 배포 Gate 실패 |

# 따라 하는 실무 절차

## 1단계. 세 개 ServiceId를 등록한다

\`\`\`text id=“impl34180” CT.Reservation.selectList

CT.Reservation.selectDetail

CT.Reservation.create



완료 증적:

\`\`\`text id="impl34181"
Handler

OM Catalog

권한

거래코드

Timeout

## 2단계. Request와 내부 DTO를 만든다

\`\`\`text id=“impl34182” ListRequest

DetailRequest

CreateRequest

ListQuery

DetailQuery

CreateCommand


\## 3단계. Rule을 구현한다

\`\`\`text id="impl34183"
조회기간

Page Size

상태코드

예약일시

메모

상담목적

## 4단계. 목록·상세 SQL을 구현한다

완료 증적:

\`\`\`text id=“impl34184” 안정적 정렬

데이터권한

Paging

SQL Timeout

실행계획


\## 5단계. 등록 SQL과 Unique를 구현한다

\`\`\`text id="impl34185"
Master INSERT

History INSERT

Active Unique Key

## 6단계. Facade Transaction을 구현한다

Master·History Rollback Test를 먼저 수행한다.

## 7단계. 고객 Client와 Idempotency를 연결한다

실패·Timeout·재요청 정책을 검증한다.

## 8단계. 로그·감사·Metric을 연결한다

민감정보 원문이 없는지 확인한다.

## 9단계. 전체 테스트를 수행한다

\`\`\`text id=“impl34186” 정상

경계

권한

중복

Rollback

Timeout

성능


\## 10단계. 운영 Smoke와 추적성을 검증한다

화면 이벤트부터 DB와 거래로그까지 연결한다.

\---

\# 완료 체크리스트

\## 구조

| 점검 항목 | 완료 |
|---|:---:|
| 업무 Controller를 추가하지 않았다. | □ |
| Handler에 세 ServiceId가 등록됐다. | □ |
| Facade가 Transaction Owner다. | □ |
| Service가 업무흐름을 담당한다. | □ |
| DAO가 영향 행 수를 반환한다. | □ |
| Mapper Interface와 XML이 일치한다. | □ |

\## 조회

| 점검 항목 | 완료 |
|---|:---:|
| 목록과 상세 Request가 분리된다. | □ |
| 목록 0건은 정상이다. | □ |
| 상세 0건은 Not Found다. | □ |
| 조회기간 상한이 있다. | □ |
| Page Size 상한이 있다. | □ |
| 정렬에 고유 Tie-breaker가 있다. | □ |
| Cursor Paging이 안정적이다. | □ |
| 데이터권한이 SQL에 있다. | □ |
| 목록 N+1이 없다. | □ |
| 상세조회 감사가 있다. | □ |

\## 등록

| 점검 항목 | 완료 |
|---|:---:|
| 서버가 ID·상태·지점·사용자를 결정한다. | □ |
| 고객 유효성을 확인한다. | □ |
| 상담목적 코드를 확인한다. | □ |
| 업무 중복키가 확정됐다. | □ |
| DB Unique가 있다. | □ |
| Idempotency가 있다. | □ |
| Master 영향 행이 1건이다. | □ |
| History 영향 행이 1건이다. | □ |
| 둘이 같은 Transaction이다. | □ |
| Timeout 후 결과확인이 가능하다. | □ |

\## SQL·DB

| 점검 항목 | 완료 |
|---|:---:|
| SELECT \*가 없다. | □ |
| 문자열 치환 SQL이 없다. | □ |
| SQL ID가 있다. | □ |
| Query Timeout이 있다. | □ |
| 실행계획을 확인했다. | □ |
| Index의 DML 영향을 검증했다. | □ |
| History 보존정책을 검토했다. | □ |
| Unique 충돌을 표준 오류로 변환한다. | □ |

\## 보안·운영

| 점검 항목 | 완료 |
|---|:---:|
| Request userId·branchId를 신뢰하지 않는다. | □ |
| 고객번호를 마스킹한다. | □ |
| 메모를 로그에 기록하지 않는다. | □ |
| 등록 감사를 남긴다. | □ |
| OM Catalog가 있다. | □ |
| 거래별 Timeout이 있다. | □ |
| Metric과 Alert가 있다. | □ |
| GUID로 전체 거래가 추적된다. | □ |

\## 테스트

| 점검 항목 | 완료 |
|---|:---:|
| Rule Unit Test가 있다. | □ |
| DTO 변환 Test가 있다. | □ |
| Mapper Test가 있다. | □ |
| Paging 안정성 Test가 있다. | □ |
| 동시 중복 Test가 있다. | □ |
| Idempotency Test가 있다. | □ |
| Rollback Test가 있다. | □ |
| 권한 Test가 있다. | □ |
| Timeout Test가 있다. | □ |
| 성능 Test가 있다. | □ |
| 운영 Smoke가 있다. | □ |

\---

\# 변경·호환성·폐기 관리

\## 조회조건 추가

예:

\`\`\`text id="impl34187"
ownerUserId 조건 추가

확인:

\`\`\`text id=“impl34188” Request 선택필드

Rule

Query

SQL

Index

기존 화면

성능시험


\## 정렬 변경

정렬순서를 바꾸면 기존 Cursor와 호환되지 않을 수 있다.

\`\`\`text id="impl34189"
Cursor Version

정렬 Version

기존 Cursor 거절정책

을 검토한다.

## Page Size 변경

상한 증가 시:

\`\`\`text id=“impl34190” 응답 크기

SQL 시간

Heap

Network

화면 Rendering

개인정보 대량조회



를 시험한다.

\## 중복기준 변경

\`\`\`text id="impl34191"
고객 + 일시

→ 고객 + 지점 + 일시

변경 시:

\`\`\`text id=“impl34192” 기존 데이터 중복 대사

Unique Index 변경

Idempotency 영향

화면 안내

Migration



이 필요하다.

\## 등록 Request 필드 추가

선택필드로 시작하고 서버 기본값을 정의한다.

필수필드 추가는 구 화면과 Rolling 배포 호환성에 영향을 준다.

\## ServiceId 폐기

\`\`\`text id="impl34193"
Deprecated

→ 화면 호출 전환

→ 호출량 0

→ Handler 제거

→ OM Catalog 폐기

→ 테스트·문서 폐기

# 시사점

## 핵심 아키텍처 판단

첫째, 목록·상세·등록은 목적과 오류정책이 다르므로 독립 ServiceId와 DTO를 사용해야 한다.

둘째, 목록 결과 0건은 정상이며 상세 결과 0건은 명확한 Not Found 업무 오류다.

셋째, Paging은 Page Size만 적용한다고 완성되지 않으며 안정적인 정렬키와 Tie-breaker가 필요하다.

넷째, 데이터권한은 전체 데이터를 읽은 후 Java에서 제거하는 방식보다 SQL 범위에 반영하는 것이 안전하다.

다섯째, 등록 중복은 사전조회만으로 막을 수 없으며 DB Unique Constraint가 최종 방어선이어야 한다.

여섯째, 업무 중복과 동일 네트워크 요청의 재전송은 다른 문제이므로 Business Key와 Idempotency Key를 각각 관리해야 한다.

일곱째, Master와 History는 하나의 Aggregate 변경이므로 같은 Transaction 안에서 함께 Commit·Rollback돼야 한다.

여덟째, 현재 샘플 코드는 구조 학습에 활용할 수 있지만 인증문맥·영향 행·DB Unique·구조화 로그·테스트 측면의 운영 보완이 필요하다.

## 주요 위험

| 위험 | 결과 |
| --- | --- |
| Request Map 직접 사용 | 계약·형식 오류 |
| 변환 실패를 Null 처리 | 잘못된 요청 정상화 |
| 정렬 고유성 없음 | Page 중복·누락 |
| Offset 무제한 | 깊은 Page 성능저하 |
| 행별 고객 호출 | N+1 |
| Java 권한필터 | 개인정보 과다조회 |
| 목록·상세 결과정책 혼합 | 화면 오류 |
| 화면 사용자정보 신뢰 | 권한 위조 |
| 사전 중복조회만 적용 | 동시 중복 |
| DB Unique 없음 | 데이터 중복 |
| Idempotency 없음 | 재요청 중복 |
| 영향 행 미검사 | 저장 실패 은폐 |
| History 오류 무시 | Master·이력 불일치 |
| 외부 호출 장기 Transaction | Pool 점유 |
| Timeout 결과조회 없음 | 중복 재시도 |
| System.out 사용 | 운영로그 품질저하 |
| 테스트 부족 | Race·Rollback 결함 |

## 우선 보완 과제

1.  CT 업무 WAR와 실제 패키지 Root를 확정한다.
2.  상담예약 목록의 정렬·Cursor 계약을 확정한다.
3.  데이터권한 Scope와 관리자 조회정책을 확정한다.
4.  상담예약 Business Duplicate 기준을 업무 승인한다.
5.  Oracle 조건부 Unique 구현방식을 DBA와 확정한다.
6.  공통 Idempotency Framework의 적용방식을 확정한다.
7.  고객 확인 계약과 Timeout·오류 Mapping을 확정한다.
8.  Handler·Facade·Service·Rule·DAO·Mapper Template을 생성한다.
9.  Master·History DDL과 Index를 배포 Migration에 등록한다.
10.  동시 중복·Rollback·Timeout 테스트를 CI에 추가한다.
11.  현재 샘플의 System.out과 Request 사용자정보 사용패턴을 운영 표준에서 차단한다.
12.  목록·상세·등록 ServiceId를 OM Catalog와 거래통제에 등록한다.

## 중장기 발전 방향

\`\`\`text id=“impl34194” Map 기반 요청

→ Typed Contract

Offset Paging

→ Keyset Cursor

사전 중복조회

→ DB Unique + Idempotency

Request 사용자정보

→ 인증문맥

단일 INSERT

→ Master·History Aggregate

문자열 로그

→ 구조화 거래 추적

정상 테스트

→ 동시성·Rollback·Timeout 검증

수작업 추적성

→ 화면–ServiceId–SQL–Test 자동 Gate


\---

\# 마무리말

조회·등록 구현을 완료하려면 다음 질문에 답할 수 있어야 한다.

\`\`\`text id="impl34195"
새 업무 Controller를 만들지 않고
공통 TCF 경로를 사용하는가?

Handler가 지원 ServiceId를 명확히 선언하는가?

목록·상세·등록 DTO가 분리돼 있는가?

잘못된 입력형식을 기본값으로 숨기지 않는가?

목록 결과 없음과 상세 결과 없음을 구분하는가?

Paging 정렬이 항상 같은 순서를 보장하는가?

다음 Page에서 중복과 누락이 없는가?

조회권한이 SQL 범위에 반영되는가?

목록에서 다른 도메인을 행마다 호출하지 않는가?

등록 ID와 상태를 서버가 결정하는가?

업무 중복과 요청 중복을 구분하는가?

동시 등록을 DB Unique가 최종 차단하는가?

Idempotency 재요청은 기존 결과를 반환하는가?

Master와 History가 같은 Transaction인가?

각 INSERT 영향 행 수가 정확히 1건인가?

고객 연계 실패 시 DB 변경이 없는가?

등록 Timeout 후 실제 결과를 확인할 수 있는가?

로그에 고객번호와 메모 원문이 없는가?

GUID로 화면부터 SQL과 DB 결과까지 추적할 수 있는가?

정상뿐 아니라 경계·권한·중복·Rollback을 테스트했는가?

제34장의 핵심 흐름은 다음과 같다.

\`\`\`text id=“impl34196” ServiceId·입출력 계약

→ Typed DTO

→ Rule·권한 Scope

→ 목록·상세 SQL

→ 등록 중복·Idempotency

→ Master·History Transaction

→ 영향 행 검증

→ 표준 오류·응답

→ 테스트·운영 추적



가장 중요한 원칙은 다음과 같다.

\`\`\`text id="impl34197"
조회는 필요한 데이터만
안정된 순서와 허용된 범위로 반환해야 한다.

등록은 요청값을 그대로 저장하는 것이 아니라
서버가 업무규칙과 권한을 검증하고,

중복·재요청·부분 반영을 방지하면서
Master와 이력을 함께 Commit해야 한다.

SQL이 실행됐다는 사실이 아니라
정확한 결과와 운영 증적이 남았을 때
조회·등록 구현이 완료된다.
