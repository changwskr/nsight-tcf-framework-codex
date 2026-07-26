#!/usr/bin/env node
'use strict';

const fs = require('fs');
const path = require('path');
const { execSync } = require('child_process');

const root = path.join(__dirname, '..');
const guideDir = path.join(root, 'znsight-guide');
const outDir = __dirname;
const { cacheDir, getRawText, buildDocxMaps } = require('./_docx-map.cjs');

if (!fs.existsSync(path.join(cacheDir, 'docx-82.txt')) || !fs.existsSync(path.join(cacheDir, 'naming-1.txt'))) {
  execSync(`node "${path.join(__dirname, '_extract-docx.cjs')}" "${guideDir}" "${cacheDir}"`, {
    stdio: 'inherit',
  });
}

/** 명명규칙 상세 docx → 병합 대상 장 */
const NAMING_TO_CHAPTER = {
  1: 14, 2: 14, 3: 15, 4: 15, 5: 8, 6: 13, 7: 16,
  8: 14, 9: 14, 10: 14, 11: 18, 12: 28, 13: 14, 14: 14, 15: 14,
  16: 34, 17: 17, 18: 18, 19: 19, 20: 20, 21: 21,
};

const { byChapter: DOCX_BY_CHAPTER, byAppendix: APPENDIX_DOCX } = buildDocxMaps();

function getNamingForChapter(chNum) {
  return Object.entries(NAMING_TO_CHAPTER)
    .filter(([, ch]) => ch === chNum)
    .map(([n]) => parseInt(n, 10))
    .sort((a, b) => a - b);
}

function getDocxText(n) {
  return getRawText('docx', n);
}

const { formatDocxMarkdown } = require('./format-docx-md.cjs');
const { namingLink } = require('./_naming-specs.cjs');
const { getFallbackBody } = require('./_fallback-bodies.cjs');

function applyCorrections(text) {
  return text
    .replace(/17개 업무/g, '9개 업무')
    .replace(/17개 WAR/g, '9개 WAR')
    .replace(/NSIGHT 17개 업무코드/g, 'NSIGHT 업무코드 (settings.gradle 기준)')
    .replace(/19 WAR/g, '13 WAR (deploy-wars.sh)')
    .replace(/19개 WAR/g, '13개 WAR (ztomcat deploy)')
    .replace(/16업무/g, '9업무')
    .replace(/17 WAR 배포/g, '13 WAR 배포 (deploy-wars.sh)')
    .replace(/cc-service|bc-service|cm-service|bp-service|bd-service|cs-service|ct-service/g, '(미포함·확장 예정)')
    .replace(
      /(\| 9 \| 이벤트 \| EB \| Event Batch[\s\S]*?\| \/eb \| eb\.war \|\n)(?:\| 1[0-7] \|[\s\S]*?\| \/om \| om\.war \|\n)+/,
      ''
    );
}

function loadRefContent(refs, max = 14000) {
  if (!refs?.length) return '';
  const parts = [];
  for (const r of refs) {
    const p = path.join(root, r);
    if (fs.existsSync(p)) {
      const text = fs.readFileSync(p, 'utf8').slice(0, max);
      parts.push(`> **보완 출처:** [${path.basename(r)}](../${r})\n\n${text}`);
    }
  }
  return applyCorrections(parts.join('\n\n---\n\n'));
}

function loadDocx(n) {
  if (!n) return '';
  return applyCorrections(formatDocxMarkdown(getDocxText(n)));
}

function loadNaming(n) {
  if (!n) return '';
  return applyCorrections(formatDocxMarkdown(getRawText('naming', n)));
}

function buildNamingLinkSection(chNum) {
  const namingNums = getNamingForChapter(chNum);
  if (!namingNums.length) return '';
  const lines = [
    '---',
    '',
    '## 관련 명명규칙 상세',
    '',
    '세부 명명규칙은 [`명명규칙-00-목차.md`](./명명규칙-00-목차.md)의 분리본을 참조한다.',
    '',
  ];
  for (const n of namingNums) {
    const link = namingLink(n);
    if (link) lines.push(`- ${link}`);
  }
  return lines.join('\n');
}

