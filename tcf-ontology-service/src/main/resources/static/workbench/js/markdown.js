/**
 * Lightweight Markdown → HTML renderer for Architecture Knowledge docs.
 * Supports: headings, fenced code, lists, tables, links, images, hr, bold/italic/code.
 */
(function (global) {
  function esc(v) {
    return String(v ?? "")
      .replace(/&/g, "&amp;")
      .replace(/</g, "&lt;")
      .replace(/>/g, "&gt;")
      .replace(/"/g, "&quot;");
  }

  function inline(text) {
    let s = esc(text);
    // images ![alt](url)
    s = s.replace(/!\[([^\]]*)\]\(([^)\s]+)(?:\s+\"([^\"]*)\")?\)/g, (_, alt, url) => {
      return `<img src="${esc(url)}" alt="${esc(alt)}" loading="lazy">`;
    });
    // links [text](url)
    s = s.replace(/\[([^\]]+)\]\(([^)\s]+)(?:\s+\"([^\"]*)\")?\)/g, (_, label, url) => {
      const href = String(url || "");
      // keep relative md links as hash knowledge links when possible
      if (href.endsWith(".md") || href.includes(".md#") || href.includes(".md)")) {
        const file = href.replace(/^\.\//, "").split("#")[0].split("/").pop();
        const id = decodeURIComponent(file).replace(/ /g, "_");
        return `<a href="#/knowledge?doc=${encodeURIComponent(id)}">${label}</a>`;
      }
      return `<a href="${esc(href)}" target="_blank" rel="noopener">${label}</a>`;
    });
    // bold ** ** / __ __
    s = s.replace(/\*\*([^*]+)\*\*/g, "<strong>$1</strong>");
    s = s.replace(/__([^_]+)__/g, "<strong>$1</strong>");
    // italic * * / _ _
    s = s.replace(/(^|[^*])\*([^*\n]+)\*(?!\*)/g, "$1<em>$2</em>");
    // inline code
    s = s.replace(/`([^`]+)`/g, "<code>$1</code>");
    return s;
  }

  function isTableSeparator(line) {
    return /^\s*\|?[\s:-]+\|[\s|:-]*\|?\s*$/.test(line) && line.includes("-");
  }

  function parseTable(lines, start) {
    const rows = [];
    let i = start;
    while (i < lines.length && lines[i].includes("|")) {
      const raw = lines[i].trim();
      if (!raw) break;
      if (i === start + 1 && isTableSeparator(raw)) {
        i++;
        continue;
      }
      const cells = raw
        .replace(/^\|/, "")
        .replace(/\|$/, "")
        .split("|")
        .map((c) => c.trim());
      rows.push(cells);
      i++;
    }
    if (rows.length < 1) return null;
    const head = rows[0];
    const body = rows.slice(1);
    let html = "<table><thead><tr>";
    head.forEach((c) => {
      html += `<th>${inline(c)}</th>`;
    });
    html += "</tr></thead><tbody>";
    body.forEach((r) => {
      html += "<tr>";
      r.forEach((c) => {
        html += `<td>${inline(c)}</td>`;
      });
      html += "</tr>";
    });
    html += "</tbody></table>";
    return { html, next: i };
  }

  function renderMarkdown(md) {
    const src = String(md || "").replace(/\r\n/g, "\n");
    const lines = src.split("\n");
    const out = [];
    let i = 0;
    let inCode = false;
    let codeLang = "";
    let codeBuf = [];
    let listType = null; // ul | ol
    let para = [];

    function flushPara() {
      if (!para.length) return;
      out.push(`<p>${inline(para.join(" "))}</p>`);
      para = [];
    }

    function flushList() {
      if (!listType) return;
      // list items already pushed as open tags handled below differently
      listType = null;
    }

    function closeList() {
      if (listType === "ul") out.push("</ul>");
      if (listType === "ol") out.push("</ol>");
      listType = null;
    }

    while (i < lines.length) {
      const line = lines[i];

      // fenced code
      const fence = line.match(/^```\s*([a-zA-Z0-9_-]*)\s*$/);
      if (fence) {
        flushPara();
        closeList();
        if (!inCode) {
          inCode = true;
          codeLang = fence[1] || "";
          codeBuf = [];
        } else {
          out.push(
            `<pre class="wb-md__code"><code class="language-${esc(codeLang)}">${esc(codeBuf.join("\n"))}</code></pre>`
          );
          inCode = false;
          codeLang = "";
          codeBuf = [];
        }
        i++;
        continue;
      }
      if (inCode) {
        codeBuf.push(line);
        i++;
        continue;
      }

      // hr
      if (/^\s*(-{3,}|\*{3,}|_{3,})\s*$/.test(line)) {
        flushPara();
        closeList();
        out.push("<hr>");
        i++;
        continue;
      }

      // headings
      const heading = line.match(/^(#{1,6})\s+(.+)$/);
      if (heading) {
        flushPara();
        closeList();
        const level = heading[1].length;
        const text = heading[2].trim();
        const id = text.toLowerCase().replace(/[^\w가-힣]+/g, "-").replace(/^-|-$/g, "");
        out.push(`<h${level} id="${esc(id)}">${inline(text)}</h${level}>`);
        i++;
        continue;
      }

      // table
      if (line.includes("|") && i + 1 < lines.length && isTableSeparator(lines[i + 1])) {
        flushPara();
        closeList();
        const table = parseTable(lines, i);
        if (table) {
          out.push(table.html);
          i = table.next;
          continue;
        }
      }

      // lists
      const ul = line.match(/^\s*[-*+]\s+(.+)$/);
      const ol = line.match(/^\s*\d+\.\s+(.+)$/);
      if (ul || ol) {
        flushPara();
        const type = ul ? "ul" : "ol";
        if (listType !== type) {
          closeList();
          out.push(type === "ul" ? "<ul>" : "<ol>");
          listType = type;
        }
        out.push(`<li>${inline((ul || ol)[1])}</li>`);
        i++;
        continue;
      }

      // blockquote
      const bq = line.match(/^\s*>\s?(.*)$/);
      if (bq) {
        flushPara();
        closeList();
        const parts = [bq[1]];
        i++;
        while (i < lines.length) {
          const m = lines[i].match(/^\s*>\s?(.*)$/);
          if (!m) break;
          parts.push(m[1]);
          i++;
        }
        out.push(`<blockquote>${inline(parts.join(" "))}</blockquote>`);
        continue;
      }

      // blank line
      if (/^\s*$/.test(line)) {
        flushPara();
        closeList();
        i++;
        continue;
      }

      // paragraph text
      closeList();
      para.push(line.trim());
      i++;
    }

    flushPara();
    closeList();
    if (inCode) {
      out.push(`<pre class="wb-md__code"><code>${esc(codeBuf.join("\n"))}</code></pre>`);
    }
    return out.join("\n");
  }

  global.WorkbenchMarkdown = { render: renderMarkdown, escape: esc };
})(window);
