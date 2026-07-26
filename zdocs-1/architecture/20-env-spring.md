# 20. Spring 환경 아키텍처

| 항목 | 내용 |
|------|------|
| 문서 번호 | 20 |
| 제목 | Spring Environment Architecture (프로젝트별 Spring 설정) |
| 상위 문서 | [architecture.md](architecture.md) |
| 관련 문서 | [25-env-profile.md](25-env-profile.md), [24-env-spring-detail.md](24-env-spring-detail.md), [16-deploy.md](16-deploy.md), [19-tcf-table.md](19-tcf-table.md), [09-transaction log.md](09-transaction log.md), [10-session.md](10-session.md), [12-cache.md](12-cache.md), [13-batch.md](13-batch.md) |
| 대상 | 프레임워크·업무·운영·인프라 담당자 |
| Spring Boot 기동 | [30-springboot.md](30-springboot.md) |
| AutoConfiguration | [31-autoconfiguration.md](31-autoconfiguration.md) |
| AOP | [32-AOP.md](32-AOP.md) |

---

## 1. 개요

NSIGHT TCF Framework는 **Spring Boot 3.3.5** + **Java 21** 기반 Gradle 멀티 모듈이다.

| 계층 | 모듈 | Spring Boot 앱 | 역할 |
|------|------|----------------|------|
| 유틸 | `tcf-util` | ✕ | Spring 비의존 |
| 엔진 | `tcf-core` | ✕ | `TcfProperties`, STF/TCF/ETF |
| HTTP·DB | `tcf-web` | ✕ (AutoConfiguration) | Controller, Filter, DS, MyBatis, 거래로그 |
| 캐시 | `tcf-cache` | ✕ (AutoConfiguration) | EhCache JCache |
| 운영 | `tcf-om` | ● WAR | OM Admin, UD, Spring Session JDBC |
| 배치 | `tcf-batch` | ● WAR | 대시보드 수집 스케줄 |
| UI | `tcf-ui` | ● WAR | Relay·정적 화면 |
| 업무 | `*-service` ×16 | ● WAR | 온라인 Handler |
| 레거시 | `om-service` | ● (미배포) | 샘플 OM WAR |

의존 방향:

```text
tcf-util → tcf-core → tcf-web → (*-service | tcf-om | tcf-batch)
                              ↘ tcf-cache → tcf-om
```

---

## 2. 공통 기술 스택

| 항목 | 버전·값 |
|------|---------|
| Spring Boot BOM | `3.3.5` (`build.gradle`) |
| Java | `21` (toolchain) |
| MyBatis Spring Boot | `3.0.3` |
| 로컬 DB | H2 (`MODE=Oracle`, `DATABASE_TO_UPPER=false`) |
| WAR 배포 | `providedRuntime spring-boot-starter-tomcat` |
| 외부 Tomcat | `10.1.x` (`ztomcat`) |

컴파일 옵션: `-parameters` (Spring MVC `@PathVariable` 바인딩).

---

## 3. Spring 프로파일

환경은 **`local` · `dev` · `prod`** 세 단계로 통일한다. 상세: [25-env-profile.md](25-env-profile.md).

| 프로파일 | 활성화 시점 | 주요 효과 |
|----------|-------------|-----------|
| `local` | bootRun 기본 (`application.yml`, `tcf-batch` `application-local.yml`) | 모듈별 포트, H2 로컬, UI `deployment-mode: bootrun` |
| `dev` | ztomcat `setenv` — `-Dspring.profiles.active=dev` | 게이트웨이 8080, 파일 로깅, **13 WAR** 배포·Actuator 수집 |
| `prod` | 운영 Tomcat — `-Dspring.profiles.active=prod` | `dev` 설정 + `NSIGHT_GATEWAY_BASE_URL` 등 운영 URL |

`prod`는 프로파일 그룹으로 `dev`를 함께 로드한다 (`tcf-web` `spring.profiles.group.prod`).

### 3.1 프로파일별 설정 파일

| 모듈 | 공통 | local | dev | prod |
|------|------|-------|-----|------|
| `tcf-web` | `application.yml` | — | `application-dev.yml` | `application-prod.yml` |
| `tcf-om` | `application.yml` | — | `application-dev.yml` | `application-prod.yml` |
| `tcf-batch` | `application.yml` | `application-local.yml` | `application-dev.yml` | `application-prod.yml` |
| `tcf-ui` | `application.yml` | — | `application-dev.yml` | `application-prod.yml` |
| 업무 WAR | `application.yml` only | — | — | — (`tcf-web` dev/prod 상속) |

