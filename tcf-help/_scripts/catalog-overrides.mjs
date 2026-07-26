#!/usr/bin/env node
/**
 * catalog-overrides.yml 파서 (단순 key-value / entries 블록)
 */
import fs from 'fs';

export function stripQuotes(s) {
  const t = (s || '').trim();
  if ((t.startsWith('"') && t.endsWith('"')) || (t.startsWith("'") && t.endsWith("'"))) {
    return t.slice(1, -1);
  }
  return t;
}

export function parseInlineList(val) {
  const inner = val.replace(/^\[/, '').replace(/\]$/, '').trim();
  if (!inner) return [];
  return inner.split(',').map(s => stripQuotes(s.trim()).replace(/^\[/, '')).filter(Boolean);
}

export function parseOverridesYaml(text) {
  const result = { entries: {} };
  let section = null;
  let currentKey = null;
  let current = null;

  for (const raw of text.split(/\r?\n/)) {
    const line = raw.replace(/\s+#.*$/, '');
    if (!line.trim()) continue;

    if (/^entries:/.test(line)) {
      section = 'entries';
      continue;
    }

    if (section === 'entries' && /^  "[^"]+":/.test(line)) {
      currentKey = line.match(/^  "([^"]+)":/)[1];
      current = {};
      result.entries[currentKey] = current;
      continue;
    }
    if (section === 'entries' && /^  [^:\s#][^:]*:$/.test(line) && !line.includes(': ')) {
      currentKey = line.trim().replace(/:$/, '');
      current = {};
      result.entries[currentKey] = current;
      continue;
    }

    if (!current || !currentKey) continue;

    const m = line.match(/^\s{4}(\w+):\s*(.*)$/);
    if (!m) continue;
    const [, key, val] = m;
    if (key === 'tags') {
      current.tags = parseInlineList(val);
    } else if (key === 'featured') {
      current.featured = val.trim() === 'true';
    } else if (key === 'priority') {
      current.priority = parseInt(val, 10) || 0;
    } else {
      current[key] = stripQuotes(val);
    }
  }

  return result;
}

export function loadCatalogOverrides(tcfHelpRoot) {
  const p = `${tcfHelpRoot}/catalog-overrides.yml`;
  if (!fs.existsSync(p)) return { entries: {} };
  return parseOverridesYaml(fs.readFileSync(p, 'utf8'));
}
