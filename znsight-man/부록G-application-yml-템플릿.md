# 부록 G. application.yml 템플릿

> **NSIGHT TCF 개발 Manual** · 원본: [`znsight-guide-word`](../znsight-guide-word/) · 갱신: 2026-07-05

## G. application.yml 템플릿

### G.1 도입 전 안내말

본 부록은 NSIGHT TCF Framework에서 사용하는 Spring Boot application.yml 표준 템플릿을 정의한다.
NSIGHT에서 application.yml은 단순한 설정 파일이 아니라 Port, Context Path, 세션, DB, HikariCP, MyBatis, Transaction, TCF 실행정책, 보안, 로그, Actuator, Batch, Cache, 파일경로를 통제하는 운영 기준 파일이다. Spring 환경 설정 정의서에서도 application.yml은 실행환경, WAS 배포, 거래 처리, 세션, DB, Pool, MyBatis, 트랜잭션, 운영통제, 보안을 관리하는 핵심 기준으로 정의하고 있다.
```text
application.yml
↓
Spring Boot 실행환경
↓
TCF 거래처리 정책
↓
Session / DB / Pool / Timeout
↓
Log / Metrics / Health
↓
```

OM 환경설정 조회 / 운영 점검

### G.2 설정 파일 분리 기준

NSIGHT 설정 파일은 다음 구조로 분리한다.
src/main/resources
```text
 ├─ application.yml
 ├─ application-local.yml
 ├─ application-dev.yml
 ├─ application-stg.yml
 ├─ application-prod.yml
 │
 ├─ application-datasource.yml
 ├─ application-session.yml
 ├─ application-tcf.yml
 ├─ application-gateway.yml
 ├─ application-jwt.yml
 ├─ application-cache.yml
 ├─ application-batch.yml
 │
 ├─ logback-spring.xml
 └─ mapper/
     ├─ sv/
     ├─ cm/
     ├─ om/
     └─ ...
```

| 파일 | 역할 |
| --- | --- |
| application.yml | 공통 기본 설정 |
| application-local.yml | 개발자 PC 실행 설정 |
| application-dev.yml | 개발/통합검증 환경 설정 |
| application-stg.yml | 성능/인수검증 환경 설정 |
| application-prod.yml | 운영 환경 설정 |
| application-datasource.yml | RDW, ADW, SESSIONDB, LOGDB, OMDB 설정 |
| application-session.yml | Spring Session JDBC 설정 |
| application-tcf.yml | TCF 거래처리, 거래통제, Timeout, 로그 설정 |
| application-gateway.yml | Gateway 라우팅 및 Downstream Timeout 설정 |
| application-jwt.yml | SSO/JWT 인증 설정 |
| application-cache.yml | Cache 정책 설정 |
| application-batch.yml | Batch/Scheduler 설정 |

운영 설정은 Git/CI-CD 기준으로 관리하고, 운영 서버에서 직접 수정하지 않는 것을 원칙으로 한다. 비밀번호, JWT Key, API Key는 Git에 저장하지 않고 환경변수 또는 Secret 관리 체계로 외부화한다.

### G.3 기본 application.yml 템플릿

