<!-- source: ztcf-집필본/NSIGHT TCF Chapter 35- Reservation Update and Cancel Implementation.docx -->
<!-- tables: mammoth-html + placeholder table-to-md -->

# 제35장. 수정·취소·이력 구현

## 도입 전 안내말

제34장에서는 상담예약의 다음 거래를 구현했다.

\`\`\`text id=“chg35001” CT.Reservation.selectList

CT.Reservation.selectDetail

CT.Reservation.create



이번 장에서는 나머지 변경거래를 구현한다.

\`\`\`text id="chg35002"
CT.Reservation.update

CT.Reservation.cancel

수정과 취소는 등록보다 구현이 복잡하다.

등록은 일반적으로 존재하지 않는 데이터를 새로 생성한다.

반면 수정과 취소는 이미 존재하는 데이터의 현재 상태를 확인하고 변경해야 한다.

\`\`\`text id=“chg35003” 수정 요청

→ 예약 존재 여부

→ 조회·변경 권한

→ 현재 예약상태

→ 화면이 조회한 Version

→ 서버의 최신 Version

→ 변경 가능한 필드

→ 다른 사용자의 선행 변경

→ UPDATE 영향 행 수

→ 변경이력



초보 개발자는 수정 기능을 다음과 같이 구현하기 쉽다.

\`\`\`sql id="chg35004"
UPDATE CT\_CONTACT\_RESERVATION
SET RESERVATION\_DTM = :reservationDtm,
PURPOSE\_CD = :purposeCode,
MEMO\_CONTENT = :memo
WHERE RESERVATION\_ID = :reservationId;

이 SQL은 한 사용자가 혼자 사용하는 단순 프로그램에서는 동작할 수 있다.

그러나 실제 운영환경에서는 다음 상황이 발생한다.

\`\`\`text id=“chg35005” 사용자 A가 Version 3을 조회한다.

사용자 B도 Version 3을 조회한다.

사용자 A가 먼저 예약일시를 변경한다.

사용자 B가 이전 화면을 기준으로 메모를 변경한다.

Version 조건이 없으면 사용자 B의 UPDATE가 사용자 A의 변경을 덮어쓴다.



이를 \*\*Lost Update\*\*라고 한다.

예약 취소도 다음처럼 물리 삭제하면 안 된다.

\`\`\`sql id="chg35006"
DELETE FROM CT\_CONTACT\_RESERVATION
WHERE RESERVATION\_ID = :reservationId;

물리 삭제를 하면 다음 정보를 잃을 수 있다.

\`\`\`text id=“chg35007” 누가 취소했는가?

언제 취소했는가?

취소 전 예약상태는 무엇인가?

어떤 이유로 취소했는가?

같은 시간에 다시 예약할 수 있는가?

업무분쟁 발생 시 원래 데이터는 무엇이었는가?



따라서 상담예약 취소는 상태전이로 구현한다.

\`\`\`text id="chg35008"
READY

→ CANCELED

수정·취소 거래의 핵심은 SQL을 실행하는 것이 아니다.

\`\`\`text id=“chg35009” 현재 상태가 변경 가능한가?

요청자는 변경권한이 있는가?

화면의 Version이 최신인가?

동일 요청이 다시 들어오면 어떻게 응답하는가?

Master와 History가 함께 Commit되는가?

UPDATE 0건의 정확한 원인은 무엇인가?

Timeout 후 실제 변경결과를 확인할 수 있는가?



를 코드·SQL·테스트·로그로 증명하는 것이다.

원본 기준도 수정과 삭제는 현재 상태와 Version을 조건으로 수행하고, UPDATE 영향 행이 0이면 존재하지 않음·권한 부족·상태 변경·동시 수정 중 무엇인지 구분하도록 요구한다.

\---

\# 문서 개요

\## 목적

본 장의 목적은 상담예약 수정·취소 거래를 상태·권한·Version 기반으로 안전하게 구현하고, Master 변경과 업무 이력·감사정보를 일관되게 관리하는 것이다.

세부 목적은 다음과 같다.

\`\`\`text id="chg35010"
수정·취소 ServiceId 구현

현재 예약 조회와 변경조건 검증

상태별 수정·취소 권한 구현

Version 기반 낙관적 동시성 제어

Lost Update 방지

UPDATE 영향 행 0건 원인 분류

논리 취소와 활성 중복키 해제

수정·취소 반복요청 멱등성

Master·History 원자성 확보

변경 전후 데이터 비교

변경사유·사용자·Trace 기록

업무 History와 감사로그 역할 분리

안전한 오류 메시지와 원인 예외 보존

개인정보·메모 로그 노출 방지

정상·권한·상태·동시성·Rollback 테스트

운영 Metric·Alert·Runbook 정의

## 적용범위

| 구분 | 적용 대상 |
| --- | --- |
| 화면 | CT-RSV-0001 |
| 수정 이벤트 | CT-RSV-0001-E05 |
| 취소 이벤트 | CT-RSV-0001-E06 |
| 수정 ServiceId | CT.Reservation.update |
| 취소 ServiceId | CT.Reservation.cancel |
| Master | CT\_CONTACT\_RESERVATION |
| History | CT\_CONTACT\_RESERVATION\_HISTORY |
| 동시성 | VERSION\_NO |
| 상태 | READY·COMPLETED·CANCELED |
| 권한 | 기능권한·담당자·지점범위 |
| 중복통제 | 상태·Version·Idempotency |
| 감사 | 변경 전후·수행자·사유 |
| 운영 | 거래로그·Metric·OM·Alert |
| 테스트 | Unit·Mapper·Transaction·Concurrency·E2E |

## 대상 독자

\`\`\`text id=“chg35011” UPDATE SQL을 처음 구현하는 개발자

동시 수정 문제를 경험하지 못한 개발자

Version Column의 목적이 궁금한 개발자

논리 삭제와 물리 삭제를 구분해야 하는 개발자

변경이력과 감사로그의 차이를 이해해야 하는 개발자

UPDATE 0건을 어떻게 처리할지 어려운 개발자

AI가 생성한 단순 UPDATE 코드를 검토해야 하는 개발자

운영 가능한 CRUD 변경거래를 구현해야 하는 개발자


\## 선행조건

\`\`\`text id="chg35012"
제33장 상담예약 설계

제34장 조회·등록 구현

ServiceId·Handler 구조

Facade Transaction

AuthenticationContext

MyBatis UPDATE

낙관적 Lock

논리 삭제

Idempotency

오류코드·거래로그·감사로그

# 핵심 관점

\`\`\`text id=“chg35013” 수정·취소 구현의 핵심은 요청받은 값을 DB에 반영하는 것이 아니다.

현재 상태와 사용자 권한, 화면이 알고 있는 Version과 DB의 최신 Version을 비교한 뒤

허용된 변경만 정확히 한 건 처리하고, 그 변경 전후를 증명 가능한 이력으로 남기는 것이다.


\---

\# 학습 목표

이 장을 마치면 다음 내용을 설명하고 구현할 수 있어야 한다.

| 번호 | 학습 목표 |
|---:|---|
| 1 | 수정과 취소를 독립 ServiceId로 구현한다. |
| 2 | 현재 예약상태를 조회한 뒤 업무규칙을 검증한다. |
| 3 | 기능권한과 데이터 변경권한을 구분한다. |
| 4 | READY 상태에서만 일반 수정하도록 통제한다. |
| 5 | 취소를 물리 DELETE가 아닌 상태전이로 구현한다. |
| 6 | Version 기반 낙관적 Lock을 구현한다. |
| 7 | Lost Update의 발생과 방지방법을 설명한다. |
| 8 | 사전 Version 검증과 UPDATE Version 조건의 차이를 설명한다. |
| 9 | UPDATE 영향 행 0건을 성공으로 처리하지 않는다. |
| 10 | 존재하지 않음·무권한·상태 충돌·Version 충돌을 구분한다. |
| 11 | 수정 가능 필드를 Allow List로 제한한다. |
| 12 | 화면이 보낸 상태·사용자·지점값을 신뢰하지 않는다. |
| 13 | 취소 시 활성 중복키를 해제한다. |
| 14 | 같은 취소요청이 반복됐을 때의 계약을 정의한다. |
| 15 | Version과 Idempotency의 차이를 설명한다. |
| 16 | Idempotency Key와 Request Fingerprint를 검증한다. |
| 17 | Master와 History를 같은 Transaction으로 처리한다. |
| 18 | 변경 전후 값과 변경 필드를 이력으로 남긴다. |
| 19 | History와 감사로그를 구분한다. |
| 20 | 민감정보를 이력·로그에서 보호한다. |
| 21 | 오류 유형별 사용자 행동과 운영 대응을 구분한다. |
| 22 | 원인 예외를 보존하면서 안전한 응답을 생성한다. |
| 23 | 동시 수정·중복 취소·Rollback 테스트를 구현한다. |
| 24 | 변경거래를 GUID·Version·History로 추적한다. |
| 25 | 수정·취소 품질 Gate를 적용한다. |

\---

\# 핵심 용어

| 용어 | 의미 |
|---|---|
| Update | 기존 Aggregate의 허용된 속성을 변경하는 거래 |
| Cancel | 현재 상태를 취소상태로 전이하는 거래 |
| Lost Update | 다른 사용자의 선행 변경을 이전 데이터로 덮어쓰는 문제 |
| Optimistic Lock | Version 조건으로 동시 수정 충돌을 발견하는 방식 |
| Pessimistic Lock | DB Row Lock을 먼저 확보하고 변경하는 방식 |
| Version | 데이터 변경세대를 나타내는 번호 |
| State Guard | 현재 상태가 변경 가능한지 확인하는 조건 |
| Authorization Guard | 요청자가 해당 데이터를 변경할 수 있는지 확인하는 조건 |
| Affected Rows | UPDATE가 실제로 변경한 행 수 |
| Logical Cancel | Row를 삭제하지 않고 \`CANCELED\`로 변경 |
| Tombstone | 삭제·취소됐음을 나타내는 상태정보 |
| Idempotency | 동일 요청이 반복돼도 한 번만 업무효과가 발생하는 성질 |
| Request Fingerprint | Idempotency Key와 요청내용의 일치 여부를 확인하는 Hash |
| Retry | 같은 요청을 다시 수행 |
| Replay | 과거 요청이 다시 전달되는 상황 |
| Before Image | 변경 전 데이터 |
| After Image | 변경 후 데이터 |
| Change Set | 실제 변경된 필드 목록 |
| Domain History | 업무 Aggregate의 상태변경 이력 |
| Audit Log | 누가 어떤 중요행위를 수행했는지 증명하는 기록 |
| Immutable History | 저장 후 일반 수정·삭제하지 않는 이력 |
| Compensation | 잘못된 변경을 반대 거래로 복구하는 방식 |
| Restore | 취소 데이터를 다시 활성화하는 별도 업무 |
| UNKNOWN | 응답은 불명확하지만 Commit 여부를 확정하지 못한 상태 |

\---

\# 수정·취소 전체 구현 흐름

\## 수정

\`\`\`text id="chg35014"
화면

→ CT.Reservation.update

→ STF 인증·기능권한

→ CtReservationHandler

→ CtReservationFacade.update

→ 현재 예약 조회

→ 데이터권한 확인

→ READY 상태 확인

→ 요청 Version 확인

→ 입력·코드 검증

→ Version 조건 UPDATE

→ 영향 행 1건 확인

→ UPDATE History INSERT

→ Commit

→ Version+1 응답

## 취소

\`\`\`text id=“chg35015” 화면

→ CT.Reservation.cancel

→ STF 인증·취소권한

→ CtReservationHandler

→ CtReservationFacade.cancel

→ 현재 예약 조회

→ 데이터권한 확인

→ READY 상태 확인

→ 요청 Version 확인

→ 취소사유 확인

→ 상태 CANCELED UPDATE

→ 활성 중복키 NULL 처리

→ 영향 행 1건 확인

→ CANCEL History INSERT

→ Commit

→ CANCELED·Version+1 응답


\---

\# 현재 기준 소스에서 참고할 패턴과 보완사항

현재 기준 소스의 OM 사용자·메뉴·오류코드 관리에는 다음과 같은 패턴이 존재한다.

\`\`\`text id="chg35016"
USE\_YN='N'으로 논리 비활성화

DAO가 UPDATE 영향 행 수 반환

영향 행 0건이면 Not Found 오류

이 패턴은 물리 삭제를 피하고 영향 행을 확인한다는 점에서 참고할 수 있다.

그러나 상담예약에는 다음 보완이 필요하다.

| 기존 단순 패턴 | 상담예약 목표 |
| --- | --- |
| PK 조건만 UPDATE | PK+상태+Version+권한 Scope |
| USE\_YN='N' | STATUS\_CD='CANCELED' |
| Version 없음 | VERSION\_NO 증가 |
| 영향 행 0건=Not Found | 원인별 분류 |
| 변경 전후 이력 제한 | Field-level History |
| 반복요청 계약 없음 | Idempotency |
| 취소사유 없음 | 필수 취소사유 |
| 활성 중복키 유지 | 취소 시 해제 |

현재 소스의 단순 논리 비활성화 패턴을 그대로 복사하지 않고 상담예약의 상태·동시성·업무 이력 요구를 추가한다.

# 목표 Handler 확장

\`\`\`java id=“chg35017” @Component public class CtReservationHandler implements TransactionHandler {

public static final String SELECT\_LIST =
"CT.Reservation.selectList";

public static final String SELECT\_DETAIL =
"CT.Reservation.selectDetail";

public static final String CREATE =
"CT.Reservation.create";

public static final String UPDATE =
"CT.Reservation.update";

public static final String CANCEL =
"CT.Reservation.cancel";

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
CREATE,
UPDATE,
CANCEL
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

case UPDATE ->
facade.update(body, context);

case CANCEL ->
facade.cancel(body, context);

default ->
throw new BusinessException(
ErrorCode.SERVICE\_NOT\_FOUND,
"지원하지 않는 ServiceId입니다."
);
};
}

}


\---

\# Facade 확장

\`\`\`java id="chg35018"
@Transactional(timeout = 5)
public Map<String, Object> update(
Map<String, Object> body,
TransactionContext context) {

ReservationUpdateRequest request =
ReservationUpdateRequest.from(body);

return service
.update(request, context)
.toMap();
}

@Transactional(timeout = 5)
public Map<String, Object> cancel(
Map<String, Object> body,
TransactionContext context) {

ReservationCancelRequest request =
ReservationCancelRequest.from(body);

return service
.cancel(request, context)
.toMap();
}

수정과 취소는 Master와 History가 함께 처리되므로 Facade의 동일 Transaction 안에서 실행한다.

# 35.1 상태별 변경 권한

## 35.1.1 수정 Request DTO

\`\`\`java id=“chg35019” public record ReservationUpdateRequest( String reservationId, LocalDateTime reservationDtm, String purposeCode, String memo, Long versionNo, String changeReason ) { public static ReservationUpdateRequest from( Map<String, Object> body) {

if (body == null) {
return null;
}

return new ReservationUpdateRequest(
TextValues.trimToNull(
body.get("reservationId")
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
),
NumberValues.toLong(
body.get("versionNo")
),
TextValues.trimToNull(
body.get("changeReason")
)
);
}

}



화면이 수정 요청에 포함하지 않을 값:

\`\`\`text id="chg35020"
customerNo

statusCode

branchId

ownerUserId

updatedBy

updatedDtm

새 Version

고객번호·등록 지점·담당자는 일반 수정에서 변경할 수 없는 필드다.

## 35.1.2 취소 Request DTO

\`\`\`java id=“chg35021” public record ReservationCancelRequest( String reservationId, Long versionNo, String cancelReason ) { public static ReservationCancelRequest from( Map<String, Object> body) {

if (body == null) {
return null;
}

return new ReservationCancelRequest(
TextValues.trimToNull(
body.get("reservationId")
),
NumberValues.toLong(
body.get("versionNo")
),
TextValues.trimToNull(
body.get("cancelReason")
)
);
}

}


\---

\## 35.1.3 현재 데이터 조회

수정·취소 전 현재 Master를 조회한다.

\`\`\`java id="chg35022"
public record ReservationCurrentQuery(
String reservationId,
String permittedBranchId
) {}

java id="chg35023" public record ReservationCurrentRow( String reservationId, String customerNo, LocalDateTime reservationDtm, String purposeCode, String memo, String statusCode, String branchId, String ownerUserId, long versionNo, String activeReservationKey, String updatedBy, LocalDateTime updatedDtm ) {}

Mapper:

\`\`\`xml id=“chg35024”

/\* SQL\_ID: CT-RSV-SEL-004 \*/

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
, r.ACTIVE\_RESERVATION\_KEY
AS activeReservationKey
, r.UPDATED\_BY
AS updatedBy
, r.UPDATED\_DTM
AS updatedDtm
FROM CT\_CONTACT\_RESERVATION r
WHERE r.RESERVATION\_ID
\= #{reservationId}
AND r.BRANCH\_ID
\= #{permittedBranchId}



기본 구현에서는 \`FOR UPDATE\`를 사용하지 않는다.

Version 기반 낙관적 Lock으로 충돌을 검출한다.

\---

\## 35.1.4 낙관적 Lock과 비관적 Lock

\### 낙관적 Lock

\`\`\`text id="chg35025"
현재 데이터 조회

→ 업무검증

→ Version 조건 UPDATE

→ 충돌 시 0건

장점:

\`\`\`text id=“chg35026” DB Row Lock 보유시간이 짧다.

사용자 Think Time 동안 Lock을 잡지 않는다.

일반 온라인 CRUD에 적합하다.


\### 비관적 Lock

\`\`\`sql id="chg35027"
SELECT ...
FROM CT\_CONTACT\_RESERVATION
WHERE RESERVATION\_ID = :id
FOR UPDATE;

장점:

text id="chg35028" Transaction 중 다른 변경을 차단한다.

위험:

\`\`\`text id=“chg35029” Lock 대기

Deadlock

Connection 장기점유

사용자 화면 대기와 결합 시 장애



상담예약 일반 수정에는 낙관적 Lock을 기본 적용한다.

재고차감·잔액변경처럼 강한 직렬화가 필요한 거래는 별도 검토한다.

\---

\## 35.1.5 상태별 허용행위

| 현재 상태 | 조회 | 수정 | 취소 | 완료 |
|---|:---:|:---:|:---:|:---:|
| READY | O | O | O | O |
| COMPLETED | O | X | X | 이미 완료 |
| CANCELED | O | X | X | X |

관리자 복구가 필요하면 기존 \`update\`나 \`cancel\`에 옵션을 추가하지 않는다.

\`\`\`text id="chg35030"
CT.Reservation.restore

와 같은 별도 승인형 ServiceId를 설계한다.

## 35.1.6 기능권한과 데이터권한

### 기능권한

\`\`\`text id=“chg35031” CT\_RESERVATION\_UPDATE

CT\_RESERVATION\_CANCEL



STF 또는 공통 권한계층에서 확인한다.

\### 데이터권한

\`\`\`text id="chg35032"
예약 담당자

또는

같은 지점의 관리자

Rule에서 현재 예약과 인증문맥을 비교한다.

## 35.1.7 변경권한 Rule

\`\`\`java id=“chg35033” public void validateUpdateAuthority( ReservationCurrentRow current, ActorContext actor) {

boolean owner =
actor.userId().equals(
current.ownerUserId()
);

boolean branchManager =
actor.roles().contains(
"CT\_BRANCH\_MANAGER"
)
&& actor.branchId().equals(
current.branchId()
);

if (!owner && !branchManager) {
throw new BusinessException(
CtReservationErrorCode
.UPDATE\_FORBIDDEN,
"상담예약을 변경할 권한이 없습니다."
);
}

}



권한 Rule은 화면에서 버튼을 숨겼는지 여부와 관계없이 서버에서 실행한다.

\---

\## 35.1.8 수정 상태 Rule

\`\`\`java id="chg35034"
public void validateUpdatableStatus(
ReservationCurrentRow current) {

if (!ReservationStatus.READY.name()
.equals(current.statusCode())) {

throw new BusinessException(
CtReservationErrorCode
.INVALID\_STATUS,
"현재 상태에서는 상담예약을 "
\+ "수정할 수 없습니다."
);
}
}

## 35.1.9 취소 상태 Rule

\`\`\`java id=“chg35035” public void validateCancelableStatus( ReservationCurrentRow current) {

if (!ReservationStatus.READY.name()
.equals(current.statusCode())) {

throw new BusinessException(
CtReservationErrorCode
.INVALID\_STATUS,
"현재 상태에서는 상담예약을 "
\+ "취소할 수 없습니다."
);
}

}


\---

\## 35.1.10 Version 사전검증

\`\`\`java id="chg35036"
public void validateVersion(
long requestVersion,
ReservationCurrentRow current) {

if (requestVersion != current.versionNo()) {
throw new BusinessException(
CtReservationErrorCode
.CONCURRENT\_MODIFICATION,
"다른 사용자가 먼저 변경했습니다. "
\+ "최신 내용을 다시 조회해 주세요."
);
}
}

사전검증은 사용자에게 빠르게 명확한 오류를 반환할 수 있다.

그러나 사전검증만으로 동시성 문제가 완전히 해결되지는 않는다.

\`\`\`text id=“chg35037” 사전조회

→ Version 3 일치

→ 검증 중 다른 거래가 Version 4로 변경

→ 현재 거래 UPDATE



따라서 UPDATE SQL에도 반드시 Version 조건을 포함한다.

\---

\## 35.1.11 수정 가능 필드 Allow List

수정 허용:

\`\`\`text id="chg35038"
reservationDtm

purposeCode

memo

변경 금지:

\`\`\`text id=“chg35039” reservationId

customerNo

statusCode

branchId

ownerUserId

versionNo 직접 지정

createdBy

createdDtm



다음과 같은 범용 Map 업데이트를 금지한다.

\`\`\`java id="chg35040"
for (Map.Entry<String, Object> field
: request.entrySet()) {
updateColumn(field.getKey(), field.getValue());
}

Column 이름을 화면에서 전달받는 동적 업데이트는 권한우회와 SQL Injection 위험이 있다.

## 35.1.12 수정 Command

java id="chg35041" public record ReservationUpdateCommand( String reservationId, LocalDateTime reservationDtm, String purposeCode, String memo, long expectedVersionNo, String actorUserId, String actorBranchId, String traceId, String changeReason, String newActiveReservationKey ) {}

## 35.1.13 수정 Row DTO

java id="chg35042" public record ReservationUpdateRow( String reservationId, LocalDateTime reservationDtm, String purposeCode, String memo, long expectedVersionNo, String actorUserId, String actorBranchId, String activeReservationKey ) {}

## 35.1.14 수정 SQL

\`\`\`xml id=“chg35043”

/\* SQL\_ID: CT-RSV-UPD-001 \*/

UPDATE CT\_CONTACT\_RESERVATION
SET RESERVATION\_DTM
\= #{reservationDtm}
, PURPOSE\_CD
\= #{purposeCode}
, MEMO\_CONTENT
\= #{memo}
, ACTIVE\_RESERVATION\_KEY
\= #{activeReservationKey}
, VERSION\_NO
\= VERSION\_NO + 1
, UPDATED\_BY
\= #{actorUserId}
, UPDATED\_DTM
\= SYSTIMESTAMP
WHERE RESERVATION\_ID
\= #{reservationId}
AND STATUS\_CD
\= 'READY'
AND VERSION\_NO
\= #{expectedVersionNo}
AND BRANCH\_ID
\= #{actorBranchId}



담당자 본인 또는 지점관리자 같은 복합권한은 사전 Rule로 확인하되 SQL에는 최소한 지점 Scope를 포함한다.

담당자 본인만 수정 가능한 정책이면 SQL에도 다음 조건을 추가할 수 있다.

\`\`\`sql id="chg35044"
AND OWNER\_USER\_ID = :actorUserId

## 35.1.15 수정으로 중복키 변경

예약일시가 변경되면 활성 중복키도 바뀐다.

\`\`\`text id=“chg35045” 기존 C001|20260720103000

변경 C001|20260721110000



UPDATE 전에 새 Business Key 중복을 확인하고 DB Unique Constraint로 최종 통제한다.

\`\`\`text id="chg35046"
사전 중복조회

\+ UPDATE Unique Constraint

수정에서도 Race Condition이 발생할 수 있다.

## 35.1.16 취소 Command

java id="chg35047" public record ReservationCancelCommand( String reservationId, long expectedVersionNo, String actorUserId, String actorBranchId, String traceId, String cancelReason ) {}

## 35.1.17 취소 SQL

\`\`\`xml id=“chg35048”

/\* SQL\_ID: CT-RSV-UPD-002 \*/

UPDATE CT\_CONTACT\_RESERVATION
SET STATUS\_CD
\= 'CANCELED'
, ACTIVE\_RESERVATION\_KEY
\= NULL
, VERSION\_NO
\= VERSION\_NO + 1
, UPDATED\_BY
\= #{actorUserId}
, UPDATED\_DTM
\= SYSTIMESTAMP
WHERE RESERVATION\_ID
\= #{reservationId}
AND STATUS\_CD
\= 'READY'
AND VERSION\_NO
\= #{expectedVersionNo}
AND BRANCH\_ID
\= #{actorBranchId}



취소 시 활성 중복키를 \`NULL\`로 만들어 동일 고객·동일 시간의 신규 예약을 허용할 수 있다.

업무가 취소 후에도 동일 Slot 재등록을 금지한다면 별도 정책을 적용한다.

\---

\## 35.1.18 논리 취소 후 조회정책

| 조회 | CANCELED 포함 |
|---|:---:|
| 기본 예약목록 | 기본 제외 또는 상태필터 |
| 고객 전체이력 | 포함 |
| 운영 조회 | 포함 |
| 통계 | 지표별 정책 |
| 중복 확인 | 제외 |
| 감사 조회 | 포함 |

모든 목록 SQL에 무조건 다음 조건을 넣지 않는다.

\`\`\`sql id="chg35049"
AND STATUS\_CD <> 'CANCELED'

조회 목적에 따라 명시적으로 상태범위를 정의한다.

## 35.1.19 UPDATE 영향 행 수

\`\`\`text id=“chg35050” 1건 → 정상

0건 → 상태·Version·권한·존재 재판단

2건 이상 → PK·SQL·데이터 결함



수정과 취소에서 \`affectedRows == 0\`을 정상으로 처리하지 않는다.

\---

\## 35.1.20 0건 원인 판단 전략

기본 전략:

\`\`\`text id="chg35051"
1\. 권한 Scope를 포함해 현재 데이터 조회

2\. 존재하지 않으면 Not Found

3\. 현재 상태가 READY가 아니면 상태 오류

4\. Request Version이 다르면 동시성 오류

5\. UPDATE 실행

6\. UPDATE가 0이면
검증 이후 발생한 동시 변경으로 판단

이 구조는 대부분의 원인을 구분한다.

## 35.1.21 보안상 존재 여부 은닉

다른 지점 예약을 요청한 경우:

\`\`\`text id=“chg35052” 실제 예약은 존재

하지만 사용자 Scope 밖



외부 응답:

\`\`\`text id="chg35053"
E-CT-RSV-0001
예약을 찾을 수 없습니다.

내부 보안감사:

text id="chg35054" ACCESS\_SCOPE\_DENIED

다른 사용자가 해당 예약 ID의 존재를 추측하지 못하게 한다.

# 35.2 동시 수정과 멱등성

## 35.2.1 Version 동시성 흐름

\`\`\`text id=“chg35055” 사용자 A Version 3 조회

사용자 B Version 3 조회

사용자 A UPDATE WHERE VERSION\_NO=3 → 1건 → Version 4

사용자 B UPDATE WHERE VERSION\_NO=3 → 0건 → 동시성 오류



Version은 변경 성공 때마다 1씩 증가한다.

\`\`\`text id="chg35056"
생성
Version 1

첫 수정
Version 2

두 번째 수정
Version 3

취소
Version 4

## 35.2.2 Version은 Timestamp보다 명확하다

다음 조건도 사용할 수 있다.

sql id="chg35057" AND UPDATED\_DTM = :lastUpdatedDtm

그러나 다음 위험이 있다.

\`\`\`text id=“chg35058” DB·Java 정밀도 차이

Timezone

Serialization

동일 Timestamp

밀리초 손실



동시성 전용 \`VERSION\_NO\`를 사용하는 것이 명확하다.

\---

\## 35.2.3 Version을 화면에서 숨기면 안 되는 이유

화면은 상세조회 Response의 Version을 보관하고 수정·취소 요청에 다시 전송해야 한다.

\`\`\`text id="chg35059"
상세 Response
versionNo=3

수정 Request
versionNo=3

화면이 수정 직전 Version을 다시 조회해 자동 교체하면 사용자가 보던 데이터와 변경 기준이 달라질 수 있다.

사용자가 조회한 Version을 전달해야 충돌을 정확히 발견할 수 있다.

## 35.2.4 Version 위변조

사용자가 Version을 임의로 큰 값으로 바꿔도 UPDATE는 성공하지 않는다.

\`\`\`text id=“chg35060” DB Version 4

Request Version 999

WHERE VERSION\_NO=999

→ 0건



그러나 Version 자체를 권한정보로 사용하지 않는다.

권한과 상태조건은 별도로 검증한다.

\---

\## 35.2.5 수정 멱등성

Version만 사용하는 경우 동일 수정요청이 반복되면 다음처럼 된다.

\`\`\`text id="chg35061"
첫 요청
Version 3 → 성공
DB Version 4

응답 유실

동일 요청 재전송
Version 3 → 충돌

데이터 중복은 발생하지 않지만 사용자는 첫 요청의 성공결과를 알 수 없다.

따라서 중요 변경거래에는 Idempotency Key를 적용할 수 있다.

## 35.2.6 취소 멱등성

취소는 특히 반복요청 계약을 명확히 해야 한다.

### 정책 A: 엄격 오류

\`\`\`text id=“chg35062” 첫 취소 READY → CANCELED

두 번째 취소 이미 CANCELED → 상태 오류


\### 정책 B: 멱등 성공

\`\`\`text id="chg35063"
동일 Idempotency Key의 재요청

→ 기존 취소 성공결과 반환

권장:

\`\`\`text id=“chg35064” 동일 Idempotency Key → 기존 결과 반환

다른 Idempotency Key로 이미 취소된 예약 요청 → 이미 취소됨 업무 오류


\---

\## 35.2.7 Idempotency Key 범위

\`\`\`text id="chg35065"
사용자 ID

\+ ServiceId

\+ Idempotency Key

필요 시 대상 식별자도 포함한다.

text id="chg35066" reservationId

## 35.2.8 Request Fingerprint

같은 Idempotency Key로 다른 내용을 보내면 안 된다.

예:

\`\`\`text id=“chg35067” Key IDEM-001

첫 요청 예약일시 10:00

두 번째 요청 예약일시 11:00



같은 Key지만 다른 요청이므로 오류 처리한다.

Fingerprint 예:

\`\`\`text id="chg35068"
SHA-256(
canonicalServiceId
\+ reservationId
\+ reservationDtm
\+ purposeCode
\+ memoHash
\+ versionNo
)

## 35.2.9 Idempotency 상태

| 상태 | 의미 |
| --- | --- |
| RECEIVED | 요청 접수 |
| PROCESSING | 처리 중 |
| SUCCESS | 성공결과 저장 |
| BUSINESS\_FAIL | 확정 업무 실패 |
| SYSTEM\_FAIL | 시스템 실패 |
| UNKNOWN | Commit 여부 불명확 |

## 35.2.10 동일 Key 처리

\`\`\`java id=“chg35069” IdempotencyDecision decision = idempotencyService.begin( serviceId, actor.userId(), idempotencyKey, requestFingerprint );

if (decision.requestMismatch()) { throw new BusinessException( CtReservationErrorCode .IDEMPOTENCY\_KEY\_REUSED, “동일 요청 식별자가 다른 내용으로” + “사용되었습니다.” ); }

if (decision.completed()) { return decision.savedResponse(); }

if (decision.processing()) { throw new BusinessException( CtReservationErrorCode .PROCESSING, “동일한 요청이 처리 중입니다.” ); }


\---

\## 35.2.11 수정 Service 구현

\`\`\`java id="chg35070"
public ReservationChangeResponse update(
ReservationUpdateRequest request,
TransactionContext context) {

ActorContext actor =
ActorContext.from(context);

rule.validateUpdateRequest(request);

IdempotencyContext idempotency =
idempotencyService.beginUpdate(
actor,
request,
context
);

if (idempotency.hasSavedResponse()) {
return idempotency.savedResponse();
}

ReservationCurrentRow current =
dao.selectCurrent(
new ReservationCurrentQuery(
request.reservationId(),
actor.branchId()
)
);

rule.requireExisting(current);
rule.validateUpdateAuthority(
current,
actor
);
rule.validateUpdatableStatus(
current
);
rule.validateVersion(
request.versionNo(),
current
);
rule.validateUpdateValues(
request
);

String newActiveKey =
ReservationKeys.activeKey(
current.customerNo(),
request.reservationDtm()
);

if (!newActiveKey.equals(
current.activeReservationKey())) {

rule.validateNoDuplicateForUpdate(
current.reservationId(),
current.customerNo(),
request.reservationDtm()
);
}

ReservationUpdateCommand command =
rule.buildUpdateCommand(
request,
current,
actor,
context,
newActiveKey
);

int affected =
dao.updateReservation(
ReservationUpdateRow.from(
command
)
);

if (affected == 0) {
throw concurrentModification();
}

requireOneRow(
affected,
"상담예약 수정"
);

ReservationHistoryInsertRow history =
historyFactory.forUpdate(
current,
command
);

int historyAffected =
dao.insertHistory(history);

requireOneRow(
historyAffected,
"상담예약 수정이력 저장"
);

ReservationChangeResponse response =
ReservationChangeResponse.updated(
command.reservationId(),
command.expectedVersionNo() + 1,
context
);

idempotencyService.completeSuccess(
idempotency.executionId(),
response
);

return response;
}

## 35.2.12 취소 Service 구현

\`\`\`java id=“chg35071” public ReservationChangeResponse cancel( ReservationCancelRequest request, TransactionContext context) {

ActorContext actor =
ActorContext.from(context);

rule.validateCancelRequest(request);

IdempotencyContext idempotency =
idempotencyService.beginCancel(
actor,
request,
context
);

if (idempotency.hasSavedResponse()) {
return idempotency.savedResponse();
}

ReservationCurrentRow current =
dao.selectCurrent(
new ReservationCurrentQuery(
request.reservationId(),
actor.branchId()
)
);

rule.requireExisting(current);
rule.validateUpdateAuthority(
current,
actor
);
rule.validateCancelableStatus(
current
);
rule.validateVersion(
request.versionNo(),
current
);

ReservationCancelCommand command =
rule.buildCancelCommand(
request,
current,
actor,
context
);

int affected =
dao.cancelReservation(
ReservationCancelRow.from(
command
)
);

if (affected == 0) {
throw concurrentModification();
}

requireOneRow(
affected,
"상담예약 취소"
);

int historyAffected =
dao.insertHistory(
historyFactory.forCancel(
current,
command
)
);

requireOneRow(
historyAffected,
"상담예약 취소이력 저장"
);

ReservationChangeResponse response =
ReservationChangeResponse.canceled(
command.reservationId(),
command.expectedVersionNo() + 1,
context
);

idempotencyService.completeSuccess(
idempotency.executionId(),
response
);

return response;

}


\---

\## 35.2.13 재시도 가능한 오류

일시적 DB Deadlock이나 Network 오류는 기술적으로 재시도할 수 있다.

하지만 변경거래 재시도 전 다음이 필요하다.

\`\`\`text id="chg35072"
Idempotency

Transaction Rollback 확인

동일 Version 여부

외부 변경 결과 확인

Retry 상한

Backoff

다음 오류는 같은 요청을 자동 재시도하지 않는다.

\`\`\`text id=“chg35073” Validation 오류

권한 오류

상태 오류

Version 충돌

업무 중복

Idempotency Key 불일치


\---

\## 35.2.14 Timeout과 UNKNOWN

변경거래 Timeout:

\`\`\`text id="chg35074"
애플리케이션은 Timeout 응답

DB Commit은 성공했을 가능성

특히 응답 전송단계에서 Timeout·연결종료가 발생하면 결과가 불명확하다.

대응:

\`\`\`text id=“chg35075” Idempotency 결과조회

Master Version 조회

History TraceId 조회

거래로그 상태

UNKNOWN 대사



사용자에게 무조건 재실행을 안내하지 않는다.

\---

\## 35.2.15 수정 후 결과조회

결과조회 식별자:

\`\`\`text id="chg35076"
reservationId

idempotencyKey

guid

traceId

확인:

\`\`\`text id=“chg35077” Master Version

최종 상태

History Change Type

Idempotency 상태


\---

\## 35.2.16 중복 클릭

화면에서는 버튼을 비활성화해 사용자 경험을 개선할 수 있다.

\`\`\`text id="chg35078"
첫 클릭

→ 버튼 Disabled

→ 처리 중 표시

그러나 다음은 계속 가능하다.

\`\`\`text id=“chg35079” 브라우저 Retry

Proxy Retry

사용자 새로고침

두 개 Tab

모바일 Network 재전송

악의적 직접 호출



따라서 서버의 Idempotency와 Version 통제가 필수다.

\---

\# 35.3 변경 이력

\## 35.3.1 변경 이력의 목적

변경 이력은 다음 질문에 답한다.

\`\`\`text id="chg35080"
어떤 예약이 변경됐는가?

변경 전 값은 무엇이었는가?

변경 후 값은 무엇인가?

어떤 필드가 바뀌었는가?

누가 변경했는가?

어떤 ServiceId로 변경했는가?

왜 변경했는가?

어떤 GUID·TraceId와 연결되는가?

변경 전후 Version은 무엇인가?

## 35.3.2 History와 Audit의 차이

| 구분 | Domain History | Audit Log |
| --- | --- | --- |
| 목적 | Aggregate 변화 복원·조회 | 중요행위 증명 |
| 대상 | 예약 데이터 | 사용자 행위 |
| Transaction | Master와 같은 Transaction | 정책에 따라 별도 |
| 내용 | Before·After·Change Set | 사용자·행위·결과 |
| 소비자 | 업무·분쟁·복구 | 보안·감사·운영 |
| 수정 | 원칙적 금지 | 원칙적 금지 |
| 보존 | 업무 보존정책 | 감사·법적 정책 |

하나의 History Table이 모든 감사 요구를 자동 충족하지 않는다.

중요 변경거래의 감사로그에는 수행자, 대상, 행위, 결과, GUID, 변경 전후 값 등을 마스킹해 기록하는 것이 기준이다.

## 35.3.3 이력 저장방식 비교

### 전체 Snapshot

\`\`\`text id=“chg35081” BEFORE\_JSON

AFTER\_JSON



장점:

\`\`\`text id="chg35082"
복원과 비교가 쉽다.

위험:

\`\`\`text id=“chg35083” 개인정보 복제

용량 증가

Schema 변경

검색 어려움


\### Field-level History

\`\`\`text id="chg35084"
FIELD\_NAME

BEFORE\_VALUE

AFTER\_VALUE

장점:

\`\`\`text id=“chg35085” 변경필드가 명확하다.

검색·감사가 쉽다.



위험:

\`\`\`text id="chg35086"
행 수 증가

복원 시 조립 필요

### Change Summary

text id="chg35087" 예약일시와 상담목적 변경

장점:

text id="chg35088" 사용자가 이해하기 쉽다.

위험:

text id="chg35089" 정확한 복원 불가

## 35.3.4 권장 이력모델

상담예약에는 다음 조합을 권장한다.

\`\`\`text id=“chg35090” History Header → 변경 거래 단위

History Detail → 변경 Field 단위


\---

\## 35.3.5 History Header Table

\`\`\`sql id="chg35091"
CREATE TABLE
CT\_CONTACT\_RESERVATION\_HISTORY (

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

BEFORE\_VERSION\_NO
NUMBER(18),

AFTER\_VERSION\_NO
NUMBER(18) NOT NULL,

CHANGE\_REASON
VARCHAR2(500),

CHANGE\_SUMMARY
VARCHAR2(1000),

SERVICE\_ID
VARCHAR2(200) NOT NULL,

CHANGED\_BY
VARCHAR2(100) NOT NULL,

CHANGED\_BRANCH\_ID
VARCHAR2(20),

CHANGED\_DTM
TIMESTAMP NOT NULL,

GUID
VARCHAR2(100),

TRACE\_ID
VARCHAR2(100),

IDEMPOTENCY\_KEY\_HASH
VARCHAR2(64),

CONSTRAINT PK\_CT\_RSV\_HISTORY
PRIMARY KEY (HISTORY\_ID)
);

Idempotency Key 원문은 저장하지 않고 필요한 경우 Hash를 저장한다.

## 35.3.6 History Detail Table

\`\`\`sql id=“chg35092” CREATE TABLE CT\_CONTACT\_RESERVATION\_HIST\_DTL (

HISTORY\_DETAIL\_ID
VARCHAR2(40) NOT NULL,

HISTORY\_ID
VARCHAR2(40) NOT NULL,

FIELD\_NAME
VARCHAR2(100) NOT NULL,

BEFORE\_VALUE
VARCHAR2(2000),

AFTER\_VALUE
VARCHAR2(2000),

MASKING\_YN
CHAR(1) DEFAULT 'N'
NOT NULL,

CONSTRAINT PK\_CT\_RSV\_HIST\_DTL
PRIMARY KEY (HISTORY\_DETAIL\_ID)

);


\---

\## 35.3.7 변경 필드 계산

\`\`\`java id="chg35093"
public List<FieldChange> calculateChanges(
ReservationCurrentRow before,
ReservationUpdateCommand after) {

List<FieldChange> changes =
new ArrayList<>();

addIfChanged(
changes,
"reservationDtm",
before.reservationDtm(),
after.reservationDtm(),
false
);

addIfChanged(
changes,
"purposeCode",
before.purposeCode(),
after.purposeCode(),
false
);

addIfChanged(
changes,
"memo",
before.memo(),
after.memo(),
true
);

return List.copyOf(changes);
}

## 35.3.8 변경사항 없음

수정 요청의 값이 현재 값과 모두 같을 수 있다.

정책 대안:

### No-op 성공

\`\`\`text id=“chg35094” DB UPDATE 없음

Version 증가 없음

History 없음

현재 결과 반환


\### 변경 시도로 이력

\`\`\`text id="chg35095"
DB UPDATE 없음

감사로그에 No-op 요청 기록

권장:

\`\`\`text id=“chg35096” 업무 데이터가 실제로 바뀌지 않으면 Master Version과 Domain History는 변경하지 않는다.

중요행위 감사에는 수정 시도 사실을 남길 수 있다.


\---

\## 35.3.9 No-op 판단

\`\`\`java id="chg35097"
List<FieldChange> changes =
historyFactory.calculateChanges(
current,
command
);

if (changes.isEmpty()) {
return ReservationChangeResponse.noChange(
current.reservationId(),
current.statusCode(),
current.versionNo(),
context
);
}

단, Idempotency 상태와 감사로그는 정책에 맞게 완료 처리한다.

## 35.3.10 수정 History Header

\`\`\`java id=“chg35098” public ReservationHistoryInsertRow forUpdate( ReservationCurrentRow before, ReservationUpdateCommand after) {

List<FieldChange> changes =
calculateChanges(before, after);

return new ReservationHistoryInsertRow(
GuidGenerator.newGuid(),
before.reservationId(),
"UPDATE",
before.statusCode(),
before.statusCode(),
before.versionNo(),
before.versionNo() + 1,
after.changeReason(),
summarize(changes),
"CT.Reservation.update",
after.actorUserId(),
after.actorBranchId(),
after.traceId(),
hashKey(after.idempotencyKey())
);

}


\---

\## 35.3.11 취소 History Header

\`\`\`java id="chg35099"
public ReservationHistoryInsertRow forCancel(
ReservationCurrentRow before,
ReservationCancelCommand command) {

return new ReservationHistoryInsertRow(
GuidGenerator.newGuid(),
before.reservationId(),
"CANCEL",
before.statusCode(),
"CANCELED",
before.versionNo(),
before.versionNo() + 1,
command.cancelReason(),
"상담예약 취소",
"CT.Reservation.cancel",
command.actorUserId(),
command.actorBranchId(),
command.traceId(),
hashKey(command.idempotencyKey())
);
}

## 35.3.12 History 저장순서

수정:

\`\`\`text id=“chg35100” 현재값 조회

→ Change Set 계산

→ Master UPDATE

→ History Header INSERT

→ History Detail INSERT

→ Commit



취소:

\`\`\`text id="chg35101"
현재값 조회

→ Master 상태 UPDATE

→ History Header INSERT

→ 상태 Detail INSERT

→ Commit

History 저장이 실패하면 Master UPDATE도 Rollback한다.

## 35.3.13 History Detail Batch INSERT

\`\`\`xml id=“chg35102”

/\* SQL\_ID: CT-RSV-INS-004 \*/

INSERT ALL

<foreach collection="list"
item="item">
INTO CT\_CONTACT\_RESERVATION\_HIST\_DTL (
HISTORY\_DETAIL\_ID
, HISTORY\_ID
, FIELD\_NAME
, BEFORE\_VALUE
, AFTER\_VALUE
, MASKING\_YN
)
VALUES (
#{item.historyDetailId}
, #{item.historyId}
, #{item.fieldName}
, #{item.beforeValue}
, #{item.afterValue}
, #{item.maskingYn}
)
</foreach>

SELECT 1 FROM DUAL



변경필드가 없는 경우 Detail INSERT를 실행하지 않는다.

\---

\## 35.3.14 메모 이력 보안

상담메모에는 개인정보가 포함될 수 있다.

대안:

\`\`\`text id="chg35103"
전체 원문 저장

마스킹 저장

Hash만 저장

암호화 저장

변경 여부만 저장

업무분쟁을 위해 원문 이력이 필요한 경우:

\`\`\`text id=“chg35104” 암호화

별도 접근권한

열람 감사

보존기간

파기



가 필요하다.

일반 거래로그에는 메모 원문을 기록하지 않는다.

\---

\## 35.3.15 History 불변성

일반 업무 ServiceId에서 History를 수정·삭제하지 않는다.

금지:

\`\`\`text id="chg35105"
CT.Reservation.updateHistory

CT.Reservation.deleteHistory

정정이 필요한 경우:

\`\`\`text id=“chg35106” 기존 History 유지

-   정정 History 추가
-   사유·승인


\---

\## 35.3.16 History 보존

확인:

\`\`\`text id="chg35107"
업무 보존기간

민원·분쟁기간

개인정보 파기

법적 Hold

Archive

조회성능

Partition

Master보다 History 데이터가 더 빠르게 증가할 수 있다.

## 35.3.17 변경이력 조회 ServiceId

운영·업무상 필요하다면 별도 거래를 설계한다.

text id="chg35108" CT.Reservation.selectHistory

권한:

\`\`\`text id=“chg35109” 일반 담당자 → 본인·지점 범위

관리자 → 승인된 범위

감사 담당자 → 별도 권한



이력 조회 자체도 감사대상이 될 수 있다.

\---

\## 35.3.18 Domain Event

향후 다른 도메인에 예약변경을 전달해야 한다면 Commit 이후 Event를 발행한다.

\`\`\`text id="chg35110"
Master·History Commit

→ Outbox Event

→ ReservationUpdated

또는

ReservationCanceled

DB Commit 전에 외부 메시지를 발행하면 Rollback 후 잘못된 Event가 전달될 수 있다.

# 35.4 오류와 보안 검증

## 35.4.1 오류 유형

| 유형 | 예 | 사용자 행동 | 운영 대응 |
| --- | --- | --- | --- |
| Validation | 필수값·길이 | 입력 수정 | 일반 |
| Not Found | 예약 없음 | 재조회 | 필요 시 확인 |
| Authorization | 변경권한 없음 | 관리자 문의 | 보안감사 |
| Invalid State | 완료 예약 수정 | 최신상태 확인 | 일반 |
| Concurrent | Version 충돌 | 재조회 후 재입력 | 충돌 Metric |
| Duplicate | 동일 Slot | 시간 변경 | 중복 Metric |
| Idempotency | Key 재사용 | 기존 요청 확인 | 요청 분석 |
| System | DB·연계 오류 | 잠시 후 확인 | 운영 Alert |
| Timeout | 결과 불명확 | 결과조회 | UNKNOWN 대사 |

입력 오류·업무 거절·시스템 실패는 사용자의 다음 행동과 운영 대응이 다르므로 하나의 오류코드로 통합해서는 안 된다.

## 35.4.2 오류코드 확장

| 오류코드 | 의미 |
| --- | --- |
| E-CT-RSV-0001 | 예약을 찾을 수 없음 |
| E-CT-RSV-0003 | 유효하지 않은 상담목적 |
| E-CT-RSV-0004 | 현재 상태에서 변경 불가 |
| E-CT-RSV-0005 | 예약 변경권한 없음 |
| E-CT-RSV-0006 | 동시 수정 충돌 |
| E-CT-RSV-0008 | 동일 예약 중복 |
| E-CT-RSV-0010 | 예약일시 정책 위반 |
| E-CT-RSV-0011 | 취소사유 필수 |
| E-CT-RSV-0012 | 처리결과 확인 불가 |
| E-CT-RSV-0013 | 변경할 내용 없음 |
| E-CT-RSV-0014 | Idempotency Key 재사용 오류 |
| E-CT-RSV-0015 | 동일 요청 처리 중 |
| E-CT-RSV-9002 | 예약 DB 처리 오류 |
| E-CT-RSV-9003 | 예약 처리 Timeout |

## 35.4.3 안전한 사용자 메시지

동시성 오류:

text id="chg35111" 다른 사용자가 먼저 변경했습니다. 최신 내용을 다시 조회해 주세요.

권한 오류:

text id="chg35112" 상담예약을 변경할 권한이 없습니다.

DB 오류:

\`\`\`text id=“chg35113” 상담예약 처리 중 오류가 발생했습니다. 잠시 후 처리결과를 확인해 주세요.

문의 ID: G-20260718-000001



사용자에게 다음을 노출하지 않는다.

\`\`\`text id="chg35114"
SQL 문장

Table명

DB 계정

Stack Trace

Class명

서버 경로

Bind 값

Token

## 35.4.4 원인 예외 보존

나쁜 코드:

java id="chg35115" catch (Exception ex) { throw new BusinessException( "수정 오류" ); }

원인 예외가 사라진다.

권장:

\`\`\`java id=“chg35116” catch (DuplicateKeyException ex) { throw new BusinessException( CtReservationErrorCode.DUPLICATE, “동일한 상담예약이 존재합니다.”, ex ); }

catch (DataAccessException ex) { throw new SystemException( CtReservationErrorCode .PERSISTENCE\_ERROR, “상담예약 변경 DB 처리 오류”, ex ); }



응답에는 안전한 메시지를 제공하고 로그에는 표준 오류코드와 원인 예외를 민감정보 없이 남긴다.

\---

\## 35.4.5 오류 로그 항목

\`\`\`text id="chg35117"
guid

traceId

serviceId

reservationId

expectedVersion

resultType

errorCode

exceptionClass

sqlId

affectedRows

instanceId

artifactVersion

금지:

\`\`\`text id=“chg35118” customerNo 원문

memo 원문

JWT

Authorization Header

Request Body 전체

DB 비밀번호


\---

\## 35.4.6 SQL Injection 방지

금지:

\`\`\`xml id="chg35119"
SET ${columnName} = #{value}

xml id="chg35120" WHERE RESERVATION\_ID = '${reservationId}'

사용:

xml id="chg35121" #{reservationId}

수정필드는 서버 Allow List로 고정한다.

## 35.4.7 Mass Assignment 방지

화면 Request를 Bean Copy로 DB Entity에 전부 복사하면 다음 값이 변경될 수 있다.

\`\`\`text id=“chg35122” statusCode

ownerUserId

branchId

versionNo

createdBy



금지:

\`\`\`java id="chg35123"
BeanUtils.copyProperties(
request,
reservationEntity
);

권장:

\`\`\`text id=“chg35124” 명시적 Command 생성

허용된 필드만 Mapping


\---

\## 35.4.8 권한의 이중 확인

\`\`\`text id="chg35125"
STF
→ 기능권한

업무 Rule·SQL
→ 데이터권한

STF가 CT\_RESERVATION\_UPDATE 권한을 확인했다고 해서 모든 예약을 수정할 수 있는 것은 아니다.

## 35.4.9 화면 버튼 통제

화면은 상태와 권한에 따라 버튼을 제어한다.

\`\`\`text id=“chg35126” READY + 수정권한 → 수정 버튼 활성

COMPLETED → 수정·취소 비활성



그러나 서버 Rule이 최종 판단한다.

브라우저 개발도구로 버튼을 활성화해 직접 요청할 수 있기 때문이다.

\---

\## 35.4.10 취소사유 검증

\`\`\`java id="chg35127"
public void validateCancelRequest(
ReservationCancelRequest request) {

if (request == null) {
throw validation(
"요청 Body가 없습니다."
);
}

requireText(
request.reservationId(),
"예약 ID"
);

if (request.versionNo() == null) {
throw validation(
"Version은 필수입니다."
);
}

requireText(
request.cancelReason(),
"취소사유"
);

if (request.cancelReason()
.length() > 500) {
throw validation(
"취소사유는 최대 500자입니다."
);
}
}

취소사유에 HTML·Script가 입력될 수 있으므로 화면 출력 시 Encoding한다.

## 35.4.11 XSS 방지

상담메모·변경사유·취소사유는 사용자 입력이다.

text id="chg35128" <script>alert(1)</script>

DB에 저장된 문자열을 화면에서 그대로 HTML로 렌더링하지 않는다.

기준:

\`\`\`text id=“chg35129” 입력 길이·제어문자 검증

출력 Context 기반 Encoding

HTML 허용 시 Sanitizer

CSP 적용


\---

\## 35.4.12 개인정보 변경이력

상담메모에 개인정보가 있을 수 있다.

이력·감사 조회권한:

\`\`\`text id="chg35130"
업무 담당자

관리자

감사 담당자

를 분리한다.

이력조회 화면에서 마스킹과 열람감사를 적용한다.

## 35.4.13 Timeout 오류

Timeout 응답 시:

\`\`\`text id=“chg35131” 거래 GUID

결과확인 안내

중복 재실행 경고



를 제공한다.

운영에서는 다음을 확인한다.

\`\`\`text id="chg35132"
Master Version

History TraceId

Idempotency 상태

거래로그 최종상태

## 35.4.14 오류 후 Transaction

다음 오류는 Master 변경 전에 발생하므로 DB 변경이 없다.

\`\`\`text id=“chg35133” Validation

Not Found

권한

상태

Version 사전충돌

상담목적 오류



다음 오류는 UPDATE 후 발생할 수 있으므로 Rollback이 필요하다.

\`\`\`text id="chg35134"
History INSERT 실패

History Detail 실패

Idempotency 결과 저장 실패

예상하지 못한 Runtime 오류

## 35.4.15 BusinessException Rollback

프로젝트에서 BusinessException이 Checked Exception이거나 Rollback 제외정책을 가진 경우 주의한다.

\`\`\`text id=“chg35135” Master UPDATE 성공

History 업무 오류 발생

BusinessException이 Rollback되지 않음

→ Master만 Commit



Facade Transaction의 Rollback 규칙을 명확히 한다.

예:

\`\`\`java id="chg35136"
@Transactional(
timeout = 5,
rollbackFor = Exception.class
)

단, 모든 Exception Rollback 정책은 프로젝트 공통 기준과 일치해야 한다.

# DAO·Mapper 확장

## Mapper Interface

\`\`\`java id=“chg35137” @Mapper public interface CtReservationMapper {

ReservationCurrentRow
selectReservationCurrent(
ReservationCurrentQuery query
);

int updateReservation(
ReservationUpdateRow row
);

int cancelReservation(
ReservationCancelRow row
);

int insertReservationHistory(
ReservationHistoryInsertRow row
);

int insertReservationHistoryDetails(
List<ReservationHistoryDetailRow> rows
);

}


\## DAO

\`\`\`java id="chg35138"
@Repository
public class CtReservationDao {

private final CtReservationMapper mapper;

public ReservationCurrentRow selectCurrent(
ReservationCurrentQuery query) {
return mapper
.selectReservationCurrent(query);
}

public int updateReservation(
ReservationUpdateRow row) {
return mapper.updateReservation(row);
}

public int cancelReservation(
ReservationCancelRow row) {
return mapper.cancelReservation(row);
}

public int insertHistory(
ReservationHistoryInsertRow row) {
return mapper
.insertReservationHistory(row);
}

public int insertHistoryDetails(
List<ReservationHistoryDetailRow>
rows) {

if (rows == null || rows.isEmpty()) {
return 0;
}

return mapper
.insertReservationHistoryDetails(
rows
);
}
}

# 영향 행 검증

## Master UPDATE

\`\`\`java id=“chg35139” private void requireUpdatedOneRow( int affectedRows, String operation) {

if (affectedRows == 0) {
throw new BusinessException(
CtReservationErrorCode
.CONCURRENT\_MODIFICATION,
"다른 사용자가 먼저 변경했습니다. "
\+ "최신 내용을 다시 조회해 주세요."
);
}

if (affectedRows != 1) {
throw new SystemException(
CtReservationErrorCode
.PERSISTENCE\_ERROR,
operation
\+ " 영향 행이 1건이 아닙니다. "
\+ "affectedRows="
\+ affectedRows
);
}

}


\## History Detail

Field-level History는 변경필드 수만큼 저장되므로 영향 행이 \`changes.size()\`와 같아야 한다.

\`\`\`java id="chg35140"
int historyDetailAffected =
dao.insertHistoryDetails(details);

if (historyDetailAffected
!= details.size()) {
throw new SystemException(
CtReservationErrorCode
.PERSISTENCE\_ERROR,
"변경이력 Detail 저장 건수가 "
\+ "일치하지 않습니다."
);
}

MyBatis·Oracle의 Batch 반환방식은 실제 Driver 기준으로 검증한다.

# Transaction 구조

\`\`\`text id=“chg35141” Facade @Transactional

├─ 현재 Master 조회 ├─ Rule 검증 ├─ Master UPDATE ├─ History Header INSERT ├─ History Detail INSERT ├─ Idempotency 결과 └─ Commit



다음은 같은 Transaction에 포함하지 않는 것을 검토한다.

\`\`\`text id="chg35142"
외부 알림 발송

메일·문자

다른 업무 WAR 호출

대용량 파일생성

Commit 후 Event·Outbox로 처리한다.

# 수정 정상 흐름

text id="chg35143" 1. 화면이 예약 ID·수정값·Version을 전송한다. 2. STF가 인증과 수정 기능권한을 확인한다. 3. Handler가 update 거래를 선택한다. 4. Facade가 Transaction을 시작한다. 5. 현재 예약을 지점 Scope로 조회한다. 6. 존재·데이터권한·READY 상태를 확인한다. 7. Request Version과 DB Version을 비교한다. 8. 예약일시·목적코드·메모를 검증한다. 9. 변경 필드를 계산한다. 10. 변경이 없으면 No-op 정책을 적용한다. 11. 새 예약일시의 중복을 확인한다. 12. 상태·Version·지점 조건으로 UPDATE한다. 13. 영향 행 1건을 확인한다. 14. UPDATE History를 저장한다. 15. Transaction을 Commit한다. 16. 증가된 Version을 응답한다.

# 취소 정상 흐름

text id="chg35144" 1. 화면이 예약 ID·Version·취소사유를 전송한다. 2. STF가 취소 기능권한을 확인한다. 3. 현재 예약을 조회한다. 4. 존재·권한·READY 상태를 확인한다. 5. Version을 확인한다. 6. STATUS\_CD를 CANCELED로 변경한다. 7. ACTIVE\_RESERVATION\_KEY를 NULL로 변경한다. 8. Version을 증가시킨다. 9. 영향 행 1건을 확인한다. 10. CANCEL History를 저장한다. 11. Transaction을 Commit한다. 12. CANCELED 상태와 신규 Version을 반환한다.

# 오류·Timeout·장애 흐름

## 다른 사용자가 먼저 수정

\`\`\`text id=“chg35145” Request Version 3

DB Version 4

→ E-CT-RSV-0006

→ DB 변경 없음

→ 화면 최신조회


\## 검증 이후 동시 변경

\`\`\`text id="chg35146"
사전 Version 일치

→ 다른 거래가 먼저 Commit

→ UPDATE 0건

→ E-CT-RSV-0006

→ History 없음

## 완료 예약 수정

\`\`\`text id=“chg35147” DB 상태 COMPLETED

→ 상태 Rule 실패

→ E-CT-RSV-0004

→ UPDATE 없음


\## 무권한 수정

\`\`\`text id="chg35148"
타 지점 예약

→ Scope 조회 0건

→ 외부 Not Found

→ 보안감사 ACCESS\_SCOPE\_DENIED

## 수정 중 Unique 충돌

\`\`\`text id=“chg35149” 예약일시 변경

→ 동일 고객 READY 예약과 충돌

→ DuplicateKeyException

→ 전체 Rollback

→ E-CT-RSV-0008


\## History 실패

\`\`\`text id="chg35150"
Master UPDATE 성공

→ History INSERT 실패

→ 전체 Rollback

→ Version 원복

→ E-CT-RSV-9002

## 취소 응답 유실

\`\`\`text id=“chg35151” CANCELED Commit 성공

→ Network 단절

→ 동일 Idempotency Key 재요청

→ 기존 성공결과 반환


\---

\# 정상 예시

\`\`\`text id="chg35152"
현재 예약

reservationId
RSV-20260718-000001

status
READY

version
3

owner
U001

branch
B001

요청 사용자
U001

수정 요청
예약일시 10:30 → 11:00
목적 PRODUCT → DEPOSIT
version 3

UPDATE 조건
ID 일치
READY
VERSION 3
BRANCH B001

결과
영향 행 1

Master
Version 4

History
UPDATE
Before Version 3
After Version 4
변경필드 2건

응답
SUCCESS
Version 4

# 금지 예시

\`\`\`text id=“chg35153” 예약 ID만 조건으로 UPDATE한다.

Version을 화면에 반환하지 않는다.

수정 직전에 최신 Version을 서버가 임의로 교체한다.

현재 상태를 확인하지 않고 UPDATE한다.

COMPLETED 예약을 일반 수정한다.

취소를 DELETE SQL로 처리한다.

취소 시 활성 중복키를 그대로 둔다.

화면이 보낸 statusCode를 UPDATE한다.

화면이 보낸 userId·branchId로 권한을 판단한다.

모든 Request 필드를 Entity에 자동 복사한다.

UPDATE 영향 행 0건을 성공으로 처리한다.

UPDATE 0건을 무조건 Not Found로 처리한다.

사전 Version 비교만 하고 SQL Version 조건을 생략한다.

변경이 없는데 Version을 증가시킨다.

History를 Master와 다른 Transaction으로 저장한다.

History 오류를 로그만 남기고 무시한다.

변경 전 값을 조회하지 않고 AFTER 값만 이력으로 남긴다.

상담메모 원문을 애플리케이션 로그에 기록한다.

Idempotency Key 원문을 무기한 저장한다.

같은 Idempotency Key로 다른 요청을 허용한다.

이미 취소된 예약에 다른 Key로 성공을 반복 반환한다.

Timeout 후 새로운 Key로 즉시 다시 수정한다.

원인 예외를 버리고 모든 오류를 하나의 업무코드로 반환한다.

다른 지점 예약의 존재 여부를 외부에 노출한다.

취소사유를 검증하지 않는다.

History Table을 일반 화면에서 수정·삭제한다.


\---

\# 테스트 전략

\## Rule Unit Test

| 테스트 | 기대 |
|---|---|
| READY 수정 | 허용 |
| COMPLETED 수정 | 거절 |
| CANCELED 수정 | 거절 |
| READY 취소 | 허용 |
| COMPLETED 취소 | 거절 |
| Version 일치 | 통과 |
| Version 불일치 | 동시성 오류 |
| 담당자 | 허용 |
| 지점 관리자 | 허용 |
| 타 지점 사용자 | 거절 |
| 취소사유 Null | Validation |
| 변경값 없음 | No-op 정책 |

\---

\## 수정 정상 테스트

\`\`\`java id="chg35154"
@Test
void READY예약은\_수정되고\_Version과\_이력이\_증가한다() {

ReservationCurrentRow current =
fixture.readyReservation(
"RSV-001",
3L
);

when(dao.selectCurrent(any()))
.thenReturn(current);

when(dao.updateReservation(any()))
.thenReturn(1);

when(dao.insertHistory(any()))
.thenReturn(1);

when(dao.insertHistoryDetails(any()))
.thenAnswer(invocation ->
invocation
.<List<?>>getArgument(0)
.size()
);

ReservationChangeResponse response =
service.update(
updateRequestVersion3(),
context()
);

assertThat(response.versionNo())
.isEqualTo(4L);

verify(dao).updateReservation(any());
verify(dao).insertHistory(any());
}

## 동시 수정 통합 테스트

text id="chg35155" 1. 예약 Version 3 생성 2. Transaction A와 B가 Version 3 조회 3. A가 UPDATE·Commit 4. B가 UPDATE 실행 5. A 영향 행 1 6. B 영향 행 0 7. Master는 A의 변경값 8. Version 4 9. History는 A의 이력만 존재

실제 DB에서 동시에 실행해야 한다.

Mock Test만으로 낙관적 Lock을 완전히 검증할 수 없다.

## 취소 반복요청 테스트

| 요청 | Key | 기대 |
| --- | --- | --- |
| 첫 취소 | K1 | 성공 |
| 같은 요청 재전송 | K1 | 기존 성공 |
| 이미 취소 후 신규 요청 | K2 | 상태 오류 |
| K1에 다른 예약 ID | K1 | Key 재사용 오류 |
| K1에 다른 사유 | K1 | Fingerprint 오류 |

## Rollback 테스트

### 수정 History 실패

\`\`\`text id=“chg35156” Master UPDATE

→ History Header 실패

기대 Master 원래 값 Version 원래 값 History 0건


\### History Detail 실패

\`\`\`text id="chg35157"
Master UPDATE

History Header INSERT

History Detail 실패

기대
세 작업 전체 Rollback

### 취소 History 실패

\`\`\`text id=“chg35158” STATUS CANCELED UPDATE

ACTIVE KEY NULL

History 실패

기대 상태 READY Active Key 복원 Version 원복


\---

\## 권한 테스트

| ID | 시나리오 | 기대 |
|---|---|---|
| CT35-001 | 담당자 수정 | 성공 |
| CT35-002 | 지점 관리자 수정 | 성공 |
| CT35-003 | 같은 지점 일반 타인 | 정책상 거절 |
| CT35-004 | 타 지점 사용자 | Not Found·감사 |
| CT35-005 | 수정 기능권한 없음 | 403 |
| CT35-006 | 취소 기능권한 없음 | 403 |
| CT35-007 | Request userId 위조 | 무시 |
| CT35-008 | Request branchId 위조 | 무시 |

\---

\## 상태 테스트

| ID | 상태 | 거래 | 기대 |
|---|---|---|---|
| CT35-009 | READY | update | 성공 |
| CT35-010 | COMPLETED | update | 상태 오류 |
| CT35-011 | CANCELED | update | 상태 오류 |
| CT35-012 | READY | cancel | 성공 |
| CT35-013 | COMPLETED | cancel | 상태 오류 |
| CT35-014 | CANCELED | cancel·새 Key | 상태 오류 |
| CT35-015 | CANCELED | cancel·기존 Key | 기존 결과 |

\---

\## Version 테스트

| ID | 상황 | 기대 |
|---|---|---|
| CT35-016 | 요청·DB Version 동일 | 성공 |
| CT35-017 | 요청 Version 과거 | 충돌 |
| CT35-018 | 요청 Version 미래 | 충돌 |
| CT35-019 | 사전검증 후 선행 Commit | UPDATE 0·충돌 |
| CT35-020 | Version Null | Validation |
| CT35-021 | 성공 수정 | Version +1 |
| CT35-022 | 실패 수정 | Version 유지 |

\---

\## 이력 테스트

| ID | 시나리오 | 기대 |
|---|---|---|
| CT35-023 | 예약일시 변경 | Detail 1건 |
| CT35-024 | 목적·메모 변경 | Detail 2건 |
| CT35-025 | 값 변화 없음 | Domain History 없음 |
| CT35-026 | 취소 | Header·상태 Detail |
| CT35-027 | History Header 실패 | 전체 Rollback |
| CT35-028 | History Detail 실패 | 전체 Rollback |
| CT35-029 | 메모 변경 | 마스킹·암호화 정책 |
| CT35-030 | 이력 일반 수정 | 차단 |

\---

\## 오류·보안 테스트

| ID | 시나리오 | 기대 |
|---|---|---|
| CT35-031 | SQL Injection 문자열 | Bind 처리 |
| CT35-032 | statusCode 위조 | 무시 |
| CT35-033 | ownerUserId 위조 | 무시 |
| CT35-034 | HTML 취소사유 | 출력 Encoding |
| CT35-035 | 메모 로그 검색 | 원문 없음 |
| CT35-036 | DB 오류 | 안전 메시지·원인 보존 |
| CT35-037 | Timeout | GUID·결과조회 |
| CT35-038 | 다른 지점 ID 추측 | 존재 은닉 |
| CT35-039 | Key 원문 로그 | 없어야 함 |
| CT35-040 | Stack Trace 응답 | 없어야 함 |

\---

\## 성능·Lock 테스트

\`\`\`text id="chg35159"
동시 수정 100건

동일 예약 집중충돌

서로 다른 예약 병렬수정

History 다중 INSERT

Unique Index 경합

DB Pool Pending

SQL p95

Lock Wait

낙관적 Lock 충돌은 정상 업무 결과일 수 있으므로 시스템 오류율과 별도 Metric으로 관리한다.

# 추적성 Matrix

| 요구사항 | 이벤트 | ServiceId | Mapper | Table | 테스트 |
| --- | --- | --- | --- | --- | --- |
| 예약 수정 | E05 | CT.Reservation.update | updateReservation | Master | CT35-001~011 |
| 동시성 | E05 | CT.Reservation.update | Version UPDATE | Master | CT35-016~022 |
| 예약 취소 | E06 | CT.Reservation.cancel | cancelReservation | Master | CT35-012~015 |
| 수정이력 | E05 | update | History INSERT | History | CT35-023~030 |
| 취소이력 | E06 | cancel | History INSERT | History | CT35-026~030 |
| 변경권한 | E05·E06 | update·cancel | Scope 조건 | Master | CT35-001~008 |
| 멱등성 | E05·E06 | update·cancel | Idempotency | 상태 Table | 반복요청 Test |
| 보안 | 전체 | 전체 | Masking·Bind | Log·DB | CT35-031~040 |

화면–ServiceId–프로그램–SQL–Table–Test 연결이 누락된 변경거래는 완료로 판정하지 않는다.

# 표준 응답

## 수정 성공

json id="chg35160" { "reservationId": "RSV-20260718-000001", "statusCode": "READY", "versionNo": 4, "resultType": "UPDATED", "guid": "G-20260718-000001", "traceId": "T-20260718-000001" }

## 변경 없음

json id="chg35161" { "reservationId": "RSV-20260718-000001", "statusCode": "READY", "versionNo": 3, "resultType": "NO\_CHANGE", "guid": "G-20260718-000002" }

## 취소 성공

json id="chg35162" { "reservationId": "RSV-20260718-000001", "statusCode": "CANCELED", "versionNo": 4, "resultType": "CANCELED", "guid": "G-20260718-000003" }

## 동시성 오류

json id="chg35163" { "resultCode": "E-CT-RSV-0006", "message": "다른 사용자가 먼저 변경했습니다. 최신 내용을 다시 조회해 주세요.", "guid": "G-20260718-000004" }

# 운영 Metric

\`\`\`text id=“chg35164” ct.reservation.update.count

ct.reservation.update.duration

ct.reservation.cancel.count

ct.reservation.cancel.duration

ct.reservation.concurrent.conflict.count

ct.reservation.no.change.count

ct.reservation.idempotency.hit.count

ct.reservation.idempotency.mismatch.count

ct.reservation.history.failure.count

ct.reservation.permission.denied.count

ct.reservation.invalid.status.count

ct.reservation.timeout.count

ct.reservation.unknown.count



Label:

\`\`\`text id="chg35165"
serviceId

resultType

statusFrom

statusTo

instanceId

artifactVersion

금지 Label:

\`\`\`text id=“chg35166” reservationId

customerNo

userId

guid

idempotencyKey


\---

\# 운영 Alert

| 조건 | 등급 | 확인 |
|---|---|---|
| History 저장 실패 1건 | Critical | Master Rollback |
| UNKNOWN 변경거래 | Critical | 결과 대사 |
| 동시성 충돌 급증 | Warning | UI·업무 프로세스 |
| 무권한 변경 증가 | Security | 사용자·IP |
| Invalid State 증가 | Warning | 화면 상태 갱신 |
| DB UPDATE Timeout | Major | Pool·SQL |
| Idempotency 불일치 | Security·Warning | 재사용·공격 |
| 취소 오류율 증가 | Major | 상태·Version |
| UPDATE 영향 행 2건 이상 | Critical | PK·SQL 결함 |

\---

\# 장애 대응

\## 수정 Timeout

\`\`\`text id="chg35167"
GUID 확인

→ Idempotency 상태

→ Master Version

→ History TraceId

→ 결과 확정

→ 필요 시 사용자 안내

## History 실패

\`\`\`text id=“chg35168” 오류 거래 GUID

→ Rollback 여부 확인

→ Master 상태·Version 확인

→ History 건수 확인

→ 부분 반영이면 SEV 상향


\## 충돌 급증

\`\`\`text id="chg35169"
특정 예약 집중

화면 장시간 열린 상태

자동 저장

잘못된 재시도

Version 미갱신

동일 데이터를 여러 조직이 수정

을 확인한다.

## 무권한 요청 증가

\`\`\`text id=“chg35170” 사용자 ID

지점

Source IP

대상 ID Hash

요청 빈도

JWT 상태



를 보안로그에서 확인한다.

\---

\# 책임 경계와 RACI

| 활동 | 업무 | UI | 개발 | AA | DBA | 보안 | QA | 운영 |
|---|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|
| 상태전이 | A/R | C | R/C | A/C | C | C | C | I |
| 수정필드 | A/R | C | R | C | C | C | C | I |
| Version 정책 | C | C | R | A | A/C | I | R/C | I |
| 권한 | A/C | C | R | C | I | A/R | C | C |
| 취소정책 | A/R | C | R | C | C | C | C | C |
| 멱등성 | C | C | R | A | C | C | R/C | C |
| History | A/C | I | R | A/C | C | A/C | C | C |
| History 보존 | A/C | I | C | C | A/R | A/C | I | R |
| SQL·Index | C | I | R | C | A/R | I | C | C |
| 오류코드 | A/C | C | R | A/C | I | C | C | C |
| 동시성 Test | C | C | R | C | C | I | A/R | I |
| 운영 Alert | I | I | C | C | C | C | C | A/R |
| 장애 대사 | A/C | I | R/C | C | R/C | C | C | A/R |

\---

\# 성능·용량·확장성

\## 현재값 조회

수정·취소마다 현재값 조회와 UPDATE가 발생한다.

\`\`\`text id="chg35171"
SELECT Current

\+ UPDATE Master

\+ INSERT History Header

\+ INSERT History Detail

단건 변경이므로 PK Index를 사용해야 한다.

## Version 충돌

동일 예약에 변경이 집중되면 충돌률이 증가한다.

\`\`\`text id=“chg35172” 충돌률

\= 동시성 오류 건수 ÷ 수정 요청 건수



충돌률이 높으면 다음을 검토한다.

\`\`\`text id="chg35173"
업무 소유권

화면 편집시간

자동저장

세분화된 Aggregate

작업 배정

Lock 정책

## History 용량

\`\`\`text id=“chg35174” History Header 건수 ≈ 등록 + 수정 + 취소 건수

History Detail 건수 ≈ 각 변경의 수정 필드 수 합계



예:

\`\`\`text id="chg35175"
일 변경거래
200,000건

평균 변경필드
2개

일 Detail
400,000건

Partition·Archive·보존기간을 산정한다.

## 취소 Index

취소 후 ACTIVE\_RESERVATION\_KEY=NULL UPDATE가 Unique Index와 관련 Index에 미치는 비용을 성능시험으로 확인한다.

# 자동검증 및 품질 Gate

## 변경 SQL Gate

필수 조건:

\`\`\`text id=“chg35176” PK

현재 상태

Version

권한 Scope

영향 행 수 검증

History

Rollback Test



변경 SQL Gate는 상태·Version·권한조건, 영향 행 검증, 감사·Rollback 테스트를 필수로 검사해야 한다.

\## DTO Gate

\`\`\`text id="chg35177"
statusCode Request 금지

ownerUserId Request 금지

branchId Request 금지

updatedBy Request 금지

Version 필수

변경사유 길이

## State Gate

\`\`\`text id=“chg35178” 허용 상태전이 표

Rule 구현

SQL 상태조건

화면 버튼

테스트


\## Version Gate

\`\`\`text id="chg35179"
상세 Response Version

수정 Request Version

취소 Request Version

UPDATE Version 조건

성공 후 Version 증가

충돌 테스트

## Idempotency Gate

\`\`\`text id=“chg35180” Key Scope

Request Fingerprint

동일 Key 기존결과

다른 Body 거절

PROCESSING 상태

UNKNOWN 대사

보존기간


\## History Gate

\`\`\`text id="chg35181"
Before·After Version

Change Type

Change Set

Actor

Reason

GUID·TraceId

Master와 동일 Transaction

민감정보 정책

## Security Gate

\`\`\`text id=“chg35182” 기능권한

데이터권한

Mass Assignment 방지

SQL Bind

XSS 출력 Encoding

민감정보 로그 금지

존재 여부 은닉


\---

\# 따라 하는 실무 절차

\## 1단계. ServiceId를 등록한다

\`\`\`text id="chg35183"
CT.Reservation.update

CT.Reservation.cancel

완료 증적:

\`\`\`text id=“chg35184” Handler

OM Catalog

거래코드

권한

Timeout

감사


\## 2단계. 상태와 수정필드를 확정한다

\`\`\`text id="chg35185"
READY만 수정·취소

수정 허용필드

취소사유

복구 여부

## 3단계. Version 계약을 구현한다

\`\`\`text id=“chg35186” 상세 Response

화면 보관

수정·취소 Request

SQL 조건

충돌 응답


\## 4단계. 현재값 조회와 권한 Rule을 구현한다

\## 5단계. 상태·Version 조건 UPDATE를 작성한다

\## 6단계. History Header·Detail을 구현한다

Master와 동일 Transaction으로 Rollback을 검증한다.

\## 7단계. Idempotency를 연결한다

\`\`\`text id="chg35187"
Key

Fingerprint

기존 결과

UNKNOWN

## 8단계. 오류·보안 Mapping을 적용한다

## 9단계. 동시성·Rollback·반복요청을 시험한다

## 10단계. Metric·Alert·Runbook을 등록한다

# 완료 체크리스트

## 상태·권한

| 점검 항목 | 완료 |
| --- | --- |
| 수정·취소 ServiceId가 분리됐다. | □ |
| READY만 수정할 수 있다. | □ |
| READY만 취소할 수 있다. | □ |
| 관리자 복구는 별도 거래다. | □ |
| 기능권한을 확인한다. | □ |
| 데이터권한을 확인한다. | □ |
| 다른 지점 존재 여부를 노출하지 않는다. | □ |
| 화면 상태와 서버 상태를 구분한다. | □ |

## Version·동시성

| 점검 항목 | 완료 |
| --- | --- |
| 상세 Response에 Version이 있다. | □ |
| 수정·취소 Request에 Version이 있다. | □ |
| 사전 Version을 검증한다. | □ |
| UPDATE SQL에 Version 조건이 있다. | □ |
| 성공 시 Version이 증가한다. | □ |
| 영향 행 0건을 충돌로 처리한다. | □ |
| 동시 수정 통합 테스트가 있다. | □ |
| 실패 시 Version이 유지된다. | □ |

## 수정·취소

| 점검 항목 | 완료 |
| --- | --- |
| 수정 필드 Allow List가 있다. | □ |
| Request의 상태·사용자·지점을 무시한다. | □ |
| 예약일시 변경 시 중복을 확인한다. | □ |
| DB Unique가 최종 통제한다. | □ |
| 취소는 상태변경이다. | □ |
| 취소 시 활성 중복키를 해제한다. | □ |
| 취소사유가 필수다. | □ |
| 물리 DELETE를 사용하지 않는다. | □ |

## 멱등성

| 점검 항목 | 완료 |
| --- | --- |
| 수정·취소 적용기준을 정했다. | □ |
| Key Scope가 명확하다. | □ |
| Request Fingerprint가 있다. | □ |
| 동일 Key 재요청은 기존 결과다. | □ |
| 다른 요청내용은 거절한다. | □ |
| 처리 중 상태를 구분한다. | □ |
| Timeout UNKNOWN을 대사한다. | □ |
| Key 원문을 로그에 기록하지 않는다. | □ |

## 이력·감사

| 점검 항목 | 완료 |
| --- | --- |
| 변경 전 데이터를 확보한다. | □ |
| 변경 후 Version을 기록한다. | □ |
| 변경 필드를 계산한다. | □ |
| 수정·취소 History가 있다. | □ |
| 변경사유를 저장한다. | □ |
| GUID·TraceId가 있다. | □ |
| Master와 History가 같은 Transaction이다. | □ |
| History 오류 시 Master가 Rollback된다. | □ |
| 민감정보 정책이 있다. | □ |
| History와 Audit을 구분한다. | □ |

## 오류·보안

| 점검 항목 | 완료 |
| --- | --- |
| Validation·업무·시스템 오류를 구분한다. | □ |
| 원인 예외를 보존한다. | □ |
| 안전한 사용자 메시지를 반환한다. | □ |
| Mass Assignment를 방지한다. | □ |
| SQL Bind를 사용한다. | □ |
| XSS 출력 Encoding을 적용한다. | □ |
| 메모 원문을 로그에 남기지 않는다. | □ |
| Stack Trace를 응답하지 않는다. | □ |

## 테스트·운영

| 점검 항목 | 완료 |
| --- | --- |
| 상태 Rule Test가 있다. | □ |
| 권한 Test가 있다. | □ |
| Version 충돌 Test가 있다. | □ |
| 동시 UPDATE Test가 있다. | □ |
| 반복 취소 Test가 있다. | □ |
| History Rollback Test가 있다. | □ |
| Timeout 결과확인 Test가 있다. | □ |
| Metric·Alert가 있다. | □ |
| 운영 Runbook이 있다. | □ |
| GUID로 전체 변경을 추적할 수 있다. | □ |

# 변경·호환성·폐기 관리

## 상태 추가

예:

text id="chg35188" NO\_SHOW

영향:

\`\`\`text id=“chg35189” 상태전이

화면 버튼

수정·취소 Rule

SQL 조건

공통코드

통계

History

테스트


\## Version 정책 변경

Version을 제거하거나 Timestamp 방식으로 바꾸는 것은 동시성 계약 변경이다.

구·신 화면이 혼재하는 Rolling 배포에서는 필수 Request 필드 변경에 주의한다.

\## 수정 가능 필드 추가

예:

\`\`\`text id="chg35190"
ownerUserId 변경

일반 수정에 단순 추가하지 않는다.

담당자 변경은 권한·감사·업무의미가 다르므로 다음처럼 별도 거래를 검토한다.

text id="chg35191" CT.Reservation.assignOwner

## 취소 복구 추가

text id="chg35192" CT.Reservation.restore

필수:

\`\`\`text id=“chg35193” 승인권한

복구 가능기간

중복 Slot 검증

History

감사

Idempotency


\## History Schema 변경

History는 장기 보존 데이터이므로 Column 삭제·의미 변경보다 신규 Column 추가와 Version 정책을 우선한다.

\## Idempotency 보존기간 변경

최대 재시도 가능시간과 Timeout 대사기간보다 짧아서는 안 된다.

\## ServiceId 폐기

\`\`\`text id="chg35194"
Deprecated

→ 화면 전환

→ 호출량 0

→ Handler·OM 제거

→ 권한·Timeout 제거

→ 문서·테스트 폐기

# 시사점

## 핵심 아키텍처 판단

첫째, 수정과 취소는 현재 상태와 Version을 전제로 하는 조건부 상태변경 거래다.

둘째, 사전 Version 확인은 사용자 오류를 명확히 하기 위한 것이고, SQL의 Version 조건은 최종 동시성 보장을 위한 것이다.

셋째, UPDATE 영향 행 0건은 성공이 아니며 상태·권한·존재·동시성을 구분해야 한다.

넷째, 취소는 물리 삭제가 아니라 상태전이이며 조회·중복·보존·복구정책까지 함께 설계해야 한다.

다섯째, Version은 중복 데이터 반영을 막지만 응답 유실 후 기존 성공결과를 반환하지는 못하므로 중요 변경거래에는 Idempotency가 필요하다.

여섯째, 동일 Idempotency Key를 다른 요청내용에 사용할 수 없도록 Request Fingerprint를 검증해야 한다.

일곱째, Domain History는 Aggregate 변화의 일부이므로 Master와 같은 Transaction으로 저장해야 한다.

여덟째, History와 Audit은 목적·보존·조회권한이 다르며 하나의 로그가 둘을 모두 대신할 수 없다.

## 주요 위험

| 위험 | 결과 |
| --- | --- |
| Version 조건 없음 | Lost Update |
| 상태조건 없음 | 완료·취소 데이터 변경 |
| 영향 행 0건 성공처리 | 변경 실패 은폐 |
| 화면 사용자정보 신뢰 | 권한 위조 |
| Mass Assignment | 상태·Owner 변조 |
| 물리 DELETE | 감사·복구 불가 |
| 취소 중복키 유지 | 재예약 차단 |
| 사전조회만 동시성 통제 | Race Condition |
| Idempotency 없음 | 응답 유실 후 혼란 |
| 같은 Key 다른 Body | 잘못된 기존결과 |
| History 별도 Transaction | Master·이력 불일치 |
| History 원문 개인정보 | 정보유출 |
| 원인 예외 제거 | 장애 분석 불가 |
| Stack Trace 응답 | 내부정보 노출 |
| 다른 지점 존재 노출 | 정보추론 |
| Timeout 즉시 재시도 | 중복 변경 |
| 동시성 Test 누락 | 운영 충돌 |

## 우선 보완 과제

1.  CT.Reservation.update와 cancel을 Handler·OM Catalog에 등록한다.
2.  수정 가능필드와 상태전이 기준을 업무 승인한다.
3.  Version Column·상세 Response·화면 Request 계약을 확정한다.
4.  현재값 조회와 상태·권한 Rule을 구현한다.
5.  상태·Version·지점조건 UPDATE SQL을 구현한다.
6.  취소 시 활성 중복키 해제정책을 확정한다.
7.  수정 중 새 예약일시의 Unique 충돌을 검증한다.
8.  수정·취소 Idempotency Key와 Fingerprint를 구현한다.
9.  History Header·Detail 구조와 개인정보 정책을 확정한다.
10.  Master·History Rollback 통합 테스트를 자동화한다.
11.  UPDATE 0건·Version 충돌·무권한 오류 Mapping을 표준화한다.
12.  충돌·History 실패·UNKNOWN 거래 Metric과 Alert를 구축한다.

## 중장기 발전 방향

\`\`\`text id=“chg35195” PK 단순 UPDATE

→ 상태·Version·권한 조건 UPDATE

마지막 저장 우선

→ 낙관적 동시성

물리 Delete

→ 상태전이·보존·복구

Version 충돌 오류

→ Idempotency 결과복원

단순 History 문장

→ Header·Field Change Set

변경 이력 단독

→ Domain History + Audit

수작업 오류분석

→ GUID·Version·History 통합추적

정상 수정 테스트

→ 동시성·Rollback·Timeout 자동검증


\---

\# 마무리말

수정·취소·이력 구현을 완료하려면 다음 질문에 답할 수 있어야 한다.

\`\`\`text id="chg35196"
어떤 상태에서 수정과 취소가 가능한가?

기능권한과 데이터권한을 모두 확인하는가?

화면이 보낸 사용자·지점·상태를 신뢰하지 않는가?

수정 가능한 필드가 명시적으로 제한돼 있는가?

상세조회 Version이 수정요청에 다시 전달되는가?

사전 Version 검증과 SQL Version 조건이 모두 있는가?

UPDATE 영향 행이 정확히 1건인가?

0건이면 존재·권한·상태·동시성 중 무엇인가?

완료·취소 예약을 변경할 수 없는가?

취소가 물리 DELETE가 아닌 상태전이인가?

취소 후 중복예약 키를 어떻게 처리하는가?

같은 수정·취소 요청이 재전송되면 어떤 결과를 반환하는가?

동일 Idempotency Key로 다른 요청을 차단하는가?

Timeout 후 실제 Commit 결과를 확인할 수 있는가?

변경 전과 변경 후 값이 모두 남는가?

실제로 바뀐 필드만 History에 기록되는가?

Master와 History가 같은 Transaction인가?

History 실패 시 Master가 Rollback되는가?

상담메모와 개인정보가 로그에 노출되지 않는가?

오류 응답에는 안전한 메시지와 GUID가 있는가?

동시성·중복·권한·Rollback 테스트가 자동화됐는가?

제35장의 핵심 흐름은 다음과 같다.

\`\`\`text id=“chg35197” 현재 데이터 조회

→ 상태·권한 검증

→ Version 비교

→ 허용 필드 변경

→ 조건부 UPDATE

→ 영향 행 검증

→ 변경 전후 History

→ Commit

→ 멱등 결과·운영 추적



가장 중요한 원칙은 다음과 같다.

\`\`\`text id="chg35198"
수정은 현재 데이터를 덮어쓰는 작업이 아니다.

사용자가 보았던 상태와
DB의 최신 상태가 같은지 확인하고,

업무상 허용된 필드만
정확히 한 건 변경해야 한다.

취소는 데이터를 지우는 일이 아니라
취소됐다는 사실을 상태와 이력으로 보존하는 일이다.

Master·Version·History·Idempotency가
하나의 일관된 계약으로 동작할 때

동시 사용자와 장애 상황에서도
안전한 변경거래가 완성된다.