function buildChapterBody(ch) {
  const parts = [];
  const docxN = DOCX_BY_CHAPTER[ch.num] || 0;
  ch.docx = docxN;
  ch.docxFile = docxN ? `통합 (${docxN}).docx` : null;

  const fromDocx = loadDocx(docxN);
  if (fromDocx.trim()) parts.push(fromDocx);

  const namingNums = getNamingForChapter(ch.num);
  ch.namingNums = namingNums;
  const namingLinks = buildNamingLinkSection(ch.num);
  if (namingLinks) parts.push(namingLinks);

  const fallback = getFallbackBody(ch.num);

  if (fallback && !fromDocx.trim()) {
    parts.unshift(fallback);
    ch.source = docxN ? 'znsight-guide+fallback' : 'fallback';
  } else if (!parts.length) {
    parts.push(loadRefContent(ch.ref));
    ch.source = 'zdoc/zman';
  } else {
    ch.source = docxN ? 'znsight-guide' : 'znsight-guide+naming';
  }

  return parts.join('\n\n');
}

function buildAppendixBody(ap) {
  const docxN = APPENDIX_DOCX[ap.key] || 0;
  ap.docx = docxN;
  ap.docxFile = docxN ? `통합 (${docxN}).docx` : null;
  const fromDocx = loadDocx(docxN);
  if (fromDocx.trim()) {
    ap.source = 'znsight-guide';
    return fromDocx;
  }
  ap.source = 'zdoc/zman';
  return loadRefContent(ap.ref);
}

function buildFooter(ch) {
  const lines = ['## 소스·관련 문서', '', '| 참고 |', '|------|'];
  for (const r of ch.ref || []) lines.push(`| [${path.basename(r)}](../${r}) |`);
  lines.push(
    '',
    '## 코드베이스 정정 (develop 기준)',
    '',
    '| 항목 | 값 |',
    '|------|-----|',
    '| 업무 WAR | ic, pc, ms, sv, pd, eb, ep, ss, mg + tcf-om |',
    '| ztomcat deploy | `ztomcat/deploy-wars.sh` 13 WAR |',
    '| buildZtomcatWars | 15 WAR |',
    '| bootRun | gateway 8100, uj 8102, jwt 8110, ui 8099 |',
    ''
  );
  if (!ch.docx) {
    const note = ch.source === 'fallback' || ch.source === 'znsight-guide+fallback'
      ? `> docx 본문 없음 (${ch.source} 보완)`
      : `> docx 본문 없음 (${ch.source || 'zdoc/zman'} 보완)`;
    lines.splice(4, 0, '', note, '');
  } else {
    lines.splice(4, 0, '', `> znsight-guide: \`${ch.docxFile || '통합 docx'}\`${ch.namingNums?.length ? ` + 명명규칙 상세 ${ch.namingNums.join(',')}` : ''}`, '');
  }
  return lines.join('\n');
}

