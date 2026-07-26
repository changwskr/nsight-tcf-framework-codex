# tcf-oc — 용량 산정 · 환경설정 (CAP / ENV)

NSIGHT 마케팅플랫폼 **용량 산정(CAP-010~050)** 과 **통합 환경설정(ENV-001~004)** 을 제공하는 Spring Boot 업무 모듈입니다.  
`eb-service` 패키지 구조를 따르며, 업무코드 **OC** · WAR **`oc.war`** · bootRun 포트 **8094** 입니다.

| 항목 | 값 |
|------|-----|
| Gradle 모듈 | `tcf-oc` |
| 메인 클래스 | `com.nh.nsight.marketing.oc.NsightOcServiceApplication` |
| bootRun | 포트 **8094** (`application-local.yml`) |
| WAR (bootWar) | `oc.war` → ztomcat `oc.war` (`/oc`) |
| UI 진입 | **tcf-ui** Relay (`8099`) — 정적 화면·API 프록시 |

```text
브라우저 → tcf-ui (/oc/*.html, /api/oc/*) → tcf-oc (8094 또는 Tomcat /oc)
```

## 기능 개요

| 영역 | 설계서 | 설명 |
|------|--------|------|
| **CAP** | CAP-010~050 | 지점·사용자·시나리오 → TPS → AP → WAS Thread → DB Pool |
| **ENV** | ENV-001~004 | 산정 조건·VM/TPMC·계층별 설정 Grid·Rule 점검·Excel보내기 |
| **TCF Online** | `OC.Hello.inquiry` | 샘플 온라인 거래 (헬스·연동 확인) |

산정 가이드 Markdown: [`ztcf-book-capacity-md/`](../ztcf-book-capacity-md/README.md)

**용량산정 설계기준 (산정 전제·공식·판정):** [`docs/capacity-design-standard.md`](docs/capacity-design-standard.md)

**아키텍처 점검리스트:** [`docs/architecture-inspection-checklist.md`](docs/architecture-inspection-checklist.md)

## 패키지 구조

```text
com.nh.nsight.marketing.oc
├── entry/
│   ├── controller/          REST — ACMSC71001(CAP), ACMSC72001(WAS), OcEnvApiController
│   ├── handler/             TCF Online — OcHelloHandler
│   └── facade/              OcHelloFacade
├── application/
│   ├── service/             ASMSC71001, ASMSC72001, DCCapacity
│   ├── service/env/         CapacityPlannerService, EnvironmentRuleEngineService, …
│   ├── rule/                CapacityCDtoConverter, OcHelloRule
│   └── dto/
│       ├── capacity/        CapacityCalculationCDTO, CapacityCalculationResultCDTO, …
│       └── env/             CapacityPlannerRequest, IntegratedEnvironmentView, …
├── persistence/             OcHelloDao, OcHelloMapper (H2·MyBatis)
└── support/                 NsightCapacityDerivation, VmProfile, CapacityCalcStep, …
```

| eb-service 대응 | tcf-oc | 비고 |
|-----------------|--------|------|
| entry/handler | `OcHelloHandler` | TCF Online |
| entry/facade | `OcHelloFacade` | |
| — | `entry/controller` | REST 전용 (`ACMSC*`, `OcEnv*`) |
| application/service | `ASMSC71001`, `DCCapacity`, `env/*` | |
| application/rule | `CapacityCDtoConverter` | CDTO → DDTO 변환 |
| persistence | `OcHelloDao` | |
| support | `NsightCapacityDerivation`, `VmProfile` | 산정 공식·VM 프로파일 |

## 화면 (tcf-ui 정적 리소스)

UI는 **tcf-ui** 에서 제공하고, API는 `OcRelayService`가 tcf-oc로 Relay 합니다.

| 화면 | URL (bootRun) | 설계서 |
|------|---------------|--------|
| 용량 산정 | `/oc/capacity.html` | CAP-010~050 (3단계 마법사) |
| ENV 허브 | `/oc/env-001.html` | ENV-001 |
| 산정 조건 | `/oc/env-002.html` | ENV-002 |
| TPS/VM 결과 | `/oc/env-003.html` | ENV-003 |
| 계층별 설정 Grid | `/oc/env-004.html` | ENV-004 |
| 환경 점검 | `/oc/check.html` | 통합 점검 요약 |
| Rule 점검 | `/oc/rule-check.html` | Timeout·Pool·SC 규칙 |

리다이렉트: `/oc/plan.html` → `/oc/capacity.html` · `/traceenvironment/*` → `/oc/*` (하위 호환)

Tomcat WAR 배포 시 접두사 `/ui` 가 붙습니다. (`ui-context.js` 자동 보정)

## API — 용량 산정 (`/api/oc/capacity`)

| 메서드 | 경로 | 설명 |
|--------|------|------|
| `GET` | `/defaults` | CAP-010 기본 입력 (`CapacityCalculationCDTO`) |
| `POST` | `/calculate` | CAP-020~050 **전체** 산정 |
| `POST` | `/calculate-step` | 단계 산정 (`calculationStep`: `020` \| `030` \| `040` \| `050`) |
| `POST` | `/was-thread/calculate` | WAS Thread 단독 산정 (`ACMSC72001`) |

