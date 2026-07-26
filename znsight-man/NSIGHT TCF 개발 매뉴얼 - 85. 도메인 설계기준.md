NSIGHT TCF 개발 방법론

85. 도메인 설계기준

1. 도입 전 안내말

NSIGHT TCF에서 도메인은 단순한 Java 패키지명이나 ServiceId의 가운데 단어가 아니다.

도메인은 다음 요소가 동일한 업무 책임 안에서 함께 변경되는 논리적 경계다.

```
업무 용어
+ 업무 규칙
+ 사용 사례
+ ServiceId
+ Handler·Facade·Service·Rule
+ 데이터 소유권
+ 권한·감사 기준
+ 변경 책임 조직
```

예를 들어 SV.Customer.selectSummary의 Customer는 보기 좋은 분류명이 아니라 다음 항목을 하나로 묶는 업무 경계여야 한다.

```
고객 조회 유스케이스
→ 고객 조회 규칙
→ 고객 조회 프로그램
→ 고객 조회 데이터
→ 고객정보 접근권한
→ 개인정보 마스킹
→ 고객 조회 감사로그
→ 고객 도메인 담당 조직
```

NSIGHT TCF의 전체 온라인 처리 흐름은 다음과 같다.

```
화면 이벤트
  ↓
표준 요청 전문
  ↓
OnlineTransactionController
  ↓
TCF.process()
  ↓
STF.preProcess()
  ↓
ServiceId 기반 TransactionDispatcher
  ↓
Handler
  ↓
Facade
  ↓
Service
  ├─ Rule
  ├─ DAO → Mapper → DB
  └─ 외부 도메인 Client
  ↓
ETF
  ↓
표준 응답
```

TCF는 serviceId를 기준으로 Handler를 선택하고, 업무 구현은 Handler 이하 6계층에서 수행한다. 따라서 도메인 설계는 ServiceId, 패키지, 프로그램, 데이터 및 OM 운영기준을 일관되게 연결해야 한다.

본 기준은 모든 업무를 이론적인 DDD 방식으로 복잡하게 구현하려는 것이 아니다. NSIGHT TCF 구조에 맞게 다음 수준을 적용하는 실용적 도메인 중심 설계를 목적으로 한다.

```
단순 조회·코드성 업무
→ 계층형 서비스 중심 설계

복잡한 상태변경·규칙 중심 업무
→ Entity·Value Object·Aggregate를 적용한 도메인 모델 설계

여러 도메인을 조합하는 유스케이스
→ Facade 또는 별도 Orchestrator를 통한 조정
```

2. 문서 개요

2.1 목적

본 기준서의 목적은 NSIGHT TCF 업무 애플리케이션의 도메인을 일관된 방법으로 식별·분해·구현·운영하기 위한 기준을 정의하는 것이다.

| 목적 | 설명 |
| --- | --- |
| 업무 경계 명확화 | 기능·규칙·데이터·책임이 어디에 속하는지 명확히 정의 |
| 변경 영향 최소화 | 하나의 업무 변경이 다른 영역으로 무분별하게 확산되는 것을 방지 |
| 프로그램 구조 표준화 | 도메인과 Handler·Facade·Service·Rule·DAO·Mapper 연결 |
| ServiceId 정합성 | ServiceId의 도메인 구간을 실제 업무 경계와 일치 |
| 데이터 소유권 명확화 | 테이블·조회모델·변경 권한의 소유 도메인 정의 |
| 연계 통제 | 다른 도메인의 Service·DAO·테이블 직접 접근 방지 |
| 운영 추적성 | ServiceId·오류코드·로그·감사·Timeout을 도메인 기준으로 분류 |
| 조직 책임 명확화 | 도메인별 설계·개발·운영 담당 조직 정의 |
| 확장성 확보 | 신규 업무와 신규 WAR를 기존 구조에 일관되게 추가 |
| 자동검증 | 패키지·의존성·ServiceId·데이터 소유권 위반을 CI/CD에서 검출 |

2.2 적용범위

본 기준은 다음 영역에 적용한다.

| 적용 영역 | 적용 내용 |
| --- | --- |
| 업무 WAR | SV, IC, PC, BC, MS, PD, CM, EB, EP, BP, BD, SS, CS, CT, MG, OM 등 |
| Java 패키지 | 업무코드 하위 도메인 및 계층 구조 |
| ServiceId | {업무코드}.{도메인}.{행위} 구조 |
| 거래설계 | 도메인별 조회·등록·변경·삭제·승인·실행 거래 |
| 프로그램 | Handler, Facade, Service, Rule, DAO, Mapper, Client |
| 데이터 | 테이블, View, 조회모델, Cache, 파일, 메시지 |
| 운영관리 | OM Service Catalog, 거래통제, Timeout, 오류코드 |
| 보안 | 기능권한, 데이터권한, 개인정보, 감사대상 |
| 연계 | 업무 WAR 간 호출, 외부 시스템, 이벤트·메시지 |
| 테스트 | 단위·통합·계약·구조·성능 테스트 |
| CI/CD | 패키지·의존성·등록정보·변경 영향 검증 |

다음은 본 문서에서 직접 정의하지 않는다.

| 제외 영역 | 별도 기준 |
| --- | --- |
| DB 컬럼의 데이터 도메인 | 데이터 모델링·DB 표준화 기준 |
| 화면 메뉴 분류 | 화면·메뉴·기능권한 설계기준 |
| 물리 서버 배치 | 배포·인프라 아키텍처 기준 |
| JWT 발급 및 키 관리 | 인증·토큰 설계기준 |
| 상세 SQL 작성 방식 | DAO·Mapper·SQL 설계기준 |

2.3 대상 독자

| 대상 | 활용 목적 |
| --- | --- |
| 애플리케이션 아키텍트 | 도메인 경계와 의존성 검토 |
| 업무 분석가 | 업무 기능과 용어 체계 정의 |
| 업무 개발자 | 패키지·클래스·ServiceId 구현 |
| 프레임워크팀 | 도메인 구조와 TCF 연계 표준 제공 |
| 데이터 아키텍트·DBA | 데이터 소유권과 변경 경계 검토 |
| 보안 담당자 | 도메인별 권한·개인정보·감사 기준 검토 |
| 운영 담당자 | 도메인별 거래·장애·변경 영향 추적 |
| 테스트 담당자 | 도메인 단위 테스트와 계약 검증 |
| DevOps·품질팀 | 자동검증과 품질 Gate 운영 |
| PMO | 도메인별 책임·진척·위험 관리 |

2.4 선행조건

도메인 설계를 수행하기 전에 다음 조건이 충족되어야 한다.

| 선행조건 | 기준 |
| --- | --- |
| 업무 범위 | 대상 업무와 제외 업무가 정의되어 있어야 함 |
| 업무코드 | WAR 또는 최상위 업무 영역 코드가 확정되어 있어야 함 |
| 업무 기능 | 상위·중위·하위 업무 기능이 식별되어 있어야 함 |
| 핵심 용어 | 고객, 캠페인, 상품, 접촉 등 주요 업무 용어가 정의되어 있어야 함 |
| 화면 기능 | 주요 화면과 이벤트가 식별되어 있어야 함 |
| 거래 목록 | 조회·등록·변경·승인·실행 거래 후보가 존재해야 함 |
| 데이터 목록 | 주요 데이터와 기준정보가 식별되어 있어야 함 |
| 조직 책임 | 업무 담당 조직과 시스템 담당 조직이 식별되어 있어야 함 |
| TCF 표준 | ServiceId, 거래코드, 패키지 및 계층 기준이 정의되어 있어야 함 |
| 연계 목록 | 내부·외부 시스템 간 연계 후보가 식별되어 있어야 함 |

TCF 적용 전에는 표준 전문, 업무코드, ServiceId, Handler, OM Catalog, 거래통제, Timeout 정책 등이 함께 준비되어야 한다.

2.5 용어 정의

| 용어 | 정의 |
| --- | --- |
| 비즈니스 도메인 | 조직이 수행하는 업무 지식과 문제 영역 |
| 상위 업무영역 | 고객, 마케팅, 실시간 처리, 운영관리 등 최상위 업무 분류 |
| 업무코드 | WAR 또는 상위 업무영역을 식별하는 코드 |
| 서브도메인 | 상위 업무영역을 업무 책임에 따라 세분화한 영역 |
| 경계 컨텍스트 | 동일한 용어·규칙·모델이 일관되게 적용되는 명시적 경계 |
| 도메인 모델 | 업무 개념과 상태, 행위, 규칙을 코드로 표현한 모델 |
| Entity | 식별자와 생명주기를 가지는 업무 객체 |
| Value Object | 식별자 없이 값 자체로 의미가 결정되는 불변 객체 |
| Aggregate | 하나의 트랜잭션에서 일관성을 보장해야 하는 객체 집합 |
| Aggregate Root | Aggregate 외부에서 접근 가능한 대표 Entity |
| Domain Service | 특정 Entity에 자연스럽게 속하지 않는 핵심 업무 규칙 |
| Application Service | 유스케이스 실행 순서를 조정하는 서비스 |
| Repository | Aggregate 저장·조회 책임을 추상화한 인터페이스 |
| Read Model | 조회 목적에 최적화된 데이터 모델 |
| Ubiquitous Language | 업무·설계·코드에서 동일하게 사용하는 공통 업무 용어 |
| Shared Kernel | 여러 도메인이 제한적으로 공동 소유하는 최소 공통 모델 |
| Anti-Corruption Layer | 외부 또는 다른 도메인의 모델이 내부로 직접 침투하지 않도록 변환하는 계층 |
| 도메인 소유권 | 도메인의 규칙·프로그램·데이터·변경 승인에 대한 책임 |

