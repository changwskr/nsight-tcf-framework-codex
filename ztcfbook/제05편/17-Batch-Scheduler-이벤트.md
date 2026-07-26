# 제17장. Batch · Scheduler · 이벤트

| 항목 | 내용 |
| --- | --- |
| **편** | 제5편 · 플랫폼·운영 관리 (OM) |
| **장** | 제17장 |
| **파일** | `제05편/17-Batch-Scheduler-이벤트.md` |
| **상태** | 집필 완료 |
| **목차** | [00-목차](../00-목차.md) |

---

## 17.1 Batch/Scheduler 아키텍처

NSIGHT TCF의 **`tcf-batch`**는 OM 대시보드용 운영 상태를 주기적으로 **수집·적재**하는 애플리케이션이다. 업무 배치(정산·마감·대량 이체)를 대체하지 않으며, AP/DB/세션/배포 스냅샷을 OM 테이블에 넣어 OM Admin이 조회만 하도록 분리한다.

| Job ID | 서비스 | 수집 대상 | 저장 테이블 |
| --- | --- | --- | --- |
| BAT-BATCH-001 | ApStatusCollectService | AP health·CPU·Heap·Thread | OM_AP_STATUS |
| BAT-BATCH-002 | DbStatusCollectService | DB health·Pool·JDBC ping | OM_DB_STATUS |
| BAT-BATCH-003 | SessionStatusCollectService | Spring Session + Tomcat Session | OM_SESSION_STATUS |
| BAT-BATCH-004 | DeployStatusCollectService | 배포·버전·기동 상태 | OM_DEPLOY_STATUS |

모듈 포트: bootRun **8098**, Tomcat WAR **`/batch`**. 실행 이력은 `OM_BATCH_HISTORY`에 기록된다. Job 결과 모델은 `runTime`, `runStatus`(SUCCESS/PARTIAL/FAIL), `durationMs`, target/success/fail count, snapshots를 공통으로 가진다.

아키텍처 흐름: Scheduler 또는 Manual API → CollectService → MetricsClient(Actuator/JDBC/HTTP) → Snapshot → `OmDashboardStatusRepository.upsert*` + `insertBatchHistory` → OM_*_STATUS → tcf-om Dashboard 조회. **배치가 UI를 그리지 않는다**는 원칙을 지킨다.

플랫폼 배치와 업무 배치의 경계를 명확히 한다. `tcf-batch`는 **관측·수집**만 담당하고, eb-service의 Outbox 발행 Scheduler, 업무 WAR `@Scheduled` 정산 Job은 각 모듈 책임이다. OM `OM.Batch.*`는 tcf-batch HTTP API를 원격 호출해 Job 메타·이력을 통합 조회한다.

```text
@Scheduled / POST /batch/jobs/*/run
        ▼
CollectService (001~004)
        ├─ ApMetricsClient → Actuator
        ├─ DbMetricsClient → Hikari + ping
        ├─ SessionMetricsClient → SPRING_SESSION + tomcat metric
        └─ DeployMetricsClient → manifest + info
        ▼
OmDashboardStatusRepository → OM_*_STATUS
        ▼
tcf-om OM.Dashboard.* (조회만)
```

---

## 17.2 Batch 개발 기준

배치 Job 추가·변경 시 다음 기준을 따른다. Job ID는 `BAT-BATCH-NNN` 또는 업무 배치는 `{BC}-BATCH-NNN` 명명. Scheduler는 `@Scheduled` + `ScheduledCollectSupport` 워밍업 게이트로 기동 직후 불안정 수집을 방지한다. Tomcat 배포 시 `DashboardCollectStartupRunner`의 `initial-delay-ms`를 길게 두어 WAR 순차 기동을 기다린다.

수동 실행 API:

| 엔드포인트 | 기능 |
| --- | --- |
| POST /batch/jobs/ap-status/run | AP 상태 수집 |
| POST /batch/jobs/db-status/run | DB 상태 수집 |
| POST /batch/jobs/session-status/run | 세션 상태 수집 |
| POST /batch/jobs/deploy-status/run | 배포 상태 수집 |

OM `OM.Batch.*` Handler로 Job 메타 조회·스케줄 CRUD·실행 이력 조회가 가능하다. `OmBatchRemoteClient`가 tcf-batch HTTP API를 호출한다. 업무 WAR 내부 `@Scheduled`는 **도메인 배치**(eb Outbox 발행 등)에 한정하고, 플랫폼 수집은 tcf-batch에 모은다.

