<!-- source: ztcf-집필본/NSIGHT TCF 개발 입문서 - 제3부 첫 번째 업무 거래 구현하기_재구성확장판_v3.docx -->
<!-- tables: mammoth-html + placeholder table-to-md -->

나는 제3부. 첫 번째 업무 거래 구현하기의 개념을 코드·설정·로그·산출물 중 어디에서 확인할 수 있는가? 문제가 발생했을 때 어느 경계부터 조사해야 하는가?

읽기 전 질문

① 장의 학습 목표를 먼저 읽습니다. ② 기존 설명과 예제를 따라갑니다. ③ 실무 적용 절차를 자신의 프로젝트에 대입합니다. ④ 완료 기준으로 이해 여부를 확인합니다.

권장 학습법

요청부터 데이터 처리, 오류와 트랜잭션까지 한 거래를 안전하게 완성하는 방법을 학습합니다.

이 부의 학습 좌표

RESTRUCTURED & EXPANDED EDITION

2단계 · 거래 구현

**초보 IT 개발자를 위한 NSIGHT TCF 개발 입문서**

# **제3부. 첫 번째 업무 거래 구현하기**

# **1\. 도입 전 안내말**

제1부에서는 TCF의 전체 처리 흐름을 배웠습니다.

화면
→ Gateway
→ 업무 WAR
→ OnlineTransactionController
→ TCF
→ STF
→ Dispatcher
→ Handler
→ Facade
→ Service
→ Rule
→ DAO
→ Mapper
→ ETF
→ 표준 응답

제2부에서는 거래를 식별하기 위한 업무코드, ServiceId, 거래코드, 표준 요청·응답 전문을 배웠습니다.

제3부에서는 실제로 다음 기능을 구현합니다.

고객번호를 입력한다.
→ 고객 요약정보를 조회한다.
→ 고객명과 고객등급을 화면에 반환한다.

구현 대상 거래는 다음과 같습니다.

| **항목** | **정의** |
| --- | --- |
| 업무코드 | SV |
| 업무도메인 | Customer |
| 화면 ID | SV-CUS-0001 |
| 이벤트 ID | SV-CUS-0001-E01 |
| ServiceId | SV.Customer.selectSummary |
| 거래코드 | SV-INQ-0001 |
| 처리유형 | INQUIRY |
| 업무 WAR | sv-service |
| Context Path | /sv |
| 공통 Endpoint | POST /sv/online |
| Timeout | 3000ms |
| 권한코드 | SV\_CUSTOMER\_VIEW |

이번 실습에서는 단순히 Java 클래스만 작성하지 않습니다.

거래 정의
→ 패키지 생성
→ DTO 작성
→ Validation 작성
→ Handler 작성
→ Facade 작성
→ Service 작성
→ Rule 작성
→ DAO 작성
→ Mapper 작성
→ SQL 작성
→ 오류 처리
→ 테스트
→ OM 등록
→ 로그 확인

기업 시스템에서 거래 한 건이 완성되었다는 의미는 다음과 같습니다.

코드가 동작한다.
\+ 표준 구조를 따른다.
\+ 오류가 표준화된다.
\+ 운영자가 거래를 통제할 수 있다.
\+ 로그로 장애를 추적할 수 있다.
\+ 테스트 결과가 존재한다.

# **2\. 제3부 개요**

## **2.1 목적**

제3부의 목적은 초보 개발자가 NSIGHT TCF 표준에 따라 첫 번째 온라인 거래를 직접 구현하도록 돕는 것입니다.

학습을 마친 뒤에는 다음 작업을 수행할 수 있어야 합니다.

1.  업무 WAR와 개발 위치를 찾는다.
2.  ServiceId에 맞는 패키지를 생성한다.
3.  요청·응답 DTO를 작성한다.
4.  입력값 형식 검증과 업무 검증을 구분한다.
5.  도메인 단위 Handler에 ServiceId를 등록한다.
6.  Facade에 트랜잭션 경계를 설정한다.
7.  Service에서 업무 처리 순서를 구현한다.
8.  Rule에서 업무 조건을 검증한다.
9.  DAO와 Mapper를 통해 SQL을 실행한다.
10.  단위·통합·거래 테스트를 작성한다.
11.  OM 기준정보를 등록한다.
12.  TraceId로 전체 실행 로그를 확인한다.

## **2.2 구현 대상 기능**

사용자 요구사항은 다음과 같습니다.

사용자가 고객번호를 입력하고 조회 버튼을 누르면
고객번호, 고객명, 고객등급, 기준일을 반환한다.

정상 결과 예:

{
"customerNo": "CUST000001",
"customerName": "홍길동",
"customerGrade": "VIP",
"baseDate": "20260717"
}

오류 조건은 다음과 같습니다.

| **조건** | **결과** |
| --- | --- |
| 고객번호 미입력 | 필수값 오류 |
| 고객번호 길이 초과 | 형식 오류 |
| 기준일 형식 오류 | 형식 오류 |
| 고객정보 없음 | 업무 오류 |
| 고객조회 권한 없음 | 권한 오류 |
| 거래 중지 상태 | 거래통제 오류 |
| SQL 수행시간 초과 | Timeout |
| DB 연결 실패 | 시스템 오류 |

## **2.3 대상 독자**

-   Java와 Spring 기본 문법을 배운 초보 개발자
-   NSIGHT 업무 WAR에 처음 프로그램을 추가하는 개발자
-   Controller와 Service 구조만 경험한 개발자
-   MyBatis Mapper를 처음 사용하는 개발자
-   ServiceId 기반 거래 개발이 처음인 개발자

## **2.4 선행조건**

| **항목** | **필요한 상태** |
| --- | --- |
| JDK | 프로젝트 표준 버전 설치 |
| Gradle | Wrapper를 이용한 빌드 가능 |
| IDE | IntelliJ IDEA 또는 Eclipse |
| 프로젝트 | 전체 멀티모듈 프로젝트 Import |
| 업무 WAR | sv-service 모듈 확인 |
| 공통 모듈 | tcf-core, tcf-web 의존 확인 |
| DB | 로컬 또는 개발 DB 접속정보 확보 |
| 테이블 | 고객 요약 조회용 테이블 또는 테스트 데이터 |
| 표준 전문 | StandardRequest, StandardResponse 사용 가능 |
| 오류체계 | BusinessException과 공통 오류코드 사용 가능 |

## **2.5 이번 실습에서 지킬 원칙**

업무 Controller를 새로 만들지 않는다.

Handler는 Facade만 호출한다.

트랜잭션은 Facade에 둔다.

Service는 업무 흐름을 조립한다.

Rule은 DB를 직접 호출하지 않는다.

DAO는 Mapper만 호출한다.

Mapper는 SQL 실행만 담당한다.

표준 응답은 ETF가 생성한다.

# **제12장. 개발환경과 프로젝트 구조 확인하기**

학습 목표 | 12장. 개발환경과 프로젝트 구조 확인하기의 핵심 개념을 설명하고, 프로젝트에서 적용 위치와 검증 방법을 스스로 판단한다.

핵심 관점 | 재현 가능한 개발환경: 개인 PC에서만 되는 설정을 피하고 버전, 의존성, 실행 옵션을 팀이 재현할 수 있게 고정해야 합니다.

읽는 순서 | 먼저 용어와 책임 경계를 확인하고, 이어지는 정상 예시와 금지 예시를 비교한 뒤 실제 설정·코드·로그에서 근거를 찾아봅니다.

## **12.1 소스부터 만들지 않는다**

신규 기능을 받으면 곧바로 클래스를 만드는 경우가 많습니다.

CustomerController 생성
CustomerService 생성
CustomerMapper 생성

NSIGHT TCF에서는 먼저 프로젝트 구조를 확인해야 합니다.

확인 순서는 다음과 같습니다.

전체 멀티모듈 확인
→ 업무 WAR 확인
→ 공통 모듈 의존성 확인
→ 기존 유사 거래 확인
→ 패키지 표준 확인
→ Mapper 경로 확인
→ 환경설정 확인
→ 기본 빌드 확인

## **12.2 Gradle 멀티모듈 확인**

터미널에서 다음 명령을 실행합니다.

Windows:

gradlew.bat projects

Linux 또는 macOS:

./gradlew projects

예상 구조 예:

Root project 'nsight-tcf-framework'
+--- Project ':tcf-core'
+--- Project ':tcf-web'
+--- Project ':tcf-util'
+--- Project ':tcf-eai'
+--- Project ':tcf-om'
+--- Project ':sv-service'
+--- Project ':ic-service'
+--- Project ':cm-service'

고객 요약 조회 거래는 SV 업무에 속하므로 sv-service에 작성합니다.

## **12.3 업무 WAR의 의존성 확인**

sv-service/build.gradle 또는 build.gradle.kts를 확인합니다.

개념적인 의존관계는 다음과 같습니다.

dependencies {
implementation project(':tcf-core')
implementation project(':tcf-web')
implementation project(':tcf-util')

implementation 'org.mybatis.spring.boot:mybatis-spring-boot-starter'
implementation 'org.springframework.boot:spring-boot-starter-validation'

testImplementation 'org.springframework.boot:spring-boot-starter-test'
}

프로젝트에서 이미 공통 의존성을 관리한다면 업무 모듈에 같은 라이브러리를 중복 선언하지 않습니다.

## **12.4 패키지 기준 확인**

업무 프로그램의 권장 패키지는 다음과 같습니다.

com.nh.nsight.marketing.{업무코드}.{도메인}.{계층}

이번 거래의 기본 패키지는 다음과 같습니다.

