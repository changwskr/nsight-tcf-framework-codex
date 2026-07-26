# 47. 데이터 거버넌스 설계서

| 항목 | 내용 |
|------|------|
| 문서 번호 | 47 |
| 제목 | Data Governance Design |
| 상위 문서 | [architecture.md](architecture.md) |
| 관련 문서 | [19-tcf-table.md](19-tcf-table.md), [07-DAO.md](07-DAO.md), [26-mybatis.md](26-mybatis.md), [37-transaction-log.md](37-transaction-log.md), [46-service-integration-contract.md](46-service-integration-contract.md) |
| 상세 매뉴얼 | [zman/19-DB-테이블.md](../../zman/19-DB-테이블.md), [zdoc/테이블정보.md](../../zdoc/테이블정보.md) |
| 대상 | 업무·DB·운영·아키텍트 담당자 |

---

## 1. 문서 목적

본 문서는 NSIGHT TCF의 **데이터 소유·접근·품질·보존·변경** 거버넌스 기준을 정의한다.

| 구분 | 담당 문서 |
|------|-----------|
| 물리 테이블·DS 토폴로지 | [19-tcf-table.md](19-tcf-table.md) |
| DAO·MyBatis 계층 | [07-DAO.md](07-DAO.md), [26-mybatis.md](26-mybatis.md) |
| 서비스 간 데이터 접근 | [46-service-integration-contract.md](46-service-integration-contract.md) |
| **거버넌스 원칙·역할·정책** | **본 문서 (47)** |

핵심 문장:

> 데이터는 **논리 DB(RDW/ADW/SESSIONDB/LOGDB/OMDB)별로 소유권이 분리**되며, 기준정보는 **OM을 단일 진실 공급원(SoT)** 으로 등록·승인·이력을 남긴다.

---

## 2. 논리 DB 분류·소유권

### 2.1 DB 맵

```text
업무 SQL     → RDW (실시간 OLTP Read/Write)
분석 SQL     → ADW (Analytical — 온라인 경로 분리)
세션 SQL     → SESSIONDB (Spring Session, Gateway 세션)
운영 SQL     → OMDB (사용자·Catalog·통제·코드)
로그 SQL     → LOGDB (TCF_TX_LOG, 감사, Gateway 로그)
Gateway SQL  → Gateway Route DB
JWT SQL      → JWT Token / Denylist
파일 메타    → OMDB (UD_*) + 디스크 스토리지
```

### 2.2 접근 주체 매트릭스

| 주체 | RDW/ADW | SESSIONDB | LOGDB | OMDB |
|------|---------|-----------|-------|------|
| 업무 WAR (`*-service`) | ● R/W (업무 DS) | ○ 간접 | ● W (`TCF_TX_LOG`) | ✕ |
| tcf-om | ○ | ● R/W | ● R/W | ● R/W |
| tcf-gateway | ✕ | ● R | ● W (GW 로그) | ○ (Route) |
| tcf-batch | ● ping | ● R (집계) | ○ | ● W (스냅샷) |
| tcf-ui | ✕ | ✕ | ✕ | ✕ |

● 허용 · ○ 제한적 · ✕ 금지

### 2.3 DB 선택 의사결정

```text
데이터를 저장/조회하는가?
  ├─ 세션·로그인?           → SESSIONDB
  ├─ 거래 추적·감사 이력?    → LOGDB
  ├─ 실시간 업무 조회/변경?  → RDW
  ├─ 분석·통계·대량?        → ADW
  └─ 운영 기준정보·통제?     → OMDB
```

---

## 3. 데이터 소유·스튜어드십

| 데이터 영역 | SoT (진실 공급원) | 스튜어드 | 변경 경로 |
|-------------|-------------------|----------|-----------|
| `serviceId`·Handler 메타 | `OM_SERVICE_CATALOG` | 프레임워크·업무 아키텍트 | OM Admin |
| Header 7 거래통제 | `TCF_TRANSACTION_CONTROL` | 운영·보안 | OM Admin |
| Timeout 정책 | `TCF_SERVICE_TIMEOUT_POLICY` | 운영 | OM Admin / seed |
| 공통코드·채널 | `OM_COMMON_CODE` | 운영 | OM Admin |
| 오류코드 사전 | `OM_ERROR_CODE` | 프레임워크·업무 | OM Admin |
| 사용자·권한 | `OM_USER`, `OM_AUTH_GROUP` | 보안·운영 | OM Admin |
| 데이터권한(지점·고객 범위) | `OM_DATA_AUTH` | 보안·업무 | OM Admin |
| 전문 Body 메타 | `OM_MESSAGE_STRUCT` / `OM_MESSAGE_FIELD` | 업무·연동 | OM Admin |
| 업무 거래 데이터 | 업무 RDW 스키마 | 업무 도메인 오너 | 업무 WAR (승인된 DDL) |
| 거래 이력 | `TCF_TX_LOG` | 플랫폼 (자동) | TCF ETF — **수동 수정 금지** |
| 감사 | `OM_AUDIT_LOG` | 보안·준법 | TCF 감사 리스너 + OM 조회 |

