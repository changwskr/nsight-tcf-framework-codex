# 52. OM 운영 아키텍처 설계서

| 항목 | 내용 |
|------|------|
| 문서 번호 | 52 |
| 제목 | Operations Management (OM) Architecture Design |
| 상위 문서 | [architecture.md](architecture.md) |
| 관련 문서 | [19-tcf-table.md](19-tcf-table.md), [39-header-transaction-control.md](39-header-transaction-control.md), [41-service-timeout-policy.md](41-service-timeout-policy.md), [47-data-governance.md](47-data-governance.md), [49-release-strategy.md](49-release-strategy.md), [51-api-gateway.md](51-api-gateway.md) |
| 상세 매뉴얼 | [zarchitecture/05-운영관리-OM-아키텍처.md](../../zarchitecture/05-운영관리-OM-아키텍처.md), [zman/12-OM운영관리.md](../../zman/12-OM운영관리.md) |
| 구현 모듈 | `tcf-om` (레거시 `om-service` **미사용**) |
| 대상 | 운영·프레임워크·업무 아키텍트, OM 개발자 |

---

## 1. 문서 목적

본 문서는 **`tcf-om`** 이 담당하는 **운영 기준정보 원장(System of Record)** · Admin API · 플랫폼 연계를 정의한다.

| 구분 | 담당 문서 |
|------|-----------|
| OMDB 테이블 | [19-tcf-table.md](19-tcf-table.md) |
| 거래통제·Timeout | [39-header-transaction-control.md](39-header-transaction-control.md), [41-service-timeout-policy.md](41-service-timeout-policy.md) |
| 기준정보 거버넌스 | [47-data-governance.md](47-data-governance.md) |
| 배포 승인·CI | [49-release-strategy.md](49-release-strategy.md) |
| **OM 운영 아키텍처 (본 문서)** | Handler·UI·연계·등록 절차 |

핵심 문장:

> OM은 **업무 도메인이 아니라 운영 메타의 SoT** 이다. Catalog·거래통제·Timeout·권한·로그 조회가 여기서 등록되고, 업무 STF는 **조회만** 한다.

---

## 2. OM 위치·경계

### 2.1 담당 / 비담당

| OM 담당 | OM 비담당 |
|---------|-----------|
| 사용자·권한·메뉴·데이터권한 | SV/IC 등 **업무 거래** 로직 |
| `OM_SERVICE_CATALOG` | 실시간 마케팅 처리 |
| `TCF_TRANSACTION_CONTROL` (Allow-List) | |
| `TCF_SERVICE_TIMEOUT_POLICY` | |
| 공통코드·오류코드·전문 메타 | |
| 통합 거래로그·감사로그 **조회** | |
| 세션 관리·`SPRING_SESSION` (JDBC) | |
| 배포 요청·승인·이력 (통제) | CI/CD **실행** (Runner) |
| 대시보드·헬스 스냅샷 **조회** | `tcf-batch` **수집** |
| 파일 업·다운로드 (UD) | Gateway Route **실행** |

### 2.2 배포 식별

| 항목 | 값 |
|------|-----|
| 모듈 | `tcf-om` |
| 업무코드 | `OM` |
| bootRun | **8097** |
| Context | `/om` |
| WAR | `tcf-om.war` → `om.war` |
| 패키지 | `com.nh.nsight.marketing.om` |

의존: `tcf-util`, `tcf-core`, `tcf-web`, **`tcf-cache`** (유일한 cache 의존 WAR) — [48-multi-module-dependencies.md](48-multi-module-dependencies.md).

---

## 3. 처리 아키텍처

업무 WAR와 **동일 TCF 파이프라인**:

```text
POST /om/online
  StandardRequest (header.serviceId = OM.{Domain}.{action})
        ▼
OnlineTransactionController → TCF.process()
  STF → Dispatcher → Om{Domain}Handler
        ▼
Om{Domain}Facade (@Transactional)
        ▼
Service → Rule → OmOperationDao / 전용 Dao
        ▼
OMDB (./data/nsight-txlog/nsight_om)
        ▼
ETF → StandardResponse + TCF_TX_LOG
```

Admin UI(`tcf-ui` / `tcf-uj`)는 **Relay**로 동일 API 호출 — `POST /api/relay/OM/online` 또는 Gateway 경유.