com.nh.nsight.marketing.sv.customer

하위 구조:

com.nh.nsight.marketing.sv.customer
├─ handler
├─ facade
├─ service
├─ rule
├─ dao
├─ mapper
├─ dto
│ ├─ request
│ ├─ response
│ └─ data
└─ exception

실제 디렉터리 예:

sv-service
└─ src
├─ main
│ ├─ java
│ │ └─ com/nh/nsight/marketing/sv/customer
│ │ ├─ handler
│ │ ├─ facade
│ │ ├─ service
│ │ ├─ rule
│ │ ├─ dao
│ │ ├─ mapper
│ │ └─ dto
│ └─ resources
│ └─ mapper
│ └─ sv
│ └─ customer
└─ test
└─ java

## **12.5 기존 유사 거래 찾기**

새 거래를 만들기 전에 기존 거래를 검색합니다.

검색어 예:

implements TransactionHandler
serviceIds()
StandardRequest
TransactionContext
BusinessException
@Mapper

기존 고객 조회 또는 목록 조회 거래가 있다면 다음을 확인합니다.

| **확인 대상** | **확인 내용** |
| --- | --- |
| Handler | 여러 ServiceId 등록 방식 |
| Facade | @Transactional 위치 |
| Service | Context 전달 방식 |
| Rule | 오류코드 사용 방식 |
| DAO | Mapper 예외 처리 방식 |
| Mapper | Namespace와 SQL ID |
| DTO | Record 또는 Class 사용 기준 |
| 테스트 | 공통 테스트 Base Class |

실제 프로젝트의 공통 인터페이스와 메서드 서명은 기존 구현을 우선합니다.

이 책의 코드는 구조 이해를 위한 표준 예시이며, 프로젝트의 실제 공통 클래스명과 메서드 인자는 다를 수 있습니다.

## **12.6 기본 빌드 확인**

신규 코드를 작성하기 전에 현재 프로젝트가 정상 빌드되는지 확인합니다.

Windows:

gradlew.bat clean test

Linux 또는 macOS:

./gradlew clean test

특정 모듈만 확인:

./gradlew :sv-service:test

현재 코드가 이미 실패하는 상태라면 신규 개발 오류와 기존 오류를 구분하기 어렵습니다.

따라서 개발 시작 시점의 빌드 결과를 기록하는 것이 좋습니다.

## **12.7 환경설정 확인**

application.yml, application-local.yml 또는 프로젝트의 환경별 설정을 확인합니다.

예:

spring:
datasource:
url: jdbc:oracle:thin:@localhost:1521/XEPDB1
username: nsight
password: ${NSIGHT\_DB\_PASSWORD}
hikari:
pool-name: SV-HikariPool
maximum-pool-size: 10
connection-timeout: 3000

mybatis:
mapper-locations:
\- classpath:/mapper/\*\*/\*.xml
configuration:
map-underscore-to-camel-case: true

주의사항:

운영 비밀번호를 application.yml에 직접 작성하지 않는다.

실제 운영 URL을 로컬 설정에 복사하지 않는다.

업무 WAR별 Pool 이름을 구분한다.

Mapper XML 경로가 신규 파일 위치를 포함하는지 확인한다.

## **12.8 개발 시작 전 체크리스트**

| **점검 항목** | **확인** |
| --- | --- |
| sv-service 모듈 위치를 확인했는가? | □ |
| 기본 빌드가 성공하는가? | □ |
| 기존 Handler 구현을 확인했는가? | □ |
| 패키지 기준을 확인했는가? | □ |
| Mapper XML 경로를 확인했는가? | □ |
| 로컬 DB 접속이 가능한가? | □ |
| 테스트 데이터가 존재하는가? | □ |
| ServiceId와 거래코드가 확정되었는가? | □ |
| OM 등록 담당자를 확인했는가? | □ |

## **제12장 요약**

학습 목표 | 12장 요약의 핵심 개념을 설명하고, 프로젝트에서 적용 위치와 검증 방법을 스스로 판단한다.

핵심 관점 | 책임과 흐름: 이 주제는 개별 기술보다 입력, 처리 책임, 출력과 다음 단계의 연결 관계를 먼저 파악해야 합니다.

읽는 순서 | 먼저 용어와 책임 경계를 확인하고, 이어지는 정상 예시와 금지 예시를 비교한 뒤 실제 설정·코드·로그에서 근거를 찾아봅니다.

코드를 만들기 전에
모듈·패키지·기존 구현·설정·빌드 상태를 먼저 확인한다.

신규 거래는 해당 업무 WAR 안에서
도메인과 계층 기준으로 작성한다.

# **제13장. 거래 골격과 패키지 만들기**

학습 목표 | 13장. 거래 골격과 패키지 만들기의 핵심 개념을 설명하고, 프로젝트에서 적용 위치와 검증 방법을 스스로 판단한다.

핵심 관점 | 식별 가능성과 일관성: 이름은 단순 표기가 아니라 소스, 거래, 로그, 운영 설정과 산출물을 연결하는 공통 키입니다.

읽는 순서 | 먼저 용어와 책임 경계를 확인하고, 이어지는 정상 예시와 금지 예시를 비교한 뒤 실제 설정·코드·로그에서 근거를 찾아봅니다.

## **13.1 거래 기본정보 확정**

이번 거래의 기준정보입니다.

업무코드
SV

도메인
Customer

ServiceId
SV.Customer.selectSummary

거래코드
SV-INQ-0001

권한
SV\_CUSTOMER\_VIEW

Timeout
3000ms

## **13.2 프로그램 목록 정의**

| **계층** | **클래스** |
| --- | --- |
| Handler | SvCustomerHandler |
| Facade | SvCustomerFacade |
| Service | SvCustomerService |
| Rule | SvCustomerRule |
| DAO | SvCustomerDao |
| Mapper | SvCustomerMapper |
| Request DTO | CustomerSummaryRequest |
| Response DTO | CustomerSummaryResponse |
| DB Result DTO | CustomerSummaryData |

## **13.3 호출 관계**

SvCustomerHandler
↓
SvCustomerFacade.selectSummary()
↓
SvCustomerService.selectSummary()
├─ SvCustomerRule.validateSummaryRequest()
├─ SvCustomerDao.selectCustomerSummary()
│ ↓
│ SvCustomerMapper.selectCustomerSummary()
└─ SvCustomerRule.validateCustomerExists()

## **13.4 먼저 빈 골격을 만든다**

클래스 내용을 한꺼번에 작성하지 않고 먼저 전체 파일을 생성합니다.

handler/SvCustomerHandler.java
facade/SvCustomerFacade.java
service/SvCustomerService.java
rule/SvCustomerRule.java
dao/SvCustomerDao.java
mapper/SvCustomerMapper.java
dto/request/CustomerSummaryRequest.java
dto/response/CustomerSummaryResponse.java
dto/data/CustomerSummaryData.java

Mapper XML:

src/main/resources/mapper/sv/customer/SvCustomerMapper.xml

이 단계에서 프로젝트를 다시 빌드합니다.

./gradlew :sv-service:compileJava

빈 클래스 상태에서도 패키지명과 Import 오류를 먼저 잡을 수 있습니다.

## **13.5 클래스 이름 규칙**

| **대상** | **형식** | **예** |
| --- | --- | --- |
| Handler | {업무코드}{도메인}Handler | SvCustomerHandler |
| Facade | {업무코드}{도메인}Facade | SvCustomerFacade |
| Service | {업무코드}{도메인}Service | SvCustomerService |
| Rule | {업무코드}{도메인}Rule | SvCustomerRule |
| DAO | {업무코드}{도메인}Dao | SvCustomerDao |
| Mapper | {업무코드}{도메인}Mapper | SvCustomerMapper |
| Request | {기능}Request | CustomerSummaryRequest |
| Response | {기능}Response | CustomerSummaryResponse |
| DB 결과 | {기능}Data | CustomerSummaryData |

## **13.6 메서드 이름 규칙**

ServiceId의 행위와 메서드 이름을 가능한 한 일치시킵니다.

ServiceId
SV.Customer.selectSummary

Facade
selectSummary()

Service
selectSummary()

DAO
selectCustomerSummary()

Mapper
selectCustomerSummary()

각 계층에서 의미가 없는 이름을 사용하지 않습니다.

금지:

run()
process()
execute()
doWork()
getData()
query1()

## **13.7 정상 구조**

com.nh.nsight.marketing.sv.customer.handler.SvCustomerHandler
com.nh.nsight.marketing.sv.customer.facade.SvCustomerFacade
com.nh.nsight.marketing.sv.customer.service.SvCustomerService
com.nh.nsight.marketing.sv.customer.rule.SvCustomerRule
com.nh.nsight.marketing.sv.customer.dao.SvCustomerDao
com.nh.nsight.marketing.sv.customer.mapper.SvCustomerMapper

## **13.8 금지 구조**

com.nh.nsight.common.CustomerController
com.nh.nsight.util.CustomerUtil
com.nh.nsight.service.ServiceImpl
com.nh.nsight.mapper.CommonMapper

문제:

-   업무코드를 식별할 수 없음
-   도메인 소유권이 불명확함
-   모든 기능이 common에 집중됨
-   장애 발생 시 담당 업무를 찾기 어려움
-   동일 이름의 클래스가 반복 생성될 가능성이 큼

## **제13장 요약**

학습 목표 | 13장 요약의 핵심 개념을 설명하고, 프로젝트에서 적용 위치와 검증 방법을 스스로 판단한다.

핵심 관점 | 책임과 흐름: 이 주제는 개별 기술보다 입력, 처리 책임, 출력과 다음 단계의 연결 관계를 먼저 파악해야 합니다.

