<!-- source: ztcf-집필본/NSIGHT TCF Chapter 18- 등록 거래 구현.docx -->
<!-- tables: mammoth-html + placeholder table-to-md -->

# 제18장. 등록 거래

## 이 장을 시작하며

제17장에서는 여러 건의 데이터를 제한된 범위와 안정된 순서로 조회하는 목록·페이징 거래를 구현했다.

이제 새로운 업무 데이터를 생성하는 등록 거래를 구현한다.

등록 거래는 조회 거래와 본질적으로 다르다.

조회 거래
→ 데이터를 읽는다.
→ 실패해도 원본 데이터가 바뀌지 않는다.

등록 거래
→ 새로운 상태를 만든다.
→ 한 번의 잘못된 처리가 데이터에 남는다.
→ 중복·부분 반영·재요청을 반드시 통제해야 한다.

초보 개발자는 등록 거래를 다음처럼 생각하기 쉽다.

요청을 받는다.
→ INSERT SQL을 실행한다.
→ 성공 응답을 반환한다.

그러나 운영 가능한 등록 거래는 다음 질문에 모두 답해야 한다.

무엇을 새로 만드는가?

등록 대상 데이터의 소유 업무는 어디인가?

누가 등록할 수 있는가?

클라이언트가 입력할 값과 서버가 생성할 값은 무엇인가?

등록 시 최초 상태는 무엇인가?

동일한 업무 데이터가 이미 존재하면 어떻게 하는가?

같은 요청이 네트워크 문제로 다시 들어오면 어떻게 하는가?

동시에 두 요청이 들어오면 어느 요청이 성공하는가?

Master는 등록됐는데 Detail 등록이 실패하면 어떻게 하는가?

Timeout이 발생했지만 DB Commit은 완료됐을 가능성이 있는가?

성공 응답을 받지 못한 사용자가 다시 버튼을 누르면 어떻게 되는가?

등록 결과를 무엇으로 확인할 수 있는가?

누가 무엇을 등록했는지 감사할 수 있는가?

등록 거래에는 서로 다른 두 가지 중복 문제가 존재한다.

1\. 업무 데이터 중복
같은 업무 의미의 데이터가 두 번 생성되는 문제

2\. 요청 중복
같은 사용자의 같은 요청이 여러 번 전달되는 문제

예를 들어 캠페인을 등록한다고 가정한다.

캠페인명과 기준일이 같은 데이터가 이미 존재
→ 업무 키 중복

사용자가 등록 버튼을 두 번 클릭
→ 동일 요청 중복

첫 요청이 Timeout된 후 같은 요청을 재전송
→ 재요청·처리결과 불명확

서로 다른 사용자가 동시에 같은 캠페인을 등록
→ 동시성 업무 중복

이 문제들은 서로 다른 통제로 해결해야 한다.

업무 데이터 중복
→ 업무 키
→ 사전 존재 검증
→ DB Unique Constraint

동일 요청 중복
→ Idempotency Key
→ 요청 Hash
→ PROCESSING·SUCCESS·FAIL·TIMEOUT 상태

화면 버튼을 비활성화하는 것은 사용자 편의 기능일 뿐이다.

UI 중복클릭 방지
≠ 서버 중복요청 방지
≠ 업무 데이터 중복 방지

NSIGHT TCF에서는 등록·변경·삭제·승인·외부 연계처럼 중복 실행의 영향이 있는 거래에 Idempotency를 우선 적용하고, STF에서 요청 상태를 확인한 후 업무 Handler를 실행하는 구조를 기준으로 한다.

대표 등록 거래는 다음과 같이 정의할 수 있다.

ServiceId
CM.Campaign.create

거래코드
CM-CMD-0001

처리유형
COMMAND

Transaction
Facade @Transactional(timeout = 5)

Idempotency
필수

Audit
필수

Timeout
5초

표준 정상 흐름:

표준 요청 수신
↓
STF 공통검증
↓
Idempotency PROCESSING 예약
↓
Handler
↓
Facade Transaction 시작
↓
Service·Rule
↓
DAO·Mapper INSERT
↓
영향 행 수 검증
↓
Transaction Commit
↓
ETF Idempotency SUCCESS
↓
거래로그·감사로그 성공 종료
↓
등록 결과 응답

이 흐름은 TCF 등록 거래의 정상 예시와 동일한 책임 배치를 따른다.

## 핵심 관점

등록 거래의 성공은
INSERT 문이 실행된 순간이 아니다.

업무 중복이 방지되고,
필수 데이터가 하나의 트랜잭션으로 반영되며,
Commit된 결과를 다시 조회할 수 있고,
동일 요청이 재전송돼도 한 번만 처리되며,
사용자·운영자가 결과를 추적할 수 있어야
등록이 완료된 것이다.

## 학습 목표

이 장을 마치면 다음 내용을 설명하고 구현할 수 있어야 한다.

| 번호 | 학습 목표 |
| --- | --- |
| 1 | 조회 거래와 등록 거래의 차이를 설명한다. |
| 2 | 등록 대상 데이터의 소유 도메인을 확인한다. |
| 3 | 업무 키와 DB PK의 차이를 설명한다. |
| 4 | 업무 데이터 중복과 요청 중복을 구분한다. |
| 5 | 사전 중복 조회만으로 동시 등록을 막을 수 없는 이유를 설명한다. |
| 6 | DB Unique Constraint를 최종 중복 방어선으로 사용한다. |
| 7 | 클라이언트 입력값과 서버 생성값을 구분한다. |
| 8 | 등록자·등록시각·지점·최초 상태를 서버에서 생성한다. |
| 9 | Sequence·UUID 등 식별자 생성방식을 선택한다. |
| 10 | MAX(ID)+1 방식의 위험을 설명한다. |
| 11 | 등록 Command DTO를 설계한다. |
| 12 | Facade에 변경 트랜잭션을 선언한다. |
| 13 | Master·Detail 등록을 하나의 트랜잭션으로 처리한다. |
| 14 | INSERT 영향 행 수를 검증한다. |
| 15 | 등록 후 생성 ID와 상태를 응답한다. |
| 16 | Idempotency Key의 생성·전달·저장 책임을 설명한다. |
| 17 | PROCESSING·SUCCESS·FAIL·TIMEOUT·UNKNOWN 상태를 구분한다. |
| 18 | 같은 Key로 다른 Body가 전달된 경우를 차단한다. |
| 19 | 성공한 요청의 재전송 정책을 정의한다. |
| 20 | Timeout 후 무조건 재등록하면 안 되는 이유를 설명한다. |
| 21 | 등록 거래의 감사로그를 기록한다. |
| 22 | 동시 등록·중복 요청·부분 실패를 테스트한다. |
| 23 | 등록 결과를 응답·DB·로그로 교차 검증한다. |
| 24 | 등록 계약 변경의 호환성과 폐기 영향을 판단한다. |

# 한눈에 보는 등록 거래 흐름

┌────────────────────────────────────────────────────────────┐
│ 1. 화면·Client │
│ 등록 요청 + Idempotency Key │
└─────────────────────────┬──────────────────────────────────┘
▼
┌────────────────────────────────────────────────────────────┐
│ 2. STF │
│ 인증·권한·거래통제·Idempotency 예약 │
└─────────────────────────┬──────────────────────────────────┘
▼
┌────────────────────────────────────────────────────────────┐
│ 3. Handler │
│ Request DTO 변환·Validation │
└─────────────────────────┬──────────────────────────────────┘
▼
┌────────────────────────────────────────────────────────────┐
│ 4. Facade │
│ 변경 트랜잭션 시작 │
└─────────────────────────┬──────────────────────────────────┘
▼
┌────────────────────────────────────────────────────────────┐
│ 5. Service·Rule │
│ 업무 키·상태·권한·기본값 판단 │
└─────────────────────────┬──────────────────────────────────┘
▼
┌────────────────────────────────────────────────────────────┐
│ 6. Command 생성 │
│ ID·등록자·등록시각·최초 상태 │
└─────────────────────────┬──────────────────────────────────┘
▼
┌────────────────────────────────────────────────────────────┐
│ 7. DAO·Mapper │
│ Master INSERT → Detail INSERT │
└─────────────────────────┬──────────────────────────────────┘
▼
┌────────────────────────────────────────────────────────────┐
│ 8. 영향 행 수·등록결과 검증 │
└─────────────────────────┬──────────────────────────────────┘
▼
┌────────────────────────────────────────────────────────────┐
│ 9. Transaction Commit │
└─────────────────────────┬──────────────────────────────────┘
▼
┌────────────────────────────────────────────────────────────┐
│ 10. ETF │
│ Idempotency SUCCESS·거래로그·감사로그 │
└─────────────────────────┬──────────────────────────────────┘
▼
┌────────────────────────────────────────────────────────────┐
│ 11. Response │
│ 생성 ID·상태·등록시각 │
└────────────────────────────────────────────────────────────┘

# 등록 거래의 데이터 계약

## 요청 예

{
"header": {
"businessCode": "CM",
"serviceId": "CM.Campaign.create",
"transactionCode": "CM-CMD-0001",
"processingType": "COMMAND",
"channelId": "WEBTOP",
"idempotencyKey": "IDEMP-20260718-CM-7f9c2e9a5e12"
},
"body": {
"campaignName": "2026년 하반기 우수고객 캠페인",
"campaignTypeCode": "TARGET",
"startDate": "2026-08-01",
"endDate": "2026-08-31",
"description": "우수고객 대상 마케팅 캠페인",
"targetGradeCodes": \[
"VIP",
"GOLD"
\]
}
}

## 응답 예

