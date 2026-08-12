# ONTOLOGY-ANALYSIS

- Date: 2026-08-10 (재분석)
- Source prompt: `zdiary/26-08-10-01-온톨로지-생성/26-08-10-01-온톨로지생성분석-프롬프트.md`
- Scope: **분석만** (이 문서 작성 시 소스 대규모 수정 없음)
- Companion: `ONTOLOGY-1.0-IMPLEMENTATION-REPORT.md` (이미 구현된 Core 1.0 상세)
- Mission: NSIGHT/PDMG 지식을 구조화·조회·검증하고, 향후 Architecture Knowledge Base / LLM Context로 확장

---

## 1. 현재 `tcf-ontology-service` 디렉터리 구조

```text
tcf-ontology-service/                    # 독립 Gradle Boot WAR (Java 21, Boot 3.5.14)
├─ build.gradle / settings.gradle / gradlew*
├─ RUN.bat / README.md / 01.디렉토리구조.md
├─ ONTOLOGY-ANALYSIS.md                  # 본 문서
├─ ONTOLOGY-1.0-IMPLEMENTATION-REPORT.md
├─ config/                               # 배포 프로필 오버레이 샘플
├─ ontology/                             # ★ YAML Source of Truth (classpath 적재)
│  ├─ core/       concepts, meta-model, relations
│  ├─ business/   classification
│  ├─ technical/  tx-runtime.yml
│  ├─ shapes/     service-id.yml
│  ├─ rules/      component-boundaries.yml
│  ├─ mappings/   mgcoa8888|9000|9001|5530|9999 (+ _generated/)
│  └─ versions/
├─ docs/          ADR, API, ontology-design, operation
├─ scripts/       build / seed / import / validate / export
├─ src/main/java/nhnis/ontology/
│  ├─ domain/     Concept·Relation·Provenance (1.0 Core)
│  ├─ store/      OntologyStore (in-memory graph)
│  ├─ seed/       Mgcoa8888OntologySeed, MappingSeedGenerator
│  ├─ query/      OntologyQueryService
│  ├─ validate/   ArchitectureRuleValidator, OntologyValidator
│  ├─ ontology/   OntologyRegistry (YAML bundle)
│  ├─ graph/      OntologyGraphService (mapping 기반)
│  ├─ impact/     ImpactAnalyzer (구 API용)
│  ├─ scan/       PdmgInventoryScanner
│  ├─ prompt/     PromptContextExporter
│  ├─ recommend/  RecommendService
│  ├─ web/        Controllers
│  └─ support/    ServiceIdParser
├─ src/test/java/...
├─ test-data/
└─ zdiary/        작업 프롬프트
```

### 기술스택 AS-IS

| 항목 | 현황 |
|------|------|
| Spring Boot | 3.5.14 + WAR, port **8098** |
| Web | `spring-boot-starter-web` |
| Validation / Actuator / AOP | 사용 |
| DB / MyBatis / JPA | **미사용** (H2 dependency만 존재, 스키마·Repository 없음) |
| 온톨로지 저장 | YAML 파일 → 기동 시 로드 → **In-memory Concept/Relation Graph** |
| Graph DB / RDF / Jena / Neo4j | **미도입** (의도적) |
| pdmg-fw 의존 | **없음** (독립 모듈) |

---

## 2. 현재 구현된 클래스와 책임

### 2.1 Ontology Core 1.0 (Concept + Relation Graph)

| 클래스 | 책임 |
|--------|------|
| `OntologyConcept` / `ConceptType` / `ConceptIds` | 1급 Concept, 안정 ID (`system:MG`, `service:mgcoa8888S0` …) |
| `OntologyRelation` / `RelationType` / `GraphType` | 1급 Relation, DESIGN/RUNTIME 구분 |
| `Provenance` | 출처·검증상태 (YAML/SOURCE_CODE 등) |
| `ServiceIdParts` / `ServiceIdParser` | 11자리 ServiceId 분해 |
| `OntologyStore` | In-memory concept/relation + alias + traverse/reversePaths |
| `Mgcoa8888OntologySeed` | Golden Graph 시드 (ApplicationRunner) |
| `OntologyQueryService` | structure / program / handler / table / business / impact |
| `ArchitectureRuleValidator` | RULE-001~006 |
| `OntologyCoreQueryController` | `/api/ontology/query/**`, `/impact/table/**`, `/validate/rules` |
| `OntologyQueryController` | `/api/ontology/v1/**` (호환 조회) |

