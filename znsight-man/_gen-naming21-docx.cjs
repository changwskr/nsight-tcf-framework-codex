#!/usr/bin/env node
'use strict';

const fs = require('fs');
const path = require('path');
const { execFileSync } = require('child_process');

const scriptDir = __dirname;
const repoRoot = path.join(scriptDir, '..');
const mdFile = fs.readdirSync(scriptDir).find((f) => /^명명규칙-21-Header-/.test(f));
if (!mdFile) {
  console.error('Markdown source not found');
  process.exit(1);
}

const mdFull = path.join(scriptDir, mdFile);
const outFull = path.join(repoRoot, 'znsight-guide-word', 'NSIGHT TCF 개발 매뉴얼 - 명명 - 21. Header 항목.docx');
const ps1 = path.join(scriptDir, '_gen-naming21-docx.ps1');

execFileSync(
  'powershell',
  [
    '-NoProfile',
    '-ExecutionPolicy',
    'Bypass',
    '-File',
    ps1,
    '-MdPath',
    mdFull,
    '-OutPath',
    outFull,
  ],
  {
    stdio: 'inherit',
    encoding: 'utf8',
    env: {
      ...process.env,
      DOC_TITLE: 'Header 항목 명명규칙 설계기준',
      OUT_PATH: outFull,
    },
  },
);

console.log('Done:', outFull);
