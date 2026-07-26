# TCF-OC 용량산정 설계기준

> NSIGHT 마케팅플랫폼 용량·환경설정의 **산정 전제·공식·판정 기준**을 tcf-oc 구현 관점에서 정리한 문서입니다.  
> 화면/API 상세는 [`README.md`](../README.md), NEW Wizard는 [`cap-new-design.md`](cap-new-design.md)를 참고하세요.

## 1. 문서 목적

| 대상 | 용도 |
|------|------|
| 아키텍트·운영 | 산정 가정·한계·DR 규칙 확인 |
| 개발 | `support`·`DCCapacity`·`capnew` 구현과 설계 기준 대조 |
| 검토·성능시험 | 운영 기준 TPS·AP·Thread·Pool·DB Session 근거 확인 |

**원칙:** 서버 대수만이 아니라 **사용자 → 동시요청 → TPS → AP → WAS Thread → DB Pool → 장애 잔여**가 하나의 체인으로 연결되어야 합니다.

---

## 2. 모듈 구성 (tcf-oc)

```text
┌─────────────────────────────────────────────────────────────────┐
│  tcf-ui (8099) — 정적 화면 + API Relay                          │
└────────────┬───────────────────────────────┬────────────────────┘
             │                               │
   /api/oc/capacity/*              /api/oc/cap-new/*
   /api/oc/env/*                           │
             ▼                               ▼
┌──────────────────────┐         ┌──────────────────────────────┐
│  기존 CAP (CAP-010~050)│         │  NEW 용량산정 (8단계 Wizard)   │
│  DCCapacity           │         │  CapNewDerivationService      │
│  ASMSC71001           │         │  CapNewWizardService          │
└──────────┬───────────┘         └──────────────┬───────────────┘
           │                                      │
           └──────────────┬───────────────────────┘
                          ▼
           ┌──────────────────────────────────────┐
           │  공통 산정 엔진 (support)               │
           │  NsightCapacityDerivation              │
           │  NsightDbPoolDerivation                │
           │  VmProfile · JvmSizingGuide            │
           │  TomcatHikariSizingGuide               │
           └──────────────────────────────────────┘
                          │
           ┌──────────────┴───────────────────────┐
           │  ENV (ENV-001~004)                     │
           │  CapacityPlannerService                │
           │  EnvironmentRuleEngineService          │
           └──────────────────────────────────────┘
```

| 구분 | API prefix | UI | 저장 |
|------|------------|-----|------|
| **기존 CAP** | `/api/oc/capacity` | `/oc/capacity.html` | 세션/일회성 산정 |
| **NEW cap-new** | `/api/oc/cap-new` | `/oc/cap-new/*` | `CAP_NEW_SCENARIO` DB |
| **ENV** | `/api/oc/env` | `/oc/env-00*.html` | 점검 Run·handoff |

기존 CAP와 cap-new는 **코드·API·DB가 분리**되어 있으며, 산정 공식만 `support` 패키지를 **공유 참조**합니다.

---

## 3. 표준 전제 (NSIGHT 운영 기준)

### 3.1 cap-new Wizard 기본값

`CapNewDefaultsService` · 템플릿 seed 기준:

| 항목 | 기본값 | 비고 |
|------|--------|------|
| 지점 수 | 6,000 | `branchCount` |
| 지점당 사용자 | 6 | |
| **전체 사용자** | **36,000** | 본부·기타 가산 가능 |
| 세션 여유율 | 30% | `sessionMarginRate` |
| Session Timeout | 60분 | |
| 기준 VM | 16CORE-128GB | 운영 표준 |
| TPMC/TPS | 3,000 | SingleView 조회 기준 |
| Core당 TPS | 36 | TPMC 연동 역산 |
| 센터 구성 | Active-Active | 2센터 50:50 |
| DR 단일센터 전부하 | true | 페일오버 AP 산정 |
| 센터당 AP 여유 | +1 | `apMarginPerCenter` |
| DB Session 한도 | 800 | 운영 RDW 기준 |

### 3.2 TPS 시나리오 프리셋 (STEP 3)

