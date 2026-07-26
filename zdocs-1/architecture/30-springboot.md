# 30. Spring Boot 기동 원리

| 항목 | 내용 |
|------|------|
| 문서 번호 | 30 |
| 제목 | Spring Boot Startup Architecture |
| 상위 문서 | [architecture.md](architecture.md) |
| 관련 문서 | [20-env-spring.md](20-env-spring.md), [24-env-spring-detail.md](24-env-spring-detail.md), [25-env-profile.md](25-env-profile.md), [28-tcf-framework-ref.md](28-tcf-framework-ref.md), [29-facade.md](29-facade.md) |
| 대상 | 프레임워크·업무·운영 개발자 |

---

## 1. 개요

NSIGHT TCF는 **Spring Boot 3.3.5** + **Java 21**로 구성된 Gradle 멀티 모듈이다.  
Spring Boot **애플리케이션(실행 WAR)** 은 19개이고, 나머지 JAR(`tcf-core`, `tcf-web`, `tcf-cache`, `tcf-util`)은 **라이브러리 + AutoConfiguration** 으로 기동 과정에 합류한다.

| 구분 | 모듈 | Spring Boot `main` / WAR |
|------|------|--------------------------|
| 라이브러리 | `tcf-util`, `tcf-core`, `tcf-web`, `tcf-cache` | ✕ — 의존 JAR로 classpath 합류 |
| 실행 WAR | `*-service` ×16, `tcf-om`, `tcf-batch`, `tcf-ui` | ● — `@SpringBootApplication` |
| 비-Gradle | `tcf-cicd`, `tcf-scripts` | ✕ — 설정·스크립트만 |

**기동이란:** JVM 프로세스(또는 Tomcat 내 WAR)가 시작되어 **ApplicationContext**를 만들고, **빈(Bean) 그래프**를 완성한 뒤 **HTTP 요청을 받을 준비**가 되는 것까지의 과정이다.

환경 yml·프로파일·포트 매트릭스: [20-env-spring.md](20-env-spring.md)  
요청 처리(기동 **이후**): [29-facade.md](29-facade.md)

---

## 2. 기동 방식 두 가지

### 2.1 bootRun (개발 PC)

```bash
gradle :sv-service:bootRun
# 또는 tcf-scripts/run-local.bat sv
```

```text
NsightSvServiceApplication.main()
  → SpringApplication.run(...)
  → Embedded Tomcat 기동 (내장 WAS)
  → 단일 JVM, 단일 포트 (예: 8086)
```

루트 `build.gradle`이 모든 `bootRun`에 공통 JVM·작업 디렉터리를 주입한다.

```56:70:build.gradle
def nsightTxlogPath = layout.projectDirectory.dir('data/nsight-txlog').asFile.absolutePath.replace('\\', '/')

subprojects { sub ->
    sub.plugins.withId('org.springframework.boot') {
        sub.tasks.withType(org.springframework.boot.gradle.tasks.run.BootRun).configureEach {
            workingDir = rootProject.projectDir
            systemProperty 'nsight.txlog.path', nsightTxlogPath
            systemProperty 'spring.profiles.active', 'local'
            systemProperty 'file.encoding', 'UTF-8'
            // ...
        }
    }
}
```

### 2.2 WAR + 외부 Tomcat (ztomcat / 운영)

```text
Tomcat catalina.sh start
  → WAR deploy (예: sv.war → context /sv)
  → SpringBootServletInitializer.configure()
  → 동일 ApplicationContext — Embedded Tomcat 없음
  → spring.profiles.active=dev (setenv)
```

업무 WAR는 `NsightWarBootstrap`을 상속한다.

```9:19:tcf-web/src/main/java/com/nh/nsight/tcf/web/boot/NsightWarBootstrap.java
public abstract class NsightWarBootstrap extends SpringBootServletInitializer {
    private final Class<?> source;

    protected NsightWarBootstrap(Class<?> source) {
        this.source = source;
    }

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
        return application.sources(source);
    }
}
```

| 항목 | bootRun | WAR (ztomcat) |
|------|---------|---------------|
| Tomcat | Embedded (Boot) | 외부 Tomcat 10.1 |
| `spring-boot-starter-tomcat` | runtime classpath | `providedRuntime` |
| 프로파일 | `local` (Gradle 강제) | `dev` / `prod` (setenv) |
| 포트 | 모듈별 8081~8099 | 게이트웨이 8080 |
| workingDir | 프로젝트 루트 | Tomcat `CATALINA_BASE` |

