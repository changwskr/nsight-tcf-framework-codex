# Ontology P1 Integrity Report

- 작성일: 2026-08-10
- 기준: 지시서 Phase 3

---

## 항목별 결과

| ID | 항목 | 결과 | 비고 |
|----|------|------|------|
| P1-01 | Provenance VERIFIED 정책 | **DONE** | YAML/추정 sourceCode → DISCOVERED; Scanner VERIFIED 유지 |
| P1-02 | classificationPath 가상 Relation | **DONE** | `relationStatus` VERIFIED/INFERRED |
| P1-03 | summarizeStructure findFirst | **PARTIAL** | 정렬+존재 기반. 다중 path 정본 API는 미도입 |
| P1-04 | Impact GraphPath 모델 | **DEFERRED** | Flat reverse/synthesize 유지. 영속 GraphPath는 P2 |
| P1-05 | Alias ambiguity | **PARTIAL** | TABLE type filter 있음. 범용 alias Map은 단일 |
| P1-06 | Registry/Store Reload | **DONE** | `POST /reload` → registry.reload + store.clear + bootstrap.loadAll |
| P1-07 | MappingSeed SQL DRAFT | **DEFERRED** | Regex seed 한계 문서화. DRAFT 강제 표기는 후속 |
| P1-08 | OntologyValidator MISSING | **DEFERRED** | null skip 유지. MISSING 상태 모델 후속 |
| P1-09 | Program AUTO regex | **DONE** | 9자 Program / 11자 ServiceId |
| P1-10 | Business/Function UI 동적화 | **DONE** | catalog programs에서 Business/Function 옵션 추출 |
| P1-11 | Google Fonts 제거 | **DONE** | 시스템 한글 폰트 스택 |
| P1-12 | 보안/권한 | **PARTIAL** | `nhnis.ontology.admin-mutations-enabled` (prod=false). AuthN/AuthZ는 게이트웨이 전제 |

---

## 잔여 리스크 (Release 비차단 / 후속)

1. GraphPath 영속 모델 부재 → Impact 경로 설명력 한계
2. Alias 충돌 시 명시적 ambiguity 응답 부재
3. Seed SQL table 추출은 DRAFT 수준으로 운영 표기 필요
4. 관리 API는 플래그로 차단 가능하나 본격 RBAC 아님

이 항목들은 Knowledge/Release 필수 Gate를 깨지 않으나 Architecture Intelligence 고도화 시 우선 처리한다.
