/**
 * 부록 A~AD — 23장 수준 심화 본문
 */
const { buildAppendixARichBlock } = require('./cap-field-mapping');
function premise() {
  return `| 항목 | 기준값 |
|------|--------|
| 지점 수 | 3,600 |
| 전체 사용자 | 21,600 |
| 설계 세션 | 26,000~28,000 |
| TPS | 360 / 720 / 1,080 |
| 기준 VM | 8 vCPU / 32GB |
| VM당 TPS | 250 |
| p95 | 3초 이하 |`;
}

function verify() {
  return `| 시나리오 | 합격 기준 |
|----------|----------|
| 360 TPS | p95≤3s, CPU≤70% |
| 720 TPS | Thread/Pool 고갈 없음 |
| 1,080 TPS | Fail-fast 정상 |
| 센터 장애 | 잔여 TPS≥720 |`;
}

const APPENDIX_META = {
  A: { ch: '8, 45', use: 'CAP-010 산정 입력', domain: '용량산정 입력' },
  B: { ch: '8, 9', use: 'TPS 산출', domain: '사용자·TPS 계산' },
  C: { ch: '10', use: 'Core 교차검증', domain: 'TPMC·Core' },
  D: { ch: '11, 37~40', use: 'VM 선정', domain: 'VM 비교' },
  E: { ch: '15, 43', use: 'ENV 문서화', domain: '설정 표준 양식' },
  F: { ch: '37~40', use: '프로파일 적용', domain: '8C/16C/32C' },
  G: { ch: '22', use: 'server.xml', domain: 'Tomcat Connector' },
  H: { ch: '23', use: 'setenv.sh', domain: 'JVM 옵션' },
  I: { ch: '24', use: 'application.yml', domain: 'Spring Boot' },
  J: { ch: '25', use: 'TCF 설정', domain: 'ServiceId·GUID' },
  K: { ch: '26', use: 'HikariCP', domain: 'DB Pool' },
  L: { ch: '27', use: 'MyBatis', domain: 'SQL Timeout' },
  M: { ch: '16', use: 'Timeout 계층', domain: 'Timeout 매트릭스' },
  N: { ch: '18~20', use: 'L4·Apache', domain: 'GSLB·L4·WEB' },
  O: { ch: '21', use: 'Gateway', domain: 'Route·CB' },
  P: { ch: '29, 30', use: '세션·JWT', domain: 'Session·JWT' },
  Q: { ch: '31', use: 'Cache', domain: 'TTL·Eviction' },
  R: { ch: '32', use: 'CruzAPIM', domain: 'HTTP Client' },
  S: { ch: '33', use: 'Batch', domain: 'Scheduler' },
  T: { ch: '34', use: 'Kafka', domain: 'CDC·Lag' },
  U: { ch: '36, 49', use: '로그·APM', domain: '관측성' },
  V: { ch: '26, 28', use: 'DB Session 합산', domain: 'Pool 총량' },
  W: { ch: '11, 45', use: 'DR·센터 장애', domain: '잔여 TPS' },
  X: { ch: '47', use: '성능시험', domain: '시나리오' },
  Y: { ch: '49', use: 'APM 임계치', domain: 'Warning/Critical' },
  Z: { ch: '48', use: '시험 결과', domain: '검증결과서' },
  AA: { ch: '52', use: 'Go-Live', domain: '운영 전환' },
  AB: { ch: '53', use: '변경 이력', domain: '변경관리' },
  AC: { ch: '42', use: 'ADR', domain: '아키텍처 결정' },
  AD: { ch: '45, 57', use: '최종 산정', domain: '결과서' },
};

