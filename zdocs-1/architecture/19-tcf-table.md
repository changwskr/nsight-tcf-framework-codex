# 19. TCF 테이블 아키텍처

| 항목 | 내용 |
|------|------|
| 문서 번호 | 19 |
| 제목 | TCF Table Architecture (프로젝트별 DB·테이블) |
| 상위 문서 | [architecture.md](architecture.md) |
| 관련 문서 | [07-DAO.md](07-DAO.md), [09-transaction log.md](09-transaction log.md), [10-session.md](10-session.md), [11-login.md](11-login.md), [12-cache.md](12-cache.md), [13-batch.md](13-batch.md), [18-fileupdownload.md](18-fileupdownload.md) |
| 대상 | 프레임워크·업무·운영·DB 담당자 |

---

## 1. 개요

NSIGHT TCF Framework는 **모듈별로 DB 사용 범위가 다르다**.

| 구분 | 모듈 수 | DB 특성 | 테이블 |
|------|---------|---------|--------|
| **공유 운영 DB** | `tcf-om`, `tcf-batch` | H2 file (`nsight_om`) | OM·UD·세션·거래로그 등 **23개** |
| **거래로그만 공유** | 9개 업무 WAR (목표 17) | 업무 H2 mem + 거래로그 file DS | `TCF_TX_LOG` (쓰기) |
| **DB 미사용** | `tcf-ui`, `tcf-cache`, `tcf-core` | — | 없음 |
| **레거시** | `om-service` | 업무 H2 mem | 없음 (샘플만) |

핵심 설계:

1. **운영·공통 메타**는 `tcf-om` 단일 DB(`./data/nsight-txlog/nsight_om`)에 집중
2. **거래 이력**(`TCF_TX_LOG`)은 모든 온라인 WAR가 **동일 file DB**에 적재 → OM에서 통합 조회
3. **업무 WAR**는 현재 샘플 단계로 **업무 전용 테이블 없음** — 향후 RDW/ADW 등 외부 DB 또는 mem/file 확장

---

## 2. DB 토폴로지

```text
                    ┌─────────────────────────────────────────┐
                    │  ./data/nsight-txlog/nsight_om (H2 file) │
                    │  ─────────────────────────────────────  │
                    │  TCF_TX_LOG          ← 모든 WAR 적재     │
                    │  OM_* / UD_* / SPRING_SESSION*          │
                    │  ← tcf-om CRUD, tcf-batch 수집·이력    │
                    └─────────────────────────────────────────┘
                           ▲                    ▲
              separate DS  │                    │ primary DS
                           │                    │
     ┌─────────────────────┴──┐    ┌───────────┴──────────────┐
     │  sv-service, bc-service │    │  tcf-om (8097)            │
     │  cm-service, ... (16)   │    │  tcf-batch (8098)         │
     │  ─────────────────────  │    └──────────────────────────┘
     │  jdbc:h2:mem:nsight_*   │
     │  (업무 테이블 없음)      │
     └─────────────────────────┘

     ┌─────────────────────────┐
     │  ./data/updownload/     │  ← 물리 파일 (DB 외부)
     │  {fileId}.bin           │
     └─────────────────────────┘
```

### 2.1 데이터소스 설정 요약

| 모듈 | Primary DS | 거래로그 DS | `transaction-log-enabled` |
|------|------------|-------------|---------------------------|
| `tcf-om` | `nsight_om` file | **동일** (`separate: false`) | `true` |
| `tcf-batch` | `nsight_om` file | — | `false` |
| 업무 WAR (16) | `h2:mem:nsight_{code}` | `nsight_om` file (`separate: true`, 기본) | `true` (기본) |
| `tcf-ui` | 없음 | — | — |

설정 키:

- Primary: `spring.datasource.url`
- 거래로그: `nsight.tcf.transaction-log-datasource.*`
- 공유 경로: `nsight.txlog.path` (기본 `./data/nsight-txlog`)

상세: [09-transaction log.md §5](09-transaction log.md).

---

## 3. 프로젝트별 테이블 사용 매트릭스

### 3.1 프레임워크·인프라 모듈

| 모듈 | Gradle | DB | 사용 테이블 | 비고 |
|------|--------|-----|-------------|------|
| `tcf-core` | 라이브러리 | ✕ | — | `TcfTransactionLogConstants` 상수만 정의 |
| `tcf-web` | 라이브러리 | 조건부 | `TCF_TX_LOG` | `TransactionLogSchemaInitializer` 자동 DDL |
| `tcf-cache` | 라이브러리 | ✕ | — | EhCache (메모리); 상태 스냅샷은 OM이 `OM_CACHE_STATUS`에 기록 |
| `tcf-ui` | WAR | ✕ | — | Relay 전용, DB 없음 |

