설계를 정리했습니다. 먼저 판단 기준을 하나 세우고, 거기서 모든 배치를 도출하는 방식으로 잡았습니다.

## 0. 판단 기준: "무엇을 볼 수 있는가"

Filter와 AOP 중 어디에 둘지는 취향이 아니라, 그 지점에서 **볼 수 있는 정보**로 결정됩니다.

| 지점                    | 볼 수 있는 것                                     | 볼 수 없는 것                            |
| ----------------------- | ------------------------------------------------- | ---------------------------------------- |
| Servlet Filter          | HTTP 바이트, URI, 응답 상태                       | 어느 거래인지, 역직렬화된 DTO, 업무 예외 |
| Security Filter         | 위 + 인증 주체                                    | 위와 동일                                |
| AOP `@Around`           | 타입 있는 인자·반환값, 애노테이션 메타, 업무 예외 | HTTP 원문                                |
| `@RestControllerAdvice` | 컨트롤러 밖으로 나간 예외                         | 필터에서 난 예외                         |

여기서 한 문장이 나옵니다.

> **필터는 "어떤 거래인지 몰라도 해야 하는 일", AOP는 "어떤 거래인지 알아야 할 수 있는 일"**

guid 채번과 MDC는 404가 나든 인증이 깨지든 무조건 필요하니 필터입니다. 헤더 검증, 결과 코드 판정, 업무 예외 처리는 전문의 타입을 알아야 하니 AOP입니다.

## 1. 패키지 구조

```
nhnis/fw/tcf/
├─ TcfTransaction.java                거래 선언 애노테이션 (serviceId, transactionCode, processingType)
├─ aspect/
│   └─ TCFAspect.java      @Around — STF/ETF 오케스트레이션만
├─ stf/
│   ├─ STF.java                       선처리 진입점
│   └─ StandardHeaderValidator.java   헤더 필수값 검증
├─ etf/
│   └─ ETF.java                       후처리 진입점 + 실패 전문 조립
├─ context/
│   ├─ TcfContext.java                거래 컨텍스트 (헤더, 시작시각, 클라이언트 원본헤더)
│   └─ TcfContextHolder.java          ThreadLocal
├─ web/
│   ├─ TcfTraceFilter.java            guid/traceId/MDC — 기존 MdcLoggingFilter 승계
│   └─ JwtAuthenticationFilter.java   Security 체인에 등록
└─ dto/                               (이미 있음)
```

핵심은 **Aspect가 로직을 갖지 않는다**는 것입니다. Aspect는 `STF`와 `ETF`를 순서대로 부르는 배선 담당이고, 실제 로직은 평범한 `@Component`에 있습니다. 그래야 STF/ETF를 AOP 없이 단위 테스트할 수 있고, 나중에 수단을 바꿔도 로직이 안 흔들립니다.

## 2. 요청 흐름

```
HTTP 요청
  │
  ├─① TcfTraceFilter                    [Filter, HIGHEST_PRECEDENCE]
  │     guid/traceId 채번, clientIp 추출, MDC.put
  │     try { chain } finally { MDC.clear() }   ← 심은 곳에서 지운다
  │
  ├─② JwtAuthenticationFilter            [Security 체인]
  │     토큰 검증 → SecurityContext
  │
  ├─③ DispatcherServlet
  │     컨트롤러 매칭 + Jackson 역직렬화 → StandardRequestDto<mpcoa9999DtoIn>
  │
  ├─④ TCFAspect ─ STF 시작 ───┐   [AOP @Around]
  │     STF.preProcess(request, @TcfTransaction)
  │       · 헤더 정규화 + 거래정의(@TcfTransaction) 보완
  │       · 필수값 검증
  │       · SecurityContext에서 userId 보강 → MDC 갱신
  │       · TcfContext 생성, 거래 시작 로그, 시작 시각 기록
  │                                       │
  ├─⑤ Controller → Service → DAO         │
  │                                       │
  ├─⑥ TCFAspect ─ ETF ────────┘
  │     ETF.success / businessFail / systemError
  │       · Result 코드 판정, 응답 헤더 echo
  │       · 소요시간·거래 종료 로그, TcfContext 정리
  │
  └─ Jackson 직렬화 → HTTP 응답
```

제안하셨던 5단계와 비교하면 **ETFFilter가 사라지고 JWT가 Security 체인으로 옮겨간 것**이 차이입니다. ETF는 필터가 아니라 AOP의 후반부입니다.

## 3. 컨트롤러 계약