const TEMPLATES = {
  A: `${buildAppendixARichBlock()}

### 산출 항목

| 산출 | 산식 |
|------|------|
| totalUsers | branchCount × userPerBranch |
| designedSessions | totalUsers × (1 + sessionMarginRate) |
| 동시요청자 | totalUsers × concurrentRequestRates |
| TPS | ⌈동시요청자 ÷ targetResponseTimes⌉ |
| vmTpsAtBase | vmCores × tpsPerCore |
| AP 대수 | CAP-030 (A-A·DR 반영) |
| Pool/VM | CAP-050 max(30, min(②,③)) |
| DB Session | Σ(AP×Pool) + 배치 + 20% |`,

  B: `### 사용자·세션·TPS 계산표

| 시나리오 | 동시요청률 | 동시요청자 | TPS(3초) | AP(250) |
|----------|-----------|-----------|---------|---------|
| 기본 | 5% | 1,080 | 360 | 4 (A-A 6) |
| 피크 | 10% | 2,160 | 720 | 6 (A-A 8) |
| 스트레스 | 15% | 3,240 | 1,080 | 9 (A-A 12) |

\`\`\`
전체 사용자 = 3,600 × 6 = 21,600
설계 세션 = 21,600 × 1.2~1.3 = 26,000~28,000
TPS = (21,600 × 동시요청률) ÷ 3
\`\`\`

> **주의**: 세션 수 ≠ TPS. 세션은 로그인 유지, TPS는 동시 요청자 기준.`,

  C: `### TPMC·Core 교차검증표

| 항목 | 이론/벤치 | NSIGHT 실효 | 비고 |
|------|-----------|-------------|------|
| TPMC/TPS | 60 | 1,500~3,500 | 정보계 |
| TPS/Core | 1,782 (참고) | **30~40** | 보수 |
| 8Core VM | — | **250 TPS** | Scale-Out |
| CPU 목표 | — | 60~70% | 운영 |

\`\`\`
교차검증: VM TPS ≤ Core × 35 × 0.7
8 × 35 × 0.7 = 196 → 권고 250 (여유)
\`\`\`

| 업무 | TPMC/TPS | Core TPS |
|------|----------|----------|
| 일반 조회 | 1,500~2,000 | 30~35 |
| SingleView | 2,000~3,500 | 35~40 |
| 등록·변경 | 2,500+ | 25~30 |`,

  D: `### VM 사양별 비교 (온라인 AP)

| 항목 | 8C/32G | 16C/64G | 32C/256G |
|------|--------|---------|----------|
| 용도 | **온라인 표준** | 특수·배치 | ETL·배치 |
| TPS/VM | 250 | ~500 | 배치 |
| 장애 영향 | 12.5% (8대) | 25% (4대) | 큼 |
| Heap | 12~14GB | 24~28GB | 32GB+ |
| Scale-Out | **권장** | 제한 | 비권장 |

**결론**: 온라인 AP는 8C/32G Scale-Out이 운영 안정성 최적.`,

  E: `### 솔루션 환경설정 표준 양식

| 필드 | 예시 | 필수 |
|------|------|------|
| 항목명 | maxThreads | ✓ |
| 설정 위치 | server.xml Connector | ✓ |
| 단위 | 개 | ✓ |
| 가정값 | — | |
| 권고값 | 500 | ✓ |
| 확정값 | 480 (시험 후) | ✓ |
| 산식/근거 | 250×1.2×1.2 | |
| 검증 방법 | 720 TPS 시험 | ✓ |
| 변경번호 | CHG-2026-001 | ✓ |`,

  F: `### 8C·16C·32C 표준 프로파일 요약

| 프로파일 | Thread | Heap | Pool | Tx | Proxy |
|----------|--------|------|------|-----|-------|
| 8C/32G | 500 | 12~14G | 50/60 | 5s | 10s |
| 16C/64G | 600~800 | 24~28G | 60~80 | 5s | 10s |
| 16C/128G | 800 | 28~32G | 60~80 | 5s | 10s |
| 32C/256G | 800+ | 32G+ | 업무별 | 5s | 10s |`,

  G: `### Tomcat Connector 템플릿 (8C/32G)

\`\`\`xml
<Connector port="8080"
  protocol="org.apache.coyote.http11.Http11NioProtocol"
  maxThreads="500"
  minSpareThreads="100"
  acceptCount="500"
  maxConnections="10000"
  connectionTimeout="8000"
  keepAliveTimeout="120000"
  maxKeepAliveRequests="200" />
\`\`\`

\`\`\`yaml
# Spring Boot 내장 Tomcat
server:
  tomcat:
    threads:
      max: 500
      min-spare: 100
    accept-count: 500
    max-connections: 10000
\`\`\``,

  H: `### JVM 옵션 템플릿 (8C/32G 일반 AP)

\`\`\`shell
# setenv.sh
JAVA_OPTS="-Xms12g -Xmx12g -Xss512k"
JAVA_OPTS="$JAVA_OPTS -XX:+UseG1GC -XX:MaxGCPauseMillis=200"
JAVA_OPTS="$JAVA_OPTS -XX:MetaspaceSize=256m -XX:MaxMetaspaceSize=1g"
JAVA_OPTS="$JAVA_OPTS -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/logs/dump"
JAVA_OPTS="$JAVA_OPTS -XX:+ExitOnOutOfMemoryError"
JAVA_OPTS="$JAVA_OPTS -Xlog:gc*,safepoint:file=/logs/gc/ap-gc-%t.log:time,uptime,level,tags:filecount=10,filesize=100M"
JAVA_OPTS="$JAVA_OPTS -Dfile.encoding=UTF-8 -Duser.timezone=Asia/Seoul"
\`\`\`

**SV AP**: Heap 14GB. 16C는 Heap 24~28GB 별도 검토.`,

  I: `### Spring Boot application.yml 템플릿

\`\`\`yaml
server:
  shutdown: graceful
spring:
  lifecycle:
    timeout-per-shutdown-phase: 30s
  datasource:
    hikari:
      maximum-pool-size: 50
      connection-timeout: 3000
      auto-commit: false
  transaction:
    default-timeout: 5
mybatis:
  configuration:
    default-statement-timeout: 3
    default-fetch-size: 300
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
\`\`\``,

  J: `### TCF 환경설정 템플릿

\`\`\`yaml
nsight:
  tcf:
    service-timeout:
      default: 5
      inquiry: 4
      register: 5
    guid:
      required: true
      mdc-key: GUID
    idempotency:
      enabled: true
      ttl: 300
    audit:
      enabled: true
spring:
  transaction:
    default-timeout: 5
nsight:
  cruzapim:
    connect-timeout: 2000
    read-timeout: 5000
\`\`\`

| 영역 | 항목 | 권고 | 위치 |
|------|------|------|------|
| TCF | ServiceId Timeout | 4~5초 | TCF 설정 |
| TCF | GUID/MDC | 필수 | Filter |
| TCF | Idempotency | 활성 | TCF |
| Spring | Tx Timeout | 5초 | @Transactional |
| 연계 | CruzAPIM Connect | 3s | WebClient |
| 연계 | CruzAPIM Read | 5s | WebClient |
| Resilience | CB·Bulkhead | 필수 | Resilience4j |
| Retry | 동기거래 | **금지** | — |`,

  K: `### HikariCP 템플릿

\`\`\`yaml
spring.datasource.hikari:
  pool-name: marketing-pool
  maximum-pool-size: 50        # SV: 60
  minimum-idle: 15
  connection-timeout: 3000
  validation-timeout: 1000
  idle-timeout: 600000
  max-lifetime: 1800000
  keepalive-time: 120000
  auto-commit: false
\`\`\`

\`\`\`
Pool = max(30, min(AP_TPS × 0.15 × 1.3, Thread × 30%))
8C/250TPS → Pool ≈ 50
\`\`\``,

  L: `### MyBatis 설정 템플릿

\`\`\`yaml
mybatis:
  configuration:
    default-statement-timeout: 3
    default-fetch-size: 300
    map-underscore-to-camel-case: true
    local-cache-scope: STATEMENT
    lazy-loading-enabled: false
\`\`\`

\`\`\`xml
<select id="findList" timeout="3" fetchSize="300" resultType="...">
  SELECT ... FROM RDW_... WHERE ... FETCH FIRST 100 ROWS ONLY
</select>
\`\`\`

| SQL 유형 | timeout | 비고 |
|----------|---------|------|
| 일반 조회 | 3s | RDW |
| SV 핵심 | 3s | — |
| 복합 조회 | 5s | 예외 승인 |
| 분석 SQL | — | ADW 전용 |`,

  M: `### Timeout 매트릭스

| 계층 | 권고 | 설정 위치 | 검증 |
|------|------|----------|------|
| SQL | 3s | MyBatis | rule-check |
| Pool 획득 | 3s | Hikari | ≠ SQL |
| Transaction | 5s | @Transactional | > SQL |
| TCF ServiceId | 4~5s | TCF | — |
| CruzAPIM Connect | 3s | WebClient | — |
| CruzAPIM Read | 5s | WebClient | CB |
| Proxy | 10s | Apache | — |
| WebTop | 15s | 단말 | — |
| L4 Idle | 120s | L4 | = Tomcat |`,

  N: `### GSLB·L4·WEB 설정표

| 계층 | 항목 | 권고 |
|------|------|------|
| GSLB | TTL | 30초 |
| L4 | LB Method | Round Robin |
| L4 | Sticky | JSESSIONID, 70분 |
| L4 | Health | /actuator/health/l4, 5s/2s/Fail3 |
| L4 | Idle | 120초 |
| Apache | ProxyTimeout | 10s |
| Apache | KeepAlive | 120s |
| Apache | X-GUID | Header 전달 |`,

  O: `### Gateway 설정표

\`\`\`yaml
spring.cloud.gateway:
  httpclient:
    connect-timeout: 3000
    response-timeout: 10s
  routes:
    - id: marketing-api
      uri: lb://marketing-ap
      predicates:
        - Path=/api/marketing/**
      filters:
        - name: CircuitBreaker
          args:
            name: marketingCB
\`\`\`

| 항목 | 권고 |
|------|------|
| route timeout | ≤ Backend Proxy |
| CB | failure 50% |
| Rate Limit | 업무별 |`,

  P: `### 세션·JWT 설정표

| 항목 | 권고 | 위치 |
|------|------|------|
| Session Idle | 60분 | application.yml |
| Absolute | 8시간 | Filter |
| Cookie Secure | true | — |
| Access Token | 15~30분 | JWT |
| Refresh Token | 8시간 | JWT |
| JWKS Cache | 5~10분 | — |`,

  Q: `### Cache 설정표

\`\`\`yaml
spring.cache:
  type: caffeine
  caffeine:
    spec: maximumSize=10000,expireAfterWrite=300s
\`\`\`

| 유형 | TTL | 비고 |
|------|-----|------|
| Local | 1~5분 | Stampede 방지 |
| Redis | 5~30분 | fallback 정책 |
| SV 마스터 | 10~30분 | 무효화 정책 |`,

  R: `### 외부연계 Client 설정표

\`\`\`yaml
nsight:
  cruzapim:
    connect-timeout: 3000
    read-timeout: 5000
resilience4j:
  circuitbreaker:
    instances:
      cruzapim:
        failure-rate-threshold: 50
        wait-duration-in-open-state: 30s
  bulkhead:
    instances:
      cruzapim:
        max-concurrent-calls: 50
\`\`\`

**Retry 기본 금지** — GUID 상태조회 API 사용.`,

  S: `### Batch·Scheduler 설정표

| 항목 | 권고 |
|------|------|
| Window | 00:00~06:00 |
| Pool | 배치 전용 DataSource |
| Chunk | 100~500 |
| 중복 실행 | ShedLock / DB Lock |
| CPU | 온라인과 분리 VM |`,

  T: `### Kafka·CDC 설정표

\`\`\`yaml
spring.kafka.consumer:
  max-poll-records: 500
  max-poll-interval-ms: 300000
  enable-auto-commit: false
\`\`\`

| 지표 | Warning | Critical |
|------|---------|----------|
| Consumer Lag | 업무별 | DLQ |
| DLQ | 필수 | — |
| 온라인 SLA | 별도 기준 | — |`,

  U: `### 로그·모니터링 설정표

| 항목 | 권고 |
|------|------|
| Access Log | GUID·TraceId 필수 |
| GC Log | filecount=10, 100M |
| APM | p95·Slow SQL |
| Actuator | health, metrics, prometheus |
| MDC | GUID, userId, centerId |

\`\`\`xml
<Pattern>%d{yyyy-MM-dd HH:mm:ss} [%X{GUID}] [%X{traceId}] %msg%n</Pattern>
\`\`\``,

  V: `### 전체 DB Connection 합산표

| 구분 | 산식 | 예(720 TPS) |
|------|------|-------------|
| AP당 Pool | 50 | 50 |
| AP 대수 | 8 | 8 |
| 온라인 Pool | AP×Pool | 400 |
| 배치 | 별도 | 50 |
| BI/운영 | 별도 | 30 |
| 여유 20% | ×1.2 | 576 |
| **DB max sessions** | ≥ 합계 | DBA 확인 |

\`\`\`
Σ(AP × Pool × DataSource) ≤ DB max sessions × 80%
\`\`\``,

  W: `### 센터 장애 수용량표

| TPS | AP 총 | 센터당 | 1센터 Down 잔여 | 판정 |
|-----|-------|--------|----------------|------|
| 360 | 4 | 2 | 2×250=500 | ✓ |
| 720 | 8 | 4 | 4×250=1000 | ✓ |
| 1080 | 12 | 6 | 6×250=1500 | ✓ |

**규칙**: 잔여 TPS ≥ 목표 TPS(720). 센터당 AP **4대 권장**.`,

  X: `### 성능시험 시나리오

| # | 시나리오 | 부하 | 합격 기준 |
|---|----------|------|----------|
| 1 | 기본 | 360 TPS | p95≤3s, CPU≤70% |
| 2 | 피크 | 720 TPS | Pending=0 |
| 3 | 스트레스 | 1,080 TPS | Fail-fast |
| 4 | AP 1대 Down | 720 TPS | L4 failover |
| 5 | 센터 장애 | 720 TPS | 잔여≥720 |
| 6 | Slow SQL | 720 TPS | SQL timeout→Thread 회수 |
| 7 | Pool 고갈 | 720 TPS | connectionTimeout 3s |
| 8 | CruzAPIM 지연 | 720 TPS | CB Open |
| 9 | Rolling 배포 | 720 TPS | Drain·무중단 |
| 10 | OOM 모의 | — | Dump·재기동 |`,

  Y: `### 운영 임계치 표준표

| 지표 | Warning | Critical | 조치 |
|------|---------|----------|------|
| CPU | 70% | 85% | Scale-Out |
| Heap | 70% | 85% | Dump·GC |
| Hikari Active | 70% | 90% | Pool·SQL |
| Hikari Pending | >0 1분 | >0 5분 | 즉시 |
| GC Pause p95 | 200ms | 500ms | Heap |
| p95 응답 | 2.5s | 3s | E2E |
| 오류율 | 0.5% | 1% | Trace |`,

  Z: `### 설정값 검증결과서 양식

| 항목 | 권고값 | 실측값 | 판정 | 비고 |
|------|--------|--------|------|------|
| maxThreads | 500 | | | 720 TPS |
| Pool | 50 | | | Pending |
| Heap | 12GB | | | GC p95 |
| p95 | ≤3s | | | |
| CPU | ≤70% | | | |

**판정**: 전 항목 합격 시 운영 전환 가능(52장).`,

  AA: `### 운영 전환 체크리스트

| # | 항목 | 확인 |
|---|------|------|
| 1 | 산정표 PMO·DBA 승인 | ☐ |
| 2 | 360/720/1080 시험 합격 | ☐ |
| 3 | Timeout rule-check 통과 | ☐ |
| 4 | Pool 합산 ≤ DB max | ☐ |
| 5 | GC Log·Dump 경로 | ☐ |
| 6 | APM 임계치 등록 | ☐ |
| 7 | Runbook(Thread/Heap Dump) | ☐ |
| 8 | Rollback 절차 | ☐ |
| 9 | L4 Drain·Graceful | ☐ |
| 10 | 변경관리 이력 | ☐ |`,

  AB: `### 설정 변경관리대장

| 필드 | 설명 |
|------|------|
| 변경번호 | CHG-YYYY-NNN |
| 요청일 | |
| 요청자/승인자 | |
| 영향 챕터 | 22~26 등 |
| 변경 전/후 | |
| 영향도 | TPS·Pool·Timeout |
| STG 검증 | 360/720 |
| PRD 반영일 | |
| Rollback | |`,

  AC: `### 아키텍처 의사결정 기록 (ADR)

| # | 결정 | 근거 | 대안 |
|---|------|------|------|
| ADR-001 | 8C/32G Scale-Out | 장애 격리 | 16C Scale-Up |
| ADR-002 | 2센터 A-A AP | 잔여 TPS | Active-Standby |
| ADR-003 | Pool 50/VM | 산식+시험 | Pool 80 |
| ADR-004 | G1GC 표준 | p95 SLA | ZGC |
| ADR-005 | Retry 금지 | 부하 증폭 | 3회 Retry |`,

  AD: `### 용량산정 최종 결과서 (720 TPS)

| 항목 | 값 |
|------|-----|
| 전체 사용자 | 21,600 |
| 동시요청률(피크) | 10% |
| TPS | 720 |
| AP 대수 | 8 (A-A) |
| maxThreads | 500 |
| Pool/VM | 50 |
| DB Session(온라인) | 400 |
| 잔여 TPS(센터 장애) | 1,000 |
| p95 | ≤3s |
| 확정일 | 선도개발·시험 후 |`,
};

