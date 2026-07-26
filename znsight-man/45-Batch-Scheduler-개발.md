# 45. Batch / Scheduler 개발 기준

> **NSIGHT TCF 개발 Manual** · 원본: [`znsight-guide-word`](../znsight-guide-word/) · 갱신: 2026-07-05

> **보완 출처:** [배치관리.md](../zdoc/배치관리.md)

# TCF 배치 관리 정리

NSIGHT TCF의 **배치**는 역할에 따라 **수집 배치(`tcf-batch`)**, **OM 오케스트레이션(`OM.Batch.*`)**, **업무 스케줄(`*-service`)** 로 나뉩니다.

| 관점 | 역할 | 핵심 |
|------|------|------|
| **수집 배치** | AP/DB/세션/배포 스냅샷 | `tcf-batch` — Job + CollectService |
| **OM 관리** | Job 조회·수동 실행·이력 | `OM.Batch.inquiry/execute/deleteAll` |
| **상태 저장** | 대시보드용 테이블 | `OM_*_STATUS`, `OM_BATCH_HISTORY` |
| **원격 실행** | OM → tcf-batch HTTP | `OmBatchRemoteClient` |
| **업무 배치** | 도메인 주기 처리 | `*-service` `@Scheduled` (예: EB) |
| **세션 정리** | 만료 세션 삭제 | `OmSessionCleanupService` (`BAT-OM-002`) |

어플리케이션 6계층과 다른 패턴: [어플리케이션계층.md](../zdoc/어플리케이션계층.md) §8.4

---

## 1. 개요 — 배치 3종

```text
① tcf-batch (8098)     — 운영 모니터링 수집 (Handler 패턴 X)
② tcf-om OM.Batch.*    — Job 카탈로그·수동 실행·이력 조회
③ *-service @Scheduled — 업무 배치 (EB 이벤트 발행 등)
```

**핵심 원칙 (수집 배치)**

1. 배치가 화면을 그리지 **않음** — **상태 테이블**만 갱신
2. OM 대시보드는 테이블 **조회만**
3. 실패·부분 성공도 **`OM_BATCH_HISTORY`** 에 기록

---

## 2. 전체 아키텍처

```text
┌─ tcf-batch ─────────────────────────────────────────┐
│  @Scheduled / POST /jobs/*/run                       │
│       ↓                                              │
│  *CollectService → *MetricsClient (Actuator/JDBC)   │
│       ↓                                              │
│  OmDashboardStatusRepository (MERGE upsert)          │
│       ↓                                              │
│  OM_AP_STATUS / OM_DB_STATUS /                       │
│  OM_SESSION_STATUS / OM_DEPLOY_STATUS /              │
│  OM_BATCH_HISTORY                                    │
└──────────────────────┬──────────────────────────────┘
                       │ JDBC (공유 H2 nsight_om)
┌─ tcf-om ─────────────▼──────────────────────────────┐
│  OM.Dashboard.inquiry  → 상태 테이블 READ            │
│  OM.Batch.inquiry      → OM_BATCH_JOB + HISTORY      │
│  OM.Batch.execute      → OmBatchRemoteClient → batch │
└─────────────────────────────────────────────────────┘
```

**공유 DB:** `data/nsight-txlog/nsight_om` — `tcf-om` + `tcf-batch`

---

## 3. tcf-batch — 수집 Job 4종

| Job ID | 서비스 | 수집 대상 | 저장 테이블 |
|--------|--------|----------|-------------|
| `BAT-BATCH-001` | `ApStatusCollectService` | AP CPU/Heap/Thread (Actuator) | `OM_AP_STATUS` |
| `BAT-BATCH-002` | `DbStatusCollectService` | DB Pool / JDBC ping | `OM_DB_STATUS` |
| `BAT-BATCH-003` | `SessionStatusCollectService` | Spring Session + Tomcat Session | `OM_SESSION_STATUS` |
| `BAT-BATCH-004` | `DeployStatusCollectService` | WAR 배포·헬스 | `OM_DEPLOY_STATUS` |

**결과 모델 (공통)**

- `runStatus`: `SUCCESS` / `PARTIAL` / `FAIL`
- `durationMs`, `targetCount`, `successCount`, `failCount`
- `snapshots` — 대상별 healthStatus (`NORMAL`/`WARN`/`FAIL`)

