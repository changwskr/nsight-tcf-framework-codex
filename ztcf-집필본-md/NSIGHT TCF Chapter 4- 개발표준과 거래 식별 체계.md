<!-- source: ztcf-집필본/NSIGHT TCF Chapter 4- 개발표준과 거래 식별 체계.docx -->
<!-- tables: mammoth-html + placeholder table-to-md -->

# 제4장. 개발표준과 거래 식별 체계

## 이 장을 시작하며

제3장에서는 NSIGHT TCF 프로젝트가 Gradle 모듈, 업무 WAR, 도메인 패키지와 프로그램 계층으로 어떻게 나뉘는지 살펴보았다.

이제 그 구조 안에 들어가는 화면, 거래와 프로그램에 일관된 이름과 식별자를 부여해야 한다.

초보 개발자는 명명규칙을 다음 정도로 생각하기 쉽다.

클래스 이름을 보기 좋게 짓는다.

메서드 이름을 알아보기 쉽게 짓는다.

화면 번호가 중복되지 않게 만든다.

ServiceId 문자열을 규칙에 맞춰 작성한다.

이것도 필요하다. 그러나 NSIGHT TCF에서 개발표준과 식별 체계의 목적은 단순히 이름을 보기 좋게 만드는 것이 아니다.

요구사항
↓
업무영역과 담당 조직
↓
화면과 사용자 이벤트
↓
실행할 ServiceId
↓
거래통제·감사·통계용 거래코드
↓
Handler·Facade·Service·Rule
↓
DAO·Mapper·SQL·DB 객체
↓
OM 운영 기준정보
↓
거래로그·감사로그·Metric
↓
장애·변경 영향 추적

한 거래에 사용되는 식별자가 서로 연결되어야 다음 질문에 답할 수 있다.

이 화면의 조회 버튼은 어느 프로그램을 호출하는가?

이 ServiceId는 어느 업무팀이 소유하는가?

운영에서 이 거래만 차단할 수 있는가?

Timeout 정책은 어느 거래에 적용되는가?

오류가 발생한 SQL은 어느 화면에 영향을 주는가?

테이블을 변경하면 어떤 ServiceId를 다시 시험해야 하는가?

로그에 기록된 거래가 어느 사용자 행동에서 시작되었는가?

NSIGHT TCF의 화면 이벤트와 업무 프로그램을 연결하는 핵심 식별자는 Controller URL이 아니라 ServiceId이며, 정방향으로는 화면에서 DB 객체까지, 역방향으로는 DB 객체에서 영향 화면까지 추적할 수 있어야 한다.

## 핵심 관점

좋은 이름은 알아보기 쉬운 이름에 그치지 않는다.

누가 소유하고,
어디에서 실행되며,
어떤 정책의 적용을 받고,
어떤 로그로 추적되고,
변경하면 어디에 영향을 주는지를
반복해서 증명할 수 있어야 한다.

## 학습 목표

이 장을 마치면 다음 내용을 설명하고 적용할 수 있어야 한다.

| 번호 | 학습 목표 |
| --- | --- |
| 1 | 업무코드가 단순 기능 분류가 아닌 소유권·배포·데이터 경계인 이유를 설명한다. |
| 2 | 화면 ID, 이벤트 ID, 기능 ID의 차이를 설명한다. |
| 3 | ServiceId와 거래코드의 목적을 구분한다. |
| 4 | 하나의 화면이 여러 ServiceId를 호출할 수 있음을 설명한다. |
| 5 | 하나의 Handler가 여러 ServiceId를 등록하는 구조를 이해한다. |
| 6 | ServiceId에서 Handler·Facade·Service·Mapper를 찾는다. |
| 7 | 패키지·클래스·메서드의 이름에서 업무와 책임을 식별한다. |
| 8 | GUID와 TraceId의 역할을 구분한다. |
| 9 | ServiceId와 OM Catalog·거래통제·Timeout 정책의 관계를 설명한다. |
| 10 | 소스·설계서·운영설정·로그 간 불일치를 발견한다. |
| 11 | 신규 거래에 필요한 등록·검증 항목을 작성한다. |
| 12 | 명명규칙과 추적성 검사를 CI/CD에서 자동화한다. |

# 한눈에 보는 거래 식별 체계

┌────────────────────────────────────────────────────────────┐
│ 요구사항 │
│ REQ-SV-CUS-001 │
└─────────────────────────┬──────────────────────────────────┘
▼
┌────────────────────────────────────────────────────────────┐
│ 업무코드·도메인 │
│ SV / Customer │
└─────────────────────────┬──────────────────────────────────┘
▼
┌────────────────────────────────────────────────────────────┐
│ 화면·이벤트 │
│ SV-CUS-0001 / SV-CUS-0001-E01 │
└─────────────────────────┬──────────────────────────────────┘
▼
┌────────────────────────────────────────────────────────────┐
│ ServiceId │
│ SV.Customer.selectSummary │
└─────────────────────────┬──────────────────────────────────┘
▼
┌────────────────────────────────────────────────────────────┐
│ 거래코드 │
│ SV-INQ-0001 │
└─────────────────────────┬──────────────────────────────────┘
▼
┌────────────────────────────────────────────────────────────┐
│ 프로그램 │
│ SvCustomerHandler → Facade → Service → DAO → Mapper │
└─────────────────────────┬──────────────────────────────────┘
▼
┌────────────────────────────────────────────────────────────┐
│ 데이터 │
│ selectSummary → SV\_CUSTOMER\_SUMMARY │
└─────────────────────────┬──────────────────────────────────┘
▼
┌────────────────────────────────────────────────────────────┐
│ 운영 │
│ OM Catalog · 거래통제 · Timeout · 로그 · 감사 · Metric │
└────────────────────────────────────────────────────────────┘

# 핵심 식별자 비교

| 식별자 | 핵심 질문 | 대표 예 | 주요 사용 위치 |
| --- | --- | --- | --- |
| 요구사항 ID | 왜 만드는가 | REQ-SV-CUS-001 | 요구사항·RTM |
| 업무코드 | 누가 소유하는가 | SV | WAR·패키지·라우팅 |
| 도메인 | 어떤 업무 책임인가 | Customer | ServiceId·패키지 |
| 화면 ID | 어느 화면인가 | SV-CUS-0001 | UI·메뉴·권한 |
| 이벤트 ID | 어느 사용자 행동인가 | SV-CUS-0001-E01 | 화면설계·테스트 |
| 기능 ID | 어떤 기능 단위인가 | SV-CUS-0001-F01 | 기능권한·추적성 |
| ServiceId | 어느 Handler를 실행하는가 | SV.Customer.selectSummary | Dispatcher·OM·로그 |
| 거래코드 | 어떤 거래 유형인가 | SV-INQ-0001 | 통제·감사·통계 |
| GUID | 시스템 전체에서 어느 거래인가 | UUID 등 | 전문·거래로그 |
| TraceId | 어느 호출 흐름인가 | Trace 문자열 | 내부·분산 추적 |
| SQL ID | 어느 SQL인가 | selectSummary | Mapper·SQL 로그 |
| DB 객체 | 어느 데이터인가 | SV\_CUSTOMER\_SUMMARY | DB·영향도 분석 |

# 식별자의 생명주기

\[1\] 분석
업무코드·도메인·화면 ID 확정
↓
\[2\] 설계
이벤트 ID·ServiceId·거래코드 정의
↓
\[3\] 구현
Handler·클래스·메서드·Mapper 연결
↓
\[4\] 운영등록
Catalog·거래통제·Timeout·권한 등록
↓
\[5\] 시험
화면–거래–프로그램–SQL 추적 검증
↓
\[6\] 배포
Route·WAR·설정 정합성 확인
↓
\[7\] 운영
GUID·TraceId·ServiceId로 추적
↓
\[8\] 변경·폐기
영향도 분석·호환·등록정보 제거

# 4.1 업무코드와 업무 경계

## 4.1.1 업무코드는 무엇인가

업무코드는 시스템의 상위 업무영역을 식별하는 짧은 코드다.

예를 들어 다음과 같이 구분할 수 있다.

| 업무코드 | 업무영역 | 대표 책임 |
| --- | --- | --- |
| CC | Common | 전사 공통 |
| IC | Integration Customer | 통합 고객 |
| PC | Private Customer | 개인 고객 |
| BC | Business Customer | 기업 고객 |
| MS | Mini Single View | 고객 요약정보 |
| SV | Single View | 고객 단일뷰 |
| PD | Product | 상품 |
| CM | Campaign | 캠페인 |
| EB | EBM | 이벤트 기반 마케팅 |
| EP | Event Processing | 실시간 이벤트 처리 |
| BP | Behavior Processing | 행동 처리 |
| BD | Behavior Data | 행동 데이터 |
| SS | Sales Support | 영업 지원 |
| CS | Common Service | 업무 공통 서비스 |
| CT | Contact | 접촉·상담 이력 |
| MG | Management | 관리 기능 |
| OM | Operation Management | 운영관리 |

