<!-- source: ztcf-집필본/NSIGHT TCF Chapter 9- Program Analysis Guide.docx -->
<!-- tables: mammoth-html + placeholder table-to-md -->

# 제3부. 기존 프로그램을 읽는 법을 배우다

## 도입 전 안내말

제2부에서는 개발환경을 구성하고 Git 저장소를 가져와 Gradle로 빌드한 뒤, 업무 애플리케이션을 로컬에서 실행하고 디버깅하는 방법을 살펴보았다.

이제부터는 이미 만들어져 있는 프로그램을 읽어야 한다.

프로젝트에 처음 참여한 개발자가 바로 새로운 코드를 작성하면 다음 문제가 발생하기 쉽다.

프로젝트 표준을 모른다.
↓
인터넷 예제나 이전 프로젝트 코드를 복사한다.
↓
기능은 동작한다.
↓
TCF 거래통제·Timeout·로그 체계와 맞지 않는다.
↓
운영·테스트 단계에서 다시 수정한다.

기존 프로그램을 읽는 목적은 단순히 코드를 이해하는 것이 아니다.

프로젝트에서 인정된 정상 구현을 찾고,
그 구현에 포함된 설계 판단을 이해한 뒤,
새로운 요구사항에 필요한 부분만 안전하게 확장하는 것

이 제3부에서는 다음 순서로 기존 프로그램을 분석한다.

저장소의 전체 지도를 만든다.
↓
화면과 ServiceId에서 프로그램을 찾는다.
↓
SQL과 테이블에서 영향 거래를 역추적한다.
↓
변경 전 영향 범위를 정리한다.

## 제3부의 목표

| 학습 영역 | 제3부를 마친 뒤 할 수 있어야 하는 것 |
| --- | --- |
| 저장소 탐색 | 처음 보는 저장소에서 실행·공통·업무 모듈을 구분한다. |
| 거래 추적 | ServiceId에서 Handler·Facade·Service·Mapper를 찾는다. |
| 화면 추적 | 화면 이벤트와 호출 ServiceId를 연결한다. |
| 데이터 추적 | SQL·테이블에서 영향 ServiceId와 화면을 역추적한다. |
| 설정 추적 | Profile·Port·DB·Timeout 설정 위치를 찾는다. |
| 변경 영향 | 코드 한 줄을 변경하기 전에 영향 범위를 작성한다. |
| 실행 검증 | 정적 검색 결과를 실제 로그와 테스트로 검증한다. |
| 표준 판단 | 현재 구현과 목표 아키텍처 표준의 차이를 구분한다. |

## 제3부 전체 학습 흐름

| 구분 | 제9장 | 제10장 | 제11장 | 제12장 |
| --- | --- | --- | --- | --- |
| 핵심 질문 | 저장소를 어디서부터 읽는가? | 화면과 거래는 어디에서 연결되는가? | SQL과 테이블의 사용처는 어디인가? | 이 변경이 어디까지 영향을 주는가? |
| 출발점 | 모듈·패키지·리소스 | 화면 ID·ServiceId | SQL ID·Table | 요구사항·변경파일 |
| 주요 도구 | Project View·Search | 문자열 검색·Call Hierarchy | Mapper 검색·DB 정의 | 추적성 매트릭스 |
| 완료 결과 | 저장소 지도 | 거래 호출 지도 | 데이터 영향 지도 | 변경 영향 보고서 |
| 검증 방법 | 대표 거래 실행 | GUID·TraceId 로그 | SQL 실행·DB 결과 | 회귀테스트 |

### 그림으로 보는 제3부 학습 여정

\[저장소 전체 구조\]
↓
실행 모듈과 라이브러리 구분
↓
\[정방향 추적\]
화면 → ServiceId → 프로그램 → SQL
↓
\[역방향 추적\]
Table → SQL → Mapper → ServiceId → 화면
↓
\[변경 영향\]
코드·설정·DB·배포·운영 영향 확인

# 제9장. 처음 보는 저장소 탐색법

## 이 장을 시작하며

처음 보는 대규모 저장소를 열면 다음과 같은 생각이 들 수 있다.

파일이 너무 많다.

어디가 시작점인지 모르겠다.

비슷한 이름의 Service가 여러 개다.

공통 코드와 업무 코드가 섞여 보인다.

문서도 많고 소스도 많다.

모든 파일을 읽어야 할 것 같다.

그러나 대규모 프로젝트를 이해하기 위해 모든 파일을 처음부터 순서대로 읽을 필요는 없다.

오히려 그렇게 읽으면 중요한 실행 경로를 놓치기 쉽다.

저장소 탐색의 핵심은 다음 세 가지다.

첫째,
모든 파일을 동일한 중요도로 보지 않는다.

둘째,
대표 거래 하나를 기준으로 연결된 파일만 먼저 읽는다.

셋째,
정적 검색 결과를 실제 실행 로그와 테스트로 검증한다.

NSIGHT TCF 저장소는 플랫폼 모듈, 실행 애플리케이션, 업무 WAR, 설정·스크립트, 문서·테스트 자료가 함께 존재한다. 따라서 먼저 파일의 수가 아니라 **책임과 실행 경계**를 기준으로 분류해야 한다.

프로젝트 자료도 내부 프레임워크를 읽을 때 모든 클래스를 순서대로 보지 말고 실제 ServiceId 하나를 선택하여 진입점부터 업무 Service까지 호출 스택을 따라가도록 권장한다. 그 후 예외 흐름과 로그 생성 지점을 역방향으로 확인하는 것이 효과적이다.

## 핵심 관점

저장소를 읽는 기준은
디렉터리 순서가 아니다.

실행되는 모듈
→ 요청 진입점
→ ServiceId
→ 업무 책임
→ 데이터 처리
→ 운영 증거

순서로 읽는다.

## 학습 목표

이 장을 마치면 다음 내용을 직접 수행할 수 있어야 한다.

| 번호 | 학습 목표 |
| --- | --- |
| 1 | 저장소 Root와 Gradle Module을 구분한다. |
| 2 | 실행 모듈과 라이브러리 모듈을 구분한다. |
| 3 | 플랫폼 모듈과 업무 WAR의 책임을 설명한다. |
| 4 | 문서·스크립트·UI·생성파일을 소스 모듈과 구분한다. |
| 5 | 업무 모듈의 Main Class와 기동 설정을 찾는다. |
| 6 | 업무 패키지에서 Handler·Facade·Service·Rule·DAO·Mapper를 찾는다. |
| 7 | application.yml과 환경별 설정파일을 찾는다. |
| 8 | Mapper XML·DDL·초기데이터·로그 설정 위치를 찾는다. |
| 9 | 테스트 코드와 샘플 요청을 찾는다. |
| 10 | ServiceId 한 건의 전체 소스 지도를 작성한다. |
| 11 | 현재 구현과 목표 패키지 표준을 구분한다. |
| 12 | 문자열 검색과 Call Hierarchy의 차이를 설명한다. |
| 13 | 검색 결과를 Breakpoint와 로그로 검증한다. |
| 14 | 폐기·샘플·우회 코드와 운영 코드를 구분한다. |
| 15 | 저장소 분석 결과를 다른 개발자가 재사용할 수 있게 기록한다. |

# 한눈에 보는 저장소 탐색 흐름