```yaml
spring:
  application:
    name: ${NSIGHT_APP_NAME:nsight-sv-service}
  profiles:
    active: ${SPRING_PROFILES_ACTIVE:local}
    group:
      local:
        - datasource-local
        - session
        - mybatis
        - tcf
        - cache
        - actuator
      dev:
        - datasource-dev
        - session
        - mybatis
        - tcf
        - cache
        - actuator
      stg:
        - datasource-stg
        - session
        - mybatis
        - tcf
        - cache
        - actuator
        - security
      prod:
        - datasource-prod
        - session
        - mybatis
        - tcf
        - cache
        - actuator
        - security
server:
  port: ${SERVER_PORT:8080}
  forward-headers-strategy: framework
  servlet:
    context-path: ${SERVER_CONTEXT_PATH:/sv}
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
        secure: ${SESSION_COOKIE_SECURE:false}
        same-site: Lax
spring:
  transaction:
    default-timeout: 5
  servlet:
    multipart:
      enabled: true
      max-file-size: 100MB
      max-request-size: 100MB
mybatis:
  mapper-locations:
    - classpath:/mapper/**/*.xml
  type-aliases-package: com.nh.nsight
  configuration:
    map-underscore-to-camel-case: true
    default-statement-timeout: 3
    default-fetch-size: 500
    jdbc-type-for-null: NULL
    call-setters-on-nulls: true
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
      show-details: when_authorized
  metrics:
    tags:
      application: ${spring.application.name}
logging:
  level:
    root: INFO
    com.nh.nsight: INFO
    org.springframework: WARN
    org.mybatis: WARN
```

### G.4 업무 WAR 공통 설정 기준

업무 WAR는 같은 설정 패턴을 사용하고, 업무별 차이는 application.name, context-path, business-code, DB Pool, Timeout 정도로 제한한다.
| 항목 | SV 예시 |
| --- | --- |
| OM 예시 | 설명 |
| spring.application.name | nsight-sv-service |
| nsight-om-service | 애플리케이션명 |
| server.servlet.context-path | /sv |
| /om | 업무 Context |
| nsight.business-code | SV |
| OM | 업무코드 |
| nsight.tcf.online-endpoint | /online |
| /online | 표준 온라인 Endpoint |
| server.port | 8086 |
| 8090 | bootRun 기준 포트 |
| WAR 파일 | sv.war |
| om.war | Tomcat 배포 단위 |
| spring: | application: |

    name: nsight-sv-service

```yaml
server:
  port: ${SERVER_PORT:8086}
  servlet:
    context-path: ${SERVER_CONTEXT_PATH:/sv}
nsight:
  business-code: SV
  module-name: sv-service
  online-endpoint: /online
```

외부 Tomcat에 WAR로 배포할 경우 server.port는 내장 Tomcat 실행 시에만 의미가 있고, 실제 운영 포트는 Tomcat Connector와 Apache/L4 라우팅 기준으로 관리한다.

### G.5 Profile별 설정 템플릿

#### G.5.1 application-local.yml

```yaml
spring:
  config:
    activate:
      on-profile: local
server:
  port: 8086
  servlet:
    context-path: /sv
spring:
  datasource:
    rdw:
      jdbc-url: jdbc:h2:mem:nsight_rdw;MODE=Oracle;DB_CLOSE_DELAY=-1
      username: sa
      password:
      driver-class-name: org.h2.Driver
      hikari:
        pool-name: local-rdw-pool
        maximum-pool-size: 10
        minimum-idle: 2
        connection-timeout: 3000
        validation-timeout: 3000
        auto-commit: false
nsight:
  tcf:
    session-validation-enabled: false
    authorization-validation-enabled: false
    transaction-control-enabled: false
    idempotency-enabled: false
```

#### G.5.2 application-dev.yml

```yaml
spring:
  config:
    activate:
      on-profile: dev
server:
  servlet:
    context-path: ${SERVER_CONTEXT_PATH:/sv}
spring:
  datasource:
    rdw:
      jdbc-url: ${NSIGHT_DEV_RDW_DB_URL}
      username: ${NSIGHT_DEV_RDW_DB_USER}
      password: ${NSIGHT_DEV_RDW_DB_PASSWORD}
      driver-class-name: oracle.jdbc.OracleDriver
nsight:
  tcf:
    session-validation-enabled: true
    authorization-validation-enabled: true
    transaction-control-enabled: true
    idempotency-enabled: true
```

#### G.5.3 application-prod.yml