---

## 4. 실행 방식

### 4.1 스케줄 (`@Scheduled`)

| Scheduler | cron (예) |
|-----------|-----------|
| `ApStatusCollectScheduler` | `0 */5 * * * *` (5분) |
| `DbStatusCollectScheduler` | `30 */5 * * * *` |
| `SessionStatusCollectScheduler` | `45 */5 * * * *` |
| `DeployStatusCollectScheduler` | `55 */5 * * * *` |

공통: `ScheduledCollectSupport` 워밍업 게이트 → `collectAndPersist()`

### 4.2 수동 REST API

| 엔드포인트 | 기능 |
|------------|------|
| `POST /batch/jobs/ap-status/run` | AP 수집 |
| `POST /batch/jobs/db-status/run` | DB 수집 |
| `POST /batch/jobs/session-status/run` | 세션 수집 |
| `POST /batch/jobs/deploy-status/run` | 배포 수집 |

```bash
# bootRun
curl -X POST http://localhost:8098/batch/jobs/ap-status/run

# ztomcat
curl -X POST http://localhost:8080/batch/jobs/ap-status/run
```

### 4.3 기동 시 초기 수집

`DashboardCollectStartupRunner` — 앱 기동 후 1회 수집

- `nsight.batch.startup-collect.enabled`
- `nsight.batch.startup-collect.initial-delay-ms` (Tomcat WAR 순차 배포 대기)

---

## 5. 수집 소스 (source-type)

| source-type | 의미 | 예 |
|-------------|------|-----|
| `actuator` | 대상 WAR Actuator health/metrics | AP/DB/Deploy |
| `jdbc` | JDBC ping·Pool 조회 | LOGDB |
| `spring-session` | `SPRING_SESSION` 직접 집계 | `OM-PORTAL` |

**프로파일별 대상 URL**

| 프로파일 | base-url |
|----------|----------|
| `local` | `http://127.0.0.1:{port}` 개별 bootRun |
| `dev` / `prod` | `${nsight.gateway.base-url}/{context}` |

설정: `application-local.yml`, `application-dev.yml`, `application-prod.yml`

---

## 6. OM 배치 관리 (`OM.Batch.*`)

### 6.1 serviceId

| serviceId | transactionCode | 역할 |
|-----------|-----------------|------|
| `OM.Batch.inquiry` | `OM-BAT-0001` | Job 목록 + 실행 이력 조회 |
| `OM.Batch.execute` | `OM-BAT-0002` | Job 수동 실행 |
| `OM.Batch.deleteAll` | `OM-BAT-0003` | 실행 이력 전체 삭제 |

**화면:** `/ui/om/admin/batch.html` (또는 배치/스케줄 관리)

### 6.2 inquiry 응답

- `jobs` — `OM_BATCH_JOB` (Job ID, cron, useYn …)
- `histories` — `OM_BATCH_HISTORY` (페이징)
- `pageNo`, `pageSize`, `totalCount`

### 6.3 execute — Job별 분기 (`OmBatchService`)

| jobId | 실행 주체 |
|-------|-----------|
| `BAT-BATCH-001` ~ `004` | `OmBatchRemoteClient` → tcf-batch REST |
| `BAT-OM-002` | `OmSessionCleanupService.runManual()` (tcf-om 내부) |
| 기타 (샘플 Job) | 이력만 INSERT (플레이스홀더) |

**수동 실행 필수:** `executeReason` (감사)

```yaml
# tcf-om
nsight.om.batch-service-url: http://127.0.0.1:8098/batch   # bootRun
# nsight.om.batch-service-url: http://127.0.0.1:8080/batch  # ztomcat
```

`OmBatchRemoteClient`: connect 3초, read **30초**

---

## 7. 테이블

### 7.1 Job · 이력

| 테이블 | 용도 |
|--------|------|
| `OM_BATCH_JOB` | Job 마스터 (ID, cron, useYn, 설명) |
| `OM_BATCH_HISTORY` | 실행 이력 (runStatus, durationMs, message) |

**등록 Job 예 (`data.sql`)**

