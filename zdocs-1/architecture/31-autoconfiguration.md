# 31. AutoConfiguration 처리 원리

| 항목 | 내용 |
|------|------|
| 문서 번호 | 31 |
| 제목 | Spring Boot AutoConfiguration Architecture |
| 상위 문서 | [architecture.md](architecture.md) |
| 관련 문서 | [20-env-spring.md](20-env-spring.md), [26-mybatis.md](26-mybatis.md), [30-springboot.md](30-springboot.md), [12-cache.md](12-cache.md) |
| 구현 모듈 | `tcf-web`, `tcf-cache` |
| 대상 | 프레임워크·업무 WAR 개발자 |

---

## 1. 개요

**AutoConfiguration**은 Spring Boot가 classpath에 특정 JAR가 있을 때 **조건에 맞으면 빈을 자동 등록**하는 메커니즘이다.

NSIGHT TCF에서 AutoConfiguration을 제공하는 모듈은 **2개**뿐이다.

| 모듈 | `AutoConfiguration.imports` | 역할 |
|------|----------------------------|------|
| `tcf-web` | 5개 클래스 | TCF 프로퍼티, DataSource, MyBatis, 거래로그 JDBC |
| `tcf-cache` | 2개 클래스 | EhCache + Spring Cache, `TcfCacheSupport` |

`tcf-core`·`tcf-util`은 AutoConfiguration **없음** — `@Component`는 업무 WAR의 `@ComponentScan("com.nh.nsight")`으로 등록된다.  
`EnvironmentPostProcessor`(`NsightTxlogPathEnvironmentPostProcessor`)는 AutoConfiguration **이전** 단계에서 동작한다 ([30-springboot.md](30-springboot.md) Phase 1).

---

## 2. Spring Boot 3에서의 발견·로드

### 2.1 등록 파일 (Spring Boot 2.7+ / 3.x)

과거 `META-INF/spring.factories`의 `EnableAutoConfiguration` 키는 **폐기**되었다.  
Spring Boot 3는 아래 파일만 읽는다.

```text
META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
```

한 줄에 **FQCN 하나** — `@AutoConfiguration` 또는 `@Configuration` 클래스 모두 가능.

### 2.2 트리거: `@EnableAutoConfiguration`

업무 WAR의 `@SpringBootApplication`은 내부적으로 `@EnableAutoConfiguration`을 포함한다.

```text
@SpringBootApplication
  = @Configuration
  + @EnableAutoConfiguration   ← AutoConfiguration.imports 로드
  + @ComponentScan             ← com.nh.nsight 패키지 스캔 (별도 경로)
```

**AutoConfiguration과 ComponentScan은 독립**이다.

| 메커니즘 | 등록 대상 | NSIGHT 예 |
|----------|-----------|-----------|
| AutoConfiguration | JAR `imports` + `@Conditional*` | `dataSource`, `SqlSessionFactory` |
| ComponentScan | `@Component`/`@Service`/`@Repository` | `TCF`, `Handler`, `OnlineTransactionController` |

### 2.3 처리 파이프라인

```text
SpringApplication.run(mainClass)
    │
    ▼
① AutoConfigurationImportSelector
    · classpath 전체에서 *.imports 수집 (Boot + tcf-web + tcf-cache + …)
    · spring.autoconfigure.exclude 적용
    │
    ▼
② AutoConfigurationSorter
    · @AutoConfiguration(before= / after=) 로 순서 정렬
    · Boot 내장 AutoConfig와 NSIGHT AutoConfig interleave
    │
    ▼
③ ConfigurationClassParser
    · 각 AutoConfiguration 클래스를 @Configuration처럼 파싱
    · @ConditionalOnClass / @ConditionalOnProperty / @ConditionalOnBean 평가
    · 조건 불충족 → 해당 클래스 전체 스킵 (Exclusion)
    │
    ▼
④ BeanDefinitionRegistry
    · @Bean 메서드 등록 → 인스턴스화·의존성 주입
    │
    ▼
ApplicationContext refresh 완료
```

조건 평가는 **기동 시 1회**(및 `@ConditionalOnBean`은 다른 빈 등록 후 재평가) 이루어진다.

---

## 3. NSIGHT AutoConfiguration 전체 목록

### 3.1 `tcf-web`

