<!-- source: ztcf-집필본/NSIGHT TCF 개발 입문서 - 제18부. 소스코드 읽기 기존 프로그램 분석·영향도 파악 실전_재구성확장판_v3.docx -->
<!-- tables: mammoth-html + placeholder table-to-md -->

나는 제18부. 소스코드 읽기·기존 프로그램 분석·영향도 파악 실전의 개념을 코드·설정·로그·산출물 중 어디에서 확인할 수 있는가? 문제가 발생했을 때 어느 경계부터 조사해야 하는가?

읽기 전 질문

① 장의 학습 목표를 먼저 읽습니다. ② 기존 설명과 예제를 따라갑니다. ③ 실무 적용 절차를 자신의 프로젝트에 대입합니다. ④ 완료 기준으로 이해 여부를 확인합니다.

권장 학습법

개발환경 구축부터 기존 코드 분석, 신규 CRUD, 코드 리뷰까지 혼자 수행하는 흐름을 완성합니다.

이 부의 학습 좌표

RESTRUCTURED & EXPANDED EDITION

6단계 · 독립 수행

**초보 IT 개발자를 위한 NSIGHT TCF 개발 입문서**

# **제18부. 소스코드 읽기·기존 프로그램 분석·영향도 파악 실전**

# **1\. 도입 전 안내말**

제17부에서는 개발환경을 구축하고 Git 저장소를 내려받아 Gradle로 빌드한 뒤, 업무 WAR를 실행하고 다음 흐름을 디버깅하는 방법을 배웠습니다.

HTTP 요청

→ OnlineTransactionController

→ TCF.process()

→ STF

→ TransactionDispatcher

→ Handler

→ Facade

→ Service

→ Rule

→ DAO

→ Mapper

→ DB

→ ETF

→ StandardResponse

이제 실제 프로젝트에서 다음과 같은 요청을 받았다고 가정해 보겠습니다.

“기존 고객조회 기능을 분석해 주세요.”

“이 화면에서 호출하는 프로그램을 찾아 주세요.”

“이 컬럼을 100자에서 200자로 변경하면 어디까지 영향이 있습니까?”

“이 ServiceId가 어떤 SQL을 실행하는지 알려 주세요.”

“사용자를 어디에서 검증하는지 확인해 주세요.”

“Timeout이 어느 계층에서 적용되는지 찾아 주세요.”

“현재 기능을 수정해도 다른 업무에 영향이 없는지 검토해 주세요.”

처음 보는 소스가 수백 개 또는 수천 개라면 초보 개발자는 다음과 같이 접근하기 쉽습니다.

Package Explorer를 위에서부터 차례대로 연다.

이름이 익숙한 Service Class부터 읽는다.

모든 Java 파일을 하나씩 확인한다.

검색 결과를 많이 열어 놓고 관계를 추측한다.

주석이 많은 Class를 공식 설계로 간주한다.

Controller에서 시작하지 못하면 Table명으로만 검색한다.

이 방식은 시간이 많이 걸리며 다음 문제를 발생시킵니다.

현재 사용하지 않는 코드까지 분석한다.

같은 이름의 오래된 Class를 잘못 선택한다.

ServiceId와 실제 Handler 연결을 놓친다.

업무 WAR 간 직접 호출과 HTTP 연계를 혼동한다.

Transaction과 Timeout 적용위치를 확인하지 못한다.

SQL은 찾았지만 어느 화면에서 호출하는지 알 수 없다.

수정 대상은 찾았지만 영향받는 테스트와 OM 설정을 놓친다.

NSIGHT TCF 소스를 분석할 때는 파일 수보다 **공식 식별자와 실행경로**가 중요합니다.

가장 안정적인 분석 시작점은 다음과 같습니다.

화면 ID

화면 이벤트 ID

ServiceId

거래코드

Handler

Mapper ID

SQL ID

테이블명

TraceId

오류코드

소스 분석은 크게 두 방향으로 수행합니다.

## **정방향 분석**

요구사항·화면

→ ServiceId

→ Handler

→ Facade

→ Service

→ Rule

→ DAO

→ Mapper

→ SQL

→ DB

## **역방향 분석**

DB 컬럼·SQL·오류로그

→ Mapper

→ DAO

→ Service

→ ServiceId

→ 화면·연계 호출자

→ 요구사항

제18부의 핵심 원칙은 다음과 같습니다.

모든 소스를 읽지 않는다.

업무 식별자에서 시작한다.

파일명보다 실제 Bean 등록과 호출관계를 확인한다.

정적 구조와 Runtime 실행결과를 함께 확인한다.

Source만 보지 않고 설정·DB·OM·테스트를 함께 본다.

변경 대상과 영향 대상을 구분한다.

추측과 확인된 사실을 구분한다.

분석 결과는 추적성 표와 영향도 목록으로 남긴다.

# **2\. 제18부 개요**

## **2.1 목적**

제18부의 목적은 처음 접하는 NSIGHT TCF 업무 소스를 짧은 시간 안에 구조적으로 파악하고, 특정 기능의 실행 흐름과 변경 영향범위를 정확하게 분석하도록 돕는 것입니다.

학습 후에는 다음 작업을 수행할 수 있어야 합니다.

1.  저장소와 멀티모듈 구조를 빠르게 파악한다.
2.  공통모듈과 업무모듈을 구분한다.
3.  화면 ID와 ServiceId를 기준으로 프로그램을 찾는다.
4.  Handler에서 Mapper까지 정방향으로 추적한다.
5.  SQL·테이블·오류코드에서 화면까지 역방향으로 추적한다.
6.  Spring Bean과 Runtime 구현체를 확인한다.
7.  Transaction·Timeout·인증·권한 적용위치를 찾는다.
8.  업무 간 연계와 직접 DB 접근을 구분한다.
9.  설정값과 환경별 동작 차이를 분석한다.
10.  Source·Mapper·DB·테스트·OM의 연결관계를 작성한다.
11.  변경 대상별 영향범위를 산정한다.
12.  변경 위험을 등급화한다.
13.  미사용 코드와 중복 구현을 식별한다.
14.  정적분석과 동적분석을 결합한다.
15.  AI 코딩도구를 이용하되 분석결과를 검증한다.
16.  분석보고서와 영향도 매트릭스를 작성한다.

## **2.2 적용범위**

| **영역** | **주요 내용** |
| --- | --- |
| 저장소 | Root·Module·Build 구조 |
| 업무모듈 | 업무 WAR·Package·도메인 |
| 공통모듈 | tcf-\* 공통기능 |
| 거래 | ServiceId·거래코드·Handler |
| 프로그램 | Facade·Service·Rule·DAO |
| 데이터 | Mapper·SQL·Table·Column |
| 연계 | 업무 Client·EAI·외부 API |
| 설정 | YAML·Bean·Profile·Feature Flag |
| 보안 | JWT·권한·데이터권한 |
| 안정성 | Transaction·Timeout·Idempotency |
| 운영 | Log·Trace·Metric·OM |
| 테스트 | Unit·Integration·E2E |
| 영향도 | 정방향·역방향 분석 |
| 품질 | 자동검증·Architecture Gate |

## **2.3 대상 독자**

-   처음 보는 업무 WAR를 분석해야 하는 개발자
-   기존 소스를 수정해야 하지만 영향범위를 모르는 개발자
-   ServiceId에서 SQL까지 추적해야 하는 개발자
-   테이블 변경이 화면에 미치는 영향을 조사하는 개발자
-   Legacy 코드와 신규 TCF 코드를 구분해야 하는 개발자
-   코드 리뷰와 영향도 검토를 수행하는 모듈 리더
-   장애 로그에서 원인 프로그램을 찾아야 하는 운영 개발자
-   AI 도구로 대규모 소스를 분석하려는 개발자

## **2.4 선행조건**

다음 내용을 이해하고 있어야 합니다.

NSIGHT 온라인 거래는 ServiceId로 기능을 식별한다.

Handler는 ServiceId와 Facade를 연결한다.

Facade는 유스케이스와 트랜잭션 경계를 담당한다.

Service는 업무 흐름을 담당한다.

Rule은 검증과 계산을 담당한다.

DAO와 Mapper는 DB 접근을 담당한다.

업무 간 호출은 Client와 공개 계약을 사용한다.

OM은 ServiceId별 정책과 운영정보를 관리한다.

## **2.5 주요 용어**

| **용어** | **설명** |
| --- | --- |
| 정적분석 | Source를 실행하지 않고 구조를 분석 |
| 동적분석 | 실제 실행·로그·디버깅으로 동작을 확인 |
| Call Graph | 프로그램 호출관계 |
| Dependency Graph | 모듈·Class 의존관계 |
| Entry Point | 실행이 시작되는 진입점 |
| Impact Analysis | 변경 영향범위 분석 |
| Forward Trace | 화면에서 DB 방향으로 추적 |
| Backward Trace | DB에서 화면 방향으로 추적 |
| Direct Impact | 직접 수정하거나 직접 참조하는 대상 |
| Indirect Impact | 호출·계약·운영을 통해 간접 영향받는 대상 |
| Runtime Binding | 실행 시 선택되는 실제 구현체 |
| Dead Code | 호출되지 않거나 사용할 수 없는 코드 |
| Duplicate Logic | 같은 기능이 여러 곳에 반복 구현된 상태 |
| Feature Flag | 기능의 활성 여부를 설정으로 제어 |
| Evidence | 분석을 뒷받침하는 Source·로그·실행결과 |
| Confidence | 분석결과의 확실성 수준 |

# **제177장. 처음 보는 저장소를 분석하는 순서**

학습 목표 | 177장. 처음 보는 저장소를 분석하는 순서의 핵심 개념을 설명하고, 프로젝트에서 적용 위치와 검증 방법을 스스로 판단한다.

핵심 관점 | 책임과 흐름: 이 주제는 개별 기술보다 입력, 처리 책임, 출력과 다음 단계의 연결 관계를 먼저 파악해야 합니다.

읽는 순서 | 먼저 용어와 책임 경계를 확인하고, 이어지는 정상 예시와 금지 예시를 비교한 뒤 실제 설정·코드·로그에서 근거를 찾아봅니다.

## **177.1 전체 파일부터 읽지 않는다**

대규모 저장소를 파일 이름순으로 읽는 것은 비효율적입니다.

먼저 다음 순서로 범위를 좁힙니다.

Root Build 구조

→ 실행 가능한 업무모듈

→ 대상 업무코드

→ 도메인 Package

→ ServiceId

→ Handler

→ 하위 계층

## **177.2 첫 번째 확인 파일**

저장소 Root에서 다음 파일을 확인합니다.

settings.gradle

build.gradle

gradle.properties

README

application.yml

모듈별 build.gradle

## **177.3 settings.gradle 분석**

예:

rootProject.name = "nsight-tcf-framework"

include(
"tcf-util",
"tcf-core",
"tcf-web",
"tcf-jwt",
"tcf-cache",
"tcf-eai",
"tcf-batch",
"tcf-om",
"sv-service",
"ic-service",
"ct-service",
"cm-service"
)

이 파일을 통해 다음을 파악합니다.

공통모듈 목록

업무모듈 목록

실제 모듈명

폐기되었지만 남은 모듈

문서와 실제 구조의 차이

## **177.4 모듈 분류표**

| **분류** | **예** | **분석 관점** |
| --- | --- | --- |
| 기반 | tcf-util | 공통 Utility |
| 거래 | tcf-core | STF·ETF·Dispatcher |
| Web | tcf-web | Controller·Filter |
| 인증 | tcf-jwt | Token |
| 연계 | tcf-eai | 외부 호출 |
| 운영 | tcf-om | 정책·관측 |
| 업무 | sv-service | 실제 업무 |
| Legacy | om-service | 이전 구현 여부 |
| 문서 | docs | 설계·가이드 |

## **177.5 실행모듈과 Library 구분**

다음 기준으로 구분합니다.

### **실행 가능한 모듈**

Spring Boot Main Class 존재

bootRun Task 존재

WAR 또는 Boot JAR 생성

server.port 설정 존재

### **Library 모듈**

Main Class 없음

다른 모듈이 implementation project()로 참조

JAR 생성 중심

## **177.6 업무모듈의 의존성 확인**

예:

dependencies {
implementation project(":tcf-util")
implementation project(":tcf-core")
implementation project(":tcf-web")
implementation project(":tcf-jwt")
}

확인할 사항:

어떤 공통모듈을 사용하는가?

다른 업무 WAR를 직접 의존하는가?

동일 Library Version이 중복되는가?

Spring Security가 어디서 유입되는가?

DB Driver는 어느 Scope인가?

## **177.7 Source Set 확인**

