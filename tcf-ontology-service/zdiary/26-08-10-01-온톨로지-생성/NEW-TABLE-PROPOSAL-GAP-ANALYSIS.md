# NEW-TABLE-PROPOSAL-GAP-ANALYSIS

- 기준 설계서: `26-08-10-18-DataTable_NewTableProposal_설계서.md`
- 작성일: 2026-08-10
- 대상: `tcf-ontology-service` Architecture Design Wizard STEP 4

## 1. 분석 시점 Gap (구현 전)

| 항목 | 설계 | 구현 전 상태 | Gap |
|---|---|---|---|
| Table Search No Result UI | [다시 검색][UNRESOLVED 유지][+ 신규 Table 설계] | 단순 "검색 결과 없음" 행 | 있음 |
| 신규 Table 설계 | Sub-step 4-2~4-7 | `prompt()`로 이름만 기록 | 있음 |
| Column Grid | 행 추가 편집 | 없음 | 있음 |
| Composite PK | column flag → list | 없음 | 있음 |
| Index / Relation | PROPOSED 입력 | 없음 | 있음 |
| Security/Capacity | UNRESOLVED 허용 | 없음 | 있음 |
| Validation API | DATA-TBL/COL/PK/SEC | 없음 → 이후 추가됨 | 있음 |
| Proposal API | create/get/update, status=PROPOSED | 없음 → 이후 추가됨 | 있음 |
| Design Gate | proposal ≠ VERIFIED, column≥1 | legacy string만 | 있음 |
| Cursor Export | NEW_TABLE_PROPOSAL 섹션 | legacy 이름만 | 있음 |

## 2. 핵심 원칙 준수 여부

- 신규 Table을 Ontology VERIFIED로 등록하지 않음 → `TableProposalService` in-memory PROPOSED만 저장
- Column 0건 FAIL → `DATA-COL-001`
- PK를 단일 문자열 컬럼으로 합치지 않음 → `primaryKey: string[]`
- 개인정보 미확인 → `UNRESOLVED` (NO 강제 금지)
- DDL 실행 없음

## 3. 구현 후 남은 Gap

1. Proposal 저장소가 프로세스 메모리(`ConcurrentHashMap`) — 재시작 시 소실 (Session `dataDesign.tableProposals`로 보완)
2. DA/DBA APPROVED → IMPLEMENTED → Metadata Scan → VERIFIED 워크플로는 후속
3. Oracle DDL 생성/실행은 의도적으로 제외
4. Update Column 역할(UPDATE target) UI는 Filter/Select 중심 — 확장 여지
5. `description` 필수 검증은 UI 가드 위주(백엔드 rule ID 미부여)

## 4. 결론

설계서 Phase 1~4의 Workbench 범위(검색 없음 → Proposal → Validation → Baseline/Export)는 구현 대상으로 닫을 수 있다.
승인/DDL/Ontology VERIFIED 승격은 후속 단계로 남긴다.