| Job ID | 설명 | 실제 구현 |
|--------|------|-----------|
| `BAT-BATCH-001`~`004` | 대시보드 수집 | ● tcf-batch |
| `BAT-OM-002` | 세션 정리 | ● tcf-om `OmSessionCleanupService` |
| `BAT-SV-001`, `BAT-CM-001` … | 업무 샘플 | ○ 카탈로그만 (플레이스홀더) |

### 7.2 스냅샷 (수집 결과)

| 테이블 | 내용 |
|--------|------|
| `OM_AP_STATUS` | AP별 CPU/Heap/Thread/health |
| `OM_DB_STATUS` | DB Pool·ping |
| `OM_SESSION_STATUS` | scope별 active/expired 세션 |
| `OM_DEPLOY_STATUS` | WAR 버전·기동 상태 |

저장: `OmDashboardStatusRepository` — **`MERGE` upsert** (idempotent)

---

## 8. 세션 수집 · 세션 정리 (구분)

| 기능 | 모듈 | Job ID | 동작 |
|------|------|--------|------|
| **세션 현황 수집** | tcf-batch | `BAT-BATCH-003` | `OM_SESSION_STATUS` 스냅샷 |
| **만료 세션 삭제** | tcf-om | `BAT-OM-002` | `SPRING_SESSION` 정리 |

**세션 수집 scope**

- `OM-PORTAL` — Spring Session JDBC
- `{CODE}-AP` — 업무 WAR Actuator `tomcat.sessions.active.current`
- OM Tomcat HTTP Session(`OM-AP`)은 Spring Session과 중복 → **수집 제외**

상세: [세션관리.md](../zdoc/세션관리.md)

---

## 9. 업무 배치 (`*-service`) — 별도 패턴

TCF **Handler/Facade 온라인 거래와 별개**로, 업무 WAR에서 `@Scheduled` 사용.

**예: `eb-service`**

```text
EbEventPublishScheduler (@Scheduled fixedDelay)
  → EbEventPublishService.publishReadyEvents()
  → EpOnlineClient (TCF 거래 호출)
```

- OM `OM_BATCH_JOB`에 등록 가능 (운영 표시용)
- 실제 스케줄은 **업무 WAR application.yml** 에 정의

---

## 10. tcf-batch vs TCF 온라인 거래

| 구분 | tcf-batch | 온라인 거래 |
|------|-----------|-------------|
| 진입 | Scheduler / REST | `POST /online` |
| 패턴 | CollectService | Handler → Facade |
| 거래로그 | **비활성** (`transaction-log-enabled: false`) | `TCF_TX_LOG` |
| AOP `@Transactional` | Facade 없음 — JDBC MERGE | Facade TX |

---

## 11. 장애 · 실패 내성

| 원칙 | 내용 |
|------|------|
| 대상별 격리 | 개별 try-catch — 일부 FAIL → Job `PARTIAL` |
| unreachable | FAIL 스냅샷 저장 (관측 공백 최소화) |
| timeout | connect 3s / read 5s (AP 등) — hang보다 빠른 FAIL |
| 이력 | `OM_BATCH_HISTORY`에 항상 기록 |

---

## 12. 로컬 검증

```text
1. gradle :tcf-om:bootRun          (8097)
2. gradle :sv-service:bootRun      (8086) 등
3. gradle :tcf-batch:bootRun       (8098)
4. curl -X POST http://localhost:8098/batch/jobs/ap-status/run
5. http://localhost:8099/om/admin/dashboard.html
```

---

## 13. 신규 수집 Job 추가 (tcf-batch)

1. `config` — `{Domain}BatchProperties`
2. `client` — MetricsClient (source-type별)
3. `support/model` — Snapshot / CollectResult
4. `application/service` — `collectAndPersist()`
5. `persistence/repository` — upsert + `insertBatchHistory`
6. `application/scheduler` + `entry/web` Controller
7. `application*.yml` — cron·targets
8. `OM_BATCH_JOB` MERGE + `OmBatchService.execute` 분기 (OM 수동 실행 시)

---

## 14. 개발자 체크리스트

