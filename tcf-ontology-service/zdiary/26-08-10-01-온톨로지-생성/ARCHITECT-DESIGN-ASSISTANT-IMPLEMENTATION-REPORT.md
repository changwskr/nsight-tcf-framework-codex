# Architecture Design Assistant — Implementation Report

- 작성일: 2026-08-10
- 기준: `26-08-10-11-Architecture_Design_Assistant_2차_설계서.md` §44
- 선행 분석: `ARCHITECT-DESIGN-ASSISTANT-ANALYSIS.md`
- 판정: **2차 Design Assistant 구현 완료 (실 API, Mock 없음)**

---

## 1. 변경 파일

| 파일 | 내용 |
|------|------|
| `ARCHITECT-DESIGN-ASSISTANT-ANALYSIS.md` | Phase 0 분석 |
| `ARCHITECT-DESIGN-ASSISTANT-IMPLEMENTATION-REPORT.md` | 본 보고서 |
| `static/workbench/js/design.js` | Design Assistant (Requirement→Export) |
| `static/workbench/js/api.js` | `recommend` / `promptJson` / `promptMarkdown` |
| `static/workbench/js/app.js` | `#/design` route, Home 퀵액션 |
| `static/workbench/index.html` | 메뉴 05, design.js 로드 |
| `static/workbench/css/workbench.css` | Design form/candidate 스타일 |
| `WorkbenchStaticUiTest.java` | design.js 서빙 검증 |
| `test-data/queries/design-scenario*.json|md` | Golden 실측 증거 |

---

## 2. 신규 API

**없음.**  
분석 결론대로 `/design/recommend` 미추가. 기존 API 조합:

- `GET /api/ontology/recommend`
- `GET /api/ontology/query/service/{id}/structure`
- `GET /api/ontology/validate/rules`
- `GET /api/ontology/prompt/{id}.md`

---

## 3. Route / UI

| Route | 기능 |
|-------|------|
| `#/design` | Architecture Design Assistant |
| `#/design?business=CO&type=QUERY` | Golden 입력 프리셋 |

메뉴: `05 · Architecture Design`  
Component: RequirementForm, CandidateList/Detail, Compare, Pattern, Baseline, Gate panel, Context Export (Evidence Drawer 재사용)

---

## 4. Golden 실측 (port 8102)

### Scenario 1 — Candidate Search
`intent=query`, business=CO, function=A → **status=OK, 5건**

| programId | score | reasons |
|-----------|-------|---------|
| mgcoa5530 | 95 | same MG/CO/A + query-oriented |
| mgcoa8888 | 95 | same |
| mgcoa9999 | 95 | same |
| mgcoa9000 | 75 | has query service |
| mgcoa9001 | 75 | has query service |

증거: `test-data/queries/design-scenario1-recommend.json`

### Scenario 2 — Structure
top `mgcoa5530S0`:

```text
mgcoa5530S0 → Handler → Facade → Service → DAO → Mapper → TB_MK_CO_A_5530
```

### Scenario 3 — Baseline / Pattern
- Pattern: `ONLINE_PAGING_QUERY`
- Status: **`DERIVED_PATTERN`** (ArchitecturePattern registry 없음)
- 신규 Program/ServiceId/Table: **UNRESOLVED**
- 임의 Spring/JPA 추천 없음

### Scenario 4 — Context Export
- Markdown/JSON 샘플: `design-scenario4-context.md` / `.json`
- 섹션: Requirement, Pattern, Reference ServiceId, Structure, Message, Transaction, Paging, Rules, Evidence, Unresolved, Prohibited, Validation

### Gate
- `status=PASS`, `failCount=0`

---

## 5. Error / Timeout

- recommend/structure/prompt 실패 → Error Banner / PARTIAL
- api.js 12초 AbortController (기존 정책)
- 후보 0건 → 임의 ServiceId 제시 **금지** 메시지 + Search 이동

---

## 6. 테스트 / 회귀

- `WorkbenchStaticUiTest` PASS (design.js 포함)
- `HealthControllerTest` PASS
- 1차 Route (`#/home|search|impact|gate`) 유지

---

## 7. 남은 Gap

1. ArchitecturePattern **영속 모델/YAML** 미도입 (의도적 DERIVED_PATTERN)
2. paging/channel 등 Requirement 필드는 UI 보관, Recommend 점수에는 미반영
3. Design session 브라우저 메모리만 (ADR 영속화는 범위 외)
4. `/design/result/{sessionId}` 서버 세션 미구현

---

## 8. 핵심 질문에 대한 답 (실데이터)

> 신규 조회 거래를 만들려고 한다. 무엇을 참고하고 어떤 구조로, 근거는?

- **참고 후보:** Ontology recommend 결과 (예: `mgcoa5530`, `mgcoa8888`, …) — score/reasons 공개
- **구조:** 후보 ServiceId structure의 Handler→Facade→Service→DAO→Mapper→Table
- **Pattern:** `ONLINE_PAGING_QUERY` (**DERIVED_PATTERN**)
- **근거:** YAML mapping + YamlGraphLoader Provenance + RULE Gate snapshot
- **신규 ID/Table:** UNRESOLVED (추정 생성 안 함)

---

## 9. 사용법

```bat
cd tcf-ontology-service
gradlew.bat bootRun
```

브라우저: `http://localhost:8098/workbench/index.html#/design?business=CO&type=QUERY`  
(검증 시에는 `8102`에서 기동했음)