**파일:** `tcf-web/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`

```text
com.nh.nsight.tcf.web.config.TcfAutoConfiguration
com.nh.nsight.tcf.web.config.TcfPrimaryDataSourceAutoConfiguration
com.nh.nsight.tcf.web.config.TcfMyBatisAutoConfiguration
com.nh.nsight.tcf.web.config.TcfTransactionLogDataSourceConfiguration
com.nh.nsight.tcf.web.config.TcfTransactionLogConfiguration
```

### 3.2 `tcf-cache`

**파일:** `tcf-cache/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`

```text
com.nh.nsight.tcf.cache.config.TcfCacheAutoConfiguration
com.nh.nsight.tcf.cache.config.TcfCacheSupportAutoConfiguration
```

---

## 4. 클래스별 상세 — `tcf-web`

### 4.1 `TcfAutoConfiguration`

| 항목 | 내용 |
|------|------|
| 어노테이션 | `@AutoConfiguration` |
| 조건 | 없음 (classpath에 tcf-web 있으면 후보) |
| 역할 | `TcfProperties` 바인딩 |

```7:9:tcf-web/src/main/java/com/nh/nsight/tcf/web/config/TcfAutoConfiguration.java
@AutoConfiguration
@EnableConfigurationProperties(TcfProperties.class)
public class TcfAutoConfiguration {
```

`nsight.tcf.*` 프로퍼티가 `TcfProperties` 빈으로 바인딩된다 — STF·ETF·거래로그 설정의 **중심**.

| 프로퍼티 | 기본값 | 용도 |
|----------|--------|------|
| `transaction-log-enabled` | `true` | 거래로그 AutoConfig on/off |
| `transaction-log-datasource.separate` | `true` | 거래로그 DS 분리 |
| `transaction-log-schema-auto-init` | `true` | `TCF_TX_LOG` DDL |
| `session-validation-enabled` | `false` | STF 세션 검증 |

### 4.2 `TcfPrimaryDataSourceAutoConfiguration`

| 항목 | 내용 |
|------|------|
| 순서 | `before = TcfMyBatisAutoConfiguration` |
| 조건 | `spring.datasource.url` 존재, **`dataSource` 빈 없음** |
| 생성 빈 | `@Primary` `dataSource` (Hikari) |

```19:30:tcf-web/src/main/java/com/nh/nsight/tcf/web/config/TcfPrimaryDataSourceAutoConfiguration.java
@AutoConfiguration(before = TcfMyBatisAutoConfiguration.class)
@ConditionalOnProperty(prefix = "spring.datasource", name = "url")
@ConditionalOnMissingBean(name = "dataSource")
public class TcfPrimaryDataSourceAutoConfiguration {

    @Bean(name = "dataSource")
    @Primary
    public DataSource dataSource(DataSourceProperties properties) {
        return properties.initializeDataSourceBuilder().build();
    }
```

**왜 필요한가?**

거래로그용 `transactionLogDataSource`를 추가하면 Spring Boot **`DataSourceAutoConfiguration`이 백오프**할 수 있다.  
업무 MyBatis는 **`@Primary dataSource`** 가 반드시 있어야 하므로, NSIGHT가 **명시 재등록**한다.

```text
[문제 시나리오 — TcfPrimary 없을 때]
transactionLogDataSource 등록
  → Boot DataSourceAutoConfiguration 비활성
  → dataSource 빈 없음
  → MyBatis·업무 DAO 기동 실패

[NSIGHT 해결]
TcfPrimaryDataSourceAutoConfiguration
  → spring.datasource.* 로 dataSource @Primary 생성
  → TcfMyBatisAutoConfiguration 연결
```

### 4.3 `TcfMyBatisAutoConfiguration`

| 항목 | 내용 |
|------|------|
| 순서 | `after = DataSourceAutoConfiguration` |
| 조건 | MyBatis classpath, `DataSource` 빈 존재, **`SqlSessionFactory` 없음** |
| 생성 빈 | `SqlSessionFactory`, `SqlSessionTemplate` |

