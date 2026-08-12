# NSIGHT Architect Workbench 화면 설계서
## tcf-ontology-service 기반 아키텍트 업무 지원 UI

- 문서버전: v1.0
- 작성일: 2026-08-10
- 대상 시스템: `tcf-ontology-service`
- 문서 목적: Cursor가 Architect Workbench 화면을 설계·구현할 수 있도록 요구사항, 화면구조, API 연계, 검증기준을 정의
- 구현 원칙: **Ontology 관리 UI가 아니라 아키텍트 업무 수행 UI**
- 1차 구현 범위: Architect Home / Architecture Search / Impact Analysis / Architecture Gate
- 2차 확장 범위: Architecture Design Assistant / Architecture Decision / Knowledge Graph / Standards & Evidence

---

# 1. 도입 전 안내말

`tcf-ontology-service`는 일반 업무 CRUD 서비스가 아니라 NSIGHT/PDMG의 업무·아키텍처·프로그램·데이터·표준·근거 정보를 구조화하여 조회하고 검증하는 Architecture Knowledge Service이다.

따라서 화면의 목적도 Concept/Relation을 단순 CRUD하는 관리도구가 되어서는 안 된다.

사용자가 화면에서 수행해야 하는 핵심 행위는 다음 네 가지이다.

```text
찾는다
  ↓
분석한다
  ↓
판단한다
  ↓
검증한다
```

본 설계서는 이를 실제 업무 화면으로 구현하기 위한 기준을 정의한다.

---

# 2. 문서 개요

## 2.1 목적

다음 업무를 하나의 화면 체계에서 지원한다.

1. ServiceId, Program, Handler, Table 등 아키텍처 정보 통합 검색
2. ServiceId 기준 End-to-End 처리구조 조회
3. Table 등 변경 대상 기준 영향도 분석
4. Architecture Rule 기반 개발표준 검증
5. Ontology 정보의 출처(Provenance) 확인
6. 향후 신규 시스템 설계 추천과 ADR 등록으로 확장
7. Cursor/DAVIS CODER/LLM에 Architecture Context 제공

## 2.2 적용범위

### 1차 범위

- Architect Home
- Architecture Search
- Impact Analysis
- Architecture Gate

### 2차 범위

- Architecture Design Assistant
- Architecture Decision / ADR
- Knowledge Graph Explorer
- Standards & Evidence
- Cursor/LLM 연계

## 2.3 대상 독자

- Application Architect
- Solution Architect
- Framework Architect
- 개발 PL
- 개발자
- DBA
- 운영 아키텍트
- Cursor/DAVIS CODER를 이용하는 AI 기반 개발 담당자

## 2.4 선행조건

화면 구현 전에 최소 다음 Ontology API가 정상 동작해야 한다.

```text
GET  /api/ontology/catalog
GET  /api/ontology/query/service/{serviceId}/structure
GET  /api/ontology/query/program/{programId}/services
GET  /api/ontology/query/handler/{handler}/services
GET  /api/ontology/query/table/{table}/services
GET  /api/ontology/query/service/{serviceId}/tables
GET  /api/ontology/query/business/{businessCode}/tree
GET  /api/ontology/impact/table/{tableName}
POST /api/ontology/validate/rules
GET  /api/ontology/v1/concept/{id}
GET  /api/ontology/runtime/tx-chain
GET  /api/ontology/recommend
GET  /api/ontology/prompt/{id}
```

현재 Ontology Core의 주요 모델은 다음을 포함한다.

```text
System
Business
Function
Program
ServiceId
Component
Mapper
SqlId
Table
Column
Relation
Provenance
ArchitectureRule
```

## 2.5 용어 정의

| 용어 | 정의 |
|---|---|
| Architect Workbench | Ontology를 이용하여 아키텍트 업무를 수행하는 화면 |
| Ontology | NSIGHT/PDMG의 개념과 관계를 기계가 해석할 수 있게 구조화한 지식 |
| Concept | System, Program, ServiceId, Table 등의 개념/인스턴스 |
| Relation | HANDLED_BY, CALLS, USES, ACCESSES 등의 관계 |
| Provenance | 관계와 정보가 어디에서 발견·검증되었는지를 나타내는 근거 |
| Impact Analysis | 특정 변경대상으로부터 영향을 받는 상위/하위 구성요소를 탐색하는 기능 |
| Architecture Gate | Architecture Rule을 이용한 표준 준수 검증 |
| Architecture Pattern | 신규 시스템 설계 시 재사용할 수 있는 검증된 구조 패턴 |
| ADR | Architecture Decision Record |

