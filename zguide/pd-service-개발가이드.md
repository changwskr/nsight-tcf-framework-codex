# pd-service 개발자 가이드

> **Product (PD)** · 포트 **8087** · Context **`/pd`** · 업무코드 **`PD`**

---

## 1. 이 모듈이 하는 일

상품(PD) 도메인 온라인 거래 WAR.

---

## 2. 5분 빠른 시작

```bash
gradle :pd-service:bootRun

curl -X POST http://127.0.0.1:8087/pd/online ^
  -H "Content-Type: application/json" ^
  -d @tcf-ui/src/main/resources/sample-requests/pd-sample-inquiry.json
```

UI: `http://localhost:8099/pd/index.html`

---

## 3. 실행

| | |
|---|---|
| bootRun | `gradle :pd-service:bootRun` |
| 스크립트 | `tcf-scripts/run-local.bat pd` |
| ztomcat | `http://localhost:8080/pd/online` |

**메인:** `com.nh.nsight.marketing.pd.NsightPdServiceApplication`

---

## 4. Handler · serviceId

| Handler | serviceId |
|---------|-----------|
| `PdSampleHandler` | `PD.Sample.inquiry` |

---

## 5. 의존성

`tcf-util`, `tcf-core`, `tcf-web`

---

## 6. 신규 거래 추가

[sv-service-개발가이드.md §8](./sv-service-개발가이드.md) 참고. prefix **`PD.`**

---

## 7. 참고

[pd-service/README.md](../pd-service/README.md) · [zman/08-업무Handler개발.md](../zman/08-업무Handler개발.md)
