
## 초보 IT 개발자를 위한 NSIGHT TCF 개발 입문서

## 제19부. 요구사항·화면·테이블 정보로 신규 CRUD 설계·구현하기

## 1. 도입 전 안내말

제18부에서는 기존 업무 프로그램을 다음 경로로 분석하는 방법을 배웠습니다.

```
화면

→ ServiceId

→ Handler

→ Facade

→ Service

→ Rule

→ DAO

→ Mapper

→ SQL

→ DB
```

이번 제19부에서는 반대 작업을 수행합니다.

아직 프로그램은 없고 다음 정보만 있다고 가정합니다.

```
업무 요구사항

화면 이미지 또는 화면 항목 목록

버튼과 화면 이벤트

테이블·컬럼 정보

기본적인 업무 설명
```

예를 들어 업무팀이 다음 정도의 정보만 제공할 수 있습니다.

```
고객 상담예약을 조회·등록·수정·취소하는 화면이 필요하다.

고객번호를 기준으로 상담예약을 조회한다.

상담예약일시와 상담목적을 등록한다.

본인이 등록한 예약 또는 동일 지점 예약만 변경할 수 있다.

완료된 예약은 변경하거나 취소할 수 없다.

예약 변경이력을 관리해야 한다.
```

화면 설계자는 다음 정보만 전달할 수 있습니다.

```
고객번호 입력

조회기간 입력

조회 버튼

예약 목록 Grid

신규 버튼

저장 버튼

취소 버튼
```

DB 설계자는 다음 테이블만 제공할 수 있습니다.

```
CT_CONTACT_RESERVATION

CT_CONTACT_RESERVATION_HISTORY
```

이 상태에서 바로 Controller와 Mapper를 만들면 다음 문제가 발생할 수 있습니다.

```
화면 버튼과 ServiceId 관계가 불명확하다.

조회·등록·수정·취소가 하나의 ServiceId에 섞인다.

사용자 ID를 화면에서 전달받는다.

등록과 이력 저장의 트랜잭션 경계가 없다.

수정 시 동시성 충돌을 처리하지 않는다.

취소를 물리 삭제로 처리한다.

조회기간과 최대건수 제한이 없다.

권한·Timeout·감사 기준이 없다.

프로그램은 존재하지만 OM에 등록되지 않는다.

정상 처리만 구현하고 오류·Timeout 테스트가 없다.
```

따라서 화면과 테이블 정보를 프로그램으로 전환하기 전에 다음 단계를 거쳐야 합니다.

```
입력자료 정리

→ 업무 개념 정의

→ 기능 분해

→ 업무규칙 정의

→ 화면 이벤트 정의

→ ServiceId 설계

→ 거래코드·권한·Timeout 정의

→ 프로그램 계층 설계

→ DTO 설계

→ SQL·DB 설계

→ 정상·오류 흐름 구현

→ 테스트

→ OM·배포·추적성 완성
```

제19부에서는 다음 기능을 종합 사례로 사용합니다.

```
업무
고객 상담예약 관리

업무코드
CT

업무세구분
RSV

화면 ID
CT-RSV-0001

주요 기능
목록조회
상세조회
등록
수정
취소
```

제19부의 핵심 원칙은 다음과 같습니다.

```
화면 버튼 수와 ServiceId 수를 무조건 동일하게 만들지 않는다.

하나의 ServiceId는 하나의 명확한 업무 목적을 가진다.

Request DTO와 DB DTO를 분리한다.

화면 사용자정보를 신뢰하지 않는다.

Facade가 트랜잭션 경계를 담당한다.

업무상 삭제는 상태 변경으로 우선 설계한다.

수정 거래에는 Version 기반 동시성 통제를 적용한다.

등록·수정·취소는 이력과 감사를 함께 설계한다.

목록조회에는 기간·건수·Paging 제한을 적용한다.

구현 완료는 소스 작성이 아니라
테스트·OM·추적성·배포 준비까지 끝난 상태다.
```

## 2. 제19부 개요

### 2.1 목적

제19부의 목적은 화면과 테이블 정보만 제공된 상황에서 신규 CRUD 업무기능을 NSIGHT TCF 표준에 맞게 설계·구현하는 전체 절차를 설명하는 것입니다.

학습을 마친 뒤에는 다음 작업을 수행할 수 있어야 합니다.

- 업무팀이 제공한 요구사항을 구조화한다.
- 화면 항목과 테이블 컬럼을 업무 개념으로 연결한다.
- CRUD 기능을 독립된 ServiceId로 분해한다.
- 화면 이벤트와 ServiceId를 매핑한다.
- 거래코드·권한·Timeout·감사정책을 설계한다.
- Handler·Facade·Service·Rule·DAO·Mapper를 설계한다.
- Request·Response·Command·Query·Data DTO를 구분한다.
- 등록·수정·취소의 트랜잭션과 이력을 설계한다.
- Version 기반 낙관적 동시성 제어를 구현한다.
- 사용자·지점 기반 데이터권한을 구현한다.
- 목록조회 Paging과 최대조회 범위를 적용한다.
- MyBatis Mapper XML과 SQL을 작성한다.
- 정상·오류·Timeout·중복 시나리오를 테스트한다.
- OM Service Catalog와 운영 Metric을 등록한다.
- 요구사항부터 DB와 테스트까지 추적성을 완성한다.
- AI 코딩도구가 생성한 CRUD 코드를 검증한다.

### 2.2 적용범위

| 영역 | 주요 내용 |
| --- | --- |
| 요구사항 | 기능·업무규칙·수용기준 |
| 화면 | 화면 ID·항목·이벤트 |
| 거래 | ServiceId·거래코드 |
| 프로그램 | Handler부터 Mapper |
| DTO | Request·Response·Command·Data |
| DB | Master·History·Index |
| SQL | 조회·등록·수정·취소 |
| 보안 | 인증·권한·데이터권한 |
| 안정성 | Timeout·동시성·Idempotency |
| 테스트 | Unit·Mapper·Integration·E2E |
| 운영 | OM·Metric·Alert·Runbook |
| 추적성 | 요구사항부터 Release |
| 변경 | 호환성·Migration·폐기 |

### 2.3 대상 독자

- 화면과 테이블 정보만 받고 CRUD를 구현해야 하는 개발자
- CRUD를 Controller·Service·Mapper 세 개만으로 만드는 개발자
- ServiceId를 어떻게 나눌지 어려운 개발자
- 등록·수정·삭제의 업무규칙을 설계해야 하는 개발자
- 화면 사용자정보와 인증 Context를 혼동하는 개발자
- 프로그램 설계서와 실제 소스를 동시에 작성해야 하는 개발자
- AI 코딩도구로 CRUD 코드를 생성하고 검증해야 하는 개발자
- 신규 기능의 운영전환 자료를 준비하는 개발자

### 2.4 선행조건

다음 내용을 이해하고 있어야 합니다.

```
온라인 거래는 TCF 공통 Endpoint로 들어온다.

ServiceId가 실행할 기능을 식별한다.

Handler는 ServiceId를 Facade와 연결한다.

Facade는 트랜잭션 경계를 담당한다.

Service는 업무 처리순서를 담당한다.

Rule은 업무 검증과 계산을 담당한다.

DAO와 Mapper는 데이터 접근을 담당한다.

ETF는 표준 결과와 거래로그를 마무리한다.
```

### 2.5 주요 용어

| 용어 | 설명 |
| --- | --- |
| CRUD | 등록·조회·수정·삭제 |
| Logical Delete | 실제 행을 삭제하지 않고 상태를 변경 |
| Aggregate | 하나의 트랜잭션으로 일관성을 유지하는 업무 단위 |
| Master | 현재 상태를 보관하는 주 테이블 |
| History | 변경 이력을 보관하는 테이블 |
| Optimistic Lock | Version을 비교하여 동시 수정을 통제 |
| Command DTO | 내부 변경 명령 데이터 |
| Query DTO | 내부 조회 조건 데이터 |
| Data DTO | DB 조회결과 데이터 |
| Request DTO | 외부 요청 계약 |
| Response DTO | 외부 응답 계약 |
| Idempotency | 같은 요청의 중복 실행 방지 |
| Business Key | 업무적으로 유일한 식별값 |
| State Transition | 업무상태 변경규칙 |
| Acceptance Criteria | 요구사항 완료 판단 기준 |

## 제192장. 화면·테이블·요구사항 입력자료 정리

### 192.1 개발 시작 전에 확보할 정보

최소한 다음 정보를 확보합니다.

```
화면 ID와 화면명

화면 항목

버튼과 이벤트

목록·상세 구조

테이블과 컬럼

업무상태

사용자·지점 권한

조회기간과 예상건수

등록·수정·취소 조건

이력과 감사 요구

성능·Timeout 요구
```

### 192.2 원시 요구사항 예

```
고객 상담예약을 관리한다.

고객번호와 기간으로 예약을 조회한다.

상담예약일시와 상담목적을 등록한다.

예약일시와 메모를 수정할 수 있다.

완료된 예약은 수정할 수 없다.

예약은 삭제하지 않고 취소 처리한다.

동일한 예약을 여러 사용자가 동시에 수정하지 못하게 한다.

모든 변경이력을 남긴다.
```

### 192.3 구조화된 요구사항

| 요구사항 ID | 내용 | 분류 |
| --- | --- | --- |
| REQ-CT-RSV-001 | 상담예약 목록을 조회한다 | 조회 |
| REQ-CT-RSV-002 | 상담예약 상세를 조회한다 | 조회 |
| REQ-CT-RSV-003 | 상담예약을 등록한다 | 등록 |
| REQ-CT-RSV-004 | 상담예약을 수정한다 | 수정 |
| REQ-CT-RSV-005 | 상담예약을 취소한다 | 상태변경 |
| REQ-CT-RSV-006 | 모든 변경이력을 저장한다 | 감사 |
| REQ-CT-RSV-007 | 동시 수정을 통제한다 | 정합성 |
| NFR-CT-RSV-001 | 목록조회 p95 3초 이하 | 성능 |
| NFR-CT-RSV-002 | 등록·수정 p95 2초 이하 | 성능 |

### 192.4 화면 항목 예