3. 본문

3.1 문제 정의 및 설계 배경

3.1.1 기능 중심 분류의 문제

업무 프로그램을 조회·등록·변경 또는 화면·테이블 단위로만 분류하면 다음 문제가 발생한다.

```
customer
 ├─ select
 ├─ insert
 └─ update
```

또는 다음과 같이 기술 계층만 기준으로 구성될 수 있다.

```
handler
facade
service
rule
dao
mapper
```

계층만 존재하고 업무 도메인 경계가 없으면 다음 문제가 발생한다.

| 문제 | 영향 |
| --- | --- |
| 업무 책임 불명확 | 특정 기능의 담당 도메인을 알기 어려움 |
| 대형 Service 증가 | 여러 업무 규칙이 하나의 Service에 집중 |
| 데이터 무단 접근 | 다른 업무의 Mapper와 테이블을 직접 사용 |
| 변경 영향 확대 | 작은 변경이 다수 패키지와 업무로 확산 |
| 용어 불일치 | 화면·설계·코드·DB에서 다른 용어 사용 |
| 운영 분류 어려움 | ServiceId와 장애를 업무별로 집계하기 어려움 |
| 조직 책임 충돌 | 여러 팀이 동일 프로그램과 데이터를 변경 |
| 재사용 오해 | 공통화가 필요하지 않은 업무를 common으로 이동 |

3.1.2 화면·메뉴 중심 도메인 분류의 문제

화면이나 메뉴는 사용자 경험과 업무 절차에 따라 자주 바뀐다. 따라서 화면을 도메인 경계로 사용하면 안 된다.

```
금지 예:
customerMainScreen
customerPopup
customerTab1
customerTab2
```

하나의 화면은 여러 도메인의 데이터를 조합할 수 있고, 하나의 도메인은 여러 화면과 채널에서 재사용될 수 있다.

```
고객 종합정보 화면
  ├─ Customer 도메인
  ├─ ProductHolding 도메인
  ├─ Contact 도메인
  └─ Campaign 도메인
```

따라서 다음을 구분한다.

```
화면
= 사용자에게 기능을 제공하는 표현 단위

ServiceId
= 하나의 실행 거래 단위

도메인
= 업무 규칙과 책임의 경계
```

3.1.3 DB 테이블 중심 분류의 문제

테이블별로 도메인을 만들면 기술적인 저장 구조가 업무 구조를 지배하게 된다.

```
금지 예:
CustomerMasterDomain
CustomerDetailDomain
CustomerHistoryDomain
```

테이블은 도메인의 영속화 수단이지 도메인 자체가 아니다.

하나의 도메인이 여러 테이블을 소유할 수 있으며, 하나의 조회모델이 여러 소유 도메인의 데이터를 조합할 수 있다.

3.2 현행 구조와 문제점

NSIGHT TCF의 기본 계층은 다음과 같이 명확하게 정의되어 있다.

```
Handler
  ↓
Facade
  ↓
Service
  ├─ Rule
  ├─ DAO
  │   ↓
  │ Mapper
  └─ Client
```

그러나 계층 기준만 적용하고 도메인 기준이 없으면 다음과 같은 구조가 만들어질 수 있다.

```
com.nh.nsight.sv
 ├─ handler
 │   ├─ CustomerHandler
 │   ├─ ProductHandler
 │   └─ ContactHandler
 ├─ service
 │   ├─ CustomerService
 │   ├─ ProductService
 │   └─ ContactService
 ├─ dao
 └─ mapper
```

이 구조는 초기에는 단순하지만 업무가 커지면 한 도메인의 프로그램이 여러 계층 디렉터리에 흩어진다.

```
Customer 변경 시 확인 대상
- handler/CustomerHandler
- facade/CustomerFacade
- service/CustomerService
- rule/CustomerRule
- dao/CustomerDao
- mapper/CustomerMapper
- dto/CustomerDto
```

따라서 NSIGHT 업무 WAR 내부는 도메인 우선, 계층 후순위 구조를 기본으로 한다.

```
com.nh.nsight.sv.customer
 ├─ handler
 ├─ facade
 ├─ service
 ├─ rule
 ├─ dao
 ├─ mapper
 └─ dto
```

3.3 요구사항과 제약조건

3.3.1 기능 요구사항

| ID | 요구사항 |
| --- | --- |
| DOM-001 | 모든 업무 거래는 하나의 주 도메인에 소속되어야 한다. |
| DOM-002 | 모든 ServiceId는 등록된 도메인명을 포함해야 한다. |
| DOM-003 | 도메인별 업무 용어와 책임이 정의되어야 한다. |
| DOM-004 | 도메인별 Handler·Facade·Service·Rule·DAO·Mapper가 식별되어야 한다. |
| DOM-005 | 도메인별 소유 데이터와 참조 데이터가 구분되어야 한다. |
| DOM-006 | 다른 도메인의 데이터 변경은 소유 도메인을 통해 수행해야 한다. |
| DOM-007 | 도메인 간 호출 방식과 오류·Timeout 정책이 정의되어야 한다. |
| DOM-008 | 도메인별 권한·개인정보·감사 기준이 정의되어야 한다. |
| DOM-009 | 도메인별 담당 업무조직과 시스템조직이 지정되어야 한다. |
| DOM-010 | 도메인 변경 시 영향 ServiceId·화면·SQL을 추적할 수 있어야 한다. |

3.3.2 비기능 요구사항

| 항목 | 기준 |
| --- | --- |
| 응집도 | 동일한 변경 이유를 가진 프로그램과 규칙을 같은 도메인에 배치 |
| 결합도 | 다른 도메인의 내부 구현에 직접 의존하지 않음 |
| 추적성 | 화면 → ServiceId → 도메인 → 프로그램 → SQL → DB 연결 |
| 테스트 가능성 | 도메인 규칙을 독립적으로 테스트 가능 |
| 확장성 | 신규 도메인을 기존 도메인 수정 없이 추가 가능 |
| 운영성 | 도메인별 거래량·오류·응답시간을 집계 가능 |
| 보안성 | 도메인별 접근권한과 데이터 분류 적용 |
| 변경성 | 도메인 내부 변경이 외부 계약에 미치는 영향 최소화 |
| 가용성 | 중요 도메인의 장애 범위와 우회·차단 기준 정의 |
| 성능 | 도메인 간 과도한 동기 호출과 N+1 호출 방지 |

3.3.3 제약조건

- 조직명만으로 도메인을 정의하지 않는다.
- 화면명이나 메뉴명을 도메인명으로 사용하지 않는다.
- 테이블 하나당 도메인 하나를 만들지 않는다.
- Common, Etc, Misc, Manager, Process 같은 포괄 명칭을 업무 도메인으로 사용하지 않는다.
- 하나의 ServiceId를 여러 도메인에 중복 소속시키지 않는다.
- 다른 도메인의 DAO·Mapper를 직접 호출하지 않는다.
- 다른 도메인의 테이블을 임의로 등록·변경·삭제하지 않는다.
- 도메인 간 순환 의존성을 허용하지 않는다.
- 단순 코드 재사용을 이유로 업무 모델을 공통 모듈로 이동하지 않는다.
- 도메인 모델을 화면 DTO나 DB Result DTO와 동일하게 사용하지 않는다.
- 도메인 간 내부 Java 클래스 직접 공유를 기본 연계 방식으로 사용하지 않는다.
- 도메인 경계 변경을 단순 리팩터링으로 처리하지 않고 영향도 분석과 승인을 수행한다.
3.4 설계 원칙

3.4.1 업무 책임 중심 원칙

도메인은 다음 질문에 하나의 일관된 답을 줄 수 있어야 한다.

```
이 업무의 핵심 목적은 무엇인가?
어떤 업무 용어를 사용하는가?
어떤 규칙을 책임지는가?
어떤 상태를 생성·변경하는가?
어떤 데이터를 소유하는가?
누가 변경을 승인하는가?
```

예:

| 도메인 | 핵심 책임 |
| --- | --- |
| Customer | 고객 식별·기본정보·상태 조회 |
| Campaign | 캠페인 정의·승인·실행 상태 관리 |
| Product | 상품 기준정보와 판매 가능 상태 관리 |
| Contact | 고객 접촉·상담·접촉결과 관리 |
| Authorization | 사용자·역할·기능권한 관리 |
| ServiceCatalog | ServiceId와 운영 속성 관리 |

3.4.2 단일 변경 이유 원칙

도메인 내부 구성요소는 같은 업무 이유로 함께 변경되어야 한다.

예를 들어 고객 개인정보 마스킹 정책 변경으로 다음 항목이 함께 변경된다면 동일한 고객 도메인에 배치하는 것이 자연스럽다.

```
CustomerRule
CustomerService
CustomerResponseAssembler
CustomerMaskingPolicy
CustomerAuditPolicy
```

반면 캠페인 승인정책은 고객 도메인 변경 이유와 다르므로 Campaign 도메인으로 분리한다.