---

# 3. 문제 정의 및 설계 배경

현재 아키텍처 지식은 다음과 같이 여러 곳에 존재한다.

```text
Markdown
Java Source
Mapper XML
YAML Mapping
Architecture Rule
운영설정
개발가이드
```

기존 방식은 아키텍트가 각각의 자료를 직접 찾아 관계를 해석해야 한다.

```text
질문
 ↓
문서 검색
 ↓
소스 검색
 ↓
Mapper 검색
 ↓
ServiceId 확인
 ↓
구조 해석
 ↓
영향도 판단
```

Architect Workbench의 목표는 다음과 같이 변경하는 것이다.

```text
질문 / 변경대상 / 검증대상
            ↓
      Architect Workbench
            ↓
     tcf-ontology-service
            ↓
   구조 / 영향 / 규칙 / 근거
            ↓
       아키텍트 판단
```

---

# 4. 현행 구조와 문제점

## 4.1 현행

`tcf-ontology-service`는 이미 다음 기능을 제공하는 프로토타입 Knowledge Hub 형태이다.

- YAML 기반 Ontology 적재
- Concept / Relation Graph
- ServiceId 11자리 파싱
- 정방향 구조 조회
- Table 영향도 조회
- Architecture Rule 검증
- Provenance
- 소스 Scan / Seed
- Recommend
- Prompt Context Export

## 4.2 문제점

현재는 대부분 REST API 또는 내부 기능 중심이므로 아키텍트가 일상 업무에서 사용하기 어렵다.

| 문제 | 설명 |
|---|---|
| API 중심 | 사용자가 URL을 알아야 한다 |
| 업무흐름 부재 | 검색/영향/검증이 서로 다른 API로 분리됨 |
| 관계 가시성 부족 | Graph 관계를 한눈에 보기 어렵다 |
| 근거 확인 불편 | Provenance가 응답 데이터에 있으나 업무화면으로 노출되지 않음 |
| 신규 설계 연결 부족 | Recommend/Prompt가 있으나 아키텍트 업무 시나리오로 연결되지 않음 |

---

# 5. 요구사항과 제약조건

## 5.1 기능 요구사항

### FR-001 통합 검색

사용자는 다음 키워드로 검색할 수 있어야 한다.

- ServiceId
- Program
- Handler
- Facade
- Service
- DAO
- Mapper
- SQL ID
- Table
- Column
- Business
- Function

### FR-002 구조 조회

ServiceId 검색 시 다음 전체 구조를 보여야 한다.

```text
System
→ Business
→ Function
→ Program
→ ServiceId
→ Handler
→ Facade
→ Service
→ DAO
→ Mapper/SQL
→ Table
→ Column
```

### FR-003 영향도 분석

Table 등 변경 대상에서 역방향으로 영향 요소를 탐색해야 한다.

```text
Table
← Mapper/SQL
← DAO
← Service
← Facade
← Handler
← ServiceId
← Program
← Function
← Business
← System
```

### FR-004 Architecture Rule 검증

검증 대상에 대해 Rule PASS/FAIL/WARNING을 보여야 한다.

### FR-005 Provenance

모든 주요 관계에서 다음 정보를 확인할 수 있어야 한다.

- sourceType
- sourcePath
- discoveredBy
- verificationStatus
- extractedAt
- verifiedAt

### FR-006 Graph Drill-down

Graph Node를 클릭하면 상세 속성과 연결 관계를 조회할 수 있어야 한다.

### FR-007 Evidence 기반 판단

AI/추천 결과를 표시할 경우 반드시 근거가 되는 기존 Program, ServiceId, Rule, Source를 함께 보여야 한다.

## 5.2 비기능 요구사항

| 항목 | 요구사항 |
|---|---|
| 가독성 | 아키텍처 경로를 2~3초 내 파악할 수 있어야 함 |
| 성능 | 일반 Query 화면 p95 3초 이내 목표 |
| 확장성 | 향후 Neo4j/RDF 도입 여부와 무관하게 UI 계약 유지 |
| 호환성 | 기존 `/api/ontology/**` API를 우선 재사용 |
| 추적성 | 화면 결과에서 원천 Source까지 Drill-down 가능 |
| 보안 | Source Path나 내부정보의 노출수준을 권한별 통제 |
| 감사 | Architecture Decision, Gate 실행 이력 향후 감사로그 연계 |