실제 적용 업무코드와 의미는 프로젝트의 승인된 업무코드 대장을 기준으로 한다.

## 4.1.2 업무코드는 분류코드보다 넓은 의미를 갖는다

업무코드는 다음 경계를 함께 나타낸다.

업무코드
├─ 업무 책임
├─ 담당 조직
├─ 업무 WAR
├─ Context Path
├─ Java 패키지
├─ ServiceId Prefix
├─ 거래코드 Prefix
├─ DB 객체 Prefix
├─ 로그 분류
├─ 배포·장애 영향
└─ 운영 인수 책임

따라서 업무코드를 정하는 것은 단순 채번 작업이 아니라 아키텍처 경계를 결정하는 작업이다.

## 4.1.3 업무코드와 WAR

일반적인 대응 관계는 다음과 같다.

| 업무코드 | Gradle 모듈 | WAR | Context Path |
| --- | --- | --- | --- |
| SV | sv-service | sv.war | /sv |
| IC | ic-service | ic.war | /ic |
| PC | pc-service | pc.war | /pc |
| MG | mg-service | mg.war | /mg |
| OM | tcf-om 또는 om-service | om.war | /om |

다음 항목이 반드시 일대일일 필요는 없다.

업무코드 1개
≠ 반드시 Tomcat 1개

업무코드 1개
≠ 반드시 서버 1대

업무코드 1개
≠ 반드시 DB Schema 1개

그러나 운영과 변경 책임을 명확히 하려면 업무코드와 업무 애플리케이션의 대응관계가 관리대장에 기록되어야 한다.

## 4.1.4 업무코드와 패키지

권장 업무 패키지는 다음 형식을 사용한다.

com.nh.nsight.marketing.{업무코드소문자}.{도메인}.{계층}

예:

com.nh.nsight.marketing.sv.customer.handler
com.nh.nsight.marketing.sv.customer.facade
com.nh.nsight.marketing.sv.customer.service
com.nh.nsight.marketing.sv.customer.rule
com.nh.nsight.marketing.sv.customer.dao
com.nh.nsight.marketing.sv.customer.mapper

업무코드, 도메인과 계층 순서로 패키지를 구성하면 클래스 전체 경로만으로 소유 업무와 책임을 식별할 수 있다.

## 4.1.5 업무코드와 데이터 소유권

같은 데이터베이스에 테이블이 존재한다고 해서 모든 업무가 자유롭게 조회·변경할 수 있는 것은 아니다.

물리적으로 접근 가능
≠ 업무적으로 소유

### 데이터 소유권 예시

| 데이터 | 소유 업무 | 다른 업무의 권장 접근 |
| --- | --- | --- |
| 고객 기본정보 | IC | IC 공개 ServiceId·Contract |
| 고객 단일뷰 | SV | SV 조회 Contract |
| 상품 기준정보 | PD | PD 공개 ServiceId |
| 캠페인 정보 | CM | CM Contract·Event |
| 접촉 이력 | CT | CT 조회 ServiceId |
| 운영 사용자 | OM | OM 관리 API |

### 금지 구조

SV CustomerService
↓
IC\_CUSTOMER\_MASTER 직접 UPDATE

### 권장 구조

SV
↓ IC.Customer.updateBasic
IC Handler
↓
IC Service
↓
IC 소유 테이블 변경

다른 업무 WAR나 도메인의 Service·DAO·Mapper·테이블을 직접 참조하지 않고 공개 계약을 이용하는 것이 원칙이다.

## 4.1.6 업무코드와 장애 경계

업무코드는 운영 장애의 분류 기준이기도 하다.

| 장애 | 업무코드 활용 |
| --- | --- |
| 특정 ServiceId 오류 | 소유 업무팀 식별 |
| 특정 DB Pool 포화 | 대상 WAR 식별 |
| 배포 오류 | 영향 Context 확인 |
| 느린 SQL | Mapper·업무코드 연결 |
| 거래량 급증 | 업무별 TPS 분석 |
| 오류율 증가 | 업무별 오류코드 분석 |
| 보안사고 | 사용자·업무·거래범위 확인 |

다만 하나의 Tomcat에 여러 WAR를 배포하는 경우 JVM·Heap·GC·Connector Thread를 공유하므로 업무코드는 분리되어도 장애 영역은 공유될 수 있다.

## 4.1.7 업무코드 결정 절차

업무 기능 목록 작성
↓
업무 용어와 규칙 분석
↓
데이터 소유권 분석
↓
담당 조직 분석
↓
배포·장애 격리 요구 분석
↓
도메인과 업무코드 후보 작성
↓
중복·경계 검토
↓
아키텍처 승인
↓
업무코드 대장 등록

### 결정 질문

| 질문 | 판단 기준 |
| --- | --- |
| 별도 업무 책임이 있는가 | 조직·업무 소유자 |
| 독립 변경주기가 있는가 | 릴리스 주기 |
| 독립 배포가 필요한가 | 영향·장애 격리 |
| 데이터 소유권이 다른가 | 변경 권한 |
| 별도 보안 정책이 있는가 | 권한·개인정보 |
| 독립 용량관리가 필요한가 | TPS·Pool·Thread |
| 다른 업무와 호출 계약이 필요한가 | 서비스 경계 |

## 4.1.8 정상 예시와 금지 예시

| 구분 | 정상 예시 | 금지 예시 |
| --- | --- | --- |
| 코드 | SV, IC, CM | A1, TEMP, NEW |
| 의미 | 승인된 업무영역 | 개발자 임의 기능명 |
| 패키지 | marketing.sv.customer | marketing.common.temp |
| 데이터 | 소유 도메인 경유 | 타 업무 테이블 직접 변경 |
| 배포 | 업무코드–WAR 대장 관리 | 같은 코드가 여러 의미로 사용 |
| 로그 | businessCode=SV | 업무코드 누락 |
| 운영 | 업무별 담당조직 등록 | 장애 담당자 불명확 |

## 4.1.9 업무코드 변경 시 영향

업무코드는 여러 식별자의 Prefix이므로 변경 비용이 크다.

업무코드 변경
├─ WAR·Context 변경
├─ 패키지 변경
├─ ServiceId 변경
├─ 거래코드 변경
├─ 화면 ID 변경
├─ DB Prefix 변경 가능성
├─ Gateway Route 변경
├─ OM 기준정보 변경
├─ 권한·메뉴 변경
├─ 로그·대시보드 변경
└─ 연계 계약 변경

운영 이후에는 기존 업무코드를 직접 변경하기보다 신규 코드 도입, 호환 Route 유지와 단계적 폐기 방식을 사용한다.

# 4.2 화면 ID·ServiceId·거래코드

## 4.2.1 세 식별자는 같은 것이 아니다

| 식별자 | 식별 대상 | 핵심 목적 |
| --- | --- | --- |
| 화면 ID | 사용자 화면 | UI 자산·메뉴·권한 |
| ServiceId | 실행할 업무 거래 | Dispatcher 라우팅 |
| 거래코드 | 운영상의 거래 유형 | 통제·감사·통계 |

화면 ID
\= 사용자가 보는 위치

ServiceId
\= 서버에서 실행할 유스케이스

거래코드
\= 운영에서 분류하고 통제할 거래

## 4.2.2 화면 ID

### 권장 형식

{업무코드}-{업무세구분코드}-{4자리 일련번호}

예:

SV-CUS-0001
SV-CUS-0002
CM-MST-0001
OM-USR-0001

### 구성요소

| 구성 | 설명 | 예 |
| --- | --- | --- |
| 업무코드 | 화면 소유 업무 | SV |
| 세구분코드 | 화면 도메인·기능그룹 | CUS |
| 일련번호 | 그룹 내 화면 번호 | 0001 |

### 화면 ID에 넣지 말아야 할 정보

개발자 이름
개발 연도
서버 번호
프로그램 언어
메뉴 순서
변경 가능한 조직명

화면 ID는 메뉴 위치가 바뀌어도 유지되는 안정적인 식별자여야 한다.

## 4.2.3 화면 ID와 메뉴 ID

화면과 메뉴는 다른 개념이다.

