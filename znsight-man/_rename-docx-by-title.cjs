#!/usr/bin/env node
'use strict';

/**
 * znsight-guide-word docx → 본문 메인 타이틀 기준 파일명 변경
 *
 * Usage:
 *   node _rename-docx-by-title.cjs [--dry-run] [--force] [--tonghap|--naming|--all] [guideDir]
 */

const fs = require('fs');
const path = require('path');
const os = require('os');
const { execSync } = require('child_process');

const dryRun = process.argv.includes('--dry-run');
const forceReplace = process.argv.includes('--force');
const modeTonghap = process.argv.includes('--tonghap');
const modeNaming = process.argv.includes('--naming');
const modeAll = process.argv.includes('--all') || (!modeTonghap && !modeNaming);
const extraArgs = process.argv.slice(2).filter((a) => !a.startsWith('--'));
const guideDir = extraArgs[0] || path.join(__dirname, '..', 'znsight-guide-word');

const PREFIX = 'NSIGHT TCF 개발 매뉴얼 - ';
const INVALID_CHARS = /[<>:"/\\|?*\u0000-\u001f]/g;

function getDocxText(filePath) {
  const escaped = filePath.replace(/'/g, "''");
  const tmpOut = path.join(os.tmpdir(), `docx-out-${process.pid}-${Math.random().toString(36).slice(2)}.txt`);
  const tmpPs = path.join(os.tmpdir(), `docx-extract-${process.pid}-${Math.random().toString(36).slice(2)}.ps1`);
  const script = `Add-Type -AssemblyName System.IO.Compression.FileSystem
$zip = [System.IO.Compression.ZipFile]::OpenRead('${escaped}')
try {
  $entry = $zip.Entries | Where-Object { $_.FullName -eq 'word/document.xml' } | Select-Object -First 1
  if (-not $entry) { throw 'word/document.xml not found' }
  $sr = New-Object System.IO.StreamReader($entry.Open(), [System.Text.Encoding]::UTF8)
  try { $xml = $sr.ReadToEnd() } finally { $sr.Close() }
} finally { $zip.Dispose() }
$text = $xml -replace '</w:p>', [char]10 -replace '<[^>]+>', ''
$text = $text -replace '&amp;','&' -replace '&lt;','<' -replace '&gt;','>' -replace '&quot;','"'
[System.IO.File]::WriteAllText('${tmpOut.replace(/\\/g, '\\\\')}', $text, [System.Text.UTF8Encoding]::new($false))
`;
  fs.writeFileSync(tmpPs, `\uFEFF${script}`, 'utf8');
  try {
    execSync(`powershell -NoProfile -ExecutionPolicy Bypass -File "${tmpPs}"`, {
      encoding: 'utf8',
      maxBuffer: 20 * 1024 * 1024,
    });
    return fs.readFileSync(tmpOut, 'utf8');
  } finally {
    for (const f of [tmpPs, tmpOut]) {
      try {
        fs.unlinkSync(f);
      } catch {
        /* ignore */
      }
    }
  }
}

/** 본문 메인 타이틀 추출 (_docx-map detectChapterFromText 기준) */
function extractMainTitle(text) {
  if (!text || text.trim().length < 10) return '';

  const lines = text
    .split(/\r?\n/)
    .map((l) => l.replace(/\s+/g, ' ').trim())
    .filter(Boolean);

  for (let i = 0; i < lines.length; i++) {
    const m = lines[i].match(/^(\d{1,2})\.\s+(\S.*)$/);
    if (!m) continue;
    const n = parseInt(m[1], 10);
    if (n < 1 || n > 78) continue;
    const subRe = new RegExp(`^${n}\\.\\d+(?:\\.\\d+)?\\s+`);
    for (let j = i + 1; j < Math.min(i + 40, lines.length); j++) {
      if (subRe.test(lines[j])) {
        return `${n}. ${m[2].trim()}`;
      }
    }
  }

  for (let i = 0; i < lines.length; i++) {
    const explicit = lines[i].match(/^부록\s*([A-J])\.\s*(.+)$/i);
    if (explicit) {
      const letter = explicit[1].toUpperCase();
      const subRe = new RegExp(`^${letter}\\.\\d+\\s+`, 'i');
      for (let j = i + 1; j < Math.min(i + 40, lines.length); j++) {
        if (subRe.test(lines[j])) {
          return `부록${letter}. ${explicit[2].trim()}`;
        }
      }
    }
    const letterOnly = lines[i].match(/^([A-J])\.\s+(\S.*)$/i);
    if (!letterOnly) continue;
    const letter = letterOnly[1].toUpperCase();
    const subRe = new RegExp(`^${letter}\\.\\d+\\s+`, 'i');
    for (let j = i + 1; j < Math.min(i + 40, lines.length); j++) {
      if (subRe.test(lines[j])) {
        return `부록${letter}. ${letterOnly[2].trim()}`;
      }
    }
  }

  return '';
}

/** 명명규칙 상세: 문서 주제 첫 줄 (섹션 번호 오탐 방지) */
function extractNamingTitle(text) {
  if (!text || text.trim().length < 10) return '';

  const lines = text
    .split(/\r?\n/)
    .map((l) => l.replace(/\s+/g, ' ').trim())
    .filter(Boolean);

  // NSIGHT 헤더 직후 장 번호 형식 (예: 54. 명명규칙 총정리 + 54.1) — 본문 인용 제외
  for (let i = 0; i < lines.length - 1; i++) {
    if (!/^NSIGHT TCF\s+개발가이드/i.test(lines[i])) continue;
    const next = lines[i + 1];
    const m = next?.match(/^(\d{1,2})\.\s+(\S.*)$/);
    if (!m) continue;
    const n = parseInt(m[1], 10);
    const subRe = new RegExp(`^${n}\\.\\d+(?:\\.\\d+)?\\s+`);
    for (let j = i + 2; j < Math.min(i + 20, lines.length); j++) {
      if (subRe.test(lines[j])) {
        return `${n}. ${m[2].trim()}`;
      }
    }
  }

  // 일반 명명규칙: "1. 도입" 이전 첫 주제 줄
  const skipLine = (line) =>
    /^NSIGHT TCF/i.test(line)
    || /^개발가이드/i.test(line)
    || /^아래는 NSIGHT/i.test(line)
    || /^본 절은 NSIGHT/i.test(line)
    || /^명명규칙\s*-\s*모듈$/i.test(line)
    || (line.length > 80 && /입니다\.?$/.test(line));

  const beforeIntro = [];
  for (const line of lines) {
    if (skipLine(line)) continue;
    if (/^1\.\s+도입/.test(line)) break;
    if (line.length >= 3) beforeIntro.push(line.trim());
  }
  if (!beforeIntro.length) return '';

  const pick =
    beforeIntro.find((l) => /명명규칙\s+설계기준\s*$/.test(l) || /명명규칙\s*-\s*.+\s+설계기준\s*$/.test(l))
    || beforeIntro.find((l) => /명명규칙/i.test(l) && !/^\d+\.\s+/.test(l))
    || beforeIntro.find((l) => /명명규칙/i.test(l))
    || beforeIntro[0];

  let t = pick;
  if (/\s+설계기준\s*$/.test(t) && !/^명명규칙\s*-/.test(t)) {
    t = t.replace(/\s+설계기준\s*$/, '');
  }
  return t.replace(/^(\d{1,2})장\.\s*/, '').trim();
}

function sanitizeFileName(title) {
  return title
    .replace(INVALID_CHARS, '')
    .replace(/\//g, ' · ')
    .replace(/\s+/g, ' ')
    .trim();
}

function buildTargetName(title) {
  return `${PREFIX}${sanitizeFileName(title)}.docx`;
}

function uniqueTargetPath(dir, baseName, sourcePath) {
  return path.join(dir, baseName);
}

function collectFiles(guideDir) {
  const all = fs.readdirSync(guideDir);
  const tonghap = all
    .filter((f) => /^NSIGHT TCF 개발 매뉴얼 - 통합 \(\d+\)\.docx$/.test(f))
    .sort((a, b) => parseInt(a.match(/\((\d+)\)/)[1], 10) - parseInt(b.match(/\((\d+)\)/)[1], 10))
    .map((file) => ({ file, kind: 'tonghap', extract: extractMainTitle }));

  const naming = all
    .filter((f) => /^NSIGHT TCF 개발 매뉴얼 - 명명규칙 상세 \(\d+\)\.docx$/.test(f))
    .sort((a, b) => parseInt(a.match(/\((\d+)\)/)[1], 10) - parseInt(b.match(/\((\d+)\)/)[1], 10))
    .map((file) => ({ file, kind: 'naming', extract: extractNamingTitle }));

  if (modeAll) return [...tonghap, ...naming];
  if (modeNaming) return naming;
  return tonghap;
}

function processFiles(guideDir, entries) {
  const results = [];
  const errors = [];

  for (const { file, kind, extract } of entries) {
    const src = path.join(guideDir, file);
    try {
      const text = getDocxText(src);
      const title = extract(text);
      if (!title) {
        const size = fs.statSync(src).size;
        errors.push({ file, reason: size < 100 ? 'empty or placeholder file' : 'title not found' });
        continue;
      }
      const newName = buildTargetName(title);
      const dest = uniqueTargetPath(guideDir, newName, src);
      results.push({ file, title, newName: path.basename(dest), dest, kind });

      if (path.basename(dest) === file) {
        console.log(`SKIP (already named): ${file}`);
        continue;
      }
      if (fs.existsSync(dest) && path.resolve(dest) !== path.resolve(src)) {
        const srcSize = fs.statSync(src).size;
        const destSize = fs.statSync(dest).size;
        if (forceReplace) {
          console.log(`[${kind}] ${file}`);
          console.log(`  title: ${title}`);
          console.log(`  -> ${path.basename(dest)} (replace existing ${destSize} bytes)`);
          if (!dryRun) {
            fs.unlinkSync(dest);
            fs.renameSync(src, dest);
          }
          continue;
        }
        if (Math.abs(srcSize - destSize) < 512) {
          console.log(`[${kind}] DUPLICATE (same size, remove source): ${file} -> ${path.basename(dest)}`);
          if (!dryRun) fs.unlinkSync(src);
          continue;
        }
        // 명명규칙 (1) 등: 소형 중복본 + 동명 54.* 대형 파일 존재 시 소형본 제거
        if (kind === 'naming' && srcSize < 600000 && title.startsWith('54.')) {
          console.log(`[${kind}] DUPLICATE (small copy of ${title}, remove): ${file}`);
          if (!dryRun) fs.unlinkSync(src);
          continue;
        }
        console.log(`[${kind}] CONFLICT (target exists): ${file} -> ${path.basename(dest)} (src=${srcSize}, dest=${destSize}). Use --force to replace.`);
        continue;
      }

      console.log(`[${kind}] ${file}`);
      console.log(`  title: ${title}`);
      console.log(`  -> ${path.basename(dest)}`);

      if (!dryRun) {
        fs.renameSync(src, dest);
      }
    } catch (e) {
      errors.push({ file, reason: e.message });
    }
  }

  return { results, errors };
}

if (!fs.existsSync(guideDir)) {
  console.error(`Guide directory not found: ${guideDir}`);
  process.exit(1);
}

const entries = collectFiles(guideDir);

if (!entries.length) {
  console.log('No target docx files found.');
  process.exit(0);
}

console.log(`${dryRun ? '[DRY-RUN] ' : ''}Guide: ${guideDir}`);
console.log(`Mode: ${modeAll ? 'all' : modeNaming ? 'naming' : 'tonghap'}`);
console.log(`Found ${entries.length} file(s)\n`);

const { results, errors } = processFiles(guideDir, entries);

console.log(`\nDone: ${results.length} processed, ${errors.length} error(s)`);
if (errors.length) {
  console.log('\nErrors:');
  for (const err of errors) console.log(`  ${err.file}: ${err.reason}`);
}

if (dryRun) {
  console.log('\nRe-run without --dry-run to apply renames.');
}
