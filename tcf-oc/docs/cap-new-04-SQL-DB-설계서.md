# NSIGHT TCF-OC NEW 용량산정 SQL·DB 설계서

| 항목 | 내용 |
|------|------|
| **문서 ID** | OC-CAP-NEW-DB-001 |
| **버전** | V1.0 |
| **작성일** | 2026-07-15 |
| **대상 DB** | H2 (개발) / RDW (운영) |
| **관련 문서** | `cap-new-03-프로그램설계서.md`, `schema.sql` |
| **문서 목적** | cap-new 기능의 테이블 정의, 인덱스, MyBatis Mapper SQL, 데이터 정책을 정의한다. |

---

## 1. 도입

### 1.1 DB 명명 규칙

| 규칙 | cap-new 적용 |
|------|-------------|
| 테이블 | `CAP_NEW_` prefix + 대문자 스네이크 |
| PK | `{엔티티}_ID` VARCHAR(50) |
| 상태 | VARCHAR(20) enum 문자열 |
| JSON | CLOB (`STEP_PAYLOAD`, `SEED_PAYLOAD`, `SNAPSHOT_JSON`) |
| 일시 | TIMESTAMP, DEFAULT CURRENT_TIMESTAMP |

### 1.2 데이터베이스 배치

| 환경 | DB | 스키마 파일 |
|------|-----|------------|
| 개발 (bootRun) | H2 in-memory | `tcf-oc/src/main/resources/schema.sql` |
| 통합 (ztomcat) | H2 / RDW | 동일 DDL 운영 반영 |
| MyBatis | `mapper/oc/CapNew*.xml` | namespace = Mapper 인터페이스 FQCN |

---

## 2. ERD

```text
CAP_NEW_SCENARIO_TEMPLATE (1) ──seed──> (N) CAP_NEW_SCENARIO
                                              │
                                              │ 1
                                              │
                                              ▼ N
                                       CAP_NEW_APPROVAL
```

### 2.1 관계 설명

| 관계 | 설명 |
|------|------|
| TEMPLATE → SCENARIO | `templateCode`로 seed payload 복사 (논리적, FK 없음) |
| SCENARIO → APPROVAL | 1:N 확정·취소 이력. SCENARIO_ID 참조 |

---

## 3. 테이블 정의

### 3.1 CAP_NEW_SCENARIO — 용량산정 시나리오

| 컬럼명 | 타입 | NULL | 기본값 | 설명 |
|--------|------|------|--------|------|
| SCENARIO_ID | VARCHAR(50) | N | — | PK. `CAP-NEW-XXXXXXXX` |
| PROJECT_ID | VARCHAR(50) | N | — | 프로젝트 식별자 |
| PROJECT_NAME | VARCHAR(200) | Y | — | 프로젝트명 |
| SCENARIO_NAME | VARCHAR(200) | N | — | 산정 시나리오명 |
| TARGET_ENV | VARCHAR(20) | Y | — | DEV/STG/PROD/DR |
| BASE_DATE | DATE | Y | — | 산정 기준일 |
| VERSION_NO | VARCHAR(20) | Y | — | V1.0, V1.1, ... |
| AUTHOR | VARCHAR(100) | Y | — | 작성자 |
| DESCRIPTION | VARCHAR(500) | Y | — | 설명 |
| PURPOSE | VARCHAR(30) | Y | — | NEW_BUILD/SCALE_REVIEW/... |
| STATUS | VARCHAR(20) | Y | DRAFT | DRAFT/COMPLETED/APPROVED |
| CURRENT_STEP | INT | Y | 1 | 현재 Wizard 단계 (1~8) |
| STEP_PAYLOAD | CLOB | Y | — | STEP 1~8 JSON |
| CREATED_AT | TIMESTAMP | Y | CURRENT_TIMESTAMP | 생성일시 |
| UPDATED_AT | TIMESTAMP | Y | CURRENT_TIMESTAMP | 수정일시 |

#### DDL

