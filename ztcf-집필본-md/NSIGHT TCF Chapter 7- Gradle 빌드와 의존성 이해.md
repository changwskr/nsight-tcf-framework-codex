<!-- source: ztcf-집필본/NSIGHT TCF Chapter 7- Gradle 빌드와 의존성 이해.docx -->
<!-- tables: mammoth-html + placeholder table-to-md -->

# 제7장. Gradle 빌드와 의존성 이해

## 이 장을 시작하며

제6장에서는 Git 저장소를 Clone하고, 올바른 Branch에서 변경을 Commit한 뒤 Pull Request로 검토받는 방법을 살펴보았다.

이제 Git으로 가져온 소스를 실제 프로그램으로 만들어야 한다.

Java 소스
\+ 설정 파일
\+ Mapper XML
\+ 라이브러리
\+ 테스트 코드
↓
Gradle
↓
컴파일·테스트·검증·패키징
↓
JAR·WAR·테스트 결과·품질 결과

초보 개발자는 Gradle을 다음처럼 생각하기 쉽다.

명령창에서 build를 실행한다.

BUILD SUCCESSFUL이 나오면 끝이다.

그러나 Gradle 빌드는 단순히 Java 소스를 컴파일하는 작업이 아니다.

어떤 모듈이 프로젝트에 참여하는가?

각 모듈은 무엇을 의존하는가?

어느 모듈이 단독 실행되는가?

어느 모듈은 다른 모듈에 포함되는가?

테스트는 어느 범위까지 실행되는가?

최종 산출물은 JAR인가, WAR인가?

운영 Tomcat에 배포할 파일은 무엇인가?

동일 소스로 다시 빌드했을 때 같은 결과가 나오는가?

이 질문에 답하는 것이 Gradle 빌드의 핵심이다.

NSIGHT TCF는 하나의 애플리케이션이 아니라, tcf-util, tcf-core, tcf-web 같은 플랫폼 라이브러리와 Gateway·JWT·OM·Batch 및 여러 업무 WAR가 함께 구성되는 멀티모듈 프로젝트다. 실제 모듈 역할과 포함 범위는 사용하는 Branch의 settings.gradle과 각 모듈의 build.gradle을 기준으로 확인해야 한다.

## 핵심 관점

Gradle은 단순한 컴파일 도구가 아니다.

모듈 경계,
허용 의존성,
사용할 기술 버전,
테스트 범위,
실행 방식,
배포 산출물을
코드로 정의하는 빌드 아키텍처다.

## 학습 목표

이 장을 마치면 다음 내용을 설명하고 직접 수행할 수 있어야 한다.

| 번호 | 학습 목표 |
| --- | --- |
| 1 | Gradle Project와 멀티모듈 Project의 차이를 설명한다. |
| 2 | settings.gradle과 build.gradle의 역할을 구분한다. |
| 3 | Root 빌드 파일과 모듈별 빌드 파일을 구분한다. |
| 4 | Gradle Wrapper와 시스템 Gradle의 차이를 설명한다. |
| 5 | 실행 모듈과 라이브러리 모듈을 구분한다. |
| 6 | api, implementation, runtimeOnly, testImplementation의 차이를 설명한다. |
| 7 | NSIGHT TCF의 권장 모듈 의존 방향을 설명한다. |
| 8 | 특정 업무 WAR가 어떤 공통 모듈을 사용하는지 확인한다. |
| 9 | 전체 Build와 단일 모듈 Build를 실행한다. |
| 10 | test, bootRun, bootJar, bootWar의 차이를 설명한다. |
| 11 | 생성된 JAR·WAR와 Resource 포함 여부를 확인한다. |
| 12 | 의존성 충돌 경로를 dependencyInsight로 분석한다. |
| 13 | 빌드 실패를 설정·의존성·컴파일·테스트·패키징 단계로 분류한다. |
| 14 | 로컬과 CI 빌드 결과가 다른 원인을 진단한다. |
| 15 | Build 결과와 Commit ID·Artifact Version을 연결한다. |
| 16 | Gradle 빌드를 CI/CD 품질 Gate로 사용하는 이유를 설명한다. |

# 한눈에 보는 Gradle 빌드 흐름

┌─────────────────────────────────────────────────────────────┐
│ 1. settings.gradle │
│ 전체 프로젝트와 참여 모듈 결정 │
└──────────────────────────┬──────────────────────────────────┘
▼
┌─────────────────────────────────────────────────────────────┐
│ 2. Root build.gradle │
│ Java·Spring Boot·Repository·공통 Test 정책 │
└──────────────────────────┬──────────────────────────────────┘
▼
┌─────────────────────────────────────────────────────────────┐
│ 3. Module build.gradle │
│ Plugin·Dependency·JAR/WAR·Main Class 설정 │
└──────────────────────────┬──────────────────────────────────┘
▼
┌─────────────────────────────────────────────────────────────┐
│ 4. Configuration │
│ Task와 Dependency Graph 구성 │
└──────────────────────────┬──────────────────────────────────┘
▼
┌─────────────────────────────────────────────────────────────┐
│ 5. Task 실행 │
│ compileJava → processResources → test → package │
└──────────────────────────┬──────────────────────────────────┘
▼
┌─────────────────────────────────────────────────────────────┐
│ 6. 검증 │
│ Unit Test·Integration Test·정적검사·구조검사 │
└──────────────────────────┬──────────────────────────────────┘
▼
┌─────────────────────────────────────────────────────────────┐
│ 7. 산출물 │
│ JAR·Boot JAR·WAR·Test Report·Checksum │
└──────────────────────────┬──────────────────────────────────┘
▼
┌─────────────────────────────────────────────────────────────┐
│ 8. CI/CD │
│ Artifact 저장·배포 승인·Tomcat 배포 │
└─────────────────────────────────────────────────────────────┘

# Gradle 핵심 용어

| 용어 | 의미 |
| --- | --- |
| Project | 하나의 빌드 대상 |
| Root Project | 멀티모듈 전체를 대표하는 최상위 Project |
| Subproject | Root 아래의 개별 모듈 |
| Plugin | Java·Spring Boot·WAR 같은 빌드 기능 제공 |
| Dependency | 모듈이 컴파일·실행·테스트에 사용하는 라이브러리 |
| Configuration | 의존성 사용 범위 |
| Task | 컴파일·테스트·패키징 같은 실행 작업 |
| Task Graph | 실행할 Task와 선후관계 |
| Repository | 외부 라이브러리를 가져오는 저장소 |
| Artifact | 빌드 결과물인 JAR·WAR 등 |
| BOM | 관련 라이브러리 버전을 일관되게 관리하는 기준 |
| Toolchain | Gradle이 사용할 Java 버전 지정 |
| Wrapper | 프로젝트가 정한 Gradle 버전을 실행하는 도구 |
| Daemon | 빌드 속도를 높이기 위한 Gradle 백그라운드 프로세스 |
| Cache | 이전 빌드 결과를 재사용하는 저장영역 |
| Transitive Dependency | 직접 의존성이 다시 가져오는 간접 의존성 |

# Gradle 파일 지도

nsight-tcf-framework
├─ settings.gradle
├─ build.gradle
├─ gradle.properties
├─ gradle
│ └─ wrapper
│ └─ gradle-wrapper.properties
├─ gradlew
├─ gradlew.bat
│
├─ tcf-util
│ └─ build.gradle
├─ tcf-core
│ └─ build.gradle
├─ tcf-web
│ └─ build.gradle
├─ tcf-cache
│ └─ build.gradle
├─ tcf-eai
│ └─ build.gradle
│
├─ tcf-gateway
│ └─ build.gradle
├─ tcf-jwt
│ └─ build.gradle
├─ tcf-om
│ └─ build.gradle
├─ tcf-batch
│ └─ build.gradle
│
├─ sv-service
│ └─ build.gradle
├─ ic-service
│ └─ build.gradle
└─ ...

저장소와 Branch에 따라 참여 모듈 수와 명칭은 달라질 수 있다.

따라서 문서에 적힌 모듈 목록보다 현재 Branch의 settings.gradle을 우선한다.

# 7.1 settings.gradle과 build.gradle 읽기

## 7.1.1 settings.gradle의 역할

settings.gradle은 멀티모듈 프로젝트의 시작점이다.

이 파일은 다음 질문에 답한다.

전체 프로젝트 이름은 무엇인가?

어떤 디렉터리가 Gradle 모듈인가?

어떤 모듈이 이번 빌드에 참여하는가?

Plugin은 어느 저장소에서 찾는가?

모듈의 실제 디렉터리는 어디인가?

개념 예:

rootProject.name = "nsight-tcf-framework"

include "tcf-util"
include "tcf-core"
include "tcf-web"
include "tcf-cache"
include "tcf-eai"

include "tcf-gateway"
include "tcf-jwt"
include "tcf-om"
include "tcf-batch"

include "sv-service"
include "ic-service"
include "pc-service"

이 코드는 구조 설명용이다. 실제 모듈은 현재 Branch의 파일을 기준으로 확인한다.

## 7.1.2 include의 의미

