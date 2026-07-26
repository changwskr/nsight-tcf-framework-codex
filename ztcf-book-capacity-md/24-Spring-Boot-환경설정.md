# 24. Spring Boot 환경설정

> 제4부. 솔루션별 환경설정 가이드

## NSIGHT 1차 표준 전제

| 항목 | 기준값 |
|------|--------|
| 지점 수 | 3,600 |
| 지점당 사용자 | 6 |
| 전체 사용자 | 21,600 |
| 설계 세션 | 26,000~28,000 |
| TPS | 360 / 720 / 1,080 |
| 목표 응답 | p95 3초 이하 |
| 기준 VM | 8 vCPU / 32GB |
| VM당 TPS | 250 (보수) |
| AP 구조 | 2센터 Active-Active |

> 출처: `znsight-capacity-word` · [13단계 요약](./zNSIGHT-용량산정-전체-흐름.md)


**Spring Boot** — Transaction·Graceful·Actuator·Async.

## 원문 기반 본문

NSIGHT Spring Boot 설정 가이드마케팅플랫폼 / SingleView / 정보계 온라인 AP 기준구분기준문서 목적Spring Boot 기반 온라인 AP의 설정 기준, 실제 설정 위치, 예시, 검증 방법을 표준화한다.

적용 대상마케팅 AP, SingleView AP, 일반 업무 AP, 외부연계 호출 AP, 감사/로그/운영 공통 모듈기준 사용자3,600개 지점 × 6명 = 21,600명기준 세션21,600 세션 + 여유율 20~30% = 26,000~28,000 세션기준 TPS360 TPS 기본 운영 / 720 TPS 피크 설계 / 1,080 TPS 스트레스 테스트기준 VMIaaS

8 vCPU / 32GB, VM당 약 250 TPS 산정 기준작성 원칙설정값은 선도개발과 성능테스트 전 기준선이며, 실측 후 보정한다.

> **핵심**: Spring Boot 설정은 단순 property 나열이 아니라 Runtime에서 실제 거래가 안전하게 흐르도록 하는 공통 메커니즘이다. 세션, 트랜잭션, DB Pool, SQL Timeout, 외부연계 Timeout, 로그, 보안, 모니터링은 반드시 함께 정합성을 맞춰야 한다.

목차

1. Spring Boot 설정의 위치와 역할

## 2. 기준 용량 및 설정 원칙

## 3. Profile / 외부화 설정 구조4. application.yml 표준 구조

## 5. Server / Session / Cookie 설정

## 6. Transaction 설정

## 7. DataSource / HikariCP 설정

## 8. MyBatis / SQL Timeout 설정

## 9. CruzAPIM / 외부연계 Timeout 설정

## 10. Resilience4j 설정

## 11. Async / ThreadPool 설정

## 12. Logging / GUID / MDC / 감사로그 설정

## 13. Error / Response / Idempotency 설정

## 14. Security / Header / CORS / CSRF 설정

## 15. Actuator / Monitoring 설정

## 16. 운영 배포 및 환경변수 설정

## 17. 성능검증 및 운영 임계치

## 18. 전체 application.yml 템플릿

## 19. 최종 적용 체크리스트

## 20. 참고 공식 문서

## 1. Spring Boot 설정의 위치와 역할Spring Boot는 Tomcat 위에서 업무 로직을 실행하는 단순 애플리케이션 프레임워크가 아니라, 세션·쿠키·트랜잭션·DB Pool·SQL Timeout·외부연계·로그·보안·모니터링 정책을 실제 Runtime에 적용하는 실행 계층이다.

Spring Boot 설정 적용 위치[WebTopSuite]
↓[L4 / Apache]
↓[Tomcat Connector / Thread]
↓[Spring Boot Application]   ├─ Controller / Facade / Service / Rule   ├─ Transaction / Validation / Error / Response   ├─ HikariCP / MyBatis / RDW·ADW 접근   ├─ CruzAPIM Client / Resilience4j   └─ Logging / MDC / Actuator / Security
↓[RDW / ADW / 외부연계 시스템]설정 영역Spring Boot에서의 역할주요 설정 파일/위치운영 영향Server / Servlet세션, 쿠키, 컨텍스트, 내장 Tomcat 설정 일부 관리application.yml세션 유지, 요청 처리 안정성Transaction업무 거래의 수행 한도, ReadOnly, Rollback 정책 관리@Transactional, TransactionConfigThread/DB 자원 장시간 점유 방지DataSource / HikariRDW/ADW/감사DB 접속, Pool 대기, 수명, 검증 관리application.yml, DataSourceConfigDB 세션 고갈 및 자원 경합 방지MyBatisSQL Timeout, Fetch Size, Mapper 위치, SQL 표준 관리application.yml, MyBatisConfigRDW 조회 성능 보호External ClientCruzAPIM 등 외부연계 Timeout, Header, 오류 매핑 관리ClientConfig, Resilience4j연계 장애 전파 차단Logging / MDCGUID, TraceId, UserId, BranchId를 로그에 자동 삽입Filter, Interceptor, logback-spring.xml장애추적, 감사, 운영 대시보드ActuatorHealth, Metrics, Prometheus, Liveness/Readiness 제공management.*운영 감시 및 자동화 연계

## 2. 기준 용량 및 설정 원칙항목기준값설명지점/사용자3,600개 × 6명 = 21,600명전체 등록 사용자 및 최대 로그인 가능 사용자 기준설계 세션26,000~28,000전체 사용자 + 20~30% 여유율기본 운영360 TPS5% 동시 요청률, 목표 응답시간 3초 기준피크 설계720 TPS

10% 동시 요청률, 목표 응답시간 3초 기준스트레스 테스트1,080 TPS15% 동시 요청률, 한계 검증 기준VM 기준8 vCPU / 32GB정보계 Java/Tomcat AP의 기본 Scale-Out 단위Spring Boot AP 기준200~250 TPS / VM일반 마케팅 AP는 연계 대기를 고려해 200 TPS, SingleView는 250 TPS 기준목표 응답시간p95 3초 이하평균 응답시간은 1.0~1.5초 수준으로 관리 권장산정 원칙: TPS, Tomcat Thread, DB Pool은 전체 사용자 수가 아니라 실제 동시 요청자 기준으로 산정한다. 세션 수는 로그인 유지 규모이고, TPS는 같은 순간 서버 자원을 점유하는 요청 수이다.3. Profile / 외부화 설정 구조Spring Boot 설정은 같은 소스코드를 유지하면서 DEV, SIT, UAT, PRD 환경별 값을 외부화하는 방식으로 관리해야 한다. 운영 비밀번호, DB 접속정보, 인증키 등 민감정보는 소스 저장소에 넣지 않고 환경변수나 Secret 저장소로 분리한다.

