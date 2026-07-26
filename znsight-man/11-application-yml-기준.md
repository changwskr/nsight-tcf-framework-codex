# 11. application.yml 구성 기준

> **NSIGHT TCF 개발 Manual** · 원본: [`znsight-guide-word`](../znsight-guide-word/) · 갱신: 2026-07-05

## 11. application.yml 구성 기준

### 11.1 도입 전 안내말

본 장은 NSIGHT TCF Framework에서 사용하는 application.yml 구성 기준을 정의한다.
application.yml은 단순히 Spring Boot 실행값을 적어두는 파일이 아니다. NSIGHT에서는 application.yml을 서버, Context Path, 세션, DB, HikariCP, MyBatis, 트랜잭션, Timeout, TCF 실행정책, Gateway 라우팅, JWT, Cache, Batch, 로그, 모니터링을 통제하는 운영 기준 파일로 본다.
NSIGHT Spring Boot application.yml 표준에서도 application.yml은 Port, Context Path, Forward Header, 세션, DB, HikariCP, MyBatis, Transaction, Security, Log, Actuator 설정을 통제하는 핵심 파일로 정의되어 있다. 또한 NSIGHT 구축·운영 표준서는 Apache, Tomcat, Spring Boot, Spring Session JDBC, RDW/ADW/SESSIONDB, GitLab CI/CD를 적용 범위로 둔다.

### 11.2 application.yml 구성 원칙

| 원칙 | 설명 |
| --- | --- |
| 환경별 분리 | local, dev, stg, prd 설정을 분리한다 |
| 운영 직접 수정 금지 | 운영 서버에서 직접 수정하지 않고 Git 형상관리 기준으로 반영한다 |
| 업무코드 일치 | Context Path, 업무코드, Mapper 경로, WAR명이 일치해야 한다 |
| Secret 외부화 | DB Password, JWT Secret, API Key는 환경변수 또는 Secret으로 관리한다 |
| Timeout 명확화 | Session Timeout, Transaction Timeout, Query Timeout, Gateway Timeout을 구분한다 |
| DB 역할 분리 | RDW, ADW, SESSIONDB, LOGDB, OMDB를 목적별로 분리한다 |
| 관측성 포함 | GUID, TraceId, MDC, Actuator, Health Check 설정을 포함한다 |
| 보안 기본 적용 | Cookie Secure, HttpOnly, SameSite, CSRF, 마스킹 기준을 적용한다 |
| 업무별 독립성 | 업무 WAR별로 필요한 설정을 독립 관리한다 |
| 공통 기준 유지 | 공통 TCF 설정은 모든 업무 WAR에서 동일한 구조로 사용한다 |

### 11.3 파일 구성 기준

NSIGHT 업무 WAR는 다음과 같이 설정 파일을 구성한다.
src/main/resources
```text
 ├─ application.yml
 ├─ application-local.yml
 ├─ application-dev.yml
 ├─ application-stg.yml
 ├─ application-prd.yml
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
     └─ sv/
         └─ SvCustomerMapper.xml
```

| 파일 | 역할 | 적용 기준 |
| --- | --- | --- |
| application.yml | 공통 기본 설정 | 모든 환경 공통 |
| application-local.yml | 로컬 개발 설정 | 개발자 PC |
| application-dev.yml | 개발환경 설정 | 개발 서버 |
| application-stg.yml | 검증환경 설정 | 통합/성능/인수 검증 |
| application-prd.yml | 운영환경 설정 | 운영 서버 |
| application-datasource.yml | DB 연결 설정 | RDW/ADW/SESSIONDB/LOGDB |
| application-session.yml | 세션 설정 | Spring Session JDBC |
| application-tcf.yml | TCF 실행정책 | 거래통제, Timeout, 로그 |
| application-gateway.yml | Gateway 설정 | 업무코드 라우팅 |
| application-jwt.yml | JWT 설정 | 토큰 발급·검증 |
| application-cache.yml | Cache 설정 | 공통코드, ServiceId, 정책정보 |
| application-batch.yml | Batch 설정 | Job, Scheduler |
| logback-spring.xml | 로그 설정 | 업무 로그, 거래로그, 감사로그 |

### 11.4 기본 application.yml 표준 구조