ztomcat 통합 시 활성 프로파일: **`dev`**  
상세 배포: [16-deploy.md](16-deploy.md).

---

## 4. AutoConfiguration (프레임워크 JAR)

### 4.1 `tcf-web` — `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`

상세 처리 원리: [31-autoconfiguration.md](31-autoconfiguration.md)

| 클래스 | 역할 |
|--------|------|
| `TcfAutoConfiguration` | `@EnableConfigurationProperties(TcfProperties)` |
| `TcfPrimaryDataSourceAutoConfiguration` | `dataSource` 빈 (DS 2개일 때 MyBatis 보호) |
| `TcfMyBatisAutoConfiguration` | Primary DS 기준 `SqlSessionFactory` |
| `TcfTransactionLogDataSourceConfiguration` | `transactionLogDataSource` (separate=true 시) |
| `TcfTransactionLogConfiguration` | `TransactionLogRepository`, 스키마 초기화 |

`application.yml` 기본값 (`tcf-web.jar`):

```yaml
nsight:
  tcf:
    transaction-log-datasource:
      url: jdbc:h2:file:${nsight.txlog.path:./data/nsight-txlog}/nsight_om;...
```

### 4.2 `tcf-cache`

| 클래스 | 조건 | 역할 |
|--------|------|------|
| `TcfCacheAutoConfiguration` | `nsight.tcf.cache.enabled=true` (기본) | EhCache config 로드 |
| `TcfCacheSupportAutoConfiguration` | `JCacheCacheManager` 존재 | `@EnableCaching`, `TcfCacheSupport` |

### 4.3 `tcf-core` — EnvironmentPostProcessor

`NsightTxlogPathEnvironmentPostProcessor`:

- `nsight.txlog.path` 미지정 시 프로젝트 루트 `data/nsight-txlog` 자동 해석
- `settings.gradle` 상위 탐색
- 오버라이드: `-Dnsight.txlog.path=...`, 환경변수 `NSIGHT_TXLOG_PATH`

### 4.4 `tcf-web` 공통 빈 (컴포넌트 스캔)

`scanBasePackages = "com.nh.nsight"` 인 앱에서 로드:

| 컴포넌트 | 역할 |
|----------|------|
| `OnlineTransactionController` | `POST /online`, `POST /{code}/online` |
| `GlobalStandardExceptionHandler` | `@RestControllerAdvice` |
| `GuidMdcCleanupFilter` | 요청 종료 시 `MDC.clear()` |

---

## 5. bootRun 공통 JVM·작업 디렉터리

루트 `build.gradle`이 모든 `bootRun`에 적용:

| systemProperty / JVM | 값 | 목적 |
|----------------------|-----|------|
| `workingDir` | 프로젝트 루트 | H2·data 경로 통일 |
| `nsight.txlog.path` | `{root}/data/nsight-txlog` | 거래로그 H2 공유 |
| `file.encoding` | UTF-8 | 한글·전문 인코딩 |

`tcf-batch:bootRun` 추가: `spring.profiles.active=local`.

---

## 6. 프로젝트별 Spring 요약

### 6.1 실행 모듈 매트릭스

