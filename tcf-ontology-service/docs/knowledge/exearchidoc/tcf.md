# TCF / 온라인 거래 흐름 (PDMG)

이 문서는 **현재 `pdmg-service` + `pdmg-fw` 동작**을 기준으로 한다.  
과거 Pdmk의 `@TcfTransaction` + STF/ETF AOP 설계안과는 진입·전문·예외 조립이 다르다.

관련: [pdmg-service 트랜잭션흐름.md](./pdmg-service%20트랜잭션흐름.md) · [트랜잭션처리.md](./트랜잭션처리.md) · [에러처리.md](./에러처리.md)

---

## 0. 판단 기준: "무엇을 볼 수 있는가"

Filter와 공통 Controller / Aspect 중 어디에 둘지는, 그 지점에서 **볼 수 있는 정보**로 결정한다.

| 지점 | 볼 수 있는 것 | 볼 수 없는 것 |
|------|----------------|----------------|
| Servlet Filter (`DefaultFilter`) | HTTP Body 캐시, URI, ServiceContext 시드 | 어느 Handler인지, 업무 DTO 완성본 |
| Interceptor (`ServicePreventionInterceptor`) | GUID, 요청 전문, ImageLog 선처리 | Facade TX 안 업무 예외 세부(후처리에서 보완) |
| OnlineTransactionController + TCF | `serviceId` → Handler 라우팅 · TimeoutExecutor 위임 | DB commit 자체 |
| OnlineTimeoutExecutor | 공통 SLA · Worker 외부 TX · 504/503 | 업무 분기 |
| Business Facade `@Transactional` | 업무 TX 참여(REQUIRED) / BEGIN(비활성 시) | HTTP 원문 |
| BizPrePostAspect (Service) | Service 인자·반환, 업무 선후처리 | Filter에서 난 예외 |
| `GlobalExceptionHandler` | Controller/Handler 경로 밖으로 나간 예외 | Filter 전용 인증 실패 등 |

한 줄 요약:

> **필터·시스템 선후처리는 “어떤 거래인지 몰라도 해야 하는 일”, Handler/Facade/Service는 “거래 식별 후 업무”**

GUID·MDC·ImageLog 선처리는 거래 매칭 전에도 필요하므로 Filter/Interceptor에 둔다.  
DB commit은 OnlineTimeoutExecutor(또는 Facade) 경계, 업무 선후처리는 Service Pointcut에 둔다.  
공통 타임아웃: [20.타임아웃.md](./20.타임아웃.md)

---

## 1. 현재 패키지·컴포넌트 (업무)

```text
nhnis.mg.co.a/
├─ entry/
│   ├─ handler/     *Handler — serviceId 라우팅
│   └─ aspect/      BizPrePostAspect, PdmgBizTxFlowAspect
├─ application/
│   ├─ facade/      *Facade — @Transactional
│   ├─ service/     *Service — 업무
│   └─ controller/  *Controller — TCF OFF 호환
├─ dto/             *DTOin / *DTOout / Sub
├─ persistence/dao/ *DAO
└─ config/          RdwDataSourceConfig, SecurityConfig, …
```

공통 FW (`pdmg-fw`):

```text
nhnis.fw.tcf.web.OnlineTransactionController
nhnis.fw.tcf.core.facade.TcfFacade
nhnis.fw.tcf.timeout.OnlineTimeoutExecutor   ← 공통 online timeout
nhnis.fw.commons.filter.DefaultFilter
nhnis.fw.commons.interceptor.ServicePreventionInterceptor
nhnis.fw.exception.GlobalExceptionHandler
```

---

## 2. 요청 흐름 (TCF ON)

```text
HTTP POST /{serviceId}   body: { hdr_nhnis, dto }
  │
  ├─① DefaultFilter
  │     Body 캐시, ServiceContext / GUID
  │
  ├─② ServicePreventionInterceptor
  │     시스템 선처리, ImageLog INSERT, 요청 전문 로그
  │
  ├─③ OnlineTransactionController
  │     serviceId 경로 매칭
  │
  ├─④ TcfFacade → OnlineTimeoutExecutor → Dispatcher → Handler(entry.handler)
  │
  ├─⑤ Facade(application.facade)  @Transactional(REQUIRED / rdw)
  │       → [BizPrePostAspect] Service → DAO → Mapper(rdw.mg.co.a)
  │
  ├─⑥ ResponseBodyAdvice — 시스템 후처리, { hdr_nhnis, dto } 봉투
  │
  └─⑦ afterCompletion — ImageLog UPDATE
```

TCF OFF일 때는 `application.controller.*Controller`가 HTTP 진입하고 Service(또는 Facade)를 호출한다.

---

## 3. 컨트롤러·Handler 계약

- **TCF ON**: 거래별 REST Controller를 두지 않는다. `POST /mgcoa8888S0` 형태.
- Handler는 `serviceId`에 맞는 Facade 메서드만 호출한다. TX·SQL 없음.
- 요청/응답 봉투는 `{ hdr_nhnis, dto }` (구 `StandardRequestDto` / STF·ETF 봉투와 다름).
- DTO 타입: `mgcoa8888S0DTOin` / `mgcoa8888S0DTOout` 등 (`nhnis.mg.co.a.dto`).

예시 (Facade):

```java
@Transactional(transactionManager = "rdwTransactionManager", readOnly = true)
public mgcoa8888S0DTOout mgcoa8888S0(Object dtoBody) throws Exception {
    mgcoa8888S0DTOin in = MappingUtil.convert(dtoBody, mgcoa8888S0DTOin.class);
    return service.mgcoa8888S0(in);
}
```

---

## 4. 실패 응답

PDMG(TCF ON) 실패 응답은 **`NH_NIS_ERR_DTO`** 를 `GlobalExceptionHandler` 등이 조립한다.  
STF/ETF 표준 전문(`result.resultCode` 등)을 쓰지 않는다. 상세: [에러처리.md](./에러처리.md) · [11.예외처리.md](./11.예외처리.md)

| 예외 | HTTP | stdErrCode |
|------|------|------------|
| `OnlineTimeoutException` | **504** | `FW_TIMEOUT` |
| `OnlineOverloadException` | **503** | `FW_OVERLOADED` |

→ [20.타임아웃.md](./20.타임아웃.md)

| 예외 위치 | 처리 |
|-----------|------|
| Handler 미매칭 | `ServiceHandlerNotFound` → GlobalExceptionHandler |
| 업무 `BizException` | GlobalExceptionHandler → `NH_NIS_ERR_DTO` |
| Facade/Service 예외 | TX rollback 후 동일 경로 |
| Filter/인증 전 | Security·Filter 쪽 처리 (Aspect 밖) |

---

## 5. 성능 감시

HTTP 전체·업무 구간·SQL은 Filter/Interceptor/TX-FLOW 로그와 `MybatisLogInterceptor`로 본다.  
상세: [성능감시.md](./성능감시.md)

---

## 6. (참고) 과거 Pdmk STF/ETF 설계

아래는 **이 저장소의 구 Pdmk TCF AOP 설계 메모**이다. PDMG 현행 구현과 혼동하지 말 것.

- `@TcfTransaction` + `TCFAspect` + STF/ETF
- `StandardRequestDto` / `StandardResponseDto`
- 거래별 Controller 메서드에 AOP

PDMG는 commons Filter·공통 OnlineController·Handler·Facade TX로 대체했다.
