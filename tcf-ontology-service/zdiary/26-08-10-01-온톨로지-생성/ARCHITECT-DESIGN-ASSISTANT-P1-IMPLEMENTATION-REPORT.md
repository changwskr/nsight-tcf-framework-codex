# Architecture Design Assistant P1 Implementation Report

- 작성일: 2026-08-10
- 기준: `26-08-10-11` 설계서 + `26-08-10-13` 재개 지시
- 선행: P0 `ONTOLOGY-SOURCE-CODE-REVIEW-FIX-REPORT.md`
- 판정: **P1 보완 + Design Assistant Golden 실측 PASS**

---

## 1. P1 수정 항목

| ID | 내용 | 결과 |
|----|------|------|
| Gate 스코프 | 전체 / ServiceId / Design-Time Baseline 분리 | 완료 |
| Program AUTO | `mgcoa8888`(9자) vs `mgcoa8888S0`(11자) | 완료 |
| Recommend | Graph·Rule·Evidence·confidence(HIGH/MED/LOW) | 완료 |
| Evidence | YAML/추정 경로 → **DISCOVERED** (추측 VERIFIED 금지) | 완료 |
| Classification | Graph 존재 시 **VERIFIED**, 파싱만이면 **INFERRED** | 완료 |
| Design UI | `#/design` 전 흐름 + scoped Gate + Export | 완료 |

---

## 2. 변경 파일

- `ArchitectureRuleValidator.java` — `validateService`, `validateDesignBaseline`, verdict 확장
- `OntologyCoreQueryController.java` — `GET /validate/service/{id}`, `POST /validate/design-baseline`
- `RecommendService.java` — Graph/Rule 보강, confidence, evidence
- `OntologyController.java` — recommend query params (`dbAccess`,`paging`,`channel`)
- `Provenance.java` — yaml/sourceCode → DISCOVERED, `scannerVerified` 추가
- `OntologyQueryService.java` — classification `relationStatus`, summarize 정렬
- `ConceptIds.programFromShortId` — 9자 Program 정합
- `ProgramIdParser.java` + Test
- `ArchitectureRuleScopedValidationTest.java`
- Workbench `app.js` / `api.js` / `design.js`

---

## 3. 신규 API

| Method | Path | 용도 |
|--------|------|------|
| GET | `/api/ontology/validate/rules` | 전체 (기존) |
| GET | `/api/ontology/validate/service/{serviceId}` | ServiceId 단위 |
| POST | `/api/ontology/validate/design-baseline` | Design-Time (UNRESOLVED/NOT_YET_IMPLEMENTED) |

Design verdict: `PASS` · `FAIL` · `NOT_YET_IMPLEMENTED` · `UNRESOLVED` · `NOT_APPLICABLE`  
`failCount`는 **FAIL만** 집계.

---

## 4. Golden 실측 (port 8103)

입력: Business=CO, Function=A, Transaction=QUERY, DB=YES, Paging=YES

### Recommend
- status=OK, 5 candidates
- `mgcoa5530` / `mgcoa8888` / `mgcoa9999` → **confidence=HIGH**
- evidence.verificationStatus=**DISCOVERED**
- 증거: `test-data/queries/p1-design-recommend.json`

### Structure (mgcoa5530S0)
```text
mgcoa5530S0 → Handler → Facade → Service → DAO → Mapper → TB_MK_CO_A_5530
```
classification relationStatus: **VERIFIED,VERIFIED,VERIFIED,VERIFIED**

### Gate
- Service scope: **PASS** fail=0
- Design baseline UNRESOLVED: **PASS** fail=0, unresolved=2, notYet=4
- ALL validate: PASS

### UI
- `http://localhost:8103/workbench/index.html#/design?business=CO&type=QUERY`
- design.js served

### Pattern / Baseline / UNRESOLVED
- Pattern: `ONLINE_PAGING_QUERY` / **DERIVED_PATTERN**
- 신규 ProgramId/ServiceId/Table → **UNRESOLVED** (임의 생성 없음)
- Markdown/JSON Export 유지

---

## 5. 테스트

```text
gradlew.bat test  → BUILD SUCCESSFUL
```

포함: ProgramIdParserTest, ArchitectureRuleScopedValidationTest, Workbench AUTO regex, 기존 회귀.

---

## 6. 남은 Gap

- Paging/Transaction **전용 YAML 메타** 부재 → unmatchedAttributes로만 표시
- ArchitecturePattern 영속 레지스트리 없음 (DERIVED_PATTERN 유지)
- Google Fonts 외부 의존 (P1-08 미처리)
- `/reload` ↔ OntologyStore 동기화 (P1-06 미처리)
- Design session 서버 영속화 없음

---

## 7. 최종 질문에 대한 답 (실데이터)

신규 조회 거래 시:

1. **참고 후보:** Ontology recommend HIGH (`mgcoa5530S0` 등) + DISCOVERED evidence  
2. **구조:** Handler→Facade→Service→DAO→Mapper→Table (실 structure API)  
3. **근거:** Graph VERIFIED classification + Design-Time Gate(UNRESOLVED/NOT_YET) + Reference Service Gate PASS  

**Architecture Design Assistant: COMPLETE (Golden 실측 기준)**
