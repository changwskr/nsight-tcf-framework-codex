# ic-service 개발자 가이드

> **Individual Customer (IC)** · 포트 **8082** · Context **`/ic`** · 업무코드 **`IC`**

---

## 1. 이 모듈이 하는 일

개인고객(IC) 도메인 온라인 거래. SV 등 다른 WAR에서 **tcf-eai**로 호출되는 연동 대상이기도 합니다.

---

## 2. 5분 빠른 시작

```bash
gradle :ic-service:bootRun

curl -X POST http://127.0.0.1:8082/ic/online ^
  -H "Content-Type: application/json" ^
  -d @tcf-ui/src/main/resources/sample-requests/ic-sample-inquiry.json
```

UI: `http://localhost:8099/ic/index.html` (tcf-ui 필요)

---

## 3. 실행

| | |
|---|---|
| bootRun | `gradle :ic-service:bootRun` |
| 스크립트 | `tcf-scripts/run-local.bat ic` |
| ztomcat | `http://localhost:8080/ic/online` |

**메인:** `com.nh.nsight.marketing.ic.NsightIcServiceApplication`

---

## 4. Handler · serviceId

| Handler | serviceId |
|---------|-----------|
| `IcSampleHandler` | `IC.Sample.inquiry` |
| `IcCustomerHandler` | `IC.Customer.inquiry` |

패키지: `com.nh.nsight.marketing.ic`

---

## 5. 의존성

```gradle
implementation project(':tcf-util')
implementation project(':tcf-core')
implementation project(':tcf-web')
implementation project(':tcf-eai')   // SV→IC 등 연동
```

---

## 6. 연동 (호출받기 / 호출하기)

- **SV → IC:** `SV.Integration.icSample` (sv-service)  
- **IC → SV:** tcf-eai + `nsight.integration.services.SV` 설정  
- 가이드: [tcf-eai-개발가이드.md](./tcf-eai-개발가이드.md)

---

## 7. 신규 거래 추가

[sv-service-개발가이드.md §8](./sv-service-개발가이드.md)과 동일 패턴.  
serviceId prefix는 반드시 **`IC.`**

---

## 8. 참고

| | |
|---|---|
| [ic-service/README.md](../ic-service/README.md) | |
| [zdoc/서비스간연동.md](../zdoc/서비스간연동.md) | EAI |
| [zman/08-업무Handler개발.md](../zman/08-업무Handler개발.md) | |
