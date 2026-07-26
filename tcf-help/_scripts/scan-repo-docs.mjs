#!/usr/bin/env node
/**
 * 저장소 전체 .md 스캔 → doc-catalog.json
 * Usage: node _scripts/scan-repo-docs.mjs
 */
import fs from 'fs';
import path from 'path';
import { fileURLToPath } from 'url';
import { loadCatalogOverrides, parseInlineList, stripQuotes } from './catalog-overrides.mjs';

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const tcfHelpRoot = path.resolve(__dirname, '..');
const repoRoot = path.resolve(tcfHelpRoot, '..');
const outPath = path.join(tcfHelpRoot, 'doc-catalog.json');

const EXCLUDE_DIR = new Set([
  '.git', '.gradle', '.idea', 'node_modules', 'build', 'bin', 'out',
  '_extract', '.tmp-docx', '.tmp-docx-zman', '.tmp-docx-om-simple'
]);

const EXCLUDE_PATH_RE = [
  /[/\\]build[/\\]/,
  /[/\\]_extract[/\\]/,
  /[/\\]_tmp_/,
  /[/\\]\.gradle[/\\]/,
  /[/\\]tcf-help[/\\]build[/\\]/
];

/** 코퍼스 정의 — 우선순위 순 */
const CORPORA = [
  {
    id: 'help-meta',
    title: 'HELP 문서 지도',
    description: '저장소 문서 코퍼스별 안내·읽는 순서',
    audience: ['developer', 'operator', 'architect'],
    order: 0,
    match: p => p.startsWith('tcf-help/meta/'),
    meta: 'meta/00-문서-지도.md',
    metaDocId: 'doc-map'
  },
  {
    id: 'help-native',
    title: 'HELP 운영 가이드',
    description: 'tcf-ui HELP 화면용 짧은 운영·화면 가이드',
    audience: ['operator', 'developer'],
    order: 1,
    match: p => p.startsWith('tcf-help/docs/'),
    meta: 'meta/help-native-개요.md',
    metaDocId: 'meta-help-native'
  },
  {
    id: 'zguide',
    title: 'zguide — 모듈별 개발 가이드',
    description: '업무·플랫폼 모듈 bootRun, Handler, Relay 시작 가이드',
    audience: ['developer'],
    order: 2,
    match: p => p.startsWith('zguide/'),
    meta: 'meta/zguide-개요.md',
    metaDocId: 'meta-zguide'
  },
  {
    id: 'zman',
    title: 'zman — 설계·코드베이스 대조',
    description: '설계서와 소스 대조, Handler·모듈·DB 구조',
    audience: ['architect', 'developer'],
    order: 3,
    match: p => p.startsWith('zman/'),
    meta: 'meta/zman-개요.md',
    metaDocId: 'meta-zman'
  },
  {
    id: 'zdoc',
    title: 'zdoc — 기능·운영 설명',
    description: '세션, SSO, 페이징, 환경구성 등 운영 기능 문서',
    audience: ['developer', 'operator'],
    order: 4,
    match: p => p.startsWith('zdoc/'),
    meta: 'meta/zdoc-개요.md',
    metaDocId: 'meta-zdoc'
  },
  {
    id: 'znsight-man',
    title: 'znsight-man — 개발 표준·매뉴얼',
    description: 'ServiceId, DTO, 테스트, 배포, 보안, 명명규칙 등 표준',
    audience: ['developer', 'qa'],
    order: 5,
    match: p => p.startsWith('znsight-man/'),
    meta: 'meta/znsight-man-개요.md',
    metaDocId: 'meta-znsight-man'
  },
  {
    id: 'capacity',
    title: 'ztcf-book-capacity-md — 용량·환경설정',
    description: 'TPS, VM, Tomcat, Hikari, Timeout 계층 등 용량산정 가이드',
    audience: ['architect', 'capacity'],
    order: 6,
    match: p => p.startsWith('ztcf-book-capacity-md/'),
    meta: 'meta/capacity-개요.md',
    metaDocId: 'meta-capacity'
  },
  {
    id: 'architecture',
    title: 'docs · zarchitecture — 아키텍처',
    description: '시스템·배포·Gateway·환경 아키텍처 설계 문서',
    audience: ['architect'],
    order: 7,
    match: p => p.startsWith('docs/') || p.startsWith('zarchitecture/'),
    meta: 'meta/architecture-개요.md',
    metaDocId: 'meta-architecture'
  },
  {
    id: 'ztcfbook',
    title: 'ztcfbook — TCF 교재 (표준)',
    description: 'NSIGHT TCF 학습 교재 본편',
    audience: ['learner', 'developer'],
    order: 8,
    match: p => p.startsWith('ztcfbook/') && !p.startsWith('ztcfbook-m/') && !p.startsWith('ztcfbook-h/'),
    meta: 'meta/ztcfbook-개요.md',
    metaDocId: 'meta-ztcfbook'
  },
  {
    id: 'ztcfbook-m',
    title: 'ztcfbook-m — TCF 교재 (쉬운 버전)',
    description: '입문자용 쉬운 TCF 교재',
    audience: ['learner'],
    order: 9,
    match: p => p.startsWith('ztcfbook-m/'),
    meta: 'meta/ztcfbook-m-개요.md',
    metaDocId: 'meta-ztcfbook-m'
  },
  {
    id: 'ztcfbook-h',
    title: 'ztcfbook-h — TCF 교재 (상세)',
    description: '상세·심화 TCF 교재',
    audience: ['learner', 'architect'],
    order: 10,
    match: p => p.startsWith('ztcfbook-h/'),
    meta: 'meta/ztcfbook-h-개요.md',
    metaDocId: 'meta-ztcfbook-h'
  },
  {
    id: 'config',
    title: 'znsight-config — 설정 참고',
    description: 'Apache, Tomcat, 운영 설정 참고 문서',
    audience: ['operator', 'architect'],
    order: 11,
    match: p => p.startsWith('znsight-config-info/') || p.startsWith('znsight-config-value-word/'),
    meta: 'meta/config-개요.md',
    metaDocId: 'meta-config'
  },
  {
    id: 'module-readme',
    title: '모듈 README',
    description: 'Gradle 모듈·업무 WAR별 README',
    audience: ['developer'],
    order: 12,
    match: p => /^[^/]+\/README\.md$/.test(p) && !p.startsWith('tcf-help/'),
    meta: 'meta/module-readme-개요.md',
    metaDocId: 'meta-module-readme'
  },
  {
    id: 'infra',
    title: '인프라·도구',
    description: 'ztomcat, tcf-gateway docs, tcf-scripts, tcf-cicd',
    audience: ['operator', 'developer'],
    order: 13,
    match: p => p.startsWith('ztomcat/') || p.startsWith('tcf-gateway/docs/') ||
      p.startsWith('tcf-scripts/') || p.startsWith('tcf-cicd/') || p.startsWith('zguide/') === false &&
      (p.startsWith('tcf-gateway/') || p.startsWith('eb-service/') && false),
    meta: 'meta/infra-개요.md',
    metaDocId: 'meta-infra'
  },
  {
    id: 'help-internal',
    title: 'tcf-help 내부',
    description: 'HELP 모듈 README·메타 (개발용)',
    audience: ['developer'],
    order: 14,
    match: p => p.startsWith('tcf-help/') && !p.startsWith('tcf-help/docs/') && !p.startsWith('tcf-help/meta/'),
    meta: null
  },
  {
    id: 'root',
    title: '루트·기타',
    description: '저장소 루트 및 기타 문서',
    audience: ['developer'],
    order: 99,
    match: () => true,
    meta: 'meta/root-개요.md',
    metaDocId: 'meta-root'
  }
];

