#!/usr/bin/env node
'use strict';

const fs = require('fs');
const path = require('path');
const os = require('os');
const { execSync } = require('child_process');

const guideDir = process.argv[2] || path.join(__dirname, '..', 'znsight-guide-word');
const outDir = process.argv[3] || path.join(__dirname, '_docx-cache');

if (!fs.existsSync(outDir)) fs.mkdirSync(outDir, { recursive: true });

function getDocxText(filePath) {
  const escaped = filePath.replace(/'/g, "''");
  const tmpOut = path.join(os.tmpdir(), `docx-out-${process.pid}-${Math.random().toString(36).slice(2)}.txt`);
  const tmpPs = path.join(os.tmpdir(), `docx-extract-${process.pid}-${Math.random().toString(36).slice(2)}.ps1`);
  const script = `Add-Type -AssemblyName System.IO.Compression.FileSystem
$zip = [System.IO.Compression.ZipFile]::OpenRead('${escaped}')
try {
  $entry = $zip.Entries | Where-Object { $_.FullName -eq 'word/document.xml' } | Select-Object -First 1
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

/** 명명규칙 docx — `NSIGHT TCF 개발 매뉴얼 - 명명 - {NN}. {제목}.docx` */
const NAMING_DOCX_TITLE = {
  1: '명명 - 01. 명명규칙 총정리',
  2: '명명 - 02. 명명규칙 최상위 원칙',
  3: '명명 - 03. 업무코드 Context WAR Package',
  4: '명명 - 04. 업무코드 표준표',
  5: '명명 - 05. Gradle 모듈 설계기준',
  6: '명명 - 06. Package',
  7: '명명 - 07. ServiceId',
  8: '명명 - 08. 거래코드',
  9: '명명 - 09. Java Class',
  10: '명명 - 10. Java Method Field',
  11: '명명 - 11. Java DTO',
  12: '명명 - 12. MyBatis Mapper SQL ID',
  13: '명명 - 13. DB 객체',
  14: '명명 - 14. 오류코드',
  15: '명명 - 15. 화면번호',
  16: '명명 - 16. 로그 감사로그',
  17: '명명 - 17. 화면번호와 ServiceId 연결',
  18: '명명 - 18. Gateway 라우팅',
  19: '명명 - 19. Batch Scheduler',
  20: '명명 - 20. Cache',
  21: '명명 - 21. Header 항목',
};

function resolveNamingDocx(guideDir, n) {
  const legacy = path.join(guideDir, `NSIGHT TCF 개발 매뉴얼 - 명명규칙 상세 (${n}).docx`);
  if (fs.existsSync(legacy)) return legacy;
  const titlePart = NAMING_DOCX_TITLE[n];
  if (!titlePart) return null;
  const hit = fs.readdirSync(guideDir).find(
    (f) => f.endsWith('.docx') && f.includes(titlePart) && !/명명규칙 상세 \(\d+\)/.test(f),
  );
  return hit ? path.join(guideDir, hit) : null;
}

const main = {};
const naming = {};

for (const name of fs.readdirSync(guideDir)) {
  if (!name.endsWith('.docx')) continue;
  const m = name.match(/통합 \((\d+)\)\.docx$/);
  if (!m) continue;
  main[parseInt(m[1], 10)] = path.join(guideDir, name);
}

for (const n of Object.keys(NAMING_DOCX_TITLE).map(Number)) {
  const file = resolveNamingDocx(guideDir, n);
  if (file) naming[n] = file;
}

for (const [n, file] of Object.entries(main).sort((a, b) => a[0] - b[0])) {
  const text = getDocxText(file);
  const out = path.join(outDir, `docx-${n}.txt`);
  fs.writeFileSync(out, text, 'utf8');
  console.log(`Extracted docx-${n}.txt (${text.length} bytes) <- ${path.basename(file)}`);
}

for (const [n, file] of Object.entries(naming).sort((a, b) => a[0] - b[0])) {
  const text = getDocxText(file);
  const out = path.join(outDir, `naming-${n}.txt`);
  fs.writeFileSync(out, text, 'utf8');
  console.log(`Extracted naming-${n}.txt (${text.length} bytes) <- ${path.basename(file)}`);
}