| 모듈 | `spring.application.name` | bootRun 포트 | Context (Tomcat) | WAR 파일명 |
|------|---------------------------|--------------|------------------|------------|
| `cc-service` | `nsight-cc-service` | 8081 | `/cc` | `cc.war` |
| `ic-service` | `nsight-ic-service` | 8082 | `/ic` | `ic.war` |
| `pc-service` | `nsight-pc-service` | 8083 | `/pc` | `pc.war` |
| `bc-service` | `nsight-bc-service` | 8084 | `/bc` | `bc.war` |
| `ms-service` | `nsight-ms-service` | 8085 | `/ms` | `ms.war` |
| `sv-service` | `nsight-sv-service` | 8086 | `/sv` | `sv.war` |
| `pd-service` | `nsight-pd-service` | 8087 | `/pd` | `pd.war` |
| `cm-service` | `nsight-cm-service` | 8088 | `/cm` | `cm.war` |
| `eb-service` | `nsight-eb-service` | 8089 | `/eb` | `eb.war` |
| `ep-service` | `nsight-ep-service` | 8090 | `/ep` | `ep.war` |
| `bp-service` | `nsight-bp-service` | 8091 | `/bp` | `bp.war` |
| `bd-service` | `nsight-bd-service` | 8092 | `/bd` | `bd.war` |
| `ss-service` | `nsight-ss-service` | 8093 | `/ss` | `ss.war` |
| `cs-service` | `nsight-cs-service` | 8094 | `/cs` | `cs.war` |
| `ct-service` | `nsight-ct-service` | 8095 | `/ct` | `ct.war` |
| `mg-service` | `nsight-mg-service` | 8096 | `/mg` | `mg.war` |
| `tcf-om` | `nsight-tcf-om` | 8097 | `/om` | `tcf-om.war` → `om.war` |
| `tcf-batch` | `nsight-tcf-batch` | 8098 | `/batch` | `tcf-batch.war` → `batch.war` |
| `tcf-ui` | `nsight-tcf-ui` | 8099 | `/ui` | `tcf-ui.war` → `ui.war` |

### 6.2 Gradle 의존성 (Spring Starter)

| 모듈 | starters·프레임워크 |
|------|---------------------|
| **업무 WAR** | `tcf-core`, `tcf-web`, web, validation, actuator, jdbc, aop, mybatis, h2, tomcat(provided) |
| **tcf-om** | 위 + `tcf-cache`, **spring-session-jdbc**, spring-security-crypto |
| **tcf-batch** | `tcf-core`, `tcf-web`, web, jdbc, actuator, h2, tomcat(provided) — **MyBatis 없음** |
| **tcf-ui** | web, actuator, tomcat(provided) — **TCF JAR 없음** |

---

## 7. 모듈별 상세 Spring 설정

### 7.1 업무 WAR (`*-service`) — 공통 템플릿

16개 모듈은 `application.yml` 구조가 **동일**하며, 포트·`application.name`·H2 mem DB명만 다르다.

**메인 클래스**

```java
@SpringBootApplication(scanBasePackages = "com.nh.nsight")
public class NsightXxServiceApplication extends NsightWarBootstrap { ... }
```

- `NsightWarBootstrap`: 외부 Tomcat WAR 배포용 `SpringBootServletInitializer`
- `com.nh.nsight` 스캔 → `tcf-web` Filter·Controller 자동 등록

**대표 설정 (`sv-service` 기준)**

| 영역 | 키 | 값 |
|------|-----|-----|
| Server | `server.port` | 모듈별 (예: 8086) |
| Server | `server.servlet.session` | cookie, 60m, `JSESSIONID` |
| Spring | `spring.profiles.active` | `local` |
| DS | `spring.datasource.url` | `jdbc:h2:mem:nsight_{code}` |
| DS | `spring.datasource.hikari.auto-commit` | `false` |
| Session | `spring.session.store-type` | `none` |
| TX | `spring.transaction.default-timeout` | `5` (초) |
| MyBatis | `default-statement-timeout` | `3` (초) |
| Actuator | `management.endpoints.web.exposure.include` | health, info, metrics, prometheus, threaddump |

**`nsight.tcf` (업무 공통)**

| 키 | 기본 | 설명 |
|----|------|------|
| `session-validation-enabled` | `false` | STF 세션 검증 |
| `authorization-validation-enabled` | `false` | 권한 검증 |
| `idempotency-enabled` | `true` | 멱등성 |
| `audit-enabled` | `true` | 감사 (OM DB 연동은 OM 전용) |
| `transaction-log-enabled` | `true` | `TCF_TX_LOG` 적재 |
| `transaction-log-schema-auto-init` | `true` (명시 모듈) / 기본 `true` | DDL 자동 생성 |

**거래로그 DS**

- `bc-service`, `sv-service`: `transaction-log-datasource.url` **명시**
- 그 외 14개: `tcf-web.jar` 기본 URL 상속 (`separate: true` 기본)
- 상세: [19-tcf-table.md §2.1](19-tcf-table.md)

**차이 없음**: Cache, Spring Session JDBC, `sql.init`, multipart — 업무 WAR 미사용.