{
"header": {
"businessCode": "CM",
"serviceId": "CM.Campaign.create",
"transactionCode": "CM-CMD-0001",
"guid": "G-20260718-000301",
"traceId": "T-20260718-000301"
},
"result": {
"resultStatus": "SUCCESS",
"resultCode": "0000",
"message": "정상 처리되었습니다."
},
"body": {
"campaignId": "CMP-20260718-000001",
"campaignStatus": "DRAFT",
"createdAt": "2026-07-18T10:30:00+09:00",
"duplicate": false
},
"error": null
}

# 현재 구현과 목표 구조

## 실제 기준 소스에서 확인되는 Idempotency 구성

현재 TCF Core에는 다음 Interface가 존재한다.

public interface IdempotencyChecker {

void checkAndMarkProcessing(
StandardHeader header
);

void markSuccess(
StandardHeader header
);

void markFail(
StandardHeader header
);
}

메모리 구현은 다음 구조를 가진다.

ConcurrentHashMap
↓
idempotencyKey 또는 GUID
↓
PROCESSING·SUCCESS·FAIL 상태 저장

## 현재 구현 상태 판단

| 항목 | 상태 | 판단 |
| --- | --- | --- |
| IdempotencyChecker Interface | 구현 확인 | 확장 가능한 구조 |
| STF PROCESSING 확인 | 구현 확인 | 공통 경계 적용 가능 |
| ETF SUCCESS·FAIL 상태 반영 | 구현 확인 | 기본 상태 전환 |
| 메모리 동시성 자료구조 | 구현 확인 | 단일 JVM 내 처리 가능 |
| Idempotency Key 우선 사용 | 구현 확인 | Key 없으면 GUID 사용 |
| PROCESSING 중복 차단 | 구현 확인 | 기본 중복 차단 |
| SUCCESS 재요청 처리 | 보완 필요 | 기존 응답 반환 정책 필요 |
| 같은 Key·다른 Body 비교 | 미구현 확인 | Request Hash 필요 |
| Timeout·UNKNOWN 상태 | 보완 필요 | 상태 구분 필요 |
| 상태 만료·정리 | 보완 필요 | TTL·정리 Job 필요 |
| 공유 영속 저장소 | 보완 필요 | 다중 인스턴스 필수 |
| DB Unique Constraint | 설계 기준 | 최종 중복 방어 필요 |
| 응답 재사용 저장 | 권장 확장 | 성공 재요청 처리 |
| 운영 조회 화면 | 권장 확장 | UNKNOWN·PROCESSING 확인 |

## 메모리 구현의 한계

단일 JVM에서만 상태 공유

애플리케이션 재기동 시 상태 소실

VM·Tomcat·WAR가 다르면 상태 불일치

SUCCESS 상태의 동일 요청 정책 미흡

오래된 상태가 계속 메모리에 남을 수 있음

실제 DB Commit과 상태 불일치 가능

따라서 로컬 개발과 단일 인스턴스 검증에는 사용할 수 있지만, 다중 WAS 운영환경의 최종 구현으로 사용해서는 안 된다.

## 운영 목표 구조

STF
↓
JDBC Idempotency Store
├─ Atomic INSERT
├─ Unique Constraint
├─ Request Hash
├─ PROCESSING
├─ SUCCESS
├─ FAIL
├─ TIMEOUT
├─ UNKNOWN
└─ EXPIRED
↓
업무 Transaction
↓
ETF 상태 갱신

# 등록 거래의 목표 아키텍처

StandardRequest
↓
STF
├─ Header 검증
├─ 인증·권한
├─ 거래통제
├─ Idempotency 필수 여부
├─ Request Hash
└─ PROCESSING 예약
↓
CampaignCreateRequest
↓
Handler
↓
CampaignFacade
├─ @Transactional(timeout = 5)
└─ Service 호출
↓
CampaignService
├─ AuthenticationContext
├─ 업무 키 생성
├─ 중복 사전검증
├─ ID 생성
├─ 최초 상태 결정
├─ Command 생성
├─ Master 등록
├─ Detail 등록
├─ 영향 행 수 확인
└─ Response 생성
↓
Transaction Commit
↓
ETF
├─ Idempotency SUCCESS
├─ 거래로그 SUCCESS
├─ 감사로그
└─ Metric

# 18.1 업무 키와 중복 방지

## 18.1.1 업무 키란 무엇인가

업무 키는 업무 관점에서 한 데이터를 유일하게 식별하는 값 또는 값의 조합이다.

예:

| 업무 데이터 | 업무 키 예 |
| --- | --- |
| 고객 | 고객번호 |
| 캠페인 | 캠페인코드 |
| 일자별 실적 | 기준일 + 조직코드 + 실적유형 |
| 상담 메모 | 고객번호 + 상담시각 + 상담자 |
| 배치 실행 | Job ID + 기준일 + 실행회차 |
| 파일 적재 | 파일 Hash + 업무일자 |
| 이벤트 | 원천 시스템 + 원천 이벤트 ID |

업무 키는 DB 내부 PK와 다를 수 있다.

DB PK
\= 내부 Row 식별

업무 키
\= 업무적으로 같은 데이터인지 판단

## 18.1.2 내부 PK와 업무 키 분리

권장:

CAMPAIGN\_ID
→ DB 내부 PK 또는 시스템 식별자

CAMPAIGN\_CODE
→ 업무에서 사용하는 캠페인 식별자

CAMPAIGN\_NAME + START\_DATE
→ 중복 검토용 업무조건

테이블 예:

CREATE TABLE CM\_CAMPAIGN (
CAMPAIGN\_ID VARCHAR2(40) NOT NULL,
CAMPAIGN\_CODE VARCHAR2(30) NOT NULL,
CAMPAIGN\_NAME VARCHAR2(200) NOT NULL,
CAMPAIGN\_TYPE\_CODE VARCHAR2(20) NOT NULL,
START\_DATE DATE NOT NULL,
END\_DATE DATE NOT NULL,
STATUS\_CODE VARCHAR2(20) NOT NULL,
CREATED\_BY VARCHAR2(50) NOT NULL,
CREATED\_BRANCH\_CODE VARCHAR2(30) NOT NULL,
CREATED\_AT TIMESTAMP NOT NULL,
VERSION\_NO NUMBER(10) NOT NULL,
CONSTRAINT PK\_CM\_CAMPAIGN
PRIMARY KEY (CAMPAIGN\_ID),
CONSTRAINT UK\_CM\_CAMPAIGN\_01
UNIQUE (CAMPAIGN\_CODE)
);

## 18.1.3 업무 키를 먼저 정의해야 하는 이유

업무 키가 없으면 다음 질문에 답할 수 없다.

같은 데이터가 이미 있는가?

등록 버튼을 다시 누르면 신규인가 중복인가?

다른 사용자가 동시에 등록하면 어떻게 되는가?

파일을 다시 적재하면 같은 데이터인가?

어떤 데이터를 수정·삭제해야 하는가?

업무 키를 Java 코드에서 임의로 만들지 않고 데이터 모델·업무 규칙·DB 제약조건과 함께 확정한다.

## 18.1.4 업무 중복과 요청 중복

| 구분 | 업무 중복 | 요청 중복 |
| --- | --- | --- |
| 질문 | 같은 업무 데이터가 존재하는가 | 같은 요청이 다시 왔는가 |
| 기준 | 업무 키 | Idempotency Key |
| 예 | 동일 캠페인코드 | 동일 버튼 요청 |
| 주체 | 업무 Service·Rule·DB | STF·Idempotency Store |
| 최종 방어 | Unique Constraint | Idempotency Unique Key |
| 다른 사용자 | 중복 가능 | 일반적으로 다른 요청 |
| 다른 Body | 업무 키에 따라 판단 | 같은 Key면 오류 |
| 목적 | 데이터 무결성 | 중복 실행 방지 |

두 통제 중 하나만 구현해서는 충분하지 않다.

## 18.1.5 사전 중복 조회

public void validateCampaignCodeAvailable(
String campaignCode) {

if (campaignDao.existsByCampaignCode(
campaignCode)) {

throw new BusinessException(
"E-CM-CMP-0001",
"이미 사용 중인 캠페인 코드입니다."
);
}
}

장점:

사용자에게 명확한 업무 메시지 제공

불필요한 INSERT 시도 감소

업무 상태를 추가 검증 가능

하지만 사전 조회만으로 동시 등록을 완전히 막을 수 없다.

## 18.1.6 사전 조회의 동시성 문제

요청 A
→ 중복 조회: 없음

요청 B
→ 중복 조회: 없음

요청 A
→ INSERT

요청 B
→ INSERT

두 요청 모두 중복 조회 시점에는 데이터가 없었다.

따라서 최종적으로 DB Unique Constraint가 필요하다.

사전 조회
\= 친절한 업무 검증

Unique Constraint
\= 동시성 최종 방어

## 18.1.7 DB Unique Constraint

CREATE UNIQUE INDEX UX\_CM\_CAMPAIGN\_01
ON CM\_CAMPAIGN (
CAMPAIGN\_CODE
);

복합 업무 키:

CREATE UNIQUE INDEX UX\_SV\_DAILY\_RESULT\_01
ON SV\_DAILY\_RESULT (
BASE\_DATE,
BRANCH\_CODE,
RESULT\_TYPE\_CODE
);

Unique Constraint 없이 애플리케이션 SELECT EXISTS만 사용하는 것은 안전하지 않다.

## 18.1.8 Duplicate Key 예외 처리

try {
int inserted =
campaignDao.insertCampaign(command);

validateInsertedCount(inserted);

} catch (DuplicateKeyException exception) {
throw new BusinessException(
"E-CM-CMP-0001",
"이미 등록된 캠페인입니다.",
exception
);
}

주의:

모든 DuplicateKeyException
→ 같은 업무 오류

로 처리해서는 안 된다.

다음 중 어떤 제약조건이 실패했는지 구분해야 한다.

캠페인코드 중복

내부 PK 중복