application.yml은 환경별 상세값을 직접 많이 넣기보다, 공통 구조와 import 기준을 정의한다.
```yaml
spring:
  application:
    name: sv-service
  profiles:
    active: local
  config:
    import:
      - optional:classpath:application-datasource.yml
      - optional:classpath:application-session.yml
      - optional:classpath:application-tcf.yml
      - optional:classpath:application-cache.yml
server:
  servlet:
    context-path: /sv
mybatis:
  mapper-locations: classpath:/mapper/sv/**/*.xml
  configuration:
    map-underscore-to-camel-case: true
    default-statement-timeout: 3
nsight:
  business-code: SV
```

| 설정 | 기준 | 예시 |
| --- | --- | --- |
| spring.application.name | 업무 서비스명 | sv-service |
| spring.profiles.active | 기본 Profile | local |
| server.servlet.context-path | 업무 Context | /sv |
| mybatis.mapper-locations | 업무 Mapper 경로 | mapper/sv/**/*.xml |
| nsight.business-code | 업무코드 | SV |

### 11.5 환경별 Profile 구성 기준

환경별 Profile은 다음 기준으로 나눈다.
| Profile | 용도 |
| --- | --- |
| DB | 로그 |
| 보안 | local |
| 개발자 PC | H2, Local DB, Dev DB |
| DEBUG 허용 | 완화 가능 |
| dev | 개발 서버 |
| 개발 DB | DEBUG/INFO |
| 기본 적용 | stg |
| 검증 서버 | 검증 DB |
| INFO | 운영과 유사 |
| prd | 운영 서버 |
| 운영 DB | INFO/WARN |
실행 예시는 다음과 같다.

| 엄격 적용 | |

```text
./gradlew :sv-service:bootRun --args='--spring.profiles.active=local'
```

Tomcat WAR 배포 시에는 setenv.sh 또는 환경변수로 Profile을 지정한다.
export JAVA_OPTS="$JAVA_OPTS -Dspring.profiles.active=prd"
export JAVA_OPTS="$JAVA_OPTS -Dfile.encoding=UTF-8"
export JAVA_OPTS="$JAVA_OPTS -Duser.timezone=Asia/Seoul"

### 11.6 서버 / Context Path 설정 기준

업무 WAR는 업무코드와 Context Path가 반드시 일치해야 한다.
```yaml
server:
  port: 8086
  servlet:
    context-path: /sv
    encoding:
      charset: UTF-8
      enabled: true
      force: true
  forward-headers-strategy: framework
```

항목

| 기준 | 설명 |
| --- | --- |
| --- | --- | --- |
| server.port | 로컬에서만 주로 사용 | 운영 Tomcat 배포 시 외부 Tomcat Port 기준 |
| context-path | 업무코드 소문자 | /sv, /ic, /om |
| encoding.charset | UTF-8 | 한글 전문 깨짐 방지 |
| forward-headers-strategy | framework | Apache, Gateway 뒤쪽에서 원 요청 정보 인식 |

업무별 예시는 다음과 같다.
| 업무 | Context Path | Endpoint |
| --- | --- | --- |
| SV | /sv | POST /sv/online |
| IC | /ic | POST /ic/online |
| OM | /om | POST /om/online |
| MG | /mg | POST /mg/online |

### 11.7 세션 설정 기준

NSIGHT는 Spring Session JDBC + SESSIONDB 기준으로 세션을 중앙화한다. Spring Session JDBC 운영 매뉴얼도 세션 중앙화, WAS 독립 세션 관리, 장애 시 세션 유지, 다중 WAR 세션 공유, 사용자 인증 상태 유지를 주요 역할로 정의한다.
```yaml
server:
  servlet:
    session:
      timeout: 60m
      cookie:
        name: JSESSIONID
        http-only: true
        secure: true
        same-site: Lax
spring:
  session:
    store-type: jdbc
    jdbc:
      initialize-schema: never
      table-name: SPRING_SESSION
    timeout: 60m
```

항목