**운영 서버 직접 DML 금지** — 변경은 OM·CI/CD·승인된 스크립트만 ([68-운영-전환-체크리스트.md](../../znsight-man/68-운영-전환-체크리스트.md)).

---

## 4. 접근 통제 거버넌스

### 4.1 기능 권한 vs 데이터 권한 vs 거래통제

| 계층 | 질문 | 저장소 | 검증 위치 |
|------|------|--------|-----------|
| **기능 권한** | 메뉴·기능을 쓸 수 있는가? | `OM_FUNCTION_AUTH` | STF·OM |
| **데이터 권한** | 어떤 지점·고객 범위인가? | `OM_DATA_AUTH` | Service/Rule (업무) |
| **거래통제** | 이 Header 7 조합이 허용됐는가? | `TCF_TRANSACTION_CONTROL` | STF (최종) |

세 계층은 **독립**이다. 기능 권한이 있어도 거래통제 미등록 시 차단 (`E-TCF-CTL-*`).

### 4.2 업무 데이터 접근 규칙

| 규칙 | 설명 |
|------|------|
| G1 | 업무 DAO는 **자기 WAR Primary DS(RDW)** 만 사용 |
| G2 | 타 업무 DB **직접 JDBC 금지** — API 연동 ([46-service-integration-contract.md](46-service-integration-contract.md)) |
| G3 | SQL은 Mapper XML에만 — Service에서 문자열 SQL 금지 |
| G4 | 조회 시 `OM_DATA_AUTH`·`branchId`·`userId` 기준 필터 (업무 Rule) |
| G5 | 대량 조회·리포트는 **ADW** — 온라인 RDW 부하 분리 |

### 4.3 Pool 분리

| Pool | DS | 용도 |
|------|-----|------|
| Primary | `spring.datasource` | RDW 업무 |
| ADW (목표) | `nsight.datasource.adw` | 분석 전용 |
| Transaction Log | `nsight.tcf.transaction-log-datasource` | LOGDB (`separate: true`) |
| Session (OM) | `spring.session` JDBC | SESSIONDB |

Hikari `pool-name`으로 구분·모니터링 (`OM_DB_STATUS`).

---

## 5. 기준정보(Master Data) 거버넌스

### 5.1 등록·변경 절차

```text
1. 요청 — 업무/운영 (변경 사유·영향 범위)
2. 검토 — 아키텍트 (serviceId 명명·Contract 정합)
3. 등록 — OM Admin (Catalog·TC·Message·ErrorCode)
4. 배포 — WAR (Handler 코드) + OM 기준정보 동기
5. 검증 — Smoke · sample-requests
6. 이력 — OM_AUTH_HISTORY / 배포 이력
```

### 5.2 `OM_SERVICE_CATALOG` Contract

| 컬럼군 | 거버넌스 |
|--------|----------|
| `SERVICE_ID` (UK) | [06-naming.md](06-naming.md) 명명 준수 |
| `HANDLER_CLASS` | 배포 WAR에 실제 존재 |
| `TIMEOUT_SEC` | `TCF_SERVICE_TIMEOUT_POLICY`와 정합 |
| `AUDIT_YN` | 감사 대상 여부 — 준법 검토 |
| `USE_YN` | 폐기 시 `N` — 물리 삭제 지양 |

### 5.3 전문 메타 (`OM_MESSAGE_STRUCT`)

업무 Body 필드의 **운영 등록 메타** (길이·필수·마스킹). 프레임워크 Header/Result는 `TcfStandardMessageCatalog` (tcf-core)가 SoT.

