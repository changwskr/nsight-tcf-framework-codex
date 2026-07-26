const { buildCap010MappingTable, buildEnv002MappingTable, buildAppendixAUnifiedTable } = require('./cap-field-mapping');

function verify() {
  return `| 시나리오 | 합격 기준 |
|----------|----------|
| 360 TPS | p95≤3s, CPU≤70% |
| 720 TPS | Thread/Pool 고갈 없음 |
| 1,080 TPS | Fail-fast 정상 |
| 센터 장애 | 잔여 TPS≥720 |`;
}

/** 부록별 절 목록 */
const APPENDIX_SECTIONS = {
  A: ['입력 항목 정의', '산출 항목', 'CAP 화면 매핑', '검증·판정', '작성 예시'],
  B: ['사용자·세션', '동시요청·TPS', 'AP·Pool 연계', '시나리오별 표', '검증'],
  C: ['TPMC 개념', 'Core 실효 TPS', 'VM 교차검증', 'CPU 이용률', '시험 보정'],
  D: ['8C vs 16C 비교', '장애 영향', 'Scale-Out 근거', '배치·온라인 구분', '결론'],
  E: ['양식 필드', '가정/권고/확정', '문서화 규칙', '변경 이력', '검증'],
  F: ['8C/32G 프로파일', '16C 프로파일', '32C 프로파일', '업무별 차등', '적용'],
  G: ['Connector 기본', 'Thread·Queue', 'KeepAlive', '내장 Tomcat', '검증'],
  H: ['Heap·GC', 'Stack·Metaspace', 'GC Log', 'OOM·Dump', '검증'],
  I: ['Profile·Graceful', 'Datasource·Tx', 'MyBatis', 'Actuator', '검증'],
  J: ['ServiceId Timeout', 'GUID·MDC', 'Idempotency', 'CruzAPIM', '검증'],
  K: ['산정 공식', '8C/250TPS 예시', 'SV 예시', 'Slow SQL 판단', '검증'],
  L: ['Statement Timeout', 'Fetch Size', 'Executor', 'Slow SQL', '검증'],
  M: ['DB·Pool', 'Transaction', 'Proxy·Web', 'L4', 'rule-check'],
  N: ['GSLB', 'L4 VIP', 'Sticky·Health', 'Apache', '검증'],
  O: ['Route Timeout', 'Circuit Breaker', 'Rate Limit', 'Bulkhead', '검증'],
  P: ['Session', 'JWT Access', 'JWT Refresh', 'JWKS', '검증'],
  Q: ['Local Cache', 'Redis Cache', 'TTL·Eviction', 'Stampede', '검증'],
  R: ['Connect·Read', 'Circuit Breaker', 'Bulkhead', 'Retry 정책', '검증'],
  S: ['Scheduler', 'Batch Window', 'Chunk', 'Pool 분리', '검증'],
  T: ['Consumer', 'Lag·DLQ', 'Partition', '온라인 분리', '검증'],
  U: ['Access Log', 'GC Log', 'APM·MDC', '임계치', '검증'],
  V: ['온라인 Pool 합산', '배치·BI', 'DB max sessions', '판정', '검증'],
  W: ['센터당 AP', '잔여 TPS', 'GSLB 전환', 'Sticky 영향', '검증'],
  X: ['360 TPS', '720 TPS', '1080 TPS', '장애 시나리오', '합격 기준'],
  Y: ['CPU·Heap', 'Thread·Pool', 'GC·p95', '오류율', '조치'],
  Z: ['시험 항목', '실측 기록', '판정', '운영 전환', '이력'],
  AA: ['산정·설정', '시험·rule-check', 'Runbook', '배포·Rollback', 'Go-Live'],
  AB: ['변경 요청', '영향도', '승인·검증', '반영·이력', 'Rollback'],
  AC: ['ADR 템플릿', '8C 결정', 'A-A 결정', 'Pool·GC 결정', '이력'],
  AD: ['720 TPS 결과', '설정 확정', 'DB Session', '장애 수용', '승인'],
};