1. **수집**은 `tcf-batch`, **조회·수동 실행 UI**는 `tcf-om`
2. 대시보드 데이터는 **`OM_*_STATUS`** — OM은 READ only
3. `BAT-BATCH-001`~`004` 수동 실행 → **`batch-service-url`** 설정 확인
4. Actuator **`health`/`metrics`** 노출 필수
5. 프로파일(`local`/`dev`/`prod`)별 **수집 target URL** 확인
6. `startup-collect` delay — Tomcat 순차 배포와 맞출 것
7. 업무 배치는 **`*-service` Scheduler** — OM Job 카탈로그와 동기화 여부 검토
8. `PARTIAL`/`FAIL` 이력 모니터링

---

## 15. 구현 소스 (읽는 순서)

| 순서 | 파일 |
|------|------|
| 1 | `tcf-batch/README.md` |
| 2 | `tcf-batch/.../ApStatusCollectService.java` |
| 3 | `tcf-batch/.../ApStatusCollectScheduler.java` |
| 4 | `tcf-batch/.../OmDashboardStatusRepository.java` |
| 5 | `tcf-om/.../OmBatchService.java` |
| 6 | `tcf-om/.../OmBatchRemoteClient.java` |
| 7 | `tcf-om/.../OmSessionCleanupService.java` |
| 8 | `eb-service/.../EbEventPublishScheduler.java` — 업무 배치 예 |

---

## 관련 문서

- [docs/architecture/13-batch.md](../docs/architecture/13-batch.md) — 배치 아키텍처 (상세)
- [docs/architecture/19-tcf-table.md](../docs/architecture/19-tcf-table.md) — OM_*_STATUS 테이블
- [zdoc/세션관리.md](../zdoc/세션관리.md) — 세션 수집·정리
- [zdoc/DAO처리.md](../zdoc/DAO처리.md) — MERGE·Repository
- [tcf-batch/README.md](../tcf-batch/README.md)

---

> **보완 출처:** [스케줄러.md](../zdoc/스케줄러.md)

# TCF 스케줄러 정리

NSIGHT TCF의 **스케줄러**는 Spring `@Scheduled` 기반으로, **온라인 거래(`TCF.process()`)와 분리된** 주기 작업을 담당합니다.

| 관점 | 역할 | 핵심 |
|------|------|------|
| **수집 스케줄** | AP/DB/세션/배포 모니터링 | `tcf-batch` — cron 4종 |
| **운영 유지보수** | 만료 세션 정리 | `tcf-om` — fixedRate |
| **Gateway 동기화** | 활성 세션 → `TCF_USER_SESSION` | `tcf-gateway` — fixedRate |
| **업무 스케줄** | 도메인 주기 처리 | `*-service` — fixedDelay/cron |
| **기동 시 1회** | 대시보드 초기 스냅샷 | `DashboardCollectStartupRunner` |

배치 Job·테이블·OM 수동 실행: [배치관리.md](../zdoc/배치관리.md)  
온라인 vs 배치 구분: [온라인처리.md](../zdoc/온라인처리.md)

---

## 1. 개요 — 스케줄러 4계층

```text
① tcf-batch     @Scheduled(cron)      — 운영 모니터링 수집 (BAT-BATCH-001~004)
② tcf-om        @Scheduled(fixedRate)  — 세션 정리 (BAT-OM-002)
③ tcf-gateway   @Scheduled(fixedRate)  — Gateway 세션 동기화
④ *-service     @Scheduled(fixedDelay) — 업무 배치 (예: EB 이벤트 발행)
```

**공통 원칙**

1. 스케줄러는 **Handler/TCF 파이프라인을 거치지 않음** — Service 직접 호출
2. 온라인과 **독립** — 거래로그(`TCF_TX_LOG`) 비활성 (tcf-batch)
3. 실행 결과는 **`OM_BATCH_HISTORY`** 등으로 추적 (수집·정리 Job)
4. DB TX는 Facade AOP가 아닌 **`TransactionTemplate`** 또는 Service 내부 TX

---

## 2. 전체 구조

```text
@EnableScheduling (모듈별 Configuration)
   │
   ▼
Scheduler Component (@Scheduled)
   │
   ├─ warmup gate / USE_YN / enabled 플래그
   └─ Service.collectAndPersist() / runScheduled() / sync...
           │
           ├─ 대상 수집·정리·동기화
           ├─ 상태 테이블 MERGE/DELETE
           └─ OM_BATCH_HISTORY INSERT (해당 Job)
```

### `@EnableScheduling` 모듈