다음 디렉터리가 존재한다고 해서 자동으로 Gradle 모듈이 되는 것은 아니다.

tcf-core/
tcf-web/
sv-service/
docs/
tcf-scripts/

settings.gradle에서 include된 Project만 멀티모듈 빌드에 참여한다.

디렉터리 존재
≠ Gradle 모듈

settings.gradle에 include
\= Gradle 모듈

예를 들어 문서, 배포 Script 또는 CI 설정 디렉터리는 저장소에 존재하더라도 Gradle 모듈이 아닐 수 있다.

## 7.1.3 모듈 목록 확인

프로젝트 목록을 확인한다.

gradle projects

Wrapper 사용 시:

./gradlew projects

Windows:

gradlew.bat projects

출력 예:

Root project 'nsight-tcf-framework'
+--- Project ':tcf-util'
+--- Project ':tcf-core'
+--- Project ':tcf-web'
+--- Project ':tcf-om'
+--- Project ':sv-service'
\\--- Project ':ic-service'

확인할 것:

| 확인 | 판단 |
| --- | --- |
| 예상 모듈이 표시되는가 | include 정상 |
| 같은 업무가 중복되어 있는가 | 모듈 정리 필요 |
| 문서상 모듈과 다른가 | 현재 Branch 기준 재분석 |
| 모듈 경로가 잘못되었는가 | projectDir 확인 |
| 폐기 모듈이 남았는가 | 변경관리 대상 |

## 7.1.4 Plugin Management

settings.gradle에는 Plugin 저장소를 정의할 수 있다.

pluginManagement {
repositories {
gradlePluginPortal()
mavenCentral()
}
}

사내망에서는 다음 구조가 사용될 수 있다.

개발자 PC
↓
사내 Maven·Plugin Repository
↓
승인된 외부 라이브러리 Cache

운영 프로젝트에서는 개발자가 임의로 외부 저장소를 추가하지 않는다.

### 금지 예

repositories {
maven { url = uri("https://검증되지-않은-저장소") }
}

위험:

-   악성 또는 변조 라이브러리 유입
-   동일 Version의 다른 Artifact 수신
-   네트워크 환경별 빌드 결과 불일치
-   공급망 추적 불가
-   운영 승인되지 않은 License 사용

## 7.1.5 Root build.gradle의 역할

Root 빌드 파일은 전체 프로젝트에 적용할 공통 정책을 정의한다.

대표 항목:

Java Version
Group·Version
공통 Repository
Spring Boot Plugin
Dependency Management
Compiler Encoding
Test Framework
공통 JVM Option
공통 Build Task
WAR 통합 Build
품질검사

개념 예:

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

tasks.withType(Test).configureEach {
useJUnitPlatform()
}
}

NSIGHT 자료는 Java 21과 Spring Boot 3.x 계열, Gradle 8.x를 개발 기준으로 설명한다. 세부 Patch Version은 현재 저장소의 Toolchain, Plugin과 CI 환경을 최종 기준으로 확인해야 한다.

## 7.1.6 allprojects와 subprojects

### allprojects

Root를 포함한 모든 Project에 적용한다.

allprojects {
group = "com.nh.nsight"
version = "1.0.0"
}

### subprojects

Root를 제외한 하위 모듈에 적용한다.

subprojects {
repositories {
mavenCentral()
}
}

### 주의사항

모든 설정을 Root에 몰아넣으면 모듈별 차이를 표현하기 어렵다.

반대로 각 모듈에서 같은 설정을 반복하면 Version과 정책이 분산된다.

공통 정책
→ Root

모듈 고유 정책
→ Module build.gradle

## 7.1.7 모듈별 build.gradle의 역할

모듈별 빌드 파일은 해당 모듈의 성격과 의존성을 정의한다.

| 항목 | 설명 |
| --- | --- |
| Plugin | Java Library·Spring Boot·WAR |
| Dependency | 다른 Project·외부 Library |
| Packaging | JAR·Boot JAR·WAR |
| Main Class | 애플리케이션 기동점 |
| Resource | Mapper XML·YML·정적파일 |
| Test | Test Library·Test Task |
| BootRun | Profile·JVM·Working Directory |
| BootWar | WAR 파일명과 설정 |
| Exclude | 중복·취약 라이브러리 제거 |

## 7.1.8 라이브러리 모듈 예

tcf-core처럼 다른 모듈이 사용하는 공통 모듈은 일반적으로 Library 성격을 갖는다.

plugins {
id "java-library"
}

dependencies {
api project(":tcf-util")

implementation "org.springframework.boot:spring-boot-starter"
implementation "jakarta.validation:jakarta.validation-api"

testImplementation "org.junit.jupiter:junit-jupiter"
}

의미:

tcf-core는 tcf-util을 외부 계약 일부로 공개한다.

tcf-core 자체는 단독 웹 애플리케이션으로 실행되지 않는다.

결과는 일반 JAR다.

## 7.1.9 실행 모듈 예

업무 WAR는 Spring Boot와 WAR Plugin을 사용할 수 있다.

plugins {
id "org.springframework.boot"
id "io.spring.dependency-management"
id "war"
}

dependencies {
implementation project(":tcf-web")
implementation project(":tcf-core")

implementation "org.springframework.boot:spring-boot-starter-web"
implementation "org.mybatis.spring.boot:mybatis-spring-boot-starter"

providedRuntime "org.springframework.boot:spring-boot-starter-tomcat"

testImplementation "org.springframework.boot:spring-boot-starter-test"
}

설명용 예시이며 실제 설정은 해당 모듈의 빌드 파일을 기준으로 한다.

## 7.1.10 providedRuntime의 의미

외부 Tomcat에 WAR를 배포하는 경우 Tomcat Library를 WAR에 중복 포함하지 않도록 providedRuntime을 사용할 수 있다.

로컬 bootRun
→ 내장 Tomcat 사용 가능

외부 Tomcat WAR
→ 서버가 Tomcat Runtime 제공

잘못 포함하면 다음 문제가 생길 수 있다.

Servlet API Version 충돌
Tomcat Library 중복
ClassLoader 충돌
WAR 크기 증가
기동 실패

## 7.1.11 Gradle Wrapper

Gradle Wrapper는 프로젝트가 정한 Gradle Version으로 빌드하게 한다.

구성:

gradlew
gradlew.bat
gradle/wrapper/gradle-wrapper.jar
gradle/wrapper/gradle-wrapper.properties

실행:

./gradlew build

Windows:

gradlew.bat build

### 시스템 Gradle과 비교

| 구분 | Wrapper | 시스템 Gradle |
| --- | --- | --- |
| Version | 프로젝트 고정 | PC별 차이 가능 |
| 설치 | 별도 설치 최소화 | 개발자 설치 필요 |
| CI 재현성 | 높음 | 환경관리 필요 |
| Upgrade | 저장소 변경으로 통제 | PC별 변경 |
| 권장 | 우선 사용 | 프로젝트 정책에 따라 |

다만 현재 Branch에 Wrapper Script와 JAR가 완전하게 포함되어 있지 않거나, 사내망 정책상 시스템 Gradle을 사용하는 경우에는 승인된 실행방식을 따른다.

문서 예시보다
저장소와 CI 실행명령이 우선이다.

## 7.1.12 Wrapper Version 확인

./gradlew --version

또는:

gradle --version

확인 항목:

Gradle Version
JVM Version
JVM Vendor
OS
Gradle Home
Daemon JVM

완료 증적 예:

| 항목 | 확인값 |
| --- | --- |
| Branch | develop |
| Commit | a12bc34 |
| Gradle | 승인된 8.x |
| JVM | JDK 21 |
| Encoding | UTF-8 |
| OS | Windows 또는 Linux |
| Build | Success |

## 7.1.13 gradle.properties

gradle.properties에는 Gradle 실행 옵션과 프로젝트 공통 Property를 둘 수 있다.

예:

org.gradle.jvmargs=-Xms512m -Xmx2g -Dfile.encoding=UTF-8
org.gradle.parallel=true
org.gradle.caching=true

### 주의사항

Gradle Property에는 다음을 넣지 않는다.

DB 비밀번호
Git Token
JWT Private Key
개인 Access Key
운영 Secret

개인 Secret은 사용자 Home의 보호된 설정 또는 승인된 Secret 저장소를 사용한다.

## 7.1.14 Plugin과 Dependency Version

Version은 가능한 한 중앙 관리한다.

Root Plugin Version
Spring Boot BOM
Version Catalog
Dependency Constraint
사내 BOM

개별 모듈마다 같은 라이브러리 Version을 반복하면 다음 문제가 생긴다.

sv-service : Library A 1.1
ic-service : Library A 1.2
tcf-web : Library A 2.0

결과:
Runtime Class 충돌
테스트 환경별 결과 차이
보안 패치 누락

## 7.1.15 BOM

BOM은 관련 라이브러리 Version 조합을 관리한다.

예:

dependencies {
implementation platform(
"org.springframework.boot:spring-boot-dependencies:3.x.x"
)
}

BOM을 사용한다면 임의로 개별 Spring Library Version을 덮어쓰지 않는다.

BOM 사용
\+ 일부 Library 수동 Version 강제
\= 호환성 검증 필요

