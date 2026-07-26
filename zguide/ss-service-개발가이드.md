# ss-service 개발자 가이드

> **Self Service (SS)** · 포트 **8093** · Context **`/ss`** · 업무코드 **`SS`**

---

## 1. 이 모듈이 하는 일

셀프서비스(SS) 도메인 온라인 거래 WAR.

---

## 2. 5분 빠른 시작

```bash
gradle :ss-service:bootRun

curl -X POST http://127.0.0.1:8093/ss/online ^
  -H "Content-Type: application/json" ^
  -d @tcf-ui/src/main/resources/sample-requests/ss-sample-inquiry.json
```

UI: `http://localhost:8099/ss/index.html`

---

## 3. 실행

| | |
|---|---|
| bootRun | `gradle :ss-service:bootRun` |
| 스크립트 | `tcf-scripts/run-local.bat ss` |
| ztomcat | `http://localhost:8080/ss/online` |

**메인:** `com.nh.nsight.marketing.ss.NsightSsServiceApplication`

---

## 4. Handler · serviceId

| Handler | serviceId |
|---------|-----------|
| `SsSampleHandler` | `SS.Sample.inquiry` |

---

## 5. 의존성

`tcf-util`, `tcf-core`, `tcf-web`

---

## 6. 신규 거래 추가

[sv-service-개발가이드.md §8](./sv-service-개발가이드.md) 참고. prefix **`SS.`**

---

## 7. 참고

[ss-service/README.md](../ss-service/README.md) · [zman/08-업무Handler개발.md](../zman/08-업무Handler개발.md)
