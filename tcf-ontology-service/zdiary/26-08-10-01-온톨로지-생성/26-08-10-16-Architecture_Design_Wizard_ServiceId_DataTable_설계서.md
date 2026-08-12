# NSIGHT Architecture Design Wizard 설계서
## ServiceId Design + Data/Table Design + Application Architecture Design

- 문서버전: v1.0
- 작성일: 2026-08-10
- 대상 시스템: `tcf-ontology-service`
- 대상 UI: `NSIGHT Architect Workbench`
- 구현 목적: 신규 업무/거래 설계 시 ServiceId, Program, Table, Column, Application Component, Runtime Policy를 하나의 Wizard에서 설계
- 구현 대상: Architecture Design Assistant 고도화
- 핵심 원칙: **추천 화면이 아니라 실제 개발 착수를 위한 Architecture Design 도구**
- 구현 순서: Requirement → Classification → ServiceId → Data/Table → Application → Runtime/Policy → Gate/Export

---

# 1. 도입 전 안내말

현재 `tcf-ontology-service`의 Architecture Design Assistant는 다음 역할을 수행할 수 있다.

```text
Requirement
   ↓
Candidate Search
   ↓
Reference Program / ServiceId
   ↓
Architecture Pattern
   ↓
Baseline
   ↓
Gate / Export
```

그러나 실제 신규 프로그램 설계에서 반드시 결정해야 하는 다음 정보가 부족하다.

```text
신규 Program은 무엇인가?
신규 ServiceId는 무엇인가?
어떤 Table을 사용할 것인가?
어떤 Column을 조회/갱신할 것인가?
어떤 DAO / Mapper를 만들 것인가?
```

따라서 Architecture Design Assistant를 단순 추천 기능에서 실제 설계 Wizard로 확장한다.

최종 흐름:

```text
Requirement
   ↓
Business Classification
   ↓
ServiceId Design
   ↓
Data / Table Design
   ↓
Application Architecture Design
   ↓
Runtime / Policy Design
   ↓
Architecture Gate
   ↓
Cursor Context Export
```

---

# 2. 문서 개요

## 2.1 목적

본 설계서는 Cursor가 다음 기능을 구현할 수 있도록 기준을 정의한다.

1. 신규 업무 요구사항 입력
2. System / Business / Function 분류
3. 기존 Program / ServiceId 현황 조회
4. 신규 Program ID 설계
5. 신규 ServiceId 설계 및 중복검증
6. 관련 Table 후보 검색
7. Table/Column/PK/Join/Access Type 설계
8. Handler/Facade/Service/Rule/DAO/Mapper 설계
9. Message/Transaction/Timeout/Paging/Security 정책 설계
10. Architecture Gate
11. Evidence/Provenance
12. Markdown/JSON Cursor Context Export

---

## 2.2 적용범위

### 포함

```text
Architecture Design Wizard
├─ STEP 1 Requirement
├─ STEP 2 Business Classification
├─ STEP 3 ServiceId Design
├─ STEP 4 Data / Table Design
├─ STEP 5 Application Architecture
├─ STEP 6 Runtime / Policy
└─ STEP 7 Gate / Export
```

### 제외

- 실제 DB DDL 자동 실행
- 실제 Table 자동 생성
- 실제 Source 자동 Commit
- 자동 PR
- ADR 영속화
- Graph Editor
- Neo4j/RDF
- LLM의 근거 없는 자유 설계

---

## 2.3 대상 독자

- Application Architect
- Framework Architect
- Solution Architect
- 개발 PL
- DB Architect / DBA
- Cursor/DAVIS CODER 사용자
- Architecture Governance 담당자

---

## 2.4 선행조건

다음 기능이 정상 동작해야 한다.

```text
Architecture Search
Impact Analysis
Architecture Gate
Design Recommendation
Provenance
Ontology Query
ServiceId Parser
Business / Function Catalog
Table / Column Concept
```

---

# 3. 문제 정의 및 설계 배경

신규 거래 개발 시 실제 아키텍트는 단순히 기존 프로그램 하나를 추천하지 않는다.

실제로는 다음을 결정한다.

```text
요건
 ↓
업무분류
 ↓
Program
 ↓
ServiceId
 ↓
Table / Column
 ↓
Application Component
 ↓
Transaction / Timeout / Paging
 ↓
표준검증
```

따라서 Architecture Design Assistant도 동일한 사고순서를 따라야 한다.

---

# 4. 현행 구조와 문제점

## 4.1 현행

현재 Design Assistant:

```text
Requirement
→ Candidate
→ Pattern
→ Baseline
→ Gate
```

## 4.2 문제점

