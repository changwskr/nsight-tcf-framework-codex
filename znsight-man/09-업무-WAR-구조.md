# 9. 업무 WAR 구조

> **NSIGHT TCF 개발 Manual** · 원본: [`znsight-guide-word`](../znsight-guide-word/) · 갱신: 2026-07-05

## 9. 업무 WAR 구조

※ 앞 장이 “8. Gradle 멀티 모듈 구조”이므로, 목차 기준으로는 본 장을 9. 업무 WAR 구조로 두는 것이 자연스럽습니다.

### 9.1 도입 전 안내말

본 장은 NSIGHT TCF Framework에서 업무 애플리케이션을 어떤 단위로 WAR로 구성하고, 어떤 Context Path로 배포하며, Apache/Gateway/Tomcat/Spring Boot와 어떻게 연결할 것인지 정의한다.
NSIGHT 업무 WAR는 단순히 Java 프로그램을 묶은 배포 파일이 아니다.각 WAR는 하나의 업무 경계이며, 다음 기준을 동시에 가진다.
| 구분 | 의미 | 업무코드 |
| --- | --- | --- |
| SV, IC, OM 등 업무 식별 기준 | Context Path | /sv, /ic, /om 등 URL 진입 기준 |
| WAR 파일명 | sv.war, ic.war, om.war 등 배포 산출물 | Java Package |
| com.nh.nsight.sv 등 소스 기준 | ServiceId | SV.Customer.selectSummary 등 거래 실행 기준 |
| Mapper 경로 | mapper/sv, mapper/ic 등 SQL 관리 기준 | 로그 기준 |

businessCode, serviceId, transactionCode 기준 추적
NSIGHT Tomcat 운영 기준도 17 WAR와 Spring Session JDBC 구조를 전제로 하며, Tomcat은 Spring Boot Runtime, Session Cookie 처리, Transaction 처리, Thread Pool 관리, Connection Pool 연계 역할을 수행한다. 또한 Spring Boot 운영 매뉴얼에서도 NSIGHT 업무 구성을 업무별 WAR 구조로 정리하고 있다.

### 9.2 업무 WAR 구조 결론

NSIGHT 업무 WAR 구조는 다음 기준으로 설계한다.
업무코드
```text
   ↓
Context Path
↓
WAR 파일
↓
Java Package
↓
POST /{businessCode}/online
↓
ServiceId Dispatcher
↓

```

Handler → Facade → Service → Rule → DAO/Mapper

| 구분 | 설계 기준 | 배포 단위 | 업무별 WAR | Context 기준 |
| --- | --- | --- | --- | --- |
| 업무코드 소문자 | URL 기준 | POST /{businessCode}/online | 실행 기준 | JSON Header의 serviceId |
| 공통 처리 | tcf-web, tcf-core 공통 모듈 사용 | 업무 처리 | 각 업무 WAR 내부 Handler부터 시작 | DB 접근 |
| 각 업무 WAR 내부 DAO/Mapper에서 처리 | 세션 | Spring Session JDBC + SESSIONDB 기준 공유 | 운영관리 | OM WAR에서 ServiceId, 거래통제, Timeout, 오류코드 관리 |

배포 방식
Gradle bootWar → Tomcat 배포
핵심 원칙은 다음이다.
WAR는 업무 실행 단위이고, ServiceId는 거래 실행 단위이다.
즉, /sv/online은 SV 업무 WAR로 들어가는 입구이고, 실제 실행 프로그램은 SV.Customer.selectSummary 같은 ServiceId로 결정된다.

### 9.3 전체 업무 WAR 구성