┌─────────────────────────────────────────────────────────────┐
│ 1. 저장소 Root 확인 │
│ Git·settings.gradle·build.gradle·README │
└──────────────────────────┬──────────────────────────────────┘
▼
┌─────────────────────────────────────────────────────────────┐
│ 2. 모듈 분류 │
│ Platform Library · 실행 Application · 업무 WAR │
│ Script · UI · 문서 · 생성파일 │
└──────────────────────────┬──────────────────────────────────┘
▼
┌─────────────────────────────────────────────────────────────┐
│ 3. 업무 모듈 선택 │
│ 예: sv-service │
└──────────────────────────┬──────────────────────────────────┘
▼
┌─────────────────────────────────────────────────────────────┐
│ 4. 실행 정보 확인 │
│ Main Class · build.gradle · Profile · Port · Datasource │
└──────────────────────────┬──────────────────────────────────┘
▼
┌─────────────────────────────────────────────────────────────┐
│ 5. 대표 ServiceId 선택 │
│ SV.Customer.selectSummary │
└──────────────────────────┬──────────────────────────────────┘
▼
┌─────────────────────────────────────────────────────────────┐
│ 6. 정방향 소스 추적 │
│ Handler → Facade → Service → Rule → DAO → Mapper │
└──────────────────────────┬──────────────────────────────────┘
▼
┌─────────────────────────────────────────────────────────────┐
│ 7. 설정·리소스 연결 │
│ YML · Mapper XML · schema.sql · data.sql · logback │
└──────────────────────────┬──────────────────────────────────┘
▼
┌─────────────────────────────────────────────────────────────┐
│ 8. 실행 검증 │
│ bootRun · 표준 요청 · GUID 로그 · SQL · DB 결과 │
└──────────────────────────┬──────────────────────────────────┘
▼
┌─────────────────────────────────────────────────────────────┐
│ 9. 저장소 지도 작성 │
│ 프로그램·설정·데이터·테스트·운영 연결표 │
└─────────────────────────────────────────────────────────────┘

# 저장소에서 확인할 다섯 개의 지도

| 지도 | 핵심 질문 | 대표 확인 대상 |
| --- | --- | --- |
| 모듈 지도 | 무엇이 실행되고 무엇이 포함되는가 | settings.gradle, build.gradle |
| 실행 지도 | 요청은 어디에서 시작되는가 | Main Class, Controller, Filter |
| 업무 지도 | 실제 업무는 어디에서 처리되는가 | Handler·Facade·Service·Rule |
| 데이터 지도 | 어떤 SQL과 테이블을 사용하는가 | DAO·Mapper XML·DDL |
| 운영 지도 | 장애 시 무엇으로 찾는가 | ServiceId·GUID·TraceId·로그 |

# 탐색 전 준비사항

저장소를 읽기 전에 다음 기준을 기록한다.

| 항목 | 기록 예 |
| --- | --- |
| Repository | 공식 저장소명 |
| Branch | develop |
| Commit | a12bc34 |
| JDK | 21 |
| Gradle | 승인된 8.x |
| 대상 모듈 | sv-service |
| 대상 ServiceId | SV.Customer.selectSummary |
| Profile | local |
| 탐색 목적 | 고객요약 조회 변경 분석 |
| 작성자 | 담당 개발자 |
| 작성일 | 분석 수행일 |

같은 파일이라도 Branch와 Commit이 다르면 내용이 달라질 수 있으므로 분석 결과에는 반드시 Commit ID를 함께 기록한다.

# 9.1 실행 모듈과 라이브러리 구분

## 9.1.1 모듈부터 구분해야 하는 이유

저장소의 디렉터리는 모두 같은 성격이 아니다.

tcf-core
tcf-web
tcf-gateway
tcf-jwt
sv-service
docs
scripts
node\_modules

이들을 모두 “프로그램”이라고 생각하면 다음 질문에 답하기 어렵다.

어느 모듈을 실행해야 하는가?

어느 모듈은 JAR로 포함되는가?

어느 모듈이 WAR를 만드는가?

어느 모듈의 장애가 독립적인가?

어느 디렉터리는 빌드 대상이 아닌가?

## 9.1.2 최상위 분류

NSIGHT 저장소의 최상위 구성은 다음처럼 분류한다.

저장소
├─ 플랫폼 라이브러리
├─ 플랫폼 실행 애플리케이션
├─ 업무 애플리케이션
├─ 개발·배포 도구
├─ UI
├─ 문서
└─ 생성·외부 의존 파일

## 9.1.3 플랫폼 라이브러리

다른 실행 애플리케이션이 가져다 사용하는 공통 모듈이다.

| 모듈 | 주요 책임 | 일반 산출물 | 단독 실행 |
| --- | --- | --- | --- |
| tcf-util | 상태 없는 공통 유틸리티 | JAR | X |
| tcf-core | TCF·STF·ETF·Dispatcher·Context | JAR | X |
| tcf-web | 공통 Controller·JWT Filter·AutoConfiguration | JAR | X |
| tcf-eai | 업무 WAR 간 표준 연계 Client | JAR | X |
| tcf-cache | Cache 공통 계약과 구현 | JAR | X |

플랫폼 라이브러리는 보통 다음 특징을 가진다.

@SpringBootApplication이 없다.

bootRun 대상이 아니다.

다른 모듈의 dependencies에서 project(...)로 참조된다.

일반 JAR를 생성한다.

## 9.1.4 플랫폼 실행 애플리케이션

독립적인 실행 목적이 있는 플랫폼 모듈이다.

| 모듈 | 주요 책임 | 실행·배포 형태 |
| --- | --- | --- |
| tcf-gateway | 외부 요청 인증·라우팅 | Boot JAR·WAR |
| tcf-jwt | JWT 발급·갱신·JWKS | Boot JAR·WAR |
| tcf-om | 운영 기준정보·통제·관리화면 | WAR |
| tcf-batch | Batch·Scheduler 실행 | Boot JAR·WAR |
| tcf-ui | 개발·관리 UI 지원 | 구성에 따라 |
| tcf-uj | 채널·UI 연계 | 구성에 따라 |

실행 모듈은 일반적으로 다음 중 하나를 가진다.

@SpringBootApplication
public static void main(...)
bootRun Task
bootJar 또는 bootWar
독립 Port
독립 Profile
Health Endpoint

## 9.1.5 업무 애플리케이션

실제 고객·상품·캠페인 등 업무 거래를 소유하는 모듈이다.

대표 예:

sv-service
ic-service
pc-service
ms-service
pd-service
eb-service
ep-service
ss-service
mg-service

업무 모듈은 다음을 포함한다.

업무 Main Class
업무 Handler
Facade
Service
Rule
DAO
Mapper
업무 DTO
업무 설정
Mapper XML
업무 테스트

## 9.1.6 현재 확인되는 업무·플랫폼 모듈

현재 기준 소스에는 다음과 같은 모듈군이 존재한다.

| 분류 | 확인되는 주요 모듈 |
| --- | --- |
| 플랫폼 Core | tcf-util, tcf-core, tcf-web |
| 플랫폼 확장 | tcf-cache, tcf-eai, tcf-batch |
| 인증·라우팅 | tcf-gateway, tcf-jwt |
| 운영 | tcf-om, 레거시 성격의 om-service |
| 채널·UI | tcf-ui, tcf-uj |
| 개발·배포 | tcf-cicd, tcf-scripts, scripts |
| 업무 | sv, ic, pc, ms, pd, eb, ep, ss, mg 계열 |
| 문서 | docs, zarchitecture, zman, ztcfbook 계열 |

프로젝트 자료에서도 플랫폼은 tcf-util, tcf-core, tcf-web, Gateway·JWT·OM·EAI·Cache·Batch 등으로 나누고, 업무 거래는 별도의 업무 WAR에서 실행하도록 설명한다.

## 9.1.7 문서·스크립트·UI 구분

### 문서 디렉터리

docs
zarchitecture
zdoc
zguide
zman
znsight-man
ztcfbook
ztcfbook-m
ztcfbook-h

