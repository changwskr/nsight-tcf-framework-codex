# Ontology Design Overview

## Mission

**tcf-ontology-service** = NSIGHT/PDMG의 업무·아키텍처·개발표준·프로그램·데이터 간 관계를  
기계가 이해할 수 있는 형태로 축적하고,  
신규 시스템 구축 시 기존 지식과 표준을 **조회·검증·추천**하는 서비스이다.

→ [`mission.md`](./mission.md)

## Layers

```text
docs/                 사람용 (ADR, 설계 설명)
ontology/
  core/               공통 개념·컴포넌트 타입
  business/           MG 분류표
  technical/          런타임·TX
  mappings/           프로그램/서비스 실체 연결
  shapes/             형식 제약
  rules/              설계 규칙
  versions/           스냅샷 메타
```

## Capability

| 능력 | API/수단 |
|------|----------|
| 조회 | `/api/ontology/service/{id}`, `/program/{id}`, `/catalog` |
| 검증 | `/api/ontology/validate/pdmg`, `gradlew validatePdmg` |
| 추천 | `/api/ontology/impact`, `/prompt/{id}.md` |

## Seed sources (PDMG)

| source | ontology target |
|--------|-----------------|
| `00.NSIGHT 애플리케이션 코드 분류표.md` | business/ |
| `00.MG-NAMING_CONVENTION.md` | core/ + shapes/ |
| `00.BigPicture Tx 처리-1.md` | technical/ + rules/ |
| pdmg 소스·UI·Mapper | mappings/ (+ seed 자동화) |
