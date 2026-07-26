# sv-service 개발자 가이드

> **Single View (SV)** · 포트 **8086** · Context **`/sv`** · 업무코드 **`SV`**

---

## 1. 이 모듈이 하는 일

고객 **Single View** 조회 등 SV 도메인 온라인 거래를 처리합니다.  
표준 TCF 패턴: **Handler → Facade → Service → Rule → DAO**.

---

## 2. 5분 빠른 시작

```bash
# 1) SV 기동
gradle :sv-service:bootRun

# 2) 샘플 호출 (다른 터미널)
curl -X POST http://127.0.0.1:8086/sv/online ^
  -H "Content-Type: application/json" ^
  -d @tcf-ui/src/main/resources/sample-requests/sv-sample-inquiry.json

# 3) UI (tcf-ui 기동 후)
# http://localhost:8099/sv/index.html
# http://localhost:8099/sv/sample-list.html  ← 페이징 샘플
```

---

## 3. 실행 방법

| 방법 | 명령 |
|------|------|
| Gradle | `gradle :sv-service:bootRun` |
| 스크립트 | `tcf-scripts/run-local.bat sv` |
| ztomcat | `ztomcat/deploy-wars.bat sv` → `http://localhost:8080/sv/online` |
| WAR 빌드 | `gradle :sv-service:bootWar` |

**메인 클래스:** `com.nh.nsight.marketing.sv.NsightSvServiceApplication`

---

## 4. 프로젝트 구조

```
sv-service/src/main/java/com/nh/nsight/marketing/sv/
├── entry/handler/     ← TransactionHandler (도메인당 1개)
├── entry/facade/
├── application/service/, rule/
├── persistence/dao/, mapper/
├── config/
└── support/
```

---

## 5. Handler · serviceId (현재)

| Handler | serviceId | Facade |
|---------|-----------|--------|
| `SvSampleHandler` | `SV.Sample.inquiry` | SvSampleFacade |
| `SvCustomerHandler` | `SV.Customer.selectSummary` | SvCustomerFacade |
| `SvIntegrationHandler` | `SV.Integration.icSample` | SvIntegrationFacade |

> 신규 거래: **같은 도메인**이면 Handler에 `serviceIds()` + `switch` case 추가.

---

## 6. 의존성 (build.gradle)

```gradle
implementation project(':tcf-util')
implementation project(':tcf-core')
implementation project(':tcf-web')
implementation project(':tcf-eai')   // IC 등 타 WAR 호출
```

---

## 7. 서비스 간 연동 (tcf-eai)

SV → IC 데모: `SvIntegrationHandler` / `SvIntegrationDemoService`

```java
tcfServiceClient.callForBody("IC", "IC.Sample.inquiry", txCode, body, context);
```

설정: `application.yml` → `nsight.integration.services.IC.base-url`

가이드: [tcf-eai-개발가이드.md](./tcf-eai-개발가이드.md) · [zdoc/서비스간연동.md](../zdoc/서비스간연동.md)

---

## 8. 신규 거래 추가 체크리스트

1. `application/service` — Service 메서드  
2. `entry/facade` — `@Transactional` Facade  
3. `entry/handler` — `serviceIds()` + switch  
4. `persistence` — DAO/Mapper (필요 시)  
5. **tcf-om** — `OM_SERVICE_CATALOG`, 거래통제, Timeout seed  
6. `tcf-ui/.../sample-requests/sv-*.json` — 테스트 JSON  

---

## 9. 테스트

| 유형 | 방법 |
|------|------|
| curl | `sample-requests/sv-*.json` |
| tcf-ui | `/sv/index.html`, `/sv/sample-list.html` |
| JUnit | `gradle :sv-service:test` |

---

## 10. 참고 문서

| 문서 | |
|------|--|
| [sv-service/README.md](../sv-service/README.md) | |
| [zdoc/SV고객요약샘플.md](../zdoc/SV고객요약샘플.md) | 고객 요약 |
| [zdoc/sv-service-페이징-가이드.md](../zdoc/sv-service-페이징-가이드.md) | 페이징 |
| [zman/22-업무서비스샘플.md](../zman/22-업무서비스샘플.md) | 샘플 설계 |
| [zman/08-업무Handler개발.md](../zman/08-업무Handler개발.md) | Handler 규칙 |

---

## 11. 자주 하는 실수

- `/online`용 Controller 추가 ❌  
- Handler에서 DAO 직접 호출 ❌ → Facade 경유  
- serviceId만 등록하고 **Catalog/거래통제** 누락 ❌  
- IC Java 직접 import ❌ → **tcf-eai**