---

# 6. 설계 원칙

1. **Ontology 관리 중심이 아니라 Architect 업무 중심**
2. Graph는 목적이 아니라 판단을 위한 시각화 수단
3. 모든 추천은 근거를 표시
4. 검색 → 구조 → 영향 → 검증으로 자연스럽게 이동
5. 최초 화면에서 복잡한 Ontology 용어를 강요하지 않음
6. AI 답변과 Ontology 사실 데이터를 시각적으로 구분
7. AS-IS / RECOMMENDED / PROPOSED / DEPRECATED 상태를 구분 가능하게 설계
8. 한 화면에서 너무 많은 Graph Node를 동시에 보여주지 않음
9. 실패한 API 응답을 성공 데이터처럼 표시하지 않음
10. Source of Truth는 `tcf-ontology-service`이고 UI 자체가 별도 지식을 임의 생성하지 않음

---

# 7. 대안 비교 및 의사결정

| 대안 | 설명 | 판단 |
|---|---|---|
| Ontology CRUD Admin | Concept/Relation 직접 등록/수정 중심 | 관리자 기능으로만 제한 |
| Graph Explorer 중심 | 모든 관계를 Graph로 탐색 | 보조 기능 |
| Architect Workbench | 검색·영향·검증·판단 중심 | **채택** |
| AI Chat 중심 | 대화창만 제공 | 2차 보조기능 |

최종 결정:

> **1차 UI는 Architect Workbench 방식으로 구현한다.**

---

# 8. 목표 아키텍처

```text
┌───────────────────────────────────────────────────────────────┐
│                 NSIGHT Architect Workbench                    │
├───────────────────────────────────────────────────────────────┤
│ Architect Home                                                │
│ Architecture Search                                           │
│ Impact Analysis                                               │
│ Architecture Gate                                             │
│ Architecture Design       (2차)                               │
│ Architecture Decision     (2차)                               │
│ Knowledge Graph           (2차)                               │
│ Standards & Evidence      (2차)                               │
└───────────────────────────┬───────────────────────────────────┘
                            │ REST
                            ▼
┌───────────────────────────────────────────────────────────────┐
│                    tcf-ontology-service                       │
├───────────────────────────────────────────────────────────────┤
│ Query                                                         │
│ Impact                                                        │
│ Validation                                                    │
│ Recommend                                                     │
│ Prompt Context                                                │
│ Provenance                                                    │
│ Graph Store                                                   │
└───────────────────────────────────────────────────────────────┘
```

---

# 9. 메뉴 구조

## 9.1 1차 메뉴

```text
NSIGHT Architect Workbench
├─ 01. Architect Home
├─ 02. Architecture Search
├─ 03. Impact Analysis
└─ 04. Architecture Gate
```

## 9.2 2차 메뉴

```text
├─ 05. Architecture Design
├─ 06. Architecture Decision
├─ 07. Knowledge Graph
└─ 08. Standards & Evidence
```

---

# 10. 화면 공통 표준

## 10.1 공통 Header

```text
┌──────────────────────────────────────────────────────────────┐
│ NSIGHT Architect Workbench                    [환경] [사용자] │
├──────────────────────────────────────────────────────────────┤
│ [통합검색                                      ] [Search]    │
└──────────────────────────────────────────────────────────────┘
```

## 10.2 공통 상태

- VERIFIED
- DISCOVERED
- APPROVED
- DEPRECATED
- ERROR
- WARNING

## 10.3 Provenance 표시

모든 주요 Node/Relation 상세에 Evidence 버튼 제공.

```text
[Evidence]

Source Type      YAML_MAPPING
Source Path      ontology/mappings/mgcoa8888.yml
Discovered By    YamlGraphLoader
Status           VERIFIED
```

---

# 11. 화면 01 — Architect Home

## 11.1 목적

현재 Ontology 상태와 아키텍처 품질을 한눈에 확인하고 다른 기능으로 진입한다.

## 11.2 화면 ID

`ARC-HOME-0001`

## 11.3 Wireframe