| UI 객체 ID | 항목명 | 유형 | 필수 | 비고 |
| --- | --- | --- | --- | --- |
| txtCustomerNo | 고객번호 | Text | Y | 조회조건 |
| dtFromDate | 시작일 | Date | Y | 최대 3개월 |
| dtToDate | 종료일 | Date | Y | 시작일 이상 |
| cboStatus | 상태 | Select | N | 공통코드 |
| grdReservation | 예약목록 | Grid | - | 최대 100건 |
| dtReservationDtm | 예약일시 | DateTime | Y | 등록·수정 |
| cboPurposeCode | 상담목적 | Select | Y | 공통코드 |
| txtMemo | 메모 | Textarea | N | 최대 1,000자 |
| btnSearch | 조회 | Button | - | 목록조회 |
| btnNew | 신규 | Button | - | 입력 초기화 |
| btnSave | 저장 | Button | - | 등록·수정 |
| btnCancelReservation | 예약취소 | Button | - | 상태 변경 |

### 192.5 제공된 테이블 예

#### CT_CONTACT_RESERVATION

| 컬럼 | 의미 | 유형 |
| --- | --- | --- |
| RESERVATION_ID | 예약 ID | PK |
| CUSTOMER_NO | 고객번호 | 업무참조 |
| RESERVATION_DTM | 상담예약일시 | 업무데이터 |
| PURPOSE_CD | 상담목적코드 | 코드 |
| MEMO_CONTENT | 예약 메모 | 업무데이터 |
| STATUS_CD | 예약상태 | 상태 |
| BRANCH_ID | 등록 지점 | 데이터권한 |
| OWNER_USER_ID | 담당 사용자 | 데이터권한 |
| VERSION_NO | Version | 동시성 |
| CREATED_BY | 생성자 | 감사 |
| CREATED_DTM | 생성일시 | 감사 |
| UPDATED_BY | 변경자 | 감사 |
| UPDATED_DTM | 변경일시 | 감사 |

#### CT_CONTACT_RESERVATION_HISTORY

| 컬럼 | 의미 |
| --- | --- |
| HISTORY_ID | 이력 ID |
| RESERVATION_ID | 예약 ID |
| CHANGE_TYPE_CD | CREATE·UPDATE·CANCEL |
| BEFORE_STATUS_CD | 이전 상태 |
| AFTER_STATUS_CD | 이후 상태 |
| CHANGE_CONTENT | 변경 요약 |
| CHANGED_BY | 변경 사용자 |
| CHANGED_DTM | 변경일시 |
| TRACE_ID | 거래 추적 ID |

### 192.6 입력자료의 문제점 확인

테이블과 화면만으로는 다음 내용이 부족할 수 있습니다.

```
상담목적 코드의 공식 코드그룹은 무엇인가?

예약상태의 종류는 무엇인가?

취소 후 재활성화가 가능한가?

본인 예약만 수정 가능한가?

관리자는 모든 지점 예약을 수정 가능한가?

과거 예약일시를 등록할 수 있는가?

같은 고객에게 동일 시간 예약을 중복 등록할 수 있는가?

메모에 개인정보를 저장할 수 있는가?

완료 처리는 어느 기능에서 수행하는가?
```

이 항목은 구현자가 임의 결정하지 않고 업무 확인사항으로 등록합니다.

### 192.7 결정·가정·미결 구분

| 구분 | 내용 |
| --- | --- |
| 결정 | 예약 취소는 논리 삭제로 처리 |
| 결정 | 수정은 Version 기반 동시성 통제 |
| 가정 | 예약 조회기간 최대 3개월 |
| 가정 | 페이지 크기 최대 100건 |
| 미결 | 동일 시간 중복 예약 허용 여부 |
| 미결 | 관리자 타 지점 수정 허용 여부 |

### 제192장 요약

```
화면과 테이블은 구현의 입력자료이지만
완전한 업무설계는 아니다.

부족한 업무규칙·권한·상태·성능 조건을 확인하고
결정·가정·미결사항을 분리해야 한다.
```

## 제193장. 업무 개념·상태·규칙 설계

### 193.1 Aggregate 정의

이 실습에서 하나의 상담예약 Aggregate는 다음으로 구성됩니다.

```
상담예약 Master

+ 상담예약 변경이력

+ Version

+ 상태전이
```

등록·수정·취소 시 Master와 History는 하나의 업무 단위로 처리합니다.

### 193.2 예약 상태

```
READY
예약대기

COMPLETED
상담완료

CANCELED
예약취소
```

필요에 따라 IN_PROGRESS, NO_SHOW 등을 추가할 수 있지만, 이번 실습에서는 세 가지 상태만 사용합니다.

### 193.3 상태전이

```
READY
  ├─→ COMPLETED
  └─→ CANCELED
```

금지:

```
COMPLETED → READY

COMPLETED → CANCELED

CANCELED → READY
```

재활성화가 필요한 경우 별도 복구 ServiceId와 승인을 설계합니다.

### 193.4 주요 업무규칙

| Rule ID | 내용 |
| --- | --- |
| BR-CT-RSV-001 | 조회기간은 최대 3개월 |
| BR-CT-RSV-002 | 페이지 크기는 최대 100건 |
| BR-CT-RSV-003 | 예약일시는 필수 |
| BR-CT-RSV-004 | 상담목적코드는 유효한 코드여야 함 |
| BR-CT-RSV-005 | READY 상태만 수정 가능 |
| BR-CT-RSV-006 | READY 상태만 취소 가능 |
| BR-CT-RSV-007 | Version이 일치해야 수정 가능 |
| BR-CT-RSV-008 | 동일 지점 또는 담당 사용자만 변경 가능 |
| BR-CT-RSV-009 | 등록·수정·취소 이력 필수 |
| BR-CT-RSV-010 | 고객 존재 여부를 확인 |

### 193.5 조회 권한과 변경 권한

```
조회
같은 지점 예약 조회 가능

변경
예약 담당자 또는 관리자만 가능

관리자
권한정책에 따라 타 지점 접근 가능
```

기능권한과 데이터권한을 구분합니다.

```
기능권한
CT_RESERVATION_UPDATE

데이터권한
해당 사용자가 이 예약 데이터를 변경할 수 있는가
```

### 193.6 등록 정책

등록 시 서버가 설정할 값:

```
RESERVATION_ID

STATUS_CD = READY

BRANCH_ID = 인증 Context의 지점

OWNER_USER_ID = 인증 Context의 사용자

VERSION_NO = 1

CREATED_BY

CREATED_DTM

UPDATED_BY

UPDATED_DTM
```

화면에서 전달받지 않습니다.

### 193.7 수정 정책

수정 가능 필드:

```
RESERVATION_DTM

PURPOSE_CD

MEMO_CONTENT
```

수정 불가 필드:

```
RESERVATION_ID

CUSTOMER_NO

BRANCH_ID

OWNER_USER_ID

STATUS_CD
```

상태 변경은 별도 ServiceId로 처리합니다.

### 193.8 취소 정책

취소는 다음 Update로 처리합니다.

```
STATUS_CD
READY → CANCELED

VERSION_NO
현재값 + 1

UPDATED_BY
인증 사용자

UPDATED_DTM
현재시각
```

History에는 CANCEL을 저장합니다.

### 193.9 오류코드 설계

| 오류코드 | 의미 |
| --- | --- |
| E-CT-RSV-0001 | 예약을 찾을 수 없음 |
| E-CT-RSV-0002 | 조회기간 초과 |
| E-CT-RSV-0003 | 유효하지 않은 상담목적 |
| E-CT-RSV-0004 | 변경할 수 없는 예약상태 |
| E-CT-RSV-0005 | 예약 변경권한 없음 |
| E-CT-RSV-0006 | 다른 사용자가 먼저 변경함 |
| E-CT-RSV-0007 | 고객을 찾을 수 없음 |
| E-CT-RSV-0008 | 중복 예약 |
| E-CT-RSV-0009 | 페이지 크기 초과 |

### 제193장 요약

```
CRUD 구현 전에 Aggregate·상태·상태전이·권한을 정의한다.

수정 가능 필드와 상태 변경 기능을 분리한다.

화면 입력값과 서버가 결정하는 감사·권한 값을 구분한다.
```

## 제194장. 화면 이벤트·ServiceId·거래코드 설계

### 194.1 기능 분해

CRUD를 다음 독립 거래로 나눕니다.

```
목록조회

상세조회

등록

수정

취소
```

하나의 save ServiceId가 등록과 수정을 모두 처리하지 않도록 합니다.

### 194.2 ServiceId

| 기능 | ServiceId |
| --- | --- |
| 목록조회 | CT.Reservation.selectList |
| 상세조회 | CT.Reservation.selectDetail |
| 등록 | CT.Reservation.create |
| 수정 | CT.Reservation.update |
| 취소 | CT.Reservation.cancel |

### 194.3 거래코드

| ServiceId | 거래코드 |
| --- | --- |
| CT.Reservation.selectList | CT-INQ-0101 |
| CT.Reservation.selectDetail | CT-INQ-0102 |
| CT.Reservation.create | CT-REG-0101 |
| CT.Reservation.update | CT-UPD-0101 |
| CT.Reservation.cancel | CT-UPD-0102 |

### 194.4 화면 이벤트

| 이벤트 ID | 화면 동작 | ServiceId |
| --- | --- | --- |
| CT-RSV-0001-E01 | 화면 초기조회 | CT.Reservation.selectList |
| CT-RSV-0001-E02 | 조회 버튼 | CT.Reservation.selectList |
| CT-RSV-0001-E03 | Grid 행 선택 | CT.Reservation.selectDetail |
| CT-RSV-0001-E04 | 신규 저장 | CT.Reservation.create |
| CT-RSV-0001-E05 | 수정 저장 | CT.Reservation.update |
| CT-RSV-0001-E06 | 예약 취소 | CT.Reservation.cancel |

신규 버튼은 서버를 호출하지 않고 화면 상태만 초기화할 수 있습니다.

### 194.5 권한코드

| 기능 | 권한코드 |
| --- | --- |
| 조회 | CT_RESERVATION_VIEW |
| 등록 | CT_RESERVATION_CREATE |
| 수정 | CT_RESERVATION_UPDATE |
| 취소 | CT_RESERVATION_CANCEL |

### 194.6 Timeout

| ServiceId | Timeout |
| --- | --- |
| 목록조회 | 3,000ms |
| 상세조회 | 2,000ms |
| 등록 | 5,000ms |
| 수정 | 5,000ms |
| 취소 | 5,000ms |

실제 값은 성능시험 후 확정합니다.

### 194.7 감사정책

| ServiceId | 감사 |
| --- | --- |
| 목록조회 | 일반 거래로그 |
| 상세조회 | 개인정보 조회감사 |
| 등록 | 변경감사 |
| 수정 | 변경감사 |
| 취소 | 중요 변경감사 |

### 194.8 Idempotency

| 기능 | 적용 |
| --- | --- |
| 목록조회 | 불필요 |
| 상세조회 | 불필요 |
| 등록 | 필수 권고 |
| 수정 | 적용 검토 |
| 취소 | 필수 권고 |