```21:34:tcf-web/src/main/java/com/nh/nsight/tcf/web/config/TcfMyBatisAutoConfiguration.java
@AutoConfiguration(after = DataSourceAutoConfiguration.class)
@ConditionalOnClass(SqlSessionFactory.class)
@ConditionalOnBean(DataSource.class)
@ConditionalOnMissingBean(SqlSessionFactory.class)
public class TcfMyBatisAutoConfiguration {

    public SqlSessionFactory sqlSessionFactory(
            @Qualifier("dataSource") DataSource dataSource,
            MybatisProperties properties) throws Exception {
```

**mybatis-spring-boot-starter** 자체 AutoConfiguration과의 관계:

| 상황 | Boot/MyBatis starter | NSIGHT |
|------|----------------------|--------|
| DataSource 1개 | starter가 Factory 생성 | `@ConditionalOnMissingBean` → **스킵** |
| DataSource 2개+ | starter 백오프 가능 | **`@Qualifier("dataSource")`로 Factory 생성** |

업무 WAR는 `@MapperScan`으로 Mapper 인터페이스를 **별도 등록** — AutoConfiguration은 **Factory만** 만든다.  
상세: [26-mybatis.md](26-mybatis.md)

### 4.4 `TcfTransactionLogDataSourceConfiguration`

| 항목 | 내용 |
|------|------|
| 클래스 어노테이션 | `@Configuration` (imports에 등록) |
| 조건 | `nsight.tcf.transaction-log-enabled=true` (기본 true) |
| 중첩 조건 | `transaction-log-datasource.separate=true` (기본 true) |
| 생성 빈 | `transactionLogDataSource`, `transactionLogJdbcTemplate` |

```16:27:tcf-web/src/main/java/com/nh/nsight/tcf/web/config/TcfTransactionLogDataSourceConfiguration.java
@Configuration
@ConditionalOnProperty(prefix = "nsight.tcf", name = "transaction-log-enabled", havingValue = "true", matchIfMissing = true)
public class TcfTransactionLogDataSourceConfiguration {

    @Configuration
    @ConditionalOnProperty(prefix = "nsight.tcf.transaction-log-datasource", name = "separate", havingValue = "true", matchIfMissing = true)
    static class SeparateTransactionLogDataSourceConfiguration {
```

URL은 `tcf-web` `application-{profile}.yml` + `nsight.txlog.path` placeholder로 결정된다.

| 프로파일 | 거래로그 JDBC URL (요약) |
|----------|-------------------------|
| local | `jdbc:h2:file:${nsight.txlog.path}/nsight_om` |
| dev | `jdbc:h2:tcp://127.0.0.1:9092/...` (ztomcat H2 TCP) |

### 4.5 `TcfTransactionLogConfiguration`

| 항목 | 내용 |
|------|------|
| 클래스 어노테이션 | `@Configuration` |
| 분기 | `separate=true` vs `separate=false` |

| `separate` | `TransactionLogRepository` 주입 | 스키마 초기화 |
|------------|-----------------------------------|---------------|
| `true` (sv-service 등) | `transactionLogJdbcTemplate` | 별도 H2 `TCF_TX_LOG` |
| `false` (tcf-om local) | 기본 `jdbcTemplate` (업무 DS) | OM과 동일 DB |

```37:45:tcf-web/src/main/java/com/nh/nsight/tcf/web/config/TcfTransactionLogConfiguration.java
    @Configuration
    @ConditionalOnProperty(prefix = "nsight.tcf.transaction-log-datasource", name = "separate", havingValue = "false")
    static class PrimaryDatasourceTransactionLogConfiguration {

        @Bean
        public TransactionLogRepository jdbcTransactionLogRepository(
                JdbcTemplate jdbcTemplate,
                TcfProperties properties) {
```

`TransactionLogSchemaInitializer`는 `@PostConstruct`로 `TCF_TX_LOG` DDL 실행 (`transaction-log-schema-auto-init=true`).

---

## 5. 클래스별 상세 — `tcf-cache`

`tcf-om`만 `implementation project(':tcf-cache')` — **tcf-om 기동 시에만** 아래 AutoConfig가 classpath에 포함된다.

### 5.1 `TcfCacheAutoConfiguration`

| 항목 | 내용 |
|------|------|
| 순서 | `after = CacheAutoConfiguration` |
| 조건 | `nsight.tcf.cache.enabled=true` (기본 true) |
| 생성 빈 | `TcfCacheInitializer` (EhCache XML 경로 검증·로그) |

