/**
 * 읽기순서.md Markdown 항목 1:1 — 문서 내용을 파악할 수 있도록 풍부 요약 (Word 제외)
 */
const fs = require("fs");
const path = require("path");

const tocDir = path.join(
  "c:\\Programming(23-08-15)\\nsight-tcf-framework",
  "ztcf-다이어리",
  "2026-07-25 전체 파일 목차"
);
const tocPath = path.join(tocDir, "NSIGHT-TCF-전체문서-읽기순서.md");
const outPath = path.join(tocDir, "NSIGHT-TCF-목차순서별-핵심내용.md");

function decodeHref(href) {
  try {
    return decodeURIComponent(href);
  } catch {
    return href;
  }
}

function cleanInline(line) {
  return String(line || "")
    .replace(/^>\s*/, "")
    .replace(/!\[[^\]]*\]\([^)]*\)/g, "")
    .replace(/\[([^\]]+)\]\([^)]*\)/g, "$1")
    .replace(/[*_`]/g, "")
    .replace(/\s+/g, " ")
    .trim();
}

function truncate(s, n) {
  const t = String(s || "").trim();
  if (t.length <= n) return t;
  return t.slice(0, n - 1) + "…";
}

function parseTableBlock(lines, startIdx) {
  const rows = [];
  let i = startIdx;
  while (i < lines.length) {
    const line = lines[i].trim();
    if (!line.startsWith("|")) break;
    if (/^\|\s*:?-{2,}/.test(line)) {
      i += 1;
      continue;
    }
    const cells = line
      .replace(/^\|/, "")
      .replace(/\|$/, "")
      .split("|")
      .map((c) => cleanInline(c));
    if (cells.some((c) => c.length)) rows.push(cells);
    i += 1;
  }
  return { rows, next: i };
}

function tableToText(rows, maxRows = 5) {
  if (!rows.length) return "";
  const header = rows[0];
  const body = rows.slice(1, maxRows);
  const parts = [];
  parts.push(`(표) ${header.join(" / ")}`);
  for (const r of body) {
    const pairs = header.map((h, idx) => `${h}: ${r[idx] || ""}`).join("; ");
    parts.push(`  · ${truncate(pairs, 200)}`);
  }
  if (rows.length - 1 > body.length) parts.push(`  · …외 ${rows.length - 1 - body.length}행`);
  return parts.join("\n");
}

/**
 * 문서를 섹션 단위로 분해해 풍부 요약 생성
 */
function extractDigest(text) {
  const rawLines = text.replace(/\r\n/g, "\n").split("\n");
  let title = "";
  const introParas = [];
  const globalBullets = [];
  const globalQuotes = [];
  const flowBlocks = [];
  const sections = []; // {title, paras, bullets, tables, quotes}

  let current = null; // null = intro
  let inCode = false;
  let codeBuf = [];
  let codeLang = "";
  let paraBuf = [];

  function ensureSection(name) {
    if (!current || current.title !== name) {
      current = { title: name, paras: [], bullets: [], tables: [], quotes: [] };
      sections.push(current);
    }
  }

  function flushPara() {
    if (!paraBuf.length) return;
    const p = cleanInline(paraBuf.join(" "));
    paraBuf = [];
    if (p.length < 18) return;
    if (!current) {
      if (introParas.length < 8) introParas.push(p);
    } else if (current.paras.length < 6) {
      current.paras.push(p);
    }
  }

  function addBullet(b) {
    if (b.length < 8) return;
    if (/^(ztcfbook|znsight|zman|zguide|docs\/|📘|이전|다음)/i.test(b)) return;
    if (!current) {
      if (globalBullets.length < 12) globalBullets.push(b);
    } else if (current.bullets.length < 10) {
      current.bullets.push(b);
    }
  }

  for (let i = 0; i < rawLines.length; i++) {
    const line = rawLines[i].trim();

    if (line.startsWith("```")) {
      flushPara();
      if (!inCode) {
        inCode = true;
        codeLang = line.slice(3).trim().toLowerCase();
        codeBuf = [];
      } else {
        inCode = false;
        const body = codeBuf.join("\n").trim();
        // keep short text/mermaid/flow diagrams as readable flow
        if (
          body &&
          flowBlocks.length < 3 &&
          (codeLang === "text" ||
            codeLang === "" ||
            codeLang === "mermaid" ||
            /STF|ETF|Handler|POST |serviceId|→|↓/.test(body))
        ) {
          const clipped = truncate(body.replace(/\n+/g, " | "), 420);
          if (clipped.length >= 20) flowBlocks.push(clipped);
        }
        codeBuf = [];
      }
      continue;
    }
    if (inCode) {
      if (codeBuf.length < 40) codeBuf.push(rawLines[i]);
      continue;
    }

    const hm = line.match(/^(#{1,3})\s+(.+)$/);
    if (hm) {
      flushPara();
      const level = hm[1].length;
      const t = cleanInline(hm[2]);
      if (level === 1 && !title) {
        title = t;
        continue;
      }
      if (level <= 2) {
        if (/^(관련|참고|링크|변경|이력|이전|다음|📘|집필 상태)/.test(t)) continue;
        if (sections.length < 18) ensureSection(t);
        else current = sections[sections.length - 1];
      }
      continue;
    }

    if (!line) {
      flushPara();
      continue;
    }
    if (/^---+$/.test(line)) {
      flushPara();
      continue;
    }

    if (line.startsWith("|")) {
      flushPara();
      const { rows, next } = parseTableBlock(rawLines, i);
      i = next - 1;
      const tt = tableToText(rows, 4);
      if (tt) {
        if (!current) {
          // treat important early tables as intro material via a synthetic note in bullets
          if (globalBullets.length < 12) globalBullets.push(truncate(tt.replace(/\n/g, " "), 220));
        } else if (current.tables.length < 3) {
          current.tables.push(tt);
        }
      }
      continue;
    }

    if (line.startsWith(">")) {
      flushPara();
      const q = cleanInline(line);
      if (q.length >= 12) {
        if (!current) {
          if (globalQuotes.length < 6) globalQuotes.push(q);
        } else if (current.quotes.length < 4) current.quotes.push(q);
      }
      continue;
    }

    if (/^[-*]\s+/.test(line) || /^\d+\.\s+/.test(line)) {
      flushPara();
      addBullet(cleanInline(line.replace(/^[-*]\s+/, "").replace(/^\d+\.\s+/, "")));
      continue;
    }

    if (line.startsWith("<") || line.startsWith("![")) continue;

    paraBuf.push(line);
    if (paraBuf.join(" ").length > 360) flushPara();
  }
  flushPara();

  // Prefer informative sections (skip tiny README shells later)
  const rankedSections = [...sections].sort((a, b) => {
    const sa = a.paras.length * 2 + a.bullets.length + a.tables.length * 2 + a.quotes.length;
    const sb = b.paras.length * 2 + b.bullets.length + b.tables.length * 2 + b.quotes.length;
    return sb - sa;
  });

  return {
    title,
    introParas,
    globalBullets,
    globalQuotes,
    flowBlocks,
    sections,
    rankedSections,
  };
}

function formatDigest(name, href, abs) {
  const out = [];
  out.push(`**[${name}](${href})**`);

  if (!fs.existsSync(abs)) {
    out.push("- 상태: 파일을 찾을 수 없음");
    return out;
  }

  let text = "";
  try {
    text = fs.readFileSync(abs, "utf8");
  } catch (e) {
    out.push(`- 상태: 읽기 실패 (${e.message})`);
    return out;
  }

  const bytes = Buffer.byteLength(text, "utf8");
  if (text.length > 500000) text = text.slice(0, 500000);

  const d = extractDigest(text);
  if (d.title) out.push(`- 제목: ${d.title}`);
  out.push(`- 분량: 약 ${Math.max(1, Math.round(bytes / 1024))}KB · 원문 기준 요약`);

  // 개요: intro + quotes
  const overviewBits = [];
  for (const q of d.globalQuotes.slice(0, 3)) overviewBits.push(q);
  for (const p of d.introParas) {
    overviewBits.push(p);
    if (overviewBits.join(" ").length > 1100) break;
  }
  // if intro thin, pull from first rich sections
  if (overviewBits.join(" ").length < 200) {
    for (const s of d.sections.slice(0, 4)) {
      for (const p of s.paras.slice(0, 2)) overviewBits.push(p);
      for (const q of s.quotes.slice(0, 1)) overviewBits.push(q);
      if (overviewBits.join(" ").length > 900) break;
    }
  }

  let overview = overviewBits.join(" ");
  if (overview.length > 1400) overview = overview.slice(0, 1399) + "…";
  out.push(`- 개요: ${overview || "(서술 본문이 거의 없는 목차/표지형 문서)"}`);

  // 전체 핵심 포인트
  const points = [...d.globalBullets];
  for (const s of d.rankedSections.slice(0, 6)) {
    for (const b of s.bullets) {
      if (points.length >= 14) break;
      const item = truncate(`${b}`, 180);
      if (!points.includes(item)) points.push(item);
    }
  }
  if (points.length) {
    out.push("- 핵심 포인트:");
    for (const p of points.slice(0, 12)) out.push(`  - ${p}`);
  }

  // 흐름/다이어그램
  if (d.flowBlocks.length) {
    out.push("- 흐름·구조 스케치:");
    for (const f of d.flowBlocks.slice(0, 2)) out.push(`  - ${f}`);
  }

  // 섹션별 요지 (풍부함의 핵심)
  const sectionDigest = [];
  for (const s of d.sections) {
    if (/^(이전|다음|요약|📘|관련 문서|참고)/.test(s.title)) continue;
    const rich =
      s.paras.length + s.bullets.length + s.tables.length + s.quotes.length;
    if (rich === 0) continue;
    sectionDigest.push(s);
    if (sectionDigest.length >= 10) break;
  }

  // if too few in order, fill from ranked
  if (sectionDigest.length < 5) {
    for (const s of d.rankedSections) {
      if (sectionDigest.includes(s)) continue;
      if (s.paras.length + s.bullets.length === 0) continue;
      sectionDigest.push(s);
      if (sectionDigest.length >= 8) break;
    }
  }

  if (sectionDigest.length) {
    out.push("- 섹션별 요지:");
    for (const s of sectionDigest) {
      out.push(`  - **${s.title}**`);
      const body = [];
      for (const q of s.quotes.slice(0, 2)) body.push(q);
      for (const p of s.paras.slice(0, 3)) body.push(p);
      let para = truncate(body.join(" "), 520);
      if (para) out.push(`    - ${para}`);
      const bs = s.bullets.slice(0, 5);
      for (const b of bs) out.push(`    - ${truncate(b, 160)}`);
      for (const t of s.tables.slice(0, 1)) {
        for (const row of t.split("\n").slice(0, 4)) out.push(`    - ${row}`);
      }
    }
  }

  // 구성 목록
  const titles = d.sections
    .map((s) => s.title)
    .filter((t) => !/^(이전|다음|요약|📘)/.test(t))
    .slice(0, 16);
  if (titles.length) out.push(`- 구성: ${titles.join(" · ")}`);

  return out;
}

function resolveFromToc(href) {
  return path.normalize(path.join(tocDir, decodeHref(href).replace(/\\/g, "/")));
}

const toc = fs.readFileSync(tocPath, "utf8");
const lines = toc.split(/\r?\n/);
const out = [];

out.push("# NSIGHT TCF — 목차 순서별 핵심 내용 (Markdown 1:1, 풍부판)");
out.push("");
out.push("생성일: 2026-07-25");
out.push("");
out.push("기준: [NSIGHT-TCF-전체문서-읽기순서.md](./NSIGHT-TCF-전체문서-읽기순서.md)");
out.push("");
out.push("- 목차 Markdown과 **1:1** 대응, **Word/PDF 제외**");
out.push("- 각 문서마다 **개요 · 핵심 포인트 · 흐름 스케치 · 섹션별 요지 · 구성**을 넣어 원문 없이도 내용을 파악할 수 있게 작성");
out.push("- 원문 전체가 필요하면 각 항목 링크를 열면 됩니다");
out.push("");

let section = "";
let itemNo = 0;
let totalMd = 0;
let skippedBin = 0;
const linkRe = /^(\d+)\.\s+\[([^\]]+)\]\(([^)]+)\)\s*$/;
const sectionRe = /^##\s+(.+)$/;