| 구분 | 화면 | 메뉴 |
| --- | --- | --- |
| 의미 | 사용자에게 표시되는 UI | 화면에 접근하는 탐색 경로 |
| 관계 | 하나의 화면이 여러 메뉴에서 사용 가능 | 하나의 메뉴가 화면·팝업 호출 |
| 변경 | 화면 기능 변경 | 조직·업무 탐색 변경 |
| 권한 | 기능·화면권한 | 메뉴 노출권한 |
| 식별자 | 화면 ID | 메뉴 ID |

메뉴 이동
≠ 화면 ID 변경

## 4.2.4 이벤트 ID

한 화면에는 여러 사용자 이벤트가 존재한다.

### 권장 형식

{화면ID}-E{2자리 순번}

예:

| 이벤트 ID | 이벤트 |
| --- | --- |
| SV-CUS-0001-E01 | 최초 조회 |
| SV-CUS-0001-E02 | 검색 |
| SV-CUS-0001-E03 | 상세 행 선택 |
| SV-CUS-0001-E04 | 메모 저장 |
| SV-CUS-0001-E05 | 엑셀 다운로드 |

이벤트 ID는 화면 버튼명과 동일할 필요는 없다. 버튼명이 바뀌어도 업무 행위가 같으면 이벤트 ID를 유지할 수 있다.

## 4.2.5 기능 ID

기능권한을 세밀하게 관리할 때 기능 ID를 사용할 수 있다.

{화면ID}-F{2자리 순번}

예:

| 기능 ID | 기능 |
| --- | --- |
| SV-CUS-0001-F01 | 고객요약 조회 |
| SV-CUS-0001-F02 | 고객메모 변경 |
| SV-CUS-0001-F03 | 엑셀 다운로드 |

이벤트와 기능은 반드시 일대일은 아니다.

하나의 기능
→ 여러 UI 이벤트로 실행 가능

하나의 UI 이벤트
→ 여러 기능·ServiceId를 조합 가능

## 4.2.6 ServiceId

ServiceId는 Dispatcher가 실행할 Handler를 찾는 논리적 거래 식별자다.

### 권장 형식

{업무코드}.{도메인}.{행위}

예:

SV.Customer.selectSummary
SV.Customer.selectList
SV.Customer.updateMemo
CM.Campaign.create
CM.Campaign.approve
OM.User.lock
OM.Timeout.updatePolicy

### 구성요소

| 구성 | 기준 | 예 |
| --- | --- | --- |
| 업무코드 | 승인된 대문자 코드 | SV |
| 도메인 | 업무 명사·PascalCase | Customer |
| 행위 | 표준 동사·lowerCamelCase | selectSummary |

## 4.2.7 ServiceId의 역할

화면 요청
↓
StandardHeader.serviceId
↓
STF 검증
↓
거래통제·Timeout 정책 조회
↓
TransactionDispatcher
↓
등록된 Handler 탐색
↓
업무 처리
↓
거래로그·Metric 기록

ServiceId는 소스 내부 라우팅뿐 아니라 운영 정책의 연결키로 사용된다.

## 4.2.8 ServiceId 표준 동사

| 유형 | 권장 동사 | 설명 |
| --- | --- | --- |
| 단건 조회 | select, get | 단건 결과 |
| 목록 조회 | selectList, search | 조건 목록 |
| 요약 조회 | selectSummary | 조합·집계 결과 |
| 등록 | create, register | 신규 생성 |
| 변경 | update, modify | 기존 데이터 변경 |
| 삭제 | delete, remove | 물리·논리 삭제 |
| 승인 | approve | 승인 상태 전이 |
| 반려 | reject | 반려 상태 전이 |
| 취소 | cancel | 업무 취소 |
| 실행 | execute, run | 처리 실행 |
| 검증 | validate, check | 업무 검증 |
| 다운로드 | download, export | 파일 제공 |
| 업로드 | upload, import | 파일 수신 |

다음처럼 의미가 불명확한 동사는 피한다.

process
handle
doWork
executeTask
manage
runData

업무 의미가 명확한 경우에만 사용한다.

## 4.2.9 거래코드

거래코드는 운영 통제·감사·통계·보고를 위한 거래 분류 코드다.

### 권장 형식

{업무코드}-{처리유형}-{4자리 일련번호}

예:

SV-INQ-0001
SV-REG-0001
SV-UPD-0001
SV-DEL-0001
CM-APR-0001
OM-CTL-0001

### 처리유형 예시

| 코드 | 의미 |
| --- | --- |
| INQ | 조회 |
| REG | 등록 |
| UPD | 변경 |
| DEL | 삭제 |
| APR | 승인 |
| REJ | 반려 |
| CNL | 취소 |
| DWL | 다운로드 |
| UPL | 업로드 |
| BAT | 배치 |
| CTL | 운영 통제 |

실제 처리유형 코드 사전은 프로젝트 표준표를 기준으로 관리한다.

## 4.2.10 ServiceId와 거래코드 차이

| 항목 | ServiceId | 거래코드 |
| --- | --- | --- |
| 목적 | 실행할 Handler 탐색 | 운영 거래 분류 |
| 형식 | 의미 기반 문자열 | 코드 기반 |
| 사용 위치 | Header·Dispatcher | Header·OM·감사·통계 |
| 중복 | 절대 금지 | 관리 단위에 따라 금지 |
| 변경 영향 | 프로그램 라우팅 영향 | 운영·통계 영향 |
| 예 | SV.Customer.selectSummary | SV-INQ-0001 |

ServiceId가 같고 거래코드만 다른 경우
→ 채널·감사 분류가 다른지 설계 검토 필요

거래코드가 같고 ServiceId가 여러 개인 경우
→ 동일 운영분류인지 명확한 근거 필요

기본적으로는 하나의 ServiceId에 대표 거래코드 하나를 대응시키는 편이 추적하기 쉽다.

## 4.2.11 화면과 ServiceId 관계

### 1:1 관계

화면 조회 버튼
↓
SV.Customer.selectSummary

### 1:N 관계

고객 종합화면 최초 진입
├─ SV.Customer.selectSummary
├─ CT.Contact.selectRecentList
└─ PD.Product.selectHoldingList

### N:1 관계

고객조회 화면
고객상담 팝업
캠페인 대상자 화면
└─ IC.Customer.selectBasic

따라서 화면 ID와 ServiceId를 단순 일대일 컬럼으로만 관리하면 안 된다.

## 4.2.12 화면–거래 추적성 매트릭스

| 화면 ID | 이벤트 ID | 기능 | ServiceId | 거래코드 | 순서 | 호출방식 |
| --- | --- | --- | --- | --- | --- | --- |
| SV-CUS-0001 | E01 | 고객요약 조회 | SV.Customer.selectSummary | SV-INQ-0001 | 1 | 동기 |
| SV-CUS-0001 | E01 | 접촉이력 조회 | CT.Contact.selectRecentList | CT-INQ-0003 | 2 | 동기 |
| SV-CUS-0001 | E02 | 고객목록 검색 | SV.Customer.selectList | SV-INQ-0002 | 1 | 동기 |
| SV-CUS-0001 | E04 | 고객메모 변경 | SV.Customer.updateMemo | SV-UPD-0001 | 1 | 동기 |
| SV-CUS-0001 | E05 | 엑셀 다운로드 | SV.Customer.exportList | SV-DWL-0001 | 1 | 파일 |

화면 이벤트, ServiceId, 프로그램과 데이터 처리 경로를 하나의 추적성 매트릭스로 관리해야 변경 영향과 테스트 누락을 방지할 수 있다.

## 4.2.13 ServiceId와 Handler 등록

현재 구조에서는 Handler가 하나 이상의 ServiceId를 등록할 수 있다.

@Component
@RequiredArgsConstructor
public class SvCustomerHandler
implements TransactionHandler {

private final SvCustomerFacade facade;

@Override
public Set<String> serviceIds() {
return Set.of(
"SV.Customer.selectSummary",
"SV.Customer.selectList",
"SV.Customer.updateMemo"
);
}

@Override
public Object doHandle(StandardRequest request) {
return switch (request.header().serviceId()) {
case "SV.Customer.selectSummary" ->
facade.selectSummary(request.body());

case "SV.Customer.selectList" ->
facade.selectList(request.body());

case "SV.Customer.updateMemo" ->
facade.updateMemo(request.body());

default ->
throw new BusinessException(
"TCF-SERVICE-0001",
"지원하지 않는 ServiceId입니다."
);
};
}
}

### 등록 원칙

