# NSIGHT Architecture Design Assistant 2차 화면 설계서
## tcf-ontology-service 기반 신규 시스템/거래 아키텍처 설계 지원

- 문서버전: v1.0
- 작성일: 2026-08-10
- 대상 시스템: `tcf-ontology-service`
- 대상 UI: `NSIGHT Architect Workbench`
- 1차 선행 구현: Architect Home / Architecture Search / Impact Analysis / Architecture Gate
- 2차 구현 대상: **Architecture Design Assistant**
- 구현 원칙: 기존 Workbench UI 스택과 기존 Ontology API를 우선 재사용
- 구현 금지: 근거 없는 AI 자동설계, Mock 데이터 기반 완료처리, Ontology와 무관한 일반 Spring 패턴 추천

---

# 1. 도입 전 안내말

Architect Workbench 1차는 다음 기능을 실제 API와 연결하여 구현하였다.

```text
Architect Home
Architecture Search
Impact Analysis
Architecture Gate
Evidence / Provenance
```

2차 Architecture Design Assistant의 목적은 단순히 “AI가 설계안을 작성하는 화면”을 만드는 것이 아니다.

핵심 목적은 다음과 같다.

> **신규 요구사항을 입력하면 Ontology에서 유사한 기존 업무·Program·ServiceId·Architecture Pattern을 찾고, 근거가 있는 설계 Baseline을 제시하며, 최종적으로 Cursor/DAVIS CODER/LLM이 개발에 사용할 수 있는 Context를 생성하는 것**

따라서 본 기능은 다음 순서를 반드시 지킨다.

```text
신규 요구사항
   ↓
Ontology 검색
   ↓
유사 기존 구현 탐색
   ↓
Architecture Pattern 추출/선정
   ↓
표준 Rule 검증
   ↓
설계 Baseline 생성
   ↓
아키텍트 검토/선택
   ↓
Cursor Context Export
```

AI 또는 LLM은 Ontology에서 찾지 못한 사실을 임의 생성해서는 안 된다.

---

# 2. 문서 개요

## 2.1 목적

Architecture Design Assistant는 신규 업무 시스템 또는 신규 거래 설계 시 다음을 지원한다.

1. 신규 요구사항 입력
2. 업무/거래 특성 분류
3. Ontology 기반 유사 Program/ServiceId 검색
4. 기존 검증된 Architecture Pattern 추천
5. ServiceId 구조 추천
6. 계층구조 추천
7. 전문/트랜잭션/페이징/데이터 접근 구조 추천
8. 추천 근거와 Provenance 표시
9. Architecture Rule 사전 검증
10. 설계 Baseline 생성
11. Cursor/DAVIS CODER/LLM Context 생성
12. 향후 ADR 등록과 연결

---

## 2.2 적용범위

### 2차 구현 범위

- Architecture Design Assistant 메인 화면
- 요구사항 입력
- 유사 구현 검색
- 추천 Pattern
- Pattern 비교
- 설계 Baseline Preview
- Evidence / Provenance
- Architecture Gate 사전검증
- Cursor Context Export
- 설계안 Markdown Export

### 2차에서 제외

- AI Chat
- 자유형 Graph Editor
- Concept CRUD
- Relation CRUD
- Ontology 관리화면
- ADR 영속화
- 자동 코드 Commit
- 자동 PR 생성
- 완전 자동 코드 생성
- Neo4j/RDF/Vector DB 도입

---

## 2.3 대상 독자

- Application Architect
- Framework Architect
- Solution Architect
- 개발 PL
- 개발 표준 담당자
- Cursor/DAVIS CODER 기반 개발자
- Architecture Governance 담당자

---

## 2.4 선행조건

1차 Workbench와 다음 기능이 정상 동작해야 한다.

```text
GET /api/ontology/catalog
GET /api/ontology/query/service/{serviceId}/structure
GET /api/ontology/query/program/{programId}/services
GET /api/ontology/query/business/{businessCode}/tree
GET /api/ontology/impact/table/{tableName}
GET /api/ontology/recommend
GET /api/ontology/prompt/{id}
GET /api/ontology/validate/rules
GET /api/ontology/v1/concept/{id}
GET /api/ontology/runtime/tx-chain
```

현재 Workbench는 다음 구조를 유지한다.

```text
src/main/resources/static/workbench/
├─ index.html
├─ css/workbench.css
└─ js/
   ├─ api.js
   └─ app.js
```

라우팅은 Hash SPA를 유지한다.

---

## 2.5 용어 정의

| 용어 | 정의 |
|---|---|
| Design Assistant | 신규 시스템/거래의 Architecture Baseline을 생성하는 아키텍트 지원 화면 |
| Candidate | Ontology 검색으로 찾은 유사 Program/ServiceId 후보 |
| Pattern | 반복 가능한 검증된 아키텍처 구조 |
| Baseline | 신규 개발을 시작하기 위한 아키텍처 초기 설계 |
| Recommendation | Ontology Evidence에 기반한 추천 |
| Confidence | 추천 근거의 충족 정도를 표현한 점수/등급 |
| Evidence | 추천을 뒷받침하는 Source, Rule, Program, ServiceId |
| Context Export | Cursor/LLM에 전달할 구조화된 Markdown/JSON |
| Constraint | 신규 설계가 따라야 하는 기술/업무 제약조건 |

