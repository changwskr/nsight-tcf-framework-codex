#!/usr/bin/env node
/**
 * HELP 산출물 품질 검증
 * Usage: node _scripts/verify-help.mjs
 */
import fs from 'fs';
import path from 'path';
import { fileURLToPath } from 'url';

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const tcfHelpRoot = path.resolve(__dirname, '..');
const repoRoot = path.resolve(tcfHelpRoot, '..');

const errors = [];
const warnings = [];

function err(msg) { errors.push(msg); }
function warn(msg) { warnings.push(msg); }

function readJson(name) {
  const p = path.join(tcfHelpRoot, name);
  if (!fs.existsSync(p)) {
    err(`missing ${name}`);
    return null;
  }
  return JSON.parse(fs.readFileSync(p, 'utf8'));
}

function existsRepo(rel) {
  return fs.existsSync(path.join(repoRoot, rel.replace(/\//g, path.sep)));
}

function existsHelpDoc(file) {
  if (file.startsWith('meta/')) {
    return fs.existsSync(path.join(tcfHelpRoot, file));
  }
  return fs.existsSync(path.join(tcfHelpRoot, 'docs', file));
}

// ── help-index ──
const index = readJson('help-index.json');
if (index) {
  for (const section of index.sections || []) {
    for (const item of section.items || []) {
      if (item.external) continue;
      if (!item.file) {
        err(`help-index item ${item.id}: missing file`);
        continue;
      }
      if (!existsHelpDoc(item.file)) {
        err(`help-index item ${item.id}: file not found ${item.file}`);
      }
    }
  }
}

// ── doc-catalog ──
const catalog = readJson('doc-catalog.json');
if (catalog) {
  if (!catalog.total || catalog.total !== (catalog.entries || []).length) {
    warn(`doc-catalog total mismatch: ${catalog.total} vs ${(catalog.entries || []).length}`);
  }
  const ids = new Set();
  for (const e of catalog.entries || []) {
    if (ids.has(e.id)) err(`duplicate catalog id: ${e.id}`);
    ids.add(e.id);
    if (!existsRepo(e.source)) {
      err(`catalog source missing: ${e.source}`);
    }
  }
  for (const fid of catalog.featured || []) {
    if (!ids.has(fid)) err(`featured id not in catalog: ${fid}`);
  }
}

// ── catalog-overrides ──
const overridesPath = path.join(tcfHelpRoot, 'catalog-overrides.yml');
if (fs.existsSync(overridesPath)) {
  const text = fs.readFileSync(overridesPath, 'utf8');
  for (const line of text.split('\n')) {
    const m = line.match(/^  ([^:\s#][^:]*):$/);
    if (m && !m[1].startsWith('#')) {
      const key = m[1].trim();
      if (key !== 'entries' && !key.includes(' ')) {
        if (!existsRepo(key)) warn(`catalog-overrides path not found: ${key}`);
      }
    }
  }
}

// ── screen maps ──
const screenMap = readJson('help-screen-map.json');
const businessMap = readJson('help-business-map.json');

if (screenMap) {
  for (const [screen, meta] of Object.entries(screenMap.screens || {})) {
    if (!meta.docId) err(`screen-map ${screen}: missing docId`);
    if (index) {
      const found = (index.sections || []).some(s =>
        (s.items || []).some(i => i.id === meta.docId || i.external)
      );
      if (!found && !meta.external) warn(`screen-map ${screen}: docId ${meta.docId} not in help-index`);
    }
  }
}

if (businessMap) {
  for (const [screen, meta] of Object.entries(businessMap.screens || {})) {
    if (meta.src && !existsRepo(meta.src)) {
      err(`business-map ${screen}: src missing ${meta.src}`);
    }
  }
}

// ── library mirror (if built) ──
const mirrorDir = path.join(tcfHelpRoot, 'build', 'library-mirror');
if (catalog && fs.existsSync(mirrorDir)) {
  let mirrored = 0;
  for (const e of catalog.entries) {
    const dest = path.join(mirrorDir, e.source);
    if (fs.existsSync(dest)) mirrored++;
  }
  if (mirrored !== catalog.entries.length) {
    warn(`library mirror: ${mirrored}/${catalog.entries.length} files (run mirrorLibrary)`);
  }
}

// ── export bundle ──
const exportDir = path.join(tcfHelpRoot, 'build', 'help');
const requiredExport = ['help-index.json', 'help-screen-map.json', 'help-business-map.json', 'doc-catalog.json', 'help-link-report.json'];
for (const f of requiredExport) {
  if (!fs.existsSync(path.join(exportDir, f))) {
    warn(`export bundle missing ${f} (run exportHelp)`);
  }
}

console.log('[verify-help]');
if (catalog) console.log(`  catalog: ${catalog.total} entries, ${(catalog.featured || []).length} featured`);
if (screenMap) console.log(`  screen-map: ${Object.keys(screenMap.screens || {}).length} screens`);
if (businessMap) console.log(`  business-map: ${businessMap.total || 0} screens`);

const linkReport = readJson('help-link-report.json');
if (linkReport) {
  console.log(`  link-report: ${linkReport.links?.broken || 0} broken, ${linkReport.screens?.unmapped || 0} unmapped screens`);
  if (linkReport.links?.broken) {
    err(`help-link-report: ${linkReport.links.broken} broken link(s)`);
  }
}

if (warnings.length) {
  console.log(`\nWarnings (${warnings.length}):`);
  warnings.forEach(w => console.log(`  ⚠ ${w}`));
}

if (errors.length) {
  console.error(`\nErrors (${errors.length}):`);
  errors.forEach(e => console.error(`  ✗ ${e}`));
  process.exit(1);
}

console.log('\nverify-help OK');
process.exit(0);