---

## 3. 기동 타임라인 (전체)

```text
┌─────────────────────────────────────────────────────────────────┐
│ Phase 0  JVM 시작 — main() 또는 Tomcat WAR 로더                  │
├─────────────────────────────────────────────────────────────────┤
│ Phase 1  Environment 준비 (ApplicationContext 생성 **전**)       │
│   · EnvironmentPostProcessor                                    │
│   · application.yml / application-{profile}.yml 로드             │
├─────────────────────────────────────────────────────────────────┤
│ Phase 2  ApplicationContext refresh                              │
│   · @SpringBootApplication 메타 처리                             │
│   · AutoConfiguration.imports (tcf-web, tcf-cache)              │
│   · @ComponentScan ("com.nh.nsight")                            │
│   · @MapperScan (업무·tcf-om)                                    │
│   · @Configuration (@EnableJdbcHttpSession 등)                  │
├─────────────────────────────────────────────────────────────────┤
│ Phase 3  빈 생성·의존성 주입 (BeanFactory)                        │
│   · DataSource → SqlSessionFactory → Mapper proxy               │
│   · TCF / STF / ETF / TransactionDispatcher                     │
│   · Handler 40+ → handlerMap 등록 (Dispatcher 생성자)            │
├─────────────────────────────────────────────────────────────────┤
│ Phase 4  초기화 콜백                                              │
│   · @PostConstruct (TransactionLogSchemaInitializer 등)          │
│   · ApplicationRunner (@Order 순)                               │
│   · spring.sql.init (tcf-om schema.sql / data.sql)              │
├─────────────────────────────────────────────────────────────────┤
│ Phase 5  Web 서버 Ready                                          │
│   · Embedded Tomcat start (bootRun)                             │
│   · Filter 등록 (GuidMdcCleanupFilter)                          │
│   · DispatcherServlet 매핑                                       │
│   · Actuator endpoints                                          │
├─────────────────────────────────────────────────────────────────┤
│ Phase 6  런타임 — HTTP 요청 → [29-facade.md](29-facade.md)       │
└─────────────────────────────────────────────────────────────────┘
```

---

## 4. Phase 1 — Environment (Context 이전)

ApplicationContext가 만들어지기 **전에** 실행되는 확장 지점이다.

### 4.1 `EnvironmentPostProcessor`

| 파일 | `tcf-core/.../NsightTxlogPathEnvironmentPostProcessor.java` |
|------|-------------------------------------------------------------|
| 등록 | `META-INF/spring/org.springframework.boot.env.EnvironmentPostProcessor` |

```24:32:tcf-core/src/main/java/com/nh/nsight/tcf/core/config/NsightTxlogPathEnvironmentPostProcessor.java
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        if (hasExplicitPath(environment)) {
            return;
        }
        Path txlogDir = resolveTxlogDir();
        Map<String, Object> map = new HashMap<>();
        map.put(PROPERTY, txlogDir.toString().replace('\\', '/'));
        environment.getPropertySources().addFirst(new MapPropertySource("nsightTxlogPathAuto", map));
```

**역할:** `nsight.txlog.path`를 프로젝트 루트 `data/nsight-txlog`로 자동 설정.  
`settings.gradle`이 있는 디렉터리를 상위로 탐색 — bootRun·IDE·Tomcat **실행 위치가 달라도** H2 파일 DB 경로를 통일한다.

이후 yml의 `${nsight.txlog.path}` placeholder가 해석된다.

### 4.2 설정 파일 로딩 순서

Spring Boot 3 기본 우선순위 (낮음 → 높음):

```text
1. tcf-web.jar / tcf-cache.jar 내부 application.yml
2. {실행 모듈}/application.yml
3. application-{profile}.yml  (local / dev / prod)
4. JVM -DsystemProperty
5. OS 환경변수
6. EnvironmentPostProcessor가 addFirst한 PropertySource  (nsight.txlog.path)
```

예: `tcf-om` bootRun 시 `application-local.yml`이 datasource URL·Spring Session·sql.init을 덮어쓴다.

