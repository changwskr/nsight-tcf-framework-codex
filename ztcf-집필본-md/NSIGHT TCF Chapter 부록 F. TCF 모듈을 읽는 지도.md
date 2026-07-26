<!-- source: ztcf-집필본/NSIGHT TCF Chapter 부록 F. TCF 모듈을 읽는 지도.docx -->
<!-- tables: mammoth-html + placeholder table-to-md -->

# 확장 부록 F. TCF 모듈을 읽는 지도

## 도입 전 안내말

NSIGHT TCF 소스를 처음 열면 다음과 같은 어려움을 느끼기 쉽다.

모듈이 너무 많다.

어느 프로젝트부터 읽어야 하는지 모르겠다.

같은 이름의 Service·Handler·Mapper가 반복된다.

공통 Framework와 업무코드의 경계가 보이지 않는다.

Gateway·JWT·OM·Batch가 업무 WAR와 어떻게 연결되는지 모르겠다.

오류가 발생했을 때 어느 Module부터 확인해야 하는지 모르겠다.

이때 가장 좋지 않은 접근은 다음과 같다.

첫 번째 Package부터 모든 Class를 순서대로 읽는다.

모든 설정파일을 먼저 암기한다.

모든 Module의 README를 한 번에 이해하려고 한다.

Service·DAO·Mapper를 각각 따로 읽는다.

소스코드만 보고 실제 실행경로를 확인하지 않는다.

TCF는 Class 목록이 아니라 **거래 실행 흐름**을 중심으로 읽어야 한다.

가장 빠른 방법은 실제 ServiceId 하나를 선택하는 것이다.

예:

SV.Sample.inquiry

그리고 다음 순서로 이동한다.

HTTP 요청

→ Gateway Route

→ OnlineTransactionController

→ TCF.process()

→ STF.preProcess()

→ Timeout Executor

→ TransactionDispatcher

→ SvSampleHandler

→ SvSampleFacade

→ SvSampleService

→ SvSampleRule

→ SvSampleDao

→ SvSampleMapper

→ ETF

→ 표준 응답·거래로그

이후 동일한 경로를 반대로 따라간다.

오류가 발생했을 때

Mapper
→ DAO
→ Service
→ Facade
→ Handler
→ Dispatcher
→ ETF
→ 사용자 응답

이렇게 읽으면 각 Module의 역할과 책임 경계를 빠르게 파악할 수 있다.

# 문서 개요

## 목적

본 부록의 목적은 NSIGHT TCF 전체 소스를 모듈별 책임과 거래 흐름의 관점에서 이해할 수 있도록 읽는 순서와 탐색기준을 제공하는 것이다.

세부 목적은 다음과 같다.

Framework Module과 실행 Module 구분

HTTP 진입부터 SQL까지 거래 흐름 이해

Gateway·JWT·업무 WAR의 인증 책임 구분

TCF Core와 Web Adapter의 책임 구분

업무 Handler·Facade·Service·Rule·DAO·Mapper 구조 이해

외부연계·Cache·Batch·OM의 역할 파악

UI·UJ와 Gateway Relay 구조 이해

환경설정·배포 Module 위치 확인

오류 유형별 우선 탐색 Module 결정

ServiceId 기반 정방향·역방향 소스 추적

모듈 의존성 위반과 순환참조 방지

신규 업무 Module 추가 시 필요한 변경범위 확인

## 적용범위

| 영역 | 대상 Module |
| --- | --- |
| 공통 유틸리티 | tcf-util |
| 거래 엔진 | tcf-core |
| HTTP·Spring Boot Adapter | tcf-web |
| 외부 연계 | tcf-eai |
| Cache | tcf-cache |
| Gateway | tcf-gateway |
| 인증·Token | tcf-jwt |
| 운영관리 | tcf-om |
| 운영 수집 Batch | tcf-batch |
| 개발·운영 UI | tcf-ui, tcf-uj |
| 환경·배포 | tcf-cicd, tcf-scripts |
| 업무 실행 | ic-service, pc-service, sv-service 등 |
| 레거시·전환 | om-service 등 기존 Module |

## 대상 독자

신규 업무 개발자

Framework 개발자

애플리케이션 아키텍트

코드 리뷰어

장애 분석 담당자

DevOps 담당자

WAS 운영자

보안 담당자

QA·테스트 담당자

## 선행조건

Java·Spring Boot 기본 이해

Gradle 멀티모듈 기본 이해

HTTP·JSON 기본 이해

ServiceId 개념 이해

Handler·Facade·Service·DAO·Mapper 기본 이해

Tomcat WAR와 Context 개념 이해

Git에서 Class·문자열을 검색할 수 있는 능력

# 핵심 관점

모듈을 이해하는 가장 빠른 방법은
모듈의 모든 Class를 읽는 것이 아니다.

실제 거래 하나를 선택하고
요청이 지나가는 모듈과 책임 경계를 따라가는 것이다.

# 주요 용어

| 용어 | 의미 |
| --- | --- |
| Library Module | 다른 Module이 의존하는 JAR 형태의 공통 Module |
| Executable Module | main() 또는 WAR로 독립 실행 가능한 Module |
| Business WAR | 업무코드별로 거래를 구현한 실행 Module |
| Entry Point | 요청이 처음 들어오는 Controller·Handler |
| Bootstrap | Application과 WAR 환경을 시작하는 초기화 구성 |
| Dispatch | ServiceId에 맞는 Handler를 찾는 처리 |
| Pipeline | STF·업무처리·ETF가 연결된 실행 흐름 |
| Adapter | 외부 기술형식을 내부 계약으로 변환하는 구성요소 |
| Contract | Module 간 주고받는 Request·Response·Interface |
| Dependency Direction | Module 간 참조가 허용되는 방향 |
| Shared Kernel | 여러 Module이 공유하는 안정된 최소 공통계약 |
| Runtime Path | 실행 중 실제로 호출되는 Class 순서 |
| Control Plane | 정책·Route·Timeout·권한을 관리하는 운영영역 |
| Data Plane | 실제 사용자 거래가 처리되는 실행영역 |
| Observability Plane | 로그·Metric·상태를 수집·조회하는 영역 |

# F.1 원본 모듈 지도

원본 부록은 다음과 같은 기본 지도를 제시한다.

| 영역 | 주요 책임 | 읽을 때 확인할 것 |
| --- | --- | --- |
| 진입·Gateway | 요청 수신, 인증정보와 표준 Header 전달 | Route·Filter·Timeout |
| tcf-core | 거래 실행 흐름과 공통 처리 | 진입점·Interceptor·오류 변환 |
| tcf-common | 여러 Module이 공유하는 안정된 계약 | 업무 Logic 유입 방지 |
| tcf-util | 상태를 갖지 않는 범용 기능 | 의존성 최소화 |
| 업무 WAR | 업무규칙과 거래 구현 | 업무 경계·배포단위 |
| 연계 Adapter | 외부 계약과 내부 Model 변환 | Timeout·Retry·오류 Mapping |
| OM·관측 | 거래등록·로그·Metric·운영통제 | ServiceId·거래코드 연결 |

현재 기준 소스에는 독립적인 tcf-common Module이 존재하지 않는다.

원본의 tcf-common 개념은 현재 다음 Module에 분산돼 있다.

| 원본 개념 | 현재 실제 위치 |
| --- | --- |
| 표준전문·오류·거래 Context | tcf-core |
| Spring·HTTP·DataSource 공통구성 | tcf-web |
| 상태 없는 범용 Utility | tcf-util |
| 외부연계 공통계약 | tcf-eai |
| Cache 공통계약 | tcf-cache |

따라서 현재 소스에서 tcf-common을 찾으려고 해서는 안 된다.

이전 문서에서 다음 이름이 발견되면 실제 구현위치를 다시 확인한다.

common-core
common-web
tcf-common
common-util

현재 기준의 우선 Mapping:

common-core
→ tcf-core

common-web
→ tcf-web

공통 Utility
→ tcf-util

# F.2 전체 모듈 지도

## F.2.1 논리 구조

