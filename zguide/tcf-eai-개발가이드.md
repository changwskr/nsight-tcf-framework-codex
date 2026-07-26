# tcf-eai 개발자 가이드

> **역할:** 업무 WAR 간 **HTTP/JSON + serviceId** 연동 (EAI)  
> **유형:** 라이브러리 JAR · **README 없음** → 본 가이드 + zdoc 기준

---

## 1. 왜 필요한가

| ❌ 금지 | ✅ 권장 |
|---------|---------|
| ic-service가 sv-service Java 클래스 import | `TcfServiceClient.call(...)` HTTP 호출 |
| WAR 간 직접 Bean 참조 | `POST /{code}/online` 표준 전문 |

---

## 2. 의존성

```gradle
implementation project(':tcf-eai')
```

사용 모듈 예: **ic-service**, **sv-service**

---

## 3. 설정 (application.yml)

```yaml
nsight:
  integration:
    default-timeout-ms: 3000
    services:
      SV:
        base-url: http://127.0.0.1:8086
        context-path: /sv
        online-path: /online
      IC:
        base-url: http://127.0.0.1:8082
        context-path: /ic
        online-path: /online
```

Gateway 경유 시 base-url을 Gateway(8100)로 변경 가능.

---

## 4. 호출 코드

```java
@Service
public class SvIntegrationDemoService {
    private final TcfServiceClient client;

    public Map<String, Object> callIcSample(Map<String, Object> body, TransactionContext ctx) {
        return client.callForBody("IC", "IC.Sample.inquiry", "IC-INQ-0001", body, ctx);
    }
}
```

- **Header 전파:** GUID, TraceId, user, channel — `HeaderPropagationHelper`  
- **serviceName** blank 시 serviceId로 default — `StandardRequestBuilder`

---

## 5. 패키지

```
com.nh.nsight.tcf.eai/
├── client/      TcfServiceClient, DefaultTcfServiceClient
├── config/      TcfIntegrationConfiguration, TcfIntegrationProperties
├── model/       IntegrationCallRequest, IntegrationCallResult
├── support/     StandardRequestBuilder, ResponseResultValidator
└── exception/   IntegrationException, IntegrationTimeoutException
```

호출 측 **Adapter/Client** 클래스는 **호출하는 WAR**에 둡니다 (예: `SvIntegrationFacade`).

---

## 6. 데모 시나리오

| 방향 | serviceId |
|------|-----------|
| SV → IC | `SV.Integration.icSample` → `IC.Sample.inquiry` |
| IC → SV | ic-service 측 tcf-eai 설정 |

샘플 JSON: `tcf-ui/.../sample-requests/sv-integration-icsample.json`

---

## 7. 오류

| | |
|---|---|
| 연동 Timeout | `IntegrationTimeoutException` |
| 상대 WAR BusinessException | `IntegrationBusinessException` |
| 표준 오류코드 | `E-TCF-IF-*`, `E-TCF-MSG-*` |

---

## 8. 참고

| | |
|---|---|
| [zdoc/서비스간연동.md](../zdoc/서비스간연동.md) | **주 가이드** |
| [zman/04-모듈구성.md](../zman/04-모듈구성.md) | |
| [ic-service-개발가이드.md](./ic-service-개발가이드.md) | |
| [sv-service-개발가이드.md](./sv-service-개발가이드.md) | |
