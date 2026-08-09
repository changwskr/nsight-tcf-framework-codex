# PDMK 통합 아키텍처 정의서

| 항목 | 내용 |
|---|---|
| 문서명 | PDMK 통합 아키텍처 정의서 (12장체계) |
| 버전 | v1.0 (**참고 · 병합 원천** — 정본은 [PDMK_아키텍처_정의서.md](./PDMK_아키텍처_정의서.md) v2.0) |
| 기준일 | 2026-08-08 |
| 기준 소스 | `pdmk-fw`, `pdmk-service`, `pdmg-ui` |
| 기준 문서 | `통합 아키텍처 정의서.md`, `통합 아키텍처 정의서 copy.md`, `표 아키텍처 정의서.md` |
| 아키텍처 기준 | **소스 우선(Source Evidence)**. 문서와 소스가 다르면 현행은 소스 기준으로 기록하고 차이를 Gap으로 관리 |
| 표기 | **AS-IS** = 현재 소스에서 확인됨 / **TO-BE** = 권고 또는 목표 / **GAP** = 현행과 목표·문서 간 불일치 |

> 본 정의서는 PDMK 프레임워크와 업무 서비스, 그리고 함께 제공된 UI 소스를 하나의 거래 스택 관점에서 분석한다.  
> 단, 업로드된 UI 소스의 실제 명칭은 `pdmg-ui`이며 서비스 ID도 `mgcoa*`를 사용한다. 반면 업무 서비스는 `pdmk-service`이고 `mkcoa*`를 사용한다. 이 차이는 임의로 보정하지 않고 **통합 계약 Gap**으로 기록한다.

---

# 목차