3.4.3 공통 언어 원칙

업무 문서·화면·ServiceId·클래스·테이블 설명에서 동일한 용어를 사용한다.

| 구분 | 표준 용어 사용 예 |
| --- | --- |
| 업무 용어 | 고객 |
| 영문 도메인명 | Customer |
| 패키지명 | customer |
| ServiceId | SV.Customer.selectSummary |
| Handler | SvCustomerHandler |
| Facade | SvCustomerFacade |
| 오류코드 영역 | SV-CUST |
| 로그 도메인 | CUSTOMER |

다음과 같이 같은 대상을 여러 용어로 표현하지 않는다.

```
Customer
Client
User
Member
Cstmr
```

용어가 실제로 다른 개념이면 정의서에서 차이를 명확히 기술해야 한다.

3.4.4 명시적 경계 원칙

도메인 외부에서는 공개된 인터페이스만 사용한다.

```
도메인 내부
- Entity
- Value Object
- 내부 Rule
- DAO
- Mapper
- 내부 DTO

도메인 외부 공개
- ServiceId
- Facade 또는 Application Interface
- 연계 Request·Response
- 이벤트 계약
```

다른 도메인은 내부 DAO·Mapper·Entity에 의존하지 않는다.

3.4.5 데이터 소유권 단일화 원칙

하나의 데이터 변경 책임은 하나의 소유 도메인에만 둔다.

| 구분 | 기준 |
| --- | --- |
| 소유 도메인 | 데이터 생성·변경·삭제 규칙 책임 |
| 참조 도메인 | 조회 또는 계약을 통한 제한적 사용 |
| 통합 조회 | Read Model 또는 조회 전용 View 사용 |
| 변경 요청 | 소유 도메인의 ServiceId·API·이벤트 사용 |
| 직접 DML | 소유 도메인 외에는 금지 |

```
Customer 도메인 소유 데이터
→ 다른 도메인이 직접 UPDATE 금지

Campaign 도메인이 고객 상태 변경 필요
→ Customer 도메인 거래 호출
```

3.4.6 도메인 우선 패키지 원칙

업무 WAR 내부 패키지는 다음 형식을 사용한다.

```
{기본패키지}.{업무코드소문자}.{도메인소문자}.{계층}
```

예:

```
com.nh.nsight.sv.customer.handler
com.nh.nsight.sv.customer.facade
com.nh.nsight.sv.customer.service
com.nh.nsight.sv.customer.rule
com.nh.nsight.sv.customer.dao
com.nh.nsight.sv.customer.mapper
com.nh.nsight.sv.customer.dto
```

도메인 중심 패키지 구조를 사용하면 하나의 업무 변경에 필요한 프로그램을 가까운 위치에서 확인할 수 있다.

3.4.7 ServiceId 정합성 원칙

ServiceId 형식은 다음을 따른다.

```
{업무코드}.{도메인}.{행위}
```

예:

```
SV.Customer.selectSummary
SV.Customer.selectProfile
CM.Campaign.create
CM.Campaign.approve
OM.ServiceCatalog.register
OM.TimeoutPolicy.update
```

| 구성 | 기준 |
| --- | --- |
| 업무코드 | 등록된 대문자 업무코드 |
| 도메인 | 단수형 PascalCase 명사 |
| 행위 | lowerCamelCase 동사구 |
| 환경명 | 포함 금지 |
| 화면번호 | 포함 금지 |
| 버전번호 | 원칙적으로 포함 금지 |
| 기술계층명 | 포함 금지 |

ServiceId는 Dispatcher의 라우팅 키이므로 도메인명 변경은 단순 표시명 변경이 아니라 거래 계약 변경으로 관리한다. TCF의 Dispatcher는 ServiceId와 Handler를 연결하며, 중복 ServiceId는 기동 단계에서 차단하는 것이 기준이다.

3.4.8 복잡도에 따른 모델 선택 원칙

모든 도메인에 Entity와 Aggregate를 강제하지 않는다.

| 업무 유형 | 권장 모델 |
| --- | --- |
| 단순 목록·기준정보 조회 | Service + DAO + Query/Result DTO |
| 여러 데이터를 조합한 화면 조회 | Facade + 조회 Service + Read Model |
| 단순 등록·변경 | Service + Rule + Command DTO |
| 상태전이와 규칙이 복잡한 업무 | Entity + Value Object + Aggregate |
| 승인·실행·취소 생명주기 | Aggregate + 상태전이 규칙 |
| 여러 도메인 조합 | Facade 또는 Orchestrator |
| 장시간·비동기 업무 | 이벤트·메시지 기반 프로세스 |

3.4.9 도메인 간 순환 의존 금지 원칙

다음 구조는 금지한다.

```
Customer → Campaign
Campaign → Customer
```

필요한 경우 다음 대안을 적용한다.

```
대안 1. 상위 Orchestrator가 양쪽 호출
대안 2. 읽기 전용 계약으로 단방향 호출
대안 3. 이벤트 발행·구독
대안 4. 최소 Shared Kernel 분리
```

3.4.10 플랫폼과 업무 도메인 분리 원칙

TCF 공통 기능은 업무 도메인으로 취급하지 않는다.

| 플랫폼 책임 | 업무 도메인 책임 |
| --- | --- |
| Header 검증 | 고객 조회조건 판단 |
| 인증 문맥 확인 | 고객 데이터권한 세부 판단 |
| 거래통제 | 캠페인 승인 가능 여부 |
| Timeout 적용 | 상품 추천 규칙 |
| 거래로그 | 업무 감사 상세정보 제공 |
| Handler 라우팅 | 업무 유스케이스 실행 |
| 표준 응답 조립 | 업무 결과 데이터 생성 |

TCF는 거래 실행 방법을 통제하고, 업무 도메인은 업무 판단을 수행한다.

3.5 대안 비교 및 의사결정

3.5.1 패키지 구조 대안

| 대안 | 구조 | 장점 | 문제점 | 판단 |
| --- | --- | --- | --- | --- |
| 계층 우선 | sv.service.customer | 계층 구조가 단순 | 도메인 프로그램이 분산 | 제한적 |
| 도메인 우선 | sv.customer.service | 변경·책임·소스 응집 | 도메인 정의가 선행돼야 함 | 권장 |
| 기능 거래별 | sv.customer.selectsummary | 거래별 독립성 | 클래스·패키지 과다 | 비권장 |
| 테이블별 | sv.customermaster | DB 추적 용이 | 업무와 저장구조 결합 | 금지 |
| 화면별 | sv.cus0001 | 화면 추적 용이 | 화면 변경 시 구조 붕괴 | 금지 |

최종 결정

```
업무 WAR
→ 도메인 우선
→ 도메인 내부 계층 분리
```

3.5.2 도메인 간 연계 대안

| 방식 | 적용 상황 | 장점 | 유의사항 |
| --- | --- | --- | --- |
| 동일 도메인 내부 호출 | 같은 규칙·데이터 책임 | 단순·빠름 | 내부 API 외부 노출 금지 |
| 공개 Facade 호출 | 동일 WAR 내 다른 도메인 | Java 계약 가능 | 순환 의존 금지 |
| TCF/EAI ServiceId 호출 | 다른 WAR·독립 경계 | 추적·통제 용이 | Timeout·실패정책 필요 |
| 이벤트 발행·구독 | 비동기·느슨한 결합 | 장애 격리 | 최종 일관성 고려 |
| DB 직접 접근 | 단순 조회로 보일 때 | 초기 구현이 쉬움 | 소유권 침해·강결합 |
| 공통 테이블 공유 | 여러 도메인 사용 | 구현 단순 | 변경 책임 불명확 |

DB 직접 접근과 공통 테이블 공유는 원칙적으로 선택하지 않는다.

3.5.3 조회모델 대안

고객 종합화면처럼 여러 도메인 데이터를 한 번에 조회해야 하는 경우 다음 순서로 판단한다.

| 우선순위 | 방식 | 적용 기준 |
| --- | --- | --- |
| 1 | 조회 전용 Read Model | 조회가 빈번하고 응답시간 중요 |
| 2 | Facade가 여러 도메인 조회 조합 | 호출 수가 적고 실시간 정합성 중요 |
| 3 | DB View | 데이터팀 승인, 소유권·성능 명확 |
| 4 | 다른 도메인 테이블 직접 Join | 원칙적으로 금지 |

3.6 목표 아키텍처

3.6.1 전체 도메인 구조

```
┌─────────────────────────────────────────────────────┐
│ 채널·화면                                           │
│ WEBTOPSUITE / BI Portal / UJ / 외부 채널            │
└───────────────────────┬─────────────────────────────┘
                        │ 표준 전문
                        ▼
┌─────────────────────────────────────────────────────┐
│ TCF 플랫폼 경계                                     │
│ Controller → TCF → STF → Dispatcher → ETF           │
└───────────────────────┬─────────────────────────────┘
                        │ ServiceId
                        ▼
┌─────────────────────────────────────────────────────┐
│ 업무 WAR                                            │
│                                                     │
│ Customer Domain                                     │
│ Handler → Facade → Service → Rule → DAO → Mapper    │
│                                                     │
│ ProductHolding Domain                               │
│ Handler → Facade → Service → Rule → DAO → Mapper    │
│                                                     │
│ Contact Domain                                      │
│ Handler → Facade → Service → Rule → DAO → Mapper    │
└───────────────────────┬─────────────────────────────┘
                        │ 공개 계약
                        ▼
┌─────────────────────────────────────────────────────┐
│ 다른 업무 WAR / 외부 시스템                         │
│ TCF-EAI / HTTP / Event / File                       │
└─────────────────────────────────────────────────────┘
```

