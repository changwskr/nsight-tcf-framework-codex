# 부록 S. Batch·Scheduler 설정표

> 원본: `znsight-capacity-word` · 23장 수준 템플릿 확장

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

> 출처: `znsight-capacity-word` · [13단계 요약](../zNSIGHT-용량산정-전체-흐름.md)


## 원문 기반 본문

NSIGHT 마케팅플랫폼타임아웃 설정 가이드영역별 설정값 · 실제 설정 위치 · 예시 · 검증 기준기준: IaaS VM, WebTopSuite, L4, Tomcat, Spring Boot, HikariCP, RDW/ADW, CruzAPIM, Kafka/CDC, ETL/DataStage문서 개요본 문서는 농협상호금융 NSIGHT 마케팅플랫폼의 타임아웃 설정을 영역별로 표준화하기 위한 작업 가이드이다. 타임아웃은 단순히 “기다리는 시간”이 아니라, 장애 전파를 차단하고 사용자 응답성을 보호하며 DB·연계·AP 자원 고갈을 방지하기 위한 실행 메커니즘이다.

특히 NSIGHT는 WebTopSuite 단말, L4, Tomcat, Spring Boot, HikariCP, RDW/ADW, CruzAPIM, Kafka/CDC, ETL/DataStage가 함께 동작하므로 각 계층의 타임아웃 순서를 일관되게 맞추어야 한다.

구분기준문서 목적영역별 타임아웃 권장값, 설정 위치, 설정 예시, 검증 방법을 하나의 기준으로 정리적용 대상WebTopSuite, Proxy, L4, Tomcat, Spring Boot, HikariCP, MyBatis, RDW/ADW, CruzAPIM, Kafka/CDC, ETL, Batch, 모니터링핵심 원칙DB Query Timeout < Application Transaction Timeout < Client/Proxy Timeout < L4/Session Timeout적용 전제일반 온라인 목표 응답시간 p95 3초, 동기 온라인 거래는 짧게 실패 처리, 장시간 처리는 비동기·배치 전환주의사항서버 Core/Memory가 커져도 온라인 동기거래 Timeout을 무조건 늘리면 안 됨

## 1. 타임아웃 설계 기본 원칙

### 1.1 타임아웃은 장애 차단 경계이다타임아웃은 사용자의 대기시간을 줄이기 위한 값이기도 하지만, 더 중요하게는 장애가 다음 계층으로 전파되지 않도록 끊어주는 경계값이다. DB가 지연되면 AP Thread가 고갈되고, AP Thread가 고갈되면 L4 Queue가 쌓이며, L4 Queue가 쌓이면 단말에서 장애처럼 보인다. 따라서 가장 안쪽의 자원인 DB와 외부연계를 먼저 짧게 통제해야 한다.

원칙설명작업 기준안쪽 자원부터 짧게DB, 외부연계, Pool 대기시간을 먼저 제한DB Query 2~3초, Hikari 대기 3초, CruzAPIM Connect 3초업무 트랜잭션은 짧게온라인 사용자가 기다리는 동기 거래는 3초 목표, 5초 내 실패 처리

```java
@Transactional(timeout=5)클라이언트는 더 길게서버가 오류를 만들고 응답할 시간을 확보WebTopSuite Request 15초, Proxy Read 10초세션과 트랜잭션 분리세션은 로그인 유지시간, 트랜잭션은 거래 1건 수행시간세션 60분, 트랜잭션 4~5초장시간 거래 비동기화10초 이상 걸리는 조회·처리는 온라인 동기 금지배치, Queue, 비동기 조회, 결과조회 방식 전환재시도는 제한Timeout 후 무조건 재시도하면 중복 처리와 부하 증폭 발생GUID/IdempotencyKey 기반 상태조회 후 재처리
```


### 1.2 권장 타임아웃 계층 구조DB Query Timeout             : 2~3초Hikari Connection Timeout     : 2~3초CruzAPIM Connect Timeout      : 3초CruzAPIM Read Timeout         : 5초Spring Transaction Timeout    : 4~5초Proxy Read Timeout            : 10초WebTopSuite Request Timeout   : 15초L4 Idle Timeout               : 120초Session Idle Timeout          : 30~60분 또는 업무 예외 180분Absolute Session Timeout      : 8~12시간이 순서가 깨지면 장애 분석이 어려워진다. 예를 들어 WebTopSuite가 5초에 먼저 끊기고 서버 Transaction Timeout이 10초라면 사용자는 Timeout을 받았지만 서버는 계속 처리 중일 수 있다. 이 경우 중복 재요청 위험이 발생한다.