상세 yml 원문: [24-env-spring-detail.md](24-env-spring-detail.md)

### 4.3 프로파일

| 실행 | 활성 프로파일 | 효과 |
|------|---------------|------|
| bootRun (Gradle) | `local` | `-Dspring.profiles.active=local` |
| ztomcat | `dev` | setenv |
| 운영 | `prod` | `spring.profiles.group.prod` → `dev` 포함 |

```1:7:tcf-web/src/main/resources/application.yml
spring:
  profiles:
    default: local
    group:
      prod:
        - dev
```

---

## 5. Phase 2 — `@SpringBootApplication`과 스캔 범위

### 5.1 메인 클래스 (업무 WAR 표준)

```8:10:sv-service/src/main/java/com/nh/nsight/marketing/sv/NsightSvServiceApplication.java
@SpringBootApplication(scanBasePackages = "com.nh.nsight")
@MapperScan("com.nh.nsight.marketing.sv.mapper")
public class NsightSvServiceApplication extends NsightWarBootstrap {
```

`@SpringBootApplication` = `@Configuration` + `@EnableAutoConfiguration` + `@ComponentScan`.

| 어노테이션 | NSIGHT에서의 효과 |
|------------|-------------------|
| `@EnableAutoConfiguration` | classpath의 `AutoConfiguration.imports` 로드 |
| `@ComponentScan("com.nh.nsight")` | **프레임워크 + 업무** 빈 한 번에 스캔 |
| `@MapperScan(...)` | MyBatis Mapper 인터페이스 → `MapperFactoryBean` |

**`scanBasePackages = "com.nh.nsight"`가 핵심** — 이 한 줄로 `tcf-core`의 `TCF`, `tcf-web`의 `OnlineTransactionController`, 업무 `Handler`가 **같은 ApplicationContext**에 등록된다.

### 5.2 스캔으로 등록되는 프레임워크 빈 (tcf-core)

| 클래스 | 스테레오타입 | 기동 시 동작 |
|--------|-------------|--------------|
| `TCF` | `@Component` | STF·Dispatcher·ETF 주입 |
| `STF` | `@Component` | Header 검증·로그 SPI |
| `ETF` | `@Component` | 응답·감사·메트릭 |
| `TransactionDispatcher` | `@Component` | **생성자에서 모든 Handler 등록** |
| `StandardHeaderValidator` 등 | `@Component` | STF 의존 |

```22:39:tcf-core/src/main/java/com/nh/nsight/tcf/core/dispatch/TransactionDispatcher.java
    public TransactionDispatcher(List<TransactionHandler> handlers) {
        for (TransactionHandler handler : handlers) {
            Collection<String> serviceIds = handler.serviceIds();   // 도메인당 1개 핸들러가 여러 serviceId 담당
            if (serviceIds == null || serviceIds.isEmpty()) {
                log.warn("Handler declares no serviceId, skipped: {}", handler.getClass().getName());
                continue;
            }
            for (String serviceId : serviceIds) {
                TransactionHandler previous = handlerMap.put(serviceId, handler);
                if (previous != null) {
                    throw new IllegalStateException("Duplicate serviceId detected: " + serviceId);
                }
                log.info("Registered NSIGHT handler. serviceId={} handler={}",
                        serviceId, handler.getClass().getSimpleName());
            }
        }
    }
```

기동 로그에 `Registered NSIGHT handler. serviceId=SV.Sample.inquiry`가 보이면 **Dispatcher 준비 완료**이다.  
serviceId 중복은 **기동 실패** — 첫 HTTP 요청 전에 발견된다.

### 5.3 스캔으로 등록되는 Web 빈 (tcf-web)

| 클래스 | 역할 |
|--------|------|
| `OnlineTransactionController` | `POST /online` |
| `GuidMdcCleanupFilter` | `OncePerRequestFilter` — 요청 종료 MDC 정리 |
| `GlobalStandardExceptionHandler` | `@RestControllerAdvice` |
| `TcfGateway` | 비표준 REST 진입 |

---

## 6. Phase 2 — AutoConfiguration (JAR 자동 설정)

상세: [31-autoconfiguration.md](31-autoconfiguration.md)

Spring Boot 3는 `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` 파일로 자동 설정 클래스를 찾는다.  
**컴포넌트 스캔과 별도**로 classpath JAR에서 로드된다.

