# TCF Ontology Service 전체 소스 재점검 보고서

- 점검일: 2026-08-10
- 대상: `tcf-ontology-service(1).zip`
- 범위: 전체 프로젝트 소스 / Ontology YAML / Workbench / Design Assistant / Test Results / WAR
- 판정 기준: 신규 시스템 구축 시 Architecture Knowledge Service 및 Architect Workbench로 안전하게 재사용 가능한가
- 점검 방식: 압축본 전체 정적분석 + 압축본 내 Test Result/WAR 교차검증
- 주의: 본 점검 환경에서는 Gradle 배포본을 새로 내려받을 수 없어 `gradlew clean test war`를 재실행하지 못했다. 대신 압축본의 `build/test-results/test`와 WAR 내용/정적자산 해시를 검증하였다.

---

# 1. 종합 판정

## 현재 판정

> **INTERNAL PILOT READY / RELEASE CANDIDATE**
>
> **PRODUCTION / ARCHITECTURE GOVERNANCE READY는 아님**

압축본 내부 증거는 다음과 같다.

- Test Suite: 27
- Test: 61
- Failure: 0
- Error: 0
- Skip: 0
- WAR 존재
- WAR 내부 Workbench 포함
- `index.html`, `api.js`, `app.js`, `design.js`는 현재 `src/main/resources`와 WAR 내부 파일 해시가 동일

그러나 현재 Test가 잡지 못한 P0 결함이 존재한다.

---

# 2. P0 — 즉시 수정

## P0-01 ArchitectureRuleValidator 미등록 ServiceId 무한 재귀

### 위치

`src/main/java/nhnis/ontology/validate/ArchitectureRuleValidator.java`

현재 흐름:

```java
public Map<String, Object> validateService(String serviceId) {
    ...
    if (sid == null) {
        return validateDesignBaseline(
            Map.of("serviceId", canonical, "reason", "not_in_graph"));
    }
}
```

`validateDesignBaseline()`:

```java
if (serviceId == null
        || serviceId.isBlank()
        || "UNRESOLVED".equalsIgnoreCase(serviceId)) {
    ...
}
return validateService(serviceId);
```

따라서 형식은 정상이나 Graph에 없는 ServiceId:

```text
mgcoa7777S0
```

를 검증하면:

```text
validateService
→ sid 없음
→ validateDesignBaseline(mgcoa7777S0)
→ validateService(mgcoa7777S0)
→ ...
```

무한 재귀가 발생할 수 있다.

### 수정 원칙

SERVICE 검증과 DESIGN 검증을 절대 상호 재귀시키지 않는다.

권장:

```text
validateService(existingServiceId)
- 형식 오류 → FAIL
- Graph 미등록 → NOT_FOUND 또는 UNRESOLVED
- 등록됨 → SERVICE Rule 실행

validateDesignBaseline()
- ServiceId UNRESOLVED → Design-Time 상태
- ServiceId 할당됐으나 미구현 → NOT_YET_IMPLEMENTED
- 기존 Service를 참조검증하고 싶으면 명시적으로 validateService 호출
```

### 필수 테스트

```text
validateService("mgcoa7777S0")
```

기대:

- StackOverflow 없음
- scope=SERVICE
- status=NOT_FOUND 또는 FAIL/UNRESOLVED 정책값
- 명시적 finding 반환

---

## P0-02 Design Context Export의 Backend/UI 데이터 계약 불일치

### Backend

`DesignRecommendationService.buildBaseline()`은:

```java
baseline.put("message", prop(...));
baseline.put("transaction", prop(...));
```

형태다.

즉:

```json
"message": {
  "property": "envelope",
  "value": "UNRESOLVED",
  "status": "UNRESOLVED",
  "note": "..."
}
```

형태다.

또 `baseline.paging`은 별도 필드로 생성하지 않는다.

### UI

`design.js`의 `buildExportDocs()`는:

```javascript
baseline.message.envelope
baseline.message.detail
baseline.transaction
baseline.paging
```