### 2.2 YAML Knowledge Hub (기존 파이프라인)

| 클래스 | 책임 |
|--------|------|
| `OntologyRegistry` | `ontology/**` YAML bundle 로드, program/service 인덱스, 4축 조립 |
| `OntologyGraphService` | Program 기준 nodes/edges, MG→CO→A path |
| `ImpactAnalyzer` | `?from=` 기반 blastRadius (구 API) |
| `OntologyValidator` | shapes + 소스 스캔 드리프트 |
| `PdmgInventoryScanner` / `InventorySnapshot` | pdmg-* 인벤토리 |
| `MappingSeedGenerator` | inventory → mappings YAML 초안 |
| `PromptContextExporter` | CRUD 프롬프트 Markdown/JSON |
| `RecommendService` | system/business/function/intent 패턴 추천 |
| `OntologyController` | catalog/service/path/recommend/prompt/import/seed/validate |
| `OntologyJobRunner` | CLI job (import/seed/validate/prompt) |
| `MetaTypes` / `RelationPredicates` | 상수 |
| `OntologyProperties` | scan 경로 설정 |

### 2.3 이중 지식 표현 (중요 관찰)

현재 서비스에는 **두 계층**이 공존한다.

1. **YAML Mapping Hub** (`OntologyRegistry` + 4축 API) — 프로그램 문서형 지식
2. **Concept/Relation Graph** (`OntologyStore` + query API) — 그래프형 지식 (Golden: mgcoa8888)

둘 다 가치 있으나, **전체 mappings → Graph 자동 적재**는 아직 없고 Golden은 시드 코드가 주도한다.

---

## 3. 현재 기능 중 재사용 가능한 것

1. **독립 Boot 모듈 골격** (WAR, UTF-8, scripts, job 모드)
2. **`ontology/**` YAML 정본** — ADR-0001 Source of Truth
3. **PDMG curated mappings 5건** (8888/9000/9001/5530/9999) — Seed 확장 원천
4. **ServiceId 11자 shape + Parser** — 검증·분해 공통
5. **scan / seed / validate 파이프라인** — 소스↔지식 동기화
6. **Ontology Core 1.0 Graph + Query/Impact/Rules** — 프롬프트 §7 A~E 상당 부분 **이미 충족**
7. **`technical/tx-runtime.yml`** — RUNTIME Graph 시드 원천 (아직 Graph 미적재)
8. **Prompt/Recommend API** — 향후 LLM Context 접점
9. **테스트 베이스** (GoldenGraph, Query ITs, Rule, Parser)

### 프롬프트 §7 조회 요구 vs AS-IS

| 요구 질의 | AS-IS API | 충족 |
|-----------|-----------|------|
| 1. ServiceId 전체 구조 | `GET .../query/service/{id}/structure` | ✅ |
| 2. Program → ServiceIds | `GET .../query/program/{id}/services` | ✅ |
| 3. Handler → ServiceIds | `GET .../query/handler/{name}/services` | ✅ |
| 4. Table → ServiceIds | `GET .../query/table/{table}/services` | ✅ |
| 5. ServiceId → Tables | `GET .../query/service/{id}/tables` | ✅ |
| 6. Business CO 트리 | `GET .../query/business/{code}/tree` | ✅ |
| 영향도 Table | `GET .../impact/table/{table}` | ✅ |
| 규칙검증 | `GET .../validate/rules` | ✅ |

---

## 4. 불필요하거나 잘못/부족한 설계

### 4.1 부족한 점 (프롬프트 목표 대비 Gap)