## 2. 전체 권장값 총괄표영역설정 항목권장값실제 설정 위치설정 예시핵심 설명

WebTopSuiteRequest Timeout15초Runtime ConfigREQUEST_TIMEOUT=15000사용자 최종 대기시간WebTopSuiteConnect Timeout3초Runtime ConfigCONNECT_TIMEOUT=3000AP 연결 실패 판단WebTopSuiteRead Timeout10초Runtime ConfigREAD_TIMEOUT=10000AP 응답 대기ProxyConnect Timeout3초nginx.conf/httpd.confproxy_connect_timeout 3sAP 연결 실패 판단ProxyRead Timeout10초nginx.conf/httpd.confproxy_read_timeout 10sAP 응답 대기L4Idle Timeout120초L4 설정 콘솔idle timeout=120sKeepAlive 고려L4Sticky TimeoutSession+10분L4 설정 콘솔70m 또는 190m세션 유지보다 길게TomcatconnectionTimeout8초server.xml/application.ymlserver.tomcat.connection-timeout=8s연결 후 요청 대기TomcatkeepAliveTimeout120초server.xmlkeepAliveTimeout=120000연결 재사용 유지SpringTransaction Timeout4~5초@Transactional/TransactionConfig@Transactional(timeout=5)온라인 거래 전체 한도HikariCPconnectionTimeout3초application.ymlconnection-timeout: 3000Connection Pool 획득 대기시간MyBatisStatement Timeout3초application.yml/Mapperdefault-statement-timeout: 3SQL 수행 한도CruzAPIMConnect Timeout3초ClientConfigconnect-timeout: 3000연계 연결 실패 판단CruzAPIMRead Timeout5초ClientConfigread-timeout: 5000연계 응답 대기BatchJob Timeout업무별 정의Batch Config/SchedulerjobTimeout=30m~2h온라인과 별도 관리KafkaConsumer Poll/Session업무별 정의consumer configsession.timeout.ms=30000Consumer 장애 감지ETLStep/Job Timeout업무별 정의DataStage Job Option정의 필요배치 윈도우 기준

## 3. WebTopSuite 단말 타임아웃 설정 가이드WebTopSuite는 사용자가 직접 체감하는 최상위 계층이다. 단말 Timeout은 너무 짧으면 서버 처리 결과를 받기 전에 끊기고, 너무 길면 사용자가 장애를 오래 기다리게 된다. 따라서 온라인 일반 조회는 15초를 기준으로 하되, 다운로드·대량조회는 별도 비동기 방식으로 분리한다.

설정 항목권장값실제 설정 위치설정 예시설명REQUEST_TIMEOUT15초WebTopSuite Environment/Runtime ConfigREQUEST_TIMEOUT=15000최종 사용자 대기시간. 서버 Transaction Timeout보다 길어야 함CONNECT_TIMEOUT3초WebTopSuite Runtime ConfigCONNECT_TIMEOUT=3000L4 또는 AP 연결 실패 판단READ_TIMEOUT10초WebTopSuite Runtime ConfigREAD_TIMEOUT=10000서버 응답 대기시간RETRY_COUNT0~1회WebTopSuite Runtime ConfigRETRY_COUNT=1무분별한 재시도 금지. 상태조회 API와 연계DOWNLOAD_TIMEOUT60초화면/다운로드 공통 설정DOWNLOAD_TIMEOUT=60000다운로드는 일반 조회와 분리LONG_QUERY_POLICY비동기 전환업무 공통 정책ASYNC_REQUIRED=true장시간 조회는 요청-접수-결과조회 구조로 전환설정 예시REQUEST_TIMEOUT=15000CONNECT_TIMEOUT=3000READ_TIMEOUT=10000RETRY_COUNT=1DOWNLOAD_TIMEOUT=60000작업 가이드단말 Timeout 발생 시 클라이언트가 원거래를 즉시 재실행하지 않도록 해야 한다. GUID 또는 IdempotencyKey를 보관하고 거래상태조회 API를 호출한 뒤, 서버가 반환한 상태에 따라 결과표시·재조회·재처리 여부를 판단한다.4. WEB / Reverse Proxy 타임아웃 설정 가이드Proxy는 단말과 AP 사이에서 연결 실패, 응답 지연, KeepAlive를 제어한다. Proxy Timeout은 단말 Timeout보다 짧거나 같게 두되, 서버가 오류 응답을 반환할 시간을 확보해야 한다.