장애 시에도 `OM_BATCH_HISTORY`에 FAIL을 남겨 관측 가능하게 유지한다. `SessionMetricsClient`는 외부 WAR metric 미존재 시 health UP이면 0으로 수집하는 완충 로직을 갖는다. AP 수집 대상 scope는 `OM-PORTAL`(spring-session), `{CODE}-AP`(actuator) 등이다.

신규 Collect Job 추가 체크리스트:

| # | 항목 |
| --- | --- |
| 1 | Job ID `BAT-BATCH-NNN` 등록 |
| 2 | CollectService + Scheduler 구현 |
| 3 | Snapshot DTO + Repository upsert |
| 4 | Manual API `/batch/jobs/{type}/run` |
| 5 | OM Batch Handler 연동 |
| 6 | Dashboard 화면 필드 추가 |

`ScheduledCollectSupport`는 application ready 후 N초간 수집 skip하여 Tomcat cold start·Actuator 미준비 구간의 FAIL flood를 방지한다. ztomcat 통합 환경에서는 `nsight.batch.startup-collect.initial-delay-ms`를 120000 이상으로 두는 사례가 많다.

---

## 17.3 tcf-batch 모니터링 수집

세션 수집(`SessionStatusCollectService`)은 `SPRING_SESSION` 직접 집계와 각 WAR `tomcat.sessions.active.current` metric을 scope별로 저장한다. `activeCount`, `expiredCount`, `totalCount`, `uniqueUserCount`, `healthStatus`(NORMAL/WARN/FAIL)가 핵심 필드이다.

AP 수집은 Actuator health·metrics endpoint를 HTTP로 호출한다. bootRun 포트 매핑표(zarchitecture/16)에 없는 WAR는 수집 skip 또는 FAIL로 기록한다. DB 수집은 HikariCP pool active/idle, JDBC ping latency를 측정한다.

Deploy 수집은 WAR manifest·build-info·Actuator info를 조합해 버전 문자열을 만든다. OM Deploy 화면과 CI/CD Artifact 버전을 대조해 drift를 탐지한다. 수집 주기는 `application.yml`의 cron/fixed-rate로 환경별 조정한다.

운영 체크: tcf-batch 기동, OM Dashboard 데이터 갱신 시각, `OM_BATCH_HISTORY` 최근 SUCCESS, Tomcat 전환 후 startup-collect delay 충분 여부. 장애 대응 FAQ(매뉴얼 70장)에 "Dashboard 빈 화면 → batch Job 수동 run"을 포함한다.

AP scope별 수집 URL 예(bootRun):

| scope | health URL |
| --- | --- |
| SV-AP | http://127.0.0.1:8086/sv/actuator/health |
| OM-AP | http://127.0.0.1:8097/om/actuator/health |
| GW-AP | http://127.0.0.1:8100/actuator/health |

PARTIAL runStatus는 일부 target FAIL·나머지 SUCCESS일 때 기록된다. Dashboard는 최신 스냅샷만 표시하므로, FAIL target scope를 `OM_BATCH_HISTORY` detail JSON에서 확인한다.

---

## 17.4 이벤트 연계 (EB/EP)

업무 **이벤트 연계** 샘플은 **`eb-service`(Event Bridge)**와 **`ep-service`(Event Processing)** WAR이다. BC EB(8089), EP(8090). 사용자 등록 → Outbox(`EB_EVENT` READY) → Scheduler 발행 → EP `EP.UserEvent.receive` 수신 → `EP_USER_EVENT` 저장 흐름이다.

```text
Client → EB.User.create → EB_USER + EB_EVENT(READY)
@Scheduled EbEventPublishScheduler
  → SELECT READY → POST ep-service /ep/online
  → SUCCESS → EB_EVENT SENT
```

eb Handler: `EbUserHandler`, `EbEventHandler`, `EbBatchHandler` 등. ep Handler: `EpUserEventHandler` 등. Outbox 패턴으로 **DB 트랜잭션과 이벤트 발행 원자성**을 확보한다. EP 호출 실패 시 EB_EVENT FAIL·재시도 정책을 `nsight.eb.event-publish` 설정으로 조정한다.

EB/EP는 TCF 6계층·Online Endpoint·Catalog·TC 등록을 동일하게 따른다. 이벤트 페이로드는 표준 전문 body DTO로 정의하고, idempotency key로 EP 중복 수신을 방지한다. Kafka·MQ 전환은 설계 Gap으로 zman/23·24 보완 과제에 포함될 수 있다.