```text
┌─────────────────────────────────────────────────────────────────────┐
│ NSIGHT Architecture Intelligence                                   │
├─────────────────────────────────────────────────────────────────────┤
│ [ ServiceId / Program / Table / 업무를 검색하세요               ] │
├─────────────────────────────────────────────────────────────────────┤
│ Systems      Programs      ServiceIds      Relations      Rules     │
│   1             5             12             149           6       │
├──────────────────────────────────┬──────────────────────────────────┤
│ Architecture Health              │ Quick Actions                    │
│                                  │                                  │
│ Rule PASS              xx        │ [Service 구조 조회]             │
│ Rule FAIL              xx        │ [Table 영향도]                  │
│ Provenance VERIFIED    xx        │ [Architecture Gate]             │
│ Unverified             xx        │ [신규 설계] (2차)               │
├──────────────────────────────────┴──────────────────────────────────┤
│ Recently Viewed / Recent Architecture Findings                     │
│                                                                     │
└─────────────────────────────────────────────────────────────────────┘
```

## 11.4 주요 API

- `GET /api/ontology/catalog`
- `GET /api/ontology/consistency`
- `POST /api/ontology/validate/rules`

---

# 12. 화면 02 — Architecture Search

## 12.1 목적

ServiceId, Program, Table 등으로 아키텍처 구조를 검색한다.

## 12.2 화면 ID

`ARC-SRCH-0001`

## 12.3 검색 조건

```text
검색유형 [AUTO ▼]
검색어   [mgcoa8888S0                              ]
                                                [조회]
```

## 12.4 ServiceId 결과 화면

```text
┌───────────────────────────────────────────────────────────────┐
│ ServiceId : mgcoa8888S0                        VERIFIED       │
├───────────────────────────────────────────────────────────────┤
│ 업무분류                                                       │
│ MG > CO > A > mgcoa8888 > mgcoa8888S0                         │
├───────────────────────────────────────────────────────────────┤
│ 처리 구조                                                       │
│ Handler → Facade → Service → DAO → Mapper/SQL → Table        │
├───────────────────────────────────────────────────────────────┤
│ [Graph 보기] [Table 보기] [영향도] [Evidence]                 │
└───────────────────────────────────────────────────────────────┘
```

## 12.5 주요 API

- `GET /api/ontology/query/service/{serviceId}/structure`
- `GET /api/ontology/query/program/{programId}/services`
- `GET /api/ontology/query/handler/{handler}/services`
- `GET /api/ontology/query/service/{serviceId}/tables`
- `GET /api/ontology/v1/concept/{id}`

## 12.6 상세 Tabs

```text
[Overview] [Structure] [Tables] [Runtime] [Rules] [Evidence]
```

---

# 13. 화면 03 — Impact Analysis

## 13.1 목적

Table, ServiceId, Program 등의 변경 시 영향 범위를 분석한다.

## 13.2 화면 ID

`ARC-IMPT-0001`

## 13.3 입력

```text
변경대상유형 [TABLE ▼]
변경대상     [TB_FW_IMAGE_LOG                       ]
                                                       [분석]
```

## 13.4 결과 Wireframe

```text
┌───────────────────────────────────────────────────────────────┐
│ Impact Analysis : TB_FW_IMAGE_LOG                             │
├───────────────────────────────────────────────────────────────┤
│ Mapper 1 | DAO 1 | Service 1 | Handler 1 | ServiceId 2       │
├───────────────────────────────────────────────────────────────┤
│                      TB_FW_IMAGE_LOG                           │
│                              ↑                                │
│                      mgcoa8888-ORA.xml                        │
│                              ↑                                │
│                        mgcoa8888DAO                           │
│                              ↑                                │
│                      mgcoa8888Service                         │
│                              ↑                                │
│                      mgcoa8888Facade                          │
│                              ↑                                │
│                      mgcoa8888Handler                         │
│                       ↑            ↑                          │
│                mgcoa8888S0    mgcoa8888D0                    │
│                       └───────┬───────┘                       │
│                            mgcoa8888                           │
│                               ↑                               │
│                               A                               │
│                               ↑                               │
│                              CO                               │
│                               ↑                               │
│                              MG                               │
├───────────────────────────────────────────────────────────────┤
│ [Path] [Affected List] [Evidence] [Export]                    │
└───────────────────────────────────────────────────────────────┘
```