### 6.1 `tcf-web` AutoConfiguration

```1:5:tcf-web/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
com.nh.nsight.tcf.web.config.TcfAutoConfiguration
com.nh.nsight.tcf.web.config.TcfPrimaryDataSourceAutoConfiguration
com.nh.nsight.tcf.web.config.TcfMyBatisAutoConfiguration
com.nh.nsight.tcf.web.config.TcfTransactionLogDataSourceConfiguration
com.nh.nsight.tcf.web.config.TcfTransactionLogConfiguration
```

| 클래스 | 조건 | 생성 빈 |
|--------|------|---------|
| `TcfAutoConfiguration` | 항상 | `TcfProperties` 바인딩 |
| `TcfPrimaryDataSourceAutoConfiguration` | `spring.datasource.url` 존재, `dataSource` 빈 없음 | `@Primary dataSource` |
| `TcfMyBatisAutoConfiguration` | `DataSource` 존재, `SqlSessionFactory` 없음 | `SqlSessionFactory`, `SqlSessionTemplate` |
| `TcfTransactionLogDataSourceConfiguration` | `transaction-log-enabled=true` | `transactionLogDataSource` (separate=true) |
| `TcfTransactionLogConfiguration` | 위와 동일 | `TransactionLogRepository`, `TransactionLogSchemaInitializer` |

**왜 Primary DS AutoConfig가 필요한가?**

거래로그용 `transactionLogDataSource`를 추가하면 Spring Boot 기본 `DataSourceAutoConfiguration`이 꼬일 수 있다.  
업무 MyBatis는 **`@Primary dataSource`** 에만 붙도록 `TcfPrimaryDataSourceAutoConfiguration`이 보호한다.

```19:31:tcf-web/src/main/java/com/nh/nsight/tcf/web/config/TcfPrimaryDataSourceAutoConfiguration.java
@AutoConfiguration(before = TcfMyBatisAutoConfiguration.class)
@ConditionalOnProperty(prefix = "spring.datasource", name = "url")
@ConditionalOnMissingBean(name = "dataSource")
public class TcfPrimaryDataSourceAutoConfiguration {

    @Bean(name = "dataSource")
    @Primary
    public DataSource dataSource(DataSourceProperties properties) {
        return properties.initializeDataSourceBuilder().build();
    }
}
```

### 6.2 `tcf-cache` AutoConfiguration (tcf-om)

```text
TcfCacheAutoConfiguration       → EhCache config-location 로드
TcfCacheSupportAutoConfiguration → @EnableCaching, TcfCacheSupport
```

조건: `nsight.tcf.cache.enabled=true` (기본값 true).  
`tcf-ui`·업무 WAR는 `tcf-cache` 의존 없음 → 캐시 AutoConfig **미로드**.

### 6.3 `@ConditionalOn*` — 빈이 “조건부”로 생기는 이유

AutoConfiguration·`@Configuration` 클래스는 프로퍼티·classpath·다른 빈 존재 여부에 따라 **스킵**될 수 있다.

| 예 | 조건 |
|----|------|
| 거래로그 DS 분리 | `nsight.tcf.transaction-log-datasource.separate=true` |
| MyBatis Factory | `SqlSessionFactory` 빈이 아직 없을 때만 |
| tcf-batch | `@MapperScan` 없음 → MyBatis 빈 없음 (정상) |

---

## 7. Phase 3 — 인프라 빈 생성 순서

의존성 방향에 따라 대략 다음 순서로 빈이 만들어진다.

```text
TcfProperties (ConfigurationProperties 바인딩)
    ↓
dataSource (HikariCP — spring.datasource.*)
transactionLogDataSource (separate=true 시, nsight.tcf.*)
    ↓
JdbcTemplate / transactionLogJdbcTemplate
    ↓
SqlSessionFactory (@Primary dataSource)
    ↓
Mapper 인터페이스 프록시 (@MapperScan)
    ↓
DAO (@Repository) → Service → Facade
    ↓
Handler (@Component) → TransactionDispatcher 생성
    ↓
TCF (@Component) — STF, Dispatcher, ETF 조립
    ↓
OnlineTransactionController — TCF 주입
```

### 7.1 DataSource · H2

