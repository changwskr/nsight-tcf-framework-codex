# TCF AOP 정리

NSIGHT TCF에서 **AOP(관점 지향 프로그래밍)** 는 **선언적 어노테이션**으로 횡단 관심사를 Facade·Service 호출 **앞뒤에 자동 삽입**하는 데 쓰입니다.

| 관점 | 역할 | 핵심 |
|------|------|------|
| **프레임워크** | Spring AOP (런타임 프록시) | `spring-boot-starter-aop` (`tcf-web`) |
| **TX AOP** | DB 트랜잭션 경계 | Facade `@Transactional` |
| **정책 TX** | Timeout 런타임 덮어쓰기 | `PolicyDrivenTransactionAttributeSource` |
| **Cache AOP** | EhCache 조회 캐싱 | `OmCommonCodeCacheService` |
| **커스텀 Aspect** | — | **미사용** (`@Around` 0개) |
| **비-AOP TX** | 스케줄·수동 TX | `TransactionTemplate`, `PolicyDrivenTransactionExecutor` |

---

## 1. 개요 — NSIGHT에서 AOP란?

| 구분 | 내용 |
|------|------|
| AOP 엔진 | Spring AOP (AspectJ **어노테이션** 스타일, compile-time weaving 아님) |
| **실제 사용** | ① `@Transactional` (Facade) ② `@Cacheable` / `@CacheEvict` (tcf-om) |
| **미사용** | 커스텀 `@Aspect`, `@Async`, `@Validated` (메서드) |

검증·로깅·MDC는 **TCF STF/ETF**와 **Servlet Filter**가 담당 — AOP로 분리하지 않습니다.

---

## 2. AOP 활성화

### 2.1 의존성

`tcf-web`이 `spring-boot-starter-aop`를 **api**로 노출 → 업무 WAR·`tcf-om`이 transitively 포함.

| 모듈 | AOP starter | Facade `@Transactional` |
|------|-------------|-------------------------|
| `*-service`, `tcf-om` | ● | ● |
| `tcf-batch` | classpath 경유 | Facade 없음 |
| `tcf-ui` | ✕ | ✕ |

### 2.2 Spring Boot 자동 설정

| AutoConfiguration | 역할 |
|-------------------|------|
| `AopAutoConfiguration` | `@EnableAspectJAutoProxy` — 프록시 AOP |
| `TransactionAutoConfiguration` | `PlatformTransactionManager`, TX 관리 |
| `TcfCacheSupportAutoConfiguration` | `@EnableCaching` (tcf-om, 캐시 on 시) |

별도 `@EnableAspectJAutoProxy` / `@EnableTransactionManagement` **작성하지 않음**.

---

## 3. 프록시 동작 원리

```text
Handler
   │
   ▼
┌──────────────────────────────────┐
│  SvSampleFacade $Proxy (CGLIB)   │  ← @Transactional
│    TransactionInterceptor        │
│      · getConnection             │
│      · timeout 시작              │
│      · proceed() → 실제 메서드    │
│      · commit / rollback         │
└──────────────┬───────────────────┘
               ▼
        SvSampleFacade (target)
               ▼
        SvSampleService (AOP 없음)
```

| 대상 | 프록시 |
|------|--------|
| Facade (`@Service` 구체 클래스) | **CGLIB** 서브클래스 |
| `@Cacheable` Service | CGLIB |

**Spring 빈으로 외부에서 주입·호출**할 때만 Advice가 동작합니다.  
같은 클래스 내부 `this.method()` (**self-invocation**)은 프록시를 거치지 **않습니다**.

---

## 4. 사용 ① — `@Transactional` (핵심)

### 4.1 적용 위치

**Facade public 메서드에만** 선언 — NSIGHT 표준.

```17:19:sv-service/src/main/java/com/nh/nsight/marketing/sv/entry/facade/SvSampleFacade.java
    @Transactional(readOnly = true, timeout = 5)
    public Map<String, Object> inquiry(Map<String, Object> body, TransactionContext context) {
        return service.inquiry(body, context);
```