3.6.2 업무코드·WAR·도메인 관계

업무코드는 상위 배포·운영 경계를 나타내고, 도메인은 해당 업무코드 안의 업무 책임 경계를 나타낸다.

```
업무코드/WAR
  1
  └─ N개 도메인

도메인
  1
  └─ N개 ServiceId

ServiceId
  1
  └─ 1개 논리 Handler 경로
```

| 구분 | 의미 | 예 |
| --- | --- | --- |
| 업무코드 | 최상위 업무·배포 식별 | SV |
| WAR | 물리 배포 단위 | sv-service.war |
| 도메인 | 논리 업무 책임 단위 | Customer |
| ServiceId | 실행 거래 단위 | SV.Customer.selectSummary |
| 프로그램 | 거래 구현 | SvCustomerHandler 등 |
| 데이터 | 도메인 소유·참조 데이터 | 고객요약 등 |

업무코드와 WAR가 항상 완전한 도메인 경계인 것은 아니다. 하나의 WAR 안에 여러 도메인이 포함될 수 있으며, 업무 규모와 장애 격리 요구가 커질 경우 도메인을 별도 WAR로 분리할 수 있다.

3.7 표준 형식

3.7.1 도메인 식별 형식

| 속성 | 형식 | 예 |
| --- | --- | --- |
| 한글 도메인명 | 업무 표준 용어 | 고객 |
| 영문 도메인명 | 단수형 PascalCase | Customer |
| 패키지 도메인명 | 소문자 | customer |
| 도메인 코드 | 영문 대문자 2~6자 | CUST |
| 업무코드 | 대문자 2자 원칙 | SV |
| ServiceId 구간 | 영문 도메인명 | SV.Customer.* |
| 오류코드 영역 | 업무코드+도메인코드 | E-SV-CUST-* |
| 로그 분류 | 대문자 도메인명 | CUSTOMER |

3.7.2 도메인 정의서 필수 속성

| 항목 | 필수 | 설명 |
| --- | --- | --- |
| 도메인 ID | 예 | 관리용 고유 식별자 |
| 업무코드 | 예 | 소속 상위 업무 |
| 한글명 | 예 | 업무 표준 용어 |
| 영문명 | 예 | ServiceId·클래스 기준명 |
| 도메인 코드 | 예 | 오류·통계 분류 코드 |
| 목적 | 예 | 도메인이 해결하는 업무 문제 |
| 핵심 책임 | 예 | 도메인이 담당하는 기능 |
| 제외 책임 | 예 | 다른 도메인에 속하는 기능 |
| 핵심 용어 | 예 | 도메인 내 공통 언어 |
| 주요 Entity | 조건 | 식별자·생명주기 객체 |
| 주요 Value Object | 조건 | 업무 값 객체 |
| 주요 규칙 | 예 | 핵심 업무 규칙 |
| 소유 데이터 | 예 | 생성·변경 책임 데이터 |
| 참조 데이터 | 조건 | 읽기만 허용되는 데이터 |
| 주요 ServiceId | 예 | 도메인의 거래 목록 |
| 입력 연계 | 조건 | 외부에서 수신하는 계약 |
| 출력 연계 | 조건 | 외부로 제공하는 계약 |
| 권한 | 예 | 기능·데이터 접근권한 |
| 개인정보 | 예 | 개인정보 등급과 마스킹 |
| 감사 기준 | 예 | 조회·변경 감사 여부 |
| 성능 등급 | 예 | 일반·중요·고부하 |
| 담당 조직 | 예 | 업무·개발·운영 담당 |
| 상태 | 예 | 초안·승인·운영·폐기 |
| 버전 | 예 | 정의서 버전 |

3.7.3 권장 패키지 형식

```
com.nh.nsight.{업무코드소문자}.{도메인소문자}
 ├─ handler
 ├─ facade
 ├─ service
 ├─ rule
 ├─ dao
 ├─ mapper
 ├─ dto
 │   ├─ request
 │   ├─ response
 │   ├─ command
 │   ├─ query
 │   └─ result
 ├─ model
 │   ├─ entity
 │   ├─ value
 │   └─ aggregate
 ├─ client
 ├─ event
 ├─ assembler
 └─ support
```

모든 하위 패키지를 의무적으로 생성하지 않는다. 실제 책임이 존재하는 패키지만 생성한다.

3.8 구성요소 및 속성

3.8.1 Handler

| 항목 | 기준 |
| --- | --- |
| 책임 | ServiceId와 유스케이스 연결 |
| 입력 | 표준 요청에서 업무 Request DTO 변환 |
| 호출 | 해당 도메인의 Facade |
| 금지 | DAO·Mapper·다른 도메인 내부 Service 직접 호출 |
| 트랜잭션 | 선언 금지 |
| 업무 규칙 | 구현 금지 |

3.8.2 Facade

| 항목 | 기준 |
| --- | --- |
| 책임 | 유스케이스 조립과 트랜잭션 경계 |
| 호출 | 같은 도메인의 Service, 승인된 도메인 계약 |
| 트랜잭션 | 조회·변경 특성에 맞게 선언 |
| 보상처리 | 여러 도메인 조합 시 명시 |
| 금지 | Mapper 직접 호출, 화면 응답 직접 조립 남용 |

3.8.3 Service

| 항목 | 기준 |
| --- | --- |
| 책임 | 도메인 업무 흐름 수행 |
| 호출 | Rule, DAO, Domain Model, 공개 Client |
| 입력 | Command·Query 또는 업무 DTO |
| 출력 | 업무 결과 DTO 또는 Domain Model |
| 금지 | StandardResponse 생성, 타 도메인 Mapper 직접 호출 |

3.8.4 Rule 또는 Domain Policy

| 항목 | 기준 |
| --- | --- |
| 책임 | 업무 조건·검증·계산·상태전이 판단 |
| 특성 | 가능한 한 부작용 없는 순수 로직 |
| 입력 | Entity, Value Object, 업무 값 |
| 출력 | 판단 결과 또는 업무 예외 |
| 금지 | DB·외부 API·세션 직접 호출 |

DB 조회가 필요한 규칙은 Service가 필요한 데이터를 조회한 뒤 Rule에 전달한다.

3.8.5 Entity

Entity는 다음 조건을 만족할 때 적용한다.

- 고유 식별자가 존재한다.
- 생성부터 폐기까지 생명주기가 존재한다.
- 상태가 변경된다.
- 상태변경 규칙을 객체 내부에서 보호할 필요가 있다.

```
public class Campaign {

    private final CampaignId id;
    private CampaignStatus status;
    private CampaignPeriod period;

    public void approve(Approver approver) {
        if (status != CampaignStatus.REQUESTED) {
            throw new BusinessException("승인 요청 상태만 승인할 수 있습니다.");
        }
        status = CampaignStatus.APPROVED;
    }
}
```

단순 DB 조회 결과에는 Entity를 강제하지 않는다.

3.8.6 Value Object

Value Object는 다음 기준을 따른다.

- 값으로 동일성을 판단한다.
- 생성 이후 변경하지 않는다.
- 유효성 검증을 생성 시점에 수행한다.
- 주민번호·계좌번호 등 민감값은 별도 노출 통제를 적용한다.
예:

```
public record CampaignPeriod(
        LocalDateTime startAt,
        LocalDateTime endAt) {

    public CampaignPeriod {
        if (startAt == null || endAt == null || !startAt.isBefore(endAt)) {
            throw new IllegalArgumentException("캠페인 기간이 올바르지 않습니다.");
        }
    }
}
```

3.8.7 Aggregate

Aggregate는 하나의 트랜잭션에서 반드시 일관성을 보장해야 하는 범위로 제한한다.

| 원칙 | 기준 |
| --- | --- |
| 크기 | 가능한 한 작게 유지 |
| 접근 | 외부에서는 Aggregate Root만 접근 |
| 트랜잭션 | 한 Aggregate 변경을 기본 단위로 함 |
| 참조 | 다른 Aggregate는 식별자로 참조 |
| 조회 | 대형 조회는 Read Model 사용 |
| 분산 | 다른 도메인의 Aggregate를 한 트랜잭션에 직접 묶지 않음 |

3.8.8 DAO·Mapper·Repository

기존 TCF 구조에서는 DAO·Mapper를 기본 데이터 접근 구조로 유지한다.

복잡한 Aggregate를 적용할 때만 Repository 추상화를 추가할 수 있다.

```
단순 조회
Service → DAO → Mapper

복잡한 Aggregate
Service → Repository Interface
             ↓
       Repository Implementation
             ↓
          Mapper
```

Repository를 적용하더라도 MyBatis Mapper가 도메인 외부에 직접 노출되어서는 안 된다.

3.9 책임 경계와 RACI

3.9.1 책임 경계

