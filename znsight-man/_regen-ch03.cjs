#!/usr/bin/env node
'use strict';

const fs = require('fs');
const { formatDocxMarkdown } = require('./format-docx-md.cjs');

const raw = fs.readFileSync('_docx-cache/docx-8.txt', 'utf8');
let body = formatDocxMarkdown(raw);

// §3.4 ServiceId 처리 흐름 — 마지막 Handler 실행 줄 병합
body = body.replace(
  /```text\nPOST \/sv\/online\n↓\nbusinessCode = SV 확인\n↓\nserviceId = SV\.Customer\.selectSummary 확인\n↓\nTransactionDispatcher\n↓\n```\n\nSvCustomerSelectSummaryHandler 실행/,
  '```text\nPOST /sv/online\n↓\nbusinessCode = SV 확인\n↓\nserviceId = SV.Customer.selectSummary 확인\n↓\nTransactionDispatcher\n↓\nSvCustomerSelectSummaryHandler 실행\n```'
);

const footer = [
  '## 소스·관련 문서',
  '',
  '| 참고 |',
  '|------|',
  '| [TCF.md](../zdoc/TCF.md) |',
  '| [05-TCF처리구조.md](../zman/05-TCF처리구조.md) |',
  '',
  '> znsight-guide: `통합 (8).docx`',
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
  '← [2. 개발자가 알아야 할 NSIGHT 기본 구조](./02-NSIGHT-기본구조.md) · [4. 개발 표준 전체 요약](./04-개발표준-전체요약.md) →';

const full = [
  '# 3. TCF Framework 개발 원칙',
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

fs.writeFileSync('03-TCF-개발원칙.md', full, 'utf8');
console.log('Written 03-TCF-개발원칙.md');