| 기준 | 설명 |
| --- | --- |
| --- | --- | --- |
| Session Timeout | 60분 기준 | 업무 요구에 따라 조정 |
| Cookie Name | JSESSIONID 또는 NSIGHTSID | 전 업무 WAR 기준 통일 |
| HttpOnly | true | JavaScript 접근 차단 |
| Secure | 운영 true | HTTPS 기준 |
| SameSite | Lax 또는 업무 기준 | SSO/iframe 구조 고려 |
| Store Type | jdbc | SESSIONDB 저장 |
| Schema 초기화 | never | 운영에서 자동 생성 금지 |

### 11.8 DataSource 구성 기준

NSIGHT는 DB 목적에 따라 DataSource를 분리한다.
RDW       = 실시간 조회 / Single View
ADW       = 분석 조회 / 대량 조회
SESSIONDB = Spring Session JDBC
LOGDB     = 거래로그 / 감사로그
OMDB      = 운영관리 기준정보

```yaml
spring:
  datasource:
    rdw:
      jdbc-url: ${NSIGHT_RDW_JDBC_URL}
      username: ${NSIGHT_RDW_USERNAME}
      password: ${NSIGHT_RDW_PASSWORD}
      driver-class-name: oracle.jdbc.OracleDriver
    adw:
      jdbc-url: ${NSIGHT_ADW_JDBC_URL}
      username: ${NSIGHT_ADW_USERNAME}
      password: ${NSIGHT_ADW_PASSWORD}
      driver-class-name: oracle.jdbc.OracleDriver
    sessiondb:
      jdbc-url: ${NSIGHT_SESSION_JDBC_URL}
      username: ${NSIGHT_SESSION_USERNAME}
      password: ${NSIGHT_SESSION_PASSWORD}
      driver-class-name: oracle.jdbc.OracleDriver
```

| DB | 사용 목적 | 주의사항 |
| --- | --- | --- |
| RDW | 온라인 실시간 조회 | 장시간 SQL 금지 |
| ADW | 분석성 조회 | 대량 조회 통제 |
| SESSIONDB | 세션 저장 | 전체 업무 WAR 공통 |
| LOGDB | 거래로그, 감사로그 | 쓰기 지연 관리 |
| OMDB | 운영 기준정보 | OM에서 관리 |

RDW/ADW 운영 기준은 실시간 서비스 보호, 분석업무 분리, 대량조회 영향 최소화, 운영 안정성 확보를 목표로 한다.

### 11.9 HikariCP 설정 기준

HikariCP는 DB Connection Pool을 관리하는 핵심 설정이다. HikariCP 운영 표준에서도 HikariCP를 단순 성능 설정이 아니라 DB 자원 통제 장치로 정의한다.
```yaml
spring:
  datasource:
    rdw:
      hikari:
        pool-name: SV-RDW-POOL
        maximum-pool-size: 80
        minimum-idle: 20
        connection-timeout: 3000
        validation-timeout: 3000
        idle-timeout: 600000
        max-lifetime: 1800000
        keepalive-time: 300000
        auto-commit: false
```

| 항목 | 권장 기준 | 설명 |
| --- | --- | --- |
| maximum-pool-size | 업무별 산정 | DB 동시 Connection 상한 |
| minimum-idle | 20 내외 | 기본 유지 Connection |
| connection-timeout | 3초 | Pool 대기 상한 |
| validation-timeout | 3초 | Connection 검증 상한 |
| idle-timeout | 10분 | 유휴 Connection 정리 |
| max-lifetime | 30분 이하 | DB/방화벽 Idle 정리 전 교체 |
| keepalive-time | 5분 | 장시간 유휴 연결 유지 |
| auto-commit | false | 트랜잭션 명시 관리 |

### 11.10 MyBatis 설정 기준

MyBatis는 DAO/Mapper 계층에서 SQL 실행을 담당한다. NSIGHT MyBatis 표준은 MyBatis를 SQL 표준화, DB 접근 경계, Query Timeout, Mapper 표준, Fetch Size, Paging, Full Scan 방지, 관측성 통제 메커니즘으로 정의한다.
```yaml
mybatis:
  mapper-locations: classpath:/mapper/sv/**/*.xml
  type-aliases-package: com.nh.nsight.sv
  configuration:
    map-underscore-to-camel-case: true
    default-statement-timeout: 3
    default-fetch-size: 500
    jdbc-type-for-null: "NULL"
```