사용자·WebTopSuite·Browser
│
▼
tcf-ui / tcf-uj
│
▼
tcf-gateway
├─ Route
├─ JWT 검증
├─ Header 보정
└─ Gateway 거래로그
│
▼
┌────────────────────────────┐
│ 업무 WAR │
│ ic·pc·ms·sv·pd·eb·ep·ss·mg │
└────────────────────────────┘
│
├──────────────┐
▼ ▼
tcf-web tcf-eai
│ │
▼ ▼
tcf-core 다른 업무·외부 시스템
│
┌────┴────┐
▼ ▼
tcf-util tcf-cache
│
▼
DB·SQL

운영관리 측면
────────────────────────────────────
tcf-om
├─ Service Catalog
├─ 거래통제
├─ Timeout 정책
├─ 권한·사용자
├─ 거래로그·감사
├─ 배포·Rollback
└─ Dashboard

tcf-batch
├─ AP 상태수집
├─ DB 상태수집
└─ 배포상태 수집

tcf-cicd·tcf-scripts
├─ 환경설정
├─ Build
├─ WAR 배포
├─ Tomcat 설정
└─ Health 검증

## F.2.2 Module 유형

| 유형 | Module | 산출물 |
| --- | --- | --- |
| 최하위 Utility | tcf-util | JAR |
| Framework Core | tcf-core | JAR |
| Framework Web | tcf-web | JAR |
| Framework 기능 | tcf-eai, tcf-cache | JAR |
| 독립 실행 Platform | tcf-gateway, tcf-jwt | WAR·bootRun |
| 운영관리 | tcf-om, tcf-batch | WAR·bootRun |
| UI Relay | tcf-ui, tcf-uj | WAR·bootRun |
| 업무 | \*-service | 업무 WAR |
| 구성관리 | tcf-cicd | 설정·Script |
| 실행 Script | tcf-scripts | Script |
| 레거시 | om-service | 확인 필요 |

# F.3 권장 의존 방향

## F.3.1 기본 방향

tcf-util
↑
tcf-core
↑
tcf-web
↑
업무 WAR

선택 기능:

업무 WAR
├─ tcf-eai
├─ tcf-cache
└─ 기타 승인된 공통 Module

독립 실행 Module:

tcf-gateway

tcf-jwt

tcf-om

tcf-batch

tcf-ui

tcf-uj

이들 Module은 필요한 Framework Library를 의존할 수 있지만 서로의 내부 구현 Class를 직접 참조해서는 안 된다.

## F.3.2 허용·금지 의존

| 출발 | 대상 | 판단 |
| --- | --- | --- |
| tcf-core | tcf-util | 허용 |
| tcf-web | tcf-core | 허용 |
| 업무 WAR | tcf-web | 허용 |
| 업무 WAR | tcf-eai | 허용 |
| 업무 WAR | tcf-cache | 필요 시 허용 |
| 업무 WAR A | 업무 WAR B의 구현 Class | 금지 |
| tcf-core | 특정 업무 WAR | 금지 |
| tcf-util | Spring·업무 WAR | 금지 |
| tcf-core | tcf-om 구현 | 금지 |
| 업무 Rule | 다른 업무 Mapper | 금지 |
| Gateway | 업무 내부 Service Class | 금지 |
| UI | 업무 DB | 금지 |

## F.3.3 의존방향 판단 질문

이 Module이 상위 업무의 존재를 알아야 하는가?

해당 Class가 특정 업무코드에 종속되는가?

공통 Module이 업무 Table을 직접 조회하는가?

하위 Module이 Spring Web에 불필요하게 의존하는가?

한 업무의 변경 때문에 전체 Framework를 다시 배포해야 하는가?

Interface 계약이 아니라 구현 Class를 직접 참조하는가?

# F.4 소스를 읽는 가장 좋은 순서

## F.4.1 처음부터 읽지 말아야 할 순서

tcf-util의 모든 Utility

→ tcf-core 모든 Class

→ tcf-web 모든 Configuration

→ 모든 업무 WAR

→ 모든 OM Service

이 방식은 전체 관계를 이해하기 전에 세부 구현에 빠지게 한다.

## F.4.2 권장 1단계 — 한 업무 ServiceId 선택

초보자는 다음처럼 단순한 조회거래를 먼저 선택한다.

SV.Sample.inquiry

선택 기준:

정상 실행 가능한 거래

외부연계가 복잡하지 않은 거래

Mapper SQL이 단순한 거래

Handler·Facade·Service·Rule·DAO·Mapper가 모두 있는 거래

## F.4.3 권장 2단계 — 요청 Sample 확인

우선 요청 JSON에서 다음을 찾는다.

businessCode

serviceId

transactionCode

userId

branchId

channelId

body

Sample 요청 위치 예:

tcf-ui/src/main/resources/sample-requests/

tcf-uj/src/main/resources/sample-requests/

확인할 질문:

어떤 ServiceId인가?

어느 업무코드인가?

어느 Endpoint로 호출하는가?

Gateway를 경유하는가?

Body에는 어떤 입력이 있는가?

## F.4.4 권장 3단계 — HTTP 진입 확인

파일:

tcf-web/src/main/java/
com/nh/nsight/tcf/web/entry/web/
OnlineTransactionController.java

확인:

POST Mapping 경로

Request Body 형식

businessCode 경로변수

TcfGateway 또는 TCF 호출

표준 Response 반환

Exception Handler

Controller는 업무규칙을 처리하지 않는다.

## F.4.5 권장 4단계 — TCF 엔진 확인

파일:

tcf-core/src/main/java/
com/nh/nsight/tcf/core/support/processor/
TCF.java

먼저 process() 하나만 본다.

확인:

STF 호출시점

Timeout Executor 진입

Dispatcher 호출

ETF 성공·실패 처리

Context 정리

거래로그 종료

## F.4.6 권장 5단계 — STF 확인

파일:

tcf-core/src/main/java/
com/nh/nsight/tcf/core/support/processor/
STF.java

확인:

Header 검증

GUID·TraceId

인증문맥

세션·권한

거래통제

Idempotency

Timeout 정책

TransactionContext 생성

STF의 모든 상세 Service를 처음부터 읽기보다 preProcess()에서 어떤 순서로 호출되는지 먼저 확인한다.

## F.4.7 권장 6단계 — Timeout Executor 확인

파일:

tcf-core/src/main/java/
com/nh/nsight/tcf/core/support/timeout/
OnlineTransactionTimeoutExecutor.java

확인:

Worker Thread Pool

Timeout 값 결정

Context 전달

Future 취소

Timeout Exception 변환

Thread 정리

Timeout이 발생했다고 업무 Thread와 DB SQL이 즉시 종료된다고 가정해서는 안 된다.

## F.4.8 권장 7단계 — Dispatcher 확인

파일:

tcf-core/src/main/java/
com/nh/nsight/tcf/core/support/dispatch/
TransactionDispatcher.java

확인:

Handler Bean 수집

ServiceId 등록

중복 검증

미등록 ServiceId 처리

doHandle 호출

ServiceId 오류가 발생하면 가장 먼저 확인할 Module이다.

## F.4.9 권장 8단계 — 업무 Handler 확인

예:

sv-service/src/main/java/
com/nh/nsight/marketing/sv/entry/handler/
SvSampleHandler.java

확인:

serviceIds()

ServiceId 상수

switch 분기

Facade 호출

미지원 ServiceId 오류

금지 여부도 본다.

Handler가 Mapper를 호출하는가?

Handler가 Transaction을 시작하는가?

Handler에 업무 SQL이 있는가?

Handler에서 외부 Client를 직접 호출하는가?

## F.4.10 권장 9단계 — Facade 확인

예:

sv-service/src/main/java/
com/nh/nsight/marketing/sv/entry/facade/
SvSampleFacade.java

확인:

유스케이스 Method

Transaction Annotation

Service 호출순서

Request→Command 변환

Response 조립 경계

변경거래에서는 Transaction 시작·종료 위치를 가장 중요하게 본다.

## F.4.11 권장 10단계 — Service와 Rule 확인

예:

application/service/SvSampleService.java

application/rule/SvSampleRule.java

Service:

업무 흐름

Rule 호출

DAO·Client 호출순서

오류변환

결과조립

Rule:

필수 업무조건

상태전이

권한 판단

중복 판단

순수 검증 가능성

## F.4.12 권장 11단계 — DAO와 Mapper 확인

예:

persistence/dao/SvSampleDao.java

persistence/mapper/SvSampleMapper.java

resources/.../SvSampleMapper.xml

확인:

Mapper Method

XML Namespace

SQL id

Bind Parameter

Result Mapping

영향 행 수

Table·Index

Query Timeout

## F.4.13 권장 12단계 — ETF 확인

파일:

tcf-core/src/main/java/
com/nh/nsight/tcf/core/support/processor/
ETF.java

확인:

정상 Response

Business Error Response

System Error Response

Timeout Response

오류코드·메시지

감사·Metric

거래종료 기록

## F.4.14 권장 13단계 — 역방향 확인

이제 실패흐름을 따라간다.

Mapper SQLException

→ DAO

→ Service

→ Facade Transaction Rollback

→ TCF Catch

→ ETF.systemError

→ StandardResponse

→ 거래로그

이 과정을 설명할 수 있으면 첫 번째 거래 읽기가 완료된다.

# F.5 tcf-util 읽는 지도

## 역할

tcf-util은 Spring에 의존하지 않는 순수 Java Utility Module이다.

날짜·시간

GUID

Masking

문자열

Map

암호 Hash

HTTP Header·Cookie

Logging Helper

공통 상수

## 핵심 원칙

상태를 보유하지 않는다.

업무코드를 알지 않는다.

DB에 접근하지 않는다.

Spring Bean이 아니어도 동작한다.

다른 Module을 역으로 참조하지 않는다.

## 먼저 볼 위치

com.nh.nsight.tcf.util.id

com.nh.nsight.tcf.util.masking

com.nh.nsight.tcf.util.datetime

com.nh.nsight.tcf.util.logging

com.nh.nsight.tcf.util.meta

## 읽을 때 확인

정말 범용기능인가?

업무규칙이 들어오지 않았는가?

동일 Utility가 다른 Module에 중복돼 있는가?

Thread-safe한가?

민감정보를 반환하거나 기록하지 않는가?

외부 Library 의존이 최소인가?

## 금지 예

CtReservationUtils.validateStatus()

CustomerDatabaseUtils.selectCustomer()

OmPermissionUtils.isAdmin()

업무 의미가 들어가면 업무 Module로 이동해야 한다.

# F.6 tcf-core 읽는 지도

## 역할

tcf-core는 TCF의 거래 엔진이다.

표준전문

TransactionContext

STF·TCF·ETF

ServiceId Dispatch

거래통제

Timeout

인증문맥 검증

Idempotency

거래로그·감사 Interface

공통 오류

## 핵심 처리흐름

TCF.process(request)

├─ STF.preProcess()
│ ├─ Header 검증
│ ├─ GUID·TraceId
│ ├─ 인증·인가
│ ├─ 거래통제
│ ├─ Idempotency
│ └─ Timeout 정책
│
├─ OnlineTransactionTimeoutExecutor
│ └─ TransactionDispatcher
│ └─ TransactionHandler
│
└─ ETF
├─ success
├─ businessFail
├─ timeout
└─ systemError

## 우선순위 파일

1\. support/processor/TCF.java

2\. support/processor/STF.java

3\. support/processor/ETF.java

4\. support/dispatch/TransactionDispatcher.java

5\. support/transaction/TransactionHandler.java

6\. support/message/StandardRequest.java

7\. support/message/StandardResponse.java

8\. support/context/TransactionContext.java

9\. support/timeout/OnlineTransactionTimeoutExecutor.java

10\. support/error/BusinessException.java

## 읽을 때 확인

공통 처리순서가 무엇인가?

업무 WAR가 확장할 Interface는 무엇인가?

어떤 오류를 업무 오류로 판단하는가?

Context는 언제 생성·정리되는가?

Timeout이 어느 Thread에서 수행되는가?

거래로그가 시작·종료되는 시점은 언제인가?

Spring·HTTP 구현이 Core에 유입되지 않았는가?

## 변경 시 영향

tcf-core 변경은 모든 업무 WAR에 영향을 줄 수 있다.

따라서 다음 회귀시험이 필요하다.

전체 업무 WAR Build

표준 Header 검증

ServiceId Dispatch

정상·업무·시스템 오류

Timeout

거래통제

거래로그

비동기 Context 정리

# F.7 tcf-web 읽는 지도

## 역할

tcf-web은 tcf-core를 Spring Boot·HTTP·MyBatis·DataSource 환경에 연결하는 Adapter다.

HTTP Controller

JWT Filter

전역 Exception Handler

Spring Auto Configuration

DataSource 구성

거래로그 JDBC

거래통제 JDBC

Timeout 정책 JDBC

MyBatis Query Timeout

WAR Bootstrap

## 주요 Class

| Class | 역할 |
| --- | --- |
| OnlineTransactionController | 표준 온라인 HTTP 진입 |
| TcfGateway | 비표준 REST·파일 진입을 TCF로 위임 |
| TcfJwtAuthenticationFilter | 업무 WAR 직접진입 JWT 검증 |
| GlobalStandardExceptionHandler | HTTP 오류응답 변환 |
| GuidMdcCleanupFilter | 요청 종료 시 Context 정리 |
| TcfAutoConfiguration | Framework Bean 자동구성 |
| PolicyDrivenQueryTimeoutInterceptor | MyBatis Query Timeout |
| NsightWarBootstrap | WAR Profile·Context 초기화 |

## 읽는 순서

OnlineTransactionController

→ TcfGateway

→ TcfAutoConfiguration

→ JWT Filter

→ Exception Handler

→ DataSource Configuration

→ Query Timeout Interceptor

→ WAR Bootstrap

## 읽을 때 확인

Endpoint가 무엇인가?

업무별 Controller가 필요 없는 이유는 무엇인가?

JWT Filter는 어떤 조건에서 활성화되는가?

Gateway 미경유 직접 요청을 어떻게 막는가?

DataSource가 어떤 Module에 제공되는가?

WAR와 bootRun 설정이 어떻게 달라지는가?

Query Timeout이 ServiceId 정책과 연결되는가?

# F.8 업무 WAR 읽는 지도

## 대상

현재 기준 소스에서 확인되는 대표 업무 WAR:

ic-service

pc-service

ms-service

sv-service

pd-service

eb-service

ep-service

ss-service

mg-service

프로젝트 전체 업무코드 대장에는 추가 업무코드가 존재할 수 있으나, 실제 실행 Module 존재 여부는 현재 Branch의 설정과 Build 목록으로 확인한다.

## 공통 구조

com.nh.nsight.marketing.{bc}
├─ Nsight{Bc}ServiceApplication
├─ entry
│ ├─ handler
│ └─ facade
├─ application
│ ├─ service
│ └─ rule
├─ persistence
│ ├─ dao
│ └─ mapper
├─ client
├─ config
└─ support

## 공통 실행 흐름

{Bc}{Domain}Handler

→ {Bc}{Domain}Facade

→ {Bc}{Domain}Service

├─ {Bc}{Domain}Rule
├─ {Bc}{Target}Client
└─ {Bc}{Domain}Dao
└─ {Bc}{Domain}Mapper

## 먼저 읽을 파일

README.md

Nsight{Bc}ServiceApplication.java

entry/handler/\*Handler.java

entry/facade/\*Facade.java

application/service/\*Service.java

application/rule/\*Rule.java

persistence/dao/\*Dao.java

persistence/mapper/\*Mapper.java

resources/\*\*/\*Mapper.xml

application-local.yml

## 업무 WAR에서 확인할 것

업무코드와 Package가 일치하는가?

WAR와 Context가 일치하는가?

Handler의 ServiceId가 중복되지 않는가?

Facade에 Transaction이 있는가?

Rule에 DB 호출이 없는가?

DAO가 업무판단을 하지 않는가?

Mapper SQL이 다른 업무 Table을 직접 변경하지 않는가?

업무 간 호출이 Client 계약을 사용하는가?

오류코드 Prefix가 업무코드와 일치하는가?

로그와 Metric에 업무코드가 연결되는가?

# F.9 tcf-eai 읽는 지도

## 역할

tcf-eai는 업무 WAR 간 또는 외부 시스템 호출의 공통 Client 계약을 제공한다.

현재 주요 구성:

TcfServiceClient

DefaultTcfServiceClient