## 7.1.16 모듈 유형 판정표

| 유형 | 대표 모듈 | 단독 실행 | 대표 산출물 |
| --- | --- | --- | --- |
| 순수 Utility | tcf-util | X | JAR |
| TCF Engine | tcf-core | X | JAR |
| Web 공통 | tcf-web | 일반적으로 X | JAR |
| Cache 공통 | tcf-cache | X | JAR |
| EAI 공통 | tcf-eai | X | JAR |
| 업무 Application | sv-service 등 | O | WAR·Boot JAR |
| Gateway | tcf-gateway | O | WAR·Boot JAR |
| JWT | tcf-jwt | O | WAR·Boot JAR |
| 운영관리 | tcf-om | O | WAR |
| Batch | tcf-batch | O | WAR·Boot JAR |
| UI 지원 | tcf-ui, tcf-uj | 구성에 따라 | WAR·JAR |
| Script·CI | tcf-scripts, tcf-cicd | Gradle 모듈 아닐 수 있음 | Script·설정 |

TCF 플랫폼과 업무 모듈의 세부 역할은 프로젝트 문서에서도 분리되어 있으며, 업무 WAR는 공통 플랫폼 기능을 의존해 실제 업무 거래를 실행하는 구조로 설명된다.

# 7.2 멀티모듈 의존 관계

## 7.2.1 의존성이란 무엇인가

의존성이 있다는 것은 한 모듈이 다른 모듈의 클래스, 설정 또는 Runtime 기능을 필요로 한다는 의미다.

sv-service가
OnlineTransactionController와 TCF를 사용한다.

따라서
sv-service → tcf-web → tcf-core

의존성은 단순히 Compile을 성공시키는 설정이 아니다.

변경 영향
배포 영향
테스트 범위
버전 호환성
장애 전파
보안 취약점

을 결정한다.

## 7.2.2 NSIGHT 권장 의존 방향

┌─────────────┐
│ tcf-util │
└──────▲──────┘
│
┌──────┴──────┐
│ tcf-core │
└──────▲──────┘
│
┌──────────────────┼──────────────────┐
│ │ │
┌──────┴──────┐ ┌──────┴──────┐ ┌──────┴──────┐
│ tcf-web │ │ tcf-eai │ │ tcf-cache │
└──────▲──────┘ └──────▲──────┘ └──────▲──────┘
│ │ │
└──────────────┬───┴──────────┬───────┘
│ │
┌──────┴──────┐ ┌────┴────────┐
│ sv-service │ │ ic-service │
└─────────────┘ └─────────────┘

권장 원칙:

업무 WAR
→ 플랫폼 모듈

플랫폼 모듈
↛ 업무 WAR

업무 WAR
↛ 다른 업무 WAR의 Java 구현

실제 프로젝트의 기본 의존 방향은 업무 WAR가 tcf-web, tcf-core, tcf-util을 사용하는 형태이며, 다른 업무 WAR를 직접 Project Dependency로 참조하지 않는 것이 기준이다.

## 7.2.3 금지 의존 구조

dependencies {
implementation project(":ic-service")
}

SV가 IC 업무 모듈을 직접 참조한 예다.

### 발생 문제

| 문제 | 설명 |
| --- | --- |
| 독립 Build 훼손 | IC 없이는 SV Compile 불가 |
| 독립 배포 훼손 | IC 변경이 SV 재배포로 연결 |
| 순환 의존 | IC가 다시 SV를 참조할 수 있음 |
| 내부 클래스 노출 | IC 구현이 계약처럼 사용됨 |
| 장애 경계 훼손 | 프로세스·배포 경계와 코드 경계 불일치 |
| 버전 충돌 | 두 WAR의 Library Version 결합 |
| 책임 불명확 | 데이터·규칙 소유권 혼재 |

## 7.2.4 권장 업무 간 연계

sv-service
↓
tcf-eai 또는 표준 Client
↓
표준 요청 전문
↓
IC.Customer.selectProfile
↓
ic-service

Java Project Dependency
≠ 업무 서비스 연계

ServiceId 기반 호출
\= 업무 서비스 연계

다른 업무 도메인과의 연동은 공개 계약과 ServiceId를 사용하며, 다른 업무의 DAO·Mapper·테이블을 직접 사용하는 구조는 금지한다.

## 7.2.5 api와 implementation

### api

해당 모듈의 의존성을 소비 모듈에도 노출한다.

api project(":tcf-util")

예:

tcf-core가 공개 Interface에서
tcf-util의 Type을 사용

tcf-core 소비자도
tcf-util Type을 알아야 함

### implementation

해당 모듈 내부 구현에만 사용한다.

implementation project(":tcf-util")

소비 모듈은 내부 의존성에 직접 접근하지 않는다.

## 7.2.6 api와 implementation 선택 기준

| 질문 | api | implementation |
| --- | --- | --- |
| 공개 Method Parameter에 Type이 나타나는가 | O |  |
| 공개 Return Type에 나타나는가 | O |  |
| 상속·구현 공개 계약에 필요한가 | O |  |
| 내부 구현에서만 사용하는가 |  | O |
| 외부에 숨기고 싶은 Library인가 |  | O |
| 소비 모듈 Compile에 필요한가 | O |  |

### 과도한 api의 문제

내부 Library가 모두 외부에 노출
→ 모듈 경계 약화
→ Compile 범위 증가
→ 변경 영향 증가
→ 의존성 충돌 확대

기본은 implementation이며, 공개 계약에 필요한 경우에만 api를 사용한다.

## 7.2.7 주요 Dependency Configuration

| Configuration | 사용 목적 |
| --- | --- |
| api | 외부 공개 계약 |
| implementation | Main 코드 내부 구현 |
| compileOnly | Compile 시만 필요 |
| runtimeOnly | 실행 시만 필요 |
| annotationProcessor | Lombok 등 코드 생성 |
| providedRuntime | 외부 WAS가 제공하는 Runtime |
| testImplementation | Test Compile·실행 |
| testRuntimeOnly | Test Runtime 전용 |
| developmentOnly | 개발환경 전용 |

## 7.2.8 예시

dependencies {
implementation project(":tcf-web")
implementation project(":tcf-core")

implementation "org.mybatis.spring.boot:mybatis-spring-boot-starter"

runtimeOnly "com.h2database:h2"
runtimeOnly "com.oracle.database.jdbc:ojdbc11"

annotationProcessor "org.projectlombok:lombok"

providedRuntime "org.springframework.boot:spring-boot-starter-tomcat"

testImplementation "org.springframework.boot:spring-boot-starter-test"
}

실제 H2·Oracle Driver 적용 범위는 Profile과 프로젝트 정책을 확인해야 한다.

## 7.2.9 Transitive Dependency

다음 구조를 보자.

sv-service
→ tcf-web
→ tcf-core
→ tcf-util

sv-service가 tcf-web만 선언해도 일부 의존성은 간접으로 들어올 수 있다.

이를 Transitive Dependency라고 한다.

### 위험

직접 선언하지 않은 Library 사용
↓
상위 모듈 의존성 변경
↓
업무 모듈 Compile 실패

업무 모듈이 직접 사용하는 Library라면 직접 의존성 선언 여부를 검토한다.

Classpath에 우연히 있으므로 사용
\= 숨은 의존성

직접 필요성을 선언
\= 명시적 의존성

## 7.2.10 의존성 목록 확인

전체:

gradle dependencies

특정 모듈:

gradle :sv-service:dependencies

특정 Configuration:

gradle :sv-service:dependencies \\
\--configuration runtimeClasspath

Windows에서는 명령을 한 줄로 실행할 수 있다.

## 7.2.11 dependencyInsight

특정 Library가 왜 포함되었고 어떤 Version이 선택되었는지 확인한다.

gradle :sv-service:dependencyInsight \\
\--dependency jackson-databind \\
\--configuration runtimeClasspath

확인할 수 있는 것:

요청된 Version
실제로 선택된 Version
어느 모듈이 가져왔는가
Version 충돌이 있었는가
Constraint·BOM의 영향

원본도 의존성 오류를 임의 Version 변경으로 해결하지 말고 dependencyInsight와 BOM·공통 Version 정책으로 충돌 경로를 먼저 식별하도록 안내한다.

## 7.2.12 의존성 충돌 예

tcf-web
→ jackson-databind 2.A

외부 연계 Library
→ jackson-databind 2.B

Gradle
→ 하나의 Version 선택

Compile은 성공할 수 있지만 Runtime에 다음 오류가 발생할 수 있다.

NoSuchMethodError
ClassNotFoundException
NoClassDefFoundError
AbstractMethodError

이때 무작정 최신 Version으로 변경하지 않는다.

진단 순서:

오류 클래스 확인
↓
어느 JAR에 들어 있는지 확인
↓
runtimeClasspath 조회
↓
dependencyInsight 실행
↓
BOM과 Constraint 확인
↓
호환 조합 결정
↓
전체 회귀테스트

## 7.2.13 Dependency Exclude

불필요하거나 충돌하는 Transitive Dependency를 제외할 수 있다.

