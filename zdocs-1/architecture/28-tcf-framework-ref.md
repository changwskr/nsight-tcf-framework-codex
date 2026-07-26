# 28. TCF 프레임워크 모듈 레퍼런스

| 항목 | 내용 |
|------|------|
| 문서 번호 | 28 |
| 제목 | TCF Framework Module Reference |
| 상위 문서 | [architecture.md](architecture.md) |
| 관련 문서 | [01-application-layer.md](01-application-layer.md), [03-transaction.md](03-transaction.md), [13-batch.md](13-batch.md), [16-deploy.md](16-deploy.md), [17-script.md](17-script.md), [25-env-profile.md](25-env-profile.md), [26-mybatis.md](26-mybatis.md) |
| 대상 | 프레임워크·플랫폼·운영 개발자 |

---

## 1. 개요

NSIGHT TCF Gradle 멀티 모듈에서 **플랫폼 9개 디렉터리**(`tcf-util` ~ `tcf-cicd`)의 역할·의존·소스 구조를 한곳에 정리한다.

| 디렉터리 | Gradle `include` | 산출물 | 한 줄 역할 |
|----------|------------------|--------|------------|
| `tcf-util` | O | JAR | Spring 없는 순수 Java 유틸 |
| `tcf-core` | O | JAR | TCF 엔진 (STF/TCF/ETF, Handler SPI) |
| `tcf-web` | O | JAR | HTTP 진입·거래로그·MyBatis 자동설정 |
| `tcf-cache` | O | JAR | EhCache + Spring Cache |
| `tcf-om` | O | WAR | 운영관리 OM + 파일 UD |
| `tcf-batch` | O | WAR | OM 대시보드 상태 수집 |
| `tcf-ui` | O | WAR | 거래 테스트 UI · OM Admin Relay |
| `tcf-scripts` | **X** | — | bootRun·빌드·배포 래퍼 스크립트 |
| `tcf-cicd` | **X** | — | 환경 설정 SoT (local/dev/prod yml) |

업무 WAR 16개(`cc-service` … `mg-service`)는 본 문서 범위 밖 — 동일 패턴으로 `tcf-web`을 의존한다.

---

## 2. 의존 방향

```text
tcf-util                    (Spring 없음)
   ↑ api
tcf-core                    (STF/TCF/ETF, 표준전문, Handler SPI)
   ↑ api
tcf-web                     (/online, Gateway, 거래로그 DS, MyBatis AutoConfig)
   ↑                    ↑
tcf-cache (선택)      tcf-batch
   ↑
tcf-om  ←── HTTP ── tcf-ui (Relay, 정적 UI)

*-service (업무 9) ──→ tcf-web (+ tcf-core transitively)

tcf-cicd ──sync──→ {모듈}/application-{profile}.yml, ztomcat/setenv.*
tcf-scripts ──invoke──→ gradle, ztomcat (설정 SoT 아님)
```

---

## 3. tcf-util

| 항목 | 내용 |
|------|------|
| 경로 | `tcf-util/` |
| README | [tcf-util/README.md](../../tcf-util/README.md) |
| 의존 | **없음** (Spring 의도적 미사용) |
| 소비자 | `tcf-core`, `tcf-web`, 모든 업무 WAR |

### 3.1 역할

GUID·날짜·마스킹 등 **프레임워크 공통 순수 Java** 유틸. Spring Context 없이 단위 테스트·재사용 가능.

### 3.2 소스 트리

```text
src/main/java/com/nh/nsight/tcf/
├── util/
│   ├── GuidGenerator.java       UUID 기반 GUID / TraceId
│   ├── DateTimeUtil.java        KST 현재 시각·날짜 포맷
│   ├── MaskingUtils.java        고객ID·계좌번호 마스킹
│   └── tpmutil/
│       └── tpcutil.java         TPM → 업무 WAS /online HTTP 클라이언트
└── tpmutil/
    └── tpsutil.java             (스텁, 미구현)
```

리소스: **없음**

### 3.3 빌드

```bash
gradle :tcf-util:build
```

