# 24. Spring 설정 파일 상세 (프로젝트별)

| 항목 | 내용 |
|------|------|
| 문서 번호 | 24 |
| 제목 | Spring Configuration Files Detail |
| 상위 문서 | [architecture.md](architecture.md) |
| 관련 문서 | [20-env-spring.md](20-env-spring.md), [21-env-tomcat.md](21-env-tomcat.md), [19-tcf-table.md](19-tcf-table.md) |
| 대상 | 프레임워크·업무·운영 담당자 |

---

## 1. 문서 목적

[20-env-spring.md](20-env-spring.md)가 개념·매트릭스 중심이라면, 본 문서는 **저장소에 있는 `application*.yml` 원문**을 그대로 제시하고 항목별 의미를 설명한다.

환경 프로파일(`local` / `dev` / `prod`) 정의: [25-env-profile.md](25-env-profile.md).

설정 로딩 순서 (Spring Boot 3):

```text
1. tcf-web.jar / tcf-cache.jar 내부 application.yml  (의존 JAR, 낮은 우선순위)
2. {모듈}/src/main/resources/application.yml
3. application-{profile}.yml  (활성 프로파일일 때)
4. JVM -D / 환경변수  (최종 오버라이드)
```

프로파일 활성화 요약:

| 실행 방식 | 활성 프로파일 | 비고 |
|-----------|---------------|------|
| 업무·`tcf-om` bootRun | `local` | yml `spring.profiles.active` |
| `tcf-batch` bootRun | `local` | `application-local.yml`, `build.gradle` |
| ztomcat WAR | `dev` | `ztomcat/conf/setenv.sh` |
| 운영 Tomcat | `prod` | `dev` 프로파일 그룹 포함 |

---

## 2. 프레임워크 JAR (실행 모듈 아님)

### 2.1 `tcf-web` — 거래로그 DataSource 기본값

**경로:** `tcf-web/src/main/resources/application.yml`

```yaml
# TCF framework defaults (tcf-web.jar)
nsight:
  tcf:
    transaction-log-datasource:
      url: jdbc:h2:file:${nsight.txlog.path:./data/nsight-txlog}/nsight_om;MODE=Oracle;AUTO_SERVER=TRUE;DATABASE_TO_UPPER=false
      username: sa
      password:
      driver-class-name: org.h2.Driver
```

| 키 | 설명 |
|----|------|
| `nsight.tcf.transaction-log-datasource.url` | 업무 WAR가 별도 URL을 쓰지 않을 때 **공유 file H2** (`nsight_om`) |
| `${nsight.txlog.path:...}` | Gradle bootRun·`setenv`가 주입; 미지정 시 `./data/nsight-txlog` |
| `AUTO_SERVER=TRUE` | 다중 프로세스(bootRun·Tomcat)가 동일 file DB 동시 접근 |

`TcfTransactionLogDataSourceConfiguration`이 `separate: true`(기본)일 때 이 URL로 **두 번째 DataSource**를 만든다.

---

### 2.2 `tcf-web` — Tomcat 프로파일 로깅 (`dev`)

**경로:** `tcf-web/src/main/resources/application-dev.yml`

```yaml
# ztomcat(8080) WAR 공통 — tcf-web.jar (dev 프로파일)
logging:
  charset:
    file: UTF-8
  file:
    name: ${CATALINA_BASE:./logs}/logs/nsight-${spring.application.name}.log
  logback:
    rollingpolicy:
      max-file-size: 50MB
      max-history: 14
```

| 키 | 설명 |
|----|------|
| `logging.file.name` | WAR별 롤링 파일 (`nsight-nsight-sv-service.log` 등) |
| `${CATALINA_BASE}` | ztomcat 기동 시 Tomcat 로그 디렉터리 |

**적용 대상:** `tcf-web`을 의존하는 모든 WAR (업무 9 + `tcf-om` + `tcf-batch`). `tcf-ui`는 `tcf-web` 미의존 → 이 파일 미적용.

---

### 2.3 `tcf-cache` — EhCache JCache 기본

**경로:** `tcf-cache/src/main/resources/application.yml`

```yaml
# TCF framework cache defaults (tcf-cache.jar)
spring:
  cache:
    type: jcache
    jcache:
      config: classpath:ehcache.xml

nsight:
  tcf:
    cache:
      enabled: true
      config-location: classpath:ehcache.xml
```

