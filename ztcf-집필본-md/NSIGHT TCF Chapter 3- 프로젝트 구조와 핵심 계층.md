<!-- source: ztcf-집필본/NSIGHT TCF Chapter 3- 프로젝트 구조와 핵심 계층.docx -->
<!-- tables: mammoth-html + placeholder table-to-md -->

# 제3장. 프로젝트 구조와 핵심 계층

## 이 장을 시작하며

제2장에서는 화면에서 시작된 거래가 Gateway, TCF, 업무 프로그램과 데이터베이스를 거쳐 다시 화면으로 돌아오는 전체 흐름을 살펴보았다.

이제 다음 질문에 답해야 한다.

그 프로그램들은 소스 저장소의 어디에 있는가?

어느 모듈을 실행해야 하는가?

어느 모듈은 단독으로 실행되지 않는가?

업무 코드는 어느 패키지에 작성해야 하는가?

Controller, Handler, Facade, Service, Rule,
DAO, Mapper는 왜 나누는가?

DTO와 Domain 객체는 무엇이 다른가?

계층을 건너뛰면 실제로 어떤 문제가 발생하는가?

처음 프로젝트를 내려받은 개발자는 대개 소스 파일 수와 디렉터리 수에 압도된다.

root
├─ tcf-core
├─ tcf-web
├─ tcf-util
├─ tcf-gateway
├─ tcf-jwt
├─ tcf-om
├─ tcf-eai
├─ tcf-cache
├─ tcf-batch
├─ sv-service
├─ ic-service
├─ pc-service
└─ ...

이때 모든 디렉터리를 순서대로 읽는 것은 효율적이지 않다.

먼저 다음 세 가지 기준으로 프로젝트를 나눠야 한다.

첫째, 혼자 실행되는 모듈인가?
둘째, 다른 모듈이 가져다 쓰는 라이브러리인가?
셋째, 실제 업무를 소유하는 업무 WAR인가?

그리고 업무 WAR 내부에서는 다시 다음 기준으로 구조를 나눈다.

어떤 업무인가?
→ 어느 도메인인가?
→ 어느 계층의 책임인가?

NSIGHT TCF의 권장 업무 패키지는 업무코드 → 도메인 → 계층 순서로 구성하며, 전체 공통 루트는 com.nh.nsight, TCF 플랫폼은 com.nh.nsight.tcf, 업무 프로그램은 com.nh.nsight.marketing 아래에 배치하는 구조를 기준으로 한다.

## 핵심 관점

프로젝트 구조는 파일을 보기 좋게 정리한 폴더 체계가 아니다.

모듈 구조
\= 빌드·배포·장애 경계

패키지 구조
\= 업무·도메인·책임 경계

계층 구조
\= 호출·트랜잭션·변경 경계

DTO 구조
\= 시스템과 계층 사이의 데이터 계약

구조를 이해하지 못한 상태에서 기존 코드를 복사하면 기능은 동작할 수 있다.

하지만 다음 문제가 뒤늦게 나타난다.

| 문제 | 결과 |
| --- | --- |
| 실행 모듈 오인 | 애플리케이션을 기동하지 못함 |
| 잘못된 의존성 추가 | 순환 의존·라이브러리 충돌 |
| 패키지 위치 오류 | Component Scan·Mapper Scan 실패 |
| 계층 우회 | 트랜잭션·예외처리·테스트 구조 훼손 |
| DTO 무분별한 공유 | 화면·업무·DB 구조 강결합 |
| 업무 WAR 간 직접 참조 | 독립 배포 불가능 |
| 공통 모듈 오염 | 업무 변경이 전체 시스템에 전파 |
| Mapper 직접 사용 | 데이터 소유권과 감사 책임 훼손 |

## 학습 목표

이 장을 마치면 다음 내용을 설명하고 직접 확인할 수 있어야 한다.

| 번호 | 학습 목표 |
| --- | --- |
| 1 | Gradle 멀티모듈 프로젝트의 의미를 설명한다. |
| 2 | 실행 모듈과 라이브러리 모듈을 구분한다. |
| 3 | settings.gradle과 build.gradle의 역할을 설명한다. |
| 4 | 플랫폼 모듈과 업무 WAR의 책임을 구분한다. |
| 5 | 업무코드·도메인·계층 패키지 구조를 찾는다. |
| 6 | Controller와 Handler의 차이를 설명한다. |
| 7 | Handler·Facade·Service·Rule·DAO·Mapper 책임을 구분한다. |
| 8 | Request·Response·Query·Result DTO를 구분한다. |
| 9 | DTO와 Entity·Value Object 같은 Domain 객체를 구분한다. |
| 10 | 계층 위반이 트랜잭션·테스트·운영에 미치는 영향을 설명한다. |
| 11 | 하나의 ServiceId에서 SQL과 테이블까지 역추적한다. |
| 12 | Gradle 의존성 오류를 추측이 아니라 명령 결과로 분석한다. |

# 한눈에 보는 프로젝트 구조

┌───────────────────────────────────────────────────────────┐
│ Git 저장소 Root │
│ │
│ settings.gradle │
│ build.gradle │
│ gradle.properties │
│ gradlew / gradlew.bat │
│ │
│ ┌───────────────────────────────────────────────────────┐ │
│ │ TCF 플랫폼 모듈 │ │
│ │ │ │
│ │ tcf-util │ │
│ │ tcf-core │ │
│ │ tcf-web │ │
│ │ tcf-eai │ │
│ │ tcf-cache │ │
│ │ tcf-gateway │ │
│ │ tcf-jwt │ │
│ │ tcf-om │ │
│ │ tcf-batch │ │
│ └───────────────────────────────────────────────────────┘ │
│ │
│ ┌───────────────────────────────────────────────────────┐ │
│ │ 업무 애플리케이션 모듈 │ │
│ │ │ │
│ │ sv-service ic-service pc-service │ │
│ │ ms-service pd-service eb-service │ │
│ │ ep-service ss-service mg-service ... │ │
│ └───────────────────────────────────────────────────────┘ │
└───────────────────────────────────────────────────────────┘
│
▼
Gradle 의존성·빌드
│
┌───────────────┴────────────────┐
▼ ▼
공통 JAR 실행 JAR·WAR
다른 모듈이 사용 Spring Boot·Tomcat

# 모듈을 이해하는 네 가지 관점

| 관점 | 질문 | 확인 대상 |
| --- | --- | --- |
| 빌드 | 어떤 모듈이 함께 빌드되는가? | settings.gradle |
| 의존성 | 이 모듈은 무엇을 참조하는가? | build.gradle |
| 실행 | 어느 클래스가 기동점인가? | \*Application.java |
| 배포 | 무엇이 서버에 배포되는가? | JAR·WAR·Context Path |

# 프로젝트 구조 상태 표기

소스를 분석할 때는 다음 상태를 구분하는 것이 좋다.

| 상태 | 의미 |
| --- | --- |
| 구현 확인 | 현재 소스에서 클래스·설정·실행 경로가 확인됨 |
| 부분 구현 | 골격은 있으나 운영 기능이 일부 미완성 |
| 설계 기준 | 문서상 표준이며 프로젝트 적용 여부를 확인해야 함 |
| 권장 확장 | 현재 구조를 유지하며 추가 구현하는 항목 |
| 확인 필요 | 환경·브랜치·배포방식에 따라 달라지는 항목 |

# 3.1 Gradle 멀티모듈과 실행 단위

## 3.1.1 멀티모듈 프로젝트란 무엇인가

멀티모듈 프로젝트는 하나의 저장소 안에 여러 개의 독립 빌드 단위를 두는 구조다.

하나의 Git 저장소
├─ 공통 프레임워크
├─ 인증 모듈
├─ Gateway
├─ 운영관리
├─ 배치
└─ 여러 업무 애플리케이션

모든 코드를 하나의 모듈에 넣을 수도 있다.

그러나 그렇게 하면 다음 문제가 발생한다.

| 단일 모듈 문제 | 영향 |
| --- | --- |
| 모든 코드가 한 산출물에 포함 | 불필요한 코드까지 배포 |
| 업무와 공통 코드 혼합 | 책임 조직 불명확 |
| 작은 변경도 전체 빌드 | 배포 영향 확대 |
| 라이브러리 경계 부재 | 순환 참조 증가 |
| 테스트 범위 확대 | 빌드 시간 증가 |
| 장애 격리 어려움 | 특정 업무 문제의 영향 확산 |

