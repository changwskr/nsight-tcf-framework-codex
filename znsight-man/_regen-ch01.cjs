#!/usr/bin/env node
'use strict';

const fs = require('fs');
const { formatDocxMarkdown } = require('./format-docx-md.cjs');

const raw = fs.readFileSync('_docx-cache/docx-6.txt', 'utf8');
const body = formatDocxMarkdown(raw);

const footer = [
  '## 소스·관련 문서',
  '',
  '| 참고 |',
  '|------|',
  '',
  '> znsight-guide: `통합 (6).docx`',
  '',
  '| [01-문서개요.md](../zman/01-문서개요.md) |',
  '| [README.md](../znsight-man/README.md) |',
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

const nav = '[2. 개발자가 알아야 할 NSIGHT 기본 구조](./02-NSIGHT-기본구조.md) →';

const full = [
  '# 1. 문서 개요',
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

fs.writeFileSync('01-문서개요.md', full, 'utf8');
console.log('Written 01-문서개요.md');