---

# 3. 문제 정의 및 설계 배경

신규 시스템 구축 시 반복적으로 다음 문제가 발생한다.

```text
신규 요구사항 발생
   ↓
비슷한 기존 프로그램 찾기
   ↓
ServiceId 규칙 확인
   ↓
Handler/Facade/Service 구조 확인
   ↓
전문 규칙 확인
   ↓
Transaction 확인
   ↓
Paging 확인
   ↓
DB 접근 구조 확인
   ↓
아키텍트 판단
```

이 과정은 사람 경험에 크게 의존한다.

Architecture Design Assistant는 다음으로 전환한다.

```text
신규 요구사항
   ↓
업무특성 입력
   ↓
Ontology Candidate 검색
   ↓
Pattern 비교
   ↓
Rule/Evidence 확인
   ↓
추천 Baseline
   ↓
아키텍트 승인
```

---

# 4. 현행 구조와 문제점

## 4.1 현재 확보된 기능

현재 Workbench 1차는 실제 API 기반으로 다음 기능을 갖는다.

- ServiceId 구조 조회
- Table 영향도
- Architecture Gate
- Evidence Drawer
- Runtime TX 조회
- Catalog/Consistency
- API Timeout/Error 처리

## 4.2 현재 Design Assistant 관련 Gap

| Gap | 설명 |
|---|---|
| Recommend 근거 약함 | 기존 RecommendService가 휴리스틱 중심일 수 있음 |
| Pattern 1급 모델 부족 | ArchitecturePattern 객체가 명시적으로 관리되지 않을 수 있음 |
| 요구사항→검색조건 변환 없음 | 사용자가 직접 검색키를 알아야 함 |
| 후보 비교 화면 없음 | 유사 ServiceId들을 한 화면에서 비교하기 어려움 |
| Baseline 생성 흐름 없음 | 후보 조회 결과가 설계안으로 이어지지 않음 |
| Cursor 전달 포맷 부족 | Prompt Context는 있으나 신규 설계 목적의 Context 조립 필요 |
| 설계 승인 상태 없음 | 추천과 최종 Architect 결정이 구분되지 않음 |

---

# 5. 요구사항과 제약조건

## 5.1 핵심 기능 요구사항

### FR-DES-001 요구사항 입력

사용자는 신규 거래/시스템 특성을 입력할 수 있어야 한다.

필수/선택 항목:

| 항목 | 예 |
|---|---|
| 시스템/업무영역 | MG / CO / 고객관리 |
| 신규/기존 업무 여부 | 신규 |
| 거래유형 | 조회/등록/수정/삭제/혼합/리포트 |
| 채널 | WEB / 전용단말 / Batch / API |
| DB 사용 | YES/NO |
| 외부연계 | YES/NO |
| 대용량 여부 | YES/NO |
| Paging 여부 | YES/NO |
| 개인정보 여부 | YES/NO/UNKNOWN |
| Timeout 필요 | YES/NO/DEFAULT |
| 예상 처리특성 | 온라인 동기 조회 등 |
| 설명 | 자연어 요구사항 |

---

### FR-DES-002 유사 후보 검색

입력된 특성을 기준으로 Ontology에서 후보를 찾는다.

후보 유형:

- Program
- ServiceId
- Business
- Function
- Architecture Pattern

후보 예:

```text
mgcoa5530S0
mgcoa8888S0
mgcoa9000S0
```

---

### FR-DES-003 후보 점수/근거

Candidate마다 다음을 표시한다.

```text
Candidate
ServiceId: mgcoa8888S0

Match
- 업무유형       MATCH
- 조회거래       MATCH
- DB사용         MATCH
- Handler 구조   MATCH
- Timeout 구조   MATCH
- Paging         PARTIAL

Confidence: HIGH

Evidence:
- Program
- ServiceId
- Source Path
- Architecture Rule
```

점수는 근거 없이 정밀한 퍼센트로 표시하지 않는다.

권장:

```text
HIGH
MEDIUM
LOW
```

또는 설명 가능한 Weighted Score만 사용한다.

---

### FR-DES-004 Architecture Pattern 추천

최소 다음 Pattern 유형을 고려한다.

```text
ONLINE_QUERY_STANDARD
ONLINE_COMMAND_STANDARD
ONLINE_PAGING_QUERY
ONLINE_EXTERNAL_CALL
ONLINE_MIXED_TRANSACTION
REPORT_QUERY
```

주의:

위 Pattern이 현재 Ontology에 실제 등록되지 않았다면
UI/Backend에서 “기존 확정 Pattern”이라고 표시하지 않는다.

그 경우:

```text
DERIVED_PATTERN
```

으로 표시하고 어떤 기존 사례에서 추출했는지 Evidence를 제공한다.

---

### FR-DES-005 설계 Baseline 생성