Boot `CacheAutoConfiguration` + `tcf-om`의 `spring.cache.type=jcache` 설정이 **`JCacheCacheManager`** 를 만든 **후**에 동작한다.

### 5.2 `TcfCacheSupportAutoConfiguration`

| 항목 | 내용 |
|------|------|
| 순서 | `after = CacheAutoConfiguration`, `TcfCacheAutoConfiguration` |
| 조건 | cache enabled + **`CacheManager` 빈 존재** |
| 부가 | `@EnableCaching` |
| 생성 빈 | `TcfCacheSupport` (`JCacheCacheManager` 필수) |

```13:26:tcf-cache/src/main/java/com/nh/nsight/tcf/cache/config/TcfCacheSupportAutoConfiguration.java
@AutoConfiguration(after = {CacheAutoConfiguration.class, TcfCacheAutoConfiguration.class})
@ConditionalOnProperty(prefix = "nsight.tcf.cache", name = "enabled", havingValue = "true", matchIfMissing = true)
@ConditionalOnBean(CacheManager.class)
@EnableCaching
public class TcfCacheSupportAutoConfiguration {

    @Bean
    TcfCacheSupport tcfCacheSupport(CacheManager cacheManager) {
        if (!(cacheManager instanceof JCacheCacheManager jcache)) {
            throw new IllegalStateException(
                    "TCF cache requires JCacheCacheManager but got: " + cacheManager.getClass().getName());
        }
```

`spring.cache.type`이 `jcache`가 아니면 기동 **실패** — OM은 `ehcache.xml` + JCache 전제.

상세: [12-cache.md](12-cache.md)

---

## 6. AutoConfiguration 실행 순서 (NSIGHT 관련)

Boot 내장 설정과 NSIGHT 설정이 **`before`/`after`** 로 정렬된다.

```text
… (Boot) DataSourceAutoConfiguration
         │
         ▼
TcfPrimaryDataSourceAutoConfiguration     ← dataSource @Primary
         │
         ▼
TcfTransactionLogDataSourceConfiguration  ← transactionLogDataSource (separate=true)
         │
         ▼
TcfMyBatisAutoConfiguration               ← SqlSessionFactory (@Qualifier dataSource)
         │
         ▼
TcfTransactionLogConfiguration            ← TransactionLogRepository
         │
… (Boot) CacheAutoConfiguration           ← tcf-om only
         │
         ▼
TcfCacheAutoConfiguration
         │
         ▼
TcfCacheSupportAutoConfiguration          ← @EnableCaching, TcfCacheSupport

(병렬) TcfAutoConfiguration               ← TcfProperties (순서 제약 없음, 초기 바인딩)
```

같은 `before`/`after` 레벨에서는 **imports 파일 등록 순서**와 Boot 버전 정렬 규칙이 추가로 적용된다.

---

## 7. `@Conditional*` — 적용 vs 스킵

AutoConfiguration은 **조건 불충족 시 클래스 전체가 무시**된다.

| 어노테이션 | NSIGHT 사용 예 | 의미 |
|------------|----------------|------|
| `@ConditionalOnClass` | `SqlSessionFactory`, `DataSource` | classpath에 클래스 있을 때 |
| `@ConditionalOnBean` | `DataSource`, `CacheManager` | 다른 빈 등록 후 |
| `@ConditionalOnMissingBean` | `dataSource`, `SqlSessionFactory` | **사용자 정의 빈 우선** |
| `@ConditionalOnProperty` | `transaction-log-enabled`, `cache.enabled` | yml/JVM 값 |

### 7.1 모듈 classpath별 적용 매트릭스

| AutoConfiguration | sv-service | tcf-om | tcf-batch | tcf-ui |
|-------------------|------------|--------|-----------|--------|
| TcfAutoConfiguration | ● | ● | ● | ✕ (tcf-web 없음) |
| TcfPrimaryDataSourceAutoConfiguration | ● | ● | ● | ✕ |
| TcfMyBatisAutoConfiguration | ● (mybatis CP) | ● | ✕ (mybatis CP 없음) | ✕ |
| TcfTransactionLog* | ● | ● (separate=false 가능) | ● | ✕ |
| TcfCache* | ✕ | ● | ✕ | ✕ |