IntegrationCallRequest

IntegrationCallResult

StandardRequestBuilder

HeaderPropagationHelper

ResponseResultValidator

IntegrationException

IntegrationBusinessException

IntegrationTimeoutException

## 처리 흐름

업무 Service

→ 도메인 Client Interface

→ tcf-eai TcfServiceClient

→ StandardRequest 생성

→ GUID·TraceId·Header 전파

→ 대상 /online 호출

→ 표준 Response 검증

→ 업무·시스템·Timeout 오류 변환

## 읽는 순서

TcfServiceClient

→ DefaultTcfServiceClient

→ IntegrationCallRequest

→ StandardRequestBuilder

→ HeaderPropagationHelper

→ ResponseResultValidator

→ Exception 계층

## 확인할 것

Connect·Read Timeout이 분리되는가?

상위 거래 Timeout 잔여시간을 고려하는가?

Authorization·GUID·TraceId가 전파되는가?

상대 Business Error와 System Error를 구분하는가?

변경거래 Retry가 멱등성을 보장하는가?

상대 Request·Response DTO가 내부 Domain과 분리되는가?

## 금지

Service에서 RestClient 직접 생성

다른 업무 URL Hard Coding

상대 오류응답을 사용자에게 그대로 전달

외부 전문 전체 로그

변경거래 무조건 Retry

# F.10 tcf-cache 읽는 지도

## 역할

tcf-cache는 Spring Cache와 Ehcache 기반 공통 Cache 환경을 제공한다.

대표 Cache:

commonCode

serviceCatalog

sessionRegion

## 주요 Class

TcfCacheAutoConfiguration

TcfCacheProperties

TcfCacheSupport

TcfCacheNames

## 읽는 순서

ehcache.xml

→ TcfCacheNames

→ TcfCacheProperties

→ AutoConfiguration

→ TcfCacheSupport

→ 실제 @Cacheable 사용처

## 확인할 것

Cache가 원본 데이터인가?

TTL은 업무 변경주기와 맞는가?

원본 변경 시 Evict되는가?

다중 WAR의 Local Cache가 서로 다른 값을 가질 수 있는가?

Cache 장애 시 DB Fallback이 가능한가?

개인정보가 Key·Value에 포함되는가?

운영에서 Cache 상태와 Evict를 감사하는가?

## 다중 WAR 주의

Ehcache가 각 JVM·WAR에 Local로 존재하면 다음 문제가 발생할 수 있다.

AP01은 신규 Service Catalog

AP02는 구 Service Catalog

OM에서 정책 변경

→ 일부 Instance만 반영

따라서 다음이 필요하다.

Cache Version

전체 Instance Evict

TTL

정책 변경 Event

운영 확인

# F.11 tcf-gateway 읽는 지도

## 역할

tcf-gateway는 단일 진입점에서 업무코드별 Route를 결정하고 JWT를 검증한 뒤 Downstream 업무 WAR로 전달한다.

## 처리 흐름

업무 Proxy Controller

→ BusinessRouteService

→ GRF.forwardOnline()

→ GSF.preProcess()
├─ Route 조회
├─ JWT 검증
├─ Header 보정
└─ RouteContext 생성

→ GatewayRouteDispatcher

→ Downstream /{code}/online

→ GEF
├─ success
├─ authFail
├─ routeNotFound
├─ httpError
└─ connectionError

→ Gateway 거래로그

## 먼저 볼 Class

entry/web/AbstractBusinessProxyController

entry/facade/BusinessRouteService

support/GRF.java

support/GSF.java

support/GEF.java

application/service/GatewayRouteResolver

application/service/GatewayAuthenticationService

application/service/GatewayJwtValidator

client/GatewayRouteDispatcher

application/service/GatewayTransactionLogRecorder

## 읽을 때 확인

Route의 유일한 기준은 무엇인가?

업무코드가 어느 Target URL로 연결되는가?

JWT 검증은 어느 Class가 담당하는가?

Header userId·branchId는 어떻게 보정되는가?

Authorization Header가 Downstream에 전달되는가?

Gateway Timeout과 업무 Timeout의 관계는 무엇인가?

Route 미등록·401·Downstream 5xx를 어떻게 구분하는가?

Gateway 거래로그와 업무 거래로그를 어떻게 연결하는가?

## 보안 확인

JWT 발급을 Gateway가 하지 않는가?

서명·issuer·audience를 검증하는가?

Header 사용자 위조를 차단하는가?

Gateway 우회가 Network에서 차단되는가?

업무 WAR에서도 2차 검증이 가능한가?

폐기 Token 차단정책이 실제 적용됐는가?

현재 Gateway 문서상 Denylist 연동은 별도 보완대상으로 관리해야 한다.

# F.12 tcf-jwt 읽는 지도

## 역할

tcf-jwt는 RS256 JWT Access·Refresh Token을 발급·갱신·폐기하고 JWKS를 제공한다.

## 역할 분리

| 역할 | 담당 |
| --- | --- |
| Token 발급 | tcf-jwt |
| Refresh | tcf-jwt |
| Revoke·Logout | tcf-jwt |
| JWKS 제공 | tcf-jwt |
| Gateway 검증 | tcf-gateway |
| 업무 WAR 검증 | tcf-web JWT Filter |
| SSO 인증완료 | IdP·tcf-om |

## 주요 ServiceId

JWT.Auth.login

JWT.Auth.ssoIssue

JWT.Auth.refresh

JWT.Auth.revoke

JWT.Auth.logout

## 먼저 볼 Class

JwtAuthLoginHandler

JwtAuthRefreshHandler

JwtAuthSsoIssueHandler

JwtAuthRevokeHandler

JwtAuthFacade

JwtAuthService

JwtTokenIssuer

JwtTokenStore

JwtKeyConfiguration

JwtSecurityConfiguration

JwkSetController

## 읽는 순서

Handler

→ Facade

→ Service

→ JwtTokenIssuer

→ Token Store

→ Mapper·Table

→ JWKS Controller

→ Gateway Validator

## 확인할 것

Private Key는 어디서 주입되는가?

Public Key·kid는 어떻게 제공되는가?

Access·Refresh 수명은 얼마인가?

Refresh Rotation이 적용되는가?

Token 원문을 DB에 저장하는가?

jti·Denylist는 어떻게 관리되는가?

발급 Claim과 StandardHeader가 일치하는가?

Key Rotation 시 구·신 Key가 공존하는가?

로그에 Token이 남지 않는가?

# F.13 tcf-om 읽는 지도

## 역할

tcf-om은 운영 Control Plane이다.

주요 기능:

사용자·권한

메뉴

Service Catalog

거래통제

Timeout 정책

오류코드

거래로그

감사로그

공통코드

Cache 관리

배포·Rollback

Health·Dashboard

Batch 관리

파일 업·다운로드

시스템 설정

## 구조

Om{Domain}Handler

→ Om{Domain}Facade

→ Om{Domain}Service

→ OmOperationDao·Mapper

## 먼저 볼 도메인

초보자는 모든 OM 기능을 한 번에 읽지 않는다.

다음 순서를 권장한다.

1\. OmServiceCatalogHandler
2\. OmTransactionControlHandler
3\. OmTimeoutPolicyHandler
4\. OmTransactionLogHandler
5\. OmAuditLogHandler
6\. OmFunctionAuthHandler
7\. OmDashboardHandler
8\. OmDeployHandler
9\. OmCacheHandler

## 운영 정책 연결

OM Service Catalog
→ Dispatcher 대상 ServiceId의 운영정보

OM 거래통제
→ STF 허용·차단 판단

OM Timeout 정책
→ TCF·Transaction·SQL Timeout

OM 기능권한
→ STF Authorization

OM 오류코드
→ 사용자 메시지·운영 대응

OM 거래로그
→ GUID·ServiceId 검색

OM 배포관리
→ Artifact·승인·Rollback

## 읽을 때 확인

OM이 업무 거래의 Runtime 필수 Dependency인가?

OM 장애 시 업무 거래는 어떻게 되는가?

정책 Cache는 어디에 존재하는가?

변경 후 전체 Instance에 어떻게 반영되는가?

운영 변경에 감사로그가 남는가?

관리자 기능과 일반 업무 기능이 분리되는가?

