const fs = require('fs');
const path = require('path');
const { loadExtract, convertDocxTextToMarkdown, extractRawSlice } = require('./docx-to-md');
const { CHAPTER_SOURCES, APPENDIX_SOURCES } = require('./chapter-sources');
const { getSectionText, SECTION_OVERRIDES } = require('./section-content');
const { buildChapterSupplement } = require('./section-rich-builder');
const { buildAppendixRich } = require('./appendix-rich-builder');
const { buildAppendixSections } = require('./appendix-sections');

const BOOK = path.join(__dirname, '..');
const TOC = path.join(BOOK, '_tmp-toc', 'toc-lines.txt');

const COMMON = `## NSIGHT 1차 표준 전제

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

> 출처: \`znsight-capacity-word\` · [13단계 요약](./zNSIGHT-용량산정-전체-흐름.md)
`;

const CHAPTER_INTRO = {
  1: '용량산정은 **서버 대수만 계산하는 작업이 아닙니다.** 사용자 수부터 장애 시 잔여 처리량까지 하나의 체인으로 연결해야 합니다.',
  2: '본 가이드의 **목적·범위·독자·참조 자료** — 산정과 환경설정을 하나의 문서 체인으로 관리합니다.',
  3: '**문제 정의** — CPU·메모리만으로는 Thread·Pool·Timeout·장애 잔여량을 검증할 수 없습니다.',
  4: '**현행 구조 점검** — GSLB~DB 구성과 Tomcat·JVM·Pool·Timeout 현행값의 불일치를 식별합니다.',
  5: '**요구사항·제약** — 21,600명, p95 3초, 360/720/1080 TPS, 2센터 A-A, DB Session 상한.',
  6: '**설계 원칙** 12가지 — 피크 기준, 세션≠TPS, Thread≠Pool, Timeout 계층, 시험으로 확정.',
  7: '산정 결과를 Tomcat·JVM·Pool·Timeout·임계치로 변환하는 **연결 모델**.',
  8: '**사용자·지점·세션·동시요청** — TPS 산정의 출발점.',
  9: '**TPS·동시처리량** — 360 / 720 / 1,080 시나리오.',
  10: '**TPMC·Core 교차검증** — 실효 30~40 TPS/Core.',
  11: '**VM·서버 대수** — 8CORE Scale-Out, 센터 장애 N+1.',
  12: '**메모리·스토리지** — Heap·OS·GC Log·Dump·로그 디스크 산정.',
  13: '**네트워크·Connection** — L4·Apache·Tomcat maxConnections 정합.',
  14: '**환경설정값 분류체계** — 인프라·WAS·프레임워크·DB·보안·모니터링.',
  15: '**설정값 표준 표현** — 항목명·위치·단위·권고값·산식·검증방법.',
  16: '**Timeout 계층** — DB → Pool → Tx → Proxy → Web → L4.',
  17: '**OS/VM** — ulimit, TCP, 로그·Dump 경로.',
  18: '**GSLB** — TTL, Health, 센터 라우팅, 장애 전환.',
  19: '**L4** — VIP, Pool, Sticky, Health, Drain.',
  20: '**Apache/Proxy** — Worker, ProxyTimeout, KeepAlive, GUID Header.',
  21: '**Gateway** — Route Timeout, CB, Bulkhead, Rate Limit.',
  22: '**Tomcat Connector** — Thread·Connection·Session·Cluster.',
  23: '**JVM** — Heap·G1GC·GC Log·OOM.',
  24: '**Spring Boot** — Transaction·Graceful·Actuator·Async.',
  25: '**NSIGHT TCF** — ServiceId Timeout, GUID, Idempotency.',
  26: '**HikariCP** — Pool 산정 4단계 공식.',
  27: '**MyBatis·SQL** — statement-timeout, fetch-size, Slow SQL.',
  28: '**DBMS** — max sessions, Lock/Wait Timeout.',
  29: '**세션** — Idle/Absolute, DeltaManager, Cookie.',
  30: '**JWT** — Access/Refresh TTL, JWKS Cache.',
  31: '**Cache** — Local/분산, TTL, Stampede 방지.',
  32: '**외부연계 HTTP Client** — Connect/Read, CB, Retry 금지.',
  33: '**Batch·Scheduler** — Window, Pool 분리, 중복 방지.',
  34: '**Kafka·CDC** — Lag, DLQ, Partition, 온라인 SLA 분리.',
  35: '**파일 업로드/다운로드** — Multipart, Streaming, 전용 서버.',
  36: '**로그·감사·관측성** — GUID, MDC, Access Log, APM.',
  37: '**8Core·32GB 표준 프로파일** — 환경설정 최종 기준안.',
  38: '**16Core·64GB 프로파일** — 특수·고부하 검토용.',
  39: '**16Core·128GB 프로파일** — Heap·Cache 여유, 선도검증 필수.',
  40: '**32Core·256GB 프로파일** — 배치·ETL·특수 AP.',
  41: '**업무 유형별** — 조회/SV/연계/배치 프로파일.',
  42: '**목표 아키텍처** — 2센터 A-A, 업무별 VM, Pool 분리.',
  43: '**설정 파일·배포 구조** — server.xml, yml, setenv.sh 계층.',
  44: '**RACI** — 설정 변경 책임·승인·검증 경계.',
  45: '**정상 적용 예시** — 720 TPS end-to-end.',
  46: '**금지 예시** — 운영 장애로 이어지는 오설정.',
  47: '**성능시험 계획** — 360/720/1080·장애 시나리오.',
  48: '**검증 기준** — CPU/Heap/Thread/Pool/TPS/p95.',
  49: '**운영 임계치·모니터링** — Warning/Critical, Pending 추세.',
  50: '**장애·Timeout·자원고갈** — 증상·원인·조치.',
  51: '**자동검증 Gate** — rule-check, Pool 합산.',
  52: '**운영 전환 체크리스트** — 시험·Runbook·Rollback.',
  53: '**변경관리** — 요청·승인·시험·이력.',
  54: '**Capacity Review** — 일간·주간·월간 Baseline.',
  55: '**호환성·폐기 관리** — Deprecated, 업그레이드 매트릭스.',
  56: '**시사점** — 핵심 판단, 위험, 우선 보완 과제.',
  57: '**마무리말** — 운영 가능한 안정성, 지속적 Capacity Management.',
};