| 문제 | 설명 |
|---|---|
| 신규 ServiceId 결정 단계 부족 | Candidate ServiceId는 찾지만 신규 ServiceId 설계가 없음 |
| Program 번호 관리 부족 | 신규 Program No 중복/사용현황 확인이 어려움 |
| Table 설계 부족 | Reference Table을 보여주는 수준 |
| Column 설계 부족 | 실제 조회/갱신 Column 선정 없음 |
| Access Type 부족 | READ/CREATE/UPDATE/DELETE 구분 부족 |
| Join/Paging 설계 부족 | Data 구조가 실제 Mapper/SQL 설계로 이어지지 않음 |
| Component 명명 부족 | 신규 Handler/Service/DAO 이름 확정 단계 부족 |

---

# 5. 설계 원칙

1. ServiceId는 자동 확정하지 않는다.
2. Ontology는 후보와 중복 여부를 제공한다.
3. Architect가 최종 승인한다.
4. 신규 Table을 임의 생성하지 않는다.
5. Ontology에 없는 Table은 `NEW_TABLE_PROPOSAL`로 구분한다.
6. Column이 확인되지 않으면 `UNRESOLVED`.
7. 기존 Program과 신규 Program을 명확히 구분한다.
8. 실제 Source Evidence와 설계 Proposal을 구분한다.
9. 모든 Design Result에 상태를 둔다.
10. Cursor Export는 Architect 승인된 결과만 사용한다.

---

# 6. 목표 Architecture Design Wizard

```text
┌────────────────────────────────────────────┐
│        Architecture Design Wizard          │
├────────────────────────────────────────────┤
│ STEP 1 Requirement                         │
│ STEP 2 Business Classification             │
│ STEP 3 ServiceId Design                    │
│ STEP 4 Data / Table Design                 │
│ STEP 5 Application Architecture            │
│ STEP 6 Runtime / Policy                    │
│ STEP 7 Gate / Export                       │
└────────────────────────────────────────────┘
```

Wizard 상태:

```text
DRAFT
IN_PROGRESS
READY_FOR_REVIEW
APPROVED
REJECTED
```

---

# 7. 전체 화면 구성

```text
┌──────────────────────────────────────────────────────────────┐
│ NSIGHT Architecture Design                                  │
├──────────────────────────────────────────────────────────────┤
│ ① Requirement                                                │
│ ② Classification                                             │
│ ③ ServiceId                                                  │
│ ④ Data/Table                                                 │
│ ⑤ Application                                                │
│ ⑥ Runtime/Policy                                             │
│ ⑦ Gate/Export                                                │
├──────────────────────────────────────────────────────────────┤
│                                                              │
│                   Current Step Area                          │
│                                                              │
├──────────────────────────────────────────────────────────────┤
│ Evidence / Reference                                         │
├──────────────────────────────────────────────────────────────┤
│ [Previous]                       [Save Draft] [Next]          │
└──────────────────────────────────────────────────────────────┘
```

---

# 8. STEP 1 — Requirement Design

## 8.1 화면 ID

`ARC-DES-REQ-0001`

## 8.2 입력

| 항목 | 필수 | 예 |
|---|---:|---|
| 설계명 | Y | 고객 목록 조회 |
| 업무 설명 | Y | 고객 기본정보와 상태정보 조회 |
| 거래 유형 | Y | QUERY |
| Channel | Y | WEB |
| DB 사용 | Y | YES |
| 외부연계 | Y | NO |
| 대용량 | N | YES |
| Paging | N | YES |
| 개인정보 | N | UNKNOWN |
| Timeout | N | DEFAULT |

## 8.3 Transaction Type

```text
QUERY
CREATE
UPDATE
DELETE
MIXED
REPORT
```

Operation Mapping:

```text
QUERY  → S
CREATE → C
UPDATE → U
DELETE → D
MIXED  → A
REPORT → R
```

---

# 9. STEP 2 — Business Classification

## 9.1 화면 ID

`ARC-DES-CLS-0001`

## 9.2 목적

신규 프로그램의 업무분류를 결정한다.

```text
System
  ↓
Business
  ↓
Function
```

## 9.3 화면

```text
System    [MG ▼]

Business  [CO ▼]

Function  [A ▼]

Current Classification

MG
└─ CO
   └─ A
```

## 9.4 데이터

UI에 Business/Function을 하드코딩하지 않는다.

Ontology Catalog/Classification에서 동적으로 조회한다.

---

# 10. STEP 3 — ServiceId Design

## 10.1 화면 ID

`ARC-DES-SID-0001`

## 10.2 목적

신규 Program ID와 ServiceId를 설계하고 기존 사용현황과 중복을 검증한다.