| 모듈 | 설정 클래스 | 스케줄러 |
|------|-------------|----------|
| `tcf-batch` | `NsightTcfBatchApplication` | 수집 4종 |
| `tcf-om` | `OmSchedulingConfiguration` | 세션 정리 |
| `tcf-gateway` | `GatewaySchedulingConfiguration` | 세션 동기화 |
| `eb-service` | `EbSchedulerConfiguration` | 이벤트 발행 |
| 기타 `*-service` | 필요 시 `*SchedulerConfiguration` | 업무별 |

---

## 3. 트리거 방식 비교

| 방식 | Spring 어노테이션 | TCF 사용처 | 특징 |
|------|-------------------|------------|------|
| **cron** | `@Scheduled(cron = "...")` | tcf-batch 수집 | 5분 주기, **초 단위 분산** (0/30/45/55초) |
| **fixedRate** | `@Scheduled(fixedRateString = "...")` | tcf-om, tcf-gateway | 이전 실행 **시작** 기준 고정 간격 |
| **fixedDelay** | `@Scheduled(fixedDelayString = "...")` | eb-service | 이전 실행 **종료** 후 대기 (겹침 방지) |
| **기동 1회** | `ApplicationRunner` + `TaskScheduler` | tcf-batch startup | cron 전 초기 스냅샷 |

**cron 분산 이유 (tcf-batch):** AP/DB/세션/배포 수집이 동시에 몰려 Actuator burst·부하를 일으키는 것을 방지.

---

## 4. tcf-batch — 수집 스케줄러 4종

| Scheduler | 프로퍼티 | 기본 cron | Job ID |
|-----------|----------|-----------|--------|
| `ApStatusCollectScheduler` | `nsight.batch.ap-status.cron` | `0 */5 * * * *` | `BAT-BATCH-001` |
| `DbStatusCollectScheduler` | `nsight.batch.db-status.cron` | `30 */5 * * * *` | `BAT-BATCH-002` |
| `SessionStatusCollectScheduler` | `nsight.batch.session-status.cron` | `45 */5 * * * *` | `BAT-BATCH-003` |
| `DeployStatusCollectScheduler` | `nsight.batch.deploy-status.cron` | `55 */5 * * * *` | `BAT-BATCH-004` |

### 실행 흐름

```27:36:tcf-batch/src/main/java/com/nh/nsight/tcf/batch/application/scheduler/ApStatusCollectScheduler.java
    @Scheduled(cron = "${nsight.batch.ap-status.cron:0 */5 * * * *}")
    public void runScheduled() {
        if (scheduledCollectSupport.skipIfWarmingUp(properties.getJobId(), log)) {
            return;
        }
        // ...
        ApStatusCollectResult result = collectService.collectAndPersist();
    }
```

```text
@Scheduled(cron)
  → ScheduledCollectSupport.skipIfWarmingUp()   ← Warmup Gate
  → *CollectService.collectAndPersist()
    → MetricsClient / JDBC 수집
    → OM_*_STATUS MERGE
    → OM_BATCH_HISTORY INSERT
```

---

## 5. Warmup Gate — Tomcat 배포 안정화

Tomcat **순차 WAR 배포** 중 cron이 먼저 돌아 실패 호출이 쏟아지는 것을 막습니다.

| 컴포넌트 | 역할 |
|----------|------|
| `BatchCollectWarmupGate` | `readyAtEpochMs = now + initial-delay-ms` |
| `ScheduledCollectSupport` | `isReady()` 전까지 스케줄 skip + 로그 |

설정: `nsight.batch.startup-collect.initial-delay-ms`

- **local (bootRun):** 보통 `0`
- **dev/prod (ztomcat):** `420000` 등 — **13 WAR** 배포 완료 대기

Warmup Gate와 Startup Runner가 **동일 delay**를 공유합니다.

---

## 6. 기동 시 초기 수집 — `DashboardCollectStartupRunner`

cron 첫 tick 전에 대시보드 데이터 공백을 줄이기 위한 **1회 수집**입니다.

| 설정 | 기본값 | 의미 |
|------|--------|------|
| `nsight.batch.startup-collect.enabled` | `true` | 기동 시 실행 |
| `nsight.batch.startup-collect.initial-delay-ms` | `0` | 지연 후 4 Job 순차 실행 |

