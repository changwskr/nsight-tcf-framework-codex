# NSIGHT Architecture Design Wizard
## STEP 4 · Data / Table Design — 신규 테이블 제안(New Table Proposal) 화면 설계서

- 문서버전: v1.0
- 작성일: 2026-08-10
- 대상 시스템: `tcf-ontology-service`
- 대상 UI: `NSIGHT Architect Workbench > Architecture Design`
- 적용 단계: `STEP 4 · Data / Table Design`
- 기능명: **New Table Proposal**
- 목적: Ontology에 필요한 Table이 존재하지 않을 때 신규 Table 설계정보를 입력하고 Architecture Design Baseline에 `PROPOSED` 상태로 반영
- 핵심 원칙: **신규 Table을 Ontology의 기존 사실(VERIFIED)처럼 등록하지 않는다.**
- 최종 승인 주체: Architect / DA(DB Architect)

---

# 1. 도입 전 안내말

Architecture Design Wizard의 STEP 4에서는 우선 기존 Ontology에서 사용할 Table을 검색한다.

정상 흐름:

```text
Data Requirement
      ↓
Ontology Table Search
      ↓
Existing Table 발견
      ↓
Table / Column 선택
```

신규 업무에서 기존 Table이 없을 경우:

```text
Requirement
"고객 AI 추천 결과를 저장해야 한다."

Ontology Search
TB_AI_CUSTOMER_RECOMMEND
        ↓
검색 결과 없음
```

이 경우 존재하지 않는 Table을 임의로 생성하지 않고 다음 흐름을 따른다.

```text
NO_TABLE_FOUND
      ↓
[신규 Table 설계]
      ↓
NEW_TABLE_PROPOSAL
      ↓
Architect / DA Review
      ↓
APPROVED
      ↓
실제 DB 설계/DDL
      ↓
Metadata Scan
      ↓
VERIFIED
```

---

# 2. 문서 개요

## 2.1 목적

Cursor가 다음 기능을 구현할 수 있도록 기준을 정의한다.

1. 기존 Table 검색
2. 검색 결과 없음 처리
3. `신규 Table 설계` 버튼 제공
4. 신규 Table 기본정보 입력
5. Column 입력
6. Composite PK 입력
7. Index 입력
8. Join/Relation 입력
9. CRUD Access Type 설정
10. 개인정보/암호화/마스킹 설정
11. 데이터 규모/성능 속성 입력
12. 설계 Validation
13. `NEW_TABLE_PROPOSAL` 생성
14. Architecture Design Session 반영
15. Cursor Context Export
16. 실제 DB 생성 전까지 `PROPOSED` 유지

## 2.2 적용범위

```text
STEP 4 Data / Table Design
├─ Existing Table Search
├─ No Result
├─ New Table Proposal
│   ├─ Basic Information
│   ├─ Column Design
│   ├─ Primary Key
│   ├─ Index
│   ├─ Join / Relation
│   ├─ Access Type
│   ├─ Security
│   ├─ Capacity
│   └─ Validation
└─ Design Baseline 반영
```

제외:
- 실제 DB DDL 자동 실행
- 실제 Table 자동 생성
- Production DB 직접 변경
- DB 권한 자동부여
- DBA 승인 자동처리

---

# 3. 문제 정의 및 설계 배경

STEP 4에서 Table이 없다고 단순 `UNRESOLVED`로 끝내면 실제 개발설계가 진행되지 않는다.

신규 시스템에서는:

```text
신규 업무
   ↓
신규 데이터 필요
   ↓
기존 Table 없음
   ↓
신규 Table 설계 필요
```

가 정상적인 설계 시나리오다.

따라서 Architecture Design 도구는:

> "Table이 없습니다."

에서 끝나는 것이 아니라:

> "새 Table이 필요하므로 설계 Proposal을 작성하십시오."

까지 지원해야 한다.

---

# 4. 설계 원칙