멱등성 키 중복

상세 Sequence 중복

잘못된 데이터 적재

가능한 경우 제약조건명을 기준으로 안전하게 분류한다.

## 18.1.9 중복 발생 시 응답 정책

업무 중복:

{
"result": {
"resultStatus": "FAIL",
"resultCode": "E-CM-CMP-0001",
"message": "이미 등록된 캠페인입니다."
},
"error": {
"errorType": "BUSINESS"
}
}

동일 성공 요청 재전송:

{
"result": {
"resultStatus": "SUCCESS",
"resultCode": "0000",
"message": "이미 처리된 요청입니다."
},
"body": {
"campaignId": "CMP-20260718-000001",
"duplicate": true
}
}

두 상황을 같은 오류로 처리하지 않는다.

## 18.1.10 상태를 포함한 업무 중복

같은 캠페인코드가 존재해도 상태에 따라 정책이 다를 수 있다.

| 기존 상태 | 신규 등록 |
| --- | --- |
| DRAFT | 중복 거절 |
| APPROVED | 중복 거절 |
| RUNNING | 중복 거절 |
| COMPLETED | 재사용 정책 검토 |
| CANCELLED | 신규 버전 허용 가능 |
| DELETED | 복구·재등록 정책 |

단순히 COUNT(\*) > 0만 확인하지 말고 상태와 이력 정책을 정의한다.

## 18.1.11 Soft Delete와 Unique Key

논리삭제 컬럼:

DELETE\_YN = Y

기존 삭제 데이터를 무시하고 같은 업무 키를 재사용하려면 DB Unique 설계를 별도로 검토해야 한다.

대안:

업무 키 + 활성여부

업무 키 + Version

삭제 시 업무 키 변경

삭제 데이터도 영구 중복으로 판단

DB 제품과 데이터 보존정책을 고려해 DA·DBA와 결정한다.

## 18.1.12 데이터 소유권

등록은 반드시 데이터 소유 도메인에서 수행한다.

금지:

IC Service
↓
CM Mapper 직접 호출
↓
CM\_CAMPAIGN INSERT

권장:

IC 업무
↓
CM.Campaign.create
↓
CM 업무 WAR
↓
CM\_CAMPAIGN INSERT

같은 DB를 사용하더라도 다른 업무 소유 테이블을 직접 등록하지 않는다.

# 18.2 기본값과 생성 정보

## 18.2.1 클라이언트 입력과 서버 생성정보

등록 Request는 사용자가 결정할 수 있는 값만 포함해야 한다.

### 클라이언트 입력

캠페인명

캠페인 유형

시작일·종료일

설명

대상 등급

### 서버 생성

campaignId

campaignCode

createdBy

createdBranchCode

createdAt

statusCode

versionNo

guid

traceId

auditId

## 18.2.2 Request DTO

public record CampaignCreateRequest(

@NotBlank
@Size(max = 200)
String campaignName,

@NotNull
CampaignType campaignType,

@NotNull
@FutureOrPresent
LocalDate startDate,

@NotNull
@FutureOrPresent
LocalDate endDate,

@Size(max = 2000)
String description,

@NotEmpty
@Size(max = 20)
Set<@NotBlank String> targetGradeCodes
) {}

포함 금지:

public record CampaignCreateRequest(
String campaignId,
String createdBy,
String createdBranchCode,
String statusCode,
LocalDateTime createdAt,
Long versionNo
) {}

클라이언트가 감사정보와 최초 상태를 결정하게 해서는 안 된다.

## 18.2.3 인증 Context에서 생성할 정보

AuthenticationContext auth =
context.getAuthenticationContext();

String createdBy =
auth.getUserId();

String createdBranchCode =
auth.getBranchId();

금지:

String createdBy =
request.createdBy();

Header에도 사용자 ID가 존재한다면 STF가 JWT Claim과 정합성을 검증한 뒤 생성된 Authentication Context를 사용한다.

## 18.2.4 최초 상태

등록 즉시 어떤 상태로 생성되는지 정의한다.

예:

캠페인
→ DRAFT

승인 요청
→ REQUESTED

파일 업로드
→ RECEIVED

배치 실행
→ READY

고객 메모
→ ACTIVE

Request에서 최초 상태를 받지 않는다.

CampaignStatus initialStatus =
CampaignStatus.DRAFT;

업무별 최초 상태는 Rule 또는 Domain 정책으로 관리한다.

## 18.2.5 생성시각

다음 중 하나를 기준으로 확정한다.

### 애플리케이션 시각

LocalDateTime createdAt =
businessClock.now();

장점:

테스트 가능

응답과 Command에 동일값 사용

업무 기준시각 적용 가능

조건:

서버 시간 동기화

Timezone 표준

Clock 주입

### DB 시각

SYSTIMESTAMP

장점:

DB 저장시각 단일 기준

여러 애플리케이션 서버 간 편차 감소

주의:

응답에서 생성시각이 필요하면
RETURNING·재조회 필요

프로젝트 내에서 두 방식을 무분별하게 혼용하지 않는다.

## 18.2.6 업무일자와 시스템시각

다음을 구분한다.

시스템시각
\= 실제 처리시각

업무일자
\= 영업일·기준일

효력시작일
\= 업무 적용 시작일

예:

2026-07-18 00:30에 등록

은행 영업일
\= 2026-07-17

캠페인 시작일
\= 2026-08-01

한 개의 createdAt으로 모든 날짜 의미를 표현하지 않는다.

## 18.2.7 식별자 생성방식

| 방식 | 장점 | 주의 |
| --- | --- | --- |
| DB Sequence | Oracle 친화·충돌 방지 | DB 의존 |
| UUID | 분산 생성 가능 | 길이·Index 비용 |
| ULID 계열 | 시간 정렬 가능 | 표준·라이브러리 확정 필요 |
| 업무 채번 테이블 | 업무 형식 지원 | Lock·병목 |
| 조합 업무코드 | 사람이 읽기 쉬움 | 충돌·예측성 |
| Identity | 단순 | DB·배포 제약 |

## 18.2.8 DB Sequence 예

CREATE SEQUENCE SQ\_CM\_CAMPAIGN
START WITH 1
INCREMENT BY 1
CACHE 100
NOORDER;

Mapper:

<insert
id="insertCampaign"
parameterType="CampaignCreateCommand">

<selectKey
keyProperty="campaignSequence"
resultType="long"
order="BEFORE">
SELECT SQ\_CM\_CAMPAIGN.NEXTVAL
FROM DUAL
</selectKey>

INSERT INTO CM\_CAMPAIGN (
CAMPAIGN\_ID,
CAMPAIGN\_CODE,
CAMPAIGN\_NAME,
STATUS\_CODE,
CREATED\_BY,
CREATED\_AT,
VERSION\_NO
) VALUES (
#{campaignId},
#{campaignCode},
#{campaignName},
#{statusCode},
#{createdBy},
#{createdAt},
1
)

</insert>

## 18.2.9 MAX(ID)+1 금지

금지:

SELECT MAX(CAMPAIGN\_NO) + 1
FROM CM\_CAMPAIGN

동시 요청:

요청 A
→ MAX + 1 = 101

요청 B
→ MAX + 1 = 101

두 요청
→ 같은 번호 사용

Sequence·UUID·승인된 채번 서비스를 사용한다.

## 18.2.10 업무코드 생성

사람이 읽는 업무코드가 필요할 수 있다.

CMP-20260718-000001

다음 사항을 정의한다.

생성 주체

일련번호 범위

일자 전환

지점별 채번 여부

재사용 여부

실패 시 번호 공백 허용 여부

동시성

DR 전환

업무번호가 연속이어야 한다는 요구는 성능과 분산운영에 큰 영향을 주므로 실제 법적·업무적 필요를 확인한다.

## 18.2.11 Command DTO

public record CampaignCreateCommand(
String campaignId,
String campaignCode,
String campaignName,
CampaignType campaignType,
LocalDate startDate,
LocalDate endDate,
String description,
CampaignStatus status,
String createdBy,
String createdBranchCode,
LocalDateTime createdAt,
long versionNo
) {

public static CampaignCreateCommand create(
CampaignCreateRequest request,
AuthenticationContext auth,
CampaignIdGenerator idGenerator,
CampaignCodeGenerator codeGenerator,
BusinessClock clock) {

LocalDateTime now =
clock.now();

return new CampaignCreateCommand(
idGenerator.nextId(),
codeGenerator.nextCode(now.toLocalDate()),
request.campaignName(),
request.campaignType(),
request.startDate(),
request.endDate(),
request.description(),
CampaignStatus.DRAFT,
auth.getUserId(),
auth.getBranchId(),
now,
1L
);
}
}

## 18.2.12 Master·Detail 생성정보

캠페인 Master와 대상 등급 Detail을 등록한다고 가정한다.

Master
→ campaignId 생성

Detail
→ campaignId 참조
→ detailSequence 생성
→ createdAt·createdBy 공통 적용

Command 생성 시 동일한 등록시각과 등록자를 사용한다.

Master.createdAt
\=
Detail.createdAt

계층마다 now()를 다시 호출해 미세하게 다른 시각을 만들지 않는다.

## 18.2.13 DB Default의 역할

DB Default:

CREATED\_AT TIMESTAMP
DEFAULT SYSTIMESTAMP

장점:

직접 SQL 입력의 방어

누락 방지

데이터 무결성

그러나 애플리케이션이 어떤 기본값이 적용되는지 모르는 구조는 피한다.

애플리케이션 계약
\+ DB 방어 제약

을 일치시킨다.

## 18.2.14 Null과 기본값

다음 두 상황을 구분한다.

필드 미전송
→ 서버 기본값

필드 null 전송
→ 허용 여부에 따라 오류