### 3.2 운영·배치 모듈

| 모듈 | 포트 | 읽기(R) / 쓰기(W) 테이블 |
|------|------|--------------------------|
| **tcf-om** | 8097 | **전체 23개** R/W. `schema.sql` + `data.sql` 초기화, `OmDatabaseMigration` 보강 |
| **tcf-batch** | 8098 | `OM_AP_STATUS` W, `OM_DB_STATUS` W, `OM_SESSION_STATUS` W, `OM_DEPLOY_STATUS` W, `OM_BATCH_HISTORY` W, `SPRING_SESSION` R. `BatchDatabaseMigration` DDL |

### 3.3 업무 WAR (16개)

| 코드 | 모듈 | Primary DB (mem) | 거래로그 (file) | 업무 테이블 |
|------|------|------------------|-----------------|-------------|
| CC | `cc-service` | `nsight_cc` | `TCF_TX_LOG` | 없음 (샘플 Mapper) |
| IC | `ic-service` | `nsight_ic` | 동일 | 없음 |
| PC | `pc-service` | `nsight_pc` | 동일 | 없음 |
| BC | `bc-service` | `nsight_bc` | 동일 | 없음 |
| MS | `ms-service` | `nsight_ms` | 동일 | 없음 |
| SV | `sv-service` | `nsight_sv` | 동일 | 없음 |
| PD | `pd-service` | `nsight_pd` | 동일 | 없음 |
| CM | `cm-service` | `nsight_cm` | 동일 | 없음 |
| EB | `eb-service` | `nsight_eb` | 동일 | 없음 |
| EP | `ep-service` | `nsight_ep` | 동일 | 없음 |
| BP | `bp-service` | `nsight_bp` | 동일 | 없음 |
| BD | `bd-service` | `nsight_bd` | 동일 | 없음 |
| SS | `ss-service` | `nsight_ss` | 동일 | 없음 |
| CS | `cs-service` | `nsight_cs` | 동일 | 없음 |
| CT | `ct-service` | `nsight_ct` | 동일 | 없음 |
| MG | `mg-service` | `nsight_mg` | 동일 | 없음 |

업무 Mapper(`*SampleMapper.xml`)는 현재 **인라인 SELECT**만 사용하며 물리 테이블을 참조하지 않는다.

### 3.4 레거시

| 모듈 | 상태 | 테이블 |
|------|------|--------|
| `om-service` | **미배포**(레거시) | 없음. 운영 기능은 `tcf-om`으로 이전 |

### 3.5 UD(파일 업·다운로드)

| 논리 코드 | 실제 모듈 | 테이블 |
|-----------|-----------|--------|
| `UD` | `tcf-om` (`updownload` 패키지) | `UD_FILE_META` + 디스크 `./data/updownload` |

상세: [18-fileupdownload.md](18-fileupdownload.md).

---

## 4. 테이블 카탈로그 (공유 DB `nsight_om`)

총 **23개** 테이블. DDL: `tcf-om/src/main/resources/schema.sql`.

### 4.1 네이밍 규칙

| 접두어 | 소유 | 의미 |
|--------|------|------|
| `TCF_` | 프레임워크 | TCF 공통 (거래로그) |
| `OM_` | 운영관리 | OM Admin·배치·감사 |
| `UD_` | 공통 파일 | 업·다운로드 메타 |
| `SPRING_SESSION` | Spring Session | JDBC 세션 저장 (표준 스키마) |

### 4.2 TCF — 거래로그

#### `TCF_TX_LOG`

| 항목 | 내용 |
|------|------|
| 용도 | 온라인 거래 요약 이력 (성공/실패, 소요시간) |
| 쓰기 | 모든 업무 WAR ETF (`JdbcTransactionLogRepository`, 독립 커밋) |
| 읽기/삭제 | `tcf-om` (`OmTransactionLogHandler` — 조회·삭제 serviceId 통합) |
| 인덱스 | `(GUID, TX_TIME)`, `(SERVICE_ID, TX_TIME)`, `(USER_ID, TX_TIME)` |