| 모듈 | 업무 DB (`dataSource`) | 거래로그 |
|------|------------------------|----------|
| `sv-service` bootRun | `jdbc:h2:mem:nsight_sv` | 별도 H2 파일 (`nsight_om`) |
| `tcf-om` local | `jdbc:h2:file:.../nsight_om` | **same DB** (`separate: false`) |

`tcf-om` local은 업무·거래로그·OM 메타·배치 상태가 **한 H2 파일**을 공유한다.

### 7.2 MyBatis

1. `@MapperScan`이 Mapper 인터페이스 스캔
2. `TcfMyBatisAutoConfiguration`이 `SqlSessionFactory` 생성 (`mapper/**/*.xml`)
3. DAO가 Mapper를 주입받아 SQL 호출

상세: [26-mybatis.md](26-mybatis.md)

### 7.3 AOP · `@Transactional`

`spring-boot-starter-aop` + `@EnableAutoConfiguration`으로 **트랜잭션 AOP**가 활성화된다.  
Facade의 `@Transactional`은 **프록시 빈**을 통해 DB 커넥션·commit/rollback을 제어한다.  
Service에는 `@Transactional`을 두지 않는 것이 NSIGHT 관례 ([29-facade.md](29-facade.md)).  
AOP 상세: [32-AOP.md](32-AOP.md)

---

## 8. Phase 4 — 기동 후 초기화 콜백

ApplicationContext refresh가 끝난 뒤, **HTTP accept 전**에 실행되는 작업이다.

### 8.1 `@PostConstruct`

```51:58:tcf-web/src/main/java/com/nh/nsight/tcf/web/logging/TransactionLogSchemaInitializer.java
    @PostConstruct
    public void init() {
        String tableName = JdbcTransactionLogRepository.validateTableName(properties.getTransactionLogTableName());
        jdbcTemplate.execute(CREATE_TABLE_TEMPLATE.formatted(tableName));
        // 인덱스 생성 ...
        log.info("Transaction log table ready: {}", tableName);
    }
```

`TCF_TX_LOG` 테이블이 없으면 H2에 자동 생성 (`transaction-log-schema-auto-init=true`).

### 8.2 `spring.sql.init` (tcf-om)

`application-local.yml`:

```yaml
spring.sql.init:
  mode: embedded
  schema-locations: classpath:schema.sql
  data-locations: classpath:data.sql
```

DataSource 준비 직후 **OM 스키마·시드 데이터**를 JDBC로 적재한다.

### 8.3 `ApplicationRunner` (@Order)

| Runner | 모듈 | Order | 역할 |
|--------|------|-------|------|
| `OmDatabaseMigration` | tcf-om | 0 | MERGE 시드, 카탈로그·메뉴·공통코드 보정 |
| `OmUserPasswordInitializer` | tcf-om | — | 초기 사용자 비밀번호 |
| `BatchDatabaseMigration` | tcf-batch | — | 배치 테이블 |
| `DashboardCollectStartupRunner` | tcf-batch | 50 | 기동 직후 AP/DB/세션/배포 1회 수집 |

```13:15:tcf-om/src/main/java/com/nh/nsight/marketing/om/support/OmDatabaseMigration.java
@Component
@Order(0)
public class OmDatabaseMigration implements ApplicationRunner {
```

**기동 완료 ≠ 데이터 준비 완료** — OM Admin을 쓰려면 `OmDatabaseMigration.run()`까지 끝나야 카탈로그·메뉴가 최신이다.

### 8.4 `@EnableScheduling` / `@EnableJdbcHttpSession` (tcf-om)

| 설정 클래스 | 효과 |
|-------------|------|
| `OmSpringSessionConfiguration` | `@EnableJdbcHttpSession` — `SPRING_SESSION` 테이블 기반 HTTP 세션 |
| `OmSchedulingConfiguration` | OM 내부 스케줄 (세션 정리 등) |
| `NsightTcfBatchApplication` | `@EnableScheduling` — 5분 cron 수집 Job |

이들은 **Phase 2**에서 `@Configuration`으로 파싱되고, 스케줄러·세션 필터는 Phase 5 직전에 등록된다.

---

## 9. Phase 5 — Web 서버 Ready

### 9.1 Embedded Tomcat (bootRun)

Spring Boot `WebServerStartStopLifecycle`이 Tomcat을 기동한다.