## 10.3 화면 Wireframe

```text
┌───────────────────────────────────────────────────────────────┐
│ ServiceId Design                                              │
├───────────────────────────────────────────────────────────────┤
│ Classification                                                │
│                                                               │
│ System       MG                                               │
│ Business     CO                                               │
│ Function     A                                                │
│                                                               │
├───────────────────────────────────────────────────────────────┤
│ Existing Programs                                             │
│                                                               │
│ mgcoa5530                                                     │
│   └─ mgcoa5530S0                                              │
│                                                               │
│ mgcoa8888                                                     │
│   ├─ mgcoa8888S0                                              │
│   └─ mgcoa8888D0                                              │
│                                                               │
│ mgcoa9000                                                     │
│   ├─ mgcoa9000S0                                              │
│   ├─ mgcoa9000C0                                              │
│   ├─ mgcoa9000U0                                              │
│   └─ mgcoa9000D0                                              │
├───────────────────────────────────────────────────────────────┤
│ New Program                                                   │
│                                                               │
│ Program No     [7777]                                         │
│ Program ID     mgcoa7777                                      │
│                                                               │
│ Operation      QUERY → S                                      │
│ Sequence       [0]                                            │
│                                                               │
│ ServiceId      mgcoa7777S0                                    │
│                                                               │
│ Validation                                                    │
│ ✅ Format                                                     │
│ ✅ Program ID not used                                        │
│ ✅ ServiceId not used                                         │
│ ✅ Business / Function match                                  │
│ ✅ QUERY → S                                                  │
│                                                               │
│ [중복검증]                         [ServiceId 확정]            │
└───────────────────────────────────────────────────────────────┘
```

---

# 11. ServiceId 표준

현재 PDMG ServiceId:

```text
mgcoa7777S0
```

구성:

```text
mg       System / Group
co       Business
a        Function
7777     Program No
S        Operation
0        Sequence
```

정규식:

```regex
^[a-z]{2}[a-z]{2}[a-z][0-9]{4}[SCUDAR][0-9A-Z]$
```

---

# 12. ServiceId Design Rule

| Rule ID | 검증 |
|---|---|
| SID-001 | 11자리 ServiceId 형식 |
| SID-002 | System 존재 |
| SID-003 | Business 존재 |
| SID-004 | Function 존재 |
| SID-005 | Program No 4자리 |
| SID-006 | Program ID 중복 여부 |
| SID-007 | Operation Mapping |
| SID-008 | ServiceId 중복 여부 |
| SID-009 | Program-ServiceId 일관성 |
| SID-010 | Sequence 중복 여부 |

---

# 13. Program No 추천

Ontology가 임의로 번호를 확정하지 않는다.

가능한 기능:

```text
현재 사용번호
5530
8888
9000
9999

미사용 후보
7777
7778
...
```

추천 결과는 `PROPOSED` 상태로 표시한다.

최종 확정은 Architect가 한다.

---

# 14. ServiceId Design API

권장:

```http
POST /api/ontology/design/service-id/validate
```

Request:

```json
{
  "system": "MG",
  "business": "CO",
  "function": "A",
  "programNo": "7777",
  "transactionType": "QUERY",
  "sequence": "0"
}
```

Response:

```json
{
  "programId": "mgcoa7777",
  "serviceId": "mgcoa7777S0",
  "operation": "S",
  "available": true,
  "findings": [],
  "status": "PROPOSED"
}
```

---

# 15. STEP 4 — Data / Table Design

## 15.1 화면 ID

`ARC-DES-DATA-0001`

## 15.2 목적

신규 ServiceId가 사용할 데이터 구조를 설계한다.

---

# 16. Data/Table 화면 Wireframe

```text
┌──────────────────────────────────────────────────────────────────┐
│ Data / Table Design                                              │
├──────────────────────────────────────────────────────────────────┤
│ Requirement                                                      │
│ 고객 기본정보 + 고객 상태정보 조회                              │
├──────────────────────────────────────────────────────────────────┤
│ Reference                                                        │
│                                                                  │
│ Candidate Program       mgcoa5530S0                              │
│ Reference Tables        TB_MK_CO_A_5530                          │
├──────────────────────────────────────────────────────────────────┤
│ Table Search                                                     │
│                                                                  │
│ [ CUSTOMER                                               ] [검색]│
│                                                                  │
│ Candidate                                                       │
│ □ TB_CUSTOMER                                                   │
│ □ TB_CUSTOMER_STATUS                                            │
│ □ TB_CUSTOMER_INFO                                              │
├──────────────────────────────────────────────────────────────────┤
│ Selected Tables                                                  │
│                                                                  │
│ ☑ TB_CUSTOMER              READ                                 │
│ ☑ TB_CUSTOMER_STATUS       READ                                 │
├──────────────────────────────────────────────────────────────────┤
│ Join                                                             │
│                                                                  │
│ TB_CUSTOMER.CUST_ID                                              │
│      =                                                           │
│ TB_CUSTOMER_STATUS.CUST_ID                                       │
├──────────────────────────────────────────────────────────────────┤
│ Paging                                                           │
│                                                                  │
│ Enabled      YES                                                 │
│ Paging Key   CUST_ID                                             │
│ Order        ASC                                                 │
│                                                                  │
│ [Table 설계 확정]                                                │
└──────────────────────────────────────────────────────────────────┘
```