등록 거래에서는 명시적인 null이 데이터 삭제 의미를 갖지 않으므로 필수 필드 null은 일반적으로 Validation 오류다.

## 18.2.15 문자열 기본값 주의

금지:

String description =
request.description() == null
? ""
: request.description();

null과 빈 문자열의 의미가 다른 데이터라면 임의 변환하지 않는다.

DB 저장표준을 정한다.

선택 설명 미입력
→ null

명시적 빈 설명
→ 허용 여부 결정

# 18.3 멱등성과 재요청

## 18.3.1 멱등성이란 무엇인가

멱등성은 같은 요청을 여러 번 보내도 업무 결과가 한 번만 반영되는 성질이다.

동일 등록 요청 1회
→ 캠페인 1건

동일 등록 요청 5회
→ 캠페인 1건

HTTP 요청 횟수와 데이터 반영 횟수가 같아서는 안 된다.

## 18.3.2 중복클릭·중복요청·멱등성

| 개념 | 의미 | 처리 위치 |
| --- | --- | --- |
| 버튼 비활성화 | 사용자의 반복 클릭 억제 | UI |
| 중복요청 차단 | 동일 요청의 동시 처리 차단 | STF |
| 멱등성 | 반복 요청에도 결과 한 번 반영 | TCF+업무 DB |
| 업무 중복 방지 | 같은 업무 데이터 생성 방지 | Service+DB |
| 재처리 | 실패·불명확 거래를 통제 후 다시 수행 | OM·업무 |

## 18.3.3 Idempotency 적용 대상

| 거래 유형 | 적용 |
| --- | --- |
| 단건 조회 | 선택 |
| 목록 조회 | 선택 |
| 등록 | 필수 |
| 변경 | 필수 |
| 삭제·상태변경 | 필수 |
| 승인·확정 | 필수 |
| 외부 시스템 변경 호출 | 필수 |
| 파일 업로드 | 권장·필수 검토 |
| 배치 수동실행 | 필수 |
| 단순 다운로드 | 감사정책에 따라 |

## 18.3.4 Idempotency Key 생성 주체

| 호출자 | 생성 기준 |
| --- | --- |
| 화면 | 사용자가 등록행위를 시작할 때 1회 |
| UI Relay | 필수 거래의 Key 누락 보완 가능 |
| 내부 Client | 하위 호출 전에 생성·전파 |
| 외부 연계 Adapter | 원요청 Key와 연계 |
| Batch | Job Instance ID |
| Scheduler | 실행 예정시각 + Job ID |
| 서버 STF | 변경 거래 Key 누락 시 임의 생성보다 차단 우선 |

중요:

재시도할 때
→ 같은 Key 사용

새로운 등록을 할 때
→ 새로운 Key 사용

## 18.3.5 Key 형식

예:

IDEMP-20260718-CM-CMD-0001-7f9c2e9a5e12

Key는 추측 가능한 단순 Sequence만 사용하지 않는 것이 좋다.

Header:

{
"idempotencyKey":
"IDEMP-20260718-CM-CMD-0001-7f9c2e9a5e12"
}

## 18.3.6 중복 판단 범위

Idempotency Key 하나만 전역 비교하면 다른 업무와 충돌할 수 있다.

권장 범위:

businessCode
\+ serviceId
\+ transactionCode
\+ userId 또는 호출자
\+ channelId
\+ branchCode
\+ idempotencyKey

필요 시 Tenant·기관·Client ID를 포함한다.

## 18.3.7 Request Hash

같은 Key로 다른 Body가 들어오면 동일 재요청이 아니다.

첫 요청:

{
"idempotencyKey": "IDEMP-001",
"body": {
"campaignName": "VIP 캠페인"
}
}

두 번째 요청:

{
"idempotencyKey": "IDEMP-001",
"body": {
"campaignName": "GOLD 캠페인"
}
}

처리:

같은 Key
\+ 다른 Request Hash
→ Key 재사용 오류

Hash 생성 시 제외 검토 항목:

guid

traceId

requestTime

재전송마다 달라지는 기술 Header

포함 항목:

업무 Body

업무 결과에 영향을 주는 Header

ServiceId

호출자 범위

JSON 필드 순서가 달라도 같은 Hash가 나오도록 Canonicalization이 필요하다.

## 18.3.8 상태 모델

PROCESSING
→ SUCCESS
→ EXPIRED

PROCESSING
→ FAIL
→ 재시도 가능 또는 종료

PROCESSING
→ TIMEOUT
→ 상태확인

PROCESSING
→ UNKNOWN
→ 운영확인

| 상태 | 의미 | 동일 요청 처리 |
| --- | --- | --- |
| 없음 | 최초 요청 | PROCESSING 예약 후 실행 |
| PROCESSING | 처리 중 | 중복 처리 중 응답 |
| SUCCESS | 성공 완료 | 기존 응답 반환 |
| FAIL | 실패 확정 | 정책에 따라 재시도 |
| TIMEOUT | 시간 초과 | 상태조회 우선 |
| UNKNOWN | 결과 불명확 | 자동 재실행 금지 |
| EXPIRED | 보관기간 만료 | 신규 처리 가능 |

## 18.3.9 최초 요청 예약

운영 저장소는 다음 방식으로 원자적으로 예약해야 한다.

동일 Key Row INSERT 시도
↓
성공
→ 최초 요청

Unique 충돌
→ 기존 상태 조회

사전 SELECT 후 INSERT만 사용하면 동시성 Race Condition이 발생한다.

## 18.3.10 Idempotency 테이블 예

CREATE TABLE TCF\_IDEMPOTENCY (
IDEMPOTENCY\_ID VARCHAR2(50) NOT NULL,
IDEMPOTENCY\_KEY VARCHAR2(200) NOT NULL,
BUSINESS\_CODE VARCHAR2(20) NOT NULL,
SERVICE\_ID VARCHAR2(100) NOT NULL,
TRANSACTION\_CODE VARCHAR2(50) NOT NULL,
USER\_ID VARCHAR2(50) NOT NULL,
CHANNEL\_ID VARCHAR2(30) NOT NULL,
BRANCH\_CODE VARCHAR2(30) NOT NULL,
REQUEST\_HASH VARCHAR2(128) NOT NULL,
GUID VARCHAR2(100) NOT NULL,
TRACE\_ID VARCHAR2(100),
TX\_STATUS VARCHAR2(20) NOT NULL,
RESPONSE\_CODE VARCHAR2(50),
ERROR\_CODE VARCHAR2(50),
RESPONSE\_BODY CLOB,
RETRYABLE\_YN CHAR(1) NOT NULL,
EXPIRE\_AT TIMESTAMP NOT NULL,
CREATED\_AT TIMESTAMP NOT NULL,
UPDATED\_AT TIMESTAMP NOT NULL,
CONSTRAINT PK\_TCF\_IDEMPOTENCY
PRIMARY KEY (IDEMPOTENCY\_ID)
);

Unique:

CREATE UNIQUE INDEX UX\_TCF\_IDEMPOTENCY\_01
ON TCF\_IDEMPOTENCY (
BUSINESS\_CODE,
SERVICE\_ID,
TRANSACTION\_CODE,
USER\_ID,
CHANNEL\_ID,
BRANCH\_CODE,
IDEMPOTENCY\_KEY
);

## 18.3.11 저장 트랜잭션 주의

Idempotency의 PROCESSING 예약은 업무 트랜잭션 시작 전에 다른 요청에서 확인할 수 있어야 한다.

PROCESSING 예약
→ Commit
→ 업무 Transaction 실행

업무 Transaction Rollback과 함께 PROCESSING Row까지 사라지면 다른 요청이 동일 거래를 다시 실행할 수 있다.

따라서 다음 중 승인된 방식을 사용한다.

별도 Idempotency 저장소

별도 TransactionManager

독립된 짧은 Transaction

DB Atomic Insert 후 즉시 Commit

구현 방식은 TCF·DB 구성과 장애 복구 정책을 함께 검토해야 한다.

## 18.3.12 SUCCESS 재요청

대안 1: 기존 성공 응답 반환

장점
→ 호출자 입장에서 멱등한 성공

조건
→ 응답 안전 저장
→ 개인정보·보관기간 관리

대안 2: 이미 처리됨 응답

장점
→ 응답 저장부담 감소

조건
→ 생성 ID와 상태조회 방법 제공

권장 응답:

{
"result": {
"resultStatus": "SUCCESS",
"resultCode": "0000",
"message": "이미 처리된 요청입니다."
},
"body": {
"campaignId": "CMP-20260718-000001",
"duplicate": true
}
}

## 18.3.13 FAIL 재요청

모든 FAIL을 동일하게 재시도하지 않는다.

| 실패 유형 | 재시도 |
| --- | --- |
| 입력 오류 | 수정 후 신규 요청 |
| 업무 중복 | 재시도 불필요 |
| 권한 오류 | 권한 변경 후 신규 요청 |
| 일시적 DB 연결 오류 | 같은 Key 재시도 검토 |
| SQL 문법 오류 | 운영 수정 전 금지 |
| 외부 시스템 일시 장애 | 정책에 따른 재시도 |
| 부분 처리 가능성 | 상태확인 우선 |

RETRYABLE\_YN과 재시도 횟수·간격을 관리한다.

## 18.3.14 Timeout과 UNKNOWN

Timeout은 다음 의미일 수 있다.

TCF는 응답 제한시간 초과

하지만
DB Commit은 완료됨

또는
외부 시스템은 성공함

또는
작업 Thread가 계속 실행 중

따라서 Timeout 후 즉시 신규 Key로 다시 등록하면 중복 데이터가 생길 수 있다.

권장:

같은 Key로 결과조회

등록된 업무 키 조회

거래상태 조회

UNKNOWN 운영 확인

확정 실패일 때만 재처리

## 18.3.15 하위 시스템으로 Key 전파