| 키 | 설명 |
|----|------|
| `spring.cache.type: jcache` | JSR-107 + EhCache 3 |
| `nsight.tcf.cache.enabled` | `false` 시 `TcfCacheAutoConfiguration` 비활성 |

**사용 모듈:** `tcf-om`만 `tcf-cache` 의존. 업무 WAR는 캐시 미사용.

---

## 3. `tcf-om` — 운영 WAR

### 3.1 `application.yml` (기본 · `local` 프로파일)

**경로:** `tcf-om/src/main/resources/application.yml`

```yaml
server:
  port: 8097
  servlet:
    context-path: /
    encoding:
      charset: UTF-8
      enabled: true
      force: true
    session:
      timeout: 60m
      tracking-modes: cookie
      cookie:
        name: JSESSIONID
        path: /
        http-only: true
        secure: false
        same-site: Lax

spring:
  application:
    name: nsight-tcf-om
  profiles:
    active: local
  cache:
    type: jcache
    jcache:
      config: classpath:ehcache.xml
  autoconfigure:
    exclude: []
  datasource:
    driver-class-name: org.h2.Driver
    url: jdbc:h2:file:${nsight.txlog.path:./data/nsight-txlog}/nsight_om;MODE=Oracle;AUTO_SERVER=TRUE;DATABASE_TO_UPPER=false
    username: sa
    password:
    hikari:
      pool-name: nsight-om-hikari
      maximum-pool-size: 10
      minimum-idle: 2
      connection-timeout: 3000
      validation-timeout: 3000
      idle-timeout: 600000
      max-lifetime: 1800000
      keepalive-time: 300000
      auto-commit: true
  h2:
    console:
      enabled: true
  sql:
    init:
      mode: embedded
      continue-on-error: true
      encoding: UTF-8
      schema-locations: classpath:schema.sql
      data-locations: classpath:data.sql
  session:
    store-type: jdbc
    timeout: 60m
    jdbc:
      initialize-schema: always
      table-name: SPRING_SESSION
  transaction:
    default-timeout: 5
  servlet:
    multipart:
      max-file-size: 50MB
      max-request-size: 50MB

mybatis:
  mapper-locations:
    - classpath:/mapper/**/*.xml
  configuration:
    map-underscore-to-camel-case: true
    default-statement-timeout: 3
    jdbc-type-for-null: NULL
    call-setters-on-nulls: true
    default-fetch-size: 500
    cache-enabled: false

management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus,threaddump
  endpoint:
    health:
      probes:
        enabled: true

nsight:
  system-id: NSIGHT-MP
  domain: nh.marketing.com
  tcf:
    cache:
      enabled: true
      config-location: classpath:ehcache.xml
    session-validation-enabled: false
    authorization-validation-enabled: false
    idempotency-enabled: false
    audit-enabled: true
    transaction-log-enabled: true
    transaction-log-schema-auto-init: false
    transaction-log-datasource:
      separate: false
      url: jdbc:h2:file:${nsight.txlog.path:./data/nsight-txlog}/nsight_om;MODE=Oracle;AUTO_SERVER=TRUE;DATABASE_TO_UPPER=false
      username: sa
      password:
      driver-class-name: org.h2.Driver
  timeout:
    online-transaction-seconds: 5
    db-query-seconds: 3
  om:
    batch-service-url: http://127.0.0.1:8098/batch
    session-cleanup:
      fixed-rate-ms: 10000
  updownload:
    storage-path: ./data/updownload
    max-file-size-bytes: 52428800
```

**섹션별 설명**

| 영역 | 핵심 설정 | OM 전용 의미 |
|------|-----------|--------------|
| `server` | port `8097` | bootRun 단독 기동 포트. Tomcat에서는 `om.war` → `/om` |
| `spring.datasource` | file H2 `nsight_om` | OM·세션·UD·거래로그 **단일 DB** |
| `spring.sql.init` | `schema.sql`, `data.sql` | 23개 OM 테이블 + 초기 데이터 |
| `spring.session` | `store-type: jdbc` | `@EnableJdbcHttpSession`, `SPRING_SESSION` |
| `spring.servlet.multipart` | 50MB | 파일 업·다운로드 ([18-fileupdownload.md](18-fileupdownload.md)) |
| `nsight.tcf.idempotency-enabled` | `false` | Admin 화면 중복 요청 허용 |
| `transaction-log-datasource.separate` | `false` | Primary DS = 거래로그 DS |
| `transaction-log-schema-auto-init` | `false` | DDL은 `schema.sql`에 포함 |
| `nsight.om.batch-service-url` | `:8098/batch` | bootRun 시 배치 직접 호출 URL |
| `nsight.updownload` | storage-path | UD 메타·파일 저장 루트 |

