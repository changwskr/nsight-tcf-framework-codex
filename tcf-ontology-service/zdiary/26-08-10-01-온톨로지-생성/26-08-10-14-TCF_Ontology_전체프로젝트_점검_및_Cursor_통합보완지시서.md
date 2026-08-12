# TCF Ontology Service 전체 프로젝트 점검 및 Cursor 통합 보완 지시서
## NSIGHT Architecture Intelligence / Architect Workbench / Design Assistant

- 문서버전: v1.0
- 점검일: 2026-08-10
- 대상 프로젝트: `tcf-ontology-service`
- 입력 기준: 전체 프로젝트 압축본 `tcf-ontology-service.zip`
- 역할: NSIGHT/PDMG 수석 애플리케이션 아키텍트 관점 전체 점검
- 목적: 현재 프로젝트를 **신규 시스템 구축 시 재사용 가능한 Architecture Knowledge Service** 수준으로 안정화하기 위한 Cursor 실행 지시
- 중요 원칙: **새 기능 확대보다 정합성·증거성·배포 가능성·재사용성을 먼저 확보한다.**

---

# 1. 도입 전 안내말

`tcf-ontology-service`는 현재 단순 프로토타입을 넘어 다음 구성요소를 이미 가지고 있다.

```text
Ontology Core
├─ Concept / Relation
├─ ServiceId Parser
├─ YAML Graph Loader
├─ Runtime Graph Loader
├─ Query
├─ Impact
├─ Validation
├─ Provenance
├─ Recommend
├─ Prompt Context Export
└─ Source Scan / Seed

Architect Workbench
├─ Home
├─ Search
├─ Impact
├─ Gate
└─ Architecture Design Assistant
```

전체 방향은 적절하다.

그러나 **현재 상태를 바로 "신규 시스템 구축에 사용할 수 있는 Architecture Intelligence Platform 완성"으로 판단해서는 안 된다.**

이번 전체 프로젝트 점검에서 다음 종류의 문제가 확인되었다.

1. 현재 WAR 산출물이 최신 소스와 일치하지 않음
2. 외부 Tomcat WAR 배포 시 Workbench API 경로가 깨질 수 있음
3. 복합 PK가 하나의 가짜 Column으로 만들어짐
4. 다중 Table 프로그램이 하나의 가짜 Table 문자열로 만들어질 수 있음
5. Design Assistant가 요청한 거래유형과 다른 ServiceId를 후보 기준으로 사용할 수 있음
6. Paging/Message/Transaction 추천 중 일부가 실제 Ontology Evidence가 아니라 UI 코드에서 생성됨
7. Architecture Gate가 표방하는 검증 범위와 실제 Rule 구현 범위가 다름
8. Provenance VERIFIED의 신뢰수준이 과도함
9. Registry와 Graph Store의 reload 일관성이 없음
10. 현재 테스트 증거는 전체 테스트가 아니라 일부 타깃 테스트 결과임

따라서 다음 단계는 **Design Assistant 기능 확대가 아니라 Release Readiness + Knowledge Integrity 보완**이다.

---

# 2. 문서 개요

## 2.1 목적

본 문서는 Cursor에게 다음 작업을 순차적으로 수행하도록 지시한다.

```text
전체 프로젝트 재분석
      ↓
P0 데이터/기능 정합성 수정
      ↓
WAR/Context Path 배포 정합성 확보
      ↓
Recommendation Evidence 강화
      ↓
Architecture Gate 의미 정합화
      ↓
Provenance 신뢰등급 개선
      ↓
전체 Regression Test
      ↓
최신 WAR 재생성
      ↓
Release Acceptance
```

---

## 2.2 적용범위

다음 전체 범위를 포함한다.

- Gradle / WAR
- Configuration
- Ontology YAML
- Concept / Relation Domain
- OntologyStore
- YAML Loader
- Runtime Loader
- Registry
- Scanner / Seed
- Query
- Impact
- Validation
- Recommend
- Prompt Context
- REST API
- Architect Workbench
- Architecture Design Assistant
- Test
- 배포
- 운영
- 보안
- 버전/기준본 관리

---

## 2.3 대상 독자

- Cursor
- NSIGHT/PDMG Application Architect
- Framework Architect
- 개발 PL
- QA
- DevOps
- Ontology 운영 담당자

---

## 2.4 점검 환경 및 한계

전체 프로젝트 압축본을 실제로 해제하여 소스·리소스·테스트 산출물·WAR를 점검하였다.

프로젝트 규모:

```text
전체 파일              : 307
src/main Java          : 36
src/test Java          : 23
Ontology YAML          : 19
Workbench JavaScript   : 3
```

현재 압축본 내부의 기존 테스트 결과에는 다음 6개 Suite가 존재하며 모두 failures=0이다.

```text
ReverseImpactUnitTest
ArchitectureRuleNegativeCasesTest
ArchitectureRuleValidationTest
PdmgValidationIT
TableImpactQueryTest
WorkbenchStaticUiTest
```

그러나 전체 테스트 클래스는 23개이므로,
**현재 압축본의 test-results만으로 전체 Regression PASS를 선언해서는 안 된다.**

점검 환경에서 Gradle Wrapper 실행을 시도했으나,
Gradle 8.10.1 배포본 다운로드가 외부망 제한으로 실패하였다.

따라서 Cursor가 사용하는 실제 개발환경에서 반드시:

```text
gradlew.bat clean test war
```

를 다시 실행하여 최종 증거를 확보해야 한다.

---

## 2.5 용어 정의

| 용어 | 정의 |
|---|---|
| Knowledge Integrity | Ontology에 저장된 개념/관계가 실제 Source/Evidence와 일치하는 상태 |
| Release Integrity | Source, Test, WAR가 동일한 최신 Baseline을 나타내는 상태 |
| Provenance | Ontology 정보가 어디에서 발견·검증되었는지 나타내는 근거 |
| DERIVED | 직접 확인이 아니라 기존 근거에서 유도한 값 |
| VERIFIED | 실제 Source 또는 공식 정본에서 검증된 상태 |
| UNRESOLVED | 현재 Ontology 근거로 결정할 수 없는 값 |
| Design-Time Gate | 구현 전 설계 Baseline을 검증하는 Gate |
| Implementation Gate | 실제 Source 구현 결과를 검증하는 Gate |
| Context Path | 외부 Tomcat에서 WAR가 배포되는 Web Application 경로 |

---