---

### 7.2 `tcf-om` — 운영 WAR

**추가 의존**: `tcf-cache`, `spring-session-jdbc`, `spring-security-crypto`

**전용 `@Configuration`**

| 클래스 | 어노테이션 | 역할 |
|--------|-----------|------|
| `OmSpringSessionConfiguration` | `@EnableJdbcHttpSession` (3600s) | JDBC 세션 |
| `OmSchedulingConfiguration` | `@EnableScheduling` | 세션 정리 스케줄 |
| `OmPasswordConfiguration` | — | BCrypt 비밀번호 |
| `OmUpdownloadConfiguration` | `@EnableConfigurationProperties` | UD 저장 경로 |

**Spring 설정 특이점**

| 영역 | 설정 | 비고 |
|------|------|------|
| Cache | `spring.cache.type: jcache`, `ehcache.xml` | OM Cache Admin |
| DS | `h2:file:.../nsight_om` | Primary = 운영 DB |
| `sql.init` | `schema.sql`, `data.sql` | embedded 모드 |
| Session | `store-type: jdbc`, `initialize-schema: always` | `SPRING_SESSION*` |
| Multipart | max 50MB | UD 업로드 |
| TCF | `idempotency-enabled: false` | OM Admin 중복 허용 |
| TCF | `transaction-log-datasource.separate: false` | Primary와 동일 DS |
| TCF | `transaction-log-schema-auto-init: false` | `schema.sql`에 포함 |
| OM | `nsight.om.batch-service-url` | 배치 호출 URL |
| OM | `nsight.om.session-cleanup.fixed-rate-ms` | 10000 |
| UD | `nsight.updownload.*` | storage-path, max size |

**`dev` 프로파일** (`application-dev.yml`):

```yaml
nsight:
  gateway.base-url: http://127.0.0.1:8080
  om.batch-service-url: http://127.0.0.1:8080/batch
```

---

### 7.3 `tcf-batch` — 수집 배치

**메인 클래스**

```java
@SpringBootApplication
@ConfigurationPropertiesScan
@EnableScheduling
public class NsightTcfBatchApplication extends NsightWarBootstrap
```

| 영역 | 설정 |
|------|------|
| DS | `nsight_om` file (OM과 공유) |
| TCF | `transaction-log-enabled: false` |
| Scheduling | `ApStatusCollectScheduler` 등 4종 `@Scheduled` |
| Profile `local` | `spring.servlet.context-path: /batch` |
| Profile `local` | 소수 모듈 Actuator 타겟 (OM, SV, CC, MG, UI, BATCH) |
| Profile `dev` / `prod` | ztomcat **13 WAR** + bootRun gateway/uj |
| Batch | `nsight.batch.*` — cron, job-id, targets, timeouts |

**전용 빈**: `BatchClientConfiguration` → `RestTemplate` (AP 수집 타임아웃).

---

### 7.4 `tcf-ui` — Relay UI

| 영역 | 설정 |
|------|------|
| Starter | web, actuator only |
| DS / MyBatis / TCF | **없음** |
| `nsight.tcf-ui.deployment-mode` | `bootrun` \| `tomcat` |
| `nsight.tcf-ui.tomcat-gateway-url` | `http://localhost:8080` |
| `nsight.tcf-ui.bootrun-host` | `http://127.0.0.1` |

**메인**: `SpringBootServletInitializer` 직접 상속 (`NsightWarBootstrap` 미사용).  
`main()`에서 `server.port=8099` 고정.

**`dev` / `prod` 프로파일**: `deployment-mode: tomcat` → Relay가 context path URL 사용.

---

### 7.5 `om-service` (레거시)

| 항목 | 값 |
|------|-----|
| 포트 | 8097 (`tcf-om`과 충돌 — 동시 기동 불가) |
| DS | `h2:mem:nsight_om` |
| Session | `none` |
| Spring Session / OM schema | **없음** |
| 배포 | 파이프라인 미포함 — **`tcf-om` 사용** |

---

## 8. `nsight.*` 커스텀 프로퍼티 전체

### 8.1 `nsight.tcf` (`TcfProperties`)