등록은 중복 생성 위험이 높으므로 Idempotency Key를 적용합니다.

수정과 취소는 Version과 상태조건으로 중복 실행을 상당 부분 통제할 수 있지만, Client 재시도 정책에 따라 Idempotency를 추가할 수 있습니다.

### 194.9 거래 설계 요약

| ServiceId | 유형 | 권한 | Timeout | Idempotency |
| --- | --- | --- | --- | --- |
| selectList | 조회 | VIEW | 3초 | N |
| selectDetail | 조회 | VIEW | 2초 | N |
| create | 등록 | CREATE | 5초 | Y |
| update | 변경 | UPDATE | 5초 | 검토 |
| cancel | 상태변경 | CANCEL | 5초 | Y |

### 제194장 요약

```
화면 이벤트와 ServiceId는 1:1 또는 N:M 관계가 될 수 있다.

등록·수정·취소는 서로 다른 ServiceId로 분리한다.

각 ServiceId에 거래코드·권한·Timeout·감사정책을 연결한다.
```

## 제195장. 패키지·프로그램·DTO 구조 설계

### 195.1 패키지 구조

```
com.nh.nsight.marketing.ct.reservation
├─ handler
├─ facade
├─ service
├─ rule
├─ dao
├─ mapper
├─ client
├─ dto
│  ├─ request
│  ├─ response
│  ├─ command
│  ├─ query
│  └─ data
└─ constant
```

### 195.2 프로그램 목록

| 계층 | 프로그램 |
| --- | --- |
| Handler | CtReservationHandler |
| Facade | CtReservationFacade |
| Service | CtReservationService |
| Rule | CtReservationRule |
| DAO | CtReservationDao |
| Mapper | CtReservationMapper |
| Client | IcCustomerClient |
| ServiceId | CtReservationServiceIds |

### 195.3 DTO 목록

#### Request

```
ReservationListRequest

ReservationDetailRequest

ReservationCreateRequest

ReservationUpdateRequest

ReservationCancelRequest
```

#### Response

```
ReservationListResponse

ReservationListItemResponse

ReservationDetailResponse

ReservationCreateResponse

ReservationUpdateResponse

ReservationCancelResponse
```

#### 내부 DTO

```
ReservationListQuery

ReservationCreateCommand

ReservationUpdateCommand

ReservationCancelCommand

ReservationData

ReservationHistoryCommand
```

### 195.4 목록 Request 예

```
public record ReservationListRequest(
    String customerNo,
    LocalDate fromDate,
    LocalDate toDate,
    String statusCode,
    Integer pageNumber,
    Integer pageSize
) {
}
```

### 195.5 등록 Request 예

```
public record ReservationCreateRequest(
    String customerNo,
    LocalDateTime reservationDateTime,
    String purposeCode,
    String memoContent,
    String idempotencyKey
) {
}
```

다음 필드는 포함하지 않습니다.

```
userId

branchId

statusCode

versionNo

createdBy
```

### 195.6 수정 Request 예

```
public record ReservationUpdateRequest(
    String reservationId,
    LocalDateTime reservationDateTime,
    String purposeCode,
    String memoContent,
    Long versionNo
) {
}
```

### 195.7 취소 Request 예

```
public record ReservationCancelRequest(
    String reservationId,
    String cancelReason,
    Long versionNo,
    String idempotencyKey
) {
}
```

### 195.8 Command 변환

Request와 Context를 조합합니다.

```
ReservationCreateCommand command =
    new ReservationCreateCommand(
        generatedReservationId,
        request.customerNo(),
        request.reservationDateTime(),
        request.purposeCode(),
        request.memoContent(),
        ReservationStatus.READY,
        context.getBranchId(),
        context.getUserId(),
        1L,
        context.getTraceId()
    );
```

### 195.9 Data DTO

```
public record ReservationData(
    String reservationId,
    String customerNo,
    LocalDateTime reservationDateTime,
    String purposeCode,
    String memoContent,
    String statusCode,
    String branchId,
    String ownerUserId,
    Long versionNo,
    String createdBy,
    LocalDateTime createdDateTime,
    String updatedBy,
    LocalDateTime updatedDateTime
) {
}
```

DB 컬럼 구조를 담지만 외부 응답과는 분리합니다.

### 195.10 DTO 분리 이유

```
Request
외부 계약과 Validation

Command
인증 Context와 서버 계산값

Data
DB 결과

Response
화면 공개계약
```

같은 DTO를 전 계층에서 사용하면 DB 변경이 화면 계약에 직접 전파됩니다.

### 제195장 요약

```
Package는 업무·도메인·계층으로 구성한다.

Request·Command·Data·Response를 역할별로 분리한다.

사용자·지점·감사정보는 Context에서 Command로 구성한다.
```

## 제196장. Handler·Facade 구현

### 196.1 ServiceId 상수

```
public final class CtReservationServiceIds {

    public static final String SELECT_LIST =
        "CT.Reservation.selectList";

    public static final String SELECT_DETAIL =
        "CT.Reservation.selectDetail";

    public static final String CREATE =
        "CT.Reservation.create";

    public static final String UPDATE =
        "CT.Reservation.update";

    public static final String CANCEL =
        "CT.Reservation.cancel";

    private CtReservationServiceIds() {
    }
}
```

### 196.2 Handler 구조

```
@Component
@RequiredArgsConstructor
public class CtReservationHandler
        implements TransactionHandler {

    private final CtReservationFacade facade;
    private final RequestBodyConverter converter;

    @Override
    public Set<String> serviceIds() {
        return Set.of(
            CtReservationServiceIds.SELECT_LIST,
            CtReservationServiceIds.SELECT_DETAIL,
            CtReservationServiceIds.CREATE,
            CtReservationServiceIds.UPDATE,
            CtReservationServiceIds.CANCEL
        );
    }

    @Override
    public Object handle(
            String serviceId,
            Object body,
            TransactionContext context) {

        return switch (serviceId) {
            case CtReservationServiceIds.SELECT_LIST ->
                facade.selectList(
                    converter.convert(
                        body,
                        ReservationListRequest.class
                    ),
                    context
                );

            case CtReservationServiceIds.SELECT_DETAIL ->
                facade.selectDetail(
                    converter.convert(
                        body,
                        ReservationDetailRequest.class
                    ),
                    context
                );

            case CtReservationServiceIds.CREATE ->
                facade.create(
                    converter.convert(
                        body,
                        ReservationCreateRequest.class
                    ),
                    context
                );

            case CtReservationServiceIds.UPDATE ->
                facade.update(
                    converter.convert(
                        body,
                        ReservationUpdateRequest.class
                    ),
                    context
                );

            case CtReservationServiceIds.CANCEL ->
                facade.cancel(
                    converter.convert(
                        body,
                        ReservationCancelRequest.class
                    ),
                    context
                );

            default ->
                throw new UnsupportedServiceIdException(
                    serviceId
                );
        };
    }
}
```

### 196.3 Handler 책임

Handler는 다음만 담당합니다.

```
ServiceId 분기

Request DTO 변환

Facade 호출

응답 반환
```

Handler에서 다음을 수행하지 않습니다.

```
DB 조회

업무상태 검증

Transaction 시작

사용자권한 계산

SQL Parameter 생성
```

### 196.4 Facade 구조

```
@Component
@RequiredArgsConstructor
public class CtReservationFacade {

    private final CtReservationService service;

    @Transactional(readOnly = true)
    public ReservationListResponse selectList(
            ReservationListRequest request,
            TransactionContext context) {

        return service.selectList(
            request,
            context
        );
    }

    @Transactional(readOnly = true)
    public ReservationDetailResponse selectDetail(
            ReservationDetailRequest request,
            TransactionContext context) {

        return service.selectDetail(
            request,
            context
        );
    }

    @Transactional
    public ReservationCreateResponse create(
            ReservationCreateRequest request,
            TransactionContext context) {

        return service.create(
            request,
            context
        );
    }

    @Transactional
    public ReservationUpdateResponse update(
            ReservationUpdateRequest request,
            TransactionContext context) {

        return service.update(
            request,
            context
        );
    }

    @Transactional
    public ReservationCancelResponse cancel(
            ReservationCancelRequest request,
            TransactionContext context) {

        return service.cancel(
            request,
            context
        );
    }
}
```

### 196.5 Facade 트랜잭션 원칙

```
조회
readOnly = true

등록·수정·취소
readOnly = false

Master와 History
같은 Transaction

외부 고객조회
가능하면 DB Transaction 시작 전 수행
```

외부 고객검증을 Transaction 내부에서 수행하면 DB Connection 장기 점유 위험이 있습니다.

### 196.6 Facade 대안

외부 고객검증을 먼저 수행하는 경우:

```
Handler

→ Application Orchestrator

→ 외부 고객검증

→ Transactional Facade

→ Master·History 저장
```

프로젝트 표준과 호출 복잡도에 따라 별도 Orchestrator를 둘 수 있습니다.

### 제196장 요약

```
Handler는 ServiceId 분기와 DTO 변환만 담당한다.

Facade는 유스케이스의 트랜잭션 경계를 담당한다.

외부 호출과 DB Transaction의 위치를 명확히 설계한다.
```

## 제197장. Service·Rule 구현

### 197.1 목록조회 Service

```
@Service
@RequiredArgsConstructor
public class CtReservationService {

    private final CtReservationRule rule;
    private final CtReservationDao dao;
    private final IcCustomerClient customerClient;
    private final ReservationIdGenerator idGenerator;
    private final Clock clock;

    public ReservationListResponse selectList(
            ReservationListRequest request,
            TransactionContext context) {

        rule.validateListRequest(request);

        ReservationListQuery query =
            ReservationListQuery.from(
                request,
                context.getBranchId(),
                context.hasRole("CT_RESERVATION_ADMIN")
            );

        long totalCount =
            dao.countReservationList(query);

        List<ReservationData> dataList =
            totalCount == 0
                ? List.of()
                : dao.selectReservationList(query);

        return ReservationListResponse.of(
            dataList,
            request.pageNumber(),
            request.pageSize(),
            totalCount
        );
    }
}
```

### 197.2 상세조회 Service

```
public ReservationDetailResponse selectDetail(
        ReservationDetailRequest request,
        TransactionContext context) {

    ReservationData reservation =
        dao.selectReservationDetail(
            request.reservationId()
        );

    rule.validateExists(reservation);
    rule.validateReadable(
        reservation,
        context
    );

    return ReservationDetailResponse.from(
        reservation
    );
}
```

### 197.3 등록 Service 흐름

