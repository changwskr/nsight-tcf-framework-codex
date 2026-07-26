#!/usr/bin/env node
'use strict';

/**
 * ztcfbook → ztcfbook-h (Master Edition) 변환
 * - ztcfbook 본문 전체 포함
 * - 아키텍처 뷰 · 코드 샘플 · Deep Dive · 심화 참고 자동 삽입
 */
const fs = require('fs');
const path = require('path');
const { MASTER_ENRICH, deepDive } = require('./_master-enrich-data.cjs');
const { buildNarratives } = require('./_master-narrative.cjs');

const MASTER_NARRATIVE = buildNarratives();

const ROOT = __dirname;
const SRC_BOOK = path.join(ROOT, '..', 'ztcfbook');
const OUT_BOOK = ROOT;

function readLines(relPath, start, end) {
  const abs = path.join(ROOT, '..', relPath.replace(/\//g, path.sep));
  if (!fs.existsSync(abs)) return `// (파일 없음: ${relPath})`;
  const lines = fs.readFileSync(abs, 'utf8').split(/\r?\n/);
  const s = (start ?? 1) - 1;
  const e = end ?? lines.length;
  return lines.slice(s, e).join('\n');
}

function extLang(file) {
  if (file.endsWith('.java')) return 'java';
  if (file.endsWith('.xml')) return 'xml';
  if (file.endsWith('.json')) return 'json';
  if (file.endsWith('.yml') || file.endsWith('.yaml')) return 'yaml';
  if (file.endsWith('.sql')) return 'sql';
  if (file.endsWith('.gradle')) return 'gradle';
  if (file.endsWith('.sh')) return 'shell';
  if (file.endsWith('.md')) return 'markdown';
  return 'text';
}

function renderSamples(samples) {
  if (!samples?.length) return '';
  const blocks = samples.map((s) => {
    const lang = extLang(s.file);
    const code = readLines(s.file, s.start, s.end);
    return `### ${s.title}

\`\`\`${lang}
${code}
\`\`\`

원본: [\`${s.file}\`](../${s.file})`;
  });
  return `## 구현 샘플 (코드베이스)

${blocks.join('\n\n')}

---\n\n`;
}

function renderMermaid(mermaid, mermaidExtra, mermaidExtraTitle) {
  if (!mermaid && !mermaidExtra) return '';
  let out = '## 아키텍처 뷰\n\n';
  if (mermaid) {
    out += `\`\`\`mermaid\n${mermaid.trim()}\n\`\`\`\n\n`;
  }
  if (mermaidExtra) {
    const sub = mermaidExtraTitle ?? '상세 흐름';
    out += `### ${sub}\n\n\`\`\`mermaid\n${mermaidExtra.trim()}\n\`\`\`\n\n`;
  }
  return `${out}---\n\n`;
}

function renderNarrative(text) {
  if (!text?.trim()) return '';
  return `## Master 해설

${text.trim()}

---\n\n`;
}

function renderDeepDive(deepDiveText) {
  if (!deepDiveText) return '';
  return `${deepDiveText.trim()}\n\n---\n\n`;
}

function renderRefs(refs) {
  if (!refs?.length) return '';
  return `## 심화 참고 (Master)

${refs.map((r) => `- [${r}](../${r})`).join('\n')}

---\n\n`;
}

function parseRefsFromSource(content) {
  const section = content.match(/## 출처 색인[\s\S]*?(?=\n## |$)/);
  if (!section) return [];
  const paths = new Set();
  const linkRe = /\[[^\]]*\]\(\.\.\/\.\.\/([^)]+)\)/g;
  const backtickRe = /`([^`]+\.(?:md|java|xml|yml|sql|sh|gradle))`/g;
  let m;
  while ((m = linkRe.exec(section[0])) !== null) paths.add(m[1]);
  while ((m = backtickRe.exec(section[0])) !== null) paths.add(m[1]);
  return [...paths].slice(0, 8);
}

function chapterTitle(content, relPath) {
  const m = content.match(/^# (.+)$/m);
  if (m) return m[1].replace(/^제\d+장\.\s*/, '').replace(/^부록\s*[A-Z]\.\s*/, '');
  return path.basename(relPath, '.md');
}

function resolveEnrich(key, content) {
  const base = MASTER_ENRICH[key] ?? {};
  const title = chapterTitle(content, key);
  const refs = base.refs?.length ? base.refs : parseRefsFromSource(content);
  const narrative = base.narrative ?? MASTER_NARRATIVE[key] ?? '';
  const dd = base.deepDive ?? deepDive(title, [
    'ztcfbook 본문과 상단 Master 블록을 함께 본다.',
    '**구현 샘플**은 코드베이스 최신 상태와 diff 확인.',
    '**심화 참고** 링크로 설계·매뉴얼 원문 추적.',
  ]);
  return { ...base, refs, narrative, deepDive: dd };
}

function renderMasterBlock(enrich) {
  return (
    renderMermaid(enrich.mermaid, enrich.mermaidExtra, enrich.mermaidExtraTitle)
    + renderNarrative(enrich.narrative)
    + renderSamples(enrich.samples)
    + renderDeepDive(enrich.deepDive)
    + renderRefs(enrich.refs)
  );
}

function normalizeEol(s) {
  return s.replace(/\r\n/g, '\n');
}

function stripSourceMeta(body) {
  return body
    .replace(/^\| \*\*상태\*\* \|[^\n]+\n/m, '| **상태** | Master Edition (ztcfbook-h) |\n')
    .replace(/\*\*상태\*\* \| 집필 완료/g, '**상태** | Master Edition');
}

function relKey(relFromSrcBook) {
  return relFromSrcBook.replace(/\.md$/, '').replace(/\\/g, '/');
}

function masterMeta(relPath) {
  const ztcfRel = `../ztcfbook/${relPath.replace(/\\/g, '/')}`;
  return `| **에디션** | **Master** — 아키텍트·시니어·플랫폼 |