● = classpath + 조건 충족 시 적용, ✕ = JAR 미의존 또는 `@ConditionalOnClass` 실패

---

## 8. Boot 내장 AutoConfiguration과의 상호작용

```text
┌─────────────────────────────────────────────────────────────┐
│ Spring Boot Built-in                                         │
│  DataSourceAutoConfiguration                                 │
│  MybatisAutoConfiguration (mybatis-spring-boot-starter)      │
│  CacheAutoConfiguration                                      │
│  DataSourceTransactionManagerAutoConfiguration               │
│  AopAutoConfiguration (@Transactional)                       │
└───────────────────────────┬─────────────────────────────────┘
                            │ 백오프 / MissingBean / after
┌───────────────────────────▼─────────────────────────────────┐
│ NSIGHT (tcf-web / tcf-cache)                                   │
│  TcfPrimaryDataSourceAutoConfiguration  ← DS 복구             │
│  TcfMyBatisAutoConfiguration            ← Factory 복구          │
│  TcfTransactionLog*                     ← 2번째 DS + JDBC 로그 │
│  TcfCache*                              ← JCache 위 Spring Cache│
└─────────────────────────────────────────────────────────────┘
```

NSIGHT AutoConfiguration은 Boot를 **대체**하기보다 **이중 DataSource·MyBatis 공백**을 메우는 **보완 레이어**이다.

---

## 9. 프로퍼티·ConfigurationProperties 바인딩

AutoConfiguration이 `@EnableConfigurationProperties`로 등록하는 타입:

| Properties 클래스 | prefix | 모듈 |
|-------------------|--------|------|
| `TcfProperties` | `nsight.tcf` | tcf-core (바인딩은 tcf-web AutoConfig) |
| `TcfCacheProperties` | `nsight.tcf.cache` | tcf-cache |
| `DataSourceProperties` | `spring.datasource` | Boot (TcfPrimary에서 enable) |
| `MybatisProperties` | `mybatis` | Boot/MyBatis (TcfMyBatis에서 enable) |

로딩 순서: JAR `application.yml` → 모듈 yml → profile yml → JVM (`[24-env-spring-detail.md](24-env-spring-detail.md)`).

---

## 10. 오버라이드·비활성화

### 10.1 `@ConditionalOnMissingBean` — 사용자 빈 우선

업무 모듈에서 동일 이름/타입 빈을 `@Bean`으로 정의하면 NSIGHT AutoConfiguration **해당 빈은 스킵**한다.

| 사용자 정의 | 효과 |
|-------------|------|
| `@Bean SqlSessionFactory` | `TcfMyBatisAutoConfiguration` 스킵 |
| `@Bean(name="dataSource")` | `TcfPrimaryDataSourceAutoConfiguration` 스킵 |

### 10.2 `spring.autoconfigure.exclude`

```yaml
spring:
  autoconfigure:
    exclude:
      - com.nh.nsight.tcf.web.config.TcfMyBatisAutoConfiguration
```

특정 NSIGHT AutoConfiguration만 끌 수 있다. (운영에서는 거의 사용하지 않음)

### 10.3 프로퍼티 off

```yaml
nsight:
  tcf:
    transaction-log-enabled: false    # 거래로그 AutoConfig 전체 off
    cache:
      enabled: false                  # tcf-cache AutoConfig off
```

---

## 11. AutoConfiguration vs `@ComponentScan` (NSIGHT)

```text
tcf-web.jar
├── AutoConfiguration.imports
│     → dataSource, SqlSessionFactory, TransactionLogRepository
└── @Component (com.nh.nsight.tcf.web.*)
      → OnlineTransactionController, GuidMdcCleanupFilter
      → scanBasePackages="com.nh.nsight" 일 때만 등록

tcf-core.jar
└── @Component (com.nh.nsight.tcf.core.*)
      → TCF, STF, ETF, TransactionDispatcher
      → scanBasePackages="com.nh.nsight" 일 때만 등록
```

| 모듈 | scan 범위 | TCF `@Component` | tcf-web AutoConfig |
|------|-----------|------------------|---------------------|
| `sv-service` | `com.nh.nsight` | ● | ● |
| `tcf-om` | `com.nh.nsight` | ● | ● |
| `tcf-batch` | `com.nh.nsight.tcf.batch` only | ✕ | ● (DS·로그) |
| `tcf-ui` | `com.nh.nsight.tcf.ui` only | ✕ | ✕ |

