# NEW-TABLE-PROPOSAL-IMPLEMENTATION-REPORT

- 기준 설계서: `26-08-10-18-DataTable_NewTableProposal_설계서.md`
- 작성일: 2026-08-10
- 범위: STEP 4 · Data / Table Design — New Table Proposal

## 1. 요약

Ontology Table 검색 결과가 없을 때 `[+ 신규 Table 설계]`로 Sub-wizard(4-2~4-7)를 열고,
Validation 통과 시 `NEW_TABLE_PROPOSAL`을 **PROPOSED**로 Design Baseline(`dataDesign.tableProposals`)에 반영한다.
Ontology Concept를 VERIFIED로 생성하지 않으며 DDL을 실행하지 않는다.

## 2. 변경 파일

### Backend
- `src/main/java/nhnis/ontology/design/TableProposalService.java` (신규)
- `src/main/java/nhnis/ontology/web/DesignWizardController.java` — validate/create/get/update
- `src/main/java/nhnis/ontology/design/DesignWizardService.java` — Gate/Export `tableProposals` 연동

### Frontend
- `src/main/resources/static/workbench/js/table-proposal.js` (신규)
- `src/main/resources/static/workbench/js/design.js` — STEP 4 No Result / Proposal 목록
- `src/main/resources/static/workbench/js/api.js` — tableProposal* API
- `src/main/resources/static/workbench/index.html` — script 로드
- `src/main/resources/static/workbench/css/workbench.css` — column editor

### Test / Docs
- `src/test/java/nhnis/ontology/design/TableProposalServiceTest.java`
- `src/test/java/nhnis/ontology/web/DesignWizardApiTest.java`
- `src/test/java/nhnis/ontology/web/WorkbenchStaticUiTest.java`
- `NEW-TABLE-PROPOSAL-GAP-ANALYSIS.md`
- `NEW-TABLE-PROPOSAL-IMPLEMENTATION-REPORT.md` (본 문서)

## 3. 화면

| Sub | 내용 |
|---|---|
| 4-1 | Table Search + No Result 액션 3종 |
| 4-2 | Basic (논리/물리/Schema/축/유형/설명) |
| 4-3 | Column Grid (Type/Length/PK/Null/개인정보/Enc/Mask) |
| 4-4 | Key/Index (Composite PK = column flags) |
| 4-5 | Relation / LOGICAL_JOIN |
| 4-6 | Access / Security / Capacity / Filter·Select |
| 4-7 | Review + Validation + PROPOSED 반영 |

## 4. API

```http
POST /api/ontology/design/table-proposal/validate
POST /api/ontology/design/table-proposal
GET  /api/ontology/design/table-proposal/{proposalId}
PUT  /api/ontology/design/table-proposal/{proposalId}
```

Create 성공 시:
- `proposalStatus=PROPOSED`
- `mode=NEW_TABLE_PROPOSAL`
- `verificationStatus=PROPOSED` (VERIFIED 금지)

## 5. Validation

- Table: DATA-TBL-001~006, TABLE-NAME-006
- Column: DATA-COL-001 + COL-001~005, COL-007
- PK: DATA-PK-001~004 (list, Composite 지원)
- Security: DATA-SEC-001 (UNRESOLVED 허용)

FAIL이면 Proposal 미반영(`accepted=false`).

## 6. Design Gate / Export

- Gate: `DATA-TBL-PROPOSAL` — PROPOSED는 PASS_WITH_UNRESOLVED, VERIFIED면 FAIL
- Column 0건: `DATA-COL-001` FAIL
- Export markdown: `### NEW_TABLE_PROPOSAL` + physicalName/columns 요약

## 7. Session 상태

```js
dataDesign: {
  selectedTables: [],
  joins: [],
  tableUnresolved: false,
  tableProposals: [ /* PROPOSED objects */ ],
  draftProposal: null
}
```

## 8. Test

- `TableProposalServiceTest` — no-column FAIL, create PROPOSED, composite PK, gate/export
- `DesignWizardApiTest.table_proposal_validate_create_get`
- `WorkbenchStaticUiTest` — `table-proposal.js` 서빙 및 STEP4 문구

## 9. Golden Scenario

요구: "고객별 AI 추천 결과 저장"
→ Ontology 검색 없음 → 신규 Table 설계
→ `TB_MK_CO_A_AI_RECOMMEND` + columns/PK
→ Validation PASS_WITH_UNRESOLVED (개인정보 UNRESOLVED)
→ status=PROPOSED → Baseline/Gate/Export

## 10. 남은 Gap

- Proposal 메모리 저장소 / APPROVED·DDL·VERIFIED 승격 미구현
- Update-column 역할 UI 확장
- 설명(description) 백엔드 필수 rule 미부여

## 11. 사용 방법

1. Workbench `#/design` → STEP 4
2. Keyword로 검색 → 결과 없으면 `[+ 신규 Table 설계]`
3. 4-2~4-7 입력 → Validation → `PROPOSED 반영`
4. STEP 7 Gate/Export에서 Proposal 확인
5. 정적 캐시 시 Ctrl+F5