# 3. 전체 프로젝트 현재 구조

현재 프로젝트의 논리 구조는 다음과 같이 평가한다.

```text
tcf-ontology-service
│
├─ ontology/
│   ├─ core
│   ├─ business
│   ├─ technical
│   ├─ mappings
│   ├─ shapes
│   ├─ rules
│   └─ versions
│
├─ src/main/java/nhnis/ontology
│   ├─ domain
│   ├─ loader
│   ├─ store
│   ├─ query
│   ├─ impact
│   ├─ validate
│   ├─ recommend
│   ├─ scan
│   ├─ seed
│   ├─ prompt
│   └─ web
│
├─ src/main/resources/static/workbench
│   ├─ index.html
│   ├─ css/workbench.css
│   └─ js/
│       ├─ api.js
│       ├─ app.js
│       └─ design.js
│
├─ src/test
├─ test-data
├─ docs
├─ scripts
└─ build
```

전체 아키텍처 방향 자체는 유지할 가치가 있다.

---

# 4. 종합 평가

## 4.1 점수

| 영역 | 평가 |
|---|---:|
| Ontology Core 모델 | 90 |
| ServiceId | 95 |
| Design/Runtime Graph 구분 | 90 |
| Query | 82 |
| Impact | 80 |
| Provenance 구조 | 80 |
| Architecture Gate | 75 |
| Recommend | 60 |
| Workbench 1차 | 85 |
| Design Assistant | 60 |
| Test 체계 | 72 |
| WAR/배포 정합성 | 45 |
| 운영/보안 준비 | 55 |
| 신규 시스템 재사용 준비도 | 68 |

현재 판정:

> **PILOT / PARTIAL READY**

아직 Production Architecture Knowledge Platform으로 승인하지 않는다.

---

# 5. P0 — 반드시 먼저 수정할 항목

---

# 5.1 P0-01 최신 소스와 WAR 불일치

## 문제

압축본 내부의 WAR:

```text
build/libs/tcf-ontology-service.war
```

은 Workbench/Design Assistant 및 최근 P0 수정 소스보다 이전에 생성되었다.

WAR 내부를 확인한 결과:

```text
WEB-INF/classes/static/workbench
```

항목이 존재하지 않는다.

즉 현재 WAR를 Tomcat에 배포하면
현재 소스에 존재하는 Architect Workbench가 배포되지 않는다.

## 위험

```text
Source       최신
Test 일부    최신
WAR          구버전

→ 운영 배포물이 Source와 다름
```

이는 Release Gate 실패다.

## Cursor 지시

최종 모든 수정 후 반드시:

```bat
gradlew.bat clean test war
```

실행.

그리고:

```bat
jar tf build\libs\tcf-ontology-service.war
```

또는:

```bat
unzip -l build/libs/tcf-ontology-service.war
```

로 다음 파일 존재를 확인한다.

```text
WEB-INF/classes/static/workbench/index.html
WEB-INF/classes/static/workbench/css/workbench.css
WEB-INF/classes/static/workbench/js/api.js
WEB-INF/classes/static/workbench/js/app.js
WEB-INF/classes/static/workbench/js/design.js
```

### Acceptance

- 최신 Workbench 포함
- 최신 Java class 포함
- 전체 Test PASS 이후 생성된 WAR
- WAR timestamp가 최종 Source 수정 이후

---

# 5.2 P0-02 WAR Context Path에서 API 호출 깨짐

## 문제

Workbench `api.js`가 API를 다음처럼 절대경로로 호출한다.

```javascript
fetch("/api/ontology/...")
```

BootRun에서는 Context Root `/` 이므로 정상이다.

그러나 외부 Tomcat에:

```text
tcf-ontology-service.war
```

로 배포하면 일반적인 Context Path는:

```text
/tcf-ontology-service
```

가 된다.

UI:

```text
/tcf-ontology-service/workbench/index.html
```

에서 API를 호출해야 하는 주소는:

```text
/tcf-ontology-service/api/ontology/...
```

이다.

현재 코드는:

```text
/api/ontology/...
```

를 호출하므로 외부 Tomcat에서 404가 발생할 수 있다.

## 목표

Workbench는 다음 두 환경 모두에서 동작해야 한다.

```text
BootRun
http://localhost:8098/workbench/index.html

External Tomcat
http://host:port/tcf-ontology-service/workbench/index.html
```

## 권장 구현

`api.js`에 Context Path resolver를 둔다.

예:

```javascript
function resolveContextPath() {
  const marker = "/workbench/";
  const path = window.location.pathname;
  const idx = path.indexOf(marker);
  return idx >= 0 ? path.substring(0, idx) : "";
}

const APP_CONTEXT = resolveContextPath();

function apiUrl(path) {
  return `${APP_CONTEXT}${path}`;
}
```

모든 fetch는:

```javascript
fetch(apiUrl(path))
```

를 사용한다.

`promptMarkdown()`도 동일하게 적용한다.

## HealthController

현재:

```text
workbench = /workbench/index.html
```

역시 외부 Context Path에서 오해를 만들 수 있다.

가능하면 상대 경로 또는 요청 Context를 반영한다.

## 테스트

### Boot Root

```text
server.servlet.context-path=
```

### Simulated WAR Context

```text
server.servlet.context-path=/tcf-ontology-service
```

둘 모두에서:

```text
/workbench/index.html
/api/ontology/catalog
#/search
#/impact
#/gate
#/design
```

연계 성공 확인.

---

# 5.3 P0-03 Composite PK가 가짜 Column 1개로 생성됨

## 실제 확인

일부 Mapping YAML의 `pk`가 List이다.

예:

```yaml
pk:
  - L5101
  - L5103
```

현재 `YamlGraphLoader`는 이를 문자열화하여:

```text
"[L5101, L5103]"
```

라는 Column 하나로 생성할 수 있다.

실제 Design Assistant 구조/Context에서 다음과 같은 현상이 확인된다.

```text
TB_MK_CO_A_5530
  → [L5101, L5103]
```

이는 잘못된 Ontology다.

## 올바른 결과

```text
TB_MK_CO_A_5530
  ├─ HAS_COLUMN → L5101
  └─ HAS_COLUMN → L5103
```

## Cursor 지시

`YamlGraphLoader`에 scalar/list normalization utility를 추가한다.

예:

```java
List<String> normalizeStringList(Object value)
```

다음 모두 지원:

```yaml
pk: GUID
```

```yaml
pk:
  - L5101
  - L5103
```