---

## 4. tcf-core

| 항목 | 내용 |
|------|------|
| 경로 | `tcf-core/` |
| README | [tcf-core/README.md](../../tcf-core/README.md) |
| 의존 | `api :tcf-util`, `spring-boot-starter`, `validation`, `jackson-annotations` |

### 4.1 역할

**TCF 거래 엔진** — 표준 HTTP/JSON 전문 모델, STF(전처리) → Dispatcher → Handler → ETF(후처리) 파이프라인. 업무 코드는 `TransactionHandler`만 구현.

상세: [33-TCF.md](33-TCF.md)

### 4.2 소스 트리

```text
com.nh.nsight.tcf.core/
├── config/           TcfProperties, NsightTxlogPathEnvironmentPostProcessor
├── context/          TransactionContext, TransactionContextHolder
├── dispatch/         TransactionDispatcher        serviceId → Handler
├── error/            BusinessException, SystemException, ErrorCode
├── idempotency/      IdempotencyChecker, InMemoryIdempotencyChecker
├── logging/          TransactionLogService, AuditLogService, TransactionLogRepository(SPI)
├── message/          StandardRequest/Response/Header, Result, ProcessingType
├── metrics/          TransactionMetricService
├── processor/        TCF, STF, ETF                 파이프라인 핵심
├── security/         SessionValidator, AuthorizationValidator
├── support/          TcfConsoleLog
├── transaction/      TransactionHandler            업무 SPI
└── validation/       StandardHeaderValidator
```

**먼저 읽을 파일:** `processor/TCF.java`, `transaction/TransactionHandler.java`, `message/StandardRequest.java`, `dispatch/TransactionDispatcher.java`

### 4.3 리소스

| 파일 | 용도 |
|------|------|
| `application.yml` / `application-{local,dev,prod}.yml` | `nsight.tcf.*` 기본값 |
| `logback-tcf-console.xml` | TCF 콘솔 로그 |
| `META-INF/spring/...EnvironmentPostProcessor` | `nsight.txlog.path` 자동 설정 |

---

## 5. tcf-web

| 항목 | 내용 |
|------|------|
| 경로 | `tcf-web/` |
| README | [tcf-web/README.md](../../tcf-web/README.md) |
| 의존 | `api :tcf-core`, `:tcf-util`, web, validation, actuator, aop, jdbc, **mybatis-spring-boot-starter 3.0.3** |

### 5.1 역할

- **HTTP 진입:** `POST /online`, `POST /{bc}/online` → `TCF.process()`
- **비표준 진입:** `TcfGateway` (REST Controller·multipart → TCF)
- **거래로그 DB:** 별도 DataSource + `JdbcTransactionLogRepository` → `TCF_TX_LOG`
- **MyBatis 자동설정:** 이중 DS 환경에서 업무 `dataSource` + `SqlSessionFactory` ([26-mybatis.md](26-mybatis.md))
- **WAR 부트스트랩:** `NsightWarBootstrap` (`SpringBootServletInitializer`)

### 5.2 소스 트리

```text
com.nh.nsight.tcf.web/
├── boot/             NsightWarBootstrap
├── config/
│   ├── TcfAutoConfiguration
│   ├── TcfPrimaryDataSourceAutoConfiguration
│   ├── TcfMyBatisAutoConfiguration
│   ├── TcfTransactionLogDataSourceConfiguration
│   └── TcfTransactionLogConfiguration
├── controller/       OnlineTransactionController
├── exception/        GlobalStandardExceptionHandler
├── filter/           GuidMdcCleanupFilter
├── gateway/          TcfGateway
└── logging/
    ├── JdbcTransactionLogRepository
    └── TransactionLogSchemaInitializer
```

### 5.3 AutoConfiguration 등록

`META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`:

```text
TcfAutoConfiguration
TcfPrimaryDataSourceAutoConfiguration
TcfMyBatisAutoConfiguration
TcfTransactionLogDataSourceConfiguration
TcfTransactionLogConfiguration
```