```yaml
spring:
  config:
    activate:
      on-profile: prod
server:
  servlet:
    session:
      cookie:
        secure: true
        http-only: true
        same-site: Lax
spring:
  datasource:
    rdw:
      jdbc-url: ${NSIGHT_RDW_DB_URL}
      username: ${NSIGHT_RDW_DB_USER}
      password: ${NSIGHT_RDW_DB_PASSWORD}
      driver-class-name: oracle.jdbc.OracleDriver
    adw:
      jdbc-url: ${NSIGHT_ADW_DB_URL}
      username: ${NSIGHT_ADW_DB_USER}
      password: ${NSIGHT_ADW_DB_PASSWORD}
      driver-class-name: oracle.jdbc.OracleDriver
    sessiondb:
      jdbc-url: ${NSIGHT_SESSION_DB_URL}
      username: ${NSIGHT_SESSION_DB_USER}
      password: ${NSIGHT_SESSION_DB_PASSWORD}
      driver-class-name: oracle.jdbc.OracleDriver
    logdb:
      jdbc-url: ${NSIGHT_LOG_DB_URL}
      username: ${NSIGHT_LOG_DB_USER}
      password: ${NSIGHT_LOG_DB_PASSWORD}
      driver-class-name: oracle.jdbc.OracleDriver
nsight:
  tcf:
    session-validation-enabled: true
    authorization-validation-enabled: true
    transaction-control-enabled: true
    timeout-policy-enabled: true
    transaction-log-enabled: true
    audit-log-enabled: true
    idempotency-enabled: true
```

운영 환경에서는 DB URL, 사용자, 비밀번호, Gateway URL, 파일 경로 같은 값은 환경변수로 주입한다. 운영 비밀번호를 application-prod.yml에 직접 저장하지 않는다.

### G.6 DataSource / HikariCP 템플릿

NSIGHT는 DB 목적별로 DataSource와 HikariCP Pool을 분리한다.
```yaml
spring:
  datasource:
    rdw:
      jdbc-url: ${NSIGHT_RDW_DB_URL}
      username: ${NSIGHT_RDW_DB_USER}
      password: ${NSIGHT_RDW_DB_PASSWORD}
      driver-class-name: oracle.jdbc.OracleDriver
      hikari:
        pool-name: nsight-rdw-hikari
        maximum-pool-size: ${NSIGHT_RDW_POOL_MAX:120}
        minimum-idle: ${NSIGHT_RDW_POOL_MIN:30}
        connection-timeout: 3000
        validation-timeout: 3000
        idle-timeout: 600000
        max-lifetime: 1800000
        keepalive-time: 300000
        auto-commit: false
    adw:
      jdbc-url: ${NSIGHT_ADW_DB_URL}
      username: ${NSIGHT_ADW_DB_USER}
      password: ${NSIGHT_ADW_DB_PASSWORD}
      driver-class-name: oracle.jdbc.OracleDriver
      hikari:
        pool-name: nsight-adw-hikari
        maximum-pool-size: ${NSIGHT_ADW_POOL_MAX:80}
        minimum-idle: ${NSIGHT_ADW_POOL_MIN:20}
        connection-timeout: 3000
        validation-timeout: 3000
        idle-timeout: 600000
        max-lifetime: 1800000
        keepalive-time: 300000
        auto-commit: false
    sessiondb:
      jdbc-url: ${NSIGHT_SESSION_DB_URL}
      username: ${NSIGHT_SESSION_DB_USER}
      password: ${NSIGHT_SESSION_DB_PASSWORD}
      driver-class-name: oracle.jdbc.OracleDriver
      hikari:
        pool-name: nsight-sessiondb-hikari
        maximum-pool-size: ${NSIGHT_SESSION_POOL_MAX:120}
        minimum-idle: ${NSIGHT_SESSION_POOL_MIN:30}
        connection-timeout: 3000
        validation-timeout: 3000
        idle-timeout: 600000
        max-lifetime: 1800000
        keepalive-time: 300000
        auto-commit: false
    logdb:
      jdbc-url: ${NSIGHT_LOG_DB_URL}
      username: ${NSIGHT_LOG_DB_USER}
      password: ${NSIGHT_LOG_DB_PASSWORD}
      driver-class-name: oracle.jdbc.OracleDriver
      hikari:
        pool-name: nsight-logdb-hikari
        maximum-pool-size: ${NSIGHT_LOG_POOL_MAX:50}
        minimum-idle: ${NSIGHT_LOG_POOL_MIN:10}
        connection-timeout: 3000
        validation-timeout: 3000
        idle-timeout: 600000
        max-lifetime: 1800000
        keepalive-time: 300000
        auto-commit: false
```