각 PK를 별도 Column Concept로 생성한다.

## 테스트

최소:

```text
mgcoa5530
mgcoa9999
```

Composite PK Golden Test 추가.

금지:

```text
column.name = "[L5101, L5103]"
```

---

# 5.4 P0-04 Multi-Table Mapping 손상 가능성

## 문제

`MappingSeedGenerator`는 SQL에서 여러 Table을 발견하면 `data.table`에 List를 저장할 수 있다.

반면 `YamlGraphLoader`는 이를 String으로 읽는다.

결과:

```yaml
table:
  - TB_A
  - TB_B
```

가:

```text
"[TB_A, TB_B]"
```

라는 가짜 Table Concept 하나로 만들어질 수 있다.

## 목표

Ontology는 반드시:

```text
Mapper
 ├─ ACCESSES → TB_A
 └─ ACCESSES → TB_B
```

또는 SQL 단위 Evidence가 있다면:

```text
SQL-1 → TB_A
SQL-2 → TB_B
```

로 표현해야 한다.

## 1차 호환 구현

기존 YAML 호환을 위해:

```text
data.table scalar
data.table list
```

둘 다 지원한다.

새 정규형은 가능하면:

```yaml
data:
  tables:
    - TB_A
    - TB_B
```

로 이동한다.

## 더 좋은 2차 구조

```yaml
data:
  accesses:
    - sqlId: selectA
      mode: READ
      tables:
        - TB_A
    - sqlId: updateB
      mode: WRITE
      tables:
        - TB_B
```

## 중요

실제 SQL별 Table 관계를 확인할 수 없다면
모든 SQL ID를 모든 Table에 임의 연결하지 않는다.

그 경우:

```text
Mapper ACCESSES Table
```

까지만 VERIFIED로 표시하고:

```text
SqlId ACCESSES Table
```

은 UNRESOLVED 또는 INFERRED로 둔다.

---

# 5.5 P0-05 Design Assistant Transaction Type 매핑 오류

## 현재 문제

`design.js`의 `intentFromTx()`는 사실상:

```text
QUERY  → query
DELETE → delete
CRUD   → crud
기타   → query
```

이다.

따라서:

```text
UPDATE
```

가 `query`로 검색된다.

또한 현재 UI 거래유형이:

```text
QUERY
CRUD
DELETE
UPDATE
```

만 제공된다.

PDMG ServiceId Operation은:

```text
S 조회
C 등록
U 수정
D 삭제
A 혼합
R 리포트
```

이다.

## 목표 표준

Design UI:

```text
QUERY
CREATE
UPDATE
DELETE
MIXED
REPORT
```

Backend Operation:

```text
QUERY  → S
CREATE → C
UPDATE → U
DELETE → D
MIXED  → A
REPORT → R
```

`CRUD`는 거래종류가 아니라 UI shorthand라면 제거하거나 MIXED로 명확히 정의한다.

## 테스트

예:

```text
mgcoa9000S0
mgcoa9000C0
mgcoa9000U0
mgcoa9000D0
mgcoa8888D0
```

각 거래유형이 올바른 Operation 후보를 선택하는지 검증.

---

# 5.6 P0-06 primaryServiceId가 무조건 S를 우선함

## 문제

현재:

```javascript
services.find(op === "S") || services[0]
```

으로 ServiceId를 선택한다.

즉 사용자가 DELETE 또는 UPDATE를 요청해도
후보 Program에 S가 있으면 S를 선택한다.

이는 Design Assistant의 핵심 기능 오류다.

## 수정

```javascript
primaryServiceId(rec, requestedOperation)
```

형식으로 변경한다.

예:

```text
QUERY  → S
CREATE → C
UPDATE → U
DELETE → D
MIXED  → A
REPORT → R
```

해당 Operation이 없는 Candidate는:

```text
operationMatch = false
```

로 표시하거나 추천 순위를 낮춘다.

절대로 다른 Operation을 “대표 ServiceId”로 조용히 사용하지 않는다.

---

# 5.7 P0-07 Pattern이 Evidence 없이 UI에서 만들어짐

## 현재 문제

`derivePattern()`은 다음 내용을 UI 코드에서 생성한다.

```text
ONLINE_PAGING_QUERY
hdr_nhnis + dto
TCF ON + Timeout
Paging YES
```

그러나 일부 값은 Candidate Ontology에서 실제 확인하지 않고
사용자 입력 또는 하드코딩 문자열을 기반으로 만든다.

예:

```text
Paging = YES
```

라는 Requirement만으로:

```text
ONLINE_PAGING_QUERY
```

를 만든다.

또:

```text
TCF ON + Timeout — 후보 architecture.runtimeRef 근거
```

라고 표현하지만,
현재 Recommend 응답에서 해당 근거를 실제로 조회·검증하지 않는다.

## 위험

이것은 아키텍트 화면에서 가장 피해야 할 형태다.

```text
UI 추측
→ Architecture Recommendation
→ VERIFIED처럼 보임
```

## 목표

추천 로직은 Browser JavaScript가 아니라 Backend Application Service에 둔다.

권장 신규 Use Case:

```text
DesignRecommendationService
```

권장 API:

```http
POST /api/ontology/design/recommend
```

Request:

```json
{
  "system": "MG",
  "business": "CO",
  "function": "A",
  "transactionType": "QUERY",
  "channel": "WEB",
  "dbAccess": true,
  "externalCall": false,
  "largeData": true,
  "paging": true,
  "timeoutPolicy": "DEFAULT",
  "personalData": "UNKNOWN",
  "requirement": "..."
}
```

Response:

```json
{
  "candidates": [],
  "pattern": {},
  "baseline": {},
  "evidence": [],
  "unresolved": [],
  "designGate": {}
}
```

## 필드별 Evidence

Pattern 전체뿐 아니라 각 Recommendation Property에 상태가 필요하다.

예:

```json
{
  "property": "messageEnvelope",
  "value": "hdr_nhnis + dto",
  "status": "VERIFIED",
  "evidence": [...]
}
```

또는:

```json
{
  "property": "paging",
  "value": "DB_OFFSET",
  "status": "INFERRED",
  "evidence": [...]
}
```

근거가 없다면:

```json
{
  "value": "UNRESOLVED",
  "status": "UNRESOLVED"
}
```

---

# 5.8 P0-08 Gate 의미와 실제 검증 의미 불일치

## 문제 1 — 현재 Gate Scope