구분파일/설정역할운영 기준공통 설정application.yml모든 환경 공통 기본값구조·공통 기본값만 포함개발 환경application-dev.yml개발 DB, 개발 CruzAPIM, 낮은 로그 레벨운영 데이터 접속 금지통합 환경application-sit.yml통합 테스트 DB, 연계 테스트 주소성능값과 분리성능 환경application-perf.yml성능 테스트용 Pool, Timeout, 로그 레벨운영과 동일 또는 유사운영 환경application-prd.yml운영 DB, 운영 연계, 보안값파일 접근권한 및 Secret 관리 필수환경 변수SPRING_PROFILES_ACTIVE, DB_URL 등배포 시점 주입CI/CD와 eCAMS 연계 검토명령행 옵션--spring.profiles.active=prd긴급 보정 또는 운영 자동화변경 이력 관리 필요Profile 및 외부 설정 실행 예시# 실행 예시java -jar nsight-marketing.jar   --spring.profiles.active=prd   --spring.config.additional-location=/app/config/# Linux 환경변수 예시export SPRING_PROFILES_ACTIVE=prdexport DB_RDW_URL=jdbc:oracle:thin:@//rdw-scan:1521/RDWexport DB_RDW_USER=NSIGHT_APP4. application.yml 표준 구조application.yml은 영역별로 일관된 순서와 네이밍을 사용해야 한다. 프로젝트별 임의 속성은 nsight.* 하위에 배치하여 Spring Boot 표준 속성과 구분한다.

상위 영역관리 항목주요 속성 예시비고spring프로파일, Datasource, Transaction, Jackson, Taskspring.datasource, spring.transaction, spring.jacksonSpring Boot 표준server포트, 세션, 쿠키, Tomcatserver.servlet.session, server.tomcatWAS 경계 설정mybatisMapper, TypeAlias, SQL Timeoutmybatis.configuration.default-statement-timeoutSQL 통제managementActuator, Metrics, Healthmanagement.endpoints.web.exposure.include운영 감시logging로그 레벨, 로그 파일, 패턴logging.level, logging.file.name상세 패턴은 logback 권장resilience4jCircuitBreaker, Bulkhead, Retry, TimeLimiterresilience4j.circuitbreaker.instances.*외부연계 장애 격리nsight업무 공통 정책nsight.timeout, nsight.audit, nsight.guid프로젝트 표준 속성

## 5. Server / Session / Cookie 설정설정 항목권장값설정 위치설정 예시설명Session Idle Timeout60m 기본server.servlet.session.timeouttimeout: 60m유휴 세션 만료. 상담 업무는 120~180분 예외 검토Cookie HttpOnlytrueserver.servlet.session.cookie.http-onlyhttp-only: trueJavaScript 쿠키 접근 차단Cookie Securetrueserver.servlet.session.cookie.securesecure: trueHTTPS에서만 쿠키 전송Cookie SameSiteLaxserver.servlet.session.cookie.same-sitesame-site: LaxCSRF 완화. 크로스도메인은 None + Secure 검토Absolute Session Timeout8시간Filter/InterceptorloginTime + 8hSpring 기본 기능이 아니라 업무공통에서 구현Context Path필요 시 명시server.servlet.context-path/marketingL4/Apache 경로 정책과 일치 필요Compression정적/대용량 응답 검토server.compression.*enabled: trueAPI 응답 특성에 따라 적용Session / Cookie / 내장 Tomcat 기본 예시server:  servlet:    session:      timeout: 60m      cookie:        http-only: true        secure: true        same-site: Lax  tomcat:    threads:      max: 500      min-spare: 100    accept-count: 500    max-connections: 10000    connection-timeout: 8s구분: 세션 Timeout은 로그인 상태 유지시간이고, Transaction Timeout은 한 거래가 DB/연계 자원을 점유하는 최대 시간이다. 두 값을 혼동하면 안 된다.6. Transaction 설정온라인 정보계 거래는 사용자가 기다리는 동기 거래이므로 트랜잭션은 짧게 끊어야 한다. 긴 처리나 대량 처리는 비동기, 배치, Queue로 전환한다.

거래 유형권장 TimeoutReadOnlyRollback 기준적용 방식일반 조회3~5초trueRuntimeException / QueryTimeout@Transactional(timeout=5, readOnly=true)SingleView 조회3~5초trueSQL Timeout 우선@Transactional(timeout=5, readOnly=true)일반 변경5초 이내false업무 오류별 명확화@Transactional(timeout=5)외부연계 동기 거래5~8초업무별Timeout/연계 오류 매핑CircuitBreaker + TimeLimiter감사로그 저장비동기 권장false업무거래와 분리 검토@Async 또는 Queue장시간/대량 처리동기 금지업무별배치/비동기 재처리Batch/Queue 전환Transaction 적용 예시

```java
@Transactional(timeout = 5, readOnly = true)public SingleViewResponse getSingleView(SingleViewRequest request) {    // 1. 권한 확인    // 2. RDW 조
```


회    // 3. 마스킹 적용    // 4. 표준 응답 조립    return response;}

```java
@Transactional(timeout = 5, rollbackFor = Exception.class)public MarketingResult executeCampaignAction(Command command) {    // 변경 거래는 중복방지키와 상태
```


저장을 함께 검토한다.    return result;}7. DataSource / HikariCP 설정NSIGHT는 RDW와 ADW의 역할을 분리하므로 Spring Boot 설정에서도 DataSource와 SQL 사용 기준을 명확히 분리해야 한다. 온라인 AP는 RDW 중심, 분석성 조회는 ADW 중심으로 통제한다.

설정 항목일반 APSingleView AP설정 위치설명maximumPoolSize40~5050~60spring.datasource.*.hikari.maximum-pool-sizeDB 접속 상한. AP 수 × Pool 수 = DB 총 세션minimumIdle10~1515~20minimum-idle피크 진입 지연 방지connectionTimeout2~3초2~3초connection-timeoutPool에서 Connection 획득 대기시