---

## 4. Handler 도메인 (24개)

설계서 83 Handler → 코드 **24 Handler** (도메인당 1 클래스, `serviceIds()` + `switch`).

| Handler | serviceId prefix | 운영 영역 |
|---------|------------------|-----------|
| `OmAuthHandler` | `OM.Auth.*` | login, ssoLogin, logout, session |
| `OmUserHandler` | `OM.User.*` | 사용자 |
| `OmAuthGroupHandler` | `OM.AuthGroup.*` | 권한 그룹 |
| `OmAuthHistoryHandler` | `OM.AuthHistory.*` | 권한 변경 이력 |
| `OmMenuHandler` | `OM.Menu.*` | 메뉴 트리 |
| `OmFunctionAuthHandler` | `OM.FunctionAuth.*` | 기능 권한 |
| `OmDataAuthHandler` | `OM.DataAuth.*` | 데이터 권한(지점·범위) |
| `OmServiceCatalogHandler` | `OM.ServiceCatalog.*` | **ServiceId 마스터** |
| `OmTransactionControlHandler` | `OM.TransactionControl.*` | **Header 7 거래통제** |
| `OmTimeoutPolicyHandler` | `OM.TimeoutPolicy.*` | Timeout 정책 |
| `OmCommonCodeHandler` | `OM.CommonCode.*` | 공통코드 |
| `OmErrorCodeHandler` | `OM.ErrorCode.*` | 오류코드 사전 |
| `OmMessageStructureHandler` | `OM.MessageStructure.*` | 전문 Body 메타 |
| `OmTransactionLogHandler` | `OM.TransactionLog.*` | 거래로그 조회·삭제 |
| `OmAuditLogHandler` | `OM.AuditLog.*` | 감사로그 |
| `OmSessionHandler` | `OM.Session.*` | 세션·강제 로그아웃 |
| `OmBatchHandler` | `OM.Batch.*` | 배치 Job·스케줄 |
| `OmDeployHandler` | `OM.Deploy.*` | **배포 요청·승인·롤백** |
| `OmDashboardHandler` | `OM.Dashboard.*` | 대시보드 |
| `OmHealthCheckHandler` | `OM.HealthCheck.*` | 헬스 |
| `OmCacheHandler` | `OM.Cache.*` | EhCache 조회·삭제 |
| `OmFileDownloadHandler` | `OM.File.*` | 파일·다운로드 감사 |
| `OmSystemConfigHandler` | `OM.SystemConfig.*` | 환경설정 조회 |
| `OmSampleHandler` | `OM.Sample.*` | 샘플 |

경로: `tcf-om/.../entry/handler/`

---

## 5. 기준정보 SoT (Catalog · 통제 · Timeout)

### 5.1 `OM_SERVICE_CATALOG`

| 역할 | 전 플랫폼 serviceId **마스터** |
|------|-------------------------------|
| 주요 컬럼 | `SERVICE_ID`(UK), `BUSINESS_CODE`, `HANDLER_CLASS`, `TRANSACTION_CODE`, `TIMEOUT_SEC`, `AUDIT_YN`, `USE_YN` |
| 소비자 | 업무 STF (Catalog 존재), Dispatcher (Handler 매핑), OM Admin |

### 5.2 `TCF_TRANSACTION_CONTROL`

Header **7 Allow-List** — 권한이 있어도 미등록 조합은 차단 (`E-TCF-CTL-*`).  
관리: `OmTransactionControlHandler` · STF `TransactionControlValidator` · EhCache ([12-cache.md](12-cache.md)).

### 5.3 `TCF_SERVICE_TIMEOUT_POLICY`

serviceId·업무코드별 온라인·연동·쿼리 Timeout.  
관리: `OmTimeoutPolicyHandler` · STF `TimeoutPolicyService`.

### 5.4 신규 serviceId 등록 절차

```text
1. 업무 Handler 코드 배포 준비
2. OM.ServiceCatalog.save — Catalog INSERT/UPDATE
3. OM.TransactionControl.save — Allow-List (Header 7)
4. OM.TimeoutPolicy.save — Timeout
5. (선택) OM.ErrorCode, OM.FunctionAuth, OM.MessageStructure
6. OM.Cache.delete — 거래통제·코드 캐시 Evict
7. Smoke — sample-requests + /online
8. 배포 이력·변경 사유 기록
```