const chapters = [
  { num: 1, title: '문서 개요', file: '01-문서개요', ref: ['zman/01-문서개요.md', 'znsight-man/README.md'] },
  { num: 2, title: '개발자가 알아야 할 NSIGHT 기본 구조', file: '02-NSIGHT-기본구조', ref: ['zman/03-전체아키텍처.md', 'zman/04-모듈구성.md'] },
  { num: 3, title: 'TCF Framework 개발 원칙', file: '03-TCF-개발원칙', ref: ['zdoc/TCF.md', 'zman/05-TCF처리구조.md'] },
  { num: 4, title: '개발 표준 전체 요약', file: '04-개발표준-전체요약', ref: ['zdoc/applicationNaming.md'] },
  { num: 5, title: '개발자 역할과 책임', file: '05-개발자-역할과-책임', ref: ['zdoc/어플리케이션계층.md'] },
  { num: 6, title: '로컬 개발환경 구성', file: '06-로컬-개발환경-구성', ref: ['zdoc/환경구성.md', 'zdoc/솔루션환경구성.md'] },
  { num: 7, title: 'Git 저장소 및 브랜치 사용 기준', file: '07-Git-브랜치-기준', ref: ['zman/21-CICD-배포.md'] },
  { num: 8, title: 'Gradle 멀티 모듈 구조', file: '08-Gradle-멀티모듈', ref: ['docs/architecture/22-build-project.md'] },
  { num: 9, title: '업무 WAR 구조', file: '09-업무-WAR-구조', ref: ['docs/architecture/16-deploy.md'] },
  { num: 10, title: 'bootRun / Tomcat WAR 배포 차이', file: '10-bootRun-Tomcat-WAR-차이', ref: ['zman/21-CICD-배포.md'] },
  { num: 11, title: 'application.yml 구성 기준', file: '11-application-yml-기준', ref: ['zman/20-Spring환경설정.md'] },
  { num: 12, title: '애플리케이션 계층구조', file: '12-애플리케이션-계층구조', ref: ['zdoc/어플리케이션계층.md'] },
  { num: 13, title: '패키지 구조 표준', file: '13-패키지-구조-표준', ref: ['zdoc/applicationNaming.md'] },
  { num: 14, title: '명명 규칙', file: '14-명명-규칙', ref: ['zdoc/네이밍.md', 'zdoc/applicationNaming.md'] },
  { num: 15, title: '업무코드 / Context / WAR 기준', file: '15-업무코드-Context-WAR', ref: ['zdoc/applicationNaming.md'] },
  { num: 16, title: 'ServiceId 설계 기준', file: '16-ServiceId-설계', ref: ['zman/07-ServiceIdDispatcher.md'] },
  { num: 17, title: '거래코드 설계 기준', file: '17-거래코드-설계', ref: ['zdoc/applicationNaming.md'] },
  { num: 18, title: 'DTO 작성 기준', file: '18-DTO-작성-기준', ref: ['zdoc/applicationNaming.md'] },
  { num: 19, title: 'Validation 작성 기준', file: '19-Validation-작성-기준', ref: ['zdoc/어플리케이션계층.md'] },
  { num: 20, title: '표준 전문 구조', file: '20-표준-전문-구조', ref: ['zdoc/전문관리.md', 'zman/06-표준전문구조.md'] },
  { num: 21, title: 'Header 작성 기준', file: '21-Header-작성-기준', ref: ['zdoc/전문관리.md'] },
  { num: 22, title: 'Online Endpoint 기준', file: '22-Online-Endpoint-기준', ref: ['zdoc/온라인처리.md'] },
  { num: 23, title: 'TransactionHandler 개발', file: '23-TransactionHandler-개발', ref: ['zman/08-업무Handler개발.md'] },
  { num: 24, title: 'Facade 개발', file: '24-Facade-개발', ref: ['zdoc/어플리케이션계층.md'] },
  { num: 25, title: 'Service 개발', file: '25-Service-개발', ref: ['zdoc/어플리케이션계층.md'] },
  { num: 26, title: 'Rule 개발', file: '26-Rule-개발', ref: ['zdoc/어플리케이션계층.md'] },
  { num: 27, title: 'DAO 개발', file: '27-DAO-개발', ref: ['zdoc/DAO처리.md'] },
  { num: 28, title: 'MyBatis Mapper 개발', file: '28-MyBatis-Mapper-개발', ref: ['zdoc/mybatisNaming.md'] },
  { num: 29, title: 'SQL 작성 기준', file: '29-SQL-작성-기준', ref: ['zdoc/mybatisNaming.md', 'zdoc/DAO처리.md'] },
  { num: 30, title: '페이징 처리 기준', file: '30-페이징-처리-기준', ref: ['zdoc/업무페이징.md', 'zdoc/sv-service-페이징-가이드.md'] },
  { num: 31, title: '서비스 간 연동 개발', file: '31-서비스간-연동-개발', ref: ['zdoc/서비스간연동.md', 'zguide/tcf-eai-개발가이드.md'] },
  { num: 32, title: '예외처리 기준', file: '32-예외처리-기준', ref: ['zdoc/예외처리.md'] },
  { num: 33, title: '오류코드 / 메시지 기준', file: '33-오류코드-메시지-기준', ref: ['zdoc/예외처리.md'] },
  { num: 34, title: '로그 작성 기준', file: '34-로그-작성-기준', ref: ['zman/15-거래로그-감사로그.md'] },
  { num: 35, title: '거래로그 / 감사로그 기준', file: '35-거래로그-감사로그-기준', ref: ['zman/15-거래로그-감사로그.md'] },
  { num: 36, title: '트랜잭션 기준', file: '36-트랜잭션-기준', ref: ['zdoc/어플리케이션계층.md'] },
  { num: 37, title: 'Timeout 기준', file: '37-Timeout-기준', ref: ['zdoc/타임아웃관리.md', 'zman/14-Timeout관리.md'] },
  { num: 38, title: 'Idempotency / 중복요청 처리', file: '38-Idempotency-중복요청', ref: ['zman/13-거래통제.md'] },
  { num: 39, title: '세션 사용 기준', file: '39-세션-사용-기준', ref: ['zdoc/세션관리.md', 'zman/10-세션관리.md'] },
  { num: 40, title: '권한 검증 기준', file: '40-권한-검증-기준', ref: ['zman/13-거래통제.md', 'zdoc/로그인.md'] },
  { num: 41, title: 'JWT / SSO 연계 기준', file: '41-JWT-SSO-연계', ref: ['zdoc/SSO-TOKEN처리.md', 'zman/11-JWT-SSO인증.md', 'zguide/tcf-jwt-개발가이드.md', 'zguide/tcf-gateway-개발가이드.md'] },
  { num: 42, title: '보안 코딩 기준', file: '42-보안-코딩-기준', ref: [] },
  { num: 43, title: 'Cache 사용 기준', file: '43-Cache-사용-기준', ref: ['zdoc/캐시관리.md', 'zman/16-Cache구조.md'] },
  { num: 44, title: '파일 업다운로드 기준', file: '44-파일-업다운로드-기준', ref: ['zman/18-파일업다운로드.md'] },
  { num: 45, title: 'Batch / Scheduler 개발 기준', file: '45-Batch-Scheduler-개발', ref: ['zdoc/배치관리.md', 'zdoc/스케줄러.md', 'zman/17-Batch-Scheduler.md'] },
  { num: 46, title: 'OM 운영관리 개발 구조', file: '46-OM-운영관리-개발', ref: ['zman/12-OM운영관리.md', 'zguide/tcf-om-개발가이드.md'] },
  { num: 47, title: 'ServiceId 등록 절차', file: '47-ServiceId-등록-절차', ref: ['zman/12-OM운영관리.md'] },
  { num: 48, title: '거래통제 등록 절차', file: '48-거래통제-등록-절차', ref: ['zman/13-거래통제.md'] },
  { num: 49, title: 'Timeout 정책 등록 절차', file: '49-Timeout-정책-등록', ref: ['zman/14-Timeout관리.md'] },
  { num: 50, title: '공통코드 사용 절차', file: '50-공통코드-사용-절차', ref: ['zman/12-OM운영관리.md'] },
  { num: 51, title: '오류코드 등록 절차', file: '51-오류코드-등록-절차', ref: ['zdoc/예외처리.md'] },
  { num: 52, title: '배포관리 연계', file: '52-배포관리-연계', ref: ['zman/21-CICD-배포.md'] },
  { num: 53, title: '환경설정 조회 연계', file: '53-환경설정-조회-연계', ref: ['zman/20-Spring환경설정.md'] },
  { num: 54, title: '단위 테스트 기준', file: '54-단위-테스트-기준', ref: [] },
  { num: 55, title: '통합 테스트 기준', file: '55-통합-테스트-기준', ref: [] },
  { num: 56, title: 'TCF 거래 테스트 기준', file: '56-TCF-거래-테스트-기준', ref: ['zdoc/온라인처리.md'] },
  { num: 57, title: 'MyBatis SQL 테스트 기준', file: '57-MyBatis-SQL-테스트-기준', ref: ['zdoc/DAO처리.md'] },
  { num: 58, title: '보안 테스트 기준', file: '58-보안-테스트-기준', ref: [] },
  { num: 59, title: '성능 테스트 기준', file: '59-성능-테스트-기준', ref: [] },
  { num: 60, title: '장애 테스트 기준', file: '60-장애-테스트-기준', ref: [] },
  { num: 61, title: '코드 리뷰 기준', file: '61-코드-리뷰-기준', ref: [] },
  { num: 62, title: '품질 게이트 기준', file: '62-품질-게이트-기준', ref: [] },
  { num: 63, title: '로컬 빌드 방법', file: '63-로컬-빌드-방법', ref: ['docs/architecture/22-build-project.md', 'docs/manual/artifacts.md'] },
  { num: 64, title: 'WAR 생성 기준', file: '64-WAR-생성-기준', ref: ['docs/manual/artifacts.md'] },
  { num: 65, title: 'CI/CD 파이프라인 기준', file: '65-CICD-파이프라인-기준', ref: ['zguide/tcf-cicd-개발가이드.md', 'zman/21-CICD-배포.md'] },
  { num: 66, title: '배포 절차', file: '66-배포-절차', ref: ['zman/21-CICD-배포.md', 'docs/architecture/16-deploy.md'] },
  { num: 67, title: '롤백 절차', file: '67-롤백-절차', ref: ['zman/21-CICD-배포.md'] },
  { num: 68, title: '운영 전환 체크리스트', file: '68-운영-전환-체크리스트', ref: [] },
  { num: 69, title: '장애 발생 시 개발자 확인 항목', file: '69-장애-개발자-확인-항목', ref: ['znsight-man/70-FAQ-Troubleshooting.md'] },
  { num: 70, title: 'FAQ / Troubleshooting', file: '70-FAQ-Troubleshooting', ref: [] },
  { num: 71, title: 'SV 고객요약조회 샘플', file: '71-SV-고객요약조회-샘플', ref: ['zdoc/SV고객요약샘플.md', 'zman/22-업무서비스샘플.md'] },
  { num: 72, title: '목록조회 + 페이징 샘플', file: '72-목록조회-페이징-샘플', ref: ['zdoc/sv-service-페이징-가이드.md'] },
  { num: 73, title: '등록/변경 거래 샘플', file: '73-등록변경-거래-샘플', ref: ['zdoc/어플리케이션계층.md'] },
  { num: 74, title: '외부 서비스 호출 샘플', file: '74-외부-서비스-호출-샘플', ref: ['zdoc/서비스간연동.md'] },
  { num: 75, title: '파일 다운로드 샘플', file: '75-파일-다운로드-샘플', ref: ['zman/18-파일업다운로드.md'] },
  { num: 76, title: 'Batch Job 샘플', file: '76-Batch-Job-샘플', ref: ['zdoc/배치관리.md'] },
  { num: 77, title: '오류처리 샘플', file: '77-오류처리-샘플', ref: ['zdoc/예외처리.md'] },
  { num: 78, title: '테스트 코드 샘플', file: '78-테스트-코드-샘플', ref: [] },
];

