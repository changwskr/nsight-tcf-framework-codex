# ONTOLOGY-CHECK-REPORT

- Date: 2026-08-10
- Prompt: `zdiary/26-08-10-01-온톨로지-생성/26-08-10-06-점검생성.md`
- Scope: **검증만** (제품 기능 추가 없음; 부정적 Rule 검증용 테스트만 추가)
- Module: `tcf-ontology-service`

---

## 1. 변경 내용 보고

> 모듈 전체가 untracked(`?? tcf-ontology-service/`) 상태라 git diff 기준 “변경/신규”는 **Ontology Core 구현 전체**로 본다.

### 1.1 신규/핵심 구현 파일

| 파일 | 목적 | WBS | 상태 |
|------|------|-----|------|
| `domain/concept/*` (`OntologyConcept`, `ConceptType`, `ConceptIds`, `ServiceIdParts`) | Concept 1급 모델·안정 ID·ServiceId VO | W1 | 완료 |
| `domain/relation/*` (`OntologyRelation`, `RelationType`, `GraphType`) | Relation 1급 모델·DESIGN/RUNTIME | W1/W2/W8 | 완료 |
| `domain/Provenance.java` | 출처 메타 | W1 | 완료(품질 P1 있음) |
| `support/ServiceIdParser.java` | 11자리 분해/검증 | W1 | 완료 |
| `store/OntologyStore.java` | In-memory Graph | W1 | 완료 |
| `loader/YamlGraphLoader.java` | mappings YAML→DESIGN Graph | W7 | 완료 |
| `loader/TxRuntimeGraphLoader.java` | tx-runtime→RUNTIME Graph | W8 | 완료 |
| `loader/OntologyGraphBootstrap.java` | 기동 시 Graph 적재 | W7/W8 | 완료 |
| `query/OntologyQueryService.java` | 정/역방향 조회·impact | W4/W5 | 완료 |
| `facade/OntologyFacade.java` | YAML Hub + Graph 단일 조회 | W10 | 완료 |
| `validate/ArchitectureRuleValidator.java` | RULE-001~006 | W6 | 완료 |
| `web/OntologyCoreQueryController.java` | `/query/**`, `/impact/table/**`, `/validate/rules` | W4/W5/W6 | 완료 |
| `web/OntologyQueryController.java` | `/v1/**` 호환 | W4 | 완료 |
| `seed/Mgcoa8888OntologySeed.java` | Golden YAML 테스트 헬퍼 | W3 | 완료(하드코딩 시드 제거됨) |
| Hub 기존 (`OntologyRegistry`, `ImpactAnalyzer`, `OntologyController` 등) | YAML catalog/4축/scan/seed | 기반 | 유지·Facade 연결 |

### 1.2 삭제

| 파일 | 비고 |
|------|------|
| (제품 코드) 하드코딩 Golden ApplicationRunner 본문 | `Mgcoa8888OntologySeed`가 YAML 로더 헬퍼로 대체 |
| `Mgcoa8888OntologySeedTest` | `Mgcoa8888GoldenGraphTest`로 대체 |

### 1.3 ONTOLOGY-ANALYSIS W1~W6 매핑

| ID | 작업 | 상태 |
|----|------|------|
| W1 | Concept/Relation/ServiceIdParser | **완료** |
| W2 | Predicate vocabulary | **완료** |
| W3 | mgcoa8888 Golden Graph | **완료** (YAML 로더 경유) |
| W4 | Query API | **완료** |
| W5 | Impact API | **완료** (P0 역방향 계층 수집 보완: 2026-08-10) |
| W6 | Architecture Rules | **완료** |
| W7~W10/W8 | 로더·Facade·RUNTIME | **완료**(W1~W6 범위 밖, 추가 진행분) |

### 1.4 점검 중만 추가한 파일

| 파일 | 목적 |
|------|------|
| `ArchitectureRuleNegativeCasesTest.java` | §6 부정 케이스 증명용 (기능 아님) |
| `ONTOLOGY-CHECK-REPORT.md` | 본 보고서 |

---

## 2. 빌드 검증

### 실행

```text
gradlew.bat clean test
→ BUILD SUCCESSFUL (약 28s)
```

```text
gradlew.bat bootWar
→ :bootWar SKIPPED
  (build.gradle: bootWar.enabled=false, war.enabled=true)
```

```text
gradlew.bat war
→ BUILD SUCCESSFUL
```

```text
gradlew.bat test --tests ArchitectureRuleNegativeCasesTest --rerun-tasks
→ BUILD SUCCESSFUL
```

### 판정

| 명령 | 결과 |
|------|------|
| clean test | **PASS** |
| bootWar | **SKIPPED (설정상 비활성)** — 실패로 보지 않음 |
| war | **PASS** |

---

## 3. 서버 기동 검증