function slugify(t) {
  return t.replace(/^\d+\.\s*/, '').replace(/[·/\\:*?"<>|]/g, '-').replace(/\s+/g, '-').replace(/-+/g, '-').replace(/^-|-$/g, '');
}

function parseToc(lines) {
  let part = null, cur = null;
  const chapters = {}, appendices = {};
  for (const raw of lines) {
    const line = raw.trim();
    if (!line) continue;
    if (line.startsWith('제') && line.includes('부.')) { part = line; continue; }
    let m = line.match(/^(\d+)\.\s+(.+)$/);
    if (m && !line.match(/^\d+\.\d+/)) {
      const n = +m[1];
      cur = { num: n, title: m[2], part, sections: [] };
      chapters[n] = cur;
      continue;
    }
    m = line.match(/^(\d+)\.(\d+)\s+(.+)$/);
    if (m && cur && +m[1] === cur.num) {
      cur.sections.push({ num: `${m[1]}.${m[2]}`, title: m[3] });
      continue;
    }
    m = line.match(/^([A-Z]{1,2})\.\s+(.+)$/);
    if (m) appendices[m[1]] = m[2];
  }
  return { chapters, appendices };
}

function buildSourceBody(chapterNum) {
  const cfg = CHAPTER_SOURCES[chapterNum];
  if (!cfg) return null;

  const parts = [];
  for (const f of cfg.files) {
    const raw = loadExtract(f.name);
    if (!raw) continue;
    let text = raw;
    if (f.slice) {
      text = extractRawSlice(raw, f.slice[0], f.slice[1]);
    }
    const md = convertDocxTextToMarkdown(text, null, f.name);
    if (md.trim()) parts.push(md.trim());
  }
  return parts.length ? parts.join('\n\n---\n\n') : null;
}

function buildChapterBody(ch, sourceBody) {
  const lines = [];
  lines.push(`# ${ch.num}. ${ch.title}`, '');
  if (ch.part) lines.push(`> ${ch.part}`, '');
  lines.push(COMMON, '');
  if (CHAPTER_INTRO[ch.num]) lines.push(CHAPTER_INTRO[ch.num], '');

  // 원문 본문 (전체)
  if (sourceBody) {
    lines.push('## 원문 기반 본문', '', sourceBody, '');
  }

  // 목차 전 절 심화 본문 (항상 전체 출력)
  if (ch.sections.length) {
    lines.push('## 절별 상세', '');
    for (const s of ch.sections) {
      lines.push(`### ${s.num} ${s.title}`, '', getSectionText(s.num, ch.num, s.title));
    }
    if (ch.sections.length < 12 || [1, 56, 57].includes(ch.num)) {
      lines.push(buildChapterSupplement(ch.num, ch.title), '');
    }
  }

  lines.push('---', '', '[← 목차](./00-목차.md)', '');
  return lines.join('\n');
}

function buildAppendix(key, title) {
  const lines = [
    `# 부록 ${key}. ${title}`,
    '',
    '> 원본: `znsight-capacity-word` · 23장 수준 템플릿 확장',
    '',
    COMMON.replace('./zNSIGHT', '../zNSIGHT').replace('./00-목차', '../00-목차'),
    '',
  ];

  const srcFile = APPENDIX_SOURCES[key];
  if (srcFile) {
    const raw = loadExtract(srcFile);
    if (raw) {
      let md = convertDocxTextToMarkdown(raw, null, srcFile);
      if (md.length > 15000) {
        md = md.slice(0, 15000) + '\n\n> *(원문 일부 발췌 — 전체는 znsight-capacity-word 참조)*\n';
      }
      if (md.trim()) {
        lines.push('## 원문 기반 본문', '', md.trim(), '');
      }
    }
  }

  lines.push(buildAppendixRich(key, title), '');
  lines.push(buildAppendixSections(key, title), '');
  lines.push('---', '', '[← 목차](../00-목차.md)', '');
  return lines.join('\n');
}

// === main ===
const lines = fs.readFileSync(TOC, 'utf8').split(/\r?\n/);
const { chapters, appendices } = parseToc(lines);

const index = [
  '# NSIGHT 통합 용량산정 및 솔루션 환경설정 가이드 — 목차',
  '',
  '> 업무부하 산정에서 운영 설정·검증·변경관리까지',
  '> 원본: `znsight-capacity-word` (원문 수준 확장)',
  '',
  COMMON,
  '',
  '## 빠른 참조',
  '',
  '- [zNSIGHT-용량산정-전체-흐름.md](./zNSIGHT-용량산정-전체-흐름.md) — 13단계 요약',
  '',
  '## 본문',
  '',
];

let lastPart = '';
let sourceCount = 0;
for (const n of Object.keys(chapters).map(Number).sort((a, b) => a - b)) {
  const ch = chapters[n];
  if (ch.part && ch.part !== lastPart) {
    index.push(`### ${ch.part}`, '');
    lastPart = ch.part;
  }
  const fn = `${String(n).padStart(2, '0')}-${slugify(ch.title)}.md`;
  index.push(`- [${n}. ${ch.title}](./${fn})`);

  const sourceBody = buildSourceBody(n);
  if (sourceBody) sourceCount++;
  const body = buildChapterBody(ch, sourceBody);
  fs.writeFileSync(path.join(BOOK, fn), body, 'utf8');
}

index.push('', '## 부록', '');
const appDir = path.join(BOOK, '부록');
fs.mkdirSync(appDir, { recursive: true });
for (const k of Object.keys(appendices)) {
  const fn = `${k}-${slugify(appendices[k])}.md`;
  index.push(`- [${k}. ${appendices[k]}](./부록/${fn})`);
  fs.writeFileSync(path.join(appDir, fn), buildAppendix(k, appendices[k]), 'utf8');
}

fs.writeFileSync(path.join(BOOK, '00-목차.md'), index.join('\n') + '\n', 'utf8');
fs.writeFileSync(
  path.join(BOOK, 'README.md'),
  `# NSIGHT 통합 용량산정 및 솔루션 환경설정 가이드

목차 docx 기준 **${Object.keys(chapters).length}개 챕터 + ${Object.keys(appendices).length}개 부록** (23장 수준 절별·템플릿 확장).

| 문서 | 설명 |
|------|------|
| [00-목차.md](./00-목차.md) | 전체 목차 |
| [zNSIGHT-용량산정-전체-흐름.md](./zNSIGHT-용량산정-전체-흐름.md) | 13단계 요약 |

- **57개 챕터** — 원문 + 절별 상세(권고값·설정·검증)
- **30개 부록** — 원문 + 실무 템플릿 + 절별 상세

원본: \`znsight-capacity-word/\`

\`\`\`bash
node _scripts/generate-chapters.js
\`\`\`
`,
  'utf8'
);

console.log(`OK: ${Object.keys(chapters).length} chapters (${sourceCount} with full source), ${Object.keys(appendices).length} appendices`);