추천 결과를 기반으로 다음 설계 Baseline을 생성한다.

```text
Business Classification
ServiceId
Endpoint
Request Message
Response Message
Handler
Facade
Service
Rule
DAO
Mapper
Table
Transaction
Timeout
Paging
Security
Logging
Validation Rules
Reference Programs
Evidence
```

미확인 값은 반드시:

```text
UNRESOLVED
```

로 표시한다.

임의 생성 금지.

---

### FR-DES-006 Pattern 비교

최대 3개 후보를 비교한다.

```text
             Candidate A    Candidate B    Candidate C
ServiceId    8888S0         5530S0         9000S0
Type         Query          Query          Query
Paging       No/Unknown     Yes            Yes
Timeout      Default        Default        Default
Table        ...            ...            ...
Evidence     VERIFIED       VERIFIED       VERIFIED
```

---

### FR-DES-007 Architecture Gate 사전검증

Baseline이 생성되면 기존 Rule Engine으로 검증 가능한 항목을 미리 확인한다.

예:

```text
RULE-001 ServiceId 형식
RULE-002 Handler 관계
RULE-003 Handler 등록
RULE-004 Program-ServiceId
RULE-005 Service-DAO/Client
RULE-006 DAO-Mapper/SQL
```

아직 파일이 없는 신규 설계는 다음처럼 표시한다.

```text
DESIGN-TIME CHECK
IMPLEMENTATION-TIME CHECK REQUIRED
```

---

### FR-DES-008 Cursor Context Export

Architect 승인 후 다음 형식으로 Export 가능해야 한다.

```text
Markdown
JSON
```

Context에는 최소 다음을 포함한다.

- 신규 요구사항
- 선택 Candidate
- 추천 Pattern
- ServiceId 규칙
- 패키지/계층구조
- Message 규칙
- Transaction 규칙
- Paging 규칙
- Table/DAO/Mapper 구조
- Architecture Rules
- Evidence
- 금지사항
- 미결정사항
- 구현 완료 검증방법

---

# 6. 설계 원칙

1. **추천보다 Evidence가 우선**
2. Ontology에 없는 사실은 만들지 않는다.
3. 기존 구현을 “표준”과 동일시하지 않는다.
4. Existing / Recommended / Proposed를 구분한다.
5. Pattern은 반드시 출처를 가진다.
6. 사용자가 최종 선택한다.
7. AI 자동생성은 Architect 승인 후 단계로 둔다.
8. 후보가 없으면 “없음”을 정상 결과로 처리한다.
9. Candidate 1개를 억지로 추천하지 않는다.
10. 최종 설계안은 재현 가능해야 한다.

---

# 7. 대안 비교 및 의사결정

## 7.1 추천 방식

| 방식 | 장점 | 단점 | 판단 |
|---|---|---|---|
| 단순 키워드 | 구현 쉬움 | 품질 낮음 | 보조 |
| 휴리스틱 점수 | 빠름 | 근거 불투명 가능 | 1차 허용 |
| Ontology Relation 기반 | 설명 가능 | 모델 필요 | **주 방식** |
| LLM 직접추천 | 유연 | 근거 왜곡 가능 | 3차 이후 |

결정:

> **2차는 Relation + Rule + Metadata 기반 추천을 사용하고 LLM 직접 판단은 사용하지 않는다.**

---

# 8. 목표 아키텍처

```text
┌────────────────────────────────────────────────────┐
│          Architecture Design Assistant             │
├────────────────────────────────────────────────────┤
│ 1. Requirement Input                               │
│ 2. Candidate Search                                │
│ 3. Candidate Compare                               │
│ 4. Pattern Recommendation                          │
│ 5. Baseline Preview                                │
│ 6. Rule Check                                      │
│ 7. Architect Approval                              │
│ 8. Cursor Context Export                           │
└────────────────────────┬───────────────────────────┘
                         │
                         ▼
┌────────────────────────────────────────────────────┐
│              tcf-ontology-service                  │
├────────────────────────────────────────────────────┤
│ Recommend                                          │
│ Query                                              │
│ Pattern                                            │
│ Validation                                         │
│ Evidence                                           │
│ Prompt Context                                     │
└────────────────────────────────────────────────────┘
```

---

# 9. 화면 Route

기존 Hash SPA에 다음 Route를 추가한다.

```text
#/design
#/design?business=CO&type=QUERY
#/design/result/{sessionId}
```

1차 기존 Route는 유지한다.

```text
#/home
#/search
#/impact
#/gate
```

---

# 10. 화면 메뉴

Side Menu:

```text
01. Architect Home
02. Architecture Search
03. Impact Analysis
04. Architecture Gate
05. Architecture Design     ← 신규
```

아직 다음은 표시하지 않거나 Disabled 처리한다.

```text
Architecture Decision
Knowledge Graph
Standards & Evidence
```

---

# 11. 화면 05 — Architecture Design Assistant

## 11.1 화면 ID

`ARC-DESN-0001`

## 11.2 목적

신규 시스템/거래 요구사항으로부터 Ontology 기반 설계 Baseline을 생성한다.