const appendices = [
  { key: 'A', title: '업무코드 표준표', file: '부록A-업무코드-표준표', ref: ['zdoc/applicationNaming.md'] },
  { key: 'B', title: 'ServiceId 명명 규칙', file: '부록B-ServiceId-명명규칙', ref: ['zdoc/applicationNaming.md', 'zman/07-ServiceIdDispatcher.md'] },
  { key: 'C', title: '거래코드 명명 규칙', file: '부록C-거래코드-명명규칙', ref: ['zdoc/applicationNaming.md'] },
  { key: 'D', title: '표준 전문 예시', file: '부록D-표준-전문-예시', ref: ['zdoc/전문관리.md'] },
  { key: 'E', title: 'Mapper XML 템플릿', file: '부록E-Mapper-XML-템플릿', ref: ['zdoc/mybatisNaming.md'] },
  { key: 'F', title: '오류코드 표준표', file: '부록F-오류코드-표준표', ref: ['zdoc/예외처리.md'] },
  { key: 'G', title: 'application.yml 템플릿', file: '부록G-application-yml-템플릿', ref: ['zman/20-Spring환경설정.md'] },
  { key: 'H', title: '개발 완료 체크리스트', file: '부록H-개발-완료-체크리스트', ref: ['zdoc/SV고객요약샘플.md'] },
  { key: 'I', title: '코드 리뷰 체크리스트', file: '부록I-코드-리뷰-체크리스트', ref: ['znsight-man/61-코드-리뷰-기준.md'] },
  { key: 'J', title: '운영 전환 체크리스트', file: '부록J-운영-전환-체크리스트', ref: ['znsight-man/68-운영-전환-체크리스트.md'] },
];