간. SQL 수행시간 아님validationTimeout3초 이하3초 이하validation-timeoutConnection 유효성 검증 시간idleTimeout10분10분idle-timeout미사용 Connection 반환maxLifetime30분 이하30분 이하max-lifetimeDB/L4/방화벽 Idle보다 짧게 설정keepaliveTime5분5분keepalive-timeidle connection 유지. maxLifetime보다 작아야 함autoCommitfalsefalseauto-commitSpring Transaction 통제RDW HikariCP 설정 예시spring:  datasource:    rdw:      jdbc-url: ${DB_RDW_URL}      username: ${DB_RDW_USER}      password: ${DB_RDW_PASSWORD}      hikari:        maximum-pool-size: 50        minimum-idle: 10        connection-timeout: 3000        validation-timeout: 3000        idle-timeout: 600000        max-lifetime: 1800000        keepalive-time: 300000        auto-commit: false

> **주의**: Hikari connectionTimeout은 DB SQL 실행시간이 아니라 Connection Pool에서 Connection을 얻기 위한 대기시간이다. SQL 실행시간은 MyBatis statement timeout 또는 JDBC queryTimeout으로 별도 통제한다.8. MyBatis / SQL Timeout 설정설정 항목권장값설정 위치설명mapper-locationsclasspath:/mapper/**/*.xmlmybatis.mapper-locations업무 컴포넌트별 Mapper 분리type-aliases-package도메인 모델 패키지mybatis.type-aliases-packageXML 가독성 확보map-underscore-to-camel-casetruemybatis.configuration.*DB 컬럼과 Java 필드 매핑default-statement-timeout3초mybatis.configuration.default-statement-timeoutSQL 기본 수행 한도default-fetch-size100~500mybatis.configuration.default-fetch-size대량 조회 메모리 점유 완화Full Scan금지SQL 리뷰 기준RDW 자원 보호분석성 SQLADW 전용SQL 표준/리뷰RDW와 ADW 자원 경계 준수MyBatis 설정 예시mybatis:  mapper-locations: classpath:/mapper/**/*.xml  type-aliases-package: com.nh.nsight.marketing.domain  configuration:    map-underscore-to-camel-case: true    default-fetch-size: 300    default-statement-timeout: 39. CruzAPIM / 외부연계 Timeout 설정외부연계는 장애 전파의 주요 원인이므로 Connect Timeout, Read Timeout, Circuit Breaker, Bulkhead, 표준 오류코드 매핑을 함께 적용한다.

항목권장값설정 위치설명base-url환경별 분리nsight.cruzapim.base-urlDEV/SIT/PRD 분리connect-timeout2~3초ClientConfig연계 시스템 연결 실패 판단read-timeout3~5초ClientConfig응답 대기 한도retry기본 금지RetryConfig중복 거래와 부하 증폭 방지GUID 전달필수Header InterceptorX-GUID, X-Trace-Id 전달오류 매핑필수ErrorCodeConfigAPIM_TIMEOUT, APIM_UNAVAILABLE 등 표준화CruzAPIM 설정 예시nsight:  cruzapim:    base-url: ${CRUZAPIM_BASE_URL}    connect-timeout: 3s    read-timeout: 5s    retry-enabled: false    default-headers:      source-system-id: NSIGHT-MKT      target-system-id: CRUZAPIM10. Resilience4j 설정Resilience4j는 외부연계 장애가 Spring Boot AP 전체로 확산되지 않도록 격리하는 설정이다. 동기 거래의 Retry는 기본 금지하고, CircuitBreaker와 Bulkhead를 우선 적용한다.

패턴권장 적용권장값 예시설명CircuitBreakerCruzAPIM/외부연계failureRateThreshold 50%오류율 증가 시 호출 차단Bulkhead외부연계 호출maxConcurrentCalls 50~100연계 대기로 인한 Thread 고갈 방지TimeLimiter비동기/외부호출timeoutDuration 5s연계 응답 대기 상한Retry조회성 일부만maxAttempts 1~2변경 거래에는 기본 금지RateLimiter필요 시업무별 한도연계 시스템 보호Resilience4j 설정 예시resilience4j:  circuitbreaker:    instances:      cruzapim:        registerHealthIndicator: true        slidingWindowSize: 100        minimumNumberOfCalls: 20        failureRateThreshold: 50        waitDurationInOpenState: 30s  bulkhead:    instances:      cruzapim:        maxConcurrentCalls: 100        maxWaitDuration: 0  timelimiter:    instances:      cruzapim:        timeoutDuration: 5s11. Async / ThreadPool 설정감사로그, 거래로그 저장, 알림, 이벤트 후처리처럼 사용자 응답을 지연시키면 안 되는 작업은 비동기 처리 대상으로 분리한다. 단, 비동기 ThreadPool도 무제한으로 두면 장애 시 Queue가 폭증하므로 명확한 상한이 필요하다.

ThreadPool용도Core/Max 권장Queue 권장운영 기준auditExecutor고객정보 조회 감사로그core 10 / max 301,000실패 시 재처리 Queue 또는 별도 저장소 검토logExecutor거래로그 비동기 저장core 10 / max 302,000사용자 응답 지연 방지eventExecutor이벤트 후처리core 20 / max 502,000Kafka/EBM과 분리notificationExecutor알림/통보core 5 / max 20500외부 연계 장애 격리Spring TaskExecutor 기본 예시spring:  task:    execution:      pool:        core-size: 20        max-size: 50        queue-capacity: 2000        keep-alive: 60s      thread-name-prefix: nsight-async-12. Logging / GUID / MDC / 감사로그 설정Spring Boot 설정에서 가장 중요한 운영 기준은 GUID 기반 End-to-End 추적이다. 모든 요청은 Filter 또는 Interceptor에서 GUID를 생성·검증하고 MDC에 주입해야 한다.

로그 항목필수 여부생성/설정 위치설명guid필수Header Filter거래 단위 고유 식별자traceId필수Filter / 연계 Adapter내부 호출 및 연계 흐름 추적transactionId필수Controller/Facade업무 거래 식별userId필수세션/인증 정보사용자 추적branchId필수세션/인증 정보지점 추적serviceId필수Controller/전문 Header서비스 단위 분석elapsedTime필수Filter/AOP응답시간 측정sqlTime권장MyBatis Interceptor/APMDB 병목 식별errorCode필수ExceptionHandler표준 오류 분석로그 설정 예시logging:  level:    root: INFO    com.nh.nsight: INFO    org.springframework.transaction: WARN    com.zaxxer.hikari: INFO# logback-spring.xml 패턴 예시# %d{yyyy-MM-dd HH:mm:ss.

SSS} %-5level [%thread]# guid=%X{guid} traceId=%X{traceId} userId=%X{userId}# %logger{36} - %msg%n13. Error / Response / Idempotency 설정온라인 거래의 오류처리는 예외를 잡는 수준이 아니라 표준 응답, 표준 오류코드, GUID 추적, 재처리 판단까지 연결되어야 한다.