NSIGHT는 업무코드를 기준으로 업무 WAR를 분리한다.기준 업무코드는 다음처럼 정리한다.
| No | 업무코드 | 업무명 | Context Path | WAR 파일명 | Java Package |
| --- | --- | --- | --- | --- | --- |
| 1 | CC | Common | /cc | cc.war | com.nh.nsight.cc |
| 2 | IC | Integration Customer | /ic | ic.war | com.nh.nsight.ic |
| 3 | PC | Private Customer | /pc | pc.war | com.nh.nsight.pc |
| 4 | BC | Business Customer | /bc | bc.war | com.nh.nsight.bc |
| 5 | MS | Marketing Service | /ms | ms.war | com.nh.nsight.ms |
| 6 | SV | Single View | /sv | sv.war | com.nh.nsight.sv |
| 7 | PD | Product | /pd | pd.war | com.nh.nsight.pd |
| 8 | CM | Campaign | /cm | cm.war | com.nh.nsight.cm |
| 9 | EB | Event Batch | /eb | eb.war | com.nh.nsight.eb |
| 10 | EP | Event Processing | /ep | ep.war | com.nh.nsight.ep |
| 11 | BP | Batch Processing | /bp | bp.war | com.nh.nsight.bp |
| 12 | BD | Big Data | /bd | bd.war | com.nh.nsight.bd |
| 13 | SS | Statistics Service | /ss | ss.war | com.nh.nsight.ss |
| 14 | CS | Customer Segment | /cs | cs.war | com.nh.nsight.cs |
| 15 | CT | Contact | /ct | ct.war | com.nh.nsight.ct |
| 16 | MG | Message | /mg | mg.war | com.nh.nsight.mg |
| 17 | OM | Operation Management | /om | om.war | com.nh.nsight.om |

기존 업무코드 정리에서도 9개 WAR 기준으로 OM = Operation Management를 추가하여 관리업무를 별도 WAR로 두는 것이 적절하다고 정리되어 있다.

### 9.4 업무 WAR의 내부 표준 구조

각 업무 WAR는 동일한 내부 구조를 가져야 한다.SV 업무를 예로 들면 다음과 같다.
sv-service
```text
 ├─ build.gradle
 ├─ src/main/java
 │   └─ com/nh/nsight/sv
 │       ├─ SvServiceApplication.java
 │       │
 │       ├─ entry
 │       │   ├─ handler
 │       │   │   └─ SvCustomerSummaryHandler.java
 │       │   └─ facade
 │       │       └─ SvCustomerFacade.java
 │       │
 │       ├─ application
 │       │   ├─ service
 │       │   │   └─ SvCustomerService.java
 │       │   └─ rule
 │       │       └─ SvCustomerRule.java
 │       │
 │       ├─ persistence
 │       │   ├─ dao
 │       │   │   └─ SvCustomerDao.java
 │       │   └─ mapper
 │       │       └─ SvCustomerMapper.java
 │       │
 │       ├─ client
 │       ├─ support
 │       └─ config
 │
 ├─ src/main/resources
 │   ├─ application.yml
 │   ├─ application-local.yml
 │   ├─ application-dev.yml
 │   ├─ application-prd.yml
 │   ├─ mapper
 │   │   └─ sv
 │   │       └─ SvCustomerMapper.xml
 │   └─ logback-spring.xml
 │
 └─ src/test/java
```

| 영역 | 역할 | 작성 기준 |
| --- | --- | --- |
| entry.handler | ServiceId별 진입 Handler | Body 변환 후 Facade 호출 |
| entry.facade | 유스케이스 조립 | 트랜잭션 경계 관리 |
| application.service | 업무 처리 흐름 | Rule, DAO, Client 조정 |
| application.rule | 업무 규칙 | 조건 판단, 값 검증, 정책 계산 |
| persistence.dao | DB 접근 캡슐화 | Mapper 호출 |
| persistence.mapper | MyBatis Interface | XML SQL과 연결 |
| client | 외부/내부 서비스 호출 | tcf-eai 사용 |
| support | 업무 내부 지원 기능 | 업무 전용 유틸 |
| config | 업무별 설정 | Bean, Mapper Scan 등 |
| resources/mapper/{업무코드} | SQL XML | SQL ID 표준 준수 |

NSIGHT 애플리케이션 계층구조는 Handler → Facade → Service → Rule → DAO/Mapper를 기준으로 하며, MyBatis는 DAO/Mapper 계층에서 SQL 실행 책임만 갖도록 정의되어 있다.

### 9.5 업무 WAR와 TCF 공통 모듈 관계

업무 WAR는 TCF 공통 모듈을 포함하여 빌드된다.
sv.war
```text
 ├─ 업무 코드
 │   ├─ Handler
 │   ├─ Facade
 │   ├─ Service
 │   ├─ Rule
 │   └─ DAO/Mapper
 │
 ├─ TCF 공통 모듈
 │   ├─ tcf-util
 │   ├─ tcf-core
 │   ├─ tcf-web
 │   ├─ tcf-cache
 │   └─ tcf-eai
 │
 └─ 설정 파일
     ├─ application.yml
     ├─ mapper XML
     └─ logback-spring.xml
```

