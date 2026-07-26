
## 초보 IT 개발자를 위한 NSIGHT TCF 개발 입문서

## 제11부. 명명규칙·패키지·화면 ID·ServiceId·거래코드·DB 객체 표준

## 1. 도입 전 안내말

제10부에서는 OM 운영관리와 관측성을 이용하여 화면에서 발생한 문제를 ServiceId, WAR, Thread, DB Pool, Mapper와 SQL까지 추적하는 방법을 배웠습니다.

그런데 이러한 추적이 가능하려면 모든 개발 산출물과 프로그램 구성요소가 서로 연결될 수 있는 이름을 가져야 합니다.

다음과 같은 이름을 생각해 봅시다.

```
Controller1
TestService
CommonUtil2
process
execute
TB001
screen01
TR0001
```

이 이름만 보고는 다음 내용을 알 수 없습니다.

```
어느 업무인가?

어떤 기능인가?

조회인가, 등록인가?

어느 화면에서 호출하는가?

어느 WAR에 배포되는가?

어느 테이블을 사용하는가?

운영 중지 대상은 무엇인가?

장애가 발생하면 누가 담당하는가?
```

이름이 불명확하면 개발자는 코드를 읽어야 의미를 알 수 있고, 운영자는 로그만으로 문제를 찾기 어렵습니다.

반대로 다음 이름을 살펴봅시다.

```
화면 ID
SV-CUS-0001

화면 이벤트 ID
SV-CUS-0001-E01

ServiceId
SV.Customer.selectSummary

거래코드
SV-INQ-0001

Handler
SvCustomerHandler

Facade
SvCustomerFacade

Service
SvCustomerService

Mapper
SvCustomerMapper.selectCustomerSummary

테이블
SV_CUSTOMER_SUMMARY
```

이 이름들은 서로 다른 기술요소이지만 동일한 업무 의미를 공유합니다.

```
SV
= Single View 업무

Customer
= 고객 도메인

selectSummary
= 고객 요약조회
```

따라서 화면에서 장애가 발생하면 다음과 같이 추적할 수 있습니다.

```
화면
SV-CUS-0001

→ 이벤트
SV-CUS-0001-E01

→ ServiceId
SV.Customer.selectSummary

→ 거래코드
SV-INQ-0001

→ Handler
SvCustomerHandler

→ Service
SvCustomerService.selectSummary

→ Mapper
SvCustomerMapper.selectCustomerSummary

→ SQL
SV-CUST-SEL-001

→ 테이블
SV_CUSTOMER_SUMMARY
```

명명규칙은 단순한 코딩 스타일이 아닙니다.

```
명명규칙
= 업무 의미를 코드와 운영정보에 전달하는 공통 언어
```

제11부의 핵심 원칙은 다음과 같습니다.

```
이름만 보고 업무와 역할을 추정할 수 있어야 한다.

같은 개념은 모든 산출물에서 같은 용어를 사용한다.

하나의 식별자는 하나의 의미만 가져야 한다.

업무코드·도메인·행위를 일관되게 연결한다.

약어는 개인이 임의로 만들지 않고 표준사전으로 관리한다.

명명규칙은 문서에만 두지 않고 CI에서 자동검증한다.
```

## 2. 제11부 개요

### 2.1 목적

제11부의 목적은 초보 개발자가 NSIGHT TCF의 업무코드, 도메인, 모듈, 패키지, 화면, 거래, Java 클래스, Mapper와 DB 객체를 일관된 체계로 이름 짓도록 돕는 것입니다.

학습을 마친 뒤에는 다음 작업을 수행할 수 있어야 합니다.

- 업무코드와 도메인 경계를 정의한다.
- 업무세구분코드를 설계한다.
- 모듈·WAR·Context Path를 일관되게 구성한다.
- Java Package를 표준 계층으로 구성한다.
- 화면 ID와 화면 이벤트 ID를 정의한다.
- 메뉴 ID와 화면 ID를 구분한다.
- ServiceId를 업무·도메인·행위 기준으로 정의한다.
- 거래코드를 처리유형에 따라 부여한다.
- Handler·Facade·Service·Rule·DAO·Mapper 이름을 정의한다.
- Request·Response·Command·Data DTO를 구분한다.
- Mapper 메서드와 SQL ID를 일치시킨다.
- 테이블·컬럼·인덱스·제약조건 이름을 정의한다.
- 파일·Batch·Cache·환경설정 이름을 정의한다.
- 화면부터 DB까지 추적성 매트릭스를 작성한다.
- 명명규칙 위반을 CI 품질 Gate로 자동검증한다.

### 2.2 적용범위

| 영역 | 주요 내용 |
| --- | --- |
| 업무 분류 | 업무코드·업무세구분코드 |
| 도메인 | 도메인명·하위 도메인 |
| 모듈 | Gradle Module·JAR·WAR |
| 배포 | WAR명·Context Path |
| 패키지 | 기본 Package·계층 Package |
| 화면 | 화면 ID·화면그룹·이벤트 ID |
| 메뉴 | 메뉴 ID·메뉴 계층 |
| 거래 | ServiceId·거래코드 |
| Java | Class·Interface·Method |
| DTO | Request·Response·Command·Data |
| MyBatis | Mapper·SQL ID·ResultMap |
| DB | Table·Column·Index·Constraint |
| 운영 | Batch·Cache·환경설정·로그 |
| 추적성 | 화면부터 DB까지 연결 |
| 품질 | 정규식·중복·참조 자동검증 |

### 2.3 대상 독자

- 클래스와 메서드 이름을 정하기 어려운 초보 개발자
- 화면 ID와 메뉴 ID가 같은 것이라고 생각하는 개발자
- ServiceId와 거래코드의 차이가 어려운 개발자
- Mapper를 테이블 단위로 만들어야 하는지 고민하는 개발자
- 업무코드와 도메인코드를 혼용하는 개발자
- DB 컬럼 약어를 개인적으로 만드는 개발자
- 개발 산출물과 실제 소스를 연결해야 하는 개발자
- 명명규칙을 자동검증하려는 아키텍트와 품질담당자

### 2.4 선행조건

다음 내용을 이해하고 있어야 합니다.

```
화면 이벤트는 ServiceId를 호출한다.

ServiceId는 Handler를 선택한다.

Handler는 Facade를 호출한다.

Facade는 Service와 트랜잭션을 조정한다.

Service는 Rule과 DAO를 사용한다.

DAO는 Mapper를 통해 SQL을 실행한다.

OM은 ServiceId와 거래코드를 관리한다.
```

### 2.5 주요 용어

| 용어 | 쉬운 설명 |
| --- | --- |
| Naming Convention | 이름을 만드는 공통 규칙 |
| Identifier | 대상을 유일하게 구분하는 값 |
| Business Code | 업무영역을 나타내는 짧은 코드 |
| Sub-business Code | 업무 내부의 세부 영역 코드 |
| Domain | 동일한 업무 책임을 가지는 개념영역 |
| Vocabulary | 프로젝트에서 사용하는 표준 업무용어 |
| Abbreviation | 길이가 긴 단어를 줄여 쓴 표현 |
| Prefix | 이름 앞에 붙는 식별문자 |
| Suffix | 이름 뒤에 붙는 역할문자 |
| Traceability | 화면부터 소스·DB까지 연결해 추적하는 능력 |
| Canonical Name | 동일 개념에 대해 공식적으로 사용하는 이름 |
| Registry | 코드와 이름을 등록·관리하는 대장 |
| Reserved Word | 시스템에서 사용이 제한된 단어 |
| Deprecated | 더 이상 신규 사용하지 않는 폐기 예정 상태 |

## 제86장. 명명규칙이 필요한 이유

### 86.1 이름은 가장 작은 설계서다

다음 두 메서드를 비교해 봅시다.

```
process();
selectCustomerSummary();
```

첫 번째 이름은 코드를 열어 보기 전까지 의미를 알 수 없습니다.

두 번째 이름은 다음 내용을 알려줍니다.

```
select
= 조회

Customer
= 고객

Summary
= 요약정보
```

좋은 이름은 짧은 설계서 역할을 합니다.

### 86.2 잘못된 이름이 만드는 비용

```
업무 이해시간 증가

코드 리뷰시간 증가

중복 기능 생성

운영 추적 실패

잘못된 테이블 접근

담당 업무 혼동

변경 영향분석 누락

신규 개발자 교육비 증가
```

### 86.3 이름의 네 가지 질문

모든 이름은 가능하면 다음 질문에 답해야 합니다.

```
어느 업무인가?

어느 도메인인가?

무엇을 처리하는가?

어떤 역할인가?
```

예:

```
SvCustomerSummaryResponse
```

| 요소 | 의미 |
| --- | --- |
| Sv | Single View 업무 |
| Customer | 고객 도메인 |
| Summary | 요약정보 |
| Response | 응답 DTO |

### 86.4 이름과 식별자의 차이

표시명:

```
고객 종합정보 조회
```

식별자:

```
SV.Customer.selectSummary
```

표시명은 사용자와 문서에서 한글로 표현할 수 있습니다.

식별자는 프로그램과 운영 시스템이 안정적으로 참조해야 하므로 임의로 변경하면 안 됩니다.

### 86.5 좋은 이름의 조건

```
업무 의미가 분명하다.

너무 짧지 않다.

불필요하게 길지 않다.

동일 개념에 같은 단어를 사용한다.

기술 구현보다 업무 목적을 표현한다.

역할과 계층이 구분된다.

검색하기 쉽다.
```

### 86.6 나쁜 이름의 유형

#### 의미 없는 이름

```
data
info
temp
test
value
object
```

#### 지나치게 일반적인 이름

```
CommonService
CommonUtil
Manager
Processor
Handler1
```

#### 구현기술 중심 이름

```
OracleCustomerService
JsonCampaignHandler
HttpProductManager
```

업무 목적보다 현재 구현기술에 종속됩니다.

#### 숫자 접미사

```
CustomerService2
CustomerServiceNew
CustomerServiceFinal
```

버전과 변경이력을 이름에 임의로 표현하면 안 됩니다.

### 86.7 표준 용어사전

프로젝트에서 동일 개념을 여러 단어로 사용하면 안 됩니다.