implementation("sample:legacy-client:1.0") {
exclude group: "commons-logging",
module: "commons-logging"
}

주의:

Exclude 후 Compile 성공
≠ Runtime 정상

제외 대상이 실제 실행에 필요한지 반드시 시험한다.

## 7.2.14 순환 의존성

금지 구조:

tcf-core
→ tcf-web
→ tcf-core

또는:

sv-service
→ ic-service
→ sv-service

순환 의존은 다음을 의미한다.

책임 경계가 불명확하다.

공통 계약이 잘못된 위치에 있다.

도메인이 서로의 내부 구현을 사용한다.

해결 방향:

공통 Contract를 별도 안정 모듈로 이동
업무 간 호출을 ServiceId로 전환
의존성 역전 Interface 적용
모듈 책임 재분리

## 7.2.15 의존성 범위와 보안

모든 Dependency는 공급망 위험을 가진다.

검토 항목:

| 항목 | 확인 |
| --- | --- |
| 출처 | 승인 Repository인가 |
| Version | 승인된 Version인가 |
| License | 사용 가능한가 |
| CVE | 알려진 취약점이 있는가 |
| 유지보수 | 더 이상 관리되지 않는가 |
| Transitive | 불필요한 Library가 포함되는가 |
| Scope | Test Library가 운영에 들어가는가 |
| Checksum | Artifact 무결성을 검증하는가 |

## 7.2.16 Dependency 변경 Pull Request

Library Version 변경은 단순 Build 수정이 아니다.

PR에 포함할 내용:

변경 Library
기존 Version
신규 Version
변경 사유
보안 취약점
API 변경
영향 모듈
Runtime 영향
회귀테스트
Rollback Version

## 7.2.17 모듈 의존성 품질 Gate

자동검증 후보:

tcf-core → 업무 WAR 의존 금지
업무 WAR → 다른 업무 WAR 의존 금지
tcf-util → Spring 의존 금지
Test Dependency의 Runtime 포함 금지
Dynamic Version 금지
Snapshot 운영 사용 금지
승인되지 않은 Repository 금지
중복 Class 검사
취약 Dependency 검사

# 7.3 build·test·bootRun 실행

## 7.3.1 Gradle Task란 무엇인가

Task는 Gradle이 수행하는 하나의 작업이다.

compileJava
processResources
classes
test
jar
bootJar
bootWar
build
bootRun

각 Task는 독립적이지만 선후관계를 가질 수 있다.

## 7.3.2 Task 목록 확인

전체 Task:

gradle tasks

모든 Task:

gradle tasks --all

특정 모듈:

gradle :sv-service:tasks --all

Task가 존재하지 않으면 해당 Plugin이 적용되지 않았을 가능성이 있다.

예:

bootRun Task 없음
→ Spring Boot Plugin 미적용 가능

bootWar Task 없음
→ WAR Plugin 또는 Boot WAR 구성 확인

## 7.3.3 Java Build 생명주기

일반적인 흐름:

compileJava
↓
processResources
↓
classes
↓
compileTestJava
↓
processTestResources
↓
testClasses
↓
test
↓
check
↓
assemble
↓
build

## 7.3.4 주요 Task 비교

| Task | 수행 내용 |
| --- | --- |
| clean | 이전 Build 디렉터리 삭제 |
| compileJava | Main Java Compile |
| processResources | YML·XML·정적파일 복사 |
| classes | Compile과 Resource 처리 |
| test | Test 실행 |
| check | Test·정적검사 등 검증 |
| jar | 일반 JAR 생성 |
| bootJar | 실행 가능한 Spring Boot JAR |
| war | 일반 WAR 생성 |
| bootWar | Spring Boot WAR 생성 |
| assemble | 산출물 생성 |
| build | Assemble + Check |
| bootRun | Spring Boot Application 실행 |
| dependencies | 의존성 Graph 표시 |

## 7.3.5 clean

gradle clean

기존 build/ 산출물을 삭제한다.

### 언제 사용하는가

JDK 변경
Branch 전환
Resource 포함 이상
Generated Source 이상
오래된 Class 의심
CI 재현

### 매번 필요한가

항상 clean하면 Build Cache와 증분 빌드 효과가 줄어든다.

일상 개발
→ 증분 Build

기준 검증·CI·환경 변경
→ Clean Build

## 7.3.6 전체 Build

gradle clean build

Wrapper:

./gradlew clean build

의미:

전체 모듈 Configuration
→ 전체 Compile
→ 전체 Test
→ 전체 Check
→ 전체 Artifact 생성

### 장점

-   전체 프로젝트 정합성 확인
-   공통 모듈 변경 영향 검증
-   업무 WAR 간 Compile 영향 확인
-   CI와 유사한 기준 확보

### 단점

-   시간이 오래 걸릴 수 있음
-   관련 없는 모듈 Test도 실행
-   초기 원인 파악이 어려울 수 있음

## 7.3.7 특정 모듈 Build

gradle :sv-service:build

필요한 선행 모듈도 Task Graph에 따라 함께 Build될 수 있다.

Clean 포함:

gradle clean :sv-service:build

주의:

Root clean
→ 모든 모듈 build 디렉터리 삭제

:sv-service:clean
→ SV 모듈만 삭제

## 7.3.8 플랫폼 모듈 Build

gradle :tcf-util:build
gradle :tcf-core:build
gradle :tcf-web:build

공통 모듈을 변경했다면 소비 업무 모듈의 Test도 수행해야 한다.

tcf-core Test 성공
≠ 모든 업무 WAR 호환성 성공

권장:

공통 모듈 자체 Test
\+ 대표 업무 WAR Build
\+ 전체 Build
\+ 대표 거래 Smoke Test

## 7.3.9 test

전체 Test:

gradle test

모듈 Test:

gradle :sv-service:test

특정 Test Class:

gradle :sv-service:test \\
\--tests "com.nh.nsight.marketing.sv.customer.SvCustomerServiceTest"

특정 Method:

gradle :sv-service:test \\
\--tests "\*.SvCustomerServiceTest.selectSummary"

## 7.3.10 Test 실패 결과 확인

대표 경로:

sv-service/build/reports/tests/test/index.html
sv-service/build/test-results/test/

확인할 것:

실패 Test
기대값
실제값
원인 Exception
표준 출력
실행시간
환경 의존 여부

Test를 단순히 Skip하지 않는다.

## 7.3.11 Test Skip

gradle build -x test

이 명령은 Test를 제외한다.

### 사용 가능 상황

-   원인분석 과정에서 Compile과 Test를 분리
-   임시 진단
-   문서 생성 등 별도 Task

### 금지 상황

Test 실패를 숨기기 위해 운영 Artifact 생성
PR 품질 Gate 우회
배포 일정 때문에 무조건 Skip

운영 배포 Artifact는 승인된 Test Gate를 통과해야 한다.

## 7.3.12 bootRun

업무 모듈 실행:

gradle :sv-service:bootRun

Wrapper:

./gradlew :sv-service:bootRun

bootRun은 일반적으로 다음 작업을 수행한다.

Compile
→ Resource 처리
→ Runtime Classpath 구성
→ Main Class 실행
→ Embedded Tomcat 기동
→ Spring Context 생성

## 7.3.13 bootRun에서 확인할 항목

| 항목 | 예 |
| --- | --- |
| Application Name | nsight-sv-service |
| Active Profile | local |
| Port | 업무 모듈별 Port |
| Context Path | 로컬 설정 |
| Java | JDK 21 |
| Datasource | 로컬 H2·개발 DB |
| TCF Bean | TCF·STF·ETF |
| Handler Registry | ServiceId 등록 |
| Mapper | Interface·XML 등록 |
| JWT | Local 보안정책 |
| Log | GUID·TraceId·ServiceId |

NSIGHT 자료는 로컬 bootRun과 외부 Tomcat WAR를 서로 다른 실행 모드로 구분하고, 동일 애플리케이션을 두 방식에서 모두 검증하도록 설명한다.

## 7.3.14 Profile 전달

명령 예:

gradle :sv-service:bootRun \\
\--args='--spring.profiles.active=local'

또는 Gradle 설정에서 System Property를 주입할 수 있다.

tasks.named("bootRun") {
systemProperty "spring.profiles.active", "local"
systemProperty "file.encoding", "UTF-8"
}

프로젝트 Root에서 공통으로 bootRun Working Directory와 Profile을 통제할 수도 있다.

## 7.3.15 bootRun과 java -jar

| 구분 | bootRun | java -jar |
| --- | --- | --- |
| 입력 | Source·Build 설정 | 완성된 Boot JAR |
| 주용도 | 개발·디버깅 | 실행 Artifact 검증 |
| Classpath | Gradle 구성 | JAR 내부 |
| 변경 반영 | 재실행 필요 | 재Build 필요 |
| Debug | IDE·Gradle 연계 | JVM Option |
| 운영 유사성 | 제한 | Boot JAR 운영 시 높음 |

외부 Tomcat을 사용하는 NSIGHT 운영 구조에서는 java -jar 성공만으로 WAR 배포 성공을 보장할 수 없다.

## 7.3.16 bootJar

gradle :sv-service:bootJar