읽는 순서 | 먼저 용어와 책임 경계를 확인하고, 이어지는 정상 예시와 금지 예시를 비교한 뒤 실제 설정·코드·로그에서 근거를 찾아봅니다.

ServiceId를 기준으로
필요한 전체 프로그램 목록을 먼저 정의한다.

패키지는
업무코드 → 도메인 → 계층
순서로 구성한다.

# **제14장. DTO와 Validation 구현하기**

학습 목표 | 14장. DTO와 Validation 구현하기의 핵심 개념을 설명하고, 프로젝트에서 적용 위치와 검증 방법을 스스로 판단한다.

핵심 관점 | 책임과 흐름: 이 주제는 개별 기술보다 입력, 처리 책임, 출력과 다음 단계의 연결 관계를 먼저 파악해야 합니다.

읽는 순서 | 먼저 용어와 책임 경계를 확인하고, 이어지는 정상 예시와 금지 예시를 비교한 뒤 실제 설정·코드·로그에서 근거를 찾아봅니다.

## **14.1 DTO란 무엇인가요?**

DTO는 계층 사이에 데이터를 전달하는 객체입니다.

Data Transfer Object
\= 데이터를 전달하기 위한 객체

이번 거래에서는 세 종류의 DTO를 사용합니다.

CustomerSummaryRequest
\= 화면 요청 데이터

CustomerSummaryData
\= DB 조회 결과

CustomerSummaryResponse
\= 화면 응답 데이터

DTO를 구분하면 DB 구조와 화면 계약을 분리할 수 있습니다.

## **14.2 요청 DTO**

package com.nh.nsight.marketing.sv.customer.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CustomerSummaryRequest(

@NotBlank(message = "고객번호는 필수입니다.")
@Size(max = 20, message = "고객번호는 20자를 초과할 수 없습니다.")
String customerNo,

@NotBlank(message = "기준일은 필수입니다.")
@Pattern(
regexp = "\\\\d{8}",
message = "기준일은 YYYYMMDD 형식이어야 합니다."
)
String baseDate

) {
}

## **14.3 형식 검증과 업무 검증**

두 검증은 목적이 다릅니다.

### **형식 검증**

값이 존재하는가?
길이가 맞는가?
숫자인가?
날짜 형식인가?
허용된 코드인가?

DTO Annotation으로 처리할 수 있습니다.

### **업무 검증**

고객이 존재하는가?
조회 가능한 상태인가?
사용자가 해당 고객을 볼 수 있는가?
기준일이 업무 허용기간인가?

Service와 Rule에서 처리합니다.

## **14.4 DB 결과 DTO**

package com.nh.nsight.marketing.sv.customer.dto.data;

public record CustomerSummaryData(
String customerNo,
String customerName,
String customerGrade,
String baseDate
) {
}

MyBatis가 Record 생성자를 지원하는지 프로젝트 설정과 버전을 확인해야 합니다.

지원하지 않거나 프로젝트 표준이 JavaBean 방식이라면 다음과 같이 작성할 수 있습니다.

package com.nh.nsight.marketing.sv.customer.dto.data;

public class CustomerSummaryData {

private String customerNo;
private String customerName;
private String customerGrade;
private String baseDate;

public String getCustomerNo() {
return customerNo;
}

public void setCustomerNo(String customerNo) {
this.customerNo = customerNo;
}

public String getCustomerName() {
return customerName;
}

public void setCustomerName(String customerName) {
this.customerName = customerName;
}

public String getCustomerGrade() {
return customerGrade;
}

public void setCustomerGrade(String customerGrade) {
this.customerGrade = customerGrade;
}

public String getBaseDate() {
return baseDate;
}

public void setBaseDate(String baseDate) {
this.baseDate = baseDate;
}
}

## **14.5 응답 DTO**

package com.nh.nsight.marketing.sv.customer.dto.response;

import com.nh.nsight.marketing.sv.customer.dto.data.CustomerSummaryData;

public record CustomerSummaryResponse(
String customerNo,
String customerName,
String customerGrade,
String baseDate
) {

public static CustomerSummaryResponse from(
CustomerSummaryData data) {

return new CustomerSummaryResponse(
data.customerNo(),
data.customerName(),
data.customerGrade(),
data.baseDate()
);
}
}

DB 결과가 JavaBean이라면 Getter를 사용합니다.

## **14.6 DB 결과와 응답 DTO를 분리하는 이유**

DB에서 다음 컬럼을 조회한다고 가정합니다.

CUSTOMER\_NO
CUSTOMER\_NAME
CUSTOMER\_GRADE\_CD
BASE\_DT
INTERNAL\_STATUS\_CD
UPDATE\_DTM

화면에는 내부 상태와 수정시각이 필요하지 않을 수 있습니다.

화면 응답
\- customerNo
\- customerName
\- customerGrade
\- baseDate

DB 결과 DTO와 응답 DTO를 같은 객체로 사용하면 불필요한 내부 컬럼이 화면에 노출될 위험이 있습니다.

## **14.7 사용자 정보를 Body에 넣지 않는다**

금지 요청:

{
"customerNo": "CUST000001",
"userId": "admin",
"branchId": "999999"
}

사용자와 지점정보는 검증된 TransactionContext에서 가져옵니다.

String userId = context.getUserId();
String branchId = context.getBranchId();

화면이 보낸 사용자 정보를 업무 권한의 기준으로 사용하지 않습니다.

## **14.8 날짜 검증 주의사항**

정규식 \\d{8}은 숫자 8자리만 확인합니다.

20261340

도 숫자 8자리이므로 형식 검증을 통과할 수 있습니다.

실제 날짜 유효성은 Rule에서 확인합니다.

public LocalDate parseBaseDate(String baseDate) {
try {
return LocalDate.parse(
baseDate,
DateTimeFormatter.BASIC\_ISO\_DATE
);
} catch (DateTimeParseException e) {
throw new BusinessException(
"E-SV-CUST-0003",
"유효하지 않은 기준일입니다."
);
}
}

## **14.9 DTO 금지 예시**

public class CommonDto {
private String value1;
private String value2;
private String value3;
private Map<String, Object> data;
}

문제:

-   각 필드의 업무 의미를 알 수 없음
-   Validation을 적용하기 어려움
-   타입 안정성이 없음
-   변경 영향 분석이 어려움
-   문서와 코드 추적성이 낮음

## **제14장 요약**

학습 목표 | 14장 요약의 핵심 개념을 설명하고, 프로젝트에서 적용 위치와 검증 방법을 스스로 판단한다.

핵심 관점 | 책임과 흐름: 이 주제는 개별 기술보다 입력, 처리 책임, 출력과 다음 단계의 연결 관계를 먼저 파악해야 합니다.

읽는 순서 | 먼저 용어와 책임 경계를 확인하고, 이어지는 정상 예시와 금지 예시를 비교한 뒤 실제 설정·코드·로그에서 근거를 찾아봅니다.

Request DTO
\= 화면 입력

Data DTO
\= DB 결과

Response DTO
\= 화면 반환

형식 검증은 DTO에서,
업무 검증은 Rule에서 수행한다.

# **제15장. Handler 구현하기**

학습 목표 | 15장. Handler 구현하기의 핵심 개념을 설명하고, 프로젝트에서 적용 위치와 검증 방법을 스스로 판단한다.

핵심 관점 | 책임과 흐름: 이 주제는 개별 기술보다 입력, 처리 책임, 출력과 다음 단계의 연결 관계를 먼저 파악해야 합니다.

읽는 순서 | 먼저 용어와 책임 경계를 확인하고, 이어지는 정상 예시와 금지 예시를 비교한 뒤 실제 설정·코드·로그에서 근거를 찾아봅니다.

## **15.1 Handler의 역할**

Handler는 ServiceId와 Facade를 연결합니다.

Dispatcher
→ ServiceId 확인
→ Handler 선택
→ Facade 호출

Handler의 책임은 다음으로 제한합니다.

지원 ServiceId 등록
요청 Body를 DTO로 변환
ServiceId에 맞는 Facade 호출
결과 반환

## **15.2 도메인 단위 Handler**

고객 도메인 Handler가 여러 고객 ServiceId를 처리할 수 있습니다.

package com.nh.nsight.marketing.sv.customer.handler;

import java.util.Set;

import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class SvCustomerHandler implements TransactionHandler {

private static final String SELECT\_SUMMARY =
"SV.Customer.selectSummary";

private final SvCustomerFacade customerFacade;

@Override
public Set<String> serviceIds() {
return Set.of(SELECT\_SUMMARY);
}

@Override
public Object handle(
StandardRequest<?> request,
TransactionContext context) {

String serviceId = context.getServiceId();

return switch (serviceId) {
case SELECT\_SUMMARY ->
handleSelectSummary(request, context);

default ->
throw new ServiceNotFoundException(serviceId);
};
}

private CustomerSummaryResponse handleSelectSummary(
StandardRequest<?> request,
TransactionContext context) {

CustomerSummaryRequest input =
request.bodyAs(CustomerSummaryRequest.class);

return customerFacade.selectSummary(input, context);
}
}

TransactionHandler, StandardRequest, TransactionContext의 실제 패키지와 메서드명은 프로젝트 구현을 따릅니다.

## **15.3 ServiceId 상수**

문자열을 여러 곳에 반복 작성하지 않습니다.

권장:

private static final String SELECT\_SUMMARY =
"SV.Customer.selectSummary";

프로젝트에 중앙 ServiceId 상수 클래스나 Catalog가 있다면 해당 표준을 따릅니다.

