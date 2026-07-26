# 제10장. Handler 만드는 법

| 항목 | 내용 |
| --- | --- |
| **편** | 제3편 |
| **상태** | 집필 완료 |
| **원본** | [ztcfbook 제10장](../ztcfbook/제03편/10-TransactionHandler-개발.md) |

---

## 그림으로 보기

```mermaid
flowchart TB
  COMP[@Component Handler] --> IDS[serviceIds]
  IDS --> SW[switch serviceId]
  SW --> FAC[Facade]
```

---

## 10.1 Handler가 하는 일 — **한 가지**

**serviceId에 맞는 Facade를 호출**합니다. 그게 전부입니다.

```java
@Component
@RequiredArgsConstructor
public class SvCustomerHandler implements TransactionHandler {

  private final SvCustomerFacade facade;

  @Override
  public List<String> serviceIds() {
    return List.of("SV.Customer.selectSummary");
  }

  @Override
  public Object doHandle(StandardRequest request, TransactionContext context) {
    // body → DTO 또는 Map 변환 후
    return facade.selectSummary(request, context);
  }
}
```

| ✅ Handler | ❌ Handler |
| --- | --- |
| Facade 호출 | SQL, for문 업무 |
| serviceIds() 등록 | @GetMapping URL |
| 업무 결과 return | ResponseEntity 직접 |

---

## 10.2 여러 serviceId — switch 패턴

팀 표준은 **도메인당 Handler 1개** + `switch` 입니다.

```java
@Override
public List<String> serviceIds() {
  return List.of(
      "SV.Customer.selectSummary",
      "SV.Customer.selectDetail");
}

@Override
public Object doHandle(StandardRequest req, TransactionContext ctx) {
  return switch (ctx.getHeader().getServiceId()) {
    case "SV.Customer.selectSummary" -> facade.selectSummary(req, ctx);
    case "SV.Customer.selectDetail"   -> facade.selectDetail(req, ctx);
    default -> throw new BusinessException("E-TCF-DSP-0001", "...");
  };
}
```

---

## 10.3 호출 주소

| 환경 | URL |
| --- | --- |
| sv-service bootRun | `POST http://localhost:8086/sv/online` |
| ztomcat | `POST http://localhost:8080/sv/online` |

Content-Type: `application/json`

---

## 10.4 등록 확인

1. `@Component` 붙었는가  
2. `serviceIds()` 가 Catalog **SERVICE_ID** 와 같은가  
3. **같은 serviceId** 가 다른 Handler에 없는가 (서버 기동 시 확인)

---

## 10.5 ⚠️ 초보자 실수

| 실수 | |
| --- | --- |
| `@RestController` 추가 | TCF **우회** |
| Handler에 `@Transactional` | **Facade**에 둠 |
| serviceId 오타 | Dispatcher **못 찾음** |

---

## 요약

- Handler = **`serviceIds()` + `doHandle` → Facade**
- URL Controller **대신** 이 인터페이스 구현
- 22장에서 **SV 실습**

---

## 이전 · 다음

| | |
| --- | --- |
| ← 이전 | [9장 JSON](./09-JSON-요청-응답-만들기.md) |
| → 다음 | [11장 로그·Timeout](./11-로그-Timeout-실수-방지.md) |

---

## 📘 원본에서 더 보기

- [ztcfbook/제03편/10-TransactionHandler-개발.md](../ztcfbook/제03편/10-TransactionHandler-개발.md)
