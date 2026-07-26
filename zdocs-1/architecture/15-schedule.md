# 15. 스케줄 아키텍처

| 항목 | 내용 |
|------|------|
| 문서 번호 | 15 |
| 제목 | Schedule Architecture |
| 상위 문서 | [architecture.md](architecture.md) |
| 관련 문서 | [13-batch.md](13-batch.md), [10-session.md](10-session.md), [11-login.md](11-login.md) |
| 구현 모듈 | `tcf-batch`, `tcf-om` |
| 대상 | 배치/운영/플랫폼 개발자 |

---

## 1. 개요

NSIGHT TCF의 스케줄은 크게 두 축으로 운영된다.

| 축 | 모듈 | 역할 |
|----|------|------|
| **상태 수집 스케줄** | `tcf-batch` | AP/DB/세션/배포 상태 주기 수집 |
| **운영 유지보수 스케줄** | `tcf-om` | 만료 세션 정리 등 운영 하우스키핑 |

공통 목표:

1. 온라인 거래 성능과 독립적으로 운영성 데이터를 갱신
2. 스케줄 실패/부분성공을 실행 이력으로 남겨 추적 가능하게 유지

---

## 2. 전체 구조

```text
@Scheduled Trigger
   │
   ▼
Scheduler Component
   │
   ├─ warmup gate / 실행조건 체크
   └─ Collect/Cleanup Service 호출
           │
           ├─ 대상 수집 또는 정리 실행
           ├─ 상태 테이블 갱신(MERGE/DELETE)
           └─ 실행 이력 저장(OM_BATCH_HISTORY)
```

---

## 3. `tcf-batch` 스케줄 아키텍처

## 3.1 배치 스케줄러 구성

| Scheduler 클래스 | 프로퍼티 키 | 기본 cron | Job ID |
|------------------|-------------|-----------|--------|
| `ApStatusCollectScheduler` | `nsight.batch.ap-status.cron` | `0 */5 * * * *` | `BAT-BATCH-001` |
| `DbStatusCollectScheduler` | `nsight.batch.db-status.cron` | `30 */5 * * * *` | `BAT-BATCH-002` |
| `SessionStatusCollectScheduler` | `nsight.batch.session-status.cron` | `45 */5 * * * *` | `BAT-BATCH-003` |
| `DeployStatusCollectScheduler` | `nsight.batch.deploy-status.cron` | `55 */5 * * * *` | `BAT-BATCH-004` |

각 스케줄러는 공통적으로:

1. `ScheduledCollectSupport.skipIfWarmingUp(...)` 확인
2. `collectAndPersist()` 실행
3. 시작/종료 로그 기록

## 3.2 스케줄 시간 분산 설계

기본 cron이 5분 주기라도 초(second)를 분산시킨 이유:

- AP/DB/세션/배포 수집이 동시에 몰려 시스템 부하를 일으키는 것을 방지
- 대상 서비스 Actuator 호출 burst 감소

---

## 4. Startup 수집 스케줄 아키텍처

`DashboardCollectStartupRunner`는 앱 시작 시 초기 수집을 담당한다.

관련 설정:

| 설정 | 기본값 | 의미 |
|------|--------|------|
| `nsight.batch.startup-collect.enabled` | `true` | 기동 시 초기 수집 실행 여부 |
| `nsight.batch.startup-collect.initial-delay-ms` | `0` | 초기 수집 지연 시간 |

Tomcat 프로파일에서는 `initial-delay-ms`를 크게 둬서(예: 420000ms), WAR 순차 배포 완료 후 수집이 시작되도록 설계했다.

---

## 5. Warmup Gate 아키텍처

Tomcat 통합 배포 중 조기 스케줄 실행을 막기 위한 안전장치:

- `BatchCollectWarmupGate`
- `ScheduledCollectSupport`

동작:

1. 기동 시 `readyAtEpochMs = now + initial-delay-ms`
2. `isReady()` 전까지 스케줄러는 skip
3. skip 시 남은 ms를 로그로 기록

효과:

- 배포 중(아직 기동되지 않은 대상)에 대한 실패성 호출 감소
- 초기 실행 실패 노이즈 최소화

---

## 6. `tcf-om` 스케줄 아키텍처 (세션 정리)

`OmSessionCleanupScheduler`는 고정 주기 실행으로 만료 세션을 정리한다.

| 항목 | 값 |
|------|----|
| Scheduler | `OmSessionCleanupScheduler` |
| 트리거 | `@Scheduled(fixedRateString = "${nsight.om.session-cleanup.fixed-rate-ms:10000}")` |
| 기본 주기 | 10초 |
| 서비스 | `OmSessionCleanupService` |
| Job ID | `BAT-OM-002` |

## 6.1 실행 조건

