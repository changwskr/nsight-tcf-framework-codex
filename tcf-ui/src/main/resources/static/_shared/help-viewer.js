/**
 * NSIGHT TCF HELP — 문서 뷰어 (마크다운 → HTML, 외부 의존 없음)
 */
(function () {
  'use strict';

  function uiUrl(path) {
    return typeof window.nsightUiUrl === 'function' ? window.nsightUiUrl(path) : path;
  }

  function escapeHtml(t) {
    const d = document.createElement('div');
    d.textContent = t ?? '';
    return d.innerHTML;
  }

  function stripFrontmatter(md) {
    if (!md.startsWith('---')) return { meta: {}, body: md };
    const end = md.indexOf('\n---', 3);
    if (end < 0) return { meta: {}, body: md };
    const raw = md.slice(4, end).trim();
    const body = md.slice(end + 4).replace(/^\s*\n/, '');
    const meta = {};
    raw.split('\n').forEach(line => {
      const i = line.indexOf(':');
      if (i > 0) meta[line.slice(0, i).trim()] = line.slice(i + 1).trim();
    });
    return { meta, body };
  }

  function inlineFormat(text) {
    let s = escapeHtml(text);
    s = s.replace(/`([^`]+)`/g, '<code>$1</code>');
    s = s.replace(/\*\*([^*]+)\*\*/g, '<strong>$1</strong>');
    s = s.replace(/\[([^\]]+)\]\(([^)]+)\)/g, (_, label, href) => {
      if (href.startsWith('?doc=')) {
        const id = href.slice(5);
        return `<a href="${uiUrl('/help/view.html?doc=' + encodeURIComponent(id))}">${escapeHtml(label)}</a>`;
      }
      if (href.startsWith('view.html')) {
        return `<a href="${uiUrl('/help/' + href)}">${escapeHtml(label)}</a>`;
      }
      if (href.startsWith('library.html')) {
        return `<a href="${uiUrl('/help/' + href)}">${escapeHtml(label)}</a>`;
      }
      if (href.startsWith('/')) {
        return `<a href="${uiUrl(href)}">${escapeHtml(label)}</a>`;
      }
      return `<a href="${escapeHtml(href)}" target="_blank" rel="noopener">${escapeHtml(label)}</a>`;
    });
    return s;
  }

  function renderMarkdown(md) {
    const { body } = stripFrontmatter(md);
    const lines = body.split('\n');
    const out = [];
    let inCode = false;
    let codeLang = '';
    let codeBuf = [];
    let tableRows = [];
    let listType = null;
    let listBuf = [];

    function flushList() {
      if (!listBuf.length) return;
      const tag = listType === 'ol' ? 'ol' : 'ul';
      out.push(`<${tag} class="help-md-list">` + listBuf.map(li => `<li>${inlineFormat(li)}</li>`).join('') + `</${tag}>`);
      listBuf = [];
      listType = null;
    }

    function flushTable() {
      if (!tableRows.length) return;
      const [head, ...rest] = tableRows;
      const ths = head.map(c => `<th>${inlineFormat(c.trim())}</th>`).join('');
      const trs = rest.map(row => '<tr>' + row.map(c => `<td>${inlineFormat(c.trim())}</td>`).join('') + '</tr>').join('');
      out.push(`<div class="help-md-table-wrap"><table class="help-md-table"><thead><tr>${ths}</tr></thead><tbody>${trs}</tbody></table></div>`);
      tableRows = [];
    }

    for (const line of lines) {
      if (line.trim().startsWith('```')) {
        if (!inCode) {
          flushList();
          flushTable();
          inCode = true;
          codeLang = line.trim().slice(3).trim();
          codeBuf = [];
        } else {
          const cls = codeLang ? ` class="language-${escapeHtml(codeLang)}"` : '';
          const preCls = codeLang === 'mermaid' ? 'help-md-pre help-md-mermaid' : 'help-md-pre';
          out.push(`<pre class="${preCls}"><code${cls}>${escapeHtml(codeBuf.join('\n'))}</code></pre>`);
          inCode = false;
          codeLang = '';
          codeBuf = [];
        }
        continue;
      }
      if (inCode) {
        codeBuf.push(line);
        continue;
      }

      if (line.includes('|') && line.trim().startsWith('|')) {
        flushList();
        const cells = line.split('|').slice(1, -1);
        if (/^[\s|:-]+$/.test(line.replace(/[^|:\-\s]/g, ''))) continue;
        tableRows.push(cells);
        continue;
      } else {
        flushTable();
      }

      const ol = line.match(/^\d+\.\s+(.*)$/);
      const ul = line.match(/^[-*]\s+(.*)$/);
      if (ol) {
        if (listType && listType !== 'ol') flushList();
        listType = 'ol';
        listBuf.push(ol[1]);
        continue;
      }
      if (ul) {
        if (listType && listType !== 'ul') flushList();
        listType = 'ul';
        listBuf.push(ul[1]);
        continue;
      }
      flushList();

      if (/^####\s+/.test(line)) {
        out.push(`<h4 class="help-md-h4">${inlineFormat(line.replace(/^####\s+/, ''))}</h4>`);
      } else if (/^###\s+/.test(line)) {
        out.push(`<h3 class="help-md-h3">${inlineFormat(line.replace(/^###\s+/, ''))}</h3>`);
      } else if (/^##\s+/.test(line)) {
        out.push(`<h2 class="help-md-h2">${inlineFormat(line.replace(/^##\s+/, ''))}</h2>`);
      } else if (/^#\s+/.test(line)) {
        out.push(`<h1 class="help-md-h1">${inlineFormat(line.replace(/^#\s+/, ''))}</h1>`);
      } else if (line.trim() === '') {
        /* skip */
      } else {
        out.push(`<p class="help-md-p">${inlineFormat(line)}</p>`);
      }
    }
    flushList();
    flushTable();
    return out.join('\n');
  }

  function highlightCode(code, lang) {
    const l = (lang || '').toLowerCase();
    if (l === 'json') {
      return code
        .replace(/(&quot;(?:[^&]|&(?!quot;))*&quot;)(\s*:)/g, '<span class="hl-key">$1</span>$2')
        .replace(/: (&quot;(?:[^&]|&(?!quot;))*&quot;)/g, ': <span class="hl-str">$1</span>')
        .replace(/\b(true|false|null)\b/g, '<span class="hl-kw">$1</span>')
        .replace(/\b(-?\d+(?:\.\d+)?)\b/g, '<span class="hl-num">$1</span>');
    }
    if (l === 'yaml' || l === 'yml') {
      return code
        .replace(/^(\s*[\w.-]+)(:)/gm, '<span class="hl-key">$1</span>$2')
        .replace(/: (.+)$/gm, (m, v) => ': <span class="hl-str">' + v + '</span>');
    }
    if (l === 'bash' || l === 'sh' || l === 'shell' || l === 'powershell') {
      return code
        .replace(/^(\s*#.*)$/gm, '<span class="hl-cmt">$1</span>')
        .replace(/\b(gradle|npm|node|cd|export|set)\b/g, '<span class="hl-kw">$1</span>');
    }
    if (l === 'java') {
      return code
        .replace(/\b(public|private|class|interface|void|return|new|import|package|static|final)\b/g, '<span class="hl-kw">$1</span>')
        .replace(/(&quot;(?:[^&]|&(?!quot;))*&quot;)/g, '<span class="hl-str">$1</span>')
        .replace(/(\/\/.*)$/gm, '<span class="hl-cmt">$1</span>');
    }
    return code;
  }

  function enhanceCodeBlocks(root) {
    root.querySelectorAll('pre.help-md-pre code').forEach(codeEl => {
      const lang = (codeEl.className.match(/language-(\S+)/) || [])[1] || '';
      if (lang === 'mermaid') return;
      const raw = codeEl.textContent;
      codeEl.innerHTML = highlightCode(escapeHtml(raw), lang);
      codeEl.closest('pre')?.classList.add('help-md-pre--highlighted');
    });
  }

  let mermaidLoading = null;

  function loadMermaid() {
    if (window.mermaid) return Promise.resolve(window.mermaid);
    if (mermaidLoading) return mermaidLoading;
    mermaidLoading = new Promise((resolve, reject) => {
      const s = document.createElement('script');
      s.src = 'https://cdn.jsdelivr.net/npm/mermaid@10/dist/mermaid.min.js';
      s.async = true;
      s.onload = () => {
        window.mermaid.initialize({ startOnLoad: false, theme: 'dark', securityLevel: 'loose' });
        resolve(window.mermaid);
      };
      s.onerror = () => reject(new Error('mermaid load failed'));
      document.head.appendChild(s);
    });
    return mermaidLoading;
  }

  async function enhanceMermaid(root) {
    const blocks = [...root.querySelectorAll('pre.help-md-mermaid')];
    if (!blocks.length) return;
    try {
      const mermaid = await loadMermaid();
      for (let i = 0; i < blocks.length; i++) {
        const pre = blocks[i];
        const src = pre.textContent.trim();
        const id = 'help-mermaid-' + i;
        const div = document.createElement('div');
        div.className = 'help-md-mermaid-render';
        div.id = id;
        pre.replaceWith(div);
        const { svg } = await mermaid.render(id + '-svg', src);
        div.innerHTML = svg;
      }
    } catch {
      blocks.forEach(pre => {
        pre.classList.add('help-md-mermaid--fallback');
      });
    }
  }

  async function enhanceArticle(root) {
    if (!root) return;
    enhanceCodeBlocks(root);
    await enhanceMermaid(root);
  }

  function findDoc(index, docId) {
    for (const section of index.sections || []) {
      for (const item of section.items || []) {
        if (item.id === docId) return { section, item };
      }
    }
    return null;
  }

  function flatDocs(index) {
    const list = [];
    (index.sections || []).forEach(section => {
      (section.items || []).forEach(item => list.push({ section, item }));
    });
    return list;
  }

  function renderNav(index, activeId) {
    const nav = document.getElementById('helpViewerNav');
    if (!nav) return;
    nav.innerHTML = (index.sections || []).map(section => `
      <section class="help-viewer-nav__section">
        <h3 class="help-viewer-nav__heading">${escapeHtml(section.title)}</h3>
        <ul class="help-viewer-nav__list">
          ${(section.items || []).map(item => {
            const href = item.external
              ? uiUrl(item.external)
              : uiUrl('/help/view.html?doc=' + encodeURIComponent(item.id));
            return `
            <li>
              <a class="help-viewer-nav__link${item.id === activeId ? ' help-viewer-nav__link--active' : ''}"
                 href="${href}">
                ${escapeHtml(item.title)}
              </a>
            </li>`;
          }).join('')}
        </ul>
      </section>`).join('');
  }

  async function loadIndex() {
    const res = await fetch(uiUrl('/help/help-index.json'));
    if (!res.ok) throw new Error('help-index.json 로드 실패');
    return res.json();
  }

  function resolveHelpFilePath(file) {
    if (file.startsWith('meta/')) {
      return '/help/' + file.split('/').map(encodeURIComponent).join('/');
    }
    return '/help/docs/' + file.split('/').map(encodeURIComponent).join('/');
  }

  async function loadMarkdown(file) {
    const res = await fetch(uiUrl(resolveHelpFilePath(file)));
    if (!res.ok) throw new Error('문서 로드 실패: ' + file);
    return res.text();
  }

  async function loadLibraryMarkdown(src) {
    const clean = src.replace(/^\/+/, '');
    const res = await fetch(uiUrl('/help/library/' + clean.split('/').map(encodeURIComponent).join('/')));
    if (!res.ok) throw new Error('원문 로드 실패: ' + src);
    return res.text();
  }

  function setNavLinks() {
    document.querySelectorAll('[data-help-ui-link]').forEach(el => {
      const path = el.getAttribute('href');
      if (path) el.setAttribute('href', uiUrl(path));
    });
  }

  async function initViewer() {
    setNavLinks();
    const params = new URLSearchParams(location.search);
    const docId = params.get('doc');
    const srcPath = params.get('src');
    const sectionId = params.get('section');
    const article = document.getElementById('helpViewerArticle');
    const titleEl = document.getElementById('helpViewerTitle');
    const crumbEl = document.getElementById('helpViewerBreadcrumb');
    const nav = document.getElementById('helpViewerNav');

    if (srcPath) {
      try {
        const catalogRes = await fetch(uiUrl('/help/doc-catalog.json'));
        const catalog = catalogRes.ok ? await catalogRes.json() : null;
        const entry = catalog?.entries?.find(e => e.source === srcPath);
        const title = entry?.title || srcPath.split('/').pop();
        if (titleEl) titleEl.textContent = title;
        if (crumbEl) {
          crumbEl.innerHTML = `<a href="${uiUrl('/help.html')}">HELP</a> · <a href="${uiUrl('/help/library.html')}">라이브러리</a> · <strong>${escapeHtml(title)}</strong>`;
        }
        document.title = title + ' · NSIGHT TCF HELP';
        if (nav) {
          nav.innerHTML = `<p class="help-viewer-empty" style="font-size:0.82rem"><code>${escapeHtml(srcPath)}</code></p>
            <a class="help-viewer-nav__link" href="${uiUrl('/help/library.html?corpus=' + encodeURIComponent(entry?.corpus || ''))}">← 라이브러리</a>`;
        }
        const md = await loadLibraryMarkdown(srcPath);
        if (article) {
          article.innerHTML = renderMarkdown(md);
          await enhanceArticle(article);
        }
        const pager = document.getElementById('helpViewerPager');
        if (pager) pager.innerHTML = '';
      } catch (e) {
        if (article) article.innerHTML = `<p class="help-viewer-empty">문서를 불러오지 못했습니다. ${escapeHtml(e.message)}</p>`;
      }
      return;
    }

    try {
      const index = await loadIndex();
      let target = docId ? findDoc(index, docId) : null;
      if (!target && sectionId) {
        const section = (index.sections || []).find(s => s.id === sectionId);
        if (section?.items?.length) {
          target = { section, item: section.items[0] };
          history.replaceState(null, '', uiUrl('/help/view.html?doc=' + encodeURIComponent(section.items[0].id)));
        }
      }
      if (!target) {
        const first = flatDocs(index)[0];
        if (first) target = first;
      }
      if (!target) {
        if (article) article.innerHTML = '<p class="help-viewer-empty">등록된 HELP 문서가 없습니다.</p>';
        return;
      }

      const { section, item } = target;
      renderNav(index, item.id);
      if (titleEl) titleEl.textContent = item.title;
      if (crumbEl) {
        crumbEl.innerHTML = `<a href="${uiUrl('/help.html')}">HELP</a> · ${escapeHtml(section.title)} · <strong>${escapeHtml(item.title)}</strong>`;
      }
      document.title = item.title + ' · NSIGHT TCF HELP';

      const md = await loadMarkdown(item.file);
      if (article) {
        article.innerHTML = renderMarkdown(md);
        await enhanceArticle(article);
      }

      const docs = flatDocs(index);
      const idx = docs.findIndex(d => d.item.id === item.id);
      const prev = docs[idx - 1];
      const next = docs[idx + 1];
      const pager = document.getElementById('helpViewerPager');
      if (pager) {
        pager.innerHTML = `
          <a class="help-viewer-pager__btn${prev ? '' : ' help-viewer-pager__btn--disabled'}"
             ${prev ? `href="${uiUrl('/help/view.html?doc=' + encodeURIComponent(prev.item.id))}"` : ''}>
            ← ${prev ? escapeHtml(prev.item.title) : '이전 없음'}
          </a>
          <a class="help-viewer-pager__btn${next ? '' : ' help-viewer-pager__btn--disabled'}"
             ${next ? `href="${uiUrl('/help/view.html?doc=' + encodeURIComponent(next.item.id))}"` : ''}>
            ${next ? escapeHtml(next.item.title) + ' →' : '다음 없음'}
          </a>`;
      }
    } catch (e) {
      if (article) article.innerHTML = `<p class="help-viewer-empty">문서를 불러오지 못했습니다. ${escapeHtml(e.message)}</p>`;
    }
  }

  window.NsightHelpViewer = { renderMarkdown, loadIndex, enhanceArticle };

  if (document.body.classList.contains('tcf-help-viewer')) {
    initViewer();
  }
})();