설정/기능권장 기준구현 위치설명

공통 응답 포맷header/body/error 구조ResponseAdvice모든 API 응답 형식 통일표준 오류코드업무/검증/권한/DB/연계/시스템ErrorCode Enum/Config사용자 메시지와 운영 메시지 분리GlobalExceptionHandler필수@RestControllerAdviceStackTrace 사용자 노출 금지IdempotencyKey변경/연계 거래 필수Header/DB 상태 저장Timeout 후 중복 처리 방지거래 상태 조회 APITimeout 대응 필수TransactionStatusService처리 전/중/완료 상태 판단Retry 정책동기 변경 거래 금지RetryConfig조회성 거래만 제한적으로 허용오류/중복방지 정책 예시nsight:  error:    expose-stacktrace: false    default-system-error-code: SYS-9999    default-validation-error-code: VAL-4000  idempotency:    enabled: true    header-name: X-Idempotency-Key    status-retention-minutes: 3014. Security / Header / CORS / CSRF 설정금융권 정보계 Spring Boot 설정은 보안을 개발 이후 점검이 아니라 기본 설정으로 내재화해야 한다. 쿠키 보안, 보안 Header, CORS, CSRF, 관리자 엔드포인트 보호가 핵심이다.

항목권장값설정 위치설명Session Cookie Securetrueserver.servlet.session.cookie.secureHTTPS 전송 강제Session Cookie HttpOnlytrueserver.servlet.session.cookie.http-onlyXSS 세션 탈취 완화SameSiteLaxserver.servlet.session.cookie.same-siteCSRF 완화CORS허용 Origin 명시SecurityConfig/WebMvcConfig전체 허용 금지CSRF업무 특성별 검토SecurityConfig세션 기반 화면은 적용 우선Actuator 보안내부망/인증 제한SecurityConfig/망분리health 외 상세정보 제한Header SecurityX-Frame-Options 등SecurityConfigClickjacking 및 기본 방어보안 설정 예시

```yaml
server:  servlet:    session:      cookie:        http-only: true        secure: true        same-site: Laxnsight:  security:    allowed-origins:      - https://webtopsuite.nh.local    actuator-admin-role: ROLE_SYSTEM_ADMIN15. Actuator / Monitoring 설정Actuator는 운영 감시와 L4/Apache Health Check의 근거가 된다. 운영에서는 노출 Endpoint를 최소화하고, Metrics는 Prometheus/APM과 연계한다.

Endpoint/Metric권장 노출활용
```
주의사항health노출L4/Apache/운영 Health Check상세정보는 내부망 또는 권한 제한info선택버전/빌드 정보 확인민감정보 포함 금지metrics내부망JVM, HTTP, Hikari, Tomcat 지표 확인운영자만 접근prometheus내부망Prometheus 수집인증/망분리 필수loggers제한긴급 로그 레벨 변경변경 이력 필요env/configprops운영 비노출설정 확인비밀번호 노출 위험heapdump/threaddump운영 비노출 또는 강력 제한장애 분석개인정보/성능 리스크Actuator 설정 예시management:  endpoints:    web:      exposure:        include: health,info,metrics,prometheus  endpoint:    health:      show-details: when_authorized      probes:        enabled: true  metrics:    tags:      application: nsight-marketing      system: marketing-platform16. 운영 배포 및 환경변수 설정구분설정 방식예시운영 기준Profile환경변수SPRING_PROFILES_ACTIVE=prd운영 배포 시 명시DB URL환경변수/SecretDB_RDW_URL소스 저장소 저장 금지DB PasswordSecretDB_RDW_PASSWORD평문 파일 금지CruzAPIM URL환경별 설정CRUZAPIM_BASE_URLDEV/SIT/PRD 분리Log Path환경변수LOG_PATH=/logs/nsightAP별 디렉터리 분리App ID환경변수APP_ID=marketing-ap-01MDC/모니터링 태그 활용Center ID환경변수CENTER_ID=DC1센터 장애 분석 및 세션 정책 활용운영 환경변수 예시# systemd EnvironmentFile 예시SPRING_PROFILES_ACTIVE=prdAPP_ID=marketing-ap-01CENTER_ID=DC1LOG_PATH=/logs/nsight/marketing-ap-01DB_RDW_URL=jdbc:oracle:thin:@//rdw-scan:1521/RDWCRUZAPIM_BASE_URL=https://cruzapim.nh.local17. 성능검증 및 운영 임계치검증 항목WarningCritical조치 기준HTTP p95 응답시간2.5초 이상3초 초과SQL/연계/Thread 병목 분석HTTP 오류율1% 이상3% 이상오류코드별 원인 분류Tomcat Busy Thread70% 이상85% 이상Thread dump 및 DB/연계 대기 확인Hikari Active70% 이상85% 이상Pool Wait/SQL 지연 확인Hikari Pending지속 발생급증Pool 부족 또는 SQL 지연 판단DB Query p95500ms 이상1초 이상SQL 튜닝/인덱스/ADW 분리 검토GC Pause p95200ms 이상500ms 이상Heap/GC/객체 생성 분석CircuitBreaker Open발생지속연계 시스템 상태 및 우회 정책 확인Audit Log Queue70% 이상90% 이상비동기 저장 지연 조치성능 테스트는 360 TPS, 720 TPS, 1,080 TPS 순서로 단계적으로 수행한다.

테스트 중에는 HTTP 응답시간만 보지 말고 Tomcat Thread, Hikari Pool, SQL Time, GC Pause, 연계 Timeout을 함께 본다.

성능 기준 통과 후에도 운영 모니터링 지표와 동일한 이름으로 대시보드를 구성해야 한다.

## 18. 전체 application.yml 템플릿아래 템플릿은 운영 복붙용 완성본이 아니라 프로젝트 표준의 출발점이다. 운영 URL, 계정, 비밀번호, 서버명, Pool 값은 환경별로 분리하여 적용한다.application.yml 통합 템플릿spring:  profiles:    active: ${SPRING_PROFILES_ACTIVE:dev}  transaction:    default-timeout: 5s  jackson:    time-zone: Asia/Seoul    default-property-inclusion: non_null  datasource:    rdw:      jdbc-url: ${DB_RDW_URL}      username: ${DB_RDW_USER}      password: ${DB_RDW_PASSWORD}      hikari:        maximum-pool-size: 50        minimum-idle: 10        connection-timeout: 3000        validation-timeout: 3000        idle-timeout: 600000        max-lifetime: 1800000        keepalive-time: 300000        auto-commit: false