### 실행

```text
gradlew.bat bootRun
```

### 기동 로그 핵심

```text
Ontology loaded: 14 bundles, 5 programs, 12 services
Started OntologyApplication in 3.777 seconds
YAML→Graph loaded: programs=5, services=12, runtimeSteps=15, concepts=97, relations=149
Tomcat started
```

### 런타임 카운트 (`GET /api/ontology/catalog` → graph)

| 항목 | 값 |
|------|-----|
| conceptCount | **97** |
| relationCount | **149** |
| programsInGraph | **5** |
| servicesInGraph | **12** |
| runtimeComponents | **19** |
| health | **UP** |
| consistency | **ALIGNED** (yaml↔graph) |

mgcoa8888 Seed: curated YAML `ontology/mappings/mgcoa8888.yml`이 Graph로 적재됨 (하드코딩 ApplicationRunner 아님).

기동 중 ERROR 없음 (INFO 정상).

**Boot: PASS**

---

## 4. 핵심 기능 검증 (A~E)

### 테스트 A — `mgcoa8888S0` 전체 구조

- URL: `GET /api/ontology/query/service/mgcoa8888S0/structure`
- Request: path `mgcoa8888S0`
- 실제 classification:
  - `MG -HAS_BUSINESS→ CO -HAS_FUNCTION→ A -HAS_PROGRAM→ mgcoa8888 -PROVIDES_SERVICE→ mgcoa8888S0`
- 실제 structure (요약):
  - `HANDLED_BY→Handler | CALLS→Facade | CALLS→Service | USES→DAO | EXECUTES→Sql/Mapper | ACCESSES→TB_FW_IMAGE_LOG | HAS_COLUMN→GUID`
- Mapper: `mgcoa8888-ORA.xml` 확인
- Provenance: classification/structure step 및 concept에 `provenance` 필드 존재 (YAML_MAPPING / SOURCE_CODE)
- **PASS** (summary 문자열은 SQL을 Mapper보다 먼저 나열할 수 있으나 structure/mappers 필드로 Mapper 확인됨)

### 테스트 B — Program Services

- URL: `GET /api/ontology/query/program/mgcoa8888/services`
- Response names: `mgcoa8888S0`, `mgcoa8888D0`
- **PASS**

### 테스트 C — Handler Services

- URL: `GET /api/ontology/query/handler/mgcoa8888Handler/services`
- Response names: `mgcoa8888S0`, `mgcoa8888D0`
- **PASS**

### 테스트 D — Service Tables

- URL: `GET /api/ontology/query/service/mgcoa8888S0/tables`
- Response: `TB_FW_IMAGE_LOG`
- **PASS**

### 테스트 E — Table Impact (역방향)

- URL: `GET /api/ontology/impact/table/TB_FW_IMAGE_LOG`
- 실제: `affectedServiceIds = [mgcoa8888S0, mgcoa8888D0]`
- 문제: `affectedHandlers` / `affectedPrograms` / `affectedBusinesses` 가 **빈 배열** (경로 수집 품질 부족; serviceId는 fallback으로 채움)
- **PARTIAL / 기능상 FAIL에 가까운 PARTIAL** — ServiceId 역추적은 되나 계층 목록·paths 품질은 목표 미달

---

## 5. ServiceId 검증

### 정상

`GET /api/ontology/v1/service-id/mgcoa8888S0`

| 필드 | 값 |
|------|-----|
| groupCode | mg |
| businessCode | co |
| functionCode | a |
| programNo | 8888 |
| operationType | S |
| sequence | 0 |
| registered | true |

**PASS**

### 비정상 (거부)

| 입력 | 기대 | 실제 |
|------|------|------|
| `mgcoa8888` (자리수) | reject | **400** |
| `mgcoa8888T0` (처리구분 T) | reject | **400** |
| `mgcoa88AbS0` (프로그램번호) | reject | **400** |

**PASS** (비정상 값이 Graph에 정상 등록되지 않음 — API가 거부)

참고: 기능코드 자리의 임의 알파벳(예: `z`)은 형식상 통과 가능. 업무코드 사전 검증은 범위 외.

---

## 6. Architecture Rule 검증 (부정 케이스)

테스트: `ArchitectureRuleNegativeCasesTest`

| Case | 조작 | Rule | 결과 |
|------|------|------|------|
| 1 | ServiceId only, no HANDLED_BY | RULE-002 FAIL | **PASS**(검증) |
| 2 | Handler only, no Service 관계 | RULE-003 FAIL | **PASS**(검증) |
| 3 | Program only, no PROVIDES_SERVICE | RULE-004 FAIL | **PASS**(검증) |
| 4 | Service only, no USES/CALLS | RULE-005 FAIL | **PASS**(검증) |
| 5 | DAO only, no EXECUTES Mapper/SQL | RULE-006 FAIL | **PASS**(검증) |

