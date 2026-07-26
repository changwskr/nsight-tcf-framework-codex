#!/usr/bin/env node
'use strict';

const fs = require('fs');
const path = require('path');

const HEADER_WORDS = new Set([
  '구분', '항목', '번호', '업무', '업무코드', 'ServiceId', '설명', '예시', '원칙', '필드', 'Layer', '계층',
  '역할', '책임', '대상', '유형', '상태', '값', '기준', '방식', 'Module', 'Package', 'Context', 'WAR',
  'Gradle', '처리', '검증', '단계', '확인', '조치', '증상', '원인', '개발', 'Method', 'Endpoint', 'Handler',
  'Facade', 'Service', 'Rule', 'DAO', '오류', '로그', '환경', '브랜치', '작업', 'Gate', 'Type', 'Scope',
  '변경', 'Merge', 'Plugin', '파일', '패키지', '모듈', 'Tag', 'Claim', 'Token', 'Level', 'Timeout', 'Session',
  'JWT', 'Batch', 'Profile', 'Secret', 'Cache', 'SQL', 'DTO', 'Mapper', 'No', '순서', '목적', '영역', 'Prefix',
  '금지', '호출', '비교', '좋은', '나쁜', '잘못된', '판단', '용도', '산출물', '비고', 'Action', 'DB', '설정',
  'Header', '필드', 'Health', '로그', '배치', '운영', '개발', '단계', 'WAR', '거래', '오류코드', '테스트',
  '대상', '금지', '표준', '형식', 'Key', '컬럼', '파라미터', '태그', '증상', '장점', 'Rollback', 'bootRun',
  'Tomcat', 'Local', 'Dev', 'STG', 'PRD', 'Hotfix', '충돌', '변경', 'OM', '도메인', '실행', '필요',
  '개발', '작업', '운영', '항목', 'Health Check', 'Cache', '대상', 'Timeout', '구분', '유형', '나쁜 예',
  '문제', '좋은 예', '대상', '테스트', '클래스', 'OM', '도메인', '업무', 'Context Path', 'Endpoint', 'WAR',
  'ServiceId', '거래코드', '설명', '로그', '배치', '방식', '장점', '주의', 'Header', 'businessCode', 'URL',
  '금지', '항목', '나쁜', '표준', 'Scope', '변경', '영역', '승인', '브랜치', '보호', 'Type', 'Merge',
  '충돌', '유형', '해결', '변경', '대상', '기준', 'Handler', 'Facade', 'Service', 'DAO', 'Mapper', 'SQL',
  '오류코드', '점검', '확인', '금지사항', '사유', '리뷰', '품질', 'Gate', 'Blocker', '증상', '가능', '원인',
]);