---

### 3.2 `application-dev.yml` (`dev` 프로파일)

**경로:** `tcf-om/src/main/resources/application-dev.yml`

```yaml
# ztomcat(8080) WAR 배포 — om.war → /om
nsight:
  gateway:
    base-url: http://127.0.0.1:8080
  om:
    batch-service-url: http://127.0.0.1:8080/batch
```

| 키 | local (`8098`) | dev/prod Tomcat (`8080` / gateway) |
|----|----------------|-------------------------------------|
| `om.batch-service-url` | `http://127.0.0.1:8098/batch` | `http://127.0.0.1:8080/batch` 또는 `${NSIGHT_GATEWAY_BASE_URL}/batch` |

OM 대시보드·스케줄이 배치 API를 호출할 때 context 경로가 바뀌므로 프로파일로 분리한다.

---

## 4. `tcf-batch` — 수집 배치

### 4.1 `application.yml` (공통)

**경로:** `tcf-batch/src/main/resources/application.yml`

```yaml
server:
  port: 8098

spring:
  application:
    name: nsight-tcf-batch
  profiles:
    default: local
  datasource:
    driver-class-name: org.h2.Driver
    url: jdbc:h2:file:${nsight.txlog.path:./data/nsight-txlog}/nsight_om;MODE=Oracle;AUTO_SERVER=TRUE;DATABASE_TO_UPPER=false
    username: sa
    password:
    hikari:
      pool-name: nsight-batch-hikari
      maximum-pool-size: 5
      connection-timeout: 3000

management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics

nsight:
  tcf:
    transaction-log-enabled: false
  batch:
    startup-collect:
      enabled: true
      initial-delay-ms: 0
    ap-status:
      job-id: BAT-BATCH-001
      cron: "0 */5 * * * *"
      connect-timeout-ms: 3000
      read-timeout-ms: 5000
    db-status:
      job-id: BAT-BATCH-002
      cron: "30 */5 * * * *"
    session-status:
      job-id: BAT-BATCH-003
      cron: "45 */5 * * * *"
      warn-active-threshold: 200
    deploy-status:
      job-id: BAT-BATCH-004
      cron: "55 */5 * * * *"
      default-version: 1.0.0-SNAPSHOT
```

| 키 | 설명 |
|----|------|
| `spring.profiles.default: local` | 프로파일 미지정 시 `local` (Tomcat은 `setenv`가 `dev`/`prod` 주입) |
| `transaction-log-enabled: false` | 배치 자체는 `TCF_TX_LOG` 미적재 |
| `nsight.batch.*.cron` | AP/DB/세션/배포 상태 5분 주기 수집 |
| `startup-collect.initial-delay-ms` | 기동 직후 1회 수집 지연 (dev yml에서 420000ms로 오버라이드) |

MyBatis·`sql.init` 없음 — JDBC·RestTemplate·스케줄만 사용.

---

### 4.2 `application-local.yml` (`local` 프로파일)

**경로:** `tcf-batch/src/main/resources/application-local.yml`

```yaml
# bootRun (:8098) — 각 모듈 개별 포트에서 수집
spring:
  servlet:
    context-path: /batch

nsight:
  gateway:
    mode: local
  batch:
    ap-status:
      targets:
        - ap-id: om-ap
          ap-name: tcf-om (:8097)
          base-url: http://127.0.0.1:8097
          enabled: true
        # ... sv, cc, mg 등 소수 타겟
```

| 키 | 설명 |
|----|------|
| `context-path: /batch` | bootRun URL이 `http://host:8098/batch/...` |
| `gateway.mode: local` | 수집기가 **모듈별 포트** 직접 호출 |
| `targets` | 소수 AP만 등록 (로컬 개발 부하 절감) |

루트 `build.gradle`·`tcf-batch/build.gradle`이 `spring.profiles.active=local`을 주입한다.

---

### 4.3 `application-dev.yml` (`dev` 프로파일)

**경로:** `tcf-batch/src/main/resources/application-dev.yml`

ztomcat 통합 시 **`deploy-wars.sh` 13 WAR**를 `${nsight.gateway.base-url}/{context}`로 수집한다.

**헤더·게이트웨이**

```yaml
nsight:
  gateway:
    mode: dev
    base-url: http://127.0.0.1:8080
  batch:
    startup-collect:
      initial-delay-ms: 420000
```