## **15.4 요청 DTO 변환**

CustomerSummaryRequest input =
request.bodyAs(CustomerSummaryRequest.class);

공통 bodyAs() 기능이 없다면 프로젝트의 ObjectMapper 또는 변환기를 사용합니다.

중요한 기준은 다음입니다.

Handler는 Map에서 문자열을 하나씩 꺼내지 않는다.

요청 Body를 명확한 DTO로 변환한다.

금지:

Map<String, Object> body = request.getBody();
String customerNo = (String) body.get("value1");

## **15.5 Handler에서 하지 않는 일**

금지 예:

public Object handle(
StandardRequest<?> request,
TransactionContext context) {

CustomerSummaryRequest input =
request.bodyAs(CustomerSummaryRequest.class);

if (input.customerNo() == null) {
throw new RuntimeException("고객번호 없음");
}

CustomerSummaryData data =
customerMapper.selectCustomerSummary(
input.customerNo(),
input.baseDate()
);

return new StandardResponse(data);
}

문제:

-   Handler에서 업무 검증
-   Mapper 직접 호출
-   Service와 Rule 우회
-   표준 응답 직접 생성
-   트랜잭션 경계 없음
-   DB 예외 변환 없음

## **15.6 Handler 테스트**

Handler 테스트에서는 다음을 확인합니다.

지원 ServiceId가 등록되는가?
올바른 ServiceId가 Facade를 호출하는가?
DTO 변환이 정상인가?
지원하지 않는 ServiceId가 오류를 발생시키는가?

개념적인 테스트:

@ExtendWith(MockitoExtension.class)
class SvCustomerHandlerTest {

@Mock
private SvCustomerFacade customerFacade;

@InjectMocks
private SvCustomerHandler handler;

@Test
void shouldRegisterSelectSummaryServiceId() {
assertThat(handler.serviceIds())
.contains("SV.Customer.selectSummary");
}
}

실제 StandardRequest 생성방법은 프로젝트 테스트 도구를 사용합니다.

## **제15장 요약**

학습 목표 | 15장 요약의 핵심 개념을 설명하고, 프로젝트에서 적용 위치와 검증 방법을 스스로 판단한다.

핵심 관점 | 책임과 흐름: 이 주제는 개별 기술보다 입력, 처리 책임, 출력과 다음 단계의 연결 관계를 먼저 파악해야 합니다.

읽는 순서 | 먼저 용어와 책임 경계를 확인하고, 이어지는 정상 예시와 금지 예시를 비교한 뒤 실제 설정·코드·로그에서 근거를 찾아봅니다.

Handler는 얇게 만든다.

ServiceId를 등록하고,
요청을 DTO로 변환하고,
Facade를 호출한다.

SQL과 업무 규칙은 작성하지 않는다.

# **제16장. Facade와 Service 구현하기**

학습 목표 | 16장. Facade와 Service 구현하기의 핵심 개념을 설명하고, 프로젝트에서 적용 위치와 검증 방법을 스스로 판단한다.

핵심 관점 | 책임과 흐름: 이 주제는 개별 기술보다 입력, 처리 책임, 출력과 다음 단계의 연결 관계를 먼저 파악해야 합니다.

읽는 순서 | 먼저 용어와 책임 경계를 확인하고, 이어지는 정상 예시와 금지 예시를 비교한 뒤 실제 설정·코드·로그에서 근거를 찾아봅니다.

## **16.1 Facade의 역할**

Facade는 하나의 유스케이스와 트랜잭션 경계를 담당합니다.

ServiceId 하나
→ 업무 유스케이스 하나
→ Facade 메서드 하나

조회 거래이므로 읽기 전용 트랜잭션을 적용합니다.

package com.nh.nsight.marketing.sv.customer.facade;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class SvCustomerFacade {

private final SvCustomerService customerService;

@Transactional(readOnly = true)
public CustomerSummaryResponse selectSummary(
CustomerSummaryRequest request,
TransactionContext context) {

return customerService.selectSummary(request, context);
}
}

## **16.2 왜 트랜잭션을 Facade에 둘까요?**

ServiceId가 실행하는 전체 업무 흐름을 하나의 트랜잭션으로 관리하기 쉽기 때문입니다.

등록 거래 예:

캠페인 기본정보 등록
→ 대상조건 등록
→ 메시지 템플릿 연결

세 작업을 하나의 거래로 취급해야 한다면 Facade가 전체 트랜잭션을 관리합니다.

## **16.3 readOnly = true**

조회 거래에서는 다음과 같이 선언할 수 있습니다.

@Transactional(readOnly = true)

효과는 DB와 JPA 사용 여부 등 환경에 따라 달라질 수 있지만, 코드 수준에서는 해당 유스케이스가 조회 전용임을 명확하게 표현합니다.

조회 Facade 안에서 데이터를 변경하면 안 됩니다.

## **16.4 Service의 역할**

Service는 업무 처리 순서를 구현합니다.

이번 거래의 처리 순서는 다음과 같습니다.

요청 검증
→ 기준일 변환
→ 데이터 접근 권한 검증
→ 고객 요약 조회
→ 조회결과 존재 검증
→ 응답 DTO 변환

구현 예:

package com.nh.nsight.marketing.sv.customer.service;

import java.time.LocalDate;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SvCustomerService {

private final SvCustomerRule customerRule;
private final SvCustomerDao customerDao;

public CustomerSummaryResponse selectSummary(
CustomerSummaryRequest request,
TransactionContext context) {

LocalDate baseDate =
customerRule.validateSummaryRequest(request);

customerRule.validateInquiryAuthority(
context.getUserId(),
context.getBranchId()
);

CustomerSummaryData data =
customerDao.selectCustomerSummary(
request.customerNo(),
baseDate
);

customerRule.validateCustomerExists(data);

return CustomerSummaryResponse.from(data);
}
}

## **16.5 Service 코드는 업무 문장처럼 읽혀야 한다**

좋은 Service 코드는 처리 순서를 쉽게 이해할 수 있습니다.

요청을 검증한다.
권한을 확인한다.
고객정보를 조회한다.
고객 존재 여부를 확인한다.
응답으로 변환한다.

다음과 같이 기술 코드가 지나치게 섞이면 읽기 어렵습니다.

Connection con = dataSource.getConnection();
PreparedStatement ps = con.prepareStatement(...);
ResultSet rs = ps.executeQuery();

이 코드는 DAO와 Mapper의 책임입니다.

## **16.6 Context 사용**

Service는 검증된 거래 문맥을 사용합니다.

예:

String userId = context.getUserId();
String branchId = context.getBranchId();
String traceId = context.getTraceId();

원본 요청 Body의 사용자 ID를 신뢰하지 않습니다.

## **16.7 Service 금지 예시**

@Service
public class SvCustomerService {

public Map<String, Object> process(
HttpServletRequest httpRequest,
Map<String, Object> body) {

String token =
httpRequest.getHeader("Authorization");

// JWT 직접 해석
// SQL 문자열 생성
// DB Connection 직접 사용
// 응답 Map 직접 생성

return result;
}
}

문제:

-   HTTP 기술에 직접 의존
-   JWT 검증 책임 침범
-   DTO 없음
-   DAO·Mapper 우회
-   표준 응답 구조 훼손
-   단위 테스트 어려움

## **16.8 Service 단위 테스트**

Service 테스트에서는 Rule과 DAO를 Mock으로 대체할 수 있습니다.

@ExtendWith(MockitoExtension.class)
class SvCustomerServiceTest {

@Mock
private SvCustomerRule customerRule;

@Mock
private SvCustomerDao customerDao;

@Mock
private TransactionContext context;

@InjectMocks
private SvCustomerService customerService;

@Test
void shouldReturnCustomerSummary() {
CustomerSummaryRequest request =
new CustomerSummaryRequest(
"CUST000001",
"20260717"
);

LocalDate baseDate =
LocalDate.of(2026, 7, 17);

CustomerSummaryData data =
new CustomerSummaryData(
"CUST000001",
"홍길동",
"VIP",
"20260717"
);

given(customerRule.validateSummaryRequest(request))
.willReturn(baseDate);

given(context.getUserId())
.willReturn("user01");

given(context.getBranchId())
.willReturn("001234");

given(customerDao.selectCustomerSummary(
"CUST000001",
baseDate
)).willReturn(data);

CustomerSummaryResponse response =
customerService.selectSummary(request, context);

assertThat(response.customerNo())
.isEqualTo("CUST000001");

assertThat(response.customerGrade())
.isEqualTo("VIP");
}
}

## **제16장 요약**

학습 목표 | 16장 요약의 핵심 개념을 설명하고, 프로젝트에서 적용 위치와 검증 방법을 스스로 판단한다.

핵심 관점 | 책임과 흐름: 이 주제는 개별 기술보다 입력, 처리 책임, 출력과 다음 단계의 연결 관계를 먼저 파악해야 합니다.

읽는 순서 | 먼저 용어와 책임 경계를 확인하고, 이어지는 정상 예시와 금지 예시를 비교한 뒤 실제 설정·코드·로그에서 근거를 찾아봅니다.

Facade
\= 유스케이스와 트랜잭션 경계

Service
\= 업무 처리 순서

Service는
Rule과 DAO를 조합하여
업무 흐름을 완성한다.

# **제17장. Rule 구현하기**

학습 목표 | 17장. Rule 구현하기의 핵심 개념을 설명하고, 프로젝트에서 적용 위치와 검증 방법을 스스로 판단한다.