항목

| 기준 | 설명 |
| --- | --- |
| --- | --- | --- |
| mapper-locations | 업무코드별 분리 | mapper/sv/**/*.xml |
| type-aliases-package | 업무 Package | com.nh.nsight.sv |
| map-underscore-to-camel-case | true | DB 컬럼과 Java 필드 매핑 |
| default-statement-timeout | 3초 | 장시간 SQL 차단 |
| default-fetch-size | 500 이하 기준 | 대량 Fetch 방지 |
| jdbc-type-for-null | NULL | Null 처리 표준화 |

### 11.11 트랜잭션 / Timeout 설정 기준

Timeout은 온라인 거래, DB 트랜잭션, SQL, 외부연계, Gateway 호출 구간을 분리해서 관리한다.
```yaml
spring:
  transaction:
    default-timeout: 5
nsight:
  tcf:
    timeout:
      online-transaction-seconds: 5
      db-query-seconds: 3
      external-call-seconds: 5
      gateway-call-seconds: 5
```

Timeout 구분

| 기준 | 설명 |
| --- | --- |
| --- | --- | --- |
| Online Transaction Timeout | 5초 | 전체 온라인 거래 제한 |
| Spring Transaction Timeout | 5초 | DB 트랜잭션 제한 |
| MyBatis Query Timeout | 3초 | SQL 실행 제한 |
| Hikari Connection Timeout | 3초 | Pool 대기 제한 |
| External Call Timeout | 5초 | 외부 연계 제한 |
| Gateway Call Timeout | 5초 | Downstream 호출 제한 |

Timeout은 거래통제 테이블에 넣지 않고 별도 Timeout 정책으로 분리하는 것이 원칙이다.
TCF_TRANSACTION_CONTROL
= 이 거래를 허용할 것인가?

TCF_SERVICE_TIMEOUT_POLICY
= 이 거래를 몇 초 안에 끝내야 하는가?

### 11.12 TCF Framework 설정 기준

TCF 설정은 표준 전문 처리, 거래통제, Timeout, 거래로그, 멱등성, 권한검증을 제어한다.
```yaml
nsight:
  tcf:
    enabled: true
    online:
      endpoint: /sv/online
      business-code-check: true
    header:
      required:
        - serviceId
        - transactionCode
        - businessCode
        - user
        - channelId
        - branch
    dispatcher:
      service-id-required: true
      fail-if-handler-not-found: true
    transaction-control:
      enabled: true
      default-allow: false
    transaction-log:
      enabled: true
      processing-status: PROCESSING
    audit-log:
      enabled: true
    idempotency:
      enabled: true
      ttl-seconds: 300
```

설정

| 기준 | 설명 |
| --- | --- |
| --- | --- | --- |
| enabled | true | TCF 처리 사용 |
| business-code-check | true | URL 업무코드와 Header 업무코드 일치 검증 |
| header.required | 필수 Header | 표준 전문 검증 |
| service-id-required | true | ServiceId 필수 |
| fail-if-handler-not-found | true | 미등록 Handler 차단 |
| transaction-control.default-allow | false | 미등록 거래 기본 차단 |
| transaction-log.enabled | true | 거래로그 기록 |
| audit-log.enabled | true | 감사로그 기록 |
| idempotency.enabled | 업무 기준 | 중복요청 통제 |

### 11.13 Gateway 설정 기준

Gateway 설정은 업무코드 기준 라우팅과 Downstream 호출 Timeout을 관리한다.
```yaml
nsight:
  gateway:
    enabled: true
    env-code: local
    routing:
      source: db
      validate-path-business-code: true
      validate-header-business-code: true
    http-client:
      connect-timeout-millis: 3000
      read-timeout-millis: 5000
    default-routes:
      SV: http://localhost:8086/sv/online
      IC: http://localhost:8082/ic/online
      OM: http://localhost:8081/om/online
```

항목

| 기준 | 설명 |
| --- | --- |
| --- | --- | --- |
| env-code | local/dev/stg/prd | 환경별 라우팅 분리 |
| routing.source | db 권장 | OM/Gateway 라우팅 테이블 사용 |
| validate-path-business-code | true | URL 업무코드 검증 |
| validate-header-business-code | true | Header 업무코드 검증 |
| connect-timeout-millis | 3초 | 연결 Timeout |
| read-timeout-millis | 5초 | 응답 대기 Timeout |