```
Request Validation

→ 고객 존재 확인

→ 상담목적코드 검증

→ 중복예약 검증

→ ID 생성

→ Command 생성

→ Master Insert

→ History Insert

→ 응답 생성
```

### 197.4 등록 Service 예

```
public ReservationCreateResponse create(
        ReservationCreateRequest request,
        TransactionContext context) {

    rule.validateCreateRequest(request);

    CustomerBasicInfo customer =
        customerClient.selectBasic(
            request.customerNo(),
            context
        );

    rule.validateCustomerExists(customer);
    rule.validatePurposeCode(
        request.purposeCode()
    );

    boolean duplicated =
        dao.existsDuplicateReservation(
            request.customerNo(),
            request.reservationDateTime()
        );

    rule.validateNotDuplicated(duplicated);

    String reservationId =
        idGenerator.generate();

    ReservationCreateCommand command =
        ReservationCreateCommand.of(
            reservationId,
            request,
            context,
            clock.instant()
        );

    int inserted =
        dao.insertReservation(command);

    rule.validateSingleAffected(inserted);

    dao.insertHistory(
        ReservationHistoryCommand.create(
            command,
            context.getTraceId()
        )
    );

    return new ReservationCreateResponse(
        reservationId,
        ReservationStatus.READY.code(),
        1L
    );
}
```

### 197.5 수정 Service 흐름

```
현재 데이터 조회

→ 존재 확인

→ 수정 권한 확인

→ 상태 확인

→ Version 확인

→ 입력 Validation

→ Version 조건 Update

→ 영향 건수 확인

→ History Insert

→ 응답
```

### 197.6 수정 Service 예

```
public ReservationUpdateResponse update(
        ReservationUpdateRequest request,
        TransactionContext context) {

    rule.validateUpdateRequest(request);

    ReservationData current =
        dao.selectReservationDetail(
            request.reservationId()
        );

    rule.validateExists(current);
    rule.validateUpdatable(current);
    rule.validateWritable(current, context);
    rule.validateVersion(
        current.versionNo(),
        request.versionNo()
    );

    ReservationUpdateCommand command =
        ReservationUpdateCommand.of(
            request,
            context,
            clock.instant()
        );

    int updated =
        dao.updateReservation(command);

    rule.validateOptimisticLock(updated);

    dao.insertHistory(
        ReservationHistoryCommand.update(
            current,
            command,
            context.getTraceId()
        )
    );

    return new ReservationUpdateResponse(
        request.reservationId(),
        request.versionNo() + 1
    );
}
```

### 197.7 취소 Service 흐름

```
현재 데이터 조회

→ 존재 확인

→ 취소 권한

→ READY 상태 확인

→ Version 확인

→ CANCELED 상태 Update

→ History Insert

→ 응답
```

### 197.8 Rule 예

```
@Component
public class CtReservationRule {

    public void validateListRequest(
            ReservationListRequest request) {

        if (request.fromDate() == null
                || request.toDate() == null) {
            throw new BusinessException(
                "E-CT-RSV-0002",
                "조회기간은 필수입니다."
            );
        }

        if (request.fromDate()
                .plusMonths(3)
                .isBefore(request.toDate())) {
            throw new BusinessException(
                "E-CT-RSV-0002",
                "조회기간은 최대 3개월입니다."
            );
        }

        if (request.pageSize() > 100) {
            throw new BusinessException(
                "E-CT-RSV-0009",
                "페이지 크기는 최대 100건입니다."
            );
        }
    }

    public void validateUpdatable(
            ReservationData current) {

        if (!ReservationStatus.READY.code()
                .equals(current.statusCode())) {
            throw new BusinessException(
                "E-CT-RSV-0004",
                "현재 상태에서는 변경할 수 없습니다."
            );
        }
    }

    public void validateOptimisticLock(
            int affectedRows) {

        if (affectedRows != 1) {
            throw new BusinessException(
                "E-CT-RSV-0006",
                "다른 사용자가 먼저 변경했습니다."
            );
        }
    }
}
```

### 197.9 Rule의 시간 의존성

현재시각 검증이 필요한 경우 LocalDateTime.now()를 Rule 내부에서 직접 호출하지 않고 Clock 또는 기준시각을 전달합니다.

테스트의 재현성이 높아집니다.

### 197.10 Rule 금지 예

```
Rule에서 DB Mapper 호출

Rule에서 외부 Client 호출

Rule에서 Transaction 시작

Rule에서 인증 Token 해석

Rule에서 상태를 직접 저장
```

### 제197장 요약

```
Service는 업무 처리순서를 조립한다.

Rule은 상태·권한·기간·Version 등 업무조건을 검증한다.

수정과 취소는 현재 데이터 조회 후 권한·상태·Version을 확인한다.
```

## 제198장. DAO·Mapper·SQL 구현

### 198.1 DAO 구조

```
@Repository
@RequiredArgsConstructor
public class CtReservationDao {

    private final CtReservationMapper mapper;

    public long countReservationList(
            ReservationListQuery query) {
        return mapper.countReservationList(query);
    }

    public List<ReservationData> selectReservationList(
            ReservationListQuery query) {
        return mapper.selectReservationList(query);
    }

    public ReservationData selectReservationDetail(
            String reservationId) {
        return mapper.selectReservationDetail(
            reservationId
        );
    }

    public int insertReservation(
            ReservationCreateCommand command) {
        return mapper.insertReservation(command);
    }

    public int updateReservation(
            ReservationUpdateCommand command) {
        return mapper.updateReservation(command);
    }

    public int cancelReservation(
            ReservationCancelCommand command) {
        return mapper.cancelReservation(command);
    }

    public int insertHistory(
            ReservationHistoryCommand command) {
        return mapper.insertHistory(command);
    }
}
```

### 198.2 Mapper Interface

```
@Mapper
public interface CtReservationMapper {

    long countReservationList(
        ReservationListQuery query
    );

    List<ReservationData> selectReservationList(
        ReservationListQuery query
    );

    ReservationData selectReservationDetail(
        String reservationId
    );

    boolean existsDuplicateReservation(
        String customerNo,
        LocalDateTime reservationDateTime
    );

    int insertReservation(
        ReservationCreateCommand command
    );

    int updateReservation(
        ReservationUpdateCommand command
    );

    int cancelReservation(
        ReservationCancelCommand command
    );

    int insertHistory(
        ReservationHistoryCommand command
    );
}
```

### 198.3 SQL ID

| Mapper Method | SQL ID |
| --- | --- |
| countReservationList | CT-RSV-SEL-001 |
| selectReservationList | CT-RSV-SEL-002 |
| selectReservationDetail | CT-RSV-SEL-003 |
| existsDuplicateReservation | CT-RSV-SEL-004 |
| insertReservation | CT-RSV-INS-001 |
| updateReservation | CT-RSV-UPD-001 |
| cancelReservation | CT-RSV-UPD-002 |
| insertHistory | CT-RSV-INS-002 |

### 198.4 목록조회 SQL 원칙

```
고객번호 필수

조회기간 필수

지점 권한조건

상태 선택조건

정렬기준 고정

최대건수

Paging

목록에 불필요한 메모 본문 제외
```

### 198.5 목록 SQL 예

```
<select
    id="selectReservationList"
    parameterType="...ReservationListQuery"
    resultType="...ReservationData">

    SELECT RESERVATION_ID
         , CUSTOMER_NO
         , RESERVATION_DTM
         , PURPOSE_CD
         , STATUS_CD
         , BRANCH_ID
         , OWNER_USER_ID
         , VERSION_NO
         , UPDATED_DTM
      FROM CT_CONTACT_RESERVATION
     WHERE CUSTOMER_NO = #{customerNo}
       AND RESERVATION_DTM >= #{fromDateTime}
       AND RESERVATION_DTM < #{toDateTimeExclusive}
       <if test="statusCode != null
                 and statusCode != ''">
       AND STATUS_CD = #{statusCode}
       </if>
       <if test="admin == false">
       AND BRANCH_ID = #{branchId}
       </if>
     ORDER BY RESERVATION_DTM DESC
     OFFSET #{offset} ROWS
     FETCH NEXT #{pageSize} ROWS ONLY
</select>
```

Oracle Version과 프로젝트 Paging 표준에 맞게 문법을 조정합니다.

### 198.6 상세조회 SQL

상세에서만 메모 본문을 조회합니다.

```
<select
    id="selectReservationDetail"
    resultType="...ReservationData">

    SELECT RESERVATION_ID
         , CUSTOMER_NO
         , RESERVATION_DTM
         , PURPOSE_CD
         , MEMO_CONTENT
         , STATUS_CD
         , BRANCH_ID
         , OWNER_USER_ID
         , VERSION_NO
         , CREATED_BY
         , CREATED_DTM
         , UPDATED_BY
         , UPDATED_DTM
      FROM CT_CONTACT_RESERVATION
     WHERE RESERVATION_ID = #{reservationId}
</select>
```

### 198.7 등록 SQL

```
<insert id="insertReservation">
    INSERT INTO CT_CONTACT_RESERVATION
    (
        RESERVATION_ID,
        CUSTOMER_NO,
        RESERVATION_DTM,
        PURPOSE_CD,
        MEMO_CONTENT,
        STATUS_CD,
        BRANCH_ID,
        OWNER_USER_ID,
        VERSION_NO,
        CREATED_BY,
        CREATED_DTM,
        UPDATED_BY,
        UPDATED_DTM
    )
    VALUES
    (
        #{reservationId},
        #{customerNo},
        #{reservationDateTime},
        #{purposeCode},
        #{memoContent},
        #{statusCode},
        #{branchId},
        #{ownerUserId},
        #{versionNo},
        #{createdBy},
        #{createdDateTime},
        #{updatedBy},
        #{updatedDateTime}
    )
</insert>
```

### 198.8 수정 SQL

```
<update id="updateReservation">
    UPDATE CT_CONTACT_RESERVATION
       SET RESERVATION_DTM = #{reservationDateTime}
         , PURPOSE_CD = #{purposeCode}
         , MEMO_CONTENT = #{memoContent}
         , VERSION_NO = VERSION_NO + 1
         , UPDATED_BY = #{updatedBy}
         , UPDATED_DTM = #{updatedDateTime}
     WHERE RESERVATION_ID = #{reservationId}
       AND STATUS_CD = 'READY'
       AND VERSION_NO = #{versionNo}
</update>
```

상태와 Version을 SQL 조건에 함께 넣습니다.

### 198.9 취소 SQL

