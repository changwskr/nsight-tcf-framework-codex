# NSIGHT TCF-OC NEW 용량산정 프로그램설계서

| 항목 | 내용 |
|------|------|
| **문서 ID** | OC-CAP-NEW-PRG-001 |
| **버전** | V1.0 |
| **작성일** | 2026-07-15 |
| **대상 모듈** | tcf-oc (`com.nh.nsight.marketing.oc.capnew`) |
| **관련 문서** | `cap-new-design.md`, `cap-new-02-거래설계서.md`, `cap-new-04-SQL-DB-설계서.md` |
| **문서 목적** | cap-new 기능의 계층 구조, 클래스 책임, 처리 흐름, 산정 엔진 연동을 정의한다. |

---

## 1. 도입

### 1.1 개발 원칙

| 원칙 | 내용 |
|------|------|
| 기존 CAP 분리 | `DCCapacity`, `ASMSC71001`, `CapacityPlannerService` **수정 금지** |
| 참조만 | `NsightCapacityDerivation`, `NsightDbPoolDerivation` 등 support 클래스 참조 |
| REST 진입 | `CapNewApiController` — TCF Online Handler 미사용 |
| JSON Payload | STEP 1~8 데이터는 `STEP_PAYLOAD` CLOB에 통합 저장 |
| 연쇄 재산정 | 상위 STEP 저장 시 하위 STEP 자동 enrich |

### 1.2 모듈 식별

| 항목 | 값 |
|------|-----|
| Gradle 모듈 | tcf-oc |
| Java 패키지 | `com.nh.nsight.marketing.oc.capnew` |
| REST prefix | `/api/oc/cap-new` |
| UI 모듈 | tcf-ui (`/oc/cap-new/*.html`) |
| DB | H2 (개발) / RDW (운영) |

---

## 2. 패키지·계층 구조

```
com.nh.nsight.marketing.oc.capnew
├── entry.controller          ← REST API 진입
│   └── CapNewApiController
├── application
│   ├── service               ← 비즈니스 오케스트레이션
│   │   ├── CapNewWizardService
│   │   ├── CapNewDerivationService
│   │   ├── CapNewCompareService
│   │   ├── CapNewApprovalService
│   │   ├── CapNewDefaultsService
│   │   ├── CapNewEnvBridgeService
│   │   ├── CapNewExcelExportService
│   │   ├── CapNewLegacyCompareService
│   │   ├── CapNewVmCompareService
│   │   ├── CapNewScenarioTemplateService
│   │   └── CapNewTemplateDataInitializer
│   ├── rule                  ← STEP 검증
│   │   └── CapNewStepRule
│   └── dto                   ← CDTO·Request·Response
├── persistence
│   ├── dao                   ← DAO 래퍼
│   ├── mapper                ← MyBatis Mapper 인터페이스
│   └── dto                   ← Row DTO
└── support                   ← 상수·유틸·예외
    ├── CapNewStep
    ├── CapNewScenarioStatus
    ├── CapNewStepSnapshot
    ├── CapNewWarPoolAllocation
    └── CapNewBizException
```

### 2.1 계층별 책임

| 계층 | 책임 | 금지 사항 |
|------|------|----------|
| Controller | HTTP 매핑·응답 래핑·예외 핸들링 | 비즈니스 로직·DB 직접 접근 |
| Service | Wizard 흐름·산정·검증·트랜잭션 | SQL 직접 작성 |
| Rule | STEP별 입력 검증 | DB 접근 |
| DAO | Mapper 호출·Row↔DTO 변환 | 비즈니스 판단 |
| Mapper | SQL 실행 | — |
| Support | 산정 유틸·스냅샷·WAR Pool | Spring Bean 외부 호출 최소화 |

---

## 3. 전체 처리 흐름

### 3.1 시나리오 생성

```text
[tcf-ui wizard.html]
        │
        │ POST /api/oc/cap-new/scenarios
        ▼
[CapNewApiController.createScenario]
        │
        ▼
[CapNewWizardService.createScenario]
        ├─ templateCode 있음 → CapNewScenarioTemplateService.getSeedPayload()
        │                      → materializeSeedPayload()
        └─ 없음 → CapNewStepRule.validateStep1()
        │
        ▼
[CapNewScenarioDao.insert] → CAP_NEW_SCENARIO
        │
        ▼
[CapNewScenarioCDTO] 응답 (scenarioId, stepTrack)
```

### 3.2 STEP 저장·연쇄 재산정