Design Assistant는:

```text
OntologyApi.validateRules()
```

를 호출한다.

이는 전체 Graph 검증이다.

하지만 화면 사용자는 특정 신규 설계 또는 Candidate가 검증됐다고 이해할 수 있다.

## 문제 2 — RULE-003

RULE-003은 “Handler ServiceId 등록 일치”처럼 보이지만
실제로는 Ontology Relation을 검사하며
실제 Java Handler의 `serviceIds()`와 직접 비교하지 않는다.

즉:

```text
Architecture Gate PASS
```

가 실제 Source Registration PASS와 동일하지 않다.

## 목표 Gate 계층

다음 3종으로 분리한다.

### GLOBAL

```text
전체 Ontology integrity
```

### SERVICE

```text
특정 ServiceId + 실제 Source consistency
```

### DESIGN

```text
아직 구현되지 않은 Architecture Baseline
```

## API 제안

```http
GET /api/ontology/validate/rules
GET /api/ontology/validate/service/{serviceId}
POST /api/ontology/validate/design
```

## Design-Time 상태

```text
PASS
FAIL
UNRESOLVED
NOT_YET_IMPLEMENTED
NOT_APPLICABLE
```

## Rule 메타

각 Rule에:

```text
ruleId
name
scope
implemented
executable
sourceBacked
severity
```

를 관리한다.

YAML에 정의되어 있으나 Java validator가 실행하지 않는 Rule은
UI에서 “Active Rule”처럼 표시하지 않는다.

---

# 5.9 P0-09 최신 전체 Regression + WAR Acceptance 필요

현재 test-results에는 6개 Suite만 존재한다.

하지만 실제 Test Java는 23개다.

최종 Acceptance 전에 반드시:

```bat
gradlew.bat clean test
```

전체 실행.

그 후:

```bat
gradlew.bat war
```

실행.

## 결과 문서

```text
build/reports/tests/test/index.html
```

뿐 아니라 Cursor 보고서에:

```text
Total Test Classes
Total Tests
Failures
Errors
Skipped
```

를 기록한다.

---

# 6. P1 — 다음으로 수정할 항목

---

# 6.1 P1-01 Provenance VERIFIED 정책

## 문제

`YamlGraphLoader`가 FQCN에서 Java 경로를 추측한 후:

```text
SOURCE_CODE
VERIFIED
```

로 설정할 수 있다.

실제 파일 존재를 확인하지 않았는데 VERIFIED는 과도하다.

## 정책

### YAML만 있음

```text
sourceType = YAML_MAPPING
verificationStatus = DISCOVERED
```

### Scanner로 실제 Source 확인

```text
sourceType = SOURCE_CODE
verificationStatus = VERIFIED
```

### 사람이 승인

```text
verificationStatus = APPROVED
```

### Parser 추론

```text
verificationStatus = INFERRED
```

---

# 6.2 P1-02 classificationPath가 가상 Relation 생성

ServiceId 문자열을 파싱하여:

```text
MG → CO → A → Program → ServiceId
```

를 반환하지만,
실제 Graph Relation 존재 여부를 확인하지 않는다.

## 수정

우선 실제 Relation 탐색:

```text
HAS_BUSINESS
HAS_FUNCTION
HAS_PROGRAM
PROVIDES_SERVICE
```

성공 시 VERIFIED.

파싱으로만 유도하면:

```text
status = INFERRED
```

로 명확히 구분.

---

# 6.3 P1-03 summarizeStructure findFirst() 제거

현재 Branch가 여러 개여도 `.findFirst()` 기반 summary가 하나의 체인만 선택한다.

다중:

```text
DAO
├─ Mapper
├─ SqlId A
└─ SqlId B
```

등이 손실된다.

## 권장

Summary를 핵심 표시용으로만 유지하고
정본은:

```text
paths[]
nodes[]
edges[]
```

로 반환.

동일 순서 보장을 위해 Sort 기준 정의.

---

# 6.4 P1-04 Impact Path Algorithm 개선

`OntologyStore.traverse()`는 Flat Edge List 기반이다.

분기 구조에서:

```text
A → B
A → C
```

가 있을 경우 단순 List Reverse는 하나의 실제 경로가 아닐 수 있다.

## 권장

실제 Path 객체를 반환하는 API를 둔다.

```java
List<GraphPath> findPaths(...)
```

GraphPath:

```text
start
edges[]
end
complete
```

Impact에서는 Path 단위로 처리.

---

# 6.5 P1-05 Alias Index 충돌

현재 alias는 단일:

```text
Map<String,String>
```

이다.

Table/Column/Program 등이 같은 alias 문자열을 공유할 수 있다.

Table 전용 검색은 type filter로 일부 보완됐지만
generic lookup은 여전히 ambiguity가 있다.

## 변경

```text
Map<ConceptType, Map<String, Set<String>>>
```

또는:

```text
Map<String, Set<String>>
```

+ type filter.

동일 alias가 여러 Concept에 매핑되면
무조건 첫 번째를 선택하지 않고:

```text
AMBIGUOUS
```

를 반환.

---

# 6.6 P1-06 Registry / Store Reload 일관성

현재 `/reload`는 Registry만 갱신하고
OntologyStore를 같이 갱신하지 않는다.

더 큰 문제는:

```text
Registry
→ classpath ontology

Seed
→ project filesystem ontology/mappings
```

구조가 섞여 있다는 점이다.

## 아키텍처 결정을 먼저 한다

### 대안 A — Immutable Packaged Ontology

```text
ontology/
→ build
→ WAR
→ restart
```

운영 중 Seed/Reload 금지 또는 Admin Job 전용.

### 대안 B — External Ontology Repository

```text
external ontology root
→ Registry
→ Graph
→ atomic reload
```

## 권장

현재 1차 Production은 **대안 A**가 더 단순하다.

향후 운영 중 지식 갱신이 필요하면 대안 B로 승격한다.

현재 `OntologyProperties.basePath`가 실제 loader의 Source of Truth가 아니라면
설정 의미를 정리한다.

---

# 6.7 P1-07 MappingSeedGenerator의 SQL Table 추출 한계

현재 Regex 기반 SQL Table 추출은 다음을 놓칠 수 있다.

- JOIN
- MERGE
- schema.table
- WITH/CTE
- Dynamic SQL
- Nested SQL

따라서 Seed 결과는:

```text
GENERATED / DRAFT
```

로 취급한다.