CM.Campaign.create
↓
외부 메시지 발송
↓
하위 시스템

상위 요청이 재시도될 때 하위 시스템에도 같은 Idempotency Key 또는 파생 Key를 전달한다.

원요청 Key
\+ 하위 작업 구분

예:

IDEMP-001:CAMPAIGN-INSERT

IDEMP-001:MESSAGE-SEND

하위 호출마다 무작위 새 Key를 생성하면 전체 멱등성을 보장하기 어렵다.

## 18.3.16 외부 연계와 Outbox

위험:

DB 캠페인 INSERT
↓
외부 메시지 발송 성공
↓
DB Transaction Rollback

또는:

DB Commit
↓
외부 메시지 발송 실패

대안:

업무 DB 변경
\+ Outbox Event INSERT
↓
같은 Transaction Commit
↓
별도 Publisher가 외부 전송
↓
전송 Idempotency 적용

등록 결과와 외부 부수효과의 원자성이 중요하다면 Outbox Pattern을 검토한다.

## 18.3.17 현재 InMemory 구현의 사용 기준

허용:

로컬 개발

단위·통합 테스트

단일 JVM 기능 확인

TCF 흐름 교육

운영 금지 또는 보완 필수:

다중 VM

다중 Tomcat

업무 WAR 이중화

재기동 후 중복 방지 필요

성공 응답 재사용 필요

장기 상태 추적 필요

# 18.4 등록 결과 검증

## 18.4.1 등록 성공의 검증 범위

등록 성공은 다음 네 가지가 일치해야 한다.

1\. Response
2\. DB 결과
3\. 거래로그
4\. 감사로그

추가로 Idempotency 상태가 SUCCESS여야 한다.

## 18.4.2 Facade 트랜잭션

@Component
@RequiredArgsConstructor
public class CampaignFacade {

private final CampaignService campaignService;

@Transactional(timeout = 5)
public CampaignCreateResponse createCampaign(
CampaignCreateRequest request,
TransactionContext context) {

return campaignService.createCampaign(
request,
context
);
}
}

등록 거래에는 readOnly=true를 사용하지 않는다.

트랜잭션 범위:

Master INSERT

Detail INSERT

업무 이력 INSERT

필수 감사 데이터 또는 Outbox INSERT

하나의 업무 불변식에 포함되는 변경은 함께 Commit·Rollback해야 한다.

## 18.4.3 Service 구현

@Service
@RequiredArgsConstructor
public class CampaignService {

private final CampaignRule campaignRule;
private final CampaignDao campaignDao;
private final CampaignIdGenerator idGenerator;
private final CampaignCodeGenerator codeGenerator;
private final BusinessClock businessClock;

public CampaignCreateResponse createCampaign(
CampaignCreateRequest request,
TransactionContext context) {

AuthenticationContext auth =
context.getAuthenticationContext();

campaignRule.validateCreateRequest(
request,
auth
);

CampaignCreateCommand command =
CampaignCreateCommand.create(
request,
auth,
idGenerator,
codeGenerator,
businessClock
);

campaignRule.validateBusinessKeyAvailable(
command.campaignCode(),
campaignDao.existsByCampaignCode(
command.campaignCode()
)
);

int masterInserted =
campaignDao.insertCampaign(command);

validateMasterInserted(masterInserted);

List<CampaignTargetCommand> targets =
createTargetCommands(
command,
request.targetGradeCodes()
);

int detailInserted =
campaignDao.insertTargets(targets);

validateDetailInserted(
detailInserted,
targets.size()
);

return CampaignCreateResponse.of(
command.campaignId(),
command.status(),
command.createdAt()
);
}
}

## 18.4.4 Rule 구현

@Component
public class CampaignRule {

public void validateCreateRequest(
CampaignCreateRequest request,
AuthenticationContext auth) {

if (request.startDate()
.isAfter(request.endDate())) {

throw new BusinessException(
"E-CM-CMP-0002",
"캠페인 기간을 확인해 주세요."
);
}

if (!auth.hasPermission(
"CM\_CAMPAIGN\_CREATE")) {

throw new AuthorizationException(
"캠페인 등록 권한이 없습니다."
);
}
}

public void validateBusinessKeyAvailable(
String campaignCode,
boolean exists) {

if (exists) {
throw new BusinessException(
"E-CM-CMP-0001",
"이미 등록된 캠페인입니다."
);
}
}
}

Rule은 Mapper를 직접 호출하지 않는다.

## 18.4.5 DAO·Mapper

Mapper:

@Mapper
public interface CampaignMapper {

int insertCampaign(
CampaignCreateCommand command
);

int insertCampaignTarget(
CampaignTargetCommand command
);

CampaignCreatedRow selectCreatedCampaign(
String campaignId
);
}

DAO:

@Repository
@RequiredArgsConstructor
public class CampaignDao {

private final CampaignMapper mapper;

public int insertCampaign(
CampaignCreateCommand command) {

return mapper.insertCampaign(command);
}

public int insertTargets(
List<CampaignTargetCommand> targets) {

int inserted = 0;

for (CampaignTargetCommand target :
targets) {

inserted +=
mapper.insertCampaignTarget(
target
);
}

return inserted;
}
}

대량 Detail 등록은 MyBatis Batch와 실패 위치·영향 건수 정책을 별도로 검토한다.

## 18.4.6 INSERT 영향 행 수

Master:

private void validateMasterInserted(
int inserted) {

if (inserted != 1) {
throw new DataIntegrityException(
"캠페인 Master 등록 건수가 올바르지 않습니다."
);
}
}

Detail:

private void validateDetailInserted(
int inserted,
int expected) {

if (inserted != expected) {
throw new DataIntegrityException(
"캠페인 대상 등록 건수가 올바르지 않습니다."
);
}
}

등록 Mapper가 void를 반환하면 영향 행 수를 검증하기 어렵다.

## 18.4.7 등록 후 재조회

등록 응답을 Command 값만으로 만들 수 있다.

그러나 다음 값이 DB에서 생성되는 경우 재조회가 필요할 수 있다.

DB 생성시각

Trigger 생성값

DB Default

가공된 상태

Sequence 값

계산 컬럼

재조회:

CampaignCreatedRow created =
campaignDao.findCreatedCampaign(
command.campaignId()
).orElseThrow(
RegisteredDataNotFoundException::new
);

주의:

INSERT 성공
\+ 재조회 실패

는 데이터가 없는 것이 아니라 SQL·Transaction·Read Replica 지연 문제일 수 있다.

## 18.4.8 생성 ID 응답

등록 성공 응답에는 후속 조회와 화면 전환에 필요한 식별자를 제공한다.

{
"campaignId": "CMP-20260718-000001",
"campaignCode": "CMP-20260718-000001",
"campaignStatus": "DRAFT",
"createdAt": "2026-07-18T10:30:00+09:00"
}

전체 등록 데이터를 다시 반환할지 최소 결과만 반환할지 계약으로 정한다.

권장 기본:

생성 식별자

최초 상태

등록시각

후속 조회에 필요한 최소 정보

## 18.4.9 부분 실패와 Rollback

상황:

Master INSERT 성공

Detail 1 INSERT 성공

Detail 2 INSERT 실패

기대:

전체 Transaction Rollback

Master 0건

Detail 0건

성공 응답 없음

Idempotency FAIL 또는 UNKNOWN 정책

거래로그 실패

원인 예외 보존

Master만 남아서는 안 된다.

## 18.4.10 감사로그

등록 감사정보:

| 항목 | 설명 |
| --- | --- |
| Audit ID | 감사 식별자 |
| GUID | 원거래 연결 |
| ServiceId | 등록 기능 |
| 사용자 ID | 등록자 |
| 지점 ID | 등록 조직 |
| 대상 유형 | CAMPAIGN |
| 대상 ID | campaignId |
| 행위 | CREATE |
| 이전 상태 | 없음 |
| 신규 상태 | DRAFT |
| 결과 | SUCCESS·FAIL |
| 수행시각 | 감사 시각 |
| Client IP | 접속 위치 |
| Idempotency Key | 중복 추적 |
| 변경내용 | 중요 필드·마스킹 |

중요 등록 거래는 감사 대상 여부를 OM에 등록한다. 중요 업무의 감사로그는 GUID·ServiceId·사용자·대상·행위·결과를 연결하도록 설계한다.

## 18.4.11 감사로그 실패 정책

다음 중 하나를 명확히 선택한다.

### 감사로그가 업무 성공 필수조건

감사로그 INSERT 실패
→ 업무 Transaction Rollback

적합:

권한 변경

개인정보 원문 생성

관리자 정책 등록

법적 감사 필수 거래

### 감사로그를 비동기 처리

업무 Commit
→ Audit Outbox
→ 비동기 저장

조건:

Outbox 유실 방지

재처리

대사

경보

단순 Fire-and-Forget으로 감사 실패를 무시하지 않는다.

## 18.4.12 ETF와 Commit 순서

정상 순서:

Facade Method 실행
↓
Spring Transaction Commit
↓
Facade 반환
↓
TCF 업무 성공 인식
↓
ETF Idempotency SUCCESS

Idempotency를 업무 Commit 전에 SUCCESS로 표시하면 안 된다.

Idempotency SUCCESS
→ DB Commit 실패

가 발생하면 재요청이 차단되지만 실제 데이터는 존재하지 않는 상태가 된다.

## 18.4.13 등록 결과 로그

event=COMMAND\_TRANSACTION\_COMPLETED
guid=G-20260718-000301
traceId=T-20260718-000301
serviceId=CM.Campaign.create
transactionCode=CM-CMD-0001
idempotencyStatus=SUCCESS
targetType=CAMPAIGN
targetId=CMP-20260718-000001
masterAffectedRows=1
detailAffectedRows=2
resultStatus=SUCCESS
elapsedMs=284