| 설정 | 출처 | 예 (sv-service) |
|------|------|-----------------|
| `server.port` | `application.yml` | 8086 |
| `server.servlet.context-path` | dev yml / Tomcat | bootRun `/`, ztomcat `/sv` |
| `DispatcherServlet` | Boot auto | `/` 매핑 |
| Actuator | `management.*` | `/actuator/health` |

### 9.2 Servlet Filter 체인

HTTP 요청은 대략 다음 순서를 탄다.

```text
Tomcat Connector
  → GuidMdcCleanupFilter (OncePerRequestFilter)
  → Spring Session Filter (tcf-om only)
  → DispatcherServlet
  → OnlineTransactionController.handle(...)
  → TCF.process(...)   ← [29-facade.md](29-facade.md)
  → GuidMdcCleanupFilter.finally: MDC.clear()
  → TCF.process.finally: TransactionContextHolder.clear(), MDC.clear()
```

MDC는 **이중 정리** — Filter와 TCF 모두에서 clear.

### 9.3 기동 완료 신호

| 신호 | 의미 |
|------|------|
| `Started NsightSvServiceApplication in X seconds` | Boot 기동 완료 |
| `Registered NSIGHT handler. serviceId=...` | Handler 맵 등록 |
| `Transaction log table ready` | 거래로그 DDL 완료 |
| `Tomcat started on port(s): 8086` | HTTP 수신 가능 |
| Actuator `GET /actuator/health` → UP | 헬스체크 통과 |

---

## 10. 모듈별 기동 차이

```text
                    ┌─────────────┐
                    │  tcf-core   │  TCF/STF/ETF/Dispatcher (스캔)
                    │  tcf-web    │  AutoConfig + Controller/Filter
                    └──────┬──────┘
           ┌───────────────┼───────────────┐
           ▼               ▼               ▼
    *-service WAR     tcf-om WAR      tcf-batch WAR
    + MyBatis         + tcf-cache     + @EnableScheduling
    + Handler×N       + Spring Session + ApplicationRunner
                      + OmDatabaseMigration   (수집 Job)
           │
           ▼
      tcf-ui WAR  (tcf-* JAR 없음 — Relay·정적 파일만)
```

| 모듈 | scanBasePackages | tcf-web | tcf-cache | MyBatis | Session JDBC | 기동 Runner |
|------|------------------|---------|-----------|---------|--------------|-------------|
| `sv-service` | `com.nh.nsight` | ● | ✕ | ● | ✕ | ✕ |
| `tcf-om` | `com.nh.nsight` | ● | ● | ● | ● | OmDatabaseMigration |
| `tcf-batch` | `com.nh.nsight.tcf.batch` (기본) | AutoConfig만 ● | ✕ | ✕ | ✕ | Batch + Dashboard collect |
| `tcf-ui` | `com.nh.nsight.tcf.ui` (기본) | ✕ | ✕ | ✕ | ✕ | ✕ |

`*-service`·`tcf-om`은 `scanBasePackages = "com.nh.nsight"`로 **프레임워크 빈(TCF, Controller)까지** 스캔한다.  
`tcf-batch`·`tcf-ui`는 **자기 패키지만** 스캔 — `tcf-web` AutoConfiguration(DataSource 등)은 classpath로 로드되지만 `OnlineTransactionController`·`TCF` `@Component`는 **등록되지 않는다** (배치·UI에 불필요).

---

## 11. Walkthrough — `gradle :sv-service:bootRun` 한 줄씩

| 순서 | 무엇이 | 어디서 |
|------|--------|--------|
| 1 | JVM 시작, workingDir=프로젝트 루트 | Gradle BootRun |
| 2 | `nsight.txlog.path`, `spring.profiles.active=local` | Gradle systemProperty |
| 3 | `NsightTxlogPathEnvironmentPostProcessor` | `data/nsight-txlog` 설정 |
| 4 | yml 로드: tcf-web.jar → sv-service → (local 없으면 default) | Environment |
| 5 | AutoConfiguration: Tcf*, DataSource, MyBatis, TransactionLog | imports |
| 6 | ComponentScan: TCF, Controller, SvSampleHandler, … | `com.nh.nsight` |
| 7 | `@MapperScan`: SvSampleMapper 프록시 | sv Application |
| 8 | `TransactionDispatcher` — Handler 목록 등록 | tcf-core |
| 9 | `TransactionLogSchemaInitializer.init()` | @PostConstruct |
| 10 | Tomcat :8086 listen | Boot Web |
| 11 | `Started NsightSvServiceApplication` | 완료 |