Scanner/Parser로 검증된 경우만 VERIFIED.

---

# 6.8 P1-08 OntologyValidator 누락 비교

`compareField()`가 expected/actual 중 하나가 null이면 비교를 생략한다.

그 결과:

```text
Ontology에는 Handler 없음
Source에는 Handler 있음
```

같은 누락도 경고하지 않을 수 있다.

## 변경

다음 상태 구분:

```text
MATCH
MISMATCH
MISSING_IN_ONTOLOGY
MISSING_IN_SOURCE
NOT_APPLICABLE
```

---

# 6.9 P1-09 Workbench AUTO Program Regex

`mgcoa8888`은 9자리 Program Prefix다.

현재 Program AUTO regex가 8자리 기준이면 오분류된다.

정상 기준:

```regex
^[a-z]{5}[0-9]{4}$
```

ServiceId:

```regex
^[a-z]{5}[0-9]{4}[SCUDAR][0-9A-Z]$
```

UI와 Backend Parser 규칙을 중복 정의하지 않는 방안도 검토한다.

---

# 6.10 P1-10 Design UI Business/Function 하드코딩 제거

현재 Design 화면 Business:

```text
CO
CM
CS
```

Function:

```text
A
B
C
```

정도로 하드코딩되어 있다.

신규 시스템 재사용 도구라면 적절하지 않다.

## 변경

Ontology Classification API에서 동적으로 로드한다.

```text
System
→ Business
→ Function
```

사용자가 Business를 선택하면
해당 Business의 Function만 표시.

---

# 6.11 P1-11 Google Fonts 외부망 의존 제거

Workbench가 Google Fonts를 직접 참조한다.

폐쇄망/보안환경에서는 실패할 수 있다.

외부 폰트 URL 제거.

System Font Stack 사용.

예:

```css
font-family:
  "Malgun Gothic",
  "Noto Sans KR",
  "Apple SD Gothic Neo",
  Arial,
  sans-serif;
```

폰트 파일을 프로젝트에 임의 포함하지 않는다.

---

# 6.12 P1-12 보안/권한

현재 UI/API에는 실질적인 인증·권한이 없다.

로컬 Pilot에는 허용 가능하지만
공유 서버 또는 운영 배포 전에 반드시 통제한다.

특히 다음 쓰기/관리 API는 보호 필요:

```text
/reload
/seed/pdmg
/import/pdmg
/validate/pdmg
snapshot/inventory 관련 API
```

권한 예:

```text
ARCHITECT_VIEW
ARCHITECT_DESIGN
ONTOLOGY_ADMIN
```

---

# 7. P2 — 중기 개선

1. `OntologyStore` incoming/outgoing index
2. GraphPath 전용 모델
3. ArchitecturePattern 영속 모델
4. Approved Baseline 영속화
5. ADR
6. API `/v1`, `/query`, Legacy 정리
7. RFC7807 기반 공통 오류계약
8. H2 미사용 Dependency 제거
9. Vector/RAG는 Ontology Core 안정화 후
10. Neo4j/RDF는 실제 Graph 규모/질의 요구 발생 후

---

# 8. 목표 아키텍처

```text
┌─────────────────────────────────────────────┐
│            Architect Workbench             │
│                                             │
│ Search / Impact / Gate / Design            │
└───────────────────┬─────────────────────────┘
                    │ context-aware REST
                    ▼
┌─────────────────────────────────────────────┐
│            Application Use Cases            │
│                                             │
│ Query                                       │
│ Impact                                      │
│ Validation                                  │
│ DesignRecommendation                        │
│ PromptContext                               │
└───────────────────┬─────────────────────────┘
                    │
                    ▼
┌─────────────────────────────────────────────┐
│                Ontology Core                │
│                                             │
│ Concept / Relation / GraphPath / Rule       │
│ Provenance / ServiceId                      │
└──────────────┬──────────────────┬───────────┘
               │                  │
               ▼                  ▼
       Knowledge Source       Source Scanner
       YAML / Runtime         Java / Mapper
               │                  │
               └─────────┬────────┘
                         ▼
                Evidence / Validation
```

---

# 9. Architecture Design Assistant 목표 흐름

현재 Browser에서 많은 판단을 하지 않도록 변경한다.

## 현재

```text
UI
→ /recommend
→ 구조 여러 번 조회
→ UI derivePattern()
→ UI Baseline 생성
→ global Gate
```

## 목표

```text
UI Requirement
      ↓
POST /design/recommend
      ↓
DesignRecommendationService
      ├─ Candidate Graph Search
      ├─ Operation Match
      ├─ Evidence
      ├─ Pattern Derivation
      ├─ Baseline Assembly
      └─ Design-Time Gate
      ↓
Structured Design Recommendation
      ↓
UI Rendering
```

Browser는:

```text
입력
표시
후보선택
승인
Export
```

에 집중한다.

---

# 10. Design Recommendation 표준 응답

권장 DTO:

```json
{
  "requestId": "...",
  "requirement": {},
  "candidates": [
    {
      "programId": "mgcoa5530",
      "serviceId": "mgcoa5530S0",
      "operationMatch": true,
      "confidence": "HIGH",
      "matched": [],
      "unmatched": [],
      "unknown": [],
      "structure": {},
      "evidence": []
    }
  ],
  "pattern": {
    "name": "ONLINE_PAGING_QUERY",
    "status": "DERIVED_PATTERN",
    "derivedFrom": [],
    "properties": []
  },
  "baseline": {
    "classification": {},
    "application": {},
    "message": {},
    "transaction": {},
    "data": {},
    "security": {},
    "unresolved": []
  },
  "designGate": {
    "scope": "DESIGN",
    "status": "PASS_WITH_UNRESOLVED",
    "findings": []
  }
}
```

---

# 11. 표준 상태값

추천/설계 전반에서 다음 상태를 통일한다.

```text
VERIFIED
APPROVED
DISCOVERED
INFERRED
DERIVED
PROPOSED
UNRESOLVED
DEPRECATED
```

Rule:

```text
PASS
FAIL
UNRESOLVED
NOT_YET_IMPLEMENTED
NOT_APPLICABLE
```

---

# 12. 책임 경계와 RACI