1. 신규 Table은 `PROPOSED` 상태로 생성한다.
2. 실제 DB에 존재하지 않는 Table을 `VERIFIED`로 표시하지 않는다.
3. Table Name만 입력하고 완료하지 않는다.
4. 최소 Column/PK/용도를 입력해야 한다.
5. 미확정 정보는 `UNRESOLVED`.
6. 개인정보 여부를 알 수 없으면 `NO`가 아니라 `UNRESOLVED`.
7. 신규 Table Proposal은 기존 Ontology Concept와 구분한다.
8. DBA/DA 승인 전 실제 Table로 취급하지 않는다.
9. DDL 생성 기능은 후속 단계로 분리한다.
10. Architecture Design과 Data Architecture Design의 책임을 구분한다.

---

# 5. 전체 STEP 4 흐름

```text
STEP 4 Data/Table Design
          ↓
   Table Search
          ↓
   ┌──────┴──────┐
   │             │
 FOUND         NOT FOUND
   │             │
   ▼             ▼
기존 Table    신규 Table 필요?
선택             │
                 ├─ NO → UNRESOLVED
                 │
                 └─ YES
                     ↓
              New Table Proposal
                     ↓
              Table 기본정보
                     ↓
               Column 정의
                     ↓
                 PK 정의
                     ↓
              Index / Join
                     ↓
            Security / Capacity
                     ↓
              Validation
                     ↓
              PROPOSED
                     ↓
             Design Baseline
```

---

# 6. 기존 Table 검색 화면 변경

```text
┌─────────────────────────────────────────────────────────────────────┐
│ STEP 4 · Data / Table Design                                       │
├─────────────────────────────────────────────────────────────────────┤
│ Table Search                                                        │
│                                                                     │
│ [ 고객추천                                               ] [검색]  │
│                                                                     │
│ Search Result                                                       │
│                                                                     │
│ 검색 결과가 없습니다.                                              │
│ 필요한 Table이 Ontology에 등록되어 있지 않습니다.                 │
│                                                                     │
│ [다시 검색]       [UNRESOLVED로 유지]       [+ 신규 Table 설계]   │
└─────────────────────────────────────────────────────────────────────┘
```

---

# 7. 신규 Table 설계 진입

`[+ 신규 Table 설계]` 클릭 시 STEP 4 내부 Sub-Step으로 표시한다.

```text
4-1 Table Search
4-2 New Table Basic
4-3 Column Design
4-4 Key / Index
4-5 Relation
4-6 Security / Capacity
4-7 Review
```

---

# 8. 화면 4-2 — 신규 Table 기본정보

## 화면 ID

`ARC-DES-DATA-NEW-0001`

## Wireframe

```text
┌─────────────────────────────────────────────────────────────────────┐
│ New Table Proposal                                                  │
│ Status : PROPOSED                                                   │
├─────────────────────────────────────────────────────────────────────┤
│ 논리 테이블명 *                                                     │
│ [ 고객 AI 추천 결과 관리                                       ]  │
│                                                                     │
│ 물리 테이블명 *                                                     │
│ [ TB_MK_CO_A_AI_RECOMMEND                                      ]  │
│                                                                     │
│ Schema                                                              │
│ [ RDW ▼ ]                                                           │
│                                                                     │
│ System   [MG]    Business [CO]    Function [A]                     │
│                                                                     │
│ Table 유형                                                         │
│ [ MASTER ▼ ]                                                        │
│                                                                     │
│ 설명 *                                                              │
│ [ 고객별 AI 추천 결과 및 추천점수를 저장한다.                   ]  │
│ [                                                                 ] │
│                                                                     │
│ 데이터 생성주기 [ONLINE ▼]                                        │
│ 보존기간        [13] [개월 ▼]                                     │
│                                                                     │
│ [취소]                                         [다음: Column 설계] │
└─────────────────────────────────────────────────────────────────────┘
```

## 기본 속성