| Gap | 설명 |
|-----|------|
| G1. YAML→Graph 범용 로더 | ✅ 완료 (`YamlGraphLoader` + Bootstrap, curated mappings 전체 적재) |
| G2. RUNTIME Graph | ✅ 완료 (`TxRuntimeGraphLoader`, FLOWS_TO/DISPATCHES_TO/STARTS_TRANSACTION …) |
| G3. Message/Header/DTO | 전문 모델(`hdr_nhnis`, `rms_svc_c IDENTIFIES ServiceId`) 미구현 |
| G4. 확장 Concept | Screen, Policy, ArchitectureRule(as Concept), TestCase, Interface 등 미구현 |
| G5. Type vs Instance 문서화 | 코드상 ConceptType/Instance 구분됨. 메타모델 YAML과의 완전 정렬 필요 |
| G6. API 중복 | `/api/ontology/v1/**` vs `/query/**` vs 구 `impact?from=` — 정리 필요 |
| G7. RULE-003 실스캔 | Handler 소스 register와 Ontology 관계의 **근사 검증**만 (어노테이션/맵 실스캔 미완) |
| G8. H2 영속 | dependency만 있고 미사용 (1.0 범위 외로 타당) |
| G9. 01.디렉토리구조.md | `domain/`, `store/`, `query/` 미반영 — 문서 드리프트 |

### 4.2 잘못/위험할 수 있는 점

1. **이중 진실**: Registry(YAML Map)와 Store(Graph)가 동기화되지 않으면 조회 결과가 API별로 달라질 수 있음  
2. **시드 하드코딩**: `Mgcoa8888OntologySeed`가 YAML 값을 복제 — YAML 변경 시 시드 미갱신 위험  
3. **Impact 이중 구현**: `ImpactAnalyzer`(mapping) vs `OntologyQueryService.impactByTable`(graph) — 의미·응답 스키마 상이  
4. **역추적 fallback**: reverse path가 빈약할 때 forward reachability fallback 존재 — 경로(`paths`) 품질 편차 가능

### 4.3 “불필요”로 보이지 않는 것

- Neo4j/Jena **미도입**은 원칙(§6)에 부합 → 유지 권장  
- Recommend/Prompt는 “1차 AI 금지”와 충돌하지 않음 (규칙·템플릿 기반, LLM 아님)

---

## 5. 필요한 Ontology Concept 목록

### 5.1 1차 (Core — 대부분 구현됨)

| Concept | 비고 |
|---------|------|
| System | MG |
| Business | CO |
| Function | A |
| Program | mgcoa8888 |
| ServiceId | mgcoa8888S0 (분해 속성) |
| Component | Handler/Facade/Service/DAO (role) |
| Mapper | MyBatis XML |
| SqlId | select/delete id |
| Table / Column | TB_FW_IMAGE_LOG.GUID |

### 5.2 2차 (Architecture / Message — 미구현, 필요)

| Concept | 용도 |
|---------|------|
| Application / BusinessGroup | 상위 분류 확장 |
| RequestMessage / Header / HeaderField / DTO | 전문, `rms_svc_c` |
| Screen / ScreenEvent | UI |
| Database | RDW 등 |
| TimeoutPolicy / TransactionPolicy / SecurityPolicy | 정책 |
| ArchitectureRule | 규칙을 Concept화 (현재는 validator 코드) |
| TestCase | 회귀·시나리오 |
| Interface | 외부 연동 |
| RuntimeComponent | Filter, TcfFacade, TimeoutExecutor, Dispatcher … |

### 5.3 Type vs Instance (설계 원칙)

```text
ConceptType = SYSTEM          Instance = system:MG (name=MG)
ConceptType = SERVICE_ID      Instance = service:mgcoa8888S0
```

Type과 Instance를 동일 레벨로 취급하지 않는다. (프롬프트 §2 의도)

---

## 6. 필요한 Relation 목록

### 6.1 DESIGN (Canonical — 1.0 구현)