이후 `POST /sv/online` → [29-facade.md](29-facade.md) 흐름.

---

## 12. Walkthrough — ztomcat WAR 배포

```text
1. tcf-cicd/scripts/sync-to-framework.ps1 -Profile dev
2. gradle buildZtomcatWars
3. tcf-cicd/local/ztomcat/deploy-restart.ps1
4. Tomcat setenv: -Dspring.profiles.active=dev
5. 각 WAR (sv.war, om.war, …) 별도 ApplicationContext
6. 동일 nsight.txlog.path (H2 TCP 9092 또는 file 공유)
7. 게이트웨이 http://localhost:8080/sv/online
```

WAR마다 **독립 Spring Context** — Handler·Dispatcher는 WAR 내부에만 존재한다.  
`tcf-ui` Relay는 dev yml의 gateway URL로 각 context-path를 조합한다.

---

## 13. 기동 실패·디버깅

| 증상 | 원인·확인 |
|------|-----------|
| `Duplicate serviceId detected` | 서로 다른 Handler의 `serviceIds()`에 같은 serviceId 중복 |
| `Failed to configure a DataSource` | `spring.datasource.url` 누락 |
| `SqlSessionFactory` 없음 | DS 빈 없음, `@MapperScan` 패키지 오타 |
| H2 file lock | 동일 파일에 mem+file 혼용, Tomcat 중복 기동 |
| OM Admin 빈 화면 | `OmDatabaseMigration` 실패 — 로그에서 SQL 오류 |
| Handler 404 (SERVICE_NOT_FOUND) | **기동은 됐으나** serviceId 미등록 — WAR 재빌드·스캔 패키지 |
| MyBatis XML not found | `mapper-locations`, xml namespace ≠ interface FQCN |

**유용한 로그 키워드:** `Registered NSIGHT handler`, `AutoConfiguration`, `HikariPool`, `Started`, `ApplicationRunner`.

**Actuator:** `GET /actuator/conditions` — 어떤 AutoConfig가 적용/제외됐는지 (dev/local).

---

## 14. Spring Boot vs 순수 Spring (NSIGHT 관점)

| 개념 | NSIGHT에서의 구현 |
|------|-------------------|
| `@Configuration` | `OmSpringSessionConfiguration`, `TcfTransactionLogConfiguration` |
| `@Bean` | AutoConfiguration 내부 DataSource, SqlSessionFactory |
| `@Component` | TCF, Handler, Rule, DAO(Repository) |
| `@Autowired` / 생성자 주입 | 전 모듈 — 생성자 주입 권장 |
| `ApplicationContext` | WAR·bootRun당 1개 |
| Starter | `spring-boot-starter-web`, `jdbc`, `aop`, `actuator` |
| BOM | `spring-boot-dependencies:3.3.5` (`build.gradle`) |

프레임워크 JAR(`tcf-web`)는 **실행 main 없이** AutoConfiguration + `@Component`만 제공 — 업무 WAR가 `implementation project(':tcf-web')`로 가져와 기동 시 합류한다.

---

## 15. 관련 문서

| 주제 | 문서 |
|------|------|
| yml·프로파일·포트 매트릭스 | [20-env-spring.md](20-env-spring.md) |
| yml 원문 상세 | [24-env-spring-detail.md](24-env-spring-detail.md) |
| 프로파일·sync | [25-env-profile.md](25-env-profile.md) |
| 모듈·의존 | [28-tcf-framework-ref.md](28-tcf-framework-ref.md) |
| 기동 후 HTTP 처리 | [29-facade.md](29-facade.md) |
| MyBatis AutoConfig | [26-mybatis.md](26-mybatis.md) |
| Tomcat 배포 | [16-deploy.md](16-deploy.md) |

---

## 16. 변경 이력

| 일자 | 변경 내용 |
|------|-----------|
| 2026-06 | 최초 작성 — Spring Boot 3.3.5 기동 단계·AutoConfig·NSIGHT 모듈별 차이 |
