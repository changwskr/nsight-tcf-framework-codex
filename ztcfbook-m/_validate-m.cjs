#!/usr/bin/env node
'use strict';
const fs = require('fs');
const path = require('path');

const ROOT = __dirname;

function walkMd(dir, base = '') {
  const out = [];
  for (const e of fs.readdirSync(dir, { withFileTypes: true })) {
    if (e.name.startsWith('_')) continue;
    const rel = path.join(base, e.name).replace(/\\/g, '/');
    const full = path.join(dir, e.name);
    if (e.isDirectory()) out.push(...walkMd(full, rel));
    else if (e.name.endsWith('.md')) out.push(rel);
  }
  return out;
}

function extractMermaid(content) {
  const blocks = [];
  const re = /```mermaid\n([\s\S]*?)```/g;
  let m;
  while ((m = re.exec(content)) !== null) blocks.push(m[1]);
  return blocks;
}

function extractLinks(content, file) {
  const links = [];
  const re = /\[([^\]]*)\]\(([^)]+)\)/g;
  let m;
  while ((m = re.exec(content)) !== null) {
    const href = m[2];
    if (href.startsWith('http') || href.startsWith('#')) continue;
    links.push({ text: m[1], href, file });
  }
  return links;
}

function resolveLink(fromFile, href) {
  if (href.startsWith('../ztcfbook')) return { ok: true, external: 'ztcfbook' };
  if (href.startsWith('../z')) return { ok: true, external: 'repo' };
  const fromDir = path.dirname(fromFile);
  const target = path.normalize(path.join(ROOT, fromDir, href.split('#')[0])).replace(/\\/g, '/');
  const rel = path.relative(ROOT, target).replace(/\\/g, '/');
  return { ok: fs.existsSync(target), rel, target };
}

async function main() {
  const { JSDOM } = require('jsdom');
  const dom = new JSDOM('<!DOCTYPE html><html><body></body></html>');
  global.window = dom.window;
  global.document = dom.window.document;
  global.DOMPurify = require('dompurify')(dom.window);
  const mermaid = (await import('mermaid')).default;
  mermaid.initialize({ startOnLoad: false, securityLevel: 'loose' });

  const files = walkMd(ROOT);
  const mermaidErrors = [];
  const linkErrors = [];

  for (const rel of files) {
    const content = fs.readFileSync(path.join(ROOT, rel), 'utf8');
    const blocks = extractMermaid(content);
    for (let i = 0; i < blocks.length; i++) {
      try {
        await mermaid.parse(blocks[i]);
      } catch (e) {
        mermaidErrors.push({ file: rel, block: i + 1, msg: (e.message || String(e)).split('\n')[0] });
      }
    }
    for (const link of extractLinks(content, rel)) {
      const r = resolveLink(rel, link.href);
      if (!r.ok) linkErrors.push({ file: rel, text: link.text, href: link.href, target: r.rel });
    }
  }

  console.log('mermaid errors:', mermaidErrors.length);
  mermaidErrors.forEach((e) => console.log('  M', e.file, `#${e.block}`, e.msg));
  console.log('link errors:', linkErrors.length);
  linkErrors.forEach((e) => console.log('  L', e.file, `"${e.text}" -> ${e.href} (${e.target})`));
}

main().catch((e) => {
  console.error(e);
  process.exit(1);
});
