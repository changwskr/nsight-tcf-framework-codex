#!/usr/bin/env node
'use strict';

const fs = require('fs');
const path = require('path');

const dir = __dirname;
const outPath = path.join(dir, '00-Index.md');

const CATEGORIES = [
  {
    name: '개요·원칙',
    files: ['01', '02', '03', '04', '05'],
  },
  {
    name: '환경·빌드',
    files: ['06', '07', '08', '09', '10', '11'],
  },
  {
    name: '구조·명명',
    files: ['12', '13', '14', '15', '16', '17', '18', '19'],
  },
  {
    name: '전문·계층 개발',
    files: ['20', '21', '22', '23', '24', '25', '26', '27', '28', '29', '30', '31'],
  },
  {
    name: '품질·보안·운영',
    files: ['32', '33', '34', '35', '36', '37', '38', '39', '40', '41', '42', '43', '44', '45'],
  },
  {
    name: 'OM 등록 절차',
    files: ['46', '47', '48', '49', '50', '51', '52', '53'],
  },
  {
    name: '테스트·품질',
    files: ['54', '55', '56', '57', '58', '59', '60', '61', '62'],
  },
  {
    name: '빌드·배포',
    files: ['63', '64', '65', '66', '67', '68', '69', '70'],
  },
  {
    name: '샘플',
    files: ['71', '72', '73', '74', '75', '76', '77', '78'],
  },
];

const APPENDIX = ['A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J'];

const KEYWORDS = [
  { term: 'ServiceId', chapters: ['16', '17', '20', '21', '22', '23', '47', '부록B'] },
  { term: '거래코드 (TransactionCode)', chapters: ['17', '20', '21', '35', '부록C'] },
  { term: 'TransactionHandler', chapters: ['23', '71', '72', '73'] },
  { term: 'Facade', chapters: ['24', '12', '25', '71'] },
  { term: 'Service / Rule / DAO', chapters: ['25', '26', '27', '12'] },
  { term: 'MyBatis / Mapper / SQL', chapters: ['28', '29', '30', '57', '부록E'] },
  { term: '표준 전문 (Header / Body)', chapters: ['20', '21', '부록D'] },
  { term: 'Online Endpoint', chapters: ['22', '15', '09'] },
  { term: '업무코드 / WAR / Context', chapters: ['15', '09', '08', '부록A'] },
  { term: 'DTO / Validation', chapters: ['18', '19'] },
  { term: '예외 / 오류코드', chapters: ['32', '33', '51', '77', '부록F'] },
  { term: '거래로그 / 감사로그', chapters: ['35', '34'] },
  { term: '세션 / JWT / SSO', chapters: ['39', '41', '40'] },
  { term: 'Timeout / Idempotency', chapters: ['37', '38', '49'] },
  { term: '거래통제 / OM 등록', chapters: ['46', '47', '48', '49', '50', '51'] },
  { term: 'Cache / 파일 / Batch', chapters: ['43', '44', '45', '76'] },
  { term: '단위·통합·TCF 거래 테스트', chapters: ['54', '55', '56', '78'] },
  { term: 'CI/CD / WAR / 배포 / 롤백', chapters: ['63', '64', '65', '66', '67', '68'] },
  { term: 'application.yml', chapters: ['11', '부록G'] },
  { term: '체크리스트', chapters: ['부록H', '부록I', '부록J', '68'] },
  { term: 'FAQ / 장애 대응', chapters: ['69', '70', '60'] },
  { term: 'Gradle / Git / 로컬 환경', chapters: ['06', '07', '08', '63'] },
  { term: '명명 규칙 / 패키지', chapters: ['13', '14', '부록B', '부록C'] },
  { term: 'TCF 개발 원칙 / STF·ETF', chapters: ['03', '04', '12'] },
];