### 11.14 JWT / SSO 설정 기준

JWT는 사용자 인증 수단이며, TCF는 계속 거래통제, 권한, 로그, 감사, Timeout을 담당한다.
```yaml
nsight:
  jwt:
    enabled: true
    issuer: nsight-tcf
    access-token-validity-seconds: 1800
    refresh-token-validity-seconds: 7200
    signing-key-location: ${NSIGHT_JWT_KEY_LOCATION}
    token-store: db
  sso:
    enabled: true
    token-verify-url: ${NSIGHT_SSO_VERIFY_URL}
    connect-timeout-millis: 3000
    read-timeout-millis: 5000
```

항목

| 기준 | 설명 |
| --- | --- |
| --- | --- | --- |
| Access Token | 30분 예시 | 짧게 유지 |
| Refresh Token | 2시간 예시 | 업무 기준 조정 |
| Signing Key | 외부화 | Git 저장 금지 |
| Token Store | DB | 폐기·추적 가능 |
| SSO Verify URL | 환경변수 | 운영 정보 보호 |

### 11.15 Cache 설정 기준

Cache는 업무 데이터가 아니라 기준정보와 정책정보 중심으로 사용한다. Cache 구조 설계에서도 업무 데이터는 캐시하지 않고 공통코드, ServiceId, 정책정보 같은 기준정보를 Cache 대상으로 둔다.
```yaml
spring:
  cache:
    type: jcache
    jcache:
      config: classpath:ehcache.xml
nsight:
  cache:
    enabled: true
    names:
      common-code:
        ttl-seconds: 300
      service-catalog:
        ttl-seconds: 300
      transaction-control:
        ttl-seconds: 60
      timeout-policy:
        ttl-seconds: 60
```

| Cache 대상 | 사용 여부 | 설명 |
| --- | --- | --- |
| 공통코드 | 사용 | 코드그룹, 코드값 |
| ServiceId 카탈로그 | 사용 | Handler 매핑 기준 |
| 거래통제 정책 | 사용 | Allow-List 조회 |
| Timeout 정책 | 사용 | 서비스별 Timeout |
| 사용자 업무 데이터 | 금지 | 정합성 위험 |
| 대량 조회 결과 | 금지 | 메모리 위험 |

### 11.16 Batch 설정 기준

Batch 설정은 Job 실행, Scheduler, 이력 저장, 실패 재처리 기준을 포함한다.
```yaml
nsight:
  batch:
    enabled: true
    scheduler:
      enabled: true
    jobs:
      ap-status-collect:
        enabled: true
        cron: "0 */1 * * * *"
      db-status-collect:
        enabled: true
        cron: "0 */1 * * * *"
      session-status-collect:
        enabled: true
        cron: "0 */5 * * * *"
```

항목

| 기준 | 설명 |
| --- | --- |
| --- | --- | --- |
| batch.enabled | 업무 기준 | Batch 기능 사용 여부 |
| scheduler.enabled | 업무 기준 | Scheduler 사용 여부 |
| Job별 enabled | 필수 | 개별 Job 제어 |
| Cron | 운영 기준 | 실행 주기 관리 |
| 실행이력 | DB 저장 | OM에서 조회 |
| 실패 재처리 | 정책화 | 수동/자동 재처리 기준 |

### 11.17 로그 / MDC 설정 기준

로그는 GUID, TraceId, ServiceId, TransactionCode, BusinessCode 기준으로 추적 가능해야 한다.
```yaml
logging:
  level:
    root: INFO
    com.nh.nsight: INFO
    org.mybatis: WARN
  file:
    path: /logs/nsight/sv
nsight:
  logging:
    mdc:
      enabled: true
      keys:
        - guid
        - traceId
        - serviceId
        - transactionCode
        - businessCode
        - user
        - branch
    masking:
      enabled: true
      fields:
        - customerName
        - rrn
        - accountNo
        - phoneNo
```