function buildAppendixRich(key, title) {
  const meta = APPENDIX_META[key] || { ch: '—', use: '—', domain: title };
  const template = TEMPLATES[key] || `### ${title} 템플릿\n\n| 항목 | 권고 |\n|------|------|\n| 기준 VM | 8 vCPU / 32GB |\n| TPS | 360/720/1080 |`;
  const lines = [
    `> **용도**: ${meta.use} · **연관 본문**: ${meta.ch}장`,
    '',
    `## ${title} — 실무 템플릿`,
    '',
    `본 부록은 **${meta.domain}** 영역의 표준 템플릿입니다. 상단 **NSIGHT 1차 표준 전제**와 함께 사용합니다.`,
    '',
    template,
    '',
    '### 적용 절차',
    '',
    '| 단계 | 작업 | 담당 |',
    '|------|------|------|',
    '| 1 | 권고값 초안 작성 | 아키텍처·WAS |',
    '| 2 | DEV 환경 적용·단위 검증 | 개발 |',
    '| 3 | STG 360/720 TPS 시험 | 성능시험 |',
    '| 4 | 확정값 PRD 반영 | 운영·TA |',
    '| 5 | 변경관리 이력 등록(부록 AB) | 운영 |',
    '',
    '### 환경별 설정 차이',
    '',
    '| 항목 | DEV | STG | PRD |',
    '|------|-----|-----|-----|',
    '| 수치 | 완화 가능 | 권고값 | **확정값** |',
    '| leakDetection | 60s | 60s | 선택 |',
    '| Actuator | 전체 | metrics+health | 제한 노출 |',
    '| 로그 레벨 | DEBUG | INFO | INFO/WARN |',
    '',
    '### 체크리스트',
    '',
    '| # | 확인 |',
    '|---|------|',
    '| 1 | NSIGHT 1차 표준(21,600명·720 TPS) 전제 반영 |',
    '| 2 | Timeout 계층 정합 (M 부록) |',
    '| 3 | Pool 합산 ≤ DB max (V 부록) |',
    '| 4 | 360/720 TPS 시험 합격 (X·Z 부록) |',
    '| 5 | ENV rule-check 통과 |',
    '',
    '### 트러블슈팅',
    '',
    '| 증상 | 점검 | 조치 |',
    '|------|------|------|',
    '| p95 급증 | Thread·Pool·SQL | GUID Trace |',
    '| Pool Pending | SQL p95 vs Pool 크기 | SQL 튜닝 우선 |',
    '| Timeout 다발 | 계층 역전 여부 | M 부록 대조 |',
    '| 센터 장애 | 잔여 TPS | W 부록 |',
    '',
    '## 산정 공식 참조',
    '',
    '```',
    '동시요청자 = 전체사용자 × 동시요청률',
    'TPS = 동시요청자 ÷ 3',
    'AP = ⌈TPS ÷ 250⌉ (A-A)',
    'Pool = max(30, min(TPS×0.15×1.3, Thread×30%))',
    '```',
    '',
    '## 검증 기준',
    '',
    verify(),
    '',
    '## CAP/ENV 연동',
    '',
    '- 용량산정: `/oc/capacity.html` · `/api/oc/capacity/*`',
    '- 환경설정: `/oc/env-002.html` · `/api/oc/env/rule-check`',
    '',
    '## 연관 본문',
    '',
    `| 본문 챕터 | 내용 |`,
    `|----------|------|`,
    `| ${meta.ch} | ${meta.domain} 상세 |`,
    '',
    '### 연관 부록',
    '',
    '| 부록 | 내용 |',
    '|------|------|',
    '| A~B | 산정 입력·TPS |',
    '| G~L | 솔루션 템플릿 |',
    '| M | Timeout 매트릭스 |',
    '| V~W | DB·센터 장애 |',
    '| X~Z | 시험·검증 |',
    '| AA~AB | 전환·변경 |',
  ];

  // 720 TPS 실무 예시 (전 부록 공통)
  lines.push(
    '',
    '### 720 TPS 실무 예시',
    '',
    '| 항목 | 산출 | 설정 연결 |',
    '|------|------|----------|',
    '| 사용자 | 21,600 | — |',
    '| 동시요청(10%) | 2,160 | — |',
    '| TPS | 720 | — |',
    '| AP | 8 (A-A) | 8C/32G VM |',
    '| Thread | 400~500 | maxThreads |',
    '| Pool/VM | 50 | HikariCP |',
    '| DB Session | 400 | max sessions |',
    '| 잔여(센터 Down) | 1,000 | W 부록 |',
    '',
    '### 작성·승인',
    '',
    '| 역할 | 담당 | 산출물 |',
    '|------|------|--------|',
    '| PMO·업무 | 입력값 합의 | A 부록 |',
    '| 아키텍처 | 산정·권고값 | 본 부록 |',
    '| 성능시험 | 실측·확정값 | Z 부록 |',
    '| 운영·TA | PRD 반영 | AB 부록 |',
    '',
  );

  const extra = APPENDIX_EXTRA[key];
  if (extra) lines.push(extra, '');

  return lines.join('\n');
}