---

## 11.3 전체 Wireframe

```text
┌────────────────────────────────────────────────────────────────────┐
│ NSIGHT Architecture Design Assistant                              │
├────────────────────────────────────────────────────────────────────┤
│ STEP 1 Requirement                                                │
│                                                                    │
│ Business        [CO ▼]       Function       [A ▼]                 │
│ Transaction     [QUERY ▼]    Channel        [WEB ▼]               │
│ DB Access       [YES ▼]      External Call  [NO ▼]                │
│ Large Data      [YES ▼]      Paging         [YES ▼]               │
│ Timeout         [DEFAULT ▼]  Personal Data  [UNKNOWN ▼]           │
│                                                                    │
│ Requirement                                                        │
│ [ 고객 목록을 조건별로 조회하고 대용량 Paging이 필요하다       ] │
│                                                                    │
│                                     [유사 아키텍처 검색]           │
├────────────────────────────────────────────────────────────────────┤
│ STEP 2 Candidate                                                  │
│                                                                    │
│ ○ mgcoa5530S0   HIGH    VERIFIED                                  │
│ ○ mgcoa8888S0   MEDIUM  VERIFIED                                  │
│ ○ mgcoa9000S0   MEDIUM  VERIFIED                                  │
│                                                                    │
│ [후보 비교]                                                        │
├────────────────────────────────────────────────────────────────────┤
│ STEP 3 Recommendation                                             │
│                                                                    │
│ Pattern: ONLINE_PAGING_QUERY                                      │
│ Status : DERIVED_PATTERN / VERIFIED_PATTERN                       │
│                                                                    │
│ Handler → Facade → Service → DAO → Mapper                         │
│ Message : hdr_nhnis + dto                                         │
│ TX      : TimeoutExecutor outer transaction                       │
│ Paging  : DB paging                                               │
│                                                                    │
│ [Evidence] [Rule Check]                                            │
├────────────────────────────────────────────────────────────────────┤
│ STEP 4 Baseline                                                   │
│                                                                    │
│ ServiceId      [____________________]                              │
│ Handler        [____________________]                              │
│ Facade         [____________________]                              │
│ Service        [____________________]                              │
│ DAO            [____________________]                              │
│ Mapper         [____________________]                              │
│ Table          [____________________]                              │
│                                                                    │
│ [Baseline 생성]                                                    │
├────────────────────────────────────────────────────────────────────┤
│ STEP 5 Architect Decision                                        │
│                                                                    │
│ [승인] [수정] [후보 재선택]                                       │
│                                                                    │
│ [Markdown Export] [JSON Export] [Cursor Context 생성]             │
└────────────────────────────────────────────────────────────────────┘
```

---

# 12. STEP 1 — Requirement Input

## 12.1 필드

| 필드 | 타입 | 필수 | 기본값 |
|---|---|---:|---|
| Business | Select | Y | - |
| Function | Select | N | - |
| Transaction Type | Select | Y | QUERY |
| Channel | Select | Y | WEB |
| DB Access | Select | Y | YES |
| External Call | Select | Y | NO |
| Large Data | Select | N | NO |
| Paging | Select | N | NO |
| Timeout | Select | N | DEFAULT |
| Personal Data | Select | N | UNKNOWN |
| Requirement | Textarea | Y | - |

## 12.2 Transaction Type

```text
QUERY
CREATE
UPDATE
DELETE
MIXED
REPORT
```

PDMG ServiceId 처리구분과 연결 시:

```text
QUERY  → S
CREATE → C
UPDATE → U
DELETE → D
MIXED  → A
REPORT → R
```

단, 실제 ServiceId 규칙은 Backend의 ServiceIdParser/Rule 기준을 따른다.

---

# 13. STEP 2 — Candidate Search

## 13.1 후보 카드

```text
┌─────────────────────────────────────────┐
│ mgcoa5530S0                             │
│                                         │
│ Business     CO / A                     │
│ Type         QUERY                      │
│ Paging       YES                        │
│ DB           YES                        │
│ Confidence   HIGH                       │
│ Status       VERIFIED                   │
│                                         │
│ Evidence                                │
│  - mapping YAML                         │
│  - source code                          │
│                                         │
│ [상세] [선택]                           │
└─────────────────────────────────────────┘
```

## 13.2 Candidate 검색 기준

최소 다음 속성을 비교한다.

```text
Business
Function
Operation Type
DB Access
External Call
Paging
Large Data
Transaction
Component Chain
```

---

# 14. STEP 3 — Candidate Compare

## 14.1 최대 후보

최대 3개.

## 14.2 비교표

```text
┌────────────────┬──────────────┬──────────────┬──────────────┐
│ Attribute      │ mgcoa5530S0  │ mgcoa8888S0  │ mgcoa9000S0  │
├────────────────┼──────────────┼──────────────┼──────────────┤
│ Business       │ CO/A         │ CO/A         │ CO/A         │
│ Type           │ QUERY        │ QUERY        │ QUERY        │
│ Paging         │ YES          │ ?            │ YES          │
│ Handler        │ YES          │ YES          │ YES          │
│ Facade         │ YES          │ YES          │ YES          │
│ DAO            │ YES          │ YES          │ YES          │
│ Mapper         │ YES          │ YES          │ YES          │
│ Provenance     │ VERIFIED     │ VERIFIED     │ VERIFIED     │
└────────────────┴──────────────┴──────────────┴──────────────┘
```

