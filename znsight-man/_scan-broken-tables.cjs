#!/usr/bin/env node
'use strict';
const fs = require('fs');
const path = require('path');
const { formatDocxMarkdown } = require('./format-docx-md.cjs');
const { buildDocxMaps } = require('./_docx-map.cjs');

const HEADER_RE =
  /^(구분|항목|영역|유형|Type|Scope|브랜치|목표|문제|원칙|Layer|계층|Package|No|순서|방식|목적|대상|역할|필드|Header|Module|처리|로그|테스트|점검|변경|Tag|Merge|업무|Context|식별자|오류|채널|FunctionCode|MDC|컬럼|Field|Method|활동|질문|단계|산출물|용어|Prefix|분류|검증|Gate|Function|Job|Screen|Menu|Cache|SQL|DTO|Handler|Facade|Service|Rule|DAO|Mapper|WAR|Gradle|Profile|Timeout|Session|JWT|Batch|파일|모듈|패키지|호출|금지|비교|좋은|나쁜|잘못된|설명|기준|의미|예시|Plugin|산출물|비고|Action|판단|요청|응답|TYPE|Git|RACI|시사점|효과|관리|확인|추적|공통|보안|거래|적용|원칙|대표|생성|삭제|보호|승인|변경 영역|변경 유형|Online|Endpoint|REST|Local|운영|로컬|개발|배포|롤백|Pipeline|JobId|Scheduler|OM|DevOps|리뷰|품질|장애|표준|형식|Key|Value|Name|Column|Table|Body|Resource|Controller|Filter|Filter|Level|Step|Status|State|Code|Category|Group|Class|Kind|Mode|Format|Pattern|Policy|Config|Setting|Option|Param|Input|Output|Source|Target|Path|Url|Port|Host|Env|Property|Feature|Requirement|Limit|Size|Count|Rate|Duration|Period|Range|Priority|Order|Seq|Token|Secret|Auth|Permission|Role|Enable|Disable|Active|Internal|External|Sync|Async|File|Dir|Encoding|Locale|Timezone|Language|Unit|Quality|Performance|Memory|CPU|Network|Thread|Task|Worker|Pool|Queue|Store|Repository|Entity|Model|VO|Command|Query|Criteria|Result|Entry|Audit|Trace|Metric|Health|Swagger|Aspect|Listener|Event|Message|Payload|Schema|Version|Release|Hotfix|Feature|Chore|Docs|Refactor|Perf|Security|Style|Fix|Feat|Test|Chore|업무코드|처리 유형|검증 유형|검증 대상|검증 위치|허용 여부|금지 Endpoint|권장 Endpoint|문제점|적용 Plugin|DTO 유형|대표 클래스|명명 기준|idempotencyKey 기준|거래 유형|대상 업무 WAR|Gateway 내부 라우팅|외부 호출|로컬 bootRun|운영 Tomcat WAR)$/;

const H2_RE =
  /^(설명|기준|의미|예시|역할|용도|비고|판단|확인|검증|대상|내용|사유|이유|문제|영향|효과|목적|위치|형식|표준|규칙|원칙|방식|유형|구분|항목|영역|필드|모듈|패키지|파일|Plugin|산출물|Merge 대상|삭제 여부|생성 기준|보호 수준|관리 기준|브랜치 표기|확인 목적|테스트 내용|주요 책임|개발 기준|작성 기준|표준 기준|SQL ID 예시|권장 행위명|처리유형|로그 항목|보안 영역|Cache 대상|테스트 유형|나쁜 예|좋은 Commit|나쁜 Commit|로컬 bootRun|운영 Tomcat WAR|로컬 기준|주의사항|사용 시점|설치 항목|확인 명령|정상 예시|Git Commit 가능 여부|핵심 기준|금지사항|점검 항목|확인|로컬 Port 예시|로컬 URL|대표 클래스|명명 기준|처리 유형|사용 예시|검증 위치|검증 대상|검증 유형|허용 여부|금지 Endpoint|권장 Endpoint|문제점|적용 Plugin|적용 기준|DTO 유형|Action|idempotencyKey 기준|거래 유형|대상 업무 WAR|Gateway 내부 라우팅|외부 호출|적용 기준|역할|비고|예시|의미|기준|설명|용도|Plugin|산출물|대상|판단|확인|검증|내용|사유|형식|표준|규칙|원칙|방식|유형|구분|항목|영역|필드|모듈|패키지|파일)$/;