| 컬럼 | 타입 | 설명 |
|------|------|------|
| `LOG_ID` | VARCHAR(64) PK | 로그 ID |
| `TX_TIME` | VARCHAR(40) | 거래 시각 (KST ISO) |
| `BUSINESS_CODE` | VARCHAR(10) | 업무코드 (SV, OM, …) |
| `SERVICE_ID` | VARCHAR(100) | 서비스 ID |
| `TRANSACTION_CODE` | VARCHAR(50) | 거래코드 |
| `GUID` | VARCHAR(64) | 거래 GUID |
| `TRACE_ID` | VARCHAR(64) | 추적 ID |
| `USER_ID` | VARCHAR(50) | 사용자 |
| `BRANCH_ID` | VARCHAR(20) | 지점 |
| `RESULT_STATUS` | VARCHAR(20) | `SUCCESS` / `FAIL` |
| `RESULT_CODE` | VARCHAR(20) | `S0000` / `E0001` |
| `ERROR_CODE` | VARCHAR(50) | 업무·시스템 오류코드 |
| `ELAPSED_TIME_MS` | BIGINT | 소요 시간(ms) |

---

### 4.3 OM — 서비스·권한·사용자

| 테이블 | 용도 | 주요 OM Handler / 화면 |
|--------|------|------------------------|
| `OM_SERVICE_CATALOG` | `serviceId`·Handler·타임아웃 메타 | ServiceId 관리 |
| `OM_USER` | OM 포털 사용자 | 사용자/권한 |
| `OM_AUTH_GROUP` | 권한 그룹 | 사용자/권한 |
| `OM_MENU` | Admin 메뉴 트리 | 로그인 후 메뉴 |
| `OM_FUNCTION_AUTH` | 메뉴별 CRUD·다운로드 권한 | 기능권한 |
| `OM_DATA_AUTH` | 지점·고객 데이터 범위 | 데이터권한 |
| `OM_AUTH_HISTORY` | 권한·설정 변경 이력 | 권한이력 |

#### `OM_SERVICE_CATALOG` 주요 컬럼

`CATALOG_ID`, `BUSINESS_CODE`, `SERVICE_ID`(UK), `TRANSACTION_CODE`, `PROCESSING_TYPE`, `HANDLER_CLASS`, `AUTH_CODE`, `AUDIT_YN`, `TIMEOUT_SEC`, `USE_YN`, `DESCRIPTION`

#### `OM_USER` 주요 컬럼

`USER_ID` PK, `USER_NAME`, `PASSWORD_HASH`(BCrypt), `BRANCH_ID`, `AUTH_GROUP_ID` → `OM_AUTH_GROUP`, `USE_YN`, `LAST_LOGIN_TIME`

---

### 4.4 OM — 감사·오류·설정

| 테이블 | 용도 | 쓰기 주체 |
|--------|------|-----------|
| `OM_AUDIT_LOG` | 고객정보 조회 등 **업무 감사** (TCF `audit-enabled`) | STF 감사 리스너 / OM 조회 |
| `OM_ERROR_CODE` | 표준 오류코드 사전 | OM Admin CRUD |
| `OM_SYSTEM_CONFIG` | 환경설정 스냅샷(조회용) | seed `data.sql` |
| `OM_COMMON_CODE` | 공통코드 (업무코드·채널 등) | OM Admin CRUD |

#### `OM_AUDIT_LOG` 주요 컬럼

`AUDIT_ID`, `AUDIT_TIME`, `USER_ID`, `BRANCH_ID`, `CUSTOMER_NO`, `FUNCTION_ID`, `FUNCTION_NAME`, `INQUIRY_REASON`, `RESULT_STATUS`, `CLIENT_IP`

---

### 4.5 OM — 모니터링·배치 (대시보드)

| 테이블 | 용도 | 쓰기 | 읽기 |
|--------|------|------|------|
| `OM_AP_STATUS` | AP CPU/Heap/Thread 스냅샷 | `tcf-batch` | OM Health Check |
| `OM_DB_STATUS` | DB Pool 사용률 | `tcf-batch` | OM Health Check |
| `OM_SESSION_STATUS` | 세션 수·활성 사용자 | `tcf-batch` | OM Health Check |
| `OM_DEPLOY_STATUS` | WAR 배포·헬스 | `tcf-batch` | OM Health Check |
| `OM_BATCH_JOB` | 배치 Job 정의·cron | seed / `OmDatabaseMigration` | OM 배치 관리 |
| `OM_BATCH_HISTORY` | 배치 실행 이력 | `tcf-batch`, OM 수동 실행 | OM 배치 관리 |
| `OM_CACHE_STATUS` | EhCache 엔트리 스냅샷 | OM Cache 조회 Handler | Cache 관리 |

