const fs = require("fs");
const path = require("path");

const root = "c:\\Programming(23-08-15)\\nsight-tcf-framework";
const outDir = path.join(root, "ztcf-다이어리", "2026-07-25 전체 파일 목차");
fs.mkdirSync(outDir, { recursive: true });

const dirs = [
  "zarchitecture",
  "zdocs-1",
  "zdocs-2",
  "zguide",
  "zman",
  "znsight-capacity-word",
  "znsight-config-info",
  "znsight-config-value-word",
  "znsight-guide-word",
  "znsight-man",
  "znsight-구축방법론",
  "ztcf-book-capacity-md",
  "ztcf-engine-config-info",
  "ztcf-개발북",
  "ztcf-다이어리",
  "ztcf-집필본",
  "ztcf-집필본-md",
  "ztcfbook",
  "ztcfbook-h",
  "ztcfbook-m",
];

const exts = new Set([".md", ".html", ".htm", ".docx", ".pdf", ".txt", ".adoc"]);

function walk(dir, topDir, acc = []) {
  let entries;
  try {
    entries = fs.readdirSync(dir, { withFileTypes: true });
  } catch {
    return acc;
  }
  for (const e of entries) {
    if (e.name === "node_modules" || e.name === ".git" || e.name === "__pycache__") continue;
    const full = path.join(dir, e.name);
    if (e.isDirectory()) walk(full, topDir, acc);
    else {
      const ext = path.extname(e.name).toLowerCase();
      if (!exts.has(ext)) continue;
      if (e.name.startsWith("_")) continue;
      const rel = path.relative(root, full).split(path.sep).join("/");
      acc.push({ dir: topDir, rel, name: e.name, ext });
    }
  }
  return acc;
}

function naturalKey(s) {
  return String(s)
    .toLowerCase()
    .replace(/(\d+)/g, (n) => n.padStart(8, "0"));
}

function link(rel) {
  // Cursor/VS Code: keep Hangul readable; encode only unsafe path chars per segment
  const href =
    "../../" +
    rel
      .split("/")
      .map((seg) => encodeURIComponent(seg).replace(/%2F/gi, "/"))
      .join("/");
  return `[${rel.split("/").pop()}](${href})`;
}