상세 거버넌스: [47-data-governance.md](47-data-governance.md) §5.

### 5.5 Seed

| 경로 | 용도 |
|------|------|
| `tcf-om/.../schema.sql` | DDL |
| `tcf-om/.../data.sql` | 초기 Catalog·TC·사용자 |
| `ServiceCatalogSeedData.java` | 프로그램 seed 보강 |
| `MessageStructureSeedData.java` | 전문 메타 샘플 |

---

## 6. 인증·세션

### 6.1 OM Auth

| serviceId | 역할 |
|-----------|------|
| `OM.Auth.login` | ID/PW → Spring Session JDBC |
| `OM.Auth.ssoLogin` | SSO → `tcf-jwt` 연동 (`OmJwtSsoClient`) |
| `OM.Auth.logout` | 세션 무효화 |
| `OM.Auth.session` | 현재 세션 조회 |

테스트 계정(로컬 seed): `admin01` / `nsight01!`

### 6.2 SESSIONDB

| 테이블 | OM 역할 |
|--------|---------|
| `SPRING_SESSION` / `ATTRIBUTES` | **소유** — `spring.session.store-type: jdbc` |
| `TCF_USER_SESSION` | Gateway·OM 공유 레지스트리 |

Gateway `session-datasource`는 OM과 **동일 DB** — [51-api-gateway.md](51-api-gateway.md), [10-session.md](10-session.md).

### 6.3 JWT 연계

- `OM.Auth.ssoLogin` → Access Token 발급 요청 (`tcf-jwt`)
- OM 업무 + Gateway: Bearer JWT **필수** (정책) — [42-jwt.md](42-jwt.md)

---

## 7. 로그·감사·모니터링

### 7.1 거래·감사 로그

| 기능 | Handler | 데이터 |
|------|---------|--------|
| 거래로그 통합 조회 | `OmTransactionLogHandler` | `TCF_TX_LOG` (전 WAR 적재) |
| 감사 조회 | `OmAuditLogHandler` | `OM_AUDIT_LOG` |
| 파일 다운로드 감사 | `OmFileDownloadHandler` | `OM_FILE_DOWNLOAD_LOG` |

업무 WAR ETF가 `TCF_TX_LOG` INSERT — OM은 **조회·운영 삭제** (권한 통제).

### 7.2 대시보드 (`tcf-batch` 연계)

```text
tcf-batch (8098) — 수집
  → OM_AP_STATUS, OM_DB_STATUS, OM_SESSION_STATUS, OM_DEPLOY_STATUS
  → OM_BATCH_HISTORY
        ▼
tcf-om OmDashboardHandler — 조회만
```

배치 Job 원격 실행: `OmBatchHandler` ↔ `OmBatchRemoteClient` → `tcf-batch`.  
상세: [13-batch.md](13-batch.md).

---

## 8. 배포 관리 (OM vs CI/CD)

| 역할 | OM (`OmDeployHandler`) | CI/CD |
|------|------------------------|-------|
| 배포 **요청** | `OM.Deploy.deployRequest` | — |
| **승인** | `OM.Deploy.approve` | — |
| **롤백 지시** | `OM.Deploy.rollbackRequest` | Runner 실행 |
| 빌드·WAR 배포 | `buildRequest`, `execute` (연계 설계) | `tcf-cicd/scripts` |
| 이력·상태 | `history`, `buildStatus`, `healthCheck` | Artifact 메타 |

핵심: **OM = 통제·기록**, **CI/CD = 실행** — [49-release-strategy.md](49-release-strategy.md) §5.1.

`OmDeployHandler` serviceId 예:

- `OM.Deploy.deployRequest`, `approve`, `rollbackRequest`
- `OM.Deploy.history`, `healthCheck`, `buildStatus`

---

## 9. Admin UI 아키텍처

| 화면 (tcf-ui) | OM serviceId 예 |
|---------------|-----------------|
| `/om/admin/login.html` | `OM.Auth.login` |
| `service-catalog.html` | `OM.ServiceCatalog.*` |
| `transaction-log.html` | `OM.TransactionLog.*` |
| `cache.html` | `OM.Cache.*` (`ROLE_OM_CACHE`) |
| `session.html` | `OM.Session.*` |
| `batch.html` | `OM.Batch.*` |
| `dashboard.html` | `OM.Dashboard.*` |