예:

```
고객
Customer
Client
Member
User
```

이들이 같은 개념인지 다른 개념인지 정의해야 합니다.

표준사전 예:

| 한글 용어 | 영문명 | 약어 | 정의 |
| --- | --- | --- | --- |
| 고객 | Customer | CUST | 금융상품과 서비스를 이용하는 개인·법인 |
| 캠페인 | Campaign | CAM | 마케팅 실행 단위 |
| 상품 | Product | PROD | 판매·추천 대상 금융상품 |
| 상담 | Contact | CTCT | 고객 접촉·상담 이력 |
| 지점 | Branch | BR | 영업점 조직 |

### 86.8 금지어와 예약어

다음 단어는 의미가 모호하거나 기술 예약어와 충돌할 수 있습니다.

```
DATA
INFO
VALUE
OBJECT
TYPE
DATE
NUMBER
USER
ORDER
GROUP
COMMENT
```

DB 예약어는 테이블·컬럼명으로 직접 사용하지 않습니다.

### 제86장 요약

```
좋은 이름은 업무와 역할을 설명한다.

명명규칙은 개발 편의를 위한 장식이 아니라
추적성·운영성·변경관리의 기반이다.

동일 개념에는 동일한 표준용어를 사용한다.
```

## 제87장. 업무코드·도메인·업무세구분코드

### 87.1 업무코드

업무코드는 시스템의 가장 큰 업무영역을 나타냅니다.

NSIGHT 업무코드 예:

| 업무영역 | 코드 | 영문명 |
| --- | --- | --- |
| 공통 | CC | Common |
| 고객 통합 | IC | Integrated Customer |
| 개인고객 | PC | Private Customer |
| 기업고객 | BC | Business Customer |
| 미니 싱글뷰 | MS | Mini Single View |
| 싱글뷰 | SV | Single View |
| 상품 | PD | Product |
| 캠페인 | CM | Campaign |
| EBM | EB | Event Based Marketing |
| 이벤트 처리 | EP | Event Processing |
| 행동 처리 | BP | Behavior Processing |
| 행동 데이터 | BD | Behavior Data |
| 영업지원 | SS | Sales Support |
| 공통 서비스 | CS | Common Service |
| 상담 | CT | Contact |
| 관리 | MG | Management |
| 운영관리 | OM | Operation Management |

실제 최종 코드목록은 공식 업무코드 대장을 기준으로 관리합니다.

### 87.2 업무코드의 사용범위

업무코드는 다음 영역에서 동일하게 사용합니다.

```
WAR 이름

Context Path

Java Package

ServiceId Prefix

거래코드 Prefix

화면 ID Prefix

DB 객체 Prefix

로그·Metric Label

운영 담당구분
```

예:

```
업무코드
SV

WAR
sv-service.war

Context
/sv

Package
com.nh.nsight.marketing.sv

ServiceId
SV.Customer.selectSummary

화면 ID
SV-CUS-0001

테이블
SV_CUSTOMER_SUMMARY
```

### 87.3 업무코드 정합성

다음 값은 서로 일치해야 합니다.

```
URL Context
/sv

Header businessCode
SV

ServiceId Prefix
SV

대상 WAR
sv-service
```

불일치 예:

```
POST /sv/online

businessCode = IC

serviceId = CM.Campaign.create
```

STF에서 차단해야 합니다.

### 87.4 도메인

도메인은 특정 업무 안에서 동일한 책임과 규칙을 가지는 개념영역입니다.

예:

```
SV 업무
├─ Customer
├─ Product
├─ Activity
└─ Dashboard
CM 업무
├─ Campaign
├─ Target
├─ Approval
└─ Execution
```

### 87.5 도메인 이름

도메인 이름은 명사형 단수 영문을 권장합니다.

권장:

```
Customer
Campaign
Product
Approval
Target
```

금지:

```
Customers
CampaignProcessing
DoApproval
CustomerDataManagement
```

도메인은 처리행위보다 업무대상을 나타냅니다.

### 87.6 업무세구분코드

화면 ID와 관리대장에서 업무 하위영역을 짧게 표현하기 위해 업무세구분코드를 사용할 수 있습니다.

예:

| 업무코드 | 업무세구분 | 코드 |
| --- | --- | --- |
| SV | 고객 | CUS |
| SV | 상품 | PRD |
| SV | 활동 | ACT |
| CM | 캠페인 | CAM |
| CM | 대상자 | TGT |
| CM | 승인 | APR |
| OM | 서비스관리 | SVC |
| OM | 거래로그 | TXL |
| OM | Batch 관리 | BAT |

### 87.7 업무세구분코드와 도메인

가능하면 업무세구분코드는 도메인과 대응합니다.

```
CUS
↔ Customer

CAM
↔ Campaign

APR
↔ Approval
```

그러나 화면 분류와 애플리케이션 도메인이 항상 완전히 같을 필요는 없습니다.

화면 중심 분류를 위해 별도의 화면그룹코드를 사용할 수도 있습니다.

### 87.8 코드 길이

권장 원칙:

```
업무코드
2자리 영문 대문자

업무세구분코드
3자리 영문 대문자

일련번호
4자리 숫자
```

예:

```
SV-CUS-0001
```

코드 길이를 무분별하게 늘리면 화면과 DB 객체 이름이 지나치게 길어질 수 있습니다.

### 87.9 코드 재사용 금지

폐기된 업무코드를 다른 의미로 다시 사용하면 안 됩니다.

```
기존
CM = Campaign

변경
CM = Customer Management
```

로그와 과거 문서의 의미가 달라집니다.

폐기코드는 상태만 종료하고 재사용하지 않습니다.

### 제87장 요약

```
업무코드는 시스템의 큰 업무경계를 나타낸다.

도메인은 업무 안의 책임영역을 나타낸다.

업무세구분코드는 화면과 관리대장의 세부 분류에 사용한다.

업무코드는 WAR·Package·ServiceId·화면·DB에서
일관되게 사용해야 한다.
```

## 제88장. 모듈·WAR·Context Path·패키지 표준

### 88.1 모듈 이름

Gradle Module은 역할을 알 수 있어야 합니다.

공통 Framework:

```
tcf-util
tcf-core
tcf-web
tcf-cache
tcf-eai
tcf-gateway
tcf-jwt
tcf-om
tcf-batch
tcf-ui
```

업무 서비스:

```
sv-service
ic-service
cm-service
pd-service
```

### 88.2 모듈명 형식

```
{업무코드 소문자}-{역할}
```

예:

```
sv-service
cm-service
```

공통 Framework:

```
tcf-{기능}
```

예:

```
tcf-core
tcf-web
tcf-cache
```

### 88.3 WAR 이름

권장:

```
sv-service.war
ic-service.war
cm-service.war
```

운영 배포 시 Version을 Artifact 이름에 포함할 수 있습니다.

```
sv-service-1.3.0.war
```

다만 Tomcat Context Path는 Version과 분리해야 합니다.

### 88.4 Context Path

권장:

```
/sv
/ic
/cm
/om
```

Artifact 파일명 변경으로 Context가 바뀌지 않도록 명시 설정합니다.

### 88.5 기본 Java Package

권장 구조:

```
com.nh.nsight.marketing.{업무코드}.{도메인}.{계층}
```

예:

```
com.nh.nsight.marketing.sv.customer.handler
com.nh.nsight.marketing.sv.customer.facade
com.nh.nsight.marketing.sv.customer.service
com.nh.nsight.marketing.sv.customer.rule
com.nh.nsight.marketing.sv.customer.dao
com.nh.nsight.marketing.sv.customer.mapper
com.nh.nsight.marketing.sv.customer.dto
```

### 88.6 표준 패키지 계층

| Package | 역할 |
| --- | --- |
| handler | ServiceId 분기 |
| facade | 유스케이스·트랜잭션 |
| service | 업무 흐름 |
| rule | 검증·계산 |
| dao | 데이터 접근 추상화 |
| mapper | MyBatis Interface |
| dto.request | 입력 전문 |
| dto.response | 출력 전문 |
| dto.command | 내부 변경 명령 |
| dto.data | DB·조회 데이터 |
| client | 다른 업무·외부 호출 |
| contract | 공개 Application Contract |
| event | Event 정의 |
| config | 업무 설정 |
| support | 제한된 업무 보조 기능 |

### 88.7 패키지 이름 원칙

```
소문자만 사용한다.

단수 명사를 우선한다.

약어를 남용하지 않는다.

기술과 업무를 혼합하지 않는다.

깊이는 필요한 수준으로 제한한다.
```

금지:

```
com.nh.nsight.marketing.sv.Customer.ServiceImpl
com.nh.nsight.marketing.sv.customer.util.common
```

### 88.8 impl 패키지

모든 Service에 무조건 Interface와 impl을 만들 필요는 없습니다.

다음 경우 Interface를 사용합니다.

- 공개 Contract
- 구현 교체 가능성
- 여러 구현체
- Framework 확장점
- 테스트 대역 필요성이 높음
단순 업무 Service 하나를 위해 다음 구조를 반복하면 복잡도만 증가할 수 있습니다.

```
CustomerService
CustomerServiceImpl
```

### 88.9 공통 패키지 남용

금지:

```
common
util
shared
base
```

패키지에 모든 기능을 넣습니다.

공통 기능은 소유 책임을 명확히 해야 합니다.

```
tcf-util
= 기술적 순수 Utility

tcf-core
= 거래 처리 공통

cc-service
= 업무 공통 서비스
```

### 88.10 Package 의존방향

```
Handler
→ Facade

Facade
→ Service·Contract

Service
→ Rule·DAO·Client

DAO
→ Mapper

Mapper
→ DB
```

금지:

```
Mapper → Service

Rule → DAO

DAO → Handler

Service → Controller
```

Architecture Test로 자동검증합니다.

### 제88장 요약

```
모듈과 WAR 이름은 업무와 실행 역할을 나타낸다.

패키지는 업무코드·도메인·계층 순서로 구성한다.

공통 패키지에 기능을 무분별하게 모으지 않는다.

패키지 의존방향도 자동검증해야 한다.
```

## 제89장. 화면 ID·화면그룹·이벤트 ID·메뉴 ID