---

# 17. Table Design 속성

Table마다 다음을 관리한다.

| 속성 | 설명 |
|---|---|
| tableName | Table 이름 |
| accessType | READ / CREATE / UPDATE / DELETE |
| role | MASTER / DETAIL / LOOKUP / LOG |
| primaryKey | PK |
| joinKeys | Join |
| filterColumns | 조회 조건 |
| selectColumns | 조회 Column |
| updateColumns | 변경 Column |
| sortColumns | 정렬 |
| pagingKey | Paging |
| personalData | 개인정보 |
| encryption | 암호화 |
| masking | 마스킹 |
| estimatedRows | 예상건수 |
| evidence | 근거 |

---

# 18. Table Access Type

```text
READ
CREATE
UPDATE
DELETE
READ_WRITE
```

Operation과 기본 관계:

```text
QUERY
→ READ

CREATE
→ CREATE / READ

UPDATE
→ UPDATE / READ

DELETE
→ DELETE / READ
```

단, 자동 확정하지 않는다.

---

# 19. Table 후보 선정

후보 Table은 다음 기준으로 찾는다.

```text
Reference ServiceId
→ DAO
→ Mapper
→ Table
```

추가 후보:

```text
동일 Business
동일 Function
동일 업무 Keyword
동일 Column Keyword
```

Ontology에 없는 Table을 추천하지 않는다.

신규 Table이 필요하면:

```text
NEW_TABLE_PROPOSAL
```

로 별도 등록한다.

---

# 20. Column Design

Table 선택 후 Column을 조회한다.

예:

```text
TB_CUSTOMER

PK
CUST_ID

Columns
☑ CUST_ID
☑ CUST_NM
☑ CUSTOMER_STATUS
□ ADDRESS
□ PHONE
```

Column 역할:

```text
SELECT
FILTER
JOIN
SORT
PAGING
INSERT
UPDATE
```

하나의 Column은 여러 역할을 가질 수 있다.

---

# 21. 개인정보/암호화/마스킹

Column에 Metadata가 존재하면 표시한다.

```text
PHONE_NO
Personal Data = YES
Encryption    = YES
Masking       = YES
```

정보가 없으면:

```text
UNRESOLVED
```

임의로 NO 처리하지 않는다.

---

# 22. Data Design API

## Table Search

```http
GET /api/ontology/design/tables
```

예:

```text
?business=CO
&function=A
&keyword=CUSTOMER
&referenceServiceId=mgcoa5530S0
```

## Table Detail

```http
GET /api/ontology/design/table/{tableName}
```

Response:

```json
{
  "table": {},
  "columns": [],
  "usedBy": [],
  "evidence": []
}
```

---

# 23. Ontology Data 관계

추가/표준 관계:

```text
ServiceId
  DESIGNS_DATA_ACCESS
DAO

DAO
  EXECUTES
Mapper

Mapper
  READS
Table

Mapper
  WRITES
Table

Table
  HAS_COLUMN
Column

SqlId
  READS
Table

SqlId
  WRITES
Table

SqlId
  USES_COLUMN
Column
```

실제 Source가 확인되지 않은 신규 설계 관계는:

```text
PROPOSED
```

상태로 표시한다.

---

# 24. STEP 5 — Application Architecture Design

## 24.1 화면 ID

`ARC-DES-APP-0001`

## 24.2 목적

ServiceId 및 Data Design 결과를 Application Component 구조로 구체화한다.

---

# 25. Application 화면

```text
ServiceId
mgcoa7777S0

Reference
mgcoa5530S0

Recommended Components

mgcoa7777Handler
       ↓
mgcoa7777Facade
       ↓
mgcoa7777Service
       ↓
mgcoa7777DAO
       ↓
mgcoa7777Mapper

Tables

TB_CUSTOMER
TB_CUSTOMER_STATUS
```

---

# 26. Component Naming

Program:

```text
mgcoa7777
```

Components:

```text
mgcoa7777Handler
mgcoa7777Facade
mgcoa7777Service
mgcoa7777DAO
mgcoa7777Mapper
```

실제 프로젝트 Naming Convention을 우선 적용한다.

UI에서 임의 다른 이름을 만들지 않는다.

---

# 27. Rule Design

업무 Rule이 필요한 경우:

```text
Service
   ↓
Rule
```

예:

```text
mgcoa7777CustomerRule
```

단순 CRUD에서 Rule이 필요하지 않으면:

```text
NOT_APPLICABLE
```

을 허용한다.

---

# 28. STEP 6 — Runtime / Policy Design

## 28.1 화면 ID

`ARC-DES-POLICY-0001`

## 28.2 항목

```text
Message
Transaction
Timeout
Paging
Security
Masking
Encryption
Logging
Audit
External Call
```

---

# 29. Runtime 화면

```text
Message
────────────────────────────
Request
hdr_nhnis + dto

Success
hdr_nhnis + dto

Failure
hdr_nhnis + result


Transaction
────────────────────────────
TCF                 ON
Timeout             DEFAULT
Transaction Owner   TimeoutExecutor

Paging
────────────────────────────
Enabled             YES
Type                UNRESOLVED
Key                 CUST_ID

Security
────────────────────────────
Personal Data       YES
Masking             UNRESOLVED
Encryption          UNRESOLVED
```

Evidence 없는 정책은 반드시:

```text
UNRESOLVED
```

로 둔다.

---

# 30. STEP 7 — Architecture Gate / Export

## 30.1 화면 ID

`ARC-DES-GATE-0001`

---

# 31. Design Gate

검증 영역:

```text
Business Classification
ServiceId
Data Design
Application Structure
Runtime Policy
Security
Unresolved
```

상태:

```text
PASS
PASS_WITH_UNRESOLVED
FAIL
```

---

# 32. Gate Rule 예

## ServiceId

```text
SID-001~010
```

## Data

```text
DATA-001 Table 선택 존재
DATA-002 Access Type 존재
DATA-003 PK 확인
DATA-004 Join Key 정합
DATA-005 Paging Key 존재
DATA-006 개인정보 정책
```

## Application

```text
APP-001 Handler 존재
APP-002 Facade 존재
APP-003 Service 존재
APP-004 DAO/Client 존재
APP-005 Mapper/Table 관계
```

## Runtime

```text
RUN-001 Transaction 정책
RUN-002 Timeout 정책
RUN-003 Message 정책
RUN-004 Paging 정책
```

---

# 33. Design Result Summary

```text
Architecture Design

Business
MG / CO / A

Program
mgcoa7777

ServiceId
mgcoa7777S0

Tables
TB_CUSTOMER          READ
TB_CUSTOMER_STATUS   READ

Join
CUST_ID

Paging
YES
Key = CUST_ID

Application
mgcoa7777Handler
→ mgcoa7777Facade
→ mgcoa7777Service
→ mgcoa7777DAO
→ mgcoa7777Mapper

Runtime
TCF ON
Timeout DEFAULT

Reference
mgcoa5530S0

Gate
PASS_WITH_UNRESOLVED
```

---

# 34. Cursor Context Export

최종 Markdown:

```markdown
# NSIGHT Development Context

## Requirement
고객 목록 조회

## Business Classification
System: MG
Business: CO
Function: A

## Program
mgcoa7777

## ServiceId
mgcoa7777S0
Operation: QUERY / S

## Data Design

### TB_CUSTOMER
Access: READ
PK: CUST_ID

Columns:
- CUST_ID
- CUST_NM

### TB_CUSTOMER_STATUS
Access: READ
PK: CUST_ID

Join:
TB_CUSTOMER.CUST_ID
=
TB_CUSTOMER_STATUS.CUST_ID

## Paging
Enabled: YES
Key: CUST_ID
Type: UNRESOLVED

## Application Architecture
mgcoa7777Handler
→ mgcoa7777Facade
→ mgcoa7777Service
→ mgcoa7777DAO
→ mgcoa7777Mapper

## Runtime
TCF: ON
Timeout: DEFAULT

## Reference
mgcoa5530S0

## Unresolved
- Paging Type
- Masking
- Encryption

## Architecture Rules
...

## Completion Validation
...
```

---

# 35. 정상 처리 흐름

```text
Requirement
    ↓
Classification
    ↓
Program / ServiceId
    ↓
Table / Column
    ↓
Application Component
    ↓
Runtime Policy
    ↓
Gate
    ↓
Architect Approval
    ↓
Cursor Context
```

---

# 36. 오류 흐름

## ServiceId 중복

```text
mgcoa7777S0 already exists
```

다음 단계 이동 금지.

## Table 미존재

