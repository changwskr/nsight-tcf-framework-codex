# NSIGHT TCF-OC NEW 용량산정 거래설계서

| 항목 | 내용 |
|------|------|
| **문서 ID** | OC-CAP-NEW-TXN-001 |
| **버전** | V1.0 |
| **작성일** | 2026-07-15 |
| **대상 모듈** | tcf-oc / tcf-ui |
| **관련 문서** | `cap-new-01-화면설계서.md`, `cap-new-03-프로그램설계서.md` |
| **문서 목적** | cap-new 기능의 화면–API–권한 매핑, REST 인터페이스 계약, 기존 TCF Online 연계를 정의한다. |

---

## 1. 도입

### 1.1 거래 유형 분류

cap-new는 **REST API 중심** 관리 기능이다. 업무 WAR의 TCF Online(`POST /{bc}/online`) 표준 전문과 달리, tcf-ui가 tcf-oc REST API를 직접 호출한다.

| 구분 | 프로토콜 | 경로 prefix | 용도 |
|------|---------|-------------|------|
| **Primary** | REST JSON | `/api/oc/cap-new` | Wizard CRUD·산정·비교·확정 |
| **Relay** | REST (tcf-ui) | `/api/oc/cap-new/*` → tcf-oc | 개발 환경 프록시 |
| **Bridge** | REST + sessionStorage | ENV handoff | cap-new → ENV-002 설정 점검 |
| **Legacy** | 내부 서비스 호출 | `ASMSC71001.calculate` | 기존 CAP 산정 대조 |
| **TCF Online** | — | 해당 없음 | cap-new는 Handler 미사용 |

> cap-new는 OM Service Catalog·거래코드·Header 7항 기반 TCF Online 거래가 **아니다**.  
> 다만 ENV 연동·기존 CAP 대조 시 내부적으로 기존 용량산정 서비스를 호출한다.

### 1.2 업무 식별

| 항목 | 값 |
|------|-----|
| 업무코드 | OC |
| 업무명 | Operations Center |
| Context Path | /oc |
| 기능 도메인 | cap-new (NEW 용량산정) |
| 패키지 | `com.nh.nsight.marketing.oc.capnew` |

---

## 2. 화면–API–기능 매핑표

| 화면 ID | 기능 | functionCode | HTTP Method | API Path | 비고 |
|---------|------|--------------|-------------|----------|------|
| CAPN-001 | 기본값 조회 | CAPN001_DEF | GET | `/defaults` | Wizard 초기화 |
| CAPN-001 | 템플릿 목록 | CAPN001_TPL | GET | `/templates` | — |
| CAPN-001 | 템플릿 상세 | CAPN001_TPL_D | GET | `/templates/{code}` | seed payload |
| CAPN-001 | 시나리오 목록 | CAPN001_LIST | GET | `/scenarios?status=` | 상태 필터 |
| CAPN-001 | 시나리오 생성 | CAPN001_CREATE | POST | `/scenarios` | STEP 1 |
| CAPN-001 | 시나리오 삭제 | CAPN001_DEL | DELETE | `/scenarios/{id}` | DRAFT만 |
| CAPN-002 | 시나리오 상세 | CAPN002_GET | GET | `/scenarios/{id}` | stepTrack 포함 |
| CAPN-002 | STEP 저장 | CAPN002_SAVE | PUT | `/scenarios/{id}/step/{n}` | 검증·연쇄 |
| CAPN-003 | 시나리오 비교 | CAPN003_CMP | POST | `/compare` | 2개 이상 |
| CAPN-004 | 확정 | CAPN004_APPR | POST | `/scenarios/{id}/approve` | COMPLETED→APPROVED |
| CAPN-004 | 확정 취소 | CAPN004_REV | POST | `/scenarios/{id}/revoke` | APPROVED→COMPLETED |
| CAPN-004 | 버전 복제 | CAPN004_CLONE | POST | `/scenarios/{id}/clone` | V1.0→V1.1 |
| CAPN-004 | 확정 이력 | CAPN004_HIST | GET | `/approvals` | 전체 |
| CAPN-004 | 시나리오 이력 | CAPN004_HIST_S | GET | `/scenarios/{id}/approvals` | 개별 |
| CAPN-002-S8 | ENV 연동 | CAPN008_ENV | GET | `/scenarios/{id}/env-handoff` | ENV-002 prefill |
| CAPN-002-S8 | 기존 CAP 대조 | CAPN008_LEG | GET | `/scenarios/{id}/legacy-compare` | — |
| CAPN-002-S8 | VM 대안 비교 | CAPN008_VM | GET | `/scenarios/{id}/vm-compare` | profiles 파라미터 |
| CAPN-002-S8 | Excel 단건 | CAPN008_XLS | POST | `/scenarios/{id}/export/excel` | binary |
| CAPN-003 | Excel 비교 | CAPN003_XLS | POST | `/export/excel` | COMPARE 타입 |