```
<update id="cancelReservation">
    UPDATE CT_CONTACT_RESERVATION
       SET STATUS_CD = 'CANCELED'
         , VERSION_NO = VERSION_NO + 1
         , UPDATED_BY = #{updatedBy}
         , UPDATED_DTM = #{updatedDateTime}
     WHERE RESERVATION_ID = #{reservationId}
       AND STATUS_CD = 'READY'
       AND VERSION_NO = #{versionNo}
</update>
```

### 198.10 History SQL

```
<insert id="insertHistory">
    INSERT INTO CT_CONTACT_RESERVATION_HISTORY
    (
        HISTORY_ID,
        RESERVATION_ID,
        CHANGE_TYPE_CD,
        BEFORE_STATUS_CD,
        AFTER_STATUS_CD,
        CHANGE_CONTENT,
        CHANGED_BY,
        CHANGED_DTM,
        TRACE_ID
    )
    VALUES
    (
        #{historyId},
        #{reservationId},
        #{changeTypeCode},
        #{beforeStatusCode},
        #{afterStatusCode},
        #{changeContent},
        #{changedBy},
        #{changedDateTime},
        #{traceId}
    )
</insert>
```

### 198.11 영향을 받은 행 수

다음 조건을 검사합니다.

```
등록
1행

수정
1행

취소
1행

이력
1행
```

수정 영향 건수가 0이면 다음을 구분해야 합니다.

```
데이터 없음

상태 변경됨

Version 충돌

권한조건 불일치
```

업무 메시지를 정확하게 구분하려면 Update 전 조회결과를 사용하거나 실패 후 재조회할 수 있습니다.

### 198.12 Index 설계

후보:

```
PK
RESERVATION_ID

조회 Index
CUSTOMER_NO, RESERVATION_DTM

지점 조회
BRANCH_ID, RESERVATION_DTM

중복 검사용
CUSTOMER_NO, RESERVATION_DTM, STATUS_CD
```

실제 데이터 분포와 실행계획을 기준으로 결정합니다.

### 제198장 요약

```
DAO는 Mapper 호출과 데이터 접근 추상화를 담당한다.

수정·취소 SQL에는 상태와 Version 조건을 함께 사용한다.

목록 SQL은 기간·권한·Paging을 적용하고
상세 데이터는 상세조회에서만 반환한다.
```

## 제199장. 보안·동시성·Idempotency·감사 구현

### 199.1 인증정보 사용

금지:

```
String userId =
    request.userId();

String branchId =
    request.branchId();
```

권장:

```
String userId =
    context.getUserId();

String branchId =
    context.getBranchId();
```

### 199.2 기능권한과 데이터권한

```
STF
ServiceId 기능권한 확인

Service·Rule
예약 데이터 변경권한 확인

SQL
지점 범위 조회조건 적용
```

### 199.3 데이터권한 Rule

```
public void validateWritable(
        ReservationData reservation,
        TransactionContext context) {

    boolean owner =
        reservation.ownerUserId()
            .equals(context.getUserId());

    boolean sameBranch =
        reservation.branchId()
            .equals(context.getBranchId());

    boolean admin =
        context.hasRole(
            "CT_RESERVATION_ADMIN"
        );

    if (!admin && !(owner && sameBranch)) {
        throw new BusinessException(
            "E-CT-RSV-0005",
            "예약 변경권한이 없습니다."
        );
    }
}
```

실제 정책이 동일 지점 사용자 전체 변경 허용이라면 조건을 조정합니다.

### 199.4 낙관적 동시성

사용자 A와 B가 동시에 Version 3을 조회했다고 가정합니다.

```
사용자 A
Version 3으로 수정
→ Version 4 성공

사용자 B
Version 3으로 수정
→ 영향 건수 0
→ 동시성 오류
```

마지막 저장이 무조건 덮어쓰는 문제를 방지합니다.

### 199.5 Idempotency 상태

```
RECEIVED

PROCESSING

SUCCESS

FAIL

TIMEOUT

UNKNOWN
```

등록 요청이 같은 Key로 다시 들어오면 기존 결과를 반환합니다.

### 199.6 Idempotency Key 구성

Key는 Client가 생성하거나 서버가 발급할 수 있습니다.

저장정보:

```
Idempotency Key

ServiceId

사용자

요청 Hash

처리상태

예약 ID

결과코드

생성시각

만료시각
```

같은 Key에 다른 요청내용이 들어오면 오류로 처리합니다.

### 199.7 Timeout 후 처리

등록 요청이 Timeout됐지만 DB Commit이 완료될 수 있습니다.

화면은 즉시 재등록하지 않고 다음을 수행합니다.

```
Idempotency 결과조회

또는

예약조회
```

### 199.8 History와 감사로그 차이

```
History
업무 데이터의 변경이력

감사로그
누가 어떤 권한으로 어떤 작업을 수행했는지 기록
```

두 기록은 목적이 다릅니다.

### 199.9 개인정보

고객번호와 메모는 민감정보가 될 수 있습니다.

```
목록에서 고객번호 마스킹

메모 전체 로그 금지

상세조회 감사

파일 Export 제한

History 보관기간 정의
```

### 199.10 SQL Injection 방지

금지:

```
ORDER BY ${sortColumn}
```

권장:

```
허용 정렬 필드를 서버에서 Enum으로 변환

또는

고정 정렬
```

### 제199장 요약

```
인증된 사용자와 지점정보는 Context에서 사용한다.

기능권한·데이터권한·SQL 범위조건을 계층별로 적용한다.

Version은 동시성을 통제하고,
Idempotency는 중복 요청을 통제한다.
```

## 제200장. 정상·오류·Timeout 처리 흐름

### 200.1 정상 목록조회

```
화면 조회

→ STF 인증·권한

→ Handler

→ Facade Readonly Transaction

→ 기간·Paging Rule

→ Count SQL

→ List SQL

→ Response

→ ETF 성공
```

### 200.2 정상 등록

```
등록 요청

→ Idempotency 확인

→ 고객 검증

→ Rule 검증

→ ID 생성

→ Master Insert

→ History Insert

→ Commit

→ Idempotency SUCCESS

→ ETF 성공
```

### 200.3 등록 중 History 실패

```
Master Insert 성공

→ History Insert 실패

→ 예외 발생

→ Transaction Rollback

→ Master도 미반영

→ ETF 시스템 오류
```

History가 필수라면 예외를 무시하지 않습니다.

### 200.4 수정 동시성 충돌

```
현재 Version 4

화면 Request Version 3

→ Update 영향 건수 0

→ E-CT-RSV-0006

→ Rollback

→ 화면 재조회 안내
```

### 200.5 취소 상태 오류

```
현재 상태 COMPLETED

→ 취소 요청

→ Rule 오류

→ E-CT-RSV-0004

→ DB 미변경
```

### 200.6 권한 오류

```
기능권한 없음
→ STF 차단
→ Handler 미실행

데이터권한 없음
→ Service·Rule 차단
→ DB 변경 미실행
```

### 200.7 Timeout

```
TCF Timeout 발생

→ ETF TIMEOUT

→ 변경 결과 UNKNOWN 가능

→ Idempotency·DB 결과 확인

→ 중복 재요청 방지
```

### 200.8 DB Pool 장애

```
Connection 획득 실패

→ DataAccessException

→ SystemException 변환

→ ETF 시스템 오류

→ DB Pool Metric 경보
```

### 200.9 외부 고객조회 Timeout

등록 전 고객조회가 실패한 경우:

```
Master Insert 전 실패

→ DB 변경 없음

→ 외부 Timeout 오류

→ 재시도 정책에 따라 재호출 가능
```

### 200.10 표준 오류응답

```
{
  "result": {
    "resultStatus": "FAIL",
    "resultCode": "E-CT-RSV-0006",
    "resultMessage": "다른 사용자가 먼저 변경했습니다."
  },
  "body": null
}
```

내부 Stack Trace는 응답에 포함하지 않습니다.

### 제200장 요약

```
정상 처리뿐 아니라 업무 오류·시스템 오류·Timeout을 설계한다.

Master와 History는 동일 Transaction으로 처리한다.

Timeout 후 변경 결과가 불명확하면 Idempotency와 DB 상태를 확인한다.
```

## 제201장. 단위·Mapper·통합·E2E 테스트

### 201.1 테스트 계층

```
Rule Unit Test

Service Unit Test

Mapper Test

Transaction Integration Test

Security Test

E2E Test

Performance Test
```

### 201.2 Rule 테스트

필수 시나리오:

```
조회기간 정상

조회기간 3개월 초과

페이지 크기 100 초과

READY 수정 가능

COMPLETED 수정 불가

Version 일치

Version 불일치

권한 있음

권한 없음
```

### 201.3 Service 등록 테스트

Mock 대상:

```
CustomerClient

DAO

ID Generator

Clock
```

검증:

```
고객검증 호출

Master Insert 1회

History Insert 1회

응답 예약 ID

인증 사용자 사용
```

### 201.4 등록 Rollback 통합 테스트

```
Master Insert 성공

History Insert 강제 실패

→ 전체 예외

→ Master 조회 0건

→ History 조회 0건
```

### 201.5 수정 동시성 테스트

```
Version 1 데이터 준비

첫 번째 수정 성공
→ Version 2

두 번째 Version 1 수정
→ 동시성 오류
```

### 201.6 Mapper 테스트

| SQL | 테스트 |
| --- | --- |
| 목록 | 기간·지점·Paging |
| 상세 | 존재·미존재 |
| 중복 | 동일 고객·시간 |
| Insert | 감사값 |
| Update | 상태·Version 조건 |
| Cancel | READY만 가능 |
| History | 변경유형·Trace |

### 201.7 권한 테스트

```
일반 사용자 같은 지점 조회 성공

다른 지점 조회 제한

담당자 수정 성공

다른 사용자 수정 실패

관리자 수정 성공
```

### 201.8 E2E 정상 등록

```
JWT 발급

→ CT.Reservation.create 호출

→ SUCCESS

→ 예약 ID 반환

→ Master 확인

→ History 확인

→ 감사로그 확인
```

### 201.9 E2E Timeout

인위적 SQL 또는 Client 지연을 사용해 검증합니다.

```
Timeout 응답

Thread 정리

Transaction 결과

Idempotency 상태

재요청 결과
```

### 201.10 성능 테스트

목록조회:

```
데이터 100만 건 조건

고객별 분포

조회기간 3개월

페이지 크기 100

동시 사용자 부하

p95 3초 이하
```

등록:

```
동시 등록

중복 검증

Index 경합

History Insert

p95 2초 이하
```

### 201.11 테스트 ID

