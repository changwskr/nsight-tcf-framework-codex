# 48. 멀티모듈 의존성 설계서

| 항목 | 내용 |
|------|------|
| 문서 번호 | 48 |
| 제목 | Multi-Module Dependency Design |
| 상위 문서 | [architecture.md](architecture.md) |
| 관련 문서 | [22-build-project.md](22-build-project.md), [31-autoconfiguration.md](31-autoconfiguration.md), [46-service-integration-contract.md](46-service-integration-contract.md), [../manual/lib-module.md](../manual/lib-module.md) |
| 상세 매뉴얼 | [08-Gradle-멀티모듈.md](../../znsight-man/08-Gradle-멀티모듈.md), [63-로컬-빌드-방법.md](../../znsight-man/63-로컬-빌드-방법.md) |
| 설정 파일 | `settings.gradle`, 루트 `build.gradle`, 각 모듈 `build.gradle` |
| 대상 | 프레임워크·업무 개발자, 빌드·릴리즈 담당자 |

---

## 1. 문서 목적

본 문서는 NSIGHT TCF Gradle **멀티모듈 의존 방향·허용 범위·변경 영향**을 정의한다.

| 구분 | 담당 문서 |
|------|-----------|
| Gradle 명령·빌드 태스크 | [22-build-project.md](22-build-project.md), [63-로컬-빌드-방법.md](../../znsight-man/63-로컬-빌드-방법.md) |
| JAR·WAR 물리 경로 | [../manual/lib-module.md](../manual/lib-module.md) |
| 업무 WAR 간 호출 | [46-service-integration-contract.md](46-service-integration-contract.md) |
| **모듈 의존 규칙·매트릭스** | **본 문서 (48)** |

핵심 문장:

> 의존성은 **단방향 계층**만 허용한다. 공통 lib는 업무 WAR를 모르고, **업무 WAR끼리 Gradle 의존은 금지** — 런타임 연동은 `tcf-eai` + HTTP.

---

## 2. 모듈 인벤토리

`settings.gradle` 기준 **22개** 서브프로젝트 (`rootProject.name`: `nsight-tcf-framework-tcfmodules`).

| 영역 | 모듈 | 산출물 | bootRun |
|------|------|--------|---------|
| **Lib** | `tcf-util` | JAR | ✕ |
| | `tcf-core` | JAR | ✕ |
| | `tcf-web` | JAR | ✕ |
| | `tcf-cache` | JAR | ✕ |
| | `tcf-eai` | JAR | ✕ |
| **운영·인프라 WAR** | `tcf-om` | `tcf-om.war` | 8097 |
| | `tcf-batch` | `tcf-batch.war` | 8098 |
| | `tcf-ui` | `tcf-ui.war` | 8099 |
| | `tcf-uj` | `tcf-uj.war` | 8102 |
| | `tcf-gateway` | `gw.war` | 8100 |
| | `tcf-jwt` | `jwt.war` | 8110 |
| **업무 WAR** | `ic-service` … `mg-service` (9개) | `{code}.war` | 포트별 |
| **레거시** | `om-service` | `om.war` | 미배포 권장 — `tcf-om` 사용 |

업무 9개: `ic`, `pc`, `ms`, `sv`, `pd`, `eb`, `ep`, `ss`, `mg`.

---

## 3. 의존 계층 (허용 방향)

```text
                    ┌─────────────────────────────────────┐
                    │  업무 WAR · tcf-om · tcf-jwt ·      │
                    │  tcf-batch · tcf-ui · tcf-uj        │
                    │  tcf-gateway (독립 스택)             │
                    └──────────────┬──────────────────────┘
                                   │ implementation / api
                    ┌──────────────▼──────────────────────┐
                    │  tcf-eai (선택) · tcf-cache (선택)   │
                    └──────────────┬──────────────────────┘
                                   │
                    ┌──────────────▼──────────────────────┐
                    │  tcf-web  (HTTP·MyBatis·AutoConfig)  │
                    └──────────────┬──────────────────────┘
                                   │ api
                    ┌──────────────▼──────────────────────┐
                    │  tcf-core  (TCF·STF·ETF·전문)        │
                    └──────────────┬──────────────────────┘
                                   │ api
                    ┌──────────────▼──────────────────────┐
                    │  tcf-util  (순수 유틸, Spring 무관)   │
                    └─────────────────────────────────────┘
```

**금지:** 위 화살표 **역방향** (예: `tcf-core` → `sv-service`), **업무 WAR ↔ 업무 WAR** Gradle `project()` 참조.

---

## 4. 모듈별 의존 매트릭스 (현행)

`build.gradle` 기준. ● = 직접 `project()` 의존.

