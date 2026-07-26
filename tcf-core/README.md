# tcf-core — TCF 엔진 코어

표준 HTTP/JSON 전문 모델, 거래 컨텍스트, STF/TCF/ETF 처리기, `TransactionHandler` 디스패처를 제공하는 프레임워크 핵심 모듈입니다.

| 항목 | 값 |
|------|-----|
| Gradle 모듈 | `tcf-core` |
| 패키지 | `com.nh.nsight.tcf.core` |
| 산출물 | JAR (라이브러리) |

## 처리 흐름

```text
TCF.process(request)
  ├─ STF.preProcess()        Header 검증, GUID/TraceId, 세션·권한, 멱등성
  │                          거래통제(7필드) → Timeout 정책 → TimeoutContextHolder
  ├─ OnlineTransactionTimeoutExecutor
  │     └─ TransactionDispatcher   serviceId → TransactionHandler
  └─ ETF.success/fail/error  표준 응답, 감사·메트릭
```

## 주요 패키지

최상위는 `application`, `client`, `config`, `entry`, `persistence`, `support` 만 둡니다.
프레임워크 엔진·전문·거래 파이프라인은 `support` 하위에 위치합니다.

| 패키지 | 설명 |
|--------|------|
| `support.message` | `StandardRequest`, `StandardResponse`, `StandardHeader`, `Result`, `TcfStandardMessageCatalog` |
| `support.processor` | `TCF`, `STF`, `ETF` |
| `support.dispatch` | `TransactionDispatcher` |
| `support.transaction` | `TransactionHandler` 인터페이스 |
| `support.context` | `TransactionContext`, `TransactionContextHolder` |
| `support.control` | `TransactionControlService` — Header 7필드 거래통제 |
| `support.timeout` | `TimeoutPolicyService`, `OnlineTransactionTimeoutExecutor`, `TimeoutExceptionResolver` |
| `support.validation` | `StandardHeaderValidator` |
| `support.security` | `SessionValidator`, `AuthorizationValidator`, `AuthenticationContextValidator` |
| `support.idempotency` | `IdempotencyChecker` |
| `support.logging` | `TransactionLogService`, `AuditLogService` |
| `support.error` | `BusinessException`, `ErrorCode` |
| `support` (루트) | `TcfConsoleLog` (UTF-8 콘솔 로그) |

## Handler 구현 규약

업무 WAR에서는 Handler를 **`entry.handler`** 패키지에 둡니다 (6계층 규약).

핸들러는 **도메인(Service)당 1개**를 원칙으로 하며, `serviceIds()`로 담당 거래 목록을 선언하고
`doHandle` 내부에서 `context.getHeader().getServiceId()` 기준으로 분기합니다.
새 거래 추가 시 상수 + `case` 한 줄씩만 확장하면 됩니다.

```java
@Component
public class SvSampleHandler implements TransactionHandler {

    private static final String INQUIRY = "SV.Sample.inquiry";

    private final SvSampleFacade facade;

    public SvSampleHandler(SvSampleFacade facade) {
        this.facade = facade;
    }

    @Override
    public Collection<String> serviceIds() {
        return List.of(INQUIRY);
    }

    @Override
    public Object doHandle(StandardRequest<Map<String, Object>> request, TransactionContext context) {
        String serviceId = context.getHeader().getServiceId();
        return switch (serviceId) {
            case INQUIRY -> facade.inquiry(request.getBody(), context);
            default -> throw new BusinessException(ErrorCode.SERVICE_NOT_FOUND,
                    "SvSampleHandler 미지원 serviceId: " + serviceId);
        };
    }
}
```

> 하위호환: 단일 거래만 처리하는 경우 `serviceId()` 하나만 재정의해도 됩니다
> (`serviceIds()` 기본 구현이 `serviceId()` 단일값을 감쌉니다).

## 먼저 볼 소스

1. `support/processor/TCF.java`
2. `support/processor/STF.java`
3. `support/timeout/OnlineTransactionTimeoutExecutor.java`
4. `support/control/TransactionControlService.java`
5. `support/message/StandardRequest.java`
6. `support/dispatch/TransactionDispatcher.java`

## 빌드

```bash
gradle :tcf-core:build
```
