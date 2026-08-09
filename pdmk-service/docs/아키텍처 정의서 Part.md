# PDMK 아키텍처 정의서

| 항목 | 내용 |
| ---- | ---- |
| **역할** | **읽기·통제 정본** → 이관됨: [PDMK_아키텍처_정의서.md](./PDMK_아키텍처_정의서.md) (v2.0) |
| 대상 스택 | `pdmk-fw` · `pdmk-service` · `pdmk-ui` |
| 기준일 | 2026-08-08 |
| 상태 | **참고(Part 골격 원천)** — 신규 읽기는 정본 v2.0 |
| 입문 요약 | [아키텍처 정의서.md](./아키텍처%20정의서.md) (왜→검증 9단) |
| 상세 백과 | [통합 아키텍처 정의서 copy.md](./통합%20아키텍처%20정의서%20copy.md) |
| 도해 요약 | [표 아키텍처 정의서.md](./표%20아키텍처%20정의서.md) |
| 네이밍 | [네이밍원칙.md](./네이밍원칙.md) · [MK-NAMING_CONVENTION.md](./MK-NAMING_CONVENTION.md) |

> **정본은 [PDMK_아키텍처_정의서.md](./PDMK_아키텍처_정의서.md) 로 이관되었다.** 본 Part 문서는 골격·이력 참고용이다.  
> Part 0(기준선) → XI(검증·거버넌스) 순 골격은 정본 v2.0에 흡수되었다.  
> 표기: **[AS-IS]** · **[TO-BE]** · **[Evidence]** · **[권고]** · **[Gap]**

### 문서 체계

| 우선 | 문서 | 용도 |
| ---- | ---- | ---- |
| **1 정본** | [PDMK_아키텍처_정의서.md](./PDMK_아키텍처_정의서.md) | v2.0 통합 정본 |
| 2 입문 | [아키텍처 정의서.md](./아키텍처%20정의서.md) | 왜→검증 빠른 한 바퀴 |
| 3 상세 | [통합 … copy.md](./통합%20아키텍처%20정의서%20copy.md) | 절별 장문·체크리스트 |
| 4 도해 | [표 아키텍처 정의서.md](./표%20아키텍처%20정의서.md) | 표·TEXT 표 그림 |
| 참고 | **본 문서 (Part)** | Part 골격 원천 |

---

## 목차

