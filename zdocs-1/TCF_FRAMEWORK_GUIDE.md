# NSIGHT TCF Framework Guide

## 1. TCF의 역할

TCF(Transaction Control Framework)는 NSIGHT 온라인 거래의 메인 실행 엔진입니다.
업무 개발자는 Controller를 직접 만들기보다 `TransactionHandler`를 구현하고 `serviceId`를 등록합니다.

## 2. 처리 순서

```text
OnlineTransactionController
  → TCF
    → STF
      - Header 검증
      - GUID / TraceId 생성
      - Session / Auth 검증
      - Idempotency 검증
      - 거래 시작 로그
    → TransactionDispatcher
      - serviceId 기준 Handler 탐색
    → Handler
      - Facade / Service / Rule / DAO / Mapper 호출
    → ETF
      - 표준 응답 조립
      - 오류코드 매핑
      - 거래 종료 로그
      - 감사 로그
      - 성능 메트릭
```

## 3. Handler 개발 규칙

핸들러는 **도메인(application Service)당 1개**를 원칙으로 하며, `serviceIds()`로 담당 거래를 선언하고 `doHandle`에서 `serviceId`로 분기한다.

```java
@Component
public class SvSampleHandler implements TransactionHandler {
    private static final String INQUIRY = "SV.Sample.inquiry";

    @Override
    public Collection<String> serviceIds() {
        return List.of(INQUIRY);
    }

    @Override
    public Object doHandle(StandardRequest<Map<String, Object>> request, TransactionContext context) {
        String serviceId = context.getHeader().getServiceId();
        return switch (serviceId) {
            case INQUIRY -> facade.inquiry(request.getBody(), context);
            default -> throw new BusinessException(ErrorCode.SERVICE_NOT_FOUND, serviceId);
        };
    }
}
```

## 4. 요청 전문 구조

```json
{
  "header": {
    "systemId": "NSIGHT-MP",
    "businessCode": "SV",
    "serviceId": "SV.Sample.inquiry",
    "transactionCode": "SV-INQ-0001",
    "processingType": "INQUIRY",
    "guid": "",
    "traceId": "",
    "channelId": "WEBTOP",
    "userId": "U123456",
    "branchId": "001234",
    "centerId": "DC1",
    "requestTime": "2026-06-15T10:30:00+09:00",
    "clientIp": "10.10.10.10"
  },
  "body": {
    "sampleKey": "A001"
  }
}
```


## 모듈 재구성 기준

- `common-core`는 `tcf-core`로 변경했습니다.
- `common-web`은 `tcf-web`으로 변경했습니다.
- `GuidGenerator`, `MaskingUtils` 등 공통 Util은 `tcf-util`로 분리했습니다.