ServiceId Seed와 실제 Handler가 일치하는가?

## 주의

tcf-om과 om-service가 함께 존재할 경우 다음을 먼저 확인한다.

현재 운영표준은 어느 Module인가?

동일 ServiceId가 양쪽에 존재하는가?

DB·Context·Route가 중복되는가?

구 Module의 폐기계획이 있는가?

현재 신규 기준은 tcf-om을 우선하고 om-service는 레거시·전환대상으로 검토한다.

# F.14 tcf-batch 읽는 지도

## 역할

현재 tcf-batch는 OM Dashboard용 AP·DB·배포상태를 수집한다.

대표 Job:

BAT-BATCH-001
AP 상태 수집

BAT-BATCH-002
DB 상태 수집

BAT-BATCH-004
배포 상태 수집

## 처리 흐름

Scheduler 또는 수동 Controller

→ Collect Service

→ Metrics Client

→ 대상 Actuator

→ 상태 변환

→ OM 상태 Repository

→ Dashboard

## 먼저 볼 Class

NsightTcfBatchApplication

ApStatusCollectScheduler

ApStatusCollectService

ApMetricsClient

OmDashboardStatusRepository

BatchDatabaseMigration

수동 실행 Controller

## 확인할 것

수집주기는 얼마인가?

수집 대상 Module 목록은 어디에 있는가?

Actuator 접근권한은 안전한가?

수집 실패와 대상 장애를 구분하는가?

Scheduler 중복 실행을 막는가?

DB 적재 실패 시 재처리가 가능한가?

OM Dashboard의 마지막 수집시각이 표시되는가?

수집 자체가 업무 시스템에 부하를 주지 않는가?

# F.15 tcf-ui와 tcf-uj 읽는 지도

## 역할

tcf-ui와 tcf-uj는 업무 Test 화면과 OM 관리화면을 제공하고 요청을 Gateway로 Relay한다.

주요 흐름:

Browser

→ TcfApiController

→ TransactionRelayService

→ GatewayRelayService

→ tcf-gateway

→ 업무 WAR

파일 업·다운로드 예외 흐름:

Browser

→ UpdownloadApiController

→ UpdownloadRelayService

→ tcf-om

## 먼저 볼 Class

TcfApiController

TransactionRelayService

GatewayRelayService

BusinessModuleCatalog

BusinessTransactionCatalog

BusinessModuleDefinitions

UpdownloadApiController

## 확인할 것

업무 Module 목록이 실제 Module과 일치하는가?

bootRun·Tomcat Gateway URL이 어떻게 다른가?

Authorization Header가 전달되는가?

UI가 Downstream URL을 임의로 결정할 수 있는가?

파일 연계만 Gateway를 우회하는 이유는 무엇인가?

Sample Request와 실제 ServiceId가 일치하는가?

tcf-ui와 tcf-uj의 책임이 중복되는가?

## 중요 Gap

UI Module의 업무코드 Catalog와 실제 소스 Module 목록이 다를 수 있다.

예:

Catalog에는 CT 존재

하지만 ct-service Module은 없음

따라서 UI에 메뉴가 보인다는 사실만으로 업무 WAR가 구현됐다고 판단하지 않는다.

# F.16 tcf-cicd와 tcf-scripts 읽는 지도

## tcf-cicd 역할

local·dev·prod 환경설정 Source of Truth

Spring Profile 설정

Tomcat setenv

Apache Route

Module Manifest

Build·Deploy Script

운영 Artifact 생성

## 주요 파일

manifest.yaml

local/spring/

dev/spring/

prod/spring/

prod/apache/

scripts/cicd-deploy.\*

scripts/sync-to-framework.\*

scripts/apply-tomcat-config.\*

## tcf-scripts 역할

로컬 bootRun

선택 Module 실행

WAR Build

Tomcat webapps 배포

Sample curl 호출

## 읽을 때 확인

설정의 최종 Source of Truth는 어디인가?

Module이 Manifest에 등록됐는가?

환경별 Profile이 모두 존재하는가?

WAR명과 Context가 일치하는가?

운영 Secret이 Source에 포함됐는가?

Build Once·Deploy Many를 지키는가?

Health Check가 업무 Smoke까지 포함하는가?

Rollback Artifact가 보존되는가?

# F.17 실제 거래 하나를 읽는 예시

## 대상

ServiceId
SV.Sample.inquiry

업무코드
SV

Endpoint
POST /sv/online

## 정방향 지도

1\. Sample Request
tcf-ui/.../sv-sample-inquiry.json

2\. Gateway
SvProxyController
BusinessRouteService
GSF
GatewayRouteResolver
GatewayJwtValidator
GatewayRouteDispatcher

3\. HTTP Adapter
OnlineTransactionController

4\. TCF Engine
TCF.process

5\. STF
Header·GUID·권한·거래통제·Timeout

6\. Timeout
OnlineTransactionTimeoutExecutor

7\. Dispatch
TransactionDispatcher

8\. 업무 Handler
SvSampleHandler

9\. Facade
SvSampleFacade

10\. Service·Rule
SvSampleService
SvSampleRule

11\. Persistence
SvSampleDao
SvSampleMapper
Mapper XML

12\. ETF
StandardResponse

13\. 운영
거래로그·Metric·OM

## 각 단계의 핵심 질문

| 단계 | 질문 |
| --- | --- |
| Request | ServiceId와 Header가 올바른가? |
| Gateway | 어느 Target URL로 Route되는가? |
| JWT | Token과 Header 사용자가 일치하는가? |
| Controller | TCF로 정상 위임되는가? |
| STF | 어떤 통제가 실행되는가? |
| Timeout | 제한시간은 어디서 결정되는가? |
| Dispatcher | Handler가 등록됐는가? |
| Handler | 올바른 Facade로 분기되는가? |
| Facade | Transaction 경계가 적합한가? |
| Service | 업무 흐름이 명확한가? |
| Rule | 업무 판단이 분리됐는가? |
| Mapper | SQL·DB 결과가 올바른가? |
| ETF | 오류가 표준응답으로 변환되는가? |
| OM | GUID로 전체 흐름을 찾을 수 있는가? |

# F.18 변경거래를 읽는 순서

조회거래를 이해한 뒤 등록·수정·취소를 읽는다.

## 등록

Handler

→ Facade @Transactional

→ 입력·고객·코드 Rule

→ 중복조회

→ Master INSERT

→ History INSERT

→ 영향 행 확인

→ Commit

→ Idempotency SUCCESS

## 수정

현재 상태조회

→ 권한·상태·Version Rule

→ Version 조건 UPDATE

→ History

→ 영향 행 1건

→ Version 증가

## 취소

현재 상태조회

→ 취소가능 상태

→ 논리상태 CANCELED

→ Active Key 해제

→ History

→ Commit

## 변경거래에서 추가로 볼 것

Transaction 시작위치

Rollback 대상 Exception

DB Unique

Version 조건

영향 행 수

Idempotency 상태

Timeout UNKNOWN

Master·History 정합성

감사로그

# F.19 오류 유형별 소스 탐색 지도

## 401 인증 오류

tcf-gateway
GatewayAuthenticationService
GatewayJwtValidator

또는

tcf-web
TcfJwtAuthenticationFilter

또는

tcf-jwt
JWKS·Token 발급

확인:

Authorization Header

issuer·audience

만료

kid·Public Key

시스템 시각

Gateway 우회

## 403 권한 오류

STF

AuthorizationValidator

OM 기능권한

업무 Rule

SQL 데이터 Scope

## 404 Route 오류

Gateway Proxy Controller

GatewayRouteResolver

TCF\_GATEWAY\_ROUTE

업무코드

Target URL

Context Path

## ServiceId 미등록

TransactionDispatcher

업무 Handler.serviceIds()

Spring Component Scan

OM Service Catalog

## Handler는 있는데 실행되지 않음

ServiceId 대소문자

Handler Bean 등록

Package Scan

중복 ServiceId

Gateway가 전달한 Body Header

## Mapper 오류

DAO Method

Mapper Interface

XML Namespace

SQL id

WAR 내부 Mapper XML

MyBatis Scan

## DB Connection 오류

tcf-web DataSource Configuration

업무 application-{profile}.yml

Hikari 설정