기록 금지:

전체 Request Body

캠페인 설명의 민감정보

JWT

개인정보 대상 목록 원문

DB SQL Parameter 전체

## 18.4.14 DB 결과 확인

SELECT CAMPAIGN\_ID,
CAMPAIGN\_CODE,
CAMPAIGN\_NAME,
STATUS\_CODE,
CREATED\_BY,
CREATED\_BRANCH\_CODE,
CREATED\_AT,
VERSION\_NO
FROM CM\_CAMPAIGN
WHERE CAMPAIGN\_ID = :campaignId;

Detail:

SELECT CAMPAIGN\_ID,
TARGET\_GRADE\_CODE,
CREATED\_AT
FROM CM\_CAMPAIGN\_TARGET
WHERE CAMPAIGN\_ID = :campaignId
ORDER BY TARGET\_GRADE\_CODE;

확인:

Master 1건

Detail 요청 건수와 일치

최초 상태 정확

등록자·지점 정확

등록시각 정확

Version = 1

중복 Row 없음

# 정상 처리 흐름

1\. 화면이 Idempotency Key를 생성한다.
2\. 등록 요청을 TCF Endpoint로 전송한다.
3\. STF가 인증·권한·거래통제를 검증한다.
4\. STF가 Idempotency Key와 Request Hash를 확인한다.
5\. 최초 요청이면 PROCESSING 상태를 예약한다.
6\. Handler가 Body를 CampaignCreateRequest로 변환한다.
7\. Bean Validation이 필수·형식·범위를 검증한다.
8\. Facade가 변경 트랜잭션을 시작한다.
9\. Service가 Authentication Context를 조회한다.
10\. Rule이 기간·권한·상태를 검증한다.
11\. 서버가 ID·업무코드·등록자·등록시각·최초 상태를 생성한다.
12\. Service가 업무 키 중복을 사전 확인한다.
13\. Mapper가 Master를 INSERT한다.
14\. Mapper가 Detail을 INSERT한다.
15\. Service가 영향 행 수를 확인한다.
16\. Transaction이 Commit된다.
17\. ETF가 Idempotency를 SUCCESS로 변경한다.
18\. 거래로그와 감사로그가 성공 종료된다.
19\. 생성 ID와 상태를 응답한다.

# 업무 중복 오류 흐름

등록 요청
↓
Idempotency 최초 요청
↓
업무 키 사전 조회
↓
기존 캠페인 존재
↓
BusinessException
↓
업무 Transaction Rollback
↓
Idempotency FAIL
↓
BUSINESS 오류 응답

동시 요청에서 사전 조회를 모두 통과한 경우:

요청 A INSERT 성공

요청 B Unique Constraint 충돌
↓
DuplicateKeyException
↓
업무 중복 오류 변환

# 동일 요청 처리 중 흐름

첫 요청
→ PROCESSING
→ 업무 처리 중

같은 Key 두 번째 요청
→ 기존 PROCESSING 발견
→ Handler 미실행
→ “동일 요청이 처리 중입니다.”

# 성공 요청 재전송 흐름

첫 요청
→ DB Commit
→ Idempotency SUCCESS

동일 Key 재요청
→ SUCCESS 발견
→ 기존 생성 ID·응답 반환
→ 신규 INSERT 없음

# 같은 Key·다른 Body 흐름

동일 Key
↓
Request Hash 비교
↓
Hash 불일치
↓
KEY\_REUSED\_WITH\_DIFFERENT\_REQUEST
↓
Handler 미실행
↓
보안·오류 로그

# Timeout·UNKNOWN 흐름

PROCESSING
↓
업무 처리 지연
↓
TCF Timeout
↓
DB Commit 여부 불명확
↓
TIMEOUT 또는 UNKNOWN
↓
자동 신규 등록 금지
↓
업무 키·거래상태 조회
↓
확정 결과 후 재처리 판단

# 부분 실패 흐름

Master INSERT 1건
↓
Detail INSERT 일부
↓
DB 오류
↓
Exception
↓
Facade Transaction Rollback
↓
Master·Detail 모두 0건
↓
Idempotency FAIL·UNKNOWN 정책
↓
시스템 오류 응답

# 정상 예시

## Handler

@Component
@RequiredArgsConstructor
public class CampaignHandler
implements TransactionHandler {

private static final String CREATE =
"CM.Campaign.create";

private final CampaignFacade facade;
private final TransactionBodyConverter converter;

@Override
public Set<String> serviceIds() {
return Set.of(CREATE);
}

@Override
public Object handle(
StandardRequest<?> request,
TransactionContext context) {

CampaignCreateRequest body =
converter.convertAndValidate(
request.getBody(),
CampaignCreateRequest.class
);

return facade.createCampaign(
body,
context
);
}
}

## Facade

@Component
@RequiredArgsConstructor
public class CampaignFacade {

private final CampaignService service;

@Transactional(timeout = 5)
public CampaignCreateResponse createCampaign(
CampaignCreateRequest request,
TransactionContext context) {

return service.createCampaign(
request,
context
);
}
}

## Service 핵심 흐름

업무 Rule

→ Command 생성

→ 업무 키 사전검증

→ Master INSERT

→ Detail INSERT

→ 영향 행 수 검증

→ Response 생성

# 금지 예시

등록 Request에 createdBy를 받는다.

등록 Request에 최초 상태를 받는다.

등록 Request에 DB PK를 필수 입력하게 한다.

사용자가 보낸 branchId를 등록지점으로 신뢰한다.

업무 키를 정의하지 않고 INSERT한다.

사전 중복 SELECT만으로 중복을 막는다.

DB Unique Constraint를 만들지 않는다.

MAX(ID)+1로 식별자를 만든다.

Handler가 Mapper를 직접 호출한다.

Handler에 @Transactional을 선언한다.

Master와 Detail을 서로 다른 독립 트랜잭션으로 등록한다.

INSERT 영향 행 수를 확인하지 않는다.

등록 실패를 성공 응답으로 숨긴다.

DuplicateKeyException을 모두 동일 업무 오류로 변환한다.

등록 성공 전에 Idempotency를 SUCCESS로 표시한다.

등록 거래에 Idempotency Key를 적용하지 않는다.

재시도마다 새로운 Idempotency Key를 만든다.

같은 Key로 다른 Body를 허용한다.

TIMEOUT 후 바로 신규 Key로 재등록한다.

InMemory Idempotency를 다중 인스턴스 운영에 사용한다.

Idempotency 상태를 만료 없이 영구 보관한다.

외부 호출을 업무 DB Lock 안에서 장시간 기다린다.

감사로그 실패를 아무 기록 없이 무시한다.

등록 결과를 응답만 보고 DB에서 확인하지 않는다.

등록 Request·Response 전체를 로그에 남긴다.

# 책임 경계와 RACI

| 활동 | 업무분석 | 업무개발 | AA | DA·DBA | FW | 보안 | QA | 운영 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 등록 업무 정의 | A/R | C | C | C | I | C | C | I |
| 업무 키 정의 | A | R | C | A/R | I | I | C | I |
| PK·Unique 설계 | C | C | C | A/R | I | I | C | C |
| Request·Response | C | R | A | C | C | C | C | I |
| 최초 상태 | A/R | R | C | C | I | C | C | I |
| ID 생성정책 | C | R | A | R | C | C | C | C |
| 트랜잭션 경계 | C | R | A | C | C | I | C | C |
| Idempotency 공통구현 | I | C | A | C | R | C | C | R/C |
| 멱등성 업무등록 | C | R | A | I | C | C | C | R |
| 감사정책 | C | C | C | I | C | A/R | C | R |
| 동시성 테스트 | I | R | C | C | C | I | A/R | C |
| Rollback 검증 | I | R | C | C | C | I | A/R | C |
| 운영 상태조회 | I | C | C | I | C | C | C | A/R |

# 데이터 및 상태관리

## 업무 데이터 상태

DRAFT

REQUESTED

APPROVED

RUNNING

COMPLETED

CANCELLED

등록 거래는 최초 상태 하나만 만든다.

이후 상태 변경은 별도의 변경·승인 ServiceId가 담당한다.

## Idempotency 상태

PROCESSING

SUCCESS

FAIL

TIMEOUT

UNKNOWN

EXPIRED

업무 상태와 Idempotency 상태를 같은 컬럼으로 관리하지 않는다.

Campaign.status
≠ Idempotency.txStatus

## Version

등록 시:

VERSION\_NO = 1

변경 시:

VERSION\_NO = VERSION\_NO + 1

등록부터 Version을 생성하면 이후 낙관적 잠금에 활용할 수 있다.

## 보관기간

Idempotency Key 보관기간은 다음을 고려한다.

클라이언트 최대 재시도 기간

업무 중복 위험 기간

외부 연계 확인 기간

개인정보 보관정책

저장용량

만료 상태를 즉시 삭제하기보다 운영 추적기간과 정리 배치 기준을 정의한다.

# 성능·용량·확장성

## 등록 거래 비용

전체 처리시간
\=
STF 공통검증
\+ Idempotency 예약
\+ 업무 중복 조회
\+ ID 생성
\+ Master INSERT
\+ Detail INSERT
\+ 감사·Outbox
\+ Commit
\+ ETF 상태 갱신

## 대량 Detail

대상 등급·항목이 많은 등록은 다음을 검토한다.

단건 INSERT 반복

MyBatis Batch

다중 VALUES

임시 Table

파일·Batch 거래 분리

온라인 거래의 최대 Detail 건수를 제한한다.

## Idempotency 저장소 Index

주요 Index:

Unique 중복판단 Key

TX\_STATUS + UPDATED\_AT

EXPIRE\_AT

GUID

TRACE\_ID

PROCESSING 장기 잔존과 만료 데이터 정리 조회에 필요하다.

