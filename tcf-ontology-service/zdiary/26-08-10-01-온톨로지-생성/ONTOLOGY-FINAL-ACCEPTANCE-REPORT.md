# ONTOLOGY-FINAL-ACCEPTANCE-REPORT

- Date: 2026-08-10
- Prompt: `zdiary/26-08-10-01-온톨로지-생성/26-08-10-07-점검보완.md`
- Scope: **최종 승인 검증만** (신규 기능 추가 없음)
- Evidence dir: `test-data/queries/`
- Boot URL used: `http://localhost:8099` (기존 8098과 충돌 회피)

---

## 0. 이전 보고서 모순에 대한 재검증 목적

`ONTOLOGY-CHECK-REPORT.md`에서 Test E PARTIAL / P0와 최종 PASS가 모순이었음.  
본 문서는 **P0 수정 후 실제 HTTP 응답**으로 Impact/Reverse를 다시 판정한다.

---

## 1. 빌드 / 기동 실측

### 1.1 `gradlew.bat clean test`

```text
BUILD SUCCESSFUL in 24s
5 actionable tasks: 5 executed
```

로그: `test-data/queries/last-clean-test.log`

### 1.2 `gradlew.bat war`

```text
BUILD SUCCESSFUL in 3s
artifact: build/libs/tcf-ontology-service.war (23,189,435 bytes)
```

로그: `test-data/queries/last-war.log`  
참고: `bootWar`는 `build.gradle`에서 disabled → 이번 승인 범위에서 `war`로 대체.

### 1.3 `gradlew.bat bootRun --args="--server.port=8099"`

```text
Tomcat started on port 8099
Started OntologyApplication in 3.945 seconds
YAML→Graph loaded: programs=5, services=12, runtimeSteps=15, concepts=97, relations=149
```

기동 **PASS**.

---

## 2. Impact API 실측

### Request

```http
GET http://localhost:8099/api/ontology/impact/table/TB_FW_IMAGE_LOG
```

### 전체 Response

파일로 저장:

`test-data/queries/acceptance-impact-TB_FW_IMAGE_LOG.json` (16,392 bytes)

### §2 필수 필드 (비어 있으면 안 됨)

| 필드 | count | 샘플 | 판정 |
|------|-------|------|------|
| affectedMappers | 1 | mgcoa8888-ORA.xml | PASS |
| affectedDaos | 1 | mgcoa8888DAO | PASS |
| affectedServices | 1 | mgcoa8888Service | PASS |
| affectedFacades | 1 | mgcoa8888Facade | PASS |
| affectedHandlers | 1 | mgcoa8888Handler | PASS |
| affectedServiceIds | 2 | mgcoa8888D0, mgcoa8888S0 | PASS |
| affectedPrograms | 1 | mgcoa8888 | PASS |
| affectedBusinesses | 1 | CO | PASS |
| paths | 2 | 존재 | PASS |

→ **이전 P0(빈 배열)은 해소되었음을 실측 증명.**

### 이상 징후 (승인에 영향)

Response의 `table` 객체가 **TABLE이 아니라 COLUMN(GUID)** 으로 나옴:

```json
"table": {
  "id": "column:RDW:TB_FW_IMAGE_LOG:GUID",
  "type": "COLUMN",
  "name": "GUID"
}
```

원인 추정: alias `TB_FW_IMAGE_LOG`가 Column의 `tableName` 속성으로도 등록되어 `resolveTable`이 COLUMN을 반환.  
→ Impact 루트 노드 품질 결함. **임의로 고치지 않고 FAIL 요인으로 기록.**

---

## 3. 역추적 구조 증명

기대:

```text
TB_FW_IMAGE_LOG
← Mapper/SqlId ← DAO ← Service ← Facade ← Handler
← ServiceId ← Program ← Function ← Business ← System
```

### 실측 판정

| 단계 | 증거 | 판정 |
|------|------|------|
| Table | paths 내 `fromName=TB_FW_IMAGE_LOG` / type TABLE | PASS (중간 노드) |
| Mapper/SqlId | affectedMappers, affectedSqlIds, paths ACCESSES/EXECUTES | PASS |
| DAO | affectedDaos, paths USES/EXECUTES | PASS |
| Service | affectedServices | PASS |
| Facade | affectedFacades | PASS |
| Handler | affectedHandlers | PASS |
| ServiceId | affectedServiceIds | PASS |
| Program | affectedPrograms | PASS |
| Business | affectedBusinesses=`CO` | PASS |
| Function | impact JSON에 `function:MG:CO:A` / FUNCTION **미포함** | **NOT_AVAILABLE** |
| System | impact JSON에 `system:MG` **미포함** | **NOT_AVAILABLE** |

Function/System 관계는 **정방향 structure에는 존재**하지만, Impact Response의 `paths`/`affected*`로는 **증명되지 않음**.