| 설정 | 기준 |
| --- | --- |
| connection-timeout | 3초 |
| validation-timeout | 3초 |
| idle-timeout | 10분 |
| max-lifetime | 30분 이하 |
| keepalive-time | 5분 |

auto-commit

| false | Pool 사용률 |
| --- | --- |
Hikari connection-timeout은 SQL 실행 제한시간이 아니라 Pool에서 Connection을 빌리기 위해 대기하는 시간이다. SQL 실행 제한은 MyBatis Query Timeout으로 관리한다.

| 70~80% 이하 유지 | |

### G.7 Session 설정 템플릿

```yaml
server:
  servlet:
    session:
      timeout: 60m
      tracking-modes: cookie
      cookie:
        name: JSESSIONID
        path: /
        http-only: true
        secure: ${SESSION_COOKIE_SECURE:true}
        same-site: Lax
spring:
  session:
    store-type: jdbc
    timeout: 60m
    jdbc:
      table-name: SPRING_SESSION
      initialize-schema: never
      cleanup-cron: "0 */5 * * * *"
```

| 항목 | 기준 |
| --- | --- |
| Session Timeout | 60분 |
| 저장 방식 | Spring Session JDBC |
| 세션 테이블 | SPRING_SESSION, SPRING_SESSION_ATTRIBUTES |
| Tracking | Cookie Only |
| Cookie | HttpOnly, 운영 Secure=true |
| 만료 세션 정리 | 5분 주기 |

Spring Session JDBC는 WAS 메모리 세션이 아니라 SESSIONDB 중심으로 세션을 공유하기 위한 구조이며, 다중 WAR와 다중 WAS 환경에서 세션 중앙화 기준으로 사용한다.

### G.8 MyBatis 설정 템플릿

```yaml
mybatis:
  mapper-locations:
    - classpath:/mapper/**/*.xml
  type-aliases-package: com.nh.nsight
  configuration:
    map-underscore-to-camel-case: true
    default-statement-timeout: 3
    default-fetch-size: 500
    jdbc-type-for-null: NULL
    call-setters-on-nulls: true
    safe-row-bounds-enabled: true
    safe-result-handler-enabled: true
    cache-enabled: false
nsight:
  mybatis:
    sql-id-required: true
    rdw-query-timeout-seconds: 3
    adw-query-timeout-seconds: 5
    default-page-size: 100
    max-page-size: 1000
    block-select-star: true
    block-no-where-update-delete: true
```

| 항목 | 기준 |
| --- | --- |
| Mapper 위치 | classpath:/mapper/**/*.xml |
| SQL Timeout | RDW 3초, ADW 5초 |
| Fetch Size | 기본 500 |
| MyBatis Cache | 기본 비활성 |
| 목록조회 | Paging 필수 |
| 금지 | SELECT *, ${} 사용자 입력 직접 사용 |

### G.9 TCF 설정 템플릿