업무 WAR는 `@SpringBootApplication` + `extends NsightWarBootstrap` + `@MapperScan`으로 조립.

---

## 6. tcf-cache

| 항목 | 내용 |
|------|------|
| 경로 | `tcf-cache/` |
| README | [tcf-cache/README.md](../../tcf-cache/README.md) |
| 의존 | `api :tcf-core`, `spring-boot-starter-cache`, `ehcache`(jakarta), JCache API |

### 6.1 역할

**Spring Cache + EhCache 3(JCache)** 공통 모듈. `@Cacheable` / `@CacheEvict` 및 OM 관리 화면용 캐시 스냅샷.

| 캐시 alias (`TcfCacheNames`) | TTL | 용도 |
|------------------------------|-----|------|
| `commonCode` | 30분 | OM 공통코드 |
| `serviceCatalog` | 60분 | ServiceId 카탈로그 |
| `sessionRegion` | 10분 | 세션 영역 |

주 소비자: **`tcf-om`** (`OmCommonCodeCacheService`, `/om/admin/cache.html`)

### 6.2 소스 트리

```text
com.nh.nsight.tcf.cache/
├── config/
│   ├── TcfCacheAutoConfiguration
│   ├── TcfCacheProperties          nsight.tcf.cache.enabled, configLocation
│   └── TcfCacheSupportAutoConfiguration
└── support/
    ├── TcfCacheNames
    └── TcfCacheSupport               evict, snapshot (OM UI)
```

리소스: `ehcache.xml`, `application-{profile}.yml`

---

## 7. tcf-om

| 항목 | 내용 |
|------|------|
| 경로 | `tcf-om/` |
| README | [tcf-om/README.md](../../tcf-om/README.md) |
| 의존 | `:tcf-core`, `:tcf-web`, `:tcf-cache`, web, jdbc, mybatis, spring-session-jdbc, H2 |
| bootRun | **8097** |
| WAR | `tcf-om.war` → ztomcat **`om.war`** (`/om`) |

### 7.1 역할

**운영관리(OM) + 파일 업·다운로드(UD)** 통합 WAR. 레거시 `om-service` 대체.

| 기능 | 설명 |
|------|------|
| Handler 40+ | `OM.*.inquiry/save/...` — 로그인, 대시보드, 사용자, ServiceId, 공통코드, 거래로그 등 |
| Spring Session JDBC | OM 포털 로그인 세션 |
| EhCache | 공통코드·카탈로그 캐시 |
| UD REST | `OmUpdownloadFileController` — `/ud/files` 업·다운·목록 |
| 배치 연동 | `OmBatchRemoteClient` → tcf-batch Job 실행 |
| DB 마이그레이션 | `OmDatabaseMigration`, `ServiceCatalogSeedData` — 기동 시 스키마·카탈로그 MERGE |

### 7.2 소스 트리

```text
com.nh.nsight.marketing.om/
├── NsightTcfOmApplication.java
├── batch/              OmSessionCleanupScheduler
├── config/             Spring Session, 스케줄, 비밀번호
├── dao/                OmOperationDao, OmSampleDao
├── facade/             (20) 업무별 Facade — @Transactional 경계
├── handler/            (43) TransactionHandler — serviceId 등록
├── mapper/             OmOperationMapper, OmSampleMapper
├── rule/               OmOperationRule (페이징 normalizePaging 등)
├── service/            (22) 업무 Service
├── support/            OmDatabaseMigration, OmBatchRemoteClient, Health ...
└── updownload/
    ├── controller/     OmUpdownloadFileController
    └── service/        OmUpdownloadService, OmFileStorageService
```

### 7.3 리소스

| 유형 | 경로 |
|------|------|
| yml | `application.yml`, `application-{local,dev,prod}.yml` |
| SQL seed | `schema.sql`, `data.sql` |
| MyBatis | `mapper/om/OmOperationMapper.xml` (800+ lines), `OmSampleMapper.xml` |
| static | 없음 (UI → `tcf-ui`) |