문서는 설계 의도와 표준을 이해하는 데 중요하지만 실제 실행 여부는 소스와 설정으로 다시 검증해야 한다.

문서에 적혀 있다.
≠ 현재 코드에 구현되어 있다.

### 스크립트 디렉터리

scripts
tcf-scripts
tcf-cicd

확인할 것:

Build 명령
배포 대상 WAR
Profile
환경변수
Tomcat 경로
Rollback 방식

### 생성·외부 의존 디렉터리

node\_modules
build
out
logs
data

이 디렉터리는 원칙적으로 핵심 Java 구조 분석 대상에서 제외한다.

## 9.1.8 실행 모듈 판정 절차

### 1단계: settings.gradle

gradle projects

모듈이 Gradle Project로 등록되어 있는지 확인한다.

### 2단계: build.gradle

검색:

org.springframework.boot
war
java-library
bootJar
bootWar

### 3단계: Main Class

검색:

@SpringBootApplication
public static void main
SpringBootServletInitializer

### 4단계: 실행 Task

gradle :sv-service:tasks --all

### 5단계: 산출물

JAR
Boot JAR
WAR

## 9.1.9 실행 모듈 판정표

| 확인 | 라이브러리 | 실행 애플리케이션 |
| --- | --- | --- |
| @SpringBootApplication | 일반적으로 X | O |
| bootRun | 일반적으로 X | O |
| 독립 Port | X | O |
| 일반 JAR | O | 가능 |
| WAR | X | 가능 |
| 다른 모듈에 포함 | O | 일반적으로 X |
| 독립 Health | X | O |
| 독립 배포 | X | O |

## 9.1.10 빌드 단위와 장애 단위

모듈을 구분할 때는 빌드 단위와 런타임 장애 단위를 혼동하지 않는다.

sv-service
\= 독립 Gradle 모듈
\= 독립 WAR

그러나

하나의 Tomcat에 여러 WAR 배포
\= JVM·Heap·GC·Connector Thread 공유

| 구분 | 의미 |
| --- | --- |
| 모듈 경계 | Compile과 Dependency 경계 |
| WAR 경계 | Spring Context와 ClassLoader 경계 |
| Tomcat 경계 | JVM·Thread·Heap·GC 경계 |
| VM 경계 | OS·CPU·Memory 경계 |

## 9.1.11 의존 방향 확인

권장:

업무 WAR
↓
tcf-web
↓
tcf-core
↓
tcf-util

금지:

tcf-core
↓
sv-service

금지:

sv-service
↓ Java Project Dependency
ic-service

업무 WAR 간 연계는 다른 WAR의 Java 클래스를 참조하기보다 ServiceId 기반 연계 Client를 사용해야 한다.

## 9.1.12 의존성 확인 명령

gradle :sv-service:dependencies

Runtime 기준:

gradle :sv-service:dependencies \\
\--configuration runtimeClasspath

특정 라이브러리:

gradle :sv-service:dependencyInsight \\
\--dependency <library-name> \\
\--configuration runtimeClasspath

## 9.1.13 현재 구현과 목표 구조 구분

저장소를 읽을 때 매우 중요한 원칙이다.

현재 존재하는 구조
\= As-Is 구현

프로젝트가 지향하는 표준
\= To-Be 기준

현재 업무 소스에서는 다음과 같은 계층 중심 패키지가 확인된다.

com.nh.nsight.marketing.sv
├─ entry
│ ├─ handler
│ ├─ facade
│ └─ controller
├─ application
│ ├─ service
│ ├─ rule
│ └─ dto
├─ persistence
│ ├─ dao
│ ├─ mapper
│ └─ dto
└─ client

현재 소스의 대표 호출 구조도 다음과 같다.

SvCustomerHandler
→ SvCustomerFacade
→ SvCustomerService
→ SvCustomerRule·SvCustomerDao
→ SvCustomerMapper
→ SvCustomerMapper.xml

실제 샘플에서는 SvCustomerHandler가 SV.Customer.selectSummary를 등록하고, SvCustomerFacade가 조회 트랜잭션 경계를 갖는 구조가 확인된다.

반면 장기 목표 패키지 표준은 다음 형태다.

com.nh.nsight.marketing.{업무코드}.{도메인}.{계층}

예:

com.nh.nsight.marketing.sv.customer.handler
com.nh.nsight.marketing.sv.customer.facade
com.nh.nsight.marketing.sv.customer.service
com.nh.nsight.marketing.sv.customer.rule
com.nh.nsight.marketing.sv.customer.dao
com.nh.nsight.marketing.sv.customer.mapper

목표 구조는 업무 도메인, ServiceId, 데이터 소유권과 변경 책임을 같은 경계로 정렬하기 위한 것이다.

### 판단 원칙

현재 소스를 읽을 때
→ 현재 경로를 정확히 기록한다.

신규 구조를 설계할 때
→ 프로젝트 승인 표준을 적용한다.

현재 소스를 표준과 다르다는 이유만으로
즉시 대규모 이동하지 않는다.

패키지 이동은 Import, Spring Scan, Mapper Namespace, 테스트, 운영 추적성에 영향을 주므로 별도 리팩터링 과제로 관리한다.

# 9.2 패키지·설정·리소스 지도 만들기

## 9.2.1 저장소 지도란 무엇인가

저장소 지도는 폴더 목록을 복사한 문서가 아니다.

다음 질문에 답하는 연결표다.

어떤 모듈인가?

어디에서 실행되는가?

어떤 ServiceId를 처리하는가?

어떤 업무 계층을 거치는가?

어떤 설정을 사용하는가?

어떤 SQL과 테이블을 사용하는가?

어떤 테스트로 검증되는가?

운영에서 어떤 로그로 찾는가?

## 9.2.2 대표 업무 모듈 지도

sv-service를 예로 들면 다음과 같은 지도를 만들 수 있다.

sv-service
├─ src/main/java
│ └─ com/nh/nsight/marketing/sv
│ ├─ NsightSvServiceApplication.java
│ ├─ entry
│ │ ├─ controller
│ │ ├─ handler
│ │ └─ facade
│ ├─ application
│ │ ├─ dto
│ │ ├─ service
│ │ ├─ rule
│ │ └─ scheduler
│ ├─ persistence
│ │ ├─ dao
│ │ ├─ mapper
│ │ └─ dto
│ ├─ client
│ ├─ config
│ └─ support
│
├─ src/main/resources
│ ├─ application.yml
│ ├─ application-local.yml
│ ├─ application-dev.yml
│ ├─ application-prod.yml
│ ├─ logback-spring.xml
│ ├─ mapper
│ │ └─ sv
│ ├─ schema.sql
│ └─ data.sql
│
└─ src/test
├─ java
└─ resources

## 9.2.3 패키지별 책임 지도

| 패키지 | 현재 구현상 책임 | 먼저 볼 파일 |
| --- | --- | --- |
| 업무 Base | Main Class·업무 전체 경계 | NsightSvServiceApplication |
| entry.controller | 직접·특수 진입 Endpoint | Controller |
| entry.handler | ServiceId 등록과 Facade 호출 | SvCustomerHandler |
| entry.facade | 유스케이스 조립·트랜잭션 | SvCustomerFacade |
| application.service | 업무 처리 흐름 | SvCustomerService |
| application.rule | 업무 검증·조건 판단 | SvCustomerRule |
| application.dto | 요청·응답·조회조건 | Customer DTO |
| persistence.dao | Mapper 호출 추상화 | SvCustomerDao |
| persistence.mapper | MyBatis Interface | SvCustomerMapper |
| persistence.dto | SQL 조회 결과 | CustomerSummaryRow |
| client | 다른 업무·외부 시스템 연계 | IcIntegrationClient |
| config | 업무 전역 Spring 설정 | Configuration |
| support | 업무 내부 지원기능 | 최소 범위 |