```46:64:tcf-batch/src/main/java/com/nh/nsight/tcf/batch/support/DashboardCollectStartupRunner.java
    public void run(ApplicationArguments args) {
        if (initialDelayMs > 0) {
            taskScheduler.schedule(this::runInitialCollect, Instant.now().plusMillis(initialDelayMs));
            return;
        }
        runInitialCollect();  // AP → DB → Session → Deploy
    }
```

- `@Order(50)` — 다른 Runner 이후 실행
- `@ConditionalOnProperty` — `enabled=false`면 비활성

---

## 7. tcf-om — 세션 정리 스케줄러

| 항목 | 값 |
|------|-----|
| Scheduler | `OmSessionCleanupScheduler` |
| 트리거 | `@Scheduled(fixedRateString = "${nsight.om.session-cleanup.fixed-rate-ms:10000}")` |
| 기본 주기 | 10초 |
| Service | `OmSessionCleanupService` |
| Job ID | `BAT-OM-002` |

### 이중 활성화 제어

```35:39:tcf-om/src/main/java/com/nh/nsight/marketing/om/application/service/OmSessionCleanupService.java
    public Optional<OmSessionCleanupResult> runScheduled() {
        Map<String, Object> job = dao.selectBatchJobById(JOB_ID);
        if (!isJobEnabled(job)) {
            return Optional.empty();   // OM_BATCH_JOB.USE_YN = N 이면 skip
        }
```

| 제어 | 위치 |
|------|------|
| 코드 주기 | `application.yml` — `fixed-rate-ms` |
| 운영 on/off | `OM_BATCH_JOB.USE_YN` (DB) |

### 처리 내용

1. 만료 `SPRING_SESSION` 건수 조회 → 삭제
2. 활성 세션 재집계
3. 삭제 발생 시 또는 수동 실행 시 `OM_BATCH_HISTORY` 기록
4. DB TX: `TransactionTemplate` (timeout 10초)

수동 실행: `OM.Batch.execute` → `runManual()` — [배치관리.md](../zdoc/배치관리.md) §6

---

## 8. tcf-gateway — 세션 동기화 스케줄러

| 항목 | 값 |
|------|-----|
| Scheduler | `GatewayUserSessionSyncScheduler` |
| 트리거 | `@Scheduled(fixedRateString = "${nsight.gateway.user-session-sync.fixed-rate-ms:10000}")` |
| Service | `GatewayUserSessionSyncService.syncActiveSessions()` |
| on/off | `nsight.gateway.user-session-sync.enabled` |

`SPRING_SESSION`의 활성 세션을 **`TCF_USER_SESSION`** 에 동기화해 Gateway 관문 검증에 사용합니다.

상세: [세션관리.md](../zdoc/세션관리.md) — Gateway 4단계 검증

---

## 9. 업무 스케줄러 (`*-service`)

TCF Handler/Facade **온라인 패턴과 별개**로, 업무 WAR `application.scheduler` 패키지에 둡니다.

### 예: `eb-service` — 이벤트 발행

```15:22:eb-service/src/main/java/com/nh/nsight/marketing/eb/application/scheduler/EbEventPublishScheduler.java
    @Scheduled(fixedDelayString = "${nsight.eb.event-publish.fixed-delay-ms:60000}")
    public void publishUserEvents() {
        publishService.publishReadyEvents();
    }
```

```text
EbEventPublishScheduler (fixedDelay 60s)
  → EbEventPublishService.publishReadyEvents()
  → EpOnlineClient → POST ep-service /ep/online (TCF 온라인 호출)
```

| 특징 | 설명 |
|------|------|
| `fixedDelay` | 이전 tick **완료 후** 60초 대기 — 처리 중 겹침 방지 |
| OM Job 카탈로그 | `OM_BATCH_JOB`에 등록 가능 (표시용) |
| 실제 cron | **업무 WAR `application.yml`** 에 정의 |
| 온라인 연계 | 스케줄러 → Service → **HTTP TCF 거래** (Handler 경유) |

### 패키지 규칙

| 패키지 | 클래스 예 |
|--------|-----------|
| `application.scheduler` | `EbEventPublishScheduler` |
| `config` | `EbSchedulerConfiguration` (`@EnableScheduling`) |

