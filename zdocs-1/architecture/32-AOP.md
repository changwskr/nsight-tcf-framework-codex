# 32. AOP 아키텍처

| 항목 | 내용 |
|------|------|
| 문서 번호 | 32 |
| 제목 | AOP (Aspect-Oriented Programming) Architecture |
| 상위 문서 | [architecture.md](architecture.md) |
| 관련 문서 | [03-transaction.md](03-transaction.md), [08-timeout.md](08-timeout.md), [12-cache.md](12-cache.md), [29-facade.md](29-facade.md), [31-autoconfiguration.md](31-autoconfiguration.md) |
| 구현 모듈 | `tcf-web`, `*-service`, `tcf-om`, `tcf-cache` |
| 대상 | 프레임워크·업무 WAR 개발자 |

---

## 1. 개요

NSIGHT TCF에서 **AOP(관점 지향 프로그래밍)** 는 **선언적 어노테이션**으로 횡단 관심사(cross-cutting concern)를 Facade·Service 호출 **앞뒤에 자동 삽입**하는 데 쓰인다.

| 구분 | 내용 |
|------|------|
| AOP 프레임워크 | Spring AOP (AspectJ **어노테이션** 스타일, compile-time weaving 아님) |
| 의존 | `spring-boot-starter-aop` (`tcf-web` + 업무 WAR) |
| **커스텀 `@Aspect`** | **없음** — 프로젝트 전체에 `@Around`/`@Before` Aspect 클래스 0개 |
| 실제 사용 | ① **`@Transactional`** (Facade) ② **`@Cacheable` / `@CacheEvict`** (tcf-om) |

즉 NSIGHT의 AOP는 **Spring이 제공하는 두 가지 내장 Advice** — 트랜잭션·캐시 — 에 한정된다.

---

## 2. AOP가 켜지는 방법

### 2.1 의존성

`tcf-web`이 AOP starter를 **api**로 노출하고, 업무 WAR·`tcf-om`이 이를 transitively 포함한다.

```11:11:tcf-web/build.gradle
    api 'org.springframework.boot:spring-boot-starter-aop'
```

| 모듈 | `spring-boot-starter-aop` | Facade `@Transactional` |
|------|---------------------------|-------------------------|
| `*-service`, `tcf-om` | ● | ● |
| `tcf-batch` | tcf-web 경유 (classpath) | Facade 없음 — 미사용 |
| `tcf-ui` | ✕ | ✕ |

### 2.2 Spring Boot 자동 활성화

`@SpringBootApplication` → `@EnableAutoConfiguration` → Boot 내장 설정:

| AutoConfiguration | 역할 |
|-------------------|------|
| `AopAutoConfiguration` | `@EnableAspectJAutoProxy` — **프록시 기반 AOP** 활성 |
| `TransactionAutoConfiguration` | `PlatformTransactionManager`, `@EnableTransactionManagement` |

별도 `@EnableAspectJAutoProxy` / `@EnableTransactionManagement` **작성하지 않음** — Boot가 처리한다.

캐시 AOP는 `tcf-cache`의 `TcfCacheSupportAutoConfiguration`에서 **`@EnableCaching`** 으로 활성 (`tcf-om`만).

상세: [31-autoconfiguration.md](31-autoconfiguration.md)

---

## 3. 프록시 동작 원리

Spring AOP는 기본적으로 **런타임 프록시**를 만든다.

```text
[클라이언트] Handler
       │
       ▼
┌──────────────────────────────────┐
│  SvSampleFacade $Proxy (CGLIB)   │  ← @Transactional 적용
│    TransactionInterceptor        │
│      · getConnection             │
│      · setAutoCommit(false)      │
│      · timeout 시작              │
│      · proceed() → 실제 메서드    │
│      · commit / rollback         │
└──────────────┬───────────────────┘
               ▼
        SvSampleFacade (target)
               ▼
        SvSampleService (AOP 없음)
```

| 대상 클래스 | 프록시 방식 |
|-------------|-------------|
| Facade (`@Service` 구체 클래스) | **CGLIB** 서브클래스 프록시 |
| `@Cacheable` Service | 동일 — CGLIB |