| 필드 | 타입 | 필수 | 예 |
|---|---|---:|---|
| logicalName | Text | Y | 고객 AI 추천 결과 관리 |
| physicalName | Text | Y | TB_MK_CO_A_AI_RECOMMEND |
| schema | Select/Text | Y | RDW |
| system | Select | Y | MG |
| business | Select | Y | CO |
| function | Select | Y | A |
| tableType | Select | Y | MASTER |
| description | Textarea | Y | 고객별 AI 추천 결과 저장 |
| creationType | Select | N | ONLINE |
| retentionValue | Number | N | 13 |
| retentionUnit | Select | N | MONTH |
| status | Hidden | Y | PROPOSED |

Table 유형:

```text
MASTER
DETAIL
TRANSACTION
HISTORY
LOG
CODE
MAPPING
TEMPORARY
SUMMARY
INTERFACE
ETC
```

---

# 9. Table Name 검증

```text
TABLE-NAME-001 빈값 금지
TABLE-NAME-002 허용문자
TABLE-NAME-003 최대 길이
TABLE-NAME-004 기존 Table 중복
TABLE-NAME-005 Proposal 중복
TABLE-NAME-006 금지 Prefix
```

동일 Table 존재 시:

```text
FAIL
TABLE_ALREADY_EXISTS
```

---

# 10. 화면 4-3 — Column Design

Column은 **행 추가 방식의 Text Box Grid**로 구현한다.

```text
┌───┬───────────────────┬──────────────────────┬────────────┬─────────┬──────┬──────┬─────────────┐
│ # │ 논리명            │ 물리 Column명        │ Data Type  │ Length  │ PK   │ Null │ Default     │
├───┼───────────────────┼──────────────────────┼────────────┼─────────┼──────┼──────┼─────────────┤
│ 1 │ [고객번호       ] │ [CUST_NO           ] │ [VARCHAR2▼]│ [20 ]   │ [✓]  │ [ ]  │ [          ] │
│ 2 │ [추천상품코드   ] │ [RECOMMEND_PRD_C   ] │ [VARCHAR2▼]│ [20 ]   │ [✓]  │ [ ]  │ [          ] │
│ 3 │ [추천점수       ] │ [RECOMMEND_SCORE   ] │ [NUMBER ▼] │ [10,4]  │ [ ]  │ [✓]  │ [          ] │
│ 4 │ [등록일시       ] │ [REG_DTM           ] │ [TIMESTAMP]│ [   ]   │ [ ]  │ [ ]  │ [SYSTIMESTAMP]│
└───┴───────────────────┴──────────────────────┴────────────┴─────────┴──────┴──────┴─────────────┘

[+ Column 추가]   [선택 삭제]   [Ontology Column 검색/참조]
```

Column 속성:

| 속성 | 필수 | 설명 |
|---|---:|---|
| logicalName | Y | 논리명 |
| physicalName | Y | 물리 Column |
| dataType | Y | VARCHAR2 / NUMBER / DATE 등 |
| length | 조건 | 길이 |
| precision | 조건 | 숫자 Precision |
| scale | 조건 | 숫자 Scale |
| primaryKey | N | PK |
| nullable | Y | NULL 허용 |
| defaultValue | N | Default |
| description | N | 설명 |
| personalData | Y | 개인정보 |
| encryption | Y | 암호화 |
| masking | Y | 마스킹 |
| role | N | SELECT/FILTER/JOIN/PAGING 등 |

Column Role:

```text
PRIMARY_KEY
SELECT
FILTER
JOIN
SORT
PAGING
INSERT
UPDATE
AUDIT
```

---

# 11. Composite PK

반드시 복합 PK를 지원한다.

```text
PK
├─ CUST_NO
└─ RECOMMEND_PRD_C
```

금지:

```text
"[CUST_NO, RECOMMEND_PRD_C]"
```

를 하나의 Column으로 저장.

---

# 12. Column Validation

```text
COL-001 논리명 필수
COL-002 물리명 필수
COL-003 물리명 중복 금지
COL-004 Data Type 필수
COL-005 VARCHAR/CHAR Length 필수
COL-006 NUMBER precision/scale 정합
COL-007 PK는 nullable=false
COL-008 개인정보 Unknown 허용
COL-009 암호화/마스킹 미결정은 UNRESOLVED
COL-010 Paging Key는 PK/Index 검토 Warning
```

---

