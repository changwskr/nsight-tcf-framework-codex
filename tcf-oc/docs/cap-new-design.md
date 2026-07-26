# TCF-OC NEW 용량산정 (cap-new) 설계

> Phase 0 고정 문서 · 기존 CAP(`/api/oc/capacity`)·ENV(`/api/oc/env`)와 **완전 분리**

## 1. 목적

8단계 Wizard로 용량산정 시나리오를 작성·저장·비교·확정한다.  
기존 `DCCapacity`, `ACMSC71001`, `CapacityPlannerService` 등은 수정하지 않는다.

## 2. 패키지·API 경로

| 구분 | 값 |
|------|-----|
| Java 패키지 | `com.nh.nsight.marketing.oc.capnew` |
| REST prefix | `/api/oc/cap-new` |
| UI 경로 | `/oc/cap-new/index.html`, `/oc/cap-new/wizard.html` |
| Relay (tcf-ui) | `/api/oc/cap-new/*` → tcf-oc 동일 경로 |

## 3. 8단계 Wizard 매핑

| STEP | 화면명 | payload 키 | Phase 1 |
|------|--------|------------|---------|
| 1 | 프로젝트 기본정보 | `step1` | 입력·검증 |
| 2 | 사용자·세션 조건 | `step2` | 입력·자동계산·검증 |
| 3 | 동시요청·TPS 시나리오 | `step3` | 프리셋·자동계산·검증 |
| 4 | 업무복잡도·CPU·VM | `step4` | 입력·VM TPS 산정 |
| 5 | AP 대수·센터·DR | `step5` | 센터·DR·AP 표 |
| 6 | WAS Thread·JVM | `step6` | DCCapacity WAS + JVM |
| 7 | DB Pool·DB Session | `step7` | NsightDbPoolDerivation + WAR별 Pool 배분 |
| 8 | 종합 결과·비교·확정 | `step8` | riskSummary·COMPLETED |

`STEP_PAYLOAD` (CLOB/JSON) 예:

```json
{
  "step1": { "projectId": "NSIGHT-MP", "scenarioName": "2026 운영 기준안", ... },
  "step2": { "branchCount": 6000, "totalUsers": 36000, ... },
  "step3": { "scenarios": [...], "operatingBaseline": "DESIGN_PEAK", ... }
}
```

## 4. REST API (Phase 1)

| Method | Path | 설명 |
|--------|------|------|
| GET | `/defaults` | Wizard 기본값·코드목록 |
| GET | `/scenarios` | 시나리오 목록 (요약) |
| POST | `/scenarios` | 신규 시나리오 생성 (STEP 1) |
| GET | `/scenarios/{id}` | 시나리오 상세 |
| PUT | `/scenarios/{id}/step/{n}` | STEP n 저장·검증·자동계산 |
| DELETE | `/scenarios/{id}` | 초안 삭제 (DRAFT만) |
| POST | `/compare` | 시나리오 2개 이상 비교 (Phase 3) |
| GET | `/scenarios?status=COMPLETED` | 상태 필터 목록 |
| POST | `/scenarios/{id}/approve` | 최종 확정 (Phase 4) |
| POST | `/scenarios/{id}/revoke` | 확정 취소 |
| POST | `/scenarios/{id}/clone` | 새 버전 복제 |
| GET | `/approvals` | 확정 이력 전체 |

응답 래퍼: `CapNewApiResponse<T>` — `{ success, message, data }`

## 5. DB ERD (H2)

```
CAP_NEW_SCENARIO
├── SCENARIO_ID      PK
├── PROJECT_ID, PROJECT_NAME, SCENARIO_NAME
├── TARGET_ENV, BASE_DATE, VERSION_NO, AUTHOR, DESCRIPTION, PURPOSE
├── STATUS           DRAFT | COMPLETED | APPROVED
├── CURRENT_STEP     1~8
├── STEP_PAYLOAD     CLOB (JSON)
├── CREATED_AT, UPDATED_AT
```

Phase 2+: `CAP_NEW_APPROVAL` 확정 이력 테이블 (Phase 4 구현).

