# tcf-batch 개발자 가이드

> **역할:** OM 대시보드용 **AP/DB/세션/배포 상태 수집** 배치  
> **포트:** **8098** · Context **`/batch`**

---

## 1. 이 모듈이 하는 일

Actuator·JDBC·Spring Session 등에서 메트릭을 수집해 OM DB 테이블에 적재합니다.  
tcf-om 대시보드가 이 데이터를 조회합니다.

| Job ID | 설명 | 저장 테이블 |
|--------|------|-------------|
| `BAT-BATCH-001` | AP 상태 (CPU/Heap/Thread) | `OM_AP_STATUS` |
| `BAT-BATCH-002` | DB 상태 | `OM_DB_STATUS` |
| `BAT-BATCH-003` | 세션 현황 | `OM_SESSION_STATUS` |
| `BAT-BATCH-004` | 배포 현황 | `OM_DEPLOY_STATUS` |

---

## 2. 5분 빠른 시작

```bash
# 1) OM + 대상 AP 기동
gradle :tcf-om:bootRun        # 8097
gradle :sv-service:bootRun    # 8086

# 2) 배치 기동
gradle :tcf-batch:bootRun     # 8098

# 3) 수동 수집
curl -X POST http://localhost:8098/batch/jobs/ap-status/run

# 4) 대시보드 확인 (tcf-ui)
# http://localhost:8099/om/admin/dashboard.html
```

---

## 3. 실행

| | |
|---|---|
| bootRun | `gradle :tcf-batch:bootRun` |
| 스크립트 | `tcf-batch/scripts/run-local.bat` |
| ztomcat | `http://localhost:8080/batch/jobs/ap-status/run` |

**메인:** `com.nh.nsight.tcf.batch.NsightTcfBatchApplication`  
**공유 DB:** `data/nsight-txlog/nsight_om` (tcf-om과 동일 H2)

---

## 4. 프로파일

| Profile | 수집 대상 base-url |
|---------|-------------------|
| `local` | 개별 bootRun 포트 (8097, 8086 …) |
| `dev` | `http://127.0.0.1:8080/{context}` |
| `prod` | `${NSIGHT_GATEWAY_BASE_URL}/{context}` |

---

## 5. 수동 실행 API

| 엔드포인트 | 설명 |
|------------|------|
| `POST /batch/jobs/ap-status/run` | AP |
| `POST /batch/jobs/db-status/run` | DB |
| `POST /batch/jobs/session-status/run` | 세션 |
| `POST /batch/jobs/deploy-status/run` | 배포 |

---

## 6. OM 연동

tcf-om 설정:

```yaml
nsight.om.batch-service-url: http://127.0.0.1:8098/batch   # bootRun
# Tomcat: http://127.0.0.1:8080/batch
```

OM Admin → 배치 화면에서 Job `BAT-BATCH-001`~`004` 수동 재실행 가능.

---

## 7. 패키지 구조

```
com.nh.nsight.tcf.batch
├── application/service/    ApStatusCollectService, …
├── application/scheduler/  ApStatusCollectScheduler, …
├── client/                 ApMetricsClient, …
├── entry/web/              ApStatusBatchController, …
└── persistence/repository/ OmDashboardStatusRepository
```

---

## 8. 사전 조건

- 수집 대상 WAS: Actuator `health`, `metrics` 노출
- tcf-om H2/DB 공유

---

## 9. 참고

| | |
|---|---|
| [tcf-batch/README.md](../tcf-batch/README.md) | |
| [tcf-om-개발가이드.md](./tcf-om-개발가이드.md) | 대시보드 |
| [docs/architecture/25-env-profile.md](../docs/architecture/25-env-profile.md) | 프로파일 |