| Test ID | 대상 |
| --- | --- |
| CT-RSV-UT-001 | 기간 Rule |
| CT-RSV-UT-010 | 상태 Rule |
| CT-RSV-ST-001 | 등록 Service |
| CT-RSV-MT-001 | 목록 Mapper |
| CT-RSV-IT-001 | 등록 Rollback |
| CT-RSV-IT-002 | 동시성 |
| CT-RSV-E2E-001 | 정상 등록 |
| CT-RSV-E2E-010 | 권한 없음 |
| CT-RSV-PERF-001 | 목록 성능 |

### 제201장 요약

```
CRUD 테스트는 정상 등록·조회만 확인하지 않는다.

Rollback·동시성·권한·Timeout·중복을 반드시 검증한다.

Mapper와 실제 DB 문법도 별도로 테스트한다.
```

## 제202장. OM·모니터링·배포 준비

### 202.1 Service Catalog 등록

| ServiceId | 상태 | Timeout | 권한 |
| --- | --- | --- | --- |
| CT.Reservation.selectList | ACTIVE | 3000 | VIEW |
| CT.Reservation.selectDetail | ACTIVE | 2000 | VIEW |
| CT.Reservation.create | ACTIVE | 5000 | CREATE |
| CT.Reservation.update | ACTIVE | 5000 | UPDATE |
| CT.Reservation.cancel | ACTIVE | 5000 | CANCEL |

### 202.2 Metric

```
ServiceId별 호출건수

성공·업무 오류·시스템 오류

평균·p95·p99 처리시간

DB Connection 대기시간

SQL 수행시간

조회 건수

동시성 충돌 건수

중복 요청 건수
```

### 202.3 Alert

| 경보 | 조건 |
| --- | --- |
| 목록 p95 | 3초 초과 5분 |
| 등록 실패율 | 1% 초과 |
| 동시성 충돌 급증 | 기준 대비 증가 |
| DB Pending | 1분 지속 |
| Slow SQL | 2초 초과 |
| History 실패 | 1건 이상 Critical |

### 202.4 Runbook

#### 목록조회 지연

```
ServiceId p95 확인

→ DB Pool 확인

→ CT-RSV-SEL-002 SQL 확인

→ 조회건수·기간 확인

→ 실행계획 확인

→ 필요 시 Rate Limit
```

#### 등록 실패

```
오류코드 확인

→ 고객 Client 확인

→ Master·History Transaction 확인

→ Idempotency 상태 확인

→ 미확정 예약 조회
```

### 202.5 배포 전 확인

```
ServiceId 중복 없음

OM 등록 완료

권한코드 등록

Mapper XML 포함

DB Migration 적용

Index 생성

테스트 성공

설계서·RTM 반영

Rollback Script

Smoke Test 준비
```

### 202.6 DB Migration 순서

하위 호환 방식:

```
1. 신규 Table·Column·Index 배포

2. 기존 Version과 호환 확인

3. 신규 WAR 배포

4. Smoke Test

5. 기능 활성화
```

### 202.7 Smoke Test

```
목록조회

상세조회

등록

수정

취소

권한 없음

동시성 충돌

History 확인
```

운영에서는 실제 고객정보 대신 승인된 테스트 데이터를 사용합니다.

### 202.8 Rollback

WAR Rollback 시 확인:

```
신규 DB 구조가 구 WAR와 호환되는가?

신규 상태값을 구 WAR가 처리할 수 있는가?

신규 컬럼이 Null 허용인가?

신규 ServiceId를 OM에서 중지할 수 있는가?

신규 등록 데이터는 유지 가능한가?
```

### 제202장 요약

```
소스 개발이 끝나도 OM·권한·Metric·Runbook이 없으면 운영 준비가 완료된 것이 아니다.

DB 변경은 구·신 WAR 호환성을 고려해 먼저 확장 방식으로 배포한다.
```

## 제203장. 통합 추적성·산출물 완성

### 203.1 요구사항 추적성

| 요구사항 | 화면 | ServiceId | 프로그램 | SQL | Test |
| --- | --- | --- | --- | --- | --- |
| REQ-CT-RSV-001 | CT-RSV-0001 | selectList | Service | SEL-001·002 | E2E-020 |
| REQ-CT-RSV-003 | CT-RSV-0001 | create | Service | INS-001·002 | E2E-001 |
| REQ-CT-RSV-004 | CT-RSV-0001 | update | Service | UPD-001·INS-002 | IT-002 |
| REQ-CT-RSV-005 | CT-RSV-0001 | cancel | Service | UPD-002·INS-002 | E2E-030 |

### 203.2 화면 이벤트 추적성

```
CT-RSV-0001-E04

→ CT.Reservation.create

→ CtReservationHandler

→ CtReservationFacade.create

→ CtReservationService.create

→ CT-RSV-INS-001

→ CT_CONTACT_RESERVATION

→ CT-RSV-INS-002

→ CT_CONTACT_RESERVATION_HISTORY
```

### 203.3 운영 추적성

```
CT.Reservation.create

→ CT-REG-0101

→ CT_RESERVATION_CREATE

→ Timeout 5초

→ Metric ct.reservation.create.duration

→ Alert ALT-CT-RSV-002

→ Runbook RB-CT-RSV-002
```

### 203.4 필수 산출물

```
요구사항 정의서

화면 설계서

화면 이벤트 정의서

거래 설계서

프로그램 설계서

SQL 설계서

DB 설계서

테스트 시나리오

OM 등록표

배포·Rollback 계획

통합 추적성 매트릭스
```

### 203.5 Source와 산출물 자동비교

자동 확인:

```
ServiceId 상수와 Catalog

Handler 등록 ServiceId

Class와 Package

Mapper Interface와 XML

SQL ID와 SQL Registry

DB 컬럼과 DTO

Test ID와 요구사항

Timeout 설정과 OM 정책
```

### 203.6 완료 상태

```
PLANNED

→ DESIGNED

→ IMPLEMENTED

→ TESTED

→ DEPLOY_READY

→ DEPLOYED

→ OPERATED
```

### 제203장 요약

```
하나의 CRUD 기능은 화면·ServiceId·프로그램·SQL·테스트·운영으로 연결되어야 한다.

Source와 산출물의 구조적 정보는 자동으로 비교한다.
```

## 제204장. AI 코딩도구를 이용한 CRUD 구현

### 204.1 AI에 제공할 입력정보

```
업무코드와 도메인

화면 항목

ServiceId

테이블·컬럼

업무규칙

상태전이

권한

Timeout

Package 규칙

기존 샘플 프로그램
```

### 204.2 좋은 요청 예

```
NSIGHT TCF 표준으로 고객 상담예약 등록 기능을 구현한다.

ServiceId:
CT.Reservation.create

계층:
Handler → Facade → Service → Rule → DAO → Mapper

규칙:
- 고객 존재 확인
- 상태는 READY로 서버 설정
- 사용자와 지점은 TransactionContext 사용
- Master와 History 동일 트랜잭션
- 등록은 Idempotency 적용
- Mapper XML은 MyBatis 사용
- 영향 건수 1건 검증

Request와 DB DTO는 분리한다.
```

### 204.3 AI가 누락하기 쉬운 항목

```
ServiceId OM 등록

기능권한

데이터권한

Transaction Proxy

History Rollback

Version 조건

Idempotency

Timeout

Metric

테스트

문서 추적성
```

### 204.4 AI 생성코드 검토순서

```
Package

→ 의존성 방향

→ DTO 책임

→ 사용자정보 출처

→ Transaction

→ Rule

→ SQL 조건

→ 영향 건수

→ 예외

→ Test

→ OM
```

### 204.5 금지 방식

```
테이블만 제공하고 전체 업무규칙을 AI가 추정하게 함

운영 DB 계정과 고객 데이터를 입력

AI가 생성한 SQL을 실행계획 없이 운영 반영

테스트 없이 생성코드를 Merge

기존 Framework 구조를 무시한 독자 Controller 생성
```

### 제204장 요약

```
AI는 CRUD 초안 생성에 유용하지만 업무규칙과 책임경계를 대신 결정하지 않는다.

생성코드는 Transaction·권한·동시성·SQL·운영 기준으로 검증한다.
```

## 3. 문제 정의 및 설계 배경

화면과 테이블만 주어진 개발에서 발생하는 근본 문제는 다음과 같습니다.

```
업무규칙이 프로그램에 숨는다.

화면 이벤트와 거래 경계가 불명확하다.

테이블 구조가 외부 API 계약으로 직접 노출된다.

등록·수정·취소의 권한과 상태전이가 누락된다.

트랜잭션과 이력의 원자성이 보장되지 않는다.

운영정책이 개발 완료 후 추가된다.
```

목표는 화면과 테이블을 단순 CRUD로 연결하는 것이 아닙니다.

```
화면 요구

→ 업무 의미

→ 독립 거래

→ 명확한 계층 책임

→ 안전한 데이터 처리

→ 검증 가능한 운영 기능
```

으로 전환해야 합니다.

## 4. 현행 구조와 문제점

| 비표준 구조 | 문제 |
| --- | --- |
| 화면별 Controller | 공통 TCF 우회 |
| 하나의 save 거래 | 등록·수정 혼합 |
| Request를 Mapper에 전달 | 외부·DB 계약 결합 |
| 물리 Delete | 감사·복구 어려움 |
| Version 없음 | 동시 수정 유실 |
| 화면 사용자 ID 사용 | 위변조 위험 |
| History 별도 Transaction | 이력 불일치 |
| 무제한 목록조회 | 성능 장애 |
| OM 등록 후순위 | 운영 차단 |
| 정상 테스트만 존재 | 오류 대응 미검증 |

## 5. 요구사항과 제약조건

### 5.1 기능 요구사항

```
예약 목록조회

예약 상세조회

예약 등록

예약 수정

예약 취소

변경이력 저장
```

### 5.2 비기능 요구사항

```
목록 p95 3초

변경 p95 2초

한 WAS 장애 시 지속

개인정보 상세조회 감사

동시성 충돌 통제

중복 등록 방지
```

### 5.3 제약조건

```
TCF ServiceId 구조 사용

MyBatis 사용

Oracle 사용

WAR 배포

Redis 미사용 가능

외부 고객정보는 IC 도메인 소유
```

## 6. 설계 원칙

```
서비스 목적별 ServiceId 분리

업무 데이터 소유권 준수

Context 신뢰정보 사용

DTO 역할 분리

Facade Transaction

Version 동시성 통제

논리 삭제 우선

Master·History 원자성

목록조회 제한

운영관측 기본 적용
```

## 7. 대안 비교 및 의사결정

### 7.1 등록·수정 ServiceId

| 대안 | 장점 | 단점 |
| --- | --- | --- |
| 하나의 save | 화면 구현 단순 | 규칙·권한 혼합 |
| create·update 분리 | 계약 명확 | ServiceId 증가 |