을 읽는다.

따라서 Backend Baseline을 그대로 사용할 경우 Export Markdown에:

```text
Message
- undefined
- detail: undefined

Transaction
- [object Object]

Paging
- undefined
```

형태가 들어갈 수 있다.

### 영향

Architecture Design Assistant의 핵심 산출물인:

```text
Cursor Context Export
```

가 잘못될 수 있다.

### 수정 원칙

Backend 응답 DTO를 정본으로 고정한다.

권장:

```json
"message": {
  "envelope": {
    "value": "UNRESOLVED",
    "status": "UNRESOLVED",
    "evidence": []
  }
},
"transaction": {
  "value": "UNRESOLVED",
  "status": "UNRESOLVED",
  "evidence": []
},
"paging": {
  "value": "REQUESTED_YES",
  "status": "UNRESOLVED",
  "evidence": []
}
```

UI는 동일 DTO만 사용한다.

### 필수 테스트

실제 `POST /api/ontology/design/recommend` 결과를
UI Export 함수에 넣고 다음 문자열이 없어야 한다.

```text
undefined
[object Object]
null
```

---

## P0-03 Design Recommendation이 실제 Operation Match 0건이어도 OK 가능

### 위치

`DesignRecommendationService`

현재:

```java
out.put("status", candidates.isEmpty() ? "NO_MATCH" : "OK");
```

이다.

하지만 Candidate가 존재하더라도:

```text
operationMatch=false
```

만 존재할 수 있다.

현재 Mapping은 실제로:

```text
S / C / U / D
```

위주이며 A(MIXED), R(REPORT) 사례는 없다.

따라서 MIXED/REPORT 요청 시:

```text
Candidate 5건
operationMatch = false
status = OK
```

상태가 가능하다.

### 추가 문제

`RecommendService.intentScore()`는:

- REPORT → R 존재가 아니라 S 존재로 점수를 줌
- MIXED → A 존재가 아니라 CRUD(S/C/U/D) 기준으로 평가

즉 Operation 모델과 Recommend Ranking 모델이 일치하지 않는다.

### 수정

`matchingCandidates`를 별도로 계산한다.

```java
boolean anyOperationMatch =
    candidates.stream()
        .anyMatch(c -> Boolean.TRUE.equals(c.get("operationMatch")));
```

판정 예:

```text
NO_MATCH              Candidate 자체 없음
OPERATION_NO_MATCH    Candidate는 있으나 요청 op 없음
OK                    op match 존재
PARTIAL               일부 증거/구조 미확인
```

### 필수 테스트

```text
QUERY  → S
CREATE → C
UPDATE → U
DELETE → D
MIXED  → A
REPORT → R
```

A/R 사례가 현재 Ontology에 없다면:

```text
OPERATION_NO_MATCH
```

이어야 하며 S로 대체하면 안 된다.

---

## P0-04 Architecture Pattern/Baseline 구조가 실제 Candidate 구조 확인 없이 DERIVED 처리됨

### 위치

`DesignRecommendationService.buildPattern()`

현재 structure:

```text
Handler → Facade → Service → DAO → Mapper/SQL → Table
```

를 코드에 직접 넣는다.

그리고:

```java
derivedFrom.isEmpty()
    ? "UNRESOLVED"
    : "DERIVED"
```

로 상태를 정한다.

즉 `derivedFrom`에 ServiceId 하나만 있어도
실제 그 ServiceId가 전체 Chain을 갖는지 확인하지 않고
전체 구조를 DERIVED로 표현한다.

`buildBaseline()`도:

```java
"layers",
"Handler → Facade → Service → DAO → Mapper → Table"
```

를 고정한다.

### 문제

Ontology에 존재하는 사실과
프로그램 코드에 하드코딩된 Architecture 가정이 섞인다.

### 수정

Backend `DesignRecommendationService`가
`OntologyQueryService.serviceStructure()`를 이용해 Candidate별 실제 Edge를 확인한다.

Pattern 구조는:

