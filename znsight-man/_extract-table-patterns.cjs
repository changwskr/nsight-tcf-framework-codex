#!/usr/bin/env node
'use strict';
const fs = require('fs');
const path = require('path');
const { buildDocxMaps } = require('./_docx-map.cjs');

const { byChapter: DOCX_BY_CHAPTER } = buildDocxMaps();
const cacheDir = path.join(__dirname, '_docx-cache');

function isSection(t) {
  return /^(\d+\.)+\s+\S/.test(t) || /^\d+\.\d+\s+\S/.test(t) || /^부록/.test(t);
}

function isProse(t) {
  if (!t || t.length > 90) return true;
  if (/[다요]\.$/.test(t) && t.length > 20) return true;
  if (/^(git |POST |export |spring:|feat\(|fix\(|test\(|chore\(|docs\()/.test(t)) return true;
  if (/^[\[{]/.test(t)) return true;
  return false;
}

function isHeaderCell(t) {
  t = t.trim();
  if (!t || t.length > 40 || isProse(t) || isSection(t)) return false;
  if (/^↓$|^→$|^├|^└|^│/.test(t)) return false;
  if (/^\d+$/.test(t) && t.length <= 2) return true; // No column
  if (/^[a-z]{2,4}$/.test(t) && ['feat', 'fix', 'docs', 'test', 'chore', 'core', 'web', 'cache', 'om', 'sv', 'ic', 'jwt', 'batch', 'ci', 'main', 'develop'].includes(t)) return false;
  if (/^[a-z.]+\(/.test(t)) return false;
  if (/^v\d/.test(t)) return false;
  if (/\.(yml|yaml|gradle|java|xml|war|jar|sql|md|sh|bat)$/.test(t)) return false;
  if (/^https?:/.test(t)) return false;
  if (/^[A-Z]{2,3}$/.test(t)) return true; // SV, OM
  if (/^[가-힣A-Za-z0-9\s\/\*·\-_\.]{2,35}$/.test(t) && !/[다요]\.$/.test(t)) return true;
  return false;
}

function detectTableAt(lines, start) {
  const headers = [];
  let i = start;
  while (i < lines.length && headers.length < 12) {
    const t = lines[i].trim();
    if (!t) break;
    if (isSection(t) || isProse(t)) break;
    if (!isHeaderCell(t)) break;
    // stop if looks like flow diagram cell only
    if (t === '↓' || t === '→') break;
    headers.push(t);
    i++;
    if (headers.length >= 2) {
      // If next line doesn't look like header, stop collecting headers
      const n = lines[i]?.trim() ?? '';
      if (!n || !isHeaderCell(n)) break;
      // 3rd header - continue if still header-like and not data
      if (headers.length >= 3) {
        const n2 = lines[i + 1]?.trim() ?? '';
        if (!n2 || isProse(n2) || isSection(n2)) break;
        // if 4th line looks like data for 3-col, stop at 3
        if (headers.length === 3 && !isHeaderCell(n2)) break;
      }
    }
  }
  if (headers.length < 2) return null;

  const cols = headers.length;
  let j = i;
  let rows = 0;
  while (j + cols <= lines.length) {
    const row = lines.slice(j, j + cols).map((l) => l.trim());
    if (row.some((c) => !c || isSection(c))) break;
    if (row.some((c) => c === '↓' || c === '→')) break;
    if (isProse(row[0]) && rows > 0) break;
    if (row[0] === headers[0]) break;
    // first data row validation
    if (rows === 0 && isHeaderCell(row[0]) && isHeaderCell(row[1]) && cols === 2) {
      // might be mis-detected nested headers
      if (headers[0] !== 'No' && !/^\d+$/.test(row[0])) {
        // allow if row[0] is clearly data (longer text)
      }
    }
    rows++;
    j += cols;
    if (rows > 80) break;
  }
  if (rows < 1) return null;
  return { headers, cols, rows, start, end: j };
}

const patterns = new Map();

for (let ch = 8; ch <= 78; ch++) {
  const docxN = DOCX_BY_CHAPTER[ch];
  if (!docxN) continue;
  const fp = path.join(cacheDir, `docx-${docxN}.txt`);
  if (!fs.existsSync(fp)) continue;
  const lines = fs.readFileSync(fp, 'utf8').split(/\r?\n/);
  for (let i = 0; i < lines.length; i++) {
    const t = lines[i].trim();
    if (!isHeaderCell(t)) continue;
    const det = detectTableAt(lines, i);
    if (!det || det.rows < 2) continue;
    const key = JSON.stringify(det.headers);
    if (!patterns.has(key)) {
      patterns.set(key, { headers: det.headers, cols: det.cols, count: 0, chapters: new Set(), rows: det.rows });
    }
    const p = patterns.get(key);
    p.count++;
    p.chapters.add(ch);
    p.rows = Math.max(p.rows, det.rows);
    i = det.end - 1;
  }
}

// Load existing FIXED_HEADER_TABLES keys
const fmt = fs.readFileSync(path.join(__dirname, 'format-docx-md.cjs'), 'utf8');
const existing = new Set();
const m = fmt.match(/const FIXED_HEADER_TABLES = \[([\s\S]*?)\];/);
if (m) {
  for (const arr of m[1].matchAll(/\[(.*?)\]/g)) {
    try {
      const items = arr[1].split(',').map((s) => s.trim().replace(/^'|'$/g, ''));
      if (items.length >= 2 && items[0]) existing.add(JSON.stringify(items));
    } catch (_) {}
  }
}

const missing = [...patterns.values()]
  .filter((p) => !existing.has(JSON.stringify(p.headers)))
  .filter((p) => p.chapters.size >= 1)
  .sort((a, b) => b.count - a.count);

console.log('Unique table patterns in docx 8-78:', patterns.size);
console.log('Already in FIXED_HEADER_TABLES:', patterns.size - missing.length);
console.log('Missing patterns:', missing.length);
console.log('\n=== Top 80 missing patterns ===');
missing.slice(0, 80).forEach((p) => {
  console.log(
    `${p.count}x ${p.cols}col rows~${p.rows} ch[${[...p.chapters].slice(0, 8).join(',')}${p.chapters.size > 8 ? '...' : ''}] ${p.headers.join(' | ')}`
  );
});

// Output JS array snippet for top patterns
console.log('\n=== JS snippet (top 60) ===');
missing.slice(0, 60).forEach((p) => {
  console.log(`  [${p.headers.map((h) => `'${h.replace(/'/g, "\\'")}'`).join(', ')}],`);
});