DB Network

Schema·계정

Tomcat 외부 설정

## Timeout

OM Timeout Policy

STF Timeout Resolver

OnlineTransactionTimeoutExecutor

Transaction Timeout

Query Timeout Interceptor

tcf-eai Connect·Read Timeout

DB Pool Pending

## 응답은 실패했는데 DB에 데이터 존재

Facade Transaction

Idempotency

Master

History

Transaction Log

Timeout Executor

ETF 응답전송

UNKNOWN 결과조회

## 한 인스턴스만 오류

Artifact Checksum

Profile

Environment Variable

Cache

Route

DataSource

JVM·Thread·GC

WAR Exploded Directory

# F.20 정상 처리 흐름

1\. UI가 표준 Request를 생성한다.
2\. Gateway가 Route와 JWT를 검증한다.
3\. Gateway가 인증 Header를 보정한다.
4\. 업무 WAR의 OnlineTransactionController가 Request를 수신한다.
5\. TCF가 TransactionContext를 생성한다.
6\. STF가 Header·권한·거래통제·Timeout을 검증한다.
7\. Timeout Executor가 업무 실행을 감싼다.
8\. Dispatcher가 ServiceId에 맞는 Handler를 찾는다.
9\. Handler가 Facade에 위임한다.
10\. Facade가 Transaction을 시작한다.
11\. Service와 Rule이 업무흐름을 처리한다.
12\. DAO·Mapper가 DB를 처리한다.
13\. Facade Transaction이 Commit된다.
14\. ETF가 표준 성공응답을 생성한다.
15\. 거래로그와 Metric이 종료된다.
16\. Gateway가 Client에 응답한다.

# F.21 오류·Timeout·장애 흐름

## 업무 오류

Rule

→ BusinessException

→ Facade Rollback

→ TCF Catch

→ ETF.businessFail

→ 거래로그 BUSINESS\_ERROR

## 시스템 오류

Mapper SQLException

→ SystemException 또는 Runtime Exception

→ Transaction Rollback

→ ETF.systemError

→ 안전한 사용자 응답

→ ERROR 로그·Alert

## Timeout

OnlineTransactionTimeoutExecutor

→ 제한시간 초과

→ Future 취소

→ TimeoutExceptionResolver

→ ETF.timeout

→ DB·Thread 실제 종료 확인

→ 필요 시 UNKNOWN 대사

## Gateway 장애

Route 조회 실패

또는 JWT 실패

또는 Downstream 연결 실패

→ GEF 오류변환

→ Gateway 거래로그

→ 업무 WAR 미진입 또는 부분 진입 여부 확인

# F.22 정상 예시

대상
SV.Sample.inquiry

1\. sample JSON에서 ServiceId 확인
2\. Gateway Route에서 SV Target 확인
3\. OnlineTransactionController 확인
4\. TCF.process 흐름 확인
5\. STF 처리순서 확인
6\. Dispatcher에서 Handler 등록 확인
7\. SvSampleHandler의 serviceIds 확인
8\. Facade·Service·Rule 책임 확인
9\. DAO·Mapper·SQL 확인
10\. ETF 응답과 거래로그 확인
11\. GUID로 Gateway와 SV 로그 연결
12\. 실제 curl 호출로 Breakpoint 검증

판정:

거래 흐름을 정방향으로 설명할 수 있다.

오류 흐름을 역방향으로 설명할 수 있다.

각 Module의 책임을 구분할 수 있다.

변경할 Class와 변경하면 안 되는 Class를 구분할 수 있다.

# F.23 금지 예시

모든 Class를 가나다순으로 읽는다.

README 내용만 보고 실제 소스를 확인하지 않는다.

tcf-common이 현재 존재한다고 가정한다.

tcf-core에 특정 업무규칙을 추가한다.

tcf-util에 DB·Spring·업무 Logic을 넣는다.

업무 Handler에서 Mapper를 직접 호출한다.

Rule에서 다른 업무 API를 호출한다.

업무 WAR가 다른 업무 Mapper를 참조한다.

Gateway가 업무 DB를 직접 조회한다.

tcf-jwt와 Gateway의 발급·검증 역할을 혼동한다.

OM 장애 때문에 모든 업무 거래가 즉시 실패하도록 결합한다.

UI Catalog에 업무코드가 있다는 이유로 업무 WAR가 존재한다고 판단한다.

Health UP만 보고 ServiceId가 정상이라고 판단한다.

Timeout 오류를 Timeout Class 하나만 보고 분석한다.

Mapper 오류를 SQL만 보고 Namespace·Resource를 확인하지 않는다.

공통 Module 변경 후 한 업무 WAR만 시험한다.

레거시 om-service와 tcf-om을 동시에 신규 표준으로 사용한다.

운영 오류가 발생하자 모든 Module의 로그를 무작정 읽는다.

# F.24 모듈별 Source of Truth

| 정보 | 우선 확인 위치 |
| --- | --- |
| 실제 Module 목록 | settings.gradle·현재 Build 구성 |
| Module 의존성 | 각 build.gradle |
| 업무코드·Port | Module README·Profile |
| ServiceId | Handler serviceIds() |
| HTTP Endpoint | OnlineTransactionController·Proxy Controller |
| Gateway Route | TCF\_GATEWAY\_ROUTE·Gateway 설정 |
| Timeout | OM 정책·Timeout Resolver |
| 권한 | OM 권한·STF |
| SQL | Mapper XML |
| DB 객체 | DDL·Mapper SQL |
| Cache | ehcache.xml·Cache Names |
| 배포 | tcf-cicd/manifest.yaml |
| 환경설정 | tcf-cicd/{profile} |
| 운영상태 | OM·Actuator·거래로그 |
| Artifact | CI·Repository·Checksum |

문서와 소스가 다르면 현재 실행환경·현재 Branch·운영설정을 기준으로 차이를 기록하고 문서를 갱신한다.

# F.25 책임 경계와 RACI

| 영역 | Framework | 업무개발 | AA | DevOps | 운영 | 보안 | DBA |
| --- | --- | --- | --- | --- | --- | --- | --- |
| tcf-util | A/R | C | C | I | I | C | I |
| tcf-core | A/R | C | A/C | I | C | C | I |
| tcf-web | A/R | C | C | C | C | C | C |
| 업무 WAR | C | A/R | C | C | C | C | C |
| tcf-eai | A/R | C | C | I | C | C | I |
| tcf-cache | A/R | C | C | C | A/C | C | I |
| Gateway | R | C | A/C | C | C | A/C | C |
| JWT | R | C | C | C | C | A | C |
| OM | C | C | A/C | C | A/R | C | C |
| Batch | C | C | C | C | A/R | C | C |
| UI·UJ | C | R | C | C | C | C | I |
| CI/CD | C | C | C | A/R | C | C | I |
| DB Mapper | C | R | C | I | C | I | A/C |
| Module 폐기 | C | C | A | R/C | R/C | C | C |

# F.26 성능·용량·확장성 관점

## tcf-core

모든 거래가 통과

→ Logging·Validation·Timeout 비용이 전체 TPS에 영향

공통 변경은 거래당 비용으로 측정한다.

## tcf-web

Tomcat Thread

Hikari Pool

MyBatis Timeout

HTTP Body

MDC Filter

가 업무 WAR별로 어떻게 구성되는지 확인한다.

## Gateway

모든 외부 거래 집중

Route DB

JWT 검증

Connection Pool

Downstream Timeout

이 병목이 될 수 있다.

## Cache

Local Cache 불일치

Cache Stampede

대형 Value

TTL 동시 만료

를 검토한다.

## OM·Batch

관측을 위한 수집이 업무 시스템에 과도한 부하를 주어서는 안 된다.

수집주기

Actuator 호출 수

대상 Instance 수

DB 적재량

보존기간

을 산정한다.

# F.27 보안·개인정보·감사

## Module별 보안책임

| Module | 주요 책임 |
| --- | --- |
| Gateway | 외부 JWT 검증·Route 통제 |
| tcf-web | 직접진입 2차 JWT 검증 |
| STF | Header 정합·기능권한 |
| 업무 WAR | 데이터권한·업무권한 |
| JWT | Key·Token 발급·폐기 |
| OM | 사용자·권한·정책·감사 |
| EAI | 인증 Header·개인정보 최소전달 |
| UI·UJ | Token 안전 Relay·XSS |
| CI/CD | Secret·Artifact 무결성 |