```text
Candidate A 실제 path
Candidate B 실제 path
Candidate C 실제 path
```

의 공통 구조 또는 승인된 Pattern Registry로부터 생성한다.

증거 부족 시:

```text
structure = UNRESOLVED
status = UNRESOLVED
```

---

## P0-05 Production 기본 설정이 Local + Admin Mutation ON

### 위치

`src/main/resources/application.yml`

현재:

```yaml
spring:
  profiles:
    active: local

nhnis:
  ontology:
    admin-mutations-enabled: true
```

prod profile에서는 false이나,
운영에서 profile을 명시하지 않으면 기본적으로 local profile이 활성화된다.

즉 배포 실수 시:

```text
/reload
/import/pdmg
/seed/pdmg
```

등 관리 API가 활성 상태가 될 수 있다.

### 권장

안전한 기본값:

```yaml
nhnis:
  ontology:
    admin-mutations-enabled: false
```

local/dev에서만:

```yaml
admin-mutations-enabled: true
```

`spring.profiles.active: local`을 packaged application.yml에서 제거한다.

로컬 실행은:

```text
--spring.profiles.active=local
```

또는 RUN.bat에서 명시한다.

---

# 3. P1 — 높은 우선순위 보완

## P1-01 Architecture Gate가 실제 Application Architecture Rule 전체를 검증하지 않음

Ontology Rule YAML:

```text
R-HANDLER-NO-DAO
R-TX-OWNER-EXECUTOR
R-IMAGELOG-OUTSIDE-TX
R-BIZPREPOST-ON-SERVICE
R-NAMING-SERVICEID-METHOD
R-PROGRAM-SINGLE-HANDLER
```

그러나 `ArchitectureRuleValidator`가 실행하는 것은:

```text
RULE-001 ServiceId 형식
RULE-002 HANDLED_BY 존재
RULE-003 Handler와 ServiceId programId 일치
RULE-004 Program→ServiceId 존재
RULE-005 Service outgoing dependency 존재
RULE-006 DAO→Mapper/SqlId 존재
```

이다.

즉 현재 Gate는 엄밀히 말하면:

> **Application Architecture Conformance Gate보다 Ontology Structural Integrity Gate에 가깝다.**

### 특히 RULE-005

현재:

```java
anyMatch(
  predicate == USES
  || predicate == CALLS
)
```

만 검사한다.

Target이 실제 DAO/CLIENT인지 검사하지 않는다.

따라서:

```text
Service → CALLS → Service
```

만 있어도 RULE-005가 PASS할 수 있다.

### 권장

Gate를 두 계층으로 분리:

```text
ONTOLOGY_INTEGRITY_GATE
APPLICATION_ARCHITECTURE_GATE
```

그리고 `component-boundaries.yml`의 R-* Rule에:

```text
implemented
executor
scope
sourceBacked
```

를 추가한다.

---

## P1-02 RULE-003은 실제 Handler Source Registration 검증이 아님

RULE-003 설명은:

```text
Handler가 등록한 ServiceId와 Ontology 관계가 일치
```

지만 실제 구현은 Ontology Graph의 reverse HANDLED_BY와 programId prefix를 확인한다.

실제 Java:

```text
handler.serviceIds()
```

와 비교하지 않는다.

따라서 이름을:

```text
Ontology Handler-ServiceId Relation Consistency
```

로 축소하거나,
Scanner 결과와 실제 Java registration을 비교하도록 구현한다.

---

## P1-03 Scanner VERIFIED가 실제 Graph Provenance에 반영되지 않음

`Provenance.scannerVerified()` 메서드는 존재하지만
전체 main source에서 실제 호출되는 곳이 없다.

따라서 현재 Design Graph의 Source Evidence는 대부분:

```text
DISCOVERED
```

상태에 머문다.

즉:

```text
Scanner가 실제 Source를 검증
→ Graph Provenance VERIFIED로 승격
```

하는 Closed Loop가 아직 구현되지 않았다.

### 권장

