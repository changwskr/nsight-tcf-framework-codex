/**
 * 모든 절을 23장(JVM) 수준으로 생성
 * 구조: 목적 → 권고값 표 → 설정 예시 → 검증 기준 → 주의사항
 */
const { buildCap010MappingTable, buildEnv002MappingTable } = require('./cap-field-mapping');

const STD = {
  branches: 3600,
  usersPerBranch: 6,
  totalUsers: 21600,
  sessions: '26,000~28,000',
  tps: { base: 360, peak: 720, stress: 1080 },
  vm: '8 vCPU / 32GB',
  tpsPerVm: 250,
  p95: '3초 이하',
  heap: { normal: '12GB', sv: '14GB' },
  pool: { normal: 50, sv: 60 },
  threads: '400~500',
};

const CHAPTER_CTX = {
  1: { part: '도입', domain: '용량산정 체인', files: 'CAP-010 산정표', config: '가정값·권고값·확정값' },
  2: { part: '문서 개요', domain: '가이드 범위', files: 'README·00-목차', config: '문서 변경이력' },
  3: { part: '문제 정의', domain: '설계 배경', files: '산정표·현행 대조표', config: '병목 연쇄 모델' },
  4: { part: '현행 구조', domain: 'As-Is 점검', files: 'server.xml·application.yml·setenv.sh', config: '현행 vs 권고' },
  5: { part: '요구사항', domain: 'NSIGHT 1차 요구', files: 'CAP-010', config: '21,600명·720 TPS' },
  6: { part: '설계 원칙', domain: '12대 원칙', files: '산정표', config: '피크·세션≠TPS' },
  7: { part: '연결 모델', domain: '산정→설정 변환', files: 'ENV-001·CAP-010', config: '입력·계산·설정·임계치' },
  8: { part: '사용자 모델', domain: '지점·세션', files: 'CAP-010', config: 'branchCount·userPerBranch' },
  9: { part: 'TPS 산정', domain: '동시처리량', files: 'CAP-010', config: '360/720/1080' },
  10: { part: 'TPMC·Core', domain: 'CPU 교차검증', files: 'CAP-010', config: '30~40 TPS/Core' },
  11: { part: 'VM·서버 대수', domain: 'Scale-Out', files: '인프라 산정표', config: '8대 권장' },
  12: { part: '메모리·스토리지', domain: 'Heap·디스크', files: 'setenv.sh·/logs', config: '12~14GB Heap' },
  13: { part: '네트워크', domain: 'Connection', files: 'L4·Apache·Tomcat', config: 'maxConnections' },
  14: { part: '분류체계', domain: '설정 분류', files: '환경셋팅(최종)', config: '7개 영역' },
  15: { part: '표준 표현', domain: '설정 문서화', files: 'ENV 템플릿', config: '항목·단위·산식' },
  16: { part: 'Timeout 계층', domain: '계층 Timeout', files: 'application.yml·Apache·L4', config: 'DB3·Tx5·Proxy10' },
  17: { part: 'OS·VM', domain: 'OS 튜닝', files: 'sysctl·limits.conf·systemd', config: 'ulimit·TZ' },
  18: { part: 'GSLB', domain: '센터 라우팅', files: 'GSLB Profile', config: 'TTL 30s' },
  19: { part: 'L4', domain: 'VIP·Sticky', files: 'L4 Virtual Server', config: 'Idle 120s' },
  20: { part: 'Apache', domain: 'Reverse Proxy', files: 'httpd.conf·vhost', config: 'ProxyTimeout 10s' },
  21: { part: 'Gateway', domain: 'API Gateway', files: 'gateway routes', config: 'CB·Bulkhead' },
  22: { part: 'Tomcat', domain: 'Connector', files: 'server.xml', config: 'maxThreads 500' },
  23: { part: 'JVM', domain: 'Heap·GC', files: 'setenv.sh·systemd', config: 'G1GC·12GB' },
  24: { part: 'Spring Boot', domain: 'Framework', files: 'application.yml', config: 'Tx·Graceful' },
  25: { part: 'NSIGHT TCF', domain: 'TCF 프레임워크', files: 'application.yml·TCF', config: 'ServiceId·GUID' },
  26: { part: 'HikariCP', domain: 'DB Pool', files: 'application.yml', config: 'Pool 50/60' },
  27: { part: 'MyBatis', domain: 'SQL Timeout', files: 'application.yml·Mapper XML', config: 'statement-timeout 3s' },
  28: { part: 'DBMS', domain: 'DB Session', files: 'DBA 파라미터', config: 'max sessions' },
  29: { part: '세션', domain: 'Session', files: 'web.xml·application.yml', config: 'Idle 60m' },
  30: { part: 'JWT', domain: '인증', files: 'application.yml', config: 'Access/Refresh TTL' },
  31: { part: 'Cache', domain: '캐시', files: 'application.yml', config: 'TTL·Eviction' },
  32: { part: 'HTTP Client', domain: '외부연계', files: 'CruzAPIM 설정', config: 'Connect 3s·Read 5s' },
  33: { part: 'Batch', domain: '배치', files: 'scheduler config', config: 'Window 00~06' },
  34: { part: 'Kafka·CDC', domain: '이벤트', files: 'application.yml', config: 'Lag·DLQ' },
  35: { part: '파일', domain: 'Upload/Download', files: 'application.yml', config: 'Multipart Size' },
  36: { part: '로그·관측', domain: 'Access·APM', files: 'logback·server.xml Valve', config: 'GUID·MDC' },
  37: { part: '8C/32G 프로파일', domain: '표준 프로파일', files: '전체 설정 세트', config: '250 TPS/VM' },
  38: { part: '16C/64G', domain: '특수 프로파일', files: '배치·BI', config: 'Heap 24~28GB' },
  39: { part: '16C/128G', domain: '고메모리', files: '선도검증', config: 'Cache 여유' },
  40: { part: '32C/256G', domain: '배치·ETL', files: 'ETL AP', config: 'JVM 분리' },
  41: { part: '업무 프로파일', domain: '업무별 차등', files: '업무별 yml', config: 'Pool·Timeout' },
  42: { part: '목표 아키텍처', domain: 'To-Be', files: '아키텍처도', config: 'A-A·VM 분리' },
  43: { part: '설정·배포', domain: 'Config 구조', files: 'Git·배포 파이프', config: 'profile 계층' },
  44: { part: 'RACI', domain: '책임', files: '변경관리대장', config: '승인·검증' },
  45: { part: '정상 예시', domain: '720 TPS E2E', files: '산정표', config: '8대·Pool 400' },
  46: { part: '금지 예시', domain: '안티패턴', files: '—', config: '오설정 목록' },
  47: { part: '성능시험', domain: '시험 계획', files: 'X 부록', config: '360/720/1080' },
  48: { part: '검증 기준', domain: '합격 기준', files: 'Z 부록', config: 'CPU·p95·Pending' },
  49: { part: '임계치', domain: '모니터링', files: 'APM·Actuator', config: '70/85%' },
  50: { part: '장애 흐름', domain: '장애 대응', files: 'Runbook', config: '증상·원인·조치' },
  51: { part: '자동검증', domain: 'ENV Gate', files: '/api/oc/env/*', config: 'rule-check' },
  52: { part: '운영 전환', domain: 'Go-Live', files: 'AA 체크리스트', config: '시험·Rollback' },
  53: { part: '변경관리', domain: 'Change', files: 'AB 부록', config: '요청→이력' },
  54: { part: 'Capacity Review', domain: 'Baseline', files: '월간 리포트', config: '일·주·월' },
  55: { part: '호환성', domain: 'Deprecated', files: '호환 매트릭스', config: '업그레이드' },
  56: { part: '시사점', domain: '핵심 판단', files: '—', config: '세션≠TPS' },
  57: { part: '마무리', domain: '운영 안정성', files: '—', config: '지속 보정' },
};