```sql
CREATE TABLE IF NOT EXISTS CAP_NEW_SCENARIO (
    SCENARIO_ID    VARCHAR(50)  NOT NULL PRIMARY KEY,
    PROJECT_ID     VARCHAR(50)  NOT NULL,
    PROJECT_NAME   VARCHAR(200),
    SCENARIO_NAME  VARCHAR(200) NOT NULL,
    TARGET_ENV     VARCHAR(20),
    BASE_DATE      DATE,
    VERSION_NO     VARCHAR(20),
    AUTHOR         VARCHAR(100),
    DESCRIPTION    VARCHAR(500),
    PURPOSE        VARCHAR(30),
    STATUS         VARCHAR(20)  DEFAULT 'DRAFT',
    CURRENT_STEP   INT          DEFAULT 1,
    STEP_PAYLOAD   CLOB,
    CREATED_AT     TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    UPDATED_AT     TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
);
```

#### 인덱스 (권장)

```sql
CREATE INDEX IDX_CAP_NEW_SCENARIO_STATUS ON CAP_NEW_SCENARIO (STATUS);
CREATE INDEX IDX_CAP_NEW_SCENARIO_PROJECT ON CAP_NEW_SCENARIO (PROJECT_ID);
CREATE INDEX IDX_CAP_NEW_SCENARIO_UPDATED ON CAP_NEW_SCENARIO (UPDATED_AT DESC);
```

#### STATUS 상태 전이

```text
DRAFT ──(STEP 8 완료)──> COMPLETED ──(approve)──> APPROVED
                              ^                        │
                              └──────(revoke)──────────┘
```

| STATUS | 설명 | 삭제 | 편집 |
|--------|------|------|------|
| DRAFT | 작성 중 | 가능 | 가능 |
| COMPLETED | 산정 완료 | 불가 | STEP 수정 가능 |
| APPROVED | 확정 | 불가 | revoke 후 수정 |

#### STEP_PAYLOAD JSON 구조

키: `step1` ~ `step8`. 상세 스키마는 `cap-new-02-거래설계서.md` §4 참조.

---

### 3.2 CAP_NEW_SCENARIO_TEMPLATE — 시나리오 템플릿 카탈로그

| 컬럼명 | 타입 | NULL | 기본값 | 설명 |
|--------|------|------|--------|------|
| TEMPLATE_CODE | VARCHAR(50) | N | — | PK. 예: NH_6000_BRANCH |
| LABEL | VARCHAR(200) | N | — | 표시명 |
| DESCRIPTION | VARCHAR(500) | Y | — | 설명 |
| PURPOSE | VARCHAR(30) | Y | — | 기본 산정 목적 |
| TARGET_ENV | VARCHAR(20) | Y | — | 기본 환경 |
| VM_PROFILE_CODE | VARCHAR(30) | Y | — | 기본 VM |
| TOTAL_USERS | INT | Y | — | 요약 지표 |
| DESIGN_PEAK_TPS | INT | Y | — | 요약 지표 |
| DEPLOYMENT_AP | INT | Y | — | 요약 지표 |
| MAX_THREADS | INT | Y | — | 요약 지표 |
| POOL_PER_VM | INT | Y | — | 요약 지표 |
| SORT_ORDER | INT | Y | 0 | 정렬 순서 |
| ENABLED | VARCHAR(1) | Y | Y | Y/N |
| SEED_PAYLOAD | CLOB | Y | — | step1~7 seed JSON |
| CREATED_AT | TIMESTAMP | Y | CURRENT_TIMESTAMP | — |
| UPDATED_AT | TIMESTAMP | Y | CURRENT_TIMESTAMP | — |

#### DDL

```sql
CREATE TABLE IF NOT EXISTS CAP_NEW_SCENARIO_TEMPLATE (
    TEMPLATE_CODE     VARCHAR(50)  NOT NULL PRIMARY KEY,
    LABEL             VARCHAR(200) NOT NULL,
    DESCRIPTION       VARCHAR(500),
    PURPOSE           VARCHAR(30),
    TARGET_ENV        VARCHAR(20),
    VM_PROFILE_CODE   VARCHAR(30),
    TOTAL_USERS       INT,
    DESIGN_PEAK_TPS   INT,
    DEPLOYMENT_AP     INT,
    MAX_THREADS       INT,
    POOL_PER_VM       INT,
    SORT_ORDER        INT          DEFAULT 0,
    ENABLED           VARCHAR(1)   DEFAULT 'Y',
    SEED_PAYLOAD      CLOB,
    CREATED_AT        TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    UPDATED_AT        TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
);
```