```yaml
nsight:
  tcf:
    enabled: true
    online:
      endpoint: /online
      default-business-code: ${NSIGHT_BUSINESS_CODE:SV}
    transaction-control-enabled: true
    timeout-policy-enabled: true
    session-validation-enabled: true
    authorization-validation-enabled: true
    idempotency-enabled: true
    transaction-log-enabled: true
    audit-log-enabled: true
    timeout:
      default-online-timeout-sec: 5
      default-tx-timeout-sec: 5
      default-db-query-timeout-sec: 3
      default-external-connect-timeout-ms: 3000
      default-external-read-timeout-ms: 5000
      fail-when-policy-not-found: false
    dispatcher:
      fail-on-duplicated-service-id: true
      fail-on-handler-not-found: true
    log:
      guid-required: false
      trace-id-required: false
      generate-guid-when-empty: true
      generate-trace-id-when-empty: true
      mdc-enabled: true
      mask-sensitive-data: true
    idempotency:
      enabled: true
      header-name: idempotencyKey
      ttl-seconds: 300
      apply-processing-types:
        - CREATE
        - UPDATE
        - DELETE
        - EXECUTE
        - SEND
```

Timeout 기본값은 application.yml에 두되, 서비스별 정책은 TCF_SERVICE_TIMEOUT_POLICY 같은 정책 테이블 값을 우선 적용한다. TCF Timeout 설계에서도 기본 정책은 application.yml, 서비스별 정책은 DB 정책값 우선으로 정의한다.

### G.10 Gateway 설정 템플릿

```yaml
nsight:
  gateway:
    enabled: true
    mode: route-by-business-code
    default-connect-timeout-ms: 3000
    default-read-timeout-ms: 8000
    preserve-guid: true
    preserve-trace-id: true
    routes:
      SV:
        context-path: /sv
        target-base-url: ${NSIGHT_ROUTE_SV_URL:http://sv-service:8080}
        online-path: /online
        connect-timeout-ms: 3000
        read-timeout-ms: 5000
        enabled: true
      CM:
        context-path: /cm
        target-base-url: ${NSIGHT_ROUTE_CM_URL:http://(미포함·확장 예정):8080}
        online-path: /online
        connect-timeout-ms: 3000
        read-timeout-ms: 5000
        enabled: true
      OM:
        context-path: /om
        target-base-url: ${NSIGHT_ROUTE_OM_URL:http://om-service:8080}
        online-path: /online
        connect-timeout-ms: 3000
        read-timeout-ms: 5000
        enabled: true
```

Gateway는 업무코드 기준으로 Downstream 업무 WAR에 라우팅한다. Header의 businessCode, serviceId, transactionCode, guid, traceId는 유지되어야 한다.

### G.11 JWT / SSO 설정 템플릿

```yaml
nsight:
  auth:
    mode: sso-jwt-session
    sso:
      enabled: true
      provider: nh-sso
      token-verify-url: ${SSO_VERIFY_URL}
      connect-timeout-ms: 3000
      read-timeout-ms: 5000
      user-id-claim: userId
      branch-id-claim: branchId
      channel-id-claim: branchId
      channel-id-claim: channelId
    jwt:
      enabled: true
      issuer: nsight-tcf-jwt
      algorithm: RS256
      access-token-expire-minutes: 30
      refresh-token-expire-hours: 8
      key-id: nsight-key-01
      public-key-location: ${JWT_PUBLIC_KEY_LOCATION}
      private-key-location: ${JWT_PRIVATE_KEY_LOCATION}
      denylist-enabled: true
      refresh-token-store: database
    security:
      compare-token-claim-with-header: true
      require-session-for-web: true
      require-jwt-for-gateway: true
      mask-token-in-log: true
security:
  csrf:
    enabled: true
  headers:
    frame-options: SAMEORIGIN
    content-security-policy-enabled: true
  mask:
    fields:
      - password
      - token
      - accessToken
      - refreshToken
      - rrn
      - accountNo
```

JWT/SSO 설정은 Token 자체보다 세션, Header, 권한검증, 로그 마스킹과 함께 관리해야 한다.

### G.12 Cache 설정 템플릿