```
CAP_NEW_APPROVAL
├── APPROVAL_ID, SCENARIO_ID, PROJECT_ID
├── SCENARIO_NAME, VERSION_NO, ACTION (APPROVE|REVOKE)
├── APPROVER, REVIEWER, APPROVAL_NOTE, OVERALL_JUDGMENT
├── SNAPSHOT_JSON, CREATED_AT
```

## 6. 검증 규칙 (Phase 1 구현 범위)

### STEP 1
- `projectId`: 영문·숫자·하이픈
- 필수: projectId, projectName, scenarioName, targetEnv, purpose

### STEP 2
- 지점 수·지점당 사용자 ≥ 1
- 세션 여유율 0~100%
- `totalUsers = branchCount×userPerBranch + hqUsers + otherUsers`
- `designedSessions = ceil(totalUsers × (1 + marginRate))`

### STEP 3
- 최소 1개 시나리오 활성
- 운영 기준 시나리오 1개 지정
- 스트레스 TPS ≥ 운영 기준 TPS

## 7. 기존 코드와의 관계

- 계산 엔진: Phase 2부터 `NsightCapacityDerivation` 등 **참조만** (직접 수정 없음)
- UI: `capacity.html` 유지, NEW는 별도 메뉴 **「NEW 용량산정」**
- tcf-ui Relay: `OcRelayService.relayCapNewGet/Put/Post` 추가

## 8. 후속 Phase

| Phase | 내용 |
|-------|------|
| 2 | ✅ STEP 4~7 산정 엔진 연동 (`CapNewDerivationService`) |
| 3 | ✅ compare.html, `POST /compare` 시나리오 비교 |
| 4 | ✅ approved.html, 확정·버전·이력 |
| 5 | ✅ ENV 연동, Excel export |
| 6 | ✅ HELP 화면 맵, verifyHelp, 기존 CAP 대조 |


## 9. Phase 2 산정 엔진 (구현)

- `CapNewDerivationService` — `NsightCapacityDerivation`, `NsightDbPoolDerivation`, `JvmSizingGuide`, `DCCapacity.calculateWasThreadOnly` 참조
- STEP 8 저장 시 `STATUS=COMPLETED`
- STEP 4: VM 이론/보정 TPS, 최소 Core
- STEP 5: 시나리오별 AP·DR 판정 테이블
- STEP 6~7: WAS Thread·JVM·Hikari Pool 자동 산정

## 10. Phase 3 시나리오 비교 (구현)

- `CapNewCompareService` — COMPLETED/APPROVED + STEP 8 필수
- `POST /compare` body: `{ scenarioIds: [...], baselineScenarioId?: "..." }`
- UI: `/oc/cap-new/compare.html` — 체크박스 선택 · 기준 시나리오 · 지표 매트릭스
- 차이 요약(`diffHighlights`) · 운영 권장안(`recommendation`) 자동 생성

## 11. Phase 4 확정·버전 (구현)

- `CAP_NEW_APPROVAL` 테이블 — APPROVE/REVOKE 이력·스냅샷
- `POST /scenarios/{id}/approve` — COMPLETED → APPROVED (PROD는 검토자 필수, CRITICAL은 사유+override)
- `POST /scenarios/{id}/revoke` — APPROVED → COMPLETED
- `POST /scenarios/{id}/clone` — 버전 자동 증가(V1.0→V1.1) 복제본 DRAFT 생성
- UI: `/oc/cap-new/approved.html`

## 12. Phase 5 ENV 연동·Excel export (구현)

### ENV Bridge
- `CapNewEnvBridgeService` — cap-new STEP 1~7 → `CapacityPlannerRequest` 변환 후 `CapacityDesignService.analyze()` 호출
- `GET /scenarios/{id}/env-handoff` — ENV-002 prefill용 `capacityRequest`·`capacityView` JSON
- UI: `oc-cap-new-env-handoff.js` — `sessionStorage` (`nsight.env.capacityRequest/View`) 저장 후 `/oc/env-002.html` 이동
- ENV-002: `oc-env-planner.js` — handoff 시 저장된 request를 `applyDefaults()`로 폼에 반영