`OM_AP_STATUS` 예시 컬럼: `AP_ID` PK, `AP_NAME`, `HEALTH_STATUS`, `CPU_USAGE_PCT`, `HEAP_USAGE_PCT`, `THREAD_COUNT`, `CHECKED_AT`

---

### 4.6 OM — 세션 (Spring Session JDBC)

| 테이블 | 용도 |
|--------|------|
| `SPRING_SESSION` | 세션 메타 (`PRINCIPAL_NAME` = `USER_ID`) |
| `SPRING_SESSION_ATTRIBUTES` | 세션 속성 바이너리 |

| 모듈 | 세션 저장 |
|------|-----------|
| `tcf-om` | `spring.session.store-type: jdbc` |
| 업무 WAR | `store-type: none` (OM 로그인 세션은 OM WAR) |

OM 세션 관리·만료 정리: `OmSessionHandler`, `OmSessionCleanupScheduler`, `tcf-batch` `SessionMetricsClient`.

상세: [10-session.md](10-session.md), [11-login.md](11-login.md).

---

### 4.7 OM — 파일·UD

| 테이블 | 용도 | 쓰기 | 읽기 |
|--------|------|------|------|
| `UD_FILE_META` | 업로드 파일 메타 | `OmUpdownloadService` (REST) | UD REST, OM 파일 관리 |
| `OM_FILE_DOWNLOAD_LOG` | 다운로드 감사 | `OmFileDownloadAuditListener` | `OM.FileDownload.inquiry` |

`UD_FILE_META` 주요 컬럼: `FILE_ID` PK, `ORIGINAL_NAME`, `CONTENT_TYPE`, `FILE_SIZE`, `DESCRIPTION`, `UPLOAD_USER`, `UPLOAD_TIME`, `BUSINESS_CODE`, `USE_YN`

물리 바이너리는 DB 외부: `nsight.updownload.storage-path` (기본 `./data/updownload`).

---

## 5. 테이블 관계 (개념 ER)

```text
OM_AUTH_GROUP ──< OM_USER ──< SPRING_SESSION (PRINCIPAL_NAME)
      │
      ├──< OM_FUNCTION_AUTH >── OM_MENU
      └──< OM_DATA_AUTH

OM_SERVICE_CATALOG          (독립 — serviceId 레지스트리)

TCF_TX_LOG                  (독립 — 거래 이력, BUSINESS_CODE로 업무 구분)

OM_BATCH_JOB ──< OM_BATCH_HISTORY

UD_FILE_META                (독립 — fileId)
      │
      └── 다운로드 시 ──> OM_FILE_DOWNLOAD_LOG

OM_AP_STATUS / OM_DB_STATUS / OM_SESSION_STATUS / OM_DEPLOY_STATUS
                            (배치 스냅샷, 독립)
```

---

## 6. 스키마 초기화 경로

| 경로 | 담당 | 대상 |
|------|------|------|
| `tcf-om/.../schema.sql` + `data.sql` | Spring `sql.init` (`mode: embedded`) | 공유 DB 전체 + seed |
| `OmDatabaseMigration` | `tcf-om` 기동 시 | 메뉴·카탈로그 MERGE, `UD_FILE_META`·`OM_SESSION_STATUS` 보강, 레거시 행 정리 |
| `TransactionLogSchemaInitializer` | 업무 WAR·`tcf-om` (조건부) | `TCF_TX_LOG` + 인덱스 |
| `BatchDatabaseMigration` | `tcf-batch` 기동 시 | 모니터링 4종 + `OM_BATCH_HISTORY` DDL, 레거시 행 DELETE |

업무 WAR는 Primary DB에 **DDL을 실행하지 않는다** (in-memory, 테이블 없음).

---

## 7. 프로젝트별 접근 패턴

### 7.1 tcf-om

- **MyBatis**: `OmOperationMapper.xml` — 대부분의 `OM_*`, `TCF_TX_LOG`, `SPRING_SESSION`
- **JdbcTemplate**: `OmUpdownloadService` — `UD_FILE_META`
- **JDBC 직접**: `OmFileDownloadAuditListener` — `OM_FILE_DOWNLOAD_LOG` INSERT

### 7.2 tcf-batch