| 모듈 | util | core | web | cache | eai | 비고 |
|------|:----:|:----:|:---:|:-----:|:---:|------|
| `tcf-util` | — | | | | | Spring 의존 없음 |
| `tcf-core` | ● | | | | | `api` 전파 |
| `tcf-web` | ● | ● | | | | `api` 전파 |
| `tcf-cache` | | ● | | | | `api tcf-core` |
| `tcf-eai` | | ● | | | | `api tcf-core` |
| `*-service` (표준) | ● | ● | ● | | | MyBatis·Session·AOP |
| `ic-service`, `sv-service` | ● | ● | ● | | ● | 연동 샘플 |
| `tcf-om` | | ● | ● | ● | | **유일** cache 사용 WAR |
| `tcf-batch` | | ● | ● | | | |
| `tcf-jwt` | ● | ● | ● | | | OAuth2 RS |
| `tcf-ui`, `tcf-uj` | | ● | | | | Relay·테스트 UI |
| `tcf-gateway` | | | | | | **tcf-core 미사용** — 독립 Gateway |
| `om-service` | ● | ● | ● | | | 레거시 |

### 4.1 외부 의존 (공통)

루트 `subprojects` — Spring Boot BOM `3.3.5`, Java **21** toolchain.

업무 WAR·`tcf-om` 공통 스타터:

- `spring-boot-starter-web`, `validation`, `actuator`, `jdbc`, `aop`
- `spring-session-jdbc`, `mybatis-spring-boot-starter:3.0.3`
- `runtimeOnly h2:2.2.224`
- `providedRuntime tomcat` (외부 Tomcat WAR 배포)

---

## 5. 의존 규칙

### 5.1 필수 원칙

| # | 규칙 |
|---|------|
| R1 | **공통 → 업무** 역참조 금지 |
| R2 | **업무 WAR → 업무 WAR** `project()` 금지 — [46-service-integration-contract.md](46-service-integration-contract.md) |
| R3 | **tcf-core**에 Servlet·MyBatis·업무 패키지 넣지 않음 |
| R4 | **tcf-web**에 업무 Handler·DAO 넣지 않음 |
| R5 | `tcf-cache`는 **필요 WAR만** `implementation` — 현재 `tcf-om`만 |
| R6 | `tcf-eai`는 **연동하는 WAR만** 추가 |
| R7 | `api` vs `implementation` — lib가 하위에 타입을 노출하면 `api`, 내부만 쓰면 `implementation` |

### 5.2 `api` vs `implementation`

| 모듈 | 설정 | 이유 |
|------|------|------|
| `tcf-core` → `tcf-util` | `api` | core 공개 API가 util 타입 사용 |
| `tcf-web` → `tcf-core`, `tcf-util` | `api` | WAR가 web만 의존해도 core 타입 노출 |
| 업무 WAR → lib | `implementation` | WAR fat jar — 전이 노출 최소화 |

### 5.3 AutoConfiguration 경계

`tcf-web` `META-INF/spring/...AutoConfiguration.imports` — WAR classpath에 `tcf-web`이 있으면 TCF 파이프라인 자동 기동.

| 모듈 추가 | 효과 |
|-----------|------|
| `tcf-cache` | EhCache AutoConfig — **tcf-om만** 의존 ([31-autoconfiguration.md](31-autoconfiguration.md)) |
| `tcf-eai` | `TcfIntegrationConfiguration` — `nsight.integration` 바인딩 |

---

## 6. 모듈 프로필 (권장 템플릿)

### 6.1 신규 업무 WAR

```gradle
plugins {
    id 'org.springframework.boot'
    id 'war'
}

dependencies {
    implementation project(':tcf-util')
    implementation project(':tcf-core')
    implementation project(':tcf-web')
    // 연동 필요 시만:
    // implementation project(':tcf-eai')
    implementation 'org.springframework.boot:spring-boot-starter-web'
    // ... (표준 스타터 — ic-service/build.gradle 참고)
}
```

`settings.gradle`에 `include '{code}-service'` 추가.

### 6.2 서비스 간 연동 WAR

`ic-service`, `sv-service` 패턴:

```gradle
implementation project(':tcf-eai')
```

업무 `client` 패키지 Adapter — core의 `TransactionContext` 사용 (`com.nh.nsight.tcf.core.support.context`).

### 6.3 운영 WAR (`tcf-om`)

```gradle
implementation project(':tcf-cache')  // 거래통제·Timeout 캐시
```

OM은 **전체 OMDB** SoT — 업무 WAR가 OM DAO를 참조하지 않음.

### 6.4 Gateway (`tcf-gateway`)

- `tcf-core` / `tcf-web` **미의존**
- Spring Security OAuth2 Resource Server + JDBC (Route·SESSIONDB)
- 표준 TCF 파이프라인과 **별도 스택** — Gateway 코드는 `tcf-gateway` 모듈 내부에만

---

## 7. 빌드·태스크 의존

루트 `build.gradle` 등록 태스크:

| 태스크 | 의존 모듈 |
|--------|-----------|
| `:tcf-core:build` | util |
| `:sv-service:bootWar` | util, core, web, [eai] |
| `buildBusinessWars` | `businessModules` 중 `bootWar` 있는 모듈 |
| `buildZtomcatWars` | business + batch + ui + uj + jwt + gateway |

`ext.businessModules`: 9 업무 + `tcf-om`.

bootRun 공통 (`subprojects`):

- `workingDir` = 루트 프로젝트
- `nsight.txlog.path` = `./data/nsight-txlog`
- `spring.profiles.active` = `local`

---

## 8. 변경 영향도

### 8.1 모듈 변경 시 영향 범위

| 변경 모듈 | 직접 영향 | 검증 |
|-----------|-----------|------|
| `tcf-util` | core → web → **모든 WAR** | `:tcf-core:test`, 대표 WAR smoke |
| `tcf-core` | web → **모든 tcf-web 의존 WAR** | core test + om + sv |
| `tcf-web` | **모든 업무·om·batch·jwt** | AutoConfig 회귀, `/online` |
| `tcf-cache` | **tcf-om** | OM 캐시·거래통제 |
| `tcf-eai` | eai 의존 WAR (ic, sv) | 연동 E2E |
| 단일 `*-service` | 해당 WAR만 | 해당 `bootWar` |

### 8.2 승인 권장

| 변경 | 승인 |
|------|------|
| `tcf-util`, `tcf-core`, `tcf-web` | 프레임워크 리드 + 회귀 테스트 |
| `settings.gradle` 모듈 추가 | 아키텍트 |
| 업무 WAR `build.gradle` 의존 추가 | 해당 업무 + 아키텍트 |

---

## 9. 금지·안티패턴

| 안티패턴 | 문제 | 대안 |
|----------|------|------|
| `sv-service` → `project(':ic-service')` | WAR 결합·순환 위험 | `tcf-eai` HTTP |
| `tcf-core`에 `*-service` import | 프레임워크 오염 | Handler는 업무 WAR |
| 모든 WAR에 `tcf-cache` | 불필요 EhCache·메모리 | om만 |
| `tcf-gateway`에 `tcf-web` 추가 (무분별) | 이중 필터·TCF 중복 | Gateway 전용 코드 유지 |
| 업무 코드를 `tcf-core/support`에 추가 | 전 WAR 강제 배포 | 업무 패키지에 유지 |
| `compileOnly`로 core 타입만 사용 후 런타임 누락 | `NoClassDefFoundError` | `implementation project(':tcf-core')` |

---

## 10. 패키지·모듈 정합

| 모듈 | 루트 패키지 |
|------|-------------|
| lib | `com.nh.nsight.tcf.{util,core,web,eai,cache}` |
| 업무 | `com.nh.nsight.marketing.{ic,sv,om,...}` |
| gateway | `com.nh.nsight.tcf.gateway` |
| jwt | `com.nh.nsight.tcf.jwt` |

`tcf-core` 최상위: `application`, `client`, `config`, `entry`, `persistence`, `support` — 하위 도메인은 `core.support.*`.

---

## 11. 신규 모듈 체크리스트

- [ ] `settings.gradle` `include`
- [ ] `build.gradle` — 표준 WAR 템플릿
- [ ] `application-local.yml` — port·DS·[integration]
- [ ] `bootWar` `archiveFileName` = `{code}.war`
- [ ] `ztomcat/deploy-wars` 대상 목록 (통합 배포 시)
- [ ] **다른 업무 `project()` 없음**
- [ ] 연동 시 `tcf-eai` + [46](46-service-integration-contract.md)
- [ ] CI 변경 모듈 빌드 경로

---

## 12. 레거시·명칭 정리

| 항목 | 현행 | 비고 |
|------|------|------|
| 연동 모듈 | `tcf-eai` | 구 문서 `tcf-integration` → 동일 역할 |
| OM 운영 | `tcf-om` | `om-service` 대체 |
| 문서상 16개 업무 | workspace 9개 | `bc`, `cc`, `cm` 등은 확장 예정 — include 전 빌드 불가 |

---

## 13. 관련 소스

| 경로 | 설명 |
|------|------|
| `settings.gradle` | 모듈 목록 |
| `build.gradle` | BOM·businessModules·집계 태스크 |
| `tcf-core/build.gradle` | lib 체인 시작 |
| `tcf-web/build.gradle` | web 스타터·MyBatis |
| `sv-service/build.gradle` | 업무 WAR + eai 표준 |
| `tcf-om/build.gradle` | cache 포함 운영 WAR |
| `tcf-gateway/build.gradle` | 독립 Gateway 스택 |

---

← [47-data-governance.md](47-data-governance.md) · [49-release-strategy.md](49-release-strategy.md) →
