#!/usr/bin/env node
'use strict';

const fs = require('fs');
const path = require('path');
const { execSync } = require('child_process');

const root = path.join(__dirname, '..');
const guideDir = path.join(root, 'znsight-guide');
const outDir = __dirname;
const { cacheDir, getRawText } = require('./_docx-map.cjs');
const { formatDocxMarkdown } = require('./format-docx-md.cjs');
const { NAMING_SPECS, pad2 } = require('./_naming-specs.cjs');

function applyCorrections(text) {
  return text
    .replace(/17개 업무/g, '9개 업무')
    .replace(/17개 WAR/g, '9개 WAR')
    .replace(/NSIGHT 17개 업무코드/g, 'NSIGHT 업무코드 (settings.gradle 기준)')
    .replace(/19 WAR/g, '13 WAR (deploy-wars.sh)')
    .replace(/19개 WAR/g, '13개 WAR (ztomcat deploy)')
    .replace(/16업무/g, '9업무')
    .replace(/17 WAR 배포/g, '13 WAR 배포 (deploy-wars.sh)')
    .replace(/cc-service|bc-service|cm-service|bp-service|bd-service|cs-service|ct-service/g, '(미포함·확장 예정)')
    .replace(
      /(\| 9 \| 이벤트 \| EB \| Event Batch[\s\S]*?\| \/eb \| eb\.war \|\n)(?:\| 1[0-7] \|[\s\S]*?\| \/om \| om\.war \|\n)+/,
      ''
    );
}

function ensureCache() {
  if (!fs.existsSync(path.join(cacheDir, 'naming-1.txt'))) {
    execSync(`node "${path.join(__dirname, '_extract-docx.cjs')}" "${guideDir}" "${cacheDir}"`, {
      stdio: 'inherit',
    });
  }
}

function loadNamingMarkdown(n) {
  const raw = getRawText('naming', n);
  if (raw && raw.trim().length >= 50) {
    return applyCorrections(formatDocxMarkdown(raw));
  }
  // 명명규칙 상세 (21).docx 원본이 비어 있음 → 통합 본문 사용
  if (n === 21) {
    const fallbackPath = path.join(__dirname, '_naming-21-header-body.md');
    if (fs.existsSync(fallbackPath)) {
      return fs.readFileSync(fallbackPath, 'utf8').trim();
    }
  }
  return '';
}

function chapterLink(num) {
  const files = fs.readdirSync(outDir).filter((f) => f.startsWith(String(num).padStart(2, '0') + '-') && f.endsWith('.md'));
  if (!files.length) return `[${num}장](./)`;
  return `[${num}장](./${files[0].replace(/\.md$/, '')}.md)`;
}

function buildFooter(spec, hasBody) {
  const lines = ['## 관련 Manual 장', ''];
  for (const ch of spec.related) {
    lines.push(`- ${chapterLink(ch)}`);
  }
  lines.push('', '## 원본', '', `- [\`znsight-guide\`](../znsight-guide/) — \`명명규칙 상세 (${spec.n}).docx\``);
  if (!hasBody) {
    lines.push('', '> 원본 docx에서 본문을 추출하지 못했습니다. guide 파일을 확인하세요.');
  } else if (spec.n === 21) {
    lines.push('', '> 원본 `명명규칙 상세 (21).docx` 본문 없음. `_naming-21-header-body.md`(명명규칙 8·10·11·14·16·17·18 통합) 사용.');
  }
  return lines.join('\n');
}

ensureCache();

const indexLines = [
  '# 명명규칙 상세 (znsight-guide)',
  '',
  '> `znsight-guide/NSIGHT TCF 개발 매뉴얼 - 명명규칙 상세 (N).docx` → 개별 Markdown',
  '',
  '| No | 문서 | 원본 docx |',
  '| --- | --- | --- |',
];

const written = [];

for (const spec of NAMING_SPECS) {
  const fileBase = `명명규칙-${pad2(spec.n)}-${spec.slug}`;
  const outPath = path.join(outDir, `${fileBase}.md`);
  const body = loadNamingMarkdown(spec.n);
  const hasBody = body.trim().length > 0;

  const md = [
    `# ${spec.title}`,
    '',
    '> **NSIGHT TCF 명명규칙 상세** · 원본: [`znsight-guide`](../znsight-guide/) · 갱신: 2026-07-05',
    '',
    hasBody ? body : `_원본 \`명명규칙 상세 (${spec.n}).docx\`에서 추출할 본문이 없습니다._`,
    '',
    '---',
    '',
    buildFooter(spec, hasBody),
    '',
  ].join('\n');

  fs.writeFileSync(outPath, md, 'utf8');
  written.push({ ...spec, file: `${fileBase}.md`, hasBody });
  indexLines.push(`| ${spec.n} | [${spec.title}](./${fileBase}.md) | 명명규칙 상세 (${spec.n}).docx |`);
}

indexLines.push('', '---', '', '## 재생성', '', '```bash', 'cd znsight-man', 'node _extract-docx.cjs ../znsight-guide ./_docx-cache', 'node _generate-naming.cjs', '```', '');

fs.writeFileSync(path.join(outDir, '명명규칙-00-목차.md'), indexLines.join('\n'), 'utf8');

const empty = written.filter((w) => !w.hasBody);
console.log(`Written: ${written.length} naming docs + 명명규칙-00-목차.md`);
if (empty.length) {
  console.log(`Empty source: ${empty.map((w) => w.n).join(', ')}`);
}

module.exports = { NAMING_SPECS };
