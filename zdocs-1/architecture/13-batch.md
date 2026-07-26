# 13. 배치 아키텍처

| 항목 | 내용 |
|------|------|
| 문서 번호 | 13 |
| 제목 | Batch Architecture |
| 상위 문서 | [architecture.md](architecture.md) |
| 관련 문서 | [10-session.md](10-session.md), [09-transaction log.md](09-transaction log.md), [08-timeout.md](08-timeout.md) |
| 구현 모듈 | `tcf-batch`, `tcf-om` |
| 대상 | 운영/배치/플랫폼 개발자 |

---

## 1. 개요

`tcf-batch`는 OM 대시보드용 운영 상태(AP/DB/세션/배포)를 주기적으로 수집해 OM 테이블에 적재하는 **수집 전용 배치 애플리케이션**이다.

| 구분 | 내용 |
|------|------|
| 실행 모듈 | `tcf-batch` |
| 역할 | 상태 수집 + 스냅샷 저장 + 실행 이력 기록 |
| 데이터 저장 | `OM_AP_STATUS`, `OM_DB_STATUS`, `OM_SESSION_STATUS`, `OM_DEPLOY_STATUS`, `OM_BATCH_HISTORY` |
| 포트 | bootRun `:8098`, Tomcat WAR `/batch` |

핵심 원칙:

1. 배치가 직접 운영 화면을 그리지 않고 **상태 테이블**만 갱신
2. OM 화면은 해당 테이블을 조회만 수행
3. 수집 실패도 이력으로 남겨 관측 가능하게 유지

---

## 2. 아키텍처 구조

```text
Scheduler / Manual API
   │
   ▼
CollectService (AP/DB/Session/Deploy)
   │
   ├─ MetricsClient (Actuator/JDBC/HTTP)
   │
   ├─ Snapshot 모델 생성
   │
   └─ OmDashboardStatusRepository.upsert*
         + insertBatchHistory
               │
               ▼
      OM_*_STATUS / OM_BATCH_HISTORY
               │
               ▼
          tcf-om Dashboard 조회
```

---

## 3. Job 모델

기본 Job 4종:

| Job ID | 서비스 | 수집 대상 | 저장 테이블 |
|--------|--------|----------|-------------|
| `BAT-BATCH-001` | `ApStatusCollectService` | AP health/CPU/Heap/Thread | `OM_AP_STATUS` |
| `BAT-BATCH-002` | `DbStatusCollectService` | DB health/pool/JDBC ping | `OM_DB_STATUS` |
| `BAT-BATCH-003` | `SessionStatusCollectService` | Spring Session + Tomcat Session | `OM_SESSION_STATUS` |
| `BAT-BATCH-004` | `DeployStatusCollectService` | 배포/버전/기동 상태 | `OM_DEPLOY_STATUS` |

모든 Job은 동일한 결과 모델 패턴을 가진다.

- `runTime`
- `runStatus` (`SUCCESS`/`PARTIAL`/`FAIL`)
- `durationMs`
- `targetCount/successCount/failCount`
- `snapshots`

---

## 4. 실행 방식

## 4.1 스케줄 실행

각 Scheduler가 `@Scheduled`로 주기 실행한다.

예:

- `ApStatusCollectScheduler`
- `DbStatusCollectScheduler`
- `SessionStatusCollectScheduler`
- `DeployStatusCollectScheduler`

공통 패턴:

1. 워밍업 게이트(`ScheduledCollectSupport`) 확인
2. `collectAndPersist()` 호출
3. 시작/종료 로그 기록

## 4.2 수동 실행 API

`/jobs/{type}/run` 엔드포인트로 즉시 실행 가능.

| 엔드포인트 | 기능 |
|------------|------|
| `POST /batch/jobs/ap-status/run` | AP 상태 수집 |
| `POST /batch/jobs/db-status/run` | DB 상태 수집 |
| `POST /batch/jobs/session-status/run` | 세션 상태 수집 |
| `POST /batch/jobs/deploy-status/run` | 배포 상태 수집 |

`GET /jobs/{type}`는 Job 메타 정보(`jobId`, 설명, 실행 경로) 제공.

## 4.3 기동 시 초기 수집

`DashboardCollectStartupRunner`가 애플리케이션 시작 시 1회 초기 수집을 수행한다.

- `nsight.batch.startup-collect.enabled`
- `nsight.batch.startup-collect.initial-delay-ms`

Tomcat 배포에서는 초기 지연을 길게 두어(WAR 순차배포 대기) 수집 안정성을 확보한다.

---

## 5. 수집 대상/소스 아키텍처

## 5.1 프로파일 기반 대상 분기

| 프로파일 | 대상 URL 전략 |
|----------|---------------|
| `local` | `http://127.0.0.1:{port}` 개별 포트 |
| `dev` / `prod` | `${nsight.gateway.base-url}/{context}` 게이트웨이 |

구성 파일:

- `application.yml` (공통/기본 스케줄)
- `application-local.yml` (bootRun 수집 타겟)
- `application-dev.yml` (ztomcat 19 context)
- `application-prod.yml` (운영 게이트웨이 URL)

## 5.2 source-type 패턴

| source-type | 의미 | 예 |
|-------------|------|----|
| `actuator` | 대상 WAR Actuator 호출 | CPU/Heap/DB/Session/Deploy |
| `jdbc` | 직접 JDBC ping/조회 | `LOGDB` 상태, Spring Session 집계 |
| `spring-session` | `SPRING_SESSION` 직접 집계 | `OM-PORTAL` 세션 |

