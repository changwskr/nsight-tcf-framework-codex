#!/usr/bin/env node
/**
 * doc-catalog.json 기준 저장소 .md → build/help/library/ 미러
 * Usage: node _scripts/mirror-library.mjs
 */
import fs from 'fs';
import path from 'path';
import { fileURLToPath } from 'url';

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const tcfHelpRoot = path.resolve(__dirname, '..');
const repoRoot = path.resolve(tcfHelpRoot, '..');
const catalogPath = path.join(tcfHelpRoot, 'doc-catalog.json');
const outRoot = path.join(tcfHelpRoot, 'build', 'library-mirror');

const catalog = JSON.parse(fs.readFileSync(catalogPath, 'utf8'));
let copied = 0;
let failed = 0;

if (fs.existsSync(outRoot)) {
  fs.rmSync(outRoot, { recursive: true, force: true });
}
fs.mkdirSync(outRoot, { recursive: true });

for (const entry of catalog.entries) {
  const src = path.join(repoRoot, entry.source);
  const dest = path.join(outRoot, entry.source);
  try {
    fs.mkdirSync(path.dirname(dest), { recursive: true });
    fs.copyFileSync(src, dest);
    copied += 1;
  } catch (e) {
    console.warn(`[mirror] skip ${entry.source}: ${e.message}`);
    failed += 1;
  }
}

console.log(`[mirror-library] copied ${copied} files to ${outRoot} (${failed} failed)`);