### Excel Export
- `CapNewExcelExportService` (Apache POI) — 시나리오 요약·STEP별·AP/DR 시트, 비교 매트릭스
- `POST /scenarios/{id}/export/excel` — 단일 시나리오
- `POST /export/excel` body `{ exportType: "COMPARE", scenarioIds, baselineScenarioId }` — 비교
- tcf-ui Relay: `OcRelayService.relayCapNewPostBinary()`
- UI: `oc-cap-new-export.js` — Wizard STEP 8·approved·compare 화면

### API 추가
| Method | Path |
|--------|------|
| GET | `/scenarios/{id}/env-handoff` |
| POST | `/scenarios/{id}/export/excel` |
| POST | `/export/excel` |

## 13. Phase 6 품질·문서 (구현)

### HELP
- `tcf-help/docs/20-oc-용량-환경/05-cap-new-wizard.md` — cap-new 화면·Wizard·API 안내
- `help-index.yml` — `cap-new-wizard` 항목 + 4개 화면 (`index`, `wizard`, `compare`, `approved`)
- `help-screen-overrides.yml` — cap-new 화면별 docId·설계서 링크
- `catalog-overrides.yml` — `tcf-oc/docs/cap-new-design.md` 카탈로그 등록
- `:tcf-help:verifyHelp` — help-index·화면 맵 품질 검증

### 기존 CAP 대조
- `CapNewLegacyCompareService` — cap-new STEP 1~7 → `CapacityCalculationCDTO` 변환 후 `ASMSC71001.calculate(ALL)` 호출
- 운영 기준 TPS 시나리오(STEP 3 `operatingBaseline`) 행과 cap-new STEP 8 headline 지표 비교
- 비교 지표: 목표 TPS, 배포 AP, maxThreads, Pool/VM, DB Session 합계
- `GET /scenarios/{id}/legacy-compare`
- UI: `oc-cap-new-legacy-compare.js` — Wizard STEP 8 「기존 CAP 대조」버튼

### API 추가
| Method | Path |
|--------|------|
| GET | `/scenarios/{id}/legacy-compare` |

## 15. VM 대안 비교 (§12 구현)

운영 기준 TPS에 대해 8C/16C/32C 등 VM Profile별 VM TPS·필요 AP·전체 Core·장애범위를 비교합니다.

### 산정
- `CapNewVmCompareService` — STEP 4 보정계수·STEP 5 센터/DR 규칙으로 VM별 AP 산정
- 기본 비교 대상: `8CORE-64GB`, `16CORE-128GB`, `32CORE-256GB` + 현재 선택 VM
- 판단: 8C 이하 확장성 우수 · 16C 운영 권장 · 32C 이상 집중 위험

### API·UI
- `GET /scenarios/{id}/vm-compare?profiles=8CORE-64GB,16CORE-128GB` (profiles 생략 시 기본 3종)
- Wizard STEP 8 「VM 대안 비교」버튼 + `oc-cap-new-vm-compare.js`
- Excel — `VM 대안` 시트

### API 추가
| Method | Path |
|--------|------|
| GET | `/scenarios/{id}/vm-compare` |

## 16. 입력값 변경 영향·하위 단계 재산정 (§13 구현)

앞 단계 저장 시 이미 존재하는 하위 STEP payload를 **자동 재산정**합니다.

### 연쇄 규칙
| 저장 STEP | 재산정 대상 (존재 시) |
|-----------|----------------------|
| 2 | 3 → 4 → 5 → 6 → 7 → 8 |
| 3 | 4 → 5 → 6 → 7 → 8 |
| 4 | 5 → 6 → 7 → 8 |
| 5 | 6 → 7 → 8 |
| 6 | 7 → 8 |
| 7 | 8 |

### 구현
- `CapNewWizardService.saveStep` — 저장 후 `cascadeDownstream()` 호출
- `CapNewStepSnapshot` — 핵심 지표 스냅샷·차이 (TPS, AP, Thread, Pool, DB Session 등)
- 응답 `CapNewScenarioCDTO.cascadeImpact` — 영향 STEP, 변경 목록, 메시지
- UI: Wizard 저장 시 노란색 **하위 단계 자동 재산정** 패널 표시

### 예시 메시지
`VM Profile 16CORE-128GB → 8CORE-64GB — 하위 4개 단계 자동 재산정`