## 읽을 때 확인

Private Key가 어디에 존재하는가?

Token 원문이 로그에 남는가?

Gateway Header를 업무 WAR가 무조건 신뢰하는가?

관리 Endpoint가 외부에 노출되는가?

OM 변경행위가 감사되는가?

업무 간 개인정보 전달이 최소화됐는가?

Cache와 Batch에 개인정보가 축적되는가?

# F.28 운영·모니터링·장애 대응

## 장애 시 Module 선택표

| 증상 | 첫 확인 Module |
| --- | --- |
| 모든 업무 401 | Gateway·JWT |
| 특정 업무만 401 | 업무 WAR JWT Filter·Profile |
| 특정 ServiceId 404 | Gateway Route |
| ServiceId 미등록 | Core Dispatcher·Handler |
| DB Pool 대기 | 업무 WAR·tcf-web |
| 모든 업무 Timeout | Gateway·Core Thread·DB |
| 특정 SQL 지연 | 업무 Mapper·DB |
| OM 화면만 장애 | tcf-om·tcf-ui |
| Dashboard 갱신중지 | tcf-batch |
| Token 발급 실패 | tcf-jwt |
| 업무 간 호출 실패 | tcf-eai·대상 WAR |
| Cache 불일치 | tcf-cache·OM |
| 배포 후 한 서버만 오류 | CI/CD·Artifact·Profile |
| 파일 기능만 오류 | UI Relay·tcf-om |

## 증거 보존

대표 GUID

TraceId

ServiceId

Gateway Route

Artifact Version

Instance ID

Profile

Thread·Pool 상태

SQL ID

최근 배포·설정 변경

# F.29 자동검증 및 품질 Gate

## Architecture Test

tcf-util은 Spring에 의존하지 않는다.

tcf-core는 업무 Package를 참조하지 않는다.

Handler는 Facade만 호출한다.

Rule은 DAO·Mapper를 호출하지 않는다.

업무 WAR 간 구현 직접참조를 금지한다.

Gateway는 업무 Service Class를 참조하지 않는다.

UI는 업무 DB에 접근하지 않는다.

## ServiceId Gate

Handler ServiceId 중복 없음

OM Catalog 등록

Gateway Route 업무코드 등록

Timeout 정책

기능권한

오류코드

## Module Gate

settings.gradle 등록

build.gradle 의존성

WAR명

Context

Port

Profile

tcf-cicd Manifest

Health

Smoke

## 보안 Gate

Gateway JWT

업무 WAR 2차 검증

Secret Scan

Token 로그 금지

관리 Endpoint 접근제한

Module 간 개인정보 전달검토

# F.30 테스트 시나리오

| ID | 시나리오 | 기대 결과 |
| --- | --- | --- |
| MOD-001 | SV.Sample.inquiry 정방향 추적 | 전체 Class 연결 |
| MOD-002 | Mapper에서 요구사항 역추적 | 화면·ServiceId 확인 |
| MOD-003 | 미등록 ServiceId | Dispatcher 오류 |
| MOD-004 | 중복 ServiceId | 기동·Gate 실패 |
| MOD-005 | Handler Bean 누락 | Service 미등록 |
| MOD-006 | Handler→Mapper 의존 | Architecture 실패 |
| MOD-007 | Rule→DAO 의존 | Architecture 실패 |
| MOD-008 | Core→업무 WAR 의존 | Build Gate 실패 |
| MOD-009 | 업무 A→업무 B Mapper | Gate 실패 |
| MOD-010 | Gateway Route 누락 | 404 |
| MOD-011 | JWT JWKS 실패 | 401·Alert |
| MOD-012 | Header 사용자 위조 | 차단 |
| MOD-013 | Mapper XML 누락 | 통합 Test 실패 |
| MOD-014 | DB Profile 누락 | 기동 실패 |
| MOD-015 | Query Timeout | 정책 적용 |
| MOD-016 | 업무 간 EAI Timeout | 표준 오류 |
| MOD-017 | Cache Evict | 전체 Instance 반영 |
| MOD-018 | OM 정책 변경 | STF 반영 |
| MOD-019 | Batch 수집대상 장애 | 상태 오류 기록 |
| MOD-020 | UI Catalog만 CT 등록 | WAR 존재검사 실패 |
| MOD-021 | tcf-core 변경 | 전체 WAR 회귀 |
| MOD-022 | tcf-web 변경 | HTTP·DB 회귀 |
| MOD-023 | Gateway 변경 | 전 업무 Route 회귀 |
| MOD-024 | JWT Key Rotation | 구·신 Token 검증 |
| MOD-025 | tcf-common 검색 | 현재 Mapping 안내 |
| MOD-026 | om-service·tcf-om 중복 | 전환 Gate |
| MOD-027 | 신규 ct-service 편입 | 전체 설정 연결 |
| MOD-028 | 한 Instance 설정 Drift | Artifact·Profile 탐지 |
| MOD-029 | 거래 Timeout UNKNOWN | 데이터 대사 |
| MOD-030 | 실제 Breakpoint 추적 | Runtime 경로 일치 |

# F.31 신규 업무 Module 추가 지도

예: ct-service

## 필수 작업

1\. 공식 업무코드 확인

2\. settings.gradle Module 등록

3\. ct-service 디렉터리 생성

4\. build.gradle 의존성
tcf-util
tcf-core
tcf-web
필요 시 tcf-eai·tcf-cache

5\. NsightCtServiceApplication

6\. Handler·Facade·Service·Rule·DAO·Mapper

7\. application-local·dev·prod

8\. bootRun Port

9\. WAR ct.war

10\. Context /ct

11\. Gateway Proxy·Route

12\. UI·UJ Catalog

13\. OM Service Catalog

14\. 권한·Timeout·오류코드

15\. tcf-cicd Manifest

16\. Build·Deploy Script

17\. Health·Smoke

18\. Rollback

## 완료 판단

Module이 Build된다.

bootRun이 기동된다.

ServiceId가 Dispatch된다.

Mapper SQL이 실행된다.

Gateway를 경유한다.

OM에서 검색된다.

WAR가 Tomcat에 배포된다.

Health와 Smoke가 통과한다.

Rollback이 가능하다.

# F.32 완료 체크리스트

## 전체 구조

| 점검 | 완료 |
| --- | --- |
| Library와 실행 Module을 구분한다. | □ |
| 전체 요청 흐름을 설명할 수 있다. | □ |
| Data Plane과 Control Plane을 구분한다. | □ |
| Module 의존방향을 설명할 수 있다. | □ |
| tcf-common의 현재 Mapping을 이해한다. | □ |

## Core·Web

| 점검 | 완료 |
| --- | --- |
| TCF.process()를 찾을 수 있다. | □ |
| STF·ETF 책임을 설명할 수 있다. | □ |
| Dispatcher에서 Handler 등록을 찾을 수 있다. | □ |
| Timeout Executor를 설명할 수 있다. | □ |
| OnlineTransactionController를 찾을 수 있다. | □ |
| JWT Filter·Exception Handler 위치를 안다. | □ |

## 업무 WAR

| 점검 | 완료 |
| --- | --- |
| Handler부터 Mapper까지 추적할 수 있다. | □ |
| Facade Transaction을 확인할 수 있다. | □ |
| Service와 Rule을 구분한다. | □ |
| Mapper XML과 DB 객체를 찾을 수 있다. | □ |
| 다른 업무와의 연계를 찾을 수 있다. | □ |
| 업무코드·WAR·Context를 연결한다. | □ |

## Platform

| 점검 | 완료 |
| --- | --- |
| Gateway Route 흐름을 설명할 수 있다. | □ |
| JWT 발급·검증 책임을 구분한다. | □ |
| EAI Client 흐름을 이해한다. | □ |
| Cache 설정과 Evict를 찾을 수 있다. | □ |
| OM 정책과 Runtime 연결을 설명한다. | □ |
| Batch 수집흐름을 찾을 수 있다. | □ |
| UI Relay 흐름을 이해한다. | □ |
| CI/CD 설정 위치를 찾을 수 있다. | □ |

