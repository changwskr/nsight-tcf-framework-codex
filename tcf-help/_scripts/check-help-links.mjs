#!/usr/bin/env node
/**
 * HELP 문서·화면 링크 검증 → help-link-report.json
 * Usage: node _scripts/check-help-links.mjs [--fail-on-error]
 */
import fs from 'fs';
import path from 'path';
import { fileURLToPath } from 'url';
import { loadScreenOverrides, normalizeScreenPath } from './screen-overrides.mjs';

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const tcfHelpRoot = path.resolve(__dirname, '..');
const repoRoot = path.resolve(tcfHelpRoot, '..');
const uiStatic = path.join(repoRoot, 'tcf-ui', 'src', 'main', 'resources', 'static');
const outPath = path.join(tcfHelpRoot, 'help-link-report.json');
const failOnError = process.argv.includes('--fail-on-error');

const broken = [];
const warnings = [];

function readJson(name) {
  const p = path.join(tcfHelpRoot, name);
  return fs.existsSync(p) ? JSON.parse(fs.readFileSync(p, 'utf8')) : null;
}

function existsRepo(rel) {
  return fs.existsSync(path.join(repoRoot, rel.replace(/\//g, path.sep)));
}

function existsUiHtml(urlPath) {
  const clean = urlPath.split('#')[0].split('?')[0];
  if (!clean.endsWith('.html')) return null;
  const rel = clean.replace(/^\//, '');
  return fs.existsSync(path.join(uiStatic, rel.replace(/\//g, path.sep)));
}

function walkMd(dir, base, files = []) {
  if (!fs.existsSync(dir)) return files;
  for (const ent of fs.readdirSync(dir, { withFileTypes: true })) {
    const full = path.join(dir, ent.name);
    if (ent.isDirectory()) walkMd(full, base, files);
    else if (ent.isFile() && ent.name.endsWith('.md')) {
      files.push(path.relative(base, full).replace(/\\/g, '/'));
    }
  }
  return files;
}

function collectDocIds(index) {
  const ids = new Set();
  (index?.sections || []).forEach(s => (s.items || []).forEach(i => ids.add(i.id)));
  return ids;
}

function resolveSrcHref(href, docFile) {
  let h = decodeURIComponent(href);
  if (h.startsWith('/help/view.html?')) {
    const q = h.split('?')[1] || '';
    const params = new URLSearchParams(q);
    if (params.get('src')) return { type: 'src', target: params.get('src') };
    if (params.get('doc')) return { type: 'doc', target: params.get('doc') };
  }
  if (h.startsWith('?doc=')) return { type: 'doc', target: h.slice(5) };
  if (h.startsWith('/help/view.html?doc=')) {
    return { type: 'doc', target: decodeURIComponent(h.split('doc=')[1] || '') };
  }
  if (h.includes('?src=')) {
    const m = h.match(/[?&]src=([^&]+)/);
    if (m) return { type: 'src', target: decodeURIComponent(m[1]) };
  }
  if (h.startsWith('/') && h.endsWith('.html')) return { type: 'ui', target: h };
  if (h.startsWith('/') && !h.startsWith('//')) return { type: 'ui', target: h };
  if (/^https?:\/\//i.test(h)) return { type: 'external', target: h };
  if (h.endsWith('.md') && !h.startsWith('http')) {
    const baseDir = path.dirname(docFile).replace(/\\/g, '/');
    const resolved = path.posix.normalize(path.posix.join(baseDir, h));
    return { type: 'src', target: resolved };
  }
  return { type: 'unknown', target: h };
}

function checkHref(file, lineNo, href, docIds) {
  const r = resolveSrcHref(href, file);
  if (r.type === 'external' || r.type === 'unknown') return;
  if (r.type === 'doc') {
    if (!docIds.has(r.target)) {
      broken.push({ file, line: lineNo, href, kind: 'doc', target: r.target, reason: 'help-index에 없는 doc id' });
    }
    return;
  }
  if (r.type === 'src') {
    if (!existsRepo(r.target)) {
      broken.push({ file, line: lineNo, href, kind: 'src', target: r.target, reason: '저장소 md 없음' });
    }
    return;
  }
  if (r.type === 'ui') {
    const html = r.target.split('#')[0];
    if (html.endsWith('.html') && !existsUiHtml(html)) {
      broken.push({ file, line: lineNo, href, kind: 'ui', target: html, reason: 'tcf-ui HTML 없음' });
    }
  }
}

function scanMarkdownLinks(file, text, docIds) {
  const lines = text.split('\n');
  const linkRe = /\[([^\]]*)\]\(([^)]+)\)/g;
  for (let i = 0; i < lines.length; i++) {
    let m;
    while ((m = linkRe.exec(lines[i])) !== null) {
      checkHref(file, i + 1, m[2], docIds);
    }
  }
}

function walkUiHtml(dir, base = uiStatic, files = []) {
  if (!fs.existsSync(dir)) return files;
  for (const ent of fs.readdirSync(dir, { withFileTypes: true })) {
    const full = path.join(dir, ent.name);
    const rel = '/' + path.relative(base, full).replace(/\\/g, '/');
    if (ent.isDirectory()) {
      if (ent.name === '_shared') continue;
      walkUiHtml(full, base, files);
    } else if (ent.isFile() && ent.name.endsWith('.html')) {
      files.push(rel);
    }
  }
  return files;
}

function collectMappedScreens() {
  const mapped = new Set();
  const native = readJson('help-screen-map.json');
  const business = readJson('help-business-map.json');
  const overrides = loadScreenOverrides(tcfHelpRoot);
  for (const p of Object.keys(native?.screens || {})) mapped.add(normalizeScreenPath(p));
  for (const p of Object.keys(business?.screens || {})) mapped.add(normalizeScreenPath(p));
  for (const p of Object.keys(overrides.entries || {})) mapped.add(normalizeScreenPath(p));
  return mapped;
}

const index = readJson('help-index.json');
const docIds = collectDocIds(index);
const mdFiles = [
  ...walkMd(path.join(tcfHelpRoot, 'docs'), path.join(tcfHelpRoot, 'docs')).map(f => `docs/${f}`),
  ...walkMd(path.join(tcfHelpRoot, 'meta'), path.join(tcfHelpRoot, 'meta')).map(f => `meta/${f}`)
];

let linkTotal = 0;
for (const rel of mdFiles) {
  const full = path.join(tcfHelpRoot, rel);
  const text = fs.readFileSync(full, 'utf8');
  const before = broken.length;
  scanMarkdownLinks(rel, text, docIds);
  linkTotal += (text.match(/\[([^\]]*)\]\(([^)]+)\)/g) || []).length;
  if (broken.length > before) { /* counted in broken */ }
}

const SKIP_UNMAPPED = new Set([
  '/help.html', '/help/view.html', '/help/library.html', '/help/health.html',
  '/error-popup.html', '/updownload.html'
]);
const allHtml = walkUiHtml(uiStatic).filter(p =>
  !p.startsWith('/help/') && !SKIP_UNMAPPED.has(p)
);
const mapped = collectMappedScreens();
const unmappedScreens = allHtml
  .filter(p => !mapped.has(p))
  .map(p => ({ screen: p, reason: 'HELP screen 맵 없음' }))
  .sort((a, b) => a.screen.localeCompare(b.screen));

const overrideCount = Object.keys(loadScreenOverrides(tcfHelpRoot).entries || {}).length;

const report = {
  version: '1.0',
  generatedAt: new Date().toISOString(),
  links: {
    scanned: linkTotal,
    broken: broken.length,
    warnings: warnings.length
  },
  screens: {
    totalHtml: allHtml.length,
    mapped: mapped.size,
    unmapped: unmappedScreens.length,
    manualOverrides: overrideCount
  },
  broken,
  unmappedScreens: unmappedScreens.slice(0, 200),
  unmappedTruncated: unmappedScreens.length > 200
};

fs.writeFileSync(outPath, JSON.stringify(report, null, 2), 'utf8');

console.log('[check-help-links]');
console.log(`  links scanned: ${linkTotal}, broken: ${broken.length}`);
console.log(`  screens: ${allHtml.length} html, ${mapped.size} mapped, ${unmappedScreens.length} unmapped`);
console.log(`  → ${outPath}`);

if (broken.length) {
  broken.slice(0, 10).forEach(b => console.log(`  ✗ ${b.file}:${b.line} ${b.href} (${b.reason})`));
  if (broken.length > 10) console.log(`  ... +${broken.length - 10} more`);
}

if (failOnError && broken.length) process.exit(1);
process.exit(0);
