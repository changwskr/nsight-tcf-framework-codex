# 7장. ServiceId Dispatcher 구조 — 설명

## 설계서 절 목차

7.1~7.2 개요 · 7.3 위치 · 7.4 URL vs serviceId · 7.5 명명 · 7.6 Catalog · 7.7 Registry · 7.8~7.12 처리흐름·원칙 · 7.13~7.27 오류·Cache·운영

---

## 핵심 결론

> **Dispatcher는 serviceId로 Handler를 실행한다.** URL은 WAR 입구만 결정.

---

## URL vs serviceId (7.4)

동일 업무 WAR당 `POST /{code}/online` 하나, **serviceId**로 Handler 분기:

| URL | serviceId | Handler |
|-----|-----------|---------|
| `POST /sv/online` | `SV.Customer.selectSummary` | SvCustomerHandler |
| `POST /sv/online` | `SV.Sample.inquiry` | SvSampleHandler |
| `POST /ic/online` | `IC.Customer.inquiry` | IcCustomerHandler |

## Dispatcher 위치 (7.3)

```
STF (검증·통제·PROCESSING)
  → TransactionDispatcher
  → Handler → ETF
```

Dispatcher는 세션·권한·응답 **미처리**.

## serviceId 명명 (7.5)

```
{업무코드}.{도메인}.{행위}
```

- 도메인 = application Service = **Handler 1개** (`SvCustomerHandler`)
- Catalog·Mapper·SQL ID와 **동일 접두** 유지

## Catalog vs Registry (7.6~7.7)

| | OM_SERVICE_CATALOG | Spring Registry |
|---|-------------------|-----------------|
| 질문 | 거래가 **등록**됐는가 | **어느 Bean**이 실행하는가 |
| 시점 | STF 조회·Cache | **기동 시** serviceIds() 등록 |
| 관리 | OM.ServiceCatalog.* | `@Component` Handler |

```
Startup → @Component TransactionHandler beans
→ serviceIds() flatten → Map<serviceId, Handler>
→ duplicate → IllegalStateException (기동 실패)
```

## Dispatcher 12단계

1. StandardRequest 수신  
2. serviceId 추출  
3. 필수 확인  
4. businessCode ↔ Prefix  
5. Catalog (Cache)  
6. 활성 상태  
7. 거래통제 (STF)  
8. Registry lookup  
9. Handler 선택  
10. handle(request, context)  
11. 결과 반환  
12. ETF → StandardResponse  

## TransactionHandler API (코드)

```java
default String serviceId() { return null; }
default Collection<String> serviceIds() { ... wrap serviceId(); }
Object doHandle(StandardRequest<Map<String, Object>> request, TransactionContext context);
```

## 미등록·중복

- 미등록 serviceId → `ErrorCode.SERVICE_NOT_FOUND`
- 중복 등록 → `Duplicate serviceId detected: ...` (IcCustomerInquiryHandler 잔존 시)

## 코드베이스

- `tcf-core/.../TransactionDispatcher.java`
- `tcf-core/.../TransactionHandler.java`
- Seed: `tcf-om/.../data.sql` — HANDLER_CLASS

## 관련 문서

- [00-대조표](./00-설계서-코드베이스-대조표.md) — Handler 패턴
- `docs/architecture/06-naming.md`

## 이전 · 다음

← [6장](./06-표준전문구조.md) · [8장](./08-업무Handler개발.md) →
