# tcf-batch — OM 대시보드 데이터 수집 배치

OM 운영 대시보드에 표시되는 **AP/DB/세션/배포 상태**를 수집해 `OM_AP_STATUS`, `OM_DB_STATUS`, `OM_SESSION_STATUS`, `OM_DEPLOY_STATUS` 테이블에 적재하는 배치 모듈입니다.

| 항목 | 값 |
|------|-----|
| Gradle 모듈 | `tcf-batch` |
| 메인 클래스 | `com.nh.nsight.tcf.batch.NsightTcfBatchApplication` |
| bootRun 포트 | **8098** (`spring.profiles.active=local`) |
| WAR (bootWar) | `tcf-batch.war` → ztomcat `/batch` |
| 공유 DB | `data/nsight-txlog/nsight_om` (tcf-om과 동일 H2) |

## Job 목록

| Job ID | 설명 | 저장 테이블 |
|--------|------|-------------|
| `BAT-BATCH-001` | AP 상태 수집 (Actuator CPU/Heap/Thread) | `OM_AP_STATUS` |
| `BAT-BATCH-002` | DB 상태 수집 (Actuator/JDBC) | `OM_DB_STATUS` |
| `BAT-BATCH-003` | 세션 현황 (Spring Session + Tomcat Session) | `OM_SESSION_STATUS` |
| `BAT-BATCH-004` | 배포 현황 (Actuator health·기동 시각) | `OM_DEPLOY_STATUS` |

## 프로파일

| 프로파일 | 용도 | 수집 대상 base-url |
|----------|------|-------------------|
| `local` (기본) | 로컬 bootRun | `http://127.0.0.1:8097` 등 개별 포트 |
| `dev` | ztomcat WAR | `http://127.0.0.1:8080/{context}` 게이트웨이 |
| `prod` | 운영 Tomcat | `${NSIGHT_GATEWAY_BASE_URL}/{context}` |

설정 파일:

- `application.yml` — 공통
- `application-local.yml` — local bootRun 수집 대상
- `application-dev.yml` — AP 상태 수집 대상으로 등록된 13개 context
- `application-prod.yml` — 운영 gateway URL

ztomcat WAR 배포 시: `spring.profiles.active=dev` ([25-env-profile.md](../zdocs-1/architecture/25-env-profile.md))

## 실행

```bash
# bootRun
gradle :tcf-batch:bootRun
tcf-batch/scripts/run-local.bat

# ztomcat (batch.war → /batch)
ztomcat/deploy-wars.bat batch
```

**사전 조건:** tcf-om과 동일 H2. 수집 대상은 Actuator `health`, `metrics` 노출 필요.

## 수동 실행 API

```bash
# bootRun
curl -X POST http://localhost:8098/batch/jobs/ap-status/run

# ztomcat
curl -X POST http://localhost:8080/batch/jobs/ap-status/run
```

| 엔드포인트 | 설명 |
|------------|------|
| `POST .../jobs/ap-status/run` | AP 상태 |
| `POST .../jobs/db-status/run` | DB 상태 |
| `POST .../jobs/session-status/run` | 세션 현황 |
| `POST .../jobs/deploy-status/run` | 배포 현황 |

## OM 연동

- tcf-om 설정 (Tomcat): `nsight.om.batch-service-url: http://127.0.0.1:8080/batch`
- tcf-om 설정 (bootRun): `nsight.om.batch-service-url: http://127.0.0.1:8098/batch`
- OM 배치 관리 화면에서 Job `BAT-BATCH-001` ~ `004` 수동 재실행 가능

## 세션 수집 참고 (Tomcat)

- **OM 운영 포털**: `OM-PORTAL` — Spring Session JDBC (`SPRING_SESSION`)만 수집
- **업무 WAR**: 각 context의 `tomcat.sessions.active.current` (HTTP Session)
- OM의 Tomcat HTTP Session(`OM-AP`)은 Spring Session과 중복이므로 **수집 대상에서 제외**

## 로컬 검증 (bootRun)

1. `gradle :tcf-om:bootRun` (8097)
2. `gradle :sv-service:bootRun` (8086) 등 대상 AP
3. `gradle :tcf-batch:bootRun` (8098)
4. `curl -X POST http://localhost:8098/batch/jobs/ap-status/run`
5. http://localhost:8099/om/admin/dashboard.html

## 패키지 구조

```text
com.nh.nsight.tcf.batch
├── NsightTcfBatchApplication        # extends NsightWarBootstrap
├── application/
│   ├── service/       ApStatusCollectService, DbStatusCollectService, SessionStatusCollectService, DeployStatusCollectService
│   └── scheduler/     ApStatusCollectScheduler, DbStatusCollectScheduler, …
├── client/            ApMetricsClient, DbMetricsClient, SessionMetricsClient, DeployMetricsClient
├── config/            ApStatusBatchProperties, DbStatusBatchProperties, …
├── entry/web/         ApStatusBatchController, DbStatusBatchController, … (POST /batch/jobs/*/run)
├── persistence/repository/  OmDashboardStatusRepository
└── support/           BatchDatabaseMigration, ScheduledCollectSupport, DashboardCollectStartupRunner
```

## 로컬 검증 (ztomcat)

1. `ztomcat/deploy-wars.bat all` + `start.bat`
2. `curl -X POST http://localhost:8080/batch/jobs/ap-status/run`
3. http://localhost:8080/ui/om/admin/dashboard.html
