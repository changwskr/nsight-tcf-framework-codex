#!/usr/bin/env node
'use strict';

const fs = require('fs');
const path = require('path');

const cacheDir = path.join(__dirname, '_docx-cache');

/** TOC 전용 docx — 장 본문 매핑에서 제외 */
const SKIP_DOCX = new Set([82]);

/** 자동 감지 보정 (docx 파일 번호 ≠ 장 번호) */
const CHAPTER_DOCX_OVERRIDE = {
  54: 55,
};

function getRawText(kind, n) {
  if (!n) return null;
  const p = path.join(cacheDir, `${kind}-${n}.txt`);
  if (!fs.existsSync(p)) return null;
  return fs.readFileSync(p, 'utf8');
}

function countSectionHeaders(text, prefix) {
  const re = new RegExp(`^${prefix}\\.\\d+(?:\\.\\d+)?\\s+`, 'gm');
  return (text.match(re) || []).length;
}

function detectChapterFromText(text) {
  if (!text) return 0;
  const lines = text.split(/\r?\n/).map((l) => l.trim()).filter(Boolean);

  for (let i = 0; i < lines.length; i++) {
    const m = lines[i].match(/^(\d{1,2})\.\s+(\S.*)$/);
    if (!m) continue;
    const n = parseInt(m[1], 10);
    if (n < 1 || n > 78) continue;
    const subRe = new RegExp(`^${n}\\.\\d+\\s+`);
    for (let j = i + 1; j < Math.min(i + 30, lines.length); j++) {
      if (subRe.test(lines[j])) return n;
    }
  }

  for (let i = 0; i < lines.length; i++) {
    const m =
      lines[i].match(/^부록\s+([A-J])\.\s+/i) ||
      lines[i].match(/^([A-J])\.\s+(\S.*)$/i);
    if (!m) continue;
    const letter = m[1].toUpperCase();
    const subRe = new RegExp(`^${letter}\\.\\d+\\s+`, 'i');
    for (let j = i + 1; j < Math.min(i + 30, lines.length); j++) {
      if (subRe.test(lines[j])) return `appendix:${letter}`;
    }
  }
  return 0;
}

function buildDocxMaps() {
  const chapterCandidates = {};
  const appendixCandidates = {};

  for (let n = 1; n <= 82; n++) {
    if (SKIP_DOCX.has(n)) continue;
    const raw = getRawText('docx', n);
    if (!raw || raw.length < 200) continue;
    const id = detectChapterFromText(raw);
    if (typeof id === 'number' && id >= 1) {
      if (!chapterCandidates[id]) chapterCandidates[id] = [];
      chapterCandidates[id].push({
        n,
        score: countSectionHeaders(raw, String(id)),
        len: raw.length,
      });
    } else if (typeof id === 'string' && id.startsWith('appendix:')) {
      const key = id.slice(9);
      if (!appendixCandidates[key]) appendixCandidates[key] = [];
      appendixCandidates[key].push({
        n,
        score: countSectionHeaders(raw, key),
        len: raw.length,
      });
    }
  }

  const byChapter = {};
  for (const [ch, cands] of Object.entries(chapterCandidates)) {
    const chNum = parseInt(ch, 10);
    if (CHAPTER_DOCX_OVERRIDE[chNum]) {
      byChapter[chNum] = CHAPTER_DOCX_OVERRIDE[chNum];
      continue;
    }
    cands.sort((a, b) => b.score - a.score || b.len - a.len);
    byChapter[chNum] = cands[0].n;
  }
  for (const [ch, n] of Object.entries(CHAPTER_DOCX_OVERRIDE)) {
    byChapter[parseInt(ch, 10)] = n;
  }

  const byAppendix = {};
  for (const [key, cands] of Object.entries(appendixCandidates)) {
    cands.sort((a, b) => b.score - a.score || b.len - a.len);
    byAppendix[key] = cands[0].n;
  }

  return { byChapter, byAppendix };
}

module.exports = {
  cacheDir,
  SKIP_DOCX,
  CHAPTER_DOCX_OVERRIDE,
  getRawText,
  detectChapterFromText,
  buildDocxMaps,
};