```text
Table not found in Ontology
```

선택:

```text
Search Again
New Table Proposal
UNRESOLVED
```

## Column 미확인

```text
Column metadata unavailable
```

임의 생성 금지.

## Gate FAIL

Cursor Export는 가능하더라도:

```text
NOT_APPROVED
```

배너를 표시한다.

권장 기본은 Export 차단이다.

---

# 37. 정상 예시 — 신규 고객 목록 조회

Requirement:

```text
고객 목록 조회
```

Classification:

```text
MG / CO / A
```

ServiceId:

```text
mgcoa7777S0
```

Data:

```text
TB_CUSTOMER
TB_CUSTOMER_STATUS
```

Application:

```text
mgcoa7777Handler
→ mgcoa7777Facade
→ mgcoa7777Service
→ mgcoa7777DAO
→ mgcoa7777Mapper
```

Gate:

```text
PASS_WITH_UNRESOLVED
```

---

# 38. 금지 예시

## 금지 1 — ServiceId 자동 확정

```text
Ontology가 알아서 7777을 선택하고 바로 확정
```

금지.

## 금지 2 — Table 자동 생성

```text
요구사항에 Customer가 있으니
TB_CUSTOMER를 자동으로 만든다.
```

금지.

## 금지 3 — 존재하지 않는 Column 생성

```text
CUST_SCORE
```

Evidence가 없으면 생성 금지.

## 금지 4 — 정책 임의 확정

```text
Paging = OFFSET
```

근거 없으면:

```text
UNRESOLVED
```

---

# 39. 데이터 및 상태관리

Design Session:

```text
DRAFT
REVIEW
APPROVED
```

1차에서는 Browser memory 가능.

향후:

```text
DesignSession
ArchitectureDecision
```

서버 저장.

---

# 40. RACI

| 업무 | Architect | Workbench | Ontology | Cursor |
|---|---|---|---|---|
| Requirement | A/R | R | - | - |
| Classification | A | R | C | - |
| Program/ServiceId | A/R | R | C | - |
| 중복검증 | C | C | A/R | - |
| Table 선정 | A/R | R | C | - |
| Column 선정 | A/R | R | C | - |
| App 구조 | A | R | C | - |
| Runtime 정책 | A/R | R | C | - |
| Gate | A | C | R | - |
| Cursor Context | A | R | C | C |
| 구현 | C | - | C | A/R |

---

# 41. API 구성 권장

```text
POST /api/ontology/design/session

POST /api/ontology/design/service-id/validate

GET  /api/ontology/design/programs
GET  /api/ontology/design/tables
GET  /api/ontology/design/table/{table}
GET  /api/ontology/design/table/{table}/columns

POST /api/ontology/design/application
POST /api/ontology/design/policy
POST /api/ontology/validate/design

GET  /api/ontology/design/export/{sessionId}?format=markdown
GET  /api/ontology/design/export/{sessionId}?format=json
```

기존 API 재사용 가능 여부를 먼저 분석한다.

불필요한 신규 API 생성 금지.

---

# 42. UI Component

추가:

```text
DesignStepper
RequirementForm
ClassificationSelector
ProgramInventory
ServiceIdDesigner
ServiceIdValidationPanel

TableCandidateSearch
SelectedTableGrid
ColumnSelector
JoinDesigner
PagingDesigner

ApplicationComponentDesigner
PolicyDesigner
DesignGatePanel
DesignSummary
CursorExportPanel
```

---

# 43. 테스트 시나리오

| TC | 내용 | 기대 |
|---|---|---|
| DES-SID-001 | QUERY | S |
| DES-SID-002 | CREATE | C |
| DES-SID-003 | UPDATE | U |
| DES-SID-004 | DELETE | D |
| DES-SID-005 | MIXED | A |
| DES-SID-006 | REPORT | R |
| DES-SID-007 | 중복 Program | FAIL |
| DES-SID-008 | 중복 ServiceId | FAIL |
| DES-DATA-001 | Existing Table | 조회 |
| DES-DATA-002 | Multi Table | 다중 선택 |
| DES-DATA-003 | Composite PK | 별도 Column |
| DES-DATA-004 | Join | 검증 |
| DES-DATA-005 | 개인정보 Metadata 없음 | UNRESOLVED |
| DES-APP-001 | Component naming | PASS |
| DES-GATE-001 | 미결정 존재 | PASS_WITH_UNRESOLVED |
| DES-EXP-001 | Markdown | 정상 |
| DES-EXP-002 | undefined 없음 | PASS |
| DES-EXP-003 | [object Object] 없음 | PASS |

---

# 44. Golden Scenario

Cursor는 다음 시나리오를 실제 데이터로 검증한다.