네이밍: [네이밍.md](../zdoc/네이밍.md)

---

## 10. 스케줄 vs 수동 실행

동일 Service 메서드를 **자동·수동** 모두에서 재사용합니다.

| 모드 | 진입 | 용도 |
|------|------|------|
| **자동** | `@Scheduled` | 정기 관측·정리 |
| **REST** | `POST /batch/jobs/*/run` | tcf-batch 즉시 수집 |
| **OM 포털** | `OM.Batch.execute` | 운영자 수동 실행 + `executeReason` 감사 |

결과 일관성: 스케줄·REST·OM 수동 모두 `collectAndPersist()` / `runManual()` 공유.

---

## 11. 스케줄러 vs TCF 온라인 거래

| 구분 | 스케줄러 | 온라인 |
|------|--------|--------|
| 진입 | `@Scheduled` / StartupRunner | `POST /online` |
| 프레임워크 | Service 직접 | STF → Dispatcher → ETF |
| Handler | **없음** | `serviceId` 라우팅 |
| 거래로그 | tcf-batch: **비활성** | `TCF_TX_LOG` |
| DB TX | `TransactionTemplate` / Service | Facade `@Transactional` |
| 성공 판별 | Job runStatus / 로그 | `resultCode S0000` |

AOP `@Transactional`은 Facade 프록시 대상 — 스케줄러는 [AOP.md](../zdoc/AOP.md) § TransactionTemplate 사용.

---

## 12. 환경별 설정

| 항목 | local (bootRun) | dev/prod (ztomcat) |
|------|-----------------|---------------------|
| startup delay | `0` | `420000` ms 등 |
|  warmup gate | 동일 delay | 동일 delay |
| 수집 target URL | `127.0.0.1:{port}` | `${gateway}/{context}` |
| cron | 5분 (기본) | 5분 (기본, 조정 가능) |

설정 파일:

- `tcf-batch/src/main/resources/application*.yml`
- `tcf-om/.../application.yml` — `session-cleanup`
- `tcf-gateway/.../application.yml` — `user-session-sync`
- `eb-service/.../application-local.yml` — `event-publish`

---

## 13. 장애 내성

| 설계 | 내용 |
|------|------|
| Warmup skip | 배포 중 조기 실행 → skip 로그 |
| USE_YN | DB로 Job on/off (세션 정리) |
| enabled 플래그 | Gateway sync, startup-collect |
| 부분 성공 | 수집 Job — 대상별 try-catch → `PARTIAL` |
| 짧은 timeout | Actuator connect 3s / read 5s — hang 방지 |
| fixedDelay | 업무 배치 — tick 겹침 방지 |

---

## 14. 신규 스케줄러 추가 가이드

### tcf-batch 수집 Job

[배치관리.md](../zdoc/배치관리.md) §13 — CollectService + Scheduler + REST + `OM_BATCH_JOB`

### tcf-om / gateway 유지보수

1. `application.scheduler` — `@Component` + `@Scheduled`
2. `application.service` — 핵심 로직 (`TransactionTemplate` 필요 시)
3. `*SchedulingConfiguration` — `@EnableScheduling` (최초 1회)
4. `application.yml` — 주기·enabled
5. `OM_BATCH_JOB` 등록 (이력·수동 실행 연계 시)

### 업무 WAR

1. `EbEventPublishScheduler` 패턴 참고
2. `fixedDelay` vs `fixedRate` — **겹침 허용 여부**로 선택
3. 온라인 호출 필요 시 `*OnlineClient` → `/online`
4. OM Job 카탈로그 동기화 여부 검토

---

## 15. 개발자 체크리스트

- [ ] `@EnableScheduling`이 해당 모듈에 등록되어 있는가?
- [ ] cron 초 분산이 필요한 다중 Job인가? (tcf-batch 패턴)
- [ ] Tomcat 환경에서 `initial-delay-ms` / Warmup Gate를 설정했는가?
- [ ] `USE_YN` / `enabled`로 운영 on/off가 가능한가?
- [ ] 스케줄러가 Facade를 거치지 않을 때 DB TX를 `TransactionTemplate`으로 처리했는가?
- [ ] 업무 스케줄이 길어지면 `fixedDelay`로 겹침을 막았는가?
- [ ] `OM_BATCH_HISTORY`에 실행 이력이 쌓이는가?

