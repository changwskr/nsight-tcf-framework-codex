#!/usr/bin/env node
'use strict';

const fs = require('fs');
const path = require('path');

const root = path.join(__dirname, '..');
const guideDir = path.join(root, 'znsight-guide');
const manDir = __dirname;
const { cacheDir, buildDocxMaps } = require('./_docx-map.cjs');
const { byChapter: DOCX_BY_CHAPTER, byAppendix: APPENDIX_DOCX } = buildDocxMaps();

function norm(s) {
  return s
    .replace(/\s+/g, ' ')
    .replace(/[^\w가-힣 .\-/]/g, '')
    .trim()
    .toLowerCase();
}

function extractSections(text) {
  const secs = [];
  for (const line of text.split(/\r?\n/)) {
    const t = line.trim();
    let m;
    if ((m = t.match(/^#{1,4}\s+(.+)$/))) secs.push(m[1].trim());
    else if ((m = t.match(/^(\d+\.\d+(?:\.\d+)?)\s+(.+)$/))) secs.push(`${m[1]} ${m[2]}`.trim());
    else if ((m = t.match(/^([A-J]\.\d+(?:\.\d+)?)\s+(.+)$/i))) secs.push(`${m[1]} ${m[2]}`.trim());
  }
  return [...new Set(secs)];
}

function extractDocxSections(text) {
  const secs = [];
  for (const line of text.split(/\r?\n/)) {
    const t = line.trim();
    const m = t.match(/^(\d+\.\d+(?:\.\d+)?|[A-J]\.\d+(?:\.\d+)?)\s+(.+)$/i);
    if (m) secs.push(`${m[1]} ${m[2]}`.trim());
  }
  return [...new Set(secs)];
}

function sigWords(text) {
  return text
    .split(/\r?\n/)
    .map((l) => l.trim())
    .filter((l) => l.length > 8 && l.length < 120 && !/^(\d+\.|#{1,4})/.test(l))
    .slice(0, 200);
}

function coverage(docxText, mdBody) {
  const dn = norm(docxText);
  const mn = norm(mdBody);
  const lines = sigWords(docxText);
  if (!lines.length) return { pct: 100, missing: [] };
  let hit = 0;
  const missing = [];
  for (const line of lines) {
    const key = norm(line).slice(0, 40);
    if (key.length < 6) continue;
    if (mn.includes(key)) hit++;
    else if (missing.length < 8) missing.push(line.slice(0, 80));
  }
  const total = lines.filter((l) => norm(l).length >= 6).length || 1;
  return { pct: Math.round((hit / total) * 100), missing, total, hit };
}

// guide docx inventory (same rule as extract script)
const byNum = {};
for (const f of fs.readdirSync(guideDir).filter((x) => x.endsWith('.docx'))) {
  const m = f.match(/\((\d+)\)\.docx$/);
  if (!m) continue;
  const n = parseInt(m[1], 10);
  const full = path.join(guideDir, f);
  const size = fs.statSync(full).size;
  if (!byNum[n] || size > byNum[n].size) byNum[n] = { file: f, size };
}

const usedDocx = new Set([...Object.values(DOCX_BY_CHAPTER), ...Object.values(APPENDIX_DOCX)].filter(Boolean));
const unmappedDocx = Object.keys(byNum)
  .map(Number)
  .filter((n) => !usedDocx.has(n))
  .sort((a, b) => a - b);

const chapterFiles = fs
  .readdirSync(manDir)
  .filter((f) => /^(\d{2}-|부록)/.test(f) && f.endsWith('.md'))
  .sort();

const report = [];
report.push('# znsight-guide vs znsight-man audit');
report.push('');
report.push(`Guide docx numbers: ${Object.keys(byNum).length}, cache files: ${fs.readdirSync(cacheDir).filter((f) => f.startsWith('docx-')).length}`);
report.push(`Unmapped docx numbers (not in DOCX_BY_CHAPTER/APPENDIX): ${unmappedDocx.join(', ') || 'none'}`);
report.push('');

const issues = [];

for (const [chStr, docxN] of Object.entries({ ...Object.fromEntries(Object.entries(DOCX_BY_CHAPTER).map(([k, v]) => [`ch${k}`, v])), ...Object.fromEntries(Object.entries(APPENDIX_DOCX).map(([k, v]) => [`ap${k}`, v])) })) {
  if (!docxN) continue;
  const isAp = chStr.startsWith('ap');
  const id = isAp ? chStr.replace('ap', '부록 ') : `ch${chStr.replace('ch', '')}`;
  const mdFile = chapterFiles.find((f) => {
    if (isAp) return f.startsWith(`부록${chStr.replace('ap', '')}-`);
    const n = chStr.replace('ch', '').padStart(2, '0');
    return f.startsWith(`${n}-`);
  });
  if (!mdFile) {
    issues.push({ id, docxN, type: 'NO_MD', msg: 'markdown file missing' });
    continue;
  }
  const docxPath = path.join(cacheDir, `docx-${docxN}.txt`);
  if (!fs.existsSync(docxPath)) {
    issues.push({ id, docxN, type: 'NO_CACHE', msg: 'docx cache missing' });
    continue;
  }
  const docxRaw = fs.readFileSync(docxPath, 'utf8');
  const mdFull = fs.readFileSync(path.join(manDir, mdFile), 'utf8');
  const mdBody = mdFull.split('## 소스·관련 문서')[0];
  const docxSecs = extractDocxSections(docxRaw);
  const mdSecs = extractSections(mdBody);
  const cov = coverage(docxRaw, mdBody);

  const missingSecs = docxSecs.filter((s) => {
    const key = norm(s).slice(0, 30);
    return !mdSecs.some((m) => norm(m).includes(key) || key.includes(norm(m).slice(0, 20)));
  });

  if (cov.pct < 75 || missingSecs.length > 3) {
    issues.push({
      id,
      docxN,
      mdFile,
      type: 'LOW_COVERAGE',
      pct: cov.pct,
      missingSecs: missingSecs.slice(0, 10),
      sampleMissing: cov.missing,
      docxSource: byNum[docxN]?.file,
      docxSecs: docxSecs.length,
      mdSecs: mdSecs.length,
    });
  }
}

// chapters with docx=0 — check if guide has content for nearby numbers
const noDocxChapters = [];
for (let i = 1; i <= 78; i++) {
  if (!DOCX_BY_CHAPTER[i]) noDocxChapters.push(i);
}

report.push('## Issues');
report.push('');
if (!issues.length) report.push('No major issues detected.');
else {
  for (const i of issues) {
    report.push(`### ${i.id} ← docx-${i.docxN} (${i.type})`);
    if (i.mdFile) report.push(`- md: ${i.mdFile}`);
    if (i.docxSource) report.push(`- guide: ${i.docxSource}`);
    if (i.pct != null) report.push(`- line coverage: ${i.pct}% (${i.hit}/${i.total})`);
    if (i.docxSecs != null) report.push(`- sections: docx ${i.docxSecs} → md ${i.mdSecs}, missing ${i.missingSecs?.length || 0}`);
    if (i.missingSecs?.length) report.push(`- missing sections: ${i.missingSecs.join(' | ')}`);
    if (i.sampleMissing?.length) report.push(`- sample missing lines: ${i.sampleMissing.map((x) => '`' + x + '`').join(', ')}`);
    report.push('');
  }
}

report.push('## Unmapped guide docx');
report.push('');
for (const n of unmappedDocx) {
  report.push(`- docx-${n}: ${byNum[n].file} (${byNum[n].size} bytes)`);
}

report.push('');
report.push('## Chapters without docx mapping (use zdoc/zman fallback)');
report.push('');
report.push(noDocxChapters.join(', '));

const out = path.join(manDir, '_audit-report.md');
fs.writeFileSync(out, report.join('\n'), 'utf8');

console.log('Issues:', issues.length);
console.log('Unmapped docx:', unmappedDocx.join(', ') || 'none');
issues.slice(0, 15).forEach((i) => {
  console.log(`  ${i.id} docx-${i.docxN} ${i.type} ${i.pct != null ? i.pct + '%' : ''}`);
});
console.log('Report:', out);
