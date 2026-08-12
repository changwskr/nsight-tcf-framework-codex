# Current Project Gap Check

- 작성일: 2026-08-10
- 기준: `26-08-10-14-TCF_Ontology_전체프로젝트_점검_및_Cursor_통합보완지시서.md`
- 원칙: 본 문서는 **수정 전** 소스 대조 결과이다.

판정 값: `CONFIRMED` | `ALREADY_FIXED` | `NOT_FOUND` | `DIFFERENT_IMPLEMENTATION`

---

## P0

| ID | 항목 | 판정 | 근거 |
|----|------|------|------|
| P0-01 | WAR ≠ 최신 소스 / workbench 미포함 | **CONFIRMED** | 이전 WAR는 workbench 이전 생성 가능. clean war 재검증 필요 |
| P0-02 | api.js Context Path (`/api` absolute) | **CONFIRMED** | `fetch("/api/...")` 절대경로. `resolveContextPath` 없음 |
| P0-03 | Composite PK → 가짜 Column 1개 | **CONFIRMED** | `YamlGraphLoader` `str(data.get("pk"))`; mgcoa5530 `pk: [L5101,L5103]` |
| P0-04 | Multi-table → 가짜 Table 문자열 | **CONFIRMED** | `str(data.get("table"))`만 지원. list 미정규화 |
| P0-05 | UPDATE→query 매핑 | **CONFIRMED** | `intentFromTx` default → query; UI에 CREATE/UPDATE/MIXED/REPORT 미비 |
| P0-06 | primaryServiceId 무조건 S 우선 | **CONFIRMED** | design.js `find(op===S)||services[0]`; RecommendService도 S 우선 |
| P0-07 | derivePattern UI 하드코딩 | **CONFIRMED** | `design.js derivePattern()`; `POST /design/recommend` **없음** |
| P0-08 | Gate Scope/Semantic | **DIFFERENT_IMPLEMENTATION** | SERVICE/DESIGN API는 추가됨. 다만 design UI가 혼용·RULE-003 source 미검사·`/validate/design` 명칭은 baseline |
| P0-09 | Full Regression + WAR | **CONFIRMED** | 로컬에서 전체 test는 돌렸으나 Release 문서/WAR acceptance 미완성 |

## P1

| ID | 항목 | 판정 | 근거 |
|----|------|------|------|
| P1-01 | Provenance VERIFIED 과다 | **ALREADY_FIXED** | yaml/sourceCode → DISCOVERED; scannerVerified 추가 |
| P1-02 | classificationPath 가상 Relation | **ALREADY_FIXED** | `relationStatus` VERIFIED/INFERRED |
| P1-03 | summarizeStructure findFirst | **DIFFERENT_IMPLEMENTATION** | sort+findFirst만; paths/nodes/edges 정본 API 없음 |
| P1-04 | Impact GraphPath 모델 | **CONFIRMED** | Flat reverse/synthesize만 |
| P1-05 | Alias ambiguity | **DIFFERENT_IMPLEMENTATION** | TABLE type filter 있음; generic alias 단일 Map |
| P1-06 | Registry/Store reload | **CONFIRMED** | `/reload` Registry만 |
| P1-07 | Seed SQL table DRAFT | **CONFIRMED** | Regex seed; DRAFT 표기 정책 미명시 |
| P1-08 | OntologyValidator null skip | **CONFIRMED** | (코드 대조 후 유지) |
| P1-09 | Program AUTO regex | **ALREADY_FIXED** | 9자 Program / 11자 ServiceId |
| P1-10 | Business/Function 하드코딩 | **CONFIRMED** | design.js select 고정 |
| P1-11 | Google Fonts | **CONFIRMED** | index.html fonts.googleapis.com |
| P1-12 | 보안/권한 | **CONFIRMED** | 관리 API 무보호 |

## P2

대부분 **CONFIRMED** (미구현): GraphPath 영속, Pattern registry, ADR, RFC7807, H2 제거, Neo4j/Vector 보류.

---

## 종합

- Knowledge Integrity P0 (PK/Table/Op/Pattern Evidence): **미해결 다수**
- Gate/Provenance/AUTO: **부분 해결**
- Release (Context Path + WAR): **미해결**

→ Phase 1(P0 Knowledge) → Phase 2(Deploy) 순으로 진행.