동시요청률 × 전체 사용자 ÷ 응답시간(초) = **목표 TPS** (`CapNewStepRule.enrichStep3`).

| 코드 | 라벨 | 동시요청률 | 응답(초) | 36,000명 기준 TPS |
|------|------|-----------|---------|-------------------|
| NORMAL | 평시 | 3% | 3 | 360 |
| PEAK | 정상 피크 | 5% | 3 | 600 |
| DESIGN_PEAK | 설계 피크 | 10% | 3 | 1,200 |
| STRESS | 스트레스 | 15% | 3 | 1,800 |
| SLOW_RESPONSE | 응답지연 | 10% | 5 | (검증용, 기본 비활성) |
| DR_FAULT | DR 장애 | 10% | 3 | 1,200 (DR 전용) |

**운영 기준 시나리오** (`operatingBaseline`): AP·WAS·Pool·종합 판정의 기준 TPS로 사용합니다.  
일반적으로 `DESIGN_PEAK` 또는 `PEAK`를 지정하며, STEP 6 `baselineScenarioCode`와 **반드시 일치**해야 합니다.

### 3.3 기존 CAP README 요약 전제 (참고)

문서·가이드 1차안(3,600지점 × 6명 = 21,600)도 병행 존재합니다.  
**tcf-oc cap-new 구현 기본값은 6,000지점·36,000명**이며, 스테이징 소형 템플릿은 1,000지점·6,000명으로 축소합니다.

---

## 4. 산정 체인 (단계별 공식)

### STEP 2 — 사용자·세션 (cap-new)

```
totalUsers = branchCount × userPerBranch + hqUsers + otherUsers
designedSessions = ceil(totalUsers × (1 + sessionMarginRate))
```

- `calcMode=DIRECT`이면 `totalUsersDirect` 직접 입력 가능.

### STEP 3 — TPS

```
concurrentUsers = round(totalUsers × concurrentRate)
targetTps = ceil(concurrentUsers / responseSec)
```

검증:
- 활성 시나리오 ≥ 1
- `operatingBaseline` 지정 필수
- `STRESS TPS ≥ 운영 기준 TPS` (스트레스 활성 시)

### STEP 4 — VM·Core TPS

```
vmTheoreticalTps = vmCores × tpsPerCore
vmAdjustedTps = floor(vmTheoretical × cpuTarget × virtFactor × opsFactor / perfSafety)   [보정 ON]
designPeakTps = operatingBaseline 시나리오의 targetTps
minRequiredAp = ceil(designPeakTps / vmAdjustedTps)
```

**TPMC ↔ Core TPS 연동** (`NsightCapacityDerivation.coreTpsFromTpmc`):

```
기준: 35 TPS/Core @ TPMC 3,000
tpsPerCore = floor(35 × 3000 / tpmcPerTps)
```

| 업무 유형 | TPMC/TPS | Core TPS(기준) |
|-----------|----------|----------------|
| CACHE | 1,500 | 71 |
| SIMPLE | 2,000 | 53 |
| SINGLE_VIEW | 3,000 | 36 |
| COMPLEX | 4,000 | 27 |
| TXN | 5,000 | 21 |

### STEP 5 — AP·센터·DR

센터 모드별 AP 산정 (`CapNewDerivationService.enrichStep5`):

| centerMode | 센터 수 | AP 산정 개요 |
|------------|---------|--------------|
| ACTIVE_ACTIVE | 2 | 트래픽 50:50, 센터당 `ceil(TPS/2 / vmTps) + margin` |
| DR_STANDBY | 2 | 운영 + DR 대기 |
| SINGLE | 1 | 단일 센터 전부하 |

```
singleCenterAp = ceil(targetTps / vmEffectiveTps)
failoverAp = drSingleCenterFullLoad ? singleCenter + margin : normalPerCenterAp
totalDeploymentAp = apPerCenter × centerCount
```

판정 (`classifyAp`):
- **NORMAL**: 용량 여유
- **WARN**: 설계 TPS가 센터 용량에 근접
- **CRITICAL**: DR 전부하 시 단일 센터 TPS 초과