| 공통 모듈 | 업무 WAR에서의 역할 | tcf-util |
| --- | --- | --- |
| 공통 유틸, 날짜, 문자열, 마스킹 | tcf-core | 표준 전문, TCF, STF, ETF, Dispatcher, 예외 |
| tcf-web | Online Controller, Filter, Exception Handler | tcf-cache |
| 공통코드, ServiceId, 정책정보 Cache | tcf-eai | 업무 간 HTTP/JSON 표준 전문 호출 |
공통 모듈은 업무 WAR를 참조하지 않고, 업무 WAR가 공통 모듈을 참조한다.

| 허용: | sv-service → tcf-web → tcf-core → tcf-util |

금지:
tcf-core → sv-service
ic-service → sv-service
sv-service → om-service

### 9.6 업무 WAR 실행 흐름

업무 WAR의 온라인 거래 실행 흐름은 다음과 같다.
사용자 / WebTopSuite / UI
```text
        ↓
```

Apache 또는 Gateway
```text
        ↓
POST /sv/online
        ↓
sv.war
        ↓
OnlineTransactionController
        ↓
```

TCF.process()
```text
        ↓
```

STF.preProcess()
 - Header 검증
 - GUID / TraceId 생성
 - 세션 검증
 - 권한 검증
 - 거래통제 확인
 - Timeout 정책 확인
 - 거래로그 PROCESSING 생성
```text
        ↓
TransactionDispatcher
        ↓
```

serviceId = SV.Customer.selectSummary
```text
        ↓
SvCustomerSummaryHandler
        ↓
SvCustomerFacade
        ↓
SvCustomerService
        ↓
SvCustomerRule
        ↓
SvCustomerDao
        ↓
SvCustomerMapper
        ↓
RDW / ADW / SESSIONDB
        ↓
ETF.postProcess()
        ↓
StandardResponse

```

TCF Framework 처리 구조에서도 모든 온라인 거래는 STF 전처리 → TransactionDispatcher → Business Handler → ETF 후처리 흐름으로 실행되며, serviceId를 기준으로 Handler를 찾는 구조로 정의되어 있다.

### 9.7 Context Path 기준

각 업무 WAR는 업무코드와 동일한 Context Path를 사용한다.
| 업무코드 | Context Path | 온라인 Endpoint |
| --- | --- | --- |
| SV | /sv | POST /sv/online |
| IC | /ic | POST /ic/online |
| PC | /pc | POST /pc/online |
| CM | /cm | POST /cm/online |
| MG | /mg | POST /mg/online |
| OM | /om | POST /om/online |
| Gateway | /gw 또는 /api/tcf | POST /gw/{businessCode}/online |
업무 WAR의 Context Path와 Header의 businessCode는 일치해야 한다.

| { | "header": { |
| "businessCode": "SV", | "serviceId": "SV.Customer.selectSummary" | } |
호출 URL은 다음과 같아야 한다.

| } | |

```text
POST /sv/online
```

다음과 같은 요청은 차단 대상이다.
URL
| Header businessCode | 판단 |
| --- | --- |
| /sv/online | SV |
| 정상 | /sv/online |
| IC | 차단 |
| /om/online | SV |
| 차단 | /ic/online |
| IC | 정상 |

### 9.8 업무 WAR와 ServiceId 관계

업무 WAR는 여러 ServiceId를 포함할 수 있다.예를 들어 sv.war는 SV 업무의 여러 거래를 포함한다.
| WAR | ServiceId | 거래코드 | 설명 |
| --- | --- | --- | --- |
| sv.war | SV.Customer.selectSummary | SV-INQ-0001 | 고객 요약 조회 |
| sv.war | SV.Customer.selectDetail | SV-INQ-0002 | 고객 상세 조회 |
| sv.war | SV.Product.selectHolding | SV-INQ-0003 | 보유상품 조회 |
| sv.war | SV.Transaction.selectList | SV-INQ-0004 | 거래내역 조회 |
| sv.war | SV.Customer.updateMemo | SV-UPD-0001 | 고객 메모 변경 |

관계는 다음과 같다.
sv.war
```text
 ├─ SV.Customer.selectSummary
 ├─ SV.Customer.selectDetail
 ├─ SV.Product.selectHolding
 ├─ SV.Transaction.selectList
 └─ SV.Customer.updateMemo
```