`?`는 Ontology에 정보가 없음을 의미한다.

빈 값을 임의로 NO로 바꾸지 않는다.

---

# 15. STEP 4 — Architecture Pattern Recommendation

## 15.1 Pattern 모델

권장 Domain Model:

```text
ArchitecturePattern
- id
- name
- description
- status
- applicability
- requiredRelations
- optionalRelations
- constraints
- referencePrograms
- referenceServiceIds
- rules
- provenance
- version
```

## 15.2 Pattern Status

```text
VERIFIED_PATTERN
DERIVED_PATTERN
PROPOSED_PATTERN
DEPRECATED_PATTERN
```

## 15.3 예시

```text
Pattern
ONLINE_PAGING_QUERY

Status
DERIVED_PATTERN

Derived From
- mgcoa5530S0
- mgcoa9000S0

Structure
Handler
→ Facade
→ Service
→ DAO
→ Mapper
→ Table

Message
hdr_nhnis + dto

Transaction
TCF ON + Timeout ON
→ TimeoutExecutor outer Transaction

Paging
DB Paging

Evidence
...
```

---

# 16. STEP 5 — Baseline Preview

Baseline은 다음 영역으로 구분한다.

## 16.1 Classification

```text
System
Business
Function
Program
ServiceId
```

## 16.2 Application

```text
Handler
Facade
Service
Rule
DAO
Mapper
```

## 16.3 Message

```text
Request
hdr_nhnis
dto

Response Success
hdr_nhnis
dto

Response Failure
hdr_nhnis
result
```

## 16.4 Runtime

```text
Filter
Interceptor
Controller
TcfFacade
TimeoutExecutor
Worker Thread
TransactionTemplate
Dispatcher
Handler
...
```

## 16.5 Data

```text
Database
Table
Column
Paging
SQL
```

## 16.6 Governance

```text
Rules
Evidence
Reference Program
Unresolved Decisions
```

---

# 17. Baseline 화면 예시

```text
Architecture Baseline

Classification
────────────────────────────────────
Business       CO
Function       A
Program        NEW
ServiceId      UNRESOLVED

Recommended Structure
────────────────────────────────────
Handler
 ↓
Facade
 ↓
Service
 ↓
DAO
 ↓
Mapper
 ↓
Table

Transaction
────────────────────────────────────
TCF ON
Timeout DEFAULT
Outer TX: TimeoutExecutor

Message
────────────────────────────────────
Request : hdr_nhnis + dto
Success : hdr_nhnis + dto
Failure : hdr_nhnis + result

Reference
────────────────────────────────────
mgcoa5530S0
mgcoa8888S0

Unresolved
────────────────────────────────────
- 신규 Program 번호
- 실제 Table
- 개인정보 여부
```

---

# 18. Evidence Panel

Recommendation/Baseline의 모든 핵심 값은 Evidence 조회 가능해야 한다.

예:

```text
Property
Transaction Owner

Value
TimeoutExecutor

Evidence
Source Type   YAML / Markdown / Source
Source Path   ...
Status        VERIFIED
```

UI는 다음 색상/표시 정책을 사용한다.

```text
VERIFIED    : 정상 표시
DISCOVERED  : 확인 필요
PROPOSED    : 제안
DEPRECATED  : 사용 비권장
UNRESOLVED  : 미결정
```

색상은 기존 workbench.css 디자인 체계를 따른다.

---

# 19. Architecture Gate 연계

Baseline Preview에서:

```text
[Architecture Gate]
```

클릭 시 Gate 화면으로 이동하거나 Design-Time Validation을 실행한다.

Design-Time Rule은 다음 상태를 구분한다.

```text
PASS
FAIL
NOT_APPLICABLE
NOT_YET_IMPLEMENTED
UNRESOLVED
```

신규 설계 단계에서 Handler Java 파일이 아직 없다는 이유만으로 RULE-002를 무조건 FAIL 처리하지 않는다.

예:

```text
RULE-002
ServiceId → Handler

Status:
NOT_YET_IMPLEMENTED

Reason:
Design Baseline only
```

---

# 20. Cursor Context Export

## 20.1 목적

Cursor가 일반 지식이 아닌 NSIGHT/PDMG Ontology 근거로 구현하도록 Context를 제공한다.

## 20.2 Export 구조

```markdown
# NSIGHT Development Context

## Requirement

## Selected Architecture Pattern

## Business Classification

## ServiceId Rule

## Component Structure

## Message Contract

## Transaction

## Timeout

## Paging

## Data Access

## Security

## Architecture Rules

## Reference Implementations

## Evidence

## Unresolved Decisions

## Prohibited Implementations

## Completion Validation
```