### STEP 6 — WAS Thread·JVM

운영 기준 TPS·배포 AP로 Thread 산정 (`DCCapacity.calculateWasThreadOnly`):

```
apTps = ceil(targetTps / deploymentAp)
totalThreads = apTps × avgThreadHoldSec × threadMarginRate
threadsPerVm = ceil(totalThreads / deploymentAp)
recommendedMaxThreads = threadsPerVm × maxThreadMarginRate
```

JVM 권장: `JvmSizingGuide.recommend(vmProfile)` — Heap·GC·Thread Stack.

**중요:** `targetTps`는 STEP 3 `operatingBaseline`과 STEP 6 `baselineScenarioCode`가 일치해야 합니다.

### STEP 7 — DB Pool·DB Session

4단계 Pool 공식 (`NsightDbPoolDerivation`):

```
① AP TPS ≈ targetTps / deploymentAp
② theoreticalPool = ceil(AP TPS × holdSec × dbUsageRatio × poolSafetyFactor)
③ ceilingPool = ceil(threadsPerVm × threadDbUsageRatio)
⑤ sizedPool = min(②, ③)
④ recommendedPool = max(minPoolPerVm, ⑤)  [프로파일 cap 적용]
totalDbSessions = deploymentAp × poolPerVm
```

기본 계수:

| 항목 | 일반 | SingleView |
|------|------|------------|
| DB 연결 점유(hold) | 0.15초 | 0.20초 |
| DB 트랜잭션 비율 | 1.0 | 1.0 |
| Pool 안전계수 | 1.3 | 1.3 |
| Thread 대비 DB 사용 | 30% | 30% |
| VM당 최소 Pool | 30 | 30 |

**WAR별 Pool** (`CapNewWarPoolAllocation`): 동일 Tomcat 다중 WAR 시 비중 분배 후 Pool 합산·`warPoolTotalSessions` 검증.

### STEP 8 — 종합 판정

`riskSummary` 집계 → headline `overallJudgment`:

| 판정 | 조건 |
|------|------|
| NORMAL | AP·WAS·DB·WAR Pool 모두 정상 |
| WARN | 일부 지표 주의 |
| CRITICAL | DR·DB Session·Thread 한계 초과 |

---

## 5. VM Profile 기준

구현: `com.nh.nsight.marketing.oc.support.VmProfile`

| 코드 | Core/RAM | 문서 1차 TPS | Tomcat Thread(참고) | Hikari SV max |
|------|----------|-------------|---------------------|---------------|
| 4CORE-32GB | 4/32 | 120 | 280~350 | 40 |
| 8CORE-64GB | 8/64 | 250 | 400~500 | 80 |
| 16CORE-128GB | 16/128 | 500 | 800~1,000 | 80 |
| 32CORE-256GB | 32/256 | 1,000 | 1,200~1,500 | 150 |

- **산정 TPS**: Core × Core당 TPS (TPMC·보정계수 반영)
- **문서 1차 TPS** (`guideNominalTps`): 보수 운영 참고값 (8C=250 등)
- VM 대안 비교: `CapNewVmCompareService` — 8C/16C/32C Scale-Out vs Scale-Up

---

## 6. 시나리오 템플릿 (cap-new)

DB 테이블: `CAP_NEW_SCENARIO_TEMPLATE`  
카탈로그 API: `GET /api/oc/cap-new/templates`

| 코드 | 용도 | 운영 기준 | VM | 요약 TPS/AP |
|------|------|-----------|-----|-------------|
| PROD_STANDARD | 표준 운영 16C | DESIGN_PEAK | 16CORE-128GB | 1,200 TPS · 8 AP |
| PEAK_OPS | 정상 피크 운영 | PEAK | 16CORE-128GB | 600 TPS · 4 AP |
| PERF_STRESS | 성능시험 | STRESS | 16CORE-128GB | 1,800 TPS · 12 AP |
| DR_FAULT | DR 장애 수용 | DR_FAULT | 16CORE-128GB | 1,200 TPS · 10 AP |
| SCALE_8C | 8C 증설 검토 | DESIGN_PEAK | 8CORE-64GB | 1,200 TPS · 15 AP |
| STG_SMALL | 스테이징 소형 | PEAK | 4CORE-32GB | 100 TPS · 2 AP |

