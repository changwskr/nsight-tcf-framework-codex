<!-- source: ztcf-집필본/NSIGHT TCF Chapter 부록 B. Naming Conventions and Identifier Guidelines.docx -->
<!-- tables: mammoth-html + placeholder table-to-md -->

# 확장 부록 B. 식별자와 명명규칙 작업표

## 도입 전 안내말

이름은 보기 좋은 문자열이 아니다.

NSIGHT TCF에서 이름은 다음 요소를 연결하는 공통 Key다.

\`\`\`text id=“namB001” 요구사항

→ 업무코드·도메인

→ 화면·이벤트

→ ServiceId·거래코드

→ Handler·Facade·Service

→ DAO·Mapper·SQL

→ DB 객체

→ 권한·Timeout·오류코드

→ 로그·Metric·배포



개발 전에 아래 질문에 답하지 못한다면 코드를 먼저 작성해서는 안 된다.

\`\`\`text id="namB002"
이 기능은 어느 업무가 소유하는가?

어느 화면과 사용자 이벤트에서 시작되는가?

어떤 ServiceId로 실행되는가?

어느 Handler와 프로그램이 처리하는가?

어떤 SQL과 DB 객체를 사용하는가?

운영에서는 어떤 거래코드로 식별하는가?

오류·권한·Timeout은 어디에 등록하는가?

배포 후 어떤 로그와 Metric으로 찾는가?

명명규칙의 목적은 이름을 예쁘게 만드는 것이 아니다.

\`\`\`text id=“namB003” 중복 방지

책임 경계 식별

검색 가능성

정방향 추적성

역방향 영향 분석

운영 통제

변경·폐기 관리



를 확보하는 것이다.

NSIGHT TCF의 명명체계는 업무코드에서 시작해 Context·WAR·Package·ServiceId·프로그램·SQL·DB·운영로그까지 하나의 추적 사슬을 형성해야 한다.

\---

\# 문서 개요

\## 목적

본 부록의 목적은 신규 거래를 설계하기 전에 업무코드·화면 ID·ServiceId·거래코드·프로그램·DB 객체·오류코드·운영정보를 한 번에 정의하고, 설계서·소스·설정·로그의 이름을 일치시키는 데 있다.

세부 목적은 다음과 같다.

\`\`\`text id="namB004"
업무 소유권과 배포단위 식별

화면과 사용자 이벤트 표준화

ServiceId 중복과 오등록 방지

거래코드·권한·Timeout 운영 연계

패키지·클래스·메서드 책임 명확화

DTO 역할 구분

Mapper Method와 SQL ID 정합성 확보

DB 객체 소유영역 식별

오류·로그·감사·Metric 표준화

Gateway·Batch·Cache·파일 식별자 통일

자동검증·품질 Gate 적용

변경·호환·폐기 절차 수립

## 적용범위

| 영역 | 적용 대상 |
| --- | --- |
| 업무 분류 | 업무코드·업무세구분코드·도메인 |
| 실행 구조 | Context·WAR·Gradle Module·Package |
| UI | 화면 ID·이벤트 ID·기능 ID·메뉴 ID |
| 거래 | ServiceId·거래코드·GUID·TraceId |
| Java | Package·Class·Method·Field·Constant |
| DTO | Request·Response·Query·Command·Row·Event |
| MyBatis | Mapper·Namespace·SQL ID |
| DB | Table·Column·Constraint·Index·Sequence·View |
| 오류 | 오류코드·메시지 Key |
| 운영 | 로그·감사·Metric·Dashboard·Alert |
| 연계 | Client·Route·Topic·Event |
| Batch | Job·Step·Scheduler |
| Cache | Cache Name·Key |
| 파일 | 업로드·다운로드·Migration·설정 파일 |
| 배포 | Artifact·Context·Profile·Release |
| 품질 | 자동검증·중복검사·추적성 Gate |

## 대상 독자

\`\`\`text id=“namB005” 업무 분석가

화면 설계자

업무 개발자

프레임워크 개발자

DB 설계자·DBA

아키텍트

운영·OM 담당자

테스트·품질 담당자

DevOps 담당자

PMO


\## 선행조건

\`\`\`text id="namB006"
공식 업무코드 대장

업무세구분·도메인 정의

화면 관리대장

ServiceId 관리대장

거래코드 분류체계

BASE Package

DB 명명표준

오류코드 관리대장

OM Service Catalog

CI/CD 자동검증 도구

# 핵심 관점

\`\`\`text id=“namB007” 한 거래의 이름은 각 문서와 프로그램에서 따로 정하지 않는다.

업무코드에서 출발해 화면·ServiceId·프로그램·SQL·DB·운영정보를 하나의 작업표에서 함께 결정해야 한다.


\---

\# B.1 통합 식별자 작업표

\## B.1.1 신규 거래 착수용 Master 작업표

신규 개발을 시작하기 전에 다음 표를 한 행 이상 완성한다.

| 분류 | 항목 | 작성 규칙 | 상담예약 예 | 확인 위치 |
|---|---|---|---|---|
| 요구사항 | 요구사항 ID | \`REQ-{BC}-{SUB}-{NNN}\` | \`REQ-CT-RSV-001\` | 요구사항 정의서 |
| 업무 | 업무코드 | 대문자 2자리 | \`CT\` | 업무코드 대장 |
| 업무 | 업무세구분 | 대문자 2~5자리 | \`RSV\` | 기능 분류표 |
| 업무 | 도메인 | PascalCase 명사 | \`Reservation\` | 도메인 정의서 |
| 배포 | Context | \`/{bc.lower}\` | \`/ct\` | Tomcat·Apache |
| 배포 | WAR | \`{bc.lower}.war\` | \`ct.war\` | Gradle·배포 |
| 배포 | Module | \`{bc.lower}-service\` | \`ct-service\` | \`settings.gradle\` |
| UI | 화면 ID | \`{BC}-{SUB}-{NNNN}\` | \`CT-RSV-0001\` | 화면 관리대장 |
| UI | 이벤트 ID | \`{화면ID}-E{NN}\` | \`CT-RSV-0001-E04\` | 화면 설계서 |
| UI | 기능 ID | \`{화면ID}-F{NN}\` | \`CT-RSV-0001-F02\` | 기능권한 |
| UI | 메뉴 ID | \`MNU-{BC}-{SUB}-{NNNN}\` | \`MNU-CT-RSV-0001\` | 메뉴 관리 |
| 거래 | ServiceId | \`{BC}.{Domain}.{action}\` | \`CT.Reservation.create\` | Handler·OM |
| 거래 | 거래코드 | \`{BC}-{TYPE}-{NNNN}\` | \`CT-REG-0101\` | OM·거래로그 |
| 보안 | 권한코드 | \`{BC}\_{DOMAIN}\_{ACTION}\` | \`CT\_RESERVATION\_CREATE\` | 권한 관리 |
| 운영 | Timeout | ServiceId별 ms | \`5000ms\` | OM Timeout |
| 프로그램 | Package | BASE+업무+도메인+계층 | \`.ct.reservation\` | 소스 |
| 프로그램 | Handler | \`{Bc}{Domain}Handler\` | \`CtReservationHandler\` | Java |
| 프로그램 | Facade | \`{Bc}{Domain}Facade\` | \`CtReservationFacade\` | Java |
| 프로그램 | Service | \`{Bc}{Domain}Service\` | \`CtReservationService\` | Java |
| 프로그램 | Rule | \`{Bc}{Domain}Rule\` | \`CtReservationRule\` | Java |
| 프로그램 | DAO | \`{Bc}{Domain}Dao\` | \`CtReservationDao\` | Java |
| 프로그램 | Mapper | \`{Bc}{Domain}Mapper\` | \`CtReservationMapper\` | Java·XML |
| 데이터 | Mapper SQL ID | lowerCamelCase | \`insertReservation\` | Mapper XML |
| 데이터 | 관리 SQL ID | \`{BC}-{SUB}-{OP}-{NNN}\` | \`CT-RSV-INS-001\` | SQL 설계서 |
| 데이터 | Table | \`{BC}\_{업무명}\` | \`CT\_CONTACT\_RESERVATION\` | DB 정의서 |
| 데이터 | History | \`{Table}\_HISTORY\` | \`CT\_CONTACT\_RESERVATION\_HISTORY\` | DB 정의서 |
| 오류 | 오류코드 | \`E-{BC}-{SUB}-{NNNN}\` | \`E-CT-RSV-0008\` | 오류코드 대장 |
| 로그 | Metric Prefix | \`{bc}.{domain}\` | \`ct.reservation\` | Metric 규격 |
| 테스트 | 테스트 ID | \`TC-{BC}-{SUB}-{NNN}\` | \`TC-CT-RSV-015\` | 테스트 결과 |
| 배포 | Migration | \`V{DATE}\_{NN}\_\_{desc}.sql\` | \`V20260718\_01\_\_create\_ct\_reservation.sql\` | DB 배포 |
| 운영 | Runbook ID | \`RB-{BC}-{SUB}-{NNN}\` | \`RB-CT-RSV-001\` | 운영 Runbook |

\---

\## B.1.2 완료 기준

다음 정방향 추적이 가능해야 한다.

\`\`\`text id="namB008"
REQ-CT-RSV-003

→ CT-RSV-0001-E04

→ CT.Reservation.create

→ CT-REG-0101

→ CtReservationHandler

→ CtReservationFacade.create

→ CtReservationService.create

→ CtReservationDao.insertReservation

→ CtReservationMapper.insertReservation

→ CT-RSV-INS-001

→ CT\_CONTACT\_RESERVATION

→ TC-CT-RSV-015

다음 역방향 영향 분석도 가능해야 한다.

\`\`\`text id=“namB009” CT\_CONTACT\_RESERVATION 변경

→ CT-RSV-SEL-001

→ selectReservationList

→ CtReservationMapper

→ CT.Reservation.selectList

→ CT-RSV-0001-E02

→ CT-RSV-0001 화면

→ REQ-CT-RSV-001


\---

\# B.2 최상위 명명 원칙

\## B.2.1 표기 원칙

| 대상 | 표기 | 예 |
|---|---|---|
| 업무코드 | 대문자 | \`CT\` |
| 업무세구분 | 대문자 | \`RSV\` |
| Java Class | PascalCase | \`CtReservationService\` |
| Java Method·Field | lowerCamelCase | \`selectReservationList\` |
| Java Constant | UPPER\_SNAKE\_CASE | \`DEFAULT\_PAGE\_SIZE\` |
| Java Package | 소문자 | \`ct.reservation.service\` |
| JSON | lowerCamelCase | \`reservationId\` |
| DB 객체 | UPPER\_SNAKE\_CASE | \`RESERVATION\_ID\` |
| HTML·JS·파일 | kebab-case | \`reservation-list.js\` |
| Metric | 소문자 점 구분 | \`ct.reservation.create.count\` |
| 환경변수 | UPPER\_SNAKE\_CASE | \`CT\_DB\_PASSWORD\` |

\## B.2.2 이름 작성 순서

\`\`\`text id="namB010"
소유 영역

→ 업무 대상

→ 역할·행위

→ 필요 시 유형·순번

예:

\`\`\`text id=“namB011” Ct + Reservation + Service

\= CtReservationService


\`\`\`text id="namB012"
CT
\+ Reservation
\+ create

\= CT.Reservation.create

## B.2.3 의미 없는 축약 금지

금지:

\`\`\`text id=“namB013” Svc

Mgr

Proc

Ctrl2

UserInfo2

CommonUtil

EtcService

doWork

processData

sql001



예외:

\`\`\`text id="namB014"
프로젝트에서 공식 승인된 업무코드

국제·기술 표준 약어

HTTP

JWT

DTO

DAO

SQL

## B.2.4 숫자 접미사 금지

\`\`\`text id=“namB015” CustomerService2

ReservationMapperNew

UserDtoV2Temp

selectList3



Version 차이가 필요하면 정식 Version·기능 의미를 이름에 반영한다.

\`\`\`text id="namB016"
CustomerSummaryV2Response

LegacyReservationAdapter

ReservationMigrationService

단, 영구 이름에 New, Old, Temp를 남기지 않는다.

# B.3 업무코드·Context·WAR·Module

## B.3.1 변환 공식

업무코드가 CT로 확정되면 기본적으로 다음이 자동 결정된다.

| 항목 | 공식 | 결과 |
| --- | --- | --- |
| Context | /{bc.lower} | /ct |
| WAR | {bc.lower}.war | ct.war |
| Gradle Module | {bc.lower}-service | ct-service |
| Java Prefix | PascalCase | Ct |
| Package | com.nh.nsight.marketing.{bc.lower} | .ct |
| ServiceId Prefix | 업무코드 | CT. |
| DB Prefix | 업무코드 | CT\_ |
| 오류 Prefix | E-{BC}- | E-CT- |

## B.3.2 공식 업무코드 대장 우선

업무코드는 의미를 추정해서 사용하지 않는다.

프로젝트 문서 사이에서 다음과 같은 의미 충돌이 발생할 수 있다.

\`\`\`text id=“namB017” CT Contact인가?

Customer Targeting인가?

MG Management인가?

Message인가?



판정 기준:

\`\`\`text id="namB018"
공식 업무코드 대장

→ 데이터 소유권

→ 조직 책임

→ WAR·Context

→ 운영 책임자

본 상담예약 예제에서는 CT를 **Contact·상담접점 업무**로 사용한다.

공식 업무코드 대장에서 CT가 다른 의미로 승인되어 있다면 상담예약 구현 전에 신규 코드를 배정해야 한다.

코드 충돌을 알고도 개발 후 이름만 변경하는 방식은 금지한다.

# B.4 화면·이벤트·메뉴·권한 명명

## B.4.1 신규 화면 ID

신규 업무 권장 형식:

text id="namB019" {업무코드}-{업무세구분코드}-{4자리 순번}

예:

text id="namB020" CT-RSV-0001

| 구간 | 의미 |
| --- | --- |
| CT | 업무코드 |
| RSV | 업무세구분·예약 |
| 0001 | 세구분 내 화면순번 |

## B.4.2 기존 화면번호와 병행

기존 OM 등에는 다음 형식이 존재할 수 있다.

text id="namB021" OM0101

원칙:

\`\`\`text id=“namB022” 신규 화면 → {BC}-{SUB}-{NNNN}

기존 화면 → 기존 ID 유지

일괄 Rename → 금지

신·구 연결 → 화면 관리대장 Alias로 관리



예:

| 기존 화면번호 | 신규 표준 Alias | 상태 |
|---|---|---|
| \`OM0101\` | \`OM-USR-0001\` | 기존 호환 |
| \`OM0201\` | \`OM-SVC-0001\` | 기존 호환 |

외부 연계·메뉴·권한·즐겨찾기에서 사용 중인 화면 ID를 영향분석 없이 변경하지 않는다.

\---

\## B.4.3 화면 이벤트 ID

형식:

\`\`\`text id="namB023"
{화면ID}-E{2자리}

예:

| 이벤트 | ID |
| --- | --- |
| 초기조회 | CT-RSV-0001-E01 |
| 조회 | CT-RSV-0001-E02 |
| 상세선택 | CT-RSV-0001-E03 |
| 등록 | CT-RSV-0001-E04 |
| 수정 | CT-RSV-0001-E05 |
| 취소 | CT-RSV-0001-E06 |

이벤트 ID는 버튼 ID와 다르다.

\`\`\`text id=“namB024” btnSearch → UI 객체 ID

CT-RSV-0001-E02 → 사용자 이벤트 식별자


\## B.4.4 기능 ID

화면 안에서 권한·요구사항을 관리할 기능 단위가 필요할 때 사용한다.

\`\`\`text id="namB025"
{화면ID}-F{NN}

예:

| 기능 | 기능 ID |
| --- | --- |
| 상담예약 조회 | CT-RSV-0001-F01 |
| 상담예약 등록 | CT-RSV-0001-F02 |
| 상담예약 수정 | CT-RSV-0001-F03 |
| 상담예약 취소 | CT-RSV-0001-F04 |

## B.4.5 메뉴 ID

text id="namB026" MNU-{BC}-{SUB}-{NNNN}

예:

text id="namB027" MNU-CT-RSV-0001

메뉴와 화면은 1:1이 아닐 수 있다.

\`\`\`text id=“namB028” 하나의 화면

← 여러 메뉴 진입

하나의 메뉴

→ 화면 또는 외부 URL


\## B.4.6 권한코드

형식:

\`\`\`text id="namB029"
{BC}\_{DOMAIN}\_{ACTION}

예:

\`\`\`text id=“namB030” CT\_RESERVATION\_VIEW

CT\_RESERVATION\_CREATE

CT\_RESERVATION\_UPDATE

CT\_RESERVATION\_CANCEL



권한코드에서 화면번호를 직접 사용하지 않는 것을 권장한다.

화면이 재구성돼도 업무권한의 의미는 유지돼야 하기 때문이다.

\---

\# B.5 ServiceId와 거래코드

\## B.5.1 ServiceId

형식:

\`\`\`text id="namB031"
{BusinessCode}.{Domain}.{action}

예:

\`\`\`text id=“namB032” CT.Reservation.selectList

CT.Reservation.selectDetail

CT.Reservation.create

CT.Reservation.update

CT.Reservation.cancel


\## B.5.2 각 구간 규칙

| 구간 | 규칙 | 예 |
|---|---|---|
| BusinessCode | 대문자 2자리 | \`CT\` |
| Domain | PascalCase 명사 | \`Reservation\` |
| action | lowerCamelCase 동사 | \`selectList\` |

\## B.5.3 권장 Action

\### 조회

\`\`\`text id="namB033"
select

selectDetail

selectList

selectSummary

search

count

exists

### 변경

\`\`\`text id=“namB034” create

update

cancel

delete

approve

reject

assign

complete


\### 실행·연계

\`\`\`text id="namB035"
execute

send

receive

retry

export

import

## B.5.4 금지 Action

\`\`\`text id=“namB036” doIt

process

work

handleData

saveData

execute1

updateNew



프레임워크가 정한 공통 메서드인 \`process()\`와 \`doHandle()\`은 예외다.

업무 ServiceId에는 업무 의미가 드러나는 동사를 사용한다.

\---

\## B.5.5 ServiceId와 Method 의미 일치

| ServiceId | Facade Method | Service Method |
|---|---|---|
| \`CT.Reservation.selectList\` | \`selectList()\` | \`selectList()\` |
| \`CT.Reservation.create\` | \`create()\` | \`create()\` |
| \`CT.Reservation.cancel\` | \`cancel()\` | \`cancel()\` |

문자열을 완전히 동일하게 강제할 필요는 없지만 의미가 달라서는 안 된다.

금지:

\`\`\`text id="namB037"
ServiceId
CT.Reservation.cancel

Facade Method
deleteData()

## B.5.6 거래코드

형식:

text id="namB038" {BC}-{TYPE}-{NNNN}

대표 거래 유형:

| 유형 | 의미 |
| --- | --- |
| INQ | 조회 |
| REG | 등록 |
| UPD | 수정·상태변경 |
| DEL | 물리 삭제 |
| EXE | 실행 |
| SND | 송신 |
| RCV | 수신 |
| BAT | Batch |
| ADM | 운영관리 |

상담예약 예:

| ServiceId | 거래코드 |
| --- | --- |
| selectList | CT-INQ-0101 |
| selectDetail | CT-INQ-0102 |
| create | CT-REG-0101 |
| update | CT-UPD-0101 |
| cancel | CT-UPD-0102 |

취소가 논리 상태변경이면 UPD를 기본으로 한다.

운영 통계에서 취소를 별도 유형으로 관리해야 한다면 CNL 추가를 공식 승인한 뒤 전체 업무에 일관되게 적용한다.

## B.5.7 ServiceId와 거래코드의 차이

| 구분 | ServiceId | 거래코드 |
| --- | --- | --- |
| 목적 | Handler 실행 대상 선택 | 운영·통계·감사 분류 |
| 형식 | 점 구분 | 하이픈 구분 |
| 예 | CT.Reservation.create | CT-REG-0101 |
| 사용 위치 | Dispatcher·Handler | OM·로그·통제 |
| 변경 영향 | 실행경로 변경 | 운영 분류 변경 |

# B.6 Package·Class·Method·Field

## B.6.1 업무 Package

권장 구조:

text id="namB039" com.nh.nsight.marketing.{bc}.{domain}.{layer}

상담예약 예:

text id="namB040" com.nh.nsight.marketing.ct.reservation ├─ entry.handler ├─ entry.facade ├─ application.service ├─ application.rule ├─ application.dto ├─ persistence.dao ├─ persistence.mapper ├─ persistence.dto ├─ client ├─ config └─ support

프로젝트가 계층 Package를 단순화해 사용하는 경우에도 다음 관계는 유지한다.

\`\`\`text id=“namB041” 업무

→ 도메인

→ 계층



금지:

\`\`\`text id="namB042"
com.nh.common

com.nh.service

com.nh.util2

com.nh.nsight.marketing.all

## B.6.2 Class 명명

| 역할 | 형식 | 예 |
| --- | --- | --- |
| Handler | {Bc}{Domain}Handler | CtReservationHandler |
| Facade | {Bc}{Domain}Facade | CtReservationFacade |
| Service | {Bc}{Domain}Service | CtReservationService |
| Rule | {Bc}{Domain}Rule | CtReservationRule |
| DAO | {Bc}{Domain}Dao | CtReservationDao |
| Mapper | {Bc}{Domain}Mapper | CtReservationMapper |
| Client | {Target}{Domain}Client | IcCustomerClient |
| Adapter | {Target}{Domain}Adapter | IcCustomerAdapter |
| Controller | {Domain}{Resource}Controller | ReservationFileController |
| Scheduler | {Bc}{Domain}{Purpose}Scheduler | CtReservationExpiryScheduler |
| Configuration | {Feature}Configuration | CtReservationConfiguration |
| Properties | {Feature}Properties | CtReservationProperties |
| Exception | {Bc}{Domain}Exception | CtReservationException |

Controller는 파일·Health·관리 API와 같은 비표준 HTTP 기능에 한정한다.

TCF 표준 온라인 거래를 위해 업무별 Controller를 생성하지 않는다.

## B.6.3 Method 명명

### 조회

| 목적 | 권장 |
| --- | --- |
| 단건 조회 | selectReservation() |
| 상세 조회 | selectReservationDetail() |
| 목록 조회 | selectReservationList() |
| 검색 | searchReservations() |
| 건수 | countReservations() |
| 존재 확인 | existsReservation() |

### 변경

| 목적 | 권장 |
| --- | --- |
| 등록 | createReservation() |
| 변경 | updateReservation() |
| 취소 | cancelReservation() |
| 완료 | completeReservation() |
| 담당자 배정 | assignReservationOwner() |

### 검증

\`\`\`text id=“namB043” validateCreateRequest()

validateUpdatableStatus()

requireReservation()

canCancel()

normalizeCustomerNo()


\## B.6.4 Boolean Method·Field

권장:

\`\`\`text id="namB044"
active

enabled

editable

cancellable

existsReservation()

hasPermission()

canUpdate()

프로젝트 내 Java Bean·Record 직렬화 규칙을 고려해 isActive와 active를 혼용하지 않는다.

## B.6.5 Constant

java id="namB045" private static final int DEFAULT\_PAGE\_SIZE = 20; private static final int MAX\_PAGE\_SIZE = 100; private static final String READY\_STATUS = "READY";

금지:

java id="namB046" private static final int size = 100; private static final String temp = "READY";

# B.7 DTO 명명규칙

## B.7.1 역할별 형식

| DTO 역할 | 형식 | 예 |
| --- | --- | --- |
| 외부 요청 | {UseCase}Request | ReservationCreateRequest |
| 외부 응답 | {UseCase}Response | ReservationCreateResponse |
| 조회조건 | {UseCase}Query | ReservationListQuery |
| 변경명령 | {UseCase}Command | ReservationUpdateCommand |
| DB Row | {UseCase}Row | ReservationDetailRow |
| 목록 Item | {UseCase}Item | ReservationListItem |
| 결과 | {UseCase}Result | ReservationValidationResult |
| 연계 요청 | {Target}{UseCase}Request | IcCustomerVerifyRequest |
| 연계 응답 | {Target}{UseCase}Response | IcCustomerVerifyResponse |
| Event | {BusinessFact}Event | ReservationCanceledEvent |
| Properties | {Feature}Properties | ReservationTimeoutProperties |

## B.7.2 금지 이름

\`\`\`text id=“namB047” ReservationDto

ReservationVO

ReservationData

ReservationInfo

ReservationInfo2

RequestMap

ResultMap



\`Data\`, \`Info\`, \`VO\`처럼 역할이 불명확한 단어는 프로젝트에서 별도 정의하지 않는 한 사용하지 않는다.

\## B.7.3 DTO 책임 분리

\`\`\`text id="namB048"
ReservationUpdateRequest
화면 입력

ReservationUpdateCommand
서버 결정값 포함

ReservationUpdateRow
DB UPDATE Parameter

ReservationChangeResponse
외부 응답

같은 DTO를 전 계층에 전달하지 않는다.

# B.8 MyBatis·SQL 명명

## B.8.1 Mapper

\`\`\`text id=“namB049” Interface CtReservationMapper

XML CtReservationMapper.xml

Namespace Java Interface 전체 Package



다음 셋은 일치해야 한다.

\`\`\`text id="namB050"
Mapper Method

\=

XML SQL id

\=

DAO가 호출하는 이름

## B.8.2 Mapper SQL ID

| 유형 | 규칙 | 예 |
| --- | --- | --- |
| 단건 | select{Entity} | selectReservation |
| 상세 | select{Entity}Detail | selectReservationDetail |
| 목록 | select{Entity}List | selectReservationList |
| 건수 | count{Entity} | countReservation |
| 존재 | exists{Entity} | existsReservation |
| 등록 | insert{Entity} | insertReservation |
| 수정 | update{Entity} | updateReservation |
| 취소 | cancel{Entity} | cancelReservation |
| 삭제 | delete{Entity} | deleteReservation |
| 병합 | merge{Entity} | mergeReservation |

금지:

\`\`\`text id=“namB051” select1

queryData

getList2

sql001

updateAll

procData


\## B.8.3 관리용 SQL ID

Mapper Method와 별도로 설계·튜닝·로그에서 사용하는 관리 ID다.

형식:

\`\`\`text id="namB052"
{BC}-{SUB}-{OP}-{NNN}

연산코드:

\`\`\`text id=“namB053” SEL

INS

UPD

DEL

MRG

PRC



상담예약 예:

| SQL | 관리 ID |
|---|---|
| 목록조회 | \`CT-RSV-SEL-001\` |
| 상세조회 | \`CT-RSV-SEL-002\` |
| 중복확인 | \`CT-RSV-SEL-003\` |
| 현재값 조회 | \`CT-RSV-SEL-004\` |
| Master 등록 | \`CT-RSV-INS-001\` |
| History 등록 | \`CT-RSV-INS-002\` |
| 예약 수정 | \`CT-RSV-UPD-001\` |
| 예약 취소 | \`CT-RSV-UPD-002\` |

\---

\# B.9 DB 객체 명명

\## B.9.1 기본 원칙

\`\`\`text id="namB054"
UPPER\_SNAKE\_CASE

업무 또는 플랫폼 Prefix

전체 단어 우선

의미 없는 번호 최소화

DB 객체 길이는 운영 DBMS와 모델링·배포도구의 제한을 따른다.

다양한 도구와의 호환성이 필요하면 물리명은 30자 내외를 우선 검토한다.

## B.9.2 Table

형식:

text id="namB055" {업무코드}\_{업무대상}\_{성격}

예:

\`\`\`text id=“namB056” CT\_CONTACT\_RESERVATION

CT\_CONTACT\_RESERVATION\_HISTORY

CT\_CONTACT\_RESERVATION\_HIST\_DTL



대표 성격:

| 접미어 | 의미 |
|---|---|
| \`\_MASTER\` | 기준·현재 Master |
| \`\_DETAIL\`·\`\_DTL\` | 상세 |
| \`\_HISTORY\`·\`\_HIST\` | 변경이력 |
| \`\_LOG\` | 로그 |
| \`\_MAP\` | 매핑 |
| \`\_CONTROL\` | 통제 |
| \`\_STATUS\` | 상태 |
| \`\_QUEUE\` | 대기 |
| \`\_OUTBOX\` | Event Outbox |

한 시스템 안에서 \`\_DETAIL\`과 \`\_DTL\`, \`\_HISTORY\`와 \`\_HIST\`를 임의로 혼용하지 않는다.

\---

\## B.9.3 Column

| 의미 | 권장 |
|---|---|
| 식별자 | \`\_ID\` |
| 코드 | \`\_CD\` |
| 명칭 | \`\_NM\` |
| 여부 | \`\_YN\` |
| 일자 | \`\_DT\` |
| 일시 | \`\_DTM\` |
| 건수 | \`\_CNT\` |
| 금액 | \`\_AMT\` |
| 번호 | \`\_NO\` |
| 순번 | \`\_SEQ\` |
| 내용 | \`\_CONTENT\` |
| 상태 | \`STATUS\_CD\` |
| 생성자 | \`CREATED\_BY\` |
| 생성일시 | \`CREATED\_DTM\` |
| 수정자 | \`UPDATED\_BY\` |
| 수정일시 | \`UPDATED\_DTM\` |
| Version | \`VERSION\_NO\` |

금지:

\`\`\`text id="namB057"
DATE1

TEMP\_VAL

FLAG1

USER

VALUE

DATA

DB 예약어와 의미 없는 Column명을 사용하지 않는다.

## B.9.4 Constraint

| 객체 | 형식 | 예 |
| --- | --- | --- |
| Primary Key | PK\_{TABLE} | PK\_CT\_CONTACT\_RESERVATION |
| Unique | UK\_{TABLE}\_{NN} | UK\_CT\_CONTACT\_RESERVATION\_01 |
| Foreign Key | FK\_{CHILD}\_{PARENT}\_{NN} | FK\_CT\_RSV\_HIST\_RSV\_01 |
| Check | CK\_{TABLE}\_{MEANING} | CK\_CT\_RSV\_STATUS |
| Not Null | Column 정의 | NOT NULL |

## B.9.5 Index

신규 표준:

text id="namB058" IX\_{TABLE축약}\_{의미}

예:

\`\`\`text id=“namB059” IX\_CT\_RSV\_CUST\_DTM

IX\_CT\_RSV\_BRANCH\_DTM

IX\_CT\_RSV\_HISTORY



기존 객체가 \`IDX\_\`를 사용하면 일괄 변경하지 않고 기존 표준과 신규 표준의 적용 범위를 DB 명명대장에 기록한다.

\---

\## B.9.6 Sequence·View·Synonym

| 객체 | 형식 | 예 |
|---|---|---|
| Sequence | \`SEQ\_{BC}\_{ENTITY}\` | \`SEQ\_CT\_RESERVATION\` |
| View | \`V\_{BC}\_{SUBJECT}\` | \`V\_CT\_RESERVATION\_SUMMARY\` |
| Materialized View | \`MV\_{BC}\_{SUBJECT}\` | \`MV\_SV\_CUSTOMER\_SUMMARY\` |
| Synonym | \`SYN\_{SOURCE}\_{OBJECT}\` | \`SYN\_RDW\_CUSTOMER\_MASTER\` |

Synonym은 실제 소유 Schema를 숨기므로 무분별하게 사용하지 않는다.

\---

\## B.9.7 Procedure·Function·Package·Trigger

| 객체 | 형식 | 예 |
|---|---|---|
| Procedure | \`PRC\_{BC}\_{TARGET}\_{ACTION}\` | \`PRC\_CT\_RESERVATION\_ARCHIVE\` |
| Function | \`FN\_{BC}\_{TARGET}\` | \`FN\_CT\_MASK\_CUSTOMER\_NO\` |
| DB Package | \`PKG\_{BC}\_{DOMAIN}\` | \`PKG\_CT\_RESERVATION\` |
| Trigger | \`TRG\_{TABLE}\_{TIMING}\_{ACTION}\` | \`TRG\_CT\_RESERVATION\_BU\` |

Trigger Timing·Action:

\`\`\`text id="namB060"
BI Before Insert

BU Before Update

BD Before Delete

AI After Insert

AU After Update

AD After Delete

Trigger는 처리경로를 숨기므로 명확한 감사·무결성 목적이 있을 때만 사용한다.

# B.10 오류코드·메시지

## B.10.1 업무 오류코드

본 가이드의 신규 업무 권장 형식:

text id="namB061" E-{BC}-{SUB}-{NNNN}

예:

\`\`\`text id=“namB062” E-CT-RSV-0001 예약 없음

E-CT-RSV-0006 동시 수정 충돌

E-CT-RSV-0008 중복 예약

E-CT-RSV-9002 DB 처리 오류



권장 순번영역 예:

| 범위 | 유형 |
|---:|---|
| \`0001~0999\` | 업무·Validation |
| \`1000~1999\` | 권한·보안 |
| \`2000~2999\` | 연계 |
| \`8000~8999\` | Timeout·복구 |
| \`9000~9999\` | 시스템·DB |

프로젝트의 중앙 오류코드 체계가 \`E-{BC}-{TYPE}-{NNNN}\`로 승인돼 있다면 해당 표준을 우선한다.

한 업무 안에서 두 방식을 혼용하지 않는다.

\---

\## B.10.2 공통 오류코드

\`\`\`text id="namB063"
E-COM-VALID-0001

E-COM-DISP-0001

E-COM-AUTH-0001

E-COM-SYS-0001

공통 Framework 오류와 업무 오류를 분리한다.

## B.10.3 메시지 Key

\`\`\`text id=“namB064” error.ct.rsv.0006

error.com.auth.0001



오류코드와 사용자 메시지를 하드코딩으로 여러 Class에 복제하지 않는다.

\---

\# B.11 Header·추적 ID·로그

\## B.11.1 Header Field

| JSON·Java | DB | 의미 |
|---|---|---|
| \`businessCode\` | \`BUSINESS\_CODE\` | 업무 |
| \`serviceId\` | \`SERVICE\_ID\` | 실행 거래 |
| \`transactionCode\` | \`TRANSACTION\_CODE\` | 운영 거래 |
| \`guid\` | \`GUID\` | 업무거래 식별 |
| \`traceId\` | \`TRACE\_ID\` | 호출경로 추적 |
| \`requestId\` | \`REQUEST\_ID\` | HTTP 요청 식별 |
| \`userId\` | \`USER\_ID\` | 인증 사용자 |
| \`branchId\` | \`BRANCH\_ID\` | 지점 |
| \`channelId\` | \`CHANNEL\_ID\` | 채널 |
| \`idempotencyKey\` | 저장 시 Hash 권장 | 반복요청 식별 |

\## B.11.2 추적 ID 원칙

\`\`\`text id="namB065"
GUID
→ 업무거래 중심

TraceId
→ 분산 호출 중심

RequestId
→ 개별 HTTP 요청 중심

프로젝트 구현에서 동일 값을 공유할 수 있으나 의미와 생성·전파 규칙을 문서화한다.

## B.11.3 로그 파일

권장 예:

\`\`\`text id=“namB066” ct-application.log

ct-transaction.log

ct-audit.log

ct-error.log



모든 WAR가 하나의 \`application.log\`에 기록하지 않는다.

\## B.11.4 구조화 로그 Field

\`\`\`text id="namB067"
timestamp

level

businessCode

serviceId

transactionCode

guid

traceId

resultType

errorCode

elapsedMs

instanceId

artifactVersion

금지:

\`\`\`text id=“namB068” Password

JWT

Authorization Header

고객번호 원문

상담메모

Idempotency Key 원문


\---

\# B.12 Metric·Dashboard·Alert

\## B.12.1 Metric 이름

형식:

\`\`\`text id="namB069"
{bc}.{domain}.{purpose}.{measure}

예:

\`\`\`text id=“namB070” ct.reservation.transaction.count

ct.reservation.transaction.duration

ct.reservation.timeout.count

ct.reservation.concurrent.conflict.count

ct.reservation.history.failure.count


\## B.12.2 Metric Tag

권장:

\`\`\`text id="namB071"
serviceId

resultType

errorType

instanceId

artifactVersion

금지:

\`\`\`text id=“namB072” customerNo

reservationId

userId

guid

traceId

fullUrl



고유값을 Tag로 사용하면 Metric Cardinality가 폭증한다.

\## B.12.3 Dashboard ID

\`\`\`text id="namB073"
DSH-{BC}-{DOMAIN}-{NNN}

예:

text id="namB074" DSH-CT-RESERVATION-001

## B.12.4 Alert ID

text id="namB075" ALT-{BC}-{DOMAIN}-{NNN}

예:

text id="namB076" ALT-CT-RESERVATION-001

Alert 이름에는 증상과 조건을 포함한다.

\`\`\`text id=“namB077” CT Reservation History Failure

CT Reservation p95 Exceeded


\---

\# B.13 Gateway·연계·Event

\## B.13.1 Gateway Route

| 객체 | 형식 | 예 |
|---|---|---|
| Route ID | \`GW-{ENV}-{BC}-ONLINE\` | \`GW-PRD-CT-ONLINE\` |
| Target Group | \`TG-{ENV}-{BC}-{NN}\` | \`TG-PRD-CT-01\` |
| Context | \`/{bc.lower}\` | \`/ct\` |

\## B.13.2 Client

\`\`\`text id="namB078"
{TargetBc}{Domain}Client

예:

\`\`\`text id=“namB079” IcCustomerClient

SvCustomerSummaryClient

OmServiceCatalogClient



금지:

\`\`\`text id="namB080"
HttpUtil

ApiService

CommonRestClient

## B.13.3 Event Class

업무사실을 과거형으로 표현한다.

\`\`\`text id=“namB081” ReservationCreatedEvent

ReservationUpdatedEvent

ReservationCanceledEvent



명령형 Event는 피한다.

\`\`\`text id="namB082"
CreateReservationEvent

명령은 Command, 발생한 사실은 Event로 구분한다.

## B.13.4 Topic·Queue

권장 형식:

text id="namB083" nsight.{bc}.{domain}.{event}.v{version}

예:

text id="namB084" nsight.ct.reservation.canceled.v1

Queue가 Consumer 전용이면 Consumer 목적을 추가한다.

text id="namB085" nsight.ct.reservation.canceled.om.v1

# B.14 Batch·Scheduler·Cache

## B.14.1 Batch Job

text id="namB086" BAT-{BC}-{DOMAIN}-{NNN}

예:

\`\`\`text id=“namB087” BAT-CT-RSV-001 상담예약 만료처리

BAT-CT-RSV-002 상담예약 이력 Archive


\## B.14.2 Job·Step Class

\`\`\`text id="namB088"
CtReservationExpiryJob

CtReservationExpiryStep

CtReservationArchiveJob

## B.14.3 Scheduler

text id="namB089" CtReservationExpiryScheduler

Method:

\`\`\`text id=“namB090” scheduleReservationExpiry()

executeReservationExpiryJob()



Scheduler가 업무 Batch Logic을 직접 수행하지 않는다.

\## B.14.4 Cache Name

\`\`\`text id="namB091"
lowerCamelCase

예:

\`\`\`text id=“namB092” commonCode

serviceCatalog

reservationPurposeCode


\## B.14.5 Cache Key

\`\`\`text id="namB093"
{BC}:{DOMAIN}:{TYPE}:{KEY}

예:

text id="namB094" CT:RESERVATION:PURPOSE:PRODUCT

개인정보를 Cache Key에 원문으로 사용하지 않는다.

# B.15 파일·설정·배포 명명

## B.15.1 Source·Resource 파일

| 파일 | 형식 | 예 |
| --- | --- | --- |
| Java | Class와 동일 | CtReservationService.java |
| Mapper XML | Mapper와 동일 | CtReservationMapper.xml |
| Sample JSON | {bc}-{domain}-{action}.json | ct-reservation-create.json |
| UI HTML | kebab-case | reservation-management.html |
| JavaScript | kebab-case | reservation-management.js |
| CSS | kebab-case | reservation-management.css |

## B.15.2 Spring 설정

\`\`\`text id=“namB095” application.yml

application-local.yml

application-dev.yml

application-prod.yml



업무 설정 Prefix:

\`\`\`text id="namB096"
ct.reservation.search.max-months

ct.reservation.page.max-size

ct.reservation.create.timeout

환경변수:

\`\`\`text id=“namB097” CT\_RESERVATION\_SEARCH\_MAX\_MONTHS

CT\_DB\_URL

CT\_DB\_USERNAME

CT\_DB\_PASSWORD


\## B.15.3 DB Migration

\`\`\`text id="namB098"
V{YYYYMMDD}\_{NN}\_\_{lower\_snake\_description}.sql

예:

\`\`\`text id=“namB099” V20260718\_01\_\_create\_ct\_reservation.sql

V20260718\_02\_\_create\_ct\_reservation\_history.sql

V20260718\_03\_\_create\_ct\_reservation\_indexes.sql



검증:

\`\`\`text id="namB100"
Q20260718\_01\_\_validate\_ct\_reservation.sql

Rollback:

text id="namB101" R20260718\_01\_\_rollback\_ct\_reservation.sql

## B.15.4 Artifact

| 항목 | 형식 | 예 |
| --- | --- | --- |
| Module | {bc}-service | ct-service |
| WAR | {bc}.war | ct.war |
| Context | /{bc} | /ct |
| Release | {module}-{version} | ct-service-1.0.0 |
| Git Tag | v{semver} | v1.0.0 |

환경명을 WAR 파일에 포함해 환경별로 재빌드하지 않는다.

금지:

\`\`\`text id=“namB102” ct-prod.war

ct-final.war

ct-final2.war

ct-new.war


\---

\# B.16 상담예약 통합 작업표 예시

\## B.16.1 화면–거래–프로그램 작업표

| 이벤트 | ServiceId | 거래코드 | Facade Method | 권한 |
|---|---|---|---|---|
| \`E01\` 초기조회 | \`CT.Reservation.selectList\` | \`CT-INQ-0101\` | \`selectList\` | \`CT\_RESERVATION\_VIEW\` |
| \`E02\` 조회 | \`CT.Reservation.selectList\` | \`CT-INQ-0101\` | \`selectList\` | \`CT\_RESERVATION\_VIEW\` |
| \`E03\` 상세 | \`CT.Reservation.selectDetail\` | \`CT-INQ-0102\` | \`selectDetail\` | \`CT\_RESERVATION\_VIEW\` |
| \`E04\` 등록 | \`CT.Reservation.create\` | \`CT-REG-0101\` | \`create\` | \`CT\_RESERVATION\_CREATE\` |
| \`E05\` 수정 | \`CT.Reservation.update\` | \`CT-UPD-0101\` | \`update\` | \`CT\_RESERVATION\_UPDATE\` |
| \`E06\` 취소 | \`CT.Reservation.cancel\` | \`CT-UPD-0102\` | \`cancel\` | \`CT\_RESERVATION\_CANCEL\` |

\## B.16.2 프로그램–SQL 작업표

| ServiceId | Handler | Service | Mapper SQL | 관리 SQL ID |
|---|---|---|---|---|
| selectList | \`CtReservationHandler\` | \`selectList\` | \`selectReservationList\` | \`CT-RSV-SEL-001\` |
| selectDetail | 동일 | \`selectDetail\` | \`selectReservationDetail\` | \`CT-RSV-SEL-002\` |
| create | 동일 | \`create\` | \`insertReservation\` | \`CT-RSV-INS-001\` |
| create | 동일 | \`create\` | \`insertReservationHistory\` | \`CT-RSV-INS-002\` |
| update | 동일 | \`update\` | \`updateReservation\` | \`CT-RSV-UPD-001\` |
| cancel | 동일 | \`cancel\` | \`cancelReservation\` | \`CT-RSV-UPD-002\` |

\## B.16.3 DB 작업표

| SQL ID | DB 객체 | CRUD | Index |
|---|---|:---:|---|
| \`SEL-001\` | \`CT\_CONTACT\_RESERVATION\` | R | \`IX\_CT\_RSV\_BRANCH\_DTM\` |
| \`SEL-002\` | \`CT\_CONTACT\_RESERVATION\` | R | PK |
| \`INS-001\` | \`CT\_CONTACT\_RESERVATION\` | C | PK·UK |
| \`INS-002\` | \`CT\_CONTACT\_RESERVATION\_HISTORY\` | C | PK |
| \`UPD-001\` | \`CT\_CONTACT\_RESERVATION\` | U | PK·UK |
| \`UPD-002\` | \`CT\_CONTACT\_RESERVATION\` | U | PK |

\## B.16.4 운영 작업표

| ServiceId | Timeout | 감사 | Metric | Runbook |
|---|---:|---|---|---|
| selectList | 3초 | 일반 거래 | list.duration | 조회지연 |
| selectDetail | 2초 | 개인정보 조회 | detail.duration | 조회오류 |
| create | 5초 | 변경감사 | create.duration | 중복·UNKNOWN |
| update | 5초 | 변경감사 | update.duration | Version 충돌 |
| cancel | 5초 | 중요 변경감사 | cancel.duration | 취소 실패 |

\---

\# B.17 정상 예시

\`\`\`text id="namB103"
업무코드
CT

세구분
RSV

화면
CT-RSV-0001

이벤트
CT-RSV-0001-E04

ServiceId
CT.Reservation.create

거래코드
CT-REG-0101

Handler
CtReservationHandler

Facade
CtReservationFacade.create

Service
CtReservationService.create

Mapper
CtReservationMapper.insertReservation

SQL ID
CT-RSV-INS-001

Table
CT\_CONTACT\_RESERVATION

오류코드
E-CT-RSV-0008

Metric
ct.reservation.create.duration

Test
TC-CT-RSV-015

판정:

\`\`\`text id=“namB104” 업무 소유권이 식별된다.

화면에서 SQL까지 이동할 수 있다.

SQL에서 영향 화면을 역추적할 수 있다.

운영 등록과 로그를 찾을 수 있다.

중복 이름을 자동검증할 수 있다.


\---

\# B.18 금지 예시

\`\`\`text id="namB105"
업무코드를 개발자가 임의로 만든다.

CT와 MG의 공식 의미를 확인하지 않는다.

신규 화면에 기존 화면번호와 다른 형식을 임의 혼용한다.

한 화면 버튼마다 별도 화면 ID를 만든다.

ServiceId를 URL처럼 작성한다.

ServiceId에 한글·공백·하이픈을 넣는다.

ServiceId Action을 process·doIt으로 작성한다.

하나의 save ServiceId에서 모든 CRUD를 처리한다.

거래코드와 ServiceId를 같은 값으로 사용한다.

Handler·Facade·Service 역할 접미사를 생략한다.

CommonService·DataService처럼 책임이 모호한 이름을 사용한다.

Request DTO를 CustomerDto 하나로 통합한다.

Mapper Method와 XML SQL ID가 다르다.

SQL ID를 select1·sql001로 만든다.

다른 업무 Table에 자기 업무 Prefix를 붙인다.

DB Column을 FLAG1·DATA·VALUE로 만든다.

물리 UPDATE SQL인데 Mapper ID를 saveData로 만든다.

오류코드를 등록하지 않고 문자열 메시지만 반환한다.

Metric Tag에 고객번호·예약 ID·GUID를 넣는다.

Cache Key에 주민번호·고객번호 원문을 넣는다.

운영 WAR를 ct-final2.war로 관리한다.

같은 ServiceId의 의미를 운영 중 변경한다.

폐기된 이름을 호출량 확인 없이 즉시 삭제한다.

# B.19 책임 경계와 RACI

| 활동 | 업무 | UI | 개발 | AA | DA·DBA | 보안 | 운영 | DevOps | QA |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 업무코드 | A/C | I | I | A/R | C | I | C | I | I |
| 세구분·도메인 | A/R | C | C | A/C | C | I | I | I | C |
| 화면·이벤트 | A/C | A/R | C | C | I | C | I | I | C |
| ServiceId | C | C | R | A | I | C | C | I | C |
| 거래코드 | C | I | R/C | A | I | C | A/C | I | C |
| Package·Class | I | I | A/R | C | I | I | I | C | C |
| DTO·Method | C | C | A/R | C | I | C | I | I | C |
| Mapper·SQL | C | I | R | C | A/C | I | C | I | C |
| DB 객체 | C | I | C | C | A/R | C | C | I | C |
| 오류코드 | A/C | C | R | A/C | I | C | C | I | C |
| 로그·Metric | I | I | R/C | C | I | C | A/R | C | C |
| 배포명 | I | I | C | C | I | I | C | A/R | I |
| 자동검증 | I | I | R/C | A | C | C | C | A/R | R |
| 변경·폐기 | A/C | C | R | A | C | C | A/C | C | C |

# B.20 자동검증 및 품질 Gate

## B.20.1 정규식 예

### 업무코드

regex id="namB106" ^\[A-Z\]{2}$

### 업무세구분

regex id="namB107" ^\[A-Z0-9\]{2,5}$

### 화면 ID

regex id="namB108" ^\[A-Z\]{2}-\[A-Z0-9\]{2,5}-\[0-9\]{4}$

### 이벤트 ID

regex id="namB109" ^\[A-Z\]{2}-\[A-Z0-9\]{2,5}-\[0-9\]{4}-E\[0-9\]{2}$

### ServiceId

regex id="namB110" ^\[A-Z\]{2}\\.\[A-Z\]\[A-Za-z0-9\]\*\\.\[a-z\]\[A-Za-z0-9\]\*$

### 거래코드

regex id="namB111" ^\[A-Z\]{2}-\[A-Z\]{3,4}-\[0-9\]{4}$

### 오류코드

regex id="namB112" ^E-\[A-Z\]{2,3}-\[A-Z0-9\]{2,5}-\[0-9\]{4}$

### 관리 SQL ID

regex id="namB113" ^\[A-Z\]{2}-\[A-Z0-9\]{2,5}-(SEL|INS|UPD|DEL|MRG|PRC)-\[0-9\]{3}$

## B.20.2 교차 정합성 검사

\`\`\`text id=“namB114” businessCode=CT

→ Context=/ct

→ WAR=ct.war

→ Module=ct-service

→ Package Prefix=.ct

→ ServiceId Prefix=CT.

→ DB Prefix=CT\_

→ 오류 Prefix=E-CT-



자동검증 항목:

\`\`\`text id="namB115"
화면 ID 중복

이벤트 ID 중복

ServiceId 중복

Handler 미등록

ServiceId Prefix와 businessCode 불일치

Context와 WAR 불일치

Mapper Method와 XML ID 불일치

Mapper Namespace 불일치

거래코드 중복

오류코드 중복

SQL 관리 ID 중복

DB Prefix 소유권 불일치

OM Catalog 미등록

권한·Timeout 미등록

Metric Prefix 불일치

## B.20.3 금지어 검사

\`\`\`text id=“namB116” CommonService

CommonUtil

DataService

InfoDto

Temp

New

Old

Test2

select1

sql001

doIt

processData



공식 Framework Class와 테스트 Fixture 등 승인 예외는 Allow List로 관리한다.

\## B.20.4 Gate 등급

| 위반 | 등급 |
|---|---|
| ServiceId 중복 | Blocker |
| 업무코드·WAR 불일치 | Blocker |
| Handler 미등록 | Blocker |
| Mapper SQL ID 불일치 | Blocker |
| DB 소유권 Prefix 오류 | Critical |
| 오류코드 중복 | Critical |
| OM·권한·Timeout 누락 | Major |
| 모호한 Class명 | Major |
| 파일명 표기 위반 | Minor |
| 기존 Legacy 예외 | 승인·관리 |

\---

\# B.21 테스트 시나리오

| ID | 시나리오 | 기대 결과 |
|---|---|---|
| NAM-001 | 정상 업무코드 \`CT\` | PASS |
| NAM-002 | 소문자 업무코드 \`ct\` | 실패 |
| NAM-003 | 미등록 업무코드 | 실패 |
| NAM-004 | 화면 ID 중복 | 실패 |
| NAM-005 | 화면 ID 세구분 누락 | 실패 |
| NAM-006 | 이벤트 ID 화면 불일치 | 실패 |
| NAM-007 | ServiceId 정상 | PASS |
| NAM-008 | ServiceId 소문자 Prefix | 실패 |
| NAM-009 | ServiceId 중복 Handler | 기동 실패 |
| NAM-010 | businessCode와 Prefix 불일치 | 실패 |
| NAM-011 | 거래코드 중복 | 실패 |
| NAM-012 | 거래유형 미등록 | 실패 |
| NAM-013 | Handler 접미사 누락 | 실패 |
| NAM-014 | Handler→Mapper 직접 의존 | Architecture 실패 |
| NAM-015 | DTO 역할 불명확 | Review 실패 |
| NAM-016 | Mapper Method·XML ID 불일치 | Context 실패 |
| NAM-017 | Mapper Namespace 불일치 | Context 실패 |
| NAM-018 | SQL ID \`select1\` | Gate 실패 |
| NAM-019 | DB Prefix 타 업무 | Gate 실패 |
| NAM-020 | DB 예약어 Column | Gate 실패 |
| NAM-021 | 오류코드 중복 | Gate 실패 |
| NAM-022 | Metric 고유값 Tag | Gate 실패 |
| NAM-023 | Cache Key 개인정보 | Security 실패 |
| NAM-024 | Legacy 화면 ID 유지 | 승인 시 PASS |
| NAM-025 | Legacy 화면 일괄 Rename | 영향분석 전 실패 |
| NAM-026 | ct-prod.war | Artifact Gate 실패 |
| NAM-027 | Migration 이름 중복 | DB Gate 실패 |
| NAM-028 | OM Catalog 미등록 | 배포 실패 |
| NAM-029 | 권한코드 누락 | 배포 차단 |
| NAM-030 | 정방향·역방향 추적 | 전체 연결 성공 |

\---

\# B.22 완료 체크리스트

\## 업무·배포

| 점검 | 완료 |
|---|:---:|
| 공식 업무코드를 확인했다. | □ |
| 업무세구분과 도메인을 확정했다. | □ |
| Context·WAR·Module이 자동 연결된다. | □ |
| Package Prefix가 업무코드와 일치한다. | □ |
| 업무코드 의미 충돌이 없다. | □ |

\## 화면·거래

| 점검 | 완료 |
|---|:---:|
| 화면 ID가 표준 형식이다. | □ |
| 이벤트 ID가 모두 정의됐다. | □ |
| 이벤트별 ServiceId가 연결된다. | □ |
| 거래코드가 등록됐다. | □ |
| 기능권한이 연결된다. | □ |
| Timeout이 연결된다. | □ |

\## 프로그램

| 점검 | 완료 |
|---|:---:|
| Package가 업무·도메인·계층을 표현한다. | □ |
| Class에 역할 접미사가 있다. | □ |
| Method가 업무행위를 표현한다. | □ |
| 의미 없는 축약과 번호가 없다. | □ |
| DTO가 역할별로 분리됐다. | □ |
| 다른 WAR 구현을 직접 참조하지 않는다. | □ |

\## SQL·DB

| 점검 | 완료 |
|---|:---:|
| Mapper Interface와 XML명이 같다. | □ |
| Mapper Method와 SQL ID가 같다. | □ |
| 관리 SQL ID가 있다. | □ |
| DB Prefix가 소유업무와 일치한다. | □ |
| Constraint·Index 이름이 표준이다. | □ |
| 예약어·모호한 Column명이 없다. | □ |

\## 운영·보안

| 점검 | 완료 |
|---|:---:|
| 오류코드가 등록됐다. | □ |
| GUID·TraceId·RequestId가 구분된다. | □ |
| 로그 Field가 표준화됐다. | □ |
| 민감정보가 이름·Tag·Key에 없다. | □ |
| Metric Prefix가 일치한다. | □ |
| Dashboard·Alert·Runbook이 연결된다. | □ |

\## 자동검증

| 점검 | 완료 |
|---|:---:|
| 정규식 검사를 수행한다. | □ |
| 중복 ServiceId를 차단한다. | □ |
| Handler 등록을 검증한다. | □ |
| Mapper 정합성을 검증한다. | □ |
| OM·권한·Timeout 누락을 차단한다. | □ |
| 정방향·역방향 추적을 검증한다. | □ |

\---

\# B.23 변경·호환성·폐기 관리

\## B.23.1 업무코드 변경

업무코드는 다음 요소 전체에 영향을 준다.

\`\`\`text id="namB117"
Context

WAR

Module

Package

ServiceId

거래코드

DB Prefix

오류코드

로그

Metric

Route

구현 이후 업무코드 변경은 단순 Rename이 아니라 아키텍처 변경으로 관리한다.

## B.23.2 ServiceId 변경

운영에 등록된 ServiceId는 직접 Rename하지 않는다.

권장:

\`\`\`text id=“namB118” 신규 ServiceId 추가

→ 구·신 동시 지원

→ 화면 호출 전환

→ 호출량 0 확인

→ Deprecated

→ Handler·OM 제거


\## B.23.3 화면 ID 변경

화면 ID가 다음에 사용되는지 확인한다.

\`\`\`text id="namB119"
메뉴

권한

즐겨찾기

화면 로그

테스트 자동화

외부 호출

사용자 매뉴얼

Alias 또는 Redirect 기간을 둔다.

## B.23.4 DB 객체 변경

물리명 변경은 다음을 함께 수정한다.

\`\`\`text id=“namB120” Mapper SQL

View·Synonym

Procedure

권한

배치

보고서

운영 Script

모니터링



DB 객체는 Rename보다 신규 객체 생성·데이터 이행·구 객체 폐기를 우선 검토한다.

\## B.23.5 오류코드 폐기

기존 오류코드는 재사용하지 않는다.

\`\`\`text id="namB121"
E-CT-RSV-0006 폐기

→ 다른 의미로 재사용 금지

오류코드는 운영 통계와 사용자 문의 이력에 남기 때문이다.

## B.23.6 Legacy 이름

기존 규칙을 따르지 않는 이름은 다음과 같이 관리한다.

\`\`\`text id=“namB122” Legacy 등록

Owner 지정

신규 사용 금지

호환기간 정의

전환대상 연결

폐기조건 정의


\---

\# 시사점

\## 핵심 아키텍처 판단

첫째, 명명규칙의 기준점은 Class가 아니라 업무코드와 데이터 소유권이다.

둘째, 화면 ID와 ServiceId는 1:1 관계가 아니며 화면 이벤트 단위로 연결해야 한다.

셋째, ServiceId는 실행 식별자이고 거래코드는 운영·통계 식별자이므로 역할을 분리해야 한다.

넷째, Handler·Facade·Service·Rule·DAO·Mapper의 역할 접미사는 프로그램 책임을 표시하는 아키텍처 표식이다.

다섯째, Request·Command·Row·Response DTO의 명칭을 분리해야 외부 계약과 DB 구조의 결합을 줄일 수 있다.

여섯째, Mapper Method와 XML SQL ID의 일치는 MyBatis 오류 방지뿐 아니라 SQL 역추적의 핵심 조건이다.

일곱째, DB 객체명에는 데이터를 소유하는 업무영역이 드러나야 하며 같은 DB를 사용한다는 이유로 소유권을 혼합해서는 안 된다.

여덟째, 로그·Metric·Cache Key·파일명에도 개인정보나 무제한 고유값을 사용해서는 안 된다.

아홉째, 기존 화면번호와 신규 화면 ID가 다를 때 일괄 Rename보다 Alias와 단계적 폐기를 적용해야 한다.

열째, 명명규칙은 문서 검토만으로 유지되지 않으므로 CI/CD에서 정규식·중복·교차 정합성을 자동검증해야 한다.

\---

\## 주요 위험

| 위험 | 결과 |
|---|---|
| 업무코드 의미 충돌 | 소유권·배포 혼란 |
| 화면 ID 혼용 | 메뉴·권한 추적 실패 |
| ServiceId 중복 | 잘못된 Handler 실행 |
| 모호한 Action | 거래 의미 불명 |
| DTO 역할 혼합 | 계층 강결합 |
| Mapper 이름 불일치 | 기동·실행 오류 |
| DB Prefix 오류 | 데이터 소유권 위반 |
| 오류코드 재사용 | 운영 통계 왜곡 |
| 고유값 Metric Tag | 모니터링 장애 |
| 환경별 WAR 재명명 | Artifact 추적 실패 |
| Legacy 일괄 Rename | 호환성 장애 |
| 수동검사 의존 | 규칙 지속성 상실 |

\---

\## 우선 보완 과제

1\. 공식 업무코드와 업무 의미 대장을 단일 Baseline으로 확정한다.
2\. 신규 화면 ID를 \`{BC}-{SUB}-{NNNN}\` 형식으로 통일한다.
3\. 기존 화면번호와 신규 ID의 Alias 관리대장을 작성한다.
4\. 화면 이벤트–ServiceId–프로그램–SQL 통합 관리대장을 구축한다.
5\. ServiceId·거래코드·오류코드 중앙 중복검사를 구현한다.
6\. Mapper Method·Namespace·SQL ID 자동검증을 CI에 적용한다.
7\. Package·Class 계층규칙을 ArchUnit으로 검증한다.
8\. DB Prefix와 업무 소유권 검사를 설계 Gate에 포함한다.
9\. Metric Tag·Cache Key의 개인정보와 Cardinality를 자동검사한다.
10\. Legacy 이름의 Owner·전환기한·폐기조건을 등록한다.

\---

\# 마무리말

식별자와 명명규칙 작업을 완료하려면 다음 질문에 답할 수 있어야 한다.

\`\`\`text id="namB123"
이 기능의 공식 업무코드는 무엇인가?

업무코드의 의미가 다른 문서와 충돌하지 않는가?

Context·WAR·Module·Package가 일치하는가?

화면 ID와 이벤트 ID가 구분되는가?

각 화면 이벤트는 어떤 ServiceId를 호출하는가?

ServiceId의 Domain과 Action이 업무의미를 표현하는가?

거래코드는 ServiceId와 다른 목적을 갖는가?

Handler부터 Mapper까지 이름만 보고 역할을 알 수 있는가?

Request·Command·Row·Response DTO가 구분되는가?

Mapper Method와 XML SQL ID가 일치하는가?

관리용 SQL ID로 운영에서 SQL을 찾을 수 있는가?

DB 객체명에서 데이터 소유업무를 확인할 수 있는가?

오류코드가 중복되지 않고 재사용되지 않는가?

로그·Metric·Cache Key에 개인정보가 없는가?

신규 이름이 OM·권한·Timeout·배포정보와 연결되는가?

기존 Legacy 이름의 호환·폐기절차가 있는가?

정방향과 역방향으로 전체 거래를 추적할 수 있는가?

이 규칙을 CI/CD가 자동으로 검증하는가?

부록 B의 핵심 흐름은 다음과 같다.

\`\`\`text id=“namB124” 업무코드

→ 화면·이벤트

→ ServiceId·거래코드

→ Package·Class·Method·DTO

→ Mapper·SQL

→ DB 객체

→ 오류·로그·Metric

→ 배포·운영

→ 자동검증·변경관리



가장 중요한 원칙은 다음과 같다.

\`\`\`text id="namB125"
좋은 이름은
개발자가 이해하기 쉬운 이름을 넘어선다.

화면에서 프로그램과 데이터로 이동하고,
데이터에서 영향 화면과 테스트로 돌아오며,

운영에서 장애 거래와 담당 조직을
즉시 찾을 수 있어야 한다.

한 거래의 모든 이름을
하나의 작업표에서 함께 결정할 때

명명규칙은 문서상의 형식이 아니라
실행 가능한 추적성 체계가 된다.