```yaml
spring:
  cache:
    type: caffeine
nsight:
  cache:
    enabled: true
    caches:
      service-catalog:
        name: serviceCatalogCache
        ttl-seconds: 300
        maximum-size: 10000
      transaction-control:
        name: transactionControlCache
        ttl-seconds: 300
        maximum-size: 50000
      timeout-policy:
        name: timeoutPolicyCache
        ttl-seconds: 300
        maximum-size: 10000
      common-code:
        name: commonCodeCache
        ttl-seconds: 600
        maximum-size: 50000
      error-code:
        name: errorCodeCache
        ttl-seconds: 600
        maximum-size: 10000
    refresh:
      enabled: true
      admin-only: true
      audit-required: true
```

Cache 대상은 공통코드, ServiceId Catalog, 거래통제, Timeout 정책, 오류코드처럼 변경 빈도는 낮고 조회 빈도는 높은 기준정보로 제한한다.

### G.13 Batch 설정 템플릿

```yaml
spring:
  task:
    scheduling:
      pool:
        size: 4
    execution:
      pool:
        core-size: 8
        max-size: 16
        queue-capacity: 100
nsight:
  batch:
    enabled: true
    instance-id: ${HOSTNAME:local-batch-01}
    scheduler:
      enabled: true
      scan-interval-ms: 30000
      timezone: Asia/Seoul
    execution:
      max-concurrent-jobs: 8
      default-timeout-sec: 300
      default-retry-count: 1
      history-retention-days: 90
    lock:
      enabled: true
      lease-seconds: 600
    jobs:
      ap-status:
        job-id: BAT-BATCH-001
        enabled: true
        fixed-delay-seconds: 60
      db-status:
        job-id: BAT-BATCH-002
        enabled: true
        fixed-delay-seconds: 60
      session-status:
        job-id: BAT-BATCH-003
        enabled: true
        fixed-delay-seconds: 300
      deploy-status:
        job-id: BAT-BATCH-004
        enabled: true
        fixed-delay-seconds: 300
```

Batch 설정은 application.yml 또는 DB 정책 테이블에서 관리할 수 있다. 운영형 구조에서는 OM에서 스케줄과 실행 이력을 조회하고, 실제 실행은 tcf-batch가 수행한다.

### G.14 파일 업로드/다운로드 설정 템플릿

```yaml
spring:
  servlet:
    multipart:
      enabled: true
      max-file-size: 100MB
      max-request-size: 100MB
      file-size-threshold: 1MB
nsight:
  file:
    storage:
      type: nas
      base-path: ${NSIGHT_FILE_STORAGE_PATH:/data/nsight/files}
      temp-path: ${NSIGHT_FILE_TEMP_PATH:/data/nsight/files/tmp}
    upload:
      enabled: true
      allowed-extensions:
        - xlsx
        - csv
        - txt
        - pdf
      max-file-size: 100MB
      virus-scan-enabled: false
    download:
      enabled: true
      require-reason: true
      audit-enabled: true
      buffer-size: 8192
      large-file-threshold: 10MB
      content-disposition-charset: UTF-8
```

파일 원본은 DB나 Session에 저장하지 않고 Storage에 저장한다. DB에는 파일 메타정보와 다운로드 이력을 저장한다.

### G.15 Actuator / Health 설정 템플릿

management:
```java
server:
    port: ${MANAGEMENT_PORT:${SERVER_PORT:8080}}
  endpoints:
    web:
      base-path: /actuator
      exposure:
        include: health,info,metrics,prometheus,threaddump
  endpoint:
    health:
      probes:
        enabled: true
      show-details: when_authorized
  health:
    livenessstate:
      enabled: true
    readinessstate:
      enabled: true
    db:
      enabled: true
  metrics:
    tags:
      application: ${spring.application.name}
      business-code: ${NSIGHT_BUSINESS_CODE:SV}
      instance: ${HOSTNAME:local}
info:
  app:
    name: ${spring.application.name}
    business-code: ${NSIGHT_BUSINESS_CODE:SV}
    version: ${APP_VERSION:local}
```

