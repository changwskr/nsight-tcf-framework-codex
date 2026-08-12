# ONTOLOGY-1.0-IMPLEMENTATION-REPORT

작성일: 2026-08-10  
대상: `tcf-ontology-service` Ontology Core 1.0  
기준 문서: `zdiary/26-08-10-01-온톨로지-생성/26-08-10-04-온톨로지생성-구현지시-프롬프트.md`

## 1. 변경 파일 목록

### 신규
- `src/main/java/nhnis/ontology/domain/Provenance.java`
- `src/main/java/nhnis/ontology/domain/concept/ConceptIds.java`
- `src/main/java/nhnis/ontology/domain/relation/GraphType.java`
- `src/main/java/nhnis/ontology/validate/ArchitectureRuleValidator.java`
- `src/main/java/nhnis/ontology/web/OntologyCoreQueryController.java`
- 지시서 요구 테스트 클래스 일식 (`Mgcoa8888GoldenGraphTest`, `*QueryTest`, `ArchitectureRuleValidationTest` 등)
- `ONTOLOGY-1.0-IMPLEMENTATION-REPORT.md` (본 문서)

### 수정
- Concept/Relation 도메인, `OntologyStore`, `Mgcoa8888OntologySeed`, `OntologyQueryService`
- `ontology/core/relations.yml` (PROVIDES_SERVICE canonical, CALLS/USES/ACCESSES/HAS_COLUMN)
- `RelationPredicates`, `OntologyGraphService` (HAS_SERVICE → PROVIDES_SERVICE)
- 기존 호환 API: `/api/ontology/v1/**` 유지

## 2. 신규 클래스

| 클래스 | 역할 |
|--------|------|
| `Provenance` | 출처/검증 상태 |
| `ConceptIds` | 안정 ID 생성 |
| `GraphType` | DESIGN / RUNTIME |
| `ArchitectureRuleValidator` | RULE-001~006 |
| `OntologyCoreQueryController` | `/query/**`, `/impact/table/**`, `/validate/rules` |

## 3. Concept 모델

`OntologyConcept`: `id`, `type`, `name`, `description`, `attributes`, `version`, `status`, `provenance`

Type: `SYSTEM`, `BUSINESS`, `FUNCTION`, `PROGRAM`, `SERVICE_ID`, `COMPONENT`, `MAPPER`, `SQL_ID`, `TABLE`, `COLUMN`

안정 ID 예:
- `system:MG`
- `business:MG:CO`
- `function:MG:CO:A`
- `program:MG:CO:A:8888`
- `service:mgcoa8888S0`
- `component:nhnis.mg.co.a.entry.handler.mgcoa8888Handler`
- `table:RDW:TB_FW_IMAGE_LOG`

Alias 조회 지원: `MG`, `CO`, `mgcoa8888`, `mgcoa8888S0`, `TB_FW_IMAGE_LOG`, `mgcoa8888Handler` 등

## 4. Relation 모델

`OntologyRelation`: `id`, `fromId`, `predicate`, `toId`, `graphType`, `attributes`, `version`, `status`, `provenance`

## 5. Predicate 목록

Canonical: `HAS_BUSINESS`, `HAS_FUNCTION`, `HAS_PROGRAM`, `PROVIDES_SERVICE`, `BELONGS_TO_PROGRAM`, `HANDLED_BY`, `CALLS`, `USES`, `EXECUTES`, `ACCESSES`, `HAS_COLUMN`

Alias: `HAS_SERVICE` → `PROVIDES_SERVICE` (삭제하지 않음)

## 6. Provenance 구조

`sourceType`, `sourceSystem`, `sourcePath`, `sourceDocument`, `sourceCommit`, `discoveredBy`, `extractedAt`, `verifiedAt`, `verificationStatus`

Golden Graph 출처 예:
- YAML: `ontology/mappings/mgcoa8888.yml`
- Source: `pdmg-service/.../mgcoa8888Handler.java`, `.../mgcoa8888-ORA.xml` 등

## 7. ServiceId 구조

`ServiceIdParser` / `ServiceIdParts`: group/business/function/programNo/operationType/sequence/fullServiceId  
예: `mgcoa8888S0` → mg / co / a / 8888 / S / 0

## 8. mgcoa8888 Golden Graph