src/main/java

src/main/resources

src/test/java

src/test/resources

추가 Source Set이 있을 수 있습니다.

src/integrationTest/java

src/generated/java

src/local/java

Build Script를 통해 실제 Compile 대상인지 확인합니다.

## **177.8 Package 구조 확인**

예:

com.nh.nsight.marketing.ct.contact
├─ handler
├─ facade
├─ service
├─ rule
├─ dao
├─ mapper
├─ client
└─ dto

이 구조가 없다면 다음 가능성을 확인합니다.

Legacy 구조

비표준 Package

도메인 이름 차이

기능이 다른 모듈에 존재

공통화 과정에서 이동

## **177.9 최초 분석 결과표**

| **항목** | **내용** |
| --- | --- |
| 대상 저장소 | nsight-tcf-framework |
| 대상 Branch | develop |
| 기준 Commit | Commit Hash |
| 대상 업무 | CT |
| 대상 모듈 | ct-service |
| 공통 의존성 | tcf-core, tcf-web |
| 실행방식 | bootRun·WAR |
| DB | Oracle·H2 |
| 주요 Package | ct.contact |
| 분석 대상 ServiceId | CT.Contact.selectList |

분석 기준 Commit을 기록해야 이후 Source 변경과 결과 차이를 설명할 수 있습니다.

## **제177장 요약**

학습 목표 | 177장 요약의 핵심 개념을 설명하고, 프로젝트에서 적용 위치와 검증 방법을 스스로 판단한다.

핵심 관점 | 책임과 흐름: 이 주제는 개별 기술보다 입력, 처리 책임, 출력과 다음 단계의 연결 관계를 먼저 파악해야 합니다.

읽는 순서 | 먼저 용어와 책임 경계를 확인하고, 이어지는 정상 예시와 금지 예시를 비교한 뒤 실제 설정·코드·로그에서 근거를 찾아봅니다.

처음에는 Source 파일보다 모듈과 Build 구조를 확인한다.

실행모듈·Library·업무모듈·Legacy 모듈을 구분한다.

분석 대상 Branch와 Commit을 고정한 뒤 세부 추적을 시작한다.

# **제178장. 화면과 ServiceId에서 소스 찾기**

학습 목표 | 178장. 화면과 ServiceId에서 소스 찾기의 핵심 개념을 설명하고, 프로젝트에서 적용 위치와 검증 방법을 스스로 판단한다.

핵심 관점 | 재현 가능한 개발환경: 개인 PC에서만 되는 설정을 피하고 버전, 의존성, 실행 옵션을 팀이 재현할 수 있게 고정해야 합니다.

읽는 순서 | 먼저 용어와 책임 경계를 확인하고, 이어지는 정상 예시와 금지 예시를 비교한 뒤 실제 설정·코드·로그에서 근거를 찾아봅니다.

## **178.1 가장 좋은 시작점**

업무 요청에 다음 정보가 있다면 우선순위를 적용합니다.

1\. ServiceId

2\. 화면 이벤트 ID

3\. 화면 ID

4\. 거래코드

5\. 오류코드

6\. 프로그램명

7\. 테이블명

ServiceId가 가장 직접적인 실행 식별자입니다.

## **178.2 ServiceId 문자열 검색**

예:

CT.Contact.selectList

검색대상:

Java 상수

Handler serviceIds()

Switch Case

OM Seed Data

테스트

문서

화면 호출정의

명령행 예:

grep -R "CT.Contact.selectList" .

Windows PowerShell:

Get-ChildItem -Recurse -File |
Select-String "CT.Contact.selectList"

IDE에서는 전체 Workspace 검색을 사용합니다.

## **178.3 검색결과 분류**

동일 문자열이 여러 곳에서 발견될 수 있습니다.

| **위치** | **의미** |
| --- | --- |
| ServiceId 상수 | 공식 코드 정의 |
| Handler | 실제 실행 분기 |
| Test | 검증 |
| OM SQL | 기준정보 등록 |
| 문서 | 설계정보 |
| 화면 JSON | 호출자 |
| Legacy 코드 | 사용 여부 확인 필요 |

문서 검색 결과만으로 실제 실행 프로그램을 확정하지 않습니다.

## **178.4 Handler 등록 확인**

예:

@Component
public class CtContactHandler
implements TransactionHandler {

@Override
public Set<String> serviceIds() {
return Set.of(
"CT.Contact.selectList",
"CT.Contact.selectDetail",
"CT.Contact.create"
);
}
}

확인할 사항:

Spring Bean으로 등록되는가?

실행 Profile 조건이 있는가?

중복 Handler가 있는가?

현재 Branch에 포함되는가?

해당 WAR가 실제 배포 대상인가?

## **178.5 화면 ID에서 시작하는 경우**

예:

CT-CNT-0001

검색대상:

화면 Source

화면 이벤트 정의

메뉴 정의

권한 설정

API 호출 Script

산출물

테스트 자동화

화면 코드에서 다음을 찾습니다.

serviceId

businessCode

transactionCode

Endpoint

Request Body

## **178.6 거래코드에서 시작하는 경우**

예:

CT-INQ-0001

거래코드는 운영 분류용으로 사용될 수 있으므로 ServiceId와 반드시 1:1이라고 가정하지 않습니다.

다음을 확인합니다.

거래코드 Registry

Service Catalog

Header 생성부

Handler

거래로그

## **178.7 오류코드에서 시작하는 경우**

예:

E-CT-CNT-0008

검색하면 다음을 찾을 수 있습니다.

Rule

Service

오류코드 Registry

메시지 Bundle

테스트

화면 오류처리

오류코드 위치는 해당 업무규칙의 핵심 진입점이 될 수 있습니다.

## **178.8 ServiceId가 검색되지 않는 경우**

가능한 원인:

문자열을 동적으로 생성

상수 조합

설정파일에서 등록

Legacy 이름 사용

Branch가 다름

폐기된 기능

문서만 존재하고 구현 없음

예:

String serviceId =
businessCode + "." + domain + "." + action;

동적 생성은 추적성과 자동검증을 어렵게 하므로 권장하지 않습니다.

## **178.9 실제 실행 여부 확인**

Source에 존재한다고 Runtime에서 실행되는 것은 아닙니다.

확인방법:

Handler Registry 로그

Spring Bean 목록

OM Service Catalog

통합 테스트

실제 Trace 로그

배포 WAR 내부 Class

## **제178장 요약**

학습 목표 | 178장 요약의 핵심 개념을 설명하고, 프로젝트에서 적용 위치와 검증 방법을 스스로 판단한다.

핵심 관점 | 책임과 흐름: 이 주제는 개별 기술보다 입력, 처리 책임, 출력과 다음 단계의 연결 관계를 먼저 파악해야 합니다.

읽는 순서 | 먼저 용어와 책임 경계를 확인하고, 이어지는 정상 예시와 금지 예시를 비교한 뒤 실제 설정·코드·로그에서 근거를 찾아봅니다.

ServiceId가 있으면 ServiceId부터 검색한다.

검색결과는 상수·실행코드·설정·테스트·문서로 분류한다.

Source 존재 여부와 Runtime 등록 여부를 별도로 확인한다.

# **제179장. Handler부터 Mapper까지 정방향 분석**

학습 목표 | 179장. Handler부터 Mapper까지 정방향 분석의 핵심 개념을 설명하고, 프로젝트에서 적용 위치와 검증 방법을 스스로 판단한다.

핵심 관점 | 데이터 일관성: 화면 동작보다 데이터 소유권, 상태 전이, 동시성, 트랜잭션 경계를 먼저 결정해야 합니다.

읽는 순서 | 먼저 용어와 책임 경계를 확인하고, 이어지는 정상 예시와 금지 예시를 비교한 뒤 실제 설정·코드·로그에서 근거를 찾아봅니다.

## **179.1 정방향 분석의 목적**

정방향 분석은 하나의 거래가 어떤 프로그램과 데이터를 거쳐 실행되는지 확인하는 과정입니다.

ServiceId

→ Handler

→ Facade

→ Service

→ Rule

→ DAO

→ Mapper

→ SQL

→ Table

## **179.2 Handler 분석**

예:

case CtContactServiceIds.SELECT\_LIST ->
contactFacade.selectList(
converter.convert(
body,
ContactListRequest.class
),
context
);

분석항목:

| **항목** | **확인** |
| --- | --- |
| ServiceId | 어떤 Case인가 |
| Request DTO | 어떤 Class인가 |
| Facade Method | 어느 Method인가 |
| Context | 전달되는가 |
| 예외 | 어디서 처리되는가 |
| Response | 어떤 유형인가 |

## **179.3 Handler에서 확인할 금지구조**

Handler가 Mapper 직접 호출

Handler가 DB Transaction 관리

Handler가 사용자 ID를 Request에서 추출

Handler가 여러 업무 Use Case를 복잡하게 조합

Handler가 외부 Client 직접 호출

발견 시 현재 기능 분석과 별도로 구조 개선 후보로 기록합니다.

## **179.4 Facade 분석**

예:

@Transactional(readOnly = true)
public ContactListResponse selectList(
ContactListRequest request,
TransactionContext context) {

return contactService.selectList(
request,
context
);
}

확인:

트랜잭션 Annotation

readOnly

호출 Service 수

외부 호출 위치

예외 변환

Transaction 범위

## **179.5 Self Invocation 확인**

예:

public void create() {
this.saveInTransaction();
}

@Transactional
public void saveInTransaction() {
}

같은 Class 내부 직접 호출은 Spring Proxy를 거치지 않아 트랜잭션이 적용되지 않을 수 있습니다.

분석보고서에 다음을 기록합니다.

Annotation 존재

실제 Proxy 호출 여부

Rollback Test 존재 여부

## **179.6 Service 분석**

Service에서는 업무 흐름을 순서대로 정리합니다.

예:

1\. 조회기간 정규화

2\. 데이터권한 구성

3\. 전체건수 조회

4\. 목록 조회

5\. 코드명 변환

6\. 응답 생성

Source를 그대로 복사하기보다 **업무 동작**으로 요약합니다.

## **179.7 Rule 분석**

확인:

필수값

상태전이

날짜·금액 범위

중복

권한 범위

오류코드

현재시간 의존성

DB 접근 여부

Rule이 DAO를 호출한다면 순수 Rule과 조회형 Validation의 경계를 검토합니다.

## **179.8 DAO 분석**

DAO Method에서 다음을 확인합니다.

호출 Mapper

Parameter 변환

조회·영향 건수

예외 변환

여러 Mapper 호출

DataSource 선택

## **179.9 Mapper Interface 분석**

예:

List<ContactListData> selectContactList(
ContactListQuery query
);

확인:

Method명

Parameter Type

Return Type

XML Namespace

SQL ID

여러 DataSource 여부

## **179.10 Mapper XML 분석**

예:

<select
id="selectContactList"
resultType="...ContactListData">

SELECT ...
FROM CT\_CONTACT\_MASTER
WHERE CUSTOMER\_NO = #{customerNo}
</select>

확인:

Namespace

Statement ID

Parameter Binding

Result Mapping

조회조건

권한조건

정렬

최대건수

대상 Table

동적 SQL

## **179.11 Table까지 추적**

SQL에서 Table과 Column을 추출합니다.

| **SQL ID** | **Table** | **Column** | **유형** |
| --- | --- | --- | --- |
| CT-CNT-SEL-001 | CT\_CONTACT\_MASTER | CUSTOMER\_NO | 조건 |
| CT-CNT-SEL-001 | CT\_CONTACT\_MASTER | CONTACT\_TITLE | 출력 |
| CT-CNT-SEL-001 | CT\_CONTACT\_MASTER | OPEN\_YN | 권한 |

## **179.12 정방향 분석표**

| **단계** | **대상** | **분석결과** |
| --- | --- | --- |
| ServiceId | CT.Contact.selectList | 목록조회 |
| Handler | CtContactHandler | selectList 분기 |
| Facade | CtContactFacade | Readonly Transaction |
| Service | CtContactService | 기간·권한·Paging |
| Rule | CtContactRule | 최대 3개월 |
| DAO | CtContactDao | Count·List |
| Mapper | CtContactMapper | 2개 SQL |
| SQL | CT-CNT-SEL-001 | 목록 |
| Table | CT\_CONTACT\_MASTER | 상담 Master |

## **제179장 요약**

학습 목표 | 179장 요약의 핵심 개념을 설명하고, 프로젝트에서 적용 위치와 검증 방법을 스스로 판단한다.

핵심 관점 | 책임과 흐름: 이 주제는 개별 기술보다 입력, 처리 책임, 출력과 다음 단계의 연결 관계를 먼저 파악해야 합니다.

