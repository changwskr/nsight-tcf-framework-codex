# Ontology Source Code Review Fix Report

- 작성일: 2026-08-10
- 기준: `zdiary/.../26-08-10-12-정합성점검.md`
- 범위: **P0 수정 완료** / P1 분석·목록화 / Design Assistant **완료 판정 보류**

---

## 1. 수정 전 문제

### P0-01 ArchitectureRuleValidator
`validateAll()`이 findings의 **`severity`** 로 failCount를 집계했으나, finding 생성은 **`verdict`** 만 넣는다.  
결과적으로 FAIL finding이 있어도 `failCount`가 항상 0, `status`가 항상 PASS로 나올 수 있었다.

### P0-02 Architecture Design Route
점검 시점 기준으로는 `design.js` 부재 / Router 미연결이 지적됨.  
현재 workspace에는 이미 `design.js`·`META.design`·`DesignAssistant.render`가 존재했으나, **정적 자산·라우트 정합을 테스트로 고정**할 필요가 있었다.

### P0-03 Impact Path 의미 오류
`synthesizeImpactPaths()` fallback이 실제 Graph에 없는  
`ServiceId -HANDLED_BY→ Table` edge를 path 증명용으로 삽입할 수 있었다.  
HANDLED_BY 대상은 Handler여야 한다.

---

## 2. 변경 파일

| 파일 | 변경 |
|------|------|
| `ArchitectureRuleValidator.java` | failCount를 `verdict=FAIL` 기준으로 집계 |
| `ArchitectureRuleNegativeCasesTest.java` | broken graph → `validateAll` status/failCount IT 추가 |
| `OntologyQueryService.java` | 가짜 HANDLED_BY→Table 제거, `pathStatus` COMPLETE/PARTIAL/UNRESOLVED |
| `ReverseImpactUnitTest.java` | HANDLED_BY→TABLE 금지 테스트 |
| `WorkbenchStaticUiTest.java` | design.js 200 + META/Router 소스 검증 |
| `ONTOLOGY-SOURCE-CODE-REVIEW-FIX-REPORT.md` | 본 보고서 |

---

## 3. 수정 코드 요지

### P0-01
```java
long fails = findings.stream()
    .filter(f -> "FAIL".equals(f.get("verdict")))
    .count();
```

### P0-03
- `synthesizeImpactPaths`: 존재하지 않는 head edge 삭제
- forward `traverse`로 얻은 **실제 DESIGN relation**만 reverse replay
- classification ancestor는 store에 있는 HAS_* / BELONGS_TO_PROGRAM만 사용
- impact 응답에 `pathStatus`: `COMPLETE` | `PARTIAL` | `UNRESOLVED`

### P0-02
- 자산 존재 확인 + 테스트 보강 (신규 기능 확장 없음)

---

## 4. 신규 테스트

| 테스트 | 검증 |
|--------|------|
| `validateAll_broken_graph_status_fail_and_failCount_positive` | 깨진 Graph → status=FAIL, failCount>0, failCount==verdict FAIL 수 |
| `impact_paths_never_invent_handledBy_to_table` | HANDLED_BY target에 table: / TABLE 없음 |
| `workbench_app_js_has_design_route_and_meta` | META.design, DesignAssistant.render, design.js 200 |

개별 rule 메서드 FAIL만으로 P0-01을 완료 처리하지 않음.

---

## 5. 실제 테스트 결과

```text
gradlew.bat test
  --tests nhnis.ontology.validate.*
  --tests nhnis.ontology.query.ReverseImpactUnitTest
  --tests nhnis.ontology.web.WorkbenchStaticUiTest
  --tests nhnis.ontology.web.TableImpactQueryTest

BUILD SUCCESSFUL
```

---

## 6. 회귀 결과

| 영역 | 결과 |
|------|------|
| ArchitectureRuleValidation (golden PASS) | PASS |
| ArchitectureRuleNegativeCases | PASS |
| ReverseImpact / TableImpact | PASS |
| Workbench static UI | PASS |

---

## 7. 아직 남은 P1 / P2 (분석만)

| ID | 항목 | 메모 |
|----|------|------|
| P1-01 | RecommendService가 OntologyRegistry YAML 휴리스틱에 의존 | Graph Concept 기반 추천과 분리·보완 필요 |
| P1-02 | Provenance.sourceCode/경로 추측도 VERIFIED 가능 | 검증 등급 세분화(DISCOVERED vs VERIFIED) |
| P1-03 | `classificationPath`가 Relation 확인 없이 ServiceId 파싱으로 단계 생성 | Search UI overview에 가상 edge 혼입 위험 |
| P1-04 | `summarizeStructure`의 `findFirst()` 비결정성 | 다중 CALLS 시 체인 요약이 흔들림 |
| P1-05 | Gate UI가 입력 ServiceId가 아니라 전체 `validateAll()` | Design/Gate 화면 대상 스코프 필터 필요 |
| P1-06 | `/reload`가 Registry만 reload, OntologyStore 미갱신 | bootstrap/reload 일관성 |
| P1-07 | Workbench `detectType` Program regex가 `mgcoa8888`(8자)와 불일치 | AUTO 검색 오분류 |
| P1-08 | 외부 Google Fonts 의존 | 오프라인/보안 환경에서 실패 가능 → 로컬 폰트 검토 |

P2(후속): ArchitecturePattern 영속 모델, Design session 서버 저장, `/design/recommend` 통합 API 필요성 재평가.

---

## 8. Architecture Design Assistant 구현 재개 가능 여부

| 질문 | 답 |
|------|----|
| P0 모두 해결? | **예** (P0-01/02/03) |
| Design Assistant를 “완료”로 판정? | **아니오** — 본 문서는 P0 정합성 수정 보고이며, Design 완료 판정은 하지 않음 |
| 재개 가능? | **가능**. P0 차단 사유는 해소됨. 재개 시 P1-05(Gate 스코프), P1-07(detectType), DERIVED_PATTERN/UNRESOLVED 정책을 유지할 것 |

---

## 판정

**P0 Source Integrity: FIXED**  
**Architecture Design Assistant: NOT DECLARED COMPLETE (재개 가능)**
