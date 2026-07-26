/**
 * 목차 전 절(섹션) 자동 심화 본문 생성
 * NSIGHT 1차 표준 + 챕터/제목 키워드 기반
 */
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
  timeout: 'DB 3s · Pool 3s · Tx 5s · Proxy 10s · Web 15s · L4 120s',
};

function enrichByTitle(title, ch) {
  const t = title;
  const lines = [];

  if (/지점|사용자/.test(t)) {
    lines.push(`NSIGHT 1차: **${STD.branches}지점 × ${STD.usersPerBranch}명 = ${STD.totalUsers.toLocaleString()}명**.`);
    lines.push('지점 수·지점당 사용자는 업무팀 합의 후 CAP 산정 입력값으로 고정.');
  }
  if (/세션/.test(t)) {
    lines.push(`설계 세션 **${STD.sessions}** (전체 사용자 + 20~30% 여유).`);
    lines.push('세션 = 로그인 유지 규모. **세션 증가 ≠ TPS 증가** (Heap·복제 부담은 증가).');
  }
  if (/TPS|동시처리|동시요청|Little/.test(t)) {
    lines.push('| 시나리오 | 동시요청률 | 동시요청자 | TPS(3초) |');
    lines.push('|----------|-----------|-----------|---------|');
    lines.push(`| 기본 | 5% | 1,080 | ${STD.tps.base} |`);
    lines.push(`| 피크 | 10% | 2,160 | ${STD.tps.peak} |`);
    lines.push(`| 스트레스 | 15% | 3,240 | ${STD.tps.stress} |`);
    lines.push('```\nTPS = 동시요청자 ÷ 목표응답시간(초)\n동시처리량 = TPS × 평균점유시간(초)\n```');
  }
  if (/TPMC|Core|CPU/.test(t)) {
    lines.push('실효 **30~40 TPS/Core** → 8Core VM **250 TPS** (보수).');
    lines.push('TPMC/TPS: 일반 1,500~2,000 / SingleView 2,000~3,500. CPU 목표 60~70%.');
  }
  if (/VM|서버 대수|N\+|Scale|Rolling|센터 장애/.test(t)) {
    lines.push(`기준 VM: **${STD.vm}**, VM당 **${STD.tpsPerVm} TPS**.`);
    lines.push(`피크 ${STD.tps.peak} TPS → AP **8대 권장**(센터당 4). 장애 후 잔여 ≥ 목표 TPS.`);
  }
  if (/Heap|JVM|GC|Metaspace|Code Cache|Direct|Xms|Xmx|Xss|OOM|Dump/.test(t)) {
    lines.push(`8C/32G: Heap **${STD.heap.normal}** (일반) / **${STD.heap.sv}** (SV), G1GC, Pause 목표 200ms.`);
    lines.push('Xms=Xmx 동일. 세션·캐시에 대량 객체 저장 금지.');
    if (/GC Log|Logging/.test(t)) lines.push('`-Xlog:gc*` 필수, filecount=10, filesize=100M.');
    if (/Dump|OOM/.test(t)) lines.push('HeapDumpOnOOM + ExitOnOOM + systemd Restart=on-failure.');
  }
  if (/Thread|maxThreads|acceptCount|Connector|minSpare/.test(t)) {
    lines.push(`maxThreads **${STD.threads}**, acceptCount 300~500, maxConnections 10,000.`);
    lines.push('산정: AP TPS × 평균점유(1.2s) × 여유(1.2). p95 3초를 직접 곱하지 않음.');
  }
  if (/Pool|Hikari|Connection(?! Limit)|DB Session/.test(t)) {
    lines.push(`Pool: 일반 **${STD.pool.normal}** / SV **${STD.pool.sv}**, connectionTimeout **3초**(≠SQL).`);
    lines.push('```\nPool = max(30, min(TPS×DB점유×1.3, Thread×30%))\n센터 Pool = Σ(AP×Pool) ≤ DB max sessions\n```');
  }
  if (/Timeout|타임아웃|Idle/.test(t)) {
    lines.push('안쪽 자원부터 짧게 실패:');
    lines.push(`\`${STD.timeout}\``);
    lines.push('10초 이상 동기 거래 금지 → 비동기·결과조회 API.');
  }
  if (/MyBatis|SQL|Statement|Fetch|Slow/.test(t)) {
    lines.push('`default-statement-timeout: 3`, fetch-size 100~500. RDW Full Scan 금지.');
    lines.push('Slow SQL p95 > 1s → 튜닝·ADW 분리·페이징.');
  }
  if (/L4|GSLB|VIP|Sticky|Persistence|Drain|Health/.test(t)) {
    lines.push('GSLB TTL **30초**. L4 Health 5s/2s/Fail3. Sticky **70분**(Session+10). Idle **120초**.');
    lines.push('배포: Drain → Health fail → Graceful shutdown.');
  }
  if (/Apache|Proxy|WEB|WebTop|Worker|nginx/.test(t)) {
    lines.push('Proxy Connect **3s**, Read **10s**. KeepAlive **120s**. Request **15s**.');
    lines.push('X-Forwarded-For·X-GUID Header 전 구간 유지.');
  }
  if (/Gateway|Route|JWT|JWKS/.test(t)) {
    lines.push('Gateway route timeout ≤ Backend Proxy. JWT Access Token·JWKS Cache 업무별 정의.');
    lines.push('Circuit Breaker + Rate Limit + Bulkhead 표준 적용.');
  }
  if (/Spring|Transaction|Actuator|Graceful|Profile|Multipart|Async|Scheduler/.test(t)) {
    lines.push('`@Transactional(timeout=5)`, Session Idle 60m, Absolute 8h(Filter).');
    lines.push('Actuator: health, metrics, prometheus. Graceful shutdown 적용.');
  }
  if (/TCF|ServiceId|GUID|TraceId|Idempotency|감사|거래로그/.test(t)) {
    lines.push('TCF ServiceId별 Timeout 4~5초. GUID/TraceId MDC 필수. 재시도 기본 금지.');
    lines.push('Slow Transaction: p95 3초 초과 건 APM 알림.');
  }
  if (/세션|DeltaManager|Cookie|Redis Session/.test(t)) {
    lines.push('Idle 60m, Absolute 8h, DeltaManager 센터 내부 2~4노드. 세션 객체 Serializable·최소화.');
  }
  if (/Kafka|CDC|Consumer|Lag|DLQ|Partition/.test(t)) {
    lines.push('Consumer Lag Warning/Critical 업무별. DLQ 필수. max.poll.interval.ms ≥ 처리시간.');
    lines.push('온라인 SLA와 별도 기준. 배치 Window와 분리.');
  }
  if (/Batch|Scheduler|Chunk|Window/.test(t)) {
    lines.push('배치 Window 00:00~06:00. 온라인 Pool·CPU와 분리. 중복실행 방지.');
  }
  if (/Cache|TTL|Eviction|Stampede/.test(t)) {
    lines.push('Local vs 분산 Cache 구분. TTL·Max Entry·Stampede 방지. 장애 시 fallback 정책.');
  }
  if (/CruzAPIM|외부|HTTP Client|Retry|Bulkhead|Circuit/.test(t)) {
    lines.push('Connect **3s**, Read **5s**. Retry 기본 **금지**. CB + Bulkhead 필수.');
  }
  if (/로그|감사|Access Log|MDC|모니터|임계|지표|Actuator/.test(t)) {
    lines.push('GUID·TraceId Access Log. Warning/Critical 이중 임계치. p95·Pending 추세 관측.');
    lines.push('CPU 70/85%, Heap 70/85%, Hikari Active 70/90%, GC Pause p95 200/500ms.');
  }
  if (/파일|Upload|Multipart|Download/.test(t)) {
    lines.push('대용량 파일은 전용 서버·Streaming 분리. Request/Multipart Size 업무별 상한.');
  }
  if (/OS |vCPU|Swap|File Descriptor|TCP|Ephemeral|Timezone/.test(t)) {
    lines.push(`VM **${STD.vm}**. ulimit nofile 65535+. Swap 최소(온라인 AP). TZ Asia/Seoul.`);
    lines.push('/logs, /logs/dump, /logs/gc 디렉터리 사전 생성·권한.');
  }
  if (/검증|시험|Gate|체크|Checklist/.test(t)) {
    lines.push(`360/720/${STD.tps.stress} TPS + 장애 시나리오. p95·Busy Thread·Hikari Pending·SQL p95·GC Pause 동시 수집.`);
    lines.push('합격 없이 운영 전환 금지.');
  }
  if (/장애|복구|고갈|포화|흐름/.test(t)) {
    lines.push('GUID E2E: 단말→Proxy→AP→DB→연계. Thread·Pool·JVM 동시 분석.');
    lines.push('Thread만/Pool만 증가 금지 — 병목 원인(SQL·연계·GC) 먼저.');
  }
  if (/금지|잘못|오류/.test(t)) {
    lines.push('**금지 사례**: 사용자수=TPS, 세션=Thread, maxThreads=Pool, Heap=물리50%, 무제한 Retry.');
    lines.push('벤치마크 수치·동일 Timeout·성능시험 생략 운영 반영 금지.');
  }
  if (/RACI|책임|승인|경계/.test(t)) {
    lines.push('| 역할 | 책임 |');
    lines.push('|------|------|');
    lines.push('| 아키텍처 | 산정 모델·표준 |');
    lines.push('| 인프라 | VM·OS·L4·GSLB |');
    lines.push('| WAS | Tomcat·JVM |');
    lines.push('| 프레임워크 | Spring·TCF·Pool |');
    lines.push('| DBA | max sessions·SQL |');
    lines.push('| 성능시험 | 360/720/1080 검증 |');
    lines.push('| 운영 | 모니터링·Runbook |');
  }
  if (/변경|Rollback|이력|Review|Baseline|Deprecated|폐기/.test(t)) {
    lines.push('변경: 요청→영향도→승인→검증→반영→이력. 긴급 변경도 사후 검증 필수.');
    lines.push('일간·주간·월간 Capacity Review로 Baseline 갱신.');
  }
  if (/설정 파일|배포|Config|환경변수|민감/.test(t)) {
    lines.push('계층: 공통→환경(DEV/STG/PRD)→업무. 민감정보 평문 금지.');
    lines.push('server.xml / application.yml / setenv.sh 분리. 배포 시 Drain 절차.');
  }
  if (/산정|계산|공식|산식|예시/.test(t)) {
    lines.push('```');
    lines.push('동시요청자 = 전체사용자 × 동시요청률');
    lines.push('TPS = 동시요청자 ÷ 3');
    lines.push('AP = ⌈TPS ÷ 250⌉ (A-A 배치)');
    lines.push('Pool = AP × 50~60');
    lines.push('```');
  }
  if (/분류|표준 표현|권고값|확정값|가정값/.test(t)) {
    lines.push('| 구분 | 의미 | 예 |');
    lines.push('|------|------|-----|');
    lines.push('| 가정값 | 산정 전제 | 동시요청률 5% |');
    lines.push('| 권고값 | 표준 기준선 | maxThreads 500 |');
    lines.push('| 확정값 | 시험 실측 | 선도개발 결과 |');
    lines.push('| 임계치 | APM Warning/Critical | CPU 70/85% |');
  }
  if (/프로파일|8Core|16Core|32Core|64GB|128GB|256GB/.test(t)) {
    if (/16Core·64|16Core·128|38\.|39\./.test(t) || ch === 38 || ch === 39) {
      lines.push('16C: 온라인 기본 단위 비권장. 특수·고부하 검토. Heap 24~32GB.');
    } else if (/32Core|40\.|256/.test(t) || ch === 40) {
      lines.push('32C/256G: 배치·ETL·특수 AP. 온라인 단일 JVM 비권장. JVM 분리·Scale-Out 우선.');
    } else {
      lines.push(`**8C/32G 표준**: TPS ${STD.tpsPerVm}, Thread ${STD.threads}, Heap ${STD.heap.normal}, Pool ${STD.pool.normal}.`);
    }
  }
  if (/아키텍처|물리 구성|센터별|WAR 배치|업무그룹/.test(t)) {
    lines.push('2센터 A-A AP + DB A-S. GSLB→센터 L4→Apache→Tomcat→Spring→RDW.');
    lines.push('마케팅/SV/연계 AP VM 분리. Gateway·OM·Batch Pool 분리.');
  }
  if (/요구|제약|가용성|성장|보안 제약/.test(t)) {
    lines.push(`가용성: 센터 장애 후 잔여 TPS ≥ ${STD.tps.peak}. DB max sessions 상한 준수.`);
    lines.push('성장률 20~30% 여유. 장시간 세션(180m)은 Heap·복제 영향 검증 필수.');
  }
  if (/메모리|스토리지|OS Memory|Native|Agent|Dump 저장|로그 저장/.test(t)) {
    lines.push('32GB 배분: Heap 12~14G + Stack ~0.5G + Metaspace 1G + OS 6~8G + APM 2~4G.');
    lines.push('GC Log·Heap Dump·Access Log 디스크: AP당 50~100GB 여유 권장.');
  }
  if (/Connector|Executor|asyncTimeout|maxPost|maxSwallow|maxKeepAlive/.test(t)) {
    lines.push('Connector는 Thread·Connection·Queue의 **입구**. NIO `Http11NioProtocol` 표준.');
    lines.push('Executor 분리는 특수 업무만. 기본은 공유 Connector + Bulkhead로 제어.');
  }
  if (/Graceful|Shutdown|Readiness|Liveness|Profile/.test(t)) {
    lines.push('`server.shutdown=graceful`, shutdown phase 30s. L4 Drain과 연동.');
    lines.push('Readiness: DB·Redis·연계 Health. Liveness: JVM·Thread 데드락 감지.');
  }
  if (/입력값|계산값|측정값|분류/.test(t) && ch === 7) {
    lines.push('| 분류 | 예 | 관리 주체 |');
    lines.push('|------|-----|----------|');
    lines.push('| 입력값 | 지점 수, 동시요청률 | 업무·PMO |');
    lines.push('| 계산값 | TPS, AP 대수, Pool | 아키텍처 |');
    lines.push('| 설정값 | maxThreads, Heap | WAS·프레임워크 |');
    lines.push('| 측정값 | p95, GC Pause | 성능시험 |');
    lines.push('| 확정값 | 시험 후 확정 Pool | 운영·변경관리 |');
  }
  if (/시사점|위험|보완|발전|판단/.test(t)) {
    lines.push('**핵심 판단**: 세션≠TPS, 잔여 TPS>평상시, Pool≠Thread, 안쪽 Timeout 짧게.');
    lines.push('우선순위: SQL p95 → Pool Pending → Thread Busy → GC Pause → 연계 CB.');
  }
  if (/마무리|최종 목적|Capacity Management|고정값/.test(t)) {
    lines.push('권고값은 **출발점**. 선도개발·운영 실측·Capacity Review로 지속 보정.');
    lines.push('최종 목표: 평균이 아닌 **피크·장애 시나리오**에서도 운영 가능한 안정성.');
  }
  if (/현행|불일치|실측|문제점/.test(t)) {
    lines.push('현행 설정 vs 산정표 대조: maxThreads·Heap·Pool·Timeout 계층·센터 Pool 합산.');
    lines.push('실측 부족 시 APM·GC Log·Access Log·Hikari Metrics 수집 체계 선행 구축.');
  }
  if (/필요한 이유|도입|관계|활용|개요|목적/.test(t)) {
    lines.push('본 가이드는 **산정→설정→검증→변경관리**를 하나의 문서 체인으로 연결합니다.');
    lines.push('제2부(산정) → 제3~4부(설정) → 제5부(프로파일) → 제7부(검증·전환) 순 활용.');
  }
  if (/keepaliveTime|validationTimeout|Pool Name|autoCommit/.test(t)) {
    lines.push('Hikari: validationTimeout 1s, keepaliveTime 2~5분, poolName 업무별 분리.');
    lines.push('autoCommit=false + Spring `@Transactional` 필수. leakDetection은 STG에서 활성.');
  }
  if (/네트워크|Keep-Alive|대역폭/.test(t) && ch === 13) {
    lines.push('L4 maxConnections, Apache KeepAlive, Tomcat maxConnections 정합.');
    lines.push('센터 장애 시 잔여 네트워크·Connection으로 피크 TPS 감당 검증.');
  }

  return lines.length ? lines.join('\n\n') + '\n' : '';
}

