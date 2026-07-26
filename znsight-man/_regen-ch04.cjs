#!/usr/bin/env node
'use strict';

const fs = require('fs');
const { formatDocxMarkdown } = require('./format-docx-md.cjs');

const raw = fs.readFileSync('_docx-cache/docx-9.txt', 'utf8');
let body = formatDocxMarkdown(raw);

// docx 원본 17 WAR → 코드베이스 기준 13 WAR (deploy-wars.sh)
body = body.replace(
  /Gradle, 17 WAR 배포를 기준으로/,
  'Gradle, 13 WAR 배포 (deploy-wars.sh)를 기준으로'
);

const footer = [
  '## 소스·관련 문서',
  '',
  '| 참고 |',
  '|------|',
  '',
  '> znsight-guide: `통합 (9).docx`',
  '',
  '| [applicationNaming.md](../zdoc/applicationNaming.md) |',
  '',
  '## 코드베이스 정정 (develop 기준)',
  '',
  '| 항목 | 값 |',
  '|------|-----|',
  '| 업무 WAR | ic, pc, ms, sv, pd, eb, ep, ss, mg + tcf-om |',
  '| ztomcat deploy | `ztomcat/deploy-wars.sh` 13 WAR |',
  '| buildZtomcatWars | 15 WAR |',
  '| bootRun | gateway 8100, uj 8102, jwt 8110, ui 8099 |',
  '',
].join('\n');

const nav =
  '← [3. TCF Framework 개발 원칙](./03-TCF-개발원칙.md) · [5. 개발자 역할과 책임](./05-개발자-역할과-책임.md) →';

const full = [
  '# 4. 개발 표준 전체 요약',
  '',
  '> **NSIGHT TCF 개발 Manual** · 원본: [`znsight-guide`](../znsight-guide/) · 갱신: 2026-07-05',
  '',
  body,
  '',
  footer,
  '---',
  '',
  nav,
  '',
].join('\n');

fs.writeFileSync('04-개발표준-전체요약.md', full, 'utf8');
console.log('Written 04-개발표준-전체요약.md');