읽는 순서 | 먼저 용어와 책임 경계를 확인하고, 이어지는 정상 예시와 금지 예시를 비교한 뒤 실제 설정·코드·로그에서 근거를 찾아봅니다.

정방향 분석은 ServiceId부터 DB까지 호출관계를 따라간다.

각 계층에서는 Class명보다 책임·입출력·예외·상태변경을 확인한다.

트랜잭션은 Annotation 존재뿐 아니라 실제 Proxy 적용을 확인한다.

# **제180장. SQL·테이블에서 화면까지 역방향 분석**

학습 목표 | 180장. SQL·테이블에서 화면까지 역방향 분석의 핵심 개념을 설명하고, 프로젝트에서 적용 위치와 검증 방법을 스스로 판단한다.

핵심 관점 | 데이터 일관성: 화면 동작보다 데이터 소유권, 상태 전이, 동시성, 트랜잭션 경계를 먼저 결정해야 합니다.

읽는 순서 | 먼저 용어와 책임 경계를 확인하고, 이어지는 정상 예시와 금지 예시를 비교한 뒤 실제 설정·코드·로그에서 근거를 찾아봅니다.

## **180.1 역방향 분석이 필요한 상황**

DB 컬럼 변경

Table 폐기

Slow SQL 장애

개인정보 컬럼 확인

Index 변경

데이터 정합성 오류

특정 SQL 호출자 조사

## **180.2 테이블명 검색**

예:

CT\_CONTACT\_MASTER

검색대상:

Mapper XML

JPA Entity

SQL Script

Batch

View

Procedure

운영 Query

문서

## **180.3 SQL 문장과 SQL ID 구분**

동일 Table을 여러 SQL이 사용할 수 있습니다.

따라서 다음 단위로 분리합니다.

Table

→ SQL Statement

→ Mapper Method

→ DAO Method

→ Service Method

→ ServiceId

## **180.4 Mapper에서 DAO 찾기**

Mapper Interface Method명 검색:

selectContactList

호출자 검색 결과에서 다음을 구분합니다.

DAO 실제 호출

단위 테스트

Mock 설정

Legacy 호출

사용하지 않는 Method

## **180.5 DAO에서 Service 찾기**

예:

contactDao.selectContactList(query)

호출 Service Method를 찾고 상위 호출자를 계속 추적합니다.

## **180.6 Service에서 ServiceId 찾기**

Service Method가 여러 Facade에서 호출될 수 있습니다.

CtContactService.selectList

→ CtContactFacade.selectList

→ CtContactHandler

→ CT.Contact.selectList

공통 Service라면 여러 ServiceId가 사용할 수 있습니다.

## **180.7 ServiceId에서 화면 찾기**

검색대상:

UI Source

화면 이벤트 관리대장

API Collection

타 업무 Client

Batch Job

외부 호출자

운영 수동 호출

화면만 호출한다고 가정하면 영향범위를 축소할 위험이 있습니다.

## **180.8 컬럼 단위 영향분석**

예:

CONTACT\_TITLE

사용유형을 구분합니다.

| **유형** | **영향** |
| --- | --- |
| 조회 출력 | Response·화면 |
| 검색 조건 | Request·Index |
| 정렬 | Paging·성능 |
| 등록 입력 | Validation·Command |
| 변경 입력 | Version·History |
| 파일 출력 | Export Format |
| 외부 연계 | Contract |
| 로그 | 마스킹 |

## **180.9 View·Procedure 주의**

업무 Source에 Table명이 직접 나타나지 않을 수 있습니다.

Mapper
→ View

View
→ Base Table

또는:

Mapper
→ Stored Procedure

Procedure
→ 여러 Table

DB Metadata까지 확인해야 정확한 영향분석이 가능합니다.

## **180.10 동적 Table명**

예:

FROM ${tableName}

보안상 위험할 뿐 아니라 정적분석이 어렵습니다.

가능하면 제거하고 명시적인 Mapper로 분리합니다.

## **180.11 역방향 영향도 표**

| **변경대상** | **직접 영향** | **간접 영향** |
| --- | --- | --- |
| CONTACT\_TITLE 길이 | Mapper·DTO | 화면·파일·연계 |
| CT\_CONTACT\_MASTER | SQL 7개 | ServiceId 5개 |
| CT-CNT-SEL-001 | DAO 1개 | 화면 2개 |
| CT.Contact.selectList | Handler·OM | UI·Client |

## **제180장 요약**

학습 목표 | 180장 요약의 핵심 개념을 설명하고, 프로젝트에서 적용 위치와 검증 방법을 스스로 판단한다.

핵심 관점 | 책임과 흐름: 이 주제는 개별 기술보다 입력, 처리 책임, 출력과 다음 단계의 연결 관계를 먼저 파악해야 합니다.

읽는 순서 | 먼저 용어와 책임 경계를 확인하고, 이어지는 정상 예시와 금지 예시를 비교한 뒤 실제 설정·코드·로그에서 근거를 찾아봅니다.

역방향 분석은 Table·Column·SQL에서 ServiceId와 화면 방향으로 진행한다.

Table명 검색만으로 끝내지 않고 View·Procedure·Batch·파일을 확인한다.

컬럼은 조회·조건·정렬·입력·연계 등 사용유형별로 영향이 다르다.

# **제181장. 업무 간 호출과 외부연계 분석**

학습 목표 | 181장. 업무 간 호출과 외부연계 분석의 핵심 개념을 설명하고, 프로젝트에서 적용 위치와 검증 방법을 스스로 판단한다.

핵심 관점 | 계약과 결합도: 호출자는 공개 계약에만 의존하고 상대 시스템의 내부 테이블이나 구현 세부사항을 전제로 삼지 않아야 합니다.

읽는 순서 | 먼저 용어와 책임 경계를 확인하고, 이어지는 정상 예시와 금지 예시를 비교한 뒤 실제 설정·코드·로그에서 근거를 찾아봅니다.

## **181.1 업무 간 호출 유형**

HTTP·JSON ServiceId 호출

공통 Client 호출

EAI 연계

Message·Event

DB 직접 접근

공통 Library 호출

## **181.2 정상 업무 간 호출**

CT Service

→ IcCustomerClient

→ IC.Customer.selectBasic

→ ic-service

→ IC 소유 DB

확인항목:

대상 ServiceId

요청·응답 DTO

Timeout

Retry

인증정보 전파

TraceId 전파

오류코드 매핑

## **181.3 직접 DB 접근 탐지**

예:

SV Mapper

→ IC\_CUSTOMER\_MASTER

확인:

왜 직접 접근하는가?

공식 승인 예외인가?

Read Only인가?

IC Schema 변경 시 영향은?

데이터권한은 누가 책임지는가?

직접 접근은 변경 결합도와 데이터 소유권 문제를 발생시킬 수 있습니다.

## **181.4 Client Interface 분석**

예:

public interface IcCustomerClient {

CustomerBasicInfo selectBasic(
String customerNo,
TransactionContext context
);
}

구현체가 여러 개일 수 있습니다.

HttpIcCustomerClient

MockIcCustomerClient

LocalIcCustomerClient

FallbackIcCustomerClient

Runtime에서 어떤 구현체가 선택되는지 확인해야 합니다.

## **181.5 외부 시스템 연계**

업무 Service

→ 업무 Adapter

→ tcf-eai Client

→ 외부 시스템

분석항목:

| **항목** | **확인** |
| --- | --- |
| Interface ID | 공식 연계 ID |
| 요청 전문 | 내부·외부 변환 |
| Timeout | Connect·Read |
| Retry | 안전 여부 |
| 오류 | 기술·업무 구분 |
| 보상 | 외부 성공·내부 실패 |
| 로그 | 개인정보 마스킹 |
| Trace | GUID 전파 |

## **181.6 연계의 트랜잭션 위치**

다음 흐름은 위험할 수 있습니다.

DB Transaction 시작

→ DB 조회

→ 외부 호출 10초

→ DB 변경

→ Commit

Connection 장기 점유 여부를 분석합니다.

## **181.7 비동기 Event 분석**

Event 기반이면 다음을 확인합니다.

Event Producer

Event Type

Payload Version

Broker·Queue

Consumer

Retry·DLQ

중복 처리

순서 보장

최종 정합성

단순 Java Event와 외부 Message Event를 구분합니다.

## **181.8 연계 영향도**

외부 응답 필드 변경 시:

외부 Contract

→ Adapter

→ 내부 Result DTO

→ Service Rule

→ Response DTO

→ 화면·Batch

→ 테스트

## **제181장 요약**

학습 목표 | 181장 요약의 핵심 개념을 설명하고, 프로젝트에서 적용 위치와 검증 방법을 스스로 판단한다.

핵심 관점 | 책임과 흐름: 이 주제는 개별 기술보다 입력, 처리 책임, 출력과 다음 단계의 연결 관계를 먼저 파악해야 합니다.

읽는 순서 | 먼저 용어와 책임 경계를 확인하고, 이어지는 정상 예시와 금지 예시를 비교한 뒤 실제 설정·코드·로그에서 근거를 찾아봅니다.

업무 간 호출은 Client와 대상 ServiceId를 중심으로 분석한다.

Interface 구현체가 여러 개라면 Runtime 선택조건을 확인한다.

외부 호출의 Timeout·Retry·트랜잭션·보상처리를 함께 분석한다.

# **제182장. Spring Bean·설정·Runtime 연결 분석**

학습 목표 | 182장. Spring Bean·설정·Runtime 연결 분석의 핵심 개념을 설명하고, 프로젝트에서 적용 위치와 검증 방법을 스스로 판단한다.

핵심 관점 | 책임과 흐름: 이 주제는 개별 기술보다 입력, 처리 책임, 출력과 다음 단계의 연결 관계를 먼저 파악해야 합니다.

읽는 순서 | 먼저 용어와 책임 경계를 확인하고, 이어지는 정상 예시와 금지 예시를 비교한 뒤 실제 설정·코드·로그에서 근거를 찾아봅니다.

## **182.1 파일명만으로 구현체를 확정할 수 없는 이유**

Spring은 실행 시 Bean을 선택합니다.

@Component

@Bean

@Profile

@ConditionalOnProperty

@Primary

@Qualifier

에 따라 실제 구현체가 달라질 수 있습니다.

## **182.2 Bean 등록방식**

### **Component Scan**

@Component
public class CachedOmServicePolicyProvider
implements ServicePolicyProvider {
}

### **Configuration**

@Bean
public ServicePolicyProvider servicePolicyProvider() {
return new CachedOmServicePolicyProvider();
}

## **182.3 Profile 조건**

@Profile("local")
@Component
public class MockIcCustomerClient
implements IcCustomerClient {
}

@Profile("!local")
@Component
public class HttpIcCustomerClient
implements IcCustomerClient {
}

로컬과 운영 동작이 달라질 수 있습니다.

## **182.4 Property 조건**

@ConditionalOnProperty(
name = "tcf.gateway.enabled",
havingValue = "true"
)

설정값에 따라 기능이 활성화됩니다.

## **182.5 Primary와 Qualifier**

@Primary
@Component
public class DefaultCacheProvider {
}

public Service(
@Qualifier("customerCache")
TcfCache cache
) {
}

동일 Interface 구현체 중 실제 주입 대상을 확인합니다.

## **182.6 YAML 분석**

확인할 항목:

기본값

Profile별 Override

환경변수

명령행 Override

외부 Config

Deprecated Key

## **182.7 설정값의 실제 최종값**

문서의 application.yml 값과 Runtime 값이 다를 수 있습니다.

예:

application.yml
timeout 3000

환경변수
TCF\_TIMEOUT=5000

명령행
\--tcf.timeout=10000

실제 최종값은 10,000ms일 수 있습니다.

## **182.8 Configuration Properties**

@ConfigurationProperties(
prefix = "tcf.transaction"
)
public class TransactionProperties {

private long defaultTimeoutMs;
}

분석할 때 다음을 연결합니다.

설정 Key

→ Properties Field

→ 사용하는 Bean

→ ServiceId 정책

→ Runtime 동작

## **182.9 AOP와 Proxy**

다음 기능은 Source 호출관계만으로 보이지 않을 수 있습니다.

@Transactional

@Async

@Cacheable

Method Security

Retry

Circuit Breaker

Metric

Annotation과 AOP 설정을 함께 확인합니다.

## **182.10 자동 설정**

Spring Boot Starter를 통해 Bean이 자동 등록될 수 있습니다.

AutoConfiguration

spring.factories

AutoConfiguration.imports

Starter Dependency

Source 검색으로 Bean을 찾지 못했다면 자동 설정을 확인합니다.