# 13. 화면 4-4 — Key / Index Design

```text
Primary Key

PK Name       [ PK_TB_MK_CO_A_AI_RECOMMEND ]
Columns       [ CUST_NO ] [ RECOMMEND_PRD_C ]

Indexes

Index #1
Name          [ IDX_AI_RECOMMEND_01 ]
Type          [ NORMAL ▼ ]
Columns       [ CUST_NO ] [ REG_DTM ]
Unique        [ ]

[+ Index 추가]
```

Index 속성:

```text
indexName
indexType
columns
unique
purpose
status=PROPOSED
```

---

# 14. 화면 4-5 — Join / Relation Design

```text
Relation #1

Source Table       TB_MK_CO_A_AI_RECOMMEND
Source Column      [ CUST_NO ▼ ]

Relation Type      [ MANY_TO_ONE ▼ ]

Target Table       [ TB_CUSTOMER ] [검색]
Target Column      [ CUST_NO ▼ ]

FK 생성 여부       [ UNRESOLVED ▼ ]

[+ Relation 추가]
```

Relation Type:

```text
ONE_TO_ONE
ONE_TO_MANY
MANY_TO_ONE
MANY_TO_MANY
REFERENCE
LOGICAL_JOIN
```

---

# 15. 화면 4-6 — Access / Usage Design

```text
ServiceId         mgcoa7777S0

Table             TB_MK_CO_A_AI_RECOMMEND

Access Type       [ READ ▼ ]

Filter Columns
☑ CUST_NO
□ RECOMMEND_PRD_C

Select Columns
☑ CUST_NO
☑ RECOMMEND_PRD_C
☑ RECOMMEND_SCORE

Sort
REG_DTM DESC

Paging             YES
Paging Key         CUST_NO
```

Access Type:

```text
READ
CREATE
UPDATE
DELETE
READ_WRITE
```

---

# 16. 화면 4-7 — Security / Privacy Design

```text
Column               개인정보      Encryption      Masking

CUST_NO              [YES ▼]       [UNRESOLVED▼]   [YES ▼]
RECOMMEND_PRD_C      [NO ▼]        [NO ▼]           [NO ▼]
RECOMMEND_SCORE      [NO ▼]        [NO ▼]           [NO ▼]
REG_DTM              [NO ▼]        [NO ▼]           [NO ▼]

⚠ UNRESOLVED : CUST_NO Encryption 정책
```

상태:

```text
YES
NO
UNRESOLVED
NOT_APPLICABLE
```

---

# 17. Capacity / Performance

```text
예상 초기건수        [100000]
일 증가건수          [10000]
보존기간             [13개월]
예상 Row Size         [UNRESOLVED]
대용량 여부           [YES]
Partition 필요        [UNRESOLVED]
Archive 필요          [UNRESOLVED]
```

---

# 18. Review 화면

```text
New Table Proposal Review

Table
TB_MK_CO_A_AI_RECOMMEND
고객 AI 추천 결과 관리

Status
PROPOSED

Columns        4
PK             CUST_NO + RECOMMEND_PRD_C
Index          1
Relations      1
Access         READ
Personal Data  YES

Validation
✅ Table Name
✅ Column
✅ PK
✅ Duplicate
⚠ Encryption UNRESOLVED

Result
PASS_WITH_UNRESOLVED

[이전] [Draft 저장] [신규 Table Proposal 확정]
```

---

# 19. Proposal 상태 모델

```text
DRAFT
PROPOSED
REVIEW_REQUIRED
APPROVED
REJECTED
IMPLEMENTED
VERIFIED
```

Lifecycle:

```text
DRAFT
  ↓
PROPOSED
  ↓
REVIEW_REQUIRED
  ↓
APPROVED
  ↓
DB IMPLEMENTED
  ↓
Metadata Scan
  ↓
VERIFIED
```

---

# 20. Ontology 상태 분리

신규 설계:

```text
Table Concept
status = PROPOSED
```

실제 DB/Metadata 검증 후:

```text
status = VERIFIED
```

절대 금지:

```text
사용자 Text Box 입력
→ VERIFIED
```

---

# 21. Design Session 예

```json
{
  "designId": "DES-20260810-001",
  "serviceId": "mgcoa7777S0",
  "dataDesign": {
    "tables": [
      {
        "mode": "NEW_TABLE_PROPOSAL",
        "status": "PROPOSED",
        "logicalName": "고객 AI 추천 결과 관리",
        "physicalName": "TB_MK_CO_A_AI_RECOMMEND",
        "schema": "RDW",
        "accessType": "READ",
        "columns": [
          {
            "logicalName": "고객번호",
            "physicalName": "CUST_NO",
            "dataType": "VARCHAR2",
            "length": 20,
            "primaryKey": true,
            "nullable": false,
            "personalData": "YES",
            "encryption": "UNRESOLVED",
            "masking": "YES"
          }
        ]
      }
    ]
  }
}
```

---

# 22. Backend API 설계

```http
GET  /api/ontology/design/tables
POST /api/ontology/design/table-proposal/validate
POST /api/ontology/design/table-proposal
GET  /api/ontology/design/table-proposal/{proposalId}
PUT  /api/ontology/design/table-proposal/{proposalId}
```

기존 Design Session 저장 구조가 있다면 우선 재사용한다.

---

# 23. Gate Rule

Table:

```text
DATA-TBL-001 Table Name 필수
DATA-TBL-002 Table Name 형식
DATA-TBL-003 Existing Table 중복 금지
DATA-TBL-004 Proposal 중복 금지
DATA-TBL-005 논리명 필수
DATA-TBL-006 업무분류 필수
```

Column:

```text
DATA-COL-001 Column 최소 1개
DATA-COL-002 Column 물리명 필수
DATA-COL-003 Column 중복 금지
DATA-COL-004 Data Type 필수
DATA-COL-005 Type/Length 정합
```

PK:

```text
DATA-PK-001 PK 존재 정책
DATA-PK-002 PK Column 존재
DATA-PK-003 PK nullable=false
DATA-PK-004 Composite PK 지원
```

Security:

```text
DATA-SEC-001 개인정보 여부 결정
DATA-SEC-002 암호화 정책
DATA-SEC-003 마스킹 정책
```

---

# 24. 정상 처리 흐름

```text
Table Search
        ↓
NOT FOUND
        ↓
+ 신규 Table 설계
        ↓
기본정보 입력
        ↓
Column 입력
        ↓
Composite PK
        ↓
Index / Join
        ↓
Security / Capacity
        ↓
Validation
        ↓
PASS_WITH_UNRESOLVED
        ↓
PROPOSED
        ↓
Architecture Baseline
```

---

# 25. 오류 처리

기존 Table과 중복:

```text
FAIL
TABLE_ALREADY_EXISTS
```

Column 0건:

```text
FAIL
AT_LEAST_ONE_COLUMN_REQUIRED
```

PK 참조 Column 삭제 시:

```text
PK_REFERENCE_EXISTS
```

---

# 26. Cursor Context Export

```markdown
## Data Design

### NEW_TABLE_PROPOSAL

Status: PROPOSED

Logical Name:
고객 AI 추천 결과 관리

Physical Name:
TB_MK_CO_A_AI_RECOMMEND

Schema:
RDW

Access:
READ

### Columns

| Column | Type | PK | Nullable | Personal Data |
|---|---|---:|---:|---|
| CUST_NO | VARCHAR2(20) | Y | N | YES |
| RECOMMEND_PRD_C | VARCHAR2(20) | Y | N | NO |
| RECOMMEND_SCORE | NUMBER(10,4) | N | Y | NO |
| REG_DTM | TIMESTAMP | N | N | NO |

### Primary Key

CUST_NO
RECOMMEND_PRD_C

### Unresolved

- CUST_NO Encryption Policy
- Partition Policy

IMPORTANT:

이 Table은 아직 실제 DB에 존재하는 VERIFIED Table이 아니다.
Architecture/Data Design 단계의 PROPOSED Table이다.
DDL 생성 또는 실제 DB 반영 전에 DA/DBA 승인을 받아야 한다.
```