- **JdbcTemplate**: `OmDashboardStatusRepository` — 모니터링 테이블 UPSERT/DELETE, `OM_BATCH_HISTORY` INSERT
- **조회**: `SessionMetricsClient` — `SPRING_SESSION` 집계

### 7.3 업무 WAR

- **MyBatis**: `mapper/{code}/*SampleMapper.xml` — 테이블 미사용 샘플
- **TCF**: `TransactionLogService` → `TCF_TX_LOG` INSERT only

### 7.4 향후 업무 테이블 추가 시 권장

| 단계 | 권장 |
|------|------|
| 1 | 업무별 `schema.sql` 또는 Flyway/Liquibase를 해당 WAR에 배치 |
| 2 | Primary DS를 `h2:file` 또는 운영 RDW/ADW URL로 전환 |
| 3 | `TCF_TX_LOG`는 **공유 file DS 유지** (`separate: true`) — OM 통합 조회 보존 |
| 4 | 테이블명: `{업무코드}_` 접두어 (예: `SV_CUSTOMER`) — `OM_`, `TCF_`, `UD_`와 충돌 방지 |

---

## 8. 물리 파일·경로

| 경로 | 내용 | 관련 모듈 |
|------|------|-----------|
| `./data/nsight-txlog/nsight_om.mv.db` | H2 공유 DB 파일 | `tcf-om`, `tcf-batch`, 업무 WAR 거래로그 |
| `./data/updownload/{fileId}.bin` | 업로드 바이너리 | `tcf-om` |

환경 변수/프로퍼티: `nsight.txlog.path`, `nsight.updownload.storage-path`

---

## 9. 시드 데이터

`tcf-om/src/main/resources/data.sql`에 로컬 개발용 초기 데이터:

- `OM_USER` 3건 (`admin01`, `op01`, `view01` — 비밀번호 `OmUserPasswordInitializer`)
- `OM_SERVICE_CATALOG` OM·SV 핸들러 등록
- `TCF_TX_LOG` 샘플 5건
- `OM_BATCH_JOB`, `OM_COMMON_CODE`, `OM_FUNCTION_AUTH` 등

운영 환경에서는 seed 대신 마이그레이션·운영 툴로 적재한다.

---

## 10. 체크리스트

**로컬 개발**

- [ ] `./data/nsight-txlog/` 디렉터리 쓰기 가능
- [ ] `tcf-om` 기동 후 H2 Console 또는 OM Admin에서 23개 테이블 확인
- [ ] 업무 WAR 거래 후 `TCF_TX_LOG` 건수 증가 확인
- [ ] `tcf-batch` 기동 후 `OM_AP_STATUS` 등 스냅샷 갱신 확인

**업무 테이블 신규 추가**

- [ ] Primary DS vs 거래로그 DS 분리 유지
- [ ] `OM_` / `TCF_` / `UD_` / `SPRING_` 접두어 예약 준수
- [ ] MyBatis mapper `namespace`·SQL ID 네이밍 ([06-naming.md](06-naming.md))

**운영**

- [ ] `nsight_om` file DB 백업·용량 모니터링
- [ ] `TCF_TX_LOG` 보존 정책 (`OM.TransactionLog.deleteAll` 또는 아카이브 배치)
- [ ] `UD_FILE_META` + `./data/updownload` 동시 백업

---

## 11. 참고 소스

| # | 경로 | 내용 |
|---|------|------|
| 1 | `tcf-om/src/main/resources/schema.sql` | 전체 DDL |
| 2 | `tcf-om/src/main/resources/data.sql` | seed 데이터 |
| 3 | `tcf-om/.../mapper/om/OmOperationMapper.xml` | OM MyBatis SQL |
| 4 | `tcf-om/.../support/OmDatabaseMigration.java` | 기동 시 MERGE·DDL |
| 5 | `tcf-batch/.../BatchDatabaseMigration.java` | 배치 DDL·정리 |
| 6 | `tcf-batch/.../OmDashboardStatusRepository.java` | 모니터링 적재 |
| 7 | `tcf-web/.../TransactionLogSchemaInitializer.java` | `TCF_TX_LOG` 자동 생성 |
| 8 | `tcf-web/.../JdbcTransactionLogRepository.java` | 거래로그 INSERT |
| 9 | `tcf-om/.../updownload/service/OmUpdownloadService.java` | `UD_FILE_META` |
| 10 | `tcf-core/.../TcfTransactionLogConstants.java` | 테이블명·기본 DS URL |