| 원칙 | 기준 |
| --- | --- |
| 도메인 응집 | 같은 도메인의 거래를 함께 등록 |
| 과도한 집중 금지 | 한 Handler에 무관한 도메인 혼합 금지 |
| 중복 금지 | 두 Handler가 같은 ServiceId 등록 금지 |
| 명시적 분기 | Default 오류 처리 |
| 업무 위임 | Handler는 Facade 호출만 수행 |

Dispatcher는 기동 시 Handler 등록정보를 수집하며, 중복 ServiceId가 발견되면 애플리케이션 기동을 실패시키는 것이 안전하다.

## 4.2.14 StandardHeader의 주요 식별정보

NSIGHT TCF의 표준 Header는 거래 실행과 추적에 필요한 정보를 전달한다.

| 필드 | 의미 | 생성·검증 주체 |
| --- | --- | --- |
| systemId | 호출 시스템 | UI·Gateway·TCF |
| businessCode | 업무코드 | URL·Header·STF |
| serviceId | 실행 거래 | 화면·Dispatcher |
| serviceName | 표시용 서비스명 | Catalog |
| transactionCode | 거래코드 | 화면·OM |
| processingType | 조회·변경 유형 | 거래설계 |
| guid | 전역 거래 ID | STF |
| traceId | 호출 추적 ID | Gateway·STF |
| channelId | 채널 | JWT·Gateway |
| userId | 사용자 | 인증 문맥 |
| branchId | 지점 | 인증 문맥 |
| centerId | 센터 | 운영환경 |
| requestTime | 요청시각 | 클라이언트·TCF |
| clientIp | 요청 IP | Controller |
| idempotencyKey | 중복방지 키 | 화면·호출자 |

클라이언트가 전달한 사용자·지점 정보보다 검증된 인증 Claim과 서버 문맥을 우선 신뢰해야 한다.

## 4.2.15 신규 거래 등록 3종 세트

코드에 Handler만 추가해서는 운영 가능한 거래가 완성되지 않는다.

1\. Service Catalog
2\. Transaction Control
3\. Timeout Policy

| 등록정보 | 역할 |
| --- | --- |
| Service Catalog | ServiceId·업무·담당자·사용 여부 |
| 거래통제 | 실행 허용·차단·기간·채널 |
| Timeout Policy | 최대 처리시간 |
| 권한 | 역할·메뉴·기능 접근 |
| 오류코드 | 사용자·운영 메시지 |
| 감사정책 | 중요 조회·변경 기록 |
| Metric 기준 | 거래량·오류율·응답시간 |

신규 거래는 Handler 구현과 함께 OM Catalog, 거래통제와 Timeout 정책을 등록해야 운영에서 실행·차단·추적할 수 있다.

## 4.2.16 금지 예시

| 금지 예 | 문제 |
| --- | --- |
| SV.test1 | 도메인·행위 불명확 |
| SV.Customer.doProcess | 행위 의미 불명확 |
| customer.select | 업무코드 누락 |
| SV.Customer.SelectSummary | 동사 표기 불일치 |
| SV-CUST-조회-1 | 혼합 언어·형식 |
| 화면 ID를 ServiceId로 사용 | UI·실행 식별자 혼용 |
| 거래코드로 Handler 탐색 | 책임 혼동 |
| URL을 ServiceId로 사용 | 배포 경로 변경에 취약 |
| 코드에만 ServiceId 추가 | OM 통제·Timeout 누락 |
| Handler 중복 등록 | 기동·라우팅 충돌 |

# 4.3 패키지·클래스·메서드 명명규칙

## 4.3.1 명명규칙의 목적

명명규칙은 문법 통일보다 다음 목적을 갖는다.

이름만 보고
├─ 업무코드
├─ 도메인
├─ 프로그램 계층
├─ 데이터 전달 방향
├─ 실행 행위
├─ 소유 조직
└─ 변경 영향
을 판단할 수 있게 한다.

## 4.3.2 최상위 패키지

전체 ROOT
com.nh.nsight

TCF 플랫폼
com.nh.nsight.tcf

NSIGHT 업무
com.nh.nsight.marketing

### 플랫폼 패키지

com.nh.nsight.tcf.{모듈}.{책임}

예:

com.nh.nsight.tcf.core.processor
com.nh.nsight.tcf.core.dispatcher
com.nh.nsight.tcf.core.timeout
com.nh.nsight.tcf.web.controller
com.nh.nsight.tcf.web.security
com.nh.nsight.tcf.gateway.route
com.nh.nsight.tcf.jwt.issue
com.nh.nsight.tcf.om.catalog

## 4.3.3 업무 패키지

com.nh.nsight.marketing.{업무코드}.{도메인}.{계층}

예:

com.nh.nsight.marketing.sv.customer.handler
com.nh.nsight.marketing.sv.customer.facade
com.nh.nsight.marketing.sv.customer.service
com.nh.nsight.marketing.sv.customer.rule
com.nh.nsight.marketing.sv.customer.dao
com.nh.nsight.marketing.sv.customer.mapper

### 패키지 작성 기준

| 항목 | 기준 |
| --- | --- |
| 소문자 | Java 패키지는 소문자 |
| 단수형 | 가능한 한 도메인 단수형 |
| 영문 | 한글·특수문자 금지 |
| 축약 통제 | 승인된 업무 약어만 사용 |
| 계층 고정 | handler, facade, service, rule, dao, mapper |
| 내부 구현 | 외부 도메인에 공개하지 않음 |

## 4.3.4 패키지 금지 예시

com.nh.nsight.common
com.nh.nsight.util2
com.nh.nsight.temp
com.nh.nsight.marketing.sv.impl
com.nh.nsight.marketing.sv.service.common
com.nh.nsight.marketing.customer.sv

### 금지 사유

| 이름 | 문제 |
| --- | --- |
| common | 소유권 불명확 |
| util2 | 의미·버전관리 불명확 |
| temp | 폐기·운영 기준 없음 |
| impl | 구현 책임이 드러나지 않음 |
| service.common | 도메인보다 기술계층 우선 |
| customer.sv | 업무코드·도메인 순서 불일치 |

## 4.3.5 클래스 명명규칙

### 계층별 Suffix

| 계층 | 형식 | 예 |
| --- | --- | --- |
| Handler | {업무}{도메인}Handler | SvCustomerHandler |
| Facade | {업무}{도메인}Facade | SvCustomerFacade |
| Service | {업무}{도메인}Service | SvCustomerService |
| Rule | {업무}{도메인}{목적}Rule | SvCustomerUpdateRule |
| DAO | {업무}{도메인}Dao | SvCustomerDao |
| Mapper | {업무}{도메인}Mapper | SvCustomerMapper |
| Client | {대상}{도메인}Client | IcCustomerClient |
| Adapter | {대상}{목적}Adapter | DataEyeCustomerAdapter |
| Configuration | {영역}Configuration | SvMyBatisConfiguration |
| Properties | {영역}Properties | TcfTimeoutProperties |
| Exception | {영역}Exception | BusinessException |

## 4.3.6 클래스 이름에 업무코드를 넣는 기준

업무코드 Prefix를 클래스명에 포함하면 Stack Trace와 검색에서 소유 업무를 빠르게 식별할 수 있다.

SvCustomerService
IcCustomerService
CmCampaignService

그러나 패키지가 이미 명확하고 클래스명 중복 가능성이 낮은 경우 도메인명 중심으로 단순화할 수도 있다.

com.nh.nsight.marketing.sv.customer.service.CustomerService

프로젝트 내에서 두 방식을 혼용하지 말고 하나의 기준을 승인한다.

### 권장 판단

| 상황 | 권장 |
| --- | --- |
| 여러 업무 모듈 소스를 함께 검색 | 업무 Prefix 포함 |
| 로그·Stack Trace 가독성 중요 | 업무 Prefix 포함 |
| 모듈 경계가 엄격하고 패키지가 항상 표시 | Prefix 생략 검토 |
| 같은 이름 Bean 충돌 가능 | Prefix 포함 |

## 4.3.7 메서드 명명규칙

메서드는 업무 행위를 동사로 시작한다.

| 목적 | 권장 메서드 |
| --- | --- |
| 단건 조회 | selectCustomer() |
| 목록 조회 | selectCustomerList() |
| 요약 조회 | selectCustomerSummary() |
| 존재 확인 | existsCustomer() |
| 건수 조회 | countCustomer() |
| 등록 | createCustomer() |
| 변경 | updateCustomer() |
| 삭제 | deleteCustomer() |
| 승인 | approveCampaign() |
| 취소 | cancelCampaign() |
| 검증 | validateCustomer() |
| 내보내기 | exportCustomerList() |