핵심 관점 | 책임과 흐름: 이 주제는 개별 기술보다 입력, 처리 책임, 출력과 다음 단계의 연결 관계를 먼저 파악해야 합니다.

읽는 순서 | 먼저 용어와 책임 경계를 확인하고, 이어지는 정상 예시와 금지 예시를 비교한 뒤 실제 설정·코드·로그에서 근거를 찾아봅니다.

## **17.1 Rule의 역할**

Rule은 업무 조건을 검사하고 업무 계산을 수행합니다.

이번 거래에서는 다음 규칙을 처리합니다.

고객번호가 존재하는가?
기준일이 실제 날짜인가?
미래 기준일이 허용되는가?
사용자와 지점이 고객 조회권한을 가지는가?
조회된 고객이 존재하는가?

## **17.2 Rule 구현**

package com.nh.nsight.marketing.sv.customer.rule;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

import org.springframework.stereotype.Component;

@Component
public class SvCustomerRule {

private static final DateTimeFormatter BASIC\_DATE =
DateTimeFormatter.BASIC\_ISO\_DATE;

public LocalDate validateSummaryRequest(
CustomerSummaryRequest request) {

if (request == null) {
throw new BusinessException(
"E-SV-CUST-0001",
"요청정보가 없습니다."
);
}

LocalDate baseDate = parseBaseDate(
request.baseDate()
);

if (baseDate.isAfter(LocalDate.now())) {
throw new BusinessException(
"E-SV-CUST-0004",
"미래 기준일은 조회할 수 없습니다."
);
}

return baseDate;
}

public void validateInquiryAuthority(
String userId,
String branchId) {

if (userId == null || userId.isBlank()) {
throw new BusinessException(
"E-COM-AUTH-0001",
"인증된 사용자정보가 없습니다."
);
}

if (branchId == null || branchId.isBlank()) {
throw new BusinessException(
"E-COM-AUTH-0002",
"사용자 지점정보가 없습니다."
);
}
}

public void validateCustomerExists(
CustomerSummaryData data) {

if (data == null) {
throw new BusinessException(
"E-SV-CUST-0002",
"고객을 찾을 수 없습니다."
);
}
}

private LocalDate parseBaseDate(String baseDate) {
try {
return LocalDate.parse(
baseDate,
BASIC\_DATE
);
} catch (DateTimeParseException e) {
throw new BusinessException(
"E-SV-CUST-0003",
"유효하지 않은 기준일입니다."
);
}
}
}

BusinessException 생성자와 오류코드 형식은 실제 공통 오류체계를 따릅니다.

## **17.3 Rule은 부수효과를 최소화한다**

좋은 Rule은 다음 특징을 가집니다.

같은 입력이면 같은 결과를 반환한다.

DB를 직접 변경하지 않는다.

외부 시스템을 호출하지 않는다.

파일을 생성하지 않는다.

세션 상태를 변경하지 않는다.

Rule에서 데이터가 필요하면 Service가 먼저 DAO를 호출한 뒤 결과를 Rule에 전달합니다.

Service가 DB 조회
→ Rule이 결과 검증

## **17.4 권한 검증 구분**

권한은 여러 단계에서 검증될 수 있습니다.

| **단계** | **검증 내용** |
| --- | --- |
| Gateway·Filter | JWT 유효성 |
| STF | 공통 기능권한 |
| Service·Rule | 상세 데이터 권한 |
| SQL | 최종 데이터 범위 제한 |

예를 들어 SV\_CUSTOMER\_VIEW 기능권한은 STF에서 확인하고, 특정 지점 고객만 조회하는 데이터 범위는 Service·Rule과 SQL에서 함께 적용할 수 있습니다.

## **17.5 Rule 금지 예시**

@Component
@RequiredArgsConstructor
public class SvCustomerRule {

private final SvCustomerMapper customerMapper;

public void validateCustomer(String customerNo) {
CustomerSummaryData data =
customerMapper.selectCustomerSummary(customerNo);

if (data == null) {
throw new RuntimeException();
}
}
}

문제:

-   Rule이 Mapper에 직접 의존
-   데이터 조회와 규칙이 결합
-   단위 테스트에 DB가 필요
-   Service 흐름을 우회
-   SQL 변경이 Rule까지 전파

## **17.6 Rule 단위 테스트**

class SvCustomerRuleTest {

private final SvCustomerRule rule =
new SvCustomerRule();

@Test
void shouldRejectFutureBaseDate() {
CustomerSummaryRequest request =
new CustomerSummaryRequest(
"CUST000001",
"29991231"
);

assertThatThrownBy(
() -> rule.validateSummaryRequest(request)
)
.isInstanceOf(BusinessException.class)
.hasMessageContaining("미래 기준일");
}

@Test
void shouldRejectMissingCustomer() {
assertThatThrownBy(
() -> rule.validateCustomerExists(null)
)
.isInstanceOf(BusinessException.class)
.hasMessageContaining("고객을 찾을 수 없습니다");
}
}

Rule 테스트는 Spring Context 없이 빠르게 실행할 수 있는 것이 좋습니다.

## **제17장 요약**

학습 목표 | 17장 요약의 핵심 개념을 설명하고, 프로젝트에서 적용 위치와 검증 방법을 스스로 판단한다.

핵심 관점 | 책임과 흐름: 이 주제는 개별 기술보다 입력, 처리 책임, 출력과 다음 단계의 연결 관계를 먼저 파악해야 합니다.

읽는 순서 | 먼저 용어와 책임 경계를 확인하고, 이어지는 정상 예시와 금지 예시를 비교한 뒤 실제 설정·코드·로그에서 근거를 찾아봅니다.

Rule
\= 업무 조건과 업무 계산

Rule은 DB를 직접 호출하지 않는다.

필요한 데이터는 Service가 조회하고
Rule에 전달한다.

# **제18장. DAO와 Mapper 구현하기**

학습 목표 | 18장. DAO와 Mapper 구현하기의 핵심 개념을 설명하고, 프로젝트에서 적용 위치와 검증 방법을 스스로 판단한다.

핵심 관점 | 데이터 일관성: 화면 동작보다 데이터 소유권, 상태 전이, 동시성, 트랜잭션 경계를 먼저 결정해야 합니다.

읽는 순서 | 먼저 용어와 책임 경계를 확인하고, 이어지는 정상 예시와 금지 예시를 비교한 뒤 실제 설정·코드·로그에서 근거를 찾아봅니다.

## **18.1 DAO의 역할**

DAO는 Service와 MyBatis Mapper 사이의 데이터 접근 경계입니다.

Service
→ DAO
→ Mapper
→ SQL
→ DB

DAO의 주요 책임:

Mapper 호출
조회조건 전달
DB 결과 반환
데이터 접근 예외 변환
데이터소스 선택

## **18.2 DAO 구현**

package com.nh.nsight.marketing.sv.customer.dao;

import java.time.LocalDate;

import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Repository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class SvCustomerDao {

private final SvCustomerMapper customerMapper;

public CustomerSummaryData selectCustomerSummary(
String customerNo,
LocalDate baseDate) {

try {
return customerMapper.selectCustomerSummary(
customerNo,
baseDate.format(
DateTimeFormatter.BASIC\_ISO\_DATE
)
);

} catch (DataAccessException e) {
throw new SystemException(
"E-SV-DB-0001",
"고객 요약정보 조회 중 오류가 발생했습니다.",
e
);
}
}
}

프로젝트의 공통 예외 변환기가 존재한다면 DAO에서 중복 변환하지 않고 해당 표준을 사용합니다.

## **18.3 Mapper 인터페이스**

package com.nh.nsight.marketing.sv.customer.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface SvCustomerMapper {

CustomerSummaryData selectCustomerSummary(
@Param("customerNo") String customerNo,
@Param("baseDate") String baseDate
);
}

두 개 이상의 단순 Parameter를 전달할 때는 @Param 이름을 명시하는 것이 안전합니다.

더 복잡한 조건은 Query DTO를 사용합니다.

## **18.4 Query DTO 대안**

public record CustomerSummaryQuery(
String customerNo,
String baseDate,
String branchId
) {
}

Mapper:

CustomerSummaryData selectCustomerSummary(
CustomerSummaryQuery query
);

조회조건이 많아질수록 Query DTO 방식이 관리하기 쉽습니다.

## **18.5 Mapper XML**

<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE mapper
PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
"https://mybatis.org/dtd/mybatis-3-mapper.dtd">

<mapper namespace=
"com.nh.nsight.marketing.sv.customer.mapper.SvCustomerMapper">

<resultMap id="customerSummaryResultMap"
type=
"com.nh.nsight.marketing.sv.customer.dto.data.CustomerSummaryData">

<result property="customerNo"
column="CUSTOMER\_NO"/>

<result property="customerName"
column="CUSTOMER\_NAME"/>

<result property="customerGrade"
column="CUSTOMER\_GRADE"/>

<result property="baseDate"
column="BASE\_DATE"/>

</resultMap>

<select id="selectCustomerSummary"
resultMap="customerSummaryResultMap">

SELECT CUSTOMER\_NO,
CUSTOMER\_NAME,
CUSTOMER\_GRADE,
BASE\_DATE
FROM SV\_CUSTOMER\_SUMMARY
WHERE CUSTOMER\_NO = #{customerNo}
AND BASE\_DATE = #{baseDate}

</select>

</mapper>

## **18.6 Namespace 정합성**

Mapper XML Namespace는 Java Mapper의 전체 패키지명과 같아야 합니다.

Java Mapper

com.nh.nsight.marketing.sv.customer.mapper.SvCustomerMapper

<mapper namespace=
"com.nh.nsight.marketing.sv.customer.mapper.SvCustomerMapper">