function verifyTable(extra) {
  const rows = [
    ['360 TPS', 'p95≤3s, CPU≤70%, 오류율≤1%'],
    ['720 TPS', 'Thread/Pool 고갈 없음, Hikari Pending=0'],
    ['1,080 TPS', 'Fail-fast·Timeout 정상, CB 동작 확인'],
    ['AP 1대 Down', 'L4 제외·잔여 TPS≥목표'],
    ['센터 장애', '잔여 센터 TPS≥720'],
  ];
  if (extra) rows.push(extra);
  return '| 시나리오 | 합격 기준 |\n|----------|----------|\n' + rows.map(([a, b]) => `| ${a} | ${b} |`).join('\n');
}

function stdPremiseTable() {
  return `| 항목 | NSIGHT 1차 권고 |\n|------|----------------|\n| 사용자 | ${STD.totalUsers.toLocaleString()}명 (${STD.branches}×${STD.usersPerBranch}) |\n| 세션 | ${STD.sessions} |\n| TPS | ${STD.tps.base} / ${STD.tps.peak} / ${STD.tps.stress} |\n| VM | ${STD.vm} |\n| VM당 TPS | ${STD.tpsPerVm} |\n| p95 | ${STD.p95} |`;
}

function timeoutTable() {
  return `| 계층 | 권고 | 설정 위치 |\n|------|------|----------|\n| SQL | 3초 | MyBatis / Mapper |\n| Pool 획득 | 3초 | Hikari connectionTimeout |\n| Transaction | 5초 | @Transactional |\n| TCF ServiceId | 4~5초 | TCF 설정 |\n| Proxy | 10초 | Apache / Gateway |\n| WebTop | 15초 | 단말 |\n| L4 Idle | 120초 | L4 Profile |`;
}

function sizingFormulas() {
  return '```\n동시요청자 = 전체사용자 × 동시요청률\nTPS = 동시요청자 ÷ 목표응답시간(3초)\nAP 대수 = ⌈TPS ÷ 250⌉ (A-A 배치)\nThread = AP당TPS × 1.2초 × 1.2\nPool = max(30, min(TPS×0.15×1.3, Thread×30%))\n```';
}