## 13.5 주요 API

- `GET /api/ontology/impact/table/{tableName}`

## 13.6 결과 필드

- table
- affectedMappers
- affectedSqlIds
- affectedDaos
- affectedServices
- affectedFacades
- affectedHandlers
- affectedServiceIds
- affectedPrograms
- affectedFunctions
- affectedBusinesses
- affectedSystems
- paths

## 13.7 중요 검증

`table.type`은 반드시 `TABLE`이어야 한다.
잘못된 alias로 `COLUMN` 등이 선택되면 UI는 데이터 오류로 표시하고 정상 결과처럼 보여주지 않는다.

---

# 14. 화면 04 — Architecture Gate

## 14.1 목적

신규 또는 기존 프로그램이 NSIGHT Architecture Rule을 준수하는지 검증한다.

## 14.2 화면 ID

`ARC-GATE-0001`

## 14.3 Wireframe

```text
┌───────────────────────────────────────────────────────────────┐
│ Architecture Gate                                            │
├───────────────────────────────────────────────────────────────┤
│ 검증대상 [mgcoa8888S0                              ] [검증]  │
├───────────────────────────────────────────────────────────────┤
│ RULE-001   ServiceId 11자리                 PASS              │
│ RULE-002   ServiceId → Handler              PASS              │
│ RULE-003   Handler ServiceId 등록           PASS              │
│ RULE-004   Program → ServiceId              PASS              │
│ RULE-005   Service → DAO/Client             PASS              │
│ RULE-006   DAO → Mapper/SQL                 PASS              │
├───────────────────────────────────────────────────────────────┤
│ Score : 100 / 100                                             │
│ [Evidence] [Failure Detail] [다시 검증]                       │
└───────────────────────────────────────────────────────────────┘
```

## 14.4 Rule

| Rule | 설명 |
|---|---|
| RULE-001 | ServiceId 형식 |
| RULE-002 | ServiceId → Handler |
| RULE-003 | Handler 등록관계 |
| RULE-004 | Program → ServiceId |
| RULE-005 | Service → DAO 또는 Client |
| RULE-006 | DAO → Mapper/SQL |

## 14.5 주요 API

- `POST /api/ontology/validate/rules`

---

# 15. 화면 05 — Architecture Design Assistant (2차)

## 15.1 목적

신규 거래 또는 신규 시스템 구축 시 기존 Ontology를 이용해 재사용 가능한 Architecture Pattern을 추천한다.

## 15.2 화면 ID

`ARC-DESN-0001`

## 15.3 입력 예시

```text
업무영역       [고객관리                ]
거래유형       [조회 ▼                 ]
채널           [WEB ▼                  ]
DB 사용        [YES                    ]
대용량조회     [YES                    ]
외부연계       [NO                     ]

                                  [표준 아키텍처 추천]
```

## 15.4 출력 예시

```text
추천 Pattern
ONLINE_QUERY_STANDARD

유사 구현
1. mgcoa5530S0
2. mgcoa8888S0

추천 계층
Handler → Facade → Service → DAO → Mapper

전문
hdr_nhnis + dto

ServiceId
11자리

Transaction
TimeoutExecutor 외곽 TX

[설계안 생성] [Cursor Context 생성] [ADR 생성]
```

## 15.5 원칙

추천은 반드시 다음 Evidence를 포함한다.

- 유사 ServiceId
- 유사 Program
- 적용 Rule
- Source Path
- Provenance
- 추천 근거

---

# 16. 화면 06 — Architecture Decision / ADR (2차)

## 16.1 목적

아키텍처 의사결정과 근거를 Ontology Knowledge로 축적한다.

## 16.2 화면 ID

`ARC-ADR-0001`

## 16.3 예시

```text
대상
mgcoa7777S0

의사결정 주제
대용량 고객조회 Paging 방식

대안
1. Offset Paging
2. Keyset Paging

Ontology Evidence
- mgcoa5530S0 Offset Paging
- Max pageSize 100
- Deep Offset Risk 존재

결정
[Keyset Paging]

결정사유
[................................................]

[ADR 저장]
```

---

# 17. 화면 07 — Knowledge Graph (2차)

## 17.1 목적

아키텍처 관계를 탐색하는 전문가용 보조화면.

## 17.2 원칙