멀티모듈은 이 문제를 해결하기 위해 역할별 경계를 만든다.

## 3.1.2 settings.gradle의 역할

settings.gradle은 멀티모듈 프로젝트에 참여하는 모듈을 선언한다.

개념적인 예시는 다음과 같다.

rootProject.name = "nsight-tcf"

include "tcf-util"
include "tcf-core"
include "tcf-web"
include "tcf-eai"
include "tcf-cache"
include "tcf-gateway"
include "tcf-jwt"
include "tcf-om"
include "tcf-batch"

include "sv-service"
include "ic-service"
include "pc-service"
include "ms-service"
include "pd-service"
include "eb-service"
include "ep-service"
include "ss-service"
include "mg-service"

실제 참여 모듈은 사용하는 브랜치의 settings.gradle을 기준으로 확인한다.

### settings.gradle에서 확인할 내용

| 확인 항목 | 의미 |
| --- | --- |
| rootProject.name | 전체 프로젝트 이름 |
| include | 빌드에 참여하는 모듈 |
| projectDir | 모듈 실제 경로 |
| Plugin Management | Plugin 저장소·버전 |
| Dependency Resolution | Maven 저장소 정책 |

### 흔한 문제

| 증상 | 원인 후보 |
| --- | --- |
| IDE에서 모듈이 보이지 않음 | include 누락 |
| Gradle Task가 없음 | 모듈 인식 실패 |
| Project Dependency 실패 | 모듈명 오타 |
| 다른 경로에서 모듈 탐색 | projectDir 불일치 |

## 3.1.3 루트 build.gradle의 역할

루트 빌드 파일은 프로젝트 전체 공통 정책을 정의한다.

Java 버전
Spring Boot 버전
공통 저장소
공통 테스트 설정
컴파일 인코딩
공통 Plugin
Dependency 관리

개념 예시는 다음과 같다.

subprojects {
apply plugin: "java"

group = "com.nh.nsight"
version = "1.0.0"

java {
toolchain {
languageVersion = JavaLanguageVersion.of(21)
}
}

repositories {
mavenCentral()
}

tasks.withType(JavaCompile).configureEach {
options.encoding = "UTF-8"
}

test {
useJUnitPlatform()
}
}

루트 설정과 개별 모듈 설정이 충돌하면 빌드 결과가 환경마다 달라질 수 있다.

## 3.1.4 개별 모듈 build.gradle의 역할

각 모듈의 빌드 파일은 해당 모듈의 성격을 정의한다.

| 설정 | 설명 |
| --- | --- |
| Plugin | Java Library·Spring Boot·WAR 여부 |
| Dependency | 참조하는 다른 모듈·외부 라이브러리 |
| Packaging | JAR·Boot JAR·WAR |
| Test | 단위·통합 테스트 의존성 |
| Resource | Mapper XML·설정 파일 포함 |
| Boot 설정 | Main Class·WAR 설정 |

### 업무 모듈 의존성 예시

dependencies {
implementation project(":tcf-core")
implementation project(":tcf-web")
implementation project(":tcf-util")

implementation "org.mybatis.spring.boot:mybatis-spring-boot-starter"
runtimeOnly "com.oracle.database.jdbc:ojdbc11"

testImplementation "org.springframework.boot:spring-boot-starter-test"
}

이 예시는 구조 설명용이다. 실제 의존성은 프로젝트 빌드 파일을 우선한다.

## 3.1.5 라이브러리 모듈과 실행 모듈

모든 모듈이 단독 실행되는 것은 아니다.

### 라이브러리 모듈

| 모듈 | 대표 책임 | 단독 실행 |
| --- | --- | --- |
| tcf-util | 공통 Utility·값 객체 | 아니오 |
| tcf-core | TCF·STF·ETF·Dispatcher | 아니오 |
| tcf-web | 공통 Controller·Filter | 일반적으로 아니오 |
| tcf-eai | 내부·외부 연계 공통 기능 | 일반적으로 아니오 |
| tcf-cache | 캐시 공통 추상화 | 구성에 따라 |
| 공통 Contract | DTO·Interface | 아니오 |

라이브러리 모듈은 다른 실행 모듈의 classpath에 포함된다.

sv-service
├─ tcf-web
├─ tcf-core
└─ tcf-util

### 실행 모듈

| 모듈 유형 | 대표 예 | 실행 목적 |
| --- | --- | --- |
| 업무 애플리케이션 | sv-service | SV 업무 거래 |
| Gateway | tcf-gateway | 인증·라우팅 |
| JWT | tcf-jwt | 토큰 발급·갱신 |
| 운영관리 | tcf-om | 기준정보·운영통제 |
| Batch | tcf-batch | 배치·Scheduler |
| UI 연계 | tcf-ui, tcf-uj | UI 지원 기능 |

### 실행 모듈을 찾는 방법

@SpringBootApplication 검색
↓
main(String\[\] args) 검색
↓
bootRun Task 확인
↓
bootJar 또는 bootWar 확인
↓
server.port·context-path 확인

## 3.1.6 JAR와 WAR의 차이

| 구분 | JAR | WAR |
| --- | --- | --- |
| 일반 용도 | Java 라이브러리·내장 서버 | 외부 Tomcat 배포 |
| 실행 | java -jar 가능 | Tomcat이 배포 |
| 서버 | 내장 Tomcat 가능 | 외부 Tomcat |
| Context Path | 애플리케이션 설정 | WAR명·Tomcat 설정 |
| 배포 단위 | Boot JAR | 업무 WAR |

Spring Boot WAR는 로컬에서는 내장 서버로 실행하고 운영에서는 외부 Tomcat에 배포하는 구성을 사용할 수도 있다.

이 경우 반드시 두 실행경로를 모두 시험해야 한다.

로컬 bootRun 성공
≠ 외부 Tomcat WAR 배포 성공

## 3.1.7 모듈 의존성 방향

권장 의존성은 공통 모듈에서 업무 모듈로 향하지 않는다.

허용:

업무 WAR
→ tcf-web
→ tcf-core
→ tcf-util

금지:

tcf-core
→ sv-service

sv-service
→ ic-service Java Class 직접 참조

tcf-util
→ 업무 DTO

### 의존성 원칙

| 원칙 | 설명 |
| --- | --- |
| 플랫폼 독립성 | TCF 공통 모듈은 특정 업무를 몰라야 함 |
| 업무 독립성 | 업무 WAR 간 직접 Project Dependency 금지 |
| 하위 공통화 | 진정한 공통 기능만 플랫폼으로 승격 |
| 순환 금지 | A→B→A 형태 차단 |
| 계약 우선 | 다른 업무는 ServiceId·Contract로 호출 |

다른 업무 WAR 간 연동은 Java 클래스를 직접 Import하기보다 ServiceId 기반 표준 거래 또는 승인된 계약을 사용해야 한다.

## 3.1.8 Gradle 의존성 분석

의존성 오류가 발생했다고 버전을 임의로 변경하면 다른 모듈이 깨질 수 있다.

먼저 의존성 경로를 확인한다.

### 전체 의존성 확인

./gradlew :sv-service:dependencies

### 특정 라이브러리 경로 확인

./gradlew :sv-service:dependencyInsight \\
\--dependency jackson-databind \\
\--configuration runtimeClasspath

### 주요 명령

| 명령 | 목적 |
| --- | --- |
| ./gradlew projects | 참여 모듈 확인 |
| ./gradlew tasks | 실행 가능한 Task 확인 |
| ./gradlew build | 전체 빌드 |
| ./gradlew :tcf-core:test | 특정 모듈 테스트 |
| ./gradlew :sv-service:bootRun | 업무 모듈 실행 |
| ./gradlew :sv-service:bootWar | WAR 생성 |
| ./gradlew dependencies | 의존성 트리 |
| ./gradlew dependencyInsight | 충돌 경로 분석 |
| ./gradlew clean build | 깨끗한 전체 빌드 |

Windows에서는 gradlew.bat를 사용할 수 있다.

## 3.1.9 프로젝트 모듈 관계도