즉, WAR는 업무 컨테이너이고, ServiceId는 실행 함수이다.

### 9.9 업무 WAR별 설정 파일 기준

각 업무 WAR는 다음 설정 파일을 가진다.
src/main/resources
```text
 ├─ application.yml
 ├─ application-local.yml
 ├─ application-dev.yml
 ├─ application-stg.yml
 ├─ application-prd.yml
 ├─ logback-spring.xml
 └─ mapper/{업무코드}/
```

| 설정 파일 | 역할 |
| --- | --- |
| application.yml | 공통 기본 설정 |
| application-local.yml | 로컬 개발 설정 |
| application-dev.yml | 개발환경 설정 |
| application-stg.yml | 검증환경 설정 |
| application-prd.yml | 운영환경 설정 |
| logback-spring.xml | 로그 출력 기준 |
| mapper/{업무코드} | 업무별 MyBatis XML |

Spring Boot 표준에서도 application.yml은 Port, Context Path, 세션, DB, HikariCP, MyBatis, Transaction, Security, Log, Actuator 설정을 통제하는 핵심 운영 기준 파일로 정의되어 있다.

### 9.10 업무 WAR의 application.yml 예시

SV 업무 예시는 다음과 같다.
```yaml
server:
  servlet:
    context-path: /sv
spring:
  application:
    name: sv-service
  profiles:
    active: local
  session:
    store-type: jdbc
mybatis:
  mapper-locations: classpath:/mapper/sv/**/*.xml
  configuration:
    default-statement-timeout: 3
    map-underscore-to-camel-case: true
nsight:
  business-code: SV
  tcf:
    online-endpoint: /sv/online
    timeout:
      online-transaction-seconds: 5
      db-query-seconds: 3
    transaction-log:
      enabled: true
```

운영 기준은 다음과 같다.
| 항목 | 기준 |
| --- | --- |
| server.servlet.context-path | 업무 Context와 일치 |
| spring.application.name | {업무코드소문자}-service |
| mybatis.mapper-locations | mapper/{업무코드소문자} |
| default-statement-timeout | 기본 3초 기준 |
| nsight.business-code | 업무코드 대문자 |
| online-transaction-seconds | 기본 5초 기준 |
| transaction-log.enabled | 운영에서는 true |

### 9.11 업무 WAR 빌드 기준

업무 WAR는 Gradle bootWar로 생성한다.
```text
./gradlew :sv-service:bootWar
```

빌드 산출물은 다음 기준으로 생성한다.
sv-service/build/libs/sv.war

업무별 WAR 파일명은 다음 기준을 따른다.
| 업무코드 | Gradle Module | WAR 파일명 |
| --- | --- | --- |
| SV | sv-service | sv.war |
| IC | ic-service | ic.war |
| PC | pc-service | pc.war |
| CM | (미포함·확장 예정) | cm.war |
| MG | mg-service | mg.war |
| OM | tcf-om | om.war |
| Gateway | tcf-gateway | gateway.war |
| JWT | tcf-jwt | jwt.war |

업무 WAR의 build.gradle 예시는 다음과 같다.
```gradle
plugins {
    id 'java'
    id 'war'
    id 'org.springframework.boot'
    id 'io.spring.dependency-management'
}
dependencies {
    implementation project(':tcf-web')
    implementation project(':tcf-core')
    implementation project(':tcf-cache')
    implementation project(':tcf-eai')
    implementation 'org.springframework.boot:spring-boot-starter-web'
    implementation 'org.springframework.boot:spring-boot-starter-validation'
    implementation 'org.mybatis.spring.boot:mybatis-spring-boot-starter'
    implementation 'com.zaxxer:HikariCP'
    providedRuntime 'org.springframework.boot:spring-boot-starter-tomcat'
    testImplementation 'org.springframework.boot:spring-boot-starter-test'
}
bootWar {
    archiveFileName = 'sv.war'
}
```

### 9.12 Tomcat 배포 구조

업무 WAR는 Tomcat의 webapps 아래에 배포된다.
$CATALINA_BASE
```text
 ├─ bin
 ├─ conf
 ├─ logs
 ├─ temp
 ├─ work
 └─ webapps
     ├─ cc.war
     ├─ ic.war
     ├─ pc.war
     ├─ sv.war
     ├─ cm.war
     ├─ mg.war
     └─ om.war
```

