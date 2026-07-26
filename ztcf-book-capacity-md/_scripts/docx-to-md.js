/**
 * znsight-capacity-word docx 추출 텍스트 → Markdown 변환
 */
const fs = require('fs');
const path = require('path');

const EXTRACT_DIR = path.join(__dirname, '..', '_extract');

function loadExtract(filename) {
  const p = path.join(EXTRACT_DIR, filename);
  if (!fs.existsSync(p)) return '';
  return fs.readFileSync(p, 'utf8');
}

/** 원문 텍스트에서 섹션 범위 추출 (슬라이스 키는 원문 그대로, 예: "4. 1단계") */
function extractRawSlice(raw, fromKey, toKey) {
  if (!fromKey) return raw;
  const start = raw.indexOf(fromKey);
  if (start < 0) return raw;
  let end = raw.length;
  if (toKey) {
    const rel = raw.indexOf(toKey, start + fromKey.length);
    if (rel > start) end = rel;
  }
  return raw.slice(start, end).trim();
}

function preprocessRawText(raw) {
  let s = raw.replace(/\r\n/g, '\n').trim();

  // 문서 메타·목차 블록 분리
  s = s.replace(/목차/g, '\n\n목차\n\n');
  s = s.replace(/문서 개정 이력/g, '\n\n문서 개정 이력\n\n');
  s = s.replace(/문서 유형[:：]/g, '\n\n문서 유형:');

  // 하위 절 (7.1, 17.3 등) — 공백 필수 (1.2초 같은 소수 제외)
  s = s.replace(/(\d{1,2})\.(\d{1,2}) (?=[가-힣A-Za-z\[])/g, '\n\n### $1.$2 ');
  s = s.replace(/(\d{1,2})\.(\d{1,2})\.(\d{1,2}) (?=[가-힣A-Za-z])/g, '\n\n#### $1.$2.$3 ');

  // 상위 절 — "N. 한글" (소수·버전 제외)
  s = s.replace(/([가-힣\.\)다음함음])\s*(\d{1,2})\. (?=[가-힣\[])/g, '$1\n\n## $2. ');
  s = s.replace(/(?<=^|\n)(\d{1,2})\. (?=[가-힣\[])/gm, '\n\n## $1. ');

  // 키워드·주의
  s = s.replace(/주의[:：]/g, '\n\n> **주의**:');
  s = s.replace(/핵심[:：]/g, '\n\n> **핵심**:');
  s = s.replace(/결론[:：]/g, '\n\n> **결론**:');

  // 흐름도
  s = s.replace(/(\[[^\]]*?[↓→↔][^\]]*?\])/g, '\n\n```\n$1\n```\n');
  s = s.replace(/([가-힣A-Za-z0-9\s\/·]+)\s+↓/g, '$1\n↓');

  // 산식·공식 단독 행
  s = s.replace(/([A-Za-z가-힣 ]+ = [^\n]{5,120})(?=[가-힣A-Z\d])/g, '$1\n\n');

  // 붙은 절 번호 분리 (예: "범위2. Tomcat")
  s = s.replace(/([가-힣\)])(\d{1,2})\. (?=[가-힣A-Z\[])/g, '$1\n\n## $2. ');

  // 절 제목과 본문 붙음 (예: "적용 범위본 문서는")
  s = s.replace(/(## \d{1,2}\. [^\n]{4,60}?)(본 문서는|본 가이드는|본 문서의|NSIGHT 마케팅|NSIGHT 변경)/g, '$1\n\n$2');

  // 시사점·마무리 붙음 (산정가이드 docx)
  s = s.replace(/시사점시사점내용/g, '\n\n### 시사점 요약\n\n| 시사점 | 내용 |\n|--------|------|\n| ');
  s = s.replace(/다르다세션은/g, '다르다 | 세션은');
  s = s.replace(/기준이다。\s*\n?\s*AP/g, '기준이다. |\n| AP');
  s = s.replace(/핵심이다。\s*\n?\s*DB Pool/g, '핵심이다. |\n| DB Pool');
  s = s.replace(/한다。\s*\n?\s*DB Session/g, '한다. |\n| DB Session');
  s = s.replace(/한다。\s*\n?\s*최종값/g, '한다. |\n| 최종값');
  s = s.replace(/마무리말이 가이드/g, '마무리말\n\n이 가이드');

  return s;
}

/** 환경셋팅 docx — 한 줄로 붙은 영역/설정 표 분리 */
const ENV_TABLE_HEADER = '영역설정 항목권장값실제 설정 위치설정 예시설명';

const ENV_SETTING_LOCATIONS = [
  'WebTopSuite Runtime Config',
  'Mapper XML / SQL 표준',
  'SQL 리뷰 / 튜닝 기준',
  'SQL 리뷰 기준',
  'Apache httpd.conf / vhost',
  'Apache httpd.conf',
  'application.yml / Mapper',
  'application.yml',
  '용량산정 기준서',
  '성능테스트 계획서',
  '성능 목표서',
  'SLA / 성능 기준서',
  '세션 설계서',
  'IaaS VM Spec',
  'JVM 옵션',
  'setenv.sh',
  'server.xml',
  'L4 Virtual Server',
  'L4 설정',
  'httpd.conf',
  'Resilience4j',
  'logback-spring.xml',
  '예외 승인',
  'Filter / Logback',
  'WebClient',
  '@Transactional',
  'systemd unit',
  'SQL 표준',
].sort((a, b) => b.length - a.length);

function splitSettingAndValue(text) {
  const patterns = [
    /^(.+?)(\d[\d,~%~\s/GB초분TPS%hms]*.*)$/s,
    /^(.+?)(RDW 금지[^]*|금지[^]*|p95[^가-힣]*.*)$/s,
    /^(.+?)(G1GC|필수|true|false|-\XX[\w:+\-]*.*)$/s,
    /^(.{2,40}?)([A-Z][\w:+.\-]{2,})$/,
  ];
  for (const re of patterns) {
    const m = text.match(re);
    if (m && m[1].length >= 2) return { setting: m[1].trim(), value: m[2].trim() };
  }
  return { setting: text.trim(), value: '—' };
}

function splitExampleDesc(after) {
  const yamlIdx = after.search(/(?:^mybatis:|^spring\.|^server\.|^JAVA_OPTS|^-Xms)/m);
  if (yamlIdx > 0) {
    return { example: after.slice(0, yamlIdx).trim() || '—', desc: after.slice(yamlIdx).trim() };
  }
  const m = after.match(/^([\w:=.\-\s/]+?)([가-힣(].*)$/s);
  if (m && m[1].length >= 2) return { example: m[1].trim(), desc: m[2].trim() };
  const m2 = after.match(/^(.+?)(\d)([가-힣].*)$/s);
  if (m2) return { example: (m2[1] + m2[2]).trim(), desc: m2[3].trim() };
  if (after.length > 50) {
    const mid = after.search(/[가-힣]/);
    if (mid > 2) return { example: after.slice(0, mid).trim(), desc: after.slice(mid).trim() };
  }
  return { example: after.trim() || '—', desc: '—' };
}

function parseEnvTableBody(body, area) {
  const chunks = body.split(new RegExp(`(?=${area})`, 'g')).filter((c) => c.startsWith(area));
  const rows = [];
  for (const chunk of chunks) {
    const rest = chunk.slice(area.length);
    let locIdx = -1;
    let loc = '';
    for (const candidate of ENV_SETTING_LOCATIONS) {
      const i = rest.indexOf(candidate);
      if (i > 0 && (locIdx < 0 || i < locIdx)) {
        locIdx = i;
        loc = candidate;
      }
    }
    if (locIdx < 0) continue;
    const sv = splitSettingAndValue(rest.slice(0, locIdx));
    const { example, desc } = splitExampleDesc(rest.slice(locIdx + loc.length));
    rows.push(`| ${area} | ${sv.setting} | ${sv.value} | ${loc} | ${example} | ${desc} |`);
  }
  return rows;
}

function formatEnvSettingSections(s) {
  if (!s.includes(ENV_TABLE_HEADER)) return s;

  s = s.replace(
    /(분리 산정해야 합니다。?)(\d{1,2})\./,
    '$1\n\n## $2.',
  );

  const sectionRe = /(\d{1,2})\. ([^\d\n]{4,90}?)영역설정 항목권장값실제 설정 위치설정 예시설명/g;
  let out = '';
  let last = 0;
  let m;
  const matches = [...s.matchAll(sectionRe)];
  if (matches.length === 0) return s;

  for (const match of matches) {
    out += s.slice(last, match.index);
    const num = match[1];
    const title = match[2].trim();
    const bodyStart = match.index + match[0].length;
    const next = matches.find((x) => x.index > match.index);
    const bodyEnd = next ? next.index : s.length;
    const body = s.slice(bodyStart, bodyEnd);

    const areaMatch = body.match(/^(MyBatis|공통|WebTopSuite|GSLB|L4|Apache|Tomcat|Spring|CruzAPIM|JVM|Hikari|로그|세션|SPRING)/);
    const area = areaMatch ? areaMatch[1] : null;

    out += `\n\n## ${num}. ${title}\n\n`;
    out += '| 영역 | 설정 항목 | 권고값 | 설정 위치 | 설정 예시 | 설명 |\n';
    out += '|------|-----------|--------|-----------|-----------|------|\n';

    if (area) {
      out += parseEnvTableBody(body, area).join('\n');
      const tail = body.split(new RegExp(`(?=${area})`)).pop();
      const tailText = tail && !tail.startsWith(area) ? tail : '';
      const prose = body.replace(new RegExp(`${area}[\\s\\S]*`, 'g'), '').trim();
      const extra = (prose || tailText).replace(/^mybatis:[\s\S]+?(?=RDW|$)/, (block) => `\n\n\`\`\`yaml\n${block.trim().replace(/: /g, ':\n  ')}\n\`\`\`\n\n`);
      if (extra && extra.length > 20 && !extra.startsWith('|') && !extra.endsWith('|')) {
        out += `\n\n${extra}\n`;
      }
    } else {
      out += body.trim();
    }
    out += '\n';
    last = bodyEnd;
  }
  out += s.slice(last);
  return out;
}

/** 붙은 본문·코드 블록 분리 */
function splitGluedProse(s) {
  s = s.replace(/([。\.])([가-힣A-Z])/g, '$1\n\n$2');
  s = s.replace(/(설명)(MyBatis|공통|WebTopSuite|Spring|Tomcat|JVM|Hikari|CruzAPIM|GSLB|L4|Apache)/g, '$1\n\n$2');
  s = s.replace(/mybatis:\s*configuration:/g, '\n\n```yaml\nmybatis:\n  configuration:');
  s = s.replace(/(default-fetch-size:\s*\d+)(RDW)/g, '$1\n```\n\n$2');
  return s;
}

/** JVM 옵션·systemd 블록 정리 */
function formatJvmAndConfig(s) {
  // -Xms12g-Xmx12g → 줄바꿈
  s = s.replace(/(-X[a-zA-Z][^\s-]{2,30})(?=-)/g, '$1\n');
  s = s.replace(/(-XX:[^\s-]{5,60})(?=-)/g, '$1\n');
  s = s.replace(/(-D[a-zA-Z][^\s-]{3,50})(?=-)/g, '$1\n');

  // systemd unit
  s = s.replace(/(\[Unit\][\s\S]{30,1500}?WantedBy=multi-user\.target)/g, '\n\n```ini\n$1\n```\n');

  // setenv.sh — JAVA_OPTS 줄 분리
  s = s.replace(/(JAVA_OPTS="\$\{JAVA_OPTS\} [^"]{10,200}")/g, (m) => {
    const inner = m.replace(/JAVA_OPTS="\$\{JAVA_OPTS\} /, '').replace(/"$/, '');
    const parts = inner.split(/(?=-X)/).filter(Boolean).map((p) => `JAVA_OPTS="\${JAVA_OPTS} ${p.trim()}"`);
    return parts.join('\n');
  });

  return s;
}

/** 자주 등장하는 NSIGHT 표를 Markdown 표로 치환 */
function formatKnownTables(s) {
  const rules = [
    {
      re: /시나리오전체 사용자동시 요청률동시 요청자의미낮은 부하21,600명3%648명평상시[^]*?스트레스21,600명15%3,240명한계 검증[^]*?(?=동시 요청률은|##|\n\n>)/,
      md: `| 시나리오 | 전체 사용자 | 동시 요청률 | 동시 요청자 | 의미 |
|----------|-------------|-------------|-------------|------|
| 낮은 부하 | 21,600명 | 3% | 648명 | 평상시 또는 낮은 업무 집중도 |
| 기본 운영 | 21,600명 | 5% | 1,080명 | 일반 피크 기준 |
| 피크 설계 | 21,600명 | 10% | 2,160명 | 업무 집중 및 캠페인 집중 기준 |
| 스트레스 | 21,600명 | 15% | 3,240명 | 한계 검증 및 성능시험 기준 |`,
    },
    {
      re: /시나리오동시 요청자목표 응답시간산정 TPS적용 기준낮은 부하648명3초216 TPS[^]*?스트레스3,240명3초1,080 TPS한계 검증[^]*?(?=3초는|##|\n\n>)/,
      md: `| 시나리오 | 동시 요청자 | 목표 응답시간 | 산정 TPS | 적용 기준 |
|----------|-------------|---------------|----------|-----------|
| 낮은 부하 | 648명 | 3초 | 216 TPS | 평상시 참고 |
| 기본 운영 | 1,080명 | 3초 | 360 TPS | 기본 운영 기준 |
| 피크 설계 | 2,160명 | 3초 | 720 TPS | 설계 대표 기준 |
| 스트레스 | 3,240명 | 3초 | 1,080 TPS | 한계 검증 기준 |`,
    },
    {
      re: /산정 항목산식계산값설명전체 사용자3,600개 지점 x 6명21,600명[^]*?설계 세션 기준약 26,000~28,000권장 기준[^]*?(?=설계 세션은|##|\n\n>)/,
      md: `| 산정 항목 | 산식 | 계산값 | 설명 |
|-----------|------|--------|------|
| 전체 사용자 | 3,600개 지점 x 6명 | 21,600명 | 전체 등록 또는 사용 가능 사용자 |
| 최대 로그인 세션 | 전체 사용자 100% | 21,600 세션 | 모든 사용자가 로그인한 상태 |
| 20% 여유율 세션 | 21,600 x 1.2 | 25,920 세션 | 운영 여유 반영 |
| 30% 여유율 세션 | 21,600 x 1.3 | 28,080 세션 | 보수적 설계 기준 |
| 설계 세션 기준 | — | 약 26,000~28,000 | 권장 기준 |`,
    },
    {
      re: /항목기본 기준설명지점 수3,600개[^]*?스트레스15%3,240명1,080 TPS한계 검증[^]*?(?=주의|##|\n\n>)/,
      md: `| 항목 | 기본 기준 | 설명 |
|------|-----------|------|
| 지점 수 | 3,600개 | NSIGHT 기본 사용자 산정 기준 |
| 지점당 사용자 | 6명 | 영업점 사용 기준 |
| 전체 사용자 | 21,600명 | 3,600 × 6명 |
| 설계 세션 | 26,000~28,000 세션 | 전체 사용자 + 20~30% 여유율 |
| 목표 응답시간 | p95 3초 이하 | 사용자 체감 기준 |
| 기본 운영 TPS | 360 TPS | 동시 요청률 5% 기준 |
| 피크 설계 TPS | 720 TPS | 동시 요청률 10% 기준 |
| 스트레스 테스트 | 1,080 TPS | 동시 요청률 15% 기준 |
| 기준 VM | 8 vCPU / 32GB | 운영 안정성 중심 Scale-Out 단위 |
| VM당 처리 기준 | 200~250 TPS | 일반 200, SingleView 250 TPS 보수 기준 |`,
    },
    {
      re: /설정 항목8 Core \/ 32GB 기준16 Core 기준32 Core 기준설명protocol[^]*?server빈 값[^]*?(?=주의|server\.xml|##|\n\n>)/,
      md: `| 설정 항목 | 8 Core / 32GB | 16 Core | 32 Core | 설명 |
|-------------|---------------|---------|---------|------|
| protocol | Http11NioProtocol | 동일 | 동일 | 표준 NIO Connector |
| maxThreads | 400~500 | 800~1,000 | 1,500~1,800 | Worker Thread |
| minSpareThreads | 100 | 150~200 | 300~400 | 피크 진입 전 대기 Thread |
| acceptCount | 300~500 | 500~800 | 800~1,000 | Thread 포화 시 대기 큐 |
| maxConnections | 10,000 | 16,000 | 20,000 | 동시 연결 상한 |
| connectionTimeout | 8,000ms | 8,000ms | 8,000ms | 연결 후 요청 대기 |
| keepAliveTimeout | 120,000ms | 120,000ms | 120,000ms | KeepAlive 유지 |
| maxKeepAliveRequests | 1,000 | 1,000 | 1,000 | 연결당 최대 요청 |
| URIEncoding | UTF-8 | UTF-8 | UTF-8 | 한글 파라미터 |`,
    },
    {
      re: /계층권장값설정 위치정합성 기준DB Query Timeout2~3초[^]*?L4 Idle Timeout120초[^]*?(?=권장 순서|예시|##|\n\n>)/,
      md: `| 계층 | 권장값 | 설정 위치 | 정합성 기준 |
|------|--------|-----------|-------------|
| DB Query Timeout | 2~3초 | MyBatis/JDBC | 가장 먼저 실패 |
| Hikari Connection Timeout | 2~3초 | application.yml | Pool 대기 제한 |
| Spring Transaction Timeout | 4~5초 | @Transactional | 업무 트랜잭션 한도 |
| Tomcat connectionTimeout | 8초 | server.xml | 연결 후 요청 대기 |
| Apache Proxy Timeout | 10초 | httpd.conf | AP 응답 대기 |
| WebTopSuite Request Timeout | 15초 | Runtime Config | 사용자 체감 한도 |
| L4 Idle Timeout | 120초 | L4 설정 | KeepAlive 유지 |`,
    },
    {
      re: /설정 항목권장값설정 예시검증 방법Wide IP[^]*?Failure Policy정상 Pool만 반환return only available VIP강제 장애 테스트[^]*?(?=GSLB 논리|##|###|$)/,
      md: `| 설정 항목 | 권장값 | 설정 예시 | 검증 방법 |
|-----------|--------|-----------|-----------|
| Wide IP / 서비스 도메인 | 업무 서비스 도메인 | mkt.nh.local → DC1/DC2 VIP | nslookup/dig 결과 확인 |
| Pool 구성 | 센터별 L4 VIP 등록 | DC1_L4_VIP, DC2_L4_VIP | 정상 센터만 응답 확인 |
| DNS TTL | 30초 | ttl=30 | 장애 시 재조회 전환시간 측정 |
| Load Balance Method | Priority 또는 Round Robin | Active-Active Round Robin | 센터별 유입 비율 확인 |
| Health Check | HTTPS 443 + URL | GET /health/l4 | 장애 VIP DNS 응답 제외 |
| Failure Policy | 정상 Pool만 반환 | return only available VIP | 강제 장애 테스트 |`,
    },
    {
      re: /검증 영역검증 시나리오성공 기준확인 지표정상 조회일반 조회 360\/720 TPS[^]*?세션 만료Idle Timeout[^]*?(?=##|주의|$)/,
      md: `| 검증 영역 | 검증 시나리오 | 성공 기준 | 확인 지표 |
|-----------|---------------|-----------|-----------|
| 정상 조회 | 일반 조회 360/720 TPS 부하 | p95 3초 이하, Timeout 급증 없음 | TPS, p95, CPU, SQL Time |
| DB 지연 | RDW SQL 지연 유도 | DB Query Timeout 후 AP Thread 회수 | SQL Timeout, Thread Active |
| Pool 고갈 | Hikari Pool 제한 후 부하 | 3초 내 Connection Timeout, 장애 전파 없음 | Hikari Pending, 오류코드 |
| 외부연계 지연 | CruzAPIM 응답 지연 | 5초 내 실패, Circuit Breaker 동작 | CB 상태, 연계 오류율 |
| 단말 Timeout | Client Read Timeout 유도 | 상태조회 API로 결과 확인 | GUID, 거래상태 |
| 세션 만료 | Idle Timeout 경과 | 재로그인 유도, 업무데이터 노출 없음 | 세션 로그, 보안 로그 |`,
    },
    {
      re: /모니터링 항목WarningCritical조치 방향Heap 사용률70% 이상[^]*?Native Memory지속 증가[^]*?(?=JVM 운영|##|\n\n>)/,
      md: `| 모니터링 항목 | Warning | Critical | 조치 방향 |
|---------------|---------|----------|-----------|
| Heap 사용률 | 70% 이상 지속 | 85% 이상 | 객체·세션·캐시·누수 분석 |
| GC Pause p95 | 200ms 초과 | 500ms 이상 반복 | Heap/객체/SQL 대기 분석 |
| Full GC | 발생 자체 점검 | 반복 발생 | Heap Dump·GC Log |
| Tomcat Busy Thread | 70% 이상 | 90% 이상 | Thread Dump·Pool Wait |
| Hikari Active | 70% 이상 | 90% 이상 | SQL·Pool·DB Session |
| CPU 사용률 | 70% 이상 | 85% 이상 | Scale-Out·Hot Method |
| Native Memory | 지속 증가 | OOM 위험 | NMT·Direct Buffer 점검 |`,
    },
    {
      re: /영역설정 항목8 vCPU \/ 32GB 권장16 vCPU[^]*?OS 여유Heap 외 메모리14~18GB[^]*?(?=##|주의|###|$)/,
      md: `| 영역 | 설정 항목 | 8 vCPU/32GB | 16 vCPU/64GB | 32 vCPU/256GB |
|------|-----------|-------------|--------------|---------------|
| Heap | 일반 마케팅 AP | 12GB | 24~28GB | JVM 분리 권장 |
| Heap | SingleView AP | 14GB | 28~32GB | 선도검증 필수 |
| GC | Collector | G1GC | G1GC | G1GC(ZGC 검토) |
| GC | MaxGCPauseMillis | 200ms | 200ms | 200~300ms |
| Thread Stack | -Xss | 512k | 512k | 512k~1m |
| Metaspace | MaxMetaspaceSize | 1g | 1~2g | 2~4g |
| Code Cache | ReservedCodeCacheSize | 256m | 256~512m | 512m |
| Dump | HeapDumpOnOOM | 활성화 | 활성화 | 활성화 |
| GC Log | Xlog:gc* | 활성화 | 활성화 | 활성화 |
| OS 여유 | Heap 외 메모리 | 14~18GB | 30GB+ | 100GB+ |`,
    },
    {
      re: /계층JVM과의 관계주요 검증 포인트WebTopSuite[^]*?JVMHeap, GC[^]*?(?=##|주의|###|$)/,
      md: `| 계층 | JVM과의 관계 | 검증 포인트 |
|------|-------------|-------------|
| WebTopSuite | Timeout 기준 | Client Timeout 이전 AP 정상 실패 |
| L4/Apache | 요청 전달 | Proxy Timeout > JVM 처리시간 |
| Tomcat | Thread·세션 | Thread 포화·세션 복제 부하 |
| Spring Boot | Tx·Pool | Transaction Timeout·Pool Wait |
| JVM | Heap·GC·Dump | GC Pause·Heap·OOM |`,
    },
    {
      re: /메모리 구성 요소설명8 vCPU[^]*?OS \/ File Cache[^]*?(?=###|##|주의|$)/,
      md: `| 구성 요소 | 설명 | 8C/32G 권장 |
|-----------|------|------------|
| Java Heap | DTO·세션·캐시 | 일반 12GB, SV 14GB |
| Thread Stack | Worker Stack | -Xss512k |
| Metaspace | Class Metadata | Max 1GB |
| Code Cache | JIT 코드 | 256MB |
| Direct/Native | NIO·TLS·라이브러리 | 여유 확보 |
| APM/Agent | 관측 Agent | 2~4GB |
| OS/File Cache | OS·로그 | 6~8GB+ |`,
    },
    {
      re: /GC 종류특징NSIGHT 적용 판단Serial GC[^]*?ZGC[^]*?(?=G1GC 기본|튜닝|##|###|$)/,
      md: `| GC 종류 | 특징 | NSIGHT 판단 |
|---------|------|-------------|
| Serial GC | 단일 Thread | 온라인 AP 부적합 |
| Parallel GC | 처리량 중심 | 배치성 제한 검토 |
| **G1GC** | 응답·처리량 균형 | **표준 권장** |
| ZGC | 초저지연 | JDK21+ 특수 고부하 |`,
    },
    {
      re: /증상가능 원인우선 확인조치응답시간 급증[^]*?GC Log 미생성[^]*?(?=##|점검|$)/,
      md: `| 증상 | 가능 원인 | 우선 확인 | 조치 |
|------|-----------|-----------|------|
| 응답시간 급증 | SQL·연계·GC Pause | APM·GC Log·Hikari | SQL 튜닝·Timeout·Bulkhead |
| CPU 90%+ | Thread·로깅·암호화 | top·jstack·APM | Scale-Out·로그 레벨 |
| Heap 지속 증가 | 세션 비대화·누수 | Heap Histogram | 세션 금지·캐시 제한 |
| Full GC 반복 | Old 영역 급증 | GC Log·Dump | Heap·객체 생명주기 |
| OOM | Heap/Metaspace/Direct | hs_err·Dump·NMT | 유형별 보정 |
| Thread 고갈 | Pool·연계·Deadlock | Thread Dump | Timeout·Pool 분리 |
| GC Log 미생성 | 옵션·권한 | VM.flags | setenv/systemd 수정 |`,
    },
    {
      re: /8\. JVM 설정영역설정 항목일반 AP 권장값SingleView AP 권장값[^]*?JVMTimezoneAsia\/Seoul[^]*?(?=GC Pause|9\.|##|$)/,
      md: `| 영역 | 설정 항목 | 일반 AP | SV AP | 위치 | 예시 |
|------|-----------|---------|-------|------|------|
| JVM | Heap Initial/Max | 12GB | 14GB | setenv.sh | -Xms12g -Xmx12g |
| JVM | Thread Stack | 512KB | 512KB | JVM 옵션 | -Xss512k |
| JVM | GC | G1GC | G1GC | JVM 옵션 | -XX:+UseG1GC |
| JVM | Pause 목표 | 200ms | 200ms | JVM 옵션 | MaxGCPauseMillis=200 |
| JVM | Heap Dump | 활성화 | 활성화 | JVM 옵션 | HeapDumpOnOOM |
| JVM | GC Log | 활성화 | 활성화 | JVM 옵션 | -Xlog:gc* |
| JVM | Timezone | Asia/Seoul | Asia/Seoul | JVM 옵션 | -Duser.timezone=Asia/Seoul |`,
    },
    {
      re: /10\. 최종 환경설정 요약표영역핵심 권장값VM8 vCPU[^]*?Retry동기거래 기본 금지[^]*?(?=이 표의|##|$)/,
      md: `| 영역 | 핵심 권장값 |
|------|-------------|
| VM | 8 vCPU / 32GB |
| VM당 처리량 | 250 TPS |
| 목표 응답시간 | p95 3초 이하 |
| WebTopSuite Timeout | 15초 |
| Proxy Connect/Read | 3초 / 10초 |
| L4 Sticky Timeout | 70분 |
| L4 Health Check | 5초 / 2초 / Fail 3회 |
| Tomcat maxThreads | 400~500 |
| Tomcat acceptCount | 300~500 |
| Spring Session Idle | 60분 |
| Absolute Session | 8시간 |
| Transaction Timeout | 4~5초 |
| Hikari Pool | 50 / 60 |
| connectionTimeout | 3초 |
| SQL Timeout | 3초 |
| CruzAPIM Connect/Read | 3초 / 5초 |
| JVM Heap | 12GB / 14GB |
| GC | G1GC |
| GUID/MDC | 필수 |
| Circuit Breaker | 필수 |
| Retry | 동기거래 기본 금지 |`,
    },
    {
      re: /영역설정 항목권장값실제 설정 위치(?:설정 예시)?설명MyBatis기본 Statement Timeout3초[^]*?RDW 자원경합 방지[^]*?(?=mybatis:|##|$)/,
      md: `| 영역 | 설정 항목 | 권고값 | 설정 위치 | 설명 |
|------|-----------|--------|-----------|------|
| MyBatis | default-statement-timeout | 3초 | application.yml | 전체 SQL 기본 |
| MyBatis | 일반 조회 SQL | 2~3초 | Mapper XML | timeout=3 |
| MyBatis | SV 핵심 SQL | 3초 | Mapper XML | RDW 고빈도 |
| MyBatis | 복합 조회 | 5초 이내 | 예외 승인 | — |
| MyBatis | 분석 SQL | RDW 금지 | SQL 리뷰 | ADW 전용 |
| MyBatis | default-fetch-size | 100~500 | application.yml | 300 권장 |
| MyBatis | Full Scan | 금지 | SQL 리뷰 | RDW 보호 |`,
    },
    {
      re: /영역설정 항목권장값실제 설정 위치설명SPRINGserver\.servlet\.session\.timeout60m[^]*?연계 장애가 AP 전체로 전파[^]*?(?=application\.yml|##|$)/,
      md: `| 영역 | 설정 항목 | 권고값 | 설정 위치 | 설명 |
|------|-----------|--------|-----------|------|
| SPRING | session.timeout | 60m | application.yml | 유휴 세션 |
| SPRING | cookie.http-only | true | application.yml | XSS 방지 |
| SPRING | cookie.secure | true | application.yml | HTTPS |
| SPRING | Transaction Timeout | 4~5초 | @Transactional | 업무 한도 |
| SPRING | Retry | 기본 금지 | Resilience4j | 부하 증폭 방지 |
| SPRING | Circuit Breaker | 필수 | Resilience4j | 연계 격리 |
| SPRING | GUID/MDC | 필수 | Filter/Logback | 추적 |
| SPRING | CruzAPIM Connect | 3s | WebClient | 연결 |
| SPRING | CruzAPIM Read | 5s | WebClient | 응답 |`,
    },
    {
      re: /15\. 시사점시사점내용세션과 TPS는 다르다[^]*?선도개발로 보정해야 한다[^]*?실측해야 한다。[^]*?(?=##|\d{1,2}\.|$)/,
      md: `| 시사점 | 내용 |
|--------|------|
| 세션 ≠ TPS | 세션은 로그인 유지, TPS는 동시 요청자 기준 |
| 잔여 처리량 | 센터 장애 후 목표 TPS 감당이 핵심 |
| DB Pool | SQL·점유시간 기준 산정, 무단 확대 금지 |
| DB Session | RDW/ADW·온라인/배치/BI 분리 산정 |
| 선도개발 보정 | TPS/Thread/Pool/GC/SQL Time 실측 필수 |`,
    },
    {
      md: `| 항목 | 8C/32G | 16C/64G | 32C/256G | 판단 |
|------|--------|---------|----------|------|
| 온라인 AP | **표준** | 특수 | 배치 | 8C Scale-Out |
| 장애 영향(8대) | 12.5% | 25%(4대) | — | 8C 유리 |
| Rolling 배포 | 용이 | 부담 | — | 8C 유리 |
| DB Pool 통제 | 작은 단위 | 큰 단위 | — | RDW 보호 |
| Heap 분석 | 단순 | 복잡 | — | 8C 유리 |`,
    },
  ];

  for (const { re, md } of rules) {
    s = s.replace(re, `\n\n${md}\n\n`);
  }
  return s;
}

function formatCodeBlocks(s) {
  const patterns = [
    {
      re: /(<Connector[\s\S]{30,600}?\/>)/g,
      lang: 'xml',
    },
    {
      re: /(<Server[\s\S]{50,2500}?<\/Server>)/g,
      lang: 'xml',
    },
    {
      re: /(<Valve[\s\S]{30,500}?\/>)/g,
      lang: 'xml',
    },
    {
      re: /(<web-app[\s\S]{30,800}?<\/web-app>)/g,
      lang: 'xml',
    },
    {
      re: /(<session-config>[\s\S]{20,200}?<\/session-config>)/g,
      lang: 'xml',
    },
    {
      re: /(server:\s+[\s\S]{30,1200}?)(?=\n\n##|\n\n>|주의|management:|mybatis:|spring\.)/g,
      lang: 'yaml',
    },
    {
      re: /(@Transactional[\s\S]{20,300}?)(?=\n\n|주의|##)/g,
      lang: 'java',
    },
    {
      re: /(public class \w+Session implements[\s\S]{30,500}?})/g,
      lang: 'java',
    },
    {
      re: /(\#\!\/bin\/sh[\s\S]{20,600}?)(?=\n\n##|\n\n>|주의|$)/g,
      lang: 'shell',
    },
    {
      re: /(-Xms[\w\d]+[\s\S]{20,500}?)(?=\n\n|주의|##|점검|$)/g,
      lang: 'shell',
    },
  ];

  for (const { re, lang } of patterns) {
    s = s.replace(re, (match, block) => {
      if (block.includes('```')) return match;
      return `\n\n\`\`\`${lang}\n${block.trim()}\n\`\`\`\n`;
    });
  }
  return s;
}

function convertDocxTextToMarkdown(raw, title, sourceName) {
  if (!raw || !raw.trim()) return '';

  let s = preprocessRawText(raw);
  s = formatEnvSettingSections(s);
  s = splitGluedProse(s);
  s = formatKnownTables(s);
  s = formatJvmAndConfig(s);
  s = formatCodeBlocks(s);

  if (title) {
    s = `# ${title}\n\n> 원본: znsight-capacity-word${sourceName ? ` · \`${sourceName}\`` : ''}\n\n` + s;
  }

  s = s.replace(/\n{4,}/g, '\n\n\n');
  s = s.replace(/[ \t]+\n/g, '\n');
  s = s.replace(/^##\s*$/gm, '');

  return s.trim() + '\n';
}

/** Markdown 변환 후 섹션 번호 범위 추출 (레거시) */
function extractSections(md, fromSec, toSec) {
  if (!fromSec) return md;
  const fromRe = new RegExp(`#+ ${fromSec.replace('.', '\\.')}[\\.\\s]`);
  const start = md.search(fromRe);
  if (start < 0) return md;
  let end = md.length;
  if (toSec) {
    const nextNum = parseInt(toSec.split('.')[0], 10) + 1;
    const toRe = new RegExp(`\\n#+ ${nextNum}\\. `);
    const e = md.slice(start + 10).search(toRe);
    if (e >= 0) end = start + 10 + e;
  }
  return md.slice(start, end).trim();
}

module.exports = {
  EXTRACT_DIR,
  loadExtract,
  extractRawSlice,
  convertDocxTextToMarkdown,
  extractSections,
};
