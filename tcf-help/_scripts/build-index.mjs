#!/usr/bin/env node
/**
 * help-index.yml → help-index.json
 * Usage: node _scripts/build-index.mjs
 */
import fs from 'fs';
import path from 'path';
import { fileURLToPath } from 'url';
import { loadScreenOverrides, applyScreenOverrides } from './screen-overrides.mjs';

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const root = path.resolve(__dirname, '..');
const ymlPath = path.join(root, 'help-index.yml');
const jsonPath = path.join(root, 'help-index.json');

function stripQuotes(s) {
  const t = (s || '').trim();
  if ((t.startsWith('"') && t.endsWith('"')) || (t.startsWith("'") && t.endsWith("'"))) {
    return t.slice(1, -1);
  }
  return t;
}

  function parseInlineList(val) {
    const inner = val.replace(/^\[/, '').replace(/\]$/, '').trim();
    if (!inner) return [];
    return inner.split(',').map(s => stripQuotes(s.trim()).replace(/^\[/, '')).filter(Boolean);
  }

function valueAfterColon(line) {
  return stripQuotes(line.split(':').slice(1).join(':').trim());
}

function parseHelpIndexYaml(text) {
  const result = { sections: [] };
  let section = null;
  let item = null;
  let inItems = false;

  for (const raw of text.split(/\r?\n/)) {
    const line = raw.replace(/\s+#.*$/, '');
    if (!line.trim()) continue;

    if (/^version:/.test(line)) {
      result.version = valueAfterColon(line);
      continue;
    }
    if (/^title:/.test(line) && !section && !item) {
      result.title = valueAfterColon(line);
      continue;
    }
    if (/^description:/.test(line) && !section && !item) {
      result.description = valueAfterColon(line);
      continue;
    }

    if (/^\s{2}- id:/.test(line)) {
      section = { id: valueAfterColon(line), items: [] };
      result.sections.push(section);
      item = null;
      inItems = false;
      continue;
    }

    if (section && /^\s{4}title:/.test(line) && !inItems) {
      section.title = valueAfterColon(line);
      continue;
    }
    if (section && /^\s{4}description:/.test(line) && !inItems) {
      section.description = valueAfterColon(line);
      continue;
    }
    if (section && /^\s{4}items:/.test(line)) {
      inItems = true;
      continue;
    }

    if (section && inItems && /^\s{6}- id:/.test(line)) {
      item = { id: valueAfterColon(line) };
      section.items.push(item);
      continue;
    }
    if (item && /^\s{8}title:/.test(line)) {
      item.title = valueAfterColon(line);
      continue;
    }
    if (item && /^\s{8}file:/.test(line)) {
      item.file = valueAfterColon(line);
      continue;
    }
    if (item && /^\s{8}tags:/.test(line)) {
      item.tags = parseInlineList(line.split(':').slice(1).join(':'));
      continue;
    }
    if (item && /^\s{8}screens:/.test(line)) {
      item.screens = parseInlineList(line.split(':').slice(1).join(':'));
      continue;
    }
    if (item && /^\s{8}external:/.test(line)) {
      item.external = valueAfterColon(line);
      continue;
    }
  }

  return result;
}

function enrichIndex(index) {
  let count = 0;
  (index.sections || []).forEach(section => {
    (section.items || []).forEach(item => {
      count += 1;
      item.sectionId = section.id;
      item.sectionTitle = section.title;
    });
  });
  index.docCount = count;
  index.generatedAt = new Date().toISOString();
  return index;
}

function buildScreenMap(index) {
  const screens = {};
  (index.sections || []).forEach(section => {
    (section.items || []).forEach(item => {
      (item.screens || []).forEach(screen => {
        const path = screen.startsWith('/') ? screen : '/' + screen;
        screens[path] = {
          docId: item.id,
          title: item.title,
          sectionId: section.id,
          sectionTitle: section.title,
          external: item.external || null
        };
      });
    });
  });
  return {
    version: index.version || '1.0',
    generatedAt: index.generatedAt,
    screens
  };
}

const yml = fs.readFileSync(ymlPath, 'utf8');
const index = enrichIndex(parseHelpIndexYaml(yml));
const overrides = loadScreenOverrides(root);
const screenMap = buildScreenMap(index);
screenMap.screens = applyScreenOverrides(screenMap.screens, overrides);
screenMap.overrideCount = Object.keys(overrides.entries || {}).length;
const screenMapPath = path.join(root, 'help-screen-map.json');
fs.writeFileSync(jsonPath, JSON.stringify(index, null, 2), 'utf8');
fs.writeFileSync(screenMapPath, JSON.stringify(screenMap, null, 2), 'utf8');
console.log(`[tcf-help] wrote ${jsonPath} (${index.docCount} docs)`);
console.log(`[tcf-help] wrote ${screenMapPath} (${Object.keys(screenMap.screens).length} screens)`);