---

## 3. REST API 계약

### 3.1 공통 응답 래퍼

```json
{
  "success": true,
  "message": "STEP 3 저장 완료",
  "data": { }
}
```

오류 시 `success: false`, HTTP 400, `CapNewBizException` 메시지 반환.

### 3.2 GET /defaults

**응답 data 구조 (요약)**

```json
{
  "step1": { "projectId": "NSIGHT-MP", "targetEnv": "PROD", "purpose": "NEW_BUILD", ... },
  "step2": { "branchCount": 6000, "userPerBranch": 6, "sessionMarginRate": 0.30, ... },
  "step3": { "presets": [...], "operatingBaseline": "DESIGN_PEAK", ... },
  "step4": { "businessTypeCode": "SINGLE_VIEW", "vmProfileCode": "16CORE-128GB", ... },
  "codes": {
    "targetEnv": ["DEV", "STG", "PROD", "DR"],
    "purpose": ["NEW_BUILD", "SCALE_REVIEW", "CONFIG_CHECK", "DR_VERIFY"],
    "businessTypes": [...],
    "vmProfiles": [...]
  }
}
```

### 3.3 POST /scenarios — 시나리오 생성

**요청**

```json
{
  "projectId": "NSIGHT-MP",
  "projectName": "NSIGHT 마케팅플랫폼",
  "scenarioName": "2026 운영용량 기준안",
  "targetEnv": "PROD",
  "baseDate": "2026-07-12",
  "versionNo": "V1.0",
  "author": "홍길동",
  "description": "6,000지점 운영 기준",
  "purpose": "NEW_BUILD",
  "templateCode": "NH_6000_BRANCH"
}
```

| 필드 | 필수 | 설명 |
|------|------|------|
| projectId | Y | 영문·숫자·하이픈 |
| projectName | Y | — |
| scenarioName | Y | — |
| targetEnv | Y | DEV/STG/PROD/DR |
| purpose | Y | 산정 목적 |
| templateCode | N | 지정 시 seed payload 적용 |

**응답**: `CapNewScenarioCDTO` — scenarioId, status=DRAFT, currentStep, stepPayload, stepTrack

### 3.4 PUT /scenarios/{id}/step/{n} — STEP 저장

**요청**

```json
{
  "payload": {
    "branchCount": 6000,
    "userPerBranch": 6,
    "sessionMarginRate": 0.30,
    ...
  }
}
```

**응답 data 주요 필드**

| 필드 | 설명 |
|------|------|
| stepPayload | 전체 STEP JSON (step1~step8) |
| currentStep | 현재 진행 단계 |
| status | DRAFT / COMPLETED (STEP 8 완료 시) |
| stepTrack[] | 8단계 트랙 상태 |
| cascadeImpact | 하위 재산정 영향 (있을 때) |
| validation | 검증 오류·경고 목록 |

### 3.5 POST /compare — 시나리오 비교

**요청**

```json
{
  "scenarioIds": ["CAP-NEW-AB12CD34", "CAP-NEW-EF56GH78"],
  "baselineScenarioId": "CAP-NEW-AB12CD34"
}
```

**응답**: `CapNewCompareCDTO` — matrix[], diffHighlights[], recommendation

### 3.6 POST /scenarios/{id}/approve — 확정

**요청**

```json
{
  "approver": "김운영",
  "reviewer": "이검토",
  "approvalNote": "2026 운영 기준안 확정",
  "overrideReason": null
}
```

| 규칙 | 설명 |
|------|------|
| 상태 | COMPLETED만 확정 가능 |
| PROD | reviewer 필수 |
| CRITICAL | overrideReason 필수 |
| 이력 | CAP_NEW_APPROVAL INSERT, SNAPSHOT_JSON 저장 |

### 3.7 GET /scenarios/{id}/env-handoff — ENV 연동