| 업무 | Architect | Workbench | Backend | Scanner | Cursor |
|---|---|---|---|---|---|
| 요구사항 입력 | A/R | R | - | - | - |
| 후보 검색 | C | C | A/R | - | - |
| Evidence 검증 | C | C | R | A/R | - |
| Pattern 도출 | A | C | R | C | - |
| Baseline 작성 | A | C | R | C | - |
| 최종 설계 승인 | A/R | C | C | - | - |
| Context Export | A | R | R | - | C |
| 구현 | C | - | C | - | A/R |
| 구현 후 Gate | A | C | R | R | C |

---

# 13. 정상 처리 흐름

```text
Architect
→ Requirement 입력
→ Backend Design Recommend
→ Candidate 검색
→ Operation 일치 검증
→ 실제 Graph 구조조회
→ Source Provenance
→ Pattern DERIVED/VERIFIED 판정
→ Baseline
→ Design Gate
→ Architect 승인
→ Cursor Context Export
```

---

# 14. 오류·Timeout·장애 흐름

## Candidate 없음

```text
NO_MATCH
```

임의 후보 생성 금지.

## Evidence 없음

```text
UNRESOLVED
```

## Source와 YAML 충돌

```text
MISMATCH
```

Source 우선순위 정책에 따라 표시.

## Context Path 오류

UI가 API 404를 받으면
배포 경로 문제를 일반 Ontology 오류로 숨기지 않는다.

## Partial Graph

```text
PARTIAL
```

단계별 누락 명시.

---

# 15. 정상 예시

Requirement:

```text
Business    CO
Function    A
Operation   QUERY
DB          YES
Paging      YES
```

Candidate:

```text
mgcoa5530S0
operationMatch = true
```

Pattern:

```text
ONLINE_PAGING_QUERY
status = DERIVED_PATTERN
```

단, Paging Evidence가 없으면:

```text
paging.status = UNRESOLVED
```

로 둔다.

---

# 16. 금지 예시

## 금지 1

```text
UPDATE 요청
→ mgcoa9000S0를 대표 거래로 사용
```

## 금지 2

```text
pk = [A,B]
→ Column "[A,B]" 생성
```

## 금지 3

```text
tables = [A,B]
→ Table "[A,B]" 생성
```

## 금지 4

```text
Requirement paging=YES
→ 기존 Candidate도 Paging 사용했다고 VERIFIED 처리
```

## 금지 5

```text
FQCN 경로 추측
→ SOURCE_CODE VERIFIED
```

## 금지 6

```text
전체 Gate PASS
→ 신규 Design도 PASS라고 표시
```

---

# 17. 데이터 및 상태관리

현재 1차:

```text
YAML Source of Truth
→ Memory Graph
```

유지.

단:

```text
Packaged Ontology
```

와:

```text
Runtime Writable Ontology
```

를 섞지 않는다.

Production 1차 권장:

```text
Git YAML
→ Build
→ Test
→ WAR
→ Deploy
```

지식 변경도 Code Review/Gate를 거친다.

---

# 18. 성능·용량·확장성

현재 Relation 약 149개 수준에서는 전체 Scan 기반 조회도 문제없다.

하지만 향후 여러 신규 시스템을 적재하면:

```text
Concept 100K+
Relation 500K+
```

가능.

그 전에:

```text
outgoingIndex
incomingIndex
```

추가.

Graph DB 도입은 그 이후 판단.

---

# 19. 보안·개인정보·감사

Production 이전 필수:

- Architect Read 권한
- Design 권한
- Ontology Admin 권한
- 관리 API 접근 로그
- Context Export 감사
- Source Path 노출 통제
- 고객 실제 데이터 미적재
- 비밀정보 Context Export 금지

---

# 20. 운영·모니터링·장애 대응

Home에 향후:

```text
Knowledge Version
Graph Consistency
Source Drift
Rule Fail
Unverified Count
Last Build
Last Seed
Last Validation
```

표시 권장.

---

# 21. 자동검증 및 품질 Gate

최종 Release Gate는 다음이다.

## GATE-01 Build

```text
clean test
```

PASS.

## GATE-02 Full Regression

23개 Test Class 전체 실행 확인.

## GATE-03 WAR

Workbench/Design 정적자산 포함.

## GATE-04 Context Path

Root + `/tcf-ontology-service` 모두 PASS.

## GATE-05 Knowledge Data

Composite PK PASS.

Multi Table PASS.

## GATE-06 Design

QUERY/CREATE/UPDATE/DELETE/MIXED/REPORT Operation Match PASS.

## GATE-07 Evidence

추측값을 VERIFIED로 표시하지 않음.

## GATE-08 Gate Scope

GLOBAL/SERVICE/DESIGN 구분.

## GATE-09 Regression

Search/Impact/Gate 기존 기능 PASS.

---

# 22. 필수 테스트 시나리오

| ID | 테스트 | 기대 |
|---|---|---|
| T-001 | mgcoa8888S0 parse | 11자리 정상 |
| T-002 | mgcoa5530 composite PK | L5101/L5103 별도 Column |
| T-003 | multi-table YAML | Table별 Concept 생성 |
| T-004 | QUERY recommend | S Service 선택 |
| T-005 | CREATE recommend | C Service 선택 |
| T-006 | UPDATE recommend | U Service 선택 |
| T-007 | DELETE recommend | D Service 선택 |
| T-008 | MIXED recommend | A Service 선택 |
| T-009 | REPORT recommend | R Service 선택 |
| T-010 | missing op | mismatch/UNRESOLVED |
| T-011 | guessed source | DISCOVERED/INFERRED |
| T-012 | scanner verified | VERIFIED |
| T-013 | global gate | GLOBAL 표시 |
| T-014 | service gate | service only |
| T-015 | design gate | unresolved 허용 |
| T-016 | boot root UI | API 성공 |
| T-017 | context path UI | API 성공 |
| T-018 | WAR content | workbench 포함 |
| T-019 | AUTO mgcoa8888 | PROGRAM |
| T-020 | no candidate | 임의 후보 없음 |

---

# 23. 변경·호환성·폐기 관리

1. 기존 `/api/ontology/recommend`는 당장 삭제하지 않는다.
2. Design 전용 통합 API 추가 후 기존 UI 호환 유지.
3. Legacy scalar `data.table`, `data.pk` 지원.
4. 새 list 구조 지원.
5. API 정리 시 Deprecation 문서 작성.
6. 현재 WAR 폐기 후 최신 WAR만 Baseline 지정.

---

# 24. 버전 관리

현재 다음 Version이 혼재한다.

```text
Gradle application version
Ontology bundle version
Runtime concept version
Ontology Core 1.0 문서
```

다음으로 분리한다.