```text
[tcf-ui] PUT /scenarios/{id}/step/{n}
        │
        ▼
[CapNewWizardService.saveStep]
        ├─ 1. payload 병합 (step{n})
        ├─ 2. CapNewStepRule.validateStep(n)
        ├─ 3. CapNewDerivationService.enrichStep(n)  [STEP 4~8]
        ├─ 4. cascadeDownstream(n)                  [하위 STEP 재산정]
        ├─ 5. buildStepTrack()                      [트랙 상태]
        ├─ 6. STEP 8 완료 시 STATUS=COMPLETED
        └─ 7. CapNewScenarioDao.update
        │
        ▼
[CapNewScenarioCDTO + cascadeImpact]
```

### 3.3 연쇄 재산정 규칙

| 저장 STEP | cascadeDownstream 대상 |
|-----------|----------------------|
| 2 | 3, 4, 5, 6, 7, 8 |
| 3 | 4, 5, 6, 7, 8 |
| 4 | 5, 6, 7, 8 |
| 5 | 6, 7, 8 |
| 6 | 7, 8 |
| 7 | 8 |

`CapNewStepSnapshot`으로 변경 전후 TPS·AP·Thread·Pool·DB Session 차이를 기록한다.

---

## 4. 핵심 클래스 설계

### 4.1 CapNewApiController

| 항목 | 내용 |
|------|------|
| 경로 | `@RequestMapping("/api/oc/cap-new")` |
| 의존 | 9개 Service (Wizard, Defaults, Template, Compare, Approval, Env, Excel, Legacy, Vm) |
| 예외 | `@ExceptionHandler(CapNewBizException)` → 400 + fail message |
| Binary | Excel export → `application/vnd.openxmlformats-officedocument.spreadsheetml.sheet` |

### 4.2 CapNewWizardService

| 메서드 | 설명 | @Transactional |
|--------|------|----------------|
| `listScenarios(status)` | 목록·상태 필터 | — |
| `getScenario(id)` | 상세 + stepTrack | — |
| `createScenario(request)` | 신규·템플릿 seed | Y |
| `saveStep(id, n, request)` | 저장·검증·연쇄 | Y |
| `deleteScenario(id)` | DRAFT만 삭제 | Y |
| `buildStepTrack(payload)` | 8단계 트랙 | — |
| `cascadeDownstream(n, payload)` | 하위 재산정 | — |

시나리오 ID 생성: `CAP-NEW-` + UUID 8자리 대문자

### 4.3 CapNewStepRule

| 메서드 | STEP | 검증 내용 |
|--------|------|----------|
| `validateStep1(map)` | 1 | projectId 패턴, 필수 필드 |
| `validateStep2(map)` | 2 | 지점·사용자, 세션 여유율 |
| `validateStep3(map)` | 3 | 시나리오 활성, baseline, stress≥baseline |
| `validateStep4~7(map)` | 4~7 | 필수 입력·범위 |
| `validateStep8(map, all)` | 8 | STEP 1~7 완료 여부 |

반환: `CapNewStepValidationCDTO` — valid, errors[], warnings[]

### 4.4 CapNewDerivationService

기존 support 참조만. **수정 금지 대상 코드를 호출한다.**

| 메서드 | STEP | 참조 클래스 | 산출 |
|--------|------|------------|------|
| `enrichStep4` | 4 | `NsightCapacityDerivation`, `VmProfile` | vmTheoreticalTps, vmAdjustedTps, minRequiredAp |
| `enrichStep5` | 5 | 내부 AP 산정 | deploymentAp, scenarioApTable, drJudgment |
| `enrichStep6` | 6 | `DCCapacity`, `JvmSizingGuide` | totalThreads, maxThreads, jvmRecommendation |
| `enrichStep7` | 7 | `NsightDbPoolDerivation`, `CapNewWarPoolAllocation` | poolPerVm, totalDbSessions, warPoolStatus |
| `enrichStep8` | 8 | STEP 1~7 집계 | overallJudgment, headline, riskSummary |

#### STEP 4 산정식 (구현)

```text
vmTheoreticalTps = vmCores × tpsPerCore
vmAdjustedTps    = floor(vmTheoreticalTps × cpuTarget × virtFactor × opsFactor / perfSafety)
minRequiredAp    = ceil(designPeakTps / vmAdjustedTps)
```

#### STEP 6 산정식 (구현)

```text
totalThreads         = targetTps × avgThreadHoldSec × threadMarginRate
threadsPerAp         = ceil(totalThreads / deploymentAp)
recommendedMaxThreads = ceil(threadsPerAp × maxThreadMarginRate)
```

#### STEP 7 산정식 (구현)