- 초기 Node 1개
- 1~2 Hop 우선 표시
- 사용자가 Expand
- DESIGN / RUNTIME Graph 구분
- Node Type Filter 제공

---

# 18. 화면 08 — Standards & Evidence (2차)

## 18.1 목적

Architecture Rule, Naming, ServiceId, Transaction, Message 등 기준과 근거를 조회한다.

```text
Standards
├─ ServiceId
├─ Package
├─ Transaction
├─ Message
├─ Paging
├─ Component
└─ Architecture Rules
```

---

# 19. 구성요소 및 속성

| Component | 역할 |
|---|---|
| GlobalSearch | 통합 검색 |
| ArchitectureSummaryCard | 구조 요약 |
| ArchitecturePath | 관계 경로 |
| GraphViewer | 그래프 표시 |
| ImpactSummary | 영향도 집계 |
| RuleResultGrid | Architecture Gate 결과 |
| EvidencePanel | Provenance |
| PatternRecommendation | 추천 Pattern |
| AdrEditor | Architecture Decision |

---

# 20. 책임 경계와 RACI

| 업무 | Architect | UI | tcf-ontology-service | Cursor/LLM |
|---|---|---|---|---|
| 검색 조건 입력 | R | A | C | - |
| Ontology 조회 | C | R | A | - |
| 관계 계산 | - | C | A/R | - |
| 영향도 계산 | C | C | A/R | - |
| Rule 판단 | C | C | A/R | - |
| Evidence 제공 | - | C | A/R | - |
| 최종 아키텍처 판단 | A/R | C | C | C |
| 코드 생성 | C | - | C | A/R |
| ADR 승인 | A/R | C | C | C |

---

# 21. 정상 처리 흐름

## 21.1 ServiceId 조회

```text
Architect
→ Architecture Search
→ ServiceId 입력
→ Query API
→ Ontology Graph 탐색
→ Classification + Structure + Evidence 반환
→ UI 표시
```

## 21.2 Impact

```text
Architect
→ Table 입력
→ Impact API
→ Reverse Graph Traverse
→ affected* + paths 반환
→ 영향도 Graph 표시
→ Evidence 확인
```

## 21.3 Gate

```text
Architect
→ 검증대상 선택
→ Rule Validation API
→ RuleEngine
→ PASS/FAIL/Evidence
→ UI 표시
```

---

# 22. 오류·Timeout·장애 흐름

## 22.1 API Timeout

```text
UI
→ API
→ Timeout

UI 메시지:
"Ontology 조회 시간이 초과되었습니다. 조회 조건을 줄이거나 다시 시도하십시오."
```

## 22.2 Concept 미존재

```text
"등록된 Ontology 정보를 찾을 수 없습니다."

가능 원인:
- Seed 미수행
- Source Scan 미수행
- ServiceId 오입력
- 아직 Ontology에 등록되지 않은 신규 프로그램
```

## 22.3 Graph 불일치

```text
"관계 정보가 불완전합니다."
예: ServiceId는 존재하지만 Handler 관계 없음
```

이 경우 Architecture Gate 이동 링크를 제공한다.

---

# 23. 정상 예시

검색:

```text
mgcoa8888S0
```

결과:

```text
MG
→ CO
→ A
→ mgcoa8888
→ mgcoa8888S0
→ mgcoa8888Handler
→ mgcoa8888Facade
→ mgcoa8888Service
→ mgcoa8888DAO
→ mgcoa8888-ORA.xml
→ TB_FW_IMAGE_LOG
```

---

# 24. 금지 예시

## 24.1 Ontology 관리도구로만 구현

```text
[Concept 등록]
[Relation 등록]
[Predicate 삭제]
```

이 화면을 주 화면으로 만들지 않는다.

## 24.2 AI가 근거 없이 구조 생성

금지:

```text
"아마 이 ServiceId는 Customer 테이블을 사용할 것입니다."
```

허용:

```text
"Ontology 관계상 TB_CUSTOMER를 사용합니다.
Source: xxxMapper.xml
Status: VERIFIED"
```

---

# 25. 연계 규칙

## 25.1 Cursor 연계

2차 이후 다음 Context API 연계를 고려한다.

```text
Architect Workbench
→ Prompt Context Export
→ Cursor
```

Cursor에 전달할 내용:

- Architecture Pattern
- ServiceId Rule
- 계층구조
- 유사 Program
- Table
- Message
- Transaction
- Evidence
- Architecture Gate Rule

---

# 26. 데이터 및 상태관리

UI는 Ontology 데이터를 별도 정본으로 저장하지 않는다.

```text
Source of Truth = tcf-ontology-service
```

UI Local State:

- 검색조건
- 선택 Node
- Graph 펼침상태
- Filter
- 최근조회

Architecture Decision/ADR만 별도 저장기능을 2차에서 정의한다.

---

# 27. 성능·용량·확장성

- 초기 Graph는 최대 1~2 Hop만 표시
- 전체 Graph 자동 렌더링 금지
- Path 수가 많은 경우 상위 N개 + 더보기
- Impact 결과는 Summary 우선
- 대규모 결과는 서버 Paging 또는 제한 적용
- 일반 조회 p95 3초 목표

---

# 28. 보안·개인정보·감사

1. Source Path 노출은 권한기반 통제 가능해야 함
2. 실제 업무데이터 값은 Ontology에 적재하지 않음
3. 개인정보 Column은 Metadata만 취급
4. ADR 변경은 사용자/시각/변경내용 감사이력 대상
5. Architecture Gate 실행 이력은 향후 감사로그 연계 가능하게 설계

---

# 29. 운영·모니터링·장애 대응

Home 화면에 최소 다음 상태 표시를 고려한다.

- Ontology Health
- YAML↔Graph Consistency
- Concept Count
- Relation Count
- Program Count
- ServiceId Count
- Rule Fail Count
- Unverified Provenance Count

---

# 30. 자동검증 및 품질 Gate

UI 개발 완료 전 다음을 자동검증한다.

- 주요 API Contract 테스트
- 404/500/Timeout 처리
- Graph 빈 결과 처리
- Evidence 누락 처리
- Impact Table Type 검증
- Rule PASS/FAIL 표시
- 화면 ID 및 Route 일관성
- Component 중복 방지

---

# 31. 테스트 시나리오

| TC | 시나리오 | 기대결과 |
|---|---|---|
| TC-001 | mgcoa8888S0 검색 | 전체 구조 표시 |
| TC-002 | mgcoa8888 Program 검색 | ServiceId 목록 표시 |
| TC-003 | TB_FW_IMAGE_LOG Impact | ServiceId/Program/Business 영향 표시 |
| TC-004 | 잘못된 ServiceId 검색 | 오류 안내 |
| TC-005 | Handler 없는 ServiceId Gate | RULE-002 FAIL |
| TC-006 | Evidence 클릭 | Source Path/Status 표시 |
| TC-007 | API Timeout | Timeout 메시지 |
| TC-008 | Graph Node 클릭 | 상세 Panel 표시 |
| TC-009 | Table alias 충돌 | TABLE만 정상 선택 |
| TC-010 | 미등록 신규 프로그램 | 정보 없음 + Seed 안내 |

---

# 32. 체크리스트

## 32.1 1차 완료조건

- [ ] Architect Home 구현
- [ ] Architecture Search 구현
- [ ] Impact Analysis 구현
- [ ] Architecture Gate 구현
- [ ] Evidence Panel 구현
- [ ] 기존 Ontology API 재사용
- [ ] ServiceId 구조 조회 성공
- [ ] Table 역추적 성공
- [ ] Rule 검증 성공
- [ ] 오류/Timeout 처리
- [ ] 실제 `mgcoa8888S0` Golden Scenario 통과

---

# 33. 변경·호환성·폐기 관리

- 기존 Ontology REST API를 삭제하지 않는다.
- UI 편의를 이유로 Backend 모델을 임의 변경하지 않는다.
- `/v1`과 `/query` 중복 API는 Backend 표준화 결정 전까지 호환 유지한다.
- 2차 AI 기능 추가 시 1차 Search/Impact/Gate 화면은 독립적으로 계속 사용 가능해야 한다.

---

# 34. 시사점

## 34.1 핵심 아키텍처 판단

Architect Workbench의 핵심은 Graph 시각화 자체가 아니다.

```text
Search
→ Understand
→ Impact
→ Validate
→ Decide
```

## 34.2 주요 위험