| 계층 | `@Transactional` |
|------|------------------|
| **Facade** | **● 권장** — 유스케이스 1건 = TX 경계 |
| Handler | ✕ |
| Service | ✕ (기본) |
| DAO | ✕ |

### 4.2 표준 어노테이션 값

| processingType | Facade 설정 | timeout (fallback) |
|----------------|-------------|---------------------|
| INQUIRY | `@Transactional(readOnly = true, timeout = 5)` | 5초 |
| CREATE/UPDATE/DELETE | `@Transactional(timeout = 10)` | 10초 |
| OM 배치·대량 조회 | `@Transactional(readOnly = true, timeout = 10)` | 10초 |

미지정 timeout → `spring.transaction.default-timeout` (기본 **5초**).

### 4.3 Policy-Driven TX Timeout (TCF 확장)

`nsight.tcf.timeout-policy-enabled=true`(기본)이면 AOP가 Facade `@Transactional(timeout=…)`을 **실행 시점**에 `TCF_SERVICE_TIMEOUT_POLICY.TX_TIMEOUT_SEC`로 **덮어씁니다**.

```text
STF → TimeoutContextHolder (정책 바인딩)
  → Handler → Facade.inquiry()
       → TransactionInterceptor
            → PolicyDrivenTransactionAttributeSource
                 → timeout = policy.txTimeoutSec
```

```29:36:tcf-web/src/main/java/com/nh/nsight/tcf/web/config/TcfTimeoutTransactionManagementConfiguration.java
    BeanPostProcessor policyDrivenTransactionAttributeSourceCustomizer() {
        // BeanFactoryTransactionAttributeSourceAdvisor의 AttributeSource 교체
        advisor.setTransactionAttributeSource(new PolicyDrivenTransactionAttributeSource(props));
```

Facade에 적은 `timeout=5`는 **fallback·문서 역할**에 가깝습니다.  
상세: [타임아웃관리.md](타임아웃관리.md)

### 4.4 TCF 파이프라인과의 관계

**TCF(STF → Handler → ETF)는 AOP 밖**입니다.  
AOP `@Transactional`은 **Handler → Facade** 구간에만 적용됩니다.

```text
TCF.process()
  STF                         ← AOP ✕ (거래로그 start)
  Dispatcher → Handler        ← AOP ✕
    Facade.inquiry()          ← ★ @Transactional AOP
      Service → DAO → MyBatis
    (return)                  ← ★ commit / rollback
  ETF                         ← AOP ✕ (거래로그 end)
```

| 이벤트 | TCF 거래로그 | Spring DB TX |
|--------|--------------|--------------|
| `BusinessException` | ETF FAIL (별도 DS) | **rollback** |
| 정상 return | ETF success | **commit** |
| `readOnly` inquiry | — | commit (변경 없음) |

거래로그는 **별도 DataSource** — 업무 TX 롤백과 **독립**합니다.

### 4.5 TransactionInterceptor가 하는 일

1. `PlatformTransactionManager.getTransaction()` — Hikari 커넥션 바인딩
2. `timeout` 초과 → `TransactionTimedOutException` → `E-TCF-TIME-002`
3. `readOnly` 플래그 설정
4. target 실행 (Service → DAO → MyBatis)
5. unchecked 예외 → **rollback**; 정상 → **commit**
6. 커넥션 pool 반환

DAO·Mapper는 **같은 Thread-bound Connection**을 사용합니다.

---

## 5. 사용 ② — `@Cacheable` / `@CacheEvict` (tcf-om)

프로젝트 전체에서 **캐시 AOP**는 주로 `OmCommonCodeCacheService` 한 클래스에서 사용합니다.

| 클래스 | 어노테이션 |
|--------|------------|
| `OmCommonCodeCacheService` | `@Cacheable`, `@CacheEvict`, `@Caching` |

```text
Service → OmCommonCodeCacheService.loadByCodeGroup("AUTH_CODE")
              │
              ▼
        CacheInterceptor (AOP)
          · hit  → DAO 생략
          · miss → DAO → EhCache put
```

