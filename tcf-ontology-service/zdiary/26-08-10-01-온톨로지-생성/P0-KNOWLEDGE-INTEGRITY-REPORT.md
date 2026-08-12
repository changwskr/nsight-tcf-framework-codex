# P0 Knowledge Integrity Report

- 일자: 2026-08-10
- 기준: `26-08-10-14` Phase 1

## 수정 요약

| P0 | 조치 |
|----|------|
| P0-03 Composite PK | `YamlGraphLoader.normalizeStringList` — list PK → 개별 Column |
| P0-04 Multi Table | `data.table`/`data.tables` list 지원; 가짜 `[TB_A,TB_B]` 금지; multi-table 시 SqlId→Table 임의 연결 안 함 |
| P0-05/06 Operation | Design UI QUERY/CREATE/UPDATE/DELETE/MIXED/REPORT; `DesignRecommendationService` operationMatch; DELETE→D0 |
| P0-07 Pattern Evidence | `POST /api/ontology/design/recommend` + field-level status (paging=UNRESOLVED) |
| P0-08 Gate | GLOBAL `/validate/rules`, SERVICE `/validate/service/{id}`, DESIGN `/validate/design(-baseline)` |

## 테스트

- `CompositePkAndMultiTableLoaderTest` PASS
- `DesignRecommendationServiceTest` PASS (DELETE→mgcoa8888D0, paging UNRESOLVED)
- Full suite: **60 tests, 0 failures** (27 suites)

## 남은 Gap

- RULE-003는 Ontology Relation 기준 (Handler `serviceIds()` Source 직접 비교 아님) — 이름/설명에 과장 금지 유지
- Multi-table 실 YAML 샘플 추가 적재는 후속