```yaml
server:  servlet:    session:      timeout: 60m      cookie:        http-only: true        secure: true        same-site: Lax  tomcat:    threads:      max: 500      min-spare: 100    accept-count: 500    max-connections: 10000    connection-timeout: 8s
```
mybatis:  mapper-locations: classpath:/mapper/**/*.xml  type-aliases-package: com.nh.nsight.marketing.domain  configuration:    map-underscore-to-camel-case: true    default-fetch-size: 300    default-statement-timeout: 3management:  endpoints:    web:      exposure:        include: health,info,metrics,prometheus  endpoint:    health:      show-details: when_authorized      probes:        enabled: trueresilience4j:  circuitbreaker:    instances:      cruzapim:        registerHealthIndicator: true        slidingWindowSize: 100        minimumNumberOfCalls: 20        failureRateThreshold: 50        waitDurationInOpenState: 30s  bulkhead:    instances:      cruzapim:        maxConcurrentCalls: 100        maxWaitDuration: 0  timelimiter:    instances:      cruzapim:        timeoutDuration: 5snsight:  app-id: ${APP_ID:marketing-ap}  center-id: ${CENTER_ID:DC1}  timeout:    online-transaction: 5s    sql-default: 3s    external-read: 5s  guid:    header-name: X-GUID    required: true  audit:    customer-view-log: true    async: true  idempotency:    enabled: true    header-name: X-Idempotency-Key19. 최종 적용 체크리스트영역체크 항목판정 기준확인Profile환경별 application.yml 분리dev/sit/perf/prd 구분□SecretDB 계정/비밀번호 외부화소스 저장소에 평문 없음□SessionIdle/Absolute Timeout 분리Idle 60m, Absolute 8h 구현□CookieHttpOnly/Secure/SameSite 적용운영 HTTPS 기준 적용□Transaction온라인 거래 Timeout 적용@Transactional(timeout=5)□ReadOnly조회성 거래 readOnly 적용SingleView/일반 조회 적용□HikariPool 수와 DB Session 총량 검증AP 수 × Pool 수 산정□SQLMyBatis Statement Timeout 적용기본 3초, 예외 승인□ExternalCruzAPIM Timeout 적용Connect 3s, Read 5s□ResilienceCircuitBreaker/Bulkhead 적용연계 장애 격리□LoggingGUID/MDC 적용모든 로그에 guid 포함□Error표준 오류코드/응답 적용사용자/운영 메시지 분리□ActuatorHealth/Metrics 노출 통제내부망/권한 제한□Monitoring운영 임계치 대시보드화p95, 오류율, Pool, GC 표시□Test360/720/1080 TPS 검증성능 및 장애 시나리오 수행□20. 참고 공식 문서구분참고 문서활용 내용Spring BootCommon Application Propertiesserver.*, spring.*, management.* 속성 기준Spring BootExternalized ConfigurationProfile, YAML, 환경변수, 명령행 설정 구조Spring Framework@Transactional Javadoctimeout, readOnly, propagation, rollback 정책HikariCPHikariCP ConfigurationconnectionTimeout, maxLifetime, keepaliveTime, maximumPoolSize 의미MyBatisMyBatis Spring Boot Startermapper, typeAlias, default-fetch-size, default-statement-timeout 설정Resilience4jSpring Boot application.yml configurationCircuitBreaker, Bulkhead, TimeLimiter, Retry 설정최종 원칙: 본 문서의 설정값은 선도개발 및 성능테스트 전 기준선이다. 최종 운영값은 대표 거래 기준으로 p95 응답시간, CPU, GC, Hikari Pool, SQL Time, CruzAPIM Timeout, 오류율을 측정한 뒤 확정한다.

## 절별 상세

### 24.1 Application Profile

본 절(**Application Profile**)은 Spring Boot 영역에서 **Framework** 관점의 NSIGHT 1차 표준을 정의합니다. 피크 **720 TPS**·p95 **3초 이하**·8 vCPU / 32GB 기준으로 권고값을 제시하며, 최종값은 선도개발·성능시험(360/720/1080) 실측으로 확정합니다.

#### NSIGHT 권고값

| 항목 | Framework 권고 |
|------|----------------|
| 기준 VM | 8 vCPU / 32GB |
| TPS | 360/720/1080 |
| 설정 파일 | application.yml |
| 핵심 | Tx·Graceful |

#### 설정 예시

```yaml
server:
  shutdown: graceful
spring:
  lifecycle:
    timeout-per-shutdown-phase: 30s
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
```

#### 검증 기준

| 시나리오 | 합격 기준 |
|----------|----------|
| 360 TPS | p95≤3s, CPU≤70%, 오류율≤1% |
| 720 TPS | Thread/Pool 고갈 없음, Hikari Pending=0 |
| 1,080 TPS | Fail-fast·Timeout 정상, CB 동작 확인 |
| AP 1대 Down | L4 제외·잔여 TPS≥목표 |
| 센터 장애 | 잔여 센터 TPS≥720 |

#### 주의사항

- **피크 기준** 설계 — 평균 TPS만으로 설정 확정 금지
- **세션 ≠ TPS** — 세션 증가는 Heap·복제 부담, TPS와 별도 산정
- **Thread ≠ Pool** — Pool ≤ Thread×30~40%, DB Session 총량 검증
- **안쪽 Timeout 짧게** — SQL(3s) < Tx(5s) < Proxy(10s) < Web(15s)

#### 운영 참고

관련 원문: `znsight-capacity-word` · `환경셋팅(최종).docx` · 설정 위치: application.yml

### 24.2 Graceful Shutdown

본 절(**Graceful Shutdown**)은 Spring Boot 영역에서 **Framework** 관점의 NSIGHT 1차 표준을 정의합니다. 피크 **720 TPS**·p95 **3초 이하**·8 vCPU / 32GB 기준으로 권고값을 제시하며, 최종값은 선도개발·성능시험(360/720/1080) 실측으로 확정합니다.

#### NSIGHT 권고값

| 항목 | 권고 |
|------|------|
| shutdown | graceful |
| phase | 30s |
| L4 | Drain 선행 |

#### 설정 예시

```yaml
server.shutdown: graceful
```

#### 검증 기준