**Handler 예:** `OmDashboardHandler`, `OmAuthHandler`, `OmServiceCatalogHandler` (도메인당 1개, `serviceIds()`로 다중 거래 처리)

---

## 8. tcf-batch

| 항목 | 내용 |
|------|------|
| 경로 | `tcf-batch/` |
| README | [tcf-batch/README.md](../../tcf-batch/README.md) |
| 의존 | `:tcf-core`, `:tcf-web`, web, jdbc, actuator, H2 |
| bootRun | **8098** (`context-path: /batch`) |
| WAR | `tcf-batch.war` → ztomcat **`batch.war`** (`/batch`) |

상세: [13-batch.md](13-batch.md)

### 8.1 역할

OM 대시보드용 **AP/DB/세션/배포 상태 수집 전용** 배치. 화면 렌더링 없음 — H2 `OM_*_STATUS` 테이블만 갱신.

| Job ID | Service | 저장 테이블 |
|--------|---------|-------------|
| `BAT-BATCH-001` | `ApStatusCollectService` | `OM_AP_STATUS` |
| `BAT-BATCH-002` | `DbStatusCollectService` | `OM_DB_STATUS` |
| `BAT-BATCH-003` | `SessionStatusCollectService` | `OM_SESSION_STATUS` |
| `BAT-BATCH-004` | `DeployStatusCollectService` | `OM_DEPLOY_STATUS` |

### 8.2 소스 트리

```text
com.nh.nsight.tcf.batch/
├── NsightTcfBatchApplication.java    @EnableScheduling
├── client/           ApMetricsClient, DbMetricsClient, SessionMetricsClient, DeployMetricsClient
├── config/           ApStatusBatchProperties, RestTemplate ...
├── job/              *CollectScheduler (cron 5분)
├── model/            ApStatusSnapshot, BatchCollectResult ...
├── repository/       OmDashboardStatusRepository   JDBC MERGE
├── service/          Ap/Db/Session/Deploy StatusCollectService
├── support/          BatchDatabaseMigration, DashboardCollectStartupRunner
└── web/              Ap/Db/Session/Deploy StatusBatchController
                      POST /jobs/{ap,db,session,deploy}-status/run
```

MyBatis **미사용** — JDBC 직접. `tcf-om`과 **동일 H2** (`nsight_om`, TCP 9092).

---

## 9. tcf-ui

| 항목 | 내용 |
|------|------|
| 경로 | `tcf-ui/` |
| README | [tcf-ui/README.md](../../tcf-ui/README.md) |
| 의존 | **tcf-* JAR 없음** — web, actuator만 (순수 Relay/UI) |
| bootRun | **8099** |
| WAR | `tcf-ui.war` → ztomcat **`ui.war`** (`/ui`) |

### 9.1 역할

| 기능 | 설명 |
|------|------|
| **Relay** | 브라우저 → `/api/relay` → 업무 WAS / tcf-om `/online` (CORS·URL 보정) |
| **거래 테스트 UI** | 업무별 `static/{bc}/index.html` — JSON 전문 송수신 |
| **OM Admin** | `static/om/admin/*.html` — 대시보드, ServiceId, 사용자, 로그 등 |
| **배포 모드** | `nsight.tcf-ui.deployment-mode`: `bootrun`(개별 포트) / `tomcat`(8080 gateway) |

### 9.2 Java 소스

```text
com.nh.nsight.tcf.ui/
├── NsightTcfUiApplication.java
├── catalog/          BusinessModuleDefinitions
├── config/           TcfUiProperties, TcfUiConfiguration
├── controller/       TcfApiController, UpdownloadApiController, EtcApiController
├── service/          TransactionRelayService, UpdownloadRelayService, BusinessModuleCatalog
└── web/              UiTomcatHtmlRewriteFilter   (/ui context HTML 경로 보정)
```

### 9.3 정적 리소스 (주요)

