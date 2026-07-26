# mg-service 개발자 가이드

> **Marketing Gateway (MG)** · 포트 **8096** · Context **`/mg`** · 업무코드 **`MG`**

---

## 1. 이 모듈이 하는 일

마케팅 게이트웨이(MG) 도메인 온라인 거래 WAR.

---

## 2. 5분 빠른 시작

```bash
gradle :mg-service:bootRun

curl -X POST http://127.0.0.1:8096/mg/online ^
  -H "Content-Type: application/json" ^
  -d @tcf-ui/src/main/resources/sample-requests/mg-sample-inquiry.json
```

UI: `http://localhost:8099/mg/index.html`

---

## 3. 실행

| | |
|---|---|
| bootRun | `gradle :mg-service:bootRun` |
| 스크립트 | `tcf-scripts/run-local.bat mg` |
| ztomcat | `http://localhost:8080/mg/online` |

**메인:** `com.nh.nsight.marketing.mg.NsightMgServiceApplication`

---

## 4. Handler · serviceId

| Handler | serviceId |
|---------|-----------|
| `MgSampleHandler` | `MG.Sample.inquiry` |

---

## 5. 의존성

`tcf-util`, `tcf-core`, `tcf-web`

---

## 6. 신규 거래 추가

[sv-service-개발가이드.md §8](./sv-service-개발가이드.md) 참고. prefix **`MG.`**

---

## 7. 참고

[mg-service/README.md](../mg-service/README.md) · [zman/08-업무Handler개발.md](../zman/08-업무Handler개발.md)