const SECTION_BODY = {
  '입력 항목 정의': `${buildAppendixAUnifiedTable()}\n\n핵심 필수: branchCount, userPerBranch, concurrentRequestRates, targetResponseTimes, vmSpecCode/vmProfileId.`,
  '산출 항목': '| 산출 | DTO 필드 | 산식 | 단위 |\n|------|----------|------|------|\n| totalUsers | totalUsers | branch×user | 명 |\n| designedSessions | designedSessions | total×(1+margin) | 세션 |\n| tps | scenarioResults | concurrent÷응답 | TPS |\n| apCount | cap030 | TPS÷vmTps×A-A | 대 |',
  'CAP 화면 매핑': `#### CAP-010 (\`/oc/capacity.html\`)\n\n${buildCap010MappingTable()}\n\n#### ENV-002 (\`/oc/env-002.html\`)\n\n${buildEnv002MappingTable()}`,
  'TPMC 개념': '벤치마크 60 TPMC/TPS ≠ 정보계. NSIGHT: **1,500~3,500 TPMC/TPS**.',
  'Core 실효 TPS': '| Core | 이론 | NSIGHT 실효 |\n|------|------|-------------|\n| 8 | 참고만 | **30~40 TPS/Core** |\n| 8 VM | — | **250 TPS** |',
  '8C vs 16C 비교': '| 항목 | 8C×8 | 16C×4 |\n|------|------|-------|\n| 장애 영향 | 12.5% | 25% |\n| Rolling | 용이 | 부담 |\n| DB Pool | 작은 단위 | 큰 단위 |',
  '산정 공식': '```\nPool = max(30, min(TPS×0.15×1.3, Thread×30%))\n센터 Pool = Σ(AP×Pool) ≤ DB max\n```',
  '8C/250TPS 예시': '| 항목 | 값 |\n|------|----|\n| AP TPS | 250 |\n| DB hold | 0.15s |\n| Pool | **50** |\n| Thread 상한 | 108 |',
  'ServiceId Timeout': '| ServiceId | Timeout |\n|-----------|--------|\n| 조회 | 4s |\n| 등록 | 5s |\n| 연계 | 5s |',
  'GUID·MDC': 'GUID·TraceId **MDC 필수**. Access Log·APM·거래로그 상관관계.',
  'rule-check': 'ENV `/api/oc/env/rule-check`: Timeout 순서·Pool 합산·Thread/Pool 비율.',
  '720 TPS 결과': '| 항목 | 확정값 |\n|------|--------|\n| TPS | 720 |\n| AP | 8 |\n| Pool/VM | 50 |\n| DB Session | 400 |',
};

function sectionContent(key, sectionTitle, appendixTitle) {
  const custom = SECTION_BODY[sectionTitle];
  const lines = [
    `본 절은 **부록 ${key}** — **${sectionTitle}** (${appendixTitle}) NSIGHT 1차 표준 적용 기준입니다.`,
    '',
  ];
  if (custom) lines.push(custom, '');
  else {
    lines.push(
      `| 항목 | 권고 | 비고 |`,
      `|------|------|------|`,
      `| 기준 VM | 8 vCPU / 32GB | Scale-Out |`,
      `| TPS | 360/720/1080 | 피크 720 |`,
      `| p95 | 3초 이하 | SLA |`,
      `| 검증 | 360/720/1080 | 성능시험 |`,
      '',
    );
  }
  lines.push('#### 검증 기준', '', verify(), '');
  lines.push('#### 주의사항', '', '- 권고값은 출발점 — 선도개발·시험 후 확정', '- 세션≠TPS, Pool≠Thread', '');
  return lines.join('\n');
}

function buildAppendixSections(key, title) {
  const sections = APPENDIX_SECTIONS[key] || ['기본 템플릿', '권고값', '설정 예시', '검증 기준', '운영 참고'];
  const lines = ['## 절별 상세', ''];
  sections.forEach((sec, i) => {
    lines.push(`### ${key}.${i + 1} ${sec}`, '');
    lines.push(sectionContent(key, sec, title));
  });
  return lines.join('\n');
}

module.exports = { buildAppendixSections, APPENDIX_SECTIONS };