function orderBookFiles(files) {
  const rank = (rel) => {
    if (/\/00-목차\.md$/i.test(rel) || /\/README\.md$/i.test(rel) && rel.split("/").length === 2) return "0";
    if (/\/서문\//.test(rel)) return "1";
    const m = rel.match(/\/제(\d+)편\//);
    if (m) return "2-" + String(m[1]).padStart(2, "0");
    if (/\/부록\//.test(rel)) return "3";
    if (/\/README\.md$/i.test(rel)) return "9";
    return "5";
  };
  return [...files].sort((a, b) => {
    const ra = rank(a.rel);
    const rb = rank(b.rel);
    if (ra !== rb) return ra.localeCompare(rb);
    return naturalKey(a.rel).localeCompare(naturalKey(b.rel), "ko");
  });
}

function sectionFiles(all, dirName, { preferMd = true, excludeExt = [] } = {}) {
  let list = all.filter((f) => f.dir === dirName);
  if (preferMd) {
    const mdNames = new Set(
      list.filter((f) => f.ext === ".md").map((f) => f.name.replace(/\.md$/i, ""))
    );
    list = list.filter((f) => {
      if (excludeExt.includes(f.ext)) return false;
      if (f.ext === ".docx" || f.ext === ".pdf") {
        const base = f.name.replace(/\.(docx|pdf)$/i, "");
        // if md twin exists elsewhere with same basename in same dir list, skip binary
        if (list.some((x) => x.ext === ".md" && x.name.replace(/\.md$/i, "") === base)) return false;
      }
      return true;
    });
  }
  return list.sort((a, b) => naturalKey(a.rel).localeCompare(naturalKey(b.rel), "ko"));
}

function bulletList(files) {
  return files.map((f, i) => `${i + 1}. ${link(f.rel)}`).join("\n");
}

const all = [];
for (const d of dirs) walk(path.join(root, d), d, all);
fs.writeFileSync(path.join(outDir, "_inventory.json"), JSON.stringify(all, null, 2), "utf8");

const counts = {};
for (const f of all) counts[f.dir] = (counts[f.dir] || 0) + 1;

// --- Reading order composition ---
const lines = [];
lines.push("# NSIGHT TCF 전체 문서 읽기 순서 목차");
lines.push("");
lines.push(`생성일: 2026-07-25`);
lines.push("");
lines.push("이 문서는 저장소 내 주요 문서 폴더를 **학습·참조 순서**로 정리한 링크 목차입니다.");
lines.push("같은 내용이 Word/Markdown으로 중복되면 **Markdown을 우선**합니다.");
lines.push("");
lines.push("## 어떻게 읽으면 되나 (권장 경로)");
lines.push("");
lines.push("| 순서 | 경로 | 대상 | 목적 |");
lines.push("| ---: | --- | --- | --- |");
lines.push("| 1 | A | `ztcfbook-m` | 초보자 입문(쉬운 서술) |");
lines.push("| 2 | B | `zarchitecture` | 시스템·프레임워크 그림 |");
lines.push("| 3 | C | `zman` | 설계서(표준·처리흐름) |");
lines.push("| 4 | D | `zdocs-2` → `zdocs-1` | 주제별 개념·소스 인덱스 |");
lines.push("| 5 | E | `znsight-guide-word` / `zguide` | 개발 매뉴얼·모듈 가이드 |");
lines.push("| 6 | F | `ztcf-집필본-md` / `ztcfbook` / `ztcfbook-h` | 심화 집필·책 본문 |");
lines.push("| 7 | G | `znsight-man` | 개발 입문서(장 단위) |");
lines.push("| 8 | H | `znsight-구축방법론` + AI 방법론 | 구축·자동화 절차 |");
lines.push("| 9 | I | 용량·설정·엔진 설정 | 용량산정·환경값·운영 |");
lines.push("| 10 | J | `ztcf-다이어리` | 일자별 작업 메모 |");
lines.push("");
lines.push("> 실무 개발자 빠른 경로: **A(입문) → B(아키텍처 01~04) → E(개발 매뉴얼 핵심 장) → C(Handler/ServiceId) → I(설정)**.  ");
lines.push("> 아키텍트/운영: **B 전체 → C → I → 용량 Word**.  ");
lines.push("> 교육용: **A → G → F(집필본-md)**.");
lines.push("");
lines.push("## 폴더별 파일 수");
lines.push("");
lines.push("| 폴더 | 문서 수 |");
lines.push("| --- | ---: |");
for (const d of dirs) {
  lines.push(`| \`${d}\` | ${counts[d] || 0} |`);
}
lines.push(`| **합계** | **${all.length}** |`);
lines.push("");

function pushDirSection(title, dirName, intro, transform) {
  lines.push(`## ${title}`);
  lines.push("");
  if (intro) {
    lines.push(intro);
    lines.push("");
  }
  let files = sectionFiles(all, dirName);
  if (typeof transform === "function") files = transform(files);
  if (!files.length) {
    lines.push("_(문서 없음)_");
    lines.push("");
    return;
  }
  lines.push(bulletList(files));
  lines.push("");
}

// A. Beginner book
pushDirSection(
  "A. 초보 입문 — `ztcfbook-m` (가장 먼저)",
  "ztcfbook-m",
  "쉬운 한국어 입문 책입니다. **00-목차 → 서문 → 제01편…제10편 → 부록** 순으로 읽습니다.",
  orderBookFiles
);

// B. Architecture
pushDirSection(
  "B. 아키텍처 — `zarchitecture`",
  "zarchitecture",
  "번호 순서(01→16)로 읽습니다. README는 시작 전에 한 번 확인합니다.",
  (files) => {
    const readme = files.filter((f) => f.name.toLowerCase() === "readme.md");
    const rest = files.filter((f) => f.name.toLowerCase() !== "readme.md");
    return [...readme, ...rest];
  }
);

// C. Design manual zman
pushDirSection(
  "C. 설계서 — `zman`",
  "zman",
  "00→25 번호 순이 본문 읽기 순서입니다. Word 설계서는 참고용입니다.",
  (files) => {
    const md = files.filter((f) => f.ext === ".md");
    const other = files.filter((f) => f.ext !== ".md");
    const readme = md.filter((f) => f.name.toLowerCase() === "readme.md");
    const numbered = md
      .filter((f) => f.name.toLowerCase() !== "readme.md")
      .sort((a, b) => naturalKey(a.name).localeCompare(naturalKey(b.name), "ko"));
    return [...readme, ...numbered, ...other];
  }
);

// D1 zdocs-2
pushDirSection(
  "D-1. 주제별 개념 노트 — `zdocs-2`",
  "zdocs-2",
  "아키텍처·설계를 본 뒤, 주제별로 필요할 때 참조합니다. 권장 선독: `TCF.md` → `어플리케이션계층.md` → `서비스카탈로그.md` → `명명규칙`/`applicationNaming.md` → `온라인처리.md` → `세션관리.md`/`인증관리.md` → `DAO처리.md` → `캐시관리.md` → `배치처리.md`."
);

// D2 zdocs-1
pushDirSection(
  "D-2. 프레임워크 가이드·소스 인덱스 — `zdocs-1`",
  "zdocs-1",
  "먼저 루트 가이드 3종을 읽고, `architecture/` → `manual/` → `sample-requests/` → `설계자료/` 순으로 확장합니다.",
  (files) => {
    const priority = [
      "SOURCE_INDEX.md",
      "TCF_FRAMEWORK_GUIDE.md",
      "TCF_MODULE_RESTRUCTURE.md",
    ];
    const head = [];
    for (const p of priority) {
      const hit = files.find((f) => f.name === p);
      if (hit) head.push(hit);
    }
    const rest = files.filter((f) => !priority.includes(f.name));
    const arch = rest.filter((f) => f.rel.includes("/architecture/")).sort((a, b) => naturalKey(a.rel).localeCompare(naturalKey(b.rel), "ko"));
    const man = rest.filter((f) => f.rel.includes("/manual/")).sort((a, b) => naturalKey(a.rel).localeCompare(naturalKey(b.rel), "ko"));
    const sample = rest.filter((f) => f.rel.includes("/sample-requests/")).sort((a, b) => naturalKey(a.rel).localeCompare(naturalKey(b.rel), "ko"));
    const design = rest.filter((f) => f.rel.includes("/설계자료/")).sort((a, b) => naturalKey(a.rel).localeCompare(naturalKey(b.rel), "ko"));
    const other = rest.filter(
      (f) =>
        !f.rel.includes("/architecture/") &&
        !f.rel.includes("/manual/") &&
        !f.rel.includes("/sample-requests/") &&
        !f.rel.includes("/설계자료/")
    );
    return [...head, ...arch, ...man, ...sample, ...design, ...other];
  }
);

// E1 guide word
pushDirSection(
  "E-1. 개발 매뉴얼(Word) — `znsight-guide-word`",
  "znsight-guide-word",
  "파일명의 장 번호(00, 1, 2… / 또는 부록) 순으로 읽습니다. 개발 실무의 표준 절차서입니다."
);

// E2 zguide
pushDirSection(
  "E-2. 모듈별 개발 가이드 — `zguide`",
  "zguide",
  "README 후, 플랫폼 코어(`tcf-core` → `tcf-gateway` → `tcf-jwt` → `tcf-om` → `tcf-ui`/`tcf-uj`) → 업무 WAR(`sv`→`ic`→`eb`→`ep`→…) → 인프라(`cache`/`batch`/`cicd`/`scripts`) 순을 권장합니다.",
  (files) => {
    const order = [
      "README.md",
      "tcf-core-개발가이드.md",
      "tcf-gateway-개발가이드.md",
      "tcf-jwt-개발가이드.md",
      "tcf-om-개발가이드.md",
      "tcf-ui-개발가이드.md",
      "tcf-uj-개발가이드.md",
      "tcf-eai-개발가이드.md",
      "tcf-cache-개발가이드.md",
      "tcf-batch-개발가이드.md",
      "tcf-cicd-개발가이드.md",
      "tcf-scripts-개발가이드.md",
      "sv-service-개발가이드.md",
      "ic-service-개발가이드.md",
      "eb-service-개발가이드.md",
      "ep-service-개발가이드.md",
      "pc-service-개발가이드.md",
      "pd-service-개발가이드.md",
      "ms-service-개발가이드.md",
      "mg-service-개발가이드.md",
      "ss-service-개발가이드.md",
      "om-service-개발가이드.md",
    ];
    const map = new Map(files.map((f) => [f.name, f]));
    const ordered = [];
    for (const n of order) if (map.has(n)) ordered.push(map.get(n));
    for (const f of files) if (!order.includes(f.name)) ordered.push(f);
    return ordered;
  }
);

// F books
pushDirSection(
  "F-1. 집필본 Markdown — `ztcf-집필본-md`",
  "ztcf-집필본-md",
  "Chapter 번호 순(1→…)으로 읽습니다. Word 원본은 `ztcf-집필본`에 있습니다."
);

pushDirSection(
  "F-2. 집필본 Word — `ztcf-집필본`",
  "ztcf-집필본",
  "Markdown과 병행 시 MD를 우선하고, 필요 시 Word를 확인합니다."
);

pushDirSection(
  "F-3. 통합 개발북 — `ztcfbook`",
  "ztcfbook",
  "00-목차 → 서문 → 제01편… 순. HTML/심화 버전은 `ztcfbook-h`.",
  orderBookFiles
);

pushDirSection(
  "F-4. 심화 개발북 — `ztcfbook-h`",
  "ztcfbook-h",
  "`ztcfbook`을 읽은 뒤 심화 보강용으로 같은 편·장 순서를 따릅니다.",
  orderBookFiles
);

pushDirSection(
  "F-5. 개발북(추가) — `ztcf-개발북`",
  "ztcf-개발북",
  "보조 개발북 자료입니다. 폴더 내 번호·목차 파일을 먼저 확인하세요."
);

// G man
pushDirSection(
  "G. 개발 입문서 — `znsight-man`",
  "znsight-man",
  "『개발 입문서』 장 번호(제1장→…) 순으로 읽습니다."
);

// H methodology
pushDirSection(
  "H-1. 구축방법론 — `znsight-구축방법론`",
  "znsight-구축방법론",
  "구축·이행 절차 문서입니다. 파일명 번호/장 순으로 읽습니다."
);

// AI methodology from diary
{
  lines.push("## H-2. AI 업무모델 자동화 방법론 — `ztcf-다이어리/2026-07-25-AI-Methology`");
  lines.push("");
  lines.push("Model Studio·자동화 개발 절차 관련 일자 자료입니다.");
  lines.push("");
  const ai = all
    .filter((f) => f.rel.includes("2026-07-25-AI-Methology"))
    .sort((a, b) => naturalKey(a.rel).localeCompare(naturalKey(b.rel), "ko"));
  // prefer md first
  const md = ai.filter((f) => f.ext === ".md");
  const rest = ai.filter((f) => f.ext !== ".md");
  lines.push(bulletList([...md, ...rest]));
  lines.push("");
}

// I capacity/config
pushDirSection(
  "I-1. 용량산정 Markdown — `ztcf-book-capacity-md`",
  "ztcf-book-capacity-md",
  "용량·TPS/TPMC 관련 MD를 먼저 읽고, Word 상세는 I-2로 확장합니다."
);

pushDirSection(
  "I-2. 용량산정 Word — `znsight-capacity-word`",
  "znsight-capacity-word",
  "개요·공식 → TPS 시나리오 → JVM/Tomcat/Apache/L4 설정 가이드 → OOM/운영 순으로 필요한 문서만 골라 읽습니다."
);

pushDirSection(
  "I-3. 환경설정 값(Word) — `znsight-config-value-word`",
  "znsight-config-value-word",
  "01 Apache → 02 Tomcat → 03 Spring → 04 HikariCP → … → 14 체크리스트 번호 순을 권장합니다."
);

pushDirSection(
  "I-4. 엔진 설정 정보 — `ztcf-engine-config-info`",
  "ztcf-engine-config-info",
  "엔진(Tomcat/Apache 등) 설정 레퍼런스입니다. README/목차가 있으면 먼저 읽습니다."
);

{
  lines.push("## I-5. 통합 환경·시스템 매뉴얼 — `znsight-config-info`");
  lines.push("");
  lines.push(
    "파일이 매우 많습니다. **전체 통독보다 역할별 진입**을 권장합니다."
  );
  lines.push("");
  lines.push("1. `nsight_system_manual` — 시스템 매뉴얼(개요·운영)");
  lines.push("2. `nsight_env_config_one` — 단일 묶음 환경설정");
  lines.push("3. `nsight_env_config` — 세부 환경설정 트리");
  lines.push("");
  const cfg = all.filter((f) => f.dir === "znsight-config-info");
  const tops = {
    manual: cfg.filter((f) => f.rel.includes("/nsight_system_manual/")),
    one: cfg.filter((f) => f.rel.includes("/nsight_env_config_one/")),
    env: cfg.filter((f) => f.rel.includes("/nsight_env_config/")),
    other: cfg.filter(
      (f) =>
        !f.rel.includes("/nsight_system_manual/") &&
        !f.rel.includes("/nsight_env_config_one/") &&
        !f.rel.includes("/nsight_env_config/")
    ),
  };
  for (const [k, label] of [
    ["manual", "nsight_system_manual"],
    ["one", "nsight_env_config_one"],
    ["env", "nsight_env_config"],
    ["other", "기타"],
  ]) {
    lines.push(`### ${label} (${tops[k].length}개)`);
    lines.push("");
    const sorted = tops[k].sort((a, b) => naturalKey(a.rel).localeCompare(naturalKey(b.rel), "ko"));
    // If huge, still list all since user asked for full reading links
    lines.push(bulletList(sorted));
    lines.push("");
  }
}

// J diary (excluding this catalog folder internals maybe include)
{
  lines.push("## J. 일자별 다이어리 — `ztcf-다이어리`");
  lines.push("");
  lines.push("날짜 폴더 순으로 읽되, 현재 주제와 맞는 일자만 선택해도 됩니다.");
  lines.push("");
  const diary = all
    .filter((f) => f.dir === "ztcf-다이어리" && !f.rel.includes("2026-07-25 전체 파일 목차"))
    .sort((a, b) => naturalKey(a.rel).localeCompare(naturalKey(b.rel), "ko"));
  lines.push(bulletList(diary));
  lines.push("");
}

lines.push("## 부록. 역할별 최소 읽기 세트");
lines.push("");
lines.push("### 신규 업무 개발자");
lines.push("1. `ztcfbook-m` 제01~03편");
lines.push("2. `zarchitecture` 01~04, 16");
lines.push("3. `zguide`의 담당 업무 WAR 가이드 + `tcf-core`");
lines.push("4. `znsight-guide-word` DTO/Handler/Facade/Service/DAO/SQL 장");
lines.push("5. `zman` 07~08 (Dispatcher·Handler)");
lines.push("6. AI 방법론 `ai-방법론.md` (자동화 시)");
lines.push("");
lines.push("### 플랫폼/공통 개발자");
lines.push("1. `zarchitecture` 전체");
lines.push("2. `zman` 05~16");
lines.push("3. `zguide` tcf-* 가이드");
lines.push("4. `zdocs-1` SOURCE_INDEX + architecture");
lines.push("");
lines.push("### 운영/용량");
lines.push("1. `zarchitecture` 10, 15, 16");
lines.push("2. `znsight-config-value-word` 01~14");
lines.push("3. `ztcf-book-capacity-md` + 필요 Word");
lines.push("4. `ztcf-engine-config-info`");
lines.push("");
lines.push("---");
lines.push("");
lines.push("원본 인벤토리 JSON: [_inventory.json](./_inventory.json)");
lines.push("");

const mdPath = path.join(outDir, "NSIGHT-TCF-전체문서-읽기순서.md");
fs.writeFileSync(mdPath, lines.join("\n"), "utf8");
console.log("Wrote", mdPath);
console.log("bytes", fs.statSync(mdPath).size);
console.log("files", all.length);
