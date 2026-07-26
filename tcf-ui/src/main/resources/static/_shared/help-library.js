/**
 * HELP 문서 라이브러리 — doc-catalog.json 기반 전체 md 검색·목록
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

  const CORPUS_LABEL = {
    'help-meta': '문서 지도',
    'help-native': 'HELP 가이드',
    zguide: 'zguide',
    zman: 'zman',
    zdoc: 'zdoc',
    'znsight-man': 'znsight-man',
    capacity: '용량·환경',
    architecture: '아키텍처',
    ztcfbook: 'ztcfbook',
    'ztcfbook-m': 'ztcfbook-m',
    'ztcfbook-h': 'ztcfbook-h',
    config: '설정 참고',
    'module-readme': '모듈 README',
    infra: '인프라',
    'help-internal': 'tcf-help',
    root: '루트'
  };

  let catalog = null;
  let activeCorpus = '';

  function setNavLinks() {
    document.querySelectorAll('[data-help-ui-link]').forEach(el => {
      const path = el.getAttribute('href');
      if (path) el.setAttribute('href', uiUrl(path));
    });
  }

  function readUrl(src) {
    return uiUrl('/help/view.html?src=' + encodeURIComponent(src));
  }

  function sortEntries(rows) {
    return rows.slice().sort((a, b) => {
      const fa = a.featured ? 1 : 0;
      const fb = b.featured ? 1 : 0;
      if (fb !== fa) return fb - fa;
      const pa = a.priority || 0;
      const pb = b.priority || 0;
      if (pb !== pa) return pb - pa;
      return a.title.localeCompare(b.title, 'ko');
    });
  }

  function renderCorpusNav(corpora) {
    const nav = document.getElementById('helpLibraryCorpus');
    if (!nav) return;
    const allBtn = `<button type="button" class="help-library-corpus__btn${activeCorpus === '' ? ' help-library-corpus__btn--active' : ''}" data-corpus="">전체 <span>${catalog.total}</span></button>`;
    const buttons = corpora.map(c => `
      <button type="button" class="help-library-corpus__btn${activeCorpus === c.id ? ' help-library-corpus__btn--active' : ''}" data-corpus="${escapeHtml(c.id)}">
        ${escapeHtml(CORPUS_LABEL[c.id] || c.title)} <span>${c.count}</span>
      </button>`).join('');
    nav.innerHTML = allBtn + buttons;
    nav.querySelectorAll('[data-corpus]').forEach(btn => {
      btn.addEventListener('click', () => {
        activeCorpus = btn.dataset.corpus || '';
        const url = new URL(location.href);
        if (activeCorpus) url.searchParams.set('corpus', activeCorpus);
        else url.searchParams.delete('corpus');
        history.replaceState(null, '', url.pathname + url.search);
        renderCorpusNav(corpora);
        renderFeatured();
        renderTable();
      });
    });
  }

  function filteredEntries() {
    const q = (document.getElementById('helpLibrarySearch')?.value || '').trim().toLowerCase();
    return sortEntries((catalog.entries || []).filter(e => {
      if (activeCorpus && e.corpus !== activeCorpus) return false;
      if (!q) return true;
      const hay = [e.title, e.source, e.summary, e.corpus, e.module, ...(e.tags || [])].join(' ').toLowerCase();
      return hay.includes(q);
    }));
  }

  function featuredEntries() {
    const ids = new Set(catalog.featured || []);
    const byId = new Map((catalog.entries || []).map(e => [e.id, e]));
    const ordered = (catalog.featured || []).map(id => byId.get(id)).filter(Boolean);
    const rest = (catalog.entries || []).filter(e => e.featured && !ids.has(e.id));
    return sortEntries([...ordered, ...rest]);
  }

  function renderFeatured() {
    const el = document.getElementById('helpLibraryFeatured');
    if (!el || !catalog) return;
    const q = (document.getElementById('helpLibrarySearch')?.value || '').trim();
    if (q || activeCorpus) {
      el.hidden = true;
      el.innerHTML = '';
      return;
    }
    const items = featuredEntries().slice(0, 12);
    if (!items.length) {
      el.hidden = true;
      return;
    }
    el.hidden = false;
    el.innerHTML = `
      <h2 class="help-library-featured__title">추천 문서</h2>
      <div class="help-library-featured__grid">
        ${items.map(e => `
          <a class="help-library-featured__card" href="${readUrl(e.source)}">
            <span class="help-library-featured__badge">추천</span>
            <strong>${escapeHtml(e.title)}</strong>
            <span class="help-library-corpus-pill">${escapeHtml(CORPUS_LABEL[e.corpus] || e.corpus)}</span>
            <p>${escapeHtml(e.summary || e.source)}</p>
          </a>`).join('')}
      </div>`;
  }

  function renderActiveCorpusInfo() {
    const el = document.getElementById('helpLibraryActiveCorpus');
    if (!el) return;
    if (!activeCorpus) {
      el.innerHTML = '';
      return;
    }
    const c = catalog.corpora.find(x => x.id === activeCorpus);
    if (!c) {
      el.innerHTML = '';
      return;
    }
    const guide = c.metaDocId
      ? `<a href="${uiUrl('/help/view.html?doc=' + encodeURIComponent(c.metaDocId))}">코퍼스 안내 →</a>`
      : '';
    el.innerHTML = `<p><strong>${escapeHtml(c.title)}</strong> — ${escapeHtml(c.description || '')} ${guide}</p>`;
  }

  function rowHtml(e) {
    const featuredClass = e.featured ? ' help-library-table__row--featured' : '';
    const badge = e.featured ? '<span class="help-library-featured__badge help-library-featured__badge--inline">추천</span> ' : '';
    const tags = (e.tags || []).slice(0, 3).map(t =>
      `<span class="help-library-tag-pill">${escapeHtml(t)}</span>`
    ).join('');
    return `
      <tr class="${featuredClass.trim()}">
        <td class="help-library-table__title">${badge}<strong>${escapeHtml(e.title)}</strong>${tags ? `<div class="help-library-table__tags">${tags}</div>` : ''}</td>
        <td><span class="help-library-corpus-pill">${escapeHtml(CORPUS_LABEL[e.corpus] || e.corpus)}</span></td>
        <td class="help-library-table__path"><code>${escapeHtml(e.source)}</code></td>
        <td class="help-library-table__summary">${escapeHtml(e.summary || '—')}</td>
        <td class="help-library-table__action">
          <a class="help-library-read-btn" href="${readUrl(e.source)}">읽기</a>
        </td>
      </tr>`;
  }

  function renderTable() {
    const body = document.getElementById('helpLibraryBody');
    const footer = document.getElementById('helpLibraryFooter');
    if (!body || !catalog) return;
    const rows = filteredEntries();
    renderActiveCorpusInfo();
    renderFeatured();
    if (!rows.length) {
      body.innerHTML = '<tr><td colspan="5" class="help-viewer-empty">검색 결과 없음</td></tr>';
    } else {
      body.innerHTML = rows.map(rowHtml).join('');
    }
    if (footer) {
      const featuredCount = (catalog.entries || []).filter(e => e.featured).length;
      footer.textContent = `표시 ${rows.length}건 / 전체 ${catalog.total}건 · 추천 ${featuredCount}건 · 갱신 ${catalog.generatedAt ? catalog.generatedAt.slice(0, 10) : ''}`;
    }
  }

  async function init() {
    setNavLinks();
    const params = new URLSearchParams(location.search);
    activeCorpus = params.get('corpus') || '';

    try {
      const res = await fetch(uiUrl('/help/doc-catalog.json'));
      if (!res.ok) throw new Error('doc-catalog.json');
      catalog = await res.json();
      const countEl = document.getElementById('helpLibraryCount');
      if (countEl) countEl.textContent = String(catalog.total);
      const sub = document.getElementById('helpLibrarySubtitle');
      if (sub) sub.textContent = `${catalog.total}개 마크다운 · ${catalog.corpora.length}개 코퍼스 · 추천 ${(catalog.featured || []).length}편`;
      renderCorpusNav(catalog.corpora || []);
      renderTable();
      const search = document.getElementById('helpLibrarySearch');
      if (search) search.addEventListener('input', renderTable);
    } catch (e) {
      const body = document.getElementById('helpLibraryBody');
      if (body) {
        body.innerHTML = `<tr><td colspan="5" class="help-viewer-empty">카탈로그 로드 실패. gradle :tcf-help:exportHelp :tcf-ui:processResources 실행 후 새로고침.</td></tr>`;
      }
    }
  }

  init();
})();