---

# 21. Cursor Context 예시

```text
신규 온라인 조회 거래를 구현한다.

Reference:
mgcoa5530S0

Required Structure:
Handler
→ Facade
→ Service
→ DAO
→ Mapper

ServiceId:
11자리 PDMG 형식

Message:
Request = hdr_nhnis + dto
Success = hdr_nhnis + dto
Failure = hdr_nhnis + result

Transaction:
TCF ON + Timeout ON이면 TimeoutExecutor 외곽 Transaction에 참여

Paging:
DB Paging 사용

Important:
Ontology에서 확인되지 않은 Table/Column을 임의 생성하지 말 것.

Implementation 완료 후:
Architecture Gate RULE-001~006 실행
```

---

# 22. API 설계

## 22.1 기존 API 우선 재사용

```text
GET /api/ontology/recommend
GET /api/ontology/query/service/{id}/structure
GET /api/ontology/query/business/{code}/tree
GET /api/ontology/v1/concept/{id}
GET /api/ontology/validate/rules
GET /api/ontology/prompt/{id}
```

## 22.2 필요 시 신규 Backend API

현재 API로 화면 조립이 지나치게 복잡할 때만 다음 통합 API를 추가한다.

### POST `/api/ontology/design/recommend`

Request:

```json
{
  "businessCode": "CO",
  "functionCode": "A",
  "transactionType": "QUERY",
  "channel": "WEB",
  "dbAccess": true,
  "externalCall": false,
  "largeData": true,
  "paging": true,
  "timeoutPolicy": "DEFAULT",
  "personalData": "UNKNOWN",
  "requirement": "고객 목록 조건 조회"
}
```

Response:

```json
{
  "candidates": [],
  "recommendedPattern": {},
  "baseline": {},
  "rules": [],
  "evidence": [],
  "unresolved": []
}
```

주의:

Backend 신규 API를 만들기 전에 기존 `/recommend`, `/query`, `/prompt` 조합으로 가능한지 먼저 검토한다.

---

# 23. UI Component 설계

추가 Component:

```text
RequirementForm
CandidateList
CandidateCard
CandidateCompare
PatternRecommendation
BaselinePreview
UnresolvedDecisionList
DesignRulePanel
ContextExportPanel
```

기존 Component 재사용:

```text
Shell
Header
Side Menu
Global Search
Evidence Drawer
Error Banner
```

---

# 24. 책임 경계와 RACI

| 업무 | Architect | Workbench UI | Ontology Service | Cursor/LLM |
|---|---|---|---|---|
| 요구사항 입력 | A/R | C | - | - |
| 후보 탐색 | C | R | A/R | - |
| Pattern 산정 | A | C | R | - |
| Evidence 제공 | C | C | A/R | - |
| Baseline 생성 | A | R | R | C |
| 최종 승인 | A/R | C | C | - |
| Context Export | A | R | R | C |
| 코드 구현 | C | - | C | A/R |
| 구현 후 Gate | A | C | R | C |

---

# 25. 정상 처리 흐름

```text
Architect
→ Architecture Design
→ Requirement 입력
→ Recommend/Query API
→ Candidate 조회
→ Candidate 비교
→ Pattern 추천
→ Evidence 확인
→ Baseline 생성
→ Rule 사전검증
→ Architect 승인
→ Context Export
→ Cursor 개발
```

---

# 26. 후보 없음 흐름

```text
Requirement
→ Candidate Search
→ 0건

화면:
"현재 Ontology에서 충분히 유사한 기존 구현을 찾지 못했습니다."

제공:
- 조건 완화
- Business Tree 조회
- Architecture Search 이동
- 신규 Pattern 제안 시작

금지:
유사 후보가 없는데 임의 ServiceId를 기존 사례처럼 제시
```

---

# 27. 오류·Timeout·장애 흐름

기존 Workbench 공통 오류정책을 재사용한다.

- HTTP 4xx/5xx → Error Banner
- 12초 Timeout → 요청시간 초과
- Evidence 조회실패 → 본문 결과와 별도로 Evidence Error 표시
- Candidate 일부 실패 → 성공 후보만 표시하되 PARTIAL 상태 표시

---

# 28. 정상 예시

## 입력

```text
Business       CO
Function       A
Transaction    QUERY
Channel        WEB
DB             YES
Large Data     YES
Paging         YES
Requirement    고객 목록을 조건별로 조회
```

## 결과 예시

```text
Candidates
1. mgcoa5530S0
2. mgcoa8888S0

Recommendation
ONLINE_PAGING_QUERY
Status: DERIVED_PATTERN

Baseline
Handler
→ Facade
→ Service
→ DAO
→ Mapper
→ Table

ServiceId
UNRESOLVED

Evidence
VERIFIED
```

---

# 29. 금지 예시

## 29.1 일반 Spring 패턴 임의 추천

금지:

```text
Spring에서 보통 Repository를 사용하므로
JPA Repository 구조로 생성하겠습니다.
```

Ontology/NSIGHT 기준에 없다면 사용 금지.

## 29.2 유사도 조작

