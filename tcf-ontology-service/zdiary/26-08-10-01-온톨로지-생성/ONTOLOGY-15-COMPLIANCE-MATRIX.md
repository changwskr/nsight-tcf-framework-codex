# 26-08-10-15 재점검 보고서 준수 매트릭스

- 작성일: 2026-08-10
- 기준 문서: `26-08-10-15-TCF_Ontology_전체소스_재점검_보고서.md`
- 빌드 증거: `gradlew.bat clean test war` (본 라운드 재실행)

---

## 종합 재판정

| 영역 | 보고서 당시 | 현재 |
|------|-------------|------|
| Ontology Core | GOOD | GOOD |
| Workbench | GOOD | GOOD |
| Impact | GOOD | GOOD |
| WAR/Context Path | GOOD | GOOD |
| Design Assistant | PILOT | **PILOT+ (P0 해소)** |
| Architecture Gate | PARTIAL | **PARTIAL+** (Integrity Gate 명시, Application R-* 미구현 표기) |
| Source Verification | PARTIAL | **PARTIAL+** (`/evidence/upgrade` closed-loop 최소 도입) |
| Cursor Context Export | FIX REQUIRED | **FIXED** |
| Production Security | FIX REQUIRED | **FIXED (default OFF)** |

> **INTERNAL PILOT READY / RELEASE CANDIDATE**
>
> Production Architecture Governance Ready는 아직 아님 (AuthN/RBAC, Application R-* Gate 실행기).

---

## P0

| ID | 상태 |
|----|------|
| P0-01 무한 재귀 | **DONE** |
| P0-02 Export DTO | **DONE** |
| P0-03 OPERATION_NO_MATCH | **DONE** |
| P0-04 Pattern Evidence | **DONE** |
| P0-05 Prod safe default | **DONE** |

## P1

| ID | 상태 |
|----|------|
| P1-01 Gate 계층 | **PARTIAL** — `gateFamily=ONTOLOGY_INTEGRITY_GATE`, R-* YAML에 implemented=false 표기 |
| P1-02 RULE-003 의미 | **DONE** |
| P1-03 Scanner VERIFIED loop | **PARTIAL** — `OntologyEvidenceMerger` + `POST /evidence/upgrade` |
| P1-04 relationStatus PRESENT | **DONE** |
| P1-05 PASS_WITH_UNRESOLVED | **DONE** |
| P1-06 Multi-table test | **DONE** |
| P1-07 Top-N op 우선 | **DONE** |
| P1-08 Score 분리 | **DEFERRED** |
| P1-09 Prompt runtime 동적 | **DONE** |
| P1-10 Stale golden | **DONE** (archive) |
| P1-11 Version | **DONE** — product 0.1.0-RC1 / schema 1.0 / snapshot 2026.08.10.03 |
| P1-12 Reload 원자성 | **DONE** — temp store → `replaceFrom` atomic swap |

## §6 필수 테스트

| ID | 상태 |
|----|------|
| T-NEW-001 unregistered ServiceId | DONE |
| T-NEW-002 Export contract | DONE |
| T-NEW-003/004/005 MIXED/REPORT no-match | DONE |
| T-NEW-006 multi-table synthetic | DONE |
| T-NEW-007 RULE-005 wrong target | DONE |
| T-NEW-008 admin default disabled | DONE |
| T-NEW-009 Prompt runtime dynamic | DONE |
| T-NEW-010 stale golden freshness | DONE (archive + README) |

---

## 남은 Production Gap

1. Application Architecture Gate 실행기 (R-* YAML implemented=true로 승격)
2. AuthN/AuthZ/RBAC
3. Recommendation score 축 분리
4. GraphPath 영속 / Alias ambiguity 응답
5. Evidence upgrade의 AST/Java registration 수준 강화