### 89.1 화면 ID가 필요한 이유

화면 ID는 다음 영역을 연결합니다.

```
화면 설계서

UI Source

메뉴

권한

화면 이벤트

오류 문의

사용자 교육

운영 로그
```

### 89.2 권장 화면 ID

```
{업무코드}-{업무세구분코드}-{4자리 일련번호}
```

예:

```
SV-CUS-0001
CM-CAM-0001
OM-SVC-0001
```

### 89.3 화면 ID 구성

| 구성 | 예 | 의미 |
| --- | --- | --- |
| 업무코드 | SV | Single View |
| 업무세구분 | CUS | Customer |
| 일련번호 | 0001 | 화면 고유번호 |

화면 ID에 화면유형과 기능을 지나치게 많이 넣지 않습니다.

금지:

```
SV-CUS-SEARCH-DETAIL-MAIN-2026-0001
```

### 89.4 화면 일련번호

일련번호는 해당 업무와 업무세구분 범위에서 유일해야 합니다.

예:

```
SV-CUS-0001
고객 종합정보

SV-CUS-0002
고객 상세정보

SV-CUS-0003
고객 활동조회
```

삭제된 화면의 번호를 새 화면에 재사용하지 않습니다.

### 89.5 화면그룹코드

화면그룹은 여러 화면을 관리·검색·권한부여하기 위한 논리 묶음입니다.

예:

```
고객조회 화면그룹
SV-CUS-G01
```

소속 화면:

```
SV-CUS-0001
SV-CUS-0002
SV-CUS-0003
```

화면그룹은 화면 ID 자체를 대신하지 않습니다.

### 89.6 화면그룹이 필요한 이유

```
권한 일괄 부여

메뉴 구성

사용자 교육

화면 배포관리

업무별 현황

산출물 분류
```

화면그룹이 필요하지 않은 단순 시스템에서는 관리대장의 도메인 분류로 대체할 수 있습니다.

### 89.7 화면 이벤트 ID

한 화면에는 여러 이벤트가 존재합니다.

예:

```
조회 버튼 클릭

등록 버튼 클릭

저장 버튼 클릭

파일 다운로드

팝업 호출
```

권장 형식:

```
{화면 ID}-E{2자리 번호}
```

예:

```
SV-CUS-0001-E01
SV-CUS-0001-E02
SV-CUS-0001-E03
```

### 89.8 이벤트 관리대장

| 이벤트 ID | 이벤트명 | ServiceId | 처리유형 |
| --- | --- | --- | --- |
| SV-CUS-0001-E01 | 고객 요약조회 | SV.Customer.selectSummary | 조회 |
| SV-CUS-0001-E02 | 상품목록조회 | PD.CustomerProduct.selectList | 조회 |
| SV-CUS-0001-E03 | 상담이력조회 | CT.Contact.selectRecentList | 조회 |

한 화면이 여러 업무 ServiceId를 호출할 수 있습니다.

### 89.9 메뉴 ID와 화면 ID

메뉴는 사용자가 기능을 탐색하는 구조이고, 화면은 실제 UI 구성요소입니다.

```
하나의 메뉴
→ 하나의 화면

하나의 메뉴
→ 여러 화면

여러 메뉴
→ 하나의 공통 화면
```

따라서 메뉴 ID와 화면 ID는 분리합니다.

### 89.10 메뉴 ID 예

권장 개념:

```
M-{업무코드}-{일련번호}
```

예:

```
M-SV-0001
M-CM-0001
```

메뉴 계층은 별도 Parent Menu ID로 관리합니다.

| 메뉴 ID | 메뉴명 | 상위 메뉴 | 화면 ID |
| --- | --- | --- | --- |
| M-SV-0001 | 고객정보 | ROOT | 없음 |
| M-SV-0002 | 고객 종합조회 | M-SV-0001 | SV-CUS-0001 |

### 89.11 메뉴관리 책임

메뉴는 단말솔루션의 화면 표시 기능만으로 관리하지 않습니다.

업무공통 또는 통합 권한체계가 다음 정보를 소유해야 합니다.

```
메뉴 ID

화면 ID

메뉴 계층

사용권한

활성상태

노출순서

유효기간
```

단말솔루션은 이 기준정보를 이용해 메뉴를 표시합니다.

### 89.12 팝업과 공통 컴포넌트

독립 권한·호출·운영추적이 필요한 팝업은 별도 화면 ID를 부여할 수 있습니다.

단순 UI Component에는 화면 ID를 부여하지 않습니다.

```
주소검색 공통 팝업
→ 별도 화면 ID 가능

테이블 내부 날짜 선택기
→ 화면 ID 불필요
```

### 제89장 요약

```
화면 ID는 업무코드·업무세구분·일련번호로 구성한다.

이벤트 ID는 화면의 실제 사용자 행위를 구분한다.

메뉴 ID와 화면 ID는 목적이 다르므로 분리한다.

화면 이벤트와 ServiceId를 관리대장에서 연결한다.
```

## 제90장. ServiceId 설계 표준

### 90.1 ServiceId란 무엇인가요?

ServiceId는 TCF가 실행할 업무 기능을 식별하는 공식 식별자입니다.

```
POST /sv/online
```

Endpoint는 같아도 ServiceId에 따라 실행기능이 달라집니다.

```
SV.Customer.selectSummary

SV.Customer.selectDetail

SV.Customer.updateContact
```

### 90.2 권장 형식

```
{업무코드}.{도메인}.{행위}
```

예:

```
SV.Customer.selectSummary
CM.Campaign.create
CM.Campaign.update
OM.Service.suspend
```

### 90.3 업무코드

첫 번째 영역은 대문자 업무코드를 사용합니다.

```
SV
CM
OM
```

URL·Header·WAR와 일치해야 합니다.

### 90.4 도메인

두 번째 영역은 PascalCase 단수 명사를 사용합니다.

권장:

```
Customer
Campaign
CustomerProduct
TransactionLog
```

금지:

```
Customers
CustomerManagement
Common
Data
```

### 90.5 행위

세 번째 영역은 소문자로 시작하는 동사형을 사용합니다.

조회:

```
selectDetail
selectSummary
selectList
count
exists
```

등록·변경:

```
create
update
deactivate
approve
cancel
restore
```

파일·Batch:

```
requestExport
upload
download
start
stop
restart
```

### 90.6 CRUD 동사 기준

| 처리 | 권장 동사 | 설명 |
| --- | --- | --- |
| 단건 조회 | selectDetail | 한 건 상세 |
| 요약 조회 | selectSummary | 요약정보 |
| 목록 조회 | selectList | 여러 건 |
| 건수 조회 | count | 건수 |
| 존재 확인 | exists | Boolean |
| 등록 | create | 신규 업무 생성 |
| 변경 | update | 기존 업무 변경 |
| 비활성화 | deactivate | 논리 삭제 |
| 재활성화 | activate | 사용 복구 |
| 승인 | approve | 상태 승인 |
| 반려 | reject | 승인 거절 |
| 취소 | cancel | 업무 취소 |

### 90.7 get과 select

ServiceId에서는 조회 행위를 일관되게 표현하기 위해 select를 권장할 수 있습니다.

```
SV.Customer.selectSummary
```

Java 객체 Getter의 getName()과 업무 조회를 구분하기 쉽습니다.

프로젝트가 get을 공식 표준으로 정했다면 하나로 통일해야 합니다.

### 90.8 범용 ServiceId 금지

금지:

```
CM.Campaign.manage

CM.Campaign.process

CM.Campaign.execute
```

Request Body의 actionType으로 모든 기능을 분기합니다.

```
{
  "actionType": "CREATE"
}
```

문제:

- 권한 분리 어려움
- Timeout 분리 어려움
- 거래통제 분리 어려움
- 오류통계 왜곡
- 호출자 추적 어려움

### 90.9 하나의 ServiceId, 하나의 책임

```
CM.Campaign.create
= 캠페인 신규 등록

CM.Campaign.approve
= 캠페인 승인
```

등록과 승인을 하나의 ServiceId에서 동시에 처리하지 않습니다.

단, 하나의 유스케이스를 완성하기 위한 내부 여러 DB 작업은 하나의 ServiceId에 포함될 수 있습니다.

### 90.10 ServiceId 변경

ServiceId는 화면·연계·OM·권한과 연결됩니다.

따라서 이름 변경은 API 계약 변경입니다.

```
기존
SV.Customer.selectSummary

신규
SV.Client.getOverview
```

단순 리팩터링으로 볼 수 없습니다.

필요 절차:

```
호출자 영향분석

병행지원

DEPRECATED

호출량 확인

폐기
```

### 90.11 ServiceId Registry

| 항목 | 설명 |
| --- | --- |
| ServiceId | 공식 식별자 |
| 표시명 | 한글 서비스명 |
| 업무코드 | 대상 업무 |
| 도메인 | 업무 도메인 |
| 처리유형 | 조회·등록 등 |
| 거래코드 | 운영 분류 |
| Handler | 실행 Handler |
| Timeout | 제한시간 |
| 권한 | 필요 권한 |
| 상태 | 운영 상태 |
| Version | 계약 버전 |
| 담당자 | 업무 담당 |

### 제90장 요약

```
ServiceId는 업무코드·도메인·행위로 구성한다.

한 ServiceId에는 하나의 명확한 업무책임을 부여한다.

manage·process 같은 범용 행위를 사용하지 않는다.

ServiceId 변경은 API와 운영 계약 변경으로 관리한다.
```

## 제91장. 거래코드 설계 표준

### 91.1 거래코드란 무엇인가요?

ServiceId가 애플리케이션 기능을 식별한다면 거래코드는 운영·감사·통계 관점에서 거래 유형을 분류합니다.

```
ServiceId
SV.Customer.selectSummary

거래코드
SV-INQ-0001
```

### 91.2 권장 형식

```
{업무코드}-{처리유형}-{4자리 일련번호}
```

예:

```
SV-INQ-0001
CM-REG-0001
CM-UPD-0001
CM-APR-0001
```

### 91.3 처리유형 코드

