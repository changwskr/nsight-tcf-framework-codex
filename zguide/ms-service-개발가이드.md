# ms-service 개발자 가이드

> **Marketing Strategy (MS)** · 포트 **8085** · Context **`/ms`** · 업무코드 **`MS`**

---

## 1. 이 모듈이 하는 일

마케팅 전략(MS) 도메인 온라인 거래 WAR.

---

## 2. 5분 빠른 시작

```bash
gradle :ms-service:bootRun

curl -X POST http://127.0.0.1:8085/ms/online ^
  -H "Content-Type: application/json" ^
  -d @tcf-ui/src/main/resources/sample-requests/ms-sample-inquiry.json
```

UI: `http://localhost:8099/ms/index.html`

---

## 3. 실행

| | |
|---|---|
| bootRun | `gradle :ms-service:bootRun` |
| 스크립트 | `tcf-scripts/run-local.bat ms` |
| ztomcat | `http://localhost:8080/ms/online` |

**메인:** `com.nh.nsight.marketing.ms.NsightMsServiceApplication`

---

## 4. Handler · serviceId

| Handler | serviceId |
|---------|-----------|
| `MsSampleHandler` | `MS.Sample.inquiry` |

---

## 5. 의존성

`tcf-util`, `tcf-core`, `tcf-web`

---

## 6. 신규 거래 추가

[sv-service-개발가이드.md §8](./sv-service-개발가이드.md) 참고. prefix **`MS.`**

---

## 7. 참고

[ms-service/README.md](../ms-service/README.md) · [zman/08-업무Handler개발.md](../zman/08-업무Handler개발.md)