**응답**: `CapNewEnvHandoffCDTO`

```json
{
  "scenarioId": "CAP-NEW-AB12CD34",
  "capacityRequest": { },
  "capacityView": { }
}
```

tcf-ui `oc-cap-new-env-handoff.js`가 sessionStorage 저장 후 `/oc/env-002.html` 이동.

### 3.8 GET /scenarios/{id}/legacy-compare — 기존 CAP 대조

cap-new STEP 1~7 → `CapacityCalculationCDTO` 변환 → `ASMSC71001.calculate(ALL)` 호출 후 지표 비교.

비교 지표: 목표 TPS, 배포 AP, maxThreads, Pool/VM, DB Session 합계

---

## 4. STEP Payload 스키마

`CAP_NEW_SCENARIO.STEP_PAYLOAD` (CLOB JSON) 구조:

```json
{
  "step1": {
    "projectId": "string",
    "projectName": "string",
    "scenarioName": "string",
    "targetEnv": "PROD",
    "baseDate": "2026-07-12",
    "versionNo": "V1.0",
    "author": "string",
    "description": "string",
    "purpose": "NEW_BUILD"
  },
  "step2": {
    "calcMode": "BRANCH",
    "branchCount": 6000,
    "userPerBranch": 6,
    "hqUsers": 0,
    "otherUsers": 0,
    "totalUsers": 36000,
    "sessionMarginRate": 0.30,
    "sessionTimeoutMin": 60,
    "designedSessions": 46800
  },
  "step3": {
    "scenarios": [
      {
        "code": "DESIGN_PEAK",
        "label": "설계 피크",
        "enabled": true,
        "concurrentRate": 0.10,
        "responseTimeSec": 3,
        "concurrentUsers": 3600,
        "targetTps": 1200
      }
    ],
    "operatingBaseline": "DESIGN_PEAK",
    "performanceTestTargets": ["NORMAL", "PEAK", "DESIGN_PEAK", "STRESS"]
  },
  "step4": {
    "businessTypeCode": "SINGLE_VIEW",
    "vmProfileCode": "16CORE-128GB",
    "tpmcPerTps": 3000,
    "tpsPerCore": 36,
    "vmTheoreticalTps": 576,
    "vmAdjustedTps": 308,
    "designPeakTps": 1200,
    "minRequiredAp": 4,
    "minRequiredCores": 34
  },
  "step5": {
    "centerMode": "ACTIVE_ACTIVE",
    "trafficSplit": "50:50",
    "drFullCapacity": true,
    "apSpareCount": 1,
    "deploymentAp": 8,
    "apPerCenter": 4,
    "scenarioApTable": [ ]
  },
  "step6": {
    "avgThreadHoldSec": 1.2,
    "threadMarginRate": 1.20,
    "totalThreads": 1728,
    "threadsPerAp": 216,
    "recommendedMaxThreads": 300,
    "jvmRecommendation": { }
  },
  "step7": {
    "apType": "SINGLE_VIEW",
    "avgDbHoldSec": 0.20,
    "poolPerVm": 40,
    "totalDbSessions": 320,
    "warPoolEnabled": true,
    "warAllocations": [ ]
  },
  "step8": {
    "overallJudgment": "WARN",
    "headline": { },
    "stepResults": [ ],
    "scenarioResults": [ ],
    "riskSummary": [ ]
  }
}
```

---

## 5. 검증 규칙 (거래 수준)

### STEP 1

| 규칙 ID | 조건 | 오류 메시지 |
|---------|------|------------|
| CAPN-V-001 | projectId 패턴 `^[A-Za-z0-9-]+$` | 프로젝트 ID 형식 오류 |
| CAPN-V-002 | 필수 필드 누락 | 필수 항목 미입력 |

### STEP 2

| 규칙 ID | 조건 |
|---------|------|
| CAPN-V-010 | branchCount, userPerBranch ≥ 1 |
| CAPN-V-011 | sessionMarginRate 0~1 |
| CAPN-V-012 | totalUsers 자동계산 정합 |

### STEP 3

| 규칙 ID | 조건 |
|---------|------|
| CAPN-V-020 | 활성 시나리오 ≥ 1 |
| CAPN-V-021 | operatingBaseline 1개 지정 |
| CAPN-V-022 | stress Tps ≥ baseline Tps |

