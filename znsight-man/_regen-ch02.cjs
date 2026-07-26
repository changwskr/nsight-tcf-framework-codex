#!/usr/bin/env node
'use strict';

const fs = require('fs');
const { formatDocxMarkdown } = require('./format-docx-md.cjs');

const raw = fs.readFileSync('_docx-cache/docx-7.txt', 'utf8');
let body = formatDocxMarkdown(raw);

body = body.replace(
  /```text\n([\s\S]*?)```\n\n```text\n([\s\S]*?)```/g,
  (all, a, b) => {
    if (/↓/.test(a.trimEnd()) && /\[/.test(b.trimStart())) {
      return '```text\n' + a.trimEnd() + '\n' + b.trim() + '\n```';
    }
    return all;
  }
);

const footer = [
  '## 소스·관련 문서',
  '',
  '| 참고 |',
  '|------|',
  '| [03-전체아키텍처.md](../zman/03-전체아키텍처.md) |',
  '| [04-모듈구성.md](../zman/04-모듈구성.md) |',
  '',
  '> znsight-guide: `통합 (7).docx`',
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
  '← [1. 문서 개요](./01-문서개요.md) · [3. TCF Framework 개발 원칙](./03-TCF-개발원칙.md) →';

const full = [
  '# 2. 개발자가 알아야 할 NSIGHT 기본 구조',
  '',
  '> **NSIGHT TCF 개발 Manual** · 원본: [`znsight-guide`](../znsight-guide/) · 갱신: 2026-07-05',
  '',
  body,
  footer,
  '---',
  '',
  nav,
  '',
].join('\n');

fs.writeFileSync('02-NSIGHT-기본구조.md', full, 'utf8');
console.log('Written 02-NSIGHT-기본구조.md');