Health Check는 단순 프로세스 생존 여부뿐 아니라 Readiness, DB, SessionDB, TCF 상태까지 확장해서 OM 대시보드와 연계한다.

### G.16 Logging 설정 템플릿

```yaml
logging:
  file:
    path: ${NSIGHT_LOG_PATH:/logs/nsight}
    name: ${logging.file.path}/${spring.application.name}.log
  level:
    root: INFO
    com.nh.nsight: INFO
    com.nh.nsight.tcf: INFO
    org.springframework.web: WARN
    org.mybatis: WARN
    jdbc.sqlonly: OFF
    jdbc.resultsettable: OFF
nsight:
  log:
    guid-mdc-enabled: true
    trace-id-mdc-enabled: true
    transaction-log-enabled: true
    audit-log-enabled: true
    performance-log-enabled: true
    mask-sensitive-data: true
    mask-fields:
      - password
      - token
      - accessToken
      - refreshToken
      - rrn
      - accountNo
      - customerNo
```

로그 기준은 다음과 같다.

| 로그 유형 | 기준 | 애플리케이션 로그 |
| --- | --- | --- |
| spring.application.name 기준 | 거래로그 | GUID, TraceId, ServiceId, TransactionCode 포함 |
| 감사로그 | 사용자, 메뉴, 기능, 권한, 다운로드, 관리자 기능 | 성능로그 |

elapsedTimeMs, SQL Time, External Time
보안로그
인증 실패, 권한 실패, 차단 요청
마스킹
개인정보, Token, 계좌번호, 고객번호

### G.17 환경변수 표준

환경변수
| 설명 | 예시 |
| --- | --- |
| SPRING_PROFILES_ACTIVE | 실행 Profile |
| prod | SERVER_PORT |
| bootRun 포트 | 8086 |
| SERVER_CONTEXT_PATH | Context Path |
| /sv | NSIGHT_BUSINESS_CODE |
| 업무코드 | SV |
| NSIGHT_RDW_DB_URL | RDW DB URL |
| 운영 Secret | NSIGHT_RDW_DB_USER |
| RDW 사용자 | 운영 Secret |
| NSIGHT_RDW_DB_PASSWORD | RDW 비밀번호 |
| 운영 Secret | NSIGHT_SESSION_DB_URL |
| SESSIONDB URL | 운영 Secret |
| NSIGHT_LOG_DB_URL | LOGDB URL |
| 운영 Secret | NSIGHT_FILE_STORAGE_PATH |
| 파일 저장소 경로 | /data/nsight/files |
| JWT_PUBLIC_KEY_LOCATION | JWT 공개키 위치 |
| Secret Mount | JWT_PRIVATE_KEY_LOCATION |
| JWT 개인키 위치 | Secret Mount |
| SSO_VERIFY_URL | SSO 검증 URL |
설정 우선순위는 일반적으로 JVM -D 시스템 속성 → OS 환경변수 → application-{profile}.yml → application.yml → 기본값 순서로 정리한다.

| 내부 URL | |

### G.18 운영 적용 기준

| 구분 | 운영 기준 | Profile |
| --- | --- | --- |
| 운영은 prod | 설정 변경 | Git Commit + CI/CD 배포 기준 |
| 운영 직접 수정 | 금지 | Secret |
| 환경변수, Vault, Secret 파일 사용 | 세션 | Spring Session JDBC, 60분 |
| Transaction Timeout | 기본 5초 | MyBatis Query Timeout |
| RDW 3초, ADW 5초 | Hikari Connection Timeout | 3초 |
| External Timeout | Connect 3초, Read 5초 | Actuator |
| Health, Metrics 제한 노출 | Log | GUID, TraceId, ServiceId 포함 |
| 보안 | Cookie Secure, HttpOnly, SameSite | 환경설정 조회 |

