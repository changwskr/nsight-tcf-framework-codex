# ep-service 개발자 가이드

> **Event Processing (EP)** · 포트 **8090** · Context **`/ep`** · 업무코드 **`EP`**

---

## 1. 이 모듈이 하는 일

이벤트 처리 — eb-service 배치가 전달한 이벤트를 수신·저장합니다.

---

## 2. 5분 빠른 시작

```bash
gradle :ep-service:bootRun

curl -X POST http://127.0.0.1:8090/ep/online ^
  -H "Content-Type: application/json" ^
  -d @tcf-ui/src/main/resources/sample-requests/ep-sample-inquiry.json
```

UI: `http://localhost:8099/ep/index.html`

---

## 3. 실행

| | |
|---|---|
| bootRun | `gradle :ep-service:bootRun` |
| 스크립트 | `tcf-scripts/run-local.bat ep` |
| ztomcat | `http://localhost:8080/ep/online` |

**메인:** `com.nh.nsight.marketing.ep.NsightEpServiceApplication`

---

## 4. Handler · serviceId

| Handler | serviceId | 설명 |
|---------|-----------|------|
| `EpSampleHandler` | `EP.Sample.inquiry` | 샘플 조회 |
| `EpUserEventHandler` | `EP.UserEvent.inquiry` | 수신 이벤트 목록 (페이징) |
| | `EP.UserEvent.receive` | EB 배치 이벤트 수신 → `EP_USER_EVENT` 저장 |

---

## 5. EB 연동

eb-service 배치가 호출:

```json
POST /ep/online
{
  "header": { "serviceId": "EP.UserEvent.receive", ... },
  "body": { "eventId": "EVT001", "eventType": "USER_CREATED", "userId": "U001" }
}
```

상세: [eb-service-개발가이드.md](./eb-service-개발가이드.md)

---

## 6. 프로젝트 구조

```
com.nh.nsight.marketing.ep
├── application/service/   EpSampleService, EpUserEventService
├── entry/handler/         EpSampleHandler, EpUserEventHandler
└── persistence/           EpSampleDao, EpUserEventDao
```

---

## 7. 의존성

`tcf-util`, `tcf-core`, `tcf-web`

---

## 8. 참고

[ep-service/README.md](../ep-service/README.md) · [eb-service-개발가이드.md](./eb-service-개발가이드.md)