로그 항목
| 기준 | GUID | 필수 | TraceId | 필수 |
| --- | --- | --- | --- | --- |
| ServiceId | 필수 | TransactionCode | 필수 | BusinessCode |
| 필수 | User / Branch | 감사 기준 | 개인정보 | 마스킹 필수 |

SQL Parameter
운영 로그 출력 제한

### 11.18 Actuator / Health Check 설정 기준

운영 배포 후 각 업무 WAR는 Health Check를 제공해야 한다.
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
  health:
    db:
      enabled: true

| Endpoint | 사용 목적 |
| --- | --- |
| /actuator/health | 기본 Health Check |
| /actuator/info | 애플리케이션 정보 |
| /actuator/metrics | JVM, HTTP, Pool 지표 |
| /actuator/prometheus | 모니터링 연계 |
| /actuator/threaddump | 장애 분석 |

운영에서는 Health Check를 단순 프로세스 확인이 아니라 DB, SESSIONDB, TCF, Cache, 대표 ServiceId Smoke Test까지 확장하는 것이 좋다.

### 11.19 Secret 관리 기준

다음 값은 application.yml에 직접 쓰지 않는다.

| Secret 항목 | 관리 방식 | DB Password |
| --- | --- | --- |
| 환경변수, Secret Manager | JWT Signing Key | 외부 파일 또는 Secret |

| API Key | 환경변수 | SSO 연계 Secret |
| --- | --- | --- |
| 환경변수 | 암호화 Key | Secret Manager |
예시는 다음과 같다.

| 운영 DB URL | 운영 설정 저장소에서 관리 |

```yaml
spring:
  datasource:
    rdw:
      username: ${NSIGHT_RDW_USERNAME}
      password: ${NSIGHT_RDW_PASSWORD}
```

금지 예시는 다음과 같다.
```yaml
spring:
  datasource:
    rdw:
      username: nsight
      password: real-production-password
```

### 11.20 업무별 application.yml 예시

SV 업무 기준 예시는 다음과 같다.
```yaml
spring:
  application:
    name: sv-service
  profiles:
    active: local
  session:
    store-type: jdbc
    timeout: 60m
server:
  port: 8086
  servlet:
    context-path: /sv
    session:
      timeout: 60m
      cookie:
        name: JSESSIONID
        http-only: true
        secure: false
        same-site: Lax
  forward-headers-strategy: framework
mybatis:
  mapper-locations: classpath:/mapper/sv/**/*.xml
  type-aliases-package: com.nh.nsight.sv
  configuration:
    map-underscore-to-camel-case: true
    default-statement-timeout: 3
    default-fetch-size: 500
nsight:
  business-code: SV
  tcf:
    enabled: true
    online:
      endpoint: /sv/online
      business-code-check: true
    timeout:
      online-transaction-seconds: 5
      db-query-seconds: 3
      external-call-seconds: 5
    transaction-control:
      enabled: true
      default-allow: false
    transaction-log:
      enabled: true
    audit-log:
      enabled: true
logging:
  level:
    root: INFO
    com.nh.nsight: DEBUG
```

운영에서는 secure: true, 로그 레벨 INFO, 운영 DB Secret 외부화, Actuator 접근통제를 적용해야 한다.

### 11.21 환경별 차이 예시

| 항목 | local |
| --- | --- |
| dev | stg |
| prd | server.port |
| 업무별 로컬 포트 | 개발 서버 포트 |
| 검증 포트 | Tomcat 기준 |
| DB | H2/Dev |
| Dev DB | STG DB |
| 운영 DB | Cookie Secure |
| false 가능 | 환경 기준 |
| true 권장 | true 필수 |
| Log Level | DEBUG 가능 |
| DEBUG/INFO | INFO |
| INFO/WARN | Actuator Detail |
| 상세 가능 | 제한 |
| 제한 | 인증 후 제한 |
| Schema Init | 가능 |
| 제한 | 금지 |
| 금지 | Secret |
| 로컬 환경변수 | Dev Secret |
| STG Secret | 운영 Secret |

### 11.22 application.yml 금지사항