```text
src/main/resources/static/
├── index.html                    업무 허브
├── _shared/
│   ├── ui-context.js             Relay·설정 로드
│   ├── om-admin.js               OM API, renderPagination, error popup
│   ├── online-single.js / online-multi.js
│   └── error-popup.js / .css
├── om/admin/                     login, dashboard, service-catalog, transaction-log ...
├── {cc,sv,bc,...}/               업무별 거래 테스트 HTML
└── ud/updownload.html

sample-requests/                  om-transactions.json, sv-sample-inquiry.json ...
```

OM Admin URL 예: `http://localhost:8099/om/admin/login.html` (bootRun)  
ztomcat: `http://localhost:8080/ui/om/admin/login.html`

---

## 10. tcf-scripts

| 항목 | 내용 |
|------|------|
| 경로 | `tcf-scripts/` |
| README | [tcf-scripts/README.md](../../tcf-scripts/README.md) |
| Gradle | **미포함** — framework **루트**에서 실행 |

### 10.1 역할

로컬 개발 **단축 스크립트**. 환경 yml SoT는 **`tcf-cicd`** — `tcf-scripts`는 sync 하지 않음.

| 스크립트 | 용도 |
|----------|------|
| `run-local.bat/.sh` | `gradle :{모듈}:bootRun` (sv, tcf-om, batch, ui, all) |
| `build.bat/.sh` | `buildBusinessWars`, `buildZtomcatWars`, 개별 모듈 |
| `build-all.bat/.sh` | 전체 빌드 |
| `deploy.bat/.sh` | WAR → `ztomcat/.../webapps` |
| `curl-sample.bat/.sh` | sample-requests JSON으로 `/online` 호출 |

상세: [17-script.md](17-script.md)

---

## 11. tcf-cicd

| 항목 | 내용 |
|------|------|
| 경로 | `tcf-cicd/` |
| README | [tcf-cicd/README.md](../../tcf-cicd/README.md) |
| Gradle | **미포함** |
| manifest | [tcf-cicd/manifest.yaml](../../tcf-cicd/manifest.yaml) |

### 11.1 역할

**환경 설정 Source of Truth (SoT)** — `local` / `dev` / `prod` 프로파일별 Spring yml, Tomcat setenv, Apache routing.

| 프로파일 | 용도 | sync 대상 |
|----------|------|-----------|
| `local` | bootRun (개발 PC) | `application-local.yml`, `ztomcat/setenv.local.*` |
| `dev` | ztomcat 통합 검증 | `application-dev.yml`, `ztomcat/conf/setenv.*` |
| `prod` | 운영 Tomcat + Apache | `application-prod.yml`, `setenv.prod.*`, `apache/*.conf` |

**관리 모듈 23개:** 플랫폼 6 + 업무 17 (`manifest.yaml` 참고)  
`application.yml`(공통)은 **framework 모듈**에 유지 — cicd는 **`application-{profile}.yml`만**.

### 11.2 디렉터리

```text
tcf-cicd/
├── manifest.yaml
├── local/
│   ├── spring/{module}/application-local.yml
│   ├── ztomcat/          start, stop, deploy-restart, setenv.local.*
│   └── script/           build-all, deploy-wars, h2-txlog
├── dev/
│   ├── spring/...
│   └── ztomcat/setenv.*
├── prod/
│   ├── spring/...
│   ├── ztomcat/setenv.*
│   └── apache/nsight-marketing-routing.conf
└── scripts/
    ├── sync-to-framework.ps1      cicd → framework (빌드 전)
    ├── pull-from-framework.ps1    framework → cicd (bootstrap)
    └── apply-tomcat-config.sh     prod runtime yml mount
```

### 11.3 일반 워크플로

```powershell
# 1. 설정 수정 후 framework 반영
tcf-cicd/scripts/sync-to-framework.ps1 -Profile dev

# 2. 빌드
tcf-cicd/local/script/build-all.ps1 -Target wars

# 3. ztomcat 배포·기동
tcf-cicd/local/script/deploy-wars.ps1
tcf-cicd/local/ztomcat/start.ps1 -DeployAll
```

상세: [25-env-profile.md](25-env-profile.md), [24-env-spring-detail.md](24-env-spring-detail.md)