| 프로퍼티 | 기본값 | 설명 |
|----------|--------|------|
| `session-validation-enabled` | `false` | STF 세션 검증 |
| `authorization-validation-enabled` | `false` | STF 권한 검증 |
| `idempotency-enabled` | `true` | 멱등 키 처리 |
| `audit-enabled` | `true` | 감사 로깅 |
| `transaction-log-enabled` | `true` | DB 거래로그 |
| `transaction-log-schema-auto-init` | `true` | `TCF_TX_LOG` DDL |
| `transaction-log-table-name` | `TCF_TX_LOG` | 테이블명 |
| `transaction-log-datasource.separate` | `true` | 별도 DS 여부 |
| `transaction-log-datasource.url` | file H2 template | 거래로그 URL |
| `cache.enabled` | `true` | EhCache (tcf-cache) |
| `cache.config-location` | `classpath:ehcache.xml` | 캐시 XML |

### 8.2 `nsight.timeout`

| 키 | 기본 | 용도 |
|----|------|------|
| `online-transaction-seconds` | `5` | Facade `@Transactional` 참고 |
| `db-query-seconds` | `3` | MyBatis statement timeout |

### 8.3 `nsight.tcf-ui` (`TcfUiProperties`)

| 키 | 기본 |
|----|------|
| `deployment-mode` | `bootrun` |
| `tomcat-gateway-url` | `http://localhost:8080` |
| `bootrun-host` | `http://127.0.0.1` |

### 8.4 `nsight.om` / `nsight.updownload` / `nsight.batch`

- `nsight.om.batch-service-url`, `nsight.om.session-cleanup.*` — `tcf-om` 전용
- `nsight.updownload.storage-path`, `max-file-size-bytes` — UD ([18-fileupdownload.md](18-fileupdownload.md))
- `nsight.batch.ap-status|db-status|session-status|deploy-status.*` — `tcf-batch` 스케줄·타겟
- `nsight.gateway.mode|base-url` — `local` / `dev` / `prod` 프로파일

---

## 9. DataSource·트랜잭션 전략

```text
[업무 WAR]
  dataSource (Primary)     → h2:mem — 업무 SQL
  transactionLogDataSource → h2:file nsight_om — TCF_TX_LOG (separate=true)

[tcf-om]
  dataSource (Primary)     → h2:file nsight_om — OM·UD·세션·거래로그 통합
  (transactionLog separate=false → 동일 DS)

[tcf-batch]
  dataSource               → h2:file nsight_om — 모니터링 테이블만
```

| 모듈 | Hikari `auto-commit` | `spring.transaction.default-timeout` |
|------|----------------------|--------------------------------------|
| 업무 WAR | `false` | 5s |
| `tcf-om` | `true` | 5s |
| `tcf-batch` | (기본) | — |

거래로그 INSERT는 `JdbcTransactionLogRepository`에서 **별도 autocommit** — [09-transaction log.md §6](09-transaction log.md).

---

## 10. 세션·캐시·스케줄링

| 기능 | 적용 모듈 | Spring 설정 |
|------|-----------|-------------|
| **Spring Session JDBC** | `tcf-om` only | `@EnableJdbcHttpSession`, `store-type: jdbc` |
| **Tomcat HTTP Session** | 업무 WAR, `tcf-ui` | cookie, `store-type: none` |
| **EhCache JCache** | `tcf-om` | `spring.cache.type: jcache` + `tcf-cache` |
| **`@EnableScheduling`** | `tcf-om`, `tcf-batch` | 세션 정리, AP/DB/세션/배포 수집 |

---

## 11. WAR 부트스트랩

| 패턴 | 모듈 | 클래스 |
|------|------|--------|
| `NsightWarBootstrap` 상속 | 업무 9, `tcf-om`, `tcf-batch` | `configure(SpringApplicationBuilder)` |
| `SpringBootServletInitializer` 직접 | `tcf-ui` | `configure` 오버라이드 |

외부 Tomcat은 `ztomcat` + `setenv` JVM 옵션으로 프로파일·인코딩·타임존(`Asia/Seoul`) 통일.

---

## 12. 설정 파일 위치 (참조)

> **원문·항목별 해설:** [24-env-spring-detail.md](24-env-spring-detail.md) — 각 `application*.yml` 전체 내용과 프로젝트별 설명.