| 캐시 alias | TTL | AOP |
|------------|-----|-----|
| `commonCode` | 30분 | `@Cacheable` on `OmCommonCodeCacheService` |
| `serviceCatalog` | 60분 | **AOP 없음** — `TcfCacheSupport` API |
| `sessionRegion` | 10분 | **AOP 없음** — 예약 |

`@EnableCaching`: `tcf-cache` → `TcfCacheSupportAutoConfiguration` (`nsight.tcf.cache.enabled`, 기본 true).

---

## 6. AOP가 **아닌** 트랜잭션

| 방식 | 사용처 | 특징 |
|------|--------|------|
| `@Transactional` (AOP) | Facade 온라인 거래 | Handler → Facade, Interceptor 경유 |
| `TransactionTemplate` | `@Scheduled` 배치 | 프로그램 TX, AOP 프록시 없음 |
| `PolicyDrivenTransactionExecutor` | Facade 밖 수동 TX | 정책 `txTimeoutSec` 적용 |

```text
OmSessionCleanupService — TransactionTemplate.setTimeout(10)
EbEventPublishScheduler — 스케줄러 직접 Service (Facade 미경유)
```

동일 `PlatformTransactionManager`를 쓰지만 **프록시를 거치지 않습니다**.

---

## 7. End-to-End — AOP 개입 구간

`POST /sv/online` → `SV.Sample.inquiry`:

```text
1. OnlineTransactionController     [AOP ✕]
2. TCF.process / STF               [AOP ✕]
3. TransactionDispatcher           [AOP ✕]
4. SvSampleHandler                 [AOP ✕]
5. SvSampleFacade.inquiry()        [AOP ● TransactionInterceptor]
6.   SvSampleService → DAO         [같은 TX, AOP ✕]
7. Facade return → commit          [AOP ●]
8. ETF.success                     [AOP ✕]
```

공통코드가 끼는 경우 (tcf-om):

```text
OmCommonCodeFacade.inquiry()
  → OmCommonCodeService
    → OmCommonCodeCacheService.loadByCodeGroup()   [AOP ● CacheInterceptor]
```

**한 메서드에 `@Transactional` + `@Cacheable` 동시 부착 패턴은 현재 없음.**

---

## 8. Self-Invocation 주의

Spring AOP는 **외부 빈 호출**에만 적용됩니다.

```java
public Map<String, Object> findInGroup(String codeGroup, String code) {
    for (Map<String, Object> row : loadByCodeGroup(codeGroup)) {
        // this.loadByCodeGroup() — @Cacheable 프록시 우회 가능
    }
}
```

| 문제 | 대응 |
|------|------|
| 캐시 미적용 | **다른 빈**에서 `loadByCodeGroup()` 호출, 또는 `TcfCacheSupport` API |
| Facade 내부 TX 전파 이상 | Facade 메서드 간 `this.xxx()` 지양 — **하나의 public Facade**에서 Service 조합 |

---

## 9. AOP vs 비-AOP 횡단 관심사

| 기술 | NSIGHT | 종류 |
|------|--------|------|
| `@Transactional` | Facade 전역 | `TransactionInterceptor` |
| `@Cacheable` | `OmCommonCodeCacheService` | `CacheInterceptor` |
| `PolicyDrivenTransactionAttributeSource` | TX timeout 정책 | AttributeSource 확장 |
| `PolicyDrivenQueryTimeoutInterceptor` | DB query timeout | **MyBatis Interceptor (AOP 아님)** |
| `@Aspect` 커스텀 | **미사용** | — |
| `GuidMdcCleanupFilter` | MDC 정리 | **Servlet Filter** |
| STF / ETF | Header 검증, 거래로그 | **TCF 파이프라인** |

Online timeout(`OnlineTransactionTimeoutExecutor`)도 **AOP가 아니라** TCF dispatch 래퍼입니다.

---

## 10. 모듈별 AOP 매트릭스

| 모듈 | TX AOP | Cache AOP | 커스텀 Aspect |
|------|--------|-----------|---------------|
| `*-service` | Facade 샘플 | ✕ | ✕ |
| `tcf-om` | Facade 20+ | `OmCommonCodeCacheService` | ✕ |
| `tcf-batch` | ✕ | ✕ | ✕ |
| `tcf-ui` | ✕ | ✕ | ✕ |
| `tcf-core` / `tcf-web` | ✕ (Facade 없음) | ✕ | ✕ |

