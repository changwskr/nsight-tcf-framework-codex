# Architecture Design Wizard — Gap Analysis

- 작성일: 2026-08-10
- 기준: `26-08-10-16-Architecture_Design_Wizard_ServiceId_DataTable_설계서.md`

## 현행

| 영역 | 상태 |
|------|------|
| Requirement + Classification | 부분 (한 화면에 혼재) |
| Candidate Recommend | 있음 |
| ServiceId **신규 설계/중복검증** | 없음 |
| Program Inventory | catalog만 |
| Data/Table 설계 UI | 참조 입력만 |
| Column/PK/Join 설계 | 없음 |
| Application Component 명명 | 없음 |
| Runtime/Policy 단계 | Baseline 일부 |
| 7-Step Wizard Shell | 없음 (5-step recommend flow) |

## 재사용

- `classification.yml` / bundle API
- `ServiceIdParser`
- `OntologyRegistry.listPrograms`
- `OntologyStore` TABLE/COLUMN + HAS_COLUMN
- `DesignRecommendationService` (후보 참조용)
- `ArchitectureRuleValidator` DESIGN_BASELINE
- `design.js` / `api.js` / workbench CSS

## 구현 계획

1. Backend `DesignWizardService` + `/api/ontology/design/*`
2. Frontend 7-step Wizard (`design.js` 재구성)
3. Golden Scenario 테스트
4. Implementation Report