| **기반 원본** | [ztcfbook/${relPath.replace(/\\/g, '/')}](${ztcfRel}) |
| **입문서** | [ztcfbook-m](../ztcfbook-m/README.md) |`;
}

function transformChapter(relPath, content) {
  const key = relKey(relPath);
  const enrich = resolveEnrich(key, normalizeEol(content));
  const block = renderMasterBlock(enrich);

  let body = stripSourceMeta(normalizeEol(content));
  body = body.replace(
    /(\| \*\*목차\*\* \|[^\n]+\n\n---\n\n)/,
    `$1${block}`,
  );

  body = body.replace(
    /\| \*\*편\*\* \|[^\n]+\n/,
    (m) => m + masterMeta(relPath).split('\n').map((l) => `${l}\n`).join(''),
  );

  if (!body.includes('## 아키텍처 뷰') && !body.includes('## Master 해설') && block.trim()) {
    body = body.replace(/\n---\n\n## /, `\n---\n\n${block}## `);
  }

  body = body.replace(
    /## 장 요약\n\n([\s\S]*?)\n\n---/,
    (m, summary) => `## 장 요약 (Master)\n\n${summary.trim()}\n\n> Master Edition: **아키텍처 뷰** → **Master 해설** → **구현 샘플** → **Master Deep Dive** → **심화 참고** 순으로 본문과 함께 읽는다.\n\n---`,
  );

  body = body.replace(
    /## 출처 색인/,
    '## 출처 색인 · Master 확장\n\n| 구분 | 경로 |\n| --- | --- |\n| ztcfbook-h | 본 파일 |\n| ztcfbook | `../ztcfbook/' + relPath.replace(/\\/g, '/') + '` |\n\n### 원본 출처\n',
  );

  return body;
}

function walkMd(dir, base = '') {
  const entries = fs.readdirSync(dir, { withFileTypes: true });
  const files = [];
  for (const e of entries) {
    if (e.name.startsWith('_gen') || e.name.startsWith('_master') || e.name.endsWith('.docx') || e.name === '목차.docx') {
      continue;
    }
    const rel = path.join(base, e.name).replace(/\\/g, '/');
    const full = path.join(dir, e.name);
    if (e.isDirectory()) files.push(...walkMd(full, rel));
    else if (e.name.endsWith('.md')) files.push(rel);
  }
  return files;
}

function updateNavLinks(content) {
  return content
    .replace(/\(\.\.\/ztcfbook\//g, '(../ztcfbook/')
    .replace(/\(\.\.\/제/g, '(../제')
    .replace(/\(\.\/부록/g, '(./부록')
    .replace(/\(\.\.\/00-목차\.md\)/g, '(../00-목차.md)')
    .replace(/\(\.\.\/ztcfbook-m\//g, '(../ztcfbook-m/');
}

function main() {
  if (!fs.existsSync(SRC_BOOK)) {
    console.error('ztcfbook not found:', SRC_BOOK);
    process.exit(1);
  }

  const files = walkMd(SRC_BOOK).filter((f) => f !== '00-목차.md' && f !== 'README.md');
  let count = 0;
  const missing = [];

  for (const rel of files) {
    const key = relKey(rel);
    if (!MASTER_ENRICH[key]) missing.push(key);
    const src = path.join(SRC_BOOK, rel);
    const out = path.join(OUT_BOOK, rel);
    fs.mkdirSync(path.dirname(out), { recursive: true });
    const raw = fs.readFileSync(src, 'utf8');
    const transformed = updateNavLinks(transformChapter(rel, raw));
    fs.writeFileSync(out, transformed, 'utf8');
    count++;
    console.log('Master:', rel);
  }

  const tocSrc = fs.readFileSync(path.join(SRC_BOOK, '00-목차.md'), 'utf8');
  const toc = `# NSIGHT TCF Framework — Master Edition (ztcfbook-h)

> **아키텍트·시니어·플랫폼** · ztcfbook 전체 + **Master 해설** + 코드 샘플 + Deep Dive · [ztcfbook](../ztcfbook/) · [ztcfbook-m](../ztcfbook-m/)

${tocSrc.replace(/^# NSIGHT TCF Framework — 완전 개발 가이드[\s\S]*?---\n\n/, '')}`;

  fs.writeFileSync(path.join(OUT_BOOK, '00-목차.md'), toc, 'utf8');

  console.log(`\nDone: ${count} master files, enrich keys: ${Object.keys(MASTER_ENRICH).length}`);
  if (missing.length) console.warn('Fallback enrich (no explicit key):', missing.join(', '));
}

main();