## 신규 고객조회 설계

```text
System       MG
Business     CO
Function     A
Transaction  QUERY
Channel      WEB
DB           YES
Paging       YES
```

### STEP 1
Requirement 입력.

### STEP 2
MG / CO / A 분류.

### STEP 3

Existing Program 확인:

```text
mgcoa5530
mgcoa8888
mgcoa9000
```

신규 Program 후보:

```text
mgcoa7777
```

신규 ServiceId 후보:

```text
mgcoa7777S0
```

중복검증.

### STEP 4

실제 Ontology Table 후보를 조회한다.

없는 Table 이름을 임의로 만들지 않는다.

실제 후보가 충분하지 않으면:

```text
UNRESOLVED
```

로 유지한다.

### STEP 5

신규 Application Component 설계.

### STEP 6

Runtime Policy.

### STEP 7

Gate + Cursor Export.

---

# 45. 자동검증 및 품질 Gate

완료 조건:

```text
ServiceId Design PASS
Data Design PASS/PASS_WITH_UNRESOLVED
Application Design PASS
Policy Design PASS_WITH_UNRESOLVED 가능
Export 정상
```

금지 문자열:

```text
undefined
[object Object]
NaN
```

---

# 46. 변경·호환성

1. 기존 `#/design`을 깨지 않는다.
2. 기존 Recommend API 호환.
3. 기존 Search/Impact/Gate 유지.
4. Wizard를 단계적으로 추가.
5. 기존 Design Assistant를 Wizard Shell로 확장 가능.
6. ServiceId/Data 단계를 먼저 완성한다.

---

# 47. 구현 우선순위

## Phase 0

현행 `design.js`, Backend Design API, Table/Column Concept를 분석한다.

산출:

```text
ARCHITECTURE-DESIGN-WIZARD-GAP-ANALYSIS.md
```

## Phase 1

```text
Business Classification
ServiceId Design
```

## Phase 2

```text
Data / Table Design
Column / PK / Join
```

## Phase 3

```text
Application Architecture
Runtime / Policy
```

## Phase 4

```text
Gate
Export
Golden Scenario
```

---

# 48. Cursor 최종 구현 명령

```text
너는 NSIGHT/PDMG tcf-ontology-service의 수석 Application Architect이다.

현재 Architecture Design Assistant를
"기존 사례 추천 화면"에서
"실제 신규 프로그램 Architecture Design Wizard"로 확장하라.

목표 Flow:

Requirement
→ Business Classification
→ ServiceId Design
→ Data/Table Design
→ Application Architecture
→ Runtime/Policy
→ Architecture Gate
→ Cursor Context Export

중요:

코드를 바로 수정하지 말고 먼저 현재:

- design.js
- DesignRecommendationService
- ServiceId Parser
- OntologyQueryService
- Table/Column Concept
- Mapping YAML
- Architecture Gate
- Prompt Context Export

를 분석하라.

결과:

ARCHITECTURE-DESIGN-WIZARD-GAP-ANALYSIS.md

를 작성한다.

그 다음 단계적으로 구현한다.

### STEP 1 Requirement

기존 기능 재사용.

### STEP 2 Classification

System / Business / Function을 Ontology에서 동적으로 로드한다.

### STEP 3 ServiceId Design

다음을 구현한다.

- Existing Program 조회
- Existing ServiceId 조회
- Program No 입력
- Program ID 자동 조합
- Transaction Type → Operation
- Sequence
- ServiceId 생성
- Program 중복검사
- ServiceId 중복검사
- ServiceId 11자리 Rule
- Architect 확정

Ontology가 번호를 임의 확정하지 마라.

### STEP 4 Data / Table Design

다음을 구현한다.

- Reference ServiceId Table 조회
- Table 검색
- Table 다중선택
- READ/CREATE/UPDATE/DELETE
- PK
- Column
- Join Key
- Filter Column
- Select/Update Column
- Sort
- Paging Key
- Personal Data
- Masking
- Encryption

Ontology에 없는 Table/Column은 임의 생성하지 않는다.

신규 Table 필요 시:

NEW_TABLE_PROPOSAL

미확인 정보:

UNRESOLVED

### STEP 5 Application Architecture

ServiceId/Program을 기준으로:

Handler
Facade
Service
Rule
DAO
Mapper

설계안을 만든다.

기존 Naming Convention을 따른다.

### STEP 6 Runtime / Policy

Message
Transaction
Timeout
Paging
Security
Masking
Encryption
Logging
Audit

를 설계한다.

Evidence 없는 값은 UNRESOLVED.

### STEP 7 Gate / Export

검증:

ServiceId
Data
Application
Runtime
Security

상태:

PASS
PASS_WITH_UNRESOLVED
FAIL

Markdown/JSON Export를 제공한다.

Export에 다음 문자열이 들어가면 실패다.

undefined
[object Object]
NaN

### Golden Scenario

신규 고객조회 프로그램:

MG / CO / A
QUERY
WEB
DB=YES
Paging=YES

신규 Program 후보:

mgcoa7777

신규 ServiceId 후보:

mgcoa7777S0

단, 실제 프로젝트에 이미 존재한다면 사용하지 말고
중복 검증 결과를 기준으로 다른 번호를 Architect에게 제안한다.

Table은 실제 Ontology에서 조회한다.

없는 Table을 예시값으로 실데이터처럼 생성하지 않는다.

### 완료 보고서

구현 완료 후:

ARCHITECTURE-DESIGN-WIZARD-IMPLEMENTATION-REPORT.md

작성.

포함:

1. 변경 파일
2. Route
3. UI Component
4. 신규/재사용 API
5. ServiceId Design 결과
6. Data/Table Design 결과
7. Column/PK/Join
8. Application Architecture
9. Runtime/Policy
10. Gate
11. Cursor Context Export
12. 실제 Golden Scenario
13. Test
14. 기존 기능 Regression
15. 남은 Gap

다음 질문에 실제 데이터로 답할 수 있어야 완료다.

"신규 거래를 개발하려고 한다.
어떤 Program과 ServiceId를 사용하고,
어떤 Table과 Column을 사용하며,
어떤 Handler/Service/DAO/Mapper를 만들고,
어떤 정책을 적용해야 하는가?"
```