공통 JS: `om-admin.js` — `relayFetch('OM', serviceId, body)`, `uiPath()`.

Relay 경로:

- bootRun UI: `http://localhost:8099/api/relay/OM/online`
- Gateway 경유: `tcf-uj` → `8100/om/online`

---

## 10. UD (파일 업·다운로드)

| 항목 | 내용 |
|------|------|
| REST | `OmUpdownloadFileController` — `/ud` (Gateway 미경유) |
| UI | `tcf-ui` `/api/updownload/*` → tcf-om 직접 |
| 메타 | `UD_FILE_META` |
| 감사 | `OM_FILE_DOWNLOAD_LOG` |
| 물리 파일 | `nsight.updownload.storage-path` |

상세: [18-fileupdownload.md](18-fileupdownload.md).

---

## 11. 캐시 운영

`tcf-om`만 `tcf-cache` 의존 — EhCache 영역:

- 공통코드, 거래통제, Timeout 등 `@Cacheable`

운영자 `OM.Cache.inquiry` / `OM.Cache.delete` — 기준정보 변경 후 **필수 Evict** ([12-cache.md](12-cache.md)).

---

## 12. OMDB·다중 소비자

단일 H2 file `nsight_om` (로컬) — 운영 Oracle OMDB 목표.

| 소비자 | 읽기 | 쓰기 |
|--------|------|------|
| tcf-om | ● | ● 전 테이블 |
| tcf-batch | ● 스냅샷 테이블 | ● `OM_*_STATUS`, `OM_BATCH_HISTORY` |
| 업무 WAR | ● TC·Timeout·Catalog (STF) | ● `TCF_TX_LOG` only |
| tcf-gateway | ● `SPRING_SESSION`, `TCF_USER_SESSION` | ● USER_SESSION touch |

---

## 13. 권한 모델 (요약)

| 계층 | 테이블 | 질문 |
|------|--------|------|
| 기능 권한 | `OM_FUNCTION_AUTH` | 메뉴·기능 CRUD 가능? |
| 데이터 권한 | `OM_DATA_AUTH` | 지점·고객 범위? |
| 거래통제 | `TCF_TRANSACTION_CONTROL` | Header 7 조합 허용? |

세 계층 **독립** — [40-권한-검증-기준.md](../../znsight-man/40-권한-검증-기준.md).

---

## 14. 운영 체크리스트

- [ ] 신규 serviceId: Catalog → TC → Timeout → Cache Evict → Smoke
- [ ] 배포 전 OM 기준정보와 WAR Handler 정합
- [ ] `tcf-batch` 기동 — 대시보드 스냅샷
- [ ] Gateway `session-datasource` = OM SESSION DB
- [ ] 거래로그·감사 조회 권한·마스킹 정책
- [ ] 배포 승인·롤백 이력 `OM.Deploy.*` 기록
- [ ] `om-service` 레거시 미배포 확인

---

## 15. 현행 vs 목표 (Gap)

| 항목 | 현행 | 목표 |
|------|------|------|
| Handler 수 | 24 (도메인 통합) | 설계 83개 기능 커버 유지·문서화 |
| Deploy ↔ CI | Handler·테이블 있음 | GitLab Callback 완전 연동 |
| Gateway Route | Gateway Admin | OM 단일 SoT 동기 |
| 운영 검증 | 로컬·ztomcat | stg 실운영 프로세스 |
| OM UI | tcf-ui Relay | 권한·감사 운영 서명 |

---

## 16. 관련 소스

| 경로 | 설명 |
|------|------|
| `tcf-om/.../entry/handler/Om*.java` | 24 Handler |
| `tcf-om/.../schema.sql`, `data.sql` | OMDB SoT |
| `tcf-om/.../OmDeployFacade.java` | 배포 통제 |
| `tcf-om/.../OmAuthFacade.java` | 로그인·SSO |
| `tcf-ui/.../om-admin.js` | Admin Relay |
| `tcf-batch/` | 상태 수집 |

---

← [51-api-gateway.md](51-api-gateway.md) · [53-naming-conventions.md](53-naming-conventions.md) →