| 항목 | 규칙 |
|------|------|
| 필드 추가 | 하위 호환 — optional 우선 |
| 필수 변경 | 양쪽 WAR·샘플 JSON·연동 Contract 동시 갱신 |
| PII 필드 | `MASK_YN`·감사 정책 연계 |

---

## 6. 스키마·DDL 거버넌스

### 6.1 스키마 초기화 경로

| 경로 | 담당 | 대상 |
|------|------|------|
| `tcf-om/.../schema.sql` + `data.sql` | Spring `sql.init` | OMDB 전체 + seed |
| `TransactionLogSchemaInitializer` | tcf-web 자동 | `TCF_TX_LOG` |
| `OmDatabaseMigration` | tcf-om 기동 | 스키마 보강 |
| `BatchDatabaseMigration` | tcf-batch | 배치 테이블 |
| 업무 WAR | 향후 `schema-{code}.sql` | RDW 업무 테이블 |

### 6.2 DDL 변경 원칙

| 유형 | 승인 | 롤백 |
|------|------|------|
| OM 공유 DB (`OM_*`, `TCF_*`) | 운영 CAB + OM 배포 | 스크립트 역적용 ([45-disaster-recovery.md](45-disaster-recovery.md)) |
| 업무 RDW | 업무 오너 + DBA | PITR / 역 DDL |
| 인덱스 추가 | DBA 검토 | DROP INDEX |
| 컬럼 삭제 | **금지 우선** — `USE_YN` 폐기 패턴 |

로컬 H2 `MODE=Oracle` — 운영 Oracle과 타입·길이 정합 유지.

### 6.3 네이밍 (테이블·컬럼)

| 대상 | 규칙 | 예 |
|------|------|-----|
| 테이블 접두 | `OM_`, `TCF_`, `UD_`, 업무 `EB_` | `OM_SERVICE_CATALOG` |
| 컬럼 | `UPPER_SNAKE` | `REG_DT`, `USE_YN` |
| 공통 컬럼 | 등록·수정·사용여부 | `REG_USER`, `UPD_DT`, `USE_YN` |
| PK | `{엔티티}_ID` 또는 업무 키 | `CATALOG_ID`, `USER_ID` |

상세: [06-naming.md](06-naming.md), [19-tcf-table.md](19-tcf-table.md) §4.1

---

## 7. 로그·감사·개인정보 거버넌스

### 7.1 로그 계층

| 계층 | 저장 | 용도 | 보존 (권장) |
|------|------|------|-------------|
| 앱 로그 | 파일/ELK | 장애 분석 | 30~90일 |
| `transaction.log` | 파일 | TX_START/END | 90일 |
| `TCF_TX_LOG` | LOGDB | 거래 요약·OM 조회 | 규정 (통상 1~5년) |
| `OM_AUDIT_LOG` | LOGDB | 고객정보 조회 감사 | 규정 |
| `OM_FILE_DOWNLOAD_LOG` | LOGDB | 파일 다운로드 감사 | 규정 |
| `OM_ACCESS_LOG` | LOGDB | 접근 이력 | 1년+ |

설정: `nsight.tcf.audit-enabled` (기본 `true`) — [37-transaction-log.md](37-transaction-log.md)

### 7.2 PII·마스킹

| 구분 | 정책 |
|------|------|
| 로그·TX_LOG | `customerNo` 등 — 필요 최소 기록, 마스킹 규칙 적용 |
| 콘솔 전문 로그 | 운영 `prod`에서 payload 축소·마스킹 권장 |
| `OM_AUDIT_LOG` | `CUSTOMER_NO`, `INQUIRY_REASON` 필수 — 준법 검토 |
| 파일 | `UD_FILE_META` — 다운로드 시 감사 필수 |

### 7.3 로그 무결성

- `TCF_TX_LOG` — ETF **독립 커밋** (업무 TX 롤백과 분리)
- 운영자 **수정·삭제** — OM `OmTransactionLogHandler` 삭제 API만 (권한·이력)
- `guid`로 상관 — 위변조 탐지는 DB 권한·백업으로 보호

---

## 8. 데이터 품질·일관성

### 8.1 품질 차원

| 차원 | TCF 적용 |
|------|----------|
| **정확성** | Rule 검증, `StandardHeaderValidator` |
| **완전성** | Catalog·TC 등록 필수 |
| **일관성** | `businessCode` ↔ `serviceId` ↔ Handler 정합 |
| **적시성** | `systemDate`/`bizDate`, 배치 ADW 적재 |
| **유일성** | `TCF_IDEMPOTENCY_KEY`, UK (`SERVICE_ID`) |