| 금지사항 | 사유 |
| --- | --- |
| 운영 DB 비밀번호 직접 작성 | 보안 사고 위험 |
| 운영 Profile을 로컬 기본값으로 지정 | 운영 설정 오사용 위험 |
| default-allow: true 운영 적용 | 미등록 거래 실행 위험 |
| Query Timeout 미설정 | 장시간 SQL 위험 |
| Connection Timeout 과다 설정 | Thread 대기 증가 |
| Mapper 경로 공통 혼재 | SQL 관리 혼란 |
| 업무코드와 Context 불일치 | Gateway/Apache 라우팅 오류 |
| 로그에 개인정보 출력 | 보안·감사 위반 |
| 운영에서 DEBUG 로그 상시 사용 | 성능 저하 및 정보노출 |
| 운영 서버에서 직접 파일 수정 | 형상관리 이탈 |

### 11.23 application.yml 점검 체크리스트

| 점검 항목 | 확인 |
| --- | --- |
| spring.application.name이 업무명과 일치하는가 | □ |
| server.servlet.context-path가 업무코드와 일치하는가 | □ |
| nsight.business-code가 업무코드와 일치하는가 | □ |
| Profile이 환경별로 분리되어 있는가 | □ |
| 운영 Secret이 소스에 포함되지 않았는가 | □ |
| RDW/ADW/SESSIONDB DataSource가 분리되어 있는가 | □ |
| HikariCP Timeout이 설정되어 있는가 | □ |
| MyBatis Query Timeout이 설정되어 있는가 | □ |
| Mapper XML 경로가 업무코드 기준인가 | □ |
| Session Timeout이 정의되어 있는가 | □ |
| Spring Session JDBC가 설정되어 있는가 | □ |
| TCF 거래통제가 활성화되어 있는가 | □ |
| 미등록 거래 기본 차단 기준인가 | □ |
| 거래로그 설정이 활성화되어 있는가 | □ |
| 감사로그 설정이 필요한 업무에 적용되어 있는가 | □ |
| Actuator Health Check가 노출되어 있는가 | □ |
| 운영에서 Actuator 상세정보가 보호되는가 | □ |
| 로그 마스킹 설정이 있는가 | □ |
| 운영에서 DEBUG 로그가 꺼져 있는가 | □ |

### 11.24 마무리말

application.yml은 단순 설정 파일이 아니라 NSIGHT TCF Framework의 운영 기준 파일이다.
개발자는 application.yml을 작성할 때 다음 연결 관계를 항상 유지해야 한다.
업무코드
```text
   ↓
Context Path
↓
WAR 파일명
↓
Mapper 경로
↓
ServiceId
↓

```

거래통제 / Timeout / 로그 / 세션

이 연결이 깨지면 로컬에서는 실행되더라도 운영에서는 Apache 라우팅, Gateway 라우팅, 세션 공유, 거래통제, Timeout, 거래로그, Health Check가 정상 동작하지 않을 수 있다.

### 11.25 시사점

NSIGHT에서 좋은 application.yml은 값이 많은 설정 파일이 아니다.환경별로 분리되어 있고, 운영 Secret이 보호되며, Timeout과 Pool이 명확하고, TCF 실행정책과 관측성이 포함된 설정 파일이다.
결론적으로 application.yml 구성 기준은 다음 한 문장으로 정리할 수 있다.
application.yml은 업무 WAR가 어떤 Context로 실행되고, 어떤 DB와 세션을 사용하며, 어떤 TCF 정책으로 거래를 통제하고, 어떤 로그와 Health Check로 운영될지를 정의하는 실행 표준서이다.

## 소스·관련 문서

| 참고 |
|------|

> znsight-guide-word: `통합 (16).docx`

| [20-Spring환경설정.md](../zman/20-Spring환경설정.md) |

## 코드베이스 정정 (develop 기준)

| 항목 | 값 |
|------|-----|
| 업무 WAR | ic, pc, ms, sv, pd, eb, ep, ss, mg + tcf-om |
| ztomcat deploy | `ztomcat/deploy-wars.sh` 13 WAR |
| buildZtomcatWars | 15 WAR |
| bootRun | gateway 8100, uj 8102, jwt 8110, ui 8099 |


---

← [10. bootRun / Tomcat WAR 배포 차이](./10-bootRun-Tomcat-WAR-차이.md) · [12. 애플리케이션 계층구조](./12-애플리케이션-계층구조.md) →