1. [문서 개요와 아키텍처 기준선](#1-문서-개요와-아키텍처-기준선)
2. [아키텍처 목표와 설계 원칙](#2-아키텍처-목표와-설계-원칙)
3. [통합 시스템 아키텍처](#3-통합-시스템-아키텍처)
4. [모듈 아키텍처](#4-모듈-아키텍처)
5. [런타임 거래 아키텍처](#5-런타임-거래-아키텍처)
6. [서비스·전문 계약 아키텍처](#6-서비스전문-계약-아키텍처)
7. [업무 애플리케이션 아키텍처](#7-업무-애플리케이션-아키텍처)
8. [데이터·트랜잭션 아키텍처](#8-데이터트랜잭션-아키텍처)
9. [공통·횡단 아키텍처](#9-공통횡단-아키텍처)
10. [플랫폼·환경 아키텍처](#10-플랫폼환경-아키텍처)
11. [배포·운영 아키텍처](#11-배포운영-아키텍처)
12. [아키텍처 검증·거버넌스](#12-아키텍처-검증거버넌스)
13. [부록](#부록)

---

# 1. 문서 개요와 아키텍처 기준선

## 1.1 목적

본 문서의 목적은 `pdmk-fw`, `pdmk-service`, UI 소스의 구조와 실행 흐름을 통합 관점에서 정의하고, 개발자가 동일한 패턴으로 신규 거래를 개발할 수 있는 아키텍처 기준을 제공하는 것이다.

본 문서는 단순 소스 설명서가 아니라 다음 네 가지 역할을 동시에 수행한다.

| 역할 | 설명 |
|---|---|
| 구조 정의 | 모듈, 레이어, 패키지, 의존 방향을 정의 |
| 런타임 정의 | 요청이 Filter부터 SQL까지 처리되는 순서를 정의 |
| 개발 표준 | Service ID, DTO, DAO, Mapper, TX, 예외 규칙을 정의 |
| 통제 기준 | 소스·문서 정합성, 아키텍처 Gap, 변경 영향과 검증 기준을 정의 |

## 1.2 대상 및 범위

### AS-IS 소스 기준

| 모듈 | 실제 소스 루트 | 주요 패키지 | 성격 |
|---|---|---|---|
| Framework | `pdmk-fw` | `nhnis.fw.commons.*`, `nhnis.fw.tcf.*` | 공통 프레임워크 라이브러리 |
| Service | `pdmk-service` | `nhnis.mk.*` | MK 업무 거래 서비스 |
| UI | `pdmg-ui` | `nhnis.mg.ui.*` | MG 전문 테스트 및 HTTP Relay UI |
| Data | `pdmk-service/src/main/resources/rdw.mk.co.a` | MyBatis XML | RDW SQL |
| Local DB | `db/h2/schema.sql`, `data.sql` | H2 Oracle Mode | 로컬 검증 |

### 범위 밖

- 실제 `pdmg-service` 소스는 이번 입력 소스 세트에 포함되지 않는다.
- TCF 경로는 소스에는 존재하지만 PDMK 서비스 설정상 `nhnis.fw.tcf.enabled=false`이므로 현재 PDMK 실행 경로에서는 비활성이다.
- 운영 인프라의 실제 L4, Apache, 서버 대수, 운영 DB 구성은 제공된 소스로 확인할 수 없으므로 본 정의서에서 확정하지 않는다.

## 1.3 Source Evidence 원칙

아키텍처 사실의 우선순위는 다음과 같이 정의한다.

```text
실제 실행 소스
   ↓
application.yml / build.gradle / Mapper XML
   ↓
README / 기존 통합 아키텍처 정의서
   ↓
권고 설계(TO-BE)
```

문서와 소스가 충돌할 경우 소스를 AS-IS로 기록하고, 기존 문서의 목표나 설계 의도는 TO-BE 또는 GAP으로 분리한다.

## 1.4 주요 Source Evidence

| 관심사 | 대표 소스 |
|---|---|
| Boot 진입 | `pdmk-service/src/main/java/nhnis/mk/PdmkApplication.java` |
| 외부 WAR | `ServletInitializer.java`, `pdmk-service/build.gradle` |
| Filter | `pdmk-fw/.../filter/DefaultFilter.java` |
| Interceptor | `pdmk-fw/.../interceptor/ServicePreventionInterceptor.java` |
| Request Binding | `RequestBodyArgumentResolver.java` |
| Response/Exception | `ResponseBodyArgumentResolver.java` |
| 업무 선후처리 | `pdmk-service/.../co/common/BizPrePostAspect.java` |
| Security | `pdmk-service/.../config/SecurityConfig.java` |
| RDW/MyBatis | `RdwDataSourceConfig.java` |
| SQL 계측 | `MybatisLogInterceptor.java` |
| 업무 샘플 | `mkcoa5530`, `mkcoa8888`, `mkcoa9999` Controller/Service/DAO |
| UI Relay | `pdmg-ui/.../TransactionRelayService.java` |
| UI 거래 목록 | `pdmg-ui/.../TransactionCatalog.java` |

## 1.5 핵심 기준선 Gap

| ID | AS-IS | 영향 | TO-BE 권고 |
|---|---|---|---|
| GAP-001 | UI는 `pdmg-ui`, 서비스 ID `mgcoa*`; Service는 `pdmk-service`, `mkcoa*` | UI→Service 직접 연계 실패 가능 | UI와 대상 Service의 업무코드/서비스ID 정합성 확정 |
| GAP-002 | 기존 문서는 `pdmk-ui`로 기술 | 문서와 소스 명칭 불일치 | 문서에서 실제 소스와 목표 명칭을 분리 |
| GAP-003 | `rdwTransactionManager`는 있으나 샘플 Service에 `@Transactional` 없음 | 다중 DML 원자성 보장 불명확 | Service TX 표준 적용 |
| GAP-004 | README는 `bootWar` 빌드 안내, `build.gradle`은 `bootWar=false`, `war=true` | 빌드 절차 혼선 | `gradlew war`를 정본으로 통일 또는 빌드 설정 변경 |
| GAP-005 | UI 설정 `timeout-ms=10000` 존재하나 `RestClient` 생성 시 해당 값 적용 코드 미확인 | 실제 Relay Timeout 정책 불명확 | 연결/응답 Timeout 실제 적용 |
| GAP-006 | `SecurityConfig`는 `anyRequest().permitAll()` | 비-local JWT 검증이 DefaultFilter에 집중 | 운영 인가 정책 분리 및 API 노출범위 확정 |

---

# 2. 아키텍처 목표와 설계 원칙

## 2.1 아키텍처 비전

PDMK 아키텍처는 **Service ID 중심 온라인 거래를 공통 프레임워크가 감싸고, 업무 애플리케이션은 Controller → Service → DAO → Mapper의 일관된 패턴으로 구현하는 구조**를 지향한다.

```text
표준 전문
   ↓
공통 런타임
   ↓
업무 서비스
   ↓
표준 데이터 접근
   ↓
추적 가능한 운영 로그
```

## 2.2 아키텍처 목표

| 목표 | 정의 |
|---|---|
| 표준화 | 모든 온라인 거래가 동일한 서비스 ID·전문·레이어 규칙을 사용 |
| 책임 분리 | FW, 업무 Service, UI, 데이터 접근의 역할을 분리 |
| 추적성 | GUID, Service ID, SQL ID를 통해 거래 전체를 추적 |
| 재사용성 | Filter, Header, JWT, 예외, ImageLog 등 공통 기능을 FW로 제공 |
| 유지보수성 | Controller/Service/DAO의 단방향 의존을 유지 |
| 이식성 | Local H2와 폐쇄망 Oracle의 전환 가능 구조 |
| 운영성 | 시스템 선후처리, 업무 로그, SQL 계측, ImageLog 제공 |
| 확장성 | 신규 Service ID를 동일한 구조로 추가 가능 |

## 2.3 설계 제약

소스에서 확인되는 기술 제약은 다음과 같다.

| 영역 | 기준 |
|---|---|
| Java | 21 |
| Spring Boot Plugin | 3.5.14 |
| Build | Gradle Wrapper 8.10.1 계열 |
| Service Packaging | WAR |
| FW Packaging | Library JAR |
| UI Packaging | Executable Boot JAR |
| Web | Spring MVC |
| Security | Spring Security + commons `DefaultFilter` JWT 경로 |
| Persistence | MyBatis |
| DataSource | HikariCP |
| Local DB | H2 `MODE=Oracle` |
| Target DB | Oracle 설정 주석 제공 |
| Logging | Log4j2 + log4jdbc + MDC |
| Encoding | UTF-8 강제 |

## 2.4 품질속성

현재 소스에서 직접 구현 근거가 있는 품질속성과, 추가 정의가 필요한 항목을 구분한다.

| 품질속성 | 현재 근거 | 평가 |
|---|---|---|
| 추적성 | GUID, MDC, `PdmkTxLog`, SQL ID | 강함 |
| 표준성 | `hdr_nhnis + dto`, Service ID 규칙 | 강함 |
| 오류 격리 | ImageLog DB 오류를 업무 예외로 전파하지 않음 | 양호 |
| 보안 | 비-local JWT 검사 | 부분 구현 |
| 성능 | SQL 실행시간 debug 측정 | 부분 구현 |
| Timeout | UI property, APIGW/FOS 설정 흔적 | 정책 통합 필요 |
| 트랜잭션 | TM Bean 존재 | 업무 선언 적용 Gap |
| 가용성 | 소스만으로 HA/이중화 미확인 | 별도 인프라 정의 필요 |
| 확장성 | 표준 C/S/DAO 패턴 | 양호 |
| 테스트성 | H2 Local 환경 | 양호 |

## 2.5 핵심 설계 원칙

1. **서비스 ID가 거래 식별의 중심이다.**
2. **공통 전문은 `hdr_nhnis + dto`를 사용한다.**
3. **업무 호출은 Controller → Service → DAO → Mapper 방향을 유지한다.**
4. **DB 트랜잭션 경계는 Service에 둔다.**
5. **Filter와 Interceptor는 DB 트랜잭션 경계를 만들지 않는다.**
6. **현재 PDMK는 commons 경로를 사용하고 TCF 경로는 동시에 사용하지 않는다.**
7. **FW는 업무 CRUD를 소유하지 않는다.**
8. **UI는 DB나 FW에 직접 의존하지 않고 HTTP로 Service를 호출한다.**
9. **GUID/Service ID/SQL ID를 운영 추적의 공통 키로 사용한다.**
10. **문서와 소스의 불일치는 숨기지 않고 Gap으로 관리한다.**

---

# 3. 통합 시스템 아키텍처

## 3.1 시스템 컨텍스트

이번 소스 세트의 실제 관계를 기준으로 보면 다음과 같다.

```text
┌──────────────┐
│   Browser    │
└──────┬───────┘
       │ HTTP
       ▼
┌─────────────────────────┐
│ pdmg-ui :8090           │
│ nhnis.mg.ui.*           │
│ mgcoa* Catalog / Relay  │
└──────────┬──────────────┘
           │ HTTP POST /mgcoa*
           │
           │  GAP: 제공된 서비스는 mkcoa*
           ▼
┌─────────────────────────┐
│ pdmk-service            │
│ nhnis.mk.*              │
│ POST /mkcoa*            │
└──────────┬──────────────┘
           │ implementation project(':pdmk-fw')
           ▼
┌─────────────────────────┐
│ pdmk-fw                 │
│ nhnis.fw.commons.*      │
│ Filter/Interceptor/...  │
└──────────┬──────────────┘
           │ JDBC / MyBatis
           ▼
┌─────────────────────────┐
│ RDW                     │
│ Local H2 / Target Oracle│
└─────────────────────────┘
```

## 3.2 논리 아키텍처

| 계층 | 구현 | 책임 |
|---|---|---|
| Presentation | `pdmg-ui`, `pdmk-service Controller` | 화면·API 진입 |
| Cross-cutting | `pdmk-fw commons`, `BizPrePostAspect` | Filter, Context, 선후처리, 응답, 로그 |
| Application | `pdmk-service Service` | 업무 규칙, 페이징, TX |
| Persistence | DAO + Mapper XML | SQL 실행 |
| Infrastructure | DataSource, Security, Gradle/WAS | 실행 기반 |

## 3.3 물리/프로세스 구조

소스로 확정 가능한 프로세스는 다음과 같다.

| 프로세스 | 포트/형태 | 비고 |
|---|---|---|
| UI | `8090`, Boot JAR | `pdmg-ui` |
| 업무 Service | 기본 Boot 포트는 별도 `server.port` 미지정 | UI 기본 target은 `http://localhost:8080` |
| 외부 WAS | WAR 배포 구조 | `providedRuntime` Tomcat + `ServletInitializer` |
| DB | H2/Oracle | 환경 전환 |

## 3.4 모듈 간 의존 규칙

```text
pdmg-ui ──HTTP──► target Service
                   │
                   ├── Java dependency ──► pdmk-fw
                   │
                   └── JDBC/MyBatis ─────► RDW
```

| 출발 | 목적 | 허용 |
|---|---|---|
| UI | Service | HTTP 허용 |
| UI | FW | 직접 Java 의존 금지 |
| UI | DB | 금지 |
| Service | FW | Gradle project dependency 허용 |
| Controller | Service | 허용 |
| Controller | DAO | 금지 |
| Service | DAO | 허용 |
| DAO | Service/Controller | 금지 |
| FW | 업무 DAO | 원칙상 금지 |

## 3.5 책임 매트릭스

| 관심사 | FW | Service | UI |
|---|---:|---:|---:|
| 표준 Header | ● | ○ | ○ |
| JWT 검증 | ● | 설정 | 전달 필요 |
| ServiceContext | ● | 사용 | - |
| 시스템 선후처리 | ● | - | - |
| 업무 선후처리 | - | ● | - |
| 업무 C/S/DAO | - | ● | - |
| SQL | 제한적(ImageLog) | ● | - |
| Relay | - | - | ● |
| 화면 | - | - | ● |
| Transaction | 인프라 일부 | ● | - |
| ImageLog | ● | 조회/삭제 샘플 | 화면 제공 |
| 빌드 산출물 | JAR | WAR | Boot JAR |

● 주 책임 / ○ 계약 공유

---

# 4. 모듈 아키텍처

## 4.1 `pdmk-fw`

### 목적

`pdmk-fw`는 업무와 독립적인 공통 런타임 기능을 제공하는 `java-library`이다. Boot 실행 애플리케이션이 아니며 `bootJar=false`, `jar=true`이다.

### 패키지 구조

```text
nhnis.fw
├── commons
│   ├── configuration
│   ├── filter
│   ├── interceptor
│   ├── resolver
│   ├── context
│   ├── dto
│   ├── exception
│   ├── jwt
│   ├── imagelog
│   ├── log
│   ├── message
│   ├── transaction
│   ├── apigw
│   ├── fos
│   └── util
├── tcf
│   ├── aspect
│   ├── stf
│   ├── etf
│   ├── context
│   ├── dto
│   └── web
└── exception
```

### 현재 활성 경로

```text
commons.filter.enabled=true
commons.legacy-web.enabled=true
tcf.enabled=false
```

따라서 PDMK Service 기준 런타임은 `commons`가 정본이다.

### 책임

| 영역 | 구현 | 책임 |
|---|---|---|
| 요청 Filter | `DefaultFilter` | Body cache, JWT, Header, Context |
| MVC 선후처리 | `ServicePreventionInterceptor` | Header 보강, ImageLog |
| 요청 바인딩 | `RequestBodyArgumentResolver` | `dto` 노드 → DTO |
| 응답 래핑 | `ResponseBodyArgumentResolver` | `hdr_nhnis + dto` |
| 예외 | `NhBaseException` | 표준 오류 |
| 메시지 | `MessageCache/Loader` | 업무 메시지 |
| 추적 | `PdmkTxLog`, MDC | 운영 추적 |
| 감사 | `ImageLogHandler` | PRE/POST/EX |
| 보안 | `JwtProvider` | Token 검증 |
| 외부 | APIGW/FOS | 연동 유틸 |

### 금지사항

- FW에 `nhnis.mk.*` 업무 구현을 추가하지 않는다.
- commons Security와 앱 Security를 동시에 활성화하지 않는다.
- commons JWT Filter와 TCF JWT Filter를 동시에 사용하지 않는다.
- 신규 업무 테이블 CRUD를 FW에 구현하지 않는다.

## 4.2 `pdmk-service`

### 목적

MK 업무 서비스 구현체이며 Spring Boot 진입점, 업무 거래, Security, DataSource, MyBatis를 소유한다.

### 패키지 구조

```text
nhnis.mk
├── PdmkApplication
├── ServletInitializer
├── config
│   ├── SecurityConfig
│   ├── CorsProperties
│   ├── WebMvcConfig
│   ├── RDWMapper
│   ├── RdwDataSourceConfig
│   └── MybatisLogInterceptor
├── common.util
└── co
    ├── common
    │   └── BizPrePostAspect
    └── a
        ├── controller
        ├── service
        ├── dao
        └── dto
```

`PdmkApplication`은 `scanBasePackages="nhnis"`를 사용하므로 `pdmk-fw`의 `nhnis.fw.*` Bean도 같은 ApplicationContext에 로드된다.

### 샘플 프로그램

| 프로그램 | 거래 | 기능 |
|---|---|---|
| `mkcoa5530` | `mkcoa5530S0` | 안내항목/마케팅희망고객 조회 |
| `mkcoa8888` | `mkcoa8888S0`, `mkcoa8888D0` | ImageLog 조회·삭제 |
| `mkcoa9999` | `mkcoa9999S0` | 영업팁 실적 조회 |

## 4.3 `pdmg-ui`

### 목적

브라우저와 대상 서비스 사이에서 전문을 편집하고 HTTP를 중계하는 별도 Boot 애플리케이션이다.

### 패키지 구조

```text
nhnis.mg.ui
├── PdmgUiApplication
├── config
│   └── PdmgUiProperties
├── entry.web
│   ├── PdmgUiHomeController
│   └── PdmgUiApiController
├── application.service
│   └── TransactionCatalog
├── client
│   └── TransactionRelayService
└── support
    ├── RelayResult
    └── TransactionInfo
```

### UI 내부 레이어

| 레이어 | 책임 |
|---|---|
| entry | 브라우저용 API |
| application | 거래 카탈로그 |
| client | HTTP Relay |
| support | Relay 결과/거래 메타데이터 |
| config | 대상 URL, Timeout property |

### GAP

UI는 `mgcoa*`를 등록하지만 제공된 Service는 `mkcoa*`만 제공한다. 따라서 **이번 세 소스가 그대로 하나의 실행 스택이라고 단정할 수 없다.**

TO-BE는 다음 둘 중 하나를 결정해야 한다.

1. `pdmg-ui`는 `pdmg-service`에 연결하고 PDMK 정의서 범위에서 분리한다.
2. PDMK 테스트 UI가 목적이라면 UI 패키지/Service ID/리소스를 `pdmk-ui` / `mkcoa*`로 일치시킨다.

---

# 5. 런타임 거래 아키텍처

## 5.1 End-to-End 흐름

PDMK Service에 정상적으로 `mkcoa*` 요청이 들어온 경우의 실행 체인은 다음과 같다.

```text
HTTP POST /mkcoaXXXXS0
        │
        ▼
DefaultFilter
  Body Cache / JWT / hdr_nhnis / ServiceContext / MDC
        │
        ▼
Spring Security FilterChain
        │
        ▼
DispatcherServlet
        │
        ▼
ServicePreventionInterceptor.preHandle
  GUID / Header 보강 / ImageLog PRE
        │
        ▼
BizPrePostAspect @Before
        │
        ▼
Controller
        │
        ▼
Service
        │
        ▼
DAO
        │
        ▼
MyBatis Mapper XML
        │
        ▼
RDW
        │
        ▼
Service → Controller
        │
        ▼
BizPrePostAspect @After
        │
        ▼
ServicePreventionInterceptor.postHandle
  ImageLog POST
        │
        ▼
ResponseBodyArgumentResolver
  {hdr_nhnis, dto}
        │
        ▼
DefaultFilter finally
  ServiceContext/MDC clear
```

## 5.2 `DefaultFilter`

### 책임

`DefaultFilter`는 Servlet 계층에서 가장 먼저 PDMK 거래의 실행 문맥을 만든다.

| 단계 | 처리 |
|---|---|
| Service ID 추출 | URI 마지막 segment |
| Header 수집 | HTTP Header → `HttpHeaders` |
| multipart | `X-GUID` 기반 최소 Context |
| 일반 JSON | `CachedBodyHttpServletRequest` |
| 비-local | Bearer Token 검증 |
| Body 검증 | 빈 Body 400 |
| JSON 검증 | 파싱 실패 400 |
| 공통 Header | `hdr_nhnis` 변환 |
| local Header | 미입력 시 합성 |
| Context | `ServiceContextHolder` |
| MDC | guid/ip/userId/serviceId |
| 종료 | finally에서 Context/MDC 정리 |

### local / non-local

| 항목 | local | non-local |
|---|---|---|
| JWT | 생략 | Access Token 필수 |
| `hdr_nhnis` | 생략 가능 | 필수 |
| Header 누락 | 합성 | 400 |
| 인증 실패 | - | 401 |

### 아키텍처 규칙

- Filter에서 업무 SQL을 호출하지 않는다.
- Filter 실패는 MVC Advice 이전에 끝날 수 있으므로 표준 `hdr+dto` 에러 봉투가 보장되지 않는다.
- ThreadLocal/MDC 정리는 반드시 `finally`에 둔다.

## 5.3 시스템 선후처리

`ServicePreventionInterceptor`는 Filter가 만든 Context를 기반으로 시스템 공통 처리를 담당한다.

| 시점 | 역할 |
|---|---|
| `preHandle` | GUID 확정, Header 보강, ImageLog PRE |
| `postHandle` | ImageLog POST |
| `afterCompletion` | 예외 ImageLog EX 및 정리 |

Filter와 Interceptor의 역할을 중복시키지 않는다.

```text
Filter        = 요청 성립
Interceptor   = 시스템 거래 선후
Aspect        = 업무 Controller 선후
Service       = 업무 처리와 DB TX
```

## 5.4 업무 선후처리

`BizPrePostAspect`는 `nhnis.mk.co..controller..*`를 대상으로 `@Before/@After` 로그를 남긴다.

- DB 트랜잭션을 열지 않는다.
- ImageLog를 직접 처리하지 않는다.
- DTO에서 BRC 성격 Getter를 반사 호출하여 로그 정보를 보조한다.
- `@Order(100)`이다.

## 5.5 Request Binding

Controller 파라미터의 사용자 정의 `@RequestBody` 처리는 `RequestBodyArgumentResolver`가 담당한다.

설계 의도는 전체 JSON 중 `dto` 노드를 업무 DTO로 변환하여 Controller가 공통 Header 파싱을 반복하지 않도록 하는 것이다.

```text
{
  "hdr_nhnis": {...},
  "dto": {...}
}
        │
        ├── Filter → hdr_nhnis
        └── Resolver → DTOin
```

## 5.6 Response Binding

정상 응답과 `NhBaseException` 기반 오류는 `ResponseBodyArgumentResolver`에서 공통 봉투로 조립한다.

```text
Controller 반환 DTOout
        │
        ▼
ResponseBodyAdvice
        │
        ▼
{
  "hdr_nhnis": {...},
  "dto": {...}
}
```

DTO 내부의 생성 프레임워크 보조 필드(`dtoLogicalName`, `fieldPropertyMap`, `*List`, `*Array`)는 응답 JSON에서 제거하도록 구현되어 있다.

## 5.7 예외 런타임 흐름

```text
Service/DAO
   │ throw NhBaseException
   ▼
ResponseBodyArgumentResolver @ExceptionHandler
   │
   ├─ NH_NIS_ERR_DTO 생성
   ├─ exceptionCode.yml 또는 MessageCache 조회
   └─ HTTP 500
   ▼
ResponseBodyAdvice
   ▼
{hdr_nhnis, dto: error}
```

Filter 단계 오류는 이 흐름과 다르다.

```text
DefaultFilter
  ├─ JWT 오류 → 401 sendError
  ├─ JSON 오류 → 400 sendError
  └─ Header 오류 → 400 sendError
```

## 5.8 런타임 검증항목

- [ ] Service ID가 URI와 `hdr_nhnis.sys_comm.rms_svc_c`에서 일치
- [ ] local 외 프로파일에서 JWT가 실제 검증됨
- [ ] Controller 이전에 ServiceContext가 존재
- [ ] ImageLog PRE/POST/EX가 정상 호출
- [ ] 종료 후 MDC가 제거됨
- [ ] 예외 시 응답 규격과 HTTP Status가 정책과 일치

---

# 6. 서비스·전문 계약 아키텍처

## 6.1 Service ID

현재 샘플에서 Service ID는 URL과 Controller/Service 메서드 이름의 중심 식별자로 사용된다.

예:

```text
mkcoa5530S0
││││││││││
└────────── 거래 식별자
```

소스의 상세 코드 의미는 네이밍 문서가 정본이며, 본 문서는 연결 규칙을 정의한다.

| 대상 | 예 |
|---|---|
| URL | `/mkcoa5530S0` |
| Controller Method | `mkcoa5530S0(...)` |
| Service Method | `mkcoa5530S0(...)` |
| DTO | `mkcoa5530S0DTOin/out` |
| DAO SQL ID | `mkcoa5530S0_S0`, `_count` 등 |
| Screen/Program | `mkcoa5530` |

## 6.2 전문 기본 구조

```json
{
  "hdr_nhnis": {
    "sys_comm": {
      "std_gbl_id": "...",
      "rms_svc_c": "mkcoa5530S0",
      "scid": "mkcoa5530",
      "tr_trm_ipadr": "...",
      "optr_eno": "..."
    }
  },
  "dto": {
  }
}
```

## 6.3 공통 Header

대표 필드와 사용 목적은 다음과 같다.

| 필드 | 용도 |
|---|---|
| `std_gbl_id` | 거래 GUID, ImageLog Key |
| `rms_svc_c` | Service ID |
| `scid` | Screen/Program ID |
| `tr_trm_ipadr` | Client IP |
| `optr_eno` | 사용자/조작자 식별 |
| `tr_sysid` | 시스템 식별 |
| `tr_brc` | 거래점/업무점 |
| `ttl_ug_ync` | 기존 전문 필드로 존재, DB TX와 동일 개념으로 사용 금지 |

## 6.4 DTO 규칙

| 종류 | 규칙 |
|---|---|
| 입력 | `{ServiceId}DTOin` |
| 출력 | `{ServiceId}DTOout` |
| 반복부 | `{ServiceId}DTOSub0` 등 |
| 기반 타입 | 샘플은 `DataObject` 상속 |
| JSON Marshalling | 일부 생성 DTO는 `*MsgJson` 제공 |

DTO는 Controller와 Service의 계약이며 DAO 계층의 영속 객체로 사용하지 않는 것을 원칙으로 한다. 현재 샘플 DAO는 `Map<String,Object>` 중심이다.

## 6.5 Validation

현재 샘플은 Bean Validation보다 Service 내부의 수동 검증이 중심이다.

예:
- pageNo <= 0 → 1
- pageSize <= 0 → 20
- pageSize > 100 → 100
- 삭제 GUID 목록이 비어 있으면 결과코드 반환

TO-BE:
- 전문 필수값·길이·형식 검증 기준을 DTO Validation 또는 공통 Validator로 표준화한다.
- “정상적인 0건/미입력”과 “실제 오류”를 구분한다.

## 6.6 페이징 계약

샘플 조회 Service는 다음 규칙을 사용한다.

| 항목 | 기본/규칙 |
|---|---|
| `pageNo` | 기본 1 |
| `pageSize` | 기본 20 |
| 최대 pageSize | 100 |
| `offset` | `(pageNo-1) * pageSize` |
| 응답 | `size`, `pageNo`, `pageSize`, `totalCount`, `totalPages` |

## 6.7 UI-Service 계약 Gap

### AS-IS

```text
pdmg-ui                  pdmk-service
mgcoa5530S0     !=       mkcoa5530S0
mgcoa8888S0     !=       mkcoa8888S0
mgcoa8888D0     !=       mkcoa8888D0
mgcoa9999S0     !=       mkcoa9999S0
```

UI 카탈로그가 기본 URL `http://localhost:8080`에 `mgcoa*`를 전송하면, 제공된 PDMK Service의 `mkcoa*` Controller와 매핑되지 않는다.

### TO-BE 의사결정

| 대안 | 설명 |
|---|---|
| A | `pdmg-ui`를 PDMG 전용으로 유지하고 본 PDMK 스택에서 제외 |
| B | PDMK UI를 별도 생성하여 `mkcoa*`로 통일 |
| C | Relay에서 업무 코드 변환을 수행 — 비권장, 계약을 숨김 |

권고는 **A 또는 B**이다. 서비스 식별자 변환은 아키텍처 경계를 불명확하게 하므로 권장하지 않는다.

---

# 7. 업무 애플리케이션 아키텍처

## 7.1 표준 레이어

```text
Controller
    │
    ▼
Service
    │
    ▼
DAO
    │
    ▼
Mapper XML
    │
    ▼
RDW
```

## 7.2 Controller

### 책임

- HTTP URL 매핑
- DTOin 수신
- Service 호출
- DTOout 반환
- 진입/종료 로그

### 금지

- SQL 작성
- DAO 직접 호출
- DataSource 직접 접근
- 복잡한 업무 규칙
- 장기 트랜잭션 처리

### 현재 샘플

```text
mkcoa5530Controller → POST /mkcoa5530S0
mkcoa8888Controller → POST /mkcoa8888S0, /mkcoa8888D0
mkcoa9999Controller → POST /mkcoa9999S0
```

## 7.3 Service

### 책임

- 업무 규칙
- 입력값 정규화
- 페이징 계산
- DAO 조합
- Map→DTO 매핑
- 트랜잭션 경계

현재 샘플에서 `mkcoa5530Service`, `mkcoa8888Service`, `mkcoa9999Service`가 이 역할을 수행한다.

### GAP: 선언적 TX

현재 샘플 Service 소스에서 `@Transactional` 선언이 확인되지 않는다. 따라서 설계서에서 말하는 “TX는 Service”는 **현재 구현 완료 사실이 아니라 권고 아키텍처**로 분리해야 한다.

TO-BE 예:

```java
@Service
@Transactional(readOnly = true)
public class mkcoaXXXXService {

    public ...S0(...) { ... }

    @Transactional
    public ...D0(...) { ... }
}
```

## 7.4 DAO

DAO는 `@RDWMapper`로 MyBatis Mapper 인터페이스가 된다.

### 책임

- SQL ID와 Java 메서드의 계약
- 파라미터 전달
- Result 반환

### 금지

- 업무 분기
- Controller 의존
- 전문 Header 처리
- 직접 Commit/Rollback

## 7.5 Mapper XML

위치 규칙:

```text
src/main/resources/rdw.mk.co.a/
    mkcoa5530-ORA.xml
    mkcoa8888-ORA.xml
    mkcoa9999-ORA.xml
```

`RdwDataSourceConfig`는 `classpath*:rdw.*/*.xml` 패턴으로 Mapper를 로딩한다.

## 7.6 샘플 거래 패턴

### 조회 S0

```text
DTOin
  ↓
Service: 조건 Map + paging
  ↓
DAO count
  ↓
DAO list
  ↓
Map rows → DTOSub0
  ↓
DTOout
```

### 삭제 D0

`mkcoa8888D0`는 `guidList`를 정규화하고 DAO Delete 결과를 `PROC_CNT`, `RSLT_CD`, `RSLT_MSG`에 담아 반환한다.

이 패턴은 “업무 정상 분기”를 예외가 아닌 DTO 결과로 반환하는 샘플이다.

## 7.7 신규 거래 표준 구조

신규 프로그램 `mkcoa1234`의 예:

```text
nhnis.mk.co.a.controller.mkcoa1234Controller
nhnis.mk.co.a.service.mkcoa1234Service
nhnis.mk.co.a.dao.mkcoa1234DAO
nhnis.mk.co.a.dto.mkcoa1234S0DTOin
nhnis.mk.co.a.dto.mkcoa1234S0DTOout
resources/rdw.mk.co.a/mkcoa1234-ORA.xml
```

---

# 8. 데이터·트랜잭션 아키텍처

## 8.1 RDW DataSource

`RdwDataSourceConfig`는 다음 Bean을 구성한다.

```text
spring.datasource.rdw
        │
        ▼
rdwDataSource (HikariDataSource, @Primary)
        │
        ├─ rdwSqlSessionFactory
        │      └─ MybatisLogInterceptor
        ├─ rdwSqlSessionTemplate
        └─ rdwTransactionManager
```

| Bean | 역할 |
|---|---|
| `rdwDataSource` | Connection Pool |
| `rdwSqlSessionFactory` | Mapper XML/MyBatis 설정 |
| `rdwSqlSessionTemplate` | Mapper 실행 |
| `rdwTransactionManager` | Spring DB TX |
| `MybatisLogInterceptor` | SQL ID·소요시간 |

## 8.2 환경별 DB

### Local

```yaml
jdbc:log4jdbc:h2:mem:pdmk;MODE=Oracle
```

- `schema.sql`
- `data.sql`
- `spring.sql.init.mode=always`

### 폐쇄망 Target

소스에는 Oracle 접속 예시가 주석으로 제공된다.

TO-BE 운영 전환 시:
- H2 runtime 의존성 해제
- Oracle JDBC 활성
- H2 `sql.init` 비활성
- 비밀번호는 환경변수/비밀관리로 주입

## 8.3 데이터 접근 계약

| 단계 | 계약 |
|---|---|
| Service → DAO | `Map`, primitive, List 등 |
| DAO → Mapper | 메서드명/SQL ID |
| Mapper → DB | SQL |
| DB → Service | Map/List |
| Service → DTO | 명시적 매핑 |

## 8.4 테이블 사용 현황

| 서비스 | 테이블 | DML |
|---|---|---|
| FW ImageLog | `TB_FW_IMAGE_LOG` | INSERT/UPDATE |
| `mkcoa8888S0` | `TB_FW_IMAGE_LOG` | SELECT |
| `mkcoa8888D0` | `TB_FW_IMAGE_LOG` | DELETE |
| `mkcoa5530S0` | `TB_MK_CO_A_5530` | SELECT |
| `mkcoa9999S0` | `TB_CR_AH_SALES_TIP_RACT` | SELECT |

## 8.5 트랜잭션 경계

### 목표 구조

```text
Controller
    │
    ▼
@Transactional Service
    │
    ├─ DAO SQL #1
    ├─ DAO SQL #2
    └─ DAO SQL #3
         │
         ▼
 commit / rollback
```

### 현재 상태

- `rdwTransactionManager` Bean: 존재
- MyBatis와 동일 DataSource: 존재
- 샘플 Service `@Transactional`: 미확인

따라서 **트랜잭션 인프라는 준비되어 있으나 업무 경계 선언은 Gap**이다.

## 8.6 ImageLog와 업무 TX

`ImageLogHandler`는 DataSource로 `JdbcTemplate`을 생성하고 시스템 선후처리에서 직접 실행한다.

구현의 중요한 정책은 **ImageLog DB 실패를 catch하여 업무 거래를 실패시키지 않는 것**이다.

```text
업무 거래                        감사 로그
Service TX                      Interceptor
   │                                │
   ├─ 업무 SQL                      └─ ImageLog JDBC
   │                                   try/catch
   └─ rollback/commit                  실패 격리
```

주의: 동일 DataSource 및 호출 시점에 따라 실제 Spring TX 참여 여부는 실행 컨텍스트에 영향을 받을 수 있으므로, “항상 완전히 별도 커밋”이라고 단정하지 않는다. 독립 TX가 필수라면 별도 TransactionManager/REQUIRES_NEW 등 명시적 설계가 필요하다.

## 8.7 SQL 계측

`MybatisLogInterceptor`는 MyBatis Executor의 query/update를 가로채 다음 정보를 기록한다.

- `MappedStatement.getId()` → `sqlId`
- 파라미터
- 실행시간(ms)
- MDC의 `sqlId`

TO-BE:
- 운영환경에서는 민감정보 파라미터 마스킹 기준이 필요하다.
- Slow SQL 임계값을 명시하고 별도 로그 레벨/알람 기준을 정의한다.

---

# 9. 공통·횡단 아키텍처

## 9.1 예외·에러

현재 PDMK commons의 표준 예외는 `NhBaseException`이다.

| 구분 | 구현 | PDMK 사용 |
|---|---|---|
| commons | `NhBaseException` | 사용/권고 |
| TCF | `BizException`, `ETF`, `GlobalExceptionHandler` | `tcf=false`로 비활성 |
| Filter | `sendError` | 사용 |
| Security | AuthenticationEntryPoint | 설정 존재 |

### 오류 유형

`NhBaseException.TYPE`은 RUNTIME, COMMON, AUTH, SERVICE, BIZ 등의 구분을 사용한다.

### 메시지 소스

```text
RUNTIME/COMMON/AUTH
   └─ exceptionCode.yml

SERVICE/BIZ
   └─ MessageCache
        └─ MessageLoader(DB)
```

### 응답 정책

MVC 이후 오류:
```json
{
  "hdr_nhnis": {},
  "dto": {
    "stdErrCode": "...",
    "stdErrMsgCntn": "..."
  }
}
```

Filter 이전/내부 오류:
- HTTP 400/401 + Servlet error
- 동일 봉투가 보장되지 않음

TO-BE 의사결정:
Filter 오류까지 전문 규격을 통일할지 여부를 명확히 정한다.

## 9.2 Security

### AS-IS

`pdmk-service SecurityConfig`:

- CSRF disable
- CORS enable
- HTTP Basic disable
- Form Login disable
- Stateless
- `anyRequest().permitAll()`

실제 JWT 검증은 비-local에서 `DefaultFilter`가 담당한다.

### 계층

```text
DefaultFilter JWT
      ↓
SecurityFilterChain permitAll
      ↓
Controller
```

### 금지

- TCF `JwtAuthenticationFilter`와 commons JWT 동시 사용 금지
- FW `commons.configuration.SecurityConfig`와 앱 Security 이중 사용 금지

### TO-BE

- 인증(authentication)과 인가(authorization)를 분리한다.
- 운영 API별 permit/role 정책을 확정한다.
- JWT Secret은 소스 기본값을 사용하지 않는다.
- UI Relay가 non-local 서비스에 접근할 경우 Authorization 전달 정책이 필요하다.

## 9.3 CORS

`WebMvcConfig`는 `spring.mvc.cors.*`가 비어 있으면 CORS mapping을 등록하지 않는다.

현재 mapping path는 `/api/**`인데 PDMK 업무 거래 URL은 `/mkcoa*`이다. 브라우저가 Service를 직접 호출할 계획이라면 경로 정책 재검토가 필요하다.

UI는 서버 사이드 Relay를 사용하므로 브라우저 CORS 의존을 줄이는 구조이다.

## 9.4 거래 로그와 추적

### 공통 키

| 키 | 소스 |
|---|---|
| `guid` | `std_gbl_id` |
| `serviceId` | URI / `rms_svc_c` |
| `ip` | `tr_trm_ipadr` |
| `userId` | `optr_eno` |
| `sqlId` | MyBatis MappedStatement |

### 흐름

```text
GUID
 ├─ DefaultFilter MDC
 ├─ ServiceContext
 ├─ ServicePreventionInterceptor
 ├─ TB_FW_IMAGE_LOG
 └─ 거래 로그
```

## 9.5 ImageLog

처리:

| 시점 | DB 처리 |
|---|---|
| PRE | INSERT |
| POST | RESPONSE_TIME UPDATE |
| EX | RESPONSE_TIME + 예외정보 UPDATE, 필요 시 INSERT |

목표는 “거래가 들어왔고 어디까지 처리되었는지”를 추적하는 것이다.

## 9.6 Cache

현재 소스에서 명확한 공통 Cache는 `MessageCache`이다. 또한 HTTP Body 재사용을 위한 `CachedBodyHttpServletRequest`가 있지만 이는 업무 데이터 Cache가 아니라 요청 버퍼다.

임의로 업무 조회결과에 `@Cacheable`을 추가하지 않는다. 업무 Cache는 별도의 정합성/TTL/무효화 설계를 선행한다.

## 9.7 외부연동

`pdmk-fw`에는 APIGW, FOS 관련 handler가 존재한다. 그러나 이번 샘플 업무에서 실제 호출되는 통합 시나리오는 확인되지 않는다.

따라서 본 정의서는 다음까지만 확정한다.

- 외부 연동 공통 기능은 FW 영역에 위치
- Timeout/인증/오류 변환은 공통화 대상
- 실제 업무별 외부 연동 계약은 별도 인터페이스 정의가 필요

## 9.8 Timeout

확인되는 설정:
- UI `pdmg.ui.timeout-ms: 10000`
- FOS default timeout 값 존재
- 기타 외부연동 관련 설정 가능성

GAP:
UI `timeout-ms`가 `TransactionRelayService`의 `RestClient`에 실제 적용되는 코드는 확인되지 않는다.

TO-BE Timeout 계층:

```text
Browser/UI
   timeout
      ↓
Relay HTTP
   connect/read timeout
      ↓
Service
   transaction timeout
      ↓
DB / External Client
   query/connect timeout
```

각 계층의 값과 상하 관계를 운영 기준으로 별도 확정해야 한다.

---

# 10. 플랫폼·환경 아키텍처

## 10.1 Spring Boot

| 모듈 | Boot | 역할 |
|---|---|---|
| `pdmk-fw` | Plugin 사용, 실행 JAR off | dependency/BOM 활용 |
| `pdmk-service` | Boot Application + WAR | 업무 WAS |
| `pdmg-ui` | Boot Application + Boot JAR | UI |

Service는 `@SpringBootApplication(scanBasePackages="nhnis")`로 FW를 함께 스캔한다.

## 10.2 Gradle 구조

세 모듈은 각각 Wrapper와 Gradle 설정을 가진다.

### FW

```text
java-library
bootJar = false
jar = true
```

### Service

```text
java + war
implementation project(':pdmk-fw')
providedRuntime tomcat
bootWar = false
war = true
```

### UI

```text
java + spring boot
bootJar archive = pdmg-ui.jar
```

## 10.3 Dependency 전략

### FW `api`

FW는 Web, Security, MyBatis, AOP, Log4j2, JWT 등을 `api`로 제공하여 소비 Service에 전파한다.

장점:
- 업무 프로젝트 설정 단순화

주의:
- FW에서 버전 고정을 많이 하면 업무 애플리케이션의 dependency upgrade 자유도가 낮아진다.
- Boot 3.5.14 플러그인과 일부 starter 3.5.10 명시 버전이 혼재하므로 dependency consistency 검증이 필요하다.

## 10.4 환경 설정

### Service

- `spring.profiles.active=local`
- `spring.application.name=pdmk`
- H2 Local
- `nhnis.fw.tcf.enabled=false`
- `legacy-web=true`
- `filter=true`

### UI

- port 8090
- name `pdmg-ui`
- target URL localhost:8080
- timeout property 10000

## 10.5 폐쇄망 저장소

소스에는 로컬 `mavenCentral()`과 사내 Nexus 설정을 주석 전환하는 패턴이 존재한다.

TO-BE:
- 개발자가 주석을 수동 편집하는 방식보다 Gradle property/profile로 저장소를 선택하도록 개선하는 것을 권고한다.
- Repository URL/credential을 코드에 직접 고정하지 않는다.

## 10.6 인코딩

세 프로젝트는 한글 주석/소스를 위해 UTF-8 설정을 명시하고 있다.

- `JavaCompile.options.encoding=UTF-8`
- `processResources.filteringCharset=UTF-8`
- `bootRun` JVM encoding args
- 테스트 encoding

이 설정은 Windows/CP949 환경에서의 빌드 재현성을 위한 아키텍처 운영 기준으로 유지한다.

---

# 11. 배포·운영 아키텍처

## 11.1 산출물

| 모듈 | 정본 산출물 |
|---|---|
| `pdmk-fw` | `pdmk-fw-0.0.1-SNAPSHOT.jar` |
| `pdmk-service` | WAR |
| `pdmg-ui` | `pdmg-ui.jar` |

주의: 압축 내 과거 `build/libs`에는 이전 명칭 산출물이 남아 있을 수 있으므로 **소스 `build.gradle`을 정본으로 판단**한다.

## 11.2 Service WAR 배포

`pdmk-service`는:
- `war` plugin 사용
- `providedRuntime` Tomcat
- `ServletInitializer` 제공

따라서 목표 배포는 외부 Servlet Container/Tomcat에 WAR를 배치하는 구조다.

### GAP

README의 빌드 예는 `bootWar`를 언급하지만 `build.gradle`은 `bootWar.enabled=false`, `war.enabled=true`이다.

권고 정본:

```text
gradlew clean war
```

또는 프로젝트가 실행형 Boot WAR를 요구한다면 빌드 설정 자체를 변경하고 문서와 함께 동기화한다.

## 11.3 Local 기동

Local 개발은 Boot main을 통해 실행 가능하다.

Service:
```text
nhnis.mk.PdmkApplication
```

UI:
```text
nhnis.mg.ui.PdmgUiApplication
```

## 11.4 기동 순서

UI가 Relay 대상 서비스에 의존하므로 일반적인 테스트 순서는 다음과 같다.

```text
1. 대상 Service/DB 기동
2. Service endpoint smoke test
3. UI 기동
4. UI Relay test
```

단, 현재 제공된 `pdmg-ui`와 `pdmk-service`는 Service ID가 불일치하므로 통합 테스트 전 GAP-001을 먼저 해결한다.

## 11.5 환경 전환

| 항목 | Local | Target |
|---|---|---|
| DB | H2 | Oracle |
| JDBC | H2/log4jdbc | Oracle/log4jdbc |
| SQL Init | ON | OFF 권고 |
| JWT | local bypass | 검증 |
| Repository | Maven Central | Nexus |
| Secret | 개발 기본 | 환경변수/Secret |

## 11.6 장애 추적

표준 장애 추적 순서:

```text
1. GUID 확인
2. Filter 통과 여부
3. ImageLog PRE 존재 여부
4. Controller/Service 로그
5. SQL ID와 SQL 소요시간
6. 예외 코드/메시지
7. ImageLog EX/POST
8. 응답 전문
```

### 장애 위치 판단

| 증상 | 우선 확인 |
|---|---|
| 400 | JSON/Header/Filter |
| 401 | JWT/Authorization |
| 404 | Service ID/URL 계약 |
| 500 + 에러DTO | Service/DAO/NhBaseException |
| UI 502 | Relay 대상 URL/서비스 기동 |
| 조회 지연 | SQL ID/DB/페이징 |
| GUID 혼선 | Context/MDC clear |

## 11.7 운영 관측성 TO-BE

현재 로그 기반 관측에 추가로 다음 지표를 권고한다.

- Service ID별 TPS
- 평균/p95 응답시간
- HTTP 4xx/5xx 비율
- SQL ID별 평균/최대 시간
- Hikari active/idle/pending
- JVM Heap/GC
- Tomcat busy thread
- ImageLog 누락률
- 외부연동 Timeout 건수

이 수치의 실제 목표값은 이번 소스에서 확인되지 않으므로 별도 운영 기준에서 확정한다.

---

# 12. 아키텍처 검증·거버넌스

## 12.1 목적

아키텍처 정의서가 설명 문서에서 끝나지 않도록, 소스가 정의한 규칙을 지속적으로 검증하는 체계를 둔다.

## 12.2 준수 등급

| 등급 | 의미 |
|---|---|
| MUST | 반드시 지켜야 함 |
| SHOULD | 특별한 사유가 없으면 준수 |
| MAY | 선택 가능 |
| MUST NOT | 금지 |

## 12.3 핵심 MUST 규칙

| ID | 규칙 |
|---|---|
| ARC-001 | Controller는 DAO를 직접 호출하지 않는다 |
| ARC-002 | 업무 DB TX 경계는 Service에 둔다 |
| ARC-003 | Service ID와 URL/Controller 메서드/전문 Header를 일치시킨다 |
| ARC-004 | UI는 DB에 직접 접근하지 않는다 |
| ARC-005 | commons와 TCF 보안 Filter를 동시에 활성화하지 않는다 |
| ARC-006 | 요청 종료 시 Context/MDC를 제거한다 |
| ARC-007 | Mapper XML namespace/SQL ID는 DAO와 일치시킨다 |
| ARC-008 | 운영 Secret을 저장소 기본값으로 사용하지 않는다 |
| ARC-009 | ImageLog 실패가 업무 거래 실패를 직접 유발하지 않도록 한다 |
| ARC-010 | 문서와 소스의 차이는 Gap으로 기록한다 |

## 12.4 자동 검증 항목

### 구조

- Controller가 DAO를 import하는지 검사
- DAO가 Controller/Service를 import하는지 검사
- FW에 `nhnis.mk` 패키지가 추가되었는지 검사

### Naming

- `mkcoa####Controller`
- `mkcoa####Service`
- `mkcoa####DAO`
- `mkcoa####{처리유형}DTOin/out`
- `rdw.mk.co.a/mkcoa####-ORA.xml`

### API

- `@PostMapping` URL과 메서드명
- `rms_svc_c` 샘플 전문
- UI Catalog ID
- Service Controller ID

### DB

- DAO 메서드 ↔ Mapper statement
- Mapper namespace ↔ DAO FQCN
- 조회 SQL ↔ count SQL 페이징 일관성

### 설정

- `tcf=false`일 때 TCF Bean 미로딩
- commons Security와 App Security 중복 없음
- local 외 환경에서 기본 JWT Secret 사용 금지

## 12.5 테스트 전략

| 단계 | 목적 |
|---|---|
| Unit | Service 계산/Mapping/Validation |
| Mapper | H2 기반 SQL 검증 |
| MVC | DTO binding/response envelope |
| Integration | Filter→Controller→DB 전체 |
| Security | local/non-local JWT |
| Smoke | 실제 URL 1건 호출 |
| Contract | UI Catalog ↔ Service endpoint |
| Architecture Test | 패키지/의존 규칙 |

## 12.6 Architecture Gap Backlog

| Gap | 우선도 | 조치 |
|---|---|---|
| UI `mgcoa*` vs Service `mkcoa*` | Critical | 대상 UI/Service 정합성 확정 |
| `pdmk-ui` 문서 vs `pdmg-ui` 소스 | High | 문서/소스 범위 확정 |
| Service `@Transactional` 미적용 | High | 조회/쓰기 TX 표준화 |
| README `bootWar` vs Gradle `war` | Medium | 빌드 정본 통일 |
| UI timeout property 미적용 | High | RestClient timeout 구성 |
| Security `permitAll` | High | 운영 인가 정책 |
| CORS `/api/**`와 업무 URL 차이 | Medium | 호출 방식 기준 재정의 |
| Dependency 버전 혼재 | Medium | BOM/버전 정합성 검증 |
| MessageCache 운영 테이블 설정 | Medium | 실제 DS/Table 기준 확정 |
| Filter 오류의 비표준 에러 envelope | Medium | 표준화 여부 ADR |

## 12.7 ADR 관리

중요 설계 의사결정은 다음 형식으로 관리한다.

```text
ADR-번호
제목
상태: Proposed / Accepted / Deprecated
배경
대안
결정
근거
영향
Rollback
관련 소스
관련 테스트
```

최우선 ADR 후보:

1. ADR-001: PDMK commons vs TCF 런타임 경로
2. ADR-002: UI 모듈 업무코드 정합성
3. ADR-003: Service 트랜잭션 표준
4. ADR-004: Filter 오류 응답 규격
5. ADR-005: 운영 Security/Authorization
6. ADR-006: WAR 배포 정본
7. ADR-007: Timeout 계층 정책

## 12.8 변경 영향 매트릭스

| 변경 | FW | Service | UI | DB | 문서/테스트 |
|---|---:|---:|---:|---:|---:|
| Header 필드 | ● | ● | ● | ImageLog 영향 | ● |
| Service ID | ○ | ● | ● | SQL ID 가능 | ● |
| DTO | ○ | ● | ● 샘플 | - | ● |
| DAO/Mapper | - | ● | - | ● | ● |
| JWT | ● | 설정 | 전달 | - | ● |
| Timeout | ● 가능 | ● | ● | ● 가능 | ● |
| Error 규격 | ● | ● | ● | - | ● |
| DB Schema | ImageLog 가능 | ● | - | ● | ● |

## 12.9 신규 거래 추가 절차

```text
[1] Service ID 확정
      ↓
[2] DTOin/out 정의
      ↓
[3] Controller URL/Method
      ↓
[4] Service 업무 규칙
      ↓
[5] DAO
      ↓
[6] Mapper XML / SQL
      ↓
[7] Transaction 설정
      ↓
[8] 전문 샘플
      ↓
[9] UI Catalog(해당 UI가 맞을 경우)
      ↓
[10] Smoke + Architecture 검증
```

### 완료 조건

- URL 호출 성공
- 응답 `{hdr_nhnis,dto}`
- Header의 `rms_svc_c` 일치
- GUID 로그 추적 가능
- SQL ID 확인 가능
- 예외 표준 확인
- 신규 코드가 레이어 의존규칙 준수
- UI를 사용할 경우 Catalog와 실제 endpoint 일치

## 12.10 변경관리

아키텍처 변경 시 다음 세 가지를 같은 변경 단위로 관리한다.

```text
소스
  +
아키텍처 정의서
  +
검증 테스트
```

소스만 변경하고 문서를 나중에 맞추는 방식은 금지한다. 문서가 실제 구현과 다르면 Source Evidence 기준으로 Gap을 즉시 등록한다.

---

# 부록

## A. 소스 디렉토리 맵

```text
pdmk-fw/
└── src/main/java/nhnis/fw/
    ├── commons/
    ├── tcf/
    └── exception/

pdmk-service/
├── src/main/java/nhnis/mk/
│   ├── config/
│   ├── common/
│   └── co/a/
│       ├── controller/
│       ├── service/
│       ├── dao/
│       └── dto/
└── src/main/resources/
    ├── rdw.mk.co.a/
    └── db/h2/

pdmg-ui/
├── src/main/java/nhnis/mg/ui/
│   ├── entry/
│   ├── application/
│   ├── client/
│   ├── config/
│   └── support/
└── src/main/resources/
    ├── sample-requests/
    └── static/
```

## B. 주요 클래스 인덱스

| 분류 | 클래스 |
|---|---|
| Boot | `PdmkApplication`, `PdmgUiApplication` |
| Filter | `DefaultFilter` |
| Interceptor | `ServicePreventionInterceptor` |
| Request | `RequestBodyArgumentResolver` |
| Response | `ResponseBodyArgumentResolver` |
| Context | `ServiceContext`, `ServiceContextHolder` |
| Security | `SecurityConfig`, `JwtProvider` |
| Aspect | `BizPrePostAspect` |
| DB | `RdwDataSourceConfig`, `RDWMapper` |
| SQL Log | `MybatisLogInterceptor` |
| ImageLog | `ImageLogHandler` |
| Error | `NhBaseException`, `NH_NIS_ERR_DTO` |
| UI | `TransactionCatalog`, `TransactionRelayService` |

## C. Service ID 카탈로그

### PDMK Service

| ID | Method | 기능 |
|---|---|---|
| `mkcoa5530S0` | POST | 안내항목 조회 |
| `mkcoa8888S0` | POST | ImageLog 조회 |
| `mkcoa8888D0` | POST | ImageLog 삭제 |
| `mkcoa9999S0` | POST | 영업팁 조회 |

### PDMG UI

| ID | Method | 비고 |
|---|---|---|
| `mgcoa5530S0` | POST | 제공된 PDMK Service와 불일치 |
| `mgcoa8888S0` | POST | 동일 |
| `mgcoa8888D0` | POST | 동일 |
| `mgcoa9999S0` | POST | 동일 |

## D. 핵심 설정키

| 키 | 현재 |
|---|---|
| `spring.profiles.active` | local |
| `spring.application.name` | pdmk |
| `nhnis.fw.tcf.enabled` | false |
| `nhnis.fw.commons.legacy-web.enabled` | true |
| `nhnis.fw.commons.filter.enabled` | true |
| `spring.datasource.rdw.*` | H2 Local |
| `jwt.secret` | 환경변수 fallback |
| `pdmg.ui.target-base-url` | localhost:8080 |
| `pdmg.ui.timeout-ms` | 10000 |

## E. 아키텍처 핵심 요약

```text
[현행 PDMK Service]

POST /mkcoa*
    ↓
DefaultFilter
    ↓
Security
    ↓
ServicePreventionInterceptor
    ↓
BizPrePostAspect
    ↓
Controller
    ↓
Service
    ↓
DAO / Mapper
    ↓
RDW

응답
    ↓
ResponseBodyArgumentResolver
    ↓
{ hdr_nhnis, dto }

[현재 중요한 보완]

1. UI/Service 업무코드 정합성
2. Service @Transactional
3. WAR 빌드 절차 정본
4. Timeout 실제 적용
5. 운영 인가 정책
6. 문서-소스 자동 정합성 검증
```

---

# 최종 아키텍처 판단

현재 PDMK 소스는 **공통 FW와 업무 Service를 분리하고, Service ID 중심 전문, 시스템/업무 선후처리, MyBatis RDW 접근, GUID 기반 추적을 구현한 전형적인 대형 SI 정보계 온라인 거래 구조**로 정리할 수 있다.

특히 다음 구조는 명확하다.

- `pdmk-fw` = 공통 런타임
- `pdmk-service` = MK 업무 구현
- `Controller → Service → DAO → Mapper`
- `hdr_nhnis + dto`
- `DefaultFilter → Interceptor → Aspect`
- `GUID → MDC → ImageLog → SQL ID`

반면 이번 소스 세트에서 가장 먼저 해결해야 할 것은 기능 추가가 아니라 **정합성**이다.

1. `pdmg-ui/mgcoa*`와 `pdmk-service/mkcoa*`의 계약을 확정한다.
2. Service 트랜잭션 선언을 실제 코드에 적용한다.
3. 빌드/배포 정본을 `war` 또는 `bootWar` 중 하나로 통일한다.
4. UI/HTTP/DB Timeout을 실제 코드에 적용한다.
5. 운영 Security를 `permitAll` 샘플 수준에서 역할 기반 정책으로 확장한다.
6. 아키텍처 규칙을 CI에서 자동 검증한다.

이 여섯 항목을 정리하면 본 문서는 단순한 “소스 설명서”가 아니라 **개발 표준 + 운영 기준 + 변경 통제 기준**으로 사용할 수 있다.