실행 가능한 Spring Boot JAR를 생성한다.

대표 위치:

sv-service/build/libs/

모듈 정책에 따라 bootJar가 비활성화될 수 있다.

## 7.3.17 bootWar

gradle :sv-service:bootWar

외부 Tomcat에 배포 가능한 WAR를 생성한다.

대표 위치:

sv-service/build/libs/

확인할 것:

WAR 파일명
파일 크기
WEB-INF/classes
WEB-INF/lib
Mapper XML
application.yml
Spring Boot Bootstrap Class
Tomcat Library 중복 여부

## 7.3.18 JAR와 WAR 내부 확인

JAR:

jar tf sv-service/build/libs/sv-service.jar

WAR:

jar tf sv-service/build/libs/sv.war

확인 예:

WEB-INF/classes/application.yml
WEB-INF/classes/mapper/sv/customer/SvCustomerMapper.xml
WEB-INF/classes/com/nh/nsight/...
WEB-INF/lib/tcf-core-...
WEB-INF/lib/tcf-web-...

## 7.3.19 Mapper XML 포함 확인

src/main/resources/mapper
↓
processResources
↓
build/resources/main/mapper
↓
WAR의 WEB-INF/classes/mapper

빌드는 성공했지만 Mapper XML이 산출물에 없다면 Runtime에서 다음 오류가 날 수 있다.

Invalid bound statement
Mapped Statements collection does not contain value
Mapper XML not found

## 7.3.20 Custom Build Task

프로젝트는 여러 WAR를 한 번에 빌드하기 위한 Custom Task를 정의할 수 있다.

예:

buildBusinessWars
buildZtomcatWars

개념적인 목적:

| Task | 목적 |
| --- | --- |
| buildBusinessWars | 주요 업무 WAR와 OM Build |
| buildZtomcatWars | 통합 Tomcat 배포 대상 WAR Build |
| :{module}:bootWar | 단일 WAR Build |
| :{module}:bootRun | 단일 모듈 로컬 실행 |

정확한 대상 모듈과 개수는 현재 Branch의 Root build.gradle을 확인해야 한다.

## 7.3.21 Task 실행계획 확인

실행하지 않고 Task Graph를 확인할 수 있다.

gradle :sv-service:build --dry-run

출력 예:

:tcf-util:compileJava SKIPPED
:tcf-core:compileJava SKIPPED
:tcf-web:compileJava SKIPPED
:sv-service:compileJava SKIPPED
:sv-service:test SKIPPED
:sv-service:bootWar SKIPPED
:sv-service:build SKIPPED

SKIPPED는 Dry Run이므로 실제 실패가 아니다.

## 7.3.22 Build 상세 로그

gradle build --info

더 상세:

gradle build --debug

Stack Trace:

gradle build --stacktrace

전체 Stack Trace:

gradle build --full-stacktrace

주의:

\--debug 로그에는
환경변수·경로·접속정보가 노출될 수 있다.

외부 공유 전 민감정보를 제거한다.

## 7.3.23 Daemon

상태 확인:

gradle --status

중지:

gradle --stop

Daemon 재기동이 필요한 사례:

JDK 변경
환경변수 변경
장시간 실행 후 이상
Plugin Classloader 문제
메모리 부족

재기동이 원인을 해결한 것처럼 보여도 원래 설정 문제를 기록해야 한다.

## 7.3.24 Dependency Cache

강제 재조회:

gradle build --refresh-dependencies

오프라인:

gradle build --offline

### 주의

\--refresh-dependencies를 무조건 사용하면 사내 Repository 부하와 Build 시간이 증가할 수 있다.

Cache가 의심되는 근거가 있을 때 사용

## 7.3.25 병렬 Build

gradle build --parallel

또는:

org.gradle.parallel=true

멀티모듈 Build 시간을 줄일 수 있지만 다음을 점검해야 한다.

Task가 같은 파일을 동시에 수정하는가?
통합 Test가 같은 Port를 쓰는가?
Test DB를 공유하는가?
생성파일 경로가 충돌하는가?

## 7.3.26 Build 산출물 관리

Artifact에는 다음 정보가 연결되어야 한다.

| 항목 | 예 |
| --- | --- |
| Artifact | sv-service-1.4.2.war |
| Commit ID | a12bc34 |
| Branch | release/1.4.2 |
| Build Number | BUILD-0142 |
| JDK | 21 |
| Gradle | 승인 8.x |
| Profile | Build 시 비고 |
| Checksum | SHA-256 |
| Test | PASS |
| SBOM | 파일 또는 참조 |
| 승인 | 배포 승인 ID |

## 7.3.27 Artifact Version

좋지 않은 예:

sv-final.war
sv-new.war
sv-last.war
sv-real-final.war

권장:

sv-service-1.4.2.war

Tomcat에 배포할 때 Context 표준을 위해 sv.war로 이름을 변환할 수 있지만, Artifact Repository에는 원본 Version과 Checksum을 보존한다.

## 7.3.28 재현 가능한 Build

재현 가능한 Build는 같은 입력에서 같은 결과를 만드는 것이다.

입력:

Commit ID
JDK Version
Gradle Version
Plugin Version
Dependency Version
Build Option

출력:

동일한 Class
동일한 Resource
동일한 Dependency
동일한 Artifact 구조

위험 요소:

Dynamic Version
현재시각을 무조건 Manifest에 삽입
개발자 PC 절대경로 포함
외부 Repository의 변경 가능한 Artifact
수동 파일 복사
로컬에만 있는 JAR 참조

# 7.4 빌드 실패를 진단하는 순서

## 7.4.1 첫 번째 원칙: 마지막 메시지만 보지 않는다

Gradle 출력의 마지막에는 다음 문구가 있을 수 있다.

BUILD FAILED

이 문구는 결과일 뿐 원인이 아니다.

확인해야 하는 것은 첫 번째 의미 있는 실패다.

어느 Task가 실패했는가?

예외 유형은 무엇인가?

어느 파일과 몇 번째 줄인가?

Root Cause는 무엇인가?

환경·의존성·소스·Test 중 어디인가?

## 7.4.2 빌드 단계별 실패 분류

\[1\] Gradle 실행 전
JDK·명령·권한·경로

\[2\] 설정 단계
settings.gradle·Plugin·Build Script

\[3\] 의존성 해석
Repository·Version·Network

\[4\] Compile
문법·Type·Annotation Processing

\[5\] Resource
YML·XML·Encoding·중복 파일

\[6\] Test
업무 규칙·Spring Context·DB

\[7\] Package
JAR·WAR·Main Class·중복 Entry

\[8\] bootRun
Port·Profile·Datasource·Bean

\[9\] 배포 후
Tomcat·ClassLoader·환경설정

## 7.4.3 표준 진단 순서

1\. 실패 명령을 정확히 기록한다.
2\. 현재 Branch와 Commit을 기록한다.
3\. JDK와 Gradle Version을 확인한다.
4\. 실패 Task를 식별한다.
5\. 첫 번째 Root Cause를 찾는다.
6\. 문제 범위를 전체·모듈·Test로 축소한다.
7\. Dependency와 설정 차이를 확인한다.
8\. Clean 환경에서 재현한다.
9\. 수정 후 같은 명령으로 재검증한다.
10\. CI와 로컬 결과를 비교한다.

## 7.4.4 1단계: 실행환경 확인

java -version
javac -version
gradle --version
git status -sb
git rev-parse --short HEAD

Wrapper:

./gradlew --version

확인표:

| 항목 | 기대 | 실제 |
| --- | --- | --- |
| Branch | 개발 기준 Branch |  |
| Commit | PR 대상 Commit |  |
| JDK | 21 |  |
| Gradle | 승인 Version |  |
| Encoding | UTF-8 |  |
| Working Directory | Project Root |  |
| Network | Repository 접근 가능 |  |

## 7.4.5 2단계: 실패 Task 확인

예:

Execution failed for task ':sv-service:compileJava'.

이 경우 문제는 Tomcat 기동이 아니라 Java Compile 단계다.

:compileJava
→ Java 소스·Type·Annotation

:test
→ Test 또는 Spring Context

:processResources
→ YML·XML·Resource

:bootWar
→ WAR 패키징

:bootRun
→ Runtime 기동

## 7.4.6 3단계: 상세 오류 출력

gradle :sv-service:build --stacktrace

필요 시:

gradle :sv-service:build --info

로그를 읽는 순서:

실패 Task
↓
Caused by
↓
가장 안쪽 Root Cause
↓
관련 파일·Class·Dependency

## 7.4.7 JDK Version 오류

대표 증상:

Unsupported class file major version
invalid source release
release version 21 not supported

진단:

java -version
javac -version
gradle --version

원인 후보:

JAVA\_HOME은 21
PATH의 java는 17

IDE는 21
Gradle JVM은 17

Compile은 21
실행 Tomcat은 17

해결 후 Gradle Daemon을 중지한다.

gradle --stop

## 7.4.8 Plugin 해석 실패

대표 증상:

Plugin not found
Could not resolve plugin artifact

확인:

Plugin ID
Plugin Version
pluginManagement Repository
사내 Repository
Proxy·인증서
Offline 설정