## **182.11 Runtime 검증**

실제 Bean 확인방법:

기동 로그

Actuator Beans

Debugger

ApplicationContext 조회

통합 테스트

Bean Definition Report

운영환경에서 관리 Endpoint를 무분별하게 노출하지 않습니다.

## **제182장 요약**

학습 목표 | 182장 요약의 핵심 개념을 설명하고, 프로젝트에서 적용 위치와 검증 방법을 스스로 판단한다.

핵심 관점 | 책임과 흐름: 이 주제는 개별 기술보다 입력, 처리 책임, 출력과 다음 단계의 연결 관계를 먼저 파악해야 합니다.

읽는 순서 | 먼저 용어와 책임 경계를 확인하고, 이어지는 정상 예시와 금지 예시를 비교한 뒤 실제 설정·코드·로그에서 근거를 찾아봅니다.

Spring Source에서는 Interface보다 Runtime 구현체 선택이 중요하다.

Profile·Property·Primary·Qualifier·자동 설정을 확인한다.

YAML에 적힌 값보다 Runtime 최종 설정값을 기준으로 분석한다.

# **제183장. Transaction·Timeout·보안 흐름 분석**

학습 목표 | 183장. Transaction·Timeout·보안 흐름 분석의 핵심 개념을 설명하고, 프로젝트에서 적용 위치와 검증 방법을 스스로 판단한다.

핵심 관점 | 신뢰 경계: 사용자 신원과 권한, 토큰의 유효기간, 민감정보 노출 여부를 분리해 확인해야 합니다.

읽는 순서 | 먼저 용어와 책임 경계를 확인하고, 이어지는 정상 예시와 금지 예시를 비교한 뒤 실제 설정·코드·로그에서 근거를 찾아봅니다.

## **183.1 기능 분석에서 반드시 확인할 공통항목**

Transaction

Timeout

인증

기능권한

데이터권한

Idempotency

감사로그

업무로직만 읽고 끝내면 운영 위험을 놓칠 수 있습니다.

## **183.2 Transaction 분석**

확인:

트랜잭션 시작 Method

readOnly

Isolation

Propagation

Rollback 대상

복수 DataSource

외부 호출 포함 여부

History 동일 거래 여부

## **183.3 예외를 잡는 코드**

예:

try {
mapper.insertHistory(command);
} catch (Exception e) {
log.warn("history failed", e);
}

이 구조는 History 실패를 무시해 Transaction이 Commit될 수 있습니다.

다음을 확인합니다.

필수 이력인가?

예외를 다시 던지는가?

Rollback이 필요한가?

운영경보가 있는가?

## **183.4 Timeout 분석**

Timeout 설정경로:

OM Service Policy

→ ServicePolicyProvider

→ TransactionContext.timeout

→ Timeout Executor

하위 Timeout:

DB Connection Timeout

SQL Query Timeout

HTTP Connect Timeout

HTTP Read Timeout

상·하위 시간예산을 비교합니다.

## **183.5 인증 분석**

JWT Filter

→ JwtTokenValidator

→ AuthenticationContext

→ TransactionContext

→ 업무 Service

업무코드가 Request의 사용자 ID를 사용하는지 확인합니다.

금지:

String userId = request.userId();

권장:

String userId = context.getUserId();

## **183.6 기능권한 분석**

ServiceId

→ 권한코드

→ 사용자 Role

→ STF Authorization

다음 오류를 구분합니다.

인증 실패

기능권한 실패

데이터권한 실패

## **183.7 데이터권한 분석**

SQL 또는 Rule에 다음 조건이 있는지 확인합니다.

사용자 소속

지점

조직

담당 고객

공개 여부

관리자 예외권한

기능권한이 있다고 모든 데이터를 조회할 수 있는 것은 아닙니다.

## **183.8 Idempotency 분석**

변경 거래에서 확인:

Idempotency Key 수신

요청 Hash

상태 저장

성공 결과 저장

동일 Key 재요청

TIMEOUT·UNKNOWN 처리

## **183.9 감사 분석**

감사대상:

개인정보 상세조회

등록·변경·삭제

권한 변경

파일 다운로드

관리자 운영조치

감사로그 실패가 업무 Transaction에 미치는 영향도 확인합니다.

## **183.10 공통 통제 분석표**

| **항목** | **적용위치** | **결과** |
| --- | --- | --- |
| 인증 | JWT Filter | 적용 |
| 기능권한 | STF | 적용 |
| 데이터권한 | SQL | 지점조건 |
| Timeout | TCF | 3초 |
| SQL Timeout | Mapper | 2초 |
| Idempotency | STF·DB | 미적용 |
| 감사 | ETF | 상세조회 기록 |

## **제183장 요약**

학습 목표 | 183장 요약의 핵심 개념을 설명하고, 프로젝트에서 적용 위치와 검증 방법을 스스로 판단한다.

핵심 관점 | 책임과 흐름: 이 주제는 개별 기술보다 입력, 처리 책임, 출력과 다음 단계의 연결 관계를 먼저 파악해야 합니다.

읽는 순서 | 먼저 용어와 책임 경계를 확인하고, 이어지는 정상 예시와 금지 예시를 비교한 뒤 실제 설정·코드·로그에서 근거를 찾아봅니다.

기존 기능 분석은 업무코드뿐 아니라
Transaction·Timeout·보안·중복·감사를 포함해야 한다.

Annotation이 존재하는지만 보지 말고
실제 Runtime 적용과 실패정책을 확인한다.

# **제184장. 로그·Trace·디버깅을 이용한 동적분석**

학습 목표 | 184장. 로그·Trace·디버깅을 이용한 동적분석의 핵심 개념을 설명하고, 프로젝트에서 적용 위치와 검증 방법을 스스로 판단한다.

핵심 관점 | 재현 가능한 개발환경: 개인 PC에서만 되는 설정을 피하고 버전, 의존성, 실행 옵션을 팀이 재현할 수 있게 고정해야 합니다.

읽는 순서 | 먼저 용어와 책임 경계를 확인하고, 이어지는 정상 예시와 금지 예시를 비교한 뒤 실제 설정·코드·로그에서 근거를 찾아봅니다.

## **184.1 정적분석의 한계**

Source만으로 다음을 확정하기 어려울 수 있습니다.

실제 선택 Bean

실제 Profile

실제 ServiceId Handler

Runtime SQL

실제 Timeout

실행되지 않는 분기

동적 Proxy

따라서 동적분석을 병행합니다.

## **184.2 대표 요청 준비**

분석 기능의 정상 요청과 오류 요청을 준비합니다.

정상 조회

결과 없음

권한 없음

Validation 실패

Timeout

동시성 충돌

## **184.3 Trace 확인**

GUID

TraceId

ServiceId

Server ID

WAR

처리시간

오류코드

하나의 Trace를 기준으로 로그를 연결합니다.

## **184.4 Breakpoint 순서**

Controller

TCF

STF

Dispatcher

Handler

Facade

Service

Rule

DAO

ETF

## **184.5 Runtime Call Graph 작성**

| **순서** | **Class·Method** | **주요 값** |
| --- | --- | --- |
| 1 | Controller | Request |
| 2 | STF | User·Timeout |
| 3 | Dispatcher | Handler |
| 4 | Handler | DTO |
| 5 | Facade | Transaction |
| 6 | Service | Business Flow |
| 7 | DAO | Query |
| 8 | ETF | Result |

## **184.6 SQL 확인**

SQL ID

Parameter

수행시간

조회건수

영향건수

실행계획

SQL 전체 Parameter 로그는 개인정보를 마스킹합니다.

## **184.7 예외 Breakpoint**

BusinessException

SystemException

DataAccessException

TransactionTimeoutException

ExternalTimeoutException

예외가 어느 계층에서 생성되고 어디서 변환되는지 확인합니다.

## **184.8 분기 Coverage**

특정 코드 분기가 실제로 실행되는지 확인합니다.

예:

if (request.isManager()) {
...
}

다음을 확인합니다.

어떤 입력에서 true인가?

실제 권한정보를 사용하는가?

Test가 있는가?

운영 호출이 있는가?

## **184.9 Runtime 증적**

요청 전문

응답 전문

Trace 로그

Breakpoint 확인결과

SQL 실행정보

Transaction 결과

DB 전후 데이터

## **184.10 분석 결과의 확실성**

| **등급** | **기준** |
| --- | --- |
| Confirmed | Source와 Runtime 모두 확인 |
| High | Source·Test·설정 일치 |
| Medium | Source만 확인 |
| Low | 이름·주석 기반 추정 |
| Unknown | 확인자료 없음 |

분석보고서에서 추정사항을 사실처럼 작성하지 않습니다.

## **제184장 요약**

학습 목표 | 184장 요약의 핵심 개념을 설명하고, 프로젝트에서 적용 위치와 검증 방법을 스스로 판단한다.

핵심 관점 | 책임과 흐름: 이 주제는 개별 기술보다 입력, 처리 책임, 출력과 다음 단계의 연결 관계를 먼저 파악해야 합니다.

읽는 순서 | 먼저 용어와 책임 경계를 확인하고, 이어지는 정상 예시와 금지 예시를 비교한 뒤 실제 설정·코드·로그에서 근거를 찾아봅니다.

정적분석으로 구조를 파악하고 동적분석으로 실제 실행을 확인한다.

Trace·Breakpoint·SQL·DB 결과를 하나의 거래로 연결한다.

분석결과에는 확실성 수준을 표시한다.

# **제185장. 테스트코드로 기존 기능 이해하기**

학습 목표 | 185장. 테스트코드로 기존 기능 이해하기의 핵심 개념을 설명하고, 프로젝트에서 적용 위치와 검증 방법을 스스로 판단한다.

핵심 관점 | 검증 가능성: 좋은 구현은 동작할 뿐 아니라 변경 이유와 영향 범위를 증명할 수 있어야 합니다.

읽는 순서 | 먼저 용어와 책임 경계를 확인하고, 이어지는 정상 예시와 금지 예시를 비교한 뒤 실제 설정·코드·로그에서 근거를 찾아봅니다.

## **185.1 테스트가 중요한 이유**

좋은 테스트는 기존 기능의 기대동작을 설명합니다.

어떤 입력을 받는가?

어떤 업무규칙이 있는가?

어떤 오류가 발생하는가?

어떤 데이터를 변경하는가?

## **185.2 테스트 종류**

Rule Unit Test

Service Unit Test

Mapper Test

Integration Test

Transaction Test

E2E Test

Architecture Test

## **185.3 Rule Test 분석**

예:

@Test
void shouldRejectPeriodOverThreeMonths() {
}

이름과 Assertion을 통해 업무규칙을 파악할 수 있습니다.

## **185.4 Service Test 분석**

Mock 설정을 보면 Service의 협력객체를 알 수 있습니다.

CustomerClient

Rule

DAO

History DAO

Verify를 통해 호출순서를 추정할 수 있습니다.

## **185.5 Mapper Test 분석**

확인:

실제 SQL

Test Dataset

정렬

Null

권한조건

영향건수

Oracle 호환성

## **185.6 E2E Test 분석**

Endpoint

JWT

Header

ServiceId

Request

Response

DB 결과

전체 계약을 가장 쉽게 파악할 수 있습니다.

## **185.7 테스트와 Source 불일치**

가능한 상황:

Source는 변경됨

Test는 과거 기대값

Test가 Disabled

Assertion이 부족

Mock이 실제 동작과 다름

테스트가 있다고 무조건 현재 요구사항을 정확히 표현한다고 가정하지 않습니다.

## **185.8 Disabled Test**

@Disabled("temporary")

또는 테스트 Task에서 제외된 Test를 확인합니다.

장기간 비활성화된 테스트는 품질위험입니다.

## **185.9 테스트 누락이 의미하는 것**

미구현 기능

Legacy 코드

검증되지 않은 기능

단순 누락

운영 수동 기능

추가 확인이 필요합니다.

## **185.10 테스트 기반 분석표**

| **Test** | **대상** | **확인된 규칙** |
| --- | --- | --- |
| CtContactRuleTest | 기간 | 최대 3개월 |
| CtContactServiceTest | 등록 | History 필수 |
| CtContactMapperTest | 조회 | 지점권한 |
| CtContactE2ETest | 전체 | JWT·응답 |

## **제185장 요약**

학습 목표 | 185장 요약의 핵심 개념을 설명하고, 프로젝트에서 적용 위치와 검증 방법을 스스로 판단한다.

핵심 관점 | 책임과 흐름: 이 주제는 개별 기술보다 입력, 처리 책임, 출력과 다음 단계의 연결 관계를 먼저 파악해야 합니다.