ss-service·mg-service 등 다른 WAR와의 이벤트 연계도 동일 패턴(Outbox + Scheduler + EAI Client)을 참고한다. 제9편 eb/ep 모듈 레퍼런스와 zguide eb/ep 개발가이드에 Handler·테이블·설정 상세가 있다.

Outbox 상태 전이:

| STATUS | 의미 |
| --- | --- |
| READY | 트랜잭션 커밋 후 발행 대기 |
| SENT | EP 수신 성공 |
| FAIL | EP 호출 실패·재시도 대상 |

EP `EP.UserEvent.receive`는 멱등 처리 필수이다. 동일 eventId 재전송 시 duplicate insert 없이 SUCCESS를 반환해야 EB Scheduler가 무한 재시도하지 않는다.

---

## 17.5 배치·이벤트 운영 체크리스트

일일 운영 점검: tcf-batch 프로세스 alive, `OM_BATCH_HISTORY` 최근 4 Job SUCCESS, Dashboard `COLLECTED_AT` 15분 이내(주기에 맞게). Tomcat 재기동 후 batch startup delay 경과 전 Dashboard blank는 정상일 수 있다.

이벤트 연계 운영: EB_EVENT FAIL 건수, EP 큐 적체, 재시도 max 초과 알람. EB Scheduler down 시 READY 이벤트가 적체되므로 eb-service health를 batch와 별도 모니터링한다.

| 구분 | P1 징후 | 조치 |
| --- | --- | --- |
| tcf-batch down | Dashboard 전체 stale | batch 재기동 + manual run |
| AP Job FAIL | Actuator unreachable | WAR·방화벽·포트 |
| EB FAIL 급증 | EP down | ep-service 복구 + READY 재발행 |
| Session WARN | uniqueUser spike | OM.Session.* 조회 |

DR 시 tcf-batch는 OM Dashboard 가시성에만 영향하고 **온라인 거래 경로와 분리**된다. 그러나 장기 batch 중단은 운장 blind spot을 만들므로 DR 센터 기동 checklist에 batch WAR 포함을 권장한다.

업무 도메인 배치(정산·마감)는 별도 runbook·RTO를 가진다. 플랫폼 tcf-batch Job과 혼동하지 않도록 Job ID prefix(`BAT-` vs `{BC}-`)로 문서·알람을 분리한다.

---

## 장 요약

tcf-batch는 AP·DB·세션·배포 상태를 수집해 OM Dashboard 테이블만 갱신하는 플랫폼 배치이다. Scheduler·Manual API·기동 시 초기 수집으로 운영 가시성을 확보한다. 업무 도메인 배치와 분리하며 OM.Batch와 연동한다. EB/EP는 Outbox·Scheduler 기반 이벤트 연계 샘플로 TCF 표준 거래 모델을 따른다.

---

## 이전 · 다음

| | |
| --- | --- |
| ← 이전 | [제16장 API Gateway · UI 채널](../제05편/16-API-Gateway-UI-채널.md) |
| → 다음 | [제18장 데이터·DB 아키텍처](../제05편/18-데이터-DB-아키텍처.md) |

---

## 출처 색인

| 절 | 참고 문서 |
| --- | --- |
| 17.1 | [docs/architecture/13-batch.md](../../docs/architecture/13-batch.md), [zarchitecture/12-배치-모니터링-아키텍처.md](../../zarchitecture/12-배치-모니터링-아키텍처.md) |
| 17.2 | [znsight-man/45-Batch-Scheduler-개발.md](../../znsight-man/45-Batch-Scheduler-개발.md) |
| 17.3 | [zguide/tcf-batch-개발가이드.md](../../zguide/tcf-batch-개발가이드.md), [docs/architecture/10-session.md](../../docs/architecture/10-session.md) |
| 17.4 | [zarchitecture/14-이벤트-연계-아키텍처.md](../../zarchitecture/14-이벤트-연계-아키텍처.md), [zguide/eb-service-개발가이드.md](../../zguide/eb-service-개발가이드.md), [zguide/ep-service-개발가이드.md](../../zguide/ep-service-개발가이드.md) |
| 17.5 | [znsight-man/70-장애-FAQ.md](../../znsight-man/70-장애-FAQ.md), [ztcfbook/부록/J-운영-전환-체크리스트.md](../부록/J-운영-전환-체크리스트.md) |