## 장애 분석

| 점검 | 완료 |
| --- | --- |
| 401 발생 시 확인 Module을 안다. | □ |
| Route 오류 위치를 안다. | □ |
| ServiceId 미등록 위치를 안다. | □ |
| Mapper 오류를 추적할 수 있다. | □ |
| Timeout의 여러 계층을 구분한다. | □ |
| 한 Instance만 오류일 때 비교할 항목을 안다. | □ |
| GUID로 Gateway·업무 로그를 연결할 수 있다. | □ |

# F.33 변경·호환성·폐기 관리

## 공통 Module 변경

tcf-core, tcf-web, tcf-util 변경 시 다음을 기록한다.

변경 API

영향 Module

Binary 호환성

설정 변경

전체 업무 Build

회귀 Test

배포순서

Rollback

## Module 분리

공통 Class를 별도 Module로 분리할 때:

실제 공통 사용자인가?

안정된 계약인가?

업무 Logic이 없는가?

순환의존이 생기지 않는가?

독립 Version이 필요한가?

를 확인한다.

## Module 통합

Module을 합칠 때:

배포단위

장애격리

자원격리

소유조직

변경주기

보안경계

를 검토한다.

## 레거시 Module 폐기

예: om-service

ServiceId 호출량 0

Gateway Route 제거

UI 메뉴 전환

DB 데이터 이행

운영 Script 제거

Artifact 보존

문서 갱신

삭제 승인

## 이름 변경

common-core → tcf-core

같은 이름 변경은 Import만 수정하는 작업이 아니다.

Gradle Module

Package

Artifact

CI/CD

운영 Script

문서

Dashboard

취약점 관리

전체 영향을 확인한다.

# 시사점

## 핵심 아키텍처 판단

첫째, TCF 소스는 Module별로 순차적으로 읽는 것보다 실제 ServiceId의 Runtime 경로를 따라 읽는 것이 가장 빠르다.

둘째, tcf-core는 거래 엔진이고 tcf-web은 HTTP·Spring·DataSource Adapter이므로 업무규칙이 두 Module에 유입돼서는 안 된다.

셋째, 현재 기준에는 독립 tcf-common이 없으며 기존 공통개념은 tcf-core·tcf-web·tcf-util에 분리돼 있다.

넷째, 업무 WAR의 표준 책임경로는 Handler→Facade→Service→Rule·DAO→Mapper이며 이를 벗어나는 직접호출은 변경영향과 테스트 복잡도를 증가시킨다.

다섯째, Gateway는 Route와 1차 JWT 검증을 담당하지만 업무 데이터권한을 대신하지 않는다.

여섯째, tcf-jwt는 Token 발급자이고 Gateway·업무 WAR는 Resource Server이므로 Private Key와 Public Key의 책임을 구분해야 한다.

일곱째, OM은 운영정책을 관리하는 Control Plane이며 업무 로직과 강하게 결합되면 OM 장애가 전체 업무장애로 확산될 수 있다.

여덟째, UI Catalog·문서·Script에 업무코드가 존재하더라도 실제 Module·WAR·Handler가 존재하는지는 소스와 Build 결과로 확인해야 한다.

아홉째, 공통 Module 변경은 모든 업무 WAR에 영향을 줄 수 있으므로 전체 Build·회귀·호환성 검증이 필요하다.

열째, 모듈 지도는 문서에만 존재해서는 안 되며 Architecture Test·의존성 검사·ServiceId 등록검사로 자동 검증돼야 한다.

## 주요 위험

| 위험 | 결과 |
| --- | --- |
| 모든 Class 순차 읽기 | 구조 이해 지연 |
| tcf-common 오인 | 잘못된 Module 탐색 |
| Core에 업무 Logic 유입 | 전체 업무 결합 |
| 업무 WAR 간 구현 참조 | 도메인 경계 붕괴 |
| Handler 직접 SQL | Transaction·테스트 혼란 |
| Gateway와 JWT 책임 혼동 | Key·인증구조 오류 |
| Gateway 인증만 신뢰 | 직접접근·권한 우회 |
| OM Runtime 강결합 | 운영관리 장애 전파 |
| Local Cache 불일치 | Instance별 정책 차이 |
| UI Catalog와 Module 불일치 | 존재하지 않는 거래 노출 |
| 공통 변경 단일업무 시험 | 다른 WAR 장애 |
| 레거시 Module 중복 | Route·ServiceId 충돌 |
| 문서만으로 판단 | 현재 Branch와 불일치 |
| Timeout 한 계층만 확인 | 근본 원인 누락 |
| Health만 확인 | 실제 거래장애 누락 |

## 우선 보완 과제

1.  현재 Branch의 실제 Module·WAR·Context 목록을 자동 생성한다.
2.  기존 tcf-common·common-core·common-web 문서표현을 현재 Module명과 정합화한다.
3.  Module 의존방향을 ArchUnit·Gradle 검사로 자동화한다.
4.  Handler→Facade→Service→Rule·DAO 규칙을 Architecture Gate로 적용한다.
5.  ServiceId·Handler·OM Catalog·Gateway Route의 교차검증을 구현한다.
6.  UI·UJ 업무 Catalog와 실제 Build Module을 자동 비교한다.
7.  Gateway Denylist·업무 WAR 2차 JWT 검증 상태를 확정한다.
8.  OM 장애 시 업무 거래의 Fallback·Cache 정책을 명확히 한다.
9.  om-service와 tcf-om의 전환·폐기계획을 수립한다.
10.  신규 ct-service의 Module·Profile·Route·OM·배포 편입을 완료한다.
11.  공통 Module 변경 시 전체 업무 회귀 Suite를 자동 실행한다.
12.  ServiceId별 Runtime 호출지도를 코드에서 자동 생성한다.
13.  Module별 Owner·배포주기·장애책임을 관리대장으로 운영한다.
14.  신규 개발자 교육에서 실제 ServiceId Breakpoint 추적을 필수 실습으로 적용한다.

# 마무리말

TCF 모듈 구조를 이해했다고 판단하려면 다음 질문에 답할 수 있어야 한다.

HTTP 요청은 어느 Module에서 처음 수신되는가?

Gateway는 어떤 기준으로 업무 WAR를 선택하는가?

JWT 발급과 검증은 각각 어느 Module이 담당하는가?

TCF.process는 어디에 있는가?

STF와 ETF는 무엇을 처리하는가?

ServiceId는 어느 Class에서 Handler와 연결되는가?

업무 Handler는 어느 Facade를 호출하는가?

Transaction은 어디서 시작되는가?

Service와 Rule의 책임은 어떻게 다른가?

Mapper XML과 실제 Table은 어디서 찾는가?

외부 업무 호출은 어느 Module을 사용하는가?

Cache 정책은 어디에서 확인하는가?

OM 정책이 Runtime 거래에 어떻게 반영되는가?

Dashboard 데이터는 어느 Batch가 수집하는가?

환경별 설정과 WAR 배포정보는 어디에 있는가?

401·404·ServiceId 미등록·Timeout 오류는 각각 어디서 시작해 확인하는가?

공통 Module 변경이 어떤 업무 WAR에 영향을 주는가?

현재 소스에 없는 Module과 문서상의 개념을 구분할 수 있는가?

하나의 ServiceId를 요청부터 응답까지 직접 추적할 수 있는가?

부록 F의 핵심 흐름은 다음과 같다.

ServiceId 선택

→ Sample Request

→ Gateway

→ tcf-web

→ tcf-core

→ 업무 Handler

→ Facade·Service·Rule

→ DAO·Mapper

→ ETF·로그

→ 오류 흐름 역추적

→ Module 책임과 의존성 확인

가장 중요한 원칙은 다음과 같다.

TCF를 이해하기 위해
모든 소스코드를 읽을 필요는 없다.

실제 거래 하나가
어디에서 시작되고,

어떤 공통 통제를 통과해,
어느 업무 프로그램과 SQL을 실행하며,

어떤 응답과 로그로 끝나는지를
정확히 따라갈 수 있어야 한다.

이 경로를 이해한 뒤
다른 ServiceId와 Module을 비교하면

복잡해 보이던 멀티모듈 구조는
각 책임이 연결된 하나의 거래지도로 보이게 된다.