템플릿 선택 시 STEP 1~7 seed가 materialize되어 Wizard에 채워지며, 사용자는 수치만 조정합니다.

---

## 7. 기존 CAP 단계 매핑 (CAP-010~050)

`CapacityCalcStep` · `DCCapacity.calculate`:

| 코드 | 단계 | 산출 |
|------|------|------|
| 020 | CAP-020 TPS | 동시요청자·목표 TPS·TPMC |
| 030 | CAP-030 AP | 필요/권장 AP (A-A·DR) |
| 040 | CAP-040 WAS | maxThreads·Thread 여유 |
| 050 | CAP-050 DB Pool | Hikari maximumPoolSize |
| ALL | 전체 | 020~050 연쇄 |

cap-new ↔ 기존 CAP 대조: `CapNewLegacyCompareService` (`GET /scenarios/{id}/legacy-compare`).

---

## 8. ENV 연동 기준

cap-new 확정 전 ENV-002 handoff (`CapNewEnvBridgeService`):

- STEP 1~7 → `CapacityPlannerRequest` 변환
- `CapacityDesignService.analyze()` → Grid·Timeout·Pool 매트릭스
- UI: Wizard STEP 8 → `/oc/env-002.html` (sessionStorage handoff)

ENV는 **산정 결과를 설정값(Grid)과 대조**하는 역할이며, 산정 공식 자체는 CAP/cap-new와 동일 `support`를 참조합니다.

---

## 9. 판정·검증 공통 규칙

| 영역 | WARN | CRITICAL |
|------|------|----------|
| AP/DR | 센터 용량 85% 근접 | DR 전부하 초과 |
| WAS Thread | 권장 maxThreads 근접 | Tomcat 상한 초과 |
| DB Pool | 이론 Pool > Thread 상한 | totalDbSessions > dbSessionLimit |
| WAR Pool 합계 | 비중 불균형 | warPoolTotalSessions 초과 |

연쇄 재산정: 상위 STEP 저장 시 하위 STEP 자동 재계산 (`CapNewWizardService.cascadeDownstream`).

---

## 10. 구현 클래스 맵

| 설계 영역 | Java 클래스 |
|-----------|-------------|
| TPS·Core TPMC | `NsightCapacityDerivation` |
| DB Pool 4단계 | `NsightDbPoolDerivation` |
| VM·Tomcat·Hikari | `VmProfile`, `TomcatHikariSizingGuide` |
| JVM Heap | `JvmSizingGuide` |
| 기존 CAP 산정 | `DCCapacity`, `ASMSC71001` |
| NEW STEP 4~8 | `CapNewDerivationService` |
| NEW 검증 | `CapNewStepRule` |
| NEW Wizard | `CapNewWizardService` |
| 템플릿 | `CapNewScenarioTemplateService`, `CapNewTemplateSeedFactory` |
| ENV 산정 | `CapacityPlannerService` |
| ENV Rule | `EnvironmentRuleEngineService` |

---

## 11. 관련 문서

| 문서 | 위치 |
|------|------|
| tcf-oc 모듈 README | [`../README.md`](../README.md) |
| cap-new Wizard 설계 | [`cap-new-design.md`](cap-new-design.md) |
| 아키텍처 점검리스트 | [`architecture-inspection-checklist.md`](architecture-inspection-checklist.md) |
| 용량산정 가이드 (57장) | [`../../ztcf-book-capacity-md/README.md`](../../ztcf-book-capacity-md/README.md) |
| HELP — cap-new | `tcf-help/docs/20-oc-용량-환경/05-cap-new-wizard.md` |
| 필드 매핑 (부록 A) | `ztcf-book-capacity-md/_scripts/cap-field-mapping.js` |

---

## 12. 변경 이력

| 일자 | 내용 |
|------|------|
| 2026-07-12 | 최초 작성 — CAP/cap-new/ENV 통합 설계기준, 템플릿·공식·VM Profile 정리 |