function isHeaderCell(line) {
  const t = line.trim();
  if (!t || t.length > 45) return false;
  if (t.startsWith('|') || t.startsWith('#') || t.startsWith('```')) return false;
  if (/^[a-z.]+\(|^git |^POST |^export |^spring:|^-\s|^\{|^\/\//.test(t)) return false;
  if (/[다요]\.$/.test(t) && t.length > 20) return false;
  return HEADER_WORDS.has(t) || /^[A-Za-z][A-Za-z\s]{0,22}$/.test(t) || /^[가-힣A-Za-z\s]{2,18}$/.test(t);
}

function isTableLine(line) {
  return line.trim().startsWith('|');
}

function isBlockBoundary(line) {
  const t = line.trim();
  return !t || t.startsWith('#') || t.startsWith('```') || t.startsWith('>') || t.startsWith('---');
}

/** ``` fenced 블록 내부는 후처리하지 않음 */
function repairOutsideCodeBlocks(text, fn) {
  const parts = text.split(/(```[\s\S]*?```)/g);
  return parts.map((part) => (part.startsWith('```') ? part : fn(part))).join('');
}

function mdTable(headers, rows) {
  const esc = (s) => String(s ?? '').replace(/\|/g, '\\|').replace(/\n/g, ' ').trim();
  const sep = headers.map(() => '---');
  return [
    `| ${headers.map(esc).join(' | ')} |`,
    `| ${sep.join(' | ')} |`,
    ...rows.map((r) => `| ${r.map(esc).join(' | ')} |`),
  ].join('\n');
}

/** 표 밖 key\nvalue\nkey\nvalue 패턴 → Markdown 표 */
function repairPlainTextTables(text) {
  const lines = text.split(/\r?\n/);
  const out = [];
  let i = 0;

  while (i < lines.length) {
    const line = lines[i];
    if (isBlockBoundary(line) || isTableLine(line)) {
      out.push(line);
      i++;
      continue;
    }

    const h0 = line.trim();
    const h1 = lines[i + 1]?.trim() ?? '';
    if (isHeaderCell(h0) && isHeaderCell(h1) && !isTableLine(lines[i + 1])) {
      const headers = [h0, h1];
      let j = i + 2;
      let h2 = lines[j]?.trim() ?? '';
      if (isHeaderCell(h2) && !isTableLine(lines[j]) && headers.length < 6) {
        headers.push(h2);
        j++;
      }
      const cols = headers.length;
      const rows = [];
      while (j + cols <= lines.length) {
        const slice = lines.slice(j, j + cols).map((l) => l.trim());
        if (slice.some((c) => !c)) break;
        if (slice.some((c) => isBlockBoundary(c) || isTableLine(c))) break;
        if (slice.some((c) => isHeaderCell(c) && rows.length > 0)) {
          const maybeNext = lines[j + cols]?.trim() ?? '';
          if (isHeaderCell(maybeNext)) break;
        }
        if (slice[0].length > 120 || slice.some((c) => c.length > 200)) break;
        if (/^(public |@|plugins \{|import |spring:)/.test(slice[0])) break;
        rows.push(slice);
        j += cols;
      }
      if (rows.length >= 1) {
        if (out.length && out[out.length - 1] !== '') out.push('');
        out.push(mdTable(headers, rows), '');
        i = j;
        continue;
      }
    }

    out.push(line);
    i++;
  }
  return out.join('\n');
}

const CODE_ROW = /^(public |private |protected |@RequiredArgsConstructor|@Component|@Service|@Repository|@Mapper|@Override|@ExtendWith|@Transactional|@Getter|@Setter|plugins \{|dependencies \{|implements |public class |class |<\?xml|<mapper|<select|void |return |if \(|for \(|import |package |\}|@Component\()/;

/** 표 셀에 코드가 섞인 행 → 코드 블록으로 분리 */
function repairCodeInTables(text) {
  const lines = text.split(/\r?\n/);
  const out = [];
  let i = 0;

  while (i < lines.length) {
    if (!isTableLine(lines[i])) {
      out.push(lines[i]);
      i++;
      continue;
    }

    const tableLines = [];
    while (i < lines.length && isTableLine(lines[i])) {
      tableLines.push(lines[i]);
      i++;
    }

    if (tableLines.length < 2) {
      out.push(...tableLines);
      continue;
    }

    const header = tableLines[0];
    const sep = tableLines[1];
    const dataRows = tableLines.slice(2);
    const goodRows = [];
    const codeBuf = [];

    for (const row of dataRows) {
      const cells = row.split('|').slice(1, -1).map((c) => c.trim());
      const codeCells = cells.filter((c) => c && CODE_ROW.test(c));
      const isCode =
        codeCells.length >= 1 &&
        (codeCells.length >= 2 ||
          cells.every((c) => !c || CODE_ROW.test(c)) ||
          cells.some((c) => /^(public |@|class )/.test(c)));
      if (isCode) {
        for (const c of cells) {
          if (c) codeBuf.push(c);
        }
      } else {
        goodRows.push(row);
      }
    }

    out.push(header, sep, ...goodRows);
    if (codeBuf.length) {
      if (out.length && out[out.length - 1] !== '') out.push('');
      const lang = codeBuf.some((l) => /^plugins \{|^dependencies \{/.test(l)) ? 'gradle'
        : codeBuf.some((l) => /^<mapper|^<select|^<\?xml/.test(l)) ? 'xml'
        : codeBuf.some((l) => /^(spring:|server:|mybatis:)/.test(l)) ? 'yaml'
        : 'java';
      out.push('```' + lang, ...codeBuf.map((l) => l.replace(/\\`/g, '`')), '```', '');
    }
  }
  return out.join('\n');
}

/** 2열 표에 '설명'이 1행 1열로 들어간 3열 표 복원 */
function repairShiftedDescriptionColumn(text) {
  let out = text.replace(
    /(\| 항목 \| 기준 \|\n\| --- \| --- \|\n)\| 설명 \| ([^\n|]+) \| ([^\n|]+) \|\n((?:\| [^\n]+\|\n)+)/g,
    (all, head, k1, k2, rest) => {
      const rows = [`| ${k1} | ${k2} | |`];
      for (const line of rest.trim().split('\n')) {
        const m = line.match(/^\| ([^|]+) \| ([^|]+) \|(?: ([^|]+) \|)?$/);
        if (!m) continue;
        if (m[3] !== undefined) rows.push(`| ${m[1].trim()} | ${m[2].trim()} | ${m[3].trim()} |`);
        else rows.push(`| ${m[1].trim()} | ${m[2].trim()} | |`);
      }
      return '| 항목 | 기준 | 설명 |\n| --- | --- | --- |\n' + rows.join('\n') + '\n';
    }
  );
  out = out.replace(
    /(\| 기준 \| 설명 \|\n\| --- \| --- \|\n)((?:\| [^\n]+\|\n)+)/g,
    (all, head, body) => {
      const fixed = body.replace(/^\| ([^|]{25,}?) \| 기준 \| 설명 \|$/gm, (m, prose) => {
        return prose.trim() + '\n\n| 기준 | 설명 |\n| --- | --- |';
      });
      return head + fixed;
    }
  );
  return out;
}

/** 표 1열에 본문 문장이 들어간 행 제거 */
function removeProseTableRows(text) {
  const lines = text.split(/\r?\n/);
  const out = [];
  for (let i = 0; i < lines.length; i++) {
    const line = lines[i];
    if (isTableLine(line)) {
      const cells = line.split('|').slice(1, -1).map((c) => c.trim());
      const proseIdx = cells.findIndex(
        (c) =>
          (/[다요]\.$/.test(c) && c.length > 18 && !/^[A-Z]-/.test(c)) ||
          /예시는 다음|기준은 다음|구조는 다음|순서는 다음/.test(c)
      );
      if (proseIdx >= 0) {
        out.push(cells[proseIdx], '');
        const rest = cells.filter((c, idx) => idx !== proseIdx && c);
        if (rest.length >= 2) out.push(`| ${rest.join(' | ')} |`);
        else if (rest.length === 1) out.push(`| ${rest[0]} | |`);
        continue;
      }
      if (cells.length && /작성 기준은 다음|예시는 다음|다음과 같다/.test(cells[0]) && cells[0].length > 18) {
        out.push(cells[0], '');
        const rest = cells.slice(1);
        if (rest.length >= 2 && rest[rest.length - 2] === '기준' && rest[rest.length - 1] === '설명') {
          continue;
        }
        if (rest.length === 2 && rest[0] === '기준' && rest[1] === '설명') {
          continue;
        }
        continue;
      }
      if (cells.length === 3 && cells[1] === '기준' && cells[2] === '설명') {
        out.push(cells[0], '', '| 기준 | 설명 |', '| --- | --- |');
        continue;
      }
    }
    out.push(line);
  }
  return out.join('\n');
}

/** 계층 흐름도가 잘못 표로 변환된 경우 복원 */
function repairFlowLayerMisTables(text) {
  const FLOW = new Set(['Handler', 'Facade', 'Service', 'Rule', 'DAO', 'Mapper', 'MyBatis XML', '화면', 'DB']);
  const lines = text.split(/\r?\n/);
  const out = [];
  for (let i = 0; i < lines.length; i++) {
    if (isTableLine(lines[i]) && lines[i + 1]?.includes('---') && isTableLine(lines[i + 2])) {
      const h = lines[i].split('|').slice(1, -1).map((c) => c.trim());
      if (h.length === 2 && FLOW.has(h[0]) && (h[1] === '↓' || !isHeaderCell(h[1]))) {
        const rows = [];
        let j = i + 2;
        while (j < lines.length && isTableLine(lines[j]) && !lines[j].includes('---')) {
          rows.push(lines[j].split('|').slice(1, -1).map((c) => c.trim()));
          j++;
        }
        if (rows.length <= 3) {
          const buf = [h[0], h[1] === '↓' ? '' : `  ${h[1]}`];
          for (const r of rows) {
            if (r[0]) buf.push(`  ${r[0]}`);
            if (r[1]) buf.push(`  ${r[1]}`);
          }
          buf.push('        ↓');
          out.push('```text', ...buf.filter(Boolean), '```', '');
          i = j - 1;
          continue;
        }
      }
    }
    out.push(lines[i]);
  }
  return out.join('\n');
}

/** 분리된 ```text ↓ ``` 블록을 하나의 흐름도로 병합 */
function repairFragmentedFlowDiagrams(text) {
  const LAYER = /^(화면|Handler|Facade|Service|Rule|DAO|MyBatis XML|DB)$/;
  const lines = text.split(/\r?\n/);
  const out = [];
  let i = 0;
  while (i < lines.length) {
    const t = lines[i]?.trim() ?? '';
    if (LAYER.test(t)) {
      const buf = [];
      let j = i;
      let arrowBlocks = 0;
      while (j < lines.length) {
        const lt = lines[j]?.trim() ?? '';
        if (LAYER.test(lt)) {
          buf.push(lt);
          j++;
          continue;
        }
        if (/^\s{2,}\S/.test(lines[j] ?? '')) {
          buf.push(lines[j].trimEnd());
          j++;
          continue;
        }
        if (lines[j]?.trim() === '```text' && /↓/.test(lines[j + 1] ?? '')) {
          buf.push(lines[j + 1].trim());
          arrowBlocks++;
          j += 3;
          continue;
        }
        if (lt === '') {
          j++;
          continue;
        }
        break;
      }
      if (arrowBlocks >= 2) {
        out.push('```text', ...buf, '```', '');
        i = j;
        continue;
      }
    }
    out.push(lines[i]);
    i++;
  }
  return out.join('\n');
}

/** 표 셀에 ↓ 가 포함된 흐름도 행 → text 블록으로 분리 */
function repairArrowFlowTableRows(text) {
  const lines = text.split(/\r?\n/);
  const out = [];
  let i = 0;
  while (i < lines.length) {
    if (!isTableLine(lines[i])) {
      out.push(lines[i]);
      i++;
      continue;
    }
    const block = [];
    while (i < lines.length && isTableLine(lines[i])) {
      block.push(lines[i]);
      i++;
    }
    const arrowRows = block.slice(2).filter((row) => {
      const cells = row.split('|').slice(1, -1).map((c) => c.trim());
      return cells.some((c) => c === '↓' || c === '→');
    });
    if (arrowRows.length >= 2) {
      const header = block[0];
      const sep = block[1];
      const good = block.slice(2).filter((row) => {
        const cells = row.split('|').slice(1, -1).map((c) => c.trim());
        return !cells.some((c) => c === '↓' || c === '→');
      });
      const prose = block.slice(2).flatMap((row) => {
        const cells = row.split('|').slice(1, -1).map((c) => c.trim());
        return cells.filter((c) => /다\.$/.test(c) && c.length > 20);
      });
      for (const p of prose) out.push(p, '');
      out.push(header, sep, ...good);
      const flow = [];
      for (const row of arrowRows) {
        const cells = row.split('|').slice(1, -1).map((c) => c.trim()).filter(Boolean);
        for (const c of cells) {
          if (c === '↓' || c === '→') flow.push('        ↓');
          else flow.push(c);
        }
      }
      if (flow.length) out.push('', '```text', ...flow, '```', '');
    } else {
      out.push(...block);
    }
  }
  return out.join('\n');
}

/** 표 셀에 '예시는 다음과 같다' 등 본문 문장 분리 */
function repairExampleProseInTables(text) {
  const lines = text.split(/\r?\n/);
  const out = [];
  for (const line of lines) {
    if (isTableLine(line)) {
      const cells = line.split('|').slice(1, -1).map((c) => c.trim());
      if (cells.some((c) => /^예시는 다음|^나쁜 예시|^테스트 예시|^조회 예시|^저장 예시|^금지 예시|^실행 예시/.test(c))) {
        const prose = cells.find((c) => /예시는 다음|예시는/.test(c));
        if (prose) out.push(prose, '');
        const rest = cells.filter((c) => !/예시는 다음|예시는/.test(c));
        if (rest.length >= 2) out.push(`| ${rest.join(' | ')} |`);
        continue;
      }
    }
    out.push(line);
  }
  return out.join('\n');
}

/** 연속 ```java 블록 병합 */
function mergeAdjacentCodeBlocks(text) {
  return text.replace(/```(java|gradle|xml|yaml)\n([\s\S]*?)```\n\n```\1\n([\s\S]*?)```/g, (all, lang, a, b) => {
    return '```' + lang + '\n' + a.trimEnd() + '\n' + b.trim() + '\n```';
  });
}

function repairFile(filePath) {
  const original = fs.readFileSync(filePath, 'utf8');
  const footerIdx = original.indexOf('\n## 소스·관련 문서');
  const body = footerIdx >= 0 ? original.slice(0, footerIdx) : original;
  const footer = footerIdx >= 0 ? original.slice(footerIdx) : '';

  let fixed = body;
  fixed = repairOutsideCodeBlocks(fixed, repairPlainTextTables);
  fixed = repairOutsideCodeBlocks(fixed, repairCodeInTables);
  fixed = repairOutsideCodeBlocks(fixed, removeProseTableRows);
  fixed = repairOutsideCodeBlocks(fixed, repairShiftedDescriptionColumn);
  fixed = repairOutsideCodeBlocks(fixed, repairFlowLayerMisTables);
  fixed = repairOutsideCodeBlocks(fixed, repairArrowFlowTableRows);
  fixed = repairOutsideCodeBlocks(fixed, repairExampleProseInTables);
  fixed = mergeAdjacentCodeBlocks(fixed);
  fixed = fixed.replace(/\n{3,}/g, '\n\n');

  const result = fixed + footer;
  if (result !== original) {
    fs.writeFileSync(filePath, result, 'utf8');
    return true;
  }
  return false;
}

const dir = __dirname;
const skip = new Set(['README.md']);
let changed = 0;
for (const f of fs.readdirSync(dir)) {
  if (!f.endsWith('.md') || skip.has(f)) continue;
  if (repairFile(path.join(dir, f))) changed++;
}
console.log(`Repaired tables in ${changed} file(s)`);