| 시나리오 | 합격 기준 |
|----------|----------|
| 360 TPS | p95≤3s, CPU≤70%, 오류율≤1% |
| 720 TPS | Thread/Pool 고갈 없음, Hikari Pending=0 |
| 1,080 TPS | Fail-fast·Timeout 정상, CB 동작 확인 |
| AP 1대 Down | L4 제외·잔여 TPS≥목표 |
| 센터 장애 | 잔여 센터 TPS≥720 |

#### 주의사항

- **피크 기준** 설계 — 평균 TPS만으로 설정 확정 금지
- **세션 ≠ TPS** — 세션 증가는 Heap·복제 부담, TPS와 별도 산정
- **Thread ≠ Pool** — Pool ≤ Thread×30~40%, DB Session 총량 검증
- **안쪽 Timeout 짧게** — SQL(3s) < Tx(5s) < Proxy(10s) < Web(15s)

#### 운영 참고

관련 원문: `znsight-capacity-word` · `환경셋팅(최종).docx` · 설정 위치: application.yml

### 24.3 Shutdown Timeout

본 절(**Shutdown Timeout**)은 Spring Boot 영역에서 **Framework** 관점의 NSIGHT 1차 표준을 정의합니다. 피크 **720 TPS**·p95 **3초 이하**·8 vCPU / 32GB 기준으로 권고값을 제시하며, 최종값은 선도개발·성능시험(360/720/1080) 실측으로 확정합니다.

#### NSIGHT 권고값

| 계층 | 권고 | 설정 위치 |
|------|------|----------|
| SQL | 3초 | MyBatis / Mapper |
| Pool 획득 | 3초 | Hikari connectionTimeout |
| Transaction | 5초 | @Transactional |
| TCF ServiceId | 4~5초 | TCF 설정 |
| Proxy | 10초 | Apache / Gateway |
| WebTop | 15초 | 단말 |
| L4 Idle | 120초 | L4 Profile |

#### 설정 예시

```yaml
server:
  shutdown: graceful
spring:
  lifecycle:
    timeout-per-shutdown-phase: 30s
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
```

#### 검증 기준

| 시나리오 | 합격 기준 |
|----------|----------|
| 360 TPS | p95≤3s, CPU≤70%, 오류율≤1% |
| 720 TPS | Thread/Pool 고갈 없음, Hikari Pending=0 |
| 1,080 TPS | Fail-fast·Timeout 정상, CB 동작 확인 |
| AP 1대 Down | L4 제외·잔여 TPS≥목표 |
| 센터 장애 | 잔여 센터 TPS≥720 |

#### 주의사항

- **피크 기준** 설계 — 평균 TPS만으로 설정 확정 금지
- **세션 ≠ TPS** — 세션 증가는 Heap·복제 부담, TPS와 별도 산정
- **Thread ≠ Pool** — Pool ≤ Thread×30~40%, DB Session 총량 검증
- **안쪽 Timeout 짧게** — SQL(3s) < Tx(5s) < Proxy(10s) < Web(15s)

#### 운영 참고

관련 원문: `znsight-capacity-word` · `환경셋팅(최종).docx` · 설정 위치: application.yml

### 24.4 Request Body 제한

본 절(**Request Body 제한**)은 Spring Boot 영역에서 **Framework** 관점의 NSIGHT 1차 표준을 정의합니다. 피크 **720 TPS**·p95 **3초 이하**·8 vCPU / 32GB 기준으로 권고값을 제시하며, 최종값은 선도개발·성능시험(360/720/1080) 실측으로 확정합니다.

#### NSIGHT 권고값

| 항목 | Framework 권고 |
|------|----------------|
| 기준 VM | 8 vCPU / 32GB |
| TPS | 360/720/1080 |
| 설정 파일 | application.yml |
| 핵심 | Tx·Graceful |

#### 설정 예시

```yaml
server:
  shutdown: graceful
spring:
  lifecycle:
    timeout-per-shutdown-phase: 30s
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
```

#### 검증 기준

| 시나리오 | 합격 기준 |
|----------|----------|
| 360 TPS | p95≤3s, CPU≤70%, 오류율≤1% |
| 720 TPS | Thread/Pool 고갈 없음, Hikari Pending=0 |
| 1,080 TPS | Fail-fast·Timeout 정상, CB 동작 확인 |
| AP 1대 Down | L4 제외·잔여 TPS≥목표 |
| 센터 장애 | 잔여 센터 TPS≥720 |

#### 주의사항

- **피크 기준** 설계 — 평균 TPS만으로 설정 확정 금지
- **세션 ≠ TPS** — 세션 증가는 Heap·복제 부담, TPS와 별도 산정
- **Thread ≠ Pool** — Pool ≤ Thread×30~40%, DB Session 총량 검증
- **안쪽 Timeout 짧게** — SQL(3s) < Tx(5s) < Proxy(10s) < Web(15s)

#### 운영 참고

관련 원문: `znsight-capacity-word` · `환경셋팅(최종).docx` · 설정 위치: application.yml

### 24.5 Multipart 설정

본 절(**Multipart 설정**)은 Spring Boot 영역에서 **Framework** 관점의 NSIGHT 1차 표준을 정의합니다. 피크 **720 TPS**·p95 **3초 이하**·8 vCPU / 32GB 기준으로 권고값을 제시하며, 최종값은 선도개발·성능시험(360/720/1080) 실측으로 확정합니다.

#### NSIGHT 권고값

| 항목 | 권고 |
|------|------|
| max-file-size | 업무별 10~50MB |
| 대용량 | 전용 서버 |

#### 설정 예시

```yaml
spring.servlet.multipart.max-file-size: 10MB
```

#### 검증 기준

| 시나리오 | 합격 기준 |
|----------|----------|
| 360 TPS | p95≤3s, CPU≤70%, 오류율≤1% |
| 720 TPS | Thread/Pool 고갈 없음, Hikari Pending=0 |
| 1,080 TPS | Fail-fast·Timeout 정상, CB 동작 확인 |
| AP 1대 Down | L4 제외·잔여 TPS≥목표 |
| 센터 장애 | 잔여 센터 TPS≥720 |

#### 주의사항

- **피크 기준** 설계 — 평균 TPS만으로 설정 확정 금지
- **세션 ≠ TPS** — 세션 증가는 Heap·복제 부담, TPS와 별도 산정
- **Thread ≠ Pool** — Pool ≤ Thread×30~40%, DB Session 총량 검증
- **안쪽 Timeout 짧게** — SQL(3s) < Tx(5s) < Proxy(10s) < Web(15s)

#### 운영 참고

관련 원문: `znsight-capacity-word` · `환경셋팅(최종).docx` · 설정 위치: application.yml

### 24.6 Spring Task Executor