```text
HAS_BUSINESS / HAS_FUNCTION / HAS_PROGRAM
PROVIDES_SERVICE (alias: HAS_SERVICE)
BELONGS_TO_PROGRAM
HANDLED_BY / CALLS / USES / EXECUTES / ACCESSES / HAS_COLUMN
```

### 6.2 Message (2차)

```text
Message -HAS_HEADER→ Header
Message -HAS_BODY→ DTO
HeaderField -IDENTIFIES→ ServiceId   # rms_svc_c
```

### 6.3 RUNTIME (2차, TX 아키텍처)

```text
DISPATCHES_TO
STARTS_TRANSACTION
PARTICIPATES_IN_TRANSACTION
WAITS_ON / RUNS_ON_THREAD
```

**원칙**: DESIGN의 `CALLS`(Handler→Facade)와 RUNTIME의 `DISPATCHES_TO`(Dispatcher→Handler)를 혼용하지 않는다.  
`OntologyRelation.graphType = DESIGN | RUNTIME`으로 이미 구분 가능 — **시드만 부재**.

---

## 7. Java Domain Model 제안

### 7.1 AS-IS (유지·확장 권장)

```text
OntologyConcept
  id, type, name, description, attributes, version, status, provenance

OntologyRelation
  id, fromId, predicate, toId, graphType, attributes, version, status, provenance

ServiceIdParts (Value Object)
  groupCode, businessCode, functionCode, programNo, operationType, sequence, fullServiceId

Provenance
  sourceType, sourceSystem, sourcePath, ..., verificationStatus
```

### 7.2 제안 확장 (코드 수정 없이 설계만)

```text
MessageConcept extends attributes:
  envelopeType, headerRef, bodyDtoRef

RuntimeStepConcept:
  seq, thread, configKey, txBoundary

OntologyRule (Concept + executable):
  ruleId, severity, expressionRef
```

### 7.3 로딩 파이프라인 제안

```text
ontology/mappings/*.yml  ──┐
ontology/technical/*.yml ──┼─→ MappingGraphLoader → OntologyStore
ontology/core/*.yml      ──┘
         ↑
   (SoT 유지, Seed 하드코딩 축소)
```

---

## 8. 저장 방식 대안 비교

| 대안 | 장점 | 단점 | NSIGHT 적용성 |
|------|------|------|----------------|
| **A. YAML + In-memory Graph (현재)** | 단순, Git 리뷰, Boot만으로 충분, 배포 가벼움 | 프로세스 재시작 시 재적재, 대량 그래프 한계 | **1.0~1.5 추천** |
| **B. 현재 DB(H2/Oracle) 테이블** | 기존 스택 친화, 조인·백업 | Graph 질의(가변 depth) 불편, 스키마 경직 | 감사/스냅샷 보조로 적합 |
| **C. RDF Triple (Jena 등)** | 표준 추론·SPARQL | 학습비용, 운영 복잡도, 팀 스택 이질 | 중장기 검토 |
| **D. Property Graph (Neo4j 등)** | 경로·영향도 질의 자연스러움 | 외부제품 도입, 운영·라이선스 | **필요성 입증 후** |

### 추천안

1. **단기 (현재~1.5)**: A 유지 — YAML SoT + 기동 시 Graph 적재  
2. **중기**: B를 **스냅샷/감사 전용**으로 선택 도입 (조회 엔진은 여전히 메모리 Graph)  
3. **장기**: 그래프 규모·다중 시스템 통합 시 D 또는 C를 PoC 후 결정 — **지금 추가하지 않음**

---

## 9. REST API 목록

### 9.1 Ontology Core (권장 정본 API)

| Method | Path | 목적 |
|--------|------|------|
| GET | `/api/ontology/query/service/{serviceId}/structure` | Design 구조 |
| GET | `/api/ontology/query/program/{programId}/services` | Program→ServiceId |
| GET | `/api/ontology/query/handler/{handler}/services` | Handler→ServiceId |
| GET | `/api/ontology/query/table/{table}/services` | Table 역추적 |
| GET | `/api/ontology/query/service/{serviceId}/tables` | ServiceId→Table |
| GET | `/api/ontology/query/business/{businessCode}/tree` | Business 트리 |
| GET | `/api/ontology/impact/table/{tableName}` | 영향도 + paths |
| GET | `/api/ontology/validate/rules` | RULE-001~006 |