### Boolean 반환 메서드

isActive()
hasPermission()
canApprove()
existsCustomer()

### 금지 이름

doIt()
process()
run()
execute()
work()
handleData()
method1()
test()

단, 기술 프레임워크가 정한 process(), handle() 같은 공통 메서드는 예외다.

## 4.3.8 ServiceId와 메서드 이름

ServiceId의 행위와 Facade 메서드는 가능한 한 같은 의미를 사용한다.

| ServiceId | Facade 메서드 |
| --- | --- |
| SV.Customer.selectSummary | selectSummary() |
| SV.Customer.selectList | selectList() |
| SV.Customer.updateMemo | updateMemo() |
| CM.Campaign.approve | approve() |

ServiceId 행위
↔ Handler 분기
↔ Facade 메서드
↔ Service 메서드

완전히 동일한 문자열을 강제할 필요는 없지만 의미가 달라지면 안 된다.

## 4.3.9 DTO 명명규칙

| DTO 유형 | 형식 | 예 |
| --- | --- | --- |
| 요청 | {유스케이스}Request | CustomerSummaryRequest |
| 응답 | {유스케이스}Response | CustomerSummaryResponse |
| 조회조건 | {유스케이스}Query | CustomerSummaryQuery |
| 변경명령 | {유스케이스}Command | UpdateCustomerMemoCommand |
| DB 결과 | {유스케이스}Result | CustomerSummaryResult |
| 연계 요청 | {대상}{유스케이스}Request | IcCustomerBasicRequest |
| 이벤트 | {업무사실}Event | CustomerGradeChangedEvent |

### 금지

CustomerDto
CustomerData
CustomerInfo2
CustomerVO
Map<String, Object> 전 계층 사용

VO라는 이름은 Value Object인지 조회 결과 객체인지 의미가 혼동되므로 프로젝트 내 정의 없이 사용하지 않는다.

## 4.3.10 Mapper 명명규칙

### Interface

{업무}{도메인}Mapper

### Namespace

Java Mapper Interface 전체 패키지명

### SQL ID

| 유형 | 예 |
| --- | --- |
| 단건 조회 | selectCustomer |
| 목록 조회 | selectCustomerList |
| 건수 조회 | countCustomer |
| 등록 | insertCustomer |
| 변경 | updateCustomer |
| 삭제 | deleteCustomer |
| Merge | mergeCustomer |

SQL ID는 업무 의미를 드러내야 한다.

select1
selectData
queryCustomer
sql001

같은 이름은 금지한다.

## 4.3.11 DB 객체 명명 연계

DB 객체는 대문자와 언더스코어를 사용하고 업무·운영 영역 Prefix를 적용한다.

| 객체 | 예 |
| --- | --- |
| 업무 테이블 | SV\_CUSTOMER\_SUMMARY |
| 운영 테이블 | OM\_SERVICE\_CATALOG |
| 거래로그 | TCF\_TX\_LOG |
| 컬럼 | SERVICE\_ID, TRACE\_ID |
| 인덱스 | IDX\_TCF\_TX\_LOG\_01 |
| PK | PK\_OM\_SERVICE\_CATALOG |

DB 객체도 ServiceId, Mapper와 로그의 추적 관계를 고려해 명명해야 한다.

## 4.3.12 오류코드 명명

권장 형식 예:

{영역}-{도메인}-{4자리 번호}

또는 프로젝트 표준에 따라:

E-{영역}-{세부영역}-{4자리}

예:

SV-CUSTOMER-0001
TCF-HEADER-0001
E-JWT-AUTH-0001
OM-CATALOG-0001

오류코드는 다음을 구분해야 한다.

| 구분 | 예 |
| --- | --- |
| 입력 오류 | 필수값·형식 |
| 업무 오류 | 상태·중복·업무 거절 |
| 권한 오류 | 접근·기능권한 |
| 시스템 오류 | DB·프로그램·네트워크 |
| Timeout | 실행시간 초과 |
| 연계 오류 | 외부 시스템 실패 |

## 4.3.13 설정 키 명명

Spring 설정은 계층적 Prefix를 사용한다.

nsight:
tcf:
transaction:
default-timeout-ms: 3000
jwt:
enabled: true
jwks-uri: ...
logging:
request-body-enabled: false

### 기준

| 항목 | 기준 |
| --- | --- |
| Prefix | nsight.tcf |
| 단어 구분 | kebab-case |
| 단위 표시 | \-ms, -seconds, -bytes |
| Boolean | enabled, required |
| 환경값 | Secret 평문 금지 |
| 기본값 | 코드·문서 일치 |

# 4.4 소스·로그·운영 설정을 연결하는 식별자

## 4.4.1 식별자가 연결되어야 하는 이유

서비스가 정상적으로 실행되더라도 운영자가 거래를 찾지 못하면 운영 가능한 시스템이라고 보기 어렵다.

기능 성공
\+ 로그 추적
\+ 운영 통제
\+ 영향도 분석
\+ 재현 가능한 테스트
\= 운영 완료

## 4.4.2 연결해야 할 네 영역

\[설계\]
화면 ID·이벤트 ID·ServiceId·거래코드
↕
\[소스\]
Handler·Facade·Service·Mapper·SQL ID
↕
\[운영 설정\]
Catalog·거래통제·Timeout·권한
↕
\[실행 증적\]
GUID·TraceId·로그·Metric·감사로그

## 4.4.3 GUID

GUID는 거래를 시스템 전체에서 식별하기 위한 전역 거래 ID다.

화면 요청
↓ GUID 생성 또는 전달
Gateway
↓
업무 WAR
↓
내부 연계
↓
DB·로그·감사

### GUID 기준

| 항목 | 기준 |
| --- | --- |
| 유일성 | 거래마다 고유 |
| 불변성 | 호출 경로에서 유지 |
| 재사용 | 재시도 정책에 따라 별도 정의 |
| 로그 | 모든 주요 단계 기록 |
| 응답 | 오류 추적용으로 제공 가능 |
| 개인정보 | 포함 금지 |

## 4.4.4 TraceId

TraceId는 하나의 호출 흐름을 연결한다.

GUID와 TraceId가 동일하게 사용되는 프로젝트도 있지만 의미를 구분하는 것이 좋다.

| 식별자 | 범위 |
| --- | --- |
| GUID | 업무 거래의 전역 식별 |
| TraceId | 기술 호출 흐름 |
| Span ID | Trace 내부 개별 호출 |
| Idempotency Key | 중복 방지 기준 |

### 예시

GUID: G-20260718-00000001
TraceId: 8bfa...
├─ Span: Gateway
├─ Span: SV Service
├─ Span: IC 호출
└─ Span: DB SQL

## 4.4.5 Idempotency Key

Idempotency Key는 사용자가 동일한 변경 요청을 반복했을 때 중복 반영을 막는 식별자다.

동일 사용자
\+ 동일 업무 목적
\+ 동일 Idempotency Key
\= 한 번만 처리

### 사용 대상

| 거래 | 적용 |
| --- | --- |
| 단순 조회 | 일반적으로 불필요 |
| 등록 | 권장 |
| 이체·발송·캠페인 실행 | 필수 검토 |
| 파일 업로드 | 권장 |
| 외부 시스템 호출 | 상대 정책과 함께 검토 |
| 배치 실행 | Job Instance·Lock 별도 적용 |

## 4.4.6 MDC 로그 문맥

TCF는 거래 시작 시 주요 식별자를 MDC에 등록해 모든 로그에 자동 포함시키는 구조가 적절하다.

guid
traceId
businessCode
serviceId
transactionCode
userId
branchId
channelId

### 로그 예시

2026-07-18 10:31:02.115
INFO
guid=G-20260718-000001
traceId=9af03...
businessCode=SV
serviceId=SV.Customer.selectSummary
transactionCode=SV-INQ-0001
userId=U10293
branchId=B120
step=HANDLER\_START
message="고객요약 조회 시작"

로그는 구조화된 필드로 기록해야 검색·집계가 가능하다.

## 4.4.7 단계 식별자

거래의 현재 위치를 알기 위해 처리단계를 기록할 수 있다.

| 단계 코드 | 의미 |
| --- | --- |
| REQUEST\_RECEIVED | HTTP 요청 수신 |
| AUTH\_VALIDATED | 인증 완료 |
| STF\_COMPLETED | 전처리 완료 |
| HANDLER\_START | Handler 시작 |
| SERVICE\_START | 업무 처리 시작 |
| DB\_WAIT | Connection 대기 |
| SQL\_EXECUTE | SQL 실행 |
| EXTERNAL\_CALL | 외부 연계 |
| ETF\_SUCCESS | 정상 종료 |
| ETF\_BUSINESS\_FAIL | 업무 오류 종료 |
| ETF\_SYSTEM\_ERROR | 시스템 오류 종료 |
| TIMEOUT | 제한시간 초과 |