function shouldSkipDir(name) {
  return EXCLUDE_DIR.has(name) || name.startsWith('.tmp-docx');
}

function shouldSkipFile(relPath) {
  return EXCLUDE_PATH_RE.some(re => re.test(relPath.replace(/\//g, path.sep)));
}

function walk(dir, base = repoRoot, files = []) {
  for (const ent of fs.readdirSync(dir, { withFileTypes: true })) {
    if (ent.name.startsWith('.') && ent.name !== '.github') continue;
    const full = path.join(dir, ent.name);
    const rel = path.relative(base, full).replace(/\\/g, '/');
    if (ent.isDirectory()) {
      if (shouldSkipDir(ent.name)) continue;
      if (shouldSkipFile(rel + '/')) continue;
      walk(full, base, files);
    } else if (ent.isFile() && ent.name.toLowerCase().endsWith('.md')) {
      if (!shouldSkipFile(rel)) files.push(rel);
    }
  }
  return files;
}

function parseFrontmatter(text) {
  if (!text.startsWith('---')) return { meta: {}, body: text };
  const end = text.indexOf('\n---', 3);
  if (end < 0) return { meta: {}, body: text };
  const raw = text.slice(4, end).trim();
  const body = text.slice(end + 4).replace(/^\s*\n/, '');
  const meta = {};
  for (const line of raw.split('\n')) {
    const i = line.indexOf(':');
    if (i <= 0) continue;
    const key = line.slice(0, i).trim();
    const val = line.slice(i + 1).trim();
    if (key === 'tags' && val.startsWith('[')) {
      meta.tags = parseInlineList(val);
    } else {
      meta[key] = stripQuotes(val);
    }
  }
  return { meta, body };
}

function parseTags(meta) {
  if (!meta.tags) return [];
  if (Array.isArray(meta.tags)) return meta.tags;
  const s = String(meta.tags).trim();
  if (s.startsWith('[')) return parseInlineList(s);
  return s.split(/[,\s]+/).filter(Boolean);
}

function applyOverride(entry, ov) {
  if (!ov) return;
  if (ov.title) entry.title = ov.title;
  if (ov.summary) entry.summary = ov.summary;
  if (ov.tags?.length) entry.tags = ov.tags;
  if (ov.featured) entry.featured = true;
  if (ov.priority != null) entry.priority = ov.priority;
}

function extractTitle(body, meta, filePath) {
  if (meta.title) return meta.title;
  const m = body.match(/^#\s+(.+)$/m);
  if (m) return m[1].trim();
  const base = path.basename(filePath, '.md');
  return base.replace(/-/g, ' ');
}

function extractSummary(body, meta) {
  if (meta.description || meta.summary) return (meta.description || meta.summary).slice(0, 200);
  const lines = body.split('\n');
  for (const line of lines) {
    const t = line.trim();
    if (!t || t.startsWith('#') || t.startsWith('```') || t.startsWith('|')) continue;
    if (t.startsWith('[') && t.includes('](')) continue;
    return t.replace(/\*\*/g, '').slice(0, 200);
  }
  return '';
}

function classifyCorpus(relPath) {
  for (const c of CORPORA) {
    if (c.id === 'root') continue;
    if (c.match(relPath)) return c;
  }
  if (relPath === 'README.md') return CORPORA.find(c => c.id === 'root');
  return CORPORA.find(c => c.id === 'root');
}

function slugId(relPath) {
  return relPath.replace(/\.md$/i, '').replace(/[/\\]/g, '--').replace(/[^a-zA-Z0-9가-힣_-]/g, '-');
}

function detectModule(relPath) {
  const m = relPath.match(/^([^/]+)\/README\.md$/);
  if (m) return m[1];
  const m2 = relPath.match(/^(tcf-[^/]+|[^/]+-service)\//);
  return m2 ? m2[1] : null;
}

function fixInfraMatch() {
  // infra corpus: ztomcat, tcf-gateway, tcf-scripts, tcf-cicd only
  const infra = CORPORA.find(c => c.id === 'infra');
  infra.match = p =>
    p.startsWith('ztomcat/') ||
    p.startsWith('tcf-gateway/') ||
    p.startsWith('tcf-scripts/') ||
    p.startsWith('tcf-cicd/');
}

fixInfraMatch();

const overrides = loadCatalogOverrides(tcfHelpRoot);
const allPaths = walk(repoRoot).sort();
const entries = [];
const corpusCounts = {};

for (const rel of allPaths) {
  const full = path.join(repoRoot, rel);
  let text;
  try {
    text = fs.readFileSync(full, 'utf8');
  } catch {
    continue;
  }
  const { meta, body } = parseFrontmatter(text);
  const corpus = classifyCorpus(rel);
  corpusCounts[corpus.id] = (corpusCounts[corpus.id] || 0) + 1;

  const entry = {
    id: slugId(rel),
    corpus: corpus.id,
    source: rel,
    title: extractTitle(body, meta, rel),
    summary: extractSummary(body, meta),
    module: detectModule(rel) || meta.module || null,
    audience: corpus.audience,
    tags: parseTags(meta),
    featured: false,
    priority: 0
  };
  applyOverride(entry, overrides.entries[rel]);
  entries.push(entry);
}

// Reclassify unmatched to root (only truly orphan paths)
for (const e of entries) {
  if (e.corpus === 'root' && e.source !== 'README.md') {
    const better = CORPORA.find(c => c.id !== 'root' && c.match(e.source));
    if (better) {
      corpusCounts.root = (corpusCounts.root || 1) - 1;
      e.corpus = better.id;
      e.audience = better.audience;
      corpusCounts[better.id] = (corpusCounts[better.id] || 0) + 1;
    }
  }
}

const corpora = CORPORA.filter(c => c.id !== 'root' || corpusCounts.root)
  .map(c => ({
    id: c.id,
    title: c.title,
    description: c.description,
    audience: c.audience,
    order: c.order,
    meta: c.meta,
    metaDocId: c.metaDocId || null,
    count: corpusCounts[c.id] || 0
  }))
  .filter(c => c.count > 0 || c.id === 'help-native')
  .sort((a, b) => a.order - b.order);

const featured = entries
  .filter(e => e.featured)
  .sort((a, b) => (b.priority || 0) - (a.priority || 0) || a.title.localeCompare(b.title, 'ko'));

const catalog = {
  version: '1.1',
  title: 'NSIGHT TCF 문서 라이브러리',
  description: 'nsight-tcf-framework 저장소 전체 마크다운 카탈로그',
  generatedAt: new Date().toISOString(),
  repoRoot: 'nsight-tcf-framework',
  total: entries.length,
  corpora,
  featured: featured.map(e => e.id),
  entries
};

fs.writeFileSync(outPath, JSON.stringify(catalog, null, 2), 'utf8');
console.log(`[scan-repo-docs] ${entries.length} files → ${outPath}`);
for (const c of corpora) {
  console.log(`  ${c.id}: ${c.count}`);
}
