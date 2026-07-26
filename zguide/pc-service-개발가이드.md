# pc-service 개발자 가이드

> **Private Customer (PC)** · 포트 **8083** · Context **`/pc`** · 업무코드 **`PC`**

---

## 1. 이 모듈이 하는 일

개인고객(PC) 도메인 온라인 거래 WAR. 표준 TCF 6계층 패턴을 따릅니다.

---

## 2. 5분 빠른 시작

```bash
gradle :pc-service:bootRun

curl -X POST http://127.0.0.1:8083/pc/online ^
  -H "Content-Type: application/json" ^
  -d @tcf-ui/src/main/resources/sample-requests/pc-sample-inquiry.json
```

UI: `http://localhost:8099/pc/index.html`

---

## 3. 실행

| | |
|---|---|
| bootRun | `gradle :pc-service:bootRun` |
| 스크립트 | `tcf-scripts/run-local.bat pc` |
| ztomcat | `http://localhost:8080/pc/online` |

**메인:** `com.nh.nsight.marketing.pc.NsightPcServiceApplication`

---

## 4. Handler · serviceId

| Handler | serviceId |
|---------|-----------|
| `PcSampleHandler` | `PC.Sample.inquiry` |

패키지: `com.nh.nsight.marketing.pc`

---

## 5. 의존성

`tcf-util`, `tcf-core`, `tcf-web`

---

## 6. 신규 거래 추가

[sv-service-개발가이드.md §8](./sv-service-개발가이드.md)과 동일. serviceId prefix **`PC.`**

---

## 7. 참고

| | |
|---|---|
| [pc-service/README.md](../pc-service/README.md) | |
| [zman/08-업무Handler개발.md](../zman/08-업무Handler개발.md) | Handler 규칙 |
| [zman/22-업무서비스샘플.md](../zman/22-업무서비스샘플.md) | 샘플 설계 |