┌─────────────┐
│ tcf-util │
└──────▲──────┘
│
┌──────┴──────┐
│ tcf-core │
└──────▲──────┘
│
┌─────────────────────┼─────────────────────┐
│ │ │
┌──────┴──────┐ ┌──────┴──────┐ ┌──────┴──────┐
│ tcf-web │ │ tcf-eai │ │ tcf-cache │
└──────▲──────┘ └──────▲──────┘ └──────▲──────┘
│ │ │
└──────────────┬──────┴──────────────┬──────┘
│ │
┌──────┴──────┐ ┌──────┴──────┐
│ sv-service │ │ ic-service │
└─────────────┘ └─────────────┘

실제 의존성 방향은 각 모듈의 build.gradle로 검증해야 한다.

## 3.1.10 실행 단위와 배포 단위 비교

| 구분 | 모듈 | 실행 단위 | 배포 단위 | 장애 단위 |
| --- | --- | --- | --- | --- |
| 공통 Core | tcf-core | 업무 WAR 내부 | JAR 의존성 | 사용하는 WAR |
| 공통 Web | tcf-web | 업무 WAR 내부 | JAR 의존성 | 사용하는 WAR |
| Gateway | tcf-gateway | 독립 프로세스·WAR | JAR·WAR | Gateway |
| 업무 | sv-service | Spring Context | WAR | 배포 Tomcat 구조에 따라 |
| 운영 | tcf-om | 독립 업무·운영 영역 | WAR | OM |
| Batch | tcf-batch | Scheduler·Job | JAR·WAR | Batch 인스턴스 |

하나의 Tomcat에 여러 WAR를 배포하면 업무 코드는 분리되어도 JVM·Heap·GC·Connector Thread는 공유된다. 따라서 모듈 경계와 런타임 장애 경계는 같지 않을 수 있다.

# 3.2 Controller·Service·Repository·Mapper

## 3.2.1 일반 웹 계층과 NSIGHT TCF 계층

일반적인 Spring 웹 애플리케이션은 다음 구조를 많이 사용한다.

Controller
↓
Service
↓
Repository
↓
DB

NSIGHT TCF에서는 공통 Controller와 거래 제어 계층이 추가되며, 업무 내부도 더 세분화된다.

OnlineTransactionController
↓
TCF
↓
STF
↓
TransactionDispatcher
↓
Handler
↓
Facade
↓
Service
├─ Rule
└─ DAO
↓
Mapper
↓
DB
↓
ETF

원본 목차의 Repository 개념은 NSIGHT TCF에서는 주로 DAO + Mapper 책임으로 구체화된다.

## 3.2.2 Controller의 역할

NSIGHT TCF의 온라인 거래는 업무별 Controller를 만드는 방식이 기본이 아니다.

공통 OnlineTransactionController가 요청을 받고 TCF에 위임한다.

### Controller 책임

| 허용 책임 | 설명 |
| --- | --- |
| HTTP 요청 수신 | JSON 요청 역직렬화 |
| Header 보완 | URL 업무코드·Client IP |
| TCF 호출 | tcf.process() |
| HTTP 응답 반환 | StandardResponse |

### Controller 금지 책임

| 금지 | 이유 |
| --- | --- |
| 고객 존재 여부 판단 | 업무 책임 침범 |
| Mapper 호출 | 계층 우회 |
| 트랜잭션 시작 | 업무 유스케이스 경계 불명확 |
| ServiceId별 복잡한 분기 | Dispatcher·Handler 책임 |
| JWT 직접 파싱 반복 | 공통 Filter 책임 |
| 표준 오류코드 임의 생성 | ETF·오류체계 책임 |

## 3.2.3 Handler의 역할

Handler는 ServiceId와 업무 유스케이스를 연결하는 업무 진입점이다.

ServiceId
↓
Handler
↓
Facade Method

### Handler 책임

| 책임 | 설명 |
| --- | --- |
| ServiceId 선언 | serviceId() 또는 serviceIds() |
| 요청 Body 변환 | Map → Request DTO |
| ServiceId 분기 | 도메인 Handler 내 거래 선택 |
| Facade 호출 | 실제 유스케이스 위임 |
| 최소 진입 로그 | ServiceId·GUID |

### Handler 금지

Handler → DAO
Handler → Mapper
Handler에 @Transactional
Handler에서 StandardResponse 직접 생성
Handler에서 SQL 작성

최신 코드 기준에서는 ServiceId마다 별도 Handler를 생성하기보다, 도메인 단위 Handler가 serviceIds()로 여러 ServiceId를 등록하는 구조를 우선 적용할 수 있다.

## 3.2.4 Facade의 역할

Facade는 하나의 사용자 유스케이스를 조립하고 트랜잭션 경계를 제공한다.

Handler
↓
Facade
├─ 입력 변환
├─ 여러 Service 조합
├─ 트랜잭션 시작·종료
└─ 결과 조립

### Facade 책임

| 책임 | 설명 |
| --- | --- |
| 유스케이스 조립 | 업무 흐름의 시작과 종료 |
| 트랜잭션 경계 | @Transactional |
| 복수 Service 호출 | 하나의 업무 목적 조합 |
| Query·Command 변환 | 계층 계약 변환 |
| 결과 조립 | Response DTO 생성 준비 |

### 조회 거래 예시

@Transactional(readOnly = true, timeout = 3)
public CustomerSummaryResponse selectSummary(
CustomerSummaryRequest request) {

return customerService.selectSummary(request);
}

### 변경 거래 예시

@Transactional(timeout = 3)
public CustomerMemoResponse updateMemo(
CustomerMemoCommand command) {

customerService.updateMemo(command);
return customerService.selectMemo(command.customerNo());
}

트랜잭션 경계는 무조건 모든 메서드에 붙이는 것이 아니라 하나의 업무 일관성을 보장해야 하는 유스케이스 범위로 설정한다.

## 3.2.5 Service의 역할

Service는 실제 업무 흐름을 수행한다.

입력
↓
업무 Rule 검증
↓
DAO 조회·변경
↓
외부 Contract 호출
↓
결과 조립

### Service가 판단하는 것

| 판단 | 예시 |
| --- | --- |
| 업무 존재 여부 | 고객이 존재하는가 |
| 상태 | 현재 수정 가능한 상태인가 |
| 권한 범위 | 해당 지점 고객인가 |
| 중복 | 동일 데이터가 등록되어 있는가 |
| 계산 | 등급·점수·한도 |
| 처리 순서 | 조회 후 검증 후 변경 |

### Service 금지

| 금지 | 이유 |
| --- | --- |
| StandardResponse 생성 | ETF 책임 |
| HTTP Request 직접 접근 | 웹 계층 결합 |
| 타 WAR Mapper 직접 호출 | 경계·소유권 위반 |
| 화면 메시지 직접 조립 | UI 계약 결합 |
| 무분별한 @Transactional | 경계 중복 |

## 3.2.6 Rule의 역할

Rule은 업무 규칙과 상태 전이를 판단한다.

입력값·현재 상태
↓
Rule
├─ 허용
└─ BusinessException

### Rule 예시

public void validateMemoUpdate(
CustomerStatus status,
String memo) {

if (status == CustomerStatus.CLOSED) {
throw new BusinessException(
"SV-CUSTOMER-0101",
"해지 고객의 메모는 변경할 수 없습니다."
);
}

if (memo == null || memo.length() > 500) {
throw new BusinessException(
"SV-CUSTOMER-0102",
"메모는 500자 이내로 입력해야 합니다."
);
}
}

Rule은 가능한 한 부작용 없는 로직으로 만든다.

### Rule 금지

Rule → DAO
Rule → Mapper
Rule → 외부 HTTP Client
Rule → 세션
Rule → 파일 저장

DB 조회가 필요한 규칙이라면 Service가 데이터를 조회한 후 Rule에 전달한다.

## 3.2.7 DAO의 역할

DAO는 Service와 Mapper 사이의 데이터 접근 경계다.

### DAO 책임

| 책임 | 설명 |
| --- | --- |
| Mapper 호출 | SQL 실행 위임 |
| Parameter 변환 | Query·Command → Mapper Parameter |
| 결과 변환 | DB Result → Result DTO |
| 데이터 원천 선택 | RDW·ADW·업무 DB |
| 데이터 접근 예외 전달 | 공통 예외체계 연계 |

### DAO 예시

@Repository
@RequiredArgsConstructor
public class SvCustomerDao {

private final SvCustomerMapper mapper;

public CustomerSummaryResult selectSummary(
CustomerSummaryQuery query) {

return mapper.selectSummary(query);
}
}

### DAO 금지