```text
productVersion
ontologySchemaVersion
knowledgeSnapshotVersion
apiVersion
```

예:

```text
productVersion         = 0.1.0
ontologySchemaVersion  = 1.0
knowledgeSnapshot      = 2026.08.10.01
apiVersion             = v1
```

---

# 25. 산출물 정리

최종 프로젝트 Baseline에는 다음을 유지한다.

```text
src/
ontology/
docs/
scripts/
test-data/
gradle/
build.gradle
settings.gradle
gradlew*
README
```

배포/소스 ZIP에는 가급적:

```text
.gradle/
build/
```

를 제외한다.

현재 ZIP에 포함된 build 산출물은
Source Baseline과 Release Artifact를 혼동시킬 수 있다.

---

# 26. Cursor 실행 순서

---

## Phase 0 — 현재 상태 재확인

Cursor는 먼저 이 문서와 현재 전체 소스를 비교한다.

아직 수정하지 말고:

```text
CURRENT-PROJECT-GAP-CHECK.md
```

작성.

각 항목:

```text
CONFIRMED
ALREADY_FIXED
NOT_FOUND
DIFFERENT_IMPLEMENTATION
```

으로 표시.

---

## Phase 1 — P0 Knowledge Integrity

순서:

1. Composite PK
2. Multi Table
3. Operation Type
4. primaryServiceId
5. Evidence 없는 derivePattern 제거/Backend 이동
6. Gate Scope/Semantic

산출:

```text
P0-KNOWLEDGE-INTEGRITY-REPORT.md
```

---

## Phase 2 — Deployment Integrity

1. Context Path
2. Health Workbench URL
3. Full Test
4. WAR rebuild
5. WAR content inspect
6. Root/Context smoke

산출:

```text
WAR-DEPLOYMENT-ACCEPTANCE-REPORT.md
```

---

## Phase 3 — P1 Evidence/Graph Integrity

1. Provenance
2. Classification
3. Summary/Path
4. Alias
5. Reload
6. Validator
7. Dynamic Classification UI
8. Program Regex
9. External Font

산출:

```text
ONTOLOGY-P1-INTEGRITY-REPORT.md
```

---

## Phase 4 — Design Assistant Acceptance

실제 Scenario:

```text
QUERY
CREATE
UPDATE
DELETE
```

최소 4종 검증.

Candidate:

```text
실제 ServiceId
실제 Source
실제 Evidence
```

출력:

```text
Design Recommendation
Pattern
Baseline
Gate
Context Export
```

산출:

```text
ARCHITECT-DESIGN-ASSISTANT-FINAL-ACCEPTANCE.md
```

---

# 27. Cursor에게 전달할 최종 실행 지시

다음 내용을 그대로 Cursor에게 전달한다.

---

## CURSOR MASTER INSTRUCTION

```text
너는 NSIGHT/PDMG `tcf-ontology-service`의 수석 Application Architect이자
Java/Spring Boot/Graph Knowledge Architecture 전문가다.

첨부된
`TCF Ontology Service 전체 프로젝트 점검 및 Cursor 통합 보완 지시서`
를 기준으로 현재 전체 프로젝트를 보완하라.

중요:

새 기능을 무작정 추가하지 마라.
현재 프로젝트의 Knowledge Integrity와 Release Integrity를 먼저 완성한다.

### 작업원칙

1. 기존 동작을 깨지 않는다.
2. Mock으로 PASS하지 않는다.
3. Ontology에 없는 정보를 임의 생성하지 않는다.
4. 추측값을 VERIFIED로 표시하지 않는다.
5. 실제 Source와 Graph Evidence를 구분한다.
6. 외부 Tomcat WAR 배포를 반드시 고려한다.
7. UI에서 Architecture 사실을 하드코딩하지 않는다.
8. Browser JS는 Architecture 판단 엔진이 아니다.
9. Architecture Recommendation 핵심 로직은 Backend Use Case로 이동한다.
10. 테스트를 통과하지 못한 항목은 완료라고 보고하지 않는다.

### 첫 작업

먼저 전체 소스를 다시 분석하고 본 문서의 P0/P1/P2 항목이
현재 Source에 실제로 존재하는지 점검하라.

결과를:

CURRENT-PROJECT-GAP-CHECK.md

로 작성한다.

각 항목을:

CONFIRMED
ALREADY_FIXED
NOT_FOUND
DIFFERENT_IMPLEMENTATION

중 하나로 판정한다.

그 다음 P0만 수정한다.

### P0-1 Release Artifact

현재 build/libs의 WAR를 신뢰하지 마라.

모든 수정 완료 후:

gradlew.bat clean test war

를 실제 실행한다.

최신 WAR 내부에:

static/workbench/index.html
workbench.css
api.js
app.js
design.js

가 모두 존재하는지 검사한다.

### P0-2 Context Path

Workbench API client가 `/api/...` 절대 Root에 종속되지 않도록 한다.

다음 둘 모두에서 동작시킨다.

BootRun:
http://localhost:8098/workbench/index.html

Tomcat Context:
http://localhost:8098/tcf-ontology-service/workbench/index.html

API 호출도 각 Context를 자동 반영해야 한다.

### P0-3 Composite PK

YAML pk가 scalar 또는 list일 수 있다.

pk:
  - L5101
  - L5103

를 하나의 "[L5101, L5103]" Column으로 생성하지 마라.

L5101
L5103

각각 별도 Column Concept를 만들어야 한다.

mgcoa5530, mgcoa9999로 테스트한다.

### P0-4 Multi Table

data.table 또는 data.tables가 여러 Table을 나타낼 수 있어야 한다.

List를 문자열화한 가짜 Table을 만들지 마라.

SQL별 Table Evidence가 없으면
Mapper→Table까지만 VERIFIED로 연결하고
SqlId→Table을 임의 생성하지 마라.

### P0-5 Transaction Type

Architecture Design Assistant 거래유형을 다음으로 정규화한다.

QUERY  → S
CREATE → C
UPDATE → U
DELETE → D
MIXED  → A
REPORT → R

UPDATE를 query로 매핑하지 마라.

### P0-6 Candidate ServiceId

primaryServiceId가 무조건 S를 선택하지 않게 한다.

요청한 operation과 동일한 ServiceId를 선택한다.

해당 operation이 후보 Program에 없으면
operationMatch=false 또는 UNRESOLVED로 표시한다.

### P0-7 Recommendation Evidence

design.js에서 다음을 임의로 확정하지 마라.

Paging
Message Envelope
Transaction
Timeout
Architecture Pattern

Backend DesignRecommendationService를 두고
Graph + Registry + Runtime + Provenance + Rule을 이용해
field-level Evidence를 포함한 결과를 반환하라.

필요하면:

POST /api/ontology/design/recommend

를 추가한다.

기존 /recommend는 호환 유지한다.

### P0-8 Architecture Gate

GLOBAL / SERVICE / DESIGN Scope를 분리한다.

전체 validateAll 결과를
특정 Design 검증 결과처럼 보여주지 마라.

RULE-003가 실제 Handler `serviceIds()` Source 등록까지 검사하는지 명확히 한다.

Source 검증을 하지 않는 Rule은 이름/설명을 과장하지 않는다.

### P0-9 Full Regression

현재 build/test-results에 있는 일부 6개 Suite만으로 완료하지 마라.

전체:

gradlew.bat clean test

를 수행한다.

모든 test class와 결과를 최종 보고서에 기록한다.

### P1

P0 이후 다음을 순서대로 처리한다.

- Provenance VERIFIED 정책
- classification 실제 Relation 검증
- summarizeStructure findFirst 제거
- GraphPath 모델
- Alias ambiguity
- Registry/Store reload
- MappingSeedGenerator DRAFT 정책
- OntologyValidator MISSING 상태
- Program AUTO regex
- Business/Function UI 동적화
- Google Font 제거
- Production 권한

### 금지

다음은 금지한다.

- 테스트 실패를 무시하고 완료 선언
- Candidate가 없는데 임의 Candidate 생성
- 사용자 Paging 요구를 기존 Program의 Paging Evidence로 둔갑
- 추측 Java Path를 SOURCE_CODE VERIFIED 처리
- List PK/Table을 String으로 변환
- UPDATE 요청에 S ServiceId 선택
- WAR를 재생성하지 않고 Source 완료 선언
- Context Path 테스트 없이 Tomcat 배포 가능 선언

### 최종 산출물

다음을 작성한다.

1. CURRENT-PROJECT-GAP-CHECK.md
2. P0-KNOWLEDGE-INTEGRITY-REPORT.md
3. WAR-DEPLOYMENT-ACCEPTANCE-REPORT.md
4. ONTOLOGY-P1-INTEGRITY-REPORT.md
5. ARCHITECT-DESIGN-ASSISTANT-FINAL-ACCEPTANCE.md
6. PROJECT-RELEASE-READINESS.md

### 최종 승인 조건

모든 조건을 만족할 때만:

TCF ONTOLOGY SERVICE — RELEASE READY

라고 선언한다.

조건:

- Full Test PASS
- Latest WAR
- Workbench inside WAR
- BootRoot PASS
- Tomcat Context PASS
- Composite PK PASS
- Multi Table PASS
- Operation Match PASS
- Evidence Integrity PASS
- Gate Scope PASS
- Search/Impact/Gate Regression PASS
- Design Assistant Golden Scenario PASS

하나라도 실패하면:

NOT RELEASE READY

로 판정하고 원인을 기록한다.
```

