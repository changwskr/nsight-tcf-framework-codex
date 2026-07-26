# 8장. 업무 Handler 개발 구조 — 설명

## 설계서 절 목차

8.1~8.5 Handler역할 · 8.6 패키지 · 8.7~8.12 계층 · 8.13~8.18 규칙 · 8.19 신규거래 · 8.20 금지 · 8.21 마무리

---

## 핵심 결론

> Handler = **TCF↔업무 Adapter**. Controller·STF·ETF **업무 개발자 미구현**.

---

## Handler vs 하지 않을 것

| ✅ | ❌ |
|----|-----|
| serviceId 진입, Body→Map/DTO | Header·세션·거래통제 |
| Facade 호출 | SQL, StandardResponse |

## 처리 흐름 (8.4)

Dispatcher → handle → doHandle → Facade → Service → Rule → DAO → ETF

## 패키지 (8.6)

```
entry/handler/   ← {Business}{Domain}Handler
entry/facade/
application/service/, rule/
persistence/dao/, mapper/
client/          ← tcf-eai
support/
```

## 도메인 Handler (코드 최신)

docx: `SvCustomerSummaryHandler` → 코드: **`SvCustomerHandler`**

```java
@Override
public Collection<String> serviceIds() {
    return List.of("SV.Customer.selectSummary");
}
@Override
public Object doHandle(...) {
    return switch (context.getHeader().getServiceId()) {
        case SELECT_SUMMARY -> facade.selectCustomerSummary(...);
        default -> throw new BusinessException(SERVICE_NOT_FOUND, ...);
    };
}
```

## 계층 (8.7)

Handler(분기) → Facade(@Transactional) → Service → Rule → DAO(SQL)

## 신규 거래 (8.19)

1. Service/Facade  
2. Handler serviceIds + switch  
3. data.sql Catalog + TC + Timeout  
4. tcf-ui/EAI 샘플 JSON  

## 금지 (8.20)

- Handler→DAO 직접  
- WAR 간 Java 참조 → **tcf-eai**  
- 업무 Controller for /online  

## 업무 WAR Handler 현황 (예)

| WAR | Handler 예 |
|-----|------------|
| sv | SvCustomerHandler, SvSampleHandler, SvIntegrationHandler |
| ic | IcCustomerHandler, IcSampleHandler |
| tcf-om | Om*Handler ×24 |

## 관련 문서

- [00-대조표](./00-설계서-코드베이스-대조표.md) · [22-샘플](./22-업무서비스샘플.md)  
- `docs/architecture/01-application-layer.md`

## 이전 · 다음

← [7장](./07-ServiceIdDispatcher.md) · [9장 Gateway](./09-Gateway라우팅.md) →