---

# 27. 금지 예시

- Table명만 입력하고 즉시 VERIFIED 등록 금지
- Column 0건 Proposal 완료 금지
- 개인정보 미입력을 `NO`로 자동처리 금지
- Workbench가 실제 Oracle에 CREATE TABLE 실행 금지
- 신규 Proposal을 기존 Ontology의 VERIFIED Table처럼 검색결과에 노출 금지

---

# 28. RACI

| 업무 | App Architect | DA/DBA | Workbench | Ontology |
|---|---|---|---|---|
| 신규 Table 필요 판단 | A/R | C | C | C |
| 논리 Table 설계 | A/R | C | R | - |
| 물리명 | C | A/R | R | C |
| Column | R | A | R | C |
| PK | C | A/R | R | C |
| Index | C | A/R | R | C |
| 개인정보 | C | A/R | R | C |
| Proposal 승인 | C | A/R | C | - |
| 실제 DDL | C | A/R | - | - |
| DB Scan 후 VERIFIED | C | C | C | A/R |

---

# 29. 테스트 시나리오

| TC | 시나리오 | 기대 |
|---|---|---|
| NEW-TBL-001 | Table 검색 없음 | 신규 설계 버튼 |
| NEW-TBL-002 | 기본정보 | PROPOSED |
| NEW-TBL-003 | Table 중복 | FAIL |
| NEW-TBL-004 | Column 0건 | FAIL |
| NEW-TBL-005 | Column 추가 | PASS |
| NEW-TBL-006 | Composite PK | 별도 Column 유지 |
| NEW-TBL-007 | PK nullable | FAIL/자동정합 |
| NEW-TBL-008 | 개인정보 미결정 | UNRESOLVED |
| NEW-TBL-009 | Index Multi Column | 순서 유지 |
| NEW-TBL-010 | Existing Table Join | Relation |
| NEW-TBL-011 | Cursor Export | PROPOSED 명시 |
| NEW-TBL-012 | undefined | 없음 |
| NEW-TBL-013 | DB 미존재 | VERIFIED 금지 |
| NEW-TBL-014 | Gate | PASS_WITH_UNRESOLVED |

---

# 30. Golden Scenario

Requirement:

```text
고객별 AI 추천 결과 저장
```

Search:

```text
TB_MK_CO_A_AI_RECOMMEND
```

Result:

```text
NOT FOUND
```

Proposal:

```text
Logical: 고객 AI 추천 결과 관리
Physical: TB_MK_CO_A_AI_RECOMMEND
```

Columns:

```text
CUST_NO             VARCHAR2(20) PK
RECOMMEND_PRD_C     VARCHAR2(20) PK
RECOMMEND_SCORE     NUMBER(10,4)
REG_DTM             TIMESTAMP
```

Result:

```text
NEW_TABLE_PROPOSAL
status = PROPOSED

Gate
PASS_WITH_UNRESOLVED
```

---

# 31. 구현 순서

Phase 0:
`NEW-TABLE-PROPOSAL-GAP-ANALYSIS.md`

Phase 1:
- No Result
- 신규 Table 설계
- Basic Info
- Duplicate Validation

Phase 2:
- Column Grid
- Composite PK
- Type Validation

Phase 3:
- Index
- Relation
- Privacy
- Capacity

Phase 4:
- Validation
- PROPOSED
- Baseline
- Cursor Export
- Regression

---

# 32. Cursor 최종 구현 지시

