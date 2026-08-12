# Architecture Design Wizard Implementation Report

- 일자: 2026-08-10
- 설계서: `26-08-10-16-Architecture_Design_Wizard_ServiceId_DataTable_설계서.md`
- 대상: `tcf-ontology-service` Architect Workbench `#/design`

## Summary

Design Assistant를 **후보 추천 흐름**에서 **7-Step Architecture Design Wizard**로 재구현했다.

```text
Requirement → Classification → ServiceId → Data/Table
→ Application → Runtime/Policy → Gate/Export
```

## Backend

| 구성요소 | 경로 |
|---------|------|
| Service | `nhnis.ontology.design.DesignWizardService` |
| Controller | `nhnis.ontology.web.DesignWizardController` (`/api/ontology/design/*`) |
| Gate bridge | `OntologyCoreQueryController` `/validate/design` — Wizard body면 DesignWizard Gate |

### APIs

- `POST /api/ontology/design/session` (+ GET/PUT session)
- `POST /api/ontology/design/service-id/validate`
- `GET /api/ontology/design/programs`
- `GET /api/ontology/design/tables`
- `GET /api/ontology/design/table/{table}` (+ `/columns`)
- `POST /api/ontology/design/application`
- `POST /api/ontology/design/policy`
- `POST /api/ontology/design/validate`
- `POST /api/ontology/design/export` / `GET .../export/{sessionId}`

### Constraints honored

- ServiceId/Program: **PROPOSED only**, duplicate → REJECTED
- Tables: Ontology-known only; missing → UI `NEW_TABLE_PROPOSAL`
- Policy evidence gaps → `UNRESOLVED`
- Export rejects `undefined` / `[object Object]` / `NaN`

## Frontend

- `static/workbench/js/design.js` — 7-step wizard rewrite
- `static/workbench/js/api.js` — design* API client + PUT
- `static/workbench/css/workbench.css` — stepper/table/nav styles

## Tests

- `DesignWizardServiceTest` — SID mapping, duplicate, programs, tables, export golden
- `DesignWizardApiTest` — HTTP APIs + session export
- `WorkbenchStaticUiTest` — wizard markers in design.js

## Compatibility

- `POST /api/ontology/design/recommend` 유지
- `POST /api/ontology/validate/design-baseline` 유지
- `/validate/design` 은 Wizard payload면 Wizard Gate, 아니면 baseline 호환

## Remaining (out of scope per design)

- 실제 DDL / Ontology Table 자동 생성
- ADR 영속화 / Graph Editor
- LLM 근거 없는 자유 설계