```text
PdmgInventoryScanner
→ SourceVerificationResult
→ OntologyEvidenceMerger
→ Graph Provenance Upgrade
```

단계를 도입한다.

---

## P1-04 classification relationStatus=VERIFIED 용어 혼동

`classificationPath()`는 실제 Graph Edge가 존재하면:

```text
relationStatus = VERIFIED
```

라고 한다.

그러나 해당 Edge의 Provenance가 DISCOVERED일 수 있다.

따라서 여기의 VERIFIED는:

```text
Graph에 존재함
```

이라는 의미일 뿐
Source 검증 완료라는 의미가 아니다.

권장:

```text
relationStatus = PRESENT | INFERRED
verificationStatus = DISCOVERED | VERIFIED | APPROVED
```

로 분리.

---

## P1-05 Design Gate의 PASS가 미완성 설계를 너무 긍정적으로 표현

Design Baseline에:

```text
UNRESOLVED
NOT_YET_IMPLEMENTED
```

가 여러 건 있어도 `failCount=0`이면:

```text
status=PASS
```

다.

Architect UI에서는:

```text
PASS_WITH_UNRESOLVED
INCOMPLETE
```

같은 별도 상태가 더 안전하다.

---

## P1-06 Multi Table Acceptance Test가 실제 Multi Table Mapping을 검증하지 않음

`CompositePkAndMultiTableLoaderTest`는:

- Composite PK
- normalizeStringList scalar/list

는 검증한다.

그러나 실제:

```yaml
tables:
  - TB_A
  - TB_B
```

를 Loader에 넣어:

```text
Mapper→TB_A
Mapper→TB_B
SqlId→Table 임의 생성 없음
```

까지 검증하는 Test는 확인되지 않았다.

Synthetic YAML Test를 추가한다.

---

## P1-07 Recommend Top-5 절단 시 실제 Operation Match Candidate 누락 가능

`RecommendService`는 먼저 score 기준 Top 5로 자른다.

그 뒤 `DesignRecommendationService`가 operationMatch 기준으로 재정렬한다.

시스템이 커지면:

```text
requested operation을 가진 후보가 6위 이하
```

일 경우 후보에서 완전히 사라질 수 있다.

Operation Match는 Top-N 절단보다 먼저 적용되어야 한다.

---

## P1-08 Recommendation Confidence에 순환 Evidence가 포함됨

YAML에서 Graph를 만들고,
같은 Graph에 Rule을 실행해 PASS하면
Recommendation score를 추가한다.

즉:

```text
YAML
→ Graph
→ Graph Rule PASS
→ Candidate Confidence 상승
```

은 독립적인 Source 검증 Evidence가 아니다.

Score를 최소:

```text
similarityScore
graphCompletenessScore
sourceTrustScore
ruleComplianceScore
```

로 분리한다.

---

## P1-09 PromptContextExporter가 Runtime Chain을 코드에 하드코딩

`PromptContextExporter.summarizeRuntime()`은
`tx-runtime.yml`의 실제 steps를 동적으로 조립하지 않고
Java 문자열로 Runtime 흐름을 고정한다.

Runtime YAML이 변경되면 Prompt Context와 Ontology가 달라질 수 있다.

`tx-runtime.yml`의 `steps`를 정렬하여 동적으로 생성하도록 한다.

---

## P1-10 Stale test-data 정리 필요

현재 `test-data/queries`에는 과거 수정 전 결과가 남아 있다.

예:

```text
design-scenario2-structure.json
```

에:

```text
verificationStatus = VERIFIED
```

가 남아 있고,

Composite PK가:

```text
"[L5101, L5103]"
```

처럼 하나의 값으로 표현된 과거 결과도 존재한다.

`design-scenario4-context.md`도
현재 Evidence 정책보다 강한 Transaction/Message 문구를 포함한다.

이 파일들이 Cursor/RAG/사람의 기준자료로 재사용되면
수정된 Source보다 오래된 지식이 다시 유입될 수 있다.

### 조치

