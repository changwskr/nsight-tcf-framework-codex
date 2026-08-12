# TCF Ontology 재점검(26-08-10-15) P0 보완 보고서

- 작성일: 2026-08-10
- 기준: `26-08-10-15-TCF_Ontology_전체소스_재점검_보고서.md`
- 빌드: `gradlew.bat clean test war` → **BUILD SUCCESSFUL**

---

## 재판정

> **INTERNAL PILOT READY → RELEASE CANDIDATE (P0 해소)**
>
> Production Governance / AuthN·RBAC / GraphPath 영속은 여전히 PARTIAL → 완전 Production Governance Ready는 아님

---

## P0 조치

| ID | 항목 | 결과 |
|----|------|------|
| P0-01 | validateService 무한 재귀 | **FIXED** — Graph 미등록 → `NOT_FOUND`, DESIGN과 상호 재귀 제거 |
| P0-02 | Context Export DTO 계약 | **FIXED** — baseline.message/transaction/paging evidenceField 정본 + UI `evidenceValue()` |
| P0-03 | operationMatch 0건 status | **FIXED** — `OPERATION_NO_MATCH` (MIXED/REPORT) |
| P0-04 | Pattern 하드코딩 구조 | **FIXED** — `serviceStructure()` 공통 경로 DERIVED, 없으면 UNRESOLVED |
| P0-05 | Production safe default | **FIXED** — `admin-mutations-enabled=false` 기본, local/dev만 true, `profiles.active: local` 제거, RUN.bat에 local 명시 |

---

## P1 조치 (부분)

| ID | 결과 |
|----|------|
| P1-02 RULE-003 명칭 | Ontology Relation Consistency로 축소 + `sourceBacked=false` |
| P1-04 relationStatus | `PRESENT`/`INFERRED` + verificationStatus 분리 |
| P1-05 Design Gate status | `PASS_WITH_UNRESOLVED` |
| P1-06 Multi-table synthetic test | **추가** |
| P1-07 Top-N 전 op match 우선 | RecommendService에서 op 매칭 후보 우선 절단 |
| P1-09 Prompt runtime | `tx-runtime.yml` steps 동적 조립 |
| P1-10 Stale golden | `test-data/queries/_archive/2026-08-10-stale/` 이동 |
| P1-01/03/08/11/12 | DEFERRED (Gate 계층 분리, Scanner closed-loop, score 분리, version, atomic reload) |

---

## 추가/변경 테스트

- `ArchitectureRuleScopedValidationTest`: unregistered NOT_FOUND, allocated NOT_YET_IMPLEMENTED, PASS_WITH_UNRESOLVED
- `DesignRecommendationServiceTest`: MIXED/REPORT OPERATION_NO_MATCH, baseline export contract
- `CompositePkAndMultiTableLoaderTest`: synthetic multi-table
- `OntologyPropertiesDefaultTest` / `LocalProfileTest`

---

## 최종 테스트 증거

전체 Suite/Test 수는 `build/test-results/test` 기준 재집계 (failures=0).

금지 위반 없음:

- 미등록 ServiceId StackOverflow 없음
- Export undefined/[object Object] 계약 제거
- MIXED/REPORT가 S로 위장하지 않음
- packaged admin mutation 기본 OFF