**AutoConfiguration은 scan 범위와 무관** — `tcf-web`이 classpath에만 있으면 imports가 처리된다.

---

## 12. Walkthrough — `sv-service` 기동 시 AutoConfiguration

| 순서 | 이벤트 | 결과 |
|------|--------|------|
| 1 | imports에서 5+2(cache 없음) 클래스 후보 로드 | tcf-web 5개 |
| 2 | `TcfAutoConfiguration` | `TcfProperties` 빈 |
| 3 | `spring.datasource.url` 확인 (sv `application.yml`) | OK |
| 4 | `TcfPrimaryDataSourceAutoConfiguration` | `dataSource` @Primary |
| 5 | `separate=true` → TransactionLog DS | `transactionLogDataSource` |
| 6 | `TcfMyBatisAutoConfiguration` | `SqlSessionFactory` → `@MapperScan` Mapper |
| 7 | `TcfTransactionLogConfiguration` | `JdbcTransactionLogRepository` |
| 8 | `@PostConstruct` on SchemaInitializer | `TCF_TX_LOG` DDL |
| 9 | ComponentScan | `TCF`, Handler, Controller (AutoConfig 아님) |

---

## 13. 디버깅

| 방법 | 확인 내용 |
|------|-----------|
| 기동 로그 `--debug` | `Positive matches` / `Negative matches` (조건별) |
| Actuator `/actuator/conditions` | dev/local에서 AutoConfig 적용·제외 목록 |
| 로그 키워드 | `Transaction log datasource url=`, `TCF EhCache config=`, `HikariPool` |
| 빈 absent | `@ConditionalOnMissingBean` — 다른 설정이 먼저 빈 생성했는지 |

| 증상 | AutoConfig 관점 원인 |
|------|----------------------|
| `SqlSessionFactory` 없음 | MyBatis CP 없음, `dataSource` 없음, 사용자 Factory 이미 있음 |
| `dataSource` 없음 | `spring.datasource.url` 누락, `@ConditionalOnMissingBean` 충돌 |
| 거래로그 INSERT 실패 | `TransactionLogRepository` 빈 없음 (`transaction-log-enabled=false`) |
| Cache 기동 실패 | `JCacheCacheManager` 아님 — `spring.cache.type` 확인 |

---

## 14. 새 AutoConfiguration 추가 가이드

프레임워크 JAR에 기능을 **조건부 자동 등록**하려면:

| 단계 | 작업 |
|------|------|
| 1 | `com.nh.nsight.tcf.{module}.config.XxxAutoConfiguration` 작성 |
| 2 | `@AutoConfiguration(before/after=…)` 로 순서 지정 |
| 3 | `@ConditionalOnClass` / `@ConditionalOnProperty` 로 범위 제한 |
| 4 | `@ConditionalOnMissingBean` 으로 업무 오버라이드 허용 |
| 5 | `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` 에 FQCN 추가 |
| 6 | `@EnableConfigurationProperties` + `application.yml` 기본값 |

**ComponentScan에 넣지 않는다** — 라이브러리 JAR는 AutoConfiguration + `@ComponentScan` 범위를 분리하는 것이 Boot 관례.

업무 WAR 전용 설정은 `@Configuration`을 `com.nh.nsight.marketing.{code}.config`에 두고 scan으로 등록 ([30-springboot.md](30-springboot.md)).

---

## 15. 관련 문서

| 주제 | 문서 |
|------|------|
| 전체 기동 Phase | [30-springboot.md](30-springboot.md) |
| MyBatis·이중 DS | [26-mybatis.md](26-mybatis.md) |
| yml·프로파일 | [20-env-spring.md](20-env-spring.md) |
| 캐시 AutoConfig | [12-cache.md](12-cache.md) |
| 모듈 의존 | [28-tcf-framework-ref.md](28-tcf-framework-ref.md) |

---

## 16. 변경 이력

| 일자 | 변경 내용 |
|------|-----------|
| 2026-06 | 최초 작성 — Spring Boot 3 AutoConfiguration.imports·NSIGHT tcf-web/tcf-cache 처리 |