| 역할 | 책임 |
| --- | --- |
| EA·SA | 상위 업무영역과 시스템 경계 승인 |
| AA | 도메인 경계·의존성·패키지·ServiceId 구조 승인 |
| 업무 분석팀 | 업무 용어·규칙·책임·예외 정의 |
| 업무 개발팀 | 도메인 프로그램과 테스트 구현 |
| 프레임워크팀 | TCF 연계와 자동검증 도구 제공 |
| DA·DBA | 데이터 소유권·조회·변경 경계 검토 |
| 보안팀 | 권한·개인정보·감사 기준 검토 |
| OM 운영팀 | 도메인·ServiceId·정책 기준정보 운영 |
| DevOps | CI/CD 품질 Gate 적용 |
| 품질팀 | 산출물·코드·기준정보 정합성 검증 |

3.9.2 RACI

| 활동 | AA | 업무팀 | 개발팀 | DA/DBA | 보안 | OM | DevOps |
| --- | --- | --- | --- | --- | --- | --- | --- |
| 도메인 후보 식별 | A | R | C | C | C | I | I |
| 업무 용어 정의 | C | A/R | C | C | I | I | I |
| 도메인 경계 승인 | A/R | C | C | C | C | I | I |
| 데이터 소유권 결정 | C | C | C | A/R | C | I | I |
| ServiceId 설계 | A | C | R | I | I | C | I |
| 패키지·프로그램 구현 | C | C | A/R | I | I | I | I |
| 권한·감사 정의 | C | R | C | I | A/R | C | I |
| OM 등록 | C | C | R | I | C | A/R | I |
| 자동검증 구현 | A | I | C | C | C | I | R |
| 도메인 변경 승인 | A | R | C | C | C | C | I |
| 폐기·호환성 관리 | A | R | C | C | C | R | C |

R: 수행, A: 최종 책임, C: 협의, I: 공유

3.10 정상 처리 흐름

3.10.1 단일 도메인 조회

```
화면
  ↓
SV.Customer.selectSummary
  ↓
TCF / STF
  ↓
SvCustomerHandler
  ↓
SvCustomerFacade
  ↓
SvCustomerService
  ├─ SvCustomerInquiryRule
  └─ SvCustomerDao
       ↓
     SvCustomerMapper
       ↓
     Customer Read Model
  ↓
ETF
  ↓
표준 응답
```

3.10.2 단일 도메인 상태변경

```
화면
  ↓
CM.Campaign.approve
  ↓
TCF / STF
  ↓
CmCampaignHandler
  ↓
CmCampaignFacade @Transactional
  ↓
CmCampaignService
  ├─ Campaign 조회
  ├─ 승인권한 검증
  ├─ Campaign.approve()
  └─ Campaign 저장
  ↓
ETF
```

3.10.3 여러 도메인 조합 조회

```
고객 종합화면
  ↓
SV.CustomerView.selectOverview
  ↓
CustomerViewFacade
  ├─ Customer 공개 조회계약
  ├─ ProductHolding 공개 조회계약
  └─ Contact 공개 조회계약
  ↓
CustomerOverview Read Model 조립
```

이때 CustomerView 도메인은 다른 도메인의 DAO·Mapper를 직접 호출하지 않는다.

3.10.4 다른 WAR 도메인 호출

```
IC 업무
  ↓
IC.CustomerIntegration.selectCustomer
  ↓
tcf-eai
  ↓
SV.Customer.selectSummary
  ↓
GUID / TraceId 유지
  ↓
표준 응답
```

도메인 간 호출에서도 ServiceId·Timeout·오류·거래로그를 유지한다.

3.11 오류·Timeout·장애 흐름

3.11.1 업무 규칙 오류

```
Service
  ↓
Rule 또는 Entity 상태검증 실패
  ↓
BusinessException
  ↓
ETF.businessFail
  ↓
업무 오류코드 반환
```

오류코드는 업무코드와 도메인 코드를 포함한다.

```
E-CM-CAMP-0001
E-SV-CUST-0002
E-OM-AUTH-0003
```

3.11.2 다른 도메인 호출 실패

| 호출 성격 | 처리 기준 |
| --- | --- |
| 필수 조회 | 전체 거래 실패 |
| 부가 조회 | 사전 정의된 경우 부분 성공 가능 |
| 상태변경 | 재시도·보상·중복방지 정책 필요 |
| 비동기 이벤트 | 재처리 Queue와 Dead Letter 관리 |
| 외부 시스템 | 오류코드 변환과 대상 시스템 기록 |

부분 성공은 업무팀·아키텍처팀이 명시적으로 승인한 거래에만 적용한다.

3.11.3 Timeout

```
상위 도메인 Timeout
> 하위 도메인 호출 Timeout 합계 + 내부 처리시간
```

예:

```
CustomerView 전체 Timeout: 3초

Customer 조회: 0.8초
ProductHolding 조회: 0.8초
Contact 조회: 0.6초
조립·여유시간: 0.8초
```

하위 호출 Timeout이 상위 Timeout보다 길어서는 안 된다.

3.11.4 도메인 장애 격리

특정 도메인의 장애가 다른 도메인 전체로 확산되지 않도록 다음을 검토한다.

- 연계 Timeout
- Bulkhead 또는 업무별 Executor
- DB Pool 분리 여부
- Circuit Breaker
- 요청량 통제
- 중요 거래 우선순위
- Cache 대체 가능 여부
- 부분 성공 여부
- 업무 WAR 또는 Tomcat 분리 필요성
하나의 Tomcat에 여러 WAR가 배포되면 JVM·Heap·GC·Connector Thread를 공유하므로 논리적 도메인 분리만으로 물리적 장애 격리가 보장되지는 않는다. 중요 도메인은 별도 WAR·Tomcat·VM 분리를 함께 검토해야 한다.

3.12 정상 예시

3.12.1 Customer 도메인

| 항목 | 예시 |
| --- | --- |
| 업무코드 | SV |
| 도메인명 | Customer |
| 도메인 코드 | CUST |
| 목적 | 고객 핵심 조회정보 제공 |
| ServiceId | SV.Customer.selectSummary |
| 패키지 | com.nh.nsight.sv.customer |
| Handler | SvCustomerHandler |
| Facade | SvCustomerFacade |
| Service | SvCustomerService |
| Rule | SvCustomerInquiryRule |
| DAO | SvCustomerDao |
| Mapper | SvCustomerMapper |
| 소유 데이터 | 고객 조회용 요약모델 |
| 권한 | SV_CUSTOMER_VIEW |
| 감사 | 중요 고객정보 조회 감사 |
| Timeout | 3초 |

3.12.2 Campaign 도메인

```
com.nh.nsight.cm.campaign
 ├─ handler
 │   └─ CmCampaignHandler
 ├─ facade
 │   └─ CmCampaignFacade
 ├─ service
 │   └─ CmCampaignService
 ├─ rule
 │   ├─ CampaignApprovalRule
 │   └─ CampaignPeriodRule
 ├─ model
 │   ├─ Campaign
 │   ├─ CampaignId
 │   ├─ CampaignPeriod
 │   └─ CampaignStatus
 ├─ dao
 └─ mapper
```

대표 ServiceId:

```
CM.Campaign.create
CM.Campaign.requestApproval
CM.Campaign.approve
CM.Campaign.reject
CM.Campaign.cancel
CM.Campaign.selectDetail
```

3.12.3 OM 운영 도메인

| 도메인 | 주요 책임 | 대표 ServiceId |
| --- | --- | --- |
| User | 운영 사용자 관리 | OM.User.updateStatus |
| Authorization | 역할·기능권한 관리 | OM.Authorization.assignRole |
| Menu | 메뉴 기준정보 관리 | OM.Menu.update |
| ServiceCatalog | ServiceId 기준정보 관리 | OM.ServiceCatalog.register |
| TransactionControl | 거래 허용·차단 정책 | OM.TransactionControl.block |
| TimeoutPolicy | 거래 Timeout 정책 | OM.TimeoutPolicy.update |
| Deployment | 배포 버전·이력 관리 | OM.Deployment.register |
| RuntimeDiagnostics | 런타임 상태·장애 진단 | OM.RuntimeDiagnostics.selectStatus |

3.13 금지 예시

3.13.1 포괄적 Common 도메인

```
com.nh.nsight.sv.common.service.CommonService
```

문제

- 업무 책임이 불명확하다.
- 변경 원인이 서로 다른 기능이 집중된다.
- 모든 도메인이 의존하는 거대한 결합점이 된다.
개선

```
날짜 포맷 → tcf-util
공통코드 조회 → Code 도메인
고객 검증 → Customer 도메인
상품 검증 → Product 도메인
```

3.13.2 화면 기준 도메인

```
SV.CustomerMainScreen.select
SV.CustomerPopup.select
```

개선

```
SV.Customer.selectSummary
SV.Customer.selectDetail
```

3.13.3 테이블 기준 도메인

```
SV.CustomerMaster.select
SV.CustomerDetail.select
```

개선

업무 의미에 따라 다음과 같이 정의한다.

```
SV.Customer.selectProfile
SV.Customer.selectRelationship
```

3.13.4 다른 도메인 Mapper 직접 호출

```
public class CampaignService {

    private final CustomerMapper customerMapper; // 금지
}
```

개선

```
public class CampaignService {

    private final CustomerQueryClient customerQueryClient;
}
```

또는 상위 Facade에서 고객 도메인의 공개 계약을 호출한다.