이 정보는 운영진단에서 ServiceId와 SQL 지연 원인을 연결하는 데 활용할 수 있다.

## 4.4.8 거래로그 필수 항목

| 분류 | 항목 |
| --- | --- |
| 식별 | GUID, TraceId |
| 업무 | businessCode, serviceId, transactionCode |
| 사용자 | userId, branchId, channelId |
| 시스템 | host, instance, WAR, version |
| 시간 | requestTime, startTime, endTime |
| 결과 | status, resultCode, errorCode |
| 성능 | elapsedMs |
| 연계 | targetSystem, childServiceId |
| 데이터 | 영향 건수, 파일 건수 |
| 보안 | 마스킹·감사 대상 여부 |

요청·응답 Body 원문 전체를 거래로그에 저장하는 것은 개인정보와 용량 위험이 있으므로 별도 정책이 필요하다.

## 4.4.9 애플리케이션 로그·거래로그·감사로그 차이

| 로그 | 목적 | 주요 항목 |
| --- | --- | --- |
| 애플리케이션 로그 | 기술 문제 분석 | 클래스·예외·단계 |
| 거래로그 | 거래 상태·성능 | ServiceId·결과·시간 |
| 감사로그 | 중요 행위 증명 | 사용자·대상·변경 전후 |
| 보안로그 | 인증·접근 추적 | 토큰·권한 결과 |
| SQL 로그 | DB 지연 분석 | Mapper ID·시간 |
| 연계로그 | 시스템 간 호출 | 대상·응답·Timeout |

하나의 로그가 모든 목적을 대신할 수 없다.

## 4.4.10 민감정보 로그 금지

다음 정보는 로그에 원문으로 기록하지 않는다.

비밀번호
Access Token
Refresh Token
Private Key
주민등록번호
계좌번호 전체
카드번호 전체
인증서 원문
개인정보 요청 Body 전체

필요한 경우 마스킹·Hash·부분식별자를 사용한다.

## 4.4.11 OM Service Catalog

Service Catalog는 운영 대상 거래의 기준정보다.

| 속성 | 예 |
| --- | --- |
| ServiceId | SV.Customer.selectSummary |
| 서비스명 | 고객 종합정보 조회 |
| 업무코드 | SV |
| 도메인 | Customer |
| 거래코드 | SV-INQ-0001 |
| 사용 여부 | Y |
| 담당팀 | SV 업무팀 |
| 중요도 | 중요 |
| 기본 Timeout | 3초 |
| 감사 여부 | 조회 감사 |
| 버전 | 1.0 |

## 4.4.12 거래통제

거래통제는 특정 ServiceId를 운영 중 실행 허용·차단하는 정책이다.

ServiceId
\+ 업무코드
\+ 채널
\+ 지점
\+ 적용기간
\+ 허용 여부

### 사용 사례

| 상황 | 통제 |
| --- | --- |
| 장애 거래 | ServiceId 차단 |
| 특정 채널 장애 | 채널별 차단 |
| 점검시간 | 기간 차단 |
| 신규 기능 | 제한 사용자만 허용 |
| 보안사고 | 사용자·지점 차단 |

## 4.4.13 Timeout 정책

Timeout 정책도 ServiceId와 연결된다.

| ServiceId | 기본 Timeout |
| --- | --- |
| SV.Customer.selectSummary | 3초 |
| SV.Customer.selectList | 3초 |
| CM.Campaign.create | 5초 |
| SV.Customer.exportList | 온라인 제한 또는 비동기 전환 |

Timeout은 단순 숫자가 아니다.

화면 대기시간
≥ Gateway Timeout
≥ TCF 거래 Timeout
≥ 외부연계 Timeout
≥ DB Query Timeout

각 계층의 Timeout 예산을 일관되게 설계해야 한다.

## 4.4.14 소스–운영 기준정보 대조표

| 소스·설계 | 운영정보 | 불일치 시 결과 |
| --- | --- | --- |
| Handler ServiceId | Catalog ServiceId | 미등록·실행 실패 |
| Header 거래코드 | 거래통제 거래코드 | 통제 누락 |
| Facade Timeout | OM Timeout | 실제 적용 불명확 |
| 업무코드 패키지 | Catalog 업무코드 | 담당·통계 오류 |
| 화면–ServiceId | 권한 등록 | 화면 접근 후 거래 실패 |
| 오류코드 Enum | OM 오류코드 | 메시지 불일치 |
| Gateway Route | WAR Context | 404·오라우팅 |
| Mapper SQL ID | SQL 로그 | 장애 추적 실패 |

## 4.4.15 정방향 추적

화면 ID
↓
이벤트 ID
↓
ServiceId
↓
Handler
↓
Facade
↓
Service
↓
DAO
↓
Mapper
↓
SQL ID
↓
Table

### 정방향 활용

| 목적 | 활용 |
| --- | --- |
| 신규 기능 개발 | 필요한 프로그램 식별 |
| 테스트 작성 | 화면 이벤트별 경로 |
| 운영 등록 | ServiceId 정책 등록 |
| 성능 분석 | SQL·외부 호출 확인 |
| 감사 | 사용자 행위 추적 |

## 4.4.16 역방향 추적

Table
↓
Mapper SQL
↓
DAO
↓
Service
↓
Facade
↓
Handler
↓
ServiceId
↓
화면 이벤트
↓
사용자·메뉴

### 역방향 활용

| 변경 | 확인 대상 |
| --- | --- |
| 컬럼 변경 | 영향 SQL·DTO·화면 |
| 인덱스 변경 | 영향 ServiceId 성능 |
| Mapper 변경 | 호출 Service 목록 |
| Service 변경 | 영향 화면·연계 |
| ServiceId 폐기 | 화면·권한·OM 등록 |
| 업무코드 변경 | 전체 식별체계 |

## 4.4.17 추적성 매트릭스 예시

| 요구사항 | 화면 이벤트 | ServiceId | Handler | Facade | Mapper | Table | 테스트 |
| --- | --- | --- | --- | --- | --- | --- | --- |
| REQ-SV-001 | SV-CUS-0001-E01 | SV.Customer.selectSummary | SvCustomerHandler | selectSummary | selectSummary | SV\_CUSTOMER\_SUMMARY | TC-SV-001~005 |
| REQ-SV-002 | SV-CUS-0001-E04 | SV.Customer.updateMemo | SvCustomerHandler | updateMemo | updateMemo | SV\_CUSTOMER\_MEMO | TC-SV-006~012 |
| REQ-SV-003 | SV-CUS-0001-E05 | SV.Customer.exportList | SvCustomerHandler | exportList | selectExportList | SV\_CUSTOMER\_SUMMARY | TC-SV-013~018 |

# 책임 경계와 RACI

| 활동 | BA·업무 | 아키텍처 | UI팀 | 업무개발 | FW팀 | OM·운영 | 품질 |
| --- | --- | --- | --- | --- | --- | --- | --- |
| 업무코드 정의 | R | A | I | C | C | C | I |
| 도메인 정의 | R | A | C | C | C | I | I |
| 화면 ID 채번 | R | A | R | I | I | C | C |
| ServiceId 정의 | C | A | C | R | C | C | C |
| 거래코드 정의 | C | A | I | R | C | R | C |
| Handler 구현 | I | C | I | R/A | C | I | C |
| OM Catalog 등록 | C | C | I | R | C | A | C |
| 거래통제·Timeout | C | A | I | C | C | R | C |
| 추적성 매트릭스 | R | A | C | R | C | C | C |
| 자동검증 | I | A | I | C | R | C | R |

# 정상 처리 흐름

화면 이벤트 발생
↓
화면 ID·이벤트 ID 확인
↓
표준 Header에 업무코드·ServiceId·거래코드 설정
↓
Gateway·JWT Filter 인증
↓
STF가 Header와 정책 검증
↓
Dispatcher가 ServiceId로 Handler 탐색
↓
Handler가 Facade 호출
↓
업무 처리·SQL 실행
↓
ETF 정상 종료
↓
거래로그에 GUID·ServiceId·거래코드 기록
↓
화면에 표준 응답 반환

# 오류·Timeout·장애 흐름

## 미등록 ServiceId

화면 요청
↓
Dispatcher 검색
↓
Handler 없음
↓
SERVICE\_NOT\_FOUND
↓
ETF 업무 실패 응답
↓
ServiceId·GUID 로그 기록