**수집 타겟 패턴** (`ap-status` 예시 — 19개 동일 형식):

```yaml
    ap-status:
      targets:
        - { ap-id: cc-ap, ap-name: 'cc-service (/cc)', base-url: '${nsight.gateway.base-url}/cc', enabled: true }
        # ... ic, pc, bc, ms, sv, om, batch, ui
```

| 구분 | local | dev / prod |
|------|-------|------------|
| AP URL | `http://127.0.0.1:{port}` | `${nsight.gateway.base-url}/{ctx}` |
| 대상 수 | 4 AP + UI + BATCH 등 | **13 WAR** (deploy) / 15 (buildZtomcatWars) |
| 기동 수집 지연 | `0` | `420000` ms (7분) |
| WAR 파일명 | `tcf-om.war`, `sv.war` | `om.war`, `batch.war`, `ui.war` (ztomcat rename) |

운영 `application-prod.yml`은 `base-url: ${NSIGHT_GATEWAY_BASE_URL:...}`만 오버라이드한다.

전체 YAML: `tcf-batch/src/main/resources/application-dev.yml`.

---

## 5. `tcf-ui` — Relay UI

### 5.1 `application.yml`

**경로:** `tcf-ui/src/main/resources/application.yml`

```yaml
server:
  port: 8099
  servlet:
    encoding:
      charset: UTF-8
      enabled: true
      force: true

spring:
  application:
    name: nsight-tcf-ui

management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics

nsight:
  tcf-ui:
    deployment-mode: bootrun
    tomcat-gateway-url: http://localhost:8080
    bootrun-host: http://127.0.0.1
```

| 키 | 설명 |
|----|------|
| `deployment-mode: bootrun` | 정적 HTML의 fetch가 **모듈별 포트** (`bootrun-host:8081` 등) 사용 |
| `tomcat-gateway-url` | Tomcat 모드 시 8080 게이트웨이 prefix |
| DS / MyBatis / `nsight.tcf` | **없음** — Relay·정적 리소스만 |

`main()`에서 `server.port=8099` 고정. `tcf-web` 미의존 → 거래로그·온라인 파이프라인 없음.

---

### 5.2 `application-dev.yml` / `application-prod.yml`

**경로:** `tcf-ui/src/main/resources/application-dev.yml`

```yaml
# ztomcat(8080) WAR 배포 — ui.war → /ui
nsight:
  tcf-ui:
    deployment-mode: tomcat
    tomcat-gateway-url: http://localhost:8080
```

**운영 (`application-prod.yml`):**

```yaml
nsight:
  tcf-ui:
    deployment-mode: tomcat
    tomcat-gateway-url: ${NSIGHT_GATEWAY_BASE_URL:https://marketing.example.com}
```

Tomcat 배포 시 Relay가 `/cc`, `/sv` 등 **context path URL**로 API를 호출한다.

---

## 6. 업무 WAR (`*-service` ×16)

16개 모듈은 **동일 YAML 골격**이며, 아래만 모듈별로 다르다.

| 모듈 | port | `spring.application.name` | Primary H2 mem DB |
|------|------|---------------------------|-------------------|
| `cc-service` | 8081 | `nsight-cc-service` | `nsight_cc` |
| `ic-service` | 8082 | `nsight-ic-service` | `nsight_ic` |
| `pc-service` | 8083 | `nsight-pc-service` | `nsight_pc` |
| `bc-service` | 8084 | `nsight-bc-service` | `nsight_bc` |
| `ms-service` | 8085 | `nsight-ms-service` | `nsight_ms` |
| `sv-service` | 8086 | `nsight-sv-service` | `nsight_sv` |
| `pd-service` | 8087 | `nsight-pd-service` | `nsight_pd` |
| `cm-service` | 8088 | `nsight-cm-service` | `nsight_cm` |
| `eb-service` | 8089 | `nsight-eb-service` | `nsight_eb` |
| `ep-service` | 8090 | `nsight-ep-service` | `nsight_ep` |
| `bp-service` | 8091 | `nsight-bp-service` | `nsight_bp` |
| `bd-service` | 8092 | `nsight-bd-service` | `nsight_bd` |
| `ss-service` | 8093 | `nsight-ss-service` | `nsight_ss` |
| `cs-service` | 8094 | `nsight-cs-service` | `nsight_cs` |
| `ct-service` | 8095 | `nsight-ct-service` | `nsight_ct` |
| `mg-service` | 8096 | `nsight-mg-service` | `nsight_mg` |