- Golden Data 재생성
- 파일 Header에 knowledgeSnapshot/version 기록
- stale artifact 삭제 또는 `_archive/` 이동
- CI에서 Golden Artifact freshness 검사

---

## P1-11 Version 정합성

현재:

```text
build.gradle version = 0.0.1-SNAPSHOT
ontology version     = 0.2.0
Release 제안         = 0.1.0
```

이 혼재한다.

분리:

```text
productVersion
ontologySchemaVersion
knowledgeSnapshotVersion
apiVersion
```

빌드 Artifact에도 productVersion 반영.

---

## P1-12 Reload 비원자성

현재:

```text
registry.reload()
store.clear()
graphBootstrap.loadAll()
```

이다.

`loadAll()` 중 실패하면 Store가 비어 있거나 부분상태가 될 수 있다.

향후:

```text
new temporary graph
→ validation
→ atomic swap
```

방식 권장.

---

# 4. P2 — 후속

1. Alias ambiguity
2. GraphPath 정식 모델
3. OntologyStore incoming/outgoing index
4. ArchitecturePattern Registry
5. ADR
6. AuthN/AuthZ/RBAC
7. RFC7807 공통 오류계약
8. H2 미사용 dependency 제거
9. README/운영문서 Workbench/Design Assistant 최신화

---

# 5. 현재 잘 구현된 부분

다음은 실제 Source 기준으로 긍정적이다.

- `Concept / Relation / GraphType` 분리
- DESIGN / RUNTIME Graph 분리
- ServiceId 11자리 Parser
- Composite PK normalize 로직
- Multi-table Loader 호환 로직
- UPDATE→U, CREATE→C, DELETE→D operation 선택
- S 묵시적 fallback 제거
- Context Path 자동해석 `resolveContextPath`
- Workbench 5개 화면 자산이 WAR에 포함
- Google Fonts 제거
- Provenance의 YAML/추측 Source를 DISCOVERED로 하향
- Registry + Store reload 동시 수행
- GLOBAL / SERVICE / DESIGN API 분리
- 27 Suite / 61 Test / 0 Failure 기존 결과
- WAR 정적 Workbench 자산이 현재 Source와 동일

---

# 6. 테스트 보완 필수 목록

추가 Test:

```text
T-NEW-001 valid-but-unregistered ServiceId validation
T-NEW-002 Design Context Export no undefined/[object Object]
T-NEW-003 MIXED operation A match / no-match
T-NEW-004 REPORT operation R match / no-match
T-NEW-005 all candidates operationMatch=false → OPERATION_NO_MATCH
T-NEW-006 actual multi-table synthetic YAML
T-NEW-007 RULE-005 wrong target role must FAIL
T-NEW-008 prod default admin mutation disabled
T-NEW-009 Prompt runtime generated from tx-runtime.yml
T-NEW-010 stale Golden Artifact version/freshness
```

---

# 7. 수정 우선순위

```text
1. validateService 무한 재귀
2. Cursor Context Export 계약 불일치
3. MIXED/REPORT / operation no-match 상태
4. Production safe default
5. Architecture Gate 의미 정합
6. Pattern Evidence
7. Scanner→Verified Closed Loop
8. Golden Artifact 정리
9. Version / Reload / Scale
```

---

# 8. 최종 판단

현재 프로젝트는 폐기할 수준이 아니다.

오히려 Core 구조와 Workbench 방향은 매우 유효하다.

다만 현재 상태는:

```text
Ontology Core             GOOD
Workbench                 GOOD
Impact                    GOOD
WAR/Context Path          GOOD
Design Assistant          PILOT
Architecture Gate         PARTIAL
Source Verification       PARTIAL
Cursor Context Export     FIX REQUIRED
Production Security       FIX REQUIRED
```

따라서 현재 판정은:

> **INTERNAL PILOT READY**
>
> P0 수정 후 다시 전체 `clean test war` 및 Golden Scenario를 실행해야
> **RELEASE READY**를 재선언할 수 있다.