---

## 11. 디버깅

| 목적 | 방법 |
|------|------|
| TX rollback 원인 | Facade에서 예외 swallow 여부, Handler catch 금지 |
| TX timeout | `@Transactional(timeout)`, 정책 `TX_TIMEOUT_SEC`, `E-TCF-TIME-002` |
| 정책 timeout 미적용 | `nsight.tcf.timeout-policy-enabled` 확인 |
| 캐시 miss/hit | `OmCommonCodeCacheService` debug `EhCache miss` |
| 프록시 여부 | 디버거에서 Facade 타입 `…$$SpringCGLIB$$0` |

| 증상 | AOP 관점 |
|------|----------|
| rollback 기대했는데 commit | `@Transactional`이 Service/DAO에만 있거나 Facade 밖 예외 |
| 캐시 안 먹음 | self-invocation, `@EnableCaching` off |
| tcf-ui에서 TX 없음 | 정상 — Relay만, Facade 없음 |

---

## 12. 신규 개발 가이드

| 할 일 | 권장 |
|-------|------|
| 온라인 거래 DB TX | Facade public + `@Transactional` |
| 조회 | `readOnly = true`, `timeout = 5` |
| CUD | `timeout = 10` |
| Timeout 운영 조정 | `TCF_SERVICE_TIMEOUT_POLICY` (코드 변경 최소) |
| 반복 조회 캐시 | `@Cacheable` **별도 Service** + 외부 빈 호출 |
| 스케줄·배치 TX | `TransactionTemplate` 또는 `PolicyDrivenTransactionExecutor` |
| 로깅·검증 횡단 | **커스텀 Aspect 추가 금지** — STF / Rule / Facade 패턴 |

커스텀 `@Aspect` 필요 시 `tcf-web`에 **프레임워크 Aspect 1개**로 집중 (현재 YAGNI).

---

## 13. 개발자 체크리스트

1. **`@Transactional`은 Facade** public 메서드에만
2. Handler·Service·DAO에는 **TX 어노테이션 없음**
3. Facade는 **Spring 빈 주입**으로 호출 (Handler → Facade)
4. **self-invocation** 으로 캐시·TX 기대하지 않기
5. Timeout 정책 on 시 Facade `timeout`은 **fallback**임을 인지
6. 거래로그 commit/rollback은 **TCF ETF** — Spring TX와 별개
7. **커스텀 `@Aspect`** 업무 WAR에 추가하지 않기

---

## 14. 구현 소스 (읽는 순서)

| 순서 | 파일 |
|------|------|
| 1 | `sv-service/.../facade/SvSampleFacade.java` |
| 2 | `tcf-web/.../PolicyDrivenTransactionAttributeSource.java` |
| 3 | `tcf-web/.../TcfTimeoutTransactionManagementConfiguration.java` |
| 4 | `tcf-web/.../PolicyDrivenTransactionExecutor.java` |
| 5 | `tcf-om/.../OmCommonCodeCacheService.java` |
| 6 | `tcf-cache/.../TcfCacheSupportAutoConfiguration.java` |
| 7 | `tcf-om/.../OmSessionCleanupService.java` — TransactionTemplate |

---

## 관련 문서

- [docs/architecture/32-AOP.md](../docs/architecture/32-AOP.md) — AOP 아키텍처 (상세)
- [docs/architecture/29-facade.md](../docs/architecture/29-facade.md) — Facade·TX 경계
- [docs/architecture/03-transaction.md](../docs/architecture/03-transaction.md) — TCF vs Spring TX
- [zdoc/어플리케이션계층.md](어플리케이션계층.md) — Facade 위치
- [zdoc/타임아웃관리.md](타임아웃관리.md) — Policy-Driven TX/Query timeout
- [zdoc/예외처리.md](예외처리.md) — rollback → businessFail
- [zdoc/DAO처리.md](DAO처리.md) — Facade TX 안의 DAO