| 처리유형 | 코드 | 설명 |
| --- | --- | --- |
| 조회 | INQ | Inquiry |
| 등록 | REG | Registration |
| 변경 | UPD | Update |
| 삭제 | DEL | Delete |
| 승인 | APR | Approval |
| 취소 | CAN | Cancel |
| 파일 | FIL | File |
| Batch | BAT | Batch |
| 연계 | EXT | External |
| 관리 | MGT | Management |

실제 처리유형 코드는 공식 거래코드 사전을 우선합니다.

### 91.4 ServiceId와 거래코드 관계

일반적으로 하나의 ServiceId는 하나의 대표 거래코드를 가집니다.

```
CM.Campaign.create
→ CM-REG-0001
```

동일 ServiceId가 채널별로 서로 다른 거래코드를 가져야 하는 경우는 운영·감사 목적을 명확히 해야 합니다.

### 91.5 거래코드가 필요한 이유

```
조회와 변경 통계 분리

거래통제

감사 분류

과금·사용량 통계

채널별 분석

장애 집계

운영 보고서
```

### 91.6 일련번호 관리

일련번호는 업무코드와 처리유형 내에서 유일하도록 관리할 수 있습니다.

예:

```
CM-INQ-0001
CM-INQ-0002
CM-REG-0001
```

삭제된 거래코드 번호를 재사용하지 않습니다.

### 91.7 거래코드와 DB 거래 구분

거래코드는 SQL 한 건을 의미하지 않습니다.

```
CM-REG-0001
→ Master INSERT
→ History INSERT
→ Outbox INSERT
```

여러 DB 작업이 하나의 업무 거래를 구성할 수 있습니다.

### 91.8 거래코드 변경

운영 통계와 감사가 과거부터 연결되어 있으므로 기존 코드 의미를 바꾸지 않습니다.

금지:

```
CM-INQ-0001
기존: 캠페인 상세조회

변경: 캠페인 목록조회
```

신규 코드를 발급합니다.

### 제91장 요약

```
ServiceId는 실행 기능을 식별하고,
거래코드는 운영·감사 유형을 분류한다.

거래코드는 업무코드·처리유형·일련번호로 구성한다.

기존 거래코드의 의미를 재사용하거나 변경하지 않는다.
```

## 제92장. Java 클래스·인터페이스·메서드·DTO 명명

### 92.1 클래스 이름 기본형

```
{업무 또는 도메인}{역할}
```

예:

```
SvCustomerHandler
SvCustomerFacade
SvCustomerService
SvCustomerRule
SvCustomerDao
SvCustomerMapper
```

동일 Package 안에서 업무 Prefix를 생략할 수도 있지만, 전체 검색과 운영 추적을 위해 포함하는 방식을 권장할 수 있습니다.

프로젝트 기준을 하나로 통일합니다.

### 92.2 계층별 접미사

| 계층 | 접미사 | 예 |
| --- | --- | --- |
| Handler | Handler | SvCustomerHandler |
| Facade | Facade | SvCustomerFacade |
| Service | Service | SvCustomerService |
| Rule | Rule | SvCustomerRule |
| DAO | Dao | SvCustomerDao |
| Mapper | Mapper | SvCustomerMapper |
| Client | Client | IcCustomerClient |
| Contract | Contract | CustomerInquiryContract |
| Controller | Controller | 공통 Endpoint 외 제한 |
| Configuration | Configuration | SvCacheConfiguration |
| Properties | Properties | SvClientProperties |

### 92.3 Handler 이름

Handler는 도메인 단위로 여러 ServiceId를 담당할 수 있습니다.

```
SvCustomerHandler
```

금지:

```
SelectCustomerSummaryHandler
CreateCustomerHandler
```

ServiceId마다 Handler를 무조건 하나씩 만들면 클래스 수가 지나치게 늘어날 수 있습니다.

단, 업무 복잡도와 책임 크기에 따라 기능별 분리도 가능합니다.

### 92.4 Service 이름

Service는 업무 흐름을 담당합니다.

권장:

```
CmCampaignService
CmCampaignApprovalService
```

금지:

```
CmCampaignServiceImpl2
CommonBusinessService
```

### 92.5 메서드 이름

메서드는 동사로 시작합니다.

```
selectCustomerSummary

selectCampaignList

createCampaign

updateCampaign

validateCampaignPeriod

calculateCustomerGrade

existsDuplicateCampaign
```

### 92.6 Boolean 메서드

권장:

```
isActive
hasAuthority
canApprove
existsCampaign
```

금지:

```
check
flag
result
```

### 92.7 Validation 메서드

```
validateCreateRequest

validateStatusTransition

validateDateRange

validateDownloadAuthority
```

검증 실패 시 예외를 발생시키는지 Boolean을 반환하는지 메서드 이름과 계약을 일관되게 정합니다.

### 92.8 DTO 종류

#### Request

외부 요청 전문입니다.

```
CampaignCreateRequest
CustomerSummaryRequest
```

#### Response

외부 응답 전문입니다.

```
CampaignCreateResponse
CustomerSummaryResponse
```

#### Command

내부 변경 명령입니다.

```
CampaignCreateCommand
CampaignUpdateCommand
```

#### Query

내부 조회조건입니다.

```
CampaignListQuery
CustomerSummaryQuery
```

#### Data

DB 조회·저장 데이터입니다.

```
CampaignDetailData
CustomerSummaryData
```

#### Result

내부 업무 처리 결과입니다.

```
CampaignApprovalResult
```

### 92.9 DTO 재사용 주의

하나의 DTO를 Request, DB Parameter, Response에 모두 사용하면 안 됩니다.

```
CampaignDto
```

하나로 모든 계층에서 사용합니다.

문제:

- 입력하지 않아야 할 필드 노출
- DB 컬럼 변경이 API에 영향
- 응답 개인정보 과다노출
- Validation 혼재

### 92.10 필드 이름

Java 필드는 camelCase를 사용합니다.

```
customerNo
campaignId
startDate
createdDtm
useYn
```

약어 전체를 대문자로 쓰지 않습니다.

권장:

```
customerId
apiUrl
sqlId
```

프로젝트가 URL, SQL을 특정 방식으로 정했다면 Formatter와 함께 통일합니다.

### 92.11 상수 이름

```
대문자 + 밑줄
```

예:

```
private static final String SERVICE_ID =
    "SV.Customer.selectSummary";

private static final int MAX_PAGE_SIZE = 100;
```

### 92.12 예외 클래스

```
BusinessException
SystemException
TransactionTimeoutException
ContractViolationException
```

오류 원인을 역할명으로 표현합니다.

금지:

```
CustomException
MyException
CommonException2
```

### 제92장 요약

```
클래스 이름에는 도메인과 계층 역할을 표현한다.

메서드는 동사로 시작하고 업무행위를 명확히 나타낸다.

Request·Response·Command·Query·Data를 구분하여
계층 간 데이터 계약을 분리한다.
```

## 제93장. DAO·Mapper·SQL ID 명명

### 93.1 DAO 이름

DAO는 도메인 또는 업무 저장소 관점으로 이름을 만듭니다.

권장:

```
CmCampaignDao
SvCustomerDao
```

DAO를 테이블 하나마다 무조건 만들 필요는 없습니다.

업무 도메인의 데이터 접근 책임을 기준으로 구성합니다.

### 93.2 Mapper 이름

Mapper는 SQL 실행을 담당합니다.

권장:

```
CmCampaignMapper
SvCustomerMapper
```

다음 경우 분리할 수 있습니다.

```
CmCampaignMapper
CmCampaignTargetMapper
CmCampaignHistoryMapper
```

한 Mapper가 지나치게 많은 테이블과 SQL을 가지면 도메인 책임을 나눕니다.

### 93.3 Mapper 메서드

조회:

```
selectCampaignDetail
selectCampaignList
countCampaignList
existsCampaign
```

변경:

```
insertCampaign
updateCampaign
updateCampaignStatus
deleteExpiredCampaign
```

### 93.4 SQL ID

Mapper XML의 SQL ID는 Java 메서드명과 동일하게 구성합니다.

```
CampaignDetailData selectCampaignDetail(
    CampaignDetailQuery query
);
<select id="selectCampaignDetail">
```

이름이 다르면 검색과 자동검증이 어려워집니다.

### 93.5 SQL 표준 식별자

운영 추적을 위해 별도의 SQL ID를 사용할 수 있습니다.

권장 개념:

```
{업무코드}-{도메인약어}-{SQL유형}-{3자리 또는 4자리 번호}
```

예:

```
SV-CUST-SEL-001
CM-CAM-INS-001
CM-CAM-UPD-001
```

### 93.6 SQL 유형 코드

| 유형 | 코드 |
| --- | --- |
| SELECT | SEL |
| INSERT | INS |
| UPDATE | UPD |
| DELETE | DEL |
| MERGE | MRG |
| PROCEDURE | PRC |

SQL ID 형식은 DB·운영 표준과 함께 확정합니다.

### 93.7 ResultMap 이름

```
{대상}ResultMap
```

예:

```
campaignDetailResultMap
customerSummaryResultMap
```

공통 ResultMap을 지나치게 재사용하면 컬럼 변경 영향이 커질 수 있습니다.

### 93.8 Parameter 이름

단일 값:

```
selectCampaignDetail(
    @Param("campaignId") String campaignId
);
```

복잡한 조건:

```
selectCampaignList(
    CampaignListQuery query
);
```

map, param1, arg0 같은 이름을 사용하지 않습니다.

### 93.9 Mapper Namespace

Java Interface의 전체 Package와 일치시킵니다.

```
<mapper namespace=
 "com.nh.nsight.marketing.cm.campaign.mapper.CmCampaignMapper">
```

CI에서 Interface와 XML Namespace를 자동검증합니다.

### 93.10 공통 SQL Fragment

```
campaignBaseColumns
campaignSearchConditions
```

의미를 나타내는 이름을 사용합니다.

금지:

```
sql1
base
commonWhere
```

공통 Fragment 변경은 여러 SQL에 영향을 주므로 테스트가 필요합니다.

### 93.11 DAO와 Mapper 책임

```
DAO
= 데이터 접근 의미와 예외 변환

Mapper
= SQL 실행
```

금지:

```
Service
→ Mapper 직접 호출

Handler
→ DAO 직접 호출
```

### 제93장 요약

