# PDMK 아키텍처 정의서

| 항목 | 내용 |
| ---- | ---- |
| **역할** | **읽기·통제 정본** (이 문서 1본) |
| 버전 | **v2.0** (docs 하위 아키텍처 문서 통합 정리본) |
| 기준일 | 2026-08-08 |
| 대상 | `pdmk-fw` · `pdmk-service` · `pdmk-ui` (+ 형제 `pdmg-ui` 계약 Gap) |
| 원칙 | **소스 우선(Source Evidence)**. 문서≠소스이면 AS-IS=소스, 차이는 Gap |
| 표기 | **[AS-IS]** 현행 · **[TO-BE]** 목표 · **[Evidence]** 근거 · **[Gap]** 불일치 · **[권고]** |

> 본 문서는 `pdmk-service/docs`에 흩어진 아키텍처 정의서들의 **강점만 모아 새로 쓴 정본**이다.  
> 이전 문서들은 참고·입문·도해·상세 백과로 남긴다 (하단 [문서 체계](#0-문서-체계)).  
> **모든 Part·절·부록 목차에 TEXT 표 그림을 둔다.**

---

## 0. 문서 체계

```text
 ┌─ docs 아키텍처 문서군 ─────────────────────────┐
 │  v1.0 · Part · 입문 · copy · 표 · 코드         │
 │              │ 강점 흡수                       │
 │              ▼                                 │
 │     ★ PDMK_아키텍처_정의서.md (본 정본 v2.0)    │
 └────────────────────────────────────────────────┘
```

### 0.1 이 정본이 흡수한 것

```text
 v1.0 Evidence/GAP ──┐
 Part 골격 ──────────┤
 입문 왜→검증 ───────┼──► 본 정본 v2.0
 copy/표 도해 ───────┤
 소스 대조 ──────────┘
```

| 원천 | 흡수한 강점 |
| ---- | ----------- |
| [12장체계 v1.0](./PDMK_통합_아키텍처_정의서_12장체계_v1.0.md) | Evidence·GAP-ID·책임/금지·pdmg↔mk 계약 |
| [아키텍처 정의서 Part.md](./아키텍처%20정의서%20Part.md) | Part 0~XI 골격·거버넌스 |
| [아키텍처 정의서.md](./아키텍처%20정의서.md) | 왜→검증 학습 경로 |
| [통합 … copy.md](./통합%20아키텍처%20정의서%20copy.md) | 런타임·전문·도해 세부 |
| [표 아키텍처 정의서.md](./표%20아키텍처%20정의서.md) | TEXT 표 그림 |
| 코드 | Filter·Security·WAR·Relay·샘플 서비스 대조 |

### 0.2 문서 역할 (이후)

```text
 [1] 본 정본 ★
 [2] 입문(왜→검증)
 [3] copy 상세 백과
 [4] 표 도해
 [참고] Part / v1.0 / 구판
```

| 우선 | 문서 | 역할 |
| ---- | ---- | ---- |
| **1** | **본 문서** | **정본** |
| 2 | [아키텍처 정의서.md](./아키텍처%20정의서.md) | 입문(왜→검증) |
| 3 | [통합 … copy.md](./통합%20아키텍처%20정의서%20copy.md) | 상세 백과 |
| 4 | [표 아키텍처 …](./표%20아키텍처%20정의서.md) | 도해 조회 |
| 참고 | Part / 12장 v1.0 / 통합(원본) | 이력·병합 원천 |

### 0.3 읽는 방법

```text
 [5분]  §1 기준선 · §2 원칙 · §13 Gap
 [30분] §3 Big Picture → §5 런타임 → §6 계약
 [개발] §6 → §7 → §8 → §14.9 신규거래
 [AA]   §2 · §12 · §13 · 부록 I ADR
 [운영] §9~§11 · §14.6 장애
```

| 역할 | 경로 |
| ---- | ---- |
| 신규 | §1→§3→§5→§6→§7 |
| 업무 개발 | §6→§7→§8→§14.9 |
| FW | §4.1→§5→§9 |
| 운영 | §10→§11→§9 |
| 리뷰 | §12~§14 · Gap |

---

# Part 0 · 기준선

```text
 Part 0 기준선
  └─ §1 목적 · 범위 · Evidence · Gap 요약
```

## 1. 문서 개요와 아키텍처 기준선

```text
 §1.1 목적 → §1.2 범위 → §1.3 Evidence원칙
      → §1.4 Evidence표 → §1.5 Gap요약
```

### 1.1 목적

```text
 ┌─ 구조 정의 ─┐  ┌─ 런타임 정의 ─┐
 │ 모듈·레이어 │  │ Filter→SQL   │
 └──────┬──────┘  └──────┬───────┘
        └────────┬───────┘
    ┌─ 개발 표준 ┴ 통제 기준 ─┐
    │ 서비스ID·TX·Gap·검증     │
    └──────────────────────────┘
```

PDMK를 **하나의 거래 스택**으로 정의하고, 동일 패턴으로 신규 거래를 개발·검증·운영할 수 있는 기준을 제공한다.

| 역할 | 설명 |
| ---- | ---- |
| 구조 정의 | 모듈·레이어·의존 방향 |
| 런타임 정의 | Filter→SQL 순서 |
| 개발 표준 | 서비스ID·DTO·DAO·TX·예외 |
| 통제 기준 | Evidence·Gap·영향·검증 |

### 1.2 대상 시스템 (AS-IS)

```text
 ┌─ PDMK 정합 스택 ─────────────────────────────┐
 │  pdmk-ui (mkcoa*) ──HTTP──► pdmk-service     │
 │                         ▲                    │
 │                         │ impl               │
 │                      pdmk-fw (commons★)      │
 │                         │                    │
 │                      RDW (H2/Oracle)         │
 └──────────────────────────────────────────────┘

 ┌─ 형제 UI (계약 Gap) ─────────────────────────┐
 │  pdmg-ui (mgcoa*) ──(대상 pdmg-service 전제)──│
 │  ※ pdmk-service(mkcoa)와 ID 불일치 → GAP-001  │
 └──────────────────────────────────────────────┘
```

| 모듈 | 루트 | 패키지 | 성격 |
| ---- | ---- | ------ | ---- |
| FW | `pdmk-fw` | `nhnis.fw.commons.*` / `tcf.*` | 공통 JAR |
| Service | `pdmk-service` | `nhnis.mk.*` | MK 업무 WAR |
| UI (정합) | `pdmk-ui` | `nhnis.mk.ui.*` | mkcoa 릴레이 |
| UI (형제) | `pdmg-ui` | `nhnis.mg.ui.*` | mgcoa 릴레이 |
| Data | `rdw.mk.co.a` · `db/h2` | MyBatis · H2 | RDW |

**범위 밖:** 운영 L4/대수, `pdmg-service` 전체 정의, TCF 활성 경로(설정상 OFF).

### 1.3 Source Evidence 원칙

```text
 실제 실행 소스
    ↓
 application.yml / build.gradle / Mapper XML
    ↓
 README / 기존 아키텍처 문서
    ↓
 권고 설계 (TO-BE)
```

충돌 시: **소스 = AS-IS**, 문서 희망 = TO-BE 또는 Gap.

### 1.4 주요 Evidence

```text
 Boot/WAR ── Filter ── Interceptor
      │           │
   Resolver   BizAspect ── Security/CORS
      │
   RDW/MyBatis ── 샘플 mkcoa* ── UI Relay
```

| 관심사 | 경로 |
| ------ | ---- |
| Boot | `nhnis.mk.PdmkApplication` |
| WAR | `ServletInitializer` · `pdmk-service/build.gradle` (`war{enabled=true}`, `bootWar{enabled=false}`) |
| Filter | `nhnis.fw.commons.filter.DefaultFilter` |
| Interceptor | `…interceptor.ServicePreventionInterceptor` |
| Request/Response | `…resolver.RequestBodyArgumentResolver` / `ResponseBodyArgumentResolver` |
| Biz Aspect | `nhnis.mk.co.common.BizPrePostAspect` |
| Security | `nhnis.mk.config.SecurityConfig` |
| CORS | `WebMvcConfig` → `/api/**` |
| RDW | `RdwDataSourceConfig` · `MybatisLogInterceptor` |
| 샘플 | `mkcoa5530` / `8888` / `9999` |
| UI Relay | `pdmk-ui`·`pdmg-ui` 각각의 `TransactionRelayService` |

### 1.5 핵심 기준선 Gap (요약)

```text
 GAP-001 mgcoa≠mkcoa · GAP-003 TX미선언
 GAP-004 war혼선 · GAP-005 timeout미연결
 GAP-006 permitAll · GAP-007 9999전량
 GAP-008 CORS경로 · GAP-009 TCF잔존 · GAP-010 무컨테이너
```

| ID | AS-IS | 영향 | TO-BE |
| -- | ----- | ---- | ----- |
| **GAP-001** | `pdmg-ui`=`mgcoa*` vs Service=`mkcoa*` | 교차 연계 실패 | 스택별 UI·ID 정합 |
| **GAP-002** | 구문서가 UI를 단일 `pdmk-ui`로만 기술 | 명칭 혼동 | pdmk/pdmg 병기 |
| **GAP-003** | 샘플 Service에 `@Transactional` 없음 | 다중 DML 원자성 불명 | Service TX 표준 |
| **GAP-004** | README `bootWar` 안내 vs `bootWar=false`/`war=true` | 빌드 혼선 | `gradlew war` 정본 |
| **GAP-005** | UI `timeout-ms` 있으나 RestClient 미적용 | 릴레이 timeout 무효 | connect/read 적용 |
| **GAP-006** | Security `permitAll` | 인가=Filter JWT에 편중 | 운영 인가 분리 |
| **GAP-007** | `mkcoa9999S0` 페이징 없음 | 대량 응답 위험 | count+ROWNUM |
| **GAP-008** | CORS 매핑 `/api/**` · 업무 URL `/mkcoa*` | 직접호출 CORS 불일치 | 경로 정책 재검토 |
| **GAP-009** | TCF 소스 잔존 · `tcf.enabled=false` | 혼동 | commons만 사용 명시 |
| **GAP-010** | 컨테이너/K8s 없음 | 배포 자동화 부재 | 범위 합의 |

전체: [§13](#13-architecture-gap-카탈로그).

---

# Part I · 원칙

```text
 Part I 원칙
  └─ §2 비전 · 목표 · 제약 · 품질 · 원칙 · ADR
```

## 2. 아키텍처 목표와 설계 원칙

```text
 비전 → 목표 → 제약 → 품질속성 → 설계원칙 → ADR
```

### 2.1 비전

**서비스 ID 중심 온라인 거래**를 commons 프레임워크가 감싸고, 업무는 **Controller → Service → DAO → Mapper** 일관 패턴으로 구현한다.

```text
 서비스ID ── URL · 메서드 · DTO · SQL · Catalog · rms_svc_c · ImageLog
```

### 2.2 목표

```text
 표준화 · 재사용 · 추적성 · 운영성
    │         │         │         │
  전문/ID   commons   GUID    H2↔Oracle
```

| 목표 | 내용 |
| ---- | ---- |
| 표준화 | 전문·네이밍·페이징·에러 |
| 재사용 | Filter·Interceptor·헤더·ImageLog |
| 추적성 | GUID · MDC · ImageLog · sqlId |
| 운영성 | local H2 ↔ 폐쇄망 Oracle/WAS |

### 2.3 제약

```text
 Java21 · Boot3.5 · MyBatis · Hikari
 H2/Oracle · WAR(service) · JAR(fw/ui) · JWT
```

Java 21 · Spring Boot 3.5.x · MyBatis · Hikari · H2/Oracle · service WAR · ui Boot JAR · fw JAR · commons JWT(비-local).

### 2.4 품질속성

```text
 성능 ── 페이징 · TX timeout
 보안 ── JWT · 인가(Gap)
 확장 ── 서비스ID 추가
 유지 ── 레이어 경계
```

성능(페이징·TX timeout) · 보안(JWT/인가 Gap) · 확장성(서비스ID 추가) · 유지보수(레이어 경계) · 가용성(단일 프로세스 샘플).

### 2.5 핵심 설계 원칙

```text
 ★ commons ON / TCF OFF
 ★ 서비스ID = URL = 메서드
 ★ hdr+dto · TX=Service · ui↛fw
 ★ 변경 시 FW/Service/UI 동시 검토
```

| # | 원칙 |
| - | ---- |
| 1 | 세 모듈 역할 분리 · **ui↛fw/DAO** |
| 2 | **commons ON · TCF OFF** |
| 3 | **서비스 ID = URL = 메서드** |
| 4 | 전문 = **`hdr_nhnis` + `dto`** |
| 5 | **TX = Service** `@Transactional` |
| 6 | DAO = Map · 메서드 = SQL ID |
| 7 | 변경 시 FW/Service/UI 영향 동시 검토 |
| 8 | 문서·소스·테스트 동기 변경 |

### 2.6 ADR 요약

```text
 ADR-001 commons/TCF → ADR-002 서비스ID
 ADR-003 TX=Service → ADR-004 pdmk-ui
 ADR-005 ImageLog분리 → ADR-006 war
 ADR-007 pdmg 혼용금지
```

| ADR | 결정 | 상태 |
| --- | ---- | ---- |
| ADR-001 | commons ON · TCF OFF | Accepted |
| ADR-002 | 서비스 ID URL 계약 | Accepted |
| ADR-003 | TX=Service | Accepted ([Gap] 샘플 미적용) |
| ADR-004 | 정합 UI=`pdmk-ui` · HTTP only | Accepted |
| ADR-005 | ImageLog ≠ 업무 TX | Accepted |
| ADR-006 | 산출=`war` (bootWar off) | Accepted |
| ADR-007 | pdmg-ui는 형제 스택 · mkcoa와 혼용 금지 | Accepted |

---

# Part II · 통합

```text
 Part II 통합
  └─ §3 컨텍스트 · 논리/물리 · 의존 · 책임 · 배포
```

## 3. 통합 시스템 아키텍처

```text
 Context → Logic → Physical → Dependency → Matrix → Deploy
```

### 3.1 시스템 컨텍스트

```text
 [Browser]
     │
     ▼
 pdmk-ui:8090  (정합 · mkcoa*)
     │ POST /{서비스ID}  via /api/relay/{id}
     ▼
 pdmk-service:8080 (+ pdmk-fw commons)
     │
     ▼
 RDW (H2 local / Oracle 폐쇄망)
```

### 3.2 논리 아키텍처

```text
 Presentation   UI · Controller
 Application    Service · BizAspect · @Transactional(목표)
 Framework      commons Filter · Interceptor · Resolver
 Data           DAO · MyBatis · RDW TB_*
```

### 3.3 물리·프로세스

```text
 ┌─ pdmk-ui:8090 ─┐   Boot JAR
 └────────┬───────┘
          │ HTTP
 ┌────────▼───────┐   WAR (+fw JAR)
 │ service:8080   │
 └────────┬───────┘
          ▼
       RDW DB
```

| 프로세스 | 포트 | 산출 |
| -------- | ---- | ---- |
| pdmk-ui | 8090 | Boot JAR |
| pdmk-service(+fw) | 8080 | **WAR** |
| RDW | DB | H2 / Oracle |

### 3.4 의존 규칙

```text
 pdmk-ui ──HTTP──► pdmk-service ──Gradle──► pdmk-fw
    ✕ fw/DAO                    │
                                └── MyBatis/JDBC ──► RDW
```

### 3.5 책임 매트릭스

```text
          fw   service   ui
 Filter   ███    ░░░     ░░░
 BizAsp   ░░░    ███     ░░░
 C/S/DAO  ░░░    ███     ░░░
 Relay    ░░░    ░░░     ███
```

| 관심사 | fw | service | pdmk-ui |
| ------ | -- | ------- | ------- |
| Filter/JWT/Context | ● | ○ | — |
| Interceptor/ImageLog | ● | — | — |
| BizAspect | — | ● | — |
| C/S/DAO/DTO | — | ● | — |
| Security 샘플 | ○ OFF | ● permitAll | — |
| Catalog/Relay | — | — | ● |

### 3.6 배포 단위

```text
 pdmk-fw ──────► JAR
 pdmk-service ─► WAR   (gradlew war)
 pdmk-ui ──────► Boot JAR
```

fw **JAR** · service **WAR** (`gradlew war`) · ui **Boot JAR**.

---

# Part III · 모듈

```text
 Part III 모듈
  └─ §4 fw · commons · TCF · service · ui · pdmg · relay · 금지
```

## 4. 모듈 아키텍처

```text
 pdmk-fw ★commons    pdmk-service    pdmk-ui ★
    │                     │              │
    └───── 거래 스택 ─────┴────── HTTP ──┘
 pdmg-ui (형제 · Gap)
```

### 4.1 `pdmk-fw`

```text
 nhnis.fw.commons.*  ★ Filter·Interceptor·Resolver·header·exception·imagelog·message
 nhnis.fw.tcf.*      소스 잔존 · nhnis.fw.tcf.enabled=false
```

**책임:** 거래 컨텍스트·헤더·JWT·에러 envelope·ImageLog.  
**금지:** 업무 CRUD · Boot main · TCF와 commons 이중 경로.

### 4.2 commons 런타임

```text
 DefaultFilter → Security → Interceptor → Resolver(dto)
 → Controller → Advice(hdr+dto / NH_NIS_ERR_DTO)
```

### 4.3 TCF 정책

```text
 tcf.enabled=false ★
   ├─ TcfTraceFilter OFF
   ├─ TCF JWT Filter OFF
   └─ TCFAspect OFF
 commons만 사용 · 이중경로 금지
```

**[AS-IS]** `tcf.enabled=false`. TcfTraceFilter / TCF JWT Filter / TCFAspect 미사용.  
**[Must Not]** commons와 병행 활성.

### 4.4 `pdmk-service`

```text
 nhnis.mk.co.a.{controller,service,dao,dto}
 nhnis.mk.co.common.BizPrePostAspect
 nhnis.mk.config.{Security,Rdw,WebMvc,RDWMapper}
 resources/rdw.mk.co.a/*-ORA.xml · db/h2 · exceptionCode.yml
```

### 4.5 `pdmk-ui` (정합)

```text
 nhnis.mk.ui.{entry,application,client,config,support}
 TransactionCatalog (mkcoa*) · TransactionRelayService
 static/ · sample-requests/
```

**[Evidence]** Catalog·샘플이 `mkcoa5530/8888/9999` — Service와 정합.  
**[Gap-005]** `PdmkUiProperties.timeout` → RestClient 미연결.

### 4.6 `pdmg-ui` (형제 · Gap)

```text
 nhnis.mg.ui.* · Catalog mgcoa* · 대상 주석상 pdmg-service
```

**[Gap-001]** `mgcoa*` ≠ `pdmk-service`의 `mkcoa*`. PDMK 통합 테스트에 pdmg-ui를 붙이지 말 것.

### 4.7 Relay 구조

```text
 Browser → UI /api/relay/{id} → Catalog → RestClient → {target}/{id}
 ※ Authorization 미전달 · timeout-ms 미적용
```

### 4.8 금지 의존

```text
 ❌ Controller → DAO
 ❌ UI → fw / DAO
 ❌ Spring @RequestBody (업무)
 ❌ TCF + commons 이중
 ❌ mgcoa UI → mkcoa Service
```

Controller→DAO · UI→fw/DAO · Spring `@RequestBody`(업무) · TCF+commons 이중 · mgcoa UI→mkcoa Service 혼용.

---

# Part IV · 런타임

```text
 Part IV 런타임
  └─ §5 E2E · 체인 · Filter~MDC
```

## 5. 런타임 거래 아키텍처

```text
 E2E → 체인 → Filter → Interceptor → Aspect
 → Resolver → C/S/DAO → Response → 예외 → MDC
```

### 5.1 End-to-End

```text
 Browser → pdmk-ui:8090 → pdmk-service:8080+fw → RDW → DTOout → UI → Browser
```

### 5.2 진입 체인

```text
 t0 UI relay
 t1 DefaultFilter     Body·JWT·Context·MDC
 t2 Security          permitAll
 t3 Interceptor PRE   GUID·ImageLog
 t4 BizAspect @Before
 t5 C → S → DAO
 t6 BizAspect @After
 t7 Interceptor POST
 t8 Filter finally    Context/MDC clear
 t9 UI ← DTOout
```

### 5.3 `DefaultFilter`

```text
 doFilter
  ├─ CachedBody (JSON)
  ├─ JWT (비-local) → 401
  ├─ hdr_nhnis (local 합성)
  ├─ ServiceContext + MDC
  ├─ chain.doFilter
  └─ finally clear
```

**[Evidence]** `DefaultFilter`  
CachedBody · 비-local Bearer JWT(401) · hdr 파싱(local 합성) · Context/MDC · finally clear.

### 5.4 `ServicePreventionInterceptor`

```text
 preHandle   : GUID · 헤더보강 · ImageLog PRE
 postHandle  : ImageLog POST
 afterComp   : ImageLog EX · rethrow
```

pre: GUID·헤더보강·ImageLog PRE · post: POST · afterCompletion: EX.

### 5.5 `BizPrePostAspect`

```text
 @Before  업무선처리·BRC 로그
    │
 Controller / Service / DAO
    │
 @After   업무후처리·BRC 로그
 (TX·ImageLog 비담당)
```

**[Evidence]** `nhnis.mk.co.common.BizPrePostAspect`  
Pointcut `nhnis.mk.co..controller..*` · 업무 로그만 (TX/ImageLog 비담당).

### 5.6 Request Resolver

```text
 JSON root
  ├─ Filter: hdr_nhnis → Context
  └─ Resolver: dto → DTOin
     (@nhnis.fw.commons.resolver.RequestBody)
```

commons `@RequestBody` → **dto 노드만** DTOin.

### 5.7 Controller→Service→DAO

```text
 @PostMapping("/{서비스ID}")
 Controller → Service → DAO(SQL ID) → *-ORA.xml
```

`@PostMapping("/{서비스ID}")` → Service → DAO(SQL ID) → XML.

### 5.8 Response Resolver

```text
 DTOout ── Advice ──► { hdr_nhnis, dto }
 NhBaseException ──► NH_NIS_ERR_DTO
 Filter 실패 ──► sendError (envelope 없음)
```

Advice: Context 헤더 + 반환 → `{hdr_nhnis,dto}` · 예외 → `NH_NIS_ERR_DTO`.

### 5.9 정상·예외

```text
 [성공] … → DTOout → POST ImageLog → finally
 [예외] NhBase → rollback* → ERR_DTO → EX → finally
 [Filter] sendError · envelope 없음
 *목표: @Transactional 있을 때
```

| 경로 | 결과 |
| ---- | ---- |
| 성공 | envelope DTOout · ImageLog POST |
| NhBaseException | TX rollback(목표) · NH_NIS_ERR_DTO · ImageLog EX |
| Filter 실패 | sendError · **envelope 없음** |

### 5.10 Context·MDC

```text
 Filter set(Context+MDC)
        │
 … Interceptor / Aspect / C/S …
        │
 Filter finally clear
 ※ clear 누락 = GUID 누수
```

Filter set → 전 구간 공유 → finally clear (누수 방지).

---

# Part V · 계약

```text
 Part V 계약
  └─ §6 서비스ID · 전문 · Binding · 페이징 · UI정합
```

## 6. 서비스·전문 계약 아키텍처

```text
 서비스ID → URL/메서드 → 네이밍 → 전문
 → sys_comm → DTO → Binding → 검증 → 페이징 → UI정합
```

### 6.1 서비스 ID

```text
 [대구분2][업무2][세부1][식별4][구분자1][순번1]
    mk      co     a     5530      S       0
 → mkcoa5530S0
 구분자 S/C/U/D/A/R
```

### 6.2 URL = Method = ServiceId

```text
 POST /mkcoa5530S0
      = 메서드 mkcoa5530S0(...)
      = rms_svc_c
      = Catalog.id
```

`POST /mkcoa5530S0` = 메서드 `mkcoa5530S0` = `rms_svc_c`.

### 6.3 네이밍

```text
 nhnis.mk.{업무}.{세부}.{layer}
 mkcoa{식별}Controller/Service/DAO  (구분자 없음)
 rdw.mk…/mkcoa{식별}-ORA.xml
 SQL ID = DAO 메서드명
```

패키지 `nhnis.mk.{업무}.{세부}.{layer}` · 클래스에 구분자 없음 · XML `rdw.mk…/mkcoa{식별}-ORA.xml`.  
정본: [MK-NAMING_CONVENTION.md](./MK-NAMING_CONVENTION.md) · [네이밍원칙.md](./네이밍원칙.md)

### 6.4 전문

```text
 { "hdr_nhnis": { "sys_comm": {…} }, "dto": {…} }
 local: { "dto": {…} } 허용 (Filter 합성)
```

### 6.5 `sys_comm` 핵심

```text
 hdr_nhnis.sys_comm
  ★ std_gbl_id · rms_svc_c · scid
  ★ tr_trm_ipadr · optr_eno
  · sync · tr_sysid · ttl_ug_ync · tr_dtm …
```

`std_gbl_id` · `rms_svc_c` · `scid` · `tr_trm_ipadr` · `optr_eno`

### 6.6 DTO

```text
 mkcoa5530S0
  ├─ mkcoa5530S0DTOin
  ├─ mkcoa5530S0DTOout
  └─ mkcoa5530S0DTOSub0[]
```

`{ID}DTOin` / `DTOout` / `DTOSub{n}` · camelCase · DataObject.

### 6.7 Binding

```text
 [요청] Filter(hdr) + Resolver(dto→Java)
 [응답] Advice(Java→hdr+dto JSON)
 [예외] NH_NIS_ERR_DTO
```

Filter(hdr) + Resolver(dto↔Java) + Advice(envelope).

### 6.8 Validation

```text
 [AS-IS] Filter: Body/hdr/JWT
 [TO-BE] Bean Validation · 업무필수 · 에러코드
```

**[AS-IS]** Filter Body/hdr/JWT. **[TO-BE]** Bean Validation·업무 필수값·에러코드 표준.

### 6.9 페이징

```text
 pageNo/Size → normalize(20/100) → offset
   ├─ *_count → totalCount
   └─ ROWNUM list → Sub[] + meta
 ※ mkcoa9999S0 미적용 (GAP-007)
```

pageNo≥1 · pageSize 기본20·상한100 · offset · count + ROWNUM · DTOout 메타.  
**[Gap-007]** `mkcoa9999S0` 미적용.

### 6.10 UI–Service 정합성

| UI | Catalog ID | 대상 Service | 상태 |
| -- | ---------- | ------------ | ---- |
| **pdmk-ui** | `mkcoa*` | `pdmk-service` | **정합 ★** |
| **pdmg-ui** | `mgcoa*` | (pdmg-service 전제) | **pdmk-service와 Gap** |

```text
 ✅ pdmk-ui ──mkcoa──► pdmk-service
 ❌ pdmg-ui ──mgcoa──► pdmk-service   (GAP-001)
```

---

# Part VI · 업무 앱

```text
 Part VI 업무
  └─ §7 레이어 · C/S/DAO · 패턴 · 샘플 · 신규
```

## 7. 업무 애플리케이션 아키텍처

```text
 Controller → Service → DAO → Mapper
      ▲         │ TX·변환·페이징
   BizAspect    └── DTO↔Map
```

### 7.1 표준 레이어

```text
 Presentation  Controller
 Application   Service (+TX)
 Persistence   DAO / XML
 Infrastructure fw · DS
```

C(진입) → S(규칙·TX·변환) → DAO(SQL) → XML.

### 7.2 Controller

```text
 ✅ @PostMapping · DTOin · Service 위임
 ❌ SQL · @Transactional · 비즈니스 로직
```

매핑·위임만. SQL/TX/비즈니스 금지. throws Throwable 샘플 존재.

### 7.3 Service

```text
 DTOin→Map · DAO · Map→DTO · 페이징·검증
 [Gap] 샘플 @Transactional 없음
 [TO-BE] 조회 readOnly / 쓰기 필수
```

DTOin→Map · DAO · Map→DTO · 페이징·검증.  
**[Gap-003]** 샘플에 `@Transactional` 없음 → **[TO-BE]** 조회 `readOnly` / 쓰기 필수.

### 7.4 DAO

```text
 @RDWMapper
 메서드 = SQL ID = XML id
 입출력 Map / List / int
 ❌ DTO 시그니처
```

`@RDWMapper` · 메서드=SQL ID · Map/List/int · DTO 시그니처 금지.

### 7.5 Mapper XML

```text
 <mapper namespace="…DAO FQCN">
   <select id="{SQL ID}" …>
     SELECT /* SQL ID */ …
   </select>
 </mapper>
```

namespace=FQCN · id=메서드 · `/* SQL ID */` · HashMap · ROWNUM.

### 7.6 DTO↔Map

```text
 Service only:
   DTOin ──► Map ──► DAO
   DAO ──► Map행 ──► DTOSub/DTOout
```

Service에서만 변환.

### 7.7 조회 S0 / 삭제 D0

```text
 [S0] count + list(ROWNUM) + paging meta
 [D0] @Transactional 안 DELETE (목표)
```

S0: count+list+페이징 메타. D0: `@Transactional` 안에서 DELETE (목표).

### 7.8 샘플

```text
 5530S0 안내항목  페이징○  TX선언✕
 8888S0/D0 ImageLog  ○/—   TX선언✕
 9999S0 영업팁    페이징✕  TX선언✕
```

| ID | 내용 | 페이징 | TX 선언 |
| -- | ---- | ------ | ------- |
| mkcoa5530S0 | 안내항목 | ○ | 없음[Gap] |
| mkcoa8888S0/D0 | ImageLog | ○/— | 없음[Gap] |
| mkcoa9999S0 | 영업팁 | ❌ | 없음[Gap] |

### 7.9 신규 거래 표준

```text
 ① 네이밍 → ② DTO → ③ C/S/DAO/XML
 → ④ 페이징·@Transactional·테이블
 → ⑤ pdmk-ui Catalog/샘플/화면
 → ⑥ fw는 공통만
```

---

# Part VII · 데이터·TX

```text
 Part VII 데이터
  └─ §8 DS · 환경 · TX · ImageLog · 테이블 · SQL
```

## 8. 데이터·트랜잭션 아키텍처

```text
 접근구조 → DS → H2/Oracle → TX경계
 → Commit → ImageLog → 테이블 → SQL → 금지
```

### 8.1 접근 구조

```text
 Service → DAO → *-ORA.xml → rdwSqlSessionTemplate → rdwDataSource → RDW
```

### 8.2 RDW DataSource

```text
 RdwDataSourceConfig
  ├─ rdwDataSource (@Primary, Hikari)
  ├─ rdwSqlSessionFactory (+ MybatisLog)
  ├─ rdwSqlSessionTemplate
  └─ rdwTransactionManager (@Primary)
```

**[Evidence]** `RdwDataSourceConfig`  
`rdwDataSource`(@Primary) · Factory(+MybatisLog) · Template · `rdwTransactionManager`(@Primary).

### 8.3 H2 ↔ Oracle

```text
 [local]  H2 mem MODE=Oracle · sql.init=always
 [폐쇄망] Oracle · sql.init=off · DBA 스키마
```

local: H2 mem MODE=Oracle · sql.init always.  
폐쇄망: Oracle · init off · DBA 스키마.

### 8.4 TX 경계 (목표)

```text
 Filter/Interceptor/Aspect ≠ DB TX
 Service @Transactional → rdwTM → DAO SQL → commit/rollback
```

### 8.5 Commit/Rollback

```text
 정상 → commit
 예외 → rollback  (프록시 TX 있을 때)
 [Gap] @Transactional 없으면 자동 롤백 경계 없음
```

예외 시 롤백(목표). **[Gap-003]** 선언 없으면 프록시 TX 미적용.

### 8.6 ImageLog TX

```text
 Interceptor ImageLog JDBC  ≠  업무 @Transactional
 ImageLog 실패 → 삼킴 (업무 미전파)
```

Interceptor JDBC ≠ 업무 TX. 실패 삼킴.

### 8.7 테이블

```text
 TB_FW_IMAGE_LOG ◄── fw + mkcoa8888
 TB_MK_CO_A_5530 ◄── mkcoa5530
 TB_CR_AH_SALES_TIP_RACT ◄── mkcoa9999
```

| 테이블 | 접근 |
| ------ | ---- |
| TB_FW_IMAGE_LOG | fw + mkcoa8888 |
| TB_MK_CO_A_5530 | mkcoa5530 |
| TB_CR_AH_SALES_TIP_RACT | mkcoa9999 |

### 8.8 SQL 표준

```text
 SQL ID = 메서드 · namespace = FQCN
 ROWNUM 페이징 · #{…} · Alias T1… · /* SQL ID */
```

SQL ID=메서드 · FQCN namespace · ROWNUM · `#{…}` · Alias T1….

### 8.9 금지

```text
 ❌ Controller SQL · DAO에 DTO
 ❌ ui→DB · 앱이 운영 DDL 생성
```

C→SQL · DAO에 DTO · ui→DB · 앱 운영 DDL.

---

# Part VIII · 횡단

```text
 Part VIII 횡단
  └─ §9 예외·보안·로그·ImageLog·Timeout·Cache
```

## 9. 공통·횡단 아키텍처

```text
 예외 → 메시지 → 인증인가 → CORS
 → 로그 → ImageLog → SQL로그 → Timeout → Cache → 외부
```

### 9.1 예외·에러

```text
 NhBaseException → Advice → NH_NIS_ERR_DTO
 Filter 실패 → sendError (envelope 없음)
 업무 분기 → DTOout 결과코드
```

### 9.2 메시지

```text
 exceptionCode.yml → nhnis.exception.*
 MessageLoader → MessageCache (옵션)
        ▼
 MessageFormat(args)
```

`exceptionCode.yml` · (옵션) MessageCache.

### 9.3 인증·인가

```text
 UI Relay (Auth 미전달 Gap)
   → DefaultFilter JWT (비-local)
   → Security permitAll (Gap)
   → C/S/DAO
```

| 층 | AS-IS | TO-BE |
| -- | ----- | ----- |
| Filter JWT | 비-local Bearer | 유지 |
| Security | permitAll | URL/역할 |
| UI Relay | Auth 미전달 | non-local 시 전달 |

### 9.4 CORS·Relay

```text
 Browser ─Relay(서버)─► Service  (/mkcoa*)
 CORS 매핑 AS-IS: /api/**  (GAP-008)
 ※ 직접 호출 시 경로 불일치
```

**[Evidence]** `WebMvcConfig` `/api/**`. 업무 URL은 `/mkcoa*` → 브라우저 직접호출 시 **[Gap-008]**.  
Relay는 서버사이드이므로 CORS 의존 감소.

### 9.5 로그·추적

```text
 MDC: guid · ip · userId · serviceId
 PdmkTxLog: system* / biz*
 sqlId: MybatisLogInterceptor
```

MDC guid/ip/userId/serviceId · PdmkTxLog · sqlId.

### 9.6 ImageLog

```text
 PRE  INSERT  GUID 행
 POST UPDATE  RESPONSE_TIME
 EX   UPDATE  예외 컬럼 (+필요시 INSERT)
```

PRE INSERT · POST/EX UPDATE → `TB_FW_IMAGE_LOG`.

### 9.7 SQL 로그

```text
 MybatisLogInterceptor
   → debug SQL · MDC sqlId
```

`MybatisLogInterceptor`.

### 9.8 Timeout

```text
 UI timeout-ms [Gap 미적용]
   ≥ Relay HTTP
   ≥ @Transactional(timeout 초)
   ≥ SQL / APIGW·FOS
 ttl_ug_ync ≠ TX
```

### 9.9 Cache

```text
 MessageCache(JVM옵션) · CachedBody(요청)
 Catalog(UI) · exceptionCode(기동)
 ❌ Redis / @Cacheable 업무캐시(무단)
```

MessageCache(옵션) · CachedBody(요청) · Catalog. Redis/`@Cacheable` 업무 캐시 금지(별도 설계 전).

### 9.10 외부연동

```text
 fw: APIGW / FOS handler 존재
 샘플 업무 필수 경로 아님
 Timeout·오류변환 = 공통화 대상
```

fw에 APIGW/FOS handler 존재. 샘플 업무 필수 경로 아님.

---

# Part IX · 플랫폼

```text
 Part IX 플랫폼
  └─ §10 Boot · Config · 프로파일 · 키 · Gradle
```

## 10. 플랫폼·환경 아키텍처

```text
 Boot → Configuration → Profile → Keys
 → Gradle → 의존 → 버전 → Nexus → 인코딩
```

### 10.1 Boot

```text
 PdmkApplication (scan nhnis) ── include fw
 PdmkUiApplication ── fw 미의존
 fw Main 없음
```

| 모듈 | Main | scan |
| ---- | ---- | ---- |
| service | PdmkApplication | `nhnis` |
| ui | PdmkUiApplication | ui |
| fw | 없음 | 편입 |

### 10.2 Configuration

```text
 @ConditionalOnProperty
  filter · legacy-web · tcf · security
```

`@ConditionalOnProperty` — filter / legacy-web / tcf / security.

### 10.3 프로파일

```text
 env/시크릿
   → profiles.active
   → application.yml
   → exceptionCode.yml
 local: H2 · JWT스킵 · hdr합성
```

### 10.4 설정 키

```text
 nhnis.fw.* · jwt.* · datasource.rdw
 pdmk.ui.* · pdmg.ui.* · spring.mvc.cors
 framework.message · nhnis.exception
```

| Key | AS-IS 요지 |
| --- | ---------- |
| `nhnis.fw.tcf.enabled` | false |
| `commons.filter` / `legacy-web` | true |
| `commons.security.enabled` | false |
| `jwt.*` | secret · expiration |
| `spring.datasource.rdw.*` | H2/Oracle |
| `pdmk.ui.target-base-url` | :8080 |
| `pdmk.ui.timeout-ms` | 10000 · **미적용** |
| `pdmg.ui.*` | 형제 UI용 |
| `spring.mvc.cors.*` | 비면 CORS 미등록 |
| `nhnis.exception.*` | exceptionCode.yml |

### 10.5~10.6 Gradle

```text
 pdmk-fw (jar) ← service (war)
 pdmk-ui (bootJar)
 Wrapper 8.10.1 · JDK21 · Boot 3.5.14
 빌드 정본: gradlew war (bootWar off)
```

**[Gap-004]** 빌드 정본 = **`gradlew war`** (bootWar off).

### 10.7~10.9 버전·Nexus·인코딩

```text
 local=Central / 폐쇄망=Nexus
 UTF-8 · IDE .settings 커밋 비권고
```

폐쇄망 Nexus · UTF-8 · IDE `.settings` 커밋 비권고.

---

# Part X · 배포·운영

```text
 Part X 운영
  └─ §11 산출 · WAS · local · 기동 · 전환 · 장애
```

## 11. 배포·운영 아키텍처

```text
 산출물 → Tomcat → Local → 기동순서
 → 환경전환 → 장애 → 로그 → 성능
```

### 11.1 산출물

```text
 fw JAR · service WAR · ui Boot JAR
```

fw JAR · service WAR · ui Boot JAR.

### 11.2 Tomcat

```text
 service WAR → 외부 WAS
 [Gap-010] Docker/K8s 없음
```

service WAR → 외부 WAS. **[Gap-010]** 컨테이너 없음.

### 11.3 Local

```text
 bootRun / RUN.bat · H2 mem
 profiles=local · JWT 스킵
```

bootRun / RUN.bat · H2.

### 11.4 기동 순서

```text
 [1] fw jar → [2] service:8080 → [3] pdmk-ui:8090
```

### 11.5 환경 전환

```text
 H2→Oracle · Central→Nexus
 JWT ON · sql.init off · hdr 필수
```

H2→Oracle · Central→Nexus · JWT ON · sql.init off · hdr 필수.

### 11.6 장애 추적

```text
 UI?→401?→400?→에러dto?→SQL?
 →목록폭주?→롤백?→Bean?→mgcoa?
 → target / JWT / hdr / yml / SQL ID
   / 페이징 / @TX / scan / GAP-001
```

### 11.7~11.8 로그·성능

```text
 GUID 타임라인: Interceptor→Aspect→C/S→ImageLog→SQL
 병목: Relay · SQL · 9999전량 · TX
```

GUID 중심 타임라인 · 병목: Relay·SQL·전량조회·TX.

---

# Part XI · 검증·거버넌스

```text
 Part XI 검증
  └─ §12 Must · Evidence · 정합 · Gap · ADR · 신규
```

## 12. 아키텍처 검증·거버넌스

```text
 Must/Should/MustNot → Evidence표
 → 정합 → 자동검증 → 테스트
 → Gap → ADR → 영향 → 신규절차 → 변경관리
```

### 12.1 Must / Should / Must Not

```text
 Must: ID=URL · hdr+dto · tcf=false · ui↛fw · pdmk-ui
 Should: @Transactional · 페이징 · timeout연결
 MustNot: C→DAO · Spring@RB · TCF이중 · mgcoa혼용
```

| | 규칙 |
| - | ---- |
| **Must** | 서비스ID=URL · hdr+dto · tcf=false · ui↛fw · 정합 UI=pdmk-ui(mkcoa) |
| **Should** | Service `@Transactional` · 목록 페이징 · timeout 계층 연결 |
| **Must Not** | C→DAO · Spring `@RequestBody` · TCF+commons 이중 · mgcoa→mkcoa 혼용 |

### 12.2 Evidence 검증표

```text
 yml ↔ Filter/Interceptor
 Controller ↔ Catalog ↔ SQL ID
 build.gradle ↔ war
 @Transactional ↔ [Gap 샘플]
```

| # | 주장 | 확인 |
| - | ---- | ---- |
| 1 | tcf=false | application.yml |
| 2 | filter/legacy-web | yml |
| 3 | URL=서비스ID | Controller `@PostMapping` |
| 4 | dto 바인딩 | commons `@RequestBody` |
| 5 | TX=Service | **[Gap]** 샘플 없음 |
| 6 | 메서드=SQL ID | DAO↔XML |
| 7 | war 산출 | build.gradle |
| 8 | Catalog=Endpoint | pdmk-ui vs Controllers |

### 12.3~12.5 정합·자동검증·테스트

```text
 Catalog↔Controller diff [TO-BE]
 ui→fw 의존 금지 린트 [TO-BE]
 Unit / H2 Integration / Smoke relay
```

Catalog↔Controller diff · ui→fw 의존 금지 · Naming 린트 **[TO-BE]**.  
Unit/Integration(H2)/Smoke(relay 3종).

### 12.6~12.8 Gap·ADR·영향

```text
 Gap 카탈로그(§13) · ADR(부록 I)
 변경영향: ID/DTO→svc+ui · 헤더→전모듈 · Filter→fw
```

[§13](#13-architecture-gap-카탈로그) · [부록 I](#부록-i-adr) · 아래 매트릭스.

| 변경 | fw | service | pdmk-ui |
| ---- | -- | ------- | ------- |
| 서비스ID/DTO/DAO | — | ● | ● |
| 헤더 | ● | ● | ● |
| Filter/JWT | ● | ○ | ○ |
| 테이블 | ○ | ● | — |
| 페이징 | — | ● | ● |
| Gradle/Boot | ● | ● | ● |

### 12.9 신규 거래 절차

```text
 ①네이밍→②DTO→③C/S/DAO/XML
 →④페이징·TX·테이블→⑤pdmk-ui→⑥fw공통만
```

§7.9와 동일. pdmk-ui Catalog 동기화 필수.

### 12.10 변경관리

```text
 소스 + 본 정본 + 검증테스트
        = 한 변경 단위
 문서 후갱신 금지 · Gap 즉시 등록
```

---

## 13. Architecture Gap 카탈로그

```text
 GAP-001~010
 계약·문서·TX·빌드·UI timeout·보안
 ·페이징·CORS·TCF·컨테이너
```

| ID | 영역 | AS-IS | TO-BE | 상태 |
| -- | ---- | ----- | ----- | ---- |
| GAP-001 | 계약 | pdmg-ui mgcoa ≠ service mkcoa | 스택별 정합 | Open |
| GAP-002 | 문서 | UI 단일 명칭 | pdmk/pdmg 병기 | **Closed in v2** |
| GAP-003 | TX | 샘플 `@Transactional` 없음 | Service 표준 | Open |
| GAP-004 | 빌드 | bootWar 안내 혼선 | `gradlew war` | Open(문서) |
| GAP-005 | UI | timeout 미적용 | RestClient 연결 | Open |
| GAP-006 | 보안 | permitAll | 운영 인가 | Open |
| GAP-007 | 데이터 | 9999 전량 | 페이징 | Open |
| GAP-008 | CORS | `/api/**` vs `/mkcoa*` | 경로 정책 | Open |
| GAP-009 | FW | TCF 잔존 | commons only | Accepted risk |
| GAP-010 | 배포 | 무컨테이너 | 합의 후 | Out of scope |

---

## 14. 실무 카드

```text
 포트/URL 카드 · 왜→검증 한줄 경로
```

### 14.1 포트·URL

```text
 pdmk-ui:8090 ──POST /{mkcoa*}──► service:8080(+fw) ──► RDW
 relay: /api/relay/{서비스ID}
```

### 14.2 왜→검증 (입문 한 줄)

```text
 왜(§2)→전체(§3)→모듈(§4)→흐름(§5)→계약(§6)
 →업무/DB(§7~8)→횡단(§9)→빌드·운영(§10~11)→검증(§12~13)
```

---

# 부록

## 부록 A. 디렉토리 맵

```text
 pdmk-fw/…/nhnis/fw/{commons,tcf}
 pdmk-service/…/nhnis/mk/{co/a,config}
 pdmk-ui/…/nhnis/mk/ui
 pdmg-ui/…/nhnis/mg/ui  (형제)
```

## 부록 B. 클래스 인덱스

```text
 Filter·Interceptor·Resolver·Aspect
 Security·RDW·ImageLog·NhBase
 Catalog/Relay (mk·mg) · mkcoa* C/S/DAO
```

DefaultFilter · ServicePreventionInterceptor · Request/ResponseBodyArgumentResolver · ServiceContextHolder · BizPrePostAspect · SecurityConfig · WebMvcConfig · RdwDataSourceConfig · MybatisLogInterceptor · ImageLogHandler · NhBaseException · TransactionCatalog/RelayService (mk·mg) · mkcoa5530/8888/9999 C·S·DAO

## 부록 C. API 카탈로그

```text
 mkcoa5530S0 / 8888S0 / 8888D0 / 9999S0
 /api/relay/{id}
 mgcoa* = pdmg-ui only (≠ pdmk-service)
```

| ID | URL | 비고 |
| -- | --- | ---- |
| mkcoa5530S0 | POST /mkcoa5530S0 | 안내항목 |
| mkcoa8888S0 | POST /mkcoa8888S0 | ImageLog 조회 |
| mkcoa8888D0 | POST /mkcoa8888D0 | ImageLog 삭제 |
| mkcoa9999S0 | POST /mkcoa9999S0 | 영업팁 · 페이징 Gap |
| UI relay | POST /api/relay/{id} | pdmk-ui |
| mgcoa* | (pdmg-ui) | **pdmk-service와 불일치** |

## 부록 D. 설정 키

```text
 nhnis.fw.* · jwt.* · rdw · pdmk.ui.*
 pdmg.ui.* · cors · message · exception
```

§10.4 표 참조.

## 부록 E. 테이블·SQL

```text
 8888 → IMAGE_LOG · 5530 → MK_CO_A_5530
 9999 → SALES_TIP · SQL ID = 메서드
```

| 서비스 | DAO | XML | SQL ID | 테이블 |
| ------ | --- | --- | ------ | ------ |
| 8888 | mkcoa8888DAO | mkcoa8888-ORA.xml | S0_S0(+count), D0_D0 | TB_FW_IMAGE_LOG |
| 5530 | mkcoa5530DAO | mkcoa5530-ORA.xml | S0_S0(+count) | TB_MK_CO_A_5530 |
| 9999 | mkcoa9999DAO | mkcoa9999-ORA.xml | S0_S0(+count) | TB_CR_AH_SALES_TIP_RACT |

## 부록 F. 전문 샘플

```text
 { hdr_nhnis.sys_comm , dto }
 sample-requests/*.json (pdmk-ui)
```

```json
{
  "hdr_nhnis": { "sys_comm": { "rms_svc_c": "mkcoa5530S0", "scid": "mkcoa5530" } },
  "dto": { "pageNo": 1, "pageSize": 20 }
}
```

파일: `pdmk-ui/.../sample-requests/*.json`

## 부록 G. 용어

```text
 PDMK · commons/TCF · RDW · ImageLog
 AS-IS/TO-BE · Evidence · Gap · 서비스ID
```

PDMK · commons/TCF · RDW · ImageLog · AS-IS/TO-BE · Evidence · Gap · 서비스ID · hdr_nhnis

## 부록 H. Gap

```text
 → §13 Architecture Gap 카탈로그
```

[§13](#13-architecture-gap-카탈로그)

## 부록 I. ADR

```text
 ADR-001~007 Accepted
 (TX 샘플 Gap · pdmg 혼용금지 포함)
```

| ID | 결정 | 상태 |
| -- | ---- | ---- |
| ADR-001 | commons ON · TCF OFF | Accepted |
| ADR-002 | 서비스 ID = URL = 메서드 | Accepted |
| ADR-003 | TX = Service | Accepted (샘플 Gap) |
| ADR-004 | 정합 UI = pdmk-ui | Accepted |
| ADR-005 | ImageLog ≠ 업무 TX | Accepted |
| ADR-006 | 산출 = war | Accepted |
| ADR-007 | pdmg-ui 혼용 금지 | Accepted |

---

## 변경 이력

```text
 v2.0 2026-08-08  docs 통합 정본 신설
      (+ TEXT 표 그림 전 절 보강)
```

| 버전 | 일자 | 내용 |
| ---- | ---- | ---- |
| v2.0 | 2026-08-08 | docs 아키텍처 문서 통합 정리 · 정본 신설 |

---

*PDMK 아키텍처 정의서 v2.0 — docs 통합 정본*