const APPENDIX_EXTRA = {
  C: `### TPMC 시험 보정 절차

1. 선도개발 대표거래 10종 TPMC 측정
2. Core TPS = 측정 TPS ÷ 사용 Core
3. 보정계수 = 권고 35 ÷ 실측 Core TPS
4. VM TPS = 250 × 보정계수 (상한 300)

| 시험 TPS | CPU | 판정 |
|----------|-----|------|
| 360 | ≤70% | 기본 합격 |
| 720 | ≤70% | 피크 합격 |
| 1,080 | Fail-fast | 스트레스 |`,
  D: `### VM 선정 의사결정

| 질문 | 8C Scale-Out | 16C Scale-Up |
|------|-------------|--------------|
| 장애 격리 | ✓ | △ |
| Rolling | ✓ | △ |
| RDW 보호 | ✓ | △ |
| 자원 효율 | △ | ✓ |

**NSIGHT 결론**: 온라인 AP = 8C/32G. 16C/32C = 배치·ETL·특수.`,
  J: `### TCF 코드 적용 예시

\`\`\`java
@Transactional(readOnly = true, timeout = 5)
public CustomerView getCustomerView(String customerId) {
    return singleViewService.findCustomerView(customerId);
}
\`\`\`

Timeout 계층: SQL(3s) < Tx(5s) < CruzAPIM(5s) < Proxy(10s) < Web(15s)`,
  M: `### Timeout rule-check 예시

| 검사 | 규칙 | 결과 예 |
|------|------|--------|
| SQL < Tx | 3 < 5 | PASS |
| Tx < Proxy | 5 < 10 | PASS |
| Pool < Tx | 3 < 5 | PASS |
| L4 > Tomcat | 120 > 120 | PASS |`,
};

module.exports = { buildAppendixRich, APPENDIX_META, TEMPLATES };