다르면 MyBatis가 SQL을 찾지 못할 수 있습니다.

## **18.7 SQL ID 정합성**

Java Mapper 메서드:

selectCustomerSummary()

Mapper XML:

<select id="selectCustomerSummary">

두 이름이 일치해야 합니다.

## **18.8 SELECT \*를 피한다**

금지:

SELECT \*
FROM SV\_CUSTOMER\_SUMMARY
WHERE CUSTOMER\_NO = #{customerNo}

권장:

SELECT CUSTOMER\_NO,
CUSTOMER\_NAME,
CUSTOMER\_GRADE,
BASE\_DATE
FROM SV\_CUSTOMER\_SUMMARY
WHERE CUSTOMER\_NO = #{customerNo}
AND BASE\_DATE = #{baseDate}

필요한 컬럼만 조회하면 다음 장점이 있습니다.

-   불필요한 데이터 전송 감소
-   컬럼 추가에 따른 영향 감소
-   개인정보 노출 위험 감소
-   결과 매핑 명확화
-   SQL 리뷰 용이성 향상

## **18.9 문자열 치환 금지**

금지:

WHERE CUSTOMER\_NO = '${customerNo}'

권장:

WHERE CUSTOMER\_NO = #{customerNo}

${}는 문자열을 SQL에 직접 삽입하므로 SQL Injection 위험이 있습니다.

동적 컬럼이나 정렬조건처럼 ${}가 꼭 필요한 경우에는 허용값을 서버에서 화이트리스트로 검증해야 합니다.

## **18.10 데이터 권한 조건**

사용자 지점에 따라 조회범위를 제한해야 한다면 SQL 조건에 지점정보를 포함할 수 있습니다.

SELECT CUSTOMER\_NO,
CUSTOMER\_NAME,
CUSTOMER\_GRADE,
BASE\_DATE
FROM SV\_CUSTOMER\_SUMMARY
WHERE CUSTOMER\_NO = #{customerNo}
AND BASE\_DATE = #{baseDate}
AND BRANCH\_ID = #{branchId}

branchId는 화면 Body가 아니라 검증된 TransactionContext에서 가져옵니다.

## **18.11 인덱스 검토**

조회조건:

CUSTOMER\_NO
BASE\_DATE
BRANCH\_ID

대량 테이블이라면 적절한 인덱스가 필요한지 DBA와 검토합니다.

초보 개발자가 임의로 운영 인덱스를 생성하지는 않지만, SQL이 어떤 조건을 사용하는지 설계서에 명확히 기록해야 합니다.

## **18.12 Mapper 통합 테스트**

@SpringBootTest
@Transactional
class SvCustomerMapperTest {

@Autowired
private SvCustomerMapper customerMapper;

@Test
void shouldSelectCustomerSummary() {
CustomerSummaryData data =
customerMapper.selectCustomerSummary(
"CUST000001",
"20260717"
);

assertThat(data).isNotNull();
assertThat(data.customerName())
.isEqualTo("홍길동");
}
}

실제 테스트에서는 테스트 전용 데이터나 Rollback 가능한 데이터셋을 사용합니다.

## **제18장 요약**

학습 목표 | 18장 요약의 핵심 개념을 설명하고, 프로젝트에서 적용 위치와 검증 방법을 스스로 판단한다.

핵심 관점 | 책임과 흐름: 이 주제는 개별 기술보다 입력, 처리 책임, 출력과 다음 단계의 연결 관계를 먼저 파악해야 합니다.

읽는 순서 | 먼저 용어와 책임 경계를 확인하고, 이어지는 정상 예시와 금지 예시를 비교한 뒤 실제 설정·코드·로그에서 근거를 찾아봅니다.

DAO
\= 데이터 접근 경계

Mapper
\= SQL 실행

Mapper Interface와 XML Namespace,
메서드명과 SQL ID가 일치해야 한다.

SQL에는 필요한 컬럼과 조건만 작성한다.

# **제19장. 전체 거래 연결과 테스트**

학습 목표 | 19장. 전체 거래 연결과 테스트의 핵심 개념을 설명하고, 프로젝트에서 적용 위치와 검증 방법을 스스로 판단한다.

핵심 관점 | 검증 가능성: 좋은 구현은 동작할 뿐 아니라 변경 이유와 영향 범위를 증명할 수 있어야 합니다.

읽는 순서 | 먼저 용어와 책임 경계를 확인하고, 이어지는 정상 예시와 금지 예시를 비교한 뒤 실제 설정·코드·로그에서 근거를 찾아봅니다.

## **19.1 전체 코드 흐름**

이제 작성한 프로그램을 연결해 봅니다.

POST /sv/online
↓
StandardRequest
↓
TCF.process()
↓
STF
├─ Header 검증
├─ 인증·권한
├─ 거래통제
├─ Timeout
└─ 거래로그 시작
↓
TransactionDispatcher
↓
SvCustomerHandler
↓
SvCustomerFacade.selectSummary()
↓
SvCustomerService.selectSummary()
├─ SvCustomerRule.validateSummaryRequest()
├─ SvCustomerRule.validateInquiryAuthority()
├─ SvCustomerDao.selectCustomerSummary()
│ ↓
│ SvCustomerMapper.selectCustomerSummary()
└─ SvCustomerRule.validateCustomerExists()
↓
CustomerSummaryResponse
↓
ETF.success()
↓
StandardResponse

## **19.2 요청 전문**

{
"header": {
"businessCode": "SV",
"serviceId": "SV.Customer.selectSummary",
"transactionCode": "SV-INQ-0001",
"processingType": "INQUIRY",
"channelId": "WEBTOP",
"userId": "user01",
"branchId": "001234",
"requestDtm": "20260717103000123",
"contractVersion": "1.0"
},
"body": {
"customerNo": "CUST000001",
"baseDate": "20260717"
}
}

## **19.3 정상 응답**

{
"header": {
"businessCode": "SV",
"serviceId": "SV.Customer.selectSummary",
"transactionCode": "SV-INQ-0001",
"guid": "G202607170001",
"traceId": "T202607170001"
},
"result": {
"resultStatus": "SUCCESS",
"resultCode": "S0000",
"message": "정상 처리되었습니다."
},
"body": {
"customerNo": "CUST000001",
"customerName": "홍길동",
"customerGrade": "VIP",
"baseDate": "20260717"
},
"error": null
}

## **19.4 고객 없음 응답**

{
"header": {
"businessCode": "SV",
"serviceId": "SV.Customer.selectSummary",
"transactionCode": "SV-INQ-0001",
"guid": "G202607170002",
"traceId": "T202607170002"
},
"result": {
"resultStatus": "FAIL",
"resultCode": "E-SV-CUST-0002",
"message": "고객을 찾을 수 없습니다."
},
"body": null,
"error": {
"errorCode": "E-SV-CUST-0002",
"errorType": "BUSINESS"
}
}

## **19.5 테스트 계층**

거래 테스트는 한 종류만 작성하지 않습니다.

Rule 단위 테스트
→ Service 단위 테스트
→ Mapper 통합 테스트
→ TCF 거래 통합 테스트
→ 성능·Timeout 테스트

## **19.6 테스트 시나리오**

| **ID** | **시나리오** | **기대 결과** |
| --- | --- | --- |
| TC-SV-001 | 정상 고객 조회 | SUCCESS |
| TC-SV-002 | 고객번호 미입력 | Validation 오류 |
| TC-SV-003 | 고객번호 20자 초과 | Validation 오류 |
| TC-SV-004 | 기준일 형식 오류 | 업무 오류 |
| TC-SV-005 | 미래 기준일 | 업무 오류 |
| TC-SV-006 | 존재하지 않는 고객 | 고객 없음 |
| TC-SV-007 | 권한 없는 사용자 | 권한 오류 |
| TC-SV-008 | 업무코드 불일치 | Header 검증 오류 |
| TC-SV-009 | ServiceId 오타 | Handler 미등록 |
| TC-SV-010 | 거래통제 DENY | Handler 미실행 |
| TC-SV-011 | SQL Timeout | Timeout 오류 |
| TC-SV-012 | DB 연결 실패 | 시스템 오류 |
| TC-SV-013 | Mapper Namespace 오류 | 애플리케이션 오류 |
| TC-SV-014 | SQL 결과 컬럼 불일치 | 매핑 오류 |
| TC-SV-015 | 정상 TraceId 검색 | 전체 로그 연결 |

## **19.7 거래 통합 테스트 개념**

프로젝트에서 제공하는 공통 테스트 도구가 있다면 그것을 우선 사용합니다.

MockMvc 예:

@SpringBootTest
@AutoConfigureMockMvc
class CustomerSummaryTransactionTest {

@Autowired
private MockMvc mockMvc;

@Test
void shouldSelectCustomerSummary() throws Exception {
String requestJson = """
{
"header": {
"businessCode": "SV",
"serviceId": "SV.Customer.selectSummary",
"transactionCode": "SV-INQ-0001",
"processingType": "INQUIRY",
"channelId": "TEST",
"userId": "test-user",
"branchId": "001234",
"requestDtm": "20260717103000123"
},
"body": {
"customerNo": "CUST000001",
"baseDate": "20260717"
}
}
""";

mockMvc.perform(
post("/sv/online")
.contentType(MediaType.APPLICATION\_JSON)
.content(requestJson)
)
.andExpect(status().isOk())
.andExpect(
jsonPath("$.result.resultStatus")
.value("SUCCESS")
)
.andExpect(
jsonPath("$.body.customerNo")
.value("CUST000001")
);
}
}