## 9.2.4 대표 거래의 프로그램 지도

ServiceId
SV.Customer.selectSummary
↓
Handler
SvCustomerHandler
↓
Facade
SvCustomerFacade.selectCustomerSummary()
↓
Request 변환
CustomerSummaryRequest
↓
Service
SvCustomerService.selectCustomerSummary()
↓
Rule
SvCustomerRule
↓
Criteria
CustomerSummaryCriteria
↓
DAO
SvCustomerDao.selectCustomerSummary()
↓
Mapper Interface
SvCustomerMapper.selectCustomerSummary()
↓
Mapper XML
SvCustomerMapper.xml
↓
SQL ID
selectCustomerSummary
↓
Table
SV\_CUSTOMER
↓
Result
CustomerSummaryRow
↓
Response
CustomerSummaryResponse

NSIGHT TCF의 공통 소스 지도 역시 OnlineTransactionController → TCF → STF → TransactionDispatcher → Handler → Facade → Service → Rule·DAO → Mapper → ETF 흐름으로 정리된다.

## 9.2.5 요청·응답 DTO 지도

DTO를 찾을 때 이름만 보지 않고 사용 경계를 확인한다.

| DTO | 경계 | 확인할 내용 |
| --- | --- | --- |
| Request | Handler·Facade 진입 | 화면 입력과 필수값 |
| Response | 화면 반환 | 외부 공개 필드 |
| Criteria·Query | Service→DAO | DB 조회조건 |
| Command | 변경 유스케이스 | 변경할 값 |
| Result·Row | Mapper→DAO | DB 결과 |
| Contract DTO | 업무·시스템 연계 | 버전·호환성 |

금지:

하나의 Map 또는 DTO를
Controller부터 Mapper까지 그대로 전달

문제:

-   계층 간 결합
-   DB 컬럼이 화면에 노출
-   개인정보가 불필요하게 전달
-   필드 변경 영향이 전체 계층으로 확산

## 9.2.6 설정 지도

업무 애플리케이션의 설정은 다음처럼 분류한다.

| 설정 영역 | 대표 파일·위치 | 핵심 질문 |
| --- | --- | --- |
| 공통 Spring | application.yml | 모든 환경 공통인가 |
| 로컬 | application-local.yml | Port·H2·Mock 설정은 무엇인가 |
| 개발 | application-dev.yml | 개발 DB·연계 URL은 무엇인가 |
| 운영 | application-prod.yml | 운영 값은 외부화되었는가 |
| 로그 | logback-spring.xml | 업무별 파일·마스킹 정책은 무엇인가 |
| DB | Datasource·Hikari | 어느 DB·Schema인가 |
| MyBatis | Mapper Location | XML 경로가 어디인가 |
| TCF | Timeout·통제·로그 | 공통 정책은 어디에서 오는가 |
| JWT | issuer·audience·JWKS | 누가 검증하는가 |
| 외부연계 | Client URL·Timeout | 환경별 대상은 무엇인가 |

## 9.2.7 설정 Property 추적법

설정값 하나를 찾을 때 다음 순서로 추적한다.

YML Property
↓
@ConfigurationProperties
↓
AutoConfiguration
↓
생성 Bean
↓
사용 Service

예:

tcf.timeout.default-ms
↓
TcfProperties
↓
TcfOnlineTimeoutConfiguration
↓
OnlineTransactionTimeoutExecutor

문자열 검색만 하지 말고 Property Binding 클래스와 Bean 생성 위치까지 확인한다.

## 9.2.8 설정 우선순위 기록

| 우선순위 | 설정 원천 |
| --- | --- |
| 1 | 명령행 인수 |
| 2 | JVM 시스템 속성 |
| 3 | 환경변수 |
| 4 | 외부 설정 파일 |
| 5 | application-{profile}.yml |
| 6 | application.yml |
| 7 | 코드 기본값 |

파일에 적힌 값과 실제 실행값이 다를 수 있다.

YML에서 8086 확인
≠ 실제 Port 8086 확정

기동 로그에서 최종값 확인
\= 실제 Port 확정

## 9.2.9 리소스 지도

src/main/resources는 Java 소스만큼 중요하다.

| 리소스 | 역할 | 누락 시 증상 |
| --- | --- | --- |
| application.yml | Spring 기본설정 | Profile·Bean 오류 |
| application-local.yml | 로컬환경 | 잘못된 DB·Port |
| logback-spring.xml | 로그 정책 | 로그 누락·경로 충돌 |
| Mapper XML | SQL 정의 | Invalid bound statement |
| schema.sql | 로컬 DDL | 테이블 없음 |
| data.sql | 로컬 초기데이터 | 조회 결과 없음 |
| Message Bundle | 사용자 메시지 | 코드만 노출 |
| 정적 파일 | UI·템플릿 | 404·화면 오류 |

## 9.2.10 Mapper 지도

Mapper는 Interface와 XML을 함께 확인한다.

Java Mapper FQCN
↕ XML namespace
Mapper Method
↕ SQL id
Parameter Type
↕ parameterType
Result Type
↕ resultType·resultMap

확인표:

| 확인 항목 | 정상 기준 |
| --- | --- |
| Namespace | Mapper Interface 전체 경로와 일치 |
| SQL ID | Method 이름과 일치 |
| Parameter | Query·Criteria Type과 일치 |
| Result | Row·Result DTO와 일치 |
| Timeout | 거래 정책과 정합 |
| Table | 소유 업무가 명확 |
| SQL Comment | SQL ID·목적 식별 |
| 개인정보 | 불필요한 조회 없음 |

## 9.2.11 DDL·초기데이터 지도

schema.sql
→ 어떤 테이블을 만드는가

data.sql
→ 어떤 테스트 데이터를 입력하는가

Mapper XML
→ 어떤 테이블을 사용하는가

Test
→ 어떤 데이터 조건을 기대하는가

이 네 가지가 일치해야 로컬 실행이 재현된다.

## 9.2.12 테스트 지도

테스트를 마지막에 읽지 않는다.

테스트에는 개발자가 의도한 계약이 표현되어 있을 수 있다.

| 테스트 유형 | 읽을 내용 |
| --- | --- |
| Unit Test | Rule·Service의 기대 규칙 |
| Mapper Test | SQL과 Result Mapping |
| Context Test | Bean·Mapper·Handler 등록 |
| Integration Test | 전체 거래와 DB 결과 |
| Contract Test | 업무·외부 시스템 전문 |
| Architecture Test | 패키지·의존 방향 |
| Smoke Test | 대표 ServiceId 실행 |

## 9.2.13 샘플 요청 지도

대표 경로:

docs/sample-requests
src/test/resources
tcf-scripts

확인:

Endpoint
Header
ServiceId
거래코드
Body
기대 Result
인증 Header

샘플 요청이 현재 DTO와 맞는지 실제 실행으로 검증한다.

## 9.2.14 공통 플랫폼 지도

현재 소스 인덱스에서 핵심 위치는 다음처럼 정리된다.

| 기능 | 대표 위치 |
| --- | --- |
| 표준 전문 | tcf-core/.../message |
| 거래 Context | tcf-core/.../context |
| TCF | tcf-core/.../processor/TCF.java |
| STF | tcf-core/.../processor/STF.java |
| ETF | tcf-core/.../processor/ETF.java |
| Dispatcher | tcf-core/.../TransactionDispatcher.java |
| Handler 계약 | tcf-core/.../TransactionHandler.java |
| 공통 Controller | tcf-web/.../OnlineTransactionController.java |