### 8.2 교차 검증 체크

| 검증 | 방법 |
|------|------|
| Catalog ↔ Handler | 기동 시 Dispatcher 등록 vs DB `USE_YN=Y` |
| TC ↔ sample-requests | Smoke Test |
| ErrorCode ↔ 코드 | `OM_ERROR_CODE` vs `BusinessException` |
| Message ↔ Body | 통합 테스트·OM 메타 대조 |

### 8.3 Cache와 기준정보

EhCache로 TC·Timeout 캐시 — OM 변경 후 **Cache Evict** (`OM.Cache.delete`, 권한 `ROLE_OM_CACHE`). [12-cache.md](12-cache.md)

---

## 9. 파일·비정형 데이터

| 항목 | 거버넌스 |
|------|----------|
| 메타 | `UD_FILE_META` (OMDB) |
| 바이너리 | `nsight.updownload.storage-path` — 디스크 백업 별도 |
| 업로드 | `businessCode`·`UPLOAD_USER` 기록 |
| 다운로드 | `OM_FILE_DOWNLOAD_LOG` 감사 필수 |
| 보존 | `USE_YN=N` 논리 삭제 — 물리 삭제는 배치·정책 |

상세: [18-fileupdownload.md](18-fileupdownload.md)

---

## 10. 환경·설정 데이터

| 설정 | SoT | 거버넌스 |
|------|-----|----------|
| DS URL·Pool | Git `application-{profile}.yml` | 환경별 분리, 비밀은 vault |
| `nsight.txlog.path` | 배포 표준 | OM·업무 WAR 동일 경로 |
| `nsight.integration` | 호출 측 WAR yml | [46-service-integration-contract.md](46-service-integration-contract.md) |
| `OM_SYSTEM_CONFIG` | OM seed (조회용 스냅샷) | 런타임 SoT는 yml — drift 방지 |

**운영 yml 직접 수정 금지** — Git → CI/CD → 배포.

---

## 11. 배치·ADW 거버넌스 (목표)

| 원칙 | 설명 |
|------|------|
| 온라인 분리 | 배치·리포트는 RDW 피크 시간 회피 |
| ADW 적재 | ETL 스케줄·데이터 계보(document lineage) 기록 |
| `tcf-batch` | 상태 스냅샷만 OMDB — 업무 데이터 변경 아님 |
| Lock | `OM_BATCH_LOCK` — 중복 실행 방지 |

---

## 12. 변경 영향 분석 (체크리스트)

기준정보·스키마 변경 시:

- [ ] 영향 `serviceId`·업무코드 목록
- [ ] 연동 Contract ([46](46-service-integration-contract.md)) — body 필드
- [ ] 거래통제·Timeout·감사 정책
- [ ] Gateway Route·JWT (해당 시)
- [ ] 롤백 스크립트·DR RPO ([45](45-disaster-recovery.md))
- [ ] Smoke·업무 담당자 확인
- [ ] OM/배포 이력 기록

---

## 13. 역할 (RACI 요약)

| 활동 | 업무 | DBA | 운영 | 보안 | 아키텍트 |
|------|:----:|:---:|:----:|:----:|:--------:|
| RDW 스키마 설계 | R | A | C | I | C |
| OM Catalog 등록 | C | I | R | C | A |
| 거래통제·Timeout | I | I | R | A | C |
| 감사·PII 정책 | C | I | C | A | C |
| LOGDB 보존·삭제 | I | A | R | A | I |
| 서비스 연동 승인 | R | I | C | C | A |

---

## 14. 관련 소스

| 경로 | 설명 |
|------|------|
| `tcf-om/.../schema.sql` | OMDB DDL SoT |
| `tcf-om/.../data.sql` | seed 기준정보 |
| `tcf-om/.../OmDataAuthService.java` | 데이터권한 |
| `tcf-om/.../MessageStructureSeedData.java` | 전문 메타 seed |
| `tcf-core/.../TcfStandardMessageCatalog` | 표준 Header/Result |
| `tcf-web/.../TransactionLogSchemaInitializer` | TX_LOG DDL |
| `zarchitecture/09-데이터-DB-아키텍처.md` | 논리 DB 상세 |

---

← [46-service-integration-contract.md](46-service-integration-contract.md) · [48-multi-module-dependencies.md](48-multi-module-dependencies.md) →