인증 Filter가 활성화되어 있다면 테스트용 JWT 또는 인증 Context를 함께 설정해야 합니다.

## **19.8 Timeout 테스트**

테스트용 Mapper 또는 Service에서 의도적으로 지연을 발생시켜 Timeout을 확인할 수 있습니다.

OM Timeout
3000ms

테스트 실행시간
5000ms

기대 결과
TIMEOUT

운영 DB에 실제 지연 SQL을 실행하는 방식은 위험합니다.

성능시험 환경이나 테스트 대역을 사용해야 합니다.

## **19.9 로그 확인**

정상 거래 로그 예:

traceId=T202607170001
guid=G202607170001
serviceId=SV.Customer.selectSummary
transactionCode=SV-INQ-0001
businessCode=SV
result=SUCCESS
elapsedMs=145

오류 로그 예:

traceId=T202607170002
serviceId=SV.Customer.selectSummary
result=BUSINESS\_FAIL
errorCode=E-SV-CUST-0002
elapsedMs=42

SQL 로그 예:

traceId=T202607170001
mapperId=SvCustomerMapper.selectCustomerSummary
elapsedMs=35
rowCount=1

## **19.10 로그에 남기면 안 되는 정보**

JWT 원문
비밀번호
Private Key
주민등록번호 원문
계좌번호 원문
전체 요청·응답 개인정보
DB 비밀번호
SQL Parameter의 민감정보

고객번호도 프로젝트 개인정보 기준에 따라 마스킹하거나 Hash 처리할 수 있습니다.

# **제20장. OM 등록과 운영 준비**

학습 목표 | 20장. OM 등록과 운영 준비의 핵심 개념을 설명하고, 프로젝트에서 적용 위치와 검증 방법을 스스로 판단한다.

핵심 관점 | 데이터 일관성: 화면 동작보다 데이터 소유권, 상태 전이, 동시성, 트랜잭션 경계를 먼저 결정해야 합니다.

읽는 순서 | 먼저 용어와 책임 경계를 확인하고, 이어지는 정상 예시와 금지 예시를 비교한 뒤 실제 설정·코드·로그에서 근거를 찾아봅니다.

## **20.1 코드만으로 거래는 완성되지 않는다**

신규 ServiceId가 실행 가능하려면 운영 기준정보가 필요합니다.

Handler 등록
\+ OM Service Catalog
\+ 거래통제
\+ Timeout
\+ 권한
\+ 감사
\+ 로그

## **20.2 OM Service Catalog 등록**

| **항목** | **값** |
| --- | --- |
| ServiceId | SV.Customer.selectSummary |
| 서비스명 | 고객 요약정보 조회 |
| 업무코드 | SV |
| 도메인 | Customer |
| 거래코드 | SV-INQ-0001 |
| 처리유형 | INQUIRY |
| Handler | SvCustomerHandler |
| Facade Method | selectSummary |
| Timeout | 3000ms |
| 인증 필요 | Y |
| 권한코드 | SV\_CUSTOMER\_VIEW |
| 감사 대상 | Y |
| 개인정보 조회 | Y |
| 중복방지 | N |
| 허용 채널 | WEBTOP |
| 계약 버전 | 1.0 |
| 운영 상태 | ACTIVE |

## **20.3 거래통제**

초기 등록 시 운영 상태를 정의합니다.

ACTIVE
\= 실행 가능

SUSPENDED
\= 일시 중지

DISABLED
\= 사용 중지

DEPRECATED
\= 폐기 예정

운영 적용 전까지 ACTIVE로 변경하지 않고 테스트 환경에서 검증할 수 있습니다.

## **20.4 Timeout**

이번 거래의 목표 Timeout은 3000ms입니다.

Timeout은 단순히 개발자가 임의로 정하지 않습니다.

다음 항목을 고려합니다.

목표 응답시간
SQL 수행시간
외부 호출 여부
Tomcat Thread 점유
DB Pool 점유
사용자 재시도
하위 Timeout

권장 관계:

외부 호출 Timeout
< ServiceId Timeout
< Gateway Timeout
< 사용자 화면 Timeout

모든 계층의 Timeout을 같은 값으로 설정하면 상위 계층이 먼저 끊기거나 하위 작업이 계속 실행되는 문제가 발생할 수 있습니다.

## **20.5 권한**

기능권한:

SV\_CUSTOMER\_VIEW

STF 또는 공통 권한 모듈이 사용자의 권한그룹과 기능권한을 확인합니다.

추가 데이터 권한은 Service·Rule·SQL에서 처리합니다.

## **20.6 감사로그**

고객정보 조회는 개인정보 조회에 해당할 수 있으므로 감사 대상 여부를 검토합니다.

감사로그 예:

userId
branchId
serviceId
customerKeyHash
accessPurpose
result
accessDtm
traceId

고객번호 원문 전체를 감사로그에 남길지는 보안·개인정보 기준을 따라야 합니다.

## **20.7 운영자가 확인할 수 있어야 하는 정보**

현재 거래가 활성 상태인가?
Timeout은 몇 초인가?
어느 Handler가 처리하는가?
어느 권한이 필요한가?
최근 성공·실패 건수는 얼마인가?
평균·p95 응답시간은 얼마인가?
어느 SQL이 느린가?
최근 오류코드는 무엇인가?

## **20.8 CI/CD 품질 Gate**

신규 거래 배포 전 다음 항목을 자동검증할 수 있습니다.

| **Gate** | **검증 내용** |
| --- | --- |
| 컴파일 | Java·Mapper 참조 오류 없음 |
| 단위 테스트 | Rule·Service 테스트 성공 |
| 통합 테스트 | 거래와 Mapper 테스트 성공 |
| ServiceId | 중복 없음 |
| Handler | ServiceId 등록 확인 |
| OM | Catalog 등록정보 존재 |
| Mapper | Interface·Namespace 정합성 |
| SQL ID | Mapper 메서드와 일치 |
| 패키지 | 업무코드·도메인·계층 구조 준수 |
| 의존성 | Handler→Mapper 직접 참조 없음 |
| 보안 | 민감정보 로그 없음 |
| 품질 | 정적분석 기준 통과 |

## **20.9 완료 정의**

다음 조건이 모두 충족되어야 거래 개발이 완료됩니다.

\[설계\]
화면 이벤트와 ServiceId 확정
거래코드 확정
요청·응답 전문 확정
프로그램·SQL 설계 완료

\[구현\]
Handler 이하 6계층 구현
Mapper XML 구현
표준 오류코드 적용

\[운영\]
OM Catalog 등록
거래통제 등록
Timeout 등록
권한·감사 등록

\[검증\]
단위 테스트 성공
Mapper 통합 테스트 성공
거래 통합 테스트 성공
Timeout·오류 테스트 성공
TraceId 로그 확인

\[배포\]
CI/CD Gate 통과
배포·Rollback 절차 확인

# **3\. 정상 처리 흐름**

1\. 화면에서 고객번호와 기준일 입력

2\. 화면이 StandardRequest 생성

3\. Gateway 또는 업무 WAR Filter가 JWT 검증

4\. OnlineTransactionController가 요청 수신

5\. TCF.process() 시작

6\. STF가 Header·권한·거래통제·Timeout 검증

7\. Dispatcher가 SvCustomerHandler 선택

8\. Handler가 Request DTO 변환

9\. Facade가 조회 트랜잭션 시작

10\. Service가 Rule로 요청과 권한 검증

11\. DAO가 Mapper 호출

12\. Mapper가 고객 요약 SQL 실행

13\. Rule이 고객 존재 여부 검증

14\. Service가 Response DTO 생성

15\. ETF가 표준 성공 응답 생성

16\. 거래로그에 SUCCESS와 처리시간 기록

17\. 화면이 고객정보 표시

# **4\. 오류·Timeout·장애 흐름**

## **4.1 고객번호 미입력**

DTO Validation 실패
→ Handler 이하 업무 실행 안 함
→ 표준 입력 오류

## **4.2 고객 없음**

Mapper 결과 null
→ Rule에서 BusinessException
→ Facade 트랜잭션 종료
→ ETF.businessFail()
→ 고객 없음 오류 반환

## **4.3 권한 없음**

STF 또는 업무 Rule 권한 검증 실패
→ Mapper 실행 안 함
→ 권한 오류 반환
→ 감사로그 기록

## **4.4 거래통제**

OM 상태 = SUSPENDED
→ STF 거래통제 실패
→ Handler 실행 안 함
→ 거래 중지 오류 반환

## **4.5 Timeout**

Timeout = 3000ms
SQL 처리 = 5000ms
→ TimeoutExecutor 시간 초과
→ 작업 중단 시도
→ 트랜잭션 Rollback
→ ETF Timeout 오류
→ 로그에 TIMEOUT 기록

Java Future 취소만으로 실제 DB SQL이 즉시 중단된다고 보장할 수는 없습니다.

MyBatis Query Timeout과 DB Statement Timeout도 함께 설정해야 합니다.

## **4.6 DB 장애**

DB Connection 획득 실패
→ Mapper 또는 DAO 예외
→ SystemException 변환
→ ETF.systemError()
→ 시스템 오류 반환
→ 운영 경보 대상

# **5\. 정상 예시**

ServiceId
SV.Customer.selectSummary

Handler
SvCustomerHandler

Facade
SvCustomerFacade.selectSummary()

Service
SvCustomerService.selectSummary()

Rule
SvCustomerRule

DAO
SvCustomerDao.selectCustomerSummary()

Mapper
SvCustomerMapper.selectCustomerSummary()

Table
SV\_CUSTOMER\_SUMMARY

