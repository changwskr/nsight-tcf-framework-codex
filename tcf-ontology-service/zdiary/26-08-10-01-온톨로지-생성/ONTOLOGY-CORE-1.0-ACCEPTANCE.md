# Ontology Core 1.0 Acceptance

**판정: A. Ontology Core 1.0 ACCEPTED**

일자: 2026-08-10  
근거 문서: `zdiary/26-08-10-01-온톨로지-생성/26-08-10-08-점검보완.md`  
증거: `test-data/queries/acceptance-08-impact-table.json`

---

## 1. 결함 수정 요약

| # | 결함 | 조치 |
|---|------|------|
| 1 | `/impact/table/TB_FW_IMAGE_LOG` → `table.type=COLUMN` | Column `tableName` alias 등록 제거. `findConceptOfType(..., TABLE)`로 impact resolve |
| 2 | Function/System 역추적 누락 | `affectedFunctions` / `affectedSystems` 추가, classification 조상 수집, paths에 System↔Table E2E 포함 |
| 3 | 재검증 | `clean test` / `war` / `bootRun:8099` / impact API 실측 |

---

## 2. 빌드·기동

| 단계 | 결과 |
|------|------|
| `gradlew.bat clean test` | PASS |
| `gradlew.bat war` | PASS |
| `gradlew.bat bootRun --args="--server.port=8099"` | PASS (`Started OntologyApplication`) |
| `GET /actuator/health` | `{"status":"UP"}` |

---

## 3. Impact 실측 (`TB_FW_IMAGE_LOG`)

| 항목 | 결과 |
|------|------|
| `table.type` | `TABLE` |
| `table.name` | `TB_FW_IMAGE_LOG` |
| `affectedMappers` | 1 (비어있지 않음) |
| `affectedDaos` | 1 |
| `affectedServices` | 1 |
| `affectedFacades` | 1 |
| `affectedHandlers` | 1 |
| `affectedServiceIds` | 2 (`mgcoa8888S0`, `mgcoa8888D0`) |
| `affectedPrograms` | 1 (`mgcoa8888`) |
| `affectedFunctions` | 1 (`A`) |
| `affectedBusinesses` | 1 (`CO`) |
| `affectedSystems` | 1 (`MG`) |

paths predicates (실측):

`ACCESSES`, `EXECUTES`, `USES`, `CALLS`, `HANDLED_BY`, `PROVIDES_SERVICE`, `HAS_PROGRAM`, `HAS_FUNCTION`, `HAS_BUSINESS`

→ System → Business → Function → Program → ServiceId → … → Table 역추적 PASS.

---

## 4. Provenance / 회귀

| 항목 | 결과 |
|------|------|
| Provenance `discoveredBy` | `YamlGraphLoader` |
| `sourcePath` | `ontology/mappings/mgcoa8888.yml` |
| service structure 회귀 | `GET .../query/service/mgcoa8888S0/structure` tables=1 PASS |

---

## 5. A 조건 체크리스트

- [x] clean test PASS
- [x] war PASS
- [x] boot PASS
- [x] impact.table TABLE 정상
- [x] Table→System 전체 역추적 PASS
- [x] Provenance PASS
- [x] 기존 API 회귀 PASS

---

## 6. Git Baseline (commit 미수행)

현재 상태 (commit 전 후보):

- 수정: `OntologyStore.java`, `OntologyQueryService.java`, `ReverseImpactUnitTest.java`, `TableImpactQueryTest.java`
- 추가 후보: `test-data/queries/acceptance-08-impact-table.json`, `ONTOLOGY-CORE-1.0-ACCEPTANCE.md`
- tracked (`git ls-files tcf-ontology-service`): 125
- 제외: `build/`, `.gradle/`, `ontology/mappings/_generated/` (이미 `.gitignore`)
- `.gitignore` 추가 보완 불필요 (Eclipse 메타는 이미 제외)

후보 메시지:

```
fix(ontology): close Ontology Core 1.0 with TABLE impact and System reverse path
```