이 Source Index는 저장소 전체에서 핵심 소스의 위치를 먼저 찾도록 제공된 문서다.

## 9.2.15 운영 지도

프로그램 지도에는 운영 식별자도 포함해야 한다.

| 식별자 | 찾을 위치 |
| --- | --- |
| 업무코드 | Header·WAR·Context |
| ServiceId | 화면·Handler·OM·로그 |
| 거래코드 | Header·통제·감사 |
| GUID | 거래 단위 로그 |
| TraceId | 시스템 간 호출 |
| SQL ID | Mapper·Slow SQL 로그 |
| App Version | 기동 로그·Artifact |
| Commit ID | Build·배포 이력 |

## 9.2.16 저장소 지도 표준 양식

| 구분 | 기록 |
| --- | --- |
| 업무코드 | SV |
| Gradle 모듈 | sv-service |
| 배포물 | sv.war |
| Main Class | NsightSvServiceApplication |
| Profile | local, dev, prod |
| Port | 환경별 값 |
| Context | /sv |
| 대표 ServiceId | SV.Customer.selectSummary |
| Handler | SvCustomerHandler |
| Facade | SvCustomerFacade |
| Service | SvCustomerService |
| Rule | SvCustomerRule |
| DAO | SvCustomerDao |
| Mapper | SvCustomerMapper |
| Mapper XML | mapper/sv/SvCustomerMapper.xml |
| SQL ID | selectCustomerSummary |
| Table | SV\_CUSTOMER |
| Request DTO | CustomerSummaryRequest |
| Response DTO | CustomerSummaryResponse |
| Test | 대표 통합테스트 |
| 로그 | ServiceId·GUID·TraceId |
| OM | Catalog·Timeout·거래통제 |

## 9.2.17 현재 구현 상태 표시

저장소 지도에는 상태를 함께 기록한다.

| 상태 | 의미 |
| --- | --- |
| 구현 확인 | 실제 소스와 실행으로 확인 |
| 부분 구현 | 일부 코드만 존재 |
| 설계 기준 | 문서에는 있으나 구현 검증 필요 |
| 권장 확장 | 향후 적용을 권고 |
| 폐기 예정 | 신규 사용 금지 |
| 프로젝트 확인 필요 | Branch·환경에 따라 다름 |

예:

| 항목 | 상태 | 설명 |
| --- | --- | --- |
| SV.Customer.selectSummary | 구현 확인 | Handler부터 Mapper까지 존재 |
| 도메인 우선 패키지 | 설계 기준 | 현재 구조와 차이 존재 |
| 17개 업무 WAR | 목표 기준 | 현재 구현 수와 차이 존재 |
| 직접 TCF 우회 API | 제한 샘플 | 운영 사용 금지 검토 |

## 9.2.18 지도 작성 시 금지사항

디렉터리 이름만 보고 책임을 추측한다.

문서 경로를 실제 소스 경로로 단정한다.

샘플 클래스를 운영 기준으로 복사한다.

Legacy와 신규 구현을 구분하지 않는다.

현재 구현과 목표 표준을 섞어 기록한다.

테스트 코드와 실행 로그를 확인하지 않는다.

Mapper Interface만 보고 XML을 보지 않는다.

application.yml만 보고 최종 설정값을 확정한다.

# 9.3 읽을 파일의 우선순위

## 9.3.1 모든 파일을 읽지 않는다

저장소에 파일이 1,000개 있다고 해서 1번부터 1,000번까지 읽는 방식은 효과적이지 않다.

모든 파일 순차 읽기
→ 맥락 없는 세부사항 증가
→ 중요 경로 상실
→ 피로 증가

권장 방식:

대표 거래 선정
→ 관련 파일만 정방향 추적
→ 공통 기능은 필요한 시점에 확장
→ 실행으로 검증

## 9.3.2 첫 번째 읽기: 저장소 안내문

우선순위:

README
SOURCE\_INDEX
Architecture Overview
개발자 가이드

확인할 것:

프로젝트 목적
기술 스택
실행 방법
모듈 목록
대표 거래
주요 문서 링크
현재 제한사항

주의:

문서가 오래되었을 수 있으므로 현재 Branch의 코드와 비교한다.

## 9.3.3 두 번째 읽기: 빌드 구조

settings.gradle
Root build.gradle
대상 모듈 build.gradle
gradle.properties

확인:

참여 모듈
Project Dependency
Java Version
Spring Boot Version
JAR·WAR
Test 설정
Repository

이 단계를 건너뛰면 어떤 모듈이 실행되는지 알 수 없다.

## 9.3.4 세 번째 읽기: 업무 실행 정보

Main Class
application.yml
application-local.yml
logback-spring.xml

확인:

Application Name
Port
Profile
Datasource
Mapper Scan
Component Scan
Context Path
로그 위치

## 9.3.5 네 번째 읽기: 대표 요청

다음 중 하나를 찾는다.

sample request JSON
통합테스트 요청
curl script
Postman Collection
화면 설계서

대표 ServiceId를 확정한다.

SV.Customer.selectSummary

## 9.3.6 다섯 번째 읽기: Handler

Handler는 업무 프로그램 탐색의 시작점이다.

확인:

serviceIds()
doHandle()
switch 분기
Facade 호출
Request Body 변환
미지원 ServiceId 처리

Handler에서 SQL이나 복잡한 업무판단을 수행한다면 구조 위반 가능성이 있다.

## 9.3.7 여섯 번째 읽기: Facade

확인:

유스케이스 단위
@Transactional
readOnly
timeout
Service 호출 순서
여러 도메인 조합

Facade는 구현 상세보다 거래 경계를 이해하기 위해 읽는다.

## 9.3.8 일곱 번째 읽기: Service와 Rule

Service:

업무 처리 순서
Rule 호출
DAO 호출
외부 연계
결과 조립

Rule:

필수·상태·중복·자격 검증
부작용 여부
DB·외부 호출 여부

Rule이 DB를 호출한다면 책임을 재검토한다.

## 9.3.9 여덟 번째 읽기: DAO와 Mapper

DAO Method
↓
Mapper Method
↓
XML Namespace·SQL ID
↓
SQL
↓
Table·View
↓
Result DTO

확인:

조회·등록·변경·삭제
영향 행 수
Timeout
정렬
페이징
Lock
민감 컬럼

## 9.3.10 아홉 번째 읽기: 공통 TCF

처음부터 TCF 내부 모든 클래스를 읽지 않는다.

대표 거래를 따라가다가 공통 처리가 필요한 지점에서 읽는다.

권장 순서:

OnlineTransactionController
↓
TCF.process()
↓
STF.preProcess()
↓
OnlineTransactionTimeoutExecutor
↓
TransactionDispatcher
↓
TransactionHandler
↓
ETF

## 9.3.11 열 번째 읽기: 테스트

구현을 읽은 후 테스트로 의도를 확인한다.

정상 입력
경계값
업무 오류
DB 오류
Timeout
Rollback

테스트가 없다면 구현의 계약을 별도로 문서화하고 테스트 보완 과제로 등록한다.

## 9.3.12 열한 번째 읽기: 운영 문서

OM Service Catalog
거래통제
Timeout 정책
오류코드
거래로그
감사로그
배포 Runbook

소스가 동작해도 운영 등록정보가 없으면 실제 환경에서 거래가 차단되거나 무제한으로 실행될 수 있다.

## 9.3.13 ServiceId에서 시작하는 정방향 추적

