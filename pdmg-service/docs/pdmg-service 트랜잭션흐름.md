# pdmg-service 트랜잭션 흐름

기준: TCF ON (`nhnis.fw.tcf.enabled=true`)  
관련: [01.트랜잭션처리 변경.md](./01.트랜잭션처리%20변경.md) · [트랜잭션처리.md](./트랜잭션처리.md) · [MG-NAMING_CONVENTION.md](./MG-NAMING_CONVENTION.md) §4

## 한눈에 보기

```text
[브라우저 / pdmg-ui :8090]
        │  브라우저 직접 POST http://localhost:8080/{serviceId} (CORS)
        │  body: { hdr_nhnis, dto }
        ▼
[pdmg-ui TransactionRelayService]
        │  POST http://localhost:8080/{serviceId}
        ▼
┌───────────────────────────────────────────────────────────┐
│ pdmg-service (:8080) + pdmg-fw                            │
│                                                           │
│  1. DefaultFilter                                         │
│     · Body 캐시, ServiceContext / GUID / Header           │
│                                                           │
│  2. ServicePreventionInterceptor (시스템 선처리)          │
│     · 요청전문 로그, ImageLog INSERT                      │
│                                                           │
│  3. OnlineTransactionController  POST /{serviceId}        │
│     · serviceId = Header rms_svc_c → path                 │
│                                                           │
│  4. TcfFacade → OnlineTimeoutExecutor → Dispatcher → Handler │
│     · nhnis.mg.co.a.entry.handler.*Handler                    │
│     · 공통 timeout: nhnis.fw.timeout (기본 5000ms)            │
│                                                               │
│  5. Business Facade  ★ DB TX (Executor가 BEGIN, REQUIRED)    │
│     · nhnis.mg.co.a.application.facade.*Facade                │
│     · @Transactional(rdwTransactionManager) 참여              │
│         │                                                     │
│         ▼                                                     │
│  6. BizPrePostAspect(Service) 선처리                          │
│     · Service (application.service) → DAO (persistence)       │
│     · BizPrePostAspect 후처리                                 │
│     · Deadline 재검사 → commit / rollback                     │
│     · timeout 시 요청 Thread: 504 FW_TIMEOUT                  │
│                                                           │
│  7. ResponseBodyAdvice (시스템 후처리)                    │
│     · { hdr_nhnis, dto } 응답 봉투                        │
│                                                           │
│  8. afterCompletion · ImageLog UPDATE                     │
└───────────────────────────────────────────────────────────┘
```

```text
Filter → 시스템선처리 → OnlineController → TcfFacade
  → OnlineTimeoutExecutor(Worker+TX) → Handler(entry)
  → Facade(application, @Transactional REQUIRED)
       → [업무선처리] → Service → DAO → [업무후처리]
  → 시스템후처리
```

공통 타임아웃: [20.타임아웃.md](./20.타임아웃.md)

## serviceId 매핑

| URL | Handler | Facade | Service |
|-----|---------|--------|---------|
| `/mgcoa5530S0` | mgcoa5530Handler | mgcoa5530Facade | mgcoa5530Service |
| `/mgcoa8888S0` | mgcoa8888Handler | mgcoa8888Facade | mgcoa8888Service |
| `/mgcoa8888D0` | mgcoa8888Handler | mgcoa8888Facade | mgcoa8888Service |
| `/mgcoa9000S0` 등 | mgcoa9000Handler | mgcoa9000Facade | mgcoa9000Service |
| `/mgcoa9999S0` | mgcoa9999Handler | mgcoa9999Facade | mgcoa9999Service |

패키지 공통 접두: `nhnis.mg.co.a.`  
DTO: `nhnis.mg.co.a.dto` · Mapper: `rdw.mg.co.a/`

## Business Facade 트랜잭션 선언

### 원칙

| 계층 | `@Transactional` | 역할 |
|------|------------------|------|
| OnlineTransactionController | X | URL 수신 (pdmg-fw) |
| TcfFacade / OnlineTimeoutExecutor | Executor가 **외부 TX** | 공통 SLA · Worker |
| Dispatcher / Handler | X (TX 안) | 라우팅 (`entry.handler`) |
| **Business Facade** | **O (REQUIRED)** | 업무 유스케이스 · 같은 TX 참여 |
| Service | 기본 X | Facade TX 안에서 실행 (`application.service`) |
| DAO | X | SQL만 (`persistence.dao`) |

업무 선후처리(`BizPrePostAspect`)는 **Service public 메서드**에 건다.  
→ Facade TX 시작 후 · Service 호출 전후에 실행된다.

```text
Handler
  → Facade (@Transactional)
       → [BizPrePostAspect.before]
       → Service → DAO
       → [BizPrePostAspect.after]
  → (Facade 반환 시 commit)
```

### 조회(S0)

```java
@Transactional(
        transactionManager = "rdwTransactionManager",
        readOnly = true)
public mgcoa5530S0DTOout mgcoa5530S0(Object dtoBody) throws Exception {
    ...
    return service.mgcoa5530S0(input);
}
```

### 쓰기(D0 등)

```java
@Transactional(
        transactionManager = "rdwTransactionManager",
        rollbackFor = Exception.class)
public mgcoa8888D0DTOout mgcoa8888D0(Object dtoBody) throws Exception {
    ...
    return service.mgcoa8888D0(input);
}
```

### transactionManager

```java
// nhnis.mg.co.a.config.RdwDataSourceConfig
@Bean
public PlatformTransactionManager rdwTransactionManager(DataSource rdwDataSource) { ... }
```

`rdwTransactionManager` 를 **명시**한다.

### 적용 Facade (예시)

| Facade 메서드 | 유형 | 선언 |
|---------------|------|------|
| `mgcoa5530Facade.mgcoa5530S0` | 조회 | `readOnly = true` |
| `mgcoa8888Facade.mgcoa8888S0` | 조회 | `readOnly = true` |
| `mgcoa8888Facade.mgcoa8888D0` | 삭제 | `rollbackFor = Exception.class` |
| `mgcoa9000Facade.mgcoa9000S0` 등 | 조회/쓰기 | 유형별 |
| `mgcoa9999Facade.mgcoa9999S0` | 조회 | `readOnly = true` |

### 주의

1. Facade self-invocation 에는 `@Transactional` 이 걸리지 않는다.
2. Service에 TX를 중복 선언하지 않는 것이 기본이다.
3. Filter / Interceptor / ImageLog 는 업무 TX 밖이다.
4. 업무 예외 시 쓰기 TX는 롤백되고, 시스템 후처리·응답은 FW가 담당한다.

## TCF OFF (호환)

```text
Controller (application.controller) → Service → DAO
```

거래별 Controller가 HTTP 진입한다. 현재 샘플은 Service 직호출이다.