```text
tpsBasedPool    = ceil(apTps × avgDbHoldSec × dbUsageRate × poolSafetyFactor)
threadBasedCap  = ceil(maxThreads × threadToDbRatio)
capacityPool    = min(tpsBasedPool, threadBasedCap)
finalPoolPerVm  = max(minPoolPerVm, capacityPool)
totalDbSessions = finalPoolPerVm × deploymentAp
```

### 4.5 CapNewCompareService

| 항목 | 내용 |
|------|------|
| 입력 | `CapNewCompareRequest` (scenarioIds[], baselineScenarioId) |
| 전제 | COMPLETED 또는 APPROVED, STEP 8 존재 |
| 출력 | matrix[], diffHighlights[], recommendation |
| 로직 | headline 지표 추출·기준 대비 차이·권장안 문장 생성 |

### 4.6 CapNewApprovalService

| 메서드 | 상태 전이 | 부가 |
|--------|----------|------|
| `approve(id, request)` | COMPLETED → APPROVED | CAP_NEW_APPROVAL INSERT, SNAPSHOT_JSON |
| `revoke(id, request)` | APPROVED → COMPLETED | REVOKE 이력 |
| `cloneVersion(id)` | — | 버전 증가(V1.0→V1.1), DRAFT 복제 |

PROD 환경: reviewer 필수. CRITICAL: overrideReason 필수.

### 4.7 CapNewEnvBridgeService

cap-new STEP 1~7 → `CapacityPlannerRequest` 변환 → `CapacityDesignService.analyze()` → ENV-002 prefill JSON

### 4.8 CapNewLegacyCompareService

cap-new payload → `CapacityCalculationCDTO` → `ASMSC71001.calculate(ALL)` → 운영 baseline TPS 행과 STEP 8 headline 비교

### 4.9 CapNewVmCompareService

STEP 4 보정계수 + STEP 5 센터 규칙으로 8C/16C/32C Profile별 AP·Core·장애범위 비교

### 4.10 CapNewExcelExportService

Apache POI 기반. 시트: 요약, STEP별, AP/DR, WAR Pool, VM 대안, 비교 매트릭스

### 4.11 CapNewWarPoolAllocation

| 항목 | 내용 |
|------|------|
| 입력 | warAllocations[], apTps, maxThreads, deploymentAp |
| 로직 | 비중별 TPS·Thread 분배 → WAR별 `NsightDbPoolDerivation` |
| 출력 | warPoolTotalSessions, warPoolStatus (NORMAL/CRITICAL) |
| CRITICAL | DB Session 한도 초과 시 권장 조치 4건 |

---

## 5. DTO 설계

### 5.1 주요 Request

| 클래스 | 용도 |
|--------|------|
| `CapNewCreateScenarioRequest` | POST /scenarios |
| `CapNewStepSaveRequest` | PUT /step/{n} — payload Map |
| `CapNewCompareRequest` | POST /compare |
| `CapNewApproveRequest` | POST /approve |
| `CapNewRevokeRequest` | POST /revoke |
| `CapNewExcelExportRequest` | POST /export/excel |

### 5.2 주요 Response CDTO

| 클래스 | 용도 |
|--------|------|
| `CapNewApiResponse<T>` | 공통 래퍼 success/message/data |
| `CapNewScenarioCDTO` | 상세 — stepPayload, stepTrack, cascadeImpact |
| `CapNewScenarioSummaryCDTO` | 목록 요약 |
| `CapNewDefaultsCDTO` | /defaults 전체 |
| `CapNewStepTrackStatusCDTO` | step, label, state, symbol, hint, issues |
| `CapNewCascadeImpactCDTO` | affectedSteps[], changes[], message |
| `CapNewCompareCDTO` | 비교 결과 |
| `CapNewApprovalCDTO` | 확정 이력 |
| `CapNewEnvHandoffCDTO` | ENV 연동 |
| `CapNewLegacyCompareCDTO` | 기존 CAP 대조 |
| `CapNewVmCompareCDTO` | VM 대안 |

### 5.3 Persistence Row

| 클래스 | 테이블 |
|--------|--------|
| `CapNewScenarioRow` | CAP_NEW_SCENARIO |
| `CapNewApprovalRow` | CAP_NEW_APPROVAL |
| `CapNewScenarioTemplateRow` | CAP_NEW_SCENARIO_TEMPLATE |

---

## 6. DAO·Mapper