function isBrokenPair(a, b) {
  a = a.trim();
  b = b.trim();
  if (!a || !b) return false;
  if (a.startsWith('|') || b.startsWith('|') || a.startsWith('#') || a.startsWith('```')) return false;
  if (/^[a-z.]+\(|^git |^POST |^export |^spring:|^-\s|^\{|^\/\//.test(a)) return false;
  if (/^[a-z.]+\(|^git |^POST |^export |^spring:|^-\s|^\{/.test(b)) return false;
  if (a.length > 35 || b.length > 35) return false;
  if (/[다요]\.$/.test(a) || /[다요]\.$/.test(b)) return false;
  return HEADER_RE.test(a) || H2_RE.test(b);
}

function scanMd(md) {
  const lines = md.split(/\r?\n/);
  const blocks = [];
  for (let i = 0; i < lines.length - 1; i++) {
    const a = lines[i];
    const b = lines[i + 1];
    if (!isBrokenPair(a, b)) continue;
    const headers = [a.trim(), b.trim()];
    let j = i + 2;
    while (j + 1 < lines.length && lines[j].trim() && !lines[j].trim().startsWith('|') && !lines[j].trim().startsWith('#')) {
      j++;
    }
    blocks.push({ line: i + 1, headers, rowLines: j - i - 2 });
    i = j - 1;
  }
  return blocks;
}

function extractHeaderPattern(block, lines, startIdx) {
  const headers = [];
  let i = startIdx;
  while (i < lines.length && headers.length < 10) {
    const t = lines[i].trim();
    if (!t || isSection(t) || /[다요]\.$/.test(t)) break;
    if (t.length > 40) break;
    if (headers.length >= 2 && /^\d+$/.test(t)) break;
    if (headers.length >= 2 && /^[a-z.]+\(|^git |^POST /.test(t)) break;
    headers.push(t);
    i++;
    if (headers.length >= 2) {
      const next = lines[i]?.trim() ?? '';
      if (next && next.length <= 40 && !/[다요]\.$/.test(next) && headers.length < 10) {
        // peek if 3rd is also header-like
        const n2 = lines[i + 1]?.trim() ?? '';
        if (H2_RE.test(next) || HEADER_RE.test(next) || (next.length <= 22 && !/^\d+$/.test(next))) {
          continue;
        }
      }
      break;
    }
  }
  return headers;
}

function isSection(t) {
  return /^(\d+\.)+\s+\S/.test(t) || /^\d+\.\d+\s+\S/.test(t);
}

const { byChapter: DOCX_BY_CHAPTER } = buildDocxMaps();
const outDir = __dirname;
const files = fs
  .readdirSync(outDir)
  .filter((f) => /^(0[89]|[1-7][0-9])-.*\.md$/.test(f))
  .sort((a, b) => parseInt(a, 10) - parseInt(b, 10));

const patternCounts = new Map();
const byChapter = [];

for (const f of files) {
  const n = parseInt(f, 10);
  const docxN = DOCX_BY_CHAPTER[n];
  if (!docxN) {
    byChapter.push({ n, f, blocks: [], note: 'no docx' });
    continue;
  }
  const raw = fs.readFileSync(path.join(outDir, '_docx-cache', `docx-${docxN}.txt`), 'utf8');
  const md = formatDocxMarkdown(raw);
  const blocks = scanMd(md);
  byChapter.push({ n, f, blocks: blocks.length, samples: blocks.slice(0, 5) });

  const rawLines = raw.split(/\r?\n/);
  for (let i = 0; i < rawLines.length; i++) {
    const t = rawLines[i].trim();
    if (!HEADER_RE.test(t) && !H2_RE.test(t)) continue;
    const h2 = rawLines[i + 1]?.trim() ?? '';
    if (!h2 || h2.length > 40) continue;
    const pat = JSON.stringify([t, h2]);
    if (!patternCounts.has(pat)) patternCounts.set(pat, { headers: [t, h2], count: 0, chapters: new Set() });
    const p = patternCounts.get(pat);
    p.count++;
    p.chapters.add(n);
  }
}

console.log('=== Chapters with most broken blocks (after format) ===');
byChapter
  .filter((x) => x.blocks > 0)
  .sort((a, b) => b.blocks - a.blocks)
  .slice(0, 30)
  .forEach((x) => {
    console.log(`${String(x.n).padStart(2)} ${x.f}: ${x.blocks}`);
    x.samples?.forEach((s) => console.log(`    L${s.line}: ${s.headers.join(' | ')} (+${s.rowLines} rows)`));
  });

console.log('\n=== Top 2-col header patterns in docx (ch 8-78) ===');
[...patternCounts.values()]
  .filter((p) => p.chapters.size >= 1 && [...p.chapters].some((c) => c >= 8))
  .sort((a, b) => b.count - a.count)
  .slice(0, 40)
  .forEach((p) => console.log(`${p.count}x ch[${[...p.chapters].join(',')}] ${p.headers.join(' | ')}`));

console.log('\nTotal chapters scanned:', files.length);
console.log('Chapters with broken blocks:', byChapter.filter((x) => x.blocks > 0).length);