금지:

인터넷 예시를 보고
Plugin Version을 임의 변경

## 7.4.9 Dependency 조회 실패

대표 증상:

Could not resolve all files
Could not find group:artifact:version
Connection timed out
PKIX path building failed
401 Unauthorized

분류:

| 증상 | 원인 후보 |
| --- | --- |
| Artifact 없음 | Version·Repository 오류 |
| Timeout | Network·Proxy |
| PKIX | 인증서 |
| 401·403 | Repository 권한 |
| Offline | Cache에 Artifact 없음 |
| Snapshot 변경 | Metadata 불일치 |

확인 명령:

gradle :sv-service:dependencies

## 7.4.10 Compile 오류

대표 증상:

cannot find symbol
incompatible types
method does not override
package does not exist

진단:

Import 확인
Method Signature 확인
Dependency Scope 확인
Generated Source 확인
Lombok Annotation Processor 확인
Branch 충돌 확인
공통 모듈 변경 확인

## 7.4.11 Lombok·Annotation Processing 오류

증상:

Getter가 없다고 나옴
Builder Method를 찾지 못함
IDE만 오류 표시
CI에서만 Compile 실패

확인:

compileOnly "org.projectlombok:lombok"
annotationProcessor "org.projectlombok:lombok"

testCompileOnly "org.projectlombok:lombok"
testAnnotationProcessor "org.projectlombok:lombok"

IDE 설정만 수정해서 숨기지 말고 Gradle Compile이 성공해야 한다.

## 7.4.12 Resource 처리 오류

대표 증상:

Duplicate resource
MalformedInputException
YAML parsing failure
XML parsing failure

확인:

파일 Encoding
BOM 존재
중복 경로
잘못된 XML 문자
YAML 들여쓰기
Token 치환
processResources 설정

## 7.4.13 Mapper XML 누락

빌드는 성공했지만 Runtime에서 실패할 수 있다.

Java Mapper Compile 성공
Mapper XML이 Resource에 없음
→ bootRun 시 SQL Mapping 실패

검증:

find sv-service/build/resources/main -type f
jar tf sv-service/build/libs/\*.war

확인:

Mapper XML 경로
Namespace
SQL ID
ResultMap
Parameter Type

## 7.4.14 Test 실패

대표 증상:

expected but was
AssertionFailedError
BusinessException
NullPointerException

판단:

소스 결함인가?
Test 기대값이 잘못되었는가?
테스트 데이터가 변경되었는가?
시간·순서 의존성이 있는가?
다른 Test와 상태를 공유하는가?

Test를 제거하기 전에 실패가 요구사항 변경 때문인지 확인한다.

## 7.4.15 Spring Context Test 실패

대표 증상:

Failed to load ApplicationContext
NoSuchBeanDefinitionException
BeanDefinitionOverrideException
UnsatisfiedDependencyException

확인 순서:

Active Profile
Component Scan
Mapper Scan
AutoConfiguration
중복 Bean
환경 Property
Datasource
순환 의존

NSIGHT에서는 tcf-core, tcf-web과 업무 Handler가 같은 Spring Context에 등록되므로 Scan 범위와 AutoConfiguration 정합성이 중요하다.

## 7.4.16 DB 연결로 인한 Test 실패

증상:

Connection refused
Invalid username/password
Table not found
Schema not found
Pool initialization failed

확인:

Test가 H2를 쓰는가?
개발 DB를 쓰는가?
Profile이 올바른가?
DDL 초기화가 수행되는가?
환경변수가 있는가?

단위 Test가 외부 DB에 의존하지 않도록 구조를 검토한다.

## 7.4.17 Port 충돌

bootRun 실패:

Port 8086 was already in use

확인:

Windows:

netstat -ano | findstr 8086

Linux:

ss -lntp | grep 8086

잘못된 대응:

무조건 Port를 임의 변경

Port는 다른 업무 모듈·Gateway·UI 호출 설정과 연결될 수 있으므로 전체 로컬 Port 표를 확인한다.

## 7.4.18 Main Class 오류

증상:

Main class name has not been configured
Unable to find a single main class

확인:

@SpringBootApplication 클래스 수
main Method
bootRun mainClass
패키지 위치
Module Plugin

한 모듈에 여러 Bootstrap Class가 있으면 명시적 설정이 필요할 수 있다.

## 7.4.19 WAR 생성 실패

확인:

war Plugin
Spring Boot Plugin
bootWar 활성화
Main Class
SpringBootServletInitializer
Tomcat providedRuntime
중복 파일

로컬 bootRun 성공과 bootWar 성공은 별도 완료 조건이다.

## 7.4.20 Runtime Class 충돌

Compile·Test는 성공했지만 bootRun이나 Tomcat에서 실패할 수 있다.

대표 오류:

NoSuchMethodError
NoClassDefFoundError
ClassCastException
LinkageError

진단:

gradle :sv-service:dependencyInsight \\
\--dependency <라이브러리명> \\
\--configuration runtimeClasspath

WAR 내부 WEB-INF/lib도 확인한다.

## 7.4.21 Duplicate Class

대표 증상:

Duplicate class
Duplicate entry
Bean 이름 중복

원인 후보:

같은 Class가 두 JAR에 존재
Legacy Library와 신규 Library 동시 포함
Source Set 중복
Generated Source 중복
업무 WAR 간 잘못된 JAR 포함

## 7.4.22 Gradle Daemon 문제

증상:

Daemon disappeared unexpectedly
JVM Crash
OutOfMemoryError
빌드가 멈춤

확인:

gradle --status
gradle --stop

그리고:

gradle.properties JVM Heap
운영체제 메모리
병렬 Build
Test Fork 수
백신·파일 잠금

## 7.4.23 Build Cache 문제

증상:

소스를 수정했는데 반영되지 않음
Branch 변경 후 이상한 Compile 오류
Resource가 이전 버전

진단:

gradle clean
gradle build --no-build-cache

무조건 Cache를 삭제하기보다 재현 조건을 기록한다.

## 7.4.24 로컬 성공·CI 실패

비교 항목:

| 영역 | 로컬 | CI |
| --- | --- | --- |
| JDK |  |  |
| Gradle |  |  |
| OS |  |  |
| Locale |  |  |
| Timezone |  |  |
| Encoding |  |  |
| Branch·Commit |  |  |
| Environment |  |  |
| Repository |  |  |
| Test 순서 |  |  |
| Clean Build |  |  |

대표 원인:

로컬에만 있는 JAR
개인 Maven Cache
대소문자 파일명 차이
Windows·Linux 경로 차이
환경변수 누락
Test 순서 의존
개인 설정파일 의존

## 7.4.25 CI 성공·운영 배포 실패

Build 성공
↓
WAR 생성 성공
↓
외부 Tomcat 배포 실패

확인:

운영 JDK
Tomcat Version
WAR 파일명
Context Path
setenv
Profile
Datasource
File 권한
ClassLoader
외부 설정

Build 성공
≠ 운영 기동 성공

# 빌드 진단 의사결정 트리

Gradle 명령 자체가 실행되는가?
├─ 아니오
│ └─ JAVA\_HOME·PATH·Gradle·권한 확인
│
└─ 예
↓
설정 단계가 성공하는가?
├─ 아니오
│ └─ settings·build script·plugin 확인
│
└─ 예
↓
Dependency를 가져오는가?
├─ 아니오
│ └─ repository·network·version·certificate 확인
│
└─ 예
↓
Compile이 성공하는가?
├─ 아니오
│ └─ source·type·annotation processor 확인
│
└─ 예
↓
Test가 성공하는가?
├─ 아니오
│ └─ test data·business rule·context·DB 확인
│
└─ 예
↓
Package가 생성되는가?
├─ 아니오
│ └─ bootJar·bootWar·resource·duplicate 확인
│
└─ 예
↓
bootRun이 성공하는가?
├─ 아니오
│ └─ profile·port·DB·bean·runtime dependency 확인
│
└─ 예
↓
대표 거래가 성공하는가?
├─ 아니오
│ └─ TCF·Handler·Mapper·설정·데이터 확인
│
└─ 예
└─ 로컬 Build·실행 완료

# 정상 처리 흐름

올바른 Branch·Commit
↓
JDK·Gradle Version 확인
↓
settings.gradle 모듈 확인
↓
Dependency Graph 확인
↓
Clean Build
↓
Unit·Integration Test
↓
업무 모듈 bootRun
↓
대표 ServiceId Smoke Test
↓
bootWar 생성
↓
WAR 내부 Resource 확인
↓
Artifact·Checksum 저장
↓
CI/CD 승인

# 오류 흐름

Build 실패
↓
실패 Task 식별
↓
첫 Root Cause 확인
↓
환경·설정·의존성·Compile·Test·Package 분류
↓
범위를 모듈 단위로 축소
↓
같은 명령으로 재현
↓
원인 수정
↓
해당 Task 재검증
↓
전체 Build
↓
대표 거래 회귀검증

# 정상 예시

Branch
feature/REQ-SV-014-customer-summary

Commit
a12bc34

환경
JDK 21
Gradle 승인 8.x
UTF-8