| 금지 | 이유 |
| --- | --- |
| 고객 상태변경 가능 여부 판단 | Service·Rule 책임 |
| 사용자 메시지 결정 | 업무·응답 책임 |
| HTTP Client 호출 | Client Adapter 책임 |
| 다른 도메인 Mapper 호출 | 데이터 소유권 위반 |
| Commit·Rollback 직접 제어 | Facade 트랜잭션 책임 |

## 3.2.8 Mapper의 역할

Mapper는 MyBatis SQL 실행 계약이다.

Mapper Interface
↕ namespace·method ID
Mapper XML
↓
SQL
↓
Table·View

### Mapper Interface

@Mapper
public interface SvCustomerMapper {

CustomerSummaryResult selectSummary(
CustomerSummaryQuery query);

int updateMemo(
CustomerMemoCommand command);
}

### Mapper XML

<mapper namespace="com.nh.nsight.marketing.sv.customer.mapper.SvCustomerMapper">

<select id="selectSummary"
parameterType="CustomerSummaryQuery"
resultType="CustomerSummaryResult">
SELECT CUSTOMER\_NO,
CUSTOMER\_NAME,
CUSTOMER\_GRADE
FROM SV\_CUSTOMER\_SUMMARY
WHERE CUSTOMER\_NO = #{customerNo}
</select>

</mapper>

DB 객체는 ServiceId, Handler, DAO, Mapper, SQL ID, Table과 연결해 관리해야 한다.

## 3.2.9 계층별 책임 종합표

| 계층 | 핵심 책임 | 호출 가능 | 대표 금지 |
| --- | --- | --- | --- |
| Controller | HTTP 진입 | TCF | Mapper·업무 판단 |
| TCF | 공통 실행 통제 | STF·Dispatcher·ETF | 업무 SQL |
| Handler | ServiceId 진입 | Facade | DAO·트랜잭션 |
| Facade | 유스케이스·트랜잭션 | Service | Mapper 직접 호출 |
| Service | 업무 흐름 | Rule·DAO·Client | StandardResponse |
| Rule | 업무규칙 | 순수 Utility | DB·외부 호출 |
| DAO | 데이터 접근 추상화 | Mapper | 업무 판단 |
| Mapper | SQL 실행 | DB | 업무 흐름 |
| Client | 외부 계약 호출 | 외부 API | 내부 DB 직접 변경 |

업무 프로그램의 표준 책임은 Handler → Facade → Service → Rule → DAO → Mapper 구조로 구분한다.

## 3.2.10 단방향 호출 원칙

Handler
↓
Facade
↓
Service
├─ Rule
├─ DAO
│ ↓
│ Mapper
└─ Client

### 허용 호출

| 호출자 | 피호출자 |
| --- | --- |
| Handler | Facade |
| Facade | Service |
| Service | Rule·DAO·Client |
| DAO | Mapper |
| Mapper | DB |
| Client | 외부 시스템 |

### 금지 호출

DAO → Service
Rule → DAO
Mapper → Rule
Service → Handler
Facade → Mapper
Handler → DAO
업무 WAR → 다른 업무 WAR의 Mapper

## 3.2.11 하나의 거래를 소스에서 찾는 방법

ServiceId가 SV.Customer.selectSummary라고 가정한다.

1\. ServiceId 문자열 검색
↓
2\. serviceIds() 등록 Handler 확인
↓
3\. Handler의 Facade 호출 확인
↓
4\. Facade 트랜잭션 확인
↓
5\. Service 업무 흐름 확인
↓
6\. Rule과 DAO 호출 확인
↓
7\. Mapper Interface 확인
↓
8\. Mapper XML namespace·SQL ID 확인
↓
9\. Table·View 확인

### 추적 표

| 구분 | 예시 |
| --- | --- |
| ServiceId | SV.Customer.selectSummary |
| Handler | SvCustomerHandler |
| Facade | SvCustomerFacade.selectSummary() |
| Service | SvCustomerService.selectSummary() |
| Rule | SvCustomerInquiryRule.validate() |
| DAO | SvCustomerDao.selectSummary() |
| Mapper | SvCustomerMapper.selectSummary() |
| SQL ID | selectSummary |
| Table | SV\_CUSTOMER\_SUMMARY |

문자열 검색 결과만으로 완료 판단하지 않는다.

동적 SQL, 공통 Mapper, 런타임 분기와 외부 Contract를 놓칠 수 있으므로 실행 로그나 테스트로 실제 호출을 확인한다.

# 3.3 DTO와 Domain 객체

## 3.3.1 DTO란 무엇인가

DTO는 Data Transfer Object의 약자로 경계를 넘어 데이터를 전달하기 위한 객체다.

화면
↓ Request DTO
Handler·Facade
↓ Command·Query DTO
DAO·Mapper
↓ Result DTO
Service
↓ Response DTO
화면

DTO는 단순한 필드 묶음이 아니다.

다음 계약을 표현한다.

| 계약 | 내용 |
| --- | --- |
| 필드 | 어떤 값을 전달하는가 |
| 타입 | 문자열·숫자·날짜 |
| 필수 여부 | 반드시 필요한가 |
| 길이 | 최대·최소 크기 |
| 형식 | 날짜·코드·ID 형식 |
| 보안 | 개인정보·마스킹 |
| 버전 | 하위 호환 여부 |

## 3.3.2 DTO 종류

| DTO | 목적 | 예시 |
| --- | --- | --- |
| Request DTO | 화면·외부 입력 | CustomerSummaryRequest |
| Response DTO | 화면·외부 출력 | CustomerSummaryResponse |
| Command DTO | 상태변경 명령 | UpdateCustomerMemoCommand |
| Query DTO | 조회조건 | CustomerSummaryQuery |
| Result DTO | DB 조회결과 | CustomerSummaryResult |
| Contract DTO | 시스템 간 연계 | CustomerSummaryContractResponse |
| Event DTO | 비동기 이벤트 | CustomerChangedEvent |

## 3.3.3 DTO를 분리해야 하는 이유

하나의 DTO를 화면부터 DB까지 공용으로 사용하면 처음에는 편리하다.

CustomerDto
→ 화면 요청
→ Service 입력
→ Mapper Parameter
→ DB Result
→ 화면 응답

그러나 다음 문제가 발생한다.

| 변경 | 파급 효과 |
| --- | --- |
| 화면 필드 추가 | Mapper와 DB 객체까지 영향 |
| DB 컬럼명 변경 | 화면 계약까지 영향 |
| 내부 계산값 추가 | 외부 응답 노출 위험 |
| 개인정보 추가 | 모든 계층에서 노출 |
| 조회조건 변경 | 등록 거래 DTO까지 변경 |
| 응답 마스킹 | DB Result까지 변형 |

권장 구조는 다음과 같다.

CustomerSummaryRequest
↓
CustomerSummaryQuery
↓
CustomerSummaryResult
↓
CustomerSummaryResponse

## 3.3.4 Request DTO

Request DTO는 외부 입력 계약이다.

public record CustomerSummaryRequest(
@NotBlank
@Size(max = 20)
String customerNo,

@Pattern(regexp = "\\\\d{8}")
String baseDate,

boolean includeProducts
) {}

### 경계 검증 대상

| 검증 | 예시 |
| --- | --- |
| 필수 | 고객번호 누락 |
| 길이 | 고객번호 최대 20자 |
| 형식 | 날짜 yyyyMMdd |
| 범위 | 조회건수 1~1000 |
| 허용값 | 업무코드 목록 |
| 구조 | 목록 Null 여부 |

이 검증은 가능한 한 업무 프로그램 진입 전에 빠르게 수행한다.

## 3.3.5 Command와 Query

조회와 변경의 의도를 구분한다.

Query
\= 데이터를 읽기 위한 조건

Command
\= 상태를 변경하려는 명령

### Query 예시

public record CustomerSummaryQuery(
String customerNo,
LocalDate baseDate,
String requestBranchId
) {}

### Command 예시

public record UpdateCustomerMemoCommand(
String customerNo,
String memo,
String changedBy,
LocalDateTime changedAt
) {}

사용자 ID와 변경시각 같은 신뢰 정보는 화면 입력값보다 인증 문맥과 서버시각에서 생성하는 것이 안전하다.

## 3.3.6 Result DTO

Result DTO는 DB 조회 결과를 표현한다.