**외부에서 Spring 빈으로 주입받아 호출**할 때만 Advice가 동작한다.  
같은 클래스 내부 `this.method()` 호출(self-invocation)은 프록시를 거치지 **않는다** ([§8](#8-주의-self-invocation)).

---

## 4. 사용 ① — `@Transactional` (핵심)

### 4.1 적용 위치

**Facade 메서드에만** 선언한다 — NSIGHT 표준.

```17:19:sv-service/src/main/java/com/nh/nsight/marketing/sv/facade/SvSampleFacade.java
    @Transactional(readOnly = true, timeout = 5)
    public Map<String, Object> inquiry(Map<String, Object> body, TransactionContext context) {
        return service.inquiry(body, context);
```

| 계층 | `@Transactional` | 이유 |
|------|------------------|------|
| **Facade** | **● 권장** | 유스케이스 1건 = TX 경계 |
| Handler | ✕ | TCF 어댑터만 |
| Service | ✕ (기본) | Facade TX 안에서 실행 |
| DAO | ✕ | Facade TX에 참여 |

Handler → Facade 호출 흐름: [29-facade.md](29-facade.md)

### 4.2 표준 어노테이션 값

| processingType | Facade 설정 | timeout |
|----------------|-------------|---------|
| INQUIRY | `@Transactional(readOnly = true, timeout = 5)` | 5초 |
| SAVE / UPDATE / DELETE | `@Transactional(timeout = 10)` | 10초 |
| OM 배치·카탈로그 등 | `@Transactional(readOnly = true, timeout = 10)` | 10초 |

`readOnly = true` → Hibernate/JPA가 아닌 **JDBC**에서도 DB 드라이버에 read-only 힌트 전달(가능 시).

미지정 timeout → `spring.transaction.default-timeout` (**5초**, `application.yml`).

상세: [08-timeout.md](08-timeout.md)

### 4.3 tcf-om Facade 예 — CRUD + 인증

```17:29:tcf-om/src/main/java/com/nh/nsight/marketing/om/facade/OmAuthFacade.java
    @Transactional(timeout = 5)
    public Map<String, Object> login(Map<String, Object> body, TransactionContext context) {
        return service.login(body, context);
    }

    @Transactional(readOnly = true, timeout = 5)
    public Map<String, Object> session(Map<String, Object> body, TransactionContext context) {
        return service.session(context);
    }
```

OM Facade 20개(`OmUserFacade`, `OmServiceCatalogFacade` …)가 동일 패턴으로 **수십 개** `@Transactional` 메서드를 갖는다.

### 4.4 TCF 온라인 거래와의 관계

**TCF 파이프라인(STF → Handler → ETF)은 AOP 밖**이다.  
AOP `@Transactional`은 **Handler가 Facade를 호출하는 구간**에만 걸린다.

```text
TCF.process()
  STF                    ← AOP 없음 (거래 시작 로그)
  Dispatcher → Handler   ← AOP 없음
    Facade.inquiry()     ← ★ @Transactional AOP 시작
      Service → DAO
    (Facade return)      ← ★ commit / rollback
  ETF                    ← AOP 없음 (거래 종료 로그)
```

| 이벤트 | TCF 거래 로그 (`TCF_TX_LOG`) | Spring DB TX (업무) |
|--------|------------------------------|---------------------|
| Facade에서 `BusinessException` | ETF가 종료 로그 (별도 DS) | **rollback** |
| Facade 정상 return | ETF success | **commit** |
| `readOnly` inquiry | — | commit (변경 없음) |

거래로그 INSERT는 **별도 DataSource·auto-commit** — 업무 TX 롤백과 **독립** ([03-transaction.md](03-transaction.md) §12).

### 4.5 AOP가 하는 일 (TransactionInterceptor)

Facade 메서드 1회 호출당:

1. `PlatformTransactionManager.getTransaction()` — Hikari 커넥션 바인딩
2. `timeout` 초과 시 `TransactionTimedOutException`
3. `readOnly` 플래그 설정
4. target 메서드 실행 (Service → DAO → MyBatis)
5. unchecked 예외 → **rollback**; 정상 → **commit**
6. 커넥션 해제 (Hikari pool 반환)

DAO·Mapper는 **같은 Thread-bound 커넥션**을 사용 — Facade TX 하나에 묶인다.

---

## 5. 사용 ② — `@Cacheable` / `@CacheEvict` (tcf-om)

### 5.1 적용 클래스

프로젝트 전체에서 **캐시 AOP 어노테이션**은 한 클래스만 사용한다.

| 클래스 | 어노테이션 |
|--------|------------|
| `OmCommonCodeCacheService` | `@Cacheable`, `@CacheEvict`, `@Caching` |

```27:38:tcf-om/src/main/java/com/nh/nsight/marketing/om/service/OmCommonCodeCacheService.java
    @Cacheable(cacheNames = TcfCacheNames.COMMON_CODE, key = "#codeGroup")
    public List<Map<String, Object>> loadByCodeGroup(String codeGroup) {
        log.debug("EhCache miss — DB load codeGroup={}", codeGroup);
        // ...
        return dao.searchCommonCodes(criteria);
    }

    @Cacheable(cacheNames = TcfCacheNames.COMMON_CODE, key = "'" + ALL_GROUPS_KEY + "'")
    public List<String> loadAllCodeGroupNames() {
```

```53:58:tcf-om/src/main/java/com/nh/nsight/marketing/om/service/OmCommonCodeCacheService.java
    @Caching(evict = {
            @CacheEvict(cacheNames = TcfCacheNames.COMMON_CODE, key = "#codeGroup"),
            @CacheEvict(cacheNames = TcfCacheNames.COMMON_CODE, key = "'" + ALL_GROUPS_KEY + "'")
    })
    public void evictCodeGroup(String codeGroup) {
```

### 5.2 CacheInterceptor 동작

```text
Facade → Service → OmCommonCodeCacheService.loadByCodeGroup("AUTH_CODE")
                         │
                         ▼
                   CacheInterceptor (AOP)
                     · cache key = "AUTH_CODE"
                     · hit  → DAO 호출 생략, 캐시 반환
                     · miss → DAO → EhCache put → 반환
```

| 캐시 alias | TTL | AOP 사용 |
|------------|-----|----------|
| `commonCode` | 30분 | `@Cacheable` on `OmCommonCodeCacheService` |
| `serviceCatalog` | 60분 | **AOP 없음** — `TcfCacheSupport` 프로그램 API |
| `sessionRegion` | 10분 | **AOP 없음** — 예약 영역 |

OM Admin 캐시 화면·evict: `OmCacheService` → `TcfCacheSupport.evict()` — **AOP 아님**, 직접 JCache API.

상세: [12-cache.md](12-cache.md)

### 5.3 `@EnableCaching` 활성화

`tcf-cache` AutoConfiguration:

```13:17:tcf-cache/src/main/java/com/nh/nsight/tcf/cache/config/TcfCacheSupportAutoConfiguration.java
@AutoConfiguration(after = {CacheAutoConfiguration.class, TcfCacheAutoConfiguration.class})
@ConditionalOnProperty(prefix = "nsight.tcf.cache", name = "enabled", havingValue = "true", matchIfMissing = true)
@ConditionalOnBean(CacheManager.class)
@EnableCaching
public class TcfCacheSupportAutoConfiguration {
```

---

## 6. AOP가 **아닌** 트랜잭션 — `TransactionTemplate`

`tcf-om`의 세션 정리 배치는 **프로그래밍** 트랜잭션 — AOP 어노테이션 없음.

```27:40:tcf-om/src/main/java/com/nh/nsight/marketing/om/service/OmSessionCleanupService.java
    public OmSessionCleanupService(..., PlatformTransactionManager transactionManager) {
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.transactionTemplate.setTimeout(10);
    }

    public Optional<OmSessionCleanupResult> runScheduled() {
        OmSessionCleanupResult result = transactionTemplate.execute(status -> doCleanup(false, null, null));
```

| 방식 | 사용처 | 호출 경로 |
|------|--------|-----------|
| `@Transactional` (AOP) | Facade 온라인 거래 | Handler → Facade |
| `TransactionTemplate` | 스케줄러·배치 내부 | `@Scheduled` → Service 직접 |

동일 `PlatformTransactionManager`(Hikari `dataSource`)를 쓰지만, **Interceptor 프록시를 거치지 않는다**.

---

## 7. End-to-End — AOP가 개입하는 구간

`POST /sv/online` → `SV.Sample.inquiry` 예:

```text
1. OnlineTransactionController     [AOP ✕]
2. TCF.process / STF               [AOP ✕]
3. TransactionDispatcher           [AOP ✕]
4. SvSampleHandler                 [AOP ✕]
5. SvSampleFacade.inquiry()        [AOP ● TransactionInterceptor]
6.   SvSampleService.inquiry()     [같은 TX 컨텍스트, AOP ✕]
7.   SvSampleDao → MyBatis         [같은 Connection]
8. Facade return → commit          [AOP ●]
9. ETF.success                     [AOP ✕]
```

`tcf-om` 공통코드 조회가 끼는 경우:

```text
OmCommonCodeFacade.inquiry()
  → OmCommonCodeService
    → OmCommonCodeCacheService.loadByCodeGroup()   [AOP ● CacheInterceptor]
      → (miss) OmOperationDao
```

**캐시 AOP와 TX AOP는 다른 Facade/Service 메서드**에 각각 적용 — 한 메서드에 `@Transactional` + `@Cacheable` 동시 부착 패턴은 현재 코드에 없음.

---

## 8. 주의 — Self-Invocation

Spring AOP 프록시는 **외부 호출**에만 적용된다.

```41:50:tcf-om/src/main/java/com/nh/nsight/marketing/om/service/OmCommonCodeCacheService.java
    public Map<String, Object> findInGroup(String codeGroup, String code) {
        // ...
        for (Map<String, Object> row : loadByCodeGroup(codeGroup)) {  // this.loadByCodeGroup — 프록시 우회
```

`findInGroup()` 내부에서 `this.loadByCodeGroup()` 호출 시 **`@Cacheable`이 적용되지 않을 수 있다**.  
캐시가 필요하면 **다른 빈에서** `loadByCodeGroup()`을 호출하거나, `TcfCacheSupport` 직접 API를 쓴다.

동일하게 Facade 내부에서 `this.otherTransactionalMethod()` 호출 시 **TX 전파·rollback 규칙이 기대와 다를 수 있다** — Facade 메서드 간 조합은 **하나의 public Facade 메서드**에서 Service를 여러 번 호출하는 방식을 권장.

---

## 9. 적용·미적용 요약

| 기술 | NSIGHT 사용 | AOP 종류 |
|------|-------------|----------|
| `@Transactional` | Facade 전역 | `TransactionInterceptor` |
| `@Cacheable` / `@CacheEvict` | `OmCommonCodeCacheService` | `CacheInterceptor` |
| `@Aspect` 커스텀 | **미사용** | — |
| `@Async` | **미사용** | — |
| `@Validated` (메서드) | **미사용** | — |
| Servlet `Filter` | `GuidMdcCleanupFilter` | **AOP 아님** (Filter 체인) |
| Spring Session Filter | tcf-om | **AOP 아님** |

검증·로깅·MDC는 **TCF STF/ETF**와 **Servlet Filter**가 담당 — AOP로 분리하지 않았다.

---

## 10. 모듈별 AOP 매트릭스

| 모듈 | TX AOP (`@Transactional`) | Cache AOP | 커스텀 Aspect |
|------|----------------------------|-----------|---------------|
| `sv-service` … `mg-service` | Facade 샘플 | ✕ | ✕ |
| `tcf-om` | Facade 20+ | `OmCommonCodeCacheService` | ✕ |
| `tcf-batch` | ✕ | ✕ | ✕ |
| `tcf-ui` | ✕ (tcf-web 미의존) | ✕ | ✕ |
| `tcf-core` / `tcf-web` | ✕ (Facade 없음) | ✕ | ✕ |

---

## 11. 디버깅·확인

| 목적 | 방법 |
|------|------|
| TX rollback 원인 | Facade에서 예외 swallow 여부, `BusinessException` vs `RuntimeException` |
| TX timeout | `@Transactional(timeout=N)`, `spring.transaction.default-timeout` |
| 캐시 miss/hit | `OmCommonCodeCacheService` debug 로그 `EhCache miss` |
| 프록시 여부 | 디버거에서 Facade 빈 타입이 `…$$SpringCGLIB$$0` 인지 확인 |
| AOP 활성 | 기동 `--debug` → `AopAutoConfiguration` Positive match |

| 증상 | AOP 관점 |
|------|----------|
| DAO는 commit 됐는데 롤백 기대 | `@Transactional`이 Service/DAO에 없고 Facade 밖에서 예외 |
| 캐시가 안 먹음 | self-invocation, `@EnableCaching` off, `CacheManager` 빈 없음 |
| tcf-ui에서 TX 없음 | 정상 — Relay만, Facade 없음 |

---

## 12. 신규 개발 가이드

| 할 일 | 권장 |
|-------|------|
| 온라인 거래 DB TX | Facade public 메서드에 `@Transactional` |
| 조회 | `readOnly = true`, `timeout = 5` |
| 등록·수정·삭제 | `timeout = 10`, `readOnly` 생략 |
| 반복 DB 조회 캐시 | `@Cacheable` Service **별도 클래스** + 외부 빈 호출 |
| 스케줄·배치 TX | `TransactionTemplate` (Facade 경유 불필요) |
| 로깅·검증 횡단 관심사 | **커스텀 Aspect 추가하지 말 것** — TCF STF/Rule/Facade 패턴 유지 |

커스텀 `@Aspect`가 필요해지면 `tcf-web`에 **프레임워크 Aspect 1개**로 모으고, 업무 WAR에 흩 뿌리지 않는다 (현재는 YAGNI).

---

## 13. 관련 문서

| 주제 | 문서 |
|------|------|
| Facade·TX 경계 | [29-facade.md](29-facade.md), [01-application-layer.md](01-application-layer.md) |
| TCF vs Spring TX | [03-transaction.md](03-transaction.md) |
| timeout | [08-timeout.md](08-timeout.md) |
| EhCache·캐시 | [12-cache.md](12-cache.md) |
| AOP classpath | [31-autoconfiguration.md](31-autoconfiguration.md) |

---

## 14. 변경 이력

| 일자 | 변경 내용 |
|------|-----------|
| 2026-06 | 최초 작성 — @Transactional·@Cacheable 중심 NSIGHT AOP 사용 정리 |