---

## 12. 모듈별 실행·배포 요약

| 모듈 | bootRun 포트 | Gradle WAR | ztomcat WAR | Context |
|------|-------------|------------|-------------|---------|
| tcf-om | 8097 | `tcf-om.war` | `om.war` | `/om` |
| tcf-batch | 8098 | `tcf-batch.war` | `batch.war` | `/batch` |
| tcf-ui | 8099 | `tcf-ui.war` | `ui.war` | `/ui` |
| *-service | 8081–8096 | `{bc}.war` | `{bc}.war` | `/{bc}` |
| tcf-util/core/web/cache | — | JAR only | (WAR 내 classpath) | — |

ztomcat 통합: **8080** 게이트웨이 — [16-deploy.md](16-deploy.md), [ztomcat/README.md](../../ztomcat/README.md)

---

## 13. 공유 인프라

| 항목 | 설명 |
|------|------|
| H2 txlog | `data/nsight-txlog/nsight_om` — 거래로그(`TCF_TX_LOG`), OM 메타, 배치 `OM_*_STATUS` |
| H2 TCP | ztomcat: **9092** (`ztomcat/h2-txlog.ps1`, `tcf-cicd/local/script/h2-txlog.bat`) |
| `nsight.txlog.path` | bootRun·Tomcat·batch 공통 DB 경로 ([tcf-core EnvironmentPostProcessor](../../tcf-core/src/main/java/com/nh/nsight/tcf/core/config/NsightTxlogPathEnvironmentPostProcessor.java)) |
| Spring profile | `local` / `dev` / `prod` — [25-env-profile.md](25-env-profile.md) |

---

## 14. 소스 탐색 가이드

| 목적 | 시작 파일 |
|------|-----------|
| 거래 한 건의 전체 흐름 | `tcf-web/.../OnlineTransactionController.java` → `tcf-core/.../TCF.java` |
| 업무 Handler 작성 | `sv-service/.../SvSampleHandler.java` + `TransactionHandler.java` |
| OM 목록·페이징 | `tcf-om/.../OmServiceCatalogService.java` + [27-paging.md](27-paging.md) |
| MyBatis·DAO | `tcf-om/.../OmOperationDao.java` + [26-mybatis.md](26-mybatis.md) |
| 대시보드 수집 | `tcf-batch/.../ApStatusCollectService.java` + [13-batch.md](13-batch.md) |
| UI Relay | `tcf-ui/.../TransactionRelayService.java` + `static/_shared/ui-context.js` |
| 환경 yml 변경 | `tcf-cicd/{profile}/spring/` → `sync-to-framework.ps1` |
| 로컬 기동 | `tcf-scripts/run-local.bat` 또는 `tcf-cicd/local/ztomcat/start.ps1` |

전체 소스 인덱스(구식): [SOURCE_INDEX.md](../SOURCE_INDEX.md) — **본 문서(28)가 플랫폼 모듈 기준 최신**.

---

## 15. 모듈 README 링크

| 모듈 | README |
|------|--------|
| tcf-util | [tcf-util/README.md](../../tcf-util/README.md) |
| tcf-core | [tcf-core/README.md](../../tcf-core/README.md) |
| tcf-web | [tcf-web/README.md](../../tcf-web/README.md) |
| tcf-cache | [tcf-cache/README.md](../../tcf-cache/README.md) |
| tcf-om | [tcf-om/README.md](../../tcf-om/README.md) |
| tcf-batch | [tcf-batch/README.md](../../tcf-batch/README.md) |
| tcf-ui | [tcf-ui/README.md](../../tcf-ui/README.md) |
| tcf-scripts | [tcf-scripts/README.md](../../tcf-scripts/README.md) |
| tcf-cicd | [tcf-cicd/README.md](../../tcf-cicd/README.md) |

---

## 16. 변경 이력

| 일자 | 변경 내용 |
|------|-----------|
| 2026-06 | 최초 작성 — tcf-util~cicd 9모듈 역할·소스·의존 레퍼런스 |