```
DAO와 Mapper는 도메인 책임을 기준으로 구성한다.

Mapper 메서드와 XML SQL ID는 동일하게 유지한다.

운영 추적용 SQL ID를 ServiceId와 연결한다.

다른 업무 Mapper를 직접 참조하지 않는다.
```

## 제94장. DB 테이블·컬럼·인덱스·제약조건 표준

### 94.1 DB 객체 이름의 목적

DB 객체 이름은 다음 정보를 전달해야 합니다.

```
어느 업무가 소유하는가?

어떤 데이터를 저장하는가?

Master인가, History인가?

임시 데이터인가?

운영·연계 데이터인가?
```

### 94.2 테이블 이름 기본형

권장:

```
{업무코드}_{업무객체}_{유형}
```

예:

```
CM_CAMPAIGN_MASTER
CM_CAMPAIGN_HISTORY
CM_CAMPAIGN_TARGET
SV_CUSTOMER_SUMMARY
OM_SERVICE_CATALOG
OM_TRANSACTION_LOG
```

DB 객체명 길이 제한은 사용하는 DB 버전과 운영도구 호환성을 확인합니다.

### 94.3 테이블 유형 접미사

| 유형 | 접미사 | 예 |
| --- | --- | --- |
| Master | _MASTER | CM_CAMPAIGN_MASTER |
| Detail | _DETAIL 또는 _DTL | 프로젝트 표준 선택 |
| History | _HISTORY 또는 _HIST | 하나로 통일 |
| Code | _CODE | CC_CHANNEL_CODE |
| Relation | _REL | 관계정보 |
| Log | _LOG | 거래·감사로그 |
| Temporary | _TMP | 임시 데이터 |
| Staging | _STG | 적재 중간영역 |
| Interface | _IF | 연계 송수신 |
| Queue | _QUEUE | 처리대기 |
| Outbox | _OUTBOX | Event 발행 |
| Archive | _ARCH | 장기보관 |

긴 형식과 약어 형식을 혼용하지 않습니다.

### 94.4 테이블명 금지 예

```
TB001

T_CUSTOMER

CUSTOMER_INFO

NEW_CAMPAIGN

TEMP_TABLE2
```

업무 소유와 데이터 의미가 불명확합니다.

### 94.5 컬럼 이름 기본 원칙

```
영문 대문자

단어 구분은 밑줄

표준 용어사전 사용

데이터 성격을 Suffix로 표현
```

예:

```
CUSTOMER_NO
CAMPAIGN_ID
CAMPAIGN_NM
START_DT
CREATE_DTM
USE_YN
VERSION_NO
```

### 94.6 컬럼 접미사

| 의미 | 접미사 | 예 |
| --- | --- | --- |
| 식별자 | _ID | CAMPAIGN_ID |
| 번호 | _NO | CUSTOMER_NO |
| 코드 | _CD | STATUS_CD |
| 이름 | _NM | CAMPAIGN_NM |
| 날짜 | _DT | START_DT |
| 일시 | _DTM | CREATE_DTM |
| 금액 | _AMT | LIMIT_AMT |
| 수량 | _QTY | TARGET_QTY |
| 건수 | _CNT | CUSTOMER_CNT |
| 여부 | _YN | USE_YN |
| 순번 | _SEQ | DISPLAY_SEQ |
| 비율 | _RATE | RESPONSE_RATE |
| 버전 | _VER 또는 _VERSION_NO | 프로젝트 표준 선택 |

### 94.7 ID와 NO 구분

```
ID
= 시스템 내부의 유일 식별자

NO
= 업무에서 관리하는 번호
```

예:

```
CAMPAIGN_ID
= 내부 식별값

CUSTOMER_NO
= 업무 고객번호
```

프로젝트에서 둘의 의미를 명확히 정의해야 합니다.

### 94.8 코드와 상태

상태값은 자유문자열보다 코드로 관리합니다.

```
CAMPAIGN_STATUS_CD
```

금지:

```
STATUS
```

상태 대상과 코드 여부가 불명확합니다.

### 94.9 등록·수정 감사 컬럼

권장 공통 컬럼 예:

```
CREATE_USER_ID
CREATE_DTM
UPDATE_USER_ID
UPDATE_DTM
```

필요 시:

```
CREATE_PROGRAM_ID
UPDATE_PROGRAM_ID
```

ServiceId나 거래코드를 저장할 수도 있지만 개인정보·길이·운영 목적을 검토합니다.

### 94.10 논리 삭제

```
USE_YN
DELETE_YN
ACTIVE_YN
STATUS_CD
```

중 하나를 업무 의미에 맞게 사용합니다.

동일 테이블에서 USE_YN, DELETE_YN, STATUS_CD를 중복 사용하면 상태 의미가 충돌할 수 있습니다.

### 94.11 Primary Key 이름

```
PK_{테이블명}
```

예:

```
PK_CM_CAMPAIGN_MASTER
```

DB 길이 제한이 있다면 공식 축약규칙을 적용합니다.

### 94.12 Foreign Key 이름

```
FK_{자식테이블약어}_{부모테이블약어}_{순번}
```

예:

```
FK_CM_CAM_TGT_CM_CAM_01
```

또는 전체 테이블명을 사용하는 표준을 선택할 수 있습니다.

중요한 것은 자동 생성된 무의미한 이름을 피하는 것입니다.

### 94.13 Unique Constraint

```
UK_{테이블명}_{순번}
```

예:

```
UK_CM_CAMPAIGN_MASTER_01
```

설계서에는 Unique 대상 컬럼의 업무 의미를 기록합니다.

### 94.14 Index 이름

```
IX_{테이블명}_{순번}
```

예:

```
IX_CM_CAMPAIGN_MASTER_01
```

인덱스명에 모든 컬럼을 넣으면 길어질 수 있습니다.

인덱스 관리대장에서 컬럼과 목적을 별도로 관리합니다.

### 94.15 Sequence 이름

```
SEQ_{업무객체}
```

예:

```
SEQ_CM_CAMPAIGN
```

업무 식별자 생성방식이 UUID·DB Sequence·별도 채번인지 표준을 정합니다.

### 94.16 View 이름

```
VW_{업무코드}_{업무객체}
```

예:

```
VW_SV_CUSTOMER_SUMMARY
```

Materialized View는 별도 접두사를 정의할 수 있습니다.

```
MV_SV_CUSTOMER_SUMMARY
```

### 94.17 DB Comment

이름만으로 모든 의미를 표현할 수 없으므로 Table과 Column Comment를 작성합니다.

예:

```
CAMPAIGN_STATUS_CD
= 캠페인의 현재 업무 진행상태를 나타내는 코드
```

Comment에 “상태코드”만 반복하지 않습니다.

### 제94장 요약

```
DB 객체 이름에는 업무 소유와 데이터 역할을 표현한다.

컬럼은 표준 용어와 의미별 접미사를 사용한다.

PK·FK·UK·Index도 식별 가능한 이름으로 관리한다.

테이블과 컬럼에는 업무 의미가 담긴 Comment를 작성한다.
```

## 제95장. Batch·파일·Cache·환경설정·로그 명명

### 95.1 Batch Job 이름

권장:

```
{업무대상}{처리목적}Job
```

예:

```
CustomerGradeCalculationJob
CampaignTargetGenerationJob
ExpiredFileDeletionJob
```

금지:

```
Batch1
DailyJob
CommonJob
```

### 95.2 Batch Step 이름

```
validateCampaignConditionStep

generateCampaignTargetStep

writeCampaignResultStep
```

Job 안에서 역할이 명확해야 합니다.

### 95.3 Scheduler 이름

```
{Job명}Scheduler
```

예:

```
CustomerGradeCalculationScheduler
```

Scheduler가 업무 처리까지 담당하지 않습니다.

### 95.4 파일 ID

파일 이름과 내부 식별자를 분리합니다.

```
File ID
FILE202607170001

원본 파일명
customer_upload.xlsx

실제 저장명
UUID.bin
```

### 95.5 파일명

사용자에게 제공하는 파일:

```
{업무}_{내용}_{기준일}_{생성시각}.{확장자}
```

예:

```
SV_CUSTOMER_EXPORT_20260717_103000.csv
```

개인정보나 사용자 ID를 파일명에 직접 넣지 않습니다.

### 95.6 Cache 이름

```
{업무}.{데이터영역}
```

예:

```
sv.customerGrade
om.servicePolicy
cc.commonCode
```

Key 예:

```
PRD:OM:SERVICE_POLICY:SV.Customer.selectSummary
```

### 95.7 환경설정 Key

권장 구조:

```
tcf.{기능}.{세부기능}.{속성}
```

예:

```
tcf:
  transaction:
    default-timeout-ms: 3000
  clients:
    ic:
      connect-timeout-ms: 500
      read-timeout-ms: 1500
  cache:
    service-policy:
      ttl-seconds: 30
```

### 95.8 환경변수

대문자와 밑줄을 사용합니다.

```
TCF_TRANSACTION_DEFAULT_TIMEOUT_MS
IC_SERVICE_BASE_URL
JWT_JWKS_URI
```

Secret 값은 이름만 설정하고 실제 값은 Secret Store에서 주입합니다.

### 95.9 오류코드

권장 개념:

```
E-{업무 또는 영역}-{도메인 또는 유형}-{일련번호}
```

예:

```
E-COM-AUTH-0001
E-TCF-TIME-0001
E-CM-CAM-0011
```

오류코드도 명명규칙과 Registry 관리대상이 됩니다.

### 95.10 Log Event 이름

구조화 로그의 Event 이름:

```
TRANSACTION_START

TRANSACTION_END

AUTHENTICATION_FAIL

AUTHORIZATION_FAIL

CACHE_LOAD_FAIL

BATCH_JOB_FAILED

SERVICE_CONTROL_CHANGED
```

Event 이름은 대문자와 밑줄을 일관되게 사용합니다.

### 95.11 Metric 이름

예:

```
tcf_transaction_requests_total

tcf_transaction_duration_seconds

tcf_db_pool_pending

tcf_batch_job_failures_total
```

Metric 단위는 이름에 명확히 표현합니다.

```
seconds
bytes
total
```

### 제95장 요약