SV.Customer.selectSummary
↓ 문자열 검색
SvCustomerHandler.serviceIds()
↓
doHandle() switch
↓
SvCustomerFacade
↓
SvCustomerService
↓
SvCustomerRule·SvCustomerDao
↓
SvCustomerMapper
↓
SvCustomerMapper.xml
↓
SV\_CUSTOMER

완료 기준:

정적 검색
\+ Call Hierarchy
\+ Breakpoint
\+ GUID 로그
\+ SQL·DB 결과

## 9.3.14 화면에서 시작하는 추적

화면 ID
↓
이벤트 ID
↓
요청 생성 Script
↓
ServiceId
↓
Handler 이하

화면과 ServiceId는 일대일이 아닐 수 있다.

하나의 화면 이벤트
→ 여러 ServiceId

여러 화면
→ 하나의 공통 ServiceId

화면 이벤트와 ServiceId, 프로그램, SQL과 DB 객체는 추적성 매트릭스로 관리하는 것이 적절하다.

## 9.3.15 SQL에서 시작하는 역방향 추적

Table
↓
Mapper XML 검색
↓
SQL ID
↓
Mapper Interface
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

예:

SV\_CUSTOMER
↓
SvCustomerMapper.xml
↓
selectCustomerSummary
↓
SvCustomerMapper
↓
SvCustomerDao
↓
SvCustomerService
↓
SvCustomerFacade
↓
SvCustomerHandler
↓
SV.Customer.selectSummary

## 9.3.16 오류코드에서 시작하는 추적

오류코드
↓
ErrorCode 정의
↓
throw 위치
↓
Rule·Service
↓
호출 ServiceId
↓
응답·화면 처리

동일 오류코드를 여러 거래가 사용할 수 있으므로 ServiceId·GUID를 함께 확인한다.

## 9.3.17 로그에서 시작하는 추적

운영 장애 분석:

GUID
↓
ServiceId
↓
업무코드
↓
처리 단계
↓
오류코드
↓
Mapper SQL ID
↓
소스

로그에서 우선 확인할 필드:

| 필드 | 용도 |
| --- | --- |
| GUID | 거래 한 건 연결 |
| TraceId | 시스템 간 호출 연결 |
| ServiceId | 업무 처리기 식별 |
| BusinessCode | 담당 WAR 식별 |
| Step | Controller·STF·Handler·SQL |
| ErrorCode | 오류 유형 |
| Elapsed | 지연 구간 |
| SQL ID | Mapper 식별 |
| App Version | 배포 버전 |

## 9.3.18 검색어 우선순위

| 출발 정보 | 첫 검색어 |
| --- | --- |
| 화면 | 화면 ID·이벤트 ID |
| 거래 | ServiceId |
| 운영 | GUID·TraceId |
| SQL | SQL ID·Table |
| 오류 | 오류코드 |
| 설정 | Property Key |
| 클래스 | Bean 이름·FQCN |
| 연계 | 대상 업무코드·Client |
| 배포 | Artifact·Context Path |

## 9.3.19 IDE 검색 기능 활용

### 전체 문자열 검색

사용:

ServiceId
Property Key
SQL ID
Table Name
Error Code

### Find Usages

사용:

Java Method
Class
Interface
DTO
Mapper Method

### Call Hierarchy

사용:

누가 이 메서드를 호출하는가?
이 메서드는 무엇을 호출하는가?

### Type Hierarchy

사용:

TransactionHandler 구현체
공통 Interface 구현체
추상 클래스 확장

### Navigate to Symbol

사용:

클래스·메서드 이름을 알고 있을 때

## 9.3.20 명령행 검색 예

Linux·Git Bash:

grep -R "SV.Customer.selectSummary" \\
\--include="\*.java" \\
\--include="\*.xml" \\
\--include="\*.yml" .

Table 검색:

grep -R "SV\_CUSTOMER" \\
\--include="\*.xml" \\
\--include="\*.sql" .

Property 검색:

grep -R "server.port" \\
\--include="application\*.yml" .

생성 디렉터리 제외:

grep -R "SV.Customer.selectSummary" . \\
\--exclude-dir=build \\
\--exclude-dir=node\_modules \\
\--exclude-dir=.git

## 9.3.21 문자열 검색의 한계

문자열 검색으로 놓칠 수 있는 것:

상수로 조합된 ServiceId
Interface Proxy
Spring Bean 동적 주입
MyBatis 동적 SQL
Reflection
Annotation 기반 등록
환경변수 치환

따라서 다음으로 보완한다.

Find Usages
Call Hierarchy
Bean 목록
Dispatcher 등록 로그
Breakpoint
통합테스트

## 9.3.22 실행으로 검증해야 하는 이유

정적 검색 결과:

SvCustomerHandler가
SV.Customer.selectSummary를 처리한다.

실행 검증:

실제 요청 ServiceId
→ Dispatcher 선택 Handler
→ Call Stack
→ Mapper SQL
→ ETF 응답

두 결과가 같아야 한다.

소스에 존재
≠ 실제 실행

실제 실행 로그와 Call Stack
\= 런타임 근거

## 9.3.23 대표 정상 거래 선택 기준

좋은 기준 거래:

| 기준 | 설명 |
| --- | --- |
| 운영 사용 | 실제로 사용되는 거래 |
| 단순성 | 흐름을 이해하기 쉬움 |
| 표준성 | TCF 6계층을 따름 |
| 테스트 | 정상 테스트 존재 |
| 데이터 | 로컬에서 재현 가능 |
| 로그 | GUID 추적 가능 |
| 문서 | ServiceId·요청 예시 존재 |

피해야 할 기준 거래:

폐기 예정 거래
임시 Test Controller
TCF 우회 샘플
복잡한 다중 시스템 연계
대용량 Batch
장애 재현용 코드

## 9.3.24 현재 코드와 문서가 다를 때

판단 순서:

1\. 현재 Branch·Commit 확인
2\. 실제 Build 대상 확인
3\. 실행 로그 확인
4\. 최근 승인 문서 확인
5\. 담당 아키텍트·개발자 확인
6\. Gap 등록

임의로 한쪽을 정답으로 선택하지 않는다.

## 9.3.25 코드가 두 벌 있을 때

예:

tcf-om
om-service

신규 Handler
legacy Handler

공통 Controller
업무 직접 Controller

확인:

settings.gradle 포함 여부
build.gradle 의존 여부
배포 Script 대상
현재 Tomcat 배포물
최근 Commit
운영 로그 Package

파일이 존재한다는 이유만으로 실제 사용 중이라고 판단하지 않는다.

# 저장소 탐색 정상 흐름

분석 목적 확정
↓
Branch·Commit 기록
↓
Root·모듈 확인
↓
실행 모듈 선택
↓
Main·Profile·Port 확인
↓
대표 ServiceId 선택
↓
Handler부터 Mapper까지 추적
↓
YML·XML·DDL·Test 연결
↓
bootRun
↓
표준 요청 실행
↓
GUID·SQL·DB 결과 확인
↓
저장소 지도 작성

# 저장소 탐색 오류 흐름

검색 결과가 여러 개
↓
모듈·Branch·Legacy 여부 구분
↓
Gradle Build 대상 확인
↓
Spring Bean 등록 여부 확인
↓
Dispatcher 등록 로그 확인
↓
Breakpoint로 실제 실행 확인

문서와 코드 불일치
↓
Commit·문서 Version 확인
↓
실행 기준 확인
↓
Gap 등록
↓
기준본 결정

# 정상 예시

분석 목적
고객요약 조회 거래 수정

Branch
develop

Commit
a12bc34

대상 모듈
sv-service

대표 ServiceId
SV.Customer.selectSummary