| DAO | Mapper | XML |
|-----|--------|-----|
| `CapNewScenarioDao` | `CapNewScenarioMapper` | `CapNewScenarioMapper.xml` |
| `CapNewApprovalDao` | `CapNewApprovalMapper` | `CapNewApprovalMapper.xml` |
| `CapNewScenarioTemplateDao` | `CapNewScenarioTemplateMapper` | `CapNewScenarioTemplateMapper.xml` |

상세 SQL은 `cap-new-04-SQL-DB-설계서.md` 참조.

---

## 7. Support 클래스

### 7.1 CapNewStep (enum)

STEP 1~8 — stepNumber, payloadKey(`step{n}`), label

### 7.2 CapNewScenarioStatus (enum)

`DRAFT`, `COMPLETED`, `APPROVED`

### 7.3 CapNewStepSnapshot

연쇄 재산정 시 핵심 지표 스냅샷·diff — targetTps, deploymentAp, maxThreads, poolPerVm, totalDbSessions

### 7.4 CapNewBizException

비즈니스 검증 실패. Controller에서 400 변환.

---

## 8. tcf-ui 연동

### 8.1 Relay (개발 환경)

`tcf-ui` `OcRelayService`:

| 메서드 | 대상 |
|--------|------|
| `relayCapNewGet` | GET 요청 프록시 |
| `relayCapNewPut` | PUT 요청 프록시 |
| `relayCapNewPost` | POST JSON |
| `relayCapNewPostBinary` | Excel binary |

### 8.2 JavaScript 모듈

| 파일 | 역할 |
|------|------|
| `oc-cap-new-wizard.js` | STEP UI·저장·트랙·연쇄 패널 |
| `oc-cap-new-export.js` | Excel 다운로드 |
| `oc-cap-new-env-handoff.js` | sessionStorage → ENV-002 |
| `oc-cap-new-legacy-compare.js` | 기존 CAP 대조 모달 |
| `oc-cap-new-vm-compare.js` | VM 대안 모달 |

### 8.3 ENV Handoff 흐름

```text
cap-new STEP 8 [ENV 연동] 클릭
  → GET /env-handoff
  → sessionStorage.setItem('nsight.env.capacityRequest', ...)
  → sessionStorage.setItem('nsight.env.capacityView', ...)
  → location.href = '/oc/env-002.html'
  → oc-env-planner.js applyDefaults()
```

---

## 9. 트랜잭션·동시성

| 작업 | 트랜잭션 | 비고 |
|------|---------|------|
| createScenario | `@Transactional` | INSERT |
| saveStep | `@Transactional` | UPDATE + 연쇄 enrich |
| approve/revoke | `@Transactional` | STATUS 변경 + APPROVAL INSERT |
| deleteScenario | `@Transactional` | DRAFT만 DELETE |

동시 편집: Last-Write-Wins. 운영 환경에서는 DRAFT 상태 편집 잠금(향후 과제) 검토.

---

## 10. 테스트 포인트

| 영역 | 검증 항목 |
|------|----------|
| StepRule | projectId 패턴, stress≥baseline, 세션 계산 |
| Derivation | STEP 4~7 산출값, 기존 CAP 대조 일치 |
| Cascade | STEP 2 변경 → STEP 3~8 재산정 |
| Approval | PROD reviewer, CRITICAL override |
| Compare | 2+ 시나리오 matrix |
| WarPool | 합계 Session 한도 초과 CRITICAL |
| Excel | 시트 구성·한글 헤더 |

---

## 11. 파일 목록 (구현 기준)

```
tcf-oc/src/main/java/.../capnew/
  entry/controller/CapNewApiController.java
  application/service/*.java          (11개)
  application/rule/CapNewStepRule.java
  application/dto/*.java                (20+)
  persistence/dao/*.java              (3개)
  persistence/mapper/*.java           (3개)
  persistence/dto/*.java                (3개)
  support/*.java                      (6개)

tcf-oc/src/main/resources/mapper/oc/
  CapNewScenarioMapper.xml
  CapNewApprovalMapper.xml
  CapNewScenarioTemplateMapper.xml

tcf-ui/src/main/resources/static/oc/cap-new/
  index.html, wizard.html, compare.html, approved.html
```

---

## 12. 장 요약

cap-new는 **Controller → WizardService → StepRule + DerivationService → DAO → Mapper** 구조의 REST 기반 OC 기능이다. TCF 6계층(Handler→Facade→Service→Rule→DAO→Mapper) 패턴을 따르되, Online Handler 대신 REST Controller가 진입점이다. 핵심 비즈니스는 STEP 저장 시 **검증 → 산정 enrich → 연쇄 재산정 → 트랙 빌드**이며, 기존 용량산정 엔진은 참조만 한다.