### 9.2 Knowledge Hub (기존, 유지)

| Method | Path | 목적 |
|--------|------|------|
| GET | `/api/ontology/catalog` | 프로그램 카탈로그 |
| GET | `/api/ontology/program/{id}` | 매핑 문서 |
| GET | `/api/ontology/service/{serviceId}` | 4축 조회 |
| GET | `/api/ontology/impact?from=` | 구 영향도 |
| GET | `/api/ontology/path` | 분류 path |
| GET/POST | `/api/ontology/recommend` | 패턴 추천 |
| GET | `/api/ontology/prompt/{id}` | LLM 전 Context |
| POST | `/api/ontology/import|seed|validate/pdmg` | 운영 파이프라인 |

### 9.3 호환/내부

| Method | Path |
|--------|------|
| GET | `/api/ontology/v1/meta\|snapshot\|concept\|service-id\|chain\|impact/table` |

### 9.4 2차 제안 (미구현)

```text
GET /api/ontology/query/runtime/tx-chain
GET /api/ontology/query/message/{serviceId}/envelope
GET /api/ontology/query/service/{id}/provenance
```

---

## 10. 패키지 구조 제안

### 10.1 AS-IS (대체로 양호)

```text
nhnis.ontology
  ├─ domain.concept / domain.relation / domain (Provenance)
  ├─ store
  ├─ seed
  ├─ query
  ├─ validate
  ├─ ontology      # YAML Registry (이름 충돌 주의: 패키지명 ontology)
  ├─ graph / impact / scan / prompt / recommend
  ├─ web / config / support / job
```

### 10.2 제안 (대규모 리패키징 없이 점진)

```text
nhnis.ontology
  ├─ domain/**          # 유지
  ├─ store/**           # 유지
  ├─ loader/            # NEW: YamlGraphLoader (seed 하드코딩 대체)
  ├─ query/**           # Core query (정본)
  ├─ hub/               # RENAME 후보: 현재 ontology+graph+impact(구)
  ├─ validate/**
  ├─ ops/               # scan, seed-generator, job
  └─ web/**
```

**원칙**: 대규모 리패키징 금지. `loader` 추가와 API 문서에서 Core vs Hub 구분만으로도 충분.

---

## 11. 1차 구현 WBS

> 상태: §7 A~E의 **최소 Core는 구현 완료**. 아래는 **잔여 + 1.5** WBS.

| ID | 작업 | 상태 | 산출물 |
|----|------|------|--------|
| W0 | 분석 (본 문서) | ✅ | `ONTOLOGY-ANALYSIS.md` |
| W1 | Concept/Relation/ServiceIdParser | ✅ | domain + tests |
| W2 | Predicate vocabulary 정렬 | ✅ | relations.yml + enum |
| W3 | mgcoa8888 Golden Graph | ✅ | Seed + GoldenGraphTest |
| W4 | Query API §7.C | ✅ | OntologyCoreQueryController |
| W5 | Impact API §7.D | ✅ | impact/table + paths |
| W6 | Architecture Rules §7.E | ✅ | RULE-001~006 |
| **W7** | **YAML→Graph 범용 로더** (전 mappings) | ✅ | `YamlGraphLoader`, `OntologyGraphBootstrap` |
| **W8** | **RUNTIME Graph 시드** (tx-runtime.yml) | ✅ | `TxRuntimeGraphLoader`, `/query/runtime/tx-chain` |
| **W9** | **Message/Header/rms_svc_c** | ⬜ | IDENTIFIES 관계 |
| **W10** | Registry↔Store 단일 조회 facade | ✅ | `OntologyFacade` + `/consistency` |
| **W11** | RULE-003 Handler 소스 실스캔 | ⬜ | scanner 연동 |
| **W12** | API/문서 정리 (v1 deprecate 계획) | ⬜ | docs/api |