OM에서 현재 기동값 조회, 직접 수정 금지
OM 환경설정 조회는 application.yml 원문을 보여주는 기능이 아니라 실제 Spring Environment, HikariCP Runtime, MyBatis Timeout, Tomcat/WAR 배포정보, 표준 기준값과 현재값 차이를 확인하는 운영 조회 기능으로 설계한다.

### G.19 금지 패턴

| 금지 패턴 | 사유 |
| --- | --- |
| 운영 DB 비밀번호를 yml에 직접 저장 | Secret 유출 위험 |
| 운영 서버에서 yml 직접 수정 | 변경 이력 추적 불가 |
| Profile 없이 단일 yml만 사용 | 환경별 설정 충돌 |
| RDW/ADW/SESSIONDB Pool 혼용 | 장애 영향 범위 확대 |
| connection-timeout을 SQL Timeout으로 오해 | Pool 대기시간과 SQL 실행시간 혼동 |

Session을 WAS Memory에만 저장

| 다중 WAS 세션 불안정 | Actuator 전체 노출 | 보안 위험 |
| --- | --- | --- |
| logging.level.root=DEBUG 운영 적용 | 로그 폭증, 개인정보 노출 위험 | 파일 저장경로를 코드에 하드코딩 |
| 배포환경 변경 취약 | Timeout 기본값 없이 운영 | 장애 시 Thread/Pool 고갈 위험 |

### G.20 최종 체크리스트

| 점검 항목 | 확인 기준 | Profile |
| --- | --- | --- |
| local/dev/stg/prod 분리 여부 | Context Path | 업무코드와 일치 여부 |
| Session | 60분, Cookie Only, JDBC 저장 여부 | DataSource |
| RDW/ADW/SESSIONDB/LOGDB 분리 여부 | HikariCP | Pool Size, Timeout, Lifetime 설정 여부 |
| MyBatis | Mapper 위치, Query Timeout, Fetch Size 설정 여부 | TCF |
| 거래통제, 권한, 세션, Timeout, 로그 활성화 여부 | Gateway | 업무코드별 Route, Timeout 설정 여부 |
| JWT/SSO | Secret 외부화, Token 마스킹 여부 | Cache |
| 기준정보 Cache TTL 설정 여부 | Batch | Scheduler, Lock, Timeout 설정 여부 |
| File | Storage Path, 다운로드 감사 설정 여부 | Actuator |
| Health/Metrics 제한 노출 여부 | Logging | GUID/TraceId/MDC/마스킹 설정 여부 |
| Secret | Git 저장 금지 여부 | 변경관리 |

Git/CI-CD 기준 반영 여부

### G.21 최종 정리

application.yml 템플릿의 핵심은 다음이다.
application.yml
= 서버 실행 기준
+ 세션 기준
+ DB / Pool 기준
+ MyBatis 기준
+ Timeout 기준
+ TCF 실행정책
+ 보안 기준
+ 로그 / 모니터링 기준

따라서 NSIGHT TCF Framework에서 application.yml은 다음 한 문장으로 정의할 수 있다.
application.yml은 개발 편의용 설정 파일이 아니라, NSIGHT 온라인 거래가 안정적으로 실행되고 운영자가 장애 원인을 설명할 수 있도록 만드는 런타임 운영 표준이다.

## 소스·관련 문서

| 참고 |
|------|

> znsight-guide-word: `통합 (78).docx`

| [20-Spring환경설정.md](../zman/20-Spring환경설정.md) |

## 코드베이스 정정 (develop 기준)

| 항목 | 값 |
|------|-----|
| 업무 WAR | ic, pc, ms, sv, pd, eb, ep, ss, mg + tcf-om |
| ztomcat deploy | `ztomcat/deploy-wars.sh` 13 WAR |
| buildZtomcatWars | 15 WAR |
| bootRun | gateway 8100, uj 8102, jwt 8110, ui 8099 |


---

[← 78. 테스트 코드 샘플](./78-테스트-코드-샘플.md) · [README](./README.md)