정방향 추적
SvCustomerHandler
→ SvCustomerFacade
→ SvCustomerService
→ SvCustomerRule
→ SvCustomerDao
→ SvCustomerMapper
→ SvCustomerMapper.xml
→ SV\_CUSTOMER

설정
application-local.yml
Port 8086
H2 Datasource

테스트
정상 고객 조회
없는 고객 업무 오류
미등록 ServiceId

운영 증적
GUID
TraceId
ServiceId
SQL ID

# 금지 예시

모든 파일을 첫 번째 파일부터 순서대로 읽는다.

파일 이름만 보고 책임을 추측한다.

ServiceId를 검색하지 않고 클래스명만 추측한다.

같은 이름의 클래스 중 하나를 임의로 선택한다.

현재 구현과 목표 표준을 같은 것으로 기록한다.

Mapper Interface만 보고 SQL을 확인하지 않는다.

application.yml 값만 보고 실제 실행값으로 단정한다.

build·node\_modules·생성파일까지 모두 검색한다.

문서에 적힌 클래스가 현재도 실행된다고 가정한다.

샘플 Controller를 운영 표준으로 복사한다.

TCF 우회 Endpoint를 정상 거래의 기준으로 삼는다.

정적 검색만 수행하고 실제 거래를 실행하지 않는다.

분석 결과에 Branch와 Commit을 기록하지 않는다.

# 책임 경계와 RACI

| 활동 | 아키텍트 | 프레임워크팀 | 업무개발팀 | DevOps | 운영 |
| --- | --- | --- | --- | --- | --- |
| 모듈 분류 기준 | A | R | C | C | I |
| 플랫폼 지도 | C | R/A | C | C | C |
| 업무 모듈 지도 | C | C | R/A | I | C |
| ServiceId 추적 | C | C | R/A | I | C |
| 설정 지도 | C | C | R | R/A | C |
| SQL·Table 지도 | C | I | R | I | C |
| 운영 식별자 지도 | C | R | R | C | A/C |
| Legacy 판정 | A | R | C | C | C |
| 기준본 결정 | A | R | R | C | C |
| 자동검증 | C | R | C | R/A | I |
| 저장소 문서화 | A | R | R | C | C |

# 자동검증 및 품질 Gate

## 1\. 모듈 Gate

settings.gradle 등록 모듈 목록 생성
실행 모듈 Main Class 존재
업무 WAR 간 직접 Project Dependency 금지
폐기 모듈 Build 대상 제외

## 2\. 패키지 Gate

Handler → Facade 호출
Facade → Service 호출
Rule의 DAO·Client 호출 금지
Handler의 Mapper 호출 금지
플랫폼의 업무 패키지 Import 금지

## 3\. ServiceId Gate

Handler serviceIds() 중복 없음
미등록 ServiceId 없음
OM Catalog와 정합
거래코드·Timeout 정책과 정합

## 4\. Mapper Gate

Interface FQCN과 XML namespace 일치
Method와 SQL ID 일치
Mapper XML Artifact 포함
미사용 SQL ID 검출
Table 소유 업무 검증

## 5\. 설정 Gate

Profile별 필수 Property 존재
운영 Secret 저장소 미포함
Port 중복 없음
개발·운영 Datasource 혼용 없음

## 6\. 추적성 Gate

화면 이벤트
↔ ServiceId
↔ Handler
↔ Mapper SQL
↔ Table
↔ Test

# 테스트 시나리오

| ID | 시나리오 | 기대 결과 |
| --- | --- | --- |
| NAV-001 | 저장소 Root 확인 | Git Root 식별 |
| NAV-002 | Gradle Project 목록 | 전체 모듈 표시 |
| NAV-003 | 라이브러리 모듈 판정 | Main·bootRun 없음 |
| NAV-004 | 실행 모듈 판정 | Main·bootRun 존재 |
| NAV-005 | 업무 WAR 판정 | 업무 Handler·설정 존재 |
| NAV-006 | 문서 디렉터리 분리 | Build 대상에서 제외 |
| NAV-007 | node\_modules 제외 검색 | 검색 잡음 제거 |
| NAV-008 | Main Class 탐색 | 업무 기동점 확인 |
| NAV-009 | Profile 파일 탐색 | local·dev·prod 확인 |
| NAV-010 | 대표 ServiceId 검색 | 담당 Handler 확인 |
| NAV-011 | Handler 중복 | 중복 감지 |
| NAV-012 | Facade Transaction | 경계 확인 |
| NAV-013 | Service·Rule 추적 | 업무 처리 확인 |
| NAV-014 | DAO·Mapper 추적 | SQL ID 확인 |
| NAV-015 | Table 역추적 | 영향 ServiceId 확인 |
| NAV-016 | Mapper Namespace 불일치 | Gate 실패 |
| NAV-017 | Mapper XML 누락 | Package Gate 실패 |
| NAV-018 | 설정값 Override | 실제 기동값 확인 |
| NAV-019 | 문서·소스 불일치 | Gap 등록 |
| NAV-020 | Legacy 코드 중복 | 실제 Build 대상 판정 |
| NAV-021 | TCF 우회 샘플 | 운영 표준에서 제외 |
| NAV-022 | 정상 대표 거래 | 전체 호출 성공 |
| NAV-023 | 미등록 ServiceId | 표준 오류 |
| NAV-024 | SQL 오류 | Mapper 위치 추적 |
| NAV-025 | GUID 검색 | 전체 로그 연결 |
| NAV-026 | 현재·목표 패키지 비교 | 상태 구분 기록 |
| NAV-027 | Commit 변경 후 재분석 | 지도 Version 갱신 |
| NAV-028 | 다른 개발자 재현 | 동일 경로 확인 |
| NAV-029 | 자동 Source Index 생성 | 최신 목록 출력 |
| NAV-030 | 추적성 누락 | 품질 Gate 실패 |

# 따라 하는 실무 절차

## 1단계. 저장소 기준을 기록한다

git status -sb
git rev-parse --show-toplevel
git rev-parse --short HEAD

## 2단계. 모듈 목록을 만든다

gradle projects

분류:

Library
실행 Application
업무 WAR
Script
UI
문서
생성파일

## 3단계. 대상 업무 모듈을 선택한다

예:

sv-service

확인:

build.gradle
Main Class
application.yml
application-local.yml

## 4단계. 대표 거래를 선택한다

SV.Customer.selectSummary

선택 기준:

정상 실행
테스트 가능
표준 계층
로그 추적 가능

## 5단계. ServiceId를 검색한다

grep -R "SV.Customer.selectSummary" \\
\--include="\*.java" \\
\--include="\*.xml" \\
\--include="\*.yml" .

## 6단계. Handler부터 정방향 추적한다

Handler
→ Facade
→ Service
→ Rule
→ DAO
→ Mapper
→ SQL
→ Table

## 7단계. 설정과 리소스를 연결한다

Profile
Port
Datasource
Mapper Location
Schema
초기데이터
Logback

## 8단계. 테스트를 확인한다

정상
업무 오류
미등록 ServiceId
DB 오류
Timeout

## 9단계. 로컬에서 실행한다

gradle :sv-service:bootRun

## 10단계. 대표 거래를 호출한다

POST /sv/online
ServiceId = SV.Customer.selectSummary

## 11단계. 런타임 경로를 확인한다

Controller
→ TCF
→ STF
→ Dispatcher
→ Handler
→ 업무 계층
→ ETF

## 12단계. 저장소 지도를 완성한다

| 영역 | 기록 |
| --- | --- |
| 모듈 |  |
| Main |  |
| Profile |  |
| ServiceId |  |
| Handler |  |
| Facade |  |
| Service |  |
| Rule |  |
| DAO |  |
| Mapper |  |
| Table |  |
| Test |  |
| 로그 |  |
| 상태 |  |