Tomcat 내부에서는 다음과 같이 Context가 생성된다.
| WAR 파일 | Context |
| --- | --- |
| sv.war | /sv |
| ic.war | /ic |
| om.war | /om |
| cm.war | /cm |
| mg.war | /mg |

Tomcat 운영 매뉴얼도 Apache → Tomcat Cluster → Spring Boot → Spring Session JDBC → SESSION DB → RDW/ADW 흐름을 기준으로 설명한다.

### 9.13 Apache / Gateway 라우팅 기준

Apache 또는 Gateway는 업무 Context 기준으로 대상 WAR에 라우팅한다.
사용자
```text
  ↓
GSLB
  ↓
L4
  ↓
Apache
  ↓
```

/sv/* → sv.war
/ic/* → ic.war
/om/* → om.war
/cm/* → cm.war

Apache 예시는 다음과 같다.
ProxyPass        /sv/ http://nsight-was01:8080/sv/
ProxyPassReverse /sv/ http://nsight-was01:8080/sv/

ProxyPass        /ic/ http://nsight-was01:8080/ic/
ProxyPassReverse /ic/ http://nsight-was01:8080/ic/

ProxyPass        /om/ http://nsight-was01:8080/om/
ProxyPassReverse /om/ http://nsight-was01:8080/om/

Apache는 SSL Termination, Reverse Proxy, URL Routing, Load Balancing, Sticky Session, Logging, 장애 격리 역할을 수행하도록 정의되어 있다.

### 9.14 업무 WAR와 세션 공유

업무 WAR가 여러 개로 나뉘어도 로그인 세션은 공유되어야 한다.NSIGHT는 이를 위해 Spring Session JDBC + SESSIONDB 기준을 사용한다.
사용자 로그인
```text
   ↓
```

tcf-om
```text
   ↓
Spring Session JDBC
↓
SESSIONDB
↓
SPRING_SESSION
↓

```

sv.war / ic.war / cm.war / om.war에서 동일 세션 조회

| 항목 | 기준 |
| --- | --- |
| 세션 저장소 | SESSIONDB |
| 세션 기술 | Spring Session JDBC |
| 세션 Cookie | JSESSIONID 또는 NSIGHTSID |
| 공유 범위 | 전체 업무 WAR |
| 세션 검증 위치 | TCF STF.preProcess() |
| 운영관리 | OM 세션관리 화면 |

Spring Session JDBC 운영 매뉴얼은 Spring Session JDBC가 세션 중앙화, WAS 독립 세션 관리, 장애 시 세션 유지, 다중 WAR 세션 공유, 사용자 인증 상태 유지를 담당한다고 정의한다.

### 9.15 업무 WAR 분리의 장점과 주의사항

| 구분 | 장점 | 주의사항 |
| --- | --- | --- |
| 업무 독립성 | 업무별 개발·배포 가능 | 공통 모듈 버전 일치 필요 |
| 장애 격리 | 특정 업무 장애 영향 축소 | 같은 Tomcat이면 JVM 장애는 공유 |
| 로그 분리 | 업무별 로그 관리 가능 | GUID/TraceId 기준 통합 추적 필요 |
| 보안 통제 | 업무별 권한·거래통제 가능 | 세션 공유 기준 명확화 필요 |
| 배포 편의 | 특정 WAR만 교체 가능 | Tomcat 재기동 정책 필요 |
| 운영관리 | 업무별 Health Check 가능 | Apache/Gateway 라우팅 관리 필요 |
| 확장성 | 업무별 Scale-out 가능 | L4/Apache 설정 복잡도 증가 |

### 9.16 하나의 Tomcat에 여러 WAR 배포 시 기준

하나의 Tomcat에 여러 업무 WAR를 배포할 수 있다.
Tomcat 8080
```text
 ├─ cc.war
 ├─ ic.war
 ├─ pc.war
 ├─ sv.war
 ├─ cm.war
 ├─ mg.war
 └─ om.war
```

다만 다음 기준을 지켜야 한다.
| 항목 | 기준 |
| --- | --- |
| JVM Heap | 전체 WAR가 공유 |
| Metaspace | WAR별 ClassLoader 사용으로 증가 가능 |
| Thread Pool | Tomcat Connector Thread 공유 |
| Hikari Pool | WAR별 DataSource Pool 생성 가능 |
| 로그 | WAR별 로그 파일 분리 권장 |
| 장애 | JVM 장애 발생 시 모든 WAR 영향 |
| 배포 | WAR 교체 시 영향 범위 확인 |
| 세션 | Spring Session JDBC로 중앙화 |

따라서 하나의 Tomcat에 여러 WAR를 배포하는 것은 가능하지만, 업무 중요도와 장애 격리 수준에 따라 Tomcat 인스턴스 또는 업무그룹 분리를 검토해야 한다.

### 9.17 업무그룹별 WAR 배치 예시

운영 안정성을 위해 업무그룹 단위로 Tomcat을 나눌 수 있다.
A 업무그룹 Tomcat
```text
 ├─ cc.war
 ├─ ic.war
 └─ pc.war
```

B 업무그룹 Tomcat
```text
 ├─ bc.war
 ├─ ms.war
 ├─ sv.war
 └─ pd.war
```

C 운영관리 Tomcat
```text
 ├─ om.war
 ├─ gateway.war
 └─ jwt.war
```

| 배치 방식 | 설명 | 장점 | 주의사항 |
| --- | --- | --- | --- |
| 전체 WAR 단일 Tomcat | 모든 WAR를 하나의 Tomcat에 배포 | 단순함 | 장애 영향 큼 |
| 업무그룹별 Tomcat | 업무 성격별 Tomcat 분리 | 장애 격리 우수 | 운영 복잡도 증가 |
| 핵심업무 분리 | SV, OM, Gateway 별도 분리 | 중요 업무 보호 | 자원 추가 필요 |
| 1 WAR 1 Tomcat | 완전 분리 | 격리 최고 | 운영·메모리 비용 큼 |

### 9.18 WAR 배포와 로그 구조

업무 WAR는 로그를 업무별로 분리해야 한다.
/logs/nsight
```text
 ├─ sv
 │   ├─ application.log
 │   ├─ transaction.log
 │   └─ error.log
 ├─ ic
 │   ├─ application.log
 │   ├─ transaction.log
 │   └─ error.log
 └─ om
     ├─ application.log
     ├─ audit.log
     └─ error.log
```

| 로그 | 설명 |
| --- | --- |
| Application Log | 업무 처리 로그 |
| Transaction Log | 거래 처리 상태 로그 |
| Error Log | 오류 로그 |
| Audit Log | 권한, 다운로드, 설정 변경 감사 |
| SQL Log | SQL ID, 실행시간, 오류 추적 |
| Access Log | Apache 또는 Tomcat 접근 로그 |

모든 로그는 GUID, TraceId, serviceId, transactionCode, businessCode 기준으로 추적 가능해야 한다.

### 9.19 업무 WAR Health Check 기준

각 업무 WAR는 Health Check Endpoint를 가져야 한다.
| Health Check | 설명 |
| --- | --- |
| Liveness | 프로세스가 살아 있는지 확인 |
| Readiness | 업무 요청 수신 가능한지 확인 |
| DB Check | RDW/ADW 연결 확인 |
| SessionDB Check | SESSIONDB 연결 확인 |
| TCF Check | Dispatcher, ServiceId Registry 확인 |
| Smoke Check | 대표 ServiceId 호출 확인 |

예시는 다음과 같다.
/sv/actuator/health
/ic/actuator/health
/om/actuator/health

운영 배포 후에는 단순히 WAR 파일이 올라갔는지가 아니라, 대표 ServiceId가 정상 응답하는지 Smoke Test까지 수행해야 한다.

### 9.20 업무 WAR 배포 체크리스트

| 점검 항목 | 확인 |
| --- | --- |
| WAR 파일명이 업무코드 기준과 일치하는가 | □ |
| Context Path가 업무코드와 일치하는가 | □ |
| server.servlet.context-path가 올바른가 | □ |
| spring.application.name이 올바른가 | □ |
| Mapper XML 경로가 업무코드 기준인가 | □ |
| ServiceId가 OM에 등록되어 있는가 | □ |
| 거래통제 기준이 등록되어 있는가 | □ |
| Timeout 정책이 등록되어 있는가 | □ |
| 오류코드가 등록되어 있는가 | □ |
| Health Check가 정상인가 | □ |
| 대표 ServiceId Smoke Test가 성공했는가 | □ |
| 로그 파일이 업무별로 분리되는가 | □ |
| SESSIONDB 세션 조회가 정상인가 | □ |
| HikariCP Pool 설정이 적정한가 | □ |
| MyBatis Query Timeout이 설정되어 있는가 | □ |
| Apache 또는 Gateway 라우팅이 정상인가 | □ |
| 롤백 대상 이전 WAR가 보관되어 있는가 | □ |

### 9.21 금지사항

| 금지사항 | 사유 |
| --- | --- |
| WAR 파일명 임의 지정 | CI/CD, 배포, 롤백 오류 발생 |
| Context Path와 업무코드 불일치 | 라우팅 및 거래통제 오류 |
| 업무 WAR 간 Java 직접 참조 | 배포 독립성 훼손 |
| Handler에서 DB 직접 호출 | 계층 책임 위반 |
| 업무 WAR에서 OM 기준정보 직접 수정 | 운영통제 우회 |
| 운영 DB 접속정보 WAR 내부 하드코딩 | 보안 위험 |
| 업무별 Timeout 무제한 설정 | Thread 고갈 위험 |
| Mapper XML 공통 디렉터리 혼재 | SQL 관리 어려움 |
| 모든 WAR를 무조건 하나의 Tomcat에 배치 | 장애 격리 불리 |
| 배포 후 Smoke Test 생략 | 장애 조기 발견 실패 |

### 9.22 마무리말

업무 WAR 구조는 NSIGHT TCF Framework의 실제 운영 단위를 결정한다.업무 WAR가 명확히 분리되어야 개발, 빌드, 배포, 로그, 장애 대응, 권한, 거래통제, Timeout 정책이 모두 업무 단위로 관리될 수 있다.
NSIGHT에서 업무 WAR의 기준은 다음 한 문장으로 정리할 수 있다.
업무코드는 Context가 되고, Context는 WAR가 되며, WAR 안의 실제 거래는 ServiceId로 실행된다.

### 9.23 시사점

업무 WAR 구조를 잘못 잡으면 개발 단계에서는 문제가 없어 보이더라도 운영 단계에서 다음 문제가 발생한다.
| 문제 | 영향 |
| --- | --- |
| 업무코드와 Context 불일치 | Apache/Gateway 라우팅 오류 |
| WAR와 Package 불일치 | 유지보수 혼란 |
| ServiceId 관리 부재 | Dispatcher 실행 오류 |
| 세션 공유 기준 부재 | WAR 간 로그인 상태 불일치 |
| 로그 기준 부재 | 장애 추적 어려움 |
| 공통 모듈 직접 수정 남발 | 전체 업무 영향 확대 |
| 업무 WAR 간 직접 참조 | 독립 배포 불가 |

따라서 NSIGHT 개발자는 신규 업무를 만들 때 반드시 다음 순서로 설계해야 한다.
업무코드 확정
```text
   ↓
```

Context Path 확정
```text
   ↓
```

WAR 파일명 확정
```text
   ↓
```

Package 구조 생성
```text
   ↓
```

ServiceId 설계
```text
   ↓
```

Handler / Facade / Service / Rule / DAO / Mapper 작성
```text
   ↓
```

OM 기준정보 등록
```text
   ↓
```

Gradle bootWar 생성
```text
   ↓
```

Tomcat 배포
```text
   ↓
```

Health Check / Smoke Test

결론적으로 업무 WAR 구조는 단순 배포 구조가 아니라, NSIGHT 업무 개발 표준과 운영 통제의 출발점이다.

## 소스·관련 문서

| 참고 |
|------|

> znsight-guide-word: `통합 (14).docx`

| [16-deploy.md](../docs/architecture/16-deploy.md) |

## 코드베이스 정정 (develop 기준)

| 항목 | 값 |
|------|-----|
| 업무 WAR | ic, pc, ms, sv, pd, eb, ep, ss, mg + tcf-om |
| ztomcat deploy | `ztomcat/deploy-wars.sh` 13 WAR |
| buildZtomcatWars | 15 WAR |
| bootRun | gateway 8100, uj 8102, jwt 8110, ui 8099 |


---

← [8. Gradle 멀티 모듈 구조](./08-Gradle-멀티모듈.md) · [10. bootRun / Tomcat WAR 배포 차이](./10-bootRun-Tomcat-WAR-차이.md) →