읽는 순서 | 먼저 용어와 책임 경계를 확인하고, 이어지는 정상 예시와 금지 예시를 비교한 뒤 실제 설정·코드·로그에서 근거를 찾아봅니다.

테스트코드는 기존 기능의 기대동작을 이해하는 중요한 자료다.

다만 Disabled·Mock·오래된 Assertion을 확인해야 한다.

Source·요구사항·Runtime과 테스트가 일치하는지 비교한다.

# **제186장. 변경 영향도 분석 방법**

학습 목표 | 186장. 변경 영향도 분석 방법의 핵심 개념을 설명하고, 프로젝트에서 적용 위치와 검증 방법을 스스로 판단한다.

핵심 관점 | 책임과 흐름: 이 주제는 개별 기술보다 입력, 처리 책임, 출력과 다음 단계의 연결 관계를 먼저 파악해야 합니다.

읽는 순서 | 먼저 용어와 책임 경계를 확인하고, 이어지는 정상 예시와 금지 예시를 비교한 뒤 실제 설정·코드·로그에서 근거를 찾아봅니다.

## **186.1 영향도 분석의 목적**

영향도 분석은 단순히 수정할 파일을 찾는 작업이 아닙니다.

무엇을 수정해야 하는가?

무엇이 함께 영향을 받는가?

무엇을 재검증해야 하는가?

운영과 데이터에 어떤 위험이 있는가?

Rollback이 가능한가?

에 답해야 합니다.

## **186.2 영향의 유형**

| **유형** | **설명** |
| --- | --- |
| 직접 영향 | 코드를 직접 수정 |
| 호출 영향 | 호출자·피호출자 |
| 계약 영향 | Request·Response·Interface |
| 데이터 영향 | Table·Column·Migration |
| 운영 영향 | Timeout·Metric·Runbook |
| 배포 영향 | WAR·공통 JAR·서버 |
| 보안 영향 | 권한·개인정보 |
| 테스트 영향 | 재수행 대상 |
| 호환성 영향 | 구·신 버전 혼재 |

## **186.3 영향도 분석 순서**

변경대상 정의

→ 직접 참조 검색

→ 상위 호출자 추적

→ 하위 의존성 추적

→ 설정·DB·OM 확인

→ 테스트 확인

→ 배포·호환성 확인

→ 위험 등급화

→ 검증계획 작성

## **186.4 변경대상 명확화**

나쁜 예:

고객조회 수정

좋은 예:

SV.Customer.selectSummary 응답에
customerGradeCode 필드를 추가한다.

변경대상이 구체적이어야 영향분석도 정확해집니다.

## **186.5 영향범위 매트릭스**

| **영역** | **영향 대상** | **변경 필요** |
| --- | --- | --- |
| 요구사항 | REQ-SV-0010 | Y |
| 화면 | SV-CUS-0001 | Y |
| ServiceId | 기존 유지 | N |
| Response DTO | 필드 추가 | Y |
| Service | 값 조회 | Y |
| SQL | 컬럼 추가 | Y |
| DB | 기존 컬럼 사용 | N |
| 연계 | 없음 | N |
| 테스트 | E2E·Contract | Y |
| OM | 응답 크기 | 검토 |

## **186.6 위험등급**

### **Low**

내부 메서드명 변경

로그 메시지 변경

기존 계약 미변경

### **Medium**

업무 Rule 변경

SQL 조건 변경

Timeout 조정

### **High**

Response 계약 변경

DB Schema 변경

권한 변경

트랜잭션 변경

### **Critical**

공통 TCF Pipeline 변경

JWT 검증 변경

데이터 소유권 변경

전체 WAR 공통 JAR 변경

## **186.7 영향도 점수 예**

| **평가항목** | **1점** | **3점** | **5점** |
| --- | --- | --- | --- |
| 호출자 수 | 1개 | 2~5개 | 6개 이상 |
| 데이터 변경 | 없음 | 컬럼 | Table·Migration |
| 배포범위 | 1 WAR | 여러 WAR | 전체 |
| 보안 | 없음 | 권한 | 인증·Key |
| Rollback | 쉬움 | 조건부 | 어려움 |

점수는 판단 보조도구이며 최종 위험평가를 대신하지 않습니다.

## **186.8 변경검증 계획**

Unit Test

Mapper Test

Contract Test

E2E Test

Regression Test

Performance Test

Security Test

Migration Test

Rollback Test

변경 위험에 따라 범위를 결정합니다.

## **186.9 영향도 분석 완료조건**

직접 수정대상 확인

상위·하위 영향 확인

DB·설정·OM 확인

테스트 대상 확인

배포범위 확인

Rollback 확인

잔여 위험 기록

## **제186장 요약**

학습 목표 | 186장 요약의 핵심 개념을 설명하고, 프로젝트에서 적용 위치와 검증 방법을 스스로 판단한다.

핵심 관점 | 책임과 흐름: 이 주제는 개별 기술보다 입력, 처리 책임, 출력과 다음 단계의 연결 관계를 먼저 파악해야 합니다.

읽는 순서 | 먼저 용어와 책임 경계를 확인하고, 이어지는 정상 예시와 금지 예시를 비교한 뒤 실제 설정·코드·로그에서 근거를 찾아봅니다.

영향도 분석은 수정파일뿐 아니라 계약·데이터·운영·배포까지 포함한다.

변경대상을 구체적으로 정의하고 정방향·역방향 추적을 함께 수행한다.

위험등급에 따라 검증범위를 결정한다.

# **제187장. 대표 변경유형별 영향분석**

학습 목표 | 187장. 대표 변경유형별 영향분석의 핵심 개념을 설명하고, 프로젝트에서 적용 위치와 검증 방법을 스스로 판단한다.

핵심 관점 | 책임과 흐름: 이 주제는 개별 기술보다 입력, 처리 책임, 출력과 다음 단계의 연결 관계를 먼저 파악해야 합니다.

읽는 순서 | 먼저 용어와 책임 경계를 확인하고, 이어지는 정상 예시와 금지 예시를 비교한 뒤 실제 설정·코드·로그에서 근거를 찾아봅니다.

## **187.1 Request 필드 추가**

예:

contactPriorityCode 추가

확인:

화면 입력

Request DTO

Validation

Command

Service Rule

Mapper Parameter

DB Column

감사·로그

테스트

선택필드 추가는 비교적 하위 호환적일 수 있지만 서버 기본값을 명확히 합니다.

## **187.2 Response 필드 추가**

확인:

SQL 출력

Data DTO

Response Mapper

Response DTO

화면 Binding

파일 Export

외부 소비자

응답 크기

알 수 없는 필드를 거부하는 소비자가 있다면 호환성 문제가 생길 수 있습니다.

## **187.3 Response 필드 삭제·이름 변경**

고위험 변경입니다.

호출 화면

타 업무 Client

외부 시스템

테스트

문서

Rolling 배포

신규 필드 추가 후 병행기간을 두는 방식을 권장합니다.

## **187.4 ServiceId 변경**

영향:

화면 이벤트

타 업무 Client

Handler Registry

OM Catalog

권한

Timeout

거래코드

Metric·Alert

Runbook

Test

ServiceId는 단순 문자열이 아니라 운영 계약입니다.

## **187.5 업무 Rule 변경**

예:

조회기간 최대 3개월
→ 1년

영향:

성능

SQL 실행계획

DB Pool

응답 크기

UI Paging

Timeout

NFR

운영경보

## **187.6 SQL 조건 변경**

예:

STATUS\_CD 조건 제거

확인:

데이터 의미

조회건수

Index

보안·권한

화면 표시

Cache

Test

## **187.7 컬럼 길이 변경**

예:

CONTACT\_TITLE
100 → 200

영향:

DB Column

화면 Max Length

DTO Validation

파일 형식

외부 전문

Index 길이

History

테스트 데이터

## **187.8 Table 분할**

예:

CT\_CONTACT\_MASTER

→ CT\_CONTACT\_MASTER
→ CT\_CONTACT\_CONTENT

영향:

SQL Join

Transaction

조회성능

Mapper

Migration

History

Backup·Recovery

배포순서

## **187.9 Timeout 변경**

ServiceId Timeout

Gateway Timeout

Client Timeout

SQL Timeout

Connection Timeout

경보 임계값

을 함께 확인합니다.

## **187.10 공통 JAR 변경**

tcf-core 변경 시:

전체 업무 WAR Build

Handler 호환성

STF·ETF Regression

성능

ThreadLocal

배포순서

Rollback

대표 업무만 테스트하고 전체 영향이 없다고 판단하지 않습니다.

## **187.11 인증 Claim 변경**

예:

branchId Claim 이름 변경

영향:

Token Issuer

JWKS 검증

Gateway Header

AuthenticationContext

업무 데이터권한

테스트

구 Token 호환성

## **187.12 Batch Schedule 변경**

영향:

온라인 피크시간

선행·후행 Job

DB Pool

파일 생성시각

운영인력

재처리

SLA

## **제187장 요약**

학습 목표 | 187장 요약의 핵심 개념을 설명하고, 프로젝트에서 적용 위치와 검증 방법을 스스로 판단한다.

핵심 관점 | 책임과 흐름: 이 주제는 개별 기술보다 입력, 처리 책임, 출력과 다음 단계의 연결 관계를 먼저 파악해야 합니다.

읽는 순서 | 먼저 용어와 책임 경계를 확인하고, 이어지는 정상 예시와 금지 예시를 비교한 뒤 실제 설정·코드·로그에서 근거를 찾아봅니다.

필드·ServiceId·SQL·DB·Timeout·공통모듈은
각기 다른 영향경로를 가진다.

작아 보이는 변경도 운영 계약과 성능에 영향을 줄 수 있다.

# **제188장. 미사용 코드·중복 구현·기술부채 분석**

학습 목표 | 188장. 미사용 코드·중복 구현·기술부채 분석의 핵심 개념을 설명하고, 프로젝트에서 적용 위치와 검증 방법을 스스로 판단한다.

핵심 관점 | 식별 가능성과 일관성: 이름은 단순 표기가 아니라 소스, 거래, 로그, 운영 설정과 산출물을 연결하는 공통 키입니다.

읽는 순서 | 먼저 용어와 책임 경계를 확인하고, 이어지는 정상 예시와 금지 예시를 비교한 뒤 실제 설정·코드·로그에서 근거를 찾아봅니다.

## **188.1 미사용 코드 판단 주의**

IDE에서 Reference가 없다고 바로 삭제하지 않습니다.

다음 호출방식이 있을 수 있습니다.

Reflection

Spring Bean

Mapper XML

Scheduler

Batch Registry

설정 기반 Class명

외부 호출

운영 수동 기능

## **188.2 미사용 ServiceId 판단**

확인:

Handler Registry

OM Catalog

최근 호출량

화면 이벤트

타 업무 Client

Batch

문서

Release 이력

## **188.3 미사용 Mapper 판단**

Java 호출 없음

XML Include에서 사용

Procedure 호출

Batch Script

운영 Query

를 확인합니다.

## **188.4 중복 구현 유형**

같은 업무 Rule이 여러 Service에 존재

JWT 검증을 업무 WAR마다 별도 구현

같은 SQL이 여러 Mapper에 복사

같은 DTO가 이름만 다르게 존재

공통코드 조회가 업무마다 반복

## **188.5 중복의 원인**

업무 경계 불명확

공통모듈 부재

과거 복사 개발

긴급 대응

팀 간 정보 공유 부족

Legacy 병행

## **188.6 공통화 판단**

다음 조건을 확인합니다.

의미가 정말 같은가?

변경주기가 같은가?

소유자가 같은가?

보안·운영정책이 같은가?

공통화로 결합이 증가하지 않는가?

코드가 유사하다는 이유만으로 공통화하지 않습니다.

## **188.7 기술부채 기록**

| **항목** | **내용** |
| --- | --- |
| Debt ID | 식별자 |
| 위치 | Module·Class |
| 문제 | 구조적 문제 |
| 영향 | 장애·변경 |
| 원인 | 발생 배경 |
| 임시조치 | 현재 통제 |
| 개선안 | 목표 구조 |
| 우선순위 | 위험도 |
| 담당자 | 소유자 |
| 기한 | 계획 |

## **188.8 주석 처리된 코드**

// oldService.process();
// legacyMapper.select();

Git 이력이 있으므로 오래된 코드를 주석으로 보관하지 않습니다.

삭제 여부를 검토하고 필요하면 Git에서 확인합니다.

## **188.9 TODO와 FIXME**

검색:

TODO

FIXME

TEMP

HACK

임시

추후

각 항목이 공식 기술부채 Registry와 연결되는지 확인합니다.

## **188.10 삭제 절차**