public record CustomerSummaryResult(
String customerNo,
String customerName,
String customerGrade,
LocalDate joinDate
) {}

Result DTO는 화면 표시 정책을 포함하지 않는다.

예를 들어 DB에는 고객명이 원문으로 조회되더라도 화면 응답에서는 권한에 따라 마스킹할 수 있다.

DB Result
홍길동

Response
홍\*동

## 3.3.7 Response DTO

Response DTO는 외부에 제공하는 계약이다.

public record CustomerSummaryResponse(
String customerNo,
String maskedCustomerName,
String customerGrade,
int productCount
) {}

Response DTO에는 다음을 직접 넣지 않는다.

DB 내부 컬럼명
내부 상태 코드 전체
SQL ID
Stack Trace
내부 서버 경로
JWT 원문
개인정보 원문

## 3.3.8 Domain 객체란 무엇인가

Domain 객체는 데이터를 전달하는 것보다 업무 의미와 상태·행위를 표현한다.

DTO
\= 데이터를 옮긴다.

Domain 객체
\= 업무 의미와 규칙을 보호한다.

### Domain 객체 유형

| 유형 | 특징 | 예 |
| --- | --- | --- |
| Entity | 식별자·생명주기 | Customer, Campaign |
| Value Object | 값으로 동일성 판단 | CustomerNo, CampaignPeriod |
| Aggregate | 일관성 보장 범위 | Campaign + TargetCondition |
| Domain Policy | 업무 규칙 | EligibilityPolicy |
| Domain Event | 발생한 업무 사실 | CustomerGradeChanged |

## 3.3.9 Entity

Entity는 고유 식별자와 상태변경 생명주기를 갖는다.

public class Campaign {

private final CampaignId id;
private CampaignStatus status;

public void approve() {
if (status != CampaignStatus.REQUESTED) {
throw new BusinessException(
"승인 요청 상태에서만 승인할 수 있습니다."
);
}

status = CampaignStatus.APPROVED;
}
}

단순 조회 결과에 Entity를 강제할 필요는 없다.

| 상황 | 권장 |
| --- | --- |
| 단순 통계 조회 | Result DTO |
| 코드 목록 조회 | Result DTO |
| 상태변경 규칙이 많은 업무 | Entity |
| 생명주기와 불변식 존재 | Entity·Aggregate |
| 여러 필드가 하나의 의미 | Value Object |

## 3.3.10 Value Object

Value Object는 식별자보다 값 자체가 중요하다.

public record CampaignPeriod(
LocalDateTime startAt,
LocalDateTime endAt) {

public CampaignPeriod {
if (startAt == null ||
endAt == null ||
!startAt.isBefore(endAt)) {

throw new IllegalArgumentException(
"캠페인 기간이 올바르지 않습니다."
);
}
}
}

Value Object는 생성되는 순간부터 유효한 상태를 보장할 수 있다.

Domain 객체는 단순 DTO와 달리 업무 상태와 규칙을 내부에서 보호하며, Entity·Value Object·Aggregate는 복잡한 상태변경 업무에 선택적으로 적용하는 것이 적절하다.

## 3.3.11 형식 검증과 업무 검증

두 검증을 구분해야 한다.

| 구분 | 형식 검증 | 업무 검증 |
| --- | --- | --- |
| 질문 | 값의 모양이 올바른가 | 업무적으로 허용되는가 |
| 위치 | 요청 경계·Handler 전후 | Service·Rule |
| 예시 | 필수·길이·날짜형식 | 중복·상태·권한 |
| 오류 | 입력 오류 | 업무 오류 |
| DB 필요 | 일반적으로 없음 | 필요할 수 있음 |
| 테스트 | Validation Test | Service·Rule Test |

### 예시

고객번호가 비어 있다
→ 형식 검증 오류

고객번호 형식은 맞지만 존재하지 않는다
→ 업무 오류

고객은 존재하지만 조회권한이 없다
→ 권한·업무 오류

## 3.3.12 DTO 변환 흐름

StandardRequest<Map>
↓ Handler
CustomerSummaryRequest
↓ Facade
CustomerSummaryQuery
↓ DAO·Mapper
CustomerSummaryResult
↓ Service
Customer Domain 또는 업무 결과
↓ Facade
CustomerSummaryResponse
↓ ETF
StandardResponse<CustomerSummaryResponse>

### 변환 책임

| 변환 | 권장 위치 |
| --- | --- |
| Map → Request DTO | Handler |
| Request → Query·Command | Facade |
| Result → Domain | Service·Assembler |
| Domain → Response | Facade·Assembler |
| Response → StandardResponse | ETF |

## 3.3.13 DTO 보안 기준

| 항목 | 기준 |
| --- | --- |
| 사용자 ID | 인증 문맥에서 생성 |
| 지점 ID | 검증된 Claim과 비교 |
| 비밀번호 | DTO·로그 장기 보관 금지 |
| JWT | DTO Body에 넣지 않음 |
| 주민번호 | 원문 최소화·마스킹 |
| 계좌번호 | 권한별 마스킹 |
| 내부 오류 | Response DTO 노출 금지 |
| 불필요 필드 | 계약에서 제거 |

# 3.4 계층을 건너뛰면 생기는 문제

## 3.4.1 계층은 불필요한 형식이 아니다

초보 개발자는 다음과 같이 생각할 수 있다.

Handler에서 Mapper를 바로 호출하면
코드가 짧아지지 않을까?

Facade 없이 Service에
@Transactional을 붙이면 되지 않을까?

한 DTO를 모든 계층에서 쓰면
변환 코드가 줄어들지 않을까?

실제로 코드 줄 수는 줄어들 수 있다.

하지만 구조가 단순해지는 것이 아니라 책임이 섞이는 것이다.

짧은 코드
≠ 단순한 구조

## 3.4.2 Handler에서 Mapper 직접 호출

### 금지 구조

Handler
↓
Mapper
↓
DB

### 발생 문제

| 문제 | 설명 |
| --- | --- |
| 업무 검증 누락 | Service·Rule 우회 |
| 트랜잭션 불명확 | Commit·Rollback 경계 없음 |
| DTO 결합 | 요청 객체와 DB Parameter 결합 |
| 테스트 어려움 | 진입·업무·DB를 한 번에 테스트 |
| 재사용 불가 | 다른 채널에서 업무 흐름 재사용 어려움 |
| 운영 분석 저하 | 어느 계층에서 실패했는지 구분 불가 |

### 권장

Handler
↓
Facade
↓
Service
↓
DAO
↓
Mapper

## 3.4.3 Facade에서 Mapper 직접 호출

### 금지 구조

Facade
├─ Service
└─ Mapper

Facade가 일부 데이터는 Service를 통하고 일부는 Mapper로 직접 조회하면 업무 경계가 흔들린다.

| 영향 | 설명 |
| --- | --- |
| 규칙 적용 불일치 | 같은 데이터에 다른 규칙 적용 |
| 데이터 소유권 우회 | 도메인 Service 무시 |
| 변경 영향 증가 | SQL 변경이 Facade에 직접 전파 |
| 테스트 복잡 | Mock 대상 증가 |

## 3.4.4 Rule에서 DAO 호출

### 금지 구조

Rule
↓
DAO
↓
DB

Rule이 DB를 직접 조회하면 순수 규칙이 아닌 데이터 접근 Service가 된다.

### 문제

같은 Rule 실행
→ DB 상태에 따라 결과가 달라짐

단위 테스트
→ DB 또는 Mock 필요

Rule 재사용
→ 데이터 접근 환경 필요

### 권장

Service가 필요한 데이터 조회
↓
Rule에 값 전달
↓
Rule은 판단만 수행

## 3.4.5 DAO에서 업무 판단

### 금지 예시

public void updateMemo(CustomerMemoCommand command) {
int count = mapper.updateMemo(command);

if (count == 0) {
throw new BusinessException(
"해지 고객은 수정할 수 없습니다."
);
}
}

영향 행 수가 0인 이유는 여러 가지일 수 있다.

고객 없음
해지 고객
동시 변경
조건 불일치
SQL 오류

업무 의미를 DAO가 임의로 결정하면 실제 원인이 왜곡될 수 있다.

Service에서 상태와 결과를 해석해야 한다.

## 3.4.6 Mapper에 업무 로직 작성

동적 SQL 자체는 사용할 수 있지만, 복잡한 업무 정책을 SQL에 숨기면 안 된다.