#### 초기 데이터

`CapNewTemplateDataInitializer` + `CapNewTemplateSeedFactory`가 기동 시 seed 삽입.

| TEMPLATE_CODE | LABEL | 용도 |
|---------------|-------|------|
| NH_6000_BRANCH | 6,000지점 운영 기준 | 기본 운영 산정 |
| (기타) | CapNewTemplateSeedFactory 정의 | — |

---

### 3.3 CAP_NEW_APPROVAL — 확정 이력

| 컬럼명 | 타입 | NULL | 기본값 | 설명 |
|--------|------|------|--------|------|
| APPROVAL_ID | VARCHAR(50) | N | — | PK |
| SCENARIO_ID | VARCHAR(50) | N | — | 시나리오 FK (논리) |
| PROJECT_ID | VARCHAR(50) | N | — | 프로젝트 (스냅샷) |
| SCENARIO_NAME | VARCHAR(200) | Y | — | 시나리오명 (스냅샷) |
| VERSION_NO | VARCHAR(20) | Y | — | 버전 (스냅샷) |
| ACTION | VARCHAR(20) | Y | APPROVE | APPROVE / REVOKE |
| APPROVER | VARCHAR(100) | Y | — | 확정자 |
| REVIEWER | VARCHAR(100) | Y | — | 검토자 |
| APPROVAL_NOTE | VARCHAR(500) | Y | — | 확정 사유 |
| OVERALL_JUDGMENT | VARCHAR(20) | Y | — | 확정 시점 종합 판정 |
| SNAPSHOT_JSON | CLOB | Y | — | STEP 8 headline 스냅샷 |
| CREATED_AT | TIMESTAMP | Y | CURRENT_TIMESTAMP | 이력 일시 |

#### DDL

```sql
CREATE TABLE IF NOT EXISTS CAP_NEW_APPROVAL (
    APPROVAL_ID       VARCHAR(50)  NOT NULL PRIMARY KEY,
    SCENARIO_ID       VARCHAR(50)  NOT NULL,
    PROJECT_ID        VARCHAR(50)  NOT NULL,
    SCENARIO_NAME     VARCHAR(200),
    VERSION_NO        VARCHAR(20),
    ACTION            VARCHAR(20)  DEFAULT 'APPROVE',
    APPROVER          VARCHAR(100),
    REVIEWER          VARCHAR(100),
    APPROVAL_NOTE     VARCHAR(500),
    OVERALL_JUDGMENT  VARCHAR(20),
    SNAPSHOT_JSON     CLOB,
    CREATED_AT        TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
);
```

#### 인덱스 (권장)

```sql
CREATE INDEX IDX_CAP_NEW_APPROVAL_SCENARIO ON CAP_NEW_APPROVAL (SCENARIO_ID);
CREATE INDEX IDX_CAP_NEW_APPROVAL_CREATED ON CAP_NEW_APPROVAL (CREATED_AT DESC);
```

#### SNAPSHOT_JSON 예시

```json
{
  "overallJudgment": "WARN",
  "designPeakTps": 1200,
  "deploymentAp": 8,
  "maxThreads": 300,
  "poolPerVm": 40,
  "totalDbSessions": 320,
  "vmProfileCode": "16CORE-128GB"
}
```

---

## 4. MyBatis Mapper SQL

### 4.1 CapNewScenarioMapper

**namespace**: `com.nh.nsight.marketing.oc.capnew.persistence.mapper.CapNewScenarioMapper`

| SQL ID | 유형 | 설명 | timeout |
|--------|------|------|---------|
| selectAll | SELECT | 전체 목록, UPDATED_AT DESC | default |
| selectById | SELECT | PK 조회 | default |
| insert | INSERT | 신규 시나리오 | default |
| update | UPDATE | STEP 저장·상태 변경, UPDATED_AT 갱신 | default |
| deleteById | DELETE | DRAFT 삭제 | default |