```java
@TcfTransaction(serviceId = "MP.SalesTip.list",
                transactionCode = "MP-INQ-0001",
                processingType = ProcessingType.INQUIRY)
@PostMapping("/list")
public StandardResponseDto<List<mpcoa9999DtoOut>> selectSalesTipList(
        @RequestBody StandardRequestDto<mpcoa9999DtoIn> request) {
    return StandardResponseDto.of(service.selectSalesTipList(request.getBody()));
}
```

반환 타입이 반드시 `StandardResponseDto`여야 하는 **기술적 제약**이 하나 있습니다. Spring AOP는 CGLIB 프록시로 동작하는데, 프록시 메서드의 선언 반환 타입이 그대로 유지되므로 **어드바이스가 다른 타입을 반환하면 `ClassCastException`이 납니다.** 즉 AOP는 성공 응답을 실패 전문으로 바꿔 끼울 수 있지만, 반환 타입 자체를 바꿀 수는 없습니다. 그래서 컨트롤러가 봉투 타입을 선언하고 Body만 채워 주면 ETF가 header와 result를 완성하는 방식입니다.

봉투를 컨트롤러에 노출하기 싫으시면 대안은 `ResponseBodyAdvice`입니다. 여기는 Jackson 직전이라 타입을 바꿀 수 있어서 컨트롤러가 업무 DTO를 그대로 반환할 수 있습니다. 대신 봉투가 씌워지는 지점이 코드에서 안 보여 신규 개발자가 추적하기 어렵습니다. 사내 표준 프레임워크라면 명시적인 쪽이 낫다고 봅니다.

## 4. 실패 전문은 네 군데서 발생합니다

이게 설계에서 가장 놓치기 쉬운 부분입니다.

| 예외 발생 위치        | 처리 주체                           | AOP가 볼 수 있나 |
| --------------------- | ----------------------------------- | ---------------- |
| Trace/JWT 필터        | Security `AuthenticationEntryPoint` | ✗                |
| Jackson 역직렬화 실패 | `GlobalExceptionHandler`            | ✗                |
| STF 헤더 검증         | Aspect → `ETF.businessFail`         | ✓                |
| Service 업무 예외     | Aspect → `ETF.businessFail`         | ✓                |

네 경로가 서로 다른 포맷을 내면 클라이언트가 분기 처리를 해야 합니다. 그래서 **`ETF`가 실패 전문 조립을 단독으로 책임지고, `GlobalExceptionHandler`와 `AuthenticationEntryPoint`도 `ETF`를 주입받아 쓰도록** 설계했습니다. `ETF`가 AOP 전용이 아니라 공용 컴포넌트여야 하는 이유입니다.

## 5. 성능 감시는 두 군데서 재는 게 낫습니다

필터에서 재면 역직렬화·직렬화·필터 체인까지 포함한 **총 응답시간**, AOP에서 재면 순수 **업무 처리시간**이 나옵니다. 둘의 차이가 크면 병목이 업무가 아니라 직렬화나 필터에 있다는 뜻이라 진단에 바로 쓸 수 있습니다. 임계치는 `nsight.tcf.slow-transaction-ms` 같은 설정값으로 빼고 초과 시 WARN을 권합니다.

## 6. 도입 순서

한 번에 다 넣지 않고 단계마다 독립적으로 검증 가능하게 쪼갰습니다.

1. **TcfTraceFilter** — 기존 `MdcLoggingFilter` 개명·확장. 다른 코드 영향 없음
2. **TcfTransaction + Aspect + STF/ETF** — 전문 봉투 없이 성능·거래 로그만. 컨트롤러 시그니처 안 바뀜
3. **표준 전문 적용** — 컨트롤러 시그니처 변경, pdmk-ui 샘플 JSON 동반 수정
4. **JwtAuthenticationFilter** — Security 체인 등록

2단계까지는 기존 REST 계약을 안 건드리므로 되돌리기도 쉽습니다. 3단계가 계약을 바꾸는 유일한 지점입니다.

---

`TcfContextHolder`(ThreadLocal)는 넣긴 했지만 필수는 아닙니다. Aspect가 지역 변수로 컨텍스트를 들고 있으면 되고, Service에서 guid나 userId를 직접 꺼내 쓸 일이 있을 때만 값을 합니다. ThreadLocal은 정리 책임이 따라오니 필요해질 때 추가하는 것도 방법입니다.

이 설계로 진행할지, 아니면 어느 부분을 조정할지 알려주시면 구현하겠습니다. 특히 3번의 컨트롤러 시그니처(봉투 노출 vs `ResponseBodyAdvice`)와 6번의 도입 범위를 정해 주시면 좋겠습니다.