Tomcat context: `/{업무코드}` (예: `sv.war` → `/sv`). ztomcat WAR rename 규칙은 [22-build-project.md](22-build-project.md).

---

### 6.1 대표 원문 — `sv-service`

**경로:** `sv-service/src/main/resources/application.yml`

```yaml
server:
  port: 8086
  servlet:
    context-path: /
    encoding:
      charset: UTF-8
      enabled: true
      force: true
    session:
      timeout: 60m
      tracking-modes: cookie
      cookie:
        name: JSESSIONID
        path: /
        http-only: true
        secure: false
        same-site: Lax

spring:
  application:
    name: nsight-sv-service
  profiles:
    active: local
  datasource:
    driver-class-name: org.h2.Driver
    url: jdbc:h2:mem:nsight_sv;MODE=Oracle;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false
    username: sa
    password:
    hikari:
      pool-name: nsight-sv-hikari
      maximum-pool-size: 10
      minimum-idle: 2
      connection-timeout: 3000
      validation-timeout: 3000
      idle-timeout: 600000
      max-lifetime: 1800000
      keepalive-time: 300000
      auto-commit: false
  h2:
    console:
      enabled: true
  session:
    store-type: none
  transaction:
    default-timeout: 5

mybatis:
  mapper-locations:
    - classpath:/mapper/**/*.xml
  configuration:
    map-underscore-to-camel-case: true
    default-statement-timeout: 3
    jdbc-type-for-null: NULL
    call-setters-on-nulls: true
    default-fetch-size: 500
    cache-enabled: false

management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus,threaddump
  endpoint:
    health:
      probes:
        enabled: true

nsight:
  system-id: NSIGHT-MP
  domain: nh.marketing.com
  tcf:
    session-validation-enabled: false
    authorization-validation-enabled: false
    idempotency-enabled: true
    audit-enabled: true
    transaction-log-enabled: true
    transaction-log-schema-auto-init: true
    transaction-log-datasource:
      url: jdbc:h2:file:${nsight.txlog.path:./data/nsight-txlog}/nsight_om;MODE=Oracle;AUTO_SERVER=TRUE;DATABASE_TO_UPPER=false
      username: sa
      password:
      driver-class-name: org.h2.Driver
  timeout:
    online-transaction-seconds: 5
    db-query-seconds: 3
```

**업무 WAR 공통 해설**

| 영역 | 설정 | 의미 |
|------|------|------|
| `spring.datasource` | `h2:mem:nsight_{code}` | 업무 전용 in-memory DB (재기동 시 초기화) |
| `hikari.auto-commit` | `false` | Facade `@Transactional` 제어 |
| `spring.session.store-type` | `none` | Tomcat HTTP 세션 (Spring Session 미사용) |
| `mybatis.default-statement-timeout` | `3` | [08-timeout.md](08-timeout.md) DB 쿼리 상한 |
| `nsight.tcf.idempotency-enabled` | `true` | 온라인 멱등 처리 |
| `transaction-log-enabled` | `true` | `TCF_TX_LOG` 적재 |

---

### 6.2 거래로그 URL 명시 여부

| 구분 | 모듈 | `nsight.tcf` 거래로그 블록 |
|------|------|---------------------------|
| **명시** (`transaction-log-datasource.url` + `schema-auto-init: true`) | `bc-service`, `sv-service` | yml에 file H2 URL 직접 기술 |
| **상속** (14개) | `cc`, `ic`, `pc`, … `mg` | `tcf-web.jar` 기본 URL + 코드 기본값 사용 |

**상속 패턴 예 — `cc-service`** (`nsight.tcf` 하위만):

```yaml
nsight:
  system-id: NSIGHT-MP
  domain: nh.marketing.com
  tcf:
    session-validation-enabled: false
    authorization-validation-enabled: false
    idempotency-enabled: true
    audit-enabled: true
  timeout:
    online-transaction-seconds: 5
    db-query-seconds: 3
```

`transaction-log-datasource` 미기술 → `tcf-web.jar`의 file H2 URL이 merge된다. `transaction-log-schema-auto-init`도 기본 `true`.

업무 WAR는 **`application-local/dev/prod.yml`** 만 사용. Tomcat 로깅은 `tcf-web`의 `application-dev.yml`이 적용된다.

---

## 7. `om-service` (레거시 · 미배포)