```
MG -HAS_BUSINESS→ CO -HAS_FUNCTION→ A -HAS_PROGRAM→ mgcoa8888
mgcoa8888 -PROVIDES_SERVICE→ mgcoa8888S0 / mgcoa8888D0
mgcoa8888S0 -HANDLED_BY→ Handler -CALLS→ Facade -CALLS→ Service -USES→ DAO
DAO -EXECUTES→ Mapper / SqlId -ACCESSES→ TB_FW_IMAGE_LOG -HAS_COLUMN→ GUID
```

확인된 소스 값만 사용 (임의 생성 없음).

## 9. API 목록

| Method | Path | 상태 |
|--------|------|------|
| GET | `/api/ontology/query/service/{serviceId}/structure` | 구현 |
| GET | `/api/ontology/query/program/{programId}/services` | 구현 |
| GET | `/api/ontology/query/handler/{handler}/services` | 구현 (짧은 이름 권장) |
| GET | `/api/ontology/query/table/{table}/services` | 구현 |
| GET | `/api/ontology/query/service/{serviceId}/tables` | 구현 |
| GET | `/api/ontology/query/business/{businessCode}/tree` | 구현 |
| GET | `/api/ontology/impact/table/{tableName}` | 구현 (+ paths) |
| GET | `/api/ontology/validate/rules` | 구현 |
| GET | `/api/ontology/v1/**` | 유지 (호환) |
| GET | `/api/ontology/catalog|service|impact?from=...` 등 | 유지 |

## 10. Rule 목록

| Rule | 내용 | 상태 |
|------|------|------|
| RULE-001 | ServiceId 11자리 | 구현 |
| RULE-002 | ServiceId ↔ Handler | 구현 |
| RULE-003 | Handler-ServiceId 일치 | 구현 (programId prefix 기준) |
| RULE-004 | Program ≥1 ServiceId | 구현 |
| RULE-005 | Service → DAO/Client | 구현 |
| RULE-006 | DAO → Mapper/SqlId | 구현 |

## 11. 테스트 목록

- `ServiceIdParserTest`
- `Mgcoa8888GoldenGraphTest`
- `ServiceStructureQueryTest`
- `ProgramServicesQueryTest`
- `HandlerServicesQueryTest`
- `TableServicesReverseQueryTest`
- `ServiceTablesQueryTest`
- `BusinessTreeQueryTest`
- `TableImpactQueryTest`
- `ArchitectureRuleValidationTest`
- 기존 `OntologyControllerTest`, `HealthControllerTest`, `ImpactAndPromptIT` 등

## 12. 테스트 결과

```
.\gradlew.bat test
BUILD SUCCESSFUL
```

(2026-08-10 실행)

## 13. 기존 기능과 호환성

- 기존 catalog/service/path/recommend/prompt/validate/seed API 유지
- 기존 `/api/ontology/impact?from=` 유지 (신규는 `/impact/table/{table}`)
- Graph edge predicate를 `PROVIDES_SERVICE`로 정렬 (구 `HAS_SERVICE`는 alias)

## 14. 남은 Gap

| 항목 | 상태 |
|------|------|
| YAML → Graph 범용 로더 (전 매핑 자동) | 부분 (Golden은 시드 코드, YAML을 SoT로 참조) |
| RUNTIME Graph 시드 (Dispatcher/TX) | 미구현 (모델만) |
| Handler FQCN path variable | 짧은 이름 alias로 우회 |
| H2 영속화 | 의도적으로 미구현 |
| RULE-003 Handler 소스 어노테이션 실스캔 대조 | 근사 검증만 |
| Neo4j/Jena/LLM/RAG | 의도적으로 미구현 |

## 15. Ontology 1.5 추천 작업

1. 모든 `ontology/mappings/*.yml` → Concept/Relation 자동 적재
2. RUNTIME Graph (Dispatcher, TimeoutExecutor, UnitOfWork) 시드
3. Handler `@Service`/`register` 소스 스캔과 RULE-003 완전 대조
4. Impact path 시각화 / prompt exporter와 Provenance 연계
5. 선택적 H2 스냅샷 영속화

---

### 완료 조건 대응 (질문 1~7)

| 질문 | API |
|------|-----|
| 1. mgcoa8888S0는 무엇인가? | `/query/service/mgcoa8888S0/structure` + `/v1/service-id/...` |
| 2. 어느 업무/기능? | structure.classification / business tree |
| 3. Handler? | structure HANDLED_BY |
| 4. Facade/Service/DAO? | structure CALLS/USES |
| 5. Mapper/SQL/Table? | structure + `/query/service/.../tables` |
| 6. Table 영향 ServiceId? | `/impact/table/TB_FW_IMAGE_LOG` |
| 7. 출처? | 각 concept/relation `provenance` |