금지:

```text
99.8% 유사
```

근거 계산식이 없다면 사용하지 않는다.

## 29.3 미확인 정보 생성

금지:

```text
Table = TB_CUSTOMER
```

실제 Evidence가 없으면:

```text
Table = UNRESOLVED
```

---

# 30. 데이터 및 상태관리

UI에서 Design Session은 2차 초기 버전에서 브라우저 메모리로 유지해도 된다.

영속화가 필요한 항목:

```text
Architecture Decision
Approved Baseline
ADR
```

는 후속 버전으로 분리한다.

---

# 31. 성능·용량·확장성

- Candidate 최대 초기 10건
- 비교 최대 3건
- Evidence Lazy Load
- Structure Graph는 필요한 후보만 조회
- Recommend API p95 3초 목표
- 대규모 후보 검색 시 Pagination 고려

---

# 32. 보안·개인정보·감사

- Personal Data 입력값은 메타정보만 의미한다.
- 실제 고객정보는 입력/저장하지 않는다.
- Source Path 권한정책은 기존 Workbench와 동일
- Context Export에는 비밀키/계정정보 포함 금지
- Architect 승인 기능 영속화 시 사용자/시간 기록 필요

---

# 33. 자동검증 및 품질 Gate

UI 완료 전:

- Candidate 0건
- Candidate 1건
- Candidate 3건 이상
- Evidence 존재/누락
- Recommend API 실패
- Rule API 실패
- Timeout
- Invalid ServiceId
- Unresolved field
- Context Export
- 기존 1차 Route 회귀

를 검증한다.

---

# 34. 테스트 시나리오

| TC | 시나리오 | 기대 |
|---|---|---|
| DES-001 | CO/A QUERY 검색 | Candidate 반환 |
| DES-002 | mgcoa5530S0 후보 선택 | 구조/Evidence 표시 |
| DES-003 | 3개 후보 비교 | 비교표 표시 |
| DES-004 | 후보 없음 | 정상 No Candidate 처리 |
| DES-005 | Pattern 추천 | 근거/상태 표시 |
| DES-006 | 미확인 Table | UNRESOLVED |
| DES-007 | Gate 실행 | Design-Time 상태 구분 |
| DES-008 | Markdown Export | Context 파일 생성 |
| DES-009 | API Timeout | 오류 안내 |
| DES-010 | 기존 Search/Impact/Gate | 회귀 없음 |

---

# 35. 완료 체크리스트

- [ ] `#/design` Route
- [ ] Side Menu 추가
- [ ] RequirementForm
- [ ] Candidate Search
- [ ] Candidate List
- [ ] Candidate Compare
- [ ] Pattern Recommendation
- [ ] Baseline Preview
- [ ] Evidence Drawer 연계
- [ ] Design-Time Rule Check
- [ ] Unresolved 표시
- [ ] Markdown Context Export
- [ ] JSON Context Export
- [ ] 기존 Workbench Route 회귀 테스트
- [ ] Mock 없이 실 API 연결
- [ ] mgcoa5530S0 / mgcoa8888S0 기반 Golden Scenario

---

# 36. 변경·호환성·폐기 관리

1. 기존 1차 UI를 깨지 않는다.
2. 기존 API 계약을 임의 변경하지 않는다.
3. `/recommend`가 부족할 때만 신규 API 추가를 검토한다.
4. Pattern 모델 도입 시 기존 Mapping YAML과 호환성을 유지한다.
5. AI Chat 도입을 이유로 Design Assistant를 대체하지 않는다.

---

# 37. 핵심 아키텍처 판단

Architecture Design Assistant의 핵심은 “설계 자동화”가 아니다.

핵심은:

```text
기존 지식
+ 관계
+ Rule
+ Evidence
        ↓
설명 가능한 Architecture Recommendation
```

이다.

최종 결정자는 Architect다.

---

# 38. 주요 위험

1. RecommendService의 점수가 설명 불가능할 위험
2. 기존 사례를 표준으로 오해할 위험
3. Candidate가 없는 경우 AI가 임의 생성할 위험
4. Pattern 모델이 너무 빨리 복잡해질 위험
5. Design-Time Rule과 Implementation-Time Rule을 혼동할 위험

---

# 39. 우선 보완 과제

1. RecommendService 실제 로직 분석
2. ArchitecturePattern 도입 필요성 검토
3. Candidate Evidence 구조 정리
4. Design-Time Validation 상태 모델 정의
5. Context Export 표준 포맷 정의

---

# 40. 중장기 발전 방향

```text
Architecture Search
        ↓
Design Assistant
        ↓
Approved Baseline
        ↓
Architecture Decision / ADR
        ↓
Cursor / DAVIS CODER
        ↓
Auto Harness
        ↓
Generated Code
        ↓
Architecture Gate
        ↓
Ontology Seed / Feedback
```

---

# 41. Cursor 구현 전 분석 지시

코드를 바로 수정하지 말고 먼저 다음을 분석한다.