---

## 4. 정방향 vs 역방향 비교

### Request

```http
GET http://localhost:8099/api/ontology/query/service/mgcoa8888S0/structure
```

저장: `test-data/queries/acceptance-structure-mgcoa8888S0.json`

### 정방향 (실측)

classification:

```text
system:MG -HAS_BUSINESS→ business:MG:CO
business:MG:CO -HAS_FUNCTION→ function:MG:CO:A
function:MG:CO:A -HAS_PROGRAM→ program:MG:CO:A:8888
program:MG:CO:A:8888 -PROVIDES_SERVICE→ service:mgcoa8888S0
```

summary:

```text
mgcoa8888S0 → Handler → Facade → Service → DAO → (SqlId) → TB_FW_IMAGE_LOG → GUID
```

(Mapper는 `mappers`/`structure`의 EXECUTES→mgcoa8888-ORA.xml로 존재; summary는 SqlId를 먼저 나열)

### 동일성 검증

| 구간 | 정방향 | 역방향(Impact) | 일치? |
|------|--------|----------------|-------|
| Handler↔Facade↔Service↔DAO↔Mapper/SQL↔Table | 있음 | 있음 | **YES** |
| ServiceId↔Program↔Business | 있음 | affected*로 있음 | **YES** |
| Function / System | 있음 | Impact에 없음 | **NO** |

결론: 호출 체인(DESIGN)은 서로 역탐색으로 대응한다.  
분류 계층의 Function/System은 Impact API가 아직 완전히 뒤집지 못한다.

---

## 5. Provenance 재확인

### Request

```http
GET http://localhost:8099/api/ontology/v1/concept/mgcoa8888S0
```

HANDLED_BY outgoing (저장: `acceptance-provenance-HANDLED_BY.json`):

| 필드 | 값 |
|------|-----|
| sourceType | `YAML_MAPPING` |
| sourcePath | `ontology/mappings/mgcoa8888.yml` |
| discoveredBy | **`YamlGraphLoader`** |
| verificationStatus | `VERIFIED` |

이전 `Mgcoa8888OntologySeed` 표기는 해소됨. 로더명과 **일치 → PASS**.

---

## 6. Git 상태 (Commit 없음)

```text
git status -- tcf-ontology-service
→ ?? tcf-ontology-service/

git diff --stat -- tcf-ontology-service
→ (empty; 전부 untracked)

git ls-files tcf-ontology-service
→ count=0
```

### Baseline 판단

| 항목 | 상태 |
|------|------|
| 소스/테스트/문서 존재 | YES |
| Git tracked baseline | **NO** (0 files) |
| 공식 Baseline 가능? | **아직 불가** — 최초 commit/add 필요 (요청에 따라 미수행) |

---

## 7. 최종 판정

### 체크리스트

| 조건 | 결과 |
|------|------|
| clean test PASS | YES |
| war PASS | YES |
| boot PASS | YES |
| Impact affected* 8종 비어있지 않음 | YES (P0 해소 증명) |
| Table→Business 역추적 API 증명 | YES (`affectedBusinesses`) |
| Table→…→Function→System까지 paths 증명 | **NO** (NOT_AVAILABLE) |
| Impact `table` 필드가 TABLE 타입 | **NO** (COLUMN으로 해석) |
| Provenance discoveredBy 일치 | YES |
| Git tracked baseline | **NO** |

### 판정

**B. 핵심 보완 후 완료 가능**

사유 (A 불가):

1. Impact `paths`가 Function/System까지 역추적을 **응답으로 증명하지 못함** (07번 프롬프트 §3/§7)
2. `impact.table`이 COLUMN으로 잘못 resolve됨 (alias 충돌)
3. Git에 tracked 파일이 없어 공식 Baseline 상태 아님

### A로 가려면 (기능 폭주 없이 결함 수정만)

1. `resolveTable`이 TABLE만 반환하도록 alias 충돌 제거  
2. Impact paths/enrich에 Function·System 포함  
3. (운영) `tcf-ontology-service` 최초 git add/commit으로 Baseline 확정  

---

## 첨부 파일

| 파일 | 내용 |
|------|------|
| `test-data/queries/acceptance-impact-TB_FW_IMAGE_LOG.json` | Impact 전체 JSON |
| `test-data/queries/acceptance-structure-mgcoa8888S0.json` | Structure 전체 JSON |
| `test-data/queries/acceptance-provenance-HANDLED_BY.json` | HANDLED_BY provenance |
| `test-data/queries/last-clean-test.log` | clean test 로그 |
| `test-data/queries/last-war.log` | war 로그 |

*본 보고서는 2026-08-10 실측(명령 출력 + HTTP JSON)에만 근거한다. 추측으로 PASS하지 않았다.*