```
Batch·파일·Cache·설정·로그도
업무 코드와 처리목적이 드러나는 이름을 사용한다.

파일 표시명과 실제 저장명을 분리한다.

환경설정 Key는 계층형으로 구성하고
Metric에는 단위와 유형을 표현한다.
```

## 제96장. 통합 추적성 매트릭스와 자동검증

### 96.1 추적성 매트릭스란 무엇인가요?

화면부터 DB까지 모든 식별자를 한 행으로 연결한 관리대장입니다.

예:

| 항목 | 값 |
| --- | --- |
| 요구사항 ID | REQ-SV-0010 |
| 화면 ID | SV-CUS-0001 |
| 이벤트 ID | SV-CUS-0001-E01 |
| ServiceId | SV.Customer.selectSummary |
| 거래코드 | SV-INQ-0001 |
| Handler | SvCustomerHandler |
| Facade | SvCustomerFacade |
| Service | SvCustomerService.selectSummary |
| Rule | SvCustomerRule |
| DAO | SvCustomerDao |
| Mapper | SvCustomerMapper.selectCustomerSummary |
| SQL ID | SV-CUST-SEL-001 |
| 테이블 | SV_CUSTOMER_SUMMARY |
| 권한 | SV_CUSTOMER_VIEW |
| Timeout | 3000ms |
| 담당자 | SV 업무팀 |

### 96.2 추적성의 목적

```
요구사항 누락 확인

화면 이벤트 누락 확인

미사용 ServiceId 탐지

영향분석

장애추적

테스트 범위 확인

운영 인수

폐기 대상 확인
```

### 96.3 정방향 추적

```
요구사항
→ 화면
→ ServiceId
→ Source
→ SQL
→ DB
```

신규 요구가 코드와 DB까지 구현되었는지 확인합니다.

### 96.4 역방향 추적

```
DB 테이블
→ Mapper
→ Service
→ ServiceId
→ 화면
→ 요구사항
```

테이블 변경 시 어느 화면과 거래가 영향받는지 확인합니다.

### 96.5 화면 관리대장

권장 항목:

| 항목 | 설명 |
| --- | --- |
| 화면 ID | 공식 화면 식별자 |
| 화면명 | 사용자 표시명 |
| 업무코드 | 소유 업무 |
| 업무세구분 | 세부영역 |
| 화면유형 | Main·Popup |
| 메뉴 ID | 연결 메뉴 |
| 권한 | 접근권한 |
| 담당자 | 업무 책임 |
| 상태 | Active·Deprecated |
| Version | 화면 버전 |

### 96.6 ServiceId 관리대장

```
ServiceId

표시명

거래코드

Handler

WAR

Context Path

Timeout

권한

거래통제 상태

요청·응답 DTO

오류코드

호출 화면

호출 시스템
```

### 96.7 DB 객체 관리대장

```
Table Name

한글명

소유 업무

도메인

PK

주요 Index

보관기간

개인정보 여부

생성 ServiceId

조회 ServiceId

변경 ServiceId
```

### 96.8 자동검증 항목

#### 업무코드 정합성

```
Package 업무코드
= ServiceId Prefix
= Header businessCode
= WAR
```

#### ServiceId 형식

정규식 개념:

```
^[A-Z]{2}\.[A-Z][A-Za-z0-9]*\.[a-z][A-Za-z0-9]*$
```

#### 화면 ID 형식

```
^[A-Z]{2}-[A-Z]{3}-[0-9]{4}$
```

#### 화면 이벤트 ID

```
^[A-Z]{2}-[A-Z]{3}-[0-9]{4}-E[0-9]{2}$
```

#### 거래코드

```
^[A-Z]{2}-[A-Z]{3}-[0-9]{4}$
```

처리유형 코드는 허용 목록으로 추가 검증합니다.

### 96.9 중복검증

```
화면 ID 중복

이벤트 ID 중복

ServiceId 중복

거래코드 중복

오류코드 중복

SQL ID 중복

DB 객체명 중복
```

### 96.10 참조검증

```
화면 이벤트의 ServiceId가 Catalog에 존재하는가?

ServiceId를 처리하는 Handler가 존재하는가?

거래코드가 Registry에 존재하는가?

필요 권한이 권한대장에 존재하는가?

Mapper Interface와 XML이 일치하는가?

테이블이 DB 설계서에 존재하는가?
```

### 96.11 금지 의존성 검증

```
다른 업무 Mapper Import

Handler에서 DAO 호출

Rule에서 Mapper 호출

Service에서 Controller 호출

업무 코드에서 HttpSession 직접 사용

Service에서 URL 하드코딩
```

### 96.12 미사용 항목 검증

```
화면에서 호출하지 않는 ServiceId

Handler에 등록되지 않은 ServiceId

사용되지 않는 거래코드

호출 없는 구 ServiceId

SQL에서 사용하지 않는 테이블

배포되지 않는 화면
```

미사용이라고 즉시 삭제하지 않고 Batch·외부연계·운영호출 여부를 확인합니다.

### 96.13 Architecture Gate

명명 관련 Gate 예:

| Gate | 기준 |
| --- | --- |
| 업무코드 | 공식 대장에 존재 |
| Package | 표준경로 준수 |
| 화면 ID | 형식·중복 정상 |
| 이벤트 ID | 화면과 연결 |
| ServiceId | 형식·Catalog 등록 |
| 거래코드 | 처리유형 정상 |
| Class | 계층 Suffix 준수 |
| Mapper | Interface·XML 일치 |
| SQL ID | 형식·중복 정상 |
| DB 객체 | 업무 Prefix 준수 |
| 오류코드 | Registry 등록 |
| 추적성 | 요구사항부터 DB까지 연결 |

### 제96장 요약

```
추적성 매트릭스는 화면부터 DB까지
모든 식별자를 한 행으로 연결한다.

명명규칙은 정규식·중복·참조·의존성 검증으로
CI에서 자동화한다.

문서와 실제 Source·OM·DB가 일치해야 한다.
```

## 3. 목표 아키텍처

```
[요구사항]
 REQ-SV-0010
      │
      ▼
[화면]
 SV-CUS-0001
      │
      ▼
[이벤트]
 SV-CUS-0001-E01
      │
      ▼
[ServiceId]
 SV.Customer.selectSummary
      │
      ├── 거래코드
      │   SV-INQ-0001
      │
      ├── 권한
      │   SV_CUSTOMER_VIEW
      │
      └── Timeout
          3000ms
      │
      ▼
[Handler]
 SvCustomerHandler
      │
      ▼
[Facade]
 SvCustomerFacade
      │
      ▼
[Service·Rule]
 SvCustomerService
 SvCustomerRule
      │
      ▼
[DAO·Mapper]
 SvCustomerDao
 SvCustomerMapper.selectCustomerSummary
      │
      ▼
[SQL]
 SV-CUST-SEL-001
      │
      ▼
[DB]
 SV_CUSTOMER_SUMMARY
      │
      ▼
[OM·Log·Metric]
 Service Catalog
 Transaction Log
 Performance Metric
```

## 4. 표준 형식 종합

### 4.1 주요 식별자 형식

| 대상 | 표준 형식 | 예 |
| --- | --- | --- |
| 업무코드 | {2자리 대문자} | SV |
| 업무세구분 | {3자리 대문자} | CUS |
| 화면 ID | {업무}-{세구분}-{4자리} | SV-CUS-0001 |
| 이벤트 ID | {화면ID}-E{2자리} | SV-CUS-0001-E01 |
| ServiceId | {업무}.{도메인}.{행위} | SV.Customer.selectSummary |
| 거래코드 | {업무}-{처리유형}-{4자리} | SV-INQ-0001 |
| 권한코드 | {업무}_{도메인}_{행위} | SV_CUSTOMER_VIEW |
| SQL ID | {업무}-{도메인}-{유형}-{번호} | SV-CUST-SEL-001 |
| WAR | {업무소문자}-service | sv-service |
| Context | /{업무소문자} | /sv |
| 테이블 | {업무}_{객체}_{유형} | SV_CUSTOMER_SUMMARY |
| 오류코드 | E-{영역}-{유형}-{번호} | E-SV-CUST-0001 |

### 4.2 계층 클래스 형식

| 계층 | 형식 | 예 |
| --- | --- | --- |
| Handler | {업무}{도메인}Handler | SvCustomerHandler |
| Facade | {업무}{도메인}Facade | SvCustomerFacade |
| Service | {업무}{도메인}Service | SvCustomerService |
| Rule | {업무}{도메인}Rule | SvCustomerRule |
| DAO | {업무}{도메인}Dao | SvCustomerDao |
| Mapper | {업무}{도메인}Mapper | SvCustomerMapper |
| Client | {대상업무}{도메인}Client | IcCustomerClient |
| Request | {기능}Request | CustomerSummaryRequest |
| Response | {기능}Response | CustomerSummaryResponse |
| Command | {기능}Command | CampaignCreateCommand |
| Query | {기능}Query | CampaignListQuery |
| Data | {기능}Data | CustomerSummaryData |

## 5. 구성요소 및 속성

| 구성요소 | 주요 속성 |
| --- | --- |
| Business Code Registry | 코드·업무명·담당자 |
| Domain Registry | 도메인·정의·소유업무 |
| Screen Registry | 화면·메뉴·권한 |
| Event Registry | 화면 이벤트·ServiceId |
| Service Catalog | ServiceId·거래코드 |
| Program Registry | 클래스·메서드·Package |
| SQL Registry | Mapper·SQL ID |
| DB Dictionary | 테이블·컬럼·도메인 |
| Error Registry | 오류코드·메시지 |
| Batch Registry | Job·Step·Schedule |
| Trace Matrix | 전체 식별자 연결 |
| Architecture Gate | 자동검증 규칙 |

## 6. 책임 경계와 RACI