# **6\. 금지 예시**

## **6.1 업무 Controller 생성**

@RestController
public class CustomerController {

@GetMapping("/customer")
public Object getCustomer(String customerNo) {
return customerMapper.select(customerNo);
}
}

금지 사유:

-   TCF 우회
-   거래통제·Timeout·거래로그 누락
-   표준 응답 미적용
-   Handler와 ServiceId 추적 불가

## **6.2 Handler에서 Mapper 직접 호출**

return customerMapper.selectCustomerSummary(...);

금지 사유:

-   Facade·Service·Rule·DAO 책임 우회
-   트랜잭션 경계 불명확
-   테스트와 변경 영향 확대

## **6.3 Rule에서 DB 조회**

CustomerData data =
customerMapper.selectCustomer(customerNo);

금지 사유:

-   Rule이 데이터 접근에 의존
-   단위 테스트 어려움
-   부수효과 발생 가능

## **6.4 Service가 표준 응답 생성**

return StandardResponse.success(data);

금지 사유:

-   ETF 책임 침범
-   업무 코드가 프레임워크 응답 형식에 결합
-   오류 처리 중복

## **6.5 SQL 문자열 치환**

WHERE CUSTOMER\_NO = '${customerNo}'

금지 사유:

-   SQL Injection 위험
-   Parameter Binding 미사용

# **7\. 책임 경계와 RACI**

| **활동** | **AA** | **FW** | **DEV** | **DBA** | **OM** | **QA** | **OPS** |
| --- | --- | --- | --- | --- | --- | --- | --- |
| ServiceId 승인 | A | C | R | I | C | I | I |
| 패키지 구조 | A | C | R | I | I | I | I |
| Handler 구현 | C | C | R | I | I | C | I |
| Facade·Service·Rule | C | I | R | I | I | C | I |
| DAO·Mapper | C | I | R | C | I | C | I |
| SQL 검토 | C | I | R | A | I | C | I |
| 오류코드 | A | C | R | I | C | C | I |
| OM Catalog | C | I | C | I | A/R | C | C |
| Timeout | A | C | R | C | C | C | C |
| 단위 테스트 | C | I | R | I | I | A/C | I |
| 통합 테스트 | C | C | R | C | C | A/R | C |
| 운영 인수 | C | C | C | C | C | C | A/R |

R = 수행
A = 최종 책임
C = 협의
I = 공유

# **8\. 제3부 종합 체크리스트**

## **8.1 개발환경**

| **점검 항목** | **확인** |
| --- | --- |
| 기본 빌드가 성공했는가? | □ |
| 업무 WAR가 올바른가? | □ |
| 패키지 구조가 표준과 일치하는가? | □ |
| Mapper XML 경로가 설정에 포함되는가? | □ |
| 로컬 DB와 테스트 데이터가 준비되었는가? | □ |

## **8.2 거래 정의**

| **점검 항목** | **확인** |
| --- | --- |
| 화면 이벤트가 정의되었는가? | □ |
| ServiceId가 확정되었는가? | □ |
| 거래코드가 확정되었는가? | □ |
| 요청·응답 전문이 정의되었는가? | □ |
| 권한·Timeout·감사 기준이 정의되었는가? | □ |

## **8.3 프로그램**

| **점검 항목** | **확인** |
| --- | --- |
| Handler가 ServiceId를 등록하는가? | □ |
| Handler가 Facade만 호출하는가? | □ |
| Facade에 트랜잭션이 있는가? | □ |
| Service에 업무 흐름이 표현되는가? | □ |
| Rule이 DB를 직접 호출하지 않는가? | □ |
| DAO가 Mapper만 호출하는가? | □ |
| Mapper Namespace와 Java Interface가 일치하는가? | □ |
| SQL ID와 Mapper 메서드가 일치하는가? | □ |

## **8.4 보안**

| **점검 항목** | **확인** |
| --- | --- |
| Body의 사용자 ID를 신뢰하지 않는가? | □ |
| 권한검증이 서버에서 수행되는가? | □ |
| SQL에 ${}를 사용하지 않는가? | □ |
| 개인정보가 응답에 과도하게 포함되지 않는가? | □ |
| JWT와 비밀번호가 로그에 남지 않는가? | □ |
| 데이터 권한 조건이 적용되는가? | □ |

## **8.5 테스트**

| **점검 항목** | **확인** |
| --- | --- |
| Rule 단위 테스트가 있는가? | □ |
| Service 단위 테스트가 있는가? | □ |
| Mapper 통합 테스트가 있는가? | □ |
| 정상 거래 테스트가 있는가? | □ |
| 업무 오류 테스트가 있는가? | □ |
| 시스템 오류 테스트가 있는가? | □ |
| Timeout 테스트가 있는가? | □ |
| 거래통제 테스트가 있는가? | □ |
| TraceId 로그를 확인했는가? | □ |

## **8.6 운영**

| **점검 항목** | **확인** |
| --- | --- |
| OM Catalog에 ServiceId가 등록되었는가? | □ |
| Handler 정보가 등록되었는가? | □ |
| 거래통제 상태가 정의되었는가? | □ |
| Timeout이 등록되었는가? | □ |
| 권한코드가 연결되었는가? | □ |
| 감사 대상 여부가 정의되었는가? | □ |
| 운영자가 ServiceId로 조회할 수 있는가? | □ |

# **9\. 시사점**

## **9.1 핵심 아키텍처 판단**

업무 거래 구현은 다음 순서로 진행해야 합니다.

요구사항
→ 거래 식별
→ 전문 설계
→ 프로그램 구조
→ 업무 구현
→ 데이터 접근
→ 테스트
→ 운영 등록

다음 순서는 피해야 합니다.

Controller부터 생성
→ Service와 Mapper 작성
→ 기능 동작 확인
→ 나중에 ServiceId와 문서 작성

코드를 먼저 작성하고 거래 정의를 나중에 맞추면 화면·ServiceId·Handler·OM·테스트가 서로 달라질 가능성이 큽니다.

## **9.2 주요 위험**

| **위험** | **결과** |
| --- | --- |
| 기존 유사 거래 미확인 | 중복 구현 |
| 잘못된 업무 WAR 선택 | 업무 경계 훼손 |
| DTO 대신 Map 사용 | 타입·Validation 약화 |
| Handler 비대화 | 계층 책임 혼합 |
| Service의 DB 직접 접근 | 기술 결합 |
| Rule의 Mapper 호출 | 테스트 어려움 |
| Facade 트랜잭션 누락 | 데이터 정합성 위험 |
| Mapper Namespace 불일치 | 런타임 오류 |
| OM 등록 누락 | 운영 통제 불가 |
| Timeout 미검증 | Thread·DB Pool 고갈 |
| TraceId 누락 | 장애 추적 불가 |

## **9.3 우선 보완 과제**

초보 개발자는 다음 순서로 반복 실습하는 것이 좋습니다.

1\. 단건 조회 거래
2\. 목록 조회와 페이징
3\. 등록 거래
4\. 변경 거래
5\. 업무 오류와 Rollback
6\. 외부 업무 ServiceId 호출
7\. 파일 다운로드
8\. 배치와 비동기 처리

단건 조회를 정확하게 구현하지 못한 상태에서 복잡한 등록·연계 거래를 먼저 구현하면 구조적 오류가 커질 수 있습니다.

## **9.4 중장기 발전 방향**

첫 거래 구현 이후에는 다음 능력으로 확장해야 합니다.

단일 DAO 조회
→ 여러 데이터 조합
→ 동적 조회조건
→ 페이징
→ 등록·변경 트랜잭션
→ 외부 서비스 연동
→ Timeout과 재시도
→ Cache
→ Batch
→ 성능 튜닝
→ 장애 원인 추적

# **10\. 마무리말**

제3부에서는 고객 요약 조회라는 하나의 기능을 다음 전체 구조로 구현했습니다.

CustomerSummaryRequest
→ SvCustomerHandler
→ SvCustomerFacade
→ SvCustomerService
→ SvCustomerRule
→ SvCustomerDao
→ SvCustomerMapper
→ SV\_CUSTOMER\_SUMMARY
→ CustomerSummaryResponse

초보 개발자가 가장 먼저 기억해야 할 것은 클래스 문법이 아닙니다.

각 계층이 왜 존재하는지
그리고 무엇을 해서는 안 되는지

를 이해하는 것입니다.

핵심 책임은 다음과 같습니다.

Handler
\= ServiceId와 Facade 연결

Facade
\= 유스케이스와 트랜잭션 경계

Service
\= 업무 처리 순서

Rule
\= 업무 조건과 규칙

DAO
\= 데이터 접근 경계

Mapper
\= SQL 실행

거래 한 건의 진짜 완료 기준은 다음과 같습니다.

프로그램 동작
\+ 표준 준수
\+ 오류 처리
\+ 보안 적용
\+ OM 등록
\+ 테스트 증적
\+ 운영 추적

이제 초보 개발자는 요구사항을 받았을 때 단순히 Controller → Service → Mapper를 만드는 것이 아니라 다음 질문을 할 수 있어야 합니다.

어느 업무가 책임지는가?

ServiceId와 거래코드는 무엇인가?

요청과 응답 DTO는 무엇인가?

어느 Handler와 Facade가 처리하는가?

업무 규칙은 어디에 두는가?

어느 SQL과 테이블을 사용하는가?

Timeout과 권한은 무엇인가?

OM과 테스트에는 무엇을 등록하는가?

이 질문에 답하며 구현할 수 있다면 첫 번째 NSIGHT TCF 업무 거래를 완성할 수 있습니다.
