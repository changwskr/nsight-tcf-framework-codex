# tcf-core 개발자 가이드

> **역할:** NSIGHT TCF **엔진 코어** — STF/TCF/ETF, Dispatcher, 표준 전문, 거래통제·Timeout·로그  
> **유형:** 라이브러리 JAR (단독 bootRun 없음)

---

## 1. 이 모듈이 하는 일

| 담당 | 비담당 |
|------|--------|
| `TCF.process()` 파이프라인 | HTTP 수신 (→ tcf-web) |
| STF 전처리 / ETF 후처리 | 업무 로직 (→ 업무 WAR) |
| `TransactionDispatcher` | DB SQL (→ 업무 DAO) |
| `TransactionHandler` 인터페이스 | |

**한 줄:** 모든 온라인 거래가 지나는 **공통 실행 엔진**.

---

## 2. 빠른 이해 — 처리 순서

```
StandardRequest
  → STF.preProcess()     Header, GUID, 세션, 권한, 거래통제, Timeout, PROCESSING 로그
  → OnlineTransactionTimeoutExecutor
  → TransactionDispatcher  (serviceId → Handler)
  → Handler.doHandle()     [업무 WAR]
  → ETF.postProcess()      StandardResponse, 로그 종료
```

---

## 3. 패키지·핵심 클래스

```
com.nh.nsight.tcf.core/
├── processor/     TCF, STF, ETF
├── dispatch/      TransactionDispatcher
├── transaction/   TransactionHandler
├── message/       StandardRequest, StandardResponse, StandardHeader, Result
├── context/       TransactionContext, TransactionContextHolder
├── control/       TransactionControlService
├── timeout/       OnlineTransactionTimeoutExecutor, TimeoutPolicyService
├── validation/    StandardHeaderValidator
├── security/      SessionValidator, AuthorizationValidator
├── logging/       TransactionLogService, AuditLogService
└── error/         BusinessException, ErrorCode
```

### TransactionHandler (업무가 구현)

```java
@Component
public class XxxHandler implements TransactionHandler {
    @Override
    public Collection<String> serviceIds() { return List.of("XX.Domain.action"); }
    @Override
    public Object doHandle(StandardRequest<Map<String, Object>> req, TransactionContext ctx) {
        return switch (ctx.getHeader().getServiceId()) { ... };
    }
}
```

---

## 4. 의존성

```gradle
implementation project(':tcf-core')
// HTTP 진입 필요 시
implementation project(':tcf-web')
```

- `tcf-util` ← `tcf-core` ← `tcf-web` ← 업무 WAR

---

## 5. 빌드

```bash
gradle :tcf-core:build
```

업무 WAR bootRun 시 tcf-core JAR가 transitively 포함됩니다.

---

## 6. 개발 시 자주 보는 코드

| 작업 | 클래스 |
|------|--------|
| Header 검증 규칙 | `StandardHeaderValidator` |
| serviceId 라우팅 | `TransactionDispatcher` |
| 업무 오류 | `BusinessException`, `ErrorCode` |
| Timeout | `OnlineTransactionTimeoutExecutor` |

---

## 7. 참고 문서

| 문서 | 내용 |
|------|------|
| [tcf-core/README.md](../tcf-core/README.md) | 모듈 개요 |
| [docs/TCF_FRAMEWORK_GUIDE.md](../docs/TCF_FRAMEWORK_GUIDE.md) | TCF 가이드 |
| [zman/05-TCF처리구조.md](../zman/05-TCF처리구조.md) | 처리 구조 |
| [zman/07-ServiceIdDispatcher.md](../zman/07-ServiceIdDispatcher.md) | Dispatcher |
| [docs/architecture/33-TCF.md](../docs/architecture/33-TCF.md) | 아키텍처 |

## 8. 다음 단계

- 업무 개발 → 담당 `*-service-개발가이드.md`
- HTTP·거래로그 DB → [tcf-web README](../tcf-web/README.md) (가이드는 core+web 함께 사용)