**경로:** `om-service/src/main/resources/application.yml`

`sv-service`와 구조 동일. 차이:

| 항목 | `om-service` | `tcf-om` |
|------|--------------|----------|
| port | `8097` (충돌) | `8097` |
| `application.name` | `nsight-om-service` | `nsight-tcf-om` |
| Primary DB | `h2:mem:nsight_om` | `h2:file:.../nsight_om` |
| Spring Session JDBC | 없음 | 있음 |
| `sql.init` / OM schema | 없음 | 있음 |
| 배포 | 파이프라인 미포함 | `om.war` |

**운영·개발은 `tcf-om` 사용.** `om-service`와 `tcf-om` 동시 bootRun 금지.

---

## 8. Gradle bootRun · ztomcat JVM (yml 외 설정)

### 8.1 루트 `build.gradle` — 모든 bootRun

```groovy
def nsightTxlogPath = layout.projectDirectory.dir('data/nsight-txlog').asFile.absolutePath.replace('\\', '/')

subprojects { sub ->
    sub.plugins.withId('org.springframework.boot') {
        sub.tasks.withType(org.springframework.boot.gradle.tasks.run.BootRun).configureEach {
            workingDir = rootProject.projectDir
            systemProperty 'nsight.txlog.path', nsightTxlogPath
            systemProperty 'file.encoding', 'UTF-8'
            // ...
        }
    }
}
```

yml의 `${nsight.txlog.path:./data/nsight-txlog}`보다 **JVM systemProperty가 우선**한다.

### 8.2 `tcf-batch/build.gradle`

루트 `build.gradle`과 동일하게 `spring.profiles.active=local` (중복 명시).

### 8.3 `ztomcat/conf/setenv.sh` (dev, 발췌)

```sh
if [ -n "${NSIGHT_TXLOG_PATH:-}" ]; then
  CATALINA_OPTS="${CATALINA_OPTS} -Dnsight.txlog.path=${NSIGHT_TXLOG_PATH}"
fi

CATALINA_OPTS="${CATALINA_OPTS} -Xms512m -Xmx1536m -Duser.timezone=Asia/Seoul -Dspring.profiles.active=dev ..."
```

### 8.4 `ztomcat/conf/setenv.prod.sh` (운영 샘플, 발췌)

```sh
export NSIGHT_GATEWAY_BASE_URL="${NSIGHT_GATEWAY_BASE_URL:-https://marketing.example.com}"
CATALINA_OPTS="${CATALINA_OPTS} -Xms1024m -Xmx4096m -Dspring.profiles.active=prod ..."
```

| JVM 옵션 | yml 대응 |
|----------|----------|
| `-Dnsight.txlog.path=...` | 모든 file H2 URL placeholder |
| `-Dspring.profiles.active=dev` | `application-dev.yml` 활성화 |
| `-Dspring.profiles.active=prod` | `application-prod.yml` + `dev` 그룹 |
| `NSIGHT_GATEWAY_BASE_URL` | prod 게이트웨이·Relay URL |

상세: [21-env-tomcat.md](21-env-tomcat.md).

---

## 9. 프로젝트별 설정 파일 위치 요약

| 모듈 | 설정 파일 |
|------|-----------|
| `tcf-web` | `application.yml`, `application-{local,dev,prod}.yml` |
| `tcf-cache` | `application.yml`, `application-{local,dev,prod}.yml` |
| `tcf-om` | `application.yml`, `application-{local,dev,prod}.yml` |
| `tcf-batch` | `application.yml`, `application-{local,dev,prod}.yml` |
| `tcf-ui` | `application.yml`, `application-{local,dev,prod}.yml` |
| `*-service` ×16 | `application.yml`, `application-{local,dev,prod}.yml` |
| `om-service` | `application.yml` (레거시) |

---

## 10. 신규 업무 WAR 체크리스트

1. `sv-service/application.yml` 복제 후 port·`application.name`·`h2:mem` DB명·pool-name 변경
2. 거래로그 URL을 yml에 넣을지(`bc`/`sv` 방식) `tcf-web` 상속에 맡길지 결정
3. `application-dev.yml` **작성 불필요** (프레임워크 JAR가 Tomcat 로깅 처리)
4. bootRun: 루트에서 실행해 `nsight.txlog.path`·`local` 프로파일 자동 주입 확인

개념·AutoConfiguration: [20-env-spring.md](20-env-spring.md) · 테이블 매핑: [19-tcf-table.md](19-tcf-table.md).