설정 항목권장값실제 설정 위치설정 예시설명proxy_connect_timeout3초nginx.confproxy_connect_timeout 3s;AP 연결 실패 판단proxy_send_timeout10초nginx.confproxy_send_timeout 10s;AP로 요청 전송 대기proxy_read_timeout10초nginx.confproxy_read_timeout 10s;AP 응답 대기ProxyTimeout10초httpd.confProxyTimeout 10Apache Proxy 전체 TimeoutTimeout15초httpd.confTimeout 15Apache 기본 네트워크 TimeoutKeepAliveTimeout120초httpd.confKeepAliveTimeout 120TCP 재사용 유지시간Access Log활성화nginx/httpd log configX-GUID 기록GUID 기반 E2E 추적설정 예시location / {    proxy_connect_timeout 3s;    proxy_send_timeout    10s;    proxy_read_timeout    10s;    proxy_http_version 1.1;    proxy_set_header Connection "";    proxy_set_header X-GUID $http_x_guid;    proxy_pass http://marketing_ap;}작업 가이드Proxy 로그에는 X-GUID, URL, 응답코드, 응답시간, upstream_response_time을 반드시 남겨야 한다. 그래야 단말 Timeout과 AP Timeout을 구분할 수 있다.5. L4 / GSLB 타임아웃 설정 가이드L4는 세션 유지, 센터 전환, AP Health Check를 담당한다. L4 Timeout은 업무 요청 Timeout보다 길게 두고, Sticky Timeout은 세션 Idle Timeout보다 길게 설정해야 한다.

설정 항목권장값실제 설정 위치설정 예시설명

GSLB DNS TTL30~60초GSLB 설정 콘솔TTL=30센터 장애 시 재조회 가능Health Check Interval5초L4 설정 콘솔interval=5sAP 상태 확인 주기Health Check Timeout2초L4 설정 콘솔timeout=2sHealth 응답 없음 판단Fail Count3회L4 설정 콘솔fail-count=3약 15초 내 장애 판단Sticky Persistence활성화L4 설정 콘솔JSESSIONID 기준센터 내 AP 세션 유지Sticky TimeoutSession + 10분L4 설정 콘솔70m 또는 190mTomcat Session보다 길게Idle Timeout120초L4 설정 콘솔idle-timeout=120sKeepAlive와 정합성 유지센터 장애 정책GSLB 재조회운영 정책DR center failover센터 장애 시 재로그인 또는 상태복구 정책 적용설정 예시Health Check URI: GET /actuator/healthInterval        : 5sTimeout         : 2sFail Count      : 3Sticky          : JSESSIONIDSticky Timeout  : Session Idle Timeout + 10mIdle Timeout    : 120s작업 가이드센터 내부 AP 장애는 L4와 Tomcat Cluster로 흡수하고, 센터 전체 장애는 GSLB 재조회와 재로그인 정책으로 단순화한다. 센터 간 세션 복제를 적용하지 않는 경우 센터 장애 시 세션 유지 기대치를 명확히 공지해야 한다.6. Tomcat WAS 타임아웃 설정 가이드Tomcat은 연결 수, Worker Thread, KeepAlive, 세션을 제어한다. Tomcat connectionTimeout은 업무 Transaction Timeout이 아니라, TCP 연결 이후 요청이 들어오기까지 기다리는 시간이다. 요청 처리 시간은 Spring Transaction Timeout과 SQL Timeout으로 제어해야 한다.

설정 항목권장값실제 설정 위치설정 예시설명connectionTimeout8초server.xml/application.ymlserver.tomcat.connection-timeout=8s연결 후 요청 대기시간keepAliveTimeout120초server.xmlkeepAliveTimeout=120000HTTP KeepAlive 유지maxKeepAliveRequests1,000server.xmlmaxKeepAliveRequests=1000연결당 요청 수 제한maxThreads32Core: 1500~1800server.xml/application.ymlserver.tomcat.threads.max=1600Worker Thread 상한minSpareThreads32Core: 300~400server.xml/application.ymlmin-spare=300대기 ThreadacceptCount800~1000server.xml/application.ymlaccept-count=1000Thread 포화 시 대기 QueuemaxConnections20,000~30,000server.xml/application.ymlmax-connections=20000동시 연결 상한Session Timeout30~60분/예외 180분web.xml/context.xml/application.ymlsession-timeout=60로그인 유휴 만료설정 예시

```yaml
server:  tomcat:    threads:      max: 1600      min-spare: 300    accept-count: 1000    max-connections: 20000    connection-timeout: 8s작업 가이드32 Core / 256GB 서버에서 Thread를 크게 설정할 수는 있지만, DB Pool과 외부연계 대기시간이 맞지 않으면 Thread만 쌓인다. Tomcat Thread 증가는 DB Connection 수, SQL Timeout, CPU 사용률, GC Pause와 함께 검증해야 한다.
```


## 7. 세션 타임아웃 설정 가이드세션 타임아웃과 트랜잭션 타임아웃은 완전히 다르다. 세션은 로그인 상태 유지시간이고, 트랜잭션은 한 번의 업무 요청이 서버·DB·연계 자원을 붙잡는 최대 시간이다.

설정 항목권장값실제 설정 위치설정 예시설명Session Idle Timeout60분 기본application.yml/web.xml/context.xmlserver.servlet.session.timeout=60m유휴 세션 만료장시간 상담 예외180분 검토업무 예외 정책timeout=180m상담·마케팅 장시간 업무 예외Absolute Timeout8~12시간Filter/Interceptor/SecurityloginTime + 8h계속 사용해도 강제 로그아웃L4 Sticky TimeoutSession + 10분L4 설정70m 또는 190m세션보다 길게DeltaManager 세션 객체Serializable 필수Java Session Objectimplements Serializable세션 복제 가능 조건세션 데이터 크기2KB 이하 권장, 최대 5KB개발 표준고객 조회 결과 저장 금지DeltaManager 복제 부하 통제설정 예시server:  servlet:    session:      timeout: 60m      cookie:        http-only: true        secure: true        same-site: Lax작업 가이드세션에는 사용자 ID, 지점 ID, 권한 등급, 마스킹 등급처럼 작은 정보만 저장한다. 고객 조회 결과, 캠페인 대상자 목록, Single View 결과, 거래 목록은 세션에 저장하지 않는다.8. Spring Boot / Transaction 타임아웃 설정 가이드Spring Transaction Timeout은 온라인 업무 거래 전체 수행 한도이다. 일반 조회·Single View·CruzAPIM 연계·EBM 판단은 업무 유형별로 Timeout을 달리 적용하되, 온라인 동기 거래는 짧게 실패시키는 원칙을 유지해야 한다.

설정 항목권장값실제 설정 위치설정 예시설명기본 Transaction Timeout4~5초@Transactional/TransactionConfig@Transactional(timeout=5)온라인 거래 전체 한도ReadOnly 조회true@TransactionalreadOnly=trueSingle View·조회성 거래 적용일반 마케팅 조회5초Facade/Service@Transactional(timeout=5)권한·마스킹·로그 포함Single View 조회5초Facade/Service@Transactional(timeout=5, readOnly=true)RDW 조회 중심외부연계 포함 거래5초 내Facade + CruzAPIM ConfigTX와 APIM Timeout 정합성연계 장애 전파 차단장시간 거래동기 금지업무 공통 정책10초 이상 비동기 전환Thread 점유 방지Rollback 정책명시TransactionConfigrollbackFor=Exception.classTimeout·DB 오류 일관 처리설정 예시

```java
@Transactional(timeout = 5, readOnly = true)public SingleViewResponse getSingleView(SingleViewRequest request) {    // 1. 권한 확인    // 2. RDW 조
```


회    // 3. 마스킹    // 4. 감사로그 비동기 저장    return response;}작업 가이드트랜잭션 경계는 Controller가 아니라 Facade 또는 Service 계층에서 관리한다. Controller는 요청 수신과 검증, 응답 변환에 집중하고 업무 트랜잭션은 Facade에서 통제한다.9. HikariCP Connection Pool 타임아웃 설정 가이드Hikari Connection Timeout은 SQL 실행시간 제한이 아니다. WAS가 Connection Pool에서 DB Connection을 빌리기 위해 기다리는 최대 시간이다. Connection을 얻은 이후 SQL 수행시간은 MyBatis Statement Timeout 또는 JDBC Query Timeout으로 통제한다.

설정 항목권장값실제 설정 위치설정 예시설명maximumPoolSize32Core 일반 AP: 120~150application.ymlmaximum-pool-size: 120DB 접속 상한maximumPoolSize32Core SingleView: 150~180application.ymlmaximum-pool-size: 150RDW 고빈도 조회 기준minimumIdle일반 30~40 / SingleView 40~50application.ymlminimum-idle: 30기본 유지 ConnectionconnectionTimeout3초application.ymlconnection-timeout: 3000Pool에서 Connection 획득 대기validationTimeout3초application.ymlvalidation-timeout: 3000Connection 유효성 검증idleTimeout10분application.ymlidle-timeout: 600000미사용 Connection 반환maxLifetime30분 이하application.ymlmax-lifetime: 1800000DB/L4/방화벽 Idle보다 짧게keepaliveTime5분application.ymlkeepalive-time: 300000유휴 Connection 유지설정 예시spring:  datasource:    hikari:      maximum-pool-size: 120      minimum-idle: 30      connection-timeout: 3000      validation-timeout: 3000      idle-timeout: 600000      max-lifetime: 1800000      keepalive-time: 300000      auto-commit: false작업 가이드DBA와 함께 AP 수 × maximumPoolSize로 DB 총 Session 수를 계산해야 한다. 32 Core 서버는 Pool을 크게 잡기 쉬우므로 RDW/ADW/감사DB/로그DB별 총 Session 상한을 반드시 검증한다.10. MyBatis / JDBC / SQL 타임아웃 설정 가이드SQL Timeout은 AP 자원 보호의 핵심이다. DB Query가 오래 걸리면 Tomcat Thread와 Hikari Connection이 동시에 묶인다. 따라서 RDW 온라인 조회 SQL은 짧게, 분석성 SQL은 ADW로 분리해야 한다.

설정 항목권장값실제 설정 위치설정 예시설명default-statement-timeout3초application.ymldefault-statement-timeout: 3기본 SQL 수행 한도일반 조회 SQL2~3초Mapper XML/SQL 표준timeout=3일반 화면 조회SingleView 핵심 SQL3초Mapper XML/SQL 표준timeout=3RDW 고빈도 조회복합 조회 SQL5초 이내예외 승인timeout=5예외 관리 대상분석성 SQLRDW 금지SQL 리뷰 기준ADW 전용RDW 자원 경합 차단Fetch Size100~500application.yml/Mapperdefault-fetch-size: 300대량 조회 제어Full Scan금지SQL 리뷰/튜닝 기준예외 승인 필요운영 장애 예방설정 예시

```yaml
mybatis:
  configuration:    default-statement-timeout: 3    default-fetch-size: 300작업 가이드SQL Timeout 오류는 단순 장애가 아니라 튜닝 후보이다. SQL ID, Mapper ID, 파라미터 마스킹값, 실행시간, Row Count, Plan Hash를 로그와 APM에서 연결해야 한다.11. CruzAPIM / 외부연계 타임아웃 설정 가이드외부연계는 장애 전파의 주요 원인이다. AP가 외부 시스템 응답을 오래 기다리면 내부 AP Thread가 고갈된다. 따라서 Connect Timeout과 Read Timeout을 분리하고, Circuit Breaker와 Bulkhead를 적용해야 한다.

설정 항목권장값실제 설정 위치설정 예시설명Connect Timeout3초CruzApimClientConfigconnect-timeout: 3000연계 연결 실패 판단Read Timeout5초CruzApimClientConfigread-timeout: 5000연계 응답 대기Retry기본 금지, 예외 1회RetryConfigmax-attempts=1중복 연계 방지Circuit Breaker필수Resilience4jfailureRateThreshold=50연계 장애 전파 차단Bulkhead필수Resilience4jmaxConcurrentCalls=100연계별 동시 호출 제한TimeLimiter업무별Resilience4jtimeoutDuration=5s비동기 호출 시간 제한GUID 전달필수HTTP Header InterceptorX-GUID: ${guid}E2E 추적설정 예시cruzapim:  base-url: https://cruzapim.nh.local  connect-timeout: 3000  read-timeout: 5000  retry-enabled: falseresilience4j:  circuitbreaker:    instances:      cruzApim:        failureRateThreshold: 50        slidingWindowSize: 100  bulkhead:    instances:      cruzApim:        maxConcurrentCalls: 100        maxWaitDuration: 0작업 가이드외부연계 Timeout은 업무 오류코드와 연결해야 한다. 사용자는 “연계 시스템 응답 지연” 정도의 메시지를 받고, 운영자는 GUID로 AP 로그와 CruzAPIM 로그를 연결해 원인을 확인해야 한다.12. Kafka / CDC / 이벤트 처리 타임아웃 설정 가이드Kafka와 CDC의 Timeout은 사용자 동기 응답시간과 직접 연결하기보다 이벤트 지연, 재처리, Lag 감시에 연결된다. 이벤트 흐름은 FAST 흐름으로 보고, 온라인 조회와 다른 기준으로 관리한다.

설정 항목권장값실제 설정 위치설정 예시설명Producer request.timeout.ms30초producer configrequest.timeout.ms=30000Broker 응답 대기Producer delivery.timeout.ms120초producer configdelivery.timeout.ms=120000전체 전송 한도Consumer session.timeout.ms30초consumer configsession.timeout.ms=30000Consumer 장애 감지Consumer max.poll.interval.ms업무별consumer configmax.poll.interval.ms=300000처리 지연 허용 한도Retry Topic적용Kafka 설계retry-topic재처리 분리DLQ필수Kafka 설계dead-letter-topic반복 실패 격리Consumer Lag 임계치업무별 정의모니터링lag warning/critical이벤트 지연 감시설정 예시spring:  kafka:    producer:      properties:        request.timeout.ms: 30000        delivery.timeout.ms: 120000    consumer:      properties:        session.timeout.ms: 30000        max.poll.interval.ms: 300000작업 가이드Kafka Timeout은 온라인 Transaction Timeout과 같은 기준으로 보면 안 된다. 이벤트는 재처리 가능성을 전제로 설계하고, Lag·DLQ·재처리 성공률을 모니터링한다.13. ETL / DataStage / 배치 타임아웃 설정 가이드배치와 ETL은 분~시간 단위로 관리되며, 온라인 거래 Timeout과 별도 체계로 운영한다. 핵심은 배치 윈도우, 선후행, 대사, 재처리 기준을 명확히 하는 것이다.

설정 항목권장값실제 설정 위치설정 예시설명Job Timeout업무별 정의DataStage Job Option/SchedulerjobTimeout=30m~2hJob 최대 수행시간Step Timeout업무별 정의Job Step 설정stepTimeout=10m단계별 지연 감지DB Load Timeout업무별 정의ETL/DB LoaderloadTimeout적재 지연 통제File Receive Timeout업무별 정의MFT/FOSreceiveTimeout파일 수신 지연 감지Batch Window업무별 정의배치 스케줄러window=00:00~06:00온라인 영향 방지Reprocess Timeout업무별 정의재처리 기준서retry window재처리 허용시간Long Running Alert필수모니터링duration > threshold운영자 알림설정 예시Batch Timeout Policy Example- Normal Job       : 30m- Heavy ETL Job    : 2h- File Wait        : 업무별 SLA- Reprocess Window : 배치 윈도우 내- Online Time      : 대량배치 금지작업 가이드배치 Timeout은 실패 처리가 아니라 운영 판단 기준이다. Timeout 발생 시 즉시 Kill할지, 대기할지, 재처리할지, 후속 Job을 중단할지 R&R과 절차를 정해야 한다.

## 14. 로그 / 감사 / 비동기 처리 타임아웃 설정 가이드거래로그와 감사로그는 필수지만, 사용자 응답을 지연시키면 안 된다. 핵심 거래로그는 동기 최소 기록, 상세 로그와 감사 저장은 비동기화하는 것이 적합하다.

설정 항목권장값실제 설정 위치설정 예시설명MDC 설정요청 시작 즉시Filter/InterceptorMDC.put(guid)GUID 추적동기 로그 범위최소화Logback/공통모듈Start/End/Error응답 지연 방지감사로그 저장비동기 우선Async Executor/AOPauditExecutor고객조회 감사Async Queue Offer Timeout0~100msThreadPoolConfigoffer timeout로그 때문에 거래 지연 방지Async 처리 Timeout업무별Executor/Futuretimeout=1s비동기 작업 지연 감지Log Flush비동기Appender 설정AsyncAppenderI/O 지연 완화검색 SLA운영 기준로그 플랫폼GUID 1분 내 검색장애 분석 기준설정 예시logging:  pattern:    console: "%d %-5level [%X{guid}] [%X{traceId}] %logger - %msg%n"async:  audit:    core-pool-size: 20    max-pool-size: 50    queue-capacity: 10000    task-timeout-ms: 1000작업 가이드개인정보 원문, SQL 원문 내 민감값, 주민번호, 계좌번호, 고객명은 로그에 남기지 않는다. 감사 대상은 별도 마스

> *(원문 일부 발췌 — 전체는 znsight-capacity-word 참조)*

> **용도**: Batch · **연관 본문**: 33장

## Batch·Scheduler 설정표 — 실무 템플릿

본 부록은 **Scheduler** 영역의 표준 템플릿입니다. 상단 **NSIGHT 1차 표준 전제**와 함께 사용합니다.

### Batch·Scheduler 설정표

| 항목 | 권고 |
|------|------|
| Window | 00:00~06:00 |
| Pool | 배치 전용 DataSource |
| Chunk | 100~500 |
| 중복 실행 | ShedLock / DB Lock |
| CPU | 온라인과 분리 VM |

### 적용 절차

| 단계 | 작업 | 담당 |
|------|------|------|
| 1 | 권고값 초안 작성 | 아키텍처·WAS |
| 2 | DEV 환경 적용·단위 검증 | 개발 |
| 3 | STG 360/720 TPS 시험 | 성능시험 |
| 4 | 확정값 PRD 반영 | 운영·TA |
| 5 | 변경관리 이력 등록(부록 AB) | 운영 |

### 환경별 설정 차이

| 항목 | DEV | STG | PRD |
|------|-----|-----|-----|
| 수치 | 완화 가능 | 권고값 | **확정값** |
| leakDetection | 60s | 60s | 선택 |
| Actuator | 전체 | metrics+health | 제한 노출 |
| 로그 레벨 | DEBUG | INFO | INFO/WARN |

### 체크리스트

| # | 확인 |
|---|------|
| 1 | NSIGHT 1차 표준(21,600명·720 TPS) 전제 반영 |
| 2 | Timeout 계층 정합 (M 부록) |
| 3 | Pool 합산 ≤ DB max (V 부록) |
| 4 | 360/720 TPS 시험 합격 (X·Z 부록) |
| 5 | ENV rule-check 통과 |

### 트러블슈팅

| 증상 | 점검 | 조치 |
|------|------|------|
| p95 급증 | Thread·Pool·SQL | GUID Trace |
| Pool Pending | SQL p95 vs Pool 크기 | SQL 튜닝 우선 |
| Timeout 다발 | 계층 역전 여부 | M 부록 대조 |
| 센터 장애 | 잔여 TPS | W 부록 |

## 산정 공식 참조

```
동시요청자 = 전체사용자 × 동시요청률
TPS = 동시요청자 ÷ 3
AP = ⌈TPS ÷ 250⌉ (A-A)
Pool = max(30, min(TPS×0.15×1.3, Thread×30%))
```

## 검증 기준

| 시나리오 | 합격 기준 |
|----------|----------|
| 360 TPS | p95≤3s, CPU≤70% |
| 720 TPS | Thread/Pool 고갈 없음 |
| 1,080 TPS | Fail-fast 정상 |
| 센터 장애 | 잔여 TPS≥720 |

## CAP/ENV 연동

- 용량산정: `/oc/capacity.html` · `/api/oc/capacity/*`
- 환경설정: `/oc/env-002.html` · `/api/oc/env/rule-check`

## 연관 본문

| 본문 챕터 | 내용 |
|----------|------|
| 33 | Scheduler 상세 |

### 연관 부록

| 부록 | 내용 |
|------|------|
| A~B | 산정 입력·TPS |
| G~L | 솔루션 템플릿 |
| M | Timeout 매트릭스 |
| V~W | DB·센터 장애 |
| X~Z | 시험·검증 |
| AA~AB | 전환·변경 |

### 720 TPS 실무 예시

| 항목 | 산출 | 설정 연결 |
|------|------|----------|
| 사용자 | 21,600 | — |
| 동시요청(10%) | 2,160 | — |
| TPS | 720 | — |
| AP | 8 (A-A) | 8C/32G VM |
| Thread | 400~500 | maxThreads |
| Pool/VM | 50 | HikariCP |
| DB Session | 400 | max sessions |
| 잔여(센터 Down) | 1,000 | W 부록 |

### 작성·승인

| 역할 | 담당 | 산출물 |
|------|------|--------|
| PMO·업무 | 입력값 합의 | A 부록 |
| 아키텍처 | 산정·권고값 | 본 부록 |
| 성능시험 | 실측·확정값 | Z 부록 |
| 운영·TA | PRD 반영 | AB 부록 |


## 절별 상세

### S.1 Scheduler

본 절은 **부록 S** — **Scheduler** (Batch·Scheduler 설정표) NSIGHT 1차 표준 적용 기준입니다.

| 항목 | 권고 | 비고 |
|------|------|------|
| 기준 VM | 8 vCPU / 32GB | Scale-Out |
| TPS | 360/720/1080 | 피크 720 |
| p95 | 3초 이하 | SLA |
| 검증 | 360/720/1080 | 성능시험 |

#### 검증 기준

| 시나리오 | 합격 기준 |
|----------|----------|
| 360 TPS | p95≤3s, CPU≤70% |
| 720 TPS | Thread/Pool 고갈 없음 |
| 1,080 TPS | Fail-fast 정상 |
| 센터 장애 | 잔여 TPS≥720 |

#### 주의사항

- 권고값은 출발점 — 선도개발·시험 후 확정
- 세션≠TPS, Pool≠Thread

### S.2 Batch Window

본 절은 **부록 S** — **Batch Window** (Batch·Scheduler 설정표) NSIGHT 1차 표준 적용 기준입니다.

| 항목 | 권고 | 비고 |
|------|------|------|
| 기준 VM | 8 vCPU / 32GB | Scale-Out |
| TPS | 360/720/1080 | 피크 720 |
| p95 | 3초 이하 | SLA |
| 검증 | 360/720/1080 | 성능시험 |

#### 검증 기준

| 시나리오 | 합격 기준 |
|----------|----------|
| 360 TPS | p95≤3s, CPU≤70% |
| 720 TPS | Thread/Pool 고갈 없음 |
| 1,080 TPS | Fail-fast 정상 |
| 센터 장애 | 잔여 TPS≥720 |

#### 주의사항

- 권고값은 출발점 — 선도개발·시험 후 확정
- 세션≠TPS, Pool≠Thread

### S.3 Chunk

본 절은 **부록 S** — **Chunk** (Batch·Scheduler 설정표) NSIGHT 1차 표준 적용 기준입니다.

| 항목 | 권고 | 비고 |
|------|------|------|
| 기준 VM | 8 vCPU / 32GB | Scale-Out |
| TPS | 360/720/1080 | 피크 720 |
| p95 | 3초 이하 | SLA |
| 검증 | 360/720/1080 | 성능시험 |

#### 검증 기준

| 시나리오 | 합격 기준 |
|----------|----------|
| 360 TPS | p95≤3s, CPU≤70% |
| 720 TPS | Thread/Pool 고갈 없음 |
| 1,080 TPS | Fail-fast 정상 |
| 센터 장애 | 잔여 TPS≥720 |

#### 주의사항

- 권고값은 출발점 — 선도개발·시험 후 확정
- 세션≠TPS, Pool≠Thread

### S.4 Pool 분리

본 절은 **부록 S** — **Pool 분리** (Batch·Scheduler 설정표) NSIGHT 1차 표준 적용 기준입니다.

| 항목 | 권고 | 비고 |
|------|------|------|
| 기준 VM | 8 vCPU / 32GB | Scale-Out |
| TPS | 360/720/1080 | 피크 720 |
| p95 | 3초 이하 | SLA |
| 검증 | 360/720/1080 | 성능시험 |

#### 검증 기준

| 시나리오 | 합격 기준 |
|----------|----------|
| 360 TPS | p95≤3s, CPU≤70% |
| 720 TPS | Thread/Pool 고갈 없음 |
| 1,080 TPS | Fail-fast 정상 |
| 센터 장애 | 잔여 TPS≥720 |

#### 주의사항

- 권고값은 출발점 — 선도개발·시험 후 확정
- 세션≠TPS, Pool≠Thread

### S.5 검증

본 절은 **부록 S** — **검증** (Batch·Scheduler 설정표) NSIGHT 1차 표준 적용 기준입니다.

| 항목 | 권고 | 비고 |
|------|------|------|
| 기준 VM | 8 vCPU / 32GB | Scale-Out |
| TPS | 360/720/1080 | 피크 720 |
| p95 | 3초 이하 | SLA |
| 검증 | 360/720/1080 | 성능시험 |

#### 검증 기준

| 시나리오 | 합격 기준 |
|----------|----------|
| 360 TPS | p95≤3s, CPU≤70% |
| 720 TPS | Thread/Pool 고갈 없음 |
| 1,080 TPS | Fail-fast 정상 |
| 센터 장애 | 잔여 TPS≥720 |

#### 주의사항

- 권고값은 출발점 — 선도개발·시험 후 확정
- 세션≠TPS, Pool≠Thread


---

[← 목차](../00-목차.md)