결정:

```
create와 update를 분리한다.
```

### 7.2 삭제 방식

| 대안 | 장점 | 단점 |
| --- | --- | --- |
| 물리 Delete | 단순 | 감사·복구 어려움 |
| 상태 Cancel | 추적·복구 가능 | 상태관리 필요 |

결정:

```
CANCELED 상태로 논리 취소한다.
```

### 7.3 동시성 방식

| 대안 | 장점 | 단점 |
| --- | --- | --- |
| 마지막 저장 우선 | 단순 | 데이터 유실 |
| 비관적 Lock | 강한 통제 | 대기·Lock |
| Version 낙관적 Lock | 확장성 | 충돌 처리 필요 |

결정:

```
VERSION_NO 기반 낙관적 동시성 제어를 적용한다.
```

### 7.4 고객정보 접근

| 대안 | 장점 | 단점 |
| --- | --- | --- |
| IC 테이블 직접조회 | 빠른 구현 | 소유권 위반 |
| IC ServiceId 호출 | 경계 명확 | 연계비용 |
| CT Read Model | 조회성능 | 동기화 필요 |

기본 결정:

```
IC ServiceId 계약으로 고객 존재 여부를 확인한다.
```

대량조회 성능이 필요한 경우 별도 Read Model을 ADR로 검토합니다.

## 8. 목표 아키텍처

```
[CT-RSV-0001 화면]
          │
          ▼
[POST /ct/online]
          │
          ▼
[tcf-web Controller]
          │
          ▼
[tcf-core STF]
 인증·권한·Timeout·Idempotency
          │
          ▼
[CtReservationHandler]
          │
          ▼
[CtReservationFacade]
      Transaction
          │
          ▼
[CtReservationService]
   ┌──────┼────────┐
   ▼      ▼        ▼
[Rule] [IC Client] [DAO]
                    │
                    ▼
                 [Mapper]
                    │
          ┌─────────┴─────────┐
          ▼                   ▼
[RESERVATION MASTER]   [RESERVATION HISTORY]
                    │
                    ▼
                  [ETF]
                    │
                    ▼
            [StandardResponse]
```

## 9. 표준 형식

### 9.1 ServiceId 형식

```
{업무코드}.{도메인}.{동작}
```

예:

```
CT.Reservation.create
```

### 9.2 프로그램명

```
CtReservationHandler

CtReservationFacade

CtReservationService

CtReservationRule

CtReservationDao

CtReservationMapper
```

### 9.3 SQL ID

```
{업무코드}-{도메인약어}-{유형}-{3자리}
```

예:

```
CT-RSV-SEL-001

CT-RSV-INS-001

CT-RSV-UPD-001
```

## 10. 구성요소 및 속성

| 구성요소 | 주요 속성 |
| --- | --- |
| 화면 | ID·이벤트·상태 |
| ServiceId | 유형·권한·Timeout |
| Handler | ServiceId 분기 |
| Facade | Transaction |
| Service | 업무 흐름 |
| Rule | 검증·상태전이 |
| DAO | 데이터 접근 |
| Mapper | SQL 계약 |
| Master | 현재 상태 |
| History | 변경 기록 |
| Idempotency | 중복 방지 |
| OM | 정책·Metric |
| Test | 수용기준 검증 |

## 11. 책임 경계와 RACI

| 활동 | 업무 | UI | DEV | AA | DA·DBA | QA | OPS |
| --- | --- | --- | --- | --- | --- | --- | --- |
| 요구사항 | A/R | C | C | C | I | C | I |
| 화면 이벤트 | A/C | R | C | C | I | C | I |
| ServiceId | C | C | R | A | I | C | C |
| 프로그램 설계 | I | I | A/R | C | I | C | I |
| DB·SQL | C | I | R | C | A/R | C | C |
| 권한·감사 | A/C | C | R | C | C | C | C |
| 테스트 | C | C | R | C | C | A/R | I |
| OM 등록 | C | I | R/C | C | I | C | A/R |
| 배포 | I | I | C | C | C | C | A/R |

## 12. 정상 처리 흐름

```
1. 요구사항과 화면·테이블 정보를 정리한다.

2. 상태와 업무규칙을 정의한다.

3. 화면 이벤트와 ServiceId를 설계한다.

4. 권한·Timeout·Idempotency를 연결한다.

5. Package와 DTO를 설계한다.

6. Handler와 Facade를 구현한다.

7. Service와 Rule을 구현한다.

8. DAO·Mapper·SQL을 구현한다.

9. Master·History Transaction을 검증한다.

10. 동시성·권한·중복을 테스트한다.

11. OM과 운영정보를 등록한다.

12. 추적성과 배포 증적을 완성한다.
```

## 13. 오류·Timeout·장애 흐름

### 13.1 고객 Client 장애

```
고객 확인 실패

→ Master Insert 전 중단

→ DB 변경 없음

→ 외부연계 오류
```

### 13.2 History 저장 실패

```
Master Insert

→ History 실패

→ 전체 Rollback
```

### 13.3 동시성 충돌

```
Version 불일치

→ Update 0건

→ 업무 오류

→ 화면 재조회
```

### 13.4 Timeout

```
변경 Timeout

→ 결과 UNKNOWN 가능

→ Idempotency·DB 조회

→ 재처리 여부 판단
```

### 13.5 DB Pool 장애

```
Connection 획득 실패

→ System Error

→ Pool 경보

→ ServiceId·SQL 진단
```

## 14. 정상 예시

```
사용자
예약 신규 입력

화면
CT-RSV-0001-E04

ServiceId
CT.Reservation.create

인증
CT_RESERVATION_CREATE 권한

Service
고객 확인·중복 확인

DB
Master 1건
History 1건

ETF
SUCCESS

응답
예약 ID·상태·Version
```

## 15. 금지 예시

### 15.1 하나의 Save ServiceId

```
actionType = CREATE·UPDATE·DELETE
```

서버 내부 분기 하나로 모든 기능을 처리합니다.

### 15.2 Request 사용자정보 신뢰

```
createdBy = request.userId
```

### 15.3 물리 삭제

```
DELETE FROM CT_CONTACT_RESERVATION
```

### 15.4 Version 없는 수정

```
WHERE RESERVATION_ID = #{reservationId}
```

만 사용합니다.

### 15.5 History 실패 무시

```
try {
    insertHistory();
} catch (Exception ignored) {
}
```

### 15.6 목록에서 메모 전체 조회

대량 CLOB·민감정보를 목록에 포함합니다.

### 15.7 SQL 문자열 치환

```
ORDER BY ${sortColumn}
```

### 15.8 OM 미등록 배포

Source에는 ServiceId가 있으나 Catalog에 없습니다.

## 16. 연계 규칙

```
화면 이벤트
→ ServiceId

ServiceId
→ 거래코드·권한·Timeout

Handler
→ Facade

Facade
→ Service Transaction

Service
→ Rule·Client·DAO

DAO
→ Mapper

Mapper
→ SQL

SQL
→ Master·History

Requirement
→ Test

ServiceId
→ OM·Metric·Runbook
```

## 17. 데이터 및 상태관리

### 17.1 예약 상태

```
READY

COMPLETED

CANCELED
```

### 17.2 Version

```
등록
1

수정·취소
현재값 + 1
```

### 17.3 Idempotency

```
RECEIVED

PROCESSING

SUCCESS

FAIL

TIMEOUT

UNKNOWN
```

### 17.4 기능 상태

```
DESIGNED

IMPLEMENTED

TESTED

DEPLOYED

DEPRECATED

RETIRED
```

## 18. 성능·용량·확장성

| 영역 | 기준 |
| --- | --- |
| 조회기간 | 최대 3개월 |
| Page Size | 최대 100 |
| 목록 컬럼 | 최소화 |
| 상세 메모 | 상세조회만 |
| 목록 p95 | 3초 |
| 변경 p95 | 2초 |
| SQL Timeout | 상위 TCF보다 짧게 |
| Index | 고객번호·예약일시 |
| History | 연간 증가량 산정 |
| DB Pool | 전체 WAR 합계 고려 |
| Cache | 코드성 기준정보 중심 |
| Export | 대용량은 Batch |

## 19. 보안·개인정보·감사

```
JWT와 AuthenticationContext를 사용한다.

사용자·지점정보를 Request에서 신뢰하지 않는다.

고객번호를 로그에서 마스킹한다.

메모 전체를 거래로그에 기록하지 않는다.

상세조회와 변경거래를 감사한다.

권한코드와 데이터권한을 분리한다.

운영 DB 직접 수정은 승인과 감사가 필요하다.

History와 감사로그의 보관기간을 정의한다.
```

## 20. 운영·모니터링·장애 대응

필수 운영정보:

```
ServiceId 상태

권한

Timeout

호출량

p95·p99

오류율

동시성 충돌

중복 요청

Slow SQL

History 실패

배포 Version
```

장애진단 순서:

```
화면 오류

→ ServiceId

→ TraceId

→ Handler·Service

→ DB Pool

→ SQL ID

→ Master·History 상태

→ 최근 Release
```

## 21. 자동검증 및 품질 Gate

| Gate | 검증 |
| --- | --- |
| 화면 | 이벤트·ServiceId 연결 |
| 거래 | 권한·Timeout·거래코드 |
| Handler | ServiceId 등록 |
| 계층 | 의존성 규칙 |
| DTO | Request·Data 분리 |
| 보안 | Request 사용자정보 금지 |
| SQL | SELECT *·${} 금지 |
| 수정 | Version 조건 |
| 상태 | 논리 취소 |
| Transaction | Master·History Rollback |
| Test | 정상·오류·Timeout |
| OM | Catalog·Metric·Runbook |
| 추적성 | Requirement부터 DB까지 |

## 22. 테스트 시나리오

| ID | 시나리오 | 기대 결과 |
| --- | --- | --- |
| RSV-001 | 정상 목록조회 | 목록 반환 |
| RSV-002 | 결과 없음 | 빈 목록 |
| RSV-003 | 기간 3개월 초과 | 업무 오류 |
| RSV-004 | Page Size 초과 | 업무 오류 |
| RSV-005 | 정상 상세조회 | 상세 반환 |
| RSV-006 | 예약 없음 | E-CT-RSV-0001 |
| RSV-007 | 정상 등록 | Master·History |
| RSV-008 | 고객 없음 | 등록 실패 |
| RSV-009 | 중복 예약 | 업무 오류 |
| RSV-010 | History 실패 | 전체 Rollback |
| RSV-011 | 정상 수정 | Version 증가 |
| RSV-012 | 완료 예약 수정 | 차단 |
| RSV-013 | 다른 사용자 수정 | 권한 오류 |
| RSV-014 | Version 충돌 | 동시성 오류 |
| RSV-015 | 정상 취소 | CANCELED |
| RSV-016 | 완료 예약 취소 | 차단 |
| RSV-017 | 동일 취소 재요청 | 기존 결과 또는 상태 오류 |
| RSV-018 | 등록 Timeout | 결과 확인 |
| RSV-019 | DB Connection 실패 | 시스템 오류 |
| RSV-020 | 외부 고객 Timeout | DB 변경 없음 |
| RSV-021 | JWT 없음 | 401 |
| RSV-022 | 기능권한 없음 | 403 |
| RSV-023 | 다른 지점 조회 | 정책에 따라 제한 |
| RSV-024 | 개인정보 로그 | 마스킹 |
| RSV-025 | 목록 피크부하 | p95 3초 |