for (const line of lines) {
  const sec = line.match(sectionRe);
  if (sec) {
    const title = sec[1].trim();
    if (
      title.startsWith("어떻게 읽으면") ||
      title.startsWith("폴더별 파일") ||
      title.startsWith("부록.")
    ) {
      section = "";
      continue;
    }
    section = title;
    itemNo = 0;
    out.push("---");
    out.push("");
    out.push(`## ${section}`);
    out.push("");
    if (/Word/i.test(title) && !/Markdown|AI/i.test(title)) {
      out.push(
        "> Word 전용 구간입니다. `.docx`는 제외하며, 같은 주제 Markdown(`zguide`, `ztcf-집필본-md`, `ztcf-book-capacity-md`, `znsight-man` 등)을 보세요."
      );
      out.push("");
    }
    continue;
  }
  if (!section) continue;

  if (line.startsWith("### ")) {
    out.push(line);
    out.push("");
    itemNo = 0;
    continue;
  }

  const m = line.match(linkRe);
  if (!m) continue;
  const name = m[2];
  const href = m[3];
  const lower = `${name} ${href}`.toLowerCase();
  if (lower.includes(".docx") || lower.includes(".pdf")) {
    skippedBin += 1;
    continue;
  }

  itemNo += 1;
  totalMd += 1;
  const body = formatDigest(name, href, resolveFromToc(href));
  out.push(`${itemNo}. ${body[0]}`);
  for (let i = 1; i < body.length; i++) out.push(`   ${body[i]}`);
  out.push("");
}

out.push("---");
out.push("");
out.push("## 집계");
out.push("");
out.push(`| 항목 | 수 |`);
out.push(`| --- | ---: |`);
out.push(`| Markdown 요약 | ${totalMd} |`);
out.push(`| Word/PDF 제외 | ${skippedBin} |`);
out.push("");
out.push("재생성: `node _build-toc-summaries.js`");
out.push("");

fs.writeFileSync(outPath, out.join("\n"), "utf8");
console.log("Wrote", outPath);
console.log({ totalMd, skippedBin, bytes: fs.statSync(outPath).size });