### 위험한 예

UPDATE SV\_CUSTOMER
SET STATUS = 'APPROVED'
WHERE CUSTOMER\_NO = #{customerNo}
AND STATUS = 'REQUESTED'
AND EXISTS (
SELECT 1
FROM OM\_USER\_AUTH
WHERE USER\_ID = #{userId}
AND AUTH\_CODE = 'APPROVER'
)

SQL 하나에 상태전이·권한·업무정책이 모두 들어 있다.

### 문제

| 문제 | 영향 |
| --- | --- |
| 규칙 가시성 저하 | Java 코드에서 업무 판단을 찾지 못함 |
| 테스트 어려움 | DB 통합 테스트만 가능 |
| 오류 메시지 한계 | 실패 이유 구분 어려움 |
| 재사용 어려움 | 다른 저장소에 적용 불가 |
| 감사 불명확 | 누가 어떤 규칙으로 승인했는지 추적 어려움 |

## 3.4.7 업무 WAR 간 Java 직접 참조

### 금지 구조

sv-service
→ project(":ic-service")
→ IcCustomerService 직접 호출

### 문제

| 문제 | 영향 |
| --- | --- |
| 독립 배포 훼손 | IC 변경 시 SV 재배포 |
| ClassLoader 경계 훼손 | 외부 Tomcat 다중 WAR와 불일치 |
| 순환 의존 | SV→IC→SV 가능 |
| 장애 전파 | 내부 구현 의존 |
| 계약 부재 | Timeout·오류·버전 기준 없음 |

### 권장 구조

SV
↓ tcf-eai·Client
IC.Customer.selectBasic
↓
IC Handler·Facade·Service

또는 동일 WAR 안에서는 승인된 Application Contract를 사용한다.

## 3.4.8 다른 도메인의 Mapper 직접 호출

### 금지

CustomerService
↓
ProductMapper

데이터가 같은 DB에 있다고 같은 소유권을 갖는 것은 아니다.

### 권장

CustomerViewFacade
↓
ProductHoldingQueryContract
↓
Product 도메인

데이터 변경은 반드시 데이터를 소유한 도메인이 수행해야 한다.

## 3.4.9 Controller별 개별 공통 처리

업무별 Controller가 다음 기능을 각각 구현하면 안 된다.

JWT 검증
Header 검증
GUID 생성
권한 검증
거래통제
Timeout
중복 요청
거래로그
오류 응답

업무마다 구현이 조금씩 달라져 운영 표준이 무너진다.

공통 Controller와 TCF 파이프라인을 사용한다.

## 3.4.10 TCF 우회

### 우회 구조

직접 Controller
↓
Facade
↓
Service

TCF를 우회하면 다음 기능이 누락될 수 있다.

| 누락 가능 기능 | 영향 |
| --- | --- |
| Header 검증 | 잘못된 요청 수용 |
| 인증 정합성 | 사용자 위·변조 |
| 권한 | 무권한 거래 |
| 거래통제 | 장애 거래 차단 불가 |
| Timeout | Thread 장기 점유 |
| 멱등성 | 중복 등록 |
| 거래로그 | 장애 추적 불가 |
| 감사로그 | 규제·감사 위험 |
| Metric | 성능 추세 확인 불가 |

특수 API가 TCF를 우회해야 한다면 예외 사유, 대체 통제, 승인자와 폐기 계획을 기록한다.

## 3.4.11 하나의 DTO를 모든 계층에서 사용

### 금지 구조

CustomerDto
\= Request
\= Query
\= Result
\= Response
\= Domain

### 문제

| 문제 | 결과 |
| --- | --- |
| 과도한 필드 | 거래마다 불필요한 데이터 전달 |
| Null 의미 불명확 | 입력 누락과 미조회 구분 어려움 |
| 보안 노출 | 내부 필드 외부 반환 |
| 변경 전파 | DB 변경이 화면에 영향 |
| 검증 혼합 | 입력·업무 규칙이 한 객체에 섞임 |

## 3.4.12 공통 패키지의 무분별한 확대

다음 패키지가 계속 커지면 경고 신호다.

common
util
helper
manager
shared
misc
temp

### 판단 질문

| 질문 | 판단 |
| --- | --- |
| 여러 업무가 정말 동일 의미로 사용하는가 | 플랫폼 공통 후보 |
| 특정 업무 용어가 포함되는가 | 업무 도메인에 유지 |
| 상태·DB·권한에 의존하는가 | Utility 아님 |
| 독립적인 계약이 있는가 | 공통화 검토 |
| 변경 책임자가 명확한가 | 소유권 확인 |

공통화는 중복 코드 제거보다 변경 의미와 소유권을 우선한다.

## 3.4.13 트랜잭션 계층 위반

### 잘못된 구조

Handler @Transactional
↓
Facade @Transactional
↓
Service @Transactional
↓
DAO

모든 계층에 트랜잭션을 붙이면 실제 경계를 이해하기 어렵다.

### 위험

| 위험 | 설명 |
| --- | --- |
| Propagation 혼란 | 별도 트랜잭션 발생 |
| Rollback 불일치 | 예외 처리 위치에 따라 Commit |
| 외부 호출 장기 대기 | DB Lock 유지 |
| Timeout 중첩 | 어느 제한시간이 적용되는지 불명확 |
| 테스트 오판 | 부분 Commit 놓침 |

권장 기준은 Facade에서 유스케이스 트랜잭션을 선언하고, 특별한 이유가 있을 때만 하위 트랜잭션 정책을 별도 설계하는 것이다.

## 3.4.14 예외를 숨기는 문제

### 금지

try {
mapper.updateMemo(command);
} catch (Exception e) {
log.warn("오류");
return false;
}

이 구조에서는 다음 정보가 사라진다.

원인 Exception
SQL ID
Rollback 필요 여부
오류 유형
재시도 가능 여부

예외를 적절한 업무 오류 또는 시스템 오류로 분류하되 원인 예외를 보존해야 한다.

# 계층 위반 종합표

| 위반 | 단기 장점 | 장기 문제 |
| --- | --- | --- |
| Handler→Mapper | 코드 짧음 | 규칙·트랜잭션 훼손 |
| Facade→Mapper | 빠른 조회 | 데이터 경계 훼손 |
| Rule→DAO | 한곳에서 검증 | 순수성·테스트 훼손 |
| DAO 업무판단 | SQL 결과 즉시 해석 | 오류 의미 왜곡 |
| Mapper 업무로직 | 왕복 감소 | 규칙 은닉 |
| WAR 간 Import | 호출 편리 | 독립 배포 불가 |
| DTO 전 계층 공유 | 변환 감소 | 강결합·보안 위험 |
| TCF 우회 | 빠른 구현 | 통제·감사 누락 |
| 공통 패키지 남용 | 중복 감소 | 소유권 상실 |
| 예외 숨김 | 화면 오류 감소 | 운영 진단 불가 |

# 목표 업무 패키지 구조

com.nh.nsight.marketing.sv.customer
├─ handler
│ └─ SvCustomerHandler.java
│
├─ facade
│ └─ SvCustomerFacade.java
│
├─ service
│ └─ SvCustomerService.java
│
├─ rule
│ └─ SvCustomerRule.java
│
├─ dao
│ └─ SvCustomerDao.java
│
├─ mapper
│ └─ SvCustomerMapper.java
│
├─ dto
│ ├─ request
│ │ └─ CustomerSummaryRequest.java
│ ├─ response
│ │ └─ CustomerSummaryResponse.java
│ ├─ command
│ │ └─ UpdateCustomerMemoCommand.java
│ ├─ query
│ │ └─ CustomerSummaryQuery.java
│ └─ result
│ └─ CustomerSummaryResult.java
│
├─ domain
│ ├─ Customer.java
│ ├─ CustomerNo.java
│ └─ CustomerStatus.java
│
├─ client
│ └─ ProductHoldingClient.java
│
└─ config
└─ SvCustomerConfiguration.java

Mapper XML은 일반적으로 리소스 경로에 둔다.

src/main/resources
└─ mapper
└─ sv
└─ customer
└─ SvCustomerMapper.xml

# 플랫폼 패키지 구조