const TITLE_SPECS = [
  { re: /Statement Timeout/i, rec: '| 항목 | 권고 | 비고 |\n|------|------|------|\n| default-statement-timeout | 3초 | application.yml |\n| 일반 조회 SQL | 2~3초 | Mapper timeout |\n| SV 핵심 SQL | 3초 | RDW 고빈도 |\n| 복합 조회 | 5초 이내 | 예외 승인 |', cfg: '```yaml\nmybatis:\n  configuration:\n    default-statement-timeout: 3\n```' },
  { re: /Fetch Size/i, rec: '| 업무 | fetch-size | 비고 |\n|------|------------|------|\n| 일반 조회 | 100~300 | 페이징 권장 |\n| SV 조회 | 300~500 | Fetch 상한 |\n| 대량 조회 | 금지 | ADW 분리 |', cfg: '```yaml\ndefault-fetch-size: 300\n```' },
  { re: /Executor Type/i, rec: '| Executor | 용도 | NSIGHT |\n|----------|------|--------|\n| SIMPLE | 기본 | **표준** |\n| REUSE | Statement 재사용 | 제한 검토 |\n| BATCH | 일괄 INSERT | 배치 전용 |', cfg: '```yaml\ndefault-executor-type: SIMPLE\n```' },
  { re: /Local Cache/i, rec: '| Scope | 권고 | 이유 |\n|-------|------|------|\n| SESSION | 금지(온라인) | 메모리·일관성 |\n| STATEMENT | 기본 | 요청 단위 |', cfg: '```yaml\nlocal-cache-scope: STATEMENT\n```' },
  { re: /Lazy Loading/i, rec: '| 항목 | 권고 |\n|------|------|\n| lazyLoadingEnabled | false |\n| aggressiveLazyLoading | false |', cfg: '```yaml\nlazy-loading-enabled: false\naggressive-lazy-loading: false\n```' },
  { re: /Camel Case/i, rec: '| 항목 | 권고 |\n|------|------|\n| mapUnderscoreToCamelCase | true |', cfg: '```yaml\nmap-underscore-to-camel-case: true\n```' },
  { re: /Batch Executor/i, rec: '| 항목 | 권고 |\n|------|------|\n| batch size | 100~500 |\n| Window | 00:00~06:00 |\n| Pool | 배치 전용 |', cfg: '```yaml\n# 배치 전용 DataSource·Executor 분리\n```' },
  { re: /Paging/i, rec: '| 항목 | 권고 |\n|------|------|\n| 페이지 크기 | 20~100 |\n| 최대 조회 | 1,000건 |\n| OFFSET 금지 | Keyset 페이징 |', cfg: '```sql\n-- Keyset: WHERE id > :lastId ORDER BY id FETCH FIRST 50 ROWS ONLY\n```' },
  { re: /대량 조회/i, rec: '| 금지 | 대안 |\n|------|------|\n| RDW Full Scan | 조건·인덱스 |\n| 무제한 LIST | 페이징 |\n| 분석 SQL | ADW |', cfg: 'RDW 온라인: **Full Scan 금지**. 조회 범위·기간 필수.' },
  { re: /Slow SQL/i, rec: '| 등급 | 기준 | 조치 |\n|------|------|------|\n| 정상 | p95 ≤1s | 유지 |\n| 주의 | p95 1~2s | 튜닝 |\n| 위험 | p95 >2s | 즉시 튜닝·ADW |', cfg: 'APM Slow SQL · SQL_ID · GUID 연계 수집.' },
  { re: /SQL 실행시간 수집/i, rec: '| 수집 | 위치 |\n|------|------|\n| SQL_ID | APM·DB |\n| GUID | MDC·Access Log |\n| p95 | 대시보드 |', cfg: '```yaml\nlogging:\n  level:\n    com.nh.nsight.mapper: INFO\n```' },
  { re: /OS Memory/i, rec: '| 구분 | 8C/32G |\n|------|--------|\n| OS·Cache | 6~8GB |\n| APM/Agent | 2~4GB |\n| 여유 | ≥4GB |', cfg: '32GB VM에서 Heap 24GB+ **금지**.' },
  { re: /JVM Heap/i, rec: '| AP | Heap | VM |\n|----|------|----|\n| 일반 | 12GB | 8C/32G |\n| SV | 14GB | 8C/32G |', cfg: '```shell\n-Xms12g -Xmx12g\n```' },
  { re: /Native Memory/i, rec: '| 영역 | 예상 |\n|------|------|\n| Thread Stack | ~0.5GB |\n| Direct | ~2GB |\n| JNI | 가변 |', cfg: '장애 시 `-XX:NativeMemoryTracking=summary`' },
  { re: /Metaspace/i, rec: '| VM | MaxMetaspaceSize |\n|----|------------------|\n| 8C/32G | 1GB |\n| 16C/64G | 2GB |', cfg: '```shell\n-XX:MaxMetaspaceSize=1g\n```' },
  { re: /Thread Stack/i, rec: '| 항목 | 값 |\n|------|----|\n| -Xss | 512KB |\n| maxThreads | 400~500 |\n| Stack 합계 | ~250MB |', cfg: '```shell\n-Xss512k\n```' },
  { re: /Direct Buffer/i, rec: '| 항목 | 권고 |\n|------|------|\n| NIO Buffer | 모니터링 |\n| OOM | Direct buffer memory |', cfg: 'TLS·압축·Netty 사용량 점검.' },
  { re: /로그 저장|Heap Dump 저장|임시파일|백업 공간/i, rec: '| 경로 | 권고 용량 |\n|------|----------|\n| /logs | 50GB+ |\n| /logs/gc | 10GB+ |\n| /logs/dump | 100GB+ |\n| 업로드 temp | 20GB+ |', cfg: 'AP당 디스크 **50~100GB** 여유. Dump 보관 최근 10개.' },
  { re: /GSLB/i, rec: '| 항목 | 권고 |\n|------|------|\n| TTL | 30초 |\n| Health | 센터 VIP |\n| Failover | 자동 |', cfg: '센터 장애 시 WebTopSuite 재조회 정책.' },
  { re: /Sticky/i, rec: '| 항목 | 권고 |\n|------|------|\n| Persistence | JSESSIONID |\n| Timeout | 70분 |\n| 계산 | Session 60m + 10m |', cfg: 'L4 Sticky > Tomcat Session Timeout.' },
  { re: /Health/i, rec: '| 계층 | URI | Interval |\n|------|-----|----------|\n| L4 | /actuator/health/l4 | 5s |\n| GSLB | 센터 VIP | 10s |', cfg: 'Fail 3회 → Member 제외.' },
  { re: /Graceful/i, rec: '| 항목 | 권고 |\n|------|------|\n| shutdown | graceful |\n| phase | 30s |\n| L4 | Drain 선행 |', cfg: '```yaml\nserver.shutdown: graceful\n```' },
  { re: /JWT|JWKS/i, rec: '| Token | TTL |\n|-------|-----|\n| Access | 15~30분 |\n| Refresh | 8시간 |\n| JWKS Cache | 5~10분 |', cfg: '```yaml\njwt:\n  access-token-validity: 1800s\n```' },
  { re: /Kafka|CDC|Consumer|Lag/i, rec: '| 지표 | Warning | Critical |\n|------|---------|----------|\n| Lag | 업무별 | DLQ |\n| poll interval | ≥처리시간 | — |', cfg: '```yaml\nspring.kafka.consumer.max-poll-interval-ms: 300000\n```' },
  { re: /Batch|Scheduler|Window/i, rec: '| 항목 | 권고 |\n|------|------|\n| Window | 00:00~06:00 |\n| Pool | 배치 전용 |\n| 중복 실행 | 방지 |', cfg: '온라인 Pool·CPU와 **분리**.' },
  { re: /Cache|TTL|Eviction/i, rec: '| 유형 | TTL | 비고 |\n|------|-----|------|\n| Local | 1~5분 | Stampede 방지 |\n| Redis | 5~30분 | fallback |', cfg: '```yaml\ncache:\n  ttl: 300s\n  max-size: 10000\n```' },
  { re: /Multipart|Upload|Download|파일/i, rec: '| 항목 | 권고 |\n|------|------|\n| max-file-size | 업무별 10~50MB |\n| 대용량 | 전용 서버 |', cfg: '```yaml\nspring.servlet.multipart.max-file-size: 10MB\n```' },
  { re: /Access Log|MDC|감사|관측/i, rec: '| 필드 | 필수 |\n|------|------|\n| GUID | ✓ |\n| TraceId | ✓ |\n| UserId | ✓ |', cfg: '```xml\n<Valve className="org.apache.catalina.valves.AccessLogValve"\n  pattern="%h %t \\"%r\\" %s %b %D %{X-GUID}i" />\n```' },
  { re: /ServiceId|TCF|GUID|Idempotency/i, rec: '| 항목 | 권고 |\n|------|------|\n| ServiceId Timeout | 4~5초 |\n| GUID | MDC 필수 |\n| Retry | 기본 금지 |', cfg: 'TCF Slow Transaction p95>3s → APM 알림.' },
  { re: /Circuit|Bulkhead|Rate Limit/i, rec: '| 패턴 | 권고 |\n|------|------|\n| Circuit Breaker | 필수 |\n| Bulkhead | 업무별 |\n| Rate Limit | Gateway |', cfg: '```yaml\nresilience4j.circuitbreaker.failure-rate-threshold: 50\n```' },
  { re: /minSpareThreads/i, rec: '| Core | minSpare |\n|------|----------|\n| 8C | 100 |\n| 16C | 150~200 |', cfg: '피크 진입 전 Thread 생성 지연 완화.' },
  { re: /acceptCount/i, rec: '| Core | acceptCount |\n|------|-------------|\n| 8C | 300~500 |\n| 16C | 500~800 |', cfg: 'Queue 증가는 **병목 신호** — Thread·Pool·SQL 동시 확인.' },
  { re: /keepAliveTimeout|maxKeepAlive/i, rec: '| 항목 | 권고 |\n|------|------|\n| keepAliveTimeout | 120초 |\n| maxKeepAliveRequests | 100~500 |\n| L4 Idle | 120초 |', cfg: 'Apache·Tomcat·L4 **동일 120초** 정합.' },
  { re: /connectionTimeout/i, rec: '| 계층 | 권고 | 의미 |\n|------|------|------|\n| Hikari | 3초 | Pool 획득 |\n| Tomcat | 8초 | 연결 대기 |\n| L4 Connect | 2~3초 | 백엔드 연결 |', cfg: '**SQL 실행시간과 Pool connectionTimeout은 별개**.' },
  { re: /idleTimeout|maxLifetime|keepaliveTime/i, rec: '| 항목 | 권고 |\n|------|------|\n| idleTimeout | 10분 |\n| maxLifetime | 30분 |\n| keepaliveTime | 2~5분 |', cfg: 'DB·L4 Idle보다 **짧게**.' },
  { re: /autoCommit/i, rec: '| 항목 | 권고 |\n|------|------|\n| autoCommit | false |\n| Tx | @Transactional |', cfg: '```yaml\nauto-commit: false\n```' },
  { re: /leakDetection/i, rec: '| 환경 | threshold |\n|------|----------|\n| DEV/STG | 60s |\n| PRD | 선택(오버헤드) |', cfg: 'Connection 미반환 조기 탐지.' },
  { re: /DB Session|max sessions/i, rec: '| 구분 | 산정 |\n|------|------|\n| 온라인 | Σ(AP×Pool) |\n| 배치·BI | 별도 |\n| 여유 | 20% |', cfg: 'DBA max sessions **상한** 준수.' },
  { re: /센터 장애|잔여|N\+/i, rec: '| TPS | AP 권장 | 잔여(1센터 Down) |\n|-----|----------|------------------|\n| 720 | 8대 | 4대×250=1000 |', cfg: '잔여 TPS ≥ **720** 필수.' },
  { re: /가정값|권고값|확정값|분류/i, rec: '| 구분 | 의미 | 예 |\n|------|------|----|\n| 가정값 | 산정 전제 | 동시요청률 5% |\n| 권고값 | 표준 | maxThreads 500 |\n| 확정값 | 시험 실측 | Pool 50 |\n| 임계치 | APM | CPU 70/85% |', cfg: '혼용 금지 — 변경 시 문서·설정 동기화.' },
  { re: /금지|안티|잘못/i, rec: '| 금지 | 이유 |\n|------|------|\n| 사용자수=TPS | 과대 산정 |\n| 세션=Thread | 개념 혼동 |\n| Pool만 증가 | DB Session 폭증 |\n| Heap=RAM 50% | OS OOM |', cfg: '46장 전체 안티패턴 참조.' },
];

function matchSpec(title) {
  for (const s of TITLE_SPECS) {
    if (s.re.test(title)) return s;
  }
  return null;
}

