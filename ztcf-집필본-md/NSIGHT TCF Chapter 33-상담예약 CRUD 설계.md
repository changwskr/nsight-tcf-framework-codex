<!-- source: ztcf-집필본/NSIGHT TCF Chapter 33-상담예약 CRUD 설계.docx -->
<!-- tables: mammoth-html + placeholder table-to-md -->

# 제33장. 상담예약 CRUD 프로젝트 설계

## 도입 전 안내말

제32장까지는 NSIGHT TCF의 공통 구조, 데이터 처리, 인증·권한, 오류·Timeout, Cache, Batch, 배포, 관측성, 장애 대응과 성능 분석을 살펴보았다.

제33장부터는 지금까지 배운 기준을 하나의 업무기능에 종합 적용한다.

이번 장에서 설계할 업무는 **고객 상담예약 관리**다.

업무팀이 제공한 정보는 다음과 같다고 가정한다.

\`\`\`text id=“rsv33001” 고객 상담예약을 조회·등록·수정·취소하는 화면이 필요하다.

고객번호와 조회기간으로 예약을 조회한다.

상담예약일시와 상담목적을 등록한다.

본인이 담당하거나 같은 지점에 속한 예약만 변경할 수 있다.

완료된 예약은 수정하거나 취소할 수 없다.

예약을 변경한 이력을 보관해야 한다.



화면설계자는 다음 정도의 화면정보를 제공할 수 있다.

\`\`\`text id="rsv33002"
고객번호

조회 시작일·종료일

예약상태

조회 버튼

예약 목록 Grid

신규 버튼

상세 입력영역

저장 버튼

예약 취소 버튼

DB 설계자는 다음 테이블을 제공할 수 있다.

\`\`\`text id=“rsv33003” CT\_CONTACT\_RESERVATION

CT\_CONTACT\_RESERVATION\_HISTORY



이 정보만 보고 바로 Controller·Service·Mapper를 만들면 프로그램은 빠르게 작성할 수 있다.

그러나 다음 기준이 누락될 가능성이 높다.

\`\`\`text id="rsv33004"
화면 버튼과 ServiceId의 관계

등록과 수정의 거래 경계

예약상태와 상태전이

변경 가능한 필드

사용자와 지점의 데이터권한

동시 수정 방지

중복 등록 방지

Master와 History의 원자성

조회기간과 최대건수

Timeout과 오류코드

개인정보 조회감사

OM Service Catalog

운영 로그와 성능지표

요구사항부터 테스트까지의 추적성

상담예약 CRUD는 단순히 다음 SQL을 만드는 작업이 아니다.

\`\`\`text id=“rsv33005” INSERT

SELECT

UPDATE

DELETE



NSIGHT TCF에서 CRUD를 설계한다는 것은 다음을 결정하는 것이다.

\`\`\`text id="rsv33006"
어떤 상태의 예약을 만들 수 있는가?

어떤 사용자가 어떤 예약을 볼 수 있는가?

어떤 상태에서 어떤 필드를 변경할 수 있는가?

동시에 두 명이 수정하면 누구의 변경을 인정할 것인가?

예약 취소를 물리 삭제로 처리할 것인가?

Master 변경과 History 저장은 하나의 Transaction인가?

Timeout 후 실제 DB 반영 여부를 어떻게 확인할 것인가?

화면·ServiceId·프로그램·SQL·테스트를 어떻게 연결할 것인가?

이 장에서는 상담예약 CRUD의 **설계**를 완료한다.

실제 Java·Mapper 구현은 제34장부터 이어진다.

# 문서 개요

## 목적

본 장의 목적은 화면·요구사항·테이블 정보만 제공된 상황에서 상담예약 CRUD 업무를 NSIGHT TCF 표준에 맞는 독립 거래와 프로그램 구조로 변환하는 것이다.

세부 목적은 다음과 같다.

\`\`\`text id=“rsv33007” 원시 요구사항 구조화

화면 항목과 이벤트 정의

업무 Aggregate 정의

예약상태와 상태전이 정의

등록·조회·수정·취소 규칙 정의

기능권한과 데이터권한 분리

ServiceId와 거래코드 정의

Request·Response·Command·Data DTO 분리

Facade Transaction 경계 정의

Master·History 원자성 보장

Version 기반 낙관적 동시성 제어

논리 취소 적용

오류코드와 Timeout 정책 정의

목록조회 성능·Paging 기준 정의

테스트와 추적성 계획 수립

OM·로그·Metric·운영 기준 정의


\## 적용범위

| 구분 | 적용 대상 |
|---|---|
| 업무 | 고객 상담예약 관리 |
| 업무코드 | \`CT\` |
| 업무세구분 | \`RSV\` |
| 화면 | \`CT-RSV-0001\` |
| 거래 | 목록·상세·등록·수정·취소 |
| 프로그램 | Handler부터 Mapper |
| 데이터 | 예약 Master·History |
| 연계 | 고객 존재·상태 확인 |
| 보안 | 인증·기능권한·데이터권한 |
| 안정성 | Timeout·동시성·중복요청 |
| 운영 | OM·거래로그·감사로그·Metric |
| 테스트 | Unit·Mapper·Integration·E2E |
| 추적성 | 요구사항부터 Release |

\## 대상 독자

\`\`\`text id="rsv33008"
화면과 테이블만 받고 CRUD를 개발해야 하는 초보 개발자

CRUD를 Controller·Service·Mapper만으로 구현하는 개발자

ServiceId를 어떻게 분리할지 어려운 개발자

업무 상태와 동시성을 처음 설계하는 개발자

프로그램 설계서와 소스를 함께 작성해야 하는 개발자

AI 코딩도구로 CRUD 초안을 생성하는 개발자

신규 업무의 운영전환 자료를 준비하는 개발자

설계·코드·SQL·테스트 정합성을 검토하는 아키텍트

## 선행조건

\`\`\`text id=“rsv33009” TCF 공통 온라인 진입 흐름

ServiceId와 Handler의 관계

Facade Transaction

Service와 Rule의 책임

DAO·Mapper 구조

표준 Header·Response

JWT AuthenticationContext

오류코드와 Timeout

거래로그·감사로그

MyBatis와 Oracle 기본 사용법


\---

\# 핵심 관점

\`\`\`text id="rsv33010"
상담예약 CRUD 설계의 출발점은
화면 버튼이나 Table Column이 아니다.

데이터 소유권과 Aggregate,
상태전이와 업무규칙,
트랜잭션과 동시성 경계를 먼저 정한 뒤

화면·ServiceId·DTO·SQL을
그 결정에 맞춰 배치해야 한다.

원본 제33장도 화면 동작보다 데이터 소유권·상태전이·트랜잭션 경계를 먼저 결정하도록 강조한다.

# 학습 목표

이 장을 마치면 다음 내용을 설명하고 설계할 수 있어야 한다.

| 번호 | 학습 목표 |
| --- | --- |
| 1 | 화면·요구사항·테이블을 구조화한다. |
| 2 | 결정·가정·미결사항을 구분한다. |
| 3 | 상담예약 Aggregate를 정의한다. |
| 4 | 예약상태와 허용 상태전이를 정의한다. |
| 5 | 등록·수정·취소 Transaction을 구분한다. |
| 6 | 물리 삭제 대신 논리 취소를 적용한다. |
| 7 | Version 기반 동시성 제어를 설계한다. |
| 8 | UPDATE 영향 행 0건의 의미를 구분한다. |
| 9 | 기능권한과 데이터권한을 구분한다. |
| 10 | 인증문맥에서 사용자·지점정보를 취득한다. |
| 11 | CRUD별 ServiceId를 독립적으로 설계한다. |
| 12 | 화면 이벤트와 ServiceId를 연결한다. |
| 13 | ServiceId·거래코드·권한·Timeout을 연결한다. |
| 14 | Request·Response·Query·Command·Data DTO를 분리한다. |
| 15 | 외부 계약과 DB Column을 직접 결합하지 않는다. |
| 16 | 오류코드를 업무상황별로 설계한다. |
| 17 | Master와 History를 같은 Transaction으로 처리한다. |
| 18 | 목록조회 기간·건수·Paging 기준을 적용한다. |
| 19 | 고객정보의 데이터 소유권을 준수한다. |
| 20 | 등록 중복과 재시도를 통제한다. |
| 21 | Timeout 후 결과가 불명확한 상황을 설계한다. |
| 22 | 요구사항부터 테스트까지 추적성을 작성한다. |
| 23 | 정상·경계·오류·동시성 테스트를 설계한다. |
| 24 | OM·로그·Metric·Runbook을 설계한다. |
| 25 | 설계 완료와 구현 완료를 구분한다. |

# 핵심 용어

| 용어 | 의미 |
| --- | --- |
| CRUD | Create·Read·Update·Delete |
| Aggregate | 하나의 Transaction으로 일관성을 유지하는 업무 단위 |
| Aggregate Root | Aggregate 변경을 통제하는 대표 객체 |
| Master | 현재 상태를 보관하는 주 테이블 |
| History | 변경 이력을 보관하는 테이블 |
| State | 업무 객체의 현재 상태 |
| State Transition | 허용된 상태 변경 |
| Invariant | 항상 지켜져야 하는 업무조건 |
| Logical Delete | 행을 삭제하지 않고 상태를 취소·비활성으로 변경 |
| Optimistic Lock | Version을 조건으로 동시 변경을 통제 |
| Lost Update | 나중 변경이 앞선 변경을 모르게 덮어쓰는 문제 |
| Business Key | 업무적으로 중복을 판단하는 키 |
| Request DTO | 화면에서 서버로 전달되는 외부 요청계약 |
| Response DTO | 서버에서 화면으로 반환되는 외부 응답계약 |
| Query DTO | 내부 조회조건 |
| Command DTO | 내부 변경명령 |
| Data DTO | Mapper와 DB 결과를 표현하는 데이터객체 |
| ServiceId | 화면 요청과 업무 처리기를 연결하는 논리적 거래 식별자 |
| Transaction Code | 운영·분류를 위한 거래코드 |
| Functional Authorization | 기능 실행 가능 여부 |
| Data Authorization | 특정 데이터에 대한 조회·변경 가능 여부 |
| Idempotency | 같은 요청이 반복돼도 중복 결과를 만들지 않는 성질 |
| Affected Rows | INSERT·UPDATE·DELETE의 영향 행 수 |
| Acceptance Criteria | 요구사항 완료 판단기준 |
| Traceability | 요구사항·화면·소스·SQL·테스트 연결관계 |

# 상담예약 전체 목표 구조

text id="rsv33011" \[CT-RSV-0001 화면\] │ │ ServiceId 포함 표준전문 ▼ \[공통 OnlineTransactionController\] │ ▼ \[TCF.process\] │ ▼ \[STF\] 인증·권한·거래통제·Timeout·중복요청 │ ▼ \[CtReservationHandler\] │ ▼ \[CtReservationFacade\] Transaction 경계 │ ▼ \[CtReservationService\] 업무 흐름 ┌──────┼──────────────┐ ▼ ▼ ▼ \[Rule\] \[Customer Client\] \[DAO\] │ ▼ \[Mapper XML\] │ ┌────────────┴────────────┐ ▼ ▼ \[CT\_CONTACT\_RESERVATION\] \[CT\_CONTACT\_RESERVATION\_HISTORY\] │ ▼ \[ETF\] │ ▼ \[StandardResponse\]

# 현재 구현과 목표 설계의 구분

현재 기준 소스에서 CT\_CONTACT\_RESERVATION, CT.Reservation.\* 이름의 상담예약 구현은 확인되지 않았다.

따라서 본 장의 내용은 다음으로 해석한다.

| 구분 | 판단 |
| --- | --- |
| 상담예약 요구사항 | 설계 사례 |
| 화면 ID | 목표 표준 |
| ServiceId | 신규 등록 대상 |
| Handler·Facade·Service | 신규 생성 대상 |
| Master·History | 신규 DB 객체 또는 확정 대상 |
| 오류코드 | 신규 등록 대상 |
| OM Catalog | 신규 등록 대상 |
| 테스트 | 신규 작성 대상 |
| 구현 완료 여부 | 확인되지 않음 |

즉, 이 장의 산출물은 **구현 소스가 아니라 구현을 시작하기 위한 설계 Baseline**이다.

# 문제 정의 및 설계 배경

화면과 테이블만 제공된 신규 CRUD에서 다음 문제가 반복된다.

| 비표준 구현 | 문제 |
| --- | --- |
| 화면별 독자 Controller | 공통 TCF 통제 우회 |
| 하나의 save 거래 | 등록·수정·취소 규칙 혼합 |
| Request DTO를 Mapper에 전달 | 외부계약과 DB계약 결합 |
| 화면의 사용자 ID 사용 | 위변조 가능 |
| 물리 DELETE | 감사·복구 어려움 |
| Version 조건 없음 | Lost Update |
| History 별도 Transaction | Master와 이력 불일치 |
| 목록 무제한 조회 | DB·Heap 장애 |
| 고객 Table 직접 조회 | 도메인 소유권 위반 |
| OM 등록 누락 | 운영에서 거래 미식별 |
| 정상 테스트만 작성 | 권한·동시성·Timeout 미검증 |

목표는 화면과 테이블을 단순 연결하는 것이 아니다.

\`\`\`text id=“rsv33012” 화면 요구

→ 업무 의미

→ 독립 거래

→ 계층 책임

→ 안전한 데이터 변경

→ 운영 가능한 Service



로 전환하는 것이다.

\---

\# 요구사항과 제약조건

\## 기능 요구사항

| 요구사항 ID | 내용 |
|---|---|
| \`REQ-CT-RSV-001\` | 상담예약 목록을 조회한다. |
| \`REQ-CT-RSV-002\` | 상담예약 상세를 조회한다. |
| \`REQ-CT-RSV-003\` | 상담예약을 등록한다. |
| \`REQ-CT-RSV-004\` | 상담예약을 수정한다. |
| \`REQ-CT-RSV-005\` | 상담예약을 취소한다. |
| \`REQ-CT-RSV-006\` | 등록·수정·취소 이력을 저장한다. |
| \`REQ-CT-RSV-007\` | 동시 수정을 통제한다. |
| \`REQ-CT-RSV-008\` | 기능·데이터권한을 확인한다. |
| \`REQ-CT-RSV-009\` | 중복 등록을 방지한다. |
| \`REQ-CT-RSV-010\` | 거래를 로그로 추적한다. |

\## 비기능 요구사항

| ID | 내용 |
|---|---|
| \`NFR-CT-RSV-001\` | 목록조회 p95 3초 이하 |
| \`NFR-CT-RSV-002\` | 상세조회 p95 2초 이하 |
| \`NFR-CT-RSV-003\` | 등록·수정·취소 p95 2~5초 |
| \`NFR-CT-RSV-004\` | 개인정보 상세조회 감사 |
| \`NFR-CT-RSV-005\` | Version 기반 동시성 통제 |
| \`NFR-CT-RSV-006\` | Timeout 후 결과확인 가능 |
| \`NFR-CT-RSV-007\` | 목록 최대 조회기간·건수 제한 |
| \`NFR-CT-RSV-008\` | 한 WAS 장애 시 서비스 지속 |

값은 성능시험과 프로젝트 NFR로 최종 확정한다.

\## 기술 제약조건

\`\`\`text id="rsv33013"
TCF 공통 Endpoint 사용

ServiceId Dispatcher 사용

Spring·MyBatis 사용

Oracle 기준

업무 WAR 배포

JWT AuthenticationContext 사용

Gateway 유무와 관계없이 업무 WAR 2차 인증 적용

Redis 없이도 동작 가능한 구조

고객정보는 고객 도메인이 소유

# 설계 원칙

\`\`\`text id=“rsv33014” 거래 목적별 ServiceId 분리

데이터 소유권 준수

인증문맥의 사용자·지점정보 사용

Request와 DB DTO 분리

Facade가 Transaction 담당

Rule이 상태·업무조건 판단

Version 기반 낙관적 Lock

취소는 상태 변경

Master와 History 원자성

조회기간·건수 제한

등록 중복방지

운영정책을 설계시점에 포함


\---

\# 33.1 화면·요구사항·테이블 분석

\## 33.1.1 개발 시작 전에 확보할 정보

\`\`\`text id="rsv33015"
화면 ID와 화면명

화면 항목

버튼과 이벤트

목록·상세 구조

Table·Column

업무상태

사용자·지점 권한

조회기간과 예상건수

등록·수정·취소 조건

이력·감사 요구

성능·Timeout 요구

외부 고객정보 연계

보존·폐기정책

## 33.1.2 원시 요구사항

\`\`\`text id=“rsv33016” 고객 상담예약을 관리한다.

고객번호와 기간으로 예약을 조회한다.

상담예약일시와 상담목적을 등록한다.

예약일시와 메모를 수정할 수 있다.

완료된 예약은 수정할 수 없다.

예약은 삭제하지 않고 취소한다.

동시에 두 사용자가 수정하지 못하게 한다.

모든 변경이력을 남긴다.



이 문장만으로는 구현할 수 없는 항목이 많다.

\`\`\`text id="rsv33017"
완료 상태는 누가 만든다?

과거 일시 예약이 가능한가?

동일 고객·동일 시간 중복은 허용되는가?

다른 지점 예약은 조회 가능한가?

관리자는 취소된 예약을 복구할 수 있는가?

상담 메모에 개인정보를 입력할 수 있는가?

조회기간 최대치는 얼마인가?

미확정 항목을 개발자가 임의로 결정하지 않는다.

## 33.1.3 결정·가정·미결사항

| 구분 | 내용 |
| --- | --- |
| 결정 | 예약 취소는 논리 취소로 처리한다. |
| 결정 | 수정·취소에 Version 동시성 통제를 적용한다. |
| 결정 | Master와 History는 같은 Transaction이다. |
| 결정 | 인증 사용자·지점정보를 사용한다. |
| 가정 | 조회기간은 최대 3개월이다. |
| 가정 | 한 페이지는 최대 100건이다. |
| 가정 | 예약상태는 READY·COMPLETED·CANCELED다. |
| 미결 | 동일 고객·동일 시간 중복 허용 여부 |
| 미결 | 관리자 타 지점 변경권한 |
| 미결 | 완료 예약의 취소 복구 여부 |
| 미결 | 메모 개인정보 허용범위 |
| 미결 | 고객 확인 연계 장애 시 처리 |

가정은 업무 승인 전 임시 기준이며 구현 완료조건으로 사용하지 않는다.

## 33.1.4 화면 정의

### 화면 기본정보

| 항목 | 값 |
| --- | --- |
| 화면 ID | CT-RSV-0001 |
| 화면명 | 고객 상담예약 관리 |
| 업무코드 | CT |
| 업무세구분 | RSV |
| 화면유형 | 조회·상세·등록·수정 |
| 개인정보 | 고객번호·상담메모 포함 |
| 권한 | 조회·등록·수정·취소 분리 |

### 화면 항목

| UI 객체 ID | 항목명 | 유형 | 필수 | 제약 |
| --- | --- | --- | --- | --- |
| txtCustomerNo | 고객번호 | Text | Y | 형식·길이 검증 |
| dtFromDate | 조회 시작일 | Date | Y | 최대 3개월 |
| dtToDate | 조회 종료일 | Date | Y | 시작일 이상 |
| cboStatus | 예약상태 | Select | N | 상태코드 |
| grdReservation | 예약목록 | Grid | \- | 최대 100건 |
| dtReservationDtm | 예약일시 | DateTime | Y | 정책 검증 |
| cboPurposeCode | 상담목적 | Select | Y | 공통코드 |
| txtMemo | 메모 | Textarea | N | 최대 1,000자 |
| hidReservationId | 예약 ID | Hidden | 조건 | 서버발급 |
| hidVersionNo | Version | Hidden | 수정 시 | 동시성 |
| btnSearch | 조회 | Button | \- | 목록조회 |
| btnNew | 신규 | Button | \- | 화면 초기화 |
| btnSave | 저장 | Button | \- | 등록·수정 분리 |
| btnCancelReservation | 예약취소 | Button | \- | 상태변경 |

## 33.1.5 화면 이벤트

| 이벤트 ID | 화면 동작 | 서버 호출 | ServiceId |
| --- | --- | --- | --- |
| CT-RSV-0001-E01 | 화면 초기조회 | O | CT.Reservation.selectList |
| CT-RSV-0001-E02 | 조회 버튼 | O | CT.Reservation.selectList |
| CT-RSV-0001-E03 | Grid 행 선택 | O | CT.Reservation.selectDetail |
| CT-RSV-0001-E04 | 신규 저장 | O | CT.Reservation.create |
| CT-RSV-0001-E05 | 수정 저장 | O | CT.Reservation.update |
| CT-RSV-0001-E06 | 예약 취소 | O | CT.Reservation.cancel |
| CT-RSV-0001-E07 | 신규 버튼 | X | 화면 상태 초기화 |

모든 버튼이 서버 ServiceId를 가져야 하는 것은 아니다.

화면 이벤트 추적성에는 Controller·Handler·Facade·Service·Rule·DAO·Mapper·SQL·DB 객체·테스트까지 연결해야 한다.

## 33.1.6 화면 상태

\`\`\`text id=“rsv33018” SEARCH 조회모드

NEW 신규입력

DETAIL 상세조회

EDIT 수정모드

READ\_ONLY 완료·취소 예약



화면 상태는 서버의 예약상태와 다르다.

\`\`\`text id="rsv33019"
화면 EDIT
≠ 예약상태 READY

화면 READ\_ONLY
≠ 예약상태 COMPLETED만 의미

권한·업무상태에 따라 화면모드를 계산한다.

## 33.1.7 테이블 분석

### CT\_CONTACT\_RESERVATION

| 컬럼 | 의미 | 책임 |
| --- | --- | --- |
| RESERVATION\_ID | 예약 ID | PK·서버발급 |
| CUSTOMER\_NO | 고객번호 | 업무 참조 |
| RESERVATION\_DTM | 예약일시 | 변경 가능 |
| PURPOSE\_CD | 상담목적 | 코드 검증 |
| MEMO\_CONTENT | 예약 메모 | 개인정보 검토 |
| STATUS\_CD | 예약상태 | 상태전이 |
| BRANCH\_ID | 등록 지점 | 데이터권한 |
| OWNER\_USER\_ID | 담당 사용자 | 데이터권한 |
| VERSION\_NO | Version | 동시성 |
| CREATED\_BY | 생성자 | 서버설정 |
| CREATED\_DTM | 생성일시 | 서버설정 |
| UPDATED\_BY | 변경자 | 서버설정 |
| UPDATED\_DTM | 변경일시 | 서버설정 |

### CT\_CONTACT\_RESERVATION\_HISTORY

| 컬럼 | 의미 |
| --- | --- |
| HISTORY\_ID | 이력 ID |
| RESERVATION\_ID | 예약 ID |
| CHANGE\_TYPE\_CD | CREATE·UPDATE·CANCEL |
| BEFORE\_STATUS\_CD | 변경 전 상태 |
| AFTER\_STATUS\_CD | 변경 후 상태 |
| CHANGE\_CONTENT | 변경 요약 |
| CHANGED\_BY | 변경 사용자 |
| CHANGED\_DTM | 변경일시 |
| TRACE\_ID | 거래 추적 ID |

## 33.1.8 CRUD Matrix

| 기능 | Master | History | 고객 도메인 | 공통코드 |
| --- | --- | --- | --- | --- |
| 목록조회 | R | \- | 조건부 | 상태명 |
| 상세조회 | R | 조건부 R | 조건부 | 목적명 |
| 등록 | C | C | R | R |
| 수정 | U | C | \- | R |
| 취소 | U | C | \- | \- |

## 33.1.9 데이터 소유권

상담예약은 CT 도메인이 소유한다.

\`\`\`text id=“rsv33020” CT 소유

CT\_CONTACT\_RESERVATION

CT\_CONTACT\_RESERVATION\_HISTORY



고객정보는 고객 도메인이 소유한다.

\`\`\`text id="rsv33021"
IC 소유

고객 기본정보

고객 상태

고객 유효성

금지:

text id="rsv33022" CT Mapper에서 IC 고객 Table 직접 조회

기본 권장:

\`\`\`text id=“rsv33023” CT Service

→ IC Customer Client

→ 고객 존재·상태 확인



목록 성능 때문에 고객명 표시가 필요하면 다음 대안을 검토한다.

| 대안 | 장점 | 위험 |
|---|---|---|
| IC ServiceId 호출 | 소유권 명확 | 목록 N+1 위험 |
| CT Read Model | 조회성능 | 동기화 필요 |
| 화면 별도 조회 | 구현 단순 | UX·호출 증가 |
| Data Platform View | 분석조회 적합 | 실시간성 확인 |

목록 행마다 고객 Service를 호출하는 구조는 금지한다.

\---

\## 33.1.10 조회조건

필수:

\`\`\`text id="rsv33024"
고객번호 또는 지점범위

조회 시작일

조회 종료일

Page Size

Page Cursor·Page No

검증:

\`\`\`text id=“rsv33025” 시작일 <= 종료일

조회기간 <= 3개월

Page Size <= 100

상태코드는 허용값

정렬컬럼은 Allow List


\---

\## 33.1.11 목록 응답항목

권장:

\`\`\`text id="rsv33026"
예약 ID

고객번호 마스킹

예약일시

상담목적코드·명

상태코드·명

담당 사용자 표시명

지점

Version

수정 가능 여부

목록에 포함하지 않을 항목:

\`\`\`text id=“rsv33027” 상담메모 전체

History 전체

개인정보 원문

대형 첨부정보



상세 데이터는 상세조회에서 반환한다.

\---

\## 33.1.12 예상 데이터량

설계 시 최소한 다음을 산정한다.

\`\`\`text id="rsv33028"
일 신규 예약 건수

연간 예약 건수

고객별 평균 예약 수

History 증가 배수

취소율

완료율

목록 Peak TPS

보존기간

예:

\`\`\`text id=“rsv33029” 일 신규 100,000건

연간 약 3,650만 건

변경이력 평균 예약당 2건

History 약 7,300만 건



실제 예상량이 이 수준이라면 Partition·Archive·Index·조회기간 제한을 반드시 검토해야 한다.

\---

\## 33.1.13 주요 Index 후보

\`\`\`text id="rsv33030"
PK
RESERVATION\_ID

목록
CUSTOMER\_NO,
RESERVATION\_DTM

지점 조회
BRANCH\_ID,
RESERVATION\_DTM

상태 조회
STATUS\_CD,
RESERVATION\_DTM

History
RESERVATION\_ID,
CHANGED\_DTM

Index는 조회패턴과 데이터분포·DML 비용을 성능시험으로 검증한다.

## 33.1.14 미결사항 관리

| 확인 ID | 질문 | Owner | 완료조건 |
| --- | --- | --- | --- |
| Q-CT-RSV-001 | 중복 예약 기준은 무엇인가 | 업무 | Business Key 확정 |
| Q-CT-RSV-002 | 타 지점 조회 가능 범위 | 보안·업무 | 권한정책 |
| Q-CT-RSV-003 | 완료 처리 주체 | 업무 | ServiceId 확정 |
| Q-CT-RSV-004 | 메모 개인정보 허용 | 보안 | 입력·마스킹 |
| Q-CT-RSV-005 | 고객 연계 장애 정책 | AA·업무 | Fail 정책 |
| Q-CT-RSV-006 | 취소 복구 기능 필요 | 업무 | 별도 거래 여부 |

미결사항이 해소되지 않으면 관련 구현을 확정하지 않는다.

# 33.2 Aggregate와 상태 전이

## 33.2.1 Aggregate 정의

상담예약 Aggregate:

\`\`\`text id=“rsv33031” 상담예약 Master

-   상담예약 변경이력
-   예약상태
-   Version
-   데이터권한 정보



Aggregate Root:

\`\`\`text id="rsv33032"
ConsultationReservation

다음 변경은 Aggregate Root를 통해 수행한다.

\`\`\`text id=“rsv33033” 예약 생성

예약내용 변경

예약 취소

완료 처리


\---

\## 33.2.2 불변조건

\`\`\`text id="rsv33034"
예약 ID는 생성 후 변경할 수 없다.

고객번호는 생성 후 일반 수정으로 바꿀 수 없다.

등록 지점은 생성 후 변경할 수 없다.

READY 상태만 일반 수정할 수 있다.

READY 상태만 취소할 수 있다.

Version이 일치해야 수정·취소할 수 있다.

모든 변경은 History를 남겨야 한다.

변경자는 인증문맥에서 결정한다.

## 33.2.3 예약상태

| 상태 | 의미 |
| --- | --- |
| READY | 상담예약 대기 |
| COMPLETED | 상담 완료 |
| CANCELED | 예약 취소 |

향후 확장 후보:

\`\`\`text id=“rsv33035” IN\_PROGRESS

NO\_SHOW

EXPIRED



상태를 추가할 때 화면·통계·Batch·권한·상태전이·테스트에 미치는 영향을 함께 검토한다.

\---

\## 33.2.4 상태전이

\`\`\`text id="rsv33036"
┌───────────────┐
│ READY │
└───────┬───────┘
│
┌─────────┴─────────┐
▼ ▼
┌──────────────┐ ┌──────────────┐
│ COMPLETED │ │ CANCELED │
└──────────────┘ └──────────────┘

허용:

| 현재 | 동작 | 다음 |
| --- | --- | --- |
| 없음 | 등록 | READY |
| READY | 수정 | READY |
| READY | 완료 | COMPLETED |
| READY | 취소 | CANCELED |

금지:

| 현재 | 요청 |
| --- | --- |
| COMPLETED | 일반 수정 |
| COMPLETED | 취소 |
| CANCELED | 수정 |
| CANCELED | 재취소 |
| CANCELED | READY 복원 |

복구가 필요하면 관리자 승인형 별도 ServiceId를 설계한다.

## 33.2.5 등록 규칙

화면이 입력할 수 있는 값:

\`\`\`text id=“rsv33037” customerNo

reservationDtm

purposeCode

memo



서버가 결정하는 값:

\`\`\`text id="rsv33038"
reservationId

statusCode=READY

branchId

ownerUserId

versionNo=1

createdBy

createdDtm

updatedBy

updatedDtm

금지:

\`\`\`text id=“rsv33039” request.createdBy

request.branchId

request.ownerUserId

request.statusCode


\---

\## 33.2.6 등록 업무흐름

\`\`\`text id="rsv33040"
입력 검증

→ 인증·권한 확인

→ 고객 존재·상태 확인

→ 상담목적 코드 확인

→ 중복 예약 확인

→ Master INSERT

→ History CREATE INSERT

→ Commit

→ 예약 ID·상태·Version 반환

## 33.2.7 중복 예약 기준

Business Key 후보:

\`\`\`text id=“rsv33041” customerNo

-   reservationDtm
-   activeStatus



또는:

\`\`\`text id="rsv33042"
customerNo

\+ reservationDate

\+ timeSlotCode

\+ branchId

업무정책이 확정돼야 한다.

중복 방지는 두 단계로 구성한다.

\`\`\`text id=“rsv33043” 사전 중복조회

-   DB Unique Constraint



사전조회만으로는 동시 요청을 완전히 막지 못한다.

\---

\## 33.2.8 등록 Idempotency

등록은 같은 요청의 재전송으로 중복 생성될 위험이 크다.

\`\`\`text id="rsv33044"
Idempotency-Key
CT-RSV-20260718-000001

상태:

\`\`\`text id=“rsv33045” RECEIVED

PROCESSING

SUCCESS

FAIL

TIMEOUT

UNKNOWN



동일 Key로 재요청되면 기존 결과를 반환하거나 처리 중 상태를 반환한다.

\---

\## 33.2.9 수정 가능 필드

허용:

\`\`\`text id="rsv33046"
reservationDtm

purposeCode

memo

일반 수정에서 금지:

\`\`\`text id=“rsv33047” reservationId

customerNo

branchId

ownerUserId

statusCode

createdBy

createdDtm



상태변경은 별도 거래로 처리한다.

\---

\## 33.2.10 Version 기반 수정

\`\`\`sql id="rsv33048"
UPDATE CT\_CONTACT\_RESERVATION
SET RESERVATION\_DTM = :reservationDtm,
PURPOSE\_CD = :purposeCode,
MEMO\_CONTENT = :memo,
VERSION\_NO = VERSION\_NO + 1,
UPDATED\_BY = :updatedBy,
UPDATED\_DTM = SYSTIMESTAMP
WHERE RESERVATION\_ID = :reservationId
AND STATUS\_CD = 'READY'
AND VERSION\_NO = :versionNo
AND (
OWNER\_USER\_ID = :userId
OR BRANCH\_ID = :branchId
);

영향 행:

\`\`\`text id=“rsv33049” 1건 → 수정 성공

0건 → 원인 구분 필요

2건 이상 → 데이터·SQL 결함


\---

\## 33.2.11 UPDATE 0건 구분

0건의 원인 후보:

\`\`\`text id="rsv33050"
예약이 존재하지 않음

READY가 아님

Version 충돌

변경권한 없음

정확한 오류를 반환하려면 사전 조회한 현재 상태와 UPDATE 결과를 함께 판단해야 한다.

단, 사전조회와 UPDATE 사이에도 상태가 바뀔 수 있으므로 최종 정합성은 UPDATE 조건과 영향 행 수가 보장한다.

## 33.2.12 낙관적 Lock 충돌

\`\`\`text id=“rsv33051” 사용자 A Version 3 조회

사용자 B Version 3 조회

사용자 A Version 3 조건 수정 → 성공 → Version 4

사용자 B Version 3 조건 수정 → 영향 행 0 → 동시성 오류



화면 처리:

\`\`\`text id="rsv33052"
“다른 사용자가 먼저 변경했습니다.
최신 내용을 다시 조회해 주세요.”

서버가 B의 값을 강제로 덮어쓰지 않는다.

## 33.2.13 취소

취소는 물리 삭제가 아니라 상태변경이다.

sql id="rsv33053" UPDATE CT\_CONTACT\_RESERVATION SET STATUS\_CD = 'CANCELED', VERSION\_NO = VERSION\_NO + 1, UPDATED\_BY = :updatedBy, UPDATED\_DTM = SYSTIMESTAMP WHERE RESERVATION\_ID = :reservationId AND STATUS\_CD = 'READY' AND VERSION\_NO = :versionNo AND ( OWNER\_USER\_ID = :userId OR BRANCH\_ID = :branchId );

## 33.2.14 논리 취소의 영향

다음을 함께 설계한다.

\`\`\`text id=“rsv33054” 기본 목록에서 취소건 제외 여부

상태조건을 포함한 Unique 정책

History 보존

통계 포함 여부

취소건 재등록 허용

보관기간

물리 폐기 Batch

관리자 복구 여부



논리 삭제는 \`STATUS\_CD\`만 추가하면 끝나는 기능이 아니다.

\---

\## 33.2.15 History 원자성

등록:

\`\`\`text id="rsv33055"
Master INSERT

\+ History CREATE INSERT

→ 같은 Transaction

수정:

\`\`\`text id=“rsv33056” Master UPDATE

-   History UPDATE INSERT

→ 같은 Transaction



취소:

\`\`\`text id="rsv33057"
Master 상태변경

\+ History CANCEL INSERT

→ 같은 Transaction

History 저장 실패 시 Master 변경도 Rollback한다.

## 33.2.16 History 변경내용

전체 객체 JSON을 무조건 저장하지 않는다.

권장:

\`\`\`text id=“rsv33058” 변경 필드

변경 전 값 마스킹

변경 후 값 마스킹

변경사유

사용자

TraceId



메모에 개인정보가 포함될 수 있으므로 전체 원문 History 저장 여부는 보안정책을 확인한다.

\---

\## 33.2.17 기능권한

| 기능 | 권한코드 |
|---|---|
| 목록·상세 조회 | \`CT\_RESERVATION\_VIEW\` |
| 등록 | \`CT\_RESERVATION\_CREATE\` |
| 수정 | \`CT\_RESERVATION\_UPDATE\` |
| 취소 | \`CT\_RESERVATION\_CANCEL\` |
| 관리자 복구 | \`CT\_RESERVATION\_RESTORE\` |

\---

\## 33.2.18 데이터권한

기본 예:

\`\`\`text id="rsv33059"
조회
같은 지점 예약

수정
담당 사용자 또는 지점 관리자

취소
담당 사용자 또는 취소권한 관리자

권한판정 입력:

\`\`\`text id=“rsv33060” AuthenticationContext.userId

AuthenticationContext.branchId

AuthenticationContext.roles

예약 ownerUserId

예약 branchId

예약 status



화면이 보낸 지점·사용자 값으로 판단하지 않는다.

\---

\## 33.2.19 고객 존재 확인

등록 전 고객 유효성을 확인한다.

\`\`\`text id="rsv33061"
CT Reservation Service

→ IC Customer Client

→ 고객 존재

→ 사용 가능 상태

연계 실패 시:

\`\`\`text id=“rsv33062” Master INSERT 전 중단

DB 변경 없음

외부연계 오류 반환



고객 확인 실패를 고객 없음으로 잘못 처리하지 않는다.

\---

\## 33.2.20 Transaction 경계

\### 목록·상세

\`\`\`text id="rsv33063"
readOnly Transaction

### 등록·수정·취소

\`\`\`text id=“rsv33064” Facade Method 전체

Master

History

Idempotency 상태

감사 연계정책



외부 고객 조회를 DB Transaction 안에서 수행하면 Connection을 오래 점유할 수 있다.

권장 흐름:

\`\`\`text id="rsv33065"
외부 고객 확인

→ 짧은 DB 변경 Transaction

단, 외부 확인과 DB 변경 사이의 업무정합성을 어떻게 유지할지 정책을 정의한다.

## 33.2.21 Timeout 후 상태

조회 Timeout:

text id="rsv33066" 재조회 가능

등록·수정·취소 Timeout:

text id="rsv33067" 응답은 실패처럼 보이지만 DB Commit 여부가 불명확할 수 있음

따라서 변경거래는 다음을 제공한다.

\`\`\`text id=“rsv33068” Idempotency Key

예약 ID

거래 GUID

결과조회 기능

History·Master 대사


\---

\## 33.2.22 정상 등록 흐름

\`\`\`text id="rsv33069"
1\. STF가 인증·권한·Timeout을 검증한다.
2\. Handler가 create 거래를 Facade에 전달한다.
3\. Service가 Request를 Command로 변환한다.
4\. Rule이 입력과 예약일시를 검증한다.
5\. Customer Client가 고객을 확인한다.
6\. DAO가 중복 예약을 조회한다.
7\. Facade Transaction을 시작한다.
8\. Master를 INSERT한다.
9\. History를 INSERT한다.
10\. Transaction을 Commit한다.
11\. ETF가 표준 성공응답을 반환한다.

## 33.2.23 오류·장애 흐름

### 고객 연계 Timeout

\`\`\`text id=“rsv33070” 고객 확인 실패

→ Master INSERT 전 중단

→ DB 변경 없음


\### History 실패

\`\`\`text id="rsv33071"
Master 변경

→ History 실패

→ 전체 Rollback

### 동시성 충돌

\`\`\`text id=“rsv33072” Version 불일치

→ UPDATE 0건

→ E-CT-RSV-0006

→ 화면 재조회


\### DB Connection 실패

\`\`\`text id="rsv33073"
Connection 획득 실패

→ 시스템 오류

→ Pool Alert

→ DB 변경 없음

### 변경 Timeout

\`\`\`text id=“rsv33074” TIMEOUT 응답

→ Idempotency·GUID로 결과 확인

→ 중복 재실행 금지


\---

\# 33.3 ServiceId·오류코드·DTO 설계

\## 33.3.1 ServiceId 분리

| 기능 | ServiceId |
|---|---|
| 목록조회 | \`CT.Reservation.selectList\` |
| 상세조회 | \`CT.Reservation.selectDetail\` |
| 등록 | \`CT.Reservation.create\` |
| 수정 | \`CT.Reservation.update\` |
| 취소 | \`CT.Reservation.cancel\` |

금지:

\`\`\`text id="rsv33075"
CT.Reservation.save

actionType =
CREATE·UPDATE·CANCEL

이 방식은 권한·Timeout·감사·오류·테스트를 하나의 거래 안에 혼합한다.

ServiceId는 화면·Handler·운영 Catalog·로그가 같은 값을 사용해야 한다.

## 33.3.2 거래코드

| ServiceId | 거래코드 | 유형 |
| --- | --- | --- |
| CT.Reservation.selectList | CT-INQ-0101 | 조회 |
| CT.Reservation.selectDetail | CT-INQ-0102 | 조회 |
| CT.Reservation.create | CT-REG-0101 | 등록 |
| CT.Reservation.update | CT-UPD-0101 | 수정 |
| CT.Reservation.cancel | CT-UPD-0102 | 상태변경 |

## 33.3.3 Timeout

| ServiceId | 초기 기준 | 비고 |
| --- | --- | --- |
| 목록조회 | 3,000ms | Paging·기간 제한 |
| 상세조회 | 2,000ms | 단건 |
| 등록 | 5,000ms | 고객연계 포함 |
| 수정 | 5,000ms | 동시성·History |
| 취소 | 5,000ms | 상태변경·History |

최종값은 성능시험으로 확정한다.

하위 SQL·연계 Timeout은 전체 TCF Timeout보다 짧아야 한다.

## 33.3.4 감사정책

| ServiceId | 감사 |
| --- | --- |
| 목록조회 | 일반 거래로그 |
| 상세조회 | 개인정보 조회감사 |
| 등록 | 변경감사 |
| 수정 | 변경감사 |
| 취소 | 중요 변경감사 |

목록에서 고객 개인정보 원문을 대량 조회하면 별도 대량조회 감사를 검토한다.

## 33.3.5 Idempotency

| 기능 | 정책 |
| --- | --- |
| 목록조회 | 불필요 |
| 상세조회 | 불필요 |
| 등록 | 필수 권고 |
| 수정 | Version + 필요 시 Idempotency |
| 취소 | Version·상태 + 필수 권고 |

## 33.3.6 Request DTO

### 목록조회

java id="rsv33076" public record ReservationListRequest( String customerNo, LocalDate fromDate, LocalDate toDate, String statusCode, Integer pageSize, String lastReservationId ) {}

### 상세조회

java id="rsv33077" public record ReservationDetailRequest( String reservationId ) {}

### 등록

java id="rsv33078" public record ReservationCreateRequest( String customerNo, LocalDateTime reservationDtm, String purposeCode, String memo ) {}

### 수정

java id="rsv33079" public record ReservationUpdateRequest( String reservationId, LocalDateTime reservationDtm, String purposeCode, String memo, long versionNo ) {}

### 취소

java id="rsv33080" public record ReservationCancelRequest( String reservationId, long versionNo, String cancelReason ) {}

Request에 포함하지 않을 값:

\`\`\`text id=“rsv33081” userId

branchId

createdBy

updatedBy

statusCode

serverTime


\---

\## 33.3.7 내부 Query DTO

\`\`\`java id="rsv33082"
public record ReservationListQuery(
String customerNo,
LocalDateTime fromDtm,
LocalDateTime toExclusiveDtm,
String statusCode,
String permittedBranchId,
int pageSize,
String lastReservationId
) {}

Request의 화면값을 그대로 Mapper에 전달하지 않는다.

## 33.3.8 내부 Command DTO

java id="rsv33083" public record ReservationCreateCommand( String reservationId, String customerNo, LocalDateTime reservationDtm, String purposeCode, String memo, String branchId, String ownerUserId, String actorUserId, String traceId ) {}

java id="rsv33084" public record ReservationUpdateCommand( String reservationId, LocalDateTime reservationDtm, String purposeCode, String memo, long versionNo, String actorUserId, String actorBranchId, String traceId ) {}

## 33.3.9 Data DTO

java id="rsv33085" public record ReservationData( String reservationId, String customerNo, LocalDateTime reservationDtm, String purposeCode, String memo, String statusCode, String branchId, String ownerUserId, long versionNo, String createdBy, LocalDateTime createdDtm, String updatedBy, LocalDateTime updatedDtm ) {}

DB DTO는 외부 Response로 직접 반환하지 않는다.

## 33.3.10 Response DTO

### 목록 Row

java id="rsv33086" public record ReservationListItem( String reservationId, String maskedCustomerNo, LocalDateTime reservationDtm, String purposeCode, String purposeName, String statusCode, String statusName, String ownerDisplayName, long versionNo, boolean editable, boolean cancellable ) {}

### 상세

java id="rsv33087" public record ReservationDetailResponse( String reservationId, String maskedCustomerNo, LocalDateTime reservationDtm, String purposeCode, String purposeName, String memo, String statusCode, String statusName, String branchId, String ownerDisplayName, long versionNo, boolean editable, boolean cancellable ) {}

### 변경결과

java id="rsv33088" public record ReservationChangeResponse( String reservationId, String statusCode, long versionNo, LocalDateTime updatedDtm ) {}

## 33.3.11 DTO Validation 책임

| 검증 | 위치 |
| --- | --- |
| Null·길이·형식 | Request Validation |
| 날짜 선후관계 | Rule |
| 조회기간 3개월 | Rule |
| 상담목적 코드 | Rule·Code Service |
| 고객 존재 | Customer Client |
| 권한 | Authentication·Rule |
| 현재 상태 | Rule·DB |
| Version | UPDATE 조건 |
| 중복 예약 | Service·DB Constraint |
| 영향 행 수 | Service·DAO 결과 |

## 33.3.12 오류코드

| 오류코드 | 의미 | HTTP·결과 분류 |
| --- | --- | --- |
| E-CT-RSV-0001 | 예약을 찾을 수 없음 | 업무 오류 |
| E-CT-RSV-0002 | 조회기간 초과 | Validation |
| E-CT-RSV-0003 | 유효하지 않은 상담목적 | 업무 오류 |
| E-CT-RSV-0004 | 변경할 수 없는 예약상태 | 업무 오류 |
| E-CT-RSV-0005 | 예약 변경권한 없음 | 권한 오류 |
| E-CT-RSV-0006 | 다른 사용자가 먼저 변경함 | 동시성 |
| E-CT-RSV-0007 | 고객을 찾을 수 없음 | 업무 오류 |
| E-CT-RSV-0008 | 중복 예약 | 업무 오류 |
| E-CT-RSV-0009 | 페이지 크기 초과 | Validation |
| E-CT-RSV-0010 | 예약일시가 허용범위가 아님 | 업무 오류 |
| E-CT-RSV-0011 | 예약 취소사유가 필요함 | Validation |
| E-CT-RSV-0012 | 변경결과를 확인할 수 없음 | UNKNOWN |
| E-CT-RSV-9001 | 고객 시스템 연계 오류 | 시스템·연계 |
| E-CT-RSV-9002 | 예약 DB 처리 오류 | 시스템 |
| E-CT-RSV-9003 | 예약 처리 Timeout | Timeout |

## 33.3.13 오류 판단 우선순위

수정 요청에서 다음 순서로 판단한다.

\`\`\`text id=“rsv33089” 예약 존재 여부

→ 데이터권한

→ 현재 상태

→ Version

→ 입력 업무규칙

→ UPDATE 영향 행



다만 최종 동시성 통제는 UPDATE 조건이 담당한다.

\---

\## 33.3.14 패키지 구조

\`\`\`text id="rsv33090"
ct-service
└─ com.nh.nsight.ct.reservation
├─ handler
│ └─ CtReservationHandler
├─ facade
│ └─ CtReservationFacade
├─ service
│ └─ CtReservationService
├─ rule
│ └─ CtReservationRule
├─ dao
│ └─ CtReservationDao
├─ mapper
│ └─ CtReservationMapper
├─ client
│ └─ CtCustomerClient
├─ dto
│ ├─ request
│ ├─ response
│ ├─ command
│ ├─ query
│ └─ data
├─ model
│ └─ ReservationStatus
└─ exception
└─ CtReservationException

실제 프로젝트 패키지 Root와 CT 업무 WAR 존재 여부는 프로젝트 기준으로 확정한다.

## 33.3.15 구성요소 책임

| 구성요소 | 책임 | 금지 |
| --- | --- | --- |
| Handler | ServiceId 분기·DTO 변환 시작 | SQL·Transaction |
| Facade | 유스케이스·Transaction | HTTP 응답 생성 |
| Service | 업무 순서·DAO·Client 조립 | 인증 Token 파싱 |
| Rule | 상태·권한·입력 판단 | DB 직접 변경 |
| DAO | Mapper 호출·영향 행 반환 | 업무 메시지 판단 |
| Mapper | SQL 계약 | 상태 프로세스 결정 |
| Client | 고객 도메인 계약 호출 | 고객 Table 직접 조회 |
| ETF | 표준 결과·거래 종료 | 업무규칙 판단 |

## 33.3.16 Handler 설계

Handler는 다음 ServiceId를 명시적으로 분기한다.

\`\`\`text id=“rsv33091” CT.Reservation.selectList

CT.Reservation.selectDetail

CT.Reservation.create

CT.Reservation.update

CT.Reservation.cancel



알 수 없는 ServiceId는 기본 성공이나 임의 처리로 넘어가지 않는다.

\---

\## 33.3.17 Facade 설계

| Method | Transaction |
|---|---|
| \`selectList\` | \`readOnly=true\` |
| \`selectDetail\` | \`readOnly=true\` |
| \`create\` | 일반 Transaction |
| \`update\` | 일반 Transaction |
| \`cancel\` | 일반 Transaction |

Master·History의 원자성은 Facade Transaction으로 보장한다.

\---

\## 33.3.18 Service 설계

\### 목록조회

\`\`\`text id="rsv33092"
조회조건 변환

→ 기간·Page 검증

→ 데이터권한 Scope 구성

→ DAO 목록조회

→ 코드명·수정가능 여부 변환

→ Response

### 등록

\`\`\`text id=“rsv33093” 입력 Rule

→ 고객 확인

→ 중복 확인

→ ID 생성

→ Master 저장

→ History 저장

→ Response


\### 수정

\`\`\`text id="rsv33094"
현재 예약 조회

→ 권한·상태 Rule

→ 코드 검증

→ Version 조건 UPDATE

→ 영향 행 검사

→ History 저장

→ Response

### 취소

\`\`\`text id=“rsv33095” 현재 예약 조회

→ 권한·상태 Rule

→ Version 조건 상태변경

→ 영향 행 검사

→ History 저장

→ Response


\---

\## 33.3.19 Rule 설계

| Rule ID | 내용 |
|---|---|
| \`BR-CT-RSV-001\` | 조회기간은 최대 3개월 |
| \`BR-CT-RSV-002\` | Page Size 최대 100 |
| \`BR-CT-RSV-003\` | 예약일시 필수 |
| \`BR-CT-RSV-004\` | 상담목적 코드 유효 |
| \`BR-CT-RSV-005\` | READY만 수정 가능 |
| \`BR-CT-RSV-006\` | READY만 취소 가능 |
| \`BR-CT-RSV-007\` | Version 일치 |
| \`BR-CT-RSV-008\` | 담당자·동일 지점 변경 |
| \`BR-CT-RSV-009\` | 모든 변경 History 저장 |
| \`BR-CT-RSV-010\` | 고객 존재·유효 |
| \`BR-CT-RSV-011\` | 중복 예약 금지 |
| \`BR-CT-RSV-012\` | 메모 최대 1,000자 |
| \`BR-CT-RSV-013\` | 과거 예약 허용정책 |
| \`BR-CT-RSV-014\` | 취소사유 필수 |

\---

\## 33.3.20 Mapper SQL ID

| SQL ID | 유형 |
|---|---|
| \`selectReservationList\` | 목록조회 |
| \`selectReservationDetail\` | 상세조회 |
| \`selectReservationForUpdate\` | 변경 전 현재값 |
| \`countDuplicateReservation\` | 중복확인 |
| \`insertReservation\` | Master 등록 |
| \`updateReservation\` | 예약 수정 |
| \`cancelReservation\` | 예약 취소 |
| \`insertReservationHistory\` | History 등록 |

관리대장용 SQL 식별자 예:

\`\`\`text id="rsv33096"
CT-RSV-SEL-001

CT-RSV-SEL-002

CT-RSV-SEL-003

CT-RSV-INS-001

CT-RSV-UPD-001

CT-RSV-UPD-002

CT-RSV-INS-002

## 33.3.21 OM Service Catalog

| 항목 | 예 |
| --- | --- |
| 업무코드 | CT |
| ServiceId | CT.Reservation.create |
| 거래코드 | CT-REG-0101 |
| 서비스명 | 상담예약 등록 |
| 사용여부 | Y |
| Timeout | 5,000ms |
| 권한코드 | CT\_RESERVATION\_CREATE |
| 감사유형 | 변경감사 |
| Idempotency | Y |
| 담당팀 | CT 업무팀 |
| 배포 WAR | ct-service |
| Health | 업무 WAR Health |
| Runbook | 상담예약 장애 대응 |

ServiceId 이름을 소스에만 추가하고 OM Catalog를 누락하면 운영 준비가 완료된 것이 아니다.

## 33.3.22 로그·Metric

거래로그:

\`\`\`text id=“rsv33097” guid

traceId

serviceId

transactionCode

reservationId

resultType

errorCode

elapsedMs

instanceId

artifactVersion



민감정보:

\`\`\`text id="rsv33098"
customerNo
→ 마스킹·Hash

memo
→ 기록 금지

Metric:

\`\`\`text id=“rsv33099” ct.reservation.transaction.count

ct.reservation.transaction.duration

ct.reservation.error.count

ct.reservation.concurrent.conflict.count

ct.reservation.duplicate.count

ct.reservation.timeout.count



Metric Label에 예약 ID·고객번호·GUID를 넣지 않는다.

\---

\# 33.4 테스트와 추적성 계획

\## 33.4.1 테스트 원칙

테스트는 구현 클래스의 내부 호출순서보다 외부 계약과 업무규칙을 검증한다.

\`\`\`text id="rsv33100"
어떤 입력을 주었는가?

어떤 상태에서 실행했는가?

어떤 권한으로 요청했는가?

어떤 결과와 오류코드를 받았는가?

DB Master와 History가 어떻게 변했는가?

로그와 거래 ID로 추적 가능한가?

원본 제33장도 정상 사례뿐 아니라 경계값·권한·동시성·의존 시스템 실패를 테스트하고 요구사항 ID와 테스트를 연결하도록 요구한다.

## 33.4.2 테스트 계층

| 계층 | 검증 대상 |
| --- | --- |
| Rule Unit | 날짜·상태·권한·코드 |
| Service Unit | 처리순서·Client·DAO |
| Mapper Test | SQL·영향 행·Paging |
| Transaction Test | Master·History Rollback |
| Integration | TCF→Handler→DB |
| Security | 인증·기능·데이터권한 |
| Concurrency | Version 충돌 |
| Idempotency | 중복 등록 |
| Timeout | UNKNOWN·결과조회 |
| Performance | p95·Paging·Index |
| E2E | 화면 이벤트부터 DB |
| Operations | OM·로그·Metric·Runbook |

## 33.4.3 요구사항 추적성 Matrix

| 요구사항 | 화면 이벤트 | ServiceId | Rule·프로그램 | DB | 테스트 |
| --- | --- | --- | --- | --- | --- |
| REQ-001 목록 | E01·E02 | selectList | List Rule·DAO | Master R | RSV-001~004 |
| REQ-002 상세 | E03 | selectDetail | Detail Service | Master R | RSV-005~006 |
| REQ-003 등록 | E04 | create | Create Service | Master C·History C | RSV-007~010 |
| REQ-004 수정 | E05 | update | Update Rule | Master U·History C | RSV-011~014 |
| REQ-005 취소 | E06 | cancel | Cancel Rule | Master U·History C | RSV-015~017 |
| REQ-006 이력 | E04~E06 | 변경 거래 | Facade TX | History C | RSV-010 |
| REQ-007 동시성 | E05·E06 | update·cancel | Version | Master U | RSV-014 |
| REQ-008 권한 | 전체 | 전체 | STF·Rule | Scope | RSV-021~023 |
| REQ-009 중복 | E04 | create | Idempotency·Unique | Master | RSV-009 |
| REQ-010 추적 | 전체 | 전체 | TCF Log | Log DB | RSV-026 |

## 33.4.4 정상 테스트

| ID | 시나리오 | 기대 결과 |
| --- | --- | --- |
| RSV-001 | 정상 목록조회 | Paging 목록 |
| RSV-002 | 결과 없음 | 빈 목록 |
| RSV-003 | 정상 상세조회 | 상세 반환 |
| RSV-004 | 정상 등록 | Master·History 각각 1건 |
| RSV-005 | 정상 수정 | Version 증가·History |
| RSV-006 | 정상 취소 | CANCELED·History |

## 33.4.5 Validation·경계 테스트

| ID | 시나리오 | 기대 |
| --- | --- | --- |
| RSV-007 | 조회기간 3개월 초과 | E-CT-RSV-0002 |
| RSV-008 | 시작일이 종료일보다 큼 | Validation 오류 |
| RSV-009 | Page Size 101 | E-CT-RSV-0009 |
| RSV-010 | 고객번호 형식 오류 | Validation 오류 |
| RSV-011 | 메모 1,001자 | Validation 오류 |
| RSV-012 | 상담목적 코드 오류 | E-CT-RSV-0003 |
| RSV-013 | 예약일시 경계 | 정책에 따른 결과 |
| RSV-014 | 목록 마지막 Page | 중복·누락 없음 |

## 33.4.6 상태 테스트

| ID | 현재 상태 | 동작 | 기대 |
| --- | --- | --- | --- |
| RSV-015 | READY | 수정 | 성공 |
| RSV-016 | COMPLETED | 수정 | E-CT-RSV-0004 |
| RSV-017 | CANCELED | 수정 | E-CT-RSV-0004 |
| RSV-018 | READY | 취소 | 성공 |
| RSV-019 | COMPLETED | 취소 | 차단 |
| RSV-020 | CANCELED | 재취소 | 차단·멱등정책 |

## 33.4.7 권한 테스트

| ID | 사용자 | 데이터 | 기대 |
| --- | --- | --- | --- |
| RSV-021 | 담당자 | 본인 예약 | 수정 성공 |
| RSV-022 | 같은 지점 관리자 | 지점 예약 | 정책상 성공 |
| RSV-023 | 타 지점 일반 사용자 | 다른 지점 예약 | 권한 오류 |
| RSV-024 | 기능권한 없음 | 본인 예약 | 403 |
| RSV-025 | JWT 없음 | 조회 | 401 |
| RSV-026 | Request userId 위조 | 타인 ID | 무시·인증값 사용 |

## 33.4.8 동시성 테스트

text id="rsv33101" 1. 동일 예약 Version 3을 두 사용자가 조회한다. 2. 사용자 A가 수정한다. 3. Version이 4로 증가한다. 4. 사용자 B가 Version 3으로 수정한다. 5. UPDATE 영향 행은 0이다. 6. E-CT-RSV-0006을 반환한다. 7. A의 데이터가 유지된다. 8. B의 History는 생성되지 않는다.

## 33.4.9 Transaction 테스트

### History 실패

\`\`\`text id=“rsv33102” Master INSERT 성공

History INSERT 강제 실패

기대 Master 0건 History 0건


\### 수정 History 실패

\`\`\`text id="rsv33103"
Master UPDATE

History 실패

기대
Master 원상복구
Version 증가 없음

## 33.4.10 Idempotency 테스트

\`\`\`text id=“rsv33104” 동일 Idempotency Key로 등록 요청 2회

기대 예약 1건

History 1건

동일 예약 ID 반환



동시 요청으로 실행해 Race Condition도 검증한다.

\---

\## 33.4.11 고객 연계 테스트

| ID | 상황 | 기대 |
|---|---|---|
| \`RSV-027\` | 고객 정상 | 등록 진행 |
| \`RSV-028\` | 고객 없음 | E-CT-RSV-0007 |
| \`RSV-029\` | 고객 사용불가 | 업무 오류 |
| \`RSV-030\` | 고객 연계 Timeout | DB 변경 없음 |
| \`RSV-031\` | 고객 연계 시스템 오류 | E-CT-RSV-9001 |

\---

\## 33.4.12 Timeout 테스트

\### 조회

\`\`\`text id="rsv33105"
SQL 지연

→ Query Timeout

→ 표준 오류

→ DB 변경 없음

### 등록

\`\`\`text id=“rsv33106” Commit 직전·직후 Timeout 조건

→ Idempotency 결과조회

→ 중복 예약 없음

→ UNKNOWN 상태 정책 검증


\---

\## 33.4.13 Mapper 테스트

\`\`\`text id="rsv33107"
목록 기간조건

상태조건

데이터권한 조건

Paging

정렬 안정성

상세 0·1건

Version UPDATE 0·1건

History INSERT

중복 Unique

## 33.4.14 성능 테스트

| 항목 | 기준 |
| --- | --- |
| 목록 데이터 | 운영 유사 규모 |
| 조회기간 | 최대 3개월 |
| Page Size | 100 |
| 목록 p95 | 3초 이하 |
| 상세 p95 | 2초 이하 |
| 변경 p95 | 승인 기준 |
| SQL Timeout | TCF보다 짧게 |
| 동시 수정 | 정합성 유지 |
| DB Pool | Pending 지속 없음 |

## 33.4.15 로그 추적 테스트

하나의 등록 거래를 다음 경로로 추적한다.

\`\`\`text id=“rsv33108” 화면 이벤트 E04

→ ServiceId CT.Reservation.create

→ GUID·TraceId

→ CtReservationHandler

→ CtReservationFacade

→ Customer Client

→ insertReservation

→ insertReservationHistory

→ Transaction Commit

→ ETF SUCCESS

→ OM 거래로그


\---

\## 33.4.16 운영 Smoke Test

운영 배포 후 최소 검증:

\`\`\`text id="rsv33109"
목록조회

상세조회

테스트 고객 예약 등록

등록건 수정

등록건 취소

Master·History 확인

거래로그 확인

테스트 데이터 정리

운영 Smoke 전용 고객·데이터와 정리절차를 사용한다.

## 33.4.17 품질 Gate

### 화면 Gate

\`\`\`text id=“rsv33110” 화면 ID

이벤트 ID

ServiceId 연결

권한별 버튼상태

오류코드 표시


\### ServiceId Gate

\`\`\`text id="rsv33111"
중복 없음

Handler 등록

OM Catalog

거래코드

Timeout

권한

감사

### DTO Gate

\`\`\`text id=“rsv33112” Request·Data 분리

사용자·지점 입력 금지

Validation

개인정보 최소화


\### Transaction Gate

\`\`\`text id="rsv33113"
Facade Transaction

Master·History 원자성

외부 호출 위치

Rollback Test

### SQL Gate

\`\`\`text id=“rsv33114” SELECT \* 금지

문자열 치환 금지

Paging

조회기간

Version 조건

상태조건

권한조건

영향 행 검사

실행계획


\### 보안 Gate

\`\`\`text id="rsv33115"
JWT

기능권한

데이터권한

고객번호 마스킹

메모 로그 금지

상세조회 감사

### 테스트 Gate

\`\`\`text id=“rsv33116” 정상

경계

권한

상태

동시성

Transaction

Timeout

성능

운영 Smoke


\---

\## 33.4.18 추적성 관리대장

| 영역 | 관리 항목 |
|---|---|
| 요구사항 | Requirement ID |
| 화면 | 화면 ID·이벤트 ID |
| 거래 | ServiceId·거래코드 |
| 프로그램 | Handler·Facade·Service·Rule·DAO |
| 데이터 | Mapper·SQL ID·Table |
| 보안 | 권한코드·개인정보 |
| 운영 | Timeout·Audit·Metric |
| 테스트 | Test Case ID |
| 배포 | Artifact·Version |
| 변경 | Change·PR·Release |

표준 식별자가 소스·설정·로그에서 모두 일치해야 한다.

\---

\# 정상 예시

\`\`\`text id="rsv33117"
사용자
CT 지점 상담담당자

화면
CT-RSV-0001

이벤트
CT-RSV-0001-E04

ServiceId
CT.Reservation.create

권한
CT\_RESERVATION\_CREATE

입력
고객번호
예약일시
상담목적
메모

서버 결정
예약 ID
READY
지점
담당자
Version 1

처리
고객 확인
중복 확인
Master INSERT
History CREATE INSERT

결과
SUCCESS
예약 ID·상태·Version 반환

운영
거래로그·감사로그·Metric 기록

# 금지 예시

\`\`\`text id=“rsv33118” 화면 버튼을 기준으로 바로 Mapper를 만든다.

요구사항의 미결사항을 개발자가 임의로 결정한다.

등록·수정·취소를 하나의 save ServiceId로 처리한다.

Request DTO를 Mapper Parameter로 그대로 전달한다.

화면이 보낸 userId·branchId를 신뢰한다.

CT Mapper가 고객 도메인 Table을 직접 조회한다.

등록과 History를 다른 Transaction으로 처리한다.

History 오류를 무시하고 Master를 Commit한다.

예약을 물리 DELETE한다.

COMPLETED 예약을 일반 update SQL로 변경한다.

수정 SQL에 Version 조건을 넣지 않는다.

UPDATE 영향 행 0건을 성공으로 반환한다.

목록조회에 기간·건수 제한이 없다.

목록에 메모 전체와 개인정보 원문을 반환한다.

정렬컬럼을 ${sortColumn}으로 치환한다.

중복 등록을 사전조회만으로 방지한다.

Timeout 후 신규 요청번호로 다시 등록한다.

고객 연계 오류를 고객 없음으로 처리한다.

기능권한만 확인하고 데이터권한을 확인하지 않는다.

ServiceId를 소스에만 추가하고 OM에 등록하지 않는다.

정상 테스트만 수행한다.

동시성·Rollback·권한 테스트를 생략한다.

설계서와 소스의 ServiceId가 다르다.

로그에 고객번호·상담메모 원문을 기록한다.


\---

\# 책임 경계와 RACI

| 활동 | 업무 | UI | 개발 | AA | DA·DBA | 보안 | QA | 운영 |
|---|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|
| 요구사항 | A/R | C | C | C | I | C | C | I |
| 상태·규칙 | A/R | C | R/C | A/C | C | C | C | I |
| 화면 이벤트 | A/C | A/R | C | C | I | C | C | I |
| ServiceId | C | C | R | A | I | C | C | C |
| DTO·계층 | I | C | A/R | C | I | C | C | I |
| DB·SQL | C | I | R | C | A/R | C | C | C |
| 권한·감사 | A/C | C | R | C | C | A/R | C | C |
| 동시성 | C | I | R | A | A/C | I | R/C | I |
| 테스트 | C | C | R | C | C | C | A/R | I |
| OM 등록 | C | I | R/C | C | I | C | C | A/R |
| 배포·Smoke | I | I | C | C | C | C | C | A/R |

\---

\# 성능·용량·확장성

| 영역 | 설계 기준 |
|---|---|
| 조회기간 | 최대 3개월 |
| Page Size | 최대 100 |
| 목록 컬럼 | 최소화 |
| 상담메모 | 상세조회에서만 |
| 목록 p95 | 3초 |
| 상세 p95 | 2초 |
| SQL Timeout | TCF Timeout보다 짧게 |
| Index | 고객·지점·상태·예약일시 |
| History | 연간 증가량·보존 산정 |
| DB Pool | 전체 WAR 합계 검토 |
| 고객 연계 | 목록 N+1 금지 |
| Export | 대량은 Batch·파일 |
| Cache | 상담목적·상태명 등 기준정보 |
| Archive | 완료·취소 장기데이터 분리 검토 |

\---

\# 보안·개인정보·감사

\`\`\`text id="rsv33119"
JWT AuthenticationContext를 사용한다.

사용자·지점정보를 Request에서 신뢰하지 않는다.

고객번호를 로그와 목록에서 마스킹한다.

상담메모를 거래로그에 기록하지 않는다.

상세조회와 변경거래를 감사한다.

기능권한과 데이터권한을 분리한다.

운영 DB 직접수정은 승인과 감사가 필요하다.

History와 감사로그의 보존기간을 정의한다.

대량조회·다운로드는 별도 권한과 사유를 적용한다.

# 운영·모니터링·장애 대응

## 필수 운영정보

\`\`\`text id=“rsv33120” ServiceId 상태

권한

Timeout

호출량

p95·p99

오류율

동시성 충돌

중복 등록

Slow SQL

History 실패

고객 연계 오류

배포 Version


\## 장애진단 순서

\`\`\`text id="rsv33121"
화면 오류

→ 화면 이벤트

→ ServiceId

→ GUID·TraceId

→ Handler·Facade·Service

→ 고객 연계

→ DB Pool

→ SQL ID

→ Master·History

→ 최근 Release

## 주요 Alert

| 조건 | 등급 |
| --- | --- |
| 등록 오류율 증가 | Major |
| History 저장 실패 | Critical |
| 동시성 충돌 급증 | Warning·Major |
| 중복 예약 증가 | Major |
| 고객 연계 Timeout | Major |
| 목록 p95 초과 | Major |
| SQL Timeout | Major |
| UNKNOWN 변경거래 | Critical |

# 변경·호환성·폐기 관리

## Request 필드 추가

\`\`\`text id=“rsv33122” 선택 필드로 추가

서버 기본값 정의

Validation

Command 변환

DB 영향

기존 화면 호환


\## Response 필드 추가

기존 필드를 유지하면서 신규 필드를 추가한다.

Rolling 배포 중 구·신 WAR와 화면의 혼재를 검증한다.

\## 상태값 추가

예:

\`\`\`text id="rsv33123"
NO\_SHOW

영향:

\`\`\`text id=“rsv33124” 공통코드

DB Constraint

Rule

화면

조회조건

상태전이

통계

Batch

감사

테스트


\## ServiceId 폐기

\`\`\`text id="rsv33125"
Deprecated 등록

→ 호출 화면 전환

→ 신규 호출 차단

→ 호출량 0 확인

→ Handler 제거

→ OM·문서·테스트 폐기

## 데이터 폐기

취소·완료 예약은 업무분쟁·감사를 위해 일정 기간 보관할 수 있다.

\`\`\`text id=“rsv33126” 보존기간 도래

→ 폐기대상 추출

→ Legal Hold 확인

→ 승인

→ Batch 물리삭제·Archive

→ 건수 대사

→ 감사


\---

\# 완료 체크리스트

\## 요구사항·업무규칙

| 점검 항목 | 완료 |
|---|:---:|
| CRUD 기능을 독립 거래로 분해했다. | □ |
| 결정·가정·미결을 구분했다. | □ |
| Aggregate를 정의했다. | □ |
| 상태와 상태전이를 정의했다. | □ |
| 수정 가능 필드를 정의했다. | □ |
| 기능권한과 데이터권한을 정의했다. | □ |
| 동시성 정책을 정의했다. | □ |
| 중복 정책을 정의했다. | □ |
| 이력과 감사 요구가 있다. | □ |

\## 화면·거래

| 점검 항목 | 완료 |
|---|:---:|
| 화면 ID가 있다. | □ |
| 이벤트 ID가 있다. | □ |
| 이벤트별 ServiceId가 있다. | □ |
| 거래코드가 있다. | □ |
| 권한코드가 있다. | □ |
| Timeout이 있다. | □ |
| Idempotency 대상이 있다. | □ |
| OM 등록정보가 있다. | □ |

\## 프로그램·DTO

| 점검 항목 | 완료 |
|---|:---:|
| Handler 책임이 단순하다. | □ |
| Facade가 Transaction을 담당한다. | □ |
| Service가 업무 흐름을 담당한다. | □ |
| Rule이 업무조건을 판단한다. | □ |
| Request와 Data DTO가 분리된다. | □ |
| 인증문맥에서 사용자정보를 사용한다. | □ |
| 외부 고객연계 위치가 적절하다. | □ |
| DB 객체를 Response로 직접 노출하지 않는다. | □ |

\## SQL·DB

| 점검 항목 | 완료 |
|---|:---:|
| SQL ID가 표준화됐다. | □ |
| 목록 Paging이 있다. | □ |
| 조회기간 제한이 있다. | □ |
| 수정에 상태·Version 조건이 있다. | □ |
| 영향 행 1건을 검사한다. | □ |
| Master·History가 같은 Transaction이다. | □ |
| 중복 Unique를 검토했다. | □ |
| Index와 실행계획을 검토했다. | □ |
| 물리 DELETE를 사용하지 않는다. | □ |

\## 보안·안정성

| 점검 항목 | 완료 |
|---|:---:|
| JWT 인증정보를 사용한다. | □ |
| 기능권한을 적용한다. | □ |
| 데이터권한을 적용한다. | □ |
| 개인정보를 마스킹한다. | □ |
| 등록 중복을 방지한다. | □ |
| Timeout 후 결과조회가 가능하다. | □ |
| 동시성 충돌 메시지가 명확하다. | □ |
| History 실패가 전체 Rollback된다. | □ |

\## 테스트·운영

| 점검 항목 | 완료 |
|---|:---:|
| Rule Unit Test가 있다. | □ |
| Mapper Test가 있다. | □ |
| Rollback Test가 있다. | □ |
| 동시성 Test가 있다. | □ |
| 권한 Test가 있다. | □ |
| Timeout Test가 있다. | □ |
| 성능 Test가 있다. | □ |
| OM Catalog가 준비됐다. | □ |
| Metric·Alert·Runbook이 있다. | □ |
| Smoke Test가 준비됐다. | □ |

\## 추적성·배포

| 점검 항목 | 완료 |
|---|:---:|
| Requirement부터 ServiceId가 연결된다. | □ |
| ServiceId부터 SQL·DB가 연결된다. | □ |
| Requirement별 Test가 있다. | □ |
| DB Migration 순서가 있다. | □ |
| Rollback이 가능하다. | □ |
| Release Note가 있다. | □ |
| Source와 설계서 정합성을 검증한다. | □ |

\---

\# 시사점

\## 핵심 아키텍처 판단

첫째, 상담예약 CRUD는 Table별 SQL 집합이 아니라 \*\*예약 Aggregate의 상태와 규칙을 관리하는 업무서비스\*\*다.

둘째, 등록·수정·취소는 권한·Timeout·감사·동시성 정책이 다르므로 독립 ServiceId로 분리해야 한다.

셋째, 화면에서 전달된 사용자·지점정보는 인증과 권한의 근거로 사용할 수 없다.

넷째, 수정과 취소는 현재 상태와 Version을 SQL 조건에 포함하고 영향 행 수를 반드시 확인해야 한다.

다섯째, 예약 취소는 물리 삭제가 아니라 상태전이로 설계하고 조회·중복·보존·폐기 정책을 함께 결정해야 한다.

여섯째, Master와 History는 하나의 Aggregate 변경이므로 같은 Transaction으로 처리해야 한다.

일곱째, 상담예약 도메인이 고객정보를 필요로 하더라도 고객 Table을 직접 조회해서는 안 되며 도메인 계약을 통해 접근해야 한다.

여덟째, CRUD 완료는 화면과 SQL이 동작하는 상태가 아니라 OM·로그·Metric·테스트·추적성·배포준비까지 완료된 상태다.

\---

\## 주요 위험

| 위험 | 결과 |
|---|---|
| 하나의 Save 거래 | 규칙·권한 혼합 |
| 상태전이 미정의 | 잘못된 수정·취소 |
| Request 사용자 신뢰 | 권한 위조 |
| 고객 Table 직접조회 | 소유권 위반 |
| Version 미적용 | Lost Update |
| 영향 행 미확인 | 실패를 성공 처리 |
| 물리 삭제 | 감사·복구 불가 |
| History 별도 처리 | 이력 불일치 |
| 중복조회만 적용 | 동시 중복 등록 |
| 목록 무제한 | DB·Heap 장애 |
| 메모 원문 로그 | 개인정보 유출 |
| Timeout 후 재등록 | 중복 데이터 |
| OM 등록 누락 | 운영 거래통제 불가 |
| 정상 테스트만 수행 | 운영 결함 누락 |
| 추적성 미작성 | 변경 영향 확인 불가 |

\---

\## 우선 보완 과제

1\. CT 업무코드와 \`ct-service\` 모듈의 공식 배치 위치를 확정한다.
2\. 상담예약 상태·중복기준·권한 미결사항을 업무 승인한다.
3\. \`CT\_CONTACT\_RESERVATION\`과 History의 DDL·Index·보존정책을 확정한다.
4\. 화면 이벤트–ServiceId 추적성 관리대장을 작성한다.
5\. Request·Response·Command·Query·Data DTO 명세를 확정한다.
6\. Version 기반 UPDATE·CANCEL SQL을 설계한다.
7\. Master·History Transaction과 Rollback Test를 정의한다.
8\. 고객 도메인 연계계약과 장애정책을 확정한다.
9\. 등록 Idempotency와 DB Unique 정책을 결정한다.
10\. OM Catalog·권한·Timeout·오류코드·감사를 등록한다.
11\. 정상·경계·권한·동시성·Timeout 테스트를 자동화한다.
12\. 상담예약 거래 Metric과 운영 Runbook을 작성한다.

\---

\## 중장기 발전 방향

\`\`\`text id="rsv33127"
화면 중심 CRUD
↓
Aggregate 중심 업무서비스

단순 Save
↓
목적별 ServiceId

마지막 저장 우선
↓
Version 동시성 통제

물리 Delete
↓
상태전이·보존·폐기

Request 직접 Mapper
↓
DTO 역할 분리

업무별 직접조회
↓
도메인 계약·Read Model

정상 테스트
↓
권한·동시성·Timeout·운영 검증

소스 단독관리
↓
요구사항–화면–ServiceId–SQL–Test 추적성

# 마무리말

상담예약 CRUD를 설계하는 과정은 다음 질문에 답하는 일이다.

\`\`\`text id=“rsv33128” 상담예약 Aggregate의 범위는 어디까지인가?

예약의 현재 상태는 무엇인가?

어떤 상태전이가 허용되는가?

등록·수정·취소는 각각 독립 거래인가?

어떤 필드를 화면이 입력하고 어떤 값을 서버가 결정하는가?

누가 어떤 예약을 조회·변경할 수 있는가?

동시에 두 사용자가 수정하면 어떻게 되는가?

UPDATE 영향 행이 0이면 어떤 오류인가?

취소된 예약은 조회·중복·보존에서 어떻게 처리되는가?

Master와 History가 반드시 같이 Commit되는가?

고객정보는 어느 도메인이 소유하는가?

등록 중복을 어떤 Business Key로 판단하는가?

Timeout 후 DB Commit 여부를 어떻게 확인하는가?

화면 이벤트와 ServiceId가 정확히 연결되는가?

Request·Command·Data·Response가 분리되는가?

오류코드는 업무상황을 구체적으로 표현하는가?

목록조회에 기간·건수·Paging 제한이 있는가?

개인정보 상세조회와 변경을 감사할 수 있는가?

요구사항부터 SQL·테스트까지 추적할 수 있는가?

소스 작성 전에 미결사항과 완료조건이 확정됐는가?



제33장의 핵심 흐름은 다음과 같다.

\`\`\`text id="rsv33129"
요구사항·화면·테이블

→ 결정·가정·미결

→ Aggregate·상태전이

→ 권한·동시성·Transaction

→ ServiceId·오류코드·DTO

→ 프로그램·SQL 설계

→ 테스트·추적성

→ OM·운영 준비

가장 중요한 원칙은 다음과 같다.

\`\`\`text id=“rsv33130” CRUD 개발은 Table에 값을 넣고 읽고 바꾸는 작업이 아니다.

업무상 허용된 상태와 권한 안에서 데이터가 중복되거나 유실되지 않도록 변경하고,

그 결과를 이력·로그·테스트로 증명할 수 있게 만드는 작업이다.

설계가 완료된 뒤에 코드를 작성해야 단순히 동작하는 CRUD가 아니라 운영 가능한 업무서비스가 된다. \`\`\`
