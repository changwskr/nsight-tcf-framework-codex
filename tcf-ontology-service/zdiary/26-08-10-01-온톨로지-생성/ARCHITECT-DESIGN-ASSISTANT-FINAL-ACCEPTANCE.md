# Architect Design Assistant — Final Acceptance

- 작성일: 2026-08-10
- 기준: 지시서 Phase 4
- UI: `#/design` (`static/workbench/js/design.js`)
- API: `POST /api/ontology/design/recommend`

---

## 판정: **PASS** (Golden Scenario)

---

## Golden Scenarios (BootRun 8105)

| Transaction | 기대 | 실측 (대표 후보) |
|-------------|------|------------------|
| QUERY | S ServiceId | `mgcoa8888S0` operationMatch=true |
| CREATE | C ServiceId | `mgcoa9000C0` operationMatch=true |
| UPDATE | U (S로 묵시 폴백 금지) | `mgcoa9000U0` operationMatch=true |
| DELETE | D ServiceId | `mgcoa8888D0` operationMatch=true |

추가:

| 항목 | 결과 |
|------|------|
| MIXED/REPORT UI 옵션 | 존재 |
| Paging YES → pattern.paging status | UNRESOLVED |
| Backend pattern | DERIVED_PATTERN + field-level properties |
| Design Gate scope | DESIGN_BASELINE, 신규 ServiceId NOT_YET_IMPLEMENTED |
| Reference SERVICE Gate | 별도 `validate/service/{id}` |
| Context Export | prompt Markdown + baseline JSON/MD |

---

## 금지 위반 점검

| 금지 | 상태 |
|------|------|
| Candidate 없을 때 임의 생성 | 준수 (NO_MATCH) |
| 요구 Paging을 후보 VERIFIED로 둔갑 | 준수 (UNRESOLVED) |
| UPDATE→S 묵시 선택 | 준수 |
| UI가 Architecture 사실 하드코딩 (Pattern 핵심) | Backend 우선, UI fallback만 |
| Google Fonts | 제거됨 |

---

## 테스트

- `DesignRecommendationServiceTest` 4건 PASS
- `WorkbenchStaticUiTest` PASS
- Full suite 61 / 0 failures