3.13.5 DTO 전 계층 공용

```
화면 Request DTO
= Service 입력
= Mapper Parameter
= DB Result
= 화면 Response
```

문제

화면·업무·DB 구조가 하나의 객체에 결합된다.

개선

```
Request DTO
→ Command/Query
→ Domain Model 또는 Result DTO
→ Response DTO
```

3.13.6 거대 도메인

```
Customer 도메인
- 고객 기본정보
- 고객 행동
- 상담
- 캠페인
- 상품추천
- 마케팅 동의
- 메시지 발송
```

업무 규칙·데이터·담당조직·변경주기가 다르면 분리한다.

```
Customer
CustomerConsent
CustomerBehavior
Contact
Campaign
Recommendation
Message
```

3.13.7 지나치게 작은 도메인

```
CustomerName
CustomerAddress
CustomerPhone
CustomerEmail
```

독립된 규칙·책임·변경주기가 없다면 Customer 도메인의 Value Object 또는 하위 모델로 유지한다.

3.14 연계 규칙

3.14.1 동일 도메인 내부

- 내부 Service·Rule·DAO를 직접 호출할 수 있다.
- 내부 클래스는 다른 도메인에 공개하지 않는다.
- 내부 메서드는 외부 계약으로 간주하지 않는다.
3.14.2 동일 WAR의 다른 도메인

다음 방식 중 하나를 사용한다.

- 공개 Application Interface
- 상위 Facade 또는 Orchestrator
- 도메인 이벤트
- 조회 전용 Read Model
다른 도메인의 DAO·Mapper·Entity 직접 참조는 금지한다.

3.14.3 다른 WAR

다음 표준 연계를 사용한다.

```
tcf-eai
표준 HTTP/JSON 전문
ServiceId
GUID / TraceId
표준 오류코드
Timeout
```

다른 WAR의 Java 클래스를 Gradle 의존성으로 직접 참조하여 업무를 호출하지 않는다.

3.14.4 외부 시스템

외부 모델은 Anti-Corruption Layer를 통해 내부 도메인 모델로 변환한다.

```
외부 응답
  ↓
ExternalClient DTO
  ↓
Translator / Adapter
  ↓
NSIGHT Domain Model
```

외부 시스템의 코드값·필드명·상태값을 도메인 내부 전체에 직접 전파하지 않는다.

3.14.5 이벤트

도메인 이벤트는 완료된 업무 사실을 과거형 의미로 표현한다.

```
CampaignApproved
CustomerConsentChanged
ContactCompleted
MessageSendRequested
```

금지 예:

```
DoCampaign
ProcessCustomer
RunSomething
```

이벤트에는 필요한 최소 정보와 추적정보만 포함한다.

- eventId
- eventType
- occurredAt
- aggregateId
- businessCode
- sourceDomain
- guid
- traceId
- schemaVersion
- 최소 업무 Payload
3.15 데이터 및 상태관리

3.15.1 데이터 분류

| 구분 | 설명 | 처리 기준 |
| --- | --- | --- |
| 소유 데이터 | 도메인이 생성·변경 책임 | 도메인 내부에서만 DML |
| 참조 데이터 | 다른 도메인이 소유 | 공개 조회계약 이용 |
| 기준정보 | 전사·플랫폼 공통 기준 | SoT와 Cache 정책 정의 |
| Read Model | 조회 조합·성능 목적 | 원천과 갱신주기 명시 |
| 이력 데이터 | 변경·감사·분석 목적 | 보존기간·정정정책 정의 |
| 이벤트 데이터 | 비동기 전달·재처리 | 중복·순서·보존 기준 정의 |

3.15.2 상태전이

상태를 단순 문자열 변경으로 처리하지 않는다.

```
DRAFT
  ↓ requestApproval()
REQUESTED
  ↓ approve()
APPROVED
  ↓ start()
RUNNING
  ↓ complete()
COMPLETED
```

허용되지 않은 전이는 Domain Rule 또는 Entity에서 차단한다.

3.15.3 트랜잭션

| 상황 | 기준 |
| --- | --- |
| 하나의 Aggregate 변경 | 단일 로컬 트랜잭션 |
| 동일 도메인 복수 테이블 | Facade 트랜잭션 |
| 다른 도메인 조회 조합 | 읽기 전용 조합 |
| 다른 도메인 복수 변경 | Orchestration·보상·이벤트 검토 |
| 다른 DB 복수 변경 | JTA를 기본 선택하지 않고 대안 우선 검토 |
| 장시간 처리 | 비동기 Job 또는 이벤트 방식 |

3.15.4 Cache

Cache도 도메인 소유권을 따른다.

```
Cache Key:
{환경}:{업무코드}:{도메인}:{데이터유형}:{식별자}
```

예:

```
PRD:SV:CUSTOMER:SUMMARY:CUST0001
PRD:PD:PRODUCT:DETAIL:P000123
```

Cache 무효화 책임은 원천 데이터를 변경하는 소유 도메인에 둔다.

3.16 성능·용량·확장성

3.16.1 도메인별 성능 등급

| 등급 | 특성 | 예 |
| --- | --- | --- |
| A | 사용자 핵심·고빈도·낮은 지연 | 고객요약 조회 |
| B | 일반 온라인 거래 | 캠페인 상세 조회 |
| C | 복잡 조회·대량 데이터 | 고객 종합분석 |
| D | 배치·비동기 처리 | 행동데이터 집계 |

도메인 정의서에 다음 항목을 포함한다.

- 예상 TPS
- p95 목표
- Timeout
- 주요 SQL 수
- 외부 호출 수
- 평균 Payload
- Cache 적용 여부
- DB Pool 영향
- 대량 처리 여부
3.16.2 동기 호출 수 제한

하나의 온라인 거래가 많은 도메인을 순차 호출하면 응답시간과 장애 가능성이 증가한다.

```
권장:
핵심 동기 호출 1~3개

주의:
4~5개 이상

재설계 검토:
6개 이상 또는 다단계 연쇄 호출
```

정확한 한도는 성능시험으로 확정한다.

3.16.3 도메인 분리 판단

다음 조건이 반복되면 별도 모듈·WAR·Tomcat 분리를 검토한다.

| 판단 신호 | 설명 |
| --- | --- |
| 독립 배포 요구 | 다른 업무와 배포 주기가 다름 |
| 부하 특성 차이 | CPU·메모리·DB 사용 특성이 다름 |
| 장애 영향 큼 | 장애가 다른 핵심 업무에 전파 |
| 보안 경계 차이 | 별도 접근통제·망분리가 필요 |
| 기술 스택 차이 | 실시간·배치·대용량 등 실행모델 차이 |
| 조직 독립성 | 별도 조직이 전체 생명주기 책임 |
| 확장 요구 | 특정 도메인만 수평 확장 필요 |

3.17 보안·개인정보·감사

3.17.1 도메인별 보안속성

도메인 정의서에 다음을 반드시 기록한다.

| 항목 | 예 |
| --- | --- |
| 인증 필요 여부 | Y |
| 기능권한 | SV_CUSTOMER_VIEW |
| 데이터권한 | 소속 지점·담당 고객 |
| 개인정보 등급 | 중요 개인정보 |
| 마스킹 | 이름·전화번호·식별번호 |
| 다운로드 허용 | 별도 권한 필요 |
| 감사 대상 | 조회·변경 모두 |
| 보존기간 | 감사정책에 따름 |
| 암호화 | 저장·전송 기준 |
| 관리자 접근 | 사유 입력·승인 필요 여부 |

3.17.2 인증정보 사용

사용자·지점·권한은 검증된 AuthenticationContext를 신뢰 원천으로 사용한다.

화면이 전달한 사용자 ID를 그대로 신뢰하지 않는다.

```
JWT 또는 세션 검증
  ↓
AuthenticationContext
  ↓
STF 정합성 검증
  ↓
도메인 데이터권한 검증
```

3.17.3 감사로그

감사로그에는 다음 도메인 정보가 포함되어야 한다.

- businessCode
- domainCode
- serviceId
- transactionCode
- userId
- branchId
- guid
- traceId
- 대상 식별자의 마스킹·해시값
- 행위
- 성공·실패
- 실패 사유
- 처리시각
요청·응답 원문과 개인정보 전체를 기록하지 않는다.

3.18 운영·모니터링·장애 대응

3.18.1 도메인별 운영지표

| 지표 | 설명 |
| --- | --- |
| 호출 건수 | 도메인별·ServiceId별 TPS |
| 성공률 | 정상 처리 비율 |
| 업무 오류율 | 규칙 위반·데이터 미존재 |
| 시스템 오류율 | DB·연계·프로그램 오류 |
| Timeout | 거래 제한시간 초과 |
| p50·p95·p99 | 응답시간 분포 |
| DB 처리시간 | SQL 실행·Connection 대기 |
| 외부 호출시간 | 대상 도메인·시스템별 |
| 활성 거래 | 현재 실행 중 거래 |
| 변경 건수 | 도메인 배포·정책 변경 |

3.18.2 장애 식별

로그와 진단정보에서 다음과 같이 도메인을 식별할 수 있어야 한다.

```
businessCode=SV
domainCode=CUST
serviceId=SV.Customer.selectSummary
guid=...
traceId=...
```

장애 대응은 다음 순서로 수행한다.