본 절(**Spring Task Executor**)은 Spring Boot 영역에서 **Framework** 관점의 NSIGHT 1차 표준을 정의합니다. 피크 **720 TPS**·p95 **3초 이하**·8 vCPU / 32GB 기준으로 권고값을 제시하며, 최종값은 선도개발·성능시험(360/720/1080) 실측으로 확정합니다.

#### NSIGHT 권고값

| 항목 | Framework 권고 |
|------|----------------|
| 기준 VM | 8 vCPU / 32GB |
| TPS | 360/720/1080 |
| 설정 파일 | application.yml |
| 핵심 | Tx·Graceful |

#### 설정 예시

```yaml
server:
  shutdown: graceful
spring:
  lifecycle:
    timeout-per-shutdown-phase: 30s
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
```

#### 검증 기준

| 시나리오 | 합격 기준 |
|----------|----------|
| 360 TPS | p95≤3s, CPU≤70%, 오류율≤1% |
| 720 TPS | Thread/Pool 고갈 없음, Hikari Pending=0 |
| 1,080 TPS | Fail-fast·Timeout 정상, CB 동작 확인 |
| AP 1대 Down | L4 제외·잔여 TPS≥목표 |
| 센터 장애 | 잔여 센터 TPS≥720 |

#### 주의사항

- **피크 기준** 설계 — 평균 TPS만으로 설정 확정 금지
- **세션 ≠ TPS** — 세션 증가는 Heap·복제 부담, TPS와 별도 산정
- **Thread ≠ Pool** — Pool ≤ Thread×30~40%, DB Session 총량 검증
- **안쪽 Timeout 짧게** — SQL(3s) < Tx(5s) < Proxy(10s) < Web(15s)

#### 운영 참고

관련 원문: `znsight-capacity-word` · `환경셋팅(최종).docx` · 설정 위치: application.yml

### 24.7 Async Executor

본 절(**Async Executor**)은 Spring Boot 영역에서 **Framework** 관점의 NSIGHT 1차 표준을 정의합니다. 피크 **720 TPS**·p95 **3초 이하**·8 vCPU / 32GB 기준으로 권고값을 제시하며, 최종값은 선도개발·성능시험(360/720/1080) 실측으로 확정합니다.

#### NSIGHT 권고값

| 항목 | Framework 권고 |
|------|----------------|
| 기준 VM | 8 vCPU / 32GB |
| TPS | 360/720/1080 |
| 설정 파일 | application.yml |
| 핵심 | Tx·Graceful |

#### 설정 예시

```yaml
server:
  shutdown: graceful
spring:
  lifecycle:
    timeout-per-shutdown-phase: 30s
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
```

#### 검증 기준

| 시나리오 | 합격 기준 |
|----------|----------|
| 360 TPS | p95≤3s, CPU≤70%, 오류율≤1% |
| 720 TPS | Thread/Pool 고갈 없음, Hikari Pending=0 |
| 1,080 TPS | Fail-fast·Timeout 정상, CB 동작 확인 |
| AP 1대 Down | L4 제외·잔여 TPS≥목표 |
| 센터 장애 | 잔여 센터 TPS≥720 |

#### 주의사항

- **피크 기준** 설계 — 평균 TPS만으로 설정 확정 금지
- **세션 ≠ TPS** — 세션 증가는 Heap·복제 부담, TPS와 별도 산정
- **Thread ≠ Pool** — Pool ≤ Thread×30~40%, DB Session 총량 검증
- **안쪽 Timeout 짧게** — SQL(3s) < Tx(5s) < Proxy(10s) < Web(15s)

#### 운영 참고

관련 원문: `znsight-capacity-word` · `환경셋팅(최종).docx` · 설정 위치: application.yml

### 24.8 Scheduler Thread Pool

본 절(**Scheduler Thread Pool**)은 Spring Boot 영역에서 **Framework** 관점의 NSIGHT 1차 표준을 정의합니다. 피크 **720 TPS**·p95 **3초 이하**·8 vCPU / 32GB 기준으로 권고값을 제시하며, 최종값은 선도개발·성능시험(360/720/1080) 실측으로 확정합니다.

#### NSIGHT 권고값

| 항목 | 권고 |
|------|------|
| Window | 00:00~06:00 |
| Pool | 배치 전용 |
| 중복 실행 | 방지 |

#### 설정 예시

온라인 Pool·CPU와 **분리**.

#### 검증 기준

| 시나리오 | 합격 기준 |
|----------|----------|
| 360 TPS | p95≤3s, CPU≤70%, 오류율≤1% |
| 720 TPS | Thread/Pool 고갈 없음, Hikari Pending=0 |
| 1,080 TPS | Fail-fast·Timeout 정상, CB 동작 확인 |
| AP 1대 Down | L4 제외·잔여 TPS≥목표 |
| 센터 장애 | 잔여 센터 TPS≥720 |

#### 주의사항

- **피크 기준** 설계 — 평균 TPS만으로 설정 확정 금지
- **세션 ≠ TPS** — 세션 증가는 Heap·복제 부담, TPS와 별도 산정
- **Thread ≠ Pool** — Pool ≤ Thread×30~40%, DB Session 총량 검증
- **안쪽 Timeout 짧게** — SQL(3s) < Tx(5s) < Proxy(10s) < Web(15s)

#### 운영 참고

관련 원문: `znsight-capacity-word` · `환경셋팅(최종).docx` · 설정 위치: application.yml

### 24.9 Transaction 기본 Timeout

본 절(**Transaction 기본 Timeout**)은 Spring Boot 영역에서 **Framework** 관점의 NSIGHT 1차 표준을 정의합니다. 피크 **720 TPS**·p95 **3초 이하**·8 vCPU / 32GB 기준으로 권고값을 제시하며, 최종값은 선도개발·성능시험(360/720/1080) 실측으로 확정합니다.

#### NSIGHT 권고값

| 계층 | 권고 | 설정 위치 |
|------|------|----------|
| SQL | 3초 | MyBatis / Mapper |
| Pool 획득 | 3초 | Hikari connectionTimeout |
| Transaction | 5초 | @Transactional |
| TCF ServiceId | 4~5초 | TCF 설정 |
| Proxy | 10초 | Apache / Gateway |
| WebTop | 15초 | 단말 |
| L4 Idle | 120초 | L4 Profile |

#### 설정 예시

```yaml
server:
  shutdown: graceful
spring:
  lifecycle:
    timeout-per-shutdown-phase: 30s
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
```

#### 검증 기준

