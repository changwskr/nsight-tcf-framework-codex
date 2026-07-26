#!/usr/bin/env node
'use strict';

/**
 * 명명규칙 docx → NSIGHT TCF 개발 매뉴얼 - 명명 - {NN}. {제목}.docx
 *
 * Usage:
 *   node _rename-naming-prefixed.cjs [--dry-run] [guideDir]
 */

const fs = require('fs');
const path = require('path');

const dryRun = process.argv.includes('--dry-run');
const extraArgs = process.argv.slice(2).filter((a) => !a.startsWith('--'));
const guideDir = extraArgs[0] || path.join(__dirname, '..', 'znsight-guide-word');

const PREFIX = 'NSIGHT TCF 개발 매뉴얼 - 명명 - ';

/** No → [현재 파일명 일부(매칭), 새 제목] */
const NAMING_MAP = [
  [1, '54. 명명규칙 총정리', '01. 명명규칙 총정리'],
  [2, '명명규칙 최상위 원칙', '02. 명명규칙 최상위 원칙'],
  [3, '업무코드 Context WAR Package 명명규칙', '03. 업무코드 Context WAR Package'],
  [4, 'NSIGHT 17개 업무코드 기준', '04. 업무코드 표준표'],
  [5, '명명규칙 - 모듈 설계기준', '05. Gradle 모듈 설계기준'],
  [6, 'Package 명명규칙', '06. Package'],
  [7, 'ServiceId 명명규칙', '07. ServiceId'],
  [8, '거래코드(Transaction Code) 명명규칙', '08. 거래코드'],
  [9, 'Java Class 명명규칙', '09. Java Class'],
  [10, 'Java Method Field 명명규칙', '10. Java Method Field'],
  [11, 'Java DTO 명명규칙', '11. Java DTO'],
  [12, 'MyBatis Mapper XML SQL ID 명명규칙', '12. MyBatis Mapper SQL ID'],
  [13, 'DB 객체 명명규칙', '13. DB 객체'],
  [14, '오류코드 명명규칙', '14. 오류코드'],
  [15, '화면번호 명명규칙', '15. 화면번호'],
  [16, '로그 감사로그 명명규칙', '16. 로그 감사로그'],
  [17, '화면번호와 ServiceId 연결', '17. 화면번호와 ServiceId 연결'],
  [18, 'Gateway 라우팅 명명규칙', '18. Gateway 라우팅'],
  [19, 'Batch Scheduler 명명규칙', '19. Batch Scheduler'],
  [20, 'Cache 명명규칙', '20. Cache'],
];

function findSource(all, matchPart) {
  return all.find(
    (f) =>
      f.endsWith('.docx')
      && f.includes(matchPart)
      && !f.includes(`${PREFIX}`)
      && !/명명규칙 상세 \(\d+\)/.test(f),
  );
}

if (!fs.existsSync(guideDir)) {
  console.error(`Not found: ${guideDir}`);
  process.exit(1);
}

const all = fs.readdirSync(guideDir);
console.log(`${dryRun ? '[DRY-RUN] ' : ''}${guideDir}\n`);

let ok = 0;
const missed = [];

for (const [, matchPart, newTitle] of NAMING_MAP) {
  const srcName = findSource(all, matchPart);
  if (!srcName) {
    missed.push(matchPart);
    continue;
  }
  const destName = `${PREFIX}${newTitle}.docx`;
  if (srcName === destName) {
    console.log(`SKIP: ${destName}`);
    ok++;
    continue;
  }
  const src = path.join(guideDir, srcName);
  const dest = path.join(guideDir, destName);
  if (fs.existsSync(dest) && path.resolve(src) !== path.resolve(dest)) {
    console.log(`CONFLICT: ${srcName} -> ${destName}`);
    continue;
  }
  console.log(`${srcName}`);
  console.log(`  -> ${destName}`);
  if (!dryRun) fs.renameSync(src, dest);
  ok++;
}

console.log(`\nDone: ${ok}/${NAMING_MAP.length}`);
if (missed.length) {
  console.log('Not found:', missed.join(', '));
}
