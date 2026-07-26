#!/usr/bin/env node
import fs from 'fs';
import path from 'path';
import { parseOverridesYaml } from './catalog-overrides.mjs';

export function loadScreenOverrides(tcfHelpRoot) {
  const p = path.join(tcfHelpRoot, 'help-screen-overrides.yml');
  if (!fs.existsSync(p)) return { entries: {} };
  return parseOverridesYaml(fs.readFileSync(p, 'utf8'));
}

export function normalizeScreenPath(p) {
  if (!p) return null;
  const s = p.trim();
  if (!s.startsWith('/')) return '/' + s;
  return s;
}

/** 자동 screen 맵에 overrides 병합 */
export function applyScreenOverrides(screens, overrides) {
  const out = { ...screens };
  for (const [rawPath, ov] of Object.entries(overrides.entries || {})) {
    const key = normalizeScreenPath(rawPath);
    if (!key) continue;
    out[key] = {
      ...(out[key] || {}),
      ...ov,
      manual: true
    };
  }
  return out;
}