## 다중 인스턴스

VM A
→ 요청 1

VM B
→ 동일 요청 2

공유 DB·분산 저장소의 Unique Constraint가 두 요청 중 하나만 최초 요청으로 인정해야 한다.

# 보안·개인정보·감사

| 영역 | 기준 |
| --- | --- |
| 등록자 | 인증 Context 사용 |
| 등록지점 | 검증된 조직정보 사용 |
| 권한 | ServiceId 기능권한 |
| 데이터권한 | 생성 대상 범위 검증 |
| 업무 키 | 민감정보 직접 사용 최소화 |
| Request Hash | 원문 복구 불가 Hash |
| Idempotency 응답 | 타 사용자 결과 노출 금지 |
| 로그 | Body·Token 원문 금지 |
| 감사 | 생성 대상·행위·결과 기록 |
| Key | 예측 가능성·길이 제한 |
| 응답 저장 | 개인정보 최소화·암호화 검토 |

Idempotency Key를 알고 있다는 이유만으로 다른 사용자의 기존 성공 응답을 반환해서는 안 된다.

호출자 범위와 요청 Hash를 함께 검증한다.

# 운영·모니터링·장애 대응

## 권장 Metric

command.create.success.count

command.create.businessDuplicate.count

idempotency.processingDuplicate.count

idempotency.successReplay.count

idempotency.hashMismatch.count

idempotency.timeout.count

idempotency.unknown.count

command.rollback.count

command.affectedRowsMismatch.count

## 운영 확인 질문

PROCESSING 상태가 장시간 남아 있는가?

SUCCESS인데 업무 데이터가 없는 거래가 있는가?

FAIL인데 업무 데이터가 생성된 거래가 있는가?

같은 업무 키가 여러 건 존재하는가?

같은 Key로 다른 Request Hash가 들어왔는가?

Timeout 거래의 실제 DB 상태는 무엇인가?

어느 ServiceId에서 중복요청이 증가했는가?

특정 화면 버전에서 반복 등록이 발생하는가?

## 장기 PROCESSING 점검

PROCESSING
\+ updatedAt이 Timeout보다 오래됨
↓
거래로그 확인
↓
업무 DB 확인
↓
SUCCESS·FAIL·UNKNOWN 보정
↓
운영 감사기록

자동으로 FAIL로 바꾸기 전에 실제 업무 반영 여부를 확인한다.

# 자동검증 및 품질 Gate

## 1\. 등록 거래 식별 Gate

processingType = COMMAND

Idempotency 필수

Audit 여부 등록

Facade Transaction 존재

Timeout 정책 존재

## 2\. DTO Gate

Request DTO에 다음 필드가 있으면 검토·실패시킨다.

createdBy

createdAt

approvedBy

statusCode

versionNo

internalId

예외는 명시적 업무 근거가 있어야 한다.

## 3\. DB Gate

PK 존재

업무 Unique 존재

NOT NULL 존재

상태 Check Constraint 검토

Version 기본값

감사컬럼

FK·Index

## 4\. SQL Gate

MAX(ID)+1 금지

조건 없는 INSERT SELECT 검토

영향 행 수 반환