#### selectAll

```xml
<select id="selectAll" resultType="...CapNewScenarioRow">
    SELECT SCENARIO_ID, PROJECT_ID, PROJECT_NAME, SCENARIO_NAME,
           TARGET_ENV, CAST(BASE_DATE AS VARCHAR) AS BASE_DATE,
           VERSION_NO, AUTHOR, DESCRIPTION, PURPOSE,
           STATUS, CURRENT_STEP, STEP_PAYLOAD,
           CAST(CREATED_AT AS VARCHAR) AS CREATED_AT,
           CAST(UPDATED_AT AS VARCHAR) AS UPDATED_AT
    FROM CAP_NEW_SCENARIO
    ORDER BY UPDATED_AT DESC
</select>
```

#### update (핵심)

```xml
<update id="update" parameterType="...CapNewScenarioRow">
    UPDATE CAP_NEW_SCENARIO
    SET PROJECT_ID = #{projectId},
        PROJECT_NAME = #{projectName},
        SCENARIO_NAME = #{scenarioName},
        TARGET_ENV = #{targetEnv},
        BASE_DATE = #{baseDate},
        VERSION_NO = #{versionNo},
        AUTHOR = #{author},
        DESCRIPTION = #{description},
        PURPOSE = #{purpose},
        STATUS = #{status},
        CURRENT_STEP = #{currentStep},
        STEP_PAYLOAD = #{stepPayload},
        UPDATED_AT = CURRENT_TIMESTAMP
    WHERE SCENARIO_ID = #{scenarioId}
</update>
```

### 4.2 CapNewApprovalMapper

**namespace**: `com.nh.nsight.marketing.oc.capnew.persistence.mapper.CapNewApprovalMapper`

| SQL ID | 유형 | 설명 |
|--------|------|------|
| selectAll | SELECT | 전체 확정 이력 |
| selectByScenarioId | SELECT | 시나리오별 이력 |
| insert | INSERT | APPROVE/REVOKE 이력 |

### 4.3 CapNewScenarioTemplateMapper

**namespace**: `com.nh.nsight.marketing.oc.capnew.persistence.mapper.CapNewScenarioTemplateMapper`

| SQL ID | 유형 | 설명 |
|--------|------|------|
| selectAllEnabled | SELECT | ENABLED='Y', SORT_ORDER |
| selectByCode | SELECT | PK 조회 |

---

## 5. DAO 설계

### 5.1 CapNewScenarioDao

| 메서드 | Mapper 호출 | 비고 |
|--------|------------|------|
| `findAll()` | selectAll | — |
| `findById(id)` | selectById | 없으면 예외 |
| `insert(row)` | insert | — |
| `update(row)` | update | STEP_PAYLOAD CLOB |
| `deleteById(id)` | deleteById | Service에서 DRAFT 검증 |

### 5.2 CapNewApprovalDao

| 메서드 | Mapper 호출 |
|--------|------------|
| `findAll()` | selectAll |
| `findByScenarioId(id)` | selectByScenarioId |
| `insert(row)` | insert |

### 5.3 CapNewScenarioTemplateDao

| 메서드 | Mapper 호출 |
|--------|------------|
| `findAllEnabled()` | selectAllEnabled |
| `findByCode(code)` | selectByCode |

---

## 6. 데이터 정책

### 6.1 보존·삭제

| 대상 | 정책 |
|------|------|
| DRAFT 시나리오 | 사용자 삭제 가능 |
| COMPLETED/APPROVED | 삭제 불가. revoke·clone으로 관리 |
| APPROVAL 이력 | 물리 삭제 금지. 감사 추적 |
| TEMPLATE | ENABLED='N'으로 비활성화 |

### 6.2 CLOB 관리

| 컬럼 | 예상 크기 | 비고 |
|------|----------|------|
| STEP_PAYLOAD | 10~50 KB | step1~8 전체 |
| SEED_PAYLOAD | 5~20 KB | 템플릿 seed |
| SNAPSHOT_JSON | 1~5 KB | 확정 시점 headline |