## 17. Wizard 트랙 상태 (● ✓ ! × ○)

시나리오 상세·저장 응답에 `stepTrack[]`를 포함해 8단계 진행 상태를 표시합니다.

| 심볼 | state | 조건 |
|------|-------|------|
| ● | active | UI 현재 단계 (클라이언트 오버레이) |
| ✓ | done | `step{n}` 저장·검증 통과 |
| ! | warn | 검증 경고 또는 STEP 5~8 위험 판정 |
| × | error | 미입력(중간 공백) 또는 검증 오류 |
| ○ | pending | 아직 저장되지 않은 후속 단계 |

- `CapNewWizardService.buildStepTrack()` — 저장 payload 기준 `CapNewStepRule` 재검증
- `CapNewStepTrackStatusCDTO` — step, label, state, symbol, hint, issues
- UI: `oc-cap-new-wizard.js` `renderTrack()` + 범례 + tooltip(`title`)

## 18. P1 화면 UX 보강 (설계서 대비)

| 항목 | 구현 |
|------|------|
| 상단 컨텍스트 바 | 프로젝트·시나리오·환경·목적·상태·버전 |
| STEP 3 실시간 TPS | 입력 변경 시 실요청자·목표 TPS 즉시 갱신 |
| STEP 8 단계별 결과표 | STEP 1~7 요약 + [보기] 단계 이동 |
| STEP 8 시나리오 비교표 | `scenarioResults` 기반 AP·Thread·Pool |
| 판정 한글화 | 정상/주의/위험/미확인 배지 |

## 19. P2 화면 UX 보강

| 항목 | 구현 |
|------|------|
| 트랙 클릭 이동 | 저장된 단계(✓/!/×) 클릭·Enter로 이동 |
| STEP 6 JVM·Connector 표 | maxThreads, minSpare, acceptCount, maxConnections, JVM 표 |
| STEP 2 산정 방식 | 지점 기준 / 전체 사용자 직접 (`calcMode`) |
| STEP 2 Session Timeout | 60·90·직접 입력 프리셋 |
| STEP 4 표 선택 | 업무 유형·VM Profile 라디오 표 |

## 20. P3 화면 UX 보강

| 항목 | 구현 |
|------|------|
| STEP 1 기존 시나리오 불러오기 | `GET /scenarios` 목록 선택 → STEP 1 필드 적용 (저장 시 반영) |
| STEP 1 초기화 | `defaults.step1` 기본값으로 폼 리셋 |
| STEP 3 사용자 정의 시나리오 | `CUSTOM_n` 행 추가·라벨·요청률·응답 편집·삭제 |
| STEP 3 DR 장애 시나리오 | `DR_FAULT` 프리셋 (기본 비활성) |
| STEP 3 성능시험 기준 | `performanceTestTargets[]` 다중 체크박스 (활성 시나리오 대상) |
| STEP 8 시나리오 복사 | `POST /scenarios/{id}/clone` (COMPLETED·APPROVED) |
| STEP 8 검토 요청 | `/oc/cap-new/approved.html?id=` 이동 |

## 14. WAR별 Pool 배분 (§10.4 구현)

동일 Tomcat에 여러 업무 WAR가 배포될 때 WAR마다 HikariCP Pool이 생성되므로, **합계 DB Session**을 별도 검증합니다.

### 산정
- `CapNewWarPoolAllocation` — WAR 비중에 따라 AP TPS·Thread를 분배 후 `NsightDbPoolDerivation`으로 WAR별 Pool 산정
- `warPoolTotalSessions = Σ(WAR Pool/VM × 배포 AP)`
- 한도 초과 시 `warPoolStatus=CRITICAL` 및 권장 조치 4건 제시

### STEP 7 입력
- `warPoolEnabled` (기본 true)
- `warAllocations[]` — `{ warCode, label, weightPercent, enabled }`
- `minPoolPerWar` (기본 15)

### UI
- Wizard STEP 7 — WAR 비중 편집 테이블 + 산정 결과·합계·권장 조치
- STEP 8 headline — `warPoolTotalSessions`, `warPoolStatus`
- Excel — `WAR Pool` 시트