module.exports.enrichByTitle = enrichByTitle;

function enrichByChapter(ch, sec, title) {
  const intro = {
    1: '용량산정은 서버 대수만이 아닙니다. 사용자→동시요청→TPS→AP→Pool→DB Session→장애 잔여량까지 **하나의 체인**.',
    3: 'CPU·메모리만으로 Thread·Pool·Timeout·장애 잔여량을 검증할 수 없습니다.',
    5: 'NSIGHT 1차 요구사항: 21,600명, p95 3초, 360/720/1080 TPS, 2센터 A-A, DB A-S.',
    7: '**변환 흐름**: 사용자→동시요청자→TPS→동시처리량→CPU/VM→Thread→Heap→Pool→Timeout→임계치.',
    15: '설정값은 **항목명·위치·단위·권고값·산식·검증방법**으로 표준 표현.',
    17: 'OS 튜닝은 JVM·Tomcat 한도 내에서 AP 안정성 확보가 목적.',
    25: 'NSIGHT TCF: ServiceId Timeout, GUID, Idempotency, 거래/감사 로그 표준.',
    28: 'DB max sessions ≥ 온라인 Pool + 배치 + BI + 운영 + 20% 여유.',
    30: 'JWT: Access/Refresh TTL, JWKS Cache, Rotation, DenyList.',
    41: '업무 유형별 Pool·Timeout·Bulkhead 차등: 조회/SV/등록/연계/배치.',
    43: '설정 계층: 공통 application.yml → profile → 업무 WAR override.',
    44: '설정 변경은 RACI에 따른 승인·검증·이력 필수.',
    46: '**안티패턴** 모음 — 운영 장애로 이어지는 대표 오설정.',
    48: '시험 합격 기준: CPU≤70%, Heap≤70%, p95≤3s, Pending=0, 오류율≤1%.',
    51: 'ENV rule-check: Timeout 순서, Pool 합산, Thread/Pool 비율 자동 검사.',
    53: '설정 변경 = 요청서 + 영향도 + 승인 + 시험 + 반영 + 이력.',
    54: 'Capacity Review: 일간 지표→주간 병목→월간 Baseline→분기 성장률.',
    55: 'Deprecated 설정 추적. 업그레이드 시 호환성 매트릭스 유지.',
    56: '핵심: 세션≠TPS, 잔여 TPS>평상시, Pool≠Thread, 안쪽 Timeout 짧게.',
    57: '최종 목적: **운영 가능한 안정성**. 권고값은 출발점, 실측으로 보정.',
  };
  const parts = [];
  if (intro[ch]) parts.push(intro[ch]);
  const byTitle = enrichByTitle(title, ch);
  if (byTitle) parts.push(byTitle);
  if (!parts.length) {
    parts.push(`NSIGHT 1차 표준(${STD.totalUsers.toLocaleString()}명·${STD.tps.base}/${STD.tps.peak}/${STD.tps.stress} TPS·${STD.vm}) 적용.`);
    parts.push(`관련 설정은 \`환경셋팅(최종).docx\` 및 \`znsight-capacity-word\` 원문 참조.`);
    parts.push('최종값은 선도개발·성능시험(360/720/1080) 실측으로 확정.');
  }
  return parts.join('\n\n') + '\n';
}

module.exports.enrichSection = function enrichSection(key, chapterNum, sectionTitle) {
  const sec = key.split('.')[1] || '';
  return enrichByChapter(chapterNum, sec, sectionTitle);
};