for (const ch of chapters) {
  const body = buildChapterBody(ch);
  const prev = chapters.find((c) => c.num === ch.num - 1);
  const next = chapters.find((c) => c.num === ch.num + 1);
  const nav = [];
  if (prev) nav.push(`← [${prev.num}. ${prev.title}](./${prev.file}.md)`);
  if (next) nav.push(`[${next.num}. ${next.title}](./${next.file}.md) →`);
  const md = [
    `# ${ch.num}. ${ch.title}`,
    '',
    '> **NSIGHT TCF 개발 Manual** · 원본: [`znsight-guide`](../znsight-guide/) · 갱신: 2026-07-05',
    '',
    body,
    '',
    buildFooter(ch),
    '',
    '---',
    '',
    nav.join(' · '),
  ].join('\n');
  fs.writeFileSync(path.join(outDir, `${ch.file}.md`), md, 'utf8');
}

for (const ap of appendices) {
  const body = buildAppendixBody(ap);
  const md = [
    `# 부록 ${ap.key}. ${ap.title}`,
    '',
    '> **NSIGHT TCF 개발 Manual** · 원본: [`znsight-guide`](../znsight-guide/) · 갱신: 2026-07-05',
    '',
    body,
    '',
    buildFooter(ap),
    '',
    '---',
    '',
    '[← 78. 테스트 코드 샘플](./78-테스트-코드-샘플.md) · [README](./README.md)',
  ].join('\n');
  fs.writeFileSync(path.join(outDir, `${ap.file}.md`), md, 'utf8');
}