`OmSessionCleanupService.runScheduled()` 내부에서 `BAT-OM-002` Job의 `USE_YN`을 확인해 활성화 여부를 제어한다.

즉, 코드 설정 + DB Job 사용여부가 결합된 이중 제어 구조다.

## 6.2 처리 내용

1. 만료 세션 건수 조회
2. 만료 세션 삭제
3. 활성 세션 건수 재조회
4. 조건 충족 시 `OM_BATCH_HISTORY` 기록

수동 실행(`runManual`)과 스케줄 실행 모두 동일 핵심 로직을 사용한다.

---

## 7. 스케줄 데이터 흐름

## 7.1 수집 스케줄 (`tcf-batch`)

```text
@Scheduled
  → CollectService.collectAndPersist()
    → MetricsClient/JDBC 수집
    → Snapshot 생성
    → OM_*_STATUS MERGE
    → OM_BATCH_HISTORY INSERT
```

## 7.2 정리 스케줄 (`tcf-om`)

```text
@Scheduled(fixedRate)
  → OmSessionCleanupService.runScheduled()
    → SPRING_SESSION 만료 건 정리
    → 활성 건 집계
    → OM_BATCH_HISTORY INSERT (조건부)
```

---

## 8. 스케줄 설정 아키텍처

## 8.1 공통 설정 위치

- `tcf-batch/src/main/resources/application.yml`
- `application-local.yml` / `application-dev.yml` / `application-prod.yml`
- `tcf-om/src/main/resources/application.yml` (`session-cleanup`)

## 8.2 환경별 차이

| 항목 | local (bootRun) | dev / prod (Tomcat WAR) |
|------|-----------------|-------------------------|
| startup delay | 보통 0 | 대기 시간 큼 (배포 안정화) |
| 대상 URL | 개별 포트 | 게이트웨이 context |
| 수집 대상 수 | 개발용 subset 가능 | 19 context 전체 |

---

## 9. 수동 실행과 스케줄 실행의 관계

스케줄은 자동, API는 수동/강제 실행을 담당한다.

| 모드 | 경로 | 용도 |
|------|------|------|
| 자동 | `@Scheduled` | 정기 관측 유지 |
| 수동 | `POST /batch/jobs/*/run` | 즉시 점검/장애 대응 |
| OM 수동 | `OM.Batch.execute` | 운영 포털에서 배치 재실행 |

동일 `collectAndPersist()` 로직을 재사용하므로 결과 일관성이 높다.

---

## 10. 장애 내성/운영성 설계

| 설계 포인트 | 설명 |
|-------------|------|
| 부분 성공 허용 | 한 대상 실패해도 전체 Job은 `PARTIAL` 처리 가능 |
| 상태 스냅샷 유지 | 실패 대상도 FAIL 상태로 upsert |
| 이력 저장 | `OM_BATCH_HISTORY`로 실행 결과 추적 |
| 워밍업 게이트 | 배포 직후 오탐/오류 폭주 방지 |

---

## 11. 성능/부하 관점

1. 초 단위 cron 분산(0/30/45/55초)으로 burst 완화
2. 수집 요청 timeout을 짧게 설정해 hang 회피
3. MERGE upsert로 대량 insert 누적 억제
4. startup 지연으로 cold-start 실패 감소

---

## 12. 운영 체크리스트

- [ ] 각 cron이 환경 의도(운영/개발)와 맞는가
- [ ] startup delay가 실제 배포 시간과 맞는가
- [ ] warmup skip 로그가 과도하지 않은가
- [ ] `OM_BATCH_HISTORY`가 주기적으로 쌓이고 상태가 정상인가
- [ ] `BAT-OM-002` `USE_YN` 설정이 의도대로 관리되는가

---

## 13. 참고 소스

| 파일 | 설명 |
|------|------|
| `tcf-batch/.../job/*CollectScheduler.java` | 배치 스케줄러 |
| `tcf-batch/.../support/DashboardCollectStartupRunner.java` | 기동 시 초기 수집 |
| `tcf-batch/.../support/BatchCollectWarmupGate.java` | 워밍업 게이트 |
| `tcf-batch/.../support/ScheduledCollectSupport.java` | 스케줄 skip 공통 처리 |
| `tcf-batch/src/main/resources/application*.yml` | cron/대상 설정 |
| `tcf-om/.../batch/OmSessionCleanupScheduler.java` | OM 세션 정리 스케줄 |
| `tcf-om/.../service/OmSessionCleanupService.java` | 정리 로직 + 이력 기록 |

---

## 14. 변경 이력

| 일자 | 변경 내용 |
|------|-----------|
| 2026-06 | 최초 작성 — 배치/정리 스케줄 통합 아키텍처 정리 |