```
ServiceId 확인
→ 소속 도메인 확인
→ 도메인 담당조직 확인
→ 현재 처리단계 확인
→ SQL·외부연계 확인
→ 영향 화면과 거래 확인
→ 차단·우회·복구 수행
```

3.18.3 도메인 비상통제

중요 장애 시 도메인 또는 ServiceId 단위로 다음 통제가 가능해야 한다.

- 신규 거래 차단
- 조회만 허용
- 변경 거래 차단
- 특정 채널 차단
- 특정 ServiceId 차단
- Timeout 단축
- 유입량 제한
- 대체 조회모델 사용
- 기능 점검 안내
도메인 전체 차단과 개별 ServiceId 차단을 구분한다.

3.19 자동검증 및 품질 Gate

3.19.1 ServiceId 검증

정규식 예:

```
^[A-Z]{2,3}\.[A-Z][A-Za-z0-9]*\.[a-z][A-Za-z0-9]*$
```

검증 항목:

- 등록된 업무코드인가
- 등록된 도메인명인가
- 도메인명이 PascalCase인가
- 행위가 lowerCamelCase인가
- ServiceId가 중복되지 않는가
- Handler가 등록되어 있는가
- OM Catalog와 일치하는가
- 거래통제·Timeout 정책이 존재하는가
3.19.2 패키지 검증

```
ServiceId: SV.Customer.selectSummary

허용 Handler 패키지:
com.nh.nsight.sv.customer.handler
```

다음 불일치를 자동검출한다.

```
ServiceId 도메인 = Customer
Package 도메인 = campaign
→ 빌드 실패
```

3.19.3 ArchUnit 검증

예시 규칙:

```
@AnalyzeClasses(packages = "com.nh.nsight")
class DomainArchitectureTest {

    @ArchTest
    static final ArchRule handlerOnlyCallsFacade =
        classes()
            .that().resideInAPackage("..handler..")
            .should().onlyDependOnClassesThat()
            .resideInAnyPackage(
                "..handler..",
                "..facade..",
                "..dto..",
                "java..",
                "org.springframework..",
                "com.nh.nsight.tcf..");

    @ArchTest
    static final ArchRule ruleMustNotAccessDao =
        noClasses()
            .that().resideInAPackage("..rule..")
            .should().dependOnClassesThat()
            .resideInAnyPackage("..dao..", "..mapper..", "..client..");
}
```

추가 검증 대상:

- Handler → DAO 직접 호출 금지
- Facade → Mapper 직접 호출 금지
- Rule → DB·외부 Client 의존 금지
- DAO → Service 역참조 금지
- 다른 도메인의 Mapper 의존 금지
- 도메인 간 순환 의존 금지
- 다른 WAR 업무 클래스 직접 의존 금지
3.19.4 데이터 소유권 검증

Mapper SQL에서 사용한 테이블을 분석하여 다음을 확인한다.

| 검증 | 기준 |
| --- | --- |
| 소유 테이블 DML | 허용 |
| 참조 테이블 SELECT | 승인된 경우 허용 |
| 다른 도메인 테이블 INSERT·UPDATE·DELETE | 실패 |
| 미등록 테이블 사용 | 실패 |
| 통합 View 사용 | 승인정보 확인 |
| 민감 테이블 접근 | 권한·감사 기준 확인 |

3.19.5 산출물 정합성 검증

```
도메인 정의서
↔ ServiceId 목록
↔ Handler
↔ 프로그램 설계서
↔ Mapper SQL
↔ DB 객체
↔ OM Catalog
↔ 권한·감사 기준
↔ 테스트 케이스
```

필수 연결이 하나라도 누락되면 운영 반영 품질 Gate를 통과하지 못한다.

독립 설계 대상은 목적·책임·정상·금지 예시·연계·자동검증·변경관리와 함께 설계서·코드·OM 기준정보·운영로그까지 추적 가능해야 한다.

3.20 테스트 시나리오

| 번호 | 테스트 | 기대 결과 |
| --- | --- | --- |
| 1 | 정상 ServiceId 호출 | 등록된 도메인 Handler 실행 |
| 2 | 미등록 도메인 ServiceId | 기동 또는 배포 Gate 실패 |
| 3 | ServiceId·패키지 도메인 불일치 | 정적검증 실패 |
| 4 | Handler가 DAO 직접 호출 | ArchUnit 실패 |
| 5 | Rule이 Mapper 호출 | ArchUnit 실패 |
| 6 | 다른 도메인 Mapper 참조 | 빌드 실패 |
| 7 | 다른 도메인 소유 테이블 UPDATE | SQL 품질 Gate 실패 |
| 8 | 도메인 간 순환 의존 | 의존성 검사 실패 |
| 9 | 업무 규칙 위반 | 표준 업무 오류 반환 |
| 10 | 다른 도메인 호출 Timeout | 상위 거래 Timeout·오류정책 적용 |
| 11 | 다른 도메인 부분 실패 | 승인된 부분 성공 정책 적용 |
| 12 | 이벤트 중복 수신 | 중복 처리 방지 |
| 13 | 이벤트 순서 변경 | 버전·상태 검증으로 비정상 전이 차단 |
| 14 | 개인정보 조회 | 권한·마스킹·감사 정상 |
| 15 | 도메인 ServiceId 차단 | Handler 미실행·통제 오류 반환 |
| 16 | 도메인 변경 후 회귀테스트 | 영향 ServiceId 전체 검증 |
| 17 | 도메인 분리 후 구버전 호출 | 호환 또는 명시적 폐기 응답 |
| 18 | Read Model 지연 | 데이터 기준시각 표시·정책 적용 |
| 19 | 고부하 도메인 집중 | 다른 도메인 영향과 격리 수준 확인 |
| 20 | 배포 후 Smoke Test | 대표 ServiceId와 OM 정합 확인 |

3.21 체크리스트

3.21.1 도메인 식별

| 점검 항목 | 확인 |
| --- | --- |
| 도메인의 업무 목적을 한 문장으로 설명할 수 있는가 | □ |
| 핵심 책임과 제외 책임이 구분되어 있는가 | □ |
| 도메인이 화면이나 테이블 기준으로 정의되지 않았는가 | □ |
| 같은 용어가 업무·설계·코드에서 일관되는가 | □ |
| 도메인 크기가 지나치게 크거나 작지 않은가 | □ |
| 담당 업무조직과 개발조직이 지정되어 있는가 | □ |

3.21.2 ServiceId·패키지

| 점검 항목 | 확인 |
| --- | --- |
| ServiceId 도메인명이 등록된 표준명인가 | □ |
| 패키지 도메인명과 ServiceId가 일치하는가 | □ |
| 도메인 우선 패키지 구조를 따르는가 | □ |
| ServiceId가 Handler와 연결되어 있는가 | □ |
| OM Service Catalog에 등록되어 있는가 | □ |
| 거래코드·Timeout·권한·감사가 정의되어 있는가 | □ |

3.21.3 프로그램

| 점검 항목 | 확인 |
| --- | --- |
| Handler는 Facade만 호출하는가 | □ |
| Facade에 트랜잭션 경계가 있는가 | □ |
| Service가 업무 흐름을 담당하는가 | □ |
| Rule이 DB·외부 시스템에 의존하지 않는가 | □ |
| DAO가 업무 판단을 수행하지 않는가 | □ |
| Mapper가 SQL 실행만 담당하는가 | □ |
| 다른 도메인의 내부 프로그램을 직접 참조하지 않는가 | □ |
| DTO가 화면·도메인·DB 목적에 따라 분리되어 있는가 | □ |

3.21.4 데이터

| 점검 항목 | 확인 |
| --- | --- |
| 소유 데이터와 참조 데이터가 구분되어 있는가 | □ |
| 데이터 변경 책임이 하나의 도메인에 있는가 | □ |
| 다른 도메인 테이블에 직접 DML하지 않는가 | □ |
| 통합 조회는 Read Model 또는 승인된 View를 사용하는가 | □ |
| Cache 무효화 책임이 명확한가 | □ |
| 개인정보 등급과 마스킹 기준이 정의되어 있는가 | □ |

3.21.5 연계·운영

| 점검 항목 | 확인 |
| --- | --- |
| 도메인 간 호출 계약이 명확한가 | □ |
| 동기·비동기 선택 근거가 있는가 | □ |
| Timeout·재시도·중복방지 기준이 있는가 | □ |
| GUID·TraceId가 전 구간 유지되는가 | □ |
| 도메인별 운영지표를 조회할 수 있는가 | □ |
| 도메인별 장애 담당조직과 Runbook이 있는가 | □ |
| ServiceId 또는 도메인 단위 거래통제가 가능한가 | □ |

3.21.6 자동검증

| 점검 항목 | 확인 |
| --- | --- |
| ServiceId 정규식 검증이 적용되었는가 | □ |
| ServiceId 중복을 기동 시 차단하는가 | □ |
| ArchUnit 계층 검증이 적용되었는가 | □ |
| 도메인 간 순환 의존성을 검출하는가 | □ |
| 다른 도메인 Mapper 접근을 차단하는가 | □ |
| 데이터 소유권 위반 SQL을 검출하는가 | □ |
| OM 등록정보와 코드의 정합성을 검증하는가 | □ |
| 대표 거래 Smoke Test가 자동화되어 있는가 | □ |