| 활동 | EA·AA | 업무팀 | UI | DEV | DA | DBA | FW | QA | OM |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 업무코드 | A | R/C | I | I | C | I | C | I | C |
| 도메인 정의 | A | R | C | C | C | I | C | I | I |
| 화면 ID | C | A/C | R | C | I | I | I | C | C |
| 이벤트 ID | C | C | A/R | R/C | I | I | C | C | I |
| ServiceId | A | C | C | R | I | I | C | C | C |
| 거래코드 | A/C | C | I | R | I | I | C | C | R/C |
| 패키지·클래스 | A/C | I | I | R | I | I | C | C | I |
| DB 객체 | C | C | I | C | A | R | I | C | I |
| SQL ID | C | I | I | R | C | C | C | C | I |
| 추적성 대장 | A | C | C | R | C | I | C | C | C |
| 자동 Gate | A | I | I | C | C | I | R | R/C | I |
| 변경·폐기 | A | C | C | R | C | C | C | C | R/C |

```
R = 수행
A = 최종 책임
C = 협의
I = 공유
```

## 7. 정상 처리 흐름

```
1. 요구사항 ID 발급

2. 소유 업무코드 확정

3. 도메인과 업무세구분 확정

4. 화면 ID 발급

5. 화면 이벤트 ID 발급

6. ServiceId 정의

7. 거래코드와 권한 연결

8. Handler·Facade·Service 이름 정의

9. Request·Response DTO 정의

10. DAO·Mapper·SQL ID 정의

11. DB 객체와 컬럼 정의

12. OM Service Catalog 등록

13. 추적성 매트릭스 갱신

14. CI 명명·참조검증

15. 테스트·배포·운영 인수
```

## 8. 오류·충돌·변경 흐름

### 8.1 화면 ID 중복

```
신규 화면 등록
→ 동일 화면 ID 존재
→ 발급 차단
→ 다음 일련번호 부여
```

### 8.2 ServiceId 중복

```
다른 Handler가 동일 ServiceId 등록
→ 애플리케이션 기동 실패
→ CI 중복검증 실패
```

### 8.3 업무코드 불일치

```
Package = sv
ServiceId = CM.Campaign.create
→ Architecture Gate 실패
```

### 8.4 Mapper 불일치

```
Java Method
selectCustomerSummary

XML ID
getCustomerSummary
→ Build·통합 테스트 실패
```

### 8.5 DB 객체명 충돌

```
동일 Schema에 같은 Table 이름 존재
→ 신규 생성 차단
→ 소유 업무와 목적 재확인
```

### 8.6 명칭 변경

```
변경 요청
→ 호출자·문서·OM·로그 영향분석
→ 신규 식별자 발급 여부 결정
→ 병행 운영
→ 기존 DEPRECATED
→ 폐기
```

## 9. 정상 예시

### 9.1 고객 요약조회

```
요구사항
REQ-SV-0010

화면
SV-CUS-0001

이벤트
SV-CUS-0001-E01

ServiceId
SV.Customer.selectSummary

거래코드
SV-INQ-0001

권한
SV_CUSTOMER_VIEW

WAR
sv-service

Package
com.nh.nsight.marketing.sv.customer

Handler
SvCustomerHandler

Facade
SvCustomerFacade

Service
SvCustomerService.selectSummary

Mapper
SvCustomerMapper.selectCustomerSummary

SQL ID
SV-CUST-SEL-001

테이블
SV_CUSTOMER_SUMMARY
```

### 9.2 캠페인 등록

```
화면
CM-CAM-0001

이벤트
CM-CAM-0001-E02

ServiceId
CM.Campaign.create

거래코드
CM-REG-0001

권한
CM_CAMPAIGN_CREATE

Handler
CmCampaignHandler

Facade
CmCampaignFacade

Service
CmCampaignService.create

Mapper
CmCampaignMapper.insertCampaign

SQL ID
CM-CAM-INS-001

테이블
CM_CAMPAIGN_MASTER
CM_CAMPAIGN_HISTORY
```

## 10. 금지 예시

### 10.1 의미 없는 화면 ID

```
SCREEN01
PAGE001
```

### 10.2 범용 ServiceId

```
SV.Common.process
CM.Campaign.manage
```

### 10.3 테이블 번호만 사용

```
TB001
TB002
```

### 10.4 다른 업무 Package 혼입

```
com.nh.nsight.marketing.sv
→ CmCampaignMapper 포함
```

### 10.5 하나의 DTO 전체 재사용

```
CampaignDto
→ 화면 요청
→ DB 저장
→ 화면 응답
```

### 10.6 개인 약어 사용

```
Customer
→ 개인이 CST, CSM, CUS를 혼용
```

### 10.7 폐기 ID 재사용

기존 화면번호와 거래코드를 다른 기능에 다시 부여합니다.

## 11. 연계 규칙

```
화면 관리대장
→ 이벤트 ID

이벤트 관리대장
→ ServiceId

Service Catalog
→ 거래코드·Handler·권한·Timeout

Program 설계서
→ Class·Method

SQL 설계서
→ Mapper·SQL ID

DB 설계서
→ Table·Column

OM
→ 운영 상태·통계·감사
```

식별자 간 연결은 문자열 추정이 아니라 공식 Registry와 추적성 대장으로 관리합니다.

## 12. 데이터 및 상태관리

### 12.1 식별자 상태

```
DRAFT
→ APPROVED
→ ACTIVE
→ DEPRECATED
→ DISABLED
→ RETIRED
```

### 12.2 번호 발급

중앙 관리대장 또는 발급 시스템을 사용합니다.

```
요청자 임의 발급 금지

중복검사

발급사유

발급일시

담당자

상태
```

### 12.3 이력관리

이름이 변경되어도 과거 이력을 유지합니다.

| 항목 | 내용 |
| --- | --- |
| 이전 이름 | 구 식별자 |
| 신규 이름 | 신 식별자 |
| 변경일 | 적용일 |
| 변경사유 | 업무 변경 |
| 영향대상 | 화면·연계 |
| 폐기일 | 구 식별자 종료 |

## 13. 성능·용량·확장성

명명규칙 자체가 직접 성능을 높이지는 않지만 다음 운영효율에 영향을 줍니다.

```
ServiceId별 Metric 집계

SQL별 성능분석

WAR별 장애분석

Cache Key 충돌 방지

Log 검색속도

DB 객체 관리

자동 영향분석
```

주의사항:

- 지나치게 긴 식별자
- Metric Label 폭증
- DB 객체 길이 제한
- 파일 경로 길이
- 로그 중복 저장
- Registry Index 설계

## 14. 보안·개인정보·감사

```
화면 ID와 ServiceId에 개인정보를 넣지 않는다.

파일명과 Cache Key에 고객번호 원문 사용을 최소화한다.

권한코드는 기능 의미를 명확히 표현한다.

운영자용 ServiceId는 일반 업무와 분리한다.

식별자 발급·변경·폐기를 감사한다.

DB 객체와 컬럼의 개인정보 분류를 관리한다.

오류코드에 내부 서버·계정정보를 노출하지 않는다.
```

## 15. 운영·모니터링·장애 대응

운영자는 다음 식별자로 검색할 수 있어야 합니다.

```
화면 ID

이벤트 ID

ServiceId

거래코드

TraceId

WAR

Mapper ID

SQL ID

Table

오류코드
```

장애 조회 흐름:

```
사용자 문의 화면
→ 화면 ID 확인
→ 이벤트 ID 확인
→ ServiceId 검색
→ 거래로그 확인
→ Mapper·SQL 확인
→ DB 객체 확인
```

## 16. 자동검증 및 품질 Gate

| Gate | 검증 기준 |
| --- | --- |
| 업무코드 | 공식 Registry 존재 |
| Package | 업무코드·도메인·계층 준수 |
| 화면 ID | 정규식·중복 |
| 이벤트 ID | 화면 존재·순번 |
| ServiceId | 형식·중복·Handler |
| 거래코드 | 유형·중복 |
| Class | 표준 Suffix |
| Method | 동사형·금지어 |
| DTO | 역할별 Suffix |
| Mapper | Interface·XML 일치 |
| SQL ID | 형식·중복 |
| DB 객체 | Prefix·금지어 |
| 컬럼 | 표준 Domain·Suffix |
| 추적성 | 화면부터 DB 연결 |
| 폐기 | 호출자 없는지 확인 |

## 17. 테스트 시나리오

| ID | 시나리오 | 기대 결과 |
| --- | --- | --- |
| NAM-001 | 정상 화면 ID | 등록 성공 |
| NAM-002 | 화면 ID 중복 | 등록 차단 |
| NAM-003 | 잘못된 업무코드 | 검증 실패 |
| NAM-004 | 이벤트 ID의 화면 없음 | 검증 실패 |
| NAM-005 | 정상 ServiceId | Catalog 등록 |
| NAM-006 | ServiceId 중복 | Build 실패 |
| NAM-007 | ServiceId 업무 불일치 | Gate 실패 |
| NAM-008 | 범용 행위 process | 리뷰·Gate 실패 |
| NAM-009 | 거래코드 중복 | 등록 차단 |
| NAM-010 | 처리유형 코드 오류 | 검증 실패 |
| NAM-011 | Handler에서 Mapper 참조 | Architecture 실패 |
| NAM-012 | Rule에서 DAO 참조 | Architecture 실패 |
| NAM-013 | Mapper XML ID 불일치 | 통합 테스트 실패 |
| NAM-014 | Namespace 불일치 | 기동·Build 실패 |
| NAM-015 | 다른 업무 Mapper Import | Gate 실패 |
| NAM-016 | DB 테이블 Prefix 누락 | 설계 검증 실패 |
| NAM-017 | 예약어 컬럼 | DB Gate 실패 |
| NAM-018 | 표준 약어 미등록 | 사전 검증 실패 |
| NAM-019 | 폐기 ID 재사용 | 발급 차단 |
| NAM-020 | 추적성 누락 | Release Gate 실패 |
| NAM-021 | ServiceId OM 미등록 | 배포 차단 |
| NAM-022 | 권한코드 미등록 | 검증 실패 |
| NAM-023 | SQL ID 중복 | Build 실패 |
| NAM-024 | DB Comment 누락 | DA Gate 실패 |
| NAM-025 | Metric에 TraceId Label | 관측성 Gate 실패 |

## 18. 제11부 체크리스트

### 18.1 업무코드·도메인

| 점검 항목 | 확인 |
| --- | --- |
| 공식 업무코드를 사용했는가? | □ |
| 업무코드의 소유자가 명확한가? | □ |
| 도메인이 단수 명사형인가? | □ |
| 업무세구분코드가 등록되었는가? | □ |
| 동일 개념에 같은 영문명을 사용하는가? | □ |
| 개인 약어를 사용하지 않는가? | □ |