function configByChapter(ch, title) {
  const spec = matchSpec(title);
  if (spec?.cfg) return spec.cfg;
  const t = title;
  const ctx = CHAPTER_CTX[ch] || { domain: 'NSIGHT', files: 'application.yml', config: '표준' };

  if (ch === 22 || /Thread|Connector|acceptCount|maxConnections/.test(t)) {
    return '```xml\n<Connector port="8080" protocol="org.apache.coyote.http11.Http11NioProtocol"\n  maxThreads="500" minSpareThreads="100" acceptCount="500"\n  maxConnections="10000" connectionTimeout="8000"\n  keepAliveTimeout="120000" maxKeepAliveRequests="200" />\n```';
  }
  if (ch === 23 || /Heap|JVM|GC|Xms|Xmx|Metaspace/.test(t)) {
    return '```shell\n-Xms12g -Xmx12g -Xss512k\n-XX:+UseG1GC -XX:MaxGCPauseMillis=200\n-XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/logs/dump\n-XX:+ExitOnOutOfMemoryError\n-Xlog:gc*,safepoint:file=/logs/gc/ap-gc-%t.log:time,uptime,level,tags:filecount=10,filesize=100M\n-Dfile.encoding=UTF-8 -Duser.timezone=Asia/Seoul\n```';
  }
  if (ch === 26 || /Pool|Hikari|maximumPoolSize/.test(t)) {
    return '```yaml\nspring.datasource.hikari:\n  pool-name: marketing-pool\n  maximum-pool-size: 50\n  minimum-idle: 15\n  connection-timeout: 3000\n  idle-timeout: 600000\n  max-lifetime: 1800000\n  auto-commit: false\n```';
  }
  if (ch === 27 && /MyBatis|SQL|Statement|Fetch|Slow|Paging/.test(t)) {
    return '```yaml\nmybatis:\n  configuration:\n    default-statement-timeout: 3\n    default-fetch-size: 300\n```';
  }
  if (ch === 24 || /Spring|Transaction|Graceful|Actuator|Profile/.test(t)) {
    return '```yaml\nserver:\n  shutdown: graceful\nspring:\n  lifecycle:\n    timeout-per-shutdown-phase: 30s\nmanagement:\n  endpoints:\n    web:\n      exposure:\n        include: health,info,metrics,prometheus\n```';
  }
  if (ch === 16 || /Timeout|타임아웃/.test(t)) return timeoutTable();
  if (ch >= 8 && ch <= 11 || /산정|TPS|VM|TPMC|사용자|세션/.test(t)) return sizingFormulas();
  if (ch === 20 || /Apache|Proxy|Worker/.test(t)) {
    return '```apache\nProxyTimeout 10\nProxyConnectTimeout 3\nKeepAlive On\nKeepAliveTimeout 120\nRequestHeader set X-GUID %{GUID}e\n```';
  }
  if (ch === 19 || /L4|Sticky|VIP|Health/.test(t)) {
    return '| L4 항목 | 권고 |\n|--------|------|\n| LB Method | Round Robin |\n| Sticky | JSESSIONID, 70분 |\n| Health | GET /actuator/health, 5s/2s/Fail3 |\n| Idle Timeout | 120초 |\n| Connect Timeout | 2~3초 |';
  }
  if (ch === 29 || /세션|Session|Cookie/.test(t)) {
    return '```yaml\nserver:\n  servlet:\n    session:\n      timeout: 60m\n      cookie:\n        secure: true\n        http-only: true\n```';
  }
  if (ch === 32 || /HTTP Client|CruzAPIM|연계/.test(t)) {
    return '```yaml\nhttp:\n  client:\n    connect-timeout: 3s\n    read-timeout: 5s\nresilience4j:\n  circuitbreaker:\n    failure-rate-threshold: 50\n```';
  }
  return `**설정 파일**: \`${ctx.files}\` · **핵심 항목**: ${ctx.config}`;
}

function purposeParagraph(ch, title) {
  const ctx = CHAPTER_CTX[ch] || { part: '가이드', domain: 'NSIGHT 표준' };
  return `본 절(**${title}**)은 ${ctx.part} 영역에서 **${ctx.domain}** 관점의 NSIGHT 1차 표준을 정의합니다. `
    + `피크 **${STD.tps.peak} TPS**·p95 **${STD.p95}**·${STD.vm} 기준으로 권고값을 제시하며, 최종값은 선도개발·성능시험(360/720/1080) 실측으로 확정합니다.`;
}

function cautionBlock(ch, title) {
  const lines = [
    '- **피크 기준** 설계 — 평균 TPS만으로 설정 확정 금지',
    '- **세션 ≠ TPS** — 세션 증가는 Heap·복제 부담, TPS와 별도 산정',
    '- **Thread ≠ Pool** — Pool ≤ Thread×30~40%, DB Session 총량 검증',
    '- **안쪽 Timeout 짧게** — SQL(3s) < Tx(5s) < Proxy(10s) < Web(15s)',
  ];
  if (/금지|잘못|오류|위험/.test(title)) {
    lines.push('- **금지**: 사용자수=TPS, Heap=물리50%, Pool 무단 증가, Retry 무제한');
  }
  if (ch >= 46) lines.push('- 운영 반영 전 **ENV rule-check** 및 성능시험 합격 필수');
  return lines.join('\n');
}

function recommendTable(ch, title) {
  const spec = matchSpec(title);
  if (spec?.rec) return spec.rec;
  const t = title;
  // 챕터 전체 기본값은 제목 매칭 실패 시에만 (절별 차별화)
  if (/Thread|Connector/.test(t) || (title === 'maxThreads' || title === 'Tomcat Connector')) {
    return `| 항목 | 8C/32G 권고 | 산식/근거 |\n|------|-------------|----------|\n| maxThreads | 400~500 | 250×1.2×1.2≈360 |\n| minSpareThreads | 100 | 피크 진입 지연 완화 |\n| acceptCount | 300~500 | Queue 병목 은폐 금지 |\n| maxConnections | 10,000 | L4 KeepAlive 정합 |`;
  }
  if (/Pool|Hikari/.test(t)) {
    return `| 항목 | 일반 AP | SV AP | 산식 |\n|------|---------|-------|------|\n| maximumPoolSize | 50 | 60 | TPS×0.15×1.3 |\n| minimumIdle | 15 | 15 | max의 20~30% |\n| connectionTimeout | 3초 | 3초 | ≠ SQL timeout |\n| maxLifetime | 30분 | 30분 | DB Idle보다 짧게 |`;
  }
  if (/Heap|JVM|GC/.test(t) || /Memory|Stack|Metaspace|Native|Direct/.test(t)) {
    return `| 항목 | 일반 AP | SV AP | VM |\n|------|---------|-------|----|\n| Heap (Xms=Xmx) | 12GB | 14GB | 8C/32G |\n| -Xss | 512KB | 512KB | 500 Thread 기준 |\n| GC | G1GC | G1GC | Pause 목표 200ms |\n| Metaspace Max | 1GB | 1GB | 8C/32G |`;
  }
  if (/Timeout|타임아웃/.test(t)) return timeoutTable();
  if (/임계|모니터|지표/.test(t)) {
    return `| 지표 | Warning | Critical | 조치 |\n|------|---------|----------|------|\n| CPU | 70% | 85% | Scale-Out·SQL |\n| Heap | 70% | 85% | Dump·GC 분석 |\n| Hikari Active | 70% | 90% | Pool·SQL |\n| Hikari Pending | >0 1분 | >0 5분 | 즉시 분석 |\n| GC Pause p95 | 200ms | 500ms | Heap·객체 |\n| p95 응답 | 2.5초 | 3초 | E2E Trace |`;
  }
  if (/산정|TPS|사용자|VM|TPMC|동시/.test(t)) {
    return `| 시나리오 | 동시요청률 | 동시요청자 | TPS |\n|----------|-----------|-----------|-----|\n| 기본 | 5% | 1,080 | ${STD.tps.base} |\n| 피크 | 10% | 2,160 | ${STD.tps.peak} |\n| 스트레스 | 15% | 3,240 | ${STD.tps.stress} |\n\n| VM | VM당 TPS | 피크 AP 권장 |\n|----|----------|-------------|\n| 8C/32G | 250 | 8대(A-A) |`;
  }
  if (/RACI|책임/.test(t)) {
    return `| 역할 | 책임 | 산출물 |\n|------|------|--------|\n| 아키텍처 | 산정 모델·표준 | 산정표 |\n| 인프라 | VM·OS·L4 | 인프라 산정 |\n| WAS | Tomcat·JVM | server.xml·setenv.sh |\n| 프레임워크 | Spring·TCF·Pool | application.yml |\n| DBA | max sessions | DB 파라미터 |\n| 성능시험 | 360/720/1080 | 검증결과서 |\n| 운영 | 모니터링·Runbook | Baseline |`;
  }
  if (/프로파일|8Core|16Core|32Core|64GB|128GB|256GB/.test(t)) {
    const prof = ch === 38 ? '16C/64G' : ch === 39 ? '16C/128G' : ch === 40 ? '32C/256G' : '8C/32G';
    return `| 프로파일 | ${prof} | 용도 |\n|----------|--------|------|\n| TPS/VM | ${ch === 40 ? '배치' : ch >= 38 ? '~500' : '250'} | ${ch >= 38 ? '특수·배치' : '온라인 표준'} |\n| maxThreads | ${ch === 40 ? '800+' : ch >= 38 ? '600~800' : '400~500'} | Tomcat |\n| Heap | ${ch === 40 ? '32GB+' : ch >= 38 ? '24~32GB' : '12~14GB'} | JVM |\n| Pool | ${ch === 40 ? '업무별' : ch >= 38 ? '60~80' : '50/60'} | Hikari |`;
  }
  // 챕터 도메인 폴백
  const ctx = CHAPTER_CTX[ch];
  if (ctx) {
    return `| 항목 | ${ctx.domain} 권고 |\n|------|----------------|\n| 기준 VM | ${STD.vm} |\n| TPS | ${STD.tps.base}/${STD.tps.peak}/${STD.tps.stress} |\n| 설정 파일 | ${ctx.files} |\n| 핵심 | ${ctx.config} |`;
  }
  return stdPremiseTable();
}

