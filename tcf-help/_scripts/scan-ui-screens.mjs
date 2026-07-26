#!/usr/bin/env node
/**
 * tcf-ui 정적 HTML 스캔 → help-business-map.json
 * 업무·OM·JWT 화면 HELP 딥링크 자동 생성
 */
import fs from 'fs';
import path from 'path';
import { fileURLToPath } from 'url';
import { loadScreenOverrides, applyScreenOverrides } from './screen-overrides.mjs';

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const tcfHelpRoot = path.resolve(__dirname, '..');
const repoRoot = path.resolve(tcfHelpRoot, '..');
const uiStatic = path.join(repoRoot, 'tcf-ui', 'src', 'main', 'resources', 'static');
const outPath = path.join(tcfHelpRoot, 'help-business-map.json');

/** 업무코드(소문자) → zguide 원문 */
const CODE_ZGUIDE = {
  sv: 'zguide/sv-service-개발가이드.md',
  eb: 'zguide/eb-service-개발가이드.md',
  ic: 'zguide/ic-service-개발가이드.md',
  ep: 'zguide/ep-service-개발가이드.md',
  ms: 'zguide/ms-service-개발가이드.md',
  mg: 'zguide/mg-service-개발가이드.md',
  pc: 'zguide/pc-service-개발가이드.md',
  pd: 'zguide/pd-service-개발가이드.md',
  ss: 'zguide/ss-service-개발가이드.md',
  om: 'zguide/om-service-개발가이드.md',
  jwt: 'zguide/tcf-jwt-개발가이드.md'
};

const DOC = {
  business: { docId: 'business-modules', title: '업무 모듈 테스트' },
  omAdmin: { docId: 'om-admin', title: 'OM 운영관리' },
  jwtGuide: { docId: 'business-modules', title: 'JWT·인증', src: 'zguide/tcf-jwt-개발가이드.md' },
  relay: { docId: 'relay-usage', title: 'Relay 사용법' },
  error: { docId: 'error-response', title: '오류 응답 구조' }
};

function walkHtml(dir, base = uiStatic, files = []) {
  if (!fs.existsSync(dir)) return files;
  for (const ent of fs.readdirSync(dir, { withFileTypes: true })) {
    const full = path.join(dir, ent.name);
    const rel = '/' + path.relative(base, full).replace(/\\/g, '/');
    if (ent.isDirectory()) {
      if (ent.name === 'help' || ent.name === '_shared') continue;
      walkHtml(full, base, files);
    } else if (ent.isFile() && ent.name.endsWith('.html')) {
      files.push(rel);
    }
  }
  return files;
}

function titleFromPath(rel) {
  const base = path.basename(rel, '.html');
  if (base === 'index') return '단일 서비스 테스트';
  if (base === 'index-multi') return '다중 서비스 테스트';
  return base.replace(/-/g, ' ');
}

function mapScreen(rel) {
  const omAdmin = rel.match(/^\/om\/admin\/(.+)\.html$/);
  if (omAdmin) {
    return {
      ...DOC.omAdmin,
      title: `OM Admin · ${titleFromPath(rel)}`,
      screen: rel
    };
  }

  const jwtAdmin = rel.match(/^\/jwt\/admin\/(.+)\.html$/);
  if (jwtAdmin) {
    return { ...DOC.jwtGuide, title: `JWT Admin · ${titleFromPath(rel)}`, screen: rel };
  }

  const biz = rel.match(/^\/([a-z]{2})\/(index(?:-multi)?|sample-list|user-management|event-monitor|updownload)\.html$/);
  if (biz) {
    const code = biz[1];
    const zguide = CODE_ZGUIDE[code];
    const entry = {
      ...DOC.business,
      title: `${code.toUpperCase()} · ${titleFromPath(rel)}`,
      screen: rel,
      module: code
    };
    if (zguide && fs.existsSync(path.join(repoRoot, zguide))) {
      entry.src = zguide;
      entry.title = `${code.toUpperCase()} 개발 가이드`;
    }
    return entry;
  }

  if (rel === '/index-multi.html') {
    return { ...DOC.business, title: '다중 업무 테스트', screen: rel };
  }

  if (rel === '/updownload.html' || rel === '/ud/updownload.html') {
    return { ...DOC.business, title: '파일 업·다운로드', screen: rel };
  }

  return null;
}

const htmlFiles = walkHtml(uiStatic).sort();
const screens = {};
const patterns = [
  {
    match: '^/([a-z]{2})/index(-multi)?\\.html$',
    ...DOC.business,
    title: '업무 모듈 테스트 (패턴)',
    note: '코드별 zguide는 screens 항목 우선'
  },
  {
    match: '^/om/admin/.+\\.html$',
    ...DOC.omAdmin,
    title: 'OM 운영관리 (패턴)'
  },
  {
    match: '^/jwt/admin/.+\\.html$',
    ...DOC.jwtGuide,
    title: 'JWT Admin (패턴)'
  }
];

for (const rel of htmlFiles) {
  const mapped = mapScreen(rel);
  if (mapped) {
    const { screen, ...rest } = mapped;
    screens[screen] = rest;
  }
}

const overrides = loadScreenOverrides(tcfHelpRoot);
const mergedScreens = applyScreenOverrides(screens, overrides);

const catalog = {
  version: '1.1',
  generatedAt: new Date().toISOString(),
  source: 'tcf-ui/src/main/resources/static',
  total: Object.keys(mergedScreens).length,
  overrideCount: Object.keys(overrides.entries || {}).length,
  screens: mergedScreens,
  patterns
};

fs.writeFileSync(outPath, JSON.stringify(catalog, null, 2), 'utf8');
console.log(`[scan-ui-screens] ${htmlFiles.length} html → ${catalog.total} business screens → ${outPath}`);