JSON 직렬화: Jackson `ObjectMapper`. Service 계층에서 read/write.

### 6.3 동시성

현재 Last-Write-Wins. `UPDATED_AT` 기반 낙관적 잠금은 향후 과제.

---

## 7. 운영 DB 반영 체크리스트

| # | 항목 | 완료 |
|---|------|------|
| 1 | CAP_NEW_SCENARIO DDL 반영 | □ |
| 2 | CAP_NEW_SCENARIO_TEMPLATE DDL + seed | □ |
| 3 | CAP_NEW_APPROVAL DDL | □ |
| 4 | 인덱스 3종 생성 | □ |
| 5 | MyBatis mapper 스캔 확인 | □ |
| 6 | RDW CLOB 타입 호환 (VARCHAR 대용량 또는 CLOB) | □ |
| 7 | 백업·보존 기간 정책 | □ |

---

## 8. 기존 테이블과의 관계

cap-new 테이블은 기존 용량산정 테이블과 **FK 없이 독립**한다.

| 기존 | cap-new 관계 |
|------|-------------|
| TB_CAPACITY_* (레거시) | legacy-compare 시 내부 서비스 호출만 |
| ENV 설정 테이블 | env-handoff 시 CapacityPlannerRequest 변환 |
| TCF_OM_* | 무관 |
| SPRING_SESSION | 무관 |

---

## 9. SQL 작성 기준 준수

| 기준 | 적용 |
|------|------|
| namespace = Mapper FQCN | 준수 |
| resultType = Row DTO FQCN | 준수 |
| SELECT * 금지 | 컬럼 명시 |
| PK 조건 WHERE | selectById, update, delete |
| TIMESTAMP CAST | H2 호환 `CAST(... AS VARCHAR)` |
| statement timeout | default (5초). CLOB 대용량 시 10초 검토 |

---

## 10. 샘플 데이터

### 10.1 시나리오 INSERT 예시

```sql
INSERT INTO CAP_NEW_SCENARIO (
    SCENARIO_ID, PROJECT_ID, PROJECT_NAME, SCENARIO_NAME,
    TARGET_ENV, BASE_DATE, VERSION_NO, AUTHOR, PURPOSE,
    STATUS, CURRENT_STEP, STEP_PAYLOAD
) VALUES (
    'CAP-NEW-SAMPLE01',
    'NSIGHT-MP',
    'NSIGHT 마케팅플랫폼',
    '2026 운영용량 기준안',
    'PROD',
    DATE '2026-07-12',
    'V1.0',
    '홍길동',
    'NEW_BUILD',
    'DRAFT',
    1,
    '{"step1":{"projectId":"NSIGHT-MP","scenarioName":"2026 운영용량 기준안"}}'
);
```

### 10.2 확정 이력 INSERT 예시

```sql
INSERT INTO CAP_NEW_APPROVAL (
    APPROVAL_ID, SCENARIO_ID, PROJECT_ID, SCENARIO_NAME, VERSION_NO,
    ACTION, APPROVER, REVIEWER, OVERALL_JUDGMENT, SNAPSHOT_JSON
) VALUES (
    'APPR-SAMPLE01',
    'CAP-NEW-SAMPLE01',
    'NSIGHT-MP',
    '2026 운영용량 기준안',
    'V1.0',
    'APPROVE',
    '김운영',
    '이검토',
    'NORMAL',
    '{"designPeakTps":1200,"deploymentAp":8}'
);
```

---

## 11. 장 요약

cap-new DB는 **CAP_NEW_SCENARIO**(핵심), **CAP_NEW_SCENARIO_TEMPLATE**(seed), **CAP_NEW_APPROVAL**(이력) 3테이블로 구성된다. STEP 1~8 데이터는 `STEP_PAYLOAD` CLOB JSON에 통합 저장하며, MyBatis 3 Mapper XML로 CRUD한다. 기존 용량산정 테이블과 물리 FK 없이 독립 운영한다.