### STEP 4~7

산정 엔진(`CapNewDerivationService`) 실행 후 enriched payload 반환. 위험 판정은 WARN/CRITICAL로 stepTrack에 반영.

### STEP 8

모든 필수 STEP(1~7) 완료 시 `STATUS=COMPLETED`.

---

## 6. 권한·채널 설계

cap-new는 tcf-ui OC 관리 화면으로, **OC 운영자·아키텍트** 권한을 가진 사용자만 접근한다.

| 역할 | 허용 기능 |
|------|----------|
| OC_VIEWER | 목록·상세 조회, 비교, Excel |
| OC_EDITOR | 시나리오 생성·STEP 저장·삭제(DRAFT) |
| OC_APPROVER | 확정·확정 취소 |
| OC_ADMIN | 템플릿 관리, 전체 이력 |

| 환경 | 확정 규칙 |
|------|----------|
| DEV/STG | approver만으로 확정 가능 |
| PROD | approver + reviewer 필수 |
| CRITICAL 판정 | overrideReason + 승인자 확인 |

> 구체적 menuId·functionCode 권한은 OM 메뉴관리 설계안과 연동하여 운영 반영 시 등록한다.

---

## 7. Timeout·오류 처리

| 구분 | 값 | 비고 |
|------|-----|------|
| REST 요청 Timeout | 30초 | STEP 저장·산정 포함 |
| Excel Export | 60초 | 대용량 시트 |
| 비교 API | 30초 | 5개 이하 시나리오 |
| 오류 코드 | HTTP 400 + message | CapNewBizException |
| 클라이언트 | toast/alert + 필드 하이라이트 | validation.errors |

TCF Online Timeout·거래로그·감사로그는 REST 호출에 **직접 적용되지 않는다**.  
운영 감사가 필요하면 tcf-oc 접근 로그·CAP_NEW_APPROVAL 이력으로 추적한다.

---

## 8. 내부 서비스 연계

| 연계 대상 | 호출 시점 | 용도 |
|----------|----------|------|
| `NsightCapacityDerivation` | STEP 4 | Core TPS, VM TPS |
| `DCCapacity.calculateWasThreadOnly` | STEP 6 | WAS Thread |
| `JvmSizingGuide` | STEP 6 | JVM 권장값 |
| `NsightDbPoolDerivation` | STEP 7 | Hikari Pool |
| `CapNewWarPoolAllocation` | STEP 7 | WAR별 Pool 합계 |
| `CapacityDesignService.analyze` | ENV handoff | ENV-002 prefill |
| `ASMSC71001.calculate` | legacy-compare | 기존 CAP 대조 |

기존 코드는 **참조만** 하며 수정하지 않는다 (`cap-new-design.md` §7).

---

## 9. OM 등록 체크리스트

cap-new는 TCF Online 거래가 아니므로 ServiceId·거래코드 OM 등록은 **해당 없음**.

| # | 항목 | 완료 |
|---|------|------|
| 1 | REST API 경로 Gateway/L4 라우팅 등록 | □ |
| 2 | tcf-ui Relay (`OcRelayService.relayCapNew*`) | □ |
| 3 | OC 메뉴 등록 (NEW 용량산정) | □ |
| 4 | functionCode 권한 매핑 | □ |
| 5 | HELP 카탈로그 (`cap-new-wizard`) | □ |
| 6 | 운영 PROD 확정 시 검토자 정책 | □ |

---

## 10. 정합성 검증 체크리스트

```text
□ 화면 JS API 경로 = 설계서 REST Path
□ STEP payload 키 = CapNewStepRule 검증 키
□ operatingBaseline = STEP 8 headline 기준 TPS
□ ENV handoff capacityRequest = ENV-002 폼 필드
□ legacy-compare 지표 = 기존 CAP 산정 결과
□ Excel 시트 = STEP 8 결과표 일치
□ cascadeImpact 재산정 STEP = 설계서 연쇄 규칙
```

---

## 11. 장 요약

cap-new는 REST API 기반 OC 관리 기능으로, 화면 functionCode와 HTTP Method·Path의 **삼각 정합**을 유지한다. TCF Online ServiceId·거래코드는 사용하지 않으며, ENV·기존 CAP과의 연계는 내부 서비스 Bridge로 처리한다. STEP payload JSON이 화면·API·DB·산정 엔진 간 공통 계약이다.