function readMeta(filename) {
  const p = path.join(dir, filename);
  if (!fs.existsSync(p)) return null;
  const lines = fs.readFileSync(p, 'utf8').split(/\r?\n/);
  const h1 = (lines.find((l) => /^# /.test(l)) || '').replace(/^# /, '').trim();
  const sections = lines
    .filter((l) => /^### /.test(l))
    .map((l) => l.replace(/^### /, '').trim())
    .filter((s) => !/^소스·|^코드베이스/.test(s));
  return { filename, h1, sections };
}

function findFile(prefix) {
  if (/^부록/.test(prefix)) {
    const letter = prefix.replace('부록', '');
    return fs.readdirSync(dir).find((f) => f.startsWith(`부록${letter}-`) && f.endsWith('.md'));
  }
  const n = prefix.padStart(2, '0');
  return fs.readdirSync(dir).find((f) => f.startsWith(`${n}-`) && f.endsWith('.md'));
}

function link(filename, label) {
  return `[${label}](./${filename})`;
}

function chapterLink(prefix, label) {
  const f = findFile(prefix);
  return f ? link(f, label || f.replace(/\.md$/, '')) : label || prefix;
}

const lines = [];
lines.push('# 00. NSIGHT TCF 개발 Manual 색인');
lines.push('');
lines.push('> **NSIGHT TCF 개발 Manual** · [README](./README.md) · 원본: [`znsight-guide`](../znsight-guide/) · **갱신:** 2026-07-05');
lines.push('');
lines.push('본 문서는 `znsight-man`에 포함된 **78장 + 부록 10개** Markdown 파일의 색인입니다.');
lines.push('');
lines.push('---');
lines.push('');
lines.push('## 1. 파일 목록');
lines.push('');

for (const cat of CATEGORIES) {
  lines.push(`### ${cat.name}`);
  lines.push('');
  lines.push('| 장 | 제목 | 파일 |');
  lines.push('|:--:|------|------|');
  for (const num of cat.files) {
    const f = findFile(num);
    if (!f) continue;
    const meta = readMeta(f);
    lines.push(`| ${parseInt(num, 10)} | ${meta.h1.replace(/^\d+\.\s*/, '')} | ${link(f, f)} |`);
  }
  lines.push('');
}

lines.push('### 부록');
lines.push('');
lines.push('| 부록 | 제목 | 파일 |');
lines.push('|:--:|------|------|');
for (const letter of APPENDIX) {
  const f = findFile(`부록${letter}`);
  if (!f) continue;
  const meta = readMeta(f);
  lines.push(`| ${letter} | ${meta.h1.replace(/^부록 [A-J]\.\s*/, '')} | ${link(f, f)} |`);
}
lines.push('');
lines.push('---');
lines.push('');
lines.push('## 2. 주제별 색인');
lines.push('');
lines.push('| 주제 | 관련 장·부록 |');
lines.push('|------|-------------|');
for (const { term, chapters } of KEYWORDS) {
  const refs = chapters.map((c) => chapterLink(c, c.startsWith('부록') ? `부록 ${c.replace('부록', '')}` : `${parseInt(c, 10)}장`)).join(' · ');
  lines.push(`| ${term} | ${refs} |`);
}
lines.push('');
lines.push('---');
lines.push('');
lines.push('## 3. 장별 절 목차');
lines.push('');

for (const cat of CATEGORIES) {
  for (const num of cat.files) {
    const f = findFile(num);
    if (!f) continue;
    const meta = readMeta(f);
    if (!meta.sections.length) continue;
    lines.push(`### ${meta.h1} — ${link(f, f)}`);
    lines.push('');
    for (const s of meta.sections) {
      lines.push(`- ${s}`);
    }
    lines.push('');
  }
}

lines.push('### 부록');
lines.push('');
for (const letter of APPENDIX) {
  const f = findFile(`부록${letter}`);
  if (!f) continue;
  const meta = readMeta(f);
  lines.push(`#### ${meta.h1} — ${link(f, f)}`);
  lines.push('');
  for (const s of meta.sections) {
    lines.push(`- ${s}`);
  }
  lines.push('');
}

lines.push('---');
lines.push('');
lines.push('## 4. 빠른 탐색');
lines.push('');
lines.push('| 목적 | 시작 장 |');
lines.push('|------|---------|');
lines.push(`| 처음 온보딩 | ${chapterLink('01', '1장')} → ${chapterLink('02', '2장')} → ${chapterLink('12', '12장')} |`);
lines.push(`| 거래 개발 | ${chapterLink('16', '16장')} → ${chapterLink('23', '23장')} → ${chapterLink('71', '71장')} |`);
lines.push(`| 로컬 실행 | ${chapterLink('06', '6장')} → ${chapterLink('63', '63장')} |`);
lines.push(`| OM 등록 | ${chapterLink('47', '47장')} ~ ${chapterLink('51', '51장')} |`);
lines.push(`| 배포 | ${chapterLink('64', '64장')} → ${chapterLink('66', '66장')} |`);
lines.push('');
lines.push('---');
lines.push('');
lines.push('*이 색인은 `node _build-index.cjs`로 재생성할 수 있습니다.*');

fs.writeFileSync(outPath, lines.join('\n'), 'utf8');
console.log('Written:', outPath);