호출자 조사

→ 운영 호출량 확인

→ Deprecated

→ 대체 기능 안내

→ 호출량 0

→ 삭제

→ Regression Test

→ OM·문서 폐기

## **제188장 요약**

학습 목표 | 188장 요약의 핵심 개념을 설명하고, 프로젝트에서 적용 위치와 검증 방법을 스스로 판단한다.

핵심 관점 | 책임과 흐름: 이 주제는 개별 기술보다 입력, 처리 책임, 출력과 다음 단계의 연결 관계를 먼저 파악해야 합니다.

읽는 순서 | 먼저 용어와 책임 경계를 확인하고, 이어지는 정상 예시와 금지 예시를 비교한 뒤 실제 설정·코드·로그에서 근거를 찾아봅니다.

Reference가 없다는 이유만으로 코드를 즉시 삭제하지 않는다.

Reflection·설정·Batch·외부 호출을 확인한다.

중복 코드는 의미와 소유권이 같은 경우에만 공통화를 검토한다.

# **제189장. 소스 분석 산출물 작성**

학습 목표 | 189장. 소스 분석 산출물 작성의 핵심 개념을 설명하고, 프로젝트에서 적용 위치와 검증 방법을 스스로 판단한다.

핵심 관점 | 근거 기반 의사결정: 설계안은 장점만 설명하지 말고 제약, 대안, 비용, 위험과 결정 근거를 함께 기록해야 합니다.

읽는 순서 | 먼저 용어와 책임 경계를 확인하고, 이어지는 정상 예시와 금지 예시를 비교한 뒤 실제 설정·코드·로그에서 근거를 찾아봅니다.

## **189.1 분석결과는 문서화해야 한다**

분석자의 머릿속에만 있는 정보는 프로젝트 자산이 아닙니다.

필수 산출물:

소스 분석 개요

모듈 구조도

ServiceId 추적표

프로그램 호출관계

SQL·DB 매핑

설정·Bean 매핑

영향도 분석표

위험·기술부채

검증결과

## **189.2 분석 개요**

| **항목** | **내용** |
| --- | --- |
| 분석 목적 | 고객조회 변경 영향 |
| 대상 Branch | develop |
| 기준 Commit | Hash |
| 대상 모듈 | sv-service |
| 대상 ServiceId | SV.Customer.selectSummary |
| 분석범위 | Handler~DB·OM |
| 제외범위 | UI 내부 렌더링 |
| 분석방법 | 정적·동적 |
| 분석일 | 날짜 |
| 분석자 | 담당자 |

## **189.3 프로그램 추적표**

| **순서** | **계층** | **Class·Method** | **책임** |
| --- | --- | --- | --- |
| 1 | Handler | SvCustomerHandler | ServiceId 분기 |
| 2 | Facade | selectSummary | Transaction |
| 3 | Service | selectSummary | 업무 조합 |
| 4 | Client | IcCustomerClient | 고객 기본정보 |
| 5 | DAO | selectSummary | DB 조회 |
| 6 | Mapper | SV-CUST-SEL-001 | 요약 SQL |

## **189.4 설정 매핑표**

| **설정 Key** | **기본값** | **Runtime 값** | **사용 Class** |
| --- | --- | --- | --- |
| tcf.timeout | 3000 | 5000 | Timeout Executor |
| clients.ic.read-timeout | 700 | 700 | IC Client |
| spring.datasource.hikari.maximum-pool-size | 120 | 120 | Hikari |

## **189.5 위험 목록**

| **위험** | **영향** | **등급** | **조치** |
| --- | --- | --- | --- |
| Request userId 사용 | 위변조 | Critical | Context 전환 |
| SQL 최대건수 없음 | 성능 | High | Paging |
| History 예외 무시 | 감사 | High | Rollback |
| 테스트 비활성 | 품질 | Medium | 복구 |

## **189.6 분석결과와 사실 구분**

문장 예:

확인됨:
CtContactFacade.create에 @Transactional이 선언되어 있다.

추가 검증 필요:
동일 Class 내부호출 여부로 인해 실제 Proxy 적용 여부를
통합 테스트에서 확인해야 한다.

## **189.7 분석 완료 기준**

ServiceId의 Runtime Handler 확인

정상·오류 흐름 확인

Transaction·Timeout 확인

SQL·Table 확인

화면·호출자 확인

Test 확인

OM 정책 확인

변경 영향도 작성

미확인 항목 명시

## **제189장 요약**

학습 목표 | 189장 요약의 핵심 개념을 설명하고, 프로젝트에서 적용 위치와 검증 방법을 스스로 판단한다.

핵심 관점 | 책임과 흐름: 이 주제는 개별 기술보다 입력, 처리 책임, 출력과 다음 단계의 연결 관계를 먼저 파악해야 합니다.

읽는 순서 | 먼저 용어와 책임 경계를 확인하고, 이어지는 정상 예시와 금지 예시를 비교한 뒤 실제 설정·코드·로그에서 근거를 찾아봅니다.

소스 분석결과는 호출관계·설정·SQL·위험을 표준 형식으로 남긴다.

확인된 사실과 추가 검증이 필요한 추정을 구분한다.

분석 기준 Branch와 Commit을 반드시 기록한다.

# **제190장. AI 개발도구를 이용한 소스 분석**

학습 목표 | 190장. AI 개발도구를 이용한 소스 분석의 핵심 개념을 설명하고, 프로젝트에서 적용 위치와 검증 방법을 스스로 판단한다.

핵심 관점 | 재현 가능한 개발환경: 개인 PC에서만 되는 설정을 피하고 버전, 의존성, 실행 옵션을 팀이 재현할 수 있게 고정해야 합니다.

읽는 순서 | 먼저 용어와 책임 경계를 확인하고, 이어지는 정상 예시와 금지 예시를 비교한 뒤 실제 설정·코드·로그에서 근거를 찾아봅니다.

## **190.1 AI가 유용한 영역**

모듈 목록 정리

Class·Method 추출

ServiceId 검색

호출관계 초안

Mapper·Table 매핑

중복 코드 후보

테스트 누락 후보

문서 초안

## **190.2 AI에 제공할 분석범위**

나쁜 요청:

이 소스를 분석해 줘.

좋은 요청:

대상 모듈은 ct-service다.

CT.Contact.create ServiceId를 기준으로
Handler부터 Mapper와 Table까지 추적한다.

Transaction·Timeout·권한·Idempotency 적용위치를 확인한다.

분석결과는 확인된 사실과 추정사항을 구분한다.

## **190.3 AI가 잘못 판단할 수 있는 부분**

Runtime Bean 선택

Profile Override

Reflection 호출

실제 배포 WAR

DB View 내부 Table

운영 OM 설정

실제 호출량

Transaction Proxy

외부 시스템 결과

## **190.4 AI 결과 검증**

실제 Source Line

Build 결과

Spring Bean

통합 테스트

Runtime Log

SQL 실행

DB Metadata

OM Catalog

로 검증합니다.

## **190.5 AI가 생성한 호출관계 주의**

AI가 Class명과 Method명을 보고 호출관계를 추정할 수 있습니다.

반드시 다음을 확인합니다.

실제 Method Invocation

Dependency Injection

Interface 구현체

Profile

Conditional Bean

Mock과 운영 구현 구분

## **190.6 AI 분석 프롬프트 예**

NSIGHT TCF 구조를 기준으로 다음 기능을 분석한다.

대상:
\- Module: ct-service
\- ServiceId: CT.Contact.update
\- Branch: develop
\- Commit: <hash>

분석:
1\. Handler 등록위치
2\. Facade·Service·Rule·DAO·Mapper 호출경로
3\. Request·Response·Command·Data DTO
4\. Transaction 경계와 Rollback
5\. 권한과 데이터권한
6\. Timeout과 Idempotency
7\. SQL ID와 대상 Table
8\. Test 목록
9\. OM 등록정보
10\. 변경 위험

확인되지 않은 내용은 추정으로 표시한다.

## **190.7 AI 사용 금지 방식**

분석결과를 검증 없이 공식 설계로 확정

운영 Secret을 AI 입력에 포함

개인정보가 포함된 로그 제공

전체 Source를 범위 없이 분석

AI가 없다고 한 호출자를 실제로 없다고 확정

## **제190장 요약**

학습 목표 | 190장 요약의 핵심 개념을 설명하고, 프로젝트에서 적용 위치와 검증 방법을 스스로 판단한다.

핵심 관점 | 책임과 흐름: 이 주제는 개별 기술보다 입력, 처리 책임, 출력과 다음 단계의 연결 관계를 먼저 파악해야 합니다.

읽는 순서 | 먼저 용어와 책임 경계를 확인하고, 이어지는 정상 예시와 금지 예시를 비교한 뒤 실제 설정·코드·로그에서 근거를 찾아봅니다.

AI는 대규모 Source의 검색과 구조화에 유용하다.

그러나 Runtime Bean·설정·운영 호출량은 별도로 검증해야 한다.

AI 분석결과는 증적과 확실성 수준을 포함해야 한다.

# **제191장. 종합 실습 — 기존 고객요약 조회 분석**

학습 목표 | 191장. 종합 실습 — 기존 고객요약 조회 분석의 핵심 개념을 설명하고, 프로젝트에서 적용 위치와 검증 방법을 스스로 판단한다.

핵심 관점 | 데이터 일관성: 화면 동작보다 데이터 소유권, 상태 전이, 동시성, 트랜잭션 경계를 먼저 결정해야 합니다.

읽는 순서 | 먼저 용어와 책임 경계를 확인하고, 이어지는 정상 예시와 금지 예시를 비교한 뒤 실제 설정·코드·로그에서 근거를 찾아봅니다.

## **191.1 분석 요청**

고객요약 조회가 어떤 프로그램과 SQL을 사용하는지 분석하고,
응답에 고객등급코드를 추가할 때 영향범위를 작성한다.

## **191.2 기준정보**

| **항목** | **값** |
| --- | --- |
| 업무 | SV |
| 화면 | SV-CUS-0001 |
| ServiceId | SV.Customer.selectSummary |
| 거래코드 | SV-INQ-0001 |
| 모듈 | sv-service |
| 대상 변경 | customerGradeCode 추가 |

## **191.3 ServiceId 검색**

검색결과:

SvCustomerServiceIds

SvCustomerHandler

OM Service Catalog SQL

E2E Test

화면 이벤트 정의

## **191.4 정방향 추적**

SV.Customer.selectSummary

→ SvCustomerHandler

→ SvCustomerFacade.selectSummary

→ SvCustomerService.selectSummary

→ IcCustomerClient.selectBasic

→ SvCustomerDao.selectCustomerSummary

→ SvCustomerMapper.selectCustomerSummary

→ SV-CUST-SEL-001

→ SV\_CUSTOMER\_SUMMARY

## **191.5 공통통제 확인**

| **항목** | **결과** |
| --- | --- |
| 인증 | JWT |
| 기능권한 | SV\_CUSTOMER\_VIEW |
| 데이터권한 | 지점·담당자 |
| Timeout | 3,000ms |
| SQL Timeout | 2,000ms |
| 감사 | 개인정보 조회 |
| Cache | 고객요약 5분 |
| Idempotency | 조회라 미적용 |

## **191.6 변경대상 확인**

customerGradeCode의 원천:

IC 고객기본정보 응답
또는
SV\_CUSTOMER\_SUMMARY 컬럼

두 대안을 비교해야 합니다.

### **대안 1: IC Client 응답 사용**

장점:

고객등급 소유 업무 준수

단점:

외부 호출 의존

응답시간 증가 가능

### **대안 2: SV 요약 테이블 사용**

장점:

빠른 단건조회

단점:

데이터 동기화와 최신성 관리 필요

## **191.7 영향도**

| **영역** | **영향** |
| --- | --- |
| 요구사항 | 고객등급 표시 추가 |
| 화면 | Label·Binding |
| ServiceId | 유지 |
| Response DTO | 필드 추가 |
| Service | 데이터 조합 |
| Client | IC 응답 사용 시 변경 |
| SQL | SV Table 사용 시 컬럼 추가 |
| Cache | Value Version 변경 |
| 테스트 | Unit·Contract·E2E |
| 운영 | 외부 호출시간·응답 크기 |
| 문서 | 화면·거래·프로그램 설계 |

## **191.8 위험**

구·신 응답 DTO 호환성

Cache에 과거 객체가 남는 문제

IC 장애 시 고객등급 누락

등급 데이터의 기준시점 불일치

화면이 Null을 처리하지 못하는 문제

## **191.9 검증계획**

Service Unit Test

IC Client Contract Test

Mapper Test

Cache Version Test

화면 E2E