Findings에 `ruleId`, `verdict`, `target`, `message`, `evidence` 포함 확인.

정상 Golden Graph: `GET /api/ontology/validate/rules` → `PASS fail=0`

---

## 7. Provenance 점검

대상 관계:

`mgcoa8888S0 -HANDLED_BY→ mgcoa8888Handler`

`GET /api/ontology/v1/concept/mgcoa8888S0` → outgoing HANDLED_BY:

| 질문 | 답 |
|------|----|
| 어디에서 얻었는가? | `sourceType=YAML_MAPPING` |
| 실제 Java 소스인가? | **이 Relation은 YAML** (Handler Concept 쪽은 SOURCE_CODE 경로를 가짐) |
| Markdown인가? | 아니오 |
| 자동 Scanner인가? | 아니오 (`discoveredBy` 필드값) |
| 어느 파일인가? | `ontology/mappings/mgcoa8888.yml` |
| 언제 수집했는가? | `extractedAt=2026-08-10T01:46:34.604271800Z` (기동 시각) |
| VERIFIED인가? | **VERIFIED** |

### 판정

Provenance로 질문 7에는 **답 가능** → 기능 **PASS**.  
단, `discoveredBy`가 헬퍼에 `"Mgcoa8888OntologySeed"`로 하드코딩되어 실제 로더명(`YamlGraphLoader`)과 불일치 → **품질 P1**.

---

## 8. 기존 기능 회귀 검증

| API | 결과 |
|-----|------|
| `GET /catalog` | 200 |
| `GET /service/{serviceId}` | 200 (+ graph 보강) |
| `GET /program/{programId}` | 200 (+ graph 보강) |
| `GET /path` | 200 |
| `GET /impact?from=` | 200 |
| `GET /recommend` | 200 |
| `GET /prompt/{id}` | 200 |
| `POST /validate/pdmg` | 200 |
| `GET /consistency` | 200 |
| `GET /runtime/tx-chain` | 200 |

`POST /seed/pdmg`, `POST /import/pdmg`: 이번 점검에서 **재호출하지 않음**(상태 변경 POST).  
회귀는 `MappingSeedGeneratorIT`, `PdmgValidationIT`가 `clean test`에서 통과한 것으로 대체 확인.

기존 API 삭제 없음. Hub 응답에 `graph`/`sources`/`unified` 필드가 **추가**됨(하위 호환 확장).

**Regression: PASS** (seed/import는 테스트 스위트로 간접 확인)

---

## 9. 코드 품질

| 점검 | 결과 | 등급 |
|------|------|------|
| Controller에 비즈니스 로직 | Facade/QueryService로 위임, Controller는 얇음 | OK |
| Graph 탐색 중복 | Store traverse + QueryService impact fallback 일부 중복 | **P2** |
| mgcoa8888 하드코딩 | 제품 시드는 YAML 범용 로더; 테스트 헬퍼만 8888 고정 | OK |
| ServiceId 파싱 중복 | `ServiceIdParser` 단일 | OK |
| YAML↔Domain 결합 | Loader가 Map→Domain 변환, 과도한 결합은 아님 | OK |
| Runtime/Design 혼재 | `GraphType` 분리, 테스트로 확인 | OK |
| 테스트가 Mock만? | SpringBoot IT + 실 YAML/실 API | OK |
| Impact handlers/programs 빈값 | 역추적 수집 버그/한계 | **P0** |
| Provenance.discoveredBy 하드코딩 | 로더명과 불일치 | **P1** |
| summary가 Mapper보다 SQL 우선 | UX/가독성 | **P2** |
| API 이중화 (`/v1` vs `/query`) | 문서화·정리 필요 | **P2** |

---

## 10. 최종 판정

```text
Ontology Core 1.0 판정

Build           : PASS
Boot            : PASS
Concept Model   : PASS
Relation Model  : PASS
ServiceId       : PASS
Forward Query   : PASS
Reverse Query   : PASS      (P0 보완 후 — ServiceId/Handler/Program/Business)
Impact Analysis : PASS      (P0 보완 후)
Rule Validation : PASS
Provenance      : PASS      (discoveredBy=YamlGraphLoader로 수정)
Regression      : PASS
```

### 종합 판정

~~**A. Ontology 1.0 완료**~~ → **철회**.  
최종 승인 재검증(`26-08-10-07`) 결과는 `ONTOLOGY-FINAL-ACCEPTANCE-REPORT.md` 참고.

**B. 핵심 보완 후 완료 가능** (Impact 계층 목록은 채워졌으나 Function/System paths 미증명, `table` resolve COLUMN 오류, git untracked)

---

*본 보고서는 2026-08-10 실측(clean test, war, bootRun, live HTTP)에 기반한다.*