function titleSpecificNotes(ch, title) {
  const t = title;
  const notes = [];
  if (/검증|시험|Gate|체크/.test(t)) notes.push('검증 도구: APM, `jcmd`, `jstat`, Hikari Metrics, Access Log(GUID), ENV rule-check.');
  if (/예시|템플릿|적용/.test(t)) notes.push('DEV→STG→PRD 동일 항목·환경별 수치만 변경. PRD 확정값은 변경관리(53장) 준수.');
  if (/장애|복구|고갈|포화/.test(t)) notes.push('장애 분석: GUID E2E Trace → Thread Dump → Hikari Pending → SQL p95 → GC Log 순.');
  if (/변경|Rollback|Review|폐기/.test(t)) notes.push('변경 흐름: 요청→영향도→승인→STG 검증→PRD 반영→이력(부록 AB).');
  if (/금지/.test(t)) notes.push('대표 금지: 세션=Thread, maxThreads=Pool, Heap=RAM 50%, 동일 Timeout 전 계층, 시험 생략 운영 반영.');
  if (ch === 51) notes.push('API: `/api/oc/env/rule-check` — Timeout 순서·Pool 합산·Thread/Pool 비율 자동 검사.');
  if (ch === 7) notes.push('CAP/ENV 연동: `/oc/capacity.html`, `/oc/env-002.html`.');
  if (notes.length === 0) {
    const ctx = CHAPTER_CTX[ch];
    if (ctx) notes.push(`관련 원문: \`znsight-capacity-word\` · \`환경셋팅(최종).docx\` · 설정 위치: ${ctx.files}`);
  }
  return notes.join('\n\n');
}

/**
 * 23장 수준 절 본문 생성 (목표 25~40줄)
 */
function buildRichSection(chapterNum, sectionKey, sectionTitle) {
  const parts = [];
  parts.push(purposeParagraph(chapterNum, sectionTitle));
  parts.push('#### NSIGHT 권고값\n\n' + recommendTable(chapterNum, sectionTitle));
  parts.push('#### 설정 예시\n\n' + configByChapter(chapterNum, sectionTitle));
  parts.push('#### 검증 기준\n\n' + verifyTable());
  parts.push('#### 주의사항\n\n' + cautionBlock(chapterNum, sectionTitle));
  const notes = titleSpecificNotes(chapterNum, sectionTitle);
  if (notes) parts.push('#### 운영 참고\n\n' + notes);
  return parts.join('\n\n') + '\n';
}