### 산정 단계 (`CapacityCalcStep`)

| 코드 | 단계 | 산출 |
|------|------|------|
| 020 | CAP-020 TPS | 동시요청자·목표 TPS·TPMC |
| 030 | CAP-030 AP | 필요/권장 AP 대수 (A-A·DR 반영) |
| 040 | CAP-040 WAS | maxThreads·Thread 여유 |
| 050 | CAP-050 DB Pool | Hikari `maximumPoolSize` (4단계 공식) |

### 주요 입력 필드 (`CapacityCalculationCDTO`)

| 필드 | 설명 | 기본값 |
|------|------|--------|
| `branchCount` | 지점 수 | 6000 |
| `userPerBranch` | 지점당 사용자 | 6 |
| `concurrentRequestRates` | 동시요청률 목록 | 3/5/10/15% |
| `targetResponseTimes` | 목표 응답(초) | 3, 4, 5 |
| `vmSpecCode` | VM 사양 코드 | `8C64G` |
| `tpsPerCore` / `tpmcPerTps` | Core TPS · TPMC | 35 · 3000 |
| `activeActive` / `drValidation` | 2센터 A-A · DR 검증 | true |
| `validateDbPool` / `dbSessionLimit` | DB Session 한도 검증 | true · 500 |

부록 A 양식·화면 id 대조: `ztcf-book-capacity-md/_scripts/cap-field-mapping.js`

## API — 환경설정 (`/api/oc/env`)

| 메서드 | 경로 | 설명 |
|--------|------|------|
| `GET` | `/capacity-design/defaults` | ENV-002 기본 조건 (`CapacityPlannerRequest`) |
| `POST` | `/capacity-design/analyze` | ENV-002 산정·Grid 생성 |
| `GET` | `/settings` | 통합 환경설정 카탈로그 |
| `GET` | `/projects/baseline` | 프로젝트 Baseline |
| `POST` | `/config-files/upload` | server.xml·yml 등 설정 업로드·파싱 |
| `POST` | `/assessments` | Rule Engine 점검 실행 |
| `GET` | `/assessments/{runId}` | 점검 Run 조회 |
| `GET` | `/assessments/{runId}/timeout-map` | Timeout 계층 맵 |
| `GET` | `/assessments/{runId}/concurrent-flow-map` | 동시요청·TPS 흐름 맵 |
| `POST` | `/export/excel` | ENV 보고서 Excel |
| `GET` | `/dashboard/summary` | 대시보드 요약 |

ENV-002 DTO 필드명은 CAP과 일부 상이합니다 (`usersPerBranch`, `actualRequestPercents`, `vmProfileId` 등).  
상세 매핑은 부록 A · `cap-field-mapping.js` 참조.

## TCF Online

| ServiceId | 경로 | 설명 |
|-----------|------|------|
| `OC.Hello.inquiry` | `POST /oc/online` | 샘플 조회 (Relay 경유) |

## 실행

```bash
# bootRun (권장 — local 프로필, H2 in-memory)
gradle :tcf-oc:bootRun

# 또는 스크립트
tcf-oc/scripts/run-local.bat

# WAR 빌드
gradle :tcf-oc:bootWar
# → build/libs/oc.war
```

### tcf-ui와 함께 사용

```bash
# 1) OC 백엔드
gradle :tcf-oc:bootRun          # :8094

# 2) UI Relay
gradle :tcf-ui:bootRun          # :8099
```

브라우저: http://localhost:8099/oc/capacity.html · http://localhost:8099/oc/env-002.html

Tomcat 배포: `ztomcat/deploy-wars.bat oc ui` → Gateway `http://localhost:8080/oc` · UI `http://localhost:8080/ui/oc/...`

## 설정

| 파일 | 용도 |
|------|------|
| `application.yml` | 공통 — MyBatis, TCF, Timeout |
| `application-local.yml` | bootRun — 포트 8094, H2 `nsight_capacity` |
| `application-dev.yml` | 개발/스테이징 프로필 |

```yaml
# application-local.yml 요약
server:
  port: 8094
spring:
  datasource:
    url: jdbc:h2:mem:nsight_capacity;MODE=Oracle;...
nsight:
  tcf:
    runtime:
      business-code: OC
```

## NSIGHT 1차 산정 전제 (요약)

| 항목 | 기준 |
|------|------|
| 사용자 | 3,600지점 × 6명 = 21,600 |
| TPS | 360 / 720 / 1,080 (5/10/15% × 3초) |
| VM | 8 vCPU Scale-Out, VM당 TPS 250 (보수) |
| Pool | max(30, min(TPS×hold×safety, Thread×30%)) |

구현: `NsightCapacityDerivation`, `NsightDbPoolDerivation`, `VmProfile`

## 관련 모듈

| 모듈 | 역할 |
|------|------|
| `tcf-ui` | OC 화면·API Relay (`OcRelayService`, `OcCapacityApiController`) |
| `tcf-core` / `tcf-web` / `tcf-util` | TCF 프레임워크 |
| `ztcf-book-capacity-md` | 용량·환경설정 가이드 Markdown (57장 + 부록) |
