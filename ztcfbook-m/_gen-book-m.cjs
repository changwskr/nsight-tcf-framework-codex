#!/usr/bin/env node
'use strict';

/**
 * ztcfbook-m — 입문판 mermaid(그림) 블록 삽입
 * ztcfbook-h _master-enrich-data.cjs 의 mermaid를 재사용
 */
const fs = require('fs');
const path = require('path');
const { MASTER_ENRICH } = require('../ztcfbook-h/_master-enrich-data.cjs');
const { CHAPTER_MAP } = require('./_beginner-chapter-map.cjs');

const ROOT = __dirname;
const DIAGRAM_SECTION = '## 그림으로 보기';

function normalizeEol(s) {
  return s.replace(/\r\n/g, '\n');
}

function renderMermaid(mermaid, mermaidExtra, mermaidExtraTitle) {
  if (!mermaid && !mermaidExtra) return '';
  let out = `${DIAGRAM_SECTION}\n\n`;
  if (mermaid) {
    out += `\`\`\`mermaid\n${mermaid.trim()}\n\`\`\`\n\n`;
  }
  if (mermaidExtra) {
    const sub = mermaidExtraTitle ?? '자세히 보기';
    out += `### ${sub}\n\n\`\`\`mermaid\n${mermaidExtra.trim()}\n\`\`\`\n\n`;
  }
  return `${out}---\n\n`;
}

function stripExistingDiagramBlock(body) {
  const re = new RegExp(
    `${DIAGRAM_SECTION.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')}[\\s\\S]*?\\n---\\n\\n`,
    'm',
  );
  return body.replace(re, '');
}

function resolveEnrich(mKey) {
  const masterKey = CHAPTER_MAP[mKey];
  if (!masterKey) return null;
  return MASTER_ENRICH[masterKey] ?? null;
}

function transformChapter(relPath, content) {
  const key = relPath.replace(/\.md$/, '').replace(/\\/g, '/');
  const enrich = resolveEnrich(key);
  if (!enrich?.mermaid && !enrich?.mermaidExtra) return content;

  const block = renderMermaid(enrich.mermaid, enrich.mermaidExtra, enrich.mermaidExtraTitle);
  let body = normalizeEol(stripExistingDiagramBlock(normalizeEol(content)));

  if (body.includes(DIAGRAM_SECTION)) return content;

  const inserted = body.replace(
    /(\| \*\*원본\*\* \|[^\n]+\n\n---\n\n)/,
    `$1${block}`,
  );
  if (inserted !== body) return inserted;

  return body.replace(/\n---\n\n## /, `\n---\n\n${block}## `);
}

function walkMd(dir, base = '') {
  const entries = fs.readdirSync(dir, { withFileTypes: true });
  const files = [];
  for (const e of entries) {
    if (e.name.startsWith('_gen') || e.name.startsWith('_beginner')) continue;
    const rel = path.join(base, e.name).replace(/\\/g, '/');
    const full = path.join(dir, e.name);
    if (e.isDirectory()) files.push(...walkMd(full, rel));
    else if (e.name.endsWith('.md')) files.push(rel);
  }
  return files;
}

function main() {
  const files = walkMd(ROOT).filter((f) => f !== '00-목차.md' && f !== 'README.md' && !f.endsWith('/README.md'));
  let count = 0;
  const missing = [];

  for (const rel of files) {
    const key = rel.replace(/\.md$/, '');
    if (!CHAPTER_MAP[key]) missing.push(key);

    const full = path.join(ROOT, rel);
    const raw = fs.readFileSync(full, 'utf8');
    const out = transformChapter(rel, raw);
    if (out !== raw) {
      fs.writeFileSync(full, out, 'utf8');
      count++;
      console.log('Diagram:', rel);
    }
  }

  console.log(`\nDone: ${count} files updated, map keys: ${Object.keys(CHAPTER_MAP).length}`);
  if (missing.length) console.warn('No map entry:', missing.join(', '));
}

main();