명령
gradle clean :sv-service:build

결과
tcf-util build PASS
tcf-core build PASS
tcf-web build PASS
sv-service compile PASS
sv-service test PASS
sv-service bootWar PASS

추가검증
gradle :sv-service:bootRun
SV.Customer.selectSummary PASS

산출물
sv-service-1.4.2.war

추적정보
REQ-SV-014
SV.Customer.selectSummary
BUILD-0142
a12bc34

# 금지 예시

프로젝트 Root가 아닌 임의 하위 폴더에서 Build한다.

시스템 Gradle Version이 달라도 그대로 사용한다.

Dependency 오류가 나면 무조건 최신 Version으로 변경한다.

업무 WAR에서 다른 업무 WAR를 project dependency로 추가한다.

공통 모듈이 업무 DTO를 참조한다.

Test 실패를 -x test로 숨기고 WAR를 배포한다.

bootRun 성공만 확인하고 bootWar 검증을 생략한다.

Mapper XML이 WAR에 포함되었는지 확인하지 않는다.

BUILD SUCCESSFUL만 보고 대표 거래 시험을 생략한다.

로컬 Maven Cache에만 있는 JAR를 사용한다.

동적 Version이나 변경 가능한 Snapshot을 운영에 사용한다.

WAR 파일명을 final·new·last로 관리한다.

Artifact와 Commit ID를 연결하지 않는다.

Build 로그에 비밀번호와 Token을 출력한다.

# 책임 경계와 RACI

| 활동 | 아키텍처 | 프레임워크팀 | 업무개발팀 | DevOps | 보안·품질 |
| --- | --- | --- | --- | --- | --- |
| 모듈 경계 정의 | A | R | C | C | I |
| Root Build 정책 | C | R/A | C | R | C |
| 업무 모듈 Build | I | C | R/A | C | I |
| 의존성 Version | A | R | C | C | C |
| 업무 Library 추가 | C | C | R | C | A/C |
| 공통 Library 추가 | A | R | C | C | C |
| Unit Test | I | C | R/A | I | C |
| 통합 Build | C | C | R | R/A | C |
| Dependency Scan | I | C | C | R | A |
| Artifact 생성 | I | I | C | R/A | C |
| WAR 배포 | I | C | C | R/A | I |
| Build 실패 분석 | C | C | R | R | I |
| 운영 호환성 검증 | A | C | C | R | C |

# 자동검증 및 품질 Gate

## 1\. 환경 Gate

JDK Version 일치
Gradle Version 일치
Encoding UTF-8
승인 Repository 사용
금지 환경변수 없음

## 2\. 구조 Gate

업무 WAR 간 직접 의존 금지
TCF Core의 업무 모듈 의존 금지
tcf-util의 Spring 의존 금지
순환 Project Dependency 금지
미등록 모듈 금지

## 3\. Dependency Gate

취약 Library
승인되지 않은 License
Dynamic Version
Snapshot
중복 Class
Version 충돌
Test Library 운영 포함

## 4\. Compile Gate

Java Compile
Annotation Processing
Warning 기준
Generated Source
Deprecated API
Encoding

## 5\. Test Gate

Unit Test
Integration Test
Context Test
Architecture Test
보안 Test
대표 ServiceId Test

## 6\. Package Gate

JAR·WAR 생성
파일명 표준
Mapper XML 포함
application.yml 포함
중복 Library 없음
Tomcat Library Scope 정상
Checksum 생성

## 7\. 추적성 Gate

Commit ID
Build Number
Artifact Version
요구사항 ID
ServiceId
Test ID
배포 승인

# 테스트 시나리오

| ID | 시나리오 | 기대 결과 |
| --- | --- | --- |
| GRD-001 | gradle projects 실행 | 전체 모듈 목록 표시 |
| GRD-002 | 미등록 디렉터리 존재 | Gradle Project로 미표시 |
| GRD-003 | JDK 21 Build | 성공 |
| GRD-004 | 낮은 JDK Version Build | Toolchain·Compile 실패 |
| GRD-005 | Wrapper Version 확인 | 승인 Version 표시 |
| GRD-006 | 전체 clean build | 전체 모듈 성공 |
| GRD-007 | :sv-service:build | SV와 선행 모듈 성공 |
| GRD-008 | :tcf-core:build | Core JAR와 Test 성공 |
| GRD-009 | :sv-service:test | 업무 Test 성공 |
| GRD-010 | 특정 Test 실행 | 대상 Test만 실행 |
| GRD-011 | Test 실패 | Build 실패 |
| GRD-012 | \-x test 사용 | Test 미실행 상태 식별 |
| GRD-013 | bootRun 실행 | Local Profile과 Port 정상 |
| GRD-014 | Port 중복 | 명확한 기동 실패 |
| GRD-015 | bootWar 실행 | WAR 생성 |
| GRD-016 | Mapper XML 포함 | WAR 내부 확인 |
| GRD-017 | 잘못된 Namespace | Runtime·통합 Test 실패 |
| GRD-018 | 업무 WAR 직접 의존 | 구조 Gate 실패 |
| GRD-019 | 순환 Project Dependency | Build 실패 |
| GRD-020 | Version 충돌 | dependencyInsight로 경로 확인 |
| GRD-021 | Repository 접근 실패 | Dependency Resolution 실패 |
| GRD-022 | 인증서 오류 | PKIX 오류와 조치 식별 |
| GRD-023 | Lombok Processor 누락 | Compile 실패 |
| GRD-024 | 중복 Class | Package·Runtime 검증 실패 |
| GRD-025 | Resource 인코딩 오류 | 처리 단계에서 실패 |
| GRD-026 | Spring Context 중복 Bean | Context Test 실패 |
| GRD-027 | 외부 DB 미접속 | Profile·Test DB 검증 |
| GRD-028 | Build Cache 비활성 검증 | Clean 재현 성공 |
| GRD-029 | 로컬·CI Version 불일치 | 환경 Gate 실패 |
| GRD-030 | Artifact와 Commit 연결 | 추적성 확인 |
| GRD-031 | WAR Tomcat 배포 | Context 기동 성공 |
| GRD-032 | 대표 ServiceId 호출 | 표준 응답·로그 정상 |
| GRD-033 | Secret이 Build Log에 출력 | 보안 Gate 실패 |
| GRD-034 | Snapshot 운영 Dependency | 품질 Gate 실패 |
| GRD-035 | 공통 모듈 변경 회귀 | 전체 업무 Build 성공 |

# 따라 하는 실무 절차

## 1단계. Project Root를 확인한다

git rev-parse --show-toplevel

다음 파일을 찾는다.

settings.gradle
build.gradle
gradle.properties
gradle/wrapper/gradle-wrapper.properties

## 2단계. Version을 기록한다

java -version
gradle --version
git rev-parse --short HEAD

## 3단계. 모듈 목록을 확인한다

gradle projects

모듈을 다음으로 분류한다.

| 모듈 | 유형 | 산출물 |
| --- | --- | --- |
| tcf-util | Library | JAR |
| tcf-core | Library | JAR |
| tcf-web | Library | JAR |
| sv-service | 업무 실행 | WAR |
| tcf-om | 운영 실행 | WAR |
| tcf-gateway | Gateway | WAR·JAR |

## 4단계. 업무 모듈 의존성을 확인한다

gradle :sv-service:dependencies \\
\--configuration runtimeClasspath

확인:

tcf-web
tcf-core
tcf-util
MyBatis
Spring Boot
DB Driver
Tomcat Scope

## 5단계. 금지 의존성을 확인한다

sv-service → ic-service가 있는가?
tcf-core → sv-service가 있는가?
tcf-util → Spring Boot가 있는가?

## 6단계. Clean Build를 실행한다

gradle clean :sv-service:build

실패하면 실패 Task를 기록한다.

## 7단계. Test 결과를 확인한다

sv-service/build/reports/tests/test/index.html

## 8단계. 업무 모듈을 실행한다

gradle :sv-service:bootRun

확인:

Profile
Port
Datasource
TCF
Handler Registry
Mapper

## 9단계. 대표 거래를 호출한다

{
"header": {
"businessCode": "SV",
"serviceId": "SV.Customer.selectSummary",
"transactionCode": "SV-INQ-0001"
},
"body": {
"customerNo": "C0001"
}
}

확인:

표준 응답
ServiceId
GUID
TraceId
SQL 실행
거래로그 종료

## 10단계. WAR를 만든다

gradle :sv-service:bootWar

## 11단계. WAR 내부를 확인한다

jar tf sv-service/build/libs/\*.war

확인:

Application Class
Mapper XML
YML
tcf-core JAR
tcf-web JAR
업무 Class

## 12단계. 빌드 증적을 기록한다

| 항목 | 값 |
| --- | --- |
| Branch |  |
| Commit |  |
| JDK |  |
| Gradle |  |
| Task |  |
| 결과 |  |
| Test |  |
| Artifact |  |
| Checksum |  |
| ServiceId Smoke Test |  |

# 완료 체크리스트

## 설정파일