# 완료 체크리스트

## 모듈

| 확인 항목 | 완료 |
| --- | --- |
| 저장소 Root를 확인했다. | □ |
| Branch와 Commit을 기록했다. | □ |
| Gradle 모듈 목록을 확인했다. | □ |
| 실행 모듈과 라이브러리를 구분했다. | □ |
| 업무 WAR와 플랫폼 모듈을 구분했다. | □ |
| 문서·스크립트·생성파일을 구분했다. | □ |
| Legacy 후보 모듈을 식별했다. | □ |
| 업무 WAR 간 의존성을 확인했다. | □ |

## 패키지·설정

| 확인 항목 | 완료 |
| --- | --- |
| Main Class를 찾았다. | □ |
| 현재 패키지 구조를 기록했다. | □ |
| 목표 패키지 표준과 구분했다. | □ |
| local·dev·prod 설정을 찾았다. | □ |
| 실제 Port를 확인했다. | □ |
| Datasource를 확인했다. | □ |
| Mapper Scan을 확인했다. | □ |
| Logback 설정을 확인했다. | □ |
| Secret 노출 여부를 확인했다. | □ |

## 거래 추적

| 확인 항목 | 완료 |
| --- | --- |
| 대표 ServiceId를 선택했다. | □ |
| 담당 Handler를 확인했다. | □ |
| Facade 트랜잭션을 확인했다. | □ |
| Service 처리 흐름을 확인했다. | □ |
| Rule의 책임을 확인했다. | □ |
| DAO·Mapper를 확인했다. | □ |
| SQL ID와 Table을 확인했다. | □ |
| 요청·응답 DTO를 확인했다. | □ |
| 외부 연계 Client를 확인했다. | □ |

## 실행·운영

| 확인 항목 | 완료 |
| --- | --- |
| 대표 거래를 로컬 실행했다. | □ |
| Dispatcher 선택 Handler를 확인했다. | □ |
| GUID·TraceId를 확인했다. | □ |
| Mapper SQL을 확인했다. | □ |
| DB 결과를 확인했다. | □ |
| 업무 오류를 실행했다. | □ |
| 미등록 ServiceId를 실행했다. | □ |
| 저장소 지도를 다른 개발자가 재현했다. | □ |

# 변경·호환성·폐기 관리

## 저장소 지도 변경

다음 변경이 발생하면 지도를 갱신한다.

신규 모듈 추가
모듈명 변경
패키지 이동
ServiceId 추가·폐기
Mapper·Table 변경
Profile 추가
배포 방식 변경

## 패키지 이동

패키지 이동 전 확인:

Java Import
Spring Component Scan
Mapper Scan
XML Namespace
Test Package
로그 Class Name
문서 링크
운영 장애 검색

## 모듈 폐기

settings.gradle 제거
Project Dependency 제거
CI Build 제거
배포 Script 제거
Tomcat 배포 제거
OM Catalog 폐기
운영 로그 보관
문서 상태 변경

## ServiceId 폐기

화면 호출 제거
업무 연계 제거
Handler 등록 제거
OM Catalog 비활성
거래통제·Timeout 폐기
테스트 제거·보관
운영 로그 보존기간 적용

# 시사점

## 핵심 아키텍처 판단

대규모 저장소를 이해하는 가장 빠른 방법은 파일 수를 줄이는 것이 아니라 **읽는 기준을 정하는 것**이다.

모듈
→ 실행점
→ ServiceId
→ Handler
→ 업무 계층
→ SQL
→ 운영로그

이 순서를 사용하면 수백 개의 파일 중 현재 업무와 관계된 파일을 빠르게 좁힐 수 있다.

또한 소스코드의 실제 패키지 구조는 현재 실행 아키텍처를 보여준다.

문서상의 목표 구조
≠ 항상 현재 구현 구조

현재 Build와 Call Stack
\= 현재 실행 구조

현재 구현과 목표 표준의 차이는 오류라고 단정하기보다 Gap으로 식별하고, 영향도·우선순위·전환계획을 근거로 관리해야 한다.

## 주요 위험

| 위험 | 영향 |
| --- | --- |
| 모든 파일 순차 탐색 | 핵심 흐름 상실 |
| 클래스명 추측 | 잘못된 구현 수정 |
| Legacy·신규 혼동 | 사용하지 않는 코드 변경 |
| 문서 맹신 | 현재 실행과 불일치 |
| 문자열 검색만 사용 | Proxy·동적 등록 누락 |
| 설정파일 값만 확인 | 실제 Runtime 값 오판 |
| Mapper XML 미확인 | SQL·테이블 영향 누락 |
| 샘플 거래 복사 | 운영 통제 누락 |
| 현재·목표 구조 혼합 | 잘못된 리팩터링 |
| Commit 미기록 | 분석 재현 불가 |
| 실행 검증 생략 | 정적 추측을 사실로 오인 |

## 우선 보완 과제

1.  저장소 Root에 최신 SOURCE\_INDEX.md를 유지한다.
2.  모듈별 책임·실행·배포 형태를 자동 생성한다.
3.  ServiceId와 Handler 등록 목록을 빌드 시 출력한다.
4.  Handler→Facade→Service→Mapper 추적표를 자동 생성한다.
5.  Mapper SQL과 Table 사용 목록을 자동 추출한다.
6.  현재 구현과 목표 패키지 표준의 Gap을 관리한다.
7.  Legacy·샘플·운영 코드를 명확하게 표시한다.
8.  TCF 우회 Endpoint는 개발 Profile로 제한하거나 제거한다.
9.  대표 ServiceId Smoke Test를 모듈별로 제공한다.
10.  분석 문서에 Branch·Commit ID를 자동 기록한다.
11.  화면·ServiceId·프로그램·SQL 추적성 검사를 CI에 추가한다.
12.  설정 Property의 정의·Override·사용 위치를 검색 가능하게 한다.

## 중장기 발전 방향

수동 폴더 탐색
↓
표준 SOURCE\_INDEX
↓
ServiceId 자동 등록 목록
↓
프로그램·SQL 추적성 자동 생성
↓
변경 영향 자동 분석
↓
소스·설정·OM·테스트 통합 Catalog

# 마무리말

처음 보는 저장소에서 길을 잃지 않으려면 모든 코드를 이해하려고 하지 말아야 한다.

대신 다음 질문에 순서대로 답한다.

어떤 모듈이 실행되는가?

어떤 모듈은 라이브러리인가?

어느 업무 WAR가 이 거래를 소유하는가?

요청의 ServiceId는 무엇인가?

어떤 Handler가 등록되어 있는가?

Facade의 트랜잭션 경계는 어디인가?

Service와 Rule은 무엇을 판단하는가?

어떤 Mapper SQL과 테이블을 사용하는가?

어떤 설정과 Profile이 적용되는가?

어떤 테스트와 로그로 증명할 수 있는가?

제9장에서 가장 중요한 탐색 경로는 다음과 같다.

Repository Root
→ Gradle Module
→ Main Class
→ Profile
→ ServiceId
→ Handler
→ Facade
→ Service
→ Rule
→ DAO
→ Mapper
→ SQL
→ Table
→ Test
→ GUID Log

가장 중요한 원칙은 다음과 같다.

파일을 많이 읽는 것
≠ 저장소를 이해하는 것

대표 거래 한 건을
코드·설정·데이터·로그까지 연결하는 것
\= 저장소를 이해하는 것

다음 장에서는 화면 ID와 화면 이벤트에서 출발하여 ServiceId를 찾고, ServiceId 문자열과 Handler 등록정보를 이용해 전체 업무 프로그램으로 이동하는 방법을 학습한다.