com.nh.nsight.tcf
├─ core
│ ├─ transaction
│ ├─ context
│ ├─ dispatcher
│ ├─ timeout
│ ├─ exception
│ └─ logging
│
├─ web
│ ├─ controller
│ ├─ filter
│ ├─ security
│ └─ configuration
│
├─ gateway
│ ├─ route
│ ├─ auth
│ └─ proxy
│
├─ jwt
│ ├─ issue
│ ├─ validate
│ ├─ key
│ └─ revoke
│
├─ om
│ ├─ catalog
│ ├─ control
│ ├─ timeout
│ └─ monitoring
│
└─ util
├─ time
├─ string
└─ identifier

# 책임 경계 RACI

| 활동 | 아키텍처 | 프레임워크팀 | 업무팀 | DevOps | 운영 |
| --- | --- | --- | --- | --- | --- |
| 모듈 추가 승인 | A | R | C | C | I |
| 공통 모듈 변경 | C | R/A | C | I | I |
| 업무 WAR 생성 | A | C | R | C | I |
| 패키지 구조 | A | C | R | I | I |
| 계층 책임 준수 | A | C | R | I | I |
| Gradle 의존성 | C | R | R | C | I |
| 실행·배포 설정 | C | C | C | R/A | C |
| Mapper Scan | C | C | R | I | I |
| 운영 로그 경로 | C | C | R | C | A |
| 구조 자동검증 | A | R | C | R | I |

# 정상 처리 흐름

Request
↓
공통 Controller
↓
TCF·STF
↓
Dispatcher
↓
Handler
↓
Facade @Transactional
↓
Service
├─ Rule
└─ DAO
↓
Mapper
↓
DB
↓
Response DTO
↓
ETF

### 정상 구조의 특징

| 항목 | 정상 기준 |
| --- | --- |
| 모듈 | 실행·라이브러리 구분 |
| 패키지 | 업무코드·도메인·계층 식별 |
| 호출 | 상위에서 하위로 단방향 |
| 트랜잭션 | Facade 유스케이스 경계 |
| DTO | 경계별 분리 |
| Domain | 복잡도에 맞게 선택 |
| SQL | Mapper XML에 식별 가능 |
| 로그 | ServiceId·GUID 연결 |
| 테스트 | 계층별 독립 검증 |
| 배포 | 업무 WAR 경계 유지 |

# 오류·장애 흐름

## 모듈 의존성 오류

컴파일 실패
↓
어느 모듈에서 실패했는지 확인
↓
dependencyInsight 실행
↓
직접·전이 의존성 경로 확인
↓
BOM·공통 버전 정책 확인
↓
최소 범위 수정
↓
전체 회귀 빌드

## Mapper Scan 오류

Mapper Bean 없음
↓
Mapper Interface @Mapper 확인
↓
@MapperScan 범위 확인
↓
BASE 패키지 확인
↓
XML namespace 확인
↓
리소스 포함 여부 확인

## Spring Bean 중복

BeanDefinitionOverrideException
↓
Component Scan 범위 확인
↓
동일 클래스·Configuration 중복 확인
↓
공통 모듈과 업무 모듈 소유권 확인

## 순환 의존

Bean A → Bean B → Bean A
↓
업무 책임 재검토
↓
공개 Contract 분리
↓
Orchestrator 또는 이벤트 구조 검토

# 정상 예시와 금지 예시

| 구분 | 정상 | 금지 |
| --- | --- | --- |
| 모듈 | sv-service → tcf-core | tcf-core → sv-service |
| WAR 연계 | ServiceId·Client | 다른 WAR Java Import |
| Handler | Facade 호출 | Mapper 호출 |
| Facade | 트랜잭션·조립 | SQL 직접 실행 |
| Service | Rule·DAO 호출 | StandardResponse 생성 |
| Rule | 순수 업무판단 | DB 조회 |
| DAO | Mapper 호출 | 업무상태 판단 |
| Mapper | SQL 실행 | 권한·업무 흐름 |
| DTO | Request·Query·Result 분리 | 하나의 DTO 공용 |
| Domain | 복잡한 상태 보호 | 단순 조회에 과도한 모델 |
| 공통화 | 의미가 같은 기능 | 이름만 비슷한 업무 통합 |

# 자동검증 및 품질 Gate

## Gradle 검증

| 검증 | 방법 |
| --- | --- |
| 전체 모듈 빌드 | gradlew clean build |
| Project Dependency | gradlew dependencies |
| 충돌 버전 | dependencyInsight |
| 미사용 의존성 | Dependency Analysis Plugin |
| 순환 모듈 | Project Dependency 검사 |
| JDK 정합성 | Toolchain·CI JDK 확인 |

## ArchUnit 검증 예시

@ArchTest
static final ArchRule handlers\_may\_only\_access\_facades =
classes()
.that().resideInAPackage("..handler..")
.should().onlyAccessClassesThat()
.resideInAnyPackage(
"..handler..",
"..facade..",
"java..",
"org.slf4j.."
);

@ArchTest
static final ArchRule rules\_must\_not\_access\_daos =
noClasses()
.that().resideInAPackage("..rule..")
.should().dependOnClassesThat()
.resideInAnyPackage("..dao..", "..mapper..");

@ArchTest
static final ArchRule mappers\_must\_not\_access\_services =
noClasses()
.that().resideInAPackage("..mapper..")
.should().dependOnClassesThat()
.resideInAPackage("..service..");

## 품질 Gate

| Gate | 차단 조건 |
| --- | --- |
| Compile | 모듈 의존성 오류 |
| Unit Test | Rule·Service 테스트 실패 |
| Architecture Test | 계층 위반 |
| Integration Test | Mapper·DB 연계 실패 |
| Transaction Test | Rollback 불일치 |
| Security Test | 민감정보 DTO 노출 |
| Packaging | WAR·JAR 생성 실패 |
| Deployment | Context·Bean·Mapper Scan 오류 |
| Traceability | ServiceId에서 SQL까지 미연결 |

# 테스트 시나리오

| ID | 테스트 | 조건 | 기대 결과 |
| --- | --- | --- | --- |
| CH03-001 | 전체 프로젝트 확인 | gradlew projects | 참여 모듈 출력 |
| CH03-002 | 전체 빌드 | Clean 환경 | 모든 모듈 성공 |
| CH03-003 | 특정 업무 실행 | sv-service:bootRun | 정상 기동 |
| CH03-004 | 라이브러리 실행 시도 | tcf-core | 독립 기동 대상 아님 확인 |
| CH03-005 | WAR 생성 | bootWar | 산출물 생성 |
| CH03-006 | 의존성 분석 | Jackson 충돌 | 경로 확인 |
| CH03-007 | ServiceId 추적 | 고객요약 거래 | Handler→SQL 연결 |
| CH03-008 | Handler 계층 검사 | DAO 직접 의존 | CI 실패 |
| CH03-009 | Rule 계층 검사 | Mapper 직접 의존 | CI 실패 |
| CH03-010 | DTO Validation | 고객번호 누락 | 입력 오류 |
| CH03-011 | 업무 Rule | 해지 고객 수정 | 업무 오류 |
| CH03-012 | 트랜잭션 Rollback | 두 번째 SQL 실패 | 전체 Rollback |
| CH03-013 | Mapper Scan | 패키지 오설정 | 기동 실패 |
| CH03-014 | XML Namespace | Interface 불일치 | Mapper 오류 |
| CH03-015 | DTO 보안 | 내부 필드 포함 | 품질 Gate 실패 |
| CH03-016 | WAR 간 직접 참조 | sv→ic Project Dependency | 구조 검사 실패 |
| CH03-017 | 순환 Bean | A→B→A | 기동 또는 검사 실패 |
| CH03-018 | TCF 우회 | 직접 Controller | 통제 누락 식별 |
| CH03-019 | Thread Context | 연속 거래 | 사용자 문맥 잔존 없음 |
| CH03-020 | 외부 Tomcat | WAR 배포 | Context 정상 기동 |

# 따라 하는 실무 절차

## 1단계. 전체 모듈을 확인한다

./gradlew projects

완료 증적:

모듈 목록
실행 모듈 구분
라이브러리 모듈 구분
업무 WAR 목록

## 2단계. 한 업무 WAR의 의존성을 확인한다

./gradlew :sv-service:dependencies

확인한다.

tcf-core가 포함되는가?
tcf-web이 포함되는가?
불필요한 다른 업무 WAR가 포함되는가?
동일 라이브러리의 복수 버전이 존재하는가?

## 3단계. 기동 클래스를 찾는다

검색어:

@SpringBootApplication
public static void main
SpringBootServletInitializer

완료 증적:

| 항목 | 기록 |
| --- | --- |
| 모듈 | sv-service |
| Main Class | 실제 클래스명 |
| Port | 로컬 설정 |
| Context Path | /sv |
| Profile | local |
| Packaging | WAR·JAR |

## 4단계. 대표 ServiceId를 추적한다

ServiceId
→ Handler
→ Facade
→ Service
→ Rule
→ DAO
→ Mapper
→ SQL
→ Table

완료 증적은 추적성 표와 실행 로그다.

## 5단계. 계층 위반을 찾는다

검색 예:

handler 패키지의 mapper import
rule 패키지의 dao import
facade 패키지의 mapper import
tcf-core의 업무 패키지 import
업무 WAR 간 project dependency

## 6단계. DTO 경계를 확인한다

| 확인 | 질문 |
| --- | --- |
| Request | 화면 입력만 포함하는가 |
| Query | DB 조회조건만 포함하는가 |
| Result | DB 결과와 일치하는가 |
| Response | 외부 제공 필드만 포함하는가 |
| Domain | 업무 상태와 규칙이 있는가 |
| 보안 | 민감정보가 불필요하게 전달되는가 |

## 7단계. 깨끗한 환경에서 재실행한다

./gradlew clean build
./gradlew :sv-service:bootRun

IDE 캐시나 기존 빌드 산출물에 의존하지 않는지 확인한다.

# 완료 체크리스트

## Gradle·모듈

| 확인 항목 | 완료 |
| --- | --- |
| settings.gradle의 참여 모듈을 확인했다. | □ |
| 실행 모듈과 라이브러리 모듈을 구분했다. | □ |
| 업무 WAR의 build.gradle을 확인했다. | □ |
| Project Dependency 방향을 확인했다. | □ |
| dependencyInsight를 실행할 수 있다. | □ |
| JAR와 WAR의 차이를 설명할 수 있다. | □ |

## 계층

| 확인 항목 | 완료 |
| --- | --- |
| Controller와 Handler의 차이를 설명할 수 있다. | □ |
| Facade 트랜잭션 경계를 확인했다. | □ |
| Service의 업무 흐름을 확인했다. | □ |
| Rule이 DB를 호출하지 않는지 확인했다. | □ |
| DAO와 Mapper 책임을 구분했다. | □ |
| 다른 WAR의 클래스를 직접 참조하지 않는지 확인했다. | □ |

## DTO·Domain

| 확인 항목 | 완료 |
| --- | --- |
| Request·Response DTO를 구분했다. | □ |
| Query·Command·Result DTO를 구분했다. | □ |
| 하나의 DTO를 전 계층에서 사용하지 않는다. | □ |
| 형식 검증과 업무 검증을 분리했다. | □ |
| Entity가 필요한 업무인지 판단했다. | □ |
| Value Object 적용 대상을 검토했다. | □ |
| 민감정보 노출 여부를 확인했다. | □ |

## 운영·품질

| 확인 항목 | 완료 |
| --- | --- |
| ServiceId에서 SQL까지 추적했다. | □ |
| Mapper ID와 Table을 연결했다. | □ |
| 계층 위반 자동검증이 존재한다. | □ |
| 깨끗한 환경에서 전체 빌드했다. | □ |
| 외부 Tomcat WAR 배포를 검증했다. | □ |
| 구조와 다르게 구현한 예외를 기록했다. | □ |

# 제3장의 핵심 정리

첫째,
멀티모듈은 코드를 나눈 폴더가 아니라
빌드·배포·책임 경계를 정의한다.

둘째,
모든 모듈이 실행되는 것은 아니다.
실행 모듈과 라이브러리 모듈을 구분해야 한다.

셋째,
플랫폼 모듈은 특정 업무를 몰라야 하고,
업무 WAR는 다른 업무 WAR의 내부 클래스를 직접 참조하지 않는다.

넷째,
NSIGHT TCF 업무 계층은
Handler → Facade → Service → Rule → DAO → Mapper
순서로 구성한다.

다섯째,
트랜잭션 경계는 일반적으로 Facade에 두고,
Rule은 부작용 없이 업무 판단에 집중한다.

여섯째,
DTO는 데이터를 전달하는 계약이고,
Domain 객체는 업무 의미와 상태 규칙을 보호한다.

일곱째,
계층을 건너뛰면 코드가 짧아질 수 있지만
트랜잭션·테스트·운영·변경 영향은 더 복잡해진다.

여덟째,
프로젝트 구조를 이해했다는 것은
파일 위치를 외운 것이 아니라
ServiceId에서 SQL과 테이블까지 추적할 수 있다는 뜻이다.

# 시사점

## 핵심 아키텍처 판단

프로젝트 구조의 핵심은 디렉터리 이름 자체가 아니다.

모듈은 배포와 변경의 경계를 정의하고,

도메인은 업무 책임의 경계를 정의하며,

계층은 프로그램 책임의 경계를 정의하고,

DTO는 데이터 계약의 경계를 정의한다.

이 네 경계가 일치할 때 소스 구조가 아키텍처를 설명할 수 있다.

## 주요 위험

| 위험 | 영향 |
| --- | --- |
| 공통 모듈의 업무 의존 | 모든 업무에 변경 영향 |
| 업무 WAR 간 직접 Import | 독립 배포 훼손 |
| Handler의 DB 접근 | 트랜잭션·검증 우회 |
| Rule의 외부 의존 | 단위 테스트 불가 |
| DTO 전 계층 공유 | DB·화면 강결합 |
| 트랜잭션 중복 선언 | Rollback 예측 어려움 |
| Mapper 업무 로직 | 규칙 은닉 |
| 공통 패키지 확대 | 책임 소유권 상실 |
| TCF 우회 | 통제·로그·감사 누락 |
| Gradle 버전 임의 변경 | 다른 모듈 충돌 |

## 우선 보완 과제

1.  전체 모듈을 실행·라이브러리·업무 WAR로 분류한다.
2.  모듈 간 허용 의존성 표를 작성한다.
3.  패키지 구조를 업무코드 → 도메인 → 계층으로 정리한다.
4.  Handler·Facade·Service·Rule·DAO·Mapper 구조 검사를 자동화한다.
5.  WAR 간 Java 직접 의존을 CI에서 차단한다.
6.  DTO 유형별 표준 템플릿을 제공한다.
7.  ServiceId–Handler–Mapper–Table 추적성 매트릭스를 자동 생성한다.
8.  외부 Tomcat WAR 배포를 CI/CD 검증 항목에 포함한다.
9.  공통·Util 패키지의 소유권과 승격 기준을 정한다.
10.  TCF 우회 API를 목록화하고 예외승인·폐기계획을 관리한다.

## 중장기 발전 방향

수동 패키지 검토
↓
ArchUnit 구조검사
↓
Gradle 모듈 의존성 Gate
↓
ServiceId·Handler 자동 대조
↓
Mapper·Table 자동 추적
↓
설계서·코드·OM Catalog 자동 정합성 검사

# 마무리말

프로젝트를 처음 열었을 때 보이는 수많은 폴더와 클래스는 무작위로 존재하는 것이 아니다.

Gradle 모듈은
무엇을 함께 빌드하고 배포할지를 결정한다.

업무 WAR는
어느 업무가 코드를 소유할지를 결정한다.

도메인 패키지는
어떤 업무 책임으로 함께 변경될지를 결정한다.

프로그램 계층은
각 클래스가 어디까지 책임질지를 결정한다.

DTO와 Domain 객체는
데이터와 업무 의미가 어떤 경계를 넘는지를 결정한다.

좋은 프로젝트 구조는 개발자가 설명을 듣지 않아도 클래스 경로와 의존관계만 보고 다음을 판단할 수 있게 한다.

이 코드는 플랫폼 코드인가 업무 코드인가?

어느 업무와 도메인이 소유하는가?

어느 계층의 책임인가?

어떤 모듈과 데이터에 의존하는가?

변경하면 어느 화면과 거래에 영향이 있는가?

장애가 발생하면 누가 확인해야 하는가?

다음 장에서는 이 구조 위에서 거래를 일관되게 식별하는 방법을 살펴본다. 업무코드, 화면 ID, ServiceId, 거래코드, 패키지·클래스·메서드 명명규칙과 운영 로그의 식별자를 하나의 추적체계로 연결한다.