### 우선순위 권고

```text
W11 → W9 → W12
```

(W7/W8/W10 완료: 2026-08-10)
---

## 12. 테스트 전략

### 12.1 AS-IS (유지)

| 계층 | 테스트 |
|------|--------|
| Unit | `ServiceIdParserTest`, `Mgcoa8888GoldenGraphTest`, `ArchitectureRuleValidationTest`, `ReverseImpactUnitTest` |
| API IT | structure/program/handler/table/business/impact QueryTests |
| Hub IT | `OntologyControllerTest`, `ImpactAndPromptIT`, `PdmgValidationIT`, `MappingSeedGeneratorIT` |

### 12.2 추가 권고

1. **Contract Test**: 프롬프트 질문 1~7을 Given/When/Then 고정 스펙으로  
2. **Loader Test**: YAML 한 건 변경 → Graph edge 반영  
3. **Drift Test**: curated mapping vs Graph vs 소스 스캔 삼각 검증  
4. **Negative**: 잘못된 ServiceId → 400/검증 FAIL  
5. **RUNTIME**: tx-chain 시드 후 path 단언 (W8 이후)

### 12.3 완료 게이트

```text
gradlew.bat test   # GREEN 필수
Golden Graph assertions 유지
신규 Loader 도입 시 mgcoa8888 회귀 불변
```

---

## 13. 향후 Graph RAG / LLM 연계 위치

### 13.1 넣지 말아야 할 곳 (지금)

- Ontology Core 저장 엔진 자체에 LLM 추론을 섞지 않음  
- Concept/Relation 정본을 모델이 “추측”으로 채우지 않음  

### 13.2 넣을 위치 (향후)

```text
[Ontology Store / Query API]
        ↓ 구조화 Context
[PromptContextExporter]  ← 이미 존재 (확장점)
        ↓
[Harness / LLM]  신규 시스템 Baseline·코드 생성
        ↑
[RecommendService] ← 규칙·유사도 추천 (비-LLM) 유지
```

### 13.3 Graph RAG 패턴 (장기)

1. 질의 → Ontology Query로 **정확한 부분 그래프** 회수 (structure/impact)  
2. Provenance를 근거 인용으로 첨부 (“왜 이 패턴인가”)  
3. 회수 그래프만 LLM에 전달 (환각 억제)  
4. Vector DB는 **문서/ADR 보조 검색**에만 선택 도입 — Graph 정본을 대체하지 않음  

### 13.4 신규 시스템 구축 플로우 (프롬프트 §8)

```text
요구사항 → Ontology 검색 → 유사업무/Pattern
  → ServiceId·프로그램 패턴 → Architecture Baseline
  → PromptExporter Context → LLM/Harness
```

현재 설계(Concept+Relation+Provenance+Query)는 이 플로우의 **왼쪽 절반**을 담당한다.

---

## 종합 판정

| 관점 | 판정 |
|------|------|
| 서비스 목적 부합 | ✅ Knowledge Hub (CRUD 아님) |
| 프롬프트 §7 1차 범위 | ✅ **Core 충족** (mgcoa8888 E2E) |
| 저장 전략 | ✅ YAML+Memory 유지 권장, Neo4j/Jena 보류 |
| 최대 리스크 | Message/`rms_svc_c`·RULE-003 실스캔 미완 |
| 다음 한 일 | **W11 RULE-003 Handler 소스 실스캔**, 이어서 **W9 Message 모델** |

### 분석 결론 (한 줄)

> `tcf-ontology-service`는 Ontology Core 1.0으로 **mgcoa8888 End-to-End 질의·역추적·규칙검증**이 가능하며,  
> 다음 과제는 기능 추가보다 **YAML SoT → Graph 자동 적재와 Hub/Core 일원화**, 이어서 **RUNTIME·Message 모델** 확장이다.

---

*본 문서는 분석 산출물이다. 구현 변경은 별도 구현지시/승인 후 진행한다.*