function thinChapterExtra(chapterNum) {
  if (![55, 56, 57].includes(chapterNum)) return '';
  const appendixIndex = `| 부록 | 용도 | 연관 |
|------|------|------|
| A | 산정 입력 | CAP-010 |
| B | TPS 계산 | 8~9 |
| M | Timeout | 16 |
| V | DB Session | 26~28 |
| W | 센터 장애 | 11 |
| X·Z | 시험 | 47~48 |
| AA·AB | 전환·변경 | 52~53 |`;

  const verifyScenarios = `| 시나리오 | TPS | 핵심 지표 | 합격 |
|----------|-----|----------|------|
| 기본 운영 | 360 | p95, CPU | p95≤3s |
| 피크 | 720 | Thread, Pool | Pending=0 |
| 스트레스 | 1,080 | Fail-fast | CB 동작 |
| AP Down | 720@잔여 | L4 failover | 30s 이내 |
| 센터 Down | 720@단일 | GSLB+잔여 AP | TPS≥720 |`;

  const capChain = `| 단계 | CAP | 산출 | 설정 |
|------|-----|------|------|
| 010 | 조건 | 사용자·시나리오 | branchCount |
| 020 | TPS | 360/720/1080 | — |
| 030 | AP | 8대 | vmSpec |
| 040 | WAS | Thread | maxThreads |
| 050 | Pool | 50/VM | HikariCP |`;

  if (chapterNum === 55) {
    return [
      '### 버전 업그레이드 영향 범위',
      '',
      '| 변경 | 산정 영향 | 설정 영향 | 시험 |',
      '|------|----------|----------|------|',
      '| JDK major | GC·TLS | setenv.sh | 360/720 |',
      '| Tomcat major | Connector | server.xml | 720 |',
      '| Spring major | Jakarta | application.yml | rule-check |',
      '| Hikari minor | metrics명 | yml | Pool 시험 |',
      '',
      '### Timeout 계층 재검증',
      '',
      '| 계층 | 권고 |',
      '|------|------|',
      '| SQL | 3초 |',
      '| Transaction | 5초 |',
      '| Proxy | 10초 |',
      '',
      '### 부록·본문 참조',
      '',
      appendixIndex,
      '',
      '### 검증 시나리오 요약',
      '',
      verifyScenarios,
      '',
    ].join('\n');
  }
  if (chapterNum === 56) {
    return [
      '### E2E 산정→설정 체인 (요약)',
      '',
      capChain,
      '',
      '### 검증 시나리오',
      '',
      verifyScenarios,
      '',
      '### 부록 참조',
      '',
      appendixIndex,
      '',
      '### 용어 정리',
      '',
      '| 용어 | 정의 | 주의 |',
      '|------|------|------|',
      '| 세션 | 로그인 유지 규모 | ≠ TPS |',
      '| TPS | 동시 요청 처리량 | 피크 720 기준 |',
      '| TPMC | CPU 부하 단위 | 정보계 1500~5000 |',
      '| Pool | DB Connection 풀 | ≠ Thread |',
      '| 잔여 TPS | 장애 후 처리량 | A-A 필수 검증 |',
      '',
      '### Timeout 계층 (복습)',
      '',
      '| SQL 3s | Tx 5s | Proxy 10s | Web 15s | L4 120s |',
      '',
      '### 관련 본문 챕터',
      '',
      '| 챕터 | 주제 |',
      '|------|------|',
      '| 7 | 산정→설정 연결 |',
      '| 11 | VM·잔여 TPS |',
      '| 47~48 | 성능시험 |',
      '| 54 | Capacity Review |',
      '',
      '> **핵심**: 세션≠TPS, Pool≠Thread, 잔여 TPS≥720.',
      '',
    ].join('\n');
  }
  return [
    '### NSIGHT 용량산정 13단계 (복습)',
    '',
    '| # | 단계 | 산출 | 설정 |',
    '|---|------|------|------|',
    '| 1 | 사용자 | 21,600 | — |',
    '| 2 | 세션 | 26K~28K | Session 60m |',
    '| 3 | 동시요청 | 1,080~3,240 | rate |',
    '| 4 | TPS | 360/720/1080 | — |',
    '| 5 | AP | 8대 | 8C/32G |',
    '| 6 | Thread | 400~500 | maxThreads |',
    '| 7 | Heap | 12~14GB | setenv.sh |',
    '| 8 | Pool | 50/60 | Hikari |',
    '| 9 | Timeout | 계층 | 16장 |',
    '| 10 | DB Session | ΣPool | max sessions |',
    '| 11 | 잔여 TPS | ≥720 | W부록 |',
    '| 12 | 시험 | 합격 | X·Z |',
    '| 13 | 운영 | Baseline | 54장 |',
    '',
    '### Timeout 계층 (복습)',
    '',
    '| 계층 | 권고 |',
    '|------|------|',
    '| SQL | 3초 |',
    '| Pool | 3초 |',
    '| Transaction | 5초 |',
    '| Proxy | 10초 |',
    '| WebTop | 15초 |',
    '| L4 Idle | 120초 |',
    '',
    '### 관련 본문 챕터',
    '',
    '| 챕터 | 주제 |',
    '|------|------|',
    '| 1 | 도입·체인 |',
    '| 7 | 연결 모델 |',
    '| 45 | 720 TPS 예시 |',
    '| 52 | Go-Live |',
    '',
    '> **마무리**: 권고값은 출발점 — 360/720/1080 실측·Baseline 갱신이 최종 목표입니다.',
    '',
    '### CAP 단계별 요약',
    '',
    capChain,
    '',
    '### 검증 시나리오',
    '',
    verifyScenarios,
    '',
    '### 부록 참조',
    '',
    appendixIndex,
    '',
  ].join('\n');
}