1. 현재 `RecommendService` 구현
2. `/api/ontology/recommend` Request/Response
3. `PromptContextExporter`
4. 기존 Workbench `app.js`
5. 기존 `api.js`
6. Evidence Drawer
7. Architecture Gate 응답
8. 현재 Mapping YAML에 Paging/Transaction 등 추천에 필요한 Metadata가 충분한지
9. ArchitecturePattern 모델이 이미 존재하는지
10. 신규 `/design/recommend` API가 정말 필요한지

분석 결과:

```text
ARCHITECT-DESIGN-ASSISTANT-ANALYSIS.md
```

작성.

분석이 끝나기 전 대규모 코드를 생성하지 않는다.

---

# 42. Cursor 구현 지시

분석 후 다음 순서로 구현한다.

## Phase 1

```text
#/design
Requirement Form
Candidate List
Candidate Detail
Evidence
```

## Phase 2

```text
Candidate Compare
Pattern Recommendation
Baseline Preview
```

## Phase 3

```text
Design-Time Gate
Markdown Export
JSON Export
Cursor Context
```

---

# 43. Golden Scenario

다음 시나리오를 반드시 실 API로 검증한다.

## Scenario 1 — 조회 거래

```text
Business = CO
Function = A
Transaction = QUERY
DB = YES
Paging = YES
```

Ontology에서 실제 Candidate를 조회한다.

후보 예시는 실제 등록 데이터 기준으로 판단하며 임의로 고정하지 않는다.

---

## Scenario 2 — Candidate 선택

선택한 ServiceId의 실제 구조를 조회한다.

```text
ServiceId
→ Handler
→ Facade
→ Service
→ DAO
→ Mapper
→ Table
```

Evidence까지 확인한다.

---

## Scenario 3 — Baseline

Candidate 기반 신규 Baseline 생성.

확인되지 않은:

```text
Program ID
ServiceId
Table
Column
```

은 UNRESOLVED로 남긴다.

---

## Scenario 4 — Cursor Context

생성된 Markdown에 다음이 포함되는지 검증한다.

```text
Requirement
Pattern
Reference ServiceId
Architecture Structure
Message
Transaction
Paging
Rules
Evidence
Unresolved
Prohibited
Validation
```

---

# 44. Cursor 최종 명령

```text
이 문서를 NSIGHT Architect Workbench의
Architecture Design Assistant 2차 구현 기준으로 사용하라.

먼저 현재 tcf-ontology-service의

- RecommendService
- PromptContextExporter
- /api/ontology/recommend
- Workbench app.js/api.js
- Architecture Gate
- Provenance
- Mapping YAML metadata

를 분석하라.

결과를

ARCHITECT-DESIGN-ASSISTANT-ANALYSIS.md

로 작성하라.

분석 전 대규모 구현을 하지 마라.

분석 후 기존 Workbench 기술스택인
static HTML + vanilla JS + CSS + Hash SPA를 유지하여
다음을 구현하라.

1. #/design Route
2. Requirement Input
3. Candidate Search
4. Candidate Compare
5. Pattern Recommendation
6. Baseline Preview
7. Evidence
8. Design-Time Architecture Gate
9. Markdown Context Export
10. JSON Context Export

Mock 데이터로 완료처리하지 마라.

실제 tcf-ontology-service API와 Ontology 데이터를 사용하라.

Ontology에 없는 사실은 임의 생성하지 말고
UNRESOLVED로 표시하라.

추천 결과에는 반드시 Evidence를 표시하라.

Pattern이 기존에 등록된 것이 아니라 후보들에서 추출한 경우
DERIVED_PATTERN이라고 명시하라.

구현 완료 후

ARCHITECT-DESIGN-ASSISTANT-IMPLEMENTATION-REPORT.md

를 작성한다.

보고서에는 다음을 포함한다.

- 변경 파일
- 신규 API
- Route
- UI Component
- 실제 Candidate 검색 결과
- 실제 Pattern 추천 결과
- 실제 Baseline 결과
- Evidence
- Architecture Gate
- Markdown/JSON Export 샘플
- 오류/Timeout
- 테스트 결과
- 기존 Workbench 회귀 결과
- 남은 Gap

최종적으로 다음 질문에 답할 수 있어야 한다.

"신규 조회 거래를 만들려고 한다.
우리 NSIGHT/PDMG에서 어떤 기존 거래를 참고하고,
어떤 구조로 설계해야 하며,
그 판단의 근거는 무엇인가?"
```

---

# 45. 마무리말

Architecture Design Assistant 2차의 완료 기준은 화면 수가 아니다.

다음 흐름이 실제 Ontology 데이터로 작동하면 성공이다.

```text
신규 요구사항 입력
        ↓
유사 기존 구현 조회
        ↓
Pattern 추천
        ↓
Evidence 확인
        ↓
Architecture Baseline 생성
        ↓
Architect 승인
        ↓
Cursor Context Export
```

이 기능이 완성되면 `tcf-ontology-service`는 단순 지식 조회 시스템을 넘어
**신규 시스템 구축 시 아키텍트의 설계 판단을 지원하는 Architecture Intelligence Platform**으로 발전한다.
