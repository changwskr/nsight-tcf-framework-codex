# ADR-0001: Ontology as NSIGHT Knowledge Hub Source of Truth

- Status: Accepted
- Date: 2026-08-10
- Context: PDMG 파일럿 (`pdmg-fw` / `pdmg-service` / `pdmg-ui`)

## Mission

**tcf-ontology-service** = NSIGHT/PDMG의 업무·아키텍처·개발표준·프로그램·데이터 간 관계를  
기계가 이해할 수 있는 형태로 축적하고,  
신규 시스템 구축 시 기존 지식과 표준을 **조회·검증·추천**하는 서비스이다.

## Decision

1. **기계 정본**은 `tcf-ontology-service/ontology/**` 이다.
2. **사람용 문서**(`pdmg-service/docs/*`)는 뷰·해설이며, 충돌 시 ontology를 우선한다.
3. 포맷은 **JSON-LD 호환 YAML** 로 시작한다. (이후 RDF/OWL 승격 가능)
4. 동기화 우선순위: **문서 시드 → 소스 매핑 보강 → validation**.
5. 1차 데모 단위는 **프로그램 `mgcoa9001`(거래통제)** 의 아키텍처·개발·데이터·운영 4축 조회이다.
6. 능력 축은 **조회·검증·추천** 이며, 업무 거래 실행은 범위 밖이다.

## Consequences

- `tcf-ontology-service`는 업무 거래를 실행하지 않는다.
- pdmg-\* 변경 시 mappings/shapes 갱신 또는 validation 리포트로 드리프트를 드러낸다.
- CRUD 프롬프트·설계서는 ontology 조회·추천 결과를 컨텍스트로 사용한다.

## Non-Goals (Phase 1+)

- 전 NSIGHT 도메인 일괄 적재
- CI fail 게이트 (로컬 `validatePdmg` 이후 단계)
- 실시간 pdmg 소스 파일 감시
- 온라인 거래·채널 UI 대체
