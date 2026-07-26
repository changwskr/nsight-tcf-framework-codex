#!/usr/bin/env node
'use strict';

const fs = require('fs');
const { formatDocxMarkdown } = require('./format-docx-md.cjs');

const raw = fs.readFileSync('_docx-cache/docx-10.txt', 'utf8');
const body = formatDocxMarkdown(raw);

const footer = [
  '## 소스·관련 문서',
  '',
  '| 참고 |',
  '|------|',
  '',
  '> znsight-guide: `통합 (10).docx`',
  '',
  '| [어플리케이션계층.md](../zdoc/어플리케이션계층.md) |',
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
  '← [4. 개발 표준 전체 요약](./04-개발표준-전체요약.md) · [6. 로컬 개발환경 구성](./06-로컬-개발환경-구성.md) →';

const full = [
  '# 5. 개발자 역할과 책임',
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

fs.writeFileSync('05-개발자-역할과-책임.md', full, 'utf8');
console.log('Written 05-개발자-역할과-책임.md');