---

# 28. 테스트 Golden Data

최소 기존 실데이터를 다음 용도로 활용한다.

```text
mgcoa8888
- Query
- Delete
- Image Log
- Table Impact

mgcoa5530
- Query
- Composite PK
- Paging 계열 검토

mgcoa9000
- CRUD Operation별 ServiceId 검증

mgcoa9999
- Composite PK
```

---

# 29. 핵심 아키텍처 판단

현재 가장 중요한 판단은 다음이다.

> `tcf-ontology-service`의 가치는 Graph를 많이 만드는 데 있지 않다.

가치는:

```text
실제 Source
   ↓
검증된 Knowledge
   ↓
설명 가능한 Relation
   ↓
Evidence 기반 Recommendation
   ↓
Architect Decision
   ↓
Cursor Context
   ↓
Implementation
   ↓
Architecture Gate
```

이 Closed Loop가 만들어지는 데 있다.

---

# 30. 주요 위험

1. Ontology가 실제 Source보다 “더 자신 있게” 말하는 위험
2. UI 하드코딩이 Architecture Standard로 오해되는 위험
3. Pilot API를 Production Gate로 오해하는 위험
4. Source와 WAR가 다른 버전인 위험
5. 신규 시스템이 늘어날 때 YAML schema가 단일 Program 전제에 묶이는 위험
6. Runtime Update와 Packaged Knowledge 전략이 혼재하는 위험

---

# 31. 우선 보완 과제

순위:

```text
1. Release/WAR Context Path
2. Composite PK / Multi Table
3. Design Operation Match
4. Evidence 기반 Recommendation
5. Gate Scope
6. Provenance
7. Full Regression
8. Dynamic UI
9. Reload/Version Governance
10. Performance Index
```

---

# 32. 중장기 발전 방향

```text
              Source Scanner
                    │
                    ▼
            Verified Ontology
                    │
         ┌──────────┴──────────┐
         ▼                     ▼
   Architect Workbench     RAG Context
         │                     │
         ▼                     ▼
   Design Assistant        Cursor/LLM
         │                     │
         └──────────┬──────────┘
                    ▼
                Harness
                    │
                    ▼
               Generated Code
                    │
                    ▼
            Architecture Gate
                    │
                    ▼
              Knowledge Feedback
```

Graph DB나 Vector DB는 이 순환구조가 안정된 이후 검토한다.

---

# 33. 마무리말

현재 프로젝트는 폐기하거나 다시 만들 수준이 아니다.

Core와 Workbench의 방향은 충분히 재사용할 가치가 있다.

그러나 다음 상태다.

```text
Ontology Core             성숙 중
Workbench 1차             사용 가능
Design Assistant          Pilot
Recommendation            개선 필요
Evidence Integrity        개선 필요
Release WAR               재생성 필요
Production Security       미완
```

따라서 다음 목표는:

> **기능 추가가 아니라 "이 Ontology가 말하는 내용은 실제 Source와 Evidence를 기반으로 믿을 수 있다"는 상태를 만드는 것**

이다.

그 상태가 확보되면 `tcf-ontology-service`는 신규 시스템 구축 시
아키텍처 표준 검색, 유사 설계 추천, 영향도 분석, 개발 검증,
Cursor Context 제공을 담당하는 **NSIGHT Architecture Intelligence Platform**의 기반으로 사용할 수 있다.
