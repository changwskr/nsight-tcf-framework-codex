# NSIGHT TCF Framework Source Index

## 1. TCF 공통 모듈

| 영역 | 위치 |
|---|---|
| Util | `tcf-util/src/main/java/com/nh/nsight/tcf/util` |
| 표준 전문 | `tcf-core/src/main/java/com/nh/nsight/tcf/core/message` |
| 거래 Context | `tcf-core/src/main/java/com/nh/nsight/tcf/core/context` |
| 오류/예외 | `tcf-core/src/main/java/com/nh/nsight/tcf/core/error` |
| TCF | `tcf-core/src/main/java/com/nh/nsight/tcf/core/processor/TCF.java` |
| STF | `tcf-core/src/main/java/com/nh/nsight/tcf/core/processor/STF.java` |
| ETF | `tcf-core/src/main/java/com/nh/nsight/tcf/core/processor/ETF.java` |
| Dispatcher | `tcf-core/src/main/java/com/nh/nsight/tcf/core/dispatch/TransactionDispatcher.java` |
| Handler Interface | `tcf-core/src/main/java/com/nh/nsight/tcf/core/transaction/TransactionHandler.java` |
| Controller | `tcf-web/src/main/java/com/nh/nsight/tcf/web/controller/OnlineTransactionController.java` |

## 2. 업무 샘플

| 업무 | 대표 Handler |
|---|---|
| SV | `sv-service/src/main/java/com/nh/nsight/marketing/sv/entry/handler/SvSampleHandler.java` |
| IC | `ic-service/src/main/java/com/nh/nsight/marketing/ic/entry/handler/IcSampleHandler.java` |
| MG | `mg-service/src/main/java/com/nh/nsight/marketing/mg/entry/handler/MgSampleHandler.java` |
| OM | `tcf-om/src/main/java/com/nh/nsight/marketing/om/entry/handler/OmSampleHandler.java` |

## 3. 실행 흐름

```text
OnlineTransactionController → TCF → STF → TransactionDispatcher → Handler
  → Facade → Service → Rule / DAO → Mapper
  → ETF → StandardResponse
```

상세 소스 가이드: [architecture/29-facade.md](architecture/29-facade.md)  
Spring Boot 기동: [architecture/30-springboot.md](architecture/30-springboot.md)  
AutoConfiguration: [architecture/31-autoconfiguration.md](architecture/31-autoconfiguration.md)  
AOP: [architecture/32-AOP.md](architecture/32-AOP.md)  
TCF 엔진: [architecture/33-TCF.md](architecture/33-TCF.md)  
STF: [architecture/34-STF.md](architecture/34-STF.md)  
BTF: [architecture/35-BTF.md](architecture/35-BTF.md)