### 18.2 모듈·패키지

| 점검 항목 | 확인 |
| --- | --- |
| 모듈명이 역할을 표현하는가? | □ |
| WAR와 Context가 업무코드와 일치하는가? | □ |
| Package가 업무·도메인·계층 순서인가? | □ |
| 대문자 Package가 없는가? | □ |
| 공통 Package를 남용하지 않는가? | □ |
| 계층 의존방향을 지키는가? | □ |

### 18.3 화면·메뉴

| 점검 항목 | 확인 |
| --- | --- |
| 화면 ID가 표준 형식인가? | □ |
| 업무세구분이 올바른가? | □ |
| 화면번호가 중복되지 않는가? | □ |
| 이벤트 ID가 발급되었는가? | □ |
| 이벤트와 ServiceId가 연결되는가? | □ |
| 메뉴 ID와 화면 ID가 분리되는가? | □ |
| 폐기 화면번호를 재사용하지 않는가? | □ |

### 18.4 ServiceId·거래코드

| 점검 항목 | 확인 |
| --- | --- |
| ServiceId가 업무·도메인·행위 구조인가? | □ |
| 행위가 명확한 동사형인가? | □ |
| 범용 manage, process를 피했는가? | □ |
| 하나의 책임만 수행하는가? | □ |
| 거래코드가 연결되었는가? | □ |
| 처리유형 코드가 정확한가? | □ |
| OM Catalog에 등록되었는가? | □ |
| Timeout과 권한이 연결되었는가? | □ |

### 18.5 Java·DTO

| 점검 항목 | 확인 |
| --- | --- |
| 계층별 Class Suffix가 정확한가? | □ |
| 메서드가 동사로 시작하는가? | □ |
| Boolean 메서드 의미가 분명한가? | □ |
| Request와 Response가 분리되는가? | □ |
| DB Data DTO가 API에 노출되지 않는가? | □ |
| Dto, Data, Info를 모호하게 쓰지 않는가? | □ |
| 숫자·New·Final 접미사가 없는가? | □ |

### 18.6 Mapper·SQL

| 점검 항목 | 확인 |
| --- | --- |
| Mapper가 도메인 책임 기준인가? | □ |
| Java Method와 XML ID가 같은가? | □ |
| Namespace가 Interface와 일치하는가? | □ |
| SQL ID가 등록되었는가? | □ |
| ResultMap 이름이 의미를 표현하는가? | □ |
| param1, map 사용을 피했는가? | □ |
| 다른 업무 Mapper를 참조하지 않는가? | □ |

### 18.7 DB 객체

| 점검 항목 | 확인 |
| --- | --- |
| 테이블에 업무 Prefix가 있는가? | □ |
| 객체와 유형이 표현되는가? | □ |
| 컬럼이 표준용어를 사용하는가? | □ |
| ID·NO·CD·NM 의미가 구분되는가? | □ |
| 날짜와 일시가 구분되는가? | □ |
| PK·FK·UK·Index 이름이 표준인가? | □ |
| Table·Column Comment가 있는가? | □ |
| 개인정보 분류가 등록되었는가? | □ |

### 18.8 추적성·자동검증

| 점검 항목 | 확인 |
| --- | --- |
| 요구사항부터 화면이 연결되는가? | □ |
| 화면 이벤트부터 ServiceId가 연결되는가? | □ |
| ServiceId부터 Program이 연결되는가? | □ |
| Program부터 SQL·DB가 연결되는가? | □ |
| 중복 식별자를 자동검증하는가? | □ |
| 명명 정규식을 CI에서 검사하는가? | □ |
| 미사용·폐기 대상을 확인하는가? | □ |
| 문서와 실제 Source가 일치하는가? | □ |

## 19. 변경·호환성·폐기 관리

### 19.1 식별자 변경 원칙

식별자는 문서의 제목이 아니라 시스템 간 계약입니다.

다음 식별자는 단순히 이름을 바꾸면 안 됩니다.

```
화면 ID

ServiceId

거래코드

오류코드

DB 객체명

권한코드
```

### 19.2 표시명 변경

사용자 표시명은 비교적 쉽게 변경할 수 있습니다.

```
화면 ID
SV-CUS-0001 유지

화면명
고객 종합조회
→ 고객 통합정보 조회
```

식별자는 그대로 유지할 수 있습니다.

### 19.3 ServiceId 변경 절차

```
신규 ServiceId 등록

구·신 ServiceId 병행

호출자 전환

구 ServiceId DEPRECATED

호출량 0 확인

DISABLED

코드·OM·문서 제거
```

### 19.4 DB 객체 Rename

DB Table·Column Rename은 모든 SQL과 연계에 영향을 줍니다.

권장 단계:

```
신규 객체·컬럼 추가

구·신 병행

데이터 이관

신규 코드 전환

구 객체 사용중지

최종 제거
```

### 19.5 약어 변경

표준 약어를 변경하면 DB와 기존 Source에 광범위한 영향이 발생합니다.

기존 약어는 유지하고 신규 개발부터 개선할지, 전체 이관할지 Architecture Decision이 필요합니다.

### 19.6 폐기 상태

```
ACTIVE
→ DEPRECATED
→ DISABLED
→ RETIRED
```

DEPRECATED 상태에서는 신규 사용을 차단하고 기존 호출은 제한적으로 허용할 수 있습니다.

### 19.7 번호 재사용 금지

폐기된 화면번호, 거래코드, 오류코드, SQL ID를 다른 의미로 재사용하지 않습니다.

과거 로그와 감사정보의 의미를 보호하기 위해서입니다.

## 20. 시사점

### 20.1 핵심 아키텍처 판단

제11부의 핵심은 다음과 같습니다.

```
이름을 통일한다
= 보기 좋게 만든다
```

가 아닙니다.

```
이름을 통일한다
= 요구사항·화면·Source·SQL·DB·운영정보를
  하나의 업무 의미로 연결한다
```

입니다.

### 20.2 주요 위험

| 위험 | 영향 |
| --- | --- |
| 업무코드 혼용 | Route·소유권 혼란 |
| 화면 ID 임의 발급 | 중복·추적 실패 |
| 범용 ServiceId | 권한·통제 불가 |
| 거래코드 의미 변경 | 운영통계 왜곡 |
| 공통 Package 남용 | 책임경계 붕괴 |
| DTO 전체 재사용 | 계층 결합·정보노출 |
| Mapper와 XML 불일치 | Runtime 오류 |
| 다른 업무 DB 직접 접근 | 도메인 소유권 침해 |
| 개인 약어 사용 | 용어 불일치 |
| DB 객체 무의미한 이름 | 운영·튜닝 어려움 |
| 식별자 재사용 | 과거 로그 의미 훼손 |
| 추적성 대장 미갱신 | 영향분석 실패 |
| 문서만 존재하는 규칙 | 실제 코드 위반 지속 |

### 20.3 우선 보완 과제

```
1. 업무코드와 도메인 Registry 확정

2. 표준 용어·약어사전 작성

3. 화면 ID·이벤트 ID 발급대장 구축

4. ServiceId·거래코드 Catalog 통합

5. Package·Class·DTO 표준 적용

6. Mapper·SQL ID 정합성 검사

7. DB 객체·컬럼 Domain 표준화

8. 화면–ServiceId–Program–SQL 추적성 구축

9. CI 명명규칙 Gate 적용

10. 기존 비표준 식별자 현황과 폐기계획 수립
```

### 20.4 중장기 발전 방향

```
수동 Excel 관리대장
→ 통합 Registry

문서 기반 명명검토
→ CI 자동검증

화면별 수동 영향분석
→ 추적성 그래프

ServiceId 수동 등록
→ Source Scan 자동등록 후보

DB 객체 수동 비교
→ Schema 자동검증

수동 폐기조사
→ 호출량 기반 폐기 추천
```

자동등록은 잘못된 Source 정보가 운영 기준으로 바로 반영되지 않도록 승인절차를 유지해야 합니다.

## 21. 마무리말

제11부에서 가장 중요하게 기억해야 할 내용은 다음과 같습니다.

```
업무코드
= 어느 업무인가

도메인
= 어느 책임영역인가

화면 ID
= 어느 화면인가

이벤트 ID
= 화면에서 어떤 행동인가

ServiceId
= 어떤 업무기능을 실행하는가

거래코드
= 운영상 어떤 거래유형인가

Class·Method
= 코드가 어떤 역할을 하는가

SQL ID
= 어느 SQL인가

DB 객체
= 어느 업무가 어떤 데이터를 소유하는가
```

NSIGHT의 권장 추적구조는 다음과 같습니다.

```
요구사항
→ 화면 ID
→ 이벤트 ID
→ ServiceId
→ 거래코드
→ Handler
→ Facade
→ Service
→ Rule
→ DAO
→ Mapper
→ SQL ID
→ DB 객체
→ OM·로그·Metric
```

초보 개발자가 새로운 기능의 이름을 정하기 전에 마지막으로 확인해야 할 질문은 다음과 같습니다.

```
이 기능은 어느 업무가 소유하는가?

어느 도메인에 속하는가?

공식 업무코드와 약어를 사용했는가?

화면 ID가 관리대장에서 발급되었는가?

화면 이벤트가 ServiceId와 연결되는가?

ServiceId만 보고 업무와 행위를 알 수 있는가?

거래코드가 처리유형과 일치하는가?

Class 이름에서 계층 역할을 알 수 있는가?

Request·Response·DB Data가 분리되는가?

Mapper 메서드와 SQL ID가 일치하는가?

테이블과 컬럼이 업무 의미를 표현하는가?

화면부터 DB까지 역추적할 수 있는가?

명명규칙을 CI에서 자동검증하는가?

폐기된 식별자를 다시 사용하고 있지는 않은가?
```

이 질문에 답할 수 있다면 단순히 이름을 정하는 개발자를 넘어, 시스템 전체의 업무 의미와 책임경계를 연결하고 장기적인 변경과 운영을 가능하게 하는 개발자가 될 수 있습니다.

