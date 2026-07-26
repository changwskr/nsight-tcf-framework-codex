# eb-service 개발자 가이드

> **Event Bridge (EB)** · 포트 **8089** · Context **`/eb`** · 업무코드 **`EB`**

---

## 1. 이 모듈이 하는 일

이벤트 브릿지 — 사용자·이벤트 등록 후 **Outbox 패턴**으로 ep-service에 이벤트를 발행합니다.

```
EB.User.create → EB_USER + EB_EVENT(READY)
@Scheduled 배치 → POST ep-service /ep/online (EP.UserEvent.receive)
성공/실패 → EB_EVENT = SENT / FAIL
```

---

## 2. 5분 빠른 시작

```bash
gradle :eb-service:bootRun

curl -X POST http://127.0.0.1:8089/eb/online ^
  -H "Content-Type: application/json" ^
  -d @tcf-ui/src/main/resources/sample-requests/eb-sample-inquiry.json
```

UI: `http://localhost:8099/eb/index.html`

---

## 3. 실행

| | |
|---|---|
| bootRun | `gradle :eb-service:bootRun` |
| 스크립트 | `tcf-scripts/run-local.bat eb` |
| ztomcat | `http://localhost:8080/eb/online` |

**메인:** `com.nh.nsight.marketing.eb.NsightEbServiceApplication`

---

## 4. Handler · serviceId

| Handler | serviceId | transactionCode |
|---------|-----------|-----------------|
| `EbSampleHandler` | `EB.Sample.inquiry` | EB-INQ-0001 |
| `EbUserHandler` | `EB.User.inquiry`, `EB.User.create` | EB-USR-* |
| `EbEventHandler` | `EB.Event.inquiry` | EB-EVT-0001 |
| `EbBatchHandler` | `EB.Batch.inquiry` | EB-BAT-0001 |

---

## 5. 주요 테이블

| 테이블 | 역할 |
|--------|------|
| `EB_USER` | 사용자 |
| `EB_EVENT` | EP 발행 대기 (`READY` / `SENT` / `FAIL`) |

---

## 6. EB → EP 연동

- **클라이언트:** `EpOnlineClient` — `POST /ep/online`
- **설정:** `application-local.yml` → `nsight.eb.event-publish` (기본 60초, EP URL 8090)
- **스케줄러:** `EbEventPublishScheduler`

EP 가이드: [ep-service-개발가이드.md](./ep-service-개발가이드.md)

> WAR 간 Java 직접 참조 대신 HTTP 호출. tcf-eai 도입 시 동일 패턴 적용 가능.

---

## 7. 프로젝트 구조

```
com.nh.nsight.marketing.eb
├── application/service/   EbUserService, EbEventService, EbEventPublishService
├── application/scheduler/ EbEventPublishScheduler
├── client/                EpOnlineClient
├── entry/handler/         EbSample, EbUser, EbEvent, EbBatch
└── persistence/           EbUserDao, EbEventDao, …
```

---

## 8. 의존성

`tcf-util`, `tcf-core`, `tcf-web`

---

## 9. 전체 시나리오 테스트

```bash
gradle :eb-service:bootRun   # 8089
gradle :ep-service:bootRun   # 8090
# EB.User.create → 배치 대기 → EP.UserEvent.receive 확인
```

---

## 10. 참고

| | |
|---|---|
| [eb-service/README.md](../eb-service/README.md) | |
| [ep-service-개발가이드.md](./ep-service-개발가이드.md) | EP 수신 |
| [zman/08-업무Handler개발.md](../zman/08-업무Handler개발.md) | |
