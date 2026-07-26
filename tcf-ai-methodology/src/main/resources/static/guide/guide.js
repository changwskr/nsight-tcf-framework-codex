/**
 * ai-방법론.md 기반 개발 절차서 화면.
 * /guide/ai-methodology.md 를 읽어 TOC + 본문으로 구성한다.
 */
(function () {
  const GUIDE_MD = "/guide/ai-methodology.md";
  const PHASES_JSON = "/guide/phases.json";

  function escapeHtml(value) {
    return String(value ?? "").replace(/[&<>'"]/g, (ch) =>
      ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", "'": "&#39;", '"': "&quot;" }[ch]));
  }

  function slugify(text) {
    return String(text)
      .trim()
      .toLowerCase()
      .replace(/[^\w가-힣0-9]+/g, "-")
      .replace(/^-+|-+$/g, "")
      .slice(0, 80) || "section";
  }

  /** 경량 Markdown → HTML (헤딩/표/코드/목록/강조) */
  function renderMarkdown(md) {
    const lines = md.replace(/\r\n/g, "\n").split("\n");
    const html = [];
    let i = 0;
    let inCode = false;
    let codeLang = "";
    let codeBuf = [];
    let inUl = false;
    let inOl = false;
    let inTable = false;
    let tableRows = [];
    const usedIds = new Set();

    function closeLists() {
      if (inUl) { html.push("</ul>"); inUl = false; }
      if (inOl) { html.push("</ol>"); inOl = false; }
    }

    function flushTable() {
      if (!inTable || !tableRows.length) return;
      const header = tableRows[0];
      const body = tableRows.slice(2);
      html.push('<div class="guide-table-wrap"><table class="guide-table"><thead><tr>');
      header.forEach((c) => html.push(`<th>${inline(c)}</th>`));
      html.push("</tr></thead><tbody>");
      body.forEach((row) => {
        html.push("<tr>");
        row.forEach((c) => html.push(`<td>${inline(c)}</td>`));
        html.push("</tr>");
      });
      html.push("</tbody></table></div>");
      tableRows = [];
      inTable = false;
    }

    function uniqueId(base) {
      let id = base;
      let n = 2;
      while (usedIds.has(id)) id = `${base}-${n++}`;
      usedIds.add(id);
      return id;
    }

    function inline(text) {
      let s = escapeHtml(text);
      s = s.replace(/`([^`]+)`/g, "<code>$1</code>");
      s = s.replace(/\*\*([^*]+)\*\*/g, "<strong>$1</strong>");
      s = s.replace(/\*([^*]+)\*/g, "<em>$1</em>");
      return s;
    }

    function parseRow(line) {
      return line.replace(/^\||\|$/g, "").split("|").map((c) => c.trim());
    }

    while (i < lines.length) {
      const line = lines[i];

      if (line.startsWith("```")) {
        closeLists();
        flushTable();
        if (!inCode) {
          inCode = true;
          codeLang = line.slice(3).trim();
          codeBuf = [];
        } else {
          html.push(`<pre class="guide-code"><code class="lang-${escapeHtml(codeLang)}">${escapeHtml(codeBuf.join("\n"))}</code></pre>`);
          inCode = false;
        }
        i += 1;
        continue;
      }
      if (inCode) {
        codeBuf.push(line);
        i += 1;
        continue;
      }

      if (/^\|/.test(line) && line.includes("|")) {
        closeLists();
        if (!inTable) inTable = true;
        tableRows.push(parseRow(line));
        i += 1;
        continue;
      }
      if (inTable) flushTable();

      const h = line.match(/^(#{1,3})\s+(.+)$/);
      if (h) {
        closeLists();
        const level = h[1].length;
        const title = h[2].trim();
        const id = uniqueId(slugify(title));
        html.push(`<h${level} id="${id}" class="guide-h${level}" data-toc="${level <= 2 ? "1" : "0"}">${inline(title)}</h${level}>`);
        i += 1;
        continue;
      }

      if (/^---+$/.test(line.trim())) {
        closeLists();
        html.push('<hr class="guide-hr">');
        i += 1;
        continue;
      }

      const ul = line.match(/^\s*[-*]\s+(.+)$/);
      if (ul) {
        if (inOl) { html.push("</ol>"); inOl = false; }
        if (!inUl) { html.push("<ul>"); inUl = true; }
        html.push(`<li>${inline(ul[1])}</li>`);
        i += 1;
        continue;
      }

      const ol = line.match(/^\s*\d+\.\s+(.+)$/);
      if (ol) {
        if (inUl) { html.push("</ul>"); inUl = false; }
        if (!inOl) { html.push("<ol>"); inOl = true; }
        html.push(`<li>${inline(ol[1])}</li>`);
        i += 1;
        continue;
      }

      closeLists();
      if (!line.trim()) {
        i += 1;
        continue;
      }
      html.push(`<p>${inline(line)}</p>`);
      i += 1;
    }

    closeLists();
    flushTable();
    return html.join("\n");
  }

  function buildTocFromDom(root) {
    return [...root.querySelectorAll("h1[data-toc='1'], h2[data-toc='1']")].map((h) => ({
      id: h.id,
      level: Number(h.tagName.slice(1)),
      title: h.textContent || "",
    }));
  }

  function bindPhaseNav(phasesEl, content) {
    if (!phasesEl) return;
    phasesEl.querySelectorAll(".guide-phase").forEach((btn) => {
      btn.addEventListener("click", () => {
        const id = Number(btn.dataset.phase);
        const heading = [...content.querySelectorAll("h1,h2")].find((h) => {
          const t = h.textContent || "";
          return new RegExp(`단계\\s*${id}[\\.\\s]`).test(t);
        });
        if (heading) {
          heading.scrollIntoView({ behavior: "smooth", block: "start" });
          phasesEl.querySelectorAll(".guide-phase").forEach((b) => b.classList.toggle("active", b === btn));
        }
      });
    });
  }

  async function loadGuide() {
    const content = document.getElementById("guideContent");
    const tocEl = document.getElementById("guideToc");
    const phasesEl = document.getElementById("guidePhases");
    if (!content) return;

    content.innerHTML = `<div class="guide-loading">절차서를 불러오는 중…</div>`;
    try {
      const [mdRes, phaseRes] = await Promise.all([
        fetch(GUIDE_MD),
        fetch(PHASES_JSON),
      ]);
      if (!mdRes.ok) throw new Error(`절차서 로드 실패 (${mdRes.status})`);
      const md = await mdRes.text();
      const phases = phaseRes.ok ? await phaseRes.json() : [];

      if (phasesEl) {
        phasesEl.innerHTML = phases.map((p) =>
          `<button type="button" class="guide-phase" data-phase="${p.id}" title="${escapeHtml(p.output)}">
            <span class="num">${p.id}</span>
            <span class="label">${escapeHtml(p.name)}</span>
          </button>`).join("");
      }

      content.innerHTML = `<article class="guide-body">${renderMarkdown(md)}</article>`;
      const body = content.querySelector(".guide-body");
      bindPhaseNav(phasesEl, body);

      if (tocEl) {
        const toc = buildTocFromDom(body);
        tocEl.innerHTML = toc.map((item) =>
          `<a class="guide-toc-item level-${item.level}" href="#${item.id}">${escapeHtml(item.title)}</a>`
        ).join("");
        tocEl.querySelectorAll("a").forEach((a) => {
          a.addEventListener("click", (e) => {
            e.preventDefault();
            const el = document.getElementById(a.getAttribute("href").slice(1));
            if (el) el.scrollIntoView({ behavior: "smooth", block: "start" });
          });
        });
      }
    } catch (err) {
      content.innerHTML = `<div class="guide-error">${escapeHtml(err.message)}</div>`;
    }
  }

  window.showGuideView = async function showGuideView() {
    if (typeof state !== "undefined") state.view = "guide";
    document.getElementById("browseView")?.classList.add("hidden");
    document.getElementById("editorView")?.classList.add("hidden");
    document.getElementById("guideView")?.classList.remove("hidden");
    document.getElementById("editorToolbar")?.classList.add("hidden");
    document.getElementById("browseToolbar")?.classList.add("hidden");
    document.getElementById("guideToolbar")?.classList.remove("hidden");
    const title = document.getElementById("pageTitle");
    if (title) title.textContent = "업무모델 자동화 개발 절차서";
    await loadGuide();
    if (typeof status === "function") status("개발 절차서");
  };

  window.reloadGuideContent = loadGuide;
})();