---

## 16. 관련 문서

| 문서 | 내용 |
|------|------|
| [docs/architecture/15-schedule.md](../docs/architecture/15-schedule.md) | 스케줄 아키텍처 |
| [docs/architecture/13-batch.md](../docs/architecture/13-batch.md) | 배치·수집 상세 |
| [배치관리.md](../zdoc/배치관리.md) | Job·테이블·OM execute |
| [세션관리.md](../zdoc/세션관리.md) | 세션 수집·정리·Gateway sync |
| [온라인처리.md](../zdoc/온라인처리.md) | 온라인 vs 스케줄 |
| [AOP.md](../zdoc/AOP.md) | TransactionTemplate |
| [tcf-batch/README.md](../tcf-batch/README.md) | 수집 모듈 |

---

> **보완 출처:** [17-Batch-Scheduler.md](../zman/17-Batch-Scheduler.md)

# 17장. Batch / Scheduler 구조 — 설명

## 설계서 절 목차

17.1~17.2 개요 · 17.3~17.4 Scheduler vs Batch · 17.5~17.6 Job · 17.7~17.9 실행·Lock · 17.10~17.22 OM·이력·운영

---

## 핵심 결론

| | 역할 |
|---|------|
| **Scheduler** | **언제** |
| **Batch** | **무엇을** |
| **OM** | 등록·실행·중지·이력·재처리 |

---

## 전체 구조 (17.3)

```
운영자 → tcf-ui (OM.Batch.*)
→ OM_BATCH_JOB / OM_BATCH_SCHEDULE
→ tcf-batch (Launcher, Lock, Executor)
→ DAO → OMDB/LOGDB/SESSIONDB
→ OM Dashboard
```

## tcf-batch Job (17.6)

| Job | 적재 테이블 |
|-----|-------------|
| BAT-BATCH-001 | OM_AP_STATUS |
| BAT-BATCH-002 | OM_DB_STATUS |
| BAT-BATCH-003 | OM_SESSION_STATUS |
| BAT-BATCH-004 | OM_DEPLOY_STATUS |

## Batch 대상 확장 (17.5)

- TX Log 아카이브  
- UNKNOWN 재처리  
- 파일 보관기간 정리  
- ADW 집계  

## 실행 방식 (17.7)

| | 설명 |
|---|------|
| Cron | OM_BATCH_SCHEDULE |
| 수동 | OM Admin — **감사로그 필수** |
| 재실행 | 실패 Job 재처리 |

## 중복 실행 방지 (17.9)

```
Trigger → OM_BATCH_LOCK → Lock 획득 → Job → Lock 해제
```

다중 인스턴스 **DB Lock** 필수.

## Batch vs 온라인

- Handler 경유 ❌  
- DAO/Mapper **동일 표준**, Query Timeout 적용  

## Gap·보완

현재: `@Scheduled` + 수동 API  
목표: DB Job/스케줄, 이력, Lock, 재처리 **운영 완성** (24장 P2)

## 코드베이스

- `tcf-batch/` — 8098  
- `OmBatchHandler` — tcf-om  
- `zdoc/배치관리.md`, `zdoc/스케줄러.md`

## 이전 · 다음

← [16장 Cache](../zman/16-Cache구조.md) · [18장 파일](../zman/18-파일업다운로드.md) →


## 소스·관련 문서

| 참고 |
|------|

> docx 본문 없음 (zdoc/zman 보완)

| [배치관리.md](../zdoc/배치관리.md) |
| [스케줄러.md](../zdoc/스케줄러.md) |
| [17-Batch-Scheduler.md](../zman/17-Batch-Scheduler.md) |

## 코드베이스 정정 (develop 기준)

| 항목 | 값 |
|------|-----|
| 업무 WAR | ic, pc, ms, sv, pd, eb, ep, ss, mg + tcf-om |
| ztomcat deploy | `ztomcat/deploy-wars.sh` 13 WAR |
| buildZtomcatWars | 15 WAR |
| bootRun | gateway 8100, uj 8102, jwt 8110, ui 8099 |


---

← [44. 파일 업다운로드 기준](./44-파일-업다운로드-기준.md) · [46. OM 운영관리 개발 구조](./46-OM-운영관리-개발.md) →