피크 성능시험

IC Timeout 시 부분 응답시험

Rolling 배포 호환성시험

## **191.10 분석결론 예**

고객등급의 공식 소유 업무가 IC라면
SV가 IC.Customer.selectBasic 계약을 통해 등급을 조회하는 것이 원칙이다.

다만 고객요약 조회의 p95 목표와 IC 호출비용을 고려하여
SV Read Model에 복제하는 대안을 함께 검토해야 한다.

최종 선택은 데이터 최신성 SLA와 성능시험 결과를 기준으로
ADR로 결정한다.

## **제191장 요약**

학습 목표 | 191장 요약의 핵심 개념을 설명하고, 프로젝트에서 적용 위치와 검증 방법을 스스로 판단한다.

핵심 관점 | 책임과 흐름: 이 주제는 개별 기술보다 입력, 처리 책임, 출력과 다음 단계의 연결 관계를 먼저 파악해야 합니다.

읽는 순서 | 먼저 용어와 책임 경계를 확인하고, 이어지는 정상 예시와 금지 예시를 비교한 뒤 실제 설정·코드·로그에서 근거를 찾아봅니다.

기존 기능 변경은 Source 수정 위치만 찾는 것으로 끝나지 않는다.

데이터 소유권·성능·Cache·호환성·운영 영향까지 비교해야 한다.

중요한 변경은 대안과 검증계획을 함께 작성한다.

# **3\. 문제 정의 및 설계 배경**

기존 프로그램 분석이 어려운 근본 이유는 다음과 같습니다.

문서와 Source가 일치하지 않는다.

공식 ServiceId 관리가 부족하다.

업무 간 직접 DB 접근이 존재한다.

Spring Runtime 선택조건이 복잡하다.

Legacy와 신규 구조가 함께 존재한다.

테스트와 운영정보가 분리되어 있다.

변경 이력이 Class 단위로만 남아 있다.

따라서 목표 분석체계는 다음 조건을 만족해야 합니다.

ServiceId 중심

정방향·역방향 추적

Source·DB·OM 통합

정적·동적 검증

변경 영향도 자동화

확실성 수준 표시

분석결과 자산화

# **4\. 현행 구조와 문제점**

| **현행 문제** | **영향** |
| --- | --- |
| 파일명 중심 분석 | 실제 Runtime과 차이 |
| 문서만 신뢰 | Source 불일치 |
| Source만 신뢰 | OM·설정 누락 |
| Table명 검색만 수행 | View·Procedure 누락 |
| Reference 0 즉시 삭제 | 동적 호출 장애 |
| Annotation만 확인 | Proxy 미적용 가능 |
| 영향도 파일 수로 판단 | 계약·운영 누락 |
| AI 결과 무검증 | 잘못된 결론 |

# **5\. 요구사항과 제약조건**

## **5.1 분석 요구사항**

대상 기능을 30분 이내에 식별할 수 있어야 한다.

ServiceId에서 SQL과 Table까지 추적할 수 있어야 한다.

Table에서 화면과 호출자까지 역추적할 수 있어야 한다.

Transaction·Timeout·권한을 확인할 수 있어야 한다.

변경 영향도와 재검증 범위를 제시할 수 있어야 한다.

## **5.2 제약조건**

Legacy 코드가 일부 존재한다.

문서와 Source가 완전히 일치하지 않을 수 있다.

운영 DB와 운영 로그에 직접 접근할 수 없을 수 있다.

Runtime Profile이 환경마다 다를 수 있다.

Reflection과 설정 기반 호출이 존재할 수 있다.

# **6\. 설계 원칙**

식별자 우선

실행경로 우선

소유권 우선

Runtime 검증

증적 기반

최소 범위 분석

양방향 추적

변경 전 검증계획

불확실성 명시

# **7\. 대안 비교 및 의사결정**

## **7.1 분석 방식 비교**

| **방식** | **장점** | **단점** |
| --- | --- | --- |
| 파일 전체 읽기 | 전체 이해 | 시간 과다 |
| 클래스명 검색 | 빠름 | 동적 연결 누락 |
| ServiceId 추적 | 실행 중심 | ServiceId 필요 |
| Table 역추적 | DB 변경에 유리 | 동적 SQL 주의 |
| Runtime Trace | 실제 동작 | 실행환경 필요 |
| 자동 Call Graph | 대규모 분석 | 오탐·누락 |
| 통합 방식 | 정확도 높음 | 초기 비용 |

권장:

ServiceId 기반 정적분석

\+ Runtime Trace

\+ DB·OM Metadata

\+ 자동검증

# **8\. 목표 분석 아키텍처**

\[분석 요청\]
│
▼
\[기준 식별자 확보\]
화면 ID·ServiceId·SQL ID·Table·오류코드
│
┌───────────┴───────────┐
▼ ▼
\[정방향 분석\] \[역방향 분석\]
ServiceId→Handler→SQL Table→SQL→ServiceId
│ │
└───────────┬───────────┘
▼
\[Runtime 검증\]
Bean·Profile·Trace·Transaction·SQL
│
▼
\[공통통제 확인\]
인증·권한·Timeout·Idempotency·감사
│
▼
\[영향도 분석\]
Requirement·UI·Code·DB·Test·OM·Release
│
▼
\[분석 산출물\]
호출관계·위험·검증계획·미확인 항목

# **9\. 표준 형식**

## **9.1 소스 분석 요청서**

| **항목** | **내용** |
| --- | --- |
| 분석 ID | ANA-SV-2026-001 |
| 목적 | 고객요약 변경 |
| 기준 Branch | develop |
| 기준 Commit | Hash |
| 대상 ServiceId | SV.Customer.selectSummary |
| 시작점 | ServiceId |
| 분석범위 | UI~DB·OM |
| 제외범위 | 외부 IC 내부 구현 |
| 요청자 | 업무팀 |
| 기한 | 일정 |

## **9.2 소스 추적표**

| **순서** | **식별자** | **Class·Method** | **비고** |
| --- | --- | --- | --- |
| 1 | ServiceId | SV.Customer.selectSummary | 진입 |
| 2 | Handler | SvCustomerHandler | 분기 |
| 3 | Facade | selectSummary | Tx |
| 4 | Service | selectSummary | 조합 |
| 5 | DAO | selectSummary | 조회 |
| 6 | SQL | SV-CUST-SEL-001 | SQL |
| 7 | Table | SV\_CUSTOMER\_SUMMARY | Data |

## **9.3 영향도 기록**

| **영역** | **대상** | **영향** | **조치** |
| --- | --- | --- | --- |
| UI | SV-CUS-0001 | 필드 추가 | Binding |
| API | Response DTO | 계약 변경 | 하위 호환 |
| DB | 없음 | 없음 | \- |
| Cache | 고객요약 | Value 변경 | Version |
| Test | E2E | 재시험 | 필수 |
| OM | 응답 크기 | 관찰 | Metric |

# **10\. 구성요소 및 속성**

| **구성요소** | **주요 속성** |
| --- | --- |
| Repository Map | Module·Dependency |
| Service Registry | ServiceId·Handler |
| Program Graph | Class·Method |
| SQL Registry | Mapper·Table |
| Configuration Map | Key·Bean |
| Security Map | 권한·Context |
| Runtime Trace | 실제 실행 |
| Test Map | 기대동작 |
| Impact Matrix | 변경 영향 |
| Risk Registry | 위험·조치 |
| Analysis Report | 증적·결론 |

# **11\. 책임 경계와 RACI**

| **활동** | **업무 DEV** | **모듈리더** | **AA** | **FW** | **DBA** | **QA** | **OPS** |
| --- | --- | --- | --- | --- | --- | --- | --- |
| 분석범위 정의 | R | A | C | I | I | C | I |
| ServiceId 추적 | R | A/C | C | C | I | C | I |
| 공통 Framework 분석 | C | C | C | A/R | I | C | C |
| SQL·DB 분석 | R | C | C | I | A/R | C | C |
| Runtime 검증 | R | A/C | C | C | C | C | C |
| 보안·Timeout 분석 | R | C | A/C | C | I | C | C |
| 영향도 산정 | R | A | C | C | C | C | C |
| 테스트 범위 | R | C | C | C | C | A/R | I |
| 운영 영향 | C | C | C | C | C | C | A/R |
| 최종 승인 | C | A | A/C | C | C | C | C |

# **12\. 정상 처리 흐름**

1\. 분석 목적과 변경대상을 구체화한다.

2\. Branch와 Commit을 고정한다.

3\. 저장소와 대상 모듈을 확인한다.

4\. 화면·ServiceId·SQL ID 중 시작점을 선택한다.

5\. Handler부터 Mapper까지 정방향 추적한다.

6\. Table과 Column에서 호출자까지 역방향 추적한다.

7\. Bean·Profile·설정값을 확인한다.

8\. Transaction·Timeout·보안을 확인한다.

9\. 테스트와 Runtime Trace로 검증한다.

10\. 직접·간접 영향범위를 작성한다.

11\. 위험등급과 재검증 계획을 작성한다.

12\. 분석결과와 미확인 사항을 검토·승인한다.

# **13\. 오류·Timeout·장애 흐름**

## **13.1 ServiceId를 찾지 못한 경우**

화면 호출 Source 확인

→ 거래코드 검색

→ Handler Registry 확인

→ OM Catalog 확인

→ 다른 Branch·Legacy 확인

## **13.2 Mapper를 찾지 못한 경우**

DAO 호출 확인

→ JPA·Procedure 여부

→ 동적 SQL 여부

→ 다른 DataSource Module 확인

## **13.3 Runtime이 Source와 다른 경우**

배포 WAR Version 확인

→ Profile 확인

→ Bean 조건 확인

→ 설정 Override 확인

→ 기준 Commit 재설정

## **13.4 영향도 누락 발견**

배포 후 외부 Client 오류

→ 호출자 Registry 보완

→ Contract Test 추가

→ 영향도 Gate 개선

# **14\. 정상 예시**

분석대상
CT.Contact.update

확인된 실행경로
CtContactHandler
→ CtContactFacade.update
→ CtContactService.update
→ CtContactRule.validateUpdatable
→ CtContactDao.updateContact
→ CtContactMapper.updateContact
→ CT-CNT-UPD-001
→ CT\_CONTACT\_MASTER

공통통제
JWT·CT\_CONTACT\_UPDATE·5초 Timeout·Version

직접 영향
Request·Command·Mapper

간접 영향
화면·History·E2E·OM

확실성
Source와 Runtime 모두 확인

# **15\. 금지 예시**

## **15.1 모든 파일을 처음부터 읽기**

분석범위와 우선순위가 없습니다.

## **15.2 Class 이름만으로 실제 구현 확정**

Profile과 Bean 조건을 확인하지 않습니다.

## **15.3 주석을 공식 요구사항으로 간주**

주석이 오래되었을 수 있습니다.

## **15.4 Table명 검색만으로 영향도 확정**

View·Procedure·연계 호출자를 놓칩니다.

## **15.5 Reference 0 코드를 즉시 삭제**

Reflection·설정 호출을 확인하지 않습니다.

## **15.6 Annotation만 보고 Transaction 적용 확정**

Proxy 호출 여부를 확인하지 않습니다.

## **15.7 AI 결과를 검증 없이 제출**

Runtime과 다른 결론을 낼 수 있습니다.

## **15.8 미확인 사항을 숨김**

분석 확실성을 과장합니다.

# **16\. 연계 규칙**

화면 ID
→ 이벤트 ID
→ ServiceId

ServiceId
→ Handler
→ Facade
→ Service

Service
→ DAO·Client

DAO
→ Mapper
→ SQL
→ Table

ServiceId
→ 권한·Timeout·OM

Requirement
→ Test

Commit
→ Build
→ WAR
→ Runtime

# **17\. 데이터 및 상태관리**

## **17.1 분석 상태**

REQUESTED

SCOPED

STATIC\_ANALYZED

RUNTIME\_VERIFIED

IMPACT\_ANALYZED

REVIEWED

APPROVED

CLOSED

## **17.2 확실성 상태**

CONFIRMED

HIGH

MEDIUM

LOW

UNKNOWN

## **17.3 영향 상태**

NO\_IMPACT

DIRECT\_IMPACT

INDIRECT\_IMPACT

REQUIRES\_TEST

REQUIRES\_MIGRATION

REQUIRES\_DECISION

# **18\. 성능·용량·확장성**

대규모 Source 분석에서는 다음을 고려합니다.

Java 파일 수

Module 수

ServiceId 수

Mapper 수

SQL 수

DB 객체 수

검색 Index

Call Graph 생성시간

분석 결과 저장용량

자동분석도구는 다음과 같이 범위를 제한합니다.