## 23. 제19부 체크리스트

### 23.1 요구사항·업무규칙

| 점검 항목 | 확인 |
| --- | --- |
| CRUD 기능을 독립적으로 분해했는가? | □ |
| 상태와 상태전이를 정의했는가? | □ |
| 수정 가능 필드를 정의했는가? | □ |
| 권한과 데이터권한을 정의했는가? | □ |
| 동시성 정책을 정의했는가? | □ |
| 이력과 감사 요구가 있는가? | □ |

### 23.2 화면·거래

| 점검 항목 | 확인 |
| --- | --- |
| 화면 ID가 있는가? | □ |
| 이벤트 ID가 있는가? | □ |
| ServiceId가 분리되어 있는가? | □ |
| 거래코드가 있는가? | □ |
| 권한코드가 있는가? | □ |
| Timeout이 등록되는가? | □ |
| Idempotency 대상이 정의되는가? | □ |

### 23.3 프로그램·DTO

| 점검 항목 | 확인 |
| --- | --- |
| Handler 책임이 단순한가? | □ |
| Facade가 Transaction을 담당하는가? | □ |
| Service가 업무 흐름을 담당하는가? | □ |
| Rule이 업무조건을 검증하는가? | □ |
| Request와 Data DTO가 분리되는가? | □ |
| Context에서 사용자정보를 사용하는가? | □ |
| 외부 호출 위치가 적절한가? | □ |

### 23.4 SQL·DB

| 점검 항목 | 확인 |
| --- | --- |
| SQL ID가 표준화되었는가? | □ |
| 목록에 Paging이 있는가? | □ |
| 조회기간 제한이 있는가? | □ |
| 수정에 상태·Version 조건이 있는가? | □ |
| 영향 건수 1건을 검사하는가? | □ |
| Master·History가 같은 Transaction인가? | □ |
| Index와 실행계획을 검토했는가? | □ |
| 물리 Delete를 사용하지 않는가? | □ |

### 23.5 보안·안정성

| 점검 항목 | 확인 |
| --- | --- |
| JWT 인증정보를 사용하는가? | □ |
| 기능권한을 적용하는가? | □ |
| 데이터권한을 적용하는가? | □ |
| 개인정보를 마스킹하는가? | □ |
| 등록 중복을 방지하는가? | □ |
| Timeout 후 결과조회가 가능한가? | □ |
| 동시성 충돌 메시지가 명확한가? | □ |

### 23.6 테스트·운영

| 점검 항목 | 확인 |
| --- | --- |
| Rule Unit Test가 있는가? | □ |
| Mapper Test가 있는가? | □ |
| Rollback Test가 있는가? | □ |
| 동시성 Test가 있는가? | □ |
| 권한 Test가 있는가? | □ |
| Timeout Test가 있는가? | □ |
| OM Catalog를 등록했는가? | □ |
| Metric·Alert·Runbook이 있는가? | □ |
| Smoke Test가 준비되었는가? | □ |

### 23.7 추적성·배포

| 점검 항목 | 확인 |
| --- | --- |
| Requirement부터 ServiceId가 연결되는가? | □ |
| ServiceId부터 SQL·DB가 연결되는가? | □ |
| Requirement별 Test가 있는가? | □ |
| DB Migration 순서가 있는가? | □ |
| Rollback이 가능한가? | □ |
| Release Note가 있는가? | □ |
| Source와 산출물 정합성을 검증하는가? | □ |

## 24. 변경·호환성·폐기 관리

### 24.1 Request 필드 추가

선택필드로 추가하고 서버 기본값을 정의합니다.

확인:

```
화면

Validation

Command

DB

테스트

기존 Client
```

### 24.2 Response 필드 추가

기존 필드를 유지하면서 신규 필드를 추가합니다.

Rolling 배포 중 구·신 WAR 혼재를 검증합니다.

### 24.3 상태값 추가

예:

```
NO_SHOW
```

영향:

```
DB 코드

Rule

화면 표시

조회조건

상태전이

통계

Batch

테스트
```

### 24.4 Version 정책 변경

Version 컬럼을 제거하거나 우회하면 동시성 통제가 약화됩니다.

구·신 프로그램의 혼재 기간을 고려해야 합니다.

### 24.5 ServiceId 폐기

```
Deprecated 등록

→ 호출자 전환

→ OM 신규 호출 차단

→ 호출량 0 확인

→ Handler 제거

→ 문서·Test 폐기
```

### 24.6 물리 데이터 폐기

취소된 예약도 감사와 업무분쟁을 위해 일정 기간 보관할 수 있습니다.

보관기간 종료 후 별도 폐기 Batch를 사용합니다.

## 25. 시사점

### 25.1 핵심 아키텍처 판단

제19부의 핵심은 다음과 같습니다.

```
CRUD 개발
= 테이블별 등록·조회·수정·삭제 SQL을 만든다
```

가 아닙니다.

```
CRUD 개발
= 업무 상태·권한·동시성·이력·운영정책을
  ServiceId와 계층별 책임으로 구현한다
```

입니다.

### 25.2 주요 위험

| 위험 | 영향 |
| --- | --- |
| 화면·테이블만 보고 구현 | 업무규칙 누락 |
| 하나의 Save 거래 | 책임 혼합 |
| Request 사용자정보 사용 | 보안 위변조 |
| 물리 삭제 | 감사·복구 불가 |
| Version 미적용 | 동시 수정 유실 |
| History 별도 처리 | 데이터 불일치 |
| 외부 호출을 Tx 안에서 수행 | Pool 장기 점유 |
| 목록 무제한 조회 | 성능 장애 |
| SQL 영향 건수 미검사 | 오류 은폐 |
| Idempotency 없음 | 중복 등록 |
| OM 미등록 | 운영 차단 |
| 정상 테스트만 수행 | 장애 대응 미검증 |

### 25.3 우선 보완 과제

```
1. 화면 이벤트–ServiceId 매핑

2. CRUD ServiceId 분리

3. Request·Command·Data·Response DTO 분리

4. Context 기반 사용자·지점 처리

5. Version 낙관적 동시성

6. Master·History 동일 Transaction

7. 조회기간·Paging 제한

8. 등록 Idempotency

9. 권한·Timeout·감사정책

10. Rollback·동시성·Timeout 테스트

11. OM·Metric·Runbook 등록

12. 통합 추적성 자동검증
```

### 25.4 중장기 발전 방향

```
수작업 CRUD 설계
→ 표준 Metadata 기반 생성

수동 ServiceId 등록
→ 중앙 Registry 자동 발급

수동 DTO 작성
→ Schema 기반 Generator

수동 Mapper 작성
→ SQL Template·검증

사후 권한 적용
→ 정책 기반 자동 주입

수동 테스트
→ 설계 Metadata 기반 Test 생성

문서와 코드 분리
→ 설계–코드 동시 생성

단순 AI 코드생성
→ Architecture Gate 결합 AI 개발
```

자동생성을 확대하더라도 업무규칙과 상태전이, 데이터 소유권은 사람이 승인해야 합니다.

## 26. 마무리말

제19부에서 가장 중요하게 기억해야 할 신규 CRUD 개발 순서는 다음과 같습니다.

```
화면·테이블 정보 수집

→ 요구사항 구조화

→ 업무상태·규칙 정의

→ CRUD 기능 분해

→ 화면 이벤트

→ ServiceId

→ 거래코드·권한·Timeout

→ Package·DTO

→ Handler

→ Facade

→ Service·Rule

→ DAO·Mapper·SQL

→ Master·History Transaction

→ 보안·동시성·Idempotency

→ 테스트

→ OM·배포

→ 통합 추적성
```

등록 거래는 다음 흐름을 따릅니다.

```
STF 인증·권한·중복검사

→ Handler

→ Facade Transaction

→ 고객 검증

→ 업무 Rule

→ Master Insert

→ History Insert

→ Commit

→ ETF

→ 표준 응답
```

수정 거래는 다음 흐름을 따릅니다.

```
현재 데이터 조회

→ 데이터권한

→ 상태 확인

→ Version 확인

→ Update with Version

→ 영향 건수 확인

→ History

→ Commit
```

초보 개발자가 화면과 테이블 정보로 신규 CRUD 구현을 완료하기 전에 마지막으로 확인해야 할 질문은 다음과 같습니다.

```
화면 이벤트별 업무 목적이 명확한가?

등록·수정·취소 ServiceId를 분리했는가?

업무상태와 상태전이를 정의했는가?

수정 가능 필드와 불가 필드를 구분했는가?

화면 사용자정보를 신뢰하지 않는가?

Request와 DB DTO를 분리했는가?

Facade가 트랜잭션 경계를 담당하는가?

외부 호출이 DB Connection을 장기 점유하지 않는가?

Master와 History가 동일 Transaction인가?

수정·취소 SQL에 상태와 Version 조건이 있는가?

영향 건수를 정확히 확인하는가?

목록조회에 기간·Paging·최대건수 제한이 있는가?

등록 재요청을 Idempotency로 통제하는가?

Timeout 후 실제 처리결과를 확인할 수 있는가?

기능권한과 데이터권한을 모두 적용했는가?

개인정보가 로그와 목록에 과도하게 노출되지 않는가?

Rollback·동시성·권한·Timeout 테스트가 있는가?

ServiceId와 권한·Timeout이 OM에 등록되었는가?

요구사항부터 화면·프로그램·SQL·테스트까지 추적되는가?

DB Migration과 WAR Rollback의 호환성을 확인했는가?
```

이 질문에 답할 수 있다면 화면과 테이블 정보만 제공된 상황에서도 단순 SQL 중심 CRUD가 아니라, NSIGHT TCF의 거래표준·보안·운영·품질 기준을 만족하는 업무기능을 처음부터 끝까지 설계하고 구현할 수 있습니다.

