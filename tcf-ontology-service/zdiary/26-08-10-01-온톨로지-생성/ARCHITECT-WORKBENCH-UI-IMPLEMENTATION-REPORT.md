# Architect Workbench UI Implementation Report

- 작성일: 2026-08-10
- 기준: `26-08-10-10-Architect_Workbench_설계서.md` §36
- 선행: `ARCHITECT-WORKBENCH-UI-ANALYSIS.md`
- 판정: **1차 범위 구현 완료 (실 API 연결)**

---

## 1. 변경 파일

| 경로 | 역할 |
|------|------|
| `ARCHITECT-WORKBENCH-UI-ANALYSIS.md` | Phase 0 기술스택 분석 |
| `ARCHITECT-WORKBENCH-UI-IMPLEMENTATION-REPORT.md` | 본 보고서 |
| `src/main/resources/static/workbench/index.html` | SPA shell |
| `src/main/resources/static/workbench/css/workbench.css` | Layout / theme |
| `src/main/resources/static/workbench/js/api.js` | Ontology API client + timeout |
| `src/main/resources/static/workbench/js/app.js` | Hash router + 4 views + Evidence drawer |
| `src/main/java/.../HealthController.java` | health에 `workbench` 링크 추가 |
| `src/test/java/.../WorkbenchStaticUiTest.java` | static 서빙 스모크 테스트 |
| `test-data/queries/workbench-scenario-*.json` | Golden 실측 증거 |

---

## 2. Route

| URL | 화면 |
|-----|------|
| `http://localhost:8098/workbench/index.html#/home` | Architect Home |
| `...#/search?q=mgcoa8888S0&type=SERVICE` | Architecture Search |
| `...#/impact?table=TB_FW_IMAGE_LOG` | Impact Analysis |
| `...#/gate` | Architecture Gate |

Entry: `/workbench/index.html` (root `/` 는 기존 health JSON 유지)

---

## 3. Component 구성

- **Shell**: Side menu + Header + Global Search
- **Views**: Home / Search / Impact / Gate (hash SPA, vanilla JS)
- **Evidence Drawer**: Provenance 표시 (`discoveredBy`, `sourcePath`, …)
- **API Client**: `fetch` + AbortController timeout(12s), Mock 없음

신규 React/Vue/Thymeleaf/Graph Editor/AI Chat/ADR/CRUD **미도입** (설계 준수).

---

## 4. API 연결

| 기능 | Endpoint | Method |
|------|----------|--------|
| Catalog | `/api/ontology/catalog` | GET |
| Consistency | `/api/ontology/consistency` | GET |
| Structure | `/api/ontology/query/service/{id}/structure` | GET |
| Tables | `/api/ontology/query/service/{id}/tables` | GET |
| Program/Handler | `/query/program|handler/...` | GET |
| Impact | `/api/ontology/impact/table/{table}` | GET |
| Gate | `/api/ontology/validate/rules` | **GET** (설계서 POST와 달리 현행 API 준수) |
| Runtime | `/api/ontology/runtime/tx-chain` | GET |
| Concept | `/api/ontology/v1/concept/{id}` | GET |

---

## 5. 실제 화면·API 실행 증거 (8098)

### Static UI
- `GET /workbench/index.html` → **200**, body contains `NSIGHT Architect Workbench`
- `GET /health` → `workbench: /workbench/index.html`

### Scenario A — mgcoa8888S0
```
mgcoa8888S0 → mgcoa8888Handler → mgcoa8888Facade → mgcoa8888Service → mgcoa8888DAO → … → TB_FW_IMAGE_LOG
```
증거: `test-data/queries/workbench-scenario-a-structure.json`  
Evidence: `discoveredBy=YamlGraphLoader`, `sourcePath=ontology/mappings/mgcoa8888.yml`

### Scenario B — TB_FW_IMAGE_LOG
- `table.type = TABLE`
- `affectedSystems = MG`, `affectedFunctions = A`
- Mapper~System 계층 비어 있지 않음
- 증거: `test-data/queries/workbench-scenario-b-impact.json`
- UI: `table.type !== TABLE` 이면 오류 배너

### Scenario C — Architecture Gate
- `status=PASS`, `failCount=0`, findings=44 (RULE-001~006 × 대상)
- 증거: `test-data/queries/workbench-scenario-c-gate.json`

---

## 6. Error / Timeout

- API 4xx/5xx → 화면 `wb-error` 배너
- AbortController **12초 timeout** → `요청 시간 초과` 메시지
- Impact alias 오류 → TABLE이 아니면 정상 결과로 표시하지 않음

---

## 7. 테스트 결과

```
WorkbenchStaticUiTest PASS
HealthControllerTest PASS
TableImpactQueryTest / ServiceStructureQueryTest PASS (회귀)
```

---

## 8. 1차 완료조건 체크

- [x] Architect Home
- [x] Architecture Search
- [x] Impact Analysis
- [x] Architecture Gate
- [x] Evidence Panel
- [x] 기존 Ontology API 재사용
- [x] ServiceId 구조 조회
- [x] Table 역추적
- [x] Rule 검증
- [x] 오류/Timeout 처리
- [x] Golden Scenario 실측

---

## 9. 사용 방법

```bat
cd tcf-ontology-service
gradlew.bat bootRun
```

브라우저: [http://localhost:8098/workbench/index.html](http://localhost:8098/workbench/index.html)