| 경로 | 모듈 |
|------|------|
| `{module}/src/main/resources/application.yml` | 모든 실행 모듈 |
| `tcf-web/.../application.yml` | TCF 기본 (jar) |
| `tcf-web/.../application-dev.yml` | dev/prod Tomcat 로깅 |
| `tcf-web/.../application-prod.yml` | prod 로그 보관 |
| `tcf-cache/.../application.yml` | 캐시 기본 |
| `tcf-om/.../application-dev.yml` | OM gateway |
| `tcf-om/.../application-prod.yml` | 운영 gateway URL |
| `tcf-batch/.../application-local.yml` | local bootRun 수집 타겟 |
| `tcf-batch/.../application-dev.yml` | 통합 수집 타겟 |
| `tcf-batch/.../application-prod.yml` | 운영 gateway URL |
| `tcf-ui/.../application-dev.yml` | UI Relay dev |
| `tcf-ui/.../application-prod.yml` | UI Relay prod |
| `ztomcat/conf/setenv.sh` | `spring.profiles.active=dev` |
| `ztomcat/conf/setenv.prod.sh` | 운영 Tomcat 샘플 (`prod`, `NSIGHT_GATEWAY_BASE_URL`) |

---

## 13. 환경별 오버라이드 가이드

| 목적 | 방법 |
|------|------|
| 거래로그 DB 경로 | `-Dnsight.txlog.path=...` 또는 `NSIGHT_TXLOG_PATH` |
| 활성 프로파일 | `SPRING_PROFILES_ACTIVE`, `spring.profiles.active` |
| 포트 변경 | `server.port` 또는 `SERVER_PORT` |
| UI Relay 모드 | `nsight.tcf-ui.deployment-mode=tomcat` |
| TCF 기능 토글 | `nsight.tcf.session-validation-enabled` 등 |
| 배치 수집 주기 | `nsight.batch.*.cron` |

운영 환경에서는 `application-{profile}.yml` 또는 외부 config server 대신 **환경변수·JVM 옵션**으로 DS URL·플래그를 주입하는 패턴을 권장한다.

---

## 14. 체크리스트

**로컬 bootRun**

- [ ] 루트에서 `gradle :sv-service:bootRun` — `nsight.txlog.path` 자동 설정 확인
- [ ] `tcf-om` + 업무 WAR 동시 기동 시 H2 `AUTO_SERVER=TRUE` 공유 확인
- [ ] `om-service`와 `tcf-om` **동시 기동 금지** (포트 8097)

**dev (ztomcat)**

- [ ] `setenv`에 `spring.profiles.active=dev` 프로파일 적용
- [ ] `tcf-ui` `deployment-mode=tomcat`, `tcf-batch` startup-collect delay 확인
- [ ] `tcf-web` dev yml 로그 경로 `${CATALINA_BASE}/logs/`

**prod (운영 Tomcat)**

- [ ] `setenv.prod.*` 참고해 `NSIGHT_GATEWAY_BASE_URL`·DB 환경변수 설정
- [ ] `-Dspring.profiles.active=prod` → 활성 프로파일 `prod`, `dev` (그룹)
- [ ] `tcf-ui` `tomcat-gateway-url` = 공개 URL

**신규 업무 WAR**

- [ ] `application.yml` 템플릿 복제 (포트·mem DB명·pool-name)
- [ ] `@SpringBootApplication(scanBasePackages = "com.nh.nsight")`
- [ ] `NsightWarBootstrap` 상속
- [ ] `transaction-log-datasource` URL 공유 file DB 유지

---

## 15. 참고 소스

| # | 경로 |
|---|------|
| 1 | `build.gradle` — BOM, bootRun, businessModules |
| 2 | `tcf-core/.../TcfProperties.java` |
| 3 | `tcf-core/.../NsightTxlogPathEnvironmentPostProcessor.java` |
| 4 | `tcf-web/.../META-INF/spring/*.imports` |
| 5 | `tcf-web/.../TcfPrimaryDataSourceAutoConfiguration.java` |
| 6 | `tcf-web/.../boot/NsightWarBootstrap.java` |
| 7 | `tcf-om/src/main/resources/application.yml` |
| 8 | `tcf-batch/src/main/resources/application-local.yml` |
| 9 | `docs/architecture/25-env-profile.md` |
| 10 | `ztomcat/conf/setenv.sh` |
| 11 | `ztomcat/conf/setenv.prod.sh` |