## 중복 ServiceId

애플리케이션 기동
↓
Handler Registry 생성
↓
동일 ServiceId 2건 발견
↓
기동 실패
↓
배포 차단

## Catalog 미등록

Handler는 존재
↓
OM Catalog·거래통제 조회 실패
↓
거래 차단 또는 기본정책 적용
↓
개발 성공·운영 실패

기본정책 적용 여부는 프로젝트 정책으로 명확히 정해야 한다. 운영 안전을 우선하면 미등록 거래를 차단하는 방식이 적절하다.

## Timeout 정책 불일치

코드 Timeout 5초
OM Timeout 3초
DB Query Timeout 10초
↓
TCF에서 3초 후 응답 종료
↓
DB 작업이 계속되는지 확인 필요
↓
취소·Rollback·Connection 반환 검증

## 로그 식별자 누락

오류 발생
↓
GUID 또는 ServiceId 없음
↓
SQL·사용자·화면 연결 불가
↓
장애 원인과 영향범위 확인 지연

# 정상 예시

{
"header": {
"systemId": "NSIGHT-MP",
"businessCode": "SV",
"serviceId": "SV.Customer.selectSummary",
"transactionCode": "SV-INQ-0001",
"processingType": "INQUIRY",
"channelId": "WEBTOP",
"userId": "U10293",
"branchId": "B120",
"idempotencyKey": null
},
"body": {
"customerNo": "C000001234"
}
}

### 정상 연결

화면 SV-CUS-0001
이벤트 SV-CUS-0001-E01
ServiceId SV.Customer.selectSummary
거래코드 SV-INQ-0001
Handler SvCustomerHandler
Facade SvCustomerFacade.selectSummary()
Mapper SvCustomerMapper.selectSummary()
Table SV\_CUSTOMER\_SUMMARY
Catalog 등록 완료
Timeout 3초
로그 GUID·TraceId·ServiceId 기록

# 금지 예시

{
"header": {
"businessCode": "A",
"serviceId": "test",
"transactionCode": "001",
"userId": "admin"
},
"body": {
"sql": "SELECT \* FROM CUSTOMER"
}
}

### 문제

| 항목 | 문제 |
| --- | --- |
| 업무코드 | 승인되지 않은 코드 |
| ServiceId | 도메인·행위 없음 |
| 거래코드 | 업무·처리유형 식별 불가 |
| 사용자 | 인증 문맥보다 클라이언트 값 신뢰 |
| Body | SQL 직접 전달 |
| 추적성 | 화면·Handler·OM 연결 불가 |
| 보안 | SQL Injection·권한 우회 위험 |

# 자동검증 및 품질 Gate

## 1\. 형식 검증

| 대상 | 검증 예 |
| --- | --- |
| 업무코드 | \[A-Z\]{2} |
| 화면 ID | \[A-Z\]{2}-\[A-Z0-9\]{2,5}-\[0-9\]{4} |
| 이벤트 ID | {화면ID}-E\[0-9\]{2} |
| ServiceId | \[A-Z\]{2}\\.\[A-Z\]\[A-Za-z0-9\]\*\\.\[a-z\]\[A-Za-z0-9\]\* |
| 거래코드 | \[A-Z\]{2}-\[A-Z\]{3}-\[0-9\]{4} |
| 오류코드 | 프로젝트 오류코드 정규식 |

정규식은 형식만 검증한다. 의미·중복·소유권은 별도 검증이 필요하다.

## 2\. ServiceId 자동검증

소스 Handler의 serviceIds()
↕
OM Service Catalog
↕
거래통제
↕
Timeout Policy
↕
화면–ServiceId 매트릭스

### 차단 조건

| 조건 | 결과 |
| --- | --- |
| Handler 중복 ServiceId | Build 실패 |
| Handler만 있고 Catalog 없음 | 배포 실패 |
| Catalog만 있고 Handler 없음 | 배포 실패 |
| Timeout 정책 없음 | 경고 또는 실패 |
| 화면 연결 없는 신규 ServiceId | 검토 대상 |
| 사용 중 화면에 폐기 ServiceId | 배포 실패 |

## 3\. ArchUnit 검증

@ArchTest
static final ArchRule handler\_names =
classes()
.that().resideInAPackage("..handler..")
.should().haveSimpleNameEndingWith("Handler");

@ArchTest
static final ArchRule service\_names =
classes()
.that().resideInAPackage("..service..")
.should().haveSimpleNameEndingWith("Service");

@ArchTest
static final ArchRule mapper\_names =
classes()
.that().resideInAPackage("..mapper..")
.should().haveSimpleNameEndingWith("Mapper");

## 4\. Mapper 자동검증

| 검증 | 기준 |
| --- | --- |
| Namespace | Interface 전체 경로와 일치 |
| SQL ID | Interface 메서드와 일치 |
| 미사용 SQL | 호출 여부 확인 |
| 중복 ID | Namespace 내 금지 |
| Table Prefix | 업무 소유권 점검 |
| Result Mapping | DTO 필드 정합성 |
| Timeout | 거래 정책과 일치 |

## 5\. 로그 자동검증

정상·오류 테스트에서 다음 필드가 존재하는지 검사한다.

guid
traceId
businessCode
serviceId
transactionCode
resultCode
elapsedMs

다음 값이 로그에 존재하면 실패시킨다.

Authorization: Bearer 원문
password
refreshToken
privateKey
주민등록번호 원문

# 테스트 시나리오

| ID | 테스트 | 조건 | 기대 결과 |
| --- | --- | --- | --- |
| CH04-001 | 정상 ServiceId | 등록 거래 호출 | Handler 실행 |
| CH04-002 | 미등록 ServiceId | 임의 ID | 표준 오류 |
| CH04-003 | 중복 ServiceId | 두 Handler 등록 | 기동 실패 |
| CH04-004 | Catalog 누락 | Handler만 배포 | Gate 실패 |
| CH04-005 | 거래통제 차단 | 사용 여부 N | Handler 미실행 |
| CH04-006 | Timeout 누락 | 정책 미등록 | 정책에 따른 실패·경고 |
| CH04-007 | 화면 매핑 | 화면 이벤트 호출 | 매트릭스와 일치 |
| CH04-008 | 업무코드 불일치 | URL SV, Header IC | 요청 거부 |
| CH04-009 | 사용자 위변조 | JWT와 Header 불일치 | 인증 오류 |
| CH04-010 | 거래코드 불일치 | Catalog와 다른 코드 | 검증 실패 |
| CH04-011 | 패키지 규칙 | 잘못된 계층 패키지 | 구조검사 실패 |
| CH04-012 | 클래스 Suffix | Handler 이름 위반 | 품질 Gate 실패 |
| CH04-013 | Mapper Namespace | Interface 불일치 | 기동·호출 실패 |
| CH04-014 | GUID 추적 | 정상 거래 | 전 단계 로그 연결 |
| CH04-015 | 민감정보 로그 | Token 로그 출력 | 보안검사 실패 |
| CH04-016 | 화면 1:N 호출 | 최초 진입 | 모든 ServiceId 실행·기록 |
| CH04-017 | 역방향 영향도 | Table 변경 | 영향 화면·테스트 식별 |
| CH04-018 | 폐기 ServiceId | 기존 화면 호출 | 호환·차단정책 검증 |
| CH04-019 | Idempotency | 동일 등록 반복 | 중복 반영 방지 |
| CH04-020 | 장애 추적 | SQL 오류 | ServiceId·GUID·SQL ID 연결 |

# 따라 하는 실무 절차

## 1단계. 대표 화면을 고른다

화면 ID
화면명
업무코드
도메인
이벤트 목록
담당 조직

## 2단계. 이벤트별 ServiceId를 작성한다

| 이벤트 | ServiceId | 거래코드 |
| --- | --- | --- |
| 최초 조회 | SV.Customer.selectSummary | SV-INQ-0001 |
| 검색 | SV.Customer.selectList | SV-INQ-0002 |
| 메모 저장 | SV.Customer.updateMemo | SV-UPD-0001 |

## 3단계. 소스를 추적한다

ServiceId 문자열 검색
↓
Handler 등록 확인
↓
Facade Method 확인
↓
Service 확인
↓
DAO·Mapper 확인
↓
SQL·Table 확인

## 4단계. 운영 등록을 확인한다

Service Catalog
거래통제
Timeout
권한
감사정책
오류코드
Gateway Route

## 5단계. 거래를 실행한다

완료 증적:

요청 JSON
응답 JSON
GUID
TraceId
거래로그
애플리케이션 로그
SQL 로그
DB 결과

## 6단계. 역방향으로 확인한다

변경 Table 선택
↓
Mapper 검색
↓
ServiceId 식별
↓
화면·이벤트 식별
↓
회귀 테스트 범위 확정

# 완료 체크리스트

## 업무코드

| 확인 항목 | 완료 |
| --- | --- |
| 승인된 업무코드 대장을 확인했다. | □ |
| 업무코드와 WAR·Context를 연결했다. | □ |
| 데이터 소유 업무를 확인했다. | □ |
| 담당 조직을 등록했다. | □ |
| 다른 업무 데이터 직접 접근 여부를 확인했다. | □ |

## 화면·거래

| 확인 항목 | 완료 |
| --- | --- |
| 화면 ID가 표준 형식이다. | □ |
| 이벤트 ID가 정의되어 있다. | □ |
| 기능 ID와 권한 관계를 확인했다. | □ |
| 이벤트별 ServiceId를 정의했다. | □ |
| 거래코드가 정의되어 있다. | □ |
| 화면 1:N ServiceId 관계를 기록했다. | □ |

## 프로그램

| 확인 항목 | 완료 |
| --- | --- |
| ServiceId가 Handler에 등록되어 있다. | □ |
| 중복 ServiceId가 없다. | □ |
| Facade·Service 메서드 의미가 일치한다. | □ |
| 패키지·클래스 명명규칙을 준수한다. | □ |
| Mapper Namespace·SQL ID가 일치한다. | □ |
| DB 객체 소유권을 확인했다. | □ |

## 운영

| 확인 항목 | 완료 |
| --- | --- |
| Service Catalog에 등록했다. | □ |
| 거래통제 정책을 등록했다. | □ |
| Timeout 정책을 등록했다. | □ |
| 권한·감사 정책을 연결했다. | □ |
| GUID·TraceId가 로그에 남는다. | □ |
| 거래코드·ServiceId가 Metric에 포함된다. | □ |
| 민감정보가 로그에 남지 않는다. | □ |

## 추적성·품질

| 확인 항목 | 완료 |
| --- | --- |
| 요구사항에서 화면까지 추적 가능하다. | □ |
| 화면에서 SQL·Table까지 추적 가능하다. | □ |
| Table에서 영향 화면을 역추적할 수 있다. | □ |
| 자동 형식검사가 존재한다. | □ |
| Handler–Catalog 자동대조가 존재한다. | □ |
| 정상·경계·오류 테스트를 완료했다. | □ |
| 표준 예외 사항에 승인·만료일이 있다. | □ |

# 변경·호환성·폐기 관리

## ServiceId 변경 원칙

운영 중인 ServiceId는 단순 문자열 수정으로 변경하지 않는다.

기존 ServiceId
↓ Deprecated 표시
신규 ServiceId 추가
↓
화면·연계 순차 전환
↓
호출량 0 확인
↓
Catalog·Handler·정책 폐기

### 폐기 전 확인

| 항목 | 확인 |
| --- | --- |
| 화면 호출 | 사용 중인 화면 없음 |
| 시스템 연계 | 외부 소비자 없음 |
| Batch | 스케줄 호출 없음 |
| 권한 | 기능권한 제거 |
| Gateway | Route 제거 |
| OM | Catalog·통제·Timeout 폐기 |
| 로그 | 최근 호출량 0 |
| 문서 | 추적성 매트릭스 수정 |

# 제4장의 핵심 정리

첫째,
업무코드는 단순 분류가 아니라
업무·데이터·배포·운영 책임의 경계다.

둘째,
화면 ID는 UI를,
ServiceId는 실행 유스케이스를,
거래코드는 운영 거래 유형을 식별한다.

셋째,
한 화면은 여러 ServiceId를 호출할 수 있고,
하나의 ServiceId는 여러 화면에서 재사용될 수 있다.

넷째,
ServiceId는 Dispatcher 라우팅뿐 아니라
OM Catalog·거래통제·Timeout·로그의 연결키다.

다섯째,
패키지는 업무코드 → 도메인 → 계층 순서로 구성하고,
클래스와 메서드 이름은 책임과 행위를 드러내야 한다.

여섯째,
GUID는 전역 거래를,
TraceId는 호출 흐름을,
Idempotency Key는 중복 요청을 식별한다.

일곱째,
신규 거래는 Handler만 구현해서 끝나는 것이 아니라
Catalog·거래통제·Timeout 정책을 함께 등록해야 한다.

여덟째,
좋은 식별 체계는 화면에서 Table까지,
Table에서 영향 화면까지 양방향으로 추적할 수 있어야 한다.

# 시사점

## 핵심 아키텍처 판단

NSIGHT TCF의 식별자는 각자 독립된 이름이 아니다.

업무코드
↔ 화면 ID
↔ ServiceId
↔ 거래코드
↔ 프로그램
↔ SQL·DB 객체
↔ 운영 기준정보
↔ 로그·감사·Metric

이 연결이 유지되어야 개발표준이 실제 운영 통제체계로 작동한다.

## 주요 위험

| 위험 | 영향 |
| --- | --- |
| 업무코드 경계 불명확 | 데이터·조직 책임 충돌 |
| ServiceId 임의 생성 | 중복·미등록 거래 |
| 화면과 거래 매핑 누락 | 테스트·영향도 분석 실패 |
| 거래코드 혼용 | 감사·통계 왜곡 |
| Handler만 구현 | 운영 통제·Timeout 누락 |
| 명명규칙 혼재 | 검색·자동검증 어려움 |
| GUID·TraceId 누락 | 장애 추적 단절 |
| 사용자 정보 로그 노출 | 개인정보 사고 |
| 타 업무 DB 직접 접근 | 강결합·변경 전파 |
| 운영 후 ServiceId 직접 변경 | 연계·화면 장애 |

## 우선 보완 과제

1.  업무코드와 데이터 소유권 대장을 확정한다.
2.  화면 ID–이벤트 ID–ServiceId 매트릭스를 작성한다.
3.  ServiceId와 Handler 등록정보를 자동 추출한다.
4.  Handler와 OM Catalog를 CI/CD에서 자동 대조한다.
5.  거래통제·Timeout 미등록을 배포 차단 조건으로 둔다.
6.  패키지·클래스·메서드 명명검사를 자동화한다.
7.  GUID·TraceId·ServiceId를 구조화 로그의 필수 필드로 지정한다.
8.  Mapper·SQL·Table 역추적 자료를 자동 생성한다.
9.  업무 WAR 간 직접 DB 접근을 점검한다.
10.  ServiceId 폐기·호환성 관리 절차를 운영한다.

## 중장기 발전 방향

수작업 명명검토
↓
정규식·ArchUnit 자동검증
↓
ServiceId·Handler 자동 수집
↓
OM Catalog 자동 대조
↓
화면–프로그램–SQL 추적 자동 생성
↓
운영 로그 기반 실사용 분석
↓
미사용 거래 자동 폐기 후보 선정

# 마무리말

프로그램 이름을 잘 짓는 것은 중요하다.

그러나 NSIGHT TCF에서 더 중요한 것은 각 이름이 시스템 전체에서 같은 의미로 사용되는 것이다.

화면에서는 고객요약 조회인데
ServiceId는 임의 문자열이고,

Handler에서는 다른 이름을 사용하며,
OM에는 등록되지 않고,

로그에는 ServiceId가 남지 않는다면
그 거래는 운영 가능한 거래가 아니다.

반대로 다음 관계가 일치하면 거래는 개발·시험·운영 전 과정에서 추적할 수 있다.

SV-CUS-0001-E01
↓
SV.Customer.selectSummary
↓
SV-INQ-0001
↓
SvCustomerHandler
↓
SvCustomerFacade.selectSummary()
↓
SvCustomerMapper.selectSummary()
↓
SV\_CUSTOMER\_SUMMARY
↓
OM Catalog·Timeout·거래통제
↓
GUID·TraceId 기반 거래로그

제1부에서 배운 내용을 한 문장으로 정리하면 다음과 같다.

TCF는 거래의 실행 순서를 통제하고,
ServiceId는 실행할 업무를 식별하며,
개발표준은 화면·프로그램·데이터·운영을
하나의 추적 가능한 구조로 연결한다.

다음 장부터는 제2부로 넘어가 실제 개발환경을 준비한다. JDK, IDE, Gradle JVM, 인코딩과 줄바꿈 기준을 맞추고, 모든 개발자가 동일한 결과를 재현할 수 있는 로컬 환경을 구성한다.