- 화면이 Ontology CRUD 관리도구로 변질될 위험
- Graph를 과도하게 시각화하여 가독성이 떨어질 위험
- AI 추천이 Provenance 없이 제시될 위험
- Ontology에 없는 정보를 UI가 임의 보완할 위험

## 34.3 우선 보완 과제

1. Ontology Core 1.0 Impact 최종 안정화
2. Architect Home
3. Search
4. Impact
5. Gate
6. Evidence
7. 이후 Design Assistant

## 34.4 중장기 발전 방향

```text
Ontology
   ↓
Architect Workbench
   ↓
Architecture Pattern
   ↓
Design Assistant
   ↓
Cursor / DAVIS CODER
   ↓
자동 Harness
   ↓
Architecture Gate
   ↓
Knowledge Feedback
```

---

# 35. Cursor 구현 지시

## Phase 0 — 분석

먼저 현재 UI 기술스택과 기존 화면 구조를 분석한다.

- React/Vue/Thymeleaf 여부
- Router
- API Client
- 공통 Layout
- 공통 Table
- Graph Library 존재 여부
- 인증/권한 처리

기존 프로젝트 기술스택을 우선 사용하고 새로운 UI Framework를 임의 도입하지 않는다.

분석 결과를 `ARCHITECT-WORKBENCH-UI-ANALYSIS.md`로 작성한다.
아직 대규모 코드를 생성하지 않는다.

## Phase 1 — 1차 화면

다음 4개 화면만 구현한다.

1. Architect Home
2. Architecture Search
3. Impact Analysis
4. Architecture Gate

공통:

- Header
- Side Menu
- Global Search
- Evidence Drawer

## Phase 2 — API 연결

Mock Data가 아니라 실제 `tcf-ontology-service` API를 연결한다.

Golden Test 대상:

```text
mgcoa8888S0
TB_FW_IMAGE_LOG
```

## Phase 3 — 검증

### Scenario A

```text
mgcoa8888S0 검색
→ Classification 표시
→ Handler
→ Facade
→ Service
→ DAO
→ Mapper
→ Table
→ Evidence
```

### Scenario B

```text
TB_FW_IMAGE_LOG Impact
→ Mapper
→ DAO
→ Service
→ Facade
→ Handler
→ ServiceId
→ Program
→ Function
→ Business
→ System
```

### Scenario C

```text
Architecture Gate
→ RULE-001~006
→ PASS/FAIL
→ Evidence
```

---

# 36. Cursor에게 직접 전달할 최종 명령

```text
이 문서를 `NSIGHT Architect Workbench`의 화면/기능 설계 기준으로 사용하라.

목표는 Ontology 관리 UI가 아니라
아키텍트가 실제로 사용하는 Architecture Intelligence Console을 만드는 것이다.

먼저 현재 UI 소스와 기술스택을 분석하고
`ARCHITECT-WORKBENCH-UI-ANALYSIS.md`를 작성하라.

분석 완료 후에만 1차 범위인

1. Architect Home
2. Architecture Search
3. Impact Analysis
4. Architecture Gate

를 구현하라.

기존 `tcf-ontology-service` API를 실제 연결하고 Mock 데이터로 완료 처리하지 마라.

Golden Scenario는
- ServiceId = mgcoa8888S0
- Table = TB_FW_IMAGE_LOG
를 사용한다.

구현 완료 후 `ARCHITECT-WORKBENCH-UI-IMPLEMENTATION-REPORT.md`를 작성하고 다음을 증명하라.

- 변경 파일
- Route
- Component
- API 연결
- 실제 화면 실행
- 실제 API Response
- Error/Timeout 처리
- Architecture Gate 결과
- Evidence 표시
- 테스트 결과

새로운 AI Chat, ADR, Graph Editor, Concept CRUD는 1차 범위에서 구현하지 마라.
```

---

# 37. 마무리말

`tcf-ontology-service`가 NSIGHT/PDMG의 Architecture Knowledge Engine이라면, `NSIGHT Architect Workbench`는 그 지식을 실제 업무에 사용하는 아키텍트의 작업공간이다.

1차 목표는 단순하다.

> **ServiceId를 검색하면 구조가 보이고, Table을 선택하면 영향도가 보이며, 개발 결과를 선택하면 Architecture Rule 위반이 보이는 화면**

이 세 가지가 실제 데이터로 동작하면 Architect Workbench 1차는 성공으로 판단한다.