대상 Branch

대상 Module

대상 Package

대상 ServiceId

Vendor·Build 제외

Generated Source 구분

# **19\. 보안·개인정보·감사**

Source 분석 중 운영 Secret을 수집하지 않는다.

Log와 SQL Parameter의 개인정보를 마스킹한다.

운영 DB Dump를 개인 PC에 저장하지 않는다.

인증 우회 코드를 분석용으로 운영 Branch에 반영하지 않는다.

분석결과에 취약점이 포함되면 접근권한을 제한한다.

AI 분석에 Private Key·JWT·고객정보를 입력하지 않는다.

영향도 승인과 보안 예외를 감사기록으로 남긴다.

# **20\. 운영·모니터링·장애 대응**

운영 장애에서 소스 분석은 다음 순서로 적용합니다.

오류코드·TraceId

→ ServiceId

→ 배포 WAR Version

→ Handler

→ Service

→ SQL·외부 호출

→ 최근 Commit

→ 변경 영향도

→ 복구·Rollback

운영에서 필요한 분석정보:

Server ID

WAR Version

Git Commit

ServiceId

Mapper·SQL ID

설정 Hash

Profile

최근 Release

# **21\. 자동검증 및 품질 Gate**

| **Gate** | **검증** |
| --- | --- |
| ServiceId | Handler 연결 |
| Handler | Facade 호출 |
| Package | 계층 규칙 |
| Transaction | Proxy·Rollback Test |
| Mapper | Interface·XML 일치 |
| SQL | Table Registry 연결 |
| DB | 사용 ServiceId 연결 |
| Security | Request 사용자정보 금지 |
| Timeout | 상·하위 정합성 |
| Test | Requirement 연결 |
| OM | Catalog·권한·Timeout |
| Impact | 변경 대상별 분석 완료 |
| Dead Code | 호출량·동적참조 확인 |
| Analysis | Branch·Commit·증적 |

# **22\. 테스트 시나리오**

| **ID** | **시나리오** | **기대 결과** |
| --- | --- | --- |
| ANA-001 | ServiceId 검색 | Handler 발견 |
| ANA-002 | Handler 중복 | 기동 실패 |
| ANA-003 | Profile별 Client | 실제 Bean 확인 |
| ANA-004 | Mapper XML 누락 | 연결 실패 탐지 |
| ANA-005 | Table 역추적 | 화면 확인 |
| ANA-006 | View 내부 Table | DB Metadata 확인 |
| ANA-007 | Transaction Self Invocation | Test로 미적용 탐지 |
| ANA-008 | Request userId 사용 | 보안 위반 |
| ANA-009 | Timeout Override | Runtime 값 확인 |
| ANA-010 | Disabled Test | 위험 등록 |
| ANA-011 | Reference 0 Bean | Runtime 등록 확인 |
| ANA-012 | Response 필드 추가 | 호출자 영향 생성 |
| ANA-013 | ServiceId Rename | OM·화면 영향 |
| ANA-014 | SQL 조건 변경 | 성능시험 지정 |
| ANA-015 | DB 컬럼 길이 변경 | UI·파일 영향 |
| ANA-016 | 공통 JAR 변경 | 전체 WAR Regression |
| ANA-017 | JWT Claim 변경 | 인증 영향 |
| ANA-018 | Cache Value 변경 | 호환성 검증 |
| ANA-019 | 배포 WAR 차이 | Commit 재확인 |
| ANA-020 | AI 분석 오탐 | Runtime 검증 |
| ANA-021 | 미사용 ServiceId | 호출량 확인 |
| ANA-022 | Legacy 직접 DB 접근 | 위험 등록 |
| ANA-023 | 외부 Timeout | Thread 영향 |
| ANA-024 | 오류코드 역추적 | Rule 확인 |
| ANA-025 | 종합 영향분석 | 검증계획 완료 |

# **23\. 제18부 체크리스트**

## **23.1 분석 준비**

| **점검 항목** | **확인** |
| --- | --- |
| 분석 목적이 구체적인가? | □ |
| Branch와 Commit을 기록했는가? | □ |
| 대상 Module을 확정했는가? | □ |
| 시작 식별자를 확보했는가? | □ |
| 분석범위와 제외범위가 있는가? | □ |

## **23.2 정방향 분석**

| **점검 항목** | **확인** |
| --- | --- |
| ServiceId를 찾았는가? | □ |
| Runtime Handler를 확인했는가? | □ |
| Facade Transaction을 확인했는가? | □ |
| Service 업무흐름을 정리했는가? | □ |
| Rule과 오류코드를 확인했는가? | □ |
| DAO·Mapper를 연결했는가? | □ |
| SQL과 Table을 확인했는가? | □ |

## **23.3 역방향 분석**

| **점검 항목** | **확인** |
| --- | --- |
| Table 사용 SQL을 찾았는가? | □ |
| View·Procedure를 확인했는가? | □ |
| Mapper 호출자를 찾았는가? | □ |
| 관련 ServiceId를 찾았는가? | □ |
| 화면·Client·Batch 호출자를 확인했는가? | □ |
| 컬럼 사용유형을 구분했는가? | □ |

## **23.4 Runtime·설정**

| **점검 항목** | **확인** |
| --- | --- |
| Active Profile을 확인했는가? | □ |
| Conditional Bean을 확인했는가? | □ |
| 최종 설정값을 확인했는가? | □ |
| 실제 배포 WAR Version을 확인했는가? | □ |
| Runtime Trace로 검증했는가? | □ |
| SQL Parameter와 결과를 확인했는가? | □ |

## **23.5 공통통제**

| **점검 항목** | **확인** |
| --- | --- |
| 인증 적용위치를 확인했는가? | □ |
| 기능권한을 확인했는가? | □ |
| 데이터권한을 확인했는가? | □ |
| Timeout 계층을 확인했는가? | □ |
| Idempotency를 확인했는가? | □ |
| 감사로그를 확인했는가? | □ |
| Rollback을 확인했는가? | □ |

## **23.6 영향도**

| **점검 항목** | **확인** |
| --- | --- |
| 직접 수정대상을 찾았는가? | □ |
| 상위 호출자를 찾았는가? | □ |
| 하위 의존성을 찾았는가? | □ |
| DB·설정·OM 영향을 확인했는가? | □ |
| 테스트 범위를 정했는가? | □ |
| 배포·호환성 영향을 확인했는가? | □ |
| 위험등급과 Rollback을 작성했는가? | □ |

## **23.7 분석결과**

| **점검 항목** | **확인** |
| --- | --- |
| 호출관계표가 있는가? | □ |
| SQL·DB 매핑표가 있는가? | □ |
| 설정·Bean 매핑표가 있는가? | □ |
| 확인된 사실과 추정을 구분했는가? | □ |
| 미확인 항목을 기록했는가? | □ |
| 검토자 승인을 받았는가? | □ |

# **24\. 변경·호환성·폐기 관리**

## **24.1 분석결과 변경**

기준 Commit이 변경되면 분석결과도 갱신해야 합니다.

기존 Commit

→ 신규 Commit Diff

→ 호출관계 변화

→ 영향도 재산정

→ 분석 Version 갱신

## **24.2 Source 이동**

Class가 다른 Package로 이동하면 다음을 확인합니다.

Component Scan

Import

ArchUnit

설계서

로그 Package

Bean 이름

Serialization

## **24.3 ServiceId 폐기**

호출자 조사

→ Deprecated

→ 신규 ServiceId 전환

→ OM 통제

→ 호출량 0

→ Handler 제거

→ 문서·테스트 폐기

## **24.4 Legacy 코드 폐기**

신규 구현과 기능 비교

데이터 결과 비교

운영 호출량 확인

병행기간

Rollback

구 코드 제거

## **24.5 분석도구 변경**

Call Graph나 Source Scanner Version이 바뀌면 동일 저장소를 재분석해 결과 차이를 검증합니다.

# **25\. 시사점**

## **25.1 핵심 아키텍처 판단**

제18부의 핵심은 다음과 같습니다.

소스를 분석한다
\= Java 파일을 많이 읽는다

가 아닙니다.

소스를 분석한다
\= 공식 식별자와 실행경로를 기준으로
필요한 프로그램·데이터·설정·운영 관계를
증적으로 연결한다

입니다.

## **25.2 주요 위험**

| **위험** | **영향** |
| --- | --- |
| 파일 전체 읽기 | 분석시간 증가 |
| Class명만 신뢰 | Runtime 오판 |
| 문서만 신뢰 | Source 불일치 |
| Source만 신뢰 | OM·설정 누락 |
| Table명만 검색 | View·Procedure 누락 |
| Annotation만 확인 | Transaction 오판 |
| Reference 0 삭제 | 동적 호출 장애 |
| Request 사용자정보 사용 | 보안 위험 |
| 영향도 파일 수로 판단 | 계약·운영 누락 |
| AI 결과 무검증 | 잘못된 결론 |
| 기준 Commit 미기록 | 재현 불가 |
| 추정사항 미표시 | 신뢰도 저하 |

## **25.3 우선 보완 과제**

1\. 모듈·Package 전체 지도 작성

2\. ServiceId–Handler Registry 자동추출

3\. Handler–Facade–Service Call Graph

4\. Mapper–SQL–Table 자동매핑

5\. 설정 Key–Bean 매핑

6\. Transaction·Timeout 자동점검

7\. 화면·Client 호출자 Registry

8\. Requirement–Test 추적성 연결

9\. 영향도 분석 Template

10\. Runtime Trace 검증도구

11\. Dead Code 검증절차

12\. 분석결과 Version 관리

## **25.4 중장기 발전 방향**

수동 문자열 검색
→ ServiceId 통합 검색

개별 호출관계 작성
→ 자동 Call Graph

수동 Table 역추적
→ DB Lineage

정적분석 중심
→ Runtime Trace 결합

개발자 경험 기반 영향도
→ Traceability Graph

수동 Dead Code 판단
→ 호출량·Runtime 기반 폐기

일회성 분석문서
→ 지속적 Architecture Registry

AI 단순 요약
→ 증적 기반 분석 Agent

# **26\. 마무리말**

제18부에서 가장 중요하게 기억해야 할 소스 분석 순서는 다음과 같습니다.

분석 목적 정의

→ Branch·Commit 고정

→ 모듈 구조 확인

→ 화면 ID·ServiceId 확보

→ Handler 찾기

→ Facade·Service·Rule 추적

→ DAO·Mapper·SQL 추적

→ Table·Column 확인

→ Bean·Profile·설정 확인

→ Transaction·Timeout·보안 확인

→ Test·Runtime Trace 검증

→ 정방향·역방향 영향분석

→ 위험·검증계획 작성

→ 분석결과 승인

정방향 분석은 다음과 같습니다.

화면

→ 이벤트

→ ServiceId

→ Handler

→ Facade

→ Service

→ Rule

→ DAO

→ Mapper

→ SQL

→ DB

역방향 분석은 다음과 같습니다.

DB Column

→ SQL

→ Mapper

→ DAO

→ Service

→ ServiceId

→ 화면·Client·Batch

→ 요구사항

초보 개발자가 기존 프로그램 분석을 완료하기 전에 마지막으로 확인해야 할 질문은 다음과 같습니다.

어느 Branch와 Commit을 분석했는가?

대상 기능의 공식 ServiceId는 무엇인가?

실제 Runtime Handler는 어느 Class인가?

Facade의 Transaction이 실제로 적용되는가?

Service는 어떤 업무 흐름을 수행하는가?

Rule은 어떤 조건과 오류코드를 사용하는가?

DAO와 Mapper는 어떤 SQL을 호출하는가?

SQL은 어느 Table과 Column을 사용하는가?

다른 업무 DB를 직접 조회하지 않는가?

어떤 Client와 외부 시스템을 호출하는가?

실제 Profile에서 어느 Bean이 선택되는가?

최종 Timeout 값은 어디에서 결정되는가?

사용자와 권한정보는 어느 Context를 사용하는가?

변경 거래에 Idempotency와 History가 있는가?

어떤 테스트가 현재 동작을 검증하는가?

화면·Batch·외부 Client 등 모든 호출자를 확인했는가?

직접 영향과 간접 영향을 구분했는가?

배포·OM·호환성·Rollback 영향까지 검토했는가?

확인된 사실과 추정사항을 구분했는가?

분석결과가 재현 가능한 증적으로 남아 있는가?

이 질문에 답할 수 있다면 처음 보는 대규모 Source에서도 필요한 기능을 빠르게 찾아 실행경로를 설명하고, 변경이 화면·업무·DB·운영에 미치는 영향을 체계적으로 판단할 수 있습니다.