---

# 49. 체크리스트

- [ ] 7-Step Wizard
- [ ] Dynamic Classification
- [ ] Program Inventory
- [ ] ServiceId Design
- [ ] Program 중복검증
- [ ] ServiceId 중복검증
- [ ] Table Search
- [ ] Multiple Table
- [ ] Column
- [ ] Composite PK
- [ ] Join
- [ ] Access Type
- [ ] Paging Key
- [ ] Personal Data
- [ ] Component Design
- [ ] Runtime Policy
- [ ] Design Gate
- [ ] Cursor Markdown
- [ ] Cursor JSON
- [ ] Evidence
- [ ] UNRESOLVED
- [ ] Golden Scenario
- [ ] Regression

---

# 50. 시사점

## 핵심 아키텍처 판단

Architecture Design Assistant의 중심은 더 이상 Candidate Recommendation만이 아니다.

다음 두 단계가 핵심이다.

```text
ServiceId Design
+
Data/Table Design
```

이 두 단계가 존재해야:

```text
ServiceId
→ Handler
→ Service
→ DAO
→ Mapper
→ Table
→ Column
```

이라는 실제 개발 추적성이 완성된다.

## 주요 위험

1. ServiceId 자동할당으로 번호정책이 오염될 수 있음
2. 존재하지 않는 Table을 AI가 만들 위험
3. Column Metadata 미확인을 NO로 오해할 위험
4. 기존 Reference 구조를 무조건 신규 표준으로 복사할 위험
5. 설계 Proposal과 실제 Source VERIFIED 관계가 혼재할 위험

## 우선 보완 과제

```text
1. ServiceId Design
2. Table Design
3. Column / PK / Join
4. Application Component
5. Policy
6. Gate
7. Export
```

## 중장기 발전 방향

```text
화면 Event
   ↓
ServiceId
   ↓
Handler
   ↓
Facade
   ↓
Service
   ↓
Rule
   ↓
DAO
   ↓
Mapper
   ↓
SqlId
   ↓
Table
   ↓
Column
```

향후 화면 설계서와 연결하면:

```text
Screen
→ Event
→ ServiceId
→ Program
→ Component
→ SQL
→ Table
→ Column
```

전체 추적성까지 확장할 수 있다.

---

# 51. 마무리말

Architecture Design Wizard의 성공 기준은 화면이 화려한 것이 아니다.

신규 거래 하나를 설계할 때 아키텍트가 다음 질문에 답할 수 있어야 한다.

```text
어떤 업무분류인가?
어떤 Program인가?
어떤 ServiceId인가?
어떤 Table인가?
어떤 Column인가?
어떤 Component인가?
어떤 Runtime 정책인가?
어떤 근거로 이렇게 결정했는가?
```

그리고 그 결과가 그대로 Cursor 개발 Context로 전달되어야 한다.

이 단계가 완료되면 `tcf-ontology-service`는
단순 Architecture Knowledge 조회 시스템에서
**실제 신규 시스템 개발을 지원하는 Architecture Design Platform**으로 발전한다.