| Part | 장 | 제목 |
| ---- | -- | ---- |
| **0** | [1](#1-문서-개요와-아키텍처-기준선) | 문서 개요와 아키텍처 기준선 |
| **I** | [2](#2-아키텍처-목표와-설계-원칙) | 아키텍처 목표와 설계 원칙 |
| **II** | [3](#3-통합-시스템-아키텍처) | 통합 시스템 아키텍처 |
| **III** | [4](#4-모듈-아키텍처) | 모듈 아키텍처 |
| **IV** | [5](#5-런타임-거래-아키텍처) | 런타임 거래 아키텍처 |
| **V** | [6](#6-서비스전문-계약-아키텍처) | 서비스·전문 계약 아키텍처 |
| **VI** | [7](#7-업무-애플리케이션-아키텍처) | 업무 애플리케이션 아키텍처 |
| **VII** | [8](#8-데이터트랜잭션-아키텍처) | 데이터·트랜잭션 아키텍처 |
| **VIII** | [9](#9-공통횡단-아키텍처) | 공통·횡단 아키텍처 |
| **IX** | [10](#10-플랫폼환경-아키텍처) | 플랫폼·환경 아키텍처 |
| **X** | [11](#11-배포운영-아키텍처) | 배포·운영 아키텍처 |
| **XI** | [12](#12-아키텍처-검증거버넌스) | 아키텍처 검증·거버넌스 |
| 부록 | [A~I](#부록) | 디렉토리·카탈로그·Gap·ADR |

```text
 Part0 기준선 → PartI 원칙 → PartII Big Picture
      → PartIII 모듈 → PartIV 런타임 → PartV 계약
      → PartVI 업무앱 → PartVII 데이터/TX
      → PartVIII 횡단 → PartIX 플랫폼 → PartX 운영
      → PartXI 검증·거버넌스 → 부록
```

---

# Part 0. 문서 개요와 아키텍처 기준선

## 1. 문서 개요와 아키텍처 기준선

### 1.1 문서 목적

이 문서는 Part 0~XI 골격의 **원천(참고)** 이다. 읽기·통제 정본은 [PDMK_아키텍처_정의서.md](./PDMK_아키텍처_정의서.md) 이다.

| 정의하는 것 | 정의하지 않는 것 |
| ----------- | ---------------- |
| 모듈 경계·런타임·계약·데이터·횡단·플랫폼·운영·검증 | 개별 업무 도메인 정책의 상세 설계서 |
| AS-IS와 TO-BE 구분 | 미검증 추측을 Evidence로 위장 |
| Source Evidence 기반 서술 | PDMG 등 **별도 스택**의 전체 정의 (필요 시 교차만) |

### 1.2 대상 시스템 및 범위

```text
 ┌─ 범위 안 ─────────────────────────────────────┐
 │  pdmk-ui  ·  pdmk-service  ·  pdmk-fw(commons) │
 │  RDW(H2/Oracle) · 서비스ID 거래 · hdr+dto      │
 └──────────────────────┬────────────────────────┘
                        │ 범위 밖/참고
           ┌────────────┴────────────┐
           ▼                         ▼
    nhnis.fw.tcf.* (잔존·OFF)   pdmg-* / PDMP 등
```

| 구성 | 역할 |
| ---- | ---- |
| FW | 공통 런타임(Filter·Interceptor·헤더·에러·ImageLog) |
| Service | 업무 API · TX · DAO |
| UI | 전문 테스트·릴레이 |
| DB | RDW 스키마 · `TB_*` |

### 1.3 분석 대상 소스

| 모듈 | 경로 | 비고 |
| ---- | ---- | ---- |
| `pdmk-fw` | `pdmk-fw/` | commons ★ · tcf OFF |
| `pdmk-service` | `pdmk-service/` | 업무 샘플 `nhnis.mk.co.a.*` |
| `pdmk-ui` | `pdmk-ui/` | Catalog · Relay · static |

> TOC 초안의 `pdmg-ui`는 **형제 제품(PDMG)** 표기로 보인다. 본 Part 문서의 1차 분석 대상은 **`pdmk-ui`** 다. PDMG와의 ID 정합성은 [§6.10](#610-ui-service-계약-정합성).

### 1.4 Source Evidence 원칙

| 구분 | 의미 | 표기 |
| ---- | ---- | ---- |
| **구현(AS-IS)** | 소스·설정에 존재 | `[Evidence]` 경로/클래스 |
| **설계** | 문서상 합의된 구조 | 본문 서술 |
| **권고(TO-BE)** | 아직 미구현·부분 구현 | `[TO-BE]` / `[권고]` |

규칙:

1. 구현 주장은 Evidence 없이 “확정”하지 않는다.  
2. 갭은 숨기지 않고 [부록 H](#부록-h-architecture-gap-목록)에 올린다.  
3. 권고와 현행을 한 문장에 섞지 않는다.

### 1.5 AS-IS / TO-BE 표기 기준

| 표기 | 사용 |
| ---- | ---- |
| **[AS-IS]** | 현재 샘플/설정이 그렇게 동작 |
| **[TO-BE]** | 목표 구조·운영 강화 |
| **[Gap]** | AS-IS ≠ TO-BE |

예: Security `permitAll` = **[AS-IS 샘플]** / URL별 인가 = **[TO-BE]**.

### 1.6 문서 읽는 방법

| 역할 | 권장 경로 |
| ---- | --------- |
| 신규 개발자 | §1 → §3 → §4 → §5 → §6 → §7 |
| 업무 개발 | §6 → §7 → §8 → §12.9 |
| AA / 아키텍트 | §2 → §3 → §12 → 부록 H·I |
| FW 담당 | §4.1~4.3 → §5 → §9 |
| 운영 | §10 → §11 → §9.5~9.8 |
| 리뷰어 | §12 · 부록 H |

---

# Part I. 아키텍처 목표와 설계 원칙

## 2. 아키텍처 목표와 설계 원칙

### 2.1 아키텍처 비전

**서비스 ID 중심 정보계(온라인) 서비스 스택**  
한 거래(`mkcoa5530S0` 등)가 URL·메서드·DTO·SQL·카탈로그·감사(GUID)를 관통하는 구조를 목표로 한다.

```text
 서비스ID ──┬── URL POST /{id}
            ├── Controller/Service 메서드
            ├── DTOin/out
            ├── SQL ID
            ├── UI Catalog.id
            └── rms_svc_c · ImageLog.SERVICE_ID
```

### 2.2 핵심 아키텍처 목표

| 목표 | 설명 |
| ---- | ---- |
| 표준화 | 전문·네이밍·페이징·에러 형식 통일 |
| 재사용 | commons 파이프라인·헤더·ImageLog |
| 추적성 | GUID · MDC · ImageLog · SQL ID |
| 운영성 | 로컬 H2 ↔ 폐쇄망 Oracle/WAS 전환 가능 |

### 2.3 설계 제약사항

| 제약 | [AS-IS] |
| ---- | ------- |
| 언어/런타임 | Java 21 |
| 프레임워크 | Spring Boot 3.5.x |
| 영속화 | MyBatis · Hikari · RDW |
| DB | local H2(Oracle mode) / 폐쇄망 Oracle |
| 배포 | service WAR · ui Boot JAR · fw JAR |
| WAS | 외부 Tomcat 전제(운영) |
| 인증 | commons JWT (비-local) |

### 2.4 품질속성

| 속성 | 현재 초점 |
| ---- | --------- |
| 성능 | 페이징·TX timeout·SQL 로그 |
| 가용성 | 단일 프로세스 샘플 · 컨테이너 없음[Gap] |
| 보안 | Filter JWT · Security permitAll 샘플[Gap] |
| 확장성 | 서비스ID 단위 거래 추가 |
| 운영성 | GUID 추적 · 프로파일 전환 |
| 유지보수성 | 레이어·모듈 경계 · CONVENTION |

### 2.5 핵심 설계원칙

| # | 원칙 |
| - | ---- |
| 1 | 세 모듈 역할 분리 (ui≠fw, 업무≠공통) |
| 2 | commons ON · TCF OFF |
| 3 | 서비스 ID = URL = 메서드 |
| 4 | 전문 = `hdr_nhnis` + `dto` |
| 5 | TX = Service `@Transactional` |
| 6 | DAO = Map · 메서드=SQL ID |
| 7 | 변경 시 FW/Service/UI 영향 동시 검토 |

### 2.6 핵심 아키텍처 의사결정 요약

| ADR | 결정 | 상태 |
| --- | ---- | ---- |
| [ADR-001](#부록-i-adr-목록) | commons 사용 · TCF 비활성 | Accepted |
| [ADR-002](#부록-i-adr-목록) | 서비스 ID URL 계약 | Accepted |
| [ADR-003](#부록-i-adr-목록) | TX 경계를 Service에 둠 | Accepted |
| [ADR-004](#부록-i-adr-목록) | ui는 fw 미의존 · HTTP만 | Accepted |
| [ADR-005](#부록-i-adr-목록) | ImageLog와 업무 TX 분리 | Accepted |

---

# Part II. 통합 시스템 아키텍처

## 3. 통합 시스템 아키텍처

### 3.1 시스템 컨텍스트

```text
 [Browser/개발자]
        │
        ▼
   pdmk-ui:8090
        │ HTTP POST /{서비스ID}
        ▼
   pdmk-service:8080 ──► pdmk-fw(commons)
        │
        ▼
   RDW (H2 / Oracle)
        │
        ├─ (옵션) Message 번들 테이블
        └─ (참고) APIGW/FOS 연동 설정은 fw에 존재·샘플 미필수
```

### 3.2 통합 논리 아키텍처

```text
 Presentation   pdmk-ui · Controller
 Application    Service · BizAspect · @Transactional
 Framework      pdmk-fw commons (Filter·Interceptor·Resolver)
 Data           DAO · MyBatis · RDW TB_*
```

### 3.3 통합 물리 아키텍처

| 프로세스 | 포트 | 산출 |
| -------- | ---- | ---- |
| pdmk-ui | 8090 | Boot JAR |
| pdmk-service(+fw) | 8080 | WAR (local bootRun 가능) |
| RDW | DB 포트 | H2 mem / Oracle |

### 3.4 모듈 의존관계

```text
 pdmk-ui ──HTTP──► pdmk-service ──Gradle impl──► pdmk-fw
    │                   │
    └──── fw 의존 없음 ─┘──── MyBatis/JDBC ──► RDW
```

### 3.5 모듈 책임 매트릭스

| 관심사 | fw | service | ui |
| ------ | -- | ------- | -- |
| Filter/JWT/Context | ● | ○ | — |
| Interceptor/ImageLog | ● | — | — |
| BizAspect | — | ● | — |
| C/S/DAO/DTO | — | ● | — |
| Security 샘플 | ○ OFF | ● | — |
| Catalog/Relay | — | — | ● |

### 3.6 배포 단위

| 모듈 | 산출 | 비고 |
| ---- | ---- | ---- |
| pdmk-fw | JAR | bootJar off |
| pdmk-service | WAR | bootWar off · WAS |
| pdmk-ui | Boot JAR | `pdmk-ui.jar` |

---

# Part III. 모듈 아키텍처

## 4. 모듈 아키텍처

### 4.1 `pdmk-fw` 아키텍처

```text
 pdmk-fw
  ├─ nhnis.fw.commons.*     ★ 사용
  │    filter / interceptor / resolver / dto.header
  │    exception / context / imagelog / message / util
  ├─ nhnis.fw.tcf.*         소스 잔존 · enabled=false
  └─ configuration · defaults yml (샘플)
```

**[Evidence]** `pdmk-fw/src/main/java/nhnis/fw/`

### 4.2 commons 런타임 구조

```text
 DefaultFilter
   → (Security)
   → ServicePreventionInterceptor
   → RequestBodyArgumentResolver  (dto만)
   → Controller …
   → ResponseBodyArgumentResolver (hdr+dto / 에러)
```

| 컴포넌트 | 역할 |
| -------- | ---- |
| `DefaultFilter` | Body·JWT·Context·MDC |
| `ServicePreventionInterceptor` | GUID·헤더보강·ImageLog |
| Resolver | 요청 dto / 응답 envelope |
| `ServiceContextHolder` | 요청 스코프 헤더 |

### 4.3 TCF 구조와 활성화 정책

| 항목 | [AS-IS] |
| ---- | ------- |
| `nhnis.fw.tcf.enabled` | `false` |
| TcfTraceFilter / JwtAuthenticationFilter / TCFAspect | 미활성 |
| 정책 | **commons만 사용 · TCF와 이중 경로 금지** |

### 4.4 `pdmk-service` 아키텍처

```text
 Controller → Service → DAO → Mapper XML → rdwDataSource
      ▲
 BizPrePostAspect (Controller pointcut)
 SecurityConfig (permitAll 샘플)
 RdwDataSourceConfig / RDWMapper
```

### 4.5 업무 패키지 구조

```text
 nhnis.mk.co.a
  ├─ controller  mkcoa{식별}Controller
  ├─ service     mkcoa{식별}Service
  ├─ dao         mkcoa{식별}DAO
  ├─ dto         *DTOin / *DTOout / *DTOSub*
  └─ common      BizPrePostAspect
 resources/rdw.mk.co.a/mkcoa{식별}-ORA.xml
```

### 4.6 `pdmk-ui` 아키텍처

```text
 nhnis.mk.ui
  ├─ PdmkUiApplication          entry
  ├─ entry.web                  Home / Api Controller
  ├─ application.service        TransactionCatalog
  ├─ client                     TransactionRelayService
  ├─ config                     PdmkUiProperties
  └─ support                    RelayResult · TransactionInfo
 static/ · sample-requests/
```

> TOC의 `pdmg-ui`와 구분: 본 스택 UI는 **`pdmk-ui`**.

### 4.7 UI Relay 구조

```text
 Browser → /api/relay/{서비스ID}
        → TransactionCatalog 조회
        → TransactionRelayService
        → RestClient → pdmk.ui.target-base-url + /{서비스ID}
```

**[Gap]** `pdmk.ui.timeout-ms` 설정은 있으나 RestClient 미연결.  
**[Gap]** `Authorization` 헤더 미전달.

### 4.8 모듈 간 금지 의존관계

| 금지 | 이유 |
| ---- | ---- |
| Controller → DAO | 레이어·TX 붕괴 |
| UI → fw / DAO | 경계 붕괴 |
| 업무 코드 → fw 패키지 내부 구현 의존 과다 | 공통 오염 |
| TCF ON + commons Filter 병행 | 이중 JWT/계약 |
| Spring `@RequestBody` (업무) | dto 노드 미추출 |

---

# Part IV. 런타임 거래 아키텍처

## 5. 런타임 거래 아키텍처

### 5.1 End-to-End 거래 흐름

```text
 Browser → UI(:8090) → Service(:8080)+FW → RDW → DTOout → UI → Browser
```

### 5.2 요청 진입 체인

```text
 DefaultFilter → SecurityFilterChain → DispatcherServlet
   → ServicePreventionInterceptor → BizPrePostAspect → Controller
```

### 5.3 `DefaultFilter`

**[Evidence]** `nhnis.fw.commons.filter.DefaultFilter`

| 처리 | 결과 |
| ---- | ---- |
| CachedBody | Resolver 재읽기 |
| JWT (비-local) | Bearer · 실패 401 |
| hdr_nhnis | Context · local 시 합성 |
| finally | Context/MDC clear |

### 5.4 `ServicePreventionInterceptor`

| 시점 | 처리 |
| ---- | ---- |
| preHandle | GUID 확정 · 헤더 보강 · ImageLog PRE |
| postHandle | ImageLog POST |
| afterCompletion | ImageLog EX · rethrow |

### 5.5 `BizPrePostAspect`

**[Evidence]** `nhnis.mk.co.common.BizPrePostAspect`  
Pointcut: `nhnis.mk.co..controller..*` · `@Order(100)`  
역할: 업무 선·후 로그·BRC (DB/TX/ImageLog 비담당)

### 5.6 Request Resolver

`RequestBodyArgumentResolver` — JSON root의 **`dto`만** DTOin으로 변환.  
애노테이션: `nhnis.fw.commons.resolver.RequestBody`.

### 5.7 Controller→Service→DAO

```text
 @PostMapping("/{서비스ID}")
 Controller → Service(@Transactional) → DAO(SQL ID) → XML
```

### 5.8 Response Resolver

`ResponseBodyArgumentResolver` — Context 헤더 + 반환객체를 `{hdr_nhnis, dto}`로 조립.  
예외 시 `NH_NIS_ERR_DTO`.

### 5.9 정상·예외 시퀀스

| 경로 | 흐름 |
| ---- | ---- |
| 성공 | … → DTOout → Advice envelope → ImageLog POST → finally |
| 업무/시스템 예외 | NhBaseException → TX rollback → NH_NIS_ERR_DTO → ImageLog EX → finally |
| Filter 실패 | sendError · **envelope 없음** |

### 5.10 Context·MDC 생명주기

```text
 Filter set → 전 구간 공유 → Filter finally clear
 ※ clear 누락 시 워커 재사용 GUID 누수
```

---

# Part V. 서비스·전문 계약 아키텍처

## 6. 서비스·전문 계약 아키텍처

### 6.1 서비스 ID 체계

```text
 [대구분2][업무2][세부1][식별4][구분자1][순번1]
    mk      co     a     5530      S       0
 → mkcoa5530S0
```

| 구분자 | 의미 |
| ------ | ---- |
| S/C/U/D/A/R | 조회/등록/수정/삭제/혼합/리포트 |

### 6.2 URL·메서드·ServiceId 관계

**Service ID = URL = Method = rms_svc_c**

| 위치 | 예 |
| ---- | -- |
| URL | `POST /mkcoa5530S0` |
| 메서드 | `mkcoa5530S0(...)` |
| Catalog | `mkcoa5530S0` |

### 6.3 네이밍 아키텍처

| 축 | 규칙 |
| -- | ---- |
| 패키지 | `nhnis.mk.{업무}.{세부}.{layer}` |
| 클래스 | 구분자 미포함 `mkcoa5530Controller` |
| DTO | `{서비스ID}DTOin/out/Sub*` |
| DAO/XML | `mkcoa{식별}DAO` · `rdw.mk…/mkcoa{식별}-ORA.xml` |
| SQL ID | = DAO 메서드명 |

정본: [MK-NAMING_CONVENTION.md](./MK-NAMING_CONVENTION.md)

### 6.4 요청 전문

```json
{ "hdr_nhnis": { "sys_comm": { } }, "dto": { } }
```

### 6.5 `sys_comm`

| 핵심 필드 | 용도 |
| --------- | ---- |
| `std_gbl_id` | GUID · MDC · ImageLog |
| `rms_svc_c` | 서비스 ID |
| `scid` | 화면/프로그램 ID |
| `tr_trm_ipadr` | 단말 IP |
| `optr_eno` | 조작자 |

### 6.6 DTO 구조

```text
 {서비스ID}DTOin / DTOout / DTOSub{n}
```

### 6.7 요청·응답 Binding

| 방향 | 처리 |
| ---- | ---- |
| 요청 | Filter(hdr) + Resolver(dto→Java) |
| 응답 | Advice(Java→hdr+dto JSON) |

### 6.8 Validation

| [AS-IS] | [TO-BE/권고] |
| ------- | ------------ |
| Filter: Body/hdr/JWT 검증 | Bean Validation·업무 필수값 표준화 |
| Service 내 조건 처리 | 오류코드 일관 매핑 |

### 6.9 페이징 계약

| 필드 | 규칙 |
| ---- | ---- |
| pageNo / pageSize | 기본 20 · 상한 100 · Service normalize |
| totalCount / totalPages / size | DTOout |
| SQL | count + ROWNUM |

### 6.10 UI-Service 계약 정합성

| 항목 | [AS-IS] |
| ---- | ------- |
| pdmk-ui Catalog | `mkcoa5530S0` · `8888S0/D0` · `9999S0` |
| pdmk-service | 동일 `mkcoa*` Endpoint |
| PDMG(`mgcoa*` 등) | **별도 스택** — ID prefix 혼용 금지 |
| Gap | UI Bearer 미전달 · timeout 미연결 · 9999 페이징 없음 |

```text
 [정합] mkcoa* (UI Catalog)  ==  mkcoa* (Service @PostMapping)
 [금지] mgcoa* UI ↔ mkcoa* Service 임의 혼선
```

---

# Part VI. 업무 애플리케이션 아키텍처

## 7. 업무 애플리케이션 아키텍처

### 7.1 Controller 설계

| 책임 | 비책임 |
| ---- | ------ |
| `@PostMapping` · DTOin 수신 · Service 위임 | SQL · TX · 비즈니스 규칙 |

### 7.2 Service 설계

| 책임 | 비책임 |
| ---- | ------ |
| 업무 규칙 · Map 변환 · 페이징 · `@Transactional` | HTTP · XML SQL 문자열 직접 |

### 7.3 DAO 설계

`@RDWMapper` 인터페이스 · 메서드=SQL ID · Map 입출력.

### 7.4 Mapper XML 설계

`namespace`=DAO FQCN · `id`=메서드 · `/* SQL ID */` 주석 · `rdw.mk.co.a/`.

### 7.5 DTO·Map 변환

Service가 DTOin→Map, Map행→DTOSub/DTOout. DAO에 DTO 시그니처 금지.

### 7.6 조회 패턴 (S0)

```text
 count SQL → list SQL(ROWNUM) → DTOout(Sub[], paging meta)
```

### 7.7 등록/수정/삭제 패턴 (C/U/D)

쓰기 Service에 `@Transactional` · 영향 행 수/결과코드 반환.  
샘플: `mkcoa8888D0` 삭제.

### 7.8 샘플 거래 분석

| 서비스 | 내용 | 페이징 |
| ------ | ---- | ------ |
| `mkcoa5530S0` | 안내항목 `TB_MK_CO_A_5530` | ○ |
| `mkcoa8888S0`/`D0` | ImageLog 조회/삭제 | ○ / — |
| `mkcoa9999S0` | 영업팁 | ❌ [Gap] |

---

# Part VII. 데이터·트랜잭션 아키텍처

## 8. 데이터·트랜잭션 아키텍처

### 8.1 데이터 접근 구조

```text
 Service → DAO → Mapper XML → rdwSqlSessionTemplate → rdwDataSource → RDW
```

### 8.2 RDW DataSource

**[Evidence]** `RdwDataSourceConfig`  
`rdwDataSource`(@Primary) · `rdwSqlSessionFactory`(+MybatisLog) · `rdwTransactionManager`

### 8.3 H2 ↔ Oracle 환경

| 환경 | DS | sql.init |
| ---- | -- | -------- |
| local | H2 mem MODE=Oracle | always |
| 폐쇄망 | Oracle | off · DBA 스키마 |

### 8.4 트랜잭션 경계

**Service `@Transactional`만.** Controller/DAO 금지.

### 8.5 Commit/Rollback

정상 커밋 · 런타임 예외(및 정책에 따른 예외) 롤백.  
`NhBaseException` 경유 시 TX Proxy가 롤백.

### 8.6 ImageLog TX 경계

```text
 ImageLog JDBC (Interceptor)  ≠  업무 @Transactional
 ImageLog 실패 → 삼킴 (업무 예외 미전파)
```

### 8.7 테이블 아키텍처

| 테이블 | 접근 |
| ------ | ---- |
| `TB_FW_IMAGE_LOG` | fw ImageLog + mkcoa8888 |
| `TB_MK_CO_A_5530` | mkcoa5530 |
| `TB_CR_AH_SALES_TIP_RACT` | mkcoa9999 |

### 8.8 SQL 표준

SQL ID · namespace FQCN · ROWNUM 페이징 · Alias T1… · `#{…}`

### 8.9 데이터 책임·금지사항

| 금지 |
| ---- |
| Controller SQL |
| DAO에 DTO |
| ui→DB |
| 앱이 운영 DDL 생성 |

---

# Part VIII. 공통·횡단 아키텍처

## 9. 공통·횡단 아키텍처

### 9.1 예외·에러 아키텍처

```text
 NhBaseException → Advice → NH_NIS_ERR_DTO
 Filter 실패 → sendError (envelope 없음)
 업무 분기 → DTOout 결과코드
```

### 9.2 메시지 아키텍처

`exceptionCode.yml` · (옵션) `MessageCache`/`MessageLoader` · MessageFormat.

### 9.3 인증·인가 아키텍처

| 층 | [AS-IS] | [TO-BE] |
| -- | ------- | ------- |
| Filter JWT | 비-local Bearer | 유지 |
| Security | permitAll | URL/역할 인가 |
| fw Security | OFF | 앱과 이중 ON 금지 |

### 9.4 CORS·Relay 보안

`spring.mvc.cors` · UI→Service 릴레이 시 **Authorization 전달 [Gap→TO-BE]**.

### 9.5 로그·추적 아키텍처

MDC(guid/ip/userId/serviceId) · `PdmkTxLog` system/biz.

### 9.6 ImageLog

PRE INSERT · POST UPDATE · EX UPDATE → `TB_FW_IMAGE_LOG`.

### 9.7 SQL·성능 로그

`MybatisLogInterceptor` · MDC `sqlId`.

### 9.8 Timeout

| 계층 | [AS-IS] |
| ---- | ------- |
| UI `timeout-ms` | 설정만 · RestClient 미연결 [Gap] |
| `@Transactional(timeout)` | 초 · 권장 |
| JWT expiration | ms |
| `ttl_ug_ync` | TX 미연동 |
| APIGW/FOS | fw 설정 존재(연동 시) |

### 9.9 Cache

요청 Body 버퍼 · MessageCache(옵션) · UI Catalog · Redis/Spring Cache **미사용**.

### 9.10 외부연동

APIGW/FOS timeout 등 fw 설정 네임스페이스 참고. 샘플 거래의 필수 경로 아님.

---

# Part IX. 플랫폼·환경 아키텍처

## 10. 플랫폼·환경 아키텍처

### 10.1 Spring Boot 구성

| 모듈 | Main | scan |
| ---- | ---- | ---- |
| service | `PdmkApplication` | `nhnis` |
| ui | `PdmkUiApplication` | ui 패키지 |
| fw | 없음 | 편입 |

### 10.2 Configuration 구조

`@Configuration` · `@ConditionalOnProperty` (filter/legacy-web/tcf/security).

### 10.3 프로파일·설정 계층

```text
 env/시크릿 → profiles.active → application.yml → exceptionCode.yml
```

local: H2·JWT 스킵·hdr 합성.

### 10.4 설정 키 표준

| 네임스페이스 | 예 |
| ------------ | -- |
| `nhnis.fw.*` | `tcf.enabled` · `commons.filter/legacy-web/security` |
| `jwt.*` | secret · expiration |
| `spring.datasource.rdw.*` | JDBC |
| `framework.message.*` | MessageCache |
| `pdmk.ui.*` | target-base-url · timeout-ms |
| `nhnis.exception.*` | exceptionCode.yml |

### 10.5 Gradle 프로젝트 구조

```text
 pdmk-fw/  ·  pdmk-service/(include fw)  ·  pdmk-ui/
```

### 10.6 의존성 관리

service → fw (implementation). ui → fw **없음**.

### 10.7 Java/Spring/Gradle 버전

| 항목 | [AS-IS] |
| ---- | ------- |
| JDK | 21 |
| Gradle Wrapper | 8.10.1 |
| Boot plugin | 3.5.14 |
| fw 스타터 핀 | 상이 가능 → 정렬 주의 |

### 10.8 폐쇄망 Nexus

local Central / 폐쇄망 Nexus 전환. 운영에 Central-only 금지.

### 10.9 인코딩·개발환경

UTF-8 · Windows 개발 가능 · Eclipse `.settings`는 산출물에 커밋하지 않음(권고).

---

# Part X. 배포·운영 아키텍처

## 11. 배포·운영 아키텍처

### 11.1 산출물

fw JAR · service WAR · ui Boot JAR.

### 11.2 Tomcat 배포

service WAR → 외부 WAS. **[Gap]** 컨테이너/K8s 표준 없음.

### 11.3 Local BootRun

`RUN.bat` / `gradlew bootRun` · H2.

### 11.4 기동 순서

```text
 [1] fw 빌드 → [2] service:8080 → [3] ui:8090
```

### 11.5 환경 전환

H2→Oracle · Central→Nexus · JWT ON · sql.init off · hdr 필수.

### 11.6 장애 추적

```text
 UI? → 401? → 전문? → 에러코드? → SQL? → 페이징? → TX? → Bean?
```

### 11.7 운영 로그

GUID 중심: Interceptor → Aspect → C/S → ImageLog → SQL.

### 11.8 성능·Timeout 모니터링

병목 후보: UI 릴레이 · Filter/JWT · SQL · TX timeout · 9999 전량 조회[Gap].

---

# Part XI. 아키텍처 검증·거버넌스

## 12. 아키텍처 검증·거버넌스

### 12.1 아키텍처 준수 규칙

| 등급 | 예 |
| ---- | -- |
| **Must** | 서비스ID=URL · hdr+dto · TX=Service · tcf=false · ui≠fw |
| **Should** | 모든 DB Service에 `@Transactional` 명시 · 목록 페이징 |
| **Must Not** | C→DAO · Spring `@RequestBody` · TCF/commons 이중 경로 |

### 12.2 Source Evidence

주장마다 클래스·yml·XML·Endpoint를 명시한다 (본문 `[Evidence]` · 부록 B~E).

### 12.3 아키텍처 정합성 검증

| 축 | 확인 |
| -- | ---- |
| UI Catalog.id | = Service `@PostMapping` |
| rms_svc_c | = 서비스 ID |
| DAO 메서드 | = XML id |
| scid | 식별 단위 |

### 12.4 자동 검증

| [TO-BE] | 내용 |
| ------- | ---- |
| Naming | CONVENTION 린트 |
| Dependency | ui→fw 금지 검출 |
| Endpoint | Catalog ↔ Controller 목록 diff |

### 12.5 테스트 전략

| 수준 | [AS-IS]/[권고] |
| ---- | --------------- |
| Unit | Service/매핑 | 확대 |
| Integration | local H2 거래 | 페이징·TX 시나리오 |
| Smoke | UI relay → 샘플 3종 | CI |

### 12.6 Architecture Gap

요약은 [부록 H](#부록-h-architecture-gap-목록). 상세 copy §26.0.

### 12.7 ADR

중요 결정은 [부록 I](#부록-i-adr-목록)에 상태·근거와 함께 관리.

### 12.8 변경 영향 분석

| 변경 | fw | service | ui |
| ---- | -- | ------- | -- |
| 서비스ID/DTO/DAO | — | ● | ● |
| 헤더 | ● | ● | ● |
| Filter/JWT | ● | ○ | ○ |
| 테이블 | ○ | ● | — |
| 페이징 | — | ● | ● |

### 12.9 신규 거래 추가 절차

```text
 ① 네이밍 → ② DTO → ③ C/S/DAO/XML
 → ④ 페이징·TX·테이블 → ⑤ UI Catalog/샘플/화면
 → ⑥ fw는 공통 필요 시에만
```

### 12.10 변경관리·Rollback

1. 문서(본 Part본 · copy/표)와 소스 동시 갱신  
2. Gap/ADR 상태 갱신  
3. 배포 단위별 롤백(WAR/JAR) · DB 변경은 DBA 절차

---

# 부록

## 부록 A. 소스 디렉토리 맵

```text
 pdmk-fw/src/main/java/nhnis/fw/{commons,tcf}/…
 pdmk-service/src/main/java/nhnis/mk/{co/a/…,config}/…
 pdmk-service/src/main/resources/{rdw.mk.co.a,db/h2,application.yml,exceptionCode.yml}
 pdmk-ui/src/main/java/nhnis/mk/ui/{entry,application,client,config,support}/…
 pdmk-ui/src/main/resources/{static,sample-requests,application.yml}
```

## 부록 B. 패키지·클래스 인덱스

| 구분 | 클래스 |
| ---- | ------ |
| Filter | `DefaultFilter` · `CachedBodyHttpServletRequest` |
| Interceptor | `ServicePreventionInterceptor` |
| Aspect | `BizPrePostAspect` |
| Resolver | `RequestBodyArgumentResolver` · `ResponseBodyArgumentResolver` |
| 예외 | `NhBaseException` |
| ImageLog | `ImageLogHandler` |
| UI | `TransactionCatalog` · `TransactionRelayService` |
| 샘플 | `mkcoa5530/8888/9999` Controller·Service·DAO |

## 부록 C. 서비스 ID/API 카탈로그

| 서비스 ID | Method/URL | 프로그램 |
| --------- | ---------- | -------- |
| `mkcoa5530S0` | POST `/mkcoa5530S0` | 안내항목 |
| `mkcoa8888S0` | POST `/mkcoa8888S0` | ImageLog 조회 |
| `mkcoa8888D0` | POST `/mkcoa8888D0` | ImageLog 삭제 |
| `mkcoa9999S0` | POST `/mkcoa9999S0` | 영업팁 |
| UI relay | POST `/api/relay/{id}` | 중계 전용 |

## 부록 D. 설정 Key 카탈로그

| Key | 요지 |
| --- | ---- |
| `nhnis.fw.tcf.enabled` | TCF OFF |
| `nhnis.fw.commons.filter.enabled` | DefaultFilter |
| `nhnis.fw.commons.legacy-web.enabled` | Interceptor·Resolver·Aspect |
| `nhnis.fw.commons.security.enabled` | fw Security (기본 false) |
| `jwt.*` | secret · expiration |
| `spring.datasource.rdw.*` | RDW |
| `spring.sql.init.*` | local DDL |
| `framework.message.*` | MessageCache |
| `pdmk.ui.target-base-url` | 릴레이 대상 |
| `pdmk.ui.timeout-ms` | [Gap] RestClient 미연결 |
| `nhnis.exception.*` | exceptionCode.yml |

## 부록 E. 테이블·DAO·Mapper·SQL ID

| 서비스 | DAO | XML | SQL ID | 테이블 |
| ------ | --- | --- | ------ | ------ |
| 8888 | `mkcoa8888DAO` | `mkcoa8888-ORA.xml` | `…S0_S0`(+count), `…D0_D0` | `TB_FW_IMAGE_LOG` |
| 5530 | `mkcoa5530DAO` | `mkcoa5530-ORA.xml` | `…S0_S0`(+count) | `TB_MK_CO_A_5530` |
| 9999 | `mkcoa9999DAO` | `mkcoa9999-ORA.xml` | `…S0_S0`(+count) | `TB_CR_AH_SALES_TIP_RACT` |

## 부록 F. 요청·응답 전문 샘플

요청(요약):

```json
{
  "hdr_nhnis": { "sys_comm": { "rms_svc_c": "mkcoa5530S0", "scid": "mkcoa5530" } },
  "dto": { "pageNo": 1, "pageSize": 20 }
}
```

응답(요약): `{ "hdr_nhnis": {…}, "dto": { "mkcoa5530S0DTOSub0": [], "size": 0, "pageNo": 1, … } }`  
샘플 파일: `pdmk-ui/src/main/resources/sample-requests/*.json`

## 부록 G. 용어·약어

| 용어 | 의미 |
| ---- | ---- |
| PDMK | pdmk-fw+service+ui |
| commons / TCF | 실사용 공통 / 비활성 레거시 경로 |
| RDW | 데이터소스·스키마 |
| ImageLog | GUID 감사 로그 |
| AS-IS / TO-BE | 현행 / 목표 |
| Evidence | 소스·설정 근거 |

## 부록 H. Architecture Gap 목록

| ID | Gap | 상태 |
| -- | --- | ---- |
| G-01 | mkcoa9999 페이징 없음 | Open |
| G-02 | UI timeout-ms ↔ RestClient 미연결 | Open |
| G-03 | UI Authorization 미전달 | Open |
| G-04 | Security permitAll 샘플 | Open |
| G-05 | 일부 `@Transactional` 미명시 | Open |
| G-06 | TCF 소스 잔존(런타임 OFF) | Accepted risk |
| G-07 | 컨테이너/K8s 없음 | Out of scope |

## 부록 I. ADR 목록

| ID | 결정 | 상태 | 근거 |
| -- | ---- | ---- | ---- |
| ADR-001 | commons ON · TCF OFF | Accepted | 계약·Filter 단일화 |
| ADR-002 | 서비스 ID = URL = 메서드 | Accepted | 추적·카탈로그·SQL 연결 |
| ADR-003 | TX = Service | Accepted | 레이어 경계 |
| ADR-004 | ui → HTTP only · fw 미의존 | Accepted | 모듈 분리 |
| ADR-005 | ImageLog ≠ 업무 TX | Accepted | 감사 실패 격리 |

---

## 문서 관계

| 문서 | 역할 |
| ---- | ---- |
| [PDMK_아키텍처_정의서.md](./PDMK_아키텍처_정의서.md) | **정본 v2.0** |
| **본 문서** | Part 골격 참고 |
| [아키텍처 정의서.md](./아키텍처%20정의서.md) | 입문 요약 (왜→검증) |
| [통합 아키텍처 정의서 copy.md](./통합%20아키텍처%20정의서%20copy.md) | 절별 장문 상세 |
| [표 아키텍처 정의서.md](./표%20아키텍처%20정의서.md) | 표·TEXT 그림 요약 |

---

*끝 — Part 골격 참고 (정본은 PDMK_아키텍처_정의서.md)*
