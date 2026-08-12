# Architecture Design Assistant — Analysis

- 작성일: 2026-08-10
- 기준: `26-08-10-11-Architecture_Design_Assistant_2차_설계서.md` §41
- 대상: `tcf-ontology-service` + Architect Workbench

---

## 1. 결론 요약

| 질문 | 답 |
|------|----|
| ArchitecturePattern 1급 모델 존재? | **없음** → 후보는 Ontology에서, Pattern은 **DERIVED_PATTERN**으로 UI/세션에서 추출 |
| `/design/recommend` 신규 API 필요? | **1차적으로 불필요**. 기존 API 조합으로 충분 |
| Mock 허용? | **불가**. `/recommend` + structure + validate + prompt 실호출 |
| UI 스택 | 기존 **static HTML + vanilla JS + Hash SPA** 유지 |

---

## 2. RecommendService / API

### Endpoint
- `GET /api/ontology/recommend?system&business&function&intent&like`
- `POST /api/ontology/recommend` (body Map)

### Request (현재)
`system`(default MG), `business`(CO), `function`(A), `intent`(crud|query|delete), `like`(optional)

### Response
```text
status: OK | NO_MATCH
request: {…}
recommendations[]: { programId, title, score, reasons[], reuse{development,data,services,path} }
topProgramId
promptMarkdown
nextSteps
```

### 점수
- system +20 / business +30 / function +20
- intent: CRUD full +25, query-oriented +25, delete +15, has S +5
- like 명시 +50
- top 5

### Gap vs 설계서 입력
- channel / dbAccess / externalCall / largeData / paging / timeout / personalData 는 **RecommendService에 없음**
- Mapping YAML에도 paging 전용 metadata가 **일관되게 없음** (operations/samples 힌트만 일부)
- → UI는 해당 필드를 Requirement로 보존하고, Candidate의 확인 불가 항목은 **`?` / UNRESOLVED** 표시 (임의 NO/YES 추정 금지)

`intent=QUERY`는 `toLowerCase()`로 `query` 매칭되어 **추가 백엔드 변경 없이 동작**.

---

## 3. PromptContextExporter

- `GET /api/ontology/prompt/{id}` JSON
- `GET /api/ontology/prompt/{id}.md` Markdown
- 대상: programId 또는 serviceId → mapping YAML 기반 CRUD 프롬프트 컨텍스트
- Design Assistant의 Cursor Context는 이 exporter 결과를 **참고 블록으로 포함**하고, Requirement/Pattern/Baseline/Unresolved/Prohibited를 **UI에서 조립**하는 것이 적합 (exporter 모델 변경 최소화)

---

## 4. Workbench 현황

| 항목 | 상태 |
|------|------|
| `static/workbench/index.html` | Shell + 4메뉴 |
| `api.js` | catalog/structure/impact/validate/runtime |
| `app.js` | home/search/impact/gate + Evidence Drawer |
| `#/design` | **없음** |
| ArchitecturePattern | **없음** |

재사용: Shell, Evidence Drawer, Error Banner, api timeout(12s), Gate findings(`verdict`)

---

## 5. Architecture Gate

- `GET /api/ontology/validate/rules`
- findings: `{ ruleId, verdict, target, message, evidence, source? }`
- Design-Time Gate: 전역 규칙 결과를 표시 + “신규 ServiceId는 UNRESOLVED이므로 구현 후 재검증” 안내

---

## 6. Mapping YAML Metadata

예: `mgcoa8888.yml` — classification, development, services(op), data.table, operations.envelope  
**없는 것:** ArchitecturePattern id, paging flag, channel enum 표준 필드

→ Pattern은 후보 구조에서 추출한 **DERIVED_PATTERN**만 2차에 허용.

---

## 7. 구현 방침 (분석 후)

1. **신규 Backend API 추가하지 않음** (`/design/recommend` 보류)
2. Route `#/design` + Side Menu `05. Architecture Design`
3. UI에서:
   - Requirement Form → `GET /recommend`
   - Candidate 선택 → `structure` + `tables` + Evidence
   - Compare (최대 2~3)
   - Pattern = DERIVED_PATTERN (후보 공통 계층)
   - Baseline Preview (미확정 = UNRESOLVED)
   - Gate 패널
   - Markdown / JSON Export (브라우저 다운로드 + 화면 preview)
4. 파일: `js/design.js` 분리, `api.js`에 recommend/prompt 추가, menu/CSS 소폭 확장

---

## 8. Phase 0 완료

- [x] RecommendService 분석
- [x] recommend Request/Response
- [x] PromptContextExporter
- [x] Workbench app/api/Evidence/Gate
- [x] YAML metadata / Pattern 모델 부재 확인
- [x] 신규 API 불필요 판단

→ Phase 1~3 UI 구현 진행.
