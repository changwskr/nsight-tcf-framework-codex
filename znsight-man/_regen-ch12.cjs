#!/usr/bin/env node
'use strict';

const fs = require('fs');
const { formatDocxMarkdown } = require('./format-docx-md.cjs');

const raw = fs.readFileSync('_docx-cache/docx-17.txt', 'utf8');
const body = formatDocxMarkdown(raw);

const footer = [
  '## 소스·관련 문서',
  '',
  '| 참고 |',
  '|------|',
  '',
  '> znsight-guide: `통합 (17).docx`',
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
  '← [11. application.yml 구성 기준](./11-application-yml-기준.md) · [13. 패키지 구조 표준](./13-패키지-구조-표준.md) →';

const full = [
  '# 12. 애플리케이션 계층구조',
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

fs.writeFileSync('12-애플리케이션-계층구조.md', full, 'utf8');
console.log('Written 12-애플리케이션-계층구조.md');