| 시나리오 | 합격 기준 |
|----------|----------|
| 360 TPS | p95≤3s, CPU≤70%, 오류율≤1% |
| 720 TPS | Thread/Pool 고갈 없음, Hikari Pending=0 |
| 1,080 TPS | Fail-fast·Timeout 정상, CB 동작 확인 |
| AP 1대 Down | L4 제외·잔여 TPS≥목표 |
| 센터 장애 | 잔여 센터 TPS≥720 |

#### 주의사항

- **피크 기준** 설계 — 평균 TPS만으로 설정 확정 금지
- **세션 ≠ TPS** — 세션 증가는 Heap·복제 부담, TPS와 별도 산정
- **Thread ≠ Pool** — Pool ≤ Thread×30~40%, DB Session 총량 검증
- **안쪽 Timeout 짧게** — SQL(3s) < Tx(5s) < Proxy(10s) < Web(15s)

#### 운영 참고

관련 원문: `znsight-capacity-word` · `환경셋팅(최종).docx` · 설정 위치: application.yml

### 24.10 Actuator 노출 범위

본 절(**Actuator 노출 범위**)은 Spring Boot 영역에서 **Framework** 관점의 NSIGHT 1차 표준을 정의합니다. 피크 **720 TPS**·p95 **3초 이하**·8 vCPU / 32GB 기준으로 권고값을 제시하며, 최종값은 선도개발·성능시험(360/720/1080) 실측으로 확정합니다.

#### NSIGHT 권고값

| 항목 | Framework 권고 |
|------|----------------|
| 기준 VM | 8 vCPU / 32GB |
| TPS | 360/720/1080 |
| 설정 파일 | application.yml |
| 핵심 | Tx·Graceful |

#### 설정 예시

```yaml
server:
  shutdown: graceful
spring:
  lifecycle:
    timeout-per-shutdown-phase: 30s
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
```

#### 검증 기준

| 시나리오 | 합격 기준 |
|----------|----------|
| 360 TPS | p95≤3s, CPU≤70%, 오류율≤1% |
| 720 TPS | Thread/Pool 고갈 없음, Hikari Pending=0 |
| 1,080 TPS | Fail-fast·Timeout 정상, CB 동작 확인 |
| AP 1대 Down | L4 제외·잔여 TPS≥목표 |
| 센터 장애 | 잔여 센터 TPS≥720 |

#### 주의사항

- **피크 기준** 설계 — 평균 TPS만으로 설정 확정 금지
- **세션 ≠ TPS** — 세션 증가는 Heap·복제 부담, TPS와 별도 산정
- **Thread ≠ Pool** — Pool ≤ Thread×30~40%, DB Session 총량 검증
- **안쪽 Timeout 짧게** — SQL(3s) < Tx(5s) < Proxy(10s) < Web(15s)

#### 운영 참고

관련 원문: `znsight-capacity-word` · `환경셋팅(최종).docx` · 설정 위치: application.yml

### 24.11 Health·Readiness·Liveness

본 절(**Health·Readiness·Liveness**)은 Spring Boot 영역에서 **Framework** 관점의 NSIGHT 1차 표준을 정의합니다. 피크 **720 TPS**·p95 **3초 이하**·8 vCPU / 32GB 기준으로 권고값을 제시하며, 최종값은 선도개발·성능시험(360/720/1080) 실측으로 확정합니다.

#### NSIGHT 권고값

| 계층 | URI | Interval |
|------|-----|----------|
| L4 | /actuator/health/l4 | 5s |
| GSLB | 센터 VIP | 10s |

#### 설정 예시

Fail 3회 → Member 제외.

#### 검증 기준

| 시나리오 | 합격 기준 |
|----------|----------|
| 360 TPS | p95≤3s, CPU≤70%, 오류율≤1% |
| 720 TPS | Thread/Pool 고갈 없음, Hikari Pending=0 |
| 1,080 TPS | Fail-fast·Timeout 정상, CB 동작 확인 |
| AP 1대 Down | L4 제외·잔여 TPS≥목표 |
| 센터 장애 | 잔여 센터 TPS≥720 |

#### 주의사항

- **피크 기준** 설계 — 평균 TPS만으로 설정 확정 금지
- **세션 ≠ TPS** — 세션 증가는 Heap·복제 부담, TPS와 별도 산정
- **Thread ≠ Pool** — Pool ≤ Thread×30~40%, DB Session 총량 검증
- **안쪽 Timeout 짧게** — SQL(3s) < Tx(5s) < Proxy(10s) < Web(15s)

#### 운영 참고

관련 원문: `znsight-capacity-word` · `환경셋팅(최종).docx` · 설정 위치: application.yml

### 24.12 Spring Boot 설정 검증

본 절(**Spring Boot 설정 검증**)은 Spring Boot 영역에서 **Framework** 관점의 NSIGHT 1차 표준을 정의합니다. 피크 **720 TPS**·p95 **3초 이하**·8 vCPU / 32GB 기준으로 권고값을 제시하며, 최종값은 선도개발·성능시험(360/720/1080) 실측으로 확정합니다.

#### NSIGHT 권고값

| 항목 | Framework 권고 |
|------|----------------|
| 기준 VM | 8 vCPU / 32GB |
| TPS | 360/720/1080 |
| 설정 파일 | application.yml |
| 핵심 | Tx·Graceful |

#### 설정 예시

```yaml
server:
  shutdown: graceful
spring:
  lifecycle:
    timeout-per-shutdown-phase: 30s
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
```

#### 검증 기준

| 시나리오 | 합격 기준 |
|----------|----------|
| 360 TPS | p95≤3s, CPU≤70%, 오류율≤1% |
| 720 TPS | Thread/Pool 고갈 없음, Hikari Pending=0 |
| 1,080 TPS | Fail-fast·Timeout 정상, CB 동작 확인 |
| AP 1대 Down | L4 제외·잔여 TPS≥목표 |
| 센터 장애 | 잔여 센터 TPS≥720 |

#### 주의사항

- **피크 기준** 설계 — 평균 TPS만으로 설정 확정 금지
- **세션 ≠ TPS** — 세션 증가는 Heap·복제 부담, TPS와 별도 산정
- **Thread ≠ Pool** — Pool ≤ Thread×30~40%, DB Session 총량 검증
- **안쪽 Timeout 짧게** — SQL(3s) < Tx(5s) < Proxy(10s) < Web(15s)

#### 운영 참고

검증 도구: APM, `jcmd`, `jstat`, Hikari Metrics, Access Log(GUID), ENV rule-check.

---

[← 목차](./00-목차.md)