| 확인 항목 | 완료 |
| --- | --- |
| Project Root를 확인했다. | □ |
| settings.gradle의 모듈을 확인했다. | □ |
| Root build.gradle을 확인했다. | □ |
| 업무 모듈 build.gradle을 확인했다. | □ |
| Java Toolchain을 확인했다. | □ |
| Spring Boot Version 기준을 확인했다. | □ |
| Repository가 승인된 위치인지 확인했다. | □ |
| Wrapper 또는 시스템 Gradle 정책을 확인했다. | □ |
| gradle.properties에 Secret이 없는지 확인했다. | □ |
| UTF-8 Compile 설정을 확인했다. | □ |

## 의존성

| 확인 항목 | 완료 |
| --- | --- |
| 실행 모듈과 Library 모듈을 구분했다. | □ |
| 업무 WAR의 공통 모듈 의존성을 확인했다. | □ |
| 다른 업무 WAR 직접 의존이 없다. | □ |
| 플랫폼의 업무 모듈 역의존이 없다. | □ |
| api와 implementation 사용이 적절하다. | □ |
| Runtime Dependency를 확인했다. | □ |
| Test Dependency가 운영에 포함되지 않는다. | □ |
| Version 충돌을 확인했다. | □ |
| 취약 Dependency 검사를 수행했다. | □ |
| Dynamic·Snapshot Version을 통제했다. | □ |

## Build·Test

| 확인 항목 | 완료 |
| --- | --- |
| Clone 직후 Clean Build를 수행했다. | □ |
| 특정 업무 모듈 Build를 수행했다. | □ |
| Unit Test가 성공했다. | □ |
| Integration Test가 성공했다. | □ |
| Spring Context Test가 성공했다. | □ |
| Test 결과서를 확인했다. | □ |
| 실패 Test를 Skip하여 숨기지 않았다. | □ |
| 공통 모듈 변경 시 전체 회귀를 수행했다. | □ |
| 로컬과 CI의 Version을 비교했다. | □ |
| Build 로그를 보존했다. | □ |

## 실행·산출물

| 확인 항목 | 완료 |
| --- | --- |
| 업무 모듈 bootRun이 성공했다. | □ |
| Active Profile을 확인했다. | □ |
| 업무 Port를 확인했다. | □ |
| 대표 ServiceId 호출이 성공했다. | □ |
| bootWar가 성공했다. | □ |
| WAR 파일명이 표준과 일치한다. | □ |
| Mapper XML이 WAR에 포함되어 있다. | □ |
| Tomcat Library Scope가 적절하다. | □ |
| Artifact와 Commit ID가 연결된다. | □ |
| Checksum·Build 번호를 기록했다. | □ |

## 오류 진단

| 확인 항목 | 완료 |
| --- | --- |
| 실패 명령을 기록했다. | □ |
| 실패 Task를 찾았다. | □ |
| 첫 Root Cause를 확인했다. | □ |
| 환경·의존성·Compile·Test를 구분했다. | □ |
| \--stacktrace 결과를 확인했다. | □ |
| 필요한 경우 dependencyInsight를 실행했다. | □ |
| Clean 환경에서 재현했다. | □ |
| 수정 후 같은 명령으로 재검증했다. | □ |
| 전체 Build를 다시 수행했다. | □ |
| 대표 거래 회귀검증을 수행했다. | □ |

# 변경·호환성·폐기 관리

## Gradle Version 변경

검토사항:

JDK 호환성
Spring Boot Plugin 호환성
기존 Custom Task
CI Runner
IDE Plugin
Build Cache
Wrapper

완료 기준:

전체 Clean Build
전체 Test
대표 bootRun
전체 WAR 생성
CI 검증
배포 리허설

## Java Version 변경

Source Compatibility
Target Compatibility
Toolchain
Annotation Processor
Library 호환성
Tomcat 실행 JDK

로컬 JDK만 변경해서는 안 된다.

개발자 PC
CI
Test 서버
운영 Tomcat
배포 Script

를 함께 변경한다.

## Spring Boot Version 변경

영향:

Spring Framework
Spring Security
Embedded Tomcat
Actuator
Jackson
Validation
MyBatis Starter
AutoConfiguration

단순 Patch 변경도 전체 회귀가 필요하다.

## Dependency 폐기

사용 Class 검색
↓
Runtime Classpath 확인
↓
대체 Library 선정
↓
코드 전환
↓
Dependency 제거
↓
Clean Build
↓
Runtime·보안 회귀

## 모듈 폐기

모듈 디렉터리만 삭제하지 않는다.

settings.gradle include 제거
Project Dependency 제거
Custom Task 제거
CI Pipeline 제거
배포 Script 제거
OM Catalog 영향 확인
Artifact 보관정책 적용
문서·구성도 갱신

# 시사점

## 핵심 아키텍처 판단

Gradle 의존성은 실제 아키텍처를 보여준다.

문서에 아무리 좋은 계층과 도메인 경계를 작성해도 다음 설정이 존재하면 실제 아키텍처는 이미 무너진 것이다.

implementation project(":다른-업무-service")

아키텍처 구성도
≠ 실제 의존 구조

Gradle Dependency Graph
\= 실제 Compile 구조

따라서 Architecture Review에서는 구성도와 패키지만 보지 말고 실제 settings.gradle, build.gradle과 Runtime Classpath를 확인해야 한다.

## 주요 위험

| 위험 | 영향 |
| --- | --- |
| Gradle Version 불일치 | 개발자·CI Build 차이 |
| JDK 불일치 | Compile·Runtime 오류 |
| 업무 WAR 직접 의존 | 독립 배포 훼손 |
| 과도한 api | 내부 구현 노출 |
| Transitive 의존 | 숨은 결합 |
| BOM 무시 | Library 충돌 |
| 무분별한 Exclude | Runtime Class 누락 |
| Test Skip | 결함 Artifact 배포 |
| bootRun만 검증 | 외부 Tomcat 실패 |
| Resource 누락 | Mapper·YML Runtime 오류 |
| Dynamic Version | 재현 불가능 |
| Artifact 추적정보 누락 | 장애 버전 식별 불가 |
| 로컬 전용 JAR | CI·운영 Build 실패 |
| 승인되지 않은 Repository | 공급망 위험 |

## 우선 보완 과제

1.  Gradle Wrapper 또는 승인 시스템 Gradle 정책을 단일화한다.
2.  Java Toolchain을 JDK 21 기준으로 고정한다.
3.  Root Build에서 공통 Encoding과 Test 정책을 강제한다.
4.  모듈 의존 방향을 자동검증한다.
5.  업무 WAR 간 Project Dependency를 차단한다.
6.  Dependency Version을 BOM 또는 Version Catalog로 통합한다.
7.  취약점·License 검사를 CI에 적용한다.
8.  dependencyInsight 사용법을 개발표준에 포함한다.
9.  bootRun과 bootWar를 모두 품질 Gate에 포함한다.
10.  Mapper XML·YML의 Artifact 포함 검사를 자동화한다.
11.  Artifact에 Commit ID·Build Number·Checksum을 연결한다.
12.  공통 모듈 변경 시 대표 업무 WAR 회귀검증을 자동화한다.
13.  운영 WAR와 Artifact Repository 원본의 대응관계를 관리한다.
14.  Build 실패 분류와 표준 진단 절차를 Runbook으로 운영한다.

## 중장기 발전 방향

수동 Gradle Build
↓
표준 Wrapper
↓
멀티모듈 의존성 Gate
↓
Dependency Lock·BOM
↓
자동 Test·구조검사
↓
취약점·License·SBOM
↓
재현 가능한 Artifact
↓
서명·Checksum
↓
Commit부터 운영 배포까지 공급망 추적

# 마무리말

Gradle을 잘 사용한다는 것은 build 명령을 외우는 것이 아니다.

다음 질문에 답할 수 있어야 한다.

어떤 모듈이 빌드에 참여하는가?

이 업무 WAR는 무엇을 의존하는가?

왜 이 Library가 Runtime에 들어왔는가?

어느 Task가 실패했는가?

Test가 실제로 실행되었는가?

운영 Tomcat에 배포할 WAR는 무엇인가?

Mapper와 설정파일이 산출물에 들어 있는가?

이 Artifact는 어느 Commit에서 만들어졌는가?

제7장에서 가장 자주 사용할 명령은 다음과 같다.

gradle projects
gradle tasks --all
gradle clean build
gradle :sv-service:build
gradle :sv-service:test
gradle :sv-service:bootRun
gradle :sv-service:bootWar
gradle :sv-service:dependencies
gradle :sv-service:dependencyInsight
gradle --version
gradle --stop

가장 중요한 원칙은 다음과 같다.

BUILD SUCCESSFUL
\= 빌드 도구가 요청받은 Task를 성공함

업무 개발 완료
\= Build
\+ Test
\+ bootRun
\+ 대표 거래
\+ WAR
\+ 배포 호환성
\+ 운영 추적성

다음 장에서는 Gradle로 빌드한 업무 모듈을 로컬에서 실행하고, Profile·Port·Datasource를 확인하며, Breakpoint와 로그를 이용해 화면 요청부터 Handler·Service·Mapper까지 디버깅하는 방법을 학습한다.