사용자 입력 \`${}\` 금지

다른 업무 Table INSERT 금지

## 5\. 트랜잭션 Gate

@Transactional
→ Facade

Master·Detail
→ 동일 Transaction

외부 호출
→ Lock 범위 검토

Rollback Test
→ 필수

## 6\. Idempotency Gate

Idempotency Key 필수

같은 Key·다른 Hash 차단

PROCESSING 차단

SUCCESS 재응답

TIMEOUT·UNKNOWN 자동 재실행 금지

공유 저장소

Unique Constraint

만료정책

## 7\. 감사 Gate

GUID

ServiceId

사용자

대상 ID

행위 CREATE

결과

시각

Idempotency Key 또는 안전한 식별값

## 8\. 등록 결과 Gate

Response 생성 ID

DB Master 1건

Detail 기대 건수

Idempotency SUCCESS

거래로그 SUCCESS

감사로그 SUCCESS

# 테스트 시나리오

| ID | 시나리오 | 기대 결과 |
| --- | --- | --- |
| CRT-001 | 정상 캠페인 등록 | Master·Detail Commit |
| CRT-002 | 필수 캠페인명 누락 | Validation 오류 |
| CRT-003 | 시작일 > 종료일 | 업무 오류 |
| CRT-004 | 등록권한 없음 | 권한 오류 |
| CRT-005 | 클라이언트 createdBy 위조 | 무시·거절 |
| CRT-006 | 인증 사용자 등록 | 서버 사용자 저장 |
| CRT-007 | 등록지점 | 인증 Context 지점 저장 |
| CRT-008 | 최초 상태 | DRAFT 저장 |
| CRT-009 | Version | 1 저장 |
| CRT-010 | ID 생성 | 유일값 |
| CRT-011 | 동시 ID 생성 | 충돌 없음 |
| CRT-012 | MAX+1 검출 | 품질 Gate 실패 |
| CRT-013 | 업무 키 기존 존재 | 업무 중복 오류 |
| CRT-014 | 동시 같은 업무 키 | 한 건만 성공 |
| CRT-015 | DB Unique 충돌 | 업무 오류 변환 |
| CRT-016 | Idempotency Key 누락 | STF 차단 |
| CRT-017 | 최초 Key | PROCESSING 후 실행 |
| CRT-018 | PROCESSING 동일 Key | Handler 미실행 |
| CRT-019 | SUCCESS 동일 Key | 기존 결과 반환 |
| CRT-020 | 같은 Key 다른 Body | Hash 오류 |
| CRT-021 | 다른 Key 같은 업무 키 | DB 업무 중복 차단 |
| CRT-022 | 다른 사용자 같은 Key | 범위 정책 검증 |
| CRT-023 | FAIL 재시도 가능 | 정책대로 재실행 |
| CRT-024 | FAIL 재시도 불가 | 차단 |
| CRT-025 | Timeout | TIMEOUT·UNKNOWN |
| CRT-026 | Timeout 후 신규 Key | 정책상 차단·업무 중복 방지 |
| CRT-027 | Master 성공·Detail 실패 | 전체 Rollback |
| CRT-028 | Master 영향 0건 | 시스템·정합성 오류 |
| CRT-029 | Master 영향 2건 | 정합성 오류 |
| CRT-030 | Detail 건수 불일치 | Rollback |
| CRT-031 | 감사로그 필수 실패 | 정책상 Rollback |
| CRT-032 | Outbox 등록 실패 | Rollback |
| CRT-033 | DB Connection 실패 | 시스템 오류 |
| CRT-034 | Query·Insert Timeout | Rollback |
| CRT-035 | Duplicate PK | 시스템 정합성 오류 |
| CRT-036 | 등록 응답 ID | DB ID와 일치 |
| CRT-037 | 응답 상태 | DB 상태와 일치 |
| CRT-038 | createdAt | 기준시각과 일치 |
| CRT-039 | 로그 GUID | 전체 흐름 연결 |
| CRT-040 | 민감정보 로그 | 원문 없음 |
| CRT-041 | Idempotency SUCCESS 시점 | Commit 후 갱신 |
| CRT-042 | Commit 실패 | SUCCESS 표시 안 됨 |
| CRT-043 | 재기동 후 동일 Key | JDBC 저장소에서 차단 |
| CRT-044 | 다중 인스턴스 동시 요청 | 한 요청만 실행 |
| CRT-045 | 만료 Key | 정책에 따른 신규 처리 |
| CRT-046 | 장기 PROCESSING | 운영 점검 대상 |
| CRT-047 | Soft Delete 업무 키 | 재등록 정책 일치 |
| CRT-048 | 다른 도메인 Table 등록 | Architecture Gate 실패 |
| CRT-049 | 다른 개발자 재현 | 동일 결과 |
| CRT-050 | Rollback 후 재요청 | 상태정책대로 처리 |

# 따라 하는 실무 절차

## 1단계. 등록 대상을 정의한다

완료 증적:

데이터 소유 업무

Master·Detail

최초 상태

업무 키

PK

## 2단계. 중복 정책을 확정한다

업무 중복

요청 중복

동시 요청

재요청

Timeout

완료 증적:

Unique Key

Idempotency 정책

상태별 응답

## 3단계. Request·Response를 확정한다

완료 증적:

사용자 입력

서버 생성값

생성 ID

초기 상태

오류코드

## 4단계. ID·기본값 정책을 확정한다

Sequence·UUID

등록자

등록지점

등록시각

업무일자

Version

## 5단계. Handler·Facade를 구현한다

ServiceId 등록

Request 변환

@Transactional(timeout)

## 6단계. Service·Rule을 구현한다

권한

기간

업무 키

초기 상태

Command

## 7단계. DAO·Mapper를 구현한다

Master INSERT

Detail INSERT

영향 행 수

Unique Constraint

## 8단계. Idempotency를 등록한다

OM 정책

필수 Key

상태 저장

Hash

만료

응답 재사용

## 9단계. 정상 등록을 검증한다

완료 증적:

Response

DB Master

DB Detail

거래로그

감사로그

Idempotency SUCCESS

## 10단계. 중복·동시성·Rollback을 검증한다

동일 업무 키

동일 Idempotency Key

같은 Key 다른 Body

Master·Detail 부분 실패

Timeout

## 11단계. 운영 조회를 검증한다

GUID 검색

Idempotency 상태

업무 대상 ID

영향 행 수

오류코드

## 12단계. 품질 Gate를 통과한다

계층

Transaction

Unique

Idempotency

감사

보안

테스트

# 완료 체크리스트

## 업무·데이터

| 확인 항목 | 완료 |
| --- | --- |
| 등록 데이터 소유 업무를 확인했다. | □ |
| Master·Detail 구조를 정의했다. | □ |
| 업무 키를 정의했다. | □ |
| 내부 PK와 업무 키를 구분했다. | □ |
| DB Unique Constraint가 있다. | □ |
| Soft Delete 중복정책이 있다. | □ |
| 최초 상태가 정의됐다. | □ |
| Version 초기값이 정의됐다. | □ |

## 입력·생성정보

| 확인 항목 | 완료 |
| --- | --- |
| Request에는 사용자 입력만 존재한다. | □ |
| 등록자는 인증 Context에서 가져온다. | □ |
| 등록지점은 인증 Context에서 가져온다. | □ |
| 등록시각 기준이 정의됐다. | □ |
| 업무일자와 시스템시각을 구분했다. | □ |
| ID 생성방식이 정의됐다. | □ |
| MAX+1을 사용하지 않는다. | □ |
| 서버 기본값이 문서화됐다. | □ |

## 프로그램·트랜잭션

| 확인 항목 | 완료 |
| --- | --- |
| Handler가 Facade만 호출한다. | □ |
| Facade에 변경 트랜잭션이 있다. | □ |
| Service가 Command를 생성한다. | □ |
| Rule과 DAO 책임이 분리됐다. | □ |
| Master·Detail이 같은 트랜잭션이다. | □ |
| INSERT 영향 행 수를 검증한다. | □ |
| 부분 실패 시 전체 Rollback된다. | □ |
| 외부 호출과 Lock 범위를 검토했다. | □ |

## 멱등성

| 확인 항목 | 완료 |
| --- | --- |
| 등록 거래에 Idempotency가 필수다. | □ |
| Key 생성 주체가 정의됐다. | □ |
| 재시도 시 같은 Key를 사용한다. | □ |
| Request Hash를 비교한다. | □ |
| PROCESSING 요청을 차단한다. | □ |
| SUCCESS 재요청 정책이 있다. | □ |
| FAIL 재시도 정책이 있다. | □ |
| TIMEOUT·UNKNOWN 처리정책이 있다. | □ |
| 공유 영속 저장소를 사용한다. | □ |
| Unique Constraint가 있다. | □ |
| 만료·정리 정책이 있다. | □ |

## 결과·로그·감사

| 확인 항목 | 완료 |
| --- | --- |
| 응답에 생성 ID가 있다. | □ |
| 응답에 최초 상태가 있다. | □ |
| DB Master가 1건이다. | □ |
| Detail 건수가 요청과 일치한다. | □ |
| Idempotency가 SUCCESS다. | □ |
| 거래로그가 SUCCESS로 종료됐다. | □ |
| 감사로그가 생성됐다. | □ |
| GUID로 전체 흐름을 추적한다. | □ |
| 민감정보 원문이 로그에 없다. | □ |
| Rollback 결과를 DB에서 확인했다. | □ |

# 변경·호환성·폐기 관리

## 신규 필수 필드 추가

기존 Request
→ 새 필드 없음
→ Validation 실패

비호환 변경이다.

가능하면 선택 필드로 추가하고 소비자 전환 후 필수화한다.

## 기본값 변경

기존 최초 상태
DRAFT

신규 최초 상태
REQUESTED

같은 Request가 다른 업무 결과를 만들기 때문에 중요한 업무 변경이다.

상태흐름·권한·알림·감사 영향을 분석한다.

## 업무 키 변경

기존
campaignCode

신규
campaignName + startDate

영향:

기존 중복 데이터

Unique Index

재등록 정책

정합성 정리

Idempotency와의 관계

운영 데이터 이행계획이 필요하다.

## ID 생성방식 변경

Sequence
→ UUID

영향:

컬럼 길이

Index

FK

외부 소비자

정렬

로그·파일

기존 데이터

## Idempotency 범위 변경

사용자를 Key 범위에서 제거하면 서로 다른 사용자의 요청이 충돌할 수 있다.

범위 변경은 운영 데이터와 기존 Key 상태의 호환성을 검토한다.

## 보관기간 변경

기간 축소:

재시도 가능기간보다 짧으면
→ 같은 요청이 신규 처리될 수 있음

기간 확대:

저장용량·개인정보 보관 증가

## 등록 거래 폐기

신규 화면 호출 중지
↓
외부·내부 소비자 조사
↓
대체 ServiceId 제공
↓
OM Deprecated
↓
호출량 0 확인
↓
Handler 제거
↓
Idempotency 정책 폐기
↓
DB 객체·Sequence·Index 폐기 검토

기존 등록 데이터의 보존·조회·감사 책임은 거래 폐기와 별도로 유지한다.

# 시사점

## 핵심 아키텍처 판단

첫째, 등록 거래에는 두 개의 중복 방어가 필요하다.

업무 데이터 중복
→ 업무 키 + Unique Constraint

동일 요청 중복
→ Idempotency Key + 상태 저장

둘째, 사전 중복 조회는 사용자 메시지를 위한 검증이고 DB Unique Constraint는 동시성에 대한 최종 방어다.

셋째, 등록자·등록지점·등록시각·최초 상태는 서버가 신뢰할 수 있는 Context와 정책으로 생성해야 한다.

넷째, 멱등성은 단순히 PROCESSING 요청을 막는 기능이 아니다.

SUCCESS 재응답

FAIL 재시도

TIMEOUT·UNKNOWN 확인

같은 Key·다른 Body 차단

까지 포함해야 한다.

다섯째, 등록 성공은 INSERT 호출이 아니라 Commit 완료와 등록 결과의 교차 검증으로 판단한다.

여섯째, 다중 인스턴스 환경에서는 메모리 기반 Idempotency로 업무 무결성을 보장할 수 없다.

## 주요 위험

| 위험 | 결과 |
| --- | --- |
| 업무 키 미정의 | 중복 판단 불가 |
| Unique Constraint 없음 | 동시 중복 생성 |
| Request의 감사값 신뢰 | 사용자 위·변조 |
| MAX+1 채번 | ID 충돌 |
| 부분 Transaction | Master만 남음 |
| 영향 행 수 미검증 | 실패를 성공 처리 |
| Idempotency Key 누락 | 반복 등록 |
| 재시도마다 새 Key | 멱등성 무효 |
| 같은 Key 다른 Body 허용 | 잘못된 결과 반환 |
| InMemory 운영 사용 | 인스턴스별 중복 |
| Timeout 즉시 재등록 | 이중 데이터 |
| SUCCESS 선반영 | 데이터 없는 성공상태 |
| 감사 실패 무시 | 책임 추적 불가 |
| 전체 Body 로그 | 개인정보 노출 |
| 다른 도메인 Table 등록 | 데이터 소유권 붕괴 |

## 우선 보완 과제

1.  등록·변경·삭제 거래의 Idempotency 필수 정책을 OM에 등록한다.
2.  InMemoryIdempotencyChecker를 운영용 JDBC 구현으로 교체한다.
3.  Request Hash와 같은 Key·다른 Body 검증을 추가한다.
4.  SUCCESS 재요청 시 기존 결과 반환 기능을 추가한다.
5.  TIMEOUT·UNKNOWN 상태와 운영 확인절차를 구현한다.
6.  Idempotency 상태 만료·정리 Job을 구현한다.
7.  업무별 업무 키와 DB Unique Constraint를 설계서에 필수화한다.
8.  Request DTO의 생성·감사 필드 유입을 정적 검사한다.
9.  등록 ID 생성방식을 업무별로 표준화한다.
10.  MAX(ID)+1 SQL을 품질 Gate에서 차단한다.
11.  Master·Detail Rollback 통합테스트를 필수화한다.
12.  INSERT 영향 행 수 검증을 표준화한다.
13.  Commit 후 Idempotency SUCCESS 갱신 순서를 검증한다.
14.  감사 필수 거래의 실패정책을 정의한다.
15.  등록 결과를 Response·DB·거래로그·감사로그로 자동 대사한다.

## 중장기 발전 방향

메모리 중복요청 차단
↓
JDBC 공유 Idempotency Store
↓
Request Hash·상태 모델
↓
성공 응답 재사용
↓
TIMEOUT·UNKNOWN 운영관리
↓
업무 키·Unique 자동검증
↓
Outbox 기반 외부 부수효과 처리
↓
등록 결과 자동 대사·재처리 통제

# 마무리말

등록 거래를 설계하고 구현하는 과정은 다음 질문에 답하는 일이다.

무엇이 동일한 업무 데이터인가?

업무 키와 DB PK는 무엇인가?

동시에 두 요청이 들어오면 누가 막는가?

어떤 값은 사용자가 입력하고 어떤 값은 서버가 생성하는가?

최초 상태는 무엇인가?

식별자는 누가 어떻게 생성하는가?

같은 요청이 재전송되면 어떤 결과를 반환하는가?

같은 Key로 다른 요청이 들어오면 어떻게 하는가?

Timeout된 요청이 실제로 Commit됐는지 어떻게 확인하는가?

Master와 Detail이 모두 반영되지 않으면 어떻게 Rollback하는가?

등록 결과를 어떤 응답·DB·로그·감사정보로 증명하는가?

제18장의 핵심 흐름은 다음과 같다.

업무 키 정의
↓
Request·생성정보 분리
↓
Idempotency 예약
↓
Facade Transaction
↓
업무 중복 검증
↓
ID·기본값 생성
↓
Master·Detail 등록
↓
영향 행 수 검증
↓
Commit
↓
Idempotency SUCCESS
↓
등록 결과·감사 검증

가장 중요한 원칙은 다음과 같다.

중복 조회를 했다고
중복 등록이 방지된 것은 아니다.

등록 응답을 받았다고
데이터가 안전하게 생성된 것도 아니다.

업무 키·DB 제약·멱등성·트랜잭션·감사가
함께 작동해야
신뢰할 수 있는 등록 거래가 된다.