3.22 변경·호환성·폐기 관리

3.22.1 도메인 신규 등록

```
업무 요구 분석
  ↓
도메인 후보 정의
  ↓
기존 도메인과 중복 여부 검토
  ↓
책임·용어·데이터 소유권 정의
  ↓
AA·업무·DA·보안 검토
  ↓
도메인 등록 승인
  ↓
ServiceId·패키지·OM 등록
  ↓
자동검증·테스트 적용
```

3.22.2 도메인 분리

다음 경우 분리를 검토한다.

- 책임이 둘 이상으로 명확히 분리됨
- 담당 조직과 변경주기가 다름
- 데이터 소유권이 독립됨
- 프로그램 규모와 결합도가 과도함
- 독립 배포·확장·장애 격리가 필요함
- 보안등급이 다름
분리 시 다음 영향도를 분석한다.

```
기존 ServiceId
패키지
Handler
데이터 소유권
화면
연계 계약
오류코드
권한
감사
OM 정책
로그·모니터링
테스트
배포 구조
```

3.22.3 도메인 통합

책임이 지나치게 작고 독립 변경·규칙·데이터가 없는 도메인은 상위 도메인으로 통합한다.

기존 ServiceId는 즉시 삭제하지 않고 다음 절차를 적용한다.

- 신규 ServiceId 제공
- 구 ServiceId Deprecated 표시
- 호출 화면·시스템 전환
- 병행 운영
- 미사용 검증
- 폐기 승인
- Handler·OM 정책·문서 제거
3.22.4 도메인명 변경

도메인명은 ServiceId와 외부 계약에 포함되므로 단순 Rename으로 처리하지 않는다.

```
SV.Customer.selectSummary
→ SV.Client.selectSummary
```

위 변경은 새로운 거래 계약으로 간주한다.

원칙적으로 기존 명칭을 유지하고 업무 의미가 실질적으로 변경된 경우에만 신규 ServiceId를 생성한다.

3.22.5 호환성

| 변경 유형 | 호환성 판단 |
| --- | --- |
| 내부 Rule 구현 변경 | 외부 계약 유지 시 호환 |
| 내부 Mapper 변경 | 응답 계약 유지 시 호환 |
| 선택 응답필드 추가 | 소비자 계약 검토 후 조건부 호환 |
| 필수 요청필드 추가 | 비호환 |
| 필드 의미 변경 | 비호환 |
| ServiceId 변경 | 비호환 |
| 도메인 소유권 변경 | 영향분석 필수 |
| 이벤트 Schema 변경 | 버전관리 필수 |
| 오류코드 삭제·의미 변경 | 비호환 가능성 높음 |

3.22.6 폐기

폐기 대상:

- 미사용 ServiceId
- 대체된 도메인 계약
- 미사용 Mapper·SQL
- 폐기 Read Model
- 구버전 이벤트
- 불필요한 공통 모델
- 임시 호환 Adapter
폐기 전 다음을 확인한다.

- 최근 호출 이력
- 화면·배치·외부 시스템 사용 여부
- OM 등록정보
- 거래통제·Timeout
- 권한·메뉴
- 감사·로그 조회
- 데이터 보존
- 롤백 가능성
4. 시사점

4.1 핵심 아키텍처 판단

첫째, 업무코드와 도메인은 같은 개념이 아니다

```
업무코드/WAR
= 상위 업무 및 배포 경계

도메인
= 업무 규칙과 변경 책임 경계
```

하나의 업무 WAR에는 여러 도메인이 존재할 수 있다.

둘째, NSIGHT TCF는 순수 DDD보다 실용적 도메인 중심 계층 구조가 적합하다

기존의 Handler → Facade → Service → Rule → DAO → Mapper 구조는 유지한다.

다만 패키지·ServiceId·데이터 소유권을 도메인 중심으로 정렬한다.

```
TCF 실행 표준
+ 도메인 중심 업무 경계
+ 계층별 책임 분리
```

셋째, ServiceId의 도메인 구간은 운영 계약이다

SV.Customer.selectSummary의 Customer는 단순 분류값이 아니다.

다음 항목을 연결하는 운영·개발 계약이다.

```
도메인 정의
패키지
Handler
프로그램
권한
오류코드
Timeout
거래로그
감사로그
담당조직
```

넷째, 데이터 소유권이 도메인 경계의 핵심이다

다른 도메인의 테이블을 직접 변경할 수 있다면 논리적인 도메인 분리는 실질적인 의미가 없다.

```
도메인 분리
= 프로그램 패키지 분리
+ 데이터 변경권한 분리
+ 공개 계약 분리
```

다섯째, 논리적 분리와 물리적 장애 격리는 별도 판단이다

패키지와 도메인을 분리해도 동일 Tomcat·JVM·Thread·DB Pool을 공유하면 장애가 확산될 수 있다.

중요 도메인은 부하·가용성·보안 기준에 따라 별도 WAR·Tomcat·VM 분리를 추가 검토해야 한다.

4.2 주요 위험

| 위험 | 영향 |
| --- | --- |
| 도메인을 화면 분류로 대체 | 화면 변경 시 전체 구조 불안정 |
| common 남용 | 전 도메인의 강결합 지점 발생 |
| 거대 Customer·Management 도메인 | 변경 충돌과 책임 불명확 |
| 다른 도메인 DB 직접 접근 | 소유권 붕괴와 변경 영향 확산 |
| ServiceId 도메인명 임의 사용 | 운영 분류와 코드 구조 불일치 |
| 도메인 간 동기호출 증가 | 응답지연·연쇄 장애 |
| 도메인 모델 과도 적용 | 단순 조회까지 복잡도 증가 |
| 도메인 모델 미적용 | 복잡한 상태규칙이 Service에 산재 |
| 조직 경계와 도메인 불일치 | 승인·운영 책임 충돌 |
| 변경관리 부재 | 구 ServiceId와 데이터 계약 장기 잔존 |

4.3 우선 보완 과제

| 우선순위 | 과제 | 산출물 |
| --- | --- | --- |
| 1 | NSIGHT 전체 업무코드와 도메인 후보 정리 | 도메인 카탈로그 |
| 2 | 도메인별 책임·제외책임 정의 | 도메인 정의서 |
| 3 | ServiceId 도메인명 정규화 | ServiceId 표준대장 |
| 4 | 패키지 구조를 도메인 우선으로 정렬 | 패키지 구조 정의서 |
| 5 | 도메인별 데이터 소유권 정의 | 데이터 소유권 매트릭스 |
| 6 | 도메인 간 호출관계 식별 | Context Map·의존성도 |
| 7 | 다른 도메인 Mapper·테이블 직접 접근 제거 | 개선과제 대장 |
| 8 | OM에 도메인 코드와 담당조직 등록 | OM Domain Catalog |
| 9 | ArchUnit·ServiceId·SQL 소유권 검사 | CI/CD 품질 Gate |
| 10 | 대표 복잡 도메인에 Aggregate 시범 적용 | 선도개발 검증 결과 |

4.4 중장기 발전 방향

1단계 — 분류 표준화

```
업무코드
→ 도메인
→ ServiceId
→ 패키지
→ 담당조직
```

2단계 — 데이터 소유권 정립

```
도메인
→ 소유 테이블
→ 참조 테이블
→ Read Model
→ 변경 계약
```

3단계 — 자동검증

```
코드 스캔
→ 의존성 분석
→ ServiceId 정합성
→ SQL 소유권
→ OM 정책 검증
```

4단계 — 물리적 경계 최적화

```
도메인별 부하·장애 분석
→ 중요 도메인 식별
→ WAR·Tomcat·DB Pool 분리
→ 독립 확장·배포
```

5단계 — 이벤트 기반 확장

상태변경이 많은 도메인 간 결합을 동기 호출에서 이벤트 기반으로 점진적으로 전환한다.

5. 마무리말

NSIGHT TCF의 도메인 설계는 패키지 디렉터리를 나누는 작업이 아니다.

핵심은 다음 질문에 명확하게 답할 수 있도록 만드는 것이다.

```
이 업무는 어느 도메인의 책임인가?
이 거래는 어떤 ServiceId로 실행되는가?
어떤 프로그램과 규칙이 담당하는가?
어떤 데이터를 소유하고 변경할 수 있는가?
다른 도메인은 어떤 계약으로 접근하는가?
오류·Timeout·장애 시 누가 대응하는가?
변경 시 어떤 화면·거래·SQL이 영향을 받는가?
```

최종 권장 구조는 다음과 같다.

```
업무코드 / WAR
  ↓
도메인
  ↓
ServiceId
  ↓
Handler
  ↓
Facade
  ↓
Service
  ├─ Rule·Domain Model
  ├─ DAO·Mapper
  └─ Domain Client·Event
  ↓
소유 데이터
  ↓
권한·감사·로그·운영정책
```

NSIGHT TCF 개발 방법론에서 도메인은 다음 세 가지의 공통 기준점이어야 한다.

```
업무 책임의 기준점
프로그램 구조의 기준점
데이터와 운영 소유권의 기준점
```

도메인 경계를 명확하게 정의하면 신규 업무 추가, 프로그램 변경, 데이터 영향 분석, 장애 대응 및 조직 간 책임 조정이 모두 동일한 구조를 중심으로 수행될 수 있다.