function buildChapterSupplement(chapterNum, chapterTitle) {
  const ctx = CHAPTER_CTX[chapterNum] || { part: '가이드', domain: 'NSIGHT' };
  const lines = [
    `## ${chapterTitle} — 실무 요약`,
    '',
    `본 장은 **${ctx.part}**(${ctx.domain})의 핵심을 23장(JVM)과 동일한 깊이로 요약합니다.`,
    '',
    '### E2E 용량산정 체인',
    '',
    '```',
    '[1] 사용자 21,600 → [2] 동시요청자 1,080~3,240',
    '→ [3] TPS 360/720/1080 → [4] AP 8대 권장',
    '→ [5] Thread 400~500 → [6] Pool 50/60',
    '→ [7] DB Session Σ(AP×Pool) → [8] 장애 잔여 TPS≥720',
    '```',
    '',
    '### NSIGHT 1차 표준 요약',
    '',
    stdPremiseTable(),
    '',
    '### Timeout 계층',
    '',
    timeoutTable(),
    '',
    '### 산정 공식',
    '',
    sizingFormulas(),
    '',
    '### 검증 시나리오',
    '',
    verifyTable(),
    '',
    '### 연관 챕터',
    '',
    '| 영역 | 챕터 |',
    '|------|------|',
    '| 산정 | 8~11, 45 |',
    '| 연결 | 7 |',
    '| 설정 | 16~36, 37 |',
    '| 검증 | 47~48, 50 |',
    '| 운영 | 49~54 |',
    '',
    '### CAP/ENV 연동',
    '',
    '- 용량산정: `/oc/capacity.html` · `/api/oc/capacity/*`',
    '- 환경설정: `/oc/env-002.html` · `/api/oc/env/*` · rule-check',
    '',
  ];

  const capEnvBlock = [
    '### CAP/ENV 필드 매핑 (부록 A)',
    '',
    '#### CAP-010',
    '',
    buildCap010MappingTable(),
    '',
    '#### ENV-002',
    '',
    buildEnv002MappingTable(),
    '',
  ];

  if ([1, 3, 7, 8, 45].includes(chapterNum)) {
    lines.push(...capEnvBlock);
  }

  if (chapterNum === 1) {
    lines.push(
      '### 13단계 산정 흐름 상세', '',
      '| 단계 | 산출 | 설정 연결 |',
      '|------|------|----------|',
      '| 1 사용자 | 21,600명 | — |',
      '| 2 세션 | 26K~28K | Session Idle 60m |',
      '| 3 동시요청 | 1,080~3,240 | — |',
      '| 4 TPS | 360/720/1080 | — |',
      '| 5 AP | 8대 | VM 8C/32G |',
      '| 6 Thread | 400~500 | maxThreads |',
      '| 7 Heap | 12~14GB | setenv.sh |',
      '| 8 Pool | 50/60 | HikariCP |',
      '| 9 Timeout | 계층 | 16장 |',
      '| 10 DB Session | ΣPool | max sessions |',
      '| 11 잔여 TPS | ≥720 | W 부록 |',
      '| 12 시험 | 360/720/1080 | X·Z 부록 |',
      '| 13 운영 | Baseline | 54장 |',
      '',
      '### 도입 체크리스트', '', '| # | 확인 |', '|---|------|',
      '| 1 | 21,600명·세션 26K 합의 |', '| 2 | 360/720/1080 TPS 시나리오 |',
      '| 3 | 산정→설정 체인 이해 |', '| 4 | 가정/권고/확정값 구분 |',
      '| 5 | 성능시험 계획(47장) 예정 |', '',
      '### 자주 묻는 질문', '',
      '**Q. 세션 28,000이면 TPS도 높아지나?** — 아니오. 세션≠TPS.',
      '**Q. AP 4대면 충분하지 않나?** — 피크 720 TPS + 센터 장애 시 8대 권장.',
      '**Q. Pool 100으로 늘리면?** — DB Session 폭증. SQL·산식 먼저.',
      '',
      '### 문서 읽기 순서',
      '',
      '| 순서 | 챕터 | 목적 |',
      '|------|------|------|',
      '| 1 | 1~3 | 문제·배경 이해 |',
      '| 2 | 5~11 | 산정 모델 |',
      '| 3 | 7·45 | 산정→설정 연결 |',
      '| 4 | 16~36 | 솔루션 설정 |',
      '| 5 | 37~40 | VM 프로파일 |',
      '| 6 | 47~48 | 성능시험 |',
      '| 7 | 49~54 | 운영·변경 |',
      '',
      '### 역할별 시작점',
      '',
      '| 역할 | 우선 챕터 | 화면 |',
      '|------|----------|------|',
      '| PMO·업무 | 8, 5 | CAP branchCount |',
      '| 아키텍처 | 7, 11, 42 | CAP·ENV 전체 |',
      '| WAS | 22~23 | ENV-004 |',
      '| DBA | 26~28 | CAP-050 |',
      '| 성능시험 | 47~48 | X·Z 부록 |',
      '');
  }
  if (chapterNum === 3) {
    lines.push(
      '### 병목 유형별 대응',
      '',
      '| 유형 | 증상 | 1차 조치 | 관련 장 |',
      '|------|------|----------|--------|',
      '| Thread 과소 | acceptCount↑ | maxThreads 산정 | 22 |',
      '| Pool 과다 | DB Session↑ | SQL·hold 검토 | 26 |',
      '| Pool 과소 | Hikari Pending | SQL p95 먼저 | 26·27 |',
      '| Timeout 역전 | 원인 불명 | rule-check | 16·51 |',
      '| 세션 비대 | Full GC | 세션 객체 금지 | 23·29 |',
      '| 센터 장애 | TPS 급감 | 잔여 TPS≥720 | 11·W부록 |',
      '',
      '### As-Is vs To-Be 점검표',
      '',
      '| 항목 | As-Is 위험 | To-Be 권고 |',
      '|------|-----------|-----------|',
      '| 사용자→TPS | 직접 환산 | 동시요청률 분리 |',
      '| AP 대수 | CPU만 | 250 TPS/VM + A-A |',
      '| maxThreads | 기본값 | 400~500 (산정 연계) |',
      '| Pool | Thread 동일 | 별도 4단계 산식 |',
      '| Timeout | 동일값 | 계층 분리 |',
      '',
      '### 연쇄 장애 시뮬레이션',
      '',
      '```',
      'Slow SQL → Pool Pending → Thread 점유 → acceptCount',
      '→ Proxy Queue → WebTop Timeout → 사용자 체감 장애',
      '```',
      '',
      'GUID 없이 단일 계층만 튜닝하면 **다른 계층에서 재발**합니다.',
      '',
      '### 재산정 트리거',
      '',
      '| 이벤트 | 재산정 항목 |',
      '|--------|------------|',
      '| 지점·인원 변경 | 사용자·세션·TPS |',
      '| 캠페인 | 동시요청률·피크 TPS |',
      '| 신규 채널 | TPMC·Core TPS |',
      '| DR 전환 | 잔여 AP·잔여 TPS |',
      '| VM 스펙 변경 | vmTps·AP 대수 |',
      '');
  }
  if (chapterNum === 7) {
    lines.push(
      '### 4계층 연결 모델',
      '',
      '| 계층 | 예시 | 산출/설정 |',
      '|------|------|----------|',
      '| 입력 | 3,600×6, 10% | branchCount·rate |',
      '| 계산 | 720 TPS, 8 AP | CAP-020~030 |',
      '| 설정 | maxThreads 500, Pool 50 | server.xml·yml |',
      '| 임계치 | CPU 70/85% | APM·Actuator |',
      '',
      '### 입력→설정 변환 예 (720 TPS)',
      '',
      '| 계산값 | 값 | 설정 매핑 |',
      '|--------|-----|----------|',
      '| 동시요청자 | 2,160 | CAP rate 10% |',
      '| TPS | 720 | CAP-020 |',
      '| AP | 8 (A-A) | CAP-030·vmSpec |',
      '| Thread | ~360 | maxThreads 400~500 |',
      '| Pool/VM | 50 | Hikari maximumPoolSize |',
      '| DB Session | 400 | validateDbPool |',
      '',
      '### CAP/ENV 워크플로',
      '',
      '1. **부록 A** 또는 CAP-010에 입력값 작성',
      '2. `/oc/capacity.html` 전체 산정 → CAP-020~050',
      '3. `/oc/env-002.html` 동일 조건으로 ENV Grid 생성',
      '4. `/api/oc/env/rule-check` Timeout·Pool 정합성',
      '5. 성능시험 후 **확정값**으로 PRD 반영',
      '',
      ...capEnvBlock,
      '');
  }
  if (chapterNum === 18) {
    lines.push(
      '### GSLB 권고값 요약',
      '',
      '| 항목 | 권고 | 검증 |',
      '|------|------|------|',
      '| DNS TTL | 30초 | 장애 전환 ≤60s |',
      '| Health Check | HTTPS /health/l4 | VIP 제외 확인 |',
      '| LB Method | Round Robin (A-A) | 센터별 유입 50:50 |',
      '| Failure Policy | available VIP only | 강제 장애 테스트 |',
      '',
      '### 센터 장애 시나리오',
      '',
      '| 단계 | 동작 | 용량 관점 |',
      '|------|------|----------|',
      '| 1 | DC1 Health Fail | GSLB가 DC2로 라우팅 |',
      '| 2 | TTL 만료 | 클라이언트 재조회 |',
      '| 3 | DC2 AP | 잔여 TPS ≥ 720 |',
      '| 4 | Sticky | 세션 재로그인 허용 |',
      '',
      '### GSLB ↔ L4 ↔ AP 정합',
      '',
      '- GSLB TTL(30s) < L4 Sticky(70m) — 센터 전환과 세션 정책 분리 이해',
      '- Health URL은 Tomcat Readiness와 동일 엔드포인트 권장',
      '- Wide IP 장애 시 **잔여 센터 AP·Pool·DB Session** 사전 검증',
      '',
      '### 설정 예시',
      '',
      '```',
      'Wide IP: mkt.nh.local',
      'Pool: DC1_L4_VIP, DC2_L4_VIP',
      'Health: GET /actuator/health/l4 interval=5s timeout=2s',
      'TTL: 30',
      '```',
      '',
      '### 검증 체크리스트',
      '',
      '| # | 확인 |',
      '|---|------|',
      '| 1 | DC1 강제 Down 시 DNS가 DC2만 응답 |',
      '| 2 | 전환 시간 ≤ 60초 (TTL+Health) |',
      '| 3 | 잔여 AP로 720 TPS 합격 |',
      '| 4 | Sticky 영향·재로그인 UX 합의 |',
      '',
      '### 장애 전환 타임라인',
      '',
      '| 시각 | 이벤트 | 확인 |',
      '|------|--------|------|',
      '| T+0 | DC1 Health Fail | GSLB Pool 제외 |',
      '| T+30s | DNS TTL 만료 | nslookup 재확인 |',
      '| T+60s | DC2 트래픽 100% | APM TPS·p95 |',
      '| T+5m | 잔여 TPS 안정 | ≥720 합격 |',
      '');
  }
  if (chapterNum === 42) {
    lines.push(
      '### To-Be 아키텍처 원칙',
      '',
      '| 원칙 | 내용 | 용량 영향 |',
      '|------|------|----------|',
      '| Scale-Out | 8C/32G VM 다수 | 장애 격리 12.5% |',
      '| A-A | 2센터 AP | 센터당 AP×2 |',
      '| 업무 분리 | SV·연계·배치 VM 분리 | Pool·Timeout 독립 |',
      '| RDW/ADW 분리 | 분석 SQL ADW | RDW TPS 보호 |',
      '| Fail-fast | 안쪽 Timeout 짧게 | Thread 회수 |',
      '',
      '### 논리 구성도',
      '',
      '```',
      'WebTop → GSLB → L4 → Apache → Tomcat×N',
      '  → Spring Boot → Hikari → RDW',
      '  → CruzAPIM → 외부',
      'Kafka/CDC → ADW (온라인 SLA 분리)',
      '```',
      '',
      '### 센터·VM 배치 (720 TPS)',
      '',
      '| 센터 | AP | VM | Pool/VM | DB Session |',
      '|------|-----|-----|---------|------------|',
      '| DC1 | 4 | 8C/32G×4 | 50 | 200 |',
      '| DC2 | 4 | 8C/32G×4 | 50 | 200 |',
      '| 합계 | 8 | — | — | 400 + 여유 |',
      '',
      '### ADR 연계',
      '',
      '아키텍처 결정(8C Scale-Out, A-A, Pool 산식)은 **부록 AC**에 기록하고 산정표·설정 파일 버전과 연결합니다.',
      '');
  }
  if (chapterNum === 55) {
    lines.push(
      '### 버전 호환 매트릭스',
      '',
      '| 구성요소 | 기준 버전 | 업그레이드 시 점검 |',
      '|----------|----------|------------------|',
      '| JDK | 21 | GC Log, ZGC, TLS |',
      '| Tomcat | 10.1.x | Connector, Session |',
      '| Spring Boot | 3.x | Jakarta, Actuator |',
      '| HikariCP | 5.x | 속성명·metrics |',
      '| MyBatis | 3.5.x | timeout 단위(초) |',
      '',
      '### Deprecated 대표 항목',
      '',
      '| 구 설정 | 신 설정 | 비고 |',
      '|---------|---------|------|',
      '| maxActive | maximumPoolSize | Hikari |',
      '| Parallel GC | G1GC | 온라인 AP |',
      '| log4j 1.x | Logback | 보안 |',
      '| 세션=Thread | 별도 산정 | 용량 |',
      '',
      '### 업그레이드 절차',
      '',
      '1. 영향도 분석(산정·설정·시험)',
      '2. STG 360/720 TPS 재시험',
      '3. rule-check·Pool 합산 재검증',
      '4. PRD Rolling + Rollback Runbook',
      '5. AB 부록 변경 이력',
      '',
      '### 폐기·정리 대상',
      '',
      '| 대상 | 조치 |',
      '|------|------|',
      '| 미사용 DataSource | yml 제거 |',
      '| 구 JVM 옵션 | setenv.sh 정리 |',
      '| L4 미사용 Pool Member | 인프라 제거 |',
      '| 주석 처리 설정 | 문서화 후 삭제 |',
      '',
      '### 호환성 검증 체크리스트',
      '',
      '| # | 항목 | STG | PRD |',
      '|---|------|-----|-----|',
      '| 1 | 360 TPS 합격 | □ | □ |',
      '| 2 | 720 TPS 합격 | □ | □ |',
      '| 3 | rule-check PASS | □ | □ |',
      '| 4 | Pool 합산 ≤ DB max | □ | □ |',
      '| 5 | 변경 이력(AB) | □ | □ |',
      '',
      thinChapterExtra(55),
      '');
  }
  if (chapterNum === 56) {
    lines.push(
      '### 핵심 시사점 5가지', '', '1. **세션 ≠ TPS**', '2. **잔여 TPS > 평상시**', '3. **Pool ≠ Thread**', '4. **안쪽 Timeout 짧게**', '5. **실측으로 확정**', '',
      '### 우선 보완 로드맵', '', '| 주차 | 과제 | 담당 |', '|------|------|------|', '| 1~2 | 산정표·설정 정합성 | 아키텍처 |', '| 3~4 | 360/720/1080 시험 | 성능시험 |', '| 5~6 | ENV rule-check | 프레임워크 |', '| 7~8 | Capacity Review | 운영 |', '',
      '### 조직별 핵심 메시지', '',
      '| 역할 | 메시지 |',
      '|------|--------|',
      '| PMO | 동시요청률·피크 TPS 합의가 전제 |',
      '| 아키텍처 | 산정→설정 체인 문서화 |',
      '| WAS | maxThreads·Heap는 산정 결과 |',
      '| DBA | Pool 합산 ≤ max sessions |',
      '| 운영 | Baseline·Review 주기 준수 |',
      '',
      '### 금지 패턴 요약',
      '',
      '- 사용자 수 = TPS',
      '- Pool만 키워서 TPS 해결',
      '- 시험 없이 PRD 반영',
      '- Timeout 전 계층 동일값',
      '- 센터 장애 미검증',
      '',
      '### 성공 지표 (KPI)',
      '',
      '| KPI | 목표 | 측정 |',
      '|-----|------|------|',
      '| p95 | ≤3s @720 TPS | APM |',
      '| 잔여 TPS | ≥720 | 센터 장애 시험 |',
      '| Hikari Pending | 0 | Actuator |',
      '| DB Session | ≤max×80% | DBA |',
      '| rule-check | PASS | ENV API |',
      '| 변경 이력 | 100% | AB 부록 |',
      '',
      thinChapterExtra(56),
      '');
  }
  if (chapterNum === 57) {
    lines.push(
      '### 마무리 체크리스트', '', '| # | 항목 |', '|---|------|',
      '| 1 | 피크·장애 시나리오 검증 |', '| 2 | 확정값 문서화 |',
      '| 3 | 운영 임계치 Baseline |', '| 4 | 변경관리 운영 |',
      '| 5 | Capacity Review 주기 |', '',
      '### 지속 운영 원칙', '',
      '권고값은 출발점. 업무량 변경 시 **동일 산식** 재산정, 실측으로 Baseline 갱신.', '',
      '### 문서 체인 보존', '',
      '| 문서 | 보존 항목 |',
      '|------|----------|',
      '| 산정표 | 가정값·산식·결과 |',
      '| 설정 파일 | server.xml·yml·setenv.sh 버전 |',
      '| 시험결과서 | Z 부록·360/720/1080 |',
      '| 변경이력 | AB 부록 |',
      '| ADR | AC 부록 |',
      '',
      '### 최종 메시지', '',
      'NSIGHT 용량산정의 목적은 **서버 대수 산출이 아니라 운영 가능한 안정성**입니다.',
      '피크 720 TPS, 센터 장애, Slow SQL, 연계 지연 — 모든 시나리오에서 Fail-fast와 잔여 처리량을 검증하십시오.',
      '',
      '### 다음 단계',
      '',
      '| 단계 | 산출물 |',
      '|------|--------|',
      '| 1 | 부록 A 입력·CAP 산정 |',
      '| 2 | ENV-002~004 설정 Grid |',
      '| 3 | 47장 시험 계획 |',
      '| 4 | Z 부록 검증결과서 |',
      '| 5 | AA 부록 Go-Live |',
      '',
      '### 참조 문서',
      '',
      '- [13단계 요약](./zNSIGHT-용량산정-전체-흐름.md)',
      '- [부록 A — 입력양식](./부록/A-용량산정-입력자료-양식.md)',
      '- CAP `/oc/capacity.html` · ENV `/oc/env-002.html`',
      '',
      '### 용량산정 완료 정의',
      '',
      '| # | 완료 조건 |',
      '|---|----------|',
      '| 1 | 부록 A·CAP 산정과 설정 파일 정합 |',
      '| 2 | ENV rule-check 통과 |',
      '| 3 | 360/720/1080 시험 합격 |',
      '| 4 | 확정값·이력(AB) 반영 |',
      '| 5 | Capacity Review 주기 운영 |',
      '',
      '### 감사·PMO 대응',
      '',
      '| 요청 | 제공 문서 |',
      '|------|----------|',
      '| 산정 근거 | 부록 A·AD |',
      '| 설정 근거 | ENV-004·부록 G~L |',
      '| 시험 근거 | Z 부록 |',
      '| 변경 이력 | AB 부록 |',
      '',
      '감사·PMO 요청 시 위 문서를 **동일 변경번호**로 제출합니다.',
      '',
      thinChapterExtra(57),
      '');
  }
  return lines.join('\n');
}

module.exports = { buildRichSection, buildChapterSupplement, CHAPTER_CTX, STD, verifyTable };