// README (same structure as before)
const sections = [
  { name: '개요·원칙', from: 1, to: 5 },
  { name: '환경·빌드', from: 6, to: 11 },
  { name: '구조·명명', from: 12, to: 19 },
  { name: '전문·계층 개발', from: 20, to: 31 },
  { name: '품질·보안·운영', from: 32, to: 45 },
  { name: 'OM 등록 절차', from: 46, to: 53 },
  { name: '테스트·품질', from: 54, to: 62 },
  { name: '빌드·배포', from: 63, to: 70 },
  { name: '샘플', from: 71, to: 78 },
];

let readme = `# NSIGHT TCF 개발 Manual

> **원본:** [\`znsight-guide\`](../znsight-guide/) (docx) · **대조:** [\`zdoc\`](../zdoc/) · [\`zman\`](../zman/) · [\`zguide\`](../zguide/) · **코드:** \`develop\`  
> **갱신:** 2026-07-05 — \`znsight-guide\` docx 본문 + zdoc/zman 보완

NSIGHT TCF Framework **업무 개발자용 통합 매뉴얼**입니다. **78장 + 부록 10개**가 각각 별도 Markdown 파일입니다.

---

## 빠른 시작

| 목적 | 읽을 장 |
|------|---------|
| 처음 온보딩 | [1](./01-문서개요.md) → [2](./02-NSIGHT-기본구조.md) → [12](./12-애플리케이션-계층구조.md) |
| 거래 개발 | [16](./16-ServiceId-설계.md) → [23](./23-TransactionHandler-개발.md) → [71](./71-SV-고객요약조회-샘플.md) |
| 로컬 실행 | [6](./06-로컬-개발환경-구성.md) → [63](./63-로컬-빌드-방법.md) |
| OM 등록 | [47](./47-ServiceId-등록-절차.md) ~ [51](./51-오류코드-등록-절차.md) |
| 배포 | [64](./64-WAR-생성-기준.md) → [66](./66-배포-절차.md) |

---

## 전체 목차

`;

for (const sec of sections) {
  readme += `\n### ${sec.name}\n\n| 장 | 제목 | 파일 |\n|:--:|------|------|\n`;
  for (const ch of chapters.filter((c) => c.num >= sec.from && c.num <= sec.to)) {
    readme += `| ${ch.num} | ${ch.title} | [${ch.file}.md](./${ch.file}.md) |\n`;
  }
}

readme += `\n### 부록\n\n| 부록 | 제목 | 파일 |\n|:--:|------|------|\n`;
for (const ap of appendices) {
  readme += `| ${ap.key} | ${ap.title} | [${ap.file}.md](./${ap.file}.md) |\n`;
}

readme += `
---

## 코드베이스 핵심 (develop)

| 항목 | 값 |
|------|-----|
| 업무 WAR | 9개 + tcf-om |
| ztomcat deploy | 13 WAR (\`deploy-wars.sh\`) |
| buildZtomcatWars | 15 WAR |
| 온라인 | \`POST /{bc}/online\` + \`serviceId\` |
| 6계층 | Handler → Facade → Service → Rule → DAO → Mapper |

---

## 관련

- [\`zdoc/\`](../zdoc/) · [\`zman/\`](../zman/) · [\`zguide/\`](../zguide/) · [\`docs/architecture/\`](../docs/architecture/)

---

## 재생성

\`znsight-guide\` docx 갱신 후:

\`\`\`bash
cd znsight-man
node _extract-docx.cjs ../znsight-guide ./_docx-cache
node _generate.cjs
node _generate-naming.cjs
node _build-index.cjs
\`\`\`
`;

fs.writeFileSync(path.join(outDir, 'README.md'), readme, 'utf8');

const mapped = Object.keys(DOCX_BY_CHAPTER).length;
const missing = chapters.filter((c) => !DOCX_BY_CHAPTER[c.num]).map((c) => c.num);
console.log(`Done: ${chapters.length} chapters, ${appendices.length} appendices`);
console.log(`Guide mapped: ${mapped}/78 chapters`, missing.length ? `(missing: ${missing.join(', ')})` : '');