---

## 6. 저장소(Repository) 아키텍처

`OmDashboardStatusRepository`가 배치 저장을 담당한다.

주요 동작:

- `upsertAp`, `upsertDb`, `upsertSession`, `upsertDeploy`
- `MERGE INTO ... KEY(...)` 사용으로 idempotent upsert
- `insertBatchHistory`로 실행 이력 별도 저장

이점:

1. 대상별 스냅샷 최신 상태 유지
2. 동일 키 재실행 시 중복 누적 없이 갱신
3. 실패/부분성공 이력 추적 가능

---

## 7. 상태 판정 아키텍처

각 수집 서비스는 metric/JDBC 결과를 `healthStatus`로 정규화한다.

예(AP):

- CPU/Heap 임계치 기반 `NORMAL/WARN/FAIL`
- Actuator unreachable 시 `FAIL` + 상세 메시지

예(Session):

- reachability + active/expired 조건 기반 판정

예(DB/Deploy):

- health endpoint/JDBC ping 결과 기반 판정

일관된 상태코드로 OM 대시보드 집계/표시에 재사용한다.

---

## 8. 세션 수집 특화 구조

세션은 혼합 소스 수집이 핵심이다.

| scope | 수집 방식 | 설명 |
|-------|-----------|------|
| `OM-PORTAL` | Spring Session JDBC | 운영포털 로그인 세션 |
| `{CODE}-AP` | Actuator metric | 각 WAR HTTP Session 활성수 |

설계 포인트:

- OM Spring Session과 Tomcat HTTP Session을 구분
- 중복 scope(예: legacy/OM-AP 중복)는 마이그레이션에서 정리

---

## 9. OM 연동 아키텍처

`tcf-om`은 배치를 직접 수행하지 않고 원격 호출한다.

- `nsight.om.batch-service-url`로 batch endpoint 지정
- `OmBatchRemoteClient`가 `/jobs/*/run` 호출
- OM 배치 관리 화면에서 수동 실행 가능

즉, **실행은 tcf-batch**, **오케스트레이션/조회는 tcf-om**으로 역할 분리.

---

## 10. 트랜잭션/로그/장애 처리

## 10.1 트랜잭션

- 배치는 `MERGE` 단위로 상태 스냅샷을 기록
- `tcf-batch`는 `nsight.tcf.transaction-log-enabled: false` (온라인 거래로그 비활성)

## 10.2 이력 로그

- `OM_BATCH_HISTORY`에 실행 결과 저장
- 상태값: `SUCCESS`, `PARTIAL`, `FAIL`
- 메시지에 대상수/성공수/실패수 기록

## 10.3 실패 내성

- 대상별 수집은 개별 try-catch로 격리
- 일부 대상 실패해도 전체 Job은 `PARTIAL`로 종료 가능
- unreachable 대상도 FAIL 스냅샷으로 저장(관측 공백 최소화)

---

## 11. 타임아웃/성능 설정

대표 설정(`application.yml`):

| 항목 | 값(예) | 의미 |
|------|--------|------|
| `ap-status.connect-timeout-ms` | 3000 | AP 대상 연결 제한 |
| `ap-status.read-timeout-ms` | 5000 | AP 응답 대기 제한 |
| cron | 5분 간격 계열 | 수집 주기 |
| startup delay | 환경별 0~420000ms | 기동 직후 안정화 대기 |

원칙:

- 수집 timeout은 짧게, 이력은 반드시 남긴다
- 장시간 hang보다 빠른 FAIL/재시도 전략을 우선

---

## 12. 배치 개발 확장 가이드

신규 Job 추가 시 표준 단계:

1. `config`에 `{domain}BatchProperties` 추가
2. `client`에서 source-type별 수집 로직 구현
3. `model`에 Snapshot/Result 정의
4. `service`에 `collectAndPersist()` 구현
5. `repository` upsert/이력 저장 재사용
6. `job` Scheduler + `web` Controller 추가
7. `application*.yml` 타겟/cron 등록

---

## 13. 체크리스트

- [ ] 수집 대상 Actuator `health/metrics` 노출 여부 확인
- [ ] 프로파일(local/dev/prod)에 맞는 base-url 설정 확인
- [ ] `OM_*_STATUS`와 `OM_BATCH_HISTORY` 스키마 준비 확인
- [ ] startup delay가 배포 방식과 맞는지 확인
- [ ] 부분 실패(`PARTIAL`) 경고 모니터링 체계 확인

---

## 14. 참고 소스

| 파일 | 설명 |
|------|------|
| `tcf-batch/README.md` | 모듈 개요/실행법 |
| `tcf-batch/.../service/*CollectService.java` | 수집/판정/저장 핵심 |
| `tcf-batch/.../job/*CollectScheduler.java` | 스케줄 실행 |
| `tcf-batch/.../web/*BatchController.java` | 수동 실행 API |
| `tcf-batch/.../repository/OmDashboardStatusRepository.java` | MERGE/upsert 저장소 |
| `tcf-batch/src/main/resources/application*.yml` | 타겟/주기/프로파일 설정 |
| `tcf-om/.../support/OmBatchRemoteClient.java` | OM 원격 실행 연동 |

---

## 15. 변경 이력

| 일자 | 변경 내용 |
|------|-----------|
| 2026-06 | 최초 작성 — tcf-batch 수집·저장·운영 연동 아키텍처 정리 |
