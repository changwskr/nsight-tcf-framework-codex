#!/usr/bin/env node
'use strict';

const fs = require('fs');
const path = require('path');
const os = require('os');
const { execSync } = require('child_process');
const { formatDocxMarkdown } = require('./format-docx-md.cjs');

const docxPath = path.resolve(__dirname, '..', 'znsight-guide-word', 'NSIGHT TCF 개발 매뉴얼 - JWT.docx');
const outMd = path.join(__dirname, '79-JWT-개발-매뉴얼.md');
const cacheTxt = path.join(__dirname, '_docx-cache', 'jwt-manual.txt');

function getDocxText(filePath) {
  const escaped = filePath.replace(/'/g, "''");
  const tmpOut = path.join(os.tmpdir(), `jwt-docx-out-${process.pid}.txt`);
  const tmpPs = path.join(os.tmpdir(), `jwt-docx-extract-${process.pid}.ps1`);
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
      maxBuffer: 30 * 1024 * 1024,
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

if (!fs.existsSync(docxPath)) {
  console.error('DOCX not found:', docxPath);
  process.exit(1);
}

const raw = getDocxText(docxPath);
fs.mkdirSync(path.dirname(cacheTxt), { recursive: true });
fs.writeFileSync(cacheTxt, raw, 'utf8');
console.log('Cached', cacheTxt, `(${raw.length} bytes)`);

const body = formatDocxMarkdown(raw);

const footer = [
  '## 소스·관련 문서',
  '',
  '| 참고 |',
  '|------|',
  '',
  '> 원본: [`znsight-guide-word/NSIGHT TCF 개발 매뉴얼 - JWT.docx`](../znsight-guide-word/NSIGHT%20TCF%20개발%20매뉴얼%20-%20JWT.docx)',
  '',
  '| [41-JWT-SSO-연계.md](./41-JWT-SSO-연계.md) | SSO·Gateway 연계 요약 |',
  '| [tcf-jwt/README.md](../tcf-jwt/README.md) | tcf-jwt 모듈 구현 |',
  '| [tcf-gateway/README.md](../tcf-gateway/README.md) | Gateway JWT 검증 |',
  '',
  '## 코드베이스 정정 (develop 기준)',
  '',
  '| 항목 | 값 |',
  '|------|-----|',
  '| JWT 패키지 | `com.nh.nsight.auth.jwt` |',
  '| Gateway JWT 검증 | `GatewayJwtValidator` + `GatewayJwtConfiguration` (JWKS) |',
  '| OM Gateway JWT | `businessCode=OM` + `jwt.enabled=true` 시 Bearer 필수 |',
  '| bootRun | gateway 8100, jwt 8110, ui 8099 |',
  '| Denylist | Gateway 미연동 (tcf-jwt만 관리) |',
  '',
].join('\n');

const nav =
  '← [41. JWT / SSO 연계 기준](./41-JWT-SSO-연계.md) · [70. FAQ](./70-FAQ-Troubleshooting.md) →';

const full = [
  '# 79. JWT 개발 매뉴얼',
  '',
  '> **NSIGHT TCF 개발 Manual** · 원본: [`znsight-guide-word`](../znsight-guide-word/) · 갱신: 2026-07-06',
  '',
  body,
  '',
  footer,
  '---',
  '',
  nav,
  '',
].join('\n');

fs.writeFileSync(outMd, full, 'utf8');
console.log('Written', outMd, `(${full.length} bytes)`);