```text
너는 NSIGHT/PDMG tcf-ontology-service의
Application Architect + Data Architecture 구현 담당자다.

Architecture Design Wizard의
STEP 4 · Data / Table Design
을 확장하라.

기존 Ontology에서 필요한 Table을 찾지 못한 경우
단순 UNRESOLVED로 끝내지 말고:

[+ 신규 Table 설계]

기능을 제공한다.

신규 Table은 반드시:

NEW_TABLE_PROPOSAL

상태이며 실제 DB Table처럼 VERIFIED 처리하지 않는다.

먼저 현재 Source를 분석하고:

NEW-TABLE-PROPOSAL-GAP-ANALYSIS.md

를 작성하라.

분석 후 다음을 구현한다.

1. Table Search 결과 없음 UI
2. 신규 Table 설계 버튼
3. Table 기본정보 Text Box
4. 논리/물리 Table명
5. Schema
6. 업무분류
7. Table Type
8. Description
9. Column 행 추가 Grid
10. Column 논리명/물리명
11. Data Type/Length/Precision/Scale
12. Nullable
13. Default
14. Composite PK
15. Index
16. Join/Relation
17. READ/CREATE/UPDATE/DELETE
18. Filter/Select/Update/Sort/Paging Column
19. 개인정보
20. 암호화
21. 마스킹
22. 예상 건수/보존기간
23. Validation
24. Proposal Review
25. Design Baseline 반영
26. Cursor Markdown/JSON Export

중요:

- Table명만 입력해서 완료시키지 마라.
- Column 0건 Proposal은 FAIL.
- List PK를 하나의 String Column으로 만들지 마라.
- 실제 DB에 없는 Table을 VERIFIED로 만들지 마라.
- 개인정보 미확인은 NO가 아니라 UNRESOLVED.
- Table/Column을 일반 LLM 지식으로 임의 생성하지 마라.
- 실제 Oracle CREATE TABLE을 실행하지 마라.

상태:

DRAFT
PROPOSED
REVIEW_REQUIRED
APPROVED
IMPLEMENTED
VERIFIED

를 구분한다.

Golden Scenario:

"고객별 AI 추천 결과 저장"

신규 Table 필요 상황을 사용한다.

구현 완료 후:

NEW-TABLE-PROPOSAL-IMPLEMENTATION-REPORT.md

를 작성한다.

보고서에는:

- 변경 파일
- 화면
- Component
- API
- Table Validation
- Column Validation
- Composite PK
- Index
- Relation
- Security
- Design Gate
- Cursor Export
- Test
- Regression
- 남은 Gap

을 포함한다.
```

---

# 33. 체크리스트

- [ ] Table No Result 처리
- [ ] 신규 Table 설계 버튼
- [ ] Basic Info Text Box
- [ ] 논리명/물리명
- [ ] 중복검사
- [ ] Column Grid
- [ ] Column 행 추가/삭제
- [ ] Data Type
- [ ] Length/Precision/Scale
- [ ] PK/Composite PK
- [ ] Nullable/Default
- [ ] Index
- [ ] Join
- [ ] Access
- [ ] Personal Data
- [ ] Encryption
- [ ] Masking
- [ ] Capacity
- [ ] Proposal Status
- [ ] Validation
- [ ] Gate
- [ ] Markdown/JSON Export
- [ ] VERIFIED 금지
- [ ] Regression

---

# 34. 시사점

기존 Table이 없는 것은 오류가 아니다. 신규 시스템 구축에서는 정상적인 설계 상황이다.

상태는 반드시 구분한다.

```text
기존 DB/Table + Source 확인
→ VERIFIED

문서/YAML에서 발견
→ DISCOVERED

신규 설계
→ PROPOSED

정보 미결정
→ UNRESOLVED
```

향후:

```text
New Table Proposal
      ↓
DA Review
      ↓
Approved Physical Model
      ↓
DDL Generator
      ↓
DB Deployment
      ↓
DB Metadata Scanner
      ↓
Ontology VERIFIED
```

까지 확장할 수 있다.

---

# 35. 마무리말

STEP 4의 목표는 단순히 Table명을 입력하는 것이 아니다.

신규 Table이 필요하면 다음까지 설계해야 한다.

```text
왜 필요한가?
무슨 데이터를 저장하는가?
어떤 Column이 있는가?
PK는 무엇인가?
어떤 Table과 연결되는가?
어떻게 읽고/쓰는가?
개인정보는 있는가?
성능/보존정책은 무엇인가?
```

이 결과가 Architecture Design Baseline과 Cursor Context로 이어져야 실제 개발 가능한 Data Design이 완성